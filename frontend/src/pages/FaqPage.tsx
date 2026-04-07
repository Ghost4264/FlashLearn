import { useState } from 'react'
import { Link } from 'react-router-dom'
import { FAQ_ITEMS } from '../data/faq'

const rowTransition =
  'transition-[grid-template-rows] duration-300 ease-out motion-reduce:transition-none'

function Chevron({ open }: { open: boolean }) {
  return (
    <svg
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="2"
      aria-hidden
      className={`h-4 w-4 shrink-0 transition-transform duration-300 ease-out motion-reduce:transition-none ${open ? 'rotate-180' : ''}`}
    >
      <path d="M6 9l6 6 6-6" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  )
}

export function FaqPage() {
  const [openQuestion, setOpenQuestion] = useState<string | null>(null)

  return (
    <div className="relative min-h-screen overflow-x-hidden bg-slate-950">
      <img
        src="https://images.unsplash.com/photo-1456513080510-7bf3a84b82f8?auto=format&fit=crop&w=1600&q=80"
        alt=""
        className="absolute inset-0 h-full w-full object-cover opacity-30"
      />
      <div className="absolute inset-0 bg-gradient-to-b from-slate-950/70 via-slate-900/70 to-slate-950" />

      <div className="relative mx-auto max-w-2xl px-4 pb-24 pt-8 md:pt-12">
        <div className="mt-8 flex items-start justify-between gap-4">
          <h1 className="min-w-0 text-2xl font-bold leading-tight text-white md:text-3xl">
            Частые вопросы
          </h1>
          <Link
            to="/"
            className="shrink-0 rounded-lg bg-emerald-500 px-4 py-2 text-sm font-semibold text-white transition hover:bg-emerald-400 focus:outline-none focus:ring-2 focus:ring-emerald-300/80"
          >
            Назад
          </Link>
        </div>
        <p className="mt-2 text-sm text-slate-400">
          Интервальное повторение, SM-2 и сервис FlashLearn
        </p>

        <ul className="mt-8 space-y-2">
          {FAQ_ITEMS.map((item) => {
            const open = openQuestion === item.question
            return (
              <li key={item.question}>
                <div className="overflow-hidden rounded-xl border border-white/15 bg-white/5 transition-colors hover:bg-white/[0.07]">
                  <button
                    type="button"
                    onClick={() => setOpenQuestion((q) => (q === item.question ? null : item.question))}
                    aria-expanded={open}
                    className="flex w-full items-start gap-3 px-4 py-3.5 text-left text-sm font-medium text-slate-100 transition-colors hover:text-white focus:outline-none focus-visible:ring-2 focus-visible:ring-inset focus-visible:ring-emerald-500/40"
                  >
                    <span className="min-w-0 flex-1">{item.question}</span>
                    <Chevron open={open} />
                  </button>
                  <div
                    className={`grid ${rowTransition} ${open ? '[grid-template-rows:1fr]' : '[grid-template-rows:0fr]'}`}
                  >
                    <div className="overflow-hidden min-h-0">
                      <div className="border-t border-white/10 px-4 pb-4 pt-1 text-sm leading-relaxed text-slate-300">
                        {item.answer}
                      </div>
                    </div>
                  </div>
                </div>
              </li>
            )
          })}
        </ul>
      </div>
    </div>
  )
}
