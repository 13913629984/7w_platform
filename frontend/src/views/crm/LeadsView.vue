<template>
  <section class="table-page">
    <AppPageHeader title="线索管理" description="沉淀销售线索，跟进推进并转化为客户。">
      <template #actions>
        <el-button plain :icon="Refresh" @click="loadData">刷新</el-button>
        <el-button type="primary" :icon="Plus" @click="openAdd">新增线索</el-button>
      </template>
    </AppPageHeader>

    <AppStatGrid>
      <AppStatCard label="线索总数" accent="blue" icon="📋" :value="stats.total" footer="全部线索" />
      <AppStatCard label="待跟进" accent="orange" icon="⏳" :value="stats.waiting" footer="尚未跟进" />
      <AppStatCard label="跟进中" accent="purple" icon="🔄" :value="stats.following" footer="正在推进" />
      <AppStatCard label="已转化" accent="green" icon="✅" :value="stats.converted" footer="已成客户" />
      <AppStatCard label="本月新增" accent="teal" icon="📈" :value="stats.monthAdded" footer="本月线索" />
    </AppStatGrid>

    <div class="table-card">
      <div class="filter-bar">
        <div class="filter-left">
          <el-input
            v-model="filters.keyword"
            clearable
            placeholder="搜索线索名称/负责人/备注..."
            :prefix-icon="Search"
            style="width:240px"
            @keyup.enter="handleSearch"
            @clear="handleSearch"
          />
          <el-select v-model="filters.status" clearable placeholder="状态筛选" style="width:140px" @change="handleSearch">
            <el-option v-for="opt in STATUS_OPTIONS" :key="opt" :label="opt" :value="opt" />
          </el-select>
          <el-select v-model="filters.source" clearable filterable allow-create placeholder="来源筛选" style="width:150px" @change="handleSearch">
            <el-option v-for="opt in sourceOptions" :key="opt" :label="opt" :value="opt" />
          </el-select>
          <el-input
            v-model="filters.owner"
            clearable
            placeholder="负责人"
            style="width:150px"
            @keyup.enter="handleSearch"
            @clear="handleSearch"
          />
          <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
        </div>
        <div class="filter-right">
          <el-button v-if="selected.length" type="danger" plain :icon="Delete" @click="batchDelete">批量删除 ({{ selected.length }})</el-button>
        </div>
      </div>

      <el-table v-loading="loading" :data="rows" stripe style="width:100%" @selection-change="onSelectionChange">
        <el-table-column type="selection" width="46" />
        <el-table-column prop="name" label="线索名称" min-width="180" show-overflow-tooltip>
          <template #default="{ row }"><span class="lead-name">{{ row.name }}</span></template>
        </el-table-column>
        <el-table-column prop="source" label="线索来源" min-width="110">
          <template #default="{ row }">{{ row.source || '-' }}</template>
        </el-table-column>
        <el-table-column prop="owner" label="负责人" min-width="110">
          <template #default="{ row }">{{ row.owner || '-' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" effect="light">{{ row.status || '待跟进' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="followAt" label="跟进日期" width="130" align="center">
          <template #default="{ row }">{{ row.followAt || '-' }}</template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">{{ row.remark || '-' }}</template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="160" align="center">
          <template #default="{ row }">{{ row.createTime || '-' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="230" align="center" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">详情</el-button>
            <el-button link type="primary" @click="openEdit(row)">修改</el-button>
            <el-button v-if="row.status !== '已转化'" link type="success" @click="handleConvert(row)">转化为客户</el-button>
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

    <el-dialog v-model="formVisible" :title="formMode === 'add' ? '新增线索' : '编辑线索'" width="520px" :close-on-click-modal="false" @closed="resetForm">
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="92px" label-position="right">
        <el-form-item label="线索名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入线索名称" maxlength="100" />
        </el-form-item>
        <el-form-item label="线索来源" prop="source">
          <el-select v-model="form.source" clearable filterable allow-create placeholder="请选择或输入来源" style="width:100%">
            <el-option v-for="opt in sourceOptions" :key="opt" :label="opt" :value="opt" />
          </el-select>
        </el-form-item>
        <el-form-item label="负责人" prop="owner">
          <el-input v-model="form.owner" placeholder="请输入负责人" maxlength="50" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-select v-model="form.status" placeholder="请选择状态" style="width:100%">
            <el-option v-for="opt in STATUS_OPTIONS" :key="opt" :label="opt" :value="opt" />
          </el-select>
        </el-form-item>
        <el-form-item label="跟进日期" prop="followAt">
          <el-date-picker
            v-model="form.followAt"
            type="date"
            placeholder="请选择跟进日期"
            value-format="YYYY-MM-DD"
            style="width:100%"
          />
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

    <el-dialog v-model="detailVisible" title="线索详情" width="520px" :close-on-click-modal="false">
      <el-descriptions v-if="detail" :column="1" border>
        <el-descriptions-item label="线索名称">{{ detail.name }}</el-descriptions-item>
        <el-descriptions-item label="线索来源">{{ detail.source || '-' }}</el-descriptions-item>
        <el-descriptions-item label="负责人">{{ detail.owner || '-' }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ detail.status || '待跟进' }}</el-descriptions-item>
        <el-descriptions-item label="跟进日期">{{ detail.followAt || '-' }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ detail.createTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="更新时间">{{ detail.updateTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="备注">{{ detail.remark || '-' }}</el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
        <el-button v-if="detail && detail.status !== '已转化'" type="success" @click="handleConvert(detail)">转化为客户</el-button>
        <el-button type="primary" @click="openEdit(detail!)">编辑</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Plus, Delete, Search, Refresh } from '@element-plus/icons-vue'
import {
  batchDeleteLeads,
  convertLead,
  createLead,
  deleteLead,
  listLeads,
  updateLead,
  type LeadItem,
} from '@/api/crm/lead'

type Lead = LeadItem

const STATUS_OPTIONS = ['待跟进', '跟进中', '已转化', '已失效']
const SOURCE_OPTIONS = ['官网', '电话', '展会', '介绍', '广告', '其他']

const loading = ref(false)
const rows = ref<Lead[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)
const selected = ref<Lead[]>([])

const filters = reactive<{ keyword: string; status: string; source: string; owner: string }>({
  keyword: '',
  status: '',
  source: '',
  owner: '',
})

const stats = reactive({ total: 0, waiting: 0, following: 0, converted: 0, monthAdded: 0 })

const sourceOptions = computed(() => {
  const set = new Set<string>(SOURCE_OPTIONS)
  rows.value.forEach((r) => {
    if (r.source) set.add(r.source)
  })
  return Array.from(set)
})

function statusTagType(status?: string) {
  switch (status) {
    case '待跟进':
      return 'warning'
    case '跟进中':
      return 'primary'
    case '已转化':
      return 'success'
    case '已失效':
      return 'info'
    default:
      return 'info'
  }
}

async function loadData() {
  loading.value = true
  try {
    const res = await listLeads({
      keyword: filters.keyword.trim() || undefined,
      status: filters.status || undefined,
      source: filters.source || undefined,
      owner: filters.owner.trim() || undefined,
      page: page.value,
      pageSize: pageSize.value,
    })
    if (res.code !== 0) {
      ElMessage.error(res.message || '加载线索失败')
      return
    }
    rows.value = res.data.rows || []
    total.value = res.data.total || 0
    if (res.data.stats) {
      stats.total = res.data.stats.total ?? 0
      stats.waiting = res.data.stats.waiting ?? 0
      stats.following = res.data.stats.following ?? 0
      stats.converted = res.data.stats.converted ?? 0
      stats.monthAdded = res.data.stats.monthAdded ?? 0
    }
  } catch (error: any) {
    ElMessage.error(error?.message || '加载线索失败')
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

function onSelectionChange(selection: Lead[]) {
  selected.value = selection
}

const formVisible = ref(false)
const formMode = ref<'add' | 'edit'>('add')
const formRef = ref<FormInstance>()

function createEmptyForm(): Lead {
  return { id: 0, name: '', source: '', owner: '', status: '待跟进', followAt: '', remark: '' }
}

const form = reactive<Lead>(createEmptyForm())

const formRules: FormRules = {
  name: [{ required: true, message: '请输入线索名称', trigger: 'blur' }],
}

function resetForm() {
  Object.assign(form, createEmptyForm())
  formRef.value?.clearValidate()
}

function openAdd() {
  formMode.value = 'add'
  resetForm()
  formVisible.value = true
}

function openEdit(row: Lead) {
  formMode.value = 'edit'
  resetForm()
  Object.assign(form, createEmptyForm(), row)
  detailVisible.value = false
  formVisible.value = true
}

async function submitForm() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  const payload: Lead = { ...form }
  try {
    if (formMode.value === 'add') {
      const res = await createLead(payload)
      if (res.code !== 0) {
        ElMessage.error(res.message || '保存失败')
        return
      }
      ElMessage.success('线索已添加')
    } else {
      const res = await updateLead(payload)
      if (res.code !== 0) {
        ElMessage.error(res.message || '保存失败')
        return
      }
      ElMessage.success('线索已更新')
    }
    formVisible.value = false
    loadData()
  } catch (error: any) {
    ElMessage.error(error?.message || '保存失败')
  }
}

const detailVisible = ref(false)
const detail = ref<Lead | null>(null)

function openDetail(row: Lead) {
  detail.value = { ...row }
  detailVisible.value = true
}

async function handleConvert(row: Lead) {
  try {
    await ElMessageBox.confirm(`确认将线索「${row.name}」转化为客户吗？`, '转化确认', { type: 'info' })
  } catch {
    return
  }
  try {
    const res = await convertLead({ id: row.id! })
    if (res.code !== 0) {
      ElMessage.error(res.message || '转化失败')
      return
    }
    ElMessage.success('已生成客户')
    detailVisible.value = false
    loadData()
  } catch (error: any) {
    ElMessage.error(error?.message || '转化失败')
  }
}

async function handleDelete(row: Lead) {
  try {
    await ElMessageBox.confirm(`确认删除线索「${row.name}」吗？`, '删除确认', { type: 'warning' })
  } catch {
    return
  }
  try {
    const res = await deleteLead(row.id!)
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

async function batchDelete() {
  if (!selected.value.length) return
  try {
    await ElMessageBox.confirm(`确认批量删除 ${selected.value.length} 条线索吗？`, '批量删除', { type: 'warning' })
  } catch {
    return
  }
  try {
    const ids = selected.value.map((s) => s.id!).filter(Boolean)
    const res = await batchDeleteLeads(ids)
    if (res.code !== 0) {
      ElMessage.error(res.message || '批量删除失败')
      return
    }
    ElMessage.success(`已删除 ${ids.length} 条线索`)
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

.table-card {
  background: #fff;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, .04);
  min-width: 0;
}
.table-card .el-table { width: 100%; }
.lead-name { font-weight: 600; color: var(--pw-text-primary); }
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

</style>
