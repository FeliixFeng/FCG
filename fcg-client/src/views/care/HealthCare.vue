<script setup>
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import { useRoute, useRouter } from 'vue-router'
import BaseLayout from '../../components/common/BaseLayout.vue'
import CareVitalRecordDialog from '../../components/health/CareVitalRecordDialog.vue'
import { useUserStore } from '../../stores/user'
import { fetchTodayVitals, fetchWeeklyVitals } from '../../utils/api'

const userStore = useUserStore()
const route = useRoute()
const router = useRouter()
const loading = ref(false)
const todayVitals = ref([])
const dialogVisible = ref(false)
const dialogType = ref(1)
const currentType = ref(1)
const weeklyData = ref([])
const chartRef = ref(null)
let chartInstance = null

const vitalTypes = [
  { type: 1, label: '血压', icon: '❤️', unit: 'mmHg' },
  { type: 2, label: '血糖', icon: '🩸', unit: 'mmol/L' },
  { type: 3, label: '体重', icon: '⚖️', unit: 'kg' }
]

const memberName = computed(() => userStore.member?.nickname || '家人')
const currentUserId = computed(() => Number(userStore.member?.id || userStore.member?.userId || 0))

const todayMap = computed(() => {
  const map = {}
  for (const item of todayVitals.value) {
    if (!item?.type) continue
    if (!map[item.type]) {
      map[item.type] = item
      continue
    }
    const current = new Date(map[item.type].measureTime || 0).getTime()
    const next = new Date(item.measureTime || 0).getTime()
    if (!Number.isNaN(next) && (Number.isNaN(current) || next > current)) {
      map[item.type] = item
    }
  }
  return map
})

function formatVitalValue(record) {
  if (!record) return '未录入'
  if (record.type === 1) {
    const sys = record.valueSystolic ?? '--'
    const dia = record.valueDiastolic ?? '--'
    return `${sys}/${dia} mmHg`
  }
  if (record.type === 2) return formatSugarPairValue()
  if (record.type === 3) {
    const value = record.value ?? '--'
    return `${value} kg`
  }
  return '未录入'
}

function formatSugarPairValue() {
  const sugarList = todayVitals.value.filter(item => item.type === 2)
  if (!sugarList.length) return '未录入'

  const latestByPoint = { 1: null, 2: null }
  sugarList.forEach((item) => {
    const point = item.measurePoint
    if (point !== 1 && point !== 2) return
    const prev = latestByPoint[point]
    const currTime = new Date(item.measureTime || 0).getTime()
    const prevTime = prev ? new Date(prev.measureTime || 0).getTime() : 0
    if (!prev || currTime >= prevTime) {
      latestByPoint[point] = item
    }
  })

  const fasting = latestByPoint[1]?.value
  const post = latestByPoint[2]?.value
  return `空 ${fasting ?? '--'} / 餐 ${post ?? '--'} mmol/L`
}

function isTypeRecordedToday(type) {
  return Boolean(todayMap.value[type])
}

function getStatusText(type) {
  return isTypeRecordedToday(type) ? '今日已录入' : '今日未录入'
}

function openRecordDialog(type) {
  dialogType.value = type
  dialogVisible.value = true
}

const clearQuickActionQuery = async () => {
  if (!route.query?.action) return
  const nextQuery = { ...route.query }
  delete nextQuery.action
  delete nextQuery.type
  await router.replace({ name: route.name, query: nextQuery })
}

const handleQuickAction = async () => {
  const action = route.query?.action
  if (!action) return
  if (action === 'record-vital') {
    const type = Number(route.query?.type || 1)
    openRecordDialog([1, 2, 3].includes(type) ? type : 1)
    await clearQuickActionQuery()
  }
}

function typeLabel(type) {
  return vitalTypes.find(item => item.type === type)?.label || '体征'
}

function getTimeValue(item) {
  const d = new Date(item?.measureTime || item?.measureDate || '')
  const t = d.getTime()
  return Number.isNaN(t) ? 0 : t
}

function mean(list) {
  if (!list.length) return 0
  return list.reduce((sum, n) => sum + Number(n || 0), 0) / list.length
}

