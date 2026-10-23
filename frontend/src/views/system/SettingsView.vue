<template>
  <section>
    <div class="sub-page-header">
      <div>
        <h2>系统设置</h2>
        <p class="page-desc">维护系统参数与数据字典，供各子系统统一读取。</p>
      </div>
      <el-space>
        <el-button plain :icon="Refresh" @click="reloadAll">刷新</el-button>
      </el-space>
    </div>

    <el-tabs v-model="activeTab" class="settings-tabs">
      <!-- ============ 参数配置 ============ -->
      <el-tab-pane label="参数配置" name="config">
        <div class="stats-row">
          <div class="stat-card blue">
            <div class="stat-header"><span class="stat-title">参数总数</span><div class="stat-icon">⚙️</div></div>
            <div class="stat-value">{{ configStats.total }}</div>
            <div class="stat-footer">已维护的系统参数</div>
          </div>
          <div class="stat-card green">
            <div class="stat-header"><span class="stat-title">已启用</span><div class="stat-icon">✅</div></div>
            <div class="stat-value">{{ configStats.enabled }}</div>
            <div class="stat-footer">当前生效的参数</div>
          </div>
          <div class="stat-card orange">
            <div class="stat-header"><span class="stat-title">已停用</span><div class="stat-icon">🚫</div></div>
            <div class="stat-value">{{ configStats.disabled }}</div>
            <div class="stat-footer">暂不生效的参数</div>
          </div>
          <div class="stat-card purple">
            <div class="stat-header"><span class="stat-title">分组数</span><div class="stat-icon">🗂️</div></div>
            <div class="stat-value">{{ configStats.groupCount }}</div>
            <div class="stat-footer">参数归类分组</div>
          </div>
        </div>

        <div class="table-card">
          <div class="filter-bar">
            <div class="filter-left">
              <el-input
                v-model="configKeyword"
                clearable
                placeholder="搜索参数键名 / 名称 / 说明"
                style="width: 280px"
                :prefix-icon="Search"
                @keyup.enter="loadConfigs"
                @clear="loadConfigs"
              />
              <el-select v-model="configGroupFilter" clearable placeholder="全部来源" style="width: 150px" @change="loadConfigs">
                <el-option label="全部来源" value="" />
                <el-option v-for="g in configGroups" :key="g" :label="g" :value="g" />
              </el-select>
              <el-button type="primary" :icon="Search" @click="loadConfigs">查询</el-button>
            </div>
            <el-space>
              <el-button :icon="Check" :loading="savingAll" @click="saveAllConfigs">保存全部</el-button>
              <el-button type="primary" :icon="Plus" @click="openConfigAdd">新增参数</el-button>
            </el-space>
          </div>

          <el-table v-loading="configLoading" :data="configRows" stripe border style="width: 100%" empty-text="暂无参数">
            <el-table-column prop="configKey" label="参数键名" width="200">
              <template #default="{ row }">
                <b>{{ row.configKey }}</b>
                <el-tag v-if="row.isSystem" size="small" type="info" effect="plain" style="margin-left: 6px">内置</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="configName" label="参数名称" width="160" />
            <el-table-column label="参数值" min-width="240">
              <template #default="{ row }">
                <el-switch
                  v-if="row.valueType === 'boolean'"
                  :model-value="row.configValue === 'true'"
                  @change="(v: boolean) => (row.configValue = v ? 'true' : 'false')"
                />
                <el-select v-else-if="row.valueType === 'select'" v-model="row.configValue" style="width: 100%">
                  <el-option v-for="opt in splitOptions(row.options)" :key="opt" :label="opt" :value="opt" />
                </el-select>
                <el-input v-else-if="row.valueType === 'number'" v-model="row.configValue" type="number" style="width: 100%" />
                <el-input v-else-if="row.valueType === 'textarea'" v-model="row.configValue" type="textarea" :rows="2" />
                <el-input v-else v-model="row.configValue" />
              </template>
            </el-table-column>
            <el-table-column prop="configGroup" label="分组" width="110" align="center">
              <template #default="{ row }"><el-tag effect="plain" round>{{ row.configGroup || '未分组' }}</el-tag></template>
            </el-table-column>
            <el-table-column prop="description" label="说明" min-width="200" show-overflow-tooltip />
            <el-table-column label="状态" width="90" align="center">
              <template #default="{ row }">
                <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '停用' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="140" fixed="right" align="center">
              <template #default="{ row }">
                <el-button link type="primary" @click="openConfigEdit(row)">编辑</el-button>
                <el-button link type="danger" :disabled="row.isSystem" @click="removeConfig(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
          <div class="table-tip">提示：内置参数可修改参数值，但不能删除；修改后点击「保存全部」批量提交。</div>
        </div>
      </el-tab-pane>

      <!-- ============ 数据字典 ============ -->
      <el-tab-pane label="数据字典" name="dict">
        <div class="stats-row">
          <div class="stat-card blue">
            <div class="stat-header"><span class="stat-title">字典类型</span><div class="stat-icon">📚</div></div>
            <div class="stat-value">{{ dictStats.total }}</div>
            <div class="stat-footer">已定义的字典分类</div>
          </div>
          <div class="stat-card green">
            <div class="stat-header"><span class="stat-title">字典项总数</span><div class="stat-icon">🔖</div></div>
            <div class="stat-value">{{ dictStats.itemTotal }}</div>
            <div class="stat-footer">所有字典下的条目数</div>
          </div>
          <div class="stat-card orange">
            <div class="stat-header"><span class="stat-title">已启用类型</span><div class="stat-icon">✅</div></div>
            <div class="stat-value">{{ dictStats.enabled }}</div>
            <div class="stat-footer">正在使用的字典</div>
          </div>
          <div class="stat-card purple">
            <div class="stat-header"><span class="stat-title">内置字典</span><div class="stat-icon">🔒</div></div>
            <div class="stat-value">{{ dictStats.systemCount }}</div>
            <div class="stat-footer">系统预置不可删除</div>
          </div>
        </div>

        <div class="dict-layout">
          <div class="table-card dict-types">
            <div class="filter-bar">
              <el-input
                v-model="dictKeyword"
                clearable
                placeholder="搜索字典编码 / 名称"
                style="width: 200px"
                :prefix-icon="Search"
                @keyup.enter="loadDictTypes"
                @clear="loadDictTypes"
              />
              <el-button type="primary" size="small" :icon="Plus" @click="openTypeAdd">新增</el-button>
            </div>
            <el-table
              v-loading="dictLoading"
              :data="dictTypes"
              highlight-current-row
              border
              style="width: 100%"
              empty-text="暂无字典"
              @current-change="handleTypeSelect"
            >
              <el-table-column prop="dictName" label="字典名称" min-width="120">
                <template #default="{ row }">
                  <b>{{ row.dictName }}</b>
                  <el-tag v-if="row.isSystem" size="small" type="info" effect="plain" style="margin-left: 4px">内置</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="dictCode" label="编码" width="180" show-overflow-tooltip />
              <el-table-column prop="itemCount" label="项数" width="70" align="center" />
              <el-table-column label="操作" width="110" align="center">
                <template #default="{ row }">
                  <el-button link type="primary" @click.stop="openTypeEdit(row)">编辑</el-button>
                  <el-button link type="danger" :disabled="row.isSystem" @click.stop="removeType(row)">删</el-button>
                </template>
              </el-table-column>
            </el-table>
          </div>

          <div class="table-card dict-items">
            <div class="filter-bar">
              <div class="filter-left">
                <span class="dict-current">
                  当前字典：<b>{{ currentType ? currentType.dictName : '未选择' }}</b>
                  <span v-if="currentType" class="dict-code">{{ currentType.dictCode }}</span>
                </span>
              </div>
              <el-button type="primary" size="small" :icon="Plus" :disabled="!currentType" @click="openItemAdd">新增字典项</el-button>
            </div>
            <el-table v-loading="itemLoading" :data="dictItems" border style="width: 100%" :empty-text="currentType ? '该字典下暂无字典项' : '请先在左侧选择字典'">
              <el-table-column prop="itemLabel" label="标签" min-width="120" />
              <el-table-column prop="itemValue" label="值" min-width="120" />
              <el-table-column label="配色" width="90" align="center">
                <template #default="{ row }">
                  <el-tag :type="tagType(row.color)" size="small">{{ row.color || 'primary' }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="sort" label="排序" width="70" align="center" />
              <el-table-column label="状态" width="80" align="center">
                <template #default="{ row }">
                  <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">{{ row.status === 1 ? '启用' : '停用' }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="remark" label="备注" min-width="140" show-overflow-tooltip />
              <el-table-column label="操作" width="120" align="center">
                <template #default="{ row }">
                  <el-button link type="primary" @click="openItemEdit(row)">编辑</el-button>
                  <el-button link type="danger" @click="removeItem(row)">删除</el-button>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </div>
      </el-tab-pane>
    </el-tabs>

    <!-- 参数编辑弹窗 -->
    <el-dialog v-model="configDialogVisible" :title="configForm.id ? '编辑参数' : '新增参数'" width="640px" :close-on-click-modal="false">
      <el-form ref="configFormRef" :model="configForm" :rules="configRules" label-width="100px">
        <el-form-item label="参数键名" prop="configKey">
          <el-input v-model="configForm.configKey" :disabled="!!configForm.id" placeholder="如 sys.name，创建后不可修改" maxlength="80" />
        </el-form-item>
        <el-form-item label="参数名称" prop="configName">
          <el-input v-model="configForm.configName" maxlength="100" />
        </el-form-item>
        <el-form-item label="值类型">
          <el-select v-model="configForm.valueType" style="width: 100%">
            <el-option label="文本" value="text" />
            <el-option label="数字" value="number" />
            <el-option label="布尔" value="boolean" />
            <el-option label="下拉选择" value="select" />
            <el-option label="多行文本" value="textarea" />
          </el-select>
        </el-form-item>
        <el-form-item label="参数值">
          <el-select v-if="configForm.valueType === 'select'" v-model="configForm.configValue" style="width: 100%" allow-create filterable>
            <el-option v-for="opt in splitOptions(configForm.options)" :key="opt" :label="opt" :value="opt" />
          </el-select>
          <el-switch
            v-else-if="configForm.valueType === 'boolean'"
            :model-value="configForm.configValue === 'true'"
            @change="(v: boolean) => (configForm.configValue = v ? 'true' : 'false')"
          />
          <el-input v-else v-model="configForm.configValue" maxlength="500" />
        </el-form-item>
        <el-form-item v-if="configForm.valueType === 'select'" label="可选项">
          <el-input v-model="configForm.options" placeholder="逗号分隔，如 10,20,50,100" />
        </el-form-item>
        <el-form-item label="分组">
          <el-select v-model="configForm.configGroup" style="width: 100%" allow-create filterable>
            <el-option v-for="g in configGroupOptions" :key="g" :label="g" :value="g" />
          </el-select>
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="configForm.sort" :min="0" :step="10" controls-position="right" style="width: 100%" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="configForm.status" :active-value="1" :inactive-value="0" active-text="启用" inactive-text="停用" />
        </el-form-item>
        <el-form-item label="说明">
          <el-input v-model="configForm.description" type="textarea" :rows="2" maxlength="500" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="configDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="configSubmitting" @click="submitConfig">保存</el-button>
      </template>
    </el-dialog>

    <!-- 字典类型弹窗 -->
    <el-dialog v-model="typeDialogVisible" :title="typeForm.id ? '编辑字典' : '新增字典'" width="520px" :close-on-click-modal="false">
      <el-form ref="typeFormRef" :model="typeForm" :rules="typeRules" label-width="90px">
        <el-form-item label="字典编码" prop="dictCode">
          <el-input v-model="typeForm.dictCode" :disabled="!!typeForm.id" placeholder="如 crm_lead_status，创建后不可修改" maxlength="80" />
        </el-form-item>
        <el-form-item label="字典名称" prop="dictName">
          <el-input v-model="typeForm.dictName" maxlength="100" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="typeForm.sort" :min="0" :step="10" controls-position="right" style="width: 100%" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="typeForm.status" :active-value="1" :inactive-value="0" active-text="启用" inactive-text="停用" />
        </el-form-item>
        <el-form-item label="说明">
          <el-input v-model="typeForm.description" type="textarea" :rows="2" maxlength="500" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="typeDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="typeSubmitting" @click="submitType">保存</el-button>
      </template>
    </el-dialog>

    <!-- 字典项弹窗 -->
    <el-dialog v-model="itemDialogVisible" :title="itemForm.id ? '编辑字典项' : '新增字典项'" width="520px" :close-on-click-modal="false">
      <el-form ref="itemFormRef" :model="itemForm" :rules="itemRules" label-width="90px">
        <el-form-item label="字典">
          <el-input :model-value="itemForm.dictCode" disabled />
        </el-form-item>
        <el-form-item label="标签" prop="itemLabel">
          <el-input v-model="itemForm.itemLabel" maxlength="100" />
        </el-form-item>
        <el-form-item label="值" prop="itemValue">
          <el-input v-model="itemForm.itemValue" maxlength="100" />
        </el-form-item>
        <el-form-item label="配色">
          <el-select v-model="itemForm.color" style="width: 100%">
            <el-option label="primary（蓝）" value="primary" />
            <el-option label="success（绿）" value="success" />
            <el-option label="warning（橙）" value="warning" />
            <el-option label="danger（红）" value="danger" />
            <el-option label="info（灰）" value="info" />
          </el-select>
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="itemForm.sort" :min="0" :step="10" controls-position="right" style="width: 100%" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="itemForm.status" :active-value="1" :inactive-value="0" active-text="启用" inactive-text="停用" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="itemForm.remark" type="textarea" :rows="2" maxlength="500" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="itemDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="itemSubmitting" @click="submitItem">保存</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Check, Plus, Refresh, Search } from '@element-plus/icons-vue'
import {
  batchSaveConfigs,
  createConfig,
  deleteConfig,
  listConfigs,
  updateConfig,
  type ConfigItem,
  type ConfigStats,
} from '@/api/system/config'
import {
  createDictItem,
  createDictType,
  deleteDictItem,
  deleteDictType,
  listDictItems,
  listDictTypes,
  updateDictItem,
  updateDictType,
  type DictItem,
  type DictStats,
  type DictTypeItem,
} from '@/api/system/dict'

const activeTab = ref<'config' | 'dict'>('config')

/* ---------------- 参数配置 ---------------- */
const configLoading = ref(false)
const configSubmitting = ref(false)
const savingAll = ref(false)
const configRows = ref<ConfigItem[]>([])
const configGroups = ref<string[]>([])
const configStats = ref<ConfigStats>({ total: 0, enabled: 0, disabled: 0, groupCount: 0 })
const configKeyword = ref('')
const configGroupFilter = ref('')
const configDialogVisible = ref(false)
const configFormRef = ref<FormInstance>()
const configForm = reactive<ConfigItem>(emptyConfig())

const configRules: FormRules = {
  configKey: [{ required: true, message: '请填写参数键名', trigger: 'blur' }],
  configName: [{ required: true, message: '请填写参数名称', trigger: 'blur' }],
}

const configGroupOptions = computed(() => {
  const base = ['基础设置', '外观设置', '安全设置', '文件设置', '通知设置']
  const merged = new Set([...base, ...configGroups.value])
  return Array.from(merged)
})

function emptyConfig(): ConfigItem {
  return {
    configKey: '',
    configName: '',
    configValue: '',
    valueType: 'text',
    options: '',
    configGroup: '基础设置',
    description: '',
    sort: 0,
    status: 1,
  }
}

function splitOptions(options?: string) {
  if (!options) return []
  return options.split(',').map((o) => o.trim()).filter(Boolean)
}

async function loadConfigs() {
  configLoading.value = true
  try {
    const res = await listConfigs({
      keyword: configKeyword.value.trim() || undefined,
      group: configGroupFilter.value || undefined,
    })
    if (res.code !== 0) {
      ElMessage.error(res.message || '加载参数失败')
      return
    }
    configRows.value = res.data.rows || []
    configStats.value = res.data.stats || { total: 0, enabled: 0, disabled: 0, groupCount: 0 }
    // 分组下拉只取全量分组，避免筛选后自身选项消失
    if (!configKeyword.value && !configGroupFilter.value) {
      configGroups.value = Object.keys(res.data.groups || {})
    }
  } catch (error: any) {
    ElMessage.error(error?.message || '加载参数失败')
  } finally {
    configLoading.value = false
  }
}

function openConfigAdd() {
  Object.assign(configForm, emptyConfig())
  configFormRef.value?.clearValidate()
  configDialogVisible.value = true
}

function openConfigEdit(row: ConfigItem) {
  Object.assign(configForm, emptyConfig(), row)
  configFormRef.value?.clearValidate()
  configDialogVisible.value = true
}

async function submitConfig() {
  if (!configFormRef.value) return
  const valid = await configFormRef.value.validate().catch(() => false)
  if (!valid) return
  configSubmitting.value = true
  try {
    const res = configForm.id ? await updateConfig({ ...configForm }) : await createConfig({ ...configForm })
    if (res.code !== 0) {
      ElMessage.error(res.message || '保存失败')
      return
    }
    ElMessage.success(configForm.id ? '参数已更新' : '参数已创建')
    configDialogVisible.value = false
    await loadConfigs()
  } catch (error: any) {
    ElMessage.error(error?.message || '保存失败')
  } finally {
    configSubmitting.value = false
  }
}

async function saveAllConfigs() {
  savingAll.value = true
  try {
    const payload = configRows.value
      .filter((row) => row.id)
      .map((row) => ({ id: row.id, configValue: row.configValue } as ConfigItem))
    const res = await batchSaveConfigs(payload)
    if (res.code !== 0) {
      ElMessage.error(res.message || '保存失败')
      return
    }
    ElMessage.success(`已保存 ${res.data} 项参数`)
    await loadConfigs()
  } catch (error: any) {
    ElMessage.error(error?.message || '保存失败')
  } finally {
    savingAll.value = false
  }
}

async function removeConfig(row: ConfigItem) {
  if (!row.id) return
  try {
    await ElMessageBox.confirm(`确认删除参数 ${row.configName} 吗？`, '删除确认', { type: 'warning' })
  } catch {
    return
  }
  try {
    const res = await deleteConfig(row.id)
    if (res.code !== 0) {
      ElMessage.error(res.message || '删除失败')
      return
    }
    ElMessage.success('参数已删除')
    await loadConfigs()
  } catch (error: any) {
    ElMessage.error(error?.message || '删除失败')
  }
}

/* ---------------- 数据字典 ---------------- */
const dictLoading = ref(false)
const itemLoading = ref(false)
const typeSubmitting = ref(false)
const itemSubmitting = ref(false)
const dictTypes = ref<DictTypeItem[]>([])
const dictItems = ref<DictItem[]>([])
const dictStats = ref<DictStats>({ total: 0, enabled: 0, itemTotal: 0, systemCount: 0 })
const dictKeyword = ref('')
const currentType = ref<DictTypeItem | null>(null)
const typeDialogVisible = ref(false)
const itemDialogVisible = ref(false)
const typeFormRef = ref<FormInstance>()
const itemFormRef = ref<FormInstance>()
const typeForm = reactive<DictTypeItem>(emptyType())
const itemForm = reactive<DictItem>(emptyItem())

const typeRules: FormRules = {
  dictCode: [{ required: true, message: '请填写字典编码', trigger: 'blur' }],
  dictName: [{ required: true, message: '请填写字典名称', trigger: 'blur' }],
}
const itemRules: FormRules = {
  itemLabel: [{ required: true, message: '请填写字典标签', trigger: 'blur' }],
  itemValue: [{ required: true, message: '请填写字典值', trigger: 'blur' }],
}

function emptyType(): DictTypeItem {
  return { dictCode: '', dictName: '', description: '', sort: 0, status: 1 }
}

function emptyItem(): DictItem {
  return { dictCode: '', itemValue: '', itemLabel: '', color: 'primary', sort: 0, status: 1, remark: '' }
}

function tagType(color?: string) {
  const allowed = ['primary', 'success', 'warning', 'danger', 'info']
  return allowed.includes(color || '') ? (color as any) : 'primary'
}

async function loadDictTypes() {
  dictLoading.value = true
  try {
    const res = await listDictTypes({ keyword: dictKeyword.value.trim() || undefined })
    if (res.code !== 0) {
      ElMessage.error(res.message || '加载字典失败')
      return
    }
    dictTypes.value = res.data.rows || []
    dictStats.value = res.data.stats || { total: 0, enabled: 0, itemTotal: 0, systemCount: 0 }
    // 默认选中第一个，方便直接看字典项
    if (!currentType.value && dictTypes.value.length) {
      handleTypeSelect(dictTypes.value[0])
    }
  } catch (error: any) {
    ElMessage.error(error?.message || '加载字典失败')
  } finally {
    dictLoading.value = false
  }
}

async function handleTypeSelect(row?: DictTypeItem | null) {
  if (!row) return
  currentType.value = row
  itemLoading.value = true
  try {
    const res = await listDictItems(row.dictCode)
    if (res.code !== 0) {
      ElMessage.error(res.message || '加载字典项失败')
      return
    }
    dictItems.value = res.data || []
  } catch (error: any) {
    ElMessage.error(error?.message || '加载字典项失败')
  } finally {
    itemLoading.value = false
  }
}

function openTypeAdd() {
  Object.assign(typeForm, emptyType())
  typeFormRef.value?.clearValidate()
  typeDialogVisible.value = true
}

function openTypeEdit(row: DictTypeItem) {
  Object.assign(typeForm, emptyType(), row)
  typeFormRef.value?.clearValidate()
  typeDialogVisible.value = true
}

async function submitType() {
  if (!typeFormRef.value) return
  const valid = await typeFormRef.value.validate().catch(() => false)
  if (!valid) return
  typeSubmitting.value = true
  try {
    const res = typeForm.id ? await updateDictType({ ...typeForm }) : await createDictType({ ...typeForm })
    if (res.code !== 0) {
      ElMessage.error(res.message || '保存失败')
      return
    }
    ElMessage.success(typeForm.id ? '字典已更新' : '字典已创建')
    typeDialogVisible.value = false
    await loadDictTypes()
  } catch (error: any) {
    ElMessage.error(error?.message || '保存失败')
  } finally {
    typeSubmitting.value = false
  }
}

async function removeType(row: DictTypeItem) {
  if (!row.id) return
  try {
    await ElMessageBox.confirm(
      `确认删除字典「${row.dictName}」吗？其下所有字典项将一并删除。`,
      '删除确认',
      { type: 'warning' },
    )
  } catch {
    return
  }
  try {
    const res = await deleteDictType(row.id)
    if (res.code !== 0) {
      ElMessage.error(res.message || '删除失败')
      return
    }
    ElMessage.success('字典已删除')
    if (currentType.value?.id === row.id) {
      currentType.value = null
      dictItems.value = []
    }
    await loadDictTypes()
  } catch (error: any) {
    ElMessage.error(error?.message || '删除失败')
  }
}

function openItemAdd() {
  if (!currentType.value) return
  Object.assign(itemForm, emptyItem(), { dictCode: currentType.value.dictCode })
  itemFormRef.value?.clearValidate()
  itemDialogVisible.value = true
}

function openItemEdit(row: DictItem) {
  Object.assign(itemForm, emptyItem(), row)
  itemFormRef.value?.clearValidate()
  itemDialogVisible.value = true
}

async function submitItem() {
  if (!itemFormRef.value) return
  const valid = await itemFormRef.value.validate().catch(() => false)
  if (!valid) return
  itemSubmitting.value = true
  try {
    const res = itemForm.id ? await updateDictItem({ ...itemForm }) : await createDictItem({ ...itemForm })
    if (res.code !== 0) {
      ElMessage.error(res.message || '保存失败')
      return
    }
    ElMessage.success(itemForm.id ? '字典项已更新' : '字典项已创建')
    itemDialogVisible.value = false
    if (currentType.value) await handleTypeSelect(currentType.value)
    await loadDictTypes()
  } catch (error: any) {
    ElMessage.error(error?.message || '保存失败')
  } finally {
    itemSubmitting.value = false
  }
}

async function removeItem(row: DictItem) {
  if (!row.id) return
  try {
    await ElMessageBox.confirm(`确认删除字典项「${row.itemLabel}」吗？`, '删除确认', { type: 'warning' })
  } catch {
    return
  }
  try {
    const res = await deleteDictItem(row.id)
    if (res.code !== 0) {
      ElMessage.error(res.message || '删除失败')
      return
    }
    ElMessage.success('字典项已删除')
    if (currentType.value) await handleTypeSelect(currentType.value)
    await loadDictTypes()
  } catch (error: any) {
    ElMessage.error(error?.message || '删除失败')
  }
}

async function reloadAll() {
  await Promise.all([loadConfigs(), loadDictTypes()])
  ElMessage.success('已刷新')
}

onMounted(() => {
  loadConfigs()
  loadDictTypes()
})
</script>

<style scoped>
.settings-tabs {
  margin-top: 4px;
}

.dict-layout {
  display: grid;
  grid-template-columns: minmax(420px, 5fr) 7fr;
  gap: 16px;
  align-items: start;
}

.dict-current {
  font-size: 13px;
  color: #475569;
}

.dict-code {
  margin-left: 8px;
  padding: 1px 6px;
  border-radius: 4px;
  background: #eef2f7;
  color: #64748b;
  font-size: 12px;
}

.table-tip {
  padding: 10px 2px 2px;
  font-size: 12px;
  color: #94a3b8;
}

@media (max-width: 1280px) {
  .dict-layout {
    grid-template-columns: 1fr;
  }
}
</style>
