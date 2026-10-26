<template>
  <section>
    <div class="sub-page-header">
      <div>
        <h2>预算管理</h2>
        <p class="page-desc">维护预算编制、执行分析和超预算预警。</p>
      </div>
      <el-space>
        <el-button size="small" plain @click="handleExport">导出</el-button>
        <el-button size="small" type="primary" @click="handleCreate">新建预算</el-button>
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
        <div class="card-header"><div class="card-title">各部门预算执行进度</div></div>
        <div class="bdg-chart">
          <svg :viewBox="`0 0 ${progress.width} ${progress.height}`" preserveAspectRatio="none" role="img" aria-label="各部门预算执行进度">
            <line v-for="gl in progress.gridLines" :key="'g' + gl.value" :x1="progress.padLeft" :y1="gl.y" :x2="progress.width - progress.padRight" :y2="gl.y" class="bdg-grid" />
            <text v-for="gl in progress.gridLines" :key="'t' + gl.value" :x="progress.padLeft - 6" :y="gl.y + 4" text-anchor="end" class="bdg-axis-label">{{ gl.label }}</text>
            <g v-for="(g, idx) in progress.groups" :key="'pg' + idx">
              <rect :x="g.execX" :y="g.execY" :width="progress.barWidth" :height="g.execH" class="bdg-bar exec" />
              <rect :x="g.remainX" :y="g.remainY" :width="progress.barWidth" :height="g.remainH" class="bdg-bar remain" />
              <text :x="g.labelX" :y="progress.height - 6" text-anchor="middle" class="bdg-axis-label">{{ g.name }}</text>
            </g>
          </svg>
        </div>
        <div class="chart-legend">
          <span><i class="legend-blue"></i>已执行</span>
          <span><i class="legend-green"></i>剩余</span>
        </div>
      </div>

      <div class="card">
        <div class="card-header">
          <div class="card-title">预算 vs 实际支出</div>
          <el-select v-model="month" size="small" style="width: 90px">
            <el-option v-for="m in monthOptions" :key="m" :label="m" :value="m" />
          </el-select>
        </div>
        <div class="bdg-chart">
          <svg :viewBox="`0 0 ${compare.width} ${compare.height}`" preserveAspectRatio="none" role="img" aria-label="预算 vs 实际支出">
            <line v-for="gl in compare.gridLines" :key="'cg' + gl.value" :x1="compare.padLeft" :y1="gl.y" :x2="compare.width - compare.padRight" :y2="gl.y" class="bdg-grid" />
            <text v-for="gl in compare.gridLines" :key="'ct' + gl.value" :x="compare.padLeft - 6" :y="gl.y + 4" text-anchor="end" class="bdg-axis-label">{{ gl.label }}</text>
            <g v-for="(g, idx) in compare.groups" :key="'cg' + idx">
              <rect :x="g.budgetX" :y="g.budgetY" :width="compare.barWidth" :height="g.budgetH" class="bdg-bar budget" />
              <rect :x="g.actualX" :y="g.actualY" :width="compare.barWidth" :height="g.actualH" class="bdg-bar actual" />
              <text :x="g.labelX" :y="compare.height - 6" text-anchor="middle" class="bdg-axis-label">{{ g.name }}</text>
            </g>
          </svg>
        </div>
        <div class="chart-legend">
          <span><i class="legend-gray"></i>预算</span>
          <span><i class="legend-blue"></i>实际</span>
        </div>
      </div>
    </div>

    <div class="card">
      <div class="card-header expense-header">
        <div class="card-title">📋 预算明细</div>
        <el-select v-model="year" size="small" style="width: 110px">
          <el-option v-for="y in yearOptions" :key="y" :label="y" :value="y" />
        </el-select>
      </div>

      <el-table :data="budgets" stripe style="width: 100%">
        <el-table-column prop="dept" label="部门" width="120" />
        <el-table-column prop="item" label="预算项目" min-width="140" />
        <el-table-column label="年度预算" width="120" align="right">
          <template #default="{ row }">{{ formatWan(row.budget) }}</template>
        </el-table-column>
        <el-table-column label="已执行" width="120" align="right">
          <template #default="{ row }">{{ formatWan(row.executed) }}</template>
        </el-table-column>
        <el-table-column label="执行率" min-width="180">
          <template #default="{ row }">
            <div class="rate-cell">
              <el-progress :percentage="row.rate" :stroke-width="10" :color="rateColor(row.rate)" :show-text="false" />
              <span class="rate-text">{{ row.rate }}%</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="剩余预算" width="120" align="right">
          <template #default="{ row }">{{ formatWan(row.budget - row.executed) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.rate)" effect="light">{{ statusLabel(row.rate) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="90" align="center" fixed="right">
          <template #default="{ row }"><el-button link type="primary" @click="showDetail(row)">详情</el-button></template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="detailVisible" title="预算详情" width="520px">
      <el-descriptions v-if="detailRow" :column="2" border>
        <el-descriptions-item label="部门">{{ detailRow.dept }}</el-descriptions-item>
        <el-descriptions-item label="预算项目">{{ detailRow.item }}</el-descriptions-item>
        <el-descriptions-item label="年度预算">{{ formatWanRaw(detailRow.budget) }}</el-descriptions-item>
        <el-descriptions-item label="已执行">{{ formatWanRaw(detailRow.executed) }}</el-descriptions-item>
        <el-descriptions-item label="执行率">{{ detailRow.rate }}%</el-descriptions-item>
        <el-descriptions-item label="剩余预算">{{ formatWanRaw(detailRow.budget - detailRow.executed) }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ statusLabel(detailRow.rate) }}</el-descriptions-item>
        <el-descriptions-item label="周期">{{ detailRow.year }} 年 {{ detailRow.month }} 月</el-descriptions-item>
      </el-descriptions>
    </el-dialog>

    <el-dialog v-model="createVisible" title="新建预算" width="560px" :close-on-click-modal="false">
      <el-form ref="createFormRef" :model="createForm" :rules="createRules" label-width="96px">
        <el-form-item label="部门" prop="dept">
          <el-input v-model="createForm.dept" maxlength="60" placeholder="如：销售部" />
        </el-form-item>
        <el-form-item label="预算项目">
          <el-input v-model="createForm.item" maxlength="120" placeholder="留空则自动生成「部门 + 预算」" />
        </el-form-item>
        <el-form-item label="预算金额" prop="budget">
          <el-input-number v-model="createForm.budget" :min="0" :step="10" :precision="2" controls-position="right" style="width: 100%" />
          <div class="form-tip">单位：万元（与页面展示保持一致）</div>
        </el-form-item>
        <el-form-item label="已执行">
          <el-input-number v-model="createForm.executed" :min="0" :step="10" :precision="2" controls-position="right" style="width: 100%" />
        </el-form-item>
        <el-form-item label="年度">
          <el-input-number v-model="createForm.year" :min="2000" :max="2100" :step="1" controls-position="right" style="width: 100%" />
        </el-form-item>
        <el-form-item label="月份">
          <el-input-number v-model="createForm.month" :min="1" :max="12" :step="1" controls-position="right" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="createSubmitting" @click="submitCreate">确定创建</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { downloadCsvFromRows } from '@/utils/export'