function pickTrendValues(type, list) {
  if (type === 1) return list.map(item => item.valueSystolic).filter(v => v !== null && v !== undefined)
  return list.map(item => item.value).filter(v => v !== null && v !== undefined)
}

function trendMeta(type, delta) {
  if (type === 1) {
    if (delta >= 4) return { text: '近7天血压整体上升，请关注波动', tone: 'up' }
    if (delta <= -4) return { text: '近7天血压有所下降，继续保持', tone: 'down' }
    return { text: '近7天血压基本稳定', tone: 'stable' }
  }
  if (type === 2) {
    if (delta >= 0.4) return { text: '近7天血糖略有上升，注意饮食', tone: 'up' }
    if (delta <= -0.4) return { text: '近7天血糖有所下降，状态不错', tone: 'down' }
    return { text: '近7天血糖基本稳定', tone: 'stable' }
  }
  if (delta >= 0.4) return { text: '近7天体重略有上升', tone: 'up' }
  if (delta <= -0.4) return { text: '近7天体重略有下降', tone: 'down' }
  return { text: '近7天体重基本稳定', tone: 'stable' }
}

const trendSummary = computed(() => {
  const list = [...weeklyData.value].sort((a, b) => getTimeValue(a) - getTimeValue(b))
  if (!list.length) {
    return { text: '暂无趋势数据，请先录入体征', tone: 'empty' }
  }

  const values = pickTrendValues(currentType.value, list)
  if (values.length < 2) {
    return { text: '数据较少，继续录入可生成更准确趋势', tone: 'empty' }
  }

  const windowSize = Math.max(1, Math.floor(values.length / 3))
  const early = values.slice(0, windowSize)
  const late = values.slice(values.length - windowSize)
  const delta = mean(late) - mean(early)

  return trendMeta(currentType.value, delta)
})

async function loadTodayData() {
  if (!currentUserId.value) return
  try {
    const res = await fetchTodayVitals(currentUserId.value)
    todayVitals.value = res?.data || []
  } catch (err) {
    todayVitals.value = []
    ElMessage.error('加载今日体征失败')
  }
}

async function loadWeeklyData() {
  if (!currentUserId.value) return
  try {
    const res = await fetchWeeklyVitals(currentUserId.value, currentType.value)
    weeklyData.value = res?.data || []
    updateChart()
  } catch (err) {
    weeklyData.value = []
    updateChart()
  }
}

async function reloadAllData() {
  if (!currentUserId.value) return
  loading.value = true
  try {
    await Promise.all([loadTodayData(), loadWeeklyData()])
  } finally {
    loading.value = false
  }
}

function onRecordSuccess() {
  reloadAllData()
}

function changeTrendType(type) {
  if (currentType.value === type) return
  currentType.value = type
}

function buildBloodSugarSeries(source) {
  const fastingMap = {}
  const postMap = {}
  source.forEach((item) => {
    const key = toDayLabel(item.measureDate || item.measureTime)
    if (!key) return
    if (item.measurePoint === 1) fastingMap[key] = item.value
    if (item.measurePoint === 2) postMap[key] = item.value
  })
  const labels = Object.keys({ ...fastingMap, ...postMap }).sort()
  return {
    labels,
    series: [
      { name: '空腹', data: labels.map(key => fastingMap[key] ?? null) },
      { name: '餐后', data: labels.map(key => postMap[key] ?? null) }
    ]
  }
}

function getLast7DayLabels() {
  const labels = []
  const today = new Date()
  for (let i = 6; i >= 0; i -= 1) {
    const d = new Date(today)
    d.setDate(today.getDate() - i)
    labels.push(d.toLocaleDateString('zh-CN', { month: 'numeric', day: 'numeric' }))
  }
  return labels
}

function toDayLabel(raw) {
  if (!raw) return ''
  const d = new Date(raw)
  if (Number.isNaN(d.getTime())) return ''
  return d.toLocaleDateString('zh-CN', { month: 'numeric', day: 'numeric' })
}

