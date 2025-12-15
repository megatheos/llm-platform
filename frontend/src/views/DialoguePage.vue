<script setup lang="ts">
import { ref, onMounted, nextTick, computed } from 'vue'
import { useDialogueStore } from '@/stores/dialogue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { Scenario, CreateScenarioRequest } from '@/types'

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

// Category options for scenarios
const categoryOptions = [
  { value: 'travel', label: 'Travel' },
  { value: 'business', label: 'Business' },
  { value: 'daily', label: 'Daily Life' },
  { value: 'academic', label: 'Academic' },
  { value: 'custom', label: 'Custom' }
]

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
      ElMessage.success(`Started dialogue: ${scenario.name}`)
    }
  } catch (err: any) {
    ElMessage.error(err.message || 'Failed to start session')
  }
}


// Handle sending message
async function handleSendMessage() {
  const message = messageInput.value.trim()
  if (!message) {
    ElMessage.warning('Please enter a message')
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
    ElMessage.error(err.message || 'Failed to send message')
  }
}

// Handle ending session
async function handleEndSession() {
  try {
    await ElMessageBox.confirm(
      'Are you sure you want to end this dialogue session?',
      'End Session',
      {
        confirmButtonText: 'End',
        cancelButtonText: 'Cancel',
        type: 'warning'
      }
    )

    await dialogueStore.endCurrentSession()
    dialogueStore.clearSession()
    ElMessage.success('Session ended')
  } catch (err: any) {
    if (err !== 'cancel') {
      ElMessage.error(err.message || 'Failed to end session')
    }
  }
}

// Handle creating custom scenario
async function handleCreateScenario() {
  if (!newScenario.value.name.trim()) {
    ElMessage.warning('Please enter a scenario name')
    return
  }
  if (!newScenario.value.description.trim()) {
    ElMessage.warning('Please enter a scenario description')
    return
  }
  if (!newScenario.value.category) {
    ElMessage.warning('Please select a category')
    return
  }

  try {
    const scenario = await dialogueStore.createScenario(newScenario.value)
    if (scenario) {
      ElMessage.success('Scenario created successfully')
      showCreateDialog.value = false
      // Reset form
      newScenario.value = { name: '', description: '', category: '' }
    } else if (dialogueStore.error) {
      ElMessage.error(dialogueStore.error)
    }
  } catch (err: any) {
    ElMessage.error(err.message || 'Failed to create scenario')
  }
}

// Format timestamp for display
function formatTime(timestamp: string): string {
  const date = new Date(timestamp)
  return date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
}

// Get category label
function getCategoryLabel(category: string): string {
  const option = categoryOptions.find(c => c.value === category)
  return option ? option.label : category
}

// Helper function to get scenario icon based on category
function getScenarioIcon(category: string): string {
  const icons: Record<string, string> = {
    travel: 'Location',
    business: 'Briefcase',
    daily: 'House',
    academic: 'Reading'
  }
  return icons[category] || 'ChatDotRound'
}

onMounted(() => {
  dialogueStore.fetchScenarios()
})
</script>


