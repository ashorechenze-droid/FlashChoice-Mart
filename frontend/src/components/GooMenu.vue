<template>
  <!-- 常规侧边抽屉菜单：半透明遮罩 + 右侧面板滑入（替代原全屏液态菜单） -->
  <div class="drawer-mask" :class="{ open: ui.menuOpen }" @click="ui.menuOpen = false"></div>
  <aside class="side-menu" :class="{ open: ui.menuOpen }" aria-label="全站导航">
    <div class="side-head">
      <span class="side-logo" aria-hidden="true">
        <svg viewBox="0 0 24 24" width="18" height="18" fill="currentColor">
          <path d="M13 2 4.5 13.5H11L9.5 22 19 9.5h-6.5L13 2z" />
        </svg>
      </span>
      <strong class="side-title">极速优选</strong>
      <button class="side-close" aria-label="关闭菜单" @click="ui.menuOpen = false">
        <el-icon><Close /></el-icon>
      </button>
    </div>

    <nav class="side-nav">
      <a v-for="(item, i) in items" :key="item.name" class="side-item"
         :style="{ '--i': i }" role="button" tabindex="0"
         @click="goto(item)" @keydown.enter="goto(item)">
        <span class="side-icon" aria-hidden="true">
          <el-icon><component :is="item.icon" /></el-icon>
        </span>
        <span class="side-label">{{ item.name }}</span>
        <el-icon class="side-arrow"><ArrowRight /></el-icon>
      </a>
    </nav>

    <div class="side-foot">
      <p>质感生活 · 即刻拥有</p>
    </div>
  </aside>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { useUiStore } from '../stores/ui'
import {
  House, Goods, Timer, Shop, TrendCharts, User, Setting, ArrowRight,
} from '@element-plus/icons-vue'

// 常规侧边抽屉菜单：遮罩 + 右侧面板滑入，菜单项波浪式错位入场
const ui = useUiStore()
const router = useRouter()

const items = [
  { name: '首页', path: '/', icon: House },
  { name: '全部商品', path: '/goods', icon: Goods },
  { name: '秒杀会场', path: '/seckill', icon: Timer },
  { name: '品牌馆', path: '/brands', icon: Shop },
  { name: '热销榜', path: '/goods?sort=hot', icon: TrendCharts },
  { name: '个人中心', path: '/member', icon: User },
  { name: '管理端', path: '/admin', icon: Setting },
]

function goto(item) {
  ui.menuOpen = false
  router.push(item.path)
}
</script>

<style scoped>
.drawer-mask {
  position: fixed; inset: 0; z-index: 89;
  background: rgba(15, 23, 42, .45);
  -webkit-backdrop-filter: blur(4px);
  backdrop-filter: blur(4px);
  opacity: 0; visibility: hidden; pointer-events: none;
  transition: opacity .35s ease, visibility .35s;
}
.drawer-mask.open { opacity: 1; visibility: visible; pointer-events: auto; }

.side-menu {
  position: fixed; top: 0; right: 0; bottom: 0; z-index: 90;
  width: min(340px, 84vw);
  display: flex; flex-direction: column;
  background: var(--surface);
  box-shadow: -20px 0 60px rgba(15, 23, 42, .18);
  transform: translateX(102%);
  transition: transform .5s var(--smooth);
}
.side-menu.open { transform: translateX(0); }

.side-head {
  display: flex; align-items: center; gap: 10px;
  padding: calc(18px + var(--safe-top)) 20px 16px;
  border-bottom: 1px solid var(--line);
}
.side-logo {
  width: 36px; height: 36px; border-radius: 10px;
  display: grid; place-items: center;
  background: linear-gradient(135deg, var(--brand-deep), var(--brand-2));
  color: #fff;
  box-shadow: 0 6px 16px rgba(47, 107, 255, .32);
}
.side-title { font-size: 19px; font-weight: 800; letter-spacing: 1px; }
.side-close {
  margin-left: auto;
  width: 38px; height: 38px; border-radius: 50%;
  display: grid; place-items: center;
  color: var(--ink-2);
  background: var(--bg);
  transition: transform .4s var(--elastic), color .3s ease, background .3s ease;
}
.side-close:hover { transform: rotate(90deg); color: var(--brand); background: var(--brand-soft); }
.side-close .el-icon { font-size: 18px; }

.side-nav { flex: 1; overflow-y: auto; padding: 14px; }
.side-item {
  display: flex; align-items: center; gap: 14px;
  padding: 12px 14px;
  border-radius: 12px;
  font-size: 16px; font-weight: 600; color: var(--ink);
  cursor: pointer;
  opacity: 0; transform: translateX(16px);
  transition: opacity .4s ease, transform .45s var(--smooth),
              background .3s ease, color .3s ease;
}
.side-menu.open .side-item { opacity: 1; transform: none; transition-delay: calc(.08s + var(--i) * .045s); }
.side-item:hover { background: var(--brand-soft); color: var(--brand-deep); }
.side-icon {
  width: 36px; height: 36px; border-radius: 10px;
  display: grid; place-items: center;
  background: var(--bg);
  color: var(--brand);
  flex-shrink: 0;
  transition: background .3s ease, color .3s ease;
}
.side-icon .el-icon { font-size: 18px; }
.side-item:hover .side-icon { background: #fff; color: var(--brand-deep); }
.side-label { flex: 1; min-width: 0; }
.side-arrow { color: var(--ink-2); opacity: .55; flex-shrink: 0; font-size: 14px; transition: transform .3s var(--smooth), color .3s ease, opacity .3s ease; }
.side-item:hover .side-arrow { transform: translateX(3px); color: var(--brand); opacity: 1; }

.side-foot {
  padding: 16px 20px calc(18px + var(--safe-bottom));
  border-top: 1px solid var(--line);
  font-size: 12px; letter-spacing: 2px; color: var(--ink-2);
  text-align: center;
}

@media (max-width: 768px) {
  .side-menu { width: min(320px, 86vw); }
}

@media (max-width: 480px) {
  .side-item { padding: 11px 12px; font-size: 15px; }
  .side-icon { width: 32px; height: 32px; }
}

@media (max-width: 360px) {
  .side-item { padding: 10px 10px; font-size: 14.5px; }
  .side-icon { width: 30px; height: 30px; border-radius: 8px; }
  .side-head { padding-top: calc(14px + var(--safe-top)); }
}
</style>
