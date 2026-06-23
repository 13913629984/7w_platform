<template>
  <section v-loading="loading">
    <div class="sub-page-header">
      <div>
        <h2>工作台</h2>
        <p class="page-desc">跟踪客户、线索、商机与销售活动。</p>
      </div>
      <div class="header-actions">
        <el-button plain :icon="Download" @click="handleExport">导出</el-button>
        <el-button plain :icon="Refresh" @click="loadData">刷新</el-button>
      </div>
    </div>

    <div class="stats-row">
      <div class="stat-card blue">
        <div class="stat-header"><span class="stat-title">客户总数</span><div class="stat-icon">👥</div></div>
        <div class="stat-value">{{ stats.customerTotal }}</div>
        <div class="stat-footer">全部客户</div>
      </div>
      <div class="stat-card green">
        <div class="stat-header"><span class="stat-title">本月新增</span><div class="stat-icon">➕</div></div>
        <div class="stat-value">{{ stats.monthAddedCustomers }}</div>
        <div class="stat-footer">新客户</div>
      </div>
      <div class="stat-card orange">
        <div class="stat-header"><span class="stat-title">待跟进商机</span><div class="stat-icon">🔥</div></div>
        <div class="stat-value">{{ stats.pendingDeals }}</div>
        <div class="stat-footer">商机数量</div>
      </div>
      <div class="stat-card purple">
        <div class="stat-header"><span class="stat-title">本月成交</span><div class="stat-icon">📈</div></div>
        <div class="stat-value">¥{{ stats.monthDealAmount }}万</div>
        <div class="stat-footer">累计成交</div>
      </div>
    </div>

    <div class="charts-row">
      <div class="panel-card">
        <div class="panel-title"><i class="title-bar"></i>销售趋势</div>
        <div class="chart-box">
          <div v-if="salesTrend.length" class="bar-chart">
            <div v-for="point in salesTrend" :key="point.month" class="bar-col">
              <div class="bar-value">{{ point.amount }}</div>
              <div class="bar" :style="{ height: barHeight(point.amount) + '%' }"></div>
              <div class="bar-label">{{ point.month }}</div>
            </div>
          </div>
          <div v-else class="chart-placeholder">
            <div class="chart-ph-icon">📈</div>
            <div class="chart-ph-text">销售趋势图表</div>
          </div>
        </div>
      </div>
      <div class="panel-card">
        <div class="panel-title"><i class="title-bar"></i>客户分布</div>
        <div class="chart-box">
          <div v-if="customerDistribution.length" class="dist-chart">
            <div v-for="(item, idx) in customerDistribution" :key="item.level" class="dist-row">
              <span class="dist-label">{{ item.level }}</span>
              <div class="dist-track">
                <div class="dist-fill" :style="{ width: distWidth(item.count) + '%', background: distColor(idx) }"></div>
              </div>
              <span class="dist-count">{{ item.count }}</span>
            </div>
          </div>
          <div v-else class="chart-placeholder">
            <div class="chart-ph-icon">🌐</div>
            <div class="chart-ph-text">客户分布图表</div>
          </div>
        </div>
      </div>
    </div>

    <div class="lists-row">
      <div class="panel-card">
        <div class="panel-title"><i class="title-bar"></i>待跟进线索</div>
        <div class="list-box">
          <div v-if="!leads.length" class="empty-tip">暂无待跟进线索</div>
          <div v-for="lead in leads" :key="lead.id" class="lead-item">
            <div class="lead-info">
              <div class="lead-name">{{ lead.name }}</div>
              <div class="lead-meta">{{ lead.source }} · {{ lead.followAt }}</div>
            </div>
            <el-button type="primary" size="small" @click="handleFollow(lead)">跟进</el-button>
          </div>
        </div>
      </div>
      <div class="panel-card">
        <div class="panel-title"><i class="title-bar"></i>即将到期活动</div>
        <div class="list-box">
          <div v-if="!activities.length" class="empty-tip">暂无即将到期活动</div>
          <div v-for="act in activities" :key="act.id" class="act-item">
            <div class="act-title">{{ act.title }}</div>
            <div class="act-meta">📅 {{ act.activityAt }} · {{ act.customerName }}</div>
          </div>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Download, Refresh } from '@element-plus/icons-vue'
