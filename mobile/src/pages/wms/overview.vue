<template>
  <view class="wms-page">
    <view class="header-section">
      <view class="header-bg"></view>
      <view class="header-content">
        <view class="page-title">库存概览</view>
        <view class="header-stats">
          <view class="stat-item">
            <text class="stat-value">{{ stats.totalQty }}</text>
            <text class="stat-label">库存总量(件)</text>
          </view>
          <view class="stat-item">
            <text class="stat-value">{{ stats.materialCount }}</text>
            <text class="stat-label">物料种类</text>
          </view>
        </view>
      </view>
    </view>

    <view class="content-section">
      <view class="stats-cards">
        <view class="stat-card blue">
          <view class="stat-icon">📦</view>
          <view class="stat-info">
            <text class="stat-num">{{ stats.materialTotal }}</text>
            <text class="stat-name">物料总数</text>
          </view>
          <view class="stat-trend up">+{{ stats.monthAdd }} 本月新增</view>
        </view>
        <view class="stat-card green">
          <view class="stat-icon">⬇️</view>
          <view class="stat-info">
            <text class="stat-num">{{ stats.inboundToday }}</text>
            <text class="stat-name">今日入库</text>
          </view>
          <view class="stat-trend up">↑ {{ stats.inboundDelta }} 较昨日</view>
        </view>
        <view class="stat-card orange">
          <view class="stat-icon">⬆️</view>
          <view class="stat-info">
            <text class="stat-num">{{ stats.outboundToday }}</text>
            <text class="stat-name">今日出库</text>
          </view>
          <view class="stat-trend down">↓ {{ stats.outboundDelta }} 较昨日</view>
        </view>
        <view class="stat-card red">
          <view class="stat-icon">⚠️</view>
          <view class="stat-info">
            <text class="stat-num">{{ stats.warningCount }}</text>
            <text class="stat-name">库存预警</text>
          </view>
          <view class="stat-trend down">{{ stats.warningLow }}低库存</view>
        </view>
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

      <view class="section-card">
        <view class="card-header">
          <text class="card-title">库存分布</text>
          <text class="card-more">详情 →</text>
        </view>
        <view class="donut-section">
          <view class="donut-chart">
            <view class="donut-ring"></view>
            <view class="donut-center">
              <text class="donut-value">{{ stats.totalQty }}</text>
              <text class="donut-label">总库存</text>
            </view>
          </view>
          <view class="donut-legend">
            <view
              v-for="item in distribution"
              :key="item.name"
              class="legend-item"
            >
              <view class="legend-dot" :style="{ background: item.color }"></view>
              <text class="legend-name">{{ item.name }}</text>
              <text class="legend-value">{{ item.quantity }}件</text>
            </view>
          </view>
        </view>
      </view>

      <view class="section-card">
        <view class="card-header">
          <text class="card-title">近7日出入库趋势</text>
          <view class="chart-tabs">
            <view
              class="tab-item"
              :class="{ active: chartTab === 'inbound' }"
              @tap="chartTab = 'inbound'"
            >入库</view>
            <view
              class="tab-item"
              :class="{ active: chartTab === 'outbound' }"
              @tap="chartTab = 'outbound'"
            >出库</view>
          </view>
        </view>
        <view class="line-chart">
          <view class="chart-area">
            <view
              v-for="item in chartData"
              :key="item.day"
              class="chart-bar-wrap"
            >
              <view
                class="chart-bar"
                :class="chartTab"
                :style="{ height: (chartTab === 'inbound' ? item.inbound : item.outbound) + '%' }"
              ></view>
              <text class="bar-label">{{ item.day }}</text>
            </view>
          </view>
        </view>
      </view>

      <view class="section-card">
        <view class="card-header">
          <text class="card-title">待入库列表</text>
          <text class="card-more">全部 →</text>
        </view>
        <view class="order-list">
          <view
            v-for="order in pendingInbound"
            :key="order.id"
            class="order-item"
          >
            <view class="order-header">
              <text class="order-code">{{ order.code }}</text>
              <view class="order-status" :class="'status-' + order.status">
                {{ order.statusText }}
              </view>
            </view>
            <view class="order-body">
              <view class="order-info">
                <text class="info-label">供应商</text>
                <text class="info-value">{{ order.supplier }}</text>
              </view>
              <view class="order-info">
                <text class="info-label">仓库</text>
                <text class="info-value">{{ order.warehouse }}</text>
              </view>
              <view class="order-info">
                <text class="info-label">数量</text>
                <text class="info-value highlight">{{ order.totalQty }} 件</text>
              </view>
            </view>
          </view>
        </view>
      </view>

      <view class="section-card">
        <view class="card-header">
          <text class="card-title">待出库列表</text>
          <text class="card-more">全部 →</text>
        </view>
        <view class="order-list">
          <view
            v-for="order in pendingOutbound"
            :key="order.id"
            class="order-item"
          >
            <view class="order-header">
              <text class="order-code">{{ order.code }}</text>
              <view class="order-status" :class="'status-' + order.status">
                {{ order.statusText }}
              </view>
            </view>
            <view class="order-body">
              <view class="order-info">
                <text class="info-label">客户</text>
                <text class="info-value">{{ order.customer }}</text>
              </view>
              <view class="order-info">
                <text class="info-label">仓库</text>
                <text class="info-value">{{ order.warehouse }}</text>
              </view>
              <view class="order-info">
                <text class="info-label">数量</text>
                <text class="info-value highlight">{{ order.totalQty }} 件</text>
              </view>
            </view>
          </view>
        </view>
      </view>

      <view class="bottom-space"></view>
    </view>

    <TabBar current="/pages/wms/overview" />
  </view>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import TabBar from '@/components/TabBar.vue'

