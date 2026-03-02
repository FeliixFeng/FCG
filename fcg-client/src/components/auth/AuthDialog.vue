<script setup>
import { reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Lock, User } from '@element-plus/icons-vue'
import { useUserStore } from '../../stores/user'
import { registerFamily } from '../../utils/api'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  fullscreen: { type: Boolean, default: false },
  initialTab: { type: String, default: 'login' }
})

const emit = defineEmits(['update:modelValue', 'success'])

const router = useRouter()
const userStore = useUserStore()

const activeTab = ref(props.initialTab)

watch(() => props.initialTab, (newVal) => {
  activeTab.value = newVal
})

watch(() => props.modelValue, (newVal) => {
  if (newVal) {
    activeTab.value = 'login'
  }
})

const loginFormRef = ref()
const loginLoading = ref(false)
const loginForm = reactive({
  username: '',
  password: ''
})

const loginRules = {
  username: [
    { required: true, message: '请输入家庭账号', trigger: 'blur' },
    { min: 4, max: 20, message: '账号长度为 4-20 位', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度为 6-20 位', trigger: 'blur' }
  ]
}

const registerFormRef = ref()
const registerLoading = ref(false)
const registerForm = reactive({
  familyName: '',
  username: '',
  password: '',
  confirmPassword: '',
  adminNickname: ''
})

const validateConfirmPassword = (rule, value, callback) => {
  if (!value) {
    callback(new Error('请再次输入密码'))
    return
  }
  if (value !== registerForm.password) {
    callback(new Error('两次输入密码不一致'))
    return
  }
  callback()
}

const registerRules = {
  username: [
    { required: true, message: '请输入家庭账号', trigger: 'blur' },
    { min: 4, max: 20, message: '账号长度为 4-20 位', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度为 6-20 位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, validator: validateConfirmPassword, trigger: 'blur' }
  ],
  adminNickname: [
    { required: true, message: '请输入管理员昵称', trigger: 'blur' },
    { min: 2, max: 20, message: '昵称长度为 2-20 位', trigger: 'blur' }
  ],
  familyName: [
    { max: 30, message: '家庭名称最多 30 字', trigger: 'blur' }
  ]
}

const close = () => emit('update:modelValue', false)

const switchTab = (tab) => {
  activeTab.value = tab
}

const handleLogin = async () => {
  if (!loginFormRef.value) return
  try {
    await loginFormRef.value.validate()
  } catch {
    return
  }
  try {
    loginLoading.value = true
    await userStore.login({ ...loginForm })
    ElMessage.success('登录成功，请选择家庭成员')
    emit('success')
    close()
    router.replace({ name: 'select-member' })
  } catch (err) {
    ElMessage.error(err?.message || '登录失败，请稍后重试')
  } finally {
    loginLoading.value = false
  }
}

const handleRegister = async () => {
  if (!registerFormRef.value) return
  try {
    await registerFormRef.value.validate()
  } catch {
    return
  }
  try {
    registerLoading.value = true
    await registerFamily({
      familyName: registerForm.familyName,
      username: registerForm.username,
      password: registerForm.password,
      adminNickname: registerForm.adminNickname
    })
    ElMessage.success('注册成功，请登录')
    switchTab('login')
    loginForm.username = registerForm.username
    loginForm.password = ''
  } catch (err) {
    ElMessage.error(err?.message || '注册失败，请稍后重试')
  } finally {
    registerLoading.value = false
  }
}
</script>

<template>
  <el-dialog
    :model-value="modelValue"
    :fullscreen="fullscreen"
    width="90%"
    :style="{ maxWidth: '900px' }"
    :show-close="false"
    append-to-body
    destroy-on-close
    class="auth-dialog"
    @close="close"
  >
    <div class="auth-container">
      <div class="auth-decor">
        <div class="decor-blob decor-blob-1"></div>
        <div class="decor-blob decor-blob-2"></div>
        <div class="decor-blob decor-blob-3"></div>
        <div class="decor-content">
          <div class="brand-badge">FCG</div>
          <h2>Family Care Guardian</h2>
          <p>守护家人健康，从这一页开始</p>
          <div class="decor-features">
            <span>🏠 家庭协同</span>
            <span>💊 智能药箱</span>
            <span>❤️ 健康关怀</span>
          </div>
        </div>
      </div>

      <div class="auth-form-panel">
        <button class="close-btn" @click="close">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M18 6L6 18M6 6l12 12"/>
          </svg>
        </button>

        <div class="auth-tabs">
          <button class="tab-btn" :class="{ active: activeTab === 'login' }" @click="switchTab('login')">登录</button>
          <button class="tab-btn" :class="{ active: activeTab === 'register' }" @click="switchTab('register')">注册</button>
          <div class="tab-indicator" :class="{ register: activeTab === 'register' }"></div>
        </div>

        <div class="form-stack">
          <!-- Login Form -->
          <div class="form-page" :class="{ active: activeTab === 'login' }">
            <h3>欢迎回来</h3>
            <p class="form-subtitle">登录您的家庭账号</p>
            <el-form ref="loginFormRef" :model="loginForm" :rules="loginRules" label-position="top" status-icon @submit.prevent="handleLogin">
              <el-form-item label="家庭账号" prop="username">
                <el-input v-model.trim="loginForm.username" placeholder="请输入家庭账号" size="large" clearable>
                  <template #prefix><el-icon><User /></el-icon></template>
                </el-input>
              </el-form-item>
              <el-form-item label="密码" prop="password">
                <el-input v-model="loginForm.password" placeholder="请输入密码" size="large" show-password>
                  <template #prefix><el-icon><Lock /></el-icon></template>
                </el-input>
              </el-form-item>
              <el-form-item>
                <el-button type="primary" size="large" class="submit-btn" :loading="loginLoading" @click="handleLogin">登录</el-button>
              </el-form-item>
            </el-form>
          </div>

          <!-- Register Form -->
          <div class="form-page" :class="{ active: activeTab === 'register' }">
            <h3>创建家庭</h3>
            <p class="form-subtitle">注册您的第一个家庭账号</p>
            <el-form ref="registerFormRef" :model="registerForm" :rules="registerRules" label-position="top" status-icon @submit.prevent="handleRegister">
              <div class="form-grid">
                <el-form-item label="家庭账号" prop="username">
                  <el-input v-model.trim="registerForm.username" placeholder="4-20 位" size="large" clearable>
                    <template #prefix><el-icon><User /></el-icon></template>
                  </el-input>
                </el-form-item>
                <el-form-item label="管理员昵称" prop="adminNickname">
                  <el-input v-model.trim="registerForm.adminNickname" placeholder="例如：妈妈、张三" size="large" clearable />
                </el-form-item>
                <el-form-item label="密码" prop="password">
                  <el-input v-model="registerForm.password" placeholder="6-20 位" size="large" show-password>
                    <template #prefix><el-icon><Lock /></el-icon></template>
                  </el-input>
                </el-form-item>
                <el-form-item label="确认密码" prop="confirmPassword">
                  <el-input v-model="registerForm.confirmPassword" placeholder="请再次输入密码" size="large" show-password>
                    <template #prefix><el-icon><Lock /></el-icon></template>
                  </el-input>
                </el-form-item>
              </div>
              <el-form-item label="家庭名称（可选）" prop="familyName">
                <el-input v-model.trim="registerForm.familyName" placeholder="不填则系统自动生成" size="large" clearable />
              </el-form-item>
              <el-form-item>
                <el-button type="primary" size="large" class="submit-btn" :loading="registerLoading" @click="handleRegister">创建家庭</el-button>
              </el-form-item>
            </el-form>
          </div>
        </div>
      </div>
    </div>
  </el-dialog>
</template>

<style>
.auth-dialog .el-dialog__body { padding: 0; }
.auth-dialog .el-dialog { border-radius: 24px; overflow: hidden; }

.auth-container {
  display: flex;
  min-height: 520px;
}

.auth-decor {
  position: relative;
  width: 340px;
  min-height: 520px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(145deg, #2d5f5d 0%, #1e4240 100%);
  overflow: hidden;
}

.decor-blob {
  position: absolute;
  border-radius: 50%;
  filter: blur(40px);
  opacity: 0.6;
}

.decor-blob-1 { width: 200px; height: 200px; background: rgba(255, 255, 255, 0.15); top: -40px; left: -40px; }
.decor-blob-2 { width: 180px; height: 180px; background: rgba(236, 206, 163, 0.25); bottom: 20px; right: -30px; }
.decor-blob-3 { width: 120px; height: 120px; background: rgba(255, 255, 255, 0.1); top: 50%; left: 50%; transform: translate(-50%, -50%); }

.decor-content {
  position: relative;
  z-index: 1;
  text-align: center;
  color: #fff;
  padding: 24px;
}

.brand-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 8px 20px;
  background: rgba(255, 255, 255, 0.15);
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 999px;
  font-size: 0.85rem;
  font-weight: 700;
  letter-spacing: 0.1em;
  margin-bottom: 20px;
  backdrop-filter: blur(10px);
}

.decor-content h2 { margin: 0 0 12px; font-size: 1.5rem; font-weight: 700; }
.decor-content p { margin: 0 0 24px; font-size: 0.95rem; opacity: 0.85; }

.decor-features {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: center;
}

.decor-features span {
  padding: 6px 14px;
  background: rgba(255, 255, 255, 0.12);
  border-radius: 20px;
  font-size: 0.8rem;
  backdrop-filter: blur(8px);
}

.auth-form-panel {
  flex: 1;
  padding: 40px 48px;
  position: relative;
  background: linear-gradient(170deg, rgba(255, 255, 255, 0.98) 0%, rgba(251, 248, 241, 0.95) 100%);
}

.close-btn {
  position: absolute;
  top: 16px;
  right: 16px;
  width: 36px;
  height: 36px;
  border: none;
  background: rgba(45, 95, 93, 0.06);
  border-radius: 10px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #6b6459;
  transition: all 0.2s ease;
}

.close-btn:hover { background: rgba(45, 95, 93, 0.12); color: #2d5f5d; }

.auth-tabs {
  display: flex;
  position: relative;
  margin-bottom: 32px;
  border-bottom: 1px solid rgba(45, 95, 93, 0.1);
}

.tab-btn {
  flex: 1;
  padding: 12px 0;
  border: none;
  background: none;
  font-size: 1rem;
  font-weight: 600;
  color: #6b6459;
  cursor: pointer;
  transition: color 0.3s ease;
  position: relative;
  z-index: 1;
}

.tab-btn.active { color: #2d5f5d; }

.tab-indicator {
  position: absolute;
  bottom: -1px;
  left: 0;
  width: 50%;
  height: 3px;
  background: linear-gradient(90deg, #2d5f5d, #3c7a77);
  border-radius: 3px 3px 0 0;
  transition: transform 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.tab-indicator.register { transform: translateX(100%); }

/* Stack layout - both forms exist, positioned absolutely */
.form-stack {
  position: relative;
  min-height: 380px;
}

.form-page {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  opacity: 0;
  visibility: hidden;
  transition: opacity 0.2s ease, visibility 0.2s ease;
}

.form-page.active {
  opacity: 1;
  visibility: visible;
}

.form-page h3 { margin: 0 0 6px; font-size: 1.5rem; font-weight: 700; color: #2d2b26; }
.form-subtitle { margin: 0 0 24px; color: #6b6459; font-size: 0.95rem; }
.form-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 0 16px; }

.auth-form-panel .el-form-item__label { color: #4b443a; font-size: 0.9rem; font-weight: 500; }
.auth-form-panel .el-input__wrapper { min-height: 48px; border-radius: 12px; box-shadow: 0 0 0 1px #e5dfd2 inset; background: rgba(255, 255, 255, 0.9); transition: all 0.2s ease; }
.auth-form-panel .el-input__wrapper:hover { box-shadow: 0 0 0 1px #c9c2b5 inset; }
.auth-form-panel .el-input__wrapper.is-focus { box-shadow: 0 0 0 2px rgba(45, 95, 93, 0.2), 0 0 0 1px #2d5f5d inset; }
.auth-form-panel .el-input__inner { color: #2d2b26; }
.auth-form-panel .el-input__inner::placeholder { color: #a9a299; }

.submit-btn {
  width: 100%;
  height: 48px;
  border: none;
  border-radius: 12px;
  background: linear-gradient(135deg, #2d5f5d 0%, #3c7a77 100%);
  font-size: 1rem;
  font-weight: 600;
  box-shadow: 0 8px 20px rgba(45, 95, 93, 0.25);
  transition: all 0.2s ease;
}

.submit-btn:hover { transform: translateY(-2px); box-shadow: 0 12px 28px rgba(45, 95, 93, 0.3); }
.submit-btn:active { transform: translateY(0); }

@media (max-width: 768px) {
  .auth-dialog .el-dialog { width: 95% !important; max-width: none !important; margin: 16px auto !important; border-radius: 16px; }
  .auth-container { flex-direction: column; min-height: auto; }
  .auth-decor { width: 100%; min-height: 100px; padding: 16px; }
  .decor-blob { display: none; }
  .decor-content { display: flex; flex-direction: row; align-items: center; gap: 12px; padding: 0; }
  .decor-content h2, .decor-content p, .decor-features { display: none; }
  .brand-badge { margin-bottom: 0; padding: 6px 14px; font-size: 0.75rem; }
  .auth-form-panel { padding: 20px 16px; }
  .form-grid { grid-template-columns: 1fr; }
  .form-stack { min-height: 380px; }
  .auth-tabs { margin-bottom: 16px; }
  .tab-btn { padding: 8px 0; font-size: 0.9rem; }
  .form-page h3 { font-size: 1.2rem; margin-bottom: 4px; }
  .form-subtitle { margin-bottom: 12px; font-size: 0.85rem; }
  .close-btn { top: 10px; right: 10px; width: 32px; height: 32px; }
}
</style>
