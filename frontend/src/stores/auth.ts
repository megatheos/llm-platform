import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { login as apiLogin, register as apiRegister, logout as apiLogout } from '@/api/auth'
import type { User, LoginRequest, RegisterRequest } from '@/types'

export const useAuthStore = defineStore('auth', () => {
  const user = ref<User | null>(null)
  const token = ref<string | null>(localStorage.getItem('token'))
  const loading = ref(false)

  const isAuthenticated = computed(() => !!token.value)

  function setUser(userData: User) {
    user.value = userData
  }

  function setToken(newToken: string) {
    token.value = newToken
    localStorage.setItem('token', newToken)
  }

  function clearAuth() {
    user.value = null
    token.value = null
    localStorage.removeItem('token')
  }

  /**
   * Login user with credentials
   * @param credentials Login credentials (username, password)
   * @returns Promise resolving to login success
   */
  async function login(credentials: LoginRequest): Promise<boolean> {
    loading.value = true
    try {
      const result = await apiLogin(credentials)
      if (result.code === 0 || result.code === 200) {
        setToken(result.data.token)
        setUser(result.data.user)
        return true
      }
      return false
    } catch (error) {
      clearAuth()
      throw error
    } finally {
      loading.value = false
    }
  }

  /**
   * Register new user
   * @param data Registration data (username, password, email)
   * @returns Promise resolving to registration success
   */
  async function register(data: RegisterRequest): Promise<boolean> {
    loading.value = true
    try {
      const result = await apiRegister(data)
      if (result.code === 0 || result.code === 200) {
        return true
      }
      return false
    } catch (error) {
      throw error
    } finally {
      loading.value = false
    }
  }

  /**
   * Logout current user
   * @returns Promise resolving when logout completes
   */
  async function logout(): Promise<void> {
    loading.value = true
    try {
      await apiLogout()
    } catch (error) {
      // Even if API call fails, clear local auth state
      console.error('Logout API error:', error)
    } finally {
      clearAuth()
      loading.value = false
    }
  }

  return {
    user,
    token,
    loading,
    isAuthenticated,
    setUser,
    setToken,
    clearAuth,
    login,
    register,
    logout
  }
})
