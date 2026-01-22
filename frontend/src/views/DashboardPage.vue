<script setup lang="ts">
import { onMounted, ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { useRecordsStore } from '@/stores/records'
import { useAuthStore } from '@/stores/auth'
import type { LearningRecord } from '@/types'
import {
  Search,
  ChatDotRound,
  Document,
  Trophy,
  DataLine,
  ArrowRight,
  Reading,
  TrendCharts,
  Medal,
  Lightning,
  Star
} from '@element-plus/icons-vue'

const { t } = useI18n()
const router = useRouter()
const recordsStore = useRecordsStore()
const authStore = useAuthStore()

// Animated counter ref
const animatedStats = ref({
  activitiesWeek: 0,
  wordQueries: 0,
  dialogues: 0,
  quizzes: 0
})

// Stats data configuration
const statsData = computed(() => [
  { 
    key: 'wordQueries', 
    icon: Search, 
    color: '#3b82f6', 
    bgColor: '#eff6ff',
    gradient: 'linear-gradient(135deg, #3b82f6, #60a5fa)',
    label: 'Words Learned',
    value: animatedStats.value.wordQueries,
    trend: 12
  },
  { 
    key: 'dialogues', 
    icon: ChatDotRound, 
    color: '#10b981', 
    bgColor: '#ecfdf5',
    gradient: 'linear-gradient(135deg, #10b981, #34d399)',
    label: 'Conversations',
    value: animatedStats.value.dialogues,
    trend: 8
  },
  { 
    key: 'quizzes', 
    icon: Document, 
    color: '#f59e0b', 
    bgColor: '#fffbeb',
    gradient: 'linear-gradient(135deg, #f59e0b, #fbbf24)',
    label: 'Quizzes Taken',
    value: animatedStats.value.quizzes,
    trend: -3
  },
  { 
    key: 'avgScore', 
    icon: Trophy, 
    color: '#6366f1', 
    bgColor: '#eef2ff',
    gradient: 'linear-gradient(135deg, #6366f1, #818cf8)',
    label: 'Avg Score',
    value: Math.round(recordsStore.statistics?.averageQuizScore || 0),
    trend: 5
  }
])

// Helper function for stats
function getStatValue(key: string): number {
  const stat = statsData.value.find(s => s.key === key)
  return stat?.value || 0
}

// Animate numbers on mount
function animateValue(obj: any, key: string, target: number, duration: number) {
  const start = 0
  const startTime = Date.now()
  
  const update = () => {
    const elapsed = Date.now() - startTime
    const progress = Math.min(elapsed / duration, 1)
    const easeOutQuart = 1 - Math.pow(1 - progress, 4)
    obj[key] = Math.floor(start + (target - start) * easeOutQuart)
    
    if (progress < 1) {
      requestAnimationFrame(update)
    }
  }
  
  update()
}

// Quick access features with enhanced styling
const features = [
  {
    key: 'wordQuery',
    icon: 'Search',
    route: '/word-query',
    color: '#3b82f6',
    bgColor: '#eff6ff',
    gradient: 'linear-gradient(135deg, #3b82f6 0%, #60a5fa 100%)',
    shadowColor: 'rgba(59, 130, 246, 0.3)'
  },
  {
    key: 'dialogue',
    icon: 'ChatDotRound',
    route: '/dialogue',
    color: '#10b981',
    bgColor: '#ecfdf5',
    gradient: 'linear-gradient(135deg, #10b981 0%, #34d399 100%)',
    shadowColor: 'rgba(16, 185, 129, 0.3)'
  },
  {
    key: 'quiz',
    icon: 'Document',
    route: '/quiz',
    color: '#f59e0b',
    bgColor: '#fffbeb',
    gradient: 'linear-gradient(135deg, #f59e0b 0%, #fbbf24 100%)',
    shadowColor: 'rgba(245, 158, 11, 0.3)'
  },
  {
    key: 'records',
    icon: 'DataLine',
    route: '/records',
    color: '#6366f1',
    bgColor: '#eef2ff',
    gradient: 'linear-gradient(135deg, #6366f1 0%, #818cf8 100%)',
    shadowColor: 'rgba(99, 102, 241, 0.3)'
  },
  {
    key: 'review',
    icon: 'Reading',
    route: '/review',
    color: '#8b5cf6',
    bgColor: '#f5f3ff',
    gradient: 'linear-gradient(135deg, #8b5cf6 0%, #a78bfa 100%)',
    shadowColor: 'rgba(139, 92, 246, 0.3)'
  }
]

// Real achievements data
const recentAchievements = ref<{ id: number; name: string; icon: string; unlocked: boolean }[]>([])

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

function getActivityIcon(type: string) {
  switch (type) {
    case 'WORD_QUERY': return Search
    case 'DIALOGUE': return ChatDotRound
    case 'QUIZ': return Document
    default: return Star
  }
}

function getActivityColor(type: string): string {
  switch (type) {
    case 'WORD_QUERY': return '#3b82f6'
    case 'DIALOGUE': return '#10b981'
    case 'QUIZ': return '#f59e0b'
    default: return '#6366f1'
  }
}

onMounted(async () => {
  await Promise.all([
    recordsStore.fetchRecords({ pageSize: 5 }),
    recordsStore.fetchStatistics()
  ])

  // TODO: Implement recent achievements API endpoint in backend
  // try {
  //   const achievementsResponse = await getRecentAchievements()
  //   if (achievementsResponse.code === 0 || achievementsResponse.code === 200) {
  //     recentAchievements.value = achievementsResponse.data.slice(0, 4).map(achievement => ({
  //       id: achievement.id,
  //       name: achievement.title,
  //       icon: 'Trophy',
  //       unlocked: true
  //     }))
  //   }
  // } catch (error) {
  //   console.error('Failed to load achievements:', error)
  // }

  // Trigger animations after data loads
  setTimeout(() => {
    animateValue(animatedStats.value, 'activitiesWeek', recordsStore.statistics?.activitiesLast7Days || 0, 1000)
    animateValue(animatedStats.value, 'wordQueries', recordsStore.statistics?.totalWordQueries || 0, 1200)
    animateValue(animatedStats.value, 'dialogues', recordsStore.statistics?.totalDialogueSessions || 0, 1400)
    animateValue(animatedStats.value, 'quizzes', recordsStore.statistics?.totalQuizzes || 0, 1600)
  }, 300)
})
</script>

<template>
  <div class="dashboard-page">
    <!-- Animated Background Particles -->
    <div class="particles">
      <div class="particle particle-1"></div>
      <div class="particle particle-2"></div>
      <div class="particle particle-3"></div>
      <div class="particle particle-4"></div>
      <div class="particle particle-5"></div>
    </div>

    <!-- Welcome Banner -->
    <section class="welcome-banner">
      <div class="welcome-content">
        <div class="greeting-row">
          <h2>{{ t('dashboard.welcome') }}, <span class="username">{{ authStore.user?.username || t('dashboard.learner') }}</span>!</h2>
          <div class="streak-badge">
            <el-icon><Lightning /></el-icon>
            <span>7 Day Streak</span>
          </div>
        </div>
        <p class="motivation-text">{{ t('dashboard.continueJourney') }}</p>
        <div class="quick-actions">
          <el-button type="primary" size="small" @click="navigateTo('/word-query')">
            <el-icon><Search /></el-icon>
            {{ t('word.wordQuery') }}
          </el-button>
          <el-button size="small" @click="navigateTo('/quiz')">
            <el-icon><Trophy /></el-icon>
            {{ t('quiz.title') }}
          </el-button>
        </div>
      </div>
      <div class="welcome-stat">
        <div class="stat-ring">
          <svg viewBox="0 0 100 100">
            <circle class="stat-ring-bg" cx="50" cy="50" r="45" />
            <circle class="stat-ring-progress" cx="50" cy="50" r="45" 
              :style="{ strokeDashoffset: 283 - (283 * Math.min((animatedStats.activitiesWeek || 0) / 21, 1)) }" />
          </svg>
          <div class="stat-ring-content">
            <span class="stat-number">{{ animatedStats.activitiesWeek }}</span>
            <span class="stat-label">{{ t('dashboard.activitiesThisWeek') }}</span>
          </div>
        </div>
      </div>
    </section>

    <!-- Stats Grid -->
    <section class="stats-grid">
      <div v-for="(stat, index) in statsData" :key="stat.key" 
           class="stat-card"
           :style="{ '--delay': `${index * 0.1}s` }">
        <div class="stat-glow" :style="{ background: stat.gradient }"></div>
        <div class="stat-icon-wrapper" :style="{ background: stat.bgColor }">
          <el-icon :size="24" :style="{ color: stat.color }">
            <component :is="stat.icon" />
          </el-icon>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ getStatValue(stat.key) }}</span>
          <span class="stat-label">{{ stat.label }}</span>
        </div>
        <div class="stat-trend" :class="{ up: stat.trend > 0 }">
          <el-icon v-if="stat.trend > 0"><TrendCharts /></el-icon>
          <span>{{ Math.abs(stat.trend) }}%</span>
        </div>
      </div>
    </section>

    <!-- Main Content Grid -->
    <div class="main-grid">
      <!-- Quick Access Section -->
      <section class="quick-access">
        <div class="section-header">
          <div class="section-title">
            <el-icon><Lightning /></el-icon>
            <h3>{{ t('dashboard.quickAccess') }}</h3>
          </div>
          <span class="section-subtitle">{{ t('dashboard.startNow') }}</span>
        </div>
        <div class="features-grid">
          <div
            v-for="(feature, index) in features"
            :key="feature.key"
            class="feature-card"
            :style="{ '--index': index }"
            @click="navigateTo(feature.route)"
          >
            <div class="feature-glow" :style="{ background: feature.gradient }"></div>
            <div class="feature-icon" :style="{ background: feature.bgColor }">
              <el-icon :size="28" :style="{ color: feature.color }">
                <Search v-if="feature.icon === 'Search'" />
                <ChatDotRound v-else-if="feature.icon === 'ChatDotRound'" />
                <Document v-else-if="feature.icon === 'Document'" />
                <DataLine v-else-if="feature.icon === 'DataLine'" />
                <Reading v-else-if="feature.icon === 'Reading'" />
              </el-icon>
            </div>
            <div class="feature-info">
              <span class="feature-name">{{ t(`features.${feature.key}`) }}</span>
              <span class="feature-desc">{{ t(`features.${feature.key}Desc`) }}</span>
            </div>
            <el-icon class="feature-arrow">
              <ArrowRight />
            </el-icon>
          </div>
        </div>
      </section>

      <!-- Recent Activities -->
      <section class="recent-activities">
        <div class="section-header">
          <div class="section-title">
            <el-icon><TrendCharts /></el-icon>
            <h3>{{ t('dashboard.recentActivities') }}</h3>
          </div>
          <el-button type="primary" link @click="navigateTo('/records')">
            {{ t('common.viewAll') }} <el-icon><ArrowRight /></el-icon>
          </el-button>
        </div>

        <div v-if="recordsStore.loading" class="loading-state">
          <el-skeleton v-for="i in 3" :key="i" :rows="2" animated />
        </div>

        <div v-else-if="recordsStore.hasRecords" class="activities-timeline">
          <div
            v-for="(record, index) in recordsStore.records.slice(0, 5)"
            :key="record.id"
            class="activity-item"
            :style="{ '--delay': `${index * 0.1}s` }"
          >
            <div class="activity-indicator" :style="{ background: getActivityColor(record.activityType) }">
              <el-icon :size="14">
                <component :is="getActivityIcon(record.activityType)" />
              </el-icon>
            </div>
            <div class="activity-line"></div>
            <div class="activity-content">
              <div class="activity-header">
                <el-tag
                  :type="recordsStore.getActivityTypeColor(record.activityType)"
                  size="small"
                  effect="light"
                  round
                >
                  {{ recordsStore.getActivityTypeLabel(record.activityType) }}
                </el-tag>
                <span class="activity-time">{{ formatRelativeTime(record.activityTime) }}</span>
              </div>
              <span class="activity-desc">{{ getActivityDescription(record) }}</span>
            </div>
          </div>
        </div>

        <el-empty
          v-else
          :description="t('dashboard.noActivities')"
          :image-size="100"
          class="empty-state"
        >
          <el-button type="primary" @click="navigateTo('/word-query')">
            {{ t('common.startLearning') }}
          </el-button>
        </el-empty>
      </section>
    </div>

    <!-- Achievements Section -->
    <section class="achievements-section">
      <div class="section-header">
        <div class="section-title">
          <el-icon><Medal /></el-icon>
          <h3>Recent Achievements</h3>
        </div>
        <el-button type="primary" link @click="navigateTo('/achievements')">
          View All <el-icon><ArrowRight /></el-icon>
        </el-button>
      </div>
      <div v-if="recentAchievements.length > 0" class="achievements-grid">
        <div
          v-for="achievement in recentAchievements"
          :key="achievement.id"
          class="achievement-card"
          :class="{ locked: !achievement.unlocked }"
        >
          <div class="achievement-icon">
            <el-icon :size="28">
              <component :is="achievement.icon" />
            </el-icon>
          </div>
          <span class="achievement-name">{{ achievement.name }}</span>
          <div v-if="!achievement.unlocked" class="achievement-lock">
            <el-icon><Star /></el-icon>
          </div>
        </div>
      </div>
      <el-empty
        v-else
        :description="t('motivation.noRecentAchievements')"
        :image-size="80"
      />
    </section>
  </div>
