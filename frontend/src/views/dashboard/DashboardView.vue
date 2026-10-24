<template>
  <section v-loading="loading">
    <div class="welcome-row">
      <h2>欢迎回来，{{ auth.nickname || '管理员' }} 👋</h2>
      <p v-if="lastUpdated" class="welcome-meta">数据更新于 {{ lastUpdated }}</p>
    </div>

    <!-- 顶部指标：全部来自各子系统真实聚合接口，未取到数据时显示「—」而不是 0 -->
    <AppStatGrid>
      <AppStatCard
        label="库存物料总数"
        :value="cards.materialTotal"
        accent="blue"
        icon="📦"
        :footer="cards.wmsFooter"
      />
      <AppStatCard
        label="活跃客户数"
        :value="cards.customerTotal"
        accent="green"
        icon="👥"
        :footer="cards.crmFooter"
      />
      <AppStatCard
        label="待处理工单"
        :value="cards.pendingOrders"
        accent="orange"
        icon="🔧"
        :footer="cards.maintFooter"
      />
      <AppStatCard
        label="本月销售额"
        :value="cards.monthSales"
        accent="purple"
        icon="💰"
        footer="CRM 本月签订订单金额"
      />
    </AppStatGrid>

    <div class="section-title-row">
      <div class="section-title">子系统入口</div>
      <button v-if="needsMore" type="button" class="systems-more-btn" :aria-expanded="expanded" @click="expanded = !expanded">
        {{ expanded ? '收起' : `更多... (${hiddenCount})` }}
      </button>
    </div>
    <div ref="gridRef" class="systems-grid" :style="{ '--systems-cols': gridCols }">
      <div v-for="system in displayedSystems" :key="system.name" class="system-card" @click="router.push(system.path)">
        <div class="system-icon-box" :class="system.bg">{{ system.icon }}</div>
        <h3>{{ system.name }}</h3>
        <p>{{ system.desc }}</p>
        <div class="system-stats">
          <div v-for="stat in system.stats" :key="stat.text" class="stat-item">
            <span class="stat-num">{{ stat.num }}</span>
            <span class="stat-text">{{ stat.text }}</span>
          </div>
        </div>
      </div>
    </div>

    <div class="charts-grid dashboard-charts">
      <!-- 近 7 日出入库：WMS /wms/overview 的 trend 是真实笔数，
           Y 轴刻度按数据最大值动态计算，不再写死刻度导致柱子与数字对不上 -->
      <div class="chart-card">
        <div class="card-header">
          <div class="card-title">📊 近 7 日出入库笔数</div>
          <el-button link type="primary" @click="router.push('/wms/overview')">详情</el-button>
        </div>
        <!-- 全为 0 时不再画空坐标系（空图表会被误认为加载失败），
             改为明确文案，同时保留真实日期范围 -->
        <div v-if="hasTrendData" class="monthly-chart">
          <div class="chart-y-axis">
            <span v-for="tick in trendTicks" :key="tick">{{ tick }}</span>
          </div>
          <div class="monthly-plot" :style="{ gridTemplateColumns: `repeat(${wmsTrend.length}, 1fr)` }">
            <div v-for="item in wmsTrend" :key="item.date" class="month-group">
              <div class="bar-pair">
                <span
                  class="bar inbound"
                  :style="{ height: barHeight(item.inbound, trendMax) }"
                  :title="`${item.date} 入库 ${item.inbound} 笔`"
                ></span>
                <span
                  class="bar outbound"
                  :style="{ height: barHeight(item.outbound, trendMax) }"
                  :title="`${item.date} 出库 ${item.outbound} 笔`"
                ></span>
              </div>
              <em>{{ item.date }}</em>
            </div>
          </div>
        </div>
        <el-empty
          v-else-if="wmsTrend.length"
          :description="`近 7 日（${wmsTrend[0]?.date} 至 ${wmsTrend[wmsTrend.length - 1]?.date}）无出入库记录`"
          :image-size="90"
        />
        <el-empty v-else description="暂无出入库数据" :image-size="90" />
        <div class="chart-legend"><span><i class="legend-blue"></i>入库</span><span><i class="legend-green"></i>出库</span></div>
      </div>

      <!-- 销售漏斗：CRM /crm/deal/stage-counts 的真实阶段计数。
           漏斗形状按真实数量等比收窄，数量为 0 的阶段也保留一行（灰显），
           避免"空阶段被隐藏"造成视觉上比实际更乐观 -->
      <div class="chart-card">
        <div class="card-header">
          <div class="card-title">📈 商机阶段分布</div>
          <el-button link type="primary" @click="router.push('/crm/deals')">详情</el-button>
        </div>
        <div v-if="stageCounts.length" class="stage-funnel">
          <div v-for="(item, idx) in stageCounts" :key="item.stage" class="stage-row">
            <span class="stage-name">{{ item.stage }}</span>
            <div class="stage-track">
              <div
                class="stage-fill"
                :style="{ width: stageWidth(item.count), background: STAGE_COLORS[idx % STAGE_COLORS.length] }"
              ></div>
            </div>
            <span class="stage-count">{{ item.count }}</span>
          </div>
        </div>
        <el-empty v-else description="暂无商机数据" :image-size="90" />
      </div>

      <!-- 库存健康：WMS 预警统计（总数 / 待处理 / 高危）+ 库存分布 -->
      <div class="chart-card">
        <div class="card-header">
          <div class="card-title">📦 库存分布与预警</div>
          <el-button link type="primary" @click="router.push('/wms/warnings')">预警</el-button>
        </div>
        <!-- 只有存在真实库存数量时才画环图；全部为 0 时环图会变成一整圈灰色，
             容易被误读为「100% 的某一类」 -->
        <template v-if="hasInventoryData">
          <div class="donut-wrap">
            <div class="donut-chart" :style="{ background: donutGradient }"></div>
            <div class="donut-center">
              <strong>{{ num(totalQty) }}</strong>
              <span>库存总量</span>
            </div>
          </div>
          <div class="chart-legend dist-legend">
            <span v-for="(slice, idx) in distribution" :key="slice.name">
              <i :style="{ background: DIST_COLORS[idx % DIST_COLORS.length] }"></i>{{ slice.name }}</span>
          </div>
          <div v-if="warningStats" class="warning-line">
            预警共 {{ num(warningStats.total) }} 条 · 待处理 {{ num(warningStats.pending) }} · 高危 {{ num(warningStats.high) }}
          </div>
        </template>
        <el-empty v-else description="库存数量为 0，暂无分布数据" :image-size="90" />
      </div>

      <!-- 待办：合并四个子系统的真实待处理项，按来源标注 -->
      <div class="chart-card todo-card">
        <div class="card-header">
          <div class="card-title">📝 待办事项</div>
          <el-button link type="primary" @click="router.push('/crm/workspace')">工作台</el-button>
        </div>
        <ul v-if="todos.length" class="todo-list">
          <li v-for="todo in todos" :key="todo.key" class="todo-item">
            <span class="todo-dot" :class="todo.module"></span>
            <span class="todo-content">{{ todo.content }}</span>
            <span class="todo-time">{{ todo.time }}</span>
          </li>
        </ul>
        <el-empty v-else description="暂无待办事项" :image-size="90" />
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { getWorkspaceOverview } from '@/api/crm/workspace'
import { getOverview as getWmsOverview } from '@/api/wms/overview'
import { getWarningStats } from '@/api/wms/warning'
import { getMaintOverview } from '@/api/maint/workspace'
import { getEtsOverview } from '@/api/ets/workspace'
import { getWorkspace as getFinWorkspace } from '@/api/fin/workspace'
import request from '@/api/request'

