<script setup>
import BaseLayout from '../components/common/BaseLayout.vue'
import { onMounted, ref } from 'vue'
import { fetchFamilyInfo, fetchFamilyMembers } from '../utils/api'
import { useUserStore } from '../stores/user'

const userStore = useUserStore()

const loading = ref(false)
const info = ref(null)
const members = ref([])
const error = ref('')

const loadFamily = async () => {
  error.value = ''
  try {
    const res = await fetchFamilyInfo()
    info.value = res.data
    const memberRes = await fetchFamilyMembers()
    members.value = memberRes.data
  } catch (err) {
    info.value = null
    members.value = []
    error.value = err?.message || '获取家庭信息失败'
  }
}

const startPreview = () => {
  sessionStorage.setItem('fcg_preview_care', '1')
  window.location.reload()
}

onMounted(() => {
  loadFamily()
})
</script>

<template>
  <BaseLayout>
    <section class="grid">
      <div class="card panel">
        <h2>家庭信息</h2>
        <p class="muted" v-if="!info">当前未加入家庭</p>
        <div v-else class="info">
          <div>
            <div class="label">家庭名称</div>
            <div class="value">{{ info.familyName }}</div>
          </div>
          <div>
            <div class="label">创建时间</div>
            <div class="value">{{ info.createTime }}</div>
          </div>
        </div>
      </div>

      <div class="card panel" v-if="info">
        <div class="panel-header">
          <h2>家庭成员</h2>
          <button v-if="userStore.isAdmin" @click="startPreview" class="btn-preview">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/>
              <circle cx="12" cy="12" r="3"/>
            </svg>
            预览关怀模式
          </button>
        </div>
        <div v-if="members.length === 0" class="muted">暂无成员</div>
        <div v-else class="member-list">
          <div v-for="member in members" :key="member.userId" class="member">
            <div>
              <div class="member-name">{{ member.nickname || member.username }}</div>
              <div class="muted">关系：{{ member.relation || '未设置' }}</div>
            </div>
            <div class="member-meta">
              <span>角色：{{ member.role === 0 ? '超级管理员' : member.role === 1 ? '普通成员' : '受控成员' }}</span>
              <span>关怀模式：{{ member.role === 2 ? '强制启用' : '关闭' }}</span>
            </div>
          </div>
        </div>
      </div>
    </section>
  </BaseLayout>
</template>

<style scoped>
.grid {
  display: grid;
  gap: 20px;
}

.panel {
  padding: 24px;
  display: grid;
  gap: 16px;
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.panel-header h2 {
  margin: 0;
}

.btn-preview {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  border: 1px solid rgba(45, 95, 93, 0.25);
  border-radius: 10px;
  background: rgba(45, 95, 93, 0.05);
  color: #2d5f5d;
  font-size: 0.85rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  font-family: inherit;
}

.btn-preview:hover {
  background: rgba(45, 95, 93, 0.1);
  border-color: rgba(45, 95, 93, 0.4);
}

.info {
  display: grid;
  gap: 12px;
}

.label {
  font-size: 0.8rem;
  color: var(--muted);
}

.value {
  font-size: 1.05rem;
  font-weight: 600;
}

.form {
  display: grid;
  gap: 10px;
}

.divider {
  height: 1px;
  background: var(--border);
}

.member-list {
  display: grid;
  gap: 12px;
}

.member {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 14px;
  border-radius: 12px;
  background: #f9f7f2;
}

.member-name {
  font-weight: 600;
}

.member-meta {
  display: grid;
  gap: 6px;
  font-size: 0.85rem;
  color: var(--muted);
}

.btn.ghost {
  background: transparent;
  border: 1px solid var(--primary);
  color: var(--primary);
}

.error {
  color: #b42318;
  margin: 0;
}
</style>
