<template>
  <section>
    <div class="sub-page-header">
      <div>
        <h2>库位管理</h2>
        <p class="page-desc">维护库区、货架、层位、容量和库位启停状态。</p>
      </div>
      <el-space>
        <el-button plain :icon="Refresh" @click="loadData">刷新</el-button>
        <el-button type="primary" :icon="Plus" @click="openAdd">新增库位</el-button>
      </el-space>
    </div>

    <div class="stats-row">
      <div class="stat-card blue">
        <div class="stat-header"><span class="stat-title">库位总数</span><div class="stat-icon">📍</div></div>
        <div class="stat-value">{{ stats.total }}</div>
        <div class="stat-footer">已维护库位</div>
      </div>
      <div class="stat-card green">
        <div class="stat-header"><span class="stat-title">启用库位</span><div class="stat-icon">✅</div></div>
        <div class="stat-value">{{ stats.activeCount }}</div>
        <div class="stat-footer">当前可用库位</div>
      </div>
      <div class="stat-card orange">
        <div class="stat-header"><span class="stat-title">禁用库位</span><div class="stat-icon">🚫</div></div>
        <div class="stat-value">{{ stats.inactiveCount }}</div>
        <div class="stat-footer">已停用库位</div>
      </div>
      <div class="stat-card purple">
        <div class="stat-header"><span class="stat-title">使用率</span><div class="stat-icon">📊</div></div>
        <div class="stat-value">{{ Number(stats.usageRate || 0).toFixed(1) }}%</div>
        <div class="stat-footer">{{ formatNumber(stats.usedCapacity) }} / {{ formatNumber(stats.totalCapacity) }}</div>
      </div>
    </div>

    <div class="table-card">
      <div class="filter-bar">
        <div class="filter-left">
          <el-input v-model="keyword" clearable placeholder="搜索库位编码/名称/库区/货架" style="width:280px" :prefix-icon="Search" @keyup.enter="loadData" @clear="loadData" />
          <el-select v-model="warehouseFilter" clearable placeholder="所属仓库" style="width:160px" @change="loadData">
            <el-option v-for="item in warehouses" :key="item.code" :label="item.name" :value="item.code" />
          </el-select>
          <el-select v-model="typeFilter" clearable placeholder="库位类型" style="width:130px" @change="loadData">
            <el-option v-for="item in locationTypes" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
          <el-select v-model="statusFilter" clearable placeholder="状态" style="width:110px" @change="loadData">
            <el-option label="启用" :value="1" />
            <el-option label="禁用" :value="0" />
          </el-select>
          <el-button type="primary" :icon="Search" @click="loadData">查询</el-button>
        </div>
      </div>

      <el-table v-loading="loading" :data="pagedRows" border stripe style="width:100%">
        <el-table-column prop="code" label="库位编码" width="130" />
        <el-table-column prop="name" label="库位名称" min-width="140"><template #default="{ row }"><b>{{ row.name }}</b></template></el-table-column>
        <el-table-column prop="warehouseName" label="所属仓库" width="130" />
        <el-table-column prop="area" label="库区" width="90" />
        <el-table-column prop="shelf" label="货架" width="90" />
        <el-table-column prop="layer" label="层" width="70" />
        <el-table-column prop="position" label="位" width="70" />
        <el-table-column label="类型" width="100"><template #default="{ row }"><el-tag>{{ row.typeName }}</el-tag></template></el-table-column>
        <el-table-column label="容量" width="170">
          <template #default="{ row }">
            <div>{{ formatNumber(row.usedCapacity) }} / {{ formatNumber(row.capacity) }}</div>
            <el-progress :percentage="Number(row.usageRate || 0)" :stroke-width="6" :show-text="false" />
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center"><template #default="{ row }"><el-switch :model-value="row.status === 1" @change="(val: boolean) => toggleStatus(row, val)" /></template></el-table-column>
        <el-table-column prop="sort" label="排序" width="80" align="center" />
        <el-table-column label="操作" width="170" fixed="right" align="center">
          <template #default="{ row }">
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
          :total="rows.length"
          :page-sizes="[10, 20, 50, 100]"
          background
          layout="total, sizes, prev, pager, next, jumper"
        />
      </div>
    </div>

    <el-dialog v-model="formVisible" :title="formMode === 'add' ? '新增库位' : '编辑库位'" width="760px" :close-on-click-modal="false" @closed="resetForm">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-row :gutter="20">
          <el-col :span="12"><el-form-item label="库位编码" prop="code"><el-input v-model="form.code" :disabled="formMode === 'edit'" maxlength="50" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="库位名称" prop="name"><el-input v-model="form.name" maxlength="100" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="所属仓库" prop="warehouseCode"><el-select v-model="form.warehouseCode" style="width:100%"><el-option v-for="item in warehouses" :key="item.code" :label="item.name" :value="item.code" /></el-select></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="库位类型" prop="type"><el-select v-model="form.type" style="width:100%"><el-option v-for="item in locationTypes" :key="item.value" :label="item.label" :value="item.value" /></el-select></el-form-item></el-col>
          <el-col :span="6"><el-form-item label="库区"><el-input v-model="form.area" maxlength="50" /></el-form-item></el-col>
          <el-col :span="6"><el-form-item label="货架"><el-input v-model="form.shelf" maxlength="50" /></el-form-item></el-col>
          <el-col :span="6"><el-form-item label="层"><el-input v-model="form.layer" maxlength="50" /></el-form-item></el-col>
          <el-col :span="6"><el-form-item label="位"><el-input v-model="form.position" maxlength="50" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="总容量"><el-input-number v-model="form.capacity" :min="0" :precision="2" controls-position="right" style="width:100%" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="已用容量"><el-input-number v-model="form.usedCapacity" :min="0" :precision="2" controls-position="right" style="width:100%" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="排序"><el-input-number v-model="form.sort" :min="0" :step="1" controls-position="right" style="width:100%" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="状态"><el-switch v-model="form.status" :active-value="1" :inactive-value="0" active-text="启用" inactive-text="禁用" /></el-form-item></el-col>
          <el-col :span="24"><el-form-item label="备注说明"><el-input v-model="form.description" type="textarea" :rows="3" maxlength="500" show-word-limit /></el-form-item></el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitForm">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="detailVisible" title="库位详情" width="720px">
      <el-descriptions v-if="detail" :column="2" border>
        <el-descriptions-item label="库位编码">{{ detail.code }}</el-descriptions-item>
        <el-descriptions-item label="库位名称">{{ detail.name }}</el-descriptions-item>
        <el-descriptions-item label="所属仓库">{{ detail.warehouseName }}</el-descriptions-item>
        <el-descriptions-item label="类型">{{ detail.typeName }}</el-descriptions-item>
        <el-descriptions-item label="库区/货架">{{ detail.area || '-' }} / {{ detail.shelf || '-' }}</el-descriptions-item>
        <el-descriptions-item label="层/位">{{ detail.layer || '-' }} / {{ detail.position || '-' }}</el-descriptions-item>
        <el-descriptions-item label="容量使用">{{ formatNumber(detail.usedCapacity) }} / {{ formatNumber(detail.capacity) }}（{{ Number(detail.usageRate || 0).toFixed(1) }}%）</el-descriptions-item>
        <el-descriptions-item label="状态">{{ detail.status === 1 ? '启用' : '禁用' }}</el-descriptions-item>
        <el-descriptions-item label="说明" :span="2">{{ detail.description || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'
import { listWarehouses, type WarehouseItem } from '@/api/wms/warehouse'
import { changeLocationStatus, createLocation, deleteLocation, getLocationStats, listLocations, updateLocation, type LocationItem, type LocationStats } from '@/api/wms/location'

const locationTypes = [
  { label: '普通库位', value: 'normal' },
  { label: '托盘位', value: 'pallet' },
  { label: '货架位', value: 'shelf' },
  { label: '地堆位', value: 'floor' },
  { label: '暂存位', value: 'temporary' },
]

const loading = ref(false)
const submitting = ref(false)
const rows = ref<LocationItem[]>([])
const page = ref(1)
const pageSize = ref(10)
const warehouses = ref<WarehouseItem[]>([])
const keyword = ref('')
const warehouseFilter = ref('')
const typeFilter = ref('')
const statusFilter = ref<number | null>(null)
const formVisible = ref(false)
const detailVisible = ref(false)
const formMode = ref<'add' | 'edit'>('add')
const formRef = ref<FormInstance>()
const detail = ref<LocationItem | null>(null)
const stats = ref<LocationStats>({ total: 0, activeCount: 0, inactiveCount: 0, occupiedCount: 0, emptyCount: 0, totalCapacity: 0, usedCapacity: 0, usageRate: 0 })

const pagedRows = computed(() => {
  const start = (page.value - 1) * pageSize.value
  return rows.value.slice(start, start + pageSize.value)
})

const form = reactive<LocationItem>(emptyForm())

const rules: FormRules = {
  code: [{ required: true, message: '请填写库位编码', trigger: 'blur' }],
  name: [{ required: true, message: '请填写库位名称', trigger: 'blur' }],
  warehouseCode: [{ required: true, message: '请选择所属仓库', trigger: 'change' }],
  type: [{ required: true, message: '请选择库位类型', trigger: 'change' }],
}

function emptyForm(): LocationItem {
  return { code: '', name: '', warehouseCode: '', area: '', shelf: '', layer: '', position: '', type: 'shelf', capacity: 0, usedCapacity: 0, description: '', sort: 0, status: 1 }
}

function resetForm() {
  Object.assign(form, emptyForm())
  if (!form.warehouseCode && warehouses.value.length) form.warehouseCode = warehouses.value[0].code
  formRef.value?.clearValidate()
}

function formatNumber(value?: number) {
  return Number(value || 0).toLocaleString('zh-CN', { maximumFractionDigits: 2 })
}

async function loadWarehouses() {
  const res = await listWarehouses({ status: 1 })
  if (res.code === 0) warehouses.value = res.data || []
}

async function loadData() {
  loading.value = true
  try {
    if (!warehouses.value.length) await loadWarehouses()
    const [listRes, statsRes] = await Promise.all([
      listLocations({ keyword: keyword.value.trim() || undefined, warehouseCode: warehouseFilter.value || undefined, type: typeFilter.value || undefined, status: statusFilter.value }),
      getLocationStats(),
    ])
    if (listRes.code !== 0) {
      ElMessage.error(listRes.message || '加载库位失败')
      return
    }
    rows.value = listRes.data || []
    const maxPage = Math.max(1, Math.ceil(rows.value.length / pageSize.value))
    if (page.value > maxPage) page.value = maxPage
    if (statsRes.code === 0) stats.value = statsRes.data
  } catch (error: any) {
    ElMessage.error(error?.message || '加载库位失败')
  } finally {
    loading.value = false
  }
}

function openAdd() {
  formMode.value = 'add'
  resetForm()
  formVisible.value = true
}

function openEdit(row: LocationItem) {
  formMode.value = 'edit'
  resetForm()
  Object.assign(form, row)
  formVisible.value = true
}

function openDetail(row: LocationItem) {
  detail.value = row
  detailVisible.value = true
}

async function submitForm() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  if (Number(form.usedCapacity || 0) > Number(form.capacity || 0)) {
    ElMessage.warning('已用容量不能大于总容量')
    return
  }
  submitting.value = true
  try {
    const res = formMode.value === 'add' ? await createLocation({ ...form }) : await updateLocation({ ...form })
    if (res.code !== 0) {
      ElMessage.error(res.message || '保存失败')
      return
    }
    ElMessage.success(formMode.value === 'add' ? '库位已创建' : '库位已更新')
    formVisible.value = false
    await loadData()
  } catch (error: any) {
    ElMessage.error(error?.message || '保存失败')
  } finally {
    submitting.value = false
  }
}

async function handleDelete(row: LocationItem) {
  if (!row.id) return
  try {
    await ElMessageBox.confirm(`确认删除库位 ${row.name} 吗？`, '删除确认', { type: 'warning' })
  } catch {
    return
  }
  try {
    const res = await deleteLocation(row.id)
    if (res.code !== 0) {
      ElMessage.error(res.message || '删除失败')
      return
    }
    ElMessage.success('库位已删除')
    await loadData()
  } catch (error: any) {
    ElMessage.error(error?.message || '删除失败')
  }
}

async function toggleStatus(row: LocationItem, val: boolean) {
  if (!row.id) return
  const previous = row.status
  row.status = val ? 1 : 0
  try {
    const res = await changeLocationStatus(row.id, val ? 1 : 0)
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