const stats = reactive({
  totalQty: '12,846',
  materialCount: '1,256',
  materialTotal: '3,256',
  monthAdd: 128,
  inboundToday: 48,
  inboundDelta: 12,
  outboundToday: 36,
  outboundDelta: 8,
  warningCount: 23,
  warningLow: 15
})

const quickMenus = ref([
  { icon: '📦', name: '物料管理', bg: 'bg-blue', path: '' },
  { icon: '⬇️', name: '入库管理', bg: 'bg-green', path: '' },
  { icon: '⬆️', name: '出库管理', bg: 'bg-orange', path: '' },
  { icon: '🧾', name: '库存盘点', bg: 'bg-purple', path: '' },
  { icon: '⚠️', name: '库存预警', bg: 'bg-red', path: '' },
  { icon: '🏬', name: '仓库管理', bg: 'bg-cyan', path: '' },
  { icon: '📝', name: '采购需求', bg: 'bg-yellow', path: '' },
  { icon: '🛍️', name: '采购订单', bg: 'bg-pink', path: '' }
])

const chartTab = ref('inbound')

const distribution = ref([
  { name: '原材料', quantity: 5680, color: '#409eff' },
  { name: '半成品', quantity: 3420, color: '#67c23a' },
  { name: '成品', quantity: 2860, color: '#e6a23c' },
  { name: '辅料', quantity: 886, color: '#f56c6c' }
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

const pendingInbound = ref([
  {
    id: 1,
    code: 'RK20260627001',
    supplier: '东莞电子科技有限公司',
    warehouse: '原材料仓库',
    totalQty: 500,
    status: 'pending',
    statusText: '待入库'
  },
  {
    id: 2,
    code: 'RK20260627002',
    supplier: '深圳五金制品厂',
    warehouse: '原材料仓库',
    totalQty: 1200,
    status: 'partial',
    statusText: '部分入库'
  }
])

const pendingOutbound = ref([
  {
    id: 1,
    code: 'CK20260627001',
    customer: '广州智能制造集团',
    warehouse: '成品仓库',
    totalQty: 300,
    status: 'pending',
    statusText: '待出库'
  },
  {
    id: 2,
    code: 'CK20260627002',
    customer: '佛山机械设备有限公司',
    warehouse: '成品仓库',
    totalQty: 150,
    status: 'picking',
    statusText: '拣货中'
  }
])

function handleMenuTap(item: any) {
  if (item.path) {
    uni.navigateTo({ url: item.path })
  } else {
    uni.showToast({ title: item.name, icon: 'none' })
  }
}
</script>

<style lang="scss" scoped>
.wms-page {
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
  height: 360rpx;
  background: linear-gradient(135deg, #409eff, #1890ff);
  border-bottom-left-radius: 40rpx;
  border-bottom-right-radius: 40rpx;
}

.header-content {
  position: relative;
  z-index: 1;
  padding: calc(var(--status-bar-height) + 30rpx) $spacing-lg 0;

  .page-title {
    font-size: $font-size-xl;
    font-weight: $font-weight-bold;
    color: #fff;
    margin-bottom: $spacing-lg;
  }

  .header-stats {
    display: flex;
    gap: $spacing-lg;

    .stat-item {
      .stat-value {
        display: block;
        font-size: $font-size-xxl;
        font-weight: $font-weight-bold;
        color: #fff;
        margin-bottom: 4rpx;
      }

      .stat-label {
        font-size: $font-size-xs;
        color: rgba(255, 255, 255, 0.8);
      }
    }
  }
}

.content-section {
  padding: 0 $spacing-md;
  margin-top: -20rpx;
  position: relative;
  z-index: 2;
}

.stats-cards {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: $spacing-md;
  margin-bottom: $spacing-md;

  .stat-card {
    background: #fff;
    border-radius: $border-radius-lg;
    padding: $spacing-md $spacing-lg;
    box-shadow: $shadow-sm;
    display: flex;
    flex-direction: column;
    position: relative;
    overflow: hidden;

    &.blue { border-left: 6rpx solid $primary-color; }
    &.green { border-left: 6rpx solid $success-color; }
    &.orange { border-left: 6rpx solid $warning-color; }
    &.red { border-left: 6rpx solid $danger-color; }

    .stat-icon {
      font-size: 40rpx;
      margin-bottom: $spacing-xs;
    }

    .stat-info {
      margin-bottom: $spacing-xs;

      .stat-num {
        display: block;
        font-size: $font-size-lg;
        font-weight: $font-weight-bold;
        color: $text-primary;
      }

      .stat-name {
        font-size: $font-size-xs;
        color: $text-secondary;
      }
    }

    .stat-trend {
      font-size: 20rpx;

      &.up { color: $success-color; }
      &.down { color: $danger-color; }
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
  margin-bottom: $spacing-md;

  .menu-item {
    display: flex;
    flex-direction: column;
    align-items: center;

    .menu-icon {
      width: 88rpx;
      height: 88rpx;
      border-radius: 24rpx;
      display: flex;
      align-items: center;
      justify-content: center;
      margin-bottom: $spacing-xs;

      &.bg-blue { background: linear-gradient(135deg, #66b1ff, #409eff); }
      &.bg-green { background: linear-gradient(135deg, #85ce61, #67c23a); }
      &.bg-orange { background: linear-gradient(135deg, #f0c78a, #e6a23c); }
      &.bg-purple { background: linear-gradient(135deg, #9c88ff, #764ba2); }
      &.bg-red { background: linear-gradient(135deg, #ff8a80, #f56c6c); }
      &.bg-cyan { background: linear-gradient(135deg, #5bbfd4, #2db7f5); }
      &.bg-yellow { background: linear-gradient(135deg, #ffe58f, #faad14); }
      &.bg-pink { background: linear-gradient(135deg, #ff9ebb, #f56c6c); }

      .icon-text {
        font-size: 40rpx;
      }
    }

    .menu-name {
      font-size: $font-size-xs;
      color: $text-regular;
    }
  }
}

.section-card {
  background: #fff;
  border-radius: $border-radius-lg;
  padding: $spacing-lg;
  margin-bottom: $spacing-md;
  box-shadow: $shadow-sm;

  .card-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: $spacing-lg;

    .card-title {
      font-size: $font-size-md;
      font-weight: $font-weight-semibold;
      color: $text-primary;
    }

    .card-more {
      font-size: $font-size-xs;
      color: $text-secondary;
    }
  }
}

.donut-section {
  display: flex;
  align-items: center;
  gap: $spacing-lg;

  .donut-chart {
    width: 240rpx;
    height: 240rpx;
    position: relative;
    flex-shrink: 0;

    .donut-ring {
      width: 100%;
      height: 100%;
      border-radius: 50%;
      background: conic-gradient(
        #409eff 0deg 160deg,
        #67c23a 160deg 256deg,
        #e6a23c 256deg 336deg,
        #f56c6c 336deg 360deg
      );
      position: relative;

      &::before {
        content: '';
        position: absolute;
        top: 40rpx;
        left: 40rpx;
        right: 40rpx;
        bottom: 40rpx;
        background: #fff;
        border-radius: 50%;
      }
    }

    .donut-center {
      position: absolute;
      top: 50%;
      left: 50%;
      transform: translate(-50%, -50%);
      text-align: center;

      .donut-value {
        display: block;
        font-size: $font-size-lg;
        font-weight: $font-weight-bold;
        color: $text-primary;
      }

      .donut-label {
        font-size: $font-size-xs;
        color: $text-secondary;
      }
    }
  }

  .donut-legend {
    flex: 1;

    .legend-item {
      display: flex;
      align-items: center;
      margin-bottom: $spacing-md;

      &:last-child {
        margin-bottom: 0;
      }

      .legend-dot {
        width: 16rpx;
        height: 16rpx;
        border-radius: 4rpx;
        margin-right: $spacing-sm;
      }

      .legend-name {
        flex: 1;
        font-size: $font-size-sm;
        color: $text-regular;
      }

      .legend-value {
        font-size: $font-size-sm;
        font-weight: $font-weight-medium;
        color: $text-primary;
      }
    }
  }
}

.chart-tabs {
  display: flex;
  background: $bg-page;
  border-radius: 24rpx;
  padding: 6rpx;

  .tab-item {
    padding: 8rpx 24rpx;
    font-size: $font-size-xs;
    color: $text-secondary;
    border-radius: 20rpx;
    transition: all 0.2s;

    &.active {
      background: #fff;
      color: $primary-color;
      font-weight: $font-weight-medium;
      box-shadow: $shadow-sm;
    }
  }
}

.line-chart {
  .chart-area {
    height: 300rpx;
    display: flex;
    align-items: flex-end;
    justify-content: space-between;
    padding-top: 20rpx;

    .chart-bar-wrap {
      flex: 1;
      display: flex;
      flex-direction: column;
      align-items: center;

      .chart-bar {
        width: 24rpx;
        border-radius: 12rpx 12rpx 0 0;
        min-height: 8rpx;

        &.inbound {
          background: linear-gradient(180deg, #66b1ff, #409eff);
        }

        &.outbound {
          background: linear-gradient(180deg, #85ce61, #67c23a);
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

.order-list {
  .order-item {
    padding: $spacing-md 0;
    border-bottom: 1rpx solid $border-color-light;

    &:last-child {
      border-bottom: none;
    }

    .order-header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      margin-bottom: $spacing-sm;

      .order-code {
        font-size: $font-size-sm;
        font-weight: $font-weight-medium;
        color: $text-primary;
      }

      .order-status {
        padding: 4rpx 16rpx;
        border-radius: 20rpx;
        font-size: 20rpx;

        &.status-pending {
          background: $primary-light;
          color: $primary-color;
        }

        &.status-partial {
          background: $warning-light;
          color: $warning-color;
        }

        &.status-picking {
          background: $success-light;
          color: $success-color;
        }
      }
    }

    .order-body {
      .order-info {
        display: flex;
        align-items: center;
        margin-bottom: 8rpx;

        &:last-child {
          margin-bottom: 0;
        }

        .info-label {
          width: 100rpx;
          font-size: $font-size-xs;
          color: $text-secondary;
          flex-shrink: 0;
        }

        .info-value {
          flex: 1;
          font-size: $font-size-xs;
          color: $text-regular;

          &.highlight {
            color: $primary-color;
            font-weight: $font-weight-medium;
          }
        }
      }
    }
  }
}

.bottom-space {
  height: 40rpx;
}
</style>
