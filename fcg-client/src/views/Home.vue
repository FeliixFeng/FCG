<script setup>
import BaseLayout from '../components/common/BaseLayout.vue'
import { useUserStore } from '../stores/user'
import { useRouter } from 'vue-router'
import { ref } from 'vue'
import { verifyAdmin } from '../utils/api'

const userStore = useUserStore()
const router = useRouter()

// 管理员验证弹窗
const showAdminModal = ref(false)
const adminPassword = ref('')
const adminError = ref('')
const adminLoading = ref(false)

const goFamily = () => router.push({ name: 'family' })
const goMedicine = () => router.push({ name: 'medicine' })
const goHealth = () => router.push({ name: 'health' })

// 验证管理员密码后进入管理员界面
const confirmAdmin = async () => {
  adminError.value = ''
  if (!adminPassword.value) {
    adminError.value = '请输入密码'
    return
  }
  try {
    adminLoading.value = true
    await verifyAdmin(adminPassword.value)
    showAdminModal.value = false
    adminPassword.value = ''
    router.push({ name: 'admin' })
  } catch (err) {
    adminError.value = err?.message || '密码错误'
  } finally {
    adminLoading.value = false
  }
}

// 退出当前成员，回到选人页
const switchMember = () => {
  userStore.exitMember()
  router.replace({ name: 'select-member' })
}
</script>

<template>
  <BaseLayout>
    <section class="hero card">
      <div>
        <div class="hero-title">你好，{{ userStore.member?.nickname || '朋友' }}</div>
        <p class="muted">{{ userStore.family?.familyName }}</p>
      </div>

      <div class="hero-actions">
        <button class="btn" type="button" @click="goFamily">家庭</button>
        <button class="btn ghost" type="button" @click="goMedicine">药品</button>
        <button class="btn ghost" type="button" @click="goHealth">健康</button>
        <!-- 仅管理员可见 -->
        <button v-if="userStore.isAdmin" class="btn admin" type="button" @click="showAdminModal = true">
          管理员
        </button>
      </div>

      <div class="hero-meta muted">
        <span>{{ ['管理员','普通成员','关怀成员'][userStore.member?.role] }}</span>
        <a class="switch-link" @click="switchMember">切换成员</a>
      </div>
    </section>

    <!-- 管理员密码验证弹窗 -->
    <div v-if="showAdminModal" class="modal-mask" @click.self="showAdminModal = false">
      <div class="modal card">
        <h3>验证管理员身份</h3>
        <p class="muted">请输入家庭账号密码</p>
        <input
          v-model="adminPassword"
          class="input"
          type="password"
          placeholder="家庭账号密码"
          @keyup.enter="confirmAdmin"
        />
        <p v-if="adminError" class="error">{{ adminError }}</p>
        <div class="modal-actions">
          <button class="btn ghost" @click="showAdminModal = false">取消</button>
          <button class="btn" :disabled="adminLoading" @click="confirmAdmin">
            {{ adminLoading ? '验证中...' : '确认' }}
          </button>
        </div>
      </div>
    </div>
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
  border: 1px solid var(--primary);
  color: var(--primary);
}
.btn.admin {
  background: #f0f4ff;
  color: #1a56db;
  border: 1px solid #c3d3f7;
}
.hero-meta {
  font-size: 0.9rem;
  display: flex;
  gap: 16px;
  align-items: center;
}
.switch-link {
  cursor: pointer;
  color: var(--primary);
  text-decoration: underline;
}
.modal-mask {
  position: fixed;
  inset: 0;
  background: rgba(0,0,0,0.4);
  display: grid;
  place-items: center;
  z-index: 100;
}
.modal {
  width: min(360px, 90vw);
  padding: 24px;
  display: grid;
  gap: 14px;
}
.modal-actions {
  display: flex;
  gap: 12px;
  justify-content: flex-end;
}
.error { color: #b42318; margin: 0; }
</style>
