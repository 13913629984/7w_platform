<template>
  <section v-loading="loading">
    <div class="sub-page-header">
      <div>
        <h2>库存概览</h2>
        <p class="page-desc">查看仓储运营、库存与出入库概览。</p>
      </div>
      <el-space>
        <el-button size="small" plain @click="handleExport">导出</el-button>
        <el-button size="small" plain @click="loadData">刷新</el-button>
      </el-space>
    </div>

    <div class="stats-row">
      <div v-for="item in statCards" :key="item.label" class="stat-card" :class="item.color">
        <div class="stat-header"><span class="stat-title">{{ item.label }}</span><div class="stat-icon">{{ item.icon }}</div></div>
        <div class="stat-value">{{ item.value }} <small v-if="item.unit" class="stat-unit">{{ item.unit }}</small></div>
        <div class="stat-footer"><span :class="item.trendType">{{ item.trend }}</span></div>
      </div>
    </div>

    <div class="dashboard-grid">
      <div class="card">
        <div class="card-header"><div class="card-title">库存分布</div></div>
        <div class="donut-wrap">
          <div class="donut-chart" :style="{ background: donutGradient }"></div>
          <div class="donut-center"><strong>{{ totalQtyLabel }}</strong><span>库存总量</span></div>
        </div>
        <div class="chart-legend ov-legend">
          <span v-for="slice in donutSlices" :key="slice.name"><i :style="{ background: slice.color }"></i>{{ slice.name }}</span>
        </div>
      </div>

      <div class="card">
        <div class="card-header"><div class="card-title">近7日出入库趋势</div></div>
        <div class="ov-line-chart">
          <svg :viewBox="`0 0 ${chart.width} ${chart.height}`" preserveAspectRatio="none" role="img" aria-label="近7日出入库趋势">
            <line v-for="gl in chart.gridLines" :key="'g' + gl.value" :x1="chart.padLeft" :y1="gl.y" :x2="chart.width - chart.padRight" :y2="gl.y" class="ov-grid" />
            <text v-for="gl in chart.gridLines" :key="'t' + gl.value" :x="chart.padLeft - 8" :y="gl.y + 4" text-anchor="end" class="ov-axis-label">{{ gl.value }}</text>
            <polygon :points="chart.inboundArea" class="ov-area inbound" />
            <polygon :points="chart.outboundArea" class="ov-area outbound" />
            <polyline :points="chart.inboundLine" class="ov-line inbound" />
            <polyline :points="chart.outboundLine" class="ov-line outbound" />
            <circle v-for="(pt, idx) in chart.points" :key="'in' + idx" :cx="pt.x" :cy="pt.inboundY" r="4" class="ov-dot inbound" />
            <circle v-for="(pt, idx) in chart.points" :key="'out' + idx" :cx="pt.x" :cy="pt.outboundY" r="4" class="ov-dot outbound" />
            <text v-for="(pt, idx) in chart.points" :key="'x' + idx" :x="pt.x" :y="chart.height - 6" text-anchor="middle" class="ov-axis-label">{{ pt.date }}</text>
          </svg>
        </div>
        <div class="chart-legend ov-legend">
          <span><i class="legend-blue"></i>入库</span>
          <span><i class="legend-green"></i>出库</span>
        </div>
      </div>
    </div>

    <div class="dashboard-grid">
      <div class="card">
        <div class="card-header">
          <div class="card-title">待入库列表</div>
          <el-button link type="primary" @click="goTo('/wms/inbound')">查看全部</el-button>
        </div>
        <el-table :data="data?.pendingInbound ?? []" stripe style="width: 100%" empty-text="暂无待入库单">
          <el-table-column prop="code" label="入库单号" min-width="150" />
          <el-table-column prop="supplier" label="供应商" min-width="120" show-overflow-tooltip />
          <el-table-column prop="warehouse" label="仓库" width="120" show-overflow-tooltip />
          <el-table-column prop="totalQty" label="数量" width="80" align="right" />
          <el-table-column label="状态" width="90" align="center">
            <template #default="{ row }">
              <el-tag :type="orderTagType(row.status)" effect="light">{{ orderStatusLabel(row.status) }}</el-tag>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <div class="card">
        <div class="card-header">
          <div class="card-title">待出库列表</div>
          <el-button link type="primary" @click="goTo('/wms/outbound')">查看全部</el-button>
        </div>
        <el-table :data="data?.pendingOutbound ?? []" stripe style="width: 100%" empty-text="暂无待出库单">
          <el-table-column prop="code" label="出库单号" min-width="150" />
          <el-table-column prop="customer" label="客户" min-width="120" show-overflow-tooltip />
          <el-table-column prop="warehouse" label="仓库" width="120" show-overflow-tooltip />
          <el-table-column prop="totalQty" label="数量" width="80" align="right" />
          <el-table-column label="状态" width="90" align="center">
            <template #default="{ row }">
              <el-tag :type="orderTagType(row.status)" effect="light">{{ orderStatusLabel(row.status) }}</el-tag>
            </template>
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
import { getOverview, type OverviewData } from '@/api/wms/overview'

