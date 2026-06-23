<template>
  <section>
    <div class="sub-page-header">
      <div><h2>采购订单管理</h2></div>
      <el-space>
        <el-button plain @click="handleExport">导出</el-button>
        <el-button plain :icon="Refresh" @click="loadData">刷新</el-button>
        <el-button type="primary" @click="handleAdd">+ 新建采购订单</el-button>
      </el-space>
    </div>

    <div class="stats-row">
      <div class="stat-card orange"><div class="stat-header"><span class="stat-title">待确认订单</span></div><div class="stat-value">{{ stats.pending }}</div><div class="stat-footer"><span style="color:#e6a23c">等待供应商确认</span></div></div>
      <div class="stat-card blue"><div class="stat-header"><span class="stat-title">已确认订单</span></div><div class="stat-value">{{ stats.confirmed }}</div><div class="stat-footer"><span style="color:#409eff">等待发货/入库</span></div></div>
      <div class="stat-card green"><div class="stat-header"><span class="stat-title">已入库</span></div><div class="stat-value">{{ stats.inbound }}</div><div class="stat-footer"><span style="color:#67c23a">入库验收完成</span></div></div>
      <div class="stat-card purple"><div class="stat-header"><span class="stat-title">累计采购金额</span></div><div class="stat-value">¥{{ Number(stats.totalAmount || 0).toLocaleString() }}</div><div class="stat-footer"><span style="color:#909399">累计采购</span></div></div>
    </div>

    <div class="table-card purchase-order-table">
      <div class="filter-bar">
        <div class="filter-left">
          <el-input v-model="searchText" clearable placeholder="搜索采购单号/供应商/需求单号..." style="width:280px" :prefix-icon="Search" @keyup.enter="loadData" @clear="loadData" />
          <el-select v-model="statusFilter" clearable placeholder="状态筛选" style="width:130px" @change="loadData">
            <el-option label="待确认" value="pending" />
            <el-option label="已确认" value="confirmed" />
            <el-option label="已入库" value="inbound" />
            <el-option label="已取消" value="cancelled" />
          </el-select>
          <el-button type="primary" :icon="Search" @click="loadData">查询</el-button>
        </div>
      </div>
      <el-table v-loading="loading" :data="pagedOrders" stripe style="width:100%">
        <el-table-column prop="code" label="采购单号" width="150" />
        <el-table-column prop="supplier" label="供应商" min-width="160" />
        <el-table-column label="关联需求" width="150">
          <template #default="{ row }">
            <el-link v-if="row.demandCodes.length" type="primary" :underline="false">{{ row.demandCodes.join('、') }}</el-link>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="物料数" width="90" align="right"><template #default="{ row }"><b>{{ row.materialCount }} 种</b></template></el-table-column>
        <el-table-column prop="totalQty" label="总数量" width="100" align="right"><template #default="{ row }">{{ formatNumber(row.totalQty) }}</template></el-table-column>
        <el-table-column label="总金额" width="120" align="right"><template #default="{ row }">¥{{ Number(row.totalAmount || 0).toFixed(2) }}</template></el-table-column>
        <el-table-column prop="deliveryDate" label="交货日期" width="120" />
        <el-table-column label="状态" width="110" align="center"><template #default="{ row }"><el-tag :type="getStatusType(row.status)" effect="light">{{ getStatusText(row.status) }}</el-tag></template></el-table-column>
        <el-table-column label="操作" width="260" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleDetail(row)">详情</el-button>
            <el-button v-if="row.status === 'pending'" link type="primary" @click="handleEdit(row)">编辑</el-button>
            <el-button v-if="row.status === 'pending'" link type="warning" @click="handleConfirm(row)">确认订单</el-button>
            <el-button v-if="row.status === 'confirmed'" link type="success" @click="handleCreateInbound(row)">生成入库单</el-button>
            <el-button v-if="row.status === 'pending' || row.status === 'confirmed'" link type="danger" @click="handleCancel(row)">取消</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="wms-pagination"><el-pagination v-model:current-page="page" v-model:page-size="pageSize" :page-sizes="[10, 20, 50]" :total="poList.length" background layout="total, sizes, prev, pager, next, jumper" /></div>
    </div>

    <el-dialog v-model="dialogVisible" :title="editingId > -1 ? '修改采购订单' : '新建采购订单'" width="860px" :close-on-click-modal="false">
      <el-form :model="form" label-width="100px">
        <div class="form-grid two-columns">
          <el-form-item label="供应商" required>
            <el-select v-model="form.supplier" filterable allow-create placeholder="请选择或输入供应商" style="width:100%">
              <el-option v-for="supplier in supplierOptions" :key="supplier" :label="supplier" :value="supplier" />
            </el-select>
          </el-form-item>
          <el-form-item label="入库仓库">
            <el-select v-model="form.warehouseCode" clearable placeholder="生成入库单时入库仓库" style="width:100%">
              <el-option v-for="wh in warehouseOptions" :key="wh.code" :label="wh.name" :value="wh.code" />
            </el-select>
          </el-form-item>
          <el-form-item label="关联需求"><el-select v-model="form.demandCodes" multiple collapse-tags collapse-tags-tooltip placeholder="可关联多个采购需求" style="width:100%"><el-option v-for="code in demandOptions" :key="code" :label="code" :value="code" /></el-select></el-form-item>
          <el-form-item label="交货日期" required><el-date-picker v-model="form.deliveryDate" value-format="YYYY-MM-DD" type="date" style="width:100%" /></el-form-item>
        </div>
        <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" :rows="2" /></el-form-item>
        <div class="order-items-header">
          <h4>物料明细</h4>
          <el-button size="small" type="primary" @click="addItem">+ 添加物料</el-button>
        </div>
        <el-table :data="form.items" border size="small">
          <el-table-column label="SKU" width="130"><template #default="{ row }"><el-input v-model="row.sku" placeholder="SKU" /></template></el-table-column>
          <el-table-column label="物料名称" min-width="150"><template #default="{ row }"><el-input v-model="row.name" placeholder="物料名称" /></template></el-table-column>
          <el-table-column label="规格" width="130"><template #default="{ row }"><el-input v-model="row.spec" placeholder="规格" /></template></el-table-column>
          <el-table-column label="数量" width="120" align="right"><template #default="{ row }"><el-input-number v-model="row.qty" :min="1" controls-position="right" style="width:100%" /></template></el-table-column>
          <el-table-column label="单价" width="120" align="right"><template #default="{ row }"><el-input-number v-model="row.unitPrice" :min="0" :precision="2" controls-position="right" style="width:100%" /></template></el-table-column>
          <el-table-column label="金额" width="110" align="right"><template #default="{ row }">¥{{ (row.qty * row.unitPrice).toFixed(2) }}</template></el-table-column>
          <el-table-column label="操作" width="80" align="center"><template #default="{ $index }"><el-button link type="danger" @click="removeItem($index)">删除</el-button></template></el-table-column>
        </el-table>
        <div class="order-total">合计：{{ formTotalQty }} 件 / ¥{{ formTotalAmount.toFixed(2) }}</div>
      </el-form>
      <template #footer><el-button @click="dialogVisible = false">取消</el-button><el-button type="primary" :loading="submitting" @click="submitForm">保存</el-button></template>
    </el-dialog>

    <el-dialog v-model="detailVisible" title="采购订单详情" width="760px">
      <template v-if="detailForm">
        <el-descriptions border :column="2">
          <el-descriptions-item label="采购单号">{{ detailForm.code }}</el-descriptions-item>
          <el-descriptions-item label="供应商">{{ detailForm.supplier }}</el-descriptions-item>
          <el-descriptions-item label="关联需求">{{ detailForm.demandCodes.join('、') || '-' }}</el-descriptions-item>
          <el-descriptions-item label="交货日期">{{ detailForm.deliveryDate }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ detailForm.createTime }}</el-descriptions-item>
          <el-descriptions-item label="状态"><el-tag :type="getStatusType(detailForm.status)" effect="light">{{ getStatusText(detailForm.status) }}</el-tag></el-descriptions-item>
          <el-descriptions-item v-if="detailForm.inboundCode" label="入库单号">{{ detailForm.inboundCode }}</el-descriptions-item>
          <el-descriptions-item label="备注">{{ detailForm.remark || '-' }}</el-descriptions-item>
        </el-descriptions>
        <h4 class="detail-subtitle">物料明细</h4>
        <el-table :data="detailForm.items || []" border size="small">
          <el-table-column prop="sku" label="SKU" width="110" />
          <el-table-column prop="name" label="物料名称" min-width="140" />
          <el-table-column prop="spec" label="规格" width="120" />
          <el-table-column prop="qty" label="数量" width="90" align="right" />
          <el-table-column label="单价" width="100" align="right"><template #default="{ row }">¥{{ Number(row.unitPrice || 0).toFixed(2) }}</template></el-table-column>
          <el-table-column label="金额" width="110" align="right"><template #default="{ row }">¥{{ (Number(row.qty || 0) * Number(row.unitPrice || 0)).toFixed(2) }}</template></el-table-column>
        </el-table>
      </template>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
        <el-button v-if="detailForm?.status === 'pending'" type="warning" @click="handleConfirm(detailForm)">确认订单</el-button>
        <el-button v-if="detailForm?.status === 'confirmed'" type="success" @click="handleCreateInbound(detailForm)">生成入库单</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh, Search } from '@element-plus/icons-vue'
