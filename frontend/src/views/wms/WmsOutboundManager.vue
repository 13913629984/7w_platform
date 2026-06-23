<template>
  <section>
    <div class="sub-page-header">
      <div><h2>出库管理</h2></div>
      <el-space>
        <el-button plain @click="exportOrders">导出</el-button>
        <el-button plain @click="refreshAll">刷新</el-button>
        <el-button type="primary" @click="openCreate">+ 新增出库</el-button>
      </el-space>
    </div>

    <el-tabs v-model="activeTab" class="wms-inbound-tabs" type="card">
      <el-tab-pane label="出库单列表" name="orders" />
      <el-tab-pane label="出库明细查询" name="details" />
      <el-tab-pane label="SN信息查询" name="sn" />
    </el-tabs>

    <div class="table-card wms-inbound-card">
      <template v-if="activeTab === 'orders'">
        <div class="filter-bar">
          <div class="filter-left">
            <el-input v-model="orderSearch" clearable placeholder="搜索出库单号/客户..." style="width:260px" />
            <el-select v-model="orderStatus" clearable placeholder="状态筛选" style="width:120px">
              <el-option label="已完成" value="completed" />
              <el-option label="部分出库" value="partial" />
              <el-option label="待出库" value="pending" />
            </el-select>
          </div>
        </div>
        <el-table :data="pagedOrders" stripe style="width:100%">
          <el-table-column prop="code" label="出库单号" width="140" />
          <el-table-column prop="orderNo" label="关联销售订单" width="150"><template #default="{ row }"><el-button link type="primary">{{ row.orderNo }}</el-button></template></el-table-column>
          <el-table-column prop="customer" label="客户名称" min-width="150" />
          <el-table-column prop="warehouse" label="仓库" width="120" />
          <el-table-column prop="materialCount" label="物料种类" width="90" align="right"><template #default="{ row }"><b>{{ row.materialCount }}种</b></template></el-table-column>
          <el-table-column label="出库进度" width="140"><template #default="{ row }"><div class="inbound-progress"><span :class="row.status" :style="{ width: row.outboundRate + '%' }"></span><em>{{ row.outboundQty }}/{{ row.totalQty }}</em></div></template></el-table-column>
          <el-table-column prop="totalQty" label="总数量" width="90" align="right"><template #default="{ row }"><b>{{ row.totalQty }}</b></template></el-table-column>
          <el-table-column label="总金额" width="130" align="right"><template #default="{ row }">¥{{ row.totalAmount.toFixed(2) }}</template></el-table-column>
          <el-table-column prop="creator" label="出库人" width="100" />
          <el-table-column prop="createTime" label="出库时间" width="150" />
          <el-table-column label="状态" width="100" align="center"><template #default="{ row }"><el-tag :type="getStatusType(row.status)" effect="light">{{ getStatusText(row.status) }}</el-tag></template></el-table-column>
          <el-table-column label="操作" width="170" fixed="right" align="center">
            <template #default="{ row }">
              <el-button link type="primary" @click="showDetail(row)">详情</el-button>
              <el-button v-if="row.status !== 'completed'" link type="success" @click="openExecute(row)">执行出库</el-button>
              <el-button link type="danger" @click="removeOrder(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <InboundPagination v-model:page="orderPage" v-model:size="orderPageSize" :total="filteredOrders.length" />
      </template>

      <template v-if="activeTab === 'details'">
        <div class="filter-bar">
          <div class="filter-left">
            <el-input v-model="detailSearch" clearable placeholder="搜索出库单号/SKU/物料名称..." style="width:260px" />
            <el-select v-model="detailMethod" clearable placeholder="出库方式" style="width:120px">
              <el-option label="数量出库" value="qty" />
              <el-option label="SN出库" value="sn" />
            </el-select>
          </div>
        </div>
        <el-table :data="pagedDetails" stripe style="width:100%">
          <el-table-column prop="outboundCode" label="出库单号" width="150" />
          <el-table-column prop="sku" label="SKU编码" width="120" />
          <el-table-column prop="name" label="物料名称" min-width="160" />
          <el-table-column prop="spec" label="规格型号" width="130" />
          <el-table-column prop="location" label="库位" width="120" />
          <el-table-column label="出库方式" width="110" align="center"><template #default="{ row }"><el-tag :type="row.method === 'sn' ? 'warning' : 'primary'" effect="light">{{ getMethodText(row.method) }}</el-tag></template></el-table-column>
          <el-table-column prop="qty" label="出库数量" width="100" align="right"><template #default="{ row }"><b>{{ row.qty }}</b></template></el-table-column>
          <el-table-column prop="unitPrice" label="单价" width="100" align="right" />
          <el-table-column label="金额" width="130" align="right"><template #default="{ row }">¥{{ (row.qty * row.unitPrice).toFixed(2) }}</template></el-table-column>
          <el-table-column prop="createTime" label="出库时间" width="170" />
          <el-table-column prop="operator" label="操作人" width="100" />
        </el-table>
        <InboundPagination v-model:page="detailPage" v-model:size="detailPageSize" :total="filteredDetails.length" />
      </template>

      <template v-if="activeTab === 'sn'">
        <div class="filter-bar">
          <div class="filter-left">
            <el-input v-model="snSearch" clearable placeholder="搜索SN码/SKU/物料名称..." style="width:260px" />
            <el-select v-model="snStatus" clearable placeholder="状态筛选" style="width:120px">
              <el-option label="已入库" value="inbound" />
              <el-option label="已出库" value="outbound" />
            </el-select>
          </div>
        </div>
        <el-table :data="pagedSnRecords" stripe style="width:100%">
          <el-table-column prop="sn" label="SN码" width="180" />
          <el-table-column prop="sku" label="SKU编码" width="120" />
          <el-table-column prop="name" label="物料名称" min-width="150" />
          <el-table-column prop="spec" label="规格型号" width="130" />
          <el-table-column prop="location" label="库位" width="120" />
          <el-table-column prop="inboundCode" label="入库单号" width="150"><template #default="{ row }"><el-button link type="primary">{{ row.inboundCode }}</el-button></template></el-table-column>
          <el-table-column prop="inboundTime" label="入库时间" width="170" />
          <el-table-column prop="outboundCode" label="出库单号" width="150"><template #default="{ row }"><span :class="{ danger: row.outboundCode !== '-' }">{{ row.outboundCode }}</span></template></el-table-column>
          <el-table-column prop="outboundTime" label="出库时间" width="170" />
          <el-table-column label="状态" width="100" align="center"><template #default="{ row }"><el-tag :type="row.status === 'outbound' ? 'danger' : 'success'" effect="light">{{ row.status === 'outbound' ? '已出库' : '已入库' }}</el-tag></template></el-table-column>
        </el-table>
        <InboundPagination v-model:page="snPage" v-model:size="snPageSize" :total="filteredSnRecords.length" />
      </template>
    </div>


    <el-dialog v-model="createVisible" title="新增出库" width="760px" class="create-inbound-dialog">
      <el-form label-width="92px">
        <el-form-item label="出库单号" required><el-input v-model="createForm.code" placeholder="请输入出库单号" /></el-form-item>
        <el-form-item label="关联销售单"><el-input v-model="createForm.orderNo" placeholder="请输入关联单号" /></el-form-item>
        <el-form-item label="客户名称" required><el-input v-model="createForm.customer" placeholder="请输入客户或部门" /></el-form-item>
        <el-form-item label="出库仓库" required><el-select v-model="createForm.warehouseCode" placeholder="请选择仓库" style="width:100%"><el-option v-for="warehouse in warehouses" :key="warehouse.code" :label="warehouse.name" :value="warehouse.code" /></el-select></el-form-item>
        <el-form-item label="出库人" required><el-input v-model="createForm.creator" placeholder="请输入出库人" /></el-form-item>
        <el-form-item label="备注"><el-input v-model="createForm.remark" type="textarea" placeholder="请输入备注信息" /></el-form-item>
        <el-form-item label="物料明细">
          <el-table :data="createForm.items" border size="small" style="width:100%">
            <el-table-column label="SKU编码" width="130"><template #default="{ row }"><el-input v-model="row.sku" placeholder="SKU" /></template></el-table-column>
            <el-table-column label="物料名称" min-width="130"><template #default="{ row }"><el-input v-model="row.name" placeholder="物料名称" /></template></el-table-column>
            <el-table-column label="规格型号" width="120"><template #default="{ row }"><el-input v-model="row.spec" placeholder="规格" /></template></el-table-column>
            <el-table-column label="出库方式" width="120"><template #default="{ row }"><el-select v-model="row.method"><el-option label="数量出库" value="qty" /><el-option label="SN出库" value="sn" /></el-select></template></el-table-column>
            <el-table-column label="数量" width="120" align="center"><template #default="{ row }"><el-input-number v-model="row.qty" :min="1" :step="1" size="small" /></template></el-table-column>
            <el-table-column label="单价" width="120" align="center"><template #default="{ row }"><el-input-number v-model="row.unitPrice" :min="0" :precision="2" :step="1" size="small" /></template></el-table-column>
            <el-table-column label="操作" width="80" align="center"><template #default="{ $index }"><el-button link type="danger" @click="removeCreateItem($index)">删除</el-button></template></el-table-column>
          </el-table>
          <div class="create-items-footer"><span>合计：{{ createTotalQty }} 件 / ¥{{ createTotalAmount.toFixed(2) }}</span><el-button type="primary" @click="addCreateItem">+ 添加物料</el-button></div>
        </el-form-item>
      </el-form>
      <template #footer><el-button @click="createVisible = false">取消</el-button><el-button type="primary" @click="submitCreate">确定提交</el-button></template>
    </el-dialog>
    <el-dialog v-model="executeVisible" :title="`执行出库 - ${executeOrder?.code || ''}`" width="1000px" class="execute-dialog">
      <div v-if="executeOrder">
        <div class="execute-summary"><b>客户：</b>{{ executeOrder.customer }} <b>仓库：</b>{{ executeOrder.warehouse }} <b>出库进度：</b>{{ executeOrder.outboundQty }}/{{ executeOrder.totalQty }}</div>
        <div v-for="item in executeItems" :key="item.sku" class="execute-item">
          <div class="execute-item-head">
            <span><b>SKU:</b><br>{{ item.sku }}</span>
            <span><b>物料名称:</b> {{ item.name }}</span>
            <span><b>规格:</b><br>{{ item.spec }}</span>
            <span><b>出库方式:</b><br><el-tag :type="item.method === 'sn' ? 'warning' : 'primary'" effect="light">{{ getMethodText(item.method) }}</el-tag></span>
            <span><b>总数量:</b><br>{{ item.qty }}</span>
            <span><b>已出库:</b><br><em class="success">{{ item.outboundQty }}</em></span>
            <span><b>剩余可出库:</b><br><em class="primary">{{ item.remainingQty }}</em></span>
            <span><b>本次出库合计:</b><br><em class="warning">{{ item.locations.reduce((sum, loc) => sum + Number(loc.qty || 0), 0) }}</em></span>
            <el-button type="primary" @click="addLocation(item)">+ 添加出库行</el-button>
          </div>
          <el-table :data="item.locations" size="small" border>
            <el-table-column label="序号" type="index" width="70" align="center" />
            <el-table-column label="库位" width="220"><template #default="{ row }"><el-select v-model="row.location" placeholder="选择库位" style="width:100%"><el-option v-for="loc in locations" :key="loc" :label="loc" :value="loc" /></el-select></template></el-table-column>
            <el-table-column label="出库数量" width="140" align="center"><template #default="{ row }"><el-input-number v-model="row.qty" :min="0" :max="item.remainingQty" size="small" /></template></el-table-column>
            <el-table-column label="SN码（SN出库时填写）"><template #default="{ row }"><el-input v-model="row.snList" :disabled="item.method !== 'sn'" placeholder="SN码，逗号分隔" /></template></el-table-column>
            <el-table-column label="操作" width="120" align="center"><template #default="{ $index, row }"><el-button v-if="item.method === 'sn'" link type="primary" @click="fillSn(item, row)">批量输入SN</el-button><el-button link type="danger" @click="removeLocation(item, $index)">删除</el-button></template></el-table-column>
          </el-table>
        </div>
        <div class="execute-operator"><span>出库操作员：</span><el-input v-model="executeOperator" placeholder="请输入操作员名称" style="width:220px" /></div>
      </div>
      <template #footer>
        <el-button @click="executeVisible = false">取消</el-button>
        <el-button type="primary" @click="submitExecute">确认出库</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="detailVisible" title="出库单详情" width="1080px" class="inbound-detail-dialog">
      <div v-if="detailOrder" class="inbound-detail-layout">
        <div class="inbound-detail-summary">
          <div><span>出库总数</span><b>{{ detailOrder.totalQty }}</b></div>
          <div><span>已出库</span><b class="success">{{ detailOrder.outboundQty }}</b></div>
          <div><span>出库进度</span><b>{{ detailOrder.outboundRate }}%</b></div>
          <div><span>总金额</span><b>¥{{ detailOrder.totalAmount.toFixed(2) }}</b></div>
        </div>

        <el-descriptions border :column="4" class="inbound-detail-desc">
          <el-descriptions-item label="出库单号">{{ detailOrder.code }}</el-descriptions-item>
          <el-descriptions-item label="关联销售订单">{{ detailOrder.orderNo }}</el-descriptions-item>
          <el-descriptions-item label="客户名称">{{ detailOrder.customer }}</el-descriptions-item>
          <el-descriptions-item label="仓库">{{ detailOrder.warehouse }}</el-descriptions-item>
          <el-descriptions-item label="状态"><el-tag size="small" :type="getStatusType(detailOrder.status)">{{ getStatusText(detailOrder.status) }}</el-tag></el-descriptions-item>
          <el-descriptions-item label="出库人">{{ detailOrder.creator }}</el-descriptions-item>
          <el-descriptions-item label="创建时间" :span="2">{{ detailOrder.createTime }}</el-descriptions-item>
          <el-descriptions-item label="备注" :span="4">{{ detailOrder.remark || '-' }}</el-descriptions-item>
        </el-descriptions>

        <div class="inbound-detail-section">
          <div class="inbound-detail-title"><b>出库物料</b><span>{{ detailOrder.items?.length || 0 }} 条</span></div>
          <el-table :data="detailOrder.items || []" border stripe size="small" max-height="260" empty-text="暂无出库物料">
            <el-table-column prop="sku" label="SKU编码" width="130" />
            <el-table-column prop="name" label="物料名称" min-width="150" />
            <el-table-column prop="spec" label="规格型号" min-width="120" />
            <el-table-column label="出库方式" width="100" align="center">
              <template #default="{ row }"><el-tag :type="row.method === 'sn' ? 'warning' : 'primary'" effect="light">{{ getMethodText(row.method) }}</el-tag></template>
            </el-table-column>
            <el-table-column prop="locationCode" label="默认库位" width="110"><template #default="{ row }">{{ row.locationCode || '-' }}</template></el-table-column>
            <el-table-column prop="batchNo" label="批次号" width="120"><template #default="{ row }">{{ row.batchNo || '-' }}</template></el-table-column>
            <el-table-column prop="qty" label="应出库" width="90" align="right" />
            <el-table-column prop="outboundQty" label="已出库" width="90" align="right" />
            <el-table-column label="待出库" width="90" align="right"><template #default="{ row }">{{ remainingQty(row) }}</template></el-table-column>
            <el-table-column prop="unitPrice" label="单价" width="90" align="right"><template #default="{ row }">¥{{ Number(row.unitPrice || 0).toFixed(2) }}</template></el-table-column>
            <el-table-column label="金额" width="110" align="right"><template #default="{ row }">¥{{ (Number(row.qty || 0) * Number(row.unitPrice || 0)).toFixed(2) }}</template></el-table-column>
          </el-table>
        </div>

        <div class="inbound-detail-section">
          <div class="inbound-detail-title"><b>出库记录</b><span>{{ detailRecords.length }} 条</span></div>
          <el-table :data="detailRecords" border stripe size="small" max-height="260" empty-text="暂无出库记录">
            <el-table-column prop="createTime" label="出库时间" width="165" />
            <el-table-column prop="outboundCode" label="出库单号" width="135" />
            <el-table-column prop="sku" label="SKU编码" width="130" />
            <el-table-column prop="name" label="物料名称" min-width="150" />
            <el-table-column prop="spec" label="规格型号" min-width="120" />
            <el-table-column prop="location" label="出库库位" width="110" />
            <el-table-column label="出库方式" width="100" align="center">
              <template #default="{ row }"><el-tag :type="row.method === 'sn' ? 'warning' : 'primary'" effect="light">{{ getMethodText(row.method) }}</el-tag></template>
            </el-table-column>
            <el-table-column prop="qty" label="出库数量" width="90" align="right" />
            <el-table-column prop="unitPrice" label="单价" width="90" align="right"><template #default="{ row }">¥{{ Number(row.unitPrice || 0).toFixed(2) }}</template></el-table-column>
            <el-table-column label="金额" width="110" align="right"><template #default="{ row }">¥{{ (Number(row.qty || 0) * Number(row.unitPrice || 0)).toFixed(2) }}</template></el-table-column>
            <el-table-column prop="operator" label="操作人" width="100" />
          </el-table>
        </div>
      </div>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup lang="ts">
