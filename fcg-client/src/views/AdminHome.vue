<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import BaseLayout from '../components/common/BaseLayout.vue'
import { fetchFamilyMembers, addMember } from '../utils/api'

const router = useRouter()
const userStore = useUserStore()
const members = ref([])
const error = ref('')
const showAddForm = ref(false)
const loading = ref(false)

const form = ref({
  nickname: '',
  relation: '',
  role: 1,
  phone: ''
})

const roleOptions = [
  { value: 0, label: '管理员' },
  { value: 1, label: '普通成员' },
  { value: 2, label: '关怀成员（老人）' }
]

const loadMembers = async () => {
  try {
    const res = await fetchFamilyMembers()
    members.value = res.data
  } catch (err) {
    error.value = err?.message || '获取成员失败'
  }
}

const handleAddMember = async () => {
  if (!form.value.nickname) {
    error.value = '昵称不能为空'
    return
  }
  try {
    loading.value = true
    error.value = ''
    await addMember({ ...form.value })
    showAddForm.value = false
    form.value = { nickname: '', relation: '', role: 1, phone: '' }
    await loadMembers()
  } catch (err) {
    error.value = err?.message || '添加失败'
  } finally {
    loading.value = false
  }
}

// 退出管理员界面，回到普通界面
const exitAdmin = () => {
  router.replace({ name: 'home' })
}

onMounted(() => {
  loadMembers()
})
</script>

<template>
  <BaseLayout>
    <section class="grid">
      <!-- 顶部 -->
      <div class="card panel">
        <div class="row">
          <h2>管理员界面</h2>
          <button class="btn ghost" @click="exitAdmin">退出管理</button>
        </div>
        <p class="muted">家庭：{{ userStore.family?.familyName }}</p>
      </div>

      <!-- 成员管理 -->
      <div class="card panel">
        <div class="row">
          <h2>家庭成员</h2>
          <button class="btn" @click="showAddForm = !showAddForm">
            {{ showAddForm ? '取消' : '+ 添加成员' }}
          </button>
        </div>

        <!-- 添加成员表单 -->
        <div v-if="showAddForm" class="add-form">
          <label class="field">
            <span>昵称 *</span>
            <input v-model="form.nickname" class="input" placeholder="例如：爷爷、妈妈" />
          </label>
          <label class="field">
            <span>家庭关系</span>
            <input v-model="form.relation" class="input" placeholder="例如：父亲、母亲" />
          </label>
          <label class="field">
            <span>手机号</span>
            <input v-model="form.phone" class="input" placeholder="选填" />
          </label>
          <label class="field">
            <span>角色</span>
            <select v-model="form.role" class="input">
              <option v-for="opt in roleOptions" :key="opt.value" :value="opt.value">
                {{ opt.label }}
              </option>
            </select>
          </label>
          <button class="btn" :disabled="loading" @click="handleAddMember">
            {{ loading ? '添加中...' : '确认添加' }}
          </button>
        </div>

        <p v-if="error" class="error">{{ error }}</p>

        <!-- 成员列表 -->
        <div v-if="members.length === 0" class="muted">暂无成员</div>
        <div v-else class="member-list">
          <div v-for="m in members" :key="m.userId" class="member-item">
            <div class="avatar-sm">{{ m.nickname?.charAt(0) }}</div>
            <div>
              <div class="member-name">{{ m.nickname }}</div>
              <div class="muted">{{ m.relation || '未设置关系' }} · {{ ['管理员','普通成员','关怀成员'][m.role] }}</div>
            </div>
          </div>
        </div>
      </div>

      <!-- 更多管理功能（后续扩展） -->
      <div class="card panel">
        <h2>更多功能</h2>
        <p class="muted">药品管理、健康数据查看等功能后续开发中...</p>
      </div>
    </section>
  </BaseLayout>
</template>

<style scoped>
.grid { display: grid; gap: 20px; }
.panel { padding: 24px; display: grid; gap: 16px; }
.row { display: flex; justify-content: space-between; align-items: center; }
.add-form { display: grid; gap: 12px; padding: 16px; background: #f9f9f9; border-radius: 12px; }
.field { display: grid; gap: 6px; font-size: 0.9rem; }
.member-list { display: grid; gap: 12px; }
.member-item { display: flex; align-items: center; gap: 12px; }
.avatar-sm {
  width: 40px; height: 40px; border-radius: 50%;
  background: var(--primary, #4f7cff); color: #fff;
  display: grid; place-items: center; font-weight: 700; flex-shrink: 0;
}
.member-name { font-weight: 600; }
.btn.ghost { background: transparent; border: 1px solid var(--primary); color: var(--primary); }
.error { color: #b42318; margin: 0; }
</style>
