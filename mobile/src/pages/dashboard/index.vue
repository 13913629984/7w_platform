<template>
  <view class="dashboard-page">
    <view class="header-section">
      <view class="header-bg"></view>
      <view class="header-content">
        <view class="top-bar">
          <view class="user-info" @tap="goToMine">
            <view class="avatar">{{ userName.charAt(0) }}</view>
            <view class="user-text">
              <view class="greeting">早上好 👋</view>
              <view class="user-name">{{ userName }}</view>
            </view>
          </view>
          <view class="header-icons">
            <view class="icon-btn" @tap="showScan">
              <text class="icon">📷</text>
            </view>
            <view class="icon-btn" @tap="showNotifications">
              <text class="icon">🔔</text>
              <view class="badge">3</view>
            </view>
          </view>
        </view>

        <view class="search-bar" @tap="handleSearch">
          <text class="search-icon">🔍</text>
          <text class="search-placeholder">搜索客户、物料、工单...</text>
        </view>
      </view>
    </view>

    <view class="content-section">
      <view class="stats-grid">
        <view
          v-for="item in stats"
          :key="item.label"
          class="stat-card"
          :class="item.color"
        >
          <view class="stat-icon-box">
            <text class="stat-icon">{{ item.icon }}</text>
          </view>
          <view class="stat-info">
            <view class="stat-value">{{ item.value }}</view>
            <view class="stat-label">{{ item.label }}</view>
          </view>
          <view class="stat-trend" :class="item.trendUp ? 'up' : 'down'">
            {{ item.trendUp ? '↑' : '↓' }} {{ item.trend }}
          </view>
        </view>
      </view>

      <view class="section">
        <view class="section-header">
          <text class="section-title">常用功能</text>
          <text class="section-more">全部 →</text>
        </view>
        <view class="quick-menu">
          <view
            v-for="item in quickMenus"
            :key="item.name"
            class="menu-item"
            @tap="handleMenuTap(item)"
          >
            <view class="menu-icon" :class="item.bg">
              <text class="icon-text">{{ item.icon }}</text>
            </view>
            <text class="menu-name">{{ item.name }}</text>
          </view>
        </view>
      </view>

      <view class="section">
        <view class="section-header">
          <text class="section-title">子系统入口</text>
        </view>
        <view class="system-list">
          <view
            v-for="system in systems"
            :key="system.name"
            class="system-card"
            :class="system.color"
            @tap="handleSystemTap(system)"
          >
            <view class="system-left">
              <view class="system-icon">
                <text class="icon-text">{{ system.icon }}</text>
              </view>
              <view class="system-info">
                <view class="system-name">{{ system.name }}</view>
                <view class="system-desc">{{ system.desc }}</view>
              </view>
            </view>
            <view class="system-right">
              <view class="system-stats">
                <view v-for="stat in system.stats" :key="stat.label" class="sys-stat">
                  <text class="sys-stat-num">{{ stat.value }}</text>
                  <text class="sys-stat-label">{{ stat.label }}</text>
                </view>
              </view>
              <text class="arrow">›</text>
            </view>
          </view>
        </view>
      </view>

      <view class="section">
        <view class="section-header">
          <text class="section-title">待办事项</text>
          <text class="section-more">查看全部</text>
        </view>
        <view class="todo-list">
          <view
            v-for="(todo, index) in todos"
            :key="index"
            class="todo-item"
            @tap="handleTodoTap(todo)"
          >
            <view class="todo-dot" :class="todo.module"></view>
            <view class="todo-content">
              <text class="todo-text">{{ todo.content }}</text>
              <text class="todo-time">{{ todo.time }}</text>
            </view>
            <text class="todo-arrow">›</text>
          </view>
        </view>
      </view>

      <view class="section">
        <view class="section-header">
          <text class="section-title">数据概览</text>
        </view>
        <view class="chart-card">
          <view class="chart-header">
            <text class="chart-title">近7日出入库趋势</text>
            <view class="chart-legend">
              <view class="legend-item">
                <view class="legend-dot inbound"></view>
                <text class="legend-text">入库</text>
              </view>
              <view class="legend-item">
                <view class="legend-dot outbound"></view>
                <text class="legend-text">出库</text>
              </view>
            </view>
          </view>
          <view class="chart-container">
            <view class="chart-bars">
              <view v-for="item in chartData" :key="item.day" class="bar-group">
                <view class="bar-pair">
                  <view class="bar inbound" :style="{ height: item.inbound + '%' }"></view>
                  <view class="bar outbound" :style="{ height: item.outbound + '%' }"></view>
                </view>
                <text class="bar-label">{{ item.day }}</text>
              </view>
            </view>
          </view>
        </view>
      </view>

      <view class="bottom-space"></view>
    </view>

    <TabBar current="/pages/dashboard/index" />
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import TabBar from '@/components/TabBar.vue'

