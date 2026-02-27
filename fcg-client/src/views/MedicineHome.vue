<script setup>
import BaseLayout from '../components/common/BaseLayout.vue'
import { onMounted, ref, reactive } from 'vue'
import { fetchMedicineList, createMedicine, deleteMedicine } from '../utils/api'

const list = ref([])
const total = ref(0)
const page = ref(1)
const loading = ref(false)
const error = ref('')

const showForm = ref(false)
const submitting = ref(false)
const formError = ref('')

const form = reactive({
  name: '',
  specification: '',
  manufacturer: '',
  dosageForm: '',
  stock: '',
  stockUnit: '',
  expireDate: '',
  storageLocation: '',
})

const resetForm = () => {
  Object.assign(form, {
    name: '', specification: '', manufacturer: '', dosageForm: '',
    stock: '', stockUnit: '', expireDate: '', storageLocation: '',
  })
  formError.value = ''
}

const load = async (p = 1) => {
  error.value = ''
  loading.value = true
  try {
    const res = await fetchMedicineList({ page: p, size: 20 })
    list.value = res.data.records
    total.value = res.data.total
    page.value = p
  } catch (err) {
    error.value = err?.message || '加载失败'
  } finally {
    loading.value = false
  }
}

const submit = async () => {
  if (!form.name.trim()) { formError.value = '药品名称不能为空'; return }
  formError.value = ''
  submitting.value = true
  try {
    await createMedicine({
      name: form.name.trim(),
      specification: form.specification || null,
      manufacturer: form.manufacturer || null,
      dosageForm: form.dosageForm || null,
      stock: form.stock ? Number(form.stock) : null,
      stockUnit: form.stockUnit || null,
      expireDate: form.expireDate || null,
      storageLocation: form.storageLocation || null,
    })
    showForm.value = false
    resetForm()
    load(1)
  } catch (err) {
    formError.value = err?.message || '添加失败'
  } finally {
    submitting.value = false
  }
}

const remove = async (id) => {
  if (!confirm('确认删除该药品？')) return
  try {
    await deleteMedicine(id)
    load(page.value)
  } catch (err) {
    alert(err?.message || '删除失败')
  }
}

const isExpiringSoon = (dateStr) => {
  if (!dateStr) return false
  return new Date(dateStr) - new Date() < 30 * 24 * 60 * 60 * 1000
}

onMounted(() => load())
</script>

<template>
  <BaseLayout>
    <section class="grid">
      <div class="card panel">
        <div class="panel-header">
          <h2>药品管理</h2>
          <button class="btn" @click="showForm = true">+ 添加药品</button>
        </div>

        <p v-if="error" class="error">{{ error }}</p>
        <p v-if="loading" class="muted">加载中...</p>

        <div v-if="!loading && list.length === 0 && !error" class="muted">暂无药品，点击右上角添加</div>

        <div v-if="list.length > 0" class="medicine-list">
          <div v-for="item in list" :key="item.id" class="medicine-item card">
            <div class="medicine-main">
              <div class="medicine-name">{{ item.name }}</div>
              <div class="medicine-meta muted">
                <span v-if="item.specification">{{ item.specification }}</span>
                <span v-if="item.dosageForm">{{ item.dosageForm }}</span>
                <span v-if="item.manufacturer">{{ item.manufacturer }}</span>
              </div>
            </div>
            <div class="medicine-right">
              <div class="medicine-stock">
                <span v-if="item.stock != null">库存 {{ item.stock }}{{ item.stockUnit || '' }}</span>
                <span v-if="item.expireDate" class="expire" :class="{ warn: isExpiringSoon(item.expireDate) }">
                  效期 {{ item.expireDate }}
                </span>
              </div>
              <button class="btn-del" @click="remove(item.id)">删除</button>
            </div>
          </div>
        </div>

        <div v-if="total > 20" class="pagination muted">共 {{ total }} 条</div>
      </div>
    </section>

    <!-- 添加药品弹窗 -->
    <div v-if="showForm" class="modal-mask" @click.self="showForm = false; resetForm()">
      <div class="modal card">
        <h3>添加药品</h3>
        <div class="form">
          <input v-model="form.name" class="input" placeholder="药品名称 *" />
          <input v-model="form.specification" class="input" placeholder="规格（如 500mg）" />
          <input v-model="form.manufacturer" class="input" placeholder="生产厂家" />
          <input v-model="form.dosageForm" class="input" placeholder="剂型（如 片剂）" />
          <div class="form-row">
            <input v-model="form.stock" class="input" type="number" placeholder="库存数量" />
            <input v-model="form.stockUnit" class="input" placeholder="单位（如 片）" />
          </div>
          <input v-model="form.expireDate" class="input" type="date" placeholder="有效期" />
          <input v-model="form.storageLocation" class="input" placeholder="存放位置" />
        </div>
        <p v-if="formError" class="error">{{ formError }}</p>
        <div class="modal-actions">
          <button class="btn ghost" @click="showForm = false; resetForm()">取消</button>
          <button class="btn" :disabled="submitting" @click="submit">
            {{ submitting ? '提交中...' : '确认添加' }}
          </button>
        </div>
      </div>
    </div>
  </BaseLayout>
</template>

<style scoped>
.grid { display: grid; gap: 20px; }
.panel { padding: 24px; display: grid; gap: 16px; }
.panel-header { display: flex; justify-content: space-between; align-items: center; }
.medicine-list { display: grid; gap: 10px; }
.medicine-item {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  padding: 14px 16px;
  gap: 12px;
}
.medicine-name { font-weight: 600; font-size: 1rem; }
.medicine-meta { font-size: 0.82rem; display: flex; gap: 8px; flex-wrap: wrap; margin-top: 4px; }
.medicine-right { display: flex; flex-direction: column; align-items: flex-end; gap: 8px; flex-shrink: 0; }
.medicine-stock { font-size: 0.85rem; color: var(--muted); display: flex; flex-direction: column; align-items: flex-end; gap: 2px; }
.expire.warn { color: #b42318; }
.btn-del { font-size: 0.8rem; padding: 4px 10px; background: transparent; border: 1px solid #e0d6c8; color: #b42318; border-radius: 8px; cursor: pointer; }
.btn-del:hover { background: #fff0ee; }
.pagination { font-size: 0.85rem; text-align: right; }
.btn.ghost { background: transparent; border: 1px solid var(--primary); color: var(--primary); }
.modal-mask { position: fixed; inset: 0; background: rgba(0,0,0,0.4); display: grid; place-items: center; z-index: 100; }
.modal { width: min(400px, 92vw); padding: 24px; display: grid; gap: 14px; }
.form { display: grid; gap: 10px; }
.form-row { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; }
.modal-actions { display: flex; gap: 12px; justify-content: flex-end; }
.error { color: #b42318; margin: 0; }
</style>
