<script setup lang="ts">
import { ref, onMounted, nextTick, computed, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useDialogueStore } from '@/stores/dialogue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { Scenario, CreateScenarioRequest } from '@/types'
import {
  ChatDotRound,
  Promotion,
  Plus,
  Close,
  Check,
  Clock,
  Star,
  Edit,
  Delete,
  CopyDocument,
  VideoCamera,
  Headset
} from '@element-plus/icons-vue'

const { t } = useI18n()
const dialogueStore = useDialogueStore()

// UI state
const messageInput = ref('')
const chatContainerRef = ref<HTMLElement | null>(null)
const showCreateDialog = ref(false)
const showQuickReplies = ref(false)

// New scenario form
const newScenario = ref<CreateScenarioRequest>({
  name: '',
  description: '',
  category: ''
})

// Quick reply suggestions
const quickReplies = [
  'Can you repeat that?',
  'What does this mean?',
  'Let me think about it',
  'I understand now',
  'Could you speak more slowly?'
]

// Language options with flags
const languageOptions = [
  { value: 'en', label: '🇺🇸 English', flag: '🇺🇸' },
  { value: 'zh', label: '🇨🇳 中文', flag: '🇨🇳' },
  { value: 'ja', label: '🇯🇵 日本語', flag: '🇯🇵' },
  { value: 'ko', label: '🇰🇷 한국어', flag: '🇰🇷' },
  { value: 'fr', label: '🇫🇷 Français', flag: '🇫🇷' },
  { value: 'de', label: '🇩🇪 Deutsch', flag: '🇩🇪' },
  { value: 'es', label: '🇪🇸 Español', flag: '🇪🇸' }
]

