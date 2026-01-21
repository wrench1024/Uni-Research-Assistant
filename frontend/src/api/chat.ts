import request from './request'

// Chat message types
export interface ChatMessage {
    id?: number
    sessionId?: number
    role: 'user' | 'assistant'
    content: string
    createTime?: string
}

export interface ChatSession {
    id: number
    userId?: number
    title: string
    createTime?: string
    updateTime?: string
}

export interface ChatSendRequest {
    sessionId?: number
    content: string
}

// Chat API services - Matches backend /chat/* endpoints
export const chatAPI = {
    /**
     * Create new chat session
     * Backend: POST /chat/session
     */
    createSession(title: string = '新会话'): Promise<any> {
        return request.post('/chat/session', null, {
            params: { title }
        })
    },

    /**
     * Get messages for a session
     * Backend: GET /chat/session/{sessionId}/messages
     */
    getSessionMessages(sessionId: number): Promise<any> {
        return request.get(`/chat/session/${sessionId}/messages`)
    },

    /**
     * Send message and receive SSE streaming response
     * Backend: POST /chat/send (returns SSE stream)
     * Note: We use fetch API for SSE support with POST
     */
    async sendMessage(sessionId: number | null, content: string, signal?: AbortSignal): Promise<Response> {
        const token = localStorage.getItem('token')
        const body: ChatSendRequest = {
            content
        }
        if (sessionId) {
            body.sessionId = sessionId
        }

        return fetch('/api/chat/send', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}`
            },
            body: JSON.stringify(body),
            signal
        })
    },

    /**
     * Get user's chat sessions list
     * Backend: GET /chat/sessions
     */
    getSessions(): Promise<any> {
        return request.get('/chat/sessions')
    },

    /**
     * Delete a chat session
     * Backend: DELETE /chat/session/{sessionId}
     */
    deleteSession(sessionId: number): Promise<any> {
        return request.delete(`/chat/session/${sessionId}`)
    },

    /**
     * Update chat session title
     * Backend: PUT /chat/session/{sessionId}?title=xxx
     */
    updateSessionTitle(sessionId: number, title: string): Promise<any> {
        return request.put(`/chat/session/${sessionId}`, null, {
            params: { title }
        })
    }
}

export default chatAPI
