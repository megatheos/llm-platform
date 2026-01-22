<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import {
  Trophy,
  Star,
  Medal,
  Lock,
  Check
} from '@element-plus/icons-vue'
import type { Achievement, AchievementDefinition, AchievementCategory } from '@/types/personalized'
import { getAchievementDefinitions, getEarnedAchievements } from '@/api/motivation'

const { t } = useI18n()

// Props
interface Props {
  achievements?: Achievement[]
  totalCount?: number
  unlockedCount?: number
}

const props = withDefaults(defineProps<Props>(), {
  achievements: () => [],
  totalCount: 0,
  unlockedCount: 0
})

// State - use props if provided, otherwise load from API
const achievements = ref<Achievement[]>(props.achievements)
const definitions = ref<AchievementDefinition[]>([])
const loading = ref(false)
const selectedCategory = ref<AchievementCategory | 'all'>('all')

// Computed
const earnedIds = computed(() => new Set(achievements.value.map(a => a.achievementId)))

const filteredDefinitions = computed(() => {
  if (selectedCategory.value === 'all') return definitions.value
  return definitions.value.filter(d => d.category === selectedCategory.value)
})

const categories = computed<{ value: AchievementCategory | 'all'; label: string }[]>(() => [
  { value: 'all', label: t('records.allActivities') },
  { value: 'learning', label: t('motivation.categories.learning') },
  { value: 'streak', label: t('motivation.categories.streak') },
  { value: 'mastery', label: t('motivation.categories.mastery') },
  { value: 'exploration', label: t('motivation.categories.exploration') },
  { value: 'special', label: t('motivation.categories.special') }
])

const earnedCount = computed(() => achievements.value.length)
const totalCount = computed(() => definitions.value.length)
const completionRate = computed(() => {
  if (totalCount.value === 0) return 0
  return Math.round((earnedCount.value / totalCount.value) * 100)
})

// Methods
async function loadAchievements() {
  // If achievements were provided via props, only load definitions
  if (props.achievements && props.achievements.length > 0) {
    loading.value = true
    try {
      const defsResponse = await getAchievementDefinitions()
      if (defsResponse.code === 0 || defsResponse.code === 200) {
        definitions.value = defsResponse.data
      }
    } catch (error) {
      console.error('Failed to load definitions:', error)
    } finally {
      loading.value = false
    }
  } else {
    // Load both achievements and definitions from API
    loading.value = true
    try {
      const [defsResponse, earnedResponse] = await Promise.all([
        getAchievementDefinitions(),
        getEarnedAchievements()
      ])

      if (defsResponse.code === 0 || defsResponse.code === 200) {
        definitions.value = defsResponse.data
      }

      if (earnedResponse.code === 0 || earnedResponse.code === 200) {
        achievements.value = earnedResponse.data
      }
    } catch (error) {
      console.error('Failed to load achievements:', error)
      ElMessage.error('Failed to load achievements')
    } finally {
      loading.value = false
    }
  }
}

function isEarned(achievementId: string): boolean {
  return earnedIds.value.has(achievementId)
}

function getEarnedDate(achievementId: string): string | null {
  const achievement = achievements.value.find(a => a.achievementId === achievementId)
  return achievement?.earnedAt || null
}

function getRarityIcon(rarity: string) {
  switch (rarity) {
    case 'common': return Star
    case 'rare': return Medal
    case 'epic': return Trophy
    case 'legendary': return Trophy
    default: return Star
  }
}

function getRarityColor(rarity: string): string {
  switch (rarity) {
    case 'common': return '#6b7280'
    case 'rare': return '#3b82f6'
    case 'epic': return '#8b5cf6'
    case 'legendary': return '#f59e0b'
    default: return '#6b7280'
  }
}

function getRarityTagType(rarity: string): string {
  switch (rarity) {
    case 'common': return 'info'
    case 'rare': return 'primary'
    case 'epic': return 'success'
    case 'legendary': return 'warning'
    default: return 'info'
  }
}

function formatDate(dateString: string | null): string {
  if (!dateString) return ''
  return new Date(dateString).toLocaleDateString(undefined, {
    year: 'numeric',
    month: 'short',
    day: 'numeric'
  })
}

