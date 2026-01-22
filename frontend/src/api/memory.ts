import request from './request'
import type {
  Result,
  ReviewItem,
  ReviewCardData,
  ReviewStatistics,
  MemoryState
} from '@/types'

/**
 * Get items due for review
 * @param limit Maximum number of items to return
 * @returns List of review card data
 */
export function getDueReviews(limit?: number): Promise<Result<ReviewCardData[]>> {
  return request.get('/memory/due-reviews', { params: { limit } }).then(res => res.data)
}

/**
 * Get review card data for a specific item
 * @param itemId ID of the review item
 * @returns Review card data
 */
export function getReviewCard(itemId: number): Promise<Result<ReviewCardData>> {
  return request.get(`/memory/reviews/${itemId}`).then(res => res.data)
}

/**
 * Submit a review result
 * @param itemId ID of the review item
 * @param quality Quality rating (0-5, 0=blackout, 5=perfect recall)
 * @returns Updated review item
 */
export function submitReview(itemId: number, quality: number): Promise<Result<ReviewItem>> {
  return request.post(`/memory/reviews/${itemId}/submit`, { quality }).then(res => res.data)
}

/**
 * Get memory statistics
 * @returns Memory system statistics
 */
export function getMemoryStatistics(): Promise<Result<ReviewStatistics>> {
  return request.get('/memory/statistics').then(res => res.data)
}

/**
 * Get memory state overview
 * @returns Memory state for dashboard
 */
export function getMemoryState(): Promise<Result<MemoryState>> {
  return request.get('/memory/state').then(res => res.data)
}

/**
 * Get all review items with pagination
 * @param page Page number
 * @param pageSize Items per page
 * @param status Filter by status (all, due, learning, mastered)
 * @returns Paginated review items
 */
export function getReviewItems(
  page?: number,
  pageSize?: number,
  status?: 'all' | 'due' | 'learning' | 'mastered'
): Promise<Result<{ items: ReviewItem[]; total: number }>> {
  return request.get('/memory/items', { params: { page, pageSize, status } }).then(res => res.data)
}

/**
 * Force sync a review item
 * @param itemId ID of the review item
 * @returns Updated review item
 */
export function syncReviewItem(itemId: number): Promise<Result<ReviewItem>> {
  return request.post(`/memory/items/${itemId}/sync`).then(res => res.data)
}
