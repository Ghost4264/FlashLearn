import { useEffect, useState, useCallback, useRef } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { api } from '../lib/api'
import { useAuthStore } from '../store/authStore'
import type { Category, Deck, PageResponse } from '../types/api'

const PAGE_SIZE = 8

function Pagination({
  page,
  totalPages,
  onPage,
}: {
  page: number
  totalPages: number
  onPage: (p: number) => void
}) {
  if (totalPages <= 1) return null
  return (
    <div className="mt-3 flex items-center justify-center gap-1">
      <button
        disabled={page === 0}
        onClick={() => onPage(page - 1)}
        className="rounded border px-2.5 py-1 text-xs disabled:opacity-30 hover:bg-slate-50"
      >
        ←
      </button>
      {Array.from({ length: totalPages }, (_, i) => (
        <button
          key={i}
          onClick={() => onPage(i)}
          className={`rounded border px-2.5 py-1 text-xs ${
            i === page ? 'bg-slate-900 text-white border-slate-900' : 'hover:bg-slate-50'
          }`}
        >
          {i + 1}
        </button>
      ))}
      <button
        disabled={page === totalPages - 1}
        onClick={() => onPage(page + 1)}
        className="rounded border px-2.5 py-1 text-xs disabled:opacity-30 hover:bg-slate-50"
      >
        →
      </button>
    </div>
  )
}

