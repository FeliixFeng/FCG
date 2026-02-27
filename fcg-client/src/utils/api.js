import http from './http'

// ========== 家庭模块 ==========

/** 家庭注册 */
export const registerFamily = (data) => http.post('/api/family/register', data)

/** 家庭账号登录 */
export const loginFamily = (data) => http.post('/api/family/login', data)

/** 获取家庭信息 */
export const fetchFamilyInfo = () => http.get('/api/family/info')

/** 获取家庭成员列表（选人页用） */
export const fetchFamilyMembers = () => http.get('/api/family/members')

/** 选择成员，返回成员级 token */
export const switchMember = (memberId) => http.post(`/api/family/switch-member/${memberId}`)

/** 验证管理员密码 */
export const verifyAdmin = (password) => http.post(`/api/family/verify-admin?password=${encodeURIComponent(password)}`)

/** 添加家庭成员（仅管理员） */
export const addMember = (data) => http.post('/api/family/add-member', data)

// ========== 成员模块 ==========

/** 获取当前成员信息 */
export const fetchUserInfo = () => http.get('/api/user/info')

/** 更新成员信息 */
export const updateUserInfo = (data) => http.put('/api/user/update', data)

/** 切换关怀模式 */
export const switchCareMode = (mode) => http.put(`/api/user/care-mode/${mode}`)

// ========== 药品模块 ==========

/** 药品列表（分页） */
export const fetchMedicineList = (params) => http.get('/api/medicine/list', { params })

/** 新增药品 */
export const createMedicine = (data) => http.post('/api/medicine', data)

/** 更新药品 */
export const updateMedicine = (id, data) => http.put(`/api/medicine/${id}`, data)

/** 删除药品 */
export const deleteMedicine = (id) => http.delete(`/api/medicine/${id}`)

/** 药品详情 */
export const fetchMedicine = (id) => http.get(`/api/medicine/${id}`)
