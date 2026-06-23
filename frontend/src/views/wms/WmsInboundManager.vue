<template>
  <section>
    <div class="sub-page-header">
      <div>
        <h2>入库管理</h2>
      </div>
      <el-space>
        <el-button plain>导出</el-button>
        <el-button plain>刷新</el-button>
        <el-button type="primary" @click="openCreate">+ 新增入库</el-button>
      </el-space>
    </div>

    <el-tabs v-model="activeTab" class="wms-inbound-tabs" type="card">
      <el-tab-pane label="入库单列表" name="orders" />
      <el-tab-pane label="入库明细查询" name="details" />
      <el-tab-pane label="SN信息查询" name="sn" />
    </el-tabs>

    <div class="table-card wms-inbound-card">
      <template v-if="activeTab === 'orders'">
        <div class="filter-bar">
          <div class="filter-left">
            <el-input v-model="orderSearchInput" clearable placeholder="搜索入库单号/供应商..." style="width:260px" />
            <el-select v-model="orderStatus" clearable placeholder="状态筛选" style="width:120px">
              <el-option label="已完成" value="completed" />
              <el-option label="部分入库" value="partial" />
              <el-option label="待入库" value="pending" />
            </el-select>
          </div>
        </div>
        <el-table :data="pagedOrders" stripe style="width:100%">
          <el-table-column prop="code" label="入库单号" width="140" />
          <el-table-column prop="poCode" label="关联采购单" width="140">
            <template #default="{ row }"><el-button link type="primary">{{ row.poCode }}</el-button></template>
          </el-table-column>
          <el-table-column prop="supplier" label="供应商" min-width="120" />
          <el-table-column prop="warehouse" label="仓库" width="120" />
          <el-table-column prop="materialCount" label="物料种类" width="90" align="right">
            <template #default="{ row }"><b>{{ row.materialCount }}种</b></template>
          </el-table-column>
          <el-table-column label="入库进度" width="140">
            <template #default="{ row }">
              <div class="inbound-progress"><span :class="row.status" :style="{ width: row.inboundRate + '%' }"></span><em>{{ row.inboundQty }}/{{ row.totalQty }}</em></div>
            </template>
          </el-table-column>
          <el-table-column prop="totalQty" label="总数量" width="90" align="right">
            <template #default="{ row }"><b>{{ row.totalQty }}</b></template>
          </el-table-column>
          <el-table-column label="总金额" width="120" align="right">
            <template #default="{ row }">¥{{ row.totalAmount.toFixed(2) }}</template>
          </el-table-column>
          <el-table-column label="质检状态" width="90" align="center">
            <template #default="{ row }"><el-tag type="success" effect="light">{{ getInspectText(row.inspectStatus) }}</el-tag></template>
          </el-table-column>
          <el-table-column prop="creator" label="入库人" width="100" />
          <el-table-column prop="createTime" label="入库时间" width="150" />
          <el-table-column label="状态" width="100" align="center">
            <template #default="{ row }"><el-tag :type="getStatusType(row.status)" effect="light">{{ getStatusText(row.status) }}</el-tag></template>
          </el-table-column>
          <el-table-column label="操作" width="170" fixed="right" align="center">
            <template #default="{ row }">
              <el-button link type="primary" @click="showDetail(row)">详情</el-button>
              <el-button v-if="row.status !== 'completed'" link type="success" @click="openExecute(row)">执行入库</el-button>
              <el-button link type="danger" @click="removeOrder(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <InboundPagination v-model:page="orderPage" v-model:size="orderPageSize" :total="filteredOrders.length" />
      </template>

      <template v-if="activeTab === 'details'">
        <div class="filter-bar">
          <div class="filter-left">
            <el-input v-model="detailSearch" clearable placeholder="搜索入库单号/SKU/物料名称..." style="width:260px" />
            <el-select v-model="detailMethod" clearable placeholder="入库方式" style="width:120px">
              <el-option label="数量入库" value="qty" />
              <el-option label="SN入库" value="sn" />
            </el-select>
          </div>
        </div>
        <el-table :data="pagedDetails" stripe style="width:100%">
          <el-table-column prop="inboundCode" label="入库单号" width="150" />
          <el-table-column prop="sku" label="SKU编码" width="120" />
          <el-table-column prop="name" label="物料名称" min-width="140" />
          <el-table-column prop="spec" label="规格型号" width="130" />
          <el-table-column prop="location" label="库位" width="120" />
          <el-table-column label="入库方式" width="110" align="center">
            <template #default="{ row }"><el-tag :type="row.method === 'sn' ? 'warning' : 'primary'" effect="light">{{ getMethodText(row.method) }}</el-tag></template>
          </el-table-column>
          <el-table-column prop="qty" label="入库数量" width="100" align="right"><template #default="{ row }"><b>{{ row.qty }}</b></template></el-table-column>
          <el-table-column prop="unitPrice" label="单价" width="100" align="right" />
          <el-table-column label="金额" width="120" align="right"><template #default="{ row }">¥{{ (row.qty * row.unitPrice).toFixed(2) }}</template></el-table-column>
          <el-table-column prop="createTime" label="入库时间" width="160" />
          <el-table-column prop="operator" label="操作人" width="100" />
        </el-table>
        <InboundPagination v-model:page="detailPage" v-model:size="detailPageSize" :total="filteredDetails.length" />
      </template>

      <template v-if="activeTab === 'sn'">
        <div class="filter-bar">
          <div class="filter-left">
            <el-checkbox v-model="snSelectAll" @change="toggleSnSelectAll" />
            <span>已选 {{ selectedSnCount }} 条</span>
            <el-input v-model="snSearch" clearable placeholder="搜索SN码/SKU/物料名称..." style="width:260px" />
            <el-select v-model="snStatus" clearable placeholder="状态筛选" style="width:120px">
              <el-option label="已入库" value="inbound" />
              <el-option label="已出库" value="outbound" />
            </el-select>
          </div>
          <el-button type="primary" :disabled="selectedSnCount === 0">批量打印SN码</el-button>
        </div>
        <el-table :data="pagedSnRecords" stripe style="width:100%">
          <el-table-column label="选择" width="70" align="center"><template #default="{ row }"><el-checkbox v-model="row.selected" /></template></el-table-column>
          <el-table-column prop="sn" label="SN码" width="180" />
          <el-table-column prop="sku" label="SKU编码" width="120" />
          <el-table-column prop="name" label="物料名称" width="130" />
          <el-table-column prop="spec" label="规格型号" width="130" />
          <el-table-column prop="location" label="库位" width="120" />
          <el-table-column prop="inboundCode" label="入库单号" width="150"><template #default="{ row }"><el-button link type="primary">{{ row.inboundCode }}</el-button></template></el-table-column>
          <el-table-column prop="inboundTime" label="入库时间" width="170" />
          <el-table-column prop="outboundCode" label="出库单号" width="150"><template #default="{ row }"><span :class="{ danger: row.outboundCode !== '-' }">{{ row.outboundCode }}</span></template></el-table-column>
          <el-table-column prop="outboundTime" label="出库时间" width="170" />
          <el-table-column label="状态" width="100" align="center"><template #default="{ row }"><el-tag :type="row.status === 'inbound' ? 'success' : 'danger'" effect="light">{{ row.status === 'inbound' ? '已入库' : '已出库' }}</el-tag></template></el-table-column>
        </el-table>
        <InboundPagination v-model:page="snPage" v-model:size="snPageSize" :total="filteredSnRecords.length" />
      </template>
    </div>

    <el-dialog v-model="executeVisible" :title="`执行入库 - ${executeOrder?.code || ''}`" width="1000px" class="execute-dialog">
      <div v-if="executeOrder">
        <div class="execute-summary">
          <b>供应商：</b>{{ executeOrder.supplier }} <b>仓库：</b>{{ executeOrder.warehouse }} <b>入库进度：</b>{{ executeOrder.inboundQty }}/{{ executeOrder.totalQty }}
        </div>
        <div v-for="item in executeItems" :key="item.sku" class="execute-item">
          <div class="execute-item-head">
            <span><b>SKU:</b><br>{{ item.sku }}</span>
            <span><b>物料名称:</b> {{ item.name }}</span>
            <span><b>规格:</b><br>{{ item.spec }}</span>
            <span><b>入库方式:</b><br><el-tag :type="item.method === 'sn' ? 'warning' : 'primary'" effect="light">{{ getMethodText(item.method) }}</el-tag></span>
            <span><b>总数量:</b><br>{{ item.qty }}</span>
            <span><b>已入库:</b><br><em class="success">{{ item.inboundQty }}</em></span>
            <span><b>剩余可入库:</b><br><em class="primary">{{ item.remainingQty }}</em></span>
            <span><b>本次入库合计:</b><br><em class="warning">{{ item.locations.reduce((sum, loc) => sum + Number(loc.qty || 0), 0) }}</em></span>
            <el-button type="primary" @click="addLocation(item)">+ 添加入库行</el-button>
          </div>
          <el-table :data="item.locations" size="small" border>
            <el-table-column label="序号" type="index" width="70" align="center" />
            <el-table-column label="库位" width="220"><template #default="{ row }"><el-select v-model="row.location" placeholder="选择库位" style="width:100%"><el-option v-for="loc in locations" :key="loc" :label="loc" :value="loc" /></el-select></template></el-table-column>
            <el-table-column label="入库数量" width="140" align="center"><template #default="{ row }"><el-input-number v-model="row.qty" :min="0" :max="item.remainingQty" size="small" /></template></el-table-column>
            <el-table-column label="SN码（SN入库时填写）"><template #default="{ row }"><el-input v-model="row.snList" :disabled="item.method !== 'sn'" placeholder="SN码，逗号分隔" /></template></el-table-column>
            <el-table-column label="操作" width="100" align="center"><template #default="{ $index }"><el-button link type="danger" @click="removeLocation(item, $index)">删除</el-button></template></el-table-column>
          </el-table>
        </div>
        <div class="execute-operator"><span>入库操作员：</span><el-input v-model="executeOperator" placeholder="请输入操作员名称" style="width:220px" /></div>
      </div>
      <template #footer>
        <el-button @click="executeVisible = false">取消</el-button>
        <el-button type="primary" @click="submitExecute">确认入库</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="createVisible" title="新增入库" width="760px" class="create-inbound-dialog">
      <el-form label-width="92px">
        <el-form-item label="入库单号" required>
          <el-input v-model="createForm.code" placeholder="自动生成" />
        </el-form-item>
        <el-form-item label="关联采购单">
          <el-input v-model="createForm.poCode" placeholder="请输入关联采购单号" />
        </el-form-item>
        <el-form-item label="供应商" required>
          <el-input v-model="createForm.supplier" placeholder="请输入供应商名称" />
        </el-form-item>
        <el-form-item label="入库仓库" required>
          <el-select v-model="createForm.warehouseCode" placeholder="请选择仓库" style="width:100%">
            <el-option label="主仓库-A区" value="主仓库-A区" />
            <el-option label="主仓库-B区" value="主仓库-B区" />
            <el-option label="备件仓" value="备件仓" />
          </el-select>
        </el-form-item>
        <el-form-item label="入库人" required>
          <el-input v-model="createForm.creator" placeholder="请输入入库人" />
        </el-form-item>
        <el-form-item label="质检状态">
          <el-select v-model="createForm.inspectStatus" style="width:100%">
            <el-option label="合格" value="qualified" />
            <el-option label="待质检" value="pending" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="createForm.remark" type="textarea" placeholder="请输入备注信息" />
        </el-form-item>
        <el-form-item label="物料明细">
          <el-table :data="createForm.items" border size="small" style="width:100%">
            <el-table-column label="SKU编码" width="120"><template #default="{ row }"><el-input v-model="row.sku" placeholder="SKU编码" /></template></el-table-column>
            <el-table-column label="物料名称" min-width="130"><template #default="{ row }"><el-input v-model="row.name" placeholder="物料名称" /></template></el-table-column>
            <el-table-column label="规格型号" width="120"><template #default="{ row }"><el-input v-model="row.spec" placeholder="规格型号" /></template></el-table-column>
            <el-table-column label="入库方式" width="120"><template #default="{ row }"><el-select v-model="row.method" placeholder="方式"><el-option label="数量入库" value="qty" /><el-option label="SN入库" value="sn" /></el-select></template></el-table-column>
            <el-table-column label="数量" width="120" align="center"><template #default="{ row }"><el-input-number v-model="row.qty" :min="1" :step="1" size="small" /></template></el-table-column>
            <el-table-column label="单价" width="120" align="center"><template #default="{ row }"><el-input-number v-model="row.unitPrice" :min="0" :precision="2" :step="1" size="small" /></template></el-table-column>
            <el-table-column label="金额" width="90" align="right"><template #default="{ row }">{{ (Number(row.qty || 0) * Number(row.unitPrice || 0)).toFixed(2) }}</template></el-table-column>
            <el-table-column label="操作" width="80" align="center"><template #default="{ $index }"><el-button link type="danger" @click="removeCreateItem($index)">删除</el-button></template></el-table-column>
          </el-table>
          <div class="create-items-footer">
            <span>合计：{{ createTotalQty }} 件 / ¥{{ createTotalAmount.toFixed(2) }}</span>
            <el-button type="primary" @click="addCreateItem">+ 添加物料</el-button>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" @click="submitCreate">确定提交</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="detailVisible" title="入库单详情" width="1080px" class="inbound-detail-dialog">
      <div v-if="detailOrder" class="inbound-detail-layout">
        <div class="inbound-detail-summary">
          <div><span>入库总数</span><b>{{ detailOrder.totalQty }}</b></div>
          <div><span>已入库</span><b class="success">{{ detailOrder.inboundQty }}</b></div>
          <div><span>入库进度</span><b>{{ detailOrder.inboundRate }}%</b></div>
          <div><span>总金额</span><b>¥{{ detailOrder.totalAmount.toFixed(2) }}</b></div>
        </div>

        <el-descriptions border :column="4" class="inbound-detail-desc">
          <el-descriptions-item label="入库单号">{{ detailOrder.code }}</el-descriptions-item>
          <el-descriptions-item label="关联采购单">{{ detailOrder.poCode }}</el-descriptions-item>
          <el-descriptions-item label="供应商">{{ detailOrder.supplier }}</el-descriptions-item>
          <el-descriptions-item label="仓库">{{ detailOrder.warehouse }}</el-descriptions-item>
          <el-descriptions-item label="质检状态"><el-tag size="small" type="success">{{ getInspectText(detailOrder.inspectStatus) }}</el-tag></el-descriptions-item>
          <el-descriptions-item label="状态"><el-tag size="small" :type="getStatusType(detailOrder.status)">{{ getStatusText(detailOrder.status) }}</el-tag></el-descriptions-item>
          <el-descriptions-item label="入库人">{{ detailOrder.creator }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ detailOrder.createTime }}</el-descriptions-item>
          <el-descriptions-item label="备注" :span="4">{{ detailOrder.remark || '-' }}</el-descriptions-item>
        </el-descriptions>

        <div class="inbound-detail-section">
          <div class="inbound-detail-title"><b>入库物料</b><span>{{ detailOrder.items?.length || 0 }} 条</span></div>
          <el-table :data="detailOrder.items || []" border stripe size="small" max-height="260" empty-text="暂无入库物料">
            <el-table-column prop="sku" label="SKU编码" width="130" />
            <el-table-column prop="name" label="物料名称" min-width="150" />
            <el-table-column prop="spec" label="规格型号" min-width="120" />
            <el-table-column label="入库方式" width="100" align="center">
              <template #default="{ row }"><el-tag :type="row.method === 'sn' ? 'warning' : 'primary'" effect="light">{{ getMethodText(row.method) }}</el-tag></template>
            </el-table-column>
            <el-table-column prop="locationCode" label="默认库位" width="110" />
            <el-table-column prop="batchNo" label="批次号" width="120"><template #default="{ row }">{{ row.batchNo || '-' }}</template></el-table-column>
            <el-table-column prop="qty" label="应入库" width="90" align="right" />
            <el-table-column prop="inboundQty" label="已入库" width="90" align="right" />
            <el-table-column label="待入库" width="90" align="right"><template #default="{ row }">{{ remainingQty(row) }}</template></el-table-column>
            <el-table-column prop="unitPrice" label="单价" width="90" align="right"><template #default="{ row }">¥{{ Number(row.unitPrice || 0).toFixed(2) }}</template></el-table-column>
            <el-table-column label="金额" width="110" align="right"><template #default="{ row }">¥{{ (Number(row.qty || 0) * Number(row.unitPrice || 0)).toFixed(2) }}</template></el-table-column>
          </el-table>
        </div>

        <div class="inbound-detail-section">
          <div class="inbound-detail-title"><b>入库记录</b><span>{{ detailRecords.length }} 条</span></div>
          <el-table :data="detailRecords" border stripe size="small" max-height="260" empty-text="暂无入库记录">
            <el-table-column prop="createTime" label="入库时间" width="165" />
            <el-table-column prop="inboundCode" label="入库单号" width="135" />
            <el-table-column prop="sku" label="SKU编码" width="130" />
            <el-table-column prop="name" label="物料名称" min-width="150" />
            <el-table-column prop="spec" label="规格型号" min-width="120" />
            <el-table-column prop="location" label="入库库位" width="110" />
            <el-table-column label="入库方式" width="100" align="center">
              <template #default="{ row }"><el-tag :type="row.method === 'sn' ? 'warning' : 'primary'" effect="light">{{ getMethodText(row.method) }}</el-tag></template>
            </el-table-column>
            <el-table-column prop="qty" label="入库数量" width="90" align="right" />
            <el-table-column prop="unitPrice" label="单价" width="90" align="right"><template #default="{ row }">¥{{ Number(row.unitPrice || 0).toFixed(2) }}</template></el-table-column>
            <el-table-column label="金额" width="110" align="right"><template #default="{ row }">¥{{ (Number(row.qty || 0) * Number(row.unitPrice || 0)).toFixed(2) }}</template></el-table-column>
            <el-table-column prop="operator" label="操作人" width="100" />
          </el-table>
        </div>
      </div>
    </el-dialog>
  </section>
