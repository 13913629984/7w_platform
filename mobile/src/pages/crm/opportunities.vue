<template>
  <view class="opportunities-page">
    <view class="page-header">
      <view class="nav-bar">
        <view class="nav-back" @tap="goBack">
          <text class="back-icon">‹</text>
        </view>
        <text class="nav-title">商机管理</text>
        <view class="nav-placeholder"></view>
      </view>
      <view class="header-stats">
        <view class="stat-item">
          <text class="stat-num">{{ stats.total }}</text>
          <text class="stat-label">商机总数</text>
        </view>
        <view class="stat-divider"></view>
        <view class="stat-item">
          <text class="stat-num text-warning">¥{{ stats.amount }}</text>
          <text class="stat-label">预计金额</text>
        </view>
        <view class="stat-divider"></view>
        <view class="stat-item">
          <text class="stat-num text-success">{{ stats.wonRate }}%</text>
          <text class="stat-label">赢单率</text>
        </view>
      </view>
    </view>

    <view class="search-section">
      <view class="search-box">
        <text class="search-icon">🔍</text>
        <input
          class="search-input"
          v-model="keyword"
          placeholder="搜索商机名称、客户"
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

    <view class="stage-bar">
      <scroll-view class="stage-scroll" scroll-x show-scrollbar="{{false}}">
        <view class="stage-list">
          <view
            v-for="(stage, index) in stages"
            :key="index"
            class="stage-item"
            :class="{ active: activeStage === index }"
            @tap="activeStage = index"
          >
            <text class="stage-name">{{ stage.name }}</text>
            <text class="stage-count">{{ stage.count }}</text>
          </view>
        </view>
      </scroll-view>
    </view>

    <scroll-view
      class="opportunity-list"
      scroll-y
      @scrolltolower="loadMore"
    >
      <view class="list-section">
        <view
          v-for="opp in filteredOpportunities"
          :key="opp.id"
          class="opportunity-card"
          @tap="goToDetail(opp)"
        >
          <view class="card-header">
            <view class="opp-main">
              <text class="opp-name">{{ opp.name }}</text>
              <view class="opp-tag-row">
                <view class="stage-tag" :class="'stage-' + opp.stageCode">
                  {{ opp.stage }}
                </view>
                <view class="priority-tag" :class="'priority-' + opp.priorityCode">
                  {{ opp.priority }}
                </view>
              </view>
            </view>
            <text class="opp-amount">¥{{ opp.amount }}</text>
          </view>

          <view class="card-body">
            <view class="customer-row">
              <view class="customer-avatar">{{ opp.customer.charAt(0) }}</view>
              <view class="customer-info">
                <text class="customer-name">{{ opp.customer }}</text>
                <text class="contact-name">{{ opp.contact }}</text>
              </view>
            </view>

            <view class="progress-section">
              <view class="progress-header">
                <text class="progress-label">赢单概率</text>
                <text class="progress-value">{{ opp.probability }}%</text>
              </view>
              <view class="progress-bar">
                <view class="progress-fill" :style="{ width: opp.probability + '%' }"></view>
              </view>
            </view>
          </view>

          <view class="card-footer">
            <view class="footer-info">
              <text class="info-item">👤 {{ opp.owner }}</text>
              <text class="info-item">📅 预计{{ opp.expectedCloseAt }}</text>
            </view>
            <view class="footer-actions">
              <view class="action-btn" @tap.stop="callContact(opp)">
                <text class="action-icon">📞</text>
              </view>
              <view class="action-btn primary" @tap.stop="advanceStage(opp)">
                <text class="action-icon">➡️</text>
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

    <view class="fab-btn" @tap="addOpportunity">
      <text class="fab-icon">➕</text>
    </view>

    <view v-if="showFilter" class="filter-popup" @tap="showFilter = false">
      <view class="filter-panel" @tap.stop>
        <view class="panel-header">
          <text class="panel-title">筛选条件</text>
          <text class="panel-reset" @tap="resetFilter">重置</text>
        </view>

        <view class="filter-group">
          <text class="group-label">商机阶段</text>
          <view class="tag-group">
            <view
              v-for="s in stageFilters"
              :key="s"
              class="filter-tag"
              :class="{ active: filterForm.stage === s }"
              @tap="filterForm.stage = filterForm.stage === s ? '' : s"
            >
              {{ s }}
            </view>
          </view>
        </view>

        <view class="filter-group">
          <text class="group-label">优先级</text>
          <view class="tag-group">
            <view
              v-for="p in priorities"
              :key="p"
              class="filter-tag"
              :class="{ active: filterForm.priority === p }"
              @tap="filterForm.priority = filterForm.priority === p ? '' : p"
            >
              {{ p }}
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
const activeStage = ref(0)

