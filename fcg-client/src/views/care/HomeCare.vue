<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import BaseLayout from '../../components/common/BaseLayout.vue'
import { useUserStore } from '../../stores/user'
import { createMedicineRecord, fetchTodayPlanRecords, updateMedicineRecord } from '../../utils/api'

const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)
const operating = ref(false)
const taskCursor = ref(0)
const planRecords = ref([])
const brokenImageMap = ref({})
const previewVisible = ref(false)
const previewImageUrl = ref('')
const previewImageTitle = ref('')

const slotOrder = { '早': 1, '中': 2, '晚': 3, '睡前': 4 }
const slotWindowMap = {
  '早': { start: 6 * 60, end: 10 * 60 },
  '中': { start: 10 * 60, end: 14 * 60 },
  '晚': { start: 17 * 60, end: 21 * 60 },
  '睡前': { start: 21 * 60, end: 24 * 60 }
}

const sortedRecords = computed(() => {
  return [...planRecords.value].sort((a, b) => {
    const slotA = slotOrder[a.slotName] || 99
    const slotB = slotOrder[b.slotName] || 99
    if (slotA !== slotB) return slotA - slotB
    return (a.planId || 0) - (b.planId || 0)
  })
})

const currentTask = computed(() => {
  const list = sortedRecords.value
  if (!list.length) return null
  const idx = Math.min(Math.max(taskCursor.value, 0), list.length - 1)
  return list[idx] || null
})

const currentIndex = computed(() => sortedRecords.value.length ? taskCursor.value + 1 : 0)
const totalCount = computed(() => sortedRecords.value.length)
const pendingCount = computed(() => sortedRecords.value.filter(item => item.recordStatus === 0).length)
const completedCount = computed(() => Math.max(totalCount.value - pendingCount.value, 0))
const completionPercent = computed(() => {
  if (!totalCount.value) return 0
  return Math.round((completedCount.value / totalCount.value) * 100)
})
const progressCircleStyle = computed(() => {
  const percent = Math.max(0, Math.min(completionPercent.value, 100))
  if (percent >= 100) {
    return {
      '--progress-bg': '#2f8f75'
    }
  }
  return {
    '--progress-bg': `conic-gradient(#2f8f75 0 ${percent}%, #d2dfdc ${percent}% 100%)`
  }
})
const currentTaskOverdueMinutes = computed(() => {
  if (!currentTask.value?.slotName || currentTask.value.recordStatus !== 0) return 0
  const base = slotWindowMap[currentTask.value.slotName]
  if (!base) return 0
  const now = new Date()
  const currentMinutes = now.getHours() * 60 + now.getMinutes()
  return Math.max(currentMinutes - base.end, 0)
})

const greeting = computed(() => {
  const hour = new Date().getHours()
  if (hour < 11) return '早上好'
  if (hour < 14) return '中午好'
  if (hour < 18) return '下午好'
  return '晚上好'
})

const memberName = computed(() => userStore.member?.nickname || '家人')

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

function slotRange(slot) {
  const base = slotWindowMap[slot]
  if (!base) return '--:--'
  const start = `${String(Math.floor(base.start / 60)).padStart(2, '0')}:${String(base.start % 60).padStart(2, '0')}`
  const endMinute = base.end >= 24 * 60 ? 23 * 60 + 59 : base.end
  const end = `${String(Math.floor(endMinute / 60)).padStart(2, '0')}:${String(endMinute % 60).padStart(2, '0')}`
  return `${start}-${end}`
}

function statusLabel(status) {
  if (status === 1) return '已打卡'
  if (status === 2) return '已跳过'
  return '待打卡'
}

