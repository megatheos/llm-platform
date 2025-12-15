<script setup lang="ts">
import { useRouter, useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { useAuthStore } from '@/stores/auth'
import LanguageSwitcher from './LanguageSwitcher.vue'
import {
  Search,
  ChatDotRound,
  Document,
  DataLine,
  SwitchButton,
  HomeFilled
} from '@element-plus/icons-vue'

const { t } = useI18n()
const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const navItems = [
  { key: 'dashboard', icon: 'HomeFilled', route: '/dashboard' },
  { key: 'wordQuery', icon: 'Search', route: '/word-query' },
  { key: 'dialogue', icon: 'ChatDotRound', route: '/dialogue' },
  { key: 'quiz', icon: 'Document', route: '/quiz' },
  { key: 'records', icon: 'DataLine', route: '/records' }
]

function navigateTo(path: string) {
  router.push(path)
}

async function handleLogout() {
  await authStore.logout()
  router.push('/login')
}
</script>

<template>
  <div class="app-layout">
    <!-- Sidebar -->
    <aside class="sidebar">
      <div class="sidebar-header">
        <span class="logo-icon">📚</span>
        <span class="logo-text">{{ t('common.appName') }}</span>
      </div>
      
      <nav class="sidebar-nav">
        <a 
          v-for="item in navItems" 
          :key="item.key"
          class="nav-item"
          :class="{ active: route.path === item.route }"
          @click="navigateTo(item.route)"
        >
          <el-icon :size="20">
            <HomeFilled v-if="item.icon === 'HomeFilled'" />
            <Search v-else-if="item.icon === 'Search'" />
            <ChatDotRound v-else-if="item.icon === 'ChatDotRound'" />
            <Document v-else-if="item.icon === 'Document'" />
            <DataLine v-else />
          </el-icon>
          <span>{{ t(`nav.${item.key}`) }}</span>
        </a>
      </nav>
      
      <div class="sidebar-footer">
        <a class="nav-item logout" @click="handleLogout">
          <el-icon :size="20"><SwitchButton /></el-icon>
          <span>{{ t('nav.logout') }}</span>
        </a>
      </div>
    </aside>

    <!-- Main Content -->
    <main class="main-content">
      <!-- Header -->
      <header class="page-header">
        <div class="header-left">
          <slot name="header-title">
            <h1>{{ t(`nav.${route.meta.navKey || 'dashboard'}`) }}</h1>
          </slot>
        </div>
        <div class="header-right">
          <LanguageSwitcher />
          <div class="user-info">
            <span class="user-name">{{ authStore.user?.username || '' }}</span>
            <el-avatar :size="36" class="user-avatar">
              {{ authStore.user?.username?.charAt(0).toUpperCase() || 'U' }}
            </el-avatar>
          </div>
        </div>
      </header>

      <!-- Page Content -->
      <div class="page-content">
        <slot></slot>
      </div>
    </main>
  </div>
</template>

<style scoped>
.app-layout {
  display: flex;
  min-height: 100vh;
  background: var(--bg-primary);
}

/* Sidebar */
.sidebar {
  width: 260px;
  background: var(--color-primary);
  color: var(--text-inverse);
  display: flex;
  flex-direction: column;
  position: fixed;
  top: 0;
  left: 0;
  bottom: 0;
  z-index: 100;
}

.sidebar-header {
  padding: 24px;
  display: flex;
  align-items: center;
  gap: 12px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.logo-icon {
  font-size: 28px;
}

.logo-text {
  font-size: var(--font-size-md);
  font-weight: 600;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.sidebar-nav {
  flex: 1;
  padding: 16px 12px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  border-radius: var(--radius-md);
  color: rgba(255, 255, 255, 0.7);
  cursor: pointer;
  transition: all var(--transition-fast);
  text-decoration: none;
}

.nav-item:hover {
  background: rgba(255, 255, 255, 0.1);
  color: var(--text-inverse);
}

.nav-item.active {
  background: var(--color-accent);
  color: var(--color-primary);
  font-weight: 500;
}

.sidebar-footer {
  padding: 16px 12px;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
}

.nav-item.logout:hover {
  background: rgba(239, 68, 68, 0.2);
  color: #fca5a5;
}

/* Main Content */
.main-content {
  flex: 1;
  margin-left: 260px;
  display: flex;
  flex-direction: column;
}

/* Header */
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 32px;
  background: var(--bg-secondary);
  border-bottom: 1px solid var(--border-color-light);
  position: sticky;
  top: 0;
  z-index: 50;
}

.page-header h1 {
  font-size: var(--font-size-xl);
  font-weight: 600;
  color: var(--text-primary);
  margin: 0;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
}

.user-name {
  font-size: 14px;
  color: var(--text-primary);
  font-weight: 500;
}

.user-avatar {
  background: var(--color-accent);
  color: var(--color-primary);
  font-weight: 600;
}

/* Page Content */
.page-content {
  flex: 1;
  padding: 24px 32px;
  overflow-y: auto;
}

/* Responsive */
@media (max-width: 768px) {
  .sidebar {
    display: none;
  }
  
  .main-content {
    margin-left: 0;
  }
  
  .page-header {
    padding: 12px 16px;
  }
  
  .page-content {
    padding: 16px;
  }
}
</style>
