<script setup lang="ts">
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { useRecordsStore } from '@/stores/records'
import { useAuthStore } from '@/stores/auth'
import type { LearningRecord } from '@/types'
import LanguageSwitcher from '@/components/LanguageSwitcher.vue'
import {
  Search,
  ChatDotRound,
  Document,
  Trophy,
  DataLine,
  ArrowRight,
  SwitchButton
} from '@element-plus/icons-vue'

const { t } = useI18n()
const router = useRouter()
const recordsStore = useRecordsStore()
const authStore = useAuthStore()

// Quick access features
const features = [
  {
    key: 'wordQuery',
    icon: 'Search',
    route: '/word-query',
    color: '#3b82f6',
    bgColor: '#eff6ff'
  },
  {
    key: 'dialogue',
    icon: 'ChatDotRound',
    route: '/dialogue',
    color: '#10b981',
    bgColor: '#ecfdf5'
  },
  {
    key: 'quiz',
    icon: 'Document',
    route: '/quiz',
    color: '#f59e0b',
    bgColor: '#fffbeb'
  },
  {
    key: 'records',
    icon: 'DataLine',
    route: '/records',
    color: '#6366f1',
    bgColor: '#eef2ff'
  }
]

function navigateTo(route: string) {
  router.push(route)
}

function formatRelativeTime(dateStr: string): string {
  const date = new Date(dateStr)
  const now = new Date()
  const diffMs = now.getTime() - date.getTime()
  const diffDays = Math.floor(diffMs / (1000 * 60 * 60 * 24))
  
  if (diffDays === 0) return t('dashboard.today')
  if (diffDays === 1) return t('dashboard.yesterday')
  if (diffDays < 7) return t('dashboard.daysAgo', { n: diffDays })
  if (diffDays < 30) return t('dashboard.weeksAgo', { n: Math.floor(diffDays / 7) })
  return t('dashboard.monthsAgo', { n: Math.floor(diffDays / 30) })
}

function getActivityDescription(record: LearningRecord): string {
  const details = record.activityDetails
  if (!details) return ''
  
  switch (record.activityType) {
    case 'WORD_QUERY':
      return `"${(details as any).word}"`
    case 'DIALOGUE':
      return (details as any).scenarioName
    case 'QUIZ':
      return `${(details as any).score}/${(details as any).totalScore}`
    default:
      return ''
  }
}

async function handleLogout() {
  await authStore.logout()
  router.push('/login')
}

onMounted(async () => {
  await Promise.all([
    recordsStore.fetchRecords({ pageSize: 5 }),
    recordsStore.fetchStatistics()
  ])
})
</script>

