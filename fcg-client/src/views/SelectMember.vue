<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import AvatarIcon from '../components/common/AvatarIcon.vue'

const router = useRouter()
const userStore = useUserStore()
const members = ref([])
const loading = ref(false)
const error = ref('')
const switching = ref(null)

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

// 选择成员
const selectMember = async (member) => {
  if (switching.value) return
  try {
    switching.value = member.userId
    await userStore.switchMember(member.userId)
    router.replace({ name: 'dashboard' })
  } catch (err) {
    error.value = err?.message || '切换成员失败'
    switching.value = null
  }
}

// 退出登录
const logout = () => {
  userStore.logout()
  router.replace({ name: 'landing' })
}

// 角色/关系标签
function roleTag(member) {
  if (member.role === 0) return { label: '管理员', type: 'admin' }
  if (member.role === 2) return { label: '关怀', type: 'care' }
  return null
}

onMounted(() => loadMembers())
</script>

<template>
  <div class="shell">
    <!-- 多层装饰背景 -->
    <div class="bg-layer bg-a" aria-hidden="true"></div>
    <div class="bg-layer bg-b" aria-hidden="true"></div>
    <div class="bg-layer bg-c" aria-hidden="true"></div>
    <div class="bg-layer bg-d" aria-hidden="true"></div>

    <div class="panel">

      <!-- ── 顶部品牌栏 ── -->
      <header class="panel-header">
        <div class="brand">
          <img src="/fcg.png" alt="FCG" class="brand-logo" />
          <div>
            <div class="brand-name">{{ userStore.family?.familyName || '我的家庭' }}</div>
            <div class="brand-sub">Family Care Guardian</div>
          </div>
        </div>
        <button class="btn-logout" @click="logout">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round">
            <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/>
            <polyline points="16 17 21 12 16 7"/>
            <line x1="21" y1="12" x2="9" y2="12"/>
          </svg>
          退出
        </button>
      </header>

      <!-- ── 引导语 ── -->
      <div class="greeting">
        <h2>你好，请选择成员</h2>
        <p>选择你的身份，进入对应的健康管理空间</p>
      </div>

      <!-- ── 加载骨架 ── -->
      <div v-if="loading" class="member-grid">
        <div v-for="i in 3" :key="i" class="member-card skeleton">
          <div class="sk-avatar"></div>
          <div class="sk-line sk-name"></div>
          <div class="sk-line sk-tag"></div>
        </div>
      </div>

      <!-- ── 成员列表 ── -->
      <div v-else-if="members.length > 0" class="member-grid">
        <button
          v-for="m in members"
          :key="m.userId"
          class="member-card"
          :class="{
            'is-care': m.role === 2,
            'is-admin': m.role === 0,
            'is-switching': switching === m.userId,
          }"
          :disabled="!!switching"
          @click="selectMember(m)"
        >
          <!-- 头像 -->
          <div class="avatar-ring" :class="{ 'ring-admin': m.role === 0, 'ring-care': m.role === 2 }">
            <AvatarIcon
              :avatar="m.avatar"
              :relation="m.relation"
              :role="m.role"
              :nickname="m.nickname"
              :size="68"
            />
          </div>

          <!-- 昵称 -->
          <div class="member-name">{{ m.nickname }}</div>

          <!-- 关系 + 角色 tag -->
          <div class="member-tags">
            <span v-if="m.relation" class="tag tag-relation">{{ m.relation }}</span>
            <span v-if="roleTag(m)" class="tag" :class="'tag-' + roleTag(m).type">
              {{ roleTag(m).label }}
            </span>
          </div>

          <!-- 切换中遮罩 -->
          <transition name="fade">
            <div v-if="switching === m.userId" class="switching-mask">
              <svg class="spin-icon" width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
                <path d="M12 2v4M12 18v4M4.93 4.93l2.83 2.83M16.24 16.24l2.83 2.83M2 12h4M18 12h4M4.93 19.07l2.83-2.83M16.24 7.76l2.83-2.83"/>
              </svg>
            </div>
          </transition>
        </button>
      </div>

      <!-- ── 空状态 ── -->
      <div v-else class="empty-state">
        <div class="empty-icon">👨‍👩‍👧</div>
        <div class="empty-title">还没有家庭成员</div>
        <div class="empty-desc">请联系管理员添加成员后再进入</div>
      </div>

      <!-- 错误提示 -->
      <p v-if="error" class="error-tip">{{ error }}</p>

    </div>
  </div>
</template>

<style scoped>
/* ── 全屏背景 ── */
.shell {
  position: relative;
  min-height: 100vh;
  overflow: hidden;
  padding: clamp(16px, 4vw, 48px) clamp(12px, 3vw, 24px);
  display: grid;
  place-items: center;
  /* 暖米绿亮色底色 */
  background: linear-gradient(145deg, #eef5f4 0%, #f0ebe0 45%, #e8f2f0 100%);
}

/* ── 大模糊装饰圆（无纹理，靠形状撑空间） ── */
.shell::before {
  content: '';
  position: absolute;
  width: min(70vw, 640px);
  height: min(70vw, 640px);
  top: -25%;
  right: -20%;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(45,95,93,0.13) 0%, transparent 70%);
  pointer-events: none;
}

