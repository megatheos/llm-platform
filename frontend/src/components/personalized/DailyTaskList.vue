<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElProgress, ElCheckbox } from 'element-plus'
import {
  Check,
  Clock,
  Calendar,
  Document,
  ChatDotRound
} from '@element-plus/icons-vue'
import type { DailyTask } from '@/types/personalized'

const { t } = useI18n()

// Props
interface Props {
  tasks?: DailyTask[]
}

const props = withDefaults(defineProps<Props>(), {
  tasks: () => []
})

// Emits
const emit = defineEmits<{
  (e: 'complete', taskId: number): void
}>()

// State
const loading = ref(false)
const selectedDate = ref(new Date().toISOString().split('T')[0])
const showCompleted = ref(false)

// Computed
const filteredTasks = computed(() => {
  if (showCompleted.value) return props.tasks
  return props.tasks.filter(t => t.status !== 'completed')
})

const pendingTasks = computed(() => {
  return props.tasks.filter(t => t.status === 'pending' || t.status === 'in_progress')
})

const completedTasks = computed(() => {
  return props.tasks.filter(t => t.status === 'completed')
})

const overallProgress = computed(() => {
  if (props.tasks.length === 0) return 0
  const total = props.tasks.reduce((sum, t) => sum + t.targetCount, 0)
  const completed = props.tasks.reduce((sum, t) => sum + t.completedCount, 0)
  return Math.round((completed / total) * 100)
})

const taskTypeConfig = computed(() => ({
  review: { icon: Clock, color: '#f59e0b', label: t('memory.review'), emoji: '🔄' },
  new: { icon: Document, color: '#3b82f6', label: t('memory.new'), emoji: '📚' },
  quiz: { icon: Document, color: '#8b5cf6', label: t('quiz.title'), emoji: '📝' },
  dialogue: { icon: ChatDotRound, color: '#10b981', label: t('dialogue.title'), emoji: '💬' }
}))

// Methods
async function handleStatusChange(task: DailyTask, newStatus: 'pending' | 'in_progress' | 'completed' | 'skipped') {
  const index = props.tasks.findIndex(t => t.id === task.id)
  if (index !== -1) {
    props.tasks[index].status = newStatus
    if (newStatus === 'completed') {
      props.tasks[index].completedCount = props.tasks[index].targetCount
      emit('complete', task.id)
    }
  }
}

function handleComplete(task: DailyTask) {
  handleStatusChange(task, 'completed')
}

function handleSkip(task: DailyTask) {
  handleStatusChange(task, 'skipped')
}

function getTaskProgress(task: DailyTask): number {
  if (task.targetCount === 0) return 0
  return Math.min(100, Math.round((task.completedCount / task.targetCount) * 100))
}

function getStatusType(status: string): 'success' | 'warning' | 'info' | 'danger' {
  switch (status) {
    case 'completed': return 'success'
    case 'in_progress': return 'warning'
    case 'skipped': return 'info'
    default: return 'info'
  }
}

function formatDate(date: string): string {
  return new Date(date).toLocaleDateString(undefined, {
    weekday: 'long',
    year: 'numeric',
    month: 'long',
    day: 'numeric'
  })
}

function getWeekday(): string {
  const weekdays = ['Sunday', 'Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday']
  return weekdays[new Date().getDay()]
}
</script>

