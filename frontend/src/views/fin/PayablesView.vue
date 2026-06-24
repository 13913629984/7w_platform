<template>
  <section>
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
          <template #default><el-button size="small" type="primary" @click="handleGenerateAp">生成AP</el-button></template>
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

      <el-table :data="filteredPayables" stripe style="width: 100%">
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
            <el-button v-if="!row.matched" link type="primary">上传发票</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-bar">
        <el-pagination
          background
          layout="total, prev, pager, next, jumper"
          :total="filteredPayables.length"
          :page-size="10"
          :current-page="1"
        />
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { ElMessage } from 'element-plus'

const keyword = ref('')
const statusFilter = ref('')

const statCards = [
  { label: '应付账款总额', value: '¥108,254', trend: '4 笔单据', trendType: '', color: 'blue', icon: '📤' },
  { label: '待匹配发票', value: '2', trend: '需上传发票', trendType: '', color: 'orange', icon: '🧾' },
  { label: '已匹配发票', value: '2', trend: '可进行付款', trendType: '', color: 'green', icon: '✅' },
  { label: '逾期应付', value: '1', trend: '需紧急处理', trendType: 'down', color: 'red', icon: '⚠️' },
]

const pendingReceipts = [
  { code: 'RK20260528001', purchaseOrder: 'PO20260527001', supplier: '深圳电子科技', amount: 4500, tax: 585, payable: 5085 },
  { code: 'RK20260528002', purchaseOrder: 'PO20260528001', supplier: '东莞五金厂', amount: 3000, tax: 390, payable: 3390 },
]

const payables = [
  { code: 'AP20260528001', receiptCode: 'RK20260527001', supplier: '上海芯片公司', purchaseAmount: 8500, tax: 1105, payable: 9605, matched: true, dueDate: '2026-06-10', status: '待付款' },
  { code: 'AP20260527001', receiptCode: 'RK20260526001', supplier: '苏州原材料厂', purchaseAmount: 6500, tax: 845, payable: 7345, matched: false, dueDate: '2026-06-05', status: '待匹配' },
  { code: 'AP20260526001', receiptCode: 'RK20260525001', supplier: '宁波塑胶有限公司', purchaseAmount: 35200, tax: 4576, payable: 39776, matched: true, dueDate: '2026-05-28', status: '已付款' },
  { code: 'AP20260525001', receiptCode: 'RK20260524001', supplier: '深圳电子科技', purchaseAmount: 45600, tax: 5928, payable: 51528, matched: false, dueDate: '2026-06-02', status: '逾期' },
]

const statusOptions = ['待匹配', '待付款', '已付款', '逾期']

const filteredPayables = computed(() =>
  payables.filter((r) => {
    const matchedKeyword = !keyword.value || r.supplier.includes(keyword.value) || r.code.includes(keyword.value)
    const matchedStatus = !statusFilter.value || r.status === statusFilter.value
    return matchedKeyword && matchedStatus
  }),
)

function formatMoney(value: number) {
  return `¥${value.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`
}

function statusTagType(status: string) {
  if (status === '已付款') return 'success'
  if (status === '逾期') return 'danger'
  if (status === '待匹配') return 'warning'
  if (status === '待付款') return 'primary'
  return 'info'
}

function handleAutoGenerate() {
  ElMessage.success('自动生成AP成功')
}
function handleExport() {
  ElMessage.success('导出成功')
}
function handleRefresh() {
  ElMessage.success('已刷新')
}
function handleCreate() {
  ElMessage.info('新增应付款')
}
function handleBatchGenerate() {
  ElMessage.success('批量生成AP成功')
}
function handleGenerateAp() {
  ElMessage.success('生成AP成功')
}
</script>

<style scoped>
.pending-card { margin-bottom: 24px; }
.pending-card .card-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 16px; }
.pagination-bar { display: flex; justify-content: flex-end; margin-top: 16px; }
</style>
