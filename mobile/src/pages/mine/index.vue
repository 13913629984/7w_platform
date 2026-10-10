<template>
  <view class="mine-page">
    <view class="header-section">
      <view class="header-bg"></view>
      <view class="user-section">
        <view class="user-avatar">
          <text class="avatar-text">{{ userInfo.nickname.charAt(0) }}</text>
        </view>
        <view class="user-info">
          <view class="user-name">{{ userInfo.nickname }}</view>
          <view class="user-role">
            <text class="role-tag" v-for="role in userInfo.roles" :key="role">{{ role }}</text>
          </view>
        </view>
        <view class="setting-btn" @tap="goToSettings">
          <text class="setting-icon">⚙️</text>
        </view>
      </view>

      <view class="stats-row">
        <view class="stat-item">
          <text class="stat-num">28</text>
          <text class="stat-label">待办事项</text>
        </view>
        <view class="stat-divider"></view>
        <view class="stat-item">
          <text class="stat-num">156</text>
          <text class="stat-label">本月完成</text>
        </view>
        <view class="stat-divider"></view>
        <view class="stat-item">
          <text class="stat-num">12</text>
          <text class="stat-label">我的收藏</text>
        </view>
      </view>
    </view>

    <view class="menu-section">
      <view class="menu-group">
        <view class="menu-item" @tap="handleMenuTap('profile')">
          <view class="menu-left">
            <view class="menu-icon blue">
              <text class="icon-text">👤</text>
            </view>
            <text class="menu-name">个人资料</text>
          </view>
          <text class="menu-arrow">›</text>
        </view>
        <view class="menu-item" @tap="handleMenuTap('password')">
          <view class="menu-left">
            <view class="menu-icon green">
              <text class="icon-text">🔑</text>
            </view>
            <text class="menu-name">修改密码</text>
          </view>
          <text class="menu-arrow">›</text>
        </view>
        <view class="menu-item" @tap="handleMenuTap('permissions')">
          <view class="menu-left">
            <view class="menu-icon orange">
              <text class="icon-text">🔒</text>
            </view>
            <text class="menu-name">我的权限</text>
          </view>
          <text class="menu-arrow">›</text>
        </view>
      </view>

      <view class="menu-group">
        <view class="menu-item" @tap="handleMenuTap('systems')">
          <view class="menu-left">
            <view class="menu-icon purple">
              <text class="icon-text">📱</text>
            </view>
            <text class="menu-name">系统权限</text>
          </view>
          <view class="menu-right">
            <text class="menu-desc">6个子系统</text>
            <text class="menu-arrow">›</text>
          </view>
        </view>
        <view class="menu-item" @tap="handleMenuTap('org')">
          <view class="menu-left">
            <view class="menu-icon cyan">
              <text class="icon-text">🏢</text>
            </view>
            <text class="menu-name">所属组织</text>
          </view>
          <view class="menu-right">
            <text class="menu-desc">技术研发部</text>
            <text class="menu-arrow">›</text>
          </view>
        </view>
      </view>

      <view class="menu-group">
        <view class="menu-item" @tap="handleMenuTap('message')">
          <view class="menu-left">
            <view class="menu-icon red">
              <text class="icon-text">🔔</text>
            </view>
            <text class="menu-name">消息通知</text>
          </view>
          <view class="menu-right">
            <view class="badge">3</view>
            <text class="menu-arrow">›</text>
          </view>
        </view>
        <view class="menu-item" @tap="handleMenuTap('help')">
          <view class="menu-left">
            <view class="menu-icon yellow">
              <text class="icon-text">❓</text>
            </view>
            <text class="menu-name">帮助中心</text>
          </view>
          <text class="menu-arrow">›</text>
        </view>
        <view class="menu-item" @tap="handleMenuTap('about')">
          <view class="menu-left">
            <view class="menu-icon gray">
              <text class="icon-text">ℹ️</text>
            </view>
            <text class="menu-name">关于我们</text>
          </view>
          <view class="menu-right">
            <text class="menu-desc">v1.0.0</text>
            <text class="menu-arrow">›</text>
          </view>
        </view>
      </view>

      <view class="logout-btn" @tap="handleLogout">
        <text class="logout-text">退出登录</text>
      </view>
    </view>

    <view class="bottom-space"></view>

    <TabBar current="/pages/mine/index" />
  </view>
</template>

<script setup lang="ts">
import { reactive } from 'vue'
import TabBar from '@/components/TabBar.vue'

const userInfo = reactive({
  username: 'admin',
  nickname: '系统管理员',
  roles: ['超级管理员']
})

function handleMenuTap(type: string) {
  const map: Record<string, string> = {
    profile: '个人资料',
    password: '修改密码',
    permissions: '我的权限',
    systems: '系统权限',
    org: '所属组织',
    message: '消息通知',
    help: '帮助中心',
    about: '关于我们'
  }
  uni.showToast({ title: map[type] || type, icon: 'none' })
}

