import request from './request'
import type { Result, Word, WordQueryRequest, WordHistory } from '@/types'

/**
 * Query a word with translation
 * @param data Word query parameters
 * @returns Word definition and translation
 */
export function queryWord(data: WordQueryRequest): Promise<Result<Word>> {
  return request.post('/words/query', data).then(res => res.data)
}

/**
 * Get word query history for current user
 * @returns List of word query history
 */
export function getWordHistory(): Promise<Result<WordHistory[]>> {
  return request.get('/words/history').then(res => res.data)
}
