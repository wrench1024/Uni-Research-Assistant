<template>
  <el-dialog
    v-model="dialogVisible"
    :title="isEdit ? '编辑笔记' : '新建笔记'"
    width="600px"
    @close="handleClose"
  >
    <el-form :model="form" label-width="80px">
      <el-form-item label="笔记内容">
        <el-input
          v-model="form.content"
          type="textarea"
          :rows="8"
          placeholder="在这里记录你的想法..."
          maxlength="5000"
          show-word-limit
        />
      </el-form-item>
      
      <el-form-item label="标签">
        <el-input
          v-model="form.tags"
          placeholder="用逗号分隔多个标签，如：重要,TODO"
          clearable
        />
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="handleClose">取消</el-button>
      <el-button type="primary" @click="handleSave" :loading="saving">
        保存
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { noteAPI, type NoteInfo } from '@/api/note'

const props = defineProps<{
  visible: boolean
  docId: number
  note?: NoteInfo | null
}>()

const emit = defineEmits(['update:visible', 'saved'])

const dialogVisible = ref(false)
const saving = ref(false)
const isEdit = ref(false)

const form = ref<NoteInfo>({
  docId: props.docId,
  content: '',
  noteType: 'DOCUMENT',
  tags: ''
})

watch(() => props.visible, (val) => {
  dialogVisible.value = val
  if (val) {
    if (props.note) {
      // Edit mode
      isEdit.value = true
      form.value = {
        ...props.note
      }
    } else {
      // Create mode
      isEdit.value = false
      form.value = {
        docId: props.docId,
        content: '',
        noteType: 'DOCUMENT',
        tags: ''
      }
    }
  }
})

const handleSave = async () => {
  if (!form.value.content.trim()) {
    ElMessage.warning('请输入笔记内容')
    return
  }

  saving.value = true
  try {
    if (isEdit.value && props.note?.id) {
      // Update existing note
      await noteAPI.updateNote(props.note.id, form.value)
      ElMessage.success('笔记已更新')
    } else {
      // Create new note
      await noteAPI.createNote(form.value)
      ElMessage.success('笔记已创建')
    }
    emit('saved')
    handleClose()
  } catch (error) {
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}

const handleClose = () => {
  emit('update:visible', false)
}
</script>

<style scoped>
:deep(.el-textarea__inner) {
  font-family: inherit;
  line-height: 1.6;
}
</style>
