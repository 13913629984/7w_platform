import request from '@/api/request'
import type { ApiEnvelope, MaintListResult, EquipmentOption } from './equipment'

export type { ApiEnvelope, MaintListResult, EquipmentOption }

export interface PlanItem {
  id?: number
  code?: string
  name: string
  equipmentId?: number | null
  equipmentCode?: string
  equipmentName?: string
  cycleType?: string
  cycleValue?: number | null
  startDate?: string
  nextDate?: string
  items?: string
  manager?: string
  status?: string
  remark?: string
  lastExecuteDate?: string
  executeCount?: number | null
  createTime?: string
  updateTime?: string
}

export interface PlanStats {
  total: number
  activeCount: number
  pausedCount: number
  finishedCount: number
  dueSoonCount: number
  overdueCount: number
  executeCountTotal: number
}

export interface PlanQuery {
  keyword?: string
  status?: string
  cycleType?: string
  equipmentKeyword?: string
  page?: number
  pageSize?: number
}

async function unwrap<T>(promise: Promise<{ data: ApiEnvelope<T> }>): Promise<ApiEnvelope<T>> {
  const response = await promise
  return response.data
}

export function listPlans(params: PlanQuery = {}) {
  return unwrap<MaintListResult<PlanItem>>(request.get('/maint/plan/list', { params }))
}

export function getPlanStats() {
  return unwrap<PlanStats>(request.get('/maint/plan/stats'))
}

export function getPlanEquipmentOptions() {
  return unwrap<EquipmentOption[]>(request.get('/maint/plan/equipment-options'))
}

export function getPlan(id: number) {
  return unwrap<PlanItem>(request.get(`/maint/plan/${id}`))
}

export function createPlan(payload: PlanItem) {
  return unwrap<PlanItem>(request.post('/maint/plan/create', payload))
}

export function updatePlan(payload: PlanItem) {
  return unwrap<PlanItem>(request.post('/maint/plan/update', payload))
}

export function deletePlan(id: number) {
  return unwrap<null>(request.post('/maint/plan/delete', { id }))
}

/** 执行一次保养：后端累加 executeCount 并推算 nextDate */
export function executePlan(id: number) {
  return unwrap<PlanItem>(request.post('/maint/plan/execute', { id }))
}
