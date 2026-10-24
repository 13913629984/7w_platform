<template>
  <section class="table-page layout-page">
    <AppPageHeader title="产品BOM树管理" description="定义产品BOM层级结构，为节点指定可用物料（主料 / 备料）。">
      <template #actions>
        <el-button plain :icon="Refresh" @click="loadProducts">刷新</el-button>
        <el-button type="primary" :icon="Plus" @click="openProductDialog('add')">新增产品</el-button>
      </template>
    </AppPageHeader>

    <AppStatGrid v-if="stats">
      <AppStatCard label="产品数量" accent="blue" icon="📦" :value="stats.productTotal" footer="已维护BOM的产品" />
      <AppStatCard label="BOM节点" accent="green" icon="🌿" :value="stats.nodeTotal" footer="层级节点总数" />
      <AppStatCard label="物料关联" accent="orange" icon="🔗" :value="stats.materialTotal" footer="节点-物料关联数" />
    </AppStatGrid>

    <div class="bom-layout">
      <!-- 左侧：产品列表 -->
      <div class="bom-col bom-col-left">
        <div class="panel-card">
          <div class="panel-title">产品列表</div>
          <div class="filter-left bom-product-filter">
            <el-input v-model="productKeyword" clearable placeholder="搜索编码/名称" :prefix-icon="Search"
              @keyup.enter="handleProductSearch" @clear="handleProductSearch" />
            <el-select v-model="productStatusFilter" clearable placeholder="状态" style="width:110px" @change="handleProductSearch">
              <el-option label="启用" :value="1" />
              <el-option label="禁用" :value="0" />
            </el-select>
            <el-button type="primary" :icon="Search" @click="handleProductSearch">查询</el-button>
          </div>
          <div class="product-list" v-loading="productLoading">
            <div v-if="!products.length" class="empty-tip">暂无产品，请先新增产品</div>
            <div v-for="p in products" :key="p.id"
              class="product-item" :class="{ active: selectedProduct?.id === p.id }"
              @click="selectProduct(p)">
              <div class="product-item-top">
                <span class="product-name">{{ p.name }}</span>
                <el-tag size="small" :type="p.status === 1 ? 'success' : 'info'" effect="light">
                  {{ p.status === 1 ? '启用' : '禁用' }}
                </el-tag>
              </div>
              <div class="product-item-meta">
                <span class="product-code">{{ p.code }}</span>
                <span class="product-counts">节点 {{ p.nodeCount ?? 0 }} · 物料 {{ p.materialCount ?? 0 }}</span>
              </div>
            </div>
          </div>
          <div class="wms-pagination" v-if="productTotal > 0">
            <el-pagination v-model:current-page="productPage" :page-size="productPageSize"
              :total="productTotal" :pager-count="5" small background layout="total, prev, pager, next"
              @current-change="loadProducts" />
          </div>
        </div>
      </div>

      <!-- 中间：BOM树 -->
      <div class="bom-col bom-col-center">
        <div class="panel-card bom-tree-card">
          <div class="panel-title">BOM结构</div>
          <div class="bom-tree-body" v-loading="treeLoading">
            <el-empty v-if="!selectedProduct" description="请先在左侧选择产品" :image-size="80" />
            <el-empty v-else-if="!treeData.length" description="暂无BOM节点" :image-size="80">
              <el-button type="primary" :icon="Plus" :loading="nodeSaving" @click="addRootNode">新增根节点</el-button>
            </el-empty>
            <el-tree v-else :data="treeView" node-key="id" :props="{ label: 'label', children: 'children' }"
              :current-node-key="currentNodeKey" default-expand-all highlight-current
              :expand-on-click-node="false" @node-click="onTreeClick" class="bom-tree">
              <template #default="{ data }">
                <span v-if="data.type === 'node'" class="tree-node tree-node-group">
                  <span class="tree-node-name">{{ data.raw.nodeName }}</span>
                  <el-tag v-if="data.raw.materials?.length" size="small" type="info" effect="plain">
                    {{ data.raw.materials.length }}物料
                  </el-tag>
                  <span class="tree-node-actions">
                    <el-button link type="primary" size="small" :icon="Plus"
                      @click.stop="addMaterialFor(data.raw)">物料</el-button>
                    <el-button link type="primary" size="small" :icon="Plus"
                      @click.stop="addChildFor(data.raw)">子节点</el-button>
                    <el-button link type="primary" size="small" :icon="Edit"
                      @click.stop="openNodeEditDialog(data.raw)">编辑</el-button>
                    <el-button link type="danger" size="small" :icon="Delete"
                      title="删除节点" @click.stop="confirmDeleteNode(data.raw)" />
                  </span>
                </span>
                <span v-else class="tree-node tree-material">
                  <el-tag size="small" :type="data.material.materialType === 'MAIN' ? 'success' : 'warning'" effect="light">
                    {{ data.material.materialType === 'MAIN' ? '主料' : '备料' }}
                  </el-tag>
                  <span class="tree-material-name" :title="data.material.materialSpec || data.material.materialName">{{ data.material.materialName }}</span>
                  <span class="tree-material-sku">{{ data.material.materialSku }}</span>
                  <span class="tree-material-qty">×{{ formatQty(data.material.quantity) }} {{ data.material.materialUnit }}</span>
                  <el-button link type="danger" size="small" :icon="Delete" class="tree-material-del"
                    title="移除该物料" @click.stop="removeMaterial(data.material)" />
                </span>
              </template>
            </el-tree>
          </div>
        </div>
      </div>
    </div>

    <!-- 产品新增/编辑 -->
    <el-dialog v-model="productDialogVisible" :title="productFormMode === 'add' ? '新增产品' : '编辑产品'"
      width="560px" :close-on-click-modal="false" @closed="resetProductForm">
      <el-form ref="productFormRef" :model="productForm" :rules="productRules" label-width="90px">
        <el-form-item label="产品编码" prop="code">
          <el-input v-model="productForm.code" placeholder="如：PROD-QW900" maxlength="50" show-word-limit />
        </el-form-item>
        <el-form-item label="产品名称" prop="name">
          <el-input v-model="productForm.name" placeholder="请输入产品名称" maxlength="100" />
        </el-form-item>
        <el-form-item label="规格型号" prop="spec">
          <el-input v-model="productForm.spec" placeholder="如：QW-900" maxlength="100" />
        </el-form-item>
        <el-form-item label="产品类别" prop="category">
          <el-input v-model="productForm.category" placeholder="如：控制柜" maxlength="50" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="productForm.status">
            <el-radio :label="1">启用</el-radio>
            <el-radio :label="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="productForm.remark" type="textarea" :rows="3" maxlength="500" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="productDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="productSubmitting" @click="submitProduct">保存</el-button>
      </template>
    </el-dialog>

    <!-- 节点新增/编辑 -->
    <el-dialog v-model="nodeDialogVisible" :title="nodeDialogTitle" width="480px"
      :close-on-click-modal="false" @closed="resetNodeDialogForm">
      <el-form ref="nodeFormRef" :model="nodeDialogForm" :rules="nodeRules" label-width="90px">
        <el-form-item label="节点名称" prop="nodeName">
          <el-input v-model="nodeDialogForm.nodeName" placeholder="如：主板模块" maxlength="100" />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="nodeDialogForm.remark" type="textarea" :rows="3" maxlength="500" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="nodeDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="nodeSaving" @click="submitNodeDialog">保存</el-button>
      </template>
    </el-dialog>

    <!-- 物料选择 -->
    <el-dialog v-model="materialDialogVisible" title="添加节点物料" width="720px" :close-on-click-modal="false">
      <div class="filter-left" style="margin-bottom:12px">
        <el-input v-model="materialKeyword" clearable placeholder="搜索SKU/名称/规格" style="width:260px"
          :prefix-icon="Search" @keyup.enter="loadMaterials" @clear="loadMaterials" />
        <el-radio-group v-model="materialPickType" style="margin-left:8px">
          <el-radio-button label="主料" value="MAIN" />
          <el-radio-button label="备料" value="BACKUP" />
        </el-radio-group>
        <el-button type="primary" :icon="Search" @click="loadMaterials">查询</el-button>
      </div>
      <el-table :data="materialRows" v-loading="materialTableLoading" stripe max-height="320"
        @current-change="onMaterialPick" @row-dblclick="onMaterialDblClick" highlight-current-row ref="materialTableRef">
        <el-table-column width="46">
          <template #default="{ row }">
            <el-radio :value="row.id" :model-value="pickedMaterial?.id" @change="onMaterialPick(row)"> </el-radio>
          </template>
        </el-table-column>
        <el-table-column prop="sku" label="SKU" width="130" />
        <el-table-column prop="name" label="名称" min-width="140" show-overflow-tooltip />
        <el-table-column prop="spec" label="规格" width="120" show-overflow-tooltip />
        <el-table-column prop="unit" label="单位" width="70" align="center" />
      </el-table>
      <div class="filter-left" style="margin-top:12px;align-items:flex-end">
        <span style="font-size:13px;color: var(--pw-text-secondary)">用量：</span>
        <el-input-number v-model="materialPickQty" :min="0" :precision="2" :step="1" :value-on-clear="1" controls-position="right" style="width:140px" />
      </div>
      <template #footer>
        <el-button @click="materialDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="materialSubmitting" :disabled="!pickedMaterial"
          @click="submitMaterial">确定添加</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Plus, Refresh, Search, Delete, Edit } from '@element-plus/icons-vue'
