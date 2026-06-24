<template>
  <section>
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

        <el-table :data="filteredReceivables" stripe style="width: 100%">
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
              <el-button v-if="row.pending > 0" link type="primary">回款核销</el-button>
              <el-button v-if="!row.invoiced" link type="primary">开票</el-button>
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

        <el-table :data="filteredRecords" stripe style="width: 100%">
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
          background
          layout="total, prev, pager, next, jumper"
          :total="activeTab === 'list' ? filteredReceivables.length : filteredRecords.length"
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

const activeTab = ref<'list' | 'records'>('list')
const keyword = ref('')
const statusFilter = ref('')
const customerFilter = ref('')
const recordKeyword = ref('')

const statCards = [
  { label: '应收款总额', value: '¥31,100,000', trend: '全部应收款', trendType: '', color: 'blue', icon: '📥' },
  { label: '已核销', value: '¥3,200,000', trend: '已回款金额', trendType: '', color: 'green', icon: '✅' },
  { label: '待核销', value: '¥17,090,000', trend: '待回款金额', trendType: '', color: 'orange', icon: '⏳' },
  { label: '本月回款', value: '¥285万', trend: '本月累计回款', trendType: '', color: 'purple', icon: '💰' },
]

const receivables = [
  { code: 'AR20260528001', customer: '华为技术有限公司', salesOrder: 'SO20260528001', amount: 5800000, writtenOff: 4060000, pending: 1740000, invoiced: true, dueDate: '2026-06-28', status: '部分核销' },
  { code: 'AR20260527001', customer: '比亚迪股份有限公司', salesOrder: 'SO20260527001', amount: 8600000, writtenOff: 0, pending: 8600000, invoiced: true, dueDate: '2026-06-27', status: '待核销' },
  { code: 'AR20260526001', customer: '宁德时代新能源', salesOrder: 'SO20260526001', amount: 12000000, writtenOff: 6000000, pending: 6000000, invoiced: false, dueDate: '2026-06-26', status: '部分核销' },
  { code: 'AR20260525001', customer: '小米科技有限公司', salesOrder: 'SO20260525001', amount: 3200000, writtenOff: 3200000, pending: 0, invoiced: true, dueDate: '2026-05-25', status: '已核销' },
  { code: 'AR20260524001', customer: '大疆创新科技有限公司', salesOrder: 'SO20260524001', amount: 1500000, writtenOff: 750000, pending: 750000, invoiced: true, dueDate: '2026-05-10', status: '逾期' },
]

const records = [
  { code: 'RK20260528001', customer: '华为技术有限公司', receivableCode: 'AR20260528001', amount: 4060000, method: '银行转账', date: '2026-06-15', operator: '张三' },
  { code: 'RK20260526001', customer: '宁德时代新能源', receivableCode: 'AR20260526001', amount: 6000000, method: '银行转账', date: '2026-06-12', operator: '李四' },
  { code: 'RK20260525001', customer: '小米科技有限公司', receivableCode: 'AR20260525001', amount: 3200000, method: '承兑汇票', date: '2026-05-20', operator: '王五' },
  { code: 'RK20260524001', customer: '大疆创新科技有限公司', receivableCode: 'AR20260524001', amount: 750000, method: '银行转账', date: '2026-05-08', operator: '张三' },
]

const statusOptions = ['待核销', '部分核销', '已核销', '逾期']
const customerOptions = computed(() => Array.from(new Set(receivables.map((r) => r.customer))))

const filteredReceivables = computed(() =>
  receivables.filter((r) => {
    const matchedKeyword = !keyword.value || r.customer.includes(keyword.value) || r.code.includes(keyword.value)
    const matchedStatus = !statusFilter.value || r.status === statusFilter.value
    const matchedCustomer = !customerFilter.value || r.customer === customerFilter.value
    return matchedKeyword && matchedStatus && matchedCustomer
  }),
)

const filteredRecords = computed(() =>
  records.filter((r) => !recordKeyword.value || r.customer.includes(recordKeyword.value) || r.code.includes(recordKeyword.value)),
)

function formatMoney(value: number) {
  return `¥${value.toLocaleString()}`
}

function statusTagType(status: string) {
  if (status === '已核销') return 'success'
  if (status === '逾期') return 'danger'
  if (status === '部分核销') return 'warning'
  if (status === '待核销') return 'primary'
  return 'info'
}

function handleExport() {
  ElMessage.success('导出成功')
}

function handleRefresh() {
  ElMessage.success('已刷新')
}

function handleGenerate() {
  ElMessage.success('生成应收款成功')
}
</script>

<style scoped>
.amount { color: #e6a23c; }
.pagination-bar { display: flex; justify-content: flex-end; margin-top: 16px; }
</style>
