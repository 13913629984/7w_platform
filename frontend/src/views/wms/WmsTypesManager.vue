<template>
  <section>
    <div class="sub-page-header">
      <div>
        <h2>类型管理</h2>
        <p class="page-desc">维护物料类型树和库存分类层级。</p>
      </div>
      <el-space>
        <el-button plain :icon="Refresh" @click="loadTree">刷新</el-button>
        <el-button type="primary" :icon="Plus" @click="handleAddRoot">新增一级类型</el-button>
      </el-space>
    </div>

    <div class="stats-row">
      <div class="stat-card blue">
        <div class="stat-header"><span class="stat-title">类型总数</span><div class="stat-icon">📂</div></div>
        <div class="stat-value">{{ stats.total }}</div>
        <div class="stat-footer">含一级与子类型</div>
      </div>
      <div class="stat-card green">
        <div class="stat-header"><span class="stat-title">一级类型</span><div class="stat-icon">🌲</div></div>
        <div class="stat-value">{{ stats.rootCount }}</div>
        <div class="stat-footer">顶层分类数量</div>
      </div>
      <div class="stat-card orange">
        <div class="stat-header"><span class="stat-title">子类型</span><div class="stat-icon">🌿</div></div>
        <div class="stat-value">{{ stats.childCount }}</div>
        <div class="stat-footer">细分类型数量</div>
      </div>
      <div class="stat-card purple">
        <div class="stat-header"><span class="stat-title">禁用类型</span><div class="stat-icon">🚫</div></div>
        <div class="stat-value">{{ stats.disabledCount }}</div>
        <div class="stat-footer">已停用分类</div>
      </div>
    </div>

    <div class="wms-type-layout">
      <div class="wms-type-tree-panel">
        <div class="wms-type-panel-header">
          <span class="wms-type-title">搜索</span>
          <el-input v-model="searchText" clearable placeholder="搜索类型..." style="width: 220px" :prefix-icon="Search" />
        </div>
        <div class="wms-type-tree" v-loading="loading">
          <el-tree
            ref="treeRef"
            :data="treeData"
            :props="treeProps"
            :expand-on-click-node="false"
            :filter-node-method="filterNode"
            highlight-current
            node-key="code"
            default-expand-all
            @node-click="handleNodeClick"
          >
            <template #default="{ node, data }">
              <span class="wms-tree-node">
                <span class="wms-tree-label">{{ node.label }}</span>
                <el-tag size="small" type="info" v-if="data.materialCount > 0">{{ data.materialCount }} 物料</el-tag>
                <el-tag size="small" type="danger" v-if="data.status === 0">禁用</el-tag>
                <el-button v-if="data.level === 1" link type="primary" @click.stop="handleAddChild(data)">+ 子类型</el-button>
              </span>
            </template>
          </el-tree>
        </div>
      </div>

      <div class="wms-type-form-panel">
        <div class="wms-type-panel-header">
          <span class="wms-type-title">{{ panelTitle }}</span>
          <el-space v-if="selectedNode || isEdit">
            <template v-if="!isEdit && selectedNode">
              <el-button type="primary" plain @click="handleEdit">编辑</el-button>
              <el-button type="warning" plain @click="handleToggleStatus" v-if="selectedNode.status === 1">禁用</el-button>
              <el-button type="success" plain @click="handleToggleStatus" v-else>启用</el-button>
              <el-button type="danger" plain @click="handleDelete">删除</el-button>
            </template>
            <template v-else>
              <el-button type="primary" :loading="submitting" @click="submitForm">保存</el-button>
              <el-button @click="handleCancel">取消</el-button>
            </template>
          </el-space>
        </div>

        <div v-if="selectedNode || isEdit" class="wms-type-form-content">
          <el-form :model="form" :rules="rules" ref="formRef" label-width="100px">
            <el-form-item label="类型编码" prop="code">
              <el-input v-model="form.code" :disabled="!isEdit || !!form.id" placeholder="如：electronics-ic" maxlength="50" />
            </el-form-item>
            <el-form-item label="类型名称" prop="name">
              <el-input v-model="form.name" :disabled="!isEdit" placeholder="请输入类型名称" maxlength="100" />
            </el-form-item>
            <el-form-item label="上级类型">
              <el-select
                v-model="form.parentCode"
                :disabled="!isEdit || (form.id && form.level === 1 && hasChildren(form.code))"
                placeholder="无（一级类型）"
                clearable
                style="width: 100%"
              >
                <el-option
                  v-for="item in parentOptions"
                  :key="item.code"
                  :label="`${item.name} (${item.code})`"
                  :value="item.code"
                  :disabled="form.id ? (item.code === form.code) : false"
                />
              </el-select>
            </el-form-item>
            <el-form-item label="层级">
              <el-tag>{{ form.parentCode ? '二级类型' : '一级类型' }}</el-tag>
            </el-form-item>
            <el-form-item label="排序">
              <el-input-number v-model="form.sort" :min="0" :step="1" :disabled="!isEdit" controls-position="right" style="width: 200px" />
            </el-form-item>
            <el-form-item label="状态">
              <el-switch v-model="form.status" :disabled="!isEdit" :active-value="1" :inactive-value="0" active-text="启用" inactive-text="禁用" />
            </el-form-item>
            <el-form-item label="类型描述">
              <el-input v-model="form.description" :disabled="!isEdit" type="textarea" :rows="4" placeholder="请输入类型描述" maxlength="500" show-word-limit />
            </el-form-item>
            <el-form-item label="绑定物料数" v-if="form.id">
              <el-tag type="success">{{ form.materialCount ?? 0 }}</el-tag>
            </el-form-item>
            <el-form-item label="创建时间" v-if="form.id">
              <span>{{ formatTime(form.createTime) }}</span>
            </el-form-item>
            <el-form-item label="更新时间" v-if="form.id">
              <span>{{ formatTime(form.updateTime) }}</span>
            </el-form-item>
          </el-form>
        </div>
        <el-empty v-else description="请选择左侧类型，或新增一级/子类型" />
      </div>
    </div>
  </section>
