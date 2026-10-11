<template>
  <div class="fab" :class="{ open, hidden: ui.menuOpen }">
    <!-- 展开的子按钮：购物车 / 收藏 / 回顶（springy 弹出） -->
    <transition-group tag="div" name="fabsub" class="fab-subs">
      <button v-if="open" key="cart" class="fab-sub" aria-label="查看购物车" @click="show('cart')">
        <el-icon><ShoppingCart /></el-icon>
        <span v-if="cart.count" class="fab-badge">{{ cart.count }}</span>
      </button>
      <button v-if="open" key="fav" class="fab-sub" aria-label="查看收藏" @click="show('favorite')">
        <el-icon><Star /></el-icon>
        <span v-if="fav.count" class="fab-badge fav-badge">{{ fav.count }}</span>
      </button>
      <button v-if="open" key="top" class="fab-sub" aria-label="回到顶部" @click="onTop">
        <el-icon><Top /></el-icon>
      </button>
    </transition-group>

    <!-- 主按钮：+ 展开 / 收起 -->
    <button class="fab-main" :aria-label="open ? '收起工具' : '展开工具'" @click="open = !open">
      <el-icon class="fab-plus"><Plus /></el-icon>
    </button>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { useCartStore } from '../stores/cart'
import { useFavoriteStore } from '../stores/favorite'
import { useUiStore } from '../stores/ui'

// 右下角悬浮工具：点击「+」弹簧式展开 购物车 / 收藏 / 回顶
const open = ref(false)
const cart = useCartStore()
const fav = useFavoriteStore()
const ui = useUiStore()

function show(type) {
  open.value = false
  ui.openPanel(type)
}

function onTop() {
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

// 点击工具区域外自动收起
function onClickOutside(e) {
  if (open.value && !e.target.closest('.fab')) open.value = false
}
onMounted(() => document.addEventListener('click', onClickOutside))
onBeforeUnmount(() => document.removeEventListener('click', onClickOutside))
</script>

<style scoped>
.fab {
  position: fixed; right: 28px; bottom: calc(28px + var(--safe-bottom)); z-index: 80;
  display: flex; flex-direction: column; align-items: center;
  transition: opacity .3s ease, transform .35s var(--smooth);
}
/* 抽屉菜单打开时隐藏悬浮球，避免重叠 */
.fab.hidden { opacity: 0; pointer-events: none; transform: translateY(12px); }
.fab-subs { display: flex; flex-direction: column; align-items: center; gap: 14px; margin-bottom: 14px; }
.fab-sub {
  position: relative;
  width: 48px; height: 48px; border-radius: 50%;
  display: grid; place-items: center;
  background: #fff; color: var(--ink-2);
  box-shadow: var(--shadow-md);
  transition: color .3s ease, box-shadow .3s ease;
}
.fab-sub .el-icon { font-size: 22px; }
.fab-sub:hover { color: var(--brand); box-shadow: var(--shadow-lg); }
.fab-badge {
  position: absolute; top: -4px; right: -4px;
  min-width: 20px; height: 20px; padding: 0 5px;
  border-radius: var(--radius-pill);
  background: var(--price); color: #fff;
  font-size: 11px; font-weight: 700; line-height: 20px;
}
.fav-badge { background: var(--like); }

/* 子按钮 springy 弹出 / 收起 */
.fabsub-enter-active, .fabsub-leave-active { transition: transform .5s var(--elastic), opacity .35s ease; }
.fabsub-enter-from, .fabsub-leave-to { transform: scale(0) translateY(16px); opacity: 0; }
.fabsub-enter-active:nth-child(1) { transition-delay: .08s; }
.fabsub-enter-active:nth-child(2) { transition-delay: .16s; }
.fabsub-enter-active:nth-child(3) { transition-delay: .24s; }

.fab-main {
  width: 56px; height: 56px; border-radius: 50%;
  display: grid; place-items: center;
  color: #fff;
  background: linear-gradient(135deg, var(--brand-deep), var(--brand));
  box-shadow: 0 12px 28px rgba(47, 107, 255, .42);
  transition: transform .45s var(--elastic);
}
.fab-main:hover { transform: scale(1.08); }
.fab-main .el-icon { font-size: 26px; transition: transform .5s var(--elastic); }
.fab.open .fab-main .el-icon { transform: rotate(135deg); }

@media (max-width: 768px) {
  .fab { right: 18px; bottom: calc(18px + var(--safe-bottom)); }
}

@media (max-width: 480px) {
  .fab { right: 14px; bottom: calc(14px + var(--safe-bottom)); }
  .fab-main { width: 52px; height: 52px; }
  .fab-sub { width: 44px; height: 44px; }
  .fab-subs { gap: 12px; margin-bottom: 12px; }
}

@media (max-width: 360px) {
  .fab { right: 12px; bottom: calc(12px + var(--safe-bottom)); }
  .fab-main { width: 48px; height: 48px; }
  .fab-sub { width: 42px; height: 42px; }
  .fab-subs { gap: 10px; margin-bottom: 10px; }
}
</style>
