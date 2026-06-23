import request from '@/api/request'

export type InboundMethod = 'qty' | 'sn'
export type InboundStatus = 'completed' | 'partial' | 'pending'
export type InspectStatus = 'qualified' | 'passed' | 'pending'

export interface InboundItem {
  id?: number
  materialId?: number
  sku: string
  name: string
  spec?: string
  locationCode?: string
  batchNo?: string
  method: InboundMethod
  qty: number
  inboundQty?: number
  unitPrice: number
}

export interface InboundOrder {
  id?: number
  code: string
  poCode?: string
  supplier: string
  warehouseCode: string
  warehouse?: string
  materialCount: number
  totalQty: number
  inboundQty: number
  inboundRate: number
  totalAmount: number
  inspectStatus: InspectStatus
  status: InboundStatus
  creator: string
  remark?: string
  createTime?: string
  items?: InboundItem[]
  records?: InboundRecord[]
}

export interface InboundRecord {
  id?: number
  inboundCode: string
  sku: string
  name: string
  spec?: string
  location?: string
  method: InboundMethod
  qty: number
  inboundQty?: number
  unitPrice: number
  createTime?: string
  operator?: string
}

export interface InboundSnRecord {
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

export interface InboundQuery {
  keyword?: string
  status?: string
}

export interface InboundDetailQuery {
  keyword?: string
  method?: string
}

export interface InboundSnQuery {
  keyword?: string
  status?: string
}

export interface ExecuteInboundLocation {
  locationCode: string
  qty: number
  snList?: string
}

export interface ExecuteInboundItem {
  id?: number
  locations: ExecuteInboundLocation[]
}

export interface ExecuteInboundPayload {
  id: number
  operator: string
  items: ExecuteInboundItem[]
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

export function listInboundOrders(params: InboundQuery = {}) {
  return unwrap<InboundOrder[]>(request.get('/wms/inbound/list', { params }))
}

export function listInboundDetails(params: InboundDetailQuery = {}) {
  return unwrap<InboundRecord[]>(request.get('/wms/inbound/details', { params }))
}

export function listInboundSn(params: InboundSnQuery = {}) {
  return unwrap<InboundSnRecord[]>(request.get('/wms/inbound/sn', { params }))
}

export function getInboundOrder(id: number) {
  return unwrap<InboundOrder>(request.get(`/wms/inbound/${id}`))
}

export function createInboundOrder(payload: Partial<InboundOrder> & { items: InboundItem[] }) {
  return unwrap<InboundOrder>(request.post('/wms/inbound/create', payload))
}

export function executeInboundOrder(payload: ExecuteInboundPayload) {
  return unwrap<InboundOrder>(request.post('/wms/inbound/execute', payload))
}

export function deleteInboundOrder(id: number) {
  return unwrap<null>(request.post('/wms/inbound/delete', { id }))
}