import { computed, defineComponent, h, onMounted, ref, watch } from 'vue'
import { ElMessage, ElMessageBox, ElPagination } from 'element-plus'
import { listLocations } from '@/api/wms/location'
import { listWarehouses, type WarehouseItem } from '@/api/wms/warehouse'
import {
  createOutboundOrder,
  deleteOutboundOrder,
  executeOutboundOrder,
  getOutboundOrder,
  listOutboundDetails,
  listOutboundOrders,
  listOutboundSn,
  type OutboundItem,
  type OutboundMethod,
  type OutboundOrder,
  type OutboundRecord,
  type OutboundSnRecord,
  type OutboundStatus,
} from '@/api/wms/outbound'

type Method = OutboundMethod
type ExecuteLocation = { location: string; qty: number; snList: string }
type ExecuteItem = OutboundItem & { remainingQty: number; locations: ExecuteLocation[] }
type CreateItem = { sku: string; name: string; spec: string; method: Method; qty: number; unitPrice: number }
type CreateForm = { code: string; orderNo: string; customer: string; warehouseCode: string; creator: string; remark: string; items: CreateItem[] }
type SnRecord = OutboundSnRecord

const InboundPagination = defineComponent({
  props: { page: { type: Number, required: true }, size: { type: Number, required: true }, total: { type: Number, required: true } },
  emits: ['update:page', 'update:size'],
  setup(props, { emit }) {
    return () => h('div', { class: 'wms-pagination' }, h(ElPagination, {
      currentPage: props.page,
      pageSize: props.size,
      total: props.total,
      background: true,
      layout: 'total, prev, pager, next, jumper',
      'onUpdate:currentPage': (value: number) => emit('update:page', value),
      'onUpdate:pageSize': (value: number) => emit('update:size', value),
    }))
  },
})