import {
  listBudgets,
  budgetStats,
  budgetCharts,
  createBudget,
  type Budget,
  type BudgetChartItem,
  type BudgetCompareItem,
} from '@/api/fin/budget'

const month = ref('5月')
const year = ref('2026年')
const monthOptions = ['1月', '2月', '3月', '4月', '5月', '6月']
const yearOptions = ['2026年', '2025年', '2024年']

const stats = ref({ total: 0, executed: 0, balance: 0, rate: 0, overBudget: 0 })
const progressData = ref<BudgetChartItem[]>([])
const compareData = ref<BudgetCompareItem[]>([])
const budgets = ref<Budget[]>([])

const statCards = computed(() => [
  { label: '年度预算总额', value: formatWanRaw(stats.value.total), trend: `执行率 ${stats.value.rate}%`, trendType: 'up', color: 'green', icon: '💰' },
  { label: '已执行预算', value: formatWanRaw(stats.value.executed), trend: '进度正常', trendType: '', color: 'blue', icon: '📈' },
  { label: '预算余额', value: formatWanRaw(stats.value.balance), trend: `剩余 ${(100 - Number(stats.value.rate)).toFixed(1)}%`, trendType: '', color: 'orange', icon: '💳' },
  { label: '超预算预警', value: String(stats.value.overBudget), trend: '需关注', trendType: 'down', color: 'red', icon: '⚠️' },
])

