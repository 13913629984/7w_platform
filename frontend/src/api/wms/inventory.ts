import request from '@/api/request'

export interface InventoryItem {
  id?: number
  materialId: number
  sku?: string
  materialName?: string
  spec?: string
  unit?: string
  category?: string
  brand?: string
  warehouseCode: string
  warehouseName?: string
  locationCode: string
  locationName?: string
  batchNo?: string
  quantity?: number
  availableQty?: number
  lockedQty?: number
  safeStock?: number
  stockStatus?: string
  stockStatusName?: string
  productionDate?: string
  expireDate?: string
  remark?: string
  status?: number
  statusName?: string
  createTime?: string
  updateTime?: string
}

export interface InventoryStats {
  totalItems: number
  materialCount: number
  totalQty: number
  availableQty: number
  lockedQty: number
  lowCount: number
  emptyCount: number
  activeCount: number
}

export interface InventoryQuery {
  keyword?: string
  warehouseCode?: string
  locationCode?: string
  stockStatus?: string
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

export function listInventories(params: InventoryQuery = {}) {
  return unwrap<InventoryItem[]>(request.get('/wms/inventory/list', { params }))
}

export function getInventoryStats() {
  return unwrap<InventoryStats>(request.get('/wms/inventory/stats'))
}

export function getInventory(id: number) {
  return unwrap<InventoryItem>(request.get(`/wms/inventory/${id}`))
}

export function createInventory(payload: InventoryItem) {
  return unwrap<InventoryItem>(request.post('/wms/inventory/create', payload))
}

export function updateInventory(payload: InventoryItem) {
  return unwrap<InventoryItem>(request.post('/wms/inventory/update', payload))
}

export function deleteInventory(id: number) {
  return unwrap<null>(request.post('/wms/inventory/delete', { id }))
}

export function changeInventoryStatus(id: number, status: number) {
  return unwrap<InventoryItem>(request.post('/wms/inventory/status', { id, status }))
}