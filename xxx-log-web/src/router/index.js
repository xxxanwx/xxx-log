import { createRouter, createWebHistory } from 'vue-router'
import { isLoggedIn } from '../utils/auth'
import LogSearch from '../views/LogSearch.vue'
import TraceDetail from '../views/TraceDetail.vue'
import Login from '../views/Login.vue'

const routes = [
  { path: '/login', component: Login, meta: { public: true } },
  { path: '/', redirect: '/logs' },
  { path: '/logs', component: LogSearch, meta: { requiresAuth: true } },
  { path: '/trace/:traceId', component: TraceDetail, props: true, meta: { requiresAuth: true } }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to) => {
  if (to.meta.public) {
    if (to.path === '/login' && isLoggedIn()) {
      return '/logs'
    }
    return true
  }
  if (to.meta.requiresAuth && !isLoggedIn()) {
    return '/login'
  }
  return true
})

export default router
