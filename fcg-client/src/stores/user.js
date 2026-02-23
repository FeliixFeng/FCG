import { defineStore } from 'pinia'
import { getToken, setToken, clearToken } from '../utils/storage'
import http from '../utils/http'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: getToken(),
    profile: null
  }),
  actions: {
    async login(payload) {
      const res = await http.post('/api/user/login', payload)
      this.token = res.data.token
      setToken(this.token)
      this.profile = res.data
      return res.data
    },
    async fetchProfile() {
      const res = await http.get('/api/user/info')
      this.profile = res.data
      return res.data
    },
    logout() {
      this.token = ''
      this.profile = null
      clearToken()
    }
  }
})
