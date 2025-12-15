<script setup lang="ts">
import { ref, onMounted, nextTick, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { useDialogueStore } from '@/stores/dialogue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { Scenario, CreateScenarioRequest } from '@/types'

const { t } = useI18n()
const dialogueStore = useDialogueStore()

// UI state
const messageInput = ref('')
const chatContainerRef = ref<HTMLElement | null>(null)
const showCreateDialog = ref(false)

// New scenario form
const newScenario = ref<CreateScenarioRequest>({
  name: '',
  description: '',
  category: ''
})

// Language options
const languageOptions = [
  { value: 'en', label: 'English' },
  { value: 'zh', label: '中文' },
  { value: 'ja', label: '日本語' },
  { value: 'ko', label: '한국어' },
  { value: 'fr', label: 'Français' },
  { value: 'de', label: 'Deutsch' },
  { value: 'es', label: 'Español' }
]

// Category options for scenarios
const categoryOptions = computed(() => [
  { value: 'travel', label: t('dialogue.categories.travel') },
  { value: 'business', label: t('dialogue.categories.business') },
  { value: 'daily', label: t('dialogue.categories.daily') },
  { value: 'academic', label: t('dialogue.categories.academic') },
  { value: 'custom', label: t('dialogue.categories.custom') }
])

// Get translated scenario name
function getScenarioName(scenario: Scenario): string {
  if (!scenario.isPreset) return scenario.name
  const key = `dialogue.scenarios.${scenario.name}.name`
  const translated = t(key)
  return translated === key ? scenario.name : translated
}

// Get translated scenario description
function getScenarioDescription(scenario: Scenario): string {
  if (!scenario.isPreset) return scenario.description
  const key = `dialogue.scenarios.${scenario.name}.description`
  const translated = t(key)
  return translated === key ? scenario.description : translated
}

// Current scenario info
const currentScenario = computed(() => {
  if (!dialogueStore.currentSession) return null
  return dialogueStore.getScenarioById(dialogueStore.currentSession.scenarioId)
})

// Scroll chat to bottom
function scrollToBottom() {
  nextTick(() => {
    if (chatContainerRef.value) {
      chatContainerRef.value.scrollTop = chatContainerRef.value.scrollHeight
    }
  })
}

// Handle scenario selection
async function handleSelectScenario(scenario: Scenario) {
  try {
    await dialogueStore.startSession(scenario.id)
    if (dialogueStore.error) {
      ElMessage.error(dialogueStore.error)
    } else {
      ElMessage.success(t('dialogue.sessionStarted'))
    }
  } catch (err: any) {
    ElMessage.error(err.message || t('dialogue.startFailed'))
  }
}

// Handle sending message
async function handleSendMessage() {
  const message = messageInput.value.trim()
  if (!message) {
    ElMessage.warning(t('dialogue.enterMessage'))
    return
  }

  messageInput.value = ''
  scrollToBottom()

  try {
    await dialogueStore.sendMessage(message)
    scrollToBottom()
    if (dialogueStore.error) {
      ElMessage.error(dialogueStore.error)
    }
  } catch (err: any) {
    ElMessage.error(err.message || t('dialogue.sendFailed'))
  }
}

// Handle ending session
async function handleEndSession() {
  try {
    await ElMessageBox.confirm(
      t('dialogue.endConfirmMessage'),
      t('dialogue.endConfirmTitle'),
      {
        confirmButtonText: t('dialogue.endSession'),
        cancelButtonText: t('common.cancel'),
        type: 'warning'
      }
    )

    await dialogueStore.endCurrentSession()
    dialogueStore.clearSession()
    ElMessage.success(t('dialogue.sessionEnded'))
  } catch (err: any) {
    if (err !== 'cancel') {
      ElMessage.error(err.message || t('dialogue.endFailed'))
    }
  }
}

// Handle creating custom scenario
async function handleCreateScenario() {
  if (!newScenario.value.name.trim()) {
    ElMessage.warning(t('dialogue.enterScenarioName'))
    return
  }
  if (!newScenario.value.description.trim()) {
    ElMessage.warning(t('dialogue.enterScenarioDesc'))
    return
  }
  if (!newScenario.value.category) {
    ElMessage.warning(t('dialogue.selectCategory'))
    return
  }

  try {
    const scenario = await dialogueStore.createScenario(newScenario.value)
    if (scenario) {
      ElMessage.success(t('dialogue.scenarioCreated'))
      showCreateDialog.value = false
      newScenario.value = { name: '', description: '', category: '' }
    } else if (dialogueStore.error) {
      ElMessage.error(dialogueStore.error)
    }
  } catch (err: any) {
    ElMessage.error(err.message || t('dialogue.createFailed'))
  }
}

// Format timestamp for display
function formatTime(timestamp: string | number[] | undefined): string {
  if (!timestamp) return ''
  let date: Date
  if (Array.isArray(timestamp)) {
    // Handle LocalDateTime array format [year, month, day, hour, minute, second, nano]
    const [year, month, day, hour = 0, minute = 0] = timestamp
    date = new Date(year, month - 1, day, hour, minute)
  } else {
    date = new Date(timestamp)
  }
  if (isNaN(date.getTime())) return ''
  return date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
}

// Get category label
function getCategoryLabel(category: string): string {
  const key = `dialogue.categories.${category}`
  const translated = t(key)
  return translated === key ? category : translated
}

onMounted(() => {
  dialogueStore.fetchScenarios()
})
</script>

<template>
  <div class="dialogue-page">
    <!-- Scenario Selection View -->
    <div v-if="!dialogueStore.hasActiveSession" class="scenario-selection">
      <div class="page-intro">
        <div class="language-row">
          <span class="label">{{ t('dialogue.targetLang') }}:</span>
          <el-select v-model="dialogueStore.targetLang" class="language-select">
            <el-option v-for="lang in languageOptions" :key="lang.value" :label="lang.label" :value="lang.value" />
          </el-select>
        </div>
        <p>{{ t('dialogue.selectScenarioHint') }}</p>
      </div>

      <el-row :gutter="20" class="scenario-section">
        <!-- Preset Scenarios -->
        <el-col :span="16">
          <el-card>
            <template #header>
              <div class="card-header">
                <span>{{ t('dialogue.presetScenarios') }}</span>
                <el-badge :value="dialogueStore.presetScenarios.length" />
              </div>
            </template>

            <div v-if="dialogueStore.loading" class="loading-container">
              <el-skeleton :rows="3" animated />
            </div>

            <el-row v-else :gutter="16">
              <el-col
                v-for="scenario in dialogueStore.presetScenarios"
                :key="scenario.id"
                :span="12"
              >
                <el-card
                  shadow="hover"
                  class="scenario-card"
                  @click="handleSelectScenario(scenario)"
                >
                  <div class="scenario-icon">
                    <el-icon :size="32"><ChatDotRound /></el-icon>
                  </div>
                  <div class="scenario-info">
                    <h3>{{ getScenarioName(scenario) }}</h3>
                    <p>{{ getScenarioDescription(scenario) }}</p>
                    <el-tag size="small" type="info">
                      {{ getCategoryLabel(scenario.category) }}
                    </el-tag>
                  </div>
                </el-card>
              </el-col>
            </el-row>

            <el-empty
              v-if="!dialogueStore.loading && dialogueStore.presetScenarios.length === 0"
              :description="t('dialogue.noPresetScenarios')"
            />
          </el-card>
        </el-col>

        <!-- Custom Scenarios -->
        <el-col :span="8">
          <el-card>
            <template #header>
              <div class="card-header">
                <span>{{ t('dialogue.customScenarios') }}</span>
                <el-button type="primary" size="small" @click="showCreateDialog = true">
                  {{ t('dialogue.createNew') }}
                </el-button>
              </div>
            </template>

            <el-scrollbar height="300px">
              <div
                v-for="scenario in dialogueStore.customScenarios"
                :key="scenario.id"
                class="custom-scenario-item"
                @click="handleSelectScenario(scenario)"
              >
                <div class="custom-scenario-name">{{ scenario.name }}</div>
                <div class="custom-scenario-desc">{{ scenario.description }}</div>
                <el-tag size="small">{{ getCategoryLabel(scenario.category) }}</el-tag>
              </div>

              <el-empty
                v-if="dialogueStore.customScenarios.length === 0"
                :description="t('dialogue.noCustomScenarios')"
                :image-size="80"
              />
            </el-scrollbar>
          </el-card>
        </el-col>
      </el-row>
    </div>

    <!-- Chat Interface View -->
    <div v-else class="chat-interface">
      <el-card class="chat-card">
        <!-- Chat Header -->
        <template #header>
          <div class="chat-header">
            <div class="chat-info">
              <el-icon><ChatDotRound /></el-icon>
              <span class="chat-title">{{ currentScenario ? getScenarioName(currentScenario) : t('dialogue.title') }}</span>
              <el-tag size="small" type="success">{{ t('dialogue.active') }}</el-tag>
            </div>
            <el-button type="danger" size="small" @click="handleEndSession">
              {{ t('dialogue.endSession') }}
            </el-button>
          </div>
        </template>

        <!-- Messages Container -->
        <div ref="chatContainerRef" class="messages-container">
          <!-- Scenario Description -->
          <div v-if="currentScenario" class="scenario-intro">
            <el-alert
              :title="getScenarioName(currentScenario)"
              type="info"
              :closable="false"
              show-icon
            >
              {{ getScenarioDescription(currentScenario) }}
            </el-alert>
          </div>

          <!-- Message Bubbles -->
          <div
            v-for="(msg, index) in dialogueStore.messages"
            :key="index"
            :class="['message-wrapper', msg.role]"
          >
            <div :class="['message-bubble', msg.role]">
              <div class="message-content">{{ msg.content }}</div>
              <div class="message-time">{{ formatTime(msg.timestamp) }}</div>
            </div>
          </div>

          <!-- Typing Indicator -->
          <div v-if="dialogueStore.sendingMessage" class="message-wrapper assistant">
            <div class="message-bubble assistant typing">
              <span class="typing-dot"></span>
              <span class="typing-dot"></span>
              <span class="typing-dot"></span>
            </div>
          </div>

          <!-- Empty State -->
          <el-empty
            v-if="dialogueStore.messages.length === 0 && !dialogueStore.sendingMessage"
            :description="t('dialogue.startHint')"
            :image-size="100"
          />
        </div>

        <!-- Message Input -->
        <div class="message-input-container">
          <el-input
            v-model="messageInput"
            type="textarea"
            :rows="2"
            :placeholder="t('dialogue.typeMessage')"
            :disabled="dialogueStore.sendingMessage"
            @keyup.enter.ctrl="handleSendMessage"
          />
          <el-button
            type="primary"
            :loading="dialogueStore.sendingMessage"
            :disabled="!messageInput.trim()"
            @click="handleSendMessage"
          >
            <el-icon><Promotion /></el-icon>
            {{ t('dialogue.send') }}
          </el-button>
        </div>
      </el-card>
    </div>

    <!-- Create Scenario Dialog -->
    <el-dialog
      v-model="showCreateDialog"
      :title="t('dialogue.createScenario')"
      width="500px"
    >
      <el-form :model="newScenario" label-width="100px">
        <el-form-item :label="t('dialogue.scenarioName')" required>
          <el-input
            v-model="newScenario.name"
            :placeholder="t('dialogue.enterScenarioName')"
            maxlength="50"
            show-word-limit
          />
        </el-form-item>
        <el-form-item :label="t('dialogue.category')" required>
          <el-select v-model="newScenario.category" :placeholder="t('dialogue.selectCategory')" style="width: 100%">
            <el-option
              v-for="cat in categoryOptions"
              :key="cat.value"
              :label="cat.label"
              :value="cat.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('dialogue.description')" required>
          <el-input
            v-model="newScenario.description"
            type="textarea"
            :rows="3"
            :placeholder="t('dialogue.enterScenarioDesc')"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreateDialog = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="dialogueStore.loading" @click="handleCreateScenario">
          {{ t('dialogue.create') }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script lang="ts">
import { ChatDotRound, Promotion } from '@element-plus/icons-vue'

export default {
  components: { ChatDotRound, Promotion }
}
</script>

<style scoped>
.dialogue-page {
  height: calc(100vh - 140px);
}

.page-intro {
  text-align: center;
  margin-bottom: 24px;
  color: var(--text-secondary);
}

.language-row {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  margin-bottom: 16px;
}

.language-row .label {
  font-weight: 500;
  color: var(--text-primary);
}

.language-select {
  width: 150px;
}

.scenario-section {
  margin-top: 20px;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-weight: 600;
}

.loading-container {
  padding: 20px;
}

.scenario-card {
  cursor: pointer;
  margin-bottom: 16px;
  transition: all 0.3s;
  display: flex;
  align-items: flex-start;
  padding: 16px;
}

.scenario-card:hover {
  border-color: var(--color-accent);
  transform: translateY(-2px);
}

.scenario-icon {
  margin-right: 16px;
  color: var(--color-accent);
  flex-shrink: 0;
}

.scenario-info h3 {
  margin: 0 0 8px 0;
  font-size: 16px;
  color: var(--text-primary);
}

.scenario-info p {
  margin: 0 0 8px 0;
  font-size: 14px;
  color: var(--text-secondary);
  line-height: 1.5;
}

.custom-scenario-item {
  padding: 12px;
  border-bottom: 1px solid var(--border-color-light);
  cursor: pointer;
  transition: background-color 0.3s;
}

.custom-scenario-item:hover {
  background-color: var(--bg-tertiary);
}

.custom-scenario-name {
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 4px;
}

.custom-scenario-desc {
  font-size: 13px;
  color: var(--text-muted);
  margin-bottom: 8px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* Chat Interface Styles */
.chat-interface {
  height: 100%;
}

.chat-card {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.chat-card :deep(.el-card__body) {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  padding: 0;
}

.chat-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.chat-info {
  display: flex;
  align-items: center;
  gap: 8px;
}

.chat-title {
  font-weight: 600;
  font-size: 16px;
}

.messages-container {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  background-color: var(--bg-tertiary);
}

.scenario-intro {
  margin-bottom: 20px;
}

.message-wrapper {
  display: flex;
  margin-bottom: 16px;
}

.message-wrapper.user {
  justify-content: flex-end;
}

.message-wrapper.assistant {
  justify-content: flex-start;
}

.message-bubble {
  max-width: 70%;
  padding: 12px 16px;
  border-radius: 12px;
  position: relative;
}

.message-bubble.user {
  background-color: var(--color-accent);
  color: var(--color-primary);
  border-bottom-right-radius: 4px;
}

.message-bubble.assistant {
  background-color: var(--bg-secondary);
  color: var(--text-primary);
  border-bottom-left-radius: 4px;
  box-shadow: var(--shadow-sm);
}

.message-content {
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
}

.message-time {
  font-size: 11px;
  margin-top: 4px;
  opacity: 0.7;
}

.message-bubble.user .message-time {
  text-align: right;
}

/* Typing Indicator */
.message-bubble.typing {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 16px 20px;
}

.typing-dot {
  width: 8px;
  height: 8px;
  background-color: var(--text-muted);
  border-radius: 50%;
  animation: typing 1.4s infinite ease-in-out;
}

.typing-dot:nth-child(1) { animation-delay: 0s; }
.typing-dot:nth-child(2) { animation-delay: 0.2s; }
.typing-dot:nth-child(3) { animation-delay: 0.4s; }

@keyframes typing {
  0%, 60%, 100% { transform: translateY(0); }
  30% { transform: translateY(-8px); }
}

/* Message Input */
.message-input-container {
  display: flex;
  gap: 12px;
  padding: 16px 20px;
  border-top: 1px solid var(--border-color-light);
  background-color: var(--bg-secondary);
}

.message-input-container .el-input {
  flex: 1;
}

.message-input-container .el-button {
  align-self: flex-end;
}
</style>
