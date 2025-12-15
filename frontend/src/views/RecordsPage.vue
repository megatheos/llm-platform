<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRecordsStore } from '@/stores/records'
import type { ActivityType, LearningRecord } from '@/types'

const recordsStore = useRecordsStore()

// Filter options
const filterOptions: { value: ActivityType | ''; label: string }[] = [
  { value: '', label: 'All Activities' },
  { value: 'WORD_QUERY', label: 'Word Queries' },
  { value: 'DIALOGUE', label: 'Dialogues' },
  { value: 'QUIZ', label: 'Quizzes' }
]

const selectedFilter = ref<ActivityType | ''>('')

// Handle filter change
async function handleFilterChange(value: ActivityType | '') {
  selectedFilter.value = value
  await recordsStore.setFilter(value || undefined)
}

// Handle page change
async function handlePageChange(page: number) {
  await recordsStore.goToPage(page)
}

// Handle page size change
async function handleSizeChange(size: number) {
  await recordsStore.changePageSize(size)
}

// Format date for display
function formatDate(dateStr: string): string {
  const date = new Date(dateStr)
  return date.toLocaleDateString(undefined, {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  })
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
      return `Queried "${wordDetails.word}" (${wordDetails.sourceLang} → ${wordDetails.targetLang})`
    case 'DIALOGUE':
      const dialogueDetails = details as any
      return `${dialogueDetails.scenarioName} - ${dialogueDetails.messageCount} messages`
    case 'QUIZ':
      const quizDetails = details as any
      return `${quizDetails.difficulty} quiz - Score: ${quizDetails.score}/${quizDetails.totalScore}`
    default:
      return 'Activity completed'
  }
}


// Get score percentage for quiz
function getScorePercentage(score: number, total: number): number {
  if (!total) return 0
  return Math.round((score / total) * 100)
}

// Get score color based on percentage
function getScoreColor(percentage: number): string {
  if (percentage >= 80) return '#67c23a'
  if (percentage >= 60) return '#e6a23c'
  return '#f56c6c'
}

// Statistics computed values
const totalActivities = computed(() => recordsStore.statistics?.totalActivities || 0)
const averageScore = computed(() => {
  const score = recordsStore.statistics?.averageQuizScore
  return score !== undefined ? Math.round(score) : 0
})

onMounted(async () => {
  await Promise.all([
    recordsStore.fetchRecords(),
    recordsStore.fetchStatistics()
  ])
})
</script>

