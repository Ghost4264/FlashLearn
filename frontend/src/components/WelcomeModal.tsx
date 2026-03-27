import { useAuthStore } from '../store/authStore'

export function WelcomeModal() {
  const welcomePending = useAuthStore((s) => s.welcomePending)
  const clearWelcome = useAuthStore((s) => s.clearWelcome)

  if (!welcomePending) return null

  const dismiss = (): void => {
    clearWelcome()
  }

  return (
    <div
      className="fixed inset-0 z-[60] flex items-center justify-center bg-slate-900/45 p-4 backdrop-blur-[2px]"
      role="dialog"
      aria-modal="true"
      aria-labelledby="welcome-modal-title"
      onClick={dismiss}
    >
      <div
        className="w-full max-w-sm rounded-2xl border border-slate-200 bg-white p-5 shadow-xl"
        onClick={(e) => e.stopPropagation()}
      >
        <h2 id="welcome-modal-title" className="text-lg font-semibold text-slate-900">
          Рады видеть вас
        </h2>
        <p className="mt-2 text-sm leading-relaxed text-slate-600">
          Короткие повторения вовремя держат в памяти лучше, чем редкие долгие зубрёжки.
        </p>
        <p className="mt-3 text-sm leading-relaxed text-slate-600">
          Если FlashLearn вам полезен, можно{' '}
          <a
            href="https://www.tbank.ru/cf/8u3qgXEiYmY"
            target="_blank"
            rel="noreferrer"
            className="font-medium text-slate-900 underline decoration-slate-300 underline-offset-2 transition hover:decoration-slate-600"
            onClick={(e) => e.stopPropagation()}
          >
            поддержать проект
          </a>
          — так проще развивать сервис и держать его доступным.
        </p>
        <button
          type="button"
          onClick={dismiss}
          className="mt-5 w-full rounded-xl bg-slate-900 py-2.5 text-sm font-medium text-white transition hover:bg-slate-800"
        >
          Продолжить
        </button>
      </div>
    </div>
  )
}