const router = useRouter()
const auth = useAuthStore()

const loading = ref(false)
const lastUpdated = ref('')

/* ---------------- 真实数据容器 ---------------- */
/** 各子系统 overview 的局部字段（只声明首页用到的，避免与各子系统类型强耦合） */
interface CrmStatsLite {
  customerTotal?: number
  monthAddedCustomers?: number
  pendingDeals?: number
  monthDealAmount?: string
}
interface WmsSummaryLite {
  materialTotal?: number
  materialThisMonth?: number
  inboundToday?: number
}
interface MaintStatsLite {
  pendingOrderCount?: number
  overdueOrderCount?: number
  planDueCount?: number
  lowStockCount?: number
}
interface EtsStatsLite {
  pendingTest?: number
  todayTested?: number
  passRate?: number
  pendingReport?: number
}
interface FinStatsLite {
  income?: number
  expense?: number
  pendingReceipt?: number
}
interface WarningStatsLite {
  total?: number
  pending?: number
  handled?: number
  high?: number
}

const crmStats = ref<CrmStatsLite | null>(null)
const wmsSummary = ref<WmsSummaryLite | null>(null)
const wmsTrend = ref<{ date: string; inbound: number; outbound: number }[]>([])
const distribution = ref<{ name: string; quantity: number }[]>([])
const warningStats = ref<WarningStatsLite | null>(null)
const stageCounts = ref<{ stage: string; count: number }[]>([])
const maintStats = ref<MaintStatsLite | null>(null)
const etsStats = ref<EtsStatsLite | null>(null)
const finStats = ref<FinStatsLite | null>(null)
const crmLeads = ref<any[]>([])
const crmActivities = ref<any[]>([])
const sysOverview = ref<{ userCount?: number | null; roleCount?: number | null } | null>(null)

