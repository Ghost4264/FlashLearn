import axios, { type InternalAxiosRequestConfig } from 'axios'
import { useAuthStore } from '../store/authStore'
import type { AuthResponse } from '../types/api'

export const api = axios.create({
  baseURL: 'http://localhost:8080',
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
      return Promise.reject(error)
    }

    if (error.response?.status !== 401 || originalRequest._retry) {
      return Promise.reject(error)
    }

    const store = useAuthStore.getState()
    if (!store.refreshToken) {
      store.clearTokens()
      return Promise.reject(error)
    }

    originalRequest._retry = true
    try {
      const { data } = await axios.post<AuthResponse>('http://localhost:8080/api/auth/refresh', {
        refreshToken: store.refreshToken,
      })
      store.setTokens(data.accessToken, data.refreshToken)
      originalRequest.headers.Authorization = `Bearer ${data.accessToken}`
      return api(originalRequest)
    } catch (refreshError) {
      store.clearTokens()
      return Promise.reject(refreshError)
    }
  },
)
