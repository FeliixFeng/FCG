<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { fetchFamilyInfo, updateFamilyInfo, updateFamilyPassword } from '../../utils/api'
import { useUserStore } from '../../stores/user'

const userStore = useUserStore()

const loading = ref(false)
const initialized = ref(false)
const savingSettings = ref(false)
const savingPassword = ref(false)

const settingsForm = ref({
  familyName: '',
  lowStockThreshold: 5,
  expiringDays: 30
})

const passwordForm = ref({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

async function loadData() {
  loading.value = true
  try {
    const res = await fetchFamilyInfo()
    const family = res?.data || {}
    userStore.family = family
    settingsForm.value.familyName = family.familyName || ''
    settingsForm.value.lowStockThreshold = Number(family.lowStockThreshold || 5)
    settingsForm.value.expiringDays = Number(family.expiringDays || 30)
  } finally {
    loading.value = false
    initialized.value = true
  }
}

async function handleSaveSettings() {
  const familyName = String(settingsForm.value.familyName || '').trim()
  const lowStockThreshold = Number(settingsForm.value.lowStockThreshold || 0)
  const expiringDays = Number(settingsForm.value.expiringDays || 0)

  if (!familyName) {
    ElMessage.warning('请先输入家庭名称')
    return
  }
  if (familyName.length > 30) {
    ElMessage.warning('家庭名称最多 30 字')
    return
  }
  if (lowStockThreshold < 1 || lowStockThreshold > 999) {
    ElMessage.warning('低库存阈值范围：1-999')
    return
  }
  if (expiringDays < 1 || expiringDays > 365) {
    ElMessage.warning('临期提醒天数范围：1-365')
    return
  }

  savingSettings.value = true
  try {
    const res = await updateFamilyInfo({
      familyName,
      lowStockThreshold,
      expiringDays
    })
    userStore.family = res?.data || {
      ...userStore.family,
      familyName,
      lowStockThreshold,
      expiringDays
    }
    settingsForm.value.familyName = userStore.family?.familyName || familyName
    settingsForm.value.lowStockThreshold = Number(userStore.family?.lowStockThreshold || lowStockThreshold)
    settingsForm.value.expiringDays = Number(userStore.family?.expiringDays || expiringDays)
    ElMessage.success('系统设置已保存')
  } catch (err) {
    ElMessage.error(err?.message || '保存失败')
  } finally {
    savingSettings.value = false
  }
}

function resetPasswordForm() {
  passwordForm.value = {
    oldPassword: '',
    newPassword: '',
    confirmPassword: ''
  }
}

async function handleUpdatePassword() {
  const oldPassword = String(passwordForm.value.oldPassword || '')
  const newPassword = String(passwordForm.value.newPassword || '')
  const confirmPassword = String(passwordForm.value.confirmPassword || '')

  if (!oldPassword || !newPassword || !confirmPassword) {
    ElMessage.warning('请完整填写密码字段')
    return
  }
  if (newPassword.length < 6 || newPassword.length > 32) {
    ElMessage.warning('新密码长度需为 6-32 位')
    return
  }
  if (newPassword !== confirmPassword) {
    ElMessage.warning('两次输入的新密码不一致')
    return
  }

  savingPassword.value = true
  try {
    await updateFamilyPassword({ oldPassword, newPassword })
    ElMessage.success('密码修改成功，请使用新密码登录')
    resetPasswordForm()
  } catch (err) {
    ElMessage.error(err?.message || '密码修改失败')
  } finally {
    savingPassword.value = false
  }
}

onMounted(() => {
  loadData()
})
</script>

<template>
  <div class="admin-settings-page">
    <header class="page-header card">
      <div class="header-main">
        <div>
          <h2>系统设置</h2>
          <p class="page-desc">维护家庭核心参数，保存后立即影响首页与计划总览风险统计。</p>
        </div>
      </div>
      <p v-if="loading" class="loading-tip">数据加载中...</p>

      <div class="stats-row">
        <div class="stat-card">
          <span class="stat-head">🏠 家庭名称</span>
          <strong class="stat-value">{{ initialized ? (settingsForm.familyName || '--') : '--' }}</strong>
        </div>
        <div class="stat-card warn">
          <span class="stat-head">📦 低库存阈值</span>
          <strong class="stat-value">{{ initialized ? settingsForm.lowStockThreshold : '--' }}</strong>
          <span class="stat-sub">库存小于该值触发提醒</span>
        </div>
        <div class="stat-card warn">
          <span class="stat-head">⏱️ 临期提醒</span>
          <strong class="stat-value">{{ initialized ? settingsForm.expiringDays : '--' }} 天</strong>
          <span class="stat-sub">距离过期天数阈值</span>
        </div>
      </div>
    </header>

    <section class="card settings-card section-family">
      <div class="card-head">
        <h3>家庭与风险规则</h3>
        <span>保存后生效</span>
      </div>

      <div class="family-grid">
        <div class="field">
          <label>家庭名称</label>
          <input
            v-model.trim="settingsForm.familyName"
            maxlength="30"
            placeholder="请输入家庭名称"
            :disabled="loading || savingSettings"
            @keydown.enter.prevent="handleSaveSettings"
          />
        </div>
        <div class="field readonly">
          <label>家庭账号</label>
          <div class="readonly-value">{{ initialized ? (userStore.family?.username || '--') : '--' }}</div>
        </div>
        <div class="field readonly">
          <label>创建时间</label>
          <div class="readonly-value">{{ initialized ? (userStore.family?.createTime || '--') : '--' }}</div>
        </div>
        <div class="field">
          <label>低库存阈值（件）</label>
          <input
            v-model.number="settingsForm.lowStockThreshold"
            type="number"
            min="1"
            max="999"
            :disabled="loading || savingSettings"
            @keydown.enter.prevent="handleSaveSettings"
          />
        </div>
        <div class="field">
          <label>临期提醒天数（天）</label>
          <input
            v-model.number="settingsForm.expiringDays"
            type="number"
            min="1"
            max="365"
            :disabled="loading || savingSettings"
            @keydown.enter.prevent="handleSaveSettings"
          />
        </div>
      </div>

      <div class="actions-row">
        <button class="btn-main" :disabled="loading || savingSettings" @click="handleSaveSettings">
          {{ savingSettings ? '保存中...' : '保存设置' }}
        </button>
      </div>
    </section>

    <section class="card settings-card section-security">
      <div class="card-head">
        <h3>管理员安全</h3>
        <span>密码修改</span>
      </div>

      <div class="password-grid">
        <div class="field">
          <label>旧密码</label>
          <input
            v-model="passwordForm.oldPassword"
            type="password"
            placeholder="请输入旧密码"
            :disabled="savingPassword"
            @keydown.enter.prevent="handleUpdatePassword"
          />
        </div>
        <div class="field">
          <label>新密码</label>
          <input
            v-model="passwordForm.newPassword"
            type="password"
            placeholder="6-32 位"
            :disabled="savingPassword"
            @keydown.enter.prevent="handleUpdatePassword"
          />
        </div>
        <div class="field">
          <label>确认新密码</label>
          <input
            v-model="passwordForm.confirmPassword"
            type="password"
            placeholder="请再次输入新密码"
            :disabled="savingPassword"
            @keydown.enter.prevent="handleUpdatePassword"
          />
        </div>
      </div>

      <div class="actions-row">
        <button class="btn-main" :disabled="savingPassword" @click="handleUpdatePassword">
          {{ savingPassword ? '提交中...' : '修改密码' }}
        </button>
        <button class="btn-ghost" :disabled="savingPassword" @click="resetPasswordForm">清空</button>
      </div>
    </section>
  </div>
</template>

<style scoped>
.admin-settings-page {
  width: 100%;
  display: grid;
  gap: 16px;
}

.card {
  background: rgba(255, 255, 255, 0.96);
  border: 1px solid rgba(45, 95, 93, 0.12);
  border-radius: 14px;
  box-shadow: 0 10px 22px rgba(45, 95, 93, 0.08);
}

.page-header {
  padding: 14px;
  display: grid;
  gap: 12px;
}

.header-main {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 10px;
}

.page-header h2 {
  margin: 0;
  font-size: 1.4rem;
  color: #2d5f5d;
}

.page-desc {
  margin: 6px 0 0;
  color: #607775;
  font-size: 0.92rem;
}

.loading-tip {
  margin: 8px 0 0;
  color: #7f9492;
  font-size: 0.82rem;
}

.stats-row {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
}

.stat-card {
  border: 1px solid rgba(45, 95, 93, 0.14);
  border-radius: 12px;
  padding: 10px 11px;
  display: grid;
  gap: 5px;
  background: linear-gradient(145deg, rgba(245, 251, 250, 0.9), rgba(255, 255, 255, 0.98));
}

.stat-card.warn {
  background: linear-gradient(145deg, rgba(255, 248, 238, 0.92), rgba(255, 255, 255, 0.99));
  border-color: rgba(214, 137, 16, 0.25);
}

.stat-card.safe {
  background: linear-gradient(145deg, rgba(242, 252, 247, 0.9), rgba(255, 255, 255, 0.99));
  border-color: rgba(46, 168, 114, 0.25);
}

.stat-head {
  font-size: 0.78rem;
  color: #5f7b78;
}

.stat-value {
  font-size: 0.98rem;
  color: #1f4543;
  line-height: 1.2;
  word-break: break-all;
}

.stat-sub {
  font-size: 0.74rem;
  color: #7a8f8d;
}

.settings-card {
  padding: 15px;
  display: grid;
  gap: 12px;
  position: relative;
  overflow: hidden;
}

.settings-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 4px;
  background: linear-gradient(90deg, rgba(45, 95, 93, 0.42), rgba(45, 95, 93, 0.15));
}

.section-family {
  background: linear-gradient(155deg, rgba(244, 251, 250, 0.86), rgba(255, 255, 255, 0.98));
}

.section-security {
  background: linear-gradient(155deg, rgba(247, 250, 253, 0.88), rgba(255, 255, 255, 0.98));
}

.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.card-head h3 {
  margin: 0;
  font-size: 1.02rem;
  color: #2d5f5d;
}

.card-head span {
  font-size: 0.82rem;
  color: #68817f;
}

.family-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.field {
  display: grid;
  gap: 6px;
}

.field label {
  font-size: 0.82rem;
  color: #607876;
}

.field input,
.readonly-value {
  height: 38px;
  border-radius: 10px;
  border: 1px solid rgba(45, 95, 93, 0.2);
  padding: 0 12px;
  box-sizing: border-box;
  background: #fff;
  color: #2a4846;
  font-size: 0.88rem;
  transition: border-color 0.18s ease, box-shadow 0.2s ease, transform 0.16s ease;
}

.field input:focus {
  outline: none;
  border-color: rgba(45, 95, 93, 0.42);
  box-shadow: 0 0 0 3px rgba(45, 95, 93, 0.09);
}

.field input:hover {
  transform: translateY(-1px);
}

.readonly-value {
  display: flex;
  align-items: center;
  color: #5f7774;
  background: rgba(247, 251, 250, 0.9);
}

.actions-row {
  display: flex;
  gap: 10px;
  justify-content: flex-start;
}

.btn-main,
.btn-ghost {
  height: 38px;
  border: none;
  border-radius: 10px;
  padding: 0 14px;
  cursor: pointer;
  font-size: 0.88rem;
  transition: transform 0.18s ease, box-shadow 0.2s ease, background 0.2s ease;
}

.btn-main {
  background: #2d5f5d;
  color: #fff;
}

.btn-main:hover {
  background: #244c4a;
  transform: translateY(-1px);
  box-shadow: 0 8px 14px rgba(35, 74, 72, 0.25);
}

.btn-ghost {
  background: rgba(45, 95, 93, 0.09);
  color: #2d5f5d;
}

.btn-ghost:hover {
  transform: translateY(-1px);
  box-shadow: 0 7px 14px rgba(45, 95, 93, 0.14);
}

.btn-main:disabled,
.btn-ghost:disabled {
  opacity: 0.62;
  cursor: not-allowed;
  transform: none;
  box-shadow: none;
}

.password-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

@media (max-width: 900px) {
  .stats-row {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .family-grid,
  .password-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 767px) {
  .header-main {
    flex-direction: column;
    align-items: stretch;
  }

  .stats-row {
    grid-template-columns: 1fr;
  }

  .actions-row {
    display: grid;
    grid-template-columns: 1fr;
  }
}
</style>
