<script setup>
import { computed, ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '../../stores/user'
import AvatarIcon from './AvatarIcon.vue'
import { HomeFilled, FirstAidKit, Monitor, UserFilled, Setting } from '@element-plus/icons-vue'

const userStore = useUserStore()
const router = useRouter()
const route = useRoute()

const isPreviewMode = ref(false)
const loadingProfile = ref(false)

onMounted(async () => {
  isPreviewMode.value = sessionStorage.getItem('fcg_preview_care') === '1'
  
  // 🔧 修复：页面刷新时，如果有 memberToken 但 member 为空，自动加载用户信息
  if (userStore.hasMember && !userStore.member) {
    try {
      loadingProfile.value = true
      await userStore.fetchProfile()
    } catch (err) {
      console.error('[BaseLayout] 加载用户信息失败', err)
      // token 可能过期，清除状态并跳转登录页
      userStore.logout()
      router.replace({ name: 'landing' })
    } finally {
      loadingProfile.value = false
    }
  }
})

const exitPreview = () => {
  sessionStorage.removeItem('fcg_preview_care')
  window.location.reload()
}

const memberName = computed(() => {
  // 正在加载用户信息
  if (loadingProfile.value) return '加载中...'
  // 显示用户昵称
  return userStore.member?.nickname || '访客'
})

const familyName = computed(() => userStore.family?.familyName || '我的家庭')
const isCareMode = computed(() => userStore.isCareMode || isPreviewMode.value)

const defaultNavItems = [
  { name: 'home', label: '首页', icon: 'HomeFilled' },
  { name: 'medicine', label: '药品', icon: 'FirstAidKit' },
  { name: 'health', label: '健康', icon: 'Monitor' },
  { name: 'ai', label: 'AI', icon: 'ChatDotRound' },
  { name: 'profile', label: '我的', icon: 'User' }
]

const careNavItems = [
  { name: 'home', label: '首页', icon: 'HomeFilled' },
  { name: 'health', label: '健康', icon: 'Monitor' },
  { name: 'profile', label: '我的', icon: 'User' }
]

// 桌面端导航菜单（关怀成员缩减为3个Tab）
const navItems = computed(() => {
  return isCareMode.value ? careNavItems : defaultNavItems
})

// 移动端导航菜单（关怀成员缩减为3个Tab）
const mobileNavItems = computed(() => {
  return isCareMode.value ? careNavItems : defaultNavItems
})

const isActive = (name) => route.name === name

const go = (name) => router.push({ name })

const handleUserCommand = (cmd) => {
  if (cmd === 'switch') {
    userStore.exitMember()
    router.replace({ name: 'select-member' })
  } else if (cmd === 'logout') {
    userStore.logout()
    router.replace({ name: 'landing' })
  }
}
</script>

<template>
  <div class="app-shell">
    <!-- ── 预览模式退出横幅 ── -->
    <div v-if="isPreviewMode" class="preview-banner">
      <div class="preview-inner">
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
          <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/>
          <circle cx="12" cy="12" r="3"/>
        </svg>
        <span class="preview-text">正在预览关怀模式</span>
        <button @click="exitPreview" class="btn-exit-preview">退出预览</button>
      </div>
    </div>
    
    <!-- ── 顶部导航（桌面） ── -->
    <header class="topbar">
      <div class="topbar-inner">
        <!-- 品牌 -->
        <div class="brand" @click="go('home')">
          <img src="/fcg.png" alt="FCG" class="brand-logo" />
          <div class="brand-text">
            <div class="brand-title">Family Care Guardian</div>
            <div class="brand-sub">{{ familyName }}</div>
          </div>
        </div>

        <!-- 导航链接 -->
        <nav class="top-nav">
          <button
            v-for="item in navItems"
            :key="item.name"
            class="nav-link"
            :class="{ active: isActive(item.name) }"
            @click="go(item.name)"
          >
            <component :is="item.icon" class="nav-icon" />
            {{ item.label }}
          </button>
        </nav>

        <!-- 成员区 -->
        <div class="user-area">
          <el-dropdown trigger="click" @command="handleUserCommand" placement="bottom-end">
            <div class="member-chip">
              <AvatarIcon
                :avatar="userStore.member?.avatar"
                :relation="userStore.member?.relation"
                :role="userStore.member?.role"
                :nickname="userStore.member?.nickname"
                :size="32"
              />
              <span class="member-name">{{ memberName }}</span>
              <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round">
                <polyline points="6 9 12 15 18 9"/>
              </svg>
            </div>
            <template #dropdown>
              <el-dropdown-menu class="user-dropdown">
                <div class="dropdown-header">
                  <AvatarIcon
                    class="dropdown-avatar-icon"
                    :avatar="userStore.member?.avatar"
                    :relation="userStore.member?.relation"
                    :role="userStore.member?.role"
                    :nickname="userStore.member?.nickname"
                    :size="40"
                  />
                  <div class="dropdown-info">
                    <div class="dropdown-name">{{ memberName }}</div>
                    <div class="dropdown-role">{{ userStore.isAdmin ? '管理员' : userStore.isCareMode ? '关怀成员' : '普通成员' }}</div>
                  </div>
                </div>
                <el-dropdown-item command="switch" divided>
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="margin-right:7px;flex-shrink:0">
                    <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/>
                    <circle cx="9" cy="7" r="4"/>
                    <path d="M23 21v-2a4 4 0 0 0-3-3.87"/>
                    <path d="M16 3.13a4 4 0 0 1 0 7.75"/>
                  </svg>
                  切换成员
                </el-dropdown-item>
                <el-dropdown-item command="logout">
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="margin-right:7px;flex-shrink:0">
                    <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/>
                    <polyline points="16 17 21 12 16 7"/>
                    <line x1="21" y1="12" x2="9" y2="12"/>
                  </svg>
                  退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>
    </header>

    <!-- ── 主内容 ── -->
    <main class="main-content">
      <slot />
    </main>

    <!-- ── 底部 Tab Bar（移动端） ── -->
    <nav class="bottom-bar" :class="{ 'care-mode': isCareMode }">
      <button
        v-for="item in mobileNavItems"
        :key="item.name"
        class="tab-item"
        :class="{ active: isActive(item.name) }"
        @click="go(item.name)"
      >
        <span class="tab-icon">
          <component :is="item.icon" />
        </span>
        <span class="tab-label">{{ item.label }}</span>
      </button>
    </nav>
  </div>
</template>

<style scoped>
/* ── 整体布局 ── */
.app-shell {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: linear-gradient(145deg, #eef5f4 0%, #f0ebe0 45%, #e8f2f0 100%);
  position: relative;
}

/* ── 预览模式横幅 ── */
.preview-banner {
  position: sticky;
  top: 0;
  z-index: 51;
  background: linear-gradient(135deg, #2d5f5d, #3a7573);
  color: white;
  padding: 10px 24px;
  box-shadow: 0 2px 12px rgba(45, 95, 93, 0.3);
}

.preview-inner {
  max-width: 1200px;
  margin: 0 auto;
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 0.9rem;
  font-weight: 500;
}

.preview-text {
  flex: 1;
}

.btn-exit-preview {
  display: inline-flex;
  align-items: center;
  padding: 6px 14px;
  border: 1px solid rgba(255, 255, 255, 0.4);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.15);
  color: white;
  font-size: 0.85rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  font-family: inherit;
}

.btn-exit-preview:hover {
  background: rgba(255, 255, 255, 0.25);
  border-color: rgba(255, 255, 255, 0.6);
}

/* ── 顶部导航 ── */
.topbar {
  position: sticky;
  top: 0;
  z-index: 50;
  background: rgba(255, 255, 255, 0.88);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  border-bottom: 1px solid rgba(45, 95, 93, 0.1);
  box-shadow: 0 1px 12px rgba(45, 95, 93, 0.07);
}

.topbar-inner {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 24px;
  height: 64px;
  display: flex;
  align-items: center;
  gap: 32px;
}

/* ── 品牌 ── */
.brand {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  flex-shrink: 0;
  text-decoration: none;
}

.brand-logo {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  object-fit: contain;
}

.brand-text {
  display: flex;
  flex-direction: column;
  gap: 1px;
}

.brand-title {
  font-size: 0.9rem;
  font-weight: 700;
  color: #1a1a1a;
  line-height: 1.2;
  white-space: nowrap;
}

.brand-sub {
  font-size: 0.7rem;
  color: #999;
  white-space: nowrap;
}

/* ── 导航链接 ── */
.top-nav {
  display: flex;
  align-items: center;
  gap: 4px;
  flex: 1;
}

.nav-link {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 7px 14px;
  border: none;
  border-radius: 10px;
  background: transparent;
  font-size: 0.88rem;
  font-weight: 500;
  color: #666;
  cursor: pointer;
  transition: background 0.15s, color 0.15s;
  font-family: inherit;
  white-space: nowrap;
}

.nav-link:hover {
  background: rgba(45, 95, 93, 0.07);
  color: #2d5f5d;
}

.nav-link.active {
  background: rgba(45, 95, 93, 0.1);
  color: #2d5f5d;
  font-weight: 600;
}

.nav-icon {
  display: flex;
  align-items: center;
  width: 16px;
  height: 16px;
  opacity: 0.8;
}

.nav-link.active .nav-icon {
  opacity: 1;
}

.nav-link.active .nav-icon {
  opacity: 1;
}

/* ── 成员区 ── */
.user-area {
  flex-shrink: 0;
  margin-left: auto;
}

.member-chip {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  padding: 6px 12px;
  border: 1px solid rgba(45, 95, 93, 0.18);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.7);
  cursor: pointer;
  transition: background 0.15s, border-color 0.15s;
  font-size: 0.85rem;
  color: #2d5f5d;
  font-weight: 500;
}

.member-chip:hover {
  background: rgba(45, 95, 93, 0.06);
  border-color: rgba(45, 95, 93, 0.35);
}

.member-avatar-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #2d5f5d;
  flex-shrink: 0;
}

