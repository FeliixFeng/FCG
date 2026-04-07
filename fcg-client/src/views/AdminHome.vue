<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import { fetchFamilyMembers, fetchMedicineList, fetchTodayPlanRecords } from '../utils/api'

const router = useRouter()
const userStore = useUserStore()

const members = ref([])
const medicines = ref([])
const todayRecords = ref([])
const loading = ref(false)

const memberCount = computed(() => members.value.length)
const medicineCount = computed(() => medicines.value.length)

const pendingCount = computed(() => {
  return todayRecords.value.filter(r => r.status === 0 || r.status === 'pending').length
})

const completedCount = computed(() => {
  return todayRecords.value.filter(r => r.status === 1 || r.status === 'done').length
})

const loadData = async () => {
  loading.value = true
  try {
    const [membersRes, medicinesRes] = await Promise.all([
      fetchFamilyMembers(),
      fetchMedicineList({ page: 1, size: 100 })
    ])
    members.value = membersRes.data
    medicines.value = medicinesRes.data.records || []
  } catch (err) {
    console.error('加载数据失败', err)
  } finally {
    loading.value = false
  }
}

const goToMembers = () => router.push({ name: 'admin-members' })
const goToMedicines = () => router.push({ name: 'admin-medicines' })
const goToSystem = () => router.push({ name: 'admin-system' })
const goToData = () => router.push({ name: 'admin-data' })

onMounted(() => {
  loadData()
})
</script>

<template>
  <div class="admin-dashboard">
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
          <div class="stat-label">药品库存</div>
        </div>
      </div>

      <div class="stat-card" @click="goToMembers">
        <div class="stat-icon today-icon">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="12" cy="12" r="10"/>
            <polyline points="12 6 12 12 16 14"/>
          </svg>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ completedCount }}/{{ pendingCount + completedCount }}</div>
          <div class="stat-label">今日打卡</div>
        </div>
      </div>
    </div>

    <!-- 快捷操作 -->
    <div class="quick-actions">
      <h3>快捷操作</h3>
      <div class="action-grid">
        <div class="action-item" @click="goToMembers">
          <div class="action-icon">+</div>
          <div class="action-text">添加成员</div>
        </div>
        <div class="action-item" @click="goToMedicines">
          <div class="action-icon">+</div>
          <div class="action-text">添加药品</div>
        </div>
        <div class="action-item" @click="goToSystem">
          <div class="action-icon">⚙</div>
          <div class="action-text">系统设置</div>
        </div>
        <div class="action-item" @click="goToData">
          <div class="action-icon">📊</div>
          <div class="action-text">数据统计</div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.admin-dashboard {
  max-width: 900px;
}

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
  grid-template-columns: repeat(3, 1fr);
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
  font-size: 1.2rem;
}

.action-text {
  font-size: 0.85rem;
  color: #333;
}

@media (max-width: 767px) {
  .stats-grid {
    grid-template-columns: 1fr;
  }

  .action-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
