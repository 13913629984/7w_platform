<template>
  <section>
    <div class="sub-page-header">
      <div><h2>采购需求管理</h2></div>
      <el-space>
        <el-button plain :loading="mrpLoading" @click="handleMRPCalc">🔄 MRP计算</el-button>
        <el-button plain @click="handleExport">导出</el-button>
        <el-button plain :icon="Refresh" @click="loadData">刷新</el-button>
        <el-button type="primary" @click="handleAdd">+ 新增需求</el-button>
      </el-space>
    </div>

    <div class="stats-row">
      <div class="stat-card orange"><div class="stat-header"><span class="stat-title">待处理需求</span></div><div class="stat-value">{{ stats.pending }}</div><div class="stat-footer"><span style="color:#e6a23c">需生成采购订单</span></div></div>
      <div class="stat-card blue"><div class="stat-header"><span class="stat-title">已生成订单</span></div><div class="stat-value">{{ stats.ordered }}</div><div class="stat-footer"><span style="color:#409eff">等待供应商确认</span></div></div>
      <div class="stat-card green"><div class="stat-header"><span class="stat-title">已完成</span></div><div class="stat-value">{{ stats.completed }}</div><div class="stat-footer"><span style="color:#67c23a">入库验收完成</span></div></div>
      <div class="stat-card red"><div class="stat-header"><span class="stat-title">库存预警触发</span></div><div class="stat-value">{{ warningTriggers.length }}</div><div class="stat-footer"><span style="color:#f56c6c">低库存物料</span></div></div>
    </div>

    <div class="card warning-demand-card">
      <div class="card-header">
        <div class="card-title">⚠️ 库存预警触发需求</div>
        <el-button size="small" type="primary" :disabled="!warningTriggers.length" @click="handleGenerateFromWarning">批量生成需求</el-button>
      </div>
      <el-table v-loading="triggerLoading" :data="warningTriggers" stripe empty-text="暂无低库存触发项">
        <el-table-column prop="sku" label="物料编码" width="120" />
        <el-table-column prop="name" label="物料名称" min-width="140" />
        <el-table-column prop="spec" label="规格型号" width="120" />
        <el-table-column prop="currentStock" label="当前库存" width="100" align="right"><template #default="{ row }">{{ formatNumber(row.currentStock) }}</template></el-table-column>
        <el-table-column prop="safetyStock" label="安全库存" width="100" align="right" />
        <el-table-column prop="suggestQty" label="建议采购量" width="110" align="right"><template #default="{ row }"><b>{{ formatNumber(row.suggestQty) }}</b></template></el-table-column>
        <el-table-column label="操作" width="140" align="center"><template #default="{ row }"><el-button link type="primary" @click="handleCreateFromWarning(row)">生成需求</el-button></template></el-table-column>
      </el-table>
    </div>

    <div class="table-card purchase-demand-table">
      <div class="filter-bar">
        <div class="filter-left">
          <el-input v-model="searchText" clearable placeholder="搜索需求单号/SKU/物料名称..." style="width:280px" :prefix-icon="Search" @keyup.enter="loadData" @clear="loadData" />
          <el-select v-model="sourceFilter" clearable placeholder="需求来源" style="width:130px" @change="loadData">
            <el-option label="库存预警" value="warning" />
            <el-option label="MRP计算" value="mrp" />
            <el-option label="手动创建" value="manual" />
          </el-select>
          <el-select v-model="statusFilter" clearable placeholder="状态筛选" style="width:130px" @change="loadData">
            <el-option label="待处理" value="pending" />
            <el-option label="已生成订单" value="ordered" />
            <el-option label="已完成" value="completed" />
          </el-select>
          <el-button type="primary" :icon="Search" @click="loadData">查询</el-button>
        </div>
        <el-button type="primary" :disabled="!selectedDemands.length" @click="handleBatchGeneratePO">生成采购订单（{{ selectedDemands.length }}）</el-button>
      </div>
      <el-table ref="demandTableRef" v-loading="loading" :data="pagedDemands" stripe style="width:100%" @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="48" :selectable="isDemandSelectable" />
        <el-table-column prop="code" label="需求单号" width="150" />
        <el-table-column label="需求来源" width="120"><template #default="{ row }"><el-tag :type="getSourceType(row.source)" effect="light">{{ getSourceText(row.source) }}</el-tag></template></el-table-column>
        <el-table-column prop="sku" label="物料编码" width="110" />
        <el-table-column prop="materialName" label="物料名称" min-width="140" />
        <el-table-column prop="qty" label="需求数量" width="100" align="right"><template #default="{ row }"><b>{{ formatNumber(row.qty) }}</b></template></el-table-column>
        <el-table-column label="预计单价" width="110" align="right"><template #default="{ row }">¥{{ Number(row.estimatedPrice || 0).toFixed(2) }}</template></el-table-column>
        <el-table-column label="需求金额" width="120" align="right"><template #default="{ row }">¥{{ (Number(row.qty || 0) * Number(row.estimatedPrice || 0)).toFixed(2) }}</template></el-table-column>
        <el-table-column prop="demandDate" label="需求日期" width="120" />
        <el-table-column label="状态" width="110" align="center"><template #default="{ row }"><el-tag :type="getStatusType(row.status)" effect="light">{{ getStatusText(row.status) }}</el-tag></template></el-table-column>
        <el-table-column label="操作" width="200" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleDetail(row)">详情</el-button>
            <el-button v-if="row.status === 'pending'" link type="primary" @click="handleEdit(row)">编辑</el-button>
            <el-button v-if="row.status === 'pending'" link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="wms-pagination"><el-pagination v-model:current-page="page" v-model:page-size="pageSize" :page-sizes="[10, 20, 50]" :total="demandList.length" background layout="total, sizes, prev, pager, next, jumper" /></div>
    </div>

    <el-dialog v-model="dialogVisible" :title="editingId > -1 ? '修改采购需求' : '新增采购需求'" width="620px" :close-on-click-modal="false">
      <el-form :model="form" label-width="100px">
        <el-form-item label="需求来源" required><el-select v-model="form.source" style="width:100%"><el-option label="库存预警" value="warning" /><el-option label="MRP计算" value="mrp" /><el-option label="手动创建" value="manual" /></el-select></el-form-item>
        <el-form-item label="物料编码" required><el-input v-model="form.sku" placeholder="请输入物料编码" /></el-form-item>
        <el-form-item label="物料名称" required><el-input v-model="form.materialName" placeholder="请输入物料名称" /></el-form-item>
        <el-form-item label="规格型号"><el-input v-model="form.spec" placeholder="请输入规格型号" /></el-form-item>
        <el-form-item label="需求数量" required><el-input-number v-model="form.qty" :min="1" style="width:100%" /></el-form-item>
        <el-form-item label="预计单价"><el-input-number v-model="form.estimatedPrice" :min="0" :precision="2" style="width:100%" /></el-form-item>
        <el-form-item label="需求日期" required><el-date-picker v-model="form.demandDate" value-format="YYYY-MM-DD" type="date" style="width:100%" /></el-form-item>
        <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="dialogVisible = false">取消</el-button><el-button type="primary" :loading="submitting" @click="submitForm">保存</el-button></template>
    </el-dialog>

    <el-dialog v-model="detailVisible" title="采购需求详情" width="620px">
      <el-descriptions v-if="detailForm" border :column="2">
        <el-descriptions-item label="需求单号">{{ detailForm.code }}</el-descriptions-item>
        <el-descriptions-item label="需求来源">{{ getSourceText(detailForm.source) }}</el-descriptions-item>
        <el-descriptions-item label="物料编码">{{ detailForm.sku }}</el-descriptions-item>
        <el-descriptions-item label="物料名称">{{ detailForm.materialName }}</el-descriptions-item>
        <el-descriptions-item label="规格型号">{{ detailForm.spec || '-' }}</el-descriptions-item>
        <el-descriptions-item label="需求数量">{{ formatNumber(detailForm.qty) }}</el-descriptions-item>
        <el-descriptions-item label="预计单价">¥{{ Number(detailForm.estimatedPrice || 0).toFixed(2) }}</el-descriptions-item>
        <el-descriptions-item label="需求金额">¥{{ (Number(detailForm.qty || 0) * Number(detailForm.estimatedPrice || 0)).toFixed(2) }}</el-descriptions-item>
        <el-descriptions-item label="需求日期">{{ detailForm.demandDate || '-' }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ getStatusText(detailForm.status) }}</el-descriptions-item>
        <el-descriptions-item label="申请人">{{ detailForm.requester || '-' }}</el-descriptions-item>
        <el-descriptions-item label="采购订单">{{ detailForm.poCode || '-' }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ detailForm.remark || '-' }}</el-descriptions-item>
      </el-descriptions>
      <template #footer><el-button @click="detailVisible = false">关闭</el-button></template>
    </el-dialog>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh, Search } from '@element-plus/icons-vue'
