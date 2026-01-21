<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElProgress } from 'element-plus'
import {
  TrendCharts,
  DataLine,
  Calendar,
  List
} from '@element-plus/icons-vue'
import type { ProgressMetrics } from '@/types/personalized'

const { t } = useI18n()

// Props
interface Props {
  totalWords?: number
  totalQuizzes?: number
  averageScore?: number
  totalStudyTime?: number
  currentStreak?: number
  longestStreak?: number
  completionRate?: number
  compact?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  totalWords: 0,
  totalQuizzes: 0,
  averageScore: 0,
  totalStudyTime: 0,
  currentStreak: 0,
  longestStreak: 0,
  completionRate: 0,
  compact: false
})

// State
const loading = ref(false)
const selectedTimeRange = ref<'week' | 'month' | 'year'>('month')
const chartData = ref<{ labels: string[]; values: number[] }>({
  labels: ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'],
  values: [5, 8, 12, 6, 10, 15, 8]
})

// Computed
const chartHeight = computed(() => props.compact ? 120 : 200)

const chartBars = computed(() => {
  const maxValue = Math.max(...chartData.value.values, 1)
  return chartData.value.values.map((value, index) => ({
    label: chartData.value.labels[index],
    value,
    percentage: Math.min(100, (value / maxValue) * 100)
  }))
})

const statsCards = computed(() => [
  {
    key: 'words',
    icon: DataLine,
    label: t('profile.totalWords'),
    value: props.totalWords,
    color: '#3b82f6',
    bgColor: 'rgba(59, 130, 246, 0.1)'
  },
  {
    key: 'quizzes',
    icon: TrendCharts,
    label: t('profile.totalQuizzes'),
    value: props.totalQuizzes,
    color: '#8b5cf6',
    bgColor: 'rgba(139, 92, 246, 0.1)'
  },
  {
    key: 'score',
    icon: List,
    label: t('records.avgQuizScore'),
    value: `${props.averageScore}%`,
    color: '#10b981',
    bgColor: 'rgba(16, 185, 129, 0.1)'
  },
  {
    key: 'streak',
    icon: Calendar,
    label: t('motivation.learningStreak'),
    value: `${props.currentStreak} ${t('motivation.days')}`,
    color: '#f59e0b',
    bgColor: 'rgba(245, 158, 11, 0.1)'
  }
])

const completionRate = computed(() => props.completionRate)

// Methods
function generateChartData() {
  const labels: string[] = []
  const values: number[] = []
  
  const days = selectedTimeRange.value === 'week' ? 7 : selectedTimeRange.value === 'month' ? 30 : 365
  const today = new Date()
  
  for (let i = days - 1; i >= 0; i--) {
    const date = new Date(today)
    date.setDate(date.getDate() - i)
    
    if (selectedTimeRange.value === 'year') {
      if (i % 30 === 0) {
        labels.push(date.toLocaleDateString('en', { month: 'short' }))
      } else {
        labels.push('')
      }
    } else {
      labels.push(date.toLocaleDateString('en', { weekday: 'short' }))
    }
    
    values.push(Math.floor(Math.random() * 15) + 5)
  }
  
  chartData.value = { labels, values }
}

watch(selectedTimeRange, () => {
  generateChartData()
})

function getBarColor(value: number): string {
  if (value >= 12) return '#10b981'
  if (value >= 8) return '#3b82f6'
  if (value >= 4) return '#f59e0b'
  return '#6b7280'
}

// Initialize
onMounted(() => {
  generateChartData()
})
</script>

