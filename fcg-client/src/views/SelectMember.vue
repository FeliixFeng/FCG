<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'

const router = useRouter()
const userStore = useUserStore()
const members = ref([])
const loading = ref(false)
const error = ref('')
const switching = ref(null) // 正在切换的成员ID

// 角色标签
const roleLabel = (role) => {
  if (role === 0) return '管理员'
  if (role === 2) return '关怀'
  return ''
}

// 加载成员列表
const loadMembers = async () => {
  try {
    loading.value = true
    members.value = await userStore.fetchMembers()
  } catch (err) {
    error.value = err?.message || '获取成员列表失败'
  } finally {
    loading.value = false
  }
}

// 选择成员进入对应界面
const selectMember = async (member) => {
  try {
    switching.value = member.userId
    await userStore.switchMember(member.userId)
    // 关怀成员进入关怀界面（暂时都跳首页，后续区分）
    router.replace({ name: 'home' })
  } catch (err) {
    error.value = err?.message || '切换成员失败'
  } finally {
    switching.value = null
  }
}

// 退出登录
const logout = () => {
  userStore.logout()
  router.replace({ name: 'login' })
}

onMounted(() => {
  loadMembers()
})
</script>

<template>
  <div class="page">
    <div class="container">
      <!-- 顶部标题 -->
      <div class="top">
        <div class="family-name">{{ userStore.family?.familyName || '我的家庭' }}</div>
        <button class="btn-text" @click="logout">退出</button>
      </div>

      <p class="muted">请选择您的身份</p>

      <!-- 加载中 -->
      <div v-if="loading" class="muted center">加载中...</div>

      <!-- 成员列表 -->
      <div v-else-if="members.length > 0" class="member-grid">
        <button
          v-for="m in members"
          :key="m.userId"
          class="member-card"
          :disabled="switching === m.userId"
          @click="selectMember(m)"
        >
          <!-- 头像 -->
          <div class="avatar">
            <img v-if="m.avatar" :src="m.avatar" :alt="m.nickname" />
            <span v-else class="avatar-placeholder">{{ m.nickname?.charAt(0) }}</span>
          </div>
          <div class="member-name">{{ m.nickname }}</div>
          <div class="member-meta">
            <span v-if="m.relation" class="tag">{{ m.relation }}</span>
            <span v-if="roleLabel(m.role)" class="tag admin">{{ roleLabel(m.role) }}</span>
          </div>
          <div v-if="switching === m.userId" class="muted" style="font-size:0.8rem">进入中...</div>
        </button>
      </div>

      <!-- 没有成员 -->
      <div v-else class="empty">
        <p class="muted">还没有家庭成员</p>
      </div>

      <p v-if="error" class="error">{{ error }}</p>
    </div>
  </div>
</template>

<style scoped>
.page {
  min-height: 100vh;
  display: grid;
  place-items: center;
  padding: 24px;
}
.container {
  width: min(480px, 100%);
  display: grid;
  gap: 20px;
}
.top {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.family-name {
  font-size: 1.4rem;
  font-weight: 700;
}
.btn-text {
  background: none;
  border: none;
  color: var(--muted);
  cursor: pointer;
  font-size: 0.9rem;
  padding: 4px 8px;
}
.member-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(120px, 1fr));
  gap: 16px;
}
.member-card {
  background: var(--card-bg, #fff);
  border: 1px solid var(--border, #e5e5e5);
  border-radius: 16px;
  padding: 20px 12px;
  display: grid;
  place-items: center;
  gap: 8px;
  cursor: pointer;
  transition: box-shadow 0.15s;
}
.member-card:hover {
  box-shadow: 0 4px 16px rgba(0,0,0,0.08);
}
.avatar {
  width: 60px;
  height: 60px;
  border-radius: 50%;
  overflow: hidden;
  background: var(--primary, #4f7cff);
  display: grid;
  place-items: center;
}
.avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.avatar-placeholder {
  color: #fff;
  font-size: 1.4rem;
  font-weight: 700;
}
.member-name {
  font-weight: 600;
  font-size: 0.95rem;
}
.member-meta {
  display: flex;
  gap: 4px;
  flex-wrap: wrap;
  justify-content: center;
}
.tag {
  font-size: 0.75rem;
  padding: 2px 6px;
  border-radius: 6px;
  background: #f0f0f0;
  color: #666;
}
.tag.admin {
  background: #e8f0fe;
  color: #1a56db;
}
.empty {
  text-align: center;
  padding: 40px 0;
}
.error {
  color: #b42318;
  margin: 0;
}
.center {
  text-align: center;
}
</style>
