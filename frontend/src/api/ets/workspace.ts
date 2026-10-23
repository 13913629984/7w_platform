import request from '@/api/request'

export interface ApiEnvelope<T> {
  code: number
  message: string
  data: T
}

/** 分布项 {name, value} */
export interface DistributionItem {
  name: string
  value: number
}

/** 趋势项 {date, label, count} */
export interface TrendItem {
  date: string
  label: string
  count: number
}

/** 电测工作台总览 */
export interface EtsOverview {
  /** 本月来货数 */
  monthRegister: number
  /** 待测试数 */
  pendingTest: number
  /** 今日完成测试数 */
  todayTested: number
  /** 合格率（百分比 0-100，保留 2 位） */
  passRate: number
  /** 不合格数 */
  failCount: number
  /** 报告待审批数 */
  pendingReport: number
  testTrend: TrendItem[]
  resultDistribution: DistributionItem[]
  categoryDistribution: DistributionItem[]
}

async function unwrap<T>(promise: Promise<{ data: ApiEnvelope<T> }>): Promise<ApiEnvelope<T>> {
  const response = await promise
  return response.data
}

export function getEtsOverview() {
  return unwrap<EtsOverview>(request.get('/ets/workspace/overview'))
}

export function getEtsTrend() {
  return unwrap<TrendItem[]>(request.get('/ets/workspace/trend'))
}

export function getEtsDistribution() {
  return unwrap<DistributionItem[]>(request.get('/ets/workspace/distribution'))
}