import {
  getWorkspaceOverview,
  type CustomerDistributionPoint,
  type SalesTrendPoint,
  type WorkspaceActivity,
  type WorkspaceLead,
} from '@/api/crm/workspace'

const loading = ref(false)
const stats = reactive({ customerTotal: 0, monthAddedCustomers: 0, pendingDeals: 0, monthDealAmount: '0' })
const leads = ref<WorkspaceLead[]>([])
const activities = ref<WorkspaceActivity[]>([])
const salesTrend = ref<SalesTrendPoint[]>([])
const customerDistribution = ref<CustomerDistributionPoint[]>([])

const maxTrend = computed(() => Math.max(1, ...salesTrend.value.map((p) => Number(p.amount) || 0)))
const maxDist = computed(() => Math.max(1, ...customerDistribution.value.map((p) => p.count || 0)))

function barHeight(amount: string) {
  const v = Number(amount) || 0
  return Math.round((v / maxTrend.value) * 100)
}

function distWidth(count: number) {
  return Math.round((count / maxDist.value) * 100)
}

const DIST_COLORS = ['#409eff', '#67c23a', '#e6a23c', '#f56c6c', '#909399']
function distColor(idx: number) {
  return DIST_COLORS[idx % DIST_COLORS.length]
}

async function loadData() {
  loading.value = true
  try {
    const res = await getWorkspaceOverview()
    if (res.code !== 0) {
      ElMessage.error(res.message || '加载工作台失败')
      return
    }
    const data = res.data
    if (data.stats) {
      stats.customerTotal = data.stats.customerTotal ?? 0
      stats.monthAddedCustomers = data.stats.monthAddedCustomers ?? 0
      stats.pendingDeals = data.stats.pendingDeals ?? 0
      stats.monthDealAmount = data.stats.monthDealAmount ?? '0'
    }
    leads.value = data.leads || []
    activities.value = data.activities || []
    salesTrend.value = data.salesTrend || []
    customerDistribution.value = data.customerDistribution || []
  } catch (error: any) {
    ElMessage.error(error?.message || '加载工作台失败')
  } finally {
    loading.value = false
  }
}

function handleFollow(lead: WorkspaceLead) {
  ElMessage.success(`已记录对「${lead.name}」的跟进`)
}