</template>

<script setup lang="ts">
import { computed, defineComponent, h, onMounted, ref, watch } from 'vue'
import { ElMessage, ElMessageBox, ElPagination } from 'element-plus'
import {
  createInboundOrder,
  deleteInboundOrder,
  executeInboundOrder,
  getInboundOrder,
  listInboundDetails,
  listInboundOrders,
  listInboundSn,
  type InboundItem,
  type InboundMethod,
  type InboundOrder,
  type InboundRecord,
  type InboundSnRecord,
  type InboundStatus,
  type InspectStatus,
} from '@/api/wms/inbound'
import { listLocations } from '@/api/wms/location'
import { listWarehouses, type WarehouseItem } from '@/api/wms/warehouse'

type Method = InboundMethod
type ExecuteLocation = { location: string; qty: number; snList: string }
type ExecuteItem = InboundItem & { remainingQty: number; locations: ExecuteLocation[] }
type CreateItem = { sku: string; name: string; spec: string; method: Method; qty: number; unitPrice: number }
type CreateForm = { code: string; poCode: string; supplier: string; warehouseCode: string; creator: string; inspectStatus: InspectStatus; remark: string; items: CreateItem[] }
type SnRecord = InboundSnRecord & { selected?: boolean }

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
const orderSearchInput = ref('')
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
const snSelectAll = ref(false)
const createVisible = ref(false)
const executeVisible = ref(false)
const detailVisible = ref(false)
const executeOrder = ref<InboundOrder | null>(null)
const detailOrder = ref<InboundOrder | null>(null)
const executeItems = ref<ExecuteItem[]>([])
const executeOperator = ref('')
const orders = ref<InboundOrder[]>([])
const inboundRecords = ref<InboundRecord[]>([])
const snRecords = ref<SnRecord[]>([])
const warehouses = ref<WarehouseItem[]>([])
const locations = ref<string[]>([])

