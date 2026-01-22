<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { useWordStore } from '@/stores/word'
import { ElMessage } from 'element-plus'
import {
  Right,
  Search,
  CopyDocument,
  Clock,
  Star,
  Delete,
  VideoPlay
} from '@element-plus/icons-vue'

const { t } = useI18n()
const wordStore = useWordStore()
const searchWord = ref('')
const activeTab = ref('definition')
const isSpeaking = ref(false)

// Language options with flags
const languageOptions = [
  { value: 'en', label: '🇺🇸 English', flag: '🇺🇸' },
  { value: 'zh', label: '🇨🇳 中文', flag: '🇨🇳' },
  { value: 'ja', label: '🇯🇵 日本語', flag: '🇯🇵' },
  { value: 'ko', label: '🇰🇷 한국어', flag: '🇰🇷' },
  { value: 'fr', label: '🇫🇷 Français', flag: '🇫🇷' },
  { value: 'de', label: '🇩🇪 Deutsch', flag: '🇩🇪' },
  { value: 'es', label: '🇪🇸 Español', flag: '🇪🇸' }
]

// History search filter
const historySearch = ref('')
const filteredHistory = computed(() => {
  if (!historySearch.value) return wordStore.history
  return wordStore.history.filter(item => 
    item.word.toLowerCase().includes(historySearch.value.toLowerCase()) ||
    item.translation?.toLowerCase().includes(historySearch.value.toLowerCase())
  )
})

async function handleQuery() {
  if (!searchWord.value.trim()) {
    ElMessage.warning(t('word.enterWord'))
    return
  }
  
  try {
    await wordStore.queryWord(searchWord.value)
    if (wordStore.error) {
      ElMessage.error(wordStore.error)
    }
  } catch (err: any) {
    ElMessage.error(err.message || t('word.queryFailed'))
  }
}

function handleHistoryClick(word: string, sourceLang: string, targetLang: string) {
  searchWord.value = word
  wordStore.setSourceLang(sourceLang)
  wordStore.setTargetLang(targetLang)
  handleQuery()
}

function formatDate(dateStr: string): string {
  const date = new Date(dateStr)
  const now = new Date()
  const diffMs = now.getTime() - date.getTime()
  const diffMins = Math.floor(diffMs / (1000 * 60))
  const diffHours = Math.floor(diffMins / 60)
  const diffDays = Math.floor(diffHours / 24)
  
  if (diffMins < 1) return 'Just now'
  if (diffMins < 60) return `${diffMins}m ago`
  if (diffHours < 24) return `${diffHours}h ago`
  if (diffDays < 7) return `${diffDays}d ago`
  return date.toLocaleDateString()
}

interface ExampleItem {
  sentence: string
  translation: string
}

function parseExamples(examples: string): ExampleItem[] {
  if (!examples) return []
  try {
    const parsed = JSON.parse(examples)
    if (Array.isArray(parsed)) {
      return parsed.map(item => {
        if (typeof item === 'object' && item.sentence) {
          return { sentence: item.sentence, translation: item.translation || '' }
        }
        return { sentence: String(item), translation: '' }
      })
    }
    return [{ sentence: examples, translation: '' }]
  } catch {
    return examples.split('\n').filter(e => e.trim()).map(s => ({ sentence: s, translation: '' }))
  }
}

async function copyToClipboard(text: string) {
  try {
    await navigator.clipboard.writeText(text)
    ElMessage.success('Copied to clipboard')
  } catch {
    ElMessage.error('Failed to copy')
  }
}

function speakText(text: string) {
  if (isSpeaking.value) {
    speechSynthesis.cancel()
    isSpeaking.value = false
    return
  }
  
  const utterance = new SpeechSynthesisUtterance(text)
  utterance.lang = wordStore.targetLang === 'zh' ? 'zh-CN' : 
                  wordStore.targetLang === 'ja' ? 'ja-JP' : 
                  wordStore.targetLang === 'ko' ? 'ko-KR' : 'en-US'
  utterance.rate = 0.9
  
  utterance.onstart = () => { isSpeaking.value = true }
  utterance.onend = () => { isSpeaking.value = false }
  utterance.onerror = () => { isSpeaking.value = false }
  
  speechSynthesis.speak(utterance)
}

function getPhoneticSymbol(word: string): string {
  const phonetics: Record<string, string> = {
    'hello': '/həˈloʊ/',
    'world': '/wɜːrld/',
    'test': '/test/',
    'example': '/ɪɡˈzæmpəl/'
  }
  return phonetics[word.toLowerCase()] || ''
}

onMounted(() => {
  wordStore.fetchHistory()
})
</script>

