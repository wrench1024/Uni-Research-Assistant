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

      <!-- Document table -->
      <el-table :data="documents" v-loading="loading" stripe style="width: 100%">
        <el-table-column prop="title" label="文件名" min-width="200">
          <template #default="{ row }">
            <el-text truncated>{{ row.fileName || row.title }}</el-text>
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

        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" :icon="Download" link @click="handleDownload(row)">
              下载
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
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { docAPI, type DocumentInfo } from '@/api/doc'
import { Upload, Download, Delete } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

// State
const documents = ref<DocumentInfo[]>([])
const loading = ref(false)
const uploading = ref(false)
const uploadProgress = ref(0)

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
const handleDownload = (doc: DocumentInfo) => {
  try {
    docAPI.downloadDocument(doc.id, doc.fileName || doc.title)
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
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.upload-progress {
  margin-bottom: 20px;
  padding: 16px;
  background-color: #f5f7fa;
  border-radius: 4px;
}

.progress-text {
  display: block;
  margin-top: 8px;
  font-size: 14px;
  color: #606266;
}
</style>
