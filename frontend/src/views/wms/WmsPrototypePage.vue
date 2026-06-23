<template>
  <section>
    <div class="sub-page-header">
      <div>
        <h2>{{ config.title }}</h2>
        <p class="page-desc">{{ config.description }}</p>
      </div>
      <el-space v-if="config.kind === 'overview'">
        <el-button size="small" plain>导出</el-button>
        <el-button size="small" plain>刷新</el-button>
      </el-space>
    </div>

    <template v-if="config.kind === 'overview'">
      <div v-if="!config.hideStats" class="stats-row" :style="config.statColumns ? { gridTemplateColumns: `repeat(${config.statColumns}, 1fr)` } : undefined">
        <div v-for="item in config.stats" :key="item.label" class="stat-card" :class="item.color">
          <div class="stat-header"><span class="stat-title">{{ item.label }}</span><div class="stat-icon">{{ item.icon }}</div></div>
          <div class="stat-value">{{ item.value }}</div>
          <div class="stat-footer"><span :class="item.trendType">{{ item.trend }}</span></div>
        </div>
      </div>

      <div class="dashboard-grid">
        <div class="card">
          <div class="card-header"><div class="card-title">库存分布</div></div>
          <div class="donut-wrap">
            <div class="donut-chart"></div>
            <div class="donut-center"><strong>15.2万</strong><span>库存总量</span></div>
          </div>
          <div class="chart-legend">
            <span><i class="legend-blue"></i>原材料</span>
            <span><i class="legend-green"></i>电子件</span>
            <span><i class="legend-orange"></i>五金件</span>
            <span><i class="legend-purple"></i>成品</span>
          </div>
        </div>
        <div class="card">
          <div class="card-header"><div class="card-title">近7日出入库趋势</div></div>
          <div class="wms-trend-chart">
            <div v-for="day in trendDays" :key="day.label" class="trend-day">
              <div class="trend-bars">
                <span class="trend-bar inbound" :style="{ height: day.inbound + '%' }"></span>
                <span class="trend-bar outbound" :style="{ height: day.outbound + '%' }"></span>
              </div>
              <em>{{ day.label }}</em>
            </div>
          </div>
          <div class="chart-legend">
            <span><i class="legend-blue"></i>入库</span>
            <span><i class="legend-green"></i>出库</span>
          </div>
        </div>
      </div>
    </template>

    <template v-else>
      <div v-if="!config.hideStats" class="stats-row" :style="config.statColumns ? { gridTemplateColumns: `repeat(${config.statColumns}, 1fr)` } : undefined">
        <div v-for="item in config.stats" :key="item.label" class="stat-card" :class="item.color">
          <div class="stat-header"><span class="stat-title">{{ item.label }}</span><div class="stat-icon">{{ item.icon }}</div></div>
          <div class="stat-value">{{ item.value }}</div>
          <div class="stat-footer">{{ item.footer }}</div>
        </div>
      </div>

      <div v-if="config.tabs?.length" class="table-card wms-tabs-card">
        <el-tabs v-model="activeTab">
          <el-tab-pane v-for="tab in config.tabs" :key="tab.name" :label="tab.label" :name="tab.name">
            <WmsDataPanel :config="tab" />
          </el-tab-pane>
        </el-tabs>
      </div>
      <WmsDataPanel v-else :config="config" />
    </template>
  </section>
</template>

<script setup lang="ts">
import { computed, defineComponent, h, ref, watch } from 'vue'
import type { TagProps } from 'element-plus'
import { ElButton, ElDescriptions, ElDescriptionsItem, ElDialog, ElForm, ElFormItem, ElInput, ElMessage, ElMessageBox, ElOption, ElPagination, ElSelect, ElSpace, ElTable, ElTableColumn, ElTag } from 'element-plus'

type Stat = { label: string; value: string; footer?: string; trend?: string; trendType?: string; color: string; icon: string }
type Column = { prop: string; label: string; width?: number; minWidth?: number; align?: 'left' | 'center' | 'right'; type?: 'tag' | 'progress' | 'money' }
type Row = Record<string, string | number>
type DataPanelConfig = { searchPlaceholder?: string; filters?: string[]; columns: Column[]; rows: Row[]; actions?: string[] }
type PageConfig = DataPanelConfig & { title: string; description: string; kind?: 'overview'; primaryAction?: string; hideStats?: boolean; statColumns?: number; stats: Stat[]; tabs?: (DataPanelConfig & { name: string; label: string })[] }

const props = defineProps<{ page: string }>()
const activeTab = ref('orders')

const tagTypeMap: Record<string, TagProps['type']> = {
  启用: 'success', 正常: 'success', 国内品牌: 'success', 正常启用: 'success', 已完成: 'success', 已处理: 'success', 已入库: 'success', 已出库: 'success', 充足: 'success', 已审核: 'success', 已下单: 'success',
  国外品牌: 'warning', 待入库: 'warning', 待出库: 'warning', 待处理: 'warning', 进行中: 'warning', 待审核: 'warning', 低库存: 'warning', 部分入库: 'warning', 部分出库: 'warning', 待下单: 'warning', 中危: 'warning',
  禁用: 'info', 已停用: 'info', 草稿: 'info',
  库存不足: 'danger', 高危: 'danger', 异常: 'danger', 缺货: 'danger', 超储: 'danger',
}