<template>
  <div class="dashboard-page">
    <!-- Sidebar -->
    <aside class="sidebar">
      <div class="sidebar-header">
        <span class="logo-icon">📚</span>
        <span class="logo-text">{{ t('common.appName') }}</span>
      </div>
      
      <nav class="sidebar-nav">
        <a 
          v-for="feature in features" 
          :key="feature.key"
          class="nav-item"
          :class="{ active: $route.path === feature.route }"
          @click="navigateTo(feature.route)"
        >
          <el-icon :size="20">
            <Search v-if="feature.icon === 'Search'" />
            <ChatDotRound v-else-if="feature.icon === 'ChatDotRound'" />
            <Document v-else-if="feature.icon === 'Document'" />
            <DataLine v-else />
          </el-icon>
          <span>{{ t(`nav.${feature.key}`) }}</span>
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
          <h1>{{ t('nav.dashboard') }}</h1>
        </div>
        <div class="header-right">
          <LanguageSwitcher />
          <div class="user-info">
            <el-avatar :size="36" class="user-avatar">
              {{ authStore.user?.username?.charAt(0).toUpperCase() || 'U' }}
            </el-avatar>
          </div>
        </div>
      </header>

      <!-- Welcome Banner -->
      <section class="welcome-banner">
        <div class="welcome-content">
          <h2>{{ t('dashboard.welcome') }}, {{ authStore.user?.username || t('dashboard.learner') }}!</h2>
          <p>{{ t('dashboard.continueJourney') }}</p>
        </div>
        <div class="welcome-stat" v-if="recordsStore.statistics">
          <span class="stat-number">{{ recordsStore.statistics.activitiesLast7Days || 0 }}</span>
          <span class="stat-label">{{ t('dashboard.activitiesThisWeek') }}</span>
        </div>
      </section>

      <!-- Stats Grid -->
      <section class="stats-grid">
        <div class="stat-card">
          <div class="stat-icon" style="background: #eff6ff; color: #3b82f6;">
            <el-icon :size="24"><Search /></el-icon>
          </div>
          <div class="stat-info">
            <span class="stat-value">{{ recordsStore.statistics?.totalWordQueries || 0 }}</span>
            <span class="stat-label">{{ t('dashboard.wordsLearned') }}</span>
          </div>
        </div>
        
        <div class="stat-card">
          <div class="stat-icon" style="background: #ecfdf5; color: #10b981;">
            <el-icon :size="24"><ChatDotRound /></el-icon>
          </div>
          <div class="stat-info">
            <span class="stat-value">{{ recordsStore.statistics?.totalDialogueSessions || 0 }}</span>
            <span class="stat-label">{{ t('dashboard.conversations') }}</span>
          </div>
        </div>
        
        <div class="stat-card">
          <div class="stat-icon" style="background: #fffbeb; color: #f59e0b;">
            <el-icon :size="24"><Document /></el-icon>
          </div>
          <div class="stat-info">
            <span class="stat-value">{{ recordsStore.statistics?.totalQuizzes || 0 }}</span>
            <span class="stat-label">{{ t('dashboard.quizzesTaken') }}</span>
          </div>
        </div>
        
        <div class="stat-card">
          <div class="stat-icon" style="background: #fef2f2; color: #ef4444;">
            <el-icon :size="24"><Trophy /></el-icon>
          </div>
          <div class="stat-info">
            <span class="stat-value">{{ Math.round(recordsStore.statistics?.averageQuizScore || 0) }}%</span>
            <span class="stat-label">{{ t('dashboard.avgScore') }}</span>
          </div>
        </div>
      </section>

      <!-- Quick Access -->
      <section class="quick-access">
        <div class="section-header">
          <h3>{{ t('dashboard.quickAccess') }}</h3>
          <span class="section-subtitle">{{ t('dashboard.startNow') }}</span>
        </div>
        <div class="features-grid">
          <div 
            v-for="feature in features" 
            :key="feature.key"
            class="feature-card"
            @click="navigateTo(feature.route)"
          >
            <div class="feature-icon" :style="{ background: feature.bgColor, color: feature.color }">
              <el-icon :size="28">
                <Search v-if="feature.icon === 'Search'" />
                <ChatDotRound v-else-if="feature.icon === 'ChatDotRound'" />
                <Document v-else-if="feature.icon === 'Document'" />
                <DataLine v-else />
              </el-icon>
            </div>
            <div class="feature-info">
              <span class="feature-name">{{ t(`features.${feature.key}`) }}</span>
              <span class="feature-desc">{{ t(`features.${feature.key}Desc`) }}</span>
            </div>
            <el-icon class="feature-arrow"><ArrowRight /></el-icon>
          </div>
        </div>
      </section>

      <!-- Recent Activities -->
      <section class="recent-activities">
        <div class="section-header">
          <h3>{{ t('dashboard.recentActivities') }}</h3>
          <el-button type="primary" link @click="navigateTo('/records')">
            {{ t('common.viewAll') }} <el-icon><ArrowRight /></el-icon>
          </el-button>
        </div>
        
        <div v-if="recordsStore.loading" class="loading-state">
          <el-skeleton :rows="3" animated />
        </div>
        
        <div v-else-if="recordsStore.hasRecords" class="activities-list">
          <div
            v-for="record in recordsStore.records.slice(0, 5)"
            :key="record.id"
            class="activity-item"
          >
            <div class="activity-icon">
              <el-icon :size="18">
                <Search v-if="record.activityType === 'WORD_QUERY'" />
                <ChatDotRound v-else-if="record.activityType === 'DIALOGUE'" />
                <Document v-else />
              </el-icon>
            </div>
            <div class="activity-content">
              <el-tag 
                :type="recordsStore.getActivityTypeColor(record.activityType)"
                size="small"
                effect="light"
              >
                {{ recordsStore.getActivityTypeLabel(record.activityType) }}
              </el-tag>
              <span class="activity-desc">{{ getActivityDescription(record) }}</span>
            </div>
            <span class="activity-time">{{ formatRelativeTime(record.activityTime) }}</span>
          </div>
        </div>
        
        <el-empty
          v-else
          :description="t('dashboard.noActivities')"
          :image-size="80"
        >
          <el-button type="primary" @click="navigateTo('/word-query')">
            {{ t('common.startLearning') }}
          </el-button>
        </el-empty>
      </section>
    </main>
  </div>
