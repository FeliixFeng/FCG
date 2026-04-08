<script setup>
import { ref, onMounted } from 'vue'
import { useUserStore } from '../../stores/user'
import { fetchFamilyMembers, addMember, fetchMemberDetail, updateMember, deleteMember, updateMemberRole } from '../../utils/api'
import AvatarIcon from '../../components/common/AvatarIcon.vue'
import AvatarPicker from '../../components/common/AvatarPicker.vue'
import { ElMessage, ElMessageBox } from 'element-plus'

const userStore = useUserStore()

const members = ref([])
const loading = ref(false)
const error = ref('')

// 添加成员
const showAddForm = ref(false)
const adding = ref(false)
const form = ref({
  nickname: '',
  relation: '',
  role: 1,
  phone: ''
})

// 编辑弹窗
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

const roleOptions = [
  { value: 0, label: '管理员' },
  { value: 1, label: '普通成员' },
  { value: 2, label: '受控成员' }
]

const relationOptions = [
  { value: '爷爷', label: '爷爷' },
  { value: '奶奶', label: '奶奶' },
  { value: '爸爸', label: '爸爸' },
  { value: '妈妈', label: '妈妈' },
  { value: '儿子', label: '儿子' },
  { value: '女儿', label: '女儿' },
  { value: '', label: '其他' }
]

const roleLabel = (role) => {
  return roleOptions.find(o => o.value === role)?.label || '未知'
}

// 从生日计算年龄
const getAge = (birthday) => {
  if (!birthday) return null
  const birth = new Date(birthday)
  const today = new Date()
  let age = today.getFullYear() - birth.getFullYear()
  const m = today.getMonth() - birth.getMonth()
  if (m < 0 || (m === 0 && today.getDate() < birth.getDate())) {
    age--
  }
  return age >= 0 ? age : null
}

