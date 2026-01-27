// WriteAPI - 写作服务

// 写作 API 类型定义
export interface WriteRequest {
    text: string
    instruction: 'polish' | 'expand' | 'continue' | 'fix_grammar'
    context?: string
}

// 写作 API 服务 - 对接 Python 后端 /api/v1/write/process
export const writeAPI = {
    /**
     * 处理写作请求 (流式响应)
     * Python Backend: POST /api/v1/write/process
     * @param params 请求参数
     * @param onChunk 每次接收到内容时的回调
     * @param onDone 完成时的回调
     * @param onError 错误时的回调
     */
    async processStream(
        params: WriteRequest,
        onChunk: (text: string) => void,
        onDone: () => void,
        onError: (error: Error) => void
    ): Promise<void> {
        const token = localStorage.getItem('token') || ''

        try {
            // 直接使用 Python AI 服务地址
            const response = await fetch('http://localhost:8000/api/v1/write/process', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${token}`
                },
                body: JSON.stringify(params)
            })

            if (!response.ok) {
                throw new Error(`API Error: ${response.status}`)
            }

            const reader = response.body?.getReader()
            const decoder = new TextDecoder()

            if (!reader) {
                throw new Error('Failed to get response reader')
            }

            let buffer = ''

            while (true) {
                const { done, value } = await reader.read()
                if (done) break

                buffer += decoder.decode(value, { stream: true })
                const lines = buffer.split('\n')
                buffer = lines.pop() || ''

                for (const line of lines) {
                    if (line.startsWith('data:')) {
                        const data = line.slice(5).trim()
                        if (data === '[DONE]') {
                            onDone()
                            return
                        }
                        // 处理转义的换行符
                        const text = data.replace(/\\n/g, '\n')
                        onChunk(text)
                    }
                }
            }

            // 处理剩余 buffer
            if (buffer.startsWith('data:')) {
                const data = buffer.slice(5).trim()
                if (data !== '[DONE]') {
                    const text = data.replace(/\\n/g, '\n')
                    onChunk(text)
                }
            }
            onDone()
        } catch (e) {
            onError(e instanceof Error ? e : new Error(String(e)))
        }
    }
}

export default writeAPI