<template>
  <div class="progress-chart">
    <!-- Stats Cards -->
    <div class="stats-grid">
      <div
        v-for="stat in statsCards"
        :key="stat.key"
        class="stat-card"
        :style="{ '--stat-color': stat.color, '--stat-bg': stat.bgColor }"
      >
        <div class="stat-icon">
          <el-icon :size="20">
            <component :is="stat.icon" />
          </el-icon>
        </div>
        <div class="stat-content">
          <span class="stat-value">{{ stat.value }}</span>
          <span class="stat-label">{{ stat.label }}</span>
        </div>
      </div>
    </div>

    <!-- Chart -->
    <div class="chart-container">
      <div class="chart-header">
        <h4>Activity Overview</h4>
        <div class="time-range-selector">
          <button
            v-for="range in ['week', 'month', 'year']"
            :key="range"
            class="range-btn"
            :class="{ active: selectedTimeRange === range }"
            @click="selectedTimeRange = range as 'week' | 'month' | 'year'"
          >
            {{ range.charAt(0).toUpperCase() + range.slice(1) }}
          </button>
        </div>
      </div>

      <div class="chart-bars" :style="{ height: `${chartHeight}px` }">
        <div
          v-for="(bar, index) in chartBars"
          :key="index"
          class="bar-wrapper"
        >
          <div
            class="bar"
            :style="{
              height: `${bar.percentage}%`,
              backgroundColor: getBarColor(bar.value)
            }"
          />
          <span class="bar-label">{{ bar.label }}</span>
        </div>
      </div>
    </div>

    <!-- Completion Rate -->
    <div class="completion-section">
      <div class="completion-header">
        <span class="completion-label">Overall Progress</span>
        <span class="completion-value">{{ completionRate }}%</span>
      </div>
      <el-progress
        :percentage="completionRate"
        :stroke-width="10"
        :show-text="false"
        color="#3b82f6"
      />
    </div>
  </div>
</template>

<style scoped>
.progress-chart {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}

.stat-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px;
  background: var(--bg-secondary);
  border-radius: var(--radius-md);
  border: 1px solid var(--border-color-light);
}

.stat-icon {
  width: 36px;
  height: 36px;
  border-radius: var(--radius-sm);
  background: var(--stat-bg);
  color: var(--stat-color);
  display: flex;
  align-items: center;
  justify-content: center;
}

.stat-content {
  display: flex;
  flex-direction: column;
}

.stat-value {
  font-size: var(--font-size-lg);
  font-weight: 700;
  color: var(--text-primary);
}

.stat-label {
  font-size: var(--font-size-xs);
  color: var(--text-muted);
}

.chart-container {
  background: var(--bg-secondary);
  border-radius: var(--radius-lg);
  padding: 16px;
  border: 1px solid var(--border-color-light);
}

.chart-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.chart-header h4 {
  font-size: var(--font-size-sm);
  font-weight: 600;
  color: var(--text-primary);
}

.time-range-selector {
  display: flex;
  gap: 4px;
  background: var(--bg-tertiary);
  padding: 4px;
  border-radius: var(--radius-sm);
}

.range-btn {
  padding: 4px 12px;
  border: none;
  background: transparent;
  color: var(--text-muted);
  font-size: var(--font-size-xs);
  cursor: pointer;
  border-radius: var(--radius-sm);
  transition: all 0.2s;
}

.range-btn:hover {
  color: var(--text-primary);
}

.range-btn.active {
  background: var(--color-primary);
  color: white;
}

.chart-bars {
  display: flex;
  align-items: flex-end;
  gap: 8px;
  padding-top: 20px;
}

.bar-wrapper {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  height: 100%;
}

.bar {
  width: 100%;
  max-width: 30px;
  border-radius: 4px 4px 0 0;
  transition: height 0.3s ease;
  min-height: 4px;
}

.bar-label {
  font-size: 10px;
  color: var(--text-muted);
  margin-top: 8px;
  transform: rotate(-45deg);
  white-space: nowrap;
}

.completion-section {
  padding: 16px;
  background: var(--bg-secondary);
  border-radius: var(--radius-lg);
  border: 1px solid var(--border-color-light);
}

.completion-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.completion-label {
  font-size: var(--font-size-sm);
  color: var(--text-muted);
}

.completion-value {
  font-size: var(--font-size-sm);
  font-weight: 600;
  color: var(--color-primary);
}
</style>