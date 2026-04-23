<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { fetchAdminTodayPlans, fetchFamilyInfo, fetchFamilyMembers, fetchMedicineList } from '../../utils/api'

const router = useRouter()

const baseLoading = ref(false)
const plansLoading = ref(false)
const members = ref([])
const medicines = ref([])
const records = ref([])
const lowStockThreshold = ref(5)
const expiringDays = ref(30)
const summary = ref({
  pending: 0,
  done: 0,
  skipped: 0
})

const selectedUserId = ref('all')
const statusFilter = ref('all')
const slotFilter = ref('all')
const keywordFilter = ref('')
const page = ref(1)
const size = ref(6)
const total = ref(0)

const memberOptions = computed(() => [
  { value: 'all', label: '全部成员' },
  ...members.value.map(item => ({ value: String(item.userId), label: item.nickname }))
])

const statusOptions = [
  { value: 'all', label: '全部状态' },
  { value: '0', label: '待处理' },
  { value: '1', label: '已打卡' },
  { value: '2', label: '已跳过' }
]

const slotOptions = [
  { value: 'all', label: '全部时段' },
  { value: '早', label: '早' },
  { value: '中', label: '中' },
  { value: '晚', label: '晚' },
  { value: '睡前', label: '睡前' }
]

const pendingCount = computed(() => Number(summary.value.pending || 0))
const doneCount = computed(() => Number(summary.value.done || 0))
const skippedCount = computed(() => Number(summary.value.skipped || 0))

const lowStockCount = computed(() => {
  return medicines.value.filter(item => Number(item.stock || 0) < Number(lowStockThreshold.value || 5)).length
})

const expiringCount = computed(() => {
  const now = new Date()
  return medicines.value.filter((item) => {
    if (!item?.expireDate) return false
    const expire = new Date(item.expireDate)
    const diff = Math.ceil((expire.getTime() - now.getTime()) / (1000 * 60 * 60 * 24))
    return diff >= 0 && diff <= Number(expiringDays.value || 30)
  }).length
})

const riskCount = computed(() => lowStockCount.value + expiringCount.value)

function slotOrder(slot) {
  const order = { '早': 1, '中': 2, '晚': 3, '睡前': 4 }
  return order[slot] || 99
}

