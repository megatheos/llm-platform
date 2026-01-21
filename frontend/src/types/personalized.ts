// ============================================
// Memory System Types (需求 1.6)
// ============================================

export interface ReviewItem {
  id: number
  contentId: number
  contentType: 'word' | 'dialogue' | 'quiz'
  content: string
  translation?: string
  masteryLevel: number // 0-5, 5 being highest mastery
  nextReviewDate: string
  lastReviewDate?: string
  reviewCount: number
  easeFactor: number // SM-2 algorithm ease factor
  interval: number // days until next review
}

export interface ReviewCardData {
  item: ReviewItem
  isDue: boolean
  daysUntilReview: number
  suggestedAction: 'review' | 'restudy' | 'mastered'
}

export interface ReviewStatistics {
  totalItems: number
  dueToday: number
  masteredCount: number
  learningCount: number
  newCount: number
  averageMastery: number
  reviewAccuracy: number // percentage of correct recalls
  streakDays: number
}

export interface MemoryState {
  totalReviews: number
  reviewsToday: number
  reviewsThisWeek: number
  reviewsThisMonth: number
  averageSessionDuration: number
}

// ============================================
// Learning Profile Types (需求 2.5, 2.6)
// ============================================

export interface LearningProfile {
  id: number
  userId: number
  totalLearningDays: number
  totalStudyHours: number
  totalWordsLearned: number
  totalDialoguesCompleted: number
  totalQuizzesCompleted: number
  averageQuizScore: number
  preferredLanguages: string[]
  strongestAreas: KnowledgeArea[]
  weakestAreas: KnowledgeArea[]
  learningStyle: LearningStyle
  peakHours: string[]
  averageSessionLength: number
  createdAt: string
  updatedAt: string
}

export interface KnowledgeArea {
  category: string
  displayName: string
  score: number // 0-100
  totalItems: number
  masteredItems: number
  trend: 'improving' | 'stable' | 'declining'
}

export type LearningStyle = 'visual' | 'auditory' | 'reading' | 'kinesthetic' | 'mixed'

export interface LearningInsights {
  profile: LearningProfile
  weeklyProgress: WeeklyProgress
  recommendations: string[]
  achievements: Achievement[]
  nextMilestones: Milestone[]
}

export interface WeeklyProgress {
  weekStartDate: string
  weekEndDate: string
  days: DailyProgress[]
  totalActivities: number
  totalTimeMinutes: number
  averageDailyActivities: number
  improvementRate: number
}

export interface DailyProgress {
  date: string
  activities: number
  timeMinutes: number
  wordsLearned: number
  quizzesTaken: number
  dialoguesCompleted: number
  score?: number
}

export interface ProgressMetrics {
  totalWords: number
  totalQuizzes: number
  averageScore: number
  totalStudyTime: number
  currentStreak: number
  longestStreak: number
  completionRate: number
}

export interface Milestone {
  id: number
  title: string
  description: string
  type: 'words' | 'quizzes' | 'streak' | 'hours' | 'score'
  targetValue: number
  currentValue: number
  isCompleted: boolean
  completedAt?: string
  icon?: string
}

// ============================================
// Study Plan Types (需求 3.1, 3.7)
// ============================================

export interface StudyPlan {
  id: number
  userId: number
  title: string
  description?: string
  startDate: string
  endDate: string
  status: 'draft' | 'active' | 'paused' | 'completed'
  goals: StudyGoal[]
  dailyTasks: DailyTask[]
  createdAt: string
  updatedAt: string
}

export interface StudyGoal {
  id: number
  planId: number
  type: 'words' | 'quizzes' | 'dialogues' | 'time' | 'score'
  targetValue: number
  currentValue: number
  deadline: string
  isCompleted: boolean
  completedAt?: string
}

export interface DailyTask {
  id: number
  planId: number
  date: string
  type: 'review' | 'new' | 'quiz' | 'dialogue'
  targetCount: number
  completedCount: number
  duration?: number // minutes
  status: 'pending' | 'in_progress' | 'completed' | 'skipped'
  items?: TaskItem[]
}

export interface TaskItem {
  id: number
  taskId: number
  contentId: number
  contentType: 'word' | 'dialogue' | 'quiz'
  content: string
  status: 'pending' | 'completed' | 'skipped'
  completedAt?: string
}

export interface PlanAdjustment {
  id: number
  planId: number
  type: 'goal_change' | 'schedule_change' | 'difficulty_adjustment' | 'content_update'
  reason: string
  oldValue?: string
  newValue?: string
  suggestedAt: string
  isRead: boolean
}

export interface GoalSettingData {
  dailyWordTarget: number
  dailyQuizTarget: number
  dailyDialogueTarget: number
  weeklyStudyHours: number
  preferredStudyTime: 'morning' | 'afternoon' | 'evening' | 'any'
  focusAreas: string[]
  difficultyLevel: 'easy' | 'medium' | 'hard' | 'adaptive'
}

// ============================================
// Motivation Types (需求 5.5)
// ============================================

export interface Achievement {
  id: number
  userId: number
  achievementId: string
  title: string
  description: string
  icon: string
  category: AchievementCategory
  rarity: 'common' | 'rare' | 'epic' | 'legendary'
  earnedAt: string
  progress?: number
  maxProgress?: number
}

export type AchievementCategory =
  | 'learning'
  | 'streak'
  | 'mastery'
  | 'exploration'
  | 'social'
  | 'special'

export interface AchievementDefinition {
  id: string
  title: string
  description: string
  icon: string
  category: AchievementCategory
  rarity: 'common' | 'rare' | 'epic' | 'legendary'
  criteria: {
    type: string
    target: number
  }
}

export interface LearningHeatmapData {
  year: number
  months: HeatmapMonth[]
  totalActivities: number
  maxDailyActivities: number
}

export interface HeatmapMonth {
  month: number
  year: number
  weeks: HeatmapWeek[]
}

export interface HeatmapWeek {
  week: number
  days: HeatmapDay[]
}

export interface HeatmapDay {
  date: string
  count: number
  level: 0 | 1 | 2 | 3 | 4 // 0=none, 1=low, 2=medium, 3=high, 4=highest
}

export interface StreakInfo {
  currentStreak: number
  longestStreak: number
  longestStreakDate?: string
  totalLearningDays: number
  lastActivityDate: string
  streakStatus: 'active' | 'at_risk' | 'broken'
  daysUntilRisk: number
}

export interface UserMotivationState {
  achievements: Achievement[]
  streak: StreakInfo
  recentBadges: Achievement[]
  unlockedCategories: AchievementCategory[]
}
