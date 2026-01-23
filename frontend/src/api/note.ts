import request from './request'

// Note types
export interface NoteInfo {
    id?: number
    userId?: number
    docId: number
    content: string
    noteType?: string
    position?: string
    tags?: string
    createTime?: string
    updateTime?: string
}

// Note API services
export const noteAPI = {
    /**
     * Create note
     * Backend: POST /note
     */
    createNote(note: NoteInfo): Promise<any> {
        return request.post('/note', note)
    },

    /**
     * Update note
     * Backend: PUT /note/{id}
     */
    updateNote(id: number, note: NoteInfo): Promise<any> {
        return request.put(`/note/${id}`, note)
    },

    /**
     * Delete note
     * Backend: DELETE /note/{id}
     */
    deleteNote(id: number): Promise<any> {
        return request.delete(`/note/${id}`)
    },

    /**
     * Get document notes
     * Backend: GET /note/doc/{docId}
     */
    getDocumentNotes(docId: number): Promise<any> {
        return request.get(`/note/doc/${docId}`)
    },

    /**
     * Search notes
     * Backend: GET /note/search?keyword=xxx
     */
    searchNotes(keyword: string): Promise<any> {
        return request.get('/note/search', {
            params: { keyword }
        })
    }
}

export default noteAPI