</template>

<style scoped>
.dashboard-page {
  padding: 24px;
  position: relative;
  overflow: hidden;
}

/* Particles Animation */
.particles {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  pointer-events: none;
  overflow: hidden;
  z-index: 0;
}

.particle {
  position: absolute;
  width: 6px;
  height: 6px;
  background: var(--color-primary);
  border-radius: 50%;
  opacity: 0.15;
  animation: float 15s infinite ease-in-out;
}

.particle-1 { left: 10%; top: 20%; animation-delay: 0s; }
.particle-2 { left: 30%; top: 60%; animation-delay: 2s; }
.particle-3 { left: 50%; top: 30%; animation-delay: 4s; }
.particle-4 { left: 70%; top: 70%; animation-delay: 6s; }
.particle-5 { left: 90%; top: 40%; animation-delay: 8s; }

@keyframes float {
  0%, 100% { transform: translateY(0) translateX(0); opacity: 0.1; }
  25% { transform: translateY(-20px) translateX(10px); opacity: 0.2; }
  50% { transform: translateY(-40px) translateX(-10px); opacity: 0.15; }
  75% { transform: translateY(-20px) translateX(5px); opacity: 0.2; }
}

/* Welcome Banner */
.welcome-banner {
  background: var(--color-primary);
  border-radius: var(--radius-xl);
  padding: 32px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  color: white;
  position: relative;
  overflow: hidden;
  z-index: 1;
  box-shadow: 0 20px 40px rgba(99, 102, 241, 0.3);
}

