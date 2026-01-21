import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { authAPI, type LoginRequest, type UserInfo } from '@/api/auth'
import { ElMessage } from 'element-plus'

export const useUserStore = defineStore('user', () => {
    // State
    const token = ref<string>(localStorage.getItem('token') || '')
    const userInfo = ref<UserInfo | null>(null)

    // Getters
    const isLoggedIn = computed(() => !!token.value)
    const username = computed(() => userInfo.value?.username || '')

    // Actions

    /**
     * User login
     */
    async function login(credentials: LoginRequest) {
        try {
            const response = await authAPI.login(credentials)

            if (response.code === 200 && response.data) {
                // Save token and user info
                token.value = response.data.token
                userInfo.value = response.data.user

                // Persist to localStorage
                localStorage.setItem('token', response.data.token)
                localStorage.setItem('userInfo', JSON.stringify(response.data.user))

                ElMessage.success('登录成功')
                return true
            } else {
                ElMessage.error(response.message || '登录失败')
                return false
            }
        } catch (error) {
            console.error('Login error:', error)
            ElMessage.error('登录失败，请稍后重试')
            return false
        }
    }

    /**
     * User logout
     */
    async function logout() {
        try {
            await authAPI.logout()
        } catch (error) {
            console.error('Logout error:', error)
        } finally {
            // Clear state and localStorage
            token.value = ''
            userInfo.value = null
            localStorage.removeItem('token')
            localStorage.removeItem('userInfo')

            ElMessage.success('已退出登录')
        }
    }

    /**
     * Load user data from localStorage
     */
    function loadUserFromLocalStorage() {
        const savedToken = localStorage.getItem('token')
        const savedUserInfo = localStorage.getItem('userInfo')

        if (savedToken) {
            token.value = savedToken
        }

        if (savedUserInfo) {
            try {
                userInfo.value = JSON.parse(savedUserInfo)
            } catch (error) {
                console.error('Failed to parse user info:', error)
            }
        }
    }

    /**
     * Fetch current user info
     */
    async function fetchUserInfo() {
        try {
            const response = await authAPI.getUserInfo()
            if (response.code === 200 && response.data) {
                userInfo.value = response.data
                localStorage.setItem('userInfo', JSON.stringify(response.data))
            }
        } catch (error) {
            console.error('Failed to fetch user info:', error)
        }
    }

    // Initialize from localStorage
    loadUserFromLocalStorage()

    return {
        // State
        token,
        userInfo,

        // Getters
        isLoggedIn,
        username,

        // Actions
        login,
        logout,
        loadUserFromLocalStorage,
        fetchUserInfo
    }
})
