import request from '@/api/request'

export interface ApiEnvelope<T> {
  code: number
  message: string
  data: T
}

/** 电测-测试执行 列表行 / 明细 */
export interface TestItem {
  id?: number
  code?: string
  registerId?: number | null
  registerNo?: string
  customerName?: string
  product?: string
  model?: string
  testItem?: string
  /** appearance/insulation/withstand/grounding/temperature/performance */
  category?: string
  testStandard?: string
  standardValue?: string
  measuredValue?: string
  /** pass/fail/pending */
  result?: string
  tester?: string
  /** yyyy-MM-dd HH:mm */
  testTime?: string
  durationMin?: number | string | null
  equipmentUsed?: string
  remark?: string
  createTime?: string
  updateTime?: string
}

export interface TestStats {
  total: number
  pass: number
  fail: number
  pending: number
  passRate: number
  todayCount: number
  byCategory: Record<string, number>
}

export interface TestListResult {
  rows: TestItem[]
  total: number
  page: number
  pageSize: number
  stats: TestStats
}

export interface TestQuery {
  keyword?: string
  result?: string
  category?: string
  registerNo?: string
  page?: number
  pageSize?: number
}

/** 测试关联的来货登记选项 */
export interface TestRegisterOption {
  id: number
  registerNo: string
  customerName?: string
  product?: string
  model?: string
  batchNo?: string
  testStandard?: string
  status?: string
}

/** 结果录入请求（result 仅 pass/fail） */
export interface TestResultPayload {
  id: number
  result: string
  measuredValue?: string
  tester?: string
  remark?: string
}

async function unwrap<T>(promise: Promise<{ data: ApiEnvelope<T> }>): Promise<ApiEnvelope<T>> {
  const response = await promise
  return response.data
}

export function listTests(params: TestQuery = {}) {
  return unwrap<TestListResult>(request.get('/ets/test/list', { params }))
}

export function getTestStats() {
  return unwrap<TestStats>(request.get('/ets/test/stats'))
}

export function getTestRegisterOptions() {
  return unwrap<TestRegisterOption[]>(request.get('/ets/test/register-options'))
}

export function getTest(id: number) {
  return unwrap<TestItem>(request.get(`/ets/test/${id}`))
}

export function createTest(payload: TestItem) {
  return unwrap<TestItem>(request.post('/ets/test/create', payload))
}

export function updateTest(payload: TestItem) {
  return unwrap<TestItem>(request.post('/ets/test/update', payload))
}

export function deleteTest(id: number) {
  return unwrap<null>(request.post('/ets/test/delete', { id }))
}

export function submitTestResult(payload: TestResultPayload) {
  return unwrap<TestItem>(request.post('/ets/test/result', payload))
}