function getAxisRange(values, { minPad = 1, step = 1, floor = null } = {}) {
  const valid = values.filter(v => v !== null && v !== undefined && Number.isFinite(Number(v))).map(Number)
  if (!valid.length) return {}
  const min = Math.min(...valid)
  const max = Math.max(...valid)
  const span = Math.max(max - min, step)
  const pad = Math.max(minPad, span * 0.2)

  let low = min - pad
  let high = max + pad
  if (floor !== null) {
    low = Math.max(floor, low)
  }

  low = Math.floor(low / step) * step
  high = Math.ceil(high / step) * step

  if (high - low < step * 2) {
    high = low + step * 2
  }
  return { min: low, max: high }
}

function updateChart() {
  if (!chartInstance) return
  const source = weeklyData.value || []
  const type = currentType.value
  const labels = getLast7DayLabels()
  let series = []

  if (!source.length) {
    chartInstance.clear()
    chartInstance.setOption({
      title: {
        text: `暂无${typeLabel(type)}数据`,
        subtext: '请点击上方对应卡片进行录入',
        left: 'center',
        top: 'middle',
        textStyle: { color: '#6b8583', fontSize: 16, fontWeight: 600 },
        subtextStyle: { color: '#8aa09e', fontSize: 13 }
      },
      xAxis: { show: false },
      yAxis: { show: false },
      series: []
    }, true)
    return
  }

  if (type === 1) {
    const sysMap = {}
    const diaMap = {}
    source.forEach((item) => {
      const key = toDayLabel(item.measureDate || item.measureTime)
      if (!key) return
      sysMap[key] = item.valueSystolic ?? null
      diaMap[key] = item.valueDiastolic ?? null
    })
    const systolicData = labels.map(key => sysMap[key] ?? null)
    const diastolicData = labels.map(key => diaMap[key] ?? null)
    series = [
      {
        name: '收缩压',
        type: 'line',
        smooth: true,
        symbol: 'circle',
        symbolSize: 7,
        data: systolicData,
        lineStyle: { width: 3, color: '#e74c3c' },
        itemStyle: { color: '#e74c3c' },
        connectNulls: true
      },
      {
        name: '舒张压',
        type: 'line',
        smooth: true,
        symbol: 'circle',
        symbolSize: 7,
        data: diastolicData,
        lineStyle: { width: 3, color: '#3498db' },
        itemStyle: { color: '#3498db' },
        connectNulls: true
      }
    ]
  } else if (type === 2) {
    const sugar = buildBloodSugarSeries(source)
    const fastingData = labels.map(key => {
      const idx = sugar.labels.indexOf(key)
      return idx >= 0 ? sugar.series[0].data[idx] : null
    })
    const postData = labels.map(key => {
      const idx = sugar.labels.indexOf(key)
      return idx >= 0 ? sugar.series[1].data[idx] : null
    })
    series = [
      {
        name: '空腹',
        type: 'line',
        smooth: true,
        symbol: 'circle',
        symbolSize: 7,
        data: fastingData,
        lineStyle: { width: 3, color: '#27ae60' },
        itemStyle: { color: '#27ae60' },
        connectNulls: true
      },
      {
        name: '餐后',
        type: 'line',
        smooth: true,
        symbol: 'circle',
        symbolSize: 7,
        data: postData,
        lineStyle: { width: 3, color: '#f39c12' },
        itemStyle: { color: '#f39c12' },
        connectNulls: true
      }
    ]
  } else {
    const weightMap = {}
    source.forEach((item) => {
      const key = toDayLabel(item.measureDate || item.measureTime)
      if (!key) return
      weightMap[key] = item.value ?? null
    })
    const weightData = labels.map(key => weightMap[key] ?? null)
    series = [
      {
        name: '体重',
        type: 'line',
        smooth: true,
        symbol: 'circle',
        symbolSize: 7,
        data: weightData,
        lineStyle: { width: 3, color: '#1abc9c' },
        itemStyle: { color: '#1abc9c' },
        connectNulls: true
      }
    ]
  }

  const allValues = series.flatMap(item => item.data || [])
  const hasPlotData = allValues.some(v => v !== null && v !== undefined)
  if (!hasPlotData) {
    chartInstance.clear()
    chartInstance.setOption({
      title: {
        text: `暂无${typeLabel(type)}数据`,
        subtext: '请点击上方对应卡片进行录入',
        left: 'center',
        top: 'middle',
        textStyle: { color: '#6b8583', fontSize: 16, fontWeight: 600 },
        subtextStyle: { color: '#8aa09e', fontSize: 13 }
      },
      xAxis: { show: false },
      yAxis: { show: false },
      series: []
    }, true)
    return
  }

  let yAxisRange = {}
  if (type === 1) {
    yAxisRange = getAxisRange(allValues, { minPad: 8, step: 5, floor: 40 })
  } else if (type === 2) {
    yAxisRange = getAxisRange(allValues, { minPad: 0.8, step: 0.5, floor: 0 })
  } else {
    yAxisRange = getAxisRange(allValues, { minPad: 1.5, step: 1, floor: 0 })
  }

  chartInstance.setOption({
    animation: false,
    tooltip: { trigger: 'axis' },
    grid: { left: 24, right: 18, top: 28, bottom: 26 },
    legend: { show: series.length > 1, top: 0, right: 0, textStyle: { color: '#5f7a77', fontSize: 12 } },
    xAxis: {
      type: 'category',
      data: labels,
      axisLine: { lineStyle: { color: 'rgba(45,95,93,0.2)' } },
      axisLabel: { color: '#6b8583', fontSize: 12 }
    },
    yAxis: {
      type: 'value',
      min: yAxisRange.min,
      max: yAxisRange.max,
      axisLine: { show: false },
      splitLine: { lineStyle: { color: 'rgba(45,95,93,0.12)' } },
      axisLabel: { color: '#6b8583', fontSize: 12 }
    },
    series
  }, true)
}

