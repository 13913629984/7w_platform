<template>
  <section>
    <div class="sub-page-header">
      <div>
        <h2>成本归集</h2>
        <p class="page-desc">归集付款成本、分析成本构成并进行费用分配。</p>
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
        <div class="card-header"><div class="card-title">📊 成本构成分析</div></div>
        <div class="donut-wrap">
          <div class="donut-chart" :style="{ background: donutGradient }"></div>
          <div class="donut-center"><strong>{{ totalCostLabel }}</strong><span>成本总额</span></div>
        </div>
        <div class="chart-legend ov-legend">
          <span v-for="slice in donutSlices" :key="slice.name"><i :style="{ background: slice.color }"></i>{{ slice.name }}</span>
        </div>
      </div>

      <div class="card">
        <div class="card-header"><div class="card-title">📈 月度成本趋势</div></div>
        <div class="bdg-chart">
          <svg :viewBox="`0 0 ${trend.width} ${trend.height}`" preserveAspectRatio="none" role="img" aria-label="月度成本趋势">
            <line v-for="gl in trend.gridLines" :key="'g' + gl.value" :x1="trend.padLeft" :y1="gl.y" :x2="trend.width - trend.padRight" :y2="gl.y" class="bdg-grid" />
            <text v-for="gl in trend.gridLines" :key="'t' + gl.value" :x="trend.padLeft - 6" :y="gl.y + 4" text-anchor="end" class="bdg-axis-label">{{ gl.label }}</text>
            <g v-for="(g, idx) in trend.groups" :key="'tg' + idx">
              <rect :x="g.materialX" :y="g.materialY" :width="trend.barWidth" :height="g.materialH" class="bdg-bar material" />
              <rect :x="g.expenseX" :y="g.expenseY" :width="trend.barWidth" :height="g.expenseH" class="bdg-bar expense" />
              <text :x="g.labelX" :y="trend.height - 6" text-anchor="middle" class="bdg-axis-label">{{ g.name }}</text>
            </g>
            <polyline :points="trend.totalLine" class="bdg-trend-line" />
            <circle v-for="(p, idx) in trend.totalPoints" :key="'tp' + idx" :cx="p.x" :cy="p.y" r="3.5" class="bdg-trend-dot" />
          </svg>
        </div>
        <div class="chart-legend">
          <span><i class="legend-blue"></i>材料成本</span>
          <span><i class="legend-orange"></i>费用分配</span>
          <span><i class="legend-green"></i>总成本</span>
        </div>
      </div>
    </div>

    <div class="card pending-card">
      <div class="card-header">
        <div class="card-title">🧾 待归集成本的付款单</div>
        <el-button size="small" type="primary" plain @click="handleBatchCollect">批量归集</el-button>
      </div>
      <el-table :data="pendingPayments" stripe style="width: 100%" empty-text="暂无待归集付款单">
        <el-table-column prop="code" label="付款单号" min-width="150" />
        <el-table-column label="关联AP单据" min-width="140">
          <template #default="{ row }"><el-button link type="primary">{{ row.apCode }}</el-button></template>
        </el-table-column>
        <el-table-column prop="supplier" label="供应商" min-width="140" show-overflow-tooltip />
        <el-table-column label="付款金额" width="130" align="right">
          <template #default="{ row }">{{ formatMoney(row.amount) }}</template>
        </el-table-column>
        <el-table-column label="成本类型" width="160">
          <template #default="{ row }">
            <el-select v-model="row.costType" size="small" style="width: 130px">
              <el-option v-for="t in costTypeOptions" :key="t" :label="t" :value="t" />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" align="center">
          <template #default><el-button size="small" type="primary" @click="handleCollect">归集</el-button></template>
        </el-table-column>
      </el-table>
    </div>

    <div class="table-card">
      <div class="filter-bar">
        <div class="filter-left">
          <el-input v-model="keyword" style="width: 240px" clearable prefix-icon="Search" placeholder="搜索单据号/成本对象..." />
          <el-select v-model="costTypeFilter" style="width: 140px" placeholder="成本类型" clearable>
            <el-option v-for="t in costTypeOptions" :key="t" :label="t" :value="t" />
          </el-select>
        </div>
      </div>

      <el-table :data="filteredCollections" stripe style="width: 100%">
        <el-table-column prop="code" label="归集单号" min-width="150" />
        <el-table-column label="成本类型" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="costTagType(row.costType)" effect="light">{{ row.costType }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="关联付款单" min-width="140">
          <template #default="{ row }"><el-button link type="primary">{{ row.paymentCode }}</el-button></template>
        </el-table-column>
        <el-table-column prop="object" label="成本对象" min-width="140" show-overflow-tooltip />
        <el-table-column label="归集金额" width="130" align="right">
          <template #default="{ row }">{{ formatMoney(row.amount) }}</template>
        </el-table-column>
        <el-table-column label="分配状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="allocTagType(row.allocStatus)" effect="light">{{ row.allocStatus }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="date" label="归集日期" width="160" />
        <el-table-column label="操作" min-width="140" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary">详情</el-button>
            <el-button v-if="row.allocStatus !== '已分配'" link type="primary">分配费用</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-bar">
        <el-pagination
          background
          layout="total, prev, pager, next, jumper"
          :total="filteredCollections.length"
          :page-size="10"
          :current-page="1"
        />
      </div>
    </div>

    <div class="table-card">
      <div class="card-header"><div class="card-title">🗂️ 费用分配明细</div></div>
      <el-table :data="allocations" stripe style="width: 100%">
        <el-table-column prop="code" label="分配单号" min-width="150" />
        <el-table-column label="费用类型" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="costTagType(row.costType)" effect="light">{{ row.costType }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="关联归集单" min-width="140">
          <template #default="{ row }"><el-button link type="primary">{{ row.collectionCode }}</el-button></template>
        </el-table-column>
        <el-table-column prop="object" label="分配对象" min-width="140" show-overflow-tooltip />
        <el-table-column label="分配金额" width="130" align="right">
          <template #default="{ row }">{{ formatMoney(row.amount) }}</template>
        </el-table-column>
        <el-table-column prop="ratio" label="分配比例" width="100" align="center" />
        <el-table-column prop="date" label="分配日期" width="160" />
        <el-table-column label="操作" width="90" align="center" fixed="right">
          <template #default><el-button link type="primary">详情</el-button></template>
        </el-table-column>
      </el-table>

      <div class="pagination-bar">
        <el-pagination
          background
          layout="total, prev, pager, next, jumper"
          :total="allocations.length"
          :page-size="10"
          :current-page="1"
        />
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  listCollections,
  listAllocations,
  costStats,
  costCharts,
  type CostAllocation,
} from '@/api/fin/cost'

const DONUT_COLORS = ['#409eff', '#e6a23c', '#67c23a', '#909399', '#f56c6c', '#5bbfd4']

const keyword = ref('')
const costTypeFilter = ref('')

interface CollectionRow {
  id?: number
  code: string
  costType: string
  paymentCode: string
  object: string
  amount: number
  allocStatus: string
  date: string
}

const stats = ref({ collected: 0, allocated: 0, accumulated: 0, pending: 0, collectionCount: 0, allocationCount: 0 })
const distribution = ref<{ name: string; value: number }[]>([])
const collections = ref<CollectionRow[]>([])
const allocations = ref<CostAllocation[]>([])

const statCards = computed(() => [
  { label: '本月归集成本', value: formatMoney(stats.value.collected), trend: `${stats.value.collectionCount} 笔归集`, trendType: '', color: 'blue', icon: '📥' },
  { label: '已分配费用', value: formatMoney(stats.value.allocated), trend: `${stats.value.allocationCount} 笔分配`, trendType: '', color: 'green', icon: '✅' },
  { label: '累计成本', value: formatMoney(stats.value.accumulated), trend: '归集+分配合计', trendType: 'up', color: 'orange', icon: '📊' },
  { label: '待处理归集', value: formatMoney(stats.value.pending), trend: '待处理归集', trendType: 'down', color: 'red', icon: '⏳' },
])

const costTypeOptions = ['原材料', '设备采购', '运输费', '制造费用', '管理费用']

const pendingPayments = ref([
  { code: 'PAY20260528001', apCode: 'AP20260526001', supplier: '宁波CC塑胶', amount: 39776, costType: '原材料' },
  { code: 'PAY20260528002', apCode: 'AP20260528001', supplier: '上海芯片公司', amount: 9605, costType: '原材料' },
])

// 月度成本趋势 (单位: 万)
const trendData = [
  { name: '1月', material: 430, expense: 90, total: 520 },
  { name: '2月', material: 470, expense: 95, total: 565 },
  { name: '3月', material: 510, expense: 100, total: 610 },
  { name: '4月', material: 480, expense: 100, total: 580 },
  { name: '5月', material: 515, expense: 105, total: 620 },
  { name: '6月', material: 530, expense: 110, total: 660 },
]

async function loadAll() {
  try {
    const [cols, allocs, st, charts] = await Promise.all([
      listCollections(),
      listAllocations(),
      costStats(),
      costCharts(),
    ])
    collections.value = cols.map((c) => ({
      id: c.id,
      code: c.code,
      costType: c.costType,
      paymentCode: '',
      object: c.source,
      amount: c.amount,
      allocStatus: c.status === '已分配' ? '已分配' : '未分配',
      date: c.date,
    }))
    allocations.value = allocs
    stats.value = st
    distribution.value = charts.distribution
  } catch (e: any) {
    ElMessage.error(e.message || '加载失败')
  }
}

onMounted(loadAll)

const filteredCollections = computed(() =>
  collections.value.filter((r) => {
    const matchedKeyword = !keyword.value || r.object.includes(keyword.value) || r.code.includes(keyword.value)
    const matchedType = !costTypeFilter.value || r.costType === costTypeFilter.value
    return matchedKeyword && matchedType
  }),
)

const donutSlices = computed(() => {
  const total = distribution.value.reduce((sum, d) => sum + d.value, 0) || 1
  let acc = 0
  return distribution.value.map((d, idx) => {
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

const totalCostLabel = computed(() => {
  const total = distribution.value.reduce((sum, d) => sum + d.value, 0)
  return total >= 10000 ? `${(total / 10000).toFixed(1)}万` : total.toLocaleString()
})

const trend = computed(() => {
  const width = 520
  const height = 280
  const padLeft = 40
  const padRight = 12
  const padTop = 16
  const padBottom = 28
  const plotW = width - padLeft - padRight
  const plotH = height - padTop - padBottom
  const allVals: number[] = []
  trendData.forEach((d) => allVals.push(d.material, d.expense, d.total))
  const niceMax = Math.ceil(Math.max(1, ...allVals) / 100) * 100
  const slot = plotW / trendData.length
  const barWidth = Math.min(18, (slot * 0.55) / 2)
  const gap = 4
  const groupW = barWidth * 2 + gap
  const y = (v: number) => padTop + plotH - (v / niceMax) * plotH

  const groups = trendData.map((d, i) => {
    const center = padLeft + slot * i + slot / 2
    const startX = center - groupW / 2
    return {
      name: d.name,
      labelX: center,
      materialX: startX,
      materialH: padTop + plotH - y(d.material),
      materialY: y(d.material),
      expenseX: startX + barWidth + gap,
      expenseH: padTop + plotH - y(d.expense),
      expenseY: y(d.expense),
      totalX: center,
      totalY: y(d.total),
    }
  })

  const totalLine = groups.map((g) => `${g.totalX.toFixed(1)},${g.totalY.toFixed(1)}`).join(' ')
  const totalPoints = groups.map((g) => ({ x: g.totalX, y: g.totalY }))

  const gridCount = 7
  const gridLines = Array.from({ length: gridCount + 1 }, (_, i) => {
    const value = Math.round((niceMax / gridCount) * (gridCount - i))
    return { value, y: padTop + (plotH / gridCount) * i, label: `${value}万` }
  })

  return { width, height, padLeft, padRight, barWidth, groups, totalLine, totalPoints, gridLines }
})

function formatMoney(value: number) {
  return `¥${Number(value || 0).toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`
}

function costTagType(type: string) {
  if (type === '原材料') return 'primary'
  if (type === '设备采购') return 'warning'
  if (type === '运输费') return 'info'
  if (type === '制造费用') return 'success'
  if (type === '管理费用') return 'danger'
  return 'info'
}

function allocTagType(status: string) {
  if (status === '已分配') return 'success'
  if (status === '部分分配') return 'warning'
  if (status === '未分配') return 'info'
  return 'info'
}

function handleExport() {
  ElMessage.success('导出成功')
}
function handleRefresh() {
  loadAll()
  ElMessage.success('已刷新')
}
function handleBatchCollect() {
  ElMessage.success('批量归集成功')
}
function handleCollect() {
  ElMessage.success('归集成功')
}
</script>

<style scoped>
.pending-card { margin-bottom: 24px; }
.pending-card .card-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 16px; }
.table-card { margin-bottom: 24px; }
.bdg-chart { height: 280px; padding: 8px 4px 0; }
.bdg-chart svg { width: 100%; height: 100%; }
.bdg-grid { stroke: #ebeef5; stroke-width: 1; }
.bdg-axis-label { fill: #909399; font-size: 11px; }
.bdg-bar.material { fill: #409eff; }
.bdg-bar.expense { fill: #e6a23c; }
.bdg-trend-line { fill: none; stroke: #67c23a; stroke-width: 2.5; stroke-linejoin: round; stroke-linecap: round; }
.bdg-trend-dot { fill: #67c23a; stroke: #fff; stroke-width: 2; }
.legend-orange { background: #e6a23c; }
.ov-legend { flex-wrap: wrap; }
.pagination-bar { display: flex; justify-content: flex-end; margin-top: 16px; }
</style>
