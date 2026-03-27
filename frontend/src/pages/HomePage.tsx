import { Link, useNavigate } from 'react-router-dom'

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
          Колоды, алгоритм SM-2 и удобный прогресс в одном месте. Учись регулярно и не забывай важное.
        </p>

        <div className="mt-8 flex flex-wrap items-center justify-center gap-3">
          <button
            onClick={() => navigate('/login')}
            className="rounded-xl bg-emerald-500 px-8 py-3 text-base font-semibold text-white transition hover:bg-emerald-400"
          >
            Войти
          </button>
          <Link
            to="/register"
            className="rounded-xl border border-sky-200/70 bg-sky-100/90 px-5 py-3 text-sm font-semibold text-sky-950 shadow-sm transition hover:-translate-y-0.5 hover:bg-white hover:shadow-md focus:outline-none focus:ring-2 focus:ring-sky-300/80"
          >
            Начать бесплатно
          </Link>
        </div>

        <div className="mt-8 grid w-full max-w-3xl grid-cols-1 gap-3 sm:grid-cols-3">
          <div className="rounded-xl border border-white/15 bg-white/10 px-4 py-3">
            <p className="text-sm font-semibold text-white">SM-2 алгоритм</p>
            <p className="mt-1 text-xs text-slate-200">Повторяй карточки в нужный момент.</p>
          </div>
          <div className="rounded-xl border border-white/15 bg-white/10 px-4 py-3">
            <p className="text-sm font-semibold text-white">Публичные колоды</p>
            <p className="mt-1 text-xs text-slate-200">Выбирай готовые темы и учись быстрее.</p>
          </div>
          <div className="rounded-xl border border-white/15 bg-white/10 px-4 py-3">
            <p className="text-sm font-semibold text-white">CSV импорт</p>
            <p className="mt-1 text-xs text-slate-200">Загружай свои колоды в пару кликов.</p>
          </div>
        </div>

        <p className="mt-4 text-xs text-slate-300">Open-source проект с активной разработкой на GitHub.</p>

      </div>

      <div className="absolute inset-x-0 bottom-0 border-t border-white/10 bg-slate-950/65">
        <div className="mx-auto flex h-10 max-w-5xl items-center justify-center gap-3 px-4 text-xs text-slate-300">
          <a
            href="https://github.com/Ghost4264/FlashLearn"
            target="_blank"
            rel="noreferrer"
            className="inline-flex items-center gap-1.5 font-medium text-slate-200 transition hover:text-white hover:underline hover:underline-offset-4 focus:outline-none focus:ring-2 focus:ring-emerald-400/80"
          >
            <svg viewBox="0 0 24 24" aria-hidden="true" className="h-3.5 w-3.5 fill-current">
              <path d="M12 2C6.48 2 2 6.58 2 12.23c0 4.52 2.87 8.35 6.84 9.7.5.1.68-.22.68-.49 0-.24-.01-.88-.01-1.73-2.78.62-3.37-1.37-3.37-1.37-.45-1.18-1.11-1.49-1.11-1.49-.91-.64.07-.63.07-.63 1 .08 1.53 1.05 1.53 1.05.9 1.56 2.35 1.11 2.92.85.09-.67.35-1.11.64-1.37-2.22-.26-4.56-1.14-4.56-5.07 0-1.12.39-2.04 1.03-2.76-.1-.26-.45-1.3.1-2.72 0 0 .84-.28 2.75 1.05A9.3 9.3 0 0 1 12 6.84c.85 0 1.7.12 2.5.35 1.9-1.33 2.74-1.05 2.74-1.05.55 1.42.2 2.46.1 2.72.64.72 1.03 1.64 1.03 2.76 0 3.94-2.34 4.8-4.58 5.06.36.32.69.94.69 1.9 0 1.37-.01 2.48-.01 2.82 0 .27.18.6.69.49A10.26 10.26 0 0 0 22 12.23C22 6.58 17.52 2 12 2Z" />
            </svg>
            GitHub
          </a>

          <span className="text-slate-500">•</span>

          <a
            href="https://github.com/Ghost4264/FlashLearn/issues/new"
            target="_blank"
            rel="noreferrer"
            className="font-medium text-slate-200 transition hover:text-white hover:underline hover:underline-offset-4 focus:outline-none focus:ring-2 focus:ring-emerald-400/80"
          >
            Сообщить о баге
          </a>

          <span className="text-slate-500">•</span>

          <a
            href="https://www.tbank.ru/cf/8u3qgXEiYmY"
            target="_blank"
            rel="noreferrer"
            className="font-medium text-slate-200 transition hover:text-white hover:underline hover:underline-offset-4 focus:outline-none focus:ring-2 focus:ring-emerald-400/80"
          >
            Поддержать проект
          </a>
        </div>
      </div>
    </div>
  )
}
