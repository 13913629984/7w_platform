<template>
  <section>
    <div class="sub-page-header">
      <div><h2>盘点管理</h2></div>
      <el-space>
        <el-button plain @click="refreshAll">刷新</el-button>
      </el-space>
    </div>

    <div class="stats-row">
      <div class="stat-card blue"><div class="stat-header"><span class="stat-title">盘点单总数</span></div><div class="stat-value">{{ stats.total }}</div><div class="stat-footer">盘点记录总数</div></div>
      <div class="stat-card green"><div class="stat-header"><span class="stat-title">已完成</span></div><div class="stat-value">{{ stats.completed }}</div><div class="stat-footer">完成盘点</div></div>
      <div class="stat-card orange"><div class="stat-header"><span class="stat-title">进行中</span></div><div class="stat-value">{{ stats.processing }}</div><div class="stat-footer">盘点中</div></div>
      <div class="stat-card purple"><div class="stat-header"><span class="stat-title">待审核</span></div><div class="stat-value">{{ stats.pendingReview }}</div><div class="stat-footer">差异审核</div></div>
    </div>

    <el-tabs v-model="activeStep" class="wms-inbound-tabs" type="card">
      <el-tab-pane label="📋 盘点单管理" name="orders" />
      <el-tab-pane label="📝 执行盘点" name="execute" />
      <el-tab-pane label="✅ 差异审核" name="review" />
    </el-tabs>

    <template v-if="activeStep === 'orders'">
      <div class="table-card wms-inventory-card">
        <div class="filter-bar">
          <div class="filter-left">
            <el-input v-model="searchText" clearable placeholder="搜索盘点单号/仓库..." style="width:280px" />
            <el-select v-model="statusFilter" clearable placeholder="状态筛选" style="width:130px">
              <el-option label="待开始" value="pending" />
              <el-option label="进行中" value="processing" />
              <el-option label="待审核" value="pending_review" />
              <el-option label="已完成" value="completed" />
            </el-select>
          </div>
          <el-button type="primary" @click="openCreate">+ 新建盘点单</el-button>
        </div>
        <el-table :data="pagedInventories" stripe style="width:100%">
          <el-table-column prop="code" label="盘点单号" width="180"><template #default="{ row }"><b>{{ row.code }}</b></template></el-table-column>
          <el-table-column prop="warehouse" label="仓库" width="130" />
          <el-table-column prop="scopeDesc" label="盘点范围" min-width="160" />
          <el-table-column label="盘点类型" width="110"><template #default="{ row }"><el-tag effect="light">{{ getTypeText(row.type) }}</el-tag></template></el-table-column>
          <el-table-column label="任务数" width="90" align="right"><template #default="{ row }"><b>{{ row.taskCount }}</b></template></el-table-column>
          <el-table-column label="完成数" width="90" align="right"><template #default="{ row }"><b class="success">{{ row.completedCount }}</b></template></el-table-column>
          <el-table-column label="差异项" width="90" align="right"><template #default="{ row }"><b class="danger">{{ row.diffCount }}</b></template></el-table-column>
          <el-table-column prop="creator" label="盘点人" width="120" />
          <el-table-column prop="createTime" label="创建时间" width="170" />
          <el-table-column label="状态" width="110" align="center"><template #default="{ row }"><el-tag :type="getStatusType(row.status)" effect="light">{{ getStatusText(row.status) }}</el-tag></template></el-table-column>
          <el-table-column label="操作" width="280" fixed="right" align="center">
            <template #default="{ row }">
              <el-button link type="primary" @click="showDetail(row)">详情</el-button>
              <el-button v-if="row.status === 'pending'" type="success" size="small" @click="startInventory(row)">启动盘点</el-button>
              <el-button v-if="row.status === 'processing'" type="warning" size="small" @click="goExecute(row)">执行盘点</el-button>
              <el-button v-if="row.status === 'pending_review'" type="danger" size="small" @click="goReview(row)">差异审核</el-button>
              <el-button v-if="row.status === 'pending_review'" type="success" size="small" @click="completeInventory(row)">结束盘点</el-button>
              <el-button v-if="row.status !== 'completed'" link type="danger" @click="removeInventory(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <div class="wms-pagination"><el-pagination v-model:current-page="page" :page-size="10" :total="filteredInventories.length" background layout="total, prev, pager, next, jumper" /></div>
      </div>
    </template>

    <template v-if="activeStep === 'execute'">
      <InventoryTaskPanel v-if="selectedInventory" :inventory="selectedInventory" mode="execute" @back="backToOrders" @process="openProcess" />
      <el-empty v-else description="请从盘点单管理中选择进行中的盘点单" />
    </template>

    <template v-if="activeStep === 'review'">
      <InventoryTaskPanel v-if="selectedInventory" :inventory="selectedInventory" mode="review" @back="backToOrders" @review="openReview" @complete="completeInventory" />
      <el-empty v-else description="请从盘点单管理中选择待审核盘点单" />
    </template>

    <el-dialog v-model="createVisible" title="新建盘点单" width="620px">
      <el-form :model="createForm" label-width="100px">
        <el-form-item label="盘点单号"><el-input v-model="createForm.code" placeholder="留空自动生成" /></el-form-item>
        <el-form-item label="盘点仓库" required>
          <el-select v-model="createForm.warehouseCode" placeholder="请选择仓库" style="width:100%" @change="onWarehouseChange">
            <el-option v-for="wh in warehouses" :key="wh.code" :label="wh.name" :value="wh.code" />
          </el-select>
        </el-form-item>
        <el-form-item label="盘点类型">
          <el-select v-model="createForm.type" style="width:100%">
            <el-option label="全盘" value="full" />
            <el-option label="循环盘点" value="cycle" />
            <el-option label="抽盘" value="spot" />
          </el-select>
        </el-form-item>
        <el-form-item label="盘点库位">
          <el-select v-model="createForm.locationCodes" multiple clearable placeholder="留空则盘点整个仓库" style="width:100%">
            <el-option v-for="loc in filteredLocations" :key="loc.code" :label="`${loc.code} ${loc.name}`" :value="loc.code" />
          </el-select>
        </el-form-item>
        <el-form-item label="盘点人" required><el-input v-model="createForm.creator" placeholder="请输入盘点人" /></el-form-item>
        <el-form-item label="备注"><el-input v-model="createForm.remark" type="textarea" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="createVisible = false">取消</el-button><el-button type="primary" @click="submitCreate">确定提交</el-button></template>
    </el-dialog>

    <el-dialog v-model="detailVisible" title="盘点单详情" width="1000px">
      <div v-if="detailOrder">
        <el-descriptions border :column="3">
          <el-descriptions-item label="盘点单号">{{ detailOrder.code }}</el-descriptions-item>
          <el-descriptions-item label="仓库">{{ detailOrder.warehouse }}</el-descriptions-item>
          <el-descriptions-item label="盘点范围">{{ detailOrder.scopeDesc }}</el-descriptions-item>
          <el-descriptions-item label="盘点类型"><el-tag effect="light">{{ getTypeText(detailOrder.type) }}</el-tag></el-descriptions-item>
          <el-descriptions-item label="状态"><el-tag :type="getStatusType(detailOrder.status)" effect="light">{{ getStatusText(detailOrder.status) }}</el-tag></el-descriptions-item>
          <el-descriptions-item label="盘点人">{{ detailOrder.creator }}</el-descriptions-item>
          <el-descriptions-item label="任务数">{{ detailOrder.taskCount }}</el-descriptions-item>
          <el-descriptions-item label="完成数">{{ detailOrder.completedCount }}</el-descriptions-item>
          <el-descriptions-item label="差异项">{{ detailOrder.diffCount }}</el-descriptions-item>
          <el-descriptions-item label="备注" :span="3">{{ detailOrder.remark || '-' }}</el-descriptions-item>
        </el-descriptions>
        <el-table :data="detailOrder.tasks || []" border stripe size="small" max-height="360" style="margin-top:16px" empty-text="暂无盘点任务">
          <el-table-column prop="locationCode" label="库位" width="100" />
          <el-table-column prop="materialCode" label="物料编码" width="120" />
          <el-table-column prop="materialName" label="物料名称" min-width="130" />
          <el-table-column prop="bookQty" label="账面数量" width="100" align="right" />
          <el-table-column prop="actualQty" label="实盘数量" width="100" align="right" />
          <el-table-column label="差异数量" width="100" align="right"><template #default="{ row }"><span :class="{ danger: row.diffQty !== 0 }">{{ row.diffQty }}</span></template></el-table-column>
          <el-table-column label="差异金额" width="110" align="right"><template #default="{ row }"><span :class="{ danger: row.diffAmount !== 0 }">¥{{ Number(row.diffAmount).toFixed(2) }}</span></template></el-table-column>
          <el-table-column label="状态" width="90" align="center"><template #default="{ row }"><el-tag :type="row.status === 'completed' ? 'success' : 'warning'" effect="light">{{ row.status === 'completed' ? '已完成' : '待开始' }}</el-tag></template></el-table-column>
          <el-table-column label="审核" width="90" align="center"><template #default="{ row }"><el-tag v-if="row.hasDiff" :type="row.reviewStatus === 'approved' ? 'success' : (row.reviewStatus === 'rejected' ? 'info' : 'warning')" effect="light">{{ getReviewText(row.reviewStatus) }}</el-tag><span v-else>-</span></template></el-table-column>
        </el-table>
      </div>
    </el-dialog>

    <el-dialog v-model="processVisible" title="执行盘点" width="520px">
      <el-form :model="processForm" label-width="100px">
        <el-form-item label="物料编码"><el-input v-model="processForm.materialCode" disabled /></el-form-item>
        <el-form-item label="物料名称"><el-input v-model="processForm.materialName" disabled /></el-form-item>
        <el-form-item label="库位"><el-input v-model="processForm.location" disabled /></el-form-item>
        <el-form-item label="账面数量"><el-input v-model="processForm.bookQty" disabled /></el-form-item>
        <el-form-item label="实盘数量"><el-input-number v-model="processForm.actualQty" :min="0" style="width:100%" /></el-form-item>
        <el-form-item label="备注"><el-input v-model="processForm.remark" type="textarea" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="processVisible = false">取消</el-button><el-button type="primary" @click="submitProcess">确认盘点</el-button></template>
    </el-dialog>

    <el-dialog v-model="reviewVisible" title="差异审核" width="520px">
      <el-form :model="reviewForm" label-width="100px">
        <el-form-item label="物料编码"><el-input v-model="reviewForm.materialCode" disabled /></el-form-item>
        <el-form-item label="物料名称"><el-input v-model="reviewForm.materialName" disabled /></el-form-item>
        <el-form-item label="库位"><el-input v-model="reviewForm.location" disabled /></el-form-item>
        <el-form-item label="差异数量"><el-input v-model="reviewForm.diffQty" disabled /></el-form-item>
        <el-form-item label="差异金额"><el-input v-model="reviewForm.diffAmount" disabled /></el-form-item>
        <el-form-item label="审核结果"><el-radio-group v-model="reviewForm.result"><el-radio label="approved">确认差异</el-radio><el-radio label="rejected">驳回</el-radio></el-radio-group></el-form-item>
        <el-form-item label="审核备注"><el-input v-model="reviewForm.remark" type="textarea" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="reviewVisible = false">取消</el-button><el-button type="primary" @click="submitReview">确认审核</el-button></template>
    </el-dialog>
  </section>
