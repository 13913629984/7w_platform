<template>
  <view class="tab-bar">
    <view
      v-for="item in tabs"
      :key="item.path"
      class="tab-item"
      :class="{ active: currentPath === item.path }"
      @tap="switchTab(item)"
    >
      <view class="tab-icon">
        <text class="icon-text">{{ item.icon }}</text>
      </view>
      <text class="tab-text">{{ item.text }}</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'

const props = defineProps<{
  current?: string
}>()

const emit = defineEmits(['change'])

const currentPath = ref(props.current || '/pages/dashboard/index')

const tabs = [
  { path: '/pages/dashboard/index', text: '工作台', icon: '🏠' },
  { path: '/pages/crm/customers', text: 'CRM', icon: '👥' },
  { path: '/pages/wms/overview', text: 'WMS', icon: '📦' },
  { path: '/pages/mine/index', text: '我的', icon: '👤' }
]

function switchTab(item: any) {
  if (currentPath.value === item.path) return
  currentPath.value = item.path
  emit('change', item.path)
  uni.redirectTo({
    url: item.path
  })
}

onMounted(() => {
  const pages = getCurrentPages()
  if (pages.length) {
    const page = pages[pages.length - 1]
    currentPath.value = '/' + page.route
  }
})
</script>

<style lang="scss" scoped>
.tab-bar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  align-items: center;
  justify-content: space-around;
  height: 100rpx;
  background: #fff;
  box-shadow: 0 -4rpx 20rpx rgba(0, 0, 0, 0.06);
  padding-bottom: env(safe-area-inset-bottom);
  z-index: 999;

  .tab-item {
    flex: 1;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    transition: all 0.2s;

    .tab-icon {
      width: 48rpx;
      height: 48rpx;
      display: flex;
      align-items: center;
      justify-content: center;
      margin-bottom: 4rpx;

      .icon-text {
        font-size: 40rpx;
      }
    }

    .tab-text {
      font-size: 22rpx;
      color: #909399;
      transition: all 0.2s;
    }

    &.active {
      .tab-icon {
        transform: translateY(-4rpx);
      }

      .tab-text {
        color: #409eff;
        font-weight: 500;
      }
    }
  }
}
</style>
