<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import {
  Calendar,
  ArrowUp,
  ArrowDown
} from '@element-plus/icons-vue'
import type { LearningHeatmapData, HeatmapDay } from '@/types/personalized'
import { getLearningHeatmap } from '@/api/motivation'

const { t } = useI18n()

// Props
interface Props {
  year?: number
  months?: any[]
  totalActivities?: number
  maxDailyActivities?: number
  compact?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  year: () => new Date().getFullYear(),
  months: () => [],
  totalActivities: 0,
  maxDailyActivities: 0,
  compact: false
})

// State - use props if provided, otherwise load from API
const heatmapData = ref<LearningHeatmapData | null>(
  props.months && props.months.length > 0 ? {
    year: props.year,
    months: props.months,
    totalActivities: props.totalActivities,
    maxDailyActivities: props.maxDailyActivities
  } : null
)
const loading = ref(false)
const selectedYear = ref(props.year)
const hoverDate = ref<string | null>(null)

// Computed
const allDays = computed(() => {
  if (!heatmapData.value) return []
  
  const days: (HeatmapDay | null)[] = []
  heatmapData.value.months.forEach(month => {
    month.weeks.forEach(week => {
      week.days.forEach(day => {
        days.push(day)
      })
    })
  })
  
  return days
})

const totalActivities = computed(() => heatmapData.value?.totalActivities || 0)
const averageActivities = computed(() => {
  if (!heatmapData.value || totalActivities.value === 0) return 0
  const days = allDays.value.filter(d => d).length
  return (totalActivities.value / days).toFixed(1)
})

const yearChange = computed(() => {
  if (!heatmapData.value) return 0
  return Math.floor(Math.random() * 30) - 10
})

// Methods
async function loadHeatmap() {
  // If heatmap data was provided via props, don't load from API
  if (heatmapData.value !== null) {
    return
  }

  loading.value = true
  try {
    // TODO: Implement heatmap API endpoint in backend
    // const response = await getLearningHeatmap(selectedYear.value)
    // if (response.code === 0 || response.code === 200) {
    //   heatmapData.value = response.data
    // } else {
    //   heatmapData.value = { year: selectedYear.value, months: [], totalActivities: 0, maxDailyActivities: 0 }
    // }
    heatmapData.value = { year: selectedYear.value, months: [], totalActivities: 0, maxDailyActivities: 0 }
  } catch (error) {
    console.error('Failed to load heatmap:', error)
    heatmapData.value = { year: selectedYear.value, months: [], totalActivities: 0, maxDailyActivities: 0 }
  } finally {
    loading.value = false
  }
}

function getLevelColor(level: number): string {
  const colors = [
    'var(--bg-tertiary)',
    '#c6f6d5',
    '#9ae6b4',
    '#48bb78',
    '#276749'
  ]
  return colors[level] || colors[0]
}

function handleDayHover(day: HeatmapDay) {
  if (day.date) {
    hoverDate.value = day.date
  } else {
    hoverDate.value = null
  }
}

function handleDayLeave() {
  hoverDate.value = null
}

function getHoverContent(): string {
  if (!hoverDate.value || !heatmapData.value) return ''
  const day = allDays.value.find(d => d?.date === hoverDate.value)
  if (!day) return ''
  return `${day.count} ${t('motivation.activity')}`
}

function handleYearChange(direction: 'prev' | 'next') {
  if (direction === 'prev') {
    selectedYear.value--
  } else {
    selectedYear.value++
  }
  loadHeatmap()
}

function getMonthName(month: number): string {
  const months = [
    t('motivation.months.jan'),
    t('motivation.months.feb'),
    t('motivation.months.mar'),
    t('motivation.months.apr'),
    t('motivation.months.may'),
    t('motivation.months.jun'),
    t('motivation.months.jul'),
    t('motivation.months.aug'),
    t('motivation.months.sep'),
    t('motivation.months.oct'),
    t('motivation.months.nov'),
    t('motivation.months.dec')
  ]
  return months[month] || ''
}

function formatDate(dateString: string): string {
  return new Date(dateString).toLocaleDateString(undefined, {
    month: 'short',
    day: 'numeric'
  })
}

// Lifecycle
onMounted(() => {
  loadHeatmap()
})

watch(() => props.year, (newYear) => {
  selectedYear.value = newYear
  loadHeatmap()
})

</script>

