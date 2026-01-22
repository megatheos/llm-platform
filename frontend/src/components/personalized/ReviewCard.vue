<script setup lang="ts">
import { ref, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElButton, ElCard, ElProgress, ElTag } from 'element-plus'
import {
  Check,
  Close,
  Clock,
  Star,
  Warning,
  RefreshRight
} from '@element-plus/icons-vue'
import type { ReviewCardData } from '@/types/personalized'

const { t } = useI18n()

// Props
interface Props {
  cardData: ReviewCardData
  loading?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  loading: false
})

// Emits
const emit = defineEmits<{
  (e: 'submit', itemId: number, quality: number): void
  (e: 'skip', itemId: number): void
}>()

// Computed
const masteryPercentage = computed(() => {
  return Math.min(100, (props.cardData.item.masteryLevel / 5) * 100)
})

const daysUntilReviewText = computed(() => {
  const days = props.cardData.daysUntilReview
  if (days === 0) return t('memory.dueToday')
  if (days < 0) return `${Math.abs(days)} ${t('memory.days')} overdue`
  return `${days} ${t('memory.days')}`
})

// Local state
const showAnswer = ref(false)
const selectedQuality = ref<number | null>(null)
const animationClass = ref('')

// Quality options based on SM-2 algorithm
const qualityOptions = [
  { value: 0, label: t('memory.again'), icon: Close, color: '#ef4444', description: 'Complete blackout' },
  { value: 3, label: t('memory.hard'), icon: Warning, color: '#f59e0b', description: 'Correct but with difficulty' },
  { value: 4, label: t('memory.good'), icon: Check, color: '#10b981', description: 'Correct with some hesitation' },
  { value: 5, label: t('memory.easy'), icon: Star, color: '#3b82f6', description: 'Perfect recall' }
]

// Methods
function handleShowAnswer() {
  animationClass.value = 'flip-in'
  setTimeout(() => {
    showAnswer.value = true
    animationClass.value = ''
  }, 150)
}

function handleSubmit(quality: number) {
  selectedQuality.value = quality
  emit('submit', props.cardData.item.id, quality)
}

function handleSkip() {
  emit('skip', props.cardData.item.id)
}

function getDifficultyColor(): string {
  const days = props.cardData.daysUntilReview
  if (days <= 0) return '#ef4444'
  if (days <= 2) return '#f59e0b'
  return '#10b981'
}
</script>

<template>
  <div class="review-card" :class="animationClass">
    <!-- Mastery Header -->
    <div class="card-header">
      <div class="mastery-info">
        <span class="mastery-label">{{ t('memory.masteryLevel') }}</span>
        <div class="progress-bar-container">
          <el-progress
            :percentage="masteryPercentage"
            :stroke-width="6"
            :show-text="false"
            color="#3b82f6"
          />
        </div>
        <span class="mastery-value">{{ cardData.item.masteryLevel }}/5</span>
      </div>
      <el-tag :type="cardData.suggestedAction === 'mastered' ? 'success' : 'warning'" size="small">
        {{ daysUntilReviewText }}
      </el-tag>
    </div>

    <!-- Content Card -->
    <div class="content-card">
      <div class="content-main">
        <h2 class="content-text">{{ cardData.item.content }}</h2>
        <p v-if="cardData.item.translation" class="content-translation">
          {{ cardData.item.translation }}
        </p>
      </div>

      <!-- Answer Section -->
      <div v-if="showAnswer" class="answer-section">
        <div class="answer-stats">
          <div class="stat-item">
            <span class="stat-icon">📝</span>
            <span class="stat-value">{{ cardData.item.contentType }}</span>
            <span class="stat-label">Type</span>
          </div>
          <div class="stat-item">
            <span class="stat-icon">🔄</span>
            <span class="stat-value">{{ cardData.item.reviewCount }}</span>
            <span class="stat-label">Reviews</span>
          </div>
          <div class="stat-item">
            <span class="stat-icon">📅</span>
            <span class="stat-value">{{ cardData.item.interval }}d</span>
            <span class="stat-label">Interval</span>
          </div>
        </div>

        <!-- Quality Selection -->
        <div class="quality-options">
          <p class="quality-label">{{ t('memory.rateRecall') }}</p>
          <div class="options-grid">
            <button
              v-for="option in qualityOptions"
              :key="option.value"
              class="quality-btn"
              :class="{ selected: selectedQuality === option.value }"
              :style="{ '--option-color': option.color }"
              @click="handleSubmit(option.value)"
            >
              <div class="btn-bg"></div>
              <el-icon :size="24"><component :is="option.icon" /></el-icon>
              <span class="option-label">{{ option.label }}</span>
            </button>
          </div>
        </div>
      </div>

      <!-- Show Answer Button -->
      <div v-else class="show-answer-section">
        <el-button type="primary" size="large" @click="handleShowAnswer">
          {{ t('word.query') || 'Show Answer' }}
        </el-button>
      </div>
    </div>

    <!-- Actions -->
    <div class="card-actions">
      <el-button @click="handleSkip">
        <el-icon><RefreshRight /></el-icon>
        {{ t('memory.skip') }}
      </el-button>
    </div>
  </div>
