import request from './request'
import type {
  Result,
  LearningProfile,
  LearningInsights,
  ProgressMetrics,
  KnowledgeArea,
  WeeklyProgress
} from '@/types'

/**
 * Get current user's learning profile
 * @returns Learning profile data
 */
export function getLearningProfile(): Promise<Result<LearningProfile>> {
  return request.get('/profile').then(res => res.data)
}

/**
 * Get learning insights and analytics
 * @param days Number of days to analyze (default: 30)
 * @returns Learning insights report
 */
export function getLearningInsights(days?: number): Promise<Result<LearningInsights>> {
  return request.get('/profile/insights', { params: { days } }).then(res => res.data)
}

/**
 * Get progress metrics
 * @returns Progress metrics for dashboard
 */
export function getProgressMetrics(): Promise<Result<ProgressMetrics>> {
  return request.get('/profile/progress').then(res => res.data)
}

/**
 * Get weak areas analysis
 * @returns List of weak knowledge areas
 */
export function getWeakAreas(): Promise<Result<KnowledgeArea[]>> {
  return request.get('/profile/weak-areas').then(res => res.data)
}

/**
 * Get strong areas analysis
 * @returns List of strong knowledge areas
 */
export function getStrongAreas(): Promise<Result<KnowledgeArea[]>> {
  return request.get('/profile/strong-areas').then(res => res.data)
}

/**
 * Get weekly progress data
 * @param weekOffset Week offset from current week (0=current, -1=previous, etc.)
 * @returns Weekly progress data
 */
export function getWeeklyProgress(weekOffset?: number): Promise<Result<WeeklyProgress>> {
  return request.get('/profile/weekly-progress', { params: { weekOffset } }).then(res => res.data)
}

/**
 * Update learning profile preferences
 * @param data Profile data to update
 * @returns Updated profile
 */
export function updateLearningProfile(
  data: Partial<LearningProfile>
): Promise<Result<LearningProfile>> {
  return request.put('/profile', data).then(res => res.data)
}

/**
 * Record a learning activity
 * @param activityType Type of activity
 * @param activityData Activity details
 * @returns Success status
 */
export function recordActivity(
  activityType: string,
  activityData: Record<string, unknown>
): Promise<Result<{ recorded: boolean }>> {
  return request.post('/profile/activities', { activityType, ...activityData }).then(res => res.data)
}
