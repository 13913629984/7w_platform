import { request, unwrap } from './http'
import type { FinFlowRecord } from './workspace'

export type { FinFlowRecord }

export interface BusinessFlowStats {
  income: number
  expense: number
  total: number
  confirmed: number
  processing: number
}

export function listBusinessFlow(params: { keyword?: string } = {}) {
  return unwrap<FinFlowRecord[]>(request.get('/fin/business-flow/list', { params }))
}

export function businessFlowStats() {
  return unwrap<BusinessFlowStats>(request.get('/fin/business-flow/stats'))
}
