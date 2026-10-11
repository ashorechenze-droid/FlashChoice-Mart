import { defineStore } from 'pinia'
import { ElMessage } from 'element-plus'
import { useUserStore } from './user'
import { USE_MOCK } from '../config'
import { getCollects, collectGood, removeCollectByGood } from '../api/collect'
import { getGoodsByIds } from '../api/goods'

// 收藏状态：
// - mock 模式：本地持久化（localStorage），纯前端演示
// - 真实模式：以后端 /api/collects 为数据源（登录后 load 同步），商品信息由 collect-api 经 good-api Feign 回显
// items 结构：[{ id(后端收藏行id，mock 为 null), good }]
const KEY = 'mall_favorites'

// 兼容历史本地数据：旧版本存的是 [good]，统一规范化为 [{ id, good }]
function normalize(raw) {
  return (Array.isArray(raw) ? raw : [])
    .map((it) => (it && it.good ? it : { id: null, good: it }))
    .filter((it) => it.good)
}

export const useFavoriteStore = defineStore('favorite', {
  state: () => ({
    items: normalize(JSON.parse(localStorage.getItem(KEY) || '[]')),
    loading: false,
  }),

  getters: {
    count: (s) => s.items.length,
  },

  actions: {
    persist() {
      localStorage.setItem(KEY, JSON.stringify(this.items))
    },

    // 登录后从后端加载收藏（真实模式）
    async load() {
      if (USE_MOCK) return
      const user = useUserStore()
      if (!user.isLoggedIn) return
      this.loading = true
      try {
        const list = await getCollects()
        let items = (list || [])
          .filter((it) => it.good)
          .map((it) => ({ id: it.id, good: it.good }))
        // 补充商品图片：收藏接口回显的 good 对象可能不含 pic 字段，批量从商品详情补全
        const missingPicIds = items
          .filter((it) => !it.good.pic && !it.good.goodPic && !it.good.image)
          .map((it) => it.good.id)
        if (missingPicIds.length > 0) {
          try {
            const goodsDetail = await getGoodsByIds(missingPicIds)
            const picMap = {}
            ;(goodsDetail || []).forEach((g) => { picMap[g.id] = g.pic || g.goodPic || '' })
            items = items.map((it) => ({
              ...it,
              good: { ...it.good, pic: it.good.pic || picMap[it.good.id] || it.good.pic },
            }))
          } catch (e) { /* 补充图片失败不影响收藏列表展示 */ }
        }
        this.items = items
        this.persist()
      } catch (e) {
        // 后端收藏服务暂不可用时保留本地数据
      } finally {
        this.loading = false
      }
    },

    isFavorite(id) {
      return this.items.some((it) => it.good.id === id)
    },

    // 收藏 / 取消收藏：真实模式同步后端（收藏接口幂等；取消按商品删除，归属由服务端校验）
    async toggle(good) {
      const hit = this.items.find((it) => it.good.id === good.id)
      if (hit) {
        if (!USE_MOCK) {
          try {
            await removeCollectByGood(good.id)
          } catch (e) {
            return true
          }
        }
        this.items = this.items.filter((it) => it !== hit)
        this.persist()
        ElMessage.success('已取消收藏')
        return false
      }
      if (!USE_MOCK) {
        if (!useUserStore().requireLogin()) return false
        try {
          await collectGood({ goodId: good.id })
        } catch (e) {
          return false
        }
      }
      this.items.push({ id: null, good })
      this.persist()
      ElMessage.success(`「${good.name}」已加入收藏`)
      return true
    },

    // 移除收藏条目（会员中心/收藏面板用；按商品撤销收藏）
    async remove(idOrItem) {
      const item = typeof idOrItem === 'object'
        ? idOrItem
        : this.items.find((it) => it.good.id === idOrItem || it.id === idOrItem)
      if (!item) return
      if (!USE_MOCK) {
        try {
          await removeCollectByGood(item.good.id)
        } catch (e) {
          return
        }
      }
      this.items = this.items.filter((x) => x !== item)
      this.persist()
    },

    // 登出时清理本地缓存（后端收藏数据保留）
    reset() {
      this.items = []
      this.persist()
    },
  },
})