async function initChart() {
  await nextTick()
  if (!chartRef.value) return
  if (chartInstance) chartInstance.dispose()
  chartInstance = echarts.init(chartRef.value)
  updateChart()
}

function handleResize() {
  chartInstance?.resize()
}

watch(currentType, () => {
  loadWeeklyData()
})

onMounted(() => {
  reloadAllData()
  initChart()
  window.addEventListener('resize', handleResize)
  handleQuickAction()
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  if (chartInstance) {
    chartInstance.dispose()
    chartInstance = null
  }
})

watch(
  () => route.query.action,
  async (val, oldVal) => {
    if (!val || val === oldVal) return
    await handleQuickAction()
  }
)
</script>

<template>
  <BaseLayout>
    <div class="care-health-page" v-loading="loading">
      <section class="hero-card">
        <p class="hero-badge">关怀模式 · 健康</p>
        <h2 class="hero-title">{{ memberName }}，今天的体征记录</h2>
      </section>

      <div class="status-grid">
        <article
          v-for="item in vitalTypes"
          :key="item.type"
          class="status-card"
          :class="[{ done: isTypeRecordedToday(item.type) }, `type-${item.type}`]"
        >
          <p class="status-name">{{ item.icon }} {{ item.label }}</p>
          <p class="status-value">{{ formatVitalValue(todayMap[item.type]) }}</p>
          <p class="status-tip">{{ getStatusText(item.type) }}</p>
          <button class="record-btn" @click="openRecordDialog(item.type)">
            录入{{ item.label }}
          </button>
        </article>
      </div>

      <section class="trend-card">
        <div class="section-head">
          <h3>最近趋势</h3>
          <span>{{ typeLabel(currentType) }}</span>
        </div>
        <p class="trend-summary" :class="`is-${trendSummary.tone}`">{{ trendSummary.text }}</p>
        <div class="trend-switch">
          <button
            v-for="item in vitalTypes"
            :key="`switch-${item.type}`"
            class="switch-btn"
            :class="{ active: currentType === item.type }"
            @click="changeTrendType(item.type)"
          >
            {{ item.icon }} {{ item.label }}
          </button>
        </div>
        <div ref="chartRef" class="trend-chart"></div>
      </section>

      <CareVitalRecordDialog
        v-model:visible="dialogVisible"
        :type="dialogType"
        :user-id="currentUserId"
        @success="onRecordSuccess"
      />
    </div>
  </BaseLayout>
</template>

<style scoped>
.care-health-page {
  width: 100%;
  max-width: 1200px;
  margin: 0 auto;
  padding: 0;
  display: grid;
  gap: 14px;
}

.hero-card,
.trend-card,
.status-card {
  background: rgba(255, 255, 255, 0.92);
  border: 1px solid rgba(45, 95, 93, 0.14);
  border-radius: 18px;
  box-shadow: 0 8px 22px rgba(45, 95, 93, 0.07);
}

