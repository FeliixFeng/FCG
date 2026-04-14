<script setup>
import BaseLayout from '../components/common/BaseLayout.vue'
import { onMounted, ref, reactive, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { fetchMedicineList, createMedicine, fetchPlanList, createPlan, recognizeMedicine, uploadFile, updateMedicine, deleteMedicine, fetchMedicine, updatePlan, deletePlan } from '../utils/api'
import { useUserStore } from '../stores/user'
import { compressImage, fileToBase64 } from '../utils/image'

const userStore = useUserStore()

const medicineList = ref([])
const planList = ref([])
const loading = ref(false)
const error = ref('')

const activeTab = ref('cabinet')
const familyMembers = ref([])
const planFormMemberId = ref(null)

const showCreatePlan = ref(false)
const submitting = ref(false)
const formError = ref('')

const showAddMedicine = ref(false)
const showEditMedicine = ref(false)
const showEditMedicineForm = ref(false)
const editingMedicine = ref(null)
const ocrLoading = ref(false)
const ocrStep = ref(0)
const previewUrl = ref('')
const coverUrl = ref('')

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

const lowStockCount = computed(() =>
  medicineList.value.filter(m => m.stock != null && m.stock < 10).length
)

const expiringSoonCount = computed(() => {
  const now = new Date()
  const soon = new Date(now.getTime() + 30 * 24 * 60 * 60 * 1000)
  return medicineList.value.filter(m => {
    if (!m.expireDate) return false
    const d = new Date(m.expireDate)
    return d > now && d <= soon
  }).length
})

const getMedicineName = (medicineId) =>
  medicineList.value.find(m => m.id == medicineId)?.name || '未知药品'

const getExpireStatus = (expireDate) => {
  if (!expireDate) return null
  const now = new Date()
  const d = new Date(expireDate)
  if (d < now) return 'expired'
  const soon = new Date(now.getTime() + 30 * 24 * 60 * 60 * 1000)
  return d <= soon ? 'soon' : 'ok'
}

const medicineOptions = computed(() =>
  medicineList.value.map(m => ({
    value: m.id,
    label: m.name + (m.specification ? ` (${m.specification})` : '')
  }))
)

const stockUnitOptions = ['片', '粒', 'ml', '支', '瓶']

const planForm = reactive({
  medicineId: '',
  dosage: '',
  remindSlots: [],
  takeDays: [],
  startDate: new Date().toISOString().split('T')[0],
  endDate: '',
  planRemark: ''
})

const medicineForm = reactive({
  name: '',
  specification: '',
  indication: '',
  stock: '',
  stockUnit: '',
  expireDate: '',
  usageNotes: ''
})

const slotOptions = [
  { value: '早', label: '早' },
  { value: '中', label: '中' },
  { value: '晚', label: '晚' },
  { value: '睡前', label: '睡前' }
]

const dayOptions = [
  { value: '1', label: '一' },
  { value: '2', label: '二' },
  { value: '3', label: '三' },
  { value: '4', label: '四' },
  { value: '5', label: '五' },
  { value: '6', label: '六' },
  { value: '7', label: '日' }
]

const ocrFiles = ref([])
const ocrPreviews = ref([])

const resetPlanForm = () => {
  planFormMemberId.value = currentMemberId.value
  Object.assign(planForm, {
    medicineId: '', dosage: '', remindSlots: [], takeDays: [],
    startDate: new Date().toISOString().split('T')[0], endDate: '', planRemark: ''
  })
  formError.value = ''
}

const resetMedicineForm = () => {
  Object.assign(medicineForm, {
    name: '', specification: '', indication: '', stock: '', stockUnit: '', expireDate: '', usageNotes: ''
  })
  previewUrl.value = ''
  coverUrl.value = ''
  ocrFiles.value = []
  ocrPreviews.value = []
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
    }
  } catch (err) {
    console.error('OCR识别失败:', err)
    ElMessage.error('识别失败，请重试')
  } finally {
    clearInterval(stepTimer)
    ocrLoading.value = false
    ocrStep.value = 0
  }
}

