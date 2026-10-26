<template>
  <section class="table-page">
    <div class="sub-page-header">
      <div>
        <h2>应收款管理</h2>
        <p class="page-desc">管理客户应收款、开票、回款核销与账龄。</p>
      </div>
      <el-space>
        <el-button size="small" plain @click="handleExport">导出</el-button>
        <el-button size="small" plain @click="handleRefresh">刷新</el-button>
        <el-button size="small" type="primary" @click="handleGenerate">+ 生成应收款</el-button>
      </el-space>
    </div>

    <div class="stats-row">
      <div v-for="item in statCards" :key="item.label" class="stat-card" :class="item.color">
        <div class="stat-header">
          <span class="stat-title">{{ item.label }}</span>
          <div class="stat-icon">{{ item.icon }}</div>
        </div>
        <div class="stat-value">{{ item.value }}</div>
        <div class="stat-footer"><span :class="item.trendType">{{ item.trend }}</span></div>
      </div>
    </div>

    <div class="table-card">
      <el-tabs v-model="activeTab">
        <el-tab-pane label="应收款列表" name="list" />
        <el-tab-pane label="回款记录" name="records" />
      </el-tabs>

      <template v-if="activeTab === 'list'">
        <div class="filter-bar">
          <div class="filter-left">
            <el-input v-model="keyword" style="width: 240px" clearable prefix-icon="Search" placeholder="搜索客户/单据号..." />
            <el-select v-model="statusFilter" style="width: 140px" placeholder="状态筛选" clearable>
              <el-option v-for="s in statusOptions" :key="s" :label="s" :value="s" />
            </el-select>
            <el-select v-model="customerFilter" style="width: 160px" placeholder="客户筛选" clearable>
              <el-option v-for="c in customerOptions" :key="c" :label="c" :value="c" />
            </el-select>
          </div>
        </div>

        <el-table :data="receivables" stripe style="width: 100%">
          <el-table-column prop="code" label="单据编号" min-width="150" />
          <el-table-column prop="customer" label="客户名称" min-width="160" show-overflow-tooltip />
          <el-table-column label="关联销售订单" min-width="140">
            <template #default="{ row }"><el-button link type="primary">{{ row.salesOrder }}</el-button></template>
          </el-table-column>
          <el-table-column label="应收金额" width="120" align="right">
            <template #default="{ row }"><span class="amount">{{ formatMoney(row.amount) }}</span></template>
          </el-table-column>
          <el-table-column label="已核销" width="120" align="right">
            <template #default="{ row }">{{ formatMoney(row.writtenOff) }}</template>
          </el-table-column>
          <el-table-column label="待核销" width="120" align="right">
            <template #default="{ row }"><span class="danger">{{ formatMoney(row.pending) }}</span></template>
          </el-table-column>
          <el-table-column label="开票状态" width="100" align="center">
            <template #default="{ row }">
              <el-tag :type="row.invoiced ? 'success' : 'warning'" effect="light">{{ row.invoiced ? '已开票' : '未开票' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="dueDate" label="到期日" width="120" />
          <el-table-column label="状态" width="100" align="center">
            <template #default="{ row }">
              <el-tag :type="statusTagType(row.status)" effect="light">{{ row.status }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" min-width="160" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary">详情</el-button>
              <el-button v-if="row.pending > 0" link type="primary" @click="handleWriteOff(row)">回款核销</el-button>
              <el-button v-if="!row.invoiced" link type="primary" @click="handleInvoice(row)">开票</el-button>
            </template>
          </el-table-column>
        </el-table>
      </template>

      <template v-else>
        <div class="filter-bar">
          <div class="filter-left">
            <el-input v-model="recordKeyword" style="width: 240px" clearable prefix-icon="Search" placeholder="搜索客户/单据号..." />
          </div>
        </div>

        <el-table :data="records" stripe style="width: 100%">
          <el-table-column prop="code" label="回款单号" min-width="150" />
          <el-table-column prop="customer" label="客户名称" min-width="160" show-overflow-tooltip />
          <el-table-column prop="receivableCode" label="关联应收单" min-width="150" />
          <el-table-column label="回款金额" width="120" align="right">
            <template #default="{ row }"><span class="amount">{{ formatMoney(row.amount) }}</span></template>
          </el-table-column>
          <el-table-column prop="method" label="回款方式" width="110" align="center" />
          <el-table-column prop="date" label="回款日期" width="120" />
          <el-table-column prop="operator" label="经办人" width="110" />
          <el-table-column label="操作" width="90" align="center" fixed="right">
            <template #default><el-button link type="primary">详情</el-button></template>
          </el-table-column>
        </el-table>
      </template>

      <div class="pagination-bar">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          background
          layout="total, sizes, prev, pager, next, jumper"
          :page-sizes="[10, 20, 50, 100]"
          :total="total"
          @current-change="loadList"
          @size-change="handleSizeChange"
        />
      </div>
    </div>

    <el-dialog v-model="createVisible" title="生成应收款" width="560px" :close-on-click-modal="false">
      <el-form ref="createFormRef" :model="createForm" :rules="createRules" label-width="96px">
        <el-form-item label="客户名称" prop="customer">
          <el-input v-model="createForm.customer" maxlength="160" placeholder="请输入客户名称" />
        </el-form-item>
        <el-form-item label="销售订单">
          <el-input v-model="createForm.salesOrder" maxlength="80" placeholder="可填订单号，便于与 CRM 对账" />
        </el-form-item>
        <el-form-item label="合同号">
          <el-input v-model="createForm.contract" maxlength="80" />
        </el-form-item>
        <el-form-item label="应收金额" prop="amount">
          <el-input-number v-model="createForm.amount" :min="0" :step="1000" :precision="2" controls-position="right" style="width: 100%" />
        </el-form-item>
        <el-form-item label="到期日">
          <el-date-picker v-model="createForm.dueDate" type="date" value-format="YYYY-MM-DD" placeholder="选择到期日" style="width: 100%" />
        </el-form-item>
        <el-form-item label="已开票">
          <el-switch v-model="createForm.invoiced" />
        </el-form-item>
        <div class="form-tip">提示：销售订单发货完成后会由系统自动生成应收款，此处用于补录或手工建单。</div>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="createSubmitting" @click="submitCreate">确定生成</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { downloadCsvFromRows } from '@/utils/export'
import {
  listReceivables,
  listReceiptRecords,
  receivableStats,
  writeOffReceivable,
  invoiceReceivable,
  createReceivable,
  type Receivable,
  type ReceiptRecord,
} from '@/api/fin/receivable'
import { ElMessageBox } from 'element-plus'

const activeTab = ref<'list' | 'records'>('list')
const keyword = ref('')
const statusFilter = ref('')
const customerFilter = ref('')
const recordKeyword = ref('')

const receivables = ref<Receivable[]>([])
const records = ref<ReceiptRecord[]>([])
const stats = ref({ total: 0, writtenOff: 0, pending: 0, monthReceipt: 0, count: 0 })

const statCards = computed(() => [
  { label: '应收款总额', value: formatMoney(stats.value.total), trend: '全部应收款', trendType: '', color: 'blue', icon: '📥' },
  { label: '已核销', value: formatMoney(stats.value.writtenOff), trend: '已回款金额', trendType: '', color: 'green', icon: '✅' },
  { label: '待核销', value: formatMoney(stats.value.pending), trend: '待回款金额', trendType: '', color: 'orange', icon: '⏳' },
  { label: '本月回款', value: formatMoney(stats.value.monthReceipt), trend: '本月累计回款', trendType: '', color: 'purple', icon: '💰' },
])

const statusOptions = ['待核销', '部分核销', '已核销', '逾期']
const customerOptions = computed(() => Array.from(new Set(receivables.value.map((r) => r.customer))))

const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

async function loadList() {
  try {
    if (activeTab.value === 'list') {
      const res = await listReceivables({
        keyword: keyword.value || undefined,
        status: statusFilter.value || undefined,
        customer: customerFilter.value || undefined,
        page: currentPage.value,
        pageSize: pageSize.value,
      })
      receivables.value = res.rows
      total.value = res.total
    } else {
      const res = await listReceiptRecords({
        keyword: recordKeyword.value || undefined,
        page: currentPage.value,
        pageSize: pageSize.value,
      })
      records.value = res.rows
      total.value = res.total
    }
  } catch (e: any) {
    ElMessage.error(e.message || '加载失败')
  }
}

function handleSearch() {
  currentPage.value = 1
  loadList()
}

function handleSizeChange() {
  currentPage.value = 1
  loadList()
}

// 切换标签页或筛选条件变化时回到第 1 页并重新请求
watch(activeTab, () => {
  currentPage.value = 1
  total.value = 0
  loadList()
})
watch([keyword, statusFilter, customerFilter, recordKeyword], handleSearch)

async function loadStats() {
  try {
    stats.value = await receivableStats()
  } catch (e: any) {
    ElMessage.error(e.message || '加载失败')
  }
}

async function loadAll() {
  await Promise.all([loadList(), loadStats()])
}

onMounted(loadAll)

function formatMoney(value: number) {
  return `¥${Number(value || 0).toLocaleString()}`
}

function statusTagType(status: string) {
  if (status === '已核销') return 'success'
  if (status === '逾期') return 'danger'
  if (status === '部分核销') return 'warning'
  if (status === '待核销') return 'primary'
  return 'info'
}

function handleExport() {
  if (activeTab.value === 'list') {
    if (!receivables.value.length) {
      ElMessage.warning('暂无可导出的应收款数据')
      return
    }
    downloadCsvFromRows('应收款列表', [
      { label: '单据号', value: (r) => r.code },
      { label: '客户', value: (r) => r.customer },
      { label: '销售订单', value: (r) => r.salesOrder },
      { label: '合同号', value: (r) => r.contract || '' },
      { label: '应收金额', value: (r) => Number(r.amount || 0).toFixed(2) },
      { label: '已核销', value: (r) => Number(r.writtenOff || 0).toFixed(2) },
      { label: '待核销', value: (r) => Number(r.pending || 0).toFixed(2) },
      { label: '开票', value: (r) => (r.invoiced ? '已开票' : '未开票') },
      { label: '到期日', value: (r) => r.dueDate },
      { label: '状态', value: (r) => r.status },
    ], receivables.value)
  } else {
    if (!records.value.length) {
      ElMessage.warning('暂无可导出的回款记录')
      return
    }
    downloadCsvFromRows('回款记录', [
      { label: '回款单号', value: (r) => r.code },
      { label: '客户', value: (r) => r.customer },
      { label: '关联应收', value: (r) => r.receivableCode },
      { label: '回款金额', value: (r) => Number(r.amount || 0).toFixed(2) },
      { label: '回款方式', value: (r) => r.method },
      { label: '回款日期', value: (r) => r.date },
      { label: '操作人', value: (r) => r.operator },
    ], records.value)
  }
  ElMessage.success('已导出当前页数据')
}

function handleRefresh() {
  loadAll()
  ElMessage.success('已刷新')
}

/* ---------------- 生成应收款（手工建单） ---------------- */
const createVisible = ref(false)
const createFormRef = ref<FormInstance>()
const createSubmitting = ref(false)
const createForm = reactive({ customer: '', salesOrder: '', contract: '', amount: 0, dueDate: '', invoiced: false })
const createRules: FormRules = {
  customer: [{ required: true, message: '请填写客户名称', trigger: 'blur' }],
  amount: [{ required: true, message: '请填写应收金额', trigger: 'blur' }],
}

function handleGenerate() {
  Object.assign(createForm, { customer: '', salesOrder: '', contract: '', amount: 0, dueDate: '', invoiced: false })
  createVisible.value = true
}

async function submitCreate() {
  if (!createFormRef.value) return
  const valid = await createFormRef.value.validate().catch(() => false)
  if (!valid) return
  if (!(Number(createForm.amount) > 0)) {
    ElMessage.warning('应收金额必须大于 0')
    return
  }
  createSubmitting.value = true
  try {
    await createReceivable({
      customer: createForm.customer.trim(),
      salesOrder: createForm.salesOrder.trim(),
      contract: createForm.contract.trim(),
      amount: Number(createForm.amount),
      invoiced: createForm.invoiced,
      dueDate: createForm.dueDate,
    })
    ElMessage.success('应收款已生成')
    createVisible.value = false
    await loadAll()
  } catch (e: any) {
    ElMessage.error(e.message || '生成失败')
  } finally {
    createSubmitting.value = false
  }
}

async function handleWriteOff(row: Receivable) {
  try {
    const { value } = await ElMessageBox.prompt(`请输入回款金额（待核销 ${formatMoney(row.pending)}）`, '回款核销', {
      inputPattern: /^\d+(\.\d{1,2})?$/,
      inputErrorMessage: '请输入有效金额',
    })
    await writeOffReceivable({ id: row.id!, amount: Number(value) })
    ElMessage.success('回款核销成功')
    loadAll()
  } catch (e: any) {
    if (e !== 'cancel') ElMessage.error(e.message || '操作失败')
  }
}

async function handleInvoice(row: Receivable) {
  try {
    await invoiceReceivable(row.id!)
    ElMessage.success('开票成功')
    loadAll()
  } catch (e: any) {
    ElMessage.error(e.message || '操作失败')
  }
}
</script>

<style scoped>
.amount { color: #e6a23c; }
.pagination-bar { display: flex; justify-content: flex-end; margin-top: 16px; }
.form-tip { margin-left: 96px; font-size: 12px; color: var(--el-text-color-secondary); line-height: 1.6; }
</style>