</template>

<script setup lang="ts">
import { computed, defineComponent, h, onMounted, ref, watch } from 'vue'
import { ElButton, ElMessage, ElMessageBox, ElPagination, ElTag } from 'element-plus'
import {
  completeStockTakingOrder,
  createStockTakingOrder,
  deleteStockTakingOrder,
  getStockTakingOrder,
  getStockTakingStats,
  listStockTakingOrders,
  processStockTakingTask,
  reviewStockTakingTask,
  startStockTakingOrder,
  type ReviewStatus,
  type StockTakingOrder,
  type StockTakingStats,
  type StockTakingStatus,
  type StockTakingTask,
  type StockTakingType,
} from '@/api/wms/stocktaking'
import { listLocations, type LocationItem } from '@/api/wms/location'
import { listWarehouses, type WarehouseItem } from '@/api/wms/warehouse'

type Inventory = StockTakingOrder
type Task = StockTakingTask

const statusText: Record<StockTakingStatus, string> = { completed: '已完成', processing: '进行中', pending_review: '待审核', pending: '待开始' }
const statusType: Record<StockTakingStatus, 'success' | 'warning' | 'primary' | 'info'> = { completed: 'success', processing: 'primary', pending_review: 'warning', pending: 'warning' }
const typeText: Record<StockTakingType, string> = { full: '全盘', cycle: '循环盘点', spot: '抽盘' }
const reviewText: Record<ReviewStatus, string> = { '': '', pending: '待审核', approved: '已确认', rejected: '已驳回' }
const getTypeText = (type: StockTakingType) => typeText[type] || type
const getStatusText = (status: StockTakingStatus) => statusText[status] || status
const getStatusType = (status: StockTakingStatus) => statusType[status] || 'info'
const getReviewText = (status: ReviewStatus) => reviewText[status] || status

