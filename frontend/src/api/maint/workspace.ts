import request from '@/api/request'
import type { ApiEnvelope } from './equipment'

export type { ApiEnvelope }

/** 近 7 日工单趋势点 */
export interface MaintTrendPoint {
  date: string
  label: string
  created: number
  finished: number
}

/** 设备状态分布项 */
export interface MaintDistributionItem {
  status: string
  label: string
  count: number
}

export interface MaintOverview {
  equipmentTotal: number
  runningCount: number
  repairCount: number
  pendingOrderCount: number
  overdueOrderCount: number
  monthCompletedCount: number
  planDueCount: number
  lowStockCount: number
  trend?: MaintTrendPoint[]
  distribution?: MaintDistributionItem[]
}

async function unwrap<T>(promise: Promise<{ data: ApiEnvelope<T> }>): Promise<ApiEnvelope<T>> {
  const response = await promise
  return response.data
}

export function getMaintOverview() {
  return unwrap<MaintOverview>(request.get('/maint/workspace/overview'))
}

export function getMaintTrend() {
  return unwrap<MaintTrendPoint[]>(request.get('/maint/workspace/trend'))
}

export function getMaintDistribution() {
  return unwrap<MaintDistributionItem[]>(request.get('/maint/workspace/distribution'))
}