</template>

<style scoped>
.dashboard-page {
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
  font-size: var(--font-size-lg);
  font-weight: 600;
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
  padding: 24px 32px;
}

/* Header */
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.page-header h1 {
  font-size: var(--font-size-2xl);
  font-weight: 700;
  color: var(--text-primary);
}

.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.user-avatar {
  background: var(--color-accent);
  color: var(--color-primary);
  font-weight: 600;
}

/* Welcome Banner */
.welcome-banner {
  background: linear-gradient(135deg, var(--color-primary) 0%, var(--color-primary-light) 100%);
  border-radius: var(--radius-xl);
  padding: 32px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  color: var(--text-inverse);
}

.welcome-content h2 {
  font-size: var(--font-size-xl);
  font-weight: 600;
  margin-bottom: 8px;
}

.welcome-content p {
  opacity: 0.9;
  font-size: var(--font-size-sm);
}

.welcome-stat {
  text-align: right;
}

.stat-number {
  display: block;
  font-size: 48px;
  font-weight: 700;
  color: var(--color-accent);
}

.stat-label {
  font-size: var(--font-size-sm);
  opacity: 0.9;
}

/* Stats Grid */
.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
  margin-bottom: 32px;
}

.stat-card {
  background: var(--bg-secondary);
  border-radius: var(--radius-lg);
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 16px;
  box-shadow: var(--shadow-sm);
  border: 1px solid var(--border-color-light);
}

.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
}

.stat-info {
  display: flex;
  flex-direction: column;
}

.stat-value {
  font-size: var(--font-size-2xl);
  font-weight: 700;
  color: var(--text-primary);
}

.stat-info .stat-label {
  font-size: var(--font-size-sm);
  color: var(--text-muted);
}

/* Section Header */
.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.section-header h3 {
  font-size: var(--font-size-lg);
  font-weight: 600;
  color: var(--text-primary);
}

.section-subtitle {
  font-size: var(--font-size-sm);
  color: var(--text-muted);
}

/* Quick Access */
.quick-access {
  margin-bottom: 32px;
}

.features-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}

.feature-card {
  background: var(--bg-secondary);
  border-radius: var(--radius-lg);
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 16px;
  cursor: pointer;
  transition: all var(--transition-fast);
  border: 1px solid var(--border-color-light);
}

.feature-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-md);
  border-color: var(--color-accent);
}

.feature-icon {
  width: 56px;
  height: 56px;
  border-radius: var(--radius-lg);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.feature-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.feature-name {
  font-size: var(--font-size-md);
  font-weight: 600;
  color: var(--text-primary);
}

.feature-desc {
  font-size: var(--font-size-sm);
  color: var(--text-muted);
}

.feature-arrow {
  color: var(--text-light);
  transition: transform var(--transition-fast);
}

.feature-card:hover .feature-arrow {
  transform: translateX(4px);
  color: var(--color-accent);
}

/* Recent Activities */
.recent-activities {
  background: var(--bg-secondary);
  border-radius: var(--radius-lg);
  padding: 24px;
  border: 1px solid var(--border-color-light);
}

.loading-state {
  padding: 20px 0;
}

.activities-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.activity-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  background: var(--bg-tertiary);
  border-radius: var(--radius-md);
  transition: background var(--transition-fast);
}

.activity-item:hover {
  background: var(--border-color-light);
}

.activity-icon {
  width: 36px;
  height: 36px;
  border-radius: var(--radius-md);
  background: var(--bg-secondary);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--text-secondary);
}

.activity-content {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 12px;
}

.activity-desc {
  font-size: var(--font-size-sm);
  color: var(--text-secondary);
}

.activity-time {
  font-size: var(--font-size-xs);
  color: var(--text-muted);
}

/* Responsive */
@media (max-width: 1024px) {
  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  
  .features-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .sidebar {
    display: none;
  }
  
  .main-content {
    margin-left: 0;
    padding: 16px;
  }
  
  .welcome-banner {
    flex-direction: column;
    text-align: center;
    gap: 20px;
  }
  
  .welcome-stat {
    text-align: center;
  }
  
  .stats-grid {
    grid-template-columns: 1fr 1fr;
  }
}
</style>
