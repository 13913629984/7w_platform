import request from '@/api/request'

export type POStatus = 'pending' | 'confirmed' | 'inbound' | 'cancelled'

export interface PurchaseOrderItem {
  id?: number
  materialId?: number
  sku: string
  name: string
  spec?: string
  qty: number
  unitPrice: number
}

export interface PurchaseOrder {
  id?: number
  code: string
  supplier: string
  demandCodes: string[]
  warehouseCode?: string
  materialCount: number
  totalQty: number
  totalAmount: number
  deliveryDate?: string
  status: POStatus
  inboundCode?: string
  creator?: string
  remark?: string
  createTime?: string
  items?: PurchaseOrderItem[]
}

export interface POStats {
  pending: number
  confirmed: number
  inbound: number
  cancelled: number
  totalAmount: number
}

export interface POQuery {
  keyword?: string
  status?: string
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

export function listPurchaseOrders(params: POQuery = {}) {
  return unwrap<PurchaseOrder[]>(request.get('/wms/purchase-order/list', { params }))
}

export function getPOStats() {
  return unwrap<POStats>(request.get('/wms/purchase-order/stats'))
}

export function getPurchaseOrder(id: number) {
  return unwrap<PurchaseOrder>(request.get(`/wms/purchase-order/${id}`))
}

export function createPurchaseOrder(payload: Partial<PurchaseOrder> & { items: PurchaseOrderItem[] }) {
  return unwrap<PurchaseOrder>(request.post('/wms/purchase-order/create', payload))
}

export function updatePurchaseOrder(payload: Partial<PurchaseOrder> & { items: PurchaseOrderItem[] }) {
  return unwrap<PurchaseOrder>(request.post('/wms/purchase-order/update', payload))
}

export function confirmPurchaseOrder(id: number) {
  return unwrap<PurchaseOrder>(request.post('/wms/purchase-order/confirm', { id }))
}

export function createInboundFromPO(id: number) {
  return unwrap<PurchaseOrder>(request.post('/wms/purchase-order/create-inbound', { id }))
}

export function cancelPurchaseOrder(id: number) {
  return unwrap<PurchaseOrder>(request.post('/wms/purchase-order/cancel', { id }))
}

export function deletePurchaseOrder(id: number) {
  return unwrap<null>(request.post('/wms/purchase-order/delete', { id }))
}
