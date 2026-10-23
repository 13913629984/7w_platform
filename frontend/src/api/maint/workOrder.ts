import request from '@/api/request'
import type { ApiEnvelope, MaintListResult } from './equipment'

export type { ApiEnvelope, MaintListResult }

export interface WorkOrderItem {
  id?: number
  code?: string
  title: string
  equipmentId?: number | null
  equipmentCode?: string
  equipmentName?: string
  customerName?: string
  type?: string
  priority?: string
  status?: string
  reporter?: string
  handler?: string
  reportTime?: string
  planTime?: string
  finishTime?: string
  hours?: number | null
  cost?: number | null
  faultDesc?: string
  solution?: string
  remark?: string
  createTime?: string
  updateTime?: string
}

export interface WorkOrderStats {
  total: number
  pendingCount: number
  assignedCount: number
  processingCount: number
  completedCount: number
  closedCount: number
  urgentCount: number
  overdueCount: number
  monthCompletedCount: number
}

export interface WorkOrderQuery {
  keyword?: string
  status?: string
  priority?: string
  type?: string
  page?: number
  pageSize?: number
}

/** 设备下拉选项 */
export interface WorkOrderEquipmentOption {
  id: number
  code: string
  name: string
  customerName?: string
}

/** 处理人下拉选项（value/label 均为姓名） */
export interface HandlerOption {
  value: string
  label: string
}

export interface WorkOrderStatusPayload {
  id: number
  status: string
  handler?: string
  remark?: string
}

async function unwrap<T>(promise: Promise<{ data: ApiEnvelope<T> }>): Promise<ApiEnvelope<T>> {
  const response = await promise
  return response.data
}

export function listWorkOrders(params: WorkOrderQuery = {}) {
  return unwrap<MaintListResult<WorkOrderItem>>(request.get('/maint/work-order/list', { params }))
}

export function getWorkOrderStats() {
  return unwrap<WorkOrderStats>(request.get('/maint/work-order/stats'))
}

export function getWorkOrderEquipmentOptions() {
  return unwrap<WorkOrderEquipmentOption[]>(request.get('/maint/work-order/equipment-options'))
}

export function getHandlerOptions() {
  return unwrap<HandlerOption[]>(request.get('/maint/work-order/handler-options'))
}

export function getWorkOrder(id: number) {
  return unwrap<WorkOrderItem>(request.get(`/maint/work-order/${id}`))
}

export function createWorkOrder(payload: WorkOrderItem) {
  return unwrap<WorkOrderItem>(request.post('/maint/work-order/create', payload))
}

export function updateWorkOrder(payload: WorkOrderItem) {
  return unwrap<WorkOrderItem>(request.post('/maint/work-order/update', payload))
}

export function deleteWorkOrder(id: number) {
  return unwrap<null>(request.post('/maint/work-order/delete', { id }))
}

export function changeWorkOrderStatus(payload: WorkOrderStatusPayload) {
  return unwrap<WorkOrderItem>(request.post('/maint/work-order/status', payload))
}
