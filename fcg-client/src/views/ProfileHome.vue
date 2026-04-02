<script setup>
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import BaseLayout from '../components/common/BaseLayout.vue'
import AvatarIcon from '../components/common/AvatarIcon.vue'
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
  router.push({ name: 'admin-members' })
}
</script>

<template>
  <BaseLayout>
    <section class="profile-container">
      <!-- 成员信息卡片 -->
      <div class="card profile-card">
        <AvatarIcon
          class="avatar"
          :class="{ 'care-mode': userStore.isCareMode }"
          :avatar="userStore.member?.avatar"
          :relation="userStore.member?.relation"
          :role="userStore.member?.role"
          :nickname="userStore.member?.nickname"
          :size="80"
        />
        <h2 class="nickname">{{ userStore.member?.nickname || '未命名' }}</h2>
        <div class="meta">
          <span class="role-badge" :class="`role-${userStore.member?.role}`">
            {{ roleLabel }}
          </span>
          <span class="relation">{{ userStore.member?.relation || '未设置关系' }}</span>
        </div>
        <div class="info-item">
          <span class="label">关怀模式</span>
          <span class="value">{{ careModeStatus }}</span>
        </div>
      </div>

      <!-- 管理入口（仅管理员） -->
      <div v-if="userStore.isAdmin" class="card admin-entrance">
        <button @click="handleEnterAdmin" class="admin-btn">
          <svg class="icon" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M12 6v6m0 0v6m0-6h6m-6 0H6"/>
            <rect x="3" y="3" width="18" height="18" rx="2"/>
          </svg>
          <span>进入管理界面</span>
          <svg class="arrow" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M5 12h14m-7-7 7 7-7 7"/>
          </svg>
        </button>
        <p class="admin-hint">管理家庭成员、权限设置、系统配置</p>
      </div>

      <!-- 功能列表 -->
      <div class="card actions-card">
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
      </div>

      <!-- 家庭信息 -->
      <div class="card family-info" v-if="userStore.family">
        <h3>家庭信息</h3>
        <div class="info-row">
          <span class="label">家庭名称</span>
          <span class="value">{{ userStore.family.familyName || '未命名家庭' }}</span>
        </div>
        <div class="info-row" v-if="userStore.isAdmin">
          <span class="label">邀请码</span>
          <span class="value">{{ userStore.family.inviteCode || '-' }}</span>
        </div>
      </div>
    </section>
  </BaseLayout>
</template>

<style scoped>
.profile-container {
  max-width: 600px;
  margin: 0 auto;
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

/* 成员信息卡片 */
.profile-card {
  padding: 32px 24px;
  text-align: center;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
}

.avatar {
  flex-shrink: 0;
}

.avatar.care-mode {
  transform: scale(1.25);
}

.nickname {
  font-size: 24px;
  font-weight: 600;
  color: #2d5f5d;
  margin: 0;
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
  padding: 12px 16px;
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

/* 管理入口卡片 */
.admin-entrance {
  padding: 20px;
  background: linear-gradient(135deg, #f8f9fa 0%, #ffffff 100%);
  border: 2px solid #2d5f5d;
}

.admin-btn {
  width: 100%;
  padding: 16px 20px;
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

.admin-hint {
  margin: 12px 0 0 0;
  text-align: center;
  color: #666;
  font-size: 13px;
}

/* 功能列表卡片 */
.actions-card {
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
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
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 12px;
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

/* 关怀模式适配 */
@media (max-width: 767px) {
  body.care-mode .profile-container {
    padding: 16px;
    gap: 16px;
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
    padding: 16px;
  }

  .avatar {
    width: 70px;
    height: 70px;
    font-size: 28px;
  }

  .avatar.care-mode {
    width: 90px;
    height: 90px;
    font-size: 36px;
  }

  .nickname {
    font-size: 20px;
  }
}
</style>
