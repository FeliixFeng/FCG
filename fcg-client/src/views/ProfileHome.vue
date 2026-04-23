<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import BaseLayout from '../components/common/BaseLayout.vue'
import AvatarIcon from '../components/common/AvatarIcon.vue'
import AvatarPicker from '../components/common/AvatarPicker.vue'
import { fetchUserProfile, fetchVitalList, updateUserInfo, updateUserProfile } from '../utils/api'
import { ElMessage, ElMessageBox } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()

// 角色标签
const roleLabel = computed(() => {
  const role = userStore.member?.role
  if (role === 0) return '超级管理员'
  if (role === 1) return '普通成员'
  if (role === 2) return '受控成员'
  return '未知角色'
})

// 关怀模式状态
const careModeStatus = computed(() => {
  return userStore.isCareMode ? '已启用（强制）' : '未启用'
})
const isControlledMember = computed(() => userStore.member?.role === 2)

const showProfileEditModal = ref(false)
const savingProfile = ref(false)
const profileForm = reactive({
  nickname: '',
  phone: '',
  avatar: ''
})

const healthProfile = ref(null)
const loadingHealthProfile = ref(false)
const latestWeightRecord = ref(null)

const showHealthEditModal = ref(false)
const savingHealthProfile = ref(false)
const healthForm = reactive({
  birthday: '',
  height: '',
  weight: '',
  disease: '',
  allergy: '',
  remark: ''
})

const openProfileEdit = () => {
  profileForm.nickname = userStore.member?.nickname || ''
  profileForm.phone = userStore.member?.phone || ''
  profileForm.avatar = userStore.member?.avatar || ''
  showProfileEditModal.value = true
}

const closeProfileEdit = () => {
  showProfileEditModal.value = false
}

const handleSaveProfile = async () => {
  if (!profileForm.nickname.trim()) {
    ElMessage.warning('昵称不能为空')
    return
  }
  try {
    savingProfile.value = true
    await updateUserInfo({
      nickname: profileForm.nickname.trim(),
      phone: profileForm.phone?.trim() || '',
      avatar: profileForm.avatar || ''
    })
    await userStore.fetchProfile()
    ElMessage.success('个人资料已更新')
    showProfileEditModal.value = false
  } catch (err) {
    ElMessage.error(err?.message || '保存失败')
  } finally {
    savingProfile.value = false
  }
}

const ageText = computed(() => {
  const birthday = healthProfile.value?.birthday
  if (!birthday) return '未填写'
  const birth = new Date(birthday)
  const today = new Date()
  let age = today.getFullYear() - birth.getFullYear()
  const m = today.getMonth() - birth.getMonth()
  if (m < 0 || (m === 0 && today.getDate() < birth.getDate())) {
    age--
  }
  return age >= 0 ? `${age} 岁` : '未填写'
})

const displayHeight = computed(() => {
  const h = healthProfile.value?.height
  return h != null && h !== '' ? `${h} cm` : '未填写'
})

const displayWeight = computed(() => {
  const latest = latestWeightRecord.value?.value
  if (latest != null && latest !== '') {
    return `${latest} kg`
  }
  const fallback = healthProfile.value?.weight
  if (fallback != null && fallback !== '') {
    return `${fallback} kg`
  }
  return '未填写'
})

const weightSourceText = computed(() => {
  if (latestWeightRecord.value?.value != null && latestWeightRecord.value?.measureTime) {
    return `来源：最近体重记录（${formatDateTime(latestWeightRecord.value.measureTime)}）`
  }
  if (healthProfile.value?.weight != null && healthProfile.value?.weight !== '') {
    return '来源：个人档案体重'
  }
  return '暂无体重数据'
})

const displayDisease = computed(() => {
  const disease = healthProfile.value?.disease
  return disease && String(disease).trim() ? disease : '待录入'
})

const displayAllergy = computed(() => {
  const allergy = healthProfile.value?.allergy
  return allergy && String(allergy).trim() ? allergy : '待录入'
})

