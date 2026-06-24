import { request, unwrap } from './http'

export interface CostCollection {
  id?: number
  code: string
  costType: string
  source: string
  amount: number
  date: string
  status: string
}

export interface CostAllocation {
  id?: number
  code: string
  collectionCode: string
  costType: string
  object: string
  amount: number
  ratio: string
  date: string
}

export interface CostStats {
  collected: number
  allocated: number
  accumulated: number
  pending: number
  collectionCount: number
  allocationCount: number
}

export interface CostDistributionItem {
  name: string
  value: number
}

export interface CostCharts {
  distribution: CostDistributionItem[]
}

export function listCollections(params: { keyword?: string; costType?: string } = {}) {
  return unwrap<CostCollection[]>(request.get('/fin/cost/collections', { params }))
}

export function listAllocations(params: { keyword?: string; costType?: string } = {}) {
  return unwrap<CostAllocation[]>(request.get('/fin/cost/allocations', { params }))
}

export function costStats() {
  return unwrap<CostStats>(request.get('/fin/cost/stats'))
}

export function costCharts() {
  return unwrap<CostCharts>(request.get('/fin/cost/charts'))
}

export function collectCost(payload: Partial<CostCollection>) {
  return unwrap<CostCollection>(request.post('/fin/cost/collect', payload))
}

export function allocateCost(payload: { collectionId: number; object: string; amount: number; ratio?: string }) {
  return unwrap<CostAllocation>(request.post('/fin/cost/allocate', payload))
}
