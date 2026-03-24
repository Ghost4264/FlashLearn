import axios, { type InternalAxiosRequestConfig } from 'axios'
import { useAuthStore } from '../store/authStore'
import type { AuthResponse } from '../types/api'

const apiBase = (import.meta.env.VITE_API_BASE_URL ?? '').replace(/\/$/, '')

export const api = axios.create({
  baseURL: apiBase || undefined,
  headers: { 'Content-Type': 'application/json' },
})

type RetryableConfig = InternalAxiosRequestConfig & { _retry?: boolean }

api.interceptors.request.use((config) => {
  const accessToken = useAuthStore.getState().accessToken
  if (accessToken) {
    config.headers.Authorization = `Bearer ${accessToken}`
  }
  return config
})

api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config as RetryableConfig | undefined
    if (!originalRequest) {
      throw error
    }

    if (error.response?.status !== 401 || originalRequest._retry) {
      throw error
    }

    const store = useAuthStore.getState()
    if (!store.refreshToken) {
      store.clearTokens()
      throw error
    }

    originalRequest._retry = true
    try {
      const refreshUrl = apiBase ? `${apiBase}/api/auth/refresh` : '/api/auth/refresh'
      const { data } = await axios.post<AuthResponse>(refreshUrl, {
        refreshToken: store.refreshToken,
      })
      store.setTokens(data.accessToken, data.refreshToken)
      originalRequest.headers.Authorization = `Bearer ${data.accessToken}`
      return api(originalRequest)
    } catch (refreshError) {
      store.clearTokens()
      throw refreshError
    }
  },
)
