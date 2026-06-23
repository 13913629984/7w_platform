import request from '@/api/request'

export interface WorkspaceStats {
  customerTotal: number
  monthAddedCustomers: number
  pendingDeals: number
  monthDealAmount: string
}

export interface WorkspaceLead {
  id: number
  name: string
  source: string
  owner: string
  followAt: string
}

export interface WorkspaceActivity {
  id: number
  title: string
  type: string
  customerName: string
  owner: string
  activityAt: string
}

export interface SalesTrendPoint {
  month: string
  amount: string
}

export interface CustomerDistributionPoint {
  level: string
  count: number
}

export interface WorkspaceOverview {
  stats: WorkspaceStats
  leads: WorkspaceLead[]
  activities: WorkspaceActivity[]
  salesTrend: SalesTrendPoint[]
  customerDistribution: CustomerDistributionPoint[]
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

export function getWorkspaceOverview() {
  return unwrap<WorkspaceOverview>(request.get('/crm/workspace/overview'))
}
