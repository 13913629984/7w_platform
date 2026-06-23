import request from '@/api/request'

export interface LocationItem {
  id?: number
  code: string
  name: string
  warehouseCode: string
  warehouseName?: string
  area?: string
  shelf?: string
  layer?: string
  position?: string
  type: string
  typeName?: string
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

export interface LocationStats {
  total: number
  activeCount: number
  inactiveCount: number
  occupiedCount: number
  emptyCount: number
  totalCapacity: number
  usedCapacity: number
  usageRate: number
}

export interface LocationQuery {
  keyword?: string
  warehouseCode?: string
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

export function listLocations(params: LocationQuery = {}) {
  return unwrap<LocationItem[]>(request.get('/wms/location/list', { params }))
}

export function getLocationStats() {
  return unwrap<LocationStats>(request.get('/wms/location/stats'))
}

export function getLocation(id: number) {
  return unwrap<LocationItem>(request.get(`/wms/location/${id}`))
}

export function createLocation(payload: LocationItem) {
  return unwrap<LocationItem>(request.post('/wms/location/create', payload))
}

export function updateLocation(payload: LocationItem) {
  return unwrap<LocationItem>(request.post('/wms/location/update', payload))
}

export function deleteLocation(id: number) {
  return unwrap<null>(request.post('/wms/location/delete', { id }))
}

export function changeLocationStatus(id: number, status: number) {
  return unwrap<LocationItem>(request.post('/wms/location/status', { id, status }))
}