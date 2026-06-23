import request from '@/api/request'

export interface MaterialCategory {
  value: string
  label: string
}

export interface MaterialItem {
  __statusLoading?: boolean
  id?: number
  sku: string
  name: string
  spec?: string
  category: string
  categoryName?: string
  brand?: string
  unit: string
  unitPrice?: number
  safeStock?: number
  supplier?: string
  barcode?: string
  shelfLifeDays?: number
  weight?: number
  volume?: number
  remark?: string
  status?: number
  statusName?: string
  createTime?: string
  updateTime?: string
}

export interface MaterialStats {
  total: number
  activeCount: number
  inactiveCount: number
  brandCount: number
  byCategory: Record<string, string>
}

export interface MaterialListResult {
  rows: MaterialItem[]
  total: number
  page: number
  pageSize: number
  stats?: MaterialStats
  categories?: MaterialCategory[]
}

export interface MaterialListQuery {
  keyword?: string
  category?: string
  status?: number | null
  page?: number
  pageSize?: number
}

export interface MaterialImportResult {
  successCount: number
  failedCount: number
  failedSkus: string[]
  items: MaterialItem[]
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

export function listMaterials(params: MaterialListQuery) {
  return unwrap<MaterialListResult>(request.get('/wms/material/list', { params }))
}

export function exportMaterials(params: MaterialListQuery) {
  return unwrap<MaterialListResult>(request.get('/wms/material/export', { params }))
}

export function getMaterial(id: number) {
  return unwrap<MaterialItem>(request.get(`/wms/material/${id}`))
}

export function createMaterial(payload: MaterialItem) {
  return unwrap<MaterialItem>(request.post('/wms/material/create', payload))
}

export function updateMaterial(payload: MaterialItem) {
  return unwrap<MaterialItem>(request.post('/wms/material/update', payload))
}

export function deleteMaterial(id: number) {
  return unwrap<null>(request.post('/wms/material/delete', { id }))
}

export function batchDeleteMaterials(ids: number[]) {
  return unwrap<number>(request.post('/wms/material/batch-delete', { ids }))
}

export function changeMaterialStatus(id: number, status: number) {
  return unwrap<MaterialItem>(request.post('/wms/material/status', { id, status }))
}

export function importMaterials(items: MaterialItem[]) {
  return unwrap<MaterialImportResult>(request.post('/wms/material/import', { items }))
}