const userName = ref('系统管理员')

const stats = ref([
  { icon: '📦', label: '库存物料', value: '12,846', trend: '3.2%', trendUp: true, color: 'blue' },
  { icon: '👥', label: '活跃客户', value: '386', trend: '18', trendUp: true, color: 'green' },
  { icon: '🔧', label: '待办工单', value: '23', trend: '5', trendUp: true, color: 'orange' },
  { icon: '💰', label: '本月销售', value: '328.5万', trend: '82%', trendUp: true, color: 'purple' }
])

const quickMenus = ref([
  { icon: '➕', name: '新增客户', bg: 'bg-blue', path: '/pages/crm/customers' },
  { icon: '📦', name: '入库登记', bg: 'bg-green', path: '/pages/wms/overview' },
  { icon: '📤', name: '出库操作', bg: 'bg-orange', path: '/pages/wms/overview' },
  { icon: '🧾', name: '新建工单', bg: 'bg-purple', path: '' },
  { icon: '🔍', name: '库存查询', bg: 'bg-cyan', path: '/pages/wms/overview' },
  { icon: '📊', name: '报表中心', bg: 'bg-pink', path: '' },
  { icon: '📝', name: '采购申请', bg: 'bg-yellow', path: '' },
  { icon: '⚙️', name: '更多', bg: 'bg-gray', path: '' }
])

const systems = ref([
  {
    name: 'WMS 仓储管理',
    icon: '📦',
    color: 'system-blue',
    desc: '入库/出库/库存/盘点',
    stats: [
      { value: '3', label: '仓库' },
      { value: '128', label: '今日单' },
      { value: '5', label: '预警' }
    ],
    path: '/pages/wms/overview'
  },
  {
    name: 'CRM 客户管理',
    icon: '👥',
    color: 'system-red',
    desc: '客户/商机/合同/回款',
    stats: [
      { value: '56', label: '商机' },
      { value: '8', label: '待审' },
      { value: '3', label: '逾期' }
    ],
    path: '/pages/crm/customers'
  },
  {
    name: '维保系统',
    icon: '🔧',
    color: 'system-green',
    desc: '设备/工单/保养/备件',
    stats: [
      { value: '23', label: '待办' },
      { value: '12', label: '保养' },
      { value: '4', label: '预警' }
    ],
    path: ''
  }
])

const todos = ref([
  { module: 'wms', content: '原材料仓库盘点计划待审批', time: '10分钟前' },
  { module: 'crm', content: '合同 HT-2026-0318 即将到期', time: '1小时前' },
  { module: 'maint', content: 'CNC-003 号机床保养任务到期', time: '2小时前' },
  { module: 'fin', content: '客户「深圳XX科技」待回款 ¥84,000', time: '3小时前' }
])

const chartData = ref([
  { day: '周一', inbound: 65, outbound: 48 },
  { day: '周二', inbound: 78, outbound: 62 },
  { day: '周三', inbound: 55, outbound: 70 },
  { day: '周四', inbound: 82, outbound: 58 },
  { day: '周五', inbound: 90, outbound: 75 },
  { day: '周六', inbound: 45, outbound: 40 },
  { day: '周日', inbound: 30, outbound: 25 }
])

function handleSearch() {
  uni.showToast({ title: '搜索功能', icon: 'none' })
}

function handleMenuTap(item: any) {
  if (item.path) {
    uni.navigateTo({ url: item.path })
  } else {
    uni.showToast({ title: item.name, icon: 'none' })
  }
}

function handleSystemTap(system: any) {
  if (system.path) {
    uni.switchTab({ url: system.path })
  } else {
    uni.showToast({ title: system.name, icon: 'none' })
  }
}

function handleTodoTap(todo: any) {
  uni.showToast({ title: todo.content, icon: 'none' })
}

function goToMine() {
  uni.switchTab({ url: '/pages/mine/index' })
}

function showScan() {
  uni.showToast({ title: '扫一扫', icon: 'none' })
}

function showNotifications() {
  uni.showToast({ title: '通知中心', icon: 'none' })
}
</script>

<style lang="scss" scoped>
.dashboard-page {
  min-height: 100vh;
  background: $bg-page;
}

.header-section {
  position: relative;
  padding-bottom: 40rpx;
}

.header-bg {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 400rpx;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-bottom-left-radius: 40rpx;
  border-bottom-right-radius: 40rpx;
}

.header-content {
  position: relative;
  z-index: 1;
  padding: calc(var(--status-bar-height) + 20rpx) $spacing-lg 0;
}

