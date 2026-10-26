<template>
  <section class="table-page">
    <div class="sub-page-header">
      <div>
        <h2>应付款管理</h2>
        <p class="page-desc">管理供应商应付款、发票匹配、付款与账期。</p>
      </div>
      <el-space>
        <el-button size="small" plain @click="handleAutoGenerate">⚙ 自动生成AP</el-button>
        <el-button size="small" plain @click="handleExport">导出</el-button>
        <el-button size="small" plain @click="handleRefresh">刷新</el-button>
        <el-button size="small" type="primary" @click="handleCreate">+ 新增应付款</el-button>
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

    <div class="card pending-card">
      <div class="card-header">
        <div class="card-title">📦 待生成应付款的入库单</div>
        <el-button size="small" type="primary" plain @click="handleBatchGenerate">批量生成AP</el-button>
      </div>
      <el-table :data="pendingReceipts" stripe style="width: 100%" empty-text="暂无待生成入库单">
        <el-table-column prop="code" label="入库单号" min-width="150" />
        <el-table-column label="关联采购单" min-width="140">
          <template #default="{ row }"><el-button link type="primary">{{ row.purchaseOrder }}</el-button></template>
        </el-table-column>
        <el-table-column prop="supplier" label="供应商" min-width="140" show-overflow-tooltip />
        <el-table-column label="入库金额" width="120" align="right">
          <template #default="{ row }">{{ formatMoney(row.amount) }}</template>
        </el-table-column>
        <el-table-column label="预计税额" width="120" align="right">
          <template #default="{ row }">{{ formatMoney(row.tax) }}</template>
        </el-table-column>
        <el-table-column label="应付金额" width="120" align="right">
          <template #default="{ row }"><span class="danger">{{ formatMoney(row.payable) }}</span></template>
        </el-table-column>
        <el-table-column label="操作" width="120" align="center">
          <template #default="{ row }"><el-button size="small" type="primary" @click="handleGenerateAp(row)">发票匹配</el-button></template>
        </el-table-column>
      </el-table>
    </div>

    <div class="table-card">
      <div class="filter-bar">
        <div class="filter-left">
          <el-input v-model="keyword" style="width: 240px" clearable prefix-icon="Search" placeholder="搜索单据号/供应商..." />
          <el-select v-model="statusFilter" style="width: 140px" placeholder="状态筛选" clearable>
            <el-option v-for="s in statusOptions" :key="s" :label="s" :value="s" />
          </el-select>
        </div>
      </div>

      <el-table :data="payables" stripe style="width: 100%">
        <el-table-column prop="code" label="AP单据号" min-width="150" />
        <el-table-column label="关联入库单" min-width="140">
          <template #default="{ row }"><el-button link type="primary">{{ row.receiptCode }}</el-button></template>
        </el-table-column>
        <el-table-column prop="supplier" label="供应商" min-width="140" show-overflow-tooltip />
        <el-table-column label="采购金额" width="120" align="right">
          <template #default="{ row }">{{ formatMoney(row.purchaseAmount) }}</template>
        </el-table-column>
        <el-table-column label="税额" width="110" align="right">
          <template #default="{ row }">{{ formatMoney(row.tax) }}</template>
        </el-table-column>
        <el-table-column label="应付金额" width="120" align="right">
          <template #default="{ row }"><span class="danger">{{ formatMoney(row.payable) }}</span></template>
        </el-table-column>
        <el-table-column label="发票状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.matched ? 'success' : 'warning'" effect="light">{{ row.matched ? '已匹配' : '待匹配' }}</el-tag>
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
            <el-button v-if="row.status === '待付款' && row.matched" link type="primary">创建付款</el-button>
            <el-button v-if="!row.matched" link type="primary" @click="handleMatch(row)">上传发票</el-button>
          </template>
        </el-table-column>
      </el-table>

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

    <el-dialog v-model="createVisible" title="新增应付款" width="580px" :close-on-click-modal="false">
      <el-form ref="createFormRef" :model="createForm" :rules="createRules" label-width="104px">
        <el-form-item label="供应商" prop="supplier">
          <el-input v-model="createForm.supplier" maxlength="160" placeholder="请输入供应商名称" />
        </el-form-item>
        <el-form-item label="采购订单号">
          <el-input v-model="createForm.purchaseOrder" maxlength="80" />
        </el-form-item>
        <el-form-item label="入库单号">
          <el-input v-model="createForm.receiptCode" maxlength="80" />
        </el-form-item>
        <el-form-item label="采购金额" prop="purchaseAmount">
          <el-input-number v-model="createForm.purchaseAmount" :min="0" :step="1000" :precision="2" controls-position="right" style="width: 100%" />
        </el-form-item>
        <el-form-item label="税额">
          <el-input-number v-model="createForm.tax" :min="0" :step="100" :precision="2" controls-position="right" style="width: 100%" />
        </el-form-item>
        <el-form-item label="到期日">
          <el-date-picker v-model="createForm.dueDate" type="date" value-format="YYYY-MM-DD" placeholder="选择到期日" style="width: 100%" />
        </el-form-item>
        <div class="form-tip">应付金额 = 采购金额 + 税额，由后端自动计算。</div>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="createSubmitting" @click="submitCreate">确定创建</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { downloadCsvFromRows } from '@/utils/export'