import { listMaterials } from '@/api/wms/material'
import { listWarehouses, type WarehouseItem } from '@/api/wms/warehouse'
import { listDemands } from '@/api/wms/purchaseDemand'
import {
  cancelPurchaseOrder,
  confirmPurchaseOrder,
  createInboundFromPO,
  createPurchaseOrder,
  deletePurchaseOrder,
  getPOStats,
  getPurchaseOrder,
  listPurchaseOrders,
  updatePurchaseOrder,
  type POStats,
  type POStatus,
  type PurchaseOrder,
  type PurchaseOrderItem,
} from '@/api/wms/purchaseOrder'

type OrderItem = { sku: string; name: string; spec: string; qty: number; unitPrice: number }
type PurchaseOrderForm = { supplier: string; warehouseCode: string; demandCodes: string[]; deliveryDate: string; remark: string; items: OrderItem[] }

const statusText: Record<POStatus, string> = { pending: '待确认', confirmed: '已确认', inbound: '已入库', cancelled: '已取消' }
const statusType: Record<POStatus, 'warning' | 'primary' | 'success' | 'danger'> = { pending: 'warning', confirmed: 'primary', inbound: 'success', cancelled: 'danger' }
const getStatusText = (status: POStatus) => statusText[status]
const getStatusType = (status: POStatus) => statusType[status]

