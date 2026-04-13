<script setup>
import BaseLayout from '../components/common/BaseLayout.vue'
import { onMounted, ref, reactive, computed } from 'vue'
import { fetchMedicineList, createMedicine, fetchPlanList, createPlan, recognizeMedicine } from '../utils/api'
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
const ocrLoading = ref(false)
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

const handleCoverFile = async (e) => {
  const file = e.target.files?.[0]
  if (!file) return
  try {
    coverUrl.value = await fileToBase64(await compressImage(file))
  } catch (err) {
    console.error(err)
  }
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
    formError.value = '识别失败，请重试'
  } finally {
    ocrLoading.value = false
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
  } catch (err) {
    formError.value = err?.message || '创建失败'
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
    await createMedicine({
      name: medicineForm.name.trim(),
      specification: medicineForm.specification.trim(),
      stock: medicineForm.stock ? Number(medicineForm.stock) : null,
      stockUnit: medicineForm.stockUnit || null,
      expireDate: (() => {
        const d = medicineForm.expireDate || ''
        if (!d) return null
        // 处理 YYYY-M 或 YYYY-MM 格式
        const match = d.match(/^(\d{4})-(\d{1,2})$/)
        if (match) {
          return `${match[1]}-${match[2].padStart(2,'0')}-01`
        }
        return d
      })(),
      usageNotes: medicineForm.usageNotes || null,
      indication: medicineForm.indication || null,
      imageUrl: null
    })
    showAddMedicine.value = false
    resetMedicineForm()
    load()
  } catch (err) {
    formError.value = err?.message || '添加失败'
  } finally {
    submitting.value = false
  }
}

const isToday = (dayStr) => {
  const today = new Date().getDay() || 7
  return dayStr?.includes(String(today))
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
          <button class="btn-link" @click="showAddMedicine = true">+ 添加药品</button>
        </div>
        <div v-if="medicineList.length > 0" class="medicine-grid">
          <div v-for="m in medicineList" :key="m.id" class="medicine-card">
            <div class="medicine-name">{{ m.name }}</div>
            <div class="medicine-spec">{{ m.specification || '-' }}</div>
            <div class="medicine-stock" :class="{ low: m.stock < 10 }">
              库存: {{ m.stock ?? 0 }}{{ m.stockUnit || '' }}
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
        <button class="btn-link" @click="showAddMedicine = true">+ 添加新药品</button>
      </div>
    </section>

    <div v-if="showCreatePlan" class="modal-mask" @click.self="showCreatePlan = false; resetPlanForm()">
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

    <div v-if="showAddMedicine" class="modal-mask" @click.self="showAddMedicine = false; resetMedicineForm()">
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
          <div v-if="ocrLoading" class="ocr-loading">识别中...</div>
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
          <input v-model="medicineForm.expireDate" class="input" placeholder="有效期(如2026-12)" />
          <textarea v-model="medicineForm.usageNotes" class="input textarea" placeholder="用法用量/注意事项" rows="2" />
        </div>

        <p v-if="formError" class="error">{{ formError }}</p>
        <div class="modal-actions">
          <button class="btn ghost" @click="showAddMedicine = false; resetMedicineForm()">取消</button>
          <button class="btn-primary" :disabled="submitting" @click="submitMedicine">
            {{ submitting ? '提交中...' : '确认添加' }}
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

.medicine-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 10px; }
.medicine-card { background: #f9fafb; border-radius: 10px; padding: 12px; }
.medicine-name { font-weight: 600; font-size: 0.9rem; }
.medicine-spec { font-size: 0.8rem; color: var(--muted); margin-top: 2px; }
.medicine-stock { font-size: 0.8rem; margin-top: 6px; }
.medicine-stock.low { color: #f59e0b; }
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
.modal { width: min(420px, 92vw); max-height: 90vh; overflow-y: auto; background: #fff; border-radius: 16px; padding: 24px; display: grid; gap: 16px; }
.modal h3 { font-size: 1.1rem; font-weight: 600; text-align: center; }

.ocr-area { border: 2px dashed #dcdfe6; border-radius: 12px; padding: 20px; text-align: center; cursor: pointer; }
.ocr-placeholder { color: var(--muted); font-size: 0.9rem; }
.preview-img img { max-height: 150px; border-radius: 8px; }
.preview-multiple { position: relative; }
.preview-multiple img { max-height: 120px; border-radius: 8px; }
.ocr-loading { position: absolute; inset: 0; background: rgba(255,255,255,0.8); display: grid; place-items: center; color: var(--primary); font-weight: 600; }
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

.modal-actions { display: flex; gap: 12px; justify-content: flex-end; margin-top: 8px; }
.btn-primary { background: var(--primary); color: #fff; padding: 10px 20px; border-radius: 8px; font-size: 0.95rem; }
.btn-primary:disabled { opacity: 0.6; }
.btn.ghost { background: transparent; border: 1px solid var(--primary); color: var(--primary); padding: 10px 20px; border-radius: 8px; }
.error { color: #dc2626; font-size: 0.85rem; }
.muted { color: var(--muted); }
</style>