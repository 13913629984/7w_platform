<template>
  <section>
    <div class="welcome-row">
      <h2>欢迎回来，管理员 👋</h2>
    </div>

    <div class="stats-cards">
      <div v-for="item in stats" :key="item.label" class="stat-card">
        <div class="stat-icon" :class="item.bg">{{ item.icon }}</div>
        <div class="stat-info">
          <div class="stat-label">{{ item.label }}</div>
          <div class="stat-value">{{ item.value }}</div>
          <div class="stat-change up">{{ item.change }}</div>
        </div>
      </div>
    </div>

    <div class="section-title">子系统入口</div>
    <div class="systems-grid">
      <div v-for="system in visibleSystems" :key="system.name" class="system-card" @click="router.push(system.path)">
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
      <div class="chart-card">
        <div class="card-header"><div class="card-title">📊 月度出入库趋势</div></div>
        <div class="monthly-chart">
          <div class="chart-y-axis">
            <span>1,800</span><span>1,500</span><span>1,200</span><span>900</span><span>600</span><span>300</span><span>0</span>
          </div>
          <div class="monthly-plot">
            <div v-for="item in monthly" :key="item.month" class="month-group">
              <div class="bar-pair">
                <span class="bar inbound" :style="{ height: item.inbound }"></span>
                <span class="bar outbound" :style="{ height: item.outbound }"></span>
              </div>
              <em>{{ item.month }}</em>
            </div>
          </div>
        </div>
        <div class="chart-legend"><span><i class="legend-blue"></i>入库</span><span><i class="legend-green"></i>出库</span></div>
      </div>

      <div class="chart-card">
        <div class="card-header"><div class="card-title">📈 销售漏斗</div></div>
        <div class="funnel-chart svg-funnel-chart">
          <svg viewBox="0 0 460 190" role="img" aria-label="销售漏斗图">
            <g v-for="item in funnel" :key="item.name">
              <polygon :points="item.points" :fill="item.color" />
              <text :x="item.textX" :y="item.textY" text-anchor="middle" dominant-baseline="middle">
                <tspan class="funnel-name">{{ item.name }}</tspan>
                <tspan class="funnel-value" dx="10">{{ item.value }}</tspan>
              </text>
            </g>
          </svg>
        </div>
        <div class="chart-legend funnel-legend">
          <span v-for="item in funnel" :key="item.name"><i :style="{ background: item.color }"></i>{{ item.name }}</span>
        </div>
      </div>

      <div class="chart-card">
        <div class="card-header"><div class="card-title">📦 库存健康分布</div></div>
        <div class="donut-wrap">
          <div class="donut-chart"></div>
          <div class="donut-center"><strong>92%</strong><span>健康库存</span></div>
        </div>
        <div class="chart-legend">
          <span><i style="background:#67c23a"></i>正常库存</span>
          <span><i style="background:#e6a23c"></i>低库存</span>
          <span><i style="background:#f56c6c"></i>超储</span>
          <span><i style="background:#909399"></i>呆滞</span>
        </div>
      </div>

      <div class="chart-card todo-card">
        <div class="card-header">
          <div class="card-title">📝 待办事项</div>
          <el-button link type="primary">查看全部</el-button>
        </div>
        <ul class="todo-list">
          <li v-for="todo in todos" :key="todo.content" class="todo-item">
            <span class="todo-dot" :class="todo.module"></span>
            <span class="todo-content">{{ todo.content }}</span>
            <span class="todo-time">{{ todo.time }}</span>
          </li>
        </ul>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const auth = useAuthStore()

const stats = [
  { icon: '📦', bg: 'bg-blue', label: '库存物料总数', value: '12,846', change: '较上月 ↑ 3.2%' },
  { icon: '👥', bg: 'bg-green', label: '活跃客户数', value: '386', change: '本月新增 18' },
  { icon: '🔧', bg: 'bg-orange', label: '待处理工单', value: '23', change: '较昨日 ↑ 5' },
  { icon: '💰', bg: 'bg-purple', label: '本月销售额', value: '¥328.5万', change: '目标完成 82%' },
]

