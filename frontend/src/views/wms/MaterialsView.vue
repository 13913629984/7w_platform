<template>
  <section>
    <div class="sub-page-header">
      <div>
        <h2>物料管理</h2>
        <p class="page-desc">维护物料编码、规格、单位、品牌和安全库存。</p>
      </div>
      <el-space>
        <el-button plain :icon="Download" @click="handleExport">导出</el-button>
        <el-button plain :icon="Upload" @click="openImport">导入</el-button>
        <el-button plain :icon="Refresh" @click="loadData">刷新</el-button>
        <el-button type="primary" :icon="Plus" @click="openAdd">新增物料</el-button>
      </el-space>
    </div>

    <div class="stats-row" v-if="stats">
      <div class="stat-card blue">
        <div class="stat-header"><span class="stat-title">物料总数</span><div class="stat-icon">📦</div></div>
        <div class="stat-value">{{ stats.total }}</div>
        <div class="stat-footer">维护中物料数</div>
      </div>
      <div class="stat-card green">
        <div class="stat-header"><span class="stat-title">启用物料</span><div class="stat-icon">✓</div></div>
        <div class="stat-value">{{ stats.activeCount }}</div>
        <div class="stat-footer">正常业务可用</div>
      </div>
      <div class="stat-card orange">
        <div class="stat-header"><span class="stat-title">禁用物料</span><div class="stat-icon">🚫</div></div>
        <div class="stat-value">{{ stats.inactiveCount }}</div>
        <div class="stat-footer">已停用物料</div>
      </div>
      <div class="stat-card purple">
        <div class="stat-header"><span class="stat-title">品牌数量</span><div class="stat-icon">🏷️</div></div>
        <div class="stat-value">{{ stats.brandCount }}</div>
        <div class="stat-footer">覆盖品牌数</div>
      </div>
    </div>

    <div class="table-card">
      <div class="filter-bar">
        <div class="filter-left">
          <el-input
            v-model="searchKeyword"
            clearable
            placeholder="搜索SKU/物料名称/规格/条码"
            style="width:280px"
            :prefix-icon="Search"
            @keyup.enter="handleSearch"
            @clear="handleSearch"
          />
          <el-select v-model="filterCategory" clearable placeholder="物料类型" style="width:140px" @change="handleSearch">
            <el-option v-for="opt in categories" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
          <el-select v-model="filterStatus" clearable placeholder="状态" style="width:120px" @change="handleSearch">
            <el-option label="启用" :value="1" />
            <el-option label="禁用" :value="0" />
          </el-select>
          <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
        </div>
        <div class="filter-right">
          <el-button v-if="selectedIds.length" type="danger" plain @click="handleBatchDelete">批量删除 ({{ selectedIds.length }})</el-button>
        </div>
      </div>

      <el-table
        v-loading="loading"
        :data="pagedRows"
        stripe
        style="width:100%"
        @selection-change="onSelectionChange"
      >
        <el-table-column type="selection" width="46" />
        <el-table-column prop="sku" label="SKU编码" width="130" fixed>
          <template #default="{ row }"><b>{{ row.sku }}</b></template>
        </el-table-column>
        <el-table-column prop="name" label="物料名称" min-width="160" show-overflow-tooltip />
        <el-table-column prop="spec" label="规格型号" width="130" show-overflow-tooltip />
        <el-table-column label="物料类型" width="110">
          <template #default="{ row }">
            <el-tag effect="plain" round>{{ row.categoryName || categoryLabel(row.category) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="brand" label="品牌" width="100" show-overflow-tooltip />
        <el-table-column prop="unit" label="单位" width="80" align="center" />
        <el-table-column label="单价(元)" width="110" align="right">
          <template #default="{ row }">¥{{ formatMoney(row.unitPrice) }}</template>
        </el-table-column>
        <el-table-column prop="safeStock" label="安全库存" width="100" align="right" />
        <el-table-column prop="supplier" label="默认供应商" width="140" show-overflow-tooltip />
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-switch
              :model-value="row.status === 1"
              :loading="row.__statusLoading"
              @change="(val: boolean) => toggleStatus(row, val)"
            />
          </template>
        </el-table-column>
        <el-table-column label="更新时间" width="160">
          <template #default="{ row }">{{ formatTime(row.updateTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right" align="center">
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

    <el-dialog v-model="formVisible" :title="formMode === 'add' ? '新增物料' : '编辑物料'" width="760px" :close-on-click-modal="false" @closed="resetForm">
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="100px" label-position="right">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="SKU编码" prop="sku">
              <el-input v-model="form.sku" placeholder="如：MAT-EL-001" maxlength="50" show-word-limit />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="物料名称" prop="name">
              <el-input v-model="form.name" placeholder="请输入物料名称" maxlength="100" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="规格型号" prop="spec">
              <el-input v-model="form.spec" placeholder="如：M8*35" maxlength="100" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="物料类型" prop="category">
              <el-select v-model="form.category" placeholder="请选择物料类型" style="width:100%">
                <el-option v-for="opt in categories" :key="opt.value" :label="opt.label" :value="opt.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="品牌" prop="brand">
              <el-select v-model="form.brand" filterable allow-create default-first-option clearable placeholder="请选择品牌" style="width:100%">
                <el-option v-for="item in brands" :key="item.id" :label="item.name" :value="item.name" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="计量单位" prop="unit">
              <el-input v-model="form.unit" placeholder="如：个/件/盒/台/套" maxlength="20" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="单价(元)" prop="unitPrice">
              <el-input-number v-model="form.unitPrice" :min="0" :precision="2" :step="0.1" style="width:100%" controls-position="right" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="安全库存" prop="safeStock">
              <el-input-number v-model="form.safeStock" :min="0" :step="1" style="width:100%" controls-position="right" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="默认供应商" prop="supplier">
              <el-select v-model="form.supplier" filterable allow-create default-first-option clearable placeholder="请选择供应商" style="width:100%">
                <el-option v-for="item in suppliers" :key="item.id" :label="item.name" :value="item.name" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="条形码" prop="barcode">
              <el-input v-model="form.barcode" placeholder="EAN-13 / UPC" maxlength="50" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="保质期(天)" prop="shelfLifeDays">
              <el-input-number v-model="form.shelfLifeDays" :min="0" :step="1" style="width:100%" controls-position="right" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态" prop="status">
              <el-radio-group v-model="form.status">
                <el-radio :label="1">启用</el-radio>
                <el-radio :label="0">禁用</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="重量(kg)" prop="weight">
              <el-input-number v-model="form.weight" :min="0" :precision="3" :step="0.1" style="width:100%" controls-position="right" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="体积(L)" prop="volume">
              <el-input-number v-model="form.volume" :min="0" :precision="3" :step="0.1" style="width:100%" controls-position="right" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="备注" prop="remark">
              <el-input v-model="form.remark" type="textarea" :rows="3" maxlength="500" show-word-limit placeholder="物料补充说明" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitForm">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="detailVisible" title="物料详情" width="640px" :close-on-click-modal="false">
      <el-descriptions v-if="detail" :column="2" border>
        <el-descriptions-item label="SKU编码">{{ detail.sku }}</el-descriptions-item>
        <el-descriptions-item label="物料名称">{{ detail.name }}</el-descriptions-item>
        <el-descriptions-item label="规格型号">{{ detail.spec || '-' }}</el-descriptions-item>
        <el-descriptions-item label="物料类型">{{ detail.categoryName || categoryLabel(detail.category) }}</el-descriptions-item>
        <el-descriptions-item label="品牌">{{ detail.brand || '-' }}</el-descriptions-item>
        <el-descriptions-item label="单位">{{ detail.unit }}</el-descriptions-item>
        <el-descriptions-item label="单价">¥{{ formatMoney(detail.unitPrice) }}</el-descriptions-item>
        <el-descriptions-item label="安全库存">{{ detail.safeStock ?? 0 }}</el-descriptions-item>
        <el-descriptions-item label="默认供应商">{{ detail.supplier || '-' }}</el-descriptions-item>
        <el-descriptions-item label="条形码">{{ detail.barcode || '-' }}</el-descriptions-item>
        <el-descriptions-item label="保质期">{{ detail.shelfLifeDays ? detail.shelfLifeDays + ' 天' : '-' }}</el-descriptions-item>
        <el-descriptions-item label="重量/体积">{{ formatWeight(detail.weight) }} / {{ formatWeight(detail.volume) }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="detail.status === 1 ? 'success' : 'info'" effect="light">{{ detail.status === 1 ? '启用' : '禁用' }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="更新时间">{{ formatTime(detail.updateTime) }}</el-descriptions-item>
        <el-descriptions-item label="创建时间" :span="2">{{ formatTime(detail.createTime) }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ detail.remark || '-' }}</el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
        <el-button type="primary" @click="openEdit(detail!)">编辑</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="importVisible" title="导入物料" width="640px" :close-on-click-modal="false">
      <el-alert
        title="导入说明"
        type="info"
        :closable="false"
        show-icon
      >
        <p>支持JSON格式批量导入，每行一个物料对象。必填字段：SKU编码、物料名称、物料类型、计量单位。可选字段：规格、品牌、单价、安全库存、供应商、状态(1启用/0禁用)等。</p>
      </el-alert>
      <el-input
        v-model="importText"
        type="textarea"
        :rows="10"
        :placeholder="'例如：' + JSON.stringify([{sku:'MAT-001', name:'测试物料', category:'electronics', unit:'件', unitPrice:10.5, safeStock:50}])"
        style="margin-top:12px"
      />
      <el-upload
        v-if="false"
        :auto-upload="false"
        :show-file-list="false"
        accept=".json"
        @change="handleImportFile"
      />
      <div style="margin-top:8px;">
        <el-button size="small" @click="loadImportTemplate">载入示例</el-button>
        <el-button size="small" @click="importText = ''">清空</el-button>
      </div>
      <template #footer>
        <el-button @click="importVisible = false">取消</el-button>
        <el-button type="primary" :loading="importing" @click="submitImport">开始导入</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Plus, Refresh, Search, Download, Upload } from '@element-plus/icons-vue'
import {
  batchDeleteMaterials,
  changeMaterialStatus,
  createMaterial,
  deleteMaterial,
  exportMaterials,
  importMaterials,
  listMaterials,
  updateMaterial,
  type MaterialCategory,
  type MaterialItem,
  type MaterialStats,
} from '@/api/wms/material'
import { listBrands, type BrandItem } from '@/api/wms/brand'
import { listSuppliers, type SupplierItem } from '@/api/wms/supplier'

const loading = ref(false)
const submitting = ref(false)
const importing = ref(false)
const rows = ref<MaterialItem[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)
const searchKeyword = ref('')
const filterCategory = ref<string>('')
const filterStatus = ref<number | null>(null)
const categories = ref<MaterialCategory[]>([
  { value: 'raw', label: '原材料' },
  { value: 'electronics', label: '电子件' },
  { value: 'hardware', label: '五金件' },
  { value: 'rubber', label: '塑胶件' },
  { value: 'finished', label: '成品' },
])
const stats = ref<MaterialStats | null>(null)
const selectedIds = ref<number[]>([])
const brands = ref<BrandItem[]>([])
const suppliers = ref<SupplierItem[]>([])

const formVisible = ref(false)
const formMode = ref<'add' | 'edit'>('add')
const formRef = ref<FormInstance>()
const form = reactive<MaterialItem>({
  sku: '',
  name: '',
  spec: '',
  category: 'electronics',
  brand: '',
  unit: '件',
  unitPrice: 0,
  safeStock: 0,
  supplier: '',
  barcode: '',
  shelfLifeDays: 0,
  weight: 0,
  volume: 0,
  remark: '',
  status: 1,
})

const formRules: FormRules = {
  sku: [{ required: true, message: '请输入SKU编码', trigger: 'blur' }, { max: 50, message: '长度不能超过50个字符', trigger: 'blur' }],
  name: [{ required: true, message: '请输入物料名称', trigger: 'blur' }, { max: 100, message: '长度不能超过100个字符', trigger: 'blur' }],
  category: [{ required: true, message: '请选择物料类型', trigger: 'change' }],
  unit: [{ required: true, message: '请输入计量单位', trigger: 'blur' }, { max: 20, message: '长度不能超过20个字符', trigger: 'blur' }],
  unitPrice: [{ type: 'number', min: 0, message: '单价不能小于0', trigger: 'blur' }],
  safeStock: [{ type: 'number', min: 0, message: '安全库存不能小于0', trigger: 'blur' }],
}

const detailVisible = ref(false)
const detail = ref<MaterialItem | null>(null)

const importVisible = ref(false)
const importText = ref('')

const pagedRows = computed(() => rows.value)

function handlePageChange(value: number) {
  page.value = value
  loadData()
}

function handleSizeChange(value: number) {
  pageSize.value = value
  page.value = 1
  loadData()
}

function categoryLabel(value?: string) {
  const found = categories.value.find((c) => c.value === value)
  return found ? found.label : value || '-'
}

function formatMoney(value?: number) {
  const num = Number(value ?? 0)
  return num.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function formatWeight(value?: number) {
  if (value === undefined || value === null) return '-'
  return value
}

function formatTime(value?: string) {
  if (!value) return '-'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`
}

async function loadData() {
  loading.value = true
  try {
    const res = await listMaterials({
      keyword: searchKeyword.value.trim() || undefined,
      category: filterCategory.value || undefined,
      status: filterStatus.value ?? undefined,
      page: page.value,
      pageSize: pageSize.value,
    })
    if (res.code !== 0) {
      ElMessage.error(res.message || '加载物料失败')
      return
    }
    rows.value = (res.data.rows || []).map((row) => ({ ...row, __statusLoading: false }))
    total.value = res.data.total || 0
    if (res.data.categories?.length) {
      categories.value = res.data.categories
    }
    if (res.data.stats) {
      stats.value = res.data.stats
    }
  } catch (error: any) {
    ElMessage.error(error?.message || '加载物料失败')
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  page.value = 1
  loadData()
}

function onSelectionChange(selection: MaterialItem[]) {
  selectedIds.value = selection.map((s) => s.id!).filter(Boolean)
}

function resetForm() {
  Object.assign(form, {
    id: undefined,
    sku: '',
    name: '',
    spec: '',
    category: 'electronics',
    brand: '',
    unit: '件',
    unitPrice: 0,
    safeStock: 0,
    supplier: '',
    barcode: '',
    shelfLifeDays: 0,
    weight: 0,
    volume: 0,
    remark: '',
    status: 1,
  })
  formRef.value?.clearValidate()
}

function openAdd() {
  formMode.value = 'add'
  resetForm()
  formVisible.value = true
}

function openEdit(row: MaterialItem) {
  formMode.value = 'edit'
  resetForm()
  Object.assign(form, row)
  detailVisible.value = false
  formVisible.value = true
}

function openDetail(row: MaterialItem) {
  detail.value = { ...row }
  detailVisible.value = true
}

async function submitForm() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    const payload: MaterialItem = { ...form }
    if (formMode.value === 'add') {
      const res = await createMaterial(payload)
      if (res.code !== 0) {
        ElMessage.error(res.message || '保存失败')
        return
      }
      ElMessage.success('物料已创建')
    } else {
      const res = await updateMaterial({ ...payload, id: form.id })
      if (res.code !== 0) {
        ElMessage.error(res.message || '保存失败')
        return
      }
      ElMessage.success('物料已更新')
    }
    formVisible.value = false
    loadData()
  } catch (error: any) {
    ElMessage.error(error?.message || '保存失败')
  } finally {
    submitting.value = false
  }
}

async function handleDelete(row: MaterialItem) {
  try {
    await ElMessageBox.confirm(`确认删除物料 ${row.sku} 吗？`, '删除确认', { type: 'warning' })
  } catch {
    return
  }
  try {
    const res = await deleteMaterial(row.id!)
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

async function handleBatchDelete() {
  if (!selectedIds.value.length) return
  try {
    await ElMessageBox.confirm(`确认批量删除 ${selectedIds.value.length} 个物料吗？`, '批量删除', { type: 'warning' })
  } catch {
    return
  }
  try {
    const res = await batchDeleteMaterials(selectedIds.value)
    if (res.code !== 0) {
      ElMessage.error(res.message || '批量删除失败')
      return
    }
    ElMessage.success(`已删除 ${res.data ?? selectedIds.value.length} 个物料`)
    selectedIds.value = []
    loadData()
  } catch (error: any) {
    ElMessage.error(error?.message || '批量删除失败')
  }
}

async function toggleStatus(row: MaterialItem, val: boolean) {
  row.__statusLoading = true
  const previous = row.status
  row.status = val ? 1 : 0
  row.statusName = val ? '启用' : '禁用'
  try {
    const res = await changeMaterialStatus(row.id!, val ? 1 : 0)
    if (res.code !== 0) {
      row.status = previous
      row.statusName = previous === 1 ? '启用' : '禁用'
      ElMessage.error(res.message || '状态更新失败')
      return
    }
    ElMessage.success(val ? '已启用' : '已停用')
    await loadData()
  } catch (error: any) {
    row.status = previous
    row.statusName = previous === 1 ? '启用' : '禁用'
    ElMessage.error(error?.message || '状态更新失败')
  } finally {
    row.__statusLoading = false
  }
}

async function handleExport() {
  try {
    const res = await exportMaterials({
      keyword: searchKeyword.value.trim() || undefined,
      category: filterCategory.value || undefined,
      status: filterStatus.value ?? undefined,
    })
    if (res.code !== 0) {
      ElMessage.error(res.message || '导出失败')
      return
    }
    const rows = res.data.rows || []
    if (!rows.length) {
      ElMessage.warning('当前条件下没有可导出的数据')
      return
    }
    const headers = ['SKU编码', '物料名称', '规格型号', '物料类型', '品牌', '单位', '单价', '安全库存', '供应商', '条形码', '状态', '更新时间']
    const lines = [headers.join(',')]
    rows.forEach((row) => {
      const cells = [
        row.sku,
        row.name,
        row.spec,
        row.categoryName || categoryLabel(row.category),
        row.brand,
        row.unit,
        row.unitPrice ?? 0,
        row.safeStock ?? 0,
        row.supplier,
        row.barcode,
        row.status === 1 ? '启用' : '禁用',
        formatTime(row.updateTime),
      ].map((value) => `"${(value ?? '').toString().replace(/"/g, '""')}"`)
      lines.push(cells.join(','))
    })
    const blob = new Blob(['\uFEFF' + lines.join('\n')], { type: 'text/csv;charset=utf-8' })
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `物料清单_${new Date().toISOString().slice(0, 10)}.csv`
    link.click()
    URL.revokeObjectURL(url)
    ElMessage.success(`已导出 ${rows.length} 条物料`)
  } catch (error: any) {
    ElMessage.error(error?.message || '导出失败')
  }
}

