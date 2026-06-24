import { request, unwrap, type PageParams, type PageResult } from './http'

export interface Payment {
  id?: number
  code: string
  type: string
  apCode?: string
  payee: string
  amount: number
  method: string
  applyDate: string
  status: string
  remark?: string
}

export interface PaymentStats {
  monthTotal: number
  paid: number
  pendingApprove: number
  approving: number
  approved: number
  rejected: number
}

export function listPayments(params: { keyword?: string; status?: string } & PageParams = {}) {
  return unwrap<PageResult<Payment>>(request.get('/fin/payment/list', { params }))
}

export function paymentStats() {
  return unwrap<PaymentStats>(request.get('/fin/payment/stats'))
}

export function createPayment(payload: Partial<Payment>) {
  return unwrap<Payment>(request.post('/fin/payment/create', payload))
}

export function approvePayment(id: number) {
  return unwrap<Payment>(request.post('/fin/payment/approve', { id }))
}

export function rejectPayment(id: number) {
  return unwrap<Payment>(request.post('/fin/payment/reject', { id }))
}
