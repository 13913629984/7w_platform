<template>
  <section class="table-page">
    <div class="sub-page-header">
      <div>
        <h2>测试报告</h2>
        <p class="page-desc">生成、审核与签发电气测试报告，汇总合格率与判定结论。</p>
      </div>
      <el-space>
        <el-button plain :icon="Refresh" @click="loadData">刷新</el-button>
        <el-button type="primary" :icon="Plus" @click="openAdd">新增报告</el-button>
      </el-space>
    </div>

    <div class="stats-row">
      <div class="stat-card blue">
        <div class="stat-header"><span class="stat-title">报告总数</span><div class="stat-icon">📄</div></div>
        <div class="stat-value">{{ stats.total }}</div>
        <div class="stat-footer">草稿 {{ stats.draft }} 份</div>
      </div>
      <div class="stat-card orange">
        <div class="stat-header"><span class="stat-title">待审核</span><div class="stat-icon">⏳</div></div>
        <div class="stat-value">{{ stats.pending }}</div>
        <div class="stat-footer">已驳回 {{ stats.rejected }} 份</div>
      </div>
      <div class="stat-card green">
        <div class="stat-header"><span class="stat-title">已通过</span><div class="stat-icon">✅</div></div>
        <div class="stat-value">{{ stats.approved }}</div>
        <div class="stat-footer">累计通过 {{ stats.totalPass }} 件样品</div>
      </div>
      <div class="stat-card purple">
        <div class="stat-header"><span class="stat-title">合格率</span><div class="stat-icon">📈</div></div>
        <div class="stat-value">{{ passRateLabel }}</div>
        <div class="stat-footer">合格数 {{ stats.totalPass }} / 抽样 {{ stats.totalSample }}</div>
      </div>
    </div>

    <div class="table-card">
      <div class="filter-bar">
        <div class="filter-left">
          <el-input v-model="keyword" clearable placeholder="搜索报告编号/登记号/客户/产品/检验员" style="width:300px" :prefix-icon="Search" @keyup.enter="searchData" @clear="searchData" />
          <el-select v-model="statusFilter" clearable placeholder="报告状态" style="width:140px" @change="searchData">
            <el-option v-for="opt in statusOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
          <el-input v-model="customerFilter" clearable placeholder="客户名称" style="width:180px" @keyup.enter="searchData" @clear="searchData" />
          <el-button type="primary" :icon="Search" @click="searchData">查询</el-button>
        </div>
      </div>

      <el-table v-loading="loading" :data="rows" stripe border style="width:100%">
        <el-table-column prop="reportNo" label="报告编号" width="150">
          <template #default="{ row }"><b>{{ row.reportNo || '-' }}</b></template>
        </el-table-column>
        <el-table-column prop="registerNo" label="登记单号" width="140" />
        <el-table-column prop="customerName" label="客户名称" min-width="150" show-overflow-tooltip />
        <el-table-column prop="product" label="产品名称" min-width="140" show-overflow-tooltip />
        <el-table-column prop="batchNo" label="批次号" width="120" />
        <el-table-column label="合格/不合格" width="120" align="center">
          <template #default="{ row }">
            <span class="ets-pass">{{ row.passQty ?? 0 }}</span> / <span class="ets-fail">{{ row.failQty ?? 0 }}</span>
          </template>
        </el-table-column>
        <el-table-column label="合格率" width="100" align="center">
          <template #default="{ row }">{{ rateText(row.passRate) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }"><el-tag :type="statusTagType(row.status)" effect="light">{{ statusLabel(row.status) }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="inspector" label="检验员" width="100" />
        <el-table-column prop="issueDate" label="签发时间" width="150" align="center" />
        <el-table-column label="操作" width="270" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">详情</el-button>
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="warning" @click="openApprove(row)">审核</el-button>
            <el-button link type="success" @click="handleIssue(row)">签发</el-button>
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

    <el-dialog v-model="formVisible" :title="formMode === 'add' ? '新增测试报告' : '编辑测试报告'" width="820px" :close-on-click-modal="false" @closed="resetForm">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="关联登记">
              <el-select v-model="form.registerId" filterable clearable placeholder="选择来货登记" style="width:100%" @change="onRegisterChange">
                <el-option v-for="opt in registerOptions" :key="opt.id" :label="`${opt.registerNo} · ${opt.customerName || ''}`" :value="opt.id">
                  <span>{{ opt.registerNo }}</span>
                  <span style="float:right;color:#909399;font-size:12px">{{ opt.product || '' }}</span>
                </el-option>
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12"><el-form-item label="报告编号"><el-input v-model="form.reportNo" maxlength="50" placeholder="留空自动生成" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="客户名称" prop="customerName"><el-input v-model="form.customerName" maxlength="100" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="产品名称" prop="product"><el-input v-model="form.product" maxlength="100" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="规格型号"><el-input v-model="form.model" maxlength="100" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="批次号"><el-input v-model="form.batchNo" maxlength="50" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="来货数量"><el-input-number v-model="form.quantity" :min="0" :step="1" controls-position="right" style="width:100%" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="抽样数量"><el-input-number v-model="form.sampleQty" :min="0" :step="1" controls-position="right" style="width:100%" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="合格数量"><el-input-number v-model="form.passQty" :min="0" :step="1" controls-position="right" style="width:100%" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="不合格数量"><el-input-number v-model="form.failQty" :min="0" :step="1" controls-position="right" style="width:100%" /></el-form-item></el-col>
          <el-col :span="12">
            <el-form-item label="报告状态">
              <el-select v-model="form.status" placeholder="请选择状态" style="width:100%">
                <el-option v-for="opt in statusOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12"><el-form-item label="检验员"><el-input v-model="form.inspector" maxlength="50" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="签发时间"><el-date-picker v-model="form.issueDate" type="datetime" value-format="YYYY-MM-DD HH:mm" placeholder="选择时间" style="width:100%" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="审批人"><el-input v-model="form.approver" maxlength="50" /></el-form-item></el-col>
          <el-col :span="24"><el-form-item label="判定结论"><el-input v-model="form.conclusion" type="textarea" :rows="2" maxlength="300" show-word-limit /></el-form-item></el-col>
          <el-col :span="24"><el-form-item label="备注"><el-input v-model="form.remark" type="textarea" :rows="3" maxlength="500" show-word-limit /></el-form-item></el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitForm">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="approveVisible" title="报告审核" width="560px" :close-on-click-modal="false">
      <el-form ref="approveFormRef" :model="approveForm" :rules="approveRules" label-width="90px">
        <el-form-item label="报告编号"><el-input :model-value="approveTarget?.reportNo || '-'" disabled /></el-form-item>
        <el-form-item label="客户名称"><el-input :model-value="approveTarget?.customerName || '-'" disabled /></el-form-item>
        <el-form-item label="审核结论" prop="action">
          <el-radio-group v-model="approveForm.action">
            <el-radio value="approve">通过</el-radio>
            <el-radio value="reject">驳回</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="审批人"><el-input v-model="approveForm.approver" maxlength="50" /></el-form-item>
        <el-form-item label="审核意见"><el-input v-model="approveForm.remark" type="textarea" :rows="3" maxlength="500" show-word-limit /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="approveVisible = false">取消</el-button>
        <el-button type="primary" :loading="approveSubmitting" @click="submitApprove">提交审核</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="detailVisible" title="测试报告详情" width="760px">
      <el-descriptions v-if="detail" :column="2" border>
        <el-descriptions-item label="报告编号">{{ detail.reportNo || '-' }}</el-descriptions-item>
        <el-descriptions-item label="报告状态">
          <el-tag :type="statusTagType(detail.status)" effect="light">{{ statusLabel(detail.status) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="登记单号">{{ detail.registerNo || '-' }}</el-descriptions-item>
        <el-descriptions-item label="客户名称">{{ detail.customerName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="产品名称">{{ detail.product || '-' }}</el-descriptions-item>
        <el-descriptions-item label="规格型号">{{ detail.model || '-' }}</el-descriptions-item>
        <el-descriptions-item label="批次号">{{ detail.batchNo || '-' }}</el-descriptions-item>
        <el-descriptions-item label="来货数量">{{ detail.quantity ?? 0 }}</el-descriptions-item>
        <el-descriptions-item label="抽样数量">{{ detail.sampleQty ?? 0 }}</el-descriptions-item>
        <el-descriptions-item label="合格数量">{{ detail.passQty ?? 0 }}</el-descriptions-item>
        <el-descriptions-item label="不合格数量">{{ detail.failQty ?? 0 }}</el-descriptions-item>
        <el-descriptions-item label="合格率">{{ rateText(detail.passRate) }}</el-descriptions-item>
        <el-descriptions-item label="判定结论" :span="2">{{ detail.conclusion || '-' }}</el-descriptions-item>
        <el-descriptions-item label="检验员">{{ detail.inspector || '-' }}</el-descriptions-item>
        <el-descriptions-item label="签发时间">{{ detail.issueDate || '-' }}</el-descriptions-item>
        <el-descriptions-item label="审批人">{{ detail.approver || '-' }}</el-descriptions-item>
        <el-descriptions-item label="审批时间">{{ detail.approveTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ detail.createTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ detail.remark || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'
import {
  approveReport,
  createReport,
  deleteReport,
  getReport,
  getReportRegisterOptions,
  issueReport,
  listReports,
  updateReport,
  type ReportApprovePayload,
  type ReportItem,
  type ReportRegisterOption,
  type ReportStats,
} from '@/api/ets/report'

const statusOptions = [
  { value: 'draft', label: '草稿' },
  { value: 'pending', label: '待审核' },
  { value: 'approved', label: '已通过' },
  { value: 'rejected', label: '已驳回' },
]
const STATUS_TAG: Record<string, string> = { draft: 'info', pending: 'warning', approved: 'success', rejected: 'danger' }

const loading = ref(false)
const submitting = ref(false)
const approveSubmitting = ref(false)
const rows = ref<ReportItem[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)
const keyword = ref('')
const statusFilter = ref('')
const customerFilter = ref('')
const registerOptions = ref<ReportRegisterOption[]>([])
const formVisible = ref(false)
const detailVisible = ref(false)
const approveVisible = ref(false)
const formMode = ref<'add' | 'edit'>('add')
const formRef = ref<FormInstance>()
const approveFormRef = ref<FormInstance>()
const detail = ref<ReportItem | null>(null)
const approveTarget = ref<ReportItem | null>(null)
const stats = ref<ReportStats>({ total: 0, draft: 0, pending: 0, approved: 0, rejected: 0, totalSample: 0, totalPass: 0, totalQuantity: 0, passRate: 0 })

const form = reactive<ReportItem>(emptyForm())
const approveForm = reactive<ReportApprovePayload>({ id: 0, action: 'approve', approver: '', remark: '' })

const passRateLabel = computed(() => `${Number(stats.value.passRate ?? 0).toFixed(1)}%`)

const rules: FormRules = {
  customerName: [{ required: true, message: '请填写客户名称', trigger: 'blur' }],
  product: [{ required: true, message: '请填写产品名称', trigger: 'blur' }],
}
const approveRules: FormRules = {
  action: [{ required: true, message: '请选择审核结论', trigger: 'change' }],
}

function emptyForm(): ReportItem {
  return {
    reportNo: '',
    registerId: null,
    registerNo: '',
    customerName: '',
    product: '',
    model: '',
    batchNo: '',
    quantity: 0,
    sampleQty: 0,
    passQty: 0,
    failQty: 0,
    status: 'draft',
    conclusion: '',
    inspector: '',
    issueDate: '',
    approver: '',
    remark: '',
  }
}

function statusLabel(value?: string) {
  const found = statusOptions.find((item) => item.value === value)
  return found ? found.label : value || '草稿'
}

function statusTagType(value?: string) {
  return STATUS_TAG[value || 'draft'] || 'info'
}

/** 后端 passRate 已是 0-100 的百分比，直接格式化，不再乘 100 */
function rateText(value?: number | string | null) {
  if (value === null || value === undefined || value === '') return '0.0%'
  return `${Number(value).toFixed(1)}%`
}

function resetForm() {
  Object.assign(form, emptyForm())
  delete form.id
  formRef.value?.clearValidate()
}

function onRegisterChange(value: number | null | undefined) {
  const option = registerOptions.value.find((item) => item.id === value)
  if (!option) return
  form.registerNo = option.registerNo
  if (option.customerName && !form.customerName) form.customerName = option.customerName
  if (option.product && !form.product) form.product = option.product
  if (option.model && !form.model) form.model = option.model
  if (option.batchNo && !form.batchNo) form.batchNo = option.batchNo
  if ((form.quantity ?? 0) === 0) form.quantity = option.quantity ?? 0
  if ((form.sampleQty ?? 0) === 0) form.sampleQty = option.sampleQty ?? 0
}

async function loadRegisterOptions() {
  try {
    const res = await getReportRegisterOptions()
    if (res.code !== 0) {
      ElMessage.error(res.message || '加载来货登记选项失败')
      return
    }
    registerOptions.value = res.data || []
  } catch (error: any) {
    ElMessage.error(error?.message || '加载来货登记选项失败')
  }
}

async function loadData() {
  loading.value = true
  try {
    const res = await listReports({
      keyword: keyword.value.trim() || undefined,
      status: statusFilter.value || undefined,
      customerName: customerFilter.value.trim() || undefined,
      page: page.value,
      pageSize: pageSize.value,
    })
    if (res.code !== 0) {
      ElMessage.error(res.message || '加载测试报告失败')
      return
    }
    rows.value = res.data?.rows || []
    total.value = Number(res.data?.total || 0)
    if (res.data?.stats) stats.value = res.data.stats
  } catch (error: any) {
    ElMessage.error(error?.message || '加载测试报告失败')
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

function openEdit(row: ReportItem) {
  formMode.value = 'edit'
  resetForm()
  Object.assign(form, row)
  form.registerId = row.registerId ?? null
  formVisible.value = true
}

async function openDetail(row: ReportItem) {
  if (!row.id) return
  try {
    const res = await getReport(row.id)
    if (res.code !== 0) {
      ElMessage.error(res.message || '加载详情失败')
      return
    }
    detail.value = res.data
    detailVisible.value = true
  } catch (error: any) {
    ElMessage.error(error?.message || '加载详情失败')
  }
}

function openApprove(row: ReportItem) {
  if (!row.id) return
  approveTarget.value = row
  approveForm.id = row.id
  approveForm.action = 'approve'
  approveForm.approver = row.approver || ''
  approveForm.remark = ''
  approveFormRef.value?.clearValidate()
  approveVisible.value = true
}

async function submitForm() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    const res = formMode.value === 'add' ? await createReport({ ...form }) : await updateReport({ ...form })
    if (res.code !== 0) {
      ElMessage.error(res.message || '保存失败')
      return
    }
    ElMessage.success(formMode.value === 'add' ? '测试报告已创建' : '测试报告已更新')
    formVisible.value = false
    await loadData()
  } catch (error: any) {
    ElMessage.error(error?.message || '保存失败')
  } finally {
    submitting.value = false
  }
}

async function submitApprove() {
  if (!approveFormRef.value) return
  const valid = await approveFormRef.value.validate().catch(() => false)
  if (!valid) return
  approveSubmitting.value = true
  try {
    const res = await approveReport({ ...approveForm })
    if (res.code !== 0) {
      ElMessage.error(res.message || '审核失败')
      return
    }
    ElMessage.success(approveForm.action === 'approve' ? '报告已审核通过' : '报告已驳回')
    approveVisible.value = false
    await loadData()
  } catch (error: any) {
    ElMessage.error(error?.message || '审核失败')
  } finally {
    approveSubmitting.value = false
  }
}

async function handleIssue(row: ReportItem) {
  if (!row.id) return
  try {
    await ElMessageBox.confirm(`确认签发报告 ${row.reportNo || ''} 吗？签发后状态为已通过。`, '签发确认', { type: 'warning' })
  } catch {
    return
  }
  try {
    const res = await issueReport(row.id)
    if (res.code !== 0) {
      ElMessage.error(res.message || '签发失败')
      return
    }
    ElMessage.success('报告已签发')
    await loadData()
  } catch (error: any) {
    ElMessage.error(error?.message || '签发失败')
  }
}

async function handleDelete(row: ReportItem) {
  if (!row.id) return
  try {
    await ElMessageBox.confirm(`确认删除测试报告 ${row.reportNo || ''} 吗？`, '删除确认', { type: 'warning' })
  } catch {
    return
  }
  try {
    const res = await deleteReport(row.id)
    if (res.code !== 0) {
      ElMessage.error(res.message || '删除失败')
      return
    }
    ElMessage.success('测试报告已删除')
    await loadData()
  } catch (error: any) {
    ElMessage.error(error?.message || '删除失败')
  }
}

onMounted(() => {
  loadRegisterOptions()
  loadData()
})
</script>

<style scoped>
.wms-pagination {
  display: flex;
  justify-content: flex-end;
  padding: 16px 0 4px;
}
.ets-pass {
  color: #67c23a;
  font-weight: 600;
}
.ets-fail {
  color: #f56c6c;
  font-weight: 600;
}
</style>