const methodText: Record<Method, string> = { qty: '数量入库', sn: 'SN入库' }
const statusText: Record<InboundStatus, string> = { completed: '已完成', partial: '部分入库', pending: '待入库' }
const statusType: Record<InboundStatus, 'success' | 'warning' | 'info'> = { completed: 'success', partial: 'warning', pending: 'warning' }
const inspectText: Record<InspectStatus, string> = { qualified: '合格', passed: '合格', pending: '待质检' }
const getMethodText = (method: Method) => methodText[method]
const getStatusText = (status: InboundStatus) => statusText[status]
const getStatusType = (status: InboundStatus) => statusType[status]
const getInspectText = (status: InspectStatus) => inspectText[status]

function emptyCreateItem(): CreateItem {
  return { sku: '', name: '', spec: '', method: 'qty', qty: 1, unitPrice: 0 }
}

function emptyCreateForm(): CreateForm {
  return { code: '', poCode: '', supplier: '', warehouseCode: '', creator: '', inspectStatus: 'passed', remark: '', items: [emptyCreateItem()] }
}

const createForm = ref<CreateForm>(emptyCreateForm())
const createTotalQty = computed(() => createForm.value.items.reduce((sum, item) => sum + Number(item.qty || 0), 0))
const createTotalAmount = computed(() => createForm.value.items.reduce((sum, item) => sum + Number(item.qty || 0) * Number(item.unitPrice || 0), 0))

