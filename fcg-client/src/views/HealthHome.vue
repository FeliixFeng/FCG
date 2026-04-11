<script setup>
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { useUserStore } from '../stores/user'
import { fetchTodayVitals, fetchWeeklyVitals, fetchVitalList, deleteVital, fetchUserProfile, fetchLatestReport, generateHealthReport, fetchHealthReports } from '../utils/api'
import VitalRecordDialog from '../components/health/VitalRecordDialog.vue'
import BaseLayout from '../components/common/BaseLayout.vue'
import * as echarts from 'echarts'
import { Delete, Plus, Refresh } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { marked } from 'marked'

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
  { value: 3, label: '体重', unit: 'kg', icon: '⚖️' }
]

const currentMemberId = computed(() => userStore.member?.id)

const isAdmin = computed(() => userStore.member?.role === 0)

const familyMembers = ref([])
const userProfile = ref(null)

const isViewingOther = computed(() => {
  return isAdmin.value && selectedMemberId.value && selectedMemberId.value !== userStore.member?.id
})

const currentViewerName = computed(() => {
  if (!isViewingOther.value) return ''
  const member = familyMembers.value.find(m => m.userId === selectedMemberId.value)
  return member?.nickname || ''
})

onMounted(async () => {
  try {
    if (isAdmin.value) {
      await loadFamilyMembers()
    }
    initSelectedMember()
    loadUserProfile()
    loadTodayVitals()
    loadWeeklyData()
    loadLatestReport()
    loadReportList()
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
    loadLatestReport()
    loadReportList()
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

const latestReport = ref(null)
const reportLoading = ref(false)
const reportList = ref([])
const vitalList = ref([])
const listLoading = ref(false)

const loadLatestReport = async () => {
  if (!selectedMember.value) return
  try {
    const res = await fetchLatestReport(selectedMember.value)
    latestReport.value = res.data
  } catch (e) {
    console.error('加载周报失败', e)
  }
}

const loadReportList = async () => {
  if (!selectedMember.value) return
  try {
    const res = await fetchHealthReports({
      userId: selectedMember.value,
      page: 1,
      size: 20
    })
    const allReports = res.data?.records || []
    
    // 过滤掉本周的周报，只保留历史周报
    const today = new Date()
    const weekStart = new Date(today)
    weekStart.setDate(today.getDate() - today.getDay())
    const weekEnd = new Date(weekStart)
    weekEnd.setDate(weekStart.getDate() + 6)
    
    reportList.value = allReports.filter(report => {
      const reportStart = new Date(report.weekStart)
      return reportStart < weekStart || reportStart.getTime() === weekStart.getTime()
    }).slice(0, 6)
  } catch (e) {
    console.error('加载历史周报失败', e)
  }
}

const handleGenerateReport = async () => {
  reportLoading.value = true
  try {
    await generateHealthReport(selectedMember.value)
    await loadLatestReport()
    ElMessage.success('周报生成成功')
  } catch (e) {
    console.error('生成周报失败', e)
    const msg = e?.message || ''
    if (msg.includes('暂无体征数据')) {
      ElMessageBox.alert('本周暂无体征数据，请先记录健康数据后再生成周报', '提示', {
        confirmButtonText: '知道了',
        type: 'warning'
      })
    } else {
      ElMessage.error('生成失败，请重试')
    }
  } finally {
    reportLoading.value = false
  }
}

const getRiskLevelInfo = (level) => {
  const map = {
    0: { label: '低风险', color: '#27ae60' },
    1: { label: '中风险', color: '#f39c12' },
    2: { label: '高风险', color: '#e74c3c' }
  }
  return map[level] || map[0]
}

// 周报详情弹窗
const reportDetailVisible = ref(false)
const currentReportDetail = ref(null)

const openReportDetail = (report) => {
  currentReportDetail.value = report
  reportDetailVisible.value = true
}

const renderedCurrentAiSummary = computed(() => {
  if (!currentReportDetail.value?.aiSummary) return ''
  return marked(currentReportDetail.value.aiSummary)
})

const renderedAiSummary = computed(() => {
  if (!latestReport.value?.aiSummary) return ''
  return marked(latestReport.value.aiSummary)
})

const trendAnalysis = computed(() => {
  const allData = weeklyData.value
  if (!allData || allData.length < 3) return null

  // 只使用当前类型的数据
  const data = allData.filter(d => d.type === currentType.value)
  if (data.length < 3) return null

  const now = new Date()
  const threeDaysAgo = new Date(now.getTime() - 3 * 24 * 60 * 60 * 1000)
  const sevenDaysAgo = new Date(now.getTime() - 7 * 24 * 60 * 60 * 1000)

  const recentData = data.filter(d => new Date(d.measureTime) >= threeDaysAgo)
  const earlierData = data.filter(d => {
    const time = new Date(d.measureTime)
    return time >= sevenDaysAgo && time < threeDaysAgo
  })

  if (recentData.length === 0 || earlierData.length === 0) return null

  if (currentType.value === 1) {
    const recentAvg = recentData.reduce((sum, d) => sum + (d.valueSystolic || 0), 0) / recentData.length
    const earlierAvg = earlierData.reduce((sum, d) => sum + (d.valueSystolic || 0), 0) / earlierData.length
    const diff = recentAvg - earlierAvg
    
    if (diff > 5) {
      return { text: '📈 血压呈上升趋势', color: '#e74c3c' }
    } else if (diff < -5) {
      return { text: '📉 血压有所下降', color: '#27ae60' }
    } else {
      return { text: '✅ 血压整体稳定', color: '#27ae60' }
    }
  }

  if (currentType.value === 2) {
    const recentFasting = recentData.filter(d => d.measurePoint === 1)
    const earlierFasting = earlierData.filter(d => d.measurePoint === 1)
    const recentPost = recentData.filter(d => d.measurePoint === 2)
    const earlierPost = earlierData.filter(d => d.measurePoint === 2)

    let result = []
    
    if (recentFasting.length > 0 && earlierFasting.length > 0) {
      const recentAvg = recentFasting.reduce((sum, d) => sum + (d.value || 0), 0) / recentFasting.length
      const earlierAvg = earlierFasting.reduce((sum, d) => sum + (d.value || 0), 0) / earlierFasting.length
      const diff = recentAvg - earlierAvg
      if (Math.abs(diff) > 0.5) {
        result.push({
          text: diff > 0 ? '📈 空腹血糖上升' : '📉 空腹血糖下降',
          color: diff > 0 ? '#f39c12' : '#27ae60'
        })
      }
    }

    if (recentPost.length > 0 && earlierPost.length > 0) {
      const recentAvg = recentPost.reduce((sum, d) => sum + (d.value || 0), 0) / recentPost.length
      const earlierAvg = earlierPost.reduce((sum, d) => sum + (d.value || 0), 0) / earlierPost.length
      const diff = recentAvg - earlierAvg
      if (Math.abs(diff) > 0.5) {
        result.push({
          text: diff > 0 ? '📈 餐后血糖上升' : '📉 餐后血糖下降',
          color: diff > 0 ? '#f39c12' : '#27ae60'
        })
      }
    }

    if (result.length === 0) {
      return { text: '✅ 血糖整体稳定', color: '#27ae60' }
    }
    return result.length === 2 ? { text: result[0].text + '，' + result[1].text, color: '#f39c12' } : result[0]
  }

  if (currentType.value === 3) {
    const recentAvg = recentData.reduce((sum, d) => sum + (d.value || 0), 0) / recentData.length
    const earlierAvg = earlierData.reduce((sum, d) => sum + (d.value || 0), 0) / earlierData.length
    const diff = recentAvg - earlierAvg
    
    if (Math.abs(diff) > 0.5) {
      return { text: diff > 0 ? '📈 体重上升' : '📉 体重下降', color: diff > 0 ? '#f39c12' : '#27ae60' }
    } else {
      return { text: '✅ 体重稳定', color: '#27ae60' }
    }
  }

  return null
})

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

const getVitalValue = (type, todayData) => {
  if (!todayData || todayData.length === 0) return '--'
  if (type === 1) {
    const bp = todayData.find(v => v.type === 1)
    return bp && bp.valueSystolic && bp.valueDiastolic 
      ? `${bp.valueSystolic}/${bp.valueDiastolic}` 
      : '--'
  }
  if (type === 2) {
    const fasting = todayData.find(v => v.type === 2 && v.measurePoint === 1)
    const postMeal = todayData.find(v => v.type === 2 && v.measurePoint === 2)
    const fastingVal = fasting?.value != null ? fasting.value : null
    const postMealVal = postMeal?.value != null ? postMeal.value : null
    if (!fastingVal && !postMealVal) return '--'
    if (fastingVal && postMealVal) {
      return `空腹${fastingVal} / 餐后${postMealVal}`
    }
    return fastingVal != null ? `空腹${fastingVal}` : `餐后${postMealVal}`
  }
  if (type === 3) {
    const wt = todayData.find(v => v.type === 3)
    return wt?.value != null ? wt.value : '--'
  }
  return '--'
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
  const today = new Date()
  const sevenDaysAgo = new Date(today.getTime() - 6 * 24 * 60 * 60 * 1000)
  const allDates = []
  for (let i = 0; i < 7; i++) {
    const d = new Date(sevenDaysAgo.getTime() + i * 24 * 60 * 60 * 1000)
    allDates.push(d.toLocaleDateString('zh-CN', { month: 'numeric', day: 'numeric' }))
  }

  const systolic = allDates.map(date => {
    const item = data.find(d => new Date(d.measureTime).toLocaleDateString('zh-CN', { month: 'numeric', day: 'numeric' }) === date)
    return item ? item.valueSystolic : null
  })
  
  const diastolic = allDates.map(date => {
    const item = data.find(d => new Date(d.measureTime).toLocaleDateString('zh-CN', { month: 'numeric', day: 'numeric' }) === date)
    return item ? item.valueDiastolic : null
  })

  // 动态计算Y轴范围
  const allBpValues = [...systolic, ...diastolic].filter(v => v != null)
  let yMin = 40, yMax = 200
  if (allBpValues.length > 0) {
    const min = Math.min(...allBpValues)
    const max = Math.max(...allBpValues)
    yMin = Math.max(40, Math.floor(min - 15))
    yMax = Math.min(200, Math.ceil(max + 15))
  }

  return {
    tooltip: { trigger: 'axis' },
    legend: { data: ['收缩压', '舒张压'], bottom: 0 },
    grid: { left: '3%', right: '4%', bottom: '15%', top: '10%', containLabel: true },
    xAxis: { type: 'category', data: allDates, boundaryGap: false },
    yAxis: { type: 'value', min: yMin, max: yMax },
    series: [
      { name: '收缩压', type: 'line', data: systolic, smooth: true, lineStyle: { color: '#e74c3c' }, itemStyle: { color: '#e74c3c' }, connectNulls: true },
      { name: '舒张压', type: 'line', data: diastolic, smooth: true, lineStyle: { color: '#3498db' }, itemStyle: { color: '#3498db' }, connectNulls: true }
    ]
  }
}

const getSingleValueOption = (data, type) => {
  const typeOpt = getTypeOption(type)

  // 血糖特殊处理：区分空腹和餐后
  if (type === 2) {
    const fastingData = data.filter(d => d.measurePoint === 1)
    const postMealData = data.filter(d => d.measurePoint === 2)
    
    // 生成完整7天日期
    const allDates = []
    const today = new Date()
    const sevenDaysAgo = new Date(today.getTime() - 6 * 24 * 60 * 60 * 1000)
    for (let i = 0; i < 7; i++) {
      const d = new Date(sevenDaysAgo.getTime() + i * 24 * 60 * 60 * 1000)
      allDates.push(d.toLocaleDateString('zh-CN', { month: 'numeric', day: 'numeric' }))
    }
    
    // 构建空值数组对齐日期
    const fastingValues = allDates.map(date => {
      const item = fastingData.find(d => new Date(d.measureTime).toLocaleDateString('zh-CN', { month: 'numeric', day: 'numeric' }) === date)
      return item ? item.value : null
    })
     
    const postMealValues = allDates.map(date => {
      const item = postMealData.find(d => new Date(d.measureTime).toLocaleDateString('zh-CN', { month: 'numeric', day: 'numeric' }) === date)
      return item ? item.value : null
    })

    // 动态计算Y轴范围
    const allBsValues = [...fastingValues, ...postMealValues].filter(v => v != null)
    let yMin = 0, yMax = 20
    if (allBsValues.length > 0) {
      const min = Math.min(...allBsValues)
      const max = Math.max(...allBsValues)
      yMin = Math.max(0, Math.floor(min - 2))
      yMax = Math.ceil(max + 2)
    }

    return {
      tooltip: { trigger: 'axis' },
      legend: { data: ['空腹', '餐后'], bottom: 0 },
      grid: { left: '3%', right: '4%', bottom: '15%', top: '10%', containLabel: true },
      xAxis: { type: 'category', data: allDates, boundaryGap: false },
      yAxis: { type: 'value', name: typeOpt?.unit, min: yMin, max: yMax },
      series: [
        { name: '空腹', type: 'line', data: fastingValues, smooth: true, lineStyle: { color: '#27ae60' }, itemStyle: { color: '#27ae60' }, connectNulls: true },
        { name: '餐后', type: 'line', data: postMealValues, smooth: true, lineStyle: { color: '#f39c12' }, itemStyle: { color: '#f39c12' }, connectNulls: true }
      ]
    }
  }

  // 体重：生成完整7天日期
  const allDates = []
  const today = new Date()
  const sevenDaysAgo = new Date(today.getTime() - 6 * 24 * 60 * 60 * 1000)
  for (let i = 0; i < 7; i++) {
    const d = new Date(sevenDaysAgo.getTime() + i * 24 * 60 * 60 * 1000)
    allDates.push(d.toLocaleDateString('zh-CN', { month: 'numeric', day: 'numeric' }))
  }
  
  const values = allDates.map(date => {
    const item = data.find(d => new Date(d.measureTime).toLocaleDateString('zh-CN', { month: 'numeric', day: 'numeric' }) === date)
    return item ? item.value : null
  })

  // 动态计算Y轴范围
  const validValues = values.filter(v => v != null)
  let yMin = 0, yMax = 150
  if (validValues.length > 0) {
    const min = Math.min(...validValues)
    const max = Math.max(...validValues)
    yMin = Math.max(0, Math.floor(min - 3))
    yMax = Math.ceil(max + 3)
  }

  return {
    tooltip: { trigger: 'axis' },
    legend: { data: [typeOpt?.label], bottom: 0 },
    grid: { left: '3%', right: '4%', bottom: '15%', top: '10%', containLabel: true },
    xAxis: { type: 'category', data: allDates, boundaryGap: false },
    yAxis: { type: 'value', name: typeOpt?.unit, min: yMin, max: yMax },
    series: [{
      name: typeOpt?.label,
      type: 'line',
      data: values,
      smooth: true,
      lineStyle: { color: '#1abc9c' },
      itemStyle: { color: '#1abc9c' },
      connectNulls: true
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
  
  window.addEventListener('resize', handleResize)
}

const handleResize = () => {
  if (chartInstance) {
    chartInstance.resize()
  }
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
  window.removeEventListener('resize', handleResize)
})
</script>

<template>
  <div class="health-page">
    <BaseLayout>
      <div class="page-header">
        <h1 class="page-title">
          健康数据
          <span v-if="isViewingOther" class="viewer-tag">查看：{{ currentViewerName }}</span>
        </h1>
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
              {{ getVitalValue(t.value, todayVitals) }}
            </div>
            <div class="card-label">{{ t.label }}</div>
            <div class="card-unit">
              {{ t.value === 2 ? (todayVitals.some(v => v.type === 2) ? 'mmol/L' : '点击记录') : (todayVitals.some(v => v.type === t.value) ? t.unit : '点击记录') }}
            </div>
            <div class="card-add">
              <Plus />
            </div>
          </div>
        </div>
      </section>

      <section class="trend-chart">
        <div class="trend-header">
          <h2 class="section-title">近7天趋势</h2>
          <div v-if="trendAnalysis" class="trend-analysis" :style="{ color: trendAnalysis.color }">
            {{ trendAnalysis.text }}
          </div>
        </div>
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

      <section class="health-report">
        <h2 class="section-title">健康周报</h2>
        
        <!-- 本周周报卡片 -->
        <div class="current-report-section">
          <h3 class="section-subtitle">本周周报</h3>
          <div v-if="latestReport" class="current-report-card" @click="openReportDetail(latestReport)">
            <div class="card-main">
              <div class="card-period">{{ latestReport.weekStart?.slice(5) }} ~ {{ latestReport.weekEnd?.slice(5) }}</div>
              <div class="card-risk" :style="{ color: getRiskLevelInfo(latestReport.riskLevel).color }">
                {{ getRiskLevelInfo(latestReport.riskLevel).label }}
              </div>
            </div>
            <div class="card-info">
              <span class="info-item">用药依从率: {{ latestReport.complianceRate }}%</span>
              <span class="info-arrow">查看详情 ></span>
            </div>
          </div>
          <div v-else class="report-empty">
            <p>暂无本周周报</p>
            <el-button type="primary" @click="handleGenerateReport" :loading="reportLoading">
              生成周报
            </el-button>
          </div>
          <div v-if="latestReport" class="report-actions">
            <el-button size="small" @click="handleGenerateReport" :loading="reportLoading" class="regen-btn">
              <Refresh class="btn-icon" /> 重新生成
            </el-button>
          </div>
        </div>

        <!-- 历史周报 -->
        <div v-if="reportList.length > 0" class="history-reports">
          <h3 class="section-subtitle">历史周报</h3>
          <div class="report-grid">
            <div 
              v-for="report in reportList.slice(0, 6)" 
              :key="report.id" 
              class="report-mini-card"
              @click="openReportDetail(report)"
            >
              <div class="mini-period">{{ report.weekStart?.slice(5) }} ~ {{ report.weekEnd?.slice(5) }}</div>
              <div class="mini-risk" :style="{ color: getRiskLevelInfo(report.riskLevel).color }">
                {{ getRiskLevelInfo(report.riskLevel).label }}
              </div>
              <div class="mini-compliance">{{ report.complianceRate }}% 依从率</div>
            </div>
          </div>
        </div>
      </section>

      <!-- 周报详情弹窗 -->
      <el-dialog v-model="reportDetailVisible" title="周报详情" width="90%" :close-on-click-modal="true">
        <div v-if="currentReportDetail" class="detail-content">
          <div class="detail-header">
            <span class="detail-period">{{ currentReportDetail.weekStart }} ~ {{ currentReportDetail.weekEnd }}</span>
            <span class="detail-risk" :style="{ color: getRiskLevelInfo(currentReportDetail.riskLevel).color }">
              {{ getRiskLevelInfo(currentReportDetail.riskLevel).label }}
            </span>
          </div>
          <div class="detail-compliance">
            <span class="label">用药依从率：</span>
            <span class="value">{{ currentReportDetail.complianceRate }}%</span>
          </div>
          <div class="detail-ai">
            <div class="ai-title">AI 健康建议</div>
            <div class="ai-content" v-html="renderedCurrentAiSummary"></div>
          </div>
        </div>
      </el-dialog>

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
  display: flex;
  align-items: center;
  gap: 12px;
}

.viewer-tag {
  font-size: 0.8rem;
  font-weight: 500;
  color: #fff;
  background: #2d5f5d;
  padding: 4px 10px;
  border-radius: 12px;
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
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
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
  min-height: 100px;
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

.trend-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  flex-wrap: wrap;
  gap: 12px;
}

.trend-header .section-title {
  margin: 0;
}

.trend-analysis {
  font-size: 0.9rem;
  font-weight: 500;
  padding: 6px 12px;
  border-radius: 16px;
  background: rgba(0, 0, 0, 0.04);
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

.health-report {
  margin-bottom: 32px;
}

/* 本周周报卡片 */
.current-report-section {
  margin-bottom: 20px;
}

.current-report-card {
  background: rgba(255, 255, 255, 0.7);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  border-radius: 16px;
  padding: 20px;
  color: #2d5f5d;
  cursor: pointer;
  transition: all 0.3s;
  box-shadow: 0 8px 32px rgba(31, 38, 135, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.5);
  position: relative;
  overflow: hidden;
}

.current-report-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  width: 3px;
  height: 100%;
  background: linear-gradient(180deg, #7fcfb8 0%, #a8e6cf 100%);
}

.current-report-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 12px 40px rgba(31, 38, 135, 0.12);
}

.card-main {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.card-period {
  font-size: 1rem;
  font-weight: 600;
}

.card-risk {
  font-size: 0.85rem;
  font-weight: 600;
  padding: 4px 12px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.2);
}

.card-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 0.85rem;
  opacity: 0.9;
}

.info-arrow {
  font-size: 0.8rem;
  opacity: 0.8;
}

.report-card {
  background: #fff;
  border-radius: 16px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.report-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.report-period {
  font-size: 0.9rem;
  color: #666;
}

.report-risk {
  font-size: 0.85rem;
  font-weight: 600;
  padding: 4px 10px;
  border-radius: 12px;
  background: rgba(0, 0, 0, 0.04);
}

.report-compliance {
  margin-bottom: 16px;
  padding: 12px;
  background: #f8f9fa;
  border-radius: 8px;
}

.report-compliance .label {
  color: #666;
  font-size: 0.9rem;
}

.report-compliance .value {
  color: #2d5f5d;
  font-size: 1.1rem;
  font-weight: 600;
}

.report-ai {
  border-top: 1px solid #eee;
  padding-top: 16px;
}

.ai-title {
  font-size: 0.9rem;
  font-weight: 600;
  color: #2d5f5d;
  margin-bottom: 8px;
}

.ai-content {
  font-size: 0.9rem;
  line-height: 1.6;
  color: #333;
}

.ai-content h1, .ai-content h2, .ai-content h3 {
  font-size: 1rem;
  font-weight: 600;
  color: #2d5f5d;
  margin: 12px 0 8px 0;
}

.ai-content h4 {
  font-size: 0.95rem;
  font-weight: 600;
  color: #333;
  margin: 10px 0 6px 0;
}

.ai-content p {
  margin: 8px 0;
}

.ai-content ul, .ai-content ol {
  padding-left: 20px;
  margin: 8px 0;
}

.ai-content li {
  margin: 4px 0;
}

.ai-content strong {
  color: #2d5f5d;
}

.ai-content em {
  color: #666;
}

.ai-content code {
  background: #f5f5f5;
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 0.85rem;
}

.ai-content p:first-child {
  margin-top: 0;
}

.report-empty {
  text-align: center;
  padding: 40px;
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.report-empty p {
  color: #999;
  margin-bottom: 16px;
}

.report-actions {
  text-align: center;
  margin-top: 12px;
}

.btn-icon {
  width: 14px;
  height: 14px;
  margin-right: 4px;
}

.regen-btn {
  background: rgba(255, 255, 255, 0.7);
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
  border: 1px solid rgba(255, 255, 255, 0.5);
  color: #2d5f5d;
  box-shadow: 0 4px 16px rgba(31, 38, 135, 0.06);
}

.regen-btn:hover {
  background: rgba(255, 255, 255, 0.85);
  box-shadow: 0 6px 20px rgba(31, 38, 135, 0.1);
}

/* 历史周报 */
.history-reports {
  margin-top: 24px;
}

.section-subtitle {
  font-size: 1rem;
  font-weight: 600;
  color: #666;
  margin: 0 0 12px 0;
}

.report-grid {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 12px;
}

.report-mini-card {
  background: rgba(255, 255, 255, 0.7);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  border-radius: 12px;
  padding: 12px;
  text-align: center;
  cursor: pointer;
  transition: all 0.3s;
  box-shadow: 0 4px 16px rgba(31, 38, 135, 0.06);
  border: 1px solid rgba(255, 255, 255, 0.5);
}

.report-mini-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(31, 38, 135, 0.1);
}

.mini-period {
  font-size: 0.8rem;
  color: #666;
  margin-bottom: 6px;
}

.mini-risk {
  font-size: 0.85rem;
  font-weight: 600;
  margin-bottom: 4px;
}

.mini-compliance {
  font-size: 0.75rem;
  color: #999;
}

/* 周报详情弹窗 */
.detail-content {
  padding: 8px 0;
}

.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #eee;
}

.detail-period {
  font-size: 1rem;
  font-weight: 600;
  color: #333;
}

.detail-risk {
  font-size: 0.9rem;
  font-weight: 600;
  padding: 4px 12px;
  border-radius: 12px;
  background: rgba(0, 0, 0, 0.04);
}

.detail-compliance {
  margin-bottom: 16px;
  padding: 12px;
  background: #f8f9fa;
  border-radius: 8px;
}

.detail-compliance .label {
  color: #666;
  font-size: 0.95rem;
}

.detail-compliance .value {
  color: #2d5f5d;
  font-size: 1.1rem;
  font-weight: 600;
}

.detail-ai {
  border-top: 1px solid #eee;
  padding-top: 16px;
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

  .report-grid {
    grid-template-columns: repeat(3, 1fr);
  }

  .report-mini-card {
    padding: 10px;
    background: rgba(255, 255, 255, 0.75);
  }

  .mini-period {
    font-size: 0.7rem;
  }

  .mini-risk {
    font-size: 0.75rem;
  }

  .mini-compliance {
    font-size: 0.65rem;
  }
}
</style>