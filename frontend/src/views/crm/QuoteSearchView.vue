<template>
  <section>
    <div class="sub-page-header">
      <div>
        <h2>报价查询</h2>
      </div>
    </div>

    <div class="table-card">
      <div class="filter-bar">
        <div class="filter-left">
          <span class="filter-label">客户姓名</span>
          <el-input
            v-model="filters.customerName"
            clearable
            placeholder="请输入客户名称"
            style="width:200px"
            @keyup.enter="handleSearch"
            @clear="handleSearch"
          />
          <span class="filter-label">品牌</span>
          <el-input
            v-model="filters.brand"
            clearable
            placeholder="请输入品牌"
            style="width:160px"
            @keyup.enter="handleSearch"
            @clear="handleSearch"
          />
          <span class="filter-label">型号</span>
          <el-input
            v-model="filters.model"
            clearable
            placeholder="请输入型号"
            style="width:160px"
            @keyup.enter="handleSearch"
            @clear="handleSearch"
          />
          <el-button type="primary" @click="handleSearch">查询</el-button>
        </div>
      </div>

      <el-table v-loading="loading" :data="rows" stripe style="width:100%">
        <el-table-column prop="customerName" label="客户名称" min-width="220" show-overflow-tooltip />
        <el-table-column prop="brand" label="品牌" min-width="160" show-overflow-tooltip />
        <el-table-column prop="model" label="型号" min-width="160" show-overflow-tooltip />
        <el-table-column label="报价" width="140" align="right">
          <template #default="{ row }">{{ formatPrice(row.price) }}</template>
        </el-table-column>
        <el-table-column prop="visitSubject" label="拜访主题" min-width="220" show-overflow-tooltip />
      </el-table>

      <div class="crm-pagination">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          background
          layout="total, prev, pager, next, jumper"
          @current-change="handlePageChange"
          @size-change="handleSizeChange"
        />
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { listQuotes, type QuoteItem } from '@/api/crm/quote'

const loading = ref(false)
const rows = ref<QuoteItem[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)

const filters = reactive({ customerName: '', brand: '', model: '' })

function formatPrice(price: number) {
  if (price == null) return '--'
  return Number(price).toFixed(2)
}

async function loadData() {
  loading.value = true
  try {
    const res = await listQuotes({
      customerName: filters.customerName.trim() || undefined,
      brand: filters.brand.trim() || undefined,
      model: filters.model.trim() || undefined,
      page: page.value,
      pageSize: pageSize.value,
    })
    if (res.code !== 0) {
      ElMessage.error(res.message || '查询报价失败')
      return
    }
    rows.value = res.data.rows || []
    total.value = res.data.total || 0
  } catch (error: any) {
    ElMessage.error(error?.message || '查询报价失败')
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  page.value = 1
  loadData()
}

function handlePageChange(value: number) {
  page.value = value
  loadData()
}

function handleSizeChange(value: number) {
  pageSize.value = value
  page.value = 1
  loadData()
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.table-card {
  background: #fff;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, .04);
  min-width: 0;
}
.table-card .el-table { width: 100%; }
.filter-bar {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 16px;
}
.filter-left {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}
.filter-label { font-size: 14px; color: #606266; white-space: nowrap; }
.crm-pagination {
  display: flex;
  justify-content: flex-end;
  padding: 16px 0 4px;
}
</style>
