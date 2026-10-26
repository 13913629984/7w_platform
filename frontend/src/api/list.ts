import request from '@/api/request'

/**
 * 统一响应与列表解析助手。
 *
 * 背景：平台上的列表接口历史上有三种形态 ——
 * - `{list, total, ...}`（SYS）
 * - `{rows, total, ...}`（CRM / MAINT / ETS / FIN）
 * - 裸数组（WMS 早期）
 *
 * 后端已逐步统一成 `{rows, total, page, pageSize}`（见后端 `ListResult`），
 * 但为了不破坏还在跑旧版本服务的环境，这里做**双向兼容**：
 * 数组就当「不分页的全部数据」，对象就取 rows/list。
 */

export interface ApiEnvelope<T> {
  code: number
  message: string
  data: T
}

export interface PageResult<T> {
  rows: T[]
  total: number
  page: number
  pageSize: number
  stats?: unknown
}

/** 解包 ApiResult；code 非 0 时抛错（带中文 message），调用方用 try/catch 统一处理 */
export async function unwrap<T>(promise: Promise<{ data: ApiEnvelope<T> }>): Promise<T> {
  const response = await promise
  const envelope = response.data
  if (!envelope || typeof envelope.code !== 'number') {
    throw new Error('接口返回格式不正确')
  }
  if (envelope.code !== 0) {
    throw new Error(envelope.message || '请求失败')
  }
  return envelope.data
}

/**
 * 解包列表接口，统一成数组。
 *
 * - 新后端：`{rows|list, total, ...}` → 取 rows/list
 * - 旧后端：裸数组 → 原样返回
 *
 * 需要在需要总数时改用 {@link unwrapPage}。
 */
export async function unwrapList<T>(promise: Promise<{ data: ApiEnvelope<unknown> }>): Promise<T[]> {
  return (await unwrapPage<T>(promise)).rows
}

/** 解包列表接口，统一成 {rows,total,page,pageSize}；裸数组时 total = 长度 */
export async function unwrapPage<T>(
  promise: Promise<{ data: ApiEnvelope<unknown> }>,
): Promise<PageResult<T>> {
  const data = await unwrap<unknown>(promise)
  if (Array.isArray(data)) {
    return { rows: data as T[], total: data.length, page: 1, pageSize: data.length }
  }
  if (data && typeof data === 'object') {
    const obj = data as Record<string, unknown>
    const rows = (obj.rows ?? obj.list ?? []) as T[]
    const total = typeof obj.total === 'number' ? obj.total : rows.length
    return {
      rows,
      total,
      page: typeof obj.page === 'number' ? obj.page : 1,
      pageSize: typeof obj.pageSize === 'number' ? obj.pageSize : rows.length,
      stats: obj.stats,
    }
  }
  return { rows: [], total: 0, page: 1, pageSize: 0 }
}
