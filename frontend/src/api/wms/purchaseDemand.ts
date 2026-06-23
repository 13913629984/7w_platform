import request from '@/api/request'

export type DemandSource = 'warning' | 'mrp' | 'manual'
export type DemandStatus = 'pending' | 'ordered' | 'completed'

export interface PurchaseDemand {
  id?: number
  code: string
  source: DemandSource
  materialId?: number
  sku: string
  materialName: string
  spec?: string
  warehouseCode?: string
  qty: number
  estimatedPrice: number
  demandDate?: string
  status: DemandStatus
  poCode?: string
  requester?: string
  remark?: string
  createTime?: string
}

export interface WarningTrigger {
  materialId?: number
  warehouseCode?: string
  sku: string
  name: string
  spec?: string
  currentStock: number
  safetyStock: number
  warningStock: number
  suggestQty: number
  estimatedPrice?: number
}

export interface DemandStats {
  pending: number
  ordered: number
  completed: number
  triggerCount: number
}

export interface DemandScanResult {
  created: number
}

export interface GeneratePOResult {
  poCode: string
  updated: number
}

export interface DemandQuery {
  keyword?: string
  source?: string
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

export function listDemands(params: DemandQuery = {}) {
  return unwrap<PurchaseDemand[]>(request.get('/wms/purchase-demand/list', { params }))
}

export function getDemandStats() {
  return unwrap<DemandStats>(request.get('/wms/purchase-demand/stats'))
}

export function listTriggers() {
  return unwrap<WarningTrigger[]>(request.get('/wms/purchase-demand/triggers'))
}

export function getDemand(id: number) {
  return unwrap<PurchaseDemand>(request.get(`/wms/purchase-demand/${id}`))
}

export function createDemand(payload: Partial<PurchaseDemand>) {
  return unwrap<PurchaseDemand>(request.post('/wms/purchase-demand/create', payload))
}

export function updateDemand(payload: Partial<PurchaseDemand>) {
  return unwrap<PurchaseDemand>(request.post('/wms/purchase-demand/update', payload))
}

export function generateFromWarning(items: WarningTrigger[] = []) {
  return unwrap<DemandScanResult>(request.post('/wms/purchase-demand/generate-from-warning', { items }))
}

export function runMrp() {
  return unwrap<DemandScanResult>(request.post('/wms/purchase-demand/mrp'))
}

export function generatePO(ids: number[]) {
  return unwrap<GeneratePOResult>(request.post('/wms/purchase-demand/generate-po', { ids }))
}

export function deleteDemand(id: number) {
  return unwrap<null>(request.post('/wms/purchase-demand/delete', { id }))
}
