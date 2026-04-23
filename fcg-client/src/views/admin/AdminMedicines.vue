<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { fetchAdminTodayPlans, fetchFamilyMembers, fetchMedicineList } from '../../utils/api'

const router = useRouter()

const loading = ref(false)
const members = ref([])
const medicines = ref([])
const records = ref([])
const selectedUserId = ref('all')

const memberOptions = computed(() => [
  { value: 'all', label: '全部成员' },
  ...members.value.map(m => ({ value: String(m.userId), label: m.nickname }))
])

const medicineMap = computed(() => {
  const map = new Map()
  medicines.value.forEach(item => map.set(item.id, item))
  return map
})

const filteredRecords = computed(() => {
  return records.value
})

const pendingCount = computed(() => filteredRecords.value.filter(item => item.recordStatus === 0).length)
const doneCount = computed(() => filteredRecords.value.filter(item => item.recordStatus === 1).length)
const skippedCount = computed(() => filteredRecords.value.filter(item => item.recordStatus === 2).length)

const lowStockCount = computed(() => {
  const ids = new Set(filteredRecords.value.map(item => item.medicineId))
  let count = 0
  ids.forEach((id) => {
    const med = medicineMap.value.get(id)
    if (med && Number(med.stock || 0) < 5) count += 1
  })
  return count
})

const expiringCount = computed(() => {
  const ids = new Set(filteredRecords.value.map(item => item.medicineId))
  const now = new Date()
  let count = 0
  ids.forEach((id) => {
    const med = medicineMap.value.get(id)
    if (!med?.expireDate) return
    const expire = new Date(med.expireDate)
    const diff = Math.ceil((expire.getTime() - now.getTime()) / (1000 * 60 * 60 * 24))
    if (diff >= 0 && diff <= 30) count += 1
  })
  return count
})

function getMemberName(userId) {
  return members.value.find(m => m.userId === userId)?.nickname || `成员#${userId}`
}

function slotOrder(slot) {
  const order = { '早': 1, '中': 2, '晚': 3, '睡前': 4 }
  return order[slot] || 99
}

function statusText(status) {
  if (status === 1) return '已打卡'
  if (status === 2) return '已跳过'
  return '待处理'
}

function statusClass(status) {
  if (status === 1) return 'is-done'
  if (status === 2) return 'is-skipped'
  return 'is-pending'
}

function openUserMedicine() {
  router.push({ name: 'medicine' })
}

function formatDosage(record) {
  const unit = record.medicineStockUnit || '次'
  return `${record.planDosage || '--'}${unit}`
}

async function loadData() {
  loading.value = true
  try {
    const [memberRes, medRes] = await Promise.all([
      fetchFamilyMembers(),
      fetchMedicineList({ page: 1, size: 200 })
    ])
    members.value = memberRes?.data || []
    medicines.value = medRes?.data?.records || []
    await loadPlans(false)
  } catch (err) {
    console.error('加载计划总览失败', err)
    records.value = []
  } finally {
    loading.value = false
  }
}

async function loadPlans(useLoading = true) {
  if (useLoading) loading.value = true
  const params = {
    page: 1,
    size: 500
  }
  try {
    if (selectedUserId.value !== 'all') {
      params.userId = Number(selectedUserId.value)
    }
    const res = await fetchAdminTodayPlans(params)
    records.value = (res?.data?.records || []).slice().sort((a, b) => slotOrder(a.slotName) - slotOrder(b.slotName))
  } catch (err) {
    console.error('加载计划总览失败', err)
    records.value = []
  } finally {
    if (useLoading) loading.value = false
  }
}

onMounted(() => {
  loadData()
})

watch(selectedUserId, () => {
  loadPlans()
})
</script>

