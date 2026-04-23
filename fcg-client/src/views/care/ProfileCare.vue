<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import BaseLayout from '../../components/common/BaseLayout.vue'
import AvatarIcon from '../../components/common/AvatarIcon.vue'
import { useUserStore } from '../../stores/user'
import { fetchUserProfile, fetchVitalList } from '../../utils/api'

const router = useRouter()
const userStore = useUserStore()
const profileLoading = ref(false)
const healthProfile = ref(null)
const latestWeightRecord = ref(null)

const memberName = computed(() => userStore.member?.nickname || '成员')
const relation = computed(() => userStore.member?.relation || '家人')
const familyName = computed(() => userStore.family?.familyName || '我的家庭')
const currentUserId = computed(() => Number(userStore.member?.id || userStore.member?.userId || 0))
const roleText = computed(() => {
  const role = userStore.member?.role
  if (role === 0) return '管理员'
  if (role === 1) return '普通成员'
  if (role === 2) return '受控成员'
  return '家庭成员'
})

const formatAge = (birthday) => {
  if (!birthday) return '待录入'
  const date = new Date(birthday)
  if (Number.isNaN(date.getTime())) return '待录入'
  const today = new Date()
  let age = today.getFullYear() - date.getFullYear()
  const monthDiff = today.getMonth() - date.getMonth()
  const dayDiff = today.getDate() - date.getDate()
  if (monthDiff < 0 || (monthDiff === 0 && dayDiff < 0)) {
    age -= 1
  }
  if (age < 0) return '待录入'
  return `${age} 岁`
}

const profileRows = computed(() => {
  return [
    { label: '关系', value: relation.value },
    { label: '年龄', value: formatAge(healthProfile.value?.birthday) },
    { label: '身高', value: healthProfile.value?.height ? `${healthProfile.value.height} cm` : '待录入' },
    { label: 'BMI', value: bmiValue.value !== null ? `${bmiValue.value}` : '待计算' }
  ]
})

const diseaseText = computed(() => healthProfile.value?.disease || '暂无病史记录')
const allergyText = computed(() => healthProfile.value?.allergy || '暂无过敏记录')
const bmiValue = computed(() => {
  const h = Number(healthProfile.value?.height)
  const latest = latestWeightRecord.value?.value
  const w = latest != null && latest !== '' ? Number(latest) : Number(healthProfile.value?.weight)
  if (!Number.isFinite(h) || !Number.isFinite(w) || h <= 0 || w <= 0) return null
  const meter = h / 100
  if (!meter) return null
  return Number((w / (meter * meter)).toFixed(1))
})
const profileStatusText = computed(() => {
  const hasBirthday = Boolean(healthProfile.value?.birthday)
  const hasHeight = Number(healthProfile.value?.height) > 0
  if (hasBirthday && hasHeight) return '档案较完整'
  if (hasBirthday || hasHeight) return '档案待补充'
  return '档案未完善'
})

const switchMember = () => {
  userStore.exitMember()
  router.replace({ name: 'select-member' })
}

const logout = () => {
  userStore.logout()
  router.replace({ name: 'landing' })
}

const loadHealthProfile = async () => {
  if (!currentUserId.value) return
  profileLoading.value = true
  try {
    const res = await fetchUserProfile(currentUserId.value)
    healthProfile.value = res?.data || null
  } catch (err) {
    healthProfile.value = null
    ElMessage.warning('健康档案加载失败，可稍后重试')
  } finally {
    profileLoading.value = false
  }
}

const loadLatestWeight = async () => {
  latestWeightRecord.value = null
  if (!currentUserId.value) return
  try {
    const firstRes = await fetchVitalList({
      userId: currentUserId.value,
      type: 3,
      page: 1,
      size: 1
    })
    const total = firstRes?.data?.total || 0
    if (!total) return

    const lastRes = await fetchVitalList({
      userId: currentUserId.value,
      type: 3,
      page: total,
      size: 1
    })
    const lastRecord = lastRes?.data?.records?.[0]
    if (!lastRecord) return
    latestWeightRecord.value = {
      value: lastRecord.value,
      measureTime: lastRecord.measureTime
    }
  } catch (err) {
    console.error('加载最近体重失败', err)
  }
}

