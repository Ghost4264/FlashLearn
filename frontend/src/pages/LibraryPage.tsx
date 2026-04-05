import { useCallback, useEffect, useRef, useState } from 'react'
import { isAxiosError } from 'axios'
import { BackToDecksLink } from '../components/BackToDecksLink'
import { LoggedInNav } from '../components/LoggedInNav'
import { api } from '../lib/api'
import { toast } from '../store/toastStore'
import type { Deck, PageResponse } from '../types/api'

const PAGE_SIZE = 9

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
    <div className="mt-6 flex flex-wrap items-center justify-center gap-1">
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
            i === page ? 'border-slate-900 bg-slate-900 text-white' : 'hover:bg-slate-50'
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

function ListSkeleton({ rows = 6 }: { rows?: number }) {
  return (
    <ul className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
      {Array.from({ length: rows }, (_, i) => (
        <li key={i} className="animate-pulse rounded-xl border border-slate-100 bg-slate-50 p-4">
          <div className="h-4 w-3/4 rounded bg-slate-200" />
          <div className="mt-3 h-3 w-20 rounded bg-slate-200" />
          <div className="mt-3 space-y-2">
            <div className="h-3 w-full rounded bg-slate-100" />
            <div className="h-3 w-full rounded bg-slate-100" />
            <div className="h-3 w-2/3 rounded bg-slate-100" />
          </div>
        </li>
      ))}
    </ul>
  )
}