import {
  changeBomProductStatus,
  createBomProduct,
  deleteBomNode,
  deleteBomProduct,
  getBomTree,
  listBomProducts,
  removeBomNodeMaterial,
  saveBomNode,
  saveBomNodeMaterial,
  updateBomProduct,
  type BomNodeItem,
  type BomNodeMaterialItem,
  type BomProductItem,
  type BomProductStats,
} from '@/api/wms/bom'
import { listMaterials, type MaterialItem } from '@/api/wms/material'

const productLoading = ref(false)
const products = ref<BomProductItem[]>([])
const productTotal = ref(0)
const productPage = ref(1)
const productPageSize = ref(10)
const productKeyword = ref('')
const productStatusFilter = ref<number | null>(null)
const stats = ref<BomProductStats | null>(null)

const selectedProduct = ref<BomProductItem | null>(null)
const treeLoading = ref(false)
const treeData = ref<BomNodeItem[]>([])
const selectedNode = ref<BomNodeItem | null>(null)

const currentNodeKey = ref<string | undefined>(undefined)

/* 树视图：BOM 节点 + 其下物料叶子节点合并展示 */
interface BomTreeViewNode {
  id: string
  type: 'node' | 'material'
  raw?: BomNodeItem
  material?: BomNodeMaterialItem
  children?: BomTreeViewNode[]
}

