import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { api } from '../lib/api'
import { useAuthStore } from '../store/authStore'
import type { StudySettings, UserProfile } from '../types/api'

export function ProfilePage() {
  const navigate = useNavigate()
  const clearTokens = useAuthStore((state) => state.clearTokens)

  const [profile, setProfile] = useState<UserProfile | null>(null)
  const [loading, setLoading] = useState(true)

  const [name, setName] = useState('')
  const [nameSaving, setNameSaving] = useState(false)
  const [nameSuccess, setNameSuccess] = useState(false)
  const [nameError, setNameError] = useState<string | null>(null)

  const [currentPassword, setCurrentPassword] = useState('')
  const [newPassword, setNewPassword] = useState('')
  const [pwSaving, setPwSaving] = useState(false)
  const [pwSuccess, setPwSuccess] = useState(false)
  const [pwError, setPwError] = useState<string | null>(null)

  const [studySettings, setStudySettings] = useState<StudySettings>({
    newCardsPerSession: 20,
    intervalModifier: 1.0,
  })
  const [studySaving, setStudySaving] = useState(false)
  const [studySuccess, setStudySuccess] = useState(false)
  const [studyError, setStudyError] = useState<string | null>(null)

  useEffect(() => {
    void (async () => {
      try {
        const { data } = await api.get<UserProfile>('/api/users/me')
        setProfile(data)
        setName(data.name ?? '')
        const settingsRes = await api.get<StudySettings>('/api/users/me/study-settings')
        setStudySettings(settingsRes.data)
      } catch {
      } finally {
        setLoading(false)
      }
    })()
  }, [])

  const handleUpdateName = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!name.trim()) return
    setNameSaving(true)
    setNameError(null)
    setNameSuccess(false)
    try {
      const { data } = await api.put<UserProfile>('/api/users/me', { name: name.trim() })
      setProfile(data)
      setNameSuccess(true)
      setTimeout(() => setNameSuccess(false), 3000)
    } catch {
      setNameError('Не удалось обновить имя')
    } finally {
      setNameSaving(false)
    }
  }

  const handleChangePassword = async (e: React.FormEvent) => {
    e.preventDefault()
    setPwSaving(true)
    setPwError(null)
    setPwSuccess(false)
    try {
      await api.patch('/api/users/me/password', { currentPassword, newPassword })
      setCurrentPassword('')
      setNewPassword('')
      setPwSuccess(true)
      setTimeout(() => setPwSuccess(false), 3000)
      clearTokens()
    } catch (err: unknown) {
      const status = (err as { response?: { status?: number } })?.response?.status
      setPwError(status === 400 ? 'Неверный текущий пароль или слишком короткий новый (мин. 8 символов)' : 'Не удалось сменить пароль')
    } finally {
      setPwSaving(false)
    }
  }

  const handleSaveStudySettings = async (e: React.FormEvent) => {
    e.preventDefault()
    setStudySaving(true)
    setStudyError(null)
    setStudySuccess(false)
    try {
      const payload: StudySettings = {
        newCardsPerSession: Number(studySettings.newCardsPerSession),
        intervalModifier: Number(studySettings.intervalModifier.toFixed(2)),
      }
      const { data } = await api.put<StudySettings>('/api/users/me/study-settings', payload)
      setStudySettings(data)
      setStudySuccess(true)
      setTimeout(() => setStudySuccess(false), 3000)
    } catch {
      setStudyError('Не удалось сохранить настройки повторений')
    } finally {
      setStudySaving(false)
    }
  }

  if (loading) {
    return (
      <div className="flex min-h-screen items-center justify-center">
        <p className="text-slate-500">Загрузка...</p>
      </div>
    )
  }

  return (
    <div className="mx-auto max-w-lg px-4 py-6">
      <div className="mb-6">
        <button
          className="mb-1 text-sm text-slate-500 hover:text-slate-700"
          onClick={() => navigate('/decks')}
        >
          ← Мои колоды
        </button>
        <h1 className="text-2xl font-semibold">Профиль</h1>
      </div>

      <div className="mb-4 rounded-xl border border-slate-200 bg-white p-4 shadow-sm">
        <p className="text-sm text-slate-500">Email</p>
        <p className="font-medium">{profile?.email}</p>
        {profile?.name ? (
          <>
            <p className="mt-2 text-sm text-slate-500">Имя</p>
            <p className="font-medium">{profile.name}</p>
          </>
        ) : null}
        <p className="mt-2 text-xs text-slate-400">
          Аккаунт создан:{' '}
          {profile?.createdAt ? new Date(profile.createdAt).toLocaleDateString('ru-RU') : '—'}
        </p>
        {profile?.role === 'ADMIN' ? (
          <button
            className="mt-3 rounded bg-slate-900 px-3 py-2 text-xs text-white"
            onClick={() => navigate('/admin')}
          >
            Открыть админ-панель
          </button>
        ) : null}
      </div>

      <form
        onSubmit={(e) => void handleUpdateName(e)}
        className="mb-4 rounded-xl border border-slate-200 bg-white p-4 shadow-sm"
      >
        <p className="mb-3 text-sm font-medium text-slate-700">Изменить имя</p>
        <input
          className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm focus:border-slate-500 focus:outline-none"
          placeholder="Ваше имя"
          value={name}
          onChange={(e) => setName(e.target.value)}
          required
        />
        {nameError ? <p className="mt-2 text-xs text-red-600">{nameError}</p> : null}
        {nameSuccess ? <p className="mt-2 text-xs text-green-600">Имя обновлено</p> : null}
        <button
          type="submit"
          disabled={nameSaving}
          className="mt-3 w-full rounded-lg bg-slate-900 py-2 text-sm font-medium text-white disabled:opacity-50"
        >
          {nameSaving ? 'Сохраняем...' : 'Сохранить'}
        </button>
      </form>

      <form
        onSubmit={(e) => void handleChangePassword(e)}
        className="mb-4 rounded-xl border border-slate-200 bg-white p-4 shadow-sm"
      >
        <p className="mb-3 text-sm font-medium text-slate-700">Сменить пароль</p>
        <div className="space-y-2">
          <input
            type="password"
            className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm focus:border-slate-500 focus:outline-none"
            placeholder="Текущий пароль"
            value={currentPassword}
            onChange={(e) => setCurrentPassword(e.target.value)}
            required
          />
          <input
            type="password"
            className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm focus:border-slate-500 focus:outline-none"
            placeholder="Новый пароль (мин. 8 символов)"
            value={newPassword}
            onChange={(e) => setNewPassword(e.target.value)}
            required
            minLength={8}
          />
        </div>
        {pwError ? <p className="mt-2 text-xs text-red-600">{pwError}</p> : null}
        {pwSuccess ? <p className="mt-2 text-xs text-green-600">Пароль изменён, выполняется выход...</p> : null}
        <button
          type="submit"
          disabled={pwSaving}
          className="mt-3 w-full rounded-lg bg-red-600 py-2 text-sm font-medium text-white disabled:opacity-50"
        >
          {pwSaving ? 'Меняем...' : 'Сменить пароль'}
        </button>
        <p className="mt-2 text-center text-xs text-slate-400">После смены пароля сессия завершится</p>
      </form>

      <form
        onSubmit={(e) => void handleSaveStudySettings(e)}
        className="rounded-xl border border-slate-200 bg-white p-4 shadow-sm"
      >
        <p className="mb-3 text-sm font-medium text-slate-700">Настройки повторений</p>
        <div className="space-y-3">
          <label className="block">
            <span className="mb-1 block text-xs text-slate-500">Новых карточек за сессию (1-100)</span>
            <input
              type="number"
              min={1}
              max={100}
              className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm focus:border-slate-500 focus:outline-none"
              value={studySettings.newCardsPerSession}
              onChange={(e) =>
                setStudySettings((prev) => ({ ...prev, newCardsPerSession: Number(e.target.value || 1) }))
              }
              required
            />
          </label>

          <label className="block">
            <span className="mb-1 block text-xs text-slate-500">Модификатор интервалов (0.5-2.0)</span>
            <input
              type="number"
              min={0.5}
              max={2.0}
              step={0.1}
              className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm focus:border-slate-500 focus:outline-none"
              value={studySettings.intervalModifier}
              onChange={(e) =>
                setStudySettings((prev) => ({ ...prev, intervalModifier: Number(e.target.value || 1) }))
              }
              required
            />
            <p className="mt-1 text-xs text-slate-400">Меньше 1.0 — чаще повторения, больше 1.0 — реже.</p>
          </label>
        </div>

        {studyError ? <p className="mt-2 text-xs text-red-600">{studyError}</p> : null}
        {studySuccess ? <p className="mt-2 text-xs text-green-600">Настройки сохранены</p> : null}
        <button
          type="submit"
          disabled={studySaving}
          className="mt-3 w-full rounded-lg bg-emerald-600 py-2 text-sm font-medium text-white disabled:opacity-50"
        >
          {studySaving ? 'Сохраняем...' : 'Сохранить настройки повторений'}
        </button>
      </form>
    </div>
  )
}
