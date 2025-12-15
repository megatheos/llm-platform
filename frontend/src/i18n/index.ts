import { createI18n } from 'vue-i18n'
import zh from './locales/zh'
import en from './locales/en'
import ja from './locales/ja'
import ko from './locales/ko'

const i18n = createI18n({
  legacy: false,
  locale: localStorage.getItem('locale') || 'zh',
  fallbackLocale: 'en',
  messages: {
    zh,
    en,
    ja,
    ko
  }
})

export default i18n
