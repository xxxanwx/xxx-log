<template>
  <el-dialog
    v-model="visible"
    title="日志管理"
    width="920px"
    destroy-on-close
    @open="loadData"
  >
    <div class="toolbar">
      <el-alert
        v-if="retentionEnabled"
        type="info"
        :closable="false"
        show-icon
        class="retention-tip"
      >
        自动清理已开启：保留最近 <strong>{{ retentionDays }}</strong> 天索引，每天凌晨执行一次过期删除
      </el-alert>
      <el-alert
        v-else
        type="warning"
        :closable="false"
        show-icon
        class="retention-tip"
      >
        自动清理未开启，索引需手动删除
      </el-alert>
      <el-button type="primary" link :loading="loading" @click="loadData">刷新</el-button>
    </div>

    <el-table
      :data="indices"
      v-loading="loading"
      stripe
      border
      size="small"
      max-height="480"
      empty-text="暂无日志索引"
    >
      <el-table-column prop="indexName" label="索引名" min-width="280" show-overflow-tooltip />
      <el-table-column prop="appName" label="应用" width="120" />
      <el-table-column prop="indexTime" label="时间分片" width="160" />
      <el-table-column prop="docCount" label="文档数" width="100" align="right" />
      <el-table-column prop="storeSize" label="占用空间" width="110" align="right" />
      <el-table-column label="操作" width="90" fixed="right" align="center">
        <template #default="{ row }">
          <el-button type="danger" link size="small" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-dialog>
</template>

<script setup>
import { ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listLogIndices, deleteLogIndex } from '../api/log'

const props = defineProps({
  modelValue: { type: Boolean, default: false }
})
const emit = defineEmits(['update:modelValue'])

const visible = ref(false)
const loading = ref(false)
const indices = ref([])
const retentionEnabled = ref(true)
const retentionDays = ref(30)

watch(() => props.modelValue, (val) => {
  visible.value = val
})
watch(visible, (val) => emit('update:modelValue', val))

async function loadData() {
  loading.value = true
  try {
    const data = await listLogIndices()
    indices.value = data.indices || []
    retentionEnabled.value = data.retentionEnabled
    retentionDays.value = data.retentionDays
  } catch (e) {
    ElMessage.error(e.message || '加载索引列表失败')
  } finally {
    loading.value = false
  }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(
      `确定删除索引「${row.indexName}」？删除后数据不可恢复。`,
      '删除确认',
      { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' }
    )
    await deleteLogIndex(row.indexName)
    ElMessage.success('索引已删除')
    await loadData()
  } catch (e) {
    if (e !== 'cancel' && e !== 'close') {
      ElMessage.error(e.message || '删除失败')
    }
  }
}
</script>

<style scoped>
.toolbar {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}
.retention-tip {
  flex: 1;
}
</style>
