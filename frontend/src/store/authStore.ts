import { create } from 'zustand'

type AuthState = {
  accessToken: string | null
  refreshToken: string | null
  welcomePending: boolean
  setTokens: (accessToken: string, refreshToken: string) => void
  requestWelcomeAfterLogin: () => void
  clearWelcome: () => void
  clearTokens: () => void
}

const TOKEN_KEY = 'flashlearn.auth'

const readStorage = (): { accessToken: string | null; refreshToken: string | null } => {
  const raw = localStorage.getItem(TOKEN_KEY)
  if (!raw) {
    return { accessToken: null, refreshToken: null }
  }

  try {
    return JSON.parse(raw)
  } catch {
    return { accessToken: null, refreshToken: null }
  }
}

const writeStorage = (accessToken: string | null, refreshToken: string | null): void => {
  if (!accessToken || !refreshToken) {
    localStorage.removeItem(TOKEN_KEY)
    return
  }
  localStorage.setItem(TOKEN_KEY, JSON.stringify({ accessToken, refreshToken }))
}

const initial = readStorage()

export const useAuthStore = create<AuthState>((set) => ({
  accessToken: initial.accessToken,
  refreshToken: initial.refreshToken,
  welcomePending: false,
  setTokens: (accessToken, refreshToken) => {
    writeStorage(accessToken, refreshToken)
    set({ accessToken, refreshToken })
  },
  requestWelcomeAfterLogin: () => set({ welcomePending: true }),
  clearWelcome: () => set({ welcomePending: false }),
  clearTokens: () => {
    writeStorage(null, null)
    set({ accessToken: null, refreshToken: null, welcomePending: false })
  },
}))
