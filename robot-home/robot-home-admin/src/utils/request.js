import axios from 'axios'
import { ElMessage } from 'element-plus'
import { getToken, removeToken } from './auth'

const DEFAULT_BASE_URL = '/api'

const service = axios.create({
  // 开发环境走 Vite 同源代理，避免浏览器跨域预检失败显示「Network Error」
  baseURL:
    (import.meta.env && import.meta.env.DEV && '/api') ||
    (import.meta.env && import.meta.env.VITE_API_BASE_URL) ||
    DEFAULT_BASE_URL,
  timeout: 30000
})

function redirectToLogin() {
  removeToken()
  const hash = window.location.hash || ''
  if (!hash.startsWith('#/login')) {
    const { origin, pathname, search } = window.location
    window.location.href = `${origin}${pathname}${search}#/login`
  }
}

service.interceptors.request.use(
  (config) => {
    const token = getToken()
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

service.interceptors.response.use(
  (response) => {
    const res = response.data
    // 非 JSON / 空响应
    if (res === undefined || res === null || typeof res !== 'object' || res.code === undefined) {
      return res
    }
    if (res.code === 200) {
      return res.data
    }
    if (res.code === 401) {
      ElMessage.error(res.message || '登录已失效，请重新登录')
      redirectToLogin()
      return Promise.reject(new Error(res.message || '登录已失效'))
    }
    ElMessage.error(res.message || '请求失败')
    return Promise.reject(new Error(res.message || '请求失败'))
  },
  (error) => {
    const status = error && error.response ? error.response.status : 0
    const data = error && error.response ? error.response.data : null
    const message = (data && (data.message || data.error)) || error.message || '网络异常，请稍后重试'
    if (status === 401) {
      ElMessage.error('登录已失效，请重新登录')
      redirectToLogin()
    } else {
      ElMessage.error(message)
    }
    return Promise.reject(error)
  }
)

export default service