function handleCategoryChange(category: AchievementCategory | 'all') {
  selectedCategory.value = category
}

function getCategoryEmoji(category: string): string {
  switch (category) {
    case 'learning': return '📚'
    case 'streak': return '🔥'
    case 'mastery': return '🏆'
    case 'exploration': return '🗺️'
    case 'special': return '⭐'
    default: return '🎯'
  }
}

// Lifecycle
onMounted(() => {
  loadAchievements()
})

</script>

<template>
  <div class="achievement-badge-wall">
    <!-- Header -->
    <div class="wall-header">
      <div class="header-left">
        <div class="header-icon">
          <el-icon :size="24"><Trophy /></el-icon>
        </div>
        <div>
          <h3>{{ t('motivation.achievements') }}</h3>
          <p class="subtitle">
            {{ earnedCount }} / {{ totalCount }} {{ t('motivation.earned') }}
          </p>
        </div>
      </div>
      <div class="header-right">
        <div class="progress-circle">
          <svg viewBox="0 0 36 36">
            <path
              class="circle-bg"
              d="M18 2.0845
                a 15.9155 15.9155 0 0 1 0 31.831
                a 15.9155 15.9155 0 0 1 0 -31.831"
            />
            <path
              class="circle-progress"
              :stroke-dasharray="`${completionRate}, 100`"
              d="M18 2.0845
                a 15.9155 15.9155 0 0 1 0 31.831
                a 15.9155 15.9155 0 0 1 0 -31.831"
            />
          </svg>
          <span class="circle-text">{{ completionRate }}%</span>
        </div>
      </div>
    </div>

    <!-- Category Filter -->
    <div class="category-filters">
      <el-radio-group
        v-model="selectedCategory"
        size="small"
        @change="(val: string | number | boolean | undefined) => handleCategoryChange(val as AchievementCategory | 'all')"
      >
        <el-radio-button
          v-for="cat in categories"
          :key="cat.value"
          :value="cat.value"
        >
          <span class="cat-emoji">{{ getCategoryEmoji(cat.value) }}</span>
          {{ cat.label }}
        </el-radio-button>
      </el-radio-group>
    </div>

    <!-- Loading State -->
    <div v-if="loading" class="loading-state">
      <div class="skeleton-grid">
        <div v-for="i in 6" :key="i" class="skeleton-card"></div>
      </div>
    </div>

    <!-- Achievement Grid -->
    <div v-else class="achievement-grid">
      <div
        v-for="def in filteredDefinitions"
        :key="def.id"
        class="achievement-card"
        :class="{ earned: isEarned(def.id), locked: !isEarned(def.id) }"
      >
        <div class="card-glow" :style="{ background: getRarityColor(def.rarity) + '15' }"></div>
        <div class="badge-container">
          <!-- Badge Icon -->
          <div
            class="badge-icon"
            :style="{
              color: isEarned(def.id) ? getRarityColor(def.rarity) : '#9ca3af',
              backgroundColor: isEarned(def.id)
                ? getRarityColor(def.rarity) + '15'
                : '#f3f4f6'
            }"
          >
            <el-icon :size="28">
              <component :is="getRarityIcon(def.rarity)" />
            </el-icon>
            <div v-if="!isEarned(def.id)" class="lock-overlay">
              <el-icon :size="20"><Lock /></el-icon>
            </div>
          </div>

          <!-- Badge Info -->
          <div class="badge-info">
            <div class="badge-title-row">
              <span class="badge-title">{{ def.title }}</span>
              <el-tag
                size="small"
                :type="getRarityTagType(def.rarity) as any"
                class="rarity-tag"
              >
                {{ def.rarity }}
              </el-tag>
            </div>
            <div class="badge-description">
              {{ def.description }}
            </div>
            <div v-if="isEarned(def.id)" class="earned-date">
              <el-icon size="12"><Check /></el-icon>
              {{ formatDate(getEarnedDate(def.id)) }}
            </div>
          </div>
        </div>
      </div>

      <!-- Empty State -->
      <div v-if="filteredDefinitions.length === 0" class="empty-state">
        <div class="empty-icon">🏆</div>
        <p>{{ t('motivation.noAchievements') }}</p>
      </div>
    </div>
  </div>
</template>