const loading = ref(false)
const submitting = ref(false)
const poList = ref<PurchaseOrder[]>([])
const stats = ref<POStats>({ pending: 0, confirmed: 0, inbound: 0, cancelled: 0, totalAmount: 0 })
const supplierOptions = ref<string[]>([])
const warehouseOptions = ref<WarehouseItem[]>([])
const demandOptions = ref<string[]>([])
const searchText = ref('')
const statusFilter = ref('')
const page = ref(1)
const pageSize = ref(10)
const dialogVisible = ref(false)
const detailVisible = ref(false)
const editingId = ref(-1)
const form = reactive<PurchaseOrderForm>(emptyForm())
const detailForm = ref<PurchaseOrder | null>(null)

const pagedOrders = computed(() => poList.value.slice((page.value - 1) * pageSize.value, page.value * pageSize.value))
const formTotalQty = computed(() => form.items.reduce((sum, item) => sum + Number(item.qty || 0), 0))
const formTotalAmount = computed(() => form.items.reduce((sum, item) => sum + Number(item.qty || 0) * Number(item.unitPrice || 0), 0))

function emptyForm(): PurchaseOrderForm {
  return { supplier: '', warehouseCode: '', demandCodes: [], deliveryDate: '', remark: '', items: [{ sku: '', name: '', spec: '', qty: 1, unitPrice: 0 }] }
}

