<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import { verifyAdmin, fetchTodayPlanRecords, updateMedicineRecord, fetchTodayVitals } from '../utils/api'
import BaseLayout from '../components/common/BaseLayout.vue'
import { ElMessage } from 'element-plus'

const userStore = useUserStore()
const router = useRouter()

// ── 管理员验证弹窗 ──
const showAdminModal = ref(false)
const adminPassword = ref('')
const adminError = ref('')
const adminLoading = ref(false)

const confirmAdmin = async () => {
  adminError.value = ''
  if (!adminPassword.value) { adminError.value = '请输入密码'; return }
  try {
    adminLoading.value = true
    await verifyAdmin(adminPassword.value)
    showAdminModal.value = false
    adminPassword.value = ''
    router.push({ name: 'admin' })
  } catch (err) {
    adminError.value = err?.message || '密码错误'
  } finally {
    adminLoading.value = false
  }
}

const closeAdminModal = () => {
  showAdminModal.value = false
  adminPassword.value = ''
  adminError.value = ''
}

// ── 时间问候语 ──
const greeting = computed(() => {
  const h = new Date().getHours()
  if (h < 6)  return '夜深了'
  if (h < 11) return '早上好'
  if (h < 14) return '中午好'
  if (h < 18) return '下午好'
  return '晚上好'
})

// ── 今日日期 ──
const todayStr = computed(() => {
  const d = new Date()
  return `${d.getMonth() + 1}月${d.getDate()}日 · ${['日','一','二','三','四','五','六'][d.getDay()]}曜日`
})

const todayISO = computed(() => new Date().toISOString().slice(0, 10))

// ── 今日用药数据 ──
const planRecords = ref([])   // MedicinePlanRecordVO[]
const loadingRecords = ref(false)
const vitalRecorded = ref(false)  // 今日是否有体征

async function loadTodayData() {
  loadingRecords.value = true
  try {
    const memberId = userStore.member?.userId
    const [recordsRes, vitalsRes] = await Promise.allSettled([
      fetchTodayPlanRecords(todayISO.value, memberId),
      fetchTodayVitals(memberId),
    ])
    if (recordsRes.status === 'fulfilled') {
      planRecords.value = recordsRes.value?.data?.records ?? []
    }
    if (vitalsRes.status === 'fulfilled') {
      vitalRecorded.value = (vitalsRes.value?.data?.total ?? 0) > 0
    }
  } finally {
    loadingRecords.value = false
  }
}

onMounted(loadTodayData)

// ── 状态派生 ──
const pendingRecords = computed(() => planRecords.value.filter(r => r.recordStatus === 0))
const doneRecords    = computed(() => planRecords.value.filter(r => r.recordStatus === 1))

// 下一条待打卡（按计划时间最近）
const nextPending = computed(() => {
  const now = new Date()
  const nowMin = now.getHours() * 60 + now.getMinutes()
  // 优先选时间已到的
  const passed = pendingRecords.value.filter(r => {
    if (!r.scheduledTime) return true
    const [h, m] = r.scheduledTime.split(':').map(Number)
    return h * 60 + m <= nowMin
  })
  return (passed[0] ?? pendingRecords.value[0]) ?? null
})

// ── 打卡 ──
async function checkin(record) {
  try {
    const now = new Date()
    const actualTime = now.toISOString().replace('T', ' ').slice(0, 19)
    await updateMedicineRecord(record.recordId, { status: 1, actualTime })
    record.recordStatus = 1  // 乐观更新
    ElMessage({ message: '打卡成功 ✓', type: 'success', duration: 1500 })
  } catch (e) {
    ElMessage({ message: '打卡失败，请重试', type: 'error', duration: 2000 })
  }
}

// ── 跳过 ──
async function skipRecord(record) {
  try {
    await updateMedicineRecord(record.recordId, { status: 2 })
    record.recordStatus = 2  // 乐观更新
  } catch (e) {
    ElMessage({ message: '操作失败', type: 'error', duration: 2000 })
  }
}