.hero-card {
  padding: 12px 14px;
}

.hero-badge {
  margin: 0 0 8px;
  display: inline-flex;
  align-items: center;
  height: 30px;
  padding: 0 12px;
  border-radius: 999px;
  font-size: 0.92rem;
  color: #2d5f5d;
  background: rgba(45, 95, 93, 0.1);
  border: 1px solid rgba(45, 95, 93, 0.16);
}

.hero-title {
  margin: 0;
  font-size: 1.54rem;
  color: #1f4341;
}

.status-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.status-card {
  position: relative;
  overflow: hidden;
  padding: 11px 12px;
  min-height: 176px;
  display: grid;
  gap: 7px;
  border-style: solid;
  border-color: rgba(45, 95, 93, 0.16);
  background:
    radial-gradient(180px 70px at 100% 0%, rgba(45, 95, 93, 0.08) 0%, transparent 72%),
    linear-gradient(150deg, rgba(255, 255, 255, 0.96), rgba(245, 251, 249, 0.92));
}

.status-card::before {
  content: '';
  position: absolute;
  left: 0;
  top: 12px;
  bottom: 12px;
  width: 3px;
  border-radius: 99px;
  background: rgba(45, 95, 93, 0.24);
}

.status-card.type-1 {
  border-color: rgba(231, 76, 60, 0.24);
  background:
    radial-gradient(210px 82px at 100% 0%, rgba(231, 76, 60, 0.12) 0%, transparent 72%),
    linear-gradient(145deg, rgba(255, 255, 255, 0.98), rgba(254, 244, 243, 0.96));
}

.status-card.type-1::before {
  background: rgba(231, 76, 60, 0.42);
}

.status-card.type-2 {
  border-color: rgba(243, 156, 18, 0.24);
  background:
    radial-gradient(210px 82px at 100% 0%, rgba(243, 156, 18, 0.13) 0%, transparent 72%),
    linear-gradient(145deg, rgba(255, 255, 255, 0.98), rgba(255, 248, 236, 0.96));
}

.status-card.type-2::before {
  background: rgba(243, 156, 18, 0.44);
}

.status-card.type-3 {
  border-color: rgba(26, 188, 156, 0.24);
  background:
    radial-gradient(210px 82px at 100% 0%, rgba(26, 188, 156, 0.12) 0%, transparent 72%),
    linear-gradient(145deg, rgba(255, 255, 255, 0.98), rgba(238, 251, 248, 0.96));
}

.status-card.type-3::before {
  background: rgba(26, 188, 156, 0.44);
}

.status-card.done {
  box-shadow:
    0 10px 18px rgba(45, 95, 93, 0.1),
    inset 0 0 0 1px rgba(45, 95, 93, 0.1);
}

.status-card.type-1 .record-btn {
  border-color: rgba(231, 76, 60, 0.2);
  background: linear-gradient(145deg, rgba(255, 250, 249, 1), rgba(254, 244, 243, 1));
  color: #8b3d33;
}

.status-card.type-1 .record-btn:hover {
  border-color: rgba(231, 76, 60, 0.28);
  box-shadow: 0 8px 14px rgba(231, 76, 60, 0.11);
}

.status-card.type-2 .record-btn {
  border-color: rgba(243, 156, 18, 0.2);
  background: linear-gradient(145deg, rgba(255, 252, 247, 1), rgba(255, 248, 236, 1));
  color: #99631b;
}

.status-card.type-2 .record-btn:hover {
  border-color: rgba(243, 156, 18, 0.3);
  box-shadow: 0 8px 14px rgba(243, 156, 18, 0.11);
}

.status-card.type-3 .record-btn {
  border-color: rgba(26, 188, 156, 0.2);
  background: linear-gradient(145deg, rgba(247, 255, 253, 1), rgba(238, 251, 248, 1));
  color: #2b7d6d;
}

.status-card.type-3 .record-btn:hover {
  border-color: rgba(26, 188, 156, 0.3);
  box-shadow: 0 8px 14px rgba(26, 188, 156, 0.11);
}