function completionOrder(status) {
  // 未完成优先展示，已处理（打卡/跳过）归为同一层级
  return status === 0 ? 0 : 1
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

function getMemberName(userId) {
  return members.value.find(item => item.userId === userId)?.nickname || `成员#${userId}`
}

function formatDosage(item) {
  const unit = item.medicineStockUnit || '次'
  return `${item.planDosage || '--'}${unit}`
}

function openUserMedicine() {
  router.push({ name: 'medicine' })
}

async function loadBaseData() {
  baseLoading.value = true
  try {
    const [familyRes, memberRes, medRes] = await Promise.all([
      fetchFamilyInfo(),
      fetchFamilyMembers(),
      fetchMedicineList({ page: 1, size: 1000 })
    ])
    members.value = memberRes?.data || []
    medicines.value = medRes?.data?.records || []
    lowStockThreshold.value = Number(familyRes?.data?.lowStockThreshold || 5)
    expiringDays.value = Number(familyRes?.data?.expiringDays || 30)
  } finally {
    baseLoading.value = false
  }
}

async function loadPlans(useLoading = true) {
  if (useLoading) plansLoading.value = true
  const params = {
    page: page.value,
    size: size.value
  }

  if (selectedUserId.value !== 'all') params.userId = Number(selectedUserId.value)
  if (statusFilter.value !== 'all') params.status = Number(statusFilter.value)
  if (slotFilter.value !== 'all') params.slotName = slotFilter.value
  if (keywordFilter.value.trim()) params.keyword = keywordFilter.value.trim()

  try {
    const res = await fetchAdminTodayPlans(params)
    const data = res?.data || {}
    total.value = Number(data.total || 0)
    records.value = (data.records || []).slice().sort((a, b) => {
      const completionDiff = completionOrder(a.recordStatus) - completionOrder(b.recordStatus)
      if (completionDiff !== 0) return completionDiff

      const nameDiff = String(a.userName || '').localeCompare(String(b.userName || ''), 'zh-Hans-CN')
      if (nameDiff !== 0) return nameDiff

      const slotDiff = slotOrder(a.slotName) - slotOrder(b.slotName)
      if (slotDiff !== 0) return slotDiff

      return String(a.medicineName || '').localeCompare(String(b.medicineName || ''), 'zh-Hans-CN')
    })
  } catch (err) {
    console.error('加载计划总览失败', err)
    total.value = 0
    records.value = []
  } finally {
    if (useLoading) plansLoading.value = false
  }
}

function buildFilterParams() {
  const params = {}
  if (selectedUserId.value !== 'all') params.userId = Number(selectedUserId.value)
  if (slotFilter.value !== 'all') params.slotName = slotFilter.value
  if (keywordFilter.value.trim()) params.keyword = keywordFilter.value.trim()
  return params
}

async function loadSummary() {
  const baseParams = buildFilterParams()
  try {
    const [pendingRes, doneRes, skippedRes] = await Promise.all([
      fetchAdminTodayPlans({ ...baseParams, status: 0, page: 1, size: 1 }),
      fetchAdminTodayPlans({ ...baseParams, status: 1, page: 1, size: 1 }),
      fetchAdminTodayPlans({ ...baseParams, status: 2, page: 1, size: 1 })
    ])
    summary.value = {
      pending: Number(pendingRes?.data?.total || 0),
      done: Number(doneRes?.data?.total || 0),
      skipped: Number(skippedRes?.data?.total || 0)
    }
  } catch (err) {
    console.error('加载计划总览统计失败', err)
    summary.value = { pending: 0, done: 0, skipped: 0 }
  }
}

async function initData() {
  loadBaseData()
  loadSummary()
  loadPlans()
}

function handlePageChange(nextPage) {
  page.value = nextPage
  loadPlans()
}

function applyFilters() {
  page.value = 1
  loadSummary()
  loadPlans()
}

watch([selectedUserId, statusFilter, slotFilter], () => {
  applyFilters()
})

onMounted(() => {
  initData()
})
</script>

<template>
  <div class="admin-plan-page">
    <header class="page-header card">
      <div class="header-main">
        <div>
          <h2>计划总览</h2>
          <p>按成员统一查看今日计划执行状态，快速识别库存与临期风险。</p>
        </div>
        <button class="btn-main" @click="openUserMedicine">去普通药品页处理</button>
      </div>

      <div class="stats-row">
        <div class="stat-card">
          <span class="stat-head"><span class="stat-icon">🕒</span><span class="stat-label">待处理</span></span>
          <strong class="stat-value">{{ pendingCount }}</strong>
        </div>
        <div class="stat-card ok">
          <span class="stat-head"><span class="stat-icon">✅</span><span class="stat-label">已打卡</span></span>
          <strong class="stat-value">{{ doneCount }}</strong>
        </div>
        <div class="stat-card skip">
          <span class="stat-head"><span class="stat-icon">⏭️</span><span class="stat-label">已跳过</span></span>
          <strong class="stat-value">{{ skippedCount }}</strong>
        </div>
        <div class="stat-card warn">
          <span class="stat-head"><span class="stat-icon">⚠️</span><span class="stat-label">风险项</span></span>
          <strong class="stat-value">{{ riskCount }}</strong>
          <span class="stat-sub">紧张 {{ lowStockCount }}（阈值 {{ lowStockThreshold }}）· 临期 {{ expiringCount }}（{{ expiringDays }} 天）</span>
        </div>
      </div>

      <div class="filters-row">
        <input
          v-model="keywordFilter"
          class="filter-input"
          placeholder="筛选药品/成员关键词"
          :disabled="baseLoading"
          @keydown.enter.prevent="applyFilters"
        />
        <select v-model="selectedUserId" class="filter-select" :disabled="baseLoading">
          <option v-for="item in memberOptions" :key="item.value" :value="item.value">{{ item.label }}</option>
        </select>
        <select v-model="statusFilter" class="filter-select" :disabled="baseLoading">
          <option v-for="item in statusOptions" :key="item.value" :value="item.value">{{ item.label }}</option>
        </select>
        <select v-model="slotFilter" class="filter-select" :disabled="baseLoading">
          <option v-for="item in slotOptions" :key="item.value" :value="item.value">{{ item.label }}</option>
        </select>
        <button class="btn-filter" :disabled="plansLoading || baseLoading" @click="applyFilters">筛选</button>
      </div>
    </header>

    <section class="records card">
      <div class="records-head">
        <h3>今日计划明细</h3>
        <span>共 {{ total }} 条（当前 {{ records.length }} 条）</span>
      </div>

      <div v-if="plansLoading" class="empty">加载中...</div>
      <div v-else-if="records.length === 0" class="empty">暂无符合条件的计划记录</div>

      <div v-else class="record-grid">
        <article
          v-for="item in records"
          :key="`${item.planId}-${item.slotName}-${item.userId}`"
          class="record-item"
          :class="statusClass(item.recordStatus)"
        >
          <div class="record-top">
            <span class="member-pill">{{ getMemberName(item.userId) }}</span>
            <span class="status-pill" :class="statusClass(item.recordStatus)">{{ statusText(item.recordStatus) }}</span>
          </div>

          <h4>{{ item.medicineName || '未命名药品' }}</h4>

          <div class="record-meta">
            <span>🕑 时段：{{ item.slotName || '--' }}</span>
            <span>💊 剂量：{{ formatDosage(item) }}</span>
          </div>
        </article>
      </div>

      <div class="pager-wrap">
        <el-pagination
          background
          layout="total, prev, pager, next"
          :current-page="page"
          :page-size="size"
          :total="total"
          @current-change="handlePageChange"
        />
      </div>
    </section>
  </div>
</template>

<style>
.admin-plan-page {
  width: 100%;
  display: grid;
  gap: 14px;
}

.admin-plan-page .card {
  background: rgba(255, 255, 255, 0.96);
  border: 1px solid rgba(45, 95, 93, 0.12);
  border-radius: 14px;
  box-shadow: 0 8px 20px rgba(45, 95, 93, 0.08);
}

.admin-plan-page .page-header {
  padding: 14px;
  display: grid;
  gap: 12px;
}

.admin-plan-page .header-main {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.admin-plan-page .header-main h2 {
  margin: 0 0 6px;
  font-size: 1.4rem;
  color: #1f4543;
}

.admin-plan-page .header-main p {
  margin: 0;
  color: #607876;
  font-size: 0.92rem;
}

.admin-plan-page .btn-main,
.admin-plan-page .btn-filter {
  height: 38px;
  border: none;
  border-radius: 10px;
  padding: 0 14px;
  background: #2d5f5d;
  color: #fff;
  font-size: 0.88rem;
  cursor: pointer;
  transition: transform 0.18s ease, box-shadow 0.22s ease, background 0.2s ease;
}

.admin-plan-page .btn-main:hover,
.admin-plan-page .btn-filter:hover {
  background: #244c4a;
  transform: translateY(-1px);
  box-shadow: 0 8px 14px rgba(35, 74, 72, 0.25);
}

.admin-plan-page .btn-filter:disabled,
.admin-plan-page .filter-input:disabled,
.admin-plan-page .filter-select:disabled {
  opacity: 0.62;
  cursor: not-allowed;
  transform: none;
  box-shadow: none;
}

.admin-plan-page .stats-row {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
}

.admin-plan-page .stat-card {
  border: 1px solid rgba(45, 95, 93, 0.14);
  border-radius: 12px;
  padding: 11px 12px;
  display: grid;
  gap: 7px;
  background: linear-gradient(145deg, rgba(245, 251, 250, 0.85), rgba(255, 255, 255, 0.98));
  box-shadow: 0 4px 10px rgba(45, 95, 93, 0.05);
  color: #2d5f5d;
  transition: transform 0.2s ease, box-shadow 0.22s ease;
}

.admin-plan-page .stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 10px 16px rgba(45, 95, 93, 0.12);
}

.admin-plan-page .stat-card.ok {
  background: linear-gradient(145deg, rgba(239, 251, 246, 0.9), rgba(255, 255, 255, 0.99));
  border-color: rgba(46, 168, 114, 0.25);
}

.admin-plan-page .stat-card.skip {
  background: linear-gradient(145deg, rgba(245, 248, 252, 0.9), rgba(255, 255, 255, 0.99));
  border-color: rgba(118, 135, 156, 0.24);
}

.admin-plan-page .stat-card.warn {
  background: linear-gradient(145deg, rgba(255, 248, 238, 0.92), rgba(255, 255, 255, 0.99));
  border-color: rgba(214, 137, 16, 0.25);
}

.admin-plan-page .stat-head {
  display: flex;
  align-items: center;
  gap: 6px;
}

.admin-plan-page .stat-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
  border-radius: 6px;
  font-size: 0.78rem;
  background: rgba(45, 95, 93, 0.11);
}

