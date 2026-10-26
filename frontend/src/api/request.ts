import axios from 'axios'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'

const request = axios.create({
  baseURL: '/api',
  timeout: 15000,
})

/** 401 单飞守卫：首屏并发请求全部 401 时，只处理第一个 */
let redirecting = false

request.interceptors.request.use((config) => {
  const auth = useAuthStore()
  if (auth.token) {
    config.headers.Authorization = `Bearer ${auth.token}`
  }
  return config
})

request.interceptors.response.use(
  (response) => response,
  (error) => {
    const status = error?.response?.status
    if (status === 401) {
      // 网关 JWT 校验失败：清理本地凭证并回到登录页，避免页面停留在无数据状态。
      // 用单飞守卫，避免首屏并发请求同时 401 时弹出一串提示、重复跳转。
      const auth = useAuthStore()
      auth.logout()
      if (!redirecting) {
        redirecting = true
        const reason = error?.response?.data?.message || '登录已过期，请重新登录'
        // window.location.replace 是硬跳转，ElMessage 会立刻消失，
        // 所以把原因塞进 sessionStorage，由登录页挂载后提示一次。
        sessionStorage.setItem('auth_redirect_reason', reason)
        if (window.location.pathname !== '/login') {
          const redirect = encodeURIComponent(window.location.pathname + window.location.search)
          window.location.replace(`/login?redirect=${redirect}`)
        }
      }
      return Promise.reject(error)
    }

    // 框架层错误（缺参/参数类型不对/JSON 解析失败）返回的是 Spring 默认体，没有 message 字段，
    // 这里把可读信息挂到 error.message 上，让各组件的 `e.message` 分支能显示真实原因。
    const backendMessage = error?.response?.data?.message
    if (backendMessage) {
      error.message = backendMessage
    } else if (status === 400) {
      error.message = error?.response?.data?.error || '请求参数不合法'
    } else if (status === 403) {
      error.message = '没有访问权限'
    } else if (status >= 500) {
      error.message = '服务暂时不可用，请稍后重试'
    }
    return Promise.reject(error)
  },
)

export default request
