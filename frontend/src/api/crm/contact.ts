import request from '@/api/request'

export interface ContactItem {
  id?: number
  name: string
  title: string
  customerId?: number | null
  customerName: string
  phone: string
  email: string
  isPrimary: boolean
  remark?: string
  createTime?: string
  updateTime?: string
}

export interface ContactStats {
  total: number
  primary: number
  monthAdded: number
  customerCount: number
}

export interface ContactListResult {
  rows: ContactItem[]
  total: number
  page: number
  pageSize: number
  stats?: ContactStats
}

export interface ContactListQuery {
  keyword?: string
  customerId?: number
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

export function listContacts(params: ContactListQuery) {
  return unwrap<ContactListResult>(request.get('/crm/contact/list', { params }))
}

export function listContactCustomers() {
  return unwrap<CustomerOption[]>(request.get('/crm/contact/customers'))
}

export function getContact(id: number) {
  return unwrap<ContactItem>(request.get(`/crm/contact/${id}`))
}

export function createContact(payload: ContactItem) {
  return unwrap<ContactItem>(request.post('/crm/contact/create', payload))
}

export function updateContact(payload: ContactItem) {
  return unwrap<ContactItem>(request.post('/crm/contact/update', payload))
}

export function deleteContact(id: number) {
  return unwrap<null>(request.post('/crm/contact/delete', { id }))
}

export function batchDeleteContacts(ids: number[]) {
  return unwrap<number>(request.post('/crm/contact/batch-delete', { ids }))
}
