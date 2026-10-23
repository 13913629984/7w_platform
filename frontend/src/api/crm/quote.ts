import request from '@/api/request'

export interface QuoteItem {
  id?: number
  customerName: string
  brand: string
  model: string
  price: number
  visitSubject: string
  createTime?: string
}

export interface BrandDistribution {
  brand: string
  count: number
}

export interface QuoteStats {
  total: number
  customerCount: number
  brandCount: number
  totalAmount: number
  avgPrice: number
  monthAdded: number
  brandDistribution?: BrandDistribution[]
}

export interface QuoteListResult {
  rows: QuoteItem[]
  total: number
  page: number
  pageSize: number
  stats?: QuoteStats
}

export interface QuoteListQuery {
  keyword?: string
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

export function getQuoteStats() {
  return unwrap<QuoteStats>(request.get('/crm/quote/stats'))
}

export function listQuoteBrands() {
  return unwrap<string[]>(request.get('/crm/quote/brands'))
}

export function getQuote(id: number) {
  return unwrap<QuoteItem>(request.get(`/crm/quote/${id}`))
}

export function createQuote(payload: QuoteItem) {
  return unwrap<QuoteItem>(request.post('/crm/quote/create', payload))
}

export function updateQuote(payload: QuoteItem) {
  return unwrap<QuoteItem>(request.post('/crm/quote/update', payload))
}

export function deleteQuote(id: number) {
  return unwrap<null>(request.post('/crm/quote/delete', { id }))
}

export function batchDeleteQuotes(ids: number[]) {
  return unwrap<null>(request.post('/crm/quote/delete', { ids }))
}