const InventoryTaskPanel = defineComponent({
  props: { inventory: { type: Object as () => Inventory, required: true }, mode: { type: String as () => 'execute' | 'review', required: true } },
  emits: ['back', 'process', 'review', 'complete'],
  setup(props, { emit }) {
    const taskPage = ref(1)
    const taskPageSize = ref(10)
    const allTasks = computed(() => props.inventory.tasks || [])
    const panelTasks = computed(() => props.mode === 'review' ? allTasks.value.filter((task) => task.hasDiff) : allTasks.value)
    const pagedTasks = computed(() => panelTasks.value.slice((taskPage.value - 1) * taskPageSize.value, taskPage.value * taskPageSize.value))
    const groupedTasks = computed(() => {
      const groups: Record<string, Task[]> = {}
      pagedTasks.value.forEach((task) => {
        const key = task.locationCode || '-'
        if (!groups[key]) groups[key] = []
        groups[key].push(task)
      })
      return Object.entries(groups)
    })
    const completed = computed(() => allTasks.value.filter((task) => task.status === 'completed').length)
    const diffTotal = computed(() => allTasks.value.filter((task) => task.hasDiff).length)

    return () => h('div', { class: 'inventory-workspace' }, [
      h('div', { class: 'inventory-summary' }, [
        h('span', null, ['盘点单号：', h('b', null, props.inventory.code)]),
        h('span', null, ['仓库：', h('b', null, props.inventory.warehouse)]),
        h('span', null, ['盘点范围：', h('b', null, props.inventory.scopeDesc)]),
        props.mode === 'review' ? h('span', null, ['差异项数：', h('b', { class: 'danger' }, `${diffTotal.value}项`)]) : h('span', null, ['状态：', h(ElTag, { type: statusType[props.inventory.status], effect: 'light' }, () => statusText[props.inventory.status])]),
        h(ElButton, { type: 'primary', onClick: () => emit('back') }, () => '返回盘点单'),
        props.mode === 'review' ? h(ElButton, { type: 'success', onClick: () => emit('complete', props.inventory) }, () => '结束盘点') : null,
      ]),
      h('div', { class: 'inventory-task-title' }, [h('strong', null, props.mode === 'review' ? '差异列表' : '盘点任务'), props.mode === 'execute' ? h('span', null, `已完成：${completed.value} / 总任务：${allTasks.value.length}`) : null]),
      h('div', { class: 'inventory-task-table' }, [
        h('table', null, [
          h('thead', null, h('tr', null, ['序号', '库位', '物料编码', '物料名称', '账面数量', '实盘数量', '差异数量', '差异金额', props.mode === 'review' ? '审核状态' : '状态', '操作'].map((head) => h('th', null, head)))),
          h('tbody', null, groupedTasks.value.flatMap(([location, tasks]) => [
            h('tr', { class: props.mode === 'review' ? 'location-row review' : 'location-row' }, [h('td', { colspan: 10 }, `📦 ${location} (${props.mode === 'review' ? '差异：' : ''}${tasks.length}项)`)]),
            ...tasks.map((task, index) => h('tr', null, [
              h('td', null, String(index + 1)), h('td', null, task.locationCode), h('td', null, task.materialCode), h('td', null, task.materialName), h('td', null, String(task.bookQty)), h('td', null, String(task.actualQty)), h('td', { class: task.diffQty !== 0 ? 'danger' : '' }, String(task.diffQty)), h('td', { class: task.diffAmount !== 0 ? 'danger' : '' }, `¥${Number(task.diffAmount).toFixed(2)}`),
              h('td', null, props.mode === 'review' ? h(ElTag, { type: task.reviewStatus === 'approved' ? 'success' : 'warning', effect: 'light' }, () => reviewText[task.reviewStatus]) : h(ElTag, { type: task.status === 'completed' ? 'success' : 'warning', effect: 'light' }, () => task.status === 'completed' ? '已完成' : '待开始')),
              h('td', null, props.mode === 'review'
                ? (task.reviewStatus === 'pending' ? h(ElButton, { type: 'warning', size: 'small', onClick: () => emit('review', task) }, () => '审核') : h(ElButton, { type: 'success', size: 'small', disabled: true }, () => '已审核'))
                : (task.status === 'pending' ? h(ElButton, { type: 'primary', size: 'small', onClick: () => emit('process', task) }, () => '执行盘点') : h(ElButton, { type: 'success', size: 'small', onClick: () => emit('process', task) }, () => '修改')))
            ])),
          ]))
        ])
      ]),
      h('div', { class: 'wms-pagination' }, h(ElPagination, {
        currentPage: taskPage.value,
        pageSize: taskPageSize.value,
        total: panelTasks.value.length,
        background: true,
        layout: 'total, sizes, prev, pager, next, jumper',
        pageSizes: [5, 10, 20, 50],
        'onUpdate:currentPage': (value: number) => { taskPage.value = value },
        'onUpdate:pageSize': (value: number) => { taskPageSize.value = value; taskPage.value = 1 },
      }))
    ])
  }
})