const filteredOrders = computed(() => orders.value)
const pagedOrders = computed(() => filteredOrders.value.slice((orderPage.value - 1) * orderPageSize.value, orderPage.value * orderPageSize.value))
const filteredDetails = computed(() => inboundRecords.value)
const pagedDetails = computed(() => filteredDetails.value.slice((detailPage.value - 1) * detailPageSize.value, detailPage.value * detailPageSize.value))
const filteredSnRecords = computed(() => snRecords.value)
const pagedSnRecords = computed(() => filteredSnRecords.value.slice((snPage.value - 1) * snPageSize.value, snPage.value * snPageSize.value))
const selectedSnCount = computed(() => snRecords.value.filter((item) => item.selected).length)
const detailRecords = computed(() => {
  if (!detailOrder.value) return []
  if (detailOrder.value.records?.length) return detailOrder.value.records
  return inboundRecords.value.filter((item) => item.inboundCode === detailOrder.value?.code)
})

function apiOk<T>(response: { code: number; message: string; data: T }) {
  if (response.code !== 0) {
    ElMessage.error(response.message || '接口请求失败')
    return false
  }
  return true
}

async function loadOrders() {
  const response = await listInboundOrders({ keyword: orderSearchInput.value, status: orderStatus.value })
  if (apiOk(response)) {
    orders.value = response.data.map(normalizeOrder)
  }
}

