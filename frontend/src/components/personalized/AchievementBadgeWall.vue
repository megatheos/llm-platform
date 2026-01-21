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
        <el-icon size="24" class="header-icon">
          <Trophy />
        </el-icon>
        <div>
          <h3>{{ t('motivation.achievements') }}</h3>
          <p class="subtitle">
            {{ earnedCount }} / {{ totalCount }} {{ t('motivation.earned') }}
          </p>
        </div>
      </div>
      <div class="completion-rate">
        <el-progress
          :percentage="completionRate"
          :stroke-width="8"
          :color="getRarityColor"
          type="line"
        />
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
          :label="cat.value"
        >
          {{ cat.label }}
        </el-radio-button>
      </el-radio-group>
    </div>

    <!-- Loading State -->
    <div v-if="loading" class="loading-state">
      <el-skeleton :rows="3" animated />
    </div>

    <!-- Achievement Grid -->
    <div v-else class="achievement-grid">
      <div
        v-for="def in filteredDefinitions"
        :key="def.id"
        class="achievement-card"
        :class="{ earned: isEarned(def.id), locked: !isEarned(def.id) }"
      >
        <div class="badge-container">
          <!-- Badge Icon -->
          <div
            class="badge-icon"
            :style="{
              color: getRarityColor(def.rarity),
              backgroundColor: isEarned(def.id)
                ? getRarityColor(def.rarity) + '20'
                : '#e5e7eb'
            }"
          >
            <el-icon size="28">
              <component :is="getRarityIcon(def.rarity)" />
            </el-icon>
            <div v-if="!isEarned(def.id)" class="lock-overlay">
              <Lock />
            </div>
          </div>

          <!-- Badge Info -->
          <div class="badge-info">
            <div class="badge-title">
              {{ def.title }}
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
        <el-empty :description="t('motivation.noAchievements')" />
      </div>
    </div>
  </div>
</template>

<style scoped>
.achievement-badge-wall {
  padding: 16px;
  background: var(--bg-card);
  border-radius: 12px;
  border: 1px solid var(--border-light);
}

.wall-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--border-light);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.header-icon {
  color: var(--primary);
  background: var(--primary-light);
  border-radius: 8px;
  padding: 8px;
}

.header-left h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: var(--text-primary);
}

.subtitle {
  margin: 4px 0 0 0;
  font-size: 12px;
  color: var(--text-secondary);
}

.completion-rate {
  width: 120px;
}

.category-filters {
  margin-bottom: 16px;
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.loading-state {
  padding: 20px;
}

.achievement-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 12px;
}

.achievement-card {
  background: var(--bg-tertiary);
  border-radius: 8px;
  padding: 12px;
  transition: all 0.2s ease;
  border: 1px solid transparent;
}

.achievement-card.earned {
  background: var(--bg-card);
  border-color: var(--border-light);
}

.achievement-card.earned:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.achievement-card.locked {
  opacity: 0.6;
}

.badge-container {
  display: flex;
  gap: 12px;
  align-items: flex-start;
}

.badge-icon {
  width: 56px;
  height: 56px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  flex-shrink: 0;
}

.lock-overlay {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.4);
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
}

.badge-info {
  flex: 1;
  min-width: 0;
}

.badge-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-weight: 600;
  font-size: 14px;
  color: var(--text-primary);
  margin-bottom: 4px;
}

.rarity-tag {
  font-size: 10px;
  padding: 2px 6px;
  border-radius: 4px;
}

.badge-description {
  font-size: 12px;
  color: var(--text-secondary);
  margin-bottom: 4px;
  line-height: 1.4;
}

.earned-date {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 11px;
  color: var(--success);
  margin-top: 4px;
}

.empty-state {
  padding: 40px 20px;
  text-align: center;
}

/* Element Plus overrides */
:deep(.el-radio-group) {
  background: var(--bg-tertiary);
  border-radius: 6px;
  padding: 2px;
}

:deep(.el-radio-button) {
  border-radius: 4px;
}

:deep(.el-radio-button__inner) {
  background: transparent !important;
  border: none !important;
  color: var(--text-secondary) !important;
  border-radius: 4px !important;
}

:deep(.el-radio-button__original-radio:checked + .el-radio-button__inner) {
  background: var(--primary) !important;
  color: white !important;
  box-shadow: none !important;
}

:deep(.el-progress) {
  --el-color-primary: var(--primary);
}
</style>