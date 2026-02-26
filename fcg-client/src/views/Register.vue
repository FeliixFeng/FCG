<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { registerFamily } from '../utils/api'

const router = useRouter()
const loading = ref(false)
const error = ref('')

const form = reactive({
  familyName: '',
  username: '',
  password: '',
  adminNickname: ''
})

const submit = async () => {
  error.value = ''
  if (!form.username || !form.password || !form.adminNickname) {
    error.value = '请填写必填项'
    return
  }
  try {
    loading.value = true
    await registerFamily({ ...form })
    // 注册成功跳转登录页
    router.replace({ name: 'login' })
  } catch (err) {
    error.value = err?.message || '注册失败'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="page">
    <div class="card panel">
      <div class="header">
        <div class="brand-mark">FCG</div>
        <div>
          <div class="title">创建家庭账号</div>
          <div class="muted">注册后可添加家庭成员</div>
        </div>
      </div>

      <form class="form" @submit.prevent="submit">
        <label class="field">
          <span>家庭账号 <em>*</em></span>
          <input v-model="form.username" class="input" placeholder="4-20位，登录用" autocomplete="username" />
        </label>
        <label class="field">
          <span>密码 <em>*</em></span>
          <input v-model="form.password" class="input" type="password" placeholder="6-20位" autocomplete="new-password" />
        </label>
        <label class="field">
          <span>您的昵称 <em>*</em></span>
          <input v-model="form.adminNickname" class="input" placeholder="例如：爸爸、张三" />
        </label>
        <label class="field">
          <span>家庭名称（选填）</span>
          <input v-model="form.familyName" class="input" placeholder="不填则自动生成" />
        </label>

        <p v-if="error" class="error">{{ error }}</p>

        <button class="btn" type="submit" :disabled="loading">
          {{ loading ? '注册中...' : '注册' }}
        </button>
        <p class="muted center">
          已有账号？<a @click="router.push({ name: 'login' })">去登录</a>
        </p>
      </form>
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
.panel {
  width: min(420px, 92vw);
  padding: 28px;
  display: grid;
  gap: 20px;
}
.header {
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
  flex-shrink: 0;
}
.title {
  font-size: 1.4rem;
  font-weight: 700;
}
.form {
  display: grid;
  gap: 14px;
}
.field {
  display: grid;
  gap: 6px;
  font-size: 0.9rem;
}
.field em {
  color: #b42318;
  font-style: normal;
}
.error {
  color: #b42318;
  margin: 0;
}
.center {
  text-align: center;
  font-size: 0.9rem;
}
.center a {
  color: var(--primary);
  cursor: pointer;
}
</style>
