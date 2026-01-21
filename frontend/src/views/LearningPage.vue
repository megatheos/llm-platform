<script setup lang="ts">
import { ref, onMounted, computed, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { useAuthStore } from '@/stores/auth'
import LanguageSwitcher from '@/components/LanguageSwitcher.vue'
import {
  Refresh,
  Trophy,
  DataLine,
  Calendar,
  Clock,
  Star,
  Aim
} from '@element-plus/icons-vue'
import ReviewList from '@/components/personalized/ReviewList.vue'
import MemoryStatistics from '@/components/personalized/MemoryStatistics.vue'
import LearningInsights from '@/components/personalized/LearningInsights.vue'
import ProgressChart from '@/components/personalized/ProgressChart.vue'
import WeakAreasDisplay from '@/components/personalized/WeakAreasDisplay.vue'
import GoalSetting from '@/components/personalized/GoalSetting.vue'
import DailyTaskList from '@/components/personalized/DailyTaskList.vue'
import PlanAdjustmentNotice from '@/components/personalized/PlanAdjustmentNotice.vue'
import AchievementBadgeWall from '@/components/personalized/AchievementBadgeWall.vue'
import LearningHeatmap from '@/components/personalized/LearningHeatmap.vue'
import StreakDisplay from '@/components/personalized/StreakDisplay.vue'
import type {
  ReviewCardData,
  LearningProfile,
  StudyPlan,
  StreakInfo,
  Achievement,
  GoalSettingData,
  DailyTask,
  PlanAdjustment
} from '@/types/personalized'
import { getDueReviews } from '@/api/memory'
import { getLearningProfile } from '@/api/profile'
import { getActivePlan, getDailyTasks } from '@/api/plans'
import { getStreakInfo, getLearningHeatmap } from '@/api/motivation'
import { ElMessage } from 'element-plus'

const { t } = useI18n()
const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

// Active tab - check route meta.tab or default to 'review'
const activeTab = ref<string>((route.meta.tab as string) || 'review')

// Watch for route changes to update active tab
watch(
  () => route.meta.tab,
  (newTab) => {
    if (newTab) {
      activeTab.value = newTab as string
    }
  }
)

// Loading states
const loading = ref({
  review: false,
  insights: false,
  plan: false,
  achievements: false
})

// Error states
const errors = ref({
  review: null as string | null,
  insights: null as string | null,
  plan: null as string | null,
  achievements: null as string | null
})

// Real data from API
const reviewCards = ref<ReviewCardData[]>([])
const learningProfile = ref<LearningProfile | null>(null)
const currentPlan = ref<StudyPlan | null>(null)
const dailyTasks = ref<DailyTask[]>([])
const streakInfo = ref<StreakInfo | null>(null)
const achievements = ref<Achievement[]>([])

// Mock data - fallback when API fails
const reviewCardsMock = ref<ReviewCardData[]>([
  {
    item: {
      id: 1,
      contentId: 101,
      contentType: 'word',
      content: 'ephemeral',
      translation: '短暂的，转瞬即逝的',
      masteryLevel: 2,
      nextReviewDate: new Date().toISOString(),
      reviewCount: 3,
      easeFactor: 2.5,
      interval: 1
    },
    isDue: true,
    daysUntilReview: 0,
    suggestedAction: 'review'
  },
  {
    item: {
      id: 2,
      contentId: 102,
      contentType: 'word',
      content: 'serendipity',
      translation: '意外惊喜，机缘巧合',
      masteryLevel: 3,
      nextReviewDate: new Date(Date.now() + 86400000).toISOString(),
      reviewCount: 5,
      easeFactor: 2.6,
      interval: 3
    },
    isDue: false,
    daysUntilReview: 1,
    suggestedAction: 'restudy'
  },
  {
    item: {
      id: 3,
      contentId: 103,
      contentType: 'word',
      content: 'ubiquitous',
      translation: '普遍存在的，无处不在的',
      masteryLevel: 4,
      nextReviewDate: new Date(Date.now() + 2 * 86400000).toISOString(),
      reviewCount: 7,
      easeFactor: 2.7,
      interval: 7
    },
    isDue: false,
    daysUntilReview: 2,
    suggestedAction: 'mastered'
  }
])

const studyPlan = ref<StudyPlan>({
  id: 1,
  userId: authStore.user?.id || 1,
  title: '30-Day Vocabulary Challenge',
  description: 'Master 500 new words through spaced repetition',
  startDate: new Date(Date.now() - 10 * 86400000).toISOString(),
  endDate: new Date(Date.now() + 20 * 86400000).toISOString(),
  status: 'active',
  goals: [
    { id: 1, planId: 1, type: 'words', targetValue: 500, currentValue: 320, deadline: new Date(Date.now() + 20 * 86400000).toISOString(), isCompleted: false },
    { id: 2, planId: 1, type: 'quizzes', targetValue: 30, currentValue: 25, deadline: new Date(Date.now() + 20 * 86400000).toISOString(), isCompleted: false },
    { id: 3, planId: 1, type: 'time', targetValue: 30, currentValue: 10, deadline: new Date(Date.now() + 20 * 86400000).toISOString(), isCompleted: false }
  ],
  dailyTasks: [
    { id: 1, planId: 1, date: new Date().toISOString().split('T')[0], type: 'review', targetCount: 20, completedCount: 15, status: 'in_progress' },
    { id: 2, planId: 1, date: new Date().toISOString().split('T')[0], type: 'new', targetCount: 10, completedCount: 8, status: 'in_progress' },
    { id: 3, planId: 1, date: new Date().toISOString().split('T')[0], type: 'quiz', targetCount: 2, completedCount: 2, status: 'completed' }
  ],
  createdAt: new Date(Date.now() - 10 * 86400000).toISOString(),
  updatedAt: new Date().toISOString()
})

const goalSettings = ref<GoalSettingData>({
  dailyWordTarget: 20,
  dailyQuizTarget: 3,
  dailyDialogueTarget: 1,
  weeklyStudyHours: 10,
  preferredStudyTime: 'morning',
  focusAreas: ['vocabulary', 'grammar'],
  difficultyLevel: 'medium'
})

const planAdjustments = ref<PlanAdjustment[]>([
  {
    id: 1,
    planId: 1,
    type: 'difficulty_adjustment',
    reason: 'Your completion rate has been above 90% this week. Consider increasing difficulty.',
    oldValue: 'medium',
    newValue: 'hard',
    suggestedAt: new Date().toISOString(),
    isRead: false
  }
])

// Computed
const todayTasks = computed(() => studyPlan.value.dailyTasks.filter(t => t.date === new Date().toISOString().split('T')[0]))
const overallProgress = computed(() => {
  const total = studyPlan.value.goals.reduce((sum, g) => sum + g.targetValue, 0)
  const current = studyPlan.value.goals.reduce((sum, g) => sum + g.currentValue, 0)
  return Math.round((current / total) * 100)
})

// API Loading Functions
async function loadReviewData() {
  loading.value.review = true
  errors.value.review = null
  try {
    const response = await getDueReviews(10)
    if (response.code === 0 || response.code === 200) {
      // Transform API data to component format
      reviewCards.value = response.data.map((record: any) => ({
        item: {
          id: record.id,
          contentId: record.contentId,
          contentType: record.contentType,
          content: record.content || record.word,
          translation: record.translation || '',
          masteryLevel: record.masteryLevel || 0,
          nextReviewDate: record.nextReviewDate,
          reviewCount: record.reviewCount || 0,
          easeFactor: record.easeFactor || 2.5,
          interval: record.interval || 0
        },
        isDue: true,
        daysUntilReview: 0,
        suggestedAction: 'review'
      }))
    } else {
      errors.value.review = response.message || 'Failed to load review data'
      // Fallback to mock data
      reviewCards.value = reviewCardsMock.value
    }
  } catch (error) {
    console.error('Error loading review data:', error)
    errors.value.review = 'Network error loading review data'
    // Fallback to mock data
    reviewCards.value = reviewCardsMock.value
  } finally {
    loading.value.review = false
  }
}

async function loadInsightsData() {
  loading.value.insights = true
  errors.value.insights = null
  try {
    const response = await getLearningProfile()
    if (response.code === 0 || response.code === 200) {
      learningProfile.value = response.data
    } else {
      errors.value.insights = response.message || 'Failed to load profile data'
    }
  } catch (error) {
    console.error('Error loading insights data:', error)
    errors.value.insights = 'Network error loading insights data'
  } finally {
    loading.value.insights = false
  }
}

async function loadPlanData() {
  loading.value.plan = true
  errors.value.plan = null
  try {
    const [planResponse, tasksResponse] = await Promise.all([
      getActivePlan(),
      getDailyTasks(new Date().toISOString().split('T')[0])
    ])

    if (planResponse.code === 0 || planResponse.code === 200) {
      currentPlan.value = planResponse.data
    }

    if (tasksResponse.code === 0 || tasksResponse.code === 200) {
      dailyTasks.value = tasksResponse.data
    }

    if (planResponse.code !== 0 && planResponse.code !== 200) {
      errors.value.plan = planResponse.message || 'Failed to load plan data'
    }
  } catch (error) {
    console.error('Error loading plan data:', error)
    errors.value.plan = 'Network error loading plan data'
  } finally {
    loading.value.plan = false
  }
}

async function loadAchievementsData() {
  loading.value.achievements = true
  errors.value.achievements = null
  try {
    const [streakResponse] = await Promise.all([
      getStreakInfo()
    ])

    if (streakResponse.code === 0 || streakResponse.code === 200) {
      streakInfo.value = streakResponse.data
    } else {
      errors.value.achievements = streakResponse.message || 'Failed to load achievements data'
    }
  } catch (error) {
    console.error('Error loading achievements data:', error)
    errors.value.achievements = 'Network error loading achievements data'
  } finally {
    loading.value.achievements = false
  }
}

// Lifecycle
onMounted(async () => {
  // Load data based on current active tab
  await loadReviewData()
  await loadInsightsData()
  await loadPlanData()
  await loadAchievementsData()
})

// Methods
function handleReviewSubmit(itemId: number, quality: number) {
  console.log('Review submitted:', itemId, quality)
  reviewCards.value = reviewCards.value.filter(r => r.item.id !== itemId)
}

function handleReviewSkip(itemId: number) {
  console.log('Review skipped:', itemId)
  reviewCards.value = reviewCards.value.filter(r => r.item.id !== itemId)
}

function handleTaskComplete(taskId: number) {
  const task = todayTasks.value.find(t => t.id === taskId)
  if (task) {
    task.status = 'completed'
    task.completedCount = task.targetCount
  }
}

function handleGoalUpdate(settings: GoalSettingData) {
  goalSettings.value = settings
}

function handlePlanAdjustmentRead(adjustmentId: number) {
  const adjustment = planAdjustments.value.find(a => a.id === adjustmentId)
  if (adjustment) {
    adjustment.isRead = true
  }
}
</script>

<template>
  <div class="learning-page">
    <!-- Header -->
    <header class="page-header">
      <div class="header-left">
        <h1>{{ t(`nav.${route.meta.navKey}`) }}</h1>
        <p class="header-subtitle">{{ t('learning.subtitle') }}</p>
      </div>
      <div class="header-right">
        <LanguageSwitcher />
      </div>
    </header>

    <!-- Tab Navigation -->
    <div class="tab-navigation">
      <a
        class="tab-item"
        :class="{ active: activeTab === 'review' }"
        @click="router.push('/review')"
      >
        <el-icon :size="16"><Refresh /></el-icon>
        <span>{{ t('learning.tabs.review') }}</span>
      </a>
      <a
        class="tab-item"
        :class="{ active: activeTab === 'insights' }"
        @click="router.push('/insights')"
      >
        <el-icon :size="16"><DataLine /></el-icon>
        <span>{{ t('learning.tabs.insights') }}</span>
      </a>
      <a
        class="tab-item"
        :class="{ active: activeTab === 'plan' }"
        @click="router.push('/plan')"
      >
        <el-icon :size="16"><Aim /></el-icon>
        <span>{{ t('learning.tabs.plan') }}</span>
      </a>
      <a
        class="tab-item"
        :class="{ active: activeTab === 'achievements' }"
        @click="router.push('/achievements')"
      >
        <el-icon :size="16"><Trophy /></el-icon>
        <span>{{ t('learning.tabs.achievements') }}</span>
      </a>
    </div>

    <!-- Content -->
    <main class="main-content">
      <!-- Review Tab -->
      <section v-if="activeTab === 'review'" class="tab-content">
        <div class="content-grid">
          <!-- Left: Review Cards -->
          <div class="review-section">
            <div class="section-header">
              <h2>{{ t('learning.review.dueToday') }}</h2>
              <span class="badge">{{ reviewCards.length }} {{ t('learning.review.items') }}</span>
            </div>

            <!-- Loading State -->
            <div v-if="loading.review" class="loading-state">
              <el-skeleton :rows="3" animated />
            </div>

            <!-- Error State -->
            <el-alert
              v-else-if="errors.review"
              :title="errors.review"
              type="error"
              show-icon
              :closable="false"
              class="mb-4"
            />

            <!-- Content -->
            <div v-else>
              <ReviewList
                :cards="reviewCards"
                @submit="handleReviewSubmit"
                @skip="handleReviewSkip"
              />
              <div v-if="reviewCards.length === 0" class="empty-state">
                <el-empty :description="t('learning.review.allDone')" :image-size="100" />
              </div>
            </div>
          </div>

          <!-- Right: Memory Stats -->
          <div class="stats-section">
            <MemoryStatistics
              :total-items="156"
              :due-today="reviewCards.length"
              :mastered="89"
              :new-today="5"
            />
          </div>
        </div>
      </section>

      <!-- Insights Tab -->
      <section v-if="activeTab === 'insights'" class="tab-content">
        <div class="content-grid">
          <!-- Loading State -->
          <div v-if="loading.insights" class="loading-state full-width">
            <el-skeleton :rows="6" animated />
          </div>

          <!-- Error State -->
          <el-alert
            v-else-if="errors.insights"
            :title="errors.insights"
            type="error"
            show-icon
            :closable="false"
            class="mb-4"
          />

          <!-- Content -->
          <div v-else class="insights-main">
            <LearningInsights
              :profile="learningProfile || undefined"
              :weekly-progress="{
                weekStartDate: new Date(Date.now() - 7 * 86400000).toISOString(),
                weekEndDate: new Date().toISOString(),
                days: [],
                totalActivities: 45,
                totalTimeMinutes: 320,
                averageDailyActivities: 6.4,
                improvementRate: 12
              }"
            />
          </div>

          <!-- Side: Charts & Analytics -->
          <div class="insights-side">
            <ProgressChart
              :data="[
                { name: 'Vocabulary', value: 85 },
                { name: 'Grammar', value: 78 },
                { name: 'Speaking', value: 65 },
                { name: 'Listening', value: 72 }
              ]"
            />
            <WeakAreasDisplay
              :areas="learningProfile?.weakestAreas || []"
            />
          </div>
        </div>
      </section>

      <!-- Plan Tab -->
      <section v-if="activeTab === 'plan'" class="tab-content">
        <div class="content-grid">
          <!-- Loading State -->
          <div v-if="loading.plan" class="loading-state full-width">
            <el-skeleton :rows="6" animated />
          </div>

          <!-- Error State -->
          <el-alert
            v-else-if="errors.plan"
            :title="errors.plan"
            type="error"
            show-icon
            :closable="false"
            class="mb-4"
          />

          <!-- Content -->
          <div v-else class="plan-main">
            <GoalSetting
              :settings="goalSettings"
              :goals="studyPlan.goals"
              @update="handleGoalUpdate"
            />
            <DailyTaskList
              :tasks="todayTasks"
              @complete="handleTaskComplete"
            />
            <PlanAdjustmentNotice
              :adjustments="planAdjustments"
              @read="handlePlanAdjustmentRead"
            />
          </div>

          <!-- Side: Plan Progress -->
          <div class="plan-side">
            <div class="progress-card">
              <h3>{{ t('learning.plan.progress') }}</h3>
              <div class="progress-value">{{ overallProgress }}%</div>
              <el-progress :percentage="overallProgress" :stroke-width="10" />
            </div>
          </div>
        </div>
      </section>

      <!-- Achievements Tab -->
      <section v-if="activeTab === 'achievements'" class="tab-content">
        <div class="content-grid">
          <!-- Loading State -->
          <div v-if="loading.achievements" class="loading-state full-width">
            <el-skeleton :rows="6" animated />
          </div>

          <!-- Error State -->
          <el-alert
            v-else-if="errors.achievements"
            :title="errors.achievements"
            type="error"
            show-icon
            :closable="false"
            class="mb-4"
          />

          <!-- Content -->
          <div v-else class="achievements-main">
            <AchievementBadgeWall
              :achievements="achievements"
              :total-count="12"
              :unlocked-count="achievements.length"
            />
          </div>

          <!-- Side: Heatmap & Streak -->
          <div class="achievements-side">
            <LearningHeatmap
              :year="2026"
              :months="[]"
              :total-activities="156"
              :max-daily-activities="12"
            />
            <StreakDisplay
              :current-streak="(streakInfo as any)?.currentStreak || 0"
              :longest-streak="(streakInfo as any)?.longestStreak || 0"
              :total-learning-days="(streakInfo as any)?.totalLearningDays || 0"
              :streak-status="(streakInfo as any)?.streakStatus || 'active'"
              :days-until-risk="(streakInfo as any)?.daysUntilRisk || 0"
            />
          </div>
        </div>
      </section>
    </main>
  </div>