function buildTreeView(nodes: BomNodeItem[]): BomTreeViewNode[] {
  return nodes.map((node) => ({
    id: `n-${node.id}`,
    type: 'node' as const,
    raw: node,
    children: [
      ...buildTreeView(node.children || []),
      ...(node.materials || []).map((m) => ({
        id: `m-${m.id}`,
        type: 'material' as const,
        raw: node,
        material: m,
      })),
    ],
  }))
}

const treeView = computed(() => buildTreeView(treeData.value))

/* ---------------- 产品 ---------------- */
async function loadProducts() {
  productLoading.value = true
  try {
    const res = await listBomProducts({
      keyword: productKeyword.value.trim() || undefined,
      status: productStatusFilter.value,
      page: productPage.value,
      pageSize: productPageSize.value,
    })
    if (res.code !== 0) {
      ElMessage.error(res.message || '加载产品失败')
      return
    }
    products.value = res.data.rows || []
    productTotal.value = res.data.total || 0
    if (res.data.stats) stats.value = res.data.stats
    if (selectedProduct.value) {
      const still = products.value.find((p) => p.id === selectedProduct.value!.id)
      if (!still) {
        selectedProduct.value = null
        treeData.value = []
        selectedNode.value = null
      }
    }
    if (!selectedProduct.value && products.value.length) {
      selectProduct(products.value[0])
    }
  } catch (err: any) {
    ElMessage.error(err?.message || '加载产品失败')
  } finally {
    productLoading.value = false
  }
}

function handleProductSearch() {
  productPage.value = 1
  loadProducts()
}

function selectProduct(p: BomProductItem) {
  selectedProduct.value = p
  selectedNode.value = null
  currentNodeKey.value = undefined
  loadTree()
}

async function loadTree() {
  if (!selectedProduct.value?.id) return
  treeLoading.value = true
  try {
    const res = await getBomTree(selectedProduct.value.id)
    if (res.code !== 0) {
      ElMessage.error(res.message || '加载BOM树失败')
      return
    }
    treeData.value = res.data || []
    if (selectedNode.value) {
      const updated = findNode(treeData.value, selectedNode.value.id!) || null
      selectedNode.value = updated
      currentNodeKey.value = updated ? `n-${updated.id}` : undefined
    } else {
      currentNodeKey.value = undefined
    }
  } catch (err: any) {
    ElMessage.error(err?.message || '加载BOM树失败')
  } finally {
    treeLoading.value = false
  }
}

