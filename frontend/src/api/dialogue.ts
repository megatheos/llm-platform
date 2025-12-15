import request from './request'
import type { Result, Scenario, CreateScenarioRequest, DialogueSession, DialogueMessage } from '@/types'

/**
 * Get all available scenarios
 * @returns List of scenarios
 */
export function getScenarios(): Promise<Result<Scenario[]>> {
  return request.get('/dialogue/scenarios').then(res => res.data)
}

/**
 * Create a custom scenario
 * @param data Scenario creation data
 * @returns Created scenario
 */
export function createScenario(data: CreateScenarioRequest): Promise<Result<Scenario>> {
  return request.post('/dialogue/scenarios', data).then(res => res.data)
}

/**
 * Start a new dialogue session
 * @param scenarioId ID of the scenario to use
 * @param targetLang Target language for the dialogue
 * @returns New dialogue session
 */
export function startSession(scenarioId: number, targetLang: string = 'en'): Promise<Result<DialogueSession>> {
  return request.post('/dialogue/sessions', { scenarioId, targetLang }).then(res => res.data)
}

/**
 * Send a message in a dialogue session
 * @param sessionId ID of the session
 * @param message User message content
 * @returns AI response message
 */
export function sendMessage(sessionId: number, message: string): Promise<Result<DialogueMessage>> {
  return request.post(`/dialogue/sessions/${sessionId}/messages`, { message }).then(res => res.data)
}

/**
 * End a dialogue session
 * @param sessionId ID of the session to end
 */
export function endSession(sessionId: number): Promise<Result<void>> {
  return request.delete(`/dialogue/sessions/${sessionId}`).then(res => res.data)
}