import {
  createDemand,
  deleteDemand,
  generateFromWarning,
  generatePO,
  getDemandStats,
  listDemands,
  listTriggers,
  runMrp,
  updateDemand,
  type DemandSource,
  type DemandStats,
  type DemandStatus,
  type PurchaseDemand,
  type WarningTrigger,
} from '@/api/wms/purchaseDemand'

type DemandForm = { source: DemandSource; sku: string; materialName: string; spec: string; qty: number; estimatedPrice: number; demandDate: string; remark: string }

const sourceText: Record<DemandSource, string> = { warning: '库存预警', mrp: 'MRP计算', manual: '手动创建' }
const sourceType: Record<DemandSource, 'warning' | 'primary' | 'success'> = { warning: 'warning', mrp: 'primary', manual: 'success' }
const statusText: Record<DemandStatus, string> = { pending: '待处理', ordered: '已生成订单', completed: '已完成' }
const statusType: Record<DemandStatus, 'warning' | 'primary' | 'success'> = { pending: 'warning', ordered: 'primary', completed: 'success' }
const getSourceText = (source: DemandSource) => sourceText[source]
const getSourceType = (source: DemandSource) => sourceType[source]
const getStatusText = (status: DemandStatus) => statusText[status]
const getStatusType = (status: DemandStatus) => statusType[status]

