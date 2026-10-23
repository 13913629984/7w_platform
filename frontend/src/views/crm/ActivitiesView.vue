<template>
  <section>
    <div class="sub-page-header">
      <div>
        <h2>跟进活动</h2>
        <p class="page-desc">记录拜访、电话、会议等客户跟进活动。</p>
      </div>
      <div class="header-actions">
        <el-button plain :icon="Refresh" @click="loadData">刷新</el-button>
        <el-button type="primary" :icon="Plus" @click="openAdd">新增跟进</el-button>
      </div>
    </div>

    <div class="stats-row">
      <div class="stat-card blue">
        <div class="stat-header"><span class="stat-title">活动总数</span><div class="stat-icon">📝</div></div>
        <div class="stat-value">{{ stats.total }}</div>
        <div class="stat-footer">全部跟进记录</div>
      </div>
      <div class="stat-card orange">
        <div class="stat-header"><span class="stat-title">待处理</span><div class="stat-icon">⏳</div></div>
        <div class="stat-value">{{ stats.pending }}</div>
        <div class="stat-footer">尚未完成</div>
      </div>
      <div class="stat-card green">
        <div class="stat-header"><span class="stat-title">已完成</span><div class="stat-icon">✅</div></div>
        <div class="stat-value">{{ stats.done }}</div>
        <div class="stat-footer">已处理完成</div>
      </div>
      <div class="stat-card purple">
        <div class="stat-header"><span class="stat-title">今日活动</span><div class="stat-icon">📅</div></div>
        <div class="stat-value">{{ stats.todayCount }}</div>
        <div class="stat-footer">今日计划</div>
      </div>
      <div class="stat-card teal">
        <div class="stat-header"><span class="stat-title">涉及客户</span><div class="stat-icon">🏢</div></div>
        <div class="stat-value">{{ stats.customerCount }}个客户</div>
        <div class="stat-footer">客户覆盖</div>
      </div>
    </div>

    <div class="table-card">
      <div class="filter-bar">
        <div class="filter-left">
          <el-input
            v-model="filters.keyword"
            clearable
            placeholder="搜索主题/客户/负责人..."
            :prefix-icon="Search"
            style="width:240px"
            @keyup.enter="handleSearch"
            @clear="handleSearch"
          />
          <el-select v-model="filters.status" clearable placeholder="状态筛选" style="width:140px" @change="handleSearch">
            <el-option v-for="opt in STATUS_OPTIONS" :key="opt" :label="opt" :value="opt" />
          </el-select>
          <el-select v-model="filters.type" clearable placeholder="类型筛选" style="width:140px" @change="handleSearch">
            <el-option v-for="opt in TYPE_OPTIONS" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
          <el-select v-model="filters.customerId" clearable filterable placeholder="客户筛选" style="width:200px" @change="handleSearch">
            <el-option v-for="opt in customerOptions" :key="opt.id" :label="opt.name" :value="opt.id" />
          </el-select>
          <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
        </div>
        <div class="filter-right">
          <el-button v-if="selected.length" type="danger" plain :icon="Delete" @click="batchDelete">批量删除 ({{ selected.length }})</el-button>
        </div>
      </div>

      <el-table v-loading="loading" :data="rows" stripe style="width:100%" @selection-change="onSelectionChange">
        <el-table-column type="selection" width="46" />
        <el-table-column prop="title" label="跟进主题" min-width="180" show-overflow-tooltip>
          <template #default="{ row }"><span class="activity-title">{{ row.title }}</span></template>
        </el-table-column>
        <el-table-column label="类型" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="typeTagType(row.type)" effect="plain">{{ typeLabel(row.type) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="customerName" label="客户名称" min-width="160" show-overflow-tooltip>
          <template #default="{ row }">{{ row.customerName || '-' }}</template>
        </el-table-column>
        <el-table-column prop="owner" label="负责人" min-width="110">
          <template #default="{ row }">{{ row.owner || '-' }}</template>
        </el-table-column>
        <el-table-column prop="activityAt" label="跟进时间" width="160" align="center">
          <template #default="{ row }">{{ row.activityAt || '-' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === '已完成' ? 'success' : 'warning'" effect="light">{{ row.status || '待处理' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">{{ row.remark || '-' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="170" align="center" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">详情</el-button>
            <el-button link type="primary" @click="openEdit(row)">修改</el-button>
            <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="crm-pagination">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          background
          layout="total, sizes, prev, pager, next, jumper"
          @current-change="handlePageChange"
          @size-change="handleSizeChange"
        />
      </div>
    </div>

    <el-dialog v-model="formVisible" :title="formMode === 'add' ? '新增跟进' : '编辑跟进'" width="520px" :close-on-click-modal="false" @closed="resetForm">
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="92px" label-position="right">
        <el-form-item label="跟进主题" prop="title">
          <el-input v-model="form.title" placeholder="请输入跟进主题" maxlength="120" />
        </el-form-item>
        <el-form-item label="跟进类型" prop="type">
          <el-select v-model="form.type" placeholder="请选择跟进类型" style="width:100%">
            <el-option v-for="opt in TYPE_OPTIONS" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="客户名称" prop="customerId">
          <el-select v-model="form.customerId" clearable filterable placeholder="请选择客户" style="width:100%" @change="onCustomerChange">
            <el-option v-for="opt in customerOptions" :key="opt.id" :label="opt.name" :value="opt.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="负责人" prop="owner">
          <el-input v-model="form.owner" placeholder="请输入负责人" maxlength="50" />
        </el-form-item>
        <el-form-item label="跟进时间" prop="activityAt">
          <el-date-picker
            v-model="form.activityAt"
            type="datetime"
            placeholder="请选择跟进时间"
            value-format="YYYY-MM-DD HH:mm"
            format="YYYY-MM-DD HH:mm"
            style="width:100%"
          />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-select v-model="form.status" placeholder="请选择状态" style="width:100%">
            <el-option v-for="opt in STATUS_OPTIONS" :key="opt" :label="opt" :value="opt" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" :rows="3" placeholder="请输入备注信息" maxlength="500" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" @click="submitForm">确定提交</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="detailVisible" title="跟进详情" width="520px" :close-on-click-modal="false">
      <el-descriptions v-if="detail" :column="1" border>
        <el-descriptions-item label="跟进主题">{{ detail.title }}</el-descriptions-item>
        <el-descriptions-item label="跟进类型">{{ typeLabel(detail.type) }}</el-descriptions-item>
        <el-descriptions-item label="客户名称">{{ detail.customerName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="负责人">{{ detail.owner || '-' }}</el-descriptions-item>
        <el-descriptions-item label="跟进时间">{{ detail.activityAt || '-' }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ detail.status || '待处理' }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ detail.createTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="备注">{{ detail.remark || '-' }}</el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
        <el-button type="primary" @click="openEdit(detail!)">编辑</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Plus, Delete, Search, Refresh } from '@element-plus/icons-vue'
import {
  batchDeleteActivities,
  createActivity,
  deleteActivity,
  listActivities,
  listActivityCustomers,
  updateActivity,
  type ActivityItem,
  type CustomerOption,
} from '@/api/crm/activity'

type Activity = ActivityItem

const STATUS_OPTIONS = ['待处理', '已完成']
const TYPE_OPTIONS = [
  { value: 'visit', label: '拜访' },
  { value: 'call', label: '电话' },
  { value: 'meeting', label: '会议' },
  { value: 'email', label: '邮件' },
  { value: 'other', label: '其他' },
]

const loading = ref(false)
const rows = ref<Activity[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)
const selected = ref<Activity[]>([])
const customerOptions = ref<CustomerOption[]>([])

const filters = reactive<{ keyword: string; status: string; type: string; customerId: number | undefined }>({
  keyword: '',
  status: '',
  type: '',
  customerId: undefined,
})

const stats = reactive({ total: 0, pending: 0, done: 0, todayCount: 0, customerCount: 0 })

function typeLabel(type?: string) {
  return TYPE_OPTIONS.find((opt) => opt.value === type)?.label ?? (type || '-')
}

function typeTagType(type?: string) {
  switch (type) {
    case 'visit':
      return 'primary'
    case 'call':
      return 'success'
    case 'meeting':
      return 'warning'
    case 'email':
      return 'info'
    default:
      return 'info'
  }
}

async function loadData() {
  loading.value = true
  try {
    const res = await listActivities({
      keyword: filters.keyword.trim() || undefined,
      status: filters.status || undefined,
      type: filters.type || undefined,
      customerId: filters.customerId || undefined,
      page: page.value,
      pageSize: pageSize.value,
    })
    if (res.code !== 0) {
      ElMessage.error(res.message || '加载跟进活动失败')
      return
    }
    rows.value = res.data.rows || []
    total.value = res.data.total || 0
    if (res.data.stats) {
      stats.total = res.data.stats.total ?? 0
      stats.pending = res.data.stats.pending ?? 0
      stats.done = res.data.stats.done ?? 0
      stats.todayCount = res.data.stats.todayCount ?? 0
      stats.customerCount = res.data.stats.customerCount ?? 0
    }
  } catch (error: any) {
    ElMessage.error(error?.message || '加载跟进活动失败')
  } finally {
    loading.value = false
  }
}

async function loadCustomers() {
  try {
    const res = await listActivityCustomers()
    if (res.code === 0) {
      customerOptions.value = res.data || []
    }
  } catch {
    // 客户下拉加载失败不阻塞主流程
  }
}

function handleSearch() {
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

function onSelectionChange(selection: Activity[]) {
  selected.value = selection
}

const formVisible = ref(false)
const formMode = ref<'add' | 'edit'>('add')
const formRef = ref<FormInstance>()

function createEmptyForm(): Activity {
  return {
    id: 0,
    title: '',
    type: 'visit',
    customerId: undefined,
    customerName: '',
    owner: '',
    activityAt: '',
    status: '待处理',
    remark: '',
  }
}

const form = reactive<Activity>(createEmptyForm())

const formRules: FormRules = {
  title: [{ required: true, message: '请输入跟进主题', trigger: 'blur' }],
  type: [{ required: true, message: '请选择跟进类型', trigger: 'change' }],
}

function resetForm() {
  Object.assign(form, createEmptyForm())
  formRef.value?.clearValidate()
}

function onCustomerChange(id: number | undefined) {
  const matched = customerOptions.value.find((c) => c.id === id)
  form.customerName = matched ? matched.name : ''
}

function openAdd() {
  formMode.value = 'add'
  resetForm()
  formVisible.value = true
}

function openEdit(row: Activity) {
  formMode.value = 'edit'
  resetForm()
  Object.assign(form, createEmptyForm(), row, { customerId: row.customerId ?? undefined })
  detailVisible.value = false
  formVisible.value = true
}

async function submitForm() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  const payload: Activity = { ...form }
  try {
    if (formMode.value === 'add') {
      const res = await createActivity(payload)
      if (res.code !== 0) {
        ElMessage.error(res.message || '保存失败')
        return
      }
      ElMessage.success('跟进记录已添加')
    } else {
      const res = await updateActivity(payload)
      if (res.code !== 0) {
        ElMessage.error(res.message || '保存失败')
        return
      }
      ElMessage.success('跟进记录已更新')
    }
    formVisible.value = false
    loadData()
  } catch (error: any) {
    ElMessage.error(error?.message || '保存失败')
  }
}

const detailVisible = ref(false)
const detail = ref<Activity | null>(null)

function openDetail(row: Activity) {
  detail.value = { ...row }
  detailVisible.value = true
}

async function handleDelete(row: Activity) {
  try {
    await ElMessageBox.confirm(`确认删除跟进记录「${row.title}」吗？`, '删除确认', { type: 'warning' })
  } catch {
    return
  }
  try {
    const res = await deleteActivity(row.id!)
    if (res.code !== 0) {
      ElMessage.error(res.message || '删除失败')
      return
    }
    ElMessage.success('已删除')
    if (rows.value.length === 1 && page.value > 1) page.value -= 1
    loadData()
  } catch (error: any) {
    ElMessage.error(error?.message || '删除失败')
  }
}

async function batchDelete() {
  if (!selected.value.length) return
  try {
    await ElMessageBox.confirm(`确认批量删除 ${selected.value.length} 条跟进记录吗？`, '批量删除', { type: 'warning' })
  } catch {
    return
  }
  try {
    const ids = selected.value.map((s) => s.id!).filter(Boolean)
    const res = await batchDeleteActivities(ids)
    if (res.code !== 0) {
      ElMessage.error(res.message || '批量删除失败')
      return
    }
    ElMessage.success(`已删除 ${ids.length} 条跟进记录`)
    selected.value = []
    loadData()
  } catch (error: any) {
    ElMessage.error(error?.message || '批量删除失败')
  }
}

onMounted(() => {
  loadData()
  loadCustomers()
})
</script>

<style scoped>
.sub-page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 20px;
}
.header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}
.stats-row {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 20px;
  margin-bottom: 24px;
}
.stat-card {
  background: #fff;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, .04);
  position: relative;
  overflow: hidden;
}
.stat-card::before { content: ''; position: absolute; top: 0; left: 0; right: 0; height: 3px; }
.stat-card.blue::before { background: linear-gradient(90deg, #409eff, #66b1ff); }
.stat-card.green::before { background: linear-gradient(90deg, #67c23a, #85ce61); }
.stat-card.orange::before { background: linear-gradient(90deg, #e6a23c, #f0c78a); }
.stat-card.purple::before { background: linear-gradient(90deg, #764ba2, #667eea); }
.stat-card.teal::before { background: linear-gradient(90deg, #13c2c2, #5cdbd3); }
.stat-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}
.stat-title { font-size: 14px; color: #909399; min-width: 0; flex: 1; }
.stat-icon {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  flex-shrink: 0;
}
.stat-card.blue .stat-icon { background: #ecf5ff; }
.stat-card.green .stat-icon { background: #f0f9eb; }
.stat-card.orange .stat-icon { background: #fdf6ec; }
.stat-card.purple .stat-icon { background: #f4f0ff; }
.stat-card.teal .stat-icon { background: #e6fffb; }
.stat-value { font-size: 28px; font-weight: 700; color: #303133; margin-bottom: 8px; }
.stat-footer { font-size: 12px; color: #909399; }

.table-card {
  background: #fff;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, .04);
  min-width: 0;
}
.table-card .el-table { width: 100%; }
.activity-title { font-weight: 600; color: #303133; }
.filter-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
  flex-wrap: nowrap;
}
.filter-left {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  flex: 1;
  min-width: 0;
}
.filter-right {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}
.crm-pagination {
  display: flex;
  justify-content: flex-end;
  padding: 16px 0 4px;
}

@media (max-width: 768px) {
  .stats-row { grid-template-columns: 1fr; }
}
</style>