function findNode(list: BomNodeItem[], id: number): BomNodeItem | null {
  for (const node of list) {
    if (node.id === id) return node
    if (node.children?.length) {
      const found = findNode(node.children, id)
      if (found) return found
    }
  }
  return null
}

/* ---------------- 节点 ---------------- */
function applyNodeSelection(node: BomNodeItem) {
  selectedNode.value = node
}

function onTreeClick(data: BomTreeViewNode) {
  if (data.type === 'node' && data.raw) {
    applyNodeSelection(data.raw)
    currentNodeKey.value = data.id
    return
  }
  if (data.type === 'material' && data.material) {
    currentNodeKey.value = data.id
    const parent = data.material.nodeId ? findNode(treeData.value, data.material.nodeId) : null
    if (parent) applyNodeSelection(parent)
  }
}

const nodeDialogForm = reactive<{ nodeName: string; remark: string }>({ nodeName: '', remark: '' })
const nodeSaving = ref(false)
const editingNode = ref<BomNodeItem | null>(null)

const nodeDialogVisible = ref(false)
const nodeDialogMode = ref<'child' | 'edit'>('child')
const nodeDialogTitle = computed(() =>
  nodeDialogMode.value === 'child' ? '新增子节点' : '编辑节点')
const nodeFormRef = ref<FormInstance>()
const nodeRules: FormRules = {
  nodeName: [{ required: true, message: '请输入节点名称', trigger: 'blur' }],
}

function openNodeDialog() {
  if (!selectedProduct.value?.id) {
    ElMessage.warning('请先选择产品')
    return
  }
  if (!selectedNode.value) {
    ElMessage.warning('请先选择父节点')
    return
  }
  nodeDialogMode.value = 'child'
  editingNode.value = null
  resetNodeDialogForm()
  nodeDialogVisible.value = true
}

/* 根节点直接使用产品名称创建 */
async function addRootNode() {
  if (!selectedProduct.value?.id) {
    ElMessage.warning('请先选择产品')
    return
  }
  nodeSaving.value = true
  try {
    const res = await saveBomNode({
      productId: selectedProduct.value.id,
      nodeName: selectedProduct.value.name,
    })
    if (res.code !== 0) {
      ElMessage.error(res.message || '创建失败')
      return
    }
    ElMessage.success('根节点已创建')
    await loadTree()
  } catch (err: any) {
    ElMessage.error(err?.message || '创建失败')
  } finally {
    nodeSaving.value = false
  }
}

function openNodeEditDialog(node: BomNodeItem) {
  if (!node.id) return
  applyNodeSelection(node)
  currentNodeKey.value = `n-${node.id}`
  nodeDialogMode.value = 'edit'
  editingNode.value = node
  resetNodeDialogForm()
  nodeDialogForm.nodeName = node.nodeName || ''
  nodeDialogForm.remark = node.remark || ''
  nodeDialogVisible.value = true
}

function resetNodeDialogForm() {
  nodeDialogForm.nodeName = ''
  nodeDialogForm.remark = ''
  nodeFormRef.value?.clearValidate()
}

async function submitNodeDialog() {
  if (!nodeFormRef.value) return
  const valid = await nodeFormRef.value.validate().catch(() => false)
  if (!valid) return
  if (!selectedProduct.value?.id) return
  const payload: any = {
    productId: selectedProduct.value.id,
    nodeName: nodeDialogForm.nodeName.trim(),
    remark: nodeDialogForm.remark,
  }
  if (nodeDialogMode.value === 'child' && selectedNode.value) {
    payload.parentId = selectedNode.value.id
  }
  if (nodeDialogMode.value === 'edit' && editingNode.value) {
    payload.id = editingNode.value.id
    payload.parentId = editingNode.value.parentId ?? null
  }
  nodeSaving.value = true
  try {
    const res = await saveBomNode(payload)
    if (res.code !== 0) {
      ElMessage.error(res.message || '保存失败')
      return
    }
    ElMessage.success('节点已保存')
    nodeDialogVisible.value = false
    await loadTree()
  } catch (err: any) {
    ElMessage.error(err?.message || '保存失败')
  } finally {
    nodeSaving.value = false
  }
}

