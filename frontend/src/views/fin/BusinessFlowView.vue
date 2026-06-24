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
        <el-button size="small" plain @click="handleExport">导出</el-button>
      </div>
      <el-table :data="relations" stripe style="width: 100%">
        <el-table-column label="业务类型" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="row.bizType === '销售业务' ? 'success' : 'primary'" effect="light">{{ row.bizType }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="销售订单" min-width="150">
          <template #default="{ row }"><el-button v-if="row.salesOrder !== '-'" link type="primary">{{ row.salesOrder }}</el-button><span v-else>-</span></template>
        </el-table-column>
        <el-table-column label="采购订单" min-width="150">
          <template #default="{ row }"><el-button v-if="row.purchaseOrder !== '-'" link type="primary">{{ row.purchaseOrder }}</el-button><span v-else>-</span></template>
        </el-table-column>
        <el-table-column label="出入库单" min-width="150">
          <template #default="{ row }"><el-button v-if="row.stockDoc !== '-'" link type="primary">{{ row.stockDoc }}</el-button><span v-else>-</span></template>
        </el-table-column>
        <el-table-column label="应收/应付" min-width="150">
          <template #default="{ row }"><el-button v-if="row.arap !== '-'" link type="primary">{{ row.arap }}</el-button><span v-else>-</span></template>
        </el-table-column>
        <el-table-column label="付款/收款" min-width="150">
          <template #default="{ row }"><el-button v-if="row.payment !== '-'" link type="primary">{{ row.payment }}</el-button><span v-else>-</span></template>
        </el-table-column>
        <el-table-column label="关联状态" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" effect="light">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
      </el-table>
    </div>

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
import { ElMessage } from 'element-plus'

const statCards = [
  { label: '销售订单数量', value: '128 笔', trend: '本月', color: 'blue' },
  { label: '采购订单数量', value: '86 笔', trend: '本月', color: 'green' },
  { label: '关联财务单据', value: '214 张', trend: '应收+应付', color: 'orange' },
  { label: '成本核算准确率', value: '98.6%', trend: '实时同步', color: 'purple' },
]

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

const relations = [
  { bizType: '销售业务', salesOrder: 'SO-2026-0528-001', purchaseOrder: '-', stockDoc: 'CK20260528001', arap: 'AR-2026-0528-001', payment: 'RCV-2026-0528-001', status: '已完成' },
  { bizType: '销售业务', salesOrder: 'SO-2026-0527-002', purchaseOrder: '-', stockDoc: 'CK20260527001', arap: 'AR-2026-0527-002', payment: '-', status: '待回款' },
  { bizType: '采购业务', salesOrder: '-', purchaseOrder: 'PO-2026-0528-001', stockDoc: 'RK20260528001', arap: 'AP-2026-0528-001', payment: 'PAY-2026-0528-001', status: '已完成' },
  { bizType: '采购业务', salesOrder: '-', purchaseOrder: 'PO-2026-0527-002', stockDoc: 'RK20260527001', arap: 'AP-2026-0527-002', payment: '-', status: '待付款' },
  { bizType: '采购业务', salesOrder: '-', purchaseOrder: 'PO-2026-0526-003', stockDoc: '-', arap: '-', payment: '-', status: '待入库' },
]

const formulas = [
  { title: '营业成本', body: '原材料成本 + 人工成本 + 制造费用 + 其他直接费用', color: 'blue' },
  { title: '毛利', body: '销售收入 - 营业成本', color: 'green' },
  { title: '净利润', body: '毛利 - 销售费用 - 管理费用 - 财务费用 - 税费', color: 'red' },
]

function statusTagType(status: string) {
  if (status === '已完成') return 'success'
  if (status === '待回款' || status === '待付款') return 'warning'
  if (status === '待入库') return 'primary'
  return 'info'
}

function handleExport() {
  ElMessage.success('导出成功')
}

function handleDetail() {
  ElMessage.info('查看详细说明')
}
</script>

<style scoped>
.flow-section { margin-bottom: 16px; }
.flow-line { display: flex; flex-wrap: wrap; align-items: flex-start; gap: 14px; padding: 16px 4px; }
.flow-arrow { align-self: flex-start; margin-top: 14px; color: #c0c4cc; font-size: 18px; }
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

.tip-box { background: #fdf6ec; border: 1px solid #f5dab1; border-radius: 10px; padding: 16px 18px; margin-bottom: 24px; }
.tip-title { font-weight: 600; color: #e6a23c; margin-bottom: 10px; }
.tip-box p { font-size: 13px; color: #606266; line-height: 1.9; margin: 0; }
.tip-box strong { color: var(--text-primary); }

.core-flow-banner { background: linear-gradient(135deg, #667eea, #764ba2); border-radius: 10px; padding: 18px; color: #fff; margin: 8px 0 24px; }
.core-flow-title { text-align: center; font-size: 16px; font-weight: 600; margin-bottom: 14px; }
.core-flow-steps { display: flex; flex-wrap: wrap; justify-content: center; gap: 10px; }
.core-flow-step { background: rgba(255,255,255,.18); border-radius: 6px; padding: 6px 14px; font-size: 13px; }

.closure-grid { display: grid; grid-template-columns: repeat(3, max-content); justify-content: center; gap: 16px 40px; padding: 8px 0; }
.closure-grid .flow-box { width: 150px; }

.expense-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 16px; }

.formula-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 16px; padding: 8px 0; }
.formula-box { border-radius: 10px; padding: 16px 18px; border-left: 4px solid; }
.formula-box.blue { background: #ecf5ff; border-color: #409eff; }
.formula-box.green { background: #f0f9eb; border-color: #67c23a; }
.formula-box.red { background: #fef0f0; border-color: #f56c6c; }
.formula-title { font-size: 15px; font-weight: 600; margin-bottom: 8px; }
.formula-box.blue .formula-title { color: #409eff; }
.formula-box.green .formula-title { color: #67c23a; }
.formula-box.red .formula-title { color: #f56c6c; }
.formula-body { font-size: 13px; color: #606266; line-height: 1.6; }
</style>