const loading = ref(false)
const triggerLoading = ref(false)
const submitting = ref(false)
const mrpLoading = ref(false)
const demandList = ref<PurchaseDemand[]>([])
const warningTriggers = ref<WarningTrigger[]>([])
const stats = ref<DemandStats>({ pending: 0, ordered: 0, completed: 0, triggerCount: 0 })
const searchText = ref('')
const sourceFilter = ref('')
const statusFilter = ref('')
const page = ref(1)
const pageSize = ref(10)
const dialogVisible = ref(false)
const detailVisible = ref(false)
const editingId = ref(-1)
const form = reactive<DemandForm>(emptyForm())
const detailForm = ref<PurchaseDemand | null>(null)
const selectedDemands = ref<PurchaseDemand[]>([])
const demandTableRef = ref()

const pagedDemands = computed(() => demandList.value.slice((page.value - 1) * pageSize.value, page.value * pageSize.value))

function emptyForm(): DemandForm {
  return { source: 'manual', sku: '', materialName: '', spec: '', qty: 1, estimatedPrice: 0, demandDate: '', remark: '' }
}

function formatNumber(value?: number) {
  return Number(value || 0).toLocaleString('zh-CN', { maximumFractionDigits: 3 })
}

function isDemandSelectable(row: PurchaseDemand) {
  return row.status === 'pending'
}

function handleSelectionChange(rows: PurchaseDemand[]) {
  selectedDemands.value = rows.filter((row) => row.status === 'pending')
}

async function loadData() {
  loading.value = true
  triggerLoading.value = true
  try {
    const [listRes, statsRes, triggerRes] = await Promise.all([
      listDemands({ keyword: searchText.value.trim() || undefined, source: sourceFilter.value || undefined, status: statusFilter.value || undefined }),
      getDemandStats(),
      listTriggers(),
    ])
    if (listRes.code !== 0) {
      ElMessage.error(listRes.message || '加载采购需求失败')
      return
    }
    demandList.value = listRes.data || []
    const maxPage = Math.max(1, Math.ceil(demandList.value.length / pageSize.value))
    if (page.value > maxPage) page.value = maxPage
    if (statsRes.code === 0) stats.value = statsRes.data
    if (triggerRes.code === 0) warningTriggers.value = triggerRes.data || []
  } catch (error: any) {
    ElMessage.error(error?.message || '加载采购需求失败')
  } finally {
    loading.value = false
    triggerLoading.value = false
  }
}

function handleAdd() {
  Object.assign(form, emptyForm())
  editingId.value = -1
  dialogVisible.value = true
}

function handleEdit(row: PurchaseDemand) {
  editingId.value = row.id ?? -1
  Object.assign(form, {
    source: row.source,
    sku: row.sku,
    materialName: row.materialName,
    spec: row.spec || '',
    qty: Number(row.qty || 0),
    estimatedPrice: Number(row.estimatedPrice || 0),
    demandDate: row.demandDate ? String(row.demandDate).slice(0, 10) : '',
    remark: row.remark || '',
  })
  dialogVisible.value = true
}

function handleDetail(row: PurchaseDemand) {
  detailForm.value = { ...row }
  detailVisible.value = true
}

