import { useCallback, useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { api } from '../lib/api'
import { useAuthStore } from '../store/authStore'
import { toast } from '../store/toastStore'
import type { StudySettings, UserProfile } from '../types/api'

export function ProfilePage() {
  const navigate = useNavigate()
  const clearTokens = useAuthStore((state) => state.clearTokens)

  const [profile, setProfile] = useState<UserProfile | null>(null)
  const [loading, setLoading] = useState(true)
  const [loadError, setLoadError] = useState<string | null>(null)

  const [name, setName] = useState('')
  const [nameSaving, setNameSaving] = useState(false)

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

  const loadProfile = useCallback(async () => {
    setLoading(true)
    setLoadError(null)
    try {
      const { data } = await api.get<UserProfile>('/api/users/me')
      setProfile(data)
      setName(data.name ?? '')
      const settingsRes = await api.get<StudySettings>('/api/users/me/study-settings')
      setStudySettings(settingsRes.data)
    } catch {
      setLoadError('Не удалось загрузить профиль. Проверьте соединение и попробуйте снова.')
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    void loadProfile()
  }, [loadProfile])

  const handleLogout = (): void => {
    clearTokens()
    navigate('/')
  }

  const handleUpdateName = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!name.trim()) return
    setNameSaving(true)
    try {
      const { data } = await api.put<UserProfile>('/api/users/me', { name: name.trim() })
      setProfile(data)
      toast.success('Имя сохранено')
    } catch {
      toast.error('Не удалось обновить имя')
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
      window.setTimeout(() => setPwSuccess(false), 3000)
      clearTokens()
      navigate('/')
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
    try {
      const payload: StudySettings = {
        newCardsPerSession: Number(studySettings.newCardsPerSession),
        intervalModifier: Number(studySettings.intervalModifier.toFixed(2)),
      }
      const { data } = await api.put<StudySettings>('/api/users/me/study-settings', payload)
      setStudySettings(data)
      toast.success('Настройки повторений сохранены')
    } catch {
      toast.error('Не удалось сохранить настройки повторений')
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

  if (loadError) {
    return (
      <div className="mx-auto max-w-lg px-4 py-10 text-center">
        <p className="text-slate-700">{loadError}</p>
        <button
          type="button"
          className="mt-4 rounded-lg bg-slate-900 px-4 py-2 text-sm font-medium text-white hover:bg-slate-800"
          onClick={() => void loadProfile()}
        >
          Повторить
        </button>
      </div>
    )
  }

  return (
    <div className="mx-auto max-w-lg px-4 py-6">
      <div className="mb-6 flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 className="text-xl font-semibold sm:text-2xl">Профиль</h1>
          <p className="mt-0.5 text-xs text-slate-500">Аккаунт, безопасность и настройки обучения</p>
        </div>
        <div className="flex flex-wrap items-center gap-2">
          <button
            type="button"
            className="rounded-lg border border-slate-300 px-3 py-1.5 text-sm hover:bg-slate-50"
            onClick={() => navigate('/decks')}
          >
            Мои колоды
          </button>
          <button
            type="button"
            className="rounded-lg border border-slate-300 px-3 py-1.5 text-sm hover:bg-slate-50"
            onClick={handleLogout}
          >
            Выйти
          </button>
        </div>
      </div>

      <section className="mb-4 rounded-xl border border-slate-200 bg-white p-4 shadow-sm">
        <h2 className="mb-3 text-sm font-medium text-slate-800">Аккаунт</h2>
        <p className="text-sm text-slate-500">Email</p>
        <p className="font-medium">{profile?.email}</p>
        <p className="mt-2 text-xs text-slate-400">
          Аккаунт создан:{' '}
          {profile?.createdAt ? new Date(profile.createdAt).toLocaleDateString('ru-RU') : '—'}
        </p>
        {profile?.role === 'ADMIN' ? (
          <div className="mt-3 flex flex-wrap items-center gap-2">
            <span className="rounded-full bg-amber-100 px-2 py-0.5 text-xs font-medium text-amber-900">
              Администратор
            </span>
            <button
              type="button"
              className="rounded-lg bg-slate-900 px-3 py-1.5 text-xs text-white hover:bg-slate-800"
              onClick={() => navigate('/admin')}
            >
              Админ-панель
            </button>
          </div>
        ) : null}
      </section>

      <form
        onSubmit={(e) => void handleUpdateName(e)}
        className="mb-4 rounded-xl border border-slate-200 bg-white p-4 shadow-sm"
      >
        <h2 className="mb-3 text-sm font-medium text-slate-800">Имя</h2>
        <label htmlFor="profile-name" className="sr-only">
          Отображаемое имя
        </label>
        <input
          id="profile-name"
          name="name"
          className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm focus:border-slate-500 focus:outline-none focus:ring-2 focus:ring-slate-400/30"
          placeholder="Как к вам обращаться"
          value={name}
          onChange={(e) => setName(e.target.value)}
          autoComplete="name"
          required
        />
        <button
          type="submit"
          disabled={nameSaving}
          className="mt-3 w-full rounded-lg bg-slate-900 py-2 text-sm font-medium text-white hover:bg-slate-800 disabled:opacity-50 sm:w-auto sm:px-6"
        >
          {nameSaving ? 'Сохраняем...' : 'Сохранить'}
        </button>
      </form>

      <form
        onSubmit={(e) => void handleChangePassword(e)}
        className="mb-4 rounded-xl border border-slate-200 bg-white p-4 shadow-sm"
      >
        <h2 className="mb-1 text-sm font-medium text-slate-800">Безопасность</h2>
        <p className="mb-3 text-xs text-slate-500">Смена пароля завершит текущую сессию — нужно будет войти снова.</p>
        <div className="space-y-3">
          <div>
            <label htmlFor="profile-current-password" className="mb-1 block text-xs font-medium text-slate-600">
              Текущий пароль
            </label>
            <input
              id="profile-current-password"
              type="password"
              name="current-password"
              className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm focus:border-slate-500 focus:outline-none focus:ring-2 focus:ring-slate-400/30"
              value={currentPassword}
              onChange={(e) => setCurrentPassword(e.target.value)}
              autoComplete="current-password"
              required
            />
          </div>
          <div>
            <label htmlFor="profile-new-password" className="mb-1 block text-xs font-medium text-slate-600">
              Новый пароль (мин. 8 символов)
            </label>
            <input
              id="profile-new-password"
              type="password"
              name="new-password"
              className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm focus:border-slate-500 focus:outline-none focus:ring-2 focus:ring-slate-400/30"
              value={newPassword}
              onChange={(e) => setNewPassword(e.target.value)}
              autoComplete="new-password"
              required
              minLength={8}
            />
          </div>
        </div>
        {pwError ? <p className="mt-2 text-xs text-red-600">{pwError}</p> : null}
        {pwSuccess ? <p className="mt-2 text-xs text-green-600">Пароль изменён, выход...</p> : null}
        <button
          type="submit"
          disabled={pwSaving}
          className="mt-3 w-full rounded-lg border border-slate-300 bg-white py-2 text-sm font-medium text-slate-900 hover:bg-slate-50 disabled:opacity-50 sm:w-auto sm:px-6"
        >
          {pwSaving ? 'Меняем...' : 'Сменить пароль'}
        </button>
        <p className="mt-2 text-center text-xs text-slate-400">После смены пароля сессия завершится</p>
      </form>

      <form
        onSubmit={(e) => void handleSaveStudySettings(e)}
        className="rounded-xl border border-slate-200 bg-white p-4 shadow-sm"
      >
        <h2 className="mb-1 text-sm font-medium text-slate-800">Обучение</h2>
        <p className="mb-3 text-xs text-slate-500">
          Влияют на режим повторений: сколько новых карточек за раз и насколько «растягивать» интервалы.
        </p>
        <div className="space-y-3">
          <div>
            <label htmlFor="profile-new-cards" className="mb-1 block text-xs font-medium text-slate-600">
              Новых карточек за сессию (1–100)
            </label>
            <input
              id="profile-new-cards"
              type="number"
              min={1}
              max={100}
              className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm focus:border-slate-500 focus:outline-none focus:ring-2 focus:ring-slate-400/30"
              value={studySettings.newCardsPerSession}
              onChange={(e) =>
                setStudySettings((prev) => ({ ...prev, newCardsPerSession: Number(e.target.value || 1) }))
              }
              required
            />
          </div>

          <div>
            <label htmlFor="profile-interval-mod" className="mb-1 block text-xs font-medium text-slate-600">
              Модификатор интервалов (0.5–2.0)
            </label>
            <input
              id="profile-interval-mod"
              type="number"
              min={0.5}
              max={2.0}
              step={0.1}
              className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm focus:border-slate-500 focus:outline-none focus:ring-2 focus:ring-slate-400/30"
              value={studySettings.intervalModifier}
              onChange={(e) =>
                setStudySettings((prev) => ({ ...prev, intervalModifier: Number(e.target.value || 1) }))
              }
              required
            />
            <p className="mt-1 text-xs text-slate-400">Меньше 1.0 — чаще повторения, больше 1.0 — реже.</p>
          </div>
        </div>

        <button
          type="submit"
          disabled={studySaving}
          className="mt-3 w-full rounded-lg bg-emerald-600 py-2 text-sm font-medium text-white hover:bg-emerald-500 disabled:opacity-50 sm:w-auto sm:px-6"
        >
          {studySaving ? 'Сохраняем...' : 'Сохранить настройки'}
        </button>
      </form>
    </div>
  )
}
