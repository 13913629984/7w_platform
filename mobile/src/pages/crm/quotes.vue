<template>
  <view class="quotes-page">
    <view class="page-header">
      <view class="nav-bar">
        <view class="nav-back" @tap="goBack">
          <text class="back-icon">‹</text>
        </view>
        <text class="nav-title">报价管理</text>
        <view class="nav-placeholder"></view>
      </view>
      <view class="header-stats">
        <view class="stat-item">
          <text class="stat-num">{{ stats.total }}</text>
          <text class="stat-label">报价总数</text>
        </view>
        <view class="stat-divider"></view>
        <view class="stat-item">
          <text class="stat-num text-warning">¥{{ stats.totalAmount }}</text>
          <text class="stat-label">报价总额</text>
        </view>
        <view class="stat-divider"></view>
        <view class="stat-item">
          <text class="stat-num text-success">{{ stats.acceptedRate }}%</text>
          <text class="stat-label">接受率</text>
        </view>
      </view>
    </view>

    <view class="search-section">
      <view class="search-box">
        <text class="search-icon">🔍</text>
        <input
          class="search-input"
          v-model="keyword"
          placeholder="搜索报价单号、客户"
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

    <view class="tabs-bar">
      <view
        v-for="(tab, index) in tabs"
        :key="index"
        class="tab-item"
        :class="{ active: activeTab === index }"
        @tap="activeTab = index"
      >
        <text class="tab-text">{{ tab }}</text>
        <view v-if="activeTab === index" class="tab-indicator"></view>
      </view>
    </view>

    <scroll-view
      class="quote-list"
      scroll-y
      @scrolltolower="loadMore"
    >
      <view class="list-section">
        <view
          v-for="quote in filteredQuotes"
          :key="quote.id"
          class="quote-card"
          @tap="goToDetail(quote)"
        >
          <view class="card-top">
            <view class="quote-no">
              <text class="no-label">报价单号</text>
              <text class="no-value">{{ quote.quoteNo }}</text>
            </view>
            <view class="status-tag" :class="'status-' + quote.statusCode">
              {{ quote.status }}
            </view>
          </view>

          <view class="card-body">
            <view class="customer-row">
              <view class="customer-avatar">{{ quote.customer.charAt(0) }}</view>
              <view class="customer-info">
                <text class="customer-name">{{ quote.customer }}</text>
                <text class="quote-title">{{ quote.title }}</text>
              </view>
            </view>

            <view class="amount-row">
              <view class="amount-main">
                <text class="amount-label">报价金额</text>
                <text class="amount-value">¥{{ quote.amount }}</text>
              </view>
              <view class="amount-sub">
                <text class="sub-item">共 {{ quote.itemCount }} 项</text>
                <text class="sub-dot">·</text>
                <text class="sub-item">有效期至 {{ quote.validUntil }}</text>
              </view>
            </view>
          </view>

          <view class="card-footer">
            <view class="footer-info">
              <text class="info-item">👤 {{ quote.owner }}</text>
              <text class="info-item">📅 {{ quote.createdAt }}</text>
            </view>
            <view class="footer-actions">
              <view class="action-btn" @tap.stop="shareQuote(quote)">
                <text class="action-icon">📤</text>
              </view>
              <view class="action-btn" @tap.stop="printQuote(quote)">
                <text class="action-icon">🖨️</text>
              </view>
              <view class="action-btn primary" @tap.stop="editQuote(quote)">
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

    <view class="fab-btn" @tap="addQuote">
      <text class="fab-icon">➕</text>
    </view>

    <view v-if="showFilter" class="filter-popup" @tap="showFilter = false">
      <view class="filter-panel" @tap.stop>
        <view class="panel-header">
          <text class="panel-title">筛选条件</text>
          <text class="panel-reset" @tap="resetFilter">重置</text>
        </view>

        <view class="filter-group">
          <text class="group-label">报价状态</text>
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

        <view class="filter-group">
          <text class="group-label">创建时间</text>
          <view class="tag-group">
            <view
              v-for="range in dateRanges"
              :key="range"
              class="filter-tag"
              :class="{ active: filterForm.dateRange === range }"
              @tap="filterForm.dateRange = filterForm.dateRange === range ? '' : range"
            >
              {{ range }}
            </view>
          </view>
        </view>

        <view class="filter-group">
          <text class="group-label">负责人</text>
          <view class="tag-group">
            <view
              v-for="o in owners"
              :key="o"
              class="filter-tag"
              :class="{ active: filterForm.owner === o }"
              @tap="filterForm.owner = filterForm.owner === o ? '' : o"
            >
              {{ o }}
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
import { ref, reactive, computed } from 'vue'

