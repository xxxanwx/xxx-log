<template>
  <div class="page">
    <el-card shadow="never" class="trace-card">
      <template #header>
        <div class="trace-header">
          <el-button link @click="$router.back()">&larr; 返回</el-button>
          <span class="title">链路追踪</span>
          <el-tag type="info" class="trace-id-tag">{{ traceId }}</el-tag>
        </div>
      </template>

      <div class="timeline-wrapper">
        <el-timeline v-loading="loading">
          <el-timeline-item
            v-for="(log, index) in logs"
            :key="log.id || index"
            :timestamp="formatTime(log.timestamp)"
            placement="top"
            :type="timelineType(log.level)"
          >
            <el-card shadow="hover" class="trace-item">
              <div class="trace-meta">
                <el-tag :type="levelType(log.level)" size="small">{{ log.level }}</el-tag>
                <span class="app">{{ log.appName }}</span>
                <span class="location">{{ log.className }}.{{ log.methodName }}</span>
                <span class="span-id" v-if="log.spanId">span: {{ log.spanId }}</span>
              </div>
              <div class="trace-msg">{{ log.message }}</div>
              <pre v-if="log.stackTrace" class="stack">{{ log.stackTrace }}</pre>
            </el-card>
          </el-timeline-item>
        </el-timeline>

        <el-empty v-if="!loading && logs.length === 0" description="未找到该链路的日志" />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted, watch, computed } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getTraceLogs } from '../api/log'

const props = defineProps({
  traceId: { type: String, required: true }
})

const route = useRoute()
const appName = computed(() => route.query.appName || '')

const loading = ref(false)
const logs = ref([])

async function loadTrace() {
  loading.value = true
  try {
    const startTime = route.query.startTime ? Number(route.query.startTime) : undefined
    const endTime = route.query.endTime ? Number(route.query.endTime) : undefined
    logs.value = await getTraceLogs(props.traceId, appName.value || undefined, startTime, endTime)
  } catch (e) {
    ElMessage.error(e.message || '加载失败')
  } finally {
    loading.value = false
  }
}

onMounted(loadTrace)
watch(() => [props.traceId, appName.value, route.query.startTime, route.query.endTime], loadTrace)

function formatTime(ts) {
  if (!ts) return ''
  return new Date(ts).toLocaleString('zh-CN', { hour12: false })
}

function levelType(level) {
  const map = { ERROR: 'danger', WARN: 'warning', INFO: 'success', DEBUG: 'info' }
  return map[level] || 'info'
}

function timelineType(level) {
  if (level === 'ERROR') return 'danger'
  if (level === 'WARN') return 'warning'
  return 'primary'
}
</script>

<style scoped>
.page {
  width: 100%;
  height: 100%;
  padding: 12px;
  box-sizing: border-box;
}
.trace-card {
  height: 100%;
  display: flex;
  flex-direction: column;
}
.trace-card :deep(.el-card__body) {
  flex: 1;
  min-height: 0;
  overflow: auto;
}
.trace-header {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}
.title {
  font-weight: 600;
  font-size: 16px;
}
.trace-id-tag {
  font-family: Consolas, Monaco, 'Courier New', monospace;
  font-size: 12px;
  max-width: 100%;
  height: auto;
  white-space: normal;
  word-break: break-all;
  line-height: 1.5;
  padding: 6px 10px;
}
.timeline-wrapper {
  min-height: 100%;
}
.trace-item {
  margin-bottom: 4px;
}
.trace-meta {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 8px;
  font-size: 13px;
  color: #666;
  flex-wrap: wrap;
}
.app {
  font-weight: 500;
  color: #1a73e8;
}
.location {
  font-family: monospace;
}
.span-id {
  color: #999;
  font-family: Consolas, Monaco, 'Courier New', monospace;
  word-break: break-all;
}
.trace-msg {
  font-size: 14px;
  line-height: 1.6;
}
.stack {
  margin-top: 8px;
  padding: 8px;
  background: #fff5f5;
  color: #c62828;
  font-size: 12px;
  border-radius: 4px;
  white-space: pre-wrap;
  word-break: break-all;
}
</style>
