<script setup>
import BaseLayout from '../../components/common/BaseLayout.vue'
import { onMounted, onUnmounted, ref, reactive, computed, nextTick, watch } from 'vue'
import { ElMessage, ElMessageBox, ElCheckbox } from 'element-plus'
import { fetchMedicineList, createMedicine, fetchPlanList, createPlan, deletePlan, recognizeMedicine, uploadFile, updateMedicine, deleteMedicine, fetchMedicine, fetchFamilyMembers, fetchTodayPlanRecords } from '../../utils/api'
import { useUserStore } from '../../stores/user'
import { useRoute, useRouter } from 'vue-router'
import { compressImage, fileToBase64 } from '../../utils/image'

const userStore = useUserStore()
const route = useRoute()
const router = useRouter()

const medicineList = ref([])
const planList = ref([])
const familyMembers = ref([]) // 家庭成员列表（管理员用）
const loading = ref(false)
const error = ref('')
const todayPendingCount = ref(0)

// 管理员切换查看的成员
const selectedMemberId = ref(null)
// 管理员创建计划时选择的成员（独立变量）
const createPlanMemberId = ref(null)
const selectedMember = computed(() => {
  if (!userStore.isAdmin) return userStore.member
  if (selectedMemberId.value) {
    return familyMembers.value.find(m => m.userId === selectedMemberId.value) || userStore.member
  }
  return userStore.member
})

// 当前查看的成员名称
const currentViewerName = computed(() => {
  if (!userStore.isAdmin) return ''
  if (selectedMemberId.value === userStore.member?.id) return ''
  const member = familyMembers.value.find(m => m.userId === selectedMemberId.value)
  return member?.nickname || ''
})

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
const searchMedQuery = ref('') // 药箱：搜索关键词
const activeMedFilter = ref('all') // 药箱：快捷筛选 (all, low, expiring)
const isPlansExpanded = ref(false) // 计划是否展开全部
const isMedsExpanded = ref(false) // 药箱是否展开全部

// 检测移动端（使用 ref 便于响应式更新）
const isMobile = ref(window.innerWidth < 768)

// 抽屉方向：移动端从下往上，桌面端从右往左
const drawerDirection = computed(() => isMobile.value ? 'btt' : 'rtl')
const drawerSize = computed(() => isMobile.value ? '85%' : '400px')

// 窗口 resize 事件处理
const handleResize = () => {
  isMobile.value = window.innerWidth < 768
}

const currentMemberId = computed(() => userStore.member?.id)
const currentMemberName = computed(() => userStore.member?.nickname || userStore.member?.username || '我')

const getViewingUserId = () => {
  if (userStore.isAdmin) {
    return selectedMemberId.value || userStore.member?.id || null
  }
  return currentMemberId.value || null
}

