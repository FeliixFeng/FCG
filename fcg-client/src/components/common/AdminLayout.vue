<script setup>
import { computed, ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '../../stores/user'

const userStore = useUserStore()
const router = useRouter()
const route = useRoute()

const loadingProfile = ref(false)

onMounted(async () => {
  if (userStore.hasMember && !userStore.member) {
    try {
      loadingProfile.value = true
      await userStore.fetchProfile()
    } catch (err) {
      userStore.logout()
      router.replace({ name: 'landing' })
    } finally {
      loadingProfile.value = false
    }
  }
  if (userStore.isLoggedIn && !userStore.family) {
    try {
      await userStore.fetchFamilyInfo()
    } catch (err) {
      console.error('[AdminLayout] 加载家庭信息失败', err)
    }
  }
})

const familyName = computed(() => userStore.family?.familyName || '我的家庭')

// 管理界面导航
const adminNavItems = [
  { name: 'admin', label: '首页', icon: 'HomeFilled' },
  { name: 'admin-members', label: '成员', icon: 'User' },
  { name: 'admin-medicines', label: '药品', icon: 'FirstAidKit' },
  { name: 'admin-data', label: '统计', icon: 'DataLine' },
  { name: 'admin-system', label: '设置', icon: 'Setting' },
]

const isActive = (name) => route.name === name

const go = (name) => router.push({ name })

// 回到管理首页
const goHome = () => {
  router.push({ name: 'admin' })
}

// 退出管理界面，回到用户首页
const exitAdmin = () => {
  router.push({ name: 'home' })
}
</script>

<template>
  <div class="admin-shell">
    <!-- 顶部导航 -->
    <header class="admin-topbar">
      <div class="topbar-inner">
        <div class="brand" @click="goHome">
          <img src="/fcg.png" alt="FCG" class="brand-logo" />
          <div class="brand-text">
            <div class="brand-title">管理中心</div>
            <div class="brand-sub">{{ familyName }}</div>
          </div>
        </div>

<!-- 桌面端导航 -->
        <nav class="admin-top-nav">
          <button
            v-for="item in adminNavItems"
            :key="item.name"
            class="nav-link"
            :class="{ active: isActive(item.name) }"
            @click="go(item.name)"
          >
            <component :is="item.icon" class="nav-icon" />
            {{ item.label }}
          </button>
        </nav>
        
        <button class="btn-exit" @click="exitAdmin">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/>
            <polyline points="16 17 21 12 16 7"/>
            <line x1="21" y1="12" x2="9" y2="12"/>
          </svg>
          退出管理
        </button>
      </div>
    </header>

    <!-- 主内容 -->
    <main class="admin-main">
      <router-view />
    </main>

    <!-- 底部 Tab Bar（移动端） -->
    <nav class="admin-bottom-bar">
      <button
        v-for="item in adminNavItems"
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
.admin-shell {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: linear-gradient(145deg, #eef5f4 0%, #f0ebe0 45%, #e8f2f0 100%);
}

/* 顶部导航 */
.admin-topbar {
  position: sticky;
  top: 0;
  z-index: 50;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(16px);
  border-bottom: 1px solid rgba(45, 95, 93, 0.1);
  box-shadow: 0 2px 12px rgba(45, 95, 93, 0.08);
}

.topbar-inner {
  max-width: 1200px;
  margin: 0 auto;
  padding: 12px 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.brand {
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
}

.brand-logo {
  width: 36px;
  height: 36px;
  object-fit: contain;
}

.brand-text {
  display: flex;
  flex-direction: column;
}

.brand-title {
  font-size: 1rem;
  font-weight: 600;
  color: #2d5f5d;
  line-height: 1.2;
}

.brand-sub {
  font-size: 0.75rem;
  color: #666;
  margin-top: 2px;
}

/* 桌面端导航 */
.admin-top-nav {
  display: none;
  align-items: center;
  gap: 4px;
  flex: 1;
}

.nav-link {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 7px 14px;
  background: transparent;
  border: none;
  border-radius: 10px;
  color: #666;
  font-size: 0.88rem;
  font-weight: 500;
  cursor: pointer;
  transition: background 0.15s, color 0.15s;
  font-family: inherit;
  white-space: nowrap;
}

.nav-icon {
  width: 18px;
  height: 18px;
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

.btn-exit {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  background: transparent;
  border: 1px solid #2d5f5d;
  border-radius: 6px;
  color: #2d5f5d;
  font-size: 0.85rem;
  cursor: pointer;
  transition: all 0.2s ease;
}

.btn-exit:hover {
  background: #2d5f5d;
  color: white;
}

/* 主内容 */
.admin-main {
  flex: 1;
  max-width: 1200px;
  width: 100%;
  margin: 0 auto;
  padding: 28px 24px 100px;
}

/* 底部 Tab Bar */
.admin-bottom-bar {
  display: none;
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  z-index: 50;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(16px);
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
  background: transparent;
  border: none;
  cursor: pointer;
  color: #aaa;
  transition: color 0.15s;
  font-family: inherit;
}

.tab-item.active {
  color: #2d5f5d;
  font-weight: 600;
}

.tab-item.active .tab-icon {
  transform: scale(1.1);
}

.tab-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  margin-bottom: 2px;
  transition: transform 0.2s ease;
}

.tab-label {
  font-size: 0.68rem;
  font-weight: 500;
  letter-spacing: 0.02em;
}

.tab-back {
  flex: 0.8;
}

.tab-back .tab-label {
  color: #2d5f5d;
  font-weight: 500;
}

/* 桌面端 */
@media (min-width: 768px) {
  .admin-top-nav {
    display: flex;
  }
  
  .admin-bottom-bar {
    display: none !important;
  }
  
  .admin-main {
    padding-bottom: 28px;
  }
  
  .btn-exit {
    padding: 10px 20px;
  }
}

/* 移动端 */
@media (max-width: 767px) {
  .admin-bottom-bar {
    display: flex;
  }
  
  .topbar-inner {
    padding: 12px 16px;
  }
  
  .brand-title {
    font-size: 0.9rem;
  }
  
  .btn-exit span {
    display: none;
  }
  
  .btn-exit {
    padding: 8px 12px;
  }
}
</style>
