import request from '@/api/request'

export type WarningRuleScope = 'global' | 'material'

export interface WarningRuleItem {
  id?: number
  name: string
  scope: WarningRuleScope
  scopeName?: string
  materialId?: number | null
  materialSku?: string | null
  materialName?: string | null
  materialSpec?: string | null
  warehouseCode?: string | null
  warehouseName?: string | null
  lowThreshold: number
  mediumThreshold: number
  highThreshold: number
  enabled: number
  enabledName?: string
  remark?: string | null
  createTime?: string
  updateTime?: string
}

export interface WarningRuleQuery {
  keyword?: string
  scope?: string
  enabled?: number
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

export function listWarningRules(params: WarningRuleQuery = {}) {
  return unwrap<WarningRuleItem[]>(request.get('/wms/warning/rule/list', { params }))
}

export function getWarningRule(id: number) {
  return unwrap<WarningRuleItem>(request.get(`/wms/warning/rule/${id}`))
}

export function createWarningRule(payload: WarningRuleItem) {
  return unwrap<WarningRuleItem>(request.post('/wms/warning/rule/create', payload))
}

export function updateWarningRule(payload: WarningRuleItem) {
  return unwrap<WarningRuleItem>(request.post('/wms/warning/rule/update', payload))
}

export function toggleWarningRule(id: number, enabled: number) {
  return unwrap<WarningRuleItem>(request.post('/wms/warning/rule/status', { id, enabled }))
}

export function deleteWarningRule(id: number) {
  return unwrap<null>(request.post('/wms/warning/rule/delete', { id }))
}
