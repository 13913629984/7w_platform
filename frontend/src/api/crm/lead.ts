import request from '@/api/request'

export interface LeadItem {
  id?: number
  name: string
  source?: string
  owner?: string
  status?: string
  followAt?: string
  remark?: string
  createTime?: string
  updateTime?: string
}

export interface LeadStats {
  total: number
  waiting: number
  following: number
  converted: number
  monthAdded: number
}

export interface LeadListResult {
  rows: LeadItem[]
  total: number
  page: number
  pageSize: number
  stats?: LeadStats
}

export interface LeadListQuery {
  keyword?: string
  status?: string
  source?: string
  owner?: string
  page?: number
  pageSize?: number
}

export interface LeadConvertPayload {
  id: number
  customerName?: string
  owner?: string
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

export function listLeads(params: LeadListQuery) {
  return unwrap<LeadListResult>(request.get('/crm/lead/list', { params }))
}

export function getLead(id: number) {
  return unwrap<LeadItem>(request.get(`/crm/lead/${id}`))
}

export function createLead(payload: LeadItem) {
  return unwrap<LeadItem>(request.post('/crm/lead/create', payload))
}

export function updateLead(payload: LeadItem) {
  return unwrap<LeadItem>(request.post('/crm/lead/update', payload))
}

export function deleteLead(id: number) {
  return unwrap<null>(request.post('/crm/lead/delete', { id }))
}

export function batchDeleteLeads(ids: number[]) {
  return unwrap<null>(request.post('/crm/lead/delete', { ids }))
}

export function convertLead(payload: LeadConvertPayload) {
  return unwrap<number>(request.post('/crm/lead/convert', payload))
}