const DONUT_COLORS = ['#5b7cd1', '#85ce61', '#e6a23c', '#f56c6c', '#5bbfd4', '#909399']

const router = useRouter()
const loading = ref(false)
const data = ref<OverviewData | null>(null)

const ORDER_STATUS_LABELS: Record<string, string> = {
  pending: '待处理', partial: '部分', completed: '已完成', cancelled: '已取消',
}

function orderStatusLabel(status: string) {
  return ORDER_STATUS_LABELS[status] ?? status
}

function orderTagType(status: string) {
  if (status === 'partial') return 'warning'
  if (status === 'completed') return 'success'
  if (status === 'cancelled') return 'info'
  return 'primary'
}

function goTo(path: string) {
  router.push(path)
}

const statCards = computed(() => {
  const s = data.value?.summary
  const fmt = (n?: number) => (n ?? 0).toLocaleString()
  const delta = (n?: number, unit = '') => {
    const v = n ?? 0
    if (v === 0) return { trend: '与昨日持平', trendType: '' }
    return { trend: `${v > 0 ? '+' : ''}${v}${unit} 较昨日`, trendType: v > 0 ? 'up' : 'down' }
  }
  const inb = delta(s?.inboundDelta, ' 单')
  const out = delta(s?.outboundDelta, ' 单')
  const monthAdd = s?.materialThisMonth ?? 0
  return [
    { label: '物料总数', value: fmt(s?.materialTotal), unit: '', trend: `+${monthAdd} 本月新增`, trendType: 'up', color: 'blue', icon: '📦' },
    { label: '当前库存总量', value: fmt(s?.totalQty), unit: '件', trend: '覆盖全部品类', trendType: 'up', color: 'green', icon: '▦' },
    { label: '今日入库', value: fmt(s?.inboundToday), unit: '单', trend: inb.trend, trendType: inb.trendType, color: 'orange', icon: '⬇️' },
    { label: '今日出库', value: fmt(s?.outboundToday), unit: '单', trend: out.trend, trendType: out.trendType, color: 'purple', icon: '⬆️' },
  ]
})

const totalQtyLabel = computed(() => {
  const qty = data.value?.summary?.totalQty ?? 0
  return qty >= 10000 ? `${(qty / 10000).toFixed(1)}万` : qty.toLocaleString()
})

const donutSlices = computed(() => {
  const items = (data.value?.distribution ?? []).filter((d) => Number(d.quantity) > 0)
  const total = items.reduce((sum, d) => sum + Number(d.quantity), 0) || 1
  let acc = 0
  return items.map((d, idx) => {
    const start = (acc / total) * 100
    acc += Number(d.quantity)
    const end = (acc / total) * 100
    return { name: d.name, color: DONUT_COLORS[idx % DONUT_COLORS.length], start, end }
  })
})

const donutGradient = computed(() => {
  if (!donutSlices.value.length) return '#ebeef5'
  const stops = donutSlices.value.map((s) => `${s.color} ${s.start.toFixed(2)}% ${s.end.toFixed(2)}%`)
  return `conic-gradient(${stops.join(', ')})`
})

