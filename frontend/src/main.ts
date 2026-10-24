import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
// 样式加载顺序有依赖：EP 原生变量 → 设计令牌 → EP 主题映射 → 平台样式。
// element-theme.css 依赖 tokens.css 里定义的 --pw-* 变量，顺序不能调换。
import 'element-plus/dist/index.css'
import './assets/tokens.css'
import './assets/element-theme.css'
import './assets/styles.css'
import App from './App.vue'
import router from './router'
// 通用布局组件：统计卡 / 卡片栅格 / 页面标题 / 列表卡片
// 全局注册后视图可直接使用，无需逐个 import
import AppStatCard from './components/AppStatCard.vue'
import AppStatGrid from './components/AppStatGrid.vue'
import AppPageHeader from './components/AppPageHeader.vue'
import AppTableCard from './components/AppTableCard.vue'

const app = createApp(App)

for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

app.component('AppStatCard', AppStatCard)
app.component('AppStatGrid', AppStatGrid)
app.component('AppPageHeader', AppPageHeader)
app.component('AppTableCard', AppTableCard)

app.use(createPinia())
app.use(router)
// 此前未传 locale，分页器、日期选择器、空状态等仍是英文文案
app.use(ElementPlus, { locale: zhCn })
app.mount('#app')
