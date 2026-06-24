import { request, unwrap, type PageParams } from './http'

export interface ArapReceivable {
  id?: number
  code: string
  customer: string
  salesOrder: string
  contract?: string
  contractAmount: number
  received: number
  pending: number
  dueDate: string
  status: string
}

export interface ArapPayable {
  id?: number
  code: string
  supplier: string
  purchaseOrder?: string
  payableAmount: number
  paid: number
  pending: number
  dueDate: string
  status: string
}

export interface ArapStats {
  arTotal: number
  apTotal: number
  arCount: number
  apCount: number
  arOverdue: number
}

export interface ArapData {
  stats: ArapStats
  receivables: ArapReceivable[]
  payables: ArapPayable[]
  arTotalCount: number
  apTotalCount: number
  page: number
  pageSize: number
}

export function getArap(params: { keyword?: string } & PageParams = {}) {
  return unwrap<ArapData>(request.get('/fin/arap', { params }))
}
