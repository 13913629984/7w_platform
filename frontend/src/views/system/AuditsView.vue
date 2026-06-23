<template>
  <section>
    <div class="sub-page-header">
      <div>
        <h2>审计日志</h2>
        <p class="page-desc">查看登录、操作与安全审计记录，支持按时间、模块、类型和状态追踪。</p>
      </div>
      <div>
        <el-button size="small" plain @click="exportCsv">导出日志</el-button>
        <el-button size="small" plain @click="loadLogs">刷新</el-button>
      </div>
    </div>

    <div class="stats-row">
      <div class="stat-card blue">
        <div class="stat-header"><span class="stat-title">今日操作</span></div>
        <div class="stat-value">{{ stats.today }}</div>
        <div class="stat-footer"><span class="up">实时统计</span></div>
      </div>
      <div class="stat-card green">
        <div class="stat-header"><span class="stat-title">成功操作</span></div>
        <div class="stat-value">{{ stats.success }}</div>
        <div class="stat-footer"><span class="up">状态：成功</span></div>
      </div>
      <div class="stat-card orange">
        <div class="stat-header"><span class="stat-title">失败操作</span></div>
        <div class="stat-value">{{ stats.fail }}</div>
        <div class="stat-footer"><span class="down">状态：失败</span></div>
      </div>
      <div class="stat-card red">
        <div class="stat-header"><span class="stat-title">异常告警</span></div>
        <div class="stat-value">{{ stats.warn }}</div>
        <div class="stat-footer"><span class="down">需关注</span></div>
      </div>
    </div>

    <div class="table-card audit-table-card">
      <div class="filter-bar audit-filter-bar">
        <div class="filter-left">
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            value-format="YYYY-MM-DD"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            style="width: 260px"
            @change="handleSearch"
          />
          <el-input v-model="keyword" style="width: 220px" clearable placeholder="搜索操作内容/用户/IP" prefix-icon="Search" @input="handleSearch" />
          <el-select v-model="moduleCode" style="width: 130px" placeholder="模块筛选" @change="handleSearch">
            <el-option label="全部模块" value="" />
            <el-option label="系统" value="SYS" />
            <el-option label="WMS" value="WMS" />
            <el-option label="CRM" value="CRM" />
            <el-option label="维保" value="MAINT" />
            <el-option label="电测" value="ETS" />
            <el-option label="财务" value="FIN" />
          </el-select>
          <el-select v-model="actionType" style="width: 120px" placeholder="操作类型" @change="handleSearch">
            <el-option label="全部" value="" />
            <el-option label="登录" value="LOGIN" />
            <el-option label="新增" value="CREATE" />
            <el-option label="修改" value="UPDATE" />
            <el-option label="删除" value="DELETE" />
            <el-option label="查询" value="QUERY" />
          </el-select>
          <el-select v-model="status" style="width: 110px" placeholder="状态" @change="handleSearch">
            <el-option label="全部" value="" />
            <el-option label="成功" value="success" />
            <el-option label="失败" value="fail" />
            <el-option label="待确认" value="pending" />
          </el-select>
        </div>
        <el-space>
          <el-button @click="resetFilters">重置</el-button>
          <el-button type="primary" @click="loadLogs">查询</el-button>
        </el-space>
      </div>

      <div class="audit-list-wrapper" v-loading="loading">
        <table class="audit-log-table">
          <thead>
            <tr>
              <th style="width: 160px">时间</th>
              <th style="width: 80px">用户</th>
              <th style="width: 64px">模块</th>
              <th style="width: 80px">操作类型</th>
              <th>操作内容</th>
              <th style="width: 82px">操作对象</th>
              <th style="width: 120px">IP地址</th>
              <th style="width: 60px">状态</th>
              <th style="width: 74px">耗时</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in logs" :key="row.id" @click="openDetail(row)">
              <td>{{ row.operateTime }}</td>
              <td>{{ row.username }}</td>
              <td><span class="audit-pill" :class="moduleClass(row.moduleCode)">{{ row.moduleName }}</span></td>
              <td>
                <span v-if="['LOGIN', 'UPDATE', 'DELETE'].includes(row.actionType)" class="audit-pill" :class="actionClass(row.actionType)">{{ row.actionName }}</span>
                <span v-else>{{ row.actionName }}</span>
              </td>
              <td>{{ row.content }}</td>
              <td>{{ row.targetName }}</td>
              <td>{{ row.ipAddress }}</td>
              <td><span class="audit-pill" :class="statusClass(row.status)">{{ statusText(row.status) }}</span></td>
              <td>{{ row.costText }}</td>
            </tr>
            <tr v-if="!loading && logs.length === 0">
              <td colspan="9" class="empty-cell">暂无审计日志</td>
            </tr>
          </tbody>
        </table>
      </div>

      <div class="audit-pagination">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="pageSize"
          layout="total, sizes, prev, pager, next"
          :page-sizes="[6, 10, 20, 50]"
          :total="total"
          @current-change="loadLogs"
          @size-change="handleSizeChange"
        />
      </div>
    </div>

    <el-dialog v-model="detailVisible" title="日志详情" width="680px">
      <el-descriptions v-if="detail" :column="2" border>
        <el-descriptions-item label="日志编号">LOG-{{ String(detail.id).padStart(6, '0') }}</el-descriptions-item>
        <el-descriptions-item label="操作时间">{{ detail.operateTime }}</el-descriptions-item>
        <el-descriptions-item label="操作用户">{{ detail.username }}</el-descriptions-item>
        <el-descriptions-item label="操作模块">{{ detail.moduleName }}</el-descriptions-item>
        <el-descriptions-item label="操作类型">{{ detail.actionName }}</el-descriptions-item>
        <el-descriptions-item label="操作对象">{{ detail.targetName }}</el-descriptions-item>
        <el-descriptions-item label="IP地址">{{ detail.ipAddress }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ statusText(detail.status) }}</el-descriptions-item>
        <el-descriptions-item label="操作耗时">{{ detail.costText }}</el-descriptions-item>
        <el-descriptions-item label="浏览器">{{ detail.browser || '-' }}</el-descriptions-item>
        <el-descriptions-item label="请求ID" :span="2">{{ detail.requestId || '-' }}</el-descriptions-item>
        <el-descriptions-item label="操作内容" :span="2">{{ detail.content }}</el-descriptions-item>
        <el-descriptions-item label="操作详情" :span="2">
          <pre>{{ detail.detail || '-' }}</pre>
        </el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup lang="ts">
