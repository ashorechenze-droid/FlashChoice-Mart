import { createRouter, createWebHashHistory } from 'vue-router'

// 路由表：PC 商城前端（会员端）核心页面
// hash 模式：静态托管（Nginx）无需额外回退配置
const routes = [
  { path: '/', name: 'home', component: () => import('../views/Home.vue'), meta: { title: '首页' } },
  { path: '/goods', name: 'goodsList', component: () => import('../views/GoodsList.vue'), meta: { title: '全部商品' } },
  { path: '/goods/:id', name: 'goodsDetail', component: () => import('../views/GoodsDetail.vue'), meta: { title: '商品详情' } },
  { path: '/seckill', name: 'seckill', component: () => import('../views/Seckill.vue'), meta: { title: '秒杀会场' } },
  { path: '/brands', name: 'brandHall', component: () => import('../views/BrandHall.vue'), meta: { title: '品牌馆' } },
  { path: '/login', name: 'login', component: () => import('../views/Login.vue'), meta: { title: '登录' } },
  { path: '/member', name: 'memberCenter', component: () => import('../views/MemberCenter.vue'), meta: { title: '个人中心', requireAuth: true } },
  { path: '/admin', name: 'adminDashboard', component: () => import('../views/AdminDashboard.vue'), meta: { title: '管理端' } },
  { path: '/:pathMatch(.*)*', redirect: '/' },
]

const router = createRouter({
  history: createWebHashHistory(),
  routes,
  scrollBehavior(to, from, savedPosition) {
    // 路由切换回到顶部（详情返回列表时保留位置由 savedPosition 处理）
    return savedPosition || { top: 0 }
  },
})

// 路由守卫：个人中心需要登录（JWT 未接入前以 token 或用户信息判定）
router.beforeEach((to, from, next) => {
  if (to.meta.requireAuth) {
    const logged = localStorage.getItem('mall_token') || localStorage.getItem('mall_user')
    if (!logged) {
      next({ path: '/login', query: { redirect: to.fullPath } })
      return
    }
  }
  next()
})

router.afterEach((to) => {
  document.title = to.meta.title ? `${to.meta.title} · 极速优选` : '极速优选 · B2C 秒杀商城'
})

export default router
