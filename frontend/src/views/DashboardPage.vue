<script setup lang="ts">
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'
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
  InfoFilled
} from '@element-plus/icons-vue'

const router = useRouter()
const recordsStore = useRecordsStore()
const authStore = useAuthStore()

// Quick access features
const features = [
  {
    name: 'Word Query',
    description: 'Look up words and translations',
    icon: 'Search',
    route: '/word-query',
    color: '#409eff',
    bgColor: '#ecf5ff'
  },
  {
    name: 'Dialogue Practice',
    description: 'Practice conversations with AI',
    icon: 'ChatDotRound',
    route: '/dialogue',
    color: '#67c23a',
    bgColor: '#f0f9eb'
  },
  {
    name: 'Take a Quiz',
    description: 'Test your language knowledge',
    icon: 'Document',
    route: '/quiz',
    color: '#e6a23c',
    bgColor: '#fdf6ec'
  },
  {
    name: 'Learning Records',
    description: 'View your learning history',
    icon: 'DataLine',
    route: '/records',
    color: '#909399',
    bgColor: '#f4f4f5'
  }
]

// Navigate to feature
function navigateTo(route: string) {
  router.push(route)
}

// Format relative time
function formatRelativeTime(dateStr: string): string {
  const date = new Date(dateStr)
  const now = new Date()
  const diffMs = now.getTime() - date.getTime()
  const diffDays = Math.floor(diffMs / (1000 * 60 * 60 * 24))
  
  if (diffDays === 0) return 'Today'
  if (diffDays === 1) return 'Yesterday'
  if (diffDays < 7) return `${diffDays} days ago`
  if (diffDays < 30) return `${Math.floor(diffDays / 7)} weeks ago`
  return `${Math.floor(diffDays / 30)} months ago`
}

// Get activity description
function getActivityDescription(record: LearningRecord): string {
  const details = record.activityDetails
  if (!details) return 'Activity completed'
  
  switch (record.activityType) {
    case 'WORD_QUERY':
      const wordDetails = details as any
      return `Queried "${wordDetails.word}"`
    case 'DIALOGUE':
      const dialogueDetails = details as any
      return `${dialogueDetails.scenarioName}`
    case 'QUIZ':
      const quizDetails = details as any
      return `Score: ${quizDetails.score}/${quizDetails.totalScore}`
    default:
      return 'Activity completed'
  }
}

// Fetch data on mount
onMounted(async () => {
  await Promise.all([
    recordsStore.fetchRecords({ pageSize: 5 }),
    recordsStore.fetchStatistics()
  ])
})
</script>

