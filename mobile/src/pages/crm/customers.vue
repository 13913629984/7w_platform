<template>
  <view class="customers-page">
    <view class="page-header">
      <view class="header-stats">
        <view class="stat-item">
          <text class="stat-num">{{ stats.total }}</text>
          <text class="stat-label">客户总数</text>
        </view>
        <view class="stat-divider"></view>
        <view class="stat-item">
          <text class="stat-num text-success">{{ stats.monthAdded }}</text>
          <text class="stat-label">本月新增</text>
        </view>
        <view class="stat-divider"></view>
        <view class="stat-item">
          <text class="stat-num text-primary">{{ stats.active }}</text>
          <text class="stat-label">活跃客户</text>
        </view>
      </view>
    </view>

    <view class="search-section">
      <view class="search-box">
        <text class="search-icon">🔍</text>
        <input
          class="search-input"
          v-model="keyword"
          placeholder="搜索客户名称、联系人"
          placeholder-class="search-placeholder"
          @input="handleSearch"
        />
        <text v-if="keyword" class="clear-btn" @tap="clearSearch">✕</text>
      </view>
      <view class="filter-btn" @tap="showFilter = true">
        <text class="filter-icon">⚙️</text>
        <text class="filter-text">筛选</text>
      </view>
    </view>

    <view class="module-grid">
      <view class="module-item" @tap="goToModule('dashboard')">
        <view class="module-icon icon-dashboard">
          <text class="icon-emoji">📊</text>
        </view>
        <text class="module-name">CRM看板</text>
      </view>
      <view class="module-item" @tap="goToModule('visits')">
        <view class="module-icon icon-visits">
          <text class="icon-emoji">🚶</text>
        </view>
        <text class="module-name">拜访管理</text>
      </view>
      <view class="module-item" @tap="goToModule('opportunities')">
        <view class="module-icon icon-opportunities">
          <text class="icon-emoji">💼</text>
        </view>
        <text class="module-name">商机管理</text>
      </view>
      <view class="module-item" @tap="goToModule('quotes')">
        <view class="module-icon icon-quotes">
          <text class="icon-emoji">📄</text>
        </view>
        <text class="module-name">报价管理</text>
      </view>
    </view>

    <scroll-view
      class="customer-list"
      scroll-y
      @scrolltolower="loadMore"
    >
      <view class="list-section">
        <view class="section-label">
          <text class="label-text">全部客户</text>
          <text class="label-count">{{ customers.length }} 家</text>
        </view>

        <view
          v-for="customer in customers"
          :key="customer.id"
          class="customer-card"
          @tap="goToDetail(customer)"
        >
          <view class="card-header">
            <view class="customer-avatar" :class="'level-' + customer.levelCode">
              {{ customer.name.charAt(0) }}
            </view>
            <view class="customer-info">
              <view class="customer-name">
                <text class="name-text">{{ customer.name }}</text>
                <view class="level-tag" :class="'tag-level-' + customer.levelCode">
                  {{ customer.level }}
                </view>
              </view>
              <view class="customer-owner">
                <text class="owner-label">负责人：</text>
                <text class="owner-name">{{ customer.owner }}</text>
              </view>
            </view>
            <text class="card-arrow">›</text>
          </view>

          <view class="card-body">
            <view class="info-row">
              <text class="info-label">📞 联系人</text>
              <text class="info-value">{{ customer.contact }}</text>
            </view>
            <view class="info-row">
              <text class="info-label">📍 地址</text>
              <text class="info-value ellipsis">{{ customer.address }}</text>
            </view>
          </view>

          <view class="card-footer">
            <view class="footer-left">
              <text class="last-visit">最近成交：{{ customer.lastDealAt }}</text>
            </view>
            <view class="footer-actions">
              <view class="action-btn" @tap.stop="callCustomer(customer)">
                <text class="action-icon">📞</text>
              </view>
              <view class="action-btn primary" @tap.stop="editCustomer(customer)">
                <text class="action-icon">✏️</text>
              </view>
            </view>
          </view>
        </view>
      </view>

      <view class="load-more">
        <text v-if="loading" class="loading-text">加载中...</text>
        <text v-else-if="noMore" class="no-more-text">— 没有更多了 —</text>
      </view>

      <view class="bottom-space"></view>
    </scroll-view>

    <TabBar current="/pages/crm/customers" />

    <view class="fab-btn" @tap="addCustomer">
      <text class="fab-icon">➕</text>
    </view>

    <view v-if="showFilter" class="filter-popup" @tap="showFilter = false">
      <view class="filter-panel" @tap.stop>
        <view class="panel-header">
          <text class="panel-title">筛选条件</text>
          <text class="panel-reset" @tap="resetFilter">重置</text>
        </view>

        <view class="filter-group">
          <text class="group-label">客户等级</text>
          <view class="tag-group">
            <view
              v-for="level in levels"
              :key="level"
              class="filter-tag"
              :class="{ active: filterForm.level === level }"
              @tap="filterForm.level = filterForm.level === level ? '' : level"
            >
              {{ level }}
            </view>
          </view>
        </view>

        <view class="filter-group">
          <text class="group-label">客户状态</text>
          <view class="tag-group">
            <view
              v-for="status in statusList"
              :key="status"
              class="filter-tag"
              :class="{ active: filterForm.status === status }"
              @tap="filterForm.status = filterForm.status === status ? '' : status"
            >
              {{ status }}
            </view>
          </view>
        </view>

        <view class="panel-footer">
          <view class="btn btn-outline" @tap="showFilter = false">取消</view>
          <view class="btn btn-primary" @tap="applyFilter">确定</view>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import TabBar from '@/components/TabBar.vue'

