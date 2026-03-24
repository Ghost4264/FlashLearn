import { useEffect, useState } from 'react'
import { api } from '../lib/api'
import { useAuthStore } from '../store/authStore'
import type { Deck, PageResponse } from '../types/api'

export function DecksPage() {
  const clearTokens = useAuthStore((state) => state.clearTokens)
  const [myDecks, setMyDecks] = useState<Deck[]>([])
  const [publicDecks, setPublicDecks] = useState<Deck[]>([])
  const [title, setTitle] = useState('')
  const [description, setDescription] = useState('')
  const [isPublic, setIsPublic] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const loadDecks = async (): Promise<void> => {
    setError(null)
    try {
      const [mine, pub] = await Promise.all([
        api.get<PageResponse<Deck>>('/api/decks'),
        api.get<PageResponse<Deck>>('/api/decks/public'),
      ])
      setMyDecks(mine.data.content)
      setPublicDecks(pub.data.content)
    } catch {
      setError('Не удалось загрузить колоды')
    }
  }

  useEffect(() => {
    let active = true
    void (async () => {
      try {
        const [mine, pub] = await Promise.all([
          api.get<PageResponse<Deck>>('/api/decks'),
          api.get<PageResponse<Deck>>('/api/decks/public'),
        ])
        if (!active) {
          return
        }
        setMyDecks(mine.data.content)
        setPublicDecks(pub.data.content)
      } catch {
        if (active) {
          setError('Не удалось загрузить колоды')
        }
      }
    })()
    return () => {
      active = false
    }
  }, [])

  const createDeck = async (): Promise<void> => {
    try {
      await api.post('/api/decks', {
        title,
        description,
        public: isPublic,
      })
      setTitle('')
      setDescription('')
      setIsPublic(false)
      await loadDecks()
    } catch {
      setError('Не удалось создать колоду')
    }
  }

  const logout = (): void => {
    clearTokens()
  }

  return (
    <div className="mx-auto max-w-5xl px-4 py-6">
      <div className="mb-6 flex items-center justify-between">
        <h1 className="text-2xl font-semibold">Мои колоды</h1>
        <button className="rounded border px-3 py-1.5 text-sm" onClick={logout}>
          Выйти
        </button>
      </div>

      {error ? <p className="mb-4 text-red-600">{error}</p> : null}

      <form
        onSubmit={(event) => {
          event.preventDefault()
          void createDeck()
        }}
        className="mb-6 grid gap-2 rounded-lg bg-white p-4 shadow-sm md:grid-cols-4"
      >
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
          {' '}
          <span>Публичная</span>
        </label>
        <button className="rounded bg-slate-900 px-3 py-2 text-white md:col-span-4">Создать колоду</button>
      </form>

      <div className="grid gap-6 md:grid-cols-2">
        <section className="rounded-lg bg-white p-4 shadow-sm">
          <h2 className="mb-3 text-lg font-medium">Ваши колоды ({myDecks.length})</h2>
          <ul className="space-y-2">
            {myDecks.map((deck) => (
              <li key={deck.id} className="rounded border border-slate-200 p-3">
                <p className="font-medium">{deck.title}</p>
                <p className="text-sm text-slate-600">{deck.description || 'Без описания'}</p>
              </li>
            ))}
            {myDecks.length === 0 ? <li className="text-sm text-slate-500">Пока нет колод</li> : null}
          </ul>
        </section>

        <section className="rounded-lg bg-white p-4 shadow-sm">
          <h2 className="mb-3 text-lg font-medium">Публичные колоды ({publicDecks.length})</h2>
          <ul className="space-y-2">
            {publicDecks.map((deck) => (
              <li key={deck.id} className="rounded border border-slate-200 p-3">
                <p className="font-medium">{deck.title}</p>
                <p className="text-sm text-slate-600">{deck.description || 'Без описания'}</p>
              </li>
            ))}
            {publicDecks.length === 0 ? <li className="text-sm text-slate-500">Пока нет публичных колод</li> : null}
          </ul>
        </section>
      </div>
    </div>
  )
}
