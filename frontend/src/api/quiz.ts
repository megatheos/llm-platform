import request from './request'
import type { Result, Quiz, QuizAnswer, QuizResult, DifficultyLevel } from '@/types'

/**
 * Generate a new quiz
 * @param difficulty Quiz difficulty level
 * @returns Generated quiz with questions
 */
export function generateQuiz(difficulty: DifficultyLevel): Promise<Result<Quiz>> {
  return request.post('/quiz/generate', { difficulty }).then(res => res.data)
}

/**
 * Submit quiz answers
 * @param quizId ID of the quiz
 * @param answers User's answers
 * @returns Quiz result with score
 */
export function submitAnswers(quizId: number, answers: QuizAnswer[]): Promise<Result<QuizResult>> {
  return request.post(`/quiz/${quizId}/submit`, { answers }).then(res => res.data)
}

/**
 * Get quiz history for current user
 * @returns List of completed quizzes
 */
export function getQuizHistory(): Promise<Result<Quiz[]>> {
  return request.get('/quiz/history').then(res => res.data)
}
