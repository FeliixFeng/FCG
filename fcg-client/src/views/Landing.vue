<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import AuthDialog from '../components/auth/AuthDialog.vue'


const router = useRouter()
const userStore = useUserStore()

const showAuth = ref(false)
const authTab = ref('login')
const isMobile = ref(false)


const primaryCtaText = computed(() => {
  if (!userStore.isLoggedIn) {
    return '登录家庭账号'
  }
  return userStore.hasMember ? '进入系统' : '选择成员'
})

const handlePrimaryCta = () => {
  if (!userStore.isLoggedIn) {
    openLogin()
    return
  }

  if (userStore.hasMember) {
    router.push({ name: 'dashboard' })
    return
  }

  router.push({ name: 'select-member' })
}

const openLogin = () => {
  authTab.value = 'login'
  showAuth.value = true
}

const openRegister = () => {
  authTab.value = 'register'
  showAuth.value = true
}

const updateIsMobile = () => {
  isMobile.value = window.innerWidth < 760
}

onMounted(() => {
  updateIsMobile()
  window.addEventListener('resize', updateIsMobile)
})

onUnmounted(() => {
  window.removeEventListener('resize', updateIsMobile)
})
</script>

<template>
  <div class="landing">
    <div class="bg-layer bg-layer-a"></div>
    <div class="bg-layer bg-layer-b"></div>
    <div class="bg-layer bg-layer-c"></div>
    <div class="bg-layer bg-layer-d"></div>

    <header class="nav">
      <div class="brand">
        <span class="brand-chip">FCG</span>
        <span class="brand-name">Family Care Guardian</span>
      </div>
      <div class="nav-actions">
        <el-button v-if="!userStore.isLoggedIn" plain @click="openLogin">登录</el-button>
        <el-button v-if="!userStore.isLoggedIn" type="primary" @click="openRegister">注册</el-button>
        <el-button v-else plain @click="handlePrimaryCta">{{ primaryCtaText }}</el-button>
      </div>
    </header>

    <main class="content">
      <section class="hero">
        <div class="hero-text">
          <h1>守护家人健康，从这一页开始</h1>
          <p>一套系统覆盖药品识别、健康记录、家庭成员管理与关怀模式，简单易用。</p>
          <div class="hero-actions">
            <el-button type="primary" size="large" @click="handlePrimaryCta">{{ primaryCtaText }}</el-button>
            <el-button plain size="large" @click="openRegister">了解注册流程</el-button>
          </div>
        </div>
        <div class="hero-card">
          <div class="hero-card-title">系统能力一览</div>
          <ul>
            <li>OCR + AI 智能识别药品信息</li>
            <li>家庭成员多身份切换与关怀模式</li>
            <li>健康数据按周汇总与提醒</li>
          </ul>
        </div>
      </section>

      <section class="feature-grid">
        <div class="feature-card">
          <h3>药品智能识别</h3>
          <p>拍照识别说明书，自动提取关键字段并生成用药建议。</p>
        </div>
        <div class="feature-card">
          <h3>健康数据管理</h3>
          <p>体征记录、周报总结与异常提醒，随时掌握健康趋势。</p>
        </div>
        <div class="feature-card">
          <h3>家庭协同</h3>
          <p>管理员、普通成员、关怀成员三种身份清晰分工。</p>
        </div>
      </section>

      <section class="steps">
        <div class="step">
          <span>01</span>
          <div>
            <h4>创建家庭账号</h4>
            <p>一个账号即可管理全家成员。</p>
          </div>
        </div>
        <div class="step">
          <span>02</span>
          <div>
            <h4>添加成员与关系</h4>
            <p>为每位家人配置身份与关怀模式。</p>
          </div>
        </div>
        <div class="step">
          <span>03</span>
          <div>
            <h4>开始健康管理</h4>
            <p>记录体征、跟踪用药、生成周报。</p>
          </div>
        </div>
      </section>
    </main>

    <footer class="footer">
      <div>毕业设计 · 家庭健康管理系统</div>
      <div>武汉纺织大学</div>
    </footer>

    <AuthDialog v-model="showAuth" :fullscreen="isMobile" :initial-tab="authTab" />
  </div>
</template>

