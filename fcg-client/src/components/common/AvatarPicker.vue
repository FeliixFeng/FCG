<script setup>
import { ref, computed, watch } from 'vue'
import { compressImage } from '../../utils/image'
import { uploadFile } from '../../utils/api'
import { ElMessage } from 'element-plus'

const props = defineProps({
  modelValue: { type: String, default: '' },
  currentRelation: { type: String, default: '' },
  currentRole: { type: Number, default: 1 }
})

const emit = defineEmits(['update:modelValue'])

// 内置头像列表
const builtInAvatars = [
  { type: 'elder-male', img: '/avatars/grandpa.png', label: '爷爷' },
  { type: 'elder-female', img: '/avatars/grandma.png', label: '奶奶' },
  { type: 'adult-male', img: '/avatars/father.png', label: '爸爸' },
  { type: 'adult-female', img: '/avatars/mother.png', label: '妈妈' },
  { type: 'child-male-elder', img: '/avatars/elderbrother.png', label: '哥哥' },
  { type: 'child-male-young', img: '/avatars/youngerbrother.png', label: '弟弟' },
  { type: 'child-female-elder', img: '/avatars/eldersister.png', label: '姐姐' },
  { type: 'child-female-young', img: '/avatars/youngersister.png', label: '妹妹' }
]

// 根据关系/角色获取默认头像
const defaultAvatar = computed(() => {
  const rel = props.currentRelation || ''
  const role = props.currentRole

  const map = [
    { keys: ['爷爷', '外公', '祖父', '姥爷'], img: '/avatars/grandpa.png' },
    { keys: ['奶奶', '外婆', '祖母', '姥姥'], img: '/avatars/grandma.png' },
    { keys: ['爸爸', '父亲'], img: '/avatars/father.png' },
    { keys: ['妈妈', '母亲'], img: '/avatars/mother.png' },
    { keys: ['哥哥', '儿子'], img: '/avatars/elderbrother.png' },
    { keys: ['弟弟'], img: '/avatars/youngerbrother.png' },
    { keys: ['姐姐', '女儿'], img: '/avatars/eldersister.png' },
    { keys: ['妹妹'], img: '/avatars/youngersister.png' },
  ]

  for (const m of map) {
    if (m.keys.some(k => rel.includes(k))) return m.img
  }

  // 角色兜底
  if (role === 0) return '/avatars/father.png'
  if (role === 2) return '/avatars/grandpa.png'
  return '/avatars/elderbrother.png'
})

const selectedAvatar = ref(props.modelValue || defaultAvatar.value)
const uploading = ref(false)

watch(() => props.modelValue, (v) => {
  selectedAvatar.value = v || defaultAvatar.value
})

// 选择内置头像
const selectBuiltIn = (img) => {
  selectedAvatar.value = img
  emit('update:modelValue', img)
}

// 处理文件上传
const handleFileChange = async (e) => {
  const file = e.target.files?.[0]
  if (!file) return

  if (!file.type.startsWith('image/')) {
    ElMessage.warning('请选择图片文件')
    return
  }

  try {
    uploading.value = true
    const compressed = await compressImage(file, 200, 0.8)
    console.log('压缩后:', compressed, compressed.size)
    
    const res = await uploadFile(compressed, 'avatar')
    console.log('上传结果:', res)
    
    if (!res.data) {
      throw new Error('上传返回为空')
    }
    
    const url = res.data
    selectedAvatar.value = url
    emit('update:modelValue', url)
    ElMessage.success('上传成功')
  } catch (err) {
    console.error('上传失败:', err)
    ElMessage.error('上传失败: ' + (err.message || '未知错误'))
  } finally {
    uploading.value = false
  }
}

// 清除选择
const clearAvatar = () => {
  selectedAvatar.value = defaultAvatar.value
  emit('update:modelValue', defaultAvatar.value)
}
</script>

<template>
  <div class="avatar-picker">
    <div class="avatar-preview">
      <img :src="selectedAvatar" alt="头像预览" />
    </div>

    <div class="avatar-section">
      <div class="avatar-grid">
        <button
          v-for="avatar in builtInAvatars"
          :key="avatar.type"
          class="avatar-option"
          :class="{ active: selectedAvatar === avatar.img }"
          @click="selectBuiltIn(avatar.img)"
          :title="avatar.label"
        >
          <img :src="avatar.img" :alt="avatar.label" />
        </button>
      </div>
    </div>

    <div class="avatar-actions">
      <label class="upload-btn">
        <input type="file" accept="image/*" @change="handleFileChange" hidden />
        <span v-if="uploading">上传中...</span>
        <span v-else>上传图片</span>
      </label>
      <button class="reset-btn" @click="clearAvatar">重置</button>
    </div>
  </div>
</template>

<style scoped>
.avatar-picker {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.avatar-preview {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  overflow: hidden;
  margin: 0 auto;
  border: 2px solid #e0e0e0;
}

.avatar-preview img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-section {
  padding-top: 8px;
}

.avatar-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 6px;
}

.avatar-option {
  width: 100%;
  aspect-ratio: 1;
  border: 2px solid #eee;
  border-radius: 6px;
  overflow: hidden;
  cursor: pointer;
  padding: 0;
  background: #fff;
  transition: border-color 0.2s;
}

.avatar-option:hover {
  border-color: #ccc;
}

.avatar-option.active {
  border-color: #2d5f5d;
}

.avatar-option img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-actions {
  display: flex;
  justify-content: center;
  gap: 12px;
  padding-top: 4px;
}

.upload-btn {
  display: inline-block;
  padding: 6px 12px;
  background: #2d5f5d;
  color: white;
  border-radius: 4px;
  cursor: pointer;
  font-size: 0.8rem;
}

.upload-btn:hover {
  background: #234947;
}

.reset-btn {
  padding: 6px 12px;
  background: #f5f5f5;
  color: #666;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 0.8rem;
}

.reset-btn:hover {
  background: #eee;
}
</style>