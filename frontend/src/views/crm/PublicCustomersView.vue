<template>
  <section>
    <div class="sub-page-header">
      <div>
        <h2>公海客户</h2>
      </div>
    </div>

    <div class="table-card">
      <div class="filter-bar">
        <div class="filter-left">
          <span class="filter-label">客户姓名</span>
          <el-input
            v-model="filters.name"
            clearable
            placeholder="请输入客户名称"
            style="width:220px"
            @keyup.enter="handleSearch"
            @clear="handleSearch"
          />
        </div>
      </div>

      <el-table v-loading="loading" :data="rows" stripe style="width:100%">
        <el-table-column prop="name" label="客户名称" min-width="180" show-overflow-tooltip />
        <el-table-column label="英文名称" min-width="160" show-overflow-tooltip>
          <template #default="{ row }">{{ row.englishName || '--' }}</template>
        </el-table-column>
        <el-table-column label="客户地址" min-width="280" show-overflow-tooltip>
          <template #default="{ row }">{{ row.address || '--' }}</template>
        </el-table-column>
        <el-table-column label="联系人" width="120">
          <template #default="{ row }">{{ contactName(row) || '--' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="110" align="center" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" :icon="User" @click="openAssign(row)">分配</el-button>
          </template>
        </el-table-column>
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

    <el-dialog v-model="assignVisible" title="客户分配" width="640px" :close-on-click-modal="false">
      <div class="assign-header">
        <span class="assign-customer">客户名称：{{ assignTarget?.name }}</span>
        <el-button size="small" plain :icon="Plus" @click="addSales">添加</el-button>
      </div>
      <div class="sub-table-block">
        <el-table :data="salesList" border size="small">
          <el-table-column label="负责人" width="80" align="center">
            <template #default="{ row }">
              <el-checkbox :model-value="row.isOwner" @change="(val: boolean) => setOwner(row, val)" />
            </template>
          </el-table-column>
          <el-table-column label="销售人员" min-width="200">
            <template #default="{ row }">
              <el-select v-model="row.name" filterable placeholder="请选择" style="width:100%">
                <el-option v-for="opt in salesOptions" :key="opt" :label="opt" :value="opt" />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="职位" min-width="160">
            <template #default="{ row }">
              <el-input v-model="row.position" placeholder="请输入职位" />
            </template>
          </el-table-column>
          <el-table-column label="操作" width="70" align="center">
            <template #default="{ $index }">
              <el-button link type="danger" :icon="Delete" @click="salesList.splice($index, 1)" />
            </template>
          </el-table-column>
        </el-table>
      </div>
      <template #footer>
        <el-button @click="assignVisible = false">关闭</el-button>
        <el-button type="primary" @click="submitAssign">保存</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { User, Plus, Delete } from '@element-plus/icons-vue'
import request from '@/api/request'
import { listCustomers, updateCustomer, type CustomerItem, type CustomerSales } from '@/api/crm/customer'

const loading = ref(false)
const rows = ref<CustomerItem[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)

const filters = reactive({ name: '' })

function contactName(row: CustomerItem) {
  return row.contactList?.[0]?.name || ''
}

async function loadData() {
  loading.value = true
  try {
    const res = await listCustomers({
      name: filters.name.trim() || undefined,
      onlyPublic: true,
      page: page.value,
      pageSize: pageSize.value,
    })
    if (res.code !== 0) {
      ElMessage.error(res.message || '加载公海客户失败')
      return
    }
    rows.value = res.data.rows || []
    total.value = res.data.total || 0
  } catch (error: any) {
    ElMessage.error(error?.message || '加载公海客户失败')
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

const assignVisible = ref(false)
const assignTarget = ref<CustomerItem | null>(null)
const salesList = ref<CustomerSales[]>([])
const salesOptions = ref<string[]>([])

async function loadSalesOptions() {
  if (salesOptions.value.length) return
  try {
    const { data } = await request.get('/system/user/list', { params: { page: 1, pageSize: 200 } })
    if (data.code === 0) {
      salesOptions.value = (data.data.list || [])
        .map((u: any) => u.nickname || u.username)
        .filter(Boolean)
    }
  } catch {
    // 下拉项加载失败时仍可手动输入，忽略
  }
}

function openAssign(row: CustomerItem) {
  assignTarget.value = row
  salesList.value = (row.salesList || []).map((s) => ({ ...s }))
  if (!salesList.value.length) {
    salesList.value.push({ name: '', position: '', isOwner: true })
  }
  loadSalesOptions()
  assignVisible.value = true
}

function addSales() {
  salesList.value.push({ name: '', position: '', isOwner: salesList.value.length === 0 })
}

function setOwner(row: CustomerSales, val: boolean) {
  if (val) {
    salesList.value.forEach((s) => (s.isOwner = s === row))
  } else {
    row.isOwner = false
  }
}

async function submitAssign() {
  if (!assignTarget.value?.id) return
  const valid = salesList.value.filter((s) => s.name && s.name.trim())
  if (!valid.length) {
    ElMessage.warning('请至少选择一名销售人员')
    return
  }
  if (!valid.some((s) => s.isOwner)) {
    ElMessage.warning('请勾选一名负责人')
    return
  }
  try {
    const res = await updateCustomer({ ...assignTarget.value, salesList: valid })
    if (res.code !== 0) {
      ElMessage.error(res.message || '分配失败')
      return
    }
    ElMessage.success('分配成功')
    assignVisible.value = false
    loadData()
  } catch (error: any) {
    ElMessage.error(error?.message || '分配失败')
  }
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
.assign-tip { margin: 0 0 12px; color: #606266; font-size: 14px; }
.assign-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}
.assign-customer { font-size: 14px; color: #303133; }
</style>