.welcome-banner::before {
  content: '';
  position: absolute;
  top: -50%;
  right: -20%;
  width: 400px;
  height: 400px;
  background: rgba(255,255,255,0.1);
  border-radius: 50%;
}

.welcome-banner::after {
  content: '';
  position: absolute;
  bottom: -30%;
  left: 10%;
  width: 200px;
  height: 200px;
  background: rgba(255,255,255,0.08);
  border-radius: 50%;
}

.greeting-row {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 8px;
}

.welcome-content h2 {
  font-size: var(--font-size-xl);
  font-weight: 700;
  margin: 0;
}

.username {
  color: #fef3c7;
}

.streak-badge {
  display: flex;
  align-items: center;
  gap: 6px;
  background: rgba(255, 255, 255, 0.2);
  backdrop-filter: blur(10px);
  padding: 6px 12px;
  border-radius: 20px;
  font-size: var(--font-size-sm);
  animation: pulse 2s infinite;
}

@keyframes pulse {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.05); }
}

.motivation-text {
  opacity: 0.95;
  font-size: var(--font-size-md);
  margin-bottom: 16px;
}

.quick-actions {
  display: flex;
  gap: 12px;
}

.quick-actions .el-button {
  background: rgba(255, 255, 255, 0.15);
}

.welcome-stat {
  position: relative;
  z-index: 1;
}

