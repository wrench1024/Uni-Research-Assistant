<template>
  <div class="doc-container">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>文档列表</span>
          <el-upload
            :action="uploadAction"
            :headers="uploadHeaders"
            :show-file-list="false"
            :before-upload="beforeUpload"
            :on-success="handleUploadSuccess"
            :on-error="handleUploadError"
          >
            <el-button type="primary" :icon="Upload" :loading="uploading">
              上传文档
            </el-button>
          </el-upload>
        </div>
      </template>

      <!-- Upload progress -->
      <div v-if="uploading" class="upload-progress">
        <el-progress :percentage="uploadProgress" />
        <span class="progress-text">正在上传...</span>
      </div>

      <el-table :data="documents" v-loading="loading" stripe style="width: 100%">
        <el-table-column prop="title" label="文件名" min-width="200">
          <template #default="{ row }">
            <div style="display: flex; align-items: center; gap: 8px;">
              <el-icon :size="20" :color="getFileIconColor(row.fileType)">
                <component :is="getFileIcon(row.fileType)" />
              </el-icon>
              <el-text truncated>{{ row.fileName || row.title }}</el-text>
            </div>
          </template>
        </el-table-column>

        <el-table-column prop="fileSize" label="文件大小" width="120">
          <template #default="{ row }">
            {{ formatFileSize(row.fileSize) }}
          </template>
        </el-table-column>

        <el-table-column prop="createTime" label="上传时间" width="180">
          <template #default="{ row }">
            {{ formatDateTime(row.createTime) }}
          </template>
        </el-table-column>

        <el-table-column label="操作" width="320" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" :icon="Download" link @click="handleDownload(row)">
              下载
            </el-button>
            <el-button type="warning" size="small" :icon="EditPen" link @click="handleShowNotes(row)">
              笔记
            </el-button>
            <el-button type="success" size="small" :icon="Document" link @click="handleShowCitation(row)">
              引用
            </el-button>
            <el-popconfirm
              title="确定要删除这个文档吗？"
              @confirm="handleDelete(row.id)"
            >
              <template #reference>
                <el-button type="danger" size="small" :icon="Delete" link>
                  删除
                </el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>

        <template #empty>
          <el-empty description="暂无文档" />
        </template>
      </el-table>
    </el-card>

    <!-- Citation Dialog -->
    <CitationDialog
      v-model:visible="citationDialogVisible"
      :document-id="selectedDocId"
      @saved="loadDocuments"
    />

    <!-- Note Panel Drawer -->
    <el-drawer
      v-model="noteDrawerVisible"
      title="文档笔记"
      size="450px"
      direction="rtl"
    >
      <NotePanel v-if="noteDrawerVisible" :doc-id="selectedDocId" />
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { docAPI, type DocumentInfo } from '@/api/doc'
import { Upload, Download, Delete, Document, Picture, VideoCamera, FolderOpened, EditPen } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import CitationDialog from '@/components/CitationDialog.vue'
import NotePanel from '@/components/NotePanel.vue'

// State
const documents = ref<DocumentInfo[]>([])
const loading = ref(false)
const uploading = ref(false)
const uploadProgress = ref(0)
const citationDialogVisible = ref(false)
const noteDrawerVisible = ref(false)
const selectedDocId = ref(0)

// Get file icon based on file type
const getFileIcon = (fileType?: string) => {
  if (!fileType) return Document
  
  const type = fileType.toLowerCase()
  
  // Image types
  if (['jpg', 'jpeg', 'png', 'gif', 'bmp', 'svg', 'webp'].includes(type)) {
    return Picture
  }
  
  // Video types
  if (['mp4', 'avi', 'mov', 'wmv', 'flv', 'mkv'].includes(type)) {
    return VideoCamera
  }
  
  // Archive types
  if (['zip', 'rar', '7z', 'tar', 'gz'].includes(type)) {
    return FolderOpened
  }
  
  // Default to document icon
  return Document
}

// Get file icon color based on file type
const getFileIconColor = (fileType?: string) => {
  if (!fileType) return '#909399'
  
  const type = fileType.toLowerCase()
  
  // Image - blue
  if (['jpg', 'jpeg', 'png', 'gif', 'bmp', 'svg', 'webp'].includes(type)) {
    return '#409EFF'
  }
  
  // Video - red
  if (['mp4', 'avi', 'mov', 'wmv', 'flv', 'mkv'].includes(type)) {
    return '#F56C6C'
  }
  
  // PDF - purple
  if (type === 'pdf') {
    return '#667eea'
  }
  
  // Word - blue
  if (['doc', 'docx'].includes(type)) {
    return '#2b579a'
  }
  
  // Excel - green
  if (['xls', 'xlsx'].includes(type)) {
    return '#217346'
  }
  
  // Archive - orange
  if (['zip', 'rar', '7z', 'tar', 'gz'].includes(type)) {
    return '#E6A23C'
  }
  
  // Default - gray
  return '#909399'
}

