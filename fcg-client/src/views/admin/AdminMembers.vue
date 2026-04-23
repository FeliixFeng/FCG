<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '../../stores/user'
import {
  addMember,
  deleteMember,
  fetchFamilyMembers,
  fetchMemberDetail,
  updateMember,
  updateMemberRole
} from '../../utils/api'
import AvatarIcon from '../../components/common/AvatarIcon.vue'
import AvatarPicker from '../../components/common/AvatarPicker.vue'

const userStore = useUserStore()

const members = ref([])
const loading = ref(false)
const error = ref('')

const searchKeyword = ref('')
const roleFilter = ref('all')

const showAddForm = ref(false)
const adding = ref(false)
const form = ref({
  nickname: '',
  relation: '',
  role: 1,
  phone: ''
})

const showEditModal = ref(false)
const editing = ref(false)
const editForm = ref({
  userId: null,
  avatar: '',
  nickname: '',
  relation: '',
  role: 1,
  phone: '',
  birthday: '',
  height: '',
  weight: '',
  disease: '',
  allergy: '',
  remark: ''
})

const showViewModal = ref(false)
const viewForm = ref({})
const roleUpdatingId = ref(null)

const roleOptions = [
  { value: 0, label: '管理员' },
  { value: 1, label: '普通成员' },
  { value: 2, label: '受控成员' }
]

const baseRelationOptions = [
  { value: '爷爷', label: '爷爷' },
  { value: '奶奶', label: '奶奶' },
  { value: '爸爸', label: '爸爸' },
  { value: '妈妈', label: '妈妈' },
  { value: '儿子', label: '儿子' },
  { value: '女儿', label: '女儿' },
  { value: '__other__', label: '其他' }
]

const roleLabel = (role) => roleOptions.find(item => item.value === role)?.label || '未知'
const roleClass = (role) => `role-${role}`

const totalCount = computed(() => members.value.length)
const adminCount = computed(() => members.value.filter(item => item.role === 0).length)
const normalCount = computed(() => members.value.filter(item => item.role === 1).length)
const careCount = computed(() => members.value.filter(item => item.role === 2).length)

const roleOrder = {
  0: 1,
  2: 2,
  1: 3
}

const filteredMembers = computed(() => {
  const keyword = searchKeyword.value.trim().toLowerCase()
  return members.value
    .filter(item => {
      const hitKeyword =
        !keyword ||
        String(item.nickname || '').toLowerCase().includes(keyword) ||
        String(item.relation || '').toLowerCase().includes(keyword) ||
        String(item.phone || '').toLowerCase().includes(keyword)
      const hitRole = roleFilter.value === 'all' || Number(roleFilter.value) === item.role
      return hitKeyword && hitRole
    })
    .slice()
    .sort((a, b) => {
      const roleDiff = (roleOrder[a.role] || 99) - (roleOrder[b.role] || 99)
      if (roleDiff !== 0) return roleDiff
      return String(a.nickname || '').localeCompare(String(b.nickname || ''), 'zh-Hans-CN')
    })
})

const getAge = (birthday) => {
  if (!birthday) return null
  const birth = new Date(birthday)
  const today = new Date()
  let age = today.getFullYear() - birth.getFullYear()
  const monthDiff = today.getMonth() - birth.getMonth()
  if (monthDiff < 0 || (monthDiff === 0 && today.getDate() < birth.getDate())) {
    age -= 1
  }
  return age >= 0 ? age : null
}

const getDiseaseShort = (disease) => {
  if (!disease) return ''
  const parts = disease
    .split(',')
    .map(item => item.trim())
    .filter(Boolean)
  if (!parts.length) return ''
  return parts.slice(0, 2).join('、') + (parts.length > 2 ? '...' : '')
}

async function loadMembers() {
  loading.value = true
  error.value = ''
  try {
    const res = await fetchFamilyMembers()
    members.value = res.data || []
  } catch (err) {
    error.value = err?.message || '获取成员失败'
  } finally {
    loading.value = false
  }
}

