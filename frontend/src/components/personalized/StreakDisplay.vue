<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import {
  Trophy,
  Calendar,
  Clock,
  Warning,
  Sunny
} from '@element-plus/icons-vue'
import type { StreakInfo } from '@/types/personalized'
import { getStreakInfo } from '@/api/motivation'

const { t } = useI18n()

// Props
interface Props {
  compact?: boolean
  currentStreak?: number
  longestStreak?: number
  totalLearningDays?: number
  streakStatus?: string
  daysUntilRisk?: number
}

const props = withDefaults(defineProps<Props>(), {
  compact: false,
  currentStreak: 0,
  longestStreak: 0,
  totalLearningDays: 0,
  streakStatus: 'active',
  daysUntilRisk: 0
})

// State - use props if provided, otherwise load from API
const streak = ref<StreakInfo | null>(props.currentStreak > 0 ? {
  currentStreak: props.currentStreak,
  longestStreak: props.longestStreak,
  totalLearningDays: props.totalLearningDays,
  lastActivityDate: new Date().toISOString(),
  streakStatus: props.streakStatus as 'active' | 'at_risk' | 'broken',
  daysUntilRisk: props.daysUntilRisk
} : null)
const loading = ref(false)
const showConfetti = ref(false)

// Computed
const streakStatus = computed(() => {
  if (!streak.value) return 'unknown'
  return streak.value.streakStatus
})

const statusConfig = computed(() => {
  switch (streakStatus.value) {
    case 'active': return {
      icon: Sunny,
      color: '#f59e0b',
      bgColor: 'rgba(245, 158, 11, 0.1)',
      label: t('motivation.learningStreak'),
      message: t('motivation.keepGoing')
    }
    case 'at_risk': return {
      icon: Warning,
      color: '#ef4444',
      bgColor: 'rgba(239, 68, 68, 0.1)',
      label: t('motivation.atRisk'),
      message: `${t('motivation.atRisk')}! ${streak.value?.daysUntilRisk} ${t('motivation.days')}`
    }
    case 'broken': return {
      icon: Clock,
      color: '#6b7280',
      bgColor: 'rgba(107, 114, 128, 0.1)',
      label: t('motivation.broken'),
      message: t('motivation.broken')
    }
    default: return {
      icon: Sunny,
      color: '#f59e0b',
      bgColor: 'rgba(245, 158, 11, 0.1)',
      label: '-',
      message: ''
    }
  }
})

const isNewRecord = computed(() => {
  if (!streak.value) return false
  return streak.value.currentStreak >= (streak.value.longestStreak || 0)
})

// Methods
async function loadStreak() {
  // If streak data was provided via props, don't load from API
  if (streak.value !== null) {
    return
  }

  loading.value = true
  try {
    const response = await getStreakInfo()
    if (response.code === 0 || response.code === 200) {
      streak.value = response.data

      if (streak.value.currentStreak >= (streak.value.longestStreak || 0) && streak.value.currentStreak > 0) {
        showConfetti.value = true
        setTimeout(() => {
          showConfetti.value = false
        }, 3000)
      }
    } else {
      streak.value = generateMockStreak()
    }
  } catch (error) {
    console.error('Failed to load streak:', error)
    streak.value = generateMockStreak()
  } finally {
    loading.value = false
  }
}

function generateMockStreak(): StreakInfo {
  const today = new Date()
  const currentStreak = Math.floor(Math.random() * 30) + 1
  const longestStreak = Math.max(currentStreak, Math.floor(Math.random() * 60) + 30)
  const longestDate = new Date(today)
  longestDate.setDate(longestDate.getDate() - Math.floor(Math.random() * 30))
  
  return {
    currentStreak,
    longestStreak,
    longestStreakDate: longestDate.toISOString(),
    totalLearningDays: Math.floor(Math.random() * 200) + 50,
    lastActivityDate: new Date(today.getTime() - 24 * 60 * 60 * 1000).toISOString(),
    streakStatus: 'active',
    daysUntilRisk: 1
  }
}

function formatDate(date: string): string {
  return new Date(date).toLocaleDateString(undefined, {
    month: 'short',
    day: 'numeric',
    year: 'numeric'
  })
}

function getDaysAgo(date: string): string {
  const now = new Date()
  const target = new Date(date)
  const diffTime = Math.abs(now.getTime() - target.getTime())
  const diffDays = Math.floor(diffTime / (1000 * 60 * 60 * 24))
  
  if (diffDays === 0) return t('dashboard.today')
  if (diffDays === 1) return t('dashboard.yesterday')
  return t('dashboard.daysAgo', { n: diffDays })
}

// Lifecycle
onMounted(() => {
  loadStreak()
})

function getConfettiStyle(index: number): Record<string, string> {
  const colors = ['#f59e0b', '#3b82f6', '#10b981', '#ef4444', '#8b5cf6', '#ec4899']
  const left = Math.random() * 100
  const delay = Math.random() * 0.5
  const duration = 2 + Math.random() * 1

  return {
    left: `${left}%`,
    background: colors[index % colors.length],
    animationDelay: `${delay}s`,
    animationDuration: `${duration}s`
  }
}
</script>

