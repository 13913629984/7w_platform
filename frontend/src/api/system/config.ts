import request from '@/api/request'

export interface ApiEnvelope<T> {
  code: number
  message: string
  data: T
}

export type ConfigValueType = 'text' | 'number' | 'boolean' | 'select' | 'textarea'

export interface ConfigItem {
  id?: number
  configKey: string
  configName: string
  configValue?: string
  valueType?: ConfigValueType
  options?: string
  configGroup?: string
  description?: string
  isSystem?: boolean
  sort?: number
  status?: number
  createTime?: string
  updateTime?: string
}

export interface ConfigStats {
  total: number
  enabled: number
  disabled: number
  groupCount: number
}

export interface ConfigListResult {
  rows: ConfigItem[]
  total: number
  groups: Record<string, ConfigItem[]>
  stats: ConfigStats
}

async function unwrap<T>(promise: Promise<{ data: ApiEnvelope<T> }>): Promise<ApiEnvelope<T>> {
  const response = await promise
  return response.data
}

export function listConfigs(params: { keyword?: string; group?: string } = {}) {
  return unwrap<ConfigListResult>(request.get('/system/config/list', { params }))
}

export function createConfig(payload: ConfigItem) {
  return unwrap<ConfigItem>(request.post('/system/config/create', payload))
}

export function updateConfig(payload: ConfigItem) {
  return unwrap<ConfigItem>(request.post('/system/config/update', payload))
}

export function batchSaveConfigs(items: ConfigItem[]) {
  return unwrap<number>(request.post('/system/config/batch-save', items))
}

export function deleteConfig(id: number) {
  return unwrap<null>(request.post('/system/config/delete', { id }))
}