async function loadAll() {
  try {
    const [list, st, charts] = await Promise.all([listBudgets(), budgetStats(), budgetCharts()])
    budgets.value = list
    stats.value = st
    progressData.value = charts.progress
    compareData.value = charts.compare
  } catch (e: any) {
    ElMessage.error(e.message || '加载失败')
  }
}

onMounted(loadAll)

function formatWanRaw(value: number) {
  return `¥${Number(value || 0).toLocaleString()}万`
}

function buildGroupedChart<T extends { name: string }>(
  items: T[],
  series: ((d: T) => number)[],
  unitLabel: (v: number) => string,
) {
  const width = 480
  const height = 280
  const padLeft = 48
  const padRight = 12
  const padTop = 16
  const padBottom = 28
  const plotW = width - padLeft - padRight
  const plotH = height - padTop - padBottom
  const allVals: number[] = []
  items.forEach((d) => series.forEach((s) => allVals.push(s(d))))
  const maxVal = Math.max(1, ...allVals)
  const niceMax = roundUpNice(maxVal)
  const slot = plotW / items.length
  const barWidth = Math.min(22, (slot * 0.6) / series.length)
  const gap = 4
  const groupW = barWidth * series.length + gap * (series.length - 1)
  const y = (v: number) => padTop + plotH - (v / niceMax) * plotH

  const groups = items.map((d, i) => {
    const center = padLeft + slot * i + slot / 2
    const startX = center - groupW / 2
    const vals = series.map((s) => s(d))
    return { d, center, startX, vals, i }
  })

  const gridCount = 5
  const gridLines = Array.from({ length: gridCount + 1 }, (_, i) => {
    const value = (niceMax / gridCount) * (gridCount - i)
    return { value, y: padTop + (plotH / gridCount) * i, label: unitLabel(value) }
  })

  return { width, height, padLeft, padRight, plotH, padTop, barWidth, gap, y, groups, gridLines, niceMax }
}

function roundUpNice(v: number) {
  const mag = Math.pow(10, Math.floor(Math.log10(v)))
  const norm = v / mag
  let nice = 1
  if (norm <= 1) nice = 1
  else if (norm <= 2) nice = 2
  else if (norm <= 5) nice = 5
  else nice = 10
  return nice * mag
}

const progress = computed(() => {
  const c = buildGroupedChart(progressData.value, [(d) => d.executed, (d) => d.remain], (v) => `${Math.round(v)}万`)
  const groups = c.groups.map((g) => ({
    name: g.d.name,
    labelX: g.center,
    execX: g.startX,
    execH: c.padTop + c.plotH - c.y(g.vals[0]),
    execY: c.y(g.vals[0]),
    remainX: g.startX + c.barWidth + c.gap,
    remainH: c.padTop + c.plotH - c.y(g.vals[1]),
    remainY: c.y(g.vals[1]),
  }))
  return { ...c, groups }
})

const compare = computed(() => {
  const c = buildGroupedChart(compareData.value, [(d) => d.budget, (d) => d.actual], (v) => `${Math.round(v)}万`)
  const groups = c.groups.map((g) => ({
    name: g.d.name,
    labelX: g.center,
    budgetX: g.startX,
    budgetH: c.padTop + c.plotH - c.y(g.vals[0]),
    budgetY: c.y(g.vals[0]),
    actualX: g.startX + c.barWidth + c.gap,
    actualH: c.padTop + c.plotH - c.y(g.vals[1]),
    actualY: c.y(g.vals[1]),
  }))
  return { ...c, groups }
})

function formatWan(value: number) {
  return `¥${value}万`
}

