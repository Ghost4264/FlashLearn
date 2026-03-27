import { useEffect, useState, useCallback, useRef } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { isAxiosError } from 'axios'
import { WelcomeModal } from '../components/WelcomeModal'
import { api } from '../lib/api'
import { useAuthStore } from '../store/authStore'
import { toast } from '../store/toastStore'
import type { Category, Deck, PageResponse, ReviewStats } from '../types/api'

const PAGE_SIZE = 8

const DUPLICATE_CLONE_MESSAGE =
  'Эта публичная колода уже есть в ваших колодах'

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
  const clearTokens = useAuthStore((state) => state.clearTokens)
  const navigate = useNavigate()
  const handleLogout = (): void => {
    clearTokens()
    navigate('/')
  }

  const [categories, setCategories] = useState<Category[]>([])
  const [filterCategoryId, setFilterCategoryId] = useState<number | null>(null)

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
  const [pubCategories, setPubCategories] = useState<string[]>([])
  const [pubCategoryFilter, setPubCategoryFilter] = useState<string>('')

  const [dueCount, setDueCount] = useState<number>(0)
  const [reviewStats, setReviewStats] = useState<ReviewStats | null>(null)

  const [title, setTitle] = useState('')
  const [description, setDescription] = useState('')
  const [selectedCategory, setSelectedCategory] = useState<string>('')

  const [deletingDeckId, setDeletingDeckId] = useState<number | null>(null)
  const [initialLoading, setInitialLoading] = useState(true)
  const [cloningDeckId, setCloningDeckId] = useState<number | null>(null)
  const [duplicateCloneModalOpen, setDuplicateCloneModalOpen] = useState(false)
  const [createDeckSaving, setCreateDeckSaving] = useState(false)

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
    setMyDecks(data.content ?? [])
    setMyPage(data.page)
    setMyTotalPages(data.totalPages)
    setMyTotal(data.totalElements)
  }, [])

  const loadPublicDecks = useCallback(async (page: number, q: string, categoryName: string) => {
    const qParam = q.trim() ? `&q=${encodeURIComponent(q.trim())}` : ''
    const catParam = categoryName ? `&categoryName=${encodeURIComponent(categoryName)}` : ''
    const { data } = await api.get<PageResponse<Deck>>(
      `/api/decks/public?page=${page}&size=${PAGE_SIZE}${qParam}${catParam}`,
    )
    setPublicDecks(data.content)
    setPubPage(data.page)
    setPubTotalPages(data.totalPages)
  }, [])

  const loadPubCategories = useCallback(async () => {
    const { data } = await api.get<string[]>('/api/decks/public/categories')
    setPubCategories(data)
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
        await Promise.all([
          loadCategories(),
          loadMyDecks(0, null, ''),
          loadPublicDecks(0, '', ''),
          loadPubCategories(),
          loadStudyMetrics(),
        ])
      } catch {
        if (active) toast.error('Не удалось загрузить данные. Проверьте соединение.')
      } finally {
        if (active) setInitialLoading(false)
      }
    })()
    return () => {
      active = false
    }
  }, [loadCategories, loadMyDecks, loadPublicDecks, loadPubCategories, loadStudyMetrics])

  useEffect(() => {
    if (debounceRef.current) clearTimeout(debounceRef.current)
    debounceRef.current = setTimeout(() => {
      setSearchQ(searchInput)
      void loadMyDecks(0, filterCategoryId, searchInput)
      void loadPublicDecks(0, searchInput, pubCategoryFilter)
    }, 350)
    return () => {
      if (debounceRef.current) clearTimeout(debounceRef.current)
    }
  }, [searchInput, filterCategoryId, pubCategoryFilter, loadMyDecks, loadPublicDecks])

  const applyFilter = (categoryId: number | null) => {
    setFilterCategoryId(categoryId)
    void loadMyDecks(0, categoryId, searchQ)
  }

  const createDeck = async (): Promise<void> => {
    const categoryId = selectedCategory !== '' ? Number(selectedCategory) : null

    if (categoryId == null) {
      toast.error('Выберите категорию')
      return
    }

    setCreateDeckSaving(true)
    try {
      await api.post('/api/decks', {
        title,
        description,
        categoryId,
      })
      setTitle('')
      setDescription('')
      if (categories.length > 0) {
        setSelectedCategory(String(categories[0].id))
      }
      await Promise.all([loadMyDecks(0, filterCategoryId, searchQ), loadStudyMetrics()])
      toast.success('Колода создана — она в списке «Ваши колоды».')
    } catch {
      toast.error('Не удалось создать колоду')
    } finally {
      setCreateDeckSaving(false)
    }
  }

  const cloneDeck = async (deckId: number): Promise<void> => {
    try {
      setCloningDeckId(deckId)
      await api.post(`/api/decks/${deckId}/clone`)
      await Promise.all([
        loadMyDecks(myPage, filterCategoryId, searchQ),
        loadPublicDecks(pubPage, searchQ, pubCategoryFilter),
        loadStudyMetrics(),
      ])
      toast.success('Колода добавлена в «Ваши колоды».')
    } catch (error: unknown) {
      if (isAxiosError(error) && typeof error.response?.data?.detail === 'string') {
        const detail = error.response.data.detail
        if (detail === DUPLICATE_CLONE_MESSAGE || detail.includes('уже есть в ваших колодах')) {
          setDuplicateCloneModalOpen(true)
          return
        }
        toast.error(detail)
        return
      }
      toast.error('Не удалось клонировать колоду')
    } finally {
      setCloningDeckId(null)
    }
  }

  const deleteDeck = async (deckId: number): Promise<void> => {
    const accepted = window.confirm('Удалить колоду? Это действие необратимо.')
    if (!accepted) return

    try {
      setDeletingDeckId(deckId)
      await api.delete(`/api/decks/${deckId}`)
      await Promise.all([
        loadMyDecks(myPage, filterCategoryId, searchQ),
        loadPublicDecks(pubPage, searchQ, pubCategoryFilter),
        loadStudyMetrics(),
      ])
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
  const hasPubFilters = Boolean(trimmedSearchInput) || Boolean(pubCategoryFilter)

  const resetSearch = (): void => {
    setSearchInput('')
  }

  return (
    <div className="mx-auto max-w-5xl px-4 py-6">
      {duplicateCloneModalOpen ? (
        <div
          className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/50 p-4"
          role="dialog"
          aria-modal="true"
          aria-labelledby="duplicate-clone-title"
          onClick={() => setDuplicateCloneModalOpen(false)}
        >
          <div
            className="w-full max-w-md rounded-2xl border border-amber-200 bg-white p-6 shadow-xl"
            onClick={(e) => e.stopPropagation()}
          >
            <div className="mb-3 flex items-start gap-3">
              <span
                className="flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-amber-100 text-amber-800"
                aria-hidden
              >
                <svg className="h-5 w-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                  <circle cx="12" cy="12" r="10" strokeLinecap="round" />
                  <path strokeLinecap="round" d="M12 16v-4M12 8h.01" />
                </svg>
              </span>
              <div>
                <h2 id="duplicate-clone-title" className="text-lg font-semibold text-slate-900">
                  Колода уже в ваших колодах!
                </h2>
                <p className="mt-2 text-sm leading-relaxed text-slate-600">
                  Эту публичную колоду вы уже добавляли к себе — повторно клонировать не нужно. Она
                  отображается в списке слева.
                </p>
              </div>
            </div>
            <button
              type="button"
              className="mt-2 w-full rounded-xl bg-slate-900 py-2.5 text-sm font-medium text-white transition hover:bg-slate-800"
              onClick={() => setDuplicateCloneModalOpen(false)}
            >
              Понятно
            </button>
          </div>
        </div>
      ) : null}
      <div className="mb-6 flex flex-wrap items-center justify-between gap-3">
        <h1 className="text-xl font-semibold sm:text-2xl">Мои колоды</h1>
        <div className="flex items-center gap-2">
          <button
            type="button"
            className={`flex items-center gap-1.5 rounded-lg px-3 py-1.5 text-sm font-medium sm:px-4 sm:py-2 ${
              dueCount > 0
                ? 'bg-slate-900 text-white'
                : 'border border-slate-300 bg-white text-slate-600 hover:bg-slate-50'
            }`}
            title={
              dueCount === 0
                ? 'Сейчас нет карточек к повторению — откроется экран с подсказкой'
                : undefined
            }
            onClick={() => navigate('/study')}
          >
            Учить
            {dueCount > 0 ? (
              <span className="rounded-full bg-white px-1.5 py-0.5 text-xs font-bold text-slate-900">
                {dueCount}
              </span>
            ) : null}
          </button>
          <Link to="/profile" className="rounded border px-2.5 py-1.5 text-sm hover:bg-slate-50">
            Профиль
          </Link>
          <button className="rounded border px-2.5 py-1.5 text-sm" onClick={handleLogout}>
            Выйти
          </button>
        </div>
      </div>

      <WelcomeModal />

      <div className="sticky top-2 z-20 mb-4 rounded-xl border border-slate-200 bg-white/95 p-3 shadow-sm backdrop-blur">
        <div className="mb-2 text-xs font-medium uppercase tracking-wide text-slate-500">Фильтры и поиск</div>
        <div className="relative">
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
      </div>

      {!initialLoading && reviewStats ? (
        <div className="mb-4">
          <div className="mb-2 text-xs font-medium uppercase tracking-wide text-slate-500">Статистика учёбы</div>
          <div className="grid grid-cols-3 gap-2 sm:gap-3">
            <div className="rounded-xl bg-white p-2 sm:p-3 text-center shadow-sm">
              <p className="text-xl font-bold text-emerald-700 sm:text-2xl">{reviewStats.reviewedToday}</p>
              <p className="text-xs text-slate-500">оценок сегодня</p>
            </div>
            <div className="rounded-xl bg-white p-2 sm:p-3 text-center shadow-sm">
              <p className="text-xl font-bold text-sky-800 sm:text-2xl">{reviewStats.reviewedThisWeek}</p>
              <p className="text-xs text-slate-500">за неделю</p>
            </div>
            <div className="rounded-xl bg-white p-2 sm:p-3 text-center shadow-sm">
              <p className="text-xl font-bold text-amber-800 sm:text-2xl">{reviewStats.streakDays}</p>
              <p className="text-xs text-slate-500">дней подряд</p>
            </div>
          </div>
        </div>
      ) : null}

      {!initialLoading && myTotal > 0 ? (
        <div className="mb-6 grid grid-cols-3 gap-2 sm:gap-3">
          <div className="rounded-xl bg-white p-2 sm:p-3 text-center shadow-sm">
            <p className="text-xl sm:text-2xl font-bold">{myTotal}</p>
            <p className="text-xs text-slate-500">колод</p>
          </div>
          <div className="rounded-xl bg-white p-2 sm:p-3 text-center shadow-sm">
            <p className="text-xl sm:text-2xl font-bold">{totalCards}</p>
            <p className="text-xs text-slate-500">карточек на стр.</p>
          </div>
          <div className="rounded-xl bg-white p-2 sm:p-3 text-center shadow-sm">
            <p className={`text-xl sm:text-2xl font-bold ${dueCount > 0 ? 'text-slate-900' : 'text-slate-300'}`}>
              {dueCount}
            </p>
            <p className="text-xs text-slate-500">к повторению</p>
          </div>
        </div>
      ) : null}

      <form
        onSubmit={(event) => {
          event.preventDefault()
          void createDeck()
        }}
        className={`mb-5 rounded-lg border border-slate-200 bg-slate-50/50 p-3 sm:p-4 ${initialLoading ? 'pointer-events-none opacity-60' : ''}`}
      >
        <div className="flex flex-wrap items-baseline justify-between gap-2">
          <h2 className="text-sm font-medium text-slate-800">Новая колода</h2>
          <span className="text-[11px] text-slate-400">личная, только у вас</span>
        </div>

        <p className="mt-1 text-[11px] leading-snug text-slate-500">
          Название и категория обязательны. Описание — по желанию.
        </p>


        <div className="mt-3 grid gap-2.5 sm:grid-cols-2">
          <label className="block sm:col-span-1">
            <span className="mb-0.5 block text-[11px] font-medium text-slate-600">Название</span>
            <input
              className="w-full rounded-md border border-slate-300 bg-white px-2.5 py-1.5 text-sm focus:border-slate-500 focus:outline-none focus:ring-1 focus:ring-slate-400/40"
              placeholder="Например: Английский A1"
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              maxLength={200}
              required
              autoComplete="off"
            />
          </label>

          <label className="block sm:col-span-1">
            <span className="mb-0.5 block text-[11px] font-medium text-slate-600">Категория</span>
            <select
              className="w-full rounded-md border border-slate-300 bg-white px-2.5 py-1.5 text-sm focus:border-slate-500 focus:outline-none focus:ring-1 focus:ring-slate-400/40"
              value={selectedCategory}
              onChange={(e) => setSelectedCategory(e.target.value)}
              required
            >
              <option value="" disabled>
                Выберите категорию
              </option>
              {categories.map((cat) => (
                <option key={cat.id} value={String(cat.id)}>
                  {cat.name}
                </option>
              ))}
            </select>
          </label>

          <label className="block sm:col-span-2">
            <span className="mb-0.5 block text-[11px] font-medium text-slate-600">Описание</span>
            <textarea
              className="min-h-[2.75rem] w-full resize-y rounded-md border border-slate-300 bg-white px-2.5 py-1.5 text-sm focus:border-slate-500 focus:outline-none focus:ring-1 focus:ring-slate-400/40"
              placeholder="Необязательно"
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              rows={2}
              maxLength={2000}
            />
          </label>
        </div>

        {categories.length === 0 && !initialLoading ? (
          <p className="mt-2 text-xs text-amber-800">Сначала нужна хотя бы одна категория — обратитесь к администратору.</p>
        ) : null}

        <button
          type="submit"
          disabled={createDeckSaving || categories.length === 0}
          className="mt-3 w-full rounded-lg bg-slate-900 px-3 py-2 text-sm font-medium text-white transition hover:bg-slate-800 disabled:cursor-not-allowed disabled:opacity-50 sm:w-auto"
        >
          {createDeckSaving ? 'Создаём…' : 'Создать колоду'}
        </button>
      </form>

      <div className="grid gap-6 md:grid-cols-2">
        <section className="overflow-hidden rounded-lg bg-white p-4 shadow-sm">
          <div className="mb-3 flex flex-wrap items-center justify-between gap-2">
            <h2 className="text-base font-medium sm:text-lg">Ваши колоды ({myTotal})</h2>
            {categories.length > 0 ? (
              <select
                className="max-w-[160px] rounded border border-slate-300 px-2 py-1 text-sm"
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
                    Создайте первую колоду в форме выше — укажите название и категорию. Справа можно добавить
                    публичные колоды к себе.
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
                      <p className="font-medium truncate">{deck.title}</p>
                      {deck.categoryName ? (
                        <span className="mt-0.5 inline-block rounded-full bg-slate-100 px-2 py-0.5 text-xs text-slate-500">
                          {deck.categoryName}
                        </span>
                      ) : null}
                      <p className="mt-1 text-sm text-slate-600 truncate">{deck.description || 'Без описания'}</p>
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
                      className="rounded border border-slate-300 px-2 py-1.5 text-xs font-medium text-slate-700 hover:bg-slate-50"
                      onClick={() => navigate(`/study?deckId=${deck.id}`)}
                    >
                      Учить
                    </button>
                    <button
                      className="rounded border border-slate-300 px-2 py-1.5 text-xs font-medium text-slate-700 hover:bg-slate-50"
                      onClick={() => navigate(`/decks/${deck.id}`)}
                    >
                      Редактировать
                    </button>
                    <button
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

        <section className="overflow-hidden rounded-lg bg-white p-4 shadow-sm">
          <div className="mb-3 flex flex-wrap items-center justify-between gap-2">
            <h2 className="text-base font-medium sm:text-lg">Публичные колоды</h2>
            {pubCategories.length > 0 ? (
              <select
                className="max-w-[160px] rounded border border-slate-300 px-2 py-1 text-sm"
                value={pubCategoryFilter}
                onChange={(e) => {
                  const value = e.target.value
                  setPubCategoryFilter(value)
                  void loadPublicDecks(0, searchQ, value)
                }}
              >
                <option value="">Все категории</option>
                {pubCategories.map((cat) => (
                  <option key={cat} value={cat}>{cat}</option>
                ))}
              </select>
            ) : null}
          </div>
          {initialLoading ? (
            <ListSkeleton rows={5} />
          ) : publicDecks.length === 0 ? (
            <div className="rounded-xl border border-dashed border-slate-200 bg-slate-50/80 p-6 text-center">
              {hasPubFilters ? (
                <>
                  <p className="text-sm font-medium text-slate-800">Нет результатов</p>
                  <p className="mt-1 text-xs text-slate-600">
                    Измените поиск или категорию — или сбросьте фильтры, чтобы увидеть все публичные колоды.
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
                    {pubCategoryFilter ? (
                      <button
                        type="button"
                        className="rounded-lg border border-slate-300 bg-white px-3 py-1.5 text-xs font-medium text-slate-800 hover:bg-slate-50"
                        onClick={() => {
                          setPubCategoryFilter('')
                          void loadPublicDecks(0, searchQ, '')
                        }}
                      >
                        Все категории
                      </button>
                    ) : null}
                  </div>
                </>
              ) : (
                <>
                  <p className="text-sm font-medium text-slate-800">Пока нет публичных колод</p>
                  <p className="mt-1 text-xs text-slate-600">
                    Создайте свою колоду слева или загляните позже — каталог может пополняться администратором.
                  </p>
                </>
              )}
            </div>
          ) : (
            <ul className="space-y-2">
              {publicDecks.map((deck) => (
                <li
                  key={deck.id}
                  className="flex flex-col gap-2 rounded border border-slate-200 p-3 sm:flex-row sm:items-start sm:justify-between sm:gap-3"
                >
                  <div className="min-w-0 flex-1">
                    <div className="flex flex-wrap items-center gap-2">
                      <p className="font-medium truncate">{deck.title}</p>
                      {deck.alreadyCloned ? (
                        <span className="shrink-0 rounded-full bg-emerald-100 px-2 py-0.5 text-[11px] font-medium text-emerald-800">
                          Уже в колодах
                        </span>
                      ) : null}
                    </div>
                    {deck.categoryName ? (
                      <span className="mt-0.5 inline-block rounded-full bg-slate-100 px-2 py-0.5 text-xs text-slate-500">
                        {deck.categoryName}
                      </span>
                    ) : null}
                    <p className="mt-1 text-sm text-slate-600 truncate">{deck.description || 'Без описания'}</p>
                    <p className="mt-0.5 text-xs text-slate-400">{deck.cardCount} карт.</p>
                  </div>
                  <button
                    type="button"
                    disabled={Boolean(deck.alreadyCloned) || cloningDeckId === deck.id}
                    onClick={() => void cloneDeck(deck.id)}
                    className={
                      deck.alreadyCloned
                        ? 'w-full shrink-0 cursor-not-allowed rounded-lg border border-slate-200 bg-slate-50 px-3 py-1.5 text-xs font-medium text-slate-500 sm:w-auto'
                        : 'w-full shrink-0 rounded-lg border border-slate-300 px-3 py-1.5 text-xs font-medium text-slate-700 transition-colors hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-60 sm:w-auto'
                    }
                  >
                    {cloningDeckId === deck.id ? 'Добавление...' : deck.alreadyCloned ? 'Добавлено' : 'Клонировать'}
                  </button>
                </li>
              ))}
            </ul>
          )}
          {!initialLoading ? (
            <Pagination
              page={pubPage}
              totalPages={pubTotalPages}
              onPage={(p) => void loadPublicDecks(p, searchQ, pubCategoryFilter)}
            />
          ) : null}
        </section>
      </div>
    </div>
  )
}