// ── 时间线（从计划记录生成，最多5条） ──
const timeline = computed(() => {
  const sorted = [...planRecords.value].sort((a, b) => {
    const ta = a.scheduledTime ?? '00:00'
    const tb = b.scheduledTime ?? '00:00'
    return ta.localeCompare(tb)
  })
  return sorted.slice(0, 5).map(r => {
    let status = 'upcoming'
    let desc = r.scheduledTime ? `计划 ${r.scheduledTime.slice(0, 5)}` : '今日'
    if (r.recordStatus === 1) { status = 'done'; desc = '已打卡' }
    else if (r.recordStatus === 2) { status = 'skipped'; desc = '已跳过' }
    else {
      // 判断是否超时未服
      const now = new Date()
      const nowMin = now.getHours() * 60 + now.getMinutes()
      if (r.scheduledTime) {
        const [h, m] = r.scheduledTime.split(':').map(Number)
        if (h * 60 + m <= nowMin) { status = 'pending'; desc = '待打卡' }
      }
    }
    return {
      time: r.scheduledTime ? r.scheduledTime.slice(0, 5) : '--:--',
      label: r.medicineName ?? '用药',
      status,
      desc,
    }
  })
})

// ── 模块卡 ──
const moduleCards = [
  {
    key: 'medicine',
    title: '药品',
    icon: 'medicine',
    color: '#2d5f5d',
    items: ['药箱库存', '用药计划', '服药记录'],
    action: () => router.push({ name: 'medicine' }),
  },
  {
    key: 'health',
    title: '健康',
    icon: 'health',
    color: '#3a7d6b',
    items: ['体征录入', '近一周趋势', '健康周报'],
    action: () => router.push({ name: 'health' }),
  },
  {
    key: 'family',
    title: '家庭',
    icon: 'family',
    color: '#5a7a9e',
    items: ['成员列表', '关怀模式', '家庭设置'],
    action: () => router.push({ name: 'family' }),
  },
]

// ── 状态标签 ──
function statusLabel(status) {
  if (status === 1) return '已服'
  if (status === 2) return '跳过'
  return '待服'
}
function statusClass(status) {
  if (status === 1) return 'med-done'
  if (status === 2) return 'med-skip'
  return 'med-pending'
}
</script>

