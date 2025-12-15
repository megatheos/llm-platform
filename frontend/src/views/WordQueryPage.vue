<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useWordStore } from '@/stores/word'
import { ElMessage } from 'element-plus'

const wordStore = useWordStore()

const searchWord = ref('')

// Language options
const languageOptions = [
  { value: 'en', label: 'English' },
  { value: 'zh', label: '中文' },
  { value: 'ja', label: '日本語' },
  { value: 'ko', label: '한국어' },
  { value: 'fr', label: 'Français' },
  { value: 'de', label: 'Deutsch' },
  { value: 'es', label: 'Español' }
]

// Handle word query
async function handleQuery() {
  if (!searchWord.value.trim()) {
    ElMessage.warning('Please enter a word to query')
    return
  }
  
  try {
    await wordStore.queryWord(searchWord.value)
    if (wordStore.error) {
      ElMessage.error(wordStore.error)
    }
  } catch (err: any) {
    ElMessage.error(err.message || 'Failed to query word')
  }
}

// Handle history item click
function handleHistoryClick(word: string, sourceLang: string, targetLang: string) {
  searchWord.value = word
  wordStore.setSourceLang(sourceLang)
  wordStore.setTargetLang(targetLang)
  handleQuery()
}

// Format date for display
function formatDate(dateStr: string): string {
  const date = new Date(dateStr)
  return date.toLocaleDateString() + ' ' + date.toLocaleTimeString()
}

// Parse examples string to array
function parseExamples(examples: string): string[] {
  if (!examples) return []
  try {
    // Try parsing as JSON array first
    const parsed = JSON.parse(examples)
    if (Array.isArray(parsed)) return parsed
    return [examples]
  } catch {
    // If not JSON, split by newlines or return as single item
    return examples.split('\n').filter(e => e.trim())
  }
}

onMounted(() => {
  wordStore.fetchHistory()
})
</script>

<template>
  <div class="word-query-page">
    <el-row :gutter="20">
      <!-- Query Section -->
      <el-col :span="16">
        <el-card class="query-card">
          <template #header>
            <div class="card-header">
              <span>Word Query</span>
            </div>
          </template>
          
          <!-- Search Form -->
          <el-form @submit.prevent="handleQuery">
            <el-row :gutter="12" align="middle">
              <el-col :span="6">
                <el-select
                  v-model="wordStore.sourceLang"
                  placeholder="Source Language"
                  class="language-select"
                >
                  <el-option
                    v-for="lang in languageOptions"
                    :key="lang.value"
                    :label="lang.label"
                    :value="lang.value"
                  />
                </el-select>
              </el-col>
              
              <el-col :span="2" class="arrow-col">
                <el-icon><Right /></el-icon>
              </el-col>
              
              <el-col :span="6">
                <el-select
                  v-model="wordStore.targetLang"
                  placeholder="Target Language"
                  class="language-select"
                >
                  <el-option
                    v-for="lang in languageOptions"
                    :key="lang.value"
                    :label="lang.label"
                    :value="lang.value"
                  />
                </el-select>
              </el-col>
              
              <el-col :span="10">
                <el-input
                  v-model="searchWord"
                  placeholder="Enter a word to query"
                  clearable
                  @keyup.enter="handleQuery"
                >
                  <template #append>
                    <el-button
                      type="primary"
                      :loading="wordStore.loading"
                      @click="handleQuery"
                    >
                      Search
                    </el-button>
                  </template>
                </el-input>
              </el-col>
            </el-row>
          </el-form>
          
          <!-- Result Display -->
          <div v-if="wordStore.currentWord" class="result-section">
            <el-divider />
            
            <div class="word-header">
              <h2 class="word-title">{{ wordStore.currentWord.word }}</h2>
              <span v-if="wordStore.currentWord.pronunciation" class="pronunciation">
                {{ wordStore.currentWord.pronunciation }}
              </span>
            </div>
            
            <el-descriptions :column="1" border class="word-details">
              <el-descriptions-item label="Translation">
                <span class="translation-text">{{ wordStore.currentWord.translation }}</span>
              </el-descriptions-item>
              
              <el-descriptions-item label="Definition">
                {{ wordStore.currentWord.definition }}
              </el-descriptions-item>
              
              <el-descriptions-item label="Examples">
                <ul class="examples-list">
                  <li
                    v-for="(example, index) in parseExamples(wordStore.currentWord.examples)"
                    :key="index"
                  >
                    {{ example }}
                  </li>
                </ul>
              </el-descriptions-item>
            </el-descriptions>
          </div>
          
          <!-- Empty State -->
          <el-empty
            v-else-if="!wordStore.loading"
            description="Enter a word to see its definition and translation"
          />
        </el-card>
      </el-col>
      
      <!-- History Section -->
      <el-col :span="8">
        <el-card class="history-card">
          <template #header>
            <div class="card-header">
              <span>Query History</span>
              <el-badge :value="wordStore.historyCount" class="history-badge" />
            </div>
          </template>
          
          <el-scrollbar height="500px">
            <div v-if="wordStore.historyLoading" class="loading-container">
              <el-skeleton :rows="5" animated />
            </div>
            
            <el-timeline v-else-if="wordStore.history.length > 0">
              <el-timeline-item
                v-for="item in wordStore.history"
                :key="item.id"
                :timestamp="formatDate(item.queryTime)"
                placement="top"
              >
                <el-card
                  shadow="hover"
                  class="history-item"
                  @click="handleHistoryClick(item.word.word, item.word.sourceLang, item.word.targetLang)"
                >
                  <div class="history-word">{{ item.word.word }}</div>
                  <div class="history-translation">{{ item.word.translation }}</div>
                  <div class="history-langs">
                    {{ item.word.sourceLang }} → {{ item.word.targetLang }}
                  </div>
                </el-card>
              </el-timeline-item>
            </el-timeline>
            
            <el-empty v-else description="No query history yet" />
          </el-scrollbar>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script lang="ts">
import { Right } from '@element-plus/icons-vue'

export default {
  components: { Right }
}
</script>

<style scoped>
.word-query-page {
  padding: 20px;
}

.query-card,
.history-card {
  height: 100%;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-weight: 600;
}

.language-select {
  width: 100%;
}

.arrow-col {
  text-align: center;
  font-size: 18px;
  color: #909399;
}

.result-section {
  margin-top: 20px;
}

.word-header {
  display: flex;
  align-items: baseline;
  gap: 12px;
  margin-bottom: 16px;
}

.word-title {
  margin: 0;
  font-size: 28px;
  color: #303133;
}

.pronunciation {
  color: #909399;
  font-style: italic;
}

.translation-text {
  font-size: 16px;
  font-weight: 500;
  color: #409eff;
}

.examples-list {
  margin: 0;
  padding-left: 20px;
}

.examples-list li {
  margin-bottom: 8px;
  line-height: 1.6;
}

.history-badge {
  margin-left: 8px;
}

.loading-container {
  padding: 20px;
}

.history-item {
  cursor: pointer;
  transition: all 0.3s;
}

.history-item:hover {
  border-color: #409eff;
}

.history-word {
  font-weight: 600;
  font-size: 16px;
  color: #303133;
}

.history-translation {
  color: #606266;
  margin: 4px 0;
  font-size: 14px;
}

.history-langs {
  font-size: 12px;
  color: #909399;
}

.word-details :deep(.el-descriptions__label) {
  width: 120px;
  font-weight: 600;
}
</style>