/* ---------------- 顶部指标卡 ---------------- */
/** 任意数据缺失时显示「—」，避免把「接口失败」误读成「业务为 0」 */
function num(value: unknown): string {
  if (value === null || value === undefined || value === '') return '—'
  const n = Number(value)
  if (!Number.isFinite(n)) return '—'
  return n.toLocaleString('zh-CN')
}

const cards = computed(() => {
  const w = wmsSummary.value
  const c = crmStats.value
  const m = maintStats.value
  return {
    materialTotal: w ? num(w.materialTotal) : '—',
    customerTotal: c ? num(c.customerTotal) : '—',
    pendingOrders: m ? num(m.pendingOrderCount) : '—',
    monthSales: c ? `¥${num(c.monthDealAmount)}万` : '—',
    wmsFooter: w ? `今年新增 ${num(w.materialThisMonth)} 种` : '暂无数据',
    crmFooter: c ? `本月新增 ${num(c.monthAddedCustomers)}` : '暂无数据',
    maintFooter: m ? `逾期 ${num(m.overdueOrderCount)} 单` : '暂无数据',
  }
})

/* ---------------- 图表：近 7 日趋势 ---------------- */
const trendMax = computed(() => {
  const values = wmsTrend.value.flatMap((p) => [Number(p.inbound) || 0, Number(p.outbound) || 0])
  // 全为 0 时给一个基准值，柱子高度归零而不是除零
  return Math.max(1, ...values)
})

/** 有真实出入库笔数时才画图；全为 0 走空状态文案 */
const hasTrendData = computed(() =>
  wmsTrend.value.some((p) => (Number(p.inbound) || 0) > 0 || (Number(p.outbound) || 0) > 0),
)

/** Y 轴 5 档刻度，随真实数据缩放 */
const trendTicks = computed(() => {
  const max = trendMax.value
  const ticks = [1, 0.75, 0.5, 0.25, 0].map((ratio) => Math.round(max * ratio))
  // 去重但保留降序（顶部 → 0）；数据量小时 1,1,1,0,0 这种刻度没有意义
  const unique = Array.from(new Set(ticks))
  if (unique[unique.length - 1] !== 0) unique.push(0)
  return unique.map((v) => v.toLocaleString('zh-CN'))
})

function barHeight(value: number, max: number) {
  const v = Number(value) || 0
  if (!max || v === 0) return '0%'
  // 非零值给 4% 保底，否则 1 笔和 0 笔视觉上无法区分
  return `${Math.max(4, Math.round((v / max) * 100))}%`
}

/* ---------------- 图表：商机阶段 ---------------- */
const STAGE_COLORS = ['#409eff', '#529bd2', '#e6a23c', '#f56c6c']
const stageMax = computed(() => Math.max(1, ...stageCounts.value.map((s) => Number(s.count) || 0)))

/** 宽度按占最大阶段数量的比例，避免最早阶段太细看不清 */
function stageWidth(count: number) {
  const v = Number(count) || 0
  if (!v) return '2%'
  return `${Math.max(8, Math.round((v / stageMax.value) * 100))}%`
}

/* ---------------- 图表：库存分布 ---------------- */
const DIST_COLORS = ['#409eff', '#67c23a', '#e6a23c', '#f56c6c', '#8b5cf6']

/** 环形图按真实库存数量占比生成 conic-gradient */
/** 环形图按真实库存数量占比生成 conic-gradient */
const totalQty = computed(() =>
  distribution.value.reduce((sum, it) => sum + (Number(it.quantity) || 0), 0),
)