<template>
  <BaseLayout>
    <!-- ── 装饰背景（与 SelectMember 同风格） ── -->
    <div class="bg-layer bg-a" aria-hidden="true"></div>
    <div class="bg-layer bg-b" aria-hidden="true"></div>

    <div class="dashboard">
      <!-- ══════════════════════════════════════════
           左列
      ═══════════════════════════════════════════ -->
      <div class="col-main">

        <!-- ① 主任务卡 -->
        <div class="card task-card">
          <div class="task-header">
            <div>
              <div class="task-greeting">{{ greeting }}，{{ userStore.member?.nickname || '朋友' }} 👋</div>
              <div class="task-date">{{ todayStr }}</div>
            </div>
            <div v-if="pendingRecords.length > 0" class="task-badge">待完成 {{ pendingRecords.length }}</div>
            <div v-else class="task-badge task-badge-done">全部完成 ✓</div>
          </div>

          <template v-if="nextPending">
            <div class="task-body">
              <div class="task-icon-wrap">
                <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M19 14c1.49-1.46 3-3.21 3-5.5A5.5 5.5 0 0 0 16.5 3c-1.76 0-3 .5-4.5 2-1.5-1.5-2.74-2-4.5-2A5.5 5.5 0 0 0 2 8.5c0 2.3 1.5 4.05 3 5.5l7 7Z"/>
                </svg>
              </div>
              <div>
                <div class="task-name">{{ nextPending.medicineName ?? '用药提醒' }}</div>
                <div class="task-desc">
                  {{ nextPending.scheduledTime ? nextPending.scheduledTime.slice(0, 5) + ' · ' : '' }}{{ nextPending.planDosage ?? '' }}
                </div>
              </div>
            </div>
            <div class="task-actions">
              <el-button type="primary" class="task-btn-primary" @click="checkin(nextPending)">立即打卡</el-button>
              <el-button class="task-btn-ghost" @click="skipRecord(nextPending)">跳过</el-button>
            </div>
          </template>

          <template v-else-if="!loadingRecords">
            <div class="task-all-done">
              <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="#2d5f5d" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/>
                <polyline points="22 4 12 14.01 9 11.01"/>
              </svg>
              <span>今日用药全部完成，做得很好！</span>
            </div>
          </template>

          <template v-else>
            <div class="task-loading">加载中…</div>
          </template>
        </div>

        <!-- ② 今日用药卡（替换原快捷操作） -->
        <div class="card">
          <div class="card-title-row">
            <span class="card-title">今日用药</span>
            <span class="card-count">{{ planRecords.length }} 条</span>
          </div>

          <div v-if="loadingRecords" class="med-loading">加载中…</div>

          <div v-else-if="planRecords.length === 0" class="med-empty">
            <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="#ccc" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
              <circle cx="12" cy="12" r="10"/>
              <path d="M8 12h8M12 8v8"/>
            </svg>
            <span>今日暂无用药计划</span>
          </div>

          <ul v-else class="med-list">
            <li
              v-for="r in planRecords"
              :key="r.recordId"
              class="med-item"
              :class="statusClass(r.recordStatus)"
            >
              <div class="med-left">
                <!-- 状态圆点 -->
                <span class="med-dot"></span>
                <div class="med-info">
                  <div class="med-name">{{ r.medicineName }}</div>
                  <div class="med-meta">
                    <span v-if="r.scheduledTime">{{ r.scheduledTime.slice(0, 5) }}</span>
                    <span v-if="r.planDosage"> · {{ r.planDosage }}</span>
                  </div>
                </div>
              </div>
              <div class="med-right">
                <span class="med-status-badge">{{ statusLabel(r.recordStatus) }}</span>
                <button
                  v-if="r.recordStatus === 0"
                  class="med-checkin-btn"
                  @click="checkin(r)"
                >打卡</button>
                <button
                  v-if="r.recordStatus === 0"
                  class="med-skip-btn"
                  @click="skipRecord(r)"
                >跳过</button>
              </div>
            </li>
          </ul>
        </div>

        <!-- ③ 模块分区卡 -->
        <div class="card">
          <div class="card-title">功能模块</div>
          <div class="module-grid">
            <div
              v-for="mod in moduleCards"
              :key="mod.key"
              class="module-card"
              :style="{ '--m-color': mod.color }"
              @click="mod.action"
            >
              <div class="module-header">
                <span class="module-icon">
                  <svg v-if="mod.icon === 'medicine'" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <path d="M19 14c1.49-1.46 3-3.21 3-5.5A5.5 5.5 0 0 0 16.5 3c-1.76 0-3 .5-4.5 2-1.5-1.5-2.74-2-4.5-2A5.5 5.5 0 0 0 2 8.5c0 2.3 1.5 4.05 3 5.5l7 7Z"/>
                  </svg>
                  <svg v-else-if="mod.icon === 'health'" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <polyline points="22 12 18 12 15 21 9 3 6 12 2 12"/>
                  </svg>
                  <svg v-else-if="mod.icon === 'family'" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/>
                    <circle cx="9" cy="7" r="4"/>
                    <path d="M23 21v-2a4 4 0 0 0-3-3.87"/>
                    <path d="M16 3.13a4 4 0 0 1 0 7.75"/>
                  </svg>
                </span>
                <span class="module-title">{{ mod.title }}</span>
                <svg class="module-arrow" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round">
                  <polyline points="9 18 15 12 9 6"/>
                </svg>
              </div>
              <ul class="module-items">
                <li v-for="item in mod.items" :key="item">{{ item }}</li>
              </ul>
            </div>
          </div>
        </div>
      </div>

      <!-- ══════════════════════════════════════════
           右列
      ═══════════════════════════════════════════ -->
      <div class="col-side">

        <!-- ④ 今日概览卡 -->
        <div class="card">
          <div class="card-title">今日概览</div>
          <div class="overview-grid">
            <div class="overview-item">
              <div class="overview-val">{{ pendingRecords.length }}</div>
              <div class="overview-key">待服药</div>
            </div>
            <div class="overview-item overview-done">
              <div class="overview-val">{{ doneRecords.length }}</div>
              <div class="overview-key">已打卡</div>
            </div>
            <div class="overview-item overview-vital">
              <div class="overview-val" :class="vitalRecorded ? '' : 'overview-val-no'">
                {{ vitalRecorded ? '✓' : '—' }}
              </div>
              <div class="overview-key">体征已录</div>
            </div>
          </div>
        </div>

        <!-- ⑤ 时间线卡 -->
        <div class="card timeline-card">
          <div class="card-title">今日任务</div>
          <div v-if="timeline.length === 0 && !loadingRecords" class="tl-empty">暂无任务</div>
          <div class="timeline">
            <div
              v-for="(item, idx) in timeline"
              :key="idx"
              class="timeline-item"
              :class="'tl-' + item.status"
            >
              <div class="tl-line">
                <div class="tl-dot"></div>
                <div v-if="idx < timeline.length - 1" class="tl-track"></div>
              </div>
              <div class="tl-content">
                <div class="tl-top">
                  <span class="tl-label">{{ item.label }}</span>
                  <span class="tl-time">{{ item.time }}</span>
                </div>
                <div class="tl-desc">{{ item.desc }}</div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- ── 管理员密码验证弹窗 ── -->
    <el-dialog
      v-model="showAdminModal"
      title="验证管理员身份"
      width="360px"
      :close-on-click-modal="true"
      @close="closeAdminModal"
    >
      <p class="dialog-sub">请输入家庭账号密码</p>
      <el-input
        v-model="adminPassword"
        type="password"
        placeholder="家庭账号密码"
        show-password
        @keyup.enter="confirmAdmin"
      />
      <p v-if="adminError" class="dialog-error">{{ adminError }}</p>
      <template #footer>
        <el-button @click="closeAdminModal">取消</el-button>
        <el-button type="primary" :loading="adminLoading" @click="confirmAdmin">确认</el-button>
      </template>
    </el-dialog>
  </BaseLayout>
