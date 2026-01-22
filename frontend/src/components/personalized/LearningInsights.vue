<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElProgress, ElCard, ElEmpty } from 'element-plus'
import {
  TrendCharts,
  Trophy,
  Check,
  ArrowUp,
  ArrowDown,
  Minus,
  Star,
  InfoFilled,
  Aim
} from '@element-plus/icons-vue'
import type { LearningProfile, WeeklyProgress, Achievement, Milestone } from '@/types/personalized'

const { t } = useI18n()

// Props
interface Props {
  profile?: LearningProfile
  weeklyProgress?: WeeklyProgress
  recommendations?: string[]
  achievements?: Achievement[]
  nextMilestones?: Milestone[]
}

const props = withDefaults(defineProps<Props>(), {
  profile: () => ({
    id: 0,
    userId: 0,
    totalLearningDays: 0,
    totalStudyHours: 0,
    totalWordsLearned: 0,
    totalDialoguesCompleted: 0,
    totalQuizzesCompleted: 0,
    averageQuizScore: 0,
    preferredLanguages: [],
    strongestAreas: [],
    weakestAreas: [],
    learningStyle: 'mixed',
    peakHours: [],
    averageSessionLength: 0,
    createdAt: '',
    updatedAt: ''
  }),
  weeklyProgress: () => ({
    weekStartDate: '',
    weekEndDate: '',
    days: [],
    totalActivities: 0,
    totalTimeMinutes: 0,
    averageDailyActivities: 0,
    improvementRate: 0
  }),
  recommendations: () => [],
  achievements: () => [],
  nextMilestones: () => []
})

// State
const loading = ref(false)
const selectedPeriod = ref<'week' | 'month'>('week')

// Computed
const improvementRate = computed(() => props.weeklyProgress?.improvementRate || 0)

const improvementClass = computed(() => {
  if (improvementRate.value > 0) return 'positive'
  if (improvementRate.value < 0) return 'negative'
  return 'neutral'
})

const improvementIcon = computed(() => {
  if (improvementRate.value > 0) return ArrowUp
  if (improvementRate.value < 0) return ArrowDown
  return Minus
})

const improvementColor = computed(() => {
  if (improvementRate.value > 0) return '#10b981'
  if (improvementRate.value < 0) return '#ef4444'
  return '#6b7280'
})

// Methods
function getTrendIcon(trend: string) {
  switch (trend) {
    case 'improving': return ArrowUp
    case 'declining': return ArrowDown
    default: return Minus
  }
}

function getTrendColor(trend: string): string {
  switch (trend) {
    case 'improving': return '#10b981'
    case 'declining': return '#ef4444'
    default: return '#6b7280'
  }
}

function formatDuration(minutes: number): string {
  if (minutes < 60) return `${minutes}m`
  const hours = Math.floor(minutes / 60)
  const mins = minutes % 60
  return mins > 0 ? `${hours}h ${mins}m` : `${hours}h`
}

function getMilestoneProgress(milestone: Milestone): number {
  if (!milestone.targetValue) return 0
  return Math.min(100, Math.round((milestone.currentValue / milestone.targetValue) * 100))
}

function getAreaProgress(value: number): number {
  return Math.min(100, value)
}
</script>