const activeTab = ref('orders')
const orderSearch = ref('')
const orderStatus = ref('')
const orderPage = ref(1)
const orderPageSize = ref(10)
const detailSearch = ref('')
const detailMethod = ref('')
const detailPage = ref(1)
const detailPageSize = ref(10)
const snSearch = ref('')
const snStatus = ref('')
const snPage = ref(1)
const snPageSize = ref(10)
const createVisible = ref(false)
const executeVisible = ref(false)
const detailVisible = ref(false)
const executeOrder = ref<OutboundOrder | null>(null)
const detailOrder = ref<OutboundOrder | null>(null)
const executeItems = ref<ExecuteItem[]>([])
const executeOperator = ref('')
const orders = ref<OutboundOrder[]>([])
const outboundRecords = ref<OutboundRecord[]>([])
const snRecords = ref<SnRecord[]>([])
const warehouses = ref<WarehouseItem[]>([])
const locations = ref<string[]>([])

const methodText: Record<Method, string> = { qty: '数量出库', sn: 'SN出库' }
const statusText: Record<OutboundStatus, string> = { completed: '已完成', partial: '部分出库', pending: '待出库' }
const statusType: Record<OutboundStatus, 'success' | 'warning' | 'info'> = { completed: 'success', partial: 'warning', pending: 'warning' }
const getMethodText = (method: Method) => methodText[method]
const getStatusText = (status: OutboundStatus) => statusText[status]
const getStatusType = (status: OutboundStatus) => statusType[status]

