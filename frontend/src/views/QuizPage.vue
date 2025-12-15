<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { useQuizStore } from '@/stores/quiz'
import { ElMessage } from 'element-plus'
import type { DifficultyLevel, QuizQuestion } from '@/types'

const { t } = useI18n()
const quizStore = useQuizStore()

const selectedDifficulty = ref<DifficultyLevel>('medium')
const currentQuestionIndex = ref(0)

const difficultyOptions = computed(() => [
  { value: 'easy' as DifficultyLevel, label: t('quiz.easy'), description: t('quiz.easyDesc') },
  { value: 'medium' as DifficultyLevel, label: t('quiz.medium'), description: t('quiz.mediumDesc') },
  { value: 'hard' as DifficultyLevel, label: t('quiz.hard'), description: t('quiz.hardDesc') }
])

const currentQuestion = computed<QuizQuestion | null>(() => {
  if (!quizStore.currentQuiz?.questions) return null
  return quizStore.currentQuiz.questions[currentQuestionIndex.value] || null
})

const progressPercentage = computed(() => {
  if (!quizStore.questionCount) return 0
  return Math.round((quizStore.answeredCount / quizStore.questionCount) * 100)
})

const selectedAnswer = computed(() => {
  if (!currentQuestion.value) return ''
  return quizStore.getAnswer(currentQuestion.value.id) || ''
})

async function handleStartQuiz() {
  try {
    await quizStore.generateQuiz(selectedDifficulty.value)
    currentQuestionIndex.value = 0
    if (quizStore.error) ElMessage.error(quizStore.error)
    else ElMessage.success(t('quiz.quizGenerated'))
  } catch (err: any) {
    ElMessage.error(err.message || t('quiz.generateFailed'))
  }
}

function handleSelectAnswer(answer: string | number | boolean | undefined) {
  if (!currentQuestion.value || answer === undefined) return
  quizStore.setAnswer(currentQuestion.value.id, String(answer))
}

function handleNextQuestion() {
  if (currentQuestionIndex.value < quizStore.questionCount - 1) currentQuestionIndex.value++
}

function handlePrevQuestion() {
  if (currentQuestionIndex.value > 0) currentQuestionIndex.value--
}

function handleJumpToQuestion(index: number) { currentQuestionIndex.value = index }

async function handleSubmitQuiz() {
  if (!quizStore.allQuestionsAnswered) { ElMessage.warning(t('quiz.answerAll')); return }
  try {
    await quizStore.submitQuiz()
    if (quizStore.error) ElMessage.error(quizStore.error)
    else ElMessage.success(t('quiz.submitSuccess'))
  } catch (err: any) { ElMessage.error(err.message || t('quiz.submitFailed')) }
}

function handleNewQuiz() { quizStore.clearQuiz(); currentQuestionIndex.value = 0 }

function formatDate(dateStr: string): string {
  return new Date(dateStr).toLocaleDateString(undefined, { year: 'numeric', month: 'short', day: 'numeric' })
}

function getScorePercentage(score: number, total: number): number { return total ? Math.round((score / total) * 100) : 0 }
function getScoreColor(p: number): string { return p >= 80 ? '#67c23a' : p >= 60 ? '#e6a23c' : '#f56c6c' }
function getResultMessage(p: number): string { return p >= 80 ? t('quiz.excellent') : p >= 60 ? t('quiz.good') : t('quiz.keepTrying') }

onMounted(() => { quizStore.fetchHistory() })
</script>

