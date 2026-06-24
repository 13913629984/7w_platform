import request from '@/api/request'

export interface ApiEnvelope<T> {
  code: number
  message: string
  data: T
}

/** 后端统一分页返回结构 */
export interface PageResult<T> {
  rows: T[]
  total: number
  page: number
  pageSize: number
}

export interface PageParams {
  page?: number
  pageSize?: number
}

export async function unwrap<T>(promise: Promise<{ data: ApiEnvelope<T> }>): Promise<T> {
  const response = await promise
  const envelope = response.data
  if (envelope.code !== 0) {
    throw new Error(envelope.message || '请求失败')
  }
  return envelope.data
}

export { request }
