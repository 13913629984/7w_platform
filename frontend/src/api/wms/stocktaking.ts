import request from '@/api/request'

export type StockTakingStatus = 'pending' | 'processing' | 'pending_review' | 'completed'
export type StockTakingType = 'full' | 'cycle' | 'spot'
export type TaskStatus = 'pending' | 'completed'
export type ReviewStatus = '' | 'pending' | 'approved' | 'rejected'

export interface StockTakingTask {
  id: number
  inventoryId?: number
  materialId?: number
  materialCode?: string
  materialName?: string
  spec?: string
  unit?: string
  locationCode?: string
  location?: string
  batchNo?: string
  bookQty: number
  actualQty: number
  diffQty: number
  diffAmount: number
  hasDiff: boolean
  unitPrice: number
  status: TaskStatus
  reviewStatus: ReviewStatus
  remark?: string
}

export interface StockTakingOrder {
  id: number
  code: string
  warehouseCode: string
  warehouse?: string
  scopeDesc?: string
  type: StockTakingType
  taskCount: number
  completedCount: number
  diffCount: number
  status: StockTakingStatus
  creator?: string
  remark?: string
  createTime?: string
  tasks?: StockTakingTask[]
}

export interface StockTakingStats {
  total: number
  completed: number
  processing: number
  pendingReview: number
}

export interface StockTakingQuery {
  keyword?: string
  status?: string
}

export interface CreateStockTakingPayload {
  code?: string
  warehouseCode: string
  type: StockTakingType
  creator: string
  scopeDesc?: string
  locationCodes?: string[]
  remark?: string
}

export interface ProcessPayload {
  id: number
  taskId: number
  actualQty: number
  remark?: string
}

export interface ReviewPayload {
  id: number
  taskId: number
  result: 'approved' | 'rejected'
  remark?: string
}

export interface ApiEnvelope<T> {
  code: number
  message: string
  data: T
}

async function unwrap<T>(promise: Promise<{ data: ApiEnvelope<T> }>): Promise<ApiEnvelope<T>> {
  const response = await promise
  return response.data
}

export function listStockTakingOrders(params: StockTakingQuery = {}) {
  return unwrap<StockTakingOrder[]>(request.get('/wms/stocktaking/list', { params }))
}

export function getStockTakingStats() {
  return unwrap<StockTakingStats>(request.get('/wms/stocktaking/stats'))
}

export function getStockTakingOrder(id: number) {
  return unwrap<StockTakingOrder>(request.get(`/wms/stocktaking/${id}`))
}

export function createStockTakingOrder(payload: CreateStockTakingPayload) {
  return unwrap<StockTakingOrder>(request.post('/wms/stocktaking/create', payload))
}

export function startStockTakingOrder(id: number) {
  return unwrap<StockTakingOrder>(request.post('/wms/stocktaking/start', { id }))
}

export function processStockTakingTask(payload: ProcessPayload) {
  return unwrap<StockTakingOrder>(request.post('/wms/stocktaking/process', payload))
}

export function reviewStockTakingTask(payload: ReviewPayload) {
  return unwrap<StockTakingOrder>(request.post('/wms/stocktaking/review', payload))
}

export function completeStockTakingOrder(id: number) {
  return unwrap<StockTakingOrder>(request.post('/wms/stocktaking/complete', { id }))
}

export function deleteStockTakingOrder(id: number) {
  return unwrap<null>(request.post('/wms/stocktaking/delete', { id }))
}