async function confirmDeleteNode(node: BomNodeItem) {
  if (!node.id) return
  try {
    await ElMessageBox.confirm(
      `确认删除节点「${node.nodeName}」及其所有子节点与物料关联吗？`,
      '删除确认', { type: 'warning' })
  } catch {
    return
  }
  try {
    const res = await deleteBomNode(node.id)
    if (res.code !== 0) {
      ElMessage.error(res.message || '删除失败')
      return
    }
    ElMessage.success('已删除')
    if (selectedNode.value?.id === node.id) {
      selectedNode.value = null
      currentNodeKey.value = undefined
    }
    await loadTree()
  } catch (err: any) {
    ElMessage.error(err?.message || '删除失败')
  }
}

/* 树节点行内快捷操作 */
function addChildFor(node: BomNodeItem) {
  applyNodeSelection(node)
  currentNodeKey.value = node.id ? `n-${node.id}` : undefined
  openNodeDialog()
}

function addMaterialFor(node: BomNodeItem) {
  applyNodeSelection(node)
  currentNodeKey.value = node.id ? `n-${node.id}` : undefined
  openMaterialDialog()
}

/* ---------------- 节点物料 ---------------- */
const materialDialogVisible = ref(false)
const materialKeyword = ref('')
const materialPickType = ref<'MAIN' | 'BACKUP'>('MAIN')
const materialPickQty = ref(1)
const materialRows = ref<MaterialItem[]>([])
const pickedMaterial = ref<MaterialItem | null>(null)
const materialTableLoading = ref(false)
const materialSubmitting = ref(false)
const materialTableRef = ref<any>(null)

function openMaterialDialog() {
  if (!selectedNode.value?.id) {
    ElMessage.warning('请先选择节点')
    return
  }
  pickedMaterial.value = null
  materialPickQty.value = 1
  materialKeyword.value = ''
  materialDialogVisible.value = true
  loadMaterials()
}

async function loadMaterials() {
  materialTableLoading.value = true
  try {
    const res = await listMaterials({
      keyword: materialKeyword.value.trim() || undefined,
      page: 1,
      pageSize: 20,
    })
    if (res.code !== 0) {
      ElMessage.error(res.message || '加载物料失败')
      return
    }
    materialRows.value = res.data.rows || []
  } catch (err: any) {
    ElMessage.error(err?.message || '加载物料失败')
  } finally {
    materialTableLoading.value = false
  }
}

function onMaterialPick(row: MaterialItem | null) {
  if (!row) return
  pickedMaterial.value = row
}

function onMaterialDblClick(row: MaterialItem) {
  if (!row?.id) return
  pickedMaterial.value = row
  submitMaterial()
}

async function submitMaterial() {
  if (!selectedNode.value?.id || !pickedMaterial.value?.id) return
  materialSubmitting.value = true
  try {
    const res = await saveBomNodeMaterial({
      nodeId: selectedNode.value.id,
      materialId: pickedMaterial.value.id,
      materialType: materialPickType.value,
      quantity: materialPickQty.value,
    })
    if (res.code !== 0) {
      ElMessage.error(res.message || '添加失败')
      return
    }
    ElMessage.success(materialPickType.value === 'MAIN' ? '主料已设置' : '备料已添加')
    materialDialogVisible.value = false
    await loadTree()
    if (selectedNode.value?.id) selectedNode.value = findNode(treeData.value, selectedNode.value.id) || null
  } catch (err: any) {
    ElMessage.error(err?.message || '添加失败')
  } finally {
    materialSubmitting.value = false
  }
}

async function removeMaterial(m: BomNodeMaterialItem) {
  if (!m.id) return
  try {
    await ElMessageBox.confirm(`确认移除物料「${m.materialName}」吗？`, '移除确认', { type: 'warning' })
  } catch {
    return
  }
  treeLoading.value = true
  try {
    const res = await removeBomNodeMaterial(m.id)
    if (res.code !== 0) {
      ElMessage.error(res.message || '移除失败')
      return
    }
    ElMessage.success('已移除')
    const parentId = m.nodeId
    await loadTree()
    if (parentId) {
      const parent = findNode(treeData.value, parentId)
      if (parent) applyNodeSelection(parent)
    }
  } catch (err: any) {
    ElMessage.error(err?.message || '移除失败')
  } finally {
    treeLoading.value = false
  }
}

function formatQty(value?: number | string) {
  const num = Number(value ?? 0)
  return Number.isInteger(num) ? String(num) : num.toFixed(2)
}

