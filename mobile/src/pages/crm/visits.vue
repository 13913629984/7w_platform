<template>
  <view class="visits-page">
    <view class="page-header">
      <view class="nav-bar">
        <view class="nav-back" @tap="goBack">
          <text class="back-icon">‹</text>
        </view>
        <text class="nav-title">拜访管理</text>
        <view class="nav-placeholder"></view>
      </view>
      <view class="header-stats">
        <view class="stat-item">
          <text class="stat-num">{{ stats.total }}</text>
          <text class="stat-label">本月拜访</text>
        </view>
        <view class="stat-divider"></view>
        <view class="stat-item">
          <text class="stat-num text-warning">{{ stats.today }}</text>
          <text class="stat-label">今日拜访</text>
        </view>
        <view class="stat-divider"></view>
        <view class="stat-item">
          <text class="stat-num text-primary">{{ stats.planned }}</text>
          <text class="stat-label">计划中</text>
        </view>
      </view>
    </view>

    <view class="search-section">
      <view class="search-box">
        <text class="search-icon">🔍</text>
        <input
          class="search-input"
          v-model="keyword"
          placeholder="搜索客户、拜访人"
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
      class="visit-list"
      scroll-y
      @scrolltolower="loadMore"
    >
      <view class="list-section">
        <view
          v-for="visit in filteredVisits"
          :key="visit.id"
          class="visit-card"
          @tap="goToDetail(visit)"
        >
          <view class="card-top">
            <view class="visit-type" :class="'type-' + visit.typeCode">
              <text class="type-icon">{{ visit.typeIcon }}</text>
            </view>
            <view class="visit-main">
              <view class="visit-title-row">
                <text class="visit-title">{{ visit.customer }}</text>
                <view class="status-tag" :class="'status-' + visit.statusCode">
                  {{ visit.status }}
                </view>
              </view>
              <text class="visit-subject">{{ visit.subject }}</text>
            </view>
          </view>

          <view class="card-body">
            <view class="info-grid">
              <view class="info-item">
                <text class="info-icon">👤</text>
                <text class="info-text">{{ visit.contactPerson }}</text>
              </view>
              <view class="info-item">
                <text class="info-icon">⏰</text>
                <text class="info-text">{{ visit.visitTime }}</text>
              </view>
              <view class="info-item">
                <text class="info-icon">📍</text>
                <text class="info-text ellipsis">{{ visit.location }}</text>
              </view>
              <view class="info-item">
                <text class="info-icon">💼</text>
                <text class="info-text">{{ visit.owner }}</text>
              </view>
            </view>
          </view>

          <view class="card-footer">
            <text class="create-time">创建于 {{ visit.createdAt }}</text>
            <view class="footer-actions">
              <view class="action-btn" @tap.stop="callContact(visit)">
                <text class="action-icon">📞</text>
              </view>
              <view class="action-btn primary" @tap.stop="editVisit(visit)">
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

    <view class="fab-btn" @tap="addVisit">
      <text class="fab-icon">➕</text>
    </view>

    <view v-if="showFilter" class="filter-popup" @tap="showFilter = false">
      <view class="filter-panel" @tap.stop>
        <view class="panel-header">
          <text class="panel-title">筛选条件</text>
          <text class="panel-reset" @tap="resetFilter">重置</text>
        </view>

        <view class="filter-group">
          <text class="group-label">拜访类型</text>
          <view class="tag-group">
            <view
              v-for="type in visitTypes"
              :key="type"
              class="filter-tag"
              :class="{ active: filterForm.type === type }"
              @tap="filterForm.type = filterForm.type === type ? '' : type"
            >
              {{ type }}
            </view>
          </view>
        </view>

        <view class="filter-group">
          <text class="group-label">拜访状态</text>
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
          <text class="group-label">时间范围</text>
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

const tabs = ['全部', '今日', '本周', '本月', '计划中']

const stats = reactive({
  total: 128,
  today: 8,
  planned: 15
})

const visitTypes = ['上门拜访', '电话沟通', '视频会议', '展会见面', '客户来访']
const statusList = ['已完成', '进行中', '计划中', '已取消']
const dateRanges = ['今天', '本周', '本月', '本季度', '今年']