<template>
  <div class="quiz-page">
    <div v-if="!quizStore.hasActiveQuiz && !quizStore.isQuizCompleted" class="quiz-selection">
      <el-card class="header-card">
        <div class="page-header"><h1>{{ t('quiz.title') }}</h1><p>{{ t('quiz.subtitle') }}</p></div>
      </el-card>
      <el-row :gutter="20">
        <el-col :span="16">
          <el-card>
            <template #header><div class="card-header"><span>{{ t('quiz.startNewQuiz') }}</span></div></template>
            <div class="difficulty-selection">
              <h3>{{ t('quiz.selectDifficulty') }}</h3>
              <el-radio-group v-model="selectedDifficulty" class="difficulty-group">
                <el-radio-button v-for="opt in difficultyOptions" :key="opt.value" :value="opt.value" class="difficulty-option">
                  <div class="difficulty-content"><span class="difficulty-label">{{ opt.label }}</span><span class="difficulty-desc">{{ opt.description }}</span></div>
                </el-radio-button>
              </el-radio-group>
              <el-button type="primary" size="large" :loading="quizStore.loading" @click="handleStartQuiz">{{ t('quiz.startQuiz') }}</el-button>
            </div>
          </el-card>
        </el-col>
        <el-col :span="8">
          <el-card>
            <template #header><div class="card-header"><span>{{ t('quiz.recentQuizzes') }}</span><el-badge :value="quizStore.historyCount" /></div></template>
            <el-scrollbar height="300px">
              <div v-for="quiz in quizStore.history" :key="quiz.id" class="history-item">
                <div class="history-header"><el-tag size="small">{{ quizStore.getDifficultyLabel(quiz.difficulty) }}</el-tag><span>{{ formatDate(quiz.createdAt) }}</span></div>
                <el-progress :percentage="getScorePercentage(quiz.userScore || 0, quiz.totalScore)" :color="getScoreColor(getScorePercentage(quiz.userScore || 0, quiz.totalScore))" />
              </div>
              <el-empty v-if="!quizStore.history.length" :description="t('quiz.noHistory')" :image-size="80" />
            </el-scrollbar>
          </el-card>
        </el-col>
      </el-row>
    </div>

    <div v-else-if="quizStore.hasActiveQuiz && !quizStore.isQuizCompleted" class="quiz-active">
      <el-card>
        <template #header>
          <div class="quiz-header">
            <span>{{ t('quiz.questionOf', { current: currentQuestionIndex + 1, total: quizStore.questionCount }) }}</span>
            <span>{{ t('quiz.progress') }}: {{ quizStore.answeredCount }}/{{ quizStore.questionCount }}</span>
          </div>
        </template>
        <div class="question-nav">
          <el-button v-for="(q, i) in quizStore.currentQuiz?.questions" :key="q.id" :type="quizStore.getAnswer(q.id) ? 'success' : i === currentQuestionIndex ? 'primary' : 'default'" circle size="small" @click="handleJumpToQuestion(i)">{{ i + 1 }}</el-button>
        </div>
        <div v-if="currentQuestion" class="question-content">
          <h2>{{ currentQuestion.question }}</h2>
          <el-radio-group :model-value="selectedAnswer" class="options-group" @update:model-value="handleSelectAnswer">
            <el-radio v-for="(opt, i) in currentQuestion.options" :key="i" :value="opt" border class="option-item">{{ String.fromCharCode(65 + i) }}. {{ opt }}</el-radio>
          </el-radio-group>
        </div>
        <div class="navigation-buttons">
          <el-button :disabled="currentQuestionIndex === 0" @click="handlePrevQuestion">{{ t('quiz.previous') }}</el-button>
          <el-button v-if="currentQuestionIndex < quizStore.questionCount - 1" type="primary" @click="handleNextQuestion">{{ t('quiz.next') }}</el-button>
          <el-button v-else type="success" :disabled="!quizStore.allQuestionsAnswered" @click="handleSubmitQuiz">{{ t('quiz.submitQuiz') }}</el-button>
        </div>
      </el-card>
    </div>

    <div v-else-if="quizStore.isQuizCompleted && quizStore.quizResult" class="quiz-results">
      <el-card>
        <div class="results-content">
          <h1>{{ t('quiz.quizComplete') }}</h1>
          <el-progress type="circle" :percentage="getScorePercentage(quizStore.quizResult.score, quizStore.quizResult.totalScore)" :width="150" :color="getScoreColor(getScorePercentage(quizStore.quizResult.score, quizStore.quizResult.totalScore))" />
          <p class="score-text">{{ quizStore.quizResult.score }} / {{ quizStore.quizResult.totalScore }}</p>
          <el-row :gutter="20" class="stats">
            <el-col :span="8"><div class="stat correct">{{ quizStore.quizResult.correctAnswers }}<span>{{ t('quiz.correct') }}</span></div></el-col>
            <el-col :span="8"><div class="stat wrong">{{ quizStore.quizResult.totalQuestions - quizStore.quizResult.correctAnswers }}<span>{{ t('quiz.wrong') }}</span></div></el-col>
            <el-col :span="8"><div class="stat total">{{ quizStore.quizResult.totalQuestions }}<span>{{ t('quiz.total') }}</span></div></el-col>
          </el-row>
          <p class="message">{{ getResultMessage(getScorePercentage(quizStore.quizResult.score, quizStore.quizResult.totalScore)) }}</p>
          <el-button type="primary" @click="handleNewQuiz">{{ t('quiz.takeAnother') }}</el-button>
        </div>
      </el-card>
    </div>
  </div>