<template>
  <div class="word-query-page">
    <el-row :gutter="24">
      <el-col :xs="24" :lg="16">
        <el-card class="query-card" shadow="hover">
          <template #header>
            <div class="card-header">
              <div class="header-left">
                <el-icon class="header-icon"><Search /></el-icon>
                <span>{{ t('word.wordQuery') }}</span>
              </div>
              <div class="header-badge">
                <el-icon><Star /></el-icon>
                AI Powered
              </div>
            </div>
          </template>
          
          <div class="search-area">
            <div class="language-row">
              <div class="lang-selector">
                <span class="lang-flag">{{ languageOptions.find(l => l.value === wordStore.sourceLang)?.flag || '🌐' }}</span>
                <el-select v-model="wordStore.sourceLang" class="language-select">
                  <el-option
                    v-for="lang in languageOptions"
                    :key="lang.value"
                    :label="lang.label"
                    :value="lang.value"
                  />
                </el-select>
              </div>
              
              <div class="arrow-wrapper">
                <el-icon class="arrow-icon"><Right /></el-icon>
              </div>
              
              <div class="lang-selector">
                <span class="lang-flag">{{ languageOptions.find(l => l.value === wordStore.targetLang)?.flag || '🌐' }}</span>
                <el-select v-model="wordStore.targetLang" class="language-select">
                  <el-option
                    v-for="lang in languageOptions"
                    :key="lang.value"
                    :label="lang.label"
                    :value="lang.value"
                  />
                </el-select>
              </div>
              
              <el-button 
                type="primary" 
                class="search-btn"
                :loading="wordStore.loading"
                @click="handleQuery"
              >
                <el-icon><Search /></el-icon>
                {{ t('word.search') }}
              </el-button>
            </div>
            
            <div class="input-wrapper">
              <el-input
                v-model="searchWord"
                :placeholder="t('word.placeholder')"
                clearable
                size="large"
                @keyup.enter="handleQuery"
              >
                <template #prefix>
                  <el-icon class="input-icon"><Search /></el-icon>
                </template>
                <template #suffix>
                  <el-icon 
                    v-if="searchWord" 
                    class="input-action"
                    @click="searchWord = ''"
                  >
                    <Delete />
                  </el-icon>
                </template>
              </el-input>
            </div>
          </div>
          
          <div v-if="wordStore.loading" class="loading-section">
            <div class="skeleton-word">
              <el-skeleton :rows="1" animated />
            </div>
            <div class="skeleton-content">
              <el-skeleton :rows="4" animated />
            </div>
          </div>
          
          <div v-else-if="wordStore.currentWord" class="result-section">
            <div class="word-hero">
              <div class="word-main">
                <h1 class="word-title">{{ wordStore.currentWord.word }}</h1>
                <div v-if="getPhoneticSymbol(wordStore.currentWord.word)" class="phonetic">
                  <span class="phonetic-text">{{ getPhoneticSymbol(wordStore.currentWord.word) }}</span>
                  <el-button 
                    class="speak-btn"
                    :class="{ speaking: isSpeaking }"
                    circle
                    size="small"
                    @click="speakText(wordStore.currentWord.word)"
                  >
                    <el-icon><VideoPlay /></el-icon>
                  </el-button>
                </div>
              </div>
              <div class="word-actions">
                <el-button 
                  v-if="wordStore.currentWord.word"
                  circle
                  @click="copyToClipboard(wordStore.currentWord.word)"
                >
                  <el-icon><CopyDocument /></el-icon>
                </el-button>
              </div>
            </div>
            
            <el-tabs v-model="activeTab" class="result-tabs">
              <el-tab-pane label="Definition" name="definition">
                <div class="tab-content">
                  <div v-if="wordStore.currentWord.translation" class="translation-block">
                    <h4>Translation</h4>
                    <p class="translation-text">{{ wordStore.currentWord.translation }}</p>
                  </div>
                  
                  <div v-if="wordStore.currentWord.definition" class="definition-block">
                    <h4>Definition</h4>
                    <p class="definition-text">{{ wordStore.currentWord.definition }}</p>
                  </div>
                </div>
              </el-tab-pane>
              
              <el-tab-pane label="Examples" name="examples">
                <div class="examples-list">
                  <div 
                    v-for="(example, index) in parseExamples(wordStore.currentWord.examples)" 
                    :key="index"
                    class="example-card"
                    :style="{ '--delay': `${index * 0.1}s` }"
                  >
                    <div class="example-sentence">
                      <el-icon class="quote-icon"><Star /></el-icon>
                      {{ example.sentence }}
                    </div>
                    <div v-if="example.translation" class="example-translation">
                      {{ example.translation }}
                    </div>
                    <div class="example-actions">
                      <el-button 
                        size="small" 
                        circle
                        @click="copyToClipboard(example.sentence)"
                      >
                        <el-icon><CopyDocument /></el-icon>
                      </el-button>
                      <el-button 
                        size="small" 
                        circle
                        @click="speakText(example.sentence)"
                      >
                        <el-icon><VideoPlay /></el-icon>
                      </el-button>
                    </div>
                  </div>
                </div>
              </el-tab-pane>
              
              <el-tab-pane label="Notes" name="notes">
                <div class="notes-block">
                  <el-empty description="No notes yet" :image-size="60" />
                </div>
              </el-tab-pane>
            </el-tabs>
          </div>
          
          <el-empty v-else class="empty-state" :description="t('word.emptyHint')">
            <template #image>
              <div class="empty-illustration">
                <el-icon :size="64"><Search /></el-icon>
              </div>
            </template>
          </el-empty>
        </el-card>
      </el-col>
      
      <el-col :xs="24" :lg="8">
        <el-card class="history-card" shadow="hover">
          <template #header>
            <div class="card-header">
              <div class="header-left">
                <el-icon class="header-icon"><Clock /></el-icon>
                <span>{{ t('word.queryHistory') }}</span>
              </div>
              <el-badge :value="wordStore.historyCount" class="history-badge" />
            </div>
          </template>
          
          <div class="history-search">
            <el-input
              v-model="historySearch"
              placeholder="Search history..."
              clearable
              size="small"
            >
              <template #prefix>
                <el-icon><Search /></el-icon>
              </template>
            </el-input>
          </div>
          
          <el-scrollbar height="450px" class="history-scroll">
            <div v-if="wordStore.historyLoading" class="loading-container">
              <el-skeleton v-for="i in 3" :key="i" :rows="2" animated />
            </div>
            
            <transition-group 
              v-else-if="filteredHistory.length > 0" 
              name="history-list"
              tag="div"
              class="history-list"
            >
              <div
                v-for="item in filteredHistory"
                :key="item.id"
                class="history-item"
                @click="handleHistoryClick(item.word, item.sourceLang, item.targetLang)"
              >
                <div class="history-content">
                  <div class="history-word-row">
                    <span class="history-word">{{ item.word }}</span>
                    <span class="history-time">{{ formatDate(item.queryTime) }}</span>
                  </div>
                  <div class="history-translation">{{ item.translation }}</div>
                  <div class="history-langs">
                    <span class="lang-tag">{{ languageOptions.find(l => l.value === item.sourceLang)?.flag }}</span>
                    <el-icon class="lang-arrow"><Right /></el-icon>
                    <span class="lang-tag">{{ languageOptions.find(l => l.value === item.targetLang)?.flag }}</span>
                  </div>
                </div>
                <div class="history-actions">
                  <el-icon class="action-icon"><Star /></el-icon>
                </div>
              </div>
            </transition-group>
            
              <el-empty 
                v-else-if="historySearch" 
                :description="`No results for '${historySearch}'`" 
                :image-size="60" 
              />
            
            <el-empty 
              v-else 
              :description="t('word.noHistory')" 
              :image-size="60" 
            />
          </el-scrollbar>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<style scoped>
