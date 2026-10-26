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

/** 跨模块单据关联行：销售链 / 采购链 */
export interface BusinessRelation {
  bizType: '销售业务' | '采购业务' | string
  salesOrder: string
  purchaseOrder: string
  stockDoc: string
  arap: string
  payment: string
  status: string
  amount: number
  partner: string
  date: string
}

export interface BusinessCompleteness {
  total: number
  complete: number
  pending: number
}

export function listBusinessFlow(params: { keyword?: string } = {}) {
  return unwrap<FinFlowRecord[]>(request.get('/fin/business-flow/list', { params }))
}

export function businessFlowStats() {
  return unwrap<BusinessFlowStats>(request.get('/fin/business-flow/stats'))
}

/**
 * 跨模块单据关联明细（真实数据源）。
 *
 * 业务流程页原来在 `relations` 里写死了 5 行假数据，导致页面看起来有内容但和库里毫无关系。
 * 这个接口从 CRM/WMS/FIN 三边的真实表 join 出来。
 */
export function listBusinessRelations(params: { keyword?: string } = {}) {
  return unwrap<BusinessRelation[]>(request.get('/fin/business-flow/relations', { params }))
}

export function businessCompleteness(params: { keyword?: string } = {}) {
  return unwrap<BusinessCompleteness>(request.get('/fin/business-flow/relations/completeness', { params }))
}
