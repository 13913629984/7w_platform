<template>
  <section>
    <div class="sub-page-header">
      <div>
        <h2>备件管理</h2>
        <p class="page-desc">维护维保备件台账、安全库存与库位信息，低库存备件自动预警。</p>
      </div>
      <el-space>
        <el-button plain :icon="Refresh" @click="loadData">刷新</el-button>
        <el-button type="primary" :icon="Plus" @click="openAdd">新增备件</el-button>
      </el-space>
    </div>

    <div class="stats-row">
      <div class="stat-card blue">
        <div class="stat-header"><span class="stat-title">备件总数</span><div class="stat-icon">🔩</div></div>
        <div class="stat-value">{{ stats.total }}</div>
        <div class="stat-footer">在册备件条目</div>
      </div>
      <div class="stat-card purple">
        <div class="stat-header"><span class="stat-title">备件分类</span><div class="stat-icon">🗂️</div></div>
        <div class="stat-value">{{ stats.categoryCount }}</div>
        <div class="stat-footer">已维护分类数量</div>
      </div>
      <div class="stat-card red">
        <div class="stat-header"><span class="stat-title">低库存备件</span><div class="stat-icon">⚠️</div></div>
        <div class="stat-value">{{ stats.lowStockCount }}</div>
        <div class="stat-footer">低于安全库存</div>
      </div>
      <div class="stat-card green">
        <div class="stat-header"><span class="stat-title">库存总值</span><div class="stat-icon">💰</div></div>
        <div class="stat-value">{{ formatMoney(stats.stockValue) }}</div>
        <div class="stat-footer">停用备件 {{ stats.disabledCount }} 项</div>
      </div>
    </div>

    <div class="table-card">
      <div class="filter-bar">
        <div class="filter-left">
          <el-input v-model="keyword" clearable placeholder="搜索编码/名称/规格/供应商" style="width:280px" :prefix-icon="Search" @keyup.enter="searchData" @clear="searchData" />
          <el-select v-model="categoryFilter" clearable placeholder="备件分类" style="width:140px" @change="searchData">
            <el-option v-for="opt in categories" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
          <el-select v-model="warehouseFilter" clearable placeholder="仓库" style="width:150px" @change="searchData">
            <el-option v-for="opt in warehouses" :key="opt" :label="opt" :value="opt" />
          </el-select>
          <el-checkbox v-model="lowStockOnly" @change="searchData">仅看低库存</el-checkbox>
          <el-button type="primary" :icon="Search" @click="searchData">查询</el-button>
        </div>
      </div>

      <el-table v-loading="loading" :data="rows" stripe border style="width:100%" empty-text="暂无备件数据">
        <el-table-column prop="code" label="备件编码" width="120" />
        <el-table-column prop="name" label="备件名称" min-width="160">
          <template #default="{ row }"><b>{{ row.name }}</b></template>
        </el-table-column>
        <el-table-column prop="spec" label="规格型号" min-width="160" show-overflow-tooltip />
        <el-table-column prop="unit" label="单位" width="80" align="center" />
        <el-table-column label="分类" width="110" align="center">
          <template #default="{ row }"><el-tag effect="plain" round>{{ categoryLabel(row.category) }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="warehouseCode" label="仓库" width="130" />
        <el-table-column label="库存数量" width="120" align="right">
          <template #default="{ row }">
            <el-tag v-if="row.lowStock" type="danger" effect="dark" size="small">{{ row.stockQty ?? 0 }}</el-tag>
            <span v-else>{{ row.stockQty ?? 0 }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="safetyQty" label="安全库存" width="110" align="right" />
        <el-table-column label="单价" width="120" align="right">
          <template #default="{ row }">{{ formatMoney(row.price) }}</template>
        </el-table-column>
        <el-table-column label="库存金额" width="130" align="right">
          <template #default="{ row }">{{ formatMoney(stockValueOf(row)) }}</template>
        </el-table-column>
        <el-table-column prop="supplier" label="供应商" min-width="140" show-overflow-tooltip />
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.lowStock ? 'danger' : 'success'" effect="light">{{ row.lowStock ? '需补货' : '正常' }}</el-tag>
          </template>
        </el-table-column>
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
          :total="total"
          :page-sizes="[10, 20, 50, 100]"
          background
          layout="total, sizes, prev, pager, next, jumper"
          @current-change="handlePageChange"
          @size-change="handleSizeChange"
        />
      </div>
    </div>

    <el-dialog v-model="formVisible" :title="formMode === 'add' ? '新增备件' : '编辑备件'" width="760px" :close-on-click-modal="false" @closed="resetForm">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-row :gutter="20">
          <el-col :span="12"><el-form-item label="备件编码"><el-input v-model="form.code" :disabled="formMode === 'edit'" placeholder="留空自动生成" maxlength="50" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="备件名称" prop="name"><el-input v-model="form.name" maxlength="100" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="规格型号"><el-input v-model="form.spec" maxlength="100" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="计量单位"><el-input v-model="form.unit" maxlength="20" placeholder="默认 个" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="备件分类"><el-select v-model="form.category" clearable placeholder="请选择分类" style="width:100%"><el-option v-for="opt in categories" :key="opt.value" :label="opt.label" :value="opt.value" /></el-select></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="所属仓库"><el-select v-model="form.warehouseCode" clearable placeholder="请选择仓库" style="width:100%"><el-option v-for="opt in warehouses" :key="opt" :label="opt" :value="opt" /></el-select></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="库存数量"><el-input-number v-model="form.stockQty" :min="0" :precision="2" :step="1" controls-position="right" style="width:100%" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="安全库存"><el-input-number v-model="form.safetyQty" :min="0" :precision="2" :step="1" controls-position="right" style="width:100%" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="单价"><el-input-number v-model="form.price" :min="0" :precision="2" :step="10" controls-position="right" style="width:100%" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="状态"><el-switch v-model="form.status" :active-value="1" :inactive-value="0" active-text="启用" inactive-text="停用" /></el-form-item></el-col>
          <el-col :span="24"><el-form-item label="供应商"><el-input v-model="form.supplier" maxlength="100" /></el-form-item></el-col>
          <el-col :span="24"><el-form-item label="备注"><el-input v-model="form.remark" type="textarea" :rows="3" maxlength="500" show-word-limit /></el-form-item></el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitForm">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="detailVisible" title="备件详情" width="720px">
      <el-descriptions v-if="detail" :column="2" border>
        <el-descriptions-item label="备件编码">{{ detail.code || '-' }}</el-descriptions-item>
        <el-descriptions-item label="备件名称">{{ detail.name }}</el-descriptions-item>
        <el-descriptions-item label="规格型号">{{ detail.spec || '-' }}</el-descriptions-item>
        <el-descriptions-item label="计量单位">{{ detail.unit || '-' }}</el-descriptions-item>
        <el-descriptions-item label="备件分类">{{ categoryLabel(detail.category) }}</el-descriptions-item>
        <el-descriptions-item label="所属仓库">{{ detail.warehouseCode || '-' }}</el-descriptions-item>
        <el-descriptions-item label="库存数量">{{ detail.stockQty ?? 0 }}</el-descriptions-item>
        <el-descriptions-item label="安全库存">{{ detail.safetyQty ?? 0 }}</el-descriptions-item>
        <el-descriptions-item label="单价">{{ formatMoney(detail.price) }}</el-descriptions-item>
        <el-descriptions-item label="库存金额">{{ formatMoney(stockValueOf(detail)) }}</el-descriptions-item>
        <el-descriptions-item label="供应商">{{ detail.supplier || '-' }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="detail.lowStock ? 'danger' : 'success'" effect="light">{{ detail.lowStock ? '低库存需补货' : '库存正常' }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="更新时间">{{ detail.updateTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="启用状态">{{ detail.status === 0 ? '停用' : '启用' }}</el-descriptions-item>
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
  createSparePart,
  deleteSparePart,
  getSparePart,
  getSparePartStats,
  listSpareParts,
  updateSparePart,
  type SparePartItem,
  type SparePartStats,
} from '@/api/maint/sparePart'

const categories = [
  { value: '电气元件', label: '电气元件' },
  { value: '线缆', label: '线缆' },
  { value: '散热件', label: '散热件' },
  { value: '仪表件', label: '仪表件' },
  { value: '网络设备', label: '网络设备' },
  { value: '电源', label: '电源' },
  { value: '辅材', label: '辅材' },
  { value: '其他', label: '其他' },
]

const warehouses = ['WH-MAINT-A', 'WH-MAINT-B', 'WH-MAINT-C']

const loading = ref(false)
const submitting = ref(false)
const rows = ref<SparePartItem[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)
const keyword = ref('')
const categoryFilter = ref('')
const warehouseFilter = ref('')
const lowStockOnly = ref(false)
const formVisible = ref(false)
const detailVisible = ref(false)
const formMode = ref<'add' | 'edit'>('add')
const formRef = ref<FormInstance>()
const detail = ref<SparePartItem | null>(null)
const stats = ref<SparePartStats>({ total: 0, categoryCount: 0, lowStockCount: 0, stockValue: 0, disabledCount: 0 })

const form = reactive<SparePartItem>(emptyForm())

const rules: FormRules = {
  name: [{ required: true, message: '请填写备件名称', trigger: 'blur' }],
}

function emptyForm(): SparePartItem {
  return {
    code: '',
    name: '',
    spec: '',
    unit: '个',
    category: '',
    warehouseCode: '',
    stockQty: 0,
    safetyQty: 0,
    price: null,
    supplier: '',
    status: 1,
    remark: '',
  }
}

function categoryLabel(value?: string) {
  const found = categories.find((opt) => opt.value === value)
  return found ? found.label : value || '-'
}

function formatMoney(value?: number | null) {
  if (value === null || value === undefined) return '-'
  return `¥${Number(value).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`
}

function stockValueOf(row: SparePartItem) {
  if (row.stockQty === null || row.stockQty === undefined || row.price === null || row.price === undefined) return null
  return Number(row.stockQty) * Number(row.price)
}

function resetForm() {
  Object.assign(form, emptyForm())
  formRef.value?.clearValidate()
}

async function loadData() {
  loading.value = true
  try {
    const [listRes, statsRes] = await Promise.all([
      listSpareParts({
        keyword: keyword.value.trim() || undefined,
        category: categoryFilter.value || undefined,
        warehouseCode: warehouseFilter.value || undefined,
        lowStockOnly: lowStockOnly.value ? true : undefined,
        page: page.value,
        pageSize: pageSize.value,
      }),
      getSparePartStats(),
    ])
    if (listRes.code !== 0) {
      ElMessage.error(listRes.message || '加载备件失败')
      return
    }
    if (statsRes.code === 0) stats.value = statsRes.data
    rows.value = listRes.data?.rows || []
    total.value = listRes.data?.total || 0
  } catch (error: any) {
    ElMessage.error(error?.message || '加载备件失败')
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

function openAdd() {
  formMode.value = 'add'
  resetForm()
  formVisible.value = true
}

function openEdit(row: SparePartItem) {
  formMode.value = 'edit'
  resetForm()
  Object.assign(form, row)
  formVisible.value = true
}

async function openDetail(row: SparePartItem) {
  if (!row.id) return
  try {
    const res = await getSparePart(row.id)
    if (res.code !== 0) {
      ElMessage.error(res.message || '加载备件详情失败')
      return
    }
    detail.value = res.data
    detailVisible.value = true
  } catch (error: any) {
    ElMessage.error(error?.message || '加载备件详情失败')
  }
}

async function submitForm() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    const payload: SparePartItem = { ...form }
    if (!payload.code) delete payload.code
    const res = formMode.value === 'add' ? await createSparePart(payload) : await updateSparePart(payload)
    if (res.code !== 0) {
      ElMessage.error(res.message || '保存失败')
      return
    }
    ElMessage.success(formMode.value === 'add' ? '备件已创建' : '备件已更新')
    formVisible.value = false
    await loadData()
  } catch (error: any) {
    ElMessage.error(error?.message || '保存失败')
  } finally {
    submitting.value = false
  }
}

async function handleDelete(row: SparePartItem) {
  if (!row.id) return
  try {
    await ElMessageBox.confirm(`确认删除备件 ${row.name} 吗？`, '删除确认', { type: 'warning' })
  } catch {
    return
  }
  try {
    const res = await deleteSparePart(row.id)
    if (res.code !== 0) {
      ElMessage.error(res.message || '删除失败')
      return
    }
    ElMessage.success('备件已删除')
    await loadData()
  } catch (error: any) {
    ElMessage.error(error?.message || '删除失败')
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
