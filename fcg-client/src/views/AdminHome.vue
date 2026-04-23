<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import { fetchAdminOverview } from '../utils/api'

const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
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
  }
}

const goToMembers = () => router.push({ name: 'admin-members' })
const goToMedicines = () => router.push({ name: 'admin-medicines' })
const goToData = () => router.push({ name: 'admin-data' })
const backToUserHome = () => router.push({ name: 'home' })

onMounted(() => {
  loadData()
})
</script>

<template>
  <div class="admin-dashboard" v-loading="loading">
    <div class="page-header">
      <h2>管理中心</h2>
      <p class="page-desc">家庭：{{ userStore.family?.familyName }}</p>
    </div>

    <!-- 统计卡片 -->
    <div class="stats-grid">
      <div class="stat-card" @click="goToMembers">
        <div class="stat-icon members-icon">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/>
            <circle cx="9" cy="7" r="4"/>
            <path d="M23 21v-2a4 4 0 0 0-3-3.87"/>
            <path d="M16 3.13a4 4 0 0 1 0 7.75"/>
          </svg>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ memberCount }}</div>
          <div class="stat-label">家庭成员</div>
        </div>
      </div>

      <div class="stat-card" @click="goToMedicines">
        <div class="stat-icon medicine-icon">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="m10.5 20.5 10-10a4.95 4.95 0 1 0-7-7l-10 10a4.95 4.95 0 1 0 7 7Z"/>
            <path d="m8.5 8.5 7 7"/>
          </svg>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ medicineCount }}</div>
          <div class="stat-label">药品总数</div>
        </div>
      </div>

      <div class="stat-card" @click="goToData">
        <div class="stat-icon today-icon">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="12" cy="12" r="10"/>
            <polyline points="12 6 12 12 16 14"/>
          </svg>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ completedCount }}/{{ pendingCount + completedCount }}</div>
          <div class="stat-label">今日完成</div>
        </div>
      </div>

      <div class="stat-card" @click="goToMedicines">
        <div class="stat-icon warn-icon">⚠️</div>
        <div class="stat-info">
          <div class="stat-value">{{ lowStockCount }}</div>
          <div class="stat-label">库存紧张</div>
        </div>
      </div>
    </div>

    <!-- 快捷操作 -->
    <div class="quick-actions">
      <h3>快捷操作</h3>
      <div class="action-grid">
        <div class="action-item" @click="goToMembers">
          <div class="action-icon">👥</div>
          <div class="action-text">成员管理</div>
        </div>
        <div class="action-item" @click="goToMedicines">
          <div class="action-icon">🗓️</div>
          <div class="action-text">计划总览</div>
        </div>
        <div class="action-item" @click="goToData">
          <div class="action-icon">📊</div>
          <div class="action-text">数据统计</div>
        </div>
        <div class="action-item" @click="backToUserHome">
          <div class="action-icon">↩</div>
          <div class="action-text">返回用户页</div>
        </div>
      </div>
    </div>

    <div class="quick-summary">
      <div class="summary-chip">待处理 {{ pendingCount }}</div>
      <div class="summary-chip">已完成 {{ completedCount }}</div>
      <div class="summary-chip">已跳过 {{ skippedCount }}</div>
    </div>
  </div>
</template>

<style scoped>
.page-header {
  margin-bottom: 24px;
}

.page-header h2 {
  margin: 0 0 8px;
  font-size: 1.5rem;
  color: #2d5f5d;
}

.page-desc {
  margin: 0;
  color: #666;
  font-size: 0.9rem;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
  margin-bottom: 32px;
}

.stat-card {
  background: white;
  border-radius: 12px;
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 16px;
  box-shadow: 0 2px 12px rgba(45, 95, 93, 0.08);
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 16px rgba(45, 95, 93, 0.12);
}

.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.members-icon {
  background: #fef3e7;
  color: #d68910;
}

.medicine-icon {
  background: #e8f4f8;
  color: #2874a6;
}

.today-icon {
  background: #e4f0ef;
  color: #2d5f5d;
}

.warn-icon {
  background: #fff3e9;
  color: #d68910;
  font-size: 1.1rem;
}

.stat-value {
  font-size: 1.5rem;
  font-weight: 700;
  color: #333;
}

.stat-label {
  font-size: 0.85rem;
  color: #666;
  margin-top: 4px;
}

.quick-actions h3 {
  font-size: 1rem;
  color: #333;
  margin: 0 0 16px;
}

.action-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
}

.action-item {
  background: white;
  border-radius: 12px;
  padding: 20px;
  text-align: center;
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
  box-shadow: 0 2px 8px rgba(45, 95, 93, 0.06);
}

.action-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(45, 95, 93, 0.1);
}

.action-icon {
  width: 40px;
  height: 40px;
  margin: 0 auto 8px;
  background: #2d5f5d;
  color: white;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1rem;
}

.action-text {
  font-size: 0.85rem;
  color: #333;
}

.quick-summary {
  margin-top: 18px;
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.summary-chip {
  height: 34px;
  padding: 0 14px;
  border-radius: 999px;
  display: inline-flex;
  align-items: center;
  font-size: 0.84rem;
  color: #2d5f5d;
  background: rgba(45, 95, 93, 0.1);
  border: 1px solid rgba(45, 95, 93, 0.16);
}

@media (max-width: 767px) {
  .stats-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .action-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .quick-summary {
    gap: 8px;
  }
}
</style>
