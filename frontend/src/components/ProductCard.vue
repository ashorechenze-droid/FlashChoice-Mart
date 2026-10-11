<template>
  <article class="product-card" role="button" tabindex="0"
           :aria-label="good.name"
           @click="goDetail" @keydown.enter="goDetail">
    <div class="card-media">
      <img :src="good.pic" :alt="good.name" loading="lazy" decoding="async">
      <span v-if="good.isHot" class="card-badge badge-hot">热销</span>
      <span v-else-if="good.isSeckill" class="card-badge badge-seckill">秒杀</span>

      <!-- 收藏按钮：需登录，仅本地收藏演示 -->
      <button class="fav-btn" :class="{ active: fav.isFavorite(good.id) }"
              :aria-label="fav.isFavorite(good.id) ? '取消收藏' : '收藏'"
              @click.stop="toggleFav">
        <el-icon><Star /></el-icon>
      </button>

      <!-- 加购按钮：点击弹出数量选择弹窗 -->
      <button class="add-btn" :class="{ added }" :aria-label="'加入购物车：' + good.name" @click.stop="openCartDialog">
        <el-icon><component :is="added ? 'Check' : 'Plus'" /></el-icon>
      </button>
    </div>

    <div class="card-body">
      <!-- 品牌标签 -->
      <div class="card-brand" v-if="brandName">
        <span class="brand-tag">{{ brandName }}</span>
      </div>
      <h3 class="card-name">{{ good.name }}</h3>
      <p class="card-summary">{{ good.summary }}</p>
      <div class="card-meta">
        <span class="price">¥{{ good.price }}</span>
        <span v-if="good.markPrice" class="mark-price">¥{{ good.markPrice }}</span>
        <span class="stock" v-if="good.qty != null">库存 {{ good.qty }}</span>
      </div>
    </div>

    <!-- 加入购物车确认弹窗 -->
    <el-dialog v-model="cartDialogVisible" width="420px" align-center class="cart-dialog"
               :title="'加入购物车'" @close="onDialogClose">
      <div class="cart-dialog-body">
        <img class="dialog-pic" :src="good.pic" :alt="good.name">
        <div class="dialog-info">
          <p class="dialog-name">{{ good.name }}</p>
          <p class="dialog-brand" v-if="brandName">品牌：{{ brandName }}</p>
          <p class="dialog-price">¥{{ good.price }}
            <span v-if="good.markPrice" class="dialog-mark">¥{{ good.markPrice }}</span>
          </p>
          <p class="dialog-stock" v-if="good.qty != null">库存 {{ good.qty }} 件</p>
        </div>
      </div>
      <div class="dialog-qty-row">
        <span class="qty-label">购买数量</span>
        <el-input-number v-model="selectedQty" :min="1" :max="good.qty || 99" size="default" />
      </div>
      <template #footer>
        <el-button @click="cartDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmAdd">确认加入</el-button>
      </template>
    </el-dialog>
  </article>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import { useCartStore } from '../stores/cart'
import { useFavoriteStore } from '../stores/favorite'

const props = defineProps({
  good: { type: Object, required: true },
  brandName: { type: String, default: '' },
})

const router = useRouter()
const user = useUserStore()
const cart = useCartStore()
const fav = useFavoriteStore()

const added = ref(false)
let addedTimer = null

// 加购弹窗状态
const cartDialogVisible = ref(false)
const selectedQty = ref(1)

// 点击卡片进入详情页（游客可浏览，交易操作才要求登录）
function goDetail() {
  router.push(`/goods/${props.good.id}`)
}

// 打开加购弹窗（先检查登录）
function openCartDialog() {
  if (!user.requireLogin()) return
  selectedQty.value = 1
  cartDialogVisible.value = true
}

// 确认加入购物车
function confirmAdd() {
  cart.add(props.good, selectedQty.value)
  cartDialogVisible.value = false
  added.value = true
  clearTimeout(addedTimer)
  addedTimer = setTimeout(() => { added.value = false }, 1400)
}

function onDialogClose() {
  selectedQty.value = 1
}

function toggleFav() {
  if (!user.requireLogin()) return
  fav.toggle(props.good)
}
</script>

<style scoped>
.product-card {
  position: relative;
  background: var(--surface);
  border-radius: 20px;
  box-shadow: var(--shadow-sm);
  overflow: hidden;
  cursor: pointer;
  display: flex; flex-direction: column;
  transition: transform .5s var(--elastic), box-shadow .45s ease;
}
.product-card:hover { transform: translateY(-5px); box-shadow: var(--shadow-md); }

.card-media { position: relative; aspect-ratio: 1 / 1; overflow: hidden; background: var(--bg); }
.card-media img { width: 100%; height: 100%; object-fit: cover; transition: transform .8s var(--smooth); }
.product-card:hover .card-media img { transform: scale(1.05); }

