import { defineStore } from 'pinia'
import { ElMessage } from 'element-plus'
import { useUserStore } from './user'
import { USE_MOCK } from '../config'
import { getCart, addToCart, updateCart, removeCart } from '../api/cart'
import { getGoodsByIds } from '../api/goods'
import { createOrder } from '../api/order'

// 购物车状态：
// - mock 模式：本地持久化（localStorage），纯前端演示
// - 真实模式：以后端 /api/carts 为数据源（登录后 load 同步），页面仍以 Pinia 驱动 UI
// items 结构：[{ cartId(后端购物车行id, mock 为 null), good, qty }]
const KEY = 'mall_cart'

export const useCartStore = defineStore('cart', {
  state: () => ({
    items: JSON.parse(localStorage.getItem(KEY) || '[]'),
    loading: false,
  }),

  getters: {
    count: (s) => s.items.reduce((n, it) => n + it.qty, 0),
    totalPrice: (s) => s.items.reduce((sum, it) => sum + it.qty * (it.good?.price || 0), 0),
  },

  actions: {
    persist() {
      localStorage.setItem(KEY, JSON.stringify(this.items))
    },

    // 登录后从后端加载购物车（真实模式），商品回显走 /api/goods/ids 批量查询
    async load() {
      if (USE_MOCK) return
      const user = useUserStore()
      if (!user.isLoggedIn) return
      this.loading = true
      try {
        const list = await getCart()
        const ids = [...new Set((list || []).map((it) => it.goodId).filter(Boolean))]
        let goods = []
        if (ids.length) {
          try { goods = await getGoodsByIds(ids) } catch (e) { goods = [] }
        }
        const map = Object.fromEntries(goods.map((g) => [g.id, g]))
        this.items = (list || [])
          .filter((it) => map[it.goodId])
          .map((it) => ({ cartId: it.id, good: map[it.goodId], qty: it.qty }))
        this.persist()
      } catch (e) {
        // 后端购物车暂不可用时保留本地数据
      } finally {
        this.loading = false
      }
    },

    // 加入购物车：真实模式同时写入后端（返回行 id 用于后续改量/删除/结算）
    async add(good, qty = 1) {
      if (USE_MOCK) {
        const hit = this.items.find((it) => it.good.id === good.id)
        if (hit) hit.qty += qty
        else this.items.push({ cartId: null, good, qty })
        this.persist()
        ElMessage.success(`「${good.name}」已加入购物车`)
        return
      }
      const user = useUserStore()
      if (!user.requireLogin()) return
      try {
        const row = await addToCart({ goodId: good.id, qty })
        const hit = this.items.find((it) => it.good.id === good.id)
        if (hit) {
          hit.qty += qty
          if (row && row.id) hit.cartId = row.id
        } else {
          this.items.push({ cartId: row?.id || null, good, qty })
        }
        this.persist()
        ElMessage.success(`「${good.name}」已加入购物车`)
      } catch (e) { /* 拦截器已提示 */ }
    },

    // 修改数量：真实模式同步 PUT /api/carts
    async changeQty(item, qty) {
      const q = Math.max(1, Number(qty) || 1)
      if (!USE_MOCK && item.cartId != null) {
        try {
          await updateCart({ id: item.cartId, qty: q })
        } catch (e) {
          return
        }
      }
      item.qty = q
      this.persist()
    },

    // 移除条目：真实模式同步 DELETE /api/carts
    async remove(idOrItem) {
      const item = typeof idOrItem === 'object' ? idOrItem : this.items.find((it) => it.good.id === idOrItem || it.cartId === idOrItem)
      if (!item) return
      if (!USE_MOCK && item.cartId != null) {
        try {
          await removeCart([item.cartId])
        } catch (e) {
          return
        }
      }
      this.items = this.items.filter((x) => x !== item)
      this.persist()
    },

    // 清空：真实模式同步删除后端条目
    async clear() {
      if (!USE_MOCK) {
        const ids = this.items.map((it) => it.cartId).filter(Boolean)
        if (ids.length) {
          try { await removeCart(ids) } catch (e) { /* 忽略 */ }
        }
      }
      this.items = []
      this.persist()
    },

    // 结算：真实模式按购物车条目提交订单（POST /api/orders {cartIds, addrId}）
    async checkout(addrId, comment = '') {
      if (USE_MOCK) {
        ElMessage.info('结算功能演示：联调时对接 /api/orders 提交订单')
        return null
      }
      const ids = this.items.map((it) => it.cartId).filter((x) => x != null)
      if (!ids.length) {
        ElMessage.warning('购物车为空，无法结算')
        return null
      }
      try {
        const order = await createOrder({ cartIds: ids, addrId, comment })
        this.items = []
        this.persist()
        return order
      } catch (e) {
        return null
      }
    },
  },
})
