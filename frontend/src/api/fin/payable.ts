import { request, unwrap, type PageParams, type PageResult } from './http'

export interface Payable {
  id?: number
  code: string
  receiptCode: string
  supplier: string
  purchaseOrder?: string
  purchaseAmount: number
  tax: number
  payable: number
  paid: number
  pending: number
  matched: boolean
  dueDate: string
  status: string
}

export interface PendingReceipt {
  code: string
  purchaseOrder: string
  supplier: string
  amount: number
  tax: number
  payable: number
}

export interface PayableStats {
  total: number
  count: number
  pendingMatch: number
  matched: number
  overdue: number
}

export function listPayables(params: { keyword?: string; status?: string } & PageParams = {}) {
  return unwrap<PageResult<Payable>>(request.get('/fin/payable/list', { params }))
}

export function listPendingReceipts() {
  return unwrap<PendingReceipt[]>(request.get('/fin/payable/pending-receipts'))
}

export function payableStats() {
  return unwrap<PayableStats>(request.get('/fin/payable/stats'))
}

export function createPayable(payload: Partial<Payable>) {
  return unwrap<Payable>(request.post('/fin/payable/create', payload))
}

export function matchPayable(id: number) {
  return unwrap<Payable>(request.post('/fin/payable/match', { id }))
}