</template>

<style scoped>
.stats-row { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; margin: 16px 0; }
.stat-card { background: #fff; border-radius: 8px; padding: 16px 20px; border-top: 3px solid #409eff; box-shadow: 0 2px 8px rgba(0,0,0,0.04); }
.stat-card.blue { border-top-color: #409eff; }
.stat-card.green { border-top-color: #67c23a; }
.stat-card.orange { border-top-color: #e6a23c; }
.stat-card.purple { border-top-color: #9b59b6; }
.stat-header { display:flex; justify-content:space-between; align-items:center; }
.stat-title { color: #606266; font-size: 14px; }
.stat-icon { font-size: 22px; }
.stat-value { font-size: 28px; font-weight: 600; margin-top: 8px; color: #303133; }
.stat-footer { color: #909399; font-size: 12px; margin-top: 4px; }
.wms-type-layout { display: grid; grid-template-columns: 360px 1fr; gap: 16px; }
.wms-type-tree-panel, .wms-type-form-panel { background: #fff; border-radius: 8px; padding: 16px; box-shadow: 0 2px 8px rgba(0,0,0,0.04); min-height: 480px; }
.wms-type-panel-header { display:flex; justify-content:space-between; align-items:center; margin-bottom: 12px; }
.wms-type-title { font-size: 16px; font-weight: 600; color: #303133; }
.wms-tree-node { display:flex; align-items:center; gap: 8px; flex: 1; }
.wms-tree-label { flex: 1; }
.wms-type-tree { max-height: 600px; overflow: auto; }
.wms-type-form-content { max-width: 720px; }
.sub-page-header { display:flex; justify-content:space-between; align-items:flex-start; padding: 8px 0 16px; }
.sub-page-header h2 { margin: 0; font-size: 22px; color: #303133; }
.page-desc { color: #909399; font-size: 13px; margin-top: 4px; }
</style>
<script setup lang="ts">
import { computed, nextTick, onMounted, reactive, ref, watch } from 'vue'
import type { ElTree, FormInstance, FormRules } from 'element-plus'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'
import {
  changeMaterialTypeStatus,
  createMaterialType,
  deleteMaterialType,
  listMaterialTypeTree,
  updateMaterialType,
  type MaterialTypeNode,
} from '@/api/wms/materialType'

const treeRef = ref<InstanceType<typeof ElTree>>()
const formRef = ref<FormInstance>()
const treeData = ref<MaterialTypeNode[]>([])
const flatList = ref<MaterialTypeNode[]>([])
const searchText = ref('')
const selectedNode = ref<MaterialTypeNode | null>(null)
const isEdit = ref(false)
const submitting = ref(false)
const loading = ref(false)

const treeProps = { label: 'name', children: 'children' }

interface FormState extends MaterialTypeNode {
  status: number
}

const form = reactive<FormState>(emptyForm())

const rules: FormRules = {
  code: [
    { required: true, message: '请填写类型编码', trigger: 'blur' },
    { max: 50, message: '类型编码长度不能超过50', trigger: 'blur' },
  ],
  name: [
    { required: true, message: '请填写类型名称', trigger: 'blur' },
    { max: 100, message: '类型名称长度不能超过100', trigger: 'blur' },
  ],
}

const stats = computed(() => {
  const total = flatList.value.length
  const rootCount = flatList.value.filter((item) => !item.parentCode).length
  const childCount = total - rootCount
  const disabledCount = flatList.value.filter((item) => item.status === 0).length
  return { total, rootCount, childCount, disabledCount }
})

const parentOptions = computed(() => flatList.value.filter((item) => (item.level ?? 1) === 1))

const panelTitle = computed(() => {
  if (selectedNode.value) return isEdit.value ? '编辑类型' : '类型详情'
  if (isEdit.value) return form.parentCode ? '新增子类型' : '新增一级类型'
  return '请选择类型'
})

watch(searchText, (value) => {
  treeRef.value?.filter(value)
})

function emptyForm(parent?: MaterialTypeNode): FormState {
  return {
    id: undefined,
    code: '',
    name: '',
    description: '',
    parentCode: parent?.code || '',
    parentName: parent?.name || '',
    level: parent ? 2 : 1,
    sort: 0,
    status: 1,
    materialCount: 0,
    createTime: undefined,
    updateTime: undefined,
  }
}

function flatten(nodes: MaterialTypeNode[], result: MaterialTypeNode[] = []): MaterialTypeNode[] {
  nodes.forEach((node) => {
    result.push(node)
    if (node.children?.length) flatten(node.children, result)
  })
  return result
}

function filterNode(value: string, data: MaterialTypeNode) {
  if (!value) return true
  const kw = value.toLowerCase()
  return (data.name || '').toLowerCase().includes(kw) || (data.code || '').toLowerCase().includes(kw)
}

function hasChildren(code?: string) {
  if (!code) return false
  return flatList.value.some((item) => item.parentCode === code)
}

function formatTime(value?: string) {
  if (!value) return '-'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`
}

async function loadTree() {
  loading.value = true
  try {
    const res = await listMaterialTypeTree()
    if (res.code !== 0) {
      ElMessage.error(res.message || '加载类型失败')
      return
    }
    treeData.value = res.data || []
    flatList.value = flatten(treeData.value)
    if (selectedNode.value) {
      const reSelect = flatList.value.find((item) => item.code === selectedNode.value?.code)
      if (reSelect) {
        selectedNode.value = reSelect
        Object.assign(form, normalizeForm(reSelect))
      } else {
        selectedNode.value = null
        Object.assign(form, emptyForm())
        isEdit.value = false
      }
    }
    nextTick(() => {
      if (selectedNode.value) {
        treeRef.value?.setCurrentKey(selectedNode.value.code)
      }
    })
  } catch (error: any) {
    ElMessage.error(error?.message || '加载类型失败')
  } finally {
    loading.value = false
  }
}

function normalizeForm(node: MaterialTypeNode): FormState {
  return {
    id: node.id,
    code: node.code,
    name: node.name,
    description: node.description || '',
    parentCode: node.parentCode || '',
    parentName: node.parentName || '',
    level: node.level || 1,
    sort: node.sort ?? 0,
    status: node.status ?? 1,
    materialCount: node.materialCount ?? 0,
    createTime: node.createTime,
    updateTime: node.updateTime,
  }
}

function handleNodeClick(data: MaterialTypeNode) {
  selectedNode.value = data
  Object.assign(form, normalizeForm(data))
  isEdit.value = false
}

function handleAddRoot() {
  selectedNode.value = null
  Object.assign(form, emptyForm())
  isEdit.value = true
  treeRef.value?.setCurrentKey(undefined as unknown as string)
}

function handleAddChild(parent: MaterialTypeNode) {
  selectedNode.value = null
  Object.assign(form, emptyForm(parent))
  isEdit.value = true
}

function handleEdit() {
  isEdit.value = true
}

function handleCancel() {
  if (selectedNode.value) {
    Object.assign(form, normalizeForm(selectedNode.value))
    isEdit.value = false
  } else {
    Object.assign(form, emptyForm())
    isEdit.value = false
  }
}

async function submitForm() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    const payload = {
      id: form.id,
      code: form.code,
      name: form.name,
      description: form.description,
      parentCode: form.parentCode || null,
      sort: form.sort,
      status: form.status,
    }
    const res = form.id ? await updateMaterialType(payload) : await createMaterialType(payload)
    if (res.code !== 0) {
      ElMessage.error(res.message || '保存失败')
      return
    }
    ElMessage.success(form.id ? '类型信息已更新' : '类型已创建')
    isEdit.value = false
    selectedNode.value = res.data
    await loadTree()
    nextTick(() => treeRef.value?.setCurrentKey(res.data.code))
  } catch (error: any) {
    ElMessage.error(error?.message || '保存失败')
  } finally {
    submitting.value = false
  }
}

async function handleDelete() {
  if (!selectedNode.value?.id) return
  try {
    await ElMessageBox.confirm(`确定要删除类型「${selectedNode.value.name}」吗？删除后无法恢复。`, '删除确认', { type: 'warning' })
  } catch {
    return
  }
  try {
    const res = await deleteMaterialType(selectedNode.value.id)
    if (res.code !== 0) {
      ElMessage.error(res.message || '删除失败')
      return
    }
    ElMessage.success('类型已删除')
    selectedNode.value = null
    Object.assign(form, emptyForm())
    isEdit.value = false
    await loadTree()
  } catch (error: any) {
    ElMessage.error(error?.message || '删除失败')
  }
}

async function handleToggleStatus() {
  if (!selectedNode.value?.id) return
  const next = selectedNode.value.status === 1 ? 0 : 1
  try {
    const res = await changeMaterialTypeStatus(selectedNode.value.id, next)
    if (res.code !== 0) {
      ElMessage.error(res.message || '状态更新失败')
      return
    }
    ElMessage.success(next === 1 ? '已启用' : '已禁用')
    await loadTree()
  } catch (error: any) {
    ElMessage.error(error?.message || '状态更新失败')
  }
}

onMounted(() => {
  loadTree()
})
</script>