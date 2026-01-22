<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { docAPI, type DocumentInfo } from '@/api/doc'
import { ElMessage } from 'element-plus'
import { marked } from 'marked'

// State
const documents = ref<DocumentInfo[]>([])
const loading = ref(false)
const selectedDocs = ref<number[]>([]) // Use document IDs (numbers)
const analysisResult = ref<{ content: string; loading: boolean } | null>(null)
const mode = ref<'summary' | 'comparison'>('summary')

// Load documents from backend: GET /api/doc/list
onMounted(async () => {
  loading.value = true
  try {
    const response = await docAPI.getDocuments()
    if (response.code === 200 && response.data) {
      documents.value = response.data.records || response.data
    }
  } catch (error) {
    console.error('Failed to load documents:', error)
    ElMessage.error('加载文档列表失败')
  } finally {
    loading.value = false
  }
})

// Run analysis
const runAnalysis = async () => {
  if (selectedDocs.value.length === 0) return
  
  analysisResult.value = { content: '', loading: true }
  
  const token = localStorage.getItem('token') || ''
  
  let url = ''
  let body = {}
  
  // Get the selected document IDs for analysis
  // IMPORTANT: Python RAG uses doc.id.toString() as doc_id (set by Java backend during ingestion)
  const getDocId = (id: number) => String(id)
  
  if (mode.value === 'summary') {
    if (selectedDocs.value.length > 1) {
      ElMessage.warning('摘要模式下仅支持选择单篇文档')
      analysisResult.value = null
      return
    }
    const docId = selectedDocs.value[0]
    if (docId === undefined) return
    url = '/api/analysis/summary'
    body = { docId: getDocId(docId), type: 'summary' }
  } else {
    url = '/api/analysis/comparison'
    body = { docIds: selectedDocs.value.map(id => getDocId(id)), aspects: [] }
  }

  try {
    const response = await fetch(url, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`
      },
      body: JSON.stringify(body)
    })

    if (!response.ok) throw new Error(`API Error: ${response.status}`)

    const reader = response.body?.getReader()
    const decoder = new TextDecoder()

    if (!reader) return

    let buffer = '' // Buffer for incomplete lines
    
    while (true) {
      const { done, value } = await reader.read()
      if (done) break
      
      buffer += decoder.decode(value, { stream: true })
      const lines = buffer.split('\n')
      
      // Keep the last incomplete line in the buffer
      buffer = lines.pop() || ''
      
      for (const line of lines) {
        if (line.startsWith('data:')) {
          const data = line.slice(5).trim() // "data:" or "data: "
          if (data === '[DONE]') {
            analysisResult.value!.loading = false
            return
          }
          // Handle escaped newlines
          const text = data.replace(/\\n/g, '\n')
          analysisResult.value!.content += text
        }
      }
    }
    
    // Process any remaining buffer
    if (buffer.startsWith('data:')) {
      const data = buffer.slice(5).trim()
      if (data !== '[DONE]') {
        const text = data.replace(/\\n/g, '\n')
        analysisResult.value!.content += text
      }
    }
    analysisResult.value!.loading = false
  } catch (e) {
    analysisResult.value!.content += `\nError: ${e}`
    analysisResult.value!.loading = false
    ElMessage.error('分析请求失败')
  }
}

// Format file size
const formatFileSize = (bytes: number) => {
  if (!bytes || bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return Math.round(bytes / Math.pow(k, i) * 100) / 100 + ' ' + sizes[i]
}

// Preprocess markdown to fix common formatting issues
const preprocessMarkdown = (content: string): string => {
  return content
    // Add space after # symbols if missing (fix headers like "##1.标题" -> "## 1.标题")
    .replace(/^(#{1,6})(\S)/gm, '$1 $2')
    // Ensure proper paragraph breaks (double newline)
    .replace(/([^\n])\n([^\n#\-\*\d])/g, '$1\n\n$2')
    // Fix bullet points without proper spacing
    .replace(/^(\s*[-*])(\S)/gm, '$1 $2')
    // Fix numbered list without space
    .replace(/^(\s*\d+\.)(\S)/gm, '$1 $2')
}

// Computed property for rendered markdown
const renderedContent = computed(() => {
  if (!analysisResult.value?.content) return ''
  const processed = preprocessMarkdown(analysisResult.value.content)
  return marked(processed) as string
})
</script>

<template>
  <div class="analysis-container">
    <div class="sidebar">
      <h3>文档选择</h3>
      <div v-if="loading" class="loading-hint">加载中...</div>
      <div v-else-if="documents.length === 0" class="empty-hint">
        暂无文档，请先在"文档管理"页面上传
      </div>
      <div v-else class="doc-list">
        <div v-for="doc in documents" :key="doc.id" class="doc-item">
          <label>
            <input type="checkbox" :value="doc.id" v-model="selectedDocs">
            <span class="doc-name">{{ doc.fileName || doc.title }}</span>
            <span class="doc-size">{{ formatFileSize(doc.fileSize) }}</span>
          </label>
        </div>
      </div>
      
      <div class="actions">
        <button @click="mode = 'summary'; runAnalysis()" :disabled="selectedDocs.length !== 1 || loading">
          生成摘要 (选1篇)
        </button>
        <button @click="mode = 'comparison'; runAnalysis()" :disabled="selectedDocs.length < 2 || loading">
          对比分析 (选多篇)
        </button>
      </div>
    </div>
    
    <div class="content-area">
      <div v-if="!analysisResult" class="placeholder">
        请在左侧选择文档并开始分析
      </div>
      <div v-else class="result-box">
        <h3>{{ mode === 'summary' ? '📝 智能摘要' : '🔍 深度对比报告' }}</h3>
        <div class="markdown-body" v-html="renderedContent"></div>
        <div v-if="analysisResult.loading" class="loading-indicator">
          <span class="dot"></span><span class="dot"></span><span class="dot"></span>
          分析生成中...
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.analysis-container {
  display: flex;
  height: 100vh;
  background: #f5f7fa;
}

.sidebar {
  width: 320px;
  background: white;
  padding: 20px;
  border-right: 1px solid #e0e0e0;
  display: flex;
  flex-direction: column;
}

.loading-hint, .empty-hint {
  padding: 20px;
  text-align: center;
  color: #909399;
}

.doc-list {
  flex: 1;
  overflow-y: auto;
  margin-top: 10px;
}

.doc-item {
  padding: 12px;
  border-bottom: 1px solid #eee;
}

.doc-item label {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
}

.doc-name {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.doc-size {
  font-size: 12px;
  color: #909399;
}

.actions {
  display: flex;
  gap: 10px;
  flex-direction: column;
  padding-top: 20px;
  border-top: 1px solid #eee;
}

button {
  padding: 12px;
  background: #409eff;
  color: white;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-weight: 500;
  transition: all 0.2s;
}

button:hover:not(:disabled) {
  background: #66b1ff;
}

button:disabled {
  background: #a0cfff;
  cursor: not-allowed;
}

.content-area {
  flex: 1;
  padding: 40px;
  overflow-y: auto;
}

.result-box {
  background: white;
  padding: 40px;
  border-radius: 12px;
  box-shadow: 0 4px 20px rgba(0,0,0,0.08);
  min-height: 500px;
}

.result-box h3 {
  margin: 0 0 24px 0;
  font-size: 1.4rem;
  color: #303133;
  border-bottom: 2px solid #409eff;
  padding-bottom: 12px;
}

.placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: #909399;
  font-size: 1.2rem;
}

/* Markdown Styles */
.markdown-body {
  font-size: 15px;
  line-height: 1.8;
  color: #303133;
}

.markdown-body :deep(h1),
.markdown-body :deep(h2),
.markdown-body :deep(h3) {
  margin-top: 24px;
  margin-bottom: 16px;
  font-weight: 600;
  color: #1a1a2e;
}

.markdown-body :deep(h1) { font-size: 1.5em; border-bottom: 1px solid #eee; padding-bottom: 8px; }
.markdown-body :deep(h2) { font-size: 1.3em; }
.markdown-body :deep(h3) { font-size: 1.15em; color: #409eff; }

.markdown-body :deep(p) {
  margin: 12px 0;
}

.markdown-body :deep(ul),
.markdown-body :deep(ol) {
  padding-left: 24px;
  margin: 12px 0;
}

.markdown-body :deep(li) {
  margin: 8px 0;
}

.markdown-body :deep(strong) {
  color: #1a1a2e;
  font-weight: 600;
}

.markdown-body :deep(hr) {
  border: none;
  border-top: 1px solid #e0e0e0;
  margin: 24px 0;
}

.markdown-body :deep(blockquote) {
  border-left: 4px solid #409eff;
  padding: 12px 20px;
  margin: 16px 0;
  background: #f5f7fa;
  border-radius: 0 8px 8px 0;
}

.markdown-body :deep(code) {
  background: #f5f7fa;
  padding: 2px 6px;
  border-radius: 4px;
  font-family: 'Consolas', monospace;
  font-size: 0.9em;
  color: #e74c3c;
}

.markdown-body :deep(pre) {
  background: #2d3436;
  color: #dfe6e9;
  padding: 16px;
  border-radius: 8px;
  overflow-x: auto;
}

.markdown-body :deep(pre code) {
  background: none;
  color: inherit;
}

.markdown-body :deep(table) {
  width: 100%;
  border-collapse: collapse;
  margin: 16px 0;
}

.markdown-body :deep(th),
.markdown-body :deep(td) {
  border: 1px solid #e0e0e0;
  padding: 10px;
  text-align: left;
}

.markdown-body :deep(th) {
  background: #f5f7fa;
  font-weight: 600;
}

/* Loading Animation */
.loading-indicator {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  margin-top: 24px;
  padding: 16px;
  background: linear-gradient(135deg, #667eea15 0%, #764ba215 100%);
  border-radius: 8px;
  color: #667eea;
  font-weight: 500;
}

.loading-indicator .dot {
  width: 8px;
  height: 8px;
  background: #667eea;
  border-radius: 50%;
  animation: bounce 1.4s infinite ease-in-out both;
}

.loading-indicator .dot:nth-child(1) { animation-delay: -0.32s; }
.loading-indicator .dot:nth-child(2) { animation-delay: -0.16s; }
.loading-indicator .dot:nth-child(3) { animation-delay: 0s; }

@keyframes bounce {
  0%, 80%, 100% { transform: scale(0); }
  40% { transform: scale(1); }
}
</style>
