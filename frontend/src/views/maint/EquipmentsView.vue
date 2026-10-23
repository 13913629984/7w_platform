<template>
  <section>
    <div class="sub-page-header">
      <div>
        <h2>设备台账</h2>
        <p class="page-desc">维护设备档案、安装位置、保修期限与运行状态。</p>
      </div>
      <el-space>
        <el-button plain :icon="Refresh" @click="loadData">刷新</el-button>
        <el-button type="primary" :icon="Plus" @click="openAdd">新增设备</el-button>
      </el-space>
    </div>

    <div class="stats-row">
      <div class="stat-card blue">
        <div class="stat-header"><span class="stat-title">设备总数</span><div class="stat-icon">🏭</div></div>
        <div class="stat-value">{{ stats.total }}</div>
        <div class="stat-footer">在册设备合计</div>
      </div>
      <div class="stat-card green">
        <div class="stat-header"><span class="stat-title">运行中</span><div class="stat-icon">✅</div></div>
        <div class="stat-value">{{ stats.runningCount }}</div>
        <div class="stat-footer">正常生产运行</div>
      </div>
      <div class="stat-card red">
        <div class="stat-header"><span class="stat-title">维修中</span><div class="stat-icon">🛠️</div></div>
        <div class="stat-value">{{ stats.repairCount }}</div>
        <div class="stat-footer">故障维修设备</div>
      </div>
      <div class="stat-card purple">
        <div class="stat-header"><span class="stat-title">7 天内待保养</span><div class="stat-icon">📅</div></div>
        <div class="stat-value">{{ stats.maintainDueCount }}</div>
        <div class="stat-footer">保修 30 天内到期 {{ stats.warrantyExpiringCount }}</div>
      </div>
    </div>

    <div class="table-card">
      <div class="filter-bar">
        <div class="filter-left">
          <el-input v-model="keyword" clearable placeholder="搜索编码/名称/型号/品牌/客户" style="width:280px" :prefix-icon="Search" @keyup.enter="searchData" @clear="searchData" />
          <el-select v-model="statusFilter" clearable placeholder="设备状态" style="width:130px" @change="searchData">
            <el-option v-for="opt in statusOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
          <el-select v-model="categoryFilter" clearable placeholder="设备分类" style="width:130px" @change="searchData">
            <el-option v-for="opt in categories" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
          <el-input v-model="customerFilter" clearable placeholder="客户名称" style="width:180px" @keyup.enter="searchData" @clear="searchData" />
          <el-button type="primary" :icon="Search" @click="searchData">查询</el-button>
        </div>
      </div>

      <el-table v-loading="loading" :data="rows" stripe border style="width:100%" empty-text="暂无设备数据">
        <el-table-column prop="code" label="设备编码" width="130" />
        <el-table-column prop="name" label="设备名称" min-width="180">
          <template #default="{ row }"><b>{{ row.name }}</b></template>
        </el-table-column>
        <el-table-column prop="model" label="型号" width="140" />
        <el-table-column prop="brand" label="品牌" width="100" />
        <el-table-column label="分类" width="110" align="center">
          <template #default="{ row }"><el-tag effect="plain" round>{{ categoryLabel(row.category) }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="customerName" label="客户" min-width="170" show-overflow-tooltip />
        <el-table-column prop="location" label="安装位置" min-width="140" show-overflow-tooltip />
        <el-table-column prop="manager" label="负责人" width="100" />
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" effect="light">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="nextMaintainDate" label="下次保养" width="120" />
        <el-table-column prop="warrantyEnd" label="保修到期" width="120" />
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

    <el-dialog v-model="formVisible" :title="formMode === 'add' ? '新增设备' : '编辑设备'" width="820px" :close-on-click-modal="false" @closed="resetForm">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-row :gutter="20">
          <el-col :span="12"><el-form-item label="设备编码" prop="code"><el-input v-model="form.code" :disabled="formMode === 'edit'" maxlength="50" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="设备名称" prop="name"><el-input v-model="form.name" maxlength="100" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="规格型号"><el-input v-model="form.model" maxlength="100" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="品牌"><el-input v-model="form.brand" maxlength="50" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="设备分类"><el-select v-model="form.category" clearable placeholder="请选择分类" style="width:100%"><el-option v-for="opt in categories" :key="opt.value" :label="opt.label" :value="opt.value" /></el-select></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="运行状态"><el-select v-model="form.status" placeholder="请选择状态" style="width:100%"><el-option v-for="opt in statusOptions" :key="opt.value" :label="opt.label" :value="opt.value" /></el-select></el-form-item></el-col>
          <el-col :span="24"><el-form-item label="客户名称"><el-input v-model="form.customerName" maxlength="100" placeholder="输入客户名称，保存时由后端自动关联客户档案" /></el-form-item></el-col>
          <el-col :span="24"><el-form-item label="安装位置"><el-input v-model="form.location" maxlength="200" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="安装日期"><el-date-picker v-model="form.installDate" type="date" value-format="YYYY-MM-DD" placeholder="请选择日期" style="width:100%" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="保修到期"><el-date-picker v-model="form.warrantyEnd" type="date" value-format="YYYY-MM-DD" placeholder="请选择日期" style="width:100%" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="上次保养"><el-date-picker v-model="form.lastMaintainDate" type="date" value-format="YYYY-MM-DD" placeholder="请选择日期" style="width:100%" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="下次保养"><el-date-picker v-model="form.nextMaintainDate" type="date" value-format="YYYY-MM-DD" placeholder="请选择日期" style="width:100%" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="负责人"><el-input v-model="form.manager" maxlength="50" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="联系电话"><el-input v-model="form.phone" maxlength="50" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="采购金额"><el-input-number v-model="form.purchasePrice" :min="0" :precision="2" :step="100" controls-position="right" style="width:100%" /></el-form-item></el-col>
          <el-col :span="24"><el-form-item label="备注"><el-input v-model="form.remark" type="textarea" :rows="3" maxlength="500" show-word-limit /></el-form-item></el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitForm">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="detailVisible" title="设备详情" width="760px">
      <el-descriptions v-if="detail" :column="2" border>
        <el-descriptions-item label="设备编码">{{ detail.code }}</el-descriptions-item>
        <el-descriptions-item label="设备名称">{{ detail.name }}</el-descriptions-item>
        <el-descriptions-item label="规格型号">{{ detail.model || '-' }}</el-descriptions-item>
        <el-descriptions-item label="品牌">{{ detail.brand || '-' }}</el-descriptions-item>
        <el-descriptions-item label="设备分类">{{ categoryLabel(detail.category) }}</el-descriptions-item>
        <el-descriptions-item label="运行状态">
          <el-tag :type="statusTagType(detail.status)" effect="light">{{ statusLabel(detail.status) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="客户名称">{{ detail.customerName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="安装位置">{{ detail.location || '-' }}</el-descriptions-item>
        <el-descriptions-item label="安装日期">{{ detail.installDate || '-' }}</el-descriptions-item>
        <el-descriptions-item label="保修到期">{{ detail.warrantyEnd || '-' }}</el-descriptions-item>
        <el-descriptions-item label="上次保养">{{ detail.lastMaintainDate || '-' }}</el-descriptions-item>
        <el-descriptions-item label="下次保养">{{ detail.nextMaintainDate || '-' }}</el-descriptions-item>
        <el-descriptions-item label="负责人">{{ detail.manager || '-' }}</el-descriptions-item>
        <el-descriptions-item label="联系电话">{{ detail.phone || '-' }}</el-descriptions-item>
        <el-descriptions-item label="采购金额">{{ formatMoney(detail.purchasePrice) }}</el-descriptions-item>
        <el-descriptions-item label="更新时间">{{ detail.updateTime || '-' }}</el-descriptions-item>
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
  createEquipment,
  deleteEquipment,
  getEquipment,
  getEquipmentStats,
  listEquipments,
  updateEquipment,
  type EquipmentItem,
  type EquipmentStats,
} from '@/api/maint/equipment'

const statusOptions = [
  { value: 'running', label: '运行中' },
  { value: 'idle', label: '闲置' },
  { value: 'repair', label: '维修中' },
  { value: 'scrapped', label: '已报废' },
]

const categories = [
  { value: '控制柜', label: '控制柜' },
  { value: 'PLC', label: 'PLC' },
  { value: '变频器', label: '变频器' },
  { value: '伺服电机', label: '伺服电机' },
  { value: '仪表', label: '仪表' },
  { value: '其他', label: '其他' },
]

const loading = ref(false)
const submitting = ref(false)
const rows = ref<EquipmentItem[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)
const keyword = ref('')
const statusFilter = ref('')
const categoryFilter = ref('')
const customerFilter = ref('')
const formVisible = ref(false)
const detailVisible = ref(false)
const formMode = ref<'add' | 'edit'>('add')
const formRef = ref<FormInstance>()
const detail = ref<EquipmentItem | null>(null)
const stats = ref<EquipmentStats>({
  total: 0,
  runningCount: 0,
  idleCount: 0,
  repairCount: 0,
  scrappedCount: 0,
  warrantyExpiringCount: 0,
  maintainDueCount: 0,
})

const form = reactive<EquipmentItem>(emptyForm())

const rules: FormRules = {
  code: [{ required: true, message: '请填写设备编码', trigger: 'blur' }],
  name: [{ required: true, message: '请填写设备名称', trigger: 'blur' }],
}

function emptyForm(): EquipmentItem {
  return {
    code: '',
    name: '',
    model: '',
    brand: '',
    category: '',
    customerId: null,
    customerName: '',
    location: '',
    installDate: '',
    warrantyEnd: '',
    status: 'running',
    manager: '',
    phone: '',
    purchasePrice: null,
    lastMaintainDate: '',
    nextMaintainDate: '',
    remark: '',
  }
}

function statusLabel(value?: string) {
  const found = statusOptions.find((opt) => opt.value === value)
  return found ? found.label : value || '-'
}

function statusTagType(value?: string): 'success' | 'info' | 'warning' | 'danger' {
  switch (value) {
    case 'running':
      return 'success'
    case 'idle':
      return 'info'
    case 'repair':
      return 'danger'
    case 'scrapped':
      return 'warning'
    default:
      return 'info'
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

function resetForm() {
  Object.assign(form, emptyForm())
  formRef.value?.clearValidate()
}

async function loadData() {
  loading.value = true
  try {
    const [listRes, statsRes] = await Promise.all([
      listEquipments({
        keyword: keyword.value.trim() || undefined,
        status: statusFilter.value || undefined,
        category: categoryFilter.value || undefined,
        customerName: customerFilter.value.trim() || undefined,
        page: page.value,
        pageSize: pageSize.value,
      }),
      getEquipmentStats(),
    ])
    if (listRes.code !== 0) {
      ElMessage.error(listRes.message || '加载设备失败')
      return
    }
    if (statsRes.code === 0) stats.value = statsRes.data
    rows.value = listRes.data?.rows || []
    total.value = listRes.data?.total || 0
  } catch (error: any) {
    ElMessage.error(error?.message || '加载设备失败')
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

function openEdit(row: EquipmentItem) {
  formMode.value = 'edit'
  resetForm()
  Object.assign(form, row)
  formVisible.value = true
}

async function openDetail(row: EquipmentItem) {
  if (!row.id) return
  try {
    const res = await getEquipment(row.id)
    if (res.code !== 0) {
      ElMessage.error(res.message || '加载设备详情失败')
      return
    }
    detail.value = res.data
    detailVisible.value = true
  } catch (error: any) {
    ElMessage.error(error?.message || '加载设备详情失败')
  }
}

async function submitForm() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    const payload: EquipmentItem = { ...form }
    const res = formMode.value === 'add' ? await createEquipment(payload) : await updateEquipment(payload)
    if (res.code !== 0) {
      ElMessage.error(res.message || '保存失败')
      return
    }
    ElMessage.success(formMode.value === 'add' ? '设备已创建' : '设备已更新')
    formVisible.value = false
    await loadData()
  } catch (error: any) {
    ElMessage.error(error?.message || '保存失败')
  } finally {
    submitting.value = false
  }
}

async function handleDelete(row: EquipmentItem) {
  if (!row.id) return
  try {
    await ElMessageBox.confirm(`确认删除设备 ${row.name} 吗？`, '删除确认', { type: 'warning' })
  } catch {
    return
  }
  try {
    const res = await deleteEquipment(row.id)
    if (res.code !== 0) {
      ElMessage.error(res.message || '删除失败')
      return
    }
    ElMessage.success('设备已删除')
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
