import { defineStore } from 'pinia'

// 全局 UI 状态：液态菜单开关 + 商品弹窗（购物车 / 收藏）
export const useUiStore = defineStore('ui', {
  state: () => ({
    menuOpen: false,
    panelType: '',       // 'cart' | 'favorite'
    panelVisible: false,
  }),
  actions: {
    openPanel(type) {
      this.panelType = type
      this.panelVisible = true
    },
    closePanel() {
      this.panelVisible = false
    },
  },
})
