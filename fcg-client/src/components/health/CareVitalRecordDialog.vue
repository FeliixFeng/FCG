<script setup>
import { computed, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { createVital } from '../../utils/api'

const props = defineProps({
  visible: Boolean,
  type: {
    type: Number,
    default: 1
  },
  userId: {
    type: Number,
    required: true
  }
})

const emit = defineEmits(['update:visible', 'success'])

const form = ref({
  valueSystolic: '',
  valueDiastolic: '',
  value: '',
  measurePoint: 1,
  notes: ''
})

const loading = ref(false)

const typeOptions = [
  { value: 1, label: '血压', unit: 'mmHg', icon: '❤️' },
  { value: 2, label: '血糖', unit: 'mmol/L', icon: '🩸' },
  { value: 3, label: '体重', unit: 'kg', icon: '⚖️' }
]

const currentType = computed(() => typeOptions.find(item => item.value === props.type) || typeOptions[0])
const isBloodPressure = computed(() => props.type === 1)
const isBloodSugar = computed(() => props.type === 2)

watch(
  () => props.visible,
  (val) => {
    if (!val) return
    form.value = {
      valueSystolic: '',
      valueDiastolic: '',
      value: '',
      measurePoint: 1,
      notes: ''
    }
  }
)

function closeDialog() {
  if (loading.value) return
  emit('update:visible', false)
}

function handleEnterSubmit(event) {
  event?.preventDefault?.()
  submitRecord()
}

function validateForm() {
  if (isBloodPressure.value) {
    if (!form.value.valueSystolic || !form.value.valueDiastolic) {
      ElMessage.warning('请填写完整血压数值')
      return false
    }
    return true
  }

  if (isBloodSugar.value) {
    if (!form.value.value) {
      ElMessage.warning('请填写血糖值')
      return false
    }
    if (!form.value.measurePoint) {
      ElMessage.warning('请选择空腹或餐后')
      return false
    }
    return true
  }

  if (!form.value.value) {
    ElMessage.warning(`请填写${currentType.value.label}数值`)
    return false
  }
  return true
}

async function submitRecord() {
  if (loading.value) return
  if (!validateForm()) return
  loading.value = true
  try {
    const payload = {
      userId: props.userId,
      type: props.type,
      measureTime: new Date().toISOString(),
      measurePoint: isBloodSugar.value ? form.value.measurePoint : null,
      ...(isBloodPressure.value
        ? {
            valueSystolic: Number(form.value.valueSystolic),
            valueDiastolic: Number(form.value.valueDiastolic),
            unit: 'mmHg'
          }
        : {
            value: Number(form.value.value),
            unit: currentType.value.unit
          }),
      notes: form.value.notes || null
    }
    await createVital(payload)
    ElMessage.success('录入成功')
    emit('update:visible', false)
    emit('success')
  } catch (err) {
    ElMessage.error(err?.message || '录入失败，请稍后重试')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <el-dialog
    :model-value="visible"
    @update:model-value="$emit('update:visible', $event)"
    :title="`${currentType.icon} 录入${currentType.label}`"
    width="min(560px, 94vw)"
    class="care-vital-dialog"
    :close-on-click-modal="false"
  >
    <div class="dialog-body">
      <template v-if="isBloodPressure">
        <div class="bp-grid">
          <div class="bp-item">
            <p class="field-label">收缩压（高压）</p>
            <div class="value-input-wrap">
              <input
                v-model="form.valueSystolic"
                class="value-input"
                type="number"
                inputmode="numeric"
                placeholder="120"
                @keydown.enter.prevent="handleEnterSubmit"
              />
              <span class="unit">mmHg</span>
            </div>
          </div>
          <div class="bp-item">
            <p class="field-label">舒张压（低压）</p>
            <div class="value-input-wrap">
              <input
                v-model="form.valueDiastolic"
                class="value-input"
                type="number"
                inputmode="numeric"
                placeholder="80"
                @keydown.enter.prevent="handleEnterSubmit"
              />
              <span class="unit">mmHg</span>
            </div>
          </div>
        </div>
      </template>

      <template v-else-if="isBloodSugar">
        <div class="sugar-point-row">
          <button
            class="point-btn"
            :class="{ active: form.measurePoint === 1 }"
            @click="form.measurePoint = 1"
            type="button"
          >
            空腹
          </button>
          <button
            class="point-btn"
            :class="{ active: form.measurePoint === 2 }"
            @click="form.measurePoint = 2"
            type="button"
          >
            餐后
          </button>
        </div>
        <div class="value-input-wrap">
          <input
            v-model="form.value"
            class="value-input"
            type="number"
            inputmode="decimal"
            step="0.1"
            :placeholder="form.measurePoint === 1 ? '建议 3.9 - 6.1' : '建议 < 7.8'"
            @keydown.enter.prevent="handleEnterSubmit"
          />
          <span class="unit">mmol/L</span>
        </div>
      </template>

      <template v-else>
        <div class="value-input-wrap">
          <input
            v-model="form.value"
            class="value-input"
            type="number"
            inputmode="decimal"
            step="0.1"
            :placeholder="`请输入${currentType.label}`"
            @keydown.enter.prevent="handleEnterSubmit"
          />
          <span class="unit">{{ currentType.unit }}</span>
        </div>
      </template>

      <el-input
        v-model="form.notes"
        class="note-input"
        type="textarea"
        :rows="2"
        placeholder="备注（可选）"
        @keydown.enter.prevent="handleEnterSubmit"
      />
    </div>

    <template #footer>
      <div class="dialog-footer">
        <button class="action-btn ghost" :disabled="loading" @click="closeDialog">取消</button>
        <button class="action-btn primary" :disabled="loading" @click="submitRecord">
          {{ loading ? '保存中...' : '确认录入' }}
        </button>
      </div>
    </template>
  </el-dialog>
</template>

<style scoped>
.dialog-body {
  display: grid;
  gap: 14px;
}

.bp-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.bp-item {
  border: 1px solid rgba(45, 95, 93, 0.2);
  border-radius: 12px;
  padding: 12px;
  background: rgba(246, 251, 250, 0.9);
}

.field-label {
  margin: 0 0 8px;
  font-size: 0.95rem;
  color: #466664;
  font-weight: 700;
}

.value-input-wrap {
  display: flex;
  align-items: center;
  gap: 8px;
  border: 1px solid rgba(45, 95, 93, 0.24);
  border-radius: 12px;
  background: #fff;
  padding: 0 10px;
}

.value-input {
  width: 100%;
  height: 52px;
  border: none;
  outline: none;
  font-size: 1.46rem;
  font-weight: 700;
  color: #214846;
  background: transparent;
  -moz-appearance: textfield;
}

.value-input::-webkit-outer-spin-button,
.value-input::-webkit-inner-spin-button {
  -webkit-appearance: none;
}

.value-input::placeholder {
  color: rgba(33, 72, 70, 0.33);
}

.unit {
  font-size: 0.9rem;
  color: #5f7a77;
  font-weight: 600;
  white-space: nowrap;
}

.sugar-point-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}

.point-btn {
  height: 48px;
  border: 1px solid rgba(45, 95, 93, 0.22);
  border-radius: 12px;
  background: #fff;
  color: #355b58;
  font-size: 1.02rem;
  font-weight: 700;
}

.point-btn.active {
  border-color: rgba(45, 95, 93, 0.4);
  background: linear-gradient(145deg, #f4faf8, #ecf5f2);
}

.dialog-footer {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}

.action-btn {
  height: 46px;
  border-radius: 12px;
  font-size: 1rem;
  font-weight: 700;
  border: 1px solid rgba(45, 95, 93, 0.26);
  cursor: pointer;
}

.action-btn.ghost {
  background: #fff;
  color: #496866;
}

.action-btn.primary {
  background: #2d5f5d;
  color: #fff;
  border-color: #2d5f5d;
}

.action-btn:disabled {
  opacity: 0.65;
  cursor: not-allowed;
}

.note-input :deep(.el-textarea__inner) {
  border-radius: 12px;
}

@media (max-width: 767px) {
  .bp-grid {
    grid-template-columns: 1fr;
  }

  .value-input {
    height: 50px;
    font-size: 1.34rem;
  }

  .dialog-footer {
    grid-template-columns: 1fr;
  }
}
</style>