const maskedPhone = computed(() => {
  const phone = userStore.member?.phone
  if (!phone) return '待补充'
  const text = String(phone)
  if (text.length < 7) return text
  return `${text.slice(0, 3)}****${text.slice(-4)}`
})

const healthProfileStatus = computed(() => {
  const hasHeight = healthProfile.value?.height != null && healthProfile.value?.height !== ''
  const hasWeight = healthProfile.value?.weight != null && healthProfile.value?.weight !== ''
  return hasHeight && hasWeight ? '已补充' : '待完善'
})

const adminSummary = reactive({
  memberCount: 0,
  controlledCount: 0
})

const bmiValue = computed(() => {
  const h = Number(healthProfile.value?.height)
  const latest = latestWeightRecord.value?.value
  const w = latest != null && latest !== '' ? Number(latest) : Number(healthProfile.value?.weight)
  if (!Number.isFinite(h) || !Number.isFinite(w) || h <= 0 || w <= 0) return null
  const meter = h / 100
  if (!meter) return null
  return Number((w / (meter * meter)).toFixed(1))
})

const bmiLabel = computed(() => {
  const bmi = bmiValue.value
  if (bmi == null) return '待计算'
  if (bmi < 18.5) return '偏低'
  if (bmi < 24) return '正常'
  if (bmi < 28) return '偏高'
  return '肥胖'
})

const bmiClass = computed(() => {
  const bmi = bmiValue.value
  if (bmi == null) return 'bmi-pending'
  if (bmi < 18.5) return 'bmi-low'
  if (bmi < 24) return 'bmi-normal'
  if (bmi < 28) return 'bmi-high'
  return 'bmi-obese'
})

const openHealthEdit = () => {
  healthForm.birthday = healthProfile.value?.birthday || ''
  healthForm.height = healthProfile.value?.height ?? ''
  healthForm.weight = healthProfile.value?.weight ?? ''
  healthForm.disease = healthProfile.value?.disease || ''
  healthForm.allergy = healthProfile.value?.allergy || ''
  healthForm.remark = healthProfile.value?.remark || ''
  showHealthEditModal.value = true
}

const closeHealthEdit = () => {
  showHealthEditModal.value = false
}

const handleSaveHealthProfile = async () => {
  try {
    savingHealthProfile.value = true
    await updateUserProfile({
      birthday: healthForm.birthday || null,
      height: healthForm.height === '' ? null : Number(healthForm.height),
      weight: healthForm.weight === '' ? null : Number(healthForm.weight),
      disease: healthForm.disease?.trim() || '',
      allergy: healthForm.allergy?.trim() || '',
      remark: healthForm.remark?.trim() || ''
    })
    ElMessage.success('健康档案已更新')
    showHealthEditModal.value = false
    await loadHealthProfile()
  } catch (err) {
    ElMessage.error(err?.message || '保存失败')
  } finally {
    savingHealthProfile.value = false
  }
}

const formatDateTime = (value) => {
  if (!value) return ''
  const text = String(value).replace('T', ' ')
  return text.length > 16 ? text.slice(0, 16) : text
}

