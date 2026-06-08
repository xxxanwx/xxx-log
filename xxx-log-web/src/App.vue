<template>
  <el-container class="layout">
    <el-header v-if="showHeader" class="header">
      <div class="header-left">
        <div class="logo">xxx-log</div>
        <div class="subtitle">分布式日志查询平台</div>
        <nav class="nav-links">
          <router-link to="/logs" class="nav-link" active-class="nav-link-active">日志查询</router-link>
          <router-link to="/dashboard" class="nav-link" active-class="nav-link-active">仪表盘</router-link>
        </nav>
      </div>
      <div v-if="username" class="header-right">
        <div v-if="health" class="health-panel">
          <span class="health-item" :class="healthClass(health.es)" title="Elasticsearch">ES</span>
          <span class="health-item" :class="healthClass(health.redis)" title="Redis">Redis</span>
          <span
            v-if="health.rabbitmq !== 'N/A'"
            class="health-item"
            :class="healthClass(health.rabbitmq)"
            title="RabbitMQ"
          >MQ</span>
        </div>
        <div v-if="queueStats" class="queue-stats-panel" :class="queueLevelClass">
          <span class="queue-item queue-total">待处理 <strong>{{ queueStats.totalPending }}</strong></span>
          <span class="queue-divider">|</span>
          <span class="queue-item">队列({{ queueStats.queueType }}) {{ queueStats.queuePending }}</span>
          <span class="queue-divider">|</span>
          <span class="queue-item">消费失败 {{ queueStats.consumeFailCount || 0 }}</span>
          <span class="queue-divider">|</span>
          <span class="queue-item queue-name">{{ queueStats.queueName }}</span>
        </div>
        <span class="user-info">{{ username }}</span>
        <el-button link class="header-btn" @click="indexManageVisible = true">日志管理</el-button>
        <el-button link class="logout-btn" @click="handleLogout">退出</el-button>
      </div>
    </el-header>
    <el-main :class="{ 'login-main': !showHeader, 'app-main': showHeader }">
      <router-view />
    </el-main>
    <LogIndexManageDialog v-model="indexManageVisible" />
  </el-container>
</template>

<script setup>
import { computed, ref, watch, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { clearAuth, getUsername, isLoggedIn } from './utils/auth'
import { logout, getQueueStats, getHealth } from './api/log'
import LogIndexManageDialog from './components/LogIndexManageDialog.vue'

const route = useRoute()
const router = useRouter()
const username = ref(getUsername())
const queueStats = ref(null)
const health = ref(null)
const indexManageVisible = ref(false)
let queueTimer = null

const showHeader = computed(() => route.path !== '/login')

const queueLevelClass = computed(() => {
  if (!queueStats.value) {
    return ''
  }
  const total = queueStats.value.totalPending
  const threshold = queueStats.value.threshold || 1000
  if (queueStats.value.backlogAlert || total >= threshold) {
    return 'queue-danger'
  }
  if (total >= threshold * 0.1) {
    return 'queue-warn'
  }
  return 'queue-normal'
})

function healthClass(status) {
  if (status === 'UP') {
    return 'health-up'
  }
  if (status === 'N/A') {
    return 'health-na'
  }
  return 'health-down'
}

watch(() => route.path, () => {
  username.value = getUsername()
  syncQueuePolling()
})

async function refreshQueueStats() {
  if (!isLoggedIn() || route.path === '/login') {
    return
  }
  try {
    const [stats, healthStatus] = await Promise.all([getQueueStats(), getHealth()])
    queueStats.value = stats
    health.value = healthStatus
  } catch (e) {
    // 轮询失败不打断页面，保留上次数值
  }
}

function syncQueuePolling() {
  if (queueTimer) {
    clearInterval(queueTimer)
    queueTimer = null
  }
  if (isLoggedIn() && route.path !== '/login') {
    refreshQueueStats()
    queueTimer = setInterval(refreshQueueStats, 5000)
  } else {
    queueStats.value = null
    health.value = null
  }
}

syncQueuePolling()

onBeforeUnmount(() => {
  if (queueTimer) {
    clearInterval(queueTimer)
  }
})

async function handleLogout() {
  try {
    if (isLoggedIn()) {
      await logout()
    }
  } catch (e) {
    // ignore
  } finally {
    clearAuth()
    ElMessage.success('已退出登录')
    router.push('/login')
  }
}
</script>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}
body {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
  background: #f0f2f5;
}
.layout {
  min-height: 100vh;
  height: 100vh;
}
.header {
  background: linear-gradient(135deg, #1a73e8 0%, #0d47a1 100%);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  height: 56px !important;
}
.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}
.nav-links {
  display: flex;
  gap: 4px;
  margin-left: 8px;
}
.nav-link {
  color: rgba(255, 255, 255, 0.85);
  text-decoration: none;
  font-size: 14px;
  padding: 4px 12px;
  border-radius: 4px;
}
.nav-link:hover,
.nav-link-active {
  color: #fff;
  background: rgba(255, 255, 255, 0.2);
}
.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-shrink: 0;
}
.logo {
  font-size: 20px;
  font-weight: 700;
  letter-spacing: 1px;
}
.subtitle {
  font-size: 13px;
  opacity: 0.85;
}
.health-panel {
  display: flex;
  gap: 6px;
}
.health-item {
  font-size: 11px;
  font-weight: 600;
  padding: 2px 8px;
  border-radius: 4px;
  background: rgba(255, 255, 255, 0.15);
}
.health-up {
  background: rgba(76, 175, 80, 0.5);
}
.health-down {
  background: rgba(244, 67, 54, 0.55);
}
.health-na {
  opacity: 0.6;
}
.queue-stats-panel {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 13px;
  padding: 5px 14px;
  border-radius: 6px;
  background: rgba(255, 255, 255, 0.15);
  white-space: nowrap;
}
.queue-item {
  opacity: 0.95;
}
.queue-total strong {
  font-size: 15px;
  font-weight: 700;
  margin-left: 2px;
}
.queue-name {
  font-family: Consolas, Monaco, 'Courier New', monospace;
  font-size: 12px;
  opacity: 0.9;
}
.queue-divider {
  opacity: 0.45;
  font-size: 12px;
}
.queue-normal {
  opacity: 1;
}
.queue-warn {
  background: rgba(255, 193, 7, 0.35);
}
.queue-danger {
  background: rgba(244, 67, 54, 0.45);
}
.user-info {
  font-size: 14px;
  opacity: 0.9;
}
.logout-btn {
  color: #fff !important;
}
.header-btn {
  color: #fff !important;
}
.login-main {
  padding: 0 !important;
}
.app-main {
  padding: 0 !important;
  height: calc(100vh - 56px);
  overflow: hidden;
}
</style>
