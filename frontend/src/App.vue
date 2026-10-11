<template>
  <!-- 应用外壳：固定导航 + 页面路由 + 页脚 + 全屏液态菜单 + 悬浮工具 + 商品弹窗 -->
  <!-- 管理端页面（/admin）独立布局，不显示商城导航/页脚/悬浮工具 -->
  <template v-if="!isAdminPage">
    <NavBar />
    <main class="page">
      <router-view v-slot="{ Component }">
        <transition name="fade" mode="out-in">
          <component :is="Component" />
        </transition>
      </router-view>
    </main>
    <AppFooter />
    <GooMenu />
    <FloatingTool />
    <GoodsPanel />
  </template>

  <!-- 管理端：全屏独立布局 -->
  <router-view v-else />
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import NavBar from './components/NavBar.vue'
import AppFooter from './components/AppFooter.vue'
import GooMenu from './components/GooMenu.vue'
import FloatingTool from './components/FloatingTool.vue'
import GoodsPanel from './components/GoodsPanel.vue'

const route = useRoute()
const isAdminPage = computed(() => route.path.startsWith('/admin'))
</script>

<style scoped>
.page {
  flex: 1;
  padding-top: var(--nav-h); /* 固定导航预留 */
  display: flex;
  flex-direction: column;
}
/* 手机端：子导航栏也占空间，额外预留 */
@media (max-width: 1100px) {
  .page { padding-top: calc(var(--nav-h) + 54px); }
}
</style>