import {
  listPayables,
  listPendingReceipts,
  payableStats,
  matchPayable,
  createPayable,
  type Payable,
  type PendingReceipt,
} from '@/api/fin/payable'

const keyword = ref('')
const statusFilter = ref('')

const payables = ref<Payable[]>([])
const pendingReceipts = ref<PendingReceipt[]>([])
const stats = ref({ total: 0, count: 0, pendingMatch: 0, matched: 0, overdue: 0 })

const statCards = computed(() => [
  { label: '应付账款总额', value: formatMoney(stats.value.total), trend: `${stats.value.count} 笔单据`, trendType: '', color: 'blue', icon: '📤' },
  { label: '待匹配发票', value: String(stats.value.pendingMatch), trend: '需上传发票', trendType: '', color: 'orange', icon: '🧾' },
  { label: '已匹配发票', value: String(stats.value.matched), trend: '可进行付款', trendType: '', color: 'green', icon: '✅' },
  { label: '逾期应付', value: String(stats.value.overdue), trend: '需紧急处理', trendType: 'down', color: 'red', icon: '⚠️' },
])

const statusOptions = ['待匹配', '待付款', '已付款', '逾期']

const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

async function loadList() {
  try {
    const res = await listPayables({
      keyword: keyword.value || undefined,
      status: statusFilter.value || undefined,
      page: currentPage.value,
      pageSize: pageSize.value,
    })
    payables.value = res.rows
    total.value = res.total
  } catch (e: any) {
    ElMessage.error(e.message || '加载失败')
  }
}

function handleSizeChange() {
  currentPage.value = 1
  loadList()
}

watch([keyword, statusFilter], () => {
  currentPage.value = 1
  loadList()
})

async function loadAll() {
  try {
    const [pending, st] = await Promise.all([listPendingReceipts(), payableStats()])
    pendingReceipts.value = pending
    stats.value = st
    await loadList()
  } catch (e: any) {
    ElMessage.error(e.message || '加载失败')
  }
}

onMounted(loadAll)

function formatMoney(value: number) {
  return `¥${Number(value || 0).toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`
}

function statusTagType(status: string) {
  if (status === '已付款') return 'success'
  if (status === '逾期') return 'danger'
  if (status === '待匹配') return 'warning'
  if (status === '待付款') return 'primary'
  return 'info'
}

async function handleMatch(row: Payable) {
  try {
    await matchPayable(row.id!)
    ElMessage.success('发票匹配成功')
    loadAll()
  } catch (e: any) {
    ElMessage.error(e.message || '操作失败')
  }
}

function handleAutoGenerate() {
  ElMessage.info('采购入库完成后系统已自动生成应付单；下表为待匹配发票的入库单')
}

