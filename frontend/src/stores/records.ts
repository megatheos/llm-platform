import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { getLearningRecords, getStatistics } from '@/api/records'
import type { 
  LearningRecord, 
  LearningStatistics, 
  RecordQueryParams, 
  ActivityType 
} from '@/types'

export const useRecordsStore = defineStore('records', () => {
  // State
  const records = ref<LearningRecord[]>([])
  const statistics = ref<LearningStatistics | null>(null)
  const loading = ref(false)
  const statisticsLoading = ref(false)
  const error = ref<string | null>(null)
  
  // Pagination state
  const currentPage = ref(1)
  const pageSize = ref(20)
  const total = ref(0)
  const totalPages = ref(0)
  
  // Filter state
  const activeFilter = ref<ActivityType | undefined>(undefined)

  // Getters
  const hasRecords = computed(() => records.value.length > 0)
  const recordCount = computed(() => records.value.length)
  const hasStatistics = computed(() => !!statistics.value)
  const hasMorePages = computed(() => currentPage.value < totalPages.value)

  /**
   * Fetch learning records with optional filtering
   * @param params Query parameters for filtering
   * @returns Promise resolving to the records list
   */
  async function fetchRecords(params?: RecordQueryParams): Promise<LearningRecord[]> {
    loading.value = true
    error.value = null

    const queryParams: RecordQueryParams = {
      page: params?.page || currentPage.value,
      pageSize: params?.pageSize || pageSize.value,
      activityType: params?.activityType || activeFilter.value,
      startDate: params?.startDate,
      endDate: params?.endDate
    }

    try {
      const result = await getLearningRecords(queryParams)
      if (result.code === 0 || result.code === 200) {
        records.value = result.data.records || []
        total.value = result.data.total
        currentPage.value = result.data.page
        pageSize.value = result.data.pageSize
        totalPages.value = result.data.totalPages
        return records.value
      } else {
        error.value = result.message || 'Failed to fetch records'
        return []
      }
    } catch (err: any) {
      error.value = err.message || 'Failed to fetch records'
      throw err
    } finally {
      loading.value = false
    }
  }


  /**
   * Fetch learning statistics for current user
   * @returns Promise resolving to the statistics
   */
  async function fetchStatistics(): Promise<LearningStatistics | null> {
    statisticsLoading.value = true

    try {
      const result = await getStatistics()
      if (result.code === 0 || result.code === 200) {
        statistics.value = result.data
        return result.data
      } else {
        return null
      }
    } catch (err: any) {
      console.error('Failed to fetch statistics:', err)
      return null
    } finally {
      statisticsLoading.value = false
    }
  }

  /**
   * Set activity type filter and refresh records
   * @param type Activity type to filter by
   */
  async function setFilter(type?: ActivityType) {
    activeFilter.value = type
    currentPage.value = 1
    await fetchRecords()
  }

  /**
   * Go to a specific page
   * @param page Page number
   */
  async function goToPage(page: number) {
    if (page >= 1 && page <= totalPages.value) {
      currentPage.value = page
      await fetchRecords()
    }
  }

  /**
   * Change page size and refresh records
   * @param size New page size
   */
  async function changePageSize(size: number) {
    pageSize.value = size
    currentPage.value = 1
    await fetchRecords()
  }

  /**
   * Get activity type label for display
   * @param type Activity type
   * @returns Human-readable label
   */
  function getActivityTypeLabel(type: ActivityType): string {
    const labels: Record<ActivityType, string> = {
      WORD_QUERY: 'Word Query',
      DIALOGUE: 'Dialogue',
      QUIZ: 'Quiz'
    }
    return labels[type] || type
  }

  /**
   * Get activity type icon name
   * @param type Activity type
   * @returns Icon name for Element Plus
   */
  function getActivityTypeIcon(type: ActivityType): string {
    const icons: Record<ActivityType, string> = {
      WORD_QUERY: 'Search',
      DIALOGUE: 'ChatDotRound',
      QUIZ: 'Document'
    }
    return icons[type] || 'Document'
  }

  /**
   * Get activity type color
   * @param type Activity type
   * @returns Element Plus tag type
   */
  function getActivityTypeColor(type: ActivityType): 'primary' | 'success' | 'warning' | 'danger' | 'info' {
    const colors: Record<ActivityType, 'primary' | 'success' | 'warning'> = {
      WORD_QUERY: 'primary',
      DIALOGUE: 'success',
      QUIZ: 'warning'
    }
    return colors[type] || 'info'
  }

  /**
   * Clear all state
   */
  function clearAll() {
    records.value = []
    statistics.value = null
    error.value = null
    currentPage.value = 1
    total.value = 0
    totalPages.value = 0
    activeFilter.value = undefined
  }

  return {
    // State
    records,
    statistics,
    loading,
    statisticsLoading,
    error,
    currentPage,
    pageSize,
    total,
    totalPages,
    activeFilter,
    // Getters
    hasRecords,
    recordCount,
    hasStatistics,
    hasMorePages,
    // Actions
    fetchRecords,
    fetchStatistics,
    setFilter,
    goToPage,
    changePageSize,
    getActivityTypeLabel,
    getActivityTypeIcon,
    getActivityTypeColor,
    clearAll
  }
})
