import axios, { AxiosError, type AxiosInstance, type AxiosResponse, type InternalAxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'
import type { Result } from '@/types'

// Create axios instance with base configuration
const request: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// Request interceptor - add auth token to requests
request.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = localStorage.getItem('token')
    if (token && config.headers) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error: AxiosError) => {
    console.error('Request error:', error)
    return Promise.reject(error)
  }
)

// Response interceptor - handle responses and errors
request.interceptors.response.use(
  (response: AxiosResponse<Result<unknown>>) => {
    const res = response.data
    
    // Check if response indicates success (code 0 or 200)
    if (res.code === 0 || res.code === 200) {
      return response
    }
    
    // Handle business logic errors
    ElMessage.error(res.message || 'Request failed')
    return Promise.reject(new Error(res.message || 'Request failed'))
  },
  (error: AxiosError<Result<unknown>>) => {
    const status = error.response?.status
    const message = error.response?.data?.message || error.message
    
    switch (status) {
      case 401:
        // Unauthorized - clear auth and redirect to login
        localStorage.removeItem('token')
        ElMessage.error('Session expired, please login again')
        // Redirect to login page
        window.location.href = '/login'
        break
      case 403:
        ElMessage.error('Access denied')
        break
      case 404:
        ElMessage.error('Resource not found')
        break
      case 429:
        ElMessage.warning('Too many requests, please try again later')
        break
      case 500:
        ElMessage.error('Server error, please try again later')
        break
      default:
        ElMessage.error(message || 'Network error')
    }
    
    return Promise.reject(error)
  }
)

export default request
