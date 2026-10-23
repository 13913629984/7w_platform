import request from '@/api/request'

export interface EquipmentItem {
  id?: number
  code: string
  name: string
  model?: string
  brand?: string
  category?: string
  customerId?: number | null
  customerName?: string
  location?: string
  installDate?: string
  warrantyEnd?: string
  status?: string
  manager?: string
  phone?: string
  purchasePrice?: number | null
  lastMaintainDate?: string
  nextMaintainDate?: string
  remark?: string
  createTime?: string
  updateTime?: string
}

export interface EquipmentStats {
  total: number
  runningCount: number
  idleCount: number
  repairCount: number
  scrappedCount: number
  warrantyExpiringCount: number
  maintainDueCount: number
}

export interface EquipmentQuery {
  keyword?: string
  status?: string
  category?: string
  customerName?: string
  page?: number
  pageSize?: number
}

/** 设备下拉选项（/list 之外的专用接口） */
export interface EquipmentOption {
  id: number
  code: string
  name: string
  customerName?: string
}

export interface MaintListResult<T> {
  rows: T[]
  total: number
  page: number
  pageSize: number
  stats?: Record<string, unknown>
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

export function listEquipments(params: EquipmentQuery = {}) {
  return unwrap<MaintListResult<EquipmentItem>>(request.get('/maint/equipment/list', { params }))
}

export function getEquipmentStats() {
  return unwrap<EquipmentStats>(request.get('/maint/equipment/stats'))
}

export function getEquipmentOptions() {
  return unwrap<EquipmentOption[]>(request.get('/maint/equipment/options'))
}

export function getEquipment(id: number) {
  return unwrap<EquipmentItem>(request.get(`/maint/equipment/${id}`))
}

export function createEquipment(payload: EquipmentItem) {
  return unwrap<EquipmentItem>(request.post('/maint/equipment/create', payload))
}

export function updateEquipment(payload: EquipmentItem) {
  return unwrap<EquipmentItem>(request.post('/maint/equipment/update', payload))
}

export function deleteEquipment(id: number) {
  return unwrap<null>(request.post('/maint/equipment/delete', { id }))
}
