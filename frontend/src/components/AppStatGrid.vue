<!--
  AppStatGrid —— 统计卡栅格容器

  各视图原本各自写了一份 .stats-row，列宽阈值有 200/220/260 三种，
  间距有 16/20 两种，语义上并无差别。这里统一为「按内容最小宽度自动换行列」，
  用 min 属性暴露极少数确需更宽卡片的场景。

  用法：
    <AppStatGrid>
      <AppStatCard ... />
    </AppStatGrid>
    <AppStatGrid :min="260"> ... </AppStatGrid>
-->
<template>
  <div class="app-stat-grid" :style="{ '--app-stat-min': `${min}px` }">
    <slot />
  </div>
</template>

<script setup lang="ts">
withDefaults(
  defineProps<{
    /** 单卡最小宽度，低于该宽度自动换行 */
    min?: number
  }>(),
  { min: 220 },
)
</script>

<style scoped>
.app-stat-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(var(--app-stat-min), 1fr));
  gap: var(--pw-space-5);
  margin-bottom: var(--pw-space-6);
}
@media (max-width: 768px) {
  .app-stat-grid {
    grid-template-columns: 1fr;
  }
}
</style>
