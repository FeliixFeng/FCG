<script setup>
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { useUserStore } from '../stores/user'
import { fetchTodayVitals, fetchWeeklyVitals, fetchVitalList, deleteVital, fetchUserProfile } from '../utils/api'
import VitalRecordDialog from '../components/health/VitalRecordDialog.vue'
import BaseLayout from '../components/common/BaseLayout.vue'
import * as echarts from 'echarts'
import { Delete, Plus } from '@element-plus/icons-vue'

const userStore = useUserStore()

const todayVitals = ref([])
const weeklyData = ref([])
const loading = ref(false)
const currentType = ref(1)
const dialogVisible = ref(false)
const dialogType = ref(1)
const page = ref(1)
const size = ref(10)
const total = ref(0)
const chartRef = ref(null)

let chartInstance = null

const typeOptions = [
  { value: 1, label: '血压', unit: 'mmHg', icon: '❤️' },
  { value: 2, label: '血糖', unit: 'mmol/L', icon: '🩸' },
  { value: 3, label: '心率', unit: 'bpm', icon: '💓' },
  { value: 4, label: '体温', unit: '℃', icon: '🌡️' },
  { value: 5, label: '体重', unit: 'kg', icon: '⚖️' }
]

const currentMemberId = computed(() => userStore.member?.id)

const isAdmin = computed(() => userStore.member?.role === 0)

const familyMembers = ref([])
const userProfile = ref(null)

onMounted(async () => {
  try {
    if (isAdmin.value) {
      await loadFamilyMembers()
    }
    initSelectedMember()
    loadUserProfile()
    loadTodayVitals()
    loadWeeklyData()
    setTimeout(initChart, 100)
  } catch (e) {
    console.error('页面初始化失败', e)
  }
})

const loadUserProfile = async () => {
  try {
    const res = await fetchUserProfile(selectedMember.value)
    userProfile.value = res.data
  } catch (e) {
    console.error('加载用户档案失败', e)
  }
}

const loadFamilyMembers = async () => {
  try {
    const res = await userStore.fetchMembers()
    if (res && Array.isArray(res)) {
      const currentId = userStore.member?.id
      familyMembers.value = res.filter(m => m.userId !== currentId)
    }
  } catch (e) {
    console.error('加载家庭成员失败', e)
  }
}

const selectedMemberId = ref(null)

const initSelectedMember = () => {
  if (!selectedMemberId.value && userStore.member?.id) {
    selectedMemberId.value = userStore.member.id
  }
}

const selectedMember = computed(() => selectedMemberId.value || userStore.member?.id || null)

watch(selectedMember, (newVal, oldVal) => {
  if (newVal && newVal !== oldVal) {
    loadUserProfile()
    loadTodayVitals()
    loadWeeklyData()
  }
})

const loadTodayVitals = async () => {
  if (!selectedMember.value) return
  try {
    const res = await fetchTodayVitals(selectedMember.value)
    todayVitals.value = res.data || []
  } catch (e) {
    console.error('加载今日体征失败', e)
  }
}

const loadWeeklyData = async () => {
  if (!selectedMember.value) return
  try {
    const res = await fetchWeeklyVitals(selectedMember.value, currentType.value)
    weeklyData.value = res.data || []
    updateChart()
  } catch (e) {
    console.error('加载周数据失败', e)
  }
}

watch(() => currentType.value, (newType) => {
  if (selectedMember.value) {
    loadWeeklyData()
  }
})

const loadVitalList = async () => {
  if (!selectedMember.value) return
  listLoading.value = true
  try {
    const res = await fetchVitalList({
      userId: selectedMember.value,
      page: page.value,
      size: size.value
    })
    vitalList.value = res.data.list || []
    total.value = res.data.total || 0
  } catch (e) {
    console.error('加载历史记录失败', e)
  } finally {
    listLoading.value = false
  }
}

const getVitalValue = (type, item) => {
  if (!item) return '--'
  if (type === 1) {
    return item.valueSystolic && item.valueDiastolic 
      ? `${item.valueSystolic}/${item.valueDiastolic}` 
      : '--'
  }
  return item.value != null ? item.value : '--'
}

const getTypeOption = (type) => typeOptions.find(t => t.value === type)

const openAddDialog = (type) => {
  dialogType.value = type
  dialogVisible.value = true
}

const handleDelete = async (id) => {
  try {
    await deleteVital(id)
    loadVitalList()
    loadTodayVitals()
    loadWeeklyData()
  } catch (e) {
    console.error('删除失败', e)
  }
}