const stages = [
  { name: '全部', count: 42 },
  { name: '初步接触', count: 8 },
  { name: '需求确认', count: 12 },
  { name: '方案报价', count: 10 },
  { name: '商务谈判', count: 7 },
  { name: '赢单', count: 5 }
]

const stats = reactive({
  total: 42,
  amount: '1,280万',
  wonRate: 35
})

const stageFilters = ['初步接触', '需求确认', '方案报价', '商务谈判', '赢单', '输单']
const priorities = ['高', '中', '低']
const owners = ['张三', '李四', '王五', '赵六']

const filterForm = reactive({
  stage: '',
  priority: '',
  owner: ''
})

const opportunities = ref([
  {
    id: 1,
    name: '智能制造MES系统采购项目',
    customer: '广州智能制造集团',
    contact: '王总 139****6666',
    amount: '380万',
    stage: '商务谈判',
    stageCode: 'negotiation',
    priority: '高',
    priorityCode: 'high',
    probability: 75,
    owner: '李四',
    expectedCloseAt: '2026-07-15'
  },
  {
    id: 2,
    name: '企业ERP系统升级',
    customer: '深圳科技创新有限公司',
    contact: '李经理 138****8888',
    amount: '268万',
    stage: '方案报价',
    stageCode: 'quotation',
    priority: '高',
    priorityCode: 'high',
    probability: 50,
    owner: '张三',
    expectedCloseAt: '2026-07-30'
  },
  {
    id: 3,
    name: 'WMS仓储管理系统建设',
    customer: '东莞电子科技有限公司',
    contact: '陈经理 137****5555',
    amount: '156万',
    stage: '需求确认',
    stageCode: 'requirement',
    priority: '中',
    priorityCode: 'medium',
    probability: 35,
    owner: '王五',
    expectedCloseAt: '2026-08-10'
  },
  {
    id: 4,
    name: 'CRM客户关系管理系统',
    customer: '佛山机械设备有限公司',
    contact: '刘工 136****4444',
    amount: '88万',
    stage: '初步接触',
    stageCode: 'initial',
    priority: '中',
    priorityCode: 'medium',
    probability: 20,
    owner: '张三',
    expectedCloseAt: '2026-09-01'
  },
  {
    id: 5,
    name: '智能照明控制系统',
    customer: '中山照明科技有限公司',
    contact: '周经理 135****3333',
    amount: '220万',
    stage: '赢单',
    stageCode: 'won',
    priority: '高',
    priorityCode: 'high',
    probability: 100,
    owner: '赵六',
    expectedCloseAt: '2026-06-25'
  },
  {
    id: 6,
    name: 'OA办公系统定制开发',
    customer: '珠海信息技术有限公司',
    contact: '吴总 134****2222',
    amount: '168万',
    stage: '商务谈判',
    stageCode: 'negotiation',
    priority: '低',
    priorityCode: 'low',
    probability: 60,
    owner: '李四',
    expectedCloseAt: '2026-08-20'
  }
])