onMounted(() => {
  loadHealthProfile()
  loadLatestWeight()
})
</script>

<template>
  <BaseLayout>
    <div class="care-page">
      <section class="care-card hero">
        <p class="care-badge">关怀模式 · 我的</p>
        <div class="profile-head">
          <AvatarIcon
            :avatar="userStore.member?.avatar"
            :relation="userStore.member?.relation"
            :role="userStore.member?.role"
            :nickname="userStore.member?.nickname"
            :size="72"
          />
          <div>
            <h2 class="care-title">{{ memberName }}</h2>
            <p class="care-desc">{{ familyName }} · {{ relation }}</p>
          </div>
        </div>
      </section>

      <div class="content-grid">
        <section class="care-card info" v-loading="profileLoading">
          <h3 class="card-title"><span class="title-icon">🗂️</span>个人与健康档案</h3>
          <div class="info-grid">
            <div v-for="item in profileRows" :key="item.label" class="info-item">
              <p class="info-label">{{ item.label }}</p>
              <p class="info-value">{{ item.value }}</p>
            </div>
          </div>
          <div class="meta-list">
            <div class="meta-row">
              <span class="meta-key">病史</span>
              <span class="meta-value">{{ diseaseText }}</span>
            </div>
            <div class="meta-row">
              <span class="meta-key">过敏</span>
              <span class="meta-value">{{ allergyText }}</span>
            </div>
          </div>
        </section>

        <section class="care-card actions">
          <h3 class="card-title"><span class="title-icon">⚙️</span>常用操作</h3>
          <div class="actions-main">
            <div class="action-list">
              <button class="action-btn" @click="switchMember">切换成员</button>
              <button class="action-btn danger" @click="logout">退出登录</button>
            </div>
          </div>
          <div class="action-notice">
            <p class="notice-title"><span class="title-icon">💡</span>温馨提醒</p>
            <p class="notice-text">如感觉不适，请先完成打卡并及时联系家人。</p>
            <p class="notice-text">档案信息可由家庭管理员统一维护。</p>
          </div>
          <div class="actions-footer">
            <div class="status-grid">
              <div class="status-item">
                <p class="status-label"><span class="status-icon">📁</span>档案状态</p>
                <p class="status-value">{{ profileStatusText }}</p>
              </div>
              <div class="status-item">
                <p class="status-label"><span class="status-icon">👤</span>当前身份</p>
                <p class="status-value">{{ roleText }}</p>
              </div>
            </div>
            <p class="action-tip">如需新增计划或调整设置，请联系家庭管理员处理。</p>
          </div>
        </section>
      </div>
    </div>
  </BaseLayout>
</template>

<style scoped>
.care-page {
  width: 100%;
  max-width: none;
  margin: 0;
  padding: 0;
  min-height: calc(100dvh - 150px);
  display: grid;
  grid-template-rows: auto 1fr;
  gap: 14px;
}

.care-card {
  background: rgba(255, 255, 255, 0.92);
  border: 1px solid rgba(45, 95, 93, 0.12);
  border-radius: 18px;
  padding: 16px;
  box-shadow: 0 8px 24px rgba(45, 95, 93, 0.08);
}

.hero {
  min-height: 114px;
}

.profile-head {
  display: flex;
  gap: 14px;
  align-items: center;
}

.care-badge {
  display: inline-block;
  font-size: 0.9rem;
  color: #2d5f5d;
  background: rgba(45, 95, 93, 0.1);
  border-radius: 999px;
  padding: 4px 10px;
  margin: 0 0 12px;
}

.care-title {
  margin: 0 0 8px;
  font-size: 1.52rem;
  color: #1f3f3e;
}

.care-desc {
  margin: 0;
  font-size: 1.08rem;
  color: #385e5d;
}

.content-grid {
  display: grid;
  grid-template-columns: 1.2fr 0.8fr;
  min-height: 0;
  align-items: stretch;
  gap: 12px;
}

