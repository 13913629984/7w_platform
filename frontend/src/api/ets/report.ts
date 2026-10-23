import request from '@/api/request'

export interface ApiEnvelope<T> {
  code: number
  message: string
  data: T
}

/** 电测-测试报告 列表行 / 明细 */
export interface ReportItem {
  id?: number
  reportNo?: string
  registerId?: number | null
  registerNo?: string
  customerName: string
  product: string
  model?: string
  batchNo?: string
  quantity?: number | null
  sampleQty?: number | null
  passQty?: number | null
  failQty?: number | null
  /** 百分比（后端已 ×100，保留 2 位） */
  passRate?: number | null
  /** draft/pending/approved/rejected */
  status?: string
  conclusion?: string
  inspector?: string
  /** yyyy-MM-dd HH:mm */
  issueDate?: string
  approver?: string
  /** yyyy-MM-dd HH:mm */
  approveTime?: string
  remark?: string
  createTime?: string
  updateTime?: string
}

export interface ReportStats {
  total: number
  draft: number
  pending: number
  approved: number
  rejected: number
  totalSample: number
  totalPass: number
  totalQuantity: number
  passRate: number
}

export interface ReportListResult {
  rows: ReportItem[]
  total: number
  page: number
  pageSize: number
  stats: ReportStats
}

export interface ReportQuery {
  keyword?: string
  status?: string
  customerName?: string
  page?: number
  pageSize?: number
}

/** 报告关联的来货登记选项 */
export interface ReportRegisterOption {
  id: number
  registerNo: string
  customerName?: string
  product?: string
  model?: string
  batchNo?: string
  quantity?: number
  sampleQty?: number
  status?: string
}

/** 审批请求（action 仅 approve/reject） */
export interface ReportApprovePayload {
  id: number
  action: 'approve' | 'reject'
  approver?: string
  remark?: string
}

async function unwrap<T>(promise: Promise<{ data: ApiEnvelope<T> }>): Promise<ApiEnvelope<T>> {
  const response = await promise
  return response.data
}

export function listReports(params: ReportQuery = {}) {
  return unwrap<ReportListResult>(request.get('/ets/report/list', { params }))
}

export function getReportStats() {
  return unwrap<ReportStats>(request.get('/ets/report/stats'))
}

export function getReportRegisterOptions() {
  return unwrap<ReportRegisterOption[]>(request.get('/ets/report/register-options'))
}

export function getReport(id: number) {
  return unwrap<ReportItem>(request.get(`/ets/report/${id}`))
}

export function createReport(payload: ReportItem) {
  return unwrap<ReportItem>(request.post('/ets/report/create', payload))
}

export function updateReport(payload: ReportItem) {
  return unwrap<ReportItem>(request.post('/ets/report/update', payload))
}

export function deleteReport(id: number) {
  return unwrap<null>(request.post('/ets/report/delete', { id }))
}

export function approveReport(payload: ReportApprovePayload) {
  return unwrap<ReportItem>(request.post('/ets/report/approve', payload))
}

/** 签发：后端支持 query id 或 body {id} */
export function issueReport(id: number) {
  return unwrap<ReportItem>(request.post('/ets/report/issue', { id }, { params: { id } }))
}
