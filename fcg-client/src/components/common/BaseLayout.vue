<script setup>
import { useUserStore } from '../../stores/user'
import { computed } from 'vue'

const userStore = useUserStore()
const username = computed(() => userStore.member?.nickname || userStore.family?.familyName || '访客')
</script>

<template>
  <div class="app-shell">
    <header class="topbar">
      <div class="container topbar-content">
        <div class="brand">
          <div class="brand-mark">
            <img src="/fcg.png" alt="FCG" class="brand-logo" />
          </div>
          <div>
            <div class="brand-title">Family Care Guardian</div>
            <div class="brand-sub">家庭健康管理系统</div>
          </div>
        </div>
        <div class="user-area">
          <span class="muted">你好，{{ username }}</span>
        </div>
      </div>
    </header>
    <main class="container main-content">
      <slot />
    </main>
  </div>
</template>

<style scoped>
.topbar {
  padding: 18px 0;
  background: linear-gradient(90deg, #f4efe6 0%, #f8f4ee 50%, #efe7da 100%);
  border-bottom: 1px solid var(--border);
}

.topbar-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.brand {
  display: flex;
  align-items: center;
  gap: 12px;
}

.brand-mark {
  width: 44px;
  height: 44px;
  border-radius: 14px;
  display: grid;
  place-items: center;
  overflow: hidden;
}

.brand-logo {
  width: 44px;
  height: 44px;
  object-fit: contain;
}

.brand-title {
  font-weight: 700;
  font-size: 1.05rem;
}

.brand-sub {
  font-size: 0.85rem;
  color: var(--muted);
}

.main-content {
  padding: 28px 0 56px;
}
</style>
