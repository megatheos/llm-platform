<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRecordsStore } from '@/stores/records'
import type { ActivityType, LearningRecord } from '@/types'
import {
  Search,
  ChatDotRound,
  Document,
  Trophy,
  Clock,
  Filter,
  Download,
  TrendCharts,
  Calendar,
  Star,
  Refresh
} from '@element-plus/icons-vue'

const { t } = useI18n()
const recordsStore = useRecordsStore()
const selectedFilter = ref<ActivityType | ''>('')
const searchQuery = ref('')
const expandedRecord = ref<number | null>(null)

// Filter options with icons
const filterOptions = computed(() => [
  { value: '', label: t('records.allActivities'), icon: '📋' },
  { value: 'WORD_QUERY', label: t('records.wordQueries'), icon: '📖' },
  { value: 'DIALOGUE', label: t('records.dialogues'), icon: '💬' },
  { value: 'QUIZ', label: t('quiz.title'), icon: '📝' }
])

// Animated stat values
const animatedStats = ref({
  wordQueries: 0,
  dialogues: 0,
  quizzes: 0,
  avgScore: 0
})

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

// Stats configuration
const statsConfig = computed(() => [
  { 
    key: 'wordQueries', 
    emoji: '📖', 
    label: 'Words Queried', 
    value: animatedStats.value.wordQueries,
    trend: 12,
    gradient: '#3b82f6',
    bgColor: '#eff6ff'
  },
  { 
    key: 'dialogues', 
    emoji: '💬', 
    label: 'Conversations', 
    value: animatedStats.value.dialogues,
    trend: 8,
    gradient: '#10b981',
    bgColor: '#ecfdf5'
  },
  { 
    key: 'quizzes', 
    emoji: '📝', 
    label: 'Quizzes Taken', 
    value: animatedStats.value.quizzes,
    trend: -3,
    gradient: '#f59e0b',
    bgColor: '#fffbeb'
  },
  { 
    key: 'avgScore', 
    emoji: '🏆', 
    label: 'Avg Score', 
    value: animatedStats.value.avgScore,
    trend: 5,
    gradient: '#6366f1',
    bgColor: '#eef2ff'
  }
])

// Activity config
const activityConfig = [
  { type: 'WORD_QUERY', label: 'Word Queries', icon: '📖', color: '#3b82f6' },
  { type: 'DIALOGUE', label: 'Dialogues', icon: '💬', color: '#10b981' },
  { type: 'QUIZ', label: 'Quizzes', icon: '📝', color: '#f59e0b' }
]

// Helper functions for template
function getStatValue(key: string): number {
  const stat = statsConfig.value.find(s => s.key === key)
  return stat?.value || 0
}

function getActivityCount(type: string): number {
  switch (type) {
    case 'WORD_QUERY': return recordsStore.statistics?.totalWordQueries || 0
    case 'DIALOGUE': return recordsStore.statistics?.totalDialogueSessions || 0
    case 'QUIZ': return recordsStore.statistics?.totalQuizzes || 0
    default: return 0
  }
}

function getActivityPercentage(type: string): number {
  const total = recordsStore.statistics?.totalActivities || 1
  const count = getActivityCount(type)
  return Math.round((count / total) * 100)
}

async function handleFilterChange(value: ActivityType | '') {
  selectedFilter.value = value
  await recordsStore.setFilter(value || undefined)
}

async function handlePageChange(page: number) { await recordsStore.goToPage(page) }
async function handleSizeChange(size: number) { await recordsStore.changePageSize(size) }

function formatDate(dateStr: string): string {
  return new Date(dateStr).toLocaleDateString(undefined, { 
    year: 'numeric', 
    month: 'short', 
    day: 'numeric', 
    hour: '2-digit', 
    minute: '2-digit' 
  })
}

function formatRelativeTime(dateStr: string): string {
  const diffDays = Math.floor((Date.now() - new Date(dateStr).getTime()) / (1000 * 60 * 60 * 24))
  if (diffDays === 0) return t('dashboard.today')
  if (diffDays === 1) return t('dashboard.yesterday')
  if (diffDays < 7) return t('dashboard.daysAgo', { n: diffDays })
  if (diffDays < 30) return t('dashboard.weeksAgo', { n: Math.floor(diffDays / 7) })
  return t('dashboard.monthsAgo', { n: Math.floor(diffDays / 30) })
}

