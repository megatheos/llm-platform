<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { useQuizStore } from '@/stores/quiz'
import { ElMessage } from 'element-plus'
import type { DifficultyLevel, QuizQuestion } from '@/types'
import {
  Trophy,
  Clock,
  Check,
  Close,
  Star,
  Refresh,
  TrendCharts,
  ArrowRight,
  ArrowLeft,
  Download
} from '@element-plus/icons-vue'

const { t } = useI18n()
const quizStore = useQuizStore()

const selectedDifficulty = ref<DifficultyLevel>('medium')
const currentQuestionIndex = ref(0)
const showResults = ref(false)

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

// Difficulty options with enhanced visual
const difficultyOptions = computed(() => [
  { 
    value: 'easy' as DifficultyLevel, 
    label: t('quiz.easy'), 
    description: t('quiz.easyDesc'),
    icon: '🌱',
    color: '#67c23a',
    bgColor: '#f0f9eb'
  },
  { 
    value: 'medium' as DifficultyLevel, 
    label: t('quiz.medium'), 
    description: t('quiz.mediumDesc'),
    icon: '🔥',
    color: '#e6a23c',
    bgColor: '#fdf6ec'
  },
  { 
    value: 'hard' as DifficultyLevel, 
    label: t('quiz.hard'), 
    description: t('quiz.hardDesc'),
    icon: '💎',
    color: '#f56c6c',
    bgColor: '#fef0f0'
  }
])

const currentQuestion = computed<QuizQuestion | null>(() => {
  if (!quizStore.currentQuiz?.questions) return null
  return quizStore.currentQuiz.questions[currentQuestionIndex.value] || null
})

const selectedAnswer = computed(() => {
  if (!currentQuestion.value) return ''
  return quizStore.getAnswer(currentQuestion.value.questionId) || ''
})

const correctCount = computed(() => {
  return quizStore.quizResult?.answerResults?.filter(r => r.isCorrect).length || 0
})

const wrongCount = computed(() => {
  const total = quizStore.quizResult?.answerResults?.length || 0
  return total - correctCount.value
})

const progressPercentage = computed(() => {
  if (!quizStore.currentQuiz?.questions) return 0
  return ((currentQuestionIndex.value + 1) / quizStore.currentQuiz.questions.length) * 100
})

async function handleStartQuiz() {
  try {
    await quizStore.generateQuiz(selectedDifficulty.value)
    currentQuestionIndex.value = 0
    showResults.value = false
    if (quizStore.error) ElMessage.error(quizStore.error)
    else ElMessage.success(t('quiz.quizGenerated'))
  } catch (err: any) {
    ElMessage.error(err.message || t('quiz.generateFailed'))
  }
}

function handleSelectAnswer(answer: string | number | boolean | undefined) {
  if (!currentQuestion.value || answer === undefined) return
  quizStore.setAnswer(currentQuestion.value.questionId, String(answer))
}

function handleNextQuestion() {
  if (currentQuestionIndex.value < quizStore.questionCount - 1) currentQuestionIndex.value++
}

function handlePrevQuestion() {
  if (currentQuestionIndex.value > 0) currentQuestionIndex.value--
}

function handleJumpToQuestion(index: number) { 
  currentQuestionIndex.value = index 
}

async function handleSubmitQuiz() {
  if (!quizStore.allQuestionsAnswered) { ElMessage.warning(t('quiz.answerAll')); return }
  try {
    await quizStore.submitQuiz()
    showResults.value = true
    if (quizStore.error) ElMessage.error(quizStore.error)
    else ElMessage.success(t('quiz.submitSuccess'))
  } catch (err: any) { ElMessage.error(err.message || t('quiz.submitFailed')) }
}

function handleNewQuiz() { 
  quizStore.clearQuiz()
  currentQuestionIndex.value = 0
  showResults.value = false
}

function formatDate(dateStr: string): string {
  return new Date(dateStr).toLocaleDateString(undefined, { year: 'numeric', month: 'short', day: 'numeric' })
}

function getScorePercentage(score: number, total: number): number { 
  return total ? Math.round((score / total) * 100) : 0 
}

function getScoreColor(p: number): string { 
  return p >= 80 ? '#67c23a' : p >= 60 ? '#e6a23c' : '#f56c6c' 
}

