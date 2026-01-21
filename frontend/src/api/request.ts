import axios, { type AxiosInstance, type AxiosError, type InternalAxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'

// Create axios instance with default config
const service: AxiosInstance = axios.create({
    baseURL: '/api',  // Will be proxied to http://localhost:8080/api by Vite
    timeout: 30000,   // 30 seconds timeout
    headers: {
        'Content-Type': 'application/json'
    }
})

// Request interceptor - inject JWT token
service.interceptors.request.use(
    (config: InternalAxiosRequestConfig) => {
        // Get token from localStorage
        const token = localStorage.getItem('token')

        // Inject Authorization header if token exists
        if (token && config.headers) {
            config.headers.Authorization = `Bearer ${token}`
        }

        return config
    },
    (error: AxiosError) => {
        console.error('Request Error:', error)
        return Promise.reject(error)
    }
)

// Response interceptor - handle errors
service.interceptors.response.use(
    (response) => {
        // Return response data directly
        return response.data
    },
    (error: AxiosError) => {
        console.error('Response Error:', error)

        // Handle different error status codes
        if (error.response) {
            const status = error.response.status

            switch (status) {
                case 401:
                    // Unauthorized - clear token and redirect to login
                    localStorage.removeItem('token')
                    localStorage.removeItem('userInfo')
                    ElMessage.error('登录已过期，请重新登录')
                    window.location.href = '/login'
                    break
                case 403:
                    ElMessage.error('没有权限访问该资源')
                    break
                case 404:
                    ElMessage.error('请求的资源不存在')
                    break
                case 500:
                    ElMessage.error('服务器错误，请稍后重试')
                    break
                default:
                    ElMessage.error(error.response.data?.message || '请求失败')
            }
        } else if (error.request) {
            ElMessage.error('网络连接失败，请检查网络设置')
        } else {
            ElMessage.error('请求配置错误')
        }

        return Promise.reject(error)
    }
)

export default service
