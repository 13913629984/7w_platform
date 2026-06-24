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
}

export function getWorkspace() {
  return unwrap<WorkspaceData>(request.get('/fin/workspace'))
}
