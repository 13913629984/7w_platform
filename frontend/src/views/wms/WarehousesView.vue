<template>
  <section>
    <div class="sub-page-header">
      <div>
        <h2>仓库管理</h2>
        <p class="page-desc">维护仓库基础信息、容量、负责人和启停状态。</p>
      </div>
      <el-space>
        <el-button plain :icon="Refresh" @click="loadData">刷新</el-button>
        <el-button type="primary" :icon="Plus" @click="openAdd">新增仓库</el-button>
      </el-space>
    </div>

    <div class="stats-row">
      <div class="stat-card blue">
        <div class="stat-header"><span class="stat-title">仓库总数</span><div class="stat-icon">🏬</div></div>
        <div class="stat-value">{{ stats.total }}</div>
        <div class="stat-footer">已维护仓库</div>
      </div>
      <div class="stat-card green">
        <div class="stat-header"><span class="stat-title">启用仓库</span><div class="stat-icon">✅</div></div>
        <div class="stat-value">{{ stats.activeCount }}</div>
        <div class="stat-footer">可用仓库</div>
      </div>
      <div class="stat-card orange">
        <div class="stat-header"><span class="stat-title">总容量</span><div class="stat-icon">📦</div></div>
        <div class="stat-value">{{ formatNumber(stats.totalCapacity) }}</div>
        <div class="stat-footer">容量单位</div>
      </div>
      <div class="stat-card purple">
        <div class="stat-header"><span class="stat-title">容量使用率</span><div class="stat-icon">📊</div></div>
        <div class="stat-value">{{ Number(stats.usageRate || 0).toFixed(1) }}%</div>
        <div class="stat-footer">已用 {{ formatNumber(stats.usedCapacity) }}</div>
      </div>
    </div>

    <div class="table-card">
      <div class="filter-bar">
        <div class="filter-left">
          <el-input v-model="keyword" clearable placeholder="搜索仓库编码/名称/负责人/地址" style="width:300px" :prefix-icon="Search" @keyup.enter="searchData" @clear="searchData" />
          <el-select v-model="typeFilter" clearable placeholder="仓库类型" style="width:140px" @change="searchData">
            <el-option v-for="item in warehouseTypes" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
          <el-select v-model="statusFilter" clearable placeholder="状态" style="width:120px" @change="searchData">
            <el-option label="启用" :value="1" />
            <el-option label="禁用" :value="0" />
          </el-select>
          <el-button type="primary" :icon="Search" @click="searchData">查询</el-button>
        </div>
      </div>

      <el-table v-loading="loading" :data="pagedRows" border stripe style="width:100%">
        <el-table-column prop="code" label="仓库编码" width="120" />
        <el-table-column prop="name" label="仓库名称" min-width="150"><template #default="{ row }"><b>{{ row.name }}</b></template></el-table-column>
        <el-table-column prop="typeName" label="类型" width="110"><template #default="{ row }"><el-tag>{{ row.typeName }}</el-tag></template></el-table-column>
        <el-table-column prop="manager" label="负责人" width="100" />
        <el-table-column prop="phone" label="联系电话" width="130" />
        <el-table-column prop="address" label="地址" min-width="200" show-overflow-tooltip />
        <el-table-column label="容量" width="170">
          <template #default="{ row }">
            <div>{{ formatNumber(row.usedCapacity) }} / {{ formatNumber(row.capacity) }}</div>
            <el-progress :percentage="Number(row.usageRate || 0)" :stroke-width="6" :show-text="false" />
          </template>
        </el-table-column>
        <el-table-column prop="area" label="面积㎡" width="90" align="right"><template #default="{ row }">{{ formatNumber(row.area) }}</template></el-table-column>
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
          @current-change="handlePageChange"
          @size-change="handleSizeChange"
        />
      </div>
    </div>

    <el-dialog v-model="formVisible" :title="formMode === 'add' ? '新增仓库' : '编辑仓库'" width="760px" :close-on-click-modal="false" @closed="resetForm">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-row :gutter="20">
          <el-col :span="12"><el-form-item label="仓库编码" prop="code"><el-input v-model="form.code" :disabled="formMode === 'edit'" maxlength="50" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="仓库名称" prop="name"><el-input v-model="form.name" maxlength="100" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="仓库类型" prop="type"><el-select v-model="form.type" style="width:100%"><el-option v-for="item in warehouseTypes" :key="item.value" :label="item.label" :value="item.value" /></el-select></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="负责人"><el-input v-model="form.manager" maxlength="50" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="联系电话"><el-input v-model="form.phone" maxlength="50" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="面积㎡"><el-input-number v-model="form.area" :min="0" :precision="2" controls-position="right" style="width:100%" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="总容量"><el-input-number v-model="form.capacity" :min="0" :precision="2" controls-position="right" style="width:100%" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="已用容量"><el-input-number v-model="form.usedCapacity" :min="0" :precision="2" controls-position="right" style="width:100%" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="排序"><el-input-number v-model="form.sort" :min="0" :step="1" controls-position="right" style="width:100%" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="状态"><el-switch v-model="form.status" :active-value="1" :inactive-value="0" active-text="启用" inactive-text="禁用" /></el-form-item></el-col>
          <el-col :span="24"><el-form-item label="仓库地址"><el-input v-model="form.address" maxlength="200" /></el-form-item></el-col>
          <el-col :span="24"><el-form-item label="备注说明"><el-input v-model="form.description" type="textarea" :rows="3" maxlength="500" show-word-limit /></el-form-item></el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitForm">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="detailVisible" title="仓库详情" width="720px">
      <el-descriptions v-if="detail" :column="2" border>
        <el-descriptions-item label="仓库编码">{{ detail.code }}</el-descriptions-item>
        <el-descriptions-item label="仓库名称">{{ detail.name }}</el-descriptions-item>
        <el-descriptions-item label="仓库类型">{{ detail.typeName }}</el-descriptions-item>
        <el-descriptions-item label="负责人">{{ detail.manager || '-' }}</el-descriptions-item>
        <el-descriptions-item label="联系电话">{{ detail.phone || '-' }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ detail.status === 1 ? '启用' : '禁用' }}</el-descriptions-item>
        <el-descriptions-item label="面积㎡">{{ formatNumber(detail.area) }}</el-descriptions-item>
        <el-descriptions-item label="容量使用">{{ formatNumber(detail.usedCapacity) }} / {{ formatNumber(detail.capacity) }}（{{ Number(detail.usageRate || 0).toFixed(1) }}%）</el-descriptions-item>
        <el-descriptions-item label="地址" :span="2">{{ detail.address || '-' }}</el-descriptions-item>
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
import { changeWarehouseStatus, createWarehouse, deleteWarehouse, getWarehouseStats, listWarehouses, updateWarehouse, type WarehouseItem, type WarehouseStats } from '@/api/wms/warehouse'

