import request from '@/api/request'

export interface QuoteItem {
  customerName: string
  brand: string
  model: string
  price: number
  visitSubject: string
}

export interface QuoteListResult {
  rows: QuoteItem[]
  total: number
  page: number
  pageSize: number
}

export interface QuoteListQuery {
  customerName?: string
  brand?: string
  model?: string
  page?: number
  pageSize?: number
}

export interface ApiEnvelope<T> {
  code: number
  message: string
  data: T
}

async function unwrap<T>(promise: Promise<{ data: ApiEnvelope<T> }>): Promise<ApiEnvelope<T>> {
  const response = await promise
  return response.data
}

export function listQuotes(params: QuoteListQuery) {
  return unwrap<QuoteListResult>(request.get('/crm/quote/list', { params }))
}
