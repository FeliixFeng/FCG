<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import BaseLayout from '../components/common/BaseLayout.vue'
import { useUserStore } from '../stores/user'
import {
  createMedicineRecord,
  fetchFamilyMembers,
  fetchLatestReport,
  fetchTodayPlanRecords,
  fetchTodayVitals,
  updateMedicineRecord
} from '../utils/api'

const userStore = useUserStore()
const router = useRouter()

const loading = ref(false)
const operatingTaskKey = ref('')
const justChecked = ref(false)
let dashboardRequestId = 0

const familyMembers = ref([])
const selectedMemberId = ref(null)
const planRecords = ref([])
const vitalRecorded = ref(false)
const latestReport = ref(null)

const isAdmin = computed(() => userStore.member?.role === 0)
const isCareMode = computed(() => userStore.isCareMode)
const currentUserId = computed(() => userStore.member?.id || userStore.member?.userId || null)
const selectedMember = computed(() => selectedMemberId.value || currentUserId.value || null)

const greeting = computed(() => {
  const hour = new Date().getHours()
  if (hour < 6) return '夜深了'
  if (hour < 11) return '早上好'
  if (hour < 14) return '中午好'
  if (hour < 18) return '下午好'
  return '晚上好'
})

const todayText = computed(() => {
  const d = new Date()
  return `${d.getMonth() + 1}月${d.getDate()}日 · 星期${['日', '一', '二', '三', '四', '五', '六'][d.getDay()]}`
})

const viewingMemberName = computed(() => {
  if (!isAdmin.value || !selectedMember.value || selectedMember.value === currentUserId.value) return ''
  const member = familyMembers.value.find(item => item.userId === selectedMember.value)
  return member?.nickname || ''
})

const viewingMemberText = computed(() => {
  if (!isAdmin.value) return ''
  return viewingMemberName.value || `${userStore.member?.nickname || '我'}（我）`
})

const pendingRecords = computed(() => planRecords.value.filter(item => item.recordStatus === 0))
const takenRecords = computed(() => planRecords.value.filter(item => item.recordStatus === 1))
const skippedRecords = computed(() => planRecords.value.filter(item => item.recordStatus === 2))
const processedRecords = computed(() => sortedRecords.value.filter(item => item.recordStatus !== 0))
const pendingSortedRecords = computed(() => sortedRecords.value.filter(item => item.recordStatus === 0))
const completionRate = computed(() => {
  if (!planRecords.value.length) return 100
  return Math.round((takenRecords.value.length / planRecords.value.length) * 100)
})
const hasLatestReport = computed(() => !!latestReport.value)

const slotOrder = { '早': 1, '中': 2, '晚': 3, '睡前': 4 }
const slotTimeMap = {
  '早': { h: 8, m: 0 },
  '中': { h: 12, m: 30 },
  '晚': { h: 18, m: 30 },
  '睡前': { h: 22, m: 0 }
}

const sortedRecords = computed(() => {
  return [...planRecords.value].sort((a, b) => {
    const slotA = slotOrder[a.slotName] || 99
    const slotB = slotOrder[b.slotName] || 99
    if (slotA !== slotB) return slotA - slotB
    return (a.recordId || 0) - (b.recordId || 0)
  })
})

const nextPending = computed(() => {
  const list = [...pendingRecords.value].sort((a, b) => {
    const slotA = slotOrder[a.slotName] || 99
    const slotB = slotOrder[b.slotName] || 99
    if (slotA !== slotB) return slotA - slotB
    return (a.recordId || 0) - (b.recordId || 0)
  })
  return list[0] || null
})

const nextPendingOverdueMinutes = computed(() => {
  if (!nextPending.value?.slotName) return 0
  const base = slotTimeMap[nextPending.value.slotName]
  if (!base) return 0
  const now = new Date()
  const currentMinutes = now.getHours() * 60 + now.getMinutes()
  const plannedMinutes = base.h * 60 + base.m
  return Math.max(currentMinutes - plannedMinutes, 0)
})

const overduePendingCount = computed(() => {
  const now = new Date()
  const currentMinutes = now.getHours() * 60 + now.getMinutes()
  return pendingRecords.value.filter((item) => {
    const slot = slotTimeMap[item.slotName]
    if (!slot) return false
    const plannedMinutes = slot.h * 60 + slot.m
    return currentMinutes > plannedMinutes
  }).length
})

const nextActionLabel = computed(() => {
  return pendingRecords.value.length > 1 ? '打卡下一条' : '立即打卡'
})