function emptyCreateItem(): CreateItem { return { sku: '', name: '', spec: '', method: 'qty', qty: 1, unitPrice: 0 } }
function emptyCreateForm(): CreateForm { return { code: '', orderNo: '', customer: '', warehouseCode: '', creator: '', remark: '', items: [emptyCreateItem()] } }
const createForm = ref<CreateForm>(emptyCreateForm())
const createTotalQty = computed(() => createForm.value.items.reduce((sum, item) => sum + Number(item.qty || 0), 0))
const createTotalAmount = computed(() => createForm.value.items.reduce((sum, item) => sum + Number(item.qty || 0) * Number(item.unitPrice || 0), 0))

const filteredOrders = computed(() => orders.value)
const pagedOrders = computed(() => paginate(filteredOrders.value, orderPage.value, orderPageSize.value))
const filteredDetails = computed(() => outboundRecords.value)
const pagedDetails = computed(() => paginate(filteredDetails.value, detailPage.value, detailPageSize.value))
const filteredSnRecords = computed(() => snRecords.value)
const pagedSnRecords = computed(() => paginate(filteredSnRecords.value, snPage.value, snPageSize.value))
const detailRecords = computed(() => {
  if (!detailOrder.value) return []
  if (detailOrder.value.records?.length) return detailOrder.value.records
  return outboundRecords.value.filter((item) => item.outboundCode === detailOrder.value?.code)
})

