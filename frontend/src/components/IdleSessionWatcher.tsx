import { useEffect, useRef } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuthStore } from '../store/authStore'

const IDLE_MS = 60 * 60 * 1000
const CHECK_INTERVAL_MS = 30_000

const ACTIVITY_EVENTS: (keyof WindowEventMap)[] = [
  'mousedown',
  'keydown',
  'scroll',
  'touchstart',
  'click',
  'wheel',
]

/**
 * Сбрасывает локальную сессию, если пользователь не взаимодействует с приложением
 */
export function IdleSessionWatcher() {
  const navigate = useNavigate()
  const accessToken = useAuthStore((s) => s.accessToken)
  const clearTokens = useAuthStore((s) => s.clearTokens)
  const lastActivityRef = useRef(0)

  useEffect(() => {
    if (!accessToken) {
      return
    }

    lastActivityRef.current = Date.now()

    const logoutIfIdle = () => {
      if (Date.now() - lastActivityRef.current >= IDLE_MS) {
        clearTokens()
        navigate('/', { replace: true })
      }
    }

    const mark = () => {
      lastActivityRef.current = Date.now()
    }

    const onVisibility = () => {
      if (document.visibilityState === 'visible') {
        logoutIfIdle()
      }
    }

    const intervalId = window.setInterval(logoutIfIdle, CHECK_INTERVAL_MS)
    for (const type of ACTIVITY_EVENTS) {
      window.addEventListener(type, mark, { passive: true })
    }
    document.addEventListener('visibilitychange', onVisibility)

    return () => {
      window.clearInterval(intervalId)
      for (const type of ACTIVITY_EVENTS) {
        window.removeEventListener(type, mark)
      }
      document.removeEventListener('visibilitychange', onVisibility)
    }
  }, [accessToken, clearTokens, navigate])

  return null
}