function formatNumber(value?: number) {
  return Number(value || 0).toLocaleString('zh-CN', { maximumFractionDigits: 3 })
}

async function loadOptions() {
  const [matRes, whRes, demandRes] = await Promise.all([
    listMaterials({ page: 1, pageSize: 1000 }),
    listWarehouses({ status: 1 }),
    listDemands({}),
  ])
  if (matRes.code === 0) {
    const suppliers = (matRes.data.rows || []).map((m: any) => m.supplier).filter(Boolean)
    supplierOptions.value = Array.from(new Set(suppliers))
  }
  if (whRes.code === 0) warehouseOptions.value = whRes.data || []
  if (demandRes.code === 0) demandOptions.value = (demandRes.data || []).map((d) => d.code)
}

async function loadData() {
  loading.value = true
  try {
    if (!warehouseOptions.value.length) await loadOptions()
    const [listRes, statsRes] = await Promise.all([
      listPurchaseOrders({ keyword: searchText.value.trim() || undefined, status: statusFilter.value || undefined }),
      getPOStats(),
    ])
    if (listRes.code !== 0) {
      ElMessage.error(listRes.message || '加载采购订单失败')
      return
    }
    poList.value = listRes.data || []
    const maxPage = Math.max(1, Math.ceil(poList.value.length / pageSize.value))
    if (page.value > maxPage) page.value = maxPage
    if (statsRes.code === 0) stats.value = statsRes.data
  } catch (error: any) {
    ElMessage.error(error?.message || '加载采购订单失败')
  } finally {
    loading.value = false
  }
}

function handleAdd() {
  Object.assign(form, emptyForm())
  editingId.value = -1
  dialogVisible.value = true
}

async function handleEdit(row: PurchaseOrder) {
  if (!row.id) return
  const res = await getPurchaseOrder(row.id)
  if (res.code !== 0) {
    ElMessage.error(res.message || '加载订单详情失败')
    return
  }
  const order = res.data
  editingId.value = order.id ?? -1
  Object.assign(form, {
    supplier: order.supplier,
    warehouseCode: order.warehouseCode || '',
    demandCodes: [...(order.demandCodes || [])],
    deliveryDate: order.deliveryDate ? String(order.deliveryDate).slice(0, 10) : '',
    remark: order.remark || '',
    items: (order.items || []).map((item) => ({ sku: item.sku, name: item.name, spec: item.spec || '', qty: Number(item.qty || 0), unitPrice: Number(item.unitPrice || 0) })),
  })
  if (!form.items.length) form.items.push({ sku: '', name: '', spec: '', qty: 1, unitPrice: 0 })
  dialogVisible.value = true
}

async function handleDetail(row: PurchaseOrder) {
  if (!row.id) return
  const res = await getPurchaseOrder(row.id)
  if (res.code !== 0) {
    ElMessage.error(res.message || '加载订单详情失败')
    return
  }
  detailForm.value = res.data
  detailVisible.value = true
}

function addItem() {
  form.items.push({ sku: '', name: '', spec: '', qty: 1, unitPrice: 0 })
}

function removeItem(index: number) {
  if (form.items.length === 1) {
    ElMessage.warning('至少保留一条物料明细')
    return
  }
  form.items.splice(index, 1)
}

async function submitForm() {
  if (!form.supplier.trim() || !form.deliveryDate) {
    ElMessage.warning('请填写供应商和交货日期')
    return
  }
  if (form.items.some((item) => !item.sku.trim() || !item.name.trim() || Number(item.qty || 0) <= 0)) {
    ElMessage.warning('请完善物料明细（SKU、名称、数量）')
    return
  }
  submitting.value = true
  try {
    const payload = {
      supplier: form.supplier.trim(),
      warehouseCode: form.warehouseCode,
      demandCodes: [...form.demandCodes],
      deliveryDate: form.deliveryDate,
      remark: form.remark,
      items: form.items.map((item) => ({ sku: item.sku.trim(), name: item.name.trim(), spec: item.spec.trim(), qty: Number(item.qty || 0), unitPrice: Number(item.unitPrice || 0) })) as PurchaseOrderItem[],
    }
    const res = editingId.value > -1
      ? await updatePurchaseOrder({ id: editingId.value, ...payload })
      : await createPurchaseOrder(payload)
    if (res.code !== 0) {
      ElMessage.error(res.message || '保存失败')
      return
    }
    ElMessage.success(editingId.value > -1 ? '采购订单修改成功' : '采购订单创建成功')
    dialogVisible.value = false
    if (editingId.value === -1) page.value = 1
    await loadData()
  } catch (error: any) {
    ElMessage.error(error?.message || '保存失败')
  } finally {
    submitting.value = false
  }
}

