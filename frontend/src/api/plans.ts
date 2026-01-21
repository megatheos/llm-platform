import request from './request'
import type {
  Result,
  StudyPlan,
  DailyTask,
  StudyGoal,
  PlanAdjustment,
  GoalSettingData
} from '@/types'

/**
 * Get current active study plan
 * @returns Active study plan
 */
export function getActivePlan(): Promise<Result<StudyPlan | null>> {
  return request.get('/plans/active').then(res => res.data)
}

/**
 * Get all study plans
 * @param status Filter by status
 * @returns List of study plans
 */
export function getPlans(status?: 'draft' | 'active' | 'paused' | 'completed'): Promise<Result<StudyPlan[]>> {
  return request.get('/plans', { params: { status } }).then(res => res.data)
}

/**
 * Get a specific study plan
 * @param planId ID of the plan
 * @returns Study plan details
 */
export function getPlan(planId: number): Promise<Result<StudyPlan>> {
  return request.get(`/plans/${planId}`).then(res => res.data)
}

/**
 * Create a new study plan
 * @param planData Plan data
 * @returns Created plan
 */
export function createPlan(planData: Partial<StudyPlan>): Promise<Result<StudyPlan>> {
  return request.post('/plans', planData).then(res => res.data)
}

/**
 * Update a study plan
 * @param planId ID of the plan
 * @param planData Updated plan data
 * @returns Updated plan
 */
export function updatePlan(
  planId: number,
  planData: Partial<StudyPlan>
): Promise<Result<StudyPlan>> {
  return request.put(`/plans/${planId}`, planData).then(res => res.data)
}

/**
 * Delete a study plan
 * @param planId ID of the plan
 * @returns Success status
 */
export function deletePlan(planId: number): Promise<Result<{ deleted: boolean }>> {
  return request.delete(`/plans/${planId}`).then(res => res.data)
}

/**
 * Get daily tasks for a specific date
 * @param date Date string (YYYY-MM-DD)
 * @param planId Optional plan ID filter
 * @returns List of daily tasks
 */
export function getDailyTasks(
  date: string,
  planId?: number
): Promise<Result<DailyTask[]>> {
  return request.get('/plans/daily-tasks', { params: { date, planId } }).then(res => res.data)
}

/**
 * Get today's tasks
 * @returns Today's daily tasks
 */
export function getTodayTasks(): Promise<Result<DailyTask[]>> {
  return request.get('/plans/today-tasks').then(res => res.data)
}

/**
 * Update task completion status
 * @param taskId ID of the task
 * @param status New status
 * @returns Updated task
 */
export function updateTaskStatus(
  taskId: number,
  status: 'pending' | 'in_progress' | 'completed' | 'skipped'
): Promise<Result<DailyTask>> {
  return request.patch(`/plans/tasks/${taskId}/status`, { status }).then(res => res.data)
}

/**
 * Get study goals for active plan
 * @returns List of study goals
 */
export function getStudyGoals(): Promise<Result<StudyGoal[]>> {
  return request.get('/plans/goals').then(res => res.data)
}

/**
 * Update a study goal
 * @param goalId ID of the goal
 * @param goalData Updated goal data
 * @returns Updated goal
 */
export function updateStudyGoal(
  goalId: number,
  goalData: Partial<StudyGoal>
): Promise<Result<StudyGoal>> {
  return request.put(`/plans/goals/${goalId}`, goalData).then(res => res.data)
}

/**
 * Get plan adjustment notifications
 * @param unreadOnly Only return unread adjustments
 * @returns List of plan adjustments
 */
export function getPlanAdjustments(unreadOnly?: boolean): Promise<Result<PlanAdjustment[]>> {
  return request.get('/plans/adjustments', { params: { unreadOnly } }).then(res => res.data)
}

/**
 * Mark adjustment as read
 * @param adjustmentId ID of the adjustment
 * @returns Success status
 */
export function markAdjustmentRead(adjustmentId: number): Promise<Result<{ read: boolean }>> {
  return request.patch(`/plans/adjustments/${adjustmentId}/read`).then(res => res.data)
}

/**
 * Save goal settings
 * @param settings Goal setting data
 * @returns Saved settings
 */
export function saveGoalSettings(settings: GoalSettingData): Promise<Result<GoalSettingData>> {
  return request.post('/plans/goal-settings', settings).then(res => res.data)
}

/**
 * Get goal settings
 * @returns Current goal settings
 */
export function getGoalSettings(): Promise<Result<GoalSettingData | null>> {
  return request.get('/plans/goal-settings').then(res => res.data)
}
