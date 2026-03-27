import { useCallback, useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { api } from '../lib/api'
import type { AdminBulkDeckResponse, Category, Deck, PageResponse, UserProfile } from '../types/api'

export function AdminPage() {
  const navigate = useNavigate()
  const [loading, setLoading] = useState(true)
  const [forbidden, setForbidden] = useState(false)

  const [categories, setCategories] = useState<Category[]>([])
  const [presetName, setPresetName] = useState('')
  const [presetSaving, setPresetSaving] = useState(false)
  const [presetMessage, setPresetMessage] = useState<string | null>(null)

  const [title, setTitle] = useState('')
  const [description, setDescription] = useState('')
  const [manualCategoryName, setManualCategoryName] = useState('')
  const [manualSaving, setManualSaving] = useState(false)
  const [manualMessage, setManualMessage] = useState<string | null>(null)

  const [publicDecks, setPublicDecks] = useState<Deck[]>([])
  const [deletingDeckId, setDeletingDeckId] = useState<number | null>(null)

  const [csvFile, setCsvFile] = useState<File | null>(null)
  const [csvSaving, setCsvSaving] = useState(false)
  const [csvMessage, setCsvMessage] = useState<string | null>(null)
  const [showCsvExample, setShowCsvExample] = useState(false)

  const loadData = useCallback(async () => {
    const { data } = await api.get<Category[]>('/api/admin/categories/presets')
    setCategories(data)
    setManualCategoryName((prev) => (prev ? prev : data.length > 0 ? data[0].name : ''))
  }, [])

  const loadPublicDecks = useCallback(async () => {
    const { data } = await api.get<PageResponse<Deck>>('/api/decks/public?size=100')
    setPublicDecks(data.content)
  }, [])

  useEffect(() => {
    void (async () => {
      try {
        const { data } = await api.get<UserProfile>('/api/users/me')
        if (data.role !== 'ADMIN') {
          setForbidden(true)
          return
        }
        await Promise.all([loadData(), loadPublicDecks()])
      } catch {
        setForbidden(true)
      } finally {
        setLoading(false)
      }
    })()
  }, [loadData, loadPublicDecks])

  const handleAddPreset = async (e: React.FormEvent) => {
    e.preventDefault()
    setPresetSaving(true)
    setPresetMessage(null)
    try {
      await api.post('/api/admin/categories/presets', { name: presetName.trim() })
      setPresetName('')
      setPresetMessage('Категория добавлена')
      await loadData()
    } catch {
      setPresetMessage('Не удалось добавить категорию')
    } finally {
      setPresetSaving(false)
    }
  }

  const handleCsvImport = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!csvFile) return

    const ok = window.confirm('Создать публичную колоду из CSV?')
    if (!ok) return

    setCsvSaving(true)
    setCsvMessage(null)
    try {
      const form = new FormData()
      form.append('file', csvFile)

      const { data } = await api.post<AdminBulkDeckResponse>('/api/admin/decks/import-csv-all-users', form, {
        headers: { 'Content-Type': 'multipart/form-data' },
      })
      setCsvMessage(`Готово: создано колод ${data.decksCreated}, карточек ${data.cardsCreated}`)
      setCsvFile(null)
      await loadPublicDecks()
    } catch {
      setCsvMessage('Ошибка импорта CSV')
    } finally {
      setCsvSaving(false)
    }
  }

  const handleDeletePublicDeck = async (deckId: number, deckTitle: string) => {
    if (!window.confirm(`Удалить публичную колоду «${deckTitle}»? Это действие нельзя отменить.`)) return
    setDeletingDeckId(deckId)
    try {
      await api.delete(`/api/admin/decks/${deckId}`)
      await loadPublicDecks()
    } catch {
      alert('Не удалось удалить колоду')
    } finally {
      setDeletingDeckId(null)
    }
  }

  const handleManualCreate = async (e: React.FormEvent) => {
    e.preventDefault()
    setManualSaving(true)
    setManualMessage(null)
    try {
      const { data } = await api.post<AdminBulkDeckResponse>('/api/admin/decks/create-for-all-users', {
        title: title.trim(),
        description: description.trim(),
        categoryName: manualCategoryName,
      })
      setManualMessage(`Готово: публичная колода создана (${data.cardsCreated} карточек)`)
      setTitle('')
      setDescription('')
    } catch {
      setManualMessage('Не удалось создать колоды')
    } finally {
      setManualSaving(false)
    }
  }

  if (loading) {
    return <div className="p-6 text-slate-500">Загрузка...</div>
  }

  if (forbidden) {
    return (
      <div className="mx-auto max-w-xl p-6">
        <p className="mb-3 text-red-600">Доступ только для администратора</p>
        <button className="rounded bg-slate-900 px-3 py-2 text-sm text-white" onClick={() => navigate('/decks')}>
          К колодам
        </button>
      </div>
    )
  }

  return (
    <div className="mx-auto max-w-2xl px-4 py-6">
      <button className="mb-3 text-sm text-slate-500 hover:text-slate-700" onClick={() => navigate('/decks')}>
        ← Мои колоды
      </button>
      <h1 className="mb-4 text-2xl font-semibold">Админ-панель</h1>

      <form onSubmit={(e) => void handleAddPreset(e)} className="mb-4 rounded-xl border border-slate-200 bg-white p-4 shadow-sm">
        <p className="mb-2 text-sm font-medium text-slate-700">Добавить категорию для всех пользователей</p>
        <div className="flex gap-2">
          <input
            className="flex-1 rounded border border-slate-300 px-3 py-2 text-sm"
            value={presetName}
            onChange={(e) => setPresetName(e.target.value)}
            placeholder="Например: Algorithms"
            required
          />
          <button disabled={presetSaving} className="rounded bg-slate-900 px-3 py-2 text-sm text-white disabled:opacity-50">
            {presetSaving ? 'Сохраняем...' : 'Добавить'}
          </button>
        </div>
        {presetMessage ? <p className="mt-2 text-xs text-slate-500">{presetMessage}</p> : null}
      </form>

      <form onSubmit={(e) => void handleCsvImport(e)} className="mb-4 rounded-xl border border-slate-200 bg-white p-4 shadow-sm">
        <p className="mb-3 text-sm font-medium text-slate-700">Добавление публичной колоды через CSV</p>
        <p className="mb-2 text-xs text-slate-500">Колода будет видна всем пользователям в разделе «Публичные колоды».</p>
        <div className="rounded border border-slate-200 p-3">
          <div className="mb-2 flex items-center gap-2">
            <span className="text-xs text-slate-500">Пример формата CSV</span>
            <button
              type="button"
              className="inline-flex h-5 w-5 items-center justify-center rounded-full border border-slate-300 text-xs font-semibold text-slate-600 hover:bg-slate-100"
              onClick={() => setShowCsvExample((prev) => !prev)}
              aria-label="Показать пример CSV"
              title="Показать пример CSV"
            >
              ?
            </button>
          </div>
          {showCsvExample ? (
            <pre className="mb-2 overflow-x-auto rounded bg-slate-50 p-2 text-xs text-slate-700">
title;Java Core{'\n'}
category;Java{'\n'}
public;true{'\n'}
description;Базовые карточки{'\n'}
{'\n'}
front;back;hint{'\n'}
Что такое JVM?;Java Virtual Machine;Запускает байткод
            </pre>
          ) : null}
          <label
            htmlFor="csv-file"
            className="inline-flex cursor-pointer items-center rounded bg-slate-900 px-3 py-2 text-sm font-medium text-white hover:bg-slate-800"
          >
            Выбрать CSV файл
          </label>
          <input
            id="csv-file"
            type="file"
            accept=".csv,text/csv"
            onChange={(e) => setCsvFile(e.target.files?.[0] ?? null)}
            required
            className="sr-only"
          />
          <p className="mt-2 text-sm text-slate-600">{csvFile ? `Выбран файл: ${csvFile.name}` : 'Файл пока не выбран'}</p>
        </div>
        <button disabled={csvSaving || !csvFile} className="mt-3 rounded bg-emerald-600 px-3 py-2 text-sm text-white disabled:opacity-50">
          {csvSaving ? 'Добавляем...' : 'Создать публичную колоду из CSV'}
        </button>
        {csvMessage ? <p className="mt-2 text-xs text-slate-500">{csvMessage}</p> : null}
      </form>

      <form onSubmit={(e) => void handleManualCreate(e)} className="rounded-xl border border-slate-200 bg-white p-4 shadow-sm">
        <p className="mb-3 text-sm font-medium text-slate-700">Ручное создание публичной колоды</p>
        <div className="grid gap-2">
          <input
            className="rounded border border-slate-300 px-3 py-2 text-sm"
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            placeholder="Название колоды"
            required
          />
          <textarea
            className="rounded border border-slate-300 px-3 py-2 text-sm"
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            placeholder="Описание (необязательно)"
            rows={2}
          />
          <select
            className="rounded border border-slate-300 px-3 py-2 text-sm"
            value={manualCategoryName}
            onChange={(e) => setManualCategoryName(e.target.value)}
            required
          >
            {categories.map((cat) => (
              <option key={cat.id} value={cat.name}>
                {cat.name}
              </option>
            ))}
          </select>
          <button disabled={manualSaving} className="rounded bg-emerald-600 px-3 py-2 text-sm text-white disabled:opacity-50">
            {manualSaving ? 'Создаем...' : 'Создать публичную колоду'}
          </button>
        </div>
        {manualMessage ? <p className="mt-2 text-xs text-slate-500">{manualMessage}</p> : null}
      </form>

      <div className="mt-6 rounded-xl border border-slate-200 bg-white p-4 shadow-sm">
        <p className="mb-3 text-sm font-medium text-slate-700">Публичные колоды ({publicDecks.length})</p>
        {publicDecks.length === 0 ? (
          <p className="text-xs text-slate-400">Публичных колод пока нет</p>
        ) : (
          <ul className="space-y-2">
            {publicDecks.map((deck) => (
              <li key={deck.id} className="flex items-center justify-between gap-3 rounded border border-slate-200 p-3">
                <div className="min-w-0">
                  <p className="text-sm font-medium truncate">{deck.title}</p>
                  {deck.categoryName ? (
                    <span className="text-xs text-slate-400">{deck.categoryName}</span>
                  ) : null}
                  <p className="text-xs text-slate-400">{deck.cardCount} карт.</p>
                </div>
                <button
                  disabled={deletingDeckId === deck.id}
                  onClick={() => void handleDeletePublicDeck(deck.id, deck.title)}
                  className="shrink-0 rounded border border-red-200 px-3 py-1.5 text-xs font-medium text-red-600 hover:bg-red-50 disabled:opacity-50"
                >
                  {deletingDeckId === deck.id ? 'Удаляем...' : 'Удалить'}
                </button>
              </li>
            ))}
          </ul>
        )}
      </div>
    </div>
  )
}
