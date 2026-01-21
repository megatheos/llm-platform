<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import {
  Warning,
  Bell,
  Check,
  Close,
  TrendCharts,
  Clock,
  DataLine,
  ArrowRight
} from '@element-plus/icons-vue'
import type { PlanAdjustment } from '@/types/personalized'

const { t } = useI18n()

// Props
interface Props {
  adjustments?: PlanAdjustment[]
}

const props = withDefaults(defineProps<Props>(), {
  adjustments: () => []
})

// Emits
const emit = defineEmits<{
  (e: 'read', adjustmentId: number): void
}>()

// State
const loading = ref(false)
const visible = ref(false)

// Computed
const unreadCount = computed(() => {
  return props.adjustments.filter(a => !a.isRead).length
})

const recentAdjustments = computed(() => {
  return props.adjustments.slice(0, 5)
})

// Methods
function handleMarkRead(adjustmentId: number) {
  emit('read', adjustmentId)
}

function handleAccept(adjustment: PlanAdjustment) {
  ElMessage.success(t('plan.acceptAdjustment'))
  handleMarkRead(adjustment.id)
}

function handleDismiss(adjustment: PlanAdjustment) {
  handleMarkRead(adjustment.id)
}

function getAdjustmentIcon(type: string) {
  switch (type) {
    case 'goal_change': return TrendCharts
    case 'schedule_change': return Clock
    case 'difficulty_adjustment': return DataLine
    default: return Warning
  }
}

function getAdjustmentColor(type: string): string {
  switch (type) {
    case 'goal_change': return '#3b82f6'
    case 'schedule_change': return '#f59e0b'
    case 'difficulty_adjustment': return '#8b5cf6'
    default: return '#ef4444'
  }
}

function formatDate(date: string): string {
  return new Date(date).toLocaleDateString(undefined, {
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  })
}

function showAll() {
  visible.value = true
}

function hideAll() {
  visible.value = false
}
</script>

<template>
  <div class="plan-adjustment-notice">
    <!-- Notification Badge -->
    <div class="notification-badge" v-if="unreadCount > 0" @click="showAll">
      <el-icon :size="20"><Bell /></el-icon>
      <span class="badge-count">{{ unreadCount }}</span>
    </div>

    <!-- Panel -->
    <el-popover
      v-model:visible="visible"
      placement="bottom-end"
      :width="360"
      trigger="click"
    >
      <template #reference>
        <el-button class="trigger-btn" text>
          <el-icon :size="20"><Bell /></el-icon>
          <span v-if="unreadCount > 0" class="badge-count-small">{{ unreadCount }}</span>
        </el-button>
      </template>
      
      <div class="adjustment-panel">
        <div class="panel-header">
          <h4>
            <el-icon><Warning /></el-icon>
            {{ t('plan.adjustmentNotice') }}
          </h4>
          <el-button text size="small" @click="hideAll">
            <el-icon><Close /></el-icon>
          </el-button>
        </div>

        <!-- Loading -->
        <div class="loading-state" v-if="loading">
          <span>{{ t('common.loading') }}</span>
        </div>

        <!-- Adjustments List -->
        <div class="adjustments-list" v-else-if="props.adjustments.length > 0">
          <div
            v-for="adjustment in recentAdjustments"
            :key="adjustment.id"
            class="adjustment-item"
            :class="{ unread: !adjustment.isRead }"
          >
            <div
              class="adjustment-icon"
              :style="{ backgroundColor: getAdjustmentColor(adjustment.type) + '20', color: getAdjustmentColor(adjustment.type) }"
            >
              <el-icon :size="18">
                <component :is="getAdjustmentIcon(adjustment.type)" />
              </el-icon>
            </div>
            
            <div class="adjustment-content">
              <div class="adjustment-header">
                <span class="adjustment-reason">{{ adjustment.reason }}</span>
                <span class="adjustment-date">{{ formatDate(adjustment.suggestedAt) }}</span>
              </div>
              
              <div class="adjustment-details" v-if="adjustment.oldValue || adjustment.newValue">
                <span class="old-value" v-if="adjustment.oldValue">
                  {{ adjustment.oldValue }}
                </span>
                <el-icon><ArrowRight /></el-icon>
                <span class="new-value" v-if="adjustment.newValue">
                  {{ adjustment.newValue }}
                </span>
              </div>
              
              <div class="adjustment-actions" v-if="!adjustment.isRead">
                <el-button
                  type="primary"
                  size="small"
                  @click="handleAccept(adjustment)"
                >
                  {{ t('plan.acceptAdjustment') }}
                </el-button>
                <el-button
                  size="small"
                  @click="handleDismiss(adjustment)"
                >
                  {{ t('common.cancel') }}
                </el-button>
              </div>
            </div>
          </div>
        </div>

        <!-- Empty State -->
        <div class="empty-state" v-else>
          <el-icon :size="32"><Check /></el-icon>
          <p>{{ t('plan.allTasksDone') }}</p>
        </div>
      </div>
    </el-popover>
  </div>
