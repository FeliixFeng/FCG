<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../../stores/user'
import { fetchAdminOverview } from '../../utils/api'

const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const initialized = ref(false)
const overview = ref({
  memberCount: 0,
  medicineCount: 0,
  todayPendingCount: 0,
  todayDoneCount: 0,
  todaySkippedCount: 0,
  lowStockCount: 0
})

const memberCount = computed(() => Number(overview.value.memberCount || 0))
const medicineCount = computed(() => Number(overview.value.medicineCount || 0))
const lowStockCount = computed(() => Number(overview.value.lowStockCount || 0))
const pendingCount = computed(() => Number(overview.value.todayPendingCount || 0))
const completedCount = computed(() => Number(overview.value.todayDoneCount || 0))
const skippedCount = computed(() => Number(overview.value.todaySkippedCount || 0))
const totalTaskCount = computed(() => pendingCount.value + completedCount.value + skippedCount.value)
const completionRate = computed(() => {
  if (!totalTaskCount.value) return 0
  return Math.round((completedCount.value * 100) / totalTaskCount.value)
})
const ringPercent = computed(() => {
  if (!initialized.value) return 0
  if (completionRate.value <= 0) return 0
  return Math.max(3, completionRate.value)
})
const donePercent = computed(() => {
  if (!totalTaskCount.value) return 0
  return Math.round((completedCount.value * 100) / totalTaskCount.value)
})
const pendingPercent = computed(() => {
  if (!totalTaskCount.value) return 0
  return Math.round((pendingCount.value * 100) / totalTaskCount.value)
})
const skippedPercent = computed(() => {
  if (!totalTaskCount.value) return 0
  return Math.max(0, 100 - donePercent.value - pendingPercent.value)
})
const statusTone = computed(() => {
  if (completionRate.value >= 80) return 'good'
  if (completionRate.value >= 50) return 'mid'
  return 'low'
})
const statusText = computed(() => {
  if (!initialized.value) return '加载中'
  if (completionRate.value >= 80) return '整体状态良好'
  if (completionRate.value >= 50) return '整体状态平稳'
  return '待处理任务较多'
})

const loadData = async () => {
  loading.value = true
  try {
    const res = await fetchAdminOverview()
    overview.value = {
      ...overview.value,
      ...(res?.data || {})
    }
  } catch (err) {
    console.error('加载数据失败', err)
  } finally {
    loading.value = false
    initialized.value = true
  }
}

const goToMembers = () => router.push({ name: 'admin-members' })
const goToPlans = () => router.push({ name: 'admin-plans' })
const goToSettings = () => router.push({ name: 'admin-settings' })
const backToUserHome = () => router.push({ name: 'home' })

onMounted(() => {
  loadData()
})
</script>