.word-query-page {
  padding: 0;
}

.query-card, .history-card {
  height: 100%;
  transition: all 0.3s ease;
}

.query-card:hover, .history-card:hover {
  box-shadow: var(--shadow-md);
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
}

.header-icon {
  color: var(--color-primary);
  font-size: 20px;
}

.header-badge {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: var(--font-size-xs);
  color: var(--color-accent);
  background: #f0f9ff;
  padding: 4px 10px;
  border-radius: 12px;
}

.search-area {
  margin-bottom: 24px;
}

.language-row {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 16px;
}

.lang-selector {
  display: flex;
  align-items: center;
  gap: 8px;
}

.lang-flag {
  font-size: 24px;
}

.language-select {
  width: 140px;
}

.arrow-wrapper {
  flex-shrink: 0;
}

.arrow-icon {
  font-size: 20px;
  color: var(--text-muted);
  animation: pulse-arrow 2s infinite;
}

@keyframes pulse-arrow {
  0%, 100% { transform: translateX(0); }
  50% { transform: translateX(4px); }
}

.search-btn {
  margin-left: auto;
  display: flex;
  align-items: center;
  gap: 8px;
}

.input-wrapper {
  position: relative;
}

.input-icon {
  color: var(--text-muted);
}

.input-action {
  cursor: pointer;
  color: var(--text-muted);
  transition: color 0.3s;
}

.input-action:hover {
  color: var(--text-primary);
}

.loading-section {
  padding: 20px 0;
}

.skeleton-word {
  margin-bottom: 20px;
}

.skeleton-word :deep(.el-skeleton__item) {
  height: 48px;
  width: 200px;
}

.word-hero {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  padding: 24px;
  background: var(--bg-tertiary);
  border-radius: var(--radius-lg);
  margin-bottom: 24px;
  position: relative;
  overflow: hidden;
}