function remainingQty(row: OutboundItem) { return Number(row.qty || 0) - Number(row.outboundQty || 0) }

function paginate<T>(rows: T[], page: number, size: number) { return rows.slice((page - 1) * size, page * size) }
function apiOk<T>(response: { code: number; message: string; data: T }) { if (response.code !== 0) { ElMessage.error(response.message || '接口请求失败'); return false } return true }
function normalizeItem(item: OutboundItem): OutboundItem { return { ...item, spec: item.spec || '', qty: Number(item.qty || 0), outboundQty: Number(item.outboundQty || 0), unitPrice: Number(item.unitPrice || 0) } }
function normalizeRecord(record: OutboundRecord): OutboundRecord { return { ...record, spec: record.spec || '', location: record.location || '-', qty: Number(record.qty || 0), unitPrice: Number(record.unitPrice || 0), operator: record.operator || '-' } }
function normalizeOrder(order: OutboundOrder): OutboundOrder { return { ...order, orderNo: order.orderNo || '-', warehouse: order.warehouse || order.warehouseCode, materialCount: Number(order.materialCount || 0), totalQty: Number(order.totalQty || 0), outboundQty: Number(order.outboundQty || 0), outboundRate: Number(order.outboundRate || 0), totalAmount: Number(order.totalAmount || 0), items: order.items?.map(normalizeItem), records: order.records?.map(normalizeRecord) } }

