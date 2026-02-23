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
      clearToken()
    }
    return Promise.reject(error)
  }
)

export default http