.word-hero::before {
  content: '';
  position: absolute;
  top: -50%;
  right: -20%;
  width: 200px;
  height: 200px;
  background: rgba(59, 130, 246, 0.1);
  border-radius: 50%;
}

.word-main {
  position: relative;
  z-index: 1;
}

.word-title {
  font-size: 42px;
  font-weight: 700;
  color: var(--text-primary);
  margin: 0 0 8px 0;
  line-height: 1.2;
  animation: fadeInUp 0.5s ease-out;
}

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.phonetic {
  display: flex;
  align-items: center;
  gap: 12px;
}

.phonetic-text {
  font-size: var(--font-size-lg);
  color: var(--text-muted);
  font-style: italic;
}

.speak-btn {
  transition: all 0.3s;
}

.speak-btn.speaking {
  background: var(--color-primary);
  color: white;
  animation: speaking-pulse 1s infinite;
}

@keyframes speaking-pulse {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.1); }
}

.word-actions {
  display: flex;
  gap: 8px;
  position: relative;
  z-index: 1;
}

.result-tabs {
  margin-top: 16px;
}

.tab-content {
  padding: 16px 0;
}

.translation-block,
.definition-block {
  margin-bottom: 20px;
}

.translation-block h4,
.definition-block h4 {
  font-size: var(--font-size-sm);
  color: var(--text-muted);
  margin: 0 0 8px 0;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.translation-text {
  font-size: var(--font-size-xl);
  font-weight: 500;
  color: var(--color-accent);
  margin: 0;
}

.definition-text {
  font-size: var(--font-size-md);
  color: var(--text-primary);
  line-height: 1.8;
  margin: 0;
}

.examples-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.example-card {
  background: var(--bg-tertiary);
  border-radius: var(--radius-lg);
  padding: 20px;
  position: relative;
  animation: fadeInUp 0.5s ease-out backwards;
  animation-delay: var(--delay);
  transition: all 0.3s;
}

.example-card:hover {
  transform: translateX(4px);
  box-shadow: var(--shadow-sm);
}

.quote-icon {
  color: var(--color-primary);
  opacity: 0.5;
  margin-right: 8px;
}

.example-sentence {
  font-size: var(--font-size-md);
  color: var(--text-primary);
  line-height: 1.6;
  margin-bottom: 8px;
}

.example-translation {
  font-size: var(--font-size-sm);
  color: var(--text-muted);
  padding-left: 28px;
}

.example-actions {
  position: absolute;
  top: 12px;
  right: 12px;
  display: flex;
  gap: 4px;
  opacity: 0;
  transition: opacity 0.3s;
}

.example-card:hover .example-actions {
  opacity: 1;
}

.history-search {
  margin-bottom: 16px;
}

.history-scroll {
  padding-right: 8px;
}

.loading-container {
  padding: 20px 0;
}

.history-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.history-list-enter-active,
.history-list-leave-active {
  transition: all 0.3s ease;
}

.history-list-enter-from,
.history-list-leave-to {
  opacity: 0;
  transform: translateX(-20px);
}

.history-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px;
  background: var(--bg-tertiary);
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: all 0.3s;
  border: 1px solid transparent;
}

.history-item:hover {
  background: var(--bg-secondary);
  border-color: var(--color-accent);
  transform: translateX(4px);
}

.history-content {
  flex: 1;
  min-width: 0;
}

.history-word-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 4px;
}

.history-word {
  font-weight: 600;
  font-size: var(--font-size-md);
  color: var(--text-primary);
}

.history-time {
  font-size: var(--font-size-xs);
  color: var(--text-muted);
}

.history-translation {
  font-size: var(--font-size-sm);
  color: var(--text-secondary);
  margin-bottom: 6px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.history-langs {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: var(--font-size-xs);
}

.lang-tag {
  background: var(--bg-secondary);
  padding: 2px 6px;
  border-radius: 4px;
}

.lang-arrow {
  font-size: 12px;
  color: var(--text-muted);
}

.history-actions {
  flex-shrink: 0;
}

.action-icon {
  color: var(--text-muted);
  opacity: 0;
  transition: all 0.3s;
}

.history-item:hover .action-icon {
  opacity: 1;
  color: var(--color-primary);
}

.empty-state {
  padding: 40px 20px;
}

.empty-illustration {
  width: 100px;
  height: 100px;
  background: var(--bg-tertiary);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--text-muted);
  margin: 0 auto 16px;
}

@media (max-width: 768px) {
  .language-row {
    flex-wrap: wrap;
    gap: 12px;
  }
  
  .lang-selector {
    flex: 1;
    min-width: 100px;
  }
  
  .language-select {
    width: 100%;
  }
  
  .search-btn {
    width: 100%;
    margin-left: 0;
  }
  
  .word-hero {
    flex-direction: column;
    gap: 16px;
  }
  
  .word-title {
    font-size: 32px;
  }
}
</style>
