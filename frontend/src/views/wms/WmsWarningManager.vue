<template>
  <section>
    <div class="sub-page-header">
      <div>
        <h2>库存预警</h2>
        <p class="page-desc">监控安全库存，支持配置全局与物料级预警规则，自动识别缺货与低库存物料并跟踪处理状态。</p>
      </div>
      <el-space>
        <el-button plain :icon="Refresh" @click="refreshAll">刷新</el-button>
        <el-button type="primary" :icon="Search" :loading="scanning" @click="handleScan">扫描预警</el-button>
      </el-space>
    </div>

    <el-tabs v-model="activeTab" class="wms-tabs-card">
      <el-tab-pane label="预警列表" name="list">
        <div class="stats-row">
          <div class="stat-card blue"><div class="stat-header"><span class="stat-title">预警总数</span><div class="stat-icon">⚠️</div></div><div class="stat-value">{{ stats.total }}</div><div class="stat-footer">预警物料总数</div></div>
          <div class="stat-card orange"><div class="stat-header"><span class="stat-title">待处理</span><div class="stat-icon">⏳</div></div><div class="stat-value">{{ stats.pending }}</div><div class="stat-footer">等待处理</div></div>
          <div class="stat-card green"><div class="stat-header"><span class="stat-title">已处理</span><div class="stat-icon">✅</div></div><div class="stat-value">{{ stats.handled }}</div><div class="stat-footer">处理完成</div></div>
          <div class="stat-card purple"><div class="stat-header"><span class="stat-title">高危预警</span><div class="stat-icon">🚨</div></div><div class="stat-value">{{ stats.high }}</div><div class="stat-footer">需紧急处理</div></div>
        </div>

        <div class="table-card">
          <div class="filter-bar">
            <div class="filter-left">
              <el-input v-model="keyword" clearable placeholder="搜索SKU/物料名称" style="width:260px" :prefix-icon="Search" @keyup.enter="loadData" @clear="loadData" />
              <el-select v-model="levelFilter" clearable placeholder="预警级别" style="width:130px" @change="loadData">
                <el-option label="高危" value="high" />
                <el-option label="中危" value="medium" />
                <el-option label="低危" value="low" />
              </el-select>
              <el-select v-model="statusFilter" clearable placeholder="处理状态" style="width:130px" @change="loadData">
                <el-option label="待处理" value="pending" />
                <el-option label="已处理" value="handled" />
                <el-option label="已解除" value="resolved" />
              </el-select>
              <el-button type="primary" :icon="Search" @click="loadData">查询</el-button>
            </div>
            <el-button type="primary" plain :icon="Plus" @click="openRuleFromList">配置规则</el-button>
          </div>

          <el-table v-loading="loading" :data="pagedRows" border stripe style="width:100%" empty-text="暂无预警，可点击「扫描预警」生成">
            <el-table-column prop="sku" label="SKU编码" width="130" />
            <el-table-column prop="name" label="物料名称" min-width="150"><template #default="{ row }"><b>{{ row.name }}</b></template></el-table-column>
            <el-table-column prop="spec" label="规格型号" width="120" />
            <el-table-column prop="warehouse" label="仓库" width="130" />
            <el-table-column prop="currentQty" label="当前库存" width="100" align="right"><template #default="{ row }">{{ formatNumber(row.currentQty) }}</template></el-table-column>
            <el-table-column prop="threshold" label="预警阈值" width="100" align="right" />
            <el-table-column label="命中规则" width="140"><template #default="{ row }"><span v-if="row.ruleName">{{ row.ruleName }}</span><span v-else class="muted">安全库存</span></template></el-table-column>
            <el-table-column label="预警级别" width="100" align="center"><template #default="{ row }"><el-tag :type="levelTagType(row.level)" effect="dark">{{ row.levelName }}</el-tag></template></el-table-column>
            <el-table-column prop="warningTime" label="预警时间" width="170" />
            <el-table-column label="状态" width="100" align="center"><template #default="{ row }"><el-tag :type="statusTagType(row.status)" effect="light">{{ row.statusName }}</el-tag></template></el-table-column>
            <el-table-column label="操作" width="240" fixed="right" align="center">
              <template #default="{ row }">
                <el-button link type="primary" @click="openDetail(row)">详情</el-button>
                <el-button v-if="row.status === 'pending'" link type="success" @click="openHandle(row)">处理</el-button>
                <el-button link type="warning" @click="openRuleFromWarning(row)">设为规则</el-button>
                <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>

          <div class="wms-pagination">
            <el-pagination v-model:current-page="page" v-model:page-size="pageSize" :total="rows.length" :page-sizes="[10, 20, 50, 100]" background layout="total, sizes, prev, pager, next, jumper" />
          </div>
        </div>
      </el-tab-pane>

      <el-tab-pane name="rules">
        <template #label>预警规则 <el-badge :value="rules.length" :max="99" type="primary" /></template>
        <div class="table-card">
          <el-alert
            class="rule-tip"
            type="info"
            :closable="false"
            show-icon
            title="规则优先级：物料级规则 > 全局规则；限定仓库的规则 > 通用规则。未命中规则的物料按安全库存判定（高危 0，中危 安全库存/2，低危 安全库存）。"
          />
          <div class="filter-bar">
            <div class="filter-left">
              <el-input v-model="ruleKeyword" clearable placeholder="搜索规则名称/物料" style="width:240px" :prefix-icon="Search" @keyup.enter="loadRules" @clear="loadRules" />
              <el-select v-model="ruleScopeFilter" clearable placeholder="适用范围" style="width:130px" @change="loadRules">
                <el-option label="全局规则" value="global" />
                <el-option label="指定物料" value="material" />
              </el-select>
              <el-select v-model="ruleEnabledFilter" clearable placeholder="状态" style="width:120px" @change="loadRules">
                <el-option label="启用" :value="1" />
                <el-option label="停用" :value="0" />
              </el-select>
              <el-button type="primary" :icon="Search" @click="loadRules">查询</el-button>
            </div>
            <el-button type="primary" :icon="Plus" @click="openRuleDialog()">新建规则</el-button>
          </div>

          <el-table v-loading="ruleLoading" :data="pagedRules" border stripe style="width:100%" empty-text="暂无预警规则，点击「新建规则」添加">
            <el-table-column prop="name" label="规则名称" min-width="170"><template #default="{ row }"><b>{{ row.name }}</b></template></el-table-column>
            <el-table-column label="适用范围" width="110" align="center"><template #default="{ row }"><el-tag :type="row.scope === 'material' ? 'warning' : 'info'" effect="light">{{ row.scopeName }}</el-tag></template></el-table-column>
            <el-table-column label="适用物料" min-width="180">
              <template #default="{ row }">
                <span v-if="row.scope === 'material'"><b>{{ row.materialSku }}</b> · {{ row.materialName }}</span>
                <span v-else class="muted">全部物料</span>
              </template>
            </el-table-column>
            <el-table-column label="适用仓库" width="140"><template #default="{ row }"><span v-if="row.warehouseName">{{ row.warehouseName }}</span><span v-else class="muted">全部仓库</span></template></el-table-column>
            <el-table-column label="阈值配置（低 / 中 / 高）" width="200" align="center">
              <template #default="{ row }">
                <el-tag type="info" effect="plain" size="small">低 {{ row.lowThreshold }}</el-tag>
                <el-tag type="warning" effect="plain" size="small">中 {{ row.mediumThreshold }}</el-tag>
                <el-tag type="danger" effect="plain" size="small">高 {{ row.highThreshold }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="状态" width="90" align="center"><template #default="{ row }"><el-tag :type="row.enabled === 1 ? 'success' : 'info'" effect="light">{{ row.enabledName }}</el-tag></template></el-table-column>
            <el-table-column prop="updateTime" label="更新时间" width="170" />
            <el-table-column label="操作" width="170" fixed="right" align="center">
              <template #default="{ row }">
                <el-button link type="primary" @click="openRuleDialog(row)">编辑</el-button>
                <el-button link :type="row.enabled === 1 ? 'warning' : 'success'" @click="handleToggleRule(row)">{{ row.enabled === 1 ? '停用' : '启用' }}</el-button>
                <el-button link type="danger" @click="handleDeleteRule(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>

          <div class="wms-pagination">
            <el-pagination v-model:current-page="rulePage" v-model:page-size="rulePageSize" :total="rules.length" :page-sizes="[10, 20, 50]" background layout="total, sizes, prev, pager, next" />
          </div>
        </div>
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="ruleDialogVisible" :title="ruleDialogMode === 'create' ? '新建预警规则' : '编辑预警规则'" width="620px" :close-on-click-modal="false">
      <el-form label-width="110px" class="wms-crud-form">
        <el-form-item label="规则名称" required>
          <el-input v-model="ruleForm.name" placeholder="如：常备物料低库存预警" maxlength="100" show-word-limit />
        </el-form-item>
        <el-form-item label="适用范围" required>
          <el-radio-group v-model="ruleForm.scope">
            <el-radio-button value="global">全局规则（适用所有物料）</el-radio-button>
            <el-radio-button value="material">指定物料</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="ruleForm.scope === 'material'" label="适用物料" required>
          <el-select
            v-model="ruleForm.materialId"
            filterable
            remote
            reserve-keyword
            clearable
            placeholder="输入SKU或名称搜索物料"
            :remote-method="searchMaterials"
            :loading="materialLoading"
            style="width:100%"
            @focus="ensureMaterialOptions"
          >
            <el-option v-for="item in materialOptions" :key="item.id" :label="`${item.sku} · ${item.name}`" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="适用仓库">
          <el-select v-model="ruleForm.warehouseCode" clearable placeholder="不选则适用于全部仓库" style="width:100%">
            <el-option v-for="wh in warehouseOptions" :key="wh.code" :label="wh.name" :value="wh.code" />
          </el-select>
        </el-form-item>
        <el-form-item label="低危阈值">
          <el-input-number v-model="ruleForm.lowThreshold" :min="0" :step="1" controls-position="right" style="width:180px" />
          <span class="field-hint">库存 ≤ 该值判为低危</span>
        </el-form-item>
        <el-form-item label="中危阈值">
          <el-input-number v-model="ruleForm.mediumThreshold" :min="0" :step="1" controls-position="right" style="width:180px" />
          <span class="field-hint">库存 ≤ 该值判为中危</span>
        </el-form-item>
        <el-form-item label="高危阈值">
          <el-input-number v-model="ruleForm.highThreshold" :min="0" :step="1" controls-position="right" style="width:180px" />
          <span class="field-hint">库存 ≤ 该值时判为高危（0 表示仅缺货告警）</span>
        </el-form-item>
        <el-form-item label="启用状态"><el-switch v-model="ruleForm.enabled" :active-value="1" :inactive-value="0" active-text="启用" inactive-text="停用" /></el-form-item>
        <el-form-item label="备注"><el-input v-model="ruleForm.remark" type="textarea" :rows="2" maxlength="500" show-word-limit placeholder="可选，说明该规则的适用场景" /></el-form-item>
        <el-form-item>
          <span class="field-hint">阈值需满足：高危 ≤ 中危 ≤ 低危。规则保存后请点击「扫描预警」重新生成预警。</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="ruleDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="ruleSubmitting" @click="submitRule">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="handleVisible" title="处理预警" width="520px" :close-on-click-modal="false">
      <el-form v-if="current" label-width="92px">
        <el-form-item label="物料">{{ current.sku }} / {{ current.name }}</el-form-item>
        <el-form-item label="当前库存">{{ formatNumber(current.currentQty) }} / 预警阈值 {{ current.threshold }}</el-form-item>
        <el-form-item label="处理人" required><el-input v-model="handleForm.handler" placeholder="请输入处理人" /></el-form-item>
        <el-form-item label="处理说明"><el-input v-model="handleForm.remark" type="textarea" :rows="3" placeholder="如：已生成采购需求 / 已补货" maxlength="500" show-word-limit /></el-form-item>
      </el-form>
      <template #footer><el-button @click="handleVisible = false">取消</el-button><el-button type="primary" :loading="submitting" @click="submitHandle">确认处理</el-button></template>
    </el-dialog>

    <el-dialog v-model="detailVisible" title="预警详情" width="640px">
      <el-descriptions v-if="current" :column="2" border>
        <el-descriptions-item label="SKU编码">{{ current.sku }}</el-descriptions-item>
        <el-descriptions-item label="物料名称">{{ current.name }}</el-descriptions-item>
        <el-descriptions-item label="规格型号">{{ current.spec || '-' }}</el-descriptions-item>
        <el-descriptions-item label="仓库">{{ current.warehouse }}</el-descriptions-item>
        <el-descriptions-item label="当前库存">{{ formatNumber(current.currentQty) }}</el-descriptions-item>
        <el-descriptions-item label="预警阈值">{{ current.threshold }}</el-descriptions-item>
        <el-descriptions-item label="命中规则">{{ current.ruleName || '安全库存' }}</el-descriptions-item>
        <el-descriptions-item label="预警级别"><el-tag :type="levelTagType(current.level)" effect="dark">{{ current.levelName }}</el-tag></el-descriptions-item>
        <el-descriptions-item label="状态"><el-tag :type="statusTagType(current.status)" effect="light">{{ current.statusName }}</el-tag></el-descriptions-item>
        <el-descriptions-item label="预警时间">{{ current.warningTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="处理人">{{ current.handler || '-' }}</el-descriptions-item>
        <el-descriptions-item label="处理时间" :span="2">{{ current.handleTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="处理说明" :span="2">{{ current.handleRemark || '-' }}</el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
        <el-button v-if="current" type="warning" @click="openRuleFromWarning(current)">设为该物料规则</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'
import {
  deleteWarning,
  getWarningStats,
  handleWarning,
  listWarnings,
  scanWarnings,
  type WarningItem,
  type WarningLevel,
  type WarningStats,
  type WarningStatus,
} from '@/api/wms/warning'
import {
  createWarningRule,
  deleteWarningRule,
  listWarningRules,
  toggleWarningRule,
  updateWarningRule,
  type WarningRuleItem,
  type WarningRuleScope,
} from '@/api/wms/warningRule'
import { listMaterials, type MaterialItem } from '@/api/wms/material'
import { listWarehouses, type WarehouseItem } from '@/api/wms/warehouse'

const activeTab = ref('list')

const loading = ref(false)
const scanning = ref(false)
const submitting = ref(false)
const rows = ref<WarningItem[]>([])
const keyword = ref('')
const levelFilter = ref('')
const statusFilter = ref('')
const page = ref(1)
const pageSize = ref(10)
const handleVisible = ref(false)
const detailVisible = ref(false)
const current = ref<WarningItem | null>(null)
const handleForm = reactive<{ handler: string; remark: string }>({ handler: '', remark: '' })
const stats = ref<WarningStats>({ total: 0, handled: 0, pending: 0, high: 0 })

const pagedRows = computed(() => rows.value.slice((page.value - 1) * pageSize.value, page.value * pageSize.value))

const ruleLoading = ref(false)
const ruleSubmitting = ref(false)
const rules = ref<WarningRuleItem[]>([])
const ruleKeyword = ref('')
const ruleScopeFilter = ref('')
const ruleEnabledFilter = ref<number | ''>('')
const rulePage = ref(1)
const rulePageSize = ref(10)
const ruleDialogVisible = ref(false)
const ruleDialogMode = ref<'create' | 'edit'>('create')
const materialOptions = ref<MaterialItem[]>([])
const materialLoading = ref(false)
const warehouseOptions = ref<WarehouseItem[]>([])
const ruleForm = reactive({
  id: undefined as number | undefined,
  name: '',
  scope: 'global' as string,
  materialId: undefined as number | undefined,
  warehouseCode: '',
  lowThreshold: 100,
  mediumThreshold: 50,
  highThreshold: 0,
  enabled: 1,
  remark: '',
})

const pagedRules = computed(() => rules.value.slice((rulePage.value - 1) * rulePageSize.value, rulePage.value * rulePageSize.value))

function formatNumber(value?: number) {
  return Number(value || 0).toLocaleString('zh-CN', { maximumFractionDigits: 3 })
}

function levelTagType(level?: WarningLevel) {
  if (level === 'high') return 'danger'
  if (level === 'medium') return 'warning'
  return 'info'
}

function statusTagType(status?: WarningStatus) {
  if (status === 'handled') return 'success'
  if (status === 'resolved') return 'info'
  return 'warning'
}

async function loadData() {
  loading.value = true
  try {
    const [listRes, statsRes] = await Promise.all([
      listWarnings({ keyword: keyword.value.trim() || undefined, level: levelFilter.value || undefined, status: statusFilter.value || undefined }),
      getWarningStats(),
    ])
    if (listRes.code !== 0) {
      ElMessage.error(listRes.message || '加载预警失败')
      return
    }
    rows.value = listRes.data || []
    const maxPage = Math.max(1, Math.ceil(rows.value.length / pageSize.value))
    if (page.value > maxPage) page.value = maxPage
    if (statsRes.code === 0) stats.value = statsRes.data
  } catch (error: any) {
    ElMessage.error(error?.message || '加载预警失败')
  } finally {
    loading.value = false
  }
}

async function loadRules() {
  ruleLoading.value = true
  try {
    const res = await listWarningRules({
      keyword: ruleKeyword.value.trim() || undefined,
      scope: ruleScopeFilter.value || undefined,
      enabled: ruleEnabledFilter.value === '' ? undefined : Number(ruleEnabledFilter.value),
    })
    if (res.code !== 0) {
      ElMessage.error(res.message || '加载预警规则失败')
      return
    }
    rules.value = res.data || []
    const maxPage = Math.max(1, Math.ceil(rules.value.length / rulePageSize.value))
    if (rulePage.value > maxPage) rulePage.value = maxPage
  } catch (error: any) {
    ElMessage.error(error?.message || '加载预警规则失败')
  } finally {
    ruleLoading.value = false
  }
}

async function refreshAll() {
  await Promise.all([loadData(), loadRules()])
}

async function handleScan() {
  scanning.value = true
  try {
    const res = await scanWarnings()
    if (res.code !== 0) {
      ElMessage.error(res.message || '扫描失败')
      return
    }
    const { created, updated, resolved } = res.data
    ElMessage.success(`扫描完成：新增 ${created} 条，更新 ${updated} 条，解除 ${resolved} 条`)
    await loadData()
  } catch (error: any) {
    ElMessage.error(error?.message || '扫描失败')
  } finally {
    scanning.value = false
  }
}

function openDetail(row: WarningItem) {
  current.value = row
  detailVisible.value = true
}

function openHandle(row: WarningItem) {
  current.value = row
  handleForm.handler = ''
  handleForm.remark = ''
  handleVisible.value = true
}

async function submitHandle() {
  if (!current.value?.id) return
  if (!handleForm.handler.trim()) {
    ElMessage.warning('请填写处理人')
    return
  }
  submitting.value = true
  try {
    const res = await handleWarning({ id: current.value.id, handler: handleForm.handler.trim(), remark: handleForm.remark.trim() })
    if (res.code !== 0) {
      ElMessage.error(res.message || '处理失败')
      return
    }
    ElMessage.success('预警已处理')
    handleVisible.value = false
    await loadData()
  } catch (error: any) {
    ElMessage.error(error?.message || '处理失败')
  } finally {
    submitting.value = false
  }
}

async function handleDelete(row: WarningItem) {
  if (!row.id) return
  try {
    await ElMessageBox.confirm(`确认删除 ${row.sku} 的预警记录吗？`, '删除确认', { type: 'warning' })
  } catch {
    return
  }
  const res = await deleteWarning(row.id)
  if (res.code !== 0) {
    ElMessage.error(res.message || '删除失败')
    return
  }
  ElMessage.success('预警已删除')
  await loadData()
}

function resetRuleForm() {
  ruleForm.id = undefined
  ruleForm.name = ''
  ruleForm.scope = 'global'
  ruleForm.materialId = undefined
  ruleForm.warehouseCode = ''
  ruleForm.lowThreshold = 100
  ruleForm.mediumThreshold = 50
  ruleForm.highThreshold = 0
  ruleForm.enabled = 1
  ruleForm.remark = ''
}

/** 打开新建规则弹窗；传入 row 则切换为编辑模式。 */
function openRuleDialog(row?: WarningRuleItem) {
  resetRuleForm()
  if (row) {
    ruleDialogMode.value = 'edit'
    ruleForm.id = row.id
    ruleForm.name = row.name
    ruleForm.scope = row.scope
    ruleForm.materialId = row.materialId ?? undefined
    ruleForm.warehouseCode = row.warehouseCode || ''
    ruleForm.lowThreshold = row.lowThreshold
    ruleForm.mediumThreshold = row.mediumThreshold
    ruleForm.highThreshold = row.highThreshold
    ruleForm.enabled = row.enabled
    ruleForm.remark = row.remark || ''
    if (row.scope === 'material' && row.materialId) {
      ensureMaterialOption(row.materialId, row.materialSku || '', row.materialName || '')
    }
  } else {
    ruleDialogMode.value = 'create'
  }
  ruleDialogVisible.value = true
  ensureMaterialOptions()
  ensureWarehouseOptions()
}

/** 从预警记录快速为该物料创建规则。 */
function openRuleFromWarning(row: WarningItem) {
  detailVisible.value = false
  openRuleDialog()
  ruleForm.scope = 'material'
  ruleForm.materialId = row.materialId
  ensureMaterialOption(row.materialId, row.sku || '', row.name || '')
  ruleForm.name = `${row.name || row.sku || '物料'}预警规则`
  ruleForm.warehouseCode = row.warehouseCode || ''
  ruleForm.lowThreshold = Number(row.threshold || 0)
  ruleForm.mediumThreshold = Math.floor(Number(row.threshold || 0) / 2)
  ruleForm.highThreshold = 0
}

/** 列表页「配置规则」：直接切到规则页签。 */
function openRuleFromList() {
  activeTab.value = 'rules'
  loadRules()
}

function ensureMaterialOption(id?: number | null, sku?: string | null, name?: string | null) {
  if (!id || materialOptions.value.some((item) => item.id === id)) return
  materialOptions.value.unshift({ id, sku: sku || '', name: name || '', category: '', unit: '' })
}

async function ensureMaterialOptions() {
  if (materialOptions.value.length) return
  await searchMaterials('')
}

async function searchMaterials(query: string) {
  materialLoading.value = true
  try {
    const res = await listMaterials({ keyword: query || undefined, page: 1, pageSize: 20 })
    const list = res.code === 0 ? (res.data.rows || []) : []
    // 保留已选物料，避免远程搜索后回显丢失
    const selectedId = ruleForm.materialId
    if (selectedId && !list.some((item) => item.id === selectedId)) {
      const existing = materialOptions.value.find((item) => item.id === selectedId)
      if (existing) list.unshift(existing)
    }
    materialOptions.value = list
  } catch {
    materialOptions.value = []
  } finally {
    materialLoading.value = false
  }
}

async function ensureWarehouseOptions() {
  if (warehouseOptions.value.length) return
  try {
    const res = await listWarehouses({})
    if (res.code === 0) warehouseOptions.value = res.data || []
  } catch {
    warehouseOptions.value = []
  }
}

async function submitRule() {
  if (!ruleForm.name.trim()) {
    ElMessage.warning('请填写规则名称')
    return
  }
  if (ruleForm.scope === 'material' && !ruleForm.materialId) {
    ElMessage.warning('请选择适用物料')
    return
  }
  if (ruleForm.lowThreshold < ruleForm.mediumThreshold || ruleForm.mediumThreshold < ruleForm.highThreshold) {
    ElMessage.warning('阈值需满足：高危 ≤ 中危 ≤ 低危')
    return
  }
  const payload: WarningRuleItem = {
    name: ruleForm.name.trim(),
    scope: ruleForm.scope as WarningRuleScope,
    materialId: ruleForm.scope === 'material' ? ruleForm.materialId ?? null : null,
    warehouseCode: ruleForm.warehouseCode || null,
    lowThreshold: ruleForm.lowThreshold,
    mediumThreshold: ruleForm.mediumThreshold,
    highThreshold: ruleForm.highThreshold,
    enabled: ruleForm.enabled,
    remark: ruleForm.remark.trim() || null,
  }
  ruleSubmitting.value = true
  try {
    const res = ruleDialogMode.value === 'create'
      ? await createWarningRule(payload)
      : await updateWarningRule({ ...payload, id: ruleForm.id })
    if (res.code !== 0) {
      ElMessage.error(res.message || '保存失败')
      return
    }
    ElMessage.success(ruleDialogMode.value === 'create' ? '预警规则已创建' : '预警规则已更新')
    ruleDialogVisible.value = false
    await loadRules()
    ElMessage.info('规则已保存，请点击「扫描预警」重新生成预警')
  } catch (error: any) {
    ElMessage.error(error?.message || '保存失败')
  } finally {
    ruleSubmitting.value = false
  }
}

async function handleToggleRule(row: WarningRuleItem) {
  if (!row.id) return
  const next = row.enabled === 1 ? 0 : 1
  const res = await toggleWarningRule(row.id, next)
  if (res.code !== 0) {
    ElMessage.error(res.message || '操作失败')
    return
  }
  ElMessage.success(next === 1 ? '规则已启用' : '规则已停用')
  await loadRules()
}

async function handleDeleteRule(row: WarningRuleItem) {
  if (!row.id) return
  try {
    await ElMessageBox.confirm(`确认删除规则「${row.name}」吗？`, '删除确认', { type: 'warning' })
  } catch {
    return
  }
  const res = await deleteWarningRule(row.id)
  if (res.code !== 0) {
    ElMessage.error(res.message || '删除失败')
    return
  }
  ElMessage.success('规则已删除')
  await loadRules()
}

onMounted(() => refreshAll())
</script>

<style scoped>
.muted {
  color: var(--pw-text-secondary);
  font-size: 12px;
}
.rule-tip {
  margin-bottom: 16px;
}
.field-hint {
  margin-left: 10px;
  color: var(--pw-text-secondary);
  font-size: 12px;
}
</style>
