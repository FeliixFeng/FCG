<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import BaseLayout from '../../components/common/BaseLayout.vue'
import AvatarIcon from '../../components/common/AvatarIcon.vue'
import { useUserStore } from '../../stores/user'

const router = useRouter()
const userStore = useUserStore()

const memberName = computed(() => userStore.member?.nickname || '成员')
const relation = computed(() => userStore.member?.relation || '家人')

const switchMember = () => {
  userStore.exitMember()
  router.replace({ name: 'select-member' })
}

const logout = () => {
  userStore.logout()
  router.replace({ name: 'landing' })
}
</script>

<template>
  <BaseLayout>
    <div class="care-page">
      <section class="care-card">
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
            <p class="care-desc">关系：{{ relation }}</p>
          </div>
        </div>
      </section>

      <section class="care-card actions">
        <button class="action-btn" @click="switchMember">切换成员</button>
        <button class="action-btn danger" @click="logout">退出登录</button>
      </section>
    </div>
  </BaseLayout>
</template>

<style scoped>
.care-page {
  max-width: 960px;
  margin: 0 auto;
  padding: 20px;
  display: grid;
  gap: 16px;
}

.care-card {
  background: rgba(255, 255, 255, 0.92);
  border: 1px solid rgba(45, 95, 93, 0.12);
  border-radius: 16px;
  padding: 20px;
  box-shadow: 0 8px 24px rgba(45, 95, 93, 0.08);
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
  font-size: 1.45rem;
  color: #1f3f3e;
}

.care-desc {
  margin: 0;
  font-size: 1.05rem;
  color: #385e5d;
}

.actions {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.action-btn {
  height: 52px;
  border: 0;
  border-radius: 12px;
  background: #2d5f5d;
  color: #fff;
  font-size: 1.05rem;
  cursor: pointer;
}

.action-btn.danger {
  background: #b44b42;
}

@media (max-width: 768px) {
  .care-page {
    padding: 14px;
  }

  .care-card {
    padding: 16px;
  }

  .actions {
    grid-template-columns: 1fr;
  }
}
</style>

