<script setup>
import { reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Lock, User } from '@element-plus/icons-vue'
import { useUserStore } from '../../stores/user'
import { registerFamily } from '../../utils/api'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  initialTab: { type: String, default: 'login' }
})

const emit = defineEmits(['update:modelValue', 'success'])

const router = useRouter()
const userStore = useUserStore()

const activeTab = ref(props.initialTab)

watch(() => props.initialTab, (v) => { activeTab.value = v })
watch(() => props.modelValue, (v) => { if (v) activeTab.value = props.initialTab })

/* ─── Login ─── */
const loginFormRef = ref()
const loginLoading = ref(false)
const loginForm = reactive({ username: '', password: '' })
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

/* ─── Register ─── */
const registerFormRef = ref()
const registerLoading = ref(false)
const registerForm = reactive({ familyName: '', username: '', password: '', confirmPassword: '', adminNickname: '' })

const validateConfirmPassword = (rule, value, callback) => {
  if (!value) { callback(new Error('请再次输入密码')); return }
  if (value !== registerForm.password) { callback(new Error('两次密码不一致')); return }
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
  confirmPassword: [{ required: true, validator: validateConfirmPassword, trigger: 'blur' }],
  adminNickname: [
    { required: true, message: '请输入管理员昵称', trigger: 'blur' },
    { min: 2, max: 20, message: '昵称长度为 2-20 位', trigger: 'blur' }
  ],
  familyName: [{ max: 30, message: '家庭名称最多 30 字', trigger: 'blur' }]
}

/* ─── Actions ─── */
const close = () => emit('update:modelValue', false)
const switchTab = (tab) => { activeTab.value = tab }

