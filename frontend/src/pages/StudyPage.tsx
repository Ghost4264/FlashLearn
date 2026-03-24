import { useEffect, useCallback, useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { api } from '../lib/api'
import type { StudyCard } from '../types/api'

type Quality = 1 | 3 | 5

const QUALITY_BUTTONS: { label: string; quality: Quality; className: string }[] = [
  { label: 'Не знаю', quality: 1, className: 'bg-red-500 hover:bg-red-600 text-white' },
  { label: 'Трудно', quality: 3, className: 'bg-yellow-400 hover:bg-yellow-500 text-white' },
  { label: 'Легко', quality: 5, className: 'bg-green-500 hover:bg-green-600 text-white' },
]

export function StudyPage() {
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const deckId = searchParams.get('deckId')
  const backPath = deckId ? `/decks/${deckId}` : '/decks'

  const [cards, setCards] = useState<StudyCard[]>([])
  const [currentIndex, setCurrentIndex] = useState(0)
  const [isFlipped, setIsFlipped] = useState(false)
  const [loading, setLoading] = useState(true)
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [reviewed, setReviewed] = useState(0)

  useEffect(() => {
    void (async () => {
      try {
        const url = deckId ? `/api/review/due?deckId=${deckId}` : '/api/review/due'
        const { data } = await api.get<StudyCard[]>(url)
        setCards(data)
      } catch {
        setError('Не удалось загрузить карточки')
      } finally {
        setLoading(false)
      }
    })()
  }, [deckId])

  const currentCard = cards[currentIndex]
  const isDone = !loading && (cards.length === 0 || currentIndex >= cards.length)

  const handleFlip = useCallback(() => {
    if (!isFlipped) setIsFlipped(true)
  }, [isFlipped])

  const handleRate = useCallback(async (quality: Quality) => {
    if (!currentCard || submitting) return
    setSubmitting(true)
    try {
      await api.post('/api/review', { cardId: currentCard.id, quality })
      setReviewed((r) => r + 1)
      setIsFlipped(false)
      setCurrentIndex((i) => i + 1)
    } catch {
      setError('Не удалось сохранить оценку')
    } finally {
      setSubmitting(false)
    }
  }, [currentCard, submitting])

  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      if (e.target instanceof HTMLInputElement || e.target instanceof HTMLTextAreaElement) return
      if (isDone || loading) return
      if (!isFlipped) {
        if (e.code === 'Space' || e.code === 'Enter') {
          e.preventDefault()
          handleFlip()
        }
      } else {
        if (e.key === '1') void handleRate(1)
        else if (e.key === '2') void handleRate(3)
        else if (e.key === '3') void handleRate(5)
      }
    }
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [isFlipped, isDone, loading, handleFlip, handleRate])

  if (loading) {
    return (
      <div className="flex min-h-screen items-center justify-center">
        <p className="text-slate-500">Загрузка карточек...</p>
      </div>
    )
  }

  if (error) {
    return (
      <div className="flex min-h-screen flex-col items-center justify-center gap-4">
        <p className="text-red-600">{error}</p>
        <button
          className="rounded bg-slate-900 px-4 py-2 text-sm text-white"
          onClick={() => navigate(backPath)}
        >
          Назад к колодам
        </button>
      </div>
    )
  }

  if (isDone) {
    return (
      <div className="flex min-h-screen flex-col items-center justify-center gap-6 px-4">
        <div className="rounded-2xl bg-white p-10 shadow-md text-center max-w-sm w-full">
          <div className="text-5xl mb-4">🎉</div>
          <h1 className="text-2xl font-bold mb-2">Готово!</h1>
          <p className="text-slate-600 mb-6">
            {reviewed > 0
              ? `Вы повторили ${reviewed} ${pluralCards(reviewed)}`
              : 'Нет карточек для повторения сегодня'}
          </p>
          <button
            className="w-full rounded-lg bg-slate-900 px-4 py-2.5 text-white font-medium"
            onClick={() => navigate(backPath)}
          >
            К колодам
          </button>
        </div>
      </div>
    )
  }

  return (
    <div className="flex min-h-screen flex-col items-center justify-center px-4 py-8">
      <div className="mb-6 flex w-full max-w-lg items-center justify-between">
        <button
          className="text-sm text-slate-500 hover:text-slate-700"
          onClick={() => navigate(backPath)}
        >
          ← Назад
        </button>
        <span className="text-sm text-slate-500">
          {currentIndex + 1} / {cards.length}
        </span>
      </div>

      <div className="mb-6 h-1.5 w-full max-w-lg overflow-hidden rounded-full bg-slate-200">
        <div
          className="h-full rounded-full bg-slate-900 transition-all duration-300"
          style={{ width: `${(currentIndex / cards.length) * 100}%` }}
        />
      </div>

      <div
        className="relative w-full max-w-lg"
        style={{ perspective: '1000px' }}
      >
        <div
          className="relative w-full transition-transform duration-500"
          style={{
            transformStyle: 'preserve-3d',
            transform: isFlipped ? 'rotateY(180deg)' : 'rotateY(0deg)',
            minHeight: '260px',
          }}
        >
          <div
            className="absolute inset-0 flex flex-col items-center justify-center rounded-2xl bg-white p-8 shadow-md"
            style={{ backfaceVisibility: 'hidden' }}
          >
            <div className="mb-4 flex items-center gap-2">
              <p className="text-xs font-medium uppercase tracking-widest text-slate-400">Вопрос</p>
              {currentCard.isNew ? (
                <span className="rounded-full bg-blue-100 px-2 py-0.5 text-xs font-semibold text-blue-600">
                  Новая
                </span>
              ) : (
                <span className="rounded-full bg-amber-100 px-2 py-0.5 text-xs font-semibold text-amber-600">
                  Повторение
                </span>
              )}
            </div>
            <p className="text-center text-xl font-medium text-slate-800">{currentCard.front}</p>
            {currentCard.hint ? (
              <p className="mt-4 text-sm text-slate-400 italic">Подсказка: {currentCard.hint}</p>
            ) : null}
          </div>

          <div
            className="absolute inset-0 flex flex-col items-center justify-center rounded-2xl bg-slate-900 p-8 shadow-md"
            style={{ backfaceVisibility: 'hidden', transform: 'rotateY(180deg)' }}
          >
            <p className="text-xs font-medium uppercase tracking-widest text-slate-400 mb-4">Ответ</p>
            <p className="text-center text-xl font-medium text-white">{currentCard.back}</p>
          </div>
        </div>
      </div>

      <div className="mt-8 w-full max-w-lg">
        {!isFlipped ? (
          <>
            <button
              className="w-full rounded-xl bg-slate-900 py-3.5 text-white font-medium text-sm tracking-wide"
              onClick={handleFlip}
            >
              Показать ответ
            </button>
            <p className="mt-2 text-center text-xs text-slate-400">Space / Enter</p>
          </>
        ) : (
          <>
            <div className="grid grid-cols-3 gap-3">
              {QUALITY_BUTTONS.map(({ label, quality, className }) => (
                <button
                  key={quality}
                  disabled={submitting}
                  className={`rounded-xl py-3.5 text-sm font-medium transition-opacity disabled:opacity-50 ${className}`}
                  onClick={() => void handleRate(quality)}
                >
                  {label}
                </button>
              ))}
            </div>
            <div className="mt-2 grid grid-cols-3 gap-3 text-center">
              {(['1', '2', '3'] as const).map((key) => (
                <p key={key} className="text-xs text-slate-400">{key}</p>
              ))}
            </div>
          </>
        )}
      </div>
    </div>
  )
}

function pluralCards(n: number): string {
  const mod10 = n % 10
  const mod100 = n % 100
  if (mod10 === 1 && mod100 !== 11) return 'карточку'
  if (mod10 >= 2 && mod10 <= 4 && (mod100 < 10 || mod100 >= 20)) return 'карточки'
  return 'карточек'
}
