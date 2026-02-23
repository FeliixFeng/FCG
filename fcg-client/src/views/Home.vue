<script setup>
import BaseLayout from '../components/common/BaseLayout.vue'
import { useUserStore } from '../stores/user'
import { useRouter } from 'vue-router'

const userStore = useUserStore()
const router = useRouter()

const goFamily = () => router.push({ name: 'family' })
const goMedicine = () => router.push({ name: 'medicine' })
const goHealth = () => router.push({ name: 'health' })
</script>

<template>
  <BaseLayout>
    <section class="hero card">
      <div>
        <div class="hero-title">欢迎回到 FCG</div>
        <p class="muted">后端已准备就绪，开始逐步建设前端页面。</p>
      </div>
      <div class="hero-actions">
        <button class="btn" type="button" @click="goFamily">进入家庭模块</button>
        <button class="btn ghost" type="button" @click="goMedicine">进入药品模块</button>
        <button class="btn ghost" type="button" @click="goHealth">进入健康模块</button>
      </div>
      <div class="hero-meta muted">当前登录：{{ userStore.profile?.username || '未获取' }}</div>
    </section>
  </BaseLayout>
</template>

<style scoped>
.hero {
  padding: 28px;
  display: grid;
  gap: 16px;
}

.hero-title {
  font-size: 1.6rem;
  font-weight: 700;
}

.hero-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.btn.ghost {
  background: transparent;
  border-color: var(--primary);
  color: var(--primary);
}

.hero-meta {
  font-size: 0.9rem;
}
</style>
