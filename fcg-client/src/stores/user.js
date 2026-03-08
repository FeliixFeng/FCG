import { defineStore } from 'pinia'
import { getToken, getFamilyToken, setFamilyToken, getMemberToken, setMemberToken, clearToken } from '../utils/storage'
import http from '../utils/http'

export const useUserStore = defineStore('user', {
  state: () => ({
    // 家庭级 token（登录后）
    familyToken: getFamilyToken(),
    // 成员级 token（选人后）
    memberToken: getMemberToken(),
    // 家庭信息
    family: null,
    // 当前选中的成员信息
    member: null,
  }),

  getters: {
    // 是否已登录（有家庭级 token）
    isLoggedIn: (state) => !!state.familyToken,
    // 是否已选人（有成员级 token）
    hasMember: (state) => !!state.memberToken,
    // 当前 token（优先成员级）
    token: (state) => state.memberToken || state.familyToken,
    // 是否为管理员
    isAdmin: (state) => state.member?.role === 0,
    // 是否为关怀模式
    isCareMode: (state) => state.member?.careMode === 1,
  },

  actions: {
    /** 家庭账号登录，获取家庭级 token */
    async login(payload) {
      const res = await http.post('/api/family/login', payload)
      this.familyToken = res.data.token
      this.family = res.data
      setFamilyToken(res.data.token)
      return res.data
    },

    /** 选择成员，获取成员级 token */
    async switchMember(memberId) {
      const res = await http.post(`/api/family/switch-member/${memberId}`)
      this.memberToken = res.data.token
      this.member = res.data
      setMemberToken(res.data.token)
      return res.data
    },

    /** 获取家庭成员列表（选人页用） */
    async fetchMembers() {
      const res = await http.get('/api/family/members')
      return res.data
    },

    /** 获取当前成员信息 */
    async fetchProfile() {
      const res = await http.get('/api/user/info')
      this.member = res.data
      return res.data
    },

    /** 获取家庭信息（刷新后恢复 family 状态） */
    async fetchFamilyInfo() {
      const res = await http.get('/api/family/info')
      this.family = res.data
      return res.data
    },

    /** 退出登录，清除所有状态 */
    logout() {
      this.familyToken = ''
      this.memberToken = ''
      this.family = null
      this.member = null
      clearToken()
    },

    /** 退出当前成员（回到选人页） */
    exitMember() {
      this.memberToken = ''
      this.member = null
      localStorage.removeItem('fcg_member_token')
    }
  }
})