/* ---------------- 产品表单 ---------------- */
const productDialogVisible = ref(false)
const productFormMode = ref<'add' | 'edit'>('add')
const productSubmitting = ref(false)
const productFormRef = ref<FormInstance>()
const productForm = reactive<BomProductItem>({
  code: '', name: '', spec: '', category: '', status: 1, remark: '',
})
const productRules: FormRules = {
  code: [{ required: true, message: '请输入产品编码', trigger: 'blur' }, { max: 50, message: '长度超限', trigger: 'blur' }],
  name: [{ required: true, message: '请输入产品名称', trigger: 'blur' }, { max: 100, message: '长度超限', trigger: 'blur' }],
}

function openProductDialog(mode: 'add' | 'edit', row?: BomProductItem) {
  productFormMode.value = mode
  resetProductForm()
  if (mode === 'edit' && row) {
    Object.assign(productForm, row)
  }
  productDialogVisible.value = true
}

function resetProductForm() {
  Object.assign(productForm, { id: undefined, code: '', name: '', spec: '', category: '', status: 1, remark: '' })
  productFormRef.value?.clearValidate()
}

async function submitProduct() {
  if (!productFormRef.value) return
  const valid = await productFormRef.value.validate().catch(() => false)
  if (!valid) return
  productSubmitting.value = true
  try {
    const res = productFormMode.value === 'add'
      ? await createBomProduct({ ...productForm })
      : await updateBomProduct({ ...productForm })
    if (res.code !== 0) {
      ElMessage.error(res.message || '保存失败')
      return
    }
    ElMessage.success(productFormMode.value === 'add' ? '产品已创建' : '产品已更新')
    productDialogVisible.value = false
    await loadProducts()
  } catch (err: any) {
    ElMessage.error(err?.message || '保存失败')
  } finally {
    productSubmitting.value = false
  }
}

onMounted(() => {
  loadProducts()
})
</script>

<style scoped>

.bom-layout { display: flex; gap: 16px; align-items: stretch; }
.bom-col { min-width: 0; }
.bom-col-left { flex: 0 0 38%; min-width: 300px; }
.bom-col-center { flex: 1 1 0; }

.panel-card {
  background: #fff;
  border-radius: 12px;
  padding: 16px;
  box-shadow: 0 2px 12px rgba(0,0,0,.04);
  height: 100%;
  display: flex;
  flex-direction: column;
}
.panel-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--pw-text-primary);
  margin-bottom: 12px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}
.bom-product-filter { flex-wrap: wrap; margin-bottom: 12px; }
.filter-left { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }

.product-list { flex: 1; overflow-y: auto; min-height: 200px; max-height: 560px; }
.empty-tip { color: var(--pw-text-secondary); font-size: 13px; text-align: center; padding: 40px 0; }
.product-item {
  padding: 12px;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  margin-bottom: 10px;
  cursor: pointer;
  transition: all .2s;
}
.product-item:hover { border-color: #c6e2ff; background: #f5faff; }
.product-item.active { border-color: #409eff; background: #ecf5ff; }
.product-item-top { display: flex; align-items: center; justify-content: space-between; }
.product-name { font-weight: 600; color: var(--pw-text-primary); }
.product-item-meta { display: flex; align-items: center; justify-content: space-between; margin-top: 6px; font-size: 12px; color: var(--pw-text-secondary); }
.product-code { font-family: monospace; }

.bom-tree-body { flex: 1; overflow: auto; min-height: 200px; }
.bom-tree { font-size: 14px; }
.tree-node { display: flex; align-items: center; gap: 8px; }
.tree-node-name { font-weight: 500; }
.tree-node-group .tree-node-name { color: var(--pw-text-primary); }
.tree-node-actions { display: none; align-items: center; gap: 2px; margin-left: 4px; }
.tree-node-group:hover .tree-node-actions { display: inline-flex; }
.tree-node-actions :deep(.el-button) { padding: 0 4px; height: 22px; }
.tree-material { width: 100%; }
.tree-material-name { color: var(--pw-text-regular); }
.tree-material-sku { color: var(--pw-text-secondary); font-size: 12px; font-family: monospace; }
.tree-material-qty { color: var(--pw-text-secondary); font-size: 12px; }
.tree-material-del { margin-left: auto; opacity: 0; transition: opacity .15s; }
.tree-material:hover .tree-material-del { opacity: 1; }

.wms-pagination { display: flex; justify-content: center; padding: 10px 0 0; overflow: hidden; }
.wms-pagination :deep(.el-pagination) { flex-wrap: nowrap; }
</style>
