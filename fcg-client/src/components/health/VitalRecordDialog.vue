<script setup>
import { ref, computed, watch } from 'vue'
import { createVital } from '../../utils/api'
import { ElMessage } from 'element-plus'

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
  measurePoint: null,
  notes: ''
})

const loading = ref(false)

const typeOptions = [
  { value: 1, label: '血压', unit: 'mmHg' },
  { value: 2, label: '血糖', unit: 'mmol/L' },
  { value: 3, label: '体重', unit: 'kg' }
]

const currentType = computed(() => typeOptions.find(t => t.value === props.type))
const isBloodPressure = computed(() => props.type === 1)
const isBloodSugar = computed(() => props.type === 2)

watch(() => props.visible, (val) => {
  if (val) {
    form.value = { valueSystolic: '', valueDiastolic: '', value: '', measurePoint: null, notes: '' }
  }
})

const handleSubmit = async () => {
  if (loading.value) return

  if (isBloodPressure.value) {
    if (!form.value.valueSystolic || !form.value.valueDiastolic) {
      ElMessage.warning('请填写收缩压和舒张压')
      return
    }
  } else if (isBloodSugar.value) {
    if (!form.value.value) {
      ElMessage.warning('请填写血糖值')
      return
    }
    if (!form.value.measurePoint) {
      ElMessage.warning('请选择测量时点（空腹/餐后）')
      return
    }
  } else {
    if (!form.value.value) {
      ElMessage.warning(`请填写${currentType.value.label}`)
      return
    }
  }

  loading.value = true
  try {
    const data = {
      userId: props.userId,
      type: props.type,
      measureTime: new Date().toISOString(),
      measurePoint: isBloodSugar.value ? form.value.measurePoint : null,
      ...(isBloodPressure.value ? {
        valueSystolic: Number(form.value.valueSystolic),
        valueDiastolic: Number(form.value.valueDiastolic),
        unit: 'mmHg'
      } : {
        value: Number(form.value.value),
        unit: currentType.value.unit
      }),
      notes: form.value.notes || null
    }
    await createVital(data)
    ElMessage.success('记录成功')
    emit('update:visible', false)
    emit('success')
  } catch (e) {
    ElMessage.error(e.message || '保存失败')
  } finally {
    loading.value = false
  }
}

const handleInputEnter = (event) => {
  event?.preventDefault?.()
  handleSubmit()
}
</script>

<template>
  <el-dialog
    :model-value="visible"
    @update:model-value="$emit('update:visible', $event)"
    :title="`记录${currentType?.label}`"
    width="min(400px, 90vw)"
    :close-on-click-modal="false"
    @keyup.enter="handleSubmit"
  >
    <el-form label-position="top">
      <el-form-item v-if="isBloodPressure" label="血压 (mmHg)">
        <div class="blood-pressure-input">
          <div class="bp-field">
            <span class="bp-label">收缩压（高压）</span>
            <input
              v-model="form.valueSystolic"
              class="num-input"
              type="number"
              inputmode="numeric"
              placeholder="120"
              min="60"
              max="250"
              @keydown.enter.prevent="handleInputEnter"
            />
          </div>
          <span class="separator">/</span>
          <div class="bp-field">
            <span class="bp-label">舒张压（低压）</span>
            <input
              v-model="form.valueDiastolic"
              class="num-input"
              type="number"
              inputmode="numeric"
              placeholder="80"
              min="40"
              max="150"
              @keydown.enter.prevent="handleInputEnter"
            />
          </div>
        </div>
      </el-form-item>

      <el-form-item v-else-if="isBloodSugar" :label="currentType?.label">
        <div class="blood-sugar-form">
          <el-radio-group v-model="form.measurePoint" class="measure-point-group">
            <el-radio :value="1">空腹</el-radio>
            <el-radio :value="2">餐后</el-radio>
          </el-radio-group>
          <div class="single-input">
            <input
              v-model="form.value"
              class="num-input"
              type="number"
              inputmode="decimal"
              :placeholder="form.measurePoint === 1 ? '正常 3.9~6.1' : form.measurePoint === 2 ? '正常 < 7.8' : ''"
              min="0"
              max="100"
              step="0.1"
              @keydown.enter.prevent="handleInputEnter"
            />
            <span class="unit">{{ currentType?.unit }}</span>
          </div>
        </div>
      </el-form-item>

      <el-form-item v-else :label="currentType?.label">
        <div class="single-input">
          <input
            v-model="form.value"
            class="num-input"
            type="number"
            inputmode="decimal"
            min="0"
            max="300"
            step="0.1"
            @keydown.enter.prevent="handleInputEnter"
          />
          <span class="unit">{{ currentType?.unit }}</span>
        </div>
      </el-form-item>

      <el-form-item label="备注（可选）">
        <el-input
          v-model="form.notes"
          type="textarea"
          :rows="2"
          placeholder="可填写测量时状态，如：饭后、睡前、运动后"
        />
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="$emit('update:visible', false)">取消</el-button>
      <el-button type="primary" :loading="loading" @click="handleSubmit">保存</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.blood-pressure-input {
  display: flex;
  align-items: flex-end;
  gap: 12px;
  width: 100%;
}
.bp-field {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.bp-label {
  font-size: 12px;
  color: #999;
}
.separator {
  font-size: 20px;
  color: #bbb;
  padding-bottom: 6px;
  flex-shrink: 0;
}
.num-input {
  width: 100%;
  height: 36px;
  padding: 0 12px;
  border: 1px solid #dcdfe6;
  border-radius: 6px;
  font-size: 16px;
  color: #333;
  box-sizing: border-box;
  outline: none;
  transition: border-color 0.2s;
  -moz-appearance: textfield;
}
.num-input::-webkit-outer-spin-button,
.num-input::-webkit-inner-spin-button {
  -webkit-appearance: none;
}
.num-input:focus {
  border-color: #2d5f5d;
}
.num-input::placeholder {
  color: #ccc;
}
.unit {
  color: #666;
  font-size: 14px;
  margin-left: 8px;
  white-space: nowrap;
  flex-shrink: 0;
}
.single-input {
  display: flex;
  align-items: center;
  width: 100%;
}
.single-input .num-input {
  flex: 1;
}
.blood-sugar-form {
  display: flex;
  flex-direction: column;
  gap: 8px;
  width: 100%;
}
.measure-point-group {
  margin-bottom: 0;
}
</style>
