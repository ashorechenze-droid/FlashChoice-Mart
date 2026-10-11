<template>
  <header class="nav" :class="{ scrolled }">
    <div class="container nav-inner">
      <!-- Logo -->
      <router-link to="/" class="logo">
        <span class="logo-mark">
          <svg viewBox="0 0 24 24" width="22" height="22" fill="currentColor" aria-hidden="true">
            <path d="M13 2 4.5 13.5H11L9.5 22 19 9.5h-6.5L13 2z" />
          </svg>
        </span>
        <span class="logo-text">极速优选</span>
        <span class="logo-tag">B2C 秒杀商城</span>
      </router-link>

      <!-- 居中导航胶囊（圆角矩形） -->
      <nav class="nav-links">
        <router-link v-for="l in navLinks" :key="l.path" :to="l.path" class="nav-link"
                     :class="{ active: route.path === l.path }">
          {{ l.name }}
        </router-link>
      </nav>

      <!-- 右侧：搜索 + 用户 + 菜单 -->
      <div class="nav-actions">
        <!-- 电脑端搜索框 -->
        <el-input v-model="keyword" class="nav-search nav-search-desktop" placeholder="搜索商品" clearable
                  @keyup.enter="onSearch" @clear="onSearch">
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>

        <!-- 手机端搜索图标按钮 -->
        <button class="icon-btn nav-search-mobile" aria-label="搜索商品" @click="toggleSearch">
          <el-icon><Search /></el-icon>
        </button>

        <!-- 已登录：头像下拉 -->
        <el-dropdown v-if="user.isLoggedIn" trigger="click" @command="onCommand">
          <button class="user-chip" aria-label="用户菜单">
            <span class="user-avatar">{{ (user.info?.name || user.info?.account || 'M').slice(0, 1) }}</span>
            <span class="user-name">{{ user.info?.name || user.info?.account }}</span>
          </button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="profile">个人中心</el-dropdown-item>
              <el-dropdown-item command="admin">管理端</el-dropdown-item>
              <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>

        <!-- 游客：登录入口 -->
        <button v-else class="login-btn" @click="router.push('/login')">登录</button>

        <button class="icon-btn menu-btn" :class="{ active: ui.menuOpen }" aria-label="打开菜单"
                @click="ui.menuOpen = !ui.menuOpen">
          <el-icon><Menu /></el-icon>
        </button>
      </div>
    </div>
  </header>

  <!-- 手机端搜索展开层：点击搜索图标后展开输入框 -->
  <div class="mobile-search-bar" :class="{ open: searchOpen }">
    <div class="container mobile-search-inner">
      <el-input v-model="mobileKeyword" class="mobile-search-input" placeholder="搜索商品" clearable
                @keyup.enter="onMobileSearch" @clear="onMobileSearch">
        <template #prefix><el-icon><Search /></el-icon></template>
      </el-input>
      <button class="mobile-search-btn" @click="onMobileSearch">搜索</button>
      <button class="mobile-search-cancel" @click="closeSearch">取消</button>
    </div>
  </div>

  <!-- 手机端：导航链接下移为横向滚动子导航栏（搜索展开时隐藏，避免遮挡） -->
  <nav class="mobile-subnav no-scrollbar" :class="{ hidden: searchOpen }" aria-label="页面导航">
    <router-link v-for="l in mobileNavLinks" :key="l.path" :to="l.path" class="mobile-subnav-link"
                 :class="{ active: route.path === l.path }">
      {{ l.name }}
    </router-link>
  </nav>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '../stores/user'
import { useUiStore } from '../stores/ui'

const route = useRoute()
const router = useRouter()
const user = useUserStore()
const ui = useUiStore()

const navLinks = [
  { name: '首页', path: '/' },
  { name: '全部商品', path: '/goods' },
  { name: '秒杀会场', path: '/seckill' },
  { name: '品牌馆', path: '/brands' },
  { name: '个人中心', path: '/member' },
]

// 手机端子导航：保留全部入口（横向滚动可容纳），个人中心直接可见无需点头像
const mobileNavLinks = computed(() => navLinks)

const keyword = ref('')
const scrolled = ref(false)

// 手机端搜索展开层
const searchOpen = ref(false)
const mobileKeyword = ref('')

function toggleSearch() {
  searchOpen.value = !searchOpen.value
  if (searchOpen.value) mobileKeyword.value = keyword.value
}
function closeSearch() {
  searchOpen.value = false
}
function onMobileSearch() {
  const kw = mobileKeyword.value.trim()
  keyword.value = kw
  searchOpen.value = false
  router.push({ path: '/goods', query: kw ? { keyword: kw } : {} })
}

function onSearch() {
  router.push({ path: '/goods', query: keyword.value ? { keyword: keyword.value } : {} })
}

function onCommand(cmd) {
  if (cmd === 'logout') user.logout()
  else if (cmd === 'profile') router.push('/member')
  else if (cmd === 'admin') router.push('/admin')
}

