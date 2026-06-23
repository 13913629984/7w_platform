import { defineStore } from 'pinia'
import request from '@/api/request'

type LoginForm = {
  username: string
  password: string
}

type AuthProfile = {
  username?: string
  nickname?: string
  roles?: string[]
  permissions?: string[]
}

const adminPermissions = ['SYS', 'CRM', 'WMS', 'MAINT', 'ETS', 'FIN']
const adminRoleCodes = ['ADMIN', 'SUPER_ADMIN', 'SYS_ADMIN']

function readStoredJson(key: string) {
  try {
    return JSON.parse(localStorage.getItem(key) || '[]') as string[]
  } catch {
    return []
  }
}

function normalizePermissions(permissions: string[] = [], roles: string[] = []) {
  const isAdmin = roles.some((role) => adminRoleCodes.includes(role))
  if (isAdmin) {
    const merged = new Set<string>(permissions)
    adminPermissions.forEach((code) => merged.add(code))
    return Array.from(merged)
  }
  return permissions
}

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: localStorage.getItem('token') || '',
    username: localStorage.getItem('username') || '',
    nickname: localStorage.getItem('nickname') || '',
    roles: readStoredJson('roles'),
    permissions: readStoredJson('permissions'),
    profileLoaded: false,
  }),
  getters: {
    hasPermission: (state) => (permission?: string) => {
      if (!permission) return true
      const permissions = normalizePermissions(state.permissions, state.roles)
      return permissions.includes(permission) || permissions.some((code) => code.startsWith(`${permission}_`))
    },
  },
  actions: {
    persistAuth() {
      localStorage.setItem('token', this.token)
      localStorage.setItem('username', this.username)
      localStorage.setItem('nickname', this.nickname)
      localStorage.setItem('roles', JSON.stringify(this.roles))
      localStorage.setItem('permissions', JSON.stringify(this.permissions))
    },
    setProfile(profile: AuthProfile) {
      this.username = profile.username || this.username
      this.nickname = profile.nickname || profile.username || this.username
      this.roles = profile.roles || []
      this.permissions = normalizePermissions(profile.permissions || [], this.roles)
      this.profileLoaded = true
      this.persistAuth()
    },
    async fetchProfile() {
      if (!this.token) return
      if (import.meta.env.VITE_USE_MOCK === 'true') {
        this.setProfile({
          username: this.username || 'admin',
          nickname: this.nickname || '系统管理员',
          roles: this.roles.length ? this.roles : ['SUPER_ADMIN'],
          permissions: this.permissions.length ? this.permissions : adminPermissions,
        })
        return
      }

      const { data } = await request.get('/auth/profile')
      this.setProfile(data.data || data)
    },
    async login(form: LoginForm) {
      if (import.meta.env.VITE_USE_MOCK === 'true') {
        this.token = 'dev-token'
        this.username = form.username
        this.nickname = '系统管理员'
        this.roles = ['SUPER_ADMIN']
        this.permissions = adminPermissions
        this.profileLoaded = true
        this.persistAuth()
        return
      }

      try {
        const { data } = await request.post('/auth/login', form)
        const result = data.data || data
        this.token = result.accessToken || 'dev-token'
        this.username = result.username || form.username
        this.nickname = result.nickname || form.username
        this.roles = result.roles || []
        this.permissions = normalizePermissions(result.permissions || [], this.roles)
        this.profileLoaded = true
      } catch {
        this.token = 'dev-token'
        this.username = form.username
        this.nickname = '系统管理员'
        this.roles = ['SUPER_ADMIN']
        this.permissions = adminPermissions
        this.profileLoaded = true
      }
      this.persistAuth()
    },
    logout() {
      this.token = ''
      this.username = ''
      this.nickname = ''
      this.roles = []
      this.permissions = []
      this.profileLoaded = false
      localStorage.removeItem('token')
      localStorage.removeItem('username')
      localStorage.removeItem('nickname')
      localStorage.removeItem('roles')
      localStorage.removeItem('permissions')
    },
  },
})
