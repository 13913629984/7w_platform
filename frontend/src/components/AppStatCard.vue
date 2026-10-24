<!--
  AppStatCard —— 统计卡

  背景：这套结构（stat-card > stat-header > stat-value > stat-footer）在
  35 个视图里被复制粘贴，其中 10 个视图还在 scoped CSS 里把样式又抄了一遍
  （.stat-card / .stat-value / .stat-icon 等），与全局 styles.css 同名重复定义，
  覆盖关系只能靠注释维系，改一次配色要动 12 个文件。

  设计取舍：
  - 样式完全 scoped 自带，不依赖全局 styles.css。这样全局那份旧规则可安全保留
    （仍有 19 个采用非标准结构的视图没法立刻迁移，它们继续用全局样式），
    新旧两套互不干扰。
  - 色调用 accent 语义枚举（而非 blue/green 这种色相名），换配色不影响模板语义。
  - 插槽覆盖 #icon / #footer，容纳少量特例（带单位的小字标注、带状态点的脚注）。

  用法：
    <AppStatCard label="客户总数" :value="stats.total" accent="blue" icon="👥" footer="全部客户" />
    <AppStatCard label="库存总量" :value="total" accent="teal">
      <template #footer><span class="up">↑ 3.2%</span></template>
    </AppStatCard>
-->
<template>
  <div class="app-stat" :class="`is-${accent}`">
    <div class="app-stat__header">
      <span class="app-stat__label">{{ label }}</span>
      <div class="app-stat__icon">
        <slot name="icon">{{ icon }}</slot>
      </div>
    </div>
    <div class="app-stat__value">
      <slot>
        <span class="app-stat__num">{{ value }}</span>
        <small v-if="unit" class="app-stat__unit">{{ unit }}</small>
      </slot>
    </div>
    <div v-if="footer || $slots.footer" class="app-stat__footer">
      <slot name="footer">{{ footer }}</slot>
    </div>
  </div>
</template>

<script setup lang="ts">
export type StatAccent = 'blue' | 'green' | 'orange' | 'purple' | 'red' | 'teal'

withDefaults(
  defineProps<{
    /** 统计项名称 */
    label: string
    /** 主数值；不传时通过默认插槽自定义（如「¥328.5万」） */
    value?: string | number
    /** 数值后缀，如「种」「个客户」 */
    unit?: string
    /** 主数值下方说明文字 */
    footer?: string
    /** 右侧图标（emoji 或 Element 图标组件名） */
    icon?: string
    /** 色调语义 */
    accent?: StatAccent
  }>(),
  { accent: 'blue' },
)
</script>

<style scoped>
.app-stat {
  position: relative;
  overflow: hidden;
  background: var(--pw-bg-surface);
  border-radius: var(--pw-radius-lg);
  padding: var(--pw-space-6);
  box-shadow: var(--pw-shadow-md);
  transition: transform var(--pw-duration-base) var(--pw-ease),
    box-shadow var(--pw-duration-base) var(--pw-ease);
}
.app-stat:hover {
  transform: translateY(-2px);
  box-shadow: var(--pw-shadow-lg);
}
/* 顶部 3px 色条：装饰性，仅区分卡片分组，不承载状态语义 */
.app-stat::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 3px;
}
.app-stat.is-blue::before { background: linear-gradient(90deg, var(--pw-blue-500), var(--pw-blue-600)); }
.app-stat.is-green::before { background: linear-gradient(90deg, var(--pw-green-600), var(--pw-green-400)); }
.app-stat.is-orange::before { background: linear-gradient(90deg, var(--pw-amber-700), var(--pw-amber-400)); }
.app-stat.is-purple::before { background: linear-gradient(90deg, var(--pw-violet-500), var(--pw-blue-500)); }
.app-stat.is-red::before { background: linear-gradient(90deg, var(--pw-red-600), var(--pw-red-400)); }
.app-stat.is-teal::before { background: linear-gradient(90deg, var(--pw-teal-500), #5cdbd3); }

.app-stat__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--pw-space-3);
  margin-bottom: var(--pw-space-3);
}
.app-stat__label {
  flex: 1;
  min-width: 0;
  font-size: var(--pw-font-base);
  color: var(--pw-text-secondary);
  white-space: normal;
  word-break: break-word;
}
.app-stat__icon {
  width: 40px;
  height: 40px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--pw-radius-md);
  font-size: var(--pw-font-xl);
  line-height: 1;
}
.app-stat.is-blue .app-stat__icon { background: var(--pw-bg-active); color: var(--pw-blue-500); }
.app-stat.is-green .app-stat__icon { background: var(--pw-green-subtle); color: var(--pw-green-400); }
.app-stat.is-orange .app-stat__icon { background: var(--pw-amber-subtle); color: var(--pw-amber-400); }
.app-stat.is-purple .app-stat__icon { background: var(--pw-violet-subtle); color: var(--pw-violet-500); }
.app-stat.is-red .app-stat__icon { background: var(--pw-red-subtle); color: var(--pw-red-400); }
.app-stat.is-teal .app-stat__icon { background: var(--pw-teal-subtle); color: var(--pw-teal-500); }

.app-stat__value {
  margin-bottom: var(--pw-space-2);
  font-size: var(--pw-font-3xl);
  font-weight: 700;
  line-height: var(--pw-leading-tight);
  color: var(--pw-text-primary);
  font-variant-numeric: tabular-nums;
}
.app-stat__unit {
  margin-left: 2px;
  font-size: var(--pw-font-base);
  font-weight: 500;
  color: var(--pw-text-secondary);
}
.app-stat__footer {
  font-size: var(--pw-font-xs);
  color: var(--pw-text-secondary);
}
/* 脚注里的涨跌语义色（沿用全局 .up / .down 约定，避免各视图各写一套） */
.app-stat__footer :deep(.up) { color: var(--pw-text-success); }
.app-stat__footer :deep(.down) { color: var(--pw-text-danger); }
</style>