const keyword = ref('')
const showFilter = ref(false)
const loading = ref(false)
const noMore = ref(false)
const activeTab = ref(0)

const tabs = ['全部', '待审批', '已发送', '已接受', '已拒绝', '已过期']

const stats = reactive({
  total: 36,
  totalAmount: '2,680万',
  acceptedRate: 42
})

const statusList = ['草稿', '待审批', '已发送', '已接受', '已拒绝', '已过期']
const dateRanges = ['本周', '本月', '本季度', '今年']
const owners = ['张三', '李四', '王五', '赵六']

const filterForm = reactive({
  status: '',
  dateRange: '',
  owner: ''
})

const quotes = ref([
  {
    id: 1,
    quoteNo: 'QT20260628001',
    title: '智能制造MES系统报价单',
    customer: '广州智能制造集团',
    amount: '380.00万',
    itemCount: 15,
    validUntil: '2026-07-28',
    status: '已发送',
    statusCode: 'sent',
    owner: '李四',
    createdAt: '2026-06-28'
  },
  {
    id: 2,
    quoteNo: 'QT20260627003',
    title: '企业ERP系统升级报价',
    customer: '深圳科技创新有限公司',
    amount: '268.00万',
    itemCount: 22,
    validUntil: '2026-07-27',
    status: '待审批',
    statusCode: 'pending',
    owner: '张三',
    createdAt: '2026-06-27'
  },
  {
    id: 3,
    quoteNo: 'QT20260625002',
    title: 'WMS仓储管理系统报价',
    customer: '东莞电子科技有限公司',
    amount: '156.00万',
    itemCount: 18,
    validUntil: '2026-07-25',
    status: '已接受',
    statusCode: 'accepted',
    owner: '王五',
    createdAt: '2026-06-25'
  },
  {
    id: 4,
    quoteNo: 'QT20260620001',
    title: '智能照明控制系统报价',
    customer: '中山照明科技有限公司',
    amount: '220.00万',
    itemCount: 12,
    validUntil: '2026-07-20',
    status: '已接受',
    statusCode: 'accepted',
    owner: '赵六',
    createdAt: '2026-06-20'
  },
  {
    id: 5,
    quoteNo: 'QT20260618004',
    title: 'CRM客户关系管理系统报价',
    customer: '佛山机械设备有限公司',
    amount: '88.00万',
    itemCount: 10,
    validUntil: '2026-07-18',
    status: '已拒绝',
    statusCode: 'rejected',
    owner: '张三',
    createdAt: '2026-06-18'
  },
  {
    id: 6,
    quoteNo: 'QT20260615002',
    title: 'OA办公系统定制开发报价',
    customer: '珠海信息技术有限公司',
    amount: '168.00万',
    itemCount: 20,
    validUntil: '2026-07-15',
    status: '草稿',
    statusCode: 'draft',
    owner: '李四',
    createdAt: '2026-06-15'
  }
])

const filteredQuotes = computed(() => {
  return quotes.value
})

function goBack() {
  uni.navigateBack()
}

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

function goToDetail(quote: any) {
  uni.showToast({ title: '查看报价详情', icon: 'none' })
}

function shareQuote(quote: any) {
  uni.showToast({ title: '分享报价单', icon: 'none' })
}

function printQuote(quote: any) {
  uni.showToast({ title: '打印报价单', icon: 'none' })
}

function editQuote(quote: any) {
  uni.showToast({ title: '编辑报价单', icon: 'none' })
}

function addQuote() {
  uni.showToast({ title: '新建报价单', icon: 'none' })
}

function resetFilter() {
  filterForm.status = ''
  filterForm.dateRange = ''
  filterForm.owner = ''
}

function applyFilter() {
  showFilter.value = false
  uni.showToast({ title: '筛选已应用', icon: 'success' })
}
</script>

<style lang="scss" scoped>
.quotes-page {
  min-height: 100vh;
  background: $bg-page;
  display: flex;
  flex-direction: column;
}

