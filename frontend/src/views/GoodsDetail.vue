<template>
  <div class="goods-detail" v-if="good">
    <div class="container">
      <!-- 面包屑 -->
      <div class="crumbs reveal">
        <span class="crumb" @click="router.push('/')">首页</span>
        <el-icon><ArrowRight /></el-icon>
        <span class="crumb" @click="router.push('/goods')">全部商品</span>
        <el-icon><ArrowRight /></el-icon>
        <span class="crumb current">{{ good.name }}</span>
      </div>

      <div class="detail-grid">
        <!-- 左：商品大图 -->
        <div class="detail-media reveal">
          <img :src="good.pic" :alt="good.name">
        </div>

        <!-- 右：信息与操作 -->
        <div class="detail-info reveal">
          <div class="info-badges">
            <span v-if="good.isHot" class="badge badge-hot">热销</span>
            <span v-if="good.isSeckill" class="badge badge-seckill">秒杀商品</span>
            <span v-if="brandName" class="badge badge-brand">{{ brandName }}</span>
          </div>
          <h1 class="info-name">{{ good.name }}</h1>
          <p v-if="good.alias" class="info-alias">{{ good.alias }}</p>
          <p class="info-summary">{{ good.summary }}</p>

          <div class="info-price-box">
            <span class="info-price">¥{{ good.price }}</span>
            <span v-if="good.markPrice" class="info-mark">¥{{ good.markPrice }}</span>
            <span v-if="good.qty != null" class="info-stock">库存 {{ good.qty }} 件</span>
          </div>

          <div class="info-desc">{{ good.description || good.summary }}</div>

          <div v-if="good.tags && good.tags.length" class="info-tags">
            <span v-for="t in good.tags" :key="t">{{ t }}</span>
          </div>

          <div class="info-buy">
            <span class="buy-label">数量</span>
            <el-input-number v-model="qty" :min="1" :max="Math.max(1, good.qty || 99)" />
          </div>

          <div class="info-actions">
            <el-button size="large" round class="act-fav" :class="{ liked: fav.isFavorite(good.id) }"
                       @click="onFav">
              <el-icon><Star /></el-icon>
              {{ fav.isFavorite(good.id) ? '已收藏' : '收藏' }}
            </el-button>
            <el-button type="primary" size="large" round class="act-cart" @click="onAdd">
              <el-icon><ShoppingCart /></el-icon> 加入购物车
            </el-button>
            <el-button size="large" round class="act-buy" @click="onBuy">
              <el-icon><Lightning /></el-icon> 立即购买
            </el-button>
          </div>

          <!-- 游客提示：可浏览，交易需登录 -->
          <p v-if="!user.isLoggedIn" class="guest-tip">
            当前为游客模式，可正常浏览商品；加购 / 收藏 / 购买前需要先登录。
          </p>
        </div>
      </div>
    </div>
  </div>

  <!-- 加载态 -->
  <div v-else class="detail-loading container">
    <el-skeleton animated :rows="5" />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getGoodsDetail } from '../api/goods'
import { getBrands } from '../api/brand'
import { useUserStore } from '../stores/user'
import { useCartStore } from '../stores/cart'
import { useFavoriteStore } from '../stores/favorite'
import { initReveal } from '../utils/reveal'

const route = useRoute()
const router = useRouter()
const user = useUserStore()
const cart = useCartStore()
const fav = useFavoriteStore()

const good = ref(null)
const qty = ref(1)
const brands = ref([])

const brandName = computed(() => {
  if (!good.value?.brandId) return ''
  const b = brands.value.find((x) => x.id === good.value.brandId)
  return b ? b.name : ''
})

async function load() {
  ;[good.value, brands.value] = await Promise.all([getGoodsDetail(route.params.id), getBrands()])
  qty.value = 1
}

onMounted(async () => {
  await load()
  requestAnimationFrame(() => initReveal())
})

function onAdd() {
  if (!user.requireLogin()) return
  cart.add(good.value, qty.value)
}