import { ElMessage } from 'element-plus'
import { onMounted, ref } from 'vue'
import request from '@/api/request'

type AuditLogRow = {
  id: number
  operateTime: string
  username: string
  moduleCode: string
  moduleName: string
  actionType: string
  actionName: string
  content: string
  targetName: string
  ipAddress: string
  status: string
  costMs: number
  costText: string
  requestId?: string
  browser?: string
  detail?: string
}

type AuditStats = {
  today: number
  success: number
  fail: number
  warn: number
}

const loading = ref(false)
const logs = ref<AuditLogRow[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(6)
const keyword = ref('')
const moduleCode = ref('')
const actionType = ref('')
const status = ref('')
const dateRange = ref<string[]>([])
const stats = ref<AuditStats>({ today: 0, success: 0, fail: 0, warn: 0 })
const detailVisible = ref(false)
const detail = ref<AuditLogRow | null>(null)

async function loadLogs() {
  loading.value = true
  try {
    const { data } = await request.get('/system/audit/list', {
      params: {
        keyword: keyword.value,
        moduleCode: moduleCode.value,
        actionType: actionType.value,
        status: status.value,
        startDate: dateRange.value?.[0] || '',
        endDate: dateRange.value?.[1] || '',
        page: page.value,
        pageSize: pageSize.value,
      },
    })
    if (data.code === 0) {
      logs.value = data.data.list
      total.value = data.data.total
      stats.value = { today: 0, success: 0, fail: 0, warn: 0, ...data.data.stats }
    } else {
      ElMessage.error(data.message || '加载审计日志失败')
    }
  } catch (error) {
    console.error('Load audit logs error:', error)
    ElMessage.error('加载审计日志失败')
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  page.value = 1
  loadLogs()
}

function handleSizeChange() {
  page.value = 1
  loadLogs()
}

function resetFilters() {
  keyword.value = ''
  moduleCode.value = ''
  actionType.value = ''
  status.value = ''
  dateRange.value = []
  page.value = 1
  loadLogs()
}

async function openDetail(row: AuditLogRow) {
  detail.value = row
  detailVisible.value = true
  try {
    const { data } = await request.get(`/system/audit/${row.id}`)
    if (data.code === 0) {
      detail.value = data.data
    }
  } catch (error) {
    console.error('Load audit detail error:', error)
  }
}

function exportCsv() {
  if (!logs.value.length) {
    ElMessage.warning('暂无可导出的日志')
    return
  }
  const headers = ['时间', '用户', '模块', '操作类型', '操作内容', '操作对象', 'IP地址', '状态', '耗时']
  const rows = logs.value.map(row => [
    row.operateTime,
    row.username,
    row.moduleName,
    row.actionName,
    row.content,
    row.targetName,
    row.ipAddress,
    statusText(row.status),
    row.costText,
  ])
  const csv = [headers, ...rows].map(cols => cols.map(escapeCsv).join(',')).join('\n')
  const blob = new Blob([`\ufeff${csv}`], { type: 'text/csv;charset=utf-8;' })
  const link = document.createElement('a')
  link.href = URL.createObjectURL(blob)
  link.download = `审计日志-${new Date().toISOString().slice(0, 10)}.csv`
  link.click()
  URL.revokeObjectURL(link.href)
}

function escapeCsv(value: string) {
  return `"${String(value ?? '').replace(/"/g, '""')}"`
}

function moduleClass(value: string) {
  return {
    'module-system': value === 'SYS',
    'module-wms': value === 'WMS',
    'module-crm': value === 'CRM',
    'module-default': !['SYS', 'WMS', 'CRM'].includes(value),
  }
}

function actionClass(value: string) {
  return {
    'action-login': value === 'LOGIN',
    'action-update': value === 'UPDATE',
    'action-delete': value === 'DELETE',
  }
}

function statusClass(value: string) {
  return {
    'status-success': value === 'success',
    'status-fail': value === 'fail',
    'status-pending': value === 'pending',
  }
}

function statusText(value: string) {
  const map: Record<string, string> = { success: '成功', fail: '失败', pending: '待确认' }
  return map[value] || value
}

onMounted(() => {
  loadLogs()
})
</script>

<style scoped>
.audit-table-card {
  padding: 22px 24px 18px;
}

.audit-filter-bar {
  align-items: flex-start;
}

.audit-list-wrapper {
  width: 100%;
  min-height: 360px;
  overflow-x: auto;
}

.audit-log-table {
  width: 100%;
  min-width: 900px;
  border-collapse: collapse;
  table-layout: fixed;
  font-size: 14px;
  color: #111827;
}

.audit-log-table thead {
  background: #fafafa;
}

.audit-log-table th {
  height: 42px;
  padding: 0 8px;
  color: #334155;
  font-weight: 500;
  text-align: left;
  border-bottom: 1px solid #e5e7eb;
}

.audit-log-table td {
  height: 52px;
  padding: 5px 8px;
  color: #111827;
  line-height: 1.45;
  vertical-align: middle;
  border-bottom: 1px solid #e5e7eb;
  word-break: break-all;
}

.audit-log-table tbody tr {
  cursor: pointer;
  transition: background-color .2s;
}

.audit-log-table tbody tr:hover {
  background: #f8fafc;
}

.audit-pill {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 44px;
  height: 20px;
  padding: 0 9px;
  border-radius: 999px;
  font-size: 12px;
  line-height: 20px;
  white-space: nowrap;
  box-sizing: border-box;
}

.module-system,
.module-default {
  color: #64748b;
  background: #f3f4f6;
}

.module-wms {
  color: #3b82f6;
  background: #eff6ff;
}

.module-crm {
  color: #f59e0b;
  background: #fff7ed;
}

.action-login {
  color: #1683ff;
  background: #edf6ff;
}

.action-update {
  color: #f59e0b;
  background: #fff7ed;
}

.action-delete,
.status-fail {
  color: #f56c6c;
  background: #fef0f0;
}

.status-success {
  min-width: 38px;
  color: #52c41a;
  background: #f0fbe8;
}

.status-pending {
  color: #e6a23c;
  background: #fdf6ec;
}

.empty-cell {
  height: 160px !important;
  color: #909399 !important;
  text-align: center;
  cursor: default;
}

.audit-pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

pre {
  margin: 0;
  padding: 10px;
  border-radius: 6px;
  background: #f5f7fa;
  white-space: pre-wrap;
  word-break: break-all;
}
</style>
