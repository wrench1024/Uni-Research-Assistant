<template>
  <div class="comparison-table-container">
    <div class="table-header">
      <el-icon><Grid /></el-icon>
      <span>对比表格</span>
    </div>
    
    <el-table 
      :data="tableData" 
      border 
      stripe 
      class="comparison-table"
      :header-cell-style="{ background: '#f5f7fa', color: '#333', fontWeight: '600' }"
    >
      <el-table-column label="对比维度" prop="dimension" width="150" fixed />
      <el-table-column 
        v-for="(doc, index) in documents" 
        :key="index"
        :label="doc.title"
        :prop="`doc${index}`"
        min-width="300"
      >
        <template #default="scope">
          <div class="cell-content">{{ scope.row[`doc${index}`] }}</div>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { Grid } from '@element-plus/icons-vue'

interface Document {
  id: string
  title: string
}

interface ComparisonData {
  dimensions: string[]
  comparison: string[][]
}

const props = defineProps<{
  documents: Document[]
  tableDataJson: string
}>()

const tableData = computed(() => {
  try {
    const parsed: ComparisonData = JSON.parse(props.tableDataJson)
    const { dimensions, comparison } = parsed
    
    return dimensions.map((dim, dimIndex) => {
      const row: any = { dimension: dim }
      comparison[dimIndex]?.forEach((content, docIndex) => {
        row[`doc${docIndex}`] = content
      })
      return row
    })
  } catch (e) {
    console.error('Failed to parse table data:', e)
    return []
  }
})
</script>

<style scoped>
.comparison-table-container {
  margin: 20px 0;
  padding: 16px;
  background: #fff;
  border-radius: 8px;
  border: 1px solid #e2e8f0;
}

.table-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
  font-size: 16px;
  font-weight: 600;
  color: #334155;
}

.comparison-table {
  font-size: 14px;
}

.cell-content {
  line-height: 1.6;
  padding: 8px 0;
  white-space: pre-wrap;
}

:deep(.el-table__cell) {
  padding: 12px 8px;
}
</style>