.stat-ring {
  position: relative;
  width: 120px;
  height: 120px;
}

.stat-ring svg {
  transform: rotate(-90deg);
}

.stat-ring-bg {
  fill: none;
  stroke: rgba(255, 255, 255, 0.2);
  stroke-width: 8;
}

.stat-ring-progress {
  fill: none;
  stroke: #fef3c7;
  stroke-width: 8;
  stroke-linecap: round;
  stroke-dasharray: 283;
  stroke-dashoffset: 283;
  transition: stroke-dashoffset 1s ease-out;
}

.stat-ring-content {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  text-align: center;
}

.stat-ring-content .stat-number {
  display: block;
  font-size: 28px;
  font-weight: 700;
}

.stat-ring-content .stat-label {
  font-size: var(--font-size-xs);
  opacity: 0.9;
}

/* Stats Grid */
.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
  margin-bottom: 32px;
  position: relative;
  z-index: 1;
}

.stat-card {
  background: var(--bg-secondary);
  border-radius: var(--radius-xl);
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 16px;
  position: relative;
  overflow: hidden;
  border: 1px solid var(--border-color-light);
  animation: slideUp 0.6s ease-out backwards;
  animation-delay: var(--delay);
  transition: all 0.3s ease;
}

@keyframes slideUp {
  from {
    opacity: 0;
    transform: translateY(30px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.stat-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 24px rgba(0, 0, 0, 0.1);
  border-color: var(--color-accent);
}

.stat-glow {
  display: none;
}

.stat-card:hover .stat-glow {
  opacity: 0.2;
}

.stat-icon-wrapper {
  width: 52px;
  height: 52px;
  border-radius: var(--radius-lg);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  position: relative;
  z-index: 1;
  transition: transform 0.3s;
}

.stat-card:hover .stat-icon-wrapper {
  transform: scale(1.1) rotate(5deg);
}

.stat-info {
  display: flex;
  flex-direction: column;
  flex: 1;
  position: relative;
  z-index: 1;
}

.stat-info .stat-value {
  font-size: var(--font-size-2xl);
  font-weight: 700;
  color: var(--text-primary);
  line-height: 1.2;
}

.stat-info .stat-label {
  font-size: var(--font-size-sm);
  color: var(--text-muted);
}

.stat-trend {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: var(--font-size-xs);
  padding: 4px 8px;
  border-radius: 12px;
  background: #fef2f2;
  color: #ef4444;
  transition: all 0.3s;
}

.stat-trend.up {
  background: #ecfdf5;
  color: #10b981;
}

/* Main Grid */
.main-grid {
  display: grid;
  grid-template-columns: 1.5fr 1fr;
  gap: 24px;
  margin-bottom: 32px;
  position: relative;
  z-index: 1;
}

/* Section Header */
.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 8px;
}

.section-title .el-icon {
  color: var(--color-primary);
  font-size: 20px;
}

.section-title h3 {
  font-size: var(--font-size-lg);
  font-weight: 600;
  color: var(--text-primary);
  margin: 0;
}

.section-subtitle {
  font-size: var(--font-size-sm);
  color: var(--text-muted);
}

/* Quick Access */
.quick-access {
  background: var(--bg-secondary);
  border-radius: var(--radius-xl);
  padding: 24px;
  border: 1px solid var(--border-color-light);
}

.features-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}