<template>
  <div class="admin-dashboard">
    <header class="page-header card">
      <div class="header-main">
        <div>
          <h2>管理中心</h2>
          <p class="page-desc">家庭：{{ userStore.family?.familyName || '未命名家庭' }}</p>
        </div>
        <span class="header-chip">管理员视图</span>
      </div>
      <p v-if="loading" class="loading-tip">数据加载中...</p>

      <div class="stats-row">
        <button class="stat-card" @click="goToMembers">
          <span class="stat-head">👥 家庭成员</span>
          <strong class="stat-value">{{ initialized ? memberCount : '--' }}</strong>
        </button>

        <button class="stat-card blue" @click="goToPlans">
          <span class="stat-head">💊 药品总数</span>
          <strong class="stat-value">{{ initialized ? medicineCount : '--' }}</strong>
        </button>

        <button class="stat-card green" @click="goToPlans">
          <span class="stat-head">✅ 今日完成</span>
          <strong class="stat-value">{{ initialized ? `${completedCount}/${totalTaskCount}` : '--/--' }}</strong>
        </button>

        <button class="stat-card warn" @click="goToPlans">
          <span class="stat-head">⚠️ 库存紧张</span>
          <strong class="stat-value">{{ initialized ? lowStockCount : '--' }}</strong>
        </button>
      </div>
    </header>

    <section class="card actions-card">
      <div class="card-head">
        <h3>快捷操作</h3>
        <span>点击进入对应模块</span>
      </div>

      <div class="action-grid">
        <button class="action-item" @click="goToMembers">
          <span class="action-icon">👥</span>
          <span class="action-text">成员管理</span>
        </button>
        <button class="action-item" @click="goToPlans">
          <span class="action-icon">🗓️</span>
          <span class="action-text">计划总览</span>
        </button>
        <button class="action-item" @click="goToSettings">
          <span class="action-icon">⚙️</span>
          <span class="action-text">系统设置</span>
        </button>
        <button class="action-item" @click="backToUserHome">
          <span class="action-icon">↩</span>
          <span class="action-text">返回用户页</span>
        </button>
      </div>
    </section>

    <section class="card deco-card">
      <div class="deco-left" :class="statusTone">
        <div class="deco-title-row">
          <h3>今日进度</h3>
          <span class="deco-badge">{{ statusText }}</span>
        </div>
        <div class="deco-progress">
          <div class="progress-ring" :style="{ '--ring': `${ringPercent}%` }">
            <span class="progress-center">
              <span class="progress-value">{{ initialized ? `${completionRate}%` : '--' }}</span>
            </span>
          </div>
          <div class="progress-meta">
            <p>完成率（已完成 / 全部任务）</p>
            <p>当前任务总数：{{ initialized ? totalTaskCount : '--' }}</p>
          </div>
        </div>

        <div class="progress-split">
          <div class="split-track">
            <span class="split-part done" :style="{ width: `${initialized ? donePercent : 0}%` }"></span>
            <span class="split-part pending" :style="{ width: `${initialized ? pendingPercent : 0}%` }"></span>
            <span class="split-part skipped" :style="{ width: `${initialized ? skippedPercent : 0}%` }"></span>
          </div>
          <div class="split-legend">
            <span>已完成 {{ initialized ? completedCount : '--' }}</span>
            <span>待处理 {{ initialized ? pendingCount : '--' }}</span>
            <span>已跳过 {{ initialized ? skippedCount : '--' }}</span>
          </div>
        </div>
      </div>

      <div class="deco-right">
        <h4>今日关注</h4>
        <div class="focus-row">
          <div class="focus-pill">待处理 {{ initialized ? pendingCount : '--' }}</div>
          <div class="focus-pill">库存紧张 {{ initialized ? lowStockCount : '--' }}</div>
          <div class="focus-pill">已完成 {{ initialized ? completedCount : '--' }}</div>
        </div>
        <ul class="tips-list">
          <li>优先处理“待处理”任务，再检查跳过记录。</li>
          <li>库存紧张项建议当天完成补货登记。</li>
          <li>成员变更后同步检查计划时段是否合理。</li>
        </ul>
      </div>
    </section>
  </div>
</template>

<style scoped>
.admin-dashboard {
  width: 100%;
  display: grid;
  gap: 14px;
}

.card {
  background: rgba(255, 255, 255, 0.96);
  border: 1px solid rgba(45, 95, 93, 0.12);
  border-radius: 14px;
  box-shadow: 0 8px 20px rgba(45, 95, 93, 0.08);
}

.page-header {
  padding: 12px 14px;
  display: grid;
  gap: 10px;
}

.header-main {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 10px;
}

.page-header h2 {
  margin: 0;
  font-size: 1.4rem;
  color: #2d5f5d;
}

.page-desc {
  margin: 6px 0 0;
  color: #607876;
  font-size: 0.92rem;
}

.header-chip {
  height: 26px;
  padding: 0 10px;
  border-radius: 999px;
  display: inline-flex;
  align-items: center;
  font-size: 0.76rem;
  color: #2d5f5d;
  background: rgba(45, 95, 93, 0.12);
  border: 1px solid rgba(45, 95, 93, 0.2);
}

.loading-tip {
  margin: 8px 0 0;
  font-size: 0.82rem;
  color: #809492;
}

.stats-row {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 8px;
}

.stat-card {
  border: 1px solid rgba(45, 95, 93, 0.14);
  border-radius: 12px;
  padding: 12px 12px;
  display: grid;
  gap: 7px;
  background: linear-gradient(145deg, rgba(245, 251, 250, 0.85), rgba(255, 255, 255, 0.98));
  color: #2d5f5d;
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.22s ease;
  text-align: left;
  min-height: 88px;
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 10px 16px rgba(45, 95, 93, 0.12);
}

.stat-card.blue {
  background: linear-gradient(145deg, rgba(239, 246, 254, 0.92), rgba(255, 255, 255, 0.99));
  border-color: rgba(70, 140, 201, 0.24);
}

.stat-card.green {
  background: linear-gradient(145deg, rgba(239, 251, 246, 0.92), rgba(255, 255, 255, 0.99));
  border-color: rgba(46, 168, 114, 0.25);
}

.stat-card.warn {
  background: linear-gradient(145deg, rgba(255, 248, 238, 0.92), rgba(255, 255, 255, 0.99));
  border-color: rgba(214, 137, 16, 0.25);
}

.stat-head {
  font-size: 0.8rem;
  color: #5f7b78;
}

.stat-value {
  font-size: 1.2rem;
  color: #1f4543;
  line-height: 1;
}

