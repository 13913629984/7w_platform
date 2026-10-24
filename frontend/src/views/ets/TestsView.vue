<template>
  <section class="table-page">
    <div class="sub-page-header">
      <div>
        <h2>测试执行</h2>
        <p class="page-desc">维护测试任务、录入测试结果并跟踪合格率与测试类别分布。</p>
      </div>
      <el-space>
        <el-button plain :icon="Refresh" @click="loadData">刷新</el-button>
        <el-button type="primary" :icon="Plus" @click="openAdd">新增测试</el-button>
      </el-space>
    </div>

    <div class="stats-row">
      <div class="stat-card blue">
        <div class="stat-header"><span class="stat-title">测试总数</span><div class="stat-icon">🧪</div></div>
        <div class="stat-value">{{ stats.total }}</div>
        <div class="stat-footer">今日完成 {{ stats.todayCount }} 项</div>
      </div>
      <div class="stat-card green">
        <div class="stat-header"><span class="stat-title">合格</span><div class="stat-icon">✅</div></div>
        <div class="stat-value">{{ stats.pass }}</div>
        <div class="stat-footer">判定为 pass 的记录</div>
      </div>
      <div class="stat-card red">
        <div class="stat-header"><span class="stat-title">不合格</span><div class="stat-icon">⚠️</div></div>
        <div class="stat-value">{{ stats.fail }}</div>
        <div class="stat-footer">待测试 {{ stats.pending }} 项</div>
      </div>
      <div class="stat-card purple">
        <div class="stat-header"><span class="stat-title">合格率</span><div class="stat-icon">📈</div></div>
        <div class="stat-value">{{ passRateLabel }}</div>
        <div class="stat-footer">合格 / (合格 + 不合格)</div>
      </div>
    </div>

    <div class="table-card">
      <div class="filter-bar">
        <div class="filter-left">
          <el-input v-model="keyword" clearable placeholder="搜索测试编号/登记号/客户/项目/测试员" style="width:300px" :prefix-icon="Search" @keyup.enter="searchData" @clear="searchData" />
          <el-select v-model="resultFilter" clearable placeholder="测试结果" style="width:130px" @change="searchData">
            <el-option v-for="opt in resultOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
          <el-select v-model="categoryFilter" clearable placeholder="测试类别" style="width:130px" @change="searchData">
            <el-option v-for="opt in categoryOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
          <el-input v-model="registerNoFilter" clearable placeholder="登记单号" style="width:160px" @keyup.enter="searchData" @clear="searchData" />
          <el-button type="primary" :icon="Search" @click="searchData">查询</el-button>
        </div>
      </div>

      <el-table v-loading="loading" :data="rows" stripe border style="width:100%">
        <el-table-column prop="code" label="测试编号" width="140">
          <template #default="{ row }"><b>{{ row.code || '-' }}</b></template>
        </el-table-column>
        <el-table-column prop="registerNo" label="登记单号" width="140" />
        <el-table-column prop="customerName" label="客户名称" min-width="150" show-overflow-tooltip />
        <el-table-column prop="product" label="产品名称" min-width="140" show-overflow-tooltip />
        <el-table-column prop="testItem" label="测试项目" min-width="130" show-overflow-tooltip />
        <el-table-column label="类别" width="90" align="center">
          <template #default="{ row }"><el-tag effect="plain" round>{{ categoryLabel(row.category) }}</el-tag></template>
        </el-table-column>
        <el-table-column label="标准值/实测值" min-width="150">
          <template #default="{ row }">{{ row.standardValue || '-' }} / {{ row.measuredValue || '-' }}</template>
        </el-table-column>
        <el-table-column label="结果" width="100" align="center">
          <template #default="{ row }"><el-tag :type="resultTagType(row.result)" effect="light">{{ resultLabel(row.result) }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="tester" label="测试员" width="100" />
        <el-table-column prop="testTime" label="测试时间" width="150" align="center" />
        <el-table-column label="操作" width="230" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="success" @click="openResult(row)">录入结果</el-button>
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

    <el-dialog v-model="formVisible" :title="formMode === 'add' ? '新增测试记录' : '编辑测试记录'" width="820px" :close-on-click-modal="false" @closed="resetForm">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="关联登记" prop="registerId">
              <el-select v-model="form.registerId" filterable clearable placeholder="选择来货登记" style="width:100%" @change="onRegisterChange">
                <el-option v-for="opt in registerOptions" :key="opt.id" :label="`${opt.registerNo} · ${opt.customerName || ''}`" :value="opt.id">
                  <span>{{ opt.registerNo }}</span>
                  <span style="float:right;color: var(--pw-text-secondary);font-size:12px">{{ opt.product || '' }}</span>
                </el-option>
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12"><el-form-item label="测试编号"><el-input v-model="form.code" maxlength="50" placeholder="留空自动生成" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="测试项目" prop="testItem"><el-input v-model="form.testItem" maxlength="100" /></el-form-item></el-col>
          <el-col :span="12">
            <el-form-item label="测试类别">
              <el-select v-model="form.category" placeholder="请选择类别" style="width:100%">
                <el-option v-for="opt in categoryOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12"><el-form-item label="测试标准"><el-input v-model="form.testStandard" maxlength="200" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="标准值"><el-input v-model="form.standardValue" maxlength="100" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="实测值"><el-input v-model="form.measuredValue" maxlength="100" /></el-form-item></el-col>
          <el-col :span="12">
            <el-form-item label="测试结果">
              <el-select v-model="form.result" placeholder="请选择结果" style="width:100%">
                <el-option v-for="opt in resultOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12"><el-form-item label="测试员"><el-input v-model="form.tester" maxlength="50" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="测试时间"><el-date-picker v-model="form.testTime" type="datetime" value-format="YYYY-MM-DD HH:mm" placeholder="选择时间" style="width:100%" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="耗时(分钟)"><el-input v-model="form.durationMin" maxlength="20" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="使用设备"><el-input v-model="form.equipmentUsed" maxlength="100" /></el-form-item></el-col>
          <el-col :span="24"><el-form-item label="备注"><el-input v-model="form.remark" type="textarea" :rows="3" maxlength="500" show-word-limit /></el-form-item></el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitForm">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="resultVisible" title="录入测试结果" width="560px" :close-on-click-modal="false">
      <el-form ref="resultFormRef" :model="resultForm" :rules="resultRules" label-width="90px">
        <el-form-item label="测试编号">
          <el-input :model-value="resultTarget?.code || '-'" disabled />
        </el-form-item>
        <el-form-item label="测试项目">
          <el-input :model-value="resultTarget?.testItem || '-'" disabled />
        </el-form-item>
        <el-form-item label="标准值">
          <el-input :model-value="resultTarget?.standardValue || '-'" disabled />
        </el-form-item>
        <el-form-item label="实测值" prop="measuredValue">
          <el-input v-model="resultForm.measuredValue" maxlength="100" placeholder="如 2.5 MΩ" />
        </el-form-item>
        <el-form-item label="测试结果" prop="result">
          <el-radio-group v-model="resultForm.result">
            <el-radio value="pass">合格</el-radio>
            <el-radio value="fail">不合格</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="测试员"><el-input v-model="resultForm.tester" maxlength="50" /></el-form-item>
        <el-form-item label="备注"><el-input v-model="resultForm.remark" type="textarea" :rows="3" maxlength="500" show-word-limit /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="resultVisible = false">取消</el-button>
        <el-button type="primary" :loading="resultSubmitting" @click="submitResult">提交结果</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="detailVisible" title="测试记录详情" width="720px">
      <el-descriptions v-if="detail" :column="2" border>
        <el-descriptions-item label="测试编号">{{ detail.code || '-' }}</el-descriptions-item>
        <el-descriptions-item label="登记单号">{{ detail.registerNo || '-' }}</el-descriptions-item>
        <el-descriptions-item label="客户名称">{{ detail.customerName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="产品名称">{{ detail.product || '-' }}</el-descriptions-item>
        <el-descriptions-item label="规格型号">{{ detail.model || '-' }}</el-descriptions-item>
        <el-descriptions-item label="测试项目">{{ detail.testItem || '-' }}</el-descriptions-item>
        <el-descriptions-item label="测试类别">{{ categoryLabel(detail.category) }}</el-descriptions-item>
        <el-descriptions-item label="测试结果">
          <el-tag :type="resultTagType(detail.result)" effect="light">{{ resultLabel(detail.result) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="测试标准" :span="2">{{ detail.testStandard || '-' }}</el-descriptions-item>
        <el-descriptions-item label="标准值">{{ detail.standardValue || '-' }}</el-descriptions-item>
        <el-descriptions-item label="实测值">{{ detail.measuredValue || '-' }}</el-descriptions-item>
        <el-descriptions-item label="测试员">{{ detail.tester || '-' }}</el-descriptions-item>
        <el-descriptions-item label="测试时间">{{ detail.testTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="耗时(分钟)">{{ detail.durationMin ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="使用设备">{{ detail.equipmentUsed || '-' }}</el-descriptions-item>
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
  createTest,
  deleteTest,
  getTest,
  getTestRegisterOptions,
  listTests,
  submitTestResult,
  updateTest,
  type TestItem,
  type TestRegisterOption,
  type TestResultPayload,
  type TestStats,
} from '@/api/ets/test'

const resultOptions = [
  { value: 'pass', label: '合格' },
  { value: 'fail', label: '不合格' },
  { value: 'pending', label: '待测试' },
]
const categoryOptions = [
  { value: 'appearance', label: '外观' },
  { value: 'insulation', label: '绝缘' },
  { value: 'withstand', label: '耐压' },
  { value: 'grounding', label: '接地' },
  { value: 'temperature', label: '温升' },
  { value: 'performance', label: '性能' },
]
const RESULT_TAG: Record<string, string> = { pass: 'success', fail: 'danger', pending: 'info' }

const loading = ref(false)
const submitting = ref(false)
const resultSubmitting = ref(false)
const rows = ref<TestItem[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)
const keyword = ref('')
const resultFilter = ref('')
const categoryFilter = ref('')
const registerNoFilter = ref('')
const registerOptions = ref<TestRegisterOption[]>([])
const formVisible = ref(false)
const detailVisible = ref(false)
const resultVisible = ref(false)
const formMode = ref<'add' | 'edit'>('add')
const formRef = ref<FormInstance>()
const resultFormRef = ref<FormInstance>()
const detail = ref<TestItem | null>(null)
const resultTarget = ref<TestItem | null>(null)
const stats = ref<TestStats>({ total: 0, pass: 0, fail: 0, pending: 0, passRate: 0, todayCount: 0, byCategory: {} })

const form = reactive<TestItem>(emptyForm())
const resultForm = reactive<TestResultPayload>({ id: 0, result: 'pass', measuredValue: '', tester: '', remark: '' })

const passRateLabel = computed(() => `${Number(stats.value.passRate ?? 0).toFixed(1)}%`)

const rules: FormRules = {
  testItem: [{ required: true, message: '请填写测试项目', trigger: 'blur' }],
  registerId: [{ required: true, message: '请选择关联的来货登记', trigger: 'change' }],
}
const resultRules: FormRules = {
  result: [{ required: true, message: '请选择测试结果', trigger: 'change' }],
}

function emptyForm(): TestItem {
  return {
    code: '',
    registerId: null,
    registerNo: '',
    testItem: '',
    category: 'appearance',
    testStandard: '',
    standardValue: '',
    measuredValue: '',
    result: 'pending',
    tester: '',
    testTime: '',
    durationMin: '',
    equipmentUsed: '',
    remark: '',
  }
}

function resultLabel(value?: string) {
  const found = resultOptions.find((item) => item.value === (value || 'pending'))
  return found ? found.label : value || '待测试'
}

function resultTagType(value?: string) {
  return RESULT_TAG[value || 'pending'] || 'info'
}

function categoryLabel(value?: string) {
  const found = categoryOptions.find((item) => item.value === value)
  return found ? found.label : value || '-'
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
  if (option.testStandard && !form.testStandard) form.testStandard = option.testStandard
}

async function loadRegisterOptions() {
  try {
    const res = await getTestRegisterOptions()
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
    const res = await listTests({
      keyword: keyword.value.trim() || undefined,
      result: resultFilter.value || undefined,
      category: categoryFilter.value || undefined,
      registerNo: registerNoFilter.value.trim() || undefined,
      page: page.value,
      pageSize: pageSize.value,
    })
    if (res.code !== 0) {
      ElMessage.error(res.message || '加载测试记录失败')
      return
    }
    rows.value = res.data?.rows || []
    total.value = Number(res.data?.total || 0)
    if (res.data?.stats) stats.value = res.data.stats
  } catch (error: any) {
    ElMessage.error(error?.message || '加载测试记录失败')
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

function openEdit(row: TestItem) {
  formMode.value = 'edit'
  resetForm()
  Object.assign(form, row)
  form.registerId = row.registerId ?? null
  formVisible.value = true
}

async function openDetail(row: TestItem) {
  if (!row.id) return
  try {
    const res = await getTest(row.id)
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

function openResult(row: TestItem) {
  if (!row.id) return
  resultTarget.value = row
  resultForm.id = row.id
  resultForm.result = row.result === 'fail' ? 'fail' : 'pass'
  resultForm.measuredValue = row.measuredValue || ''
  resultForm.tester = row.tester || ''
  resultForm.remark = ''
  resultFormRef.value?.clearValidate()
  resultVisible.value = true
}

async function submitForm() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    const payload: TestItem = { ...form }
    if (payload.durationMin === '') payload.durationMin = null
    const res = formMode.value === 'add' ? await createTest(payload) : await updateTest(payload)
    if (res.code !== 0) {
      ElMessage.error(res.message || '保存失败')
      return
    }
    ElMessage.success(formMode.value === 'add' ? '测试记录已创建' : '测试记录已更新')
    formVisible.value = false
    await loadData()
  } catch (error: any) {
    ElMessage.error(error?.message || '保存失败')
  } finally {
    submitting.value = false
  }
}

async function submitResult() {
  if (!resultFormRef.value) return
  const valid = await resultFormRef.value.validate().catch(() => false)
  if (!valid) return
  resultSubmitting.value = true
  try {
    const res = await submitTestResult({ ...resultForm })
    if (res.code !== 0) {
      ElMessage.error(res.message || '结果录入失败')
      return
    }
    ElMessage.success('测试结果已录入')
    resultVisible.value = false
    await loadData()
  } catch (error: any) {
    ElMessage.error(error?.message || '结果录入失败')
  } finally {
    resultSubmitting.value = false
  }
}

async function handleDelete(row: TestItem) {
  if (!row.id) return
  try {
    await ElMessageBox.confirm(`确认删除测试记录 ${row.code || ''} 吗？`, '删除确认', { type: 'warning' })
  } catch {
    return
  }
  try {
    const res = await deleteTest(row.id)
    if (res.code !== 0) {
      ElMessage.error(res.message || '删除失败')
      return
    }
    ElMessage.success('测试记录已删除')
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
</style>