const keyword = ref('')
const showFilter = ref(false)
const loading = ref(false)
const noMore = ref(false)

const stats = reactive({
  total: 386,
  monthAdded: 28,
  active: 256
})

const levels = ['A级', 'B级', 'C级', 'D级']
const statusList = ['合作中', '潜在', '已流失', '待跟进']

const filterForm = reactive({
  level: '',
  status: ''
})

const customers = ref([
  {
    id: 1,
    name: '深圳科技创新有限公司',
    level: 'A级',
    levelCode: 'a',
    owner: '张三',
    contact: '李经理 138****8888',
    address: '深圳市南山区科技园南区',
    lastDealAt: '2026-06-20',
    status: '合作中'
  },
  {
    id: 2,
    name: '广州智能制造集团',
    level: 'A级',
    levelCode: 'a',
    owner: '李四',
    contact: '王总 139****6666',
    address: '广州市黄埔区科学城',
    lastDealAt: '2026-06-18',
    status: '合作中'
  },
  {
    id: 3,
    name: '东莞电子科技有限公司',
    level: 'B级',
    levelCode: 'b',
    owner: '王五',
    contact: '陈经理 137****5555',
    address: '东莞市松山湖高新技术产业开发区',
    lastDealAt: '2026-06-15',
    status: '合作中'
  },
  {
    id: 4,
    name: '佛山机械设备有限公司',
    level: 'B级',
    levelCode: 'b',
    owner: '张三',
    contact: '刘工 136****4444',
    address: '佛山市顺德区伦教街道',
    lastDealAt: '2026-06-10',
    status: '潜在'
  },
  {
    id: 5,
    name: '中山照明科技有限公司',
    level: 'C级',
    levelCode: 'c',
    owner: '赵六',
    contact: '周经理 135****3333',
    address: '中山市古镇镇',
    lastDealAt: '2026-05-28',
    status: '待跟进'
  },
  {
    id: 6,
    name: '珠海信息技术有限公司',
    level: 'C级',
    levelCode: 'c',
    owner: '李四',
    contact: '吴总 134****2222',
    address: '珠海市香洲区',
    lastDealAt: '2026-05-20',
    status: '已流失'
  }
])

function handleSearch() {
  console.log('search:', keyword.value)
}

function clearSearch() {
  keyword.value = ''
}

function loadMore() {
  if (noMore.value) return
  loading.value = true
  setTimeout(() => {
    loading.value = false
    noMore.value = true
  }, 1000)
}

function goToDetail(customer: any) {
  uni.navigateTo({
    url: '/pages/crm/customer-detail?id=' + customer.id
  })
}

function callCustomer(customer: any) {
  uni.showToast({ title: '拨打电话', icon: 'none' })
}

function editCustomer(customer: any) {
  uni.showToast({ title: '编辑客户', icon: 'none' })
}

function addCustomer() {
  uni.showToast({ title: '新增客户', icon: 'none' })
}

function resetFilter() {
  filterForm.level = ''
  filterForm.status = ''
}

function applyFilter() {
  showFilter.value = false
  uni.showToast({ title: '筛选已应用', icon: 'success' })
}

function goToModule(module: string) {
  const routes: Record<string, string> = {
    dashboard: '/pages/crm/dashboard',
    visits: '/pages/crm/visits',
    opportunities: '/pages/crm/opportunities',
    quotes: '/pages/crm/quotes'
  }
  const url = routes[module]
  if (!url) return
  uni.navigateTo({ url })
}
</script>

