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
    <!-- Header -->
    <div class="card-header">
      <div class="mastery-info">
        <span class="mastery-label">{{ t('memory.masteryLevel') }}</span>
        <el-progress
          :percentage="masteryPercentage"
          :stroke-width="6"
          :show-text="false"
          color="#3b82f6"
        />
        <span class="mastery-value">{{ cardData.item.masteryLevel }}/5</span>
      </div>
      <el-tag :type="cardData.suggestedAction === 'mastered' ? 'success' : 'warning'" size="small">
        {{ daysUntilReviewText }}
      </el-tag>
    </div>

    <!-- Content -->
    <div class="card-content">
      <div class="content-main">
        <h2 class="content-text">{{ cardData.item.content }}</h2>
        <p v-if="cardData.item.translation" class="content-translation">
          {{ cardData.item.translation }}
        </p>
      </div>

      <!-- Answer Section -->
      <div v-if="showAnswer" class="answer-section">
        <div class="answer-content">
          <p>Type: {{ cardData.item.contentType }}</p>
          <p>Reviews: {{ cardData.item.reviewCount }}</p>
          <p>Interval: {{ cardData.item.interval }} days</p>
        </div>

        <!-- Quality Selection -->
        <div class="quality-options">
          <p class="quality-label">{{ t('memory.review') }}:</p>
          <div class="options-grid">
            <button
              v-for="option in qualityOptions"
              :key="option.value"
              class="quality-btn"
              :class="{ selected: selectedQuality === option.value }"
              :style="{ '--option-color': option.color }"
              @click="handleSubmit(option.value)"
            >
              <el-icon :size="20"><component :is="option.icon" /></el-icon>
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
  padding: 24px;
  border: 1px solid var(--border-color-light);
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
  margin-bottom: 20px;
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

.mastery-value {
  font-size: var(--font-size-sm);
  font-weight: 600;
  color: var(--text-primary);
  white-space: nowrap;
}

.card-content {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.content-main {
  text-align: center;
  padding: 20px 0;
}

.content-text {
  font-size: var(--font-size-2xl);
  font-weight: 700;
  color: var(--text-primary);
  margin-bottom: 8px;
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

.answer-content {
  padding: 16px;
  background: var(--bg-tertiary);
  border-radius: var(--radius-md);
  font-size: var(--font-size-sm);
  color: var(--text-secondary);
}

.quality-options {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.quality-label {
  font-size: var(--font-size-sm);
  color: var(--text-muted);
}

.options-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 8px;
}

.quality-btn {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 16px 8px;
  border: 2px solid var(--border-color);
  border-radius: var(--radius-md);
  background: var(--bg-secondary);
  cursor: pointer;
  transition: all 0.2s;
  color: var(--text-secondary);
}

.quality-btn:hover {
  border-color: var(--option-color);
  background: var(--option-color);
  background: color-mix(in srgb, var(--option-color) 10%, transparent);
}

.quality-btn.selected {
  border-color: var(--option-color);
  background: color-mix(in srgb, var(--option-color) 15%, transparent);
  color: var(--option-color);
}

.option-label {
  font-size: var(--font-size-sm);
  font-weight: 500;
}

.show-answer-section {
  display: flex;
  justify-content: center;
}

.card-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid var(--border-color-light);
}

@media (max-width: 480px) {
  .options-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