// Category options
const categoryOptions = computed(() => [
  { value: 'travel', label: t('dialogue.categories.travel'), icon: '✈️' },
  { value: 'business', label: t('dialogue.categories.business'), icon: '💼' },
  { value: 'daily', label: t('dialogue.categories.daily'), icon: '🏠' },
  { value: 'academic', label: t('dialogue.categories.academic'), icon: '📚' },
  { value: 'custom', label: t('dialogue.categories.custom'), icon: '🎯' }
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

// Get category info
function getCategoryInfo(category: string) {
  return categoryOptions.value.find(c => c.value === category) || { icon: '📁', label: category }
}

// Scroll chat to bottom with animation
function scrollToBottom(animate = true) {
  nextTick(() => {
    if (chatContainerRef.value) {
      if (animate) {
        chatContainerRef.value.style.scrollBehavior = 'smooth'
      }
      chatContainerRef.value.scrollTop = chatContainerRef.value.scrollHeight
      if (animate) {
        setTimeout(() => {
          if (chatContainerRef.value) {
            chatContainerRef.value.style.scrollBehavior = 'auto'
          }
        }, 300)
      }
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
  showQuickReplies.value = false
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

// Format timestamp
function formatTime(timestamp: string | number[] | undefined): string {
  if (!timestamp) return ''
  let date: Date
  if (Array.isArray(timestamp)) {
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

// Insert quick reply
function insertQuickReply(reply: string) {
  messageInput.value = reply
  showQuickReplies.value = false
}

// Copy message to clipboard
async function copyMessage(content: string) {
  try {
    await navigator.clipboard.writeText(content)
    ElMessage.success('Copied to clipboard')
  } catch {
    ElMessage.error('Failed to copy')
  }
}

// Watch for new messages to scroll
watch(() => dialogueStore.messages.length, () => {
  scrollToBottom()
})

onMounted(() => {
  dialogueStore.fetchScenarios()
})
</script>

<template>
  <div class="dialogue-page">
    <!-- Scenario Selection View -->
    <div v-if="!dialogueStore.hasActiveSession" class="scenario-selection">
      <div class="page-intro">
        <div class="intro-content">
          <el-icon class="intro-icon"><ChatDotRound /></el-icon>
          <h2>Practice Conversation</h2>
          <p>{{ t('dialogue.selectScenarioHint') }}</p>
        </div>
        <div class="language-row">
          <span class="label">{{ t('dialogue.targetLang') }}:</span>
          <div class="lang-selector">
            <span class="lang-flag">{{ languageOptions.find(l => l.value === dialogueStore.targetLang)?.flag || '🌐' }}</span>
            <el-select v-model="dialogueStore.targetLang" class="language-select">
              <el-option v-for="lang in languageOptions" :key="lang.value" :label="lang.label" :value="lang.value" />
            </el-select>
          </div>
        </div>
      </div>

      <el-row :gutter="24" class="scenario-section">
        <el-col :xs="24" :lg="16">
          <el-card class="scenarios-card" shadow="hover">
            <template #header>
              <div class="card-header">
                <div class="header-left">
                  <el-icon><Star /></el-icon>
                  <span>{{ t('dialogue.presetScenarios') }}</span>
                </div>
                <el-badge :value="dialogueStore.presetScenarios.length" class="scenario-count" />
              </div>
            </template>

            <div v-if="dialogueStore.loading" class="loading-container">
              <el-skeleton v-for="i in 4" :key="i" :rows="2" animated />
            </div>

              <div v-else class="scenarios-grid">
                <div
                  v-for="(scenario, index) in dialogueStore.presetScenarios"
                  :key="scenario.id"
                  class="scenario-card"
                  :style="{ '--delay': `${index * 0.1}s` }"
                  @click="handleSelectScenario(scenario)"
                >
                  <div class="scenario-icon">
                    <span class="category-icon">{{ getCategoryInfo(scenario.category).icon }}</span>
                  </div>
                  <div class="scenario-info">
                    <h3>{{ getScenarioName(scenario) }}</h3>
                    <p>{{ getScenarioDescription(scenario) }}</p>
                    <div class="scenario-footer">
                      <el-tag size="small" type="info" effect="plain">
                        {{ getCategoryLabel(scenario.category) }}
                      </el-tag>
                      <div class="scenario-meta">
                        <el-icon><Clock /></el-icon>
                        <span>~10 min</span>
                      </div>
                    </div>
                  </div>
                  <el-icon class="scenario-arrow"><Promotion /></el-icon>
                </div>
              </div>

            <el-empty
              v-if="!dialogueStore.loading && dialogueStore.presetScenarios.length === 0"
              :description="t('dialogue.noPresetScenarios')"
            />
          </el-card>
        </el-col>

        <el-col :xs="24" :lg="8">
          <el-card class="custom-card" shadow="hover">
            <template #header>
              <div class="card-header">
                <div class="header-left">
                  <el-icon><Edit /></el-icon>
                  <span>{{ t('dialogue.customScenarios') }}</span>
                </div>
                <el-button type="primary" size="small" circle @click="showCreateDialog = true">
                  <el-icon><Plus /></el-icon>
                </el-button>
              </div>
            </template>

            <el-scrollbar height="350px">
              <div
                v-for="scenario in dialogueStore.customScenarios"
                :key="scenario.id"
                class="custom-scenario-item"
                @click="handleSelectScenario(scenario)"
              >
                <div class="custom-scenario-icon">
                  <span>{{ getCategoryInfo(scenario.category).icon }}</span>
                </div>
                <div class="custom-scenario-content">
                  <div class="custom-scenario-name">{{ scenario.name }}</div>
                  <div class="custom-scenario-desc">{{ scenario.description }}</div>
                  <el-tag size="small">{{ getCategoryLabel(scenario.category) }}</el-tag>
                </div>
                <div class="custom-scenario-actions">
                  <el-icon class="action-icon edit"><Edit /></el-icon>
                  <el-icon class="action-icon delete"><Delete /></el-icon>
                </div>
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
      <el-card class="chat-card" shadow="hover">
        <template #header>
          <div class="chat-header">
            <div class="chat-info">
              <div class="chat-avatar">
                <el-icon><ChatDotRound /></el-icon>
              </div>
              <div class="chat-details">
                <span class="chat-title">{{ currentScenario ? getScenarioName(currentScenario) : t('dialogue.title') }}</span>
                <div class="chat-status">
                  <span class="status-dot"></span>
                  <span>{{ t('dialogue.active') }}</span>
                </div>
              </div>
              <el-tag size="small" type="success" effect="light">{{ getCategoryLabel(currentScenario?.category || '') }}</el-tag>
            </div>
            <div class="chat-actions">
              <el-button circle size="small">
                <el-icon><VideoCamera /></el-icon>
              </el-button>
              <el-button circle size="small">
                <el-icon><Headset /></el-icon>
              </el-button>
              <el-button type="danger" size="small" @click="handleEndSession">
                <el-icon><Close /></el-icon>
                {{ t('dialogue.endSession') }}
              </el-button>
            </div>
          </div>
        </template>

        <div ref="chatContainerRef" class="messages-container">
          <div v-if="currentScenario" class="scenario-intro">
            <div class="intro-message">
              <div class="intro-avatar">
                <el-icon><ChatDotRound /></el-icon>
              </div>
              <div class="intro-content">
                <div class="intro-name">AI Assistant</div>
                <el-alert
                  :title="getScenarioName(currentScenario)"
                  type="info"
                  :closable="false"
                  show-icon
                >
                  {{ getScenarioDescription(currentScenario) }}
                </el-alert>
              </div>
            </div>
          </div>

          <transition-group name="message" tag="div" class="messages-list">
            <div
              v-for="(msg, index) in dialogueStore.messages"
              :key="index"
              :class="['message-wrapper', msg.role]"
            >
              <div v-if="msg.role === 'assistant'" class="message-avatar">
                <el-icon><ChatDotRound /></el-icon>
              </div>
              <div class="message-bubble" :class="msg.role">
                <div class="message-content">{{ msg.content }}</div>
                <div class="message-footer">
                  <div class="message-time">{{ formatTime(msg.timestamp) }}</div>
                  <div v-if="msg.role === 'user'" class="message-status">
                    <el-icon><Check /></el-icon>
                  </div>
                </div>
                <div class="message-actions">
                  <el-button size="small" circle @click="copyMessage(msg.content)">
                    <el-icon><CopyDocument /></el-icon>
                  </el-button>
                </div>
              </div>
            </div>
          </transition-group>

          <div v-if="dialogueStore.sendingMessage" class="message-wrapper assistant">
            <div class="message-avatar typing">
              <el-icon><ChatDotRound /></el-icon>
            </div>
            <div class="message-bubble assistant typing">
              <div class="typing-dots">
                <span></span>
                <span></span>
                <span></span>
              </div>
            </div>
          </div>

          <el-empty
            v-if="dialogueStore.messages.length === 0 && !dialogueStore.sendingMessage"
            :description="t('dialogue.startHint')"
            :image-size="100"
          />
        </div>

        <transition name="slide-up">
          <div v-if="showQuickReplies && quickReplies.length > 0" class="quick-replies">
            <div
              v-for="reply in quickReplies"
              :key="reply"
              class="quick-reply-chip"
              @click="insertQuickReply(reply)"
            >
              {{ reply }}
            </div>
          </div>
        </transition>

        <div class="message-input-container">
          <el-button 
            circle 
            size="small"
            @click="showQuickReplies = !showQuickReplies"
            :class="{ active: showQuickReplies }"
          >
            <el-icon><ChatDotRound /></el-icon>
          </el-button>
          <el-input
            v-model="messageInput"
            type="textarea"
            :rows="2"
            :placeholder="t('dialogue.typeMessage')"
            :disabled="dialogueStore.sendingMessage"
            @keyup.enter.ctrl="handleSendMessage"
            class="message-input"
            resize="none"
          />
          <el-button
            type="primary"
            :loading="dialogueStore.sendingMessage"
            :disabled="!messageInput.trim()"
            @click="handleSendMessage"
            class="send-btn"
          >
            <el-icon><Promotion /></el-icon>
          </el-button>
        </div>
      </el-card>
    </div>

    <!-- Create Scenario Dialog -->
    <el-dialog
      v-model="showCreateDialog"
      :title="t('dialogue.createScenario')"
      width="500px"
      class="create-dialog"
    >
      <el-form :model="newScenario" label-position="top">
        <el-form-item :label="t('dialogue.scenarioName')" required>
          <el-input
            v-model="newScenario.name"
            :placeholder="t('dialogue.enterScenarioName')"
            maxlength="50"
            show-word-limit
          />
        </el-form-item>
        
        <el-form-item :label="t('dialogue.category')" required>
          <div class="category-grid">
            <div
              v-for="cat in categoryOptions"
              :key="cat.value"
              :class="['category-option', { active: newScenario.category === cat.value }]"
              @click="newScenario.category = cat.value"
            >
              <span class="category-icon">{{ cat.icon }}</span>
              <span class="category-label">{{ cat.label }}</span>
              <el-icon v-if="newScenario.category === cat.value" class="check-icon"><Check /></el-icon>
            </div>
          </div>
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
          <el-icon><Plus /></el-icon>
          {{ t('dialogue.create') }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.dialogue-page {
  height: calc(100vh - 140px);
}

.scenario-selection {
  height: 100%;
}

.page-intro {
  text-align: center;
  margin-bottom: 32px;
  padding: 32px;
  background: var(--bg-secondary);
  border-radius: var(--radius-xl);
  position: relative;
  overflow: hidden;
}

.page-intro::before {
  content: '';
  position: absolute;
  top: -50%;
  left: -20%;
  width: 300px;
  height: 300px;
  background: rgba(59, 130, 246, 0.1);
  border-radius: 50%;
}

.intro-content {
  position: relative;
  z-index: 1;
}

.intro-icon {
  font-size: 48px;
  color: var(--color-primary);
  margin-bottom: 16px;
}

.intro-content h2 {
  font-size: var(--font-size-2xl);
  font-weight: 700;
  color: var(--text-primary);
  margin: 0 0 8px 0;
}

.intro-content p {
  color: var(--text-secondary);
  font-size: var(--font-size-md);
  margin: 0;
}

.language-row {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  margin-top: 24px;
  position: relative;
  z-index: 1;
}

.language-row .label {
  font-weight: 500;
  color: var(--text-primary);
}

.lang-selector {
  display: flex;
  align-items: center;
  gap: 8px;
}

.lang-flag {
  font-size: 24px;
}

.language-select {
  width: 160px;
}

.scenario-section {
  margin-top: 24px;
}

.scenarios-card,
.custom-card {
  height: 100%;
  transition: all 0.3s ease;
}

.scenarios-card:hover,
.custom-card:hover {
  box-shadow: var(--shadow-md);
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
}

.scenario-count {
  margin-left: 8px;
}

.loading-container {
  padding: 20px;
}

.scenarios-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}

.scenario-card {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  padding: 20px;
  background: var(--bg-tertiary);
  border-radius: var(--radius-lg);
  cursor: pointer;
  transition: all 0.4s cubic-bezier(0.175, 0.885, 0.32, 1.275);
  border: 1px solid var(--border-color-light);
  position: relative;
  overflow: hidden;
  animation: slideUp 0.5s ease-out backwards;
  animation-delay: var(--delay);
}

@keyframes slideUp {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.scenario-card:hover {
  transform: translateY(-4px) scale(1.02);
  box-shadow: 0 12px 24px rgba(0, 0, 0, 0.1);
  border-color: var(--color-accent);
}

.scenario-icon {
  width: 48px;
  height: 48px;
  border-radius: var(--radius-md);
  background: var(--bg-secondary);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  font-size: 24px;
  position: relative;
  z-index: 1;
  transition: transform 0.3s;
}

.scenario-card:hover .scenario-icon {
  transform: scale(1.1) rotate(5deg);
}

.scenario-info {
  flex: 1;
  position: relative;
  z-index: 1;
}

.scenario-info h3 {
  margin: 0 0 8px 0;
  font-size: var(--font-size-md);
  font-weight: 600;
  color: var(--text-primary);
}

.scenario-info p {
  margin: 0 0 12px 0;
  font-size: var(--font-size-sm);
  color: var(--text-secondary);
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.scenario-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.scenario-meta {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: var(--font-size-xs);
  color: var(--text-muted);
}

.scenario-arrow {
  color: var(--text-muted);
  transition: all 0.3s;
  opacity: 0;
  transform: translateX(-10px);
}

.scenario-card:hover .scenario-arrow {
  opacity: 1;
  transform: translateX(0);
  color: var(--color-accent);
}

.custom-scenario-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px;
  border-bottom: 1px solid var(--border-color-light);
  cursor: pointer;
  transition: all 0.3s;
}

.custom-scenario-item:hover {
  background-color: var(--bg-tertiary);
}

.custom-scenario-item:hover .custom-scenario-actions {
  opacity: 1;
}

.custom-scenario-icon {
  width: 40px;
  height: 40px;
  border-radius: var(--radius-md);
  background: var(--bg-secondary);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  flex-shrink: 0;
}

.custom-scenario-content {
  flex: 1;
  min-width: 0;
}

.custom-scenario-name {
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 4px;
}

.custom-scenario-desc {
  font-size: var(--font-size-sm);
  color: var(--text-muted);
  margin-bottom: 8px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.custom-scenario-actions {
  display: flex;
  gap: 8px;
  opacity: 0;
  transition: opacity 0.3s;
}

.action-icon {
  color: var(--text-muted);
  cursor: pointer;
  transition: color 0.3s;
}

.action-icon:hover {
  color: var(--color-primary);
}

.action-icon.delete:hover {
  color: #f56c6c;
}

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
  padding: 8px 0;
}

.chat-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.chat-avatar {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  background: var(--color-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 20px;
}

.chat-details {
  display: flex;
  flex-direction: column;
}

.chat-title {
  font-weight: 600;
  font-size: var(--font-size-md);
}

.chat-status {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: var(--font-size-xs);
  color: #67c23a;
}

.status-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #67c23a;
  animation: pulse-dot 2s infinite;
}

@keyframes pulse-dot {
  0%, 100% { transform: scale(1); opacity: 1; }
  50% { transform: scale(1.2); opacity: 0.7; }
}

.chat-actions {
  display: flex;
  gap: 8px;
}

.messages-container {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
  background-color: var(--bg-tertiary);
}

.messages-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.message-enter-active {
  animation: messageSlideIn 0.3s ease-out;
}

.message-leave-active {
  animation: messageSlideOut 0.3s ease-in;
}

@keyframes messageSlideIn {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes messageSlideOut {
  from {
    opacity: 1;
    transform: translateY(0);
  }
  to {
    opacity: 0;
    transform: translateY(20px);
  }
}

.scenario-intro {
  margin-bottom: 24px;
}

.intro-message {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}

.intro-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: var(--color-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  flex-shrink: 0;
}

.intro-name {
  font-size: var(--font-size-sm);
  font-weight: 600;
  margin-bottom: 8px;
  color: var(--text-primary);
}

.message-wrapper {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}

.message-wrapper.user {
  flex-direction: row-reverse;
}

.message-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: var(--color-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  flex-shrink: 0;
}

.message-avatar.typing {
  animation: avatar-pulse 1s infinite;
}

@keyframes avatar-pulse {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.05); }
}

.message-bubble {
  max-width: 70%;
  padding: 14px 18px;
  border-radius: 16px;
  position: relative;
  transition: all 0.3s;
}

.message-bubble.user {
  background: var(--color-primary);
  color: white;
  border-bottom-right-radius: 4px;
}

.message-bubble.assistant {
  background: var(--bg-secondary);
  color: var(--text-primary);
  border-bottom-left-radius: 4px;
  box-shadow: var(--shadow-sm);
}

.message-bubble:hover {
  transform: translateY(-2px);
}

.message-content {
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
}

.message-footer {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 8px;
}

.message-time {
  font-size: 11px;
  opacity: 0.7;
}

.message-bubble.user .message-time {
  text-align: right;
}

.message-status {
  display: flex;
  align-items: center;
  font-size: 12px;
  opacity: 0.8;
}

.message-actions {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  right: -40px;
  display: flex;
  gap: 4px;
  opacity: 0;
  transition: opacity 0.3s;
}

.message-bubble:hover .message-actions {
  opacity: 1;
}

.typing-dots {
  display: flex;
  gap: 4px;
}

.typing-dots span {
  width: 8px;
  height: 8px;
  background: var(--text-muted);
  border-radius: 50%;
  animation: typing-dot 1.4s infinite ease-in-out;
}

.typing-dots span:nth-child(1) { animation-delay: 0s; }
.typing-dots span:nth-child(2) { animation-delay: 0.2s; }
.typing-dots span:nth-child(3) { animation-delay: 0.4s; }

@keyframes typing-dot {
  0%, 60%, 100% { transform: translateY(0); }
  30% { transform: translateY(-8px); }
}

.quick-replies {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  padding: 12px 20px;
  border-top: 1px solid var(--border-color-light);
  background: var(--bg-secondary);
}

.quick-reply-chip {
  padding: 8px 16px;
  background: var(--bg-tertiary);
  border-radius: 20px;
  font-size: var(--font-size-sm);
  color: var(--text-primary);
  cursor: pointer;
  transition: all 0.3s;
  border: 1px solid var(--border-color-light);
}

.quick-reply-chip:hover {
  background: var(--color-primary);
  color: white;
  border-color: var(--color-primary);
}

.slide-up-enter-active,
.slide-up-leave-active {
  transition: all 0.3s ease;
}

.slide-up-enter-from,
.slide-up-leave-to {
  opacity: 0;
  transform: translateY(20px);
}

.message-input-container {
  display: flex;
  align-items: flex-end;
  gap: 12px;
  padding: 16px 20px;
  border-top: 1px solid var(--border-color-light);
  background-color: var(--bg-secondary);
}

.message-input-container .el-button {
  flex-shrink: 0;
}

.message-input-container .el-button.active {
  background: var(--color-primary);
  color: white;
  border-color: var(--color-primary);
}

.message-input {
  flex: 1;
}

.send-btn {
  flex-shrink: 0;
}

.create-dialog :deep(.el-dialog__body) {
  padding-bottom: 0;
}

.category-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 12px;
}

.category-option {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 16px 8px;
  background: var(--bg-tertiary);
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: all 0.3s;
  border: 2px solid transparent;
  position: relative;
}

.category-option:hover {
  background: var(--bg-secondary);
}

.category-option.active {
  border-color: var(--color-primary);
  background: rgba(59, 130, 246, 0.1);
}

.category-option .category-icon {
  font-size: 24px;
}

.category-option .category-label {
  font-size: var(--font-size-xs);
  color: var(--text-secondary);
  text-align: center;
}

.category-option.active .category-label {
  color: var(--color-primary);
}

.check-icon {
  position: absolute;
  top: 4px;
  right: 4px;
  color: var(--color-primary);
  font-size: 14px;
}

@media (max-width: 1200px) {
  .scenarios-grid {
    grid-template-columns: 1fr;
  }
  
  .category-grid {
    grid-template-columns: repeat(3, 1fr);
  }
}

@media (max-width: 768px) {
  .dialogue-page {
    height: auto;
    min-height: calc(100vh - 140px);
  }
  
  .messages-container {
    padding: 16px;
  }
  
  .message-bubble {
    max-width: 85%;
  }
  
  .message-actions {
    display: none;
  }
  
  .chat-header {
    flex-direction: column;
    gap: 12px;
    align-items: flex-start;
  }
  
  .chat-actions {
    width: 100%;
    justify-content: flex-end;
  }
}
</style>
