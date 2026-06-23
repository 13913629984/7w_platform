import request from '@/api/request'

export interface WarehouseItem {
  id?: number
  code: string
  name: string
  type: string
  typeName?: string
  address?: string
  manager?: string
  phone?: string
  area?: number
  capacity?: number
  usedCapacity?: number
  usageRate?: number
  description?: string
  sort?: number
  status?: number
  statusName?: string
  createTime?: string
  updateTime?: string
}

export interface WarehouseStats {
  total: number
  activeCount: number
  inactiveCount: number
  totalCapacity: number
  usedCapacity: number
  usageRate: number
}

export interface WarehouseQuery {
  keyword?: string
  type?: string
  status?: number | null
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

export function listWarehouses(params: WarehouseQuery = {}) {
  return unwrap<WarehouseItem[]>(request.get('/wms/warehouse/list', { params }))
}

export function getWarehouseStats() {
  return unwrap<WarehouseStats>(request.get('/wms/warehouse/stats'))
}

export function getWarehouse(id: number) {
  return unwrap<WarehouseItem>(request.get(`/wms/warehouse/${id}`))
}

export function createWarehouse(payload: WarehouseItem) {
  return unwrap<WarehouseItem>(request.post('/wms/warehouse/create', payload))
}

export function updateWarehouse(payload: WarehouseItem) {
  return unwrap<WarehouseItem>(request.post('/wms/warehouse/update', payload))
}

export function deleteWarehouse(id: number) {
  return unwrap<null>(request.post('/wms/warehouse/delete', { id }))
}

export function changeWarehouseStatus(id: number, status: number) {
  return unwrap<WarehouseItem>(request.post('/wms/warehouse/status', { id, status }))
}