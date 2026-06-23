import request from '@/api/request'

export type WarningLevel = 'high' | 'medium' | 'low'
export type WarningStatus = 'pending' | 'handled' | 'resolved'

export interface WarningItem {
  id?: number
  materialId: number
  sku?: string
  name?: string
  spec?: string
  unit?: string
  warehouseCode: string
  warehouse?: string
  currentQty: number
  threshold: number
  level: WarningLevel
  levelName?: string
  status: WarningStatus
  statusName?: string
  handler?: string
  handleRemark?: string
  handleTime?: string
  warningTime?: string
}

export interface WarningStats {
  total: number
  handled: number
  pending: number
  high: number
}

export interface WarningScanResult {
  created: number
  updated: number
  resolved: number
  scanned: number
}

export interface WarningQuery {
  keyword?: string
  level?: string
  status?: string
}

export interface HandleWarningPayload {
  id: number
  handler: string
  remark?: string
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

export function listWarnings(params: WarningQuery = {}) {
  return unwrap<WarningItem[]>(request.get('/wms/warning/list', { params }))
}

export function getWarningStats() {
  return unwrap<WarningStats>(request.get('/wms/warning/stats'))
}

export function getWarning(id: number) {
  return unwrap<WarningItem>(request.get(`/wms/warning/${id}`))
}

export function scanWarnings() {
  return unwrap<WarningScanResult>(request.post('/wms/warning/scan'))
}

export function handleWarning(payload: HandleWarningPayload) {
  return unwrap<WarningItem>(request.post('/wms/warning/handle', payload))
}

export function deleteWarning(id: number) {
  return unwrap<null>(request.post('/wms/warning/delete', { id }))
}