<template>
  <div class="admin-plan-page" v-loading="loading">
    <div class="page-header">
      <h2>计划总览</h2>
      <p class="page-desc">跨成员查看今日计划执行与库存风险。</p>
    </div>

    <div class="toolbar card">
      <div class="toolbar-left">
        <span class="toolbar-label">查看范围</span>
        <el-select v-model="selectedUserId" style="width: 180px">
          <el-option v-for="opt in memberOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
        </el-select>
      </div>
      <button class="btn-link" @click="openUserMedicine">去普通药品页处理</button>
    </div>

    <div class="stats-grid">
      <div class="stat-card">
        <div class="stat-value">{{ pendingCount }}</div>
        <div class="stat-label">待处理</div>
      </div>
      <div class="stat-card">
        <div class="stat-value">{{ doneCount }}</div>
        <div class="stat-label">已打卡</div>
      </div>
      <div class="stat-card">
        <div class="stat-value">{{ skippedCount }}</div>
        <div class="stat-label">已跳过</div>
      </div>
      <div class="stat-card warn">
        <div class="stat-value">{{ lowStockCount }}</div>
        <div class="stat-label">库存紧张</div>
      </div>
      <div class="stat-card warn">
        <div class="stat-value">{{ expiringCount }}</div>
        <div class="stat-label">30天内临期</div>
      </div>
    </div>

    <div class="records card">
      <div class="records-head">
        <h3>今日计划明细</h3>
        <span>{{ filteredRecords.length }} 条</span>
      </div>

      <div v-if="filteredRecords.length === 0" class="empty">今日暂无计划数据</div>

      <div v-else class="record-grid">
        <article v-for="item in filteredRecords" :key="`${item.planId}-${item.slotName}-${item.userId}`" class="record-item">
          <div class="record-top">
            <span class="member-pill">{{ getMemberName(item.userId) }}</span>
            <span class="status-pill" :class="statusClass(item.recordStatus)">{{ statusText(item.recordStatus) }}</span>
          </div>
          <h4>{{ item.medicineName || '未命名药品' }}</h4>
          <div class="record-meta">
            <span>时段：{{ item.slotName || '--' }}</span>
            <span>剂量：{{ formatDosage(item) }}</span>
            <span>库存：{{ item.medicineStock ?? '--' }}{{ item.medicineStockUnit || '' }}</span>
          </div>
        </article>
      </div>
    </div>
  </div>
</template>

<style scoped>
.admin-plan-page {
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

.card {
  background: rgba(255, 255, 255, 0.96);
  border: 1px solid rgba(45, 95, 93, 0.12);
  border-radius: 14px;
  box-shadow: 0 8px 18px rgba(45, 95, 93, 0.08);
}

.toolbar {
  padding: 12px 14px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.toolbar-left {
  display: inline-flex;
  align-items: center;
  gap: 10px;
}

.toolbar-label {
  font-size: 0.9rem;
  color: #5b7472;
}

.btn-link {
  border: 1px solid rgba(45, 95, 93, 0.2);
  background: rgba(245, 250, 249, 0.9);
  color: #2d5f5d;
  border-radius: 999px;
  height: 34px;
  padding: 0 14px;
  cursor: pointer;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 10px;
}

.stat-card {
  background: rgba(255, 255, 255, 0.96);
  border: 1px solid rgba(45, 95, 93, 0.14);
  border-radius: 12px;
  padding: 12px;
  display: grid;
  gap: 4px;
}

.stat-card.warn {
  border-color: rgba(220, 143, 54, 0.26);
  background: linear-gradient(145deg, #fffaf2, #fff6ea);
}

.stat-value {
  font-size: 1.26rem;
  font-weight: 700;
  color: #214543;
}

.stat-label {
  font-size: 0.84rem;
  color: #607775;
}

.records {
  padding: 14px;
}

.records-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}

.records-head h3 {
  margin: 0;
  font-size: 1.05rem;
  color: #2d5f5d;
}

.records-head span {
  font-size: 0.86rem;
  color: #617976;
}

.empty {
  text-align: center;
  color: #738a88;
  padding: 24px 10px;
}

.record-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.record-item {
  border: 1px solid rgba(45, 95, 93, 0.13);
  background: rgba(247, 252, 251, 0.9);
  border-radius: 12px;
  padding: 10px 12px;
  display: grid;
  gap: 8px;
}

.record-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.member-pill,
.status-pill {
  height: 24px;
  padding: 0 10px;
  border-radius: 999px;
  display: inline-flex;
  align-items: center;
  font-size: 0.74rem;
  font-weight: 600;
}

.member-pill {
  background: rgba(45, 95, 93, 0.1);
  color: #2d5f5d;
}

.status-pill {
  border: 1px solid transparent;
}

.status-pill.is-pending {
  color: #8c6422;
  background: rgba(245, 190, 82, 0.2);
  border-color: rgba(245, 190, 82, 0.28);
}

.status-pill.is-done {
  color: #216045;
  background: rgba(102, 202, 156, 0.2);
  border-color: rgba(102, 202, 156, 0.28);
}

.status-pill.is-skipped {
  color: #626972;
  background: rgba(170, 177, 187, 0.22);
  border-color: rgba(170, 177, 187, 0.3);
}

.record-item h4 {
  margin: 0;
  font-size: 1rem;
  color: #1f4341;
}

.record-meta {
  display: grid;
  gap: 3px;
  font-size: 0.82rem;
  color: #5f7774;
}

@media (max-width: 1000px) {
  .stats-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .record-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 767px) {
  .toolbar {
    flex-direction: column;
    align-items: stretch;
  }

  .toolbar-left {
    justify-content: space-between;
  }

  .btn-link {
    width: 100%;
  }

  .stats-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .record-grid {
    grid-template-columns: 1fr;
  }
}
</style>