const filterForm = reactive({
  type: '',
  status: '',
  dateRange: ''
})

const visits = ref([
  {
    id: 1,
    customer: '深圳科技创新有限公司',
    subject: '产品需求调研与方案沟通',
    type: '上门拜访',
    typeCode: 'onsite',
    typeIcon: '🏢',
    status: '已完成',
    statusCode: 'completed',
    contactPerson: '李经理 138****8888',
    visitTime: '2026-06-28 14:00',
    location: '深圳市南山区科技园南区A座1201',
    owner: '张三',
    createdAt: '2026-06-25 09:30'
  },
  {
    id: 2,
    customer: '广州智能制造集团',
    subject: '报价方案确认',
    type: '视频会议',
    typeCode: 'video',
    typeIcon: '📹',
    status: '进行中',
    statusCode: 'ongoing',
    contactPerson: '王总 139****6666',
    visitTime: '2026-06-29 10:00',
    location: '腾讯会议',
    owner: '李四',
    createdAt: '2026-06-27 16:00'
  },
  {
    id: 3,
    customer: '东莞电子科技有限公司',
    subject: '新订单跟进',
    type: '电话沟通',
    typeCode: 'phone',
    typeIcon: '📞',
    status: '计划中',
    statusCode: 'planned',
    contactPerson: '陈经理 137****5555',
    visitTime: '2026-06-30 15:00',
    location: '电话沟通',
    owner: '王五',
    createdAt: '2026-06-28 11:20'
  },
  {
    id: 4,
    customer: '佛山机械设备有限公司',
    subject: '售后回访与满意度调查',
    type: '上门拜访',
    typeCode: 'onsite',
    typeIcon: '🏢',
    status: '已完成',
    statusCode: 'completed',
    contactPerson: '刘工 136****4444',
    visitTime: '2026-06-26 09:30',
    location: '佛山市顺德区伦教街道工业园',
    owner: '张三',
    createdAt: '2026-06-24 14:00'
  },
  {
    id: 5,
    customer: '中山照明科技有限公司',
    subject: '初次接洽与公司介绍',
    type: '展会见面',
    typeCode: 'exhibition',
    typeIcon: '🎪',
    status: '已完成',
    statusCode: 'completed',
    contactPerson: '周经理 135****3333',
    visitTime: '2026-06-20 11:00',
    location: '广州国际照明展览会',
    owner: '赵六',
    createdAt: '2026-06-19 08:00'
  },
  {
    id: 6,
    customer: '珠海信息技术有限公司',
    subject: '年度合作框架协议谈判',
    type: '客户来访',
    typeCode: 'visitor',
    typeIcon: '🤝',
    status: '计划中',
    statusCode: 'planned',
    contactPerson: '吴总 134****2222',
    visitTime: '2026-07-02 10:00',
    location: '公司总部会议室A',
    owner: '李四',
    createdAt: '2026-06-28 17:30'
  }
])

