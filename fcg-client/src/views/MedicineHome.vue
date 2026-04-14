<script setup>
import BaseLayout from '../components/common/BaseLayout.vue'
import { onMounted, ref, reactive, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { fetchMedicineList, createMedicine, fetchPlanList, createPlan, recognizeMedicine, uploadFile, updateMedicine, deleteMedicine, fetchMedicine } from '../utils/api'
import { useUserStore } from '../stores/user'
import { compressImage, fileToBase64 } from '../utils/image'

const userStore = useUserStore()

const medicineList = ref([])
const planList = ref([])
const loading = ref(false)
const error = ref('')

const showCreatePlan = ref(false)
const submitting = ref(false)
const formError = ref('')

const showAddMedicine = ref(false)
const showEditMedicine = ref(false)  // 详情弹窗
const showEditMedicineForm = ref(false)  // 编辑弹窗
const editingMedicine = ref(null)
const ocrLoading = ref(false)
const ocrStep = ref(0) // 0: 分析图片, 1: 识别文字, 2: 生成结果
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

const todayPlans = computed(() => {
  const today = new Date().getDay() || 7
  return planList.value.filter(p => {
    if (p.status === 0) return false
    const days = p.takeDays?.split(',') || []
    return days.includes(String(today))
  })
})

const lowStockCount = computed(() => {
  return medicineList.value.filter(m => m.stock != null && m.stock < 10).length
})

const plansByUser = computed(() => {
  const map = {}
  planList.value.forEach(p => {
    const userId = p.userId
    if (!map[userId]) map[userId] = []
    map[userId].push(p)
  })
  return map
})

const medicineOptions = computed(() => {
  return medicineList.value.map(m => ({
    value: m.id,
    label: m.name + (m.specification ? ` (${m.specification})` : '')
  }))
})

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

const resetPlanForm = () => {
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
    if (isEdit) {
      coverUrl.value = base64
    } else {
      previewUrl.value = base64
    }
  } catch (err) {
    console.error(err)
  }
}

// 标准化日期格式 (OCR可能返回 2027-3-2，需要转成 2027-03-02)
const normalizeDate = (d) => {
  if (!d) return null
  const match = d.match(/^(\d{4})-(\d{1,2})-(\d{1,2})$/)
  if (match) {
    return `${match[1]}-${match[2].padStart(2,'0')}-${match[3].padStart(2,'0')}`
  }
  return d
}

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

const handleMedicineFile = async (e) => {
  const files = Array.from(e.target.files || [])
  if (files.length === 0) return
  // 每次只添加一张
  const file = files[0]
  const compressed = await compressImage(file)
  const base64 = await fileToBase64(compressed)
  ocrFiles.value.push(compressed)
  ocrPreviews.value.push(base64)
  // 默认第一张作为封面
  if (ocrPreviews.value.length === 1) {
    coverUrl.value = base64
  }
  // 清空input以便重复选择同一张图片
  e.target.value = ''
}

const removeOcrImage = (idx) => {
  ocrFiles.value.splice(idx, 1)
  ocrPreviews.value.splice(idx, 1)
  // 更新封面
  if (idx === 0 && ocrPreviews.value.length > 0) {
    coverUrl.value = ocrPreviews.value[0]
  } else if (ocrPreviews.value.length === 0) {
    coverUrl.value = ''
  }
}

