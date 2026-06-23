import request from '@/api/request'

export interface BrandItem {
  id?: number
  code: string
  name: string
  englishName?: string
  logo?: string
  country?: string
  supplier?: string
  contact?: string
  phone?: string
  description?: string
  sort?: number
  status?: number
  statusName?: string
  materialCount?: number
  createTime?: string
  updateTime?: string
}

export interface BrandStats {
  total: number
  activeCount: number
  inactiveCount: number
  materialTotal: number
}

export interface BrandQuery {
  keyword?: string
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

export function listBrands(params: BrandQuery = {}) {
  return unwrap<BrandItem[]>(request.get('/wms/brand/list', { params }))
}

export function getBrandStats() {
  return unwrap<BrandStats>(request.get('/wms/brand/stats'))
}

export function getBrand(id: number) {
  return unwrap<BrandItem>(request.get(`/wms/brand/${id}`))
}

export function createBrand(payload: BrandItem) {
  return unwrap<BrandItem>(request.post('/wms/brand/create', payload))
}

export function updateBrand(payload: BrandItem) {
  return unwrap<BrandItem>(request.post('/wms/brand/update', payload))
}

export function deleteBrand(id: number) {
  return unwrap<null>(request.post('/wms/brand/delete', { id }))
}

export function changeBrandStatus(id: number, status: number) {
  return unwrap<BrandItem>(request.post('/wms/brand/status', { id, status }))
}