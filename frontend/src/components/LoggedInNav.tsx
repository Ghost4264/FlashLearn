import { useEffect, useRef, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuthStore } from '../store/authStore'

export type LoggedInNavHighlight = 'library' | 'new-deck'

type Props = {
  dueCount: number
  highlight?: LoggedInNavHighlight
}

const linkBase = 'rounded border px-2.5 py-1.5 text-sm transition-colors'

function HamburgerIcon({ open }: { open: boolean }) {
  return (
    <svg
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="2"
      strokeLinecap="round"
      aria-hidden
      className={`h-5 w-5 transition-transform ${open ? 'rotate-90' : ''}`}
    >
      {open ? (
        <>
          <path d="M18 6L6 18M6 6l12 12" />
        </>
      ) : (
        <>
          <path d="M4 6h16M4 12h16M4 18h16" />
        </>
      )}
    </svg>
  )
}

export function LoggedInNav({ dueCount, highlight }: Props) {
  const clearTokens = useAuthStore((s) => s.clearTokens)
  const navigate = useNavigate()
  const [menuOpen, setMenuOpen] = useState(false)
  const rootRef = useRef<HTMLDivElement>(null)

  const handleLogout = (): void => {
    setMenuOpen(false)
    clearTokens()
    navigate('/')
  }

  const inactiveLink = `${linkBase} border-slate-300 bg-white text-slate-700 hover:bg-slate-50`
  const activeLink = `${linkBase} border-slate-800 bg-slate-100 font-medium text-slate-900`

  const studyBtnClass = `flex w-full items-center justify-between gap-2 rounded-lg px-3 py-2.5 text-left text-sm font-medium ${
    dueCount > 0
      ? 'bg-slate-900 text-white'
      : 'border border-slate-200 bg-slate-50 text-slate-800 hover:bg-slate-100'
  }`

  const studyBtnClassDesktop = `flex items-center gap-1.5 rounded-lg px-3 py-1.5 text-sm font-medium sm:px-4 sm:py-2 ${
    dueCount > 0
      ? 'bg-slate-900 text-white'
      : 'border border-slate-300 bg-white text-slate-600 hover:bg-slate-50'
  }`

  useEffect(() => {
    if (!menuOpen) return
    const onDoc = (e: MouseEvent) => {
      if (rootRef.current && !rootRef.current.contains(e.target as Node)) {
        setMenuOpen(false)
      }
    }
    const onKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape') setMenuOpen(false)
    }
    document.addEventListener('mousedown', onDoc)
    document.addEventListener('keydown', onKey)
    return () => {
      document.removeEventListener('mousedown', onDoc)
      document.removeEventListener('keydown', onKey)
    }
  }, [menuOpen])

  const goStudy = (): void => {
    setMenuOpen(false)
    navigate('/study')
  }

  return (
    <div ref={rootRef} className="relative">
      <div className="md:hidden">
        <button
          type="button"
          className="inline-flex h-10 w-10 items-center justify-center rounded-lg border border-slate-300 bg-white text-slate-700 shadow-sm transition hover:bg-slate-50 focus:outline-none focus:ring-2 focus:ring-slate-400/50"
          aria-expanded={menuOpen}
          aria-controls="logged-in-nav-menu"
          aria-label={menuOpen ? 'Закрыть меню' : 'Открыть меню'}
          onClick={() => setMenuOpen((v) => !v)}
        >
          <HamburgerIcon open={menuOpen} />
        </button>

        {menuOpen ? (
          <div
            id="logged-in-nav-menu"
            role="menu"
            className="absolute right-0 top-full z-50 mt-1.5 min-w-[13.5rem] max-w-[calc(100vw-2rem)] rounded-xl border border-slate-200 bg-white py-1 shadow-lg"
          >
            <button type="button" role="menuitem" className={studyBtnClass} onClick={goStudy}>
              <span>Учить</span>
              {dueCount > 0 ? (
                <span className="rounded-full bg-white px-2 py-0.5 text-xs font-bold text-slate-900">{dueCount}</span>
              ) : null}
            </button>
            <Link
              to="/decks/new"
              role="menuitem"
              className={`block px-3 py-2.5 text-sm font-medium ${
                highlight === 'new-deck' ? 'bg-slate-100 text-slate-900' : 'text-slate-800 hover:bg-slate-50'
              }`}
              onClick={() => setMenuOpen(false)}
            >
              Новая колода
            </Link>
            <Link
              to="/library"
              role="menuitem"
              className={`block px-3 py-2.5 text-sm font-medium ${
                highlight === 'library' ? 'bg-slate-100 text-slate-900' : 'text-slate-800 hover:bg-slate-50'
              }`}
              onClick={() => setMenuOpen(false)}
            >
              Библиотека
            </Link>
            <Link
              to="/profile"
              role="menuitem"
              className="block px-3 py-2.5 text-sm font-medium text-slate-800 hover:bg-slate-50"
              onClick={() => setMenuOpen(false)}
            >
              Профиль
            </Link>
            <div className="my-1 border-t border-slate-100" />
            <button
              type="button"
              role="menuitem"
              className="w-full px-3 py-2.5 text-left text-sm font-medium text-red-700 hover:bg-red-50"
              onClick={handleLogout}
            >
              Выйти
            </button>
          </div>
        ) : null}
      </div>

      <div className="hidden flex-wrap items-center gap-2 md:flex">
        <button
          type="button"
          className={studyBtnClassDesktop}
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
        <Link to="/decks/new" className={highlight === 'new-deck' ? activeLink : inactiveLink}>
          Новая колода
        </Link>
        <Link to="/library" className={highlight === 'library' ? activeLink : inactiveLink}>
          Библиотека
        </Link>
        <Link to="/profile" className={inactiveLink}>
          Профиль
        </Link>
        <button type="button" className={`${linkBase} border-slate-300 bg-white hover:bg-slate-50`} onClick={handleLogout}>
          Выйти
        </button>
      </div>
    </div>
  )
}