const chart = computed(() => {
  const width = 560
  const height = 300
  const padLeft = 40
  const padRight = 16
  const padTop = 16
  const padBottom = 28
  const points = data.value?.trend ?? []
  const maxVal = Math.max(10, ...points.flatMap((p) => [p.inbound, p.outbound]))
  const niceMax = Math.ceil(maxVal / 10) * 10
  const plotW = width - padLeft - padRight
  const plotH = height - padTop - padBottom
  const x = (i: number) => padLeft + (points.length <= 1 ? plotW / 2 : (i / (points.length - 1)) * plotW)
  const y = (v: number) => padTop + plotH - (v / niceMax) * plotH

  const mapped = points.map((p, i) => ({ date: p.date, x: x(i), inboundY: y(p.inbound), outboundY: y(p.outbound) }))
  const line = (key: 'inboundY' | 'outboundY') => mapped.map((p) => `${p.x.toFixed(1)},${p[key].toFixed(1)}`).join(' ')
  const area = (key: 'inboundY' | 'outboundY') => {
    if (!mapped.length) return ''
    const base = `${(padTop + plotH).toFixed(1)}`
    return `${mapped[0].x.toFixed(1)},${base} ${line(key)} ${mapped[mapped.length - 1].x.toFixed(1)},${base}`
  }
  const gridCount = 6
  const gridLines = Array.from({ length: gridCount + 1 }, (_, i) => {
    const value = Math.round((niceMax / gridCount) * (gridCount - i))
    return { value, y: padTop + (plotH / gridCount) * i }
  })

  return {
    width, height, padLeft, padRight,
    points: mapped,
    inboundLine: line('inboundY'),
    outboundLine: line('outboundY'),
    inboundArea: area('inboundY'),
    outboundArea: area('outboundY'),
    gridLines,
  }
})

async function loadData() {
  loading.value = true
  try {
    const res = await getOverview()
    if (res.code === 0) {
      data.value = res.data
    } else {
      ElMessage.error(res.message || '加载库存概览失败')
    }
  } catch {
    ElMessage.error('加载库存概览失败，请检查服务状态')
  } finally {
    loading.value = false
  }
}

function handleExport() {
  if (!data.value) {
    ElMessage.warning('暂无可导出的数据')
    return
  }
  const s = data.value.summary
  const lines = [
    '指标,数值',
    `物料总数,${s.materialTotal}`,
    `当前库存总量,${s.totalQty}`,
    `今日入库,${s.inboundToday}`,
    `今日出库,${s.outboundToday}`,
    '',
    '库存分布,数量',
    ...data.value.distribution.map((d) => `${d.name},${d.quantity}`),
    '',
    '日期,入库,出库',
    ...data.value.trend.map((t) => `${t.date},${t.inbound},${t.outbound}`),
  ]
  const blob = new Blob(['﻿' + lines.join('\n')], { type: 'text/csv;charset=utf-8;' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = '库存概览.csv'
  a.click()
  URL.revokeObjectURL(url)
  ElMessage.success('导出成功')
}

onMounted(loadData)
</script>

<style scoped>
.stat-unit { font-size: 14px; font-weight: 500; color: var(--text-secondary); }
.ov-legend { flex-wrap: wrap; }
.ov-line-chart { height: 300px; padding: 8px 4px 0; }
.ov-line-chart svg { width: 100%; height: 100%; }
.ov-grid { stroke: #ebeef5; stroke-width: 1; }
.ov-axis-label { fill: #909399; font-size: 11px; }
.ov-line { fill: none; stroke-width: 2.5; stroke-linejoin: round; stroke-linecap: round; }
.ov-line.inbound { stroke: #409eff; }
.ov-line.outbound { stroke: #67c23a; }
.ov-area { stroke: none; }
.ov-area.inbound { fill: rgba(64, 158, 255, .12); }
.ov-area.outbound { fill: rgba(103, 194, 58, .12); }
.ov-dot { stroke: #fff; stroke-width: 2; }
.ov-dot.inbound { fill: #409eff; }
.ov-dot.outbound { fill: #67c23a; }
</style>
