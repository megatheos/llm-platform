<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useQuizStore } from '@/stores/quiz'
import { ElMessage } from 'element-plus'
import type { DifficultyLevel, QuizQuestion } from '@/types'

const quizStore = useQuizStore()

// UI state
const selectedDifficulty = ref<DifficultyLevel>('medium')
const currentQuestionIndex = ref(0)

// Difficulty options
const difficultyOptions: { value: DifficultyLevel; label: string; description: string }[] = [
  { value: 'easy', label: 'Easy', description: 'Basic vocabulary and simple sentences' },
  { value: 'medium', label: 'Medium', description: 'Intermediate vocabulary and grammar' },
  { value: 'hard', label: 'Hard', description: 'Advanced vocabulary and complex structures' }
]

// Current question
const currentQuestion = computed<QuizQuestion | null>(() => {
  if (!quizStore.currentQuiz?.questions) return null
  return quizStore.currentQuiz.questions[currentQuestionIndex.value] || null
})

// Progress percentage
const progressPercentage = computed(() => {
  if (!quizStore.questionCount) return 0
  return Math.round((quizStore.answeredCount / quizStore.questionCount) * 100)
})

// Get selected answer for current question
const selectedAnswer = computed(() => {
  if (!currentQuestion.value) return ''
  return quizStore.getAnswer(currentQuestion.value.id) || ''
})

// Handle starting a new quiz
async function handleStartQuiz() {
  try {
    await quizStore.generateQuiz(selectedDifficulty.value)
    currentQuestionIndex.value = 0
    if (quizStore.error) {
      ElMessage.error(quizStore.error)
    } else {
      ElMessage.success('Quiz generated! Good luck!')
    }
  } catch (err: any) {
    ElMessage.error(err.message || 'Failed to generate quiz')
  }
}

// Handle selecting an answer
function handleSelectAnswer(answer: string | number | boolean | undefined) {
  if (!currentQuestion.value || answer === undefined) return
  quizStore.setAnswer(currentQuestion.value.id, String(answer))
}


// Navigate to next question
function handleNextQuestion() {
  if (currentQuestionIndex.value < quizStore.questionCount - 1) {
    currentQuestionIndex.value++
  }
}

// Navigate to previous question
function handlePrevQuestion() {
  if (currentQuestionIndex.value > 0) {
    currentQuestionIndex.value--
  }
}

// Jump to specific question
function handleJumpToQuestion(index: number) {
  currentQuestionIndex.value = index
}

// Handle submitting the quiz
async function handleSubmitQuiz() {
  if (!quizStore.allQuestionsAnswered) {
    ElMessage.warning('Please answer all questions before submitting')
    return
  }

  try {
    await quizStore.submitQuiz()
    if (quizStore.error) {
      ElMessage.error(quizStore.error)
    } else {
      ElMessage.success('Quiz submitted successfully!')
    }
  } catch (err: any) {
    ElMessage.error(err.message || 'Failed to submit quiz')
  }
}

// Handle starting a new quiz after completion
function handleNewQuiz() {
  quizStore.clearQuiz()
  currentQuestionIndex.value = 0
}

// Format date for display
function formatDate(dateStr: string): string {
  const date = new Date(dateStr)
  return date.toLocaleDateString(undefined, {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  })
}

// Get score percentage
function getScorePercentage(score: number, total: number): number {
  if (!total) return 0
  return Math.round((score / total) * 100)
}

// Get score color based on percentage
function getScoreColor(percentage: number): string {
  if (percentage >= 80) return '#67c23a'
  if (percentage >= 60) return '#e6a23c'
  return '#f56c6c'
}

onMounted(() => {
  quizStore.fetchHistory()
})
</script>