<style scoped>
.landing {
  position: relative;
  min-height: 100vh;
  overflow: hidden;
  padding: 28px clamp(18px, 4vw, 56px) 56px;
  background:
    radial-gradient(124% 92% at 8% 4%, rgba(255, 255, 255, 0.84) 0%, transparent 55%),
    radial-gradient(98% 76% at 100% 8%, rgba(84, 127, 124, 0.22) 0%, transparent 60%),
    radial-gradient(88% 74% at 0% 100%, rgba(246, 228, 201, 0.68) 0%, transparent 64%),
    linear-gradient(134deg, #fbf8f1 0%, #f2e9d8 56%, #e6ede9 100%);
}

.landing::before {
  content: '';
  position: absolute;
  inset: 0;
  background-image:
    repeating-linear-gradient(135deg, rgba(69, 99, 97, 0.16) 0 1px, transparent 1px 30px),
    repeating-linear-gradient(45deg, rgba(255, 255, 255, 0.32) 0 1px, transparent 1px 30px);
  opacity: 0.56;
  -webkit-mask-image: radial-gradient(120% 94% at 50% 56%, #000 0%, rgba(0, 0, 0, 0.58) 66%, transparent 100%);
  mask-image: radial-gradient(120% 94% at 50% 56%, #000 0%, rgba(0, 0, 0, 0.58) 66%, transparent 100%);
  pointer-events: none;
}

.landing::after {
  content: '';
  position: absolute;
  inset: -8% -10% -12% -10%;
  background:
    linear-gradient(118deg, rgba(45, 95, 93, 0.16) 0%, rgba(45, 95, 93, 0) 34%),
    linear-gradient(304deg, rgba(45, 95, 93, 0.12) 0%, rgba(45, 95, 93, 0) 32%),
    linear-gradient(146deg, rgba(255, 255, 255, 0.36) 0%, rgba(255, 255, 255, 0) 32%),
    linear-gradient(338deg, rgba(238, 224, 198, 0.34) 0%, rgba(238, 224, 198, 0) 30%);
  opacity: 0.92;
  pointer-events: none;
}

.bg-layer {
  position: absolute;
  z-index: 0;
  pointer-events: none;
}

.bg-layer-a {
  width: min(44vw, 560px);
  height: min(30vw, 360px);
  left: -12%;
  top: -8%;
  clip-path: polygon(0 18%, 76% 0, 100% 44%, 60% 100%, 0 80%);
  background: linear-gradient(138deg, rgba(229, 243, 241, 0.72) 0%, rgba(77, 124, 121, 0.22) 100%);
  border: 1px solid rgba(255, 255, 255, 0.28);
  opacity: 0.88;
}

.bg-layer-b {
  width: min(48vw, 620px);
  height: min(36vw, 430px);
  right: -16%;
  bottom: -18%;
  clip-path: polygon(8% 0, 100% 24%, 92% 100%, 0 74%);
  background: linear-gradient(142deg, rgba(45, 95, 93, 0.44) 0%, rgba(45, 95, 93, 0.14) 100%);
  opacity: 0.62;
}

.bg-layer-c {
  width: min(24vw, 300px);
  height: min(20vw, 220px);
  right: 10%;
  top: 12%;
  clip-path: polygon(0 24%, 80% 0, 100% 66%, 18% 100%);
  background: linear-gradient(130deg, rgba(250, 244, 234, 0.9) 0%, rgba(45, 95, 93, 0.24) 100%);
  opacity: 0.68;
}

.bg-layer-d {
  width: min(30vw, 380px);
  height: min(22vw, 260px);
  left: -8%;
  bottom: -12%;
  clip-path: polygon(0 32%, 88% 0, 100% 86%, 12% 100%);
  background: linear-gradient(132deg, rgba(232, 202, 161, 0.44) 0%, rgba(255, 255, 255, 0.1) 100%);
  opacity: 0.58;
}

.nav {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 42px;
}

.brand {
  display: flex;
  align-items: center;
  gap: 12px;
  font-weight: 700;
}

.brand-chip {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 4px 11px;
  border-radius: 999px;
  border: 1px solid rgba(45, 95, 93, 0.23);
  background: rgba(45, 95, 93, 0.09);
  color: #2d5f5d;
  font-size: 0.76rem;
  letter-spacing: 0.08em;
}

.brand-name {
  font-size: 1rem;
  color: #2d2b26;
}

.nav-actions {
  display: flex;
  gap: 12px;
}

.content {
  position: relative;
  z-index: 1;
  display: grid;
  gap: 48px;
}

.hero {
  display: grid;
  grid-template-columns: minmax(0, 1.2fr) minmax(0, 0.8fr);
  gap: 32px;
  align-items: center;
}

.hero-text h1 {
  font-size: clamp(2rem, 3.6vw, 3rem);
  margin: 0 0 16px;
}

.hero-text p {
  margin: 0 0 20px;
  font-size: 1.05rem;
  color: var(--muted);
}

.hero-actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.hero-card {
  padding: 24px;
  border-radius: 20px;
  border: 1px solid rgba(255, 255, 255, 0.66);
  background: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(10px);
  box-shadow: 0 14px 40px rgba(26, 38, 37, 0.1);
}

.hero-card-title {
  font-weight: 700;
  margin-bottom: 12px;
}

.hero-card ul {
  margin: 0;
  padding-left: 18px;
  color: #534c43;
  display: grid;
  gap: 8px;
}

.feature-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 18px;
}

.feature-card {
  padding: 20px;
  border-radius: 18px;
  border: 1px solid rgba(255, 255, 255, 0.7);
  background: rgba(255, 255, 255, 0.76);
  box-shadow: 0 10px 30px rgba(26, 38, 37, 0.08);
}

.feature-card h3 {
  margin: 0 0 10px;
}

.feature-card p {
  margin: 0;
  color: #5a534a;
}

.steps {
  display: grid;
  gap: 16px;
}

.step {
  display: grid;
  grid-template-columns: auto 1fr;
  gap: 14px;
  align-items: start;
  padding: 18px 20px;
  border-radius: 18px;
  border: 1px solid rgba(255, 255, 255, 0.66);
  background: rgba(255, 255, 255, 0.8);
}

.step span {
  width: 38px;
  height: 38px;
  border-radius: 12px;
  display: grid;
  place-items: center;
  background: rgba(45, 95, 93, 0.12);
  color: #2d5f5d;
  font-weight: 700;
}

.step h4 {
  margin: 0 0 6px;
}

.step p {
  margin: 0;
  color: #5a534a;
}

.footer {
  position: relative;
  z-index: 1;
  display: flex;
  justify-content: space-between;
  margin-top: 46px;
  color: #6b6459;
}

@media (max-width: 980px) {
  .hero {
    grid-template-columns: 1fr;
  }

  .feature-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 760px) {
  .landing {
    padding: 20px 16px 40px;
  }

  .nav {
    flex-direction: column;
    gap: 16px;
    align-items: flex-start;
  }

  .nav-actions {
    width: 100%;
    justify-content: flex-start;
  }

  .footer {
    flex-direction: column;
    gap: 6px;
  }

  .bg-layer-a,
  .bg-layer-b {
    opacity: 0.36;
  }

  .bg-layer-c,
  .bg-layer-d {
    display: none;
  }
}
</style>