function getActivityDescription(record: LearningRecord): string {
  const d = record.activityDetails as any
  if (!d) return ''
  switch (record.activityType) {
    case 'WORD_QUERY': return `${t('records.queried')} "${d.word}" (${d.sourceLang} → ${d.targetLang})`
    case 'DIALOGUE': return `${d.scenarioName || ''}`
    case 'QUIZ': return `${getDifficultyLabel(d.difficulty)} - ${t('records.score')}: ${d.userScore ?? 0}/${d.totalScore}`
    default: return ''
  }
}

function getDifficultyLabel(difficulty: string): string {
  if (!difficulty) return ''
  const key = difficulty.toLowerCase()
  if (key === 'easy') return t('quiz.easy')
  if (key === 'medium') return t('quiz.medium')
  if (key === 'hard') return t('quiz.hard')
  return difficulty
}

function getScorePercentage(score: number, total: number): number { 
  return total ? Math.round((score / total) * 100) : 0 
}

function getScoreColor(p: number): string { 
  return p >= 80 ? '#67c23a' : p >= 60 ? '#e6a23c' : '#f56c6c' 
}

function getActivityIcon(type: string): string {
  switch (type) {
    case 'WORD_QUERY': return '📖'
    case 'DIALOGUE': return '💬'
    case 'QUIZ': return '📝'
    default: return '📋'
  }
}

function getActivityGradient(type: string): string {
  switch (type) {
    case 'WORD_QUERY': return '#3b82f6'
    case 'DIALOGUE': return '#10b981'
    case 'QUIZ': return '#f59e0b'
    default: return '#6366f1'
  }
}

function toggleExpand(recordId: number) {
  expandedRecord.value = expandedRecord.value === recordId ? null : recordId
}

function filteredRecords() {
  if (!searchQuery.value) return recordsStore.records
  const query = searchQuery.value.toLowerCase()
  return recordsStore.records.filter(record => {
    const desc = getActivityDescription(record).toLowerCase()
    return desc.includes(query)
  })
}

const totalActivities = computed(() => recordsStore.statistics?.totalActivities || 0)

onMounted(async () => {
  await Promise.all([recordsStore.fetchRecords(), recordsStore.fetchStatistics()])
  
  setTimeout(() => {
    animateValue(animatedStats.value, 'wordQueries', recordsStore.statistics?.totalWordQueries || 0, 1000)
    animateValue(animatedStats.value, 'dialogues', recordsStore.statistics?.totalDialogueSessions || 0, 1200)
    animateValue(animatedStats.value, 'quizzes', recordsStore.statistics?.totalQuizzes || 0, 1400)
    animateValue(animatedStats.value, 'avgScore', Math.round(recordsStore.statistics?.averageQuizScore || 0), 1600)
  }, 300)
})
</script>

