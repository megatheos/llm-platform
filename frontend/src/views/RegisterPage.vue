<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { User, Lock, Message } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import LanguageSwitcher from '@/components/LanguageSwitcher.vue'

interface RegisterForm {
  username: string
  email: string
  password: string
  confirmPassword: string
}

const { t } = useI18n()
const router = useRouter()
const authStore = useAuthStore()

const formRef = ref<FormInstance>()
const registerForm = reactive<RegisterForm>({
  username: '',
  email: '',
  password: '',
  confirmPassword: ''
})

const validateConfirmPassword = (_rule: any, value: string, callback: any) => {
  if (value !== registerForm.password) {
    callback(new Error(t('validation.passwordMismatch')))
  } else {
    callback()
  }
}

const rules = reactive<FormRules<RegisterForm>>({
  username: [
    { required: true, message: () => t('validation.enterUsername'), trigger: 'blur' },
    { min: 3, max: 50, message: () => t('validation.usernameLength'), trigger: 'blur' }
  ],
  email: [
    { required: true, message: () => t('validation.enterEmail'), trigger: 'blur' },
    { type: 'email', message: () => t('validation.emailInvalid'), trigger: 'blur' }
  ],
  password: [
    { required: true, message: () => t('validation.enterPassword'), trigger: 'blur' },
    { min: 6, max: 100, message: () => t('validation.passwordLength'), trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: () => t('validation.confirmPassword'), trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' }
  ]
})

async function handleRegister() {
  if (!formRef.value) return
  
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    
    try {
      const success = await authStore.register({
        username: registerForm.username,
        email: registerForm.email,
        password: registerForm.password
      })
      if (success) {
        ElMessage.success(t('auth.registerSuccess'))
        router.push('/login')
      }
    } catch (error: any) {
      ElMessage.error(error.message || t('auth.registerFailed'))
    }
  })
}

function goToLogin() {
  router.push('/login')
}
</script>

