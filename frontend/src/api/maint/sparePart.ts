import request from '@/api/request'
import type { ApiEnvelope, MaintListResult } from './equipment'

export type { ApiEnvelope, MaintListResult }

export interface SparePartItem {
  id?: number
  code?: string
  name: string
  spec?: string
  unit?: string
  category?: string
  warehouseCode?: string
  stockQty?: number | null
  safetyQty?: number | null
  price?: number | null
  supplier?: string
  status?: number | null
  remark?: string
  /** 后端计算：库存量 < 安全库存 */
  lowStock?: boolean
  createTime?: string
  updateTime?: string
}

export interface SparePartStats {
  total: number
  categoryCount: number
  lowStockCount: number
  stockValue: number
  disabledCount: number
}

export interface SparePartQuery {
  keyword?: string
  category?: string
  warehouseCode?: string
  lowStockOnly?: boolean
  page?: number
  pageSize?: number
}

async function unwrap<T>(promise: Promise<{ data: ApiEnvelope<T> }>): Promise<ApiEnvelope<T>> {
  const response = await promise
  return response.data
}

export function listSpareParts(params: SparePartQuery = {}) {
  return unwrap<MaintListResult<SparePartItem>>(request.get('/maint/spare-part/list', { params }))
}

export function getSparePartStats() {
  return unwrap<SparePartStats>(request.get('/maint/spare-part/stats'))
}

export function getSparePart(id: number) {
  return unwrap<SparePartItem>(request.get(`/maint/spare-part/${id}`))
}

export function createSparePart(payload: SparePartItem) {
  return unwrap<SparePartItem>(request.post('/maint/spare-part/create', payload))
}

export function updateSparePart(payload: SparePartItem) {
  return unwrap<SparePartItem>(request.post('/maint/spare-part/update', payload))
}

export function deleteSparePart(id: number) {
  return unwrap<null>(request.post('/maint/spare-part/delete', { id }))
}