<template>
  <div class="records-page">
    <el-card class="header-card" shadow="hover">
      <div class="page-header">
        <div class="header-content">
          <el-icon class="header-icon"><Calendar /></el-icon>
          <div>
            <h1>{{ t('records.title') }}</h1>
            <p>{{ t('records.subtitle') }}</p>
          </div>
        </div>
        <div class="header-actions">
          <el-button size="small">
            <el-icon><Download /></el-icon>
            Export
          </el-button>
        </div>
      </div>
    </el-card>

    <!-- Stats Grid -->
    <el-row :gutter="20" class="stats-section">
      <el-col :xs="12" :sm="6" v-for="(stat, index) in statsConfig" :key="stat.key">
        <el-card 
          class="stat-card"
          shadow="hover"
          :style="{ '--delay': `${index * 0.1}s` }"
        >
          <div class="stat-glow" :style="{ background: stat.gradient }"></div>
          <div class="stat-icon" :style="{ background: stat.bgColor }">
            <span class="stat-emoji">{{ stat.emoji }}</span>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ getStatValue(stat.key) }}</div>
            <div class="stat-label">{{ stat.label }}</div>
          </div>
          <div class="stat-trend" :class="{ up: stat.trend > 0 }">
            <el-icon v-if="stat.trend > 0"><TrendCharts /></el-icon>
            <span>{{ Math.abs(stat.trend) }}%</span>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- Activity Overview -->
    <el-card class="progress-card" shadow="hover">
      <template #header>
        <div class="card-header">
          <div class="header-left">
            <el-icon><TrendCharts /></el-icon>
            <span>{{ t('records.activityOverview') }}</span>
          </div>
          <span class="period">{{ t('records.last30Days', { n: recordsStore.statistics?.activitiesLast30Days || 0 }) }}</span>
        </div>
      </template>
      
      <el-row :gutter="40" class="progress-grid">
        <el-col :span="8" v-for="(item, index) in activityConfig" :key="item.type">
          <div class="progress-item">
            <div class="progress-header">
              <span class="progress-icon">{{ item.icon }}</span>
              <span class="progress-label">{{ item.label }}</span>
              <span class="progress-value">{{ getActivityCount(item.type) }}</span>
            </div>
            <el-progress 
              :percentage="getActivityPercentage(item.type)" 
              :stroke-width="10"
              :color="item.color"
              :show-text="false"
            />
            <div class="progress-bar-bg">
              <div 
                class="progress-bar-fill"
                :style="{ 
                  width: getActivityPercentage(item.type) + '%',
                  background: item.color
                }"
              ></div>
            </div>
          </div>
        </el-col>
      </el-row>
    </el-card>

    <!-- Records List -->
    <el-card class="records-card" shadow="hover">
      <template #header>
        <div class="card-header">
          <div class="header-left">
            <el-icon><Clock /></el-icon>
            <span>{{ t('records.learningHistory') }}</span>
          </div>
          <div class="header-right">
            <el-input
              v-model="searchQuery"
              placeholder="Search records..."
              size="small"
              clearable
              class="search-input"
            >
              <template #prefix>
                <el-icon><Search /></el-icon>
              </template>
            </el-input>
            <el-select 
              v-model="selectedFilter" 
              :placeholder="t('records.filterByType')"
              size="small"
              style="width: 160px"
              @change="handleFilterChange"
            >
              <el-option
                v-for="opt in filterOptions"
                :key="opt.value"
                :label="opt.label"
                :value="opt.value"
              >
                <span>{{ opt.icon }} {{ opt.label }}</span>
              </el-option>
            </el-select>
          </div>
        </div>
      </template>
      
      <div v-if="recordsStore.loading" class="loading">
        <el-skeleton v-for="i in 5" :key="i" :rows="2" animated />
      </div>
      
      <div v-else-if="recordsStore.hasRecords" class="records-timeline">
        <transition-group name="record-list" tag="div">
          <div
            v-for="(record, index) in filteredRecords()"
            :key="record.id"
            class="record-item"
            :class="{ expanded: expandedRecord === record.id }"
            :style="{ '--delay': `${index * 0.05}s` }"
          >
            <div class="record-left">
              <div class="record-icon" :style="{ background: getActivityGradient(record.activityType) }">
                <span>{{ getActivityIcon(record.activityType) }}</span>
              </div>
              <div class="record-line"></div>
            </div>
            
            <div class="record-content" @click="toggleExpand(record.id)">
              <div class="record-header">
                <el-tag 
                  :type="recordsStore.getActivityTypeColor(record.activityType)" 
                  size="small"
                  effect="light"
                  round
                >
                  {{ recordsStore.getActivityTypeLabel(record.activityType) }}
                </el-tag>
                <span class="record-time">{{ formatRelativeTime(record.activityTime) }}</span>
              </div>
              
              <div class="record-main">
                <span class="record-desc">{{ getActivityDescription(record) }}</span>
              </div>
              
              <div class="record-meta">
                <el-icon><Calendar /></el-icon>
                <span>{{ formatDate(record.activityTime) }}</span>
              </div>
              
              <transition name="expand">
                <div v-if="expandedRecord === record.id" class="record-details">
                  <div class="detail-row">
                    <span class="detail-label">Activity Type</span>
                    <span class="detail-value">{{ record.activityType }}</span>
                  </div>
                  <div v-if="record.activityDetails" class="detail-row">
                    <span class="detail-label">Details</span>
                    <pre class="detail-json">{{ JSON.stringify(record.activityDetails, null, 2) }}</pre>
                  </div>
                </div>
              </transition>
            </div>
            
            <div class="record-score" v-if="record.activityType === 'QUIZ' && record.activityDetails">
              <div class="score-ring">
                <svg viewBox="0 0 36 36">
                  <path
                    class="score-bg"
                    d="M18 2.0845 a 15.9155 15.9155 0 0 1 0 31.831 a 15.9155 15.9155 0 0 1 0 -31.831"
                  />
                  <path
                    class="score-progress"
                    :stroke="getScoreColor(getScorePercentage((record.activityDetails as any).userScore ?? 0, (record.activityDetails as any).totalScore))"
                    stroke-dasharray="100, 100"
                    :stroke-dashoffset="100 - getScorePercentage((record.activityDetails as any).userScore ?? 0, (record.activityDetails as any).totalScore)"
                    d="M18 2.0845 a 15.9155 15.9155 0 0 1 0 31.831 a 15.9155 15.9155 0 0 1 0 -31.831"
                  />
                </svg>
                <span class="score-percent">
                  {{ getScorePercentage((record.activityDetails as any).userScore ?? 0, (record.activityDetails as any).totalScore) }}%
                </span>
              </div>
            </div>
            
            <div class="record-actions">
              <el-button size="small" circle>
                <el-icon><Refresh /></el-icon>
              </el-button>
            </div>
          </div>
        </transition-group>
      </div>
      
      <el-empty 
        v-else 
        :description="t('records.noRecords')" 
        :image-size="120" 
        class="empty-state"
      />
      
      <div v-if="recordsStore.hasRecords" class="pagination">
        <el-pagination 
          v-model:current-page="recordsStore.currentPage" 
          v-model:page-size="recordsStore.pageSize" 
          :page-sizes="[10, 20, 50]" 
          :total="recordsStore.total" 
          layout="total, sizes, prev, pager, next" 
          @current-change="handlePageChange" 
          @size-change="handleSizeChange" 
        />
      </div>
    </el-card>
  </div>
