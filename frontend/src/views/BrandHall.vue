<template>
  <div class="brand-hall">
    <div class="container">
      <div class="hall-head reveal">
        <h2>品牌馆</h2>
        <p>甄选品质品牌，直达你信赖的好物</p>
      </div>

      <div class="brand-grid">
        <div v-for="(b, i) in displayedBrands" :key="b.id" class="brand-card reveal"
             :class="{ active: activeBrandId === b.id }"
             :style="{ transitionDelay: (i % 4) * 0.06 + 's' }"
             role="button" tabindex="0" @click="goBrand(b)" @keydown.enter="goBrand(b)">
          <span class="brand-logo" :style="{ background: logoBg(brandIndex(b.id)) }">
            <img v-if="showLogo(b)" :src="logoSrc(b)" :alt="b.name" loading="lazy" @error="markLogoFailed(b.id)" />
            <template v-else>{{ b.name?.slice(0, 1) || '?' }}</template>
          </span>
          <div class="brand-info">
            <h3>{{ b.name }}</h3>
            <p>{{ b.company }}</p>
            <span class="brand-desc">{{ b.description }}</span>
          </div>
          <!-- 未选中：hover 显示"逛品牌"；选中：显示"返回全部品牌" -->
          <span v-if="activeBrandId !== b.id" class="brand-go">
            逛品牌 <el-icon><ArrowRight /></el-icon>
          </span>
          <span v-else class="brand-back" @click.stop="goBrand(b)">
            <el-icon><ArrowLeft /></el-icon> 全部品牌
          </span>
        </div>
      </div>

      <!-- 品牌商品：本页展示，不跳转 -->
      <div v-if="activeBrandId" class="brand-goods" ref="brandGoodsRef">
        <div class="brand-goods-head">
          <h3>{{ activeBrandName }} · 精选好物</h3>
          <span class="brand-goods-count">{{ brandGoods.length }} 件商品</span>
        </div>
        <div v-if="brandLoading" class="grid-skeleton">
          <el-skeleton v-for="i in 4" :key="i" animated :rows="2" />
        </div>
        <div v-else-if="brandGoods.length" class="products-grid">
          <div v-for="(g, i) in brandGoods" :key="g.id" class="card-reveal reveal"
               :style="{ transitionDelay: (i % 4) * 0.05 + 's' }">
            <ProductCard :good="g" :brand-name="activeBrandName" />
          </div>
        </div>
        <el-empty v-else description="该品牌暂无商品" :image-size="110" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, nextTick, onMounted } from 'vue'
import { getBrands } from '../api/brand'
import { listGoods } from '../api/goods'
import ProductCard from '../components/ProductCard.vue'
import { initReveal } from '../utils/reveal'

const brands = ref([])
const failedLogos = ref(new Set())

// 选中品牌：本页展示商品（不跳转）
const activeBrandId = ref(null)
const brandGoods = ref([])
const brandLoading = ref(false)
const brandGoodsRef = ref(null)

const activeBrandName = computed(() => {
  const b = brands.value.find((x) => x.id === activeBrandId.value)
  return b ? b.name : ''
})

// 选中品牌后只显示该品牌，未选中时显示全部
const displayedBrands = computed(() => {
  if (!activeBrandId.value) return brands.value
  return brands.value.filter((b) => b.id === activeBrandId.value)
})

// 根据品牌 id 找原始索引，保证 logo 配色在过滤前后一致
function brandIndex(id) {
  const idx = brands.value.findIndex((b) => b.id === id)
  return idx >= 0 ? idx : 0
}

// 品牌 Logo 占位配色（按索引轮换）
const LOGO_COLORS = [
  'linear-gradient(135deg,#1B4DF5,#5B8CFF)',
  'linear-gradient(135deg,#F04E23,#FF9A5A)',
  'linear-gradient(135deg,#0F766E,#2BB8A0)',
  'linear-gradient(135deg,#6D28D9,#8B5CF6)',
  'linear-gradient(135deg,#D97706,#FBBF24)',
  'linear-gradient(135deg,#BE123C,#FB7185)',
]
const logoBg = (i) => LOGO_COLORS[i % LOGO_COLORS.length]

// 数据库中的旧版 doubaocdn 短链接已失效，使用项目自带的品牌图片。
const localBrandLogos = {
  华为: 'huawei', 小米: 'xiaomi', 苹果: 'apple', 联想: 'lenovo', 格力: 'gree',
  美的: 'midea', 海尔: 'haier', 戴尔: 'dell', 惠普: 'hp', 耐克: 'nike',
  李宁: 'lining', 三只松鼠: 'threesquirrels', 良品铺子: 'bestore',
  完美日记: 'perfectdiary', 苏泊尔: 'supor',
}

