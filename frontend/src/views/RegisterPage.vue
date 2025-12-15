<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { User, Lock, Message } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'

interface RegisterForm {
  username: string
  email: string
  password: string
  confirmPassword: string
}

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
    callback(new Error('Passwords do not match'))
  } else {
    callback()
  }
}

const rules: FormRules<RegisterForm> = {
  username: [
    { required: true, message: 'Please enter username', trigger: 'blur' },
    { min: 3, max: 50, message: 'Username must be 3-50 characters', trigger: 'blur' }
  ],
  email: [
    { required: true, message: 'Please enter email', trigger: 'blur' },
    { type: 'email', message: 'Please enter a valid email', trigger: 'blur' }
  ],
  password: [
    { required: true, message: 'Please enter password', trigger: 'blur' },
    { min: 6, max: 100, message: 'Password must be at least 6 characters', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: 'Please confirm password', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' }
  ]
}

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
        ElMessage.success('Registration successful! Please login.')
        router.push('/login')
      }
    } catch (error: any) {
      ElMessage.error(error.message || 'Registration failed')
    }
  })
}

function goToLogin() {
  router.push('/login')
}
</script>

<template>
  <div class="register-page">
    <div class="register-container">
      <div class="register-header">
        <h1>Language Learning Platform</h1>
        <p>Create an account to start learning</p>
      </div>

      <el-card class="register-card">
        <el-form
          ref="formRef"
          :model="registerForm"
          :rules="rules"
          label-position="top"
          @submit.prevent="handleRegister"
        >
          <el-form-item label="Username" prop="username">
            <el-input
              v-model="registerForm.username"
              placeholder="Choose a username"
              :prefix-icon="User"
              size="large"
            />
          </el-form-item>

          <el-form-item label="Email" prop="email">
            <el-input
              v-model="registerForm.email"
              placeholder="Enter your email"
              :prefix-icon="Message"
              size="large"
            />
          </el-form-item>

          <el-form-item label="Password" prop="password">
            <el-input
              v-model="registerForm.password"
              type="password"
              placeholder="Create a password"
              :prefix-icon="Lock"
              size="large"
              show-password
            />
          </el-form-item>

          <el-form-item label="Confirm Password" prop="confirmPassword">
            <el-input
              v-model="registerForm.confirmPassword"
              type="password"
              placeholder="Confirm your password"
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
              class="register-button"
              @click="handleRegister"
            >
              Create Account
            </el-button>
          </el-form-item>
        </el-form>

        <div class="register-footer">
          <span>Already have an account?</span>
          <el-button type="primary" link @click="goToLogin">
            Sign in
          </el-button>
        </div>
      </el-card>
    </div>
  </div>
</template>

<style scoped>
.register-page {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.register-container {
  width: 100%;
  max-width: 420px;
  padding: 20px;
}

.register-header {
  text-align: center;
  margin-bottom: 30px;
  color: white;
}

.register-header h1 {
  font-size: 28px;
  margin-bottom: 10px;
}

.register-header p {
  font-size: 14px;
  opacity: 0.9;
}

.register-card {
  border-radius: 12px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.2);
}

.register-button {
  width: 100%;
}

.register-footer {
  text-align: center;
  margin-top: 20px;
  color: #666;
}
</style>
