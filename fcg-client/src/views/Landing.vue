<script setup>
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import AuthDialog from '../components/auth/AuthDialog.vue'

const router = useRouter()
const userStore = useUserStore()

const showAuth = ref(false)
const authTab = ref('login')

const primaryCtaText = computed(() => {
  if (!userStore.isLoggedIn) return '登录家庭账号'
  return userStore.hasMember ? '进入系统' : '选择成员'
})

const handlePrimaryCta = () => {
  if (!userStore.isLoggedIn) { openLogin(); return }
  if (userStore.hasMember) { router.push({ name: 'dashboard' }); return }
  router.push({ name: 'select-member' })
}

const openLogin = () => { authTab.value = 'login'; showAuth.value = true }
const openRegister = () => { authTab.value = 'register'; showAuth.value = true }
</script>

<template>
  <div class="page">
    <!-- 顶部光晕装饰 -->
    <div class="hero-glow" aria-hidden="true"></div>

    <!-- 导航 -->
    <header class="nav">
      <div class="nav-inner">
        <div class="brand">
          <img src="/fcg.png" alt="FCG" class="brand-logo" />
          <span class="brand-name">FCG</span>
          <span class="brand-full">Family Care Guardian</span>
        </div>
        <nav class="nav-links">
          <template v-if="!userStore.isLoggedIn">
            <button class="btn-ghost" @click="openLogin">登录</button>
            <button class="btn-primary" @click="openRegister">免费注册</button>
          </template>
          <template v-else>
            <button class="btn-primary" @click="handlePrimaryCta">{{ primaryCtaText }}</button>
          </template>
        </nav>
      </div>
    </header>

    <!-- Hero -->
    <section class="hero">
      <div class="hero-inner">
        <div class="hero-badge">家庭健康助手 · Family Care Guardian</div>
        <h1 class="hero-title">
          守护家人健康<br />
          <span class="hero-accent">从这一页开始</span>
        </h1>
        <p class="hero-desc">
          一套系统覆盖用药管理、健康追踪与家庭协同。<br class="br-pc" />
          让慢病管理与家庭健康，变得简单可靠。
        </p>
        <div class="hero-actions">
          <button class="btn-primary btn-lg" @click="handlePrimaryCta">{{ primaryCtaText }}</button>
          <button class="btn-outline btn-lg" @click="openRegister">了解更多</button>
        </div>

        <!-- 能力卡片 -->
        <div class="hero-stats">
          <div class="stat">
            <span class="stat-icon">👨‍👩‍👧</span>
            <div>
              <div class="stat-label">家庭协同</div>
              <div class="stat-sub">多成员管理</div>
            </div>
          </div>
          <div class="stat-divider"></div>
          <div class="stat">
            <span class="stat-icon">❤️</span>
            <div>
              <div class="stat-label">健康追踪</div>
              <div class="stat-sub">体征记录与周报</div>
            </div>
          </div>
          <div class="stat-divider"></div>
          <div class="stat">
            <span class="stat-icon">💊</span>
            <div>
              <div class="stat-label">用药管理</div>
              <div class="stat-sub">闭环提醒打卡</div>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- Features -->
    <section class="features">
      <div class="section-inner">
        <div class="section-tag">核心功能</div>
        <h2 class="section-title">一个系统，照顾全家</h2>
        <p class="section-desc">专为居家养老场景设计，聚焦用药安全与家庭协同</p>

        <div class="feature-grid">
          <div class="feature-card">
            <div class="feature-icon">👴</div>
            <h3>关怀模式</h3>
            <p>专为长辈设计大字体高对比界面，扁平化操作路径，一键切换，子女远程配置。</p>
          </div>
          <div class="feature-card">
            <div class="feature-icon">⏰</div>
            <h3>闭环用药提醒</h3>
            <p>定时推送服药提醒，一键打卡确认，漏服自动记录，杜绝忘药风险。</p>
          </div>
          <div class="feature-card">
            <div class="feature-icon">❤️</div>
            <h3>健康数据追踪</h3>
            <p>体征数据持续记录，自动生成周报，异常数据即时提醒家庭成员。</p>
          </div>
          <div class="feature-card">
            <div class="feature-icon">🏠</div>
            <h3>家庭权限体系</h3>
            <p>管理员、普通成员、关怀成员三级权限，清晰分工，子女随时掌握父母健康动态。</p>
          </div>
          <div class="feature-card">
            <div class="feature-icon">🔬</div>
            <h3>AI 药品识别</h3>
            <p>拍照即可自动识别药品信息，OCR + 大模型联合分析，生成通俗用药建议与禁忌提醒。</p>
          </div>
          <div class="feature-card">
            <div class="feature-icon">☁️</div>
            <h3>云端同步</h3>
            <p>数据安全存储，多设备实时同步，家庭成员随时随地查看健康状态。</p>
          </div>
        </div>
      </div>
    </section>

    <!-- Steps -->
    <section class="steps-section">
      <div class="section-inner">
        <div class="section-tag">快速开始</div>
        <h2 class="section-title">三步开始守护</h2>

        <div class="steps">
          <div class="step-line"></div>
          <div class="step">
            <div class="step-num">01</div>
            <div class="step-body">
              <h4>创建家庭账号</h4>
              <p>注册一个家庭账号，设置家庭名称，即可管理所有成员。</p>
            </div>
          </div>
          <div class="step">
            <div class="step-num">02</div>
            <div class="step-body">
              <h4>添加家庭成员</h4>
              <p>为每位家人配置身份、关系与关怀模式，权限清晰分配。</p>
            </div>
          </div>
          <div class="step">
            <div class="step-num">03</div>
            <div class="step-body">
              <h4>开始健康管理</h4>
              <p>录入药品、记录体征、跟踪用药，系统自动生成健康周报。</p>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- CTA Banner -->
    <section class="cta-section">
      <div class="section-inner">
        <div class="cta-card">
          <h2>现在开始守护家人健康</h2>
          <p>免费注册，5 分钟完成配置</p>
          <button class="btn-primary btn-lg" @click="openRegister">立即注册</button>
        </div>
      </div>
    </section>

    <!-- Footer -->
    <footer class="footer">
      <div class="footer-inner">
        <div class="footer-brand">
          <span class="brand-dot brand-dot-sm"></span>
          <span>FCG · Family Care Guardian</span>
        </div>
        <div class="footer-info">
          <span>© 2026 FCG · Family Care Guardian</span>
        </div>
      </div>
    </footer>

    <AuthDialog v-model="showAuth" :initial-tab="authTab" />
  </div>
