import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import {
  generateQuiz as apiGenerateQuiz,
  submitAnswers as apiSubmitAnswers,
  getQuizHistory as apiGetQuizHistory
} from '@/api/quiz'
import type { Quiz, QuizAnswer, QuizResult, DifficultyLevel } from '@/types'

export const useQuizStore = defineStore('quiz', () => {
  // State
  const currentQuiz = ref<Quiz | null>(null)
  const quizResult = ref<QuizResult | null>(null)
  const history = ref<Quiz[]>([])
  const userAnswers = ref<Map<number, string>>(new Map())
  const loading = ref(false)
  const submitting = ref(false)
  const historyLoading = ref(false)
  const error = ref<string | null>(null)

  // Getters
  const hasActiveQuiz = computed(() => !!currentQuiz.value && !currentQuiz.value.completedAt)
  const isQuizCompleted = computed(() => !!currentQuiz.value?.completedAt || !!quizResult.value)
  const questionCount = computed(() => currentQuiz.value?.questions?.length || 0)
  const answeredCount = computed(() => userAnswers.value.size)
  const allQuestionsAnswered = computed(() => answeredCount.value === questionCount.value)
  const historyCount = computed(() => history.value.length)

  /**
   * Generate a new quiz with specified difficulty
   * @param difficulty Quiz difficulty level
   * @returns Promise resolving to the generated quiz
   */
  async function generateQuiz(difficulty: DifficultyLevel): Promise<Quiz | null> {
    loading.value = true
    error.value = null
    // Clear previous state
    currentQuiz.value = null
    quizResult.value = null
    userAnswers.value = new Map()

    try {
      const result = await apiGenerateQuiz(difficulty)
      if (result.code === 0 || result.code === 200) {
        currentQuiz.value = result.data
        return result.data
      } else {
        error.value = result.message || 'Failed to generate quiz'
        return null
      }
    } catch (err: any) {
      error.value = err.message || 'Failed to generate quiz'
      throw err
    } finally {
      loading.value = false
    }
  }


  /**
   * Set answer for a question
   * @param questionId Question ID
   * @param answer User's answer
   */
  function setAnswer(questionId: number, answer: string) {
    userAnswers.value.set(questionId, answer)
  }

  /**
   * Get answer for a question
   * @param questionId Question ID
   * @returns User's answer or undefined
   */
  function getAnswer(questionId: number): string | undefined {
    return userAnswers.value.get(questionId)
  }

  /**
   * Submit quiz answers
   * @returns Promise resolving to the quiz result
   */
  async function submitQuiz(): Promise<QuizResult | null> {
    if (!currentQuiz.value) {
      error.value = 'No active quiz'
      return null
    }

    submitting.value = true
    error.value = null

    // Convert Map to array of QuizAnswer
    const answers: QuizAnswer[] = Array.from(userAnswers.value.entries()).map(
      ([questionId, answer]) => ({ questionId, answer })
    )

    try {
      const result = await apiSubmitAnswers(currentQuiz.value.id, answers)
      if (result.code === 0 || result.code === 200) {
        quizResult.value = result.data
        // Update current quiz with completion info
        if (currentQuiz.value) {
          currentQuiz.value.userScore = result.data.score
          currentQuiz.value.completedAt = new Date().toISOString()
        }
        // Refresh history after submission
        await fetchHistory()
        return result.data
      } else {
        error.value = result.message || 'Failed to submit quiz'
        return null
      }
    } catch (err: any) {
      error.value = err.message || 'Failed to submit quiz'
      throw err
    } finally {
      submitting.value = false
    }
  }

  /**
   * Fetch quiz history for current user
   * @returns Promise resolving to the history list
   */
  async function fetchHistory(): Promise<Quiz[]> {
    historyLoading.value = true

    try {
      const result = await apiGetQuizHistory()
      if (result.code === 0 || result.code === 200) {
        history.value = result.data || []
        return history.value
      } else {
        return []
      }
    } catch (err: any) {
      console.error('Failed to fetch quiz history:', err)
      return []
    } finally {
      historyLoading.value = false
    }
  }

  /**
   * Clear current quiz and start fresh
   */
  function clearQuiz() {
    currentQuiz.value = null
    quizResult.value = null
    userAnswers.value = new Map()
    error.value = null
  }

  /**
   * Clear all state
   */
  function clearAll() {
    currentQuiz.value = null
    quizResult.value = null
    history.value = []
    userAnswers.value = new Map()
    error.value = null
  }

  /**
   * Get difficulty label for display
   * @param difficulty Difficulty level
   * @returns Human-readable label
   */
  function getDifficultyLabel(difficulty: DifficultyLevel): string {
    const labels: Record<DifficultyLevel, string> = {
      easy: 'Easy',
      medium: 'Medium',
      hard: 'Hard'
    }
    return labels[difficulty] || difficulty
  }

  /**
   * Get difficulty color for UI
   * @param difficulty Difficulty level
   * @returns Element Plus tag type
   */
  function getDifficultyType(difficulty: DifficultyLevel): 'success' | 'warning' | 'danger' | 'info' {
    const types: Record<DifficultyLevel, 'success' | 'warning' | 'danger'> = {
      easy: 'success',
      medium: 'warning',
      hard: 'danger'
    }
    return types[difficulty] || 'info'
  }

  return {
    // State
    currentQuiz,
    quizResult,
    history,
    userAnswers,
    loading,
    submitting,
    historyLoading,
    error,
    // Getters
    hasActiveQuiz,
    isQuizCompleted,
    questionCount,
    answeredCount,
    allQuestionsAnswered,
    historyCount,
    // Actions
    generateQuiz,
    setAnswer,
    getAnswer,
    submitQuiz,
    fetchHistory,
    clearQuiz,
    clearAll,
    getDifficultyLabel,
    getDifficultyType
  }
})
