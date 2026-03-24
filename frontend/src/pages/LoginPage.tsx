import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { api } from '../lib/api'
import { useAuthStore } from '../store/authStore'
import type { AuthResponse } from '../types/api'

export function LoginPage() {
  const navigate = useNavigate()
  const setTokens = useAuthStore((state) => state.setTokens)
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)

  const onSubmit = async (): Promise<void> => {
    setError(null)
    setLoading(true)
    try {
      const { data } = await api.post<AuthResponse>('/api/auth/login', { email, password })
      setTokens(data.accessToken, data.refreshToken)
      navigate('/decks')
    } catch {
      setError('Неверный email или пароль')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="relative min-h-screen overflow-hidden bg-slate-950">
      <img
        src="https://images.unsplash.com/photo-1456513080510-7bf3a84b82f8?auto=format&fit=crop&w=1600&q=80"
        alt="Книги и обучение"
        className="absolute inset-0 h-full w-full object-cover opacity-30"
      />
      <div className="absolute inset-0 bg-gradient-to-b from-slate-950/70 via-slate-900/70 to-slate-950" />

      <div className="relative mx-auto flex min-h-screen max-w-md items-center px-4 py-8">
        <div className="w-full rounded-xl bg-white p-6 shadow-sm">
          <h1 className="mb-4 text-2xl font-semibold">Вход в FlashLearn</h1>
          <form
            className="space-y-3"
            onSubmit={(event) => {
              event.preventDefault()
              void onSubmit()
            }}
          >
            <input
              className="w-full rounded border border-slate-300 px-3 py-2"
              type="email"
              placeholder="Email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              required
            />
            <input
              className="w-full rounded border border-slate-300 px-3 py-2"
              type="password"
              placeholder="Пароль"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
            />
            {error ? <p className="text-sm text-red-600">{error}</p> : null}
            <button
              className="w-full rounded bg-slate-900 px-3 py-2 text-white disabled:opacity-60"
              type="submit"
              disabled={loading}
            >
              {loading ? 'Входим...' : 'Войти'}
            </button>
          </form>
          <p className="mt-4 text-sm text-slate-600">
            Нет аккаунта?{' '}
            <Link className="text-slate-900 underline" to="/register">
              Регистрация
            </Link>
          </p>
        </div>
      </div>
    </div>
  )
}
