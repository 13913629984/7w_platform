<!--
  AppPageHeader —— 页面标题区

  同一段 .sub-page-header 在 9 个视图里各写一遍，其中 3 种不同实现
  （margin-bottom 有 20px/16px 两种、对齐方式 flex-start/stretch 不一致）。
  这里统一版式，右侧操作区通过 #actions 插槽传入。

  用法：
    <AppPageHeader title="客户管理" description="维护客户档案、等级与跟进记录。">
      <template #actions>
        <el-button plain :icon="Refresh" @click="load">刷新</el-button>
        <el-button type="primary" :icon="Plus" @click="openAdd">添加</el-button>
      </template>
    </AppPageHeader>
-->
<template>
  <div class="app-page-header">
    <div class="app-page-header__main">
      <h2 class="app-page-header__title">{{ title }}</h2>
      <p v-if="description" class="app-page-header__desc">{{ description }}</p>
    </div>
    <div v-if="$slots.actions" class="app-page-header__actions">
      <slot name="actions" />
    </div>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  title: string
  description?: string
}>()
</script>

<style scoped>
.app-page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--pw-space-4);
  flex-wrap: wrap;
  margin-bottom: var(--pw-space-5);
}
.app-page-header__main {
  min-width: 0;
}
.app-page-header__title {
  font-size: var(--pw-font-xl);
  font-weight: 600;
  line-height: var(--pw-leading-tight);
  color: var(--pw-text-primary);
}
.app-page-header__desc {
  margin-top: var(--pw-space-1);
  font-size: var(--pw-font-sm);
  color: var(--pw-text-secondary);
}
.app-page-header__actions {
  display: flex;
  align-items: center;
  gap: var(--pw-space-2);
  flex-shrink: 0;
}
</style>
