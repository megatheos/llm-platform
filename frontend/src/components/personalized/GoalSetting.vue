<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, type FormInstance, type FormRules, ElButton, ElInputNumber, ElSelect, ElOption, ElCheckboxGroup, ElCheckbox } from 'element-plus'
import {
  Aim,
  Clock,
  Calendar,
  Edit
} from '@element-plus/icons-vue'
import type { GoalSettingData, StudyGoal } from '@/types/personalized'

const { t } = useI18n()

// Props
interface Props {
  settings?: GoalSettingData
  goals?: StudyGoal[]
}

const props = withDefaults(defineProps<Props>(), {
  settings: () => ({
    dailyWordTarget: 20,
    dailyQuizTarget: 5,
    dailyDialogueTarget: 1,
    weeklyStudyHours: 10,
    preferredStudyTime: 'any',
    focusAreas: [],
    difficultyLevel: 'adaptive'
  }),
  goals: () => []
})

// Emits
const emit = defineEmits<{
  (e: 'update', settings: GoalSettingData): void
}>()

// State
const loading = ref(false)
const saving = ref(false)
const isEditing = ref(false)
const formRef = ref<FormInstance>()

const formData = reactive<GoalSettingData>({ ...props.settings })

const timeOptions = [
  { value: 'morning', label: t('plan.times.morning'), icon: '🌅' },
  { value: 'afternoon', label: t('plan.times.afternoon'), icon: '☀️' },
  { value: 'evening', label: t('plan.times.evening'), icon: '🌙' },
  { value: 'any', label: t('plan.times.any'), icon: '⏰' }
]

const difficultyOptions = [
  { value: 'easy', label: t('plan.difficulties.easy') },
  { value: 'medium', label: t('plan.difficulties.medium') },
  { value: 'hard', label: t('plan.difficulties.hard') },
  { value: 'adaptive', label: t('plan.difficulties.adaptive') }
]

const focusAreaOptions = [
  { value: 'vocabulary', label: t('profile.categories.vocabulary') },
  { value: 'grammar', label: t('profile.categories.grammar') },
  { value: 'listening', label: t('profile.categories.listening') },
  { value: 'speaking', label: t('profile.categories.speaking') },
  { value: 'reading', label: t('profile.categories.reading') },
  { value: 'writing', label: t('profile.categories.writing') }
]

// Computed
const dailyTargetsTotal = computed(() => {
  return formData.dailyWordTarget + formData.dailyQuizTarget + formData.dailyDialogueTarget
})

const difficultyDescription = computed(() => {
  const map: Record<string, string> = {
    easy: t('quiz.easyDesc'),
    medium: t('quiz.mediumDesc'),
    hard: t('quiz.hardDesc'),
    adaptive: t('plan.difficulties.adaptive')
  }
  return map[formData.difficultyLevel] || ''
})

// Methods
function handleSave() {
  saving.value = true
  setTimeout(() => {
    emit('update', { ...formData })
    ElMessage.success(t('common.save'))
    isEditing.value = false
    saving.value = false
  }, 500)
}

function handleCancel() {
  Object.assign(formData, props.settings)
  isEditing.value = false
}

function handleEdit() {
  isEditing.value = true
}
</script>