const systems = [
  { icon: '📦', bg: 'bg-blue', name: 'WMS 仓储管理', permission: 'WMS', path: '/wms/overview', desc: '入库/出库/库存管理/库位管理/盘点，实现仓储全流程数字化', stats: [{ num: '3', text: '仓库' }, { num: '128', text: '今日出入库' }, { num: '5', text: '库存预警' }] },
  { icon: '👥', bg: 'bg-red', name: 'CRM 客户管理', permission: 'CRM', path: '/crm/workspace', desc: '客户档案/商机漏斗/合同管理/回款跟踪，提升销售转化率', stats: [{ num: '56', text: '进行中商机' }, { num: '8', text: '待审批合同' }, { num: '3', text: '逾期回款' }] },
  { icon: '🔧', bg: 'bg-green', name: '维保系统', permission: 'MAINT', path: '/maint/workspace', desc: '设备台账/工单管理/保养计划/备件管理，降低非计划停机', stats: [{ num: '23', text: '待处理工单' }, { num: '12', text: '待执行保养' }, { num: '4', text: '备件预警' }] },
  { icon: '⚡', bg: 'bg-orange', name: '电气测试系统', permission: 'ETS', path: '/ets/workspace', desc: '来货登记/测试管理/出入库管理，电气元件质量检测全流程', stats: [{ num: '28', text: '今日待测' }, { num: '45', text: '今日完成' }, { num: '96.8%', text: '合格率' }] },
  { icon: '💰', bg: 'bg-purple', name: 'FIN 财务管理', permission: 'FIN', path: '/fin/workspace', desc: '应收/应付/费用报销/预算管理，让经营数据清晰可控', stats: [{ num: '328万', text: '月销售额' }, { num: '42万', text: '待收款' }, { num: '18', text: '待审批' }] },
  { icon: '⚙️', bg: 'bg-gray', name: '系统管理', permission: 'SYS', path: '/system/users', desc: '用户/角色/组织/权限/审计日志，支撑统一认证和平台治理', stats: [{ num: '128', text: '用户' }, { num: '12', text: '角色' }, { num: '6', text: '子系统' }] },
]

const visibleSystems = computed(() => systems.filter((system) => auth.hasPermission(system.permission)))

const monthly = [
  { month: '1月', inbound: '58%', outbound: '48%' },
  { month: '2月', inbound: '66%', outbound: '58%' },
  { month: '3月', inbound: '54%', outbound: '46%' },
  { month: '4月', inbound: '72%', outbound: '63%' },
  { month: '5月', inbound: '80%', outbound: '70%' },
]

const funnel = [
  { name: '线索', value: 386, points: '20,20 440,20 376,56 84,56', textX: 230, textY: 38, color: '#409eff' },
  { name: '商机', value: 186, points: '84,56 376,56 326,88 134,88', textX: 230, textY: 72, color: '#67c23a' },
  { name: '报价', value: 96, points: '134,88 326,88 286,118 174,118', textX: 230, textY: 103, color: '#e6a23c' },
  { name: '谈判', value: 48, points: '174,118 286,118 260,146 200,146', textX: 230, textY: 132, color: '#f56c6c' },
  { name: '成交', value: 26, points: '200,146 260,146 244,170 216,170', textX: 230, textY: 158, color: '#909399' },
]

const todos = [
  { module: 'wms', content: 'WMS 原材料仓库盘点计划待审批', time: '10分钟前' },
  { module: 'crm', content: 'CRM 合同 HT-2026-0318 即将到期（7天）', time: '1小时前' },
  { module: 'crm', content: 'CRM 客户「深圳XX科技」回款逾期 30 天', time: '3小时前' },
  { module: 'maint', content: '维保 CNC-003 号机床保养任务到期', time: '2小时前' },
  { module: 'ets', content: '电气测试 批次 B2026052902 待登记', time: '30分钟前' },
  { module: 'fin', content: '财务 客户「深圳XX科技」待回款 ¥84,000', time: '30分钟前' },
]
</script>

