import { request, unwrap } from './http'

export interface FinFlowRecord {
  id?: number
  code: string
  bizType: string
  module: string
  amount: number
  direction: string
  status: string
  date: string
}

export interface WorkspaceStats {
  income: number
  expense: number
  profit: number
  pendingReceipt: number
}

export interface WorkspaceData {
  stats: WorkspaceStats
  records: FinFlowRecord[]
  receivableReminders: ReceivableReminder[]
  payableReminders: PayableReminder[]
}

export interface ReceivableReminder {
  id?: number
  customer: string
  contractAmount: number
  received: number
  pending: number
  dueDate: string
  status: string
}

export interface PayableReminder {
  id?: number
  supplier: string
  purchaseOrder: string
  payableAmount: number
  pending: number
  dueDate: string
  status: string
}

export function getWorkspace() {
  return unwrap<WorkspaceData>(request.get('/fin/workspace'))
}
