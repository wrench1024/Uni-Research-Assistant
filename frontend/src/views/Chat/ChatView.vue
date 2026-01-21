<template>
  <div class="chat-layout">
    <!-- Sidebar for Session History -->
    <div class="session-sidebar">
      <div class="sidebar-header">
        <div class="brand">
          <div class="brand-inner">
            <el-icon :size="20"><Cpu /></el-icon>
          </div>
          <span>对话历史</span>
        </div>
        <el-button 
          type="primary" 
          class="new-chat-btn"
          @click="handleNewSession"
        >
          <el-icon><Plus /></el-icon>
          <span>新建对话</span>
        </el-button>
      </div>
      
      <el-scrollbar class="session-list-wrapper">
        <div v-if="chatStore.sessionList.length === 0" class="empty-sessions">
          <el-empty :image-size="40" description="开启您的第一场对话" />
        </div>
        <div 
          v-for="session in chatStore.sessionList" 
          :key="session.id"
          class="session-item"
          :class="{ active: chatStore.currentSessionId === session.id }"
          @click="handleSessionClick(session.id)"
        >
          <div class="session-info">
            <el-icon class="msg-icon"><ChatRound /></el-icon>
            <div class="session-text">
              <span class="session-title">{{ session.title }}</span>
              <span class="session-time">{{ formatTime(session.updateTime) }}</span>
            </div>
          </div>
          <div class="session-actions">
            <el-tooltip content="编辑名称" placement="top">
              <el-button 
                link 
                class="action-btn"
                :icon="EditPen" 
                @click.stop="openRenameDialog(session)"
              />
            </el-tooltip>
            <el-tooltip content="删除对话" placement="top">
              <el-popconfirm
                title="确定删除此对话？"
                width="200"
                @confirm="handleDeleteSession(session.id)"
              >
                <template #reference>
                  <el-button 
                    link 
                    class="action-btn delete"
                    :icon="Delete" 
                    @click.stop
                  />
                </template>
              </el-popconfirm>
            </el-tooltip>
          </div>
        </div>
      </el-scrollbar>

      <div class="sidebar-footer">
        <el-button link class="refresh-btn" @click="chatStore.fetchSessions">
          <el-icon><RefreshRight /></el-icon> 刷新记录
        </el-button>
      </div>
    </div>

    <!-- Main Chat Window -->
    <div class="chat-main">
      <div class="chat-header">
        <div class="header-content">
          <div class="chat-info">
            <h2 class="current-title">{{ currentSessionTitle }}</h2>
            <el-button 
              v-if="chatStore.currentSessionId"
              link 
              class="edit-title-btn"
              :icon="EditPen"
              @click="openRenameDialog(currentSession)"
            />
          </div>
          <div class="header-actions">
            <el-tag v-if="chatStore.isStreaming" type="primary" size="small" class="streaming-tag">
              <el-icon class="is-loading"><Loading /></el-icon> 正在生成
            </el-tag>
          </div>
        </div>
      </div>

      <!-- Messages Area -->
      <div class="messages-viewport">
        <el-scrollbar ref="scrollbarRef" class="messages-scrollbar">
          <div class="messages-inner">
            <!-- Empty State -->
            <div v-if="chatStore.messages.length === 0" class="welcome-hero">
              <div class="hero-icon">
                <el-icon :size="48"><MagicStick /></el-icon>
              </div>
              <h1 class="hero-title">您好！我是您的智能科研助手</h1>
              <p class="hero-subtitle">我可以为您总结文献、编写代码或者解答任何学术难题。</p>
              
              <div class="suggested-grid">
                <div 
                  v-for="prompt in quickPrompts" 
                  :key="prompt"
                  class="suggest-card"
                  @click="handleQuickPrompt(prompt)"
                >
                  <el-icon class="suggest-icon"><Pointer /></el-icon>
                  <span>{{ prompt }}</span>
                </div>
              </div>
            </div>

            <!-- Message List -->
            <div
              v-for="(message, index) in chatStore.messages"
              :key="index"
              class="message-row"
              :class="message.role"
            >
              <div class="avatar-container">
                <el-avatar v-if="message.role === 'user'" :size="32" class="user-avatar">
                  <el-icon><User /></el-icon>
                </el-avatar>
                <el-avatar v-else :size="32" class="ai-avatar">
                  <el-icon><Cpu /></el-icon>
                </el-avatar>
              </div>
              
              <div class="bubble-wrapper">
                <div class="bubble-info" v-if="message.role === 'assistant'">
                  <span class="bot-name">AI Assistant</span>
                </div>
                <div 
                  class="message-bubble"
                  :class="{ 'markdown-body': message.role === 'assistant' }"
                  v-html="message.role === 'user' ? message.content : renderMarkdown(message.content || '正在深度思考中...')"
                ></div>
              </div>
            </div>

            <!-- Typing indicator -->
            <div v-if="chatStore.isStreaming && lastMessageEmpty" class="status-indicator">
              <div class="dot-flashing"></div>
              <span>AI 正在输入...</span>
            </div>
          </div>
        </el-scrollbar>
      </div>

      <!-- Floating Input Area -->
      <div class="input-container">
        <div class="input-toolbar" v-if="chatStore.isStreaming">
          <el-button 
            type="danger" 
            size="small" 
            plain 
            round 
            @click="handleStopGeneration"
            class="stop-btn"
          >
            <el-icon><CircleClose /></el-icon> 停止生成
          </el-button>
        </div>
        
        <div class="input-box-wrapper">
          <el-input
            v-model="inputMessage"
            type="textarea"
            :autosize="{ minRows: 1, maxRows: 6 }"
            placeholder="问我任何问题... (Shift + Enter 换行，Ctrl + Enter 发送)"
            resize="none"
            class="main-input"
            @keydown.ctrl.enter="handleSendMessage"
          />
          <div class="send-action">
            <el-button
              type="primary"
              :disabled="!inputMessage.trim() || chatStore.isStreaming"
              class="send-btn"
              @click="handleSendMessage"
            >
              <el-icon :size="20"><Top /></el-icon>
            </el-button>
          </div>
        </div>
        <p class="input-footer-hint">AI 生成的内容仅供参考，请注意核实。</p>
      </div>
    </div>

    <!-- Rename Dialog -->
    <el-dialog
      v-model="renameDialogVisible"
      title="重命名对话"
      width="360px"
      align-center
      class="custom-dialog"
    >
      <el-input 
        v-model="renameTitle" 
        placeholder="输入新标题..." 
        clearable
        @keyup.enter="handleRename"
      />
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="renameDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleRename" :loading="renaming">确认</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, nextTick, watch, computed, onMounted } from 'vue'
import { useChatStore } from '@/stores/chatStore'
import { 
  Plus, User, Cpu, Top, CircleClose, Loading,
  MagicStick, Pointer, ChatRound, Delete, EditPen, RefreshRight
} from '@element-plus/icons-vue'
import type { ScrollbarInstance } from 'element-plus'
import MarkdownIt from 'markdown-it'
import { ElMessage } from 'element-plus'
import dayjs from 'dayjs'