// 滚动后加深导航投影
const onScroll = () => { scrolled.value = window.scrollY > 12 }
onMounted(() => window.addEventListener('scroll', onScroll, { passive: true }))
onBeforeUnmount(() => window.removeEventListener('scroll', onScroll))
</script>

<style scoped>
.nav {
  position: fixed;
  top: 0; left: 0; right: 0;
  height: var(--nav-h);
  z-index: 50;
  display: flex;
  align-items: center;
  background: var(--glass-bg);
  -webkit-backdrop-filter: blur(18px) saturate(1.8);
  backdrop-filter: blur(18px) saturate(1.8);
  border-bottom: 1px solid var(--glass-border);
  transition: box-shadow .4s ease, background .4s ease;
}
.nav.scrolled { box-shadow: var(--shadow-sm); background: rgba(255, 255, 255, .82); }

.nav-inner { display: flex; align-items: center; gap: 24px; min-width: 0; }

.logo { display: flex; align-items: center; gap: 10px; flex-shrink: 0; min-width: 0; }
.logo-mark {
  width: 42px; height: 42px; border-radius: 12px;
  display: grid; place-items: center;
  background: linear-gradient(135deg, var(--brand-deep), var(--brand-2));
  color: #fff;
  box-shadow: 0 6px 16px rgba(47, 107, 255, .35);
  transition: transform .45s var(--elastic);
  flex-shrink: 0;
}
.logo:hover .logo-mark { transform: rotate(-8deg) scale(1.06); }
.logo-text { font-size: 30px; font-weight: 900; letter-spacing: 1px; white-space: nowrap; flex-shrink: 0; }
.logo-tag {
  font-size: 12px; font-weight: 600; color: var(--brand-deep);
  background: var(--brand-soft); border-radius: var(--radius-pill);
  padding: 3px 10px; white-space: nowrap; flex-shrink: 0;
}

.nav-links { display: flex; align-items: center; gap: 8px; margin-inline: auto; min-width: 0; flex-shrink: 1; }
.nav-link {
  padding: 10px 22px;
  border-radius: 12px;
  font-size: 15px; font-weight: 600; color: var(--ink-2);
  white-space: nowrap;
  transition: color .3s ease, background .3s ease, transform .3s var(--elastic);
}
.nav-link:hover { color: var(--brand); background: var(--brand-soft); }
.nav-link.active {
  color: #fff;
  background: linear-gradient(135deg, var(--brand-deep), var(--brand));
  box-shadow: 0 6px 16px rgba(47, 107, 255, .32);
}

.nav-actions { display: flex; align-items: center; gap: 10px; flex-shrink: 0; min-width: 0; }
.nav-search { width: 200px; --el-input-border-radius: 999px; }
.nav-search :deep(.el-input__wrapper) { background: rgba(255, 255, 255, .75); }
.nav-search-mobile { display: none; }

.user-chip {
  display: flex; align-items: center; gap: 8px;
  padding: 5px 12px 5px 5px;
  border-radius: var(--radius-pill);
  background: rgba(255, 255, 255, .8);
  border: 1px solid var(--line);
  transition: border-color .3s ease, box-shadow .3s ease;
}
.user-chip:hover { border-color: rgba(47, 107, 255, .4); box-shadow: var(--shadow-sm); }
.user-avatar {
  width: 32px; height: 32px; border-radius: 50%;
  display: grid; place-items: center;
  background: linear-gradient(135deg, var(--brand-deep), var(--brand-2));
  color: #fff; font-size: 14px; font-weight: 700;
}
.user-name { font-size: 14px; font-weight: 600; }

.login-btn {
  padding: 9px 22px;
  border-radius: var(--radius-pill);
  background: linear-gradient(135deg, var(--brand-deep), var(--brand));
  color: #fff; font-size: 14px; font-weight: 600;
  box-shadow: 0 6px 16px rgba(47, 107, 255, .3);
  transition: transform .35s var(--elastic), box-shadow .3s ease;
}
.login-btn:hover { transform: translateY(-1px) scale(1.03); }

