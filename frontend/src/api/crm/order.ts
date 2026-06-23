import request from '@/api/request'

export interface OrderItemRow {
  id?: number
  productCode: string
  productName: string
  spec: string
  quantity: number
  price: number
  subtotal?: number
}

export interface OrderItem {
  id?: number
  orderNo: string
  customerId?: number | null
  customerName: string
  dealId?: number | null
  dealName?: string
  contractNo: string
  amount: number
  owner?: string
  signDate: string
  expectDeliverAt?: string
  status?: string
  remark?: string
  outboundCode?: string
  createTime?: string
  items: OrderItemRow[]
}

export interface OrderStats {
  total: number
  monthAdded: number
  totalAmount: string
  pendingDeliver: number
}

export interface OrderListResult {
  rows: OrderItem[]
  total: number
  page: number
  pageSize: number
  stats?: OrderStats
  statuses?: string[]
}

export interface OrderListQuery {
  keyword?: string
  status?: string
  customerId?: number
  page?: number
  pageSize?: number
}

export interface CustomerOption {
  id: number
  name: string
}

export interface DealOption {
  id: number
  name: string
  customerId?: number
  customerName?: string
  amount?: number
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

export function listOrders(params: OrderListQuery) {
  return unwrap<OrderListResult>(request.get('/crm/order/list', { params }))
}

export function getOrderNextNo() {
  return unwrap<string>(request.get('/crm/order/next-no'))
}

export function listOrderCustomers() {
  return unwrap<CustomerOption[]>(request.get('/crm/order/customers'))
}

export function listOrderDeals() {
  return unwrap<DealOption[]>(request.get('/crm/order/deals'))
}

export function getOrder(id: number) {
  return unwrap<OrderItem>(request.get(`/crm/order/${id}`))
}

export function createOrder(payload: OrderItem) {
  return unwrap<OrderItem>(request.post('/crm/order/create', payload))
}

export function updateOrder(payload: OrderItem) {
  return unwrap<OrderItem>(request.post('/crm/order/update', payload))
}

export function deleteOrder(id: number) {
  return unwrap<null>(request.post('/crm/order/delete', { id }))
}

export function batchDeleteOrders(ids: number[]) {
  return unwrap<number>(request.post('/crm/order/batch-delete', { ids }))
}

export function confirmOrder(id: number) {
  return unwrap<OrderItem>(request.post(`/crm/order/confirm/${id}`))
}

export function deliverOrder(id: number) {
  return unwrap<OrderItem>(request.post(`/crm/order/deliver/${id}`))
}