function resetForm() {
  form.value = {
    nickname: '',
    relation: '',
    role: 1,
    phone: ''
  }
}

function closeAddForm() {
  showAddForm.value = false
  resetForm()
}

function openAddForm() {
  resetForm()
  showAddForm.value = true
}

async function handleAddMember() {
  if (!form.value.nickname.trim()) {
    ElMessage.warning('请填写昵称')
    return
  }
  try {
    adding.value = true
    const payload = {
      ...form.value,
      relation: form.value.relation === '__other__' ? '' : form.value.relation
    }
    await addMember(payload)
    ElMessage.success('添加成功')
    closeAddForm()
    await loadMembers()
  } catch (err) {
    ElMessage.error(err?.message || '添加失败')
  } finally {
    adding.value = false
  }
}

async function handleView(member) {
  try {
    const res = await fetchMemberDetail(member.userId)
    viewForm.value = res.data || {}
    showViewModal.value = true
  } catch (err) {
    ElMessage.error('获取成员信息失败')
  }
}

function handleViewConfirm() {
  showViewModal.value = false
  handleEdit({ userId: viewForm.value.userId || viewForm.value.id })
}

async function handleEdit(member) {
  try {
    const res = await fetchMemberDetail(member.userId)
    const data = res.data || {}
    editForm.value = {
      userId: data.id,
      avatar: data.avatar || '',
      nickname: data.nickname || '',
      relation: data.relation || '__other__',
      role: data.role ?? 1,
      phone: data.phone || '',
      birthday: data.birthday || '',
      height: data.height || '',
      weight: data.weight || '',
      disease: data.disease || '',
      allergy: data.allergy || '',
      remark: data.remark || ''
    }
    showEditModal.value = true
  } catch (err) {
    ElMessage.error('获取成员信息失败')
  }
}

async function handleUpdate() {
  if (!editForm.value.nickname.trim()) {
    ElMessage.warning('请填写昵称')
    return
  }
  try {
    editing.value = true
    await updateMember(editForm.value.userId, {
      avatar: editForm.value.avatar || null,
      nickname: editForm.value.nickname,
      relation: editForm.value.relation === '__other__' ? '' : editForm.value.relation,
      role: editForm.value.role,
      phone: editForm.value.phone,
      birthday: editForm.value.birthday || null,
      height: editForm.value.height || null,
      weight: editForm.value.weight || null,
      disease: editForm.value.disease || '',
      allergy: editForm.value.allergy || '',
      remark: editForm.value.remark || ''
    })
    ElMessage.success('保存成功')
    showEditModal.value = false
    await loadMembers()
  } catch (err) {
    ElMessage.error(err?.message || '更新失败')
  } finally {
    editing.value = false
  }
}