</template>

<style scoped>
.records-page {
  padding: 0;
}

.header-card {
  margin-bottom: 24px;
  background: var(--bg-secondary);
  border: 1px solid var(--border-color-light);
}

.header-card :deep(.el-card__body) {
  padding: 24px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-content {
  display: flex;
  align-items: center;
  gap: 16px;
}

.header-icon {
  font-size: 40px;
  color: var(--color-primary);
}

.header-content h1 {
  margin: 0 0 4px 0;
  font-size: var(--font-size-2xl);
  font-weight: 700;
  color: var(--text-primary);
}

.header-content p {
  margin: 0;
  font-size: var(--font-size-sm);
  color: var(--text-muted);
}

/* Stats Section */
.stats-section {
  margin-bottom: 24px;
}

.stat-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px;
  position: relative;
  overflow: hidden;
  border: 1px solid var(--border-color-light);
  animation: slideUp 0.6s ease-out backwards;
  animation-delay: var(--delay);
  transition: all 0.3s ease;
}

.stat-card:hover {
  transform: translateY(-4px);
  box-shadow: var(--shadow-md);
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

.stat-glow {
  position: absolute;
  top: -50%;
  right: -50%;
  width: 100px;
  height: 100px;
  border-radius: 50%;
  opacity: 0.1;
  filter: blur(30px);
}

.stat-icon {
  width: 56px;
  height: 56px;
  border-radius: var(--radius-lg);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  flex-shrink: 0;
  position: relative;
  z-index: 1;
}

.stat-info {
  flex: 1;
}

.stat-value {
  font-size: var(--font-size-2xl);
  font-weight: 700;
  color: var(--text-primary);
  line-height: 1.2;
}

.stat-label {
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
}

.stat-trend.up {
  background: #ecfdf5;
  color: #10b981;
}

/* Progress Card */
.progress-card {
  margin-bottom: 24px;
  border: 1px solid var(--border-color-light);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
}

.period {
  font-size: var(--font-size-sm);
  font-weight: normal;
  color: var(--text-muted);
}

.progress-grid {
  padding: 16px 0;
}

.progress-item {
  padding: 8px 0;
}

.progress-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}

.progress-icon {
  font-size: 20px;
}

.progress-label {
  flex: 1;
  font-size: var(--font-size-sm);
  color: var(--text-secondary);
}

.progress-value {
  font-size: var(--font-size-lg);
  font-weight: 700;
  color: var(--text-primary);
}

.progress-bar-bg {
  height: 8px;
  background: var(--bg-tertiary);
  border-radius: 4px;
  overflow: hidden;
}

.progress-bar-fill {
  height: 100%;
  border-radius: 4px;
  transition: width 1s ease-out;
}