function statusClass(status) {
  if (status === 1) return 'is-done'
  if (status === 2) return 'is-skipped'
  return 'is-pending'
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

function hasTaskImage(record) {
  if (!record?.medicineImageUrl) return false
  return !brokenImageMap.value[getTaskKey(record)]
}

function onTaskImageError(record) {
  if (!record) return
  brokenImageMap.value = {
    ...brokenImageMap.value,
    [getTaskKey(record)]: true
  }
}

function openTaskImagePreview(record) {
  if (!record?.medicineImageUrl || !hasTaskImage(record)) return
  previewImageUrl.value = record.medicineImageUrl
  previewImageTitle.value = record.medicineName || '药品图片'
  previewVisible.value = true
}

function resetCursor() {
  const list = sortedRecords.value
  if (!list.length) {
    taskCursor.value = 0
    return
  }
  const firstPending = list.findIndex(item => item.recordStatus === 0)
  taskCursor.value = firstPending >= 0 ? firstPending : 0
}

async function loadTasks() {
  const uid = userStore.member?.id || userStore.member?.userId
  if (!uid) return
  loading.value = true
  try {
    const res = await fetchTodayPlanRecords(toLocalDateISO(), uid)
    planRecords.value = res?.data?.records || []
    brokenImageMap.value = {}
    resetCursor()
  } catch (err) {
    planRecords.value = []
    ElMessage.error('加载今日任务失败')
  } finally {
    loading.value = false
  }
}

function goPrev() {
  if (!sortedRecords.value.length) return
  taskCursor.value = (taskCursor.value - 1 + sortedRecords.value.length) % sortedRecords.value.length
}

function goNext() {
  if (!sortedRecords.value.length) return
  taskCursor.value = (taskCursor.value + 1) % sortedRecords.value.length
}

async function submitStatus(record, status) {
  if (!record || record.recordStatus !== 0 || operating.value) return
  try {
    operating.value = true
    if (record.recordId) {
      await updateMedicineRecord(record.recordId, {
        status,
        ...(status === 1 ? { actualTime: toLocalDateTimeISO() } : {})
      })
    } else {
      await createMedicineRecord({
        planId: record.planId,
        userId: record.userId,
        medicineId: record.medicineId,
        scheduledDate: toLocalDateISO(),
        slotName: record.slotName,
        status,
        ...(status === 1 ? { actualTime: toLocalDateTimeISO() } : {})
      })
    }
    ElMessage.success(status === 1 ? '打卡成功' : '已标记跳过')
    await loadTasks()
  } catch (err) {
    ElMessage.error(status === 1 ? '打卡失败，请稍后重试' : '操作失败，请稍后重试')
  } finally {
    operating.value = false
  }
}

onMounted(() => {
  loadTasks()
})

const mainActionLabel = computed(() => {
  const task = currentTask.value
  if (!task) return '打卡'
  if (operating.value) return '处理中'
  if (task.recordStatus === 1) return '已打卡'
  if (task.recordStatus === 2) return '已跳过'
  return '打卡'
})

const actionTopText = computed(() => {
  const task = currentTask.value
  if (!task) return '确认药品无误后，点击打卡'
  if (task.recordStatus === 1) return '本条已完成，可切换下一条'
  if (task.recordStatus === 2) return '本条已跳过，今天仍可补打'
  if (currentTaskOverdueMinutes.value > 0) return '当前已超时，今天仍可补打'
  return '确认药品无误后，点击打卡'
})

function goQuickRecord(type) {
  router.push({ name: 'health', query: { action: 'record-vital', type: String(type) } })
}
</script>

<template>
  <BaseLayout>
    <div class="care-page">
      <section class="care-welcome">
        <div>
          <p class="care-badge">关怀模式 · 首页</p>
          <h2 class="care-title">{{ greeting }}，{{ memberName }}</h2>
          <p class="care-desc">今天还有 {{ pendingCount }} 条待打卡任务</p>
        </div>
        <div class="count-pill">{{ currentIndex }}/{{ totalCount || 0 }}</div>
      </section>

      <section class="task-row" v-loading="loading">
        <template v-if="currentTask">
          <article class="task-main-card">
            <div class="task-main-split">
              <div class="task-info-block">
                <div class="task-head-inline">
                  <p class="task-head-label">待完成任务</p>
                  <span class="task-status-chip" :class="statusClass(currentTask.recordStatus)">
                    {{ statusLabel(currentTask.recordStatus) }}
                  </span>
                </div>

                <div class="task-hero">
                  <div
                    class="task-thumb"
                    :class="{ 'has-image': hasTaskImage(currentTask), clickable: hasTaskImage(currentTask) }"
                    @click="openTaskImagePreview(currentTask)"
                  >
                    <img
                      v-if="hasTaskImage(currentTask)"
                      :src="currentTask.medicineImageUrl"
                      :alt="currentTask.medicineName || '药品图片'"
                      @error="onTaskImageError(currentTask)"
                    />
                    <span v-else>{{ (currentTask.medicineName || '药').slice(0, 1) }}</span>
                  </div>
                  <div class="task-title-wrap">
                    <h3 class="task-title">{{ currentTask.medicineName || '未命名药品' }}</h3>
                    <p class="task-sub">请按建议时间服用，完成后点击打卡</p>
                  </div>
                </div>

                <div class="task-meta-grid">
                  <div class="meta-item">
                    <span class="meta-label">🕒 时段</span>
                    <span class="meta-value">{{ currentTask.slotName || '今日' }}</span>
                  </div>
                  <div class="meta-item is-key">
                    <span class="meta-label">⏰ 建议时间</span>
                    <span class="meta-value">{{ slotRange(currentTask.slotName) }}</span>
                  </div>
                  <div class="meta-item full is-key">
                    <span class="meta-label">💊 剂量</span>
                    <span class="meta-value">{{ formatDosage(currentTask) }}</span>
                  </div>
                </div>

                <p v-if="currentTaskOverdueMinutes > 0" class="overdue-alert">
                  <span class="alert-mark"></span>
                  <span>已超时 {{ currentTaskOverdueMinutes }} 分钟，今天仍可补打</span>
                </p>
                <p v-else-if="currentTask.recordStatus === 1" class="task-hint">本条任务已完成打卡</p>
                <p v-else-if="currentTask.recordStatus === 2" class="task-hint">本条任务已标记跳过</p>
                <p v-else class="task-hint">请按建议时段完成打卡</p>
              </div>

              <div class="task-action-block">
                <p class="action-top-chip">{{ actionTopText }}</p>

                <div class="main-action-row">
                  <button
                    class="check-circle-btn"
                    :disabled="currentTask.recordStatus !== 0 || operating"
                    @click="submitStatus(currentTask, 1)"
                  >
                    <span class="circle-mark">{{ operating ? '…' : currentTask.recordStatus === 1 ? '✓' : currentTask.recordStatus === 2 ? '↷' : '✓' }}</span>
                    <span class="circle-text">{{ mainActionLabel }}</span>
                  </button>
                </div>

                <button
                  class="skip-link-btn"
                  :disabled="currentTask.recordStatus !== 0 || operating"
                  @click="submitStatus(currentTask, 2)"
                >跳过本次</button>

                <div class="switch-row">
                  <button class="switch-btn" @click="goPrev">上一条</button>
                  <span class="switch-index">{{ currentIndex }} / {{ totalCount }}</span>
                  <button class="switch-btn" @click="goNext">下一条</button>
                </div>

                <p class="action-helper-bottom"><span class="mini-icon">🧾</span>剩余 {{ pendingCount }} 项，可补打至今日 23:59</p>
              </div>
            </div>
          </article>

          <article class="task-side-card">
            <p class="side-title"><span class="mini-icon">📊</span>任务进度</p>
            <div class="side-progress-wrap">
              <div class="side-progress-circle" :style="progressCircleStyle">
                <span class="side-progress-percent">{{ completionPercent }}%</span>
                <span class="side-progress-text">已完成</span>
              </div>
            </div>
            <div class="side-grid">
              <div class="side-item">
                <span class="side-label"><span class="mini-icon">🕒</span>待完成</span>
                <span class="side-value">{{ pendingCount }}</span>
              </div>
              <div class="side-item">
                <span class="side-label"><span class="mini-icon">✅</span>已完成</span>
                <span class="side-value">{{ completedCount }}</span>
              </div>
            </div>

            <p class="side-tip"><span class="mini-icon">💬</span>支持补打，避免漏服</p>
          </article>
        </template>

        <div v-else class="empty-wrap">
          <p class="empty-title">今日暂无计划</p>
          <p class="empty-desc">请联系管理员为你添加用药计划。</p>
        </div>
      </section>

      <section class="quick-card">
        <p class="quick-title"><span class="mini-icon">⚡</span>快捷录入</p>
        <div class="quick-grid">
          <button class="quick-btn" @click="goQuickRecord(1)">❤️ 血压</button>
          <button class="quick-btn" @click="goQuickRecord(2)">🩸 血糖</button>
          <button class="quick-btn" @click="goQuickRecord(3)">⚖️ 体重</button>
        </div>
      </section>

      <el-dialog
        v-model="previewVisible"
        :title="previewImageTitle"
        width="min(92vw, 560px)"
        class="image-preview-dialog"
      >
        <div class="preview-image-wrap">
          <img :src="previewImageUrl" :alt="previewImageTitle || '药品图片'" class="preview-image" />
        </div>
      </el-dialog>
    </div>
  </BaseLayout>
</template>

<style scoped>
.care-page {
  max-width: 1200px;
  width: 100%;
  margin: 0 auto;
  padding: 0;
  display: grid;
  grid-template-columns: 1fr;
  grid-template-areas:
    "welcome"
    "task"
    "quick";
  gap: 14px;
}

.care-welcome {
  grid-area: welcome;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.96), rgba(244, 251, 248, 0.95));
  border: 1px solid rgba(45, 95, 93, 0.15);
  border-radius: 18px;
  padding: 12px 16px;
  box-shadow: 0 10px 24px rgba(45, 95, 93, 0.08);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.count-pill {
  min-width: 68px;
  height: 40px;
  border-radius: 999px;
  background: rgba(45, 95, 93, 0.1);
  color: #2d5f5d;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 1.05rem;
  font-weight: 700;
}