function openImport() {
  importText.value = ''
  importVisible.value = true
}

function loadImportTemplate() {
  importText.value = JSON.stringify(
    [
      {
        sku: 'MAT-IM-001',
        name: '示例物料-继电器',
        spec: '24V 10A',
        category: 'electronics',
        brand: '施耐德',
        unit: '个',
        unitPrice: 28.5,
        safeStock: 200,
        supplier: '上海智造',
        status: 1,
        remark: '导入示例，请按需修改',
      },
    ],
    null,
    2,
  )
}

function handleImportFile(file: { raw?: File }) {
  const reader = new FileReader()
  reader.onload = (event) => {
    importText.value = String(event.target?.result || '')
  }
  if (file.raw) reader.readAsText(file.raw)
}

async function submitImport() {
  if (!importText.value.trim()) {
    ElMessage.warning('请先输入或粘贴要导入的物料数据')
    return
  }
  let parsed: any
  try {
    parsed = JSON.parse(importText.value)
  } catch (error: any) {
    ElMessage.error('JSON格式错误：' + (error?.message || ''))
    return
  }
  const list: any[] = Array.isArray(parsed) ? parsed : [parsed]
  if (!list.length) {
    ElMessage.warning('未解析到任何物料')
    return
  }
  importing.value = true
  try {
    const res = await importMaterials(
      list.map((item) => ({
        sku: String(item.sku || '').trim(),
        name: String(item.name || '').trim(),
        spec: item.spec,
        category: String(item.category || '').trim(),
        brand: item.brand,
        unit: String(item.unit || '').trim(),
        unitPrice: item.unitPrice !== undefined ? Number(item.unitPrice) : 0,
        safeStock: item.safeStock !== undefined ? Number(item.safeStock) : 0,
        supplier: item.supplier,
        barcode: item.barcode,
        shelfLifeDays: item.shelfLifeDays !== undefined ? Number(item.shelfLifeDays) : undefined,
        weight: item.weight !== undefined ? Number(item.weight) : undefined,
        volume: item.volume !== undefined ? Number(item.volume) : undefined,
        remark: item.remark,
        status: item.status !== undefined ? Number(item.status) : 1,
      })),
    )
    if (res.code !== 0) {
      ElMessage.error(res.message || '导入失败')
      return
    }
    const { successCount, failedCount, failedSkus } = res.data
    if (failedCount > 0) {
      ElMessageBox.alert(`成功 ${successCount} 条，失败 ${failedCount} 条。\n失败SKU：\n${failedSkus.join('\n')}`, '导入结果', { type: 'warning' })
    } else {
      ElMessage.success(`成功导入 ${successCount} 条物料`)
    }
    importVisible.value = false
    loadData()
  } catch (error: any) {
    ElMessage.error(error?.message || '导入失败')
  } finally {
    importing.value = false
  }
}

