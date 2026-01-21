import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    name: 'home',
    redirect: '/dashboard'
  },
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/LoginPage.vue'),
    meta: { requiresAuth: false, layout: 'none' }
  },
  {
    path: '/register',
    name: 'register',
    component: () => import('@/views/RegisterPage.vue'),
    meta: { requiresAuth: false, layout: 'none' }
  },
  {
    path: '/dashboard',
    name: 'dashboard',
    component: () => import('@/views/DashboardPage.vue'),
    meta: { requiresAuth: true, navKey: 'dashboard', layout: 'app' }
  },
  {
    path: '/word-query',
    name: 'wordQuery',
    component: () => import('@/views/WordQueryPage.vue'),
    meta: { requiresAuth: true, navKey: 'wordQuery', layout: 'app' }
  },
  {
    path: '/dialogue',
    name: 'dialogue',
    component: () => import('@/views/DialoguePage.vue'),
    meta: { requiresAuth: true, navKey: 'dialogue', layout: 'app' }
  },
  {
    path: '/quiz',
    name: 'quiz',
    component: () => import('@/views/QuizPage.vue'),
    meta: { requiresAuth: true, navKey: 'quiz', layout: 'app' }
  },
  {
    path: '/records',
    name: 'records',
    component: () => import('@/views/RecordsPage.vue'),
    meta: { requiresAuth: true, navKey: 'records', layout: 'app' }
  },
  {
    path: '/review',
    name: 'review',
    component: () => import('@/views/LearningPage.vue'),
    meta: { requiresAuth: true, navKey: 'review', layout: 'app', tab: 'review' }
  },
  {
    path: '/insights',
    name: 'insights',
    component: () => import('@/views/LearningPage.vue'),
    meta: { requiresAuth: true, navKey: 'insights', layout: 'app', tab: 'insights' }
  },
  {
    path: '/plan',
    name: 'plan',
    component: () => import('@/views/LearningPage.vue'),
    meta: { requiresAuth: true, navKey: 'plan', layout: 'app', tab: 'plan' }
  },
  {
    path: '/achievements',
    name: 'achievements',
    component: () => import('@/views/LearningPage.vue'),
    meta: { requiresAuth: true, navKey: 'achievements', layout: 'app', tab: 'achievements' }
  }
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes
})

// Navigation guard for authentication
router.beforeEach((to, _from, next) => {
  const token = localStorage.getItem('token')
  
  if (to.meta.requiresAuth && !token) {
    next({ name: 'login' })
  } else if ((to.name === 'login' || to.name === 'register') && token) {
    next({ name: 'dashboard' })
  } else {
    next()
  }
})

export default router