.feature-card {
  background: var(--bg-tertiary);
  border-radius: var(--radius-lg);
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 16px;
  cursor: pointer;
  transition: all 0.4s cubic-bezier(0.175, 0.885, 0.32, 1.275);
  border: 1px solid var(--border-color-light);
  position: relative;
  overflow: hidden;
  animation: slideUp 0.5s ease-out backwards;
  animation-delay: calc(var(--index) * 0.08s + 0.2s);
}

.feature-card:hover {
  transform: translateY(-4px) scale(1.02);
  box-shadow: 0 16px 32px rgba(0, 0, 0, 0.12);
  border-color: var(--color-accent);
}

.feature-glow {
  display: none;
}

.feature-card:hover .feature-glow {
  opacity: 0.05;
}

.feature-icon {
  width: 56px;
  height: 56px;
  border-radius: var(--radius-lg);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  position: relative;
  z-index: 1;
  transition: all 0.3s;
}

.feature-card:hover .feature-icon {
  transform: scale(1.1) rotate(-5deg);
  box-shadow: 0 8px 16px rgba(0, 0, 0, 0.1);
}

.feature-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
  position: relative;
  z-index: 1;
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
  transition: all 0.3s;
  position: relative;
  z-index: 1;
}

.feature-card:hover .feature-arrow {
  transform: translateX(6px);
  color: var(--color-accent);
}