async function loadOptions() {
  try {
    const [brandRes, supplierRes] = await Promise.all([
      listBrands({ status: 1 }),
      listSuppliers({ status: 1 }),
    ])
    if (brandRes.code === 0) brands.value = brandRes.data || []
    if (supplierRes.code === 0) suppliers.value = supplierRes.data || []
  } catch {
    // 下拉选项加载失败不阻塞主流程
  }
}

onMounted(() => {
  loadOptions()
  loadData()
})
</script>

<style scoped>
/* 卡片标题防止溢出：让 title 收缩、icon 不被挤出 */
.stats-row {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
  gap: 20px;
  margin-bottom: 24px;
}
.stat-card {
  background: #fff;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 2px 12px rgba(0,0,0,.04);
  position: relative;
  overflow: hidden;
  transition: transform .2s, box-shadow .2s;
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
.stat-title {
  font-size: 14px;
  color: #909399;
  min-width: 0;
  flex: 1;
  white-space: normal;
  word-break: break-word;
}
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
.stat-card.blue .stat-icon { background: #ecf5ff; color: #409eff; }
.stat-card.green .stat-icon { background: #f0f9eb; color: #67c23a; }
.stat-card.orange .stat-icon { background: #fdf6ec; color: #e6a23c; }
.stat-card.purple .stat-icon { background: #f4f0ff; color: #764ba2; }
.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: #303133;
  margin-bottom: 8px;
}
.stat-footer {
  font-size: 12px;
  color: #909399;
}

/* 表格外层：防止横向溢出，并保留卡片样式 */
.table-card {
  background: #fff;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 2px 12px rgba(0,0,0,.04);
  min-width: 0;
}
.table-card .el-table {
  width: 100%;
}
.filter-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}
.filter-left {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}
.filter-right {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.wms-pagination {
  display: flex;
  justify-content: flex-end;
  padding: 16px 0 4px;
}

/* 小屏幕下防止 emoji/icon 把卡片撑大 */
@media (max-width: 768px) {
  .stats-row { grid-template-columns: 1fr; }
}
</style>