</template>

<style scoped>
/* ─── 全局容器 ─── */
.page {
  min-height: 100vh;
  background: #ffffff;
  color: #0f0f0f;
  position: relative;
  overflow-x: hidden; /* 明确禁止横向滚动，防止手机端溢出 */
}

/* ─── 顶部光晕 ─── */
.hero-glow {
  position: absolute;
  top: -200px;
  left: 50%;
  transform: translateX(-50%);
  width: min(900px, 200vw);
  height: 600px;
  background: radial-gradient(ellipse at center, rgba(45, 95, 93, 0.1) 0%, transparent 70%);
  pointer-events: none;
  z-index: 0;
}

/* ─── 导航 ─── */
.nav {
  position: sticky;
  top: 0;
  z-index: 100;
  background: rgba(255, 255, 255, 0.88);
  backdrop-filter: blur(14px);
  -webkit-backdrop-filter: blur(14px);
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
}

.nav-inner {
  width: 100%;
  padding: 0 40px;
  height: 62px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.brand-logo {
  width: 30px;
  height: 30px;
  flex-shrink: 0;
  border-radius: 8px;
}

.brand {
  display: flex;
  align-items: center;
  gap: 8px;
}

.brand-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: #2d5f5d;
  flex-shrink: 0;
}

.brand-dot-sm {
  width: 8px;
  height: 8px;
}