<template>
  <div class="dialogue-page">
    <!-- Scenario Selection View -->
    <div v-if="!dialogueStore.hasActiveSession" class="scenario-selection">
      <el-row :gutter="20">
        <el-col :span="24">
          <el-card class="header-card">
            <div class="page-header">
              <h1>AI Dialogue Practice</h1>
              <p>Select a scenario to start practicing conversations</p>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <el-row :gutter="20" class="scenario-section">
        <!-- Preset Scenarios -->
        <el-col :span="16">
          <el-card>
            <template #header>
              <div class="card-header">
                <span>Preset Scenarios</span>
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
                    <el-icon :size="32">
                      <component :is="getScenarioIcon(scenario.category)" />
                    </el-icon>
                  </div>
                  <div class="scenario-info">
                    <h3>{{ scenario.name }}</h3>
                    <p>{{ scenario.description }}</p>
                    <el-tag size="small" type="info">
                      {{ getCategoryLabel(scenario.category) }}
                    </el-tag>
                  </div>
                </el-card>
              </el-col>
            </el-row>

            <el-empty
              v-if="!dialogueStore.loading && dialogueStore.presetScenarios.length === 0"
              description="No preset scenarios available"
            />
          </el-card>
        </el-col>

        <!-- Custom Scenarios -->
        <el-col :span="8">
          <el-card>
            <template #header>
              <div class="card-header">
                <span>Custom Scenarios</span>
                <el-button type="primary" size="small" @click="showCreateDialog = true">
                  Create New
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
                description="No custom scenarios yet"
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
              <span class="chat-title">{{ currentScenario?.name || 'Dialogue' }}</span>
              <el-tag size="small" type="success">Active</el-tag>
            </div>
            <el-button type="danger" size="small" @click="handleEndSession">
              End Session
            </el-button>
          </div>
        </template>

        <!-- Messages Container -->
        <div ref="chatContainerRef" class="messages-container">
          <!-- Scenario Description -->
          <div v-if="currentScenario" class="scenario-intro">
            <el-alert
              :title="currentScenario.name"
              type="info"
              :closable="false"
              show-icon
            >
              {{ currentScenario.description }}
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
            description="Start the conversation by sending a message"
            :image-size="100"
          />
        </div>

        <!-- Message Input -->
        <div class="message-input-container">
          <el-input
            v-model="messageInput"
            type="textarea"
            :rows="2"
            placeholder="Type your message..."
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
            Send
          </el-button>
        </div>
      </el-card>
    </div>


    <!-- Create Scenario Dialog -->
    <el-dialog
      v-model="showCreateDialog"
      title="Create Custom Scenario"
      width="500px"
    >
      <el-form :model="newScenario" label-width="100px">
        <el-form-item label="Name" required>
          <el-input
            v-model="newScenario.name"
            placeholder="Enter scenario name"
            maxlength="50"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="Category" required>
          <el-select v-model="newScenario.category" placeholder="Select category" style="width: 100%">
            <el-option
              v-for="cat in categoryOptions"
              :key="cat.value"
              :label="cat.label"
              :value="cat.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="Description" required>
          <el-input
            v-model="newScenario.description"
            type="textarea"
            :rows="3"
            placeholder="Describe the scenario context and goals"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreateDialog = false">Cancel</el-button>
        <el-button type="primary" :loading="dialogueStore.loading" @click="handleCreateScenario">
          Create
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script lang="ts">
import { ChatDotRound, Promotion, Location, Briefcase, House, Reading } from '@element-plus/icons-vue'

export default {
  components: { ChatDotRound, Promotion, Location, Briefcase, House, Reading }
}
</script>


<style scoped>
.dialogue-page {
  padding: 20px;
  height: calc(100vh - 100px);
}

/* Scenario Selection Styles */
.header-card {
  margin-bottom: 20px;
}

.page-header {
  text-align: center;
}

.page-header h1 {
  margin: 0 0 8px 0;
  color: #303133;
}

.page-header p {
  margin: 0;
  color: #909399;
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
  border-color: #409eff;
  transform: translateY(-2px);
}

.scenario-icon {
  margin-right: 16px;
  color: #409eff;
  flex-shrink: 0;
}

.scenario-info h3 {
  margin: 0 0 8px 0;
  font-size: 16px;
  color: #303133;
}

.scenario-info p {
  margin: 0 0 8px 0;
  font-size: 14px;
  color: #606266;
  line-height: 1.5;
}

.custom-scenario-item {
  padding: 12px;
  border-bottom: 1px solid #ebeef5;
  cursor: pointer;
  transition: background-color 0.3s;
}

.custom-scenario-item:hover {
  background-color: #f5f7fa;
}

.custom-scenario-name {
  font-weight: 600;
  color: #303133;
  margin-bottom: 4px;
}

.custom-scenario-desc {
  font-size: 13px;
  color: #909399;
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
  background-color: #f5f7fa;
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
  background-color: #409eff;
  color: white;
  border-bottom-right-radius: 4px;
}

.message-bubble.assistant {
  background-color: white;
  color: #303133;
  border-bottom-left-radius: 4px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
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
  background-color: #909399;
  border-radius: 50%;
  animation: typing 1.4s infinite ease-in-out;
}

.typing-dot:nth-child(1) {
  animation-delay: 0s;
}

.typing-dot:nth-child(2) {
  animation-delay: 0.2s;
}

.typing-dot:nth-child(3) {
  animation-delay: 0.4s;
}

@keyframes typing {
  0%, 60%, 100% {
    transform: translateY(0);
  }
  30% {
    transform: translateY(-8px);
  }
}

/* Message Input */
.message-input-container {
  display: flex;
  gap: 12px;
  padding: 16px 20px;
  border-top: 1px solid #ebeef5;
  background-color: white;
}

.message-input-container .el-input {
  flex: 1;
}

.message-input-container .el-button {
  align-self: flex-end;
}
</style>
