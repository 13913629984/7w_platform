<template>
  <div class="app-layout" :class="{ 'dashboard-home': route.path === '/dashboard' }">
    <aside class="sidebar">
      <div class="logo-area">
        <div class="logo-icon">七</div>
        <div class="logo-text">七维空间平台</div>
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
          <div class="collapse-btn">☰</div>
          <div class="breadcrumb"><span>{{ route.path === '/dashboard' ? '工作台' : currentSystem }}</span><template v-if="route.path !== '/dashboard'"> / <span>{{ route.meta.title }}</span></template></div>
        </div>
        <div class="header-right">
          <span style="font-size:13px;color:#909399">2026年6月16日 周二</span>
          <div class="notification">🔔</div>
          <el-button link @click="logout" style="color:#909399;margin:0 10px;">退出登录</el-button>
          <div class="user-info" style="display:flex;align-items:center;gap:8px;">
            <div class="avatar">{{ auth.nickname.charAt(0) }}</div>
            <span style="font-size:14px;color:#606266;">{{ auth.nickname }}</span>
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
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { menus, type AppMenu } from '@/router/menus'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const systemMenus = computed(() => menus.filter((menu) => menu.children?.length && auth.hasPermission(menu.permission)))
const currentMenu = computed(() => systemMenus.value.find((menu) => route.path.startsWith(menu.path)))
const currentSystem = computed(() => currentMenu.value?.title || '统一门户')

function firstChildPath(menu: AppMenu) {
  return menu.children?.[0]?.path || menu.path
}

function logout() {
  auth.logout()
  router.push('/login')
}
</script>
