import request from './request'
import type {
  Result,
  Achievement,
  LearningHeatmapData,
  StreakInfo,
  UserMotivationState,
  AchievementDefinition
} from '@/types'

/**
 * Get user motivation state
 * @returns Motivation state including achievements and streak
 */
export function getMotivationState(): Promise<Result<UserMotivationState>> {
  return request.get('/motivation/state').then(res => res.data)
}

/**
 * Get all achievements
 * @returns List of all achievements
 */
export function getAchievements(): Promise<Result<Achievement[]>> {
  return request.get('/motivation/achievements').then(res => res.data)
}

/**
 * Get achievement definitions (for badge wall)
 * @returns All achievement definitions
 */
export function getAchievementDefinitions(): Promise<Result<AchievementDefinition[]>> {
  return request.get('/motivation/achievements/definitions').then(res => res.data)
}

/**
 * Get user's earned achievements
 * @returns List of earned achievements
 */
export function getEarnedAchievements(): Promise<Result<Achievement[]>> {
  return request.get('/motivation/achievements/earned').then(res => res.data)
}

/**
 * Get recent achievements (last 30 days)
 * @returns Recently earned achievements
 */
export function getRecentAchievements(): Promise<Result<Achievement[]>> {
  return request.get('/motivation/achievements/recent').then(res => res.data)
}

/**
 * Get streak information
 * @returns Current and historical streak data
 */
export function getStreakInfo(): Promise<Result<StreakInfo>> {
  return request.get('/motivation/streak').then(res => res.data)
}

/**
 * Get learning heatmap data
 * @param year Year to fetch data for (default: current year)
 * @returns Learning heatmap data
 */
export function getLearningHeatmap(year?: number): Promise<Result<LearningHeatmapData>> {
  return request.get('/motivation/heatmap', { params: { year } }).then(res => res.data)
}

/**
 * Get activity count for a specific period
 * @param startDate Start date
 * @param endDate End date
 * @returns Daily activity counts
 */
export function getActivityCount(
  startDate: string,
  endDate: string
): Promise<Result<{ date: string; count: number }[]>> {
  return request.get('/motivation/activity/count', { params: { startDate, endDate } }).then(res => res.data)
}

/**
 * Get achievement progress
 * @param achievementId ID of the achievement
 * @returns Achievement progress data
 */
export function getAchievementProgress(
  achievementId: string
): Promise<Result<{ current: number; target: number; percentage: number }>> {
  return request.get(`/motivation/achievements/${achievementId}/progress`).then(res => res.data)
}

/**
 * Trigger motivation notification
 * @param type Type of motivation
 * @returns Generated motivation content
 */
export function getMotivationMessage(type: 'daily' | 'streak' | 'achievement' | 'reminder'): Promise<Result<{ message: string; type: string }>> {
  return request.get('/motivation/message', { params: { type } }).then(res => res.data)
}

/**
 * Claim daily bonus
 * @returns Claim result with rewards
 */
export function claimDailyBonus(): Promise<Result<{ claimed: boolean; rewards: string[] }>> {
  return request.post('/motivation/daily-bonus').then(res => res.data)
}

/**
 * Get leaderboard data
 * @param type Leaderboard type (friends, global, weekly)
 * @returns Leaderboard entries
 */
export function getLeaderboard(type: 'friends' | 'global' | 'weekly'): Promise<Result<{
  entries: { userId: number; username: string; avatar?: string; score: number; rank: number }[]
  userRank?: number
}>> {
  return request.get('/motivation/leaderboard', { params: { type } }).then(res => res.data)
}