async function handleDelete(member) {
  if (member.userId === userStore.member?.userId) {
    ElMessage.warning('不能删除自己')
    return
  }
  try {
    await ElMessageBox.confirm(
      `确定删除成员“${member.nickname}”吗？此操作不可恢复。`,
      '删除确认',
      {
        confirmButtonText: '删除',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    await deleteMember(member.userId)
    ElMessage.success('删除成功')
    await loadMembers()
  } catch (err) {
    if (err !== 'cancel') {
      ElMessage.error(err?.message || '删除失败')
    }
  }
}

async function handleRoleChange(member, targetRoleValue) {
  const targetRole = Number(targetRoleValue)
  if (targetRole === member.role) return
  if (member.userId === userStore.member?.userId) {
    ElMessage.warning('不能修改自己的角色')
    await loadMembers()
    return
  }

  try {
    await ElMessageBox.confirm(
      `将“${member.nickname}”的角色修改为“${roleLabel(targetRole)}”？`,
      '角色变更确认',
      {
        confirmButtonText: '确认',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    roleUpdatingId.value = member.userId
    await updateMemberRole(member.userId, targetRole)
    ElMessage.success('角色已更新')
    await loadMembers()
  } catch (err) {
    if (err !== 'cancel') {
      ElMessage.error(err?.message || '更新角色失败')
      await loadMembers()
    } else {
      await loadMembers()
    }
  } finally {
    roleUpdatingId.value = null
  }
}

onMounted(() => {
  loadMembers()
})
</script>

<template>
  <div class="admin-members">
    <header class="page-header card">
      <div class="header-main">
        <div>
          <h2>成员管理</h2>
          <p>统一管理家庭成员资料、角色权限与关怀对象。</p>
        </div>
        <button class="btn-add" @click="openAddForm">
          + 添加成员
        </button>
      </div>

      <div class="stats-row">
        <div class="stat-card">
          <span class="stat-head">
            <span class="stat-icon">👥</span>
            <span class="stat-label">总成员</span>
          </span>
          <strong class="stat-value">{{ totalCount }}</strong>
        </div>
        <div class="stat-card role-0">
          <span class="stat-head">
            <span class="stat-icon">🛡️</span>
            <span class="stat-label">管理员</span>
          </span>
          <strong class="stat-value">{{ adminCount }}</strong>
        </div>
        <div class="stat-card role-2">
          <span class="stat-head">
            <span class="stat-icon">❤️</span>
            <span class="stat-label">受控成员</span>
          </span>
          <strong class="stat-value">{{ careCount }}</strong>
        </div>
        <div class="stat-card role-1">
          <span class="stat-head">
            <span class="stat-icon">🙂</span>
            <span class="stat-label">普通成员</span>
          </span>
          <strong class="stat-value">{{ normalCount }}</strong>
        </div>
      </div>

      <div class="filters-row">
        <input v-model="searchKeyword" class="search-input" placeholder="搜索昵称 / 关系 / 手机号" />
        <select v-model="roleFilter" class="filter-select">
          <option value="all">全部角色</option>
          <option v-for="item in roleOptions" :key="item.value" :value="String(item.value)">
            {{ item.label }}
          </option>
        </select>
      </div>
    </header>

    <div v-if="loading" class="state card">加载中...</div>
    <div v-else-if="error" class="state card error-text">{{ error }}</div>

    <section v-else class="member-grid">
      <article
        v-for="member in filteredMembers"
        :key="member.userId"
        class="member-card card"
        :class="roleClass(member.role)"
      >
        <div class="member-head">
          <AvatarIcon
            :avatar="member.avatar"
            :relation="member.relation"
            :role="member.role"
            :nickname="member.nickname"
            :size="44"
          />
          <div class="member-title">
            <div class="name-line">
              <span class="nickname">{{ member.nickname }}</span>
              <span v-if="member.userId === userStore.member?.userId" class="self-tag">我</span>
            </div>
            <div class="sub-line">
              <span>👪 {{ member.relation || '未设置关系' }}</span>
              <span>📱 {{ member.phone || '未填写手机号' }}</span>
            </div>
          </div>
        </div>

        <div class="member-meta">
          <span v-if="member.birthday" class="meta-tag">年龄 {{ getAge(member.birthday) ?? '--' }} 岁</span>
          <span v-if="member.disease" class="meta-tag warn">{{ getDiseaseShort(member.disease) }}</span>
          <span v-if="!member.birthday && !member.disease" class="meta-tag plain">资料待完善</span>
        </div>

        <div class="member-role-row">
          <label>角色权限</label>
          <select
            class="role-select"
            :class="roleClass(member.role)"
            :disabled="roleUpdatingId === member.userId"
            :value="String(member.role)"
            @change="handleRoleChange(member, $event.target.value)"
          >
            <option v-for="item in roleOptions" :key="item.value" :value="String(item.value)">
              {{ item.label }}
            </option>
          </select>
        </div>

        <div class="member-actions">
          <button class="btn-view" @click="handleView(member)">查看</button>
          <button class="btn-edit" @click="handleEdit(member)">编辑</button>
          <button
            class="btn-delete"
            :disabled="member.userId === userStore.member?.userId"
            @click="handleDelete(member)"
          >
            删除
          </button>
        </div>
      </article>

      <div v-if="filteredMembers.length === 0" class="state card">没有匹配成员，试试更换筛选条件。</div>
    </section>

    <div v-if="showAddForm" class="modal-mask" @click.self="closeAddForm">
      <div class="modal-content modal-content-form" @keydown.enter.prevent="handleAddMember">
        <div class="modal-header modal-header-accent">
          <h3>添加新成员</h3>
          <button class="btn-close" @click="closeAddForm">&times;</button>
        </div>
        <div class="modal-body">
          <div class="form-grid">
            <div class="form-field">
              <label>昵称 *</label>
              <input v-model="form.nickname" placeholder="例如：爷爷、妈妈" />
            </div>
            <div class="form-field">
              <label>家庭关系</label>
              <select v-model="form.relation">
                <option value="" disabled>选择关系</option>
                <option v-for="item in baseRelationOptions" :key="item.value" :value="item.value">
                  {{ item.label }}
                </option>
              </select>
            </div>
            <div class="form-field">
              <label>手机号</label>
              <input v-model="form.phone" placeholder="选填" />
            </div>
            <div class="form-field">
              <label>角色</label>
              <select v-model="form.role">
                <option v-for="item in roleOptions" :key="item.value" :value="item.value">
                  {{ item.label }}
                </option>
              </select>
            </div>
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn-ghost" @click="closeAddForm">取消</button>
          <button class="btn-primary" :disabled="adding" @click="handleAddMember">
            {{ adding ? '添加中...' : '确认添加' }}
          </button>
        </div>
      </div>
    </div>

    <div v-if="showViewModal" class="modal-mask" @click.self="showViewModal = false">
      <div class="modal-content modal-content-view" @keydown.enter.prevent="handleViewConfirm">
        <div class="modal-header modal-header-accent">
          <h3>查看成员信息</h3>
          <button class="btn-close" @click="showViewModal = false">&times;</button>
        </div>
        <div class="modal-body modal-body-view">
          <div class="view-row">
            <span>昵称</span>
            <strong>{{ viewForm.nickname || '-' }}</strong>
          </div>
          <div class="view-row">
            <span>关系</span>
            <strong>{{ viewForm.relation || '-' }}</strong>
          </div>
          <div class="view-row">
            <span>手机号</span>
            <strong>{{ viewForm.phone || '-' }}</strong>
          </div>
          <div class="view-row">
            <span>角色</span>
            <strong>{{ roleLabel(viewForm.role) }}</strong>
          </div>
          <div class="view-divider">扩展信息</div>
          <div class="view-row">
            <span>出生日期</span>
            <strong>{{ viewForm.birthday || '-' }}</strong>
          </div>
          <div class="view-row">
            <span>身高</span>
            <strong>{{ viewForm.height ? `${viewForm.height} cm` : '-' }}</strong>
          </div>
          <div class="view-row">
            <span>体重</span>
            <strong>{{ viewForm.weight ? `${viewForm.weight} kg` : '-' }}</strong>
          </div>
          <div class="view-row">
            <span>病史</span>
            <strong>{{ viewForm.disease || '-' }}</strong>
          </div>
          <div class="view-row">
            <span>备注</span>
            <strong>{{ viewForm.remark || '-' }}</strong>
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn-primary" @click="handleViewConfirm">
            编辑信息
          </button>
        </div>
      </div>
    </div>

    <div v-if="showEditModal" class="modal-mask" @click.self="showEditModal = false">
      <div class="modal-content modal-content-form" @keydown.enter.prevent="handleUpdate">
        <div class="modal-header modal-header-accent">
          <h3>编辑成员</h3>
          <button class="btn-close" @click="showEditModal = false">&times;</button>
        </div>
        <div class="modal-body">
          <div class="form-field">
            <label>头像</label>
            <AvatarPicker v-model="editForm.avatar" :current-relation="editForm.relation" :current-role="editForm.role" />
          </div>
          <div class="form-field">
            <label>昵称 *</label>
            <input v-model="editForm.nickname" />
          </div>
          <div class="form-field">
            <label>家庭关系</label>
            <select v-model="editForm.relation">
              <option v-for="item in baseRelationOptions" :key="item.value" :value="item.value">
                {{ item.label }}
              </option>
            </select>
          </div>
          <div class="form-field">
            <label>手机号</label>
            <input v-model="editForm.phone" />
          </div>
          <div class="form-field">
            <label>角色</label>
            <select v-model="editForm.role">
              <option v-for="item in roleOptions" :key="item.value" :value="item.value">
                {{ item.label }}
              </option>
            </select>
          </div>
          <div class="view-divider">扩展信息（选填）</div>
          <div class="double-field">
            <div class="form-field">
              <label>出生日期</label>
              <input type="date" v-model="editForm.birthday" />
            </div>
            <div class="form-field">
              <label>身高(cm)</label>
              <input type="number" v-model="editForm.height" placeholder="如：170" />
            </div>
          </div>
          <div class="double-field">
            <div class="form-field">
              <label>体重(kg)</label>
              <input type="number" v-model="editForm.weight" placeholder="如：65" />
            </div>
            <div class="form-field">
              <label>病史</label>
              <input v-model="editForm.disease" placeholder="高血压,糖尿病" />
            </div>
          </div>
          <div class="form-field">
            <label>过敏史</label>
            <input v-model="editForm.allergy" placeholder="如：青霉素" />
          </div>
          <div class="form-field">
            <label>备注</label>
            <textarea v-model="editForm.remark" placeholder="其他说明..." @keydown.enter.stop />
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn-ghost" @click="showEditModal = false">取消</button>
          <button class="btn-primary" :disabled="editing" @click="handleUpdate">
            {{ editing ? '保存中...' : '保存' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<style>
.admin-members {
  display: grid;
  gap: 14px;
}

.card {
  background: rgba(255, 255, 255, 0.96);
  border: 1px solid rgba(45, 95, 93, 0.12);
  border-radius: 14px;
  box-shadow: 0 8px 20px rgba(45, 95, 93, 0.08);
}

.page-header {
  padding: 14px;
  display: grid;
  gap: 12px;
}

.header-main {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: flex-start;
}

.header-main h2 {
  margin: 0 0 6px;
  font-size: 1.4rem;
  color: #1f4543;
}

.header-main p {
  margin: 0;
  color: #607876;
  font-size: 0.92rem;
}

.btn-add,
.btn-primary,
.btn-ghost {
  height: 38px;
  border-radius: 10px;
  border: none;
  padding: 0 14px;
  cursor: pointer;
  font-size: 0.88rem;
  transition: transform 0.18s ease, box-shadow 0.2s ease, background 0.2s ease, border-color 0.2s ease;
}

.btn-add,
.btn-primary {
  background: #2d5f5d;
  color: #fff;
}

.btn-add:hover,
.btn-primary:hover {
  background: #234a48;
  transform: translateY(-1px);
  box-shadow: 0 8px 14px rgba(35, 74, 72, 0.24);
}

.btn-ghost {
  border: 1px solid rgba(45, 95, 93, 0.2);
  background: rgba(245, 250, 249, 0.92);
  color: #315b58;
}

.btn-ghost:hover {
  transform: translateY(-1px);
  box-shadow: 0 6px 12px rgba(45, 95, 93, 0.12);
  border-color: rgba(45, 95, 93, 0.32);
}

.stats-row {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
}

.stat-card {
  border: 1px solid rgba(45, 95, 93, 0.14);
  border-radius: 12px;
  padding: 11px 12px;
  display: grid;
  gap: 7px;
  background: linear-gradient(145deg, rgba(245, 251, 250, 0.85), rgba(255, 255, 255, 0.98));
  box-shadow: 0 4px 10px rgba(45, 95, 93, 0.05);
  color: #2d5f5d;
  transition: transform 0.2s ease, box-shadow 0.22s ease;
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 10px 16px rgba(45, 95, 93, 0.12);
}

.stat-head {
  display: flex;
  align-items: center;
  gap: 6px;
}

.stat-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
  border-radius: 6px;
  font-size: 0.78rem;
  background: rgba(45, 95, 93, 0.11);
}

.stat-label {
  font-size: 0.8rem;
  color: #5f7b78;
}

.stat-value {
  font-size: 1.2rem;
  color: #1f4543;
  line-height: 1;
}

.stat-card.role-0 {
  background: linear-gradient(145deg, rgba(255, 249, 238, 0.9), rgba(255, 255, 255, 0.98));
  border-color: rgba(214, 137, 16, 0.28);
  color: #a96a07;
}

.stat-card.role-1 {
  background: linear-gradient(145deg, rgba(241, 248, 253, 0.9), rgba(255, 255, 255, 0.98));
  border-color: rgba(40, 116, 166, 0.26);
  color: #1d6593;
}

.stat-card.role-2 {
  background: linear-gradient(145deg, rgba(255, 243, 242, 0.9), rgba(255, 255, 255, 0.98));
  border-color: rgba(192, 57, 43, 0.24);
  color: #a73328;
}

.filters-row {
  display: grid;
  grid-template-columns: minmax(280px, 1fr) 180px;
  gap: 8px;
}

.search-input,
.filter-select,
.form-field input,
.form-field select,
.form-field textarea,
.role-select {
  width: 100%;
  border: 1px solid rgba(45, 95, 93, 0.2);
  border-radius: 10px;
  height: 38px;
  padding: 0 12px;
  font-size: 0.88rem;
  color: #2a4846;
  background: #fff;
  box-sizing: border-box;
  transition: transform 0.18s ease, box-shadow 0.2s ease, border-color 0.2s ease;
}

.search-input:hover,
.filter-select:hover,
.form-field input:hover,
.form-field select:hover,
.form-field textarea:hover,
.role-select:hover {
  transform: translateY(-1px);
  box-shadow: 0 6px 12px rgba(45, 95, 93, 0.08);
  border-color: rgba(45, 95, 93, 0.32);
}

.form-field textarea {
  height: 86px;
  padding: 8px 12px;
  resize: vertical;
}

.search-input::placeholder {
  color: #9bb0af;
}

.add-form {
  padding: 14px;
  display: grid;
  gap: 12px;
}

.form-title {
  margin: 0;
  font-size: 0.95rem;
  color: #315b58;
  font-weight: 600;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.form-field {
  display: grid;
  gap: 6px;
}

.form-field label {
  font-size: 0.83rem;
  color: #5f7976;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

.member-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.member-card {
  padding: 12px;
  display: grid;
  gap: 10px;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.member-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 10px 16px rgba(45, 95, 93, 0.09);
}

.member-card.role-0 {
  background: linear-gradient(145deg, rgba(255, 251, 243, 0.92), rgba(255, 255, 255, 0.99));
  border-color: rgba(214, 137, 16, 0.22);
}

.member-card.role-1 {
  background: linear-gradient(145deg, rgba(244, 250, 254, 0.92), rgba(255, 255, 255, 0.99));
  border-color: rgba(40, 116, 166, 0.2);
}

.member-card.role-2 {
  background: linear-gradient(145deg, rgba(255, 246, 245, 0.92), rgba(255, 255, 255, 0.99));
  border-color: rgba(192, 57, 43, 0.2);
}

.member-head {
  display: flex;
  align-items: center;
  gap: 10px;
}

.member-title {
  min-width: 0;
  flex: 1;
}

.name-line {
  display: flex;
  align-items: center;
  gap: 6px;
}

.nickname {
  font-size: 1rem;
  font-weight: 700;
  color: #214644;
}

.self-tag {
  padding: 2px 8px;
  border-radius: 999px;
  font-size: 0.7rem;
  color: #2d5f5d;
  background: rgba(45, 95, 93, 0.12);
}

.sub-line {
  margin-top: 3px;
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  font-size: 0.82rem;
  color: #718886;
}

.member-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.meta-tag {
  font-size: 0.76rem;
  padding: 3px 8px;
  border-radius: 999px;
  color: #406764;
  background: rgba(45, 95, 93, 0.1);
}

.meta-tag.warn {
  background: #fff5ea;
  color: #b66c0e;
}

.meta-tag.plain {
  background: #f3f6f6;
  color: #899d9b;
}

.member-role-row {
  display: grid;
  grid-template-columns: 68px 1fr;
  align-items: center;
  gap: 8px;
}

.member-role-row label {
  font-size: 0.82rem;
  color: #637d7a;
}

.role-select.role-0 {
  background: #fef3e7;
  color: #a96a07;
}

.role-select.role-1 {
  background: #e8f4f8;
  color: #1d6593;
}

.role-select.role-2 {
  background: #fdecea;
  color: #a73328;
}

.member-actions {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
}

.member-actions button {
  height: 34px;
  border-radius: 9px;
  border: 1px solid transparent;
  font-size: 0.82rem;
  cursor: pointer;
  transition: transform 0.16s ease, box-shadow 0.2s ease, filter 0.2s ease;
}

.member-actions button:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 6px 10px rgba(45, 95, 93, 0.14);
  filter: saturate(1.04);
}

.btn-view {
  background: #eaf4f3;
  color: #2d5f5d;
}

.btn-edit {
  background: #e8f4f8;
  color: #1d6593;
}

.btn-delete {
  background: #fdecea;
  color: #bf3e31;
}

.btn-delete:disabled {
  opacity: 0.42;
  cursor: not-allowed;
}

.state {
  padding: 28px 12px;
  text-align: center;
  color: #6f8684;
}

.error-text {
  color: #c0392b;
}

.modal-mask {
  position: fixed;
  inset: 0;
  z-index: 1000;
  background: rgba(8, 15, 15, 0.46);
  display: flex;
  align-items: center;
  justify-content: center;
}

.modal-content {
  width: min(760px, 94vw);
  max-height: 90vh;
  overflow: auto;
  background: linear-gradient(180deg, #ffffff, #fbfdfd);
  border: 1px solid rgba(45, 95, 93, 0.14);
  border-radius: 16px;
  box-shadow: 0 20px 48px rgba(12, 32, 31, 0.22);
}

.modal-content-form {
  width: min(760px, 94vw);
}

.modal-content-view {
  width: min(620px, 94vw);
}

.modal-header,
.modal-footer {
  padding: 13px 16px;
  border-bottom: 1px solid #edf2f2;
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.modal-header-accent {
  background: linear-gradient(145deg, rgba(239, 248, 247, 0.9), rgba(255, 255, 255, 0.92));
}

.modal-header h3 {
  margin: 0;
  color: #224a47;
  font-size: 1.03rem;
  letter-spacing: 0.01em;
}

.btn-close {
  border: none;
  background: transparent;
  font-size: 1.4rem;
  color: #8da3a1;
  cursor: pointer;
}

.modal-body {
  padding: 16px;
  display: grid;
  gap: 11px;
}

.modal-body-view {
  gap: 8px;
}

.modal-footer {
  border-top: 1px solid #edf2f2;
  border-bottom: none;
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

.view-row {
  display: grid;
  grid-template-columns: 98px 1fr;
  gap: 8px;
  align-items: center;
  font-size: 0.88rem;
  color: #5f7976;
  padding: 8px 10px;
  border-radius: 10px;
  background: rgba(244, 250, 249, 0.72);
  border: 1px solid rgba(45, 95, 93, 0.08);
}

.view-row strong {
  color: #264a48;
  font-weight: 600;
  word-break: break-word;
}

.view-divider {
  margin-top: 4px;
  padding-top: 10px;
  border-top: 1px dashed #d8e3e2;
  font-size: 0.82rem;
  color: #587673;
  font-weight: 600;
}

.double-field {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

@media (max-width: 1024px) {
  .member-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .header-main {
    flex-direction: column;
    align-items: stretch;
  }

  .stats-row {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .filters-row {
    grid-template-columns: 1fr;
  }

  .btn-add {
    width: 100%;
  }

  .form-grid,
  .double-field {
    grid-template-columns: 1fr;
  }

  .member-actions {
    grid-template-columns: 1fr 1fr 1fr;
  }

  .modal-content {
    width: 95vw;
  }
}
</style>