<template>
  <div class="quiz-page">
    <!-- Quiz Selection View -->
    <div v-if="!quizStore.hasActiveQuiz && !quizStore.isQuizCompleted" class="quiz-selection">
      <el-row :gutter="20">
        <el-col :span="24">
          <el-card class="header-card">
            <div class="page-header">
              <h1>Language Quiz</h1>
              <p>Test your language knowledge with AI-generated quizzes</p>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <el-row :gutter="20" class="content-section">
        <!-- Start Quiz Section -->
        <el-col :span="16">
          <el-card>
            <template #header>
              <div class="card-header">
                <span>Start New Quiz</span>
              </div>
            </template>

            <div class="difficulty-selection">
              <h3>Select Difficulty Level</h3>
              <el-radio-group v-model="selectedDifficulty" class="difficulty-group">
                <el-radio-button
                  v-for="option in difficultyOptions"
                  :key="option.value"
                  :value="option.value"
                  class="difficulty-option"
                >
                  <div class="difficulty-content">
                    <span class="difficulty-label">{{ option.label }}</span>
                    <span class="difficulty-desc">{{ option.description }}</span>
                  </div>
                </el-radio-button>
              </el-radio-group>

              <el-button
                type="primary"
                size="large"
                :loading="quizStore.loading"
                class="start-button"
                @click="handleStartQuiz"
              >
                <el-icon><VideoPlay /></el-icon>
                Start Quiz
              </el-button>
            </div>
          </el-card>
        </el-col>


        <!-- Quiz History Section -->
        <el-col :span="8">
          <el-card>
            <template #header>
              <div class="card-header">
                <span>Recent Quizzes</span>
                <el-badge :value="quizStore.historyCount" />
              </div>
            </template>

            <div v-if="quizStore.historyLoading" class="loading-container">
              <el-skeleton :rows="3" animated />
            </div>

            <el-scrollbar v-else height="300px">
              <div
                v-for="quiz in quizStore.history"
                :key="quiz.id"
                class="history-item"
              >
                <div class="history-header">
                  <el-tag :type="quizStore.getDifficultyType(quiz.difficulty)" size="small">
                    {{ quizStore.getDifficultyLabel(quiz.difficulty) }}
                  </el-tag>
                  <span class="history-date">{{ formatDate(quiz.createdAt) }}</span>
                </div>
                <div class="history-score">
                  <el-progress
                    :percentage="getScorePercentage(quiz.userScore || 0, quiz.totalScore)"
                    :color="getScoreColor(getScorePercentage(quiz.userScore || 0, quiz.totalScore))"
                    :stroke-width="8"
                  />
                  <span class="score-text">
                    {{ quiz.userScore || 0 }} / {{ quiz.totalScore }}
                  </span>
                </div>
              </div>

              <el-empty
                v-if="quizStore.history.length === 0"
                description="No quiz history yet"
                :image-size="80"
              />
            </el-scrollbar>
          </el-card>
        </el-col>
      </el-row>
    </div>

    <!-- Active Quiz View -->
    <div v-else-if="quizStore.hasActiveQuiz && !quizStore.isQuizCompleted" class="quiz-active">
      <el-card class="quiz-card">
        <!-- Quiz Header -->
        <template #header>
          <div class="quiz-header">
            <div class="quiz-info">
              <el-tag :type="quizStore.getDifficultyType(quizStore.currentQuiz!.difficulty)">
                {{ quizStore.getDifficultyLabel(quizStore.currentQuiz!.difficulty) }}
              </el-tag>
              <span class="question-counter">
                Question {{ currentQuestionIndex + 1 }} of {{ quizStore.questionCount }}
              </span>
            </div>
            <div class="quiz-progress">
              <span>Progress: {{ quizStore.answeredCount }}/{{ quizStore.questionCount }}</span>
              <el-progress
                :percentage="progressPercentage"
                :stroke-width="10"
                style="width: 150px"
              />
            </div>
          </div>
        </template>

        <!-- Question Navigation -->
        <div class="question-nav">
          <el-button
            v-for="(q, index) in quizStore.currentQuiz?.questions"
            :key="q.id"
            :type="quizStore.getAnswer(q.id) ? 'success' : (index === currentQuestionIndex ? 'primary' : 'default')"
            circle
            size="small"
            @click="handleJumpToQuestion(index)"
          >
            {{ index + 1 }}
          </el-button>
        </div>

        <!-- Question Content -->
        <div v-if="currentQuestion" class="question-content">
          <h2 class="question-text">{{ currentQuestion.question }}</h2>

          <div class="options-container">
            <el-radio-group
              :model-value="selectedAnswer"
              class="options-group"
              @update:model-value="handleSelectAnswer"
            >
              <el-radio
                v-for="(option, index) in currentQuestion.options"
                :key="index"
                :value="option"
                class="option-item"
                border
              >
                <span class="option-letter">{{ String.fromCharCode(65 + index) }}.</span>
                {{ option }}
              </el-radio>
            </el-radio-group>
          </div>
        </div>


        <!-- Navigation Buttons -->
        <div class="navigation-buttons">
          <el-button
            :disabled="currentQuestionIndex === 0"
            @click="handlePrevQuestion"
          >
            <el-icon><ArrowLeft /></el-icon>
            Previous
          </el-button>

          <el-button
            v-if="currentQuestionIndex < quizStore.questionCount - 1"
            type="primary"
            @click="handleNextQuestion"
          >
            Next
            <el-icon><ArrowRight /></el-icon>
          </el-button>

          <el-button
            v-else
            type="success"
            :disabled="!quizStore.allQuestionsAnswered"
            :loading="quizStore.submitting"
            @click="handleSubmitQuiz"
          >
            <el-icon><CircleCheck /></el-icon>
            Submit Quiz
          </el-button>
        </div>
      </el-card>
    </div>

    <!-- Quiz Results View -->
    <div v-else-if="quizStore.isQuizCompleted && quizStore.quizResult" class="quiz-results">
      <el-card class="results-card">
        <div class="results-content">
          <div class="results-icon">
            <el-icon
              :size="80"
              :color="getScoreColor(getScorePercentage(quizStore.quizResult.score, quizStore.quizResult.totalScore))"
            >
              <Trophy />
            </el-icon>
          </div>

          <h1 class="results-title">Quiz Complete!</h1>

          <div class="score-display">
            <div class="score-circle">
              <el-progress
                type="circle"
                :percentage="getScorePercentage(quizStore.quizResult.score, quizStore.quizResult.totalScore)"
                :width="180"
                :stroke-width="12"
                :color="getScoreColor(getScorePercentage(quizStore.quizResult.score, quizStore.quizResult.totalScore))"
              >
                <template #default>
                  <div class="score-inner">
                    <span class="score-value">{{ quizStore.quizResult.score }}</span>
                    <span class="score-total">/ {{ quizStore.quizResult.totalScore }}</span>
                  </div>
                </template>
              </el-progress>
            </div>
          </div>

          <div class="results-stats">
            <el-row :gutter="20">
              <el-col :span="8">
                <div class="stat-item">
                  <div class="stat-value correct">{{ quizStore.quizResult.correctAnswers }}</div>
                  <div class="stat-label">Correct</div>
                </div>
              </el-col>
              <el-col :span="8">
                <div class="stat-item">
                  <div class="stat-value wrong">
                    {{ quizStore.quizResult.totalQuestions - quizStore.quizResult.correctAnswers }}
                  </div>
                  <div class="stat-label">Wrong</div>
                </div>
              </el-col>
              <el-col :span="8">
                <div class="stat-item">
                  <div class="stat-value total">{{ quizStore.quizResult.totalQuestions }}</div>
                  <div class="stat-label">Total</div>
                </div>
              </el-col>
            </el-row>
          </div>

          <div class="results-message">
            <p v-if="getScorePercentage(quizStore.quizResult.score, quizStore.quizResult.totalScore) >= 80">
              🎉 Excellent work! You've mastered this level!
            </p>
            <p v-else-if="getScorePercentage(quizStore.quizResult.score, quizStore.quizResult.totalScore) >= 60">
              👍 Good job! Keep practicing to improve!
            </p>
            <p v-else>
              💪 Don't give up! Practice makes perfect!
            </p>
          </div>

          <el-button type="primary" size="large" @click="handleNewQuiz">
            <el-icon><RefreshRight /></el-icon>
            Take Another Quiz
          </el-button>
        </div>
      </el-card>
    </div>
  </div>
