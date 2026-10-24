<template>
  <section class="table-page">
    <div class="sub-page-header">
      <div>
        <h2>报价管理</h2>
        <p class="page-desc">维护产品报价信息，跟踪品牌与客户报价分布。</p>
      </div>
      <div class="header-actions">
        <el-button plain :icon="Refresh" @click="loadData">刷新</el-button>
        <el-button type="primary" :icon="Plus" @click="openAdd">新增报价</el-button>
      </div>
    </div>

    <div class="stats-row">
      <div class="stat-card blue">
        <div class="stat-header"><span class="stat-title">报价总数</span><div class="stat-icon">🧾</div></div>
        <div class="stat-value">{{ stats.total }}</div>
        <div class="stat-footer">全部报价单</div>
      </div>
      <div class="stat-card teal">
        <div class="stat-header"><span class="stat-title">客户数</span><div class="stat-icon">🏢</div></div>
        <div class="stat-value">{{ stats.customerCount }}个客户</div>
        <div class="stat-footer">报价覆盖</div>
      </div>
      <div class="stat-card purple">
        <div class="stat-header"><span class="stat-title">品牌数</span><div class="stat-icon">🔖</div></div>
        <div class="stat-value">{{ stats.brandCount }}个品牌</div>
        <div class="stat-footer">涉及品牌</div>
      </div>
      <div class="stat-card green">
        <div class="stat-header"><span class="stat-title">报价总额</span><div class="stat-icon">💰</div></div>
        <div class="stat-value">{{ formatAmount(stats.totalAmount) }}</div>
        <div class="stat-footer">累计报价金额</div>
      </div>
      <div class="stat-card orange">
        <div class="stat-header"><span class="stat-title">平均单价</span><div class="stat-icon">📊</div></div>
        <div class="stat-value">{{ formatAmount(stats.avgPrice) }}</div>
        <div class="stat-footer">单条均价</div>
      </div>
      <div class="stat-card blue">
        <div class="stat-header"><span class="stat-title">本月新增</span><div class="stat-icon">📈</div></div>
        <div class="stat-value">{{ stats.monthAdded }}</div>
        <div class="stat-footer">本月报价</div>
      </div>
    </div>

    <div v-if="brandDistribution.length" class="table-card brand-card">
      <div class="brand-header">
        <h3>品牌分布</h3>
        <span class="brand-hint">按报价条数排序</span>
      </div>
      <div class="brand-bars">
        <div v-for="item in brandDistribution" :key="item.brand" class="brand-bar-row">
          <span class="brand-name" :title="item.brand">{{ item.brand }}</span>
          <div class="brand-track">
            <div class="brand-fill" :style="{ width: barWidth(item.count) }"></div>
          </div>
          <span class="brand-count">{{ item.count }}</span>
        </div>
      </div>
    </div>

    <div class="table-card">
      <div class="filter-bar">
        <div class="filter-left">
          <el-input
            v-model="filters.keyword"
            clearable
            placeholder="搜索客户/品牌/型号/主题..."
            :prefix-icon="Search"
            style="width:240px"
            @keyup.enter="handleSearch"
            @clear="handleSearch"
          />
          <el-input
            v-model="filters.customerName"
            clearable
            placeholder="客户名称"
            style="width:170px"
            @keyup.enter="handleSearch"
            @clear="handleSearch"
          />
          <el-select v-model="filters.brand" clearable filterable placeholder="品牌筛选" style="width:160px" @change="handleSearch">
            <el-option v-for="opt in brandOptions" :key="opt" :label="opt" :value="opt" />
          </el-select>
          <el-input
            v-model="filters.model"
            clearable
            placeholder="型号"
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
        <el-table-column prop="customerName" label="客户名称" min-width="180" show-overflow-tooltip>
          <template #default="{ row }"><span class="quote-customer">{{ row.customerName }}</span></template>
        </el-table-column>
        <el-table-column prop="brand" label="品牌" min-width="130" show-overflow-tooltip>
          <template #default="{ row }">
            <el-tag v-if="row.brand" type="primary" effect="plain">{{ row.brand }}</el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="model" label="型号" min-width="140" show-overflow-tooltip>
          <template #default="{ row }">{{ row.model || '-' }}</template>
        </el-table-column>
        <el-table-column label="报价" width="150" align="right">
          <template #default="{ row }"><span class="quote-price">{{ formatAmount(row.price) }}</span></template>
        </el-table-column>
        <el-table-column prop="visitSubject" label="拜访主题" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">{{ row.visitSubject || '-' }}</template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="160" align="center">
          <template #default="{ row }">{{ row.createTime || '-' }}</template>
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

    <el-dialog v-model="formVisible" :title="formMode === 'add' ? '新增报价' : '编辑报价'" width="520px" :close-on-click-modal="false" @closed="resetForm">
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="92px" label-position="right">
        <el-form-item label="客户名称" prop="customerName">
          <el-input v-model="form.customerName" placeholder="请输入客户名称" maxlength="100" />
        </el-form-item>
        <el-form-item label="品牌" prop="brand">
          <el-select v-model="form.brand" clearable filterable allow-create placeholder="请选择或输入品牌" style="width:100%">
            <el-option v-for="opt in brandOptions" :key="opt" :label="opt" :value="opt" />
          </el-select>
        </el-form-item>
        <el-form-item label="型号" prop="model">
          <el-input v-model="form.model" placeholder="请输入型号" maxlength="100" />
        </el-form-item>
        <el-form-item label="报价金额" prop="price">
          <el-input-number v-model="form.price" :min="0" :precision="2" :step="100" controls-position="right" style="width:100%" />
        </el-form-item>
        <el-form-item label="拜访主题" prop="visitSubject">
          <el-input v-model="form.visitSubject" placeholder="请输入拜访主题" maxlength="200" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" @click="submitForm">确定提交</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="detailVisible" title="报价详情" width="520px" :close-on-click-modal="false">
      <el-descriptions v-if="detail" :column="1" border>
        <el-descriptions-item label="客户名称">{{ detail.customerName }}</el-descriptions-item>
        <el-descriptions-item label="品牌">{{ detail.brand || '-' }}</el-descriptions-item>
        <el-descriptions-item label="型号">{{ detail.model || '-' }}</el-descriptions-item>
        <el-descriptions-item label="报价金额">{{ formatAmount(detail.price) }}</el-descriptions-item>
        <el-descriptions-item label="拜访主题">{{ detail.visitSubject || '-' }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ detail.createTime || '-' }}</el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
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
  batchDeleteQuotes,
  createQuote,
  deleteQuote,
  listQuoteBrands,
  listQuotes,
  updateQuote,
  type BrandDistribution,
  type QuoteItem,
} from '@/api/crm/quote'

