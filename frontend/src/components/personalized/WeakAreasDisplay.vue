<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElProgress } from 'element-plus'
import {
  TrendCharts,
  ArrowUp,
  ArrowDown,
  Minus,
  Aim,
  View
} from '@element-plus/icons-vue'
import type { KnowledgeArea } from '@/types/personalized'

const { t } = useI18n()

// Props
interface Props {
  weakAreas?: KnowledgeArea[]
  strongAreas?: KnowledgeArea[]
}

const props = withDefaults(defineProps<Props>(), {
  weakAreas: () => [],
  strongAreas: () => []
})

// State
const loading = ref(false)
const selectedCategory = ref<string | null>(null)

// Computed
const sortedWeakAreas = computed(() => {
  return [...props.weakAreas].sort((a, b) => a.score - b.score)
})

const sortedStrongAreas = computed(() => {
  return [...props.strongAreas].sort((a, b) => b.score - a.score)
})

const allCategories = computed(() => {
  const categories = new Set([
    ...props.weakAreas.map(a => a.category),
    ...props.strongAreas.map(a => a.category)
  ])
  return Array.from(categories)
})

const focusArea = computed(() => {
  return sortedWeakAreas.value[0] || null
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

function getScoreColor(score: number): string {
  if (score >= 80) return '#10b981'
  if (score >= 60) return '#f59e0b'
  if (score >= 40) return '#ef4444'
  return '#6b7280'
}

function getCategoryName(category: string): string {
  const categoryMap: Record<string, string> = {
    vocabulary: t('profile.categories.vocabulary'),
    grammar: t('profile.categories.grammar'),
    listening: t('profile.categories.listening'),
    speaking: t('profile.categories.speaking'),
    reading: t('profile.categories.reading'),
    writing: t('profile.categories.writing')
  }
  return categoryMap[category] || category
}

function filterByCategory(category: string | null) {
  selectedCategory.value = category
}
</script>

<template>
  <div class="weak-areas-display">
    <!-- Focus Area -->
    <div v-if="focusArea" class="focus-area">
      <div class="focus-header">
        <el-icon><Aim /></el-icon>
        <span>{{ t('profile.focusAreas') }}</span>
      </div>
      <div class="focus-content">
        <span class="focus-category">{{ focusArea.displayName || getCategoryName(focusArea.category) }}</span>
        <el-progress
          :percentage="focusArea.score"
          :stroke-width="10"
          :show-text="false"
          :color="getScoreColor(focusArea.score)"
        />
        <span class="focus-score">{{ focusArea.score }}%</span>
      </div>
    </div>

    <!-- Weak Areas -->
    <div v-if="sortedWeakAreas.length > 0" class="areas-section">
      <h4 class="section-title">
        <el-icon><TrendCharts /></el-icon>
        {{ t('profile.weakAreas') }}
      </h4>
      <div class="areas-list">
        <div
          v-for="area in sortedWeakAreas"
          :key="area.category"
          class="area-item"
          :class="{ selected: selectedCategory === area.category }"
          @click="filterByCategory(area.category)"
        >
          <div class="area-info">
            <span class="area-name">{{ area.displayName || getCategoryName(area.category) }}</span>
            <span class="area-meta">
              {{ area.masteredItems }}/{{ area.totalItems }} mastered
            </span>
          </div>
          <div class="area-progress">
            <el-progress
              :percentage="area.score"
              :stroke-width="6"
              :show-text="false"
              :color="getScoreColor(area.score)"
            />
            <div class="area-trend" :style="{ color: getTrendColor(area.trend) }">
              <el-icon>
                <component :is="getTrendIcon(area.trend)" />
              </el-icon>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Strong Areas -->
    <div v-if="sortedStrongAreas.length > 0" class="areas-section">
      <h4 class="section-title strong">
        <el-icon><View /></el-icon>
        {{ t('profile.strongAreas') }}
      </h4>
      <div class="areas-list">
        <div
          v-for="area in sortedStrongAreas.slice(0, 3)"
          :key="area.category"
          class="area-item strong"
        >
          <div class="area-info">
            <span class="area-name">{{ area.displayName || getCategoryName(area.category) }}</span>
            <span class="area-meta">
              {{ area.masteredItems }}/{{ area.totalItems }} mastered
            </span>
          </div>
          <div class="area-progress">
            <el-progress
              :percentage="area.score"
              :stroke-width="6"
              :show-text="false"
              color="#10b981"
            />
          </div>
        </div>
      </div>
    </div>

    <!-- Empty State -->
    <div v-if="sortedWeakAreas.length === 0 && sortedStrongAreas.length === 0" class="empty-state">
      <el-icon :size="40"><TrendCharts /></el-icon>
      <p>{{ t('profile.noData') }}</p>
    </div>
  </div>
</template>

<style scoped>
.weak-areas-display {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.focus-area {
  background: linear-gradient(135deg, #fef2f2 0%, #fff 100%);
  border-radius: var(--radius-lg);
  padding: 16px;
  border: 1px solid #fecaca;
}

.focus-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
  color: #ef4444;
  font-weight: 600;
  font-size: var(--font-size-sm);
}

.focus-content {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.focus-category {
  font-size: var(--font-size-lg);
  font-weight: 600;
  color: var(--text-primary);
}

.focus-score {
  font-size: var(--font-size-sm);
  color: var(--text-muted);
  align-self: flex-end;
}

.areas-section {
  background: var(--bg-secondary);
  border-radius: var(--radius-lg);
  padding: 16px;
  border: 1px solid var(--border-color-light);
}

.section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: var(--font-size-sm);
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 12px;
}

.section-title.strong {
  color: #10b981;
}

.areas-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.area-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px;
  background: var(--bg-tertiary);
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: all 0.2s;
}

.area-item:hover {
  background: var(--border-color-light);
}

.area-item.selected {
  border: 1px solid var(--color-primary);
}

.area-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.area-name {
  font-size: var(--font-size-sm);
  font-weight: 500;
  color: var(--text-primary);
}

.area-meta {
  font-size: var(--font-size-xs);
  color: var(--text-muted);
}

.area-progress {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100px;
}

.area-progress :deep(.el-progress) {
  flex: 1;
}

.area-trend {
  display: flex;
  align-items: center;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 24px;
  color: var(--text-muted);
}
</style>