/* ── 品牌色光晕叠加 ── */
.shell::after {
  content: '';
  position: absolute;
  inset: 0;
  background:
    radial-gradient(ellipse 55% 45% at 8%  10%,  rgba(45,95,93,0.12)   0%, transparent 65%),
    radial-gradient(ellipse 50% 40% at 92% 85%,  rgba(45,95,93,0.10)   0%, transparent 60%),
    radial-gradient(ellipse 40% 35% at 88% 12%,  rgba(246,228,201,0.40) 0%, transparent 55%),
    radial-gradient(ellipse 45% 40% at 5%  90%,  rgba(246,228,201,0.30) 0%, transparent 55%);
  pointer-events: none;
}

/* ── 装饰 bg-layer ── */
.bg-layer { position: absolute; z-index: 0; pointer-events: none; }

.bg-a {
  width: min(44vw, 560px); height: min(30vw, 360px);
  left: -12%; top: -8%;
  clip-path: polygon(0 18%, 76% 0, 100% 44%, 60% 100%, 0 80%);
  background: linear-gradient(138deg, rgba(45,95,93,0.09) 0%, rgba(255,255,255,0.12) 100%);
  border: 1px solid rgba(45,95,93,0.12);
  opacity: 0.8;
}
.bg-b {
  width: min(48vw, 620px); height: min(36vw, 430px);
  right: -16%; bottom: -18%;
  clip-path: polygon(8% 0, 100% 24%, 92% 100%, 0 74%);
  background: linear-gradient(142deg, rgba(45,95,93,0.10) 0%, rgba(246,228,201,0.20) 100%);
  opacity: 0.62;
}
.bg-c {
  width: min(24vw, 300px); height: min(20vw, 220px);
  right: 10%; top: 12%;
  clip-path: polygon(0 24%, 80% 0, 100% 66%, 18% 100%);
  background: linear-gradient(130deg, rgba(246,228,201,0.50) 0%, rgba(45,95,93,0.08) 100%);
  opacity: 0.7;
}
.bg-d {
  width: min(30vw, 380px); height: min(22vw, 260px);
  left: -8%; bottom: -12%;
  clip-path: polygon(0 32%, 88% 0, 100% 86%, 12% 100%);
  background: linear-gradient(132deg, rgba(45,95,93,0.08) 0%, rgba(246,228,201,0.35) 100%);
  opacity: 0.7;
}

/* ── 主面板 ── */
.panel {
  position: relative;
  z-index: 1;
  width: min(720px, 100%);
  display: grid;
  gap: 24px;
  padding: clamp(20px, 4vw, 32px);
  border-radius: 24px;
  border: 1px solid rgba(255,255,255,0.80);
  background: linear-gradient(170deg, rgba(255,255,255,0.96) 0%, rgba(255,255,255,0.90) 100%);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  box-shadow: 0 20px 60px rgba(45,95,93,0.14), 0 2px 8px rgba(45,95,93,0.08), 0 0 0 1px rgba(255,255,255,0.7);
}

/* ── 顶部品牌栏 ── */
.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.brand {
  display: flex;
  align-items: center;
  gap: 10px;
}

.brand-logo {
  width: 36px;
  height: 36px;
  border-radius: 9px;
  flex-shrink: 0;
}

.brand-name {
  font-size: 1rem;
  font-weight: 700;
  color: #0f0f0f;
  line-height: 1.3;
}

.brand-sub {
  font-size: 0.72rem;
  color: #aaa;
  margin-top: 1px;
}

.btn-logout {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 6px 12px;
  border: 1px solid rgba(0,0,0,0.1);
  background: rgba(255,255,255,0.7);
  border-radius: 8px;
  font-size: 0.82rem;
  color: #888;
  cursor: pointer;
  transition: color 0.15s, border-color 0.15s, background 0.15s;
  white-space: nowrap;
}
.btn-logout:hover {
  color: #b42318;
  border-color: rgba(180,35,24,0.25);
  background: rgba(180,35,24,0.04);
}

/* ── 引导语 ── */
.greeting { display: grid; gap: 4px; }

.greeting h2 {
  margin: 0;
  font-size: clamp(1.25rem, 3.5vw, 1.5rem);
  font-weight: 800;
  color: #0f0f0f;
  letter-spacing: -0.02em;
}

.greeting p {
  margin: 0;
  font-size: 0.88rem;
  color: #999;
}

/* ── 成员卡片网格 ── */
.member-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(140px, 1fr));
  gap: 14px;
}

/* ── 单个成员卡片 ── */
.member-card {
  position: relative;
  background: rgba(255,255,255,0.92);
  border: 1.5px solid rgba(226,216,200,0.8);
  border-radius: 18px;
  padding: 22px 12px 18px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  transition: box-shadow 0.18s, transform 0.18s, border-color 0.18s;
  overflow: hidden;
  font: inherit;
}

.member-card:not(:disabled):hover {
  box-shadow: 0 8px 28px rgba(45,95,93,0.15);
  transform: translateY(-3px);
  border-color: rgba(45,95,93,0.35);
}