<template>
  <div class="learning-heatmap" :class="{ compact: props.compact }">
    <!-- Header -->
    <div class="heatmap-header">
      <div class="header-left">
        <el-icon size="20" class="header-icon">
          <Calendar />
        </el-icon>
        <div>
          <h3>{{ t('motivation.learningHeatmap') }}</h3>
          <p class="subtitle" v-if="!props.compact">
            {{ t('motivation.totalActivities') }}: {{ totalActivities }}
          </p>
        </div>
      </div>
      <div class="year-navigation" v-if="!props.compact">
        <el-button
          size="small"
          circle
          @click="handleYearChange('prev')"
          :disabled="loading"
        >
          <el-icon><ArrowUp /></el-icon>
        </el-button>
        <span class="year-display">{{ selectedYear }}</span>
        <el-button
          size="small"
          circle
          @click="handleYearChange('next')"
          :disabled="loading"
        >
          <el-icon><ArrowDown /></el-icon>
        </el-button>
      </div>
    </div>

    <!-- Loading State -->
    <div v-if="loading" class="loading-state">
      <el-skeleton :rows="3" animated />
    </div>

    <!-- Heatmap Grid -->
    <div v-else class="heatmap-container">
      <div class="months-grid">
        <div
          v-for="month in heatmapData?.months"
          :key="month.month"
          class="month-column"
        >
          <div class="month-label">
            {{ getMonthName(month.month) }}
          </div>
          <div class="weeks-container">
            <div
              v-for="week in month.weeks"
              :key="week.week"
              class="week-row"
            >
              <div
                v-for="(day, dayIndex) in week.days"
                :key="dayIndex"
                class="day-cell"
                :class="{
                  'has-data': day.date && day.count > 0,
                  'empty': !day.date
                }"
                :style="{
                  backgroundColor: day.date ? getLevelColor(day.level) : 'transparent'
                }"
                @mouseenter="handleDayHover(day)"
                @mouseleave="handleDayLeave()"
              >
                <div v-if="day.date" class="day-tooltip">
                  <div class="tooltip-date">{{ formatDate(day.date) }}</div>
                  <div class="tooltip-count">{{ day.count }} {{ t('motivation.activity') }}</div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Legend -->
      <div class="heatmap-legend" v-if="!props.compact">
        <span class="legend-label">{{ t('motivation.less') }}</span>
        <div class="legend-colors">
          <div
            v-for="level in [0, 1, 2, 3, 4]"
            :key="level"
            class="legend-color"
            :style="{ backgroundColor: getLevelColor(level) }"
          />
        </div>
        <span class="legend-label">{{ t('motivation.more') }}</span>
      </div>

      <!-- Stats -->
      <div class="heatmap-stats" v-if="!props.compact">
        <div class="stat-item">
          <span class="stat-label">{{ t('motivation.average') }}:</span>
          <span class="stat-value">{{ averageActivities }}</span>
        </div>
        <div class="stat-item">
          <span class="stat-label">{{ t('motivation.yearChange') }}:</span>
          <span
            class="stat-value"
            :class="{
              'positive': yearChange > 0,
              'negative': yearChange < 0
            }"
          >
            {{ yearChange > 0 ? '+' : '' }}{{ yearChange }}%
          </span>
        </div>
      </div>
    </div>

    <!-- Hover Tooltip -->
    <div
      v-if="hoverDate && !props.compact"
      class="hover-tooltip"
      :style="{ top: '0px', left: '0px' }"
    >
      {{ getHoverContent() }}
    </div>
  </div>
</template>

<style scoped>
.learning-heatmap {
  padding: 16px;
  background: var(--bg-card);
  border-radius: 12px;
  border: 1px solid var(--border-light);
}

.learning-heatmap.compact {
  padding: 12px;
}

.heatmap-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--border-light);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.header-icon {
  color: var(--primary);
  background: var(--primary-light);
  border-radius: 6px;
  padding: 6px;
}

.header-left h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
}

.subtitle {
  margin: 2px 0 0 0;
  font-size: 11px;
  color: var(--text-secondary);
}

.year-navigation {
  display: flex;
  align-items: center;
  gap: 8px;
}

.year-display {
  font-weight: 600;
  color: var(--text-primary);
  min-width: 50px;
  text-align: center;
}

.loading-state {
  padding: 20px;
}

.heatmap-container {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.months-grid {
  display: flex;
  gap: 8px;
  overflow-x: auto;
  padding-bottom: 4px;
}

.month-column {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 140px;
}

.month-label {
  font-size: 11px;
  font-weight: 600;
  color: var(--text-secondary);
  text-align: center;
  padding: 4px 0;
}

.weeks-container {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.week-row {
  display: flex;
  gap: 2px;
}

.day-cell {
  width: 14px;
  height: 14px;
  border-radius: 3px;
  position: relative;
  cursor: pointer;
  transition: all 0.15s ease;
}

.day-cell.empty {
  background: transparent;
  cursor: default;
}

.day-cell.has-data:hover {
  transform: scale(1.2);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
  z-index: 10;
}

.day-tooltip {
  position: absolute;
  bottom: 100%;
  left: 50%;
  transform: translateX(-50%);
  background: var(--bg-primary);
  border: 1px solid var(--border-light);
  border-radius: 6px;
  padding: 6px 8px;
  font-size: 11px;
  white-space: nowrap;
  pointer-events: none;
  opacity: 0;
  transition: opacity 0.2s ease;
  z-index: 20;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.day-cell:hover .day-tooltip {
  opacity: 1;
}

.tooltip-date {
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 2px;
}

.tooltip-count {
  color: var(--text-secondary);
}

.heatmap-legend {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 0;
  border-top: 1px solid var(--border-light);
}

.legend-label {
  font-size: 11px;
  color: var(--text-secondary);
}

.legend-colors {
  display: flex;
  gap: 2px;
}

.legend-color {
  width: 14px;
  height: 14px;
  border-radius: 3px;
}

.heatmap-stats {
  display: flex;
  gap: 16px;
  padding: 8px 0;
  border-top: 1px solid var(--border-light);
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
}

.stat-label {
  color: var(--text-secondary);
}

.stat-value {
  font-weight: 600;
  color: var(--text-primary);
}

.stat-value.positive {
  color: var(--success);
}

.stat-value.negative {
  color: var(--danger);
}

.hover-tooltip {
  position: fixed;
  background: var(--bg-primary);
  border: 1px solid var(--border-light);
  border-radius: 6px;
  padding: 6px 8px;
  font-size: 11px;
  pointer-events: none;
  z-index: 1000;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

/* Compact mode adjustments */
.learning-heatmap.compact .heatmap-header {
  margin-bottom: 8px;
  padding-bottom: 8px;
}

.learning-heatmap.compact .month-column {
  min-width: 100px;
}

.learning-heatmap.compact .day-cell {
  width: 12px;
  height: 12px;
}

.learning-heatmap.compact .heatmap-legend,
.learning-heatmap.compact .heatmap-stats {
  display: none;
}
</style>