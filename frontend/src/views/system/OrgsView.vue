<template>
  <section>
    <div class="sub-page-header">
      <div>
        <h2>组织架构管理</h2>
        <p class="page-desc">维护公司部门树、岗位人员和组织基础数据。</p>
      </div>
      <el-space>
        <el-button size="small" type="primary" @click="openEmployeeDialog()">新增员工</el-button>
      </el-space>
    </div>

    <div class="org-page-layout">
      <div class="org-tree-card">
        <div class="card-header">
          <div class="card-title">组织结构</div>
          <el-space size="small">
            <el-button size="small" link type="primary" @click="openDepartmentDialog(null, null)">新增根节点</el-button>
            <el-button size="small" link type="primary" @click="expandAll">展开全部</el-button>
          </el-space>
        </div>
        <el-tree
          ref="treeRef"
          class="org-tree"
          :data="departmentTree"
          :props="treeProps"
          node-key="id"
          default-expand-all
          highlight-current
          :current-node-key="showAllEmployees ? undefined : selectedDepartment?.id"
          @node-click="selectDepartment"
        >
          <template #default="{ node, data }">
            <span class="org-tree-node">
              <span>{{ node.label }}</span>
              <span class="org-tree-actions">
                <el-button size="small" link type="primary" @click.stop="openDepartmentDialog(null, data)">新增</el-button>
                <el-button size="small" link @click.stop="openDepartmentDialog(data)">编辑</el-button>
                <el-button size="small" link type="danger" @click.stop="deleteDepartment(data)">删除</el-button>
              </span>
            </span>
          </template>
        </el-tree>
      </div>

      <div class="org-main">
        <div class="stats-row org-stats-row">
          <div class="stat-card blue">
            <div class="stat-header"><span class="stat-title">部门数量</span></div>
            <div class="stat-value">{{ stats.departmentCount }}</div>
            <div class="stat-footer"><span>含 {{ stats.companyCount }} 个子公司</span></div>
          </div>
          <div class="stat-card green">
            <div class="stat-header"><span class="stat-title">员工总数</span></div>
            <div class="stat-value">{{ stats.employeeCount }}</div>
            <div class="stat-footer"><span>正式员工 {{ stats.activeEmployeeCount }} 人</span></div>
          </div>
          <div class="stat-card orange">
            <div class="stat-header"><span class="stat-title">本月入职</span></div>
            <div class="stat-value">{{ stats.newThisMonth }}</div>
            <div class="stat-footer"><span>离职 {{ stats.leaveThisMonth }} 人</span></div>
          </div>
          <div class="stat-card purple">
            <div class="stat-header"><span class="stat-title">岗位数量</span></div>
            <div class="stat-value">{{ stats.positionCount }}</div>
            <div class="stat-footer"><span>含 {{ stats.managerPositionCount }} 个管理岗</span></div>
          </div>
        </div>

        <div v-if="selectedDepartment && !showAllEmployees" class="org-detail-card">
          <div class="card-header">
            <div class="card-title">{{ selectedDepartment.name }} - 部门详情</div>
            <el-space>
              <el-button size="small" @click="openDepartmentDialog(null, selectedDepartment)">新增下级</el-button>
              <el-button size="small" @click="openDepartmentDialog(selectedDepartment)">编辑部门</el-button>
            </el-space>
          </div>
          <div class="org-detail-grid">
            <div>
              <div class="org-meta-label">部门编号</div>
              <div class="org-meta-value">{{ selectedDepartment.code }}</div>
            </div>
            <div>
              <div class="org-meta-label">上级部门</div>
              <div class="org-meta-value">{{ selectedDepartment.parentName || '-' }}</div>
            </div>
            <div>
              <div class="org-meta-label">部门人数</div>
              <div class="org-meta-value">{{ selectedDepartment.employeeCount }} 人</div>
            </div>
            <div>
              <div class="org-meta-label">负责人</div>
              <div class="org-meta-value">{{ selectedDepartment.leader || '-' }}</div>
            </div>
          </div>
        </div>

        <div class="table-card org-employee-card">
          <div class="filter-bar">
            <div class="card-title">{{ showAllEmployees ? '全部员工' : '部门员工' }}</div>
            <el-space>
              <el-input
                v-model="keyword"
                style="width: 220px"
                clearable
                placeholder="搜索员工姓名"
                prefix-icon="Search"
                @input="searchEmployees"
              />
              <el-button :type="showAllEmployees ? 'primary' : 'default'" @click="showAllEmployeeList">全部员工</el-button>
              <el-button v-if="showAllEmployees && selectedDepartment" @click="showDepartmentEmployeeList">当前部门</el-button>
            </el-space>
          </div>

          <el-table :data="employees" stripe v-loading="loading">
            <el-table-column prop="employeeNo" label="工号" width="130" />
            <el-table-column prop="name" label="姓名" width="100" />
            <el-table-column prop="position" label="岗位" width="100">
              <template #default="scope">
                <el-tag :type="positionTagType(scope.row.position)" effect="light">{{ scope.row.position }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="departmentName" label="部门" width="110" />
            <el-table-column prop="email" label="邮箱" min-width="180" />
            <el-table-column prop="phone" label="电话" width="130" />
            <el-table-column prop="status" label="状态" width="90">
              <template #default="scope">
                <el-tag :type="statusTagType(scope.row.status)" effect="light">{{ scope.row.status }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="140" fixed="right">
              <template #default="scope">
                <el-button link type="primary" @click="openEmployeeDialog(scope.row)">编辑</el-button>
                <el-button link type="danger" @click="deleteEmployee(scope.row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>

          <div class="org-pagination">
            <el-pagination
              v-model:current-page="page"
              v-model:page-size="pageSize"
              :page-sizes="[5, 10, 20, 50]"
              :total="employeeTotal"
              layout="total, sizes, prev, pager, next, jumper"
              @size-change="handlePageSizeChange"
              @current-change="loadOverview"
            />
          </div>
        </div>
      </div>
    </div>

    <el-dialog v-model="departmentDialogVisible" :title="departmentEditing ? '编辑部门' : '新增部门'" width="460px">
      <el-form :model="departmentForm" label-width="90px">
        <el-form-item label="部门名称">
          <el-input v-model="departmentForm.name" placeholder="请输入部门名称" />
        </el-form-item>
        <el-form-item label="部门编号">
          <el-input v-model="departmentForm.code" placeholder="留空自动生成" />
        </el-form-item>
        <el-form-item label="上级部门">
          <el-tree-select
            v-model="departmentForm.parentId"
            :data="departmentTree"
            :props="treeProps"
            node-key="id"
            check-strictly
            clearable
            placeholder="请选择上级部门"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="负责人">
          <el-input v-model="departmentForm.leader" placeholder="请输入负责人" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="departmentForm.sort" :min="0" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="departmentDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveDepartment">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="employeeDialogVisible" :title="employeeEditing ? '编辑员工' : '新增员工'" width="520px">
      <el-form :model="employeeForm" label-width="90px">
        <el-form-item label="工号">
          <el-input v-model="employeeForm.employeeNo" placeholder="留空自动生成" />
        </el-form-item>
        <el-form-item label="姓名">
          <el-input v-model="employeeForm.name" placeholder="请输入姓名" />
        </el-form-item>
        <el-form-item label="所属部门">
          <el-tree-select
            v-model="employeeForm.departmentId"
            :data="departmentTree"
            :props="treeProps"
            node-key="id"
            check-strictly
            placeholder="请选择部门"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="岗位">
          <el-select v-model="employeeForm.position" filterable allow-create placeholder="请选择岗位" style="width: 100%">
            <el-option label="部门经理" value="部门经理" />
            <el-option label="主管" value="主管" />
            <el-option label="专员" value="专员" />
          </el-select>
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="employeeForm.phone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="employeeForm.email" placeholder="请输入邮箱" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="employeeForm.status" style="width: 100%">
            <el-option label="在职" value="在职" />
            <el-option label="试用期" value="试用期" />
            <el-option label="离职" value="离职" />
          </el-select>
        </el-form-item>
        <el-form-item label="入职日期">
          <el-date-picker v-model="employeeForm.entryDate" value-format="YYYY-MM-DD" type="date" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="employeeDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveEmployee">确定</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { TreeInstance } from 'element-plus'
import request from '@/api/request'

interface Department {
  id: number
  parentId: number | null
  code: string
  name: string
  leader: string
  sort: number
  status: number
  employeeCount: number
  parentName: string
  children: Department[]
}

interface Employee {
  id: number
  employeeNo: string
  name: string
  position: string
  departmentId: number
  departmentName: string
  email: string
  phone: string
  status: string
  entryDate?: string
  leaveDate?: string
}

interface OrgStats {
  departmentCount: number
  companyCount: number
  employeeCount: number
  activeEmployeeCount: number
  newThisMonth: number
  leaveThisMonth: number
  positionCount: number
  managerPositionCount: number
}

const loading = ref(false)
const keyword = ref('')
const showAllEmployees = ref(false)
const page = ref(1)
const pageSize = ref(10)
const employeeTotal = ref(0)
const treeRef = ref<TreeInstance>()
const departmentTree = ref<Department[]>([])
const employees = ref<Employee[]>([])
const selectedDepartment = ref<Department | null>(null)
const stats = ref<OrgStats>({
  departmentCount: 0,
  companyCount: 0,
  employeeCount: 0,
  activeEmployeeCount: 0,
  newThisMonth: 0,
  leaveThisMonth: 0,
  positionCount: 0,
  managerPositionCount: 0,
})

const departmentDialogVisible = ref(false)
const departmentEditing = ref(false)
const departmentForm = ref({
  id: null as number | null,
  parentId: null as number | null,
  code: '',
  name: '',
  leader: '',
  sort: 0,
  status: 1,
})

const employeeDialogVisible = ref(false)
const employeeEditing = ref(false)
const employeeForm = ref({
  id: null as number | null,
  employeeNo: '',
  name: '',
  position: '专员',
  departmentId: null as number | null,
  email: '',
  phone: '',
  status: '在职',
  entryDate: '',
})

const treeProps = {
  children: 'children',
  label: 'name',
}

const flatDepartments = computed(() => flattenDepartments(departmentTree.value))

async function loadOverview() {
  loading.value = true
  try {
    const { data } = await request.get('/system/org/overview', {
      params: {
        departmentId: showAllEmployees.value ? undefined : selectedDepartment.value?.id,
        keyword: keyword.value,
        page: page.value,
        pageSize: pageSize.value,
      },
    })
    if (data.code === 0) {
      departmentTree.value = data.data.tree || []
      employees.value = data.data.employees || []
      employeeTotal.value = data.data.employeeTotal || 0
      stats.value = { ...stats.value, ...data.data.stats }
      syncSelectedDepartment()
      await nextTick()
      if (!showAllEmployees.value && selectedDepartment.value) {
        treeRef.value?.setCurrentKey(selectedDepartment.value.id)
      }
    } else {
      ElMessage.error(data.message || '加载组织数据失败')
    }
  } finally {
    loading.value = false
  }
}

function syncSelectedDepartment() {
  const departments = flattenDepartments(departmentTree.value)
  if (!selectedDepartment.value) {
    selectedDepartment.value = departments[0] || null
    return
  }
  selectedDepartment.value = departments.find((dept) => dept.id === selectedDepartment.value?.id) || departments[0] || null
}

async function selectDepartment(department: Department) {
  selectedDepartment.value = department
  showAllEmployees.value = false
  page.value = 1
  await loadOverview()
}

async function showAllEmployeeList() {
  showAllEmployees.value = true
  page.value = 1
  await loadOverview()
}

async function showDepartmentEmployeeList() {
  showAllEmployees.value = false
  page.value = 1
  await loadOverview()
}

async function searchEmployees() {
  page.value = 1
  await loadOverview()
}

async function handlePageSizeChange() {
  page.value = 1
  await loadOverview()
}

function expandAll() {
  flatDepartments.value.forEach((department) => {
    const node = treeRef.value?.getNode(department.id)
    if (node) node.expanded = true
  })
}

function openDepartmentDialog(department?: Department | null, parentDepartment?: Department | null) {
  departmentEditing.value = Boolean(department)
  departmentForm.value = department
    ? {
        id: department.id,
        parentId: department.parentId,
        code: department.code,
        name: department.name,
        leader: department.leader,
        sort: department.sort || 0,
        status: department.status ?? 1,
      }
    : {
        id: null,
        parentId: parentDepartment?.id ?? null,
        code: '',
        name: '',
        leader: '',
        sort: 0,
        status: 1,
      }
  departmentDialogVisible.value = true
}

async function saveDepartment() {
  if (!departmentForm.value.name) {
    ElMessage.warning('请输入部门名称')
    return
  }
  if (departmentForm.value.id && departmentForm.value.id === departmentForm.value.parentId) {
    ElMessage.warning('上级部门不能选择自身')
    return
  }
  const url = departmentEditing.value ? '/system/org/department/update' : '/system/org/department/create'
  const { data } = await request.post(url, departmentForm.value)
  if (data.code === 0) {
    ElMessage.success(departmentEditing.value ? '部门已更新' : '部门已创建')
    departmentDialogVisible.value = false
    if (!departmentEditing.value && data.data?.id) {
      selectedDepartment.value = data.data
      showAllEmployees.value = false
    }
    page.value = 1
    await loadOverview()
  } else {
    ElMessage.error(data.message || '保存失败')
  }
}

async function deleteDepartment(department: Department) {
  try {
    await ElMessageBox.confirm(`确认删除部门“${department.name}”吗？`, '提示')
    const { data } = await request.post('/system/org/department/delete', { id: department.id })
    if (data.code === 0) {
      ElMessage.success('部门已删除')
      selectedDepartment.value = null
      page.value = 1
      await loadOverview()
    } else {
      ElMessage.error(data.message || '删除失败')
    }
  } catch (error) {
    if (error !== 'cancel') ElMessage.error('操作失败')
  }
}

function openEmployeeDialog(employee?: Employee) {
  employeeEditing.value = Boolean(employee)
  employeeForm.value = employee
    ? {
        id: employee.id,
        employeeNo: employee.employeeNo,
        name: employee.name,
        position: employee.position,
        departmentId: employee.departmentId,
        email: employee.email,
        phone: employee.phone,
        status: employee.status,
        entryDate: employee.entryDate || '',
      }
    : {
        id: null,
        employeeNo: '',
        name: '',
        position: '专员',
        departmentId: selectedDepartment.value?.id || flatDepartments.value[0]?.id || null,
        email: '',
        phone: '',
        status: '在职',
        entryDate: new Date().toISOString().slice(0, 10),
      }
  employeeDialogVisible.value = true
}

async function saveEmployee() {
  if (!employeeForm.value.name || !employeeForm.value.departmentId) {
    ElMessage.warning('请输入姓名并选择部门')
    return
  }
  const url = employeeEditing.value ? '/system/org/employee/update' : '/system/org/employee/create'
  const { data } = await request.post(url, employeeForm.value)
  if (data.code === 0) {
    ElMessage.success(employeeEditing.value ? '员工已更新' : '员工已创建')
    employeeDialogVisible.value = false
    await loadOverview()
  } else {
    ElMessage.error(data.message || '保存失败')
  }
}

async function deleteEmployee(employee: Employee) {
  try {
    await ElMessageBox.confirm(`确认删除员工“${employee.name}”吗？`, '提示')
    const { data } = await request.post('/system/org/employee/delete', { id: employee.id })
    if (data.code === 0) {
      ElMessage.success('员工已删除')
      if (employees.value.length === 1 && page.value > 1) {
        page.value -= 1
      }
      await loadOverview()
    } else {
      ElMessage.error(data.message || '删除失败')
    }
  } catch (error) {
    if (error !== 'cancel') ElMessage.error('操作失败')
  }
}

function exportEmployees() {
  const header = ['工号', '姓名', '岗位', '部门', '邮箱', '电话', '状态']
  const rows = employees.value.map((employee) => [
    employee.employeeNo,
    employee.name,
    employee.position,
    employee.departmentName,
    employee.email,
    employee.phone,
    employee.status,
  ])
  const csv = [header, ...rows].map((row) => row.join(',')).join('\n')
  const blob = new Blob([`\ufeff${csv}`], { type: 'text/csv;charset=utf-8;' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = showAllEmployees.value ? '全部员工.csv' : '部门员工.csv'
  link.click()
  URL.revokeObjectURL(url)
}

function flattenDepartments(departments: Department[]): Department[] {
  return departments.flatMap((department) => [department, ...flattenDepartments(department.children || [])])
}

function positionTagType(position: string) {
  if (position?.includes('经理')) return 'warning'
  if (position?.includes('主管')) return 'primary'
  return 'info'
}

function statusTagType(status: string) {
  if (status === '在职') return 'success'
  if (status === '试用期') return 'warning'
  return 'info'
}

onMounted(() => {
  loadOverview()
})
</script>