async function loadOrders() { const response = await listOutboundOrders({ keyword: orderSearch.value, status: orderStatus.value }); if (apiOk(response)) orders.value = response.data.map(normalizeOrder) }
async function loadDetails() { const response = await listOutboundDetails({ keyword: detailSearch.value, method: detailMethod.value }); if (apiOk(response)) outboundRecords.value = response.data.map(normalizeRecord) }
async function loadSnRecords() { const response = await listOutboundSn({ keyword: snSearch.value, status: snStatus.value }); if (apiOk(response)) snRecords.value = response.data.map((item) => ({ ...item, outboundCode: item.outboundCode || '-', outboundTime: item.outboundTime || '-' })) }
async function loadOptions() { const [warehouseResponse, locationResponse] = await Promise.all([listWarehouses({ status: 1 }), listLocations({ status: 1 })]); if (apiOk(warehouseResponse)) warehouses.value = warehouseResponse.data; if (apiOk(locationResponse)) locations.value = locationResponse.data.map((item) => item.code) }
async function refreshAll() { await Promise.all([loadOrders(), loadDetails(), loadSnRecords(), loadOptions()]) }

function exportOrders() {
  if (!filteredOrders.value.length) { ElMessage.warning('暂无可导出的出库单'); return }
  const headers = ['出库单号', '关联销售订单', '客户名称', '仓库', '物料种类', '总数量', '已出库', '总金额', '出库人', '出库时间', '状态']
  const rows = filteredOrders.value.map((order) => [order.code, order.orderNo, order.customer, order.warehouse, order.materialCount, order.totalQty, order.outboundQty, order.totalAmount, order.creator, order.createTime, getStatusText(order.status)])
  const csv = [headers, ...rows].map((row) => row.map((cell) => `"${String(cell ?? '').replace(/"/g, '""')}"`).join(',')).join('\n')
  const blob = new Blob([`﻿${csv}`], { type: 'text/csv;charset=utf-8;' })
  const link = document.createElement('a')
  link.href = URL.createObjectURL(blob)
  link.download = `出库单_${new Date().toISOString().slice(0, 10)}.csv`
  link.click()
  URL.revokeObjectURL(link.href)
  ElMessage.success('已导出出库单列表')
}

