export type AuthResponse = {
  accessToken: string
  refreshToken: string
  tokenType: string
}

export type Deck = {
  id: number
  title: string
  description: string
  cardCount: number
  dueCardCount: number
  public: boolean
}

export type PageResponse<T> = {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
  last: boolean
}
