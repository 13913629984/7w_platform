<template>
  <section>
    <div class="sub-page-header">
      <div>
        <h2>付款管理</h2>
        <p class="page-desc">管理付款申请、审批流程与付款执行。</p>
      </div>
      <el-space>
        <el-button size="small" plain @click="handleExport">导出</el-button>
        <el-button size="small" plain @click="handleRefresh">刷新</el-button>
        <el-button size="small" type="primary" @click="openCreate">+ 新增付款</el-button>
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

    <div class="card flow-card">
      <div class="card-header"><div class="card-title">📋 审批流程状态</div></div>
      <div class="flow-row">
        <div v-for="step in flowSteps" :key="step.no" class="flow-step">
          <div class="flow-step-no" :class="step.color">{{ step.no }}</div>
          <div class="flow-step-body">
            <div class="flow-step-title">{{ step.title }}</div>
            <div class="flow-step-desc">{{ step.desc }}</div>
          </div>
          <div class="flow-step-count">{{ step.count }}</div>
        </div>
      </div>
    </div>

    <div class="table-card">
      <div class="filter-bar">
        <div class="filter-left">
          <el-input v-model="keyword" style="width: 240px" clearable prefix-icon="Search" placeholder="搜索付款单号/供应商..." />
          <el-select v-model="statusFilter" style="width: 140px" placeholder="状态筛选" clearable>
            <el-option v-for="s in statusOptions" :key="s" :label="s" :value="s" />
          </el-select>
        </div>
      </div>

      <el-table :data="filteredPayments" stripe style="width: 100%">
        <el-table-column prop="code" label="付款单号" min-width="150" />
        <el-table-column label="付款类型" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="row.type === '采购付款' ? 'warning' : 'primary'" effect="light">{{ row.type }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="关联AP单据" min-width="140">
          <template #default="{ row }">
            <el-button v-if="row.apCode" link type="primary">{{ row.apCode }}</el-button>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="payee" label="供应商/收款方" min-width="140" show-overflow-tooltip />
        <el-table-column label="付款金额" width="120" align="right">
          <template #default="{ row }">{{ formatMoney(row.amount) }}</template>
        </el-table-column>
        <el-table-column prop="method" label="付款方式" width="110" align="center" />
        <el-table-column prop="applyDate" label="申请日期" width="120" />
        <el-table-column label="审批状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" effect="light">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" min-width="140" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary">详情</el-button>
            <el-button v-if="row.status === '已通过'" link type="primary" @click="handleApprove(row)">确认支付</el-button>
            <el-button v-if="row.status === '待审批' || row.status === '审批中'" link type="primary" @click="handleApprove(row)">审批</el-button>
            <el-button v-if="row.status === '待审批' || row.status === '审批中'" link type="danger" @click="handleReject(row)">驳回</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-bar">
        <el-pagination
          background
          layout="total, prev, pager, next, jumper"
          :total="filteredPayments.length"
          :page-size="10"
          :current-page="1"
        />
      </div>
    </div>

    <el-dialog v-model="createVisible" title="新增付款" width="520px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="付款单号" required>
          <el-input v-model="form.code" placeholder="自动生成" disabled />
        </el-form-item>
        <el-form-item label="付款类型" required>
          <el-select v-model="form.type" style="width: 100%">
            <el-option label="采购付款" value="采购付款" />
            <el-option label="费用报销" value="费用报销" />
          </el-select>
        </el-form-item>
        <el-form-item label="关联AP单据">
          <el-input v-model="form.apCode" placeholder="请选择关联AP单据" />
        </el-form-item>
        <el-form-item label="供应商/收款方" required>
          <el-input v-model="form.payee" placeholder="请输入收款方" />
        </el-form-item>
        <el-form-item label="付款金额" required>
          <el-input-number v-model="form.amount" :min="0" :precision="2" :step="100" controls-position="right" style="width: 100%" />
        </el-form-item>
        <el-form-item label="付款方式" required>
          <el-select v-model="form.method" style="width: 100%">
            <el-option label="银行转账" value="银行转账" />
            <el-option label="支付宝" value="支付宝" />
            <el-option label="微信" value="微信" />
            <el-option label="现金" value="现金" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="请输入备注信息" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" @click="submitCreate">提交审批</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  listPayments,
  paymentStats,
  createPayment,
  approvePayment,
  rejectPayment,
  type Payment,
} from '@/api/fin/payment'

const keyword = ref('')
const statusFilter = ref('')
const createVisible = ref(false)

