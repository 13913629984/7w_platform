<template>
  <section class="table-page">
    <div class="sub-page-header">
      <div>
        <h2>保养计划</h2>
        <p class="page-desc">按周期制定设备保养计划，执行后自动推算下次保养日期并回写设备台账。</p>
      </div>
      <el-space>
        <el-button plain :icon="Refresh" @click="loadData">刷新</el-button>
        <el-button type="primary" :icon="Plus" @click="openAdd">新增计划</el-button>
      </el-space>
    </div>

    <div class="stats-row">
      <div class="stat-card blue">
        <div class="stat-header"><span class="stat-title">计划总数</span><div class="stat-icon">📅</div></div>
        <div class="stat-value">{{ stats.total }}</div>
        <div class="stat-footer">执行中 {{ stats.activeCount }} · 已暂停 {{ stats.pausedCount }}</div>
      </div>
      <div class="stat-card orange">
        <div class="stat-header"><span class="stat-title">7 天内到期</span><div class="stat-icon">⏳</div></div>
        <div class="stat-value">{{ stats.dueSoonCount }}</div>
        <div class="stat-footer">含已逾期 {{ stats.overdueCount }}</div>
      </div>
      <div class="stat-card green">
        <div class="stat-header"><span class="stat-title">累计执行次数</span><div class="stat-icon">🔁</div></div>
        <div class="stat-value">{{ stats.executeCountTotal }}</div>
        <div class="stat-footer">全部计划执行合计</div>
      </div>
      <div class="stat-card purple">
        <div class="stat-header"><span class="stat-title">已完成计划</span><div class="stat-icon">✅</div></div>
        <div class="stat-value">{{ stats.finishedCount }}</div>
        <div class="stat-footer">已结束保养计划</div>
      </div>
    </div>

    <div class="table-card">
      <div class="filter-bar">
        <div class="filter-left">
          <el-input v-model="keyword" clearable placeholder="搜索计划编码/名称/设备/负责人" style="width:300px" :prefix-icon="Search" @keyup.enter="searchData" @clear="searchData" />
          <el-select v-model="statusFilter" clearable placeholder="计划状态" style="width:130px" @change="searchData">
            <el-option v-for="opt in statusOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
          <el-select v-model="cycleFilter" clearable placeholder="周期类型" style="width:120px" @change="searchData">
            <el-option v-for="opt in cycleOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
          <el-input v-model="equipmentKeyword" clearable placeholder="设备编码/名称" style="width:180px" @keyup.enter="searchData" @clear="searchData" />
          <el-button type="primary" :icon="Search" @click="searchData">查询</el-button>
        </div>
      </div>

      <el-table v-loading="loading" :data="rows" stripe border style="width:100%" empty-text="暂无保养计划">
        <el-table-column prop="code" label="计划编码" width="140" />
        <el-table-column prop="name" label="计划名称" min-width="180" show-overflow-tooltip>
          <template #default="{ row }"><b>{{ row.name }}</b></template>
        </el-table-column>
        <el-table-column prop="equipmentName" label="关联设备" min-width="170" show-overflow-tooltip />
        <el-table-column label="保养周期" width="120" align="center">
          <template #default="{ row }">{{ cycleLabel(row.cycleType, row.cycleValue) }}</template>
        </el-table-column>
        <el-table-column prop="manager" label="负责人" width="100" />
        <el-table-column prop="lastExecuteDate" label="上次执行" width="120" />
        <el-table-column prop="nextDate" label="下次保养" width="120" />
        <el-table-column label="执行次数" width="100" align="center">
          <template #default="{ row }"><el-tag size="small" type="success">{{ row.executeCount ?? 0 }}</el-tag></template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" effect="light">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="230" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="success" :disabled="row.status === 'finished'" @click="handleExecute(row)">执行一次</el-button>
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

    <el-dialog v-model="formVisible" :title="formMode === 'add' ? '新增保养计划' : '编辑保养计划'" width="780px" :close-on-click-modal="false" @closed="resetForm">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-row :gutter="20">
          <el-col :span="12"><el-form-item label="计划编码"><el-input v-model="form.code" :disabled="formMode === 'edit'" placeholder="留空自动生成" maxlength="50" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="计划名称" prop="name"><el-input v-model="form.name" maxlength="100" /></el-form-item></el-col>
          <el-col :span="12">
            <el-form-item label="关联设备" prop="equipmentId">
              <el-select v-model="form.equipmentId" clearable filterable placeholder="请选择设备" style="width:100%">
                <el-option v-for="opt in equipmentOptions" :key="opt.id" :label="`${opt.code} ${opt.name}`" :value="opt.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12"><el-form-item label="负责人"><el-input v-model="form.manager" maxlength="50" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="周期类型"><el-select v-model="form.cycleType" placeholder="请选择周期" style="width:100%"><el-option v-for="opt in cycleOptions" :key="opt.value" :label="opt.label" :value="opt.value" /></el-select></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="周期数值"><el-input-number v-model="form.cycleValue" :min="1" :step="1" controls-position="right" style="width:100%" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="开始日期"><el-date-picker v-model="form.startDate" type="date" value-format="YYYY-MM-DD" placeholder="请选择日期" style="width:100%" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="下次保养"><el-date-picker v-model="form.nextDate" type="date" value-format="YYYY-MM-DD" placeholder="留空自动推算" style="width:100%" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="计划状态"><el-select v-model="form.status" placeholder="请选择状态" style="width:100%"><el-option v-for="opt in statusOptions" :key="opt.value" :label="opt.label" :value="opt.value" /></el-select></el-form-item></el-col>
          <el-col :span="24"><el-form-item label="保养项目"><el-input v-model="form.items" type="textarea" :rows="2" maxlength="500" show-word-limit placeholder="多个项目用逗号分隔，如：柜内除尘,端子紧固,绝缘测试" /></el-form-item></el-col>
          <el-col :span="24"><el-form-item label="备注"><el-input v-model="form.remark" type="textarea" :rows="2" maxlength="500" show-word-limit /></el-form-item></el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitForm">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="detailVisible" title="保养计划详情" width="740px">
      <el-descriptions v-if="detail" :column="2" border>
        <el-descriptions-item label="计划编码">{{ detail.code || '-' }}</el-descriptions-item>
        <el-descriptions-item label="计划名称">{{ detail.name }}</el-descriptions-item>
        <el-descriptions-item label="关联设备">{{ detail.equipmentCode || '-' }} {{ detail.equipmentName || '' }}</el-descriptions-item>
        <el-descriptions-item label="负责人">{{ detail.manager || '-' }}</el-descriptions-item>
        <el-descriptions-item label="保养周期">{{ cycleLabel(detail.cycleType, detail.cycleValue) }}</el-descriptions-item>
        <el-descriptions-item label="计划状态">
          <el-tag :type="statusTagType(detail.status)" effect="light">{{ statusLabel(detail.status) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="开始日期">{{ detail.startDate || '-' }}</el-descriptions-item>
        <el-descriptions-item label="下次保养">{{ detail.nextDate || '-' }}</el-descriptions-item>
        <el-descriptions-item label="上次执行日期">{{ detail.lastExecuteDate || '-' }}</el-descriptions-item>
        <el-descriptions-item label="累计执行次数">{{ detail.executeCount ?? 0 }}</el-descriptions-item>
        <el-descriptions-item label="保养项目" :span="2">{{ detail.items || '-' }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ detail.remark || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </section>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'
import {
  createPlan,
  deletePlan,
  executePlan,
  getPlan,
  getPlanEquipmentOptions,
  getPlanStats,
  listPlans,
  updatePlan,
  type EquipmentOption,
  type PlanItem,
  type PlanStats,
} from '@/api/maint/plan'

const statusOptions = [
  { value: 'active', label: '执行中' },
  { value: 'paused', label: '已暂停' },
  { value: 'finished', label: '已完成' },
]

const cycleOptions = [
  { value: 'day', label: '天' },
  { value: 'week', label: '周' },
  { value: 'month', label: '月' },
  { value: 'quarter', label: '季' },
  { value: 'year', label: '年' },
]

const loading = ref(false)
const submitting = ref(false)
const rows = ref<PlanItem[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)
const keyword = ref('')
const statusFilter = ref('')
const cycleFilter = ref('')
const equipmentKeyword = ref('')
const formVisible = ref(false)
const detailVisible = ref(false)
const formMode = ref<'add' | 'edit'>('add')
const formRef = ref<FormInstance>()
const detail = ref<PlanItem | null>(null)
const equipmentOptions = ref<EquipmentOption[]>([])
const stats = ref<PlanStats>({
  total: 0,
  activeCount: 0,
  pausedCount: 0,
  finishedCount: 0,
  dueSoonCount: 0,
  overdueCount: 0,
  executeCountTotal: 0,
})

const form = reactive<PlanItem>(emptyForm())

const rules: FormRules = {
  name: [{ required: true, message: '请填写计划名称', trigger: 'blur' }],
  cycleType: [{ required: true, message: '请选择周期类型', trigger: 'change' }],
}

function emptyForm(): PlanItem {
  return {
    code: '',
    name: '',
    equipmentId: null,
    cycleType: 'month',
    cycleValue: 1,
    startDate: '',
    nextDate: '',
    items: '',
    manager: '',
    status: 'active',
    remark: '',
  }
}

function statusLabel(value?: string) {
  const found = statusOptions.find((opt) => opt.value === value)
  return found ? found.label : value || '-'
}

function statusTagType(value?: string): 'success' | 'info' | 'warning' {
  switch (value) {
    case 'active':
      return 'success'
    case 'paused':
      return 'warning'
    case 'finished':
      return 'info'
    default:
      return 'info'
  }
}

function cycleLabel(cycleType?: string, cycleValue?: number | null) {
  const found = cycleOptions.find((opt) => opt.value === cycleType)
  const unit = found ? found.label : cycleType || '-'
  return `${cycleValue ?? 1} ${unit}`
}

function resetForm() {
  Object.assign(form, emptyForm())
  formRef.value?.clearValidate()
}

async function loadOptions() {
  try {
    const res = await getPlanEquipmentOptions()
    if (res.code !== 0) {
      ElMessage.error(res.message || '加载设备选项失败')
      return
    }
    equipmentOptions.value = res.data || []
  } catch (error: any) {
    ElMessage.error(error?.message || '加载设备选项失败')
  }
}

async function loadData() {
  loading.value = true
  try {
    const [listRes, statsRes] = await Promise.all([
      listPlans({
        keyword: keyword.value.trim() || undefined,
        status: statusFilter.value || undefined,
        cycleType: cycleFilter.value || undefined,
        equipmentKeyword: equipmentKeyword.value.trim() || undefined,
        page: page.value,
        pageSize: pageSize.value,
      }),
      getPlanStats(),
    ])
    if (listRes.code !== 0) {
      ElMessage.error(listRes.message || '加载保养计划失败')
      return
    }
    if (statsRes.code === 0) stats.value = statsRes.data
    rows.value = listRes.data?.rows || []
    total.value = listRes.data?.total || 0
  } catch (error: any) {
    ElMessage.error(error?.message || '加载保养计划失败')
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
  loadData()
}

function handleSizeChange(value: number) {
  pageSize.value = value
  page.value = 1
  loadData()
}

function openAdd() {
  formMode.value = 'add'
  resetForm()
  formVisible.value = true
}

function openEdit(row: PlanItem) {
  formMode.value = 'edit'
  resetForm()
  Object.assign(form, row)
  formVisible.value = true
}

async function openDetail(row: PlanItem) {
  if (!row.id) return
  try {
    const res = await getPlan(row.id)
    if (res.code !== 0) {
      ElMessage.error(res.message || '加载计划详情失败')
      return
    }
    detail.value = res.data
    detailVisible.value = true
  } catch (error: any) {
    ElMessage.error(error?.message || '加载计划详情失败')
  }
}

async function submitForm() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    const payload: PlanItem = { ...form }
    if (!payload.code) delete payload.code
    const res = formMode.value === 'add' ? await createPlan(payload) : await updatePlan(payload)
    if (res.code !== 0) {
      ElMessage.error(res.message || '保存失败')
      return
    }
    ElMessage.success(formMode.value === 'add' ? '保养计划已创建' : '保养计划已更新')
    formVisible.value = false
    await loadData()
  } catch (error: any) {
    ElMessage.error(error?.message || '保存失败')
  } finally {
    submitting.value = false
  }
}

async function handleExecute(row: PlanItem) {
  if (!row.id) return
  try {
    await ElMessageBox.confirm(`确认执行一次「${row.name}」吗？执行后将累加执行次数并推算下次保养日期。`, '执行保养', { type: 'warning' })
  } catch {
    return
  }
  try {
    const res = await executePlan(row.id)
    if (res.code !== 0) {
      ElMessage.error(res.message || '执行失败')
      return
    }
    ElMessage.success(`保养已执行，下次保养日期 ${res.data?.nextDate || '-'}`)
    await loadData()
  } catch (error: any) {
    ElMessage.error(error?.message || '执行失败')
  }
}

async function handleDelete(row: PlanItem) {
  if (!row.id) return
  try {
    await ElMessageBox.confirm(`确认删除保养计划「${row.name}」吗？`, '删除确认', { type: 'warning' })
  } catch {
    return
  }
  try {
    const res = await deletePlan(row.id)
    if (res.code !== 0) {
      ElMessage.error(res.message || '删除失败')
      return
    }
    ElMessage.success('保养计划已删除')
    await loadData()
  } catch (error: any) {
    ElMessage.error(error?.message || '删除失败')
  }
}

onMounted(() => {
  loadOptions()
  loadData()
})
</script>

<style scoped>
.wms-pagination {
  display: flex;
  justify-content: flex-end;
  padding: 16px 0 4px;
}
</style>