<style scoped>
.achievement-badge-wall {
  padding: 20px;
  background: var(--bg-secondary);
  border-radius: var(--radius-lg);
  border: 1px solid var(--border-light);
}

.wall-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 20px;
  border-bottom: 1px solid var(--border-light);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 14px;
}

.header-icon {
  width: 48px;
  height: 48px;
  background: var(--color-primary);
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
}

.header-left h3 {
  margin: 0;
  font-size: var(--font-size-lg);
  font-weight: 600;
  color: var(--text-primary);
}

.subtitle {
  margin: 4px 0 0 0;
  font-size: var(--font-size-sm);
  color: var(--text-muted);
}

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.progress-circle {
  position: relative;
  width: 56px;
  height: 56px;
}

.progress-circle svg {
  transform: rotate(-90deg);
}

.circle-bg {
  fill: none;
  stroke: var(--border-color);
  stroke-width: 3;
}

.circle-progress {
  fill: none;
  stroke: var(--color-primary);
  stroke-width: 3;
  stroke-linecap: round;
  transition: stroke-dasharray 0.5s ease;
}

.circle-text {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: var(--font-size-sm);
  font-weight: 700;
  color: var(--text-primary);
}

.category-filters {
  margin-bottom: 20px;
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.cat-emoji {
  margin-right: 4px;
}

.loading-state {
  padding: 20px;
}

.skeleton-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 12px;
}

.skeleton-card {
  height: 80px;
  background: var(--bg-tertiary);
  border-radius: var(--radius-md);
}

.achievement-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 14px;
}

.achievement-card {
  background: var(--bg-tertiary);
  border-radius: var(--radius-lg);
  padding: 16px;
  transition: all 0.3s ease;
  border: 1px solid var(--border-light);
  position: relative;
  overflow: hidden;
}

.card-glow {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 4px;
  opacity: 0;
  transition: opacity 0.3s ease;
}

.achievement-card.earned .card-glow {
  opacity: 1;
}

.achievement-card.earned {
  background: var(--bg-card);
}

.achievement-card.earned:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
}

.achievement-card.locked {
  opacity: 0.7;
}

.badge-container {
  display: flex;
  gap: 14px;
  align-items: flex-start;
}

.badge-icon {
  width: 52px;
  height: 52px;
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  flex-shrink: 0;
  transition: all 0.3s ease;
}

.achievement-card.earned:hover .badge-icon {
  transform: scale(1.1);
}

.lock-overlay {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.3);
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
}

.badge-info {
  flex: 1;
  min-width: 0;
}

.badge-title-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}

.badge-title {
  font-weight: 600;
  font-size: var(--font-size-md);
  color: var(--text-primary);
}

.rarity-tag {
  font-size: 10px;
  padding: 2px 8px;
  border-radius: 4px;
  text-transform: capitalize;
}

.badge-description {
  font-size: var(--font-size-sm);
  color: var(--text-secondary);
  line-height: 1.5;
  margin-bottom: 8px;
}

.earned-date {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: var(--font-size-xs);
  color: #10b981;
  font-weight: 500;
}

.empty-state {
  grid-column: 1 / -1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding: 40px;
  color: var(--text-muted);
}

.empty-icon {
  font-size: 48px;
}

/* Element Plus overrides */
:deep(.el-radio-group) {
  background: var(--bg-tertiary);
  border-radius: var(--radius-md);
  padding: 4px;
}

:deep(.el-radio-button) {
  border-radius: var(--radius-sm);
}

:deep(.el-radio-button__inner) {
  background: transparent !important;
  border: none !important;
  color: var(--text-secondary) !important;
  border-radius: var(--radius-sm) !important;
  padding: 8px 12px;
}

:deep(.el-radio-button__original-radio:checked + .el-radio-button__inner) {
  background: var(--color-primary) !important;
  color: white !important;
  box-shadow: none !important;
}

:deep(.el-progress) {
  --el-color-primary: var(--primary);
}

@media (max-width: 480px) {
  .wall-header {
    flex-direction: column;
    gap: 16px;
    text-align: center;
  }

  .category-filters {
    justify-content: center;
  }

  :deep(.el-radio-button__inner) {
    padding: 6px 10px;
    font-size: var(--font-size-xs);
  }
}
</style>