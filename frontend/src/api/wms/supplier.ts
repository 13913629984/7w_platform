import request from '@/api/request'

export interface SupplierItem {
  id?: number
  code: string
  name: string
  shortName?: string
  category?: string
  categoryName?: string
  contact?: string
  phone?: string
  email?: string
  address?: string
  taxNo?: string
  bankAccount?: string
  settleType?: string
  description?: string
  sort?: number
  status?: number
  statusName?: string
  materialCount?: number
  createTime?: string
  updateTime?: string
}

export interface SupplierStats {
  total: number
  activeCount: number
  inactiveCount: number
  materialTotal: number
}

export interface SupplierQuery {
  keyword?: string
  category?: string
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

export function listSuppliers(params: SupplierQuery = {}) {
  return unwrap<SupplierItem[]>(request.get('/wms/supplier/list', { params }))
}

export function getSupplierStats() {
  return unwrap<SupplierStats>(request.get('/wms/supplier/stats'))
}

export function getSupplier(id: number) {
  return unwrap<SupplierItem>(request.get(`/wms/supplier/${id}`))
}

export function createSupplier(payload: SupplierItem) {
  return unwrap<SupplierItem>(request.post('/wms/supplier/create', payload))
}

export function updateSupplier(payload: SupplierItem) {
  return unwrap<SupplierItem>(request.post('/wms/supplier/update', payload))
}

export function deleteSupplier(id: number) {
  return unwrap<null>(request.post('/wms/supplier/delete', { id }))
}

export function changeSupplierStatus(id: number, status: number) {
  return unwrap<SupplierItem>(request.post('/wms/supplier/status', { id, status }))
}