async function loadDetails() {
  const response = await listInboundDetails({ keyword: detailSearch.value, method: detailMethod.value })
  if (apiOk(response)) {
    inboundRecords.value = response.data.map(normalizeRecord)
  }
}

async function loadSnRecords() {
  const response = await listInboundSn({ keyword: snSearch.value, status: snStatus.value })
  if (apiOk(response)) {
    snRecords.value = response.data.map((item) => ({ ...item, outboundCode: item.outboundCode || '-', outboundTime: item.outboundTime || '-', selected: false }))
  }
}

async function loadOptions() {
  const [warehouseResponse, locationResponse] = await Promise.all([listWarehouses({ status: 1 }), listLocations({ status: 1 })])
  if (apiOk(warehouseResponse)) warehouses.value = warehouseResponse.data
  if (apiOk(locationResponse)) locations.value = locationResponse.data.map((item) => item.code)
}

async function refreshAll() {
  await Promise.all([loadOrders(), loadDetails(), loadSnRecords(), loadOptions()])
}

function normalizeOrder(order: InboundOrder): InboundOrder {
  return {
    ...order,
    poCode: order.poCode || '-',
    warehouse: order.warehouse || order.warehouseCode,
    materialCount: Number(order.materialCount || 0),
    totalQty: Number(order.totalQty || 0),
    inboundQty: Number(order.inboundQty || 0),
    inboundRate: Number(order.inboundRate || 0),
    totalAmount: Number(order.totalAmount || 0),
    items: order.items?.map(normalizeItem),
    records: order.records?.map(normalizeRecord),
  }
}