.member-name {
  max-width: 80px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* ── 主内容 ── */
.main-content {
  flex: 1;
  max-width: 1200px;
  width: 100%;
  margin: 0 auto;
  padding: 28px 24px 40px;
}

/* ── 底部 Tab Bar（移动端） ── */
.bottom-bar {
  display: none;
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  z-index: 50;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  border-top: 1px solid rgba(45, 95, 93, 0.1);
  box-shadow: 0 -2px 12px rgba(45, 95, 93, 0.08);
  padding-bottom: env(safe-area-inset-bottom);
}

.tab-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 3px;
  padding: 10px 0 8px;
  border: none;
  background: transparent;
  color: #aaa;
  cursor: pointer;
  transition: color 0.15s;
  font-family: inherit;
}

.tab-item.active {
  color: #2d5f5d;
}

.tab-icon {
  display: flex;
  align-items: center;
  justify-content: center;
}

.tab-icon svg {
  width: 22px;
  height: 22px;
}

.tab-label {
  font-size: 0.68rem;
  font-weight: 500;
  letter-spacing: 0.02em;
}

.bottom-bar.care-mode .tab-item {
  padding: 14px 0 12px;
}

.bottom-bar.care-mode .tab-icon svg {
  width: 28px;
  height: 28px;
}

.bottom-bar.care-mode .tab-icon {
  width: 28px;
  height: 28px;
}

