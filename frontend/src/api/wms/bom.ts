import request from '@/api/request'

export type MaterialType = 'MAIN' | 'BACKUP'

export interface BomProductItem {
  id?: number
  code: string
  name: string
  spec?: string
  category?: string
  categoryName?: string
  status?: number
  statusName?: string
  remark?: string
  nodeCount?: number
  materialCount?: number
  createTime?: string
  updateTime?: string
}

export interface BomProductStats {
  productTotal: number
  nodeTotal: number
  materialTotal: number
}

export interface BomProductListResult {
  rows: BomProductItem[]
  total: number
  page: number
  pageSize: number
  stats?: BomProductStats
}

export interface BomNodeMaterialItem {
  id?: number
  nodeId?: number
  materialId?: number
  materialType?: MaterialType
  materialTypeName?: string
  materialSku?: string
  materialName?: string
  materialSpec?: string
  materialUnit?: string
  quantity?: number
  createTime?: string
  updateTime?: string
}

export interface BomNodeItem {
  id?: number
  productId?: number
  parentId?: number | null
  nodeName?: string
  level?: number
  sort?: number
  remark?: string
  children?: BomNodeItem[]
  materials?: BomNodeMaterialItem[]
  createTime?: string
  updateTime?: string
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

export function listBomProducts(params: { keyword?: string; status?: number | null; page?: number; pageSize?: number }) {
  return unwrap<BomProductListResult>(request.get('/wms/bom/products', { params }))
}

export function getBomProduct(id: number) {
  return unwrap<BomProductItem>(request.get(`/wms/bom/products/${id}`))
}

export function createBomProduct(payload: BomProductItem) {
  return unwrap<BomProductItem>(request.post('/wms/bom/products/create', payload))
}

export function updateBomProduct(payload: BomProductItem) {
  return unwrap<BomProductItem>(request.post('/wms/bom/products/update', payload))
}

export function deleteBomProduct(id: number) {
  return unwrap<null>(request.post('/wms/bom/products/delete', { id }))
}

export function changeBomProductStatus(id: number, status: number) {
  return unwrap<BomProductItem>(request.post('/wms/bom/products/status', { id, status }))
}

export function getBomTree(productId: number) {
  return unwrap<BomNodeItem[]>(request.get('/wms/bom/nodes/tree', { params: { productId } }))
}

export function saveBomNode(payload: {
  id?: number
  productId?: number
  parentId?: number | null
  nodeName?: string
  sort?: number
  remark?: string
}) {
  return unwrap<BomNodeItem>(request.post('/wms/bom/nodes/save', payload))
}

export function deleteBomNode(id: number) {
  return unwrap<null>(request.post('/wms/bom/nodes/delete', { id }))
}

export function saveBomNodeMaterial(payload: {
  id?: number
  nodeId?: number
  materialId?: number
  materialType?: MaterialType
  quantity?: number
}) {
  return unwrap<BomNodeMaterialItem>(request.post('/wms/bom/nodes/materials/save', payload))
}

export function removeBomNodeMaterial(id: number) {
  return unwrap<null>(request.post('/wms/bom/nodes/materials/delete', { id }))
}