function normalizeRecord(record: InboundRecord): InboundRecord {
  return {
    ...record,
    spec: record.spec || '',
    location: record.location || '-',
    qty: Number(record.qty || 0),
    unitPrice: Number(record.unitPrice || 0),
    operator: record.operator || '-',
  }
}

function normalizeItem(item: InboundItem): InboundItem {
  return {
    ...item,
    spec: item.spec || '',
    qty: Number(item.qty || 0),
    inboundQty: Number(item.inboundQty || 0),
    unitPrice: Number(item.unitPrice || 0),
  }
}

function remainingQty(item: InboundItem) {
  return Math.max(0, Number(item.qty || 0) - Number(item.inboundQty || 0))
}

function openCreate() {
  createForm.value = emptyCreateForm()
  createVisible.value = true
}

function addCreateItem() {
  createForm.value.items.push(emptyCreateItem())
}

function removeCreateItem(index: number) {
  if (createForm.value.items.length === 1) {
    ElMessage.warning('至少保留一条物料明细')
    return
  }
  createForm.value.items.splice(index, 1)
}

async function submitCreate() {
  const form = createForm.value
  if (!form.code.trim() || !form.supplier.trim() || !form.warehouseCode || !form.creator.trim()) {
    ElMessage.warning('请完善入库单号、供应商、仓库和入库人')
    return
  }
  for (const [index, item] of form.items.entries()) {
    if (!item.sku.trim() || !item.name.trim()) {
      ElMessage.warning(`请完善第 ${index + 1} 行物料的 SKU 和名称`)
      return
    }
    if (Number(item.qty || 0) <= 0) {
      ElMessage.warning(`第 ${index + 1} 行物料数量必须大于 0`)
      return
    }
    if (Number(item.unitPrice || 0) < 0) {
      ElMessage.warning(`第 ${index + 1} 行物料单价不能小于 0`)
      return
    }
  }

  const response = await createInboundOrder({
    code: form.code.trim(),
    poCode: form.poCode.trim(),
    supplier: form.supplier.trim(),
    warehouseCode: form.warehouseCode,
    creator: form.creator.trim(),
    inspectStatus: form.inspectStatus,
    remark: form.remark.trim(),
    items: form.items.map((item) => ({
      sku: item.sku.trim(),
      name: item.name.trim(),
      spec: item.spec.trim(),
      method: item.method,
      qty: Number(item.qty || 0),
      unitPrice: Number(item.unitPrice || 0),
    })),
  })
  if (!apiOk(response)) return
  createVisible.value = false
  orderPage.value = 1
  await loadOrders()
  ElMessage.success('新增入库单成功')
}

