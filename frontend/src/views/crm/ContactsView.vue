<template>
  <section>
    <div class="sub-page-header">
      <div>
        <h2>联系人管理</h2>
        <p class="page-desc">维护客户联系人和沟通信息。</p>
      </div>
      <div class="header-actions">
        <el-button plain :icon="Download" @click="handleExport">导出</el-button>
        <el-button plain :icon="Refresh" @click="loadData">刷新</el-button>
        <el-button type="primary" :icon="Plus" @click="openAdd">新增联系人</el-button>
      </div>
    </div>

    <div class="stats-row">
      <div class="stat-card blue">
        <div class="stat-header"><span class="stat-title">联系人总数</span><div class="stat-icon">👥</div></div>
        <div class="stat-value">{{ stats.total }}</div>
        <div class="stat-footer">全部联系人</div>
      </div>
      <div class="stat-card green">
        <div class="stat-header"><span class="stat-title">主要联系人</span><div class="stat-icon">⭐</div></div>
        <div class="stat-value">{{ stats.primary }}</div>
        <div class="stat-footer">关键联系人</div>
      </div>
      <div class="stat-card orange">
        <div class="stat-header"><span class="stat-title">本月新增</span><div class="stat-icon">📈</div></div>
        <div class="stat-value">{{ stats.monthAdded }}</div>
        <div class="stat-footer">新联系人</div>
      </div>
      <div class="stat-card purple">
        <div class="stat-header"><span class="stat-title">客户覆盖</span><div class="stat-icon">🏢</div></div>
        <div class="stat-value">{{ stats.customerCount }}个客户</div>
        <div class="stat-footer">联系覆盖</div>
      </div>
    </div>

    <div class="table-card">
      <div class="filter-bar">
        <div class="filter-left">
          <el-input
            v-model="filters.keyword"
            clearable
            placeholder="搜索联系人/电话/邮箱..."
            :prefix-icon="Search"
            style="width:240px"
            @keyup.enter="handleSearch"
            @clear="handleSearch"
          />
          <el-select v-model="filters.customerId" clearable placeholder="客户筛选" style="width:200px" @change="handleSearch">
            <el-option v-for="opt in customerOptions" :key="opt.id" :label="opt.name" :value="opt.id" />
          </el-select>
        </div>
        <div class="filter-right">
          <el-button v-if="selected.length" type="danger" plain :icon="Delete" @click="batchDelete">批量删除 ({{ selected.length }})</el-button>
        </div>
      </div>

      <el-table v-loading="loading" :data="rows" stripe style="width:100%" @selection-change="onSelectionChange">
        <el-table-column type="selection" width="46" />
        <el-table-column prop="name" label="联系人" min-width="100" show-overflow-tooltip>
          <template #default="{ row }"><span class="contact-name">{{ row.name }}</span></template>
        </el-table-column>
        <el-table-column prop="title" label="职位" min-width="110" show-overflow-tooltip />
        <el-table-column prop="customerName" label="客户名称" min-width="160" show-overflow-tooltip>
          <template #default="{ row }">{{ row.customerName || '-' }}</template>
        </el-table-column>
        <el-table-column prop="phone" label="联系电话" min-width="130" />
        <el-table-column prop="email" label="电子邮箱" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">{{ row.email || '-' }}</template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="130" align="center">
          <template #default="{ row }">{{ row.createTime || '-' }}</template>
        </el-table-column>
        <el-table-column label="是否主联系人" width="120" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.isPrimary" type="success" effect="light">是</el-tag>
            <el-tag v-else type="info" effect="plain">否</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="170" align="center" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">详情</el-button>
            <el-button link type="primary" @click="openEdit(row)">修改</el-button>
            <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="crm-pagination">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          background
          layout="total, sizes, prev, pager, next, jumper"
          @current-change="handlePageChange"
          @size-change="handleSizeChange"
        />
      </div>
    </div>

    <el-dialog v-model="formVisible" :title="formMode === 'add' ? '新增联系人' : '编辑联系人'" width="520px" :close-on-click-modal="false" @closed="resetForm">
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="92px" label-position="right">
        <el-form-item label="联系人姓名" prop="name">
          <el-input v-model="form.name" placeholder="请输入联系人姓名" maxlength="50" />
        </el-form-item>
        <el-form-item label="职位" prop="title">
          <el-input v-model="form.title" placeholder="请输入职位" maxlength="50" />
        </el-form-item>
        <el-form-item label="客户名称" prop="customerId">
          <el-select v-model="form.customerId" clearable filterable placeholder="请选择客户" style="width:100%" @change="onCustomerChange">
            <el-option v-for="opt in customerOptions" :key="opt.id" :label="opt.name" :value="opt.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="联系电话" prop="phone">
          <el-input v-model="form.phone" placeholder="请输入联系电话" maxlength="50" />
        </el-form-item>
        <el-form-item label="电子邮箱" prop="email">
          <el-input v-model="form.email" placeholder="请输入电子邮箱" maxlength="100" />
        </el-form-item>
        <el-form-item label="是否主联系人" prop="isPrimary">
          <el-switch v-model="form.isPrimary" />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" :rows="3" placeholder="请输入备注信息" maxlength="500" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" @click="submitForm">确定提交</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="detailVisible" title="联系人详情" width="520px" :close-on-click-modal="false">
      <el-descriptions v-if="detail" :column="1" border>
        <el-descriptions-item label="联系人姓名">{{ detail.name }}</el-descriptions-item>
        <el-descriptions-item label="职位">{{ detail.title || '-' }}</el-descriptions-item>
        <el-descriptions-item label="客户名称">{{ detail.customerName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="联系电话">{{ detail.phone || '-' }}</el-descriptions-item>
        <el-descriptions-item label="电子邮箱">{{ detail.email || '-' }}</el-descriptions-item>
        <el-descriptions-item label="是否主联系人">{{ detail.isPrimary ? '是' : '否' }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ detail.createTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="备注">{{ detail.remark || '-' }}</el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
        <el-button type="primary" @click="openEdit(detail!)">编辑</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Plus, Delete, Search, Download, Refresh } from '@element-plus/icons-vue'
import {
  batchDeleteContacts,
  createContact,
  deleteContact,
  listContactCustomers,
  listContacts,
  updateContact,
  type ContactItem,
  type CustomerOption,
} from '@/api/crm/contact'

type Contact = ContactItem

const loading = ref(false)
const rows = ref<Contact[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)
const selected = ref<Contact[]>([])
const customerOptions = ref<CustomerOption[]>([])

const filters = reactive<{ keyword: string; customerId: number | undefined }>({ keyword: '', customerId: undefined })

const stats = reactive({ total: 0, primary: 0, monthAdded: 0, customerCount: 0 })

async function loadData() {
  loading.value = true
  try {
    const res = await listContacts({
      keyword: filters.keyword.trim() || undefined,
      customerId: filters.customerId || undefined,
      page: page.value,
      pageSize: pageSize.value,
    })
    if (res.code !== 0) {
      ElMessage.error(res.message || '加载联系人失败')
      return
    }
    rows.value = res.data.rows || []
    total.value = res.data.total || 0
    if (res.data.stats) {
      stats.total = res.data.stats.total ?? 0
      stats.primary = res.data.stats.primary ?? 0
      stats.monthAdded = res.data.stats.monthAdded ?? 0
      stats.customerCount = res.data.stats.customerCount ?? 0
    }
  } catch (error: any) {
    ElMessage.error(error?.message || '加载联系人失败')
  } finally {
    loading.value = false
  }
}

async function loadCustomers() {
  try {
    const res = await listContactCustomers()
    if (res.code === 0) {
      customerOptions.value = res.data || []
    }
  } catch {
    // 客户下拉加载失败不阻塞主流程
  }
}

function handleSearch() {
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

function onSelectionChange(selection: Contact[]) {
  selected.value = selection
}

const formVisible = ref(false)
const formMode = ref<'add' | 'edit'>('add')
const formRef = ref<FormInstance>()

function createEmptyForm(): Contact {
  return { id: 0, name: '', title: '', customerId: undefined, customerName: '', phone: '', email: '', isPrimary: false, remark: '' }
}

const form = reactive<Contact>(createEmptyForm())

const formRules: FormRules = {
  name: [{ required: true, message: '请输入联系人姓名', trigger: 'blur' }],
  email: [
    {
      validator: (_rule, value, callback) => {
        if (value && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value)) {
          callback(new Error('电子邮箱格式不正确'))
        } else {
          callback()
        }
      },
      trigger: 'blur',
    },
  ],
}

function resetForm() {
  Object.assign(form, createEmptyForm())
  formRef.value?.clearValidate()
}

function onCustomerChange(id: number | undefined) {
  const matched = customerOptions.value.find((c) => c.id === id)
  form.customerName = matched ? matched.name : ''
}

function openAdd() {
  formMode.value = 'add'
  resetForm()
  formVisible.value = true
}

function openEdit(row: Contact) {
  formMode.value = 'edit'
  resetForm()
  Object.assign(form, { ...row, customerId: row.customerId ?? undefined })
  detailVisible.value = false
  formVisible.value = true
}

async function submitForm() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  const payload: Contact = { ...form }
  try {
    if (formMode.value === 'add') {
      const res = await createContact(payload)
      if (res.code !== 0) {
        ElMessage.error(res.message || '保存失败')
        return
      }
      ElMessage.success('联系人已添加')
    } else {
      const res = await updateContact(payload)
      if (res.code !== 0) {
        ElMessage.error(res.message || '保存失败')
        return
      }
      ElMessage.success('联系人已更新')
    }
    formVisible.value = false
    loadData()
  } catch (error: any) {
    ElMessage.error(error?.message || '保存失败')
  }
}

const detailVisible = ref(false)
const detail = ref<Contact | null>(null)

function openDetail(row: Contact) {
  detail.value = { ...row }
  detailVisible.value = true
}

async function handleDelete(row: Contact) {
  try {
    await ElMessageBox.confirm(`确认删除联系人「${row.name}」吗？`, '删除确认', { type: 'warning' })
  } catch {
    return
  }
  try {
    const res = await deleteContact(row.id!)
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

const batchDelete = async () => {
  if (!selected.value.length) return
  try {
    await ElMessageBox.confirm(`确认批量删除 ${selected.value.length} 个联系人吗？`, '批量删除', { type: 'warning' })
  } catch {
    return
  }
  try {
    const ids = selected.value.map((s) => s.id!).filter(Boolean)
    const res = await batchDeleteContacts(ids)
    if (res.code !== 0) {
      ElMessage.error(res.message || '批量删除失败')
      return
    }
    ElMessage.success(`已删除 ${res.data ?? ids.length} 个联系人`)
    selected.value = []
    loadData()
  } catch (error: any) {
    ElMessage.error(error?.message || '批量删除失败')
  }
}

function handleExport() {
  if (!rows.value.length) {
    ElMessage.warning('暂无可导出的联系人数据')
    return
  }
  const headers = ['联系人', '职位', '客户名称', '联系电话', '电子邮箱', '是否主联系人', '创建时间']
  const lines = rows.value.map((r) =>
    [r.name, r.title, r.customerName, r.phone, r.email, r.isPrimary ? '是' : '否', r.createTime || '']
      .map((v) => `"${String(v ?? '').replace(/"/g, '""')}"`)
      .join(','),
  )
  const blob = new Blob(['﻿' + headers.join(',') + '\n' + lines.join('\n')], { type: 'text/csv;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = '联系人列表.csv'
  link.click()
  URL.revokeObjectURL(url)
  ElMessage.success('已导出当前页联系人')
}

onMounted(() => {
  loadData()
  loadCustomers()
})
</script>

<style scoped>
.sub-page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 20px;
}
.header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}
.stats-row {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 20px;
  margin-bottom: 24px;
}
.stat-card {
  background: #fff;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, .04);
  position: relative;
  overflow: hidden;
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
.stat-title { font-size: 14px; color: #909399; min-width: 0; flex: 1; }
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
.stat-card.blue .stat-icon { background: #ecf5ff; }
.stat-card.green .stat-icon { background: #f0f9eb; }
.stat-card.orange .stat-icon { background: #fdf6ec; }
.stat-card.purple .stat-icon { background: #f4f0ff; }
.stat-value { font-size: 28px; font-weight: 700; color: #303133; margin-bottom: 8px; }
.stat-footer { font-size: 12px; color: #909399; }

.table-card {
  background: #fff;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, .04);
  min-width: 0;
}
.table-card .el-table { width: 100%; }
.contact-name { font-weight: 600; color: #303133; }
.filter-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
  flex-wrap: nowrap;
}
.filter-left {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  flex: 1;
  min-width: 0;
}
.filter-right {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}
.crm-pagination {
  display: flex;
  justify-content: flex-end;
  padding: 16px 0 4px;
}

@media (max-width: 768px) {
  .stats-row { grid-template-columns: 1fr; }
}
</style>