<template>
  <div class="dashboard-page">
    <!-- Welcome Section -->
    <el-row :gutter="20">
      <el-col :span="24">
        <el-card class="welcome-card">
          <div class="welcome-content">
            <div class="welcome-text">
              <h1>Welcome back, {{ authStore.user?.username || 'Learner' }}!</h1>
              <p>Continue your language learning journey</p>
            </div>
            <div class="welcome-stats" v-if="recordsStore.statistics">
              <div class="mini-stat">
                <span class="mini-stat-value">{{ recordsStore.statistics.activitiesLast7Days || 0 }}</span>
                <span class="mini-stat-label">Activities this week</span>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- Statistics Overview -->
    <el-row :gutter="20" class="stats-section">
      <el-col :xs="12" :sm="6">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-content">
            <el-icon class="stat-icon word-icon" :size="32"><Search /></el-icon>
            <div class="stat-info">
              <div class="stat-value">{{ recordsStore.statistics?.totalWordQueries || 0 }}</div>
              <div class="stat-label">Words Learned</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="6">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-content">
            <el-icon class="stat-icon dialogue-icon" :size="32"><ChatDotRound /></el-icon>
            <div class="stat-info">
              <div class="stat-value">{{ recordsStore.statistics?.totalDialogueSessions || 0 }}</div>
              <div class="stat-label">Conversations</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="6">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-content">
            <el-icon class="stat-icon quiz-icon" :size="32"><Document /></el-icon>
            <div class="stat-info">
              <div class="stat-value">{{ recordsStore.statistics?.totalQuizzes || 0 }}</div>
              <div class="stat-label">Quizzes Taken</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="6">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-content">
            <el-icon class="stat-icon score-icon" :size="32"><Trophy /></el-icon>
            <div class="stat-info">
              <div class="stat-value">{{ Math.round(recordsStore.statistics?.averageQuizScore || 0) }}%</div>
              <div class="stat-label">Avg Score</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- Quick Access Section -->
    <el-row :gutter="20" class="features-section">
      <el-col :span="24">
        <el-card>
          <template #header>
            <div class="section-header">
              <span>Quick Access</span>
              <span class="section-subtitle">Start learning now</span>
            </div>
          </template>
          <el-row :gutter="20">
            <el-col 
              v-for="feature in features" 
              :key="feature.name"
              :xs="12" 
              :sm="6"
            >
              <div 
                class="feature-card"
                @click="navigateTo(feature.route)"
              >
                <div 
                  class="feature-icon"
                  :style="{ backgroundColor: feature.bgColor, color: feature.color }"
                >
                  <el-icon :size="28">
                    <Search v-if="feature.icon === 'Search'" />
                    <ChatDotRound v-else-if="feature.icon === 'ChatDotRound'" />
                    <Document v-else-if="feature.icon === 'Document'" />
                    <DataLine v-else />
                  </el-icon>
                </div>
                <div class="feature-info">
                  <div class="feature-name">{{ feature.name }}</div>
                  <div class="feature-desc">{{ feature.description }}</div>
                </div>
              </div>
            </el-col>
          </el-row>
        </el-card>
      </el-col>
    </el-row>

    <!-- Recent Activities Section -->
    <el-row :gutter="20" class="recent-section">
      <el-col :span="24">
        <el-card>
          <template #header>
            <div class="section-header">
              <span>Recent Activities</span>
              <el-button type="primary" link @click="navigateTo('/records')">
                View All <el-icon><ArrowRight /></el-icon>
              </el-button>
            </div>
          </template>

          <!-- Loading State -->
          <div v-if="recordsStore.loading" class="loading-container">
            <el-skeleton :rows="3" animated />
          </div>

          <!-- Recent Activities List -->
          <div v-else-if="recordsStore.hasRecords" class="recent-list">
            <div
              v-for="record in recordsStore.records.slice(0, 5)"
              :key="record.id"
              class="recent-item"
            >
              <div class="recent-icon">
                <el-icon 
                  :size="20" 
                  :class="['activity-icon', `${record.activityType.toLowerCase()}-icon`]"
                >
                  <Search v-if="record.activityType === 'WORD_QUERY'" />
                  <ChatDotRound v-else-if="record.activityType === 'DIALOGUE'" />
                  <Document v-else />
                </el-icon>
              </div>
              <div class="recent-content">
                <div class="recent-type">
                  <el-tag 
                    :type="recordsStore.getActivityTypeColor(record.activityType)"
                    size="small"
                  >
                    {{ recordsStore.getActivityTypeLabel(record.activityType) }}
                  </el-tag>
                </div>
                <div class="recent-desc">{{ getActivityDescription(record) }}</div>
              </div>
              <div class="recent-time">{{ formatRelativeTime(record.activityTime) }}</div>
            </div>
          </div>

          <!-- Empty State -->
          <el-empty
            v-else
            description="No activities yet. Start learning to see your progress!"
            :image-size="100"
          >
            <el-button type="primary" @click="navigateTo('/word-query')">
              Start Learning
            </el-button>
          </el-empty>
        </el-card>
      </el-col>
    </el-row>

    <!-- Learning Tips Section -->
    <el-row :gutter="20" class="tips-section">
      <el-col :span="24">
        <el-card class="tips-card">
          <div class="tips-content">
            <el-icon class="tips-icon" :size="24"><InfoFilled /></el-icon>
            <div class="tips-text">
              <strong>Learning Tip:</strong> Consistent practice is key! Try to complete at least one activity each day to maintain your progress.
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<style scoped>
.dashboard-page {
  padding: 20px;
  min-height: calc(100vh - 100px);
  background-color: #f5f7fa;
}