</template>

<style scoped>
.plan-adjustment-notice {
  position: relative;
}

/* Notification Badge */
.notification-badge {
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  padding: var(--spacing-sm);
  cursor: pointer;
  color: var(--text-secondary);
  transition: color var(--transition-fast);
}

.notification-badge:hover {
  color: var(--color-primary);
}

.badge-count {
  position: absolute;
  top: 0;
  right: 0;
  min-width: 18px;
  height: 18px;
  padding: 0 4px;
  background: #ef4444;
  color: white;
  border-radius: 9px;
  font-size: 10px;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
}

.trigger-btn {
  position: relative;
  padding: var(--spacing-sm);
}

.badge-count-small {
  position: absolute;
  top: 2px;
  right: 2px;
  min-width: 14px;
  height: 14px;
  padding: 0 3px;
  background: #ef4444;
  color: white;
  border-radius: 7px;
  font-size: 9px;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
}

/* Adjustment Panel */
.adjustment-panel {
  max-height: 400px;
  overflow-y: auto;
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-bottom: var(--spacing-md);
  border-bottom: 1px solid var(--border-color-light);
  margin-bottom: var(--spacing-md);
}

.panel-header h4 {
  margin: 0;
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  font-size: var(--font-size-md);
  font-weight: 600;
}

/* Loading */
.loading-state {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--spacing-sm);
  padding: var(--spacing-lg);
  color: var(--text-tertiary);
}

.loading-spinner {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

/* Adjustments List */
.adjustments-list {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-md);
}

.adjustment-item {
  display: flex;
  gap: var(--spacing-sm);
  padding: var(--spacing-sm);
  background: var(--bg-tertiary);
  border-radius: var(--radius-md);
  border-left: 3px solid transparent;
}

.adjustment-item.unread {
  border-left-color: var(--color-accent);
  background: rgba(201, 162, 39, 0.05);
}

.adjustment-icon {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--radius-md);
  flex-shrink: 0;
}

.adjustment-content {
  flex: 1;
  min-width: 0;
}

.adjustment-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: var(--spacing-xs);
}

.adjustment-reason {
  font-size: var(--font-size-sm);
  font-weight: 500;
  color: var(--text-primary);
  line-height: 1.3;
}

.adjustment-date {
  font-size: var(--font-size-xs);
  color: var(--text-tertiary);
  white-space: nowrap;
}

.adjustment-details {
  display: flex;
  align-items: center;
  gap: var(--spacing-xs);
  margin-bottom: var(--spacing-sm);
  font-size: var(--font-size-xs);
}

.old-value {
  color: var(--text-tertiary);
  text-decoration: line-through;
}

.new-value {
  color: #10b981;
  font-weight: 600;
}

.adjustment-actions {
  display: flex;
  gap: var(--spacing-xs);
}

.adjustment-actions .el-button {
  padding: 4px 8px;
  font-size: var(--font-size-xs);
}

/* Empty State */
.empty-state {
  text-align: center;
  padding: var(--spacing-lg);
  color: var(--text-tertiary);
}

.empty-state .el-icon {
  color: #10b981;
  margin-bottom: var(--spacing-sm);
}

.empty-state p {
  margin: 0;
  font-size: var(--font-size-sm);
}
</style>
