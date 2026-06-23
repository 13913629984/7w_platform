<template>
  <section>
    <div class="sub-page-header">
      <div>
        <h2>品牌管理</h2>
        <p class="page-desc">维护物料品牌、供应商关系和品牌启停状态。</p>
      </div>
      <el-space>
        <el-button plain :icon="Refresh" @click="loadData">刷新</el-button>
        <el-button type="primary" :icon="Plus" @click="openAdd">新增品牌</el-button>
      </el-space>
    </div>

    <div class="stats-row">
      <div class="stat-card blue">
        <div class="stat-header"><span class="stat-title">品牌总数</span><div class="stat-icon">🏷️</div></div>
        <div class="stat-value">{{ stats.total }}</div>
        <div class="stat-footer">已维护品牌数</div>
      </div>
      <div class="stat-card green">
        <div class="stat-header"><span class="stat-title">启用品牌</span><div class="stat-icon">✅</div></div>
        <div class="stat-value">{{ stats.activeCount }}</div>
        <div class="stat-footer">业务可用品牌</div>
      </div>
      <div class="stat-card orange">
        <div class="stat-header"><span class="stat-title">禁用品牌</span><div class="stat-icon">🚫</div></div>
        <div class="stat-value">{{ stats.inactiveCount }}</div>
        <div class="stat-footer">已停用品牌</div>
      </div>
      <div class="stat-card purple">
        <div class="stat-header"><span class="stat-title">关联物料</span><div class="stat-icon">📦</div></div>
        <div class="stat-value">{{ stats.materialTotal }}</div>
        <div class="stat-footer">已绑定物料数量</div>
      </div>
    </div>

    <div class="table-card">
      <div class="filter-bar">
        <div class="filter-left">
          <el-input v-model="keyword" clearable placeholder="搜索编码/名称/英文名/供应商" style="width:280px" :prefix-icon="Search" @keyup.enter="searchData" @clear="searchData" />
          <el-select v-model="statusFilter" clearable placeholder="状态" style="width:120px" @change="searchData">
            <el-option label="启用" :value="1" />
            <el-option label="禁用" :value="0" />
          </el-select>
          <el-button type="primary" :icon="Search" @click="searchData">查询</el-button>
        </div>
      </div>

      <el-table v-loading="loading" :data="pagedRows" stripe border style="width:100%">
        <el-table-column prop="code" label="品牌编码" width="140" />
        <el-table-column prop="name" label="品牌名称" min-width="130">
          <template #default="{ row }"><b>{{ row.name }}</b></template>
        </el-table-column>
        <el-table-column prop="englishName" label="英文名" min-width="130" />
        <el-table-column prop="country" label="国家/地区" width="110" />
        <el-table-column prop="supplier" label="默认供应商" min-width="150" />
        <el-table-column prop="contact" label="联系人" width="100" />
        <el-table-column prop="phone" label="联系电话" width="130" />
        <el-table-column label="物料数" width="90" align="center">
          <template #default="{ row }"><el-tag size="small" type="success">{{ row.materialCount ?? 0 }}</el-tag></template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-switch :model-value="row.status === 1" @change="(val: boolean) => toggleStatus(row, val)" />
          </template>
        </el-table-column>
        <el-table-column prop="sort" label="排序" width="80" align="center" />
        <el-table-column label="操作" width="170" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">详情</el-button>
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="wms-pagination">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[10, 20, 50, 100]"
          background
          layout="total, sizes, prev, pager, next, jumper"
          @current-change="handlePageChange"
          @size-change="handleSizeChange"
        />
      </div>
    </div>

    <el-dialog v-model="formVisible" :title="formMode === 'add' ? '新增品牌' : '编辑品牌'" width="720px" :close-on-click-modal="false" @closed="resetForm">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-row :gutter="20">
          <el-col :span="12"><el-form-item label="品牌编码" prop="code"><el-input v-model="form.code" :disabled="formMode === 'edit'" maxlength="50" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="品牌名称" prop="name"><el-input v-model="form.name" maxlength="100" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="英文名"><el-input v-model="form.englishName" maxlength="100" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="国家/地区"><el-input v-model="form.country" maxlength="50" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="默认供应商"><el-input v-model="form.supplier" maxlength="100" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="联系人"><el-input v-model="form.contact" maxlength="50" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="联系电话"><el-input v-model="form.phone" maxlength="50" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="排序"><el-input-number v-model="form.sort" :min="0" :step="1" controls-position="right" style="width:100%" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="状态"><el-switch v-model="form.status" :active-value="1" :inactive-value="0" active-text="启用" inactive-text="禁用" /></el-form-item></el-col>
          <el-col :span="24"><el-form-item label="Logo地址"><el-input v-model="form.logo" maxlength="255" placeholder="可选：图片URL" /></el-form-item></el-col>
          <el-col :span="24"><el-form-item label="品牌描述"><el-input v-model="form.description" type="textarea" :rows="3" maxlength="500" show-word-limit /></el-form-item></el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitForm">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="detailVisible" title="品牌详情" width="640px">
      <el-descriptions v-if="detail" :column="2" border>
        <el-descriptions-item label="品牌编码">{{ detail.code }}</el-descriptions-item>
        <el-descriptions-item label="品牌名称">{{ detail.name }}</el-descriptions-item>
        <el-descriptions-item label="英文名">{{ detail.englishName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="国家/地区">{{ detail.country || '-' }}</el-descriptions-item>
        <el-descriptions-item label="默认供应商">{{ detail.supplier || '-' }}</el-descriptions-item>
        <el-descriptions-item label="联系人">{{ detail.contact || '-' }}</el-descriptions-item>
        <el-descriptions-item label="联系电话">{{ detail.phone || '-' }}</el-descriptions-item>
        <el-descriptions-item label="物料数">{{ detail.materialCount ?? 0 }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ detail.status === 1 ? '启用' : '禁用' }}</el-descriptions-item>
        <el-descriptions-item label="排序">{{ detail.sort ?? 0 }}</el-descriptions-item>
        <el-descriptions-item label="描述" :span="2">{{ detail.description || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'
import { changeBrandStatus, createBrand, deleteBrand, getBrandStats, listBrands, updateBrand, type BrandItem, type BrandStats } from '@/api/wms/brand'

const loading = ref(false)
const submitting = ref(false)
const rows = ref<BrandItem[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)
const keyword = ref('')
const statusFilter = ref<number | null>(null)
const formVisible = ref(false)
const detailVisible = ref(false)
const formMode = ref<'add' | 'edit'>('add')
const formRef = ref<FormInstance>()
const detail = ref<BrandItem | null>(null)
const stats = ref<BrandStats>({ total: 0, activeCount: 0, inactiveCount: 0, materialTotal: 0 })

const form = reactive<BrandItem>(emptyForm())
const pagedRows = computed(() => rows.value.slice((page.value - 1) * pageSize.value, page.value * pageSize.value))

const rules: FormRules = {
  code: [{ required: true, message: '请填写品牌编码', trigger: 'blur' }],
  name: [{ required: true, message: '请填写品牌名称', trigger: 'blur' }],
}

function emptyForm(): BrandItem {
  return { code: '', name: '', englishName: '', logo: '', country: '', supplier: '', contact: '', phone: '', description: '', sort: 0, status: 1 }
}

function resetForm() {
  Object.assign(form, emptyForm())
  formRef.value?.clearValidate()
}

async function loadData() {
  loading.value = true
  try {
    const [listRes, statsRes] = await Promise.all([
      listBrands({ keyword: keyword.value.trim() || undefined, status: statusFilter.value }),
      getBrandStats(),
    ])
    if (listRes.code !== 0) {
      ElMessage.error(listRes.message || '加载品牌失败')
      return
    }
    if (statsRes.code === 0) stats.value = statsRes.data
    rows.value = listRes.data || []
    total.value = rows.value.length
    const maxPage = Math.max(1, Math.ceil(total.value / pageSize.value))
    if (page.value > maxPage) page.value = maxPage
  } catch (error: any) {
    ElMessage.error(error?.message || '加载品牌失败')
  } finally {
    loading.value = false
  }
}

function searchData() {
  page.value = 1
  loadData()
}

function handlePageChange(value: number) {
  page.value = value
}

function handleSizeChange(value: number) {
  pageSize.value = value
  page.value = 1
}

function openAdd() {
  formMode.value = 'add'
  resetForm()
  formVisible.value = true
}

function openEdit(row: BrandItem) {
  formMode.value = 'edit'
  resetForm()
  Object.assign(form, row)
  formVisible.value = true
}

function openDetail(row: BrandItem) {
  detail.value = row
  detailVisible.value = true
}

async function submitForm() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    const res = formMode.value === 'add' ? await createBrand({ ...form }) : await updateBrand({ ...form })
    if (res.code !== 0) {
      ElMessage.error(res.message || '保存失败')
      return
    }
    ElMessage.success(formMode.value === 'add' ? '品牌已创建' : '品牌已更新')
    formVisible.value = false
    await loadData()
  } catch (error: any) {
    ElMessage.error(error?.message || '保存失败')
  } finally {
    submitting.value = false
  }
}

async function handleDelete(row: BrandItem) {
  if (!row.id) return
  try {
    await ElMessageBox.confirm(`确认删除品牌 ${row.name} 吗？`, '删除确认', { type: 'warning' })
  } catch {
    return
  }
  try {
    const res = await deleteBrand(row.id)
    if (res.code !== 0) {
      ElMessage.error(res.message || '删除失败')
      return
    }
    ElMessage.success('品牌已删除')
    await loadData()
  } catch (error: any) {
    ElMessage.error(error?.message || '删除失败')
  }
}

async function toggleStatus(row: BrandItem, val: boolean) {
  if (!row.id) return
  const previous = row.status
  row.status = val ? 1 : 0
  try {
    const res = await changeBrandStatus(row.id, val ? 1 : 0)
    if (res.code !== 0) {
      row.status = previous
      ElMessage.error(res.message || '状态更新失败')
      return
    }
    ElMessage.success(val ? '已启用' : '已禁用')
    await loadData()
  } catch (error: any) {
    row.status = previous
    ElMessage.error(error?.message || '状态更新失败')
  }
}

onMounted(() => loadData())
</script>

<style scoped>
.wms-pagination {
  display: flex;
  justify-content: flex-end;
  padding: 16px 0 4px;
}
</style>
