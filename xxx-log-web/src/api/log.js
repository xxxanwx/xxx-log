import axios from 'axios'
import { getToken, clearAuth } from '../utils/auth'

const http = axios.create({
  baseURL: '/api',
  timeout: 30000
})

http.interceptors.request.use((config) => {
  const token = getToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

http.interceptors.response.use(
  (resp) => {
    const body = resp.data
    const isLoginRequest = resp.config.url?.includes('/auth/login')
    if (body.code === 401 && !isLoginRequest) {
      clearAuth()
      if (!window.location.pathname.startsWith('/login')) {
        window.location.href = '/login'
      }
      return Promise.reject(new Error(body.message || '未登录'))
    }
    if (body.code !== 200) {
      return Promise.reject(new Error(body.message || '请求失败'))
    }
    return body.data
  },
  (err) => {
    if (err.response?.status === 401) {
      clearAuth()
      if (!window.location.pathname.startsWith('/login')) {
        window.location.href = '/login'
      }
    }
    return Promise.reject(err)
  }
)

export function login(username, password) {
  return http.post('/auth/login', { username, password })
}

export function logout() {
  return http.post('/auth/logout')
}

export function getCurrentUser() {
  return http.get('/auth/me')
}

export function searchLogs(params) {
  return http.post('/logs/search', params)
}

export function getTraceLogs(traceId, appName, startTime, endTime) {
  return http.get(`/logs/trace/${traceId}`, {
    params: { appName, startTime, endTime }
  })
}

export function listApps() {
  return http.get('/logs/apps')
}

export function getQueueStats() {
  return http.get('/logs/queue/stats')
}

export function listLogIndices() {
  return http.get('/logs/indices')
}

export function deleteLogIndex(indexName) {
  return http.delete(`/logs/indices/${encodeURIComponent(indexName)}`)
}
