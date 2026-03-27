import { useCallback, useEffect, useRef, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { api } from '../lib/api'
import { downloadPersonalDeckCsv } from '../lib/downloadDeckCsv'
import { toast } from '../store/toastStore'
import type { Card, Category, Deck, PageResponse } from '../types/api'

type CardForm = { front: string; back: string; hint: string }
const EMPTY_FORM: CardForm = { front: '', back: '', hint: '' }

export function DeckDetailPage() {
  const { id } = useParams<{ id: string }>()
  const deckId = Number(id)
  const navigate = useNavigate()

  const [deck, setDeck] = useState<Deck | null>(null)
  const [cards, setCards] = useState<Card[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const [categories, setCategories] = useState<Category[]>([])

  const [editingDeck, setEditingDeck] = useState(false)
  const [deckForm, setDeckForm] = useState({
    title: '',
    description: '',
    selectedCategory: '',
    newCategoryName: '',
  })
  const [deckSaving, setDeckSaving] = useState(false)

  const [addForm, setAddForm] = useState<CardForm>(EMPTY_FORM)
  const [adding, setAdding] = useState(false)

  const [editingId, setEditingId] = useState<number | null>(null)
  const [editForm, setEditForm] = useState<CardForm>(EMPTY_FORM)
  const [saving, setSaving] = useState(false)

  const [cardSearch, setCardSearch] = useState('')
  const [exportingCsv, setExportingCsv] = useState(false)
  const frontInputRef = useRef<HTMLInputElement>(null)

  const loadData = useCallback(async () => {
    try {
      const [deckRes, cardsRes, catsRes] = await Promise.all([
        api.get<Deck>(`/api/decks/${deckId}`),
        api.get<PageResponse<Card>>(`/api/decks/${deckId}/cards`),
        api.get<Category[]>('/api/categories'),
      ])
      setDeck(deckRes.data)
      setCards(cardsRes.data.content)
      setCategories(catsRes.data)
    } catch {
      setError('Не удалось загрузить данные колоды')
    } finally {
      setLoading(false)
    }
  }, [deckId])

  useEffect(() => {
    void loadData()
  }, [loadData])

  const handleExportCsv = async (): Promise<void> => {
    if (!deck?.title || deck.public) return
    setExportingCsv(true)
    try {
      await downloadPersonalDeckCsv(deckId, deck.title)
      toast.success('CSV сохранён — это ваша личная колода, не публичный каталог')
    } catch {
      toast.error('Не удалось выгрузить CSV')
    } finally {
      setExportingCsv(false)
    }
  }

  const startEditDeck = () => {
    if (!deck) return
    setDeckForm({
      title: deck.title,
      description: deck.description ?? '',
      selectedCategory: deck.categoryId != null ? String(deck.categoryId) : '',
      newCategoryName: '',
    })
    setEditingDeck(true)
  }

  const handleSaveDeck = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!deckForm.title.trim()) return
    setDeckSaving(true)
    try {
      let categoryId: number | null = null

      if (deckForm.selectedCategory === 'new' && deckForm.newCategoryName.trim()) {
        const { data } = await api.post<Category>('/api/categories', {
          name: deckForm.newCategoryName.trim(),
        })
        setCategories((prev) => [...prev, data].sort((a, b) => a.name.localeCompare(b.name)))
        categoryId = data.id
      } else if (deckForm.selectedCategory !== '') {
        categoryId = Number(deckForm.selectedCategory)
      }

      await api.put(`/api/decks/${deckId}`, {
        title: deckForm.title.trim(),
        description: deckForm.description.trim(),
        categoryId,
      })
      setEditingDeck(false)
      await loadData()
    } catch {
      setError('Не удалось сохранить колоду')
    } finally {
      setDeckSaving(false)
    }
  }

  const handleDeleteDeck = async () => {
    if (!confirm(`Удалить колоду "${deck?.title}" и все её карточки?`)) return
    try {
      await api.delete(`/api/decks/${deckId}`)
      navigate('/decks')
    } catch {
      setError('Не удалось удалить колоду')
    }
  }

  const handleAdd = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!addForm.front.trim() || !addForm.back.trim()) return
    setAdding(true)
    try {
      await api.post(`/api/decks/${deckId}/cards`, {
        front: addForm.front.trim(),
        back: addForm.back.trim(),
        hint: addForm.hint.trim() || null,
        position: cards.length,
      })
      setAddForm(EMPTY_FORM)
      frontInputRef.current?.focus()
      await loadData()
    } catch {
      setError('Не удалось добавить карточку')
    } finally {
      setAdding(false)
    }
  }

  const startEdit = (card: Card) => {
    setEditingId(card.id)
    setEditForm({ front: card.front, back: card.back, hint: card.hint ?? '' })
  }

  const cancelEdit = () => {
    setEditingId(null)
    setEditForm(EMPTY_FORM)
  }

  const handleSave = async (cardId: number) => {
    if (!editForm.front.trim() || !editForm.back.trim()) return
    setSaving(true)
    try {
      await api.put(`/api/decks/${deckId}/cards/${cardId}`, {
        front: editForm.front.trim(),
        back: editForm.back.trim(),
        hint: editForm.hint.trim() || null,
        position: cards.findIndex((c) => c.id === cardId),
      })
      setEditingId(null)
      await loadData()
    } catch {
      setError('Не удалось сохранить карточку')
    } finally {
      setSaving(false)
    }
  }

  const handleDelete = async (cardId: number) => {
    if (!confirm('Удалить карточку?')) return
    try {
      await api.delete(`/api/decks/${deckId}/cards/${cardId}`)
      setCards((prev) => prev.filter((c) => c.id !== cardId))
    } catch {
      setError('Не удалось удалить карточку')
    }
  }

  if (loading) {
    return (
      <div className="flex min-h-screen items-center justify-center">
        <p className="text-slate-500">Загрузка...</p>
      </div>
    )
  }

  if (error && !deck) {
    return (
      <div className="flex min-h-screen flex-col items-center justify-center gap-4">
        <p className="text-red-600">{error}</p>
        <button className="rounded bg-slate-900 px-4 py-2 text-sm text-white" onClick={() => navigate('/decks')}>
          Назад к колодам
        </button>
      </div>
    )
  }

  return (
    <div className="mx-auto max-w-2xl px-4 py-6">
      <div className="mb-6">
        <button
          className="mb-1 text-sm text-slate-500 hover:text-slate-700"
          onClick={() => navigate('/decks')}
        >
          ← Мои колоды
        </button>

        {editingDeck ? (
          <form onSubmit={(e) => void handleSaveDeck(e)} className="mt-2 rounded-xl border border-slate-200 bg-white p-4 shadow-sm">
            <p className="mb-3 text-sm font-medium text-slate-700">Редактировать колоду</p>
            <div className="space-y-2">
              <input
                className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm focus:border-slate-500 focus:outline-none"
                placeholder="Название"
                value={deckForm.title}
                onChange={(e) => setDeckForm((f) => ({ ...f, title: e.target.value }))}
                required
                autoFocus
              />
              <input
                className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm focus:border-slate-500 focus:outline-none"
                placeholder="Описание"
                value={deckForm.description}
                onChange={(e) => setDeckForm((f) => ({ ...f, description: e.target.value }))}
              />
              <div className="flex flex-wrap items-center gap-2">
                <select
                  className="rounded-lg border border-slate-300 px-3 py-2 text-sm"
                  value={deckForm.selectedCategory}
                  onChange={(e) => setDeckForm((f) => ({ ...f, selectedCategory: e.target.value }))}
                >
                  <option value="">— Без категории —</option>
                  {categories.map((cat) => (
                    <option key={cat.id} value={String(cat.id)}>
                      {cat.name}
                    </option>
                  ))}
                  <option value="new">+ Новая категория...</option>
                </select>
                {deckForm.selectedCategory === 'new' ? (
                  <input
                    className="rounded-lg border border-slate-300 px-3 py-2 text-sm"
                    placeholder="Название новой категории"
                    value={deckForm.newCategoryName}
                    onChange={(e) => setDeckForm((f) => ({ ...f, newCategoryName: e.target.value }))}
                    required
                    autoFocus
                  />
                ) : null}
              </div>
            </div>
            <div className="mt-3 flex gap-2">
              <button
                type="submit"
                disabled={deckSaving}
                className="rounded-lg bg-slate-900 px-4 py-1.5 text-sm font-medium text-white disabled:opacity-50"
              >
                {deckSaving ? 'Сохраняем...' : 'Сохранить'}
              </button>
              <button
                type="button"
                className="rounded-lg border px-4 py-1.5 text-sm text-slate-600"
                onClick={() => setEditingDeck(false)}
              >
                Отмена
              </button>
            </div>
          </form>
        ) : (
          <div className="flex flex-col gap-2 sm:flex-row sm:items-start sm:justify-between">
            <div className="min-w-0">
              <h1 className="text-2xl font-semibold">{deck?.title}</h1>
              {deck?.categoryName ? (
                <span className="mt-1 inline-block rounded-full bg-slate-100 px-2.5 py-0.5 text-xs font-medium text-slate-600">
                  {deck.categoryName}
                </span>
              ) : null}
              {deck?.description ? <p className="mt-1 text-sm text-slate-500">{deck.description}</p> : null}
              {deck?.public ? (
                <span className="mt-1 inline-block text-xs text-slate-400">Публичная колода каталога — экспорт CSV недоступен</span>
              ) : (
                <p className="mt-1 text-xs text-slate-500">Личная колода: можно скачать карточки в CSV для резервной копии.</p>
              )}
            </div>
            <div className="flex shrink-0 flex-wrap items-center gap-1">
              {deck ? (
                <button
                  type="button"
                  title={
                    deck.dueCardCount === 0
                      ? 'Сейчас нет карточек к повторению по этой колоде'
                      : undefined
                  }
                  className={`flex items-center gap-1.5 rounded-lg px-3 py-1.5 text-xs font-medium ${
                    deck.dueCardCount > 0
                      ? 'bg-slate-900 text-white'
                      : 'border border-slate-300 bg-white text-slate-600 hover:bg-slate-50'
                  }`}
                  onClick={() => navigate(`/study?deckId=${deckId}`)}
                >
                  Учить
                  {deck.dueCardCount > 0 ? (
                    <span className="rounded-full bg-white px-1.5 py-0.5 text-xs font-bold text-slate-900">
                      {deck.dueCardCount}
                    </span>
                  ) : null}
                </button>
              ) : null}
              {deck && !deck.public ? (
                <button
                  type="button"
                  title="Скачать личную колоду в CSV (UTF-8), не публичный каталог"
                  className="rounded-lg border border-slate-300 px-2.5 py-1.5 text-xs font-medium text-slate-700 hover:bg-slate-50 disabled:opacity-50"
                  disabled={exportingCsv}
                  onClick={() => void handleExportCsv()}
                >
                  {exportingCsv ? '…' : 'Скачать CSV'}
                </button>
              ) : null}
              <span className="rounded-full bg-slate-100 px-2.5 py-0.5 text-xs font-medium text-slate-600">
                {cards.length} карт.
              </span>
              <button
                className="rounded px-2 py-1 text-xs text-slate-500 hover:bg-slate-100"
                onClick={startEditDeck}
              >
                Ред.
              </button>
              <button
                className="rounded px-2 py-1 text-xs text-red-500 hover:bg-red-50"
                onClick={() => void handleDeleteDeck()}
              >
                Удалить
              </button>
            </div>
          </div>
        )}
      </div>

      {error ? (
        <p className="mb-4 rounded bg-red-50 px-3 py-2 text-sm text-red-600">{error}</p>
      ) : null}

      <form
        onSubmit={(e) => void handleAdd(e)}
        className="mb-6 rounded-xl border border-slate-200 bg-white p-4 shadow-sm"
      >
        <p className="mb-3 text-sm font-medium text-slate-700">Добавить карточку</p>
        <div className="grid gap-2 sm:grid-cols-2">
          <input
            ref={frontInputRef}
            className="rounded-lg border border-slate-300 px-3 py-2 text-sm focus:border-slate-500 focus:outline-none"
            placeholder="Вопрос (лицевая)"
            value={addForm.front}
            onChange={(e) => setAddForm((f) => ({ ...f, front: e.target.value }))}
            required
          />
          <input
            className="rounded-lg border border-slate-300 px-3 py-2 text-sm focus:border-slate-500 focus:outline-none"
            placeholder="Ответ (обратная)"
            value={addForm.back}
            onChange={(e) => setAddForm((f) => ({ ...f, back: e.target.value }))}
            required
          />
          <input
            className="rounded-lg border border-slate-300 px-3 py-2 text-sm focus:border-slate-500 focus:outline-none sm:col-span-2"
            placeholder="Подсказка (необязательно)"
            value={addForm.hint}
            onChange={(e) => setAddForm((f) => ({ ...f, hint: e.target.value }))}
          />
        </div>
        <button
          type="submit"
          disabled={adding}
          className="mt-3 w-full rounded-lg bg-slate-900 py-2 text-sm font-medium text-white disabled:opacity-50"
        >
          {adding ? 'Добавляем...' : '+ Добавить'}
        </button>
      </form>

      {cards.length > 0 ? (
        <div className="mb-3 relative">
          <span className="absolute inset-y-0 left-3 flex items-center text-slate-400 pointer-events-none text-sm">
            🔍
          </span>
          <input
            className="w-full rounded-lg border border-slate-200 py-2 pl-9 pr-4 text-sm focus:border-slate-400 focus:outline-none"
            placeholder="Фильтр по карточкам..."
            value={cardSearch}
            onChange={(e) => setCardSearch(e.target.value)}
          />
          {cardSearch ? (
            <button
              onClick={() => setCardSearch('')}
              className="absolute inset-y-0 right-3 flex items-center text-slate-400 hover:text-slate-600 text-lg"
            >
              ×
            </button>
          ) : null}
        </div>
      ) : null}

      {(() => {
        const q = cardSearch.trim().toLowerCase()
        const filtered = q
          ? cards.filter(
              (c) =>
                c.front.toLowerCase().includes(q) ||
                c.back.toLowerCase().includes(q) ||
                (c.hint ?? '').toLowerCase().includes(q),
            )
          : cards
        return (
      <ul className="space-y-2">
        {filtered.map((card, idx) => (
          <li key={card.id} className="rounded-xl border border-slate-200 bg-white shadow-sm">
            {editingId === card.id ? (
              /* Edit mode */
              <div className="p-4">
                <div className="grid gap-2 sm:grid-cols-2">
                  <input
                    className="rounded-lg border border-slate-300 px-3 py-2 text-sm focus:border-slate-500 focus:outline-none"
                    value={editForm.front}
                    onChange={(e) => setEditForm((f) => ({ ...f, front: e.target.value }))}
                    placeholder="Вопрос"
                    autoFocus
                  />
                  <input
                    className="rounded-lg border border-slate-300 px-3 py-2 text-sm focus:border-slate-500 focus:outline-none"
                    value={editForm.back}
                    onChange={(e) => setEditForm((f) => ({ ...f, back: e.target.value }))}
                    placeholder="Ответ"
                  />
                  <input
                    className="rounded-lg border border-slate-300 px-3 py-2 text-sm focus:border-slate-500 focus:outline-none sm:col-span-2"
                    value={editForm.hint}
                    onChange={(e) => setEditForm((f) => ({ ...f, hint: e.target.value }))}
                    placeholder="Подсказка"
                  />
                </div>
                <div className="mt-3 flex gap-2">
                  <button
                    disabled={saving}
                    className="rounded-lg bg-slate-900 px-4 py-1.5 text-sm font-medium text-white disabled:opacity-50"
                    onClick={() => void handleSave(card.id)}
                  >
                    {saving ? 'Сохраняем...' : 'Сохранить'}
                  </button>
                  <button
                    className="rounded-lg border px-4 py-1.5 text-sm text-slate-600"
                    onClick={cancelEdit}
                  >
                    Отмена
                  </button>
                </div>
              </div>
            ) : (
              <div className="flex items-start gap-3 p-4">
                <span className="mt-0.5 shrink-0 text-xs text-slate-400 tabular-nums w-5 text-right">
                  {idx + 1}
                </span>
                <div className="min-w-0 flex-1">
                  <p className="text-sm font-medium text-slate-800">{card.front}</p>
                  <p className="mt-0.5 text-sm text-slate-500">{card.back}</p>
                  {card.hint ? (
                    <p className="mt-0.5 text-xs text-slate-400 italic">💡 {card.hint}</p>
                  ) : null}
                </div>
                <div className="flex shrink-0 gap-1">
                  <button
                    className="rounded px-2 py-1 text-xs text-slate-500 hover:bg-slate-100"
                    onClick={() => startEdit(card)}
                  >
                    Ред.
                  </button>
                  <button
                    className="rounded px-2 py-1 text-xs text-red-500 hover:bg-red-50"
                    onClick={() => void handleDelete(card.id)}
                  >
                    Удал.
                  </button>
                </div>
              </div>
            )}
          </li>
        ))}
        {cards.length === 0 ? (
          <li className="rounded-xl border border-dashed border-slate-200 py-10 text-center text-sm text-slate-400">
            Нет карточек. Добавьте первую выше.
          </li>
        ) : filtered.length === 0 ? (
          <li className="rounded-xl border border-dashed border-slate-200 py-6 text-center text-sm text-slate-400">
            Ничего не найдено по запросу «{cardSearch}»
          </li>
        ) : null}
      </ul>
        )
      })()}
    </div>
  )
}
