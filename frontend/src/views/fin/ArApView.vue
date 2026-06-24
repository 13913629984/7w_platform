<template>
  <section>
    <div class="sub-page-header">
      <div>
        <h2>应收应付管理</h2>
        <p class="page-desc">统一管理客户应收账款与供应商应付账款、账龄和回款付款计划。</p>
      </div>
      <el-space>
        <el-button size="small" plain @click="handleExport">导出</el-button>
        <el-button size="small" type="primary" @click="handleGenerate">生成应收款</el-button>
      </el-space>
    </div>

    <div class="stats-row">
      <div v-for="item in statCards" :key="item.label" class="stat-card" :class="item.color">
        <div class="stat-header">
          <span class="stat-title">{{ item.label }}</span>
          <div class="stat-icon">{{ item.icon }}</div>
        </div>
        <div class="stat-value">{{ item.value }} <small v-if="item.unit" class="stat-unit">{{ item.unit }}</small></div>
        <div class="stat-footer"><span :class="item.trendType">{{ item.trend }}</span></div>
      </div>
    </div>

    <div class="table-card">
      <el-tabs v-model="activeTab">
        <el-tab-pane label="应收账款" name="ar" />
        <el-tab-pane label="应付账款" name="ap" />
      </el-tabs>

      <div class="filter-bar">
        <div class="filter-left">
          <el-input
            v-model="keyword"
            style="width: 240px"
            clearable
            prefix-icon="Search"
            :placeholder="activeTab === 'ar' ? '搜索客户名称' : '搜索供应商'"
          />
        </div>
        <el-space>
          <el-button>筛选</el-button>
        </el-space>
      </div>

      <el-table v-if="activeTab === 'ar'" :data="filteredReceivables" stripe style="width: 100%">
        <el-table-column prop="code" label="单据编号" min-width="150" />
        <el-table-column prop="customer" label="客户名称" min-width="160" show-overflow-tooltip />
        <el-table-column label="关联销售订单" min-width="140">
          <template #default="{ row }"><el-button link type="primary">{{ row.salesOrder }}</el-button></template>
        </el-table-column>
        <el-table-column prop="contract" label="关联合同" width="130" />
        <el-table-column label="合同金额" width="120" align="right">
          <template #default="{ row }">{{ formatMoney(row.contractAmount) }}</template>
        </el-table-column>
        <el-table-column label="已回款" width="120" align="right">
          <template #default="{ row }">{{ formatMoney(row.received) }}</template>
        </el-table-column>
        <el-table-column label="待回款" width="120" align="right">
          <template #default="{ row }"><span class="danger">{{ formatMoney(row.pending) }}</span></template>
        </el-table-column>
        <el-table-column prop="dueDate" label="到期日" width="120" />
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="arTagType(row.status)" effect="light">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="90" align="center" fixed="right">
          <template #default><el-button link type="primary">查看</el-button></template>
        </el-table-column>
      </el-table>

      <el-table v-else :data="filteredPayables" stripe style="width: 100%">
        <el-table-column prop="code" label="单据编号" min-width="150" />
        <el-table-column prop="supplier" label="供应商" min-width="160" show-overflow-tooltip />
        <el-table-column prop="purchaseOrder" label="采购单号" width="140" />
        <el-table-column label="应付金额" width="120" align="right">
          <template #default="{ row }">{{ formatMoney(row.payableAmount) }}</template>
        </el-table-column>
        <el-table-column label="已付金额" width="120" align="right">
          <template #default="{ row }">{{ formatMoney(row.paid) }}</template>
        </el-table-column>
        <el-table-column label="待付金额" width="120" align="right">
          <template #default="{ row }"><span class="danger">{{ formatMoney(row.pending) }}</span></template>
        </el-table-column>
        <el-table-column prop="dueDate" label="到期日" width="120" />
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="apTagType(row.status)" effect="light">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="90" align="center" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary">{{ row.status === '已付款' ? '查看' : '付款' }}</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-bar">
        <el-pagination
          background
          layout="prev, pager, next"
          :total="activeTab === 'ar' ? 100 : 50"
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

const activeTab = ref<'ar' | 'ap'>('ar')
const keyword = ref('')

