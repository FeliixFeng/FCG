// 家庭级 token（登录后获取，用于选人页）
const FAMILY_TOKEN_KEY = 'fcg_family_token'
// 成员级 token（选人后获取，用于业务接口）
const MEMBER_TOKEN_KEY = 'fcg_member_token'

export const getFamilyToken = () => localStorage.getItem(FAMILY_TOKEN_KEY) || ''
export const setFamilyToken = (token) => localStorage.setItem(FAMILY_TOKEN_KEY, token)

export const getMemberToken = () => localStorage.getItem(MEMBER_TOKEN_KEY) || ''
export const setMemberToken = (token) => localStorage.setItem(MEMBER_TOKEN_KEY, token)

/** 获取当前有效 token（优先用成员级，没有则用家庭级） */
export const getToken = () => getMemberToken() || getFamilyToken()

/** 清除所有 token（退出登录时调用） */
export const clearToken = () => {
  localStorage.removeItem(FAMILY_TOKEN_KEY)
  localStorage.removeItem(MEMBER_TOKEN_KEY)
}
