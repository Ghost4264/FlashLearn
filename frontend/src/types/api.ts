export type AuthResponse = {
  accessToken: string
  refreshToken: string
  tokenType: string
}

export type Category = {
  id: number
  name: string
}

export type Deck = {
  id: number
  title: string
  description: string
  cardCount: number
  dueCardCount: number
  public: boolean
  categoryId: number | null
  categoryName: string | null
}

export type PageResponse<T> = {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
  last: boolean
}

export type Card = {
  id: number
  deckId: number
  front: string
  back: string
  hint: string | null
  position: number
  createdAt: string
  updatedAt: string
}

export type StudyCard = Card & {
  isNew: boolean
}

export type ReviewResponse = {
  cardId: number
  intervalDays: number
  easeFactor: number
  nextReviewAt: string
}

export type UserProfile = {
  id: number
  email: string
  name: string | null
  role: string
  createdAt: string
  updatedAt: string
}

export type StudySettings = {
  newCardsPerSession: number
  intervalModifier: number
}

export type AdminDeckImportResponse = {
  deck: Deck
  importedCards: number
}

export type AdminBulkDeckResponse = {
  decksCreated: number
  cardsCreated: number
}
