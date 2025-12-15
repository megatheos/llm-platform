<script setup lang="ts">
import { useI18n } from 'vue-i18n'

const { locale } = useI18n()

const languages = [
  { code: 'zh', name: '中文', flag: '🇨🇳' },
  { code: 'en', name: 'English', flag: '🇺🇸' },
  { code: 'ja', name: '日本語', flag: '🇯🇵' },
  { code: 'ko', name: '한국어', flag: '🇰🇷' }
]

function changeLanguage(code: string) {
  locale.value = code
  localStorage.setItem('locale', code)
}
</script>

<template>
  <el-dropdown trigger="click" @command="changeLanguage">
    <span class="language-trigger">
      <span class="current-flag">{{ languages.find(l => l.code === locale)?.flag }}</span>
      <span class="current-name">{{ languages.find(l => l.code === locale)?.name }}</span>
      <el-icon class="arrow-icon"><arrow-down /></el-icon>
    </span>
    <template #dropdown>
      <el-dropdown-menu>
        <el-dropdown-item
          v-for="lang in languages"
          :key="lang.code"
          :command="lang.code"
          :class="{ 'is-active': locale === lang.code }"
        >
          <span class="lang-flag">{{ lang.flag }}</span>
          <span class="lang-name">{{ lang.name }}</span>
        </el-dropdown-item>
      </el-dropdown-menu>
    </template>
  </el-dropdown>
</template>

<style scoped>
.language-trigger {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: all var(--transition-fast);
  color: var(--text-secondary);
  font-size: var(--font-size-sm);
}

.language-trigger:hover {
  background-color: var(--bg-tertiary);
  color: var(--text-primary);
}

.current-flag {
  font-size: 18px;
}

.current-name {
  font-weight: 500;
}

.arrow-icon {
  font-size: 12px;
  transition: transform var(--transition-fast);
}

.lang-flag {
  margin-right: 8px;
  font-size: 16px;
}

.lang-name {
  font-size: var(--font-size-sm);
}

:deep(.el-dropdown-menu__item.is-active) {
  background-color: var(--color-accent-subtle);
  color: var(--color-accent);
}
</style>