<template>
  <div class="records-page">
    <!-- Header Section -->
    <el-row :gutter="20">
      <el-col :span="24">
        <el-card class="header-card">
          <div class="page-header">
            <h1>Learning Records</h1>
            <p>Track your learning progress and history</p>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- Statistics Section -->
    <el-row :gutter="20" class="stats-section">
      <el-col :xs="12" :sm="6">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-content">
            <el-icon class="stat-icon word-icon" :size="32"><Search /></el-icon>
            <div class="stat-info">
              <div class="stat-value">{{ recordsStore.statistics?.totalWordQueries || 0 }}</div>
              <div class="stat-label">Words Queried</div>
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
              <div class="stat-label">Dialogues</div>
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
              <div class="stat-value">{{ averageScore }}%</div>
              <div class="stat-label">Avg Quiz Score</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>


    <!-- Activity Progress Section -->
    <el-row :gutter="20" class="progress-section">
      <el-col :span="24">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>Activity Overview</span>
              <span class="activity-period">
                Last 30 days: {{ recordsStore.statistics?.activitiesLast30Days || 0 }} activities
              </span>
            </div>
          </template>
          <el-row :gutter="40">
            <el-col :span="8">
              <div class="progress-item">
                <div class="progress-label">
                  <span>Word Queries</span>
                  <span class="progress-count">{{ recordsStore.statistics?.totalWordQueries || 0 }}</span>
                </div>
                <el-progress 
                  :percentage="totalActivities ? Math.round((recordsStore.statistics?.totalWordQueries || 0) / totalActivities * 100) : 0"
                  :stroke-width="12"
                  color="#409eff"
                />
              </div>
            </el-col>
            <el-col :span="8">
              <div class="progress-item">
                <div class="progress-label">
                  <span>Dialogues</span>
                  <span class="progress-count">{{ recordsStore.statistics?.totalDialogueSessions || 0 }}</span>
                </div>
                <el-progress 
                  :percentage="totalActivities ? Math.round((recordsStore.statistics?.totalDialogueSessions || 0) / totalActivities * 100) : 0"
                  :stroke-width="12"
                  color="#67c23a"
                />
              </div>
            </el-col>
            <el-col :span="8">
              <div class="progress-item">
                <div class="progress-label">
                  <span>Quizzes</span>
                  <span class="progress-count">{{ recordsStore.statistics?.totalQuizzes || 0 }}</span>
                </div>
                <el-progress 
                  :percentage="totalActivities ? Math.round((recordsStore.statistics?.totalQuizzes || 0) / totalActivities * 100) : 0"
                  :stroke-width="12"
                  color="#e6a23c"
                />
              </div>
            </el-col>
          </el-row>
        </el-card>
      </el-col>
    </el-row>

    <!-- Records List Section -->
    <el-row :gutter="20" class="records-section">
      <el-col :span="24">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>Learning History</span>
              <el-select 
                v-model="selectedFilter" 
                placeholder="Filter by type"
                style="width: 180px"
                @change="handleFilterChange"
              >
                <el-option
                  v-for="option in filterOptions"
                  :key="option.value"
                  :label="option.label"
                  :value="option.value"
                />
              </el-select>
            </div>
          </template>

          <!-- Loading State -->
          <div v-if="recordsStore.loading" class="loading-container">
            <el-skeleton :rows="5" animated />
          </div>

          <!-- Records List -->
          <div v-else-if="recordsStore.hasRecords" class="records-list">
            <div
              v-for="record in recordsStore.records"
              :key="record.id"
              class="record-item"
            >
              <div class="record-icon">
                <el-icon 
                  :size="24" 
                  :class="['activity-icon', `${record.activityType.toLowerCase()}-icon`]"
                >
                  <Search v-if="record.activityType === 'WORD_QUERY'" />
                  <ChatDotRound v-else-if="record.activityType === 'DIALOGUE'" />
                  <Document v-else />
                </el-icon>
              </div>
              <div class="record-content">
                <div class="record-header">
                  <el-tag 
                    :type="recordsStore.getActivityTypeColor(record.activityType)"
                    size="small"
                  >
                    {{ recordsStore.getActivityTypeLabel(record.activityType) }}
                  </el-tag>
                  <span class="record-time">{{ formatRelativeTime(record.activityTime) }}</span>
                </div>
                <div class="record-description">
                  {{ getActivityDescription(record) }}
                </div>
                <div class="record-date">
                  {{ formatDate(record.activityTime) }}
                </div>
              </div>
              <!-- Quiz Score Badge -->
              <div 
                v-if="record.activityType === 'QUIZ' && record.activityDetails" 
                class="record-score"
              >
                <el-progress
                  type="circle"
                  :percentage="getScorePercentage((record.activityDetails as any).score, (record.activityDetails as any).totalScore)"
                  :width="50"
                  :stroke-width="4"
                  :color="getScoreColor(getScorePercentage((record.activityDetails as any).score, (record.activityDetails as any).totalScore))"
                />
              </div>
            </div>
          </div>

          <!-- Empty State -->
          <el-empty
            v-else
            description="No learning records yet. Start learning to see your progress!"
            :image-size="120"
          />

          <!-- Pagination -->
          <div v-if="recordsStore.hasRecords" class="pagination-container">
            <el-pagination
              v-model:current-page="recordsStore.currentPage"
              v-model:page-size="recordsStore.pageSize"
              :page-sizes="[10, 20, 50, 100]"
              :total="recordsStore.total"
              layout="total, sizes, prev, pager, next, jumper"
              @current-change="handlePageChange"
              @size-change="handleSizeChange"
            />
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>


<script lang="ts">
import {
  Search,
  ChatDotRound,
  Document,
  Trophy
} from '@element-plus/icons-vue'

export default {
  components: { Search, ChatDotRound, Document, Trophy }
}
</script>

<style scoped>
.records-page {
  padding: 20px;
  min-height: calc(100vh - 100px);
}

/* Header Styles */
.header-card {
  margin-bottom: 20px;
}

.page-header {
  text-align: center;
}

.page-header h1 {
  margin: 0 0 8px 0;
  color: #303133;
}

.page-header p {
  margin: 0;
  color: #909399;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-weight: 600;
}

.activity-period {
  font-size: 14px;
  font-weight: normal;
  color: #909399;
}

/* Statistics Section */
.stats-section {
  margin-bottom: 20px;
}

.stat-card {
  height: 100%;
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

/* Progress Section */
.progress-section {
  margin-bottom: 20px;
}

.progress-item {
  padding: 8px 0;
}

.progress-label {
  display: flex;
  justify-content: space-between;
  margin-bottom: 8px;
  font-size: 14px;
  color: #606266;
}

.progress-count {
  font-weight: 600;
  color: #303133;
}

/* Records Section */
.records-section {
  margin-bottom: 20px;
}

.loading-container {
  padding: 20px;
}

.records-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.record-item {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  padding: 16px;
  background-color: #fafafa;
  border-radius: 8px;
  transition: all 0.3s;
}

.record-item:hover {
  background-color: #f5f7fa;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.record-icon {
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

.record-content {
  flex: 1;
  min-width: 0;
}

.record-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
}

.record-time {
  font-size: 12px;
  color: #909399;
}

.record-description {
  font-size: 14px;
  color: #303133;
  margin-bottom: 4px;
  word-break: break-word;
}

.record-date {
  font-size: 12px;
  color: #c0c4cc;
}

.record-score {
  flex-shrink: 0;
}

/* Pagination */
.pagination-container {
  display: flex;
  justify-content: center;
  padding-top: 20px;
  border-top: 1px solid #ebeef5;
  margin-top: 20px;
}

/* Responsive adjustments */
@media (max-width: 768px) {
  .stat-content {
    flex-direction: column;
    text-align: center;
  }
  
  .progress-section .el-col {
    margin-bottom: 16px;
  }
  
  .record-item {
    flex-direction: column;
  }
  
  .record-score {
    align-self: flex-end;
  }
}
</style>