async function submitForm() {
  if (!form.sku.trim() || !form.materialName.trim() || !form.qty || !form.demandDate) {
    ElMessage.warning('请填写完整信息（物料编码、名称、数量、需求日期）')
    return
  }
  submitting.value = true
  try {
    const payload = { ...form, sku: form.sku.trim(), materialName: form.materialName.trim() }
    const res = editingId.value > -1
      ? await updateDemand({ id: editingId.value, ...payload })
      : await createDemand(payload)
    if (res.code !== 0) {
      ElMessage.error(res.message || '保存失败')
      return
    }
    ElMessage.success(editingId.value > -1 ? '采购需求修改成功' : '采购需求创建成功')
    dialogVisible.value = false
    if (editingId.value === -1) page.value = 1
    await loadData()
  } catch (error: any) {
    ElMessage.error(error?.message || '保存失败')
  } finally {
    submitting.value = false
  }
}

async function handleDelete(row: PurchaseDemand) {
  if (!row.id) return
  try {
    await ElMessageBox.confirm(`确认删除采购需求 ${row.code} 吗？`, '删除确认', { type: 'warning' })
  } catch {
    return
  }
  const res = await deleteDemand(row.id)
  if (res.code !== 0) {
    ElMessage.error(res.message || '删除失败')
    return
  }
  ElMessage.success('采购需求已删除')
  await loadData()
}

async function handleCreateFromWarning(item: WarningTrigger) {
  const res = await generateFromWarning([item])
  if (res.code !== 0) {
    ElMessage.error(res.message || '生成需求失败')
    return
  }
  ElMessage.success('采购需求创建成功')
  await loadData()
}

async function handleGenerateFromWarning() {
  if (!warningTriggers.value.length) {
    ElMessage.warning('当前没有预警触发需求')
    return
  }
  const res = await generateFromWarning(warningTriggers.value)
  if (res.code !== 0) {
    ElMessage.error(res.message || '批量生成失败')
    return
  }
  ElMessage.success(`已批量生成 ${res.data.created} 条采购需求`)
  await loadData()
}

async function handleMRPCalc() {
  try {
    await ElMessageBox.confirm('确定执行MRP计算吗？系统将根据当前库存安全水位自动生成采购需求。', 'MRP计算', { type: 'info' })
  } catch {
    return
  }
  mrpLoading.value = true
  try {
    const res = await runMrp()
    if (res.code !== 0) {
      ElMessage.error(res.message || 'MRP计算失败')
      return
    }
    ElMessage.success(`MRP计算完成，生成 ${res.data.created} 条采购需求`)
    await loadData()
  } catch (error: any) {
    ElMessage.error(error?.message || 'MRP计算失败')
  } finally {
    mrpLoading.value = false
  }
}

async function handleBatchGeneratePO() {
  if (!selectedDemands.value.length) {
    ElMessage.warning('请选择待处理采购需求')
    return
  }
  const ids = selectedDemands.value.map((item) => item.id!).filter(Boolean)
  try {
    await ElMessageBox.confirm(`确定将选中的 ${ids.length} 条采购需求生成同一个采购订单吗？`, '生成采购订单', { type: 'info' })
  } catch {
    return
  }
  const res = await generatePO(ids)
  if (res.code !== 0) {
    ElMessage.error(res.message || '生成采购订单失败')
    return
  }
  selectedDemands.value = []
  demandTableRef.value?.clearSelection()
  ElMessage.success(`采购订单 ${res.data.poCode} 已生成（${res.data.updated} 条需求）`)
  await loadData()
}

function handleExport() {
  if (!demandList.value.length) {
    ElMessage.warning('暂无可导出的采购需求')
    return
  }
  const headers = ['需求单号', '需求来源', '物料编码', '物料名称', '规格型号', '需求数量', '预计单价', '需求金额', '需求日期', '状态', '采购订单', '申请人']
  const rows = demandList.value.map((d) => [d.code, getSourceText(d.source), d.sku, d.materialName, d.spec, d.qty, d.estimatedPrice, (Number(d.qty || 0) * Number(d.estimatedPrice || 0)).toFixed(2), d.demandDate, getStatusText(d.status), d.poCode, d.requester])
  const csv = [headers, ...rows].map((row) => row.map((cell) => `"${String(cell ?? '').replace(/"/g, '""')}"`).join(',')).join('\n')
  const blob = new Blob([`﻿${csv}`], { type: 'text/csv;charset=utf-8;' })
  const link = document.createElement('a')
  link.href = URL.createObjectURL(blob)
  link.download = `采购需求_${new Date().toISOString().slice(0, 10)}.csv`
  link.click()
  URL.revokeObjectURL(link.href)
  ElMessage.success('已导出采购需求列表')
}

onMounted(() => loadData())
</script>
