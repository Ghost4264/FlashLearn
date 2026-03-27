import { api } from './api'

export async function downloadPersonalDeckCsv(deckId: number, titleHint?: string): Promise<void> {
  const { data } = await api.get<Blob>(`/api/decks/${deckId}/export/csv`, {
    responseType: 'blob',
  })
  const blob = data instanceof Blob ? data : new Blob([data], { type: 'text/csv;charset=utf-8' })
  const safe = (titleHint ?? `deck-${deckId}`)
    .replace(/[/\\?%*:|"<>]/g, '-')
    .slice(0, 80)
  const a = document.createElement('a')
  a.href = URL.createObjectURL(blob)
  a.download = `${safe}.csv`
  a.click()
  URL.revokeObjectURL(a.href)
}