</template>

<style scoped>
.learning-page {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  background: var(--bg-primary);
}

/* Header */
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 24px;
}

.page-header h1 {
  font-size: var(--font-size-2xl);
  font-weight: 700;
  color: var(--text-primary);
  margin-bottom: 4px;
}

.header-subtitle {
  color: var(--text-muted);
  font-size: var(--font-size-sm);
}

.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

/* Tab Navigation */
.tab-navigation {
  display: flex;
  gap: 8px;
  margin-bottom: 24px;
  border-bottom: 1px solid var(--border-light);
  padding-bottom: 0;
}

.tab-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  border-radius: 8px 8px 0 0;
  color: var(--text-secondary);
  cursor: pointer;
  transition: all 0.2s ease;
  text-decoration: none;
  font-size: 14px;
  font-weight: 500;
}

.tab-item:hover {
  background: var(--bg-secondary);
  color: var(--text-primary);
}

.tab-item.active {
  background: var(--bg-secondary);
  color: var(--primary);
  border-bottom: 2px solid var(--primary);
}

/* Main Content */
.main-content {
  flex: 1;
  width: 100%;
}

/* Tab Content */
.tab-content {
  animation: fadeIn 0.3s ease;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}

/* Content Grid */
.content-grid {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 24px;
}

/* Section Headers */
.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.section-header h2 {
  font-size: var(--font-size-lg);
  font-weight: 600;
  color: var(--text-primary);
  margin: 0;
}

.badge {
  background: var(--color-accent);
  color: white;
  padding: 4px 8px;
  border-radius: 12px;
  font-size: var(--font-size-xs);
  font-weight: 600;
}

/* Loading State */
.loading-state {
  padding: 20px 0;
}

.loading-state.full-width {
  grid-column: 1 / -1;
}

/* Empty State */
.empty-state {
  text-align: center;
  padding: 40px 0;
}

.mb-4 {
  margin-bottom: 16px;
}

/* Progress Card */
.progress-card {
  background: var(--bg-secondary);
  border-radius: var(--radius-lg);
  padding: 20px;
  text-align: center;
}

.progress-card h3 {
  font-size: var(--font-size-md);
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 12px;
}

.progress-value {
  font-size: 48px;
  font-weight: 700;
  color: var(--color-primary);
  margin-bottom: 16px;
}

/* Responsive */
@media (max-width: 1024px) {
  .content-grid {
    grid-template-columns: 1fr;
  }

  .insights-side,
  .plan-side,
  .achievements-side {
    grid-column: 1;
  }
}

@media (max-width: 768px) {
  .tab-navigation {
    overflow-x: auto;
  }

  .tab-item {
    flex-shrink: 0;
  }
}
</style>