</template>


<script lang="ts">
import {
  VideoPlay,
  ArrowLeft,
  ArrowRight,
  CircleCheck,
  Trophy,
  RefreshRight
} from '@element-plus/icons-vue'

export default {
  components: { VideoPlay, ArrowLeft, ArrowRight, CircleCheck, Trophy, RefreshRight }
}
</script>

<style scoped>
.quiz-page {
  padding: 20px;
  min-height: calc(100vh - 100px);
}

/* Header Styles */
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

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-weight: 600;
}

.content-section {
  margin-top: 20px;
}

/* Difficulty Selection Styles */
.difficulty-selection {
  text-align: center;
  padding: 20px;
}

.difficulty-selection h3 {
  margin-bottom: 24px;
  color: #303133;
}

.difficulty-group {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-bottom: 32px;
}

.difficulty-option {
  width: 100%;
}

.difficulty-option :deep(.el-radio-button__inner) {
  width: 100%;
  text-align: left;
  padding: 16px 20px;
  height: auto;
}

.difficulty-content {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.difficulty-label {
  font-weight: 600;
  font-size: 16px;
}

.difficulty-desc {
  font-size: 13px;
  color: #909399;
  font-weight: normal;
}

.start-button {
  min-width: 200px;
}

/* History Styles */
.loading-container {
  padding: 20px;
}

.history-item {
  padding: 12px;
  border-bottom: 1px solid #ebeef5;
}

.history-item:last-child {
  border-bottom: none;
}

.history-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.history-date {
  font-size: 12px;
  color: #909399;
}

.history-score {
  display: flex;
  align-items: center;
  gap: 12px;
}

.history-score .el-progress {
  flex: 1;
}

.score-text {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  min-width: 60px;
  text-align: right;
}


/* Active Quiz Styles */
.quiz-active {
  max-width: 800px;
  margin: 0 auto;
}

.quiz-card {
  min-height: 500px;
}

.quiz-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.quiz-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.question-counter {
  font-weight: 600;
  color: #303133;
}

.quiz-progress {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 14px;
  color: #606266;
}

.question-nav {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  padding: 16px 0;
  border-bottom: 1px solid #ebeef5;
  margin-bottom: 24px;
}

.question-content {
  padding: 20px 0;
}

.question-text {
  font-size: 20px;
  color: #303133;
  margin-bottom: 32px;
  line-height: 1.6;
}

.options-container {
  margin-bottom: 32px;
}

.options-group {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.option-item {
  width: 100%;
  margin: 0 !important;
  padding: 16px 20px;
  border-radius: 8px;
  transition: all 0.3s;
}

.option-item:hover {
  border-color: #409eff;
}

.option-item.is-checked {
  border-color: #409eff;
  background-color: #ecf5ff;
}

.option-letter {
  font-weight: 600;
  margin-right: 8px;
  color: #409eff;
}

.navigation-buttons {
  display: flex;
  justify-content: space-between;
  padding-top: 20px;
  border-top: 1px solid #ebeef5;
}

/* Results Styles */
.quiz-results {
  max-width: 600px;
  margin: 0 auto;
}

.results-card {
  text-align: center;
}

.results-content {
  padding: 40px 20px;
}

.results-icon {
  margin-bottom: 20px;
}

.results-title {
  font-size: 28px;
  color: #303133;
  margin-bottom: 32px;
}

.score-display {
  margin-bottom: 32px;
}

.score-circle {
  display: inline-block;
}

.score-inner {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.score-value {
  font-size: 48px;
  font-weight: 700;
  color: #303133;
  line-height: 1;
}

.score-total {
  font-size: 18px;
  color: #909399;
}

.results-stats {
  margin-bottom: 32px;
}

.stat-item {
  text-align: center;
  padding: 16px;
  background-color: #f5f7fa;
  border-radius: 8px;
}

.stat-value {
  font-size: 32px;
  font-weight: 700;
  margin-bottom: 4px;
}

.stat-value.correct {
  color: #67c23a;
}

.stat-value.wrong {
  color: #f56c6c;
}

.stat-value.total {
  color: #409eff;
}

.stat-label {
  font-size: 14px;
  color: #909399;
}

.results-message {
  margin-bottom: 32px;
}

.results-message p {
  font-size: 18px;
  color: #606266;
  margin: 0;
}
</style>