export function LibraryPage() {
  const [publicDecks, setPublicDecks] = useState<Deck[]>([])
  const [pubPage, setPubPage] = useState(0)
  const [pubTotalPages, setPubTotalPages] = useState(0)
  const [pubCategories, setPubCategories] = useState<string[]>([])
  const [pubCategoryFilter, setPubCategoryFilter] = useState<string>('')
  const [searchInput, setSearchInput] = useState('')
  const [searchQ, setSearchQ] = useState('')
  const debounceRef = useRef<ReturnType<typeof setTimeout> | null>(null)

  const [initialLoading, setInitialLoading] = useState(true)
  const [cloningDeckId, setCloningDeckId] = useState<number | null>(null)
  const [duplicateCloneModalOpen, setDuplicateCloneModalOpen] = useState(false)
  const [dueCount, setDueCount] = useState(0)

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

  const loadDueCount = useCallback(async () => {
    const { data } = await api.get<{ count: number }>('/api/review/due/count')
    setDueCount(data.count)
  }, [])

  useEffect(() => {
    let active = true
    void (async () => {
      try {
        await Promise.all([loadPublicDecks(0, '', ''), loadPubCategories(), loadDueCount()])
      } catch {
        if (active) toast.error('Не удалось загрузить библиотеку.')
      } finally {
        if (active) setInitialLoading(false)
      }
    })()
    return () => {
      active = false
    }
  }, [loadPublicDecks, loadPubCategories, loadDueCount])

  useEffect(() => {
    if (debounceRef.current) clearTimeout(debounceRef.current)
    debounceRef.current = setTimeout(() => {
      setSearchQ(searchInput)
      void loadPublicDecks(0, searchInput, pubCategoryFilter)
    }, 350)
    return () => {
      if (debounceRef.current) clearTimeout(debounceRef.current)
    }
  }, [searchInput, pubCategoryFilter, loadPublicDecks])

  const cloneDeck = async (deckId: number): Promise<void> => {
    try {
      setCloningDeckId(deckId)
      await api.post(`/api/decks/${deckId}/clone`)
      await Promise.all([
        loadPublicDecks(pubPage, searchQ, pubCategoryFilter),
        loadDueCount(),
      ])
      toast.success('Колода добавлена в «Мои колоды».')
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
      toast.error('Не удалось добавить колоду')
    } finally {
      setCloningDeckId(null)
    }
  }

  const trimmedSearch = searchInput.trim()
  const hasFilters = Boolean(trimmedSearch) || Boolean(pubCategoryFilter)

  return (
    <div className="mx-auto max-w-6xl px-4 py-6">
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
                  Колода уже у вас
                </h2>
                <p className="mt-2 text-sm leading-relaxed text-slate-600">
                  Эту публичную колоду вы уже добавляли — она есть в разделе «Мои колоды». Повторно клонировать не
                  нужно.
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

      <div className="mb-6">
        <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
          <BackToDecksLink />
          <LoggedInNav dueCount={dueCount} highlight="library" />
        </div>
        <h1 className="text-xl font-semibold sm:text-2xl">Библиотека</h1>
        <p className="mt-2 max-w-xl text-sm text-slate-600">
          Публичные колоды от сообщества и администрации. Ознакомьтесь с описанием и добавьте понравившуюся колоду к
          себе — появится копия для учёбы по SM-2.
        </p>
      </div>

      <div className="mb-6 rounded-xl border border-slate-200 bg-white p-4 shadow-sm">
        <div className="mb-3 flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">
          <div className="relative min-w-0 flex-1">
            <span className="absolute inset-y-0 left-3 flex items-center text-slate-400 pointer-events-none">🔍</span>
            <input
              className="w-full rounded-lg border border-slate-300 py-2 pl-9 pr-4 text-sm focus:border-slate-500 focus:outline-none"
              placeholder="Поиск по названию или описанию…"
              value={searchInput}
              onChange={(e) => setSearchInput(e.target.value)}
            />
            {searchInput ? (
              <button
                type="button"
                onClick={() => setSearchInput('')}
                className="absolute inset-y-0 right-3 flex items-center text-slate-400 hover:text-slate-600"
              >
                ×
              </button>
            ) : null}
          </div>
          {pubCategories.length > 0 ? (
            <select
              className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm sm:w-56"
              value={pubCategoryFilter}
              onChange={(e) => {
                const value = e.target.value
                setPubCategoryFilter(value)
                void loadPublicDecks(0, searchQ, value)
              }}
            >
              <option value="">Все категории</option>
              {pubCategories.map((cat) => (
                <option key={cat} value={cat}>
                  {cat}
                </option>
              ))}
            </select>
          ) : null}
        </div>
      </div>

      {initialLoading ? (
        <ListSkeleton rows={6} />
      ) : publicDecks.length === 0 ? (
        <div className="rounded-xl border border-dashed border-slate-200 bg-slate-50/80 p-10 text-center">
          {hasFilters ? (
            <>
              <p className="text-sm font-medium text-slate-800">Ничего не найдено</p>
              <p className="mt-1 text-sm text-slate-600">Попробуйте другой запрос или сбросьте фильтры.</p>
              <div className="mt-4 flex flex-wrap justify-center gap-2">
                {trimmedSearch ? (
                  <button
                    type="button"
                    className="rounded-lg border border-slate-300 bg-white px-3 py-1.5 text-xs font-medium hover:bg-slate-50"
                    onClick={() => setSearchInput('')}
                  >
                    Сбросить поиск
                  </button>
                ) : null}
                {pubCategoryFilter ? (
                  <button
                    type="button"
                    className="rounded-lg border border-slate-300 bg-white px-3 py-1.5 text-xs font-medium hover:bg-slate-50"
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
              <p className="mt-1 text-sm text-slate-600">
                Каталог может пополняться администратором. Свои колоды создавайте в разделе «Новая колода».
              </p>
            </>
          )}
        </div>
      ) : (
        <>
          <ul className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {publicDecks.map((deck) => (
              <li
                key={deck.id}
                className="flex flex-col rounded-xl border border-slate-200 bg-white p-4 shadow-sm transition-shadow hover:shadow-md"
              >
                <div className="min-w-0 flex-1">
                  <div className="flex flex-wrap items-start justify-between gap-2">
                    <h2 className="text-base font-semibold leading-snug text-slate-900">{deck.title}</h2>
                    {deck.alreadyCloned ? (
                      <span className="shrink-0 rounded-full bg-emerald-100 px-2 py-0.5 text-[11px] font-medium text-emerald-800">
                        У вас
                      </span>
                    ) : null}
                  </div>
                  {deck.categoryName ? (
                    <span className="mt-2 inline-block rounded-full bg-slate-100 px-2.5 py-0.5 text-xs text-slate-600">
                      {deck.categoryName}
                    </span>
                  ) : null}
                  <p className="mt-3 text-sm leading-relaxed text-slate-600">
                    {deck.description?.trim() ? deck.description : 'Описание не указано — откройте колоду у себя после добавления, чтобы добавить карточки или уточнить тему.'}
                  </p>
                  <p className="mt-3 text-xs text-slate-400">{deck.cardCount} карточек в колоде</p>
                </div>
                <div className="mt-4 border-t border-slate-100 pt-3">
                  <p className="mb-2 text-[11px] leading-snug text-slate-500">
                    Добавление копирует колоду в «Мои колоды»; прогресс повторений ведётся отдельно от других
                    пользователей.
                  </p>
                  <button
                    type="button"
                    disabled={Boolean(deck.alreadyCloned) || cloningDeckId === deck.id}
                    onClick={() => void cloneDeck(deck.id)}
                    className={
                      deck.alreadyCloned
                        ? 'w-full cursor-not-allowed rounded-lg border border-slate-200 bg-slate-50 py-2 text-sm font-medium text-slate-500'
                        : 'w-full rounded-lg border border-slate-800 bg-slate-900 py-2 text-sm font-medium text-white transition hover:bg-slate-800 disabled:cursor-not-allowed disabled:opacity-60'
                    }
                  >
                    {cloningDeckId === deck.id ? 'Добавляем…' : deck.alreadyCloned ? 'Уже добавлена' : 'Добавить к себе'}
                  </button>
                </div>
              </li>
            ))}
          </ul>
          <Pagination
            page={pubPage}
            totalPages={pubTotalPages}
            onPage={(p) => void loadPublicDecks(p, searchQ, pubCategoryFilter)}
          />
        </>
      )}
    </div>
  )
}