.bottom-bar.care-mode .tab-label {
  font-size: 0.85rem;
  font-weight: 600;
}

/* ── 用户下拉菜单 ── */
:deep(.user-dropdown) {
  border-radius: 14px !important;
  border: 1px solid rgba(45, 95, 93, 0.12) !important;
  box-shadow: 0 8px 32px rgba(45, 95, 93, 0.13) !important;
  overflow: hidden;
  padding: 4px 0 6px !important;
  min-width: 160px;
}

.dropdown-header {
  padding: 14px 16px 12px;
  border-bottom: 1px solid rgba(45, 95, 93, 0.09);
  margin-bottom: 4px;
  display: flex;
  align-items: center;
  gap: 12px;
}

.dropdown-avatar-icon {
  flex-shrink: 0;
}

.dropdown-info {
  flex: 1;
  min-width: 0;
}

.dropdown-name {
  font-size: 0.95rem;
  font-weight: 600;
  color: #1a1a1a;
  line-height: 1.3;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.dropdown-role {
  font-size: 0.75rem;
  color: #2d5f5d;
  margin-top: 3px;
  opacity: 0.85;
}

:deep(.user-dropdown .el-dropdown-menu__item) {
  display: flex;
  align-items: center;
  font-size: 0.87rem;
  color: #333;
  padding: 8px 16px;
  border-radius: 0;
  transition: background 0.13s;
}

:deep(.user-dropdown .el-dropdown-menu__item:hover) {
  background: rgba(45, 95, 93, 0.07);
  color: #2d5f5d;
}

:deep(.user-dropdown .el-dropdown-menu__item.is-divided) {
  margin-top: 4px;
  border-top: 1px solid rgba(45, 95, 93, 0.09);
}

/* ── 响应式 ── */
@media (max-width: 767px) {
  .top-nav {
    display: none;
  }

  .user-area {
    display: none;
  }

  .brand-text {
    display: flex;
    flex-direction: column;
    margin-left: 8px;
  }

  .brand-title {
    display: none;
  }

  .brand-sub {
    display: block;
    font-size: 0.85rem;
    font-weight: 500;
    color: #2d5f5d;
  }

  .topbar-inner {
    padding: 0 16px;
    height: 54px;
    justify-content: center;
  }

  .bottom-bar {
    display: flex;
  }

  .main-content {
    padding: 16px 14px calc(64px + env(safe-area-inset-bottom));
  }
}

@media (max-width: 480px) {
  .main-content {
    padding: 12px 10px calc(64px + env(safe-area-inset-bottom));
  }
}
</style>