const warehouseTypes = [
  { label: '普通仓', value: 'normal' },
  { label: '原料仓', value: 'raw' },
  { label: '成品仓', value: 'finished' },
  { label: '备品备件仓', value: 'spare' },
  { label: '暂存仓', value: 'temporary' },
]

const loading = ref(false)
const submitting = ref(false)
const rows = ref<WarehouseItem[]>([])
const page = ref(1)
const pageSize = ref(10)
const keyword = ref('')
const typeFilter = ref('')
const statusFilter = ref<number | null>(null)
const formVisible = ref(false)
const detailVisible = ref(false)
const formMode = ref<'add' | 'edit'>('add')
const formRef = ref<FormInstance>()
const detail = ref<WarehouseItem | null>(null)
const stats = ref<WarehouseStats>({ total: 0, activeCount: 0, inactiveCount: 0, totalCapacity: 0, usedCapacity: 0, usageRate: 0 })

const form = reactive<WarehouseItem>(emptyForm())
const pagedRows = computed(() => rows.value.slice((page.value - 1) * pageSize.value, (page.value - 1) * pageSize.value + pageSize.value))

const rules: FormRules = {
  code: [{ required: true, message: '请填写仓库编码', trigger: 'blur' }],
  name: [{ required: true, message: '请填写仓库名称', trigger: 'blur' }],
  type: [{ required: true, message: '请选择仓库类型', trigger: 'change' }],
}

function emptyForm(): WarehouseItem {
  return { code: '', name: '', type: 'normal', address: '', manager: '', phone: '', area: 0, capacity: 0, usedCapacity: 0, description: '', sort: 0, status: 1 }
}

function resetForm() {
  Object.assign(form, emptyForm())
  formRef.value?.clearValidate()
}

function formatNumber(value?: number) {
  return Number(value || 0).toLocaleString('zh-CN', { maximumFractionDigits: 2 })
}

async function loadData() {
  loading.value = true
  try {
    const [listRes, statsRes] = await Promise.all([
      listWarehouses({ keyword: keyword.value.trim() || undefined, type: typeFilter.value || undefined, status: statusFilter.value }),
      getWarehouseStats(),
    ])
    if (listRes.code !== 0) {
      ElMessage.error(listRes.message || '加载仓库失败')
      return
    }
    rows.value = listRes.data || []
    const maxPage = Math.max(1, Math.ceil(rows.value.length / pageSize.value))
    if (page.value > maxPage) page.value = maxPage
    if (statsRes.code === 0) stats.value = statsRes.data
  } catch (error: any) {
    ElMessage.error(error?.message || '加载仓库失败')
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
}

function handleSizeChange(value: number) {
  pageSize.value = value
  page.value = 1
}

function openAdd() {
  formMode.value = 'add'
  resetForm()
  formVisible.value = true
}

function openEdit(row: WarehouseItem) {
  formMode.value = 'edit'
  resetForm()
  Object.assign(form, row)
  formVisible.value = true
}

function openDetail(row: WarehouseItem) {
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
    const res = formMode.value === 'add' ? await createWarehouse({ ...form }) : await updateWarehouse({ ...form })
    if (res.code !== 0) {
      ElMessage.error(res.message || '保存失败')
      return
    }
    ElMessage.success(formMode.value === 'add' ? '仓库已创建' : '仓库已更新')
    formVisible.value = false
    await loadData()
  } catch (error: any) {
    ElMessage.error(error?.message || '保存失败')
  } finally {
    submitting.value = false
  }
}

async function handleDelete(row: WarehouseItem) {
  if (!row.id) return
  try {
    await ElMessageBox.confirm(`确认删除仓库 ${row.name} 吗？`, '删除确认', { type: 'warning' })
  } catch {
    return
  }
  try {
    const res = await deleteWarehouse(row.id)
    if (res.code !== 0) {
      ElMessage.error(res.message || '删除失败')
      return
    }
    ElMessage.success('仓库已删除')
    await loadData()
  } catch (error: any) {
    ElMessage.error(error?.message || '删除失败')
  }
}

async function toggleStatus(row: WarehouseItem, val: boolean) {
  if (!row.id) return
  const previous = row.status
  row.status = val ? 1 : 0
  try {
    const res = await changeWarehouseStatus(row.id, val ? 1 : 0)
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

<style scoped>
.wms-pagination {
  display: flex;
  justify-content: flex-end;
  padding: 16px 0 4px;
}
</style>
