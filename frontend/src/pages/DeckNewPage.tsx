import { useCallback, useEffect, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { BackToDecksLink } from '../components/BackToDecksLink'
import { LoggedInNav } from '../components/LoggedInNav'
import { api } from '../lib/api'
import { toast } from '../store/toastStore'
import type { AiCardDraft, AiGenerateCardsResponse, Category, Deck, DeckImportCsvResponse } from '../types/api'

export function DeckNewPage() {
  const navigate = useNavigate()

  const [categories, setCategories] = useState<Category[]>([])
  const [title, setTitle] = useState('')
  const [description, setDescription] = useState('')
  const [selectedCategory, setSelectedCategory] = useState<string>('')
  const [createDeckSaving, setCreateDeckSaving] = useState(false)
  const [csvImporting, setCsvImporting] = useState(false)
  const [csvFormatHelpOpen, setCsvFormatHelpOpen] = useState(false)
  const [aiSourceText, setAiSourceText] = useState('')
  const [aiDesiredCount, setAiDesiredCount] = useState(10)
  const [aiGenerating, setAiGenerating] = useState(false)
  const [aiCards, setAiCards] = useState<AiCardDraft[]>([])
  const [aiModel, setAiModel] = useState('')
  const [aiPanelOpen, setAiPanelOpen] = useState(false)
  const [createWithAiSaving, setCreateWithAiSaving] = useState(false)
  const [initialLoading, setInitialLoading] = useState(true)
  const [dueCount, setDueCount] = useState(0)
  const csvImportInputRef = useRef<HTMLInputElement>(null)

  const loadCategories = useCallback(async () => {
    const { data } = await api.get<Category[]>('/api/categories')
    setCategories(data)
    setSelectedCategory((prev) => (data.length > 0 && !prev ? String(data[0].id) : prev))
  }, [])

  const loadDueCount = useCallback(async () => {
    const { data } = await api.get<{ count: number }>('/api/review/due/count')
    setDueCount(data.count)
  }, [])

  useEffect(() => {
    let active = true
    void (async () => {
      try {
        await Promise.all([loadCategories(), loadDueCount()])
      } catch {
        if (active) toast.error('Не удалось загрузить данные.')
      } finally {
        if (active) setInitialLoading(false)
      }
    })()
    return () => {
      active = false
    }
  }, [loadCategories, loadDueCount])

  const importDeckFromCsv = async (file: File): Promise<void> => {
    setCsvImporting(true)
    try {
      const form = new FormData()
      form.append('file', file)
      const { data } = await api.post<DeckImportCsvResponse>('/api/decks/import-csv', form)
      await loadDueCount()
      toast.success(`Колода «${data.deck.title}» импортирована (${data.cardsImported} карточек).`)
      navigate(`/decks/${data.deck.id}`)
    } catch {
      toast.error('Не удалось импортировать CSV')
    } finally {
      setCsvImporting(false)
      if (csvImportInputRef.current) {
        csvImportInputRef.current.value = ''
      }
    }
  }

  const createDeck = async (): Promise<void> => {
    const categoryId = selectedCategory !== '' ? Number(selectedCategory) : null

    if (categoryId == null) {
      toast.error('Выберите категорию')
      return
    }

    setCreateDeckSaving(true)
    try {
      const { data } = await api.post<Deck>('/api/decks', {
        title,
        description,
        categoryId,
      })
      await loadDueCount()
      toast.success('Колода создана — можно добавлять карточки.')
      navigate(`/decks/${data.id}`)
    } catch {
      toast.error('Не удалось создать колоду')
    } finally {
      setCreateDeckSaving(false)
    }
  }

  const generateAiCards = async (): Promise<void> => {
    const normalizedText = aiSourceText.trim()
    if (!normalizedText) {
      toast.error('Введите текст для AI-генерации')
      return
    }

    setAiGenerating(true)
    try {
      const { data } = await api.post<AiGenerateCardsResponse>('/api/ai/cards/generate', {
        sourceText: normalizedText,
        desiredCount: aiDesiredCount,
      })
      setAiCards(data.cards ?? [])
      setAiModel(data.model)
      if (!data.cards?.length) {
        toast.error('AI не вернул карточки. Попробуйте другой текст.')
      } else {
        toast.success(`Сгенерировано карточек: ${data.cards.length}`)
      }
    } catch {
      toast.error('Не удалось сгенерировать карточки через AI')
    } finally {
      setAiGenerating(false)
    }
  }

  const createDeckWithAiCards = async (): Promise<void> => {
    const categoryId = selectedCategory !== '' ? Number(selectedCategory) : null
    if (categoryId == null) {
      toast.error('Выберите категорию')
      return
    }
    if (aiCards.length === 0) {
      toast.error('Сначала сгенерируйте карточки')
      return
    }

    setCreateWithAiSaving(true)
    try {
      const { data } = await api.post<Deck>('/api/decks', {
        title,
        description,
        categoryId,
      })

      await Promise.all(
        aiCards.map((card, index) =>
          api.post(`/api/decks/${data.id}/cards`, {
            front: card.front,
            back: card.back,
            hint: card.hint,
            position: index + 1,
          }),
        ),
      )

      await loadDueCount()
      toast.success(`Колода создана и заполнена (${aiCards.length} AI-карточек).`)
      navigate(`/decks/${data.id}`)
    } catch {
      toast.error('Не удалось создать колоду с AI-карточками')
    } finally {
      setCreateWithAiSaving(false)
    }
  }

  return (
    <div className="mx-auto max-w-2xl px-4 py-6">
      {csvFormatHelpOpen ? (
        <div
          className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/50 p-4"
          role="dialog"
          aria-modal="true"
          aria-labelledby="csv-format-title"
          onClick={() => setCsvFormatHelpOpen(false)}
        >
          <div
            className="w-full max-w-lg rounded-2xl border border-slate-200 bg-white p-6 shadow-xl"
            onClick={(e) => e.stopPropagation()}
          >
            <h2 id="csv-format-title" className="text-lg font-semibold text-slate-900">
              Структура CSV для импорта
            </h2>
            <p className="mt-2 text-sm text-slate-600">
              Разделитель полей — точка с запятой. Кодировка UTF-8. Сначала метаданные колоды, затем пустая строка,
              заголовок таблицы карточек и строки с данными.
            </p>
            <pre className="mt-4 overflow-x-auto rounded-lg bg-slate-900 p-3 text-left text-xs leading-relaxed text-slate-100">
              {`title;Название колоды
description;Описание (можно пустым)
category;Категория

front;back;hint
Вопрос или лицевая сторона;Ответ;Подсказка (необязательно)
...`}
            </pre>
            <p className="mt-3 text-xs text-slate-500">
              Строка <span className="font-mono text-slate-700">front;back;hint</span> — заголовок; дальше только
              карточки. Если вы укажете в <span className="font-mono text-slate-700">category</span> название, которого
              ещё нет среди ваших категорий, оно будет создано{' '}
              <span className="font-medium text-slate-700">только у вас</span> — в общий список платформы и к другим
              пользователям оно не добавляется.
            </p>
            <button
              type="button"
              className="mt-5 w-full rounded-xl bg-slate-900 py-2.5 text-sm font-medium text-white transition hover:bg-slate-800"
              onClick={() => setCsvFormatHelpOpen(false)}
            >
              Закрыть
            </button>
          </div>
        </div>
      ) : null}

      <div className="mb-6 flex flex-wrap items-start justify-between gap-3">
        <div className="flex min-w-0 flex-1 flex-wrap items-center gap-3">
          <BackToDecksLink />
          <h1 className="text-xl font-semibold sm:text-2xl">Новая колода</h1>
        </div>
        <LoggedInNav dueCount={dueCount} highlight="new-deck" />
      </div>

      <p className="mb-4 text-sm text-slate-600">
        Колода видна только вам. После создания откроется страница колоды — там можно добавить карточки.
      </p>

      <form
        onSubmit={(e) => {
          e.preventDefault()
          void createDeck()
        }}
        className={`rounded-lg border border-slate-200 bg-slate-50/50 p-4 ${initialLoading ? 'pointer-events-none opacity-60' : ''}`}
      >
        <div className="flex flex-wrap items-baseline justify-between gap-2">
          <h2 className="text-sm font-medium text-slate-800">1) Данные колоды</h2>
          <span className="text-[11px] text-slate-400">Название и категория обязательны</span>
        </div>

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
              placeholder="Необязательно — можно кратко описать тему колоды"
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              rows={3}
              maxLength={2000}
            />
          </label>
        </div>

        {categories.length === 0 && !initialLoading ? (
          <p className="mt-2 text-xs text-amber-800">
            Сначала нужна хотя бы одна категория — обратитесь к администратору.
          </p>
        ) : null}

        <div className="mt-5 rounded-lg border border-slate-200 bg-white p-3">
          <h3 className="text-sm font-medium text-slate-800">2) Как создать колоду</h3>
          <p className="mt-1 text-xs text-slate-500">Выберите один из двух вариантов ниже.</p>

          <div className="mt-3 rounded-md border border-slate-200 bg-slate-50 p-3">
            <h4 className="text-sm font-medium text-slate-800">Вариант A: создать пустую или импортировать CSV</h4>
            <div className="mt-3 flex flex-wrap items-center gap-2">
              <input
                ref={csvImportInputRef}
                type="file"
                accept=".csv,text/csv"
                className="sr-only"
                aria-hidden
                onChange={(e) => {
                  const f = e.target.files?.[0]
                  if (f) void importDeckFromCsv(f)
                }}
              />
              <button
                type="submit"
                disabled={createDeckSaving || categories.length === 0}
                className="w-full rounded-lg bg-slate-900 px-3 py-2 text-sm font-medium text-white transition hover:bg-slate-800 disabled:cursor-not-allowed disabled:opacity-50 sm:w-auto"
              >
                {createDeckSaving ? 'Создаём…' : 'Создать пустую колоду'}
              </button>
              <div className="flex w-full flex-wrap items-center gap-2 sm:w-auto">
                <button
                  type="button"
                  onClick={() => setAiPanelOpen((prev) => !prev)}
                  className="min-w-0 flex-1 rounded-lg border border-emerald-300 bg-emerald-50 px-3 py-2 text-sm font-medium text-emerald-800 transition hover:bg-emerald-100 sm:flex-none"
                >
                  {aiPanelOpen ? 'Скрыть генерацию с AI' : 'Генерация с AI'}
                </button>
                <button
                  type="button"
                  disabled={csvImporting || initialLoading}
                  onClick={() => csvImportInputRef.current?.click()}
                  className="min-w-0 flex-1 rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm font-medium text-slate-800 transition hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-50 sm:flex-none"
                >
                  {csvImporting ? 'Импорт…' : 'Импорт из CSV'}
                </button>
                <button
                  type="button"
                  className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full border border-slate-300 bg-white text-sm font-semibold text-slate-600 transition hover:bg-slate-50"
                  aria-label="Какой формат у CSV для импорта"
                  onClick={() => setCsvFormatHelpOpen(true)}
                >
                  ?
                </button>
              </div>
            </div>
            {aiPanelOpen ? (
              <div className="mt-3 rounded-md border border-emerald-200 bg-emerald-50/40 p-3">
                <div className="flex flex-wrap items-center justify-between gap-2">
                  <h4 className="text-sm font-medium text-emerald-900">Генерация карточек</h4>
                  {aiModel ? <span className="text-[11px] text-emerald-700">Модель: {aiModel}</span> : null}
                </div>
                <p className="mt-1 text-xs text-emerald-800/80">
                  Вставьте учебный текст, сгенерируйте карточки и создайте колоду сразу с ними.
                </p>

                <div className="mt-3 grid gap-2.5 sm:grid-cols-[1fr_120px]">
                  <label className="block">
                    <span className="mb-0.5 block text-[11px] font-medium text-slate-600">Исходный текст</span>
                    <textarea
                      className="min-h-[7rem] w-full resize-y rounded-md border border-slate-300 bg-white px-2.5 py-1.5 text-sm focus:border-slate-500 focus:outline-none focus:ring-1 focus:ring-slate-400/40"
                      placeholder="Например, конспект лекции или абзац из учебника"
                      value={aiSourceText}
                      onChange={(e) => setAiSourceText(e.target.value)}
                      maxLength={20000}
                    />
                  </label>
                  <label className="block">
                    <span className="mb-0.5 block text-[11px] font-medium text-slate-600">Кол-во</span>
                    <input
                      type="number"
                      min={1}
                      max={30}
                      className="w-full rounded-md border border-slate-300 bg-white px-2.5 py-1.5 text-sm focus:border-slate-500 focus:outline-none focus:ring-1 focus:ring-slate-400/40"
                      value={aiDesiredCount}
                      onChange={(e) => setAiDesiredCount(Number(e.target.value) || 1)}
                    />
                  </label>
                </div>

                <div className="mt-3 flex flex-wrap items-center gap-2">
                  <button
                    type="button"
                    disabled={aiGenerating || initialLoading}
                    onClick={() => void generateAiCards()}
                    className="rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm font-medium text-slate-800 transition hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-50"
                  >
                    {aiGenerating ? 'Генерируем…' : 'Сгенерировать карточки'}
                  </button>
                  <button
                    type="button"
                    disabled={createWithAiSaving || createDeckSaving || categories.length === 0 || aiCards.length === 0}
                    onClick={() => void createDeckWithAiCards()}
                    className="rounded-lg bg-emerald-600 px-3 py-2 text-sm font-medium text-white transition hover:bg-emerald-500 disabled:cursor-not-allowed disabled:opacity-50"
                  >
                    {createWithAiSaving ? 'Создаём…' : 'Создать колоду с AI-карточками'}
                  </button>
                </div>

                {aiCards.length > 0 ? (
                  <div className="mt-3 max-h-64 overflow-auto rounded-md border border-emerald-200 bg-white p-2">
                    <ul className="space-y-2">
                      {aiCards.map((card, index) => (
                        <li key={`${card.position}-${index}`} className="rounded border border-slate-200 bg-white p-2 text-xs">
                          <div className="font-medium text-slate-800">{index + 1}. {card.front}</div>
                          <div className="mt-1 text-slate-700">{card.back}</div>
                          {card.hint ? <div className="mt-1 text-slate-500">Подсказка: {card.hint}</div> : null}
                        </li>
                      ))}
                    </ul>
                  </div>
                ) : null}
              </div>
            ) : null}
          </div>
        </div>
      </form>
    </div>
  )
}