function handleExport() {
  if (!payables.value.length) {
    ElMessage.warning('暂无可导出的应付款数据')
    return
  }
  downloadCsvFromRows('应付款列表', [
    { label: '单据号', value: (r) => r.code },
    { label: '入库单号', value: (r) => r.receiptCode || '' },
    { label: '供应商', value: (r) => r.supplier },
    { label: '采购订单', value: (r) => r.purchaseOrder || '' },
    { label: '采购金额', value: (r) => Number(r.purchaseAmount || 0).toFixed(2) },
    { label: '税额', value: (r) => Number(r.tax || 0).toFixed(2) },
    { label: '应付金额', value: (r) => Number(r.payable || 0).toFixed(2) },
    { label: '已付', value: (r) => Number(r.paid || 0).toFixed(2) },
    { label: '待付', value: (r) => Number(r.pending || 0).toFixed(2) },
    { label: '发票匹配', value: (r) => (r.matched ? '已匹配' : '未匹配') },
    { label: '到期日', value: (r) => r.dueDate },
    { label: '状态', value: (r) => r.status },
  ], payables.value)
  ElMessage.success('已导出当前页数据')
}

function handleRefresh() {
  loadAll()
  ElMessage.success('已刷新')
}

/* ---------------- 新增应付款 / 发票匹配 ---------------- */
const createVisible = ref(false)
const createFormRef = ref<FormInstance>()
const createSubmitting = ref(false)
const createForm = reactive({
  supplier: '',
  purchaseOrder: '',
  receiptCode: '',
  purchaseAmount: 0,
  tax: 0,
  dueDate: '',
})
const createRules: FormRules = {
  supplier: [{ required: true, message: '请填写供应商', trigger: 'blur' }],
  purchaseAmount: [{ required: true, message: '请填写采购金额', trigger: 'blur' }],
}

function handleCreate() {
  Object.assign(createForm, {
    supplier: '', purchaseOrder: '', receiptCode: '', purchaseAmount: 0, tax: 0, dueDate: '',
  })
  createVisible.value = true
}

async function submitCreate() {
  if (!createFormRef.value) return
  const valid = await createFormRef.value.validate().catch(() => false)
  if (!valid) return
  if (!(Number(createForm.purchaseAmount) > 0)) {
    ElMessage.warning('采购金额必须大于 0')
    return
  }
  createSubmitting.value = true
  try {
    await createPayable({
      supplier: createForm.supplier.trim(),
      purchaseOrder: createForm.purchaseOrder.trim(),
      receiptCode: createForm.receiptCode.trim(),
      purchaseAmount: Number(createForm.purchaseAmount),
      tax: Number(createForm.tax || 0),
      dueDate: createForm.dueDate,
    })
    ElMessage.success('应付款已创建')
    createVisible.value = false
    await loadAll()
  } catch (e: any) {
    ElMessage.error(e.message || '创建失败')
  } finally {
    createSubmitting.value = false
  }
}

/** 对「待生成应付款的入库单」行发起发票匹配 */
async function handleGenerateAp(row: PendingReceipt) {
  if (!row.id) {
    ElMessage.warning('该记录缺少应付单 ID，请刷新后重试')
    return
  }
  try {
    await matchPayable(row.id)
    ElMessage.success('发票匹配成功')
    await loadAll()
  } catch (e: any) {
    ElMessage.error(e.message || '匹配失败')
  }
}

function handleBatchGenerate() {
  if (!pendingReceipts.value.length) {
    ElMessage.warning('暂无待匹配的应付单')
    return
  }
  ElMessage.info(`共 ${pendingReceipts.value.length} 笔待匹配，请在下方列表逐笔点击「发票匹配」`)
}
</script>

<style scoped>
.pending-card { margin-bottom: 24px; }
.pending-card .card-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 16px; }
.pagination-bar { display: flex; justify-content: flex-end; margin-top: 16px; }
.form-tip { margin-left: 104px; font-size: 12px; color: var(--el-text-color-secondary); line-height: 1.6; }
</style>