const activeStep = ref('orders')
const searchText = ref('')
const statusFilter = ref('')
const page = ref(1)
const inventories = ref<Inventory[]>([])
const stats = ref<StockTakingStats>({ total: 0, completed: 0, processing: 0, pendingReview: 0 })
const warehouses = ref<WarehouseItem[]>([])
const locationOptions = ref<LocationItem[]>([])
const selectedInventory = ref<Inventory | null>(null)
const createVisible = ref(false)
const detailVisible = ref(false)
const detailOrder = ref<Inventory | null>(null)
const processVisible = ref(false)
const reviewVisible = ref(false)
const createForm = ref({ code: '', warehouseCode: '', type: 'full' as StockTakingType, locationCodes: [] as string[], creator: '', remark: '' })
const processForm = ref({ taskId: 0, materialCode: '', materialName: '', location: '', bookQty: 0, actualQty: 0, remark: '' })
const reviewForm = ref({ taskId: 0, materialCode: '', materialName: '', location: '', diffQty: 0, diffAmount: 0, result: 'approved' as 'approved' | 'rejected', remark: '' })

const filteredInventories = computed(() => inventories.value)
const pagedInventories = computed(() => filteredInventories.value.slice((page.value - 1) * 10, page.value * 10))
const filteredLocations = computed(() => locationOptions.value.filter((loc) => !createForm.value.warehouseCode || loc.warehouseCode === createForm.value.warehouseCode))