.top-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: $spacing-lg;

  .user-info {
    display: flex;
    align-items: center;

    .avatar {
      width: 88rpx;
      height: 88rpx;
      border-radius: 50%;
      background: rgba(255, 255, 255, 0.2);
      border: 2rpx solid rgba(255, 255, 255, 0.4);
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 40rpx;
      font-weight: $font-weight-bold;
      color: #fff;
      margin-right: $spacing-md;
    }

    .user-text {
      .greeting {
        font-size: $font-size-sm;
        color: rgba(255, 255, 255, 0.8);
        margin-bottom: 4rpx;
      }

      .user-name {
        font-size: $font-size-lg;
        font-weight: $font-weight-semibold;
        color: #fff;
      }
    }
  }

  .header-icons {
    display: flex;
    gap: $spacing-md;

    .icon-btn {
      width: 72rpx;
      height: 72rpx;
      background: rgba(255, 255, 255, 0.2);
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      position: relative;

      .icon {
        font-size: 36rpx;
      }

      .badge {
        position: absolute;
        top: -4rpx;
        right: -4rpx;
        min-width: 32rpx;
        height: 32rpx;
        background: #f56c6c;
        border-radius: 16rpx;
        font-size: 20rpx;
        color: #fff;
        display: flex;
        align-items: center;
        justify-content: center;
        padding: 0 8rpx;
      }
    }
  }
}

.search-bar {
  display: flex;
  align-items: center;
  height: 80rpx;
  background: rgba(255, 255, 255, 0.95);
  border-radius: 40rpx;
  padding: 0 $spacing-lg;
  box-shadow: 0 4rpx 20rpx rgba(0, 0, 0, 0.1);

  .search-icon {
    font-size: 32rpx;
    margin-right: $spacing-sm;
  }

  .search-placeholder {
    font-size: $font-size-sm;
    color: $text-placeholder;
  }
}