const toLocalDateISO = () => {
  const d = new Date()
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

const loadTodayPendingCount = async () => {
  const uid = getViewingUserId()
  if (!uid) {
    todayPendingCount.value = 0
    return
  }
  try {
    const res = await fetchTodayPlanRecords(toLocalDateISO(), uid)
    const records = res?.data?.records || []
    todayPendingCount.value = records.filter(item => item.recordStatus === 0).length
  } catch (_) {
    todayPendingCount.value = 0
  }
}

// 加载数据
const load = async () => {
  error.value = ''
  loading.value = true
  try {
    // 获取药品和计划
    const [medRes, planRes] = await Promise.all([
      fetchMedicineList({ page: 1, size: 100 }),
      fetchPlanList({ page: 1, size: 100 })
    ])
    medicineList.value = medRes.data.records || []
    planList.value = planRes.data.records || []
    
    // 管理员：获取家庭成员列表（排除自己）
    if (userStore.isAdmin) {
      const membersRes = await fetchFamilyMembers()
      familyMembers.value = (membersRes.data || []).filter(m => m.userId !== userStore.member?.id)
      if (!selectedMemberId.value && userStore.member?.id) {
        selectedMemberId.value = userStore.member.id
        createPlanMemberId.value = userStore.member.id
      }
    }

    await loadTodayPendingCount()
  } catch (err) {
    error.value = err?.message || '加载失败'
  } finally {
    loading.value = false
  }
}

// 滚动到指定区块
const scrollToSection = (section, filter = null) => {
  if (section === 'plans') {
    const el = document.getElementById('section-plans')
    if (el) {
      el.scrollIntoView({ behavior: 'smooth', block: 'start' })
    }
  } else if (section === 'meds') {
    activeMedFilter.value = filter || 'all'
    const el = document.getElementById('section-meds')
    if (el) {
      el.scrollIntoView({ behavior: 'smooth', block: 'start' })
    }
  }
}

const clearQuickActionQuery = async () => {
  if (!route.query?.action) return
  const nextQuery = { ...route.query }
  delete nextQuery.action
  delete nextQuery.filter
  delete nextQuery.type
  await router.replace({ name: route.name, query: nextQuery })
}

const handleQuickAction = async () => {
  const action = route.query?.action
  if (!action) return

  if (action === 'create-plan') {
    if (canCreatePlan.value) {
      resetPlanForm()
      showCreatePlan.value = true
    }
    await clearQuickActionQuery()
    return
  }

  if (action === 'add-medicine') {
    resetMedicineForm()
    showAddMedicine.value = true
    await clearQuickActionQuery()
    return
  }

  if (action === 'view-low-stock') {
    activeMedFilter.value = 'low'
    await nextTick()
    scrollToSection('meds', 'low')
    await clearQuickActionQuery()
    return
  }

  if (action === 'view-expiring') {
    activeMedFilter.value = 'expiring'
    await nextTick()
    scrollToSection('meds', 'expiring')
    await clearQuickActionQuery()
    return
  }

  if (action === 'view-plans') {
    await nextTick()
    scrollToSection('plans')
    await clearQuickActionQuery()
  }
}

// 切换成员（管理员）
const handleMemberChange = (userId) => {
  selectedMemberId.value = userId
}

// 根据角色显示用药计划
const canCreatePlan = computed(() => userStore.isAdmin || userStore.member?.role === 1)

// 当前选中的成员 ID（用于创建计划时绑定用户）
const planUserId = computed(() => {
  if (userStore.isAdmin) {
    // 创建计划时使用独立的选择器
    return createPlanMemberId.value || selectedMemberId.value || userStore.member?.id
  }
  return currentMemberId.value
})

// 当前选中成员的名称（用于创建计划时显示）
const planUserName = computed(() => {
  if (userStore.isAdmin && selectedMemberId.value) {
    const member = familyMembers.value.find(m => m.userId === selectedMemberId.value)
    if (member) return member.nickname
    if (selectedMemberId.value === userStore.member?.id) return userStore.member?.nickname + '（我）'
  }
  return userStore.member?.nickname || userStore.member?.username || '我'
})

// 动态过滤用药计划 (带折叠截断)
const displayPlans = computed(() => {
  let list = planList.value
  
  // 管理员按选择的成员过滤，非管理员只看自己的
  if (userStore.isAdmin) {
    if (selectedMemberId.value) {
      list = list.filter(p => p.userId === selectedMemberId.value)
    }
  } else {
    list = list.filter(p => p.userId === currentMemberId.value)
  }
  
  // 简易时间线排序
  list.sort((a, b) => {
    const getWeight = (slots) => slots?.includes('早') ? 1 : (slots?.includes('中') ? 2 : (slots?.includes('晚') ? 3 : 4))
    return getWeight(a.remindSlots) - getWeight(b.remindSlots)
  })
  
  return isPlansExpanded.value ? list : list.slice(0, 3)
})
const totalFilteredPlansCount = computed(() => {
  let list = planList.value
  if (userStore.isAdmin && selectedMemberId.value) {
    list = list.filter(p => p.userId === selectedMemberId.value)
  } else if (!userStore.isAdmin) {
    list = list.filter(p => p.userId === currentMemberId.value)
  }
  return list.length
})

// 动态搜索过滤药箱 (带折叠截断)
const displayMedicines = computed(() => {
  let list = medicineList.value

  if (searchMedQuery.value) {
    const q = searchMedQuery.value.toLowerCase()
    list = list.filter(m => m.name?.toLowerCase().includes(q) || m.indication?.toLowerCase().includes(q))
  }
  
  if (activeMedFilter.value === 'low') {
    list = list.filter(m => m.stock < 5)
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
  if (activeMedFilter.value === 'low') list = list.filter(m => m.stock < 5)
  else if (activeMedFilter.value === 'expiring') {
    const now = new Date().getTime()
    list = list.filter(m => m.expireDate && (new Date(m.expireDate).getTime() - now) < 30 * 24 * 3600 * 1000 && (new Date(m.expireDate).getTime() - now) > 0)
  }
  return list.length
})

// 看板统计数据
const lowStockCount = computed(() => medicineList.value.filter(m => m.stock != null && m.stock < 5).length)
const expiringCount = computed(() => {
  const now = new Date().getTime()
  return medicineList.value.filter(m => {
    if (!m.expireDate) return false
    const exp = new Date(m.expireDate).getTime()
    return (exp - now) < 30 * 24 * 3600 * 1000 && (exp - now) > 0
  }).length
})

// 判断单个药品是否临期（30天内）
const isExpiringSoon = (expireDate) => {
  if (!expireDate) return false
  const now = new Date().getTime()
  const exp = new Date(expireDate).getTime()
  return (exp - now) < 30 * 24 * 3600 * 1000 && (exp - now) > 0
}

// 判断单个药品是否已过期
const isExpired = (expireDate) => {
  if (!expireDate) return false
  const now = new Date().getTime()
  const exp = new Date(expireDate).getTime()
  return exp - now < 0
}

// 计算药品状态优先级（用于左边框颜色）
const getMedicineStatus = (m) => {
  if (isExpired(m.expireDate)) return 'expired'      // 已过期 → 红色
  if (m.stock < 5 && isExpiringSoon(m.expireDate)) return 'expired'  // 库存不足+临期 → 红色
  if (m.stock < 5) return 'low'                       // 仅库存不足 → 橙色
  if (isExpiringSoon(m.expireDate)) return 'expiring' // 仅临期 → 红色
  return 'normal'
}

const medicineOptions = computed(() => {
  return medicineList.value.map(m => ({
    value: m.id,
    label: m.name + (m.specification ? ` (${m.specification})` : ''),
    stockUnit: m.stockUnit
  }))
})

// 当前选中的药品单位
const currentMedicineUnit = computed(() => {
  const med = medicineList.value.find(m => m.id === planForm.medicineId)
  return med?.stockUnit || ''
})

// 当前选中药品的详细信息（适应症、用法用量）
const currentMedicineInfo = computed(() => {
  return medicineList.value.find(m => m.id === planForm.medicineId)
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
  // 管理员：重置为当前查看的成员
  if (userStore.isAdmin) {
    createPlanMemberId.value = selectedMemberId.value || userStore.member?.id
  }
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
  // 替换常见分隔符为统一格式
  const normalized = d.trim().replace(/\//g, '-').replace(/\./g, '-')
  // 验证格式 YYYY-MM-DD
  const match = normalized.match(/^(\d{4})-(\d{1,2})-(\d{1,2})$/)
  if (!match) return null
  // 验证日期有效性
  const year = parseInt(match[1])
  const month = parseInt(match[2])
  const day = parseInt(match[3])
  if (month < 1 || month > 12 || day < 1 || day > 31) return null
  if (month === 2 && day > 29) return null
  if ([4,6,9,11].includes(month) && day > 30) return null
  // 返回标准化格式
  return `${year}-${String(month).padStart(2,'0')}-${String(day).padStart(2,'0')}`
}

const slotOptions = [ { value: '早', label: '早' }, { value: '中', label: '中' }, { value: '晚', label: '晚' }, { value: '睡前', label: '睡前' } ]
const dayOptions = [ { value: '1', label: '一' }, { value: '2', label: '二' }, { value: '3', label: '三' }, { value: '4', label: '四' }, { value: '5', label: '五' }, { value: '6', label: '六' }, { value: '7', label: '日' } ]

// 全选/取消全选服药星期
const toggleAllDays = (checked) => {
  if (checked) {
    planForm.takeDays = ['1', '2', '3', '4', '5', '6', '7']
  } else {
    planForm.takeDays = []
  }
}

// 获取药品单位
const getMedicineUnit = (medicineId) => {
  const med = medicineList.value.find(m => m.id === medicineId)
  return med?.stockUnit || ''
}

// 格式化服药时间显示（早中晚睡前顺序）
const formatRemindSlots = (slots) => {
  if (!slots) return ''
  const order = { '早': 1, '中': 2, '晚': 3, '睡前': 4 }
  return slots.split(',').sort((a, b) => (order[a] || 5) - (order[b] || 5)).join(',')
}

const ocrFiles = ref([])
const ocrPreviews = ref([])
const ocrFileInputRef = ref(null)
const ocrCameraInputRef = ref(null)

const openOcrAlbumPicker = () => {
  ocrFileInputRef.value?.click()
}

const openOcrCameraPicker = () => {
  ocrCameraInputRef.value?.click()
}

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
  if (!planForm.medicineId || !planForm.dosage || planForm.remindSlots.length === 0) {
    formError.value = '请填写完整的必要信息'; return;
  }
  formError.value = ''
  submitting.value = true
  try {
    await createPlan({
      medicineId: Number(planForm.medicineId), userId: Number(planUserId.value), dosage: planForm.dosage,
      remindSlots: planForm.remindSlots.join(','), 
      takeDays: planForm.takeDays.length > 0 ? planForm.takeDays.join(',') : '1,2,3,4,5,6,7',
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

// 删除计划
const handleDeletePlan = async (plan) => {
  try {
    await ElMessageBox.confirm('确定要删除这个用药计划吗？', '提示', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await deletePlan(plan.id)
    ElMessage.success('计划已删除')
    load()
  } catch (err) {
    if (err !== 'cancel') {
      ElMessage.error(err?.message || '删除失败')
    }
  }
}

const submitMedicine = async () => {
  // 验证日期格式
  if (!medicineForm.expireDate || !medicineForm.expireDate.trim()) {
    formError.value = '请填写效期'
    return
  }
  const normalizedDate = normalizeDate(medicineForm.expireDate)
  if (!normalizedDate) {
    formError.value = '效期格式错误，请使用 YYYY-MM-DD 格式（如：2026-12-31）'
    return
  }
  if (!medicineForm.name || !medicineForm.specification || medicineForm.stock === undefined || !medicineForm.stockUnit) {
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
      expireDate: normalizedDate, usageNotes: medicineForm.usageNotes || null,
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
  // 验证日期格式
  if (!medicineForm.expireDate || !medicineForm.expireDate.trim()) {
    formError.value = '请填写效期'
    return
  }
  const normalizedDate = normalizeDate(medicineForm.expireDate)
  if (!normalizedDate) {
    formError.value = '效期格式错误，请使用 YYYY-MM-DD 格式（如：2026-12-31）'
    return
  }
  if (!medicineForm.name || !medicineForm.specification || medicineForm.stock === undefined || !medicineForm.stockUnit) {
    ElMessage.warning('请填写完整的必要信息'); return;
  }
  formError.value = ''
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
    await deleteMedicine(editingMedicine.value.id)
    ElMessage.success('删除成功')
    showEditMedicine.value = false
    await load()
  } catch (e) {
    if (e !== 'cancel' && e !== 'close') {
      ElMessage.error(e?.message || '删除失败')
    }
  }
}

onMounted(async () => {
  window.addEventListener('resize', handleResize)
  await load()
  await nextTick()
  await handleQuickAction()
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
})

watch(
  () => route.query.action,
  async (val, oldVal) => {
    if (!val || val === oldVal) return
    await handleQuickAction()
  }
)

watch(
  () => selectedMemberId.value,
  async (val, oldVal) => {
    if (!val || val === oldVal) return
    await loadTodayPendingCount()
  }
)
</script>

<template>
  <BaseLayout>
    <div class="medicine-page-container">
      
      <!-- 模块 1：顶部数据看板 (代替废话欢迎语，提供3个核心指标) -->
      <div class="page-section-title">📦 用药概览</div>
      <div class="stats-dashboard">
        <div class="stat-card blue" @click="scrollToSection('plans')" style="cursor: pointer;">
          <div class="stat-icon">💊</div>
          <div class="stat-info">
            <span class="stat-num">{{ todayPendingCount }}</span>
            <span class="stat-desc">今日待服(次)</span>
          </div>
        </div>
        <div class="stat-card orange" @click="scrollToSection('meds', 'low')" style="cursor: pointer;">
          <div class="stat-icon">⚠️</div>
          <div class="stat-info">
            <span class="stat-num">{{ lowStockCount }}</span>
            <span class="stat-desc">库存紧张(款)</span>
          </div>
        </div>
        <div class="stat-card red" @click="scrollToSection('meds', 'expiring')" style="cursor: pointer;">
          <div class="stat-icon">⏳</div>
          <div class="stat-info">
            <span class="stat-num">{{ expiringCount }}</span>
            <span class="stat-desc">30天内过期</span>
          </div>
        </div>
      </div>

      <!-- 模块 2：用药计划 -->
      <section id="section-plans" class="med-section">
        <h2 class="section-title">
          🗓️ 提醒计划
          <span v-if="currentViewerName" class="viewer-tag">查看：{{ currentViewerName }}</span>
        </h2>
        
        <!-- 管理员：成员选择 + 新建按钮同一行 -->
        <div v-if="userStore.isAdmin" class="plan-header-row">
          <div class="member-switch">
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
          <button v-if="canCreatePlan" class="btn-create" @click="showCreatePlan = true; resetPlanForm()">+ 新建计划</button>
        </div>
        
        <!-- 非管理员：只显示新建按钮 -->
        <div v-else-if="canCreatePlan" class="plan-header-row">
          <button class="btn-create" @click="showCreatePlan = true; resetPlanForm()">+ 新建计划</button>
        </div>
        
        <div v-if="displayPlans.length > 0" class="plan-list mt-3">
          <div v-for="plan in displayPlans" :key="plan.id" class="plan-card" :class="{ 'is-disabled': plan.status === 0 }">
            <div class="plan-header">
              <span v-if="userStore.isAdmin" class="plan-badge">{{ plan.userId == currentMemberId ? '我' : '家人' }}</span>
              <h3 class="plan-name">{{ medicineOptions.find(m => m.value == plan.medicineId)?.label || '未知药品' }}</h3>
            </div>
            <div class="plan-tags">
              <span class="plan-tag time-tag">{{ formatRemindSlots(plan.remindSlots) }}</span>
              <span class="plan-tag day-tag">{{ ['一','二','三','四','五','六','日'].map((d, i) => plan.takeDays?.includes(String(i+1)) ? d : '').filter(d=>d).join(',') || '每天' }}</span>
              <span class="plan-tag dosage-tag">每次 {{ plan.dosage }}{{ getMedicineUnit(plan.medicineId) }}</span>
            </div>
            <button class="btn-delete-plan" @click.stop="handleDeletePlan(plan)" title="删除">🗑️</button>
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

      <!-- 模块 3：我的药箱 -->
      <section id="section-meds" class="med-section">
        <h2 class="section-title">💊 我的药箱 <span class="title-count">({{ medicineList.length }})</span></h2>
        
        <div class="plan-header-row">
          <div class="search-wrapper">
            <span class="search-icon">🔍</span>
            <input type="text" v-model="searchMedQuery" placeholder="搜索药名..." class="search-input" />
          </div>
          <button class="btn-solid" @click="showAddMedicine = true; resetMedicineForm()">+ 录入</button>
        </div>
        
        <div class="custom-tabs sm-tabs mb-4">
          <button class="tab-item" :class="{ active: activeMedFilter === 'all' }" @click="activeMedFilter = 'all'">全部</button>
          <button class="tab-item text-orange" :class="{ active: activeMedFilter === 'low' }" @click="activeMedFilter = 'low'">库存紧张</button>
          <button class="tab-item text-red" :class="{ active: activeMedFilter === 'expiring' }" @click="activeMedFilter = 'expiring'">即将过期</button>
        </div>
        
        <div v-if="displayMedicines.length > 0" class="med-grid">
          <div v-for="m in displayMedicines" :key="m.id" class="med-card" :class="getMedicineStatus(m)" @click="onMedicineClick(m)">
            <div class="med-header">
              <span v-if="m.imageUrl" class="med-img">
                <img :src="m.imageUrl" />
              </span>
              <span v-else class="med-fallback-icon">💊</span>
              <h3 class="med-name">{{ m.name }}</h3>
            </div>
            <div class="med-tags">
              <span class="med-tag spec-tag">{{ m.specification || '暂无规格' }}</span>
              <span class="med-tag stock-tag" :class="{ 'is-low': m.stock < 5, 'is-expired': isExpired(m.expireDate) }">{{ isExpired(m.expireDate) ? '⚠️ 告罄' : '余量: ' + (m.stock ?? 0) + m.stockUnit }}</span>
              <span v-if="m.expireDate" class="med-tag expire-tag" :class="{ 'is-expiring': isExpiringSoon(m.expireDate), 'is-expired': isExpired(m.expireDate) }">{{ isExpired(m.expireDate) ? '🚫 已过期' : (isExpiringSoon(m.expireDate) ? '⚠️ 临期' : '效期: ' + m.expireDate) }}</span>
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

    <el-drawer v-model="showCreatePlan" title="创建用药计划" :direction="drawerDirection" :size="drawerSize" :destroy-on-close="true" @closed="resetPlanForm" class="custom-drawer">
      <div class="form-wrapper">
        <!-- 管理员：选择为谁创建计划 -->
        <div v-if="userStore.isAdmin" class="form-group form-group-sm">
          <label>👤 为谁创建</label>
          <el-select v-model="createPlanMemberId" placeholder="选择成员" style="width:100%">
            <el-option :value="userStore.member?.id" :label="userStore.member?.nickname + '（我）'" />
            <el-option v-for="m in familyMembers" :key="m.userId" :value="m.userId" :label="m.nickname" />
          </el-select>
        </div>
        <div class="form-group form-group-sm">
          <label>💊 关联药品 <span class="req">*</span></label>
          <el-select 
            v-model="planForm.medicineId" 
            placeholder="搜索药品" 
            filterable 
            style="width:100%"
            popper-class="medicine-select-popper"
          >
            <el-option v-for="m in medicineOptions" :key="m.value" :label="m.label" :value="m.value" />
          </el-select>
        </div>
        
        <!-- 选中药品后显示参考信息 -->
        <div v-if="currentMedicineInfo && planForm.medicineId" class="medicine-info-hint">
          <div v-if="currentMedicineInfo.indication" class="hint-item">
            <span class="hint-label">适应症：</span>
            <span class="hint-value">{{ currentMedicineInfo.indication }}</span>
          </div>
          <div v-if="currentMedicineInfo.usageNotes" class="hint-item">
            <span class="hint-label">用法用量：</span>
            <span class="hint-value">{{ currentMedicineInfo.usageNotes }}</span>
          </div>
        </div>
        <div class="form-group form-group-sm">
          <label>💉 单次服用剂量 <span class="req">*</span></label>
          <div style="display:flex;gap:8px;align-items:center;">
            <el-input-number v-model="planForm.dosage" :min="1" :step="1" style="width:120px" />
            <span style="color:#64748b;font-size:0.9rem;">{{ currentMedicineUnit }}</span>
          </div>
        </div>
        <div class="form-group">
          <label>⏰ 服药时段 <span class="req">*</span></label>
          <el-checkbox-group v-model="planForm.remindSlots">
            <el-checkbox-button v-for="s in slotOptions" :key="s.value" :value="s.value">{{ s.label }}</el-checkbox-button>
          </el-checkbox-group>
        </div>
        <div class="form-group">
          <label>📅 服药频率</label>
          <div style="display:flex;gap:8px;align-items:center;margin-bottom:8px;">
            <el-checkbox :value="planForm.takeDays.length === 7" @change="toggleAllDays">全选</el-checkbox>
          </div>
          <el-checkbox-group v-model="planForm.takeDays">
            <el-checkbox-button v-for="d in dayOptions" :key="d.value" :value="d.value">{{ d.label }}</el-checkbox-button>
          </el-checkbox-group>
        </div>
        <div class="form-group"><label>📆 起止日期</label><div style="display:flex;gap:8px;"><el-date-picker v-model="planForm.startDate" type="date" value-format="YYYY-MM-DD" style="flex:1" /><el-date-picker v-model="planForm.endDate" type="date" placeholder="结束(可选)" value-format="YYYY-MM-DD" style="flex:1" /></div></div>
        <div class="form-group"><label>📝 备注(可选)</label><el-input v-model="planForm.planRemark" placeholder="如：饭后服用" /></div>
        <p v-if="formError" class="form-error">{{ formError }}</p>
      </div>
      <template #footer>
        <div style="display:flex;gap:12px;"><el-button style="flex:1" @click="showCreatePlan = false">取消</el-button><el-button type="primary" style="flex:1" :loading="submitting" @click="submitPlan">确认创建</el-button></div>
      </template>
    </el-drawer>

    <el-drawer v-model="showAddMedicine" title="新药入库" :direction="drawerDirection" :size="drawerSize" :destroy-on-close="true" @closed="resetMedicineForm" class="custom-drawer">
      <div class="form-wrapper">
        <div class="ocr-panel">
          <div class="ocr-images">
            <div v-for="(url, idx) in ocrPreviews" :key="idx" class="img-preview"><img :src="url" /><span class="img-close" @click.stop="removeOcrImage(idx)">×</span></div>
            <div v-if="ocrPreviews.length < 4" class="img-add-actions">
              <input ref="ocrFileInputRef" type="file" accept="image/*" hidden @change="handleMedicineFile" />
              <input ref="ocrCameraInputRef" type="file" accept="image/*" capture="environment" hidden @change="handleMedicineFile" />
              <button v-if="isMobile" type="button" class="img-add-btn" @click="openOcrCameraPicker">
                <span class="icon">📸</span><span class="txt">拍照</span>
              </button>
              <button type="button" class="img-add-btn" @click="openOcrAlbumPicker">
                <span class="icon">{{ isMobile ? '🖼️' : '📷' }}</span>
                <span class="txt">{{ isMobile ? '相册' : '上传图片' }}</span>
              </button>
            </div>
          </div>
          <button v-if="ocrPreviews.length > 0 && !ocrLoading" class="ai-btn" @click.stop="startOcr">✨ AI 智能解析</button>
          <div v-if="ocrLoading" class="ocr-loading-overlay">
            <div class="loading-icon">{{ ocrStep === 0 ? '📤' : (ocrStep === 1 ? '🔍' : '🧠') }}</div>
            <div class="loading-txt">{{ ocrStep === 0 ? '正在上传...' : (ocrStep === 1 ? '正在识别...' : '正在解析...') }}</div>
          </div>
        </div>
        <div class="form-group"><label>药品名称 <span class="req">*</span></label><el-input v-model="medicineForm.name" /></div>
        <div class="form-group"><label>规格 <span class="req">*</span></label><el-input v-model="medicineForm.specification" /></div>
        <div class="form-group"><label>适应症</label><el-input v-model="medicineForm.indication" /></div>
        <div class="form-group"><label>当前库存 <span class="req">*</span></label><div style="display:flex;gap:8px;"><el-input-number v-model="medicineForm.stock" :min="0" style="flex:2" /><el-select v-model="medicineForm.stockUnit" placeholder="单位" style="flex:1"><el-option v-for="u in stockUnitOptions" :key="u" :label="u" :value="u" /></el-select></div></div>
        <div class="form-group"><label>有效期至 <span class="req">*</span></label><el-input v-model="medicineForm.expireDate" placeholder="如：2026-12-31" /></div>
        <div class="form-group"><label>用法说明建议</label><el-input type="textarea" v-model="medicineForm.usageNotes" :rows="2" /></div>
        <p v-if="formError" class="form-error">{{ formError }}</p>
      </div>
      <template #footer>
        <div style="display:flex;gap:12px;"><el-button style="flex:1" @click="showAddMedicine = false">取消</el-button><el-button type="primary" style="flex:1" :loading="submitting" @click="submitMedicine">确认入库</el-button></div>
      </template>
    </el-drawer>

    <!-- 药品详情抽屉 -->
    <el-drawer v-model="showEditMedicine" title="药品详情" :direction="drawerDirection" :size="drawerSize" class="custom-drawer" destroy-on-close>
      <div v-if="editingMedicine" class="detail-wrapper">
        <!-- 头部 -->
        <div class="detail-header">
          <div class="detail-cover">
            <img v-if="editingMedicine.imageUrl" :src="editingMedicine.imageUrl" />
            <span v-else class="detail-fallback-icon">💊</span>
          </div>
          <div class="detail-title">
            <h3 class="detail-name">{{ editingMedicine.name }}</h3>
            <p class="detail-spec">{{ editingMedicine.specification || '暂无规格' }}</p>
          </div>
        </div>
        
        <!-- 信息卡片 -->
        <div class="detail-info-card">
          <div class="info-row">
            <span class="info-label">💡 适应症</span>
            <span class="info-value">{{ editingMedicine.indication || '暂无说明' }}</span>
          </div>
          <div class="info-row">
            <span class="info-label">📖 用法用量</span>
            <span class="info-value">{{ editingMedicine.usageNotes || '暂无说明' }}</span>
          </div>
          <div class="info-row-row">
            <div class="info-col">
              <span class="info-label">📦 剩余库存</span>
              <span class="info-value highlight">{{ editingMedicine.stock ?? 0 }}<small>{{ editingMedicine.stockUnit }}</small></span>
            </div>
            <div class="info-col">
              <span class="info-label">🗓️ 有效期至</span>
              <span class="info-value">{{ editingMedicine.expireDate || '-' }}</span>
            </div>
          </div>
        </div>
      </div>
      <template #footer>
        <div class="detail-footer">
          <el-button type="danger" plain @click="deleteCurrentMedicine">删除</el-button>
          <div class="detail-actions">
            <el-button plain @click="openEditForm">编辑</el-button>
            <el-button type="primary" @click="handleCreatePlanFromDetail">制定计划</el-button>
          </div>
        </div>
      </template>
    </el-drawer>

    <!-- 编辑药品抽屉 -->
    <el-drawer v-model="showEditMedicineForm" title="更新药品信息" :direction="drawerDirection" :size="drawerSize" :destroy-on-close="true" @closed="cancelEdit" class="custom-drawer">
      <div class="edit-form-wrapper">
        <!-- 上传图片 -->
        <div class="edit-cover-wrap" @click="$refs.editFileInput.click()">
          <img v-if="coverUrl || editingMedicine?.imageUrl" :src="coverUrl || editingMedicine?.imageUrl" />
          <span v-else class="edit-cover-placeholder">📷 上传照片</span>
          <input ref="editFileInput" type="file" accept="image/*" hidden @change="(e) => handleCoverFile(e, true)" />
        </div>
        
        <!-- 表单 -->
        <div class="form-group"><label>💊 名称</label><el-input v-model="medicineForm.name" placeholder="药品名称" /></div>
        <div class="form-group">
          <label>📦 库存 <span class="req">*</span></label>
          <div class="stock-input-wrap">
            <el-input-number v-model="medicineForm.stock" :min="0" :step="1" controls-position="right" />
            <el-select v-model="medicineForm.stockUnit" placeholder="单位">
              <el-option v-for="u in stockUnitOptions" :key="u" :label="u" :value="u" />
            </el-select>
          </div>
        </div>
        <div class="form-group">
          <label>🗓️ 效期 <span class="req">*</span></label>
          <el-input v-model="medicineForm.expireDate" placeholder="如：2026-12-31" />
        </div>
        <p v-if="formError" class="form-error">{{ formError }}</p>
      </div>
      <template #footer>
        <div class="form-footer">
          <el-button style="flex:1" @click="cancelEdit">返回</el-button>
          <el-button type="primary" style="flex:1" :loading="submitting" @click="submitEditMedicine">保存修改</el-button>
        </div>
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
  display: flex;
  flex-direction: column;
  gap: 24px;
}

/* 页面区块标题 */
.page-section-title {
  font-size: 1.1rem;
  font-weight: 600;
  color: var(--text-dark);
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #e5e7eb;
}

/* 顶部数据看板 */
.stats-dashboard {
  display: grid;
  grid-template-columns: repeat(3, minmax(100px, 1fr));
  gap: 12px;
}
.stat-card {
  background: var(--card-bg);
  border-radius: 16px;
  padding: 16px 20px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.03);
  border: 1px solid #fff;
  display: flex;
  align-items: center;
  gap: 16px;
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}
.stat-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 6px 20px rgba(0,0,0,0.08);
}
.stat-card.blue { background: linear-gradient(135deg, #f0f9ff 0%, #ffffff 100%); border-color: #e0f2fe; }
.stat-card.orange { background: linear-gradient(135deg, #fff7ed 0%, #ffffff 100%); border-color: #ffedd5; }
.stat-card.red { background: linear-gradient(135deg, #fef2f2 0%, #ffffff 100%); border-color: #fee2e2; }

.stat-icon {
  font-size: 1.8rem;
}
.stat-info { display: flex; flex-direction: column; }
.stat-num { font-size: 1.5rem; font-weight: 600; color: var(--text-dark); line-height: 1; }
.stat-card.blue .stat-num { color: #0284c7; }
.stat-card.orange .stat-num { color: #ea580c; }
.stat-card.red .stat-num { color: #dc2626; }
.stat-desc { font-size: 0.8rem; color: var(--text-gray); font-weight: 500; margin-top: 4px; }

/* === 模块结构 === */
.med-section {
  margin-bottom: 24px;
  padding-bottom: 24px;
  border-bottom: 1px solid var(--border-line);
}
.med-section:last-child { border-bottom: none; }
.section-title { 
  font-size: 1.1rem; 
  font-weight: 600; 
  color: var(--text-dark); 
  margin: 0 0 16px 0;
  padding-bottom: 12px;
  border-bottom: 1px solid #e5e7eb;
  display: flex;
  align-items: center;
}
.member-switch {
  display: flex;
  align-items: center;
  gap: 8px;
}
.switch-label { font-size: 0.85rem; color: var(--text-gray); }
.plan-header-row { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; gap: 12px; flex-wrap: wrap; }
.plan-header-row .search-wrapper { order: 2; flex: 1; min-width: 150px; }
.plan-header-row .btn-solid { order: 3; }
.viewer-tag {
  font-size: 0.8rem;
  font-weight: 500;
  color: #fff;
  background: #2d5f5d;
  padding: 4px 10px;
  border-radius: 12px;
  margin-left: 8px;
}
.title-count { font-size: 0.9rem; color: var(--text-gray); font-weight: normal; }
.btn-create { background: var(--primary); color: #fff; border: none; padding: 8px 16px; border-radius: 20px; font-weight: 600; font-size: 0.9rem; cursor: pointer; white-space: nowrap; }
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
.sm-tabs .tab-item.text-orange.active { background: #f97316; color: #fff; }
.sm-tabs .tab-item.text-red.active { background: #dc2626; color: #fff; }

/* === 折叠按钮 === */
.btn-fold {
  width: 100%; padding: 10px; margin-top: 8px;
  background: #f1f5f9; border: 1px solid #e2e8f0; border-radius: 10px;
  color: #64748b; font-size: 0.85rem; font-weight: 500;
  cursor: pointer; transition: all 0.2s;
}
.btn-fold:hover { background: #e0f2fe; color: var(--primary); border-color: var(--primary); }
.fold-wrapper { grid-column: 1 / -1; }

/* === 空状态 === */
.empty-box {
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  padding: 40px 0; color: #94a3b8;
}
.empty-icon { font-size: 3rem; margin-bottom: 12px; opacity: 0.5; }
.empty-box p { margin: 0; font-size: 0.9rem; }


/* ========================================= */
/* 3. 计划卡片：流式标签布局 */
/* ========================================= */
.plan-list { display: flex; flex-direction: column; gap: 10px; }
.plan-card {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-left: 4px solid #5bb5b3;
  border-radius: 12px;
  padding: 14px;
  padding-right: 40px;
  transition: all 0.2s;
  box-shadow: 0 2px 8px rgba(0,0,0,0.03);
  position: relative;
}
.plan-card:hover { box-shadow: 0 4px 16px rgba(45, 95, 93, 0.15); transform: translateY(-1px); }
.plan-card.is-disabled { opacity: 0.5; background: #f8fafc; border-left-color: #94a3b8; }

.plan-header { display: flex; align-items: center; gap: 8px; margin-bottom: 10px; }
.plan-badge { background: linear-gradient(135deg, #e0f2fe 0%, #bae6fd 100%); color: #0369a1; font-size: 0.7rem; padding: 3px 10px; border-radius: 12px; font-weight: 600; }
.plan-name { font-size: 0.95rem; font-weight: 600; color: #1e293b; margin: 0; display: flex; align-items: center; gap: 6px; }
.plan-name::before { content: '💊'; font-size: 0.9rem; }
.btn-delete-plan { position: absolute; right: 12px; top: 50%; transform: translateY(-50%); background: none; border: none; cursor: pointer; font-size: 0.9rem; opacity: 0.3; transition: 0.2s; }
.btn-delete-plan:hover { opacity: 1; }

.plan-tags { display: flex; flex-wrap: wrap; gap: 8px; }
.plan-tag {
  font-size: 0.75rem;
  padding: 5px 12px;
  border-radius: 20px;
  font-weight: 500;
  display: flex; align-items: center; gap: 4px;
}
.time-tag { background: linear-gradient(135deg, #fef3c7 0%, #fde68a 100%); color: #b45309; }
.time-tag::before { content: '⏰'; font-size: 0.7rem; }
.day-tag { background: linear-gradient(135deg, #e0e7ff 0%, #c7d2fe 100%); color: #4338ca; }
.day-tag::before { content: '📅'; font-size: 0.7rem; }
.dosage-tag { background: linear-gradient(135deg, #dcfce7 0%, #bbf7d0 100%); color: #15803d; }
.dosage-tag::before { content: '💉'; font-size: 0.7rem; }


/* ========================================= */
/* 4. 药箱卡片：流式标签布局 */
/* ========================================= */
.header-tools { display: flex; align-items: center; gap: 12px; }
.search-wrapper { display: flex; align-items: center; background: #f1f5f9; border-radius: 20px; padding: 6px 16px; border: 1px solid transparent; flex: 1; min-width: 150px; transition: 0.2s;}
.search-wrapper:focus-within { border-color: var(--primary); background: #fff; }
.search-icon { font-size: 0.9rem; margin-right: 8px; color: #94a3b8; }
.search-input { flex: 1; border: none; background: transparent; font-size: 0.9rem; outline: none; padding: 4px 0; color: var(--text-dark); width: 100%;}

.med-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(280px, 1fr)); gap: 12px; }
.med-card {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-left: 4px solid #5bb5b3;
  border-radius: 12px;
  padding: 12px;
  cursor: pointer;
  transition: all 0.2s;
  box-shadow: 0 2px 8px rgba(0,0,0,0.03);
}
.med-card:hover { border-color: var(--primary); box-shadow: 0 4px 16px rgba(45,95,93,0.15); transform: translateY(-1px); }
/* 库存紧张（<5）：橙色左边框 */
.med-card.low { border-left-color: #f97316; }
/* 即将过期（30天内）：红色左边框 */
.med-card.expiring { border-left-color: #ef4444; }
/* 已过期：深红色左边框 */
.med-card.expired { border-left-color: #991b1b; }

.med-header { display: flex; align-items: center; gap: 8px; margin-bottom: 8px; }
.med-img { width: 32px; height: 32px; border-radius: 6px; overflow: hidden; flex-shrink: 0; border: 1px solid #f1f5f9; }
.med-img img { width: 100%; height: 100%; object-fit: cover; }
.med-fallback-icon { font-size: 1.2rem; }
.med-name { font-size: 0.9rem; font-weight: 600; color: #1e293b; margin: 0; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }

.med-tags { display: flex; flex-wrap: wrap; gap: 6px; }
.med-tag {
  font-size: 0.7rem;
  padding: 3px 8px;
  border-radius: 12px;
  font-weight: 500;
  display: flex;
  align-items: center;
  gap: 2px;
}
.spec-tag { background: #f1f5f9; color: #64748b; }
.stock-tag { background: #f0fdf4; color: #15803d; }
.stock-tag.is-low { background: #fff7ed; color: #ea580c; }
.stock-tag.is-expired { background: #fef2f2; color: #dc2626; }
.expire-tag { background: #f8fafc; color: #94a3b8; }
.expire-tag.is-expiring { background: #fef2f2; color: #dc2626; }
.expire-tag.is-expired { background: #7f1d1d; color: #fecaca; }


/* ========================================= */
/* 5. 抽屉表单与 OCR 内部样式 */
/* ========================================= */
.form-wrapper { display: flex; flex-direction: column; gap: 10px; padding-bottom: 20px;}
.form-group { display: flex; flex-direction: column; gap: 4px; }
.form-group-sm { margin-bottom: 8px; }
.form-group label { font-size: 0.85rem; font-weight: 600; color: var(--text-dark); }
.medicine-info-hint { background: #f8fafc; border: 1px solid #e2e8f0; border-radius: 10px; padding: 12px; margin-bottom: 8px; }
.medicine-info-hint .hint-item { margin-bottom: 6px; }
.medicine-info-hint .hint-item:last-child { margin-bottom: 0; }
.medicine-info-hint .hint-label { font-size: 0.8rem; font-weight: 600; color: #64748b; }
.medicine-info-hint .hint-value { font-size: 0.85rem; color: #334155; }
.req { color: var(--danger); margin-left: 2px; }
.form-error { color: var(--danger); font-size: 0.85rem; background: #fef2f2; padding: 10px; border-radius: 8px; margin-top: -8px; }

/* 药品详情抽屉样式 */
.detail-wrapper { display: flex; flex-direction: column; gap: 16px; }
.detail-header { display: flex; gap: 16px; align-items: center; padding-bottom: 16px; border-bottom: 1px solid #e2e8f0; }
.detail-cover { width: 72px; height: 72px; border-radius: 12px; background: #f8fafc; border: 1px solid #e2e8f0; display: flex; align-items: center; justify-content: center; overflow: hidden; flex-shrink: 0; }
.detail-cover img { width: 100%; height: 100%; object-fit: cover; }
.detail-fallback-icon { font-size: 2.2rem; }
.detail-title { flex: 1; min-width: 0; }
.detail-name { font-size: 1.1rem; font-weight: 600; color: #1e293b; margin: 0 0 4px 0; }
.detail-spec { font-size: 0.85rem; color: #64748b; margin: 0; }

.detail-info-card { background: #f8fafc; border-radius: 12px; padding: 16px; display: flex; flex-direction: column; gap: 12px; }
.info-row { display: flex; flex-direction: column; gap: 4px; }
.info-label { font-size: 0.75rem; font-weight: 600; color: #94a3b8; text-transform: uppercase; letter-spacing: 0.5px; }
.info-value { font-size: 0.9rem; color: #334155; line-height: 1.5; }
.info-value.highlight { color: var(--primary); font-weight: 600; font-size: 1.1rem; }
.info-value small { font-size: 0.8rem; font-weight: 400; margin-left: 4px; color: #64748b; }
.info-row-row { display: flex; gap: 16px; border-top: 1px dashed #e2e8f0; padding-top: 12px; margin-top: 4px; }
.info-col { flex: 1; display: flex; flex-direction: column; gap: 4px; }

.detail-footer { display: flex; justify-content: space-between; align-items: center; }

@media (hover: hover) {
  .tab-item:hover:not(.active) {
    transform: translateY(-1px);
    box-shadow: 0 6px 12px rgba(45, 95, 93, 0.08);
  }

  .btn-create:hover,
  .btn-solid:hover {
    transform: translateY(-1px);
    box-shadow: 0 8px 14px rgba(45, 95, 93, 0.2);
  }

  .search-wrapper:hover {
    transform: translateY(-1px);
    box-shadow: 0 8px 14px rgba(45, 95, 93, 0.08);
  }

  .detail-info-card:hover {
    transform: translateY(-1px);
    box-shadow: 0 8px 16px rgba(45, 95, 93, 0.1);
  }
}
.detail-actions { display: flex; gap: 8px; }

/* 编辑药品抽屉样式 */
.edit-form-wrapper { display: flex; flex-direction: column; gap: 14px; }
.edit-cover-wrap { width: 100px; height: 100px; border-radius: 16px; margin: 0 auto 16px; background: #f8fafc; border: 2px dashed #cbd5e1; display: flex; align-items: center; justify-content: center; cursor: pointer; overflow: hidden; }
.edit-cover-wrap img { width: 100%; height: 100%; object-fit: cover; }
.edit-cover-placeholder { font-size: 0.8rem; color: #94a3b8; text-align: center; }
.stock-input-wrap { display: flex; gap: 8px; }
.stock-input-wrap .el-input-number { flex: 1; }
.stock-input-wrap .el-select { width: 80px; }
.input-hint { font-size: 0.75rem; color: #94a3b8; margin-top: 4px; }
.form-footer { display: flex; gap: 12px; }

/* OCR 图片区 */
.ocr-panel { border: 2px dashed #cbd5e1; border-radius: 16px; padding: 16px; background: #f8fafc; position: relative; }
.ocr-images { display: flex; gap: 10px; flex-wrap: wrap; margin-bottom: 12px; }
.img-preview { position: relative; width: 70px; height: 70px; border-radius: 10px; overflow: hidden; border: 1px solid #e2e8f0; }
.img-preview img { width: 100%; height: 100%; object-fit: cover; }
.img-close { position: absolute; top: 4px; right: 4px; width: 20px; height: 20px; background: rgba(0,0,0,0.6); color: #fff; border-radius: 50%; display: flex; align-items: center; justify-content: center; cursor: pointer; font-size: 12px; }
.img-add-actions { display: flex; gap: 8px; }
.img-add-btn { width: 70px; height: 70px; background: #fff; border: 1px solid #e2e8f0; border-radius: 10px; display: flex; flex-direction: column; align-items: center; justify-content: center; cursor: pointer; color: var(--primary); font: inherit; }
.img-add-btn .txt { font-size: 0.72rem; }
.ai-btn { width: 100%; background: linear-gradient(135deg, var(--primary) 0%, #1e403f 100%); color: white; border: none; padding: 10px; border-radius: 10px; font-weight: 600; cursor: pointer; box-shadow: 0 4px 12px rgba(45,95,93,0.2); }
.ocr-loading-overlay { position: absolute; inset: 0; background: rgba(255,255,255,0.9); display: flex; flex-direction: column; align-items: center; justify-content: center; border-radius: 16px; backdrop-filter: blur(2px);}
.loading-icon { font-size: 2rem; margin-bottom: 8px; animation: pulse 1s infinite;}
.loading-txt { font-weight: 600; color: var(--primary); }
@keyframes pulse { 0%, 100% { opacity: 1; transform: scale(1); } 50% { opacity: 0.7; transform: scale(1.1); } }


/* ========================================= */
/* 6. 移动端响应式适配 */
@media (max-width: 768px) {
  .medicine-page-container { padding: 12px; gap: 16px; }
  .stats-dashboard { grid-template-columns: repeat(3, 1fr); gap: 8px; }
  .stat-card { padding: 12px; }
  .stat-icon { font-size: 1.4rem; }
  .stat-num { font-size: 1.2rem; }
  .stat-desc { font-size: 0.7rem; }
  
  .med-section { margin-bottom: 16px; }
  .plan-header-row { flex-direction: column; align-items: stretch; }
  .plan-header-row .search-wrapper { order: 0; }
  .plan-header-row .btn-solid { order: 1; }
  
  .med-grid { grid-template-columns: 1fr; gap: 12px; }
  
  /* Drawer 侧滑抽屉样式优化 */
  :deep(.custom-drawer) { width: 92% !important; border-top-left-radius: 24px; border-bottom-left-radius: 24px; }
  :deep(.custom-drawer .el-drawer__header) { margin-bottom: 8px; padding: 16px 20px 8px; }
  :deep(.custom-drawer .el-drawer__title) { font-size: 1rem; font-weight: 600; }
  
  /* 移动端底部抽屉样式 */
  @media (max-width: 768px) {
    :deep(.custom-drawer) { 
      width: 100% !important; 
      border-radius: 24px 24px 0 0 !important;
      border-top-left-radius: 24px;
    }
  }

  /* 药品选择下拉框最大高度，超出可滚动 */
  :deep(.medicine-select-popper) {
    max-height: 240px !important;
  }
  :deep(.medicine-select-popper .el-select-dropdown__list) {
    max-height: 240px !important;
    overflow-y: auto;
  }
}
</style>
