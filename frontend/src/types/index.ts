// Common API response type
export interface Result<T> {
  code: number
  message: string
  data: T
}

// User types
export interface User {
  id: number
  username: string
  email: string
  avatar?: string
  createdAt?: string
  updatedAt?: string
}

// Auth types
export interface LoginRequest {
  username: string
  password: string
}

export interface LoginResponse {
  token: string
  user: User
}

export interface RegisterRequest {
  username: string
  password: string
  email: string
}

export interface RegisterResponse {
  user: User
}

// Word types
export interface Word {
  id: number
  word: string
  sourceLang: string
  targetLang: string
  definition: string
  translation: string
  examples: string
  pronunciation?: string
  createdAt?: string
}

export interface WordQueryRequest {
  word: string
  sourceLang: string
  targetLang: string
}

export interface WordHistory {
  id: number
  wordId: number
  word: string
  sourceLang: string
  targetLang: string
  translation: string
  queryTime: string
}

// Scenario and Dialogue types
export interface Scenario {
  id: number
  name: string
  description: string
  category: string
  isPreset: boolean
  createdBy?: number
  createdAt?: string
}

export interface CreateScenarioRequest {
  name: string
  description: string
  category: string
}

export interface DialogueMessage {
  role: 'user' | 'assistant'
  content: string
  timestamp: string
}

export interface DialogueSession {
  id: number
  userId: number
  scenarioId: number
  messages: DialogueMessage[]
  startedAt: string
  endedAt?: string
}

// Quiz types
export type DifficultyLevel = 'easy' | 'medium' | 'hard'

export interface QuizQuestion {
  questionId: number
  question: string
  options: string[]
  correctAnswer: string
}

export interface Quiz {
  id: number
  userId: number
  difficulty: DifficultyLevel
  targetLang?: string
  questions: QuizQuestion[]
  totalScore: number
  userScore?: number
  createdAt: string
  completedAt?: string
}

export interface QuizAnswer {
  questionId: number
  answer: string
}

export interface QuizResult {
  quizId: number
  userScore: number
  totalScore: number
  difficulty: string
  answerResults: AnswerResult[]
  completedAt?: string
}

export interface AnswerResult {
  questionId: number
  question: string
  userAnswer: string
  correctAnswer: string
  isCorrect: boolean
}

// Learning Record types
export type ActivityType = 'WORD_QUERY' | 'DIALOGUE' | 'QUIZ'

export interface LearningRecord {
  id: number
  activityType: ActivityType
  activityId: number
  activityTime: string
  activityDetails?: WordActivityDetails | DialogueActivityDetails | QuizActivityDetails
}

export interface WordActivityDetails {
  word: string
  sourceLang: string
  targetLang: string
  translation: string
}

export interface DialogueActivityDetails {
  sessionId: number
  scenarioId: number
  scenarioName: string
  scenarioCategory?: string
  startedAt?: string
  endedAt?: string
}

export interface QuizActivityDetails {
  quizId: number
  difficulty: string
  totalScore: number
  userScore: number
  completedAt?: string
}

export interface LearningRecordsResponse {
  records: LearningRecord[]
  total: number
  page: number
  pageSize: number
  totalPages: number
}

export interface LearningStatistics {
  totalWordQueries: number
  totalDialogueSessions: number
  totalQuizzes: number
  averageQuizScore: number
  totalActivities: number
  firstActivityDate?: string
  lastActivityDate?: string
  activitiesLast7Days: number
  activitiesLast30Days: number
}

export interface RecordQueryParams {
  activityType?: ActivityType
  startDate?: string
  endDate?: string
  page?: number
  pageSize?: number
}
