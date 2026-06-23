<template>
  <section>
    <div class="sub-page-header">
      <div>
        <h2>{{ title }}</h2>
        <p class="page-desc">{{ description }}</p>
      </div>
      <div>
        <el-button size="small" plain>导出</el-button>
        <el-button size="small" plain>刷新</el-button>
      </div>
    </div>

    <div class="stats-row">
      <div v-for="item in stats" :key="item.label" class="stat-card" :class="item.color">
        <div class="stat-header">
          <span class="stat-title">{{ item.label }}</span>
          <div class="stat-icon">{{ item.icon }}</div>
        </div>
        <div class="stat-value">{{ item.value }}</div>
        <div class="stat-footer"><span :class="item.trendType">{{ item.trend }}</span></div>
      </div>
    </div>

    <div class="table-card">
      <div class="filter-bar">
        <div class="filter-left">
          <el-input v-model="keyword" style="width: 240px" clearable placeholder="请输入关键字" prefix-icon="Search" />
          <el-select v-model="status" style="width: 140px" placeholder="状态">
            <el-option label="全部" value="" />
            <el-option label="启用" value="启用" />
            <el-option label="停用" value="停用" />
          </el-select>
        </div>
        <el-space>
          <el-button>导入</el-button>
          <el-button>导出</el-button>
          <el-button type="primary" icon="Plus">新增</el-button>
        </el-space>
      </div>

      <el-table :data="filteredRows" stripe>
        <el-table-column prop="code" label="编号" width="160" />
        <el-table-column prop="name" label="名称" min-width="180" />
        <el-table-column prop="category" label="分类" width="140" />
        <el-table-column prop="status" label="状态" width="120">
          <template #default="scope"><el-tag :type="scope.row.status === '启用' ? 'success' : 'info'">{{ scope.row.status }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="owner" label="负责人" width="140" />
        <el-table-column prop="updatedAt" label="更新时间" width="180" />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default>
            <el-button link type="primary">编辑</el-button>
            <el-button link type="primary">详情</el-button>
            <el-button link type="danger">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'

const props = defineProps<{
  title: string
  description: string
  moduleCode: string
}>()

const keyword = ref('')
const status = ref('')
const stats = [
  { label: '总记录数', value: '3,256', trend: '+128 本月新增', trendType: 'up', color: 'blue', icon: '📊' },
  { label: '启用数量', value: '2,980', trend: '91.5% 启用率', trendType: 'up', color: 'green', icon: '✅' },
  { label: '今日更新', value: '48', trend: '+12 较昨日', trendType: 'up', color: 'orange', icon: '📝' },
  { label: '异常/待处理', value: '12', trend: '-5 较昨日', trendType: 'down', color: 'purple', icon: '⚠️' },
]

const rows = [
  { code: `${props.moduleCode}-001`, name: `${props.title}示例一`, category: '核心数据', status: '启用', owner: '管理员', updatedAt: '2026-06-15 09:30' },
  { code: `${props.moduleCode}-002`, name: `${props.title}示例二`, category: '业务数据', status: '启用', owner: '张三', updatedAt: '2026-06-14 16:20' },
  { code: `${props.moduleCode}-003`, name: `${props.title}示例三`, category: '辅助数据', status: '停用', owner: '李四', updatedAt: '2026-06-13 11:08' },
  { code: `${props.moduleCode}-004`, name: `${props.title}示例四`, category: '核心数据', status: '启用', owner: '王五', updatedAt: '2026-06-12 14:45' },
]

const filteredRows = computed(() => rows.filter((row) => {
  const matchedKeyword = !keyword.value || row.name.includes(keyword.value) || row.code.includes(keyword.value)
  const matchedStatus = !status.value || row.status === status.value
  return matchedKeyword && matchedStatus
}))
</script>
