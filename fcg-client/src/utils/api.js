import http from './http'

// ========== 文件模块 ==========

/** 上传文件到 OSS */
export const uploadFile = (file, dir = 'avatar') => {
  const formData = new FormData()
  formData.append('file', file)
  return http.post(`/api/oss/upload?dir=${dir}`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

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

/** 获取成员详情（管理员） */
export const fetchMemberDetail = (userId) => http.get(`/api/user/${userId}`)

/** 更新成员信息（管理员） */
export const updateMember = (userId, data) => http.put(`/api/user/${userId}`, data)

/** 删除成员（管理员） */
export const deleteMember = (userId) => http.delete(`/api/user/${userId}`)

/** 修改成员角色（管理员） */
export const updateMemberRole = (userId, role) => http.put(`/api/user/${userId}/role?role=${role}`)

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

export const recognizeMedicine = (files) => {
  const formData = new FormData()
  files.forEach(file => formData.append('files', file))
  return http.post('/api/medicine/ocr', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

/** 今日计划记录联表（含 medicineName、planDosage 等）*/
export const fetchTodayPlanRecords = (scheduledDate, userId) =>
  http.get('/api/medicine/plan/records', { params: { scheduledDate, userId, page: 1, size: 50 } })

/** 更新服药记录状态（打卡/跳过） */
export const updateMedicineRecord = (id, data) => http.put(`/api/medicine/record/${id}`, data)

export const createPlan = (data) => http.post('/api/medicine/plan', data)

export const deletePlan = (id) => http.delete(`/api/medicine/plan/${id}`)

export const fetchPlanList = (params) => http.get('/api/medicine/plan/list', { params })

// ========== 健康模块 ==========

/** 近一周体征 */
export const fetchWeeklyVitals = (userId, type) =>
  http.get('/api/health/vital/weekly', { params: { userId, type } })

/** 今日最新体征（每种类型一条） */
export const fetchTodayVitals = (userId) =>
  http.get('/api/health/vital/today', { params: { userId } })

/** 体征记录列表（分页） */
export const fetchVitalList = (params) =>
  http.get('/api/health/vital/list', { params })

/** 新增体征记录 */
export const createVital = (data) => http.post('/api/health/vital', data)

/** 删除体征记录 */
export const deleteVital = (id) => http.delete(`/api/health/vital/${id}`)

/** 获取成员健康档案 */
export const fetchUserProfile = (userId) => 
  http.get('/api/user/profile', { params: { userId } })

/** 健康周报列表 */
export const fetchHealthReports = (params) => http.get('/api/health/report/list', { params })

/** 生成健康周报 */
export const generateHealthReport = (userId) => 
  http.post('/api/health/report/generate', null, { params: { userId } })

/** 获取最新周报 */
export const fetchLatestReport = (userId) => 
  http.get('/api/health/report/latest', { params: { userId } })
