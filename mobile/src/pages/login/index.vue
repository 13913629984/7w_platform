<template>
  <view class="login-page">
    <view class="login-bg">
      <view class="bg-circle c1"></view>
      <view class="bg-circle c2"></view>
      <view class="bg-circle c3"></view>
    </view>

    <view class="login-content">
      <view class="logo-section">
        <view class="logo-icon">七</view>
        <view class="logo-title">七维空间</view>
        <view class="logo-subtitle">企业信息化平台</view>
      </view>

      <view class="login-form">
        <view class="form-card">
          <view class="form-title">欢迎登录</view>
          <view class="form-desc">请输入您的账号信息</view>

          <view class="form-item">
            <view class="input-label">账号</view>
            <view class="input-wrapper">
              <text class="input-icon">👤</text>
              <input
                class="input-field"
                v-model="form.username"
                placeholder="请输入用户名"
                placeholder-class="input-placeholder"
              />
            </view>
          </view>

          <view class="form-item">
            <view class="input-label">密码</view>
            <view class="input-wrapper">
              <text class="input-icon">🔒</text>
              <input
                class="input-field"
                v-model="form.password"
                type="password"
                placeholder="请输入密码"
                placeholder-class="input-placeholder"
                :password="!showPassword"
              />
              <text class="input-suffix" @tap="showPassword = !showPassword">
                {{ showPassword ? '🙈' : '👁️' }}
              </text>
            </view>
          </view>

          <view class="form-options">
            <view class="remember-me" @tap="rememberMe = !rememberMe">
              <view class="checkbox" :class="{ checked: rememberMe }">
                <text v-if="rememberMe" class="check-icon">✓</text>
              </view>
              <text class="remember-text">记住账号</text>
            </view>
            <text class="forgot-text">忘记密码？</text>
          </view>

          <view class="btn btn-primary btn-block login-btn" @tap="handleLogin">
            登录
          </view>

          <view class="quick-login">
            <view class="divider-text">
              <view class="divider-line"></view>
              <text class="divider-label">其他登录方式</text>
              <view class="divider-line"></view>
            </view>
            <view class="quick-icons">
              <view class="quick-icon">📱</view>
              <view class="quick-icon">💬</view>
              <view class="quick-icon">🔑</view>
            </view>
          </view>
        </view>
      </view>
    </view>

    <view class="login-footer">
      <text class="footer-text">© 2026 七维空间 版权所有</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'

const form = reactive({
  username: 'admin',
  password: '123456'
})

const showPassword = ref(false)
const rememberMe = ref(true)

function handleLogin() {
  uni.showToast({
    title: '登录成功',
    icon: 'success'
  })
  setTimeout(() => {
    uni.redirectTo({
      url: '/pages/dashboard/index'
    })
  }, 1000)
}
</script>