// 立即购买：真实模式直接提交订单（POST /api/orders {goodId, qty, addrId}）
// 收货地址取默认地址，未设置默认地址时取第一条；没有地址则引导到个人中心添加
async function onBuy() {
  if (!user.requireLogin()) return
  const { listAddresses } = await import('../api/address')
  const { createOrder } = await import('../api/order')
  const addresses = await listAddresses()
  if (!addresses || !addresses.length) {
    ElMessage.warning('请先在个人中心添加收货地址后再购买')
    router.push('/member')
    return
  }
  const addr = addresses.find((a) => a.isDefault) || addresses[0]
  try {
    const order = await createOrder({ goodId: good.value.id, qty: qty.value, addrId: addr.id })
    ElMessage.success(`订单提交成功：${order.orderNo}`)
    router.push('/member')
  } catch (e) { /* 拦截器已提示 */ }
}

function onFav() {
  if (!user.requireLogin()) return
  fav.toggle(good.value)
}
</script>

<style scoped>
.goods-detail { padding: 24px 0 64px; }

.crumbs { display: flex; align-items: center; gap: 8px; font-size: 13px; color: var(--ink-2); margin-bottom: 24px; }
.crumb { cursor: pointer; transition: color .3s ease; }
.crumb:hover { color: var(--brand); }
.crumb.current { color: var(--ink); font-weight: 600; cursor: default; }

.detail-grid {
  display: grid;
  grid-template-columns: 1.05fr 1fr;
  gap: 44px;
  align-items: start;
}

.detail-media {
  border-radius: var(--radius-lg);
  overflow: hidden;
  background: var(--surface);
  box-shadow: var(--shadow-md);
}
.detail-media img { width: 100%; aspect-ratio: 1 / 1; object-fit: cover; }

.detail-info { display: flex; flex-direction: column; gap: 14px; }
.info-badges { display: flex; gap: 8px; }
.badge {
  font-size: 12px; font-weight: 700; letter-spacing: 1px;
  color: #fff; border-radius: var(--radius-pill);
  padding: 4px 12px;
}
.badge-hot { background: var(--accent); }
.badge-seckill { background: var(--price); }
.badge-brand { background: var(--brand-deep); }

.info-name { font-size: 30px; font-weight: 800; line-height: 1.3; }
.info-alias { font-size: 14px; color: var(--ink-2); }
.info-summary { font-size: 15px; color: var(--ink-2); }

.info-price-box {
  display: flex; align-items: baseline; gap: 12px;
  background: var(--brand-soft);
  border-radius: var(--radius-md);
  padding: 14px 18px;
}
.info-price { font-size: 32px; font-weight: 900; color: var(--price); font-variant-numeric: tabular-nums; }
.info-mark { font-size: 15px; color: var(--ink-2); text-decoration: line-through; }
.info-stock { margin-left: auto; font-size: 13px; color: var(--ink-2); }

.info-desc { font-size: 14.5px; line-height: 1.8; color: var(--ink-2); }
.info-tags { display: flex; flex-wrap: wrap; gap: 8px; }
.info-tags span {
  font-size: 12.5px; font-weight: 600; color: var(--brand-deep);
  background: var(--brand-soft);
  border-radius: 8px; padding: 4px 10px;
}

.info-buy { display: flex; align-items: center; gap: 14px; }
.buy-label { font-size: 14px; color: var(--ink-2); font-weight: 600; }

.info-actions { display: flex; gap: 12px; flex-wrap: wrap; margin-top: 4px; }
.act-fav { border-color: rgba(255, 77, 106, .35); color: var(--ink-2); }
.act-fav.liked { border-color: var(--like); color: var(--like); background: rgba(255, 77, 106, .06); }
.act-buy {
  background: linear-gradient(135deg, #F04E23, #FF6B35);
  border: none; color: #fff;
}

.guest-tip {
  font-size: 12.5px; color: var(--ink-2);
  background: rgba(255, 107, 53, .08);
  border: 1px dashed rgba(255, 107, 53, .35);
  border-radius: 10px;
  padding: 10px 14px;
}

.detail-loading { padding: 60px 0; }

@media (max-width: 768px) {
  .detail-grid { grid-template-columns: 1fr; gap: 22px; }
  .info-name { font-size: 22px; }
  .info-price { font-size: 26px; }
  .info-actions .el-button { flex: 1; }
}
</style>
