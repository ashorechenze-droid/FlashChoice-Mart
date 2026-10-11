import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import 'element-plus/dist/index.css'

import App from './App.vue'
import router from './router'
import './styles/main.css'

// 嵌入 WebView 兼容：部分环境（如豆包内置浏览器）不驱动 requestAnimationFrame，
// 导致 Vue 路由过渡（Transition）与组件动画永远卡在下一帧、页面切换不生效。
// 这里统一将 rAF 降级为 16ms 定时器，保证过渡与动效回调必然触发。
if (typeof window !== 'undefined' && typeof window.requestAnimationFrame === 'function') {
  const rafFallback = (cb) => setTimeout(() => cb(performance.now()), 16)
  window.requestAnimationFrame = rafFallback
  window.cancelAnimationFrame = (id) => clearTimeout(id)
}

const app = createApp(App)

app.use(createPinia())
app.use(router)
app.use(ElementPlus)

// 全局注册 Element Plus 图标（模板中可直接 <el-icon><Search /></el-icon>）
for (const [name, comp] of Object.entries(ElementPlusIconsVue)) {
  app.component(name, comp)
}

app.mount('#app')