const chatStore = useChatStore()

// Markdown renderer configuration
const md = new MarkdownIt({
  html: false,
  linkify: true,
  typographer: true,
  breaks: true
})

// Preprocess markdown
function preprocessMarkdown(content: string): string {
  if (!content) return ''
  let processed = content
  processed = processed.replace(/^(#{1,6})([^\s#])/gm, '$1 $2')
  processed = processed.replace(/^-([^\s-])/gm, '- $1')
  processed = processed.replace(/^\*([^\s*])/gm, '* $1')
  processed = processed.replace(/^(\d+\.)([^\s])/gm, '$1 $2')
  return processed
}

function renderMarkdown(content: string): string {
  if (!content) return ''
  const unescaped = content.replace(/\\n/g, '\n')
  const preprocessed = preprocessMarkdown(unescaped)
  return md.render(preprocessed)
}

// State
const inputMessage = ref('')
const scrollbarRef = ref<ScrollbarInstance>()
const renameDialogVisible = ref(false)
const renameTitle = ref('')
const renaming = ref(false)
const targetSessionId = ref<number | null>(null)

// Computed
const currentSession = computed(() => {
  return chatStore.sessionList.find(s => s.id === chatStore.currentSessionId)
})

const currentSessionTitle = computed(() => {
  return currentSession.value ? currentSession.value.title : '新建对话'
})

const lastMessageEmpty = computed(() => {
  const msgs = chatStore.messages
  if (msgs.length === 0) return false
  const last = msgs[msgs.length - 1]
  return last && last.role === 'assistant' && !last.content
})

const quickPrompts = [
  '帮我总结最近三年的 AI 发展',
  '写一段高效的 Python 爬虫代码',
  '如何写出一份漂亮的科研计划书？',
  '解释量子纠缠的基本概念'
]

// Utilities
const formatTime = (time: any) => {
  if (!time) return ''
  const date = dayjs(time)
  if (date.isSame(dayjs(), 'day')) return date.format('HH:mm')
  return date.format('MM-DD')
}

// Watchers
watch(() => chatStore.messages, () => {
  nextTick(scrollToBottom)
}, { deep: true })

onMounted(() => {
  chatStore.fetchSessions()
})

// Handlers
const handleNewSession = () => {
  chatStore.clearSession()
  inputMessage.value = ''
}

const handleSessionClick = (sessionId: number) => {
  if (chatStore.isStreaming || chatStore.currentSessionId === sessionId) return
  chatStore.loadSessionMessages(sessionId)
}

const handleDeleteSession = async (sessionId: number) => {
  await chatStore.deleteSession(sessionId)
}

const openRenameDialog = (session: any) => {
  targetSessionId.value = session.id
  renameTitle.value = session.title
  renameDialogVisible.value = true
}

const handleRename = async () => {
  if (!targetSessionId.value || !renameTitle.value.trim()) return
  renaming.value = true
  const success = await chatStore.updateSessionTitle(targetSessionId.value, renameTitle.value.trim())
  renaming.value = false
  if (success) {
    renameDialogVisible.value = false
    ElMessage.success('名称已更新')
  }
}

const handleSendMessage = async () => {
  if (!inputMessage.value.trim() || chatStore.isStreaming) return
  const msg = inputMessage.value.trim()
  inputMessage.value = ''
  await chatStore.sendMessage(msg)
}

const handleStopGeneration = () => {
  chatStore.stopGeneration()
}

const handleQuickPrompt = (prompt: string) => {
  inputMessage.value = prompt
}

const scrollToBottom = () => {
  if (scrollbarRef.value) {
    const wrap = scrollbarRef.value.wrapRef
    if (wrap) {
      wrap.scrollTo({
        top: wrap.scrollHeight,
        behavior: 'smooth'
      })
    }
  }
}
</script>

<style scoped>
.chat-layout {
  display: flex;
  height: 100%;
  background-color: #f7f9fc;
  color: #2c3e50;
  overflow: hidden;
}

/* --- Sidebar Style --- */
.session-sidebar {
  width: 280px;
  background: white;
  display: flex;
  flex-direction: column;
  border-right: 1px solid #eef2f7;
  transition: all 0.3s ease;
}

.sidebar-header {
  padding: 24px 20px;
}

.brand {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 24px;
  font-weight: 700;
  font-size: 18px;
  color: #1a1a1a;
}

.brand-inner {
  width: 36px;
  height: 36px;
  background: linear-gradient(135deg, #007aff 0%, #00c6ff 100%);
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  box-shadow: 0 4px 12px rgba(0, 122, 255, 0.3);
}

.new-chat-btn {
  width: 100%;
  height: 44px;
  border-radius: 12px;
  background-color: #f0f7ff;
  border: 1px dashed #007aff;
  color: #007aff;
  font-weight: 600;
  transition: all 0.2s;
}

.new-chat-btn:hover {
  background-color: #007aff;
  color: white;
  transform: translateY(-1px);
}

.session-list-wrapper {
  flex: 1;
  padding: 0 12px;
}

.session-item {
  padding: 12px;
  margin-bottom: 8px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  cursor: pointer;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
  border: 1px solid transparent;
}

.session-item:hover {
  background-color: #f8fafc;
  border-color: #e2e8f0;
}

.session-item.active {
  background-color: #f0f7ff;
  border-color: #007aff33;
}

.session-info {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 1;
  min-width: 0;
}

.msg-icon {
  font-size: 18px;
  color: #94a3b8;
}

.session-item.active .msg-icon {
  color: #007aff;
}

.session-text {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.session-title {
  font-size: 14px;
  font-weight: 500;
  color: #334155;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.session-item.active .session-title {
  color: #007aff;
}

.session-time {
  font-size: 11px;
  color: #94a3b8;
  margin-top: 2px;
}

.session-actions {
  display: flex;
  gap: 4px;
  opacity: 0;
  transform: scale(0.9);
  transition: all 0.2s;
}

.session-item:hover .session-actions {
  opacity: 1;
  transform: scale(1);
}

.sidebar-footer {
  padding: 16px;
  border-top: 1px solid #f1f5f9;
}

.refresh-btn {
  width: 100%;
  color: #94a3b8;
  font-size: 13px;
}

/* --- Main View Style --- */
.chat-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  position: relative;
  background: white;
}

.chat-header {
  height: 64px;
  padding: 0 24px;
  display: flex;
  align-items: center;
  border-bottom: 1px solid #f1f5f9;
  background: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(10px);
  z-index: 10;
}

.header-content {
  width: 100%;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.chat-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.current-title {
  font-size: 16px;
  font-weight: 600;
  margin: 0;
  color: #1e293b;
}

.edit-title-btn {
  font-size: 16px;
  color: #94a3b8;
}

.messages-viewport {
  flex: 1;
  overflow: hidden;
  position: relative;
}

.messages-inner {
  max-width: 1000px; /* Slightly wider for better breathing room */
  margin: 0 auto;
  padding: 40px 40px 140px;
}

/* Welcome Hero */
.welcome-hero {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 60px 0;
  animation: slideUp 0.6s ease-out;
}

@keyframes slideUp {
  from { opacity: 0; transform: translateY(20px); }
  to { opacity: 1; transform: translateY(0); }
}

.hero-icon {
  width: 80px;
  height: 80px;
  background: #f0f7ff;
  border-radius: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #007aff;
  margin-bottom: 24px;
}

.hero-title {
  font-size: 28px;
  font-weight: 700;
  margin-bottom: 12px;
  color: #1e293b;
}

.hero-subtitle {
  color: #64748b;
  margin-bottom: 40px;
}

.suggested-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
  width: 100%;
  max-width: 600px;
}

.suggest-card {
  padding: 16px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 14px;
  color: #475569;
  transition: all 0.2s;
}

.suggest-card:hover {
  background: white;
  border-color: #007aff;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
  transform: translateY(-2px);
}

.suggest-icon {
  color: #007aff;
}

/* Message Rows */
.message-row {
  display: flex;
  gap: 20px; /* Increased back from 16 */
  margin-bottom: 40px;
}

.message-row.user {
  flex-direction: row-reverse;
}

.avatar-container {
  flex-shrink: 0;
  margin-top: 4px;
}

.user-avatar { background: #334155 !important; }
.ai-avatar { background: linear-gradient(135deg, #007aff 0%, #00c6ff 100%) !important; }

.bubble-wrapper {
  display: flex;
  flex-direction: column;
  max-width: calc(100% - 120px);
}

.message-row.user .bubble-wrapper {
  align-items: flex-end;
}

.bubble-info {
  margin-bottom: 8px;
  margin-left: 2px;
}

.bot-name {
  font-size: 11px;
  font-weight: 700;
  color: #94a3b8;
  text-transform: uppercase;
  letter-spacing: 1px;
}

.message-bubble {
  padding: 18px 28px; /* High horizontal padding for internal spacing */
  border-radius: 20px;
  font-size: 15.5px;
  line-height: 1.7;
  position: relative;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
  transition: background 0.2s;
}

.message-row.assistant .message-bubble {
  background: #f8fafc;
  color: #1e293b;
  border: 1px solid #edf2f7;
  border-top-left-radius: 4px;
}

.message-row.user .message-bubble {
  background: #007aff;
  color: white;
  border-top-right-radius: 4px;
  white-space: pre-wrap;
  word-break: break-word;
}

/* Markdown Specifics - Key indentation fixes */
.markdown-body {
  word-break: break-word;
}

.markdown-body :deep(h1),
.markdown-body :deep(h2),
.markdown-body :deep(h3),
.markdown-body :deep(h4) {
  margin: 24px 0 12px;
  border: none;
  font-weight: 700;
  line-height: 1.4;
}

.markdown-body :deep(p) { margin-bottom: 16px; }
.markdown-body :deep(p:last-child) { margin-bottom: 0; }

/* Fixed padding for lists inside bubbles */
.markdown-body :deep(ul),
.markdown-body :deep(ol) {
  margin: 12px 0 16px;
  padding-left: 24px; /* Ensure bullets/numbers have their own horizontal space */
}

.markdown-body :deep(li) {
  margin: 8px 0;
}

.markdown-body :deep(li > p) {
  margin-bottom: 4px;
}

.markdown-body :deep(pre) {
  background: #1e293b !important;
  color: #f8fafc;
  border-radius: 12px;
  padding: 18px 24px;
  margin: 20px 0;
  overflow-x: auto;
}

.markdown-body :deep(code) {
  font-family: 'Fira Code', 'JetBrains Mono', monospace;
  padding: 3px 6px;
  border-radius: 5px;
  font-size: 0.9em;
  background: #eff6ff;
  color: #2563eb;
  border: 1px solid rgba(37, 99, 235, 0.1);
}

.markdown-body :deep(pre code) {
  background: none;
  border: none;
  color: inherit;
  font-size: 13.5px;
  padding: 0;
}

.markdown-body :deep(table) {
  border-radius: 10px;
  overflow: hidden;
  border: 1px solid #e2e8f0;
  margin: 20px 0;
  font-size: 14px;
}

.markdown-body :deep(th) {
  background: #f1f5f9;
  padding: 12px 16px;
  font-weight: 700;
  text-align: left;
}

.markdown-body :deep(td) {
  padding: 10px 16px;
  border-top: 1px solid #f1f5f9;
}

.markdown-body :deep(blockquote) {
  margin: 20px 0;
  padding: 10px 20px;
  border-left: 4px solid #007aff;
  background: #f0f7ff;
  color: #475569;
  border-radius: 0 8px 8px 0;
}

/* Status Indicator */
.status-indicator {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 24px;
  background: #f1f5f9;
  border-radius: 20px;
  width: fit-content;
  font-size: 13px;
  color: #64748b;
  margin-left: 52px;
}

.dot-flashing {
  position: relative;
  width: 6px;
  height: 6px;
  border-radius: 5px;
  background-color: #007aff;
  color: #007aff;
  animation: dotFlashing 1s infinite linear alternate;
  animation-delay: .5s;
}

.dot-flashing::before, .dot-flashing::after {
  content: '';
  display: inline-block;
  position: absolute;
  top: 0;
}

.dot-flashing::before {
  left: -10px;
  width: 6px;
  height: 6px;
  border-radius: 5px;
  background-color: #007aff;
  color: #007aff;
  animation: dotFlashing 1s infinite linear alternate;
  animation-delay: 0s;
}

.dot-flashing::after {
  left: 10px;
  width: 6px;
  height: 6px;
  border-radius: 5px;
  background-color: #007aff;
  color: #007aff;
  animation: dotFlashing 1s infinite linear alternate;
  animation-delay: 1s;
}

@keyframes dotFlashing {
  0% { background-color: #007aff; }
  50%, 100% { background-color: #f1f5f9; }
}

/* Floating Input */
.input-container {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  padding: 0 24px 28px;
  background: linear-gradient(to top, white 80%, transparent);
}

.input-toolbar {
  display: flex;
  justify-content: center;
  margin-bottom: 14px;
}

.input-box-wrapper {
  max-width: 860px;
  margin: 0 auto;
  position: relative;
  background: white;
  border-radius: 18px;
  box-shadow: 0 15px 40px rgba(0, 0, 0, 0.08), 0 0 1px rgba(0, 0, 0, 0.1);
  padding: 10px 14px;
  display: flex;
  align-items: flex-end;
  border: 1px solid #eef2f7;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.input-box-wrapper:focus-within {
  border-color: #007aff;
  box-shadow: 0 20px 50px rgba(0, 122, 255, 0.12);
}

.main-input :deep(.el-textarea__inner) {
  background: transparent !important;
  border: none !important;
  box-shadow: none !important;
  padding: 8px 14px;
  font-size: 15.5px;
  max-height: 200px;
  color: #1e293b;
  line-height: 1.5;
}

.send-action {
  padding-bottom: 4px;
}

.send-btn {
  width: 42px;
  height: 42px;
  border-radius: 12px;
  padding: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 12px rgba(0, 122, 255, 0.2);
  transition: all 0.2s;
}

.send-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(0, 122, 255, 0.3);
}

.input-footer-hint {
  text-align: center;
  font-size: 11px;
  color: #94a3b8;
  margin-top: 12px;
}

.custom-dialog :deep(.el-dialog) {
  border-radius: 16px;
  padding: 10px;
}

.custom-dialog :deep(.el-input__inner) {
  height: 44px;
  border-radius: 10px;
}
</style>
