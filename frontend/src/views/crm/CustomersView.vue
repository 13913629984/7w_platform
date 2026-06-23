<template>
  <section>
    <div class="sub-page-header">
      <div>
        <h2>客户管理</h2>
        <p class="page-desc">维护客户档案、等级、销售归属与跟进记录。</p>
      </div>
    </div>

    <div class="stats-row">
      <div class="stat-card blue">
        <div class="stat-header"><span class="stat-title">客户总数</span><div class="stat-icon">👥</div></div>
        <div class="stat-value">{{ stats.total }}</div>
        <div class="stat-footer">全部客户</div>
      </div>
      <div class="stat-card green">
        <div class="stat-header"><span class="stat-title">本月新增</span><div class="stat-icon">📈</div></div>
        <div class="stat-value">{{ stats.monthAdded }}</div>
        <div class="stat-footer">新客户</div>
      </div>
      <div class="stat-card orange">
        <div class="stat-header"><span class="stat-title">活跃客户</span><div class="stat-icon">🤝</div></div>
        <div class="stat-value">{{ stats.active }}</div>
        <div class="stat-footer">合作中</div>
      </div>
      <div class="stat-card purple">
        <div class="stat-header"><span class="stat-title">客户转化率</span><div class="stat-icon">🎯</div></div>
        <div class="stat-value">{{ stats.conversionRate }}%</div>
        <div class="stat-footer">本月数据</div>
      </div>
    </div>

    <div class="table-card">
      <div class="filter-bar">
        <div class="filter-left">
          <span class="filter-label">客户名称</span>
          <el-input
            v-model="filters.name"
            clearable
            placeholder="请输入客户名称"
            style="width:200px"
            @keyup.enter="handleSearch"
            @clear="handleSearch"
          />
          <span class="filter-label">客户等级</span>
          <el-select v-model="filters.level" clearable placeholder="请选择" style="width:140px" @change="handleSearch">
            <el-option v-for="opt in levels" :key="opt" :label="opt" :value="opt" />
          </el-select>
          <span class="filter-label">销售负责人</span>
          <el-input
            v-model="filters.owner"
            clearable
            placeholder="请输入销售负责人"
            style="width:200px"
            @keyup.enter="handleSearch"
            @clear="handleSearch"
          />
          <el-button type="primary" @click="handleSearch">查询</el-button>
        </div>
        <div class="filter-right">
          <el-button v-if="false" plain :icon="Download" @click="handleTemplate">模板下载</el-button>
          <el-button v-if="false" plain :icon="Upload" @click="openImport">导入</el-button>
          <el-button v-if="selected.length" type="danger" plain :icon="Delete" @click="batchDelete">批量删除 ({{ selected.length }})</el-button>
          <el-button plain :icon="Switch" :disabled="!selected.length" @click="openTransfer">一键转移</el-button>
          <el-button type="primary" :icon="Plus" @click="openAdd">添加</el-button>
        </div>
      </div>

      <el-table v-loading="loading" :data="rows" stripe style="width:100%" @selection-change="onSelectionChange">
        <el-table-column type="selection" width="46" />
        <el-table-column prop="name" label="客户名称" min-width="180" show-overflow-tooltip />
        <el-table-column prop="englishName" label="英文名称" min-width="200" show-overflow-tooltip />
        <el-table-column label="客户等级" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="levelTagType(row.level)" effect="light">{{ row.level }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="owner" label="销售负责人" width="120" align="center" />
        <el-table-column prop="lastVisitAt" label="最近拜访时间" width="150" align="center" />
        <el-table-column prop="lastDealAt" label="最近成交时间" width="150" align="center" />
        <el-table-column label="操作" width="140" align="center" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" :icon="View" @click="openDetail(row)" />
            <el-button link type="primary" :icon="Edit" @click="openEdit(row)" />
            <el-button link type="danger" :icon="Delete" @click="handleDelete(row)" />
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

    <el-dialog v-model="formVisible" :title="formMode === 'add' ? '新增客户' : '编辑客户'" width="600px" :close-on-click-modal="false" @closed="resetForm">
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="80px" label-position="right">
        <el-form-item label="客户名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入客户名称" maxlength="100" />
        </el-form-item>
        <el-form-item label="英文名称" prop="englishName">
          <el-input v-model="form.englishName" placeholder="请输入英文名称" maxlength="120" />
        </el-form-item>
        <el-form-item label="地址" prop="address">
          <el-input v-model="form.address" placeholder="请输入地址" maxlength="200" />
        </el-form-item>
        <el-form-item label="客户等级" prop="level">
          <el-select v-model="form.level" placeholder="请选择客户等级" style="width:100%">
            <el-option v-for="opt in levels" :key="opt" :label="opt" :value="opt" />
          </el-select>
        </el-form-item>
      </el-form>

      <div class="sub-table-block">
        <div class="sub-table-header">
          <span class="sub-table-title">销售列表</span>
          <el-button size="small" plain :icon="Plus" @click="addSales">添加</el-button>
        </div>
        <el-table :data="form.salesList" border size="small">
          <el-table-column label="是否负责人" width="90" align="center">
            <template #default="{ row }">
              <el-checkbox :model-value="row.isOwner" @change="(val: boolean) => setOwner(row, val)" />
            </template>
          </el-table-column>
          <el-table-column label="销售人员" min-width="160">
            <template #default="{ row }">
              <el-input v-model="row.name" placeholder="请输入销售名称" />
            </template>
          </el-table-column>
          <el-table-column label="职位" width="130">
            <template #default="{ row }">
              <el-input v-model="row.position" placeholder="职位" />
            </template>
          </el-table-column>
          <el-table-column label="操作" width="70" align="center">
            <template #default="{ $index }">
              <el-button link type="danger" @click="form.salesList.splice($index, 1)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <div class="sub-table-block">
        <div class="sub-table-header">
          <span class="sub-table-title">联系人列表</span>
          <el-button size="small" plain :icon="Plus" @click="addContact">添加</el-button>
        </div>
        <el-table :data="form.contactList" border size="small">
          <el-table-column label="联系人" min-width="140">
            <template #default="{ row }">
              <el-input v-model="row.name" placeholder="请输入联系人" />
            </template>
          </el-table-column>
          <el-table-column label="联系电话" min-width="150">
            <template #default="{ row }">
              <el-input v-model="row.phone" placeholder="请输入联系电话" />
            </template>
          </el-table-column>
          <el-table-column label="职务" width="130">
            <template #default="{ row }">
              <el-input v-model="row.title" placeholder="请输入职务" />
            </template>
          </el-table-column>
          <el-table-column label="操作" width="70" align="center">
            <template #default="{ $index }">
              <el-button link type="danger" @click="form.contactList.splice($index, 1)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" @click="submitForm">确定提交</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="detailVisible" title="客户详情" width="560px" :close-on-click-modal="false">
      <el-descriptions v-if="detail" :column="2" border>
        <el-descriptions-item label="客户名称">{{ detail.name }}</el-descriptions-item>
        <el-descriptions-item label="英文名称">{{ detail.englishName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="客户等级">{{ detail.level }}</el-descriptions-item>
        <el-descriptions-item label="销售负责人">{{ detail.owner }}</el-descriptions-item>
        <el-descriptions-item label="最近拜访时间">{{ detail.lastVisitAt || '-' }}</el-descriptions-item>
        <el-descriptions-item label="最近成交时间">{{ detail.lastDealAt || '-' }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ detail.remark || '-' }}</el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
        <el-button type="primary" @click="openEdit(detail!)">编辑</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="transferVisible" title="一键转移" width="420px" :close-on-click-modal="false">
      <p class="transfer-tip">将选中的 {{ selected.length }} 个客户转移给新的销售负责人：</p>
      <el-input v-model="transferOwner" placeholder="请输入新的销售负责人" maxlength="50" />
      <template #footer>
        <el-button @click="transferVisible = false">取消</el-button>
        <el-button type="primary" @click="submitTransfer">确认转移</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="importVisible" title="导入客户" width="560px" :close-on-click-modal="false">
      <el-alert title="导入说明" type="info" :closable="false" show-icon>
        <p>支持 JSON 格式批量导入，必填字段：客户名称(name)、客户等级(level)、销售负责人(owner)。</p>
      </el-alert>
      <el-input v-model="importText" type="textarea" :rows="8" placeholder='例如：[{"name":"示例客户","englishName":"Demo Co., Ltd.","level":"A级","owner":"张三"}]' style="margin-top:12px" />
      <template #footer>
        <el-button @click="importVisible = false">取消</el-button>
        <el-button type="primary" @click="submitImport">开始导入</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Plus, Download, Upload, Switch, View, Edit, Delete } from '@element-plus/icons-vue'
import {
  batchDeleteCustomers,
  createCustomer,
  deleteCustomer,
  importCustomers,
  listCustomers,
  transferCustomers,
  updateCustomer,
  type CustomerItem,
  type CustomerSales,
} from '@/api/crm/customer'

type SalesPerson = CustomerSales
type Customer = CustomerItem

const levels = ref<string[]>(['A级', 'B级', 'C级', 'D级'])

const loading = ref(false)
const rows = ref<Customer[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)
const selected = ref<Customer[]>([])

const filters = reactive({ name: '', level: '', owner: '' })

const stats = reactive({ total: 0, monthAdded: 0, active: 0, conversionRate: 0 })

async function loadData() {
  loading.value = true
  try {
    const res = await listCustomers({
      name: filters.name.trim() || undefined,
      level: filters.level || undefined,
      owner: filters.owner.trim() || undefined,
      page: page.value,
      pageSize: pageSize.value,
    })
    if (res.code !== 0) {
      ElMessage.error(res.message || '加载客户失败')
      return
    }
    rows.value = (res.data.rows || []).map((row) => ({
      ...row,
      salesList: row.salesList || [],
      contactList: row.contactList || [],
    }))
    total.value = res.data.total || 0
    if (res.data.levels?.length) {
      levels.value = res.data.levels
    }
    if (res.data.stats) {
      stats.total = res.data.stats.total ?? 0
      stats.monthAdded = res.data.stats.monthAdded ?? 0
      stats.active = res.data.stats.active ?? 0
      stats.conversionRate = res.data.stats.conversionRate ?? 0
    }
  } catch (error: any) {
    ElMessage.error(error?.message || '加载客户失败')
  } finally {
    loading.value = false
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

function onSelectionChange(selection: Customer[]) {
  selected.value = selection
}

function levelTagType(level: string) {
  switch (level) {
    case 'A级':
      return 'danger'
    case 'B级':
      return 'warning'
    case 'C级':
      return 'success'
    default:
      return 'info'
  }
}

const formVisible = ref(false)
const formMode = ref<'add' | 'edit'>('add')
const formRef = ref<FormInstance>()

function createEmptyForm(): Customer {
  return { id: 0, name: '', englishName: '', address: '', level: '', owner: '', lastVisitAt: '', lastDealAt: '', remark: '', salesList: [], contactList: [] }
}

const form = reactive<Customer>(createEmptyForm())

const formRules: FormRules = {
  name: [{ required: true, message: '请输入客户名称', trigger: 'blur' }],
  level: [{ required: true, message: '请选择客户等级', trigger: 'change' }],
}

function cloneCustomer(row: Customer): Customer {
  return {
    ...row,
    salesList: (row.salesList || []).map((s) => ({ ...s })),
    contactList: (row.contactList || []).map((c) => ({ ...c })),
  }
}

function resetForm() {
  Object.assign(form, createEmptyForm())
  formRef.value?.clearValidate()
}

function addSales() {
  form.salesList.push({ name: '', position: '', isOwner: form.salesList.length === 0 })
}

function addContact() {
  form.contactList.push({ name: '', phone: '', title: '' })
}

function setOwner(row: SalesPerson, val: boolean) {
  if (val) {
    form.salesList.forEach((s) => (s.isOwner = s === row))
  } else {
    row.isOwner = false
  }
}

function openAdd() {
  formMode.value = 'add'
  resetForm()
  form.salesList.push({ name: '', position: '', isOwner: true })
  form.contactList.push({ name: '', phone: '', title: '' })
  formVisible.value = true
}

function openEdit(row: Customer) {
  formMode.value = 'edit'
  resetForm()
  Object.assign(form, cloneCustomer(row))
  detailVisible.value = false
  formVisible.value = true
}

async function submitForm() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  const payload = cloneCustomer(form)
  try {
    if (formMode.value === 'add') {
      const res = await createCustomer(payload)
      if (res.code !== 0) {
        ElMessage.error(res.message || '保存失败')
        return
      }
      ElMessage.success('客户已添加')
    } else {
      const res = await updateCustomer(payload)
      if (res.code !== 0) {
        ElMessage.error(res.message || '保存失败')
        return
      }
      ElMessage.success('客户已更新')
    }
    formVisible.value = false
    loadData()
  } catch (error: any) {
    ElMessage.error(error?.message || '保存失败')
  }
}

const detailVisible = ref(false)
const detail = ref<Customer | null>(null)

function openDetail(row: Customer) {
  detail.value = cloneCustomer(row)
  detailVisible.value = true
}

async function handleDelete(row: Customer) {
  try {
    await ElMessageBox.confirm(`确认删除客户「${row.name}」吗？`, '删除确认', { type: 'warning' })
  } catch {
    return
  }
  try {
    const res = await deleteCustomer(row.id!)
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

const transferVisible = ref(false)
const transferOwner = ref('')

function openTransfer() {
  if (!selected.value.length) {
    ElMessage.warning('请先选择要转移的客户')
    return
  }
  transferOwner.value = ''
  transferVisible.value = true
}

async function submitTransfer() {
  if (!transferOwner.value.trim()) {
    ElMessage.warning('请输入新的销售负责人')
    return
  }
  try {
    const ids = selected.value.map((s) => s.id!).filter(Boolean)
    const res = await transferCustomers(ids, transferOwner.value.trim())
    if (res.code !== 0) {
      ElMessage.error(res.message || '转移失败')
      return
    }
    ElMessage.success(`已将 ${res.data ?? ids.length} 个客户转移给 ${transferOwner.value.trim()}`)
    transferVisible.value = false
    loadData()
  } catch (error: any) {
    ElMessage.error(error?.message || '转移失败')
  }
}

const importVisible = ref(false)
const importText = ref('')

function openImport() {
  importText.value = ''
  importVisible.value = true
}

async function submitImport() {
  if (!importText.value.trim()) {
    ElMessage.warning('请先输入要导入的客户数据')
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
  const items: Customer[] = list
    .filter((item) => item?.name)
    .map((item) => ({
      name: String(item.name),
      englishName: String(item.englishName || ''),
      address: String(item.address || ''),
      level: levels.value.includes(item.level) ? item.level : 'C级',
      owner: String(item.owner || ''),
      lastVisitAt: String(item.lastVisitAt || ''),
      lastDealAt: String(item.lastDealAt || ''),
      remark: item.remark,
      salesList: item.owner ? [{ name: String(item.owner), position: '', isOwner: true }] : [],
      contactList: [],
    }))
  if (!items.length) {
    ElMessage.warning('未解析到有效客户数据')
    return
  }
  try {
    const res = await importCustomers(items)
    if (res.code !== 0) {
      ElMessage.error(res.message || '导入失败')
      return
    }
    const { successCount, failedCount, failedNames } = res.data
    if (failedCount > 0) {
      ElMessageBox.alert(`成功 ${successCount} 条，失败 ${failedCount} 条。\n失败客户：\n${failedNames.join('\n')}`, '导入结果', { type: 'warning' })
    } else {
      ElMessage.success(`成功导入 ${successCount} 个客户`)
    }
    importVisible.value = false
    loadData()
  } catch (error: any) {
    ElMessage.error(error?.message || '导入失败')
  }
}

function handleTemplate() {
  const headers = ['客户名称', '英文名称', '客户等级', '销售负责人', '最近拜访时间', '最近成交时间']
  const sample = ['示例客户', 'Demo Co., Ltd.', 'A级', '张三', '2026-05-20', '2026-05-25']
  const blob = new Blob(['﻿' + headers.join(',') + '\n' + sample.join(',')], { type: 'text/csv;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = '客户导入模板.csv'
  link.click()
  URL.revokeObjectURL(url)
  ElMessage.success('模板已下载')
}

const batchDelete = async () => {
  if (!selected.value.length) return
  try {
    await ElMessageBox.confirm(`确认批量删除 ${selected.value.length} 个客户吗？`, '批量删除', { type: 'warning' })
  } catch {
    return
  }
  try {
    const ids = selected.value.map((s) => s.id!).filter(Boolean)
    const res = await batchDeleteCustomers(ids)
    if (res.code !== 0) {
      ElMessage.error(res.message || '批量删除失败')
      return
    }
    ElMessage.success(`已删除 ${res.data ?? ids.length} 个客户`)
    selected.value = []
    loadData()
  } catch (error: any) {
    ElMessage.error(error?.message || '批量删除失败')
  }
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
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
.filter-label { font-size: 14px; color: #606266; white-space: nowrap; }
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
.transfer-tip { margin: 0 0 12px; color: #606266; font-size: 14px; }

.sub-table-block { margin-bottom: 18px; }
.sub-table-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}
.sub-table-title {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}

@media (max-width: 768px) {
  .stats-row { grid-template-columns: 1fr; }
}
</style>