// Computed - Backend endpoint: POST /api/doc/upload
const uploadAction = computed(() => '/api/doc/upload')
const uploadHeaders = computed(() => {
  const token = localStorage.getItem('token')
  return {
    Authorization: `Bearer ${token}`
  }
})

// Load documents on mount
onMounted(() => {
  loadDocuments()
})

// Load documents from backend: GET /api/doc/list
const loadDocuments = async () => {
  loading.value = true
  try {
    const response = await docAPI.getDocuments()
    if (response.code === 200 && response.data) {
      // Handle paginated response
      documents.value = response.data.records || response.data
    }
  } catch (error) {
    console.error('Failed to load documents:', error)
    ElMessage.error('加载文档列表失败')
  } finally {
    loading.value = false
  }
}

// Before upload
const beforeUpload = (file: File) => {
  const maxSize = 100 * 1024 * 1024 // 100MB
  
  if (file.size > maxSize) {
    ElMessage.error('文件大小不能超过 100MB')
    return false
  }
  
  uploading.value = true
  uploadProgress.value = 0
  return true
}

// Upload success
const handleUploadSuccess = (response: any) => {
  uploading.value = false
  uploadProgress.value = 100
  
  if (response.code === 200) {
    ElMessage.success('文档上传成功')
    loadDocuments()
  } else {
    ElMessage.error(response.message || '文档上传失败')
  }
}

// Upload error
const handleUploadError = () => {
  uploading.value = false
  ElMessage.error('文档上传失败')
}

// Download document: GET /api/doc/{id}/download
const handleDownload = async (doc: DocumentInfo) => {
  try {
    await docAPI.downloadDocument(doc.id, doc.fileName || doc.title)
  } catch (error) {
    console.error('Download failed:', error)
    ElMessage.error('下载失败')
  }
}

// Delete document: DELETE /api/doc/{id}
const handleDelete = async (docId: number) => {
  try {
    const response = await docAPI.deleteDocument(docId)
    if (response.code === 200) {
      ElMessage.success('文档已删除')
      loadDocuments()
    } else {
      ElMessage.error(response.message || '删除失败')
    }
  } catch (error) {
    console.error('Delete failed:', error)
    ElMessage.error('删除失败')
  }
}

// Show citation dialog
const handleShowCitation = (doc: DocumentInfo) => {
  selectedDocId.value = doc.id
  citationDialogVisible.value = true
}

// Show note panel
const handleShowNotes = (doc: DocumentInfo) => {
  selectedDocId.value = doc.id
  noteDrawerVisible.value = true
}

// Format file size
const formatFileSize = (bytes: number) => {
  if (!bytes || bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return Math.round(bytes / Math.pow(k, i) * 100) / 100 + ' ' + sizes[i]
}

// Format date time
const formatDateTime = (dateString: string) => {
  if (!dateString) return ''
  const date = new Date(dateString)
  return date.toLocaleString('zh-CN')
}
</script>

<style scoped>
.doc-container {
  height: 100%;
  padding: 20px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 12px;
}

.doc-container :deep(.el-card) {
  border-radius: 16px;
  border: none;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
}

.doc-container :deep(.el-card__header) {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
  padding: 20px 24px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-header span {
  font-size: 20px;
  font-weight: 600;
  color: #fff;
  letter-spacing: 0.5px;
}

.doc-container :deep(.el-button--primary) {
  background: #fff;
  color: #667eea;
  border: none;
  font-weight: 600;
  padding: 10px 24px;
  border-radius: 8px;
  transition: all 0.3s ease;
}

.doc-container :deep(.el-button--primary:hover) {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(255, 255, 255, 0.3);
}

.upload-progress {
  margin-bottom: 20px;
  padding: 20px;
  background: linear-gradient(135deg, #667eea15 0%, #764ba215 100%);
  border-radius: 12px;
  border: 2px dashed #667eea;
}

.progress-text {
  display: block;
  margin-top: 12px;
  font-size: 14px;
  color: #667eea;
  font-weight: 500;
}

.doc-container :deep(.el-table) {
  border-radius: 12px;
  overflow: hidden;
}

.doc-container :deep(.el-table th) {
  background: linear-gradient(135deg, #f5f7fa 0%, #e8eaf6 100%);
  color: #667eea;
  font-weight: 600;
}

.doc-container :deep(.el-table tr:hover) {
  background: linear-gradient(135deg, #667eea08 0%, #764ba208 100%);
}

.doc-container :deep(.el-button.is-link) {
  font-weight: 500;
  transition: all 0.2s ease;
}

.doc-container :deep(.el-button--primary.is-link:hover) {
  transform: scale(1.05);
}

.doc-container :deep(.el-button--danger.is-link:hover) {
  transform: scale(1.05);
}

/* Card hover effect */
.doc-container :deep(.el-card) {
  transition: all 0.3s ease;
}

.doc-container :deep(.el-card:hover) {
  box-shadow: 0 12px 48px rgba(0, 0, 0, 0.15);
}
</style>
