<template>
  <section>
    <div class="sub-page-header">
      <div>
        <h2>维保工作台</h2>
        <p class="page-desc">汇总设备运行状态、工单处理进度、保养计划到期与备件库存预警。</p>
      </div>
      <el-space>
        <el-button plain :icon="Refresh" @click="loadData">刷新</el-button>
        <el-button type="primary" plain :icon="Tickets" @click="goTo('/maint/work-orders')">工单管理</el-button>
      </el-space>
    </div>

    <div class="stats-row">
      <div class="stat-card blue">
        <div class="stat-header"><span class="stat-title">设备总数</span><div class="stat-icon">🏭</div></div>
        <div class="stat-value">{{ overview.equipmentTotal }}</div>
        <div class="stat-footer">在册维保设备</div>
      </div>
      <div class="stat-card green">
        <div class="stat-header"><span class="stat-title">运行中</span><div class="stat-icon">✅</div></div>
        <div class="stat-value">{{ overview.runningCount }}</div>
        <div class="stat-footer">正常运行设备</div>
      </div>
      <div class="stat-card red">
        <div class="stat-header"><span class="stat-title">维修中</span><div class="stat-icon">🛠️</div></div>
        <div class="stat-value">{{ overview.repairCount }}</div>
        <div class="stat-footer">正在维修设备</div>
      </div>
      <div class="stat-card orange">
        <div class="stat-header"><span class="stat-title">待处理工单</span><div class="stat-icon">📋</div></div>
        <div class="stat-value">{{ overview.pendingOrderCount }}</div>
        <div class="stat-footer">待派单 + 已派单</div>
      </div>
      <div class="stat-card red">
        <div class="stat-header"><span class="stat-title">逾期工单</span><div class="stat-icon">⏰</div></div>
        <div class="stat-value">{{ overview.overdueOrderCount }}</div>
        <div class="stat-footer">已超计划完成时间</div>
      </div>
      <div class="stat-card green">
        <div class="stat-header"><span class="stat-title">本月完成</span><div class="stat-icon">🎯</div></div>
        <div class="stat-value">{{ overview.monthCompletedCount }}</div>
        <div class="stat-footer">本月已完工工单</div>
      </div>
      <div class="stat-card purple">
        <div class="stat-header"><span class="stat-title">计划到期</span><div class="stat-icon">📅</div></div>
        <div class="stat-value">{{ overview.planDueCount }}</div>
        <div class="stat-footer">7 天内到期保养计划</div>
      </div>
      <div class="stat-card orange">
        <div class="stat-header"><span class="stat-title">低库存备件</span><div class="stat-icon">🔩</div></div>
        <div class="stat-value">{{ overview.lowStockCount }}</div>
        <div class="stat-footer">低于安全库存备件</div>
      </div>
    </div>

    <div class="dashboard-grid">
      <div class="card">
        <div class="card-header">
          <div class="card-title">近 7 日工单趋势</div>
          <span class="maint-hint">按报修 / 完工日期统计</span>
        </div>
        <div class="maint-trend-chart" v-loading="loading">
          <div v-for="point in trendPoints" :key="point.date" class="trend-day">
            <div class="trend-bars">
              <span class="trend-bar created" :style="{ height: barHeight(point.created) }" :title="`报修 ${point.created}`"></span>
              <span class="trend-bar finished" :style="{ height: barHeight(point.finished) }" :title="`完工 ${point.finished}`"></span>
            </div>
            <em>{{ point.label }}</em>
          </div>
          <div v-if="!trendPoints.length" class="chart-empty">暂无趋势数据</div>
        </div>
        <div class="chart-legend">
          <span><i class="legend-blue"></i>报修</span>
          <span><i class="legend-green"></i>完工</span>
        </div>
      </div>

      <div class="card">
        <div class="card-header"><div class="card-title">设备状态分布</div></div>
        <div class="donut-wrap">
          <div class="donut-chart" :style="{ background: donutGradient }"></div>
          <div class="donut-center"><strong>{{ distributionTotal }}</strong><span>设备总数</span></div>
        </div>
        <div class="chart-legend">
          <span v-for="item in distribution" :key="item.status"><i :style="{ background: statusColor(item.status) }"></i>{{ item.label }} {{ item.count }}</span>
        </div>
        <div class="maint-dist-bars">
          <div v-for="item in distribution" :key="'bar-' + item.status" class="dist-row">
            <span class="dist-label">{{ item.label }}</span>
            <div class="maint-progress"><span :style="{ width: percent(item.count), background: statusColor(item.status) }"></span></div>
            <span class="dist-value">{{ item.count }}</span>
          </div>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Refresh, Tickets } from '@element-plus/icons-vue'
import {
  getMaintDistribution,
  getMaintOverview,
  getMaintTrend,
  type MaintDistributionItem,
  type MaintOverview,
  type MaintTrendPoint,
} from '@/api/maint/workspace'

const router = useRouter()

const STATUS_COLORS: Record<string, string> = {
  running: '#67c23a',
  idle: '#409eff',
  repair: '#f56c6c',
  scrapped: '#909399',
}