function goToSettings() {
  uni.showToast({ title: '设置', icon: 'none' })
}

function handleLogout() {
  uni.showModal({
    title: '提示',
    content: '确定要退出登录吗？',
    success: (res) => {
      if (res.confirm) {
        uni.showToast({ title: '已退出登录', icon: 'success' })
        setTimeout(() => {
          uni.reLaunch({ url: '/pages/login/index' })
        }, 1000)
      }
    }
  })
}
</script>

<style lang="scss" scoped>
.mine-page {
  min-height: 100vh;
  background: $bg-page;
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
  height: 400rpx;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-bottom-left-radius: 40rpx;
  border-bottom-right-radius: 40rpx;
}

.user-section {
  position: relative;
  z-index: 1;
  padding: calc(var(--status-bar-height) + 40rpx) $spacing-lg $spacing-lg;
  display: flex;
  align-items: center;

  .user-avatar {
    width: 120rpx;
    height: 120rpx;
    border-radius: 50%;
    background: rgba(255, 255, 255, 0.25);
    border: 3rpx solid rgba(255, 255, 255, 0.5);
    display: flex;
    align-items: center;
    justify-content: center;
    margin-right: $spacing-lg;

    .avatar-text {
      font-size: 52rpx;
      font-weight: $font-weight-bold;
      color: #fff;
    }
  }

  .user-info {
    flex: 1;

    .user-name {
      font-size: $font-size-xl;
      font-weight: $font-weight-bold;
      color: #fff;
      margin-bottom: $spacing-xs;
    }

    .user-role {
      display: flex;
      gap: $spacing-xs;

      .role-tag {
        padding: 4rpx 16rpx;
        background: rgba(255, 255, 255, 0.25);
        border-radius: 20rpx;
        font-size: $font-size-xs;
        color: #fff;
      }
    }
  }

  .setting-btn {
    width: 72rpx;
    height: 72rpx;
    background: rgba(255, 255, 255, 0.2);
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;

    .setting-icon {
      font-size: 36rpx;
    }
  }
}

.stats-row {
  position: relative;
  z-index: 2;
  display: flex;
  align-items: center;
  justify-content: space-around;
  margin: 0 $spacing-lg;
  padding: $spacing-lg 0;
  background: #fff;
  border-radius: $border-radius-lg;
  box-shadow: $shadow-md;

  .stat-item {
    text-align: center;
    flex: 1;

    .stat-num {
      display: block;
      font-size: $font-size-xl;
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

.menu-section {
  padding: $spacing-md;

  .menu-group {
    background: #fff;
    border-radius: $border-radius-lg;
    margin-bottom: $spacing-md;
    overflow: hidden;
    box-shadow: $shadow-sm;

    .menu-item {
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: $spacing-md $spacing-lg;
      border-bottom: 1rpx solid $border-color-light;

      &:last-child {
        border-bottom: none;
      }

      .menu-left {
        display: flex;
        align-items: center;

        .menu-icon {
          width: 72rpx;
          height: 72rpx;
          border-radius: 20rpx;
          display: flex;
          align-items: center;
          justify-content: center;
          margin-right: $spacing-md;

          &.blue { background: $primary-light; }
          &.green { background: $success-light; }
          &.orange { background: $warning-light; }
          &.purple { background: #f4f0ff; }
          &.cyan { background: #e6fffb; }
          &.red { background: $danger-light; }
          &.yellow { background: #fffbe6; }
          &.gray { background: $info-light; }

          .icon-text {
            font-size: 36rpx;
          }
        }

        .menu-name {
          font-size: $font-size-md;
          color: $text-primary;
        }
      }

      .menu-right {
        display: flex;
        align-items: center;
        gap: $spacing-sm;

        .menu-desc {
          font-size: $font-size-sm;
          color: $text-secondary;
        }

        .badge {
          min-width: 36rpx;
          height: 36rpx;
          background: #f56c6c;
          border-radius: 18rpx;
          font-size: 20rpx;
          color: #fff;
          display: flex;
          align-items: center;
          justify-content: center;
          padding: 0 10rpx;
        }
      }

      .menu-arrow {
        font-size: $font-size-lg;
        color: $text-placeholder;
      }
    }
  }
}

.logout-btn {
  background: #fff;
  border-radius: $border-radius-lg;
  padding: $spacing-lg;
  text-align: center;
  box-shadow: $shadow-sm;
  margin-top: $spacing-lg;

  .logout-text {
    font-size: $font-size-md;
    color: #f56c6c;
    font-weight: $font-weight-medium;
  }
}

.bottom-space {
  height: 40rpx;
}
</style>