/** 存在真实库存数量时才画环图 */
const hasInventoryData = computed(() => totalQty.value > 0)

const donutGradient = computed(() => {
  const items = distribution.value
  const total = items.reduce((sum, it) => sum + (Number(it.quantity) || 0), 0)
  if (!total) return 'conic-gradient(var(--pw-gray-300) 0 100%)'
  let acc = 0
  const stops = items.map((it, idx) => {
    const start = (acc / total) * 100
    acc += Number(it.quantity) || 0
    const end = (acc / total) * 100
    const color = DIST_COLORS[idx % DIST_COLORS.length]
    return `${color} ${start.toFixed(2)}% ${end.toFixed(2)}%`
  })
  return `conic-gradient(${stops.join(', ')})`
})

/* ---------------- 待办事项（真实） ---------------- */
const todos = computed(() => {
  const list: { key: string; module: string; content: string; time: string }[] = []

  crmLeads.value.slice(0, 3).forEach((lead: any) => {
    list.push({
      key: `lead-${lead.id}`,
      module: 'crm',
      content: `CRM 线索「${lead.name}」待跟进`,
      time: lead.followAt || '',
    })
  })

  crmActivities.value.slice(0, 2).forEach((act: any) => {
    list.push({
      key: `act-${act.id}`,
      module: 'crm',
      content: `CRM 活动「${act.title}」待处理`,
      time: act.activityAt || '',
    })
  })

  const m = maintStats.value
  if ((m?.planDueCount ?? 0) > 0) {
    list.push({
      key: 'maint-plan',
      module: 'maint',
      content: `维保 ${num(m?.planDueCount)} 个保养计划到期`,
      time: '待处理',
    })
  }
  if ((m?.lowStockCount ?? 0) > 0) {
    list.push({
      key: 'maint-spare',
      module: 'maint',
      content: `维保 ${num(m?.lowStockCount)} 项备件低于安全库存`,
      time: '预警',
    })
  }

  const e = etsStats.value
  if ((e?.pendingTest ?? 0) > 0) {
    list.push({ key: 'ets-test', module: 'ets', content: `电测 ${num(e?.pendingTest)} 个批次待测试`, time: '待处理' })
  }
  if ((e?.pendingReport ?? 0) > 0) {
    list.push({ key: 'ets-report', module: 'ets', content: `电测 ${num(e?.pendingReport)} 份报告待审批`, time: '待审批' })
  }

  const ws = warningStats.value
  if ((ws?.pending ?? 0) > 0) {
    list.push({
      key: 'wms-warning',
      module: 'wms',
      content: `WMS ${num(ws?.pending)} 条库存预警待处理`,
      time: '预警',
    })
  }

  const f = finStats.value
  if ((f?.pendingReceipt ?? 0) > 0) {
    list.push({
      key: 'fin-receipt',
      module: 'fin',
      content: `财务待收款 ¥${num(f?.pendingReceipt)}`,
      time: '待收款',
    })
  }

  return list.slice(0, 8)
})

