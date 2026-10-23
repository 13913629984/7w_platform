import request from '@/api/request'

export interface ApiEnvelope<T> {
  code: number
  message: string
  data: T
}

/** 电测-来货登记 列表行 / 明细 */
export interface RegisterItem {
  id?: number
  registerNo?: string
  customerName: string
  contact?: string
  phone?: string
  product: string
  model?: string
  brand?: string
  batchNo?: string
  quantity?: number | null
  sampleQty?: number | null
  /** pending/received/testing/done/returned */
  status?: string
  /** yyyy-MM-dd */
  arriveDate?: string
  /** yyyy-MM-dd */
  expectDate?: string
  testStandard?: string
  operator?: string
  remark?: string
  createTime?: string
  updateTime?: string
}

export interface RegisterStats {
  total: number
  pending: number
  received: number
  testing: number
  done: number
  returned: number
  monthAdded: number
  totalQuantity: number
}

export interface RegisterListResult {
  rows: RegisterItem[]
  total: number
  page: number
  pageSize: number
  stats: RegisterStats
}

export interface RegisterQuery {
  keyword?: string
  status?: string
  customerName?: string
  page?: number
  pageSize?: number
}

/** 来货登记下拉选项 */
export interface RegisterOption {
  id: number
  registerNo: string
  customerName?: string
  product?: string
  model?: string
  batchNo?: string
  quantity?: number
  sampleQty?: number
  testStandard?: string
  status?: string
}

async function unwrap<T>(promise: Promise<{ data: ApiEnvelope<T> }>): Promise<ApiEnvelope<T>> {
  const response = await promise
  return response.data
}

export function listRegisters(params: RegisterQuery = {}) {
  return unwrap<RegisterListResult>(request.get('/ets/register/list', { params }))
}

export function getRegisterStats() {
  return unwrap<RegisterStats>(request.get('/ets/register/stats'))
}

export function getRegisterOptions() {
  return unwrap<RegisterOption[]>(request.get('/ets/register/options'))
}

export function getRegister(id: number) {
  return unwrap<RegisterItem>(request.get(`/ets/register/${id}`))
}

export function createRegister(payload: RegisterItem) {
  return unwrap<RegisterItem>(request.post('/ets/register/create', payload))
}

export function updateRegister(payload: RegisterItem) {
  return unwrap<RegisterItem>(request.post('/ets/register/update', payload))
}

export function deleteRegister(id: number) {
  return unwrap<null>(request.post('/ets/register/delete', { id }))
}
