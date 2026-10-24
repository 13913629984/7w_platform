<template>
  <section>
    <div class="sub-page-header">
      <div>
        <h2>电测工作台</h2>
        <p class="page-desc">汇总来货登记、测试执行、报告审核与测试合格率。</p>
      </div>
      <el-space>
        <el-button plain :icon="Refresh" @click="loadData">刷新</el-button>
        <el-button type="primary" :icon="Document" @click="goTo('/ets/reports')">测试报告</el-button>
      </el-space>
    </div>

    <div class="stats-row">
      <div class="stat-card blue">
        <div class="stat-header"><span class="stat-title">本月来货</span><div class="stat-icon">🧾</div></div>
        <div class="stat-value">{{ overview.monthRegister }}</div>
        <div class="stat-footer">本月新增来货登记批次</div>
      </div>
      <div class="stat-card orange">
        <div class="stat-header"><span class="stat-title">待测试</span><div class="stat-icon">⏳</div></div>
        <div class="stat-value">{{ overview.pendingTest }}</div>
        <div class="stat-footer">待收样/已收样/测试中</div>
      </div>
      <div class="stat-card green">
        <div class="stat-header"><span class="stat-title">今日完成</span><div class="stat-icon">✅</div></div>
        <div class="stat-value">{{ overview.todayTested }}</div>
        <div class="stat-footer">今日已完成测试记录</div>
      </div>
      <div class="stat-card purple">
        <div class="stat-header"><span class="stat-title">合格率</span><div class="stat-icon">📈</div></div>
        <div class="stat-value">{{ passRateLabel }}</div>
        <div class="stat-footer">已判定记录的合格占比</div>
      </div>
      <div class="stat-card red">
        <div class="stat-header"><span class="stat-title">不合格数</span><div class="stat-icon">⚠️</div></div>
        <div class="stat-value">{{ overview.failCount }}</div>
        <div class="stat-footer">累计判定不合格记录</div>
      </div>
      <div class="stat-card blue">
        <div class="stat-header"><span class="stat-title">报告待审</span><div class="stat-icon">📄</div></div>
        <div class="stat-value">{{ overview.pendingReport }}</div>
        <div class="stat-footer">草稿 + 待审核报告</div>
      </div>
    </div>

    <div class="dashboard-grid">
      <div class="card">
        <div class="card-header">
          <div class="card-title">近 7 日测试趋势</div>
          <el-button link type="primary" @click="goTo('/ets/tests')">查看测试记录</el-button>
        </div>
        <div class="ets-trend-chart">
          <div class="ets-y-axis">
            <span v-for="tick in trendTicks" :key="tick">{{ tick }}</span>
          </div>
          <div class="ets-trend-plot">
            <div v-for="item in trend" :key="item.date" class="ets-trend-col">
              <span class="ets-trend-value">{{ item.count }}</span>
              <span class="ets-trend-bar" :style="{ height: barHeight(item.count) }"></span>
              <em>{{ item.label }}</em>
            </div>
          </div>
        </div>
        <div class="chart-legend"><span><i class="legend-blue"></i>每日测试数</span></div>
      </div>

      <div class="card">
        <div class="card-header"><div class="card-title">测试结果分布</div></div>
        <div class="donut-wrap">
          <div class="donut-chart" :style="{ background: resultGradient }"></div>
          <div class="donut-center"><strong>{{ resultTotal }}</strong><span>测试记录</span></div>
        </div>
        <div class="chart-legend ets-legend">
          <span v-for="item in resultRows" :key="item.name">
            <i :style="{ background: item.color }"></i>{{ item.label }} {{ item.value }}
          </span>
        </div>
      </div>
    </div>

    <div class="card">
      <div class="card-header"><div class="card-title">测试类别分布</div></div>
      <div class="ets-cat-list">
        <div v-for="item in categoryRows" :key="item.name" class="ets-cat-row">
          <span class="ets-cat-name">{{ item.label }}</span>
          <div class="ets-cat-track">
            <span class="ets-cat-fill" :style="{ width: item.percent + '%', background: item.color }"></span>
          </div>
          <span class="ets-cat-value">{{ item.value }}</span>
        </div>
        <div v-if="!categoryRows.length" class="ets-empty">暂无测试类别数据</div>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Document, Refresh } from '@element-plus/icons-vue'
import { getEtsOverview, type DistributionItem, type EtsOverview, type TrendItem } from '@/api/ets/workspace'

const router = useRouter()
const loading = ref(false)
const overview = ref<EtsOverview>({
  monthRegister: 0,
  pendingTest: 0,
  todayTested: 0,
  passRate: 0,
  failCount: 0,
  pendingReport: 0,
  testTrend: [],
  resultDistribution: [],
  categoryDistribution: [],
})

const RESULT_DICT: Record<string, { label: string; color: string }> = {
  pass: { label: '合格', color: '#67c23a' },
  fail: { label: '不合格', color: '#f56c6c' },
  pending: { label: '待测试', color: '#e6a23c' },
}
const CATEGORY_DICT: Record<string, string> = {
  appearance: '外观',
  insulation: '绝缘',
  withstand: '耐压',
  grounding: '接地',
  temperature: '温升',
  performance: '性能',
}
const CATEGORY_COLORS = ['#409eff', '#67c23a', '#e6a23c', '#764ba2', '#f56c6c', '#36cfc9']