.icon-btn {
  width: 44px; height: 44px; border-radius: 14px;
  display: grid; place-items: center;
  color: var(--ink-2);
  transition: color .3s ease, background .3s ease;
}
.icon-btn:hover { color: var(--brand); background: var(--brand-soft); }
.icon-btn .el-icon { font-size: 22px; }
.icon-btn.menu-btn .el-icon { transition: transform .5s var(--elastic); }
.icon-btn.menu-btn.active { color: #fff; background: var(--brand); }
.icon-btn.menu-btn.active .el-icon { transform: rotate(90deg); }

/* 手机端搜索展开层：默认隐藏，open 时从导航栏下方滑入 */
.mobile-search-bar {
  display: none;
  position: fixed;
  top: var(--nav-h);
  left: 0; right: 0;
  z-index: 50;
  background: rgba(255, 255, 255, .96);
  -webkit-backdrop-filter: blur(18px) saturate(1.8);
  backdrop-filter: blur(18px) saturate(1.8);
  border-bottom: 1px solid var(--glass-border);
  padding: 10px 16px;
  transform: translateY(-100%);
  opacity: 0;
  visibility: hidden;
  transition: transform .35s var(--smooth), opacity .3s ease, visibility .3s;
}
.mobile-search-bar.open {
  transform: translateY(0);
  opacity: 1;
  visibility: visible;
}
.mobile-search-inner { display: flex; align-items: center; gap: 8px; }
.mobile-search-input { flex: 1; --el-input-border-radius: 999px; }
.mobile-search-input :deep(.el-input__wrapper) { background: rgba(245, 247, 250, .9); }
.mobile-search-btn {
  flex-shrink: 0;
  padding: 8px 18px;
  font-size: 14px; font-weight: 600; color: #fff;
  background: linear-gradient(135deg, var(--brand), var(--brand-2));
  border: none; border-radius: 999px; cursor: pointer;
  transition: all .2s ease;
  box-shadow: 0 2px 8px rgba(47,107,255,.25);
}
.mobile-search-btn:hover { transform: translateY(-1px); box-shadow: 0 4px 12px rgba(47,107,255,.35); }
.mobile-search-cancel {
  flex-shrink: 0;
  padding: 8px 12px;
  font-size: 14px; font-weight: 600; color: var(--ink-2);
  background: none; border: none; cursor: pointer;
  transition: color .3s ease;
}
.mobile-search-cancel:hover { color: var(--brand); }

/* 手机端横向滚动子导航栏：默认隐藏，手机端显示 */
.mobile-subnav {
  display: none;
  position: fixed;
  top: var(--nav-h);
  left: 0; right: 0;
  z-index: 49;
  background: rgba(255, 255, 255, .92);
  -webkit-backdrop-filter: blur(18px) saturate(1.8);
  backdrop-filter: blur(18px) saturate(1.8);
  border-bottom: 1px solid var(--glass-border);
  padding: 8px 16px;
  gap: 8px;
  overflow-x: auto;
  white-space: nowrap;
  transition: opacity .3s ease, transform .3s ease;
}
.mobile-subnav.hidden { opacity: 0; pointer-events: none; transform: translateY(-8px); }
.mobile-subnav-link {
  display: inline-block;
  padding: 7px 16px;
  border-radius: var(--radius-pill);
  font-size: 14px; font-weight: 600; color: var(--ink-2);
  background: var(--bg);
  flex-shrink: 0;
  transition: color .3s ease, background .3s ease;
}
.mobile-subnav-link.active {
  color: #fff;
  background: linear-gradient(135deg, var(--brand-deep), var(--brand));
  box-shadow: 0 4px 12px rgba(47, 107, 255, .3);
}

/* 中等屏幕（笔记本）：隐藏 logo-tag，压缩搜索框与导航链接间距 */
@media (max-width: 1100px) {
  .logo-tag { display: none; }
  .nav-search { width: 160px; }
  .nav-link { padding: 9px 16px; font-size: 14px; }
  .nav-inner { gap: 16px; }
}

/* 移动端/平板：导航链接下移为子导航栏，顶部只留 logo + 搜索 + 用户 + 菜单 */
@media (max-width: 1100px) {
  .nav-links { display: none; }
  .nav-inner { justify-content: space-between; gap: 12px; }
  .logo { gap: 8px; }
  .logo-tag { display: none; }
  .logo-text { font-size: 24px; }
  .logo-mark { width: 36px; height: 36px; border-radius: 10px; }
  .logo-mark svg { width: 18px; height: 18px; }
  .icon-btn { width: 40px; height: 40px; border-radius: 12px; }
  .icon-btn .el-icon { font-size: 20px; }
  .nav-actions { gap: 6px; }
  .nav-search-desktop { display: none !important; }
  .nav-search-mobile { display: grid; place-items: center; }
  .mobile-search-bar { display: block; }
  .user-name { display: none; }
  .mobile-subnav { display: flex; align-items: center; }
}

@media (max-width: 480px) {
  .logo-text { font-size: 21px; letter-spacing: .5px; }
  .logo-mark { width: 32px; height: 32px; border-radius: 9px; }
  .icon-btn { width: 38px; height: 38px; }
  .login-btn { padding: 8px 16px; font-size: 13px; }
}

@media (max-width: 360px) {
  .logo-text { font-size: 18px; }
  .logo-mark { width: 30px; height: 30px; border-radius: 8px; }
  .icon-btn { width: 34px; height: 34px; border-radius: 10px; }
  .nav-actions { gap: 4px; }
  .login-btn { padding: 7px 12px; font-size: 12.5px; }
  .user-chip { padding: 3px 8px 3px 3px; }
  .user-avatar { width: 28px; height: 28px; font-size: 12px; }
}
</style>
