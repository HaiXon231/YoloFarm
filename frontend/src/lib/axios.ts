import axios from 'axios'
import type { ErrorResponse } from '@/types'

const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL || '/api/v1',
  headers: {
    'Content-Type': 'application/json',
  },
})

// Request interceptor: attach JWT token
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('access_token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

// Response interceptor: handle 401 globally
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('access_token')
      localStorage.removeItem('role')
      // Only redirect if not already on login page
      if (!window.location.pathname.includes('/login')) {
        window.location.href = '/login'
      }
    }
    return Promise.reject(error)
  }
)

// Helper to extract error message from API responses
export function getApiErrorMessage(error: unknown): string {
  if (axios.isAxiosError(error) && error.response?.data) {
    const data = error.response.data as ErrorResponse
    return data.details || data.message || 'Đã xảy ra lỗi không xác định.'
  }
  return 'Lỗi kết nối. Vui lòng thử lại.'
}

export function getApiErrorCode(error: unknown): number | null {
  if (axios.isAxiosError(error) && error.response) {
    return error.response.status
  }
  return null
}

export default api