const submitPlan = async () => {
  if (!planForm.medicineId) { formError.value = '请选择药品'; return }
  if (!planForm.dosage) { formError.value = '请输入剂量'; return }
  if (planForm.remindSlots.length === 0) { formError.value = '请选择服药时段'; return }
  if (planForm.takeDays.length === 0) { formError.value = '请选择服药星期'; return }
  formError.value = ''
  submitting.value = true
  try {
    await createPlan({
      medicineId: Number(planForm.medicineId),
      userId: Number(planFormMemberId.value || currentMemberId.value),
      dosage: planForm.dosage,
      remindSlots: planForm.remindSlots.join(','),
      takeDays: planForm.takeDays.join(','),
      startDate: planForm.startDate,
      endDate: planForm.endDate || null,
      planRemark: planForm.planRemark || null
    })
    showCreatePlan.value = false
    resetPlanForm()
    load()
    ElMessage.success('创建成功')
  } catch (err) {
    ElMessage.error(err?.message || '创建失败')
  } finally {
    submitting.value = false
  }
}

const submitMedicine = async () => {
  const nameVal = (medicineForm.name || '').trim()
  const specVal = (medicineForm.specification || '').trim()
  if (!nameVal) { formError.value = '请填写药品名称'; return }
  if (!specVal) { formError.value = '请填写规格'; return }
  if (!medicineForm.stock && medicineForm.stock !== 0) { formError.value = '请填写库存数量'; return }
  if (!medicineForm.stockUnit) { formError.value = '请选择库存单位'; return }
  if (!medicineForm.expireDate) { formError.value = '请填写有效期'; return }
  formError.value = ''
  submitting.value = true
  try {
    let imageUrl = null
    if (ocrFiles.value.length > 0) {
      const uploadRes = await uploadFile(ocrFiles.value[0], 'medicine')
      imageUrl = uploadRes.data || uploadRes.url || uploadRes
    }
    await createMedicine({
      name: medicineForm.name.trim(),
      specification: medicineForm.specification.trim(),
      stock: medicineForm.stock ? Number(medicineForm.stock) : null,
      stockUnit: medicineForm.stockUnit || null,
      expireDate: normalizeDate(medicineForm.expireDate),
      usageNotes: medicineForm.usageNotes || null,
      indication: medicineForm.indication || null,
      imageUrl
    })
    showAddMedicine.value = false
    resetMedicineForm()
    load()
    ElMessage.success('添加成功')
  } catch (err) {
    ElMessage.error(err?.message || '添加失败')
  } finally {
    submitting.value = false
  }
}

