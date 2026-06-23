export type AppMenu = {
  path: string
  title: string
  icon: string
  permission?: string
  badge?: string
  children?: AppMenu[]
}

export const menus: AppMenu[] = [
  { path: '/dashboard', title: '工作台', icon: '🏠' },
  {
    path: '/system',
    title: '系统管理',
    icon: '⚙️',
    permission: 'SYS',
    children: [
      { path: '/system/users', title: '用户管理', icon: '👤' },
      { path: '/system/orgs', title: '组织管理', icon: '🏢' },
      { path: '/system/roles', title: '角色权限', icon: '🔒' },
      { path: '/system/audits', title: '审计日志', icon: '📋' },
      { path: '/system/settings', title: '系统设置', icon: '⚙️' },
    ],
  },
  {
    path: '/crm',
    title: 'CRM客户管理',
    icon: '👥',
    permission: 'CRM',
    children: [
      { path: '/crm/workspace', title: 'CRM工作台', icon: '🖥️' },
      { path: '/crm/customers', title: '客户管理', icon: '👤' },
      { path: '/crm/public-customers', title: '公海客户', icon: '🌊' },
      { path: '/crm/contacts', title: '联系人管理', icon: '👥' },
      { path: '/crm/deals', title: '商机管理', icon: '📊' },
      { path: '/crm/quote-search', title: '报价查询', icon: '🔍' },
      { path: '/crm/orders', title: '订单管理', icon: '🛒' },
    ],
  },
  {
    path: '/maint',
    title: '维保系统',
    icon: '🔧',
    permission: 'MAINT',
    children: [
      { path: '/maint/workspace', title: '维保工作台', icon: '🖥️' },
      { path: '/maint/equipments', title: '设备台账', icon: '🏭' },
      { path: '/maint/work-orders', title: '工单管理', icon: '🧰' },
      { path: '/maint/plans', title: '保养计划', icon: '📅' },
      { path: '/maint/spare-parts', title: '备件管理', icon: '🔩' },
    ],
  },
  {
    path: '/ets',
    title: '电测系统',
    icon: '⚡',
    permission: 'ETS',
    children: [
      { path: '/ets/workspace', title: '电测工作台', icon: '🖥️' },
      { path: '/ets/registers', title: '来货登记', icon: '🧾' },
      { path: '/ets/tests', title: '测试执行', icon: '🧪' },
      { path: '/ets/reports', title: '测试报告', icon: '📄' },
    ],
  },
  {
    path: '/fin',
    title: '财务系统',
    icon: '💰',
    permission: 'FIN',
    children: [
      { path: '/fin/workspace', title: '财务工作台', icon: '🖥️' },
      { path: '/fin/receivables', title: '应收管理', icon: '📥' },
      { path: '/fin/payables', title: '应付管理', icon: '📤' },
      { path: '/fin/expenses', title: '费用管理', icon: '🧾' },
      { path: '/fin/budgets', title: '预算管理', icon: '📊' },
    ],
  },  {
    path: '/wms',
    title: 'WMS仓储管理',
    icon: '📦',
    permission: 'WMS',
    children: [
      { path: '/wms/overview', title: '仓储总览', icon: '📈' },
      { path: '/wms/materials', title: '物料管理', icon: '📦' },
      { path: '/wms/types', title: '类型管理', icon: '🏷️' },
      { path: '/wms/brands', title: '品牌管理', icon: '🔖' },
      { path: '/wms/warehouses', title: '仓库管理', icon: '🏬' },
      { path: '/wms/locations', title: '库位管理', icon: '📍' },
      { path: '/wms/inventory', title: '库存管理', icon: '▦' },
      { path: '/wms/inbound', title: '入库管理', icon: '⬇️' },
      { path: '/wms/outbound', title: '出库管理', icon: '⬆️' },
      { path: '/wms/stocktaking', title: '库存盘点', icon: '🧾' },
      { path: '/wms/warnings', title: '库存预警', icon: '⚠️' },
      { path: '/wms/purchase-demands', title: '采购需求', icon: '📝' },
      { path: '/wms/purchase-orders', title: '采购订单', icon: '🛍️' },
    ],
  },
]