function logoSrc(brand) {
  const logo = typeof brand.logo === 'string' ? brand.logo.trim() : ''
  const localLogo = localBrandLogos[brand.name]
  if (localLogo && (!logo || /^https:\/\/aka\.doubaocdn\.com\/s\//i.test(logo))) {
    return `/brands/${localLogo}.jpg`
  }
  return /^(https?:\/\/|\/(?!\/))/i.test(logo) ? logo : ''
}

function showLogo(brand) {
  return !!logoSrc(brand) && !failedLogos.value.has(brand.id)
}

function markLogoFailed(id) {
  failedLogos.value = new Set([...failedLogos.value, id])
}

async function goBrand(b) {
  // 再次点击同一品牌则收起，恢复显示全部品牌
  if (activeBrandId.value === b.id) {
    activeBrandId.value = null
    brandGoods.value = []
    // 等待 DOM 更新后重新初始化入场动画，并滚动回品牌网格顶部
    await nextTick()
    await new Promise((r) => requestAnimationFrame(r))
    initReveal()
    window.scrollTo({ top: document.querySelector('.brand-hall').offsetTop - 80, behavior: 'smooth' })
    return
  }
  activeBrandId.value = b.id
  brandLoading.value = true
  try {
    const data = await listGoods({ brandId: b.id, pageSize: 8 })
    brandGoods.value = data.list || []
  } finally {
    brandLoading.value = false
    // 等待 DOM 更新后再初始化入场动画，并平滑滚动到商品区域
    await nextTick()
    await new Promise((r) => requestAnimationFrame(r))
    initReveal()
    if (brandGoodsRef.value) {
      brandGoodsRef.value.scrollIntoView({ behavior: 'smooth', block: 'start' })
    }
  }
}

onMounted(async () => {
  brands.value = await getBrands()
  requestAnimationFrame(() => initReveal())
})
</script>

<style scoped>
.brand-hall { padding: 48px 0 64px; }
.hall-head { text-align: center; margin-bottom: 34px; }
.hall-head h2 { font-size: 32px; font-weight: 900; letter-spacing: 1px; }
.hall-head p { margin-top: 6px; font-size: 14px; color: var(--ink-2); }

.brand-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 24px;
}
.brand-card {
  display: flex; align-items: center; gap: 18px;
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: var(--radius-lg);
  padding: 24px;
  cursor: pointer;
  transition: transform .5s var(--elastic), box-shadow .45s ease, border-color .3s ease;
}
.brand-card:hover { transform: translateY(-5px); box-shadow: var(--shadow-md); border-color: rgba(47, 107, 255, .3); }
.brand-card.active {
  border-color: var(--brand);
  box-shadow: 0 10px 28px rgba(47, 107, 255, .22);
  background: linear-gradient(135deg, rgba(47, 107, 255, .04), rgba(47, 107, 255, .01));
}
.brand-card.active .brand-go { opacity: 1; transform: translateX(0); }

.brand-logo {
  flex-shrink: 0;
  width: 72px; height: 72px; border-radius: 20px;
  display: grid; place-items: center;
  color: #fff; font-size: 30px; font-weight: 900;
  box-shadow: 0 10px 24px rgba(21, 32, 58, .14);
  transition: transform .5s var(--elastic);
}
.brand-card:hover .brand-logo { transform: rotate(-6deg) scale(1.06); }
.brand-logo img {
  width: 100%; height: 100%;
  object-fit: cover;
  border-radius: 20px;
  background: #fff;
  display: block;
}

.brand-info { flex: 1; min-width: 0; }
.brand-info h3 { font-size: 18px; font-weight: 700; }
.brand-info p { font-size: 12.5px; color: var(--ink-2); margin: 2px 0 6px; }
.brand-desc {
  display: block;
  font-size: 13px; color: var(--ink-2); line-height: 1.7;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.brand-go {
  flex-shrink: 0;
  display: inline-flex; align-items: center; gap: 4px;
  font-size: 13px; font-weight: 600; color: var(--brand);
  opacity: 0; transform: translateX(-6px);
  transition: opacity .3s ease, transform .3s ease;
}
.brand-card:hover .brand-go { opacity: 1; transform: translateX(0); }

/* 选中状态：返回全部品牌按钮（始终可见） */
.brand-back {
  flex-shrink: 0;
  display: inline-flex; align-items: center; gap: 4px;
  padding: 8px 14px;
  border-radius: var(--radius-pill);
  font-size: 13px; font-weight: 600; color: #fff;
  background: linear-gradient(135deg, var(--brand-deep), var(--brand));
  box-shadow: 0 4px 12px rgba(47, 107, 255, .3);
  cursor: pointer;
  transition: transform .3s var(--elastic), box-shadow .3s ease;
}
.brand-back:hover { transform: scale(1.05); box-shadow: 0 6px 16px rgba(47, 107, 255, .4); }

@media (max-width: 768px) {
  .brand-grid { grid-template-columns: 1fr; gap: 14px; }
  .brand-card { padding: 18px; }
}

/* 品牌商品：本页展示 */
.brand-goods { margin-top: 40px; }
.brand-goods-head {
  display: flex; align-items: baseline; gap: 12px;
  margin-bottom: 20px;
}
.brand-goods-head h3 { font-size: 22px; font-weight: 800; }
.brand-goods-count { font-size: 13px; color: var(--ink-2); }
.products-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 24px;
}
.grid-skeleton { display: grid; grid-template-columns: repeat(4, 1fr); gap: 24px; }

@media (max-width: 768px) {
  .products-grid { grid-template-columns: repeat(2, 1fr); gap: 12px; }
  .grid-skeleton { grid-template-columns: repeat(2, 1fr); gap: 12px; }
}
</style>
