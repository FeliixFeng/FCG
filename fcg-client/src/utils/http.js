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
      // 清除所有 token
      clearToken()
      
      // 清除关怀模式预览状态
      sessionStorage.removeItem('fcg_preview_care')
      
      // 清除 Pinia 持久化状态（如果有的话）
      localStorage.removeItem('pinia-user')
      
      // 强制跳转到登录页，清空所有状态
      window.location.href = '/'
    }
    return Promise.reject(error)
  }
)

export default http