.card-title {
  margin: 0 0 12px;
  font-size: 1.06rem;
  color: #244a48;
  display: flex;
  align-items: center;
  gap: 6px;
}

.title-icon {
  font-size: 1rem;
  line-height: 1;
}

.info {
  display: grid;
  gap: 12px;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.info-item {
  border-radius: 12px;
  border: 1px solid rgba(45, 95, 93, 0.14);
  background: rgba(255, 255, 255, 0.85);
  padding: 10px 12px;
}

.info-label {
  margin: 0 0 6px;
  font-size: 0.9rem;
  color: #5f7a77;
}

.info-value {
  margin: 0;
  font-size: 1.08rem;
  color: #1f4341;
  font-weight: 600;
}

.meta-list {
  display: grid;
  gap: 8px;
}

.meta-row {
  display: grid;
  grid-template-columns: 64px 1fr;
  gap: 8px;
  align-items: start;
  border-radius: 12px;
  padding: 10px 12px;
  border: 1px solid rgba(45, 95, 93, 0.1);
  background: rgba(245, 250, 249, 0.9);
}

.meta-key {
  color: #56716f;
  font-size: 0.95rem;
}

.meta-value {
  color: #274b49;
  font-size: 1rem;
  line-height: 1.45;
  word-break: break-word;
}

.actions {
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-height: 100%;
}

.actions-main {
  display: grid;
  gap: 10px;
}

.action-list {
  display: grid;
  gap: 14px;
}

.action-notice {
  border-radius: 12px;
  border: 1px solid rgba(45, 95, 93, 0.12);
  background: rgba(245, 250, 249, 0.88);
  padding: 10px 12px;
}

.notice-title {
  margin: 0 0 6px;
  font-size: 0.92rem;
  color: #2b5553;
  font-weight: 700;
}

.notice-text {
  margin: 0;
  font-size: 0.9rem;
  color: #5f7a77;
  line-height: 1.45;
}

.notice-text + .notice-text {
  margin-top: 4px;
}

.actions-footer {
  margin-top: auto;
  display: grid;
  gap: 10px;
  padding-top: 12px;
  border-top: 1px solid rgba(45, 95, 93, 0.1);
}

.status-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
}

.status-item {
  border-radius: 10px;
  border: 1px solid rgba(45, 95, 93, 0.12);
  background: rgba(245, 250, 249, 0.86);
  padding: 8px 10px;
}

.status-label {
  margin: 0 0 4px;
  font-size: 0.84rem;
  color: #6a8683;
  display: flex;
  align-items: center;
  gap: 4px;
}

.status-icon {
  font-size: 0.9rem;
  line-height: 1;
}

.status-value {
  margin: 0;
  font-size: 0.95rem;
  color: #244b49;
  font-weight: 600;
}

.action-btn {
  height: 54px;
  border: 0;
  border-radius: 12px;
  background: #2d5f5d;
  color: #fff;
  font-size: 1.08rem;
  font-weight: 600;
  cursor: pointer;
  transition: transform 0.15s ease, box-shadow 0.15s ease;
}

.action-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 6px 14px rgba(45, 95, 93, 0.22);
}

.action-btn.danger {
  background: #b44b42;
}

.action-btn.danger:hover {
  box-shadow: 0 6px 14px rgba(180, 75, 66, 0.22);
}

.action-tip {
  margin: 4px 0 0;
  color: #54706d;
  font-size: 0.92rem;
  line-height: 1.45;
}

@media (max-width: 768px) {
  .care-page {
    min-height: calc(100dvh - 142px);
    gap: 10px;
  }

  .care-card {
    padding: 14px;
    border-radius: 16px;
  }

  .content-grid {
    grid-template-columns: 1fr;
    gap: 10px;
  }

  .care-title {
    font-size: 1.4rem;
  }

  .care-desc {
    font-size: 1rem;
  }

  .info-grid {
    grid-template-columns: 1fr;
    gap: 8px;
  }

  .status-grid {
    grid-template-columns: 1fr;
  }
}
</style>
