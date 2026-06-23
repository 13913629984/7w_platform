<template>
  <section>
    <div class="sub-page-header">
      <div>
        <h2>销售订单管理</h2>
        <p class="page-desc">管理客户订单和履约状态。</p>
      </div>
      <div class="header-actions">
        <el-button plain :icon="Download" @click="handleExport">导出</el-button>
        <el-button plain :icon="Refresh" @click="loadData">刷新</el-button>
        <el-button type="primary" :icon="Plus" @click="openAdd">新增订单</el-button>
      </div>
    </div>

    <div class="stats-row">
      <div class="stat-card blue">
        <div class="stat-header"><span class="stat-title">订单总数</span><div class="stat-icon">📋</div></div>
        <div class="stat-value">{{ stats.total }}</div>
        <div class="stat-footer">全部订单</div>
      </div>
      <div class="stat-card green">
        <div class="stat-header"><span class="stat-title">本月新增</span><div class="stat-icon">📈</div></div>
        <div class="stat-value">{{ stats.monthAdded }}</div>
        <div class="stat-footer">新订单</div>
      </div>
      <div class="stat-card orange">
        <div class="stat-header"><span class="stat-title">订单总金额</span><div class="stat-icon">💰</div></div>
        <div class="stat-value">¥{{ stats.totalAmount }}万</div>
        <div class="stat-footer">累计金额</div>
      </div>
      <div class="stat-card purple">
        <div class="stat-header"><span class="stat-title">待发货</span><div class="stat-icon">🚚</div></div>
        <div class="stat-value">{{ stats.pendingDeliver }}</div>
        <div class="stat-footer">待发货订单</div>
      </div>
    </div>

    <div class="table-card">
      <div class="filter-bar">
        <div class="filter-left">
          <el-input
            v-model="filters.keyword"
            clearable
            placeholder="搜索订单号/客户/商机..."
            :prefix-icon="Search"
            style="width:240px"
            @keyup.enter="handleSearch"
            @clear="handleSearch"
          />
          <el-select v-model="filters.status" clearable placeholder="状态筛选" style="width:150px" @change="handleSearch">
            <el-option v-for="opt in statuses" :key="opt" :label="opt" :value="opt" />
          </el-select>
          <el-select v-model="filters.customerId" clearable filterable placeholder="客户筛选" style="width:180px" @change="handleSearch">
            <el-option v-for="opt in customerOptions" :key="opt.id" :label="opt.name" :value="opt.id" />
          </el-select>
        </div>
        <div class="filter-right">
          <el-button v-if="selected.length" type="danger" plain :icon="Delete" @click="batchDelete">批量删除 ({{ selected.length }})</el-button>
        </div>
      </div>

      <el-table v-loading="loading" :data="rows" stripe style="width:100%" @selection-change="onSelectionChange">
        <el-table-column type="selection" width="46" />
        <el-table-column prop="orderNo" label="订单编号" min-width="150">
          <template #default="{ row }"><span class="order-no">{{ row.orderNo }}</span></template>
        </el-table-column>
        <el-table-column prop="customerName" label="客户名称" min-width="150" show-overflow-tooltip>
          <template #default="{ row }">{{ row.customerName || '-' }}</template>
        </el-table-column>
        <el-table-column prop="dealName" label="商机名称" min-width="160" show-overflow-tooltip>
          <template #default="{ row }">{{ row.dealName || '-' }}</template>
        </el-table-column>
        <el-table-column label="订单金额" width="110" align="right">
          <template #default="{ row }"><span class="amount">¥{{ formatAmount(row.amount) }}万</span></template>
        </el-table-column>
        <el-table-column prop="contractNo" label="合同编号" min-width="140" />
        <el-table-column prop="signDate" label="签订日期" width="120" align="center">
          <template #default="{ row }">{{ row.signDate || '-' }}</template>
        </el-table-column>
        <el-table-column prop="expectDeliverAt" label="预计发货" width="120" align="center">
          <template #default="{ row }">{{ row.expectDeliverAt || '-' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" effect="light">{{ row.status || '待确认' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" align="center" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">详情</el-button>
            <el-button link type="primary" @click="openEdit(row)">修改</el-button>
            <el-button v-if="row.status === '待确认'" link type="warning" @click="handleConfirm(row)">确认订单</el-button>
            <el-button v-if="row.status === '已确认'" link type="success" @click="handleDeliver(row)">生成出库单</el-button>
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

    <el-dialog v-model="formVisible" :title="formMode === 'add' ? '新增销售订单' : '编辑销售订单'" width="660px" :close-on-click-modal="false" @closed="resetForm">
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="92px" label-position="right">
        <el-form-item label="订单编号" prop="orderNo">
          <el-input v-model="form.orderNo" placeholder="系统自动生成" maxlength="40" />
        </el-form-item>
        <el-form-item label="客户名称" prop="customerName">
          <el-select v-model="form.customerName" filterable allow-create default-first-option placeholder="请选择客户" style="width:100%" @change="onCustomerChange">
            <el-option v-for="opt in customerOptions" :key="opt.id" :label="opt.name" :value="opt.name" />
          </el-select>
        </el-form-item>
        <el-form-item label="商机名称" prop="dealName">
          <el-select v-model="form.dealId" clearable filterable placeholder="请选择商机" style="width:100%" @change="onDealChange">
            <el-option v-for="opt in dealOptions" :key="opt.id" :label="opt.name" :value="opt.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="合同编号" prop="contractNo">
          <el-input v-model="form.contractNo" placeholder="请输入合同编号" maxlength="60" />
        </el-form-item>
        <el-form-item label="签订日期" prop="signDate">
          <el-date-picker v-model="form.signDate" type="date" value-format="YYYY-MM-DD" placeholder="选择日期" style="width:100%" />
        </el-form-item>
        <el-form-item label="预计发货日期" prop="expectDeliverAt">
          <el-date-picker v-model="form.expectDeliverAt" type="date" value-format="YYYY-MM-DD" placeholder="选择日期" style="width:100%" />
        </el-form-item>
        <el-form-item label="订单金额(万)" prop="amount">
          <el-input-number v-model="form.amount" :min="0" :step="10" :precision="2" controls-position="right" :disabled="hasItems" style="width:100%" />
          <div v-if="hasItems" class="form-tip">已根据产品明细自动汇总</div>
        </el-form-item>
        <el-form-item label="负责人" prop="owner">
          <el-select v-model="form.owner" clearable filterable allow-create default-first-option placeholder="请选择负责人" style="width:100%">
            <el-option v-for="opt in ownerOptions" :key="opt" :label="opt" :value="opt" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="请输入备注信息" maxlength="500" />
        </el-form-item>
      </el-form>

      <div class="sub-table-block">
        <div class="sub-table-header">
          <span class="sub-table-title">产品明细</span>
          <el-button size="small" type="primary" :icon="Plus" @click="addItem">添加产品</el-button>
        </div>
        <el-table :data="form.items" border size="small">
          <el-table-column label="产品编码" min-width="120">
            <template #default="{ row }"><el-input v-model="row.productCode" placeholder="产品编码" /></template>
          </el-table-column>
          <el-table-column label="产品名称" min-width="140">
            <template #default="{ row }"><el-input v-model="row.productName" placeholder="产品名称" /></template>
          </el-table-column>
          <el-table-column label="规格型号" min-width="110">
            <template #default="{ row }"><el-input v-model="row.spec" placeholder="规格型号" /></template>
          </el-table-column>
          <el-table-column label="数量" width="110" align="center">
            <template #default="{ row }"><el-input-number v-model="row.quantity" :min="0" :step="1" size="small" controls-position="right" style="width:90px" /></template>
          </el-table-column>
          <el-table-column label="单价(万)" width="120" align="center">
            <template #default="{ row }"><el-input-number v-model="row.price" :min="0" :step="0.1" :precision="2" size="small" controls-position="right" style="width:100px" /></template>
          </el-table-column>
          <el-table-column label="金额(万)" width="90" align="right">
            <template #default="{ row }">{{ lineTotal(row) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="60" align="center">
            <template #default="{ $index }">
              <el-button link type="danger" @click="form.items.splice($index, 1)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" @click="submitForm">确定提交</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="detailVisible" title="订单详情" width="640px" :close-on-click-modal="false">
      <el-descriptions v-if="detail" :column="2" border>
        <el-descriptions-item label="订单编号">{{ detail.orderNo }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ detail.status || '待确认' }}</el-descriptions-item>
        <el-descriptions-item label="客户名称">{{ detail.customerName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="商机名称">{{ detail.dealName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="合同编号">{{ detail.contractNo || '-' }}</el-descriptions-item>
        <el-descriptions-item label="订单金额">¥{{ formatAmount(detail.amount) }}万</el-descriptions-item>
        <el-descriptions-item label="签订日期">{{ detail.signDate || '-' }}</el-descriptions-item>
        <el-descriptions-item label="预计发货">{{ detail.expectDeliverAt || '-' }}</el-descriptions-item>
        <el-descriptions-item label="负责人">{{ detail.owner || '-' }}</el-descriptions-item>
        <el-descriptions-item label="备注">{{ detail.remark || '-' }}</el-descriptions-item>
      </el-descriptions>
      <div v-if="detail && detail.items?.length" class="sub-table-block" style="margin-top:16px">
        <div class="sub-table-title">产品明细</div>
        <el-table :data="detail.items" border size="small" style="margin-top:8px">
          <el-table-column prop="productCode" label="产品编码" min-width="120" />
          <el-table-column prop="productName" label="产品名称" min-width="140" />
          <el-table-column prop="spec" label="规格型号" min-width="100" />
          <el-table-column prop="quantity" label="数量" width="70" align="center" />
          <el-table-column label="单价(万)" width="90" align="right">
            <template #default="{ row }">{{ formatAmount(row.price) }}</template>
          </el-table-column>
          <el-table-column label="金额(万)" width="90" align="right">
            <template #default="{ row }">{{ formatAmount(row.subtotal) }}</template>
          </el-table-column>
        </el-table>
      </div>
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
  batchDeleteOrders,
  confirmOrder,
  createOrder,
  deleteOrder,
  deliverOrder,
  getOrderNextNo,
  listOrderCustomers,
  listOrderDeals,
  listOrders,
  updateOrder,
  type CustomerOption,
  type DealOption,
  type OrderItem,
  type OrderItemRow,
} from '@/api/crm/order'

type Order = OrderItem

const loading = ref(false)
const rows = ref<Order[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)
const selected = ref<Order[]>([])
const customerOptions = ref<CustomerOption[]>([])
const dealOptions = ref<DealOption[]>([])
const statuses = ref<string[]>(['待确认', '已确认', '已发货', '已完成', '已取消'])

const filters = reactive<{ keyword: string; status: string; customerId: number | undefined }>({ keyword: '', status: '', customerId: undefined })

const stats = reactive({ total: 0, monthAdded: 0, totalAmount: '0', pendingDeliver: 0 })

const ownerOptions = computed(() => {
  const set = new Set<string>()
  rows.value.forEach((r) => r.owner && set.add(r.owner))
  return Array.from(set)
})

const hasItems = computed(() => form.items.some((i) => i.productName || i.productCode))

function formatAmount(amount: number | string | undefined) {
  const n = Number(amount ?? 0)
  return Number.isInteger(n) ? String(n) : n.toFixed(2)
}

function lineTotal(row: OrderItemRow) {
  return formatAmount((Number(row.price) || 0) * (Number(row.quantity) || 0))
}

function statusTagType(status: string) {
  switch (status) {
    case '已完成':
      return 'success'
    case '已发货':
      return 'primary'
    case '已确认':
      return 'success'
    case '待确认':
      return 'warning'
    case '已取消':
      return 'info'
    default:
      return 'info'
  }
}

async function loadData() {
  loading.value = true
  try {
    const res = await listOrders({
      keyword: filters.keyword.trim() || undefined,
      status: filters.status || undefined,
      customerId: filters.customerId || undefined,
      page: page.value,
      pageSize: pageSize.value,
    })
    if (res.code !== 0) {
      ElMessage.error(res.message || '加载订单失败')
      return
    }
    rows.value = (res.data.rows || []).map((row) => ({ ...row, items: row.items || [] }))
    total.value = res.data.total || 0
    if (res.data.statuses?.length) {
      statuses.value = res.data.statuses
    }
    if (res.data.stats) {
      stats.total = res.data.stats.total ?? 0
      stats.monthAdded = res.data.stats.monthAdded ?? 0
      stats.totalAmount = res.data.stats.totalAmount ?? '0'
      stats.pendingDeliver = res.data.stats.pendingDeliver ?? 0
    }
  } catch (error: any) {
    ElMessage.error(error?.message || '加载订单失败')
  } finally {
    loading.value = false
  }
}

async function loadOptions() {
  try {
    const [cs, ds] = await Promise.all([listOrderCustomers(), listOrderDeals()])
    if (cs.code === 0) customerOptions.value = cs.data || []
    if (ds.code === 0) dealOptions.value = ds.data || []
  } catch {
    // 下拉加载失败不阻塞主流程
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

function onSelectionChange(selection: Order[]) {
  selected.value = selection
}

const formVisible = ref(false)
const formMode = ref<'add' | 'edit'>('add')
const formRef = ref<FormInstance>()

function createEmptyForm(): Order {
  return { id: 0, orderNo: '', customerId: undefined, customerName: '', dealId: undefined, dealName: '', contractNo: '', amount: 0, owner: '', signDate: '', expectDeliverAt: '', status: '待确认', remark: '', items: [] }
}

const form = reactive<Order>(createEmptyForm())

const formRules: FormRules = {
  orderNo: [{ required: true, message: '请输入订单编号', trigger: 'blur' }],
  customerName: [{ required: true, message: '请选择客户', trigger: 'change' }],
  contractNo: [{ required: true, message: '请输入合同编号', trigger: 'blur' }],
  signDate: [{ required: true, message: '请选择签订日期', trigger: 'change' }],
  amount: [{ required: true, message: '请输入订单金额', trigger: 'blur' }],
}

function resetForm() {
  Object.assign(form, createEmptyForm())
  formRef.value?.clearValidate()
}

function todayStr() {
  const d = new Date()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${d.getFullYear()}-${m}-${day}`
}

function onCustomerChange(name: string) {
  const matched = customerOptions.value.find((c) => c.name === name)
  form.customerId = matched ? matched.id : undefined
}

function onDealChange(id: number | undefined) {
  const matched = dealOptions.value.find((d) => d.id === id)
  if (!matched) {
    form.dealName = ''
    return
  }
  form.dealName = matched.name
  // 自动带出客户与金额
  if (matched.customerName) {
    form.customerName = matched.customerName
    form.customerId = matched.customerId ?? undefined
  }
  if (matched.amount != null && !form.items.length) {
    form.amount = Number(matched.amount)
  }
}

async function openAdd() {
  formMode.value = 'add'
  resetForm()
  form.signDate = todayStr()
  try {
    const res = await getOrderNextNo()
    if (res.code === 0) form.orderNo = res.data
  } catch {
    // 编号获取失败时允许手动填写
  }
  formVisible.value = true
}

function openEdit(row: Order) {
  formMode.value = 'edit'
  resetForm()
  Object.assign(form, {
    ...row,
    customerId: row.customerId ?? undefined,
    dealId: row.dealId ?? undefined,
    items: (row.items || []).map((i) => ({ ...i })),
  })
  detailVisible.value = false
  formVisible.value = true
}

function addItem() {
  form.items.push({ productCode: '', productName: '', spec: '', quantity: 1, price: 0 })
}

async function submitForm() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  const payload: Order = { ...form, items: form.items.map((i) => ({ ...i })) }
  try {
    if (formMode.value === 'add') {
      const res = await createOrder(payload)
      if (res.code !== 0) {
        ElMessage.error(res.message || '保存失败')
        return
      }
      ElMessage.success('订单已创建')
    } else {
      const res = await updateOrder(payload)
      if (res.code !== 0) {
        ElMessage.error(res.message || '保存失败')
        return
      }
      ElMessage.success('订单已更新')
    }
    formVisible.value = false
    loadData()
  } catch (error: any) {
    ElMessage.error(error?.message || '保存失败')
  }
}

const detailVisible = ref(false)
const detail = ref<Order | null>(null)

function openDetail(row: Order) {
  detail.value = { ...row, items: (row.items || []).map((i) => ({ ...i })) }
  detailVisible.value = true
}

async function handleDelete(row: Order) {
  try {
    await ElMessageBox.confirm(`确认删除订单「${row.orderNo}」吗？`, '删除确认', { type: 'warning' })
  } catch {
    return
  }
  try {
    const res = await deleteOrder(row.id!)
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

async function handleConfirm(row: Order) {
  try {
    await ElMessageBox.confirm(`确认订单「${row.orderNo}」吗？`, '确认订单', { type: 'warning' })
  } catch {
    return
  }
  try {
    const res = await confirmOrder(row.id!)
    if (res.code !== 0) {
      ElMessage.error(res.message || '确认失败')
      return
    }
    ElMessage.success('订单已确认')
    loadData()
  } catch (error: any) {
    ElMessage.error(error?.message || '确认失败')
  }
}

async function handleDeliver(row: Order) {
  try {
    await ElMessageBox.confirm(`确认为订单「${row.orderNo}」生成出库单吗？`, '生成出库单', { type: 'warning' })
  } catch {
    return
  }
  try {
    const res = await deliverOrder(row.id!)
    if (res.code !== 0) {
      ElMessage.error(res.message || '生成出库单失败')
      return
    }
    const outboundCode = res.data?.outboundCode
    ElMessage.success(outboundCode ? `已生成出库单 ${outboundCode}，订单转为已发货` : '已生成出库单，订单转为已发货')
    loadData()
  } catch (error: any) {
    ElMessage.error(error?.message || '生成出库单失败')
  }
}

const batchDelete = async () => {
  if (!selected.value.length) return
  try {
    await ElMessageBox.confirm(`确认批量删除 ${selected.value.length} 个订单吗？`, '批量删除', { type: 'warning' })
  } catch {
    return
  }
  try {
    const ids = selected.value.map((s) => s.id!).filter(Boolean)
    const res = await batchDeleteOrders(ids)
    if (res.code !== 0) {
      ElMessage.error(res.message || '批量删除失败')
      return
    }
    ElMessage.success(`已删除 ${res.data ?? ids.length} 个订单`)
    selected.value = []
    loadData()
  } catch (error: any) {
    ElMessage.error(error?.message || '批量删除失败')
  }
}

function handleExport() {
  if (!rows.value.length) {
    ElMessage.warning('暂无可导出的订单数据')
    return
  }
  const headers = ['订单编号', '客户名称', '商机名称', '订单金额(万)', '合同编号', '签订日期', '预计发货', '状态', '负责人']
  const lines = rows.value.map((r) =>
    [r.orderNo, r.customerName, r.dealName || '', r.amount, r.contractNo, r.signDate, r.expectDeliverAt || '', r.status || '待确认', r.owner || '']
      .map((v) => `"${String(v ?? '').replace(/"/g, '""')}"`)
      .join(','),
  )
  const blob = new Blob(['﻿' + headers.join(',') + '\n' + lines.join('\n')], { type: 'text/csv;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = '销售订单.csv'
  link.click()
  URL.revokeObjectURL(url)
  ElMessage.success('已导出当前页订单')
}

onMounted(() => {
  loadData()
  loadOptions()
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
.order-no { font-weight: 600; color: #303133; }
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
.sub-table-block { margin-bottom: 8px; }
.sub-table-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}
.sub-table-title { font-size: 14px; font-weight: 600; color: #303133; }

@media (max-width: 768px) {
  .stats-row { grid-template-columns: 1fr; }
}
</style>