.brand-name {
  font-weight: 800;
  font-size: 1rem;
  color: #2d5f5d;
  letter-spacing: 0.05em;
}

.brand-full {
  font-size: 0.83rem;
  color: #aaa;
  margin-left: 2px;
}

.nav-links {
  display: flex;
  align-items: center;
  gap: 8px;
}

/* ─── 按钮 ─── */
.btn-primary {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 9px 20px;
  background: #2d5f5d;
  color: #fff;
  border: none;
  border-radius: 8px;
  font-size: 0.9rem;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.2s, transform 0.15s;
}

.btn-primary:hover {
  background: #235250;
  transform: translateY(-1px);
}

.btn-ghost {
  display: inline-flex;
  align-items: center;
  padding: 9px 16px;
  background: transparent;
  color: #555;
  border: none;
  border-radius: 8px;
  font-size: 0.9rem;
  font-weight: 500;
  cursor: pointer;
  transition: color 0.15s, background 0.15s;
}

.btn-ghost:hover {
  color: #2d5f5d;
  background: rgba(45, 95, 93, 0.05);
}

.btn-outline {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 9px 20px;
  background: transparent;
  color: #2d5f5d;
  border: 1.5px solid rgba(45, 95, 93, 0.35);
  border-radius: 8px;
  font-size: 0.9rem;
  font-weight: 600;
  cursor: pointer;
  transition: border-color 0.2s, background 0.2s;
}

.btn-outline:hover {
  border-color: #2d5f5d;
  background: rgba(45, 95, 93, 0.04);
}

.btn-lg {
  padding: 12px 28px;
  font-size: 1rem;
  border-radius: 10px;
}

/* ─── Hero ─── */
.hero {
  position: relative;
  z-index: 1;
  padding: 108px 24px 88px;
}

.hero-inner {
  max-width: 700px;
  margin: 0 auto;
  text-align: center;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 22px;
}

.hero-badge {
  display: inline-flex;
  align-items: center;
  padding: 5px 14px;
  background: rgba(45, 95, 93, 0.07);
  border: 1px solid rgba(45, 95, 93, 0.15);
  border-radius: 999px;
  font-size: 0.78rem;
  color: #2d5f5d;
  font-weight: 600;
  letter-spacing: 0.03em;
}

.hero-title {
  margin: 0;
  font-size: clamp(2.6rem, 5.5vw, 4rem);
  font-weight: 800;
  line-height: 1.12;
  letter-spacing: -0.03em;
  color: #0a0a0a;
}

.hero-accent {
  background: linear-gradient(135deg, #2d5f5d 0%, #3c8a86 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.hero-desc {
  margin: 0;
  font-size: 1.1rem;
  color: #666;
  line-height: 1.75;
  max-width: 500px;
}

.br-pc { display: inline; }

.hero-actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  justify-content: center;
}

/* Hero 统计条 */
.hero-stats {
  margin-top: 16px;
  display: flex;
  align-items: center;
  gap: 24px;
  padding: 16px 32px;
  background: #f8f8f8;
  border: 1px solid #efefef;
  border-radius: 14px;
}

.stat {
  display: flex;
  align-items: center;
  gap: 10px;
}

.stat-icon { font-size: 1.4rem; }

.stat-label {
  font-size: 0.86rem;
  font-weight: 600;
  color: #111;
}

.stat-sub {
  font-size: 0.74rem;
  color: #aaa;
  margin-top: 1px;
}

.stat-divider {
  width: 1px;
  height: 32px;
  background: #e5e5e5;
  flex-shrink: 0;
}

/* ─── 通用 section ─── */
.section-inner {
  max-width: 1100px;
  margin: 0 auto;
  padding: 0 40px;
}

.section-tag {
  display: inline-flex;
  padding: 4px 12px;
  background: rgba(45, 95, 93, 0.07);
  border-radius: 999px;
  font-size: 0.75rem;
  font-weight: 600;
  color: #2d5f5d;
  letter-spacing: 0.05em;
  margin-bottom: 14px;
}

