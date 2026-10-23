<template>
  <section>
    <div class="sub-page-header">
      <div>
        <h2>工单管理</h2>
        <p class="page-desc">登记维修、保养、安装与巡检工单，按流程派单、处理、完工与归档。</p>
      </div>
      <el-space>
        <el-button plain :icon="Refresh" @click="loadData">刷新</el-button>
        <el-button type="primary" :icon="Plus" @click="openAdd">新建工单</el-button>
      </el-space>
    </div>

    <div class="stats-row">
      <div class="stat-card blue">
        <div class="stat-header"><span class="stat-title">工单总数</span><div class="stat-icon">🧰</div></div>
        <div class="stat-value">{{ stats.total }}</div>
        <div class="stat-footer">全部工单</div>
      </div>
      <div class="stat-card orange">
        <div class="stat-header"><span class="stat-title">待派单 / 已派单</span><div class="stat-icon">📋</div></div>
        <div class="stat-value">{{ stats.pendingCount }} / {{ stats.assignedCount }}</div>
        <div class="stat-footer">紧急 {{ stats.urgentCount }} 单</div>
      </div>
      <div class="stat-card purple">
        <div class="stat-header"><span class="stat-title">处理中</span><div class="stat-icon">⚙️</div></div>
        <div class="stat-value">{{ stats.processingCount }}</div>
        <div class="stat-footer">逾期 {{ stats.overdueCount }} 单</div>
      </div>
      <div class="stat-card green">
        <div class="stat-header"><span class="stat-title">本月完成</span><div class="stat-icon">🎯</div></div>
        <div class="stat-value">{{ stats.monthCompletedCount }}</div>
        <div class="stat-footer">已完成 {{ stats.completedCount }} · 已关闭 {{ stats.closedCount }}</div>
      </div>
    </div>

    <div class="table-card">
      <div class="filter-bar">
        <div class="filter-left">
          <el-input v-model="keyword" clearable placeholder="搜索工单号/标题/设备/客户/处理人" style="width:300px" :prefix-icon="Search" @keyup.enter="searchData" @clear="searchData" />
          <el-select v-model="statusFilter" clearable placeholder="工单状态" style="width:130px" @change="searchData">
            <el-option v-for="opt in statusOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
          <el-select v-model="priorityFilter" clearable placeholder="优先级" style="width:110px" @change="searchData">
            <el-option v-for="opt in priorityOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
          <el-select v-model="typeFilter" clearable placeholder="工单类型" style="width:110px" @change="searchData">
            <el-option v-for="opt in typeOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
          <el-button type="primary" :icon="Search" @click="searchData">查询</el-button>
        </div>
      </div>

      <el-table v-loading="loading" :data="rows" stripe border style="width:100%" empty-text="暂无工单数据">
        <el-table-column prop="code" label="工单号" width="150" />
        <el-table-column prop="title" label="工单标题" min-width="200" show-overflow-tooltip>
          <template #default="{ row }"><b>{{ row.title }}</b></template>
        </el-table-column>
        <el-table-column prop="equipmentName" label="关联设备" min-width="170" show-overflow-tooltip />
        <el-table-column prop="customerName" label="客户" min-width="160" show-overflow-tooltip />
        <el-table-column label="类型" width="90" align="center">
          <template #default="{ row }"><el-tag effect="plain" round>{{ typeLabel(row.type) }}</el-tag></template>
        </el-table-column>
        <el-table-column label="优先级" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="priorityTagType(row.priority)" effect="light">{{ priorityLabel(row.priority) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" effect="light">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="handler" label="处理人" width="100" />
        <el-table-column prop="reportTime" label="报修时间" width="150" />
        <el-table-column prop="planTime" label="计划完成" width="150" />
        <el-table-column label="操作" width="250" fixed="right" align="center">
          <template #default="{ row }">
            <el-button v-if="nextStatus(row.status)" link type="success" @click="handleAdvance(row)">{{ nextActionLabel(row.status) }}</el-button>
            <el-button link type="primary" @click="openDetail(row)">详情</el-button>
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="wms-pagination">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[10, 20, 50, 100]"
          background
          layout="total, sizes, prev, pager, next, jumper"
          @current-change="handlePageChange"
          @size-change="handleSizeChange"
        />
      </div>
    </div>

    <el-dialog v-model="formVisible" :title="formMode === 'add' ? '新建工单' : '编辑工单'" width="860px" :close-on-click-modal="false" @closed="resetForm">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-row :gutter="20">
          <el-col :span="24"><el-form-item label="工单标题" prop="title"><el-input v-model="form.title" maxlength="200" /></el-form-item></el-col>
          <el-col :span="12">
            <el-form-item label="关联设备" prop="equipmentId">
              <el-select v-model="form.equipmentId" clearable filterable placeholder="请选择设备" style="width:100%" @change="handleEquipmentChange">
                <el-option v-for="opt in equipmentOptions" :key="opt.id" :label="`${opt.code} ${opt.name}`" :value="opt.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12"><el-form-item label="客户名称"><el-input v-model="form.customerName" maxlength="100" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="工单类型"><el-select v-model="form.type" placeholder="请选择类型" style="width:100%"><el-option v-for="opt in typeOptions" :key="opt.value" :label="opt.label" :value="opt.value" /></el-select></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="优先级"><el-select v-model="form.priority" placeholder="请选择优先级" style="width:100%"><el-option v-for="opt in priorityOptions" :key="opt.value" :label="opt.label" :value="opt.value" /></el-select></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="报修人"><el-input v-model="form.reporter" maxlength="50" /></el-form-item></el-col>
          <el-col :span="12">
            <el-form-item label="处理人">
              <el-select v-model="form.handler" clearable filterable placeholder="请选择处理人" style="width:100%">
                <el-option v-for="opt in handlerOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12"><el-form-item label="报修时间"><el-date-picker v-model="form.reportTime" type="datetime" value-format="YYYY-MM-DD[T]HH:mm:ss" placeholder="请选择时间" style="width:100%" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="计划完成"><el-date-picker v-model="form.planTime" type="datetime" value-format="YYYY-MM-DD[T]HH:mm:ss" placeholder="请选择时间" style="width:100%" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="完成时间"><el-date-picker v-model="form.finishTime" type="datetime" value-format="YYYY-MM-DD[T]HH:mm:ss" placeholder="请选择时间" style="width:100%" /></el-form-item></el-col>
          <el-col :span="6"><el-form-item label="工时(h)"><el-input-number v-model="form.hours" :min="0" :precision="1" :step="0.5" controls-position="right" style="width:100%" /></el-form-item></el-col>
          <el-col :span="6"><el-form-item label="费用(元)"><el-input-number v-model="form.cost" :min="0" :precision="2" :step="10" controls-position="right" style="width:100%" /></el-form-item></el-col>
          <el-col :span="24"><el-form-item label="故障描述"><el-input v-model="form.faultDesc" type="textarea" :rows="2" maxlength="500" show-word-limit /></el-form-item></el-col>
          <el-col :span="24"><el-form-item label="解决方案"><el-input v-model="form.solution" type="textarea" :rows="2" maxlength="500" show-word-limit /></el-form-item></el-col>
          <el-col :span="24"><el-form-item label="备注"><el-input v-model="form.remark" type="textarea" :rows="2" maxlength="500" show-word-limit /></el-form-item></el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitForm">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="detailVisible" title="工单详情" width="800px">
      <el-descriptions v-if="detail" :column="2" border>
        <el-descriptions-item label="工单号">{{ detail.code || '-' }}</el-descriptions-item>
        <el-descriptions-item label="工单标题">{{ detail.title }}</el-descriptions-item>
        <el-descriptions-item label="关联设备">{{ detail.equipmentCode || '-' }} {{ detail.equipmentName || '' }}</el-descriptions-item>
        <el-descriptions-item label="客户名称">{{ detail.customerName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="工单类型">{{ typeLabel(detail.type) }}</el-descriptions-item>
        <el-descriptions-item label="优先级">
          <el-tag :type="priorityTagType(detail.priority)" effect="light">{{ priorityLabel(detail.priority) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="当前状态">
          <el-tag :type="statusTagType(detail.status)" effect="light">{{ statusLabel(detail.status) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="处理人">{{ detail.handler || '-' }}</el-descriptions-item>
        <el-descriptions-item label="报修人">{{ detail.reporter || '-' }}</el-descriptions-item>
        <el-descriptions-item label="报修时间">{{ detail.reportTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="计划完成">{{ detail.planTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="完成时间">{{ detail.finishTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="工时">{{ detail.hours ?? '-' }} h</el-descriptions-item>
        <el-descriptions-item label="费用">{{ formatMoney(detail.cost) }}</el-descriptions-item>
        <el-descriptions-item label="故障描述" :span="2">{{ detail.faultDesc || '-' }}</el-descriptions-item>
        <el-descriptions-item label="解决方案" :span="2">{{ detail.solution || '-' }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ detail.remark || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </section>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'
import {
  changeWorkOrderStatus,
  createWorkOrder,
  deleteWorkOrder,
  getHandlerOptions,
  getWorkOrder,
  getWorkOrderEquipmentOptions,
  getWorkOrderStats,
  listWorkOrders,
  updateWorkOrder,
  type WorkOrderEquipmentOption,
  type WorkOrderItem,
  type WorkOrderStats,
} from '@/api/maint/workOrder'

const statusOptions = [
  { value: 'pending', label: '待派单' },
  { value: 'assigned', label: '已派单' },
  { value: 'processing', label: '处理中' },
  { value: 'completed', label: '已完成' },
  { value: 'closed', label: '已关闭' },
]

const priorityOptions = [
  { value: 'low', label: '低' },
  { value: 'normal', label: '普通' },
  { value: 'high', label: '高' },
  { value: 'urgent', label: '紧急' },
]

const typeOptions = [
  { value: 'repair', label: '维修' },
  { value: 'preventive', label: '保养' },
  { value: 'install', label: '安装' },
  { value: 'inspection', label: '巡检' },
]

/** 后端状态流转顺序：pending → assigned → processing → completed → closed */
const STATUS_FLOW = ['pending', 'assigned', 'processing', 'completed', 'closed'] as const
const NEXT_ACTION: Record<string, string> = {
  pending: '派单',
  assigned: '开始处理',
  processing: '完成',
  completed: '关闭',
}

const loading = ref(false)
const submitting = ref(false)
const rows = ref<WorkOrderItem[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)
const keyword = ref('')
const statusFilter = ref('')
const priorityFilter = ref('')
const typeFilter = ref('')
const formVisible = ref(false)
const detailVisible = ref(false)
const formMode = ref<'add' | 'edit'>('add')
const formRef = ref<FormInstance>()
const detail = ref<WorkOrderItem | null>(null)
const equipmentOptions = ref<WorkOrderEquipmentOption[]>([])
const handlerOptions = ref<{ value: string; label: string }[]>([])
const stats = ref<WorkOrderStats>({
  total: 0,
  pendingCount: 0,
  assignedCount: 0,
  processingCount: 0,
  completedCount: 0,
  closedCount: 0,
  urgentCount: 0,
  overdueCount: 0,
  monthCompletedCount: 0,
})

const form = reactive<WorkOrderItem>(emptyForm())

const rules: FormRules = {
  title: [{ required: true, message: '请填写工单标题', trigger: 'blur' }],
}

function emptyForm(): WorkOrderItem {
  return {
    title: '',
    equipmentId: null,
    customerName: '',
    type: 'repair',
    priority: 'normal',
    reporter: '',
    handler: '',
    reportTime: '',
    planTime: '',
    finishTime: '',
    hours: null,
    cost: null,
    faultDesc: '',
    solution: '',
    remark: '',
  }
}

function statusLabel(value?: string) {
  const found = statusOptions.find((opt) => opt.value === value)
  return found ? found.label : value || '-'
}

function statusTagType(value?: string): 'success' | 'info' | 'warning' | 'danger' | 'primary' {
  switch (value) {
    case 'pending':
      return 'warning'
    case 'assigned':
      return 'primary'
    case 'processing':
      return 'info'
    case 'completed':
      return 'success'
    case 'closed':
      return 'info'
    default:
      return 'info'
  }
}

function priorityLabel(value?: string) {
  const found = priorityOptions.find((opt) => opt.value === value)
  return found ? found.label : value || '-'
}

function priorityTagType(value?: string): 'primary' | 'success' | 'info' | 'warning' | 'danger' {
  switch (value) {
    case 'urgent':
      return 'danger'
    case 'high':
      return 'warning'
    case 'normal':
      return 'primary'
    case 'low':
      return 'info'
    default:
      return 'info'
  }
}

function typeLabel(value?: string) {
  const found = typeOptions.find((opt) => opt.value === value)
  return found ? found.label : value || '-'
}

function nextStatus(current?: string) {
  const index = STATUS_FLOW.indexOf((current || '') as (typeof STATUS_FLOW)[number])
  if (index < 0 || index >= STATUS_FLOW.length - 1) return ''
  return STATUS_FLOW[index + 1]
}

function nextActionLabel(current?: string) {
  return NEXT_ACTION[current || ''] || ''
}

function formatMoney(value?: number | null) {
  if (value === null || value === undefined) return '-'
  return `¥${Number(value).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`
}

function resetForm() {
  Object.assign(form, emptyForm())
  formRef.value?.clearValidate()
}

async function loadOptions() {
  try {
    const [equipRes, handlerRes] = await Promise.all([getWorkOrderEquipmentOptions(), getHandlerOptions()])
    if (equipRes.code === 0) equipmentOptions.value = equipRes.data || []
    if (handlerRes.code === 0) handlerOptions.value = handlerRes.data || []
  } catch (error: any) {
    ElMessage.error(error?.message || '加载下拉选项失败')
  }
}

async function loadData() {
  loading.value = true
  try {
    const [listRes, statsRes] = await Promise.all([
      listWorkOrders({
        keyword: keyword.value.trim() || undefined,
        status: statusFilter.value || undefined,
        priority: priorityFilter.value || undefined,
        type: typeFilter.value || undefined,
        page: page.value,
        pageSize: pageSize.value,
      }),
      getWorkOrderStats(),
    ])
    if (listRes.code !== 0) {
      ElMessage.error(listRes.message || '加载工单失败')
      return
    }
    if (statsRes.code === 0) stats.value = statsRes.data
    rows.value = listRes.data?.rows || []
    total.value = listRes.data?.total || 0
  } catch (error: any) {
    ElMessage.error(error?.message || '加载工单失败')
  } finally {
    loading.value = false
  }
}

function searchData() {
  page.value = 1
  loadData()
}

function handlePageChange(value: number) {
  page.value = value
  loadData()
}

function handleSizeChange(value: number) {
  pageSize.value = value
  page.value = 1
  loadData()
}

function handleEquipmentChange(value: number | null | undefined) {
  const found = equipmentOptions.value.find((opt) => opt.id === value)
  if (found && found.customerName && !form.customerName) {
    form.customerName = found.customerName
  }
}

function openAdd() {
  formMode.value = 'add'
  resetForm()
  formVisible.value = true
}

function openEdit(row: WorkOrderItem) {
  formMode.value = 'edit'
  resetForm()
  Object.assign(form, row)
  formVisible.value = true
}

async function openDetail(row: WorkOrderItem) {
  if (!row.id) return
  try {
    const res = await getWorkOrder(row.id)
    if (res.code !== 0) {
      ElMessage.error(res.message || '加载工单详情失败')
      return
    }
    detail.value = res.data
    detailVisible.value = true
  } catch (error: any) {
    ElMessage.error(error?.message || '加载工单详情失败')
  }
}

async function submitForm() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    const payload: WorkOrderItem = { ...form }
    const res = formMode.value === 'add' ? await createWorkOrder(payload) : await updateWorkOrder(payload)
    if (res.code !== 0) {
      ElMessage.error(res.message || '保存失败')
      return
    }
    ElMessage.success(formMode.value === 'add' ? '工单已创建' : '工单已更新')
    formVisible.value = false
    await loadData()
  } catch (error: any) {
    ElMessage.error(error?.message || '保存失败')
  } finally {
    submitting.value = false
  }
}

async function handleAdvance(row: WorkOrderItem) {
  if (!row.id) return
  const target = nextStatus(row.status)
  if (!target) return
  const actionLabel = nextActionLabel(row.status)
  let handler = row.handler || ''
  if (target === 'assigned' && !handler) {
    try {
      const { value } = await ElMessageBox.prompt('派单前请指定处理人', '派单', {
        inputPlaceholder: '请输入处理人姓名',
        inputValidator: (val: string) => (val && val.trim() ? true : '请填写处理人'),
      })
      handler = value.trim()
    } catch {
      return
    }
  }
  try {
    await ElMessageBox.confirm(`确认将工单 ${row.code || row.title} ${actionLabel}（${statusLabel(row.status)} → ${statusLabel(target)}）？`, '状态流转', { type: 'warning' })
  } catch {
    return
  }
  try {
    const res = await changeWorkOrderStatus({ id: row.id, status: target, handler: handler || undefined })
    if (res.code !== 0) {
      ElMessage.error(res.message || '状态更新失败')
      return
    }
    ElMessage.success(`工单已${actionLabel}`)
    await loadData()
  } catch (error: any) {
    ElMessage.error(error?.message || '状态更新失败')
  }
}

async function handleDelete(row: WorkOrderItem) {
  if (!row.id) return
  try {
    await ElMessageBox.confirm(`确认删除工单 ${row.code || row.title} 吗？`, '删除确认', { type: 'warning' })
  } catch {
    return
  }
  try {
    const res = await deleteWorkOrder(row.id)
    if (res.code !== 0) {
      ElMessage.error(res.message || '删除失败')
      return
    }
    ElMessage.success('工单已删除')
    await loadData()
  } catch (error: any) {
    ElMessage.error(error?.message || '删除失败')
  }
}

onMounted(() => {
  loadOptions()
  loadData()
})
</script>

<style scoped>
.wms-pagination {
  display: flex;
  justify-content: flex-end;
  padding: 16px 0 4px;
}
</style>
