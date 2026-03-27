<script setup>
import { ref, onMounted } from 'vue'
import { useUserStore } from '../../stores/user'
import { fetchFamilyMembers } from '../../utils/api'
import AvatarIcon from '../../components/common/AvatarIcon.vue'

const userStore = useUserStore()

const members = ref([])
const loading = ref(false)
const error = ref('')

const roleOptions = [
  { value: 0, label: '管理员' },
  { value: 1, label: '普通成员' },
  { value: 2, label: '受控成员' }
]

const roleLabel = (role) => {
  return roleOptions.find(o => o.value === role)?.label || '未知'
}

async function loadMembers() {
  loading.value = true
  error.value = ''
  try {
    const res = await fetchFamilyMembers()
    members.value = res.data
  } catch (err) {
    error.value = err?.message || '获取成员失败'
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadMembers()
})
</script>

<template>
  <div class="admin-members">
    <div class="page-header">
      <h2>成员管理</h2>
    </div>

    <div v-if="loading" class="loading">加载中...</div>
    <div v-else-if="error" class="error">{{ error }}</div>
    
    <div v-else class="member-table">
      <div class="table-header">
        <span class="col-avatar">头像</span>
        <span class="col-name">昵称</span>
        <span class="col-relation">关系</span>
        <span class="col-role">角色</span>
      </div>
      
      <div v-for="member in members" :key="member.userId" class="table-row">
        <div class="col-avatar">
          <AvatarIcon
            :avatar="member.avatar"
            :relation="member.relation"
            :role="member.role"
            :nickname="member.nickname"
            :size="40"
          />
        </div>
        <div class="col-name">
          {{ member.nickname }}
          <span v-if="member.userId === userStore.member?.userId" class="self-tag">我</span>
        </div>
        <div class="col-relation">{{ member.relation || '-' }}</div>
        <div class="col-role">
          <span class="role-badge" :class="`role-${member.role}`">
            {{ roleLabel(member.role) }}
          </span>
        </div>
      </div>
      
      <div v-if="members.length === 0" class="empty">暂无成员</div>
    </div>
  </div>
</template>

<style scoped>
.admin-members {
  max-width: 900px;
}

.page-header {
  margin-bottom: 24px;
}

.page-header h2 {
  margin: 0;
  font-size: 1.25rem;
  color: #2d5f5d;
}

.loading, .error, .empty {
  text-align: center;
  padding: 40px;
  color: #666;
}

.error {
  color: #e74c3c;
}

.member-table {
  background: white;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 2px 12px rgba(45, 95, 93, 0.08);
}

.table-header {
  display: flex;
  align-items: center;
  padding: 14px 20px;
  background: #f8f9fa;
  font-weight: 600;
  font-size: 0.85rem;
  color: #666;
  border-bottom: 1px solid #f0f0f0;
}

.table-row {
  display: flex;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid #f5f5f5;
}

.table-row:last-child {
  border-bottom: none;
}

.table-row:hover {
  background: #fafafa;
}

.col-avatar {
  flex-shrink: 0;
  width: 50px;
}

.col-name {
  flex: 1;
  padding-left: 12px;
  font-weight: 500;
}

.self-tag {
  margin-left: 8px;
  padding: 2px 8px;
  background: #e4f0ef;
  color: #2d5f5d;
  font-size: 0.7rem;
  border-radius: 4px;
  font-weight: normal;
}

.col-relation {
  width: 100px;
  color: #666;
}

.col-role {
  width: 100px;
}

.role-badge {
  padding: 4px 10px;
  border-radius: 12px;
  font-size: 0.75rem;
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

@media (max-width: 767px) {
  .table-header {
    display: none;
  }
  
  .table-row {
    flex-wrap: wrap;
    gap: 12px;
    padding: 16px;
  }
  
  .col-avatar {
    width: auto;
  }
  
  .col-name {
    flex: 1 1 calc(100% - 60px);
    padding-left: 12px;
  }
  
  .col-relation,
  .col-role {
    width: auto;
    padding-left: 52px;
  }
}
</style>
