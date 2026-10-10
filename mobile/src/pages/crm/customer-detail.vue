<template>
  <view class="detail-page">
    <view class="header-section">
      <view class="header-bg"></view>
      <view class="customer-header">
        <view class="customer-avatar" :class="'level-' + customer.levelCode">
          {{ customer.name.charAt(0) }}
        </view>
        <view class="customer-info">
          <view class="customer-name">{{ customer.name }}</view>
          <view class="customer-tags">
            <view class="tag tag-danger">{{ customer.level }}</view>
            <view class="tag tag-primary">{{ customer.status }}</view>
          </view>
        </view>
      </view>

      <view class="quick-actions">
        <view class="action-item" @tap="callCustomer">
          <view class="action-icon-box">
            <text class="action-icon">📞</text>
          </view>
          <text class="action-text">电话</text>
        </view>
        <view class="action-item" @tap="sendMessage">
          <view class="action-icon-box">
            <text class="action-icon">💬</text>
          </view>
          <text class="action-text">消息</text>
        </view>
        <view class="action-item" @tap="navigateTo">
          <view class="action-icon-box">
            <text class="action-icon">📍</text>
          </view>
          <text class="action-text">导航</text>
        </view>
        <view class="action-item" @tap="editCustomer">
          <view class="action-icon-box">
            <text class="action-icon">✏️</text>
          </view>
          <text class="action-text">编辑</text>
        </view>
      </view>
    </view>

    <view class="stats-bar">
      <view class="stat-item">
        <text class="stat-value">¥86.5万</text>
        <text class="stat-label">累计成交额</text>
      </view>
      <view class="stat-divider"></view>
      <view class="stat-item">
        <text class="stat-value">12</text>
        <text class="stat-label">成交订单</text>
      </view>
      <view class="stat-divider"></view>
      <view class="stat-item">
        <text class="stat-value">28</text>
        <text class="stat-label">跟进记录</text>
      </view>
    </view>

    <view class="content-section">
      <view class="info-card">
        <view class="card-title">基本信息</view>
        <view class="info-list">
          <view class="info-item">
            <text class="info-label">客户全称</text>
            <text class="info-value">{{ customer.name }}</text>
          </view>
          <view class="info-item">
            <text class="info-label">英文名称</text>
            <text class="info-value">{{ customer.englishName }}</text>
          </view>
          <view class="info-item">
            <text class="info-label">客户等级</text>
            <view class="tag tag-danger">{{ customer.level }}</view>
          </view>
          <view class="info-item">
            <text class="info-label">销售负责人</text>
            <text class="info-value">{{ customer.owner }}</text>
          </view>
          <view class="info-item">
            <text class="info-label">客户地址</text>
            <text class="info-value address">{{ customer.address }}</text>
          </view>
        </view>
      </view>

      <view class="info-card">
        <view class="card-title-row">
          <text class="card-title">联系人</text>
          <text class="card-more" @tap="addContact">+ 添加</text>
        </view>
        <view class="contact-list">
          <view
            v-for="contact in contacts"
            :key="contact.id"
            class="contact-item"
          >
            <view class="contact-avatar">
              {{ contact.name.charAt(0) }}
            </view>
            <view class="contact-info">
              <view class="contact-name">
                {{ contact.name }}
                <text v-if="contact.isPrimary" class="primary-tag">主要</text>
              </view>
              <view class="contact-position">{{ contact.position }}</view>
              <view class="contact-phone">{{ contact.phone }}</view>
            </view>
            <view class="contact-actions">
              <view class="contact-btn" @tap="callContact(contact)">
                <text class="btn-icon">📞</text>
              </view>
            </view>
          </view>
        </view>
      </view>

      <view class="info-card">
        <view class="card-title-row">
          <text class="card-title">销售团队</text>
          <text class="card-more" @tap="addSales">+ 添加</text>
        </view>
        <view class="sales-list">
          <view
            v-for="sale in salesTeam"
            :key="sale.id"
            class="sales-item"
          >
            <view class="sales-avatar">{{ sale.name.charAt(0) }}</view>
            <view class="sales-info">
              <view class="sales-name">
                {{ sale.name }}
                <text v-if="sale.isOwner" class="owner-tag">负责人</text>
              </view>
              <view class="sales-position">{{ sale.position }}</view>
            </view>
          </view>
        </view>
      </view>

      <view class="info-card">
        <view class="card-title-row">
          <text class="card-title">最近跟进</text>
          <text class="card-more">全部记录 →</text>
        </view>
        <view class="timeline-list">
          <view
            v-for="(record, index) in followRecords"
            :key="index"
            class="timeline-item"
          >
            <view class="timeline-dot"></view>
            <view class="timeline-content">
              <view class="timeline-header">
                <text class="timeline-type">{{ record.type }}</text>
                <text class="timeline-time">{{ record.time }}</text>
              </view>
              <text class="timeline-text">{{ record.content }}</text>
            </view>
          </view>
        </view>
      </view>

      <view class="bottom-space"></view>
    </view>

    <view class="bottom-bar">
      <view class="bar-btn outline" @tap="createFollow">
        <text class="btn-text">写跟进</text>
      </view>
      <view class="bar-btn primary" @tap="createOrder">
        <text class="btn-text">新建订单</text>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { onLoad } from '@dcloudio/uni-app'