async function handleConfirm(row: PurchaseOrder) {
  if (!row.id) return
  try {
    await ElMessageBox.confirm(`确定确认采购订单 ${row.code} 吗？确认后将通知供应商发货。`, '确认订单', { type: 'info' })
  } catch {
    return
  }
  const res = await confirmPurchaseOrder(row.id)
  if (res.code !== 0) {
    ElMessage.error(res.message || '确认失败')
    return
  }
  detailVisible.value = false
  ElMessage.success('采购订单已确认，已通知供应商')
  await loadData()
}

async function handleCreateInbound(row: PurchaseOrder) {
  if (!row.id) return
  try {
    await ElMessageBox.confirm(`确定为采购订单 ${row.code} 生成入库单吗？将自动创建对应入库单。`, '生成入库单', { type: 'info' })
  } catch {
    return
  }
  const res = await createInboundFromPO(row.id)
  if (res.code !== 0) {
    ElMessage.error(res.message || '生成入库单失败')
    return
  }
  detailVisible.value = false
  ElMessage.success(`入库单 ${res.data.inboundCode} 已生成`)
  await loadData()
}

async function handleCancel(row: PurchaseOrder) {
  if (!row.id) return
  try {
    await ElMessageBox.confirm(`确定取消采购订单 ${row.code} 吗？`, '取消订单', { type: 'warning' })
  } catch {
    return
  }
  const res = await cancelPurchaseOrder(row.id)
  if (res.code !== 0) {
    ElMessage.error(res.message || '取消失败')
    return
  }
  ElMessage.success('采购订单已取消')
  await loadData()
}

function handleExport() {
  if (!poList.value.length) {
    ElMessage.warning('暂无可导出的采购订单')
    return
  }
  const headers = ['采购单号', '供应商', '关联需求', '物料数', '总数量', '总金额', '交货日期', '状态', '入库单号', '创建人']
  const rows = poList.value.map((p) => [p.code, p.supplier, p.demandCodes.join('、'), p.materialCount, p.totalQty, Number(p.totalAmount || 0).toFixed(2), p.deliveryDate, getStatusText(p.status), p.inboundCode, p.creator])
  const csv = [headers, ...rows].map((row) => row.map((cell) => `"${String(cell ?? '').replace(/"/g, '""')}"`).join(',')).join('\n')
  const blob = new Blob([`﻿${csv}`], { type: 'text/csv;charset=utf-8;' })
  const link = document.createElement('a')
  link.href = URL.createObjectURL(blob)
  link.download = `采购订单_${new Date().toISOString().slice(0, 10)}.csv`
  link.click()
  URL.revokeObjectURL(link.href)
  ElMessage.success('已导出采购订单列表')
}

onMounted(() => loadData())
</script>

<style scoped>
.purchase-order-table :deep(.el-table__cell) { white-space: nowrap; }
.form-grid.two-columns { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); column-gap: 18px; }
.order-items-header { display: flex; align-items: center; justify-content: space-between; margin: 8px 0 12px; }
.order-items-header .el-button { min-width: 88px; }
.order-items-header h4, .detail-subtitle { margin: 0; color: #303133; }
.detail-subtitle { margin: 18px 0 12px; }
.order-total { margin-top: 12px; text-align: right; color: #303133; font-weight: 600; }
@media (max-width: 900px) { .form-grid.two-columns { grid-template-columns: 1fr; } }
</style>
