<!--
  AppTableCard —— 列表容器卡片

  .table-card 在 11 个视图里各写一遍（4 种不同实现）。它是「筛选栏 + 表格 +
  分页」三段式的统一外壳，且必须与 styles.css 里那套固定高度布局
  （section.table-page > .table-card 纵向 flex、表格体内部滚动）配合。
  因此这里保留 table-card 类名：既吃到布局规则，又只负责外观与内边距。

  用法：
    <AppTableCard>
      <div class="filter-bar">...</div>
      <el-table ... />
      <el-pagination class="wms-pagination" ... />
    </AppTableCard>
-->
<template>
  <div class="table-card app-table-card">
    <slot />
  </div>
</template>

<style scoped>
.app-table-card {
  min-width: 0;
}
</style>