<style lang="scss" scoped>
.login-page {
  min-height: 100vh;
  background: linear-gradient(180deg, #667eea 0%, #764ba2 100%);
  position: relative;
  overflow: hidden;
}

.login-bg {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  overflow: hidden;

  .bg-circle {
    position: absolute;
    border-radius: 50%;
    background: rgba(255, 255, 255, 0.1);

    &.c1 {
      width: 400rpx;
      height: 400rpx;
      top: -100rpx;
      right: -80rpx;
    }

    &.c2 {
      width: 300rpx;
      height: 300rpx;
      top: 200rpx;
      left: -60rpx;
    }

    &.c3 {
      width: 200rpx;
      height: 200rpx;
      bottom: 200rpx;
      right: 40rpx;
    }
  }
}

.login-content {
  position: relative;
  z-index: 1;
  padding: 0 $spacing-lg;
  padding-top: calc(var(--status-bar-height) + 80rpx);
}

.logo-section {
  text-align: center;
  margin-bottom: 80rpx;

  .logo-icon {
    width: 140rpx;
    height: 140rpx;
    background: rgba(255, 255, 255, 0.2);
    border-radius: 36rpx;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 64rpx;
    font-weight: $font-weight-bold;
    color: #fff;
    margin: 0 auto $spacing-lg;
    backdrop-filter: blur(10px);
    border: 2rpx solid rgba(255, 255, 255, 0.3);
  }

  .logo-title {
    font-size: 52rpx;
    font-weight: $font-weight-bold;
    color: #fff;
    margin-bottom: $spacing-xs;
  }

  .logo-subtitle {
    font-size: $font-size-sm;
    color: rgba(255, 255, 255, 0.7);
    letter-spacing: 4rpx;
  }
}

.form-card {
  background: #fff;
  border-radius: $border-radius-xl;
  padding: $spacing-xl $spacing-lg;
  box-shadow: 0 20rpx 60rpx rgba(0, 0, 0, 0.15);

  .form-title {
    font-size: $font-size-xxl;
    font-weight: $font-weight-bold;
    color: $text-primary;
    margin-bottom: $spacing-xs;
  }

  .form-desc {
    font-size: $font-size-sm;
    color: $text-secondary;
    margin-bottom: $spacing-xl;
  }
}

.form-item {
  margin-bottom: $spacing-lg;

  .input-label {
    font-size: $font-size-sm;
    color: $text-regular;
    margin-bottom: $spacing-sm;
    font-weight: $font-weight-medium;
  }

  .input-wrapper {
    display: flex;
    align-items: center;
    height: 96rpx;
    background: $bg-page;
    border-radius: $border-radius-md;
    padding: 0 $spacing-md;
    border: 2rpx solid transparent;
    transition: all 0.2s;

    &:focus-within {
      border-color: $primary-color;
      background: #fff;
      box-shadow: 0 0 0 6rpx rgba(64, 158, 255, 0.1);
    }

    .input-icon {
      font-size: 36rpx;
      margin-right: $spacing-sm;
    }

    .input-field {
      flex: 1;
      height: 100%;
      font-size: $font-size-md;
      color: $text-primary;
    }

    .input-placeholder {
      color: $text-placeholder;
    }

    .input-suffix {
      font-size: 36rpx;
      padding: $spacing-xs;
    }
  }
}

.form-options {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: $spacing-xl;

  .remember-me {
    display: flex;
    align-items: center;

    .checkbox {
      width: 36rpx;
      height: 36rpx;
      border: 2rpx solid $border-color;
      border-radius: 8rpx;
      margin-right: $spacing-xs;
      display: flex;
      align-items: center;
      justify-content: center;
      transition: all 0.2s;

      &.checked {
        background: $primary-color;
        border-color: $primary-color;
      }

      .check-icon {
        color: #fff;
        font-size: 24rpx;
        font-weight: bold;
      }
    }

    .remember-text {
      font-size: $font-size-sm;
      color: $text-regular;
    }
  }

  .forgot-text {
    font-size: $font-size-sm;
    color: $primary-color;
  }
}

.login-btn {
  margin-bottom: $spacing-xl;
  font-size: $font-size-lg;
  height: 96rpx;
}

.quick-login {
  .divider-text {
    display: flex;
    align-items: center;
    margin-bottom: $spacing-lg;

    .divider-line {
      flex: 1;
      height: 1rpx;
      background: $border-color;
    }

    .divider-label {
      font-size: $font-size-xs;
      color: $text-secondary;
      padding: 0 $spacing-md;
    }
  }

  .quick-icons {
    display: flex;
    justify-content: center;
    gap: $spacing-xl;

    .quick-icon {
      width: 80rpx;
      height: 80rpx;
      background: $bg-page;
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 40rpx;
      transition: all 0.2s;

      &:active {
        transform: scale(0.9);
        background: $primary-light;
      }
    }
  }
}

.login-footer {
  position: absolute;
  bottom: calc(40rpx + env(safe-area-inset-bottom));
  left: 0;
  right: 0;
  text-align: center;

  .footer-text {
    font-size: $font-size-xs;
    color: rgba(255, 255, 255, 0.6);
  }
}
</style>