const handleLogin = async () => {
  if (!loginFormRef.value) return
  try { await loginFormRef.value.validate() } catch { return }
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
  try { await registerFormRef.value.validate() } catch { return }
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
    width="860px"
    :show-close="false"
    append-to-body
    destroy-on-close
    class="auth-dlg"
    @close="close"
  >
    <div class="auth-wrap">
      <!-- 左侧品牌区 -->
      <div class="auth-side">
        <div class="side-content">
          <div class="side-logo"><img src="/fcg.png" alt="FCG" class="side-logo-img" /></div>
          <h2 class="side-title">Family Care<br />Guardian</h2>
          <p class="side-desc">守护家人健康，从这一页开始</p>
          <ul class="side-list">
            <li>
              <span class="side-icon">💊</span>
              <span>AI 智能药品识别</span>
            </li>
            <li>
              <span class="side-icon">👨‍👩‍👧</span>
              <span>家庭多成员协同</span>
            </li>
            <li>
              <span class="side-icon">📊</span>
              <span>健康数据追踪与周报</span>
            </li>
            <li>
              <span class="side-icon">👴</span>
              <span>老人关怀模式</span>
            </li>
          </ul>
        </div>
        <!-- 装饰圆 -->
        <div class="side-circle side-circle-1"></div>
        <div class="side-circle side-circle-2"></div>
      </div>

      <!-- 右侧表单区 -->
      <div class="auth-main">
        <!-- 关闭按钮 -->
        <button class="close-btn" @click="close" aria-label="关闭">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
            <path d="M18 6L6 18M6 6l12 12"/>
          </svg>
        </button>

        <!-- Tab -->
        <div class="tabs">
          <button class="tab" :class="{ active: activeTab === 'login' }" @click="switchTab('login')">登录</button>
          <button class="tab" :class="{ active: activeTab === 'register' }" @click="switchTab('register')">注册</button>
          <div class="tab-bar" :style="{ transform: activeTab === 'register' ? 'translateX(100%)' : 'translateX(0)' }"></div>
        </div>

        <!-- 表单容器：两个表单叠在一起，通过 opacity+visibility 切换 -->
        <div class="form-wrap">
          <!-- 登录 -->
          <div class="form-pane" :class="{ visible: activeTab === 'login' }">
            <div class="form-head">
              <h3>欢迎回来</h3>
              <p>登录您的家庭账号</p>
            </div>
            <el-form ref="loginFormRef" :model="loginForm" :rules="loginRules" label-position="top" @submit.prevent="handleLogin">
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
              <div class="form-footer-tip">
                还没有账号？<button type="button" class="link-btn" @click="switchTab('register')">免费注册</button>
              </div>
              <el-button type="primary" size="large" class="submit-btn" :loading="loginLoading" @click="handleLogin">
                登录
              </el-button>
            </el-form>
          </div>

          <!-- 注册 -->
          <div class="form-pane" :class="{ visible: activeTab === 'register' }">
            <div class="form-head">
              <h3>创建家庭</h3>
              <p>注册您的第一个家庭账号</p>
            </div>
            <el-form ref="registerFormRef" :model="registerForm" :rules="registerRules" label-position="top" @submit.prevent="handleRegister">
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
              <div class="form-footer-tip">
                已有账号？<button type="button" class="link-btn" @click="switchTab('login')">直接登录</button>
              </div>
              <el-button type="primary" size="large" class="submit-btn" :loading="registerLoading" @click="handleRegister">
                创建家庭
              </el-button>
            </el-form>
          </div>
        </div>
      </div>
    </div>
  </el-dialog>
</template>

<style>
/* 覆盖 Element Plus dialog 外层 */
.el-dialog.auth-dlg {
  border-radius: 16px;
  overflow: hidden;
  padding: 0;
}
.el-dialog.auth-dlg .el-dialog__header { display: none; }
.el-dialog.auth-dlg .el-dialog__body { padding: 0; }

@media (max-width: 700px) {
  .el-dialog.auth-dlg {
    width: 95vw !important;
    max-width: 440px !important;
    margin: 5vh auto !important;
    max-height: 88vh !important;
    overflow-y: auto !important;
  }
  /* overlay 容器顶对齐，避免居中导致超出底部 */
  .el-overlay-dialog:has(.auth-dlg) {
    display: flex;
    align-items: flex-start;
    justify-content: center;
    padding-top: 5vh;
  }
}
</style>

<style scoped>
/* ─── 整体容器 ─── */
.auth-wrap {
  display: flex;
  min-height: 540px;
}

/* ─── 左侧品牌区 ─── */
.auth-side {
  position: relative;
  width: 280px;
  flex-shrink: 0;
  background: linear-gradient(160deg, #2d5f5d 0%, #1a3d3b 100%);
  padding: 40px 32px;
  display: flex;
  align-items: flex-start;
  overflow: hidden;
}

.side-content {
  position: relative;
  z-index: 1;
}

.side-logo {
  margin-bottom: 24px;
}

.side-logo-img {
  width: 44px;
  height: 44px;
  border-radius: 10px;
  display: block;
}

.side-title {
  margin: 0 0 12px;
  font-size: 1.5rem;
  font-weight: 800;
  color: #fff;
  line-height: 1.2;
}

.side-desc {
  margin: 0 0 28px;
  font-size: 0.88rem;
  color: rgba(255, 255, 255, 0.65);
  line-height: 1.6;
}

.side-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.side-list li {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 0.85rem;
  color: rgba(255, 255, 255, 0.82);
}

.side-icon {
  font-size: 1rem;
  flex-shrink: 0;
}

/* 装饰圆 */
.side-circle {
  position: absolute;
  border-radius: 50%;
  opacity: 0.12;
  pointer-events: none;
}
.side-circle-1 {
  width: 260px;
  height: 260px;
  background: #fff;
  bottom: -60px;
  right: -80px;
}
.side-circle-2 {
  width: 140px;
  height: 140px;
  background: #fff;
  top: -30px;
  right: -20px;
}

/* ─── 右侧表单区 ─── */
.auth-main {
  flex: 1;
  position: relative;
  padding: 36px 40px 36px;
  background: #fff;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.close-btn {
  position: absolute;
  top: 14px;
  right: 14px;
  width: 32px;
  height: 32px;
  border: none;
  background: rgba(0, 0, 0, 0.05);
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  color: #888;
  transition: background 0.15s, color 0.15s;
}
.close-btn:hover { background: rgba(0, 0, 0, 0.1); color: #333; }

/* ─── Tabs ─── */
.tabs {
  display: flex;
  position: relative;
  border-bottom: 1px solid #f0f0f0;
  margin-bottom: 28px;
  width: fit-content;
  gap: 0;
}

.tab {
  padding: 10px 20px;
  border: none;
  background: none;
  font-size: 0.95rem;
  font-weight: 600;
  color: #aaa;
  cursor: pointer;
  transition: color 0.2s;
  position: relative;
  z-index: 1;
}

.tab.active { color: #2d5f5d; }

.tab-bar {
  position: absolute;
  bottom: -1px;
  left: 0;
  width: 50%;
  height: 2px;
  background: #2d5f5d;
  border-radius: 2px 2px 0 0;
  transition: transform 0.25s cubic-bezier(0.4, 0, 0.2, 1);
}

/* ─── 表单切换 ─── */
.form-wrap {
  position: relative;
  flex: 1;
}

.form-pane {
  position: absolute;
  inset: 0;
  opacity: 0;
  visibility: hidden;
  transition: opacity 0.2s ease, visibility 0.2s ease;
  overflow-y: auto;
}

.form-pane.visible {
  opacity: 1;
  visibility: visible;
  position: relative; /* 可见时脱出 absolute 让容器撑高 */
}

.form-head h3 {
  margin: 0 0 4px;
  font-size: 1.4rem;
  font-weight: 800;
  color: #0f0f0f;
}

.form-head p {
  margin: 0 0 24px;
  font-size: 0.88rem;
  color: #999;
}

.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0 16px;
}

.form-footer-tip {
  font-size: 0.82rem;
  color: #aaa;
  margin-bottom: 12px;
}

.link-btn {
  border: none;
  background: none;
  color: #2d5f5d;
  font-weight: 600;
  cursor: pointer;
  font-size: inherit;
  padding: 0;
  text-decoration: underline;
  text-underline-offset: 2px;
}

/* ─── 提交按钮 ─── */
.submit-btn {
  width: 100%;
  height: 46px;
  border-radius: 10px !important;
  font-size: 0.95rem !important;
  font-weight: 600 !important;
  background: #2d5f5d !important;
  border-color: #2d5f5d !important;
}

.submit-btn:hover {
  background: #235250 !important;
  border-color: #235250 !important;
}

/* ─── Element Plus 覆盖 ─── */
.auth-main :deep(.el-form-item__label) {
  font-size: 0.85rem;
  font-weight: 500;
  color: #555;
  padding-bottom: 4px;
}

.auth-main :deep(.el-input__wrapper) {
  border-radius: 8px;
  box-shadow: 0 0 0 1px #e8e8e8 inset;
  background: #fafafa;
}

.auth-main :deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px #ccc inset;
}

.auth-main :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 2px rgba(45, 95, 93, 0.15), 0 0 0 1px #2d5f5d inset;
  background: #fff;
}

/* ─── 手机端 ─── */
@media (max-width: 700px) {
  .auth-side {
    display: none;
  }

  .auth-main {
    padding: 28px 24px 24px;
  }

  .form-grid {
    grid-template-columns: 1fr;
  }

  .auth-wrap {
    min-height: unset;
  }
}
</style>