const filteredVisits = computed(() => {
  return visits.value
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

function goToDetail(visit: any) {
  uni.showToast({ title: '查看拜访详情', icon: 'none' })
}

function callContact(visit: any) {
  uni.showToast({ title: '拨打电话', icon: 'none' })
}

function editVisit(visit: any) {
  uni.showToast({ title: '编辑拜访记录', icon: 'none' })
}

function addVisit() {
  uni.showToast({ title: '新增拜访记录', icon: 'none' })
}

function resetFilter() {
  filterForm.type = ''
  filterForm.status = ''
  filterForm.dateRange = ''
}

function applyFilter() {
  showFilter.value = false
  uni.showToast({ title: '筛选已应用', icon: 'success' })
}
</script>

<style lang="scss" scoped>
.visits-page {
  min-height: 100vh;
  background: $bg-page;
  display: flex;
  flex-direction: column;
}

.page-header {
  background: linear-gradient(135deg, #e6a23c, #d48806);
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

.tabs-bar {
  display: flex;
  align-items: center;
  background: #fff;
  padding: 0 $spacing-md;
  border-bottom: 1rpx solid $border-color-light;

  .tab-item {
    flex: 1;
    display: flex;
    flex-direction: column;
    align-items: center;
    padding: $spacing-md 0;
    position: relative;

    .tab-text {
      font-size: $font-size-sm;
      color: $text-regular;
      transition: all 0.2s;
    }

    .tab-indicator {
      position: absolute;
      bottom: 0;
      left: 50%;
      transform: translateX(-50%);
      width: 48rpx;
      height: 6rpx;
      background: #e6a23c;
      border-radius: 3rpx;
    }

    &.active {
      .tab-text {
        color: #e6a23c;
        font-weight: $font-weight-semibold;
      }
    }
  }
}

.visit-list {
  flex: 1;
  height: 0;
}

.list-section {
  padding: $spacing-md;

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

.visit-card {
  background: #fff;
  border-radius: $border-radius-lg;
  padding: $spacing-lg;
  margin-bottom: $spacing-md;
  box-shadow: $shadow-sm;

  .card-top {
    display: flex;
    align-items: flex-start;
    margin-bottom: $spacing-md;

    .visit-type {
      width: 72rpx;
      height: 72rpx;
      border-radius: 20rpx;
      display: flex;
      align-items: center;
      justify-content: center;
      margin-right: $spacing-md;
      flex-shrink: 0;

      &.type-onsite { background: linear-gradient(135deg, #667eea, #764ba2); }
      &.type-phone { background: linear-gradient(135deg, #67c23a, #52c41a); }
      &.type-video { background: linear-gradient(135deg, #409eff, #337ecc); }
      &.type-exhibition { background: linear-gradient(135deg, #e6a23c, #d48806); }
      &.type-visitor { background: linear-gradient(135deg, #f56c6c, #e74c3c); }

      .type-icon {
        font-size: 32rpx;
      }
    }

    .visit-main {
      flex: 1;
      min-width: 0;

      .visit-title-row {
        display: flex;
        align-items: center;
        justify-content: space-between;
        margin-bottom: 6rpx;

        .visit-title {
          font-size: $font-size-md;
          font-weight: $font-weight-semibold;
          color: $text-primary;
          overflow: hidden;
          text-overflow: ellipsis;
          white-space: nowrap;
          flex: 1;
        }

        .status-tag {
          flex-shrink: 0;
          padding: 4rpx 16rpx;
          border-radius: 20rpx;
          font-size: 20rpx;
          font-weight: $font-weight-medium;
          margin-left: $spacing-sm;

          &.status-completed {
            background: $success-light;
            color: $success-color;
          }
          &.status-ongoing {
            background: $primary-light;
            color: $primary-color;
          }
          &.status-planned {
            background: $warning-light;
            color: $warning-color;
          }
          &.status-cancelled {
            background: $info-light;
            color: $info-color;
          }
        }
      }

      .visit-subject {
        font-size: $font-size-sm;
        color: $text-regular;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      }
    }
  }

  .card-body {
    padding: $spacing-md 0;
    border-top: 1rpx solid $border-color-light;
    border-bottom: 1rpx solid $border-color-light;

    .info-grid {
      display: flex;
      flex-wrap: wrap;
      gap: $spacing-sm 0;

      .info-item {
        width: 50%;
        display: flex;
        align-items: center;
        font-size: $font-size-xs;
        color: $text-secondary;

        .info-icon {
          font-size: 24rpx;
          margin-right: 8rpx;
        }

        .info-text {
          flex: 1;
          font-size: $font-size-xs;
          color: $text-regular;
          min-width: 0;
          overflow: hidden;
          text-overflow: ellipsis;
          white-space: nowrap;
        }

        .ellipsis {
          overflow: hidden;
          text-overflow: ellipsis;
          white-space: nowrap;
        }
      }
    }
  }

  .card-footer {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding-top: $spacing-md;

    .create-time {
      font-size: $font-size-xs;
      color: $text-secondary;
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
  bottom: calc(#{$spacing-lg} + env(safe-area-inset-bottom));
  width: 112rpx;
  height: 112rpx;
  background: linear-gradient(135deg, #e6a23c, #d48806);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 8rpx 24rpx rgba(230, 162, 60, 0.4);
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
