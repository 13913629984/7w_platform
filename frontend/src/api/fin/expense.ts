import { request, unwrap } from './http'

export interface Expense {
  id?: number
  code: string
  type: string
  module: string
  item: string
  amount: number
  date: string
  applicant: string
  approved: boolean
}

export interface ExpenseStats {
  sales: number
  manage: number
  rd: number
  total: number
}

export function listExpenses(params: { startMonth?: string; endMonth?: string; type?: string } = {}) {
  return unwrap<Expense[]>(request.get('/fin/expense/list', { params }))
}

export function expenseStats() {
  return unwrap<ExpenseStats>(request.get('/fin/expense/stats'))
}

export function createExpense(payload: Partial<Expense>) {
  return unwrap<Expense>(request.post('/fin/expense/create', payload))
}

export function approveExpense(id: number) {
  return unwrap<Expense>(request.post('/fin/expense/approve', { id }))
}