.actions-card {
  padding: 14px 14px;
  display: grid;
  gap: 12px;
}

.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.card-head h3 {
  margin: 0;
  font-size: 1.05rem;
  color: #2d5f5d;
}

.card-head span {
  font-size: 0.84rem;
  color: #68817f;
}

.action-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 8px;
}

.action-item {
  border: 1px solid rgba(45, 95, 93, 0.12);
  border-radius: 12px;
  background: rgba(248, 252, 251, 0.9);
  padding: 14px 10px;
  display: grid;
  gap: 8px;
  text-align: center;
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.22s ease;
  min-height: 108px;
}

.action-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 10px 16px rgba(45, 95, 93, 0.11);
}

.action-icon {
  width: 40px;
  height: 40px;
  margin: 0 auto;
  background: linear-gradient(145deg, #2d5f5d, #3a7370);
  color: white;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1rem;
}

.action-text {
  font-size: 0.85rem;
  color: #2a4846;
}

.deco-card {
  padding: 14px 14px;
  display: grid;
  grid-template-columns: 1.2fr 1fr;
  gap: 12px;
}

.deco-left,
.deco-right {
  border: 1px solid rgba(45, 95, 93, 0.12);
  border-radius: 12px;
  padding: 12px 12px;
  background: rgba(249, 253, 252, 0.9);
  min-height: 190px;
}

.deco-left.good {
  background: linear-gradient(145deg, rgba(239, 251, 246, 0.92), rgba(255, 255, 255, 0.98));
}

.deco-left.mid {
  background: linear-gradient(145deg, rgba(248, 251, 239, 0.92), rgba(255, 255, 255, 0.98));
}

.deco-left.low {
  background: linear-gradient(145deg, rgba(255, 248, 238, 0.92), rgba(255, 255, 255, 0.98));
}

.deco-title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.deco-title-row h3,
.deco-right h4 {
  margin: 0;
  font-size: 0.98rem;
  color: #2d5f5d;
}

.deco-badge {
  height: 24px;
  padding: 0 10px;
  border-radius: 999px;
  font-size: 0.74rem;
  display: inline-flex;
  align-items: center;
  color: #2d5f5d;
  background: rgba(45, 95, 93, 0.1);
}

.deco-progress {
  margin-top: 8px;
  display: flex;
  align-items: center;
  gap: 12px;
}

.progress-ring {
  width: 76px;
  height: 76px;
  border-radius: 50%;
  background: conic-gradient(#46a77f var(--ring), rgba(45, 95, 93, 0.16) 0);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.progress-center {
  width: 58px;
  height: 58px;
  border-radius: 50%;
  background: #fff;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  box-shadow: inset 0 0 0 1px rgba(45, 95, 93, 0.08);
}

.progress-value {
  font-size: 0.92rem;
  font-weight: 700;
  color: #1f4543;
}

.progress-meta p {
  margin: 0;
  font-size: 0.8rem;
  color: #5f7b78;
}

.progress-meta p + p {
  margin-top: 5px;
}

.progress-split {
  margin-top: 10px;
  display: grid;
  gap: 6px;
}

.split-track {
  height: 8px;
  border-radius: 999px;
  background: rgba(45, 95, 93, 0.08);
  overflow: hidden;
  display: flex;
}

.split-part {
  height: 100%;
}

.split-part.done {
  background: #46a77f;
}

.split-part.pending {
  background: #d58a23;
}

.split-part.skipped {
  background: #9aa6b4;
}

.split-legend {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.split-legend span {
  font-size: 0.78rem;
  color: #5f7774;
}

.focus-row {
  margin-top: 8px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.focus-pill {
  height: 28px;
  padding: 0 10px;
  border-radius: 999px;
  display: inline-flex;
  align-items: center;
  font-size: 0.78rem;
  color: #2d5f5d;
  background: rgba(45, 95, 93, 0.1);
  border: 1px solid rgba(45, 95, 93, 0.18);
}

.tips-list {
  margin: 8px 0 0;
  padding-left: 18px;
  display: grid;
  gap: 4px;
}

.tips-list li {
  font-size: 0.81rem;
  color: #5f7774;
  line-height: 1.4;
}

@media (max-width: 1000px) {
  .stats-row,
  .action-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .deco-card {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 767px) {
  .header-main {
    flex-direction: column;
    align-items: stretch;
  }

  .stats-row,
  .action-grid,
  .deco-card {
    grid-template-columns: 1fr;
  }

  .card-head {
    flex-direction: column;
    align-items: flex-start;
    gap: 4px;
  }

  .deco-progress {
    align-items: flex-start;
  }
}
</style>