</template>

<style scoped>
/* ── 装饰背景层 ── */
.bg-layer {
  position: fixed;
  z-index: 0;
  pointer-events: none;
}
.bg-a {
  width: min(44vw, 560px); height: min(30vw, 360px);
  left: -12%; top: -8%;
  clip-path: polygon(0 18%, 76% 0, 100% 44%, 60% 100%, 0 80%);
  background: linear-gradient(138deg, rgba(45,95,93,0.07) 0%, rgba(255,255,255,0.10) 100%);
  opacity: 0.8;
}
.bg-b {
  width: min(48vw, 620px); height: min(36vw, 430px);
  right: -16%; bottom: -18%;
  clip-path: polygon(8% 0, 100% 24%, 92% 100%, 0 74%);
  background: linear-gradient(142deg, rgba(45,95,93,0.08) 0%, rgba(246,228,201,0.18) 100%);
  opacity: 0.6;
}

/* ── 主 Dashboard 布局（两列） ── */
.dashboard {
  position: relative;
  z-index: 1;
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 20px;
  align-items: start;
}

/* ── 卡片基础 ── */
.card {
  background: rgba(255, 255, 255, 0.92);
  border: 1px solid rgba(255, 255, 255, 0.8);
  border-radius: 20px;
  padding: 22px 22px 20px;
  box-shadow: 0 4px 24px rgba(45, 95, 93, 0.08), 0 1px 4px rgba(45, 95, 93, 0.05);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
}

.card-title {
  font-size: 0.82rem;
  font-weight: 700;
  color: #888;
  letter-spacing: 0.06em;
  text-transform: uppercase;
  margin-bottom: 16px;
}

.card-title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
}
.card-title-row .card-title {
  margin-bottom: 0;
}
.card-count {
  font-size: 0.75rem;
  color: #bbb;
}

