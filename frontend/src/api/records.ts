import request from './request'
import type { Result, LearningRecordsResponse, LearningStatistics, RecordQueryParams } from '@/types'

/**
 * Get learning records with optional filtering
 * @param params Query parameters for filtering
 * @returns Paginated learning records response
 */
export function getLearningRecords(params?: RecordQueryParams): Promise<Result<LearningRecordsResponse>> {
  return request.get('/records', { params }).then(res => res.data)
}

/**
 * Get learning statistics for current user
 * @returns Learning statistics summary
 */
export function getStatistics(): Promise<Result<LearningStatistics>> {
  return request.get('/records/statistics').then(res => res.data)
}
