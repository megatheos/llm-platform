import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { queryWord as apiQueryWord, getWordHistory as apiGetWordHistory } from '@/api/word'
import type { Word, WordQueryRequest, WordHistory } from '@/types'

export const useWordStore = defineStore('word', () => {
  const currentWord = ref<Word | null>(null)
  const history = ref<WordHistory[]>([])
  const loading = ref(false)
  const historyLoading = ref(false)
  const error = ref<string | null>(null)

  // Default language settings
  const sourceLang = ref('en')
  const targetLang = ref('zh')

  const hasCurrentWord = computed(() => !!currentWord.value)
  const historyCount = computed(() => history.value.length)

  /**
   * Query a word with translation
   * @param word The word to query
   * @param source Source language code
   * @param target Target language code
   * @returns Promise resolving to the word result
   */
  async function queryWord(word: string, source?: string, target?: string): Promise<Word | null> {
    loading.value = true
    error.value = null
    
    const request: WordQueryRequest = {
      word: word.trim(),
      sourceLang: source || sourceLang.value,
      targetLang: target || targetLang.value
    }

    try {
      const result = await apiQueryWord(request)
      if (result.code === 0 || result.code === 200) {
        currentWord.value = result.data
        // Refresh history after successful query
        await fetchHistory()
        return result.data
      } else {
        error.value = result.message || 'Failed to query word'
        return null
      }
    } catch (err: any) {
      error.value = err.message || 'Failed to query word'
      throw err
    } finally {
      loading.value = false
    }
  }

  /**
   * Fetch word query history for current user
   * @returns Promise resolving to the history list
   */
  async function fetchHistory(): Promise<WordHistory[]> {
    historyLoading.value = true
    
    try {
      const result = await apiGetWordHistory()
      if (result.code === 0 || result.code === 200) {
        history.value = result.data || []
        return history.value
      } else {
        return []
      }
    } catch (err: any) {
      console.error('Failed to fetch word history:', err)
      return []
    } finally {
      historyLoading.value = false
    }
  }

  /**
   * Set source language
   * @param lang Language code
   */
  function setSourceLang(lang: string) {
    sourceLang.value = lang
  }

  /**
   * Set target language
   * @param lang Language code
   */
  function setTargetLang(lang: string) {
    targetLang.value = lang
  }

  /**
   * Clear current word
   */
  function clearCurrentWord() {
    currentWord.value = null
    error.value = null
  }

  /**
   * Clear all state
   */
  function clearAll() {
    currentWord.value = null
    history.value = []
    error.value = null
  }

  return {
    currentWord,
    history,
    loading,
    historyLoading,
    error,
    sourceLang,
    targetLang,
    hasCurrentWord,
    historyCount,
    queryWord,
    fetchHistory,
    setSourceLang,
    setTargetLang,
    clearCurrentWord,
    clearAll
  }
})