function openCreate() { createForm.value = emptyCreateForm(); createVisible.value = true }
function addCreateItem() { createForm.value.items.push(emptyCreateItem()) }
function removeCreateItem(index: number) { if (createForm.value.items.length === 1) { ElMessage.warning('至少保留一条物料明细'); return } createForm.value.items.splice(index, 1) }
async function submitCreate() {
  const form = createForm.value
  if (!form.code.trim() || !form.customer.trim() || !form.warehouseCode || !form.creator.trim()) { ElMessage.warning('请完善出库单号、客户、仓库和出库人'); return }
  for (const [index, item] of form.items.entries()) {
    if (!item.sku.trim() || !item.name.trim()) { ElMessage.warning(`请完善第 ${index + 1} 行物料的 SKU 和名称`); return }
    if (Number(item.qty || 0) <= 0) { ElMessage.warning(`第 ${index + 1} 行物料数量必须大于 0`); return }
  }
  const response = await createOutboundOrder({ code: form.code.trim(), orderNo: form.orderNo.trim(), customer: form.customer.trim(), warehouseCode: form.warehouseCode, creator: form.creator.trim(), remark: form.remark.trim(), items: form.items.map((item) => ({ sku: item.sku.trim(), name: item.name.trim(), spec: item.spec.trim(), method: item.method, qty: Number(item.qty || 0), unitPrice: Number(item.unitPrice || 0) })) })
  if (!apiOk(response)) return
  createVisible.value = false
  await loadOrders()
  ElMessage.success('新增出库单成功')
}