const updateChart = () => {
  if (!chartInstance) return
  
  const data = weeklyData.value
  
  if (!data || data.length === 0) {
    chartInstance.clear()
    const typeLabel = typeOptions.find(t => t.value === currentType.value)?.label || '数据'
    chartInstance.setOption({
      title: { 
        text: `暂无${typeLabel}数据`, 
        subtext: '点击上方卡片记录数据',
        left: 'center', 
        top: 'middle',
        textStyle: { color: '#999', fontSize: 14 },
        subtextStyle: { color: '#bbb', fontSize: 12 }
      },
      series: []
    })
    return
  }

  let option
  if (currentType.value === 1) {
    option = getBloodPressureOption(data)
  } else {
    option = getSingleValueOption(data, currentType.value)
  }
  
  chartInstance.clear()
  chartInstance.setOption(option, true)
}

const getBloodPressureOption = (data) => {
  const dates = data.map(d => new Date(d.measureTime).toLocaleDateString('zh-CN', { month: 'numeric', day: 'numeric' }))
  const systolic = data.map(d => d.valueSystolic)
  const diastolic = data.map(d => d.valueDiastolic)

  return {
    tooltip: { trigger: 'axis' },
    legend: { data: ['收缩压', '舒张压'], bottom: 0 },
    grid: { left: '3%', right: '4%', bottom: '15%', top: '10%', containLabel: true },
    xAxis: { type: 'category', data: dates, boundaryGap: false },
    yAxis: { type: 'value', min: 40, max: 200 },
    series: [
      { name: '收缩压', type: 'line', data: systolic, smooth: true, lineStyle: { color: '#e74c3c' }, itemStyle: { color: '#e74c3c' } },
      { name: '舒张压', type: 'line', data: diastolic, smooth: true, lineStyle: { color: '#3498db' }, itemStyle: { color: '#3498db' } }
    ]
  }
}

const getSingleValueOption = (data, type) => {
  const typeOpt = getTypeOption(type)
  const dates = data.map(d => new Date(d.measureTime).toLocaleDateString('zh-CN', { month: 'numeric', day: 'numeric' }))
  const values = data.map(d => d.value)

  const colorMap = {
    2: '#27ae60',
    3: '#f39c12',
    4: '#9b59b6',
    5: '#1abc9c'
  }

  return {
    tooltip: { trigger: 'axis' },
    legend: { data: [typeOpt?.label], bottom: 0 },
    grid: { left: '3%', right: '4%', bottom: '15%', top: '10%', containLabel: true },
    xAxis: { type: 'category', data: dates, boundaryGap: false },
    yAxis: { type: 'value', name: typeOpt?.unit },
    series: [{
      name: typeOpt?.label,
      type: 'line',
      data: values,
      smooth: true,
      lineStyle: { color: colorMap[type] || '#2d5f5d' },
      itemStyle: { color: colorMap[type] || '#2d5f5d' },
      areaStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [{ offset: 0, color: (colorMap[type] || '#2d5f5d') + '40' }, { offset: 1, color: 'transparent' }]) }
    }]
  }
}

const initChart = () => {
  if (!chartRef.value) return
  if (chartInstance) {
    chartInstance.dispose()
  }
  chartInstance = echarts.init(chartRef.value)
  updateChart()
}

const formatTime = (time) => {
  if (!time) return '--'
  const d = new Date(time)
  return `${d.getMonth() + 1}月${d.getDate()}日 ${d.getHours().toString().padStart(2, '0')}:${d.getMinutes().toString().padStart(2, '0')}`
}

onUnmounted(() => {
  if (chartInstance) {
    chartInstance.dispose()
    chartInstance = null
  }
})
</script>