function getScoreEmoji(p: number): string {
  return p >= 90 ? '🏆' : p >= 80 ? '🌟' : p >= 70 ? '👍' : p >= 60 ? '💪' : '📚'
}

function getResultMessage(p: number): string { 
  return p >= 80 ? t('quiz.excellent') : p >= 60 ? t('quiz.good') : t('quiz.keepTrying') 
}

function getDifficultyLabel(difficulty: string): string {
  const key = difficulty.toLowerCase()
  if (key === 'easy') return t('quiz.easy')
  if (key === 'medium') return t('quiz.medium')
  if (key === 'hard') return t('quiz.hard')
  return difficulty
}

function getLanguageLabel(lang?: string): string {
  const langMap: Record<string, string> = {
    en: 'English', zh: '中文', ja: '日本語', ko: '한국어',
    fr: 'Français', de: 'Deutsch', es: 'Español'
  }
  return langMap[lang || 'en'] || lang || 'English'
}

function getOptionLetter(index: number): string {
  return String.fromCharCode(65 + index)
}

function isOptionSelected(index: number): boolean {
  return selectedAnswer.value === getOptionLetter(index)
}

onMounted(() => { quizStore.fetchHistory() })
</script>

<template>
  <div class="quiz-page">
    <!-- Quiz Selection View -->
    <div v-if="!quizStore.hasActiveQuiz && !quizStore.isQuizCompleted" class="quiz-selection">
      <el-card class="header-card" shadow="hover">
        <div class="page-header">
          <div class="header-content">
            <el-icon class="header-icon"><Trophy /></el-icon>
            <div>
              <h1>{{ t('quiz.title') }}</h1>
              <p>{{ t('quiz.subtitle') }}</p>
            </div>
          </div>
          <div class="header-decoration">
            <span class="decoration-emoji">📝</span>
          </div>
        </div>
      </el-card>
      
      <el-row :gutter="24">
        <el-col :xs="24" :lg="16">
          <el-card class="config-card" shadow="hover">
            <template #header>
              <div class="card-header">
                <el-icon><Star /></el-icon>
                <span>{{ t('quiz.startNewQuiz') }}</span>
              </div>
            </template>
            
            <div class="language-section">
              <span class="label">{{ t('quiz.targetLang') }}:</span>
              <div class="lang-selector">
                <span class="lang-flag">{{ languageOptions.find(l => l.value === quizStore.targetLang)?.flag || '🌐' }}</span>
                <el-select v-model="quizStore.targetLang" class="language-select">
                  <el-option v-for="lang in languageOptions" :key="lang.value" :label="lang.label" :value="lang.value" />
                </el-select>
              </div>
            </div>
            
            <div class="difficulty-section">
              <h3>{{ t('quiz.selectDifficulty') }}</h3>
              <div class="difficulty-cards">
                <div
                  v-for="opt in difficultyOptions"
                  :key="opt.value"
                  :class="['difficulty-card', { active: selectedDifficulty === opt.value }]"
                  :style="{ '--accent-color': opt.color, '--bg-color': opt.bgColor }"
                  @click="selectedDifficulty = opt.value"
                >
                  <div class="difficulty-icon">{{ opt.icon }}</div>
                  <div class="difficulty-content">
                    <span class="difficulty-label">{{ opt.label }}</span>
                    <span class="difficulty-desc">{{ opt.description }}</span>
                  </div>
                  <div class="difficulty-check">
                    <el-icon v-if="selectedDifficulty === opt.value"><Check /></el-icon>
                  </div>
                </div>
              </div>
            </div>
            
            <el-button 
              type="primary" 
              size="large" 
              :loading="quizStore.loading"
              class="start-btn"
              @click="handleStartQuiz"
            >
              <el-icon><Trophy /></el-icon>
              {{ t('quiz.startQuiz') }}
            </el-button>
          </el-card>
        </el-col>
        
        <el-col :xs="24" :lg="8">
          <el-card class="history-card" shadow="hover">
            <template #header>
              <div class="card-header">
                <div class="header-left">
                  <el-icon><TrendCharts /></el-icon>
                  <span>{{ t('quiz.recentQuizzes') }}</span>
                </div>
                <el-badge :value="quizStore.historyCount" class="history-badge" />
              </div>
            </template>
            
            <el-scrollbar height="300px">
              <div 
                v-for="quiz in quizStore.history" 
                :key="quiz.id" 
                class="history-item"
              >
                <div class="history-header">
                  <div class="history-tags">
                    <el-tag size="small">{{ getDifficultyLabel(quiz.difficulty) }}</el-tag>
                    <el-tag size="small" type="info">{{ getLanguageLabel(quiz.targetLang) }}</el-tag>
                  </div>
                  <span class="history-date">{{ formatDate(quiz.createdAt) }}</span>
                </div>
                <div class="history-progress">
                  <el-progress 
                    :percentage="getScorePercentage(quiz.userScore || 0, quiz.totalScore)" 
                    :color="getScoreColor(getScorePercentage(quiz.userScore || 0, quiz.totalScore))"
                    :stroke-width="8"
                  />
                  <span class="history-score">
                    {{ quiz.userScore || 0 }}/{{ quiz.totalScore }}
                  </span>
                </div>
              </div>
              <el-empty 
                v-if="!quizStore.history.length" 
                :description="t('quiz.noHistory')" 
                :image-size="80" 
              />
            </el-scrollbar>
          </el-card>
        </el-col>
      </el-row>
    </div>

    <!-- Quiz Active View -->
    <div v-else-if="quizStore.hasActiveQuiz && !quizStore.isQuizCompleted" class="quiz-active">
      <el-card class="quiz-card" shadow="hover">
        <template #header>
          <div class="quiz-header">
            <div class="quiz-progress-info">
              <el-icon><Clock /></el-icon>
              <span>{{ t('quiz.questionOf', { current: currentQuestionIndex + 1, total: quizStore.questionCount }) }}</span>
            </div>
            <div class="quiz-progress-bar">
              <el-progress 
                :percentage="progressPercentage" 
                :stroke-width="6"
                :show-text="false"
                :color="getScoreColor(progressPercentage)"
              />
            </div>
            <div class="quiz-stats">
              <span class="stat answered">
                <el-icon><Check /></el-icon>
                {{ quizStore.answeredCount }}
              </span>
              <span class="stat remaining">
                <el-icon><Close /></el-icon>
                {{ quizStore.questionCount - quizStore.answeredCount }}
              </span>
            </div>
          </div>
        </template>
        
        <div class="question-nav">
          <el-button
            v-for="(q, i) in quizStore.currentQuiz?.questions"
            :key="q.questionId"
            :type="quizStore.getAnswer(q.questionId) ? 'success' : i === currentQuestionIndex ? 'primary' : 'default'"
            circle
            size="small"
            @click="handleJumpToQuestion(i)"
          >
            {{ i + 1 }}
          </el-button>
        </div>
        
        <div v-if="currentQuestion" class="question-content">
          <div class="question-badge">
            <el-tag size="small" type="info">Question {{ currentQuestionIndex + 1 }}</el-tag>
          </div>
          <h2 class="question-text">{{ currentQuestion.question }}</h2>
          
          <div class="options-group">
            <div
              v-for="(opt, i) in currentQuestion.options"
              :key="i"
              :class="['option-item', { 
                selected: isOptionSelected(i),
                correct: quizStore.isQuizCompleted && String.fromCharCode(65 + i) === currentQuestion.correctAnswer
              }]"
              @click="handleSelectAnswer(String.fromCharCode(65 + i))"
            >
              <span class="option-letter">{{ getOptionLetter(i) }}</span>
              <span class="option-text">{{ opt }}</span>
              <el-icon v-if="isOptionSelected(i)" class="option-check"><Check /></el-icon>
            </div>
          </div>
        </div>
        
        <div class="navigation-buttons">
          <el-button 
            :disabled="currentQuestionIndex === 0" 
            @click="handlePrevQuestion"
          >
            <el-icon><ArrowLeft /></el-icon>
            {{ t('quiz.previous') }}
          </el-button>
          
          <div class="nav-center">
            <el-button
              v-for="(q, i) in quizStore.currentQuiz?.questions"
              :key="q.questionId"
              :type="quizStore.getAnswer(q.questionId) ? 'success' : i === currentQuestionIndex ? 'primary' : 'default'"
              circle
              size="small"
              class="nav-dot"
              @click="handleJumpToQuestion(i)"
            >
              {{ i + 1 }}
            </el-button>
          </div>
          
          <el-button 
            v-if="currentQuestionIndex < quizStore.questionCount - 1" 
            type="primary" 
            @click="handleNextQuestion"
          >
            {{ t('quiz.next') }}
            <el-icon><ArrowRight /></el-icon>
          </el-button>
          <el-button 
            v-else 
            type="success" 
            :disabled="!quizStore.allQuestionsAnswered" 
            @click="handleSubmitQuiz"
          >
            <el-icon><Trophy /></el-icon>
            {{ t('quiz.submitQuiz') }}
          </el-button>
        </div>
      </el-card>
    </div>

    <!-- Quiz Results View -->
    <div v-else-if="quizStore.isQuizCompleted && quizStore.quizResult" class="quiz-results">
      <el-card class="results-card" shadow="hover">
        <div class="results-content">
          <div class="results-header">
            <div class="results-emoji">{{ getScoreEmoji(getScorePercentage(quizStore.quizResult.userScore, quizStore.quizResult.totalScore)) }}</div>
            <h1>{{ t('quiz.quizComplete') }}</h1>
            <p class="results-message">{{ getResultMessage(getScorePercentage(quizStore.quizResult.userScore, quizStore.quizResult.totalScore)) }}</p>
          </div>
          
          <div class="score-circle">
            <svg viewBox="0 0 100 100">
              <circle class="score-bg" cx="50" cy="50" r="45" />
              <circle 
                class="score-progress" 
                cx="50" cy="50" r="45"
                :style="{ 
                  strokeDashoffset: 283 - (283 * getScorePercentage(quizStore.quizResult.userScore, quizStore.quizResult.totalScore) / 100),
                  stroke: getScoreColor(getScorePercentage(quizStore.quizResult.userScore, quizStore.quizResult.totalScore))
                }"
              />
            </svg>
            <div class="score-content">
              <span class="score-percentage">{{ getScorePercentage(quizStore.quizResult.userScore, quizStore.quizResult.totalScore) }}%</span>
              <span class="score-fraction">{{ quizStore.quizResult.userScore }} / {{ quizStore.quizResult.totalScore }}</span>
            </div>
          </div>
          
          <el-row :gutter="20" class="stats-row">
            <el-col :span="8">
              <div class="stat-item correct">
                <el-icon><Check /></el-icon>
                <div class="stat-value">{{ correctCount }}</div>
                <div class="stat-label">{{ t('quiz.correct') }}</div>
              </div>
            </el-col>
            <el-col :span="8">
              <div class="stat-item wrong">
                <el-icon><Close /></el-icon>
                <div class="stat-value">{{ wrongCount }}</div>
                <div class="stat-label">{{ t('quiz.wrong') }}</div>
              </div>
            </el-col>
            <el-col :span="8">
              <div class="stat-item total">
                <el-icon><Trophy /></el-icon>
                <div class="stat-value">{{ quizStore.quizResult.answerResults?.length || 0 }}</div>
                <div class="stat-label">{{ t('quiz.total') }}</div>
              </div>
            </el-col>
          </el-row>
          
          <div class="results-actions">
            <el-button type="primary" size="large" @click="handleNewQuiz">
              <el-icon><Refresh /></el-icon>
              {{ t('quiz.takeAnother') }}
            </el-button>
            <el-button size="large">
              <el-icon><Download /></el-icon>
              Export Results
            </el-button>
          </div>
        </div>
      </el-card>
    </div>
  </div>
