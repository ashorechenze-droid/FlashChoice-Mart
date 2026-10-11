import { defineStore } from 'pinia'
import { ElMessage, ElMessageBox } from 'element-plus'
import { login as apiLogin, register as apiRegister, logout as apiLogout } from '../api/member'
import router from '../router'

// 会员登录态（接口文档 3.1：登录签发双 Token accessToken + refreshToken）
const TOKEN_KEY = 'mall_token'
const REFRESH_TOKEN_KEY = 'mall_refresh_token'
const USER_KEY = 'mall_user'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem(TOKEN_KEY) || '',
    refreshToken: localStorage.getItem(REFRESH_TOKEN_KEY) || '',
    info: JSON.parse(localStorage.getItem(USER_KEY) || 'null'),
  }),

  getters: {
    isLoggedIn: (s) => !!s.token || !!s.info,
  },

  actions: {
    // 登录：后端返回 { member, accessToken, refreshToken, tokenType, expiresIn }
    async login(form) {
      const data = await apiLogin(form)
      const user = data.member || data.user || data
      this.token = data.accessToken || data.token || ''
      this.refreshToken = data.refreshToken || ''
      this.info = user
      localStorage.setItem(TOKEN_KEY, this.token)
      localStorage.setItem(REFRESH_TOKEN_KEY, this.refreshToken)
      localStorage.setItem(USER_KEY, JSON.stringify(user))
      // 会话互斥：会员登录后清除管理端会话（管理端令牌独立存于 mall_admin）
      localStorage.removeItem('mall_admin')
      // 登录后同步后端购物车与收藏（真实模式）
      try {
        const { useCartStore } = await import('./cart')
        await useCartStore().load()
      } catch (e) { /* 购物车同步失败不影响登录 */ }
      try {
        const { useFavoriteStore } = await import('./favorite')
        await useFavoriteStore().load()
      } catch (e) { /* 收藏同步失败不影响登录 */ }
      try {
        const { useNoticeStore } = await import('./notice')
        await useNoticeStore().load()
      } catch (e) { /* 通知同步失败不影响登录 */ }
      return data
    },

    async register(form) {
      const data = await apiRegister(form)
      const user = data.member || data.user || data
      this.token = data.accessToken || data.token || ''
      this.refreshToken = data.refreshToken || ''
      this.info = user
      localStorage.setItem(TOKEN_KEY, this.token)
      localStorage.setItem(REFRESH_TOKEN_KEY, this.refreshToken)
      localStorage.setItem(USER_KEY, JSON.stringify(user))
      // 会话互斥：会员登录后清除管理端会话（管理端令牌独立存于 mall_admin）
      localStorage.removeItem('mall_admin')
      try {
        const { useCartStore } = await import('./cart')
        await useCartStore().load()
      } catch (e) { /* 忽略 */ }
      try {
        const { useFavoriteStore } = await import('./favorite')
        await useFavoriteStore().load()
      } catch (e) { /* 忽略 */ }
      try {
        const { useNoticeStore } = await import('./notice')
        await useNoticeStore().load()
      } catch (e) { /* 忽略 */ }
      return data
    },

    async logout() {
      // 通知后端吊销令牌（接口文档 3.1：POST /api/members/logout）
      try {
        if (this.token) await apiLogout({ refreshToken: this.refreshToken })
      } catch (e) { /* 后端登出失败不影响本地清理 */ }
      this.token = ''
      this.refreshToken = ''
      this.info = null
      localStorage.removeItem(TOKEN_KEY)
      localStorage.removeItem(REFRESH_TOKEN_KEY)
      localStorage.removeItem(USER_KEY)
      // 清理本地收藏缓存（后端收藏数据保留）
      try {
        const { useFavoriteStore } = await import('./favorite')
        useFavoriteStore().reset()
      } catch (e) { /* 忽略 */ }
      // 清理消息中心缓存（后端通知数据保留）
      try {
        const { useNoticeStore } = await import('./notice')
        useNoticeStore().reset()
      } catch (e) { /* 忽略 */ }
      ElMessage.success('已退出登录')
      // 退出后统一跳转到首页，避免停留在需要登录的页面
      router.push('/')
    },

    // 登录拦截：游客可浏览商城，但交易类操作（加购/收藏/秒杀/结算）需登录
    requireLogin() {
      if (this.isLoggedIn) return true
      ElMessageBox.confirm('该操作需要登录后才能进行，是否前往登录？', '登录提示', {
        confirmButtonText: '去登录',
        cancelButtonText: '再逛逛',
        type: 'info',
      })
        .then(() => {
          router.push({ path: '/login', query: { redirect: router.currentRoute.value.fullPath } })
        })
        .catch(() => {})
      return false
    },
  },
})
