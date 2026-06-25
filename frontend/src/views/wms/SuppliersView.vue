<template>
  <section>
    <div class="sub-page-header">
      <div>
        <h2>供应商管理</h2>
        <p class="page-desc">维护供应商档案、结算方式和供货物料关系。</p>
      </div>
      <el-space>
        <el-button plain :icon="Refresh" @click="loadData">刷新</el-button>
        <el-button type="primary" :icon="Plus" @click="openAdd">新增供应商</el-button>
      </el-space>
    </div>

    <div class="stats-row">
      <div class="stat-card blue">
        <div class="stat-header"><span class="stat-title">供应商总数</span><div class="stat-icon">🤝</div></div>
        <div class="stat-value">{{ stats.total }}</div>
        <div class="stat-footer">已维护供应商数</div>
      </div>
      <div class="stat-card green">
        <div class="stat-header"><span class="stat-title">启用供应商</span><div class="stat-icon">✅</div></div>
        <div class="stat-value">{{ stats.activeCount }}</div>
        <div class="stat-footer">业务可用供应商</div>
      </div>
      <div class="stat-card orange">
        <div class="stat-header"><span class="stat-title">禁用供应商</span><div class="stat-icon">🚫</div></div>
        <div class="stat-value">{{ stats.inactiveCount }}</div>
        <div class="stat-footer">已停用供应商</div>
      </div>
      <div class="stat-card purple">
        <div class="stat-header"><span class="stat-title">关联物料</span><div class="stat-icon">📦</div></div>
        <div class="stat-value">{{ stats.materialTotal }}</div>
        <div class="stat-footer">已绑定物料数量</div>
      </div>
    </div>

    <div class="table-card">
      <div class="filter-bar">
        <div class="filter-left">
          <el-input v-model="keyword" clearable placeholder="搜索编码/名称/简称/联系人" style="width:280px" :prefix-icon="Search" @keyup.enter="searchData" @clear="searchData" />
          <el-select v-model="categoryFilter" clearable placeholder="供应商类型" style="width:150px" @change="searchData">
            <el-option v-for="opt in categories" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
          <el-select v-model="statusFilter" clearable placeholder="状态" style="width:120px" @change="searchData">
            <el-option label="启用" :value="1" />
            <el-option label="禁用" :value="0" />
          </el-select>
          <el-button type="primary" :icon="Search" @click="searchData">查询</el-button>
        </div>
      </div>

      <el-table v-loading="loading" :data="pagedRows" stripe border style="width:100%">
        <el-table-column prop="code" label="供应商编码" width="130" />
        <el-table-column prop="name" label="供应商名称" min-width="180">
          <template #default="{ row }"><b>{{ row.name }}</b></template>
        </el-table-column>
        <el-table-column prop="shortName" label="简称" width="120" />
        <el-table-column label="类型" width="100" align="center">
          <template #default="{ row }"><el-tag effect="plain" round>{{ categoryLabel(row.category) }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="contact" label="联系人" width="100" />
        <el-table-column prop="phone" label="联系电话" width="140" />
        <el-table-column prop="settleType" label="结算方式" width="110" />
        <el-table-column label="物料数" width="90" align="center">
          <template #default="{ row }"><el-tag size="small" type="success">{{ row.materialCount ?? 0 }}</el-tag></template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-switch :model-value="row.status === 1" @change="(val: boolean) => toggleStatus(row, val)" />
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

    <el-dialog v-model="formVisible" :title="formMode === 'add' ? '新增供应商' : '编辑供应商'" width="760px" :close-on-click-modal="false" @closed="resetForm">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-row :gutter="20">
          <el-col :span="12"><el-form-item label="供应商编码" prop="code"><el-input v-model="form.code" :disabled="formMode === 'edit'" maxlength="50" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="供应商名称" prop="name"><el-input v-model="form.name" maxlength="100" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="简称"><el-input v-model="form.shortName" maxlength="50" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="供应商类型"><el-select v-model="form.category" placeholder="请选择类型" style="width:100%"><el-option v-for="opt in categories" :key="opt.value" :label="opt.label" :value="opt.value" /></el-select></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="联系人"><el-input v-model="form.contact" maxlength="50" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="联系电话"><el-input v-model="form.phone" maxlength="50" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="邮箱"><el-input v-model="form.email" maxlength="100" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="结算方式"><el-select v-model="form.settleType" clearable placeholder="请选择结算方式" style="width:100%"><el-option v-for="opt in settleTypes" :key="opt" :label="opt" :value="opt" /></el-select></el-form-item></el-col>
          <el-col :span="24"><el-form-item label="地址"><el-input v-model="form.address" maxlength="255" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="纳税识别号"><el-input v-model="form.taxNo" maxlength="50" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="排序"><el-input-number v-model="form.sort" :min="0" :step="1" controls-position="right" style="width:100%" /></el-form-item></el-col>
          <el-col :span="24"><el-form-item label="银行账户"><el-input v-model="form.bankAccount" maxlength="100" placeholder="开户行 + 账号" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="状态"><el-switch v-model="form.status" :active-value="1" :inactive-value="0" active-text="启用" inactive-text="禁用" /></el-form-item></el-col>
          <el-col :span="24"><el-form-item label="备注描述"><el-input v-model="form.description" type="textarea" :rows="3" maxlength="500" show-word-limit /></el-form-item></el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitForm">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="detailVisible" title="供应商详情" width="680px">
      <el-descriptions v-if="detail" :column="2" border>
        <el-descriptions-item label="供应商编码">{{ detail.code }}</el-descriptions-item>
        <el-descriptions-item label="供应商名称">{{ detail.name }}</el-descriptions-item>
        <el-descriptions-item label="简称">{{ detail.shortName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="类型">{{ categoryLabel(detail.category) }}</el-descriptions-item>
        <el-descriptions-item label="联系人">{{ detail.contact || '-' }}</el-descriptions-item>
        <el-descriptions-item label="联系电话">{{ detail.phone || '-' }}</el-descriptions-item>
        <el-descriptions-item label="邮箱">{{ detail.email || '-' }}</el-descriptions-item>
        <el-descriptions-item label="结算方式">{{ detail.settleType || '-' }}</el-descriptions-item>
        <el-descriptions-item label="地址" :span="2">{{ detail.address || '-' }}</el-descriptions-item>
        <el-descriptions-item label="纳税识别号">{{ detail.taxNo || '-' }}</el-descriptions-item>
        <el-descriptions-item label="物料数">{{ detail.materialCount ?? 0 }}</el-descriptions-item>
        <el-descriptions-item label="银行账户" :span="2">{{ detail.bankAccount || '-' }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ detail.status === 1 ? '启用' : '禁用' }}</el-descriptions-item>
        <el-descriptions-item label="排序">{{ detail.sort ?? 0 }}</el-descriptions-item>
        <el-descriptions-item label="描述" :span="2">{{ detail.description || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'
import { changeSupplierStatus, createSupplier, deleteSupplier, getSupplierStats, listSuppliers, updateSupplier, type SupplierItem, type SupplierStats } from '@/api/wms/supplier'

const categories = [
  { value: 'raw', label: '原料供应商' },
  { value: 'logistics', label: '物流服务商' },
  { value: 'service', label: '服务供应商' },
  { value: 'other', label: '其他' },
]
const settleTypes = ['货到付款', '预付款', '月结30天', '月结60天', '季度结算']

const loading = ref(false)
const submitting = ref(false)
const rows = ref<SupplierItem[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)
const keyword = ref('')
const categoryFilter = ref('')
const statusFilter = ref<number | null>(null)
const formVisible = ref(false)
const detailVisible = ref(false)
const formMode = ref<'add' | 'edit'>('add')
const formRef = ref<FormInstance>()
const detail = ref<SupplierItem | null>(null)
const stats = ref<SupplierStats>({ total: 0, activeCount: 0, inactiveCount: 0, materialTotal: 0 })

const form = reactive<SupplierItem>(emptyForm())
const pagedRows = computed(() => rows.value.slice((page.value - 1) * pageSize.value, page.value * pageSize.value))

const rules: FormRules = {
  code: [{ required: true, message: '请填写供应商编码', trigger: 'blur' }],
  name: [{ required: true, message: '请填写供应商名称', trigger: 'blur' }],
}

function emptyForm(): SupplierItem {
  return { code: '', name: '', shortName: '', category: 'raw', contact: '', phone: '', email: '', address: '', taxNo: '', bankAccount: '', settleType: '', description: '', sort: 0, status: 1 }
}

function categoryLabel(value?: string) {
  const found = categories.find((c) => c.value === value)
  return found ? found.label : value || '-'
}

function resetForm() {
  Object.assign(form, emptyForm())
  formRef.value?.clearValidate()
}

async function loadData() {
  loading.value = true
  try {
    const [listRes, statsRes] = await Promise.all([
      listSuppliers({ keyword: keyword.value.trim() || undefined, category: categoryFilter.value || undefined, status: statusFilter.value }),
      getSupplierStats(),
    ])
    if (listRes.code !== 0) {
      ElMessage.error(listRes.message || '加载供应商失败')
      return
    }
    if (statsRes.code === 0) stats.value = statsRes.data
    rows.value = listRes.data || []
    total.value = rows.value.length
    const maxPage = Math.max(1, Math.ceil(total.value / pageSize.value))
    if (page.value > maxPage) page.value = maxPage
  } catch (error: any) {
    ElMessage.error(error?.message || '加载供应商失败')
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

function openEdit(row: SupplierItem) {
  formMode.value = 'edit'
  resetForm()
  Object.assign(form, row)
  formVisible.value = true
}

function openDetail(row: SupplierItem) {
  detail.value = row
  detailVisible.value = true
}

async function submitForm() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    const res = formMode.value === 'add' ? await createSupplier({ ...form }) : await updateSupplier({ ...form })
    if (res.code !== 0) {
      ElMessage.error(res.message || '保存失败')
      return
    }
    ElMessage.success(formMode.value === 'add' ? '供应商已创建' : '供应商已更新')
    formVisible.value = false
    await loadData()
  } catch (error: any) {
    ElMessage.error(error?.message || '保存失败')
  } finally {
    submitting.value = false
  }
}

async function handleDelete(row: SupplierItem) {
  if (!row.id) return
  try {
    await ElMessageBox.confirm(`确认删除供应商 ${row.name} 吗？`, '删除确认', { type: 'warning' })
  } catch {
    return
  }
  try {
    const res = await deleteSupplier(row.id)
    if (res.code !== 0) {
      ElMessage.error(res.message || '删除失败')
      return
    }
    ElMessage.success('供应商已删除')
    await loadData()
  } catch (error: any) {
    ElMessage.error(error?.message || '删除失败')
  }
}

async function toggleStatus(row: SupplierItem, val: boolean) {
  if (!row.id) return
  const previous = row.status
  row.status = val ? 1 : 0
  try {
    const res = await changeSupplierStatus(row.id, val ? 1 : 0)
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
