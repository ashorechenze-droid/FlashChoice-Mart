<template>
  <!-- 商品弹窗：展示「购物车」或「收藏」内的商品（较大的对话框窗口） -->
  <el-dialog :model-value="ui.panelVisible" :title="title" width="720px" align-center
             class="goods-panel" @update:model-value="ui.panelVisible = $event" @close="ui.closePanel">
    <!-- 购物车视图 -->
    <template v-if="ui.panelType === 'cart'">
      <div v-if="cart.items.length" class="panel-list">
        <div v-for="it in cart.items" :key="it.good.id" class="panel-row">
          <img class="row-pic" :src="it.good.pic" :alt="it.good.name">
          <div class="row-info">
            <p class="row-name">{{ it.good.name }}</p>
            <p class="row-price">¥{{ it.good.price }}</p>
          </div>
          <el-input-number v-model="it.qty" :min="1" :max="99" size="small" @change="cart.changeQty(it, it.qty)" />
          <button class="row-del" aria-label="移除" @click="cart.remove(it)">
            <el-icon><Delete /></el-icon>
          </button>
        </div>
        <div class="panel-foot">
          <span>合计：<b class="total">¥{{ cart.totalPrice }}</b></span>
          <el-button type="primary" round @click="checkout">去结算</el-button>
        </div>
      </div>
      <el-empty v-else description="购物车还是空的，去挑几件好物吧" :image-size="120" />
    </template>

    <!-- 收藏视图 -->
    <template v-else>
      <div v-if="fav.items.length" class="panel-list">
        <div v-for="it in fav.items" :key="it.good.id" class="panel-row">
          <img class="row-pic" :src="it.good.pic" :alt="it.good.name">
          <div class="row-info">
            <p class="row-name">{{ it.good.name }}</p>
            <p class="row-price">¥{{ it.good.price }}</p>
          </div>
          <el-button type="primary" size="small" round @click="addFromFav(it.good)">加入购物车</el-button>
          <button class="row-del" aria-label="移除收藏" @click="fav.remove(it)">
            <el-icon><Delete /></el-icon>
          </button>
        </div>
      </div>
      <el-empty v-else description="还没有收藏的商品，点击卡片上的星标即可收藏" :image-size="120" />
    </template>
  </el-dialog>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUiStore } from '../stores/ui'
import { useCartStore } from '../stores/cart'
import { useFavoriteStore } from '../stores/favorite'
import { useUserStore } from '../stores/user'

// 全局商品弹窗：由 FloatingTool 打开，展示购物车 / 收藏商品
const ui = useUiStore()
const cart = useCartStore()
const fav = useFavoriteStore()
const user = useUserStore()
const router = useRouter()

const title = computed(() => (ui.panelType === 'cart' ? '我的购物车' : '我的收藏'))

// 去结算：真实模式选择默认收货地址并提交订单（POST /api/orders {cartIds, addrId}）
async function checkout() {
  if (!user.requireLogin()) return
  const { listAddresses } = await import('../api/address')
  const addresses = await listAddresses()
  if (!addresses || !addresses.length) {
    ElMessage.warning('请先在个人中心添加收货地址后再结算')
    router.push('/member')
    return
  }
  const addr = addresses.find((a) => a.isDefault) || addresses[0]
  const order = await cart.checkout(addr.id)
  if (order) {
    ElMessage.success(`订单提交成功：${order.orderNo}`)
    ui.closePanel()
    router.push('/member')
  }
}

function addFromFav(g) {
  if (!user.requireLogin()) return
  cart.add(g)
}
</script>

<style scoped>
.goods-panel :deep(.el-dialog) { border-radius: 18px; overflow: hidden; }
.goods-panel :deep(.el-dialog__title) { font-weight: 700; }

.panel-list { display: flex; flex-direction: column; gap: 14px; max-height: 52vh; overflow-y: auto; padding-right: 4px; }
.panel-row {
  display: flex; align-items: center; gap: 14px;
  padding: 12px;
  border: 1px solid var(--line);
  border-radius: 14px;
  transition: box-shadow .3s ease, transform .3s var(--elastic);
}
.panel-row:hover { box-shadow: var(--shadow-sm); transform: translateY(-2px); }
.row-pic { width: 64px; height: 64px; border-radius: 10px; object-fit: cover; flex-shrink: 0; }
.row-info { flex: 1; min-width: 0; }
.row-name { font-size: 15px; font-weight: 600; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.row-price { font-size: 16px; font-weight: 800; color: var(--price); margin-top: 2px; }
.row-del {
  width: 36px; height: 36px; border-radius: 10px;
  display: grid; place-items: center;
  color: var(--ink-2);
  transition: color .3s ease, background .3s ease;
}
.row-del:hover { color: var(--price); background: rgba(255, 77, 79, .08); }

.panel-foot {
  display: flex; align-items: center; justify-content: space-between;
  padding-top: 14px;
  border-top: 1px dashed var(--line);
  font-size: 14px; color: var(--ink-2);
}
.total { font-size: 22px; font-weight: 800; color: var(--price); font-variant-numeric: tabular-nums; }

@media (max-width: 768px) {
  .goods-panel :deep(.el-dialog) { width: 92vw !important; }
  .row-pic { width: 52px; height: 52px; }
  .goods-panel :deep(.el-dialog__body) { padding-bottom: calc(16px + var(--safe-bottom)); }
}
</style>
