<template>
  <section>
    <div class="sub-page-header">
      <div>
        <h2>&#29992;&#25143;&#31649;&#29702;</h2>
        <p class="page-desc">&#32500;&#25252;&#29992;&#25143;&#36134;&#21495;&#12289;&#23703;&#20301;&#12289;&#29366;&#24577;&#19982;&#32452;&#32455;&#20851;&#31995;&#12290;</p>
      </div>
      <div>
        <el-button size="small" plain>&#23548;&#20986;</el-button>
        <el-button size="small" plain @click="loadUsers">&#21047;&#26032;</el-button>
      </div>
    </div>

    <div class="stats-row user-mgmt-stats">
      <div class="stat-card blue">
        <div class="stat-body">
          <div class="stat-info">
            <div class="stat-header"><span class="stat-title">&#29992;&#25143;&#24635;&#25968;</span></div>
            <div class="stat-value">{{ stats.total }}</div>
            <div class="stat-footer"><span>&#22312;&#29992; {{ stats.active }} / &#31105;&#29992; {{ stats.disabled }}</span></div>
          </div>
          <div class="stat-icon">&#9782;</div>
        </div>
      </div>
      <div class="stat-card green">
        <div class="stat-body">
          <div class="stat-info">
            <div class="stat-header"><span class="stat-title">&#22312;&#29992;&#25968;&#37327;</span></div>
            <div class="stat-value">{{ stats.active }}</div>
            <div class="stat-footer"><span>&#21551;&#29992;&#29366;&#24577;&#30340;&#29992;&#25143;</span></div>
          </div>
          <div class="stat-icon">&#10003;</div>
        </div>
      </div>
      <div class="stat-card red">
        <div class="stat-body">
          <div class="stat-info">
            <div class="stat-header"><span class="stat-title">&#31105;&#29992;&#25968;&#37327;</span></div>
            <div class="stat-value">{{ stats.disabled }}</div>
            <div class="stat-footer"><span>&#20572;&#29992;&#29366;&#24577;&#30340;&#29992;&#25143;</span></div>
          </div>
          <div class="stat-icon">&#10005;</div>
        </div>
      </div>
    </div>

    <div class="table-card">
      <div class="filter-bar">
        <div class="filter-left">
          <el-input v-model="keyword" style="width: 240px" clearable placeholder="&#25628;&#32034;&#22995;&#21517;/&#24037;&#21495;/&#25163;&#26426;" prefix-icon="Search" @input="loadUsers" />
          <el-select v-model="statusFilter" style="width: 140px" placeholder="&#29366;&#24577;" @change="loadUsers">
            <el-option label="&#20840;&#37096;" value="" />
            <el-option label="&#21551;&#29992;" value="&#21551;&#29992;" />
            <el-option label="&#20572;&#29992;" value="&#20572;&#29992;" />
          </el-select>
          <el-select v-model="departmentFilter" style="width: 140px" placeholder="&#37096;&#38376;" @change="loadUsers">
            <el-option label="&#20840;&#37096;&#37096;&#38376;" value="" />
            <el-option v-for="department in departmentOptions" :key="department" :label="department" :value="department" />
          </el-select>
        </div>
        <el-space>
          <el-button @click="showImportDialog">&#23548;&#20837;</el-button>
          <el-button>&#23548;&#20986;</el-button>
          <el-button type="primary" icon="Plus" @click="showAddDialog">&#26032;&#22686;&#29992;&#25143;</el-button>
        </el-space>
      </div>

      <el-table :data="filteredUsers" stripe style="width: 100%">
        <el-table-column prop="employeeId" label="&#24037;&#21495;" min-width="120" />
        <el-table-column prop="nickname" label="&#22995;&#21517;" min-width="160">
          <template #default="scope">
            <div style="display: flex; align-items: center; gap: 8px;">
              <div class="avatar" style="width: 28px; height: 28px; font-size: 11px; background: #409eff; border-radius: 50%; color: #fff; display: flex; align-items: center; justify-content: center; flex-shrink: 0;">{{ scope.row.nickname.charAt(0) }}</div>
              <span style="white-space: nowrap; overflow: hidden; text-overflow: ellipsis;">{{ scope.row.nickname }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="department" label="&#37096;&#38376;" min-width="120" />
        <el-table-column prop="position" label="&#23703;&#20301;" min-width="120" />
        <el-table-column prop="role" label="&#35282;&#33394;" min-width="120" />
        <el-table-column prop="email" label="&#37038;&#31665;" min-width="200" />
        <el-table-column prop="lastLogin" label="&#26368;&#21516;&#30331;&#24405;" min-width="170" />
        <el-table-column prop="status" label="&#29366;&#24577;" width="100" align="center">
          <template #default="scope">
            <el-tag :type="scope.row.status === enabledText ? 'success' : 'info'" size="small">{{ scope.row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="&#25805;&#20316;" width="240" fixed="right" align="center">
          <template #default="scope">
            <el-button link type="primary" @click="editUser(scope.row)">&#32534;&#36753;</el-button>
            <el-button link type="primary" @click="openRoleDialog(scope.row)">&#25480;&#26435;</el-button>
            <el-button v-if="scope.row.status === enabledText" link type="danger" @click="toggleUserStatus(scope.row)">&#31105;&#29992;</el-button>
            <el-button v-else link type="success" @click="toggleUserStatus(scope.row)">&#21551;&#29992;</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrapper">
        <el-pagination v-model:current-page="page" v-model:page-size="pageSize" :page-sizes="[10, 20, 50, 100]" :total="total" layout="total, sizes, prev, pager, next, jumper" @size-change="loadUsers" @current-change="loadUsers" />
      </div>
    </div>

    <el-dialog v-model="dialogVisible" :title="isEdit ? editTitle : addTitle" width="600px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="&#29992;&#25143;&#21517;"><el-input v-model="form.username" /></el-form-item>
        <el-form-item label="&#22995;&#21517;"><el-input v-model="form.nickname" /></el-form-item>
        <el-form-item label="&#24037;&#21495;"><el-input v-model="form.employeeId" /></el-form-item>
        <el-form-item label="&#37038;&#31665;"><el-input v-model="form.email" /></el-form-item>
        <el-form-item label="&#25163;&#26426;"><el-input v-model="form.phone" /></el-form-item>
        <el-form-item label="&#37096;&#38376;"><el-input v-model="form.department" /></el-form-item>
        <el-form-item label="&#23703;&#20301;"><el-input v-model="form.position" /></el-form-item>
        <el-form-item label="&#35282;&#33394;"><el-input v-model="form.role" /></el-form-item>
        <el-form-item label="&#29366;&#24577;">
          <el-select v-model="form.status" style="width: 100%;">
            <el-option :label="enabledText" :value="enabledText" />
            <el-option :label="disabledText" :value="disabledText" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">&#21462;&#28040;</el-button>
        <el-button type="primary" @click="saveUser">&#30830;&#23450;</el-button>
      </template>
    </el-dialog>


    <el-dialog v-model="roleDialogVisible" :title="roleDialogTitle" width="520px" @open="loadRoleOptions">
      <el-form label-width="80px">
        <el-form-item label="&#29992;&#25143;">
          <el-text>{{ roleUser?.nickname }}（{{ roleUser?.employeeId || roleUser?.username }}）</el-text>
        </el-form-item>
        <el-form-item label="&#35282;&#33394;">
          <el-checkbox-group v-model="selectedRoleCodes" class="role-checkbox-group">
            <el-checkbox v-for="role in roleOptions" :key="role.code" :label="role.code">
              <span>{{ role.name }}</span>
              <el-tag v-if="role.type === 'system'" size="small" type="primary" style="margin-left: 8px;">&#31995;&#32479;</el-tag>
            </el-checkbox>
          </el-checkbox-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="roleDialogVisible = false">&#21462;&#28040;</el-button>
        <el-button type="primary" :loading="roleSaving" @click="saveUserRoles">&#30830;&#23450;</el-button>
      </template>
    </el-dialog>
    <el-dialog v-model="importDialogVisible" title="&#23548;&#20837;&#21592;&#24037;&#20026;&#31995;&#32479;&#29992;&#25143;" width="820px" @open="loadImportableEmployees">
      <div class="filter-bar" style="padding: 0 0 12px;">
        <div class="filter-left">
          <el-input v-model="importKeyword" style="width: 260px" clearable placeholder="&#25628;&#32034;&#22995;&#21517;/&#24037;&#21495;/&#25163;&#26426;" prefix-icon="Search" @input="loadImportableEmployees" />
        </div>
        <el-text type="info">&#40664;&#35748;&#36134;&#21495;&#20026;&#21592;&#24037;&#24037;&#21495;&#65292;&#21021;&#22987;&#23494;&#30721; 123456</el-text>
      </div>
      <el-table v-loading="importLoading" :data="importableEmployees" height="360" stripe @selection-change="handleImportSelectionChange">
        <el-table-column type="selection" width="48" />
        <el-table-column prop="employeeNo" label="&#24037;&#21495;" width="130" />
        <el-table-column prop="name" label="&#22995;&#21517;" width="120" />
        <el-table-column prop="departmentName" label="&#37096;&#38376;" width="140" />
        <el-table-column prop="position" label="&#23703;&#20301;" width="140" />
        <el-table-column prop="phone" label="&#25163;&#26426;" width="140" />
        <el-table-column prop="email" label="&#37038;&#31665;" min-width="180" />
      </el-table>
      <div class="pagination-wrapper">
        <el-pagination v-model:current-page="importPage" v-model:page-size="importPageSize" :page-sizes="[10, 20, 50]" :total="importTotal" layout="total, sizes, prev, pager, next" @size-change="loadImportableEmployees" @current-change="loadImportableEmployees" />
      </div>
      <template #footer>
        <el-button @click="importDialogVisible = false">&#21462;&#28040;</el-button>
        <el-button type="primary" :disabled="!importSelection.length" :loading="importSaving" @click="importEmployees">&#30830;&#23450;&#23548;&#20837;</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/api/request'

interface UserRow {
  id: number | null
  username: string
  nickname: string
  employeeId: string
  email: string
  phone: string
  department: string
  position: string
  role: string
  status: string
  lastLogin?: string
}

interface RoleOption {
  code: string
  name: string
  type: string
}

interface EmployeeRow {
  id: number
  employeeNo: string
  name: string
  departmentName: string
  position: string
  email: string
  phone: string
  status: string
}

const enabledText = '\u542f\u7528'
const disabledText = '\u505c\u7528'
const addTitle = '\u65b0\u589e\u7528\u6237'
const editTitle = '\u7f16\u8f91\u7528\u6237'
const roleDialogTitle = '\u5206\u914d\u89d2\u8272'
const departmentOptions = ['\u7814\u53d1\u90e8', '\u9500\u552e\u90e8', '\u8d22\u52a1\u90e8', '\u4ed3\u50a8\u90e8', '\u4eba\u529b\u8d44\u6e90\u90e8']

const keyword = ref('')
const statusFilter = ref('')
const departmentFilter = ref('')
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)
const users = ref<UserRow[]>([])
const dialogVisible = ref(false)
const isEdit = ref(false)
const importDialogVisible = ref(false)
const importLoading = ref(false)
const importSaving = ref(false)
const importKeyword = ref('')
const importPage = ref(1)
const importPageSize = ref(10)
const importTotal = ref(0)
const importableEmployees = ref<EmployeeRow[]>([])
const importSelection = ref<EmployeeRow[]>([])
const roleDialogVisible = ref(false)
const roleSaving = ref(false)
const roleOptions = ref<RoleOption[]>([])
const roleUser = ref<UserRow | null>(null)
const selectedRoleCodes = ref<string[]>([])
const form = ref<UserRow>(emptyForm())

const stats = ref({ total: 0, active: 0, disabled: 0 })

function computeStats(list: UserRow[]) {
  const active = list.filter(u => u.status === enabledText).length
  return { total: list.length, active, disabled: list.length - active }
}

const filteredUsers = computed(() => {
  let result = users.value
  if (keyword.value) {
    result = result.filter(user => user.nickname.includes(keyword.value) || user.employeeId.includes(keyword.value) || user.phone.includes(keyword.value))
  }
  if (statusFilter.value) {
    result = result.filter(user => user.status === statusFilter.value)
  }
  if (departmentFilter.value) {
    result = result.filter(user => user.department === departmentFilter.value)
  }
  return result
})

function emptyForm(): UserRow {
  return { id: null, username: '', nickname: '', employeeId: '', email: '', phone: '', department: '', position: '', role: '', status: enabledText }
}

async function loadUsers() {
  try {
    const { data } = await request.get('/system/user/list', {
      params: { keyword: keyword.value, status: statusFilter.value, page: page.value, pageSize: pageSize.value }
    })
    if (data.code === 0) {
      users.value = data.data.list
      total.value = data.data.total
      stats.value = {
        total: data.data.total ?? users.value.length,
        active: (data.data.list as UserRow[]).filter(u => u.status === enabledText).length,
        disabled: (data.data.list as UserRow[]).filter(u => u.status === disabledText).length
      }
    }
  } catch (error) {
    console.error('Load users error:', error)
    const fallback = [
      { id: 1, username: 'admin', nickname: '\u7ba1\u7406\u5458', employeeId: 'EMP-001', email: 'admin@company.com', phone: '13800138000', department: '\u7cfb\u7edf\u7ba1\u7406\u90e8', position: '\u7cfb\u7edf\u7ba1\u7406\u5458', role: '\u8d85\u7ea7\u7ba1\u7406\u5458', status: enabledText, lastLogin: '2026-06-16 10:30' }
    ]
    users.value = fallback
    total.value = fallback.length
    stats.value = computeStats(fallback)
  }
}

function showAddDialog() {
  isEdit.value = false
  form.value = emptyForm()
  dialogVisible.value = true
}

function editUser(user: UserRow) {
  isEdit.value = true
  form.value = { ...user }
  dialogVisible.value = true
}

async function saveUser() {
  try {
    if (isEdit.value) {
      await request.post('/system/user/update', form.value)
      ElMessage.success('\u66f4\u65b0\u6210\u529f')
    } else {
      await request.post('/system/user/create', form.value)
      ElMessage.success('\u521b\u5efa\u6210\u529f')
    }
    dialogVisible.value = false
    loadUsers()
  } catch (error) {
    ElMessage.error('\u64cd\u4f5c\u5931\u8d25')
  }
}

function showImportDialog() {
  importKeyword.value = ''
  importPage.value = 1
  importSelection.value = []
  importDialogVisible.value = true
}

async function loadImportableEmployees() {
  if (!importDialogVisible.value) return
  importLoading.value = true
  try {
    const { data } = await request.get('/system/user/importable-employees', {
      params: { keyword: importKeyword.value, page: importPage.value, pageSize: importPageSize.value }
    })
    if (data.code === 0) {
      importableEmployees.value = data.data.list
      importTotal.value = data.data.total
    } else {
      ElMessage.error(data.message || '\u52a0\u8f7d\u5458\u5de5\u5931\u8d25')
    }
  } catch (error) {
    console.error('Load importable employees error:', error)
    ElMessage.error('\u52a0\u8f7d\u5458\u5de5\u5931\u8d25')
  } finally {
    importLoading.value = false
  }
}

function handleImportSelectionChange(selection: EmployeeRow[]) {
  importSelection.value = selection
}

async function importEmployees() {
  if (!importSelection.value.length) {
    ElMessage.warning('\u8bf7\u9009\u62e9\u8981\u5bfc\u5165\u7684\u5458\u5de5')
    return
  }
  importSaving.value = true
  try {
    const { data } = await request.post('/system/user/import-employees', {
      employeeIds: importSelection.value.map(employee => employee.id)
    })
    if (data.code === 0) {
      ElMessage.success(`\u6210\u529f\u5bfc\u5165 ${data.data.count} \u540d\u5458\u5de5`)
      importDialogVisible.value = false
      loadUsers()
    } else {
      ElMessage.error(data.message || '\u5bfc\u5165\u5931\u8d25')
    }
  } catch (error) {
    console.error('Import employees error:', error)
    ElMessage.error('\u5bfc\u5165\u5931\u8d25')
  } finally {
    importSaving.value = false
  }
}


async function loadRoleOptions() {
  try {
    const { data } = await request.get('/system/user/roles')
    if (data.code === 0) {
      roleOptions.value = data.data
    } else {
      ElMessage.error(data.message || '\u52a0\u8f7d\u89d2\u8272\u5931\u8d25')
    }
  } catch (error) {
    console.error('Load roles error:', error)
    ElMessage.error('\u52a0\u8f7d\u89d2\u8272\u5931\u8d25')
  }
}

async function openRoleDialog(user: UserRow) {
  roleUser.value = user
  selectedRoleCodes.value = []
  roleDialogVisible.value = true
  await loadRoleOptions()
  try {
    const { data } = await request.get('/system/user/role-codes', { params: { userId: user.id } })
    if (data.code === 0) {
      selectedRoleCodes.value = data.data
    }
  } catch (error) {
    console.error('Load user role codes error:', error)
  }
}

async function saveUserRoles() {
  if (!roleUser.value?.id) {
    return
  }
  roleSaving.value = true
  try {
    const { data } = await request.post('/system/user/assign-roles', {
      userId: roleUser.value.id,
      roleCodes: selectedRoleCodes.value
    })
    if (data.code === 0) {
      ElMessage.success('\u6388\u6743\u6210\u529f')
      roleDialogVisible.value = false
      await loadUsers()
    } else {
      ElMessage.error(data.message || '\u6388\u6743\u5931\u8d25')
    }
  } catch (error) {
    console.error('Save user roles error:', error)
    ElMessage.error('\u6388\u6743\u5931\u8d25')
  } finally {
    roleSaving.value = false
  }
}
async function toggleUserStatus(user: UserRow) {
  const isEnabled = user.status === enabledText
  const targetStatus = isEnabled ? disabledText : enabledText
  const actionText = isEnabled ? '\u7981\u7528' : '\u542f\u7528'
  try {
    await ElMessageBox.confirm(`\u786e\u8ba4${actionText}\u8be5\u7528\u6237\u5417\uff1f`, '\u63d0\u793a')
    await request.post('/system/user/update', { id: user.id, status: targetStatus })
    ElMessage.success(`${actionText}\u6210\u529f`)
    loadUsers()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('\u64cd\u4f5c\u5931\u8d25')
    }
  }
}

onMounted(() => {
  loadUsers()
})
</script>

<style scoped>
.user-mgmt-stats .stat-card {
  padding: 20px 24px;
}
.user-mgmt-stats .stat-card::before {
  display: none;
}
.user-mgmt-stats .stat-body {
  display: flex;
  align-items: center;
  gap: 20px;
}
.user-mgmt-stats .stat-info {
  flex: 1;
  min-width: 0;
}
.user-mgmt-stats .stat-header {
  margin-bottom: 8px;
}
.user-mgmt-stats .stat-value {
  font-size: 30px;
  font-weight: 700;
  margin-bottom: 4px;
}
.user-mgmt-stats .stat-icon {
  width: 54px;
  height: 54px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 26px;
  flex-shrink: 0;
}
.user-mgmt-stats .stat-card.blue .stat-icon {
  background: #ecf5ff;
  color: #409eff;
}
.user-mgmt-stats .stat-card.green .stat-icon {
  background: #f0f9eb;
  color: #67c23a;
}
.user-mgmt-stats .stat-card.red .stat-icon {
  background: #fef0f0;
  color: #f56c6c;
}
.table-card {
  padding: 20px 24px;
}
.table-card .filter-bar {
  margin-bottom: 16px;
}
.table-card :deep(.el-table) {
  font-size: 13px;
}
.table-card :deep(.el-table th.el-table__cell) {
  background: #fafafa !important;
  color: #606266;
  font-weight: 600;
}
.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  padding-top: 16px;
}
</style>