<template>
  <div class="register-page">
    <!-- Background Pattern -->
    <div class="bg-pattern"></div>
    
    <!-- Language Switcher -->
    <div class="language-wrapper">
      <LanguageSwitcher />
    </div>

    <div class="register-container">
      <!-- Left Side - Branding -->
      <div class="branding-section">
        <div class="brand-content">
          <div class="logo">
            <span class="logo-icon">📚</span>
          </div>
          <h1 class="brand-title">{{ t('common.appName') }}</h1>
          <p class="brand-subtitle">{{ t('auth.registerSubtitle') }}</p>
          
          <div class="features-list">
            <div class="feature-item">
              <el-icon><Search /></el-icon>
              <span>{{ t('features.wordQueryDesc') }}</span>
            </div>
            <div class="feature-item">
              <el-icon><ChatDotRound /></el-icon>
              <span>{{ t('features.dialogueDesc') }}</span>
            </div>
            <div class="feature-item">
              <el-icon><Document /></el-icon>
              <span>{{ t('features.quizDesc') }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- Right Side - Form -->
      <div class="form-section">
        <div class="form-wrapper">
          <div class="form-header">
            <h2>{{ t('auth.createAccount') }}</h2>
            <p>{{ t('auth.registerSubtitle') }}</p>
          </div>

          <el-form
            ref="formRef"
            :model="registerForm"
            :rules="rules"
            label-position="top"
            class="register-form"
            @submit.prevent="handleRegister"
          >
            <el-form-item :label="t('auth.username')" prop="username">
              <el-input
                v-model="registerForm.username"
                :placeholder="t('validation.enterUsername')"
                :prefix-icon="User"
                size="large"
              />
            </el-form-item>

            <el-form-item :label="t('auth.email')" prop="email">
              <el-input
                v-model="registerForm.email"
                :placeholder="t('validation.enterEmail')"
                :prefix-icon="Message"
                size="large"
              />
            </el-form-item>

            <el-form-item :label="t('auth.password')" prop="password">
              <el-input
                v-model="registerForm.password"
                type="password"
                :placeholder="t('validation.enterPassword')"
                :prefix-icon="Lock"
                size="large"
                show-password
              />
            </el-form-item>

            <el-form-item :label="t('auth.confirmPassword')" prop="confirmPassword">
              <el-input
                v-model="registerForm.confirmPassword"
                type="password"
                :placeholder="t('validation.confirmPassword')"
                :prefix-icon="Lock"
                size="large"
                show-password
                @keyup.enter="handleRegister"
              />
            </el-form-item>

            <el-form-item>
              <el-button
                type="primary"
                size="large"
                :loading="authStore.loading"
                class="submit-btn"
                @click="handleRegister"
              >
                {{ t('auth.createAccount') }}
              </el-button>
            </el-form-item>
          </el-form>

          <div class="form-footer">
            <span>{{ t('auth.hasAccount') }}</span>
            <el-button type="primary" link @click="goToLogin">
              {{ t('auth.signInNow') }}
            </el-button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.register-page {
  display: flex;
  min-height: 100vh;
  background-color: var(--bg-primary);
  position: relative;
}

.bg-pattern {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-image: 
    radial-gradient(circle at 20% 80%, rgba(201, 162, 39, 0.05) 0%, transparent 50%),
    radial-gradient(circle at 80% 20%, rgba(26, 26, 46, 0.03) 0%, transparent 50%);
  pointer-events: none;
}

.language-wrapper {
  position: absolute;
  top: 20px;
  right: 24px;
  z-index: 10;
}

.register-container {
  display: flex;
  width: 100%;
  max-width: 1200px;
  margin: 0 auto;
  min-height: 100vh;
}

/* Branding Section */
.branding-section {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 48px;
  background: linear-gradient(135deg, var(--color-primary) 0%, var(--color-primary-light) 100%);
  position: relative;
  overflow: hidden;
}

.branding-section::before {
  content: '';
  position: absolute;
  top: -50%;
  right: -50%;
  width: 100%;
  height: 100%;
  background: radial-gradient(circle, rgba(201, 162, 39, 0.1) 0%, transparent 70%);
}

.brand-content {
  position: relative;
  z-index: 1;
  color: var(--text-inverse);
  max-width: 400px;
}

.logo {
  margin-bottom: 24px;
}

.logo-icon {
  font-size: 64px;
}

.brand-title {
  font-size: var(--font-size-3xl);
  font-weight: 700;
  margin-bottom: 12px;
  color: var(--text-inverse);
}

.brand-subtitle {
  font-size: var(--font-size-lg);
  opacity: 0.9;
  margin-bottom: 48px;
}

.features-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.feature-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  background: rgba(255, 255, 255, 0.1);
  border-radius: var(--radius-md);
  backdrop-filter: blur(10px);
}

.feature-item .el-icon {
  font-size: 20px;
  color: var(--color-accent);
}

.feature-item span {
  font-size: var(--font-size-sm);
}

/* Form Section */
.form-section {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 48px;
  background: var(--bg-secondary);
}

.form-wrapper {
  width: 100%;
  max-width: 400px;
}

.form-header {
  margin-bottom: 32px;
}

.form-header h2 {
  font-size: var(--font-size-2xl);
  font-weight: 700;
  color: var(--text-primary);
  margin-bottom: 8px;
}

.form-header p {
  color: var(--text-secondary);
  font-size: var(--font-size-sm);
}

.register-form :deep(.el-form-item__label) {
  font-weight: 500;
  color: var(--text-primary);
}

.register-form :deep(.el-input__wrapper) {
  padding: 4px 12px;
  box-shadow: 0 0 0 1px var(--border-color) inset;
}

.register-form :deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px var(--color-accent) inset;
}

.register-form :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px var(--color-accent) inset;
}

.submit-btn {
  width: 100%;
  height: 48px;
  font-size: var(--font-size-md);
  font-weight: 600;
  border-radius: var(--radius-md);
}

.form-footer {
  text-align: center;
  margin-top: 24px;
  color: var(--text-secondary);
  font-size: var(--font-size-sm);
}

.form-footer .el-button {
  font-weight: 600;
}

/* Responsive */
@media (max-width: 768px) {
  .register-container {
    flex-direction: column;
  }
  
  .branding-section {
    padding: 32px;
    min-height: auto;
  }
  
  .brand-content {
    text-align: center;
  }
  
  .features-list {
    display: none;
  }
  
  .form-section {
    padding: 32px 24px;
  }
}
</style>