function apiOk<T>(response: { code: number; message: string; data: T }) {
  if (response.code !== 0) {
    ElMessage.error(response.message || '接口请求失败')
    return false
  }
  return true
}

async function loadOrders() {
  const response = await listStockTakingOrders({ keyword: searchText.value, status: statusFilter.value })
  if (apiOk(response)) inventories.value = response.data
}

async function loadStats() {
  const response = await getStockTakingStats()
  if (apiOk(response)) stats.value = response.data
}

async function loadOptions() {
  const [warehouseResponse, locationResponse] = await Promise.all([listWarehouses({ status: 1 }), listLocations({ status: 1 })])
  if (apiOk(warehouseResponse)) warehouses.value = warehouseResponse.data
  if (apiOk(locationResponse)) locationOptions.value = locationResponse.data
}

async function refreshAll() {
  await Promise.all([loadOrders(), loadStats(), loadOptions()])
  if (selectedInventory.value) await syncSelected(selectedInventory.value.id)
}

async function syncSelected(id: number) {
  const response = await getStockTakingOrder(id)
  if (apiOk(response)) selectedInventory.value = response.data
}

function openCreate() {
  createForm.value = { code: '', warehouseCode: '', type: 'full', locationCodes: [], creator: '', remark: '' }
  createVisible.value = true
}