/* Recent Activities */
.recent-activities {
  background: var(--bg-secondary);
  border-radius: var(--radius-xl);
  padding: 24px;
  border: 1px solid var(--border-color-light);
}

.loading-state {
  padding: 20px 0;
}

.activities-timeline {
  display: flex;
  flex-direction: column;
  gap: 0;
}

.activity-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 16px 0;
  position: relative;
  animation: fadeInLeft 0.5s ease-out backwards;
  animation-delay: var(--delay);
}

@keyframes fadeInLeft {
  from {
    opacity: 0;
    transform: translateX(-20px);
  }
  to {
    opacity: 1;
    transform: translateX(0);
  }
}

.activity-item:not(:last-child)::after {
  content: '';
  position: absolute;
  left: 15px;
  top: 48px;
  bottom: 0;
  width: 2px;
  background: var(--border-color-light);
}

.activity-indicator {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  color: white;
  position: relative;
  z-index: 1;
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.15);
}

.activity-content {
  flex: 1;
  padding-top: 4px;
}

.activity-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 6px;
}

.activity-time {
  font-size: var(--font-size-xs);
  color: var(--text-muted);
}

.activity-desc {
  font-size: var(--font-size-sm);
  color: var(--text-secondary);
  display: block;
}

.empty-state {
  padding: 40px 0;
}

/* Achievements Section */
.achievements-section {
  background: var(--bg-secondary);
  border-radius: var(--radius-xl);
  padding: 24px;
  border: 1px solid var(--border-color-light);
}

.achievements-grid {
  display: flex;
  gap: 16px;
  margin-top: 16px;
}

.achievement-card {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 16px;
  background: var(--bg-tertiary);
  border-radius: var(--radius-lg);
  border: 2px solid var(--border-color-light);
  transition: all 0.3s;
  position: relative;
  overflow: hidden;
}

.achievement-card:hover {
  transform: translateY(-4px);
  border-color: var(--color-accent);
  box-shadow: 0 8px 16px rgba(0, 0, 0, 0.1);
}

.achievement-card.locked {
  opacity: 0.5;
}

.achievement-icon {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  background: var(--color-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  transition: transform 0.3s;
}

.achievement-card:hover .achievement-icon {
  transform: scale(1.1);
}

.achievement-name {
  font-size: var(--font-size-sm);
  font-weight: 500;
  color: var(--text-primary);
  text-align: center;
}

.achievement-lock {
  position: absolute;
  top: 8px;
  right: 8px;
  color: var(--text-muted);
}

/* Responsive */
@media (max-width: 1200px) {
  .main-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 1024px) {
  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .features-grid {
    grid-template-columns: 1fr;
  }

  .achievements-grid {
    flex-wrap: wrap;
  }

  .achievement-card {
    flex: 1 1 calc(50% - 8px);
    min-width: 120px;
  }
}

@media (max-width: 768px) {
  .dashboard-page {
    padding: 16px;
  }

  .welcome-banner {
    flex-direction: column;
    text-align: center;
    gap: 24px;
    padding: 24px;
  }

  .greeting-row {
    flex-direction: column;
    gap: 12px;
  }

  .quick-actions {
    justify-content: center;
  }

  .stats-grid {
    grid-template-columns: 1fr 1fr;
    gap: 12px;
  }

  .stat-card {
    padding: 16px;
    flex-direction: column;
    text-align: center;
  }

  .stat-trend {
    position: absolute;
    top: 8px;
    right: 8px;
  }

  .achievements-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 480px) {
  .welcome-banner h2 {
    font-size: var(--font-size-lg);
  }

  .stat-ring {
    width: 100px;
    height: 100px;
  }

  .stat-ring-content .stat-number {
    font-size: 22px;
  }
}
</style>
