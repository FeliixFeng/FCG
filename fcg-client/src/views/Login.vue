<script setup>
import { reactive, ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '../stores/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const form = reactive({
  username: '',
  password: ''
})
const loading = ref(false)
const error = ref('')

const submit = async () => {
  error.value = ''
  if (!form.username || !form.password) {
    error.value = '请输入用户名与密码'
    return
  }
  try {
    loading.value = true
    await userStore.login({ ...form })
    await userStore.fetchProfile()
    const redirect = route.query.redirect || '/'
    router.replace(redirect)
  } catch (err) {
    error.value = err?.message || '登录失败'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login-page">
    <div class="login-card card">
      <div class="login-header">
        <div class="brand-mark">FCG</div>
        <div>
          <div class="login-title">登录</div>
          <div class="muted">进入家庭健康管理系统</div>
        </div>
      </div>
      <form class="login-form" @submit.prevent="submit">
        <label class="field">
          <span>用户名</span>
          <input v-model="form.username" class="input" autocomplete="username" />
        </label>
        <label class="field">
          <span>密码</span>
          <input v-model="form.password" class="input" type="password" autocomplete="current-password" />
        </label>
        <p v-if="error" class="error">{{ error }}</p>
        <button class="btn" type="submit" :disabled="loading">
          {{ loading ? '登录中...' : '登录' }}
        </button>
      </form>
    </div>
  </div>
</template>

<style scoped>
.login-page {
  min-height: 100vh;
  display: grid;
  place-items: center;
  padding: 24px;
}

.login-card {
  width: min(420px, 92vw);
  padding: 28px;
  display: grid;
  gap: 20px;
}

.login-header {
  display: flex;
  align-items: center;
  gap: 12px;
}

.brand-mark {
  width: 48px;
  height: 48px;
  border-radius: 14px;
  display: grid;
  place-items: center;
  background: var(--primary);
  color: var(--primary-ink);
  font-weight: 700;
}

.login-title {
  font-size: 1.4rem;
  font-weight: 700;
}

.login-form {
  display: grid;
  gap: 14px;
}

.field {
  display: grid;
  gap: 6px;
  font-size: 0.9rem;
}

.error {
  color: #b42318;
  margin: 0;
}

.btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
</style>
