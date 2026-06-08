<template>
  <div class="page">
    <el-card shadow="never" class="search-card">
      <el-form :model="query" inline label-width="80px" class="search-form">
        <el-form-item label="应用">
          <el-select v-model="query.appName" clearable placeholder="全部应用" style="width: 160px">
            <el-option v-for="app in apps" :key="app" :label="app" :value="app" />
          </el-select>
        </el-form-item>
        <el-form-item label="环境">
          <el-select v-model="query.env" clearable placeholder="全部环境" style="width: 140px">
            <el-option v-for="env in envs" :key="env" :label="env" :value="env" />
          </el-select>
        </el-form-item>
        <el-form-item label="TraceId">
          <el-input v-model="query.traceId" clearable placeholder="链路ID" style="width: 320px" />
        </el-form-item>
        <el-form-item label="级别">
          <el-select v-model="query.level" clearable placeholder="全部" style="width: 120px">
            <el-option label="ERROR" value="ERROR" />
            <el-option label="WARN" value="WARN" />
            <el-option label="INFO" value="INFO" />
            <el-option label="DEBUG" value="DEBUG" />
          </el-select>
        </el-form-item>
        <el-form-item label="关键词">
          <div class="keyword-row">
            <el-radio-group v-model="query.keywordOperator" class="keyword-operator">
              <el-radio-button value="AND">并且</el-radio-button>
              <el-radio-button value="OR">或者</el-radio-button>
            </el-radio-group>
            <el-select
              v-model="query.keywords"
              multiple
              filterable
              allow-create
              default-first-option
              collapse-tags
              collapse-tags-tooltip
              :max-collapse-tags="3"
              placeholder="输入关键词后回车，可添加多个"
              style="width: 420px"
            />
          </div>
        </el-form-item>
        <el-form-item label="时间范围" required>
          <el-date-picker
            v-model="timeRange"
            type="datetimerange"
            :shortcuts="timeRangeShortcuts"
            range-separator="至"
            start-placeholder="开始"
            end-placeholder="结束"
            style="width: 380px"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" @click="handleSearch(true)">查询</el-button>
          <el-button @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" class="table-card">
      <div v-if="deepMode" class="deep-pagination-tip">
        深度分页模式：使用游标翻页（超过 10000 条 offset 限制）
      </div>
      <div class="table-wrapper">
        <el-table
          :data="records"
          v-loading="loading"
          stripe
          border
          size="small"
          :height="tableHeight"
          :default-sort="{ prop: 'timestamp', order: 'descending' }"
          @sort-change="handleSortChange"
        >
          <el-table-column prop="timestamp" label="时间" width="180" fixed="left" sortable="custom">
            <template #default="{ row }">{{ formatTime(row.timestamp) }}</template>
          </el-table-column>
          <el-table-column prop="level" label="级别" width="80">
            <template #default="{ row }">
              <el-tag :type="levelType(row.level)" size="small">{{ row.level }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="appName" label="应用" width="120" />
          <el-table-column prop="env" label="环境" width="90" />
          <el-table-column prop="traceId" label="TraceId" width="220" show-overflow-tooltip>
            <template #default="{ row }">
              <el-link
                v-if="row.traceId"
                type="primary"
                class="trace-id-link"
                :title="row.traceId"
                @click="filterByTraceId(row)"
              >
                {{ row.traceId }}
              </el-link>
              <span v-else>-</span>
            </template>
          </el-table-column>
          <el-table-column prop="serverIp" label="IP" width="130" />
          <el-table-column prop="className" label="类名" min-width="180" show-overflow-tooltip />
          <el-table-column prop="message" label="日志内容" min-width="520">
            <template #default="{ row }">
              <span
                class="message-cell"
                title="双击查看详情"
                @dblclick="showDetail(row)"
                v-html="highlightMessage(row.message)"
              ></span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="80" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" size="small" @click="showDetail(row)">详情</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <div class="pagination">
        <el-pagination
          v-model:current-page="query.page"
          v-model:page-size="query.size"
          :total="total"
          :page-sizes="[20, 50, 100]"
          layout="total, sizes, prev, pager, next"
          @size-change="onSizeChange"
          @current-change="onPageChange"
        />
      </div>
    </el-card>

    <el-drawer v-model="drawerVisible" title="日志详情" size="50%">
      <el-descriptions v-if="currentLog" :column="1" border>
        <el-descriptions-item label="时间">{{ formatTime(currentLog.timestamp) }}</el-descriptions-item>
        <el-descriptions-item label="级别">{{ currentLog.level }}</el-descriptions-item>
        <el-descriptions-item label="应用">{{ currentLog.appName }}</el-descriptions-item>
        <el-descriptions-item label="环境">{{ currentLog.env || '-' }}</el-descriptions-item>
        <el-descriptions-item label="TraceId">
          <el-link
            v-if="currentLog.traceId"
            type="primary"
            class="trace-id-link"
            title="点击查看链路详情"
            @click="goTraceDetail(currentLog)"
          >
            {{ currentLog.traceId }}
          </el-link>
        </el-descriptions-item>
        <el-descriptions-item label="SpanId">{{ currentLog.spanId || '-' }}</el-descriptions-item>
        <el-descriptions-item label="服务器">{{ currentLog.serverIp }}</el-descriptions-item>
        <el-descriptions-item label="线程">{{ currentLog.threadName }}</el-descriptions-item>
        <el-descriptions-item label="Logger">{{ currentLog.loggerName }}</el-descriptions-item>
        <el-descriptions-item label="位置">
          {{ currentLog.className }}.{{ currentLog.methodName }}
        </el-descriptions-item>
        <el-descriptions-item label="内容">
          <pre class="log-content" v-html="highlightMessage(currentLog.message)"></pre>
        </el-descriptions-item>
        <el-descriptions-item v-if="currentLog.stackTrace" label="堆栈">
          <pre class="stack-trace">{{ currentLog.stackTrace }}</pre>
        </el-descriptions-item>
      </el-descriptions>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { searchLogs, listApps, listEnvs } from '../api/log'
import { highlightKeywords } from '../utils/highlight'

const MAX_OFFSET = 10000
const router = useRouter()
const loading = ref(false)
const records = ref([])
const total = ref(0)
const apps = ref([])
const envs = ref([])
const timeRange = ref(null)
const drawerVisible = ref(false)
const currentLog = ref(null)
const tableHeight = ref(480)
const deepMode = ref(false)
const nextSearchAfter = ref(null)
const cursorStack = ref([])

const query = reactive({
  appName: '',
  env: '',
  traceId: '',
  level: '',
  keywords: [],
  keywordOperator: 'AND',
  page: 1,
  size: 50,
  sortOrder: 'desc'
})

function defaultTimeRange() {
  const end = new Date()
  const start = new Date(end.getTime() - 24 * 60 * 60 * 1000)
  return [start, end]
}

function toTimestamp(value) {
  if (value instanceof Date) {
    return value.getTime()
  }
  return Number(value)
}

function recentRange(minutes) {
  const end = new Date()
  const start = new Date(end.getTime() - minutes * 60 * 1000)
  return [start, end]
}

function todayRange() {
  const end = new Date()
  const start = new Date()
  start.setHours(0, 0, 0, 0)
  return [start, end]
}

function yesterdayRange() {
  const start = new Date()
  start.setDate(start.getDate() - 1)
  start.setHours(0, 0, 0, 0)
  const end = new Date(start)
  end.setHours(23, 59, 59, 999)
  return [start, end]
}

const timeRangeShortcuts = [
  { text: '近10分钟', value: () => recentRange(10) },
  { text: '15分钟内', value: () => recentRange(15) },
  { text: '30分钟内', value: () => recentRange(30) },
  { text: '近1小时', value: () => recentRange(60) },
  { text: '24小时内', value: () => recentRange(24 * 60) },
  { text: '近一周', value: () => recentRange(7 * 24 * 60) },
  { text: '今天', value: todayRange },
  { text: '昨天', value: yesterdayRange }
]

function updateTableHeight() {
  tableHeight.value = Math.max(window.innerHeight - 280, 320)
}

function resetCursorState() {
  deepMode.value = false
  nextSearchAfter.value = null
  cursorStack.value = []
}

function needsCursorPagination(page, size) {
  return (page - 1) * size + size > MAX_OFFSET
}

function highlightMessage(message) {
  const keywords = (query.keywords || []).map((item) => String(item).trim()).filter(Boolean)
  return highlightKeywords(message, keywords)
}

onMounted(async () => {
  updateTableHeight()
  window.addEventListener('resize', updateTableHeight)
  timeRange.value = defaultTimeRange()
  const [appList, envList] = await Promise.all([listApps(), listEnvs()])
  apps.value = appList
  envs.value = envList
  handleSearch(true)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', updateTableHeight)
})

async function handleSearch(resetCursor = false) {
  if (!timeRange.value || timeRange.value.length !== 2) {
    ElMessage.warning('请选择查询时间范围')
    return
  }
  if (resetCursor) {
    resetCursorState()
  }

  loading.value = true
  try {
    const params = {
      ...query,
      keywords: (query.keywords || []).map((item) => String(item).trim()).filter(Boolean),
      startTime: toTimestamp(timeRange.value[0]),
      endTime: toTimestamp(timeRange.value[1])
    }

    const useCursor = deepMode.value || needsCursorPagination(query.page, query.size)
    if (useCursor) {
      deepMode.value = true
      const cursor = cursorStack.value[query.page - 1] || null
      if (cursor) {
        params.searchAfter = cursor
      } else if (query.page > 1) {
        ElMessage.warning('请从上一页继续翻页以获取游标')
        loading.value = false
        return
      }
    }

    const result = await searchLogs(params)
    records.value = result.records || []
    total.value = result.total || 0
    deepMode.value = result.deepPagination || deepMode.value
    nextSearchAfter.value = result.nextSearchAfter || null
    if (result.nextSearchAfter) {
      cursorStack.value[query.page] = result.nextSearchAfter
    }
  } catch (e) {
    ElMessage.error(e.message || '查询失败')
  } finally {
    loading.value = false
  }
}

function onSizeChange() {
  query.page = 1
  handleSearch(true)
}

function onPageChange(page) {
  query.page = page
  handleSearch(false)
}

function resetQuery() {
  query.appName = ''
  query.env = ''
  query.traceId = ''
  query.level = ''
  query.keywords = []
  query.keywordOperator = 'AND'
  query.page = 1
  query.sortOrder = 'desc'
  timeRange.value = defaultTimeRange()
  handleSearch(true)
}

function handleSortChange({ prop, order }) {
  if (prop !== 'timestamp') {
    return
  }
  query.sortOrder = order === 'ascending' ? 'asc' : 'desc'
  query.page = 1
  handleSearch(true)
}

function formatTime(ts) {
  if (!ts) return '-'
  return new Date(ts).toLocaleString('zh-CN', { hour12: false })
}

function levelType(level) {
  const map = { ERROR: 'danger', WARN: 'warning', INFO: 'success', DEBUG: 'info' }
  return map[level] || 'info'
}

function filterByTraceId(row) {
  if (!row || !row.traceId) {
    return
  }
  query.traceId = row.traceId
  if (row.appName) {
    query.appName = row.appName
  }
  if (row.env) {
    query.env = row.env
  }
  if (row.timestamp && timeRange.value) {
    const ts = Number(row.timestamp)
    const pad = 30 * 60 * 1000
    timeRange.value = [new Date(ts - pad), new Date(ts + pad)]
  }
  query.page = 1
  handleSearch(true)
  ElMessage.success('已按 TraceId 筛选链路日志')
}

function goTraceDetail(row) {
  if (!row?.traceId) {
    return
  }
  drawerVisible.value = false
  const queryParams = {}
  if (row.appName) {
    queryParams.appName = row.appName
  }
  if (timeRange.value && timeRange.value.length === 2) {
    queryParams.startTime = toTimestamp(timeRange.value[0])
    queryParams.endTime = toTimestamp(timeRange.value[1])
  } else if (row.timestamp) {
    const ts = Number(row.timestamp)
    const pad = 30 * 60 * 1000
    queryParams.startTime = ts - pad
    queryParams.endTime = ts + pad
  }
  router.push({
    path: `/trace/${row.traceId}`,
    query: Object.keys(queryParams).length ? queryParams : undefined
  })
}

function showDetail(row) {
  currentLog.value = row
  drawerVisible.value = true
}
</script>

<style scoped>
.page {
  display: flex;
  flex-direction: column;
  width: 100%;
  height: 100%;
  padding: 12px;
  box-sizing: border-box;
  gap: 12px;
}
.search-card {
  flex-shrink: 0;
}
.search-card :deep(.el-card__body) {
  padding-bottom: 4px;
}
.search-form {
  width: 100%;
}
.keyword-row {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}
.keyword-operator {
  flex-shrink: 0;
}
.table-card {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
}
.table-card :deep(.el-card__body) {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  padding-bottom: 12px;
}
.deep-pagination-tip {
  font-size: 12px;
  color: #e65100;
  margin-bottom: 8px;
  flex-shrink: 0;
}
.table-wrapper {
  flex: 1;
  min-height: 0;
}
.pagination {
  margin-top: 12px;
  display: flex;
  justify-content: flex-end;
  flex-shrink: 0;
}
.trace-id-link {
  display: inline-block;
  max-width: 100%;
  font-family: Consolas, Monaco, 'Courier New', monospace;
  font-size: 12px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  vertical-align: middle;
}
.message-cell {
  display: block;
  cursor: pointer;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  line-height: 1.5;
}
.message-cell:hover {
  color: #1a73e8;
}
.message-cell :deep(.kw-highlight) {
  background: #fff59d;
  color: #333;
  padding: 0 2px;
  border-radius: 2px;
}
.log-content, .stack-trace {
  white-space: pre-wrap;
  word-break: break-all;
  font-size: 13px;
  line-height: 1.6;
  margin: 0;
}
.log-content :deep(.kw-highlight) {
  background: #fff59d;
  padding: 0 2px;
}
.stack-trace {
  color: #c62828;
  background: #fff5f5;
  padding: 8px;
  border-radius: 4px;
}
</style>