const customer = reactive({
  id: 1,
  name: '深圳科技创新有限公司',
  englishName: 'Shenzhen Tech Innovation Co., Ltd.',
  level: 'A级',
  levelCode: 'a',
  status: '合作中',
  owner: '张三',
  address: '深圳市南山区科技园南区科技大厦A座18楼',
  lastVisitAt: '2026-06-20',
  lastDealAt: '2026-06-15'
})

const contacts = ref([
  { id: 1, name: '李明', position: '采购经理', phone: '138****8888', isPrimary: true },
  { id: 2, name: '王芳', position: '技术总监', phone: '139****6666', isPrimary: false }
])

const salesTeam = ref([
  { id: 1, name: '张三', position: '销售总监', isOwner: true },
  { id: 2, name: '李四', position: '销售代表', isOwner: false }
])

const followRecords = ref([
  {
    type: '电话沟通',
    time: '2026-06-25 14:30',
    content: '与客户采购经理李明电话沟通，确认下季度采购计划，预计7月初下单。'
  },
  {
    type: '上门拜访',
    time: '2026-06-20 10:00',
    content: '上门拜访客户，参观了新厂房，了解到客户扩产计划，需要新增3条生产线设备。'
  },
  {
    type: '报价单',
    time: '2026-06-15 16:00',
    content: '发送Q3采购报价单，总金额¥48.5万，客户正在审批中。'
  }
])

onLoad((options: any) => {
  console.log('customer id:', options?.id)
})

function callCustomer() {
  uni.showToast({ title: '拨打客户电话', icon: 'none' })
}

function sendMessage() {
  uni.showToast({ title: '发送消息', icon: 'none' })
}

function navigateTo() {
  uni.showToast({ title: '导航到客户地址', icon: 'none' })
}

function editCustomer() {
  uni.showToast({ title: '编辑客户', icon: 'none' })
}

function addContact() {
  uni.showToast({ title: '添加联系人', icon: 'none' })
}

function addSales() {
  uni.showToast({ title: '添加销售', icon: 'none' })
}

function callContact(contact: any) {
  uni.showToast({ title: '拨打 ' + contact.name, icon: 'none' })
}

function createFollow() {
  uni.showToast({ title: '写跟进', icon: 'none' })
}

function createOrder() {
  uni.showToast({ title: '新建订单', icon: 'none' })
}
</script>

<style lang="scss" scoped>
.detail-page {
  min-height: 100vh;
  background: $bg-page;
  padding-bottom: calc(140rpx + env(safe-area-inset-bottom));
}

.header-section {
  position: relative;
  padding-bottom: 60rpx;
}