.content-section {
  padding: 0 $spacing-md;
  margin-top: -20rpx;
  position: relative;
  z-index: 2;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: $spacing-md;
  margin-bottom: $spacing-lg;

  .stat-card {
    background: #fff;
    border-radius: $border-radius-lg;
    padding: $spacing-lg;
    box-shadow: $shadow-sm;
    position: relative;
    overflow: hidden;

    &.blue { border-top: 6rpx solid $primary-color; }
    &.green { border-top: 6rpx solid $success-color; }
    &.orange { border-top: 6rpx solid $warning-color; }
    &.purple { border-top: 6rpx solid #764ba2; }

    .stat-icon-box {
      width: 72rpx;
      height: 72rpx;
      border-radius: 20rpx;
      display: flex;
      align-items: center;
      justify-content: center;
      margin-bottom: $spacing-sm;
    }

    &.blue .stat-icon-box { background: $primary-light; }
    &.green .stat-icon-box { background: $success-light; }
    &.orange .stat-icon-box { background: $warning-light; }
    &.purple .stat-icon-box { background: #f4f0ff; }

    .stat-icon {
      font-size: 36rpx;
    }

    .stat-info {
      margin-bottom: $spacing-xs;
    }

    .stat-value {
      font-size: $font-size-xl;
      font-weight: $font-weight-bold;
      color: $text-primary;
      margin-bottom: 4rpx;
    }

    .stat-label {
      font-size: $font-size-xs;
      color: $text-secondary;
    }

    .stat-trend {
      font-size: $font-size-xs;

      &.up { color: $success-color; }
      &.down { color: $danger-color; }
    }
  }
}

.section {
  margin-bottom: $spacing-lg;

  .section-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 0 $spacing-xs;
    margin-bottom: $spacing-md;

    .section-title {
      font-size: $font-size-lg;
      font-weight: $font-weight-semibold;
      color: $text-primary;
    }

    .section-more {
      font-size: $font-size-sm;
      color: $text-secondary;
    }
  }
}

.quick-menu {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: $spacing-md;
  background: #fff;
  border-radius: $border-radius-lg;
  padding: $spacing-lg $spacing-md;
  box-shadow: $shadow-sm;

  .menu-item {
    display: flex;
    flex-direction: column;
    align-items: center;

    .menu-icon {
      width: 96rpx;
      height: 96rpx;
      border-radius: 28rpx;
      display: flex;
      align-items: center;
      justify-content: center;
      margin-bottom: $spacing-sm;

      &.bg-blue { background: linear-gradient(135deg, #66b1ff, #409eff); }
      &.bg-green { background: linear-gradient(135deg, #85ce61, #67c23a); }
      &.bg-orange { background: linear-gradient(135deg, #f0c78a, #e6a23c); }
      &.bg-purple { background: linear-gradient(135deg, #9c88ff, #764ba2); }
      &.bg-cyan { background: linear-gradient(135deg, #5bbfd4, #2db7f5); }
      &.bg-pink { background: linear-gradient(135deg, #ff9ebb, #f56c6c); }
      &.bg-yellow { background: linear-gradient(135deg, #ffe58f, #faad14); }
      &.bg-gray { background: linear-gradient(135deg, #dcdfe6, #909399); }

      .icon-text {
        font-size: 44rpx;
      }
    }

    .menu-name {
      font-size: $font-size-xs;
      color: $text-regular;
    }
  }
}

.system-list {
  display: flex;
  flex-direction: column;
  gap: $spacing-md;

  .system-card {
    background: #fff;
    border-radius: $border-radius-lg;
    padding: $spacing-lg;
    box-shadow: $shadow-sm;
    display: flex;
    align-items: center;
    justify-content: space-between;
    border-left: 8rpx solid transparent;

    &.system-blue { border-left-color: $primary-color; }
    &.system-red { border-left-color: #f56c6c; }
    &.system-green { border-left-color: $success-color; }

    .system-left {
      display: flex;
      align-items: center;

      .system-icon {
        width: 88rpx;
        height: 88rpx;
        border-radius: 24rpx;
        display: flex;
        align-items: center;
        justify-content: center;
        margin-right: $spacing-md;
        background: $bg-page;

        .icon-text {
          font-size: 44rpx;
        }
      }

      .system-info {
        .system-name {
          font-size: $font-size-md;
          font-weight: $font-weight-semibold;
          color: $text-primary;
          margin-bottom: 4rpx;
        }

        .system-desc {
          font-size: $font-size-xs;
          color: $text-secondary;
        }
      }
    }

    .system-right {
      display: flex;
      align-items: center;
      gap: $spacing-md;

      .system-stats {
        display: flex;
        gap: $spacing-md;

        .sys-stat {
          text-align: center;

          .sys-stat-num {
            display: block;
            font-size: $font-size-md;
            font-weight: $font-weight-semibold;
            color: $text-primary;
          }

          .sys-stat-label {
            font-size: 20rpx;
            color: $text-secondary;
          }
        }
      }

      .arrow {
        font-size: $font-size-lg;
        color: $text-placeholder;
      }
    }
  }
}

.todo-list {
  background: #fff;
  border-radius: $border-radius-lg;
  box-shadow: $shadow-sm;
  overflow: hidden;

  .todo-item {
    display: flex;
    align-items: center;
    padding: $spacing-md $spacing-lg;
    border-bottom: 1rpx solid $border-color-light;

    &:last-child {
      border-bottom: none;
    }

    .todo-dot {
      width: 16rpx;
      height: 16rpx;
      border-radius: 50%;
      margin-right: $spacing-md;
      flex-shrink: 0;

      &.wms { background: $primary-color; }
      &.crm { background: #f56c6c; }
      &.maint { background: $success-color; }
      &.fin { background: #764ba2; }
    }

    .todo-content {
      flex: 1;
      min-width: 0;

      .todo-text {
        display: block;
        font-size: $font-size-sm;
        color: $text-regular;
        margin-bottom: 4rpx;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      }

      .todo-time {
        font-size: $font-size-xs;
        color: $text-secondary;
      }
    }

    .todo-arrow {
      font-size: $font-size-md;
      color: $text-placeholder;
      margin-left: $spacing-sm;
    }
  }
}

.chart-card {
  background: #fff;
  border-radius: $border-radius-lg;
  padding: $spacing-lg;
  box-shadow: $shadow-sm;

  .chart-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: $spacing-lg;

    .chart-title {
      font-size: $font-size-md;
      font-weight: $font-weight-semibold;
      color: $text-primary;
    }

    .chart-legend {
      display: flex;
      gap: $spacing-md;

      .legend-item {
        display: flex;
        align-items: center;

        .legend-dot {
          width: 16rpx;
          height: 16rpx;
          border-radius: 4rpx;
          margin-right: 8rpx;

          &.inbound { background: $primary-color; }
          &.outbound { background: $success-color; }
        }

        .legend-text {
          font-size: $font-size-xs;
          color: $text-secondary;
        }
      }
    }
  }

  .chart-container {
    height: 300rpx;

    .chart-bars {
      display: flex;
      align-items: flex-end;
      justify-content: space-between;
      height: 100%;
      padding-top: 20rpx;

      .bar-group {
        flex: 1;
        display: flex;
        flex-direction: column;
        align-items: center;

        .bar-pair {
          flex: 1;
          display: flex;
          align-items: flex-end;
          justify-content: center;
          gap: 6rpx;
          width: 100%;

          .bar {
            width: 16rpx;
            border-radius: 8rpx 8rpx 0 0;
            min-height: 8rpx;

            &.inbound {
              background: linear-gradient(180deg, #66b1ff, #409eff);
            }

            &.outbound {
              background: linear-gradient(180deg, #85ce61, #67c23a);
            }
          }
        }

        .bar-label {
          font-size: 20rpx;
          color: $text-secondary;
          margin-top: $spacing-xs;
        }
      }
    }
  }
}

.bottom-space {
  height: 40rpx;
}
</style>