.section-title {
  margin: 0 0 10px;
  font-size: clamp(1.7rem, 3vw, 2.3rem);
  font-weight: 800;
  letter-spacing: -0.025em;
  color: #0a0a0a;
}

.section-desc {
  margin: 0 0 48px;
  font-size: 0.98rem;
  color: #888;
  line-height: 1.6;
}

/* ─── Features ─── */
.features {
  padding: 88px 0;
  background: #fafafa;
  border-top: 1px solid #f0f0f0;
  border-bottom: 1px solid #f0f0f0;
}

.feature-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}

.feature-card {
  padding: 26px 24px;
  background: #ffffff;
  border: 1px solid #ebebeb;
  border-radius: 14px;
  transition: box-shadow 0.2s, transform 0.2s;
}

.feature-card:hover {
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.06);
  transform: translateY(-2px);
}

.feature-icon {
  font-size: 1.6rem;
  margin-bottom: 14px;
  display: block;
}

.feature-card h3 {
  margin: 0 0 8px;
  font-size: 0.97rem;
  font-weight: 700;
  color: #0f0f0f;
}

.feature-card p {
  margin: 0;
  font-size: 0.86rem;
  color: #777;
  line-height: 1.68;
}

/* ─── Steps ─── */
.steps-section {
  padding: 88px 0;
}

.steps {
  position: relative;
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 0;
  margin-top: 8px;
}

.step-line {
  position: absolute;
  top: 24px;
  left: calc(16.67% + 24px);
  right: calc(16.67% + 24px);
  height: 1px;
  background: linear-gradient(90deg, #2d5f5d 0%, rgba(45, 95, 93, 0.15) 100%);
  z-index: 0;
}

.step {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 0 32px 0 0;
}

.step:last-child { padding-right: 0; }

.step-num {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  background: #2d5f5d;
  color: #fff;
  font-size: 0.82rem;
  font-weight: 800;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 20px;
  letter-spacing: 0.06em;
  flex-shrink: 0;
}

.step-body h4 {
  margin: 0 0 8px;
  font-size: 1rem;
  font-weight: 700;
  color: #0f0f0f;
}

.step-body p {
  margin: 0;
  font-size: 0.86rem;
  color: #777;
  line-height: 1.65;
}

/* ─── CTA ─── */
.cta-section {
  padding: 88px 0;
  background: #fafafa;
  border-top: 1px solid #f0f0f0;
}

.cta-card {
  max-width: 520px;
  margin: 0 auto;
  text-align: center;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 14px;
}

.cta-card h2 {
  margin: 0;
  font-size: clamp(1.5rem, 3vw, 2rem);
  font-weight: 800;
  letter-spacing: -0.025em;
  color: #0a0a0a;
}

.cta-card p {
  margin: 0;
  color: #888;
  font-size: 0.98rem;
}

/* ─── Footer ─── */
.footer {
  padding: 24px 0;
  border-top: 1px solid #f0f0f0;
}

.footer-inner {
  max-width: 1100px;
  margin: 0 auto;
  padding: 0 40px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 0.81rem;
  color: #bbb;
}

.footer-brand {
  display: flex;
  align-items: center;
  gap: 6px;
}

/* ─── 响应式 ─── */
@media (max-width: 900px) {
  .feature-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .steps {
    grid-template-columns: 1fr;
    gap: 32px;
  }

  .step-line { display: none; }
  .step { padding: 0; }
}

@media (max-width: 640px) {
  .nav-inner { padding: 0 20px; }
  .section-inner { padding: 0 20px; }
  .hero { padding: 68px 20px 56px; }

  .hero-stats {
    flex-direction: column;
    gap: 14px;
    align-items: flex-start;
    padding: 16px 20px;
  }

  .stat-divider {
    width: 100%;
    height: 1px;
  }

  .feature-grid {
    grid-template-columns: 1fr;
  }

  .brand-full { display: none; }
  .br-pc { display: none; }

  .footer-inner {
    flex-direction: column;
    gap: 6px;
    text-align: center;
  }
}
</style>
