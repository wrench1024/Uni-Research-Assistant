<template>
  <el-dialog
    v-model="dialogVisible"
    title="生成文献引用"
    width="700px"
    @close="handleClose"
  >
    <el-tabs v-model="activeTab">
      <el-tab-pane label="📝 编辑元数据" name="metadata">
        <el-form :model="metadata" label-width="100px" class="metadata-form">
          <el-form-item label="作者">
            <el-input v-model="metadata.authors" placeholder="多个作者用逗号分隔，如：张三,李四" clearable />
          </el-form-item>
          <el-form-item label="年份">
            <el-input-number v-model="metadata.publicationYear" :min="1900" :max="2100" />
          </el-form-item>
          <el-form-item label="期刊">
            <el-input v-model="metadata.journal" placeholder="期刊名称" clearable />
          </el-form-item>
          <el-form-item label="卷">
            <el-input v-model="metadata.volume" placeholder="如：10" clearable />
          </el-form-item>
          <el-form-item label="页码">
            <el-input v-model="metadata.pages" placeholder="如：1-20" clearable />
          </el-form-item>
          <el-form-item label="DOI">
            <el-input v-model="metadata.doi" placeholder="10.xxxx/xxxxx" clearable />
          </el-form-item>
          <el-form-item label="出版商">
            <el-input v-model="metadata.publisher" placeholder="出版商名称" clearable />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="saveMetadata" :loading="saving">保存元数据</el-button>
          </el-form-item>
        </el-form>
      </el-tab-pane>

      <el-tab-pane label="📚 生成引用" name="citation">
        <div class="citation-section">
          <el-radio-group v-model="citationFormat" class="format-selector">
            <el-radio-button label="bibtex">BibTeX</el-radio-button>
            <el-radio-button label="endnote">EndNote (RIS)</el-radio-button>
          </el-radio-group>

          <div class="citation-output">
            <pre v-if="citation" class="citation-text">{{ citation }}</pre>
            <div v-else class="empty-hint">
              {{ loading ? '正在生成引用...' : '请先填写元数据并保存' }}
            </div>
          </div>

          <div class="actions">
            <el-button @click="generateCitation" :loading="loading">刷新引用</el-button>
            <el-button type="primary" @click="copyCitation" :disabled="!citation">
              <el-icon><DocumentCopy /></el-icon> 复制引用
            </el-button>
          </div>
        </div>
      </el-tab-pane>
    </el-tabs>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { DocumentCopy } from '@element-plus/icons-vue'
import { docAPI } from '@/api/doc'

const props = defineProps<{
  visible: boolean
  documentId: number
}>()

const emit = defineEmits(['update:visible', 'saved'])

const dialogVisible = ref(false)
const activeTab = ref('metadata')
const citationFormat = ref('bibtex')
const citation = ref('')
const loading = ref(false)
const saving = ref(false)

const metadata = ref({
  authors: '',
  publicationYear: null as number | null,
  journal: '',
  volume: '',
  pages: '',
  doi: '',
  publisher: ''
})

watch(() => props.visible, (val) => {
  dialogVisible.value = val
  if (val) {
    // Load existing metadata if available
    loadMetadata()
  }
})

watch(citationFormat, () => {
  if (citation.value) {
    generateCitation()
  }
})

const loadMetadata = async () => {
  try {
    const response = await docAPI.getDetail(props.documentId)
    if (response.code === 200 && response.data) {
      const doc = response.data
      metadata.value = {
        authors: doc.authors || '',
        publicationYear: doc.publicationYear,
        journal: doc.journal || '',
        volume: doc.volume || '',
        pages: doc.pages || '',
        doi: doc.doi || '',
        publisher: doc.publisher || ''
      }
      
      // Try to generate citation if metadata exists
      if (doc.authors || doc.publicationYear || doc.journal) {
        generateCitation()
      } else {
        citation.value = ''
      }
    }
  } catch (error) {
    console.error('Available to load metadata:', error)
  }
}

const saveMetadata = async () => {
  saving.value = true
  try {
    await docAPI.updateMetadata(props.documentId, metadata.value)
    ElMessage.success('元数据已保存')
    emit('saved')
    // Auto-generate citation after saving
    await generateCitation()
  } catch (error) {
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}

const generateCitation = async () => {
  loading.value = true
  try {
    const response = await docAPI.getCitation(props.documentId, citationFormat.value as 'bibtex' | 'endnote')
    if (response.code === 200 && response.data) {
      citation.value = response.data.citation
    }
  } catch (error) {
    ElMessage.error('生成引用失败')
  } finally {
    loading.value = false
  }
}

const copyCitation = async () => {
  try {
    await navigator.clipboard.writeText(citation.value)
    ElMessage.success('已复制到剪贴板')
  } catch (error) {
    ElMessage.error('复制失败')
  }
}

const handleClose = () => {
  emit('update:visible', false)
}
</script>

<style scoped>
.metadata-form {
  padding: 20px 0;
}

.citation-section {
  padding: 20px 0;
}

.format-selector {
  margin-bottom: 20px;
}

.citation-output {
  min-height: 300px;
  background: #f5f7fa;
  border-radius: 8px;
  padding: 16px;
  margin-bottom: 20px;
}

.citation-text {
  font-family: 'Consolas', 'Monaco', monospace;
  font-size: 13px;
  line-height: 1.6;
  color: #2c3e50;
  margin: 0;
  white-space: pre-wrap;
  word-break: break-all;
}

.empty-hint {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 300px;
  color: #909399;
}

.actions {
  display: flex;
  gap: 12px;
  justify-content: flex-end;
}
</style>
