<template>
  <section>
    <div class="sub-page-header">
      <div>
        <h2>费用管理</h2>
        <p class="page-desc">管理各部门费用申请、审批与费用类型分析。</p>
      </div>
      <el-space>
        <el-button size="small" plain @click="handleExport">导出</el-button>
        <el-button size="small" type="primary" @click="handleCreate">新增费用</el-button>
      </el-space>
    </div>

    <div class="stats-row">
      <div v-for="item in statCards" :key="item.label" class="stat-card" :class="item.color">
        <div class="stat-header">
          <span class="stat-title">{{ item.label }}</span>
          <div class="stat-icon">{{ item.icon }}</div>
        </div>
        <div class="stat-value">{{ item.value }}</div>
        <div class="stat-footer"><span :class="item.trendType">{{ item.trend }}</span></div>
      </div>
    </div>

    <div class="card">
      <div class="card-header expense-header">
        <div class="card-title">📋 费用明细</div>
        <div class="filter-left">
          <el-date-picker
            v-model="startMonth"
            type="month"
            placeholder="开始月份"
            value-format="YYYY-MM"
            style="width: 150px"
          />
          <span class="range-sep">至</span>
          <el-date-picker
            v-model="endMonth"
            type="month"
            placeholder="结束月份"
            value-format="YYYY-MM"
            style="width: 150px"
          />
        </div>
      </div>

      <el-table :data="expenses" stripe style="width: 100%">
        <el-table-column prop="code" label="费用编号" min-width="150" />
        <el-table-column label="费用类型" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="typeTagType(row.type)" effect="light">{{ row.type }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="关联模块" width="110" align="center">
          <template #default="{ row }"><span class="module-tag">{{ row.module }}</span></template>
        </el-table-column>
        <el-table-column prop="item" label="费用项目" min-width="140" show-overflow-tooltip />
        <el-table-column label="金额" width="110" align="right">
          <template #default="{ row }">{{ formatMoney(row.amount) }}</template>
        </el-table-column>
        <el-table-column prop="date" label="发生日期" width="120" />
        <el-table-column prop="applicant" label="申请人" width="110" />
        <el-table-column label="审批状态" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="row.approved ? 'success' : 'primary'" effect="light">{{ row.approved ? '已审批' : '审批中' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="90" align="center" fixed="right">
          <template #default><el-button link type="primary">查看</el-button></template>
        </el-table-column>
      </el-table>

      <div class="pagination-bar">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          background
          layout="total, sizes, prev, pager, next, jumper"
          :page-sizes="[10, 20, 50, 100]"
          :total="total"
          @current-change="loadList"
          @size-change="handleSizeChange"
        />
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { listExpenses, expenseStats, type Expense } from '@/api/fin/expense'

const startMonth = ref('')
const endMonth = ref('')

const expenses = ref<Expense[]>([])
const stats = ref({ sales: 0, manage: 0, rd: 0, total: 0 })

const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

const statCards = computed(() => [
  { label: '本月销售费用', value: formatMoney(stats.value.sales), trend: '销售费用合计', trendType: '', color: 'blue', icon: '💼' },
  { label: '本月管理费用', value: formatMoney(stats.value.manage), trend: '管理费用合计', trendType: '', color: 'orange', icon: '🏢' },
  { label: '本月研发费用', value: formatMoney(stats.value.rd), trend: '研发费用合计', trendType: '', color: 'purple', icon: '🔬' },
  { label: '本月总费用', value: formatMoney(stats.value.total), trend: '全部费用合计', trendType: '', color: 'red', icon: '📊' },
])

async function loadList() {
  try {
    const res = await listExpenses({
      startMonth: startMonth.value || undefined,
      endMonth: endMonth.value || undefined,
      page: currentPage.value,
      pageSize: pageSize.value,
    })
    expenses.value = res.rows
    total.value = res.total
  } catch (e: any) {
    ElMessage.error(e.message || '加载失败')
  }
}

function handleSizeChange() {
  currentPage.value = 1
  loadList()
}

async function loadAll() {
  try {
    stats.value = await expenseStats()
    await loadList()
  } catch (e: any) {
    ElMessage.error(e.message || '加载失败')
  }
}

onMounted(loadAll)
// 月份筛选变化时回到第 1 页重新请求
watch([startMonth, endMonth], () => {
  currentPage.value = 1
  loadList()
})

function formatMoney(value: number) {
  return `¥${Number(value || 0).toLocaleString()}`
}

function typeTagType(type: string) {
  if (type === '销售费用') return 'primary'
  if (type === '管理费用') return 'warning'
  if (type === '研发费用') return 'info'
  if (type === '财务费用') return 'danger'
  return 'info'
}

function handleExport() {
  ElMessage.success('导出成功')
}

function handleCreate() {
  ElMessage.info('新增费用')
}
</script>

<style scoped>
.expense-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 16px; }
.range-sep { margin: 0 8px; color: var(--text-secondary); }
.module-tag { color: #e6a23c; font-size: 13px; }
.pagination-bar { display: flex; justify-content: flex-end; margin-top: 16px; }
</style>
