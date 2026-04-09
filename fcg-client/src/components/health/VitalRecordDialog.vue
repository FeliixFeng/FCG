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
  valueSystolic: null,
  valueDiastolic: null,
  value: null,
  measurePoint: null,
  notes: ''
})

const loading = ref(false)

const typeOptions = [
  { value: 1, label: '血压', unit: 'mmHg' },
  { value: 2, label: '血糖', unit: 'mmol/L' },
  { value: 3, label: '体重', unit: 'kg' }
]

const measurePointOptions = [
  { value: 1, label: '空腹' },
  { value: 2, label: '餐后' }
]

const currentType = computed(() => typeOptions.find(t => t.value === props.type))

const isBloodPressure = computed(() => props.type === 1)
const isBloodSugar = computed(() => props.type === 2)

watch(() => props.visible, (val) => {
  if (val) {
    form.value = {
      valueSystolic: null,
      valueDiastolic: null,
      value: null,
      measurePoint: null,
      notes: ''
    }
  }
})

const handleSubmit = async () => {
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
        valueSystolic: form.value.valueSystolic,
        valueDiastolic: form.value.valueDiastolic,
        unit: 'mmHg'
      } : {
        value: form.value.value,
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
</script>

<template>
  <el-dialog
    :model-value="visible"
    @update:model-value="$emit('update:visible', $event)"
    :title="`记录${currentType?.label}`"
    width="400px"
    :close-on-click-modal="false"
  >
    <el-form label-position="top">
      <el-form-item :label="currentType?.label">
        <div v-if="isBloodPressure" class="blood-pressure-input">
          <el-input-number
            v-model="form.valueSystolic"
            :min="60"
            :max="250"
            :precision="0"
            placeholder="收缩压"
            controls-position="right"
          />
          <span class="separator">/</span>
          <el-input-number
            v-model="form.valueDiastolic"
            :min="40"
            :max="150"
            :precision="0"
            placeholder="舒张压"
            controls-position="right"
          />
          <span class="unit">{{ currentType?.unit }}</span>
        </div>
        <div v-else class="single-input">
          <el-input-number
            v-model="form.value"
            :min="0"
            :max="props.type === 3 ? 300 : 100"
            :precision="1"
            :step="props.type === 3 ? 1 : 0.1"
            controls-position="right"
          />
          <span class="unit">{{ currentType?.unit }}</span>
        </div>
      </el-form-item>

      <el-form-item v-if="isBloodSugar" label="测量时点">
        <el-radio-group v-model="form.measurePoint">
          <el-radio :value="1">空腹</el-radio>
          <el-radio :value="2">餐后</el-radio>
        </el-radio-group>
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
      <el-button type="primary" :loading="loading" @click="handleSubmit">
        保存
      </el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.blood-pressure-input {
  display: flex;
  align-items: center;
  gap: 8px;
}
.blood-pressure-input :deep(.el-input-number) {
  width: 120px;
}
.separator {
  font-size: 18px;
  color: #999;
}
.unit {
  color: #666;
  font-size: 14px;
  margin-left: 8px;
}
.single-input {
  display: flex;
  align-items: center;
}
.single-input :deep(.el-input-number) {
  width: 180px;
}
</style>