</template>

<style scoped>
.quiz-page {
  padding: 0;
}

/* Header Card */
.header-card {
  margin-bottom: 24px;
  background: var(--color-primary);
  border: none;
}

.header-card :deep(.el-card__body) {
  padding: 24px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-content {
  display: flex;
  align-items: center;
  gap: 16px;
}

.header-icon {
  font-size: 40px;
  color: white;
}

.header-content h1 {
  margin: 0 0 4px 0;
  font-size: var(--font-size-2xl);
  font-weight: 700;
  color: white;
}

.header-content p {
  margin: 0;
  font-size: var(--font-size-sm);
  color: rgba(255, 255, 255, 0.9);
}

.header-decoration {
  font-size: 64px;
  opacity: 0.3;
}

.config-card,
.history-card {
  height: 100%;
  transition: all 0.3s ease;
}

.config-card:hover,
.history-card:hover {
  box-shadow: var(--shadow-md);
}

.card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

/* Language Section */
.language-section {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 24px;
  padding: 16px;
  background: var(--bg-tertiary);
  border-radius: var(--radius-lg);
}

.language-section .label {
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
  width: 140px;
}

/* Difficulty Section */
.difficulty-section {
  margin-bottom: 24px;
}

.difficulty-section h3 {
  font-size: var(--font-size-md);
  font-weight: 600;
  margin: 0 0 16px 0;
  color: var(--text-primary);
}

.difficulty-cards {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.difficulty-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px;
  background: var(--bg-tertiary);
  border-radius: var(--radius-lg);
  cursor: pointer;
  transition: all 0.3s ease;
  border: 2px solid transparent;
  position: relative;
  overflow: hidden;
}

.difficulty-card::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 4px;
  background: var(--accent-color);
  opacity: 0;
  transition: opacity 0.3s;
}

