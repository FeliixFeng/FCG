<script setup>
import { computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '../../stores/user'

const userStore = useUserStore()
const router = useRouter()
const route = useRoute()

const memberName = computed(() => userStore.member?.nickname || '访客')
const familyName = computed(() => userStore.family?.familyName || '我的家庭')

const navItems = [
  { name: 'dashboard', label: '首页', icon: homeIcon() },
  { name: 'medicine', label: '药品', icon: medicineIcon() },
  { name: 'health',   label: '健康', icon: healthIcon() },
  { name: 'family',   label: '家庭', icon: familyIcon() },
]

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

// SVG icon helpers
function homeIcon() { return 'home' }
function medicineIcon() { return 'medicine' }
function healthIcon() { return 'health' }
function familyIcon() { return 'family' }
</script>

<template>
  <div class="app-shell">
    <!-- ── 顶部导航（桌面） ── -->
    <header class="topbar">
      <div class="topbar-inner">
        <!-- 品牌 -->
        <div class="brand" @click="go('dashboard')">
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
            <span class="nav-icon">
              <!-- home -->
              <svg v-if="item.icon === 'home'" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"/>
                <polyline points="9 22 9 12 15 12 15 22"/>
              </svg>
              <!-- medicine -->
              <svg v-else-if="item.icon === 'medicine'" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M19 14c1.49-1.46 3-3.21 3-5.5A5.5 5.5 0 0 0 16.5 3c-1.76 0-3 .5-4.5 2-1.5-1.5-2.74-2-4.5-2A5.5 5.5 0 0 0 2 8.5c0 2.3 1.5 4.05 3 5.5l7 7Z"/>
              </svg>
              <!-- health -->
              <svg v-else-if="item.icon === 'health'" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <polyline points="22 12 18 12 15 21 9 3 6 12 2 12"/>
              </svg>
              <!-- family -->
              <svg v-else-if="item.icon === 'family'" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/>
                <circle cx="9" cy="7" r="4"/>
                <path d="M23 21v-2a4 4 0 0 0-3-3.87"/>
                <path d="M16 3.13a4 4 0 0 1 0 7.75"/>
              </svg>
            </span>
            {{ item.label }}
          </button>
        </nav>

        <!-- 成员区 -->
        <div class="user-area">
          <el-dropdown trigger="click" @command="handleUserCommand" placement="bottom-end">
            <div class="member-chip">
              <div class="member-avatar-dot"></div>
              <span class="member-name">{{ memberName }}</span>
              <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round">
                <polyline points="6 9 12 15 18 9"/>
              </svg>
            </div>
            <template #dropdown>
              <el-dropdown-menu class="user-dropdown">
                <div class="dropdown-header">
                  <div class="dropdown-name">{{ memberName }}</div>
                  <div class="dropdown-role">{{ userStore.isAdmin ? '管理员' : userStore.isCareMode ? '关怀成员' : '普通成员' }}</div>
                </div>
                <el-dropdown-item command="switch">
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="margin-right:7px;flex-shrink:0">
                    <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/>
                    <circle cx="9" cy="7" r="4"/>
                    <path d="M23 21v-2a4 4 0 0 0-3-3.87"/>
                    <path d="M16 3.13a4 4 0 0 1 0 7.75"/>
                  </svg>
                  切换成员
                </el-dropdown-item>
                <el-dropdown-item command="logout" divided>
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
    <nav class="bottom-bar">
      <button
        v-for="item in navItems"
        :key="item.name"
        class="tab-item"
        :class="{ active: isActive(item.name) }"
        @click="go(item.name)"
      >
        <span class="tab-icon">
          <svg v-if="item.icon === 'home'" width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"/>
            <polyline points="9 22 9 12 15 12 15 22"/>
          </svg>
          <svg v-else-if="item.icon === 'medicine'" width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M19 14c1.49-1.46 3-3.21 3-5.5A5.5 5.5 0 0 0 16.5 3c-1.76 0-3 .5-4.5 2-1.5-1.5-2.74-2-4.5-2A5.5 5.5 0 0 0 2 8.5c0 2.3 1.5 4.05 3 5.5l7 7Z"/>
          </svg>
          <svg v-else-if="item.icon === 'health'" width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <polyline points="22 12 18 12 15 21 9 3 6 12 2 12"/>
          </svg>
          <svg v-else-if="item.icon === 'family'" width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/>
            <circle cx="9" cy="7" r="4"/>
            <path d="M23 21v-2a4 4 0 0 0-3-3.87"/>
            <path d="M16 3.13a4 4 0 0 1 0 7.75"/>
          </svg>
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
  opacity: 0.8;
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

.tab-label {
  font-size: 0.68rem;
  font-weight: 500;
  letter-spacing: 0.02em;
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
  padding: 12px 16px 10px;
  border-bottom: 1px solid rgba(45, 95, 93, 0.09);
  margin-bottom: 4px;
}

.dropdown-name {
  font-size: 0.9rem;
  font-weight: 600;
  color: #1a1a1a;
  line-height: 1.3;
}

.dropdown-role {
  font-size: 0.72rem;
  color: #2d5f5d;
  margin-top: 2px;
  opacity: 0.8;
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
    display: none;
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