.member-card:not(:disabled):active {
  transform: translateY(-1px);
  box-shadow: 0 4px 14px rgba(45,95,93,0.12);
}

/* 关怀成员：暖色调 */
.member-card.is-care {
  background: linear-gradient(160deg, rgba(255,249,242,0.96) 0%, rgba(255,244,234,0.9) 100%);
  border-color: rgba(212,120,90,0.28);
}
.member-card.is-care:not(:disabled):hover {
  border-color: rgba(212,120,90,0.5);
  box-shadow: 0 8px 28px rgba(212,120,90,0.14);
}

/* 管理员：绿色调 */
.member-card.is-admin {
  background: linear-gradient(160deg, rgba(244,250,249,0.96) 0%, rgba(238,247,246,0.9) 100%);
  border-color: rgba(45,95,93,0.28);
}
.member-card.is-admin:not(:disabled):hover {
  border-color: rgba(45,95,93,0.5);
  box-shadow: 0 8px 28px rgba(45,95,93,0.14);
}

/* 切换中变暗 */
.member-card.is-switching {
  opacity: 0.75;
}
.member-card:disabled {
  cursor: not-allowed;
}

/* ── 头像外环 ── */
.avatar-ring {
  border-radius: 50%;
  padding: 3px;
  background: rgba(255,255,255,0.6);
  border: 2px solid rgba(226,216,200,0.6);
  transition: border-color 0.18s;
}
.ring-admin {
  border-color: rgba(45,95,93,0.3);
  background: rgba(228,240,239,0.5);
}
.ring-care {
  border-color: rgba(212,120,90,0.3);
  background: rgba(247,236,230,0.5);
}

/* ── 昵称 ── */
.member-name {
  font-size: 0.95rem;
  font-weight: 700;
  color: #1a1a1a;
  text-align: center;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* ── 标签 ── */
.member-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  justify-content: center;
  min-height: 20px;
}

.tag {
  font-size: 0.7rem;
  padding: 2px 7px;
  border-radius: 999px;
  font-weight: 600;
  white-space: nowrap;
}

.tag-relation {
  background: rgba(45,95,93,0.08);
  color: #436361;
}

.tag-admin {
  background: rgba(45,95,93,0.14);
  color: #2d5f5d;
}

.tag-care {
  background: rgba(212,120,90,0.14);
  color: #b05535;
}

/* ── 切换中遮罩 ── */
.switching-mask {
  position: absolute;
  inset: 0;
  border-radius: 18px;
  background: rgba(255,255,255,0.55);
  backdrop-filter: blur(2px);
  display: grid;
  place-items: center;
}

.spin-icon {
  color: #2d5f5d;
  animation: spin 0.9s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

/* ── 骨架屏 ── */
.skeleton {
  cursor: default;
  pointer-events: none;
}

.sk-avatar {
  width: 74px;
  height: 74px;
  border-radius: 50%;
  background: linear-gradient(90deg, #ece9e0 25%, #f5f2eb 50%, #ece9e0 75%);
  background-size: 200% 100%;
  animation: shimmer 1.4s ease infinite;
}

.sk-line {
  border-radius: 999px;
  background: linear-gradient(90deg, #ece9e0 25%, #f5f2eb 50%, #ece9e0 75%);
  background-size: 200% 100%;
  animation: shimmer 1.4s ease infinite;
}

.sk-name { width: 70%; height: 12px; }
.sk-tag  { width: 45%; height: 10px; }

@keyframes shimmer {
  0%   { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}

/* ── 空状态 ── */
.empty-state {
  display: grid;
  place-items: center;
  gap: 8px;
  padding: 48px 0 32px;
  text-align: center;
}

.empty-icon {
  font-size: 3rem;
  margin-bottom: 4px;
}

.empty-title {
  font-size: 1rem;
  font-weight: 700;
  color: #555;
}

.empty-desc {
  font-size: 0.85rem;
  color: #aaa;
}

/* ── 错误提示 ── */
.error-tip {
  margin: 0;
  font-size: 0.85rem;
  color: #b42318;
  text-align: center;
}

/* ── 过渡动画 ── */
.fade-enter-active, .fade-leave-active { transition: opacity 0.2s; }
.fade-enter-from, .fade-leave-to { opacity: 0; }

/* ── 手机端适配 ── */
@media (max-width: 480px) {
  .shell { padding: 12px; }

  .panel {
    padding: 18px 14px;
    border-radius: 20px;
    gap: 18px;
  }

  .brand-logo { width: 30px; height: 30px; }
  .brand-name { font-size: 0.9rem; }
  .brand-sub  { display: none; }

  .greeting h2 { font-size: 1.15rem; }

  /* 手机端：2 列固定，卡片紧凑 */
  .member-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: 10px;
  }

  .member-card {
    padding: 18px 8px 14px;
    border-radius: 14px;
    gap: 7px;
  }

  .member-name { font-size: 0.88rem; }
}

/* 极窄屏（< 320px）：单列 */
@media (max-width: 319px) {
  .member-grid { grid-template-columns: 1fr; }
}
</style>
