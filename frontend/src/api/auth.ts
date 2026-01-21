import request from './request'

// Login request/response types
export interface LoginRequest {
    username: string
    password: string
}

export interface LoginResponse {
    code: number
    message: string
    data: {
        token: string
        user: UserInfo
    }
}

export interface UserInfo {
    id: number
    username: string
    email?: string
    createdAt?: string
}

// Auth API services
export const authAPI = {
    /**
     * User login
     */
    login(credentials: LoginRequest): Promise<LoginResponse> {
        return request.post('/auth/login', credentials)
    },

    /**
     * User logout
     */
    logout(): Promise<any> {
        return request.post('/auth/logout')
    },

    /**
     * Get current user info
     */
    getUserInfo(): Promise<any> {
        return request.get('/auth/user')
    },

    /**
     * User registration (if needed)
     */
    register(data: { username: string; password: string; email?: string }): Promise<any> {
        return request.post('/auth/register', data)
    }
}

export default authAPI