.admin-plan-page .stat-label {
  font-size: 0.8rem;
  color: #5f7b78;
}

.admin-plan-page .stat-value {
  font-size: 1.2rem;
  color: #1f4543;
  line-height: 1;
}

.admin-plan-page .stat-sub {
  font-size: 0.76rem;
  color: #7a6642;
}

.admin-plan-page .filters-row {
  display: grid;
  grid-template-columns: minmax(220px, 1fr) 170px 170px 150px 92px;
  gap: 8px;
}

.admin-plan-page .filter-input,
.admin-plan-page .filter-select {
  width: 100%;
  border: 1px solid rgba(45, 95, 93, 0.2);
  border-radius: 10px;
  height: 38px;
  padding: 0 12px;
  font-size: 0.88rem;
  color: #2a4846;
  background: #fff;
  box-sizing: border-box;
  transition: transform 0.18s ease, box-shadow 0.2s ease, border-color 0.2s ease;
}

.admin-plan-page .filter-input::placeholder {
  color: #9bb0af;
}

.admin-plan-page .filter-input:hover,
.admin-plan-page .filter-select:hover {
  transform: translateY(-1px);
  box-shadow: 0 6px 12px rgba(45, 95, 93, 0.08);
  border-color: rgba(45, 95, 93, 0.32);
}

