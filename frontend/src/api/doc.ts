import request from './request'

// Document types
export interface DocumentInfo {
    id: number
    userId?: number
    title: string
    fileName: string
    filePath: string
    fileSize: number
    fileType?: string
    status?: number
    createTime?: string
    updateTime?: string
}

export interface DocumentPage {
    records: DocumentInfo[]
    total: number
    size: number
    current: number
}

// Document API services - Matches backend /doc/* endpoints
export const docAPI = {
    /**
     * Upload document
     * Backend: POST /doc/upload
     */
    uploadDocument(file: File, onProgress?: (progress: number) => void): Promise<any> {
        const formData = new FormData()
        formData.append('file', file)

        return request.post('/doc/upload', formData, {
            headers: {
                'Content-Type': 'multipart/form-data'
            },
            onUploadProgress: (progressEvent) => {
                if (onProgress && progressEvent.total) {
                    const progress = Math.round((progressEvent.loaded * 100) / progressEvent.total)
                    onProgress(progress)
                }
            }
        })
    },

    /**
     * Get documents list
     * Backend: GET /doc/list
     */
    getDocuments(page: number = 1, size: number = 10, keyword?: string): Promise<any> {
        return request.get('/doc/list', {
            params: { page, size, keyword }
        })
    },

    /**
     * Download document - opens in new tab (backend streams file)
     * Backend: GET /doc/{id}/download
     */
    downloadDocument(docId: number, filename: string): void {
        const token = localStorage.getItem('token')
        const url = `/api/doc/${docId}/download`

        // Create a temporary link and trigger download
        const link = document.createElement('a')
        link.href = url
        link.download = filename
        link.click()
    },

    /**
     * Delete document
     * Backend: DELETE /doc/{id}
     */
    deleteDocument(docId: number): Promise<any> {
        return request.delete(`/doc/${docId}`)
    }
}

export default docAPI