const filteredOpportunities = computed(() => {
  return opportunities.value
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

function goToDetail(opp: any) {
  uni.showToast({ title: '查看商机详情', icon: 'none' })
}

function callContact(opp: any) {
  uni.showToast({ title: '拨打电话', icon: 'none' })
}

function advanceStage(opp: any) {
  uni.showToast({ title: '推进阶段', icon: 'none' })
}

function addOpportunity() {
  uni.showToast({ title: '新增商机', icon: 'none' })
}

function resetFilter() {
  filterForm.stage = ''
  filterForm.priority = ''
  filterForm.owner = ''
}

function applyFilter() {
  showFilter.value = false
  uni.showToast({ title: '筛选已应用', icon: 'success' })
}
</script>

<style lang="scss" scoped>
.opportunities-page {
  min-height: 100vh;
  background: $bg-page;
  display: flex;
  flex-direction: column;
}

.page-header {
  background: linear-gradient(135deg, #667eea, #764ba2);
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

.stage-bar {
  background: #fff;
  border-bottom: 1rpx solid $border-color-light;

  .stage-scroll {
    white-space: nowrap;
  }

  .stage-list {
    display: inline-flex;
    padding: 0 $spacing-md;

    .stage-item {
      display: inline-flex;
      flex-direction: column;
      align-items: center;
      padding: $spacing-md $spacing-lg;
      position: relative;

      .stage-name {
        font-size: $font-size-sm;
        color: $text-regular;
        margin-bottom: 4rpx;
      }

      .stage-count {
        font-size: $font-size-xs;
        color: $text-secondary;
        background: $bg-page;
        padding: 2rpx 16rpx;
        border-radius: 20rpx;
      }

      &.active {
        .stage-name {
          color: #667eea;
          font-weight: $font-weight-semibold;
        }

        .stage-count {
          background: rgba(102, 126, 234, 0.1);
          color: #667eea;
        }
      }
    }
  }
}

.opportunity-list {
  flex: 1;
  height: 0;
}

.list-section {
  padding: $spacing-md;
}

.opportunity-card {
  background: #fff;
  border-radius: $border-radius-lg;
  padding: $spacing-lg;
  margin-bottom: $spacing-md;
  box-shadow: $shadow-sm;

  .card-header {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    margin-bottom: $spacing-md;

    .opp-main {
      flex: 1;
      min-width: 0;

      .opp-name {
        display: block;
        font-size: $font-size-md;
        font-weight: $font-weight-semibold;
        color: $text-primary;
        margin-bottom: $spacing-sm;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      }

      .opp-tag-row {
        display: flex;
        gap: $spacing-sm;

        .stage-tag,
        .priority-tag {
          padding: 4rpx 16rpx;
          border-radius: 20rpx;
          font-size: 20rpx;
          font-weight: $font-weight-medium;

          &.stage-initial {
            background: $info-light;
            color: $info-color;
          }
          &.stage-requirement {
            background: $warning-light;
            color: $warning-color;
          }
          &.stage-quotation {
            background: $primary-light;
            color: $primary-color;
          }
          &.stage-negotiation {
            background: linear-gradient(135deg, rgba(102, 126, 234, 0.1), rgba(118, 75, 162, 0.1));
            color: #667eea;
          }
          &.stage-won {
            background: $success-light;
            color: $success-color;
          }

          &.priority-high {
            background: $danger-light;
            color: $danger-color;
          }
          &.priority-medium {
            background: $warning-light;
            color: $warning-color;
          }
          &.priority-low {
            background: $info-light;
            color: $info-color;
          }
        }
      }
    }

    .opp-amount {
      font-size: $font-size-lg;
      font-weight: $font-weight-bold;
      color: #667eea;
      margin-left: $spacing-md;
      flex-shrink: 0;
    }
  }

  .card-body {
    padding: $spacing-md 0;
    border-top: 1rpx solid $border-color-light;
    border-bottom: 1rpx solid $border-color-light;

    .customer-row {
      display: flex;
      align-items: center;
      margin-bottom: $spacing-md;

      .customer-avatar {
        width: 64rpx;
        height: 64rpx;
        border-radius: 50%;
        background: linear-gradient(135deg, #f56c6c, #e74c3c);
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
          font-size: $font-size-sm;
          font-weight: $font-weight-medium;
          color: $text-primary;
          margin-bottom: 4rpx;
          overflow: hidden;
          text-overflow: ellipsis;
          white-space: nowrap;
        }

        .contact-name {
          font-size: $font-size-xs;
          color: $text-secondary;
        }
      }
    }

    .progress-section {
      .progress-header {
        display: flex;
        align-items: center;
        justify-content: space-between;
        margin-bottom: $spacing-xs;

        .progress-label {
          font-size: $font-size-xs;
          color: $text-secondary;
        }

        .progress-value {
          font-size: $font-size-xs;
          font-weight: $font-weight-semibold;
          color: #667eea;
        }
      }

      .progress-bar {
        width: 100%;
        height: 12rpx;
        background: $bg-page;
        border-radius: 6rpx;
        overflow: hidden;

        .progress-fill {
          height: 100%;
          background: linear-gradient(90deg, #667eea, #764ba2);
          border-radius: 6rpx;
          transition: width 0.3s;
        }
      }
    }
  }

  .card-footer {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding-top: $spacing-md;

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
          background: linear-gradient(135deg, rgba(102, 126, 234, 0.1), rgba(118, 75, 162, 0.1));
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
