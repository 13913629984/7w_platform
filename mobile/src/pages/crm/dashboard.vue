<template>
  <view class="crm-dashboard-page">
    <view class="page-header">
      <view class="nav-bar">
        <view class="nav-back" @tap="goBack">
          <text class="back-icon">‹</text>
        </view>
        <text class="nav-title">CRM 看板</text>
        <view class="nav-placeholder"></view>
      </view>
    </view>

    <scroll-view class="dashboard-scroll" scroll-y>
      <view class="dashboard-body">
        <view class="dash-card">
          <text class="dash-title">客户等级分布</text>
          <view class="level-bars">
            <view v-for="lv in levelStats" :key="lv.level" class="level-item">
              <view class="level-top">
                <text class="level-label">{{ lv.level }}</text>
                <text class="level-count">{{ lv.count }} 家</text>
              </view>
              <view class="level-bar-bg">
                <view class="level-bar-fill" :style="{ width: lv.percent + '%', background: lv.color }"></view>
              </view>
            </view>
          </view>
          <view class="level-summary">
            <view class="sum-row">
              <text class="sum-text">A/B级优质客户占比</text>
              <text class="sum-num">46.6%</text>
            </view>
          </view>
        </view>

        <view class="dash-card">
          <text class="dash-title">拜访统计</text>
          <view class="stat-row">
            <view class="stat-box">
              <text class="stat-box-num">{{ visitStats.today }}</text>
              <text class="stat-box-label">今日拜访</text>
            </view>
            <view class="stat-box">
              <text class="stat-box-num">{{ visitStats.week }}</text>
              <text class="stat-box-label">本周拜访</text>
            </view>
            <view class="stat-box">
              <text class="stat-box-num">{{ visitStats.month }}</text>
              <text class="stat-box-label">本月拜访</text>
            </view>
            <view class="stat-box">
              <text class="stat-box-num">{{ visitStats.planned }}</text>
              <text class="stat-box-label">待拜访</text>
            </view>
          </view>
          <view class="stat-divider"></view>
          <view class="visit-types">
            <view class="type-item">
              <text class="type-icon">🏢</text>
              <view class="type-info">
                <text class="type-num">48</text>
                <text class="type-label">上门拜访</text>
              </view>
            </view>
            <view class="type-item">
              <text class="type-icon">📞</text>
              <view class="type-info">
                <text class="type-num">36</text>
                <text class="type-label">电话沟通</text>
              </view>
            </view>
            <view class="type-item">
              <text class="type-icon">📹</text>
              <view class="type-info">
                <text class="type-num">28</text>
                <text class="type-label">视频会议</text>
              </view>
            </view>
            <view class="type-item">
              <text class="type-icon">🤝</text>
              <view class="type-info">
                <text class="type-num">16</text>
                <text class="type-label">客户来访</text>
              </view>
            </view>
          </view>
        </view>

        <view class="dash-card">
          <text class="dash-title">商机概览</text>
          <view class="opp-summary">
            <view class="opp-sum-item">
              <text class="sum-num text-primary">{{ oppStats.total }}</text>
              <text class="sum-label">总商机</text>
            </view>
            <view class="opp-sum-item">
              <text class="sum-num text-warning">¥{{ oppStats.amount }}</text>
              <text class="sum-label">总金额</text>
            </view>
            <view class="opp-sum-item">
              <text class="sum-num text-success">{{ oppStats.wonRate }}%</text>
              <text class="sum-label">赢单率</text>
            </view>
          </view>
          <view class="stage-list">
            <view v-for="st in oppStats.stages" :key="st.name" class="stage-row">
              <view class="stage-left">
                <view class="stage-dot" :style="{ background: st.color }"></view>
                <text class="stage-name">{{ st.name }}</text>
              </view>
              <view class="stage-bar-wrap">
                <view class="stage-bar-bg">
                  <view class="stage-bar-fill" :style="{ width: st.percent + '%', background: st.color }"></view>
                </view>
              </view>
              <text class="stage-num">{{ st.count }}</text>
            </view>
          </view>
        </view>

        <view class="dash-card">
          <text class="dash-title">报价统计</text>
          <view class="quote-row">
            <view class="quote-box">
              <text class="quote-num">36</text>
              <text class="quote-label">报价总数</text>
            </view>
            <view class="quote-divider"></view>
            <view class="quote-box">
              <text class="quote-num text-success">¥2,680万</text>
              <text class="quote-label">报价总额</text>
            </view>
            <view class="quote-divider"></view>
            <view class="quote-box">
              <text class="quote-num text-primary">42%</text>
              <text class="quote-label">接受率</text>
            </view>
          </view>
          <view class="quote-status">
            <view class="status-item">
              <text class="status-dot" style="background:#67c23a"></text>
              <text class="status-label">已接受</text>
              <text class="status-num">15</text>
            </view>
            <view class="status-item">
              <text class="status-dot" style="background:#409eff"></text>
              <text class="status-label">已发送</text>
              <text class="status-num">8</text>
            </view>
            <view class="status-item">
              <text class="status-dot" style="background:#e6a23c"></text>
              <text class="status-label">待审批</text>
              <text class="status-num">6</text>
            </view>
            <view class="status-item">
              <text class="status-dot" style="background:#f56c6c"></text>
              <text class="status-label">已拒绝</text>
              <text class="status-num">4</text>
            </view>
            <view class="status-item">
              <text class="status-dot" style="background:#909399"></text>
              <text class="status-label">已过期</text>
              <text class="status-num">3</text>
            </view>
          </view>
        </view>
      </view>

      <view class="bottom-space"></view>
    </scroll-view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'

