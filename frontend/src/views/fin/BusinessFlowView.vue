<template>
  <section>
    <div class="sub-page-header">
      <div>
        <h2>销售订单与采购订单 - 财务成本核算关联流程</h2>
        <p class="page-desc">展示业务单据在 CRM / WMS / 财务之间的流转与成本核算闭环。</p>
      </div>
      <el-space>
        <el-button size="small" plain @click="handleExport">导出流程图</el-button>
        <el-button size="small" type="primary" @click="handleDetail">查看详细说明</el-button>
      </el-space>
    </div>

    <div class="stats-row">
      <div v-for="item in statCards" :key="item.label" class="stat-card" :class="item.color">
        <div class="stat-header">
          <span class="stat-title">{{ item.label }}</span>
        </div>
        <div class="stat-value">{{ item.value }}</div>
        <div class="stat-footer"><span>{{ item.trend }}</span></div>
      </div>
    </div>

    <div class="card flow-section">
      <div class="card-header"><div class="card-title">📥 销售订单流程（收入侧）</div></div>
      <div class="flow-line">
        <template v-for="(step, idx) in salesFlow" :key="step.title">
          <span v-if="step.arrow" class="flow-arrow">→</span>
          <div class="flow-box" :class="step.color">
            <div class="flow-box-title">{{ step.title }}</div>
            <div class="flow-box-tag">{{ step.tag }}</div>
            <div class="flow-box-desc">{{ step.desc }}</div>
          </div>
        </template>
      </div>
    </div>

    <div class="tip-box">
      <div class="tip-title">📌 关键关联点</div>
      <p><strong>销售订单 → 出库单：</strong>销售订单确认后自动生成出库单，关联物料明细和客户信息</p>
      <p><strong>出库单 → 应收款：</strong>出库完成后自动触发应收款生成，金额=销售金额，关联销售订单号</p>
      <p><strong>应收款 → 收入核算：</strong>回款完成后，系统自动确认收入，计入损益表</p>
    </div>

    <div class="card flow-section">
      <div class="card-header"><div class="card-title">📤 采购订单流程（成本侧）</div></div>
      <div class="flow-line">
        <template v-for="(step, idx) in purchaseFlow" :key="step.title">
          <span v-if="step.arrow" class="flow-arrow">→</span>
          <div class="flow-box" :class="step.color">
            <div class="flow-box-title">{{ step.title }}</div>
            <div class="flow-box-tag">{{ step.tag }}</div>
            <div class="flow-box-desc">{{ step.desc }}</div>
          </div>
        </template>
      </div>
    </div>

    <div class="tip-box">
      <div class="tip-title">📌 关键关联点</div>
      <p><strong>采购订单 → 入库单：</strong>采购订单确认后，供应商送货入库，关联PO单号和物料明细</p>
      <p><strong>入库单 → 应付款：</strong>入库验收完成后自动触发应付款，金额=采购金额+税费</p>
      <p><strong>应付款 → 成本核算：</strong>付款完成后，材料成本归集到存货，销售时结转至营业成本</p>
    </div>

    <div class="card flow-section">
      <div class="card-header"><div class="card-title">🔄 财务成本核算闭环</div></div>
      <div class="core-flow-banner">
        <div class="core-flow-title">成本核算核心流程</div>
        <div class="core-flow-steps">
          <span v-for="s in coreSteps" :key="s" class="core-flow-step">{{ s }}</span>
        </div>
      </div>
      <div class="closure-grid">
        <div v-for="box in closureBoxes" :key="box.title" class="flow-box" :class="box.color">
          <div class="flow-box-title">{{ box.title }}</div>
          <div class="flow-box-tag">{{ box.tag }}</div>
        </div>
      </div>
    </div>

    <div class="table-card">
      <div class="card-header expense-header">
        <div class="card-title">📋 单据关联明细表</div>
        <el-space>
          <el-input
            v-model="relationKeyword"
            size="small"
            style="width: 200px"
            clearable
            placeholder="搜索单据号/客户/供应商"
            @clear="loadRelations"
            @keyup.enter="loadRelations"
          />
          <el-tag v-if="completeness.total" size="small" type="info" effect="plain">
            共 {{ completeness.total }} 条 · 单据齐全 {{ completeness.complete }} 条 · 待补齐 {{ completeness.pending }} 条
          </el-tag>
          <el-button size="small" plain @click="loadRelations">查询</el-button>
          <el-button size="small" plain @click="handleExport">导出</el-button>
        </el-space>
      </div>
      <el-table :data="relations" stripe style="width: 100%" empty-text="暂无关联单据（先在 CRM 建单或 WMS 建采购单）">
        <el-table-column label="业务类型" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="row.bizType === '销售业务' ? 'success' : 'primary'" effect="light">{{ row.bizType }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="客户/供应商" min-width="160" show-overflow-tooltip>
          <template #default="{ row }">{{ row.partner || '-' }}</template>
        </el-table-column>
        <el-table-column label="销售订单" min-width="150">
          <template #default="{ row }"><span v-if="row.salesOrder !== '-'" class="doc-no">{{ row.salesOrder }}</span><span v-else>-</span></template>
        </el-table-column>
        <el-table-column label="采购订单" min-width="150">
          <template #default="{ row }"><span v-if="row.purchaseOrder !== '-'" class="doc-no">{{ row.purchaseOrder }}</span><span v-else>-</span></template>
        </el-table-column>
        <el-table-column label="出入库单" min-width="160">
          <template #default="{ row }"><span v-if="row.stockDoc !== '-'" class="doc-no">{{ row.stockDoc }}</span><span v-else class="missing">-</span></template>
        </el-table-column>
        <el-table-column label="应收/应付" min-width="150">
          <template #default="{ row }"><span v-if="row.arap !== '-'" class="doc-no">{{ row.arap }}</span><span v-else class="missing">-</span></template>
        </el-table-column>
        <el-table-column label="付款/收款" min-width="160">
          <template #default="{ row }"><span v-if="row.payment !== '-'" class="doc-no">{{ row.payment }}</span><span v-else class="missing">-</span></template>
        </el-table-column>
        <el-table-column label="金额" width="130" align="right">
          <template #default="{ row }">{{ formatAmount(row.amount) }}</template>
        </el-table-column>
        <el-table-column label="关联状态" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" effect="light">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="detailVisible" title="单据流转与成本核算说明" width="640px">
      <div class="tip-box" style="margin-bottom: 16px">
        <div class="tip-title">两条主链</div>
        <p>
          <strong>销售链：</strong>CRM 商机 → 销售订单 → WMS 出库单（自动分配库位与库存）→ FIN 应收款（出库完成自动生成）→ 开票 → 回款核销 → 财务流水。<br />
          <strong>采购链：</strong>采购需求 / MRP → 采购订单 → WMS 入库单 → FIN 应付款（入库完成自动生成）→ 发票匹配 → 付款 → 成本归集。
        </p>
      </div>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="流水总数">{{ stats.total }} 笔</el-descriptions-item>
        <el-descriptions-item label="已确认">{{ stats.confirmed }} 笔</el-descriptions-item>
        <el-descriptions-item label="关联单据数">{{ completeness.total }} 条</el-descriptions-item>
        <el-descriptions-item label="单据齐全">{{ completeness.complete }} 条</el-descriptions-item>
        <el-descriptions-item label="收入合计">{{ formatWan(stats.income) }}</el-descriptions-item>
        <el-descriptions-item label="支出合计">{{ formatWan(stats.expense) }}</el-descriptions-item>
      </el-descriptions>
      <div class="formula-grid" style="margin-top: 16px">
        <div v-for="f in formulas" :key="f.title" class="formula-box" :class="f.color">
          <div class="formula-title">{{ f.title }}</div>
          <div class="formula-body">{{ f.body }}</div>
        </div>
      </div>
    </el-dialog>

    <div class="card">
      <div class="card-header"><div class="card-title">📊 成本核算公式说明</div></div>
      <div class="formula-grid">
        <div v-for="f in formulas" :key="f.title" class="formula-box" :class="f.color">
          <div class="formula-title">{{ f.title }}</div>
          <div class="formula-body">{{ f.body }}</div>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { downloadCsvFromRows } from '@/utils/export'
import {
  businessFlowStats,
  listBusinessRelations,
  businessCompleteness,
  type BusinessRelation,
} from '@/api/fin/businessFlow'

const stats = ref({ income: 0, expense: 0, total: 0, confirmed: 0, processing: 0 })
/** 单据关联明细：来自 CRM/WMS/FIN 真实表，不再使用前端写死数据 */
const relations = ref<BusinessRelation[]>([])
const completeness = ref({ total: 0, complete: 0, pending: 0 })
const relationKeyword = ref('')

const statCards = computed(() => [
  { label: '财务流水总数', value: `${stats.value.total} 笔`, trend: '全部业务流水', color: 'blue' },
  { label: '收入流水金额', value: formatWan(stats.value.income), trend: '收入侧', color: 'green' },
  { label: '支出流水金额', value: formatWan(stats.value.expense), trend: '成本侧', color: 'orange' },
  { label: '已确认流水', value: `${stats.value.confirmed} 笔`, trend: `处理中 ${stats.value.processing} 笔`, color: 'purple' },
])

async function loadStats() {
  try {
    stats.value = await businessFlowStats()
  } catch (e: any) {
    ElMessage.error(e.message || '加载失败')
  }
}

async function loadRelations() {
  try {
    const [rows, c] = await Promise.all([
      listBusinessRelations({ keyword: relationKeyword.value || undefined }),
      businessCompleteness({ keyword: relationKeyword.value || undefined }),
    ])
    relations.value = rows
    completeness.value = c
  } catch (e: any) {
    ElMessage.error(e.message || '加载关联明细失败')
  }
}

async function loadAll() {
  await Promise.all([loadStats(), loadRelations()])
}

onMounted(loadAll)

function formatWan(value: number) {
  const wan = Number(value || 0) / 10000
  return `¥${wan.toFixed(1)}万`
}

function formatAmount(value: number | undefined) {
  const n = Number(value || 0)
  return `¥${n.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`
}

const salesFlow = [
  { title: '商机确认', tag: 'CRM', desc: '客户需求、报价', color: 'green', arrow: false },
  { title: '销售订单', tag: 'CRM', desc: '合同签订、SO创建', color: 'green', arrow: false },
  { title: '出库发货', tag: 'WMS', desc: '库存扣减、发货', color: 'orange', arrow: false },
  { title: '应收款生成', tag: 'FIN', desc: 'AR单据、开票', color: 'blue', arrow: true },
  { title: '回款核销', tag: 'FIN', desc: '收款确认、对账', color: 'blue', arrow: false },
  { title: '收入确认', tag: 'FIN', desc: '财务报表、利润核算', color: 'red', arrow: false },
]

const purchaseFlow = [
  { title: '采购需求', tag: 'WMS', desc: '库存预警、MRP计算', color: 'orange', arrow: false },
  { title: '采购订单', tag: 'WMS', desc: 'PO创建、供应商确认', color: 'orange', arrow: false },
  { title: '入库验收', tag: 'WMS', desc: '质检、库存增加', color: 'orange', arrow: false },
  { title: '应付款生成', tag: 'FIN', desc: 'AP单据、发票匹配', color: 'blue', arrow: true },
  { title: '付款结算', tag: 'FIN', desc: '审批、支付', color: 'blue', arrow: false },
  { title: '成本归集', tag: 'FIN', desc: '材料成本、费用分配', color: 'red', arrow: false },
]

const coreSteps = [
  '采购入库 → 存货成本',
  '生产加工 → 生产成本',
  '销售出库 → 营业成本',
  '费用归集 → 期间费用',
  '收入确认 → 利润计算',
]

const closureBoxes = [
  { title: '原材料采购', tag: 'WMS采购', color: 'orange' },
  { title: '销售订单发货', tag: 'CRM', color: 'green' },
  { title: '收入确认', tag: 'FIN应收', color: 'blue' },
  { title: '入库增加库存', tag: 'WMS入库', color: 'orange' },
  { title: '出库扣减库存', tag: 'WMS出库', color: 'orange' },
  { title: '费用归集', tag: 'FIN费用', color: 'blue' },
  { title: '存货成本增加', tag: 'FIN', color: 'blue' },
  { title: '结转营业成本', tag: 'FIN', color: 'blue' },
  { title: '利润核算', tag: 'FIN报表', color: 'red' },
]

const formulas = [
  { title: '营业成本', body: '原材料成本 + 人工成本 + 制造费用 + 其他直接费用', color: 'blue' },
  { title: '毛利', body: '销售收入 - 营业成本', color: 'green' },
  { title: '净利润', body: '毛利 - 销售费用 - 管理费用 - 财务费用 - 税费', color: 'red' },
]

function statusTagType(status: string) {
  if (['已回款', '已完成', '已付款', '已入库', '已出库'].includes(status)) return 'success'
  if (['待回款', '待付款', '待匹配', '待出库'].includes(status)) return 'warning'
  if (['已发货', '入库中', '付款中', 'confirmed', 'inbound'].includes(status)) return 'primary'
  if (status === 'cancelled' || status === '已取消') return 'info'
  return 'info'
}

function handleExport() {
  if (!relations.value.length) {
    ElMessage.warning('暂无可导出的关联明细')
    return
  }
  downloadCsvFromRows('单据关联明细', [
    { label: '业务类型', value: (r) => r.bizType },
    { label: '销售订单', value: (r) => r.salesOrder },
    { label: '采购订单', value: (r) => r.purchaseOrder },
    { label: '出入库单', value: (r) => r.stockDoc },
    { label: '应收/应付', value: (r) => r.arap },
    { label: '付款/收款', value: (r) => r.payment },
    { label: '客户/供应商', value: (r) => r.partner },
    { label: '金额', value: (r) => Number(r.amount || 0).toFixed(2) },
    { label: '日期', value: (r) => r.date },
    { label: '关联状态', value: (r) => r.status },
  ], relations.value)
  ElMessage.success('已导出关联明细')
}

const detailVisible = ref(false)

function handleDetail() {
  detailVisible.value = true
}
</script>

<style scoped>
.flow-section { margin-bottom: 16px; }
.flow-line { display: flex; flex-wrap: wrap; align-items: flex-start; gap: 14px; padding: 16px 4px; }
.flow-arrow { align-self: flex-start; margin-top: 14px; color: var(--pw-text-secondary); font-size: 18px; }
.flow-box { width: 120px; border-radius: 8px; border: 1px solid transparent; padding: 12px 8px; text-align: center; background: #fff; }
.flow-box-title { font-size: 14px; font-weight: 600; color: var(--text-primary); }
.flow-box-tag { font-size: 11px; font-weight: 600; margin-top: 6px; }
.flow-box-desc { font-size: 11px; color: var(--text-secondary); margin-top: 8px; line-height: 1.5; }
.flow-box.green { background: #f0f9eb; border-color: #c2e7b0; }
.flow-box.green .flow-box-tag { color: #67c23a; }
.flow-box.orange { background: #fdf6ec; border-color: #f5dab1; }
.flow-box.orange .flow-box-tag { color: #e6a23c; }
.flow-box.blue { background: #ecf5ff; border-color: #b3d8ff; }
.flow-box.blue .flow-box-tag { color: #409eff; }
.flow-box.red { background: #fef0f0; border-color: #fab6b6; }
.flow-box.red .flow-box-tag { color: #f56c6c; }
.doc-no { font-family: var(--el-font-family-mono, monospace); font-size: 12px; color: var(--el-color-primary); }
.missing { color: var(--el-text-color-placeholder); }

.tip-box { background: #fdf6ec; border: 1px solid #f5dab1; border-radius: 10px; padding: 16px 18px; margin-bottom: 24px; }
.tip-title { font-weight: 600; color: #e6a23c; margin-bottom: 10px; }
.tip-box p { font-size: 13px; color: var(--pw-text-regular); line-height: 1.9; margin: 0; }
.tip-box strong { color: var(--text-primary); }

.core-flow-banner { background: linear-gradient(135deg, #667eea, #764ba2); border-radius: 10px; padding: 18px; color: #fff; margin: 8px 0 24px; }
.core-flow-title { text-align: center; font-size: 16px; font-weight: 600; margin-bottom: 14px; }
.core-flow-steps { display: flex; flex-wrap: wrap; justify-content: center; gap: 10px; }
.core-flow-step { background: rgba(255,255,255,.18); border-radius: 6px; padding: 6px 14px; font-size: 13px; }

.closure-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(140px, max-content)); justify-content: center; gap: 16px 24px; padding: 8px 0; }
.closure-grid .flow-box { width: 150px; max-width: 100%; }

.expense-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 16px; }

.formula-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(240px, 1fr)); gap: 16px; padding: 8px 0; }
.formula-box { border-radius: 10px; padding: 16px 18px; border-left: 4px solid; }
.formula-box.blue { background: #ecf5ff; border-color: #409eff; }
.formula-box.green { background: #f0f9eb; border-color: #67c23a; }
.formula-box.red { background: #fef0f0; border-color: #f56c6c; }
.formula-title { font-size: 15px; font-weight: 600; margin-bottom: 8px; }
.formula-box.blue .formula-title { color: #409eff; }
.formula-box.green .formula-title { color: #67c23a; }
.formula-box.red .formula-title { color: #f56c6c; }
.formula-body { font-size: 13px; color: var(--pw-text-regular); line-height: 1.6; }
</style>