/* Records Card */
.records-card {
  border: 1px solid var(--border-color-light);
}

.records-card .card-header {
  flex-wrap: wrap;
  gap: 12px;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.search-input {
  width: 200px;
}

.loading {
  padding: 20px;
}

.records-timeline {
  display: flex;
  flex-direction: column;
  gap: 0;
}

.record-list-enter-active,
.record-list-leave-active {
  transition: all 0.4s ease;
}

.record-list-enter-from,
.record-list-leave-to {
  opacity: 0;
  transform: translateX(-20px);
}

.record-item {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  padding: 20px;
  background: var(--bg-tertiary);
  border-radius: var(--radius-lg);
  margin-bottom: 12px;
  cursor: pointer;
  transition: all 0.3s ease;
  border: 1px solid transparent;
  animation: slideIn 0.5s ease-out backwards;
  animation-delay: var(--delay);
}

@keyframes slideIn {
  from {
    opacity: 0;
    transform: translateX(-20px);
  }
  to {
    opacity: 1;
    transform: translateX(0);
  }
}

.record-item:hover {
  background: var(--bg-secondary);
  border-color: var(--color-accent);
  transform: translateX(4px);
}

.record-item.expanded {
  background: var(--bg-secondary);
}

.record-left {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.record-icon {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  color: white;
  flex-shrink: 0;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.record-line {
  flex: 1;
  width: 2px;
  background: var(--border-color-light);
  min-height: 40px;
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
  font-size: var(--font-size-xs);
  color: var(--text-muted);
}

.record-main {
  margin-bottom: 8px;
}

.record-desc {
  font-size: var(--font-size-md);
  color: var(--text-primary);
  line-height: 1.5;
}

.record-meta {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: var(--font-size-xs);
  color: var(--text-muted);
}

.record-details {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid var(--border-color-light);
}

.detail-row {
  display: flex;
  gap: 16px;
  margin-bottom: 8px;
}

.detail-label {
  font-size: var(--font-size-sm);
  color: var(--text-muted);
  min-width: 100px;
}

.detail-value {
  font-size: var(--font-size-sm);
  color: var(--text-primary);
}

.detail-json {
  font-size: var(--font-size-xs);
  color: var(--text-muted);
  background: var(--bg-primary);
  padding: 8px;
  border-radius: 4px;
  overflow-x: auto;
}

.expand-enter-active,
.expand-leave-active {
  transition: all 0.3s ease;
}

.expand-enter-from,
.expand-leave-to {
  opacity: 0;
  transform: translateY(-10px);
}

.record-score {
  flex-shrink: 0;
}

.score-ring {
  position: relative;
  width: 60px;
  height: 60px;
}

.score-ring svg {
  transform: rotate(-90deg);
}

.score-bg {
  fill: none;
  stroke: var(--bg-secondary);
  stroke-width: 3;
}

.score-progress {
  fill: none;
  stroke-width: 3;
  stroke-linecap: round;
  transition: stroke-dashoffset 1s ease-out;
}

.score-percent {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  font-size: var(--font-size-xs);
  font-weight: 700;
  color: var(--text-primary);
}

.record-actions {
  flex-shrink: 0;
  opacity: 0;
  transition: opacity 0.3s;
}

.record-item:hover .record-actions {
  opacity: 1;
}

.empty-state {
  padding: 40px 0;
}

.pagination {
  display: flex;
  justify-content: center;
  padding-top: 20px;
  border-top: 1px solid var(--border-color-light);
  margin-top: 20px;
}

/* Responsive */
@media (max-width: 768px) {
  .records-page {
    padding: 0;
  }
  
  .page-header {
    flex-direction: column;
    gap: 16px;
    text-align: center;
  }
  
  .stat-card {
    flex-direction: column;
    text-align: center;
  }
  
  .header-right {
    flex-direction: column;
    width: 100%;
  }
  
  .search-input {
    width: 100%;
  }
  
  .record-item {
    flex-direction: column;
    align-items: stretch;
  }
  
  .record-left {
    flex-direction: row;
    justify-content: flex-start;
  }
  
  .record-line {
    display: none;
  }
  
  .record-score {
    position: absolute;
    top: 20px;
    right: 20px;
  }
  
  .record-actions {
    position: absolute;
    top: 20px;
    right: 80px;
    opacity: 1;
  }
}
</style>