.difficulty-card:hover {
  transform: translateX(4px);
  border-color: var(--accent-color);
}

.difficulty-card:hover::before {
  opacity: 1;
}

.difficulty-card.active {
  border-color: var(--accent-color);
  background: var(--bg-color);
}

.difficulty-card.active::before {
  opacity: 1;
}

.difficulty-icon {
  font-size: 32px;
  width: 48px;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: white;
  border-radius: var(--radius-md);
}

.difficulty-content {
  flex: 1;
}

.difficulty-label {
  display: block;
  font-size: var(--font-size-md);
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 4px;
}

.difficulty-desc {
  font-size: var(--font-size-sm);
  color: var(--text-muted);
}

.difficulty-check {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: var(--accent-color);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  opacity: 0;
  transform: scale(0.5);
  transition: all 0.3s;
}

.difficulty-card.active .difficulty-check {
  opacity: 1;
  transform: scale(1);
}

.start-btn {
  width: 100%;
  height: 48px;
  font-size: var(--font-size-md);
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

/* History */
.history-card {
  margin-top: 24px;
}

.history-item {
  padding: 16px;
  background: var(--bg-tertiary);
  border-radius: var(--radius-md);
  margin-bottom: 12px;
  transition: all 0.3s;
}

.history-item:hover {
  transform: translateX(4px);
  box-shadow: var(--shadow-sm);
}

.history-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.history-tags {
  display: flex;
  gap: 8px;
}

.history-date {
  font-size: var(--font-size-xs);
  color: var(--text-muted);
}

.history-progress {
  display: flex;
  align-items: center;
  gap: 12px;
}

.history-progress :deep(.el-progress) {
  flex: 1;
}

.history-score {
  font-size: var(--font-size-sm);
  font-weight: 600;
  color: var(--text-primary);
  min-width: 60px;
  text-align: right;
}

/* Quiz Active */
.quiz-active {
  max-width: 800px;
  margin: 0 auto;
}

.quiz-card {
  transition: all 0.3s ease;
}

.quiz-card:hover {
  box-shadow: var(--shadow-md);
}

.quiz-header {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 8px 0;
}

.quiz-progress-info {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 500;
  white-space: nowrap;
}

.quiz-progress-bar {
  flex: 1;
}

.quiz-stats {
  display: flex;
  gap: 16px;
}

.quiz-stats .stat {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: var(--font-size-sm);
  padding: 4px 12px;
  border-radius: 16px;
}

.quiz-stats .stat.answered {
  background: #ecfdf5;
  color: #67c23a;
}

.quiz-stats .stat.remaining {
  background: #fef0f0;
  color: #f56c6c;
}

.question-nav {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  padding: 16px 0;
  border-bottom: 1px solid var(--border-color-light);
}

.nav-center {
  flex: 1;
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 8px;
}

.nav-dot {
  min-width: 32px;
}

.question-content {
  padding: 32px 0;
}

.question-badge {
  margin-bottom: 16px;
}

.question-text {
  font-size: var(--font-size-xl);
  font-weight: 600;
  color: var(--text-primary);
  line-height: 1.6;
  margin: 0 0 32px 0;
}

.options-group {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.option-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px 24px;
  background: var(--bg-tertiary);
  border-radius: var(--radius-lg);
  cursor: pointer;
  transition: all 0.3s ease;
  border: 2px solid transparent;
  position: relative;
  overflow: hidden;
}

.option-item:hover {
  transform: translateX(8px);
  border-color: var(--color-accent);
  background: var(--bg-secondary);
}

.option-item.selected {
  border-color: var(--color-primary);
  background: rgba(59, 130, 246, 0.1);
}

.option-item.correct {
  border-color: #67c23a;
  background: rgba(103, 194, 58, 0.1);
}

.option-letter {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: var(--bg-secondary);
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: var(--font-size-md);
  color: var(--text-primary);
  flex-shrink: 0;
  transition: all 0.3s;
}

.option-item.selected .option-letter {
  background: var(--color-primary);
  color: white;
}

.option-item.correct .option-letter {
  background: #67c23a;
  color: white;
}

.option-text {
  flex: 1;
  font-size: var(--font-size-md);
  color: var(--text-primary);
}

.option-check {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: var(--color-primary);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transform: scale(0.5);
  transition: all 0.3s;
}

.option-item.selected .option-check {
  opacity: 1;
  transform: scale(1);
}

.navigation-buttons {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 20px;
  border-top: 1px solid var(--border-color-light);
}

/* Results */
.quiz-results {
  max-width: 500px;
  margin: 0 auto;
}

.results-card {
  overflow: hidden;
}

.results-content {
  text-align: center;
  padding: 20px;
}

.results-header {
  margin-bottom: 32px;
}

.results-emoji {
  font-size: 64px;
  margin-bottom: 16px;
  animation: bounce 1s infinite;
}

@keyframes bounce {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-10px); }
}