<template>
  <div class="learning-insights">
    <!-- Profile Summary -->
    <div class="insight-section">
      <h3 class="section-title">
        <el-icon><TrendCharts /></el-icon>
        {{ t('profile.insights') }}
      </h3>

      <div class="stats-grid">
        <div class="stat-card">
          <div class="stat-icon words">📚</div>
          <div class="stat-content">
            <span class="stat-value">{{ profile.totalWordsLearned }}</span>
            <span class="stat-label">{{ t('profile.totalWords') }}</span>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-icon time">⏱️</div>
          <div class="stat-content">
            <span class="stat-value">{{ profile.totalStudyHours }}h</span>
            <span class="stat-label">{{ t('profile.studyTime') }}</span>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-icon score">🎯</div>
          <div class="stat-content">
            <span class="stat-value">{{ profile.averageQuizScore }}%</span>
            <span class="stat-label">{{ t('profile.averageScore') }}</span>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-icon days">🔥</div>
          <div class="stat-content">
            <span class="stat-value">{{ profile.totalLearningDays }}</span>
            <span class="stat-label">{{ t('profile.learningDays') }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- Improvement Rate -->
    <div class="insight-section">
      <div class="improvement-card">
        <div class="improvement-header">
          <span class="improvement-label">Weekly Progress</span>
          <div class="improvement-rate" :style="{ color: improvementColor }">
            <el-icon>
              <component :is="improvementIcon" />
            </el-icon>
            <span>{{ Math.abs(improvementRate) }}%</span>
          </div>
        </div>
        <el-progress
          :percentage="50 + improvementRate / 2"
          :stroke-width="8"
          :show-text="false"
          :color="improvementColor"
        />
        <div class="improvement-details">
          <div class="detail-item">
            <span class="detail-icon">📊</span>
            <span class="detail-value">{{ weeklyProgress?.totalActivities || 0 }}</span>
            <span class="detail-label">Activities</span>
          </div>
          <div class="detail-item">
            <span class="detail-icon">⏰</span>
            <span class="detail-value">{{ formatDuration(weeklyProgress?.totalTimeMinutes || 0) }}</span>
            <span class="detail-label">Study Time</span>
          </div>
          <div class="detail-item">
            <span class="detail-icon">📈</span>
            <span class="detail-value">{{ weeklyProgress?.averageDailyActivities?.toFixed(1) || 0 }}</span>
            <span class="detail-label">Daily Avg</span>
          </div>
        </div>
      </div>
    </div>

    <!-- Recommendations -->
    <div v-if="recommendations.length > 0" class="insight-section">
      <h3 class="section-title">
        <el-icon><Aim /></el-icon>
        {{ t('profile.recommendations') }}
      </h3>
      <ul class="recommendations-list">
        <li v-for="(rec, index) in recommendations" :key="index" class="recommendation-item">
          <el-icon class="rec-icon"><InfoFilled /></el-icon>
          <span>{{ rec }}</span>
        </li>
      </ul>
    </div>

    <!-- Milestones -->
    <div v-if="nextMilestones.length > 0" class="insight-section">
      <h3 class="section-title">
        <el-icon><Trophy /></el-icon>
        Next Milestones
      </h3>
      <div class="milestones-list">
        <div v-for="milestone in nextMilestones" :key="milestone.id" class="milestone-item">
          <div class="milestone-header">
            <span class="milestone-title">{{ milestone.title }}</span>
            <span class="milestone-progress">{{ milestone.currentValue }}/{{ milestone.targetValue }}</span>
          </div>
          <el-progress
            :percentage="getMilestoneProgress(milestone)"
            :stroke-width="6"
            :show-text="false"
            color="#3b82f6"
          />
        </div>
      </div>
    </div>

    <!-- Recent Achievements -->
    <div v-if="achievements.length > 0" class="insight-section">
      <h3 class="section-title">
        <el-icon><Star /></el-icon>
        {{ t('motivation.recentAchievements') }}
      </h3>
      <div class="achievements-preview">
        <div
          v-for="achievement in achievements.slice(0, 3)"
          :key="achievement.id"
          class="achievement-item"
        >
          <span class="achievement-icon">{{ achievement.icon }}</span>
          <span class="achievement-title">{{ achievement.title }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.learning-insights {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.insight-section {
  background: var(--bg-secondary);
  border-radius: var(--radius-lg);
  padding: 20px;
  border: 1px solid var(--border-light);
}

.section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: var(--font-size-md);
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 16px;
}

.section-title .el-icon {
  color: var(--color-primary);
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
}

.stat-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px;
  background: var(--bg-tertiary);
  border-radius: var(--radius-md);
  transition: all 0.2s ease;
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}

.stat-icon {
  width: 44px;
  height: 44px;
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
}

.stat-icon.words {
  background: #3b82f620;
}

.stat-icon.time {
  background: #f59e0b20;
}

.stat-icon.score {
  background: #10b98120;
}

.stat-icon.days {
  background: #ef444420;
}

.stat-content {
  display: flex;
  flex-direction: column;
}

.stat-value {
  font-size: var(--font-size-xl);
  font-weight: 700;
  color: var(--text-primary);
}

.stat-label {
  font-size: var(--font-size-xs);
  color: var(--text-muted);
}

.improvement-card {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.improvement-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.improvement-label {
  font-size: var(--font-size-sm);
  color: var(--text-muted);
}

.improvement-rate {
  display: flex;
  align-items: center;
  gap: 4px;
  font-weight: 600;
  font-size: var(--font-size-lg);
}

.improvement-details {
  display: flex;
  justify-content: space-around;
  padding-top: 12px;
  border-top: 1px solid var(--border-light);
  margin-top: 8px;
}

.detail-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

.detail-icon {
  font-size: 18px;
}

.detail-value {
  font-size: var(--font-size-md);
  font-weight: 600;
  color: var(--text-primary);
}

.detail-label {
  font-size: var(--font-size-xs);
  color: var(--text-muted);
}

.recommendations-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.recommendation-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 14px;
  background: var(--bg-tertiary);
  border-radius: var(--radius-md);
  font-size: var(--font-size-sm);
  color: var(--text-secondary);
  list-style: none;
}

.rec-icon {
  color: var(--color-primary);
  margin-top: 2px;
  flex-shrink: 0;
}

.milestones-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.milestone-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.milestone-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.milestone-title {
  font-size: var(--font-size-sm);
  font-weight: 500;
  color: var(--text-primary);
}

.milestone-progress {
  font-size: var(--font-size-xs);
  color: var(--text-muted);
}

.achievements-preview {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.achievement-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  background: var(--bg-tertiary);
  border-radius: var(--radius-md);
  transition: all 0.2s ease;
}

.achievement-item:hover {
  transform: translateX(4px);
}

.achievement-icon {
  font-size: 20px;
}

.achievement-title {
  font-size: var(--font-size-sm);
  color: var(--text-primary);
}

@media (max-width: 480px) {
  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .improvement-details {
    gap: 16px;
  }
}
</style>