const loading = ref(false)
const overview = ref<MaintOverview>({
  equipmentTotal: 0,
  runningCount: 0,
  repairCount: 0,
  pendingOrderCount: 0,
  overdueOrderCount: 0,
  monthCompletedCount: 0,
  planDueCount: 0,
  lowStockCount: 0,
})
const trendPoints = ref<MaintTrendPoint[]>([])
const distribution = ref<MaintDistributionItem[]>([])

const maxTrendValue = computed(() => {
  const values = trendPoints.value.flatMap((p) => [Number(p.created) || 0, Number(p.finished) || 0])
  return Math.max(1, ...values)
})

const distributionTotal = computed(() => distribution.value.reduce((sum, item) => sum + (Number(item.count) || 0), 0))

const donutGradient = computed(() => {
  const total = distributionTotal.value
  if (!total) return 'conic-gradient(#ebeef5 0 100%)'
  let acc = 0
  const stops = distribution.value.map((item) => {
    const count = Number(item.count) || 0
    const start = (acc / total) * 100
    acc += count
    const end = (acc / total) * 100
    return `${statusColor(item.status)} ${start.toFixed(2)}% ${end.toFixed(2)}%`
  })
  return `conic-gradient(${stops.join(', ')})`
})

function statusColor(status: string) {
  return STATUS_COLORS[status] || '#909399'
}

function barHeight(value: number) {
  const ratio = (Number(value) || 0) / maxTrendValue.value
  return `${Math.max(ratio * 100, 2)}%`
}

function percent(value: number) {
  const total = distributionTotal.value || 1
  return `${((Number(value) || 0) / total) * 100}%`
}

function goTo(path: string) {
  router.push(path)
}

async function loadData() {
  loading.value = true
  try {
    const [overviewRes, trendRes, distRes] = await Promise.all([
      getMaintOverview(),
      getMaintTrend(),
      getMaintDistribution(),
    ])
    if (overviewRes.code !== 0) {
      ElMessage.error(overviewRes.message || '加载工作台数据失败')
      return
    }
    overview.value = overviewRes.data
    if (overviewRes.data.trend?.length) trendPoints.value = overviewRes.data.trend
    if (overviewRes.data.distribution?.length) distribution.value = overviewRes.data.distribution
    if (trendRes.code === 0 && trendRes.data?.length) trendPoints.value = trendRes.data
    if (distRes.code === 0 && distRes.data?.length) distribution.value = distRes.data
  } catch (error: any) {
    ElMessage.error(error?.message || '加载工作台数据失败')
  } finally {
    loading.value = false
  }
}

onMounted(() => loadData())
</script>

<style scoped>
.maint-hint { font-size: 12px; color: var(--pw-text-secondary); }
.maint-trend-chart { height: 220px; display: grid; grid-auto-flow: column; grid-auto-columns: 1fr; gap: 14px; align-items: end; padding: 18px 12px 30px; background: linear-gradient(180deg, #fafcff 0%, #f7f9fc 100%); border-radius: 10px; position: relative; }
.trend-day { height: 100%; display: flex; flex-direction: column; justify-content: flex-end; align-items: center; position: relative; }
.trend-bars { flex: 1; display: flex; align-items: flex-end; gap: 7px; width: 100%; justify-content: center; }
.trend-bar { width: 18px; border-radius: 8px 8px 0 0; display: inline-block; transition: height .3s; }
.trend-bar.created { background: linear-gradient(180deg, #66b1ff, #409eff); }
.trend-bar.finished { background: linear-gradient(180deg, #85ce61, #67c23a); }
.trend-day em { position: absolute; bottom: -24px; font-style: normal; color: var(--pw-text-secondary); font-size: 12px; }
.chart-empty { position: absolute; inset: 0; display: flex; align-items: center; justify-content: center; color: var(--pw-text-secondary); font-size: 13px; }
.maint-dist-bars { margin-top: 16px; display: flex; flex-direction: column; gap: 12px; }
.dist-row { display: flex; align-items: center; gap: 12px; font-size: 13px; color: var(--pw-text-regular); }
.dist-label { width: 62px; flex-shrink: 0; }
.dist-value { width: 36px; text-align: right; font-weight: 600; color: var(--pw-text-primary); }
.maint-progress { flex: 1; height: 8px; background: #ebeef5; border-radius: 999px; overflow: hidden; }
.maint-progress span { display: block; height: 100%; border-radius: inherit; transition: width .3s; }

/* 窄屏：7 列柱状图压缩间距与柱宽，避免横向溢出 */
@media (max-width: 768px) {
  .maint-trend-chart { gap: 6px; padding: 14px 6px 28px; }
  .trend-bars { gap: 3px; }
  .trend-bar { width: 11px; }
  .trend-day em { font-size: 10px; }
}
@media (max-width: 420px) {
  .maint-trend-chart { gap: 4px; padding: 12px 4px 26px; }
  .trend-bar { width: 9px; }
  .trend-day em { font-size: 9px; letter-spacing: -0.3px; }
}
</style>
