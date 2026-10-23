import request from '@/api/request'

export interface ActivityItem {
  id?: number
  title: string
  type?: string
  customerId?: number | null
  customerName?: string
  owner?: string
  activityAt?: string
  status?: string
  remark?: string
  createTime?: string
}

export interface ActivityStats {
  total: number
  pending: number
  done: number
  todayCount: number
  customerCount: number
}

export interface ActivityListResult {
  rows: ActivityItem[]
  total: number
  page: number
  pageSize: number
  stats?: ActivityStats
}

export interface ActivityListQuery {
  keyword?: string
  status?: string
  type?: string
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

export function listActivities(params: ActivityListQuery) {
  return unwrap<ActivityListResult>(request.get('/crm/activity/list', { params }))
}

export function listActivityCustomers() {
  return unwrap<CustomerOption[]>(request.get('/crm/activity/customers'))
}

export function getActivity(id: number) {
  return unwrap<ActivityItem>(request.get(`/crm/activity/${id}`))
}

export function createActivity(payload: ActivityItem) {
  return unwrap<ActivityItem>(request.post('/crm/activity/create', payload))
}

export function updateActivity(payload: ActivityItem) {
  return unwrap<ActivityItem>(request.post('/crm/activity/update', payload))
}

export function deleteActivity(id: number) {
  return unwrap<null>(request.post('/crm/activity/delete', { id }))
}

export function batchDeleteActivities(ids: number[]) {
  return unwrap<null>(request.post('/crm/activity/delete', { ids }))
}
