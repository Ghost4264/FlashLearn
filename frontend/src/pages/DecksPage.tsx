import { useEffect, useState, useCallback, useRef } from 'react'
import { useNavigate } from 'react-router-dom'
import { WelcomeModal } from '../components/WelcomeModal'
import { LoggedInNav } from '../components/LoggedInNav'
import { api } from '../lib/api'
import { toast } from '../store/toastStore'
import type { Category, Deck, PageResponse, ReviewStats } from '../types/api'

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
    <div className="mt-3 flex flex-wrap items-center justify-center gap-1">
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

function ListSkeleton({ rows = 4 }: { rows?: number }) {
  return (
    <ul className="space-y-2">
      {Array.from({ length: rows }, (_, i) => (
        <li key={i} className="animate-pulse rounded border border-slate-100 bg-slate-50 p-3">
          <div className="h-4 w-2/3 rounded bg-slate-200" />
          <div className="mt-2 h-3 w-24 rounded bg-slate-200" />
          <div className="mt-2 h-3 w-full rounded bg-slate-100" />
        </li>
      ))}
    </ul>
  )
}

export function DecksPage() {
  const navigate = useNavigate()

  const [categories, setCategories] = useState<Category[]>([])
  const [filterCategoryId, setFilterCategoryId] = useState<number | null>(null)

  const [searchInput, setSearchInput] = useState('')
  const [searchQ, setSearchQ] = useState('')
  const debounceRef = useRef<ReturnType<typeof setTimeout> | null>(null)

  const [myDecks, setMyDecks] = useState<Deck[]>([])
  const [myPage, setMyPage] = useState(0)
  const [myTotalPages, setMyTotalPages] = useState(0)
  const [myTotal, setMyTotal] = useState(0)

  const [dueCount, setDueCount] = useState<number>(0)
  const [reviewStats, setReviewStats] = useState<ReviewStats | null>(null)

  const [deletingDeckId, setDeletingDeckId] = useState<number | null>(null)
  const [initialLoading, setInitialLoading] = useState(true)

  const loadCategories = useCallback(async () => {
    const { data } = await api.get<Category[]>('/api/categories')
    setCategories(data)
  }, [])

  const loadMyDecks = useCallback(async (page: number, categoryId: number | null, q: string) => {
    const catParam = categoryId != null ? `&categoryId=${categoryId}` : ''
    const qParam = q.trim() ? `&q=${encodeURIComponent(q.trim())}` : ''
    const { data } = await api.get<PageResponse<Deck>>(
      `/api/decks?page=${page}&size=${PAGE_SIZE}${catParam}${qParam}`,
    )
    setMyDecks(data.content ?? [])
    setMyPage(data.page)
    setMyTotalPages(data.totalPages)
    setMyTotal(data.totalElements)
  }, [])

  const loadStudyMetrics = useCallback(async () => {
    const [dueRes, statsRes] = await Promise.all([
      api.get<{ count: number }>('/api/review/due/count'),
      api.get<ReviewStats>('/api/review/stats'),
    ])
    setDueCount(dueRes.data.count)
    setReviewStats(statsRes.data)
  }, [])

  useEffect(() => {
    let active = true
    void (async () => {
      try {
        await Promise.all([loadCategories(), loadMyDecks(0, null, ''), loadStudyMetrics()])
      } catch {
        if (active) toast.error('Не удалось загрузить данные. Проверьте соединение.')
      } finally {
        if (active) setInitialLoading(false)
      }
    })()
    return () => {
      active = false
    }
  }, [loadCategories, loadMyDecks, loadStudyMetrics])

  useEffect(() => {
    if (debounceRef.current) clearTimeout(debounceRef.current)
    debounceRef.current = setTimeout(() => {
      setSearchQ(searchInput)
      void loadMyDecks(0, filterCategoryId, searchInput)
    }, 350)
    return () => {
      if (debounceRef.current) clearTimeout(debounceRef.current)
    }
  }, [searchInput, filterCategoryId, loadMyDecks])

  const applyFilter = (categoryId: number | null) => {
    setFilterCategoryId(categoryId)
    void loadMyDecks(0, categoryId, searchQ)
  }

  const deleteDeck = async (deckId: number): Promise<void> => {
    const accepted = window.confirm('Удалить колоду? Это действие необратимо.')
    if (!accepted) return

    try {
      setDeletingDeckId(deckId)
      await api.delete(`/api/decks/${deckId}`)
      await Promise.all([loadMyDecks(myPage, filterCategoryId, searchQ), loadStudyMetrics()])
      toast.success('Колода удалена')
    } catch {
      toast.error('Не удалось удалить колоду')
    } finally {
      setDeletingDeckId(null)
    }
  }

  const totalCards = myDecks.reduce((sum, d) => sum + d.cardCount, 0)

  const trimmedSearchInput = searchInput.trim()
  const hasMyFilters = Boolean(trimmedSearchInput) || filterCategoryId != null

  const resetSearch = (): void => {
    setSearchInput('')
  }

  return (
    <div className="mx-auto max-w-3xl px-4 py-6">
      <div className="mb-6 flex flex-wrap items-center justify-between gap-3">
        <h1 className="text-xl font-semibold sm:text-2xl">Мои колоды</h1>
        <LoggedInNav dueCount={dueCount} />
      </div>

      <WelcomeModal />

      <div className="sticky top-2 z-20 mb-4 rounded-xl border border-slate-200 bg-white/95 p-3 shadow-sm backdrop-blur">
        <div className="mb-2 text-xs font-medium uppercase tracking-wide text-slate-500">Поиск и категория</div>
        <div className="relative">
          <span className="pointer-events-none absolute inset-y-0 left-3 flex items-center text-slate-400">🔍</span>
          <input
            className="w-full rounded-lg border border-slate-300 py-2 pl-9 pr-4 text-sm focus:border-slate-500 focus:outline-none"
            placeholder="Поиск по названию или описанию..."
            value={searchInput}
            onChange={(e) => setSearchInput(e.target.value)}
          />
          {searchInput ? (
            <button
              type="button"
              onClick={() => setSearchInput('')}
              className="absolute inset-y-0 right-3 flex items-center text-lg text-slate-400 hover:text-slate-600"
            >
              ×
            </button>
          ) : null}
        </div>
      </div>

      {!initialLoading && reviewStats ? (
        <div className="mb-4">
          <div className="mb-2 text-xs font-medium uppercase tracking-wide text-slate-500">Статистика учёбы</div>
          <div className="grid grid-cols-3 gap-2 sm:gap-3">
            <div className="rounded-xl bg-white p-2 text-center shadow-sm sm:p-3">
              <p className="text-xl font-bold text-emerald-700 sm:text-2xl">{reviewStats.reviewedToday}</p>
              <p className="text-xs text-slate-500">оценок сегодня</p>
            </div>
            <div className="rounded-xl bg-white p-2 text-center shadow-sm sm:p-3">
              <p className="text-xl font-bold text-sky-800 sm:text-2xl">{reviewStats.reviewedThisWeek}</p>
              <p className="text-xs text-slate-500">за неделю</p>
            </div>
            <div className="rounded-xl bg-white p-2 text-center shadow-sm sm:p-3">
              <p className="text-xl font-bold text-amber-800 sm:text-2xl">{reviewStats.streakDays}</p>
              <p className="text-xs text-slate-500">дней подряд</p>
            </div>
          </div>
        </div>
      ) : null}

      {!initialLoading && myTotal > 0 ? (
        <div className="mb-6 grid grid-cols-3 gap-2 sm:gap-3">
          <div className="rounded-xl bg-white p-2 text-center shadow-sm sm:p-3">
            <p className="text-xl font-bold sm:text-2xl">{myTotal}</p>
            <p className="text-xs text-slate-500">колод</p>
          </div>
          <div className="rounded-xl bg-white p-2 text-center shadow-sm sm:p-3">
            <p className="text-xl font-bold sm:text-2xl">{totalCards}</p>
            <p className="text-xs text-slate-500">карточек на стр.</p>
          </div>
          <div className="rounded-xl bg-white p-2 text-center shadow-sm sm:p-3">
            <p className={`text-xl font-bold sm:text-2xl ${dueCount > 0 ? 'text-slate-900' : 'text-slate-300'}`}>
              {dueCount}
            </p>
            <p className="text-xs text-slate-500">к повторению</p>
          </div>
        </div>
      ) : null}

      <section className="overflow-hidden rounded-lg bg-white p-4 shadow-sm">
        <div className="mb-3 flex flex-wrap items-center justify-between gap-2">
          <h2 className="text-base font-medium sm:text-lg">Ваши колоды ({myTotal})</h2>
          {categories.length > 0 ? (
            <select
              className="max-w-[180px] rounded border border-slate-300 px-2 py-1 text-sm"
              value={filterCategoryId == null ? '' : String(filterCategoryId)}
              onChange={(e) => {
                const value = e.target.value
                applyFilter(value === '' ? null : Number(value))
              }}
            >
              <option value="">Все категории</option>
              {categories.map((cat) => (
                <option key={cat.id} value={String(cat.id)}>
                  {cat.name}
                </option>
              ))}
            </select>
          ) : null}
        </div>
        {initialLoading ? (
          <ListSkeleton rows={5} />
        ) : myDecks.length === 0 ? (
          <div className="rounded-xl border border-dashed border-slate-200 bg-slate-50/80 p-6 text-center">
            {hasMyFilters ? (
              <>
                <p className="text-sm font-medium text-slate-800">Ничего не найдено</p>
                <p className="mt-1 text-xs text-slate-600">
                  Попробуйте другой запрос или сбросьте фильтры поиска и категории.
                </p>
                <div className="mt-4 flex flex-wrap justify-center gap-2">
                  {trimmedSearchInput ? (
                    <button
                      type="button"
                      className="rounded-lg border border-slate-300 bg-white px-3 py-1.5 text-xs font-medium text-slate-800 hover:bg-slate-50"
                      onClick={resetSearch}
                    >
                      Сбросить поиск
                    </button>
                  ) : null}
                  {filterCategoryId != null ? (
                    <button
                      type="button"
                      className="rounded-lg border border-slate-300 bg-white px-3 py-1.5 text-xs font-medium text-slate-800 hover:bg-slate-50"
                      onClick={() => applyFilter(null)}
                    >
                      Все категории
                    </button>
                  ) : null}
                </div>
              </>
            ) : (
              <>
                <p className="text-sm font-medium text-slate-800">У вас пока нет колод</p>
                <p className="mt-1 text-xs text-slate-600">
                  Создайте колоду кнопкой «Новая колода» вверху или загляните в «Библиотеку» — там публичные наборы
                  карточек, которые можно добавить к себе.
                </p>
              </>
            )}
          </div>
        ) : (
          <ul className="space-y-2">
            {myDecks.map((deck) => (
              <li key={deck.id} className="rounded border border-slate-200 p-3">
                <div className="flex flex-wrap items-start justify-between gap-2">
                  <div className="min-w-0 flex-1">
                    <p className="truncate font-medium">{deck.title}</p>
                    {deck.categoryName ? (
                      <span className="mt-0.5 inline-block rounded-full bg-slate-100 px-2 py-0.5 text-xs text-slate-500">
                        {deck.categoryName}
                      </span>
                    ) : null}
                    <p className="mt-1 truncate text-sm text-slate-600">{deck.description || 'Без описания'}</p>
                  </div>
                  <div className="text-right text-xs text-slate-500">
                    <p>{deck.cardCount} карт.</p>
                    {deck.dueCardCount > 0 ? (
                      <span className="mt-1 inline-block rounded-full bg-slate-900 px-1.5 py-0.5 font-medium text-white">
                        {deck.dueCardCount} к повт.
                      </span>
                    ) : null}
                  </div>
                </div>

                <div className="mt-3 grid grid-cols-3 gap-2">
                  <button
                    type="button"
                    className="rounded border border-slate-300 px-2 py-1.5 text-xs font-medium text-slate-700 hover:bg-slate-50"
                    onClick={() => navigate(`/study?deckId=${deck.id}`)}
                  >
                    Учить
                  </button>
                  <button
                    type="button"
                    className="rounded border border-slate-300 px-2 py-1.5 text-xs font-medium text-slate-700 hover:bg-slate-50"
                    onClick={() => navigate(`/decks/${deck.id}`)}
                  >
                    Редактировать
                  </button>
                  <button
                    type="button"
                    className="rounded border border-red-300 px-2 py-1.5 text-xs font-medium text-red-700 hover:bg-red-50 disabled:opacity-60"
                    onClick={() => void deleteDeck(deck.id)}
                    disabled={deletingDeckId === deck.id}
                  >
                    {deletingDeckId === deck.id ? 'Удаление...' : 'Удалить'}
                  </button>
                </div>
              </li>
            ))}
          </ul>
        )}
        {!initialLoading ? (
          <Pagination
            page={myPage}
            totalPages={myTotalPages}
            onPage={(p) => void loadMyDecks(p, filterCategoryId, searchQ)}
          />
        ) : null}
      </section>
    </div>
  )
}
