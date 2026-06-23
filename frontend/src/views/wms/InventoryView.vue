<template>
  <section>
    <div class="sub-page-header">
      <div>
        <h2>库存管理</h2>
        <p class="page-desc">查看和维护物料在仓库、库位和批次维度的库存数量。</p>
      </div>
      <el-space>
        <el-button plain :icon="Refresh" @click="loadData">刷新</el-button>
        <el-button type="primary" :icon="Plus" @click="openAdd">新增库存</el-button>
      </el-space>
    </div>

    <div class="stats-row">
      <div class="stat-card blue"><div class="stat-header"><span class="stat-title">库存记录</span><div class="stat-icon">📦</div></div><div class="stat-value">{{ stats.totalItems }}</div><div class="stat-footer">库存批次记录</div></div>
      <div class="stat-card green"><div class="stat-header"><span class="stat-title">库存总量</span><div class="stat-icon">📊</div></div><div class="stat-value">{{ formatNumber(stats.totalQty) }}</div><div class="stat-footer">可用 {{ formatNumber(stats.availableQty) }}</div></div>
      <div class="stat-card orange"><div class="stat-header"><span class="stat-title">低库存</span><div class="stat-icon">⚠️</div></div><div class="stat-value">{{ stats.lowCount }}</div><div class="stat-footer">低于安全库存</div></div>
      <div class="stat-card purple"><div class="stat-header"><span class="stat-title">缺货</span><div class="stat-icon">🚫</div></div><div class="stat-value">{{ stats.emptyCount }}</div><div class="stat-footer">库存为 0</div></div>
    </div>

    <div class="table-card">
      <div class="filter-bar">
        <div class="filter-left">
          <el-input v-model="keyword" clearable placeholder="搜索SKU/物料/批次/仓库/库位" style="width:280px" :prefix-icon="Search" @keyup.enter="loadData" @clear="loadData" />
          <el-select v-model="warehouseFilter" clearable placeholder="仓库" style="width:150px" @change="loadData">
            <el-option v-for="item in warehouses" :key="item.code" :label="item.name" :value="item.code" />
          </el-select>
          <el-select v-model="locationFilter" clearable placeholder="库位" style="width:150px" @change="loadData">
            <el-option v-for="item in locations" :key="item.code" :label="item.code" :value="item.code" />
          </el-select>
          <el-select v-model="stockStatusFilter" clearable placeholder="库存状态" style="width:130px" @change="loadData">
            <el-option label="正常" value="normal" />
            <el-option label="低库存" value="low" />
            <el-option label="缺货" value="empty" />
          </el-select>
          <el-select v-model="statusFilter" clearable placeholder="启停" style="width:110px" @change="loadData">
            <el-option label="启用" :value="1" />
            <el-option label="禁用" :value="0" />
          </el-select>
          <el-button type="primary" :icon="Search" @click="loadData">查询</el-button>
        </div>
      </div>

      <el-table v-loading="loading" :data="pagedRows" border stripe style="width:100%">
        <el-table-column prop="sku" label="SKU编码" width="130" />
        <el-table-column prop="materialName" label="物料名称" min-width="150"><template #default="{ row }"><b>{{ row.materialName }}</b></template></el-table-column>
        <el-table-column prop="spec" label="规格型号" width="120" />
        <el-table-column prop="warehouseName" label="仓库" width="130" />
        <el-table-column prop="locationCode" label="库位" width="120" />
        <el-table-column prop="batchNo" label="批次号" width="130" />
        <el-table-column prop="quantity" label="库存数量" width="100" align="right"><template #default="{ row }">{{ formatNumber(row.quantity) }}</template></el-table-column>
        <el-table-column prop="availableQty" label="可用" width="90" align="right"><template #default="{ row }">{{ formatNumber(row.availableQty) }}</template></el-table-column>
        <el-table-column prop="lockedQty" label="锁定" width="90" align="right"><template #default="{ row }">{{ formatNumber(row.lockedQty) }}</template></el-table-column>
        <el-table-column prop="safeStock" label="安全库存" width="100" align="right" />
        <el-table-column label="库存状态" width="100" align="center"><template #default="{ row }"><el-tag :type="stockTagType(row.stockStatus)">{{ row.stockStatusName }}</el-tag></template></el-table-column>
        <el-table-column label="启停" width="90" align="center"><template #default="{ row }"><el-switch :model-value="row.status === 1" @change="(val: boolean) => toggleStatus(row, val)" /></template></el-table-column>
        <el-table-column label="操作" width="170" fixed="right" align="center"><template #default="{ row }"><el-button link type="primary" @click="openDetail(row)">详情</el-button><el-button link type="primary" @click="openEdit(row)">编辑</el-button><el-button link type="danger" @click="handleDelete(row)">删除</el-button></template></el-table-column>
      </el-table>

      <div class="wms-pagination">
        <el-pagination v-model:current-page="page" v-model:page-size="pageSize" :total="rows.length" :page-sizes="[10, 20, 50, 100]" background layout="total, sizes, prev, pager, next, jumper" />
      </div>
    </div>

    <el-dialog v-model="formVisible" :title="formMode === 'add' ? '新增库存' : '编辑库存'" width="760px" :close-on-click-modal="false" @closed="resetForm">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-row :gutter="20">
          <el-col :span="12"><el-form-item label="物料" prop="materialId"><el-select v-model="form.materialId" filterable style="width:100%" @change="onMaterialChange"><el-option v-for="item in materials" :key="item.id" :label="`${item.sku} / ${item.name}`" :value="item.id!" /></el-select></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="仓库" prop="warehouseCode"><el-select v-model="form.warehouseCode" style="width:100%" @change="onWarehouseChange"><el-option v-for="item in warehouses" :key="item.code" :label="item.name" :value="item.code" /></el-select></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="库位" prop="locationCode"><el-select v-model="form.locationCode" filterable style="width:100%"><el-option v-for="item in formLocations" :key="item.code" :label="`${item.code} / ${item.name}`" :value="item.code" /></el-select></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="批次号"><el-input v-model="form.batchNo" maxlength="80" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="库存数量" prop="quantity"><el-input-number v-model="form.quantity" :min="0" :precision="3" controls-position="right" style="width:100%" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="可用数量"><el-input-number v-model="form.availableQty" :min="0" :precision="3" controls-position="right" style="width:100%" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="锁定数量"><el-input-number v-model="form.lockedQty" :min="0" :precision="3" controls-position="right" style="width:100%" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="安全库存"><el-input-number v-model="form.safeStock" :min="0" :step="1" controls-position="right" style="width:100%" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="生产日期"><el-date-picker v-model="form.productionDate" type="date" value-format="YYYY-MM-DD" style="width:100%" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="失效日期"><el-date-picker v-model="form.expireDate" type="date" value-format="YYYY-MM-DD" style="width:100%" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="状态"><el-switch v-model="form.status" :active-value="1" :inactive-value="0" active-text="启用" inactive-text="禁用" /></el-form-item></el-col>
          <el-col :span="24"><el-form-item label="备注"><el-input v-model="form.remark" type="textarea" :rows="3" maxlength="500" show-word-limit /></el-form-item></el-col>
        </el-row>
      </el-form>
      <template #footer><el-button @click="formVisible = false">取消</el-button><el-button type="primary" :loading="submitting" @click="submitForm">保存</el-button></template>
    </el-dialog>

    <el-dialog v-model="detailVisible" title="库存详情" width="720px">
      <el-descriptions v-if="detail" :column="2" border>
        <el-descriptions-item label="SKU">{{ detail.sku }}</el-descriptions-item><el-descriptions-item label="物料名称">{{ detail.materialName }}</el-descriptions-item>
        <el-descriptions-item label="仓库">{{ detail.warehouseName }}</el-descriptions-item><el-descriptions-item label="库位">{{ detail.locationCode }} / {{ detail.locationName }}</el-descriptions-item>
        <el-descriptions-item label="批次号">{{ detail.batchNo || '-' }}</el-descriptions-item><el-descriptions-item label="库存状态">{{ detail.stockStatusName }}</el-descriptions-item>
        <el-descriptions-item label="数量">{{ formatNumber(detail.quantity) }}</el-descriptions-item><el-descriptions-item label="可用/锁定">{{ formatNumber(detail.availableQty) }} / {{ formatNumber(detail.lockedQty) }}</el-descriptions-item>
        <el-descriptions-item label="生产日期">{{ detail.productionDate || '-' }}</el-descriptions-item><el-descriptions-item label="失效日期">{{ detail.expireDate || '-' }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ detail.remark || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'
import { listMaterials, type MaterialItem } from '@/api/wms/material'
import { listWarehouses, type WarehouseItem } from '@/api/wms/warehouse'
import { listLocations, type LocationItem } from '@/api/wms/location'
import { changeInventoryStatus, createInventory, deleteInventory, getInventoryStats, listInventories, updateInventory, type InventoryItem, type InventoryStats } from '@/api/wms/inventory'

const loading = ref(false)
const submitting = ref(false)
const rows = ref<InventoryItem[]>([])
const materials = ref<MaterialItem[]>([])
const warehouses = ref<WarehouseItem[]>([])
const locations = ref<LocationItem[]>([])
const keyword = ref('')
const warehouseFilter = ref('')
const locationFilter = ref('')
const stockStatusFilter = ref('')
const statusFilter = ref<number | null>(null)
const page = ref(1)
const pageSize = ref(10)
const formVisible = ref(false)
const detailVisible = ref(false)
const formMode = ref<'add' | 'edit'>('add')
const formRef = ref<FormInstance>()
const detail = ref<InventoryItem | null>(null)
const stats = ref<InventoryStats>({ totalItems: 0, materialCount: 0, totalQty: 0, availableQty: 0, lockedQty: 0, lowCount: 0, emptyCount: 0, activeCount: 0 })
const form = reactive<InventoryItem>(emptyForm())

const pagedRows = computed(() => rows.value.slice((page.value - 1) * pageSize.value, (page.value - 1) * pageSize.value + pageSize.value))
const formLocations = computed(() => locations.value.filter((item) => !form.warehouseCode || item.warehouseCode === form.warehouseCode))

const rules: FormRules = {
  materialId: [{ required: true, message: '请选择物料', trigger: 'change' }],
  warehouseCode: [{ required: true, message: '请选择仓库', trigger: 'change' }],
  locationCode: [{ required: true, message: '请选择库位', trigger: 'change' }],
  quantity: [{ required: true, message: '请输入库存数量', trigger: 'blur' }],
}

function emptyForm(): InventoryItem {
  return { materialId: undefined as unknown as number, warehouseCode: '', locationCode: '', batchNo: '', quantity: 0, availableQty: 0, lockedQty: 0, safeStock: 0, productionDate: '', expireDate: '', remark: '', status: 1 }
}

function formatNumber(value?: number) {
  return Number(value || 0).toLocaleString('zh-CN', { maximumFractionDigits: 3 })
}

function stockTagType(status?: string) {
  if (status === 'empty') return 'danger'
  if (status === 'low') return 'warning'
  return 'success'
}

function resetForm() {
  Object.assign(form, emptyForm())
  formRef.value?.clearValidate()
}

async function loadOptions() {
  const [matRes, whRes, locRes] = await Promise.all([
    listMaterials({ page: 1, pageSize: 1000 }),
    listWarehouses({ status: 1 }),
    listLocations({ status: 1 }),
  ])
  if (matRes.code === 0) materials.value = matRes.data.rows || []
  if (whRes.code === 0) warehouses.value = whRes.data || []
  if (locRes.code === 0) locations.value = locRes.data || []
}

async function loadData() {
  loading.value = true
  try {
    if (!materials.value.length) await loadOptions()
    const [listRes, statsRes] = await Promise.all([
      listInventories({ keyword: keyword.value.trim() || undefined, warehouseCode: warehouseFilter.value || undefined, locationCode: locationFilter.value || undefined, stockStatus: stockStatusFilter.value || undefined, status: statusFilter.value }),
      getInventoryStats(),
    ])
    if (listRes.code !== 0) {
      ElMessage.error(listRes.message || '加载库存失败')
      return
    }
    rows.value = listRes.data || []
    const maxPage = Math.max(1, Math.ceil(rows.value.length / pageSize.value))
    if (page.value > maxPage) page.value = maxPage
    if (statsRes.code === 0) stats.value = statsRes.data
  } catch (error: any) {
    ElMessage.error(error?.message || '加载库存失败')
  } finally {
    loading.value = false
  }
}

function openAdd() {
  formMode.value = 'add'
  resetForm()
  formVisible.value = true
}

function openEdit(row: InventoryItem) {
  formMode.value = 'edit'
  resetForm()
  Object.assign(form, row)
  formVisible.value = true
}

function openDetail(row: InventoryItem) {
  detail.value = row
  detailVisible.value = true
}

function onWarehouseChange() {
  if (!formLocations.value.some((item) => item.code === form.locationCode)) form.locationCode = ''
}

function onMaterialChange() {
  const material = materials.value.find((item) => item.id === form.materialId)
  if (material) form.safeStock = material.safeStock || 0
}

async function submitForm() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  if (Number(form.availableQty || 0) + Number(form.lockedQty || 0) > Number(form.quantity || 0)) {
    ElMessage.warning('可用数量+锁定数量不能大于库存数量')
    return
  }
  submitting.value = true
  try {
    const res = formMode.value === 'add' ? await createInventory({ ...form }) : await updateInventory({ ...form })
    if (res.code !== 0) {
      ElMessage.error(res.message || '保存失败')
      return
    }
    ElMessage.success(formMode.value === 'add' ? '库存已创建' : '库存已更新')
    formVisible.value = false
    await loadData()
  } catch (error: any) {
    ElMessage.error(error?.message || '保存失败')
  } finally {
    submitting.value = false
  }
}

async function handleDelete(row: InventoryItem) {
  if (!row.id) return
  try {
    await ElMessageBox.confirm(`确认删除 ${row.sku} 的库存记录吗？`, '删除确认', { type: 'warning' })
  } catch {
    return
  }
  try {
    const res = await deleteInventory(row.id)
    if (res.code !== 0) {
      ElMessage.error(res.message || '删除失败')
      return
    }
    ElMessage.success('库存已删除')
    await loadData()
  } catch (error: any) {
    ElMessage.error(error?.message || '删除失败')
  }
}

async function toggleStatus(row: InventoryItem, val: boolean) {
  if (!row.id) return
  const previous = row.status
  row.status = val ? 1 : 0
  try {
    const res = await changeInventoryStatus(row.id, val ? 1 : 0)
    if (res.code !== 0) {
      row.status = previous
      ElMessage.error(res.message || '状态更新失败')
      return
    }
    ElMessage.success(val ? '已启用' : '已禁用')
    await loadData()
  } catch (error: any) {
    row.status = previous
    ElMessage.error(error?.message || '状态更新失败')
  }
}

onMounted(() => loadData())
</script>