const levelStats = ref([
  { level: 'A级', count: 68, percent: 100, color: '#f56c6c' },
  { level: 'B级', count: 112, percent: 65, color: '#e6a23c' },
  { level: 'C级', count: 156, percent: 45, color: '#67c23a' },
  { level: 'D级', count: 50, percent: 15, color: '#909399' }
])

const visitStats = ref({
  today: 8,
  week: 32,
  month: 128,
  planned: 15
})

const oppStats = ref({
  total: 42,
  amount: '1,280万',
  wonRate: 35,
  stages: [
    { name: '初步接触', count: 8, percent: 19, color: '#909399' },
    { name: '需求确认', count: 12, percent: 29, color: '#e6a23c' },
    { name: '方案报价', count: 10, percent: 24, color: '#409eff' },
    { name: '商务谈判', count: 7, percent: 17, color: '#667eea' },
    { name: '赢单', count: 5, percent: 12, color: '#67c23a' }
  ]
})

function goBack() {
  uni.navigateBack()
}
</script>

<style lang="scss" scoped>
.crm-dashboard-page {
  min-height: 100vh;
  background: $bg-page;
  display: flex;
  flex-direction: column;
}

.page-header {
  background: linear-gradient(135deg, #f56c6c, #e74c3c);

  .nav-bar {
    display: flex;
    align-items: center;
    justify-content: space-between;
    height: 88rpx;
    padding: 0 $spacing-md;
    padding-top: var(--status-bar-height);

    .nav-back {
      width: 60rpx;
      height: 60rpx;
      display: flex;
      align-items: center;
      justify-content: center;

      .back-icon {
        font-size: 56rpx;
        color: #fff;
        font-weight: 300;
      }
    }

    .nav-title {
      font-size: $font-size-lg;
      font-weight: $font-weight-semibold;
      color: #fff;
    }

    .nav-placeholder {
      width: 60rpx;
    }
  }
}

.dashboard-scroll {
  flex: 1;
  height: 0;
}

.dashboard-body {
  padding: $spacing-md;

  .dash-card {
    background: #fff;
    border-radius: $border-radius-lg;
    padding: $spacing-lg;
    margin-bottom: $spacing-md;
    box-shadow: $shadow-sm;

    .dash-title {
      display: block;
      font-size: $font-size-md;
      font-weight: $font-weight-semibold;
      color: $text-primary;
      margin-bottom: $spacing-lg;
      padding-bottom: $spacing-sm;
      border-bottom: 1rpx solid $border-color-light;
    }
  }

  .level-bars {
    .level-item {
      margin-bottom: $spacing-md;

      &:last-child {
        margin-bottom: 0;
      }

      .level-top {
        display: flex;
        align-items: center;
        justify-content: space-between;
        margin-bottom: 8rpx;

        .level-label {
          font-size: $font-size-sm;
          color: $text-regular;
          font-weight: $font-weight-medium;
        }

        .level-count {
          font-size: $font-size-sm;
          color: $text-secondary;
        }
      }

      .level-bar-bg {
        width: 100%;
        height: 16rpx;
        background: $bg-page;
        border-radius: 8rpx;
        overflow: hidden;

        .level-bar-fill {
          height: 100%;
          border-radius: 8rpx;
          transition: width 0.5s;
        }
      }
    }
  }

  .level-summary {
    margin-top: $spacing-lg;
    padding-top: $spacing-md;
    border-top: 1rpx solid $border-color-light;

    .sum-row {
      display: flex;
      align-items: center;
      justify-content: space-between;

      .sum-text {
        font-size: $font-size-sm;
        color: $text-regular;
      }

      .sum-num {
        font-size: $font-size-lg;
        font-weight: $font-weight-bold;
        color: $success-color;
      }
    }
  }

  .stat-row {
    display: flex;
    justify-content: space-around;

    .stat-box {
      display: flex;
      flex-direction: column;
      align-items: center;

      .stat-box-num {
        font-size: $font-size-xl;
        font-weight: $font-weight-bold;
        color: $text-primary;
        margin-bottom: 6rpx;
      }

      .stat-box-label {
        font-size: $font-size-xs;
        color: $text-secondary;
      }
    }
  }

  .stat-divider {
    height: 1rpx;
    background: $border-color-light;
    margin: $spacing-lg 0;
  }

  .visit-types {
    display: flex;
    justify-content: space-around;

    .type-item {
      display: flex;
      flex-direction: column;
      align-items: center;

      .type-icon {
        font-size: 40rpx;
        margin-bottom: 6rpx;
      }

      .type-info {
        display: flex;
        flex-direction: column;
        align-items: center;

        .type-num {
          font-size: $font-size-sm;
          font-weight: $font-weight-bold;
          color: $text-primary;
          margin-bottom: 2rpx;
        }

        .type-label {
          font-size: 20rpx;
          color: $text-secondary;
        }
      }
    }
  }

  .opp-summary {
    display: flex;
    justify-content: space-around;
    margin-bottom: $spacing-lg;
    padding-bottom: $spacing-lg;
    border-bottom: 1rpx solid $border-color-light;

    .opp-sum-item {
      display: flex;
      flex-direction: column;
      align-items: center;

      .sum-num {
        font-size: $font-size-xl;
        font-weight: $font-weight-bold;
        margin-bottom: 6rpx;
      }

      .sum-label {
        font-size: $font-size-xs;
        color: $text-secondary;
      }
    }
  }

  .stage-list {
    .stage-row {
      display: flex;
      align-items: center;
      margin-bottom: $spacing-md;

      &:last-child {
        margin-bottom: 0;
      }

      .stage-left {
        display: flex;
        align-items: center;
        width: 160rpx;
        flex-shrink: 0;

        .stage-dot {
          width: 16rpx;
          height: 16rpx;
          border-radius: 50%;
          margin-right: $spacing-sm;
        }

        .stage-name {
          font-size: $font-size-sm;
          color: $text-regular;
        }
      }

      .stage-bar-wrap {
        flex: 1;
        padding-right: $spacing-sm;

        .stage-bar-bg {
          width: 100%;
          height: 12rpx;
          background: $bg-page;
          border-radius: 6rpx;
          overflow: hidden;

          .stage-bar-fill {
            height: 100%;
            border-radius: 6rpx;
            transition: width 0.5s;
          }
        }
      }

      .stage-num {
        width: 60rpx;
        text-align: right;
        font-size: $font-size-sm;
        font-weight: $font-weight-medium;
        color: $text-primary;
        flex-shrink: 0;
      }
    }
  }

  .quote-row {
    display: flex;
    align-items: center;
    justify-content: space-around;
    margin-bottom: $spacing-lg;
    padding-bottom: $spacing-lg;
    border-bottom: 1rpx solid $border-color-light;

    .quote-box {
      display: flex;
      flex-direction: column;
      align-items: center;

      .quote-num {
        font-size: $font-size-xl;
        font-weight: $font-weight-bold;
        color: $text-primary;
        margin-bottom: 6rpx;
      }

      .quote-label {
        font-size: $font-size-xs;
        color: $text-secondary;
      }
    }

    .quote-divider {
      width: 1rpx;
      height: 60rpx;
      background: $border-color-light;
    }
  }

  .quote-status {
    display: flex;
    justify-content: space-around;

    .status-item {
      display: flex;
      flex-direction: column;
      align-items: center;

      .status-dot {
        width: 16rpx;
        height: 16rpx;
        border-radius: 50%;
        margin-bottom: 6rpx;
      }

      .status-label {
        font-size: 20rpx;
        color: $text-secondary;
        margin-bottom: 2rpx;
      }

      .status-num {
        font-size: $font-size-xs;
        font-weight: $font-weight-medium;
        color: $text-primary;
      }
    }
  }
}

.bottom-space {
  height: 60rpx;
}
</style>
