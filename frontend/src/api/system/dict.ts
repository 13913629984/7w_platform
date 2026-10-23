import request from '@/api/request'

export interface ApiEnvelope<T> {
  code: number
  message: string
  data: T
}

export interface DictTypeItem {
  id?: number
  dictCode: string
  dictName: string
  description?: string
  isSystem?: boolean
  sort?: number
  status?: number
  itemCount?: number
  createTime?: string
  updateTime?: string
}

export interface DictItem {
  id?: number
  dictCode: string
  itemValue: string
  itemLabel: string
  color?: string
  sort?: number
  status?: number
  remark?: string
  createTime?: string
  updateTime?: string
}

export interface DictStats {
  total: number
  enabled: number
  itemTotal: number
  systemCount: number
}

export interface DictTypeListResult {
  rows: DictTypeItem[]
  total: number
  stats: DictStats
}

async function unwrap<T>(promise: Promise<{ data: ApiEnvelope<T> }>): Promise<ApiEnvelope<T>> {
  const response = await promise
  return response.data
}

export function listDictTypes(params: { keyword?: string; status?: number | null } = {}) {
  return unwrap<DictTypeListResult>(request.get('/system/dict/types', { params }))
}

export function listDictItems(dictCode: string) {
  return unwrap<DictItem[]>(request.get(`/system/dict/items/${dictCode}`))
}

export function createDictType(payload: DictTypeItem) {
  return unwrap<DictTypeItem>(request.post('/system/dict/type/create', payload))
}

export function updateDictType(payload: DictTypeItem) {
  return unwrap<DictTypeItem>(request.post('/system/dict/type/update', payload))
}

export function deleteDictType(id: number) {
  return unwrap<null>(request.post('/system/dict/type/delete', { id }))
}

export function createDictItem(payload: DictItem) {
  return unwrap<DictItem>(request.post('/system/dict/item/create', payload))
}

export function updateDictItem(payload: DictItem) {
  return unwrap<DictItem>(request.post('/system/dict/item/update', payload))
}

export function deleteDictItem(id: number) {
  return unwrap<null>(request.post('/system/dict/item/delete', { id }))
}