async function showDetail(row: OutboundOrder) { if (!row.id) return; const response = await getOutboundOrder(row.id); if (!apiOk(response)) return; detailOrder.value = normalizeOrder(response.data); detailVisible.value = true }
async function removeOrder(row: OutboundOrder) { if (!row.id) return; try { await ElMessageBox.confirm('确认删除该出库单？', '删除确认', { type: 'warning' }); const response = await deleteOutboundOrder(row.id); if (!apiOk(response)) return; await refreshAll(); ElMessage.success('删除成功') } catch { } }
async function openExecute(row: OutboundOrder) { if (!row.id) return; const response = await getOutboundOrder(row.id); if (!apiOk(response)) return; const order = normalizeOrder(response.data); executeOrder.value = order; executeItems.value = (order.items || []).filter((item) => Number(item.outboundQty || 0) < Number(item.qty || 0)).map((item) => ({ ...item, remainingQty: Number(item.qty || 0) - Number(item.outboundQty || 0), locations: [{ location: item.locationCode || '', qty: 0, snList: '' }] })); executeOperator.value = ''; executeVisible.value = true }
function addLocation(item: ExecuteItem) { item.locations.push({ location: '', qty: 0, snList: '' }) }
function removeLocation(item: ExecuteItem, index: number) { if (item.locations.length === 1) return; item.locations.splice(index, 1) }
function fillSn(item: ExecuteItem, row: ExecuteLocation) { const available = snRecords.value.filter((sn) => sn.sku === item.sku && sn.status === 'inbound').slice(0, Number(row.qty || item.remainingQty)); row.snList = available.map((sn) => sn.sn).join(',') }
async function submitExecute() {
  if (!executeOrder.value?.id) return
  if (!executeOperator.value.trim()) { ElMessage.warning('请填写出库操作员'); return }
  let totalOutboundQty = 0
  for (const item of executeItems.value) {
    const itemQty = item.locations.reduce((sum, loc) => sum + Number(loc.qty || 0), 0)
    if (itemQty > item.remainingQty) { ElMessage.warning(`${item.name} 本次出库数量不能超过剩余可出库数量`); return }
    for (const loc of item.locations) {
      if (Number(loc.qty || 0) > 0 && !loc.location) { ElMessage.warning(`请为 ${item.name} 选择库位`); return }
      if (Number(loc.qty || 0) > 0 && item.method === 'sn') {
        const sns = loc.snList.split(/[,，\n]/).map((sn) => sn.trim()).filter(Boolean)
        if (sns.length !== Number(loc.qty || 0)) { ElMessage.warning(`${item.name} 的SN码数量与出库数量不一致`); return }
        const unavailable = sns.find((sn) => !snRecords.value.some((record) => record.sn === sn && record.status === 'inbound'))
        if (unavailable) { ElMessage.warning(`SN码 ${unavailable} 不可出库`); return }
      }
    }
    totalOutboundQty += itemQty
  }
  if (totalOutboundQty <= 0) { ElMessage.warning('请至少选择一种物料进行出库'); return }
  try {
    await ElMessageBox.confirm(`确定执行出库吗？本次将出库 ${totalOutboundQty} 件物料`, '确认出库', { type: 'info' })
    const response = await executeOutboundOrder({ id: executeOrder.value.id, operator: executeOperator.value.trim(), items: executeItems.value.map((item) => ({ id: item.id, locations: item.locations.map((loc) => ({ locationCode: loc.location, qty: Number(loc.qty || 0), snList: loc.snList })) })) })
    if (!apiOk(response)) return
    executeVisible.value = false
    await refreshAll()
    ElMessage.success('出库成功')
  } catch { }
}

watch([orderSearch, orderStatus], () => { orderPage.value = 1; loadOrders() })
watch([detailSearch, detailMethod], () => { detailPage.value = 1; loadDetails() })
watch([snSearch, snStatus], () => { snPage.value = 1; loadSnRecords() })
onMounted(refreshAll)
</script>
