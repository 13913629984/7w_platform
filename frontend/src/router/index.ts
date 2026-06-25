import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import AppLayout from '@/layouts/AppLayout.vue'

const routes: RouteRecordRaw[] = [
  { path: '/login', component: () => import('@/views/login/LoginView.vue') },
  {
    path: '/',
    component: AppLayout,
    redirect: '/dashboard',
    children: [
      { path: 'dashboard', component: () => import('@/views/dashboard/DashboardView.vue'), meta: { title: '工作台' } },
      { path: 'system/users', component: () => import('@/views/system/UsersView.vue'), meta: { title: '用户管理', service: 'sys' } },
      { path: 'system/orgs', component: () => import('@/views/system/OrgsView.vue'), meta: { title: '组织管理', service: 'sys' } },
      { path: 'system/roles', component: () => import('@/views/system/RolesView.vue'), meta: { title: '角色权限', service: 'sys' } },
      { path: 'system/audits', component: () => import('@/views/system/AuditsView.vue'), meta: { title: '审计日志', service: 'sys' } },
      { path: 'system/settings', component: () => import('@/views/system/SettingsView.vue'), meta: { title: '系统设置', service: 'sys' } },
      { path: 'crm/workspace', component: () => import('@/views/crm/WorkspaceView.vue'), meta: { title: 'CRM工作台', service: 'crm' } },
      { path: 'crm/customers', component: () => import('@/views/crm/CustomersView.vue'), meta: { title: '客户管理', service: 'crm' } },
      { path: 'crm/public-customers', component: () => import('@/views/crm/PublicCustomersView.vue'), meta: { title: '公海客户', service: 'crm' } },
      { path: 'crm/leads', component: () => import('@/views/crm/LeadsView.vue'), meta: { title: '线索管理', service: 'crm' } },
      { path: 'crm/contacts', component: () => import('@/views/crm/ContactsView.vue'), meta: { title: '联系人管理', service: 'crm' } },
      { path: 'crm/deals', component: () => import('@/views/crm/DealsView.vue'), meta: { title: '商机管理', service: 'crm' } },
      { path: 'crm/quotes', component: () => import('@/views/crm/QuotesView.vue'), meta: { title: '报价管理', service: 'crm' } },
      { path: 'crm/quote-search', component: () => import('@/views/crm/QuoteSearchView.vue'), meta: { title: '报价查询', service: 'crm' } },
      { path: 'crm/orders', component: () => import('@/views/crm/OrdersView.vue'), meta: { title: '订单管理', service: 'crm' } },
      { path: 'crm/activities', component: () => import('@/views/crm/ActivitiesView.vue'), meta: { title: '跟进活动', service: 'crm' } },
      { path: 'maint/workspace', component: () => import('@/views/maint/WorkspaceView.vue'), meta: { title: '维保工作台', service: 'maint' } },
      { path: 'maint/equipments', component: () => import('@/views/maint/EquipmentsView.vue'), meta: { title: '设备台账', service: 'maint' } },
      { path: 'maint/work-orders', component: () => import('@/views/maint/WorkOrdersView.vue'), meta: { title: '工单管理', service: 'maint' } },
      { path: 'maint/plans', component: () => import('@/views/maint/PlansView.vue'), meta: { title: '保养计划', service: 'maint' } },
      { path: 'maint/spare-parts', component: () => import('@/views/maint/SparePartsView.vue'), meta: { title: '备件管理', service: 'maint' } },
      { path: 'ets/workspace', component: () => import('@/views/ets/WorkspaceView.vue'), meta: { title: '电测工作台', service: 'ets' } },
      { path: 'ets/registers', component: () => import('@/views/ets/RegistersView.vue'), meta: { title: '来货登记', service: 'ets' } },
      { path: 'ets/tests', component: () => import('@/views/ets/TestsView.vue'), meta: { title: '测试执行', service: 'ets' } },
      { path: 'ets/reports', component: () => import('@/views/ets/ReportsView.vue'), meta: { title: '测试报告', service: 'ets' } },
      { path: 'fin/workspace', component: () => import('@/views/fin/WorkspaceView.vue'), meta: { title: '财务工作台', service: 'fin' } },
      { path: 'fin/ar-ap', component: () => import('@/views/fin/ArApView.vue'), meta: { title: '应收应付', service: 'fin' } },
      { path: 'fin/receivables', component: () => import('@/views/fin/ReceivablesView.vue'), meta: { title: '应收款管理', service: 'fin' } },
      { path: 'fin/payables', component: () => import('@/views/fin/PayablesView.vue'), meta: { title: '应付款管理', service: 'fin' } },
      { path: 'fin/payments', component: () => import('@/views/fin/PaymentsView.vue'), meta: { title: '付款管理', service: 'fin' } },
      { path: 'fin/cost-collection', component: () => import('@/views/fin/CostCollectionView.vue'), meta: { title: '成本归集', service: 'fin' } },
      { path: 'fin/business-flow', component: () => import('@/views/fin/BusinessFlowView.vue'), meta: { title: '业务流程', service: 'fin' } },
      { path: 'fin/expenses', component: () => import('@/views/fin/ExpensesView.vue'), meta: { title: '费用管理', service: 'fin' } },
      { path: 'fin/budgets', component: () => import('@/views/fin/BudgetsView.vue'), meta: { title: '预算管理', service: 'fin' } },
      { path: 'wms/overview', component: () => import('@/views/wms/OverviewView.vue'), meta: { title: '仓储总览', service: 'wms' } },
      { path: 'wms/materials', component: () => import('@/views/wms/MaterialsView.vue'), meta: { title: '物料管理', service: 'wms' } },
      { path: 'wms/types', component: () => import('@/views/wms/TypesView.vue'), meta: { title: '类型管理', service: 'wms' } },
      { path: 'wms/brands', component: () => import('@/views/wms/BrandsView.vue'), meta: { title: '品牌管理', service: 'wms' } },
      { path: 'wms/suppliers', component: () => import('@/views/wms/SuppliersView.vue'), meta: { title: '供应商管理', service: 'wms' } },
      { path: 'wms/warehouses', component: () => import('@/views/wms/WarehousesView.vue'), meta: { title: '仓库管理', service: 'wms' } },
      { path: 'wms/locations', component: () => import('@/views/wms/LocationsView.vue'), meta: { title: '库位管理', service: 'wms' } },
      { path: 'wms/inventory', component: () => import('@/views/wms/InventoryView.vue'), meta: { title: '库存管理', service: 'wms' } },
      { path: 'wms/inbound', component: () => import('@/views/wms/InboundView.vue'), meta: { title: '入库管理', service: 'wms' } },
      { path: 'wms/outbound', component: () => import('@/views/wms/OutboundView.vue'), meta: { title: '出库管理', service: 'wms' } },
      { path: 'wms/stocktaking', component: () => import('@/views/wms/StocktakingView.vue'), meta: { title: '库存盘点', service: 'wms' } },
      { path: 'wms/warnings', component: () => import('@/views/wms/WarningsView.vue'), meta: { title: '库存预警', service: 'wms' } },
      { path: 'wms/purchase-demands', component: () => import('@/views/wms/PurchaseDemandsView.vue'), meta: { title: '采购需求', service: 'wms' } },
      { path: 'wms/purchase-orders', component: () => import('@/views/wms/PurchaseOrdersView.vue'), meta: { title: '采购订单', service: 'wms' } },
    ],
  },
]

const router = createRouter({ history: createWebHistory(), routes })

router.beforeEach(async (to) => {
  const auth = useAuthStore()
  if (to.path !== '/login' && !auth.token) return '/login'

  if (auth.token && !auth.profileLoaded) {
    try {
      await auth.fetchProfile()
    } catch {
      auth.logout()
      return '/login'
    }
  }

  const servicePermission = typeof to.meta.service === 'string' ? to.meta.service.toUpperCase() : ''
  if (to.path !== '/login' && servicePermission && !auth.hasPermission(servicePermission)) return '/dashboard'

  if (to.path === '/login' && auth.token) return '/dashboard'
})

export default router
