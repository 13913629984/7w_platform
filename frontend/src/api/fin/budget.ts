import { request, unwrap } from './http'

export interface Budget {
  id?: number
  dept: string
  item: string
  budget: number
  executed: number
  rate: number
  year?: number
  month?: number
}

export interface BudgetStats {
  total: number
  executed: number
  balance: number
  rate: number
  overBudget: number
}

export interface BudgetChartItem {
  name: string
  executed: number
  remain: number
}

export interface BudgetCompareItem {
  name: string
  budget: number
  actual: number
}

export interface BudgetCharts {
  progress: BudgetChartItem[]
  compare: BudgetCompareItem[]
}

export function listBudgets() {
  return unwrap<Budget[]>(request.get('/fin/budget/list'))
}

export function budgetStats() {
  return unwrap<BudgetStats>(request.get('/fin/budget/stats'))
}

export function budgetCharts() {
  return unwrap<BudgetCharts>(request.get('/fin/budget/charts'))
}

export function createBudget(payload: Partial<Budget>) {
  return unwrap<Budget>(request.post('/fin/budget/create', payload))
}

export function updateBudget(payload: Partial<Budget> & { id: number }) {
  return unwrap<Budget>(request.post('/fin/budget/update', payload))
}