// 病史简写（取前两个）
const getDiseaseShort = (disease) => {
  if (!disease) return null
  const list = disease.split(',').map(d => d.trim()).filter(d => d)
  if (list.length === 0) return null
  return list.slice(0, 2).join(',') + (list.length > 2 ? '...' : '')
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

const resetForm = () => {
  form.value = { nickname: '', relation: '', role: 1, phone: '' }
}

const closeAddForm = () => {
  showAddForm.value = false
  resetForm()
  error.value = ''
}

const handleAddMember = async () => {
  if (!form.value.nickname) {
    ElMessage.warning('请填写昵称')
    return
  }
  try {
    adding.value = true
    error.value = ''
    await addMember({ ...form.value })
    ElMessage.success('添加成功')
    closeAddForm()
    await loadMembers()
  } catch (err) {
    ElMessage.error(err?.message || '添加失败')
  } finally {
    adding.value = false
  }
}

// 编辑成员
const handleEdit = async (member) => {
  error.value = ''
  try {
    const res = await fetchMemberDetail(member.userId)
    const data = res.data
    editForm.value = {
      userId: data.id,
      avatar: data.avatar || '',
      nickname: data.nickname,
      relation: data.relation || '',
      role: data.role,
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

const closeEditModal = () => {
  showEditModal.value = false
  error.value = ''
}

// 查看弹窗
const showViewModal = ref(false)
const viewForm = ref({})

const handleView = async (member) => {
  error.value = ''
  try {
    const res = await fetchMemberDetail(member.userId)
    viewForm.value = res.data
    showViewModal.value = true
  } catch (err) {
    ElMessage.error('获取成员信息失败')
  }
}

const closeViewModal = () => {
  showViewModal.value = false
}

const handleUpdate = async () => {
  if (!editForm.value.nickname) {
    ElMessage.warning('请填写昵称')
    return
  }
  try {
    editing.value = true
    await updateMember(editForm.value.userId, {
      avatar: editForm.value.avatar || null,
      nickname: editForm.value.nickname,
      relation: editForm.value.relation,
      role: editForm.value.role,
      phone: editForm.value.phone,
      birthday: editForm.value.birthday || null,
      height: editForm.value.height || null,
      weight: editForm.value.weight || null,
      disease: editForm.value.disease || '',
      allergy: editForm.value.allergy || '',
      remark: editForm.value.remark || ''
    })
    ElMessage.success('更新成功')
    showEditModal.value = false
    await loadMembers()
  } catch (err) {
    ElMessage.error(err?.message || '更新失败')
  } finally {
    editing.value = false
  }
}

// 删除成员
const handleDelete = async (member) => {
  if (member.userId === userStore.member?.userId) {
    ElMessage.warning('不能删除自己')
    return
  }
  
  try {
    await ElMessageBox.confirm(
      `确定要删除成员 "${member.nickname}" 吗？此操作不可恢复。`,
      '删除确认',
      { confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning' }
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

// 修改角色
const handleRoleChange = async (member, newRole) => {
  if (member.userId === userStore.member?.userId && newRole !== member.role) {
    ElMessage.warning('不能修改自己的角色')
    await loadMembers()
    return
  }
  
  try {
    await updateMemberRole(member.userId, newRole)
    ElMessage.success('角色已更新')
    await loadMembers()
  } catch (err) {
    ElMessage.error(err?.message || '更新角色失败')
    await loadMembers()
  }
}

onMounted(() => {
  loadMembers()
})
</script>

<template>
  <div class="admin-members">
    <div class="page-header">
      <div class="header-row">
        <div>
          <h2>成员管理</h2>
          <p class="page-desc">管理家庭成员信息，包括添加、查看、修改角色等操作。</p>
        </div>
        <button class="btn-add" @click="showAddForm = !showAddForm">
          {{ showAddForm ? '取消' : '+ 添加成员' }}
        </button>
      </div>
    </div>

    <!-- 添加成员表单 -->
    <div v-if="showAddForm" class="add-form">
      <div class="form-title">添加新成员</div>
      <div class="form-fields">
        <div class="form-field">
          <label>昵称 *</label>
          <input v-model="form.nickname" placeholder="例如：爷爷、妈妈" />
        </div>
        <div class="form-field">
          <label>家庭关系</label>
          <select v-model="form.relation">
            <option value="" disabled>选择关系</option>
            <option v-for="opt in relationOptions" :key="opt.value" :value="opt.value">
              {{ opt.label }}
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
            <option v-for="opt in roleOptions" :key="opt.value" :value="opt.value">
              {{ opt.label }}
            </option>
          </select>
        </div>
      </div>
      <div class="form-actions">
        <button class="btn-cancel" @click="closeAddForm">取消</button>
        <button class="btn-submit" :disabled="adding" @click="handleAddMember">
          {{ adding ? '添加中...' : '确认添加' }}
        </button>
      </div>
    </div>

    <div v-if="loading" class="loading">加载中...</div>
    <div v-else-if="error" class="error">{{ error }}</div>
    
    <div v-else class="member-table">
      <div class="table-header">
        <span class="col-avatar">头像</span>
        <span class="col-name">昵称</span>
        <span class="col-relation">关系</span>
        <span class="col-phone">手机号</span>
        <span class="col-role">角色</span>
        <span class="col-actions">操作</span>
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
        <div class="col-phone">{{ member.phone || '-' }}</div>
        <div class="col-role">
          <select 
            class="role-select" 
            :class="`role-${member.role}`"
            :value="member.role"
            @change="handleRoleChange(member, parseInt($event.target.value))"
          >
            <option v-for="opt in roleOptions" :key="opt.value" :value="opt.value">
              {{ opt.label }}
            </option>
          </select>
        </div>
        <div class="col-actions">
          <button class="btn-action btn-view" @click="handleView(member)">查看</button>
          <button class="btn-action btn-edit" @click="handleEdit(member)">编辑</button>
          <button 
            class="btn-action btn-delete" 
            @click="handleDelete(member)"
            :disabled="member.userId === userStore.member?.userId"
          >删除</button>
        </div>
      </div>
      
      <div v-if="members.length === 0" class="empty">暂无成员</div>
    </div>

    <!-- 查看弹窗 -->
    <div v-if="showViewModal" class="modal-mask" @click.self="showViewModal = false">
      <div class="modal-content">
        <div class="modal-header">
          <h3>查看成员信息</h3>
          <button class="btn-close" @click="closeViewModal">&times;</button>
        </div>
        <div class="modal-body view-body">
          <div class="view-row">
            <span class="view-label">昵称</span>
            <span class="view-value">{{ viewForm.nickname }}</span>
          </div>
          <div class="view-row">
            <span class="view-label">关系</span>
            <span class="view-value">{{ viewForm.relation || '-' }}</span>
          </div>
          <div class="view-row">
            <span class="view-label">手机号</span>
            <span class="view-value">{{ viewForm.phone || '-' }}</span>
          </div>
          <div class="view-row">
            <span class="view-label">角色</span>
            <span class="view-value">{{ roleLabel(viewForm.role) }}</span>
          </div>
          
          <div v-if="viewForm.birthday || viewForm.height || viewForm.weight || viewForm.disease || viewForm.allergy || viewForm.remark" class="view-section">
            <div class="view-section-title">扩展信息</div>
            <div v-if="viewForm.birthday" class="view-row">
              <span class="view-label">出生日期</span>
              <span class="view-value">{{ viewForm.birthday }} ({{ getAge(viewForm.birthday) }}岁)</span>
            </div>
            <div v-if="viewForm.height" class="view-row">
              <span class="view-label">身高</span>
              <span class="view-value">{{ viewForm.height }} cm</span>
            </div>
            <div v-if="viewForm.weight" class="view-row">
              <span class="view-label">体重</span>
              <span class="view-value">{{ viewForm.weight }} kg</span>
            </div>
            <div v-if="viewForm.disease" class="view-row">
              <span class="view-label">病史</span>
              <span class="view-value">{{ viewForm.disease }}</span>
            </div>
            <div v-if="viewForm.allergy" class="view-row">
              <span class="view-label">过敏史</span>
              <span class="view-value">{{ viewForm.allergy }}</span>
            </div>
            <div v-if="viewForm.remark" class="view-row">
              <span class="view-label">备注</span>
              <span class="view-value">{{ viewForm.remark }}</span>
            </div>
          </div>
          
          <div v-else class="view-empty">暂无扩展信息</div>
        </div>
        <div class="modal-footer">
          <button class="btn-submit" @click="showViewModal = false; handleEdit({userId: viewForm.id})">编辑信息</button>
        </div>
      </div>
    </div>

    <!-- 编辑弹窗 -->
    <div v-if="showEditModal" class="modal-mask" @click.self="showEditModal = false">
      <div class="modal-content">
        <div class="modal-header">
          <h3>编辑成员</h3>
          <button class="btn-close" @click="closeEditModal">&times;</button>
        </div>
        <div class="modal-body">
          <div class="form-field">
            <label>头像</label>
            <AvatarPicker 
              v-model="editForm.avatar" 
              :current-relation="editForm.relation"
              :current-role="editForm.role"
            />
          </div>
          <div class="form-field">
            <label>昵称 *</label>
            <input v-model="editForm.nickname" />
          </div>
          <div class="form-field">
            <label>家庭关系</label>
            <select v-model="editForm.relation">
              <option value="" disabled>选择关系</option>
              <option v-for="opt in relationOptions" :key="opt.value" :value="opt.value">
                {{ opt.label }}
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
              <option v-for="opt in roleOptions" :key="opt.value" :value="opt.value">
                {{ opt.label }}
              </option>
            </select>
          </div>
          
          <div class="form-section-title">扩展信息（选填）</div>
          <div class="form-field">
            <label>出生日期</label>
            <input type="date" v-model="editForm.birthday" />
          </div>
          <div class="form-field-row">
            <div class="form-field">
              <label>身高(cm)</label>
              <input type="number" v-model="editForm.height" placeholder="如：170" />
            </div>
            <div class="form-field">
              <label>体重(kg)</label>
              <input type="number" v-model="editForm.weight" placeholder="如：65" />
            </div>
          </div>
          <div class="form-field">
            <label>病史</label>
            <input v-model="editForm.disease" placeholder="高血压,糖尿病" />
          </div>
          <div class="form-field">
            <label>过敏史</label>
            <input v-model="editForm.allergy" placeholder="如：青霉素" />
          </div>
          <div class="form-field">
            <label>备注</label>
            <textarea v-model="editForm.remark" placeholder="其他说明..." />
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn-cancel" @click="showEditModal = false">取消</button>
          <button class="btn-submit" :disabled="editing" @click="handleUpdate">
            {{ editing ? '保存中...' : '保存' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.admin-members {
  max-width: 100%;
}

.page-header {
  margin-bottom: 24px;
}

.header-row {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}

.page-header h2 {
  margin: 0 0 8px;
  font-size: 1.25rem;
  color: #2d5f5d;
}

.page-desc {
  margin: 0;
  color: #666;
  font-size: 0.9rem;
}

.btn-add {
  padding: 8px 16px;
  background: #2d5f5d;
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 0.9rem;
  cursor: pointer;
}

.btn-add:hover {
  background: #234947;
}

.add-form {
  background: white;
  border-radius: 12px;
  padding: 20px;
  margin-bottom: 20px;
  box-shadow: 0 2px 12px rgba(45, 95, 93, 0.08);
}

.form-title {
  font-size: 1rem;
  font-weight: 600;
  color: #333;
  margin-bottom: 16px;
}

.form-fields {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}

.form-field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.form-field label {
  font-size: 0.85rem;
  color: #666;
}

.form-field input,
.form-field select,
.form-field textarea {
  padding: 10px 12px;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  font-size: 0.9rem;
}

.form-field input::placeholder,
.form-field textarea::placeholder {
  color: #aaa;
}

.form-field input:focus,
.form-field select:focus,
.form-field textarea:focus {
  outline: none;
  border-color: #2d5f5d;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 20px;
}

.btn-cancel {
  padding: 10px 20px;
  background: #f5f5f5;
  color: #666;
  border: none;
  border-radius: 8px;
  font-size: 0.9rem;
  cursor: pointer;
}

.btn-submit {
  padding: 10px 20px;
  background: #2d5f5d;
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 0.9rem;
  cursor: pointer;
}

.btn-submit:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.error {
  color: #e74c3c;
  margin-top: 12px;
  font-size: 0.9rem;
}

.loading, .empty {
  text-align: center;
  padding: 40px;
  color: #666;
}

.member-table {
  background: white;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 2px 12px rgba(45, 95, 93, 0.08);
  width: 100%;
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
  min-width: 100px;
  padding-left: 16px;
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
  flex: 1;
  min-width: 80px;
  color: #666;
}

.col-phone {
  flex: 1;
  min-width: 100px;
  color: #888;
  font-size: 0.85rem;
}

.col-role {
  flex: 1;
  min-width: 80px;
}

.profile-age {
  color: #2d5f5d;
  font-weight: 500;
}

.profile-disease {
  background: #fef3e7;
  color: #d68910;
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 0.7rem;
}

.profile-allergy {
  background: #fdecea;
  color: #c0392b;
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 0.7rem;
}

.profile-empty {
  color: #bbb;
}

.col-actions {
  flex: 1;
  min-width: 160px;
  display: flex;
  gap: 8px;
}

.btn-action {
  flex: 1;
  padding: 6px 8px;
  border: none;
  border-radius: 6px;
  font-size: 0.75rem;
  cursor: pointer;
  text-align: center;
}

.role-select {
  padding: 4px 8px;
  border-radius: 12px;
  font-size: 0.75rem;
  font-weight: 500;
  border: none;
  cursor: pointer;
}

.role-select.role-0 {
  background: #fef3e7;
  color: #d68910;
}

.role-select.role-1 {
  background: #e8f4f8;
  color: #2874a6;
}

.role-select.role-2 {
  background: #fdecea;
  color: #c0392b;
}

.btn-action {
  padding: 6px 12px;
  border: none;
  border-radius: 6px;
  font-size: 0.75rem;
  cursor: pointer;
}

.btn-edit {
  background: #e8f4f8;
  color: #2874a6;
}

.btn-view {
  background: #e4f0ef;
  color: #2d5f5d;
}

.btn-delete {
  background: #fdecea;
  color: #c0392b;
}

.btn-delete:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

/* 弹窗样式 */
.modal-mask {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal-content {
  background: white;
  border-radius: 12px;
  width: 90%;
  max-width: 480px;
  max-height: 90vh;
  overflow: auto;
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid #eee;
}

.modal-header h3 {
  margin: 0;
  font-size: 1.1rem;
  color: #333;
}

.btn-close {
  background: none;
  border: none;
  font-size: 1.5rem;
  color: #999;
  cursor: pointer;
}

.modal-body {
  padding: 20px;
  display: grid;
  gap: 16px;
}

.form-section-title {
  font-size: 0.85rem;
  font-weight: 600;
  color: #2d5f5d;
  padding: 8px 0;
  border-top: 1px dashed #eee;
  margin-top: 8px;
}

/* 查看弹窗 */
.view-body {
  padding: 20px;
}

.view-row {
  display: flex;
  justify-content: space-between;
  padding: 10px 0;
  border-bottom: 1px solid #f0f0f0;
}

.view-label {
  color: #888;
}

.view-value {
  color: #333;
  font-weight: 500;
}

.view-section {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px dashed #ddd;
}

.view-section-title {
  font-size: 0.85rem;
  font-weight: 600;
  color: #2d5f5d;
  margin-bottom: 12px;
}

.view-empty {
  padding: 20px;
  text-align: center;
  color: #aaa;
}

.form-field-row {
  display: flex;
  gap: 12px;
}

.form-field-row .form-field {
  flex: 1;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding: 16px 20px;
  border-top: 1px solid #eee;
}

@media (max-width: 768px) {
  .header-row {
    flex-direction: column;
    gap: 12px;
  }
  
  .btn-add {
    width: 100%;
  }
  
  .form-fields {
    grid-template-columns: 1fr;
  }
  
  .table-header {
    display: none;
  }
  
  .member-table {
    display: flex;
    flex-direction: column;
    gap: 12px;
    background: transparent;
    box-shadow: none;
  }
  
  .table-row {
    display: flex;
    flex-wrap: wrap;
    gap: 12px;
    padding: 16px;
    background: white;
    border-radius: 12px;
    align-items: center;
  }
  
  .col-avatar {
    width: 48px;
    flex-shrink: 0;
  }
  
  .col-name {
    flex: 1;
    font-weight: 600;
    font-size: 1rem;
  }
  
  .self-tag {
    margin-left: 6px;
    font-size: 0.65rem;
    padding: 2px 6px;
  }
  
  .col-relation,
  .col-phone,
  .col-role {
    flex: 1;
    font-size: 0.8rem;
    color: #666;
  }
  
  .col-phone {
    color: #888;
  }
  
  .col-actions {
    width: 100%;
    display: flex;
    gap: 8px;
    margin-top: 8px;
  }
  
  .btn-action {
    flex: 1;
    padding: 8px 12px;
    font-size: 0.85rem;
    text-align: center;
  }
}
</style>