type Quote = QuoteItem

const loading = ref(false)
const rows = ref<Quote[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)
const selected = ref<Quote[]>([])
const brandOptions = ref<string[]>([])
const brandDistribution = ref<BrandDistribution[]>([])

const filters = reactive<{ keyword: string; customerName: string; brand: string; model: string }>({
  keyword: '',
  customerName: '',
  brand: '',
  model: '',
})

const stats = reactive({
  total: 0,
  customerCount: 0,
  brandCount: 0,
  totalAmount: 0,
  avgPrice: 0,
  monthAdded: 0,
})

const maxBrandCount = computed(() => {
  return brandDistribution.value.reduce((max, item) => Math.max(max, item.count), 0) || 1
})

function barWidth(count: number) {
  const ratio = (count / maxBrandCount.value) * 100
  return `${Math.max(ratio, 4)}%`
}

function formatAmount(value?: number | null) {
  if (value == null) return '--'
  return Number(value).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

async function loadData() {
  loading.value = true
  try {
    const res = await listQuotes({
      keyword: filters.keyword.trim() || undefined,
      customerName: filters.customerName.trim() || undefined,
      brand: filters.brand || undefined,
      model: filters.model.trim() || undefined,
      page: page.value,
      pageSize: pageSize.value,
    })
    if (res.code !== 0) {
      ElMessage.error(res.message || '加载报价失败')
      return
    }
    rows.value = res.data.rows || []
    total.value = res.data.total || 0
    if (res.data.stats) {
      stats.total = res.data.stats.total ?? 0
      stats.customerCount = res.data.stats.customerCount ?? 0
      stats.brandCount = res.data.stats.brandCount ?? 0
      stats.totalAmount = res.data.stats.totalAmount ?? 0
      stats.avgPrice = res.data.stats.avgPrice ?? 0
      stats.monthAdded = res.data.stats.monthAdded ?? 0
      brandDistribution.value = res.data.stats.brandDistribution || []
    }
  } catch (error: any) {
    ElMessage.error(error?.message || '加载报价失败')
  } finally {
    loading.value = false
  }
}

async function loadBrands() {
  try {
    const res = await listQuoteBrands()
    if (res.code === 0) {
      brandOptions.value = res.data || []
    }
  } catch {
    // 品牌下拉加载失败不阻塞主流程
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

function onSelectionChange(selection: Quote[]) {
  selected.value = selection
}

const formVisible = ref(false)
const formMode = ref<'add' | 'edit'>('add')
const formRef = ref<FormInstance>()

function createEmptyForm(): Quote {
  return { id: 0, customerName: '', brand: '', model: '', price: 0, visitSubject: '' }
}

const form = reactive<Quote>(createEmptyForm())

const formRules: FormRules = {
  customerName: [{ required: true, message: '请输入客户名称', trigger: 'blur' }],
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

function openEdit(row: Quote) {
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
  const payload: Quote = { ...form }
  try {
    if (formMode.value === 'add') {
      const res = await createQuote(payload)
      if (res.code !== 0) {
        ElMessage.error(res.message || '保存失败')
        return
      }
      ElMessage.success('报价已添加')
    } else {
      const res = await updateQuote(payload)
      if (res.code !== 0) {
        ElMessage.error(res.message || '保存失败')
        return
      }
      ElMessage.success('报价已更新')
    }
    formVisible.value = false
    loadData()
    loadBrands()
  } catch (error: any) {
    ElMessage.error(error?.message || '保存失败')
  }
}

const detailVisible = ref(false)
const detail = ref<Quote | null>(null)

function openDetail(row: Quote) {
  detail.value = { ...row }
  detailVisible.value = true
}

async function handleDelete(row: Quote) {
  try {
    await ElMessageBox.confirm(`确认删除「${row.customerName} - ${row.model || row.brand}」的报价吗？`, '删除确认', { type: 'warning' })
  } catch {
    return
  }
  try {
    const res = await deleteQuote(row.id!)
    if (res.code !== 0) {
      ElMessage.error(res.message || '删除失败')
      return
    }
    ElMessage.success('已删除')
    if (rows.value.length === 1 && page.value > 1) page.value -= 1
    loadData()
    loadBrands()
  } catch (error: any) {
    ElMessage.error(error?.message || '删除失败')
  }
}

async function batchDelete() {
  if (!selected.value.length) return
  try {
    await ElMessageBox.confirm(`确认批量删除 ${selected.value.length} 条报价吗？`, '批量删除', { type: 'warning' })
  } catch {
    return
  }
  try {
    const ids = selected.value.map((s) => s.id!).filter(Boolean)
    const res = await batchDeleteQuotes(ids)
    if (res.code !== 0) {
      ElMessage.error(res.message || '批量删除失败')
      return
    }
    ElMessage.success(`已删除 ${ids.length} 条报价`)
    selected.value = []
    loadData()
    loadBrands()
  } catch (error: any) {
    ElMessage.error(error?.message || '批量删除失败')
  }
}

onMounted(() => {
  loadData()
  loadBrands()
})
</script>

<style scoped>
.sub-page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 20px;
  flex-wrap: wrap;
}
.header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}
.stats-row {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
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
.stat-card.teal::before { background: linear-gradient(90deg, #13c2c2, #5cdbd3); }
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
.stat-card.teal .stat-icon { background: #e6fffb; }
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
.brand-card { margin-bottom: 24px; }
.brand-header {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 16px;
}
.brand-header h3 { margin: 0; font-size: 15px; color: #303133; }
.brand-hint { font-size: 12px; color: #909399; }
.brand-bars {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
  gap: 12px 32px;
}
.brand-bar-row {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}
.brand-name {
  width: 96px;
  flex-shrink: 0;
  font-size: 13px;
  color: #606266;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.brand-track {
  flex: 1;
  height: 10px;
  min-width: 0;
  background: #f0f2f5;
  border-radius: 6px;
  overflow: hidden;
}
.brand-fill {
  height: 100%;
  border-radius: 6px;
  background: linear-gradient(90deg, #667eea, #764ba2);
  transition: width .3s ease;
}
.brand-count {
  width: 40px;
  flex-shrink: 0;
  text-align: right;
  font-size: 13px;
  font-weight: 600;
  color: #303133;
}
.quote-customer { font-weight: 600; color: #303133; }
.quote-price { font-weight: 600; color: #e6a23c; }
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

@media (max-width: 768px) {
  .stats-row { grid-template-columns: 1fr; }
  .brand-bars { grid-template-columns: 1fr; }
}
</style>