/* Welcome Section */
.welcome-card {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
  margin-bottom: 20px;
}

.welcome-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  color: white;
}

.welcome-text h1 {
  margin: 0 0 8px 0;
  font-size: 24px;
  font-weight: 600;
}

.welcome-text p {
  margin: 0;
  opacity: 0.9;
}

.welcome-stats {
  text-align: right;
}

.mini-stat {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
}

.mini-stat-value {
  font-size: 32px;
  font-weight: 700;
}

.mini-stat-label {
  font-size: 14px;
  opacity: 0.9;
}

/* Statistics Section */
.stats-section {
  margin-bottom: 20px;
}

.stat-card {
  height: 100%;
  cursor: default;
}

.stat-content {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 8px 0;
}

.stat-icon {
  padding: 12px;
  border-radius: 12px;
}

.word-icon {
  background-color: #ecf5ff;
  color: #409eff;
}

.dialogue-icon {
  background-color: #f0f9eb;
  color: #67c23a;
}

.quiz-icon {
  background-color: #fdf6ec;
  color: #e6a23c;
}

.score-icon {
  background-color: #fef0f0;
  color: #f56c6c;
}

.stat-info {
  flex: 1;
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: #303133;
  line-height: 1.2;
}

.stat-label {
  font-size: 14px;
  color: #909399;
}

/* Section Headers */
.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-weight: 600;
}

.section-subtitle {
  font-size: 14px;
  font-weight: normal;
  color: #909399;
}

/* Features Section */
.features-section {
  margin-bottom: 20px;
}

.feature-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 24px 16px;
  border-radius: 12px;
  background-color: #fafafa;
  cursor: pointer;
  transition: all 0.3s;
  text-align: center;
}

.feature-card:hover {
  background-color: #f0f2f5;
  transform: translateY(-4px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.feature-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 56px;
  height: 56px;
  border-radius: 16px;
  margin-bottom: 12px;
}

.feature-info {
  width: 100%;
}

.feature-name {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 4px;
}

.feature-desc {
  font-size: 12px;
  color: #909399;
}

/* Recent Activities Section */
.recent-section {
  margin-bottom: 20px;
}

.loading-container {
  padding: 20px;
}

.recent-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.recent-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  background-color: #fafafa;
  border-radius: 8px;
  transition: all 0.3s;
}

.recent-item:hover {
  background-color: #f5f7fa;
}

.recent-icon {
  flex-shrink: 0;
  padding: 8px;
  border-radius: 8px;
  background-color: #fff;
}

.activity-icon.word_query-icon {
  color: #409eff;
}

.activity-icon.dialogue-icon {
  color: #67c23a;
}

.activity-icon.quiz-icon {
  color: #e6a23c;
}

.recent-content {
  flex: 1;
  min-width: 0;
}

.recent-type {
  margin-bottom: 4px;
}

.recent-desc {
  font-size: 14px;
  color: #606266;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.recent-time {
  flex-shrink: 0;
  font-size: 12px;
  color: #909399;
}

/* Tips Section */
.tips-section {
  margin-bottom: 20px;
}

.tips-card {
  background-color: #f0f9eb;
  border: 1px solid #e1f3d8;
}

.tips-content {
  display: flex;
  align-items: center;
  gap: 12px;
}

.tips-icon {
  color: #67c23a;
  flex-shrink: 0;
}

.tips-text {
  font-size: 14px;
  color: #606266;
}

/* Responsive adjustments */
@media (max-width: 768px) {
  .welcome-content {
    flex-direction: column;
    text-align: center;
    gap: 16px;
  }
  
  .welcome-stats {
    text-align: center;
  }
  
  .stat-content {
    flex-direction: column;
    text-align: center;
  }
  
  .feature-card {
    margin-bottom: 12px;
  }
  
  .recent-item {
    flex-wrap: wrap;
  }
  
  .recent-time {
    width: 100%;
    text-align: right;
    margin-top: 4px;
  }
}
</style>