.task-card {
  grid-area: task;
}

.task-row {
  display: grid;
  grid-template-columns: minmax(0, 1.45fr) minmax(0, 0.95fr);
  gap: 12px;
}

.task-main-card,
.task-side-card {
  background: rgba(255, 255, 255, 0.96);
  border: 1px solid rgba(45, 95, 93, 0.12);
  border-radius: 18px;
  box-shadow: 0 10px 24px rgba(45, 95, 93, 0.08);
}

.task-main-card {
  padding: 20px;
  display: grid;
  gap: 10px;
  background:
    radial-gradient(120px 100px at 92% 18%, rgba(63, 111, 107, 0.08) 0%, transparent 80%),
    linear-gradient(145deg, #f7fcfb 0%, #eff7f4 68%, #f9fcfb 100%);
}

.task-main-split {
  display: grid;
  grid-template-columns: minmax(0, 1.6fr) minmax(0, 0.9fr);
  gap: 0;
  min-height: 260px;
  align-items: stretch;
}

.task-info-block {
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  gap: 12px;
  padding-right: 20px;
  min-height: 100%;
}

.task-action-block {
  background: transparent;
  border: none;
  border-left: 1px solid rgba(45, 95, 93, 0.16);
  border-radius: 0;
  padding: 0 16px 12px 24px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: flex-start;
  gap: 14px;
}

.task-side-card {
  padding: 16px;
  display: grid;
  align-content: space-between;
  gap: 14px;
  background:
    radial-gradient(120px 90px at 85% 14%, rgba(63, 111, 107, 0.08) 0%, transparent 78%),
    linear-gradient(150deg, rgba(255, 255, 255, 0.97), rgba(246, 251, 249, 0.95));
}

.task-main-card,
.task-side-card {
  height: 100%;
}

.task-head-inline {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.task-head-label {
  margin: 0;
  color: #335d5b;
  font-size: 1.06rem;
  font-weight: 600;
}

.task-status-chip {
  padding: 4px 10px;
  border-radius: 999px;
  font-size: 0.8rem;
  font-weight: 700;
}

.task-status-chip.is-pending {
  color: #8c6422;
  background: rgba(245, 190, 82, 0.2);
  border: 1px solid rgba(245, 190, 82, 0.25);
}

.task-status-chip.is-done {
  color: #216045;
  background: rgba(102, 202, 156, 0.2);
  border: 1px solid rgba(102, 202, 156, 0.28);
}

.task-status-chip.is-skipped {
  color: #5f6670;
  background: rgba(170, 177, 187, 0.22);
  border: 1px solid rgba(170, 177, 187, 0.3);
}

.care-badge {
  display: inline-block;
  font-size: 0.9rem;
  color: #2d5f5d;
  background: rgba(45, 95, 93, 0.1);
  border-radius: 999px;
  padding: 5px 10px;
  margin: 0 0 6px;
}

.care-title {
  margin: 0 0 4px;
  font-size: 1.58rem;
  color: #1f3f3e;
  line-height: 1.2;
}

.care-desc {
  margin: 0;
  font-size: 1.04rem;
  color: #385e5d;
}

.task-title {
  margin: 0 0 6px;
  font-size: 2.16rem;
  color: #1f3f3e;
  line-height: 1.16;
}

.task-head {
  display: flex;
  gap: 14px;
  align-items: center;
  margin-bottom: 10px;
}

.task-hero {
  display: flex;
  gap: 12px;
  align-items: center;
}

.task-thumb {
  width: 72px;
  height: 72px;
  border-radius: 14px;
  border: 1px solid rgba(45, 95, 93, 0.16);
  background: rgba(45, 95, 93, 0.08);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #2d5f5d;
  font-size: 2rem;
  font-weight: 700;
  overflow: hidden;
  flex-shrink: 0;
}

.task-thumb.clickable {
  cursor: zoom-in;
}

.task-thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.task-title-wrap {
  min-width: 0;
}

.task-sub {
  margin: 0;
  color: #5d7c7a;
  font-size: 1.06rem;
  line-height: 1.25;
}

.task-meta-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.meta-item {
  background: rgba(247, 252, 251, 0.92);
  border: 1px solid rgba(45, 95, 93, 0.12);
  border-radius: 12px;
  padding: 10px 12px;
  display: grid;
  gap: 6px;
}

.meta-item.full {
  grid-column: auto;
}

.meta-item.is-key {
  background: linear-gradient(145deg, rgba(238, 248, 244, 0.95), rgba(247, 252, 250, 0.95));
  border-color: rgba(45, 95, 93, 0.26);
}

.meta-label {
  color: #5f7a77;
  font-size: 0.86rem;
  letter-spacing: 0.02em;
}

.meta-value {
  color: #214543;
  font-size: 1.14rem;
  font-weight: 600;
  line-height: 1.2;
}

.meta-item.is-key .meta-value {
  font-size: 1.24rem;
  font-weight: 700;
}

.check-circle-btn {
  width: 136px;
  height: 136px;
  border-radius: 999px;
  border: 2px solid rgba(63, 111, 107, 0.24);
  background: radial-gradient(circle at 30% 20%, #57b896, #2f8f75 62%, #2a7d66);
  color: #fff;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
  cursor: pointer;
  box-shadow: 0 14px 30px rgba(47, 143, 117, 0.28);
  transition: transform 0.15s ease, box-shadow 0.15s ease, filter 0.15s ease;
}

.main-action-row {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0;
  width: 100%;
  margin-top: 18px;
  margin-bottom: 10px;
  padding-left: 8px;
  box-sizing: border-box;
}

.action-top-chip {
  margin: 0;
  height: 34px;
  padding: 0 14px;
  border-radius: 999px;
  border: 1px solid rgba(45, 95, 93, 0.22);
  background: rgba(255, 255, 255, 0.72);
  color: #3b6563;
  font-size: 0.82rem;
  font-weight: 700;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  white-space: nowrap;
  align-self: center;
  margin-top: 2px;
}

.side-title {
  margin: 0;
  color: #2d5f5d;
  font-size: 1.14rem;
  font-weight: 700;
  letter-spacing: 0.01em;
  padding-bottom: 8px;
  border-bottom: 1px dashed rgba(45, 95, 93, 0.2);
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.side-progress-wrap {
  display: flex;
  align-items: center;
  justify-content: center;
}

.side-progress-circle {
  --progress-bg: conic-gradient(#2f8f75 0 0%, #d2dfdc 0% 100%);
  width: 148px;
  height: 148px;
  border-radius: 999px;
  position: relative;
  background: var(--progress-bg);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  box-shadow: 0 8px 18px rgba(63, 111, 107, 0.1);
}

.side-progress-circle::before {
  content: '';
  position: absolute;
  inset: 11px;
  border-radius: 50%;
  background: radial-gradient(circle at 42% 32%, #ffffff, #f4faf8);
  box-shadow: inset 0 1px 8px rgba(45, 95, 93, 0.08);
}

.side-progress-circle > span {
  position: relative;
  z-index: 1;
}

.side-progress-percent {
  font-size: 1.74rem;
  color: #204a48;
  font-weight: 700;
  line-height: 1;
}

.side-progress-text {
  margin-top: 2px;
  font-size: 0.92rem;
  color: #5e7d7b;
}

.side-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}

.side-item {
  background: rgba(255, 255, 255, 0.82);
  border: 1px solid rgba(45, 95, 93, 0.16);
  border-radius: 12px;
  padding: 11px 8px;
  display: grid;
  gap: 2px;
  text-align: center;
  transition: transform 0.15s ease, box-shadow 0.2s ease, border-color 0.2s ease;
}

.side-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 10px 16px rgba(63, 111, 107, 0.1);
  border-color: rgba(45, 95, 93, 0.28);
}

.side-label {
  font-size: 0.78rem;
  color: #6b8583;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 3px;
}

.side-value {
  font-size: 1.2rem;
  color: #244c4a;
  font-weight: 700;
}

.side-tip {
  margin: 0;
  text-align: center;
  color: #5d7d7a;
  font-size: 0.9rem;
  font-weight: 600;
  padding-top: 8px;
  border-top: 1px dashed rgba(45, 95, 93, 0.18);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
}

.check-circle-btn:not(:disabled):hover {
  transform: translateY(-1px) scale(1.01);
  box-shadow: 0 16px 32px rgba(47, 143, 117, 0.32);
  filter: saturate(1.05);
}

.check-circle-btn:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

.circle-mark {
  font-size: 2.2rem;
  line-height: 1;
}

.circle-text {
  font-size: 1.18rem;
  font-weight: 700;
}

.skip-link-btn {
  border: 1px solid rgba(45, 95, 93, 0.24);
  background: rgba(255, 255, 255, 0.82);
  color: #3e6663;
  font-size: 0.9rem;
  border-radius: 999px;
  min-height: 38px;
  min-width: 124px;
  padding: 0 16px;
  cursor: pointer;
  transition: transform 0.15s ease, box-shadow 0.2s ease, background 0.2s ease, border-color 0.2s ease;
}

.skip-link-btn:not(:disabled):hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 16px rgba(63, 111, 107, 0.14);
  background: #ffffff;
  border-color: rgba(45, 95, 93, 0.34);
}

.skip-link-btn:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

.switch-row {
  margin-top: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  width: 100%;
  padding: 0 8px 0 14px;
  box-sizing: border-box;
}

.switch-btn {
  min-width: 84px;
  height: 40px;
  border-radius: 999px;
  border: none;
  background: rgba(63, 111, 107, 0.08);
  color: #2d5f5d;
  font-size: 0.9rem;
  font-weight: 600;
  cursor: pointer;
  transition: transform 0.15s ease, box-shadow 0.2s ease, background 0.2s ease;
}

.switch-btn:not(:disabled):hover {
  transform: translateY(-2px);
  background: rgba(63, 111, 107, 0.16);
  box-shadow: 0 8px 14px rgba(63, 111, 107, 0.12);
}

.switch-index {
  min-width: 34px;
  text-align: center;
  color: #5d7775;
  font-size: 0.78rem;
}

.action-helper-bottom {
  margin: auto 0 -9px;
  font-size: 0.86rem;
  color: #5e7c79;
  text-align: center;
  line-height: 1.35;
  font-weight: 600;
  white-space: nowrap;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
}

.task-hint {
  margin: 2px 0 0;
  font-size: 0.96rem;
  color: #597674;
}

.overdue-alert {
  margin: 0;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 6px 10px;
  border-radius: 10px;
  background: rgba(206, 90, 75, 0.09);
  color: #ce5a4b;
  font-size: 0.9rem;
  font-weight: 600;
}

.alert-mark {
  width: 3px;
  height: 16px;
  border-radius: 99px;
  background: #ce5a4b;
  flex-shrink: 0;
}

.empty-wrap {
  padding: 26px 6px;
  text-align: center;
}

.empty-title {
  margin: 0 0 8px;
  font-size: 1.5rem;
  color: #2d5f5d;
}

.empty-desc {
  margin: 0;
  font-size: 1.05rem;
  color: #6b8280;
}

.quick-card {
  grid-area: quick;
  background: linear-gradient(145deg, rgba(255, 255, 255, 0.96), rgba(246, 252, 250, 0.95));
  border: 1px solid rgba(45, 95, 93, 0.14);
  border-radius: 16px;
  padding: 14px 16px;
}

.quick-title {
  margin: 0 0 10px;
  color: #2f5f5d;
  font-size: 1.02rem;
  font-weight: 700;
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.mini-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  line-height: 1;
}

.quick-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.quick-btn {
  height: 50px;
  border-radius: 12px;
  border: 1px solid rgba(45, 95, 93, 0.2);
  background: linear-gradient(145deg, #f8fcfb, #eef8f5);
  color: #2d5f5d;
  font-size: 1rem;
  font-weight: 600;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: flex-start;
  gap: 8px;
  padding: 0 14px;
  transition: transform 0.15s ease, box-shadow 0.2s ease, border-color 0.2s ease, background 0.2s ease;
}

.quick-btn:hover {
  transform: translateY(-2px);
  background: linear-gradient(145deg, #ffffff, #eef9f6);
  border-color: rgba(45, 95, 93, 0.34);
  box-shadow: 0 10px 18px rgba(63, 111, 107, 0.12);
}

.preview-image-wrap {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 240px;
}

.preview-image {
  max-width: 100%;
  max-height: min(68vh, 560px);
  object-fit: contain;
  border-radius: 12px;
}

@media (min-width: 1024px) {
  .task-main-card,
  .task-side-card {
    min-height: 420px;
  }
}

@media (max-width: 768px) {
  .care-page {
    gap: 12px;
  }

  .task-row {
    grid-template-columns: 1fr;
  }

  .care-title {
    font-size: 1.6rem;
  }

  .care-desc {
    font-size: 1.08rem;
  }

  .care-welcome {
    border-radius: 14px;
    padding: 14px;
  }

  .count-pill {
    min-width: 56px;
    height: 36px;
    font-size: 0.92rem;
  }

  .task-main-card,
  .task-side-card {
    border-radius: 16px;
    padding: 14px;
  }

  .task-main-split {
    grid-template-columns: 1fr;
    gap: 10px;
  }

  .main-action-row {
    justify-content: center;
    gap: 8px;
  }

  .task-info-block {
    padding-right: 0;
  }

  .task-action-block {
    border-left: 0;
    border-top: 1px solid rgba(45, 95, 93, 0.18);
    padding: 14px 0 0;
    gap: 12px;
  }

  .action-helper-bottom {
    white-space: normal;
    margin: auto 0 0;
  }

  .main-action-row,
  .switch-row {
    padding-left: 0;
  }

  .action-top-chip {
    height: 32px;
    padding: 0 12px;
    font-size: 0.78rem;
  }

  .task-title {
    font-size: 1.55rem;
  }

  .task-sub {
    font-size: 0.95rem;
  }

  .task-thumb {
    width: 64px;
    height: 64px;
    font-size: 1.6rem;
    border-radius: 14px;
  }

  .check-circle-btn {
    width: 122px;
    height: 122px;
  }

  .circle-mark {
    font-size: 1.9rem;
  }

  .circle-text {
    font-size: 1rem;
  }

  .task-meta-grid {
    grid-template-columns: 1fr;
  }

  .meta-item.full {
    grid-column: auto;
  }

  .quick-grid {
    grid-template-columns: 1fr;
  }

  .quick-btn {
    height: 48px;
    padding: 0 12px;
    font-size: 0.98rem;
  }

  .side-grid {
    grid-template-columns: 1fr;
  }

  .side-progress-circle {
    width: 116px;
    height: 116px;
    border-width: 8px;
  }
}
</style>
