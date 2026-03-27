import { create } from 'zustand'

export type ToastVariant = 'success' | 'error' | 'info'

export type ToastItem = { id: number; message: string; variant: ToastVariant }

type ToastState = {
  toasts: ToastItem[]
  show: (message: string, variant?: ToastVariant) => number
  dismiss: (id: number) => void
}

let idSeq = 1

export const useToastStore = create<ToastState>((set, get) => ({
  toasts: [],
  show: (message, variant = 'info') => {
    const id = idSeq++
    set((s) => {
      const next = [...s.toasts, { id, message, variant }]
      return { toasts: next.length > 5 ? next.slice(-5) : next }
    })
    window.setTimeout(() => get().dismiss(id), 4200)
    return id
  },
  dismiss: (id) => set((s) => ({ toasts: s.toasts.filter((t) => t.id !== id) })),
}))

export const toast = {
  success: (message: string) => useToastStore.getState().show(message, 'success'),
  error: (message: string) => useToastStore.getState().show(message, 'error'),
  info: (message: string) => useToastStore.getState().show(message, 'info'),
}