<template>
  <div class="goal-setting">
    <!-- Goals Progress -->
    <div v-if="goals.length > 0" class="goals-section">
      <h3 class="section-title">
        <el-icon><Aim /></el-icon>
        {{ t('plan.goals') }}
      </h3>
      <div class="goals-list">
        <div v-for="goal in goals" :key="goal.id" class="goal-item">
          <div class="goal-header">
            <span class="goal-type">{{ goal.type }}</span>
            <span class="goal-progress">{{ goal.currentValue }}/{{ goal.targetValue }}</span>
          </div>
          <el-progress
            :percentage="Math.min(100, (goal.currentValue / goal.targetValue) * 100)"
            :stroke-width="8"
            :show-text="false"
            color="#3b82f6"
          />
        </div>
      </div>
    </div>

    <!-- Goal Settings -->
    <div class="settings-section">
      <div class="settings-header">
        <h3 class="section-title">
          <el-icon><Calendar /></el-icon>
          {{ t('plan.goalSetting') }}
        </h3>
        <el-button v-if="!isEditing" link @click="handleEdit">
          <el-icon><Edit /></el-icon>
          {{ t('common.edit') }}
        </el-button>
      </div>

      <div v-if="!isEditing" class="settings-display">
        <div class="setting-item">
          <span class="setting-label">{{ t('plan.dailyWordTarget') }}</span>
          <span class="setting-value">{{ formData.dailyWordTarget }}</span>
        </div>
        <div class="setting-item">
          <span class="setting-label">{{ t('plan.dailyQuizTarget') }}</span>
          <span class="setting-value">{{ formData.dailyQuizTarget }}</span>
        </div>
        <div class="setting-item">
          <span class="setting-label">{{ t('plan.dailyDialogueTarget') }}</span>
          <span class="setting-value">{{ formData.dailyDialogueTarget }}</span>
        </div>
        <div class="setting-item">
          <span class="setting-label">{{ t('plan.weeklyStudyHours') }}</span>
          <span class="setting-value">{{ formData.weeklyStudyHours }}h</span>
        </div>
        <div class="setting-item">
          <span class="setting-label">{{ t('plan.preferredTime') }}</span>
          <span class="setting-value">{{ timeOptions.find(o => o.value === formData.preferredStudyTime)?.label }}</span>
        </div>
        <div class="setting-item">
          <span class="setting-label">{{ t('plan.difficulty') }}</span>
          <span class="setting-value">{{ difficultyOptions.find(o => o.value === formData.difficultyLevel)?.label }}</span>
        </div>
      </div>

      <div v-else class="settings-form">
        <div class="form-row">
          <label>{{ t('plan.dailyWordTarget') }}</label>
          <el-input-number v-model="formData.dailyWordTarget" :min="1" :max="500" size="small" />
        </div>
        <div class="form-row">
          <label>{{ t('plan.dailyQuizTarget') }}</label>
          <el-input-number v-model="formData.dailyQuizTarget" :min="1" :max="50" size="small" />
        </div>
        <div class="form-row">
          <label>{{ t('plan.dailyDialogueTarget') }}</label>
          <el-input-number v-model="formData.dailyDialogueTarget" :min="1" :max="20" size="small" />
        </div>
        <div class="form-row">
          <label>{{ t('plan.weeklyStudyHours') }}</label>
          <el-input-number v-model="formData.weeklyStudyHours" :min="1" :max="168" size="small" />
        </div>
        <div class="form-row">
          <label>{{ t('plan.preferredTime') }}</label>
          <el-select v-model="formData.preferredStudyTime" size="small" style="width: 140px">
            <el-option v-for="opt in timeOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
        </div>
        <div class="form-row">
          <label>{{ t('plan.difficulty') }}</label>
          <el-select v-model="formData.difficultyLevel" size="small" style="width: 140px">
            <el-option v-for="opt in difficultyOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
        </div>
        <div class="form-actions">
          <el-button @click="handleCancel">{{ t('common.cancel') }}</el-button>
          <el-button type="primary" :loading="saving" @click="handleSave">
            {{ t('common.save') }}
          </el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.goal-setting {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.goals-section,
.settings-section {
  background: var(--bg-secondary);
  border-radius: var(--radius-lg);
  padding: 20px;
  border: 1px solid var(--border-color-light);
}

.section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: var(--font-size-md);
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 16px;
}

.settings-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.settings-display {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}

.setting-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px;
  background: var(--bg-tertiary);
  border-radius: var(--radius-md);
}

.setting-label {
  font-size: var(--font-size-sm);
  color: var(--text-muted);
}

.setting-value {
  font-size: var(--font-size-sm);
  font-weight: 600;
  color: var(--text-primary);
}

.goals-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.goal-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.goal-header {
  display: flex;
  justify-content: space-between;
  font-size: var(--font-size-sm);
}

.goal-type {
  text-transform: capitalize;
  color: var(--text-secondary);
}

.goal-progress {
  color: var(--text-muted);
}

.settings-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.form-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.form-row label {
  font-size: var(--font-size-sm);
  color: var(--text-muted);
  min-width: 120px;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 8px;
}
</style>