const loadLatestWeight = async () => {
  latestWeightRecord.value = null
  const userId = userStore.member?.id
  if (!userId) return
  try {
    const firstRes = await fetchVitalList({
      userId,
      type: 3,
      page: 1,
      size: 1
    })
    const total = firstRes?.data?.total || 0
    if (!total) return

    const lastRes = await fetchVitalList({
      userId,
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
  } catch (e) {
    console.error('加载最近体重失败', e)
  }
}

const loadHealthProfile = async () => {
  const userId = userStore.member?.id
  if (!userId) return
  loadingHealthProfile.value = true
  try {
    const res = await fetchUserProfile(userId)
    healthProfile.value = res.data || {}
  } catch (e) {
    console.error('加载健康档案失败', e)
    healthProfile.value = {}
  } finally {
    loadingHealthProfile.value = false
  }
}

// 切换成员
const handleSwitchMember = () => {
  ElMessageBox.confirm(
    '确定要切换成员吗？当前页面将返回成员选择页。',
    '切换成员',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(() => {
    userStore.exitMember()
    router.push({ name: 'select-member' })
    ElMessage.success('已退出当前成员')
  }).catch(() => {
    // 用户取消
  })
}

// 退出登录
const handleLogout = () => {
  ElMessageBox.confirm(
    '确定要退出登录吗？',
    '退出登录',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(() => {
    userStore.logout()
    router.push({ name: 'landing' })
    ElMessage.success('已退出登录')
  }).catch(() => {
    // 用户取消
  })
}

// 进入管理界面（仅管理员）
const handleEnterAdmin = () => {
  router.push({ name: 'admin' })
}

onMounted(async () => {
  if (userStore.isAdmin) {
    try {
      const members = await userStore.fetchMembers()
      const list = Array.isArray(members) ? members : []
      adminSummary.memberCount = list.length
      adminSummary.controlledCount = list.filter(m => m.role === 2).length
    } catch (e) {
      console.error('加载管理概览失败', e)
    }
  }
  await loadHealthProfile()
  await loadLatestWeight()
})
</script>

<template>
  <BaseLayout>
    <section class="profile-container">
      <div class="profile-grid">
        <div class="card profile-card">
          <div class="card-title">👤 我的资料</div>
          <div class="profile-hero"></div>
          <AvatarIcon
            class="avatar"
            :class="{ 'care-mode': userStore.isCareMode }"
            :avatar="userStore.member?.avatar"
            :relation="userStore.member?.relation"
            :role="userStore.member?.role"
            :nickname="userStore.member?.nickname"
            :size="72"
          />
          <h2 class="nickname">{{ userStore.member?.nickname || '未命名' }}</h2>
          <div class="meta">
            <span class="role-badge" :class="`role-${userStore.member?.role}`">
              {{ roleLabel }}
            </span>
            <span class="relation">{{ userStore.member?.relation || '未设置关系' }}</span>
          </div>
          <div class="profile-extra-grid">
            <div class="extra-item">
              <span class="extra-label">家庭</span>
              <span class="extra-value">{{ userStore.family?.familyName || '未命名' }}</span>
            </div>
            <div class="extra-item">
              <span class="extra-label">手机号</span>
              <span class="extra-value">{{ maskedPhone }}</span>
            </div>
            <div class="extra-item">
              <span class="extra-label">档案状态</span>
              <span class="extra-value">{{ healthProfileStatus }}</span>
            </div>
            <div class="extra-item">
              <span class="extra-label">权限</span>
              <span class="extra-value">{{ roleLabel }}</span>
            </div>
          </div>
          <div v-if="isControlledMember" class="info-item">
            <span class="label">关怀模式</span>
            <span class="value">{{ careModeStatus }}</span>
          </div>
          <button class="profile-edit-btn" @click="openProfileEdit">编辑头像与昵称</button>
        </div>

        <div class="card health-summary-card">
          <div class="health-summary-head">
            <h3>❤️ 健康摘要</h3>
            <button class="profile-edit-btn slim" @click="openHealthEdit">编辑健康档案</button>
          </div>
          <div v-if="loadingHealthProfile" class="summary-loading">加载中...</div>
          <div v-else class="summary-grid">
            <div class="summary-item">
              <div class="summary-label">年龄</div>
              <div class="summary-value">{{ ageText }}</div>
            </div>
            <div class="summary-item">
              <div class="summary-label">身高</div>
              <div class="summary-value">{{ displayHeight }}</div>
            </div>
            <div class="summary-item">
              <div class="summary-label">当前体重</div>
              <div class="summary-value">{{ displayWeight }}</div>
              <div class="summary-tip">{{ weightSourceText }}</div>
            </div>
            <div class="summary-item">
              <div class="summary-label">BMI</div>
              <div class="summary-value">
                <template v-if="bmiValue !== null">{{ bmiValue }}</template>
                <template v-else>待补充</template>
                <span class="bmi-tag" :class="bmiClass">{{ bmiLabel }}</span>
              </div>
              <div v-if="bmiValue === null" class="summary-tip">补充身高与体重后可计算 BMI</div>
            </div>
            <div class="summary-item">
              <div class="summary-label">病史</div>
              <div class="summary-value summary-text">{{ displayDisease }}</div>
            </div>
            <div class="summary-item">
              <div class="summary-label">过敏史</div>
              <div class="summary-value summary-text">{{ displayAllergy }}</div>
            </div>
          </div>
        </div>

        <div class="card actions-card">
          <div class="card-title">⚙️ 常用操作</div>
          <button @click="handleSwitchMember" class="action-btn">
            <svg class="icon" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M16 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/>
              <circle cx="8.5" cy="7" r="4"/>
              <polyline points="17 11 19 13 23 9"/>
            </svg>
            <span>切换成员</span>
          </button>

          <button @click="handleLogout" class="action-btn danger">
            <svg class="icon" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/>
              <polyline points="16 17 21 12 16 7"/>
              <line x1="21" y1="12" x2="9" y2="12"/>
            </svg>
            <span>退出登录</span>
          </button>

          <div class="actions-family-row" v-if="userStore.family">
            <span class="actions-family-label">当前家庭</span>
            <span class="actions-family-value">{{ userStore.family.familyName || '未命名家庭' }}</span>
          </div>
        </div>

        <div v-if="userStore.isAdmin" class="card admin-entrance">
          <div class="card-title">🛡️ 管理入口</div>
          <div class="admin-summary-grid">
            <div class="admin-summary-item">
              <span class="sum-label">成员总数</span>
              <span class="sum-value">{{ adminSummary.memberCount }}</span>
            </div>
            <div class="admin-summary-item">
              <span class="sum-label">受控成员</span>
              <span class="sum-value">{{ adminSummary.controlledCount }}</span>
            </div>
          </div>
          <div class="admin-tip">管理员可统一维护成员资料、角色和关怀对象</div>
          <button @click="handleEnterAdmin" class="admin-btn">
            <svg class="icon" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M12 6v6m0 0v6m0-6h6m-6 0H6"/>
              <rect x="3" y="3" width="18" height="18" rx="2"/>
            </svg>
            <span>进入管理页面</span>
            <svg class="arrow" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M5 12h14m-7-7 7 7-7 7"/>
            </svg>
          </button>
        </div>
      </div>

      <div v-if="showProfileEditModal" class="modal-mask" @click.self="closeProfileEdit">
        <div class="modal-content">
          <div class="modal-header">
            <h3>编辑个人资料</h3>
            <button class="btn-close" @click="closeProfileEdit">&times;</button>
          </div>
          <div class="modal-body">
            <div class="form-field">
              <label>头像</label>
              <AvatarPicker
                v-model="profileForm.avatar"
                :current-relation="userStore.member?.relation"
                :current-role="userStore.member?.role"
              />
            </div>
            <div class="form-field">
              <label>昵称 *</label>
              <input v-model="profileForm.nickname" maxlength="20" placeholder="请输入昵称" />
            </div>
            <div class="form-field">
              <label>手机号</label>
              <input v-model="profileForm.phone" maxlength="20" placeholder="选填" />
            </div>
          </div>
          <div class="modal-footer">
            <button class="btn-cancel" @click="closeProfileEdit">取消</button>
            <button class="btn-submit" :disabled="savingProfile" @click="handleSaveProfile">
              {{ savingProfile ? '保存中...' : '保存' }}
            </button>
          </div>
        </div>
      </div>

      <div v-if="showHealthEditModal" class="modal-mask" @click.self="closeHealthEdit">
        <div class="modal-content">
          <div class="modal-header">
            <h3>编辑健康档案</h3>
            <button class="btn-close" @click="closeHealthEdit">&times;</button>
          </div>
          <div class="modal-body">
            <div class="form-field">
              <label>出生日期</label>
              <input type="date" v-model="healthForm.birthday" />
            </div>
            <div class="form-row">
              <div class="form-field">
                <label>身高 (cm)</label>
                <input type="number" v-model="healthForm.height" placeholder="如 170" />
              </div>
              <div class="form-field">
                <label>档案体重 (kg)</label>
                <input type="number" v-model="healthForm.weight" placeholder="如 65" />
              </div>
            </div>
            <div class="form-field">
              <label>病史</label>
              <input v-model="healthForm.disease" placeholder="如：高血压、糖尿病" />
            </div>
            <div class="form-field">
              <label>过敏史</label>
              <input v-model="healthForm.allergy" placeholder="如：青霉素" />
            </div>
            <div class="form-field">
              <label>备注</label>
              <textarea v-model="healthForm.remark" rows="3" placeholder="可填写长期注意事项"></textarea>
            </div>
          </div>
          <div class="modal-footer">
            <button class="btn-cancel" @click="closeHealthEdit">取消</button>
            <button class="btn-submit" :disabled="savingHealthProfile" @click="handleSaveHealthProfile">
              {{ savingHealthProfile ? '保存中...' : '保存' }}
            </button>
          </div>
        </div>
      </div>
    </section>
  </BaseLayout>
</template>

<style scoped>
.profile-container {
  width: 100%;
  max-width: none;
  margin: 0 auto;
  padding: 0 0 8px;
}

.profile-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 20px;
  align-items: stretch;
}

.card-title {
  align-self: stretch;
  margin-bottom: 4px;
  font-size: 14px;
  font-weight: 600;
  color: #2d5f5d;
  text-align: left;
  letter-spacing: 0.2px;
}

/* 成员信息卡片 */
.profile-card {
  padding: 20px 20px 18px;
  text-align: center;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  border: 1px solid rgba(45, 95, 93, 0.1);
  box-shadow: 0 12px 32px rgba(45, 95, 93, 0.08);
  min-height: 260px;
  height: 100%;
  position: relative;
  overflow: hidden;
}

.avatar {
  flex-shrink: 0;
  border-radius: 50%;
  box-shadow: 0 8px 20px rgba(45, 95, 93, 0.16);
  border: 3px solid rgba(255, 255, 255, 0.9);
  z-index: 1;
}

.avatar.care-mode {
  transform: scale(1.25);
}

.nickname {
  font-size: 20px;
  font-weight: 600;
  color: #2d5f5d;
  margin: 0;
}

.profile-hero {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 120px;
  background: linear-gradient(180deg, rgba(45, 95, 93, 0.08) 0%, rgba(45, 95, 93, 0) 100%);
  pointer-events: none;
}

.meta {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  justify-content: center;
}

.role-badge {
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 13px;
  font-weight: 500;
}

.role-badge.role-0 {
  background: #fef3e7;
  color: #d68910;
}

.role-badge.role-1 {
  background: #e8f4f8;
  color: #2874a6;
}

.role-badge.role-2 {
  background: #fdecea;
  color: #c0392b;
}

.relation {
  color: #666;
  font-size: 14px;
}

.info-item {
  width: 100%;
  display: flex;
  justify-content: space-between;
  padding: 10px 14px;
  background: #f8f9fa;
  border-radius: 8px;
  font-size: 14px;
}

.info-item .label {
  color: #666;
}

.info-item .value {
  color: #2d5f5d;
  font-weight: 500;
}

.profile-edit-btn {
  padding: 10px 16px;
  border: 1px solid #2d5f5d;
  background: #f3faf9;
  color: #2d5f5d;
  border-radius: 8px;
  font-size: 14px;
  cursor: pointer;
}

.profile-edit-btn:hover {
  background: #e8f4f2;
}

.profile-edit-btn.slim {
  padding: 8px 12px;
  font-size: 13px;
}

.profile-extra-grid {
  width: 100%;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
}

.extra-item {
  border: 1px solid #e2ebe9;
  border-radius: 10px;
  background: #fafdfc;
  padding: 8px 10px;
  display: flex;
  flex-direction: column;
  gap: 3px;
  text-align: left;
}

.extra-label {
  font-size: 12px;
  color: #7a8b89;
}

.extra-value {
  font-size: 13px;
  color: #2d5f5d;
  font-weight: 600;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* 管理入口卡片 */
.admin-entrance {
  padding: 18px;
  background: linear-gradient(135deg, #f5fbfa 0%, #ffffff 100%);
  border: 1px solid rgba(45, 95, 93, 0.22);
  box-shadow: 0 10px 24px rgba(45, 95, 93, 0.08);
  min-height: 260px;
  height: 100%;
}

.admin-summary-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
  margin-bottom: 10px;
}

.admin-summary-item {
  border: 1px solid #dbe8e6;
  border-radius: 10px;
  background: #f8fcfb;
  padding: 10px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.sum-label {
  font-size: 12px;
  color: #6f8280;
}

.sum-value {
  font-size: 20px;
  line-height: 1;
  color: #2d5f5d;
  font-weight: 700;
}

.admin-tip {
  font-size: 13px;
  color: #6f7f7d;
  margin-bottom: 12px;
}

.admin-btn {
  width: 100%;
  padding: 13px 16px;
  background: #2d5f5d;
  color: white;
  border: none;
  border-radius: 12px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  transition: all 0.3s ease;
}

.admin-btn:hover {
  background: #3a7775;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(45, 95, 93, 0.3);
}

.admin-btn .icon {
  flex-shrink: 0;
}

.admin-btn .arrow {
  margin-left: auto;
  flex-shrink: 0;
}

/* 功能列表卡片 */
.actions-card {
  padding: 18px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  border: 1px solid rgba(45, 95, 93, 0.12);
  box-shadow: 0 10px 24px rgba(45, 95, 93, 0.07);
  min-height: 260px;
  height: 100%;
}

.action-btn {
  width: 100%;
  padding: 16px 20px;
  background: white;
  color: #2d5f5d;
  border: 1px solid #ddd;
  border-radius: 8px;
  font-size: 15px;
  font-weight: 500;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 12px;
  transition: all 0.2s ease;
}

.action-btn:hover {
  border-color: #2d5f5d;
  background: #f8f9fa;
}

.action-btn.danger {
  color: #e74c3c;
  border-color: #f8d7da;
}

.action-btn.danger:hover {
  border-color: #e74c3c;
  background: #fef5f5;
}

.action-btn .icon {
  flex-shrink: 0;
}

/* 家庭信息卡片 */
.family-info {
  padding: 18px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  border: 1px solid rgba(45, 95, 93, 0.1);
  box-shadow: 0 10px 24px rgba(45, 95, 93, 0.06);
}

.family-info h3 {
  margin: 0 0 8px 0;
  font-size: 16px;
  color: #2d5f5d;
  font-weight: 600;
}

.info-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 0;
  border-bottom: 1px solid #f0f0f0;
  font-size: 14px;
}

.info-row:last-child {
  border-bottom: none;
}

.info-row .label {
  color: #666;
}

.info-row .value {
  color: #333;
  font-weight: 500;
}

.health-summary-card {
  padding: 18px;
  border: 1px solid rgba(45, 95, 93, 0.1);
  box-shadow: 0 10px 24px rgba(45, 95, 93, 0.06);
  min-height: 260px;
  height: 100%;
}

.health-summary-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 12px;
}

.health-summary-head h3 {
  margin: 0;
  font-size: 16px;
  color: #2d5f5d;
}

.summary-loading {
  padding: 16px 0;
  color: #777;
  font-size: 14px;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.summary-item {
  padding: 10px;
  border: 1px solid #ebefee;
  border-radius: 10px;
  background: #fafcfc;
}

.summary-label {
  font-size: 12px;
  color: #7a8584;
  margin-bottom: 6px;
}

.summary-value {
  font-size: 14px;
  color: #2d5f5d;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.summary-text {
  font-weight: 500;
  line-height: 1.45;
}

.summary-tip {
  margin-top: 5px;
  font-size: 12px;
  color: #8a9392;
}

.actions-family-row {
  margin-top: auto;
  border-top: 1px dashed #d9e4e3;
  padding-top: 10px;
  display: flex;
  justify-content: space-between;
  gap: 10px;
  font-size: 13px;
}

.actions-family-label {
  color: #7a8584;
}

.actions-family-value {
  color: #2d5f5d;
  font-weight: 600;
}


.bmi-tag {
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 10px;
  font-weight: 500;
}

.bmi-pending {
  color: #6d7473;
  background: #edf1f0;
}

.bmi-low {
  color: #2b6cb0;
  background: #e8f2fc;
}

.bmi-normal {
  color: #2f855a;
  background: #e8f6ef;
}

.bmi-high {
  color: #b7791f;
  background: #fdf2e2;
}

.bmi-obese {
  color: #c53030;
  background: #fdecec;
}

.modal-mask {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 2500;
  padding: 16px;
}

.modal-content {
  width: min(560px, 100%);
  max-height: 90vh;
  overflow: auto;
  background: #fff;
  border-radius: 14px;
  box-shadow: 0 16px 40px rgba(0, 0, 0, 0.2);
}

.modal-header {
  padding: 16px 18px;
  border-bottom: 1px solid #f0f0f0;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.modal-header h3 {
  margin: 0;
  font-size: 16px;
  color: #2d5f5d;
}

.btn-close {
  border: none;
  background: transparent;
  font-size: 22px;
  line-height: 1;
  cursor: pointer;
  color: #888;
}

.modal-body {
  padding: 16px 18px;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.form-field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.form-field label {
  font-size: 13px;
  color: #666;
}

.form-field input {
  border: 1px solid #dedede;
  border-radius: 8px;
  padding: 10px 12px;
  font-size: 14px;
}

.form-field textarea {
  border: 1px solid #dedede;
  border-radius: 8px;
  padding: 10px 12px;
  font-size: 14px;
  resize: vertical;
}

.form-field input:focus {
  outline: none;
  border-color: #2d5f5d;
}

.form-field textarea:focus {
  outline: none;
  border-color: #2d5f5d;
}

.form-row {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.modal-footer {
  padding: 14px 18px;
  border-top: 1px solid #f0f0f0;
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.btn-cancel {
  padding: 8px 14px;
  border: none;
  border-radius: 8px;
  background: #f3f3f3;
  color: #666;
  cursor: pointer;
}

.btn-submit {
  padding: 8px 14px;
  border: none;
  border-radius: 8px;
  background: #2d5f5d;
  color: #fff;
  cursor: pointer;
}

.btn-submit:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

/* 关怀模式适配 */
@media (max-width: 767px) {
  body.care-mode .profile-container {
    padding: 0 0 20px;
  }

  body.care-mode .nickname {
    font-size: 28px;
  }

  body.care-mode .action-btn,
  body.care-mode .admin-btn {
    padding: 20px;
    font-size: 18px;
  }

  body.care-mode .action-btn .icon,
  body.care-mode .admin-btn .icon {
    width: 24px;
    height: 24px;
  }
}

/* 移动端适配 */
@media (max-width: 767px) {
  .profile-container {
    padding: 0 0 20px;
  }

  .profile-grid {
    grid-template-columns: 1fr;
    gap: 14px;
  }

  .profile-card,
  .actions-card,
  .admin-entrance,
  .family-info,
  .health-summary-card {
    padding: 16px;
  }

  .action-btn {
    padding: 14px 12px;
    font-size: 14px;
  }

  .nickname {
    font-size: 21px;
  }

  .summary-grid {
    grid-template-columns: 1fr;
  }

  .profile-extra-grid {
    grid-template-columns: 1fr;
  }

  .admin-summary-grid {
    grid-template-columns: 1fr;
  }

  .actions-family-row {
    flex-direction: column;
    align-items: flex-start;
    gap: 4px;
  }

  .form-row {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 1024px) {
  .profile-grid {
    grid-template-columns: 1fr;
    gap: 16px;
  }
}
</style>