const statCards = [
  { label: '应收账款总额', value: '¥865,000', unit: '', trend: '12 笔待回款', trendType: '', color: 'blue', icon: '📥' },
  { label: '应付账款总额', value: '¥428,500', unit: '', trend: '8 笔待付款', trendType: '', color: 'red', icon: '📤' },
  { label: '逾期应收', value: '¥126,000', unit: '', trend: '3 笔逾期', trendType: 'down', color: 'orange', icon: '⚠️' },
  { label: '账龄分析', value: '28.5', unit: '天', trend: '较上月缩短 3.2 天', trendType: 'up', color: 'green', icon: '📊' },
]

const receivables = [
  { code: 'AR-2026-0515-001', customer: '深圳XX科技有限公司', salesOrder: 'SO20260515001', contract: 'HT-2026-0318', contractAmount: 280000, received: 196000, pending: 84000, dueDate: '2026-06-05', status: '即将到期' },
  { code: 'AR-2026-0518-002', customer: '广州YY电子股份', salesOrder: 'SO20260518001', contract: 'HT-2026-0325', contractAmount: 156000, received: 78000, pending: 78000, dueDate: '2026-06-15', status: '部分回款' },
  { code: 'AR-2026-0520-003', customer: '上海ZZ精密制造', salesOrder: 'SO20260520001', contract: 'HT-2026-0401', contractAmount: 320000, received: 0, pending: 320000, dueDate: '2026-06-20', status: '待回款' },
  { code: 'AR-2026-0428-004', customer: '北京AA科技集团', salesOrder: 'SO20260428001', contract: 'HT-2026-0215', contractAmount: 186000, received: 186000, pending: 0, dueDate: '2026-05-15', status: '已回款' },
  { code: 'AR-2026-0420-005', customer: '杭州BB智能装备', salesOrder: 'SO20260420001', contract: 'HT-2026-0210', contractAmount: 245000, received: 120000, pending: 125000, dueDate: '2026-05-10', status: '逾期' },
]

const payables = [
  { code: 'AP-2026-0515-001', supplier: '深圳XX电子元件', purchaseOrder: 'PO-2026-0515', payableAmount: 45600, paid: 0, pending: 45600, dueDate: '2026-06-02', status: '即将到期' },
  { code: 'AP-2026-0518-002', supplier: '东莞YY五金', purchaseOrder: 'PO-2026-0518', payableAmount: 18300, paid: 0, pending: 18300, dueDate: '2026-06-10', status: '待付款' },
  { code: 'AP-2026-0520-003', supplier: '苏州ZZ原材料', purchaseOrder: 'PO-2026-0520', payableAmount: 62800, paid: 0, pending: 62800, dueDate: '2026-06-18', status: '待付款' },
  { code: 'AP-2026-0510-004', supplier: '宁波CC塑胶', purchaseOrder: 'PO-2026-0510', payableAmount: 35200, paid: 35200, pending: 0, dueDate: '2026-05-28', status: '已付款' },
]

const filteredReceivables = computed(() =>
  receivables.filter((r) => !keyword.value || r.customer.includes(keyword.value) || r.code.includes(keyword.value)),
)
const filteredPayables = computed(() =>
  payables.filter((r) => !keyword.value || r.supplier.includes(keyword.value) || r.code.includes(keyword.value)),
)

function formatMoney(value: number) {
  return `¥${value.toLocaleString()}`
}

function arTagType(status: string) {
  if (status === '已回款') return 'success'
  if (status === '逾期') return 'danger'
  if (status === '即将到期') return 'warning'
  if (status === '部分回款') return 'primary'
  return 'info'
}

function apTagType(status: string) {
  if (status === '已付款') return 'success'
  if (status === '即将到期') return 'warning'
  return 'info'
}

function handleExport() {
  ElMessage.success('导出成功')
}

function handleGenerate() {
  ElMessage.success('生成应收款成功')
}
</script>

<style scoped>
.stat-unit { font-size: 14px; font-weight: 500; color: var(--text-secondary); }
.pagination-bar { display: flex; justify-content: flex-end; margin-top: 16px; }
</style>
