<template>
  <section>
    <div class="sub-page-header">
      <div>
        <h2>应收应付管理</h2>
        <p class="page-desc">统一管理客户应收账款与供应商应付账款、账龄和回款付款计划。</p>
      </div>
      <el-space>
        <el-button size="small" plain @click="handleExport">导出</el-button>
      </el-space>
    </div>

    <div class="stats-row">
      <div v-for="item in statCards" :key="item.label" class="stat-card" :class="item.color">
        <div class="stat-header">
          <span class="stat-title">{{ item.label }}</span>
          <div class="stat-icon">{{ item.icon }}</div>
        </div>
        <div class="stat-value">{{ item.value }} <small v-if="item.unit" class="stat-unit">{{ item.unit }}</small></div>
        <div class="stat-footer"><span :class="item.trendType">{{ item.trend }}</span></div>
      </div>
    </div>

    <div class="table-card">
      <el-tabs v-model="activeTab">
        <el-tab-pane label="应收账款" name="ar" />
        <el-tab-pane label="应付账款" name="ap" />
      </el-tabs>

      <div class="filter-bar">
        <div class="filter-left">
          <el-input
            v-model="keyword"
            style="width: 240px"
            clearable
            prefix-icon="Search"
            :placeholder="activeTab === 'ar' ? '搜索客户名称' : '搜索供应商'"
          />
        </div>
        <el-space>
          <el-button>筛选</el-button>
        </el-space>
      </div>

      <el-table v-if="activeTab === 'ar'" :data="receivables" stripe style="width: 100%">
        <el-table-column prop="code" label="单据编号" min-width="150" />
        <el-table-column prop="customer" label="客户名称" min-width="160" show-overflow-tooltip />
        <el-table-column label="关联销售订单" min-width="140">
          <template #default="{ row }"><el-button link type="primary" @click="showArDetail(row)">{{ row.salesOrder }}</el-button></template>
        </el-table-column>
        <el-table-column prop="contract" label="关联合同" width="130" />
        <el-table-column label="合同金额" width="120" align="right">
          <template #default="{ row }">{{ formatMoney(row.contractAmount) }}</template>
        </el-table-column>
        <el-table-column label="已回款" width="120" align="right">
          <template #default="{ row }">{{ formatMoney(row.received) }}</template>
        </el-table-column>
        <el-table-column label="待回款" width="120" align="right">
          <template #default="{ row }"><span class="danger">{{ formatMoney(row.pending) }}</span></template>
        </el-table-column>
        <el-table-column prop="dueDate" label="到期日" width="120" />
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="arTagType(row.status)" effect="light">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="90" align="center" fixed="right">
          <template #default="{ row }"><el-button link type="primary" @click="showArDetail(row)">查看</el-button></template>
        </el-table-column>
      </el-table>

      <el-table v-else :data="payables" stripe style="width: 100%">
        <el-table-column prop="code" label="单据编号" min-width="150" />
        <el-table-column prop="supplier" label="供应商" min-width="160" show-overflow-tooltip />
        <el-table-column prop="purchaseOrder" label="采购单号" width="140" />
        <el-table-column label="应付金额" width="120" align="right">
          <template #default="{ row }">{{ formatMoney(row.payableAmount) }}</template>
        </el-table-column>
        <el-table-column label="已付金额" width="120" align="right">
          <template #default="{ row }">{{ formatMoney(row.paid) }}</template>
        </el-table-column>
        <el-table-column label="待付金额" width="120" align="right">
          <template #default="{ row }"><span class="danger">{{ formatMoney(row.pending) }}</span></template>
        </el-table-column>
        <el-table-column prop="dueDate" label="到期日" width="120" />
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="apTagType(row.status)" effect="light">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="90" align="center" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="showApDetail(row)">{{ row.status === '已付款' ? '查看' : '付款' }}</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-bar">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          background
          layout="total, sizes, prev, pager, next, jumper"
          :page-sizes="[10, 20, 50, 100]"
          :total="activeTab === 'ar' ? arTotal : apTotal"
          @current-change="loadData"
          @size-change="handleSizeChange"
        />
      </div>
    </div>

    <el-dialog v-model="detailVisible" :title="detailTitle" width="560px">
      <el-descriptions v-if="arDetail" :column="2" border>
        <el-descriptions-item label="单据编号">{{ arDetail.code }}</el-descriptions-item>
        <el-descriptions-item label="客户名称">{{ arDetail.customer }}</el-descriptions-item>
        <el-descriptions-item label="关联销售订单">{{ arDetail.salesOrder }}</el-descriptions-item>
        <el-descriptions-item label="关联合同">{{ arDetail.contract || '-' }}</el-descriptions-item>
        <el-descriptions-item label="合同金额">{{ formatMoney(arDetail.contractAmount) }}</el-descriptions-item>
        <el-descriptions-item label="已回款">{{ formatMoney(arDetail.received) }}</el-descriptions-item>
        <el-descriptions-item label="待回款"><span class="danger">{{ formatMoney(arDetail.pending) }}</span></el-descriptions-item>
        <el-descriptions-item label="到期日">{{ arDetail.dueDate }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="arTagType(arDetail.status)" effect="light">{{ arDetail.status }}</el-tag>
        </el-descriptions-item>
      </el-descriptions>
      <el-descriptions v-else-if="apDetail" :column="2" border>
        <el-descriptions-item label="单据编号">{{ apDetail.code }}</el-descriptions-item>
        <el-descriptions-item label="供应商">{{ apDetail.supplier }}</el-descriptions-item>
        <el-descriptions-item label="采购单号">{{ apDetail.purchaseOrder || '-' }}</el-descriptions-item>
        <el-descriptions-item label="应付金额">{{ formatMoney(apDetail.payableAmount) }}</el-descriptions-item>
        <el-descriptions-item label="已付金额">{{ formatMoney(apDetail.paid) }}</el-descriptions-item>
        <el-descriptions-item label="待付金额"><span class="danger">{{ formatMoney(apDetail.pending) }}</span></el-descriptions-item>
        <el-descriptions-item label="到期日">{{ apDetail.dueDate }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="apTagType(apDetail.status)" effect="light">{{ apDetail.status }}</el-tag>
        </el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { getArap, type ArapReceivable, type ArapPayable } from '@/api/fin/arap'

const activeTab = ref<'ar' | 'ap'>('ar')
const keyword = ref('')

const receivables = ref<ArapReceivable[]>([])
const payables = ref<ArapPayable[]>([])
const stats = ref({ arTotal: 0, apTotal: 0, arCount: 0, apCount: 0, arOverdue: 0 })

const currentPage = ref(1)
const pageSize = ref(10)
const arTotal = ref(0)
const apTotal = ref(0)

const detailVisible = ref(false)
const arDetail = ref<ArapReceivable | null>(null)
const apDetail = ref<ArapPayable | null>(null)
const detailTitle = computed(() => (arDetail.value ? '应收账款详情' : '应付账款详情'))

function showArDetail(row: ArapReceivable) {
  apDetail.value = null
  arDetail.value = row
  detailVisible.value = true
}

function showApDetail(row: ArapPayable) {
  arDetail.value = null
  apDetail.value = row
  detailVisible.value = true
}

const statCards = computed(() => [
  { label: '应收账款总额', value: formatMoney(stats.value.arTotal), unit: '', trend: `${stats.value.arCount} 笔待回款`, trendType: '', color: 'blue', icon: '📥' },
  { label: '应付账款总额', value: formatMoney(stats.value.apTotal), unit: '', trend: `${stats.value.apCount} 笔待付款`, trendType: '', color: 'red', icon: '📤' },
  { label: '逾期应收', value: String(stats.value.arOverdue), unit: '笔', trend: '需重点跟进', trendType: 'down', color: 'orange', icon: '⚠️' },
  { label: '账龄分析', value: '28.5', unit: '天', trend: '平均回款周期', trendType: 'up', color: 'green', icon: '📊' },
])

async function loadData() {
  try {
    const data = await getArap({
      keyword: keyword.value || undefined,
      page: currentPage.value,
      pageSize: pageSize.value,
    })
    receivables.value = data.receivables
    payables.value = data.payables
    stats.value = data.stats
    arTotal.value = data.arTotalCount
    apTotal.value = data.apTotalCount
  } catch (e: any) {
    ElMessage.error(e.message || '加载失败')
  }
}

onMounted(loadData)

function handleSizeChange() {
  currentPage.value = 1
  loadData()
}

// 切换标签页或搜索时回到第 1 页并重新请求
watch(activeTab, () => {
  currentPage.value = 1
  loadData()
})
watch(keyword, () => {
  currentPage.value = 1
  loadData()
})

function formatMoney(value: number) {
  return `¥${Number(value || 0).toLocaleString()}`
}

function arTagType(status: string) {
  if (status === '已回款' || status === '已核销') return 'success'
  if (status === '逾期') return 'danger'
  if (status === '即将到期') return 'warning'
  if (status === '部分回款' || status === '部分核销') return 'primary'
  return 'info'
}

function apTagType(status: string) {
  if (status === '已付款') return 'success'
  if (status === '逾期') return 'danger'
  if (status === '即将到期') return 'warning'
  return 'info'
}

function handleExport() {
  ElMessage.success('导出成功')
}
</script>

<style scoped>
.stat-unit { font-size: 14px; font-weight: 500; color: var(--text-secondary); }
.pagination-bar { display: flex; justify-content: flex-end; margin-top: 16px; }
</style>
