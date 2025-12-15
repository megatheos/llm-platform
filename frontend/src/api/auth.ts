import request from './request'
import type { Result, LoginRequest, LoginResponse, RegisterRequest, RegisterResponse } from '@/types'

/**
 * User login
 * @param data Login credentials
 * @returns Login response with token and user info
 */
export function login(data: LoginRequest): Promise<Result<LoginResponse>> {
  return request.post('/auth/login', data).then(res => res.data)
}

/**
 * User registration
 * @param data Registration data
 * @returns Registration response with user info
 */
export function register(data: RegisterRequest): Promise<Result<RegisterResponse>> {
  return request.post('/auth/register', data).then(res => res.data)
}

/**
 * User logout
 * @returns void
 */
export function logout(): Promise<Result<void>> {
  return request.post('/auth/logout').then(res => res.data)
}

/**
 * Get current user info
 * @returns Current user data
 */
export function getCurrentUser(): Promise<Result<LoginResponse['user']>> {
  return request.get('/auth/me').then(res => res.data)
}