function handleExport() {
  const lines = [
    '指标,数值',
    `客户总数,${stats.customerTotal}`,
    `本月新增,${stats.monthAddedCustomers}`,
    `待跟进商机,${stats.pendingDeals}`,
    `本月成交(万),${stats.monthDealAmount}`,
  ]
  const blob = new Blob(['﻿' + lines.join('\n')], { type: 'text/csv;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = 'CRM工作台概览.csv'
  link.click()
  URL.revokeObjectURL(url)
  ElMessage.success('已导出工作台概览')
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.sub-page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 20px;
}
.header-actions { display: flex; align-items: center; gap: 8px; flex-shrink: 0; }

.stats-row {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 20px;
  margin-bottom: 20px;
}
.stat-card {
  background: #fff;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, .04);
  position: relative;
  overflow: hidden;
}
.stat-card::before { content: ''; position: absolute; top: 0; left: 0; right: 0; height: 3px; }
.stat-card.blue::before { background: linear-gradient(90deg, #409eff, #66b1ff); }
.stat-card.green::before { background: linear-gradient(90deg, #67c23a, #85ce61); }
.stat-card.orange::before { background: linear-gradient(90deg, #e6a23c, #f0c78a); }
.stat-card.purple::before { background: linear-gradient(90deg, #764ba2, #667eea); }
.stat-header { display: flex; align-items: center; justify-content: space-between; gap: 12px; margin-bottom: 12px; }
.stat-title { font-size: 14px; color: #909399; min-width: 0; flex: 1; }
.stat-icon {
  width: 40px; height: 40px; border-radius: 10px;
  display: flex; align-items: center; justify-content: center;
  font-size: 20px; flex-shrink: 0;
}
.stat-card.blue .stat-icon { background: #ecf5ff; }
.stat-card.green .stat-icon { background: #f0f9eb; }
.stat-card.orange .stat-icon { background: #fdf6ec; }
.stat-card.purple .stat-icon { background: #f4f0ff; }
.stat-value { font-size: 28px; font-weight: 700; color: #303133; margin-bottom: 8px; }
.stat-footer { font-size: 12px; color: #909399; }

.charts-row, .lists-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
  margin-bottom: 20px;
}
.panel-card {
  background: #fff;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, .04);
  min-width: 0;
}
.panel-title {
  display: flex; align-items: center; gap: 8px;
  font-size: 15px; font-weight: 600; color: #303133; margin-bottom: 16px;
}
.title-bar { width: 4px; height: 16px; border-radius: 2px; background: #409eff; display: inline-block; }

.chart-box { height: 240px; background: #fafafa; border-radius: 8px; padding: 16px; }
.chart-placeholder {
  height: 100%;
  display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 12px;
}
.chart-ph-icon { font-size: 44px; opacity: .55; }
.chart-ph-text { font-size: 14px; color: #c0c4cc; }

.bar-chart { height: 100%; display: flex; align-items: flex-end; justify-content: space-around; gap: 10px; }
.bar-col { flex: 1; display: flex; flex-direction: column; align-items: center; justify-content: flex-end; height: 100%; gap: 6px; }
.bar-value { font-size: 12px; color: #606266; }
.bar {
  width: 60%; max-width: 36px; min-height: 4px;
  background: linear-gradient(180deg, #66b1ff, #409eff);
  border-radius: 4px 4px 0 0; transition: height .3s;
}
.bar-label { font-size: 12px; color: #909399; }

.dist-chart { height: 100%; display: flex; flex-direction: column; justify-content: center; gap: 16px; }
.dist-row { display: flex; align-items: center; gap: 12px; }
.dist-label { width: 40px; font-size: 13px; color: #606266; flex-shrink: 0; }
.dist-track { flex: 1; height: 14px; background: #ebeef5; border-radius: 7px; overflow: hidden; }
.dist-fill { height: 100%; border-radius: 7px; transition: width .3s; }
.dist-count { width: 30px; text-align: right; font-size: 13px; color: #303133; font-weight: 600; }

.list-box { max-height: 280px; overflow-y: auto; }
.empty-tip { text-align: center; color: #c0c4cc; padding: 40px 0; font-size: 14px; }
.lead-item {
  display: flex; align-items: center; justify-content: space-between; gap: 12px;
  padding: 12px 4px; border-bottom: 1px solid #f0f0f0;
}
.lead-item:last-child { border-bottom: none; }
.lead-info { min-width: 0; }
.lead-name { font-size: 14px; font-weight: 600; color: #303133; margin-bottom: 4px; }
.lead-meta { font-size: 12px; color: #909399; }
.act-item { padding: 12px 4px; border-bottom: 1px solid #f0f0f0; }
.act-item:last-child { border-bottom: none; }
.act-title { font-size: 14px; font-weight: 600; color: #303133; margin-bottom: 4px; }
.act-meta { font-size: 12px; color: #909399; }

@media (max-width: 900px) {
  .charts-row, .lists-row { grid-template-columns: 1fr; }
}
@media (max-width: 768px) {
  .stats-row { grid-template-columns: 1fr; }
}
</style>
