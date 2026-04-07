import { Link } from 'react-router-dom'

/**
 * Возврат на список «Мои колоды» — единый стиль на вложенных страницах.
 */
export function BackToDecksLink() {
  return (
    <Link
      to="/decks"
      className="inline-flex items-center justify-center rounded-lg border border-slate-300 bg-white px-3 py-1.5 text-sm font-medium text-slate-700 shadow-sm transition hover:border-slate-400 hover:bg-slate-50 focus:outline-none focus:ring-2 focus:ring-slate-400/40"
    >
      Назад
    </Link>
  )
}
