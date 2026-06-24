import { request, unwrap, type PageParams, type PageResult } from './http'

export interface Receivable {
  id?: number
  code: string
  customer: string
  salesOrder: string
  contract?: string
  amount: number
  writtenOff: number
  pending: number
  invoiced: boolean
  dueDate: string
  status: string
}

export interface ReceiptRecord {
  id?: number
  code: string
  customer: string
  receivableCode: string
  amount: number
  method: string
  date: string
  operator: string
}

export interface ReceivableStats {
  total: number
  writtenOff: number
  pending: number
  monthReceipt: number
  count: number
}

export function listReceivables(params: { keyword?: string; status?: string; customer?: string } & PageParams = {}) {
  return unwrap<PageResult<Receivable>>(request.get('/fin/receivable/list', { params }))
}

export function listReceiptRecords(params: { keyword?: string } & PageParams = {}) {
  return unwrap<PageResult<ReceiptRecord>>(request.get('/fin/receivable/records', { params }))
}

export function receivableStats() {
  return unwrap<ReceivableStats>(request.get('/fin/receivable/stats'))
}

export function createReceivable(payload: Partial<Receivable>) {
  return unwrap<Receivable>(request.post('/fin/receivable/create', payload))
}

export function writeOffReceivable(payload: { id: number; amount: number; method?: string; date?: string; operator?: string }) {
  return unwrap<Receivable>(request.post('/fin/receivable/write-off', payload))
}

export function invoiceReceivable(id: number) {
  return unwrap<Receivable>(request.post('/fin/receivable/invoice', { id }))
}