const onMedicineClick = async (medicine) => {
  try {
    const res = await fetchMedicine(medicine.id)
    editingMedicine.value = res.data
    medicineForm.name = res.data.name || ''
    medicineForm.specification = res.data.specification || ''
    medicineForm.indication = res.data.indication || ''
    medicineForm.stock = res.data.stock ?? ''
    medicineForm.stockUnit = res.data.stockUnit || ''
    medicineForm.expireDate = res.data.expireDate || ''
    medicineForm.usageNotes = res.data.usageNotes || ''
    showEditMedicine.value = true
  } catch (err) {
    ElMessage.error('获取详情失败')
  }
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
  const nameVal = (medicineForm.name || '').trim()
  const specVal = (medicineForm.specification || '').trim()
  if (!nameVal) { ElMessage.warning('请填写药品名称'); return }
  if (!specVal) { ElMessage.warning('请填写规格'); return }
  if (!medicineForm.stock && medicineForm.stock !== 0) { ElMessage.warning('请填写库存数量'); return }
  if (!medicineForm.stockUnit) { ElMessage.warning('请选择库存单位'); return }
  if (!medicineForm.expireDate) { ElMessage.warning('请填写有效期'); return }
  submitting.value = true
  try {
    let imageUrl = editingMedicine.value?.imageUrl || null
    if (coverUrl.value && coverUrl.value !== editingMedicine.value?.imageUrl) {
      const base64 = coverUrl.value.split(',')[1]
      const byteCharacters = atob(base64)
      const byteNumbers = new Array(byteCharacters.length)
      for (let i = 0; i < byteCharacters.length; i++) byteNumbers[i] = byteCharacters.charCodeAt(i)
      const byteArray = new Uint8Array(byteNumbers)
      const blob = new Blob([byteArray], { type: 'image/jpeg' })
      const file = new File([blob], 'medicine.jpg', { type: 'image/jpeg' })
      const uploadRes = await uploadFile(file, 'medicine')
      imageUrl = uploadRes.data || uploadRes.url || uploadRes
    }
    await updateMedicine(editingMedicine.value.id, {
      name: medicineForm.name.trim(),
      specification: medicineForm.specification.trim(),
      stock: medicineForm.stock ? Number(medicineForm.stock) : null,
      stockUnit: medicineForm.stockUnit || null,
      expireDate: normalizeDate(medicineForm.expireDate),
      usageNotes: medicineForm.usageNotes || null,
      indication: medicineForm.indication || null,
      imageUrl
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
    await ElMessageBox.confirm('确认删除该药品？删除后无法恢复。', '删除药品', {
      confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning'
    })
    await deleteMedicine(editingMedicine.value.id)
    showEditMedicine.value = false
    load()
    ElMessage.success('删除成功')
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(e?.message || '删除失败')
  }
}

const openCreatePlanFromDetail = () => {
  resetPlanForm()
  planForm.medicineId = editingMedicine.value.id
  showEditMedicine.value = false
  showCreatePlan.value = true
}

const togglePlanStatus = async (plan) => {
  const newStatus = plan.status === 1 ? 0 : 1
  try {
    await updatePlan(plan.id, { status: newStatus })
    plan.status = newStatus
  } catch (e) {
    ElMessage.error(e?.message || '操作失败')
  }
}

const deletePlanItem = async (plan) => {
  try {
    await ElMessageBox.confirm('确认删除该计划？', '删除计划', {
      confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning'
    })
    await deletePlan(plan.id)
    planList.value = planList.value.filter(p => p.id !== plan.id)
    ElMessage.success('删除成功')
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(e?.message || '删除失败')
  }
}

const loadFamilyMembers = async () => {
  try {
    familyMembers.value = await userStore.fetchMembers()
  } catch (e) {
    console.error(e)
  }
}

onMounted(async () => {
  planFormMemberId.value = currentMemberId.value
  if (userStore.isAdmin) await loadFamilyMembers()
  load()
})
</script>

<template>
  <BaseLayout>
    <section class="page">
      <!-- Tab bar -->
      <div class="tab-bar">
        <button class="tab-btn" :class="{ active: activeTab === 'cabinet' }" @click="activeTab = 'cabinet'">
          药箱
        </button>
        <button class="tab-btn" :class="{ active: activeTab === 'plan' }" @click="activeTab = 'plan'">
          计划
        </button>
      </div>

      <!-- 药箱 Tab -->
      <div v-if="activeTab === 'cabinet'" class="tab-content">
        <div class="section-header">
          <span class="section-title">我的药箱</span>
          <button class="btn-primary-sm" @click="showAddMedicine = true; resetMedicineForm()">+ 添加药品</button>
        </div>

        <div class="stats-row">
          <div class="stat-card">
            <div class="stat-num">{{ medicineList.length }}</div>
            <div class="stat-label">药品总数</div>
          </div>
          <div class="stat-card" :class="{ 'stat-warn': lowStockCount > 0 }">
            <div class="stat-num">{{ lowStockCount }}</div>
            <div class="stat-label">库存紧张</div>
          </div>
          <div class="stat-card" :class="{ 'stat-warn': expiringSoonCount > 0 }">
            <div class="stat-num">{{ expiringSoonCount }}</div>
            <div class="stat-label">即将过期</div>
          </div>
        </div>

        <div v-if="loading" class="empty-state">加载中...</div>
        <div v-else-if="medicineList.length === 0" class="empty-state">暂无药品，点击添加</div>
        <div v-else class="medicine-grid">
          <div v-for="m in medicineList" :key="m.id" class="med-card" @click="onMedicineClick(m)">
            <img v-if="m.imageUrl" :src="m.imageUrl" class="card-thumb" />
            <div class="med-info">
              <div class="med-name">{{ m.name }}</div>
              <div class="med-spec">{{ m.specification || '-' }}</div>
              <div class="med-footer">
                <span class="card-stock" :class="{ 'stock-low': m.stock != null && m.stock < 10 }">
                  库存 {{ m.stock ?? 0 }}{{ m.stockUnit || '' }}
                </span>
                <span v-if="m.expireDate" class="card-expire"
                  :class="{
                    'expire-expired': getExpireStatus(m.expireDate) === 'expired',
                    'expire-soon': getExpireStatus(m.expireDate) === 'soon'
                  }">
                  {{ m.expireDate }}
                </span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 计划 Tab -->
      <div v-if="activeTab === 'plan'" class="tab-content">
        <div class="section-header">
          <span class="section-title">用药计划</span>
          <button class="btn-primary-sm" @click="showCreatePlan = true; resetPlanForm()">+ 创建计划</button>
        </div>

        <div v-if="loading" class="empty-state">加载中...</div>
        <div v-else-if="planList.length === 0" class="empty-state">暂无计划，点击创建</div>
        <div v-else class="plan-list">
          <div v-for="plan in planList" :key="plan.id" class="plan-card" :class="{ 'plan-disabled': plan.status === 0 }">
            <div class="plan-status-dot" :class="plan.status === 1 ? 'dot-active' : 'dot-inactive'"></div>
            <div class="plan-body">
              <div class="plan-name-row">
                <span class="plan-name">{{ getMedicineName(plan.medicineId) }}</span>
                <span class="plan-dosage-badge">{{ plan.dosage }}</span>
              </div>
              <div class="plan-slots">
                <span v-for="slot in plan.remindSlots?.split(',')" :key="slot" class="slot-tag">{{ slot }}</span>
              </div>
              <div class="plan-days">
                <span v-for="d in dayOptions" :key="d.value" class="day-chip"
                  :class="{
                    'day-active': plan.takeDays?.split(',').includes(d.value),
                    'day-today': d.value === String(new Date().getDay() || 7)
                  }">{{ d.label }}</span>
              </div>
              <div class="plan-range">{{ plan.startDate }} ~ {{ plan.endDate || '长期' }}</div>
            </div>
            <div class="plan-actions">
              <button class="action-btn" @click.stop="togglePlanStatus(plan)">
                {{ plan.status === 1 ? '停用' : '启用' }}
              </button>
              <button class="action-btn danger" @click.stop="deletePlanItem(plan)">删除</button>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- 创建计划弹窗 -->
    <el-dialog v-model="showCreatePlan" title="创建用药计划" width="min(460px, 92vw)"
      :close-on-click-modal="false" @close="resetPlanForm" @keyup.enter="submitPlan">
      <div class="dlg-form">
        <div class="dlg-field">
          <label class="dlg-label">药品</label>
          <el-select v-model="planForm.medicineId" placeholder="选择药品" style="width:100%">
            <el-option v-for="m in medicineOptions" :key="m.value" :value="m.value" :label="m.label" />
          </el-select>
        </div>

        <div v-if="userStore.isAdmin && familyMembers.length > 0" class="dlg-field">
          <label class="dlg-label">为谁创建</label>
          <el-select v-model="planFormMemberId" style="width:100%">
            <el-option v-for="m in familyMembers" :key="m.id" :value="m.id" :label="m.nickname || m.username" />
          </el-select>
        </div>
        <div v-else class="dlg-field">
          <label class="dlg-label">成员</label>
          <el-input :model-value="currentMemberName" disabled />
        </div>

        <div class="dlg-field">
          <label class="dlg-label">剂量</label>
          <el-input v-model="planForm.dosage" placeholder="如：1片、5ml" />
        </div>

        <div class="dlg-field">
          <label class="dlg-label">服药时段</label>
          <div class="chip-group">
            <label v-for="s in slotOptions" :key="s.value" class="chip-label">
              <input type="checkbox" :value="s.value" v-model="planForm.remindSlots" hidden />
              <span class="chip" :class="{ 'chip-active': planForm.remindSlots.includes(s.value) }">{{ s.label }}</span>
            </label>
          </div>
        </div>

        <div class="dlg-field">
          <label class="dlg-label">服药星期</label>
          <div class="chip-group">
            <label v-for="d in dayOptions" :key="d.value" class="chip-label">
              <input type="checkbox" :value="d.value" v-model="planForm.takeDays" hidden />
              <span class="chip" :class="{ 'chip-active': planForm.takeDays.includes(d.value) }">{{ d.label }}</span>
            </label>
          </div>
        </div>

        <div class="dlg-field">
          <label class="dlg-label">日期范围</label>
          <div class="date-row">
            <el-date-picker v-model="planForm.startDate" type="date" value-format="YYYY-MM-DD"
              placeholder="开始日期" style="flex:1" />
            <span class="date-sep">至</span>
            <el-date-picker v-model="planForm.endDate" type="date" value-format="YYYY-MM-DD"
              placeholder="长期" style="flex:1" />
          </div>
        </div>

        <div class="dlg-field">
          <label class="dlg-label">备注（可选）</label>
          <el-input v-model="planForm.planRemark" placeholder="用药注意事项" />
        </div>

        <p v-if="formError" class="dlg-error">{{ formError }}</p>
      </div>
      <template #footer>
        <el-button @click="showCreatePlan = false; resetPlanForm()">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitPlan">确认创建</el-button>
      </template>
    </el-dialog>

    <!-- 添加药品弹窗 -->
    <el-dialog v-model="showAddMedicine" title="添加药品" width="min(460px, 92vw)"
      :close-on-click-modal="false" @close="resetMedicineForm" @keyup.enter="submitMedicine">
      <div class="dlg-form">
        <div class="image-uploader">
          <div class="image-list">
            <div v-for="(url, idx) in ocrPreviews" :key="idx" class="image-thumb">
              <img :src="url" />
              <span v-if="idx === 0" class="image-tag">主图</span>
              <span class="image-del" @click.stop="removeOcrImage(idx)">×</span>
            </div>
            <div v-if="ocrPreviews.length < 4" class="image-add" @click="$refs.fileInput.click()">
              <input ref="fileInput" type="file" accept="image/*" hidden @change="handleMedicineFile" />
              <span>+</span>
            </div>
          </div>
          <el-button v-if="ocrPreviews.length > 0 && !ocrLoading" size="small" @click.stop="startOcr">
            AI 识别
          </el-button>
          <div v-if="ocrLoading" class="ocr-loading-row">
            <span class="step-icon" style="animation: bounce 0.6s ease-in-out infinite">
              {{ ['💊', '📷', '🧠'][ocrStep] }}
            </span>
            <span class="step-text">{{ ['分析图片中', '识别文字中', '生成结果中'][ocrStep] }}</span>
          </div>
        </div>

        <el-input v-model="medicineForm.name" placeholder="药品名称 *" />
        <el-input v-model="medicineForm.specification" placeholder="规格（如 0.3g×20粒）" />
        <el-input v-model="medicineForm.indication" placeholder="适应症" />
        <div class="date-row">
          <el-input v-model="medicineForm.stock" type="number" placeholder="库存数量" style="flex:1" />
          <el-select v-model="medicineForm.stockUnit" placeholder="单位" style="width:90px">
            <el-option v-for="u in stockUnitOptions" :key="u" :value="u" :label="u" />
          </el-select>
        </div>
        <el-date-picker v-model="medicineForm.expireDate" type="date" value-format="YYYY-MM-DD"
          placeholder="有效期" style="width:100%" />
        <el-input v-model="medicineForm.usageNotes" type="textarea" :rows="2" placeholder="用法用量/注意事项" />

        <p v-if="formError" class="dlg-error">{{ formError }}</p>
      </div>
      <template #footer>
        <el-button @click="showAddMedicine = false; resetMedicineForm()">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitMedicine">确认添加</el-button>
      </template>
    </el-dialog>

    <!-- 药品详情弹窗 -->
    <el-dialog v-model="showEditMedicine" :title="editingMedicine?.name" width="min(420px, 92vw)"
      :close-on-click-modal="true">
      <div v-if="editingMedicine" class="detail-body">
        <div v-if="editingMedicine.imageUrl" class="detail-img-wrap">
          <img :src="editingMedicine.imageUrl" class="detail-img" />
        </div>
        <div class="detail-grid">
          <div class="detail-item">
            <span class="detail-label">规格</span>
            <span class="detail-val">{{ editingMedicine.specification || '-' }}</span>
          </div>
          <div class="detail-item">
            <span class="detail-label">库存</span>
            <span class="detail-val" :class="{ 'val-warn': editingMedicine.stock != null && editingMedicine.stock < 10 }">
              {{ editingMedicine.stock ?? 0 }}{{ editingMedicine.stockUnit || '' }}
            </span>
          </div>
          <div class="detail-item">
            <span class="detail-label">有效期</span>
            <span class="detail-val"
              :class="{
                'val-danger': getExpireStatus(editingMedicine.expireDate) === 'expired',
                'val-warn': getExpireStatus(editingMedicine.expireDate) === 'soon'
              }">
              {{ editingMedicine.expireDate || '-' }}
            </span>
          </div>
        </div>
        <div v-if="editingMedicine.indication" class="detail-section">
          <span class="detail-label">适应症</span>
          <p class="detail-text">{{ editingMedicine.indication }}</p>
        </div>
        <div v-if="editingMedicine.usageNotes" class="detail-section">
          <span class="detail-label">用法用量</span>
          <p class="detail-text">{{ editingMedicine.usageNotes }}</p>
        </div>
      </div>
      <template #footer>
        <div style="display:flex;justify-content:space-between;width:100%">
          <el-button type="danger" plain @click="deleteCurrentMedicine">删除</el-button>
          <div style="display:flex;gap:8px">
            <el-button @click="openCreatePlanFromDetail">创建计划</el-button>
            <el-button type="primary" @click="openEditForm">编辑</el-button>
          </div>
        </div>
      </template>
    </el-dialog>

    <!-- 编辑药品弹窗 -->
    <el-dialog v-model="showEditMedicineForm" title="编辑药品" width="min(460px, 92vw)"
      :close-on-click-modal="false" @close="cancelEdit">
      <div class="dlg-form">
        <div class="edit-cover-wrap" @click="$refs.editFileInput.click()">
          <img v-if="coverUrl || editingMedicine?.imageUrl" :src="coverUrl || editingMedicine?.imageUrl" class="edit-cover-img" />
          <div v-else class="edit-cover-placeholder">
            <span>📷</span>
            <span class="edit-cover-hint">点击上传封面</span>
          </div>
          <input ref="editFileInput" type="file" accept="image/*" hidden @change="(e) => handleCoverFile(e, true)" />
        </div>

        <el-input v-model="medicineForm.name" placeholder="药品名称 *" />
        <el-input v-model="medicineForm.specification" placeholder="规格（如 0.3g×20粒）" />
        <el-input v-model="medicineForm.indication" placeholder="适应症" />
        <div class="date-row">
          <el-input v-model="medicineForm.stock" type="number" placeholder="库存数量" style="flex:1" />
          <el-select v-model="medicineForm.stockUnit" placeholder="单位" style="width:90px">
            <el-option v-for="u in stockUnitOptions" :key="u" :value="u" :label="u" />
          </el-select>
        </div>
        <el-date-picker v-model="medicineForm.expireDate" type="date" value-format="YYYY-MM-DD"
          placeholder="有效期" style="width:100%" />
        <el-input v-model="medicineForm.usageNotes" type="textarea" :rows="2" placeholder="用法用量/注意事项" />
      </div>
      <template #footer>
        <el-button @click="cancelEdit">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitEditMedicine">保存</el-button>
      </template>
    </el-dialog>
  </BaseLayout>