.admin-plan-page .records {
  padding: 14px;
  display: grid;
  gap: 12px;
}

.admin-plan-page .records-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.admin-plan-page .records-head h3 {
  margin: 0;
  font-size: 1.05rem;
  color: #2d5f5d;
}

.admin-plan-page .records-head span {
  font-size: 0.86rem;
  color: #617976;
}

.admin-plan-page .empty {
  text-align: center;
  color: #738a88;
  padding: 24px 10px;
}

.admin-plan-page .record-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.admin-plan-page .record-item {
  border: 1px solid rgba(45, 95, 93, 0.13);
  background: rgba(247, 252, 251, 0.9);
  border-radius: 12px;
  padding: 10px 12px;
  display: grid;
  gap: 8px;
  transition: transform 0.2s ease, box-shadow 0.22s ease;
}

.admin-plan-page .record-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 10px 16px rgba(45, 95, 93, 0.11);
}

.admin-plan-page .record-item.is-pending {
  background: linear-gradient(145deg, rgba(255, 249, 236, 0.9), rgba(255, 255, 255, 0.98));
  border-color: rgba(214, 137, 16, 0.2);
}

.admin-plan-page .record-item.is-done {
  background: linear-gradient(145deg, rgba(242, 252, 247, 0.92), rgba(255, 255, 255, 0.98));
  border-color: rgba(46, 168, 114, 0.2);
}

.admin-plan-page .record-item.is-skipped {
  background: linear-gradient(145deg, rgba(246, 249, 253, 0.92), rgba(255, 255, 255, 0.98));
  border-color: rgba(118, 135, 156, 0.2);
}

.admin-plan-page .record-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.admin-plan-page .member-pill,
.admin-plan-page .status-pill {
  height: 24px;
  padding: 0 10px;
  border-radius: 999px;
  display: inline-flex;
  align-items: center;
  font-size: 0.74rem;
  font-weight: 600;
}

.admin-plan-page .member-pill {
  background: rgba(45, 95, 93, 0.1);
  color: #2d5f5d;
}

.admin-plan-page .status-pill {
  border: 1px solid transparent;
}

.admin-plan-page .status-pill.is-pending {
  color: #8c6422;
  background: rgba(245, 190, 82, 0.2);
  border-color: rgba(245, 190, 82, 0.28);
}

.admin-plan-page .status-pill.is-done {
  color: #216045;
  background: rgba(102, 202, 156, 0.2);
  border-color: rgba(102, 202, 156, 0.28);
}

.admin-plan-page .status-pill.is-skipped {
  color: #626972;
  background: rgba(170, 177, 187, 0.22);
  border-color: rgba(170, 177, 187, 0.3);
}

.admin-plan-page .record-item h4 {
  margin: 0;
  font-size: 1rem;
  color: #1f4341;
}

.admin-plan-page .record-meta {
  display: grid;
  gap: 3px;
  font-size: 0.82rem;
  color: #5f7774;
}

.admin-plan-page .pager-wrap {
  display: flex;
  justify-content: flex-end;
  padding-top: 2px;
}

.admin-plan-page .el-pagination {
  padding: 6px 0 0;
}

.admin-plan-page .el-pager li,
.admin-plan-page .btn-prev,
.admin-plan-page .btn-next {
  transition: transform 0.18s ease, box-shadow 0.2s ease;
}

.admin-plan-page .el-pager li:hover,
.admin-plan-page .btn-prev:hover,
.admin-plan-page .btn-next:hover {
  transform: translateY(-1px);
  box-shadow: 0 6px 12px rgba(45, 95, 93, 0.14);
}

@media (max-width: 1200px) {
  .admin-plan-page .filters-row {
    grid-template-columns: minmax(220px, 1fr) 160px 160px 130px 88px;
  }
}

@media (max-width: 1000px) {
  .admin-plan-page .stats-row {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .admin-plan-page .record-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .admin-plan-page .filters-row {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .admin-plan-page .btn-filter {
    grid-column: span 2;
  }
}

@media (max-width: 767px) {
  .admin-plan-page .header-main {
    flex-direction: column;
    align-items: stretch;
  }

  .admin-plan-page .btn-main {
    width: 100%;
  }

  .admin-plan-page .stats-row,
  .admin-plan-page .filters-row,
  .admin-plan-page .record-grid {
    grid-template-columns: 1fr;
  }

  .admin-plan-page .btn-filter {
    grid-column: auto;
  }

  .admin-plan-page .records-head {
    flex-direction: column;
    align-items: flex-start;
    gap: 4px;
  }

  .admin-plan-page .pager-wrap {
    justify-content: center;
  }
}
</style>