.card-badge {
  position: absolute; top: 12px; left: 12px; z-index: 2;
  font-size: 12px; font-weight: 700; letter-spacing: 1px;
  color: #fff; border-radius: var(--radius-pill);
  padding: 4px 12px;
  box-shadow: var(--shadow-sm);
}
.badge-hot { background: var(--accent); }
.badge-seckill { background: var(--price); }

/* 收藏按钮：右上角圆形 */
.fav-btn {
  position: absolute; top: 10px; right: 10px; z-index: 3;
  width: 40px; height: 40px; border-radius: 50%;
  display: grid; place-items: center;
  background: rgba(255, 255, 255, .9);
  color: var(--ink-2);
  box-shadow: var(--shadow-sm);
  transition: transform .4s var(--elastic), color .3s ease;
}
.fav-btn:hover { transform: scale(1.1); color: var(--like); }
.fav-btn.active {
  background: var(--like);
  color: #fff;
  box-shadow: 0 4px 14px rgba(255, 77, 106, .4);
}
.fav-btn.active:hover { color: #fff; transform: scale(1.1); }
.fav-btn.active .el-icon { animation: favPop .45s var(--elastic); }
@keyframes favPop { 0% { transform: scale(1); } 50% { transform: scale(1.4); } 100% { transform: scale(1); } }

/* 加购按钮：右下角圆形（触控 >= 44px） */
.add-btn {
  position: absolute; right: 14px; bottom: 14px; z-index: 3;
  width: 46px; height: 46px; border-radius: 50%;
  display: grid; place-items: center;
  background: #fff; color: var(--brand);
  box-shadow: 0 8px 20px rgba(21, 32, 58, .22);
  transition: background .3s ease, color .3s ease, transform .4s var(--elastic);
}
.add-btn:hover { transform: scale(1.12) rotate(-6deg); }
.add-btn .el-icon { font-size: 22px; font-weight: 700; }
.add-btn.added { background: var(--ok); color: #fff; animation: addedPulse .6s var(--elastic); }
@keyframes addedPulse { 0% { transform: scale(1); } 40% { transform: scale(1.35); } 100% { transform: scale(1); } }

.card-body { padding: 16px; display: flex; flex-direction: column; gap: 6px; }
.card-brand { margin-bottom: 2px; }
.brand-tag {
  display: inline-block;
  font-size: 11px; font-weight: 700; letter-spacing: .5px;
  color: var(--brand-deep);
  background: var(--brand-soft);
  border-radius: var(--radius-pill);
  padding: 2px 8px;
}
.card-name { font-size: 19px; font-weight: 600; line-height: 1.4; }
.card-summary { font-size: 13px; color: var(--ink-2); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.card-meta { display: flex; align-items: baseline; gap: 10px; margin-top: 4px; }
.price { font-size: 25px; font-weight: 800; color: var(--price); font-variant-numeric: tabular-nums; }
.mark-price { font-size: 14px; color: var(--ink-2); text-decoration: line-through; }
.stock { margin-left: auto; font-size: 12px; color: var(--ink-2); }

/* 加购弹窗 */
.cart-dialog :deep(.el-dialog) { border-radius: 18px; overflow: hidden; }
.cart-dialog :deep(.el-dialog__title) { font-weight: 700; }
.cart-dialog-body {
  display: flex; gap: 16px; align-items: flex-start;
  padding: 8px 0 16px;
}
.dialog-pic {
  width: 110px; height: 110px; border-radius: 14px;
  object-fit: cover; flex-shrink: 0;
  border: 1px solid var(--line);
}
.dialog-info { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 6px; }
.dialog-name { font-size: 17px; font-weight: 700; line-height: 1.4; }
.dialog-brand { font-size: 13px; color: var(--brand-deep); font-weight: 600; }
.dialog-price { font-size: 22px; font-weight: 800; color: var(--price); }
.dialog-mark { font-size: 14px; color: var(--ink-2); text-decoration: line-through; font-weight: 400; margin-left: 8px; }
.dialog-stock { font-size: 13px; color: var(--ink-2); }
.dialog-qty-row {
  display: flex; align-items: center; justify-content: space-between;
  padding: 14px 0; border-top: 1px dashed var(--line);
}
.qty-label { font-size: 14px; font-weight: 600; color: var(--ink); }

@media (max-width: 768px) {
  .product-card { border-radius: 16px; }
  .card-body { padding: 12px; gap: 4px; }
  .card-name { font-size: 15px; }
  .card-summary { display: none; }
  .price { font-size: 18px; }
  .mark-price { font-size: 12px; }
  .add-btn { width: 44px; height: 44px; right: 10px; bottom: 10px; }
  .fav-btn { width: 36px; height: 36px; top: 8px; right: 8px; }
  .card-badge { top: 8px; left: 8px; font-size: 11px; padding: 3px 9px; }
  .cart-dialog :deep(.el-dialog) { width: 92vw !important; }
  .dialog-pic { width: 90px; height: 90px; }
}
</style>
