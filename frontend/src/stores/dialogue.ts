import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import {
  getScenarios as apiGetScenarios,
  createScenario as apiCreateScenario,
  startSession as apiStartSession,
  sendMessage as apiSendMessage,
  endSession as apiEndSession
} from '@/api/dialogue'
import type { Scenario, CreateScenarioRequest, DialogueSession, DialogueMessage } from '@/types'

export const useDialogueStore = defineStore('dialogue', () => {
  // State
  const scenarios = ref<Scenario[]>([])
  const currentSession = ref<DialogueSession | null>(null)
  const messages = ref<DialogueMessage[]>([])
  const loading = ref(false)
  const sendingMessage = ref(false)
  const error = ref<string | null>(null)

  // Getters
  const hasActiveSession = computed(() => !!currentSession.value && !currentSession.value.endedAt)
  const scenarioCount = computed(() => scenarios.value.length)
  const presetScenarios = computed(() => scenarios.value.filter(s => s.isPreset))
  const customScenarios = computed(() => scenarios.value.filter(s => !s.isPreset))
  const messageCount = computed(() => messages.value.length)

  /**
   * Fetch all available scenarios
   * @returns Promise resolving to the scenarios list
   */
  async function fetchScenarios(): Promise<Scenario[]> {
    loading.value = true
    error.value = null

    try {
      const result = await apiGetScenarios()
      if (result.code === 0 || result.code === 200) {
        scenarios.value = result.data || []
        return scenarios.value
      } else {
        error.value = result.message || 'Failed to fetch scenarios'
        return []
      }
    } catch (err: any) {
      error.value = err.message || 'Failed to fetch scenarios'
      throw err
    } finally {
      loading.value = false
    }
  }

  /**
   * Create a custom scenario
   * @param data Scenario creation data
   * @returns Promise resolving to the created scenario
   */
  async function createScenario(data: CreateScenarioRequest): Promise<Scenario | null> {
    loading.value = true
    error.value = null

    try {
      const result = await apiCreateScenario(data)
      if (result.code === 0 || result.code === 200) {
        // Add to scenarios list
        scenarios.value.push(result.data)
        return result.data
      } else {
        error.value = result.message || 'Failed to create scenario'
        return null
      }
    } catch (err: any) {
      error.value = err.message || 'Failed to create scenario'
      throw err
    } finally {
      loading.value = false
    }
  }


  /**
   * Start a new dialogue session with a scenario
   * @param scenarioId ID of the scenario to use
   * @returns Promise resolving to the new session
   */
  async function startSession(scenarioId: number): Promise<DialogueSession | null> {
    loading.value = true
    error.value = null

    try {
      const result = await apiStartSession(scenarioId)
      if (result.code === 0 || result.code === 200) {
        currentSession.value = result.data
        messages.value = result.data.messages || []
        return result.data
      } else {
        error.value = result.message || 'Failed to start session'
        return null
      }
    } catch (err: any) {
      error.value = err.message || 'Failed to start session'
      throw err
    } finally {
      loading.value = false
    }
  }

  /**
   * Send a message in the current dialogue session
   * @param message User message content
   * @returns Promise resolving to the AI response message
   */
  async function sendMessage(message: string): Promise<DialogueMessage | null> {
    if (!currentSession.value) {
      error.value = 'No active session'
      return null
    }

    sendingMessage.value = true
    error.value = null

    // Add user message to local state immediately
    const userMessage: DialogueMessage = {
      role: 'user',
      content: message,
      timestamp: new Date().toISOString()
    }
    messages.value.push(userMessage)

    try {
      const result = await apiSendMessage(currentSession.value.id, message)
      if (result.code === 0 || result.code === 200) {
        // Add AI response to messages
        messages.value.push(result.data)
        return result.data
      } else {
        // Remove the user message if API call failed
        messages.value.pop()
        error.value = result.message || 'Failed to send message'
        return null
      }
    } catch (err: any) {
      // Remove the user message if API call failed
      messages.value.pop()
      error.value = err.message || 'Failed to send message'
      throw err
    } finally {
      sendingMessage.value = false
    }
  }

  /**
   * End the current dialogue session
   * @returns Promise resolving when session ends
   */
  async function endCurrentSession(): Promise<void> {
    if (!currentSession.value) {
      return
    }

    loading.value = true
    error.value = null

    try {
      await apiEndSession(currentSession.value.id)
      // Mark session as ended
      if (currentSession.value) {
        currentSession.value.endedAt = new Date().toISOString()
      }
    } catch (err: any) {
      error.value = err.message || 'Failed to end session'
      throw err
    } finally {
      loading.value = false
    }
  }

  /**
   * Clear current session and messages
   */
  function clearSession() {
    currentSession.value = null
    messages.value = []
    error.value = null
  }

  /**
   * Clear all state
   */
  function clearAll() {
    scenarios.value = []
    currentSession.value = null
    messages.value = []
    error.value = null
  }

  /**
   * Get scenario by ID
   * @param id Scenario ID
   * @returns Scenario or undefined
   */
  function getScenarioById(id: number): Scenario | undefined {
    return scenarios.value.find(s => s.id === id)
  }

  return {
    // State
    scenarios,
    currentSession,
    messages,
    loading,
    sendingMessage,
    error,
    // Getters
    hasActiveSession,
    scenarioCount,
    presetScenarios,
    customScenarios,
    messageCount,
    // Actions
    fetchScenarios,
    createScenario,
    startSession,
    sendMessage,
    endCurrentSession,
    clearSession,
    clearAll,
    getScenarioById
  }
})
