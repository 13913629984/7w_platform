<template>
  <section>
    <div class="sub-page-header">
      <div>
        <h2>库存预警</h2>
        <p class="page-desc">监控安全库存，自动识别缺货与低库存物料并跟踪处理状态。</p>
      </div>
      <el-space>
        <el-button plain :icon="Refresh" @click="loadData">刷新</el-button>
        <el-button type="primary" :icon="Search" :loading="scanning" @click="handleScan">扫描预警</el-button>
      </el-space>
    </div>

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
      </div>

      <el-table v-loading="loading" :data="pagedRows" border stripe style="width:100%" empty-text="暂无预警，可点击「扫描预警」生成">
        <el-table-column prop="sku" label="SKU编码" width="130" />
        <el-table-column prop="name" label="物料名称" min-width="150"><template #default="{ row }"><b>{{ row.name }}</b></template></el-table-column>
        <el-table-column prop="spec" label="规格型号" width="120" />
        <el-table-column prop="warehouse" label="仓库" width="130" />
        <el-table-column prop="currentQty" label="当前库存" width="100" align="right"><template #default="{ row }">{{ formatNumber(row.currentQty) }}</template></el-table-column>
        <el-table-column prop="threshold" label="预警阈值" width="100" align="right" />
        <el-table-column label="预警级别" width="100" align="center"><template #default="{ row }"><el-tag :type="levelTagType(row.level)" effect="dark">{{ row.levelName }}</el-tag></template></el-table-column>
        <el-table-column prop="warningTime" label="预警时间" width="170" />
        <el-table-column label="状态" width="100" align="center"><template #default="{ row }"><el-tag :type="statusTagType(row.status)" effect="light">{{ row.statusName }}</el-tag></template></el-table-column>
        <el-table-column label="操作" width="170" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">详情</el-button>
            <el-button v-if="row.status === 'pending'" link type="success" @click="openHandle(row)">处理</el-button>
            <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="wms-pagination">
        <el-pagination v-model:current-page="page" v-model:page-size="pageSize" :total="rows.length" :page-sizes="[10, 20, 50, 100]" background layout="total, sizes, prev, pager, next, jumper" />
      </div>
    </div>

    <el-dialog v-model="handleVisible" title="处理预警" width="520px" :close-on-click-modal="false">
      <el-form v-if="current" label-width="92px">
        <el-form-item label="物料">{{ current.sku }} / {{ current.name }}</el-form-item>
        <el-form-item label="当前库存">{{ formatNumber(current.currentQty) }} / 安全库存 {{ current.threshold }}</el-form-item>
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
        <el-descriptions-item label="预警级别"><el-tag :type="levelTagType(current.level)" effect="dark">{{ current.levelName }}</el-tag></el-descriptions-item>
        <el-descriptions-item label="状态"><el-tag :type="statusTagType(current.status)" effect="light">{{ current.statusName }}</el-tag></el-descriptions-item>
        <el-descriptions-item label="预警时间">{{ current.warningTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="处理人">{{ current.handler || '-' }}</el-descriptions-item>
        <el-descriptions-item label="处理时间" :span="2">{{ current.handleTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="处理说明" :span="2">{{ current.handleRemark || '-' }}</el-descriptions-item>
      </el-descriptions>
      <template #footer><el-button @click="detailVisible = false">关闭</el-button></template>
    </el-dialog>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh, Search } from '@element-plus/icons-vue'
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

onMounted(() => loadData())
</script>