</template>

<style scoped>
.quiz-page { padding: 0; }
.header-card { margin-bottom: 20px; }
.page-header { text-align: center; }
.page-header h1 { margin: 0 0 8px; color: var(--text-primary); }
.page-header p { margin: 0; color: var(--text-secondary); }
.card-header { display: flex; justify-content: space-between; align-items: center; font-weight: 600; }
.difficulty-selection { text-align: center; padding: 20px; }
.difficulty-selection h3 { margin-bottom: 24px; }
.difficulty-group { display: flex; flex-direction: column; gap: 12px; margin-bottom: 24px; }
.difficulty-option { width: 100%; }
.difficulty-option :deep(.el-radio-button__inner) { width: 100%; text-align: left; padding: 16px; height: auto; }
.difficulty-content { display: flex; flex-direction: column; }
.difficulty-label { font-weight: 600; }
.difficulty-desc { font-size: 13px; color: var(--text-muted); }
.history-item { padding: 12px; border-bottom: 1px solid var(--border-color-light); }
.history-header { display: flex; justify-content: space-between; margin-bottom: 8px; font-size: 12px; color: var(--text-muted); }
.quiz-active { max-width: 800px; margin: 0 auto; }
.quiz-header { display: flex; justify-content: space-between; }
.question-nav { display: flex; flex-wrap: wrap; gap: 8px; padding: 16px 0; border-bottom: 1px solid var(--border-color-light); }
.question-content { padding: 24px 0; }
.question-content h2 { margin-bottom: 24px; line-height: 1.6; }
.options-group { display: flex; flex-direction: column; gap: 12px; }
.option-item { width: 100%; margin: 0 !important; padding: 16px; }
.navigation-buttons { display: flex; justify-content: space-between; padding-top: 20px; border-top: 1px solid var(--border-color-light); }
.quiz-results { max-width: 500px; margin: 0 auto; }
.results-content { text-align: center; padding: 40px 20px; }
.results-content h1 { margin-bottom: 24px; }
.score-text { font-size: 24px; font-weight: 700; margin: 16px 0 24px; }
.stats { margin-bottom: 24px; }
.stat { text-align: center; padding: 16px; background: var(--bg-tertiary); border-radius: 8px; font-size: 28px; font-weight: 700; }
.stat span { display: block; font-size: 14px; font-weight: normal; color: var(--text-muted); }
.stat.correct { color: #67c23a; }
.stat.wrong { color: #f56c6c; }
.stat.total { color: var(--color-accent); }
.message { font-size: 18px; margin-bottom: 24px; }
</style>