const payments = ref<Payment[]>([])
const stats = ref({ monthTotal: 0, paid: 0, pendingApprove: 0, approving: 0, approved: 0, rejected: 0 })

const statCards = computed(() => [
  { label: '本月付款总额', value: formatMoney(stats.value.monthTotal), trend: '全部付款单', trendType: '', color: 'blue', icon: '💸' },
  { label: '已付款', value: `${stats.value.paid} 笔`, trend: '已完成支付', trendType: '', color: 'green', icon: '✅' },
  { label: '待审批', value: `${stats.value.pendingApprove} 笔`, trend: '等待审批', trendType: '', color: 'orange', icon: '⏳' },
  { label: '已驳回', value: `${stats.value.rejected} 笔`, trend: '需重新提交', trendType: 'down', color: 'red', icon: '⛔' },
])

const flowSteps = computed(() => [
  { no: 1, title: '待审批', desc: '等待主管审批', count: stats.value.pendingApprove, color: 'orange' },
  { no: 2, title: '审批中', desc: '财务处理审核', count: stats.value.approving, color: 'blue' },
  { no: 3, title: '已通过', desc: '等待支付', count: stats.value.approved, color: 'purple' },
  { no: 4, title: '已付款', desc: '支付完成', count: stats.value.paid, color: 'green' },
])

const statusOptions = ['待审批', '审批中', '已通过', '已付款', '已驳回']

const filteredPayments = computed(() =>
  payments.value.filter((r) => {
    const matchedKeyword = !keyword.value || r.payee.includes(keyword.value) || r.code.includes(keyword.value)
    const matchedStatus = !statusFilter.value || r.status === statusFilter.value
    return matchedKeyword && matchedStatus
  }),
)

const form = reactive({
  code: '',
  type: '采购付款',
  apCode: '',
  payee: '',
  amount: 0,
  method: '银行转账',
  remark: '',
})

async function loadAll() {
  try {
    const [list, st] = await Promise.all([listPayments(), paymentStats()])
    payments.value = list
    stats.value = st
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
  if (status === '已通过') return 'success'
  if (status === '待审批') return 'warning'
  if (status === '审批中') return 'primary'
  if (status === '已驳回') return 'danger'
  return 'info'
}

function openCreate() {
  form.code = ''
  form.type = '采购付款'
  form.apCode = ''
  form.payee = ''
  form.amount = 0
  form.method = '银行转账'
  form.remark = ''
  createVisible.value = true
}

async function submitCreate() {
  if (!form.payee) {
    ElMessage.warning('请输入收款方')
    return
  }
  try {
    await createPayment({
      type: form.type,
      apCode: form.apCode,
      payee: form.payee,
      amount: form.amount,
      method: form.method,
      remark: form.remark,
    })
    createVisible.value = false
    ElMessage.success('已提交审批')
    loadAll()
  } catch (e: any) {
    ElMessage.error(e.message || '提交失败')
  }
}

async function handleApprove(row: Payment) {
  try {
    await approvePayment(row.id!)
    ElMessage.success('操作成功')
    loadAll()
  } catch (e: any) {
    ElMessage.error(e.message || '操作失败')
  }
}

async function handleReject(row: Payment) {
  try {
    await rejectPayment(row.id!)
    ElMessage.success('已驳回')
    loadAll()
  } catch (e: any) {
    ElMessage.error(e.message || '操作失败')
  }
}

function handleExport() {
  ElMessage.success('导出成功')
}

function handleRefresh() {
  loadAll()
  ElMessage.success('已刷新')
}
</script>

<style scoped>
.flow-card { margin-bottom: 24px; }
.flow-row { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; }
.flow-step { display: flex; align-items: center; gap: 12px; padding: 16px; border: 1px solid var(--border-color); border-radius: 10px; background: #fafbfc; }
.flow-step-no { width: 28px; height: 28px; border-radius: 50%; color: #fff; display: flex; align-items: center; justify-content: center; font-size: 13px; font-weight: 600; flex-shrink: 0; }
.flow-step-no.orange { background: #e6a23c; }
.flow-step-no.blue { background: #409eff; }
.flow-step-no.purple { background: #764ba2; }
.flow-step-no.green { background: #67c23a; }
.flow-step-body { flex: 1; }
.flow-step-title { font-size: 14px; font-weight: 600; color: var(--text-primary); }
.flow-step-desc { font-size: 12px; color: var(--text-secondary); margin-top: 2px; }
.flow-step-count { font-size: 20px; font-weight: 700; color: var(--text-primary); }
.pagination-bar { display: flex; justify-content: flex-end; margin-top: 16px; }
</style>
