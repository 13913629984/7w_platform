import request from '@/api/request'

export interface DealItem {
  id?: number
  name: string
  customerId?: number | null
  customerName: string
  leadName?: string
  amount: number
  stage: string
  winRate?: number | null
  owner: string
  expectDealAt: string
  status?: string
  remark?: string
  createTime?: string
  updateTime?: string
}

export interface DealStats {
  total: number
  monthAdded: number
  totalAmount: string
  avgWinRate: number
}

export interface DealListResult {
  rows: DealItem[]
  total: number
  page: number
  pageSize: number
  stats?: DealStats
  stages?: string[]
}

export interface DealListQuery {
  keyword?: string
  stage?: string
  owner?: string
  page?: number
  pageSize?: number
}

export interface CustomerOption {
  id: number
  name: string
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

export function listDeals(params: DealListQuery) {
  return unwrap<DealListResult>(request.get('/crm/deal/list', { params }))
}

export function listDealCustomers() {
  return unwrap<CustomerOption[]>(request.get('/crm/deal/customers'))
}

export function getDeal(id: number) {
  return unwrap<DealItem>(request.get(`/crm/deal/${id}`))
}

export function createDeal(payload: DealItem) {
  return unwrap<DealItem>(request.post('/crm/deal/create', payload))
}

export function updateDeal(payload: DealItem) {
  return unwrap<DealItem>(request.post('/crm/deal/update', payload))
}

export function deleteDeal(id: number) {
  return unwrap<null>(request.post('/crm/deal/delete', { id }))
}

export function batchDeleteDeals(ids: number[]) {
  return unwrap<number>(request.post('/crm/deal/batch-delete', { ids }))
}

export function convertDeal(id: number) {
  return unwrap<DealItem>(request.post(`/crm/deal/convert/${id}`))
}
