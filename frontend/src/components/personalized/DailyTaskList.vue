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
  review: { icon: Clock, color: '#f59e0b', label: t('memory.review') },
  new: { icon: Document, color: '#3b82f6', label: t('memory.new') },
  quiz: { icon: Document, color: '#8b5cf6', label: t('quiz.title') },
  dialogue: { icon: ChatDotRound, color: '#10b981', label: t('dialogue.title') }
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
</script>

<template>
  <div class="daily-task-list">
    <!-- Header -->
    <div class="list-header">
      <div class="date-display">
        <el-icon><Calendar /></el-icon>
        <span>{{ formatDate(selectedDate) }}</span>
      </div>
      <div class="progress-display">
        <span class="progress-label">{{ t('plan.currentProgress') }}</span>
        <el-progress
          :percentage="overallProgress"
          :stroke-width="8"
          :show-text="false"
          color="#10b981"
        />
        <span class="progress-value">{{ overallProgress }}%</span>
      </div>
    </div>

    <!-- Task List -->
    <div class="tasks-container">
      <!-- Pending Tasks -->
      <div v-if="pendingTasks.length > 0" class="task-group">
        <h4 class="group-title">{{ t('plan.taskStatus.inProgress') }}</h4>
        <div class="task-list">
          <div
            v-for="task in pendingTasks"
            :key="task.id"
            class="task-item"
            :class="task.status"
          >
            <div class="task-icon" :style="{ background: taskTypeConfig[task.type]?.color + '20', color: taskTypeConfig[task.type]?.color }">
              <el-icon :size="18">
                <component :is="taskTypeConfig[task.type]?.icon" />
              </el-icon>
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
        <h4 class="group-title completed">{{ t('plan.taskStatus.completed') }}</h4>
        <div class="task-list">
          <div
            v-for="task in completedTasks"
            :key="task.id"
            class="task-item completed"
          >
            <div class="task-icon" style="background: #10b98120; color: #10b981">
              <el-icon :size="18"><Check /></el-icon>
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
        <el-icon :size="40"><Calendar /></el-icon>
        <p>{{ t('plan.noTasksToday') }}</p>
      </div>
    </div>
  </div>
</template>

<style scoped>
.daily-task-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.list-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  background: var(--bg-secondary);
  border-radius: var(--radius-lg);
  border: 1px solid var(--border-color-light);
}

.date-display {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 500;
  color: var(--text-primary);
}

.progress-display {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 200px;
}

.progress-label {
  font-size: var(--font-size-sm);
  color: var(--text-muted);
}

.progress-value {
  font-size: var(--font-size-sm);
  font-weight: 600;
  color: var(--color-primary);
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
  border: 1px solid var(--border-color-light);
}

.group-title {
  font-size: var(--font-size-sm);
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 12px;
}

.group-title.completed {
  color: #10b981;
}

.task-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.task-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  background: var(--bg-tertiary);
  border-radius: var(--radius-md);
  transition: all 0.2s;
}

.task-item:hover {
  background: var(--border-color-light);
}

.task-item.completed {
  opacity: 0.7;
}

.task-icon {
  width: 36px;
  height: 36px;
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
}

.task-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.task-type {
  font-size: var(--font-size-sm);
  font-weight: 500;
  color: var(--text-primary);
}

.task-progress {
  display: flex;
  align-items: center;
  gap: 8px;
}

.task-progress :deep(.el-progress) {
  flex: 1;
}

.progress-text {
  font-size: var(--font-size-xs);
  color: var(--text-muted);
  min-width: 40px;
}

.task-done {
  font-size: var(--font-size-sm);
  color: #10b981;
}

.task-actions {
  display: flex;
  align-items: center;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 32px;
  color: var(--text-muted);
}
</style>