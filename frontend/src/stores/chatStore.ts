import { defineStore } from 'pinia'
import { ref } from 'vue'
import { chatAPI, type ChatMessage, type ChatSession } from '@/api/chat'
import { ElMessage } from 'element-plus'

// Timeout configuration (30 seconds)
const REQUEST_TIMEOUT = 30000

export const useChatStore = defineStore('chat', () => {
    // State
    const currentSessionId = ref<number | null>(null)
    const messages = ref<ChatMessage[]>([])
    const sessionList = ref<ChatSession[]>([])
    const isStreaming = ref<boolean>(false)

    // AbortController for cancelling requests
    let currentController: AbortController | null = null

    // Actions

    /**
     * Stop the current generation
     */
    function stopGeneration() {
        if (currentController) {
            currentController.abort()
            currentController = null
        }
        isStreaming.value = false

        // Update the last assistant message if it's empty
        const msgs = messages.value
        if (msgs.length > 0) {
            const lastMsg = msgs[msgs.length - 1]
            if (lastMsg && lastMsg.role === 'assistant' && !lastMsg.content) {
                lastMsg.content = '⏹️ 已停止生成'
            }
        }

        ElMessage.info('已停止生成')
    }

    /**
     * Send message and stream AI response with timeout handling
     */
    async function sendMessage(content: string) {
        if (!content.trim() || isStreaming.value) return

        // Add user message locally
        const userMessage: ChatMessage = {
            role: 'user',
            content: content.trim()
        }
        messages.value.push(userMessage)

        // Prepare assistant message placeholder
        const assistantMessage: ChatMessage = {
            role: 'assistant',
            content: ''
        }
        messages.value.push(assistantMessage)

        isStreaming.value = true

        // Create timeout controller
        currentController = new AbortController()
        const timeoutId = setTimeout(() => {
            if (currentController) {
                currentController.abort()
            }
        }, REQUEST_TIMEOUT)

        try {
            // Call backend SSE endpoint
            const response = await chatAPI.sendMessage(currentSessionId.value, content, currentController.signal)

            clearTimeout(timeoutId)

            if (!response.ok) {
                const errorText = await response.text().catch(() => '')
                throw new Error(`服务器错误 (${response.status}): ${errorText || '请检查后端日志'}`)
            }

            const reader = response.body?.getReader()
            const decoder = new TextDecoder()

            if (!reader) {
                throw new Error('无法读取响应流')
            }

            let buffer = ''

            try {
                let streamFinished = false
                while (true) {
                    const { done, value } = await reader.read()
                    if (done) break

                    buffer += decoder.decode(value, { stream: true })

                    // Process complete lines
                    const lines = buffer.split('\n')
                    buffer = lines.pop() || ''

                    for (const line of lines) {
                        const trimmedLine = line.trim()
                        if (trimmedLine.startsWith('data:')) {
                            const data = trimmedLine.substring(5).trim()
                            if (data === '[DONE]') {
                                streamFinished = true
                                break
                            }
                            if (data) {
                                // Check for sessionId (first event from backend)
                                if (data.startsWith('{"sessionId":')) {
                                    try {
                                        const parsed = JSON.parse(data)
                                        if (parsed.sessionId) {
                                            currentSessionId.value = parsed.sessionId
                                        }
                                    } catch {
                                        // Not sessionId, treat as content
                                        const unescaped = data.replace(/\\n/g, '\n')
                                        assistantMessage.content += unescaped
                                    }
                                } else {
                                    // Plain text content - unescape newlines
                                    const unescaped = data.replace(/\\n/g, '\n')
                                    assistantMessage.content += unescaped
                                }
                            }
                        }
                    }

                    if (streamFinished) break
                }

                // Process remaining buffer
                if (buffer.trim().startsWith('data:')) {
                    const data = buffer.trim().substring(5).trim()
                    if (data && data !== '[DONE]' && !data.startsWith('{"sessionId":')) {
                        const unescaped = data.replace(/\\n/g, '\n')
                        assistantMessage.content += unescaped
                    }
                }
            } finally {
                reader.releaseLock()
            }

            isStreaming.value = false
            currentController = null

            if (!assistantMessage.content) {
                assistantMessage.content = '⚠️ AI 未返回内容。\n\n可能原因：\n1. GEMINI_API_KEY 未设置\n2. API Key 无效或已过期\n3. 网络连接问题\n\n请检查 Python 终端日志获取详细错误信息。'
                ElMessage.warning('AI 未返回有效内容')
            }

            // Refresh session list if it was a new session
            fetchSessions()

        } catch (error: any) {
            console.error('Send message error:', error)
            isStreaming.value = false
            currentController = null
            clearTimeout(timeoutId)

            // Handle different error types
            if (error.name === 'AbortError') {
                if (!assistantMessage.content) {
                    assistantMessage.content = '⏱️ 请求超时或已取消\n\n请检查：\n1. Python AI 服务是否运行 (端口 8000)\n2. GEMINI_API_KEY 是否有效\n3. 网络连接是否正常'
                }
                // Don't show error message if user manually stopped
            } else if (error.message?.includes('Failed to fetch') || error.message?.includes('NetworkError')) {
                assistantMessage.content = '🔌 网络连接失败\n\n请检查：\n1. 后端服务是否运行 (端口 8080)\n2. AI 服务是否运行 (端口 8000)'
                ElMessage.error('网络连接失败')
            } else {
                assistantMessage.content = `❌ 请求失败: ${error.message || '未知错误'}`
                ElMessage.error('消息发送失败')
            }
        }
    }

    /**
     * Fetch user sessions
     */
    async function fetchSessions() {
        try {
            const response = await chatAPI.getSessions()
            if (response.code === 200 && response.data) {
                sessionList.value = response.data
            }
        } catch (error) {
            console.error('Failed to fetch sessions:', error)
        }
    }

    /**
     * Delete a session
     */
    async function deleteSession(sessionId: number) {
        try {
            const response = await chatAPI.deleteSession(sessionId)
            if (response.code === 200) {
                ElMessage.success('会话已删除')
                // If it's the current session, clear it
                if (currentSessionId.value === sessionId) {
                    clearSession()
                }
                // Refresh list
                await fetchSessions()
            }
        } catch (error) {
            console.error('Failed to delete session:', error)
            ElMessage.error('删除会话失败')
        }
    }

    /**
     * Load messages for a session
     */
    async function loadSessionMessages(sessionId: number) {
        try {
            const response = await chatAPI.getSessionMessages(sessionId)
            if (response.code === 200 && response.data) {
                messages.value = response.data
                currentSessionId.value = sessionId
            }
        } catch (error) {
            console.error('Failed to load messages:', error)
            ElMessage.error('加载聊天记录失败')
        }
    }

    /**
     * Create new chat session
     */
    async function createNewSession(title: string = '新会话') {
        try {
            const response = await chatAPI.createSession(title)
            if (response.code === 200 && response.data) {
                currentSessionId.value = response.data.id
                messages.value = []
                await fetchSessions() // Refresh list
                ElMessage.success('已创建新会话')
                return response.data
            }
        } catch (error) {
            console.error('Failed to create session:', error)
            ElMessage.error('创建会话失败')
        }
        return null
    }

    /**
     * Clear current session (local only)
     */
    function clearSession() {
        messages.value = []
        currentSessionId.value = null
        if (currentController) {
            currentController.abort()
            currentController = null
        }
        isStreaming.value = false
    }

    /**
     * Update session title
     */
    async function updateSessionTitle(sessionId: number, title: string) {
        try {
            const response = await chatAPI.updateSessionTitle(sessionId, title)
            if (response.code === 200) {
                // Refresh list
                await fetchSessions()
                return true
            }
        } catch (error) {
            console.error('Failed to update title:', error)
            ElMessage.error('更新标题失败')
        }
        return false
    }

    /**
     * Rollback messages (delete last N messages)
     */
    async function rollbackMessages(count: number) {
        if (!currentSessionId.value) return false
        try {
            const response = await chatAPI.rollbackHistory(currentSessionId.value, count)
            return response.code === 200
        } catch (error) {
            console.error('Failed to rollback messages:', error)
            return false
        }
    }

    return {
        // State
        currentSessionId,
        messages,
        sessionList,
        isStreaming,

        // Actions
        sendMessage,
        stopGeneration,
        fetchSessions,
        deleteSession,
        updateSessionTitle,
        loadSessionMessages,
        createNewSession,
        clearSession,
        rollbackMessages
    }
})