.page-header {
  background: linear-gradient(135deg, #67c23a, #52c41a);
  padding-bottom: $spacing-xl;

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

  .header-stats {
    display: flex;
    align-items: center;
    justify-content: space-around;
    padding: 0 $spacing-lg;

    .stat-item {
      text-align: center;

      .stat-num {
        display: block;
        font-size: $font-size-xl;
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

.tabs-bar {
  display: flex;
  align-items: center;
  background: #fff;
  padding: 0 $spacing-sm;
  border-bottom: 1rpx solid $border-color-light;
  overflow-x: auto;

  .tab-item {
    flex-shrink: 0;
    display: flex;
    flex-direction: column;
    align-items: center;
    padding: $spacing-md $spacing-lg;
    position: relative;

    .tab-text {
      font-size: $font-size-sm;
      color: $text-regular;
      transition: all 0.2s;
      white-space: nowrap;
    }

    .tab-indicator {
      position: absolute;
      bottom: 0;
      left: 50%;
      transform: translateX(-50%);
      width: 48rpx;
      height: 6rpx;
      background: #67c23a;
      border-radius: 3rpx;
    }

    &.active {
      .tab-text {
        color: #67c23a;
        font-weight: $font-weight-semibold;
      }
    }
  }
}

.quote-list {
  flex: 1;
  height: 0;
}

.list-section {
  padding: $spacing-md;
}

.quote-card {
  background: #fff;
  border-radius: $border-radius-lg;
  padding: $spacing-lg;
  margin-bottom: $spacing-md;
  box-shadow: $shadow-sm;

  .card-top {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: $spacing-md;
    padding-bottom: $spacing-md;
    border-bottom: 1rpx solid $border-color-light;

    .quote-no {
      .no-label {
        font-size: $font-size-xs;
        color: $text-secondary;
        margin-right: $spacing-xs;
      }

      .no-value {
        font-size: $font-size-sm;
        font-weight: $font-weight-semibold;
        color: $text-primary;
        font-family: 'Courier New', monospace;
      }
    }

    .status-tag {
      padding: 6rpx 20rpx;
      border-radius: 20rpx;
      font-size: 22rpx;
      font-weight: $font-weight-medium;

      &.status-draft {
        background: $info-light;
        color: $info-color;
      }
      &.status-pending {
        background: $warning-light;
        color: $warning-color;
      }
      &.status-sent {
        background: $primary-light;
        color: $primary-color;
      }
      &.status-accepted {
        background: $success-light;
        color: $success-color;
      }
      &.status-rejected {
        background: $danger-light;
        color: $danger-color;
      }
      &.status-expired {
        background: $info-light;
        color: $info-color;
      }
    }
  }

  .card-body {
    margin-bottom: $spacing-md;

    .customer-row {
      display: flex;
      align-items: center;
      margin-bottom: $spacing-md;

      .customer-avatar {
        width: 64rpx;
        height: 64rpx;
        border-radius: 50%;
        background: linear-gradient(135deg, #67c23a, #52c41a);
        color: #fff;
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: $font-size-md;
        font-weight: $font-weight-bold;
        margin-right: $spacing-sm;
        flex-shrink: 0;
      }

      .customer-info {
        flex: 1;
        min-width: 0;

        .customer-name {
          display: block;
          font-size: $font-size-md;
          font-weight: $font-weight-semibold;
          color: $text-primary;
          margin-bottom: 4rpx;
          overflow: hidden;
          text-overflow: ellipsis;
          white-space: nowrap;
        }

        .quote-title {
          font-size: $font-size-sm;
          color: $text-regular;
          overflow: hidden;
          text-overflow: ellipsis;
          white-space: nowrap;
        }
      }
    }

    .amount-row {
      background: linear-gradient(135deg, rgba(103, 194, 58, 0.08), rgba(82, 196, 26, 0.08));
      border-radius: $border-radius-md;
      padding: $spacing-md;

      .amount-main {
        display: flex;
        align-items: baseline;
        justify-content: space-between;
        margin-bottom: 6rpx;

        .amount-label {
          font-size: $font-size-xs;
          color: $text-secondary;
        }

        .amount-value {
          font-size: $font-size-xl;
          font-weight: $font-weight-bold;
          color: #67c23a;
        }
      }

      .amount-sub {
        display: flex;
        align-items: center;
        gap: $spacing-xs;

        .sub-item {
          font-size: $font-size-xs;
          color: $text-secondary;
        }

        .sub-dot {
          color: $text-placeholder;
        }
      }
    }
  }

  .card-footer {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding-top: $spacing-md;
    border-top: 1rpx solid $border-color-light;

    .footer-info {
      display: flex;
      flex-direction: column;
      gap: 6rpx;

      .info-item {
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
          background: $success-light;
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
  bottom: calc(#{$spacing-lg} + env(safe-area-inset-bottom));
  width: 112rpx;
  height: 112rpx;
  background: linear-gradient(135deg, #67c23a, #52c41a);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 8rpx 24rpx rgba(103, 194, 58, 0.4);
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