export function DecksPage() {
  const clearTokens = useAuthStore((state) => state.clearTokens)
  const navigate = useNavigate()

  const [categories, setCategories] = useState<Category[]>([])
  const [filterCategoryId, setFilterCategoryId] = useState<number | null>(null)
  const [categoryFilterQuery, setCategoryFilterQuery] = useState('')

  const [searchInput, setSearchInput] = useState('')
  const [searchQ, setSearchQ] = useState('')          // debounced value sent to API
  const debounceRef = useRef<ReturnType<typeof setTimeout> | null>(null)

  const [myDecks, setMyDecks] = useState<Deck[]>([])
  const [myPage, setMyPage] = useState(0)
  const [myTotalPages, setMyTotalPages] = useState(0)
  const [myTotal, setMyTotal] = useState(0)

  const [publicDecks, setPublicDecks] = useState<Deck[]>([])
  const [pubPage, setPubPage] = useState(0)
  const [pubTotalPages, setPubTotalPages] = useState(0)

  const [dueCount, setDueCount] = useState<number>(0)

  const [title, setTitle] = useState('')
  const [description, setDescription] = useState('')
  const [isPublic, setIsPublic] = useState(false)

  const [selectedCategory, setSelectedCategory] = useState<string>('')

  const [error, setError] = useState<string | null>(null)

  const loadCategories = useCallback(async () => {
    const { data } = await api.get<Category[]>('/api/categories')
    setCategories(data)
    setSelectedCategory((prev) => (data.length > 0 && !prev ? String(data[0].id) : prev))
  }, [])

  const loadMyDecks = useCallback(async (page: number, categoryId: number | null, q: string) => {
    const catParam = categoryId != null ? `&categoryId=${categoryId}` : ''
    const qParam = q.trim() ? `&q=${encodeURIComponent(q.trim())}` : ''
    const { data } = await api.get<PageResponse<Deck>>(
      `/api/decks?page=${page}&size=${PAGE_SIZE}${catParam}${qParam}`,
    )
    setMyDecks(data.content)
    setMyPage(data.page)
    setMyTotalPages(data.totalPages)
    setMyTotal(data.totalElements)
  }, [])

  const loadPublicDecks = useCallback(async (page: number, q: string) => {
    const qParam = q.trim() ? `&q=${encodeURIComponent(q.trim())}` : ''
    const { data } = await api.get<PageResponse<Deck>>(
      `/api/decks/public?page=${page}&size=${PAGE_SIZE}${qParam}`,
    )
    setPublicDecks(data.content)
    setPubPage(data.page)
    setPubTotalPages(data.totalPages)
  }, [])

  const loadDueCount = useCallback(async () => {
    const { data } = await api.get<{ count: number }>('/api/review/due/count')
    setDueCount(data.count)
  }, [])

  useEffect(() => {
    let active = true
    void (async () => {
      try {
        await Promise.all([
          loadCategories(),
          loadMyDecks(0, null, ''),
          loadPublicDecks(0, ''),
          loadDueCount(),
        ])
      } catch {
        if (active) setError('Не удалось загрузить данные')
      }
    })()
    return () => {
      active = false
    }
  }, [loadCategories, loadMyDecks, loadPublicDecks, loadDueCount])

  useEffect(() => {
    if (debounceRef.current) clearTimeout(debounceRef.current)
    debounceRef.current = setTimeout(() => {
      setSearchQ(searchInput)
      void loadMyDecks(0, filterCategoryId, searchInput)
      void loadPublicDecks(0, searchInput)
    }, 350)
    return () => {
      if (debounceRef.current) clearTimeout(debounceRef.current)
    }
  }, [searchInput])

  const applyFilter = (categoryId: number | null) => {
    setFilterCategoryId(categoryId)
    void loadMyDecks(0, categoryId, searchQ)
  }

  const createDeck = async (): Promise<void> => {
    try {
      const categoryId = selectedCategory !== '' ? Number(selectedCategory) : null

      if (categoryId == null) {
        setError('Выберите категорию')
        return
      }

      await api.post('/api/decks', {
        title,
        description,
        public: isPublic,
        categoryId,
      })
      setTitle('')
      setDescription('')
      setIsPublic(false)
      if (categories.length > 0) {
        setSelectedCategory(String(categories[0].id))
      }
      await Promise.all([loadMyDecks(0, filterCategoryId, searchQ), loadDueCount()])
    } catch {
      setError('Не удалось создать колоду')
    }
  }

  const cloneDeck = async (deckId: number): Promise<void> => {
    try {
      await api.post(`/api/decks/${deckId}/clone`)
      await Promise.all([loadMyDecks(myPage, filterCategoryId, searchQ), loadDueCount()])
    } catch {
      setError('Не удалось клонировать колоду')
    }
  }

  const totalCards = myDecks.reduce((sum, d) => sum + d.cardCount, 0)
  const visibleFilterCategories = categories.filter((cat) =>
    cat.name.toLowerCase().includes(categoryFilterQuery.trim().toLowerCase()),
  )

  return (
    <div className="mx-auto max-w-5xl px-4 py-6">
      <div className="mb-6 flex items-center justify-between">
        <h1 className="text-2xl font-semibold">Мои колоды</h1>
        <div className="flex items-center gap-2">
          <button
            className="flex items-center gap-1.5 rounded-lg bg-slate-900 px-4 py-2 text-sm font-medium text-white disabled:opacity-40"
            disabled={dueCount === 0}
            onClick={() => navigate('/study')}
          >
            Учить
            {dueCount > 0 ? (
              <span className="rounded-full bg-white px-1.5 py-0.5 text-xs font-bold text-slate-900">
                {dueCount}
              </span>
            ) : null}
          </button>
          <Link to="/profile" className="rounded border px-3 py-1.5 text-sm hover:bg-slate-50">
            Профиль
          </Link>
          <button className="rounded border px-3 py-1.5 text-sm" onClick={() => clearTokens()}>
            Выйти
          </button>
        </div>
      </div>

      <div className="mb-4 relative">
        <span className="absolute inset-y-0 left-3 flex items-center text-slate-400 pointer-events-none">
          🔍
        </span>
        <input
          className="w-full rounded-lg border border-slate-300 py-2 pl-9 pr-4 text-sm focus:border-slate-500 focus:outline-none"
          placeholder="Поиск по названию или описанию..."
          value={searchInput}
          onChange={(e) => setSearchInput(e.target.value)}
        />
        {searchInput ? (
          <button
            onClick={() => setSearchInput('')}
            className="absolute inset-y-0 right-3 flex items-center text-slate-400 hover:text-slate-600 text-lg"
          >
            ×
          </button>
        ) : null}
      </div>

      {myTotal > 0 ? (
        <div className="mb-6 grid grid-cols-3 gap-3">
          <div className="rounded-xl bg-white p-3 text-center shadow-sm">
            <p className="text-2xl font-bold">{myTotal}</p>
            <p className="text-xs text-slate-500">колод</p>
          </div>
          <div className="rounded-xl bg-white p-3 text-center shadow-sm">
            <p className="text-2xl font-bold">{totalCards}</p>
            <p className="text-xs text-slate-500">карточек на стр.</p>
          </div>
          <div className="rounded-xl bg-white p-3 text-center shadow-sm">
            <p className={`text-2xl font-bold ${dueCount > 0 ? 'text-slate-900' : 'text-slate-300'}`}>
              {dueCount}
            </p>
            <p className="text-xs text-slate-500">к повторению</p>
          </div>
        </div>
      ) : null}

      {error ? <p className="mb-4 text-red-600">{error}</p> : null}

      {categories.length > 0 ? (
        <div className="mb-4 rounded-lg border border-slate-200 bg-white p-3">
          <p className="mb-2 text-xs font-medium text-slate-500">Фильтр по категории</p>
          <div className="grid gap-2 md:grid-cols-2">
            <input
              className="rounded border border-slate-300 px-3 py-2 text-sm"
              placeholder="Поиск категории..."
              value={categoryFilterQuery}
              onChange={(e) => setCategoryFilterQuery(e.target.value)}
            />
            <select
              className="rounded border border-slate-300 px-3 py-2 text-sm"
              value={filterCategoryId == null ? '' : String(filterCategoryId)}
              onChange={(e) => {
                const value = e.target.value
                applyFilter(value === '' ? null : Number(value))
              }}
            >
              <option value="">Все категории</option>
              {visibleFilterCategories.map((cat) => (
                <option key={cat.id} value={String(cat.id)}>
                  {cat.name}
                </option>
              ))}
            </select>
          </div>
          {categoryFilterQuery.trim() && visibleFilterCategories.length === 0 ? (
            <p className="mt-2 text-xs text-slate-500">Категории не найдены</p>
          ) : null}
        </div>
      ) : null}

      <form
        onSubmit={(event) => {
          event.preventDefault()
          void createDeck()
        }}
        className="mb-6 rounded-lg bg-white p-4 shadow-sm"
      >
        <div className="grid gap-2 md:grid-cols-4">
          <input
            className="rounded border border-slate-300 px-3 py-2 md:col-span-2"
            placeholder="Название колоды"
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            required
          />
          <input
            className="rounded border border-slate-300 px-3 py-2"
            placeholder="Описание"
            value={description}
            onChange={(e) => setDescription(e.target.value)}
          />
          <label className="flex items-center gap-2 text-sm">
            <input type="checkbox" checked={isPublic} onChange={(e) => setIsPublic(e.target.checked)} />
            <span>Публичная</span>
          </label>
        </div>

        <div className="mt-2 flex flex-wrap items-center gap-2">
          <select
            className="rounded border border-slate-300 px-3 py-2 text-sm"
            value={selectedCategory}
            onChange={(e) => setSelectedCategory(e.target.value)}
            required
          >
            <option value="" disabled>
              — Выберите категорию —
            </option>
            {categories.map((cat) => (
              <option key={cat.id} value={String(cat.id)}>
                {cat.name}
              </option>
            ))}
          </select>
        </div>

        <button className="mt-3 w-full rounded bg-slate-900 px-3 py-2 text-white">Создать колоду</button>
      </form>

      <div className="grid gap-6 md:grid-cols-2">
        <section className="rounded-lg bg-white p-4 shadow-sm">
          <h2 className="mb-3 text-lg font-medium">
            Ваши колоды ({myTotal})
            {filterCategoryId != null ? (
              <span className="ml-2 text-sm font-normal text-slate-500">
                · {categories.find((c) => c.id === filterCategoryId)?.name}
              </span>
            ) : null}
          </h2>
          <ul className="space-y-2">
            {myDecks.map((deck) => (
              <li key={deck.id}>
                <Link
                  to={`/decks/${deck.id}`}
                  className="flex items-center justify-between rounded border border-slate-200 p-3 hover:border-slate-400 hover:bg-slate-50 transition-colors"
                >
                  <div className="min-w-0">
                    <p className="font-medium truncate">{deck.title}</p>
                    {deck.categoryName ? (
                      <span className="inline-block rounded-full bg-slate-100 px-2 py-0.5 text-xs text-slate-500 mt-0.5">
                        {deck.categoryName}
                      </span>
                    ) : null}
                    <p className="text-sm text-slate-600 truncate">{deck.description || 'Без описания'}</p>
                  </div>
                  <div className="ml-3 flex shrink-0 flex-col items-end gap-1 text-xs text-slate-500">
                    <span>{deck.cardCount} карт.</span>
                    {deck.dueCardCount > 0 ? (
                      <span className="rounded-full bg-slate-900 px-1.5 py-0.5 font-medium text-white">
                        {deck.dueCardCount} к повт.
                      </span>
                    ) : null}
                  </div>
                </Link>
              </li>
            ))}
            {myDecks.length === 0 ? (
              <li className="text-sm text-slate-500">
                {filterCategoryId != null ? 'Нет колод в этой категории' : 'Пока нет колод'}
              </li>
            ) : null}
          </ul>
          <Pagination
            page={myPage}
            totalPages={myTotalPages}
            onPage={(p) => void loadMyDecks(p, filterCategoryId, searchQ)}
          />
        </section>

        <section className="rounded-lg bg-white p-4 shadow-sm">
          <h2 className="mb-3 text-lg font-medium">Публичные колоды</h2>
          <ul className="space-y-2">
            {publicDecks.map((deck) => (
              <li key={deck.id} className="flex items-start justify-between gap-3 rounded border border-slate-200 p-3">
                <div className="min-w-0">
                  <p className="font-medium truncate">{deck.title}</p>
                  <p className="text-sm text-slate-600 truncate">{deck.description || 'Без описания'}</p>
                  <p className="mt-0.5 text-xs text-slate-400">{deck.cardCount} карт.</p>
                </div>
                <button
                  className="shrink-0 rounded-lg border border-slate-300 px-3 py-1.5 text-xs font-medium text-slate-700 hover:bg-slate-50 transition-colors"
                  onClick={() => void cloneDeck(deck.id)}
                >
                  Клонировать
                </button>
              </li>
            ))}
            {publicDecks.length === 0 ? (
              <li className="text-sm text-slate-500">Пока нет публичных колод</li>
            ) : null}
          </ul>
          <Pagination
            page={pubPage}
            totalPages={pubTotalPages}
            onPage={(p) => void loadPublicDecks(p, searchQ)}
          />
        </section>
      </div>
    </div>
  )
}
