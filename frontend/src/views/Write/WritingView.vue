<script setup lang="ts">
import { ref, computed } from 'vue'
import { marked } from 'marked'

const content = ref('')
const instruction = ref('polish')
const customContext = ref('')
const isProcessing = ref(false)
const resultContent = ref('')

const tools = [
  { label: '学术润色', value: 'polish', icon: '✨' },
  { label: '智能扩写', value: 'expand', icon: '📝' },
  { label: '续写段落', value: 'continue', icon: '⏩' },
  { label: '语法纠错', value: 'fix_grammar', icon: '✅' }
]

const processText = async () => {
  if (!content.value.trim()) return
  
  isProcessing.value = true
  resultContent.value = ''
  
  const token = localStorage.getItem('token') || ''
  
  try {
    const response = await fetch('/api/writing/process', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`
      },
      body: JSON.stringify({
        text: content.value,
        instruction: instruction.value,
        context: customContext.value
      })
    })

    if (!response.ok) throw new Error('API Error')

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
          const data = line.slice(5).trim()
          if (data === '[DONE]') {
            isProcessing.value = false
            return
          }
          // Handle escaped newlines
          const text = data.replace(/\\n/g, '\n')
          resultContent.value += text
        }
      }
    }
    
    // Process remaining buffer
    if (buffer.startsWith('data:')) {
      const data = buffer.slice(5).trim()
      if (data !== '[DONE]') {
        const text = data.replace(/\\n/g, '\n')
        resultContent.value += text
      }
    }
    isProcessing.value = false
  } catch (e) {
    resultContent.value = `Error: ${e}`
    isProcessing.value = false
  }
}

const applyResult = () => {
  content.value = resultContent.value
  resultContent.value = ''
}

// Templates Logic
const showTemplateSelector = ref(false)
const templates = [
  { 
    id: 'paper', 
    name: '学术论文 (Academic Paper)', 
    structure: '# 标题\n\n## 摘要\n[在此处撰写摘要]\n\n## 引言\n[研究背景与目的]\n\n## 方法\n[描述研究方法]\n\n## 结果\n[展示主要发现]\n\n## 讨论\n[结果分析与意义]\n\n## 结论\n[总结全文]'
  },
  { 
    id: 'report', 
    name: '研究报告 (Research Report)', 
    structure: '# 研究报告\n\n## 1. 概述\n\n## 2. 现状分析\n\n## 3. 问题识别\n\n## 4. 建议方案\n\n## 5. 预期成效' 
  },
  { 
    id: 'review', 
    name: '文献综述 (Literature Review)', 
    structure: '# 文献综述\n\n## 引言\n\n## 关键概念\n\n## 现有研究进展\n\n## 主要争论焦点\n\n## 研究不足与展望' 
  }
]

const applyTemplate = (tpl: any) => {
  if (content.value && !confirm('当前编辑器已有内容，是否覆盖？')) return
  content.value = tpl.structure
  showTemplateSelector.value = false
}

// Preprocess markdown to fix common formatting issues
const preprocessMarkdown = (content: string): string => {
  return content
    .replace(/^(#{1,6})(\S)/gm, '$1 $2')
    .replace(/([^\n])\n([^\n#\-\*\d])/g, '$1\n\n$2')
    .replace(/^(\s*[-*])(\S)/gm, '$1 $2')
    .replace(/^(\s*\d+\.)(\S)/gm, '$1 $2')
}

// Computed property for rendered markdown result
const renderedResult = computed(() => {
  if (!resultContent.value) return ''
  const processed = preprocessMarkdown(resultContent.value)
  return marked(processed) as string
})
</script>

<template>
  <div class="writing-container">
    <div class="editor-area">
      <div class="toolbar">
        <button class="tool-btn" @click="showTemplateSelector = true">📂 使用模版</button>
        <div class="spacer"></div>
        <span class="word-count">字数: {{ content.length }}</span>
      </div>
      
      <textarea 
        v-model="content" 
        placeholder="在此输入您的学术文本... 或点击上方'使用模版'开始"
        class="main-editor"
      ></textarea>
      
      <div v-if="resultContent" class="result-preview">
        <div class="preview-header">
          <span>AI 建议结果</span>
          <div>
            <button @click="applyResult" class="apply-btn">采纳</button>
            <button @click="resultContent = ''" class="cancel-btn">取消</button>
          </div>
        </div>
        <div class="preview-body markdown-body" v-html="renderedResult"></div>
      </div>
    </div>
    
    <div class="tools-sidebar">
      <h3>AI 写作工具</h3>
      
      <div class="tool-grid">
        <div 
          v-for="tool in tools" 
          :key="tool.value"
          class="tool-card"
          :class="{ active: instruction === tool.value }"
          @click="instruction = tool.value"
        >
          <span class="icon">{{ tool.icon }}</span>
          <span class="label">{{ tool.label }}</span>
        </div>
      </div>
      
      <div class="context-input">
        <label>额外背景/要求 (可选)</label>
        <textarea v-model="customContext" placeholder="例如：使用更正式的语气..."></textarea>
      </div>
      
      <button 
        class="action-btn" 
        :disabled="isProcessing || !content"
        @click="processText"
      >
        {{ isProcessing ? '处理中...' : '开始处理' }}
      </button>
    </div>
    <!-- Template Selector Dialog -->
    <div v-if="showTemplateSelector" class="modal-overlay" @click.self="showTemplateSelector = false">
      <div class="modal">
        <h3>选择写作模版</h3>
        <div class="template-list">
          <div 
            v-for="tpl in templates" 
            :key="tpl.id" 
            class="template-item"
            @click="applyTemplate(tpl)"
          >
            <h4>{{ tpl.name }}</h4>
            <pre>{{ tpl.structure.slice(0, 50) }}...</pre>
          </div>
        </div>
        <button class="close-btn" @click="showTemplateSelector = false">取消</button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.toolbar {
  display: flex;
  align-items: center;
  margin-bottom: 10px;
  padding: 5px;
  background: #f5f7fa;
  border-radius: 4px;
}

.tool-btn {
  padding: 6px 12px;
  background: white;
  border: 1px solid #ddd;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
}

.tool-btn:hover {
  border-color: #409eff;
  color: #409eff;
}

.spacer {
  flex: 1;
}

.word-count {
  color: #909399;
  font-size: 12px;
}

.writing-container {
  display: flex;
  height: 100vh;
  background: white;
}

.editor-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  padding: 40px;
  position: relative;
}

.main-editor {
  flex: 1;
  width: 100%;
  border: 1px solid #ddd;
  padding: 20px;
  font-size: 16px;
  line-height: 1.6;
  resize: none;
  border-radius: 8px;
  outline: none;
  transition: border-color 0.3s;
}

.main-editor:focus {
  border-color: #409eff;
}

.tools-sidebar {
  width: 320px;
  background: #f9f9f9;
  border-left: 1px solid #e0e0e0;
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.tool-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}

.tool-card {
  background: white;
  border: 1px solid #eee;
  padding: 15px;
  border-radius: 8px;
  cursor: pointer;
  text-align: center;
  transition: all 0.2s;
}

.tool-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0,0,0,0.05);
}

.tool-card.active {
  border-color: #409eff;
  background: #ecf5ff;
  color: #409eff;
}

.icon {
  display: block;
  font-size: 24px;
  margin-bottom: 5px;
}

.context-input textarea {
  width: 100%;
  height: 80px;
  margin-top: 5px;
  padding: 8px;
  border: 1px solid #ddd;
  border-radius: 4px;
}

.action-btn {
  width: 100%;
  padding: 12px;
  background: #409eff;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-weight: bold;
}

.action-btn:disabled {
  background: #a0cfff;
}

.result-preview {
  position: absolute;
  top: 40px;
  right: 40px;
  left: 40px;
  bottom: 40px;
  background: rgba(255, 255, 255, 0.98);
  border: 1px solid #409eff;
  border-radius: 8px;
  padding: 20px;
  display: flex;
  flex-direction: column;
  box-shadow: 0 8px 24px rgba(0,0,0,0.12);
}

.preview-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
  border-bottom: 1px solid #eee;
  padding-bottom: 10px;
}

.preview-body {
  flex: 1;
  overflow-y: auto;
  font-size: 16px;
  line-height: 1.8;
  padding: 10px 0;
}

/* Markdown Styles for WritingView */
.preview-body.markdown-body :deep(h1),
.preview-body.markdown-body :deep(h2),
.preview-body.markdown-body :deep(h3) {
  margin-top: 16px;
  margin-bottom: 12px;
  font-weight: 600;
  color: #303133;
}

.preview-body.markdown-body :deep(h1) { font-size: 1.4em; }
.preview-body.markdown-body :deep(h2) { font-size: 1.25em; }
.preview-body.markdown-body :deep(h3) { font-size: 1.1em; color: #409eff; }

.preview-body.markdown-body :deep(p) {
  margin: 10px 0;
}

.preview-body.markdown-body :deep(ul),
.preview-body.markdown-body :deep(ol) {
  padding-left: 20px;
  margin: 10px 0;
}

.preview-body.markdown-body :deep(li) {
  margin: 6px 0;
}

.preview-body.markdown-body :deep(strong) {
  font-weight: 600;
  color: #303133;
}

.preview-body.markdown-body :deep(code) {
  background: #f5f7fa;
  padding: 2px 5px;
  border-radius: 3px;
  font-size: 0.9em;
}

.apply-btn {
  background: #67c23a;
  color: white;
  border: none;
  padding: 6px 12px;
  border-radius: 4px;
  margin-right: 10px;
  cursor: pointer;
}

.cancel-btn {
  background: #909399;
  color: white;
  border: none;
  padding: 6px 12px;
  border-radius: 4px;
  cursor: pointer;
}

/* Modal Styles */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0,0,0,0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal {
  background: white;
  padding: 24px;
  border-radius: 8px;
  width: 500px;
  max-width: 90%;
}

.modal h3 {
  margin-top: 0;
  margin-bottom: 20px;
}

.template-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-bottom: 20px;
}

.template-item {
  padding: 15px;
  border: 1px solid #eee;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s;
}

.template-item:hover {
  background: #f5f7fa;
  border-color: #409eff;
}

.template-item h4 {
  margin: 0 0 5px 0;
  color: #303133;
}

.template-item pre {
  margin: 0;
  color: #909399;
  font-size: 12px;
  background: transparent;
  padding: 0;
}

.close-btn {
  width: 100%;
  padding: 10px;
  background: #f5f5f5;
  border: none;
  border-radius: 4px;
  cursor: pointer;
}
</style>