/* ---------------- 预算详情弹窗 ---------------- */
const detailVisible = ref(false)
const detailRow = ref<Budget | null>(null)

function showDetail(row: Budget) {
  detailRow.value = row
  detailVisible.value = true
}

function rateColor(rate: number) {
  if (rate >= 90) return '#f56c6c'
  if (rate >= 70) return '#e6a23c'
  return '#409eff'
}

function statusLabel(rate: number) {
  if (rate >= 90) return '超支'
  if (rate >= 70) return '接近超支'
  return '正常'
}

function statusTagType(rate: number) {
  if (rate >= 90) return 'danger'
  if (rate >= 70) return 'warning'
  return 'success'
}

function handleExport() {
  if (!budgets.value.length) {
    ElMessage.warning('暂无可导出的预算数据')
    return
  }
  downloadCsvFromRows('预算明细', [
    { label: '部门', value: (r) => r.dept },
    { label: '预算项目', value: (r) => r.item },
    { label: '年度预算(万)', value: (r) => Number(r.budget || 0).toFixed(2) },
    { label: '已执行(万)', value: (r) => Number(r.executed || 0).toFixed(2) },
    { label: '执行率(%)', value: (r) => Number(r.rate || 0).toFixed(2) },
    { label: '剩余(万)', value: (r) => (Number(r.budget || 0) - Number(r.executed || 0)).toFixed(2) },
    { label: '状态', value: (r) => statusLabel(Number(r.rate || 0)) },
    { label: '年度', value: (r) => r.year ?? '' },
    { label: '月份', value: (r) => r.month ?? '' },
  ], budgets.value)
  ElMessage.success('已导出当前预算数据')
}

/* ---------------- 新建预算 ---------------- */
const createVisible = ref(false)
const createFormRef = ref<FormInstance>()
const createSubmitting = ref(false)
const createForm = reactive({
  dept: '',
  item: '',
  budget: 0,
  executed: 0,
  year: 2026,
  month: new Date().getMonth() + 1,
})
const createRules: FormRules = {
  dept: [{ required: true, message: '请填写部门', trigger: 'blur' }],
  budget: [{ required: true, message: '请填写预算金额', trigger: 'blur' }],
}

function handleCreate() {
  Object.assign(createForm, {
    dept: '',
    item: '',
    budget: 0,
    executed: 0,
    year: 2026,
    month: new Date().getMonth() + 1,
  })
  createVisible.value = true
}

async function submitCreate() {
  if (!createFormRef.value) return
  const valid = await createFormRef.value.validate().catch(() => false)
  if (!valid) return
  if (!(Number(createForm.budget) > 0)) {
    ElMessage.warning('预算金额必须大于 0')
    return
  }
  createSubmitting.value = true
  try {
    await createBudget({
      dept: createForm.dept.trim(),
      item: createForm.item.trim() || `${createForm.dept.trim()}预算`,
      budget: Number(createForm.budget),
      executed: Number(createForm.executed || 0),
      year: Number(createForm.year),
      month: Number(createForm.month),
    })
    ElMessage.success('预算已创建')
    createVisible.value = false
    await loadAll()
  } catch (e: any) {
    ElMessage.error(e.message || '创建失败')
  } finally {
    createSubmitting.value = false
  }
}
</script>

<style scoped>
.bdg-chart { height: 280px; padding: 8px 4px 0; }
.bdg-chart svg { width: 100%; height: 100%; }
.bdg-grid { stroke: #ebeef5; stroke-width: 1; }
.bdg-axis-label { fill: var(--pw-text-secondary); font-size: 11px; }
.bdg-bar.exec, .bdg-bar.actual { fill: #409eff; }
.bdg-bar.remain { fill: #67c23a; }
.bdg-bar.budget { fill: var(--pw-text-secondary); }
.legend-gray { background: #909399; }
.expense-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 16px; }
.rate-cell { display: flex; align-items: center; gap: 10px; }
.rate-cell :deep(.el-progress) { flex: 1; }
.rate-text { font-size: 13px; color: var(--text-secondary); white-space: nowrap; }
.form-tip { font-size: 12px; color: var(--el-text-color-secondary); line-height: 1.6; }
</style>
