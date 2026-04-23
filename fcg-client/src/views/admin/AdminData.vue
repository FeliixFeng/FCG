<script setup>
import { computed, onMounted, ref } from 'vue'
import { fetchAdminDailyStats } from '../../utils/api'

const loading = ref(false)
const stats = ref({
  todayPendingCount: 0,
  todayDoneCount: 0,
  todaySkippedCount: 0,
  completionRate: 0,
  weeklyVitalCount: 0,
  memberRates: []
})

const totalPending = computed(() => Number(stats.value.todayPendingCount || 0))
const totalDone = computed(() => Number(stats.value.todayDoneCount || 0))
const totalSkipped = computed(() => Number(stats.value.todaySkippedCount || 0))
const totalVitalWeekly = computed(() => Number(stats.value.weeklyVitalCount || 0))
const completionRate = computed(() => Number(stats.value.completionRate || 0))

const rankedMembers = computed(() => {
  return (stats.value.memberRates || []).map(item => ({
    userId: item.userId,
    nickname: item.userName,
    done: item.doneCount,
    total: item.totalCount,
    rate: item.completionRate
  }))
})

async function loadData() {
  loading.value = true
  try {
    const res = await fetchAdminDailyStats({ days: 7 })
    stats.value = {
      ...stats.value,
      ...(res?.data || {})
    }
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadData()
})
</script>

<template>
  <div class="admin-data-page" v-loading="loading">
    <div class="page-header">
      <h2>数据统计</h2>
      <p class="page-desc">汇总家庭今日执行与近7天体征录入情况。</p>
    </div>

    <div class="kpi-grid">
      <div class="kpi-card">
        <p class="kpi-label">今日完成率</p>
        <p class="kpi-value">{{ completionRate }}%</p>
      </div>
      <div class="kpi-card">
        <p class="kpi-label">待处理</p>
        <p class="kpi-value">{{ totalPending }}</p>
      </div>
      <div class="kpi-card">
        <p class="kpi-label">已跳过</p>
        <p class="kpi-value">{{ totalSkipped }}</p>
      </div>
      <div class="kpi-card">
        <p class="kpi-label">近7天体征记录</p>
        <p class="kpi-value">{{ totalVitalWeekly }}</p>
      </div>
    </div>

    <div class="card chart-card">
      <div class="card-head">
        <h3>今日执行分布</h3>
      </div>
      <div class="bar-wrap">
        <div class="bar-row">
          <span class="bar-label">待处理</span>
          <div class="bar-track">
            <div class="bar-fill pending" :style="{ width: `${Math.min(100, totalPending * 8)}%` }"></div>
          </div>
          <span class="bar-value">{{ totalPending }}</span>
        </div>
        <div class="bar-row">
          <span class="bar-label">已完成</span>
          <div class="bar-track">
            <div class="bar-fill done" :style="{ width: `${Math.min(100, totalDone * 8)}%` }"></div>
          </div>
          <span class="bar-value">{{ totalDone }}</span>
        </div>
        <div class="bar-row">
          <span class="bar-label">已跳过</span>
          <div class="bar-track">
            <div class="bar-fill skipped" :style="{ width: `${Math.min(100, totalSkipped * 8)}%` }"></div>
          </div>
          <span class="bar-value">{{ totalSkipped }}</span>
        </div>
      </div>
    </div>

    <div class="card table-card">
      <div class="card-head">
        <h3>成员执行排名（今日）</h3>
      </div>
      <div v-if="rankedMembers.length === 0" class="empty">暂无可统计数据</div>
      <div v-else class="rank-list">
        <div v-for="(item, idx) in rankedMembers" :key="item.userId" class="rank-item">
          <div class="rank-left">
            <span class="rank-index">{{ idx + 1 }}</span>
            <div>
              <div class="rank-name">{{ item.nickname }}</div>
              <div class="rank-sub">完成 {{ item.done }} / 总计 {{ item.total }}</div>
            </div>
          </div>
          <div class="rank-right">
            <span class="rate-pill">{{ item.rate }}%</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.admin-data-page {
  width: 100%;
  display: grid;
  gap: 14px;
}

.page-header {
  margin-bottom: 4px;
}

.page-header h2 {
  margin: 0 0 8px;
  font-size: 1.4rem;
  color: #2d5f5d;
}

.page-desc {
  margin: 0;
  color: #607775;
  font-size: 0.92rem;
}

.kpi-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
}

.kpi-card {
  background: rgba(255, 255, 255, 0.96);
  border: 1px solid rgba(45, 95, 93, 0.14);
  border-radius: 12px;
  padding: 12px;
}

.kpi-label {
  margin: 0 0 8px;
  font-size: 0.84rem;
  color: #5f7774;
}

.kpi-value {
  margin: 0;
  font-size: 1.38rem;
  font-weight: 700;
  color: #204644;
}

.card {
  background: rgba(255, 255, 255, 0.96);
  border: 1px solid rgba(45, 95, 93, 0.12);
  border-radius: 14px;
  box-shadow: 0 8px 18px rgba(45, 95, 93, 0.08);
  padding: 14px;
}

.card-head {
  margin-bottom: 12px;
}

.card-head h3 {
  margin: 0;
  font-size: 1.02rem;
  color: #2d5f5d;
}

.bar-wrap {
  display: grid;
  gap: 10px;
}

.bar-row {
  display: grid;
  grid-template-columns: 74px 1fr 38px;
  align-items: center;
  gap: 8px;
}

.bar-label {
  font-size: 0.85rem;
  color: #5f7774;
}

.bar-track {
  height: 10px;
  border-radius: 999px;
  background: rgba(45, 95, 93, 0.08);
  overflow: hidden;
}

.bar-fill {
  height: 100%;
  border-radius: 999px;
}

.bar-fill.pending {
  background: linear-gradient(90deg, #f0b45e, #e9a847);
}

.bar-fill.done {
  background: linear-gradient(90deg, #55b68f, #2f8f75);
}

.bar-fill.skipped {
  background: linear-gradient(90deg, #aab1bb, #8f98a5);
}

.bar-value {
  text-align: right;
  font-size: 0.84rem;
  color: #5f7774;
}

.rank-list {
  display: grid;
  gap: 8px;
}

.rank-item {
  border: 1px solid rgba(45, 95, 93, 0.12);
  background: rgba(248, 252, 251, 0.9);
  border-radius: 11px;
  padding: 10px 12px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.rank-left {
  display: inline-flex;
  align-items: center;
  gap: 10px;
}

.rank-index {
  width: 24px;
  height: 24px;
  border-radius: 999px;
  background: rgba(45, 95, 93, 0.1);
  color: #2d5f5d;
  font-size: 0.8rem;
  font-weight: 700;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.rank-name {
  font-size: 0.94rem;
  color: #1f4341;
  font-weight: 600;
}

.rank-sub {
  margin-top: 2px;
  font-size: 0.8rem;
  color: #637b78;
}

.rate-pill {
  height: 26px;
  padding: 0 12px;
  border-radius: 999px;
  border: 1px solid rgba(45, 95, 93, 0.2);
  background: rgba(245, 250, 249, 0.9);
  color: #2d5f5d;
  font-size: 0.84rem;
  display: inline-flex;
  align-items: center;
}

.empty {
  text-align: center;
  color: #738a88;
  padding: 22px 10px;
}

@media (max-width: 1000px) {
  .kpi-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 767px) {
  .kpi-grid {
    grid-template-columns: 1fr;
  }

  .bar-row {
    grid-template-columns: 64px 1fr 30px;
  }
}
</style>