.results-header h1 {
  font-size: var(--font-size-2xl);
  font-weight: 700;
  color: var(--text-primary);
  margin: 0 0 8px 0;
}

.results-message {
  font-size: var(--font-size-lg);
  color: var(--text-secondary);
  margin: 0;
}

.score-circle {
  position: relative;
  width: 180px;
  height: 180px;
  margin: 0 auto 32px;
}

.score-circle svg {
  transform: rotate(-90deg);
}

.score-bg {
  fill: none;
  stroke: var(--bg-tertiary);
  stroke-width: 8;
}

.score-progress {
  fill: none;
  stroke-width: 8;
  stroke-linecap: round;
  stroke-dasharray: 283;
  transition: stroke-dashoffset 1s ease-out;
}

.score-content {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  text-align: center;
}

.score-percentage {
  display: block;
  font-size: 36px;
  font-weight: 700;
  color: var(--text-primary);
  line-height: 1;
}

.score-fraction {
  display: block;
  font-size: var(--font-size-sm);
  color: var(--text-muted);
  margin-top: 4px;
}

.stats-row {
  margin-bottom: 32px;
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 16px;
  background: var(--bg-tertiary);
  border-radius: var(--radius-lg);
}

.stat-item .el-icon {
  font-size: 24px;
  margin-bottom: 8px;
}

.stat-item.correct .el-icon {
  color: #67c23a;
}

.stat-item.wrong .el-icon {
  color: #f56c6c;
}

.stat-item.total .el-icon {
  color: var(--color-accent);
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: var(--text-primary);
  line-height: 1;
}

.stat-label {
  font-size: var(--font-size-sm);
  color: var(--text-muted);
  margin-top: 4px;
}

.results-actions {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.results-actions .el-button {
  width: 100%;
  height: 48px;
}

/* Responsive */
@media (max-width: 768px) {
  .quiz-page {
    padding: 0;
  }
  
  .page-header {
    flex-direction: column;
    text-align: center;
    gap: 16px;
  }
  
  .header-decoration {
    display: none;
  }
  
  .difficulty-cards {
    gap: 8px;
  }
  
  .difficulty-card {
    padding: 16px;
  }
  
  .question-text {
    font-size: var(--font-size-lg);
  }
  
  .option-item {
    padding: 16px;
  }
  
  .score-circle {
    width: 150px;
    height: 150px;
  }
  
  .score-percentage {
    font-size: 28px;
  }
}
</style>
