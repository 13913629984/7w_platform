<template>
  <div class="app-layout" :class="{ 'sidebar-collapsed': collapsed }">
    <aside class="sidebar">
      <div class="logo-area">
        <div class="logo-icon">信</div>
        <div class="logo-text">信息化平台</div>
      </div>

      <nav class="menu-section">
        <template v-if="route.path === '/dashboard'">
          <div class="menu-group-header"><span>主导航</span></div>
          <router-link to="/dashboard" class="menu-item top-menu active">
            <span class="menu-icon">🏠</span>
            <span>工作台</span>
          </router-link>
          <div class="menu-group-header"><span>子系统</span></div>
          <router-link v-for="system in systemMenus" :key="system.path" :to="firstChildPath(system)" class="menu-item">
            <span class="menu-icon">{{ system.icon }}</span>
            <span>{{ system.title }}</span>
          </router-link>
        </template>

        <template v-else>
          <router-link to="/dashboard" class="menu-item top-menu" :class="{ active: route.path === '/dashboard' }">
            <span class="menu-icon">🏠</span>
            <span>工作台</span>
          </router-link>

          <div v-if="currentMenu" class="menu-group current-system-menu">
            <div class="menu-group-header">
              <span>{{ currentMenu.title }}</span>
              <span>⌃</span>
            </div>
            <div class="menu-group-body">
              <router-link v-for="child in currentMenu.children" :key="child.path" :to="child.path" class="menu-item" :class="{ active: route.path === child.path }">
                <span class="menu-icon">{{ child.icon }}</span>
                <span>{{ child.title }}</span>
              </router-link>
            </div>
          </div>
        </template>
      </nav>

      <div v-if="route.path !== '/dashboard'" class="quick-switch">
        <div class="quick-switch-label">快速切换</div>
        <router-link v-for="system in systemMenus" :key="system.path" :to="firstChildPath(system)" class="quick-switch-item" :class="{ active: route.path.startsWith(system.path) }">
          <span class="menu-icon">{{ system.icon }}</span>
          <span>{{ system.title }}</span>
        </router-link>
        <router-link to="/dashboard" class="quick-switch-item">
          <span class="menu-icon">↩</span>
          <span>返回工作台</span>
        </router-link>
      </div>
    </aside>

    <main class="main-content">
      <header class="header">
        <div class="header-left">
          <button class="collapse-btn" type="button" aria-label="折叠菜单" @click="toggleSidebar">☰</button>
          <div class="breadcrumb"><span>{{ route.path === '/dashboard' ? '工作台' : currentSystem }}</span><template v-if="route.path !== '/dashboard'"> / <span>{{ route.meta.title }}</span></template></div>
        </div>
        <div class="header-right">
          <span class="header-date">{{ todayLabel }}</span>
          <div class="notification">🔔</div>
          <el-button link class="logout-btn" @click="logout">退出登录</el-button>
          <div class="user-info">
            <div class="avatar">{{ avatarChar }}</div>
            <span class="user-name">{{ auth.nickname }}</span>
          </div>
        </div>
      </header>
      <div class="page-content" :class="{ 'dashboard-body': route.path === '/dashboard' }">
        <router-view />
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { menus, type AppMenu } from '@/router/menus'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

/** PC 端负责：侧边栏仅支持折叠为窄轨道 */
const collapsed = ref(localStorage.getItem('sidebar-collapsed') === '1')

const systemMenus = computed(() => menus.filter((menu) => menu.children?.length && auth.hasPermission(menu.permission)))
const currentMenu = computed(() => systemMenus.value.find((menu) => route.path.startsWith(menu.path)))
const currentSystem = computed(() => currentMenu.value?.title || '统一门户')
const avatarChar = computed(() => (auth.nickname || auth.username || '用').charAt(0))

const todayLabel = computed(() => {
  const now = new Date()
  const weekdays = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
  return `${now.getFullYear()}年${now.getMonth() + 1}月${now.getDate()}日 ${weekdays[now.getDay()]}`
})

function firstChildPath(menu: AppMenu) {
  return menu.children?.[0]?.path || menu.path
}

function toggleSidebar() {
  collapsed.value = !collapsed.value
  localStorage.setItem('sidebar-collapsed', collapsed.value ? '1' : '0')
}

function logout() {
  auth.logout()
  router.push('/login')
}
</script>
