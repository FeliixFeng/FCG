<script setup>
import BaseLayout from '../components/common/BaseLayout.vue'
import { onMounted, ref, reactive, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { fetchMedicineList, createMedicine, fetchPlanList, createPlan, recognizeMedicine, uploadFile, updateMedicine, deleteMedicine, fetchMedicine } from '../utils/api'
import { useUserStore } from '../stores/user'
import { compressImage, fileToBase64 } from '../utils/image'

const userStore = useUserStore()

const medicineList = ref([])
const planList = ref([])
const loading = ref(false)
const error = ref('')

// === UI 抽屉状态控制 ===
const showCreatePlan = ref(false)
const showAddMedicine = ref(false)
const showEditMedicine = ref(false)  // 详情抽屉
const showEditMedicineForm = ref(false)  // 编辑抽屉
const submitting = ref(false)
const formError = ref('')
const editingMedicine = ref(null)

const ocrLoading = ref(false)
const ocrStep = ref(0) 
const previewUrl = ref('')
const coverUrl = ref('')

// === 页面架构优化状态 ===
const activeUserTab = ref('all') // 用药计划：成员切换
const searchMedQuery = ref('') // 药箱：搜索关键词
const activeMedFilter = ref('all') // 药箱：快捷筛选 (all, low, expiring)
const isPlansExpanded = ref(false) // 计划是否展开全部
const isMedsExpanded = ref(false) // 药箱是否展开全部

const currentMemberId = computed(() => userStore.member?.id)
const currentMemberName = computed(() => userStore.member?.nickname || userStore.member?.username || '我')

const load = async () => {
  error.value = ''
  loading.value = true
  try {
    const [medRes, planRes] = await Promise.all([
      fetchMedicineList({ page: 1, size: 100 }),
      fetchPlanList({ page: 1, size: 100 })
    ])
    medicineList.value = medRes.data.records || []
    planList.value = planRes.data.records || []
  } catch (err) {
    error.value = err?.message || '加载失败'
  } finally {
    loading.value = false
  }
}

// 提取计划中的家庭成员
const userTabs = computed(() => {
  const tabs = [{ label: '全家提醒', value: 'all' }]
  const userIds = [...new Set(planList.value.map(p => p.userId))]
  userIds.forEach(id => {
    tabs.push({ 
      label: id == currentMemberId.value ? currentMemberName.value : `成员(${id})`, 
      value: id 
    })
  })
  return tabs
})

// 动态过滤用药计划 (带折叠截断)
const displayPlans = computed(() => {
  let list = activeUserTab.value === 'all' 
    ? planList.value 
    : planList.value.filter(p => p.userId === activeUserTab.value)
  
  // 简易时间线排序
  list.sort((a, b) => {
    const getWeight = (slots) => slots?.includes('早') ? 1 : (slots?.includes('中') ? 2 : (slots?.includes('晚') ? 3 : 4))
    return getWeight(a.remindSlots) - getWeight(b.remindSlots)
  })
  
  return isPlansExpanded.value ? list : list.slice(0, 3)
})
const totalFilteredPlansCount = computed(() => {
  return activeUserTab.value === 'all' ? planList.value.length : planList.value.filter(p => p.userId === activeUserTab.value).length
})

// 动态搜索过滤药箱 (带折叠截断)
const displayMedicines = computed(() => {
  let list = medicineList.value

  if (searchMedQuery.value) {
    const q = searchMedQuery.value.toLowerCase()
    list = list.filter(m => m.name?.toLowerCase().includes(q) || m.indication?.toLowerCase().includes(q))
  }
  
  if (activeMedFilter.value === 'low') {
    list = list.filter(m => m.stock < 10)
  } else if (activeMedFilter.value === 'expiring') {
    const now = new Date().getTime()
    list = list.filter(m => {
      if (!m.expireDate) return false
      const exp = new Date(m.expireDate).getTime()
      return (exp - now) < 30 * 24 * 3600 * 1000 && (exp - now) > 0
    })
  }
  
  return isMedsExpanded.value ? list : list.slice(0, 6)
})
const totalFilteredMedsCount = computed(() => {
  let list = medicineList.value
  if (searchMedQuery.value) {
    const q = searchMedQuery.value.toLowerCase()
    list = list.filter(m => m.name?.toLowerCase().includes(q) || m.indication?.toLowerCase().includes(q))
  }
  if (activeMedFilter.value === 'low') list = list.filter(m => m.stock < 10)
  else if (activeMedFilter.value === 'expiring') {
    const now = new Date().getTime()
    list = list.filter(m => m.expireDate && (new Date(m.expireDate).getTime() - now) < 30 * 24 * 3600 * 1000 && (new Date(m.expireDate).getTime() - now) > 0)
  }
  return list.length
})

// 看板统计数据
const todayPlans = computed(() => {
  const today = new Date().getDay() || 7
  return planList.value.filter(p => p.status !== 0 && (p.takeDays?.split(',') || []).includes(String(today)))
})
const lowStockCount = computed(() => medicineList.value.filter(m => m.stock != null && m.stock < 10).length)
const expiringCount = computed(() => {
  const now = new Date().getTime()
  return medicineList.value.filter(m => {
    if (!m.expireDate) return false
    const exp = new Date(m.expireDate).getTime()
    return (exp - now) < 30 * 24 * 3600 * 1000 && (exp - now) > 0
  }).length
})

const medicineOptions = computed(() => {
  return medicineList.value.map(m => ({
    value: m.id,
    label: m.name + (m.specification ? ` (${m.specification})` : '')
  }))
})

const stockUnitOptions = ['片', '粒', 'ml', '支', '瓶', '盒', '袋']

const planForm = reactive({
  medicineId: '', dosage: '', remindSlots: [], takeDays: [],
  startDate: new Date().toISOString().split('T')[0], endDate: '', planRemark: ''
})

const medicineForm = reactive({
  name: '', specification: '', indication: '', stock: undefined, 
  stockUnit: '', expireDate: '', usageNotes: ''
})

const resetPlanForm = () => {
  Object.assign(planForm, {
    medicineId: '', dosage: '', remindSlots: [], takeDays: [],
    startDate: new Date().toISOString().split('T')[0], endDate: '', planRemark: ''
  })
  formError.value = ''
}

const resetMedicineForm = () => {
  Object.assign(medicineForm, {
    name: '', specification: '', indication: '', stock: undefined, stockUnit: '', expireDate: '', usageNotes: ''
  })
  previewUrl.value = ''
  coverUrl.value = ''
  ocrFiles.value = []
  ocrPreviews.value = []
  formError.value = ''
}

const handleCoverFile = async (e, isEdit = false) => {
  const file = e.target.files?.[0]
  if (!file) return
  try {
    const base64 = await fileToBase64(await compressImage(file))
    if (isEdit) coverUrl.value = base64
    else previewUrl.value = base64
  } catch (err) {
    console.error(err)
  }
}

const normalizeDate = (d) => {
  if (!d) return null
  const match = d.match(/^(\d{4})-(\d{1,2})-(\d{1,2})$/)
  if (match) return `${match[1]}-${match[2].padStart(2,'0')}-${match[3].padStart(2,'0')}`
  return d
}

const slotOptions = [ { value: '早', label: '早' }, { value: '中', label: '中' }, { value: '晚', label: '晚' }, { value: '睡前', label: '睡前' } ]
const dayOptions = [ { value: '1', label: '一' }, { value: '2', label: '二' }, { value: '3', label: '三' }, { value: '4', label: '四' }, { value: '5', label: '五' }, { value: '6', label: '六' }, { value: '7', label: '日' } ]

const ocrFiles = ref([])
const ocrPreviews = ref([])

const handleMedicineFile = async (e) => {
  const files = Array.from(e.target.files || [])
  if (files.length === 0) return
  const file = files[0]
  const compressed = await compressImage(file)
  const base64 = await fileToBase64(compressed)
  ocrFiles.value.push(compressed)
  ocrPreviews.value.push(base64)
  if (ocrPreviews.value.length === 1) coverUrl.value = base64
  e.target.value = ''
}

const removeOcrImage = (idx) => {
  ocrFiles.value.splice(idx, 1)
  ocrPreviews.value.splice(idx, 1)
  if (idx === 0 && ocrPreviews.value.length > 0) coverUrl.value = ocrPreviews.value[0]
  else if (ocrPreviews.value.length === 0) coverUrl.value = ''
}

const startOcr = async () => {
  if (ocrFiles.value.length === 0) return
  ocrLoading.value = true
  ocrStep.value = 0
  const stepTimer = setInterval(() => { if (ocrStep.value < 2) ocrStep.value++ }, 2000)
  try {
    const ocrRes = await recognizeMedicine(ocrFiles.value)
    const parsed = ocrRes.data?.parsed
    if (parsed) {
      if (parsed.name) medicineForm.name = parsed.name
      if (parsed.specification) medicineForm.specification = parsed.specification
      if (parsed.indication) medicineForm.indication = parsed.indication
      if (parsed.usageNotes) medicineForm.usageNotes = parsed.usageNotes
      ElMessage.success('识别成功，已自动填充')
    }
  } catch (err) {
    ElMessage.error('识别失败，请重试')
  } finally {
    clearInterval(stepTimer)
    ocrLoading.value = false
    ocrStep.value = 0
  }
}

const submitPlan = async () => {
  if (!planForm.medicineId || !planForm.dosage || planForm.remindSlots.length === 0 || planForm.takeDays.length === 0) {
    formError.value = '请填写完整的必要信息'; return;
  }
  formError.value = ''
  submitting.value = true
  try {
    await createPlan({
      medicineId: Number(planForm.medicineId), userId: Number(currentMemberId.value), dosage: planForm.dosage,
      remindSlots: planForm.remindSlots.join(','), takeDays: planForm.takeDays.join(','),
      startDate: planForm.startDate, endDate: planForm.endDate || null, planRemark: planForm.planRemark || null
    })
    showCreatePlan.value = false
    resetPlanForm()
    load()
    ElMessage.success('计划创建成功')
  } catch (err) {
    ElMessage.error(err?.message || '创建失败')
  } finally {
    submitting.value = false
  }
}

const submitMedicine = async () => {
  if (!medicineForm.name || !medicineForm.specification || medicineForm.stock === undefined || !medicineForm.stockUnit || !medicineForm.expireDate) {
    formError.value = '请填写完整的必要信息'; return;
  }
  formError.value = ''
  submitting.value = true
  try {
    let imageUrl = null
    if (ocrFiles.value.length > 0) {
      const uploadRes = await uploadFile(ocrFiles.value[0], 'medicine')
      imageUrl = uploadRes.data || uploadRes.url || uploadRes
    }
    await createMedicine({
      name: medicineForm.name.trim(), specification: medicineForm.specification.trim(),
      stock: Number(medicineForm.stock), stockUnit: medicineForm.stockUnit || null,
      expireDate: normalizeDate(medicineForm.expireDate), usageNotes: medicineForm.usageNotes || null,
      indication: medicineForm.indication || null, imageUrl
    })
    showAddMedicine.value = false
    resetMedicineForm()
    load()
    ElMessage.success('药品入库成功')
  } catch (err) {
    ElMessage.error(err?.message || '添加失败')
  } finally {
    submitting.value = false
  }
}

const isToday = (dayStr) => {
  const today = new Date().getDay() || 7
  return dayStr?.includes(String(today))
}

const onMedicineClick = async (medicine) => {
  editingMedicine.value = { ...medicine }
  showEditMedicine.value = true
  
  try {
    const res = await fetchMedicine(medicine.id)
    if(res.data) {
      editingMedicine.value = res.data
      medicineForm.name = res.data.name || ''
      medicineForm.specification = res.data.specification || ''
      medicineForm.indication = res.data.indication || ''
      medicineForm.stock = res.data.stock ?? undefined
      medicineForm.stockUnit = res.data.stockUnit || ''
      medicineForm.expireDate = res.data.expireDate || ''
      medicineForm.usageNotes = res.data.usageNotes || ''
    }
  } catch (err) {
    console.error('静默获取最新详情失败', err)
  }
}

// 业务闭环：从详情一键跳入创建计划
const handleCreatePlanFromDetail = () => {
  showEditMedicine.value = false;
  resetPlanForm();
  planForm.medicineId = editingMedicine.value.id; 
  showCreatePlan.value = true;
}

const openEditForm = () => {
  coverUrl.value = editingMedicine.value?.imageUrl || ''
  showEditMedicine.value = false
  showEditMedicineForm.value = true
}

const cancelEdit = () => {
  showEditMedicineForm.value = false
  resetMedicineForm()
  onMedicineClick(editingMedicine.value)
}

const submitEditMedicine = async () => {
  if (!medicineForm.name || !medicineForm.specification || medicineForm.stock === undefined || !medicineForm.stockUnit || !medicineForm.expireDate) {
    ElMessage.warning('请填写完整的必要信息'); return;
  }
  submitting.value = true
  try {
    let imageUrl = editingMedicine.value?.imageUrl || null
    if (coverUrl.value && coverUrl.value !== editingMedicine.value?.imageUrl) {
      const base64 = coverUrl.value.split(',')[1]
      const byteCharacters = atob(base64)
      const byteNumbers = new Array(byteCharacters.length)
      for (let i = 0; i < byteCharacters.length; i++) byteNumbers[i] = byteCharacters.charCodeAt(i)
      const blob = new Blob([new Uint8Array(byteNumbers)], { type: 'image/jpeg' })
      const uploadRes = await uploadFile(new File([blob], 'medicine.jpg', { type: 'image/jpeg' }), 'medicine')
      imageUrl = uploadRes.data || uploadRes.url || uploadRes
    }
    await updateMedicine(editingMedicine.value.id, {
      name: medicineForm.name.trim(), specification: medicineForm.specification.trim(),
      stock: Number(medicineForm.stock), stockUnit: medicineForm.stockUnit || null,
      expireDate: normalizeDate(medicineForm.expireDate), usageNotes: medicineForm.usageNotes || null,
      indication: medicineForm.indication || null, imageUrl
    })
    showEditMedicineForm.value = false
    resetMedicineForm()
    coverUrl.value = ''
    load()
    ElMessage.success('更新成功')
  } catch (err) {
    ElMessage.error(err?.message || '更新失败')
  } finally {
    submitting.value = false
  }
}

const deleteCurrentMedicine = async () => {
  try {
    await ElMessageBox.confirm(`确定要删除药品【${editingMedicine.value.name}】吗？此操作不可恢复。`, '删除警告', {
      confirmButtonText: '确认删除', cancelButtonText: '取消', type: 'warning', confirmButtonClass: 'el-button--danger'
    })
    // await deleteMedicine(editingMedicine.value.id)
    ElMessage.success('删除成功')
    showEditMedicine.value = false
    load()
  } catch (e) {}
}

onMounted(() => load())
</script>

<template>
  <BaseLayout>
    <div class="medicine-page-container">
      
      <!-- 模块 1：顶部数据看板 (代替废话欢迎语，提供3个核心指标) -->
      <div class="stats-dashboard">
        <div class="stat-card blue">
          <div class="stat-icon">💊</div>
          <div class="stat-info">
            <span class="stat-num">{{ todayPlans.length }}</span>
            <span class="stat-desc">今日待服(次)</span>
          </div>
        </div>
        <div class="stat-card orange">
          <div class="stat-icon">⚠️</div>
          <div class="stat-info">
            <span class="stat-num">{{ lowStockCount }}</span>
            <span class="stat-desc">库存紧张(款)</span>
          </div>
        </div>
        <div class="stat-card red">
          <div class="stat-icon">⏳</div>
          <div class="stat-info">
            <span class="stat-num">{{ expiringCount }}</span>
            <span class="stat-desc">30天内过期</span>
          </div>
        </div>
      </div>

      <!-- 模块 2：用药计划 (车票式排版 + 折叠功能) -->
      <section class="content-module">
        <div class="module-header">
          <h2 class="module-title">提醒计划</h2>
          <button class="btn-create" @click="showCreatePlan = true; resetPlanForm()">+ 新建计划</button>
        </div>
        
        <div class="custom-tabs">
          <button v-for="tab in userTabs" :key="tab.value" class="tab-item" :class="{ active: activeUserTab === tab.value }" @click="activeUserTab = tab.value">
            {{ tab.label }}
          </button>
        </div>

        <div v-if="displayPlans.length > 0" class="plan-list mt-3">
          <div v-for="plan in displayPlans" :key="plan.id" class="plan-ticket" :class="{ 'is-disabled': plan.status === 0 }">
            <!-- 车票左侧：时间与周期 -->
            <div class="ticket-left">
              <span class="ticket-time">{{ plan.remindSlots }}</span>
              <span class="ticket-day" :class="{ 'is-today': isToday(plan.takeDays) }">
                {{ ['一','二','三','四','五','六','日'].map((d, i) => plan.takeDays?.includes(String(i+1)) ? d : '').filter(d=>d).join(',') || '规律' }}
              </span>
            </div>
            <!-- 车票右侧：药品与剂量 -->
            <div class="ticket-right">
              <div class="ticket-head">
                <span v-if="activeUserTab === 'all'" class="ticket-badge">{{ plan.userId == currentMemberId ? '我' : '家人' }}</span>
                <h3 class="ticket-med-name">{{ medicineOptions.find(m => m.value == plan.medicineId)?.label || '未知药品' }}</h3>
              </div>
              <div class="ticket-dosage">每次 {{ plan.dosage }}</div>
            </div>
          </div>
          
          <!-- 折叠展开按钮 -->
          <button v-if="totalFilteredPlansCount > 3" class="btn-fold" @click="isPlansExpanded = !isPlansExpanded">
            {{ isPlansExpanded ? '收起计划 ↑' : `展开其余 ${totalFilteredPlansCount - 3} 条计划 ↓` }}
          </button>
        </div>
        <div v-else class="empty-box">
          <span class="empty-icon">📝</span>
          <p>当前没有用药提醒计划</p>
        </div>
      </section>

      <!-- 模块 3：家庭药箱 (Grid布局 + 搜索/筛选 + 折叠) -->
      <section class="content-module">
        <div class="module-header flex-wrap">
          <h2 class="module-title">我的药箱 <span class="title-count">({{ medicineList.length }})</span></h2>
          <div class="header-tools">
            <div class="search-wrapper">
              <span class="search-icon">🔍</span>
              <input type="text" v-model="searchMedQuery" placeholder="搜索药名..." class="search-input" />
            </div>
            <button class="btn-solid" @click="showAddMedicine = true; resetMedicineForm()">+ 录入</button>
          </div>
        </div>
        
        <div class="custom-tabs sm-tabs mb-4">
          <button class="tab-item" :class="{ active: activeMedFilter === 'all' }" @click="activeMedFilter = 'all'">全部</button>
          <button class="tab-item text-orange" :class="{ active: activeMedFilter === 'low' }" @click="activeMedFilter = 'low'">库存紧张</button>
          <button class="tab-item text-red" :class="{ active: activeMedFilter === 'expiring' }" @click="activeMedFilter = 'expiring'">即将过期</button>
        </div>
        
        <div v-if="displayMedicines.length > 0" class="med-grid">
          <div v-for="m in displayMedicines" :key="m.id" class="med-card" @click="onMedicineClick(m)">
            <div class="med-cover">
              <img v-if="m.imageUrl" :src="m.imageUrl" />
              <span v-else class="med-fallback-icon">💊</span>
            </div>
            <div class="med-info">
              <h3 class="med-name">{{ m.name }}</h3>
              <p class="med-spec">{{ m.specification || '暂无规格' }}</p>
              <div class="med-status">
                <span class="stock-tag" :class="{ 'is-low': m.stock < 10 }">余量: {{ m.stock ?? 0 }}{{ m.stockUnit }}</span>
                <span class="expire-text" v-if="m.expireDate">效期: {{ m.expireDate }}</span>
              </div>
            </div>
          </div>
          
          <!-- 折叠展开按钮 -->
          <div class="fold-wrapper" v-if="totalFilteredMedsCount > 6">
             <button class="btn-fold" @click="isMedsExpanded = !isMedsExpanded">
              {{ isMedsExpanded ? '收起药箱 ↑' : `展开其余 ${totalFilteredMedsCount - 6} 款药品 ↓` }}
            </button>
          </div>
        </div>
        <div v-else class="empty-box">
          <span class="empty-icon">📦</span>
          <p>未找到符合条件的药品</p>
        </div>
      </section>
      
    </div>

    <!-- ================= 所有抽屉组件 (保持不变，仅更新内部类名确保不受影响) ================= -->

    <el-drawer v-model="showCreatePlan" title="创建用药计划" direction="rtl" size="400px" :destroy-on-close="true" @closed="resetPlanForm" class="custom-drawer">
      <div class="form-wrapper">
        <div class="form-group"><label>关联药品 <span class="req">*</span></label><el-select v-model="planForm.medicineId" placeholder="搜索药品" filterable style="width:100%"><el-option v-for="m in medicineOptions" :key="m.value" :label="m.label" :value="m.value" /></el-select></div>
        <div class="form-group"><label>执行成员</label><el-input :model-value="currentMemberName" disabled /></div>
        <div class="form-group"><label>单次服用剂量 <span class="req">*</span></label><el-input v-model="planForm.dosage" placeholder="如：1片、半粒" /></div>
        <div class="form-group"><label>服药时段 <span class="req">*</span></label><el-checkbox-group v-model="planForm.remindSlots"><el-checkbox-button v-for="s in slotOptions" :key="s.value" :value="s.value">{{ s.label }}</el-checkbox-button></el-checkbox-group></div>
        <div class="form-group"><label>服药星期 <span class="req">*</span></label><el-checkbox-group v-model="planForm.takeDays"><el-checkbox-button v-for="d in dayOptions" :key="d.value" :value="d.value">{{ d.label }}</el-checkbox-button></el-checkbox-group></div>
        <div class="form-group"><label>起止日期</label><div style="display:flex;gap:8px;"><el-date-picker v-model="planForm.startDate" type="date" value-format="YYYY-MM-DD" style="flex:1" /><el-date-picker v-model="planForm.endDate" type="date" placeholder="结束(可选)" value-format="YYYY-MM-DD" style="flex:1" /></div></div>
        <div class="form-group"><label>备注(可选)</label><el-input v-model="planForm.planRemark" placeholder="如：饭后服用" /></div>
        <p v-if="formError" class="form-error">{{ formError }}</p>
      </div>
      <template #footer>
        <div style="display:flex;gap:12px;"><el-button style="flex:1" @click="showCreatePlan = false">取消</el-button><el-button type="primary" style="flex:1" :loading="submitting" @click="submitPlan">确认创建</el-button></div>
      </template>
    </el-drawer>

    <el-drawer v-model="showAddMedicine" title="新药入库" direction="rtl" size="400px" :destroy-on-close="true" @closed="resetMedicineForm" class="custom-drawer">
      <div class="form-wrapper">
        <div class="ocr-panel">
          <div class="ocr-images">
            <div v-for="(url, idx) in ocrPreviews" :key="idx" class="img-preview"><img :src="url" /><span class="img-close" @click.stop="removeOcrImage(idx)">×</span></div>
            <div v-if="ocrPreviews.length < 4" class="img-add-btn" @click="$refs.fileInput.click()"><input ref="fileInput" type="file" accept="image/*" hidden @change="handleMedicineFile" /><span class="icon">📷</span><span class="txt">拍药盒</span></div>
          </div>
          <button v-if="ocrPreviews.length > 0 && !ocrLoading" class="ai-btn" @click.stop="startOcr">✨ AI 智能解析</button>
          <div v-if="ocrLoading" class="ocr-loading-overlay"><div class="loading-icon">🧠</div><div class="loading-txt">解析中...</div></div>
        </div>
        <div class="form-group"><label>药品名称 <span class="req">*</span></label><el-input v-model="medicineForm.name" /></div>
        <div class="form-group"><label>规格 <span class="req">*</span></label><el-input v-model="medicineForm.specification" /></div>
        <div class="form-group"><label>适应症</label><el-input v-model="medicineForm.indication" /></div>
        <div class="form-group"><label>当前库存 <span class="req">*</span></label><div style="display:flex;gap:8px;"><el-input-number v-model="medicineForm.stock" :min="0" style="flex:2" /><el-select v-model="medicineForm.stockUnit" style="flex:1"><el-option v-for="u in stockUnitOptions" :key="u" :label="u" :value="u" /></el-select></div></div>
        <div class="form-group"><label>有效期至 <span class="req">*</span></label><el-date-picker v-model="medicineForm.expireDate" type="date" value-format="YYYY-MM-DD" style="width:100%" /></div>
        <div class="form-group"><label>用法说明建议</label><el-input type="textarea" v-model="medicineForm.usageNotes" :rows="2" /></div>
        <p v-if="formError" class="form-error">{{ formError }}</p>
      </div>
      <template #footer>
        <div style="display:flex;gap:12px;"><el-button style="flex:1" @click="showAddMedicine = false">取消</el-button><el-button type="primary" style="flex:1" :loading="submitting" @click="submitMedicine">确认入库</el-button></div>
      </template>
    </el-drawer>

    <!-- 药品详情抽屉 -->
    <el-drawer v-model="showEditMedicine" title="药品详情" direction="rtl" size="400px" class="custom-drawer" destroy-on-close>
      <div v-if="editingMedicine" class="form-wrapper">
        <div style="display:flex; gap:16px; margin-bottom:16px; align-items:center;">
          <div style="width:80px;height:80px;border-radius:12px;background:#f8fafc;border:1px solid #eee;display:flex;align-items:center;justify-content:center;overflow:hidden;">
            <img v-if="editingMedicine.imageUrl" :src="editingMedicine.imageUrl" style="width:100%;height:100%;object-fit:cover;"/><span v-else style="font-size:2.5rem;">💊</span>
          </div>
          <div>
            <h3 style="font-size:1.2rem;font-weight:bold;margin:0 0 4px 0;color:#333;">{{ editingMedicine.name }}</h3>
            <p style="color:#666;font-size:0.9rem;margin:0;">{{ editingMedicine.specification }}</p>
          </div>
        </div>
        <div style="background:#f8fafc;border-radius:12px;padding:16px;">
          <div style="margin-bottom:12px;"><span style="display:block;font-size:0.8rem;color:#888;margin-bottom:4px;">适应症</span><span style="color:#333;">{{ editingMedicine.indication || '暂无说明' }}</span></div>
          <div style="margin-bottom:16px;"><span style="display:block;font-size:0.8rem;color:#888;margin-bottom:4px;">建议用法</span><span style="color:#333;">{{ editingMedicine.usageNotes || '暂无说明' }}</span></div>
          <div style="display:flex; gap:16px; border-top:1px dashed #ddd; padding-top:16px;">
            <div style="flex:1;"><span style="display:block;font-size:0.8rem;color:#888;margin-bottom:4px;">剩余库存</span><span style="color:var(--primary);font-size:1.2rem;font-weight:bold;">{{ editingMedicine.stock ?? 0 }} <small style="font-size:0.9rem">{{ editingMedicine.stockUnit }}</small></span></div>
            <div style="flex:1;"><span style="display:block;font-size:0.8rem;color:#888;margin-bottom:4px;">有效期至</span><span style="color:#333;font-weight:bold;">{{ editingMedicine.expireDate || '-' }}</span></div>
          </div>
        </div>
      </div>
      <template #footer>
        <div style="display:flex;justify-content:space-between;align-items:center;">
          <el-button type="danger" plain @click="deleteCurrentMedicine">删除</el-button>
          <div style="display:flex;gap:8px;">
            <el-button plain @click="openEditForm">编辑</el-button>
            <el-button type="primary" @click="handleCreatePlanFromDetail">➕ 制定计划</el-button>
          </div>
        </div>
      </template>
    </el-drawer>

    <!-- 编辑药品抽屉 -->
    <el-drawer v-model="showEditMedicineForm" title="更新药品信息" direction="rtl" size="400px" :destroy-on-close="true" @closed="cancelEdit" class="custom-drawer">
      <div class="form-wrapper">
        <div style="width:100px;height:100px;border-radius:16px;margin:0 auto 16px;background:#f8fafc;border:2px dashed #cbd5e1;display:flex;align-items:center;justify-content:center;cursor:pointer;overflow:hidden;" @click="$refs.editFileInput.click()">
          <img v-if="coverUrl || editingMedicine?.imageUrl" :src="coverUrl || editingMedicine?.imageUrl" style="width:100%;height:100%;object-fit:cover;" />
          <span v-else style="font-size:0.8rem;color:#888;">📷 上传照片</span>
          <input ref="editFileInput" type="file" accept="image/*" hidden @change="(e) => handleCoverFile(e, true)" />
        </div>
        <div class="form-group"><label>名称</label><el-input v-model="medicineForm.name" /></div>
        <div class="form-group"><label>库存</label><div style="display:flex;gap:8px;"><el-input-number v-model="medicineForm.stock" :min="0" style="flex:2" /><el-select v-model="medicineForm.stockUnit" style="flex:1"><el-option v-for="u in stockUnitOptions" :key="u" :label="u" :value="u" /></el-select></div></div>
        <div class="form-group"><label>效期</label><el-date-picker v-model="medicineForm.expireDate" type="date" value-format="YYYY-MM-DD" style="width:100%" /></div>
      </div>
      <template #footer>
        <div style="display:flex;gap:12px;"><el-button style="flex:1" @click="cancelEdit">返回</el-button><el-button type="primary" style="flex:1" :loading="submitting" @click="submitEditMedicine">保存修改</el-button></div>
      </template>
    </el-drawer>

  </BaseLayout>
</template>

<style scoped>
/* ========================================= */
/* 1. 核心变量定义 (防污染) */
/* ========================================= */
:root {
  --primary: #2d5f5d;
  --bg-main: #f4f6f8;
  --text-dark: #1e293b;
  --text-gray: #64748b;
  --card-bg: #ffffff;
  --border-line: #e2e8f0;
  --warning: #f59e0b;
  --danger: #ef4444;
  --info: #0ea5e9;
}

/* Element Plus 主题覆写 */
:deep(.el-button--primary) { background-color: var(--primary); border-color: var(--primary); }
:deep(.el-button--primary:hover) { background-color: #3b7b78; border-color: #3b7b78; }
:deep(.el-checkbox-button.is-checked .el-checkbox-button__inner) { background-color: var(--primary); border-color: var(--primary); box-shadow: none; }
:deep(.el-input__wrapper.is-focus), :deep(.el-textarea__inner:focus) { box-shadow: 0 0 0 1px var(--primary) inset !important; }

/* ========================================= */
/* 2. 页面基础架构与排版 */
/* ========================================= */
.medicine-page-container {
  background-color: var(--bg-main);
  min-height: 100vh;
  padding: 24px;
  display: flex;
  flex-direction: column;
  gap: 24px;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
}

/* === 顶部数据看板 === */
.stats-dashboard {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 16px;
}
.stat-card {
  background: var(--card-bg);
  border-radius: 16px;
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 16px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.03);
  border: 1px solid #fff;
}
.stat-card.blue { background: linear-gradient(135deg, #f0f9ff 0%, #ffffff 100%); border-color: #e0f2fe; }
.stat-card.orange { background: linear-gradient(135deg, #fff7ed 0%, #ffffff 100%); border-color: #ffedd5; }
.stat-card.red { background: linear-gradient(135deg, #fef2f2 0%, #ffffff 100%); border-color: #fee2e2; }

.stat-icon {
  font-size: 2rem;
  background: rgba(255,255,255,0.7);
  width: 50px; height: 50px;
  border-radius: 14px;
  display: flex; align-items: center; justify-content: center;
}
.stat-info { display: flex; flex-direction: column; }
.stat-num { font-size: 1.8rem; font-weight: 800; color: var(--text-dark); line-height: 1; margin-bottom: 4px; }
.stat-card.blue .stat-num { color: #0284c7; }
.stat-card.orange .stat-num { color: #ea580c; }
.stat-card.red .stat-num { color: #dc2626; }
.stat-desc { font-size: 0.85rem; color: var(--text-gray); font-weight: 500; }

/* === 通用模块结构 === */
.content-module {
  background: var(--card-bg);
  border-radius: 20px;
  padding: 24px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.02);
  border: 1px solid var(--border-line);
}
.module-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.module-title { font-size: 1.2rem; font-weight: 700; color: var(--text-dark); margin: 0; display: flex; align-items: center; gap: 8px;}
.title-count { font-size: 0.9rem; color: var(--text-gray); font-weight: normal; }
.btn-create { background: none; border: none; color: var(--primary); font-weight: 600; font-size: 0.95rem; cursor: pointer; }
.btn-solid { background: var(--primary); color: #fff; border: none; padding: 6px 16px; border-radius: 20px; font-size: 0.9rem; font-weight: 600; cursor: pointer;}
.mt-3 { margin-top: 12px; }
.mb-4 { margin-bottom: 16px; }

/* === 自定义胶囊 Tab === */
.custom-tabs {
  display: flex; gap: 10px; overflow-x: auto; padding-bottom: 4px;
}
.custom-tabs::-webkit-scrollbar { display: none; }
.tab-item {
  background: #f1f5f9; color: #475569; border: none;
  padding: 8px 16px; border-radius: 20px; font-size: 0.9rem; font-weight: 600;
  cursor: pointer; transition: 0.2s; white-space: nowrap;
}
.tab-item.active { background: var(--primary); color: #fff; }
.sm-tabs .tab-item { padding: 6px 14px; font-size: 0.85rem; }
.sm-tabs .tab-item.text-orange.active { background: var(--warning); }
.sm-tabs .tab-item.text-red.active { background: var(--danger); }

/* === 折叠按钮 === */
.btn-fold {
  width: 100%; padding: 12px; margin-top: 12px;
  background: #f8fafc; border: 1px dashed #cbd5e1; border-radius: 12px;
  color: var(--text-gray); font-size: 0.85rem; font-weight: 600;
  cursor: pointer; transition: 0.2s;
}
.btn-fold:hover { background: #f1f5f9; color: var(--primary); border-color: var(--primary); }
.fold-wrapper { grid-column: 1 / -1; }

/* === 空状态 === */
.empty-box {
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  padding: 40px 0; color: #94a3b8;
}
.empty-icon { font-size: 3rem; margin-bottom: 12px; opacity: 0.5; }
.empty-box p { margin: 0; font-size: 0.9rem; }


/* ========================================= */
/* 3. 计划卡片：车票式布局 */
/* ========================================= */
.plan-list { display: flex; flex-direction: column; gap: 12px; }
.plan-ticket {
  display: flex;
  background: #ffffff;
  border: 1px solid var(--border-line);
  border-radius: 14px;
  overflow: hidden;
  transition: box-shadow 0.2s;
}
.plan-ticket:hover { box-shadow: 0 4px 12px rgba(0,0,0,0.04); border-color: #cbd5e1; }
.plan-ticket.is-disabled { opacity: 0.5; background: #f8fafc; }

.ticket-left {
  background: #f8fafc;
  padding: 16px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  border-right: 2px dashed var(--border-line);
  min-width: 90px;
}
.ticket-time { font-size: 1rem; font-weight: 800; color: var(--primary); margin-bottom: 4px; }
.ticket-day { font-size: 0.75rem; color: var(--text-gray); text-align: center;}
.ticket-day.is-today { color: var(--warning); font-weight: 700; }

.ticket-right {
  padding: 16px;
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
}
.ticket-head { display: flex; align-items: center; gap: 8px; margin-bottom: 6px; }
.ticket-badge { background: #e2e8f0; color: #334155; font-size: 0.7rem; padding: 2px 6px; border-radius: 4px; font-weight: 700; }
.ticket-med-name { font-size: 1.05rem; font-weight: 700; color: var(--text-dark); margin: 0; }
.ticket-dosage { font-size: 0.9rem; color: #475569; font-weight: 500; }


/* ========================================= */
/* 4. 药箱卡片：Grid 图片信息流 */
/* ========================================= */
.header-tools { display: flex; align-items: center; gap: 12px; }
.search-wrapper { display: flex; align-items: center; background: #f1f5f9; border-radius: 20px; padding: 6px 16px; border: 1px solid transparent; width: 200px; transition: 0.2s;}
.search-wrapper:focus-within { border-color: var(--primary); background: #fff; }
.search-icon { font-size: 0.9rem; margin-right: 8px; color: #94a3b8; }
.search-input { flex: 1; border: none; background: transparent; font-size: 0.9rem; outline: none; padding: 4px 0; color: var(--text-dark); width: 100%;}

.med-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(300px, 1fr)); gap: 16px; }
.med-card {
  display: flex; gap: 16px; padding: 16px; background: #fff;
  border: 1px solid var(--border-line); border-radius: 16px; cursor: pointer; transition: 0.2s;
}
.med-card:hover { border-color: var(--primary); box-shadow: 0 4px 12px rgba(45,95,93,0.06); }

.med-cover {
  width: 70px; height: 70px; background: #f8fafc; border-radius: 12px;
  display: flex; align-items: center; justify-content: center; overflow: hidden;
  border: 1px solid #f1f5f9; flex-shrink: 0;
}
.med-cover img { width: 100%; height: 100%; object-fit: cover; }
.med-fallback-icon { font-size: 2rem; color: #cbd5e1; }

.med-info { flex: 1; display: flex; flex-direction: column; justify-content: center; min-width: 0; }
.med-name { font-size: 1rem; font-weight: 700; color: var(--text-dark); margin: 0 0 4px 0; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.med-spec { font-size: 0.8rem; color: var(--text-gray); margin: 0 0 10px 0; }
.med-status { display: flex; justify-content: space-between; align-items: center; }
.stock-tag { font-size: 0.8rem; color: #475569; font-weight: 700; padding: 2px 8px; background: #f1f5f9; border-radius: 6px; }
.stock-tag.is-low { background: #fff7ed; color: var(--warning); }
.expire-text { font-size: 0.75rem; color: #94a3b8; }


/* ========================================= */
/* 5. 抽屉表单与 OCR 内部样式 */
/* ========================================= */
.form-wrapper { display: flex; flex-direction: column; gap: 16px; padding-top: 8px;}
.form-group { display: flex; flex-direction: column; gap: 6px; }
.form-group label { font-size: 0.9rem; font-weight: 600; color: var(--text-dark); }
.req { color: var(--danger); margin-left: 2px; }
.form-error { color: var(--danger); font-size: 0.85rem; background: #fef2f2; padding: 10px; border-radius: 8px; margin-top: -8px; }

/* OCR 图片区 */
.ocr-panel { border: 2px dashed #cbd5e1; border-radius: 16px; padding: 16px; background: #f8fafc; position: relative; }
.ocr-images { display: flex; gap: 10px; flex-wrap: wrap; margin-bottom: 12px; }
.img-preview { position: relative; width: 70px; height: 70px; border-radius: 10px; overflow: hidden; border: 1px solid #e2e8f0; }
.img-preview img { width: 100%; height: 100%; object-fit: cover; }
.img-close { position: absolute; top: 4px; right: 4px; width: 20px; height: 20px; background: rgba(0,0,0,0.6); color: #fff; border-radius: 50%; display: flex; align-items: center; justify-content: center; cursor: pointer; font-size: 12px; }
.img-add-btn { width: 70px; height: 70px; background: #fff; border: 1px solid #e2e8f0; border-radius: 10px; display: flex; flex-direction: column; align-items: center; justify-content: center; cursor: pointer; color: var(--primary);}
.ai-btn { width: 100%; background: linear-gradient(135deg, var(--primary) 0%, #1e403f 100%); color: white; border: none; padding: 10px; border-radius: 10px; font-weight: 600; cursor: pointer; box-shadow: 0 4px 12px rgba(45,95,93,0.2); }
.ocr-loading-overlay { position: absolute; inset: 0; background: rgba(255,255,255,0.9); display: flex; flex-direction: column; align-items: center; justify-content: center; border-radius: 16px; backdrop-filter: blur(2px);}
.loading-icon { font-size: 2rem; margin-bottom: 8px; animation: pulse 1s infinite;}
.loading-txt { font-weight: 700; color: var(--primary); }
@keyframes pulse { 0%, 100% { opacity: 1; transform: scale(1); } 50% { opacity: 0.7; transform: scale(1.1); } }


/* ========================================= */
/* 6. 移动端终极响应式适配 */
/* ========================================= */
@media (max-width: 768px) {
  .medicine-page-container { padding: 12px; gap: 16px; }
  .stats-dashboard { grid-template-columns: 1fr; gap: 12px; }
  .stat-card { padding: 16px; }
  
  .content-module { padding: 16px; border-radius: 16px; }
  .module-header.flex-wrap { flex-direction: column; align-items: flex-start; gap: 12px; }
  .header-tools { width: 100%; justify-content: space-between; }
  .search-wrapper { flex: 1; }
  
  .med-grid { grid-template-columns: 1fr; gap: 12px; }
  
  /* Drawer 侧滑抽屉变为全屏遮罩感 */
  :deep(.custom-drawer) { width: 92% !important; border-top-left-radius: 24px; border-bottom-left-radius: 24px; }
}
</style>