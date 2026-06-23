<template>
  <section>
    <div class="sub-page-header">
      <div>
        <h2>商机管理</h2>
        <p class="page-desc">管理销售机会、阶段和预计金额。</p>
      </div>
      <div class="header-actions">
        <el-button plain :icon="Download" @click="handleExport">导出</el-button>
        <el-button plain :icon="Refresh" @click="loadData">刷新</el-button>
        <el-button type="primary" :icon="Plus" @click="openAdd">新增商机</el-button>
      </div>
    </div>

    <div class="stats-row">
      <div class="stat-card blue">
        <div class="stat-header"><span class="stat-title">商机总数</span><div class="stat-icon">📊</div></div>
        <div class="stat-value">{{ stats.total }}</div>
        <div class="stat-footer">全部商机</div>
      </div>
      <div class="stat-card green">
        <div class="stat-header"><span class="stat-title">本月新增</span><div class="stat-icon">📈</div></div>
        <div class="stat-value">{{ stats.monthAdded }}</div>
        <div class="stat-footer">新商机</div>
      </div>
      <div class="stat-card orange">
        <div class="stat-header"><span class="stat-title">预计金额</span><div class="stat-icon">💰</div></div>
        <div class="stat-value">¥{{ stats.totalAmount }}万</div>
        <div class="stat-footer">总预估</div>
      </div>
      <div class="stat-card purple">
        <div class="stat-header"><span class="stat-title">平均胜率</span><div class="stat-icon">🎯</div></div>
        <div class="stat-value">{{ stats.avgWinRate }}%</div>
        <div class="stat-footer">胜率统计</div>
      </div>
    </div>

    <div class="table-card">
      <div class="filter-bar">
        <div class="filter-left">
          <el-input
            v-model="filters.keyword"
            clearable
            placeholder="搜索商机名称/客户..."
            :prefix-icon="Search"
            style="width:240px"
            @keyup.enter="handleSearch"
            @clear="handleSearch"
          />
          <el-select v-model="filters.stage" clearable placeholder="阶段筛选" style="width:160px" @change="handleSearch">
            <el-option v-for="opt in stages" :key="opt" :label="opt" :value="opt" />
          </el-select>
          <el-select v-model="filters.owner" clearable placeholder="负责人" style="width:140px" @change="handleSearch">
            <el-option v-for="opt in ownerOptions" :key="opt" :label="opt" :value="opt" />
          </el-select>
        </div>
        <div class="filter-right">
          <el-button v-if="selected.length" type="danger" plain :icon="Delete" @click="batchDelete">批量删除 ({{ selected.length }})</el-button>
        </div>
      </div>

      <el-table v-loading="loading" :data="rows" stripe style="width:100%" @selection-change="onSelectionChange">
        <el-table-column type="selection" width="46" />
        <el-table-column prop="name" label="商机名称" min-width="160" show-overflow-tooltip>
          <template #default="{ row }"><span class="deal-name">{{ row.name }}</span></template>
        </el-table-column>
        <el-table-column prop="customerName" label="所属客户" min-width="150" show-overflow-tooltip>
          <template #default="{ row }">{{ row.customerName || '-' }}</template>
        </el-table-column>
        <el-table-column label="关联线索" min-width="120" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.leadName" type="info" effect="light" size="small">{{ row.leadName }}</el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="预计金额" width="110" align="right">
          <template #default="{ row }"><span class="amount">¥{{ formatAmount(row.amount) }}万</span></template>
        </el-table-column>
        <el-table-column label="销售阶段" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="stageTagType(row.stage)" effect="light">{{ row.stage || '-' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="胜率" width="80" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.winRate != null" type="success" effect="plain">{{ row.winRate }}%</el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="owner" label="负责人" width="90" align="center" />
        <el-table-column prop="expectDealAt" label="预计成交" width="120" align="center">
          <template #default="{ row }">{{ row.expectDealAt || '-' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === '已转订单' ? 'success' : 'warning'" effect="light">{{ row.status || '进行中' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" align="center" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">详情</el-button>
            <el-button link type="primary" @click="openEdit(row)">修改</el-button>
            <el-button v-if="canConvert(row)" link type="success" @click="handleConvert(row)">转订单</el-button>
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

    <el-dialog v-model="formVisible" :title="formMode === 'add' ? '新增商机' : '编辑商机'" width="520px" :close-on-click-modal="false" @closed="resetForm">
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="92px" label-position="right">
        <el-form-item label="商机名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入商机名称" maxlength="100" />
        </el-form-item>
        <el-form-item label="所属客户" prop="customerName">
          <el-select
            v-model="form.customerName"
            filterable
            allow-create
            default-first-option
            placeholder="请输入客户名称"
            style="width:100%"
            @change="onCustomerChange"
          >
            <el-option v-for="opt in customerOptions" :key="opt.id" :label="opt.name" :value="opt.name" />
          </el-select>
        </el-form-item>
        <el-form-item label="关联线索" prop="leadName">
          <el-select v-model="form.leadName" clearable filterable allow-create default-first-option placeholder="选择关联线索" style="width:100%">
            <el-option v-for="opt in leadOptions" :key="opt" :label="opt" :value="opt" />
          </el-select>
          <div class="form-tip">从线索转化的商机将自动关联</div>
        </el-form-item>
        <el-form-item label="预计金额" prop="amount">
          <el-input-number v-model="form.amount" :min="0" :step="10" :precision="0" controls-position="right" style="width:100%" />
          <span class="unit-tip">万元</span>
        </el-form-item>
        <el-form-item label="销售阶段" prop="stage">
          <el-select v-model="form.stage" placeholder="请选择销售阶段" style="width:100%">
            <el-option v-for="opt in stages" :key="opt" :label="opt" :value="opt" />
          </el-select>
        </el-form-item>
        <el-form-item label="胜率" prop="winRate">
          <el-input-number v-model="form.winRate" :min="0" :max="100" :step="5" controls-position="right" placeholder="请输入胜率，如：75%" style="width:100%" />
          <span class="unit-tip">%</span>
        </el-form-item>
        <el-form-item label="负责人" prop="owner">
          <el-select v-model="form.owner" clearable filterable allow-create default-first-option placeholder="请选择负责人" style="width:100%">
            <el-option v-for="opt in ownerOptions" :key="opt" :label="opt" :value="opt" />
          </el-select>
        </el-form-item>
        <el-form-item label="预计成交日期" prop="expectDealAt">
          <el-date-picker v-model="form.expectDealAt" type="date" value-format="YYYY-MM-DD" placeholder="选择日期" style="width:100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" @click="submitForm">确定提交</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="detailVisible" title="商机详情" width="540px" :close-on-click-modal="false">
      <el-descriptions v-if="detail" :column="1" border>
        <el-descriptions-item label="商机名称">{{ detail.name }}</el-descriptions-item>
        <el-descriptions-item label="所属客户">{{ detail.customerName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="关联线索">{{ detail.leadName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="预计金额">¥{{ formatAmount(detail.amount) }}万</el-descriptions-item>
        <el-descriptions-item label="销售阶段">{{ detail.stage || '-' }}</el-descriptions-item>
        <el-descriptions-item label="胜率">{{ detail.winRate != null ? detail.winRate + '%' : '-' }}</el-descriptions-item>
        <el-descriptions-item label="负责人">{{ detail.owner || '-' }}</el-descriptions-item>
        <el-descriptions-item label="预计成交">{{ detail.expectDealAt || '-' }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ detail.status || '进行中' }}</el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
        <el-button type="primary" @click="openEdit(detail!)">编辑</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Plus, Delete, Search, Download, Refresh } from '@element-plus/icons-vue'
import {
  batchDeleteDeals,
  convertDeal,
  createDeal,
  deleteDeal,
  listDealCustomers,
  listDeals,
  updateDeal,
  type CustomerOption,
  type DealItem,
} from '@/api/crm/deal'

type Deal = DealItem

const loading = ref(false)
const rows = ref<Deal[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)
const selected = ref<Deal[]>([])
const customerOptions = ref<CustomerOption[]>([])
const stages = ref<string[]>(['需求确认', '方案报价', '商务谈判', '即将签约'])
const leadOptions = ref<string[]>([])

const filters = reactive({ keyword: '', stage: '', owner: '' })

const stats = reactive({ total: 0, monthAdded: 0, totalAmount: '0', avgWinRate: 0 })

const ownerOptions = computed(() => {
  const set = new Set<string>()
  rows.value.forEach((r) => r.owner && set.add(r.owner))
  return Array.from(set)
})

function formatAmount(amount: number | string | undefined) {
  const n = Number(amount ?? 0)
  return Number.isInteger(n) ? String(n) : n.toFixed(2)
}

function stageTagType(stage: string) {
  switch (stage) {
    case '即将签约':
      return 'success'
    case '商务谈判':
      return 'warning'
    case '方案报价':
      return 'primary'
    default:
      return 'info'
  }
}

function canConvert(row: Deal) {
  return row.stage === '即将签约' && row.status !== '已转订单'
}

async function loadData() {
  loading.value = true
  try {
    const res = await listDeals({
      keyword: filters.keyword.trim() || undefined,
      stage: filters.stage || undefined,
      owner: filters.owner || undefined,
      page: page.value,
      pageSize: pageSize.value,
    })
    if (res.code !== 0) {
      ElMessage.error(res.message || '加载商机失败')
      return
    }
    rows.value = res.data.rows || []
    total.value = res.data.total || 0
    if (res.data.stages?.length) {
      stages.value = res.data.stages
    }
    // 汇总线索选项
    const leadSet = new Set<string>()
    rows.value.forEach((r) => r.leadName && leadSet.add(r.leadName))
    leadOptions.value = Array.from(leadSet)
    if (res.data.stats) {
      stats.total = res.data.stats.total ?? 0
      stats.monthAdded = res.data.stats.monthAdded ?? 0
      stats.totalAmount = res.data.stats.totalAmount ?? '0'
      stats.avgWinRate = res.data.stats.avgWinRate ?? 0
    }
  } catch (error: any) {
    ElMessage.error(error?.message || '加载商机失败')
  } finally {
    loading.value = false
  }
}

async function loadCustomers() {
  try {
    const res = await listDealCustomers()
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

function onSelectionChange(selection: Deal[]) {
  selected.value = selection
}

const formVisible = ref(false)
const formMode = ref<'add' | 'edit'>('add')
const formRef = ref<FormInstance>()

function createEmptyForm(): Deal {
  return { id: 0, name: '', customerId: undefined, customerName: '', leadName: '', amount: 0, stage: '', winRate: undefined, owner: '', expectDealAt: '', status: '进行中' }
}

const form = reactive<Deal>(createEmptyForm())

const formRules: FormRules = {
  name: [{ required: true, message: '请输入商机名称', trigger: 'blur' }],
  customerName: [{ required: true, message: '请输入所属客户', trigger: 'change' }],
  amount: [{ required: true, message: '请输入预计金额', trigger: 'blur' }],
}

function resetForm() {
  Object.assign(form, createEmptyForm())
  formRef.value?.clearValidate()
}

function onCustomerChange(name: string) {
  const matched = customerOptions.value.find((c) => c.name === name)
  form.customerId = matched ? matched.id : undefined
}

function openAdd() {
  formMode.value = 'add'
  resetForm()
  formVisible.value = true
}

function openEdit(row: Deal) {
  formMode.value = 'edit'
  resetForm()
  Object.assign(form, { ...row, customerId: row.customerId ?? undefined, winRate: row.winRate ?? undefined })
  detailVisible.value = false
  formVisible.value = true
}

async function submitForm() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  const payload: Deal = { ...form }
  try {
    if (formMode.value === 'add') {
      const res = await createDeal(payload)
      if (res.code !== 0) {
        ElMessage.error(res.message || '保存失败')
        return
      }
      ElMessage.success('商机已添加')
    } else {
      const res = await updateDeal(payload)
      if (res.code !== 0) {
        ElMessage.error(res.message || '保存失败')
        return
      }
      ElMessage.success('商机已更新')
    }
    formVisible.value = false
    loadData()
  } catch (error: any) {
    ElMessage.error(error?.message || '保存失败')
  }
}

const detailVisible = ref(false)
const detail = ref<Deal | null>(null)

function openDetail(row: Deal) {
  detail.value = { ...row }
  detailVisible.value = true
}

async function handleDelete(row: Deal) {
  try {
    await ElMessageBox.confirm(`确认删除商机「${row.name}」吗？`, '删除确认', { type: 'warning' })
  } catch {
    return
  }
  try {
    const res = await deleteDeal(row.id!)
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

async function handleConvert(row: Deal) {
  try {
    await ElMessageBox.confirm(`确认将商机「${row.name}」转为订单吗？`, '转订单确认', { type: 'warning' })
  } catch {
    return
  }
  try {
    const res = await convertDeal(row.id!)
    if (res.code !== 0) {
      ElMessage.error(res.message || '转订单失败')
      return
    }
    ElMessage.success('已转为订单')
    loadData()
  } catch (error: any) {
    ElMessage.error(error?.message || '转订单失败')
  }
}

const batchDelete = async () => {
  if (!selected.value.length) return
  try {
    await ElMessageBox.confirm(`确认批量删除 ${selected.value.length} 个商机吗？`, '批量删除', { type: 'warning' })
  } catch {
    return
  }
  try {
    const ids = selected.value.map((s) => s.id!).filter(Boolean)
    const res = await batchDeleteDeals(ids)
    if (res.code !== 0) {
      ElMessage.error(res.message || '批量删除失败')
      return
    }
    ElMessage.success(`已删除 ${res.data ?? ids.length} 个商机`)
    selected.value = []
    loadData()
  } catch (error: any) {
    ElMessage.error(error?.message || '批量删除失败')
  }
}

function handleExport() {
  if (!rows.value.length) {
    ElMessage.warning('暂无可导出的商机数据')
    return
  }
  const headers = ['商机名称', '所属客户', '关联线索', '预计金额(万)', '销售阶段', '胜率', '负责人', '预计成交', '状态']
  const lines = rows.value.map((r) =>
    [r.name, r.customerName, r.leadName || '', r.amount, r.stage, r.winRate ?? '', r.owner, r.expectDealAt || '', r.status || '进行中']
      .map((v) => `"${String(v ?? '').replace(/"/g, '""')}"`)
      .join(','),
  )
  const blob = new Blob(['﻿' + headers.join(',') + '\n' + lines.join('\n')], { type: 'text/csv;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = '商机列表.csv'
  link.click()
  URL.revokeObjectURL(url)
  ElMessage.success('已导出当前页商机')
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
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
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
.deal-name { font-weight: 600; color: #303133; }
.amount { font-weight: 600; color: #e6a23c; }
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
.form-tip { font-size: 12px; color: #909399; line-height: 1.6; margin-top: 2px; }
.unit-tip { margin-left: 8px; color: #909399; font-size: 13px; }

@media (max-width: 768px) {
  .stats-row { grid-template-columns: 1fr; }
}
</style>