/* ── 左列 ── */
.col-main {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

/* ── 右列 ── */
.col-side {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

/* ── ① 主任务卡 ── */
.task-card {
  background: linear-gradient(145deg, rgba(45,95,93,0.06) 0%, rgba(255,255,255,0.94) 60%);
  border-color: rgba(45, 95, 93, 0.15);
}

.task-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 18px;
}

.task-greeting {
  font-size: 1.2rem;
  font-weight: 800;
  color: #1a1a1a;
  letter-spacing: -0.01em;
  margin-bottom: 4px;
}

.task-date {
  font-size: 0.8rem;
  color: #999;
}

.task-badge {
  padding: 4px 10px;
  border-radius: 999px;
  background: rgba(245, 166, 35, 0.12);
  color: #c87d00;
  font-size: 0.75rem;
  font-weight: 600;
  flex-shrink: 0;
}
.task-badge-done {
  background: rgba(45, 95, 93, 0.1);
  color: #2d5f5d;
}

.task-body {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 14px 16px;
  background: rgba(45, 95, 93, 0.05);
  border-radius: 14px;
  margin-bottom: 16px;
}

.task-icon-wrap {
  width: 48px;
  height: 48px;
  border-radius: 14px;
  background: rgba(45, 95, 93, 0.1);
  display: grid;
  place-items: center;
  color: #2d5f5d;
  flex-shrink: 0;
}

.task-name {
  font-size: 1rem;
  font-weight: 700;
  color: #1a1a1a;
  margin-bottom: 4px;
}

.task-desc {
  font-size: 0.82rem;
  color: #888;
}

.task-actions {
  display: flex;
  gap: 10px;
}

.task-btn-primary {
  --el-button-bg-color: #2d5f5d !important;
  --el-button-border-color: #2d5f5d !important;
  --el-button-hover-bg-color: #3d7370 !important;
  --el-button-hover-border-color: #3d7370 !important;
  font-family: inherit !important;
}

.task-btn-ghost {
  font-family: inherit !important;
}

.task-all-done {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  background: rgba(45, 95, 93, 0.05);
  border-radius: 14px;
  color: #2d5f5d;
  font-size: 0.9rem;
  font-weight: 500;
}

.task-loading {
  padding: 14px 0;
  color: #bbb;
  font-size: 0.85rem;
}

/* ── ② 今日用药 ── */
.med-loading,
.med-empty {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #bbb;
  font-size: 0.85rem;
  padding: 8px 0 4px;
}

.med-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.med-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 11px 14px;
  border-radius: 13px;
  background: rgba(45, 95, 93, 0.04);
  border: 1.5px solid rgba(0, 0, 0, 0.05);
  transition: border-color 0.15s;
}

.med-item.med-done {
  background: rgba(45, 95, 93, 0.04);
  opacity: 0.7;
}
.med-item.med-skip {
  background: rgba(0, 0, 0, 0.02);
  opacity: 0.55;
}

