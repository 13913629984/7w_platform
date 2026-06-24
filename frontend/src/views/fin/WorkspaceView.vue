<template>
  <section>
    <div class="sub-page-header">
      <div>
        <h2>财务工作台</h2>
        <p class="page-desc">汇总收入支出、收支构成与待办收付情况。</p>
      </div>
      <el-space>
        <el-button size="small" plain @click="handleExport">导出</el-button>
        <el-button size="small" plain @click="handleRefresh">刷新</el-button>
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

    <div class="dashboard-grid">
      <div class="card">
        <div class="card-header">
          <div class="card-title">月度收入支出趋势</div>
          <el-radio-group v-model="trendRange" size="small">
            <el-radio-button label="6">近6月</el-radio-button>
            <el-radio-button label="12">近12月</el-radio-button>
          </el-radio-group>
        </div>
        <div class="bdg-chart">
          <svg :viewBox="`0 0 ${trend.width} ${trend.height}`" preserveAspectRatio="none" role="img" aria-label="月度收入支出趋势">
            <line v-for="gl in trend.gridLines" :key="'g' + gl.value" :x1="trend.padLeft" :y1="gl.y" :x2="trend.width - trend.padRight" :y2="gl.y" class="bdg-grid" />
            <text v-for="gl in trend.gridLines" :key="'t' + gl.value" :x="trend.padLeft - 6" :y="gl.y + 4" text-anchor="end" class="bdg-axis-label">{{ gl.label }}</text>
            <g v-for="(g, idx) in trend.groups" :key="'tg' + idx">
              <rect :x="g.incomeX" :y="g.incomeY" :width="trend.barWidth" :height="g.incomeH" class="bdg-bar income" />
              <rect :x="g.expenseX" :y="g.expenseY" :width="trend.barWidth" :height="g.expenseH" class="bdg-bar expense" />
              <text :x="g.labelX" :y="trend.height - 6" text-anchor="middle" class="bdg-axis-label">{{ g.name }}</text>
            </g>
            <polyline :points="trend.profitLine" class="bdg-trend-line" />
            <circle v-for="(p, idx) in trend.profitPoints" :key="'pp' + idx" :cx="p.x" :cy="p.y" r="3.5" class="bdg-trend-dot" />
          </svg>
        </div>
        <div class="chart-legend">
          <span><i class="legend-green"></i>收入</span>
          <span><i class="legend-red"></i>支出</span>
          <span><i class="legend-blue"></i>利润</span>
        </div>
      </div>

      <div class="card">
        <div class="card-header"><div class="card-title">收支构成</div></div>
        <div class="donut-wrap">
          <div class="donut-chart" :style="{ background: donutGradient }"></div>
          <div class="donut-center"><strong>{{ totalIncomeLabel }}</strong><span>总收入</span></div>
        </div>
        <div class="chart-legend ov-legend">
          <span v-for="slice in donutSlices" :key="slice.name"><i :style="{ background: slice.color }"></i>{{ slice.name }}</span>
        </div>
      </div>
    </div>

    <div class="table-card recent-card">
      <div class="card-header expense-header">
        <div class="card-title">近期收支记录</div>
        <el-button link type="primary" @click="goTo('/fin/ar-ap')">查看更多</el-button>
      </div>
      <el-table :data="recentRecords" stripe style="width: 100%">
        <el-table-column prop="code" label="单据编号" min-width="150" />
        <el-table-column prop="bizType" label="业务类型" width="120" />
        <el-table-column label="关联模块" width="120" align="center">
          <template #default="{ row }"><span class="module-tag" :class="row.moduleColor">{{ row.module }}</span></template>
        </el-table-column>
        <el-table-column label="金额" width="140" align="right">
          <template #default="{ row }">
            <span :class="row.direction === '收入' ? 'income-text' : 'expense-text'">
              {{ row.direction === '收入' ? '+' : '-' }} {{ formatMoney(row.amount) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="方向" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.direction === '收入' ? 'success' : 'danger'" effect="light">{{ row.direction }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" effect="light">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="date" label="发生日期" width="130" />
      </el-table>
    </div>

    <div class="dashboard-grid">
      <div class="card">
        <div class="card-header">
          <div class="card-title">待回款提醒</div>
          <el-button link type="primary" @click="goTo('/fin/receivables')">查看全部</el-button>
        </div>
        <el-table :data="receivableReminders" stripe style="width: 100%">
          <el-table-column prop="customer" label="客户" min-width="110" show-overflow-tooltip />
          <el-table-column label="合同金额" min-width="92" align="right">
            <template #default="{ row }">{{ formatMoney(row.contractAmount) }}</template>
          </el-table-column>
          <el-table-column label="已回款" min-width="82" align="right">
            <template #default="{ row }">{{ formatMoney(row.received) }}</template>
          </el-table-column>
          <el-table-column label="待回款" min-width="82" align="right">
            <template #default="{ row }"><span class="danger">{{ formatMoney(row.pending) }}</span></template>
          </el-table-column>
          <el-table-column prop="dueDate" label="到期日" min-width="100" align="center" />
          <el-table-column label="状态" min-width="82" align="center">
            <template #default="{ row }"><el-tag :type="arTagType(row.status)" effect="light">{{ row.status }}</el-tag></template>
          </el-table-column>
        </el-table>
      </div>

      <div class="card">
        <div class="card-header">
          <div class="card-title">待付款提醒</div>
          <el-button link type="primary" @click="goTo('/fin/payables')">查看全部</el-button>
        </div>
        <el-table :data="payableReminders" stripe style="width: 100%">
          <el-table-column prop="supplier" label="供应商" min-width="120" show-overflow-tooltip />
          <el-table-column prop="purchaseOrder" label="采购单号" min-width="120" />
          <el-table-column label="应付金额" min-width="92" align="right">
            <template #default="{ row }"><span class="danger">{{ formatMoney(row.payable) }}</span></template>
          </el-table-column>
          <el-table-column prop="dueDate" label="到期日" min-width="100" align="center" />
          <el-table-column label="状态" min-width="82" align="center">
            <template #default="{ row }"><el-tag :type="apTagType(row.status)" effect="light">{{ row.status }}</el-tag></template>
          </el-table-column>
        </el-table>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getWorkspace } from '@/api/fin/workspace'

const DONUT_COLORS = ['#409eff', '#67c23a', '#e6a23c', '#909399']

const router = useRouter()
const trendRange = ref('6')

const wsStats = ref({ income: 0, expense: 0, profit: 0, pendingReceipt: 0 })

const statCards = computed(() => [
  { label: '本月收入', value: formatWan(wsStats.value.income), trend: '本月累计收入', trendType: 'up', color: 'green', icon: '📈' },
  { label: '本月支出', value: formatWan(wsStats.value.expense), trend: '本月累计支出', trendType: 'down', color: 'red', icon: '📉' },
  { label: '本月利润', value: formatWan(wsStats.value.profit), trend: '收入减支出', trendType: 'up', color: 'blue', icon: '💰' },
  { label: '待回款金额', value: formatWan(wsStats.value.pendingReceipt), trend: '应收待回款', trendType: '', color: 'orange', icon: '⏳' },
])

const MODULE_COLORS: Record<string, string> = { 'CRM 合同': 'green', '采购订单': 'orange', '维保工单': 'blue', '电测系统': 'blue', '费用管理': 'orange' }
const recentRecords = ref<{ code: string; bizType: string; module: string; moduleColor: string; amount: number; direction: string; status: string; date: string }[]>([])

async function loadWorkspace() {
  try {
    const data = await getWorkspace()
    wsStats.value = data.stats
    recentRecords.value = data.records.map((r) => ({
      code: r.code,
      bizType: r.bizType,
      module: r.module,
      moduleColor: MODULE_COLORS[r.module] || 'blue',
      amount: r.amount,
      direction: r.direction,
      status: r.status,
      date: r.date,
    }))
  } catch (e: any) {
    ElMessage.error(e.message || '加载失败')
  }
}

onMounted(loadWorkspace)

function formatWan(value: number) {
  const wan = Number(value || 0) / 10000
  return `¥${wan.toFixed(1)}万`
}

// 月度收入支出趋势 (单位: 万)
const trend6 = [
  { name: '1月', income: 278, expense: 156, profit: 122 },
  { name: '2月', income: 305, expense: 178, profit: 127 },
  { name: '3月', income: 292, expense: 165, profit: 127 },
  { name: '4月', income: 320, expense: 192, profit: 128 },
  { name: '5月', income: 318, expense: 182, profit: 136 },
  { name: '6月', income: 348, expense: 197, profit: 151 },
]
const trend12 = [
  { name: '7月', income: 240, expense: 150, profit: 90 },
  { name: '8月', income: 260, expense: 160, profit: 100 },
  { name: '9月', income: 255, expense: 158, profit: 97 },
  { name: '10月', income: 280, expense: 170, profit: 110 },
  { name: '11月', income: 295, expense: 175, profit: 120 },
  { name: '12月', income: 310, expense: 180, profit: 130 },
  ...trend6,
]

const income = [
  { name: '产品销售', value: 1800 },
  { name: '服务收入', value: 980 },
  { name: '维保收入', value: 320 },
  { name: '其他收入', value: 185 },
]

const receivableReminders = [
  { customer: '深圳XX科技有限公司', contractAmount: 280000, received: 196000, pending: 84000, dueDate: '2026-06-05', status: '即将到期' },
  { customer: '广州YY电子股份', contractAmount: 156000, received: 78000, pending: 78000, dueDate: '2026-06-15', status: '部分回款' },
  { customer: '上海ZZ精密制造', contractAmount: 320000, received: 0, pending: 320000, dueDate: '2026-06-20', status: '待回款' },
]

const payableReminders = [
  { supplier: '深圳XX电子元件', purchaseOrder: 'PO-2026-0515', payable: 45600, dueDate: '2026-06-02', status: '即将到期' },
  { supplier: '东莞YY五金', purchaseOrder: 'PO-2026-0518', payable: 18300, dueDate: '2026-06-10', status: '待付款' },
  { supplier: '苏州ZZ原材料', purchaseOrder: 'PO-2026-0520', payable: 62800, dueDate: '2026-06-18', status: '待付款' },
]

const trendData = computed(() => (trendRange.value === '6' ? trend6 : trend12))

const trend = computed(() => {
  const width = 540
  const height = 280
  const padLeft = 40
  const padRight = 12
  const padTop = 16
  const padBottom = 28
  const plotW = width - padLeft - padRight
  const plotH = height - padTop - padBottom
  const data = trendData.value
  const allVals: number[] = []
  data.forEach((d) => allVals.push(d.income, d.expense, d.profit))
  const niceMax = Math.ceil(Math.max(1, ...allVals) / 100) * 100
  const slot = plotW / data.length
  const barWidth = Math.min(16, (slot * 0.5) / 2)
  const gap = 3
  const groupW = barWidth * 2 + gap
  const y = (v: number) => padTop + plotH - (v / niceMax) * plotH

  const groups = data.map((d, i) => {
    const center = padLeft + slot * i + slot / 2
    const startX = center - groupW / 2
    return {
      name: d.name,
      labelX: center,
      incomeX: startX,
      incomeH: padTop + plotH - y(d.income),
      incomeY: y(d.income),
      expenseX: startX + barWidth + gap,
      expenseH: padTop + plotH - y(d.expense),
      expenseY: y(d.expense),
      profitX: center,
      profitY: y(d.profit),
    }
  })

  const profitLine = groups.map((g) => `${g.profitX.toFixed(1)},${g.profitY.toFixed(1)}`).join(' ')
  const profitPoints = groups.map((g) => ({ x: g.profitX, y: g.profitY }))

  const gridCount = 4
  const gridLines = Array.from({ length: gridCount + 1 }, (_, i) => {
    const value = Math.round((niceMax / gridCount) * (gridCount - i))
    return { value, y: padTop + (plotH / gridCount) * i, label: `${value}万` }
  })

  return { width, height, padLeft, padRight, barWidth, groups, profitLine, profitPoints, gridLines }
})

const donutSlices = computed(() => {
  const total = income.reduce((sum, d) => sum + d.value, 0) || 1
  let acc = 0
  return income.map((d, idx) => {
    const start = (acc / total) * 100
    acc += d.value
    const end = (acc / total) * 100
    return { name: d.name, color: DONUT_COLORS[idx % DONUT_COLORS.length], start, end }
  })
})

const donutGradient = computed(() => {
  const stops = donutSlices.value.map((s) => `${s.color} ${s.start.toFixed(2)}% ${s.end.toFixed(2)}%`)
  return `conic-gradient(${stops.join(', ')})`
})

const totalIncomeLabel = computed(() => {
  const total = income.reduce((sum, d) => sum + d.value, 0)
  return `${total}万`
})

function formatMoney(value: number) {
  return `¥${value.toLocaleString()}`
}

function goTo(path: string) {
  router.push(path)
}

function statusTagType(status: string) {
  if (status === '已确认' || status === '已支付') return 'success'
  if (status === '处理中') return 'primary'
  return 'info'
}

function arTagType(status: string) {
  if (status === '即将到期') return 'warning'
  if (status === '部分回款') return 'primary'
  if (status === '待回款') return 'info'
  return 'info'
}

function apTagType(status: string) {
  if (status === '即将到期') return 'warning'
  return 'info'
}

function handleExport() {
  ElMessage.success('导出成功')
}
function handleRefresh() {
  ElMessage.success('已刷新')
}
</script>

<style scoped>
.bdg-chart { height: 280px; padding: 8px 4px 0; }
.bdg-chart svg { width: 100%; height: 100%; }
.bdg-grid { stroke: #ebeef5; stroke-width: 1; }
.bdg-axis-label { fill: #909399; font-size: 11px; }
.bdg-bar.income { fill: #67c23a; }
.bdg-bar.expense { fill: #f56c6c; }
.bdg-trend-line { fill: none; stroke: #409eff; stroke-width: 2.5; stroke-linejoin: round; stroke-linecap: round; }
.bdg-trend-dot { fill: #fff; stroke: #409eff; stroke-width: 2; }
.legend-red { background: #f56c6c; }
.ov-legend { flex-wrap: wrap; }
.recent-card { margin-bottom: 24px; }
.expense-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 16px; }
.income-text { color: #67c23a; font-weight: 600; }
.expense-text { color: #f56c6c; font-weight: 600; }
.module-tag { font-size: 12px; }
.module-tag.green { color: #67c23a; }
.module-tag.orange { color: #e6a23c; }
.module-tag.blue { color: #409eff; }
</style>
