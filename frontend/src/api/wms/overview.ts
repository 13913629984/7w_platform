import request from '@/api/request'

export interface OverviewSummary {
  materialTotal: number
  materialThisMonth: number
  totalQty: number
  inboundToday: number
  inboundDelta: number
  outboundToday: number
  outboundDelta: number
}

export interface OverviewDistribution {
  category: string
  name: string
  quantity: number
}

export interface OverviewTrendPoint {
  date: string
  inbound: number
  outbound: number
}

export interface PendingInboundOrder {
  id: number
  code: string
  supplier: string
  warehouse: string
  totalQty: number
  status: string
  createTime: string
}

export interface PendingOutboundOrder {
  id: number
  code: string
  customer: string
  warehouse: string
  totalQty: number
  status: string
  createTime: string
}

export interface OverviewData {
  summary: OverviewSummary
  distribution: OverviewDistribution[]
  trend: OverviewTrendPoint[]
  pendingInbound: PendingInboundOrder[]
  pendingOutbound: PendingOutboundOrder[]
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

export function getOverview() {
  return unwrap<OverviewData>(request.get('/wms/overview'))
}
