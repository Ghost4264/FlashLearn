import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { api } from '../lib/api'
import { useAuthStore } from '../store/authStore'
import type { AuthResponse } from '../types/api'

export function RegisterPage() {
  const navigate = useNavigate()
  const setTokens = useAuthStore((state) => state.setTokens)
  const [name, setName] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)

  const onSubmit = async (event: React.SyntheticEvent<HTMLFormElement>): Promise<void> => {
    event.preventDefault()
    setError(null)
    setLoading(true)
    try {
      const { data } = await api.post<AuthResponse>('/api/auth/register', { name, email, password })
      setTokens(data.accessToken, data.refreshToken)
      navigate('/decks')
    } catch {
      setError('Не удалось зарегистрироваться. Проверьте данные.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="mx-auto mt-12 max-w-md rounded-xl bg-white p-6 shadow-sm">
      <h1 className="mb-4 text-2xl font-semibold">Регистрация</h1>
      <form className="space-y-3" onSubmit={onSubmit}>
        <input
          className="w-full rounded border border-slate-300 px-3 py-2"
          type="text"
          placeholder="Имя"
          value={name}
          onChange={(e) => setName(e.target.value)}
          required
        />
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
          placeholder="Пароль (минимум 8 символов)"
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
          {loading ? 'Создаем аккаунт...' : 'Зарегистрироваться'}
        </button>
      </form>
      <p className="mt-4 text-sm text-slate-600">
        Уже есть аккаунт?{' '}
        <Link className="text-slate-900 underline" to="/login">
          Войти
        </Link>
      </p>
    </div>
  )
}