async function showDetail(row: InboundOrder) {
  if (!row.id) return
  const response = await getInboundOrder(row.id)
  if (!apiOk(response)) return
  detailOrder.value = normalizeOrder(response.data)
  detailVisible.value = true
}

async function removeOrder(row: InboundOrder) {
  if (!row.id) return
  try {
    await ElMessageBox.confirm('确认删除该入库单？', '删除确认', { type: 'warning' })
    const response = await deleteInboundOrder(row.id)
    if (!apiOk(response)) return
    await refreshAll()
    ElMessage.success('删除成功')
  } catch {
    // user cancelled
  }
}

async function openExecute(row: InboundOrder) {
  if (!row.id) return
  const response = await getInboundOrder(row.id)
  if (!apiOk(response)) return
  const order = normalizeOrder(response.data)
  executeOrder.value = order
  executeItems.value = (order.items || []).filter((item) => Number(item.inboundQty || 0) < Number(item.qty || 0)).map((item) => ({
    ...item,
    inboundQty: Number(item.inboundQty || 0),
    remainingQty: Number(item.qty || 0) - Number(item.inboundQty || 0),
    locations: [{ location: '', qty: 0, snList: '' }],
  }))
  executeOperator.value = ''
  executeVisible.value = true
}

function addLocation(item: ExecuteItem) {
  item.locations.push({ location: '', qty: 0, snList: '' })
}

