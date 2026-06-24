import request from '@/api/request'

export interface ApiEnvelope<T> {
  code: number
  message: string
  data: T
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