/* ---------------- 子系统入口（统计数字改成真实值） ---------------- */
const systems = computed(() => {
  const w = wmsSummary.value
  const m = maintStats.value
  const e = etsStats.value
  const c = crmStats.value
  const f = finStats.value
  return [
    {
      icon: '📦', bg: 'bg-blue', name: 'WMS 仓储管理', permission: 'WMS', path: '/wms/overview',
      desc: '入库/出库/库存管理/库位管理/盘点，实现仓储全流程数字化',
      stats: [
        { num: w ? num(w.materialTotal) : '—', text: '物料' },
        { num: w ? num(w.inboundToday) : '—', text: '今日入库' },
        { num: warningStats.value ? num(warningStats.value.pending) : '—', text: '待处理预警' },
      ],
    },
    {
      icon: '👥', bg: 'bg-red', name: 'CRM 客户管理', permission: 'CRM', path: '/crm/workspace',
      desc: '客户档案/商机漏斗/合同管理/回款跟踪，提升销售转化率',
      stats: [
        { num: c ? num(c.pendingDeals) : '—', text: '进行中商机' },
        { num: crmLeads.value.length ? num(crmLeads.value.length) : '0', text: '待跟进线索' },
        { num: c ? num(c.monthAddedCustomers) : '—', text: '本月新客' },
      ],
    },
    {
      icon: '🔧', bg: 'bg-green', name: '维保系统', permission: 'MAINT', path: '/maint/workspace',
      desc: '设备台账/工单管理/保养计划/备件管理，降低非计划停机',
      stats: [
        { num: m ? num(m.pendingOrderCount) : '—', text: '待处理工单' },
        { num: m ? num(m.planDueCount) : '—', text: '待执行保养' },
        { num: m ? num(m.lowStockCount) : '—', text: '备件预警' },
      ],
    },
    {
      icon: '⚡', bg: 'bg-orange', name: '电气测试系统', permission: 'ETS', path: '/ets/workspace',
      desc: '来货登记/测试管理/出入库管理，电气元件质量检测全流程',
      stats: [
        { num: e ? num(e.pendingTest) : '—', text: '待测试' },
        { num: e ? num(e.todayTested) : '—', text: '今日完成' },
        { num: e ? `${e.passRate}%` : '—', text: '合格率' },
      ],
    },
    {
      icon: '💰', bg: 'bg-purple', name: 'FIN 财务管理', permission: 'FIN', path: '/fin/workspace',
      desc: '应收/应付/费用报销/预算管理，让经营数据清晰可控',
      stats: [
        { num: f ? `¥${num(f.income)}` : '—', text: '本月收入' },
        { num: f ? `¥${num(f.pendingReceipt)}` : '—', text: '待收款' },
        { num: f ? `¥${num(f.expense)}` : '—', text: '本月支出' },
      ],
    },
    {
      icon: '⚙️', bg: 'bg-gray', name: '系统管理', permission: 'SYS', path: '/system/users',
      desc: '用户/角色/组织/权限/审计日志，支撑统一认证和平台治理',
      stats: [
        { num: sysOverview.value ? num(sysOverview.value.userCount) : '—', text: '用户' },
        { num: sysOverview.value ? num(sysOverview.value.roleCount) : '—', text: '角色' },
        { num: '6', text: '子系统' },
      ],
    },
  ]
})

const visibleSystems = computed(() => systems.value.filter((system) => auth.hasPermission(system.permission)))

/* 子系统入口只展示一行，其余收进「更多…」 */
const GRID_MIN_CARD = 250
const GRID_GAP = 20
const gridRef = ref<HTMLElement | null>(null)
const columns = ref(4)
const expanded = ref(false)
let gridObserver: ResizeObserver | null = null

function computeColumns(width: number) {
  const count = Math.floor((width + GRID_GAP) / (GRID_MIN_CARD + GRID_GAP))
  return Math.max(count || 1, 1)
}

const needsMore = computed(() => visibleSystems.value.length > columns.value)
const gridCols = computed(() => Math.max(Math.min(columns.value, visibleSystems.value.length), 1))
const displayedSystems = computed(() => {
  const list = visibleSystems.value
  if (!needsMore.value || expanded.value) return list
  return list.slice(0, columns.value)
})
const hiddenCount = computed(() => Math.max(visibleSystems.value.length - columns.value, 0))

