<template>
  <div class="note-panel">
    <div class="panel-header">
      <h3>📝 笔记</h3>
      <el-button type="primary" size="small" :icon="Plus" @click="handleCreate">
        新建笔记
      </el-button>
    </div>

    <div class="search-box">
      <el-input
        v-model="searchKeyword"
        placeholder="搜索笔记..."
        :prefix-icon="Search"
        clearable
        @input="handleSearch"
      />
    </div>

    <div class="note-list" v-loading="loading">
      <div v-if="notes.length === 0" class="empty-hint">
        暂无笔记
      </div>
      
      <div
        v-for="note in notes"
        :key="note.id"
        class="note-item"
      >
        <div class="note-content">{{ note.content }}</div>
        <div class="note-meta">
          <span class="note-time">{{ formatTime(note.createTime) }}</span>
          <span v-if="note.tags" class="note-tags">
            <el-tag
              v-for="tag in parseTags(note.tags)"
              :key="tag"
              size="small"
              type="info"
            >
              {{ tag }}
            </el-tag>
          </span>
        </div>
        <div class="note-actions">
          <el-button type="primary" size="small" link @click="handleEdit(note)">
            编辑
          </el-button>
          <el-popconfirm
            title="确定要删除这条笔记吗？"
            @confirm="handleDelete(note.id!)"
          >
            <template #reference>
              <el-button type="danger" size="small" link>
                删除
              </el-button>
            </template>
          </el-popconfirm>
        </div>
      </div>
    </div>

    <NoteEditor
      v-model:visible="editorVisible"
      :doc-id="docId"
      :note="currentNote"
      @saved="loadNotes"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus, Search } from '@element-plus/icons-vue'
import { noteAPI, type NoteInfo } from '@/api/note'
import NoteEditor from './NoteEditor.vue'

const props = defineProps<{
  docId: number
}>()

const notes = ref<NoteInfo[]>([])
const loading = ref(false)
const editorVisible = ref(false)
const currentNote = ref<NoteInfo | null>(null)
const searchKeyword = ref('')

onMounted(() => {
  loadNotes()
})

const loadNotes = async () => {
  loading.value = true
  try {
    const response = await noteAPI.getDocumentNotes(props.docId)
    if (response.code === 200 && response.data) {
      notes.value = response.data
    }
  } catch (error) {
    console.error('Failed to load notes:', error)
  } finally {
    loading.value = false
  }
}

const handleCreate = () => {
  currentNote.value = null
  editorVisible.value = true
}

const handleEdit = (note: NoteInfo) => {
  currentNote.value = note
  editorVisible.value = true
}

const handleDelete = async (id: number) => {
  try {
    const response = await noteAPI.deleteNote(id)
    if (response.code === 200) {
      ElMessage.success('笔记已删除')
      loadNotes()
    }
  } catch (error) {
    ElMessage.error('删除失败')
  }
}

const handleSearch = async () => {
  if (!searchKeyword.value.trim()) {
    loadNotes()
    return
  }

  loading.value = true
  try {
    const response = await noteAPI.searchNotes(searchKeyword.value)
    if (response.code === 200 && response.data) {
      // Filter by current document
      notes.value = response.data.filter((n: NoteInfo) => n.docId === props.docId)
    }
  } catch (error) {
    console.error('Search failed:', error)
  } finally {
    loading.value = false
  }
}

const formatTime = (timeStr?: string) => {
  if (!timeStr) return ''
  const date = new Date(timeStr)
  return date.toLocaleString('zh-CN')
}

const parseTags = (tags?: string) => {
  if (!tags) return []
  return tags.split(',').map(t => t.trim()).filter(t => t)
}
</script>

<style scoped>
.note-panel {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: #fff;
  border-radius: 12px;
  padding: 20px;
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.panel-header h3 {
  margin: 0;
  font-size: 18px;
  color: #2c3e50;
}

.search-box {
  margin-bottom: 16px;
}

.note-list {
  flex: 1;
  overflow-y: auto;
}

.empty-hint {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 200px;
  color: #909399;
}

.note-item {
  background: linear-gradient(135deg, #f5f7fa 0%, #e8eaf6 100%);
  border-radius: 8px;
  padding: 12px;
  margin-bottom: 12px;
  transition: all 0.3s ease;
}

.note-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.note-content {
  font-size: 14px;
  line-height: 1.6;
  color: #2c3e50;
  margin-bottom: 8px;
  word-break: break-word;
}

.note-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.note-time {
  font-size: 12px;
  color: #909399;
}

.note-tags {
  display: flex;
  gap: 4px;
}

.note-actions {
  display: flex;
  gap: 8px;
  justify-content: flex-end;
}
</style>