<template>
  <div class="daily-task-list">
    <!-- Header -->
    <div class="list-header">
      <div class="header-left">
        <div class="date-icon">
          <el-icon :size="24"><Calendar /></el-icon>
        </div>
        <div class="date-info">
          <span class="weekday">{{ getWeekday() }}</span>
          <span class="date">{{ formatDate(selectedDate) }}</span>
        </div>
      </div>
      <div class="progress-section">
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
              :stroke-dasharray="`${overallProgress}, 100`"
              d="M18 2.0845
                a 15.9155 15.9155 0 0 1 0 31.831
                a 15.9155 15.9155 0 0 1 0 -31.831"
            />
          </svg>
          <span class="progress-percent">{{ overallProgress }}%</span>
        </div>
        <div class="progress-info">
          <span class="progress-label">{{ t('plan.currentProgress') }}</span>
          <span class="progress-count">
            {{ completedTasks.length }}/{{ pendingTasks.length + completedTasks.length }} {{ t('plan.taskStatus.completed').toLowerCase() }}
          </span>
        </div>
      </div>
    </div>

    <!-- Task List -->
    <div class="tasks-container">
      <!-- Pending Tasks -->
      <div v-if="pendingTasks.length > 0" class="task-group">
        <h4 class="group-title">
          <span class="title-icon">📋</span>
          {{ t('plan.taskStatus.inProgress') }}
        </h4>
        <div class="task-list">
          <div
            v-for="task in pendingTasks"
            :key="task.id"
            class="task-item"
            :class="task.status"
          >
            <div class="task-icon" :style="{ background: taskTypeConfig[task.type]?.color + '15', color: taskTypeConfig[task.type]?.color }">
              <span class="emoji">{{ taskTypeConfig[task.type]?.emoji }}</span>
            </div>
            <div class="task-content">
              <span class="task-type">{{ taskTypeConfig[task.type]?.label }}</span>
              <div class="task-progress">
                <el-progress
                  :percentage="getTaskProgress(task)"
                  :stroke-width="6"
                  :show-text="false"
                  :color="taskTypeConfig[task.type]?.color"
                />
                <span class="progress-text">{{ task.completedCount }}/{{ task.targetCount }}</span>
              </div>
            </div>
            <div class="task-actions">
              <el-checkbox
                :model-value="task.status === 'completed'"
                @change="() => handleComplete(task)"
              />
            </div>
          </div>
        </div>
      </div>

      <!-- Completed Tasks -->
      <div v-if="completedTasks.length > 0" class="task-group">
        <h4 class="group-title completed">
          <span class="title-icon">✅</span>
          {{ t('plan.taskStatus.completed') }}
        </h4>
        <div class="task-list">
          <div
            v-for="task in completedTasks"
            :key="task.id"
            class="task-item completed"
          >
            <div class="task-icon" style="background: #10b98115; color: #10b981">
              <span class="emoji">✓</span>
            </div>
            <div class="task-content">
              <span class="task-type">{{ taskTypeConfig[task.type]?.label }}</span>
              <span class="task-done">{{ task.completedCount }}/{{ task.targetCount }} done</span>
            </div>
          </div>
        </div>
      </div>

      <!-- Empty State -->
      <div v-if="props.tasks.length === 0" class="empty-state">
        <div class="empty-icon">🎯</div>
        <p>{{ t('plan.noTasksToday') }}</p>
      </div>
    </div>
  </div>
</template>

<style scoped>
.daily-task-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.list-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px;
  background: var(--bg-secondary);
  border-radius: var(--radius-lg);
  border: 1px solid var(--border-light);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 14px;
}

.date-icon {
  width: 48px;
  height: 48px;
  background: var(--color-primary);
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
}

.date-info {
  display: flex;
  flex-direction: column;
}

.weekday {
  font-size: var(--font-size-lg);
  font-weight: 600;
  color: var(--text-primary);
}

.date {
  font-size: var(--font-size-sm);
  color: var(--text-muted);
}

.progress-section {
  display: flex;
  align-items: center;
  gap: 16px;
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

.progress-percent {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: var(--font-size-sm);
  font-weight: 700;
  color: var(--text-primary);
}

.progress-info {
  display: flex;
  flex-direction: column;
}

.progress-label {
  font-size: var(--font-size-sm);
  color: var(--text-muted);
}

.progress-count {
  font-size: var(--font-size-md);
  font-weight: 600;
  color: var(--text-primary);
}

.tasks-container {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.task-group {
  background: var(--bg-secondary);
  border-radius: var(--radius-lg);
  padding: 16px;
  border: 1px solid var(--border-light);
}

.group-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: var(--font-size-sm);
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 12px;
}

.title-icon {
  font-size: 16px;
}

.group-title.completed {
  color: #10b981;
}

.task-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.task-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px;
  background: var(--bg-tertiary);
  border-radius: var(--radius-md);
  transition: all 0.2s ease;
}

.task-item:hover {
  background: var(--border-light);
  transform: translateX(4px);
}

.task-item.completed {
  opacity: 0.7;
}

.task-icon {
  width: 40px;
  height: 40px;
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
}

.emoji {
  font-size: 18px;
}

.task-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.task-type {
  font-size: var(--font-size-sm);
  font-weight: 500;
  color: var(--text-primary);
}

.task-progress {
  display: flex;
  align-items: center;
  gap: 10px;
}

.task-progress :deep(.el-progress) {
  flex: 1;
}

.task-progress :deep(.el-progress-bar__outer) {
  background: var(--border-color);
}

.progress-text {
  font-size: var(--font-size-xs);
  color: var(--text-muted);
  min-width: 40px;
  text-align: right;
}

.task-done {
  font-size: var(--font-size-sm);
  color: #10b981;
  font-weight: 500;
}

.task-actions {
  display: flex;
  align-items: center;
}

.task-actions :deep(.el-checkbox) {
  --el-checkbox-checked-bg-color: var(--color-primary);
  --el-checkbox-checked-input-border-color: var(--color-primary);
}

.empty-state {
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

@media (max-width: 480px) {
  .list-header {
    flex-direction: column;
    gap: 16px;
    text-align: center;
  }

  .header-left {
    flex-direction: column;
  }

  .progress-section {
    width: 100%;
    justify-content: center;
  }
}
</style>