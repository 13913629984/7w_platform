<template>
  <section>
    <div class="sub-page-header">
      <div>
        <h2>来货登记</h2>
        <p class="page-desc">登记待测电气元件批次、客户与样品信息，跟踪收样与测试状态。</p>
      </div>
      <el-space>
        <el-button plain :icon="Refresh" @click="loadData">刷新</el-button>
        <el-button type="primary" :icon="Plus" @click="openAdd">新增登记</el-button>
      </el-space>
    </div>

    <div class="stats-row">
      <div class="stat-card blue">
        <div class="stat-header"><span class="stat-title">登记总数</span><div class="stat-icon">🧾</div></div>
        <div class="stat-value">{{ stats.total }}</div>
        <div class="stat-footer">本月新增 {{ stats.monthAdded }} 批</div>
      </div>
      <div class="stat-card orange">
        <div class="stat-header"><span class="stat-title">待收样</span><div class="stat-icon">📥</div></div>
        <div class="stat-value">{{ stats.pending }}</div>
        <div class="stat-footer">已收样 {{ stats.received }} 批</div>
      </div>
      <div class="stat-card purple">
        <div class="stat-header"><span class="stat-title">测试中</span><div class="stat-icon">🧪</div></div>
        <div class="stat-value">{{ stats.testing }}</div>
        <div class="stat-footer">已完成 {{ stats.done }} 批</div>
      </div>
      <div class="stat-card green">
        <div class="stat-header"><span class="stat-title">来货总量</span><div class="stat-icon">📦</div></div>
        <div class="stat-value">{{ stats.totalQuantity }}</div>
        <div class="stat-footer">已退回 {{ stats.returned }} 批</div>
      </div>
    </div>

    <div class="table-card">
      <div class="filter-bar">
        <div class="filter-left">
          <el-input v-model="keyword" clearable placeholder="搜索登记号/客户/产品/型号/批次" style="width:300px" :prefix-icon="Search" @keyup.enter="searchData" @clear="searchData" />
          <el-select v-model="statusFilter" clearable placeholder="来货状态" style="width:140px" @change="searchData">
            <el-option v-for="opt in statusOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
          <el-input v-model="customerFilter" clearable placeholder="客户名称" style="width:180px" @keyup.enter="searchData" @clear="searchData" />
          <el-button type="primary" :icon="Search" @click="searchData">查询</el-button>
        </div>
      </div>

      <el-table v-loading="loading" :data="rows" stripe border style="width:100%">
        <el-table-column prop="registerNo" label="登记编号" width="150">
          <template #default="{ row }"><b>{{ row.registerNo || '-' }}</b></template>
        </el-table-column>
        <el-table-column prop="customerName" label="客户名称" min-width="160" show-overflow-tooltip />
        <el-table-column prop="product" label="产品名称" min-width="150" show-overflow-tooltip />
        <el-table-column prop="model" label="型号" width="120" />
        <el-table-column prop="batchNo" label="批次号" width="120" />
        <el-table-column label="来货/抽样" width="110" align="center">
          <template #default="{ row }">{{ row.quantity ?? 0 }} / {{ row.sampleQty ?? 0 }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }"><el-tag :type="statusTagType(row.status)" effect="light">{{ statusLabel(row.status) }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="arriveDate" label="来货日期" width="120" align="center" />
        <el-table-column prop="expectDate" label="预计完成" width="120" align="center" />
        <el-table-column prop="operator" label="登记人" width="100" />
        <el-table-column label="操作" width="180" fixed="right" align="center">
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

    <el-dialog v-model="formVisible" :title="formMode === 'add' ? '新增来货登记' : '编辑来货登记'" width="820px" :close-on-click-modal="false" @closed="resetForm">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-row :gutter="20">
          <el-col :span="12"><el-form-item label="登记编号"><el-input v-model="form.registerNo" maxlength="50" placeholder="留空自动生成" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="客户名称" prop="customerName"><el-input v-model="form.customerName" maxlength="100" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="联系人"><el-input v-model="form.contact" maxlength="50" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="联系电话"><el-input v-model="form.phone" maxlength="50" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="产品名称" prop="product"><el-input v-model="form.product" maxlength="100" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="规格型号"><el-input v-model="form.model" maxlength="100" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="品牌"><el-input v-model="form.brand" maxlength="50" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="批次号"><el-input v-model="form.batchNo" maxlength="50" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="来货数量"><el-input-number v-model="form.quantity" :min="0" :step="1" controls-position="right" style="width:100%" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="抽样数量"><el-input-number v-model="form.sampleQty" :min="0" :step="1" controls-position="right" style="width:100%" /></el-form-item></el-col>
          <el-col :span="12">
            <el-form-item label="来货状态">
              <el-select v-model="form.status" placeholder="请选择状态" style="width:100%">
                <el-option v-for="opt in statusOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12"><el-form-item label="来货日期"><el-date-picker v-model="form.arriveDate" type="date" value-format="YYYY-MM-DD" placeholder="选择日期" style="width:100%" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="预计完成"><el-date-picker v-model="form.expectDate" type="date" value-format="YYYY-MM-DD" placeholder="选择日期" style="width:100%" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="登记人"><el-input v-model="form.operator" maxlength="50" /></el-form-item></el-col>
          <el-col :span="24"><el-form-item label="测试标准"><el-input v-model="form.testStandard" maxlength="200" placeholder="如 GB/T 14048.1-2020" /></el-form-item></el-col>
          <el-col :span="24"><el-form-item label="备注"><el-input v-model="form.remark" type="textarea" :rows="3" maxlength="500" show-word-limit /></el-form-item></el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitForm">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="detailVisible" title="来货登记详情" width="720px">
      <el-descriptions v-if="detail" :column="2" border>
        <el-descriptions-item label="登记编号">{{ detail.registerNo || '-' }}</el-descriptions-item>
        <el-descriptions-item label="来货状态">
          <el-tag :type="statusTagType(detail.status)" effect="light">{{ statusLabel(detail.status) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="客户名称">{{ detail.customerName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="联系人">{{ detail.contact || '-' }}</el-descriptions-item>
        <el-descriptions-item label="联系电话">{{ detail.phone || '-' }}</el-descriptions-item>
        <el-descriptions-item label="产品名称">{{ detail.product || '-' }}</el-descriptions-item>
        <el-descriptions-item label="规格型号">{{ detail.model || '-' }}</el-descriptions-item>
        <el-descriptions-item label="品牌">{{ detail.brand || '-' }}</el-descriptions-item>
        <el-descriptions-item label="批次号">{{ detail.batchNo || '-' }}</el-descriptions-item>
        <el-descriptions-item label="来货/抽样">{{ detail.quantity ?? 0 }} / {{ detail.sampleQty ?? 0 }}</el-descriptions-item>
        <el-descriptions-item label="来货日期">{{ detail.arriveDate || '-' }}</el-descriptions-item>
        <el-descriptions-item label="预计完成">{{ detail.expectDate || '-' }}</el-descriptions-item>
        <el-descriptions-item label="测试标准" :span="2">{{ detail.testStandard || '-' }}</el-descriptions-item>
        <el-descriptions-item label="登记人">{{ detail.operator || '-' }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ detail.createTime || '-' }}</el-descriptions-item>
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
  createRegister,
  deleteRegister,
  getRegister,
  listRegisters,
  updateRegister,
  type RegisterItem,
  type RegisterStats,
} from '@/api/ets/register'

const statusOptions = [
  { value: 'pending', label: '待收样' },
  { value: 'received', label: '已收样' },
  { value: 'testing', label: '测试中' },
  { value: 'done', label: '已完成' },
  { value: 'returned', label: '已退回' },
]

const STATUS_TAG: Record<string, string> = {
  pending: 'info',
  received: 'primary',
  testing: 'warning',
  done: 'success',
  returned: 'danger',
}

const loading = ref(false)
const submitting = ref(false)
const rows = ref<RegisterItem[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)
const keyword = ref('')
const statusFilter = ref('')
const customerFilter = ref('')
const formVisible = ref(false)
const detailVisible = ref(false)
const formMode = ref<'add' | 'edit'>('add')
const formRef = ref<FormInstance>()
const detail = ref<RegisterItem | null>(null)
const stats = ref<RegisterStats>({ total: 0, pending: 0, received: 0, testing: 0, done: 0, returned: 0, monthAdded: 0, totalQuantity: 0 })

const form = reactive<RegisterItem>(emptyForm())

const rules: FormRules = {
  customerName: [{ required: true, message: '请填写客户名称', trigger: 'blur' }],
  product: [{ required: true, message: '请填写产品名称', trigger: 'blur' }],
}

function emptyForm(): RegisterItem {
  return {
    registerNo: '',
    customerName: '',
    contact: '',
    phone: '',
    product: '',
    model: '',
    brand: '',
    batchNo: '',
    quantity: 0,
    sampleQty: 0,
    status: 'pending',
    arriveDate: '',
    expectDate: '',
    testStandard: '',
    operator: '',
    remark: '',
  }
}

function statusLabel(value?: string) {
  const found = statusOptions.find((item) => item.value === value)
  return found ? found.label : value || '待收样'
}

function statusTagType(value?: string) {
  return STATUS_TAG[value || 'pending'] || 'info'
}

function resetForm() {
  Object.assign(form, emptyForm())
  delete form.id
  formRef.value?.clearValidate()
}

async function loadData() {
  loading.value = true
  try {
    const res = await listRegisters({
      keyword: keyword.value.trim() || undefined,
      status: statusFilter.value || undefined,
      customerName: customerFilter.value.trim() || undefined,
      page: page.value,
      pageSize: pageSize.value,
    })
    if (res.code !== 0) {
      ElMessage.error(res.message || '加载来货登记失败')
      return
    }
    rows.value = res.data?.rows || []
    total.value = Number(res.data?.total || 0)
    if (res.data?.stats) stats.value = res.data.stats
  } catch (error: any) {
    ElMessage.error(error?.message || '加载来货登记失败')
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

function openEdit(row: RegisterItem) {
  formMode.value = 'edit'
  resetForm()
  Object.assign(form, row)
  formVisible.value = true
}

async function openDetail(row: RegisterItem) {
  if (!row.id) return
  try {
    const res = await getRegister(row.id)
    if (res.code !== 0) {
      ElMessage.error(res.message || '加载详情失败')
      return
    }
    detail.value = res.data
    detailVisible.value = true
  } catch (error: any) {
    ElMessage.error(error?.message || '加载详情失败')
  }
}

async function submitForm() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    const res = formMode.value === 'add' ? await createRegister({ ...form }) : await updateRegister({ ...form })
    if (res.code !== 0) {
      ElMessage.error(res.message || '保存失败')
      return
    }
    ElMessage.success(formMode.value === 'add' ? '来货登记已创建' : '来货登记已更新')
    formVisible.value = false
    await loadData()
  } catch (error: any) {
    ElMessage.error(error?.message || '保存失败')
  } finally {
    submitting.value = false
  }
}

async function handleDelete(row: RegisterItem) {
  if (!row.id) return
  try {
    await ElMessageBox.confirm(`确认删除来货登记 ${row.registerNo || ''} 吗？`, '删除确认', { type: 'warning' })
  } catch {
    return
  }
  try {
    const res = await deleteRegister(row.id)
    if (res.code !== 0) {
      ElMessage.error(res.message || '删除失败')
      return
    }
    ElMessage.success('来货登记已删除')
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
