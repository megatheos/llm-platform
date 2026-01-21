<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElProgress } from 'element-plus'
import {
  Clock,
  Check,
  TrendCharts,
  DataLine,
  CircleCheck,
  Sunny
} from '@element-plus/icons-vue'
import type { ReviewStatistics, MemoryState } from '@/types/personalized'
import { getMemoryStatistics, getMemoryState } from '@/api/memory'

const { t } = useI18n()

// Props
interface Props {
  totalItems?: number
  dueToday?: number
  masteredCount?: number
  learningCount?: number
  newCount?: number
  averageMastery?: number
  reviewAccuracy?: number
  streakDays?: number
}

const props = withDefaults(defineProps<Props>(), {
  totalItems: 0,
  dueToday: 0,
  masteredCount: 0,
  learningCount: 0,
  newCount: 0,
  averageMastery: 0,
  reviewAccuracy: 0,
  streakDays: 0
})

// State
const statistics = ref<ReviewStatistics | null>(null)
const memoryState = ref<MemoryState | null>(null)
const loading = ref(false)
const timeRange = ref<'today' | 'week' | 'month'>('week')

// Computed
const statsCards = computed(() => [
  {
    key: 'totalItems',
    icon: DataLine,
    label: t('memory.totalItems'),
    value: props.totalItems,
    color: '#3b82f6',
    bgColor: 'rgba(59, 130, 246, 0.1)'
  },
  {
    key: 'dueToday',
    icon: Clock,
    label: t('memory.dueToday'),
    value: props.dueToday,
    color: '#f59e0b',
    bgColor: 'rgba(245, 158, 11, 0.1)'
  },
  {
    key: 'mastered',
    icon: CircleCheck,
    label: t('memory.mastered'),
    value: props.masteredCount,
    color: '#10b981',
    bgColor: 'rgba(16, 185, 129, 0.1)'
  },
  {
    key: 'streak',
    icon: Sunny,
    label: t('memory.streak'),
    value: `${props.streakDays} ${t('memory.days')}`,
    color: '#ef4444',
    bgColor: 'rgba(239, 68, 68, 0.1)'
  }
])

const accuracyProgress = computed(() => props.reviewAccuracy || 0)

const masteryProgress = computed(() => Math.round((props.averageMastery || 0) * 20))

// Methods
async function loadStatistics() {
  loading.value = true
  try {
    const [statsResponse, stateResponse] = await Promise.all([
      getMemoryStatistics(),
      getMemoryState()
    ])
    
    if (statsResponse.code === 0 || statsResponse.code === 200) {
      statistics.value = statsResponse.data
    }
    
    if (stateResponse.code === 0 || stateResponse.code === 200) {
      memoryState.value = stateResponse.data
    }
  } catch (error) {
    console.error('Failed to load memory statistics:', error)
  } finally {
    loading.value = false
  }
}

function getTimeBasedData(key: keyof MemoryState): number {
  if (!memoryState.value) return 0
  
  switch (timeRange.value) {
    case 'today': return memoryState.value.reviewsToday
    case 'week': return memoryState.value.reviewsThisWeek
    case 'month': return memoryState.value.reviewsThisMonth
    default: return 0
  }
}

// Lifecycle
onMounted(() => {
  loadStatistics()
})
</script>

<template>
  <div class="memory-statistics">
    <!-- Stats Cards -->
    <div class="stats-grid">
      <div
        v-for="stat in statsCards"
        :key="stat.key"
        class="stat-card"
        :style="{ '--stat-color': stat.color, '--stat-bg': stat.bgColor }"
      >
        <div class="stat-icon">
          <el-icon :size="24">
            <component :is="stat.icon" />
          </el-icon>
        </div>
        <div class="stat-content">
          <span class="stat-value">{{ stat.value }}</span>
          <span class="stat-label">{{ stat.label }}</span>
        </div>
      </div>
    </div>

    <!-- Progress Section -->
    <div class="progress-section">
      <div class="progress-item">
        <div class="progress-header">
          <span class="progress-label">{{ t('memory.accuracy') }}</span>
          <span class="progress-value">{{ accuracyProgress }}%</span>
        </div>
        <el-progress
          :percentage="accuracyProgress"
          :stroke-width="10"
          :show-text="false"
          color="#10b981"
        />
      </div>

      <div class="progress-item">
        <div class="progress-header">
          <span class="progress-label">{{ t('memory.averageMastery') }}</span>
          <span class="progress-value">{{ masteryProgress }}%</span>
        </div>
        <el-progress
          :percentage="masteryProgress"
          :stroke-width="10"
          :show-text="false"
          color="#3b82f6"
        />
      </div>
    </div>

    <!-- Learning Stats -->
    <div class="learning-stats">
      <div class="stat-row">
        <span class="stat-item">{{ t('memory.learning') }}: {{ learningCount }}</span>
        <span class="stat-item">{{ t('memory.new') }}: {{ newCount }}</span>
      </div>
    </div>
  </div>
</template>

<style scoped>
.memory-statistics {
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
  padding: 16px;
  background: var(--bg-secondary);
  border-radius: var(--radius-lg);
  border: 1px solid var(--border-color-light);
}

.stat-icon {
  width: 44px;
  height: 44px;
  border-radius: var(--radius-md);
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
  font-size: var(--font-size-xl);
  font-weight: 700;
  color: var(--text-primary);
}

.stat-label {
  font-size: var(--font-size-sm);
  color: var(--text-muted);
}

.progress-section {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 16px;
  background: var(--bg-secondary);
  border-radius: var(--radius-lg);
  border: 1px solid var(--border-color-light);
}

.progress-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.progress-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.progress-label {
  font-size: var(--font-size-sm);
  color: var(--text-muted);
}

.progress-value {
  font-size: var(--font-size-sm);
  font-weight: 600;
  color: var(--text-primary);
}

.learning-stats {
  padding: 12px 16px;
  background: var(--bg-secondary);
  border-radius: var(--radius-lg);
  border: 1px solid var(--border-color-light);
}

.stat-row {
  display: flex;
  justify-content: space-around;
  gap: 16px;
}

.stat-item {
  font-size: var(--font-size-sm);
  color: var(--text-secondary);
}

@media (max-width: 480px) {
  .stats-grid {
    grid-template-columns: 1fr;
  }
}
</style>