/* ---------------- 加载 ---------------- */
async function loadData() {
  loading.value = true
  // 子系统可能未启动或当前用户无权限：用 allSettled 各自降级，
  // 不让单个接口失败拖垮整个首页
  const [crm, wms, warn, stage, maint, ets, fin, sysUser, sysRole] = await Promise.allSettled([
    getWorkspaceOverview(),
    getWmsOverview(),
    getWarningStats(),
    request.get('/crm/deal/stage-counts'),
    getMaintOverview(),
    getEtsOverview(),
    getFinWorkspace(),
    request.get('/system/user/list', { params: { page: 1, pageSize: 1 } }),
    request.get('/system/role/list', { params: { page: 1, pageSize: 1 } }),
  ])

  /**
   * 取接口的业务数据。
   *
   * 项目里存在两套 unwrap 约定，必须分开处理：
   * - crm / wms / maint / ets 的 api 文件返回整个信封 ApiEnvelope<T> = {code,message,data}
   * - fin/http.ts 的 unwrap 已剥到内层，直接返回 T
   * - 裸 request.get 返回 axios response，业务数据在 response.data（即信封）
   * 之前统一只剥一层，导致信封被当成数据，图表全部判空。
   */
  function envelope<T>(r: PromiseSettledResult<any>): T | null {
    if (r.status !== 'fulfilled') return null
    const value = r.value
    // 裸 request.get：{ data: { code, message, data } }
    if (value && typeof value === 'object' && 'code' in value && 'data' in value) {
      return (value.code === 0 ? value.data : null) as T | null
    }
    if (value && typeof value === 'object' && 'data' in value && value.data && typeof value.data === 'object' && 'code' in value.data) {
      const env = value.data
      return (env.code === 0 ? env.data : null) as T | null
    }
    return null
  }

  /** fin 的 api 已剥到内层，直接是业务数据 */
  function direct<T>(r: PromiseSettledResult<any>): T | null {
    return r.status === 'fulfilled' ? ((r.value as T) ?? null) : null
  }

  const crmData = envelope<any>(crm)
  if (crmData) {
    crmStats.value = crmData.stats ?? null
    crmLeads.value = crmData.leads ?? []
    crmActivities.value = crmData.activities ?? []
  }

  const wmsData = envelope<any>(wms)
  if (wmsData) {
    wmsSummary.value = wmsData.summary ?? null
    wmsTrend.value = wmsData.trend ?? []
    distribution.value = wmsData.distribution ?? []
  }

  const warnData = envelope<any>(warn)
  if (warnData) warningStats.value = warnData

  const stageData = envelope<any>(stage)
  if (Array.isArray(stageData)) stageCounts.value = stageData

  const maintData = envelope<any>(maint)
  if (maintData) maintStats.value = maintData

  const etsData = envelope<any>(ets)
  if (etsData) etsStats.value = etsData

  const finData = direct<any>(fin)
  if (finData) finStats.value = finData.stats ?? null

  const sysUserData = envelope<any>(sysUser)
  const sysRoleData = envelope<any>(sysRole)
  if (sysUserData || sysRoleData) {
    sysOverview.value = {
      userCount: sysUserData?.stats?.total ?? sysUserData?.total ?? null,
      roleCount: sysRoleData?.stats?.total ?? sysRoleData?.total ?? null,
    }
  }

  const failed = [crm, wms, warn, stage, maint, ets, fin, sysUser, sysRole].filter((r) => r.status === 'rejected').length
  if (failed === 9) {
    ElMessage.error('首页数据加载失败，请检查后端服务')
  } else if (failed > 0) {
    // 部分失败不弹错误（各卡片已显示「—」），仅提示存在降级
    ElMessage.warning(`${failed} 个子系统数据暂不可用，相关卡片显示为「—」`)
  }

  lastUpdated.value = new Date().toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
  loading.value = false
}

onMounted(() => {
  if (gridRef.value) {
    const apply = () => {
      if (gridRef.value) columns.value = computeColumns(gridRef.value.clientWidth)
    }
    apply()
    gridObserver = new ResizeObserver(apply)
    gridObserver.observe(gridRef.value)
  }
  loadData()
})

onBeforeUnmount(() => {
  gridObserver?.disconnect()
  gridObserver = null
})
</script>

<style scoped>
.welcome-meta {
  margin-top: 6px;
  font-size: 13px;
  color: var(--pw-text-secondary);
}

/* 商机阶段分布：用横向条形代替原 SVG 漏斗。
   原本的漏斗多边形是手工写死的坐标，改数据后形状和数字会完全脱节；
   横向条形每条独立对应一个真实阶段计数，不会撒谎。 */
.stage-funnel {
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding: 12px 4px 4px;
}
.stage-row {
  display: flex;
  align-items: center;
  gap: 12px;
}
.stage-name {
  width: 72px;
  flex-shrink: 0;
  font-size: 13px;
  color: var(--pw-text-regular);
}
.stage-track {
  flex: 1;
  height: 22px;
  border-radius: 6px;
  background: var(--pw-gray-100);
  overflow: hidden;
}
.stage-fill {
  height: 100%;
  border-radius: 6px;
  transition: width .3s var(--pw-ease);
}
.stage-count {
  width: 44px;
  flex-shrink: 0;
  text-align: right;
  font-size: 14px;
  font-weight: 600;
  color: var(--pw-text-primary);
  font-variant-numeric: tabular-nums;
}

.dist-legend {
  flex-wrap: wrap;
  gap: 10px 16px;
}

.warning-line {
  margin-top: 10px;
  text-align: center;
  font-size: 12px;
  color: var(--pw-text-secondary);
}
</style>