const quickLinks = [
  { key: 'medicine', title: '药品管理', subtitle: '药箱与用药计划', icon: '💊', route: 'medicine' },
  { key: 'health', title: '健康中心', subtitle: '体征与健康周报', icon: '❤️', route: 'health' },
  { key: 'ai', title: 'AI 助手', subtitle: '健康问答与建议', icon: '🤖', route: 'ai' },
  { key: 'profile', title: '我的', subtitle: '成员与家庭信息', icon: '👤', route: 'profile' }
]

function toLocalDateISO() {
  const now = new Date()
  const year = now.getFullYear()
  const month = String(now.getMonth() + 1).padStart(2, '0')
  const day = String(now.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

function toLocalDateTimeISO() {
  const now = new Date()
  const year = now.getFullYear()
  const month = String(now.getMonth() + 1).padStart(2, '0')
  const day = String(now.getDate()).padStart(2, '0')
  const hour = String(now.getHours()).padStart(2, '0')
  const minute = String(now.getMinutes()).padStart(2, '0')
  const second = String(now.getSeconds()).padStart(2, '0')
  return `${year}-${month}-${day}T${hour}:${minute}:${second}`
}

function statusLabel(status) {
  if (status === 1) return '已服'
  if (status === 2) return '已跳过'
  return '待服'
}

function statusClass(status) {
  if (status === 1) return 'is-taken'
  if (status === 2) return 'is-skipped'
  return 'is-pending'
}

function slotLabel(slot) {
  if (!slot) return '今日'
  return `${slot}间`
}

function slotClock(slot) {
  const base = slotTimeMap[slot]
  if (!base) return '--:--'
  return `${String(base.h).padStart(2, '0')}:${String(base.m).padStart(2, '0')}`
}

function isFutureSlot(slot) {
  const base = slotTimeMap[slot]
  if (!base) return false
  const now = new Date()
  const currentMinutes = now.getHours() * 60 + now.getMinutes()
  const plannedMinutes = base.h * 60 + base.m
  return currentMinutes < plannedMinutes
}

function formatDosage(record) {
  if (!record?.planDosage) return '剂量未填写'
  const unit = record.medicineStockUnit || '次'
  return `每次 ${record.planDosage}${unit}`
}

function getTaskKey(record) {
  if (!record) return ''
  if (record.recordId) return `r-${record.recordId}`
  return `p-${record.planId}-${record.slotName || ''}`
}

async function loadMembers() {
  if (!isAdmin.value) return
  const res = await fetchFamilyMembers()
  familyMembers.value = (res.data || []).filter(item => item.userId !== currentUserId.value)
}

async function loadDashboardData() {
  const uid = selectedMember.value
  if (!uid) return
  const requestId = ++dashboardRequestId
  loading.value = true
  latestReport.value = null
  try {
    // 首页主卡只依赖记录和体征，先渲染，避免被周报接口拖慢
    const [recordsRes, vitalsRes] = await Promise.allSettled([
      fetchTodayPlanRecords(toLocalDateISO(), uid),
      fetchTodayVitals(uid)
    ])
    if (requestId !== dashboardRequestId) return

    if (recordsRes.status === 'fulfilled') {
      planRecords.value = recordsRes.value?.data?.records || []
    } else {
      planRecords.value = []
    }

    if (vitalsRes.status === 'fulfilled') {
      vitalRecorded.value = (vitalsRes.value?.data?.length || 0) > 0
    } else {
      vitalRecorded.value = false
    }
  } finally {
    if (requestId === dashboardRequestId) {
      loading.value = false
    }
  }

  // 周报异步加载，不阻塞主卡片显示
  fetchLatestReport(uid)
    .then((res) => {
      if (requestId !== dashboardRequestId) return
      latestReport.value = res?.data || null
    })
    .catch(() => {
      if (requestId !== dashboardRequestId) return
      latestReport.value = null
    })
}

async function checkin(record) {
  try {
    operatingTaskKey.value = getTaskKey(record)
    const actualTime = toLocalDateTimeISO()
    if (record.recordId) {
      await updateMedicineRecord(record.recordId, { status: 1, actualTime })
    } else {
      await createMedicineRecord({
        planId: record.planId,
        userId: record.userId,
        medicineId: record.medicineId,
        scheduledDate: toLocalDateISO(),
        slotName: record.slotName,
        status: 1,
        actualTime
      })
    }
    justChecked.value = true
    ElMessage.success('打卡成功')
    await loadDashboardData()
    setTimeout(() => {
      justChecked.value = false
    }, 1800)
  } catch (err) {
    ElMessage.error('打卡失败，请稍后重试')
  } finally {
    operatingTaskKey.value = ''
  }
}

async function skipRecord(record) {
  try {
    operatingTaskKey.value = getTaskKey(record)
    if (record.recordId) {
      await updateMedicineRecord(record.recordId, { status: 2 })
    } else {
      await createMedicineRecord({
        planId: record.planId,
        userId: record.userId,
        medicineId: record.medicineId,
        scheduledDate: toLocalDateISO(),
        slotName: record.slotName,
        status: 2
      })
    }
    ElMessage.success('已标记为跳过')
    await loadDashboardData()
  } catch (err) {
    ElMessage.error('操作失败，请稍后重试')
  } finally {
    operatingTaskKey.value = ''
  }
}

onMounted(async () => {
  if (currentUserId.value) {
    selectedMemberId.value = currentUserId.value
  }
  try {
    await loadMembers()
  } catch (err) {
    ElMessage.error('加载家庭成员失败')
  }
  await loadDashboardData()
})

watch(selectedMemberId, (val, oldVal) => {
  if (!val || val === oldVal) return
  loadDashboardData()
})
</script>

<template>
  <BaseLayout>
    <section class="home-page" :class="{ 'care-mode': isCareMode }">
      <header class="page-header card">
        <div class="greeting-block">
          <h1 class="title">{{ greeting }}，{{ userStore.member?.nickname || '朋友' }}</h1>
          <div class="header-meta">
            <span class="date-chip">{{ todayText }}</span>
            <span class="meta-dot"></span>
            <span class="meta-text">欢迎回来</span>
          </div>
        </div>
        <div v-if="isAdmin && familyMembers.length" class="member-selector">
          <div class="member-topline">
            <span class="member-label">查看成员</span>
            <span class="viewing-badge">当前：{{ viewingMemberText }}</span>
          </div>
          <el-select v-model="selectedMemberId" placeholder="选择成员" style="width: 188px">
            <el-option :value="currentUserId" :label="`${userStore.member?.nickname || '我'}（我）`" />
            <el-option
              v-for="member in familyMembers"
              :key="member.userId"
              :value="member.userId"
              :label="member.nickname"
            />
          </el-select>
        </div>
      </header>

      <section class="hero-grid">
        <article class="hero card check-card">
          <div v-if="loading" class="hero-loading">正在加载今日数据...</div>
          <template v-else-if="nextPending">
            <div class="check-layout">
              <div class="check-main">
                <div class="check-head">
                  <p class="hero-kicker">待完成任务</p>
                  <span class="task-status" :class="{ overdue: nextPendingOverdueMinutes > 0 }">
                    {{ nextPendingOverdueMinutes > 0 ? '已超时' : '待打卡' }}
                  </span>
                </div>
                <h2 class="hero-name">{{ nextPending.medicineName || '用药提醒' }}</h2>
                <div class="check-meta-line">
                  <span class="meta-item"><em>时段</em>{{ slotLabel(nextPending.slotName) }}</span>
                  <span class="meta-item"><em>建议时间</em>{{ slotClock(nextPending.slotName) }}</span>
                  <span class="meta-item"><em>剂量</em>{{ formatDosage(nextPending) }}</span>
                </div>
                <p v-if="nextPendingOverdueMinutes > 0" class="overdue-alert">
                  <span class="alert-mark"></span>
                  <span>已超时 {{ nextPendingOverdueMinutes }} 分钟，今天仍可补打</span>
                </p>
                <p v-else class="hero-meta">建议按时完成本次打卡</p>
                <p v-if="justChecked" class="checked-tip">已完成上一项打卡</p>
              </div>

              <div class="check-action-panel pending-panel">
                <button
                  class="btn-check-circle"
                  @click="checkin(nextPending)"
                  :disabled="operatingTaskKey === getTaskKey(nextPending)"
                >
                  <span class="circle-icon">{{ operatingTaskKey === getTaskKey(nextPending) ? '…' : '✓' }}</span>
                  <span class="circle-text">{{ operatingTaskKey === getTaskKey(nextPending) ? '处理中' : nextActionLabel }}</span>
                </button>
                <button
                  class="btn-check-skip"
                  @click="skipRecord(nextPending)"
                  :disabled="operatingTaskKey === getTaskKey(nextPending)"
                >
                  本次跳过
                </button>
                <p class="action-helper">剩余 {{ pendingRecords.length }} 项，可补打至今日 23:59</p>
              </div>
            </div>
          </template>
          <template v-else>
            <div class="check-layout done-layout">
              <div class="check-main done-main">
                <p class="hero-kicker">今日状态</p>
                <h2 class="hero-name">今日用药已全部打卡</h2>
                <p class="hero-meta">继续保持健康作息，做得很好。</p>
                <div class="done-tags">
                  <span class="done-tag">待打卡 {{ pendingRecords.length }}</span>
                  <span class="done-tag">已打卡 {{ takenRecords.length }}</span>
                </div>
              </div>
              <div class="done-visual">
                <div class="done-ring">
                  <span class="done-ring-icon">✓</span>
                  <span class="done-ring-text">已完成</span>
                </div>
                <button class="btn-done-link" @click="router.push({ name: 'medicine' })">
                  去药品页查看
                </button>
              </div>
            </div>
          </template>
        </article>

        <article class="card summary-card">
          <div class="summary-head">
            <h3>今日完成度</h3>
            <span>{{ takenRecords.length }}/{{ planRecords.length || 0 }}</span>
          </div>
          <div class="summary-main">
            <div class="progress-ring" :style="{ '--rate': `${completionRate}%` }">
              <div class="progress-inner">{{ completionRate }}%</div>
            </div>
            <div class="summary-list">
              <div class="summary-item">
                <span>待打卡</span>
                <strong>{{ pendingRecords.length }}</strong>
              </div>
              <div class="summary-item">
                <span>已打卡</span>
                <strong>{{ takenRecords.length }}</strong>
              </div>
              <div class="summary-item">
                <span>超时任务</span>
                <strong :class="{ 'text-warn': overduePendingCount > 0 }">{{ overduePendingCount }}</strong>
              </div>
              <div class="summary-item">
                <span>体征</span>
                <strong>{{ vitalRecorded ? '已录入' : '未录入' }}</strong>
              </div>
            </div>
          </div>
          <div class="report-inline" :class="{ empty: !hasLatestReport }">
            <template v-if="hasLatestReport">
              <p class="report-title">最近周报：{{ latestReport.weekStart }} ~ {{ latestReport.weekEnd }}</p>
              <p class="report-risk">
                风险：
                <span :class="`risk-${latestReport.riskLevel}`">
                  {{ latestReport.riskLevel === 2 ? '高风险' : latestReport.riskLevel === 1 ? '中风险' : '低风险' }}
                </span>
              </p>
              <button class="text-btn" @click="router.push({ name: 'health' })">前往健康页查看</button>
            </template>
            <template v-else>
              <p class="report-title">最近周报：暂无数据</p>
              <p class="report-risk">该成员还没有生成健康周报</p>
              <button class="text-btn" @click="router.push({ name: 'health' })">去健康页生成周报</button>
            </template>
          </div>
        </article>
      </section>

      <section class="card quick-section">
        <div class="section-head">
          <h3>快捷入口</h3>
        </div>
        <div class="quick-grid">
          <button
            v-for="item in quickLinks"
            :key="item.key"
            class="quick-card"
            @click="router.push({ name: item.route })"
          >
            <span class="quick-icon">{{ item.icon }}</span>
            <span class="quick-title">{{ item.title }}</span>
            <span class="quick-sub">{{ item.subtitle }}</span>
          </button>
        </div>
      </section>

      <section class="card records-section">
        <div class="section-head">
          <h3>待处理任务</h3>
          <span class="count">{{ pendingSortedRecords.length }} 项</span>
        </div>
        <div v-if="!loading && pendingSortedRecords.length === 0" class="empty">
          今日任务已全部处理完成
        </div>
        <ul v-else class="records-list">
          <li
            v-for="record in pendingSortedRecords"
            :key="getTaskKey(record)"
            class="record-item"
            :class="statusClass(record.recordStatus)"
          >
            <div class="record-left">
              <div class="slot">
                <span>{{ record.slotName || '今日' }}</span>
                <small class="slot-time">{{ slotClock(record.slotName) }}</small>
              </div>
              <div>
                <div class="medicine">{{ record.medicineName || '未命名药品' }}</div>
                <div class="dosage">{{ formatDosage(record) }}</div>
              </div>
            </div>
            <div class="record-right">
              <template v-if="record.recordStatus === 0">
                <span v-if="isFutureSlot(record.slotName)" class="time-pill">未到 {{ slotClock(record.slotName) }}</span>
                <button class="btn-action btn-checkin" @click="checkin(record)" :disabled="operatingTaskKey === getTaskKey(record)">
                  打卡
                </button>
                <button class="btn-action btn-skip" @click="skipRecord(record)" :disabled="operatingTaskKey === getTaskKey(record)">
                  跳过
                </button>
              </template>
              <span v-else class="badge">{{ statusLabel(record.recordStatus) }}</span>
            </div>
          </li>
        </ul>
      </section>

      <section class="card records-section handled-section">
        <div class="section-head">
          <h3>今日已处理</h3>
          <span class="count">{{ processedRecords.length }} 项</span>
        </div>
        <div v-if="!loading && processedRecords.length === 0" class="empty">
          还没有已处理记录，先完成第一条打卡吧
        </div>
        <ul v-else class="records-list handled-list">
          <li
            v-for="record in processedRecords"
            :key="`done-${getTaskKey(record)}`"
            class="record-item handled-item"
            :class="statusClass(record.recordStatus)"
          >
            <div class="record-left">
              <div class="slot">
                <span>{{ record.slotName || '今日' }}</span>
                <small class="slot-time">{{ slotClock(record.slotName) }}</small>
              </div>
              <div>
                <div class="medicine">{{ record.medicineName || '未命名药品' }}</div>
                <div class="dosage">{{ formatDosage(record) }}</div>
              </div>
            </div>
            <div class="record-right">
              <span class="badge">{{ statusLabel(record.recordStatus) }}</span>
            </div>
          </li>
        </ul>
      </section>

    </section>
  </BaseLayout>
</template>

<style scoped>
.home-page {
  --c-primary: #3f6f6b;
  --c-primary-deep: #2f5552;
  --c-ink: #253634;
  --c-text: #566d6a;
  --c-text-soft: #748886;
  --c-line: rgba(63, 111, 107, 0.14);
  --c-line-soft: rgba(63, 111, 107, 0.09);
  --c-surface: rgba(255, 255, 255, 0.9);
  --c-surface-soft: #f4faf8;
  --c-surface-muted: #edf5f2;
  --c-warn: #be5a4d;
  --c-warn-soft: rgba(190, 90, 77, 0.12);
  --c-success: #2f8a63;
  --c-success-soft: rgba(47, 138, 99, 0.14);
  width: 100%;
  max-width: 1200px;
  margin: 0 auto;
  padding: 0;
  padding-bottom: 100px;
  display: grid;
  gap: 16px;
}

.card {
  background: var(--c-surface);
  border: 1px solid var(--c-line-soft);
  border-radius: 16px;
  box-shadow: 0 4px 16px rgba(34, 71, 68, 0.07);
}

.page-header {
  padding: 18px 22px;
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
  background:
    radial-gradient(240px 80px at 8% 0%, rgba(63, 111, 107, 0.1) 0%, transparent 75%),
    linear-gradient(145deg, rgba(255, 255, 255, 0.96) 0%, rgba(250, 252, 251, 0.92) 100%);
}

.greeting-block {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.title {
  margin: 0;
  font-size: 1.56rem;
  color: var(--c-ink);
  letter-spacing: 0.01em;
}

.header-meta {
  display: flex;
  align-items: center;
  gap: 8px;
}

.date-chip {
  display: inline-flex;
  align-items: center;
  padding: 4px 10px;
  border-radius: 999px;
  font-size: 0.85rem;
  color: var(--c-text);
  background: rgba(63, 111, 107, 0.08);
  border: 1px solid var(--c-line);
}

.meta-dot {
  width: 4px;
  height: 4px;
  border-radius: 50%;
  background: rgba(63, 111, 107, 0.33);
}

.meta-text {
  font-size: 0.82rem;
  color: var(--c-text-soft);
}

.viewer {
  margin: 6px 0 0;
  color: #2d5f5d;
  font-size: 0.88rem;
  font-weight: 600;
}

.member-selector {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.member-topline {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.member-label {
  font-size: 0.8rem;
  color: #697978;
}

.viewing-badge {
  font-size: 0.76rem;
  color: var(--c-primary-deep);
  font-weight: 600;
  background: rgba(63, 111, 107, 0.08);
  border: 1px solid rgba(63, 111, 107, 0.2);
  border-radius: 999px;
  padding: 3px 8px;
  white-space: nowrap;
}

.hero-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.45fr) minmax(0, 1fr);
  gap: 12px;
  align-items: stretch;
}

.hero {
  padding: 22px;
  background:
    radial-gradient(120px 100px at 92% 18%, rgba(63, 111, 107, 0.12) 0%, transparent 80%),
    linear-gradient(145deg, #f7fcfb 0%, #eff7f4 68%, #f9fcfb 100%);
  color: var(--c-ink);
  border-color: var(--c-line);
}

.check-layout {
  display: grid;
  grid-template-columns: minmax(0, 1.65fr) minmax(0, 0.9fr);
  gap: 0;
  align-items: stretch;
  min-height: 206px;
  height: 100%;
}

.done-layout {
  grid-template-columns: minmax(0, 1.5fr) minmax(0, 1fr);
}

.check-main {
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  gap: 8px;
  padding: 2px 20px 2px 0;
}

.check-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.task-status {
  display: inline-flex;
  align-items: center;
  padding: 3px 9px;
  border-radius: 999px;
  font-size: 0.74rem;
  font-weight: 700;
  color: var(--c-primary-deep);
  background: rgba(63, 111, 107, 0.1);
  border: 1px solid var(--c-line);
}

.task-status.overdue {
  color: var(--c-warn);
  background: var(--c-warn-soft);
  border-color: rgba(190, 90, 77, 0.22);
}

.hero-loading {
  font-size: 0.95rem;
  opacity: 0.9;
}

.hero-kicker {
  margin: 0;
  font-size: 0.85rem;
  color: var(--c-text);
  font-weight: 600;
}

.hero-name {
  margin: 8px 0 0;
  font-size: 1.45rem;
  line-height: 1.25;
}

.hero-meta {
  margin: 0;
  font-size: 0.92rem;
  color: var(--c-text);
}

.check-meta-line {
  margin-top: 2px;
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.meta-item {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  font-size: 0.9rem;
  color: var(--c-ink);
  font-weight: 600;
}

.meta-item em {
  font-style: normal;
  color: var(--c-text-soft);
  font-size: 0.78rem;
  font-weight: 500;
}

.overdue-alert {
  margin: 0;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 3px 0 3px 2px;
  border-radius: 0;
  background: transparent;
  border: none;
  color: var(--c-warn);
  font-size: 0.82rem;
  font-weight: 600;
}

.alert-mark {
  width: 3px;
  height: 16px;
  border-radius: 99px;
  background: var(--c-warn);
  flex-shrink: 0;
}

.checked-tip {
  margin: 0;
  font-size: 0.8rem;
  display: inline-block;
  padding: 4px 9px;
  border-radius: 999px;
  color: var(--c-success);
  background: var(--c-success-soft);
  border: 1px solid rgba(47, 138, 99, 0.24);
}

.check-action-panel {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 14px 12px;
  border-left: 1px solid var(--c-line);
  background: transparent;
}

.pending-panel {
  min-height: 0;
  align-items: center;
  justify-content: center;
  gap: 9px;
}

.btn-check-circle {
  width: 96px;
  height: 96px;
  border-radius: 50%;
  border: 2px solid rgba(63, 111, 107, 0.24);
  background: #f8fcfa;
  color: var(--c-primary-deep);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 2px;
  cursor: pointer;
  transition: transform 0.15s ease, box-shadow 0.2s ease;
  box-shadow: 0 3px 8px rgba(63, 111, 107, 0.1);
}

.btn-check-circle:hover {
  transform: translateY(-1px) scale(1.01);
  box-shadow: 0 6px 12px rgba(63, 111, 107, 0.14);
}

.btn-check-circle:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.circle-icon {
  font-size: 1.65rem;
  line-height: 1;
  font-weight: 700;
}

.circle-text {
  font-size: 0.76rem;
  font-weight: 700;
}

.action-helper {
  margin: 0;
  font-size: 0.72rem;
  color: var(--c-text);
  letter-spacing: 0.02em;
  text-align: center;
  line-height: 1.35;
}

.btn-check-skip {
  border: 1px solid var(--c-line);
  background: rgba(255, 255, 255, 0.7);
  color: var(--c-primary-deep);
  font-size: 0.78rem;
  font-weight: 600;
  border-radius: 999px;
  padding: 6px 12px;
  cursor: pointer;
}

.btn-check-skip:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.done-main {
  justify-content: space-between;
}

.done-tags {
  margin-top: 10px;
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.done-tag {
  display: inline-flex;
  align-items: center;
  padding: 4px 10px;
  border-radius: 999px;
  background: rgba(63, 111, 107, 0.08);
  border: 1px solid var(--c-line);
  color: var(--c-primary-deep);
  font-size: 0.76rem;
  font-weight: 600;
}

.done-visual {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
}

.done-ring {
  width: 110px;
  height: 110px;
  border-radius: 50%;
  background: radial-gradient(circle at 30% 30%, #f7fffb 0%, #e8f7ef 100%);
  border: 2px solid rgba(39, 174, 96, 0.3);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 2px;
  box-shadow: 0 8px 18px rgba(39, 174, 96, 0.15);
}

.done-ring-icon {
  font-size: 1.7rem;
  line-height: 1;
  font-weight: 700;
  color: #1f8f58;
}

.done-ring-text {
  font-size: 0.78rem;
  font-weight: 700;
  color: #1f8f58;
}

.btn-done-link {
  border: none;
  background: rgba(63, 111, 107, 0.12);
  color: var(--c-primary-deep);
  border-radius: 10px;
  padding: 8px 12px;
  font-size: 0.78rem;
  font-weight: 600;
  cursor: pointer;
}

.summary-card {
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-height: 206px;
}

.summary-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.summary-head h3 {
  margin: 0;
  font-size: 1rem;
  color: var(--c-ink);
}

.summary-head span {
  font-size: 0.82rem;
  color: var(--c-text-soft);
}

.summary-main {
  display: flex;
  align-items: center;
  gap: 14px;
}

.progress-ring {
  --rate: 100%;
  width: 92px;
  height: 92px;
  border-radius: 50%;
  display: grid;
  place-items: center;
  background: conic-gradient(var(--c-primary) var(--rate), #dde9e5 var(--rate));
  flex-shrink: 0;
}

.progress-inner {
  width: 72px;
  height: 72px;
  border-radius: 50%;
  background: #fff;
  display: grid;
  place-items: center;
  color: var(--c-primary-deep);
  font-size: 0.9rem;
  font-weight: 700;
}

.summary-list {
  flex: 1;
  display: grid;
  gap: 7px;
}

.summary-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  background: var(--c-surface-soft);
  border: 1px solid var(--c-line-soft);
  border-radius: 8px;
  padding: 6px 8px;
}

.summary-item span {
  font-size: 0.8rem;
  color: var(--c-text);
}

.summary-item strong {
  font-size: 0.85rem;
  color: var(--c-ink);
}

.summary-item strong.text-warn {
  color: var(--c-warn);
}

.report-inline {
  border-top: 1px dashed rgba(63, 111, 107, 0.2);
  padding-top: 10px;
  height: 112px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  gap: 4px;
  box-sizing: border-box;
}

.report-inline.empty {
  opacity: 0.82;
}

.report-title {
  margin: 0;
  font-size: 0.78rem;
  color: var(--c-text-soft);
  min-height: 18px;
  display: flex;
  align-items: center;
  line-height: 1.25;
}

.report-risk {
  margin: 6px 0 0;
  font-size: 0.86rem;
  color: #3c4d4b;
  min-height: 24px;
  display: flex;
  align-items: center;
  line-height: 1.3;
}

.quick-section {
  padding: 16px;
}

.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.section-head h3 {
  margin: 0;
  font-size: 1.02rem;
  color: #213130;
}

.quick-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
}

.quick-card {
  border: 1px solid rgba(45, 95, 93, 0.12);
  border-radius: 12px;
  background: linear-gradient(145deg, #ffffff 0%, #f8fbfb 100%);
  padding: 14px 12px;
  text-align: left;
  display: flex;
  flex-direction: column;
  gap: 4px;
  cursor: pointer;
}

.quick-icon {
  font-size: 1.2rem;
}

.quick-title {
  color: #1f2a2a;
  font-size: 0.9rem;
  font-weight: 700;
}

.quick-sub {
  color: #70807f;
  font-size: 0.78rem;
}

.records-section {
  padding: 16px;
}

.count {
  font-size: 0.8rem;
  color: #7c8a89;
}

.empty {
  text-align: center;
  padding: 26px 12px;
  color: #7c8a89;
  background: #f8fbfa;
  border-radius: 12px;
}

.records-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: grid;
  gap: 10px;
}

.record-item {
  border: 1px solid rgba(45, 95, 93, 0.12);
  border-left-width: 4px;
  border-radius: 12px;
  background: #fff;
  padding: 12px;
  display: flex;
  justify-content: space-between;
  gap: 10px;
  align-items: center;
}

.record-item.is-pending {
  border-left-color: #f39c12;
}

.record-item.is-taken {
  border-left-color: #27ae60;
}

.record-item.is-skipped {
  border-left-color: #95a5a6;
}

.record-left {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}

.slot {
  min-width: 44px;
  text-align: center;
  background: #edf4f4;
  color: #2d5f5d;
  border-radius: 8px;
  padding: 5px 7px 4px;
  font-size: 0.78rem;
  font-weight: 700;
  display: flex;
  flex-direction: column;
  align-items: center;
  line-height: 1.1;
  gap: 2px;
}

.slot-time {
  font-size: 0.62rem;
  font-weight: 600;
  color: #6e8381;
}

.medicine {
  color: #223130;
  font-size: 0.92rem;
  font-weight: 600;
}

.dosage {
  margin-top: 2px;
  color: #7b8786;
  font-size: 0.79rem;
}

.record-right {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.time-pill {
  display: inline-flex;
  align-items: center;
  padding: 4px 8px;
  border-radius: 999px;
  font-size: 0.74rem;
  font-weight: 600;
  color: #9b6730;
  background: rgba(245, 166, 35, 0.12);
  border: 1px solid rgba(245, 166, 35, 0.26);
}

.btn-action {
  border: none;
  border-radius: 8px;
  padding: 7px 10px;
  font-size: 0.82rem;
  font-weight: 600;
  cursor: pointer;
}

.btn-action:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.btn-checkin {
  background: #e7f4f2;
  color: #2d5f5d;
}

.btn-skip {
  background: #f1f3f3;
  color: #5f6d6c;
}

.badge {
  padding: 4px 10px;
  border-radius: 999px;
  background: #eef2f2;
  color: #5f6d6c;
  font-size: 0.76rem;
  font-weight: 600;
}

.report-section {
  padding: 16px;
}

.text-btn {
  border: none;
  background: transparent;
  color: var(--c-primary-deep);
  font-size: 0.82rem;
  font-weight: 600;
  cursor: pointer;
}

.report-meta {
  margin: 0;
  color: #71807f;
  font-size: 0.82rem;
}

.report-risk {
  margin: 8px 0 0;
  color: #394847;
  font-size: 0.9rem;
}

.risk-0 {
  color: #1f8f58;
  font-weight: 700;
}

.risk-1 {
  color: #d68910;
  font-weight: 700;
}

.risk-2 {
  color: #c0392b;
  font-weight: 700;
}

@media (max-width: 767px) {
  .home-page {
    padding: 0;
    padding-bottom: 90px;
    gap: 12px;
  }

  .page-header {
    padding: 16px;
    align-items: flex-start;
    flex-direction: column;
  }

  .header-meta {
    gap: 6px;
  }

  .date-chip {
    font-size: 0.8rem;
  }

  .hero-grid {
    grid-template-columns: 1fr;
    align-items: start;
  }

  .check-layout,
  .done-layout {
    grid-template-columns: 1fr;
    align-items: center;
    min-height: 0;
    height: auto;
  }

  .check-action-panel {
    padding: 12px 14px 14px;
    align-items: stretch;
    justify-content: center;
    border-left: none;
    border-top: 1px solid rgba(45, 95, 93, 0.12);
  }

  .done-visual {
    align-items: stretch;
  }

  .done-ring {
    width: 94px;
    height: 94px;
    align-self: center;
  }

  .btn-done-link {
    width: 100%;
    border-radius: 12px;
    padding: 10px;
  }

  .check-head {
    align-items: flex-start;
  }

  .check-meta-line {
    width: 100%;
    gap: 8px;
  }

  .btn-check-circle {
    width: 88px;
    height: 88px;
    align-self: center;
  }

  .circle-icon {
    font-size: 1.2rem;
  }

  .circle-text {
    font-size: 0.72rem;
  }

  .action-helper {
    text-align: center;
  }

  .btn-check-skip {
    width: auto;
    align-self: center;
    border-radius: 12px;
    padding: 10px;
  }

  .done-panel .btn-check-skip {
    width: 100%;
  }

  .summary-main {
    align-items: flex-start;
  }

  .summary-card {
    min-height: 0;
  }

  .report-inline {
    height: 108px;
  }

  .progress-ring {
    width: 84px;
    height: 84px;
  }

  .progress-inner {
    width: 66px;
    height: 66px;
  }

  .quick-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .record-item {
    align-items: flex-start;
    flex-direction: column;
  }

  .record-right {
    width: 100%;
  }

  .btn-action {
    flex: 1;
  }

  .viewing-badge {
    max-width: 162px;
    overflow: hidden;
    text-overflow: ellipsis;
  }
}

.care-mode .title {
  font-size: 1.9rem;
}

.care-mode .hero-name {
  font-size: 2rem;
}

.care-mode .summary-item span {
  font-size: 0.9rem;
}

.care-mode .medicine {
  font-size: 1.05rem;
}

.care-mode .btn-action {
  font-size: 0.95rem;
  padding: 10px 12px;
}
</style>