.med-left {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.med-dot {
  width: 9px;
  height: 9px;
  border-radius: 50%;
  flex-shrink: 0;
  background: #f5a623;
}
.med-done .med-dot { background: #2d5f5d; }
.med-skip .med-dot { background: #ccc; }

.med-info {
  min-width: 0;
}

.med-name {
  font-size: 0.9rem;
  font-weight: 600;
  color: #1a1a1a;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.med-done .med-name {
  text-decoration: line-through;
  color: #aaa;
}
.med-skip .med-name {
  text-decoration: line-through;
  color: #bbb;
}

.med-meta {
  font-size: 0.75rem;
  color: #aaa;
  margin-top: 1px;
}

.med-right {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-shrink: 0;
}

.med-status-badge {
  font-size: 0.72rem;
  color: #aaa;
  min-width: 28px;
  text-align: right;
}
.med-pending .med-status-badge { color: #f5a623; }
.med-done .med-status-badge    { color: #2d5f5d; }

.med-checkin-btn,
.med-skip-btn {
  padding: 4px 10px;
  border-radius: 8px;
  font-size: 0.78rem;
  font-weight: 600;
  cursor: pointer;
  border: 1.5px solid;
  font-family: inherit;
  transition: background 0.12s, transform 0.1s;
}
.med-checkin-btn {
  background: #2d5f5d;
  color: #fff;
  border-color: #2d5f5d;
}
.med-checkin-btn:hover {
  background: #3d7370;
  transform: translateY(-1px);
}
.med-skip-btn {
  background: transparent;
  color: #bbb;
  border-color: #e0e0e0;
}
.med-skip-btn:hover {
  border-color: #ccc;
  color: #999;
}

/* ── ③ 模块卡 ── */
.module-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
}

.module-card {
  border: 1.5px solid rgba(0, 0, 0, 0.06);
  border-radius: 16px;
  padding: 14px 14px 12px;
  cursor: pointer;
  transition: box-shadow 0.15s, transform 0.15s, border-color 0.15s;
  background: rgba(255, 255, 255, 0.6);
}

.module-card:hover {
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.08);
  transform: translateY(-2px);
  border-color: var(--m-color);
}

.module-header {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 10px;
}

.module-icon {
  display: flex;
  align-items: center;
  color: var(--m-color);
}

.module-title {
  font-size: 0.9rem;
  font-weight: 700;
  color: #1a1a1a;
  flex: 1;
}

.module-arrow {
  color: #ccc;
  transition: color 0.15s;
}

.module-card:hover .module-arrow {
  color: var(--m-color);
}

.module-items {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.module-items li {
  font-size: 0.75rem;
  color: #999;
  padding-left: 12px;
  position: relative;
}

.module-items li::before {
  content: '·';
  position: absolute;
  left: 3px;
  color: var(--m-color);
}

/* ── ④ 今日概览 ── */
.overview-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
}

.overview-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  padding: 14px 8px;
  border-radius: 14px;
  background: rgba(45, 95, 93, 0.06);
}

.overview-done {
  background: rgba(58, 125, 107, 0.07);
}

.overview-vital {
  background: rgba(90, 122, 158, 0.07);
}

.overview-val {
  font-size: 1.5rem;
  font-weight: 800;
  color: #2d5f5d;
  line-height: 1;
}
.overview-val-no {
  color: #ccc;
}

.overview-done .overview-val {
  color: #3a7d6b;
}

.overview-vital .overview-val {
  color: #5a7a9e;
  font-size: 1.2rem;
}

.overview-key {
  font-size: 0.72rem;
  color: #999;
  text-align: center;
}

/* ── ⑤ 时间线 ── */
.timeline-card {
  flex: 1;
}

.tl-empty {
  font-size: 0.82rem;
  color: #ccc;
  padding: 4px 0;
}

.timeline {
  display: flex;
  flex-direction: column;
  gap: 0;
}

.timeline-item {
  display: flex;
  gap: 12px;
}

.tl-line {
  display: flex;
  flex-direction: column;
  align-items: center;
  flex-shrink: 0;
  width: 14px;
}

.tl-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: #ccc;
  flex-shrink: 0;
  margin-top: 4px;
  transition: background 0.15s;
}

.tl-done .tl-dot    { background: #2d5f5d; }
.tl-pending .tl-dot { background: #f5a623; box-shadow: 0 0 0 3px rgba(245,166,35,0.2); }
.tl-skipped .tl-dot { background: #ccc; }
.tl-upcoming .tl-dot {
  background: #ddd;
  border: 2px solid #ccc;
  width: 8px;
  height: 8px;
}

.tl-track {
  flex: 1;
  width: 1.5px;
  background: rgba(0, 0, 0, 0.08);
  margin: 4px 0;
  min-height: 16px;
}

.tl-content {
  padding-bottom: 18px;
  flex: 1;
  min-width: 0;
}

.timeline-item:last-child .tl-content {
  padding-bottom: 0;
}

.tl-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 2px;
}

.tl-label {
  font-size: 0.87rem;
  font-weight: 600;
  color: #1a1a1a;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.tl-done .tl-label {
  color: #888;
  text-decoration: line-through;
  text-decoration-color: #ccc;
}
.tl-skipped .tl-label {
  color: #bbb;
  text-decoration: line-through;
  text-decoration-color: #ddd;
}

.tl-time {
  font-size: 0.75rem;
  color: #bbb;
  flex-shrink: 0;
}

.tl-desc {
  font-size: 0.75rem;
  color: #bbb;
}
.tl-pending .tl-desc {
  color: #f5a623;
  font-weight: 500;
}

/* ── 弹窗辅助 ── */
.dialog-sub {
  margin: 0 0 14px;
  color: #888;
  font-size: 0.88rem;
}

.dialog-error {
  margin: 10px 0 0;
  color: #b42318;
  font-size: 0.85rem;
}

/* ── 响应式 ── */
@media (max-width: 960px) {
  .module-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 767px) {
  .dashboard {
    grid-template-columns: 1fr;
  }

  .col-side {
    order: -1;
  }

  .overview-grid {
    grid-template-columns: repeat(3, 1fr);
  }

  .module-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 480px) {
  .card {
    padding: 16px 16px 14px;
    border-radius: 16px;
  }

  .module-grid {
    grid-template-columns: 1fr;
  }

  .task-greeting {
    font-size: 1.05rem;
  }
}
</style>