</template>

<style scoped>
/* ===== Layout ===== */
.page { padding: 20px; display: flex; flex-direction: column; gap: 16px; min-height: 100%; }

/* ===== Tab bar ===== */
.tab-bar {
  display: flex; gap: 4px;
  background: rgba(255,255,255,0.6); backdrop-filter: blur(8px);
  border-radius: 12px; padding: 4px;
  border: 1px solid rgba(255,255,255,0.8);
}
.tab-btn {
  flex: 1; padding: 8px 0; border-radius: 9px; font-size: 0.9rem; font-weight: 500;
  color: #888; background: transparent; transition: all 0.2s; cursor: pointer;
}
.tab-btn.active { background: #fff; color: #2d5f5d; font-weight: 600; box-shadow: 0 1px 4px rgba(0,0,0,0.1); }

/* ===== Tab content ===== */
.tab-content { display: flex; flex-direction: column; gap: 14px; }
.section-header { display: flex; justify-content: space-between; align-items: center; }
.section-title { font-size: 1rem; font-weight: 600; color: #1a1a1a; }
.btn-primary-sm {
  background: #2d5f5d; color: #fff; padding: 6px 14px;
  border-radius: 20px; font-size: 0.85rem; cursor: pointer; transition: opacity 0.15s;
}
.btn-primary-sm:active { opacity: 0.8; }

/* ===== Stats row ===== */
.stats-row { display: grid; grid-template-columns: repeat(3, 1fr); gap: 10px; }
.stat-card {
  background: rgba(255,255,255,0.85); backdrop-filter: blur(8px);
  border-radius: 12px; padding: 12px 8px; text-align: center;
  border: 1px solid rgba(255,255,255,0.9);
}
.stat-card.stat-warn { border-color: #fbbf24; }
.stat-num { font-size: 1.4rem; font-weight: 700; color: #2d5f5d; }
.stat-card.stat-warn .stat-num { color: #d97706; }
.stat-label { font-size: 0.72rem; color: #999; margin-top: 2px; }

/* ===== Medicine grid ===== */
.medicine-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 10px; }
.med-card {
  background: rgba(255,255,255,0.9); border-radius: 14px; overflow: hidden;
  cursor: pointer; transition: transform 0.15s, box-shadow 0.15s;
  border: 1px solid rgba(255,255,255,0.9); display: flex; flex-direction: column;
}
.med-card:active { transform: scale(0.97); }
.card-thumb { width: 100%; height: 90px; object-fit: cover; flex-shrink: 0; }
.med-info { padding: 10px; display: flex; flex-direction: column; gap: 4px; flex: 1; }
.med-name { font-weight: 600; font-size: 0.9rem; color: #1a1a1a; line-height: 1.3; }
.med-spec { font-size: 0.78rem; color: #888; }
.med-footer { display: flex; justify-content: space-between; align-items: center; margin-top: 4px; flex-wrap: wrap; gap: 2px; }
.card-stock { font-size: 0.75rem; color: #666; }
.stock-low { color: #d97706; font-weight: 600; }
.card-expire { font-size: 0.72rem; color: #aaa; }
.expire-expired { color: #dc2626; font-weight: 600; }
.expire-soon { color: #d97706; font-weight: 600; }

/* ===== Plan list ===== */
.plan-list { display: flex; flex-direction: column; gap: 10px; }
.plan-card {
  background: rgba(255,255,255,0.9); border-radius: 14px; padding: 14px;
  display: flex; gap: 10px; align-items: flex-start;
  border: 1px solid rgba(255,255,255,0.9); transition: opacity 0.2s;
}
.plan-disabled { opacity: 0.5; }
.plan-status-dot { width: 8px; height: 8px; border-radius: 50%; margin-top: 6px; flex-shrink: 0; }
.dot-active { background: #22c55e; box-shadow: 0 0 0 3px rgba(34,197,94,0.2); }
.dot-inactive { background: #d1d5db; }
.plan-body { flex: 1; display: flex; flex-direction: column; gap: 6px; min-width: 0; }
.plan-name-row { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.plan-name { font-weight: 600; font-size: 0.95rem; color: #1a1a1a; }
.plan-dosage-badge { font-size: 0.75rem; background: #e0f2fe; color: #0369a1; padding: 2px 8px; border-radius: 10px; white-space: nowrap; }
.plan-slots { display: flex; gap: 5px; flex-wrap: wrap; }
.slot-tag { font-size: 0.75rem; background: #f0fdf4; color: #16a34a; padding: 2px 8px; border-radius: 10px; }
.plan-days { display: flex; gap: 4px; }
.day-chip {
  width: 22px; height: 22px; border-radius: 50%; font-size: 0.7rem;
  display: flex; align-items: center; justify-content: center;
  background: #f3f4f6; color: #9ca3af;
}
.day-active { background: #2d5f5d; color: #fff; }
.day-active.day-today { background: #1a3d3b; box-shadow: 0 0 0 2px rgba(45,95,93,0.3); }
.plan-range { font-size: 0.75rem; color: #aaa; }
.plan-actions { display: flex; flex-direction: column; gap: 6px; flex-shrink: 0; }
.action-btn {
  font-size: 0.75rem; padding: 4px 10px; border-radius: 8px;
  border: 1px solid #d1d5db; color: #555; background: #fff; cursor: pointer;
  white-space: nowrap; transition: all 0.15s;
}
.action-btn:hover { border-color: #2d5f5d; color: #2d5f5d; }
.action-btn.danger { border-color: #fca5a5; color: #dc2626; }
.action-btn.danger:hover { background: #fef2f2; }

/* ===== Empty state ===== */
.empty-state { text-align: center; padding: 40px 20px; color: #aaa; font-size: 0.9rem; }

/* ===== Dialog form ===== */
.dlg-form { display: flex; flex-direction: column; gap: 12px; }
.dlg-field { display: flex; flex-direction: column; gap: 6px; }
.dlg-label { font-size: 0.82rem; color: #666; font-weight: 500; }
.dlg-error { color: #dc2626; font-size: 0.82rem; margin: 0; }
.date-row { display: flex; gap: 8px; align-items: center; }
.date-sep { color: #aaa; font-size: 0.85rem; flex-shrink: 0; }
.chip-group { display: flex; gap: 6px; flex-wrap: wrap; }
.chip-label { cursor: pointer; }
.chip { display: block; padding: 5px 12px; border-radius: 20px; font-size: 0.85rem; background: #f3f4f6; color: #555; transition: all 0.15s; user-select: none; }
.chip-active { background: #2d5f5d; color: #fff; }

/* ===== Image uploader ===== */
.image-uploader { display: flex; flex-direction: column; gap: 10px; }
.image-list { display: flex; gap: 8px; flex-wrap: wrap; }
.image-thumb { position: relative; width: 68px; height: 68px; border-radius: 8px; overflow: hidden; }
.image-thumb img { width: 100%; height: 100%; object-fit: cover; }
.image-tag { position: absolute; bottom: 0; left: 0; right: 0; background: rgba(0,0,0,0.55); color: #fff; font-size: 0.65rem; text-align: center; padding: 2px; }
.image-del { position: absolute; top: 2px; right: 2px; width: 18px; height: 18px; background: rgba(0,0,0,0.55); color: #fff; border-radius: 50%; text-align: center; line-height: 18px; cursor: pointer; font-size: 0.8rem; }
.image-add { width: 68px; height: 68px; border: 2px dashed #dcdfe6; border-radius: 8px; display: grid; place-items: center; font-size: 1.4rem; color: #ccc; cursor: pointer; }
.ocr-loading-row { display: flex; align-items: center; gap: 8px; padding: 4px 0; }
.step-icon { font-size: 1.4rem; }
.step-text { font-size: 0.85rem; color: #2d5f5d; font-weight: 500; }
@keyframes bounce {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-6px); }
}

/* ===== Detail dialog ===== */
.detail-body { display: flex; flex-direction: column; gap: 14px; }
.detail-img-wrap { border-radius: 12px; overflow: hidden; }
.detail-img { width: 100%; height: 160px; object-fit: cover; }
.detail-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 10px; }
.detail-item { background: #f8f9fa; border-radius: 10px; padding: 10px; }
.detail-label { display: block; font-size: 0.72rem; color: #999; margin-bottom: 4px; }
.detail-val { font-size: 0.9rem; font-weight: 600; color: #1a1a1a; }
.val-warn { color: #d97706; }
.val-danger { color: #dc2626; }
.detail-section { display: flex; flex-direction: column; gap: 4px; }
.detail-text { font-size: 0.88rem; color: #444; line-height: 1.6; margin: 0; }

/* ===== Edit cover ===== */
.edit-cover-wrap {
  width: 120px; height: 120px; border-radius: 14px; overflow: hidden;
  margin: 0 auto; cursor: pointer; background: #f3f4f6;
  display: flex; align-items: center; justify-content: center;
}
.edit-cover-img { width: 100%; height: 100%; object-fit: cover; }
.edit-cover-placeholder { display: flex; flex-direction: column; align-items: center; gap: 4px; color: #aaa; font-size: 1.6rem; }
.edit-cover-hint { font-size: 0.75rem; }

@media (max-width: 768px) {
  .page { padding: 12px; gap: 12px; }
  .medicine-grid { gap: 8px; }
  .card-thumb { height: 70px; }
  .stats-row { gap: 8px; }
  .stat-num { font-size: 1.2rem; }
}
</style>
