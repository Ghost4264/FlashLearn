import { useNavigate } from 'react-router-dom'

export function HomePage() {
  const navigate = useNavigate()

  return (
    <div className="relative min-h-screen overflow-hidden bg-slate-950">
      <img
        src="https://images.unsplash.com/photo-1456513080510-7bf3a84b82f8?auto=format&fit=crop&w=1600&q=80"
        alt="Книги и обучение"
        className="absolute inset-0 h-full w-full object-cover opacity-30"
      />
      <div className="absolute inset-0 bg-gradient-to-b from-slate-950/70 via-slate-900/70 to-slate-950" />

      <div className="relative mx-auto flex min-h-screen max-w-5xl flex-col items-center justify-center px-6 pb-16 text-center">
        <p className="mb-3 text-xs uppercase tracking-[0.2em] text-slate-300">FlashLearn</p>
        <h1 className="max-w-3xl text-4xl font-bold text-white md:text-5xl">
          Запоминай быстрее с интервальными повторениями
        </h1>
        <p className="mt-4 max-w-2xl text-sm text-slate-200 md:text-base">
          Создавай колоды, учись по алгоритму и следи за прогрессом в удобном формате.
        </p>

        <button
          onClick={() => navigate('/login')}
          className="mt-8 rounded-xl bg-emerald-500 px-8 py-3 text-base font-semibold text-white transition hover:bg-emerald-400"
        >
          Войти
        </button>

      </div>

      <div className="absolute inset-x-0 bottom-0 border-t border-white/15 bg-black">
        <div className="mx-auto flex h-6 max-w-5xl items-center justify-center px-4">
          <a
            href="https://github.com/Ghost4264/FlashLearn"
            target="_blank"
            rel="noreferrer"
            className="text-xs text-white/95 underline underline-offset-2 transition hover:text-white"
          >
            GitHub
          </a>
        </div>
      </div>
    </div>
  )
}