const trend = computed<TrendItem[]>(() => overview.value.testTrend || [])
const maxTrend = computed(() => Math.max(1, ...trend.value.map((item) => Number(item.count) || 0)))
const trendTicks = computed(() => [maxTrend.value, Math.round(maxTrend.value * 0.66), Math.round(maxTrend.value * 0.33), 0])
const passRateLabel = computed(() => `${Number(overview.value.passRate ?? 0).toFixed(1)}%`)

const resultRows = computed<Array<DistributionItem & { label: string; color: string }>>(() =>
  (overview.value.resultDistribution || []).map((item) => {
    const dict = RESULT_DICT[item.name] || { label: item.name, color: '#909399' }
    return { ...item, label: dict.label, color: dict.color }
  }),
)
const resultTotal = computed(() => resultRows.value.reduce((sum, item) => sum + (Number(item.value) || 0), 0))
const resultGradient = computed(() => {
  const items = resultRows.value
  const total = resultTotal.value
  if (!total) return 'conic-gradient(#ebeef5 0 100%)'
  let start = 0
  const stops: string[] = []
  items.forEach((item) => {
    const value = Number(item.value) || 0
    const end = start + (value / total) * 100
    stops.push(`${item.color} ${start.toFixed(2)}% ${end.toFixed(2)}%`)
    start = end
  })
  return `conic-gradient(${stops.join(', ')})`
})

const categoryRows = computed(() => {
  const items = overview.value.categoryDistribution || []
  const max = Math.max(1, ...items.map((item) => Number(item.value) || 0))
  return items.map((item, index) => ({
    name: item.name,
    label: CATEGORY_DICT[item.name] || item.name,
    value: Number(item.value) || 0,
    percent: Math.round(((Number(item.value) || 0) / max) * 100),
    color: CATEGORY_COLORS[index % CATEGORY_COLORS.length],
  }))
})

function barHeight(count: number) {
  const percent = (Number(count) || 0) / maxTrend.value
  return `${Math.max(percent * 100, count === 0 ? 2 : 6)}%`
}

function goTo(path: string) {
  router.push(path)
}

async function loadData() {
  loading.value = true
  try {
    const res = await getEtsOverview()
    if (res.code !== 0) {
      ElMessage.error(res.message || '加载电测工作台失败')
      return
    }
    overview.value = {
      ...overview.value,
      ...(res.data || {}),
      testTrend: res.data?.testTrend || [],
      resultDistribution: res.data?.resultDistribution || [],
      categoryDistribution: res.data?.categoryDistribution || [],
    }
  } catch (error: any) {
    ElMessage.error(error?.message || '加载电测工作台失败')
  } finally {
    loading.value = false
  }
}

onMounted(() => loadData())
</script>

<style scoped>
.ets-trend-chart {
  height: 194px;
  display: flex;
  gap: 10px;
  padding: 8px 12px 0 0;
}
.ets-y-axis {
  width: 42px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  align-items: flex-end;
  color: #909399;
  font-size: 12px;
  padding-bottom: 22px;
}
.ets-trend-plot {
  flex: 1;
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  align-items: end;
  gap: 12px;
  border-left: 1px solid #ebeef5;
  border-bottom: 1px solid #dcdfe6;
  background: repeating-linear-gradient(to top, transparent 0, transparent 29px, #ebeef5 30px);
  padding: 0 12px;
}
.ets-trend-col {
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: flex-end;
  position: relative;
}
.ets-trend-value {
  font-size: 12px;
  color: #606266;
  margin-bottom: 4px;
}
.ets-trend-bar {
  width: 24px;
  border-radius: 4px 4px 0 0;
  background: linear-gradient(180deg, #66b1ff, #409eff);
  box-shadow: 0 6px 14px rgba(64, 158, 255, 0.18);
}
.ets-trend-col em {
  height: 22px;
  line-height: 22px;
  font-style: normal;
  color: #606266;
  font-size: 12px;
}
.ets-legend {
  flex-wrap: wrap;
}
.ets-cat-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}
.ets-cat-row {
  display: flex;
  align-items: center;
  gap: 12px;
}
.ets-cat-name {
  width: 64px;
  font-size: 13px;
  color: #606266;
  flex-shrink: 0;
}
.ets-cat-track {
  flex: 1;
  height: 12px;
  border-radius: 999px;
  background: #f2f3f5;
  overflow: hidden;
}
.ets-cat-fill {
  display: block;
  height: 100%;
  border-radius: 999px;
  transition: width 0.3s;
}
.ets-cat-value {
  width: 48px;
  text-align: right;
  font-size: 13px;
  font-weight: 600;
  color: #303133;
}
.ets-empty {
  color: #909399;
  font-size: 13px;
  padding: 12px 0;
}

/* 窄屏：7 列柱状图压缩间距，避免横向溢出 */
@media (max-width: 768px) {
  .ets-trend-chart { gap: 6px; padding: 4px 4px 0 0; }
  .ets-y-axis { width: 30px; font-size: 10px; }
  .ets-trend-plot { gap: 4px; padding: 0 4px; }
  .ets-trend-bar { width: 14px; }
  .ets-trend-value, .ets-trend-col em { font-size: 10px; }
}
@media (max-width: 420px) {
  .ets-trend-plot { gap: 2px; padding: 0 2px; }
  .ets-trend-bar { width: 11px; }
  .ets-trend-value { display: none; }
}
</style>
