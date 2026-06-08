<template>
  <div class="page">
    <el-card shadow="never" class="filter-card">
      <el-form inline>
        <el-form-item label="时间范围">
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
        <el-form-item label="粒度">
          <el-select v-model="interval" style="width: 120px">
            <el-option label="按小时" value="hour" />
            <el-option label="按天" value="day" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" @click="loadAll">刷新</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-row :gutter="12" class="overview-row">
      <el-col :span="6">
        <el-card shadow="never" class="stat-card">
          <div class="stat-label">日志总量</div>
          <div class="stat-value">{{ overview.totalLogs }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="never" class="stat-card stat-error">
          <div class="stat-label">ERROR</div>
          <div class="stat-value">{{ overview.errorCount }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="never" class="stat-card stat-warn">
          <div class="stat-label">WARN</div>
          <div class="stat-value">{{ overview.warnCount }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="never" class="stat-card">
          <div class="stat-label">错误应用数</div>
          <div class="stat-value">{{ Object.keys(overview.errorsByApp || {}).length }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="12" class="chart-row">
      <el-col :span="12">
        <el-card shadow="never" class="chart-card">
          <template #header>写入趋势</template>
          <div ref="ingestChartRef" class="chart-box"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="never" class="chart-card">
          <template #header>ERROR 趋势</template>
          <div ref="errorChartRef" class="chart-box"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="never" class="top-errors-card">
      <template #header>Top ERROR 消息</template>
      <el-empty v-if="!topErrors.length && !loading" description="暂无 ERROR 数据" />
      <div v-show="topErrors.length" ref="topErrorsChartRef" class="chart-box chart-box-tall"></div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onBeforeUnmount, nextTick } from 'vue'
import * as echarts from 'echarts'
import { ElMessage } from 'element-plus'
import {
  getDashboardOverview,
  getErrorTrend,
  getIngestTrend,
  getTopErrors
} from '../api/log'

const loading = ref(false)
const timeRange = ref(null)
const interval = ref('hour')
const overview = reactive({
  totalLogs: 0,
  errorCount: 0,
  warnCount: 0,
  errorsByApp: {}
})
const topErrors = ref([])

const ingestChartRef = ref(null)
const errorChartRef = ref(null)
const topErrorsChartRef = ref(null)
let ingestChart = null
let errorChart = null
let topErrorsChart = null

function defaultTimeRange() {
  const end = new Date()
  const start = new Date(end.getTime() - 24 * 60 * 60 * 1000)
  return [start, end]
}

function recentRange(hours) {
  const end = new Date()
  const start = new Date(end.getTime() - hours * 60 * 60 * 1000)
  return [start, end]
}

const timeRangeShortcuts = [
  { text: '近6小时', value: () => recentRange(6) },
  { text: '近24小时', value: () => recentRange(24) },
  { text: '近7天', value: () => recentRange(7 * 24) }
]

function toTimestamp(value) {
  return value instanceof Date ? value.getTime() : Number(value)
}

function formatChartTime(ts) {
  const d = new Date(ts)
  if (interval.value === 'day') {
    return d.toLocaleDateString('zh-CN')
  }
  return d.toLocaleString('zh-CN', { hour12: false, month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })
}

function buildLineOption(title, points, color) {
  return {
    tooltip: { trigger: 'axis' },
    grid: { left: 48, right: 16, top: 24, bottom: 32 },
    xAxis: {
      type: 'category',
      data: (points || []).map((p) => formatChartTime(p.time)),
      axisLabel: { rotate: 30, fontSize: 11 }
    },
    yAxis: { type: 'value', minInterval: 1 },
    series: [{
      name: title,
      type: 'line',
      smooth: true,
      data: (points || []).map((p) => p.count),
      areaStyle: { opacity: 0.15 },
      itemStyle: { color }
    }]
  }
}

function renderTopErrors(items) {
  const list = items || []
  topErrors.value = list
  if (!list.length) {
    return
  }
  nextTick(() => {
    if (!topErrorsChart && topErrorsChartRef.value) {
      topErrorsChart = echarts.init(topErrorsChartRef.value)
    }
    if (!topErrorsChart) {
      return
    }
    topErrorsChart.setOption({
      tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
      grid: { left: 120, right: 24, top: 16, bottom: 24 },
      xAxis: { type: 'value', minInterval: 1 },
      yAxis: {
        type: 'category',
        data: list.map((item) => item.message || '-').reverse(),
        axisLabel: {
          width: 100,
          overflow: 'truncate',
          fontSize: 11
        }
      },
      series: [{
        type: 'bar',
        data: list.map((item) => item.count).reverse(),
        itemStyle: { color: '#e53935' }
      }]
    }, true)
    topErrorsChart.resize()
  })
}

async function loadAll() {
  if (!timeRange.value || timeRange.value.length !== 2) {
    ElMessage.warning('请选择时间范围')
    return
  }
  loading.value = true
  const startTime = toTimestamp(timeRange.value[0])
  const endTime = toTimestamp(timeRange.value[1])
  try {
    const [ov, ingest, errors, topErrorsData] = await Promise.all([
      getDashboardOverview(startTime, endTime),
      getIngestTrend(startTime, endTime, interval.value),
      getErrorTrend(startTime, endTime, interval.value),
      getTopErrors(startTime, endTime, 10)
    ])
    overview.totalLogs = ov.totalLogs || 0
    overview.errorCount = ov.errorCount || 0
    overview.warnCount = ov.warnCount || 0
    overview.errorsByApp = ov.errorsByApp || {}

    ingestChart.setOption(buildLineOption('写入量', ingest, '#1a73e8'))
    errorChart.setOption(buildLineOption('ERROR', errors, '#e53935'))
    renderTopErrors(topErrorsData)
  } catch (e) {
    ElMessage.error(e.message || '加载仪表盘失败')
  } finally {
    loading.value = false
  }
}

function handleResize() {
  ingestChart?.resize()
  errorChart?.resize()
  topErrorsChart?.resize()
}

onMounted(async () => {
  timeRange.value = defaultTimeRange()
  await nextTick()
  ingestChart = echarts.init(ingestChartRef.value)
  errorChart = echarts.init(errorChartRef.value)
  window.addEventListener('resize', handleResize)
  loadAll()
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  ingestChart?.dispose()
  errorChart?.dispose()
  topErrorsChart?.dispose()
})
</script>

<style scoped>
.page {
  height: 100%;
  padding: 12px;
  box-sizing: border-box;
  overflow-y: auto;
}
.filter-card {
  margin-bottom: 12px;
}
.overview-row {
  margin-bottom: 12px;
}
.stat-card {
  text-align: center;
}
.stat-label {
  font-size: 13px;
  color: #666;
  margin-bottom: 8px;
}
.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: #1a73e8;
}
.stat-error .stat-value {
  color: #e53935;
}
.stat-warn .stat-value {
  color: #fb8c00;
}
.chart-row {
  margin-bottom: 12px;
}
.chart-card :deep(.el-card__header) {
  padding: 10px 16px;
  font-weight: 600;
}
.chart-box {
  height: 260px;
}
.chart-box-tall {
  height: 320px;
}
.top-errors-card :deep(.el-card__header) {
  padding: 10px 16px;
  font-weight: 600;
}
</style>
