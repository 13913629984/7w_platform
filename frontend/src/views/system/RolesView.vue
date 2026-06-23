<template>
  <section>
    <div class="sub-page-header">
      <div>
        <h2>角色管理</h2>
        <p class="page-desc">参考权限管理维护角色、权限点和用户角色关系。</p>
      </div>
    </div>

    <div class="stats-row role-stats-row">
      <div class="stat-card blue">
        <div class="stat-header"><span class="stat-title">角色总数</span></div>
        <div class="stat-value">{{ stats.total }}</div>
        <div class="stat-footer"><span>系统 {{ Number(stats.total) - Number(stats.custom) }} / 自定义 {{ stats.custom }}</span></div>
      </div>
      <div class="stat-card green">
        <div class="stat-header"><span class="stat-title">启用角色</span></div>
        <div class="stat-value">{{ stats.active }}</div>
        <div class="stat-footer"><span>停用 {{ Number(stats.total) - Number(stats.active) }}</span></div>
      </div>
      <div class="stat-card orange">
        <div class="stat-header"><span class="stat-title">已授权限点</span></div>
        <div class="stat-value">{{ stats.assignedPermissions }}</div>
        <div class="stat-footer"><span>权限池 {{ stats.permissions }}</span></div>
      </div>
      <div class="stat-card red">
        <div class="stat-header"><span class="stat-title">用户角色关系</span></div>
        <div class="stat-value">{{ stats.assignedUsers }}</div>
        <div class="stat-footer"><span>关联账号数量</span></div>
      </div>
    </div>

    <div class="role-page-layout">
      <div class="table-card role-table-card">
        <div class="filter-bar">
          <div class="filter-left">
            <el-input v-model="keyword" style="width: 240px" clearable placeholder="搜索编码/名称/说明" prefix-icon="Search" @input="handleSearch" />
            <el-select v-model="typeFilter" style="width: 140px" placeholder="角色类型" clearable @change="loadRoles">
              <el-option label="全部类型" value="" />
              <el-option label="系统角色" value="system" />
              <el-option label="自定义角色" value="custom" />
            </el-select>
            <el-select v-model="statusFilter" style="width: 120px" placeholder="状态" clearable @change="loadRoles">
              <el-option label="全部状态" value="" />
              <el-option label="启用" value="启用" />
              <el-option label="停用" value="停用" />
            </el-select>
          </div>
          <el-space>
            <el-button type="primary" @click="handleSearch">查询</el-button>
            <el-button type="primary" icon="Plus" @click="openRoleDialog()">新增角色</el-button>
          </el-space>
        </div>

        <el-table :data="roles" stripe v-loading="loading" highlight-current-row @current-change="selectRole">
          <el-table-column prop="code" label="角色编码" width="160" />
          <el-table-column label="角色名称" min-width="180">
            <template #default="scope">
              <div class="role-name-cell">
                <strong>{{ scope.row.name }}</strong>
                <span>{{ scope.row.description || '暂无说明' }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="type" label="类型" width="110">
            <template #default="scope">
              <el-tag :type="scope.row.type === 'system' ? 'primary' : 'success'">{{ scope.row.type === 'system' ? '系统' : '自定义' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="userCount" label="用户数" width="90" />
          <el-table-column prop="permissionCount" label="权限数" width="90" />
          <el-table-column prop="status" label="状态" width="100">
            <template #default="scope">
              <el-tag :type="scope.row.status === enabledText ? 'success' : 'info'">{{ scope.row.status }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="updateTime" label="更新时间" width="180">
            <template #default="scope">{{ formatDateTime(scope.row.updateTime) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="250" fixed="right">
            <template #default="scope">
              <el-button link type="primary" @click="openRoleDialog(scope.row)">编辑</el-button>
              <el-button link type="primary" @click="openPermissionDialog(scope.row)">授权</el-button>
              <el-button link type="primary" @click="openAssignDialog(scope.row)">用户</el-button>
              <el-button link :type="scope.row.status === enabledText ? 'warning' : 'success'" @click="toggleRoleStatus(scope.row)">{{ scope.row.status === enabledText ? '停用' : '启用' }}</el-button>
              <el-button link type="danger" :disabled="scope.row.type === 'system'" @click="deleteRole(scope.row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>

        <div class="pagination-wrap">
          <el-pagination v-model:current-page="page" v-model:page-size="pageSize" layout="total, sizes, prev, pager, next" :total="total" @size-change="loadRoles" @current-change="loadRoles" />
        </div>
      </div>

      <div class="role-detail-card">
        <div class="detail-card-header">
          <div class="card-title">角色详情</div>
          <el-button size="small" type="primary" :disabled="!selectedRole" @click="openPermissionDialog(selectedRole || undefined)">配置权限</el-button>
        </div>
        <template v-if="selectedRole">
          <div class="role-profile">
            <div class="role-avatar">{{ selectedRole.name.slice(0, 1) }}</div>
            <div>
              <h3>{{ selectedRole.name }}</h3>
              <p>{{ selectedRole.code }}</p>
            </div>
          </div>
          <div class="role-meta-grid">
            <div><span>类型</span><strong>{{ selectedRole.type === 'system' ? '系统角色' : '自定义角色' }}</strong></div>
            <div><span>状态</span><strong>{{ selectedRole.status }}</strong></div>
            <div><span>用户</span><strong>{{ selectedRole.userCount }} 人</strong></div>
            <div><span>权限</span><strong>{{ selectedRole.permissionCount }} 个</strong></div>
          </div>
          <div class="role-detail-section">
            <div class="section-title-small">角色说明</div>
            <p>{{ selectedRole.description || '暂无说明' }}</p>
          </div>
          <div class="role-detail-section">
            <div class="section-title-small">已授权限</div>
            <div class="role-permission-tags">
              <el-tag v-for="code in selectedPermissionNames" :key="code" size="small">{{ code }}</el-tag>
              <span v-if="selectedPermissionNames.length === 0" class="empty-text">暂未配置权限</span>
            </div>
          </div>
          <div class="role-detail-section">
            <div class="section-title-small">关联用户</div>
            <div class="role-user-list">
              <div v-for="user in selectedUsers" :key="user.id" class="role-user-item">
                <div class="avatar mini-avatar">{{ user.nickname?.charAt(0) || user.username.charAt(0) }}</div>
                <div>
                  <strong>{{ user.nickname || user.username }}</strong>
                  <span>{{ user.department || '-' }} · {{ user.position || '-' }}</span>
                </div>
              </div>
              <span v-if="selectedUsers.length === 0" class="empty-text">暂无关联用户</span>
            </div>
          </div>
        </template>
        <el-empty v-else description="请选择角色查看详情" />
      </div>
    </div>

    <el-dialog v-model="roleDialogVisible" :title="isEdit ? '编辑角色' : '新增角色'" width="560px">
      <el-form label-width="100px" :model="form">
        <el-form-item label="角色编码" required>
          <el-input v-model="form.code" :disabled="isEdit" placeholder="如：CRM_SALES" />
        </el-form-item>
        <el-form-item label="角色名称" required>
          <el-input v-model="form.name" placeholder="请输入角色名称" />
        </el-form-item>
        <el-form-item label="角色类型">
          <el-radio-group v-model="form.type" :disabled="isEdit && editingRole?.type === 'system'">
            <el-radio label="system">系统角色</el-radio>
            <el-radio label="custom">自定义角色</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="角色说明">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="请输入角色说明" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.status" active-value="启用" inactive-value="停用" active-text="启用" inactive-text="停用" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="roleDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveRole">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="permissionDialogVisible" :title="permissionTitle" width="620px">
      <div class="permission-tip">勾选模块或权限点后保存，将写入后台角色权限关系表。</div>
      <el-tree ref="permissionTreeRef" class="permission-tree" :data="permissionTree" :props="permissionProps" node-key="code" show-checkbox default-expand-all>
        <template #default="{ data }">
          <span class="permission-node">
            <span>{{ data.name }}</span>
            <em v-if="data.description">{{ data.description }}</em>
          </span>
        </template>
      </el-tree>
      <template #footer>
        <el-button @click="permissionDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="savePermissions">保存权限</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="assignDialogVisible" :title="assignTitle" width="620px">
      <el-form label-width="90px">
        <el-form-item label="目标角色">
          <el-select v-model="assignForm.roleCode" style="width: 100%" placeholder="请选择角色" @change="syncAssignUsers">
            <el-option v-for="role in allRoles" :key="role.code" :label="role.name" :value="role.code" />
          </el-select>
        </el-form-item>
        <el-form-item label="选择用户">
          <el-select v-model="assignForm.userIds" style="width: 100%" multiple filterable placeholder="请选择用户">
            <el-option v-for="user in users" :key="user.id" :label="`${user.nickname || user.username}（${user.department || '-'}）`" :value="user.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="说明">
          <el-input type="textarea" :rows="2" placeholder="用户分配将保存到 sys_user_role 表" disabled />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="assignDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveUserRoles">确定分配</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type TreeInstance } from 'element-plus'
import request from '@/api/request'

const enabledText = '启用'
const disabledText = '停用'

type RoleRow = {
  id: number | null
  code: string
  name: string
  type: 'system' | 'custom'
  description: string
  status: string
  userCount: number
  permissionCount: number
  permissionCodes: string[]
  userIds: number[]
  createTime?: string
  updateTime?: string
}

type PermissionNode = {
  id: number
  code: string
  name: string
  parentCode?: string
  module?: string
  description?: string
  sort: number
  children: PermissionNode[]
}

type UserRow = {
  id: number
  username: string
  nickname: string
  employeeId: string
  department: string
  position: string
}

const loading = ref(false)
const saving = ref(false)
const keyword = ref('')
const typeFilter = ref('')
const statusFilter = ref('')
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)
const roles = ref<RoleRow[]>([])
const allRoles = ref<RoleRow[]>([])
const users = ref<UserRow[]>([])
const permissionTree = ref<PermissionNode[]>([])
const selectedRole = ref<RoleRow | null>(null)
const editingRole = ref<RoleRow | null>(null)
const roleDialogVisible = ref(false)
const permissionDialogVisible = ref(false)
const assignDialogVisible = ref(false)
const isEdit = ref(false)
const permissionTreeRef = ref<TreeInstance>()
const stats = reactive({ total: 0, active: 0, custom: 0, permissions: 0, assignedPermissions: 0, assignedUsers: 0 })
const form = ref(emptyForm())
const assignForm = reactive({ roleCode: '', userIds: [] as number[] })
const permissionProps = { label: 'name', children: 'children' }

const permissionNameMap = computed(() => {
  const map: Record<string, string> = {}
  const walk = (nodes: PermissionNode[]) => nodes.forEach(node => {
    map[node.code] = node.name.replace(/^\S+\s/, '')
    if (node.children?.length) walk(node.children)
  })
  walk(permissionTree.value)
  return map
})

const selectedPermissionNames = computed(() => selectedRole.value?.permissionCodes.map(code => permissionNameMap.value[code]).filter(Boolean) || [])
const selectedUsers = computed(() => selectedRole.value ? users.value.filter(user => selectedRole.value?.userIds.includes(user.id)) : [])
const permissionTitle = computed(() => selectedRole.value ? `权限配置 - ${selectedRole.value.name}` : '权限配置')
const assignTitle = computed(() => assignForm.roleCode ? `分配用户 - ${allRoles.value.find(role => role.code === assignForm.roleCode)?.name || assignForm.roleCode}` : '分配用户')

function emptyForm(): RoleRow {
  return { id: null, code: '', name: '', type: 'custom', description: '', status: enabledText, userCount: 0, permissionCount: 0, permissionCodes: [], userIds: [] }
}

async function loadRoles() {
  loading.value = true
  try {
    const { data } = await request.get('/system/role/list', {
      params: { keyword: keyword.value, type: typeFilter.value, status: statusFilter.value, page: page.value, pageSize: pageSize.value }
    })
    if (data.code === 0) {
      roles.value = data.data.list
      total.value = data.data.total
      Object.assign(stats, data.data.stats)
      if (!selectedRole.value || !roles.value.some(role => role.code === selectedRole.value?.code)) {
        selectedRole.value = roles.value[0] || null
      } else {
        selectedRole.value = roles.value.find(role => role.code === selectedRole.value?.code) || selectedRole.value
      }
    } else {
      ElMessage.error(data.message || '加载角色失败')
    }
  } catch (error) {
    console.error('Load roles error:', error)
    ElMessage.error('加载角色失败')
  } finally {
    loading.value = false
  }
}

async function loadOverview() {
  const { data } = await request.get('/system/role/overview')
  if (data.code === 0) {
    permissionTree.value = data.data.permissions
    Object.assign(stats, data.data.stats)
  }
}

async function loadAllRoles() {
  const { data } = await request.get('/system/role/list', { params: { page: 1, pageSize: 1000 } })
  if (data.code === 0) allRoles.value = data.data.list
}

async function loadUsers() {
  const { data } = await request.get('/system/role/users')
  if (data.code === 0) users.value = data.data
}

async function loadAll() {
  await Promise.all([loadOverview(), loadUsers(), loadAllRoles(), loadRoles()])
}

function handleSearch() {
  page.value = 1
  loadRoles()
}

function selectRole(role: RoleRow) {
  selectedRole.value = role
}

function openRoleDialog(role?: RoleRow) {
  isEdit.value = !!role
  editingRole.value = role || null
  form.value = role ? { ...role, permissionCodes: [...role.permissionCodes], userIds: [...role.userIds] } : emptyForm()
  roleDialogVisible.value = true
}

async function saveRole() {
  if (!form.value.code.trim() || !form.value.name.trim()) {
    ElMessage.warning('请填写角色编码和角色名称')
    return
  }
  saving.value = true
  try {
    const api = isEdit.value ? '/system/role/update' : '/system/role/create'
    const { data } = await request.post(api, { ...form.value, code: form.value.code.trim().toUpperCase() })
    if (data.code === 0) {
      ElMessage.success(isEdit.value ? '更新成功' : '创建成功')
      roleDialogVisible.value = false
      await Promise.all([loadRoles(), loadAllRoles()])
    } else {
      ElMessage.error(data.message || '保存失败')
    }
  } catch (error) {
    console.error('Save role error:', error)
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}

async function toggleRoleStatus(role: RoleRow) {
  form.value = { ...role, status: role.status === enabledText ? disabledText : enabledText }
  isEdit.value = true
  editingRole.value = role
  await saveRole()
}

async function deleteRole(role: RoleRow) {
  try {
    await ElMessageBox.confirm(`确认删除角色「${role.name}」吗？`, '提示')
    const { data } = await request.post('/system/role/delete', { id: role.id })
    if (data.code === 0) {
      ElMessage.success('删除成功')
      await Promise.all([loadRoles(), loadAllRoles()])
    } else {
      ElMessage.error(data.message || '删除失败')
    }
  } catch (error) {
    if (error !== 'cancel') ElMessage.error('删除失败')
  }
}

function openPermissionDialog(role?: RoleRow) {
  if (!role) {
    ElMessage.warning('请先选择角色')
    return
  }
  selectedRole.value = role
  permissionDialogVisible.value = true
  nextTick(() => {
    permissionTreeRef.value?.setCheckedKeys(role.permissionCodes || [])
  })
}

async function savePermissions() {
  if (!selectedRole.value) return
  saving.value = true
  try {
    const checkedKeys = permissionTreeRef.value?.getCheckedKeys(false) || []
    const halfCheckedKeys = permissionTreeRef.value?.getHalfCheckedKeys() || []
    const permissionCodes = [...checkedKeys, ...halfCheckedKeys].map(String)
    const { data } = await request.post('/system/role/permissions', { roleCode: selectedRole.value.code, permissionCodes })
    if (data.code === 0) {
      ElMessage.success('权限保存成功')
      permissionDialogVisible.value = false
      await Promise.all([loadRoles(), loadAllRoles(), loadOverview()])
    } else {
      ElMessage.error(data.message || '权限保存失败')
    }
  } catch (error) {
    console.error('Save permissions error:', error)
    ElMessage.error('权限保存失败')
  } finally {
    saving.value = false
  }
}

function openAssignDialog(role?: RoleRow) {
  const target = role || selectedRole.value || allRoles.value[0]
  if (!target) {
    ElMessage.warning('暂无可分配角色')
    return
  }
  assignForm.roleCode = target.code
  assignForm.userIds = [...(target.userIds || [])]
  selectedRole.value = target
  assignDialogVisible.value = true
}

function syncAssignUsers() {
  const role = allRoles.value.find(item => item.code === assignForm.roleCode)
  assignForm.userIds = [...(role?.userIds || [])]
}

async function saveUserRoles() {
  if (!assignForm.roleCode) {
    ElMessage.warning('请选择目标角色')
    return
  }
  saving.value = true
  try {
    const { data } = await request.post('/system/role/users', { roleCode: assignForm.roleCode, userIds: assignForm.userIds })
    if (data.code === 0) {
      ElMessage.success('用户分配成功')
      assignDialogVisible.value = false
      await Promise.all([loadRoles(), loadAllRoles(), loadOverview()])
    } else {
      ElMessage.error(data.message || '用户分配失败')
    }
  } catch (error) {
    console.error('Save user roles error:', error)
    ElMessage.error('用户分配失败')
  } finally {
    saving.value = false
  }
}

function formatDateTime(value?: string) {
  if (!value) return '-'
  return value.replace('T', ' ').slice(0, 19)
}

onMounted(() => {
  loadAll()
})
</script>
