<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElEmpty } from 'element-plus'
import {
  Clock,
  RefreshRight,
  List,
  ArrowLeft,
  ArrowRight
} from '@element-plus/icons-vue'
import ReviewCard from './ReviewCard.vue'
import type { ReviewItem, ReviewCardData } from '@/types/personalized'
import { getDueReviews, getReviewItems, submitReview } from '@/api/memory'

const { t } = useI18n()

// Props
interface Props {
  cards?: ReviewCardData[]
  autoLoad?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  cards: () => [],
  autoLoad: true
})

// Emits
const emit = defineEmits<{
  (e: 'submit', itemId: number, quality: number): void
  (e: 'skip', itemId: number): void
}>()

// State
const reviewQueue = ref<ReviewCardData[]>(props.cards)
const currentIndex = ref(0)
const loading = ref(false)
const filterStatus = ref<'all' | 'due' | 'learning' | 'mastered'>('due')
const showHistory = ref(false)
const historyItems = ref<ReviewItem[]>([])
const historyLoading = ref(false)

// Computed
const currentCard = computed(() => reviewQueue.value[currentIndex.value])
const hasMoreReviews = computed(() => currentIndex.value < reviewQueue.value.length)
const progress = computed(() => {
  if (reviewQueue.value.length === 0) return 0
  return Math.round((currentIndex.value / reviewQueue.value.length) * 100)
})

// Methods
async function loadReviews() {
  loading.value = true
  try {
    const response = await getDueReviews(20)
    if (response.code === 0 || response.code === 200) {
      reviewQueue.value = response.data
      currentIndex.value = 0
    } else {
      ElMessage.error(response.message || t('memory.noReviewsDue'))
    }
  } catch (error) {
    console.error('Failed to load reviews:', error)
    ElMessage.error('Failed to load review items')
  } finally {
    loading.value = false
  }
}

async function loadHistory() {
  historyLoading.value = true
  try {
    const response = await getReviewItems(1, 50, filterStatus.value)
    if (response.code === 0 || response.code === 200) {
      historyItems.value = response.data.items
      showHistory.value = true
    }
  } catch (error) {
    console.error('Failed to load history:', error)
    ElMessage.error('Failed to load review history')
  } finally {
    historyLoading.value = false
  }
}

async function handleSubmit(itemId: number, quality: number) {
  emit('submit', itemId, quality)
  try {
    await submitReview(itemId, quality)
    nextCard()
    if (!hasMoreReviews.value && props.autoLoad) {
      await loadReviews()
    }
  } catch (error) {
    console.error('Failed to submit review:', error)
    ElMessage.error('Failed to submit review')
  }
}

function handleSkip(itemId: number) {
  emit('skip', itemId)
  nextCard()
}

function nextCard() {
  currentIndex.value++
}

function prevCard() {
  if (currentIndex.value > 0) {
    currentIndex.value--
  }
}

function goToCard(index: number) {
  if (index >= 0 && index < reviewQueue.value.length) {
    currentIndex.value = index
  }
}

function getStatusType(status: string): 'success' | 'warning' | 'info' | 'danger' {
  switch (status) {
    case 'mastered': return 'success'
    case 'learning': return 'warning'
    case 'new': return 'info'
    default: return 'info'
  }
}

function getMasteryLabel(mastery: number): string {
  if (mastery >= 4) return t('memory.mastered')
  if (mastery >= 2) return t('memory.learning')
  return t('memory.new')
}

// Lifecycle
onMounted(() => {
  if (props.autoLoad && props.cards.length === 0) {
    loadReviews()
  }
})
</script>

<template>
  <div class="review-list">
    <!-- Loading State -->
    <div v-if="loading" class="loading-state">
      <el-skeleton :rows="3" animated />
    </div>

    <!-- Empty State -->
    <el-empty
      v-else-if="reviewQueue.length === 0"
      :description="t('memory.allCaughtUp')"
      :image-size="120"
    >
      <template #image>
        <div class="empty-icon">🎉</div>
      </template>
      <el-button type="primary" @click="loadReviews">
        <el-icon><RefreshRight /></el-icon>
        {{ t('memory.reviewNow') }}
      </el-button>
    </el-empty>

    <!-- Review Content -->
    <div v-else class="review-content">
      <!-- Progress Bar -->
      <div class="progress-section">
        <div class="progress-info">
          <span class="progress-label">{{ t('quiz.progress') }}</span>
          <span class="progress-count">{{ currentIndex + 1 }}/{{ reviewQueue.length }}</span>
        </div>
        <el-progress
          :percentage="progress"
          :stroke-width="8"
          :show-text="false"
          color="#3b82f6"
        />
      </div>

      <!-- Current Card -->
      <div class="card-container">
        <Transition name="slide-fade" mode="out-in">
          <ReviewCard
            v-if="currentCard"
            :key="currentCard.item.id"
            :card-data="currentCard"
            :loading="loading"
            @submit="handleSubmit"
            @skip="handleSkip"
          />
        </Transition>
      </div>

      <!-- Navigation -->
      <div class="navigation">
        <el-button
          :disabled="currentIndex === 0"
          @click="prevCard"
        >
          <el-icon><ArrowLeft /></el-icon>
          {{ t('quiz.previous') }}
        </el-button>

        <div class="card-indicators">
          <span
            v-for="(card, index) in reviewQueue"
            :key="card.item.id"
            class="indicator"
            :class="{
              active: index === currentIndex,
              completed: index < currentIndex
            }"
            @click="goToCard(index)"
          />
        </div>

        <el-button
          type="primary"
          :disabled="!hasMoreReviews"
          @click="nextCard"
        >
          {{ t('common.next') }}
          <el-icon><ArrowRight /></el-icon>
        </el-button>
      </div>

      <!-- History Toggle -->
      <div class="history-toggle">
        <el-button link @click="loadHistory">
          <el-icon><List /></el-icon>
          {{ t('memory.reviewList') }}
        </el-button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.review-list {
  width: 100%;
}

.loading-state {
  padding: 40px;
}

.empty-state {
  padding: 40px;
  text-align: center;
}

.empty-icon {
  font-size: 64px;
  margin-bottom: 16px;
}

.review-content {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.progress-section {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.progress-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.progress-label {
  font-size: var(--font-size-sm);
  color: var(--text-muted);
}

.progress-count {
  font-size: var(--font-size-sm);
  font-weight: 500;
  color: var(--text-primary);
}

.card-container {
  min-height: 300px;
}

.navigation {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
}

.card-indicators {
  display: flex;
  gap: 8px;
}

.indicator {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: var(--border-color);
  cursor: pointer;
  transition: all 0.2s;
}

.indicator:hover {
  background: var(--color-accent);
}

.indicator.active {
  background: var(--color-primary);
  transform: scale(1.2);
}

.indicator.completed {
  background: var(--color-accent);
}

.history-toggle {
  text-align: center;
  margin-top: 8px;
}

.slide-fade-enter-active,
.slide-fade-leave-active {
  transition: all 0.3s ease;
}

.slide-fade-enter-from {
  opacity: 0;
  transform: translateX(20px);
}

.slide-fade-leave-to {
  opacity: 0;
  transform: translateX(-20px);
}
</style>