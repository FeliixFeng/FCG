import axios from 'axios'
import { getToken, clearToken } from './storage'

const http = axios.create({
  baseURL: '',
  timeout: 10000
})

http.interceptors.request.use((config) => {
  const token = getToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

http.interceptors.response.use(
  (response) => {
    const { data } = response
    if (data && data.code !== undefined && data.code !== 200) {
      const error = new Error(data.msg || '请求失败')
      error.code = data.code
      throw error
    }
    return data
  },
  (error) => {
    if (error.response && error.response.status === 401) {
      // 清除 localStorage 中的 token
      clearToken()
      
      // 同步清除 Pinia store 状态并跳转登录页
      // 注意：在 interceptor 中不能直接 import useUserStore，会造成循环依赖
      // 解决方案：通过事件通知或直接操作 window.location
      if (typeof window !== 'undefined') {
        // 清除 Pinia 持久化状态（如果有的话）
        localStorage.removeItem('pinia-user')
        
        // 强制跳转到登录页，清空所有状态
        window.location.href = '/'
      }
    }
    return Promise.reject(error)
  }
)

export default http