<template>
  <div class="streak-display" :class="{ compact: props.compact }">
    <!-- Loading State -->
    <div v-if="loading" class="loading-state">
      <el-skeleton :rows="2" animated />
    </div>

    <!-- Streak Content -->
    <div v-else class="streak-content">
      <!-- Main Streak Card -->
      <div class="streak-card" :class="streakStatus">
        <div class="streak-header">
          <el-icon size="24" class="streak-icon" :style="{ color: statusConfig.color }">
            <component :is="statusConfig.icon" />
          </el-icon>
          <div class="streak-info">
            <div class="streak-value">
              <span class="number">{{ streak?.currentStreak || 0 }}</span>
              <span class="unit">{{ t('motivation.days') }}</span>
            </div>
            <div class="streak-label">{{ statusConfig.label }}</div>
          </div>
        </div>

        <div class="streak-details">
          <div class="detail-item">
            <span class="detail-label">{{ t('motivation.longestStreak') }}:</span>
            <span class="detail-value">{{ streak?.longestStreak || 0 }}</span>
          </div>
          <div class="detail-item">
            <span class="detail-label">{{ t('motivation.totalDays') }}:</span>
            <span class="detail-value">{{ streak?.totalLearningDays || 0 }}</span>
          </div>
          <div class="detail-item">
            <span class="detail-label">{{ t('motivation.lastActivity') }}:</span>
            <span class="detail-value">{{ getDaysAgo(streak?.lastActivityDate || '') }}</span>
          </div>
        </div>

        <div class="streak-message" :style="{ color: statusConfig.color }">
          {{ statusConfig.message }}
        </div>

        <!-- New Record Badge -->
        <div v-if="isNewRecord" class="new-record-badge">
          <el-icon size="14"><Trophy /></el-icon>
          {{ t('motivation.newRecord') }}
        </div>
      </div>

      <!-- Confetti Effect -->
      <div v-if="showConfetti" class="confetti-container">
        <div class="confetti" v-for="i in 20" :key="i" :style="getConfettiStyle(i)" />
      </div>
    </div>
  </div>
</template>

<style scoped>
.streak-display {
  padding: 16px;
  background: var(--bg-card);
  border-radius: 12px;
  border: 1px solid var(--border-light);
  position: relative;
  overflow: hidden;
}

.streak-display.compact {
  padding: 12px;
}

.loading-state {
  padding: 20px;
}

.streak-content {
  position: relative;
}

.streak-card {
  background: var(--bg-tertiary);
  border-radius: 10px;
  padding: 16px;
  border: 2px solid transparent;
  transition: all 0.3s ease;
  position: relative;
}

.streak-card.active {
  background: linear-gradient(135deg, rgba(245, 158, 11, 0.1) 0%, rgba(251, 191, 36, 0.05) 100%);
  border-color: #f59e0b;
}

.streak-card.at_risk {
  background: linear-gradient(135deg, rgba(239, 68, 68, 0.1) 0%, rgba(248, 113, 113, 0.05) 100%);
  border-color: #ef4444;
}

.streak-card.broken {
  background: linear-gradient(135deg, rgba(107, 114, 128, 0.1) 0%, rgba(156, 163, 175, 0.05) 100%);
  border-color: #6b7280;
}

.streak-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.streak-icon {
  background: var(--bg-card);
  border-radius: 8px;
  padding: 8px;
  flex-shrink: 0;
}

.streak-info {
  flex: 1;
}

.streak-value {
  display: flex;
  align-items: baseline;
  gap: 4px;
}

.streak-value .number {
  font-size: 32px;
  font-weight: 700;
  line-height: 1;
  color: var(--text-primary);
}

.streak-value .unit {
  font-size: 14px;
  font-weight: 500;
  color: var(--text-secondary);
}

.streak-label {
  font-size: 12px;
  color: var(--text-secondary);
  margin-top: 2px;
}

.streak-details {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid var(--border-light);
}

.detail-item {
  text-align: center;
}

.detail-label {
  display: block;
  font-size: 10px;
  color: var(--text-secondary);
  margin-bottom: 2px;
}

.detail-value {
  display: block;
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
}

.streak-message {
  margin-top: 12px;
  font-size: 12px;
  font-weight: 500;
  text-align: center;
  padding: 8px;
  background: var(--bg-card);
  border-radius: 6px;
}

.new-record-badge {
  position: absolute;
  top: -8px;
  right: -8px;
  background: var(--warning);
  color: white;
  padding: 4px 8px;
  border-radius: 12px;
  font-size: 10px;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 4px;
  box-shadow: 0 2px 8px rgba(245, 158, 11, 0.3);
  animation: pulse 2s infinite;
}

@keyframes pulse {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.05); }
}

/* Confetti Animation */
.confetti-container {
  position: absolute;
  inset: 0;
  pointer-events: none;
  overflow: hidden;
}

.confetti {
  position: absolute;
  width: 8px;
  height: 8px;
  background: var(--primary);
  border-radius: 50%;
  animation: confetti-fall 3s ease-out forwards;
}

@keyframes confetti-fall {
  0% {
    transform: translateY(0) rotate(0deg);
    opacity: 1;
  }
  100% {
    transform: translateY(100px) rotate(720deg);
    opacity: 0;
  }
}

/* Compact mode adjustments */
.streak-display.compact .streak-card {
  padding: 12px;
}

.streak-display.compact .streak-value .number {
  font-size: 24px;
}

.streak-display.compact .streak-details {
  grid-template-columns: 1fr;
  gap: 4px;
}

.streak-display.compact .detail-item {
  text-align: left;
}

.streak-display.compact .streak-message {
  margin-top: 8px;
  font-size: 11px;
  padding: 6px;
}

/* Element Plus overrides */
:deep(.el-skeleton) {
  --el-skeleton-bg: var(--bg-tertiary);
}
</style>