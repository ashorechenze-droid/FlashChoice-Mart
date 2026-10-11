import { defineStore } from 'pinia'
import { useUserStore } from './user'
import { USE_MOCK } from '../config'
import { getNotices, getUnreadCount, markNoticeRead, markAllNoticesRead } from '../api/notice'

// 消息中心（站内信）：
// - mock 模式：内存演示数据（api/notice.js 的 mockNotices）
// - 真实模式：以后端 /api/notices 为数据源（登录后 load 同步），unreadCount 用于菜单红点与「全部已读」
export const useNoticeStore = defineStore('notice', {
  state: () => ({
    items: [],
    unreadCount: 0,
    loading: false,
  }),

  actions: {
    // 登录后加载通知列表与未读数（服务不可用时静默忽略，不影响其它功能）
    async load() {
      if (USE_MOCK) {
        this.items = await getNotices()
        this.unreadCount = this.items.filter((n) => !n.isRead).length
        return
      }
      const user = useUserStore()
      if (!user.isLoggedIn) return
      this.loading = true
      try {
        const [list, count] = await Promise.all([getNotices(), getUnreadCount()])
        this.items = list || []
        this.unreadCount = count || 0
      } catch (e) {
        // 通知服务暂不可用：保留现有列表
      } finally {
        this.loading = false
      }
    },

    // 单条标记已读（点击消息时同步后端）
    async markRead(n) {
      if (!n || n.isRead) return
      try {
        await markNoticeRead(n.id)
      } catch (e) {
        return
      }
      n.isRead = true
      this.unreadCount = Math.max(0, this.unreadCount - 1)
    },

    // 全部标记已读
    async markAllRead() {
      if (!this.unreadCount) return
      try {
        await markAllNoticesRead()
      } catch (e) {
        return
      }
      this.items.forEach((n) => { n.isRead = true })
      this.unreadCount = 0
    },

    // 登出清理本地缓存（后端通知数据保留）
    reset() {
      this.items = []
      this.unreadCount = 0
    },
  },
})