function onWarehouseChange() {
  createForm.value.locationCodes = []
}

async function submitCreate() {
  const form = createForm.value
  if (!form.warehouseCode) {
    ElMessage.warning('请选择盘点仓库')
    return
  }
  if (!form.creator.trim()) {
    ElMessage.warning('请填写盘点人')
    return
  }
  const response = await createStockTakingOrder({
    code: form.code.trim(),
    warehouseCode: form.warehouseCode,
    type: form.type,
    locationCodes: form.locationCodes,
    creator: form.creator.trim(),
    remark: form.remark.trim(),
  })
  if (!apiOk(response)) return
  createVisible.value = false
  page.value = 1
  await Promise.all([loadOrders(), loadStats()])
  ElMessage.success('新建盘点单成功')
}

async function showDetail(row: Inventory) {
  const response = await getStockTakingOrder(row.id)
  if (!apiOk(response)) return
  detailOrder.value = response.data
  detailVisible.value = true
}

async function startInventory(row: Inventory) {
  const response = await startStockTakingOrder(row.id)
  if (!apiOk(response)) return
  selectedInventory.value = response.data
  await Promise.all([loadOrders(), loadStats()])
  activeStep.value = 'execute'
  ElMessage.success('盘点已启动')
}

async function goExecute(row: Inventory) {
  await syncSelected(row.id)
  activeStep.value = 'execute'
}

async function goReview(row: Inventory) {
  await syncSelected(row.id)
  activeStep.value = 'review'
}

function backToOrders() {
  activeStep.value = 'orders'
  refreshAll()
}

function openProcess(task: Task) {
  processForm.value = { taskId: task.id, materialCode: task.materialCode || '', materialName: task.materialName || '', location: task.location || task.locationCode || '', bookQty: task.bookQty, actualQty: task.actualQty || task.bookQty, remark: task.remark || '' }
  processVisible.value = true
}

async function submitProcess() {
  if (!selectedInventory.value) return
  const response = await processStockTakingTask({
    id: selectedInventory.value.id,
    taskId: processForm.value.taskId,
    actualQty: Number(processForm.value.actualQty),
    remark: processForm.value.remark,
  })
  if (!apiOk(response)) return
  selectedInventory.value = response.data
  processVisible.value = false
  await Promise.all([loadOrders(), loadStats()])
  ElMessage.success('盘点任务已完成')
}

function openReview(task: Task) {
  reviewForm.value = { taskId: task.id, materialCode: task.materialCode || '', materialName: task.materialName || '', location: task.location || task.locationCode || '', diffQty: task.diffQty, diffAmount: task.diffAmount, result: 'approved', remark: task.remark || '' }
  reviewVisible.value = true
}

async function submitReview() {
  if (!selectedInventory.value) return
  const response = await reviewStockTakingTask({
    id: selectedInventory.value.id,
    taskId: reviewForm.value.taskId,
    result: reviewForm.value.result,
    remark: reviewForm.value.remark,
  })
  if (!apiOk(response)) return
  selectedInventory.value = response.data
  reviewVisible.value = false
  ElMessage.success('差异审核完成')
}

async function completeInventory(row: Inventory) {
  try {
    await ElMessageBox.confirm('确定结束此盘点吗？已确认的差异将调整库存账面数量。', '结束确认', { type: 'warning' })
  } catch {
    return
  }
  const response = await completeStockTakingOrder(row.id)
  if (!apiOk(response)) return
  selectedInventory.value = null
  activeStep.value = 'orders'
  await Promise.all([loadOrders(), loadStats()])
  ElMessage.success('盘点已结束')
}

async function removeInventory(row: Inventory) {
  try {
    await ElMessageBox.confirm('确认删除该盘点单？', '删除确认', { type: 'warning' })
  } catch {
    return
  }
  const response = await deleteStockTakingOrder(row.id)
  if (!apiOk(response)) return
  if (selectedInventory.value?.id === row.id) selectedInventory.value = null
  await Promise.all([loadOrders(), loadStats()])
  ElMessage.success('删除成功')
}

watch([searchText, statusFilter], () => {
  page.value = 1
  loadOrders()
})

onMounted(refreshAll)
</script>