const WmsDataPanel = defineComponent({
  name: 'WmsDataPanel',
  props: { config: { type: Object as () => DataPanelConfig, required: true } },
  setup(panelProps) {
    const searchText = ref('')
    const keyword = ref('')
    const filterValues = ref<Record<string, string>>({})
    const rows = ref<Row[]>([])
    const currentPage = ref(1)
    const pageSize = ref(10)
    const dialogVisible = ref(false)
    const detailVisible = ref(false)
    const dialogMode = ref<'add' | 'edit'>('add')
    const editingIndex = ref(-1)
    const form = ref<Row>({})
    const detailRow = ref<Row>({})

    const resetRows = () => {
      rows.value = panelProps.config.rows.map((row) => ({ ...row }))
      currentPage.value = 1
    }

    watch(() => panelProps.config, resetRows, { immediate: true })

    const editableColumns = computed(() => panelProps.config.columns.filter((column) => column.type !== 'progress' && column.type !== 'money'))
    const filteredRows = computed(() => rows.value.filter((row) => {
      const text = Object.values(row).join('')
      return !keyword.value || text.includes(keyword.value)
    }))
    const pagedRows = computed(() => filteredRows.value.slice((currentPage.value - 1) * pageSize.value, currentPage.value * pageSize.value))

    const emptyRow = () => panelProps.config.columns.reduce<Row>((row, column) => {
      row[column.prop] = column.type === 'tag' ? '启用' : ''
      return row
    }, {})

    const openAdd = () => {
      dialogMode.value = 'add'
      editingIndex.value = -1
      form.value = emptyRow()
      dialogVisible.value = true
    }

    const openEdit = (row: Row) => {
      dialogMode.value = 'edit'
      editingIndex.value = rows.value.indexOf(row)
      form.value = { ...row }
      dialogVisible.value = true
    }

    const openDetail = (row: Row) => {
      detailRow.value = { ...row }
      detailVisible.value = true
    }

    const removeRow = (row: Row) => {
      ElMessageBox.confirm('确认删除当前记录？', '删除确认', { type: 'warning' }).then(() => {
        rows.value = rows.value.filter((item) => item !== row)
        if ((currentPage.value - 1) * pageSize.value >= filteredRows.value.length && currentPage.value > 1) currentPage.value -= 1
        ElMessage.success('删除成功')
      }).catch(() => undefined)
    }

    const submitForm = () => {
      const payload = { ...form.value }
      if (dialogMode.value === 'add') {
        rows.value.unshift(payload)
        currentPage.value = 1
        ElMessage.success('新增成功')
      } else if (editingIndex.value > -1) {
        rows.value.splice(editingIndex.value, 1, payload)
        ElMessage.success('保存成功')
      }
      dialogVisible.value = false
    }

    const handleAction = (action: string, row: Row) => {
      if (action === '编辑') openEdit(row)
      else if (action === '详情') openDetail(row)
      else if (action === '删除') removeRow(row)
      else ElMessage.success(`${action}操作已触发`)
    }

    const renderCell = (column: Column, row: Row) => {
      const value = row[column.prop]
      if (column.type === 'tag') return h(ElTag, { type: tagTypeMap[String(value)] || 'primary', effect: 'light' }, () => String(value))
      if (column.type === 'progress') return h('div', { class: 'wms-progress' }, [h('span', { style: `width:${value}%` }), h('em', null, `${value}%`)])
      if (column.type === 'money') return `¥${Number(value).toLocaleString(undefined, { minimumFractionDigits: 2 })}`
      return String(value ?? '')
    }

    return () => h('div', { class: 'wms-data-panel' }, [
      h('div', { class: 'filter-bar' }, [
        h('div', { class: 'filter-left' }, [
          h(ElInput, {
            modelValue: searchText.value,
            'onUpdate:modelValue': (value: string) => { searchText.value = value },
            style: 'width:260px',
            clearable: true,
            placeholder: panelProps.config.searchPlaceholder || '请输入关键字',
          }),
          ...(panelProps.config.filters || []).map((filter) => h(ElSelect, {
            modelValue: filterValues.value[filter] || '',
            'onUpdate:modelValue': (value: string) => { filterValues.value[filter] = value },
            placeholder: filter,
            clearable: true,
            style: 'width:130px',
          }, () => ['全部', '启用', '禁用', '待处理', '已完成'].map((item) => h(ElOption, { label: item, value: item === '全部' ? '' : item })))),
          h(ElButton, { type: 'primary', onClick: () => { keyword.value = searchText.value; currentPage.value = 1 } }, () => '查询')
        ]),
        h(ElSpace, null, () => [
          h(ElButton, null, () => '导入'),
          h(ElButton, null, () => '导出'),
          h(ElButton, { type: 'primary', onClick: openAdd }, () => '新增'),
        ])
      ]),
      h(ElTable, { data: pagedRows.value, stripe: true, border: false, style: 'width:100%' }, () => [
        ...panelProps.config.columns.map((column) => h(ElTableColumn, {
          prop: column.prop,
          label: column.label,
          width: column.width,
          minWidth: column.minWidth,
          align: column.align,
        }, { default: ({ row }: { row: Row }) => renderCell(column, row) })),
        h(ElTableColumn, { label: '操作', width: 180, fixed: 'right', align: 'center' }, {
          default: ({ row }: { row: Row }) => (panelProps.config.actions || ['编辑', '详情', '删除']).map((action) => h(ElButton, { link: true, type: action === '删除' ? 'danger' : 'primary', onClick: () => handleAction(action, row) }, () => action))
        })
      ]),
      h('div', { class: 'wms-pagination' }, [
        h(ElPagination, {
          currentPage: currentPage.value,
          pageSize: pageSize.value,
          total: filteredRows.value.length,
          layout: 'total, sizes, prev, pager, next, jumper',
          pageSizes: [10, 20, 50, 100],
          background: true,
          'onUpdate:currentPage': (value: number) => { currentPage.value = value },
          'onUpdate:pageSize': (value: number) => { pageSize.value = value; currentPage.value = 1 },
        })
      ]),
      h(ElDialog, {
        modelValue: dialogVisible.value,
        'onUpdate:modelValue': (value: boolean) => { dialogVisible.value = value },
        title: dialogMode.value === 'add' ? '新增记录' : '编辑记录',
        width: '560px',
      }, {
        default: () => h(ElForm, { labelWidth: '96px', class: 'wms-crud-form' }, () => editableColumns.value.map((column) => h(ElFormItem, { label: column.label }, () => column.type === 'tag'
          ? h(ElSelect, {
              modelValue: form.value[column.prop],
              'onUpdate:modelValue': (value: string) => { form.value[column.prop] = value },
              style: 'width:100%',
              placeholder: `请选择${column.label}`,
            }, () => ['启用', '禁用', '待处理', '已完成', '充足', '低库存', '缺货'].map((item) => h(ElOption, { label: item, value: item })))
          : h(ElInput, {
              modelValue: form.value[column.prop],
              'onUpdate:modelValue': (value: string) => { form.value[column.prop] = value },
              placeholder: `请输入${column.label}`,
            })))),
        footer: () => h(ElSpace, null, () => [
          h(ElButton, { onClick: () => { dialogVisible.value = false } }, () => '取消'),
          h(ElButton, { type: 'primary', onClick: submitForm }, () => '保存'),
        ]),
      }),
      h(ElDialog, {
        modelValue: detailVisible.value,
        'onUpdate:modelValue': (value: boolean) => { detailVisible.value = value },
        title: '记录详情',
        width: '640px',
      }, () => h(ElDescriptions, { column: 2, border: true }, () => panelProps.config.columns.map((column) => h(ElDescriptionsItem, { label: column.label }, () => renderCell(column, detailRow.value)))))
    ])
  }
})

