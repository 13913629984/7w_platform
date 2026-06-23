import request from '@/api/request'

export interface CustomerSales {
  id?: number
  name: string
  position: string
  isOwner: boolean
}

export interface CustomerContact {
  id?: number
  name: string
  phone: string
  title: string
}

export interface CustomerItem {
  id?: number
  name: string
  englishName: string
  address: string
  level: string
  owner: string
  lastVisitAt: string
  lastDealAt: string
  remark?: string
  status?: number
  createTime?: string
  updateTime?: string
  salesList: CustomerSales[]
  contactList: CustomerContact[]
}

export interface CustomerStats {
  total: number
  active: number
  monthAdded: number
  conversionRate: number
  byLevel: Record<string, string>
}

export interface CustomerListResult {
  rows: CustomerItem[]
  total: number
  page: number
  pageSize: number
  stats?: CustomerStats
  levels?: string[]
}

export interface CustomerListQuery {
  name?: string
  level?: string
  owner?: string
  onlyPublic?: boolean
  page?: number
  pageSize?: number
}

export interface CustomerImportResult {
  successCount: number
  failedCount: number
  failedNames: string[]
  items: CustomerItem[]
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

export function listCustomers(params: CustomerListQuery) {
  return unwrap<CustomerListResult>(request.get('/crm/customer/list', { params }))
}

export function getCustomer(id: number) {
  return unwrap<CustomerItem>(request.get(`/crm/customer/${id}`))
}

export function createCustomer(payload: CustomerItem) {
  return unwrap<CustomerItem>(request.post('/crm/customer/create', payload))
}

export function updateCustomer(payload: CustomerItem) {
  return unwrap<CustomerItem>(request.post('/crm/customer/update', payload))
}

export function deleteCustomer(id: number) {
  return unwrap<null>(request.post('/crm/customer/delete', { id }))
}

export function batchDeleteCustomers(ids: number[]) {
  return unwrap<number>(request.post('/crm/customer/batch-delete', { ids }))
}

export function transferCustomers(ids: number[], owner: string) {
  return unwrap<number>(request.post('/crm/customer/transfer', { ids, owner }))
}

export function importCustomers(items: CustomerItem[]) {
  return unwrap<CustomerImportResult>(request.post('/crm/customer/import', { items }))
}