function removeLocation(item: ExecuteItem, index: number) {
  if (item.locations.length === 1) return
  item.locations.splice(index, 1)
}

async function submitExecute() {
  if (!executeOrder.value?.id) return
  if (!executeOperator.value.trim()) {
    ElMessage.warning('请输入入库操作员')
    return
  }
  let totalInboundQty = 0
  for (const item of executeItems.value) {
    const itemQty = item.locations.reduce((sum, loc) => sum + Number(loc.qty || 0), 0)
    if (itemQty > item.remainingQty) {
      ElMessage.warning(`${item.name} 本次入库数量不能超过剩余可入库数量`)
      return
    }
    for (const loc of item.locations) {
      if (Number(loc.qty || 0) > 0 && !loc.location) {
        ElMessage.warning(`请为 ${item.name} 选择库位`)
        return
      }
      if (Number(loc.qty || 0) > 0 && item.method === 'sn') {
        const sns = loc.snList.split(/[,，\n]/).map((sn) => sn.trim()).filter(Boolean)
        if (sns.length !== Number(loc.qty || 0)) {
          ElMessage.warning(`${item.name} 的SN码数量与入库数量不一致`)
          return
        }
      }
    }
    totalInboundQty += itemQty
  }
  if (totalInboundQty <= 0) {
    ElMessage.warning('请至少选择一种物料进行入库')
    return
  }

  try {
    await ElMessageBox.confirm(`确定执行入库吗？本次将入库 ${totalInboundQty} 件物料`, '确认入库', { type: 'info' })
    const response = await executeInboundOrder({
      id: executeOrder.value.id,
      operator: executeOperator.value.trim(),
      items: executeItems.value.map((item) => ({
        id: item.id,
        locations: item.locations.map((loc) => ({ locationCode: loc.location, qty: Number(loc.qty || 0), snList: loc.snList })),
      })),
    })
    if (!apiOk(response)) return
    executeVisible.value = false
    await refreshAll()
    ElMessage.success('入库成功')
  } catch {
    // user cancelled
  }
}

function toggleSnSelectAll(value: boolean) {
  snRecords.value.forEach((item) => { item.selected = value })
}

watch([orderSearchInput, orderStatus], () => {
  orderPage.value = 1
  loadOrders()
})
watch([detailSearch, detailMethod], () => {
  detailPage.value = 1
  loadDetails()
})
watch([snSearch, snStatus], () => {
  snPage.value = 1
  loadSnRecords()
})

onMounted(refreshAll)
</script>