<style lang="scss" scoped>
.customers-page {
  min-height: 100vh;
  background: $bg-page;
  display: flex;
  flex-direction: column;
}

.page-header {
  background: linear-gradient(135deg, #f56c6c, #e74c3c);
  padding: calc(var(--status-bar-height) + 30rpx) $spacing-lg $spacing-xl;

  .header-stats {
    display: flex;
    align-items: center;
    justify-content: space-around;

    .stat-item {
      text-align: center;

      .stat-num {
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

    .stat-divider {
      width: 1rpx;
      height: 60rpx;
      background: rgba(255, 255, 255, 0.3);
    }
  }
}

.search-section {
  display: flex;
  align-items: center;
  gap: $spacing-sm;
  padding: $spacing-md;
  margin-top: -40rpx;
  position: relative;
  z-index: 10;

  .search-box {
    flex: 1;
    display: flex;
    align-items: center;
    height: 80rpx;
    background: #fff;
    border-radius: 40rpx;
    padding: 0 $spacing-md;
    box-shadow: $shadow-md;

    .search-icon {
      font-size: 32rpx;
      margin-right: $spacing-sm;
    }

    .search-input {
      flex: 1;
      height: 100%;
      font-size: $font-size-sm;
      color: $text-primary;
    }

    .search-placeholder {
      color: $text-placeholder;
    }

    .clear-btn {
      font-size: 28rpx;
      color: $text-secondary;
      padding: $spacing-xs;
    }
  }

  .filter-btn {
    display: flex;
    align-items: center;
    height: 80rpx;
    padding: 0 $spacing-md;
    background: #fff;
    border-radius: 40rpx;
    box-shadow: $shadow-md;

    .filter-icon {
      font-size: 32rpx;
      margin-right: 8rpx;
    }

    .filter-text {
      font-size: $font-size-sm;
      color: $text-regular;
    }
  }
}

.customer-list {
  flex: 1;
  height: 0;
}

.module-grid {
  display: flex;
  justify-content: space-around;
  padding: $spacing-md $spacing-lg;
  background: #fff;
  margin: 0 $spacing-md $spacing-md;
  border-radius: $border-radius-lg;
  box-shadow: $shadow-sm;

  .module-item {
    display: flex;
    flex-direction: column;
    align-items: center;
    flex: 1;

    .module-icon {
      width: 96rpx;
      height: 96rpx;
      border-radius: 28rpx;
      display: flex;
      align-items: center;
      justify-content: center;
      margin-bottom: $spacing-xs;

      &.icon-dashboard { background: linear-gradient(135deg, #f56c6c, #e74c3c); }
      &.icon-visits { background: linear-gradient(135deg, #e6a23c, #d48806); }
      &.icon-opportunities { background: linear-gradient(135deg, #667eea, #764ba2); }
      &.icon-quotes { background: linear-gradient(135deg, #67c23a, #52c41a); }

      .icon-emoji {
        font-size: 40rpx;
      }
    }

    .module-name {
      font-size: $font-size-xs;
      color: $text-regular;
    }
  }
}

.list-section {
  padding: 0 $spacing-md;

  .section-label {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: $spacing-sm $spacing-xs;

    .label-text {
      font-size: $font-size-sm;
      font-weight: $font-weight-medium;
      color: $text-regular;
    }

    .label-count {
      font-size: $font-size-xs;
      color: $text-secondary;
    }
  }
}

.customer-card {
  background: #fff;
  border-radius: $border-radius-lg;
  padding: $spacing-lg;
  margin-bottom: $spacing-md;
  box-shadow: $shadow-sm;

  .card-header {
    display: flex;
    align-items: center;
    margin-bottom: $spacing-md;

    .customer-avatar {
      width: 88rpx;
      height: 88rpx;
      border-radius: 24rpx;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 40rpx;
      font-weight: $font-weight-bold;
      color: #fff;
      margin-right: $spacing-md;
      flex-shrink: 0;

      &.level-a { background: linear-gradient(135deg, #f56c6c, #e74c3c); }
      &.level-b { background: linear-gradient(135deg, #e6a23c, #d48806); }
      &.level-c { background: linear-gradient(135deg, #67c23a, #52c41a); }
      &.level-d { background: linear-gradient(135deg, #909399, #606266); }
    }

    .customer-info {
      flex: 1;
      min-width: 0;

      .customer-name {
        display: flex;
        align-items: center;
        gap: $spacing-sm;
        margin-bottom: 6rpx;

        .name-text {
          font-size: $font-size-md;
          font-weight: $font-weight-semibold;
          color: $text-primary;
          overflow: hidden;
          text-overflow: ellipsis;
          white-space: nowrap;
        }

        .level-tag {
          flex-shrink: 0;
          padding: 4rpx 16rpx;
          border-radius: 20rpx;
          font-size: 20rpx;
          font-weight: $font-weight-medium;

          &.tag-level-a {
            background: #fef0f0;
            color: #f56c6c;
          }
          &.tag-level-b {
            background: #fdf6ec;
            color: #e6a23c;
          }
          &.tag-level-c {
            background: #f0f9eb;
            color: #67c23a;
          }
          &.tag-level-d {
            background: #f4f4f5;
            color: #909399;
          }
        }
      }

      .customer-owner {
        font-size: $font-size-xs;
        color: $text-secondary;

        .owner-label {
          color: $text-secondary;
        }

        .owner-name {
          color: $text-regular;
        }
      }
    }

    .card-arrow {
      font-size: $font-size-lg;
      color: $text-placeholder;
      margin-left: $spacing-sm;
    }
  }

  .card-body {
    padding: $spacing-md 0;
    border-top: 1rpx solid $border-color-light;
    border-bottom: 1rpx solid $border-color-light;

    .info-row {
      display: flex;
      align-items: flex-start;
      margin-bottom: $spacing-sm;

      &:last-child {
        margin-bottom: 0;
      }

      .info-label {
        font-size: $font-size-xs;
        color: $text-secondary;
        width: 140rpx;
        flex-shrink: 0;
      }

      .info-value {
        flex: 1;
        font-size: $font-size-xs;
        color: $text-regular;
        min-width: 0;
      }

      .ellipsis {
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      }
    }
  }

  .card-footer {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding-top: $spacing-md;

    .footer-left {
      .last-visit {
        font-size: $font-size-xs;
        color: $text-secondary;
      }
    }

    .footer-actions {
      display: flex;
      gap: $spacing-sm;

      .action-btn {
        width: 64rpx;
        height: 64rpx;
        border-radius: 50%;
        background: $bg-page;
        display: flex;
        align-items: center;
        justify-content: center;

        &.primary {
          background: $primary-light;
        }

        .action-icon {
          font-size: 28rpx;
        }
      }
    }
  }
}

.load-more {
  padding: $spacing-lg;
  text-align: center;

  .loading-text,
  .no-more-text {
    font-size: $font-size-xs;
    color: $text-secondary;
  }
}

.bottom-space {
  height: 120rpx;
}

.fab-btn {
  position: fixed;
  right: $spacing-lg;
  bottom: calc(#{$tab-bar-height} + #{$spacing-lg} + env(safe-area-inset-bottom));
  width: 112rpx;
  height: 112rpx;
  background: linear-gradient(135deg, #667eea, #764ba2);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 8rpx 24rpx rgba(102, 126, 234, 0.4);
  z-index: 100;

  .fab-icon {
    font-size: 48rpx;
  }
}

.filter-popup {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  z-index: 1000;
  display: flex;
  align-items: flex-end;

  .filter-panel {
    width: 100%;
    background: #fff;
    border-radius: $border-radius-xl $border-radius-xl 0 0;
    padding: $spacing-lg;
    padding-bottom: calc(#{$spacing-lg} + env(safe-area-inset-bottom));

    .panel-header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      margin-bottom: $spacing-xl;

      .panel-title {
        font-size: $font-size-lg;
        font-weight: $font-weight-semibold;
        color: $text-primary;
      }

      .panel-reset {
        font-size: $font-size-sm;
        color: $primary-color;
      }
    }

    .filter-group {
      margin-bottom: $spacing-xl;

      .group-label {
        font-size: $font-size-sm;
        font-weight: $font-weight-medium;
        color: $text-regular;
        margin-bottom: $spacing-md;
        display: block;
      }

      .tag-group {
        display: flex;
        flex-wrap: wrap;
        gap: $spacing-sm;

        .filter-tag {
          padding: $spacing-sm $spacing-lg;
          background: $bg-page;
          border-radius: 32rpx;
          font-size: $font-size-sm;
          color: $text-regular;
          border: 2rpx solid transparent;
          transition: all 0.2s;

          &.active {
            background: $primary-light;
            color: $primary-color;
            border-color: $primary-color;
          }
        }
      }
    }

    .panel-footer {
      display: flex;
      gap: $spacing-md;

      .btn {
        flex: 1;
        height: 88rpx;
      }
    }
  }
}
</style>
