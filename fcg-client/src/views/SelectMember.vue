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
    router.replace({ name: 'dashboard' })
  } catch (err) {
    error.value = err?.message || '切换成员失败'
  } finally {
    switching.value = null
  }
}

// 退出登录
const logout = () => {
  userStore.logout()
  router.replace({ name: 'landing' })
}

onMounted(() => {
  loadMembers()
})
</script>

<template>
  <div class="welcome-shell">
    <div class="bg-layer bg-layer-a"></div>
    <div class="bg-layer bg-layer-b"></div>
    <div class="bg-layer bg-layer-c"></div>
    <div class="bg-layer bg-layer-d"></div>

    <div class="panel">
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
.welcome-shell {
  position: relative;
  min-height: 100vh;
  overflow: hidden;
  padding: clamp(16px, 3.6vw, 40px);
  display: grid;
  place-items: center;
  background:
    radial-gradient(124% 92% at 8% 4%, rgba(255, 255, 255, 0.84) 0%, transparent 55%),
    radial-gradient(98% 76% at 100% 8%, rgba(84, 127, 124, 0.22) 0%, transparent 60%),
    radial-gradient(88% 74% at 0% 100%, rgba(246, 228, 201, 0.68) 0%, transparent 64%),
    linear-gradient(134deg, #fbf8f1 0%, #f2e9d8 56%, #e6ede9 100%);
}

.welcome-shell::before {
  content: '';
  position: absolute;
  inset: 0;
  background-image:
    repeating-linear-gradient(135deg, rgba(69, 99, 97, 0.16) 0 1px, transparent 1px 30px),
    repeating-linear-gradient(45deg, rgba(255, 255, 255, 0.32) 0 1px, transparent 1px 30px);
  opacity: 0.56;
  -webkit-mask-image: radial-gradient(120% 94% at 50% 56%, #000 0%, rgba(0, 0, 0, 0.58) 66%, transparent 100%);
  mask-image: radial-gradient(120% 94% at 50% 56%, #000 0%, rgba(0, 0, 0, 0.58) 66%, transparent 100%);
  pointer-events: none;
}

.welcome-shell::after {
  content: '';
  position: absolute;
  inset: -8% -10% -12% -10%;
  background:
    linear-gradient(118deg, rgba(45, 95, 93, 0.16) 0%, rgba(45, 95, 93, 0) 34%),
    linear-gradient(304deg, rgba(45, 95, 93, 0.12) 0%, rgba(45, 95, 93, 0) 32%),
    linear-gradient(146deg, rgba(255, 255, 255, 0.36) 0%, rgba(255, 255, 255, 0) 32%),
    linear-gradient(338deg, rgba(238, 224, 198, 0.34) 0%, rgba(238, 224, 198, 0) 30%);
  opacity: 0.92;
  pointer-events: none;
}

.bg-layer {
  position: absolute;
  z-index: 0;
  pointer-events: none;
}

.bg-layer-a {
  width: min(44vw, 560px);
  height: min(30vw, 360px);
  left: -12%;
  top: -8%;
  clip-path: polygon(0 18%, 76% 0, 100% 44%, 60% 100%, 0 80%);
  background: linear-gradient(138deg, rgba(229, 243, 241, 0.72) 0%, rgba(77, 124, 121, 0.22) 100%);
  border: 1px solid rgba(255, 255, 255, 0.28);
  opacity: 0.88;
}

.bg-layer-b {
  width: min(48vw, 620px);
  height: min(36vw, 430px);
  right: -16%;
  bottom: -18%;
  clip-path: polygon(8% 0, 100% 24%, 92% 100%, 0 74%);
  background: linear-gradient(142deg, rgba(45, 95, 93, 0.44) 0%, rgba(45, 95, 93, 0.14) 100%);
  opacity: 0.62;
}

.bg-layer-c {
  width: min(24vw, 300px);
  height: min(20vw, 220px);
  right: 10%;
  top: 12%;
  clip-path: polygon(0 24%, 80% 0, 100% 66%, 18% 100%);
  background: linear-gradient(130deg, rgba(250, 244, 234, 0.9) 0%, rgba(45, 95, 93, 0.24) 100%);
  opacity: 0.68;
}

.bg-layer-d {
  width: min(30vw, 380px);
  height: min(22vw, 260px);
  left: -8%;
  bottom: -12%;
  clip-path: polygon(0 32%, 88% 0, 100% 86%, 12% 100%);
  background: linear-gradient(132deg, rgba(232, 202, 161, 0.44) 0%, rgba(255, 255, 255, 0.1) 100%);
  opacity: 0.58;
}

.panel {
  position: relative;
  z-index: 1;
  width: min(520px, 100%);
  display: grid;
  gap: 20px;
  padding: 26px;
  border-radius: 24px;
  border: 1px solid rgba(255, 255, 255, 0.66);
  background: linear-gradient(170deg, rgba(255, 255, 255, 0.86) 0%, rgba(255, 255, 255, 0.76) 100%);
  backdrop-filter: blur(12px);
  box-shadow: 0 20px 50px rgba(26, 38, 37, 0.12);
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
  background: rgba(255, 255, 255, 0.9);
  border: 1px solid rgba(226, 216, 200, 0.9);
  border-radius: 16px;
  padding: 20px 12px;
  display: grid;
  place-items: center;
  gap: 8px;
  cursor: pointer;
  transition: box-shadow 0.15s;
}
.member-card:hover {
  box-shadow: 0 8px 24px rgba(0,0,0,0.12);
}
.avatar {
  width: 60px;
  height: 60px;
  border-radius: 50%;
  overflow: hidden;
  background: #2d5f5d;
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
  background: rgba(45, 95, 93, 0.1);
  color: #436361;
}
.tag.admin {
  background: rgba(45, 95, 93, 0.16);
  color: #2d5f5d;
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

@media (max-width: 760px) {
  .panel {
    padding: 22px 18px;
    border-radius: 20px;
  }
}
</style>
