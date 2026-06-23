import request from '@/api/request'

export type OutboundMethod = 'qty' | 'sn'
export type OutboundStatus = 'completed' | 'partial' | 'pending'

export interface OutboundItem {
  id?: number
  materialId?: number
  sku: string
  name: string
  spec?: string
  locationCode?: string
  batchNo?: string
  method: OutboundMethod
  qty: number
  outboundQty?: number
  unitPrice: number
}

export interface OutboundRecord {
  id?: number
  outboundCode: string
  sku: string
  name: string
  spec?: string
  location?: string
  method: OutboundMethod
  qty: number
  unitPrice: number
  createTime?: string
  operator?: string
}

export interface OutboundOrder {
  id?: number
  code: string
  orderNo?: string
  customer: string
  warehouseCode: string
  warehouse?: string
  materialCount: number
  totalQty: number
  outboundQty: number
  outboundRate: number
  totalAmount: number
  status: OutboundStatus
  creator: string
  remark?: string
  createTime?: string
  items?: OutboundItem[]
  records?: OutboundRecord[]
}

export interface OutboundSnRecord {
  id?: number
  sn: string
  sku: string
  name: string
  spec?: string
  location?: string
  inboundCode: string
  inboundTime?: string
  outboundCode?: string
  outboundTime?: string
  status: 'inbound' | 'outbound'
}

export interface OutboundQuery { keyword?: string; status?: string }
export interface OutboundDetailQuery { keyword?: string; method?: string }
export interface OutboundSnQuery { keyword?: string; status?: string }

export interface ExecuteOutboundLocation { locationCode: string; qty: number; snList?: string }
export interface ExecuteOutboundItem { id?: number; locations: ExecuteOutboundLocation[] }
export interface ExecuteOutboundPayload { id: number; operator: string; items: ExecuteOutboundItem[] }

export interface ApiEnvelope<T> { code: number; message: string; data: T }

async function unwrap<T>(promise: Promise<{ data: ApiEnvelope<T> }>): Promise<ApiEnvelope<T>> {
  const response = await promise
  return response.data
}

export function listOutboundOrders(params: OutboundQuery = {}) {
  return unwrap<OutboundOrder[]>(request.get('/wms/outbound/list', { params }))
}

export function listOutboundDetails(params: OutboundDetailQuery = {}) {
  return unwrap<OutboundRecord[]>(request.get('/wms/outbound/details', { params }))
}

export function listOutboundSn(params: OutboundSnQuery = {}) {
  return unwrap<OutboundSnRecord[]>(request.get('/wms/outbound/sn', { params }))
}

export function getOutboundOrder(id: number) {
  return unwrap<OutboundOrder>(request.get(`/wms/outbound/${id}`))
}

export function createOutboundOrder(payload: Partial<OutboundOrder> & { items: OutboundItem[] }) {
  return unwrap<OutboundOrder>(request.post('/wms/outbound/create', payload))
}

export function executeOutboundOrder(payload: ExecuteOutboundPayload) {
  return unwrap<OutboundOrder>(request.post('/wms/outbound/execute', payload))
}

export function deleteOutboundOrder(id: number) {
  return unwrap<null>(request.post('/wms/outbound/delete', { id }))
}