.status-name {
  margin: 0;
  font-size: 1.12rem;
  color: #244b49;
  font-weight: 700;
  padding-left: 8px;
}

.status-value {
  margin: 0;
  font-size: 1.38rem;
  color: #1f3f3d;
  font-weight: 700;
  line-height: 1.25;
  padding-left: 8px;
}

.status-tip {
  margin: 0;
  font-size: 0.96rem;
  color: #607d7a;
  padding-left: 8px;
}

.section-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 10px;
}

.section-head h3 {
  margin: 0;
  font-size: 1.4rem;
  color: #234846;
}

.section-head span {
  font-size: 0.92rem;
  color: #6b8583;
}

.trend-card {
  padding: 12px;
  min-height: 0;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.trend-summary {
  margin: 0;
  font-size: 0.95rem;
  font-weight: 600;
  line-height: 1.4;
}

.trend-summary.is-up {
  color: #b85749;
}

.trend-summary.is-down {
  color: #2d7f5e;
}

.trend-summary.is-stable {
  color: #2d5f5d;
}

.trend-summary.is-empty {
  color: #6d8683;
}

.record-btn {
  min-height: 42px;
  border: 1px solid rgba(45, 95, 93, 0.2);
  border-radius: 11px;
  background: linear-gradient(145deg, #f7fcfb 0%, #edf7f3 100%);
  color: #25504d;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 0 14px;
  cursor: pointer;
  transition: transform 0.16s ease, box-shadow 0.2s ease, border-color 0.2s ease;
  font-size: 1.08rem;
  font-weight: 700;
}

.record-btn:hover {
  transform: translateY(-1px) scale(1.005);
  box-shadow: 0 8px 14px rgba(45, 95, 93, 0.14);
  border-color: rgba(45, 95, 93, 0.34);
}

.trend-switch {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
}

.switch-btn {
  height: 48px;
  border: 1px solid rgba(45, 95, 93, 0.2);
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.88);
  color: #2c5552;
  font-size: 1.04rem;
  font-weight: 700;
  cursor: pointer;
  transition: transform 0.16s ease, border-color 0.2s ease, background 0.2s ease;
}

.switch-btn:hover {
  transform: translateY(-1px);
  border-color: rgba(45, 95, 93, 0.32);
}

.switch-btn.active {
  background: linear-gradient(145deg, rgba(47, 143, 117, 0.14), rgba(47, 143, 117, 0.08));
  border-color: rgba(47, 143, 117, 0.42);
  color: #214f4c;
}

.trend-chart {
  height: 272px;
  min-height: 0;
  flex: 1;
  width: 100%;
}

@media (min-width: 768px) {
  .care-health-page {
    height: calc(100vh - 116px);
    min-height: calc(100vh - 116px);
    grid-template-rows: auto auto minmax(0, 1fr);
    overflow: hidden;
    padding-bottom: 0;
  }
}

@media (max-width: 767px) {
  .care-health-page {
    gap: 12px;
    padding-bottom: 92px;
    height: auto;
    overflow: visible;
  }

  .hero-card {
    padding: 14px;
  }

  .hero-title {
    font-size: 1.46rem;
  }

  .status-grid { 
    grid-template-columns: repeat(3, minmax(0, 1fr));
    gap: 8px;
  }

  .status-card {
    gap: 4px;
    padding: 8px 8px 10px;
    border-radius: 12px;
    min-height: 148px;
  }

  .status-name {
    font-size: 0.98rem;
    padding-left: 4px;
  }

  .status-value {
    font-size: 1rem;
    line-height: 1.2;
    padding-left: 4px;
  }

  .status-tip {
    font-size: 0.84rem;
    padding-left: 4px;
  }

  .trend-card {
    padding: 14px;
    gap: 10px;
    min-height: 364px;
  }

  .trend-summary {
    font-size: 0.88rem;
  }

  .trend-switch {
    grid-template-columns: repeat(3, minmax(0, 1fr));
    gap: 6px;
  }

  .switch-btn {
    height: 42px;
    font-size: 0.9rem;
    border-radius: 12px;
  }

  .record-btn {
    min-height: 32px;
    font-size: 0.84rem;
    padding: 0 6px;
    border-radius: 9px;
  }

  .trend-chart {
    height: 236px;
  }
}
</style>
