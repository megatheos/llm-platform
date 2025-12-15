<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRecordsStore } from '@/stores/records'
import type { ActivityType, LearningRecord } from '@/types'

const { t } = useI18n()
const recordsStore = useRecordsStore()
const selectedFilter = ref<ActivityType | ''>('')

const filterOptions = computed(() => [
  { value: '', label: t('records.allActivities') },
  { value: 'WORD_QUERY', label: t('records.wordQueries') },
  { value: 'DIALOGUE', label: t('records.dialogues') },
  { value: 'QUIZ', label: t('quiz.title') }
])

async function handleFilterChange(value: ActivityType | '') {
  selectedFilter.value = value
  await recordsStore.setFilter(value || undefined)
}

async function handlePageChange(page: number) { await recordsStore.goToPage(page) }
async function handleSizeChange(size: number) { await recordsStore.changePageSize(size) }

function formatDate(dateStr: string): string {
  return new Date(dateStr).toLocaleDateString(undefined, { year: 'numeric', month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' })
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

function getScorePercentage(score: number, total: number): number { return total ? Math.round((score / total) * 100) : 0 }
function getScoreColor(p: number): string { return p >= 80 ? '#67c23a' : p >= 60 ? '#e6a23c' : '#f56c6c' }

const totalActivities = computed(() => recordsStore.statistics?.totalActivities || 0)
const averageScore = computed(() => Math.round(recordsStore.statistics?.averageQuizScore || 0))

onMounted(async () => { await Promise.all([recordsStore.fetchRecords(), recordsStore.fetchStatistics()]) })
</script>

<template>
  <div class="records-page">
    <el-card class="header-card">
      <div class="page-header"><h1>{{ t('records.title') }}</h1><p>{{ t('records.subtitle') }}</p></div>
    </el-card>

    <el-row :gutter="20" class="stats-section">
      <el-col :xs="12" :sm="6">
        <el-card shadow="hover"><div class="stat-content"><el-icon class="stat-icon word" :size="32"><Search /></el-icon><div class="stat-info"><div class="stat-value">{{ recordsStore.statistics?.totalWordQueries || 0 }}</div><div class="stat-label">{{ t('records.wordsQueried') }}</div></div></div></el-card>
      </el-col>
      <el-col :xs="12" :sm="6">
        <el-card shadow="hover"><div class="stat-content"><el-icon class="stat-icon dialogue" :size="32"><ChatDotRound /></el-icon><div class="stat-info"><div class="stat-value">{{ recordsStore.statistics?.totalDialogueSessions || 0 }}</div><div class="stat-label">{{ t('records.dialogues') }}</div></div></div></el-card>
      </el-col>
      <el-col :xs="12" :sm="6">
        <el-card shadow="hover"><div class="stat-content"><el-icon class="stat-icon quiz" :size="32"><Document /></el-icon><div class="stat-info"><div class="stat-value">{{ recordsStore.statistics?.totalQuizzes || 0 }}</div><div class="stat-label">{{ t('records.quizzesTaken') }}</div></div></div></el-card>
      </el-col>
      <el-col :xs="12" :sm="6">
        <el-card shadow="hover"><div class="stat-content"><el-icon class="stat-icon score" :size="32"><Trophy /></el-icon><div class="stat-info"><div class="stat-value">{{ averageScore }}%</div><div class="stat-label">{{ t('records.avgQuizScore') }}</div></div></div></el-card>
      </el-col>
    </el-row>

    <el-card class="progress-card">
      <template #header><div class="card-header"><span>{{ t('records.activityOverview') }}</span><span class="period">{{ t('records.last30Days', { n: recordsStore.statistics?.activitiesLast30Days || 0 }) }}</span></div></template>
      <el-row :gutter="40">
        <el-col :span="8"><div class="progress-item"><div class="progress-label"><span>{{ t('records.wordQueries') }}</span><span>{{ recordsStore.statistics?.totalWordQueries || 0 }}</span></div><el-progress :percentage="totalActivities ? Math.round((recordsStore.statistics?.totalWordQueries || 0) / totalActivities * 100) : 0" color="#409eff" /></div></el-col>
        <el-col :span="8"><div class="progress-item"><div class="progress-label"><span>{{ t('records.dialogues') }}</span><span>{{ recordsStore.statistics?.totalDialogueSessions || 0 }}</span></div><el-progress :percentage="totalActivities ? Math.round((recordsStore.statistics?.totalDialogueSessions || 0) / totalActivities * 100) : 0" color="#67c23a" /></div></el-col>
        <el-col :span="8"><div class="progress-item"><div class="progress-label"><span>{{ t('quiz.title') }}</span><span>{{ recordsStore.statistics?.totalQuizzes || 0 }}</span></div><el-progress :percentage="totalActivities ? Math.round((recordsStore.statistics?.totalQuizzes || 0) / totalActivities * 100) : 0" color="#e6a23c" /></div></el-col>
      </el-row>
    </el-card>

    <el-card>
      <template #header><div class="card-header"><span>{{ t('records.learningHistory') }}</span><el-select v-model="selectedFilter" :placeholder="t('records.filterByType')" style="width: 180px" @change="handleFilterChange"><el-option v-for="opt in filterOptions" :key="opt.value" :label="opt.label" :value="opt.value" /></el-select></div></template>
      <div v-if="recordsStore.loading" class="loading"><el-skeleton :rows="5" animated /></div>
      <div v-else-if="recordsStore.hasRecords" class="records-list">
        <div v-for="record in recordsStore.records" :key="record.id" class="record-item">
          <div class="record-icon"><el-icon :size="24"><Search v-if="record.activityType === 'WORD_QUERY'" /><ChatDotRound v-else-if="record.activityType === 'DIALOGUE'" /><Document v-else /></el-icon></div>
          <div class="record-content">
            <div class="record-header"><el-tag :type="recordsStore.getActivityTypeColor(record.activityType)" size="small">{{ recordsStore.getActivityTypeLabel(record.activityType) }}</el-tag><span class="record-time">{{ formatRelativeTime(record.activityTime) }}</span></div>
            <div class="record-desc">{{ getActivityDescription(record) }}</div>
            <div class="record-date">{{ formatDate(record.activityTime) }}</div>
          </div>
          <div v-if="record.activityType === 'QUIZ' && record.activityDetails" class="record-score">
            <el-progress type="circle" :percentage="getScorePercentage((record.activityDetails as any).userScore ?? 0, (record.activityDetails as any).totalScore)" :width="50" :stroke-width="4" :color="getScoreColor(getScorePercentage((record.activityDetails as any).userScore ?? 0, (record.activityDetails as any).totalScore))" />
          </div>
        </div>
      </div>
      <el-empty v-else :description="t('records.noRecords')" :image-size="120" />
      <div v-if="recordsStore.hasRecords" class="pagination">
        <el-pagination v-model:current-page="recordsStore.currentPage" v-model:page-size="recordsStore.pageSize" :page-sizes="[10, 20, 50]" :total="recordsStore.total" layout="total, sizes, prev, pager, next" @current-change="handlePageChange" @size-change="handleSizeChange" />
      </div>
    </el-card>
  </div>
</template>

<script lang="ts">
import { Search, ChatDotRound, Document, Trophy } from '@element-plus/icons-vue'
export default { components: { Search, ChatDotRound, Document, Trophy } }
</script>

<style scoped>
.records-page { padding: 0; }
.header-card { margin-bottom: 20px; }
.page-header { text-align: center; }
.page-header h1 { margin: 0 0 8px; color: var(--text-primary); }
.page-header p { margin: 0; color: var(--text-secondary); }
.card-header { display: flex; justify-content: space-between; align-items: center; font-weight: 600; }
.period { font-size: 14px; font-weight: normal; color: var(--text-muted); }
.stats-section { margin-bottom: 20px; }
.stat-content { display: flex; align-items: center; gap: 16px; padding: 8px 0; }
.stat-icon { padding: 12px; border-radius: 12px; }
.stat-icon.word { background: #ecf5ff; color: #409eff; }
.stat-icon.dialogue { background: #f0f9eb; color: #67c23a; }
.stat-icon.quiz { background: #fdf6ec; color: #e6a23c; }
.stat-icon.score { background: #fef0f0; color: #f56c6c; }
.stat-info { flex: 1; }
.stat-value { font-size: 28px; font-weight: 700; color: var(--text-primary); }
.stat-label { font-size: 14px; color: var(--text-muted); }
.progress-card { margin-bottom: 20px; }
.progress-item { padding: 8px 0; }
.progress-label { display: flex; justify-content: space-between; margin-bottom: 8px; font-size: 14px; }
.loading { padding: 20px; }
.records-list { display: flex; flex-direction: column; gap: 12px; }
.record-item { display: flex; align-items: flex-start; gap: 16px; padding: 16px; background: var(--bg-tertiary); border-radius: 8px; }
.record-item:hover { background: var(--border-color-light); }
.record-icon { padding: 8px; border-radius: 8px; background: var(--bg-secondary); color: var(--text-secondary); }
.record-content { flex: 1; min-width: 0; }
.record-header { display: flex; align-items: center; gap: 12px; margin-bottom: 8px; }
.record-time { font-size: 12px; color: var(--text-muted); }
.record-desc { font-size: 14px; color: var(--text-primary); margin-bottom: 4px; }
.record-date { font-size: 12px; color: var(--text-light); }
.record-score { flex-shrink: 0; }
.pagination { display: flex; justify-content: center; padding-top: 20px; border-top: 1px solid var(--border-color-light); margin-top: 20px; }
</style>
