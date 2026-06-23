import request from '@/api/request'

export interface MaterialTypeNode {
  id?: number
  code: string
  name: string
  description?: string
  parentCode?: string | null
  parentName?: string | null
  level?: number
  sort?: number
  status?: number
  statusName?: string
  materialCount?: number
  createTime?: string
  updateTime?: string
  children?: MaterialTypeNode[]
}

export interface MaterialTypeRequest {
  id?: number
  code?: string
  name: string
  description?: string
  parentCode?: string | null
  sort?: number
  status?: number
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

export function listMaterialTypeTree(keyword?: string) {
  return unwrap<MaterialTypeNode[]>(request.get('/wms/material-type/tree', {
    params: keyword ? { keyword } : undefined,
  }))
}

export function listMaterialTypes() {
  return unwrap<MaterialTypeNode[]>(request.get('/wms/material-type/list'))
}

export function getMaterialType(id: number) {
  return unwrap<MaterialTypeNode>(request.get(`/wms/material-type/${id}`))
}

export function createMaterialType(payload: MaterialTypeRequest) {
  return unwrap<MaterialTypeNode>(request.post('/wms/material-type/create', payload))
}

export function updateMaterialType(payload: MaterialTypeRequest) {
  return unwrap<MaterialTypeNode>(request.post('/wms/material-type/update', payload))
}

export function deleteMaterialType(id: number) {
  return unwrap<null>(request.post('/wms/material-type/delete', { id }))
}

export function changeMaterialTypeStatus(id: number, status: number) {
  return unwrap<MaterialTypeNode>(request.post('/wms/material-type/status', { id, status }))
}