.header-bg {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 320rpx;
  background: linear-gradient(135deg, #f56c6c, #e74c3c);
}

.customer-header {
  position: relative;
  z-index: 1;
  padding: calc(var(--status-bar-height) + 30rpx) $spacing-lg 0;
  display: flex;
  align-items: center;

  .customer-avatar {
    width: 120rpx;
    height: 120rpx;
    border-radius: 32rpx;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 52rpx;
    font-weight: $font-weight-bold;
    color: #fff;
    margin-right: $spacing-lg;
    background: rgba(255, 255, 255, 0.2);
    border: 3rpx solid rgba(255, 255, 255, 0.4);

    &.level-a { background: linear-gradient(135deg, #ff8a80, #f56c6c); }
    &.level-b { background: linear-gradient(135deg, #ffd591, #e6a23c); }
    &.level-c { background: linear-gradient(135deg, #b7eb8f, #67c23a); }
  }

  .customer-info {
    flex: 1;
    min-width: 0;

    .customer-name {
      font-size: $font-size-xl;
      font-weight: $font-weight-bold;
      color: #fff;
      margin-bottom: $spacing-sm;
    }

    .customer-tags {
      display: flex;
      gap: $spacing-sm;

      .tag {
        padding: 6rpx 20rpx;
        border-radius: 24rpx;
        font-size: $font-size-xs;
        font-weight: $font-weight-medium;
        background: rgba(255, 255, 255, 0.25);
        color: #fff;
      }
    }
  }
}

.quick-actions {
  position: relative;
  z-index: 2;
  display: flex;
  justify-content: space-around;
  margin: $spacing-lg $spacing-lg 0;
  padding: $spacing-lg 0;
  background: #fff;
  border-radius: $border-radius-lg;
  box-shadow: $shadow-md;

  .action-item {
    display: flex;
    flex-direction: column;
    align-items: center;

    .action-icon-box {
      width: 88rpx;
      height: 88rpx;
      border-radius: 50%;
      background: $primary-light;
      display: flex;
      align-items: center;
      justify-content: center;
      margin-bottom: $spacing-xs;

      .action-icon {
        font-size: 40rpx;
      }
    }

    .action-text {
      font-size: $font-size-xs;
      color: $text-regular;
    }
  }
}

.stats-bar {
  display: flex;
  align-items: center;
  justify-content: space-around;
  margin: 0 $spacing-md;
  padding: $spacing-lg 0;
  background: #fff;
  border-radius: $border-radius-lg;
  box-shadow: $shadow-sm;

  .stat-item {
    text-align: center;
    flex: 1;

    .stat-value {
      display: block;
      font-size: $font-size-lg;
      font-weight: $font-weight-bold;
      color: $text-primary;
      margin-bottom: 4rpx;
    }

    .stat-label {
      font-size: $font-size-xs;
      color: $text-secondary;
    }
  }

  .stat-divider {
    width: 1rpx;
    height: 60rpx;
    background: $border-color;
  }
}

.content-section {
  padding: $spacing-md;

  .info-card {
    background: #fff;
    border-radius: $border-radius-lg;
    padding: $spacing-lg;
    margin-bottom: $spacing-md;
    box-shadow: $shadow-sm;

    .card-title-row {
      display: flex;
      align-items: center;
      justify-content: space-between;
      margin-bottom: $spacing-md;
    }

    .card-title {
      font-size: $font-size-md;
      font-weight: $font-weight-semibold;
      color: $text-primary;
      margin-bottom: $spacing-md;
    }

    .card-more {
      font-size: $font-size-sm;
      color: $primary-color;
    }
  }
}

.info-list {
  .info-item {
    display: flex;
    align-items: flex-start;
    padding: $spacing-sm 0;
    border-bottom: 1rpx solid $border-color-light;

    &:last-child {
      border-bottom: none;
    }

    .info-label {
      width: 160rpx;
      font-size: $font-size-sm;
      color: $text-secondary;
      flex-shrink: 0;
    }

    .info-value {
      flex: 1;
      font-size: $font-size-sm;
      color: $text-regular;
      min-width: 0;

      &.address {
        line-height: 1.6;
      }
    }
  }
}

.contact-list {
  .contact-item {
    display: flex;
    align-items: center;
    padding: $spacing-md 0;
    border-bottom: 1rpx solid $border-color-light;

    &:last-child {
      border-bottom: none;
    }

    .contact-avatar {
      width: 72rpx;
      height: 72rpx;
      border-radius: 50%;
      background: linear-gradient(135deg, #66b1ff, #409eff);
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 32rpx;
      font-weight: $font-weight-medium;
      color: #fff;
      margin-right: $spacing-md;
      flex-shrink: 0;
    }

    .contact-info {
      flex: 1;
      min-width: 0;

      .contact-name {
        font-size: $font-size-sm;
        font-weight: $font-weight-medium;
        color: $text-primary;
        margin-bottom: 4rpx;
        display: flex;
        align-items: center;
        gap: $spacing-xs;

        .primary-tag {
          font-size: 20rpx;
          padding: 2rpx 12rpx;
          background: $success-light;
          color: $success-color;
          border-radius: 16rpx;
          font-weight: normal;
        }
      }

      .contact-position {
        font-size: $font-size-xs;
        color: $text-secondary;
        margin-bottom: 2rpx;
      }

      .contact-phone {
        font-size: $font-size-xs;
        color: $primary-color;
      }
    }

    .contact-actions {
      .contact-btn {
        width: 64rpx;
        height: 64rpx;
        border-radius: 50%;
        background: $primary-light;
        display: flex;
        align-items: center;
        justify-content: center;

        .btn-icon {
          font-size: 28rpx;
        }
      }
    }
  }
}

.sales-list {
  .sales-item {
    display: flex;
    align-items: center;
    padding: $spacing-sm 0;

    .sales-avatar {
      width: 64rpx;
      height: 64rpx;
      border-radius: 50%;
      background: linear-gradient(135deg, #85ce61, #67c23a);
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 28rpx;
      font-weight: $font-weight-medium;
      color: #fff;
      margin-right: $spacing-md;
    }

    .sales-info {
      flex: 1;

      .sales-name {
        font-size: $font-size-sm;
        color: $text-regular;
        display: flex;
        align-items: center;
        gap: $spacing-xs;
        margin-bottom: 2rpx;

        .owner-tag {
          font-size: 20rpx;
          padding: 2rpx 12rpx;
          background: $primary-light;
          color: $primary-color;
          border-radius: 16rpx;
        }
      }

      .sales-position {
        font-size: $font-size-xs;
        color: $text-secondary;
      }
    }
  }
}

.timeline-list {
  position: relative;
  padding-left: 32rpx;

  .timeline-item {
    position: relative;
    padding-bottom: $spacing-lg;

    &:last-child {
      padding-bottom: 0;
    }

    .timeline-dot {
      position: absolute;
      left: -32rpx;
      top: 8rpx;
      width: 16rpx;
      height: 16rpx;
      border-radius: 50%;
      background: $primary-color;
      border: 4rpx solid $primary-light;
    }

    .timeline-content {
      .timeline-header {
        display: flex;
        align-items: center;
        justify-content: space-between;
        margin-bottom: $spacing-xs;

        .timeline-type {
          font-size: $font-size-sm;
          font-weight: $font-weight-medium;
          color: $text-primary;
        }

        .timeline-time {
          font-size: $font-size-xs;
          color: $text-secondary;
        }
      }

      .timeline-text {
        font-size: $font-size-sm;
        color: $text-regular;
        line-height: 1.6;
      }
    }
  }
}

.bottom-space {
  height: 40rpx;
}

.bottom-bar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  gap: $spacing-md;
  padding: $spacing-md $spacing-lg;
  padding-bottom: calc(#{$spacing-md} + env(safe-area-inset-bottom));
  background: #fff;
  box-shadow: 0 -4rpx 20rpx rgba(0, 0, 0, 0.06);

  .bar-btn {
    flex: 1;
    height: 88rpx;
    border-radius: $border-radius-md;
    display: flex;
    align-items: center;
    justify-content: center;

    &.outline {
      background: #fff;
      border: 2rpx solid $primary-color;

      .btn-text {
        color: $primary-color;
      }
    }

    &.primary {
      background: linear-gradient(135deg, #667eea, #764ba2);

      .btn-text {
        color: #fff;
        font-weight: $font-weight-medium;
      }
    }

    .btn-text {
      font-size: $font-size-md;
    }
  }
}
</style>
