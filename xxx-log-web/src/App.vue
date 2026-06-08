<template>
  <el-container class="layout">
    <el-header v-if="showHeader" class="header">
      <div class="header-left">
        <div class="logo">xxx-log</div>
        <div class="subtitle">分布式日志查询平台</div>
      </div>
      <div v-if="username" class="header-right">
        <div v-if="queueStats" class="queue-stats-panel" :class="queueLevelClass">
          <span class="queue-item queue-total">待处理 <strong>{{ queueStats.totalPending }}</strong></span>
          <span class="queue-divider">|</span>
          <span class="queue-item">队列({{ queueStats.queueType }}) {{ queueStats.queuePending }}</span>
          <span class="queue-divider">|</span>
          <span class="queue-item">写入缓冲 {{ queueStats.bufferPending }}</span>
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
import { logout, getQueueStats } from './api/log'
import LogIndexManageDialog from './components/LogIndexManageDialog.vue'

const route = useRoute()
const router = useRouter()
const username = ref(getUsername())
const queueStats = ref(null)
const indexManageVisible = ref(false)
let queueTimer = null

const showHeader = computed(() => route.path !== '/login')

const queueLevelClass = computed(() => {
  if (!queueStats.value) {
    return ''
  }
  const total = queueStats.value.totalPending
  if (total >= 1000) {
    return 'queue-danger'
  }
  if (total >= 100) {
    return 'queue-warn'
  }
  return 'queue-normal'
})

watch(() => route.path, () => {
  username.value = getUsername()
  syncQueuePolling()
})

async function refreshQueueStats() {
  if (!isLoggedIn() || route.path === '/login') {
    return
  }
  try {
    queueStats.value = await getQueueStats()
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