<template>
  <div class="health-page">
    <BaseLayout>
      <div class="page-header">
        <h1 class="page-title">健康数据</h1>
        <div v-if="isAdmin && familyMembers.length > 0" class="member-switch">
          <span class="switch-label">查看成员：</span>
          <el-select 
            v-model="selectedMemberId" 
            placeholder="选择成员" 
            size="default"
            style="width: 140px;"
          >
            <el-option :value="userStore.member?.id" :label="userStore.member?.nickname + '（我）'" />
            <el-option v-for="m in familyMembers" :key="m.userId" :value="m.userId" :label="m.nickname" />
          </el-select>
        </div>
        <span v-else-if="userStore.member" style="font-size: 12px; color: #999;">{{ userStore.member.nickname }}</span>
      </div>

      <section class="today-vitals">
        <h2 class="section-title">今日体征</h2>
        <div class="vital-cards">
          <div v-for="t in typeOptions" :key="t.value" class="vital-card" @click="openAddDialog(t.value)">
            <div class="card-icon">{{ t.icon }}</div>
            <div class="card-value">
              {{ getVitalValue(t.value, todayVitals.find(v => v.type === t.value)) }}
            </div>
            <div class="card-label">{{ t.label }}</div>
            <div class="card-unit">{{ todayVitals.find(v => v.type === t.value) ? t.unit : '点击记录' }}</div>
            <div class="card-add">
              <Plus />
            </div>
          </div>
        </div>
      </section>

      <section class="trend-chart">
        <h2 class="section-title">近7天趋势</h2>
        <div class="chart-tabs">
          <button
            v-for="t in typeOptions"
            :key="t.value"
            class="tab-btn"
            :class="{ active: currentType === t.value }"
            @click="currentType = t.value"
          >
            {{ t.label }}
          </button>
        </div>
        <div ref="chartRef" class="chart-container"></div>
      </section>

      <VitalRecordDialog
        v-model:visible="dialogVisible"
        :type="dialogType"
        :user-id="selectedMember"
        @success="() => { loadTodayVitals(); loadWeeklyData(); }"
      />
    </BaseLayout>
  </div>
</template>

<style scoped>
.health-page {
  min-height: 100%;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.page-title {
  font-size: 1.5rem;
  font-weight: 600;
  color: #1a1a1a;
  margin: 0;
}

.member-switch {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.switch-label {
  font-size: 0.9rem;
  color: #666;
  white-space: nowrap;
}

.section-title {
  font-size: 1.1rem;
  font-weight: 600;
  color: #1a1a1a;
  margin: 0 0 16px 0;
}

.today-vitals {
  margin-bottom: 32px;
}

.vital-cards {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 12px;
}

.vital-card {
  background: #fff;
  border-radius: 16px;
  padding: 16px;
  text-align: center;
  cursor: pointer;
  transition: all 0.2s;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  position: relative;
  overflow: hidden;
}

.vital-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 16px rgba(45, 95, 93, 0.12);
}

.card-icon {
  font-size: 1.5rem;
  margin-bottom: 8px;
}

.card-value {
  font-size: 1.4rem;
  font-weight: 700;
  color: #2d5f5d;
}

.card-label {
  font-size: 0.85rem;
  color: #666;
  margin-top: 4px;
}

.card-unit {
  font-size: 0.75rem;
  color: #999;
  margin-top: 2px;
}

.card-add {
  position: absolute;
  top: 8px;
  right: 8px;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: rgba(45, 95, 93, 0.1);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #2d5f5d;
  font-size: 14px;
  opacity: 0;
  transition: opacity 0.2s;
}

.vital-card:hover .card-add {
  opacity: 1;
}

.trend-chart {
  margin-bottom: 32px;
}

.chart-tabs {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.tab-btn {
  padding: 8px 16px;
  border: 1px solid #ddd;
  border-radius: 20px;
  background: #fff;
  font-size: 0.85rem;
  cursor: pointer;
  transition: all 0.2s;
}

.tab-btn.active {
  background: #2d5f5d;
  color: #fff;
  border-color: #2d5f5d;
}

.chart-container {
  width: 100%;
  height: 280px;
  background: #fff;
  border-radius: 16px;
  padding: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  box-sizing: border-box;
}

.history-records {
  margin-bottom: 24px;
}

.record-list {
  background: #fff;
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.record-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 16px;
  border-bottom: 1px solid #f0f0f0;
}

.record-item:last-child {
  border-bottom: none;
}

.record-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.record-time {
  font-size: 0.85rem;
  color: #666;
}

.record-type {
  font-size: 0.9rem;
  font-weight: 500;
  color: #2d5f5d;
}

.record-value {
  font-size: 0.95rem;
  font-weight: 600;
  color: #1a1a1a;
}

.loading-state,
.empty-state {
  padding: 40px;
  text-align: center;
  color: #999;
}

@media (max-width: 767px) {
  .page-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }

  .vital-cards {
    grid-template-columns: repeat(3, 1fr);
  }

  .vital-card {
    padding: 12px;
  }

  .card-value {
    font-size: 1.1rem;
  }

  .chart-container {
    width: 100%;
    height: 240px;
    box-sizing: border-box;
  }
}
</style>