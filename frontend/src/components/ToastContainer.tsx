import { useToastStore } from '../store/toastStore'

export function ToastContainer() {
  const toasts = useToastStore((s) => s.toasts)
  const dismiss = useToastStore((s) => s.dismiss)

  if (toasts.length === 0) return null

  return (
    <div
      className="pointer-events-none fixed bottom-0 left-0 right-0 z-[100] flex max-h-[40vh] flex-col-reverse gap-2 overflow-hidden p-4 sm:bottom-4 sm:left-auto sm:right-4 sm:max-w-md sm:flex-col sm:p-0"
      aria-live="polite"
      aria-relevant="additions text"
    >
      {toasts.map((t) => (
        <div
          key={t.id}
          role="status"
          className={`pointer-events-auto flex items-start gap-3 rounded-xl border px-4 py-3 text-sm shadow-lg ${
            t.variant === 'success'
              ? 'border-emerald-200 bg-emerald-50 text-emerald-950'
              : t.variant === 'error'
                ? 'border-red-200 bg-red-50 text-red-950'
                : 'border-slate-200 bg-white text-slate-900'
          }`}
        >
          <p className="min-w-0 flex-1 leading-snug">{t.message}</p>
          <button
            type="button"
            className="shrink-0 rounded p-0.5 text-current opacity-60 hover:opacity-100"
            onClick={() => dismiss(t.id)}
            aria-label="Закрыть"
          >
            ×
          </button>
        </div>
      ))}
    </div>
  )
}