const trendDays = [
  { label: '5/22', inbound: 70, outbound: 58 },
  { label: '5/23', inbound: 62, outbound: 66 },
  { label: '5/24', inbound: 88, outbound: 52 },
  { label: '5/25', inbound: 78, outbound: 61 },
  { label: '5/26', inbound: 82, outbound: 69 },
  { label: '5/27', inbound: 74, outbound: 59 },
  { label: '5/28', inbound: 79, outbound: 57 },
]

const commonStats = (total: string, active: string, pending: string, risk: string): Stat[] => [
  { label: '总记录数', value: total, footer: '当前数据总量', color: 'blue', icon: '📊' },
  { label: '正常/完成', value: active, footer: '正常业务数据', color: 'green', icon: '✅' },
  { label: '进行中/待处理', value: pending, footer: '需要跟进', color: 'orange', icon: '⏳' },
  { label: '异常/预警', value: risk, footer: '重点关注', color: 'purple', icon: '⚠️' },
]

const configs: Record<string, PageConfig> = {
  overview: {
    kind: 'overview', title: '库存概览', description: '查看仓储运营、库存与出入库概览。', columns: [], rows: [],
    stats: [
      { label: '物料总数', value: '3,256', trend: '+128 本月新增', trendType: 'up', color: 'blue', icon: '📦' },
      { label: '当前库存总量', value: '152,680 件', trend: '覆盖 5 大品类', trendType: 'up', color: 'green', icon: '▦' },
      { label: '今日入库', value: '48 单', trend: '+12 较昨日', trendType: 'up', color: 'orange', icon: '⬇️' },
      { label: '今日出库', value: '35 单', trend: '-5 较昨日', trendType: 'down', color: 'purple', icon: '⬆️' },
    ],
  },
  materials: {
    title: '物料管理', description: '维护物料编码、规格、单位、品牌和安全库存。', primaryAction: '新增物料', hideStats: true, searchPlaceholder: '搜索SKU/物料名称...', filters: ['物料类型', '状态'], stats: commonStats('3,256', '3,012', '128', '16'),
    columns: [
      { prop: 'sku', label: 'SKU编码', width: 130 }, { prop: 'name', label: '物料名称', minWidth: 160 }, { prop: 'spec', label: '规格型号', width: 130 }, { prop: 'type', label: '物料类型', width: 110 }, { prop: 'brand', label: '品牌', width: 100 }, { prop: 'unit', label: '单位', width: 80, align: 'center' }, { prop: 'safeStock', label: '安全库存', width: 100, align: 'right' }, { prop: 'status', label: '状态', width: 90, align: 'center', type: 'tag' },
    ],
    rows: [
      { sku: 'MAT-EL-001', name: '工业控制主板', spec: 'IPC-MB-8G', type: '电子件', brand: '西门子', unit: '件', safeStock: 120, status: '启用' },
      { sku: 'MAT-HW-014', name: '不锈钢螺栓', spec: 'M8*35', type: '五金件', brand: '固特', unit: '盒', safeStock: 300, status: '启用' },
      { sku: 'MAT-RB-006', name: '密封圈', spec: 'Φ38 NBR', type: '橡胶件', brand: '耐驰', unit: '个', safeStock: 500, status: '启用' },
      { sku: 'MAT-FG-022', name: '成品控制柜', spec: 'QW-900', type: '成品', brand: '七维', unit: '台', safeStock: 20, status: '禁用' },
    ],
  },
  types: {
    title: '类型管理', description: '维护物料类型树和库存分类层级。', primaryAction: '新增类型', searchPlaceholder: '搜索类型...', filters: ['上级类型'], stats: commonStats('24', '21', '3', '0'),
    columns: [
      { prop: 'code', label: '类型编码', width: 130 }, { prop: 'name', label: '类型名称', minWidth: 160 }, { prop: 'parent', label: '上级类型', width: 130 }, { prop: 'level', label: '层级', width: 90, align: 'center' }, { prop: 'description', label: '类型描述', minWidth: 220 }, { prop: 'status', label: '状态', width: 90, align: 'center', type: 'tag' },
    ],
    rows: [
      { code: 'TYPE-RAW', name: '原材料', parent: '-', level: '一级', description: '生产所需基础原材料', status: '启用' },
      { code: 'TYPE-EL', name: '电子件', parent: '原材料', level: '二级', description: '主板、模块、线束等电子元件', status: '启用' },
      { code: 'TYPE-HW', name: '五金件', parent: '原材料', level: '二级', description: '螺栓、支架、钣金件', status: '启用' },
      { code: 'TYPE-FG', name: '成品', parent: '-', level: '一级', description: '已完成生产可销售产品', status: '启用' },
    ],
  },
  brands: {
    title: '品牌管理', description: '维护品牌档案、品牌来源和启用状态。', primaryAction: '新增品牌', searchPlaceholder: '搜索品牌编码/品牌名称...', filters: ['品牌来源', '品牌状态'], statColumns: 3, stats: [
      { label: '品牌总数', value: '36', footer: '全部品牌档案', color: 'blue', icon: '🔖' },
      { label: '国外品牌数', value: '18', footer: '进口/海外品牌', color: 'orange', icon: '🌍' },
      { label: '国内品牌数', value: '18', footer: '国产/本土品牌', color: 'green', icon: '🏭' },
    ],
    columns: [
      { prop: 'code', label: '品牌编码', width: 120 }, { prop: 'name', label: '品牌名称', width: 140 }, { prop: 'description', label: '品牌描述', minWidth: 240 }, { prop: 'origin', label: '品牌来源', width: 120, align: 'center', type: 'tag' }, { prop: 'country', label: '国家/地区', width: 120 }, { prop: 'createTime', label: '创建时间', width: 130 }, { prop: 'status', label: '状态', width: 90, align: 'center', type: 'tag' },
    ],
    rows: [
      { code: 'BR-001', name: '西门子', description: '工业自动化及控制类品牌', origin: '国外品牌', country: '德国', createTime: '2026-05-12', status: '启用' },
      { code: 'BR-002', name: '固特', description: '五金紧固件供应品牌', origin: '国内品牌', country: '中国', createTime: '2026-05-18', status: '启用' },
      { code: 'BR-003', name: '耐驰', description: '橡胶密封类品牌', origin: '国外品牌', country: '德国', createTime: '2026-05-20', status: '启用' },
      { code: 'BR-004', name: '安控', description: '停用测试品牌', origin: '国内品牌', country: '中国', createTime: '2026-05-25', status: '禁用' },
    ],
  },
  warehouses: {
    title: '仓库管理', description: '维护仓库信息、容量、负责人和启停状态。', primaryAction: '新增仓库', searchPlaceholder: '搜索仓库名称/编号...', filters: ['状态筛选'], stats: [
      { label: '仓库总数', value: '6', footer: '个仓库', color: 'blue', icon: '🏬' }, { label: '正常启用', value: '5', footer: '个仓库', color: 'green', icon: '✅' }, { label: '已停用', value: '1', footer: '个仓库', color: 'orange', icon: '⏸️' }, { label: '总容量(件)', value: '186,000', footer: '所有仓库容量', color: 'purple', icon: '📦' },
    ],
    columns: [
      { prop: 'code', label: '仓库编号', width: 120 }, { prop: 'name', label: '仓库名称', minWidth: 150 }, { prop: 'address', label: '仓库地址', minWidth: 220 }, { prop: 'type', label: '仓库类型', width: 120 }, { prop: 'capacity', label: '容量', width: 100, align: 'right' }, { prop: 'used', label: '已用', width: 100, align: 'right' }, { prop: 'manager', label: '负责人', width: 100 }, { prop: 'status', label: '状态', width: 100, align: 'center', type: 'tag' },
    ],
    rows: [
      { code: 'WH-001', name: '华东主仓', address: '上海市浦东新区航运路88号', type: '普通仓库', capacity: 80000, used: 52680, manager: '张三', status: '正常启用' },
      { code: 'WH-002', name: '恒温备件仓', address: '苏州市工业园区星湖街66号', type: '恒温仓库', capacity: 36000, used: 21800, manager: '李四', status: '正常启用' },
      { code: 'WH-003', name: '成品仓', address: '杭州市萧山区机场路18号', type: '成品仓库', capacity: 52000, used: 42100, manager: '王五', status: '正常启用' },
      { code: 'WH-004', name: '旧料仓', address: '南京市江宁区备用库区', type: '普通仓库', capacity: 18000, used: 3200, manager: '赵六', status: '已停用' },
    ],
  },
  locations: {
    title: '库位管理', description: '维护库区、货架、库位编码和使用状态。', primaryAction: '新增库位', searchPlaceholder: '搜索库位编码/名称...', filters: ['所属仓库', '库位状态'], stats: [
      { label: '库位总数', value: '6', footer: '个库位', color: 'blue', icon: '' },
      { label: '已占用', value: '4', footer: '个库位', color: 'green', icon: '' },
      { label: '空闲', value: '2', footer: '个库位', color: 'orange', icon: '' },
      { label: '使用率', value: '67%', footer: '库位利用率', color: 'purple', icon: '' },
    ],
    columns: [
      { prop: 'code', label: '库位编码', width: 130 }, { prop: 'name', label: '库位名称', minWidth: 140 }, { prop: 'warehouse', label: '所属仓库', width: 120 }, { prop: 'area', label: '库区', width: 100 }, { prop: 'shelf', label: '货架', width: 90 }, { prop: 'capacity', label: '容量', width: 90, align: 'right' }, { prop: 'usedRate', label: '使用率', width: 140, type: 'progress' }, { prop: 'status', label: '状态', width: 90, align: 'center', type: 'tag' },
    ],
    rows: [
      { code: 'A-01-01', name: 'A区一架一层', warehouse: '华东主仓', area: 'A区', shelf: '01架', capacity: 1200, usedRate: 76, status: '启用' },
      { code: 'A-01-02', name: 'A区一架二层', warehouse: '华东主仓', area: 'A区', shelf: '01架', capacity: 1200, usedRate: 48, status: '启用' },
      { code: 'B-03-04', name: 'B区三架四层', warehouse: '恒温备件仓', area: 'B区', shelf: '03架', capacity: 800, usedRate: 91, status: '启用' },
      { code: 'C-02-01', name: 'C区二架一层', warehouse: '旧料仓', area: 'C区', shelf: '02架', capacity: 500, usedRate: 0, status: '禁用' },
    ],
  },
  inventory: {
    title: '库存管理', description: '查看库存余额、批次、可用量和库存状态。', searchPlaceholder: '搜索SKU/物料名称...', filters: ['仓库筛选'], stats: [
      { label: '总物料数', value: '7', footer: '种物料', color: 'blue', icon: '' },
      { label: '库存总量', value: '735', footer: '件', color: 'green', icon: '' },
      { label: '库存金额', value: '¥22230.00', footer: '元', color: 'orange', icon: '' },
      { label: '预警物料', value: '3', footer: '件需关注', color: 'purple', icon: '' },
    ],
    columns: [
      { prop: 'sku', label: 'SKU编码', width: 130 }, { prop: 'name', label: '物料名称', minWidth: 150 }, { prop: 'spec', label: '规格型号', width: 120 }, { prop: 'warehouse', label: '仓库', width: 150 }, { prop: 'stockQty', label: '库存数量', width: 110, align: 'right' }, { prop: 'safeStock', label: '安全库存', width: 110, align: 'right' }, { prop: 'unitPrice', label: '单价(元)', width: 120, align: 'right' }, { prop: 'amount', label: '库存金额', width: 130, align: 'right', type: 'money' }, { prop: 'status', label: '状态', width: 100, align: 'center', type: 'tag' },
    ],
    rows: [
      { sku: 'SKU001', name: '主板', spec: 'X570', warehouse: '主仓库-A区', stockQty: 50, safeStock: 30, unitPrice: 80, amount: 4000, status: '正常' },
      { sku: 'SKU002', name: '电源', spec: '500W', warehouse: '主仓库-A区', stockQty: 60, safeStock: 40, unitPrice: 50, amount: 3000, status: '正常' },
      { sku: 'SKU003', name: '机箱', spec: 'ATX', warehouse: '主仓库-B区', stockQty: 40, safeStock: 30, unitPrice: 25, amount: 1000, status: '正常' },
      { sku: 'SKU006', name: '连接器', spec: 'USB-C', warehouse: '主仓库-B区', stockQty: 280, safeStock: 300, unitPrice: 8.5, amount: 2380, status: '库存不足' },
      { sku: 'SKU009', name: '电源模块', spec: '5V/2A', warehouse: '主仓库-B区', stockQty: 180, safeStock: 200, unitPrice: 15, amount: 2700, status: '库存不足' },
      { sku: 'SKU010', name: '成品整机', spec: '标准版', warehouse: '成品仓', stockQty: 45, safeStock: 50, unitPrice: 150, amount: 6750, status: '库存不足' },
      { sku: 'SKU015', name: '外壳', spec: '铝合金', warehouse: '成品仓', stockQty: 80, safeStock: 50, unitPrice: 30, amount: 2400, status: '正常' },
    ], actions: ['详情', '修改'],
  },
  inbound: {
    title: '入库管理', description: '处理采购、退货和调拨入库，跟踪质检与上架进度。', primaryAction: '新增入库单', searchPlaceholder: '搜索入库单号/供应商...', filters: ['状态筛选', '质检状态'], stats: commonStats('128', '86', '34', '8'),
    tabs: [
      { name: 'orders', label: '入库单', searchPlaceholder: '搜索入库单号/供应商...', filters: ['状态筛选'], columns: [
        { prop: 'code', label: '入库单号', width: 140 }, { prop: 'poCode', label: '关联采购单', width: 140 }, { prop: 'supplier', label: '供应商', width: 120 }, { prop: 'warehouse', label: '仓库', width: 110 }, { prop: 'materialCount', label: '物料种类', width: 90, align: 'right' }, { prop: 'progress', label: '入库进度', width: 140, type: 'progress' }, { prop: 'totalQty', label: '总数量', width: 90, align: 'right' }, { prop: 'totalAmount', label: '总金额', width: 120, align: 'right', type: 'money' }, { prop: 'inspectStatus', label: '质检状态', width: 100, align: 'center', type: 'tag' }, { prop: 'creator', label: '入库人', width: 100 }, { prop: 'status', label: '状态', width: 90, align: 'center', type: 'tag' },
      ], rows: [
        { code: 'IN20260528001', poCode: 'PO20260526001', supplier: '上海智造', warehouse: '华东主仓', materialCount: 5, progress: 100, totalQty: 1260, totalAmount: 86200, inspectStatus: '已审核', creator: '张三', status: '已入库' },
        { code: 'IN20260528002', poCode: 'PO20260527003', supplier: '苏州精工', warehouse: '恒温备件仓', materialCount: 3, progress: 65, totalQty: 840, totalAmount: 42800, inspectStatus: '待审核', creator: '李四', status: '部分入库' },
        { code: 'IN20260528003', poCode: 'PO20260528004', supplier: '杭州七维', warehouse: '成品仓', materialCount: 2, progress: 0, totalQty: 96, totalAmount: 156000, inspectStatus: '待审核', creator: '王五', status: '待入库' },
      ], actions: ['执行入库', '详情', '编辑'] },
      { name: 'details', label: '入库明细', searchPlaceholder: '搜索入库单号/SKU/物料名称...', filters: ['入库方式'], columns: [
        { prop: 'code', label: '入库单号', width: 140 }, { prop: 'sku', label: 'SKU编码', width: 130 }, { prop: 'name', label: '物料名称', minWidth: 150 }, { prop: 'spec', label: '规格型号', width: 120 }, { prop: 'method', label: '入库方式', width: 100 }, { prop: 'qty', label: '入库数量', width: 90, align: 'right' }, { prop: 'location', label: '目标库位', width: 100 }, { prop: 'status', label: '状态', width: 90, align: 'center', type: 'tag' },
      ], rows: [
        { code: 'IN20260528001', sku: 'MAT-EL-001', name: '工业控制主板', spec: 'IPC-MB-8G', method: '采购入库', qty: 300, location: 'A-01-01', status: '已入库' },
        { code: 'IN20260528002', sku: 'MAT-RB-006', name: '密封圈', spec: 'Φ38 NBR', method: '采购入库', qty: 500, location: 'B-03-04', status: '部分入库' },
      ], actions: ['查看SN', '详情'] },
    ], columns: [], rows: [],
  },
  outbound: {
    title: '出库管理', description: '处理销售、领用和调拨出库，跟踪拣货与发运进度。', primaryAction: '新增出库单', searchPlaceholder: '搜索出库单号/客户...', filters: ['状态筛选'], stats: commonStats('96', '68', '21', '7'),
    columns: [
      { prop: 'code', label: '出库单号', width: 140 }, { prop: 'bizCode', label: '关联单号', width: 140 }, { prop: 'customer', label: '客户/部门', minWidth: 140 }, { prop: 'warehouse', label: '仓库', width: 110 }, { prop: 'materialCount', label: '物料种类', width: 90, align: 'right' }, { prop: 'progress', label: '出库进度', width: 140, type: 'progress' }, { prop: 'totalQty', label: '总数量', width: 90, align: 'right' }, { prop: 'creator', label: '出库人', width: 100 }, { prop: 'createTime', label: '出库时间', width: 150 }, { prop: 'status', label: '状态', width: 90, align: 'center', type: 'tag' },
    ],
    rows: [
      { code: 'OUT20260528001', bizCode: 'SO20260526008', customer: '上海瑞科', warehouse: '成品仓', materialCount: 2, progress: 100, totalQty: 48, creator: '赵六', createTime: '2026-05-28 09:20', status: '已出库' },
      { code: 'OUT20260528002', bizCode: 'REQ20260527003', customer: '维修部', warehouse: '华东主仓', materialCount: 4, progress: 60, totalQty: 320, creator: '钱七', createTime: '2026-05-28 11:05', status: '部分出库' },
      { code: 'OUT20260528003', bizCode: 'SO20260528011', customer: '杭州云启', warehouse: '成品仓', materialCount: 1, progress: 0, totalQty: 20, creator: '孙八', createTime: '2026-05-28 14:30', status: '待出库' },
    ], actions: ['执行出库', '详情', '编辑'],
  },
  stocktaking: {
    title: '库存盘点', description: '发起盘点任务，记录差异并生成调整审核。', primaryAction: '新建盘点单', searchPlaceholder: '搜索盘点单号/仓库...', filters: ['状态筛选'], stats: [
      { label: '盘点单总数', value: '18', footer: '盘点记录总数', color: 'blue', icon: '🧾' }, { label: '已完成', value: '11', footer: '完成盘点', color: 'green', icon: '✅' }, { label: '进行中', value: '5', footer: '盘点中', color: 'orange', icon: '⏳' }, { label: '待审核', value: '2', footer: '差异审核', color: 'purple', icon: '⚠️' },
    ],
    columns: [
      { prop: 'code', label: '盘点单号', width: 140 }, { prop: 'warehouse', label: '盘点仓库', width: 120 }, { prop: 'scope', label: '盘点范围', minWidth: 160 }, { prop: 'planDate', label: '计划日期', width: 120 }, { prop: 'owner', label: '负责人', width: 100 }, { prop: 'items', label: '物料项', width: 90, align: 'right' }, { prop: 'diffs', label: '差异项', width: 90, align: 'right' }, { prop: 'reviewStatus', label: '审核状态', width: 100, align: 'center', type: 'tag' }, { prop: 'status', label: '状态', width: 90, align: 'center', type: 'tag' },
    ],
    rows: [
      { code: 'ST20260528001', warehouse: '华东主仓', scope: 'A区、B区全量盘点', planDate: '2026-05-28', owner: '张三', items: 320, diffs: 6, reviewStatus: '已审核', status: '已完成' },
      { code: 'ST20260528002', warehouse: '恒温备件仓', scope: 'B-03 货架', planDate: '2026-05-29', owner: '李四', items: 88, diffs: 2, reviewStatus: '待审核', status: '进行中' },
      { code: 'ST20260528003', warehouse: '成品仓', scope: '成品库位抽盘', planDate: '2026-05-30', owner: '王五', items: 56, diffs: 0, reviewStatus: '待审核', status: '待处理' },
    ], actions: ['盘点录入', '差异审核', '详情'],
  },
  warnings: {
    title: '库存预警', description: '监控安全库存、呆滞库存和临期风险。', primaryAction: '新增预警', searchPlaceholder: '搜索SKU/物料名称...', filters: ['预警级别', '处理状态'], stats: [
      { label: '预警总数', value: '42', footer: '预警物料总数', color: 'blue', icon: '⚠️' }, { label: '已处理', value: '18', footer: '处理完成', color: 'green', icon: '✅' }, { label: '待处理', value: '24', footer: '待处理', color: 'orange', icon: '⏳' }, { label: '紧急级别', value: '7个高危', footer: '需紧急处理', color: 'purple', icon: '🚨' },
    ],
    columns: [
      { prop: 'sku', label: 'SKU编码', width: 130 }, { prop: 'name', label: '物料名称', minWidth: 150 }, { prop: 'spec', label: '规格型号', width: 120 }, { prop: 'warehouse', label: '仓库', width: 120 }, { prop: 'currentStock', label: '当前库存', width: 100, align: 'right' }, { prop: 'threshold', label: '预警阈值', width: 100, align: 'right' }, { prop: 'level', label: '预警级别', width: 90, align: 'center', type: 'tag' }, { prop: 'warningTime', label: '预警时间', width: 150 }, { prop: 'status', label: '状态', width: 90, align: 'center', type: 'tag' },
    ],
    rows: [
      { sku: 'MAT-RB-006', name: '密封圈', spec: 'Φ38 NBR', warehouse: '恒温备件仓', currentStock: 0, threshold: 500, level: '高危', warningTime: '2026-05-28 08:30', status: '待处理' },
      { sku: 'MAT-HW-014', name: '不锈钢螺栓', spec: 'M8*35', warehouse: '华东主仓', currentStock: 180, threshold: 300, level: '中危', warningTime: '2026-05-28 09:12', status: '待处理' },
      { sku: 'MAT-EL-018', name: '通信模块', spec: 'RS485', warehouse: '华东主仓', currentStock: 92, threshold: 100, level: '低危', warningTime: '2026-05-27 16:40', status: '已处理' },
    ], actions: ['处理', '详情'],
  },
  purchaseDemands: {
    title: '采购需求', description: '汇总库存缺口，生成采购需求并跟踪下单状态。', primaryAction: '新增需求', searchPlaceholder: '搜索需求单号/SKU/物料名称...', filters: ['需求状态'], stats: commonStats('58', '23', '29', '6'),
    columns: [
      { prop: 'code', label: '需求单号', width: 140 }, { prop: 'sku', label: 'SKU编码', width: 130 }, { prop: 'name', label: '物料名称', minWidth: 150 }, { prop: 'warehouse', label: '需求仓库', width: 120 }, { prop: 'demandQty', label: '需求数量', width: 100, align: 'right' }, { prop: 'suggestQty', label: '建议采购', width: 100, align: 'right' }, { prop: 'reason', label: '需求原因', width: 120 }, { prop: 'requester', label: '申请人', width: 100 }, { prop: 'status', label: '状态', width: 90, align: 'center', type: 'tag' },
    ],
    rows: [
      { code: 'PD20260528001', sku: 'MAT-RB-006', name: '密封圈', warehouse: '恒温备件仓', demandQty: 800, suggestQty: 1000, reason: '安全库存预警', requester: '李四', status: '待下单' },
      { code: 'PD20260528002', sku: 'MAT-HW-014', name: '不锈钢螺栓', warehouse: '华东主仓', demandQty: 600, suggestQty: 800, reason: '低库存补货', requester: '张三', status: '已下单' },
      { code: 'PD20260528003', sku: 'MAT-EL-001', name: '工业控制主板', warehouse: '华东主仓', demandQty: 200, suggestQty: 300, reason: '生产备料', requester: '王五', status: '待审核' },
    ], actions: ['生成采购单', '详情', '编辑'],
  },
  purchaseOrders: {
    title: '采购订单', description: '维护采购订单、到货状态和入库进度。', primaryAction: '新增采购单', searchPlaceholder: '搜索采购单号/供应商...', filters: ['订单状态'], stats: commonStats('76', '41', '27', '8'),
    columns: [
      { prop: 'code', label: '采购单号', width: 140 }, { prop: 'supplier', label: '供应商', minWidth: 140 }, { prop: 'materialCount', label: '物料种类', width: 90, align: 'right' }, { prop: 'totalQty', label: '采购数量', width: 100, align: 'right' }, { prop: 'arrivalQty', label: '到货数量', width: 100, align: 'right' }, { prop: 'amount', label: '订单金额', width: 120, align: 'right', type: 'money' }, { prop: 'deliveryDate', label: '交期', width: 120 }, { prop: 'progress', label: '入库进度', width: 140, type: 'progress' }, { prop: 'status', label: '状态', width: 90, align: 'center', type: 'tag' },
    ],
    rows: [
      { code: 'PO20260526001', supplier: '上海智造', materialCount: 5, totalQty: 1260, arrivalQty: 1260, amount: 86200, deliveryDate: '2026-05-28', progress: 100, status: '已入库' },
      { code: 'PO20260527003', supplier: '苏州精工', materialCount: 3, totalQty: 840, arrivalQty: 520, amount: 42800, deliveryDate: '2026-05-30', progress: 65, status: '部分入库' },
      { code: 'PO20260528004', supplier: '杭州七维', materialCount: 2, totalQty: 96, arrivalQty: 0, amount: 156000, deliveryDate: '2026-06-02', progress: 0, status: '待入库' },
    ], actions: ['到货登记', '详情', '编辑'],
  },
}

const config = computed(() => configs[props.page] || configs.overview)
</script>