const startOcr = async () => {
  if (ocrFiles.value.length === 0) return
  ocrLoading.value = true
  ocrStep.value = 0
  
  // 步骤动画：每个2秒，最后一个一直跳
  const stepTimer = setInterval(() => {
    if (ocrStep.value < 2) {
      ocrStep.value++
    }
  }, 2000)
  
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
      userId: Number(currentMemberId.value),
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
  if (!nameVal) { 
    formError.value = '请填写药品名称'; 
    return 
  }
  if (!specVal) { 
    formError.value = '请填写规格'; 
    return 
  }
  if (!medicineForm.stock && medicineForm.stock !== 0) { 
    formError.value = '请填写库存数量'; 
    return 
  }
  if (!medicineForm.stockUnit) { 
    formError.value = '请选择库存单位'; 
    return 
  }
  if (!medicineForm.expireDate) { 
    formError.value = '请填写有效期'; 
    return 
  }
  formError.value = ''
  submitting.value = true
  try {
    // Upload first image to OSS if exists
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

const isToday = (dayStr) => {
  const today = new Date().getDay() || 7
  return dayStr?.includes(String(today))
}

// 点击药品卡片 - 查看详情
const onMedicineClick = async (medicine) => {
  try {
    const res = await fetchMedicine(medicine.id)
    editingMedicine.value = res.data
    // 回填到表单
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

// 打开编辑表单
const openEditForm = () => {
  // 回填到表单（已有数据在 medicineForm 中）
  coverUrl.value = editingMedicine.value?.imageUrl || ''
  showEditMedicine.value = false
  showEditMedicineForm.value = true
}

// 取消编辑 - 返回详情页
const cancelEdit = () => {
  showEditMedicineForm.value = false
  resetMedicineForm()
  // 重新打开详情
  onMedicineClick(editingMedicine.value)
}

// 保存编辑（编辑弹窗）
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
    // 如果有新图片，先上传
    let imageUrl = editingMedicine.value?.imageUrl || null
    if (coverUrl.value && coverUrl.value !== editingMedicine.value?.imageUrl) {
      // coverUrl 是 base64，需要转为 File
      const base64 = coverUrl.value.split(',')[1]
      const byteCharacters = atob(base64)
      const byteNumbers = new Array(byteCharacters.length)
      for (let i = 0; i < byteCharacters.length; i++) {
        byteNumbers[i] = byteCharacters.charCodeAt(i)
      }
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

onMounted(() => load())
</script>

<template>
  <BaseLayout>
    <section class="page">
      <div class="header">
        <h1>💊 用药计划</h1>
        <button class="btn-primary" @click="showCreatePlan = true">+ 创建</button>
      </div>

      <div class="status-bar">
        <div class="status-card">
          <div class="status-num">{{ todayPlans.length }}</div>
          <div class="status-label">今日待服</div>
        </div>
        <div class="status-card warning">
          <div class="status-num">{{ lowStockCount }}</div>
          <div class="status-label">库存紧张</div>
        </div>
      </div>

      <div class="section">
        <div class="section-header">
          <h2>我的药箱 ({{ medicineList.length }})</h2>
          <button class="btn-link" @click="showAddMedicine = true; resetMedicineForm()">+ 添加药品</button>
        </div>
        <div v-if="medicineList.length > 0" class="medicine-grid">
          <div v-for="m in medicineList" :key="m.id" class="medicine-card" @click="onMedicineClick(m)">
            <div class="medicine-card-header">
              <div class="medicine-name">{{ m.name }}</div>
              <span v-if="m.imageUrl" class="medicine-img-icon">📷</span>
            </div>
            <div class="medicine-spec">{{ m.specification || '-' }}</div>
            <div class="medicine-stock" :class="{ low: m.stock < 10 }">
              库存: {{ m.stock ?? 0 }}{{ m.stockUnit || '' }}
              <span v-if="m.expireDate" class="medicine-expire">· 有效期: {{ m.expireDate }}</span>
            </div>
          </div>
        </div>
        <div v-else class="empty-small">暂无药品，点击添加</div>
      </div>

      <p v-if="error" class="error">{{ error }}</p>
      <p v-if="loading" class="muted">加载中...</p>

      <div v-if="!loading && planList.length === 0 && !error" class="empty">
        <p>暂无用药计划</p>
        <p class="muted">点击右上角创建计划</p>
      </div>

      <div v-for="(plans, userId) in plansByUser" :key="userId" class="plan-group">
        <div class="group-header">{{ userId == currentMemberId ? currentMemberName : '成员' }}的计划</div>
        <div class="plan-list">
          <div v-for="plan in plans" :key="plan.id" class="plan-card" :class="{ disabled: plan.status === 0 }">
            <div class="plan-left">
              <div class="plan-medicine">{{ medicineOptions.find(m => m.value == plan.medicineId)?.label || '药品' }}</div>
              <div class="plan-slots">
                <span v-for="slot in plan.remindSlots?.split(',')" :key="slot" class="slot-tag">{{ slot }}</span>
              </div>
              <div class="plan-days" :class="{ today: isToday(plan.takeDays) }">
                {{ ['一二三四五六日'].map((d, i) => plan.takeDays?.includes(String(i+1)) ? d : '').filter(d => d).join('') || '���天' }}
              </div>
            </div>
            <div class="plan-right">
              <span class="plan-dosage">{{ plan.dosage }}</span>
            </div>
          </div>
        </div>
      </div>

      <div v-if="planList.length > 0" class="footer-tip">
        <button class="btn-link" @click="showAddMedicine = true; resetMedicineForm()">+ 添加新药品</button>
      </div>
    </section>

    <div v-if="showCreatePlan" class="modal-mask" @click.self="showCreatePlan = false; resetPlanForm()" @keyup.enter="submitPlan">
      <div class="modal">
        <h3>创建用药计划</h3>
        <div class="form">
          <select v-model="planForm.medicineId" class="input">
            <option value="">选择药品</option>
            <option v-for="m in medicineOptions" :key="m.value" :value="m.value">{{ m.label }}</option>
          </select>

          <input :value="currentMemberName" class="input" disabled />

          <input v-model="planForm.dosage" class="input" placeholder="剂量（如 1片）" />

          <div class="field-label">服药时段</div>
          <div class="checkbox-group">
            <label v-for="s in slotOptions" :key="s.value" class="checkbox">
              <input type="checkbox" :value="s.value" v-model="planForm.remindSlots" />
              <span>{{ s.label }}</span>
            </label>
          </div>

          <div class="field-label">服药星期</div>
          <div class="checkbox-group">
            <label v-for="d in dayOptions" :key="d.value" class="checkbox">
              <input type="checkbox" :value="d.value" v-model="planForm.takeDays" />
              <span>{{ d.label }}</span>
            </label>
          </div>

          <div class="form-row">
            <input v-model="planForm.startDate" class="input" type="date" />
            <span class="muted">至</span>
            <input v-model="planForm.endDate" class="input" type="date" />
          </div>

          <input v-model="planForm.planRemark" class="input" placeholder="用药注意（可选）" />
        </div>

        <p v-if="formError" class="error">{{ formError }}</p>
        <div class="modal-actions">
          <button class="btn ghost" @click="showCreatePlan = false; resetPlanForm()">取消</button>
          <button class="btn-primary" :disabled="submitting" @click="submitPlan">
            {{ submitting ? '提交中...' : '确认' }}
          </button>
        </div>
      </div>
    </div>

    <div v-if="showAddMedicine" class="modal-mask" @click.self="showAddMedicine = false; resetMedicineForm()" @keyup.enter="submitMedicine">
      <div class="modal">
        <h3>添加药品</h3>
        
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
          <button v-if="ocrPreviews.length > 0 && !ocrLoading" class="btn-ocr" @click.stop="startOcr">AI识别</button>
          <div v-if="ocrLoading" class="ocr-loading">
            <div class="ocr-step-single">
              <span class="step-icon">{{ ['💊', '📷', '🧠'][ocrStep] }}</span>
              <span class="step-text">{{ ['分析图片中', '识别文字中', '生成结果中'][ocrStep] }}</span>
            </div>
          </div>
        </div>
        
        <div class="form">
          <input v-model="medicineForm.name" class="input" placeholder="药品名称 *" />
          <input v-model="medicineForm.specification" class="input" placeholder="规格（如 0.3g*20粒）" />
          <input v-model="medicineForm.indication" class="input" placeholder="适应症" />
          <div class="form-row">
            <input v-model="medicineForm.stock" class="input" type="number" placeholder="库存数量" />
            <select v-model="medicineForm.stockUnit" class="input">
              <option value="">单位</option>
              <option v-for="u in stockUnitOptions" :key="u" :value="u">{{ u }}</option>
            </select>
          </div>
          <input v-model="medicineForm.expireDate" class="input" placeholder="有效期(如2026-12-31)" />
          <textarea v-model="medicineForm.usageNotes" class="input textarea" placeholder="用法用量/注意事项" rows="2" />
        </div>

        <p v-if="formError" class="error">{{ formError }}</p>
        <div class="modal-actions">
          <button class="btn ghost" @click="showAddMedicine = false; resetMedicineForm()">取消</button>
          <button class="btn-primary" :disabled="submitting" @click="submitMedicine" @keyup.enter="submitMedicine">
            {{ submitting ? '提交中...' : '确认添加' }}
          </button>
        </div>
      </div>
    </div>

    <!-- 药品详情弹窗 -->
    <div v-if="showEditMedicine" class="modal-mask" @click.self="showEditMedicine = false">
      <div class="modal modal-detail">
        <div class="detail-header">
          <div class="detail-cover-small" v-if="editingMedicine?.imageUrl">
            <img :src="editingMedicine.imageUrl" alt="药品图片" />
          </div>
          <div class="detail-header-text">
            <h2 class="detail-title">{{ editingMedicine?.name }}</h2>
            <p class="detail-spec">{{ editingMedicine?.specification }}</p>
          </div>
        </div>
        
        <div class="detail-info">
          <div class="info-row">
            <span class="info-label">适应症</span>
            <span class="info-value">{{ editingMedicine?.indication || '暂无' }}</span>
          </div>
          <div class="info-row">
            <span class="info-label">用法用量</span>
            <span class="info-value">{{ editingMedicine?.usageNotes || '暂无' }}</span>
          </div>
          <div class="info-grid">
            <div class="info-item">
              <span class="label">库存</span>
              <span class="value">{{ editingMedicine?.stock ?? 0 }}{{ editingMedicine?.stockUnit || '' }}</span>
            </div>
            <div class="info-item">
              <span class="label">有效期</span>
              <span class="value">{{ editingMedicine?.expireDate || '-' }}</span>
            </div>
          </div>
        </div>

        <div class="modal-actions">
          <button class="btn danger" @click="deleteCurrentMedicine">删除</button>
          <div class="action-right">
            <button class="btn ghost" @click="showEditMedicine = false">关闭</button>
            <button class="btn-primary" @click="openEditForm">编辑</button>
          </div>
        </div>
      </div>
    </div>

    <!-- 编辑药品弹窗 -->
    <div v-if="showEditMedicineForm" class="modal-mask" @click.self="cancelEdit">
      <div class="modal">
        <h3>编辑药品</h3>
        
        <div class="edit-cover-center" @click="$refs.editFileInput.click()">
          <div v-if="coverUrl || editingMedicine?.imageUrl" class="edit-cover-img">
            <img :src="coverUrl || editingMedicine?.imageUrl" alt="药品图片" />
          </div>
          <div v-else class="edit-cover-placeholder">
            <span class="plus">📷</span>
            <span class="text">点击上传</span>
          </div>
          <input ref="editFileInput" type="file" accept="image/*" hidden @change="(e) => handleCoverFile(e, true)" />
        </div>
        
        <div class="form">
          <input v-model="medicineForm.name" class="input" placeholder="药品名称 *" />
          <input v-model="medicineForm.specification" class="input" placeholder="规格（如 0.3g*20粒）" />
          <input v-model="medicineForm.indication" class="input" placeholder="适应症" />
          <div class="form-row">
            <input v-model="medicineForm.stock" class="input" type="number" placeholder="库存数量" />
            <select v-model="medicineForm.stockUnit" class="input">
              <option value="">单位</option>
              <option v-for="u in stockUnitOptions" :key="u" :value="u">{{ u }}</option>
            </select>
          </div>
          <input v-model="medicineForm.expireDate" class="input" placeholder="有效期(如2026-12-31)" />
          <textarea v-model="medicineForm.usageNotes" class="input textarea" placeholder="用法用量/注意事项" rows="2" />
        </div>

        <div class="modal-actions">
          <button class="btn ghost" @click="cancelEdit">取消</button>
          <button class="btn-primary" :disabled="submitting" @click="submitEditMedicine">
            {{ submitting ? '保存中...' : '保存' }}
          </button>
        </div>
      </div>
    </div>
  </BaseLayout>
</template>

<style scoped>
.page { padding: 20px; display: grid; gap: 16px; }
.header { display: flex; justify-content: space-between; align-items: center; }
.header h1 { font-size: 1.25rem; font-weight: 600; color: var(--primary); }

.status-bar { display: flex; gap: 12px; }
.status-card {
  flex: 1; background: #fff; border-radius: 12px; padding: 16px;
  text-align: center; border-bottom: 3px solid var(--primary);
}
.status-card.warning { border-bottom-color: #f59e0b; }
.status-num { font-size: 1.5rem; font-weight: 700; }
.status-label { font-size: 0.8rem; color: var(--muted); margin-top: 4px; }

.section { background: #fff; border-radius: 12px; padding: 16px; }
.section-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.section-header h2 { font-size: 1rem; font-weight: 600; }

.medicine-grid { display: grid; grid-template-columns: 1fr; gap: 8px; }
.medicine-card { background: #fff; border-radius: 12px; padding: 14px; cursor: pointer; transition: transform 0.15s, box-shadow 0.15s; border: 1px solid #f0f0f0; }
.medicine-card:active { transform: scale(0.98); }
.medicine-card-header { display: flex; justify-content: space-between; align-items: flex-start; gap: 8px; }
.medicine-name { font-weight: 600; font-size: 0.95rem; color: #333; line-height: 1.3; }
.medicine-img-icon { font-size: 0.85rem; }
.medicine-spec { font-size: 0.85rem; color: #666; margin-top: 6px; line-height: 1.4; }
.medicine-stock { font-size: 0.8rem; margin-top: 8px; color: #888; display: flex; flex-wrap: wrap; gap: 4px; }
.medicine-stock.low { color: #f59e0b; }
.medicine-expire { color: #888; font-size: 0.75rem; }
.empty-small { text-align: center; padding: 20px; color: var(--muted); font-size: 0.9rem; }

.plan-group { display: grid; gap: 8px; }
.group-header { font-size: 0.85rem; color: var(--muted); padding: 8px 0; }
.plan-list { display: grid; gap: 8px; }
.plan-card {
  background: #fff; border-radius: 12px; padding: 14px 16px;
  display: flex; justify-content: space-between; align-items: center;
}
.plan-card.disabled { opacity: 0.5; }
.plan-medicine { font-weight: 600; font-size: 0.95rem; }
.plan-slots { display: flex; gap: 6px; margin-top: 4px; flex-wrap: wrap; }
.slot-tag {
  font-size: 0.75rem; background: #e0f2fe; color: #0369a1;
  padding: 2px 8px; border-radius: 10px;
}
.plan-days { font-size: 0.8rem; color: var(--muted); margin-top: 4px; }
.plan-days.today { color: var(--primary); font-weight: 600; }
.plan-dosage { font-size: 0.85rem; color: var(--muted); }

.empty { text-align: center; padding: 40px; color: var(--muted); }
.footer-tip { text-align: center; padding: 20px; }
.btn-link { background: none; border: none; color: var(--primary); cursor: pointer; font-size: 0.9rem; }

.modal-mask { position: fixed; inset: 0; background: rgba(0,0,0,0.4); display: grid; place-items: center; z-index: 100; }
.modal { width: min(420px, 92vw); max-height: 90vh; overflow-y: auto; background: #fff; border-radius: 16px; padding: 16px; display: grid; gap: 12px; animation: fadeInUp 0.25s ease-out; }
.modal-detail { padding: 20px; overflow: hidden; }
.modal h3 { font-size: 1rem; font-weight: 600; text-align: center; margin: 0 0 8px 0; }

.ocr-area { border: 2px dashed #dcdfe6; border-radius: 12px; padding: 20px; text-align: center; cursor: pointer; }
.ocr-placeholder { color: var(--muted); font-size: 0.9rem; }
.preview-img img { max-height: 150px; border-radius: 8px; }
.preview-multiple { position: relative; }
.preview-multiple img { max-height: 120px; border-radius: 8px; }
.ocr-loading { 
  position: absolute; inset: 0; 
  background: rgba(255,255,255,0.7);
  display: flex; align-items: center; justify-content: center;
}
.ocr-step-single { display: flex; flex-direction: column; align-items: center; gap: 8px; }
.ocr-step-single .step-icon { font-size: 2rem; animation: bounce 0.6s ease-in-out infinite; }
@keyframes bounce {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-10px); }
}
.ocr-step-single .step-text { font-size: 1rem; color: var(--primary); font-weight: 600; }
.ocr-tip { font-size: 0.75rem; color: var(--muted); margin-top: 4px; }
.btn-ocr { margin-top: 8px; width: 100%; background: var(--primary); color: #fff; padding: 8px; border-radius: 8px; font-size: 0.9rem; }

.form { display: grid; gap: 12px; }
.form-row { display: flex; gap: 8px; align-items: center; }
.form-row .input { flex: 1; }
.form-row-2 { display: flex; gap: 12px; }
.form-row-2 .ocr-area { flex: 1; }

.image-uploader { display: grid; gap: 12px; }
.image-list { display: flex; gap: 8px; flex-wrap: wrap; }
.image-thumb { position: relative; width: 70px; height: 70px; border-radius: 8px; overflow: hidden; }
.image-thumb img { width: 100%; height: 100%; object-fit: cover; }
.image-tag { position: absolute; bottom: 0; left: 0; right: 0; background: rgba(0,0,0,0.6); color: #fff; font-size: 0.7rem; text-align: center; padding: 2px; }
.image-del { position: absolute; top: 2px; right: 2px; width: 18px; height: 18px; background: rgba(0,0,0,0.6); color: #fff; border-radius: 50%; text-align: center; line-height: 16px; cursor: pointer; }
.image-add { width: 70px; height: 70px; border: 2px dashed #dcdfe6; border-radius: 8px; display: grid; place-items: center; font-size: 1.5rem; color: #dcdfe6; cursor: pointer; }
.field-label { font-size: 0.85rem; color: var(--muted); margin-top: 8px; }
.checkbox-group { display: flex; gap: 8px; flex-wrap: wrap; }
.checkbox { display: flex; align-items: center; gap: 4px; font-size: 0.9rem; cursor: pointer; }
.checkbox input { display: none; }
.checkbox span { padding: 6px 12px; background: #f5f5f5; border-radius: 8px; }
.checkbox:has(input:checked) span { background: var(--primary); color: #fff; }

.input { padding: 10px 12px; border: 1px solid #dcdfe6; border-radius: 8px; font-size: 0.95rem; width: 100%; box-sizing: border-box; }
.input:disabled { background: #f5f5f5; }
.textarea { resize: vertical; min-height: 60px; }

.modal-actions { display: flex; gap: 12px; justify-content: space-between; margin-top: 8px; }
.modal-actions .action-right { display: flex; gap: 8px; }
.btn-primary { background: var(--primary); color: #fff; padding: 10px 20px; border-radius: 8px; font-size: 0.95rem; }
.btn-primary:disabled { opacity: 0.6; }
.btn.ghost { background: transparent; border: 1px solid var(--primary); color: var(--primary); padding: 10px 20px; border-radius: 8px; }
.btn.danger { background: #dc2626; color: #fff; padding: 10px 20px; border-radius: 8px; font-size: 0.95rem; }
.detail-item { display: flex; flex-direction: column; gap: 4px; }
.detail-label { font-size: 0.8rem; color: var(--muted); }
.detail-value { font-size: 0.95rem; color: #333; line-height: 1.4; }
.error { color: #dc2626; font-size: 0.85rem; }
.muted { color: var(--muted); }

/* 详情弹窗样式 */
.modal-detail { padding: 20px; }
.detail-header { display: flex; gap: 14px; margin-bottom: 16px; }
.detail-cover-small { width: 80px; height: 80px; border-radius: 12px; overflow: hidden; flex-shrink: 0; background: #f5f5f5; }
.detail-cover-small img { width: 100%; height: 100%; object-fit: cover; }
.detail-header-text { flex: 1; display: flex; flex-direction: column; justify-content: center; }
.detail-title { font-size: 1.15rem; font-weight: 600; color: #1a1a1a; margin: 0 0 6px 0; line-height: 1.3; }
.detail-spec { font-size: 0.9rem; color: #666; margin: 0; line-height: 1.4; }

.detail-info { background: linear-gradient(135deg, #f8f9fa 0%, #fff 100%); border-radius: 14px; padding: 16px; border: 1px solid #eee; }
.info-row { margin-bottom: 14px; }
.info-row:last-of-type { margin-bottom: 0; }
.info-label { display: block; font-size: 0.75rem; color: #999; margin-bottom: 4px; font-weight: 500; }
.info-value { font-size: 0.9rem; color: #333; line-height: 1.5; }
.info-grid { display: flex; gap: 16px; margin-top: 14px; padding-top: 14px; border-top: 1px dashed #eee; }
.info-item { flex: 1; }
.info-item .label { display: block; font-size: 0.75rem; color: #999; margin-bottom: 4px; }
.info-item .value { font-size: 1rem; font-weight: 600; color: var(--primary); }

/* 编辑封面 - 居中正方形 */
.edit-cover-center { width: 140px; height: 140px; border-radius: 16px; overflow: hidden; margin: 0 auto 16px; cursor: pointer; position: relative; background: #f5f5f5; }
.edit-cover-center .edit-cover-img { width: 100%; height: 100%; }
.edit-cover-center .edit-cover-img img { width: 100%; height: 100%; object-fit: cover; }
.edit-cover-center .edit-cover-placeholder { width: 100%; height: 100%; display: flex; flex-direction: column; align-items: center; justify-content: center; background: linear-gradient(135deg, #f8f9fa 0%, #e9ecef 100%); border: 2px dashed #dee2e6; border-radius: 16px; transition: all 0.2s; }
.edit-cover-center .edit-cover-placeholder:hover { border-color: var(--primary); background: linear-gradient(135deg, #e8f5f3 0%, #d0ede8 100%); }
.edit-cover-center .plus { font-size: 2.2rem; margin-bottom: 6px; }
.edit-cover-center .text { font-size: 0.8rem; color: #868e96; }

@keyframes fadeInUp {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}
.modal { animation: fadeInUp 0.25s ease-out; }

@media (max-width: 768px) {
  .page { padding: 12px; gap: 12px; }
  .header { flex-direction: row; align-items: center; }
  .header h1 { font-size: 1.1rem; }
  .btn-primary { padding: 8px 16px; font-size: 0.9rem; }
  .status-bar { gap: 8px; }
  .status-card { padding: 12px; }
  .status-num { font-size: 1.2rem; }
  .section { padding: 12px; }
  .medicine-grid { grid-template-columns: 1fr; gap: 8px; }
  .medicine-card { padding: 10px; }
  .modal { width: 94vw; padding: 12px; gap: 12px; }
  .modal h3 { font-size: 1rem; }
  .form { gap: 10px; }
  .form-row { flex-direction: column; gap: 8px; }
  .form-row .input { width: 100%; }
  .input { padding: 8px 10px; font-size: 0.9rem; }
  .modal-actions { flex-direction: column; }
  .modal-actions .action-right { justify-content: flex-end; }
  .btn.ghost, .btn-primary, .btn.danger { flex: 1; text-align: center; }
}
</style>