</template>

<style scoped>
.review-card {
  background: var(--bg-secondary);
  border-radius: var(--radius-lg);
  padding: 20px;
  border: 1px solid var(--border-light);
  transition: all 0.3s ease;
}

.review-card.flip-in {
  animation: flipIn 0.3s ease;
}

@keyframes flipIn {
  from {
    opacity: 0;
    transform: rotateX(90deg);
  }
  to {
    opacity: 1;
    transform: rotateX(0);
  }
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--border-light);
}

.mastery-info {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 1;
}

.mastery-label {
  font-size: var(--font-size-sm);
  color: var(--text-muted);
  white-space: nowrap;
}

.progress-bar-container {
  flex: 1;
  max-width: 200px;
}

.progress-bar-container :deep(.el-progress) {
  --el-progress-text-color: var(--text-primary);
}

.mastery-value {
  font-size: var(--font-size-sm);
  font-weight: 600;
  color: var(--color-primary);
  white-space: nowrap;
}

.content-card {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.content-main {
  text-align: center;
  padding: 24px 0;
}

.content-text {
  font-size: 32px;
  font-weight: 700;
  color: var(--text-primary);
  margin-bottom: 12px;
  letter-spacing: 0.5px;
}

.content-translation {
  font-size: var(--font-size-lg);
  color: var(--text-secondary);
}

.answer-section {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.answer-stats {
  display: flex;
  justify-content: center;
  gap: 24px;
  padding: 16px;
  background: var(--bg-tertiary);
  border-radius: var(--radius-md);
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

.stat-icon {
  font-size: 20px;
}

.stat-value {
  font-size: var(--font-size-md);
  font-weight: 600;
  color: var(--text-primary);
}

.stat-label {
  font-size: var(--font-size-xs);
  color: var(--text-muted);
}

.quality-options {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.quality-label {
  font-size: var(--font-size-sm);
  color: var(--text-muted);
  text-align: center;
}

.options-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 10px;
}

.quality-btn {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 16px 8px;
  border: 2px solid var(--border-color);
  border-radius: var(--radius-md);
  background: var(--bg-tertiary);
  cursor: pointer;
  transition: all 0.2s;
  color: var(--text-secondary);
  position: relative;
  overflow: hidden;
}

.quality-btn .btn-bg {
  position: absolute;
  inset: 0;
  background: var(--option-color);
  opacity: 0;
  transition: opacity 0.2s;
}

.quality-btn:hover {
  border-color: var(--option-color);
  color: var(--option-color);
}

.quality-btn:hover .btn-bg {
  opacity: 0.08;
}

.quality-btn.selected {
  border-color: var(--option-color);
  background: color-mix(in srgb, var(--option-color) 12%, transparent);
  color: var(--option-color);
}

.quality-btn .btn-bg,
.quality-btn *,
.quality-btn ::after {
  pointer-events: none;
}

.option-label {
  font-size: var(--font-size-sm);
  font-weight: 500;
  position: relative;
  z-index: 1;
}

.show-answer-section {
  display: flex;
  justify-content: center;
}

.show-answer-section :deep(.el-button) {
  padding: 12px 32px;
  font-size: var(--font-size-md);
  border-radius: var(--radius-md);
}

.card-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid var(--border-light);
}

.card-actions :deep(.el-button) {
  border-radius: var(--radius-md);
}

@media (max-width: 480px) {
  .options-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .content-text {
    font-size: 24px;
  }

  .answer-stats {
    gap: 16px;
    padding: 12px;
  }
}
</style>
