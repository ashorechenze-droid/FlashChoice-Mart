<template>
  <div class="home">
    <!-- 全屏 Hero 宣传图 -->
    <section class="hero-full" id="heroTop">
      <div class="hero-bg"></div>
      <div class="hero-overlay"></div>
      <div class="hero-content">
        <h1 class="hero-title">极速优选</h1>
        <p class="hero-subtitle">品质好物 · 极速送达 · 限时秒杀</p>
        <div class="hero-btns">
          <button class="hero-btn primary" @click="scrollToMain">立即选购</button>
          <button class="hero-btn ghost" @click="$router.push('/seckill')">秒杀会场</button>
        </div>
      </div>
      <button class="scroll-down" @click="scrollToMain" aria-label="向下滚动">
        <span class="scroll-text">向下滑动</span>
        <el-icon class="scroll-arrow"><ArrowDown /></el-icon>
      </button>
    </section>

    <!-- 主功能区 -->
    <div class="home-main" ref="mainRef">
    <!-- 打字机标语 -->
    <Typewriter :slogans="slogans" />

    <!-- Hero 轮播 + 左侧分类菜单（京东/淘宝风格） -->
    <section class="container hero-section">
      <CategoryMenu :categories="categoryTree" @select="onCategoryMenuSelect" />
      <div class="hero-carousel-wrap">
        <HeroCarousel :slides="heroSlides">
          <template #cta="{ slide }">
            <button class="hero-cta" @click="onHeroCta(slide)">
              {{ slide.cta }}
            </button>
          </template>
        </HeroCarousel>
      </div>
    </section>

    <!-- 分类 Tab：点击在本页展示对应分类的商品，不跳转 -->
    <section class="section" id="categories">
      <div class="container">
        <div class="section-head reveal">
          <h2>选购分类</h2>
          <p>挑选你喜欢的品类，快速找到好物</p>
        </div>
        <div class="reveal">
          <CategoryTabs :categories="topCategories" :model-value="activeCategoryId" @select="onCategory" />
        </div>

        <!-- 分类商品：本页展示，不跳转 -->
        <div class="category-goods">
          <div class="category-goods-head">
            <h3>{{ activeCategoryName }}</h3>
            <span class="category-goods-count">{{ categoryGoods.length }} 件好物</span>
          </div>
          <div v-if="categoryLoading" class="grid-skeleton">
            <el-skeleton v-for="i in 4" :key="i" animated :rows="2" />
          </div>
          <div v-else-if="categoryGoods.length" class="products-grid">
            <div v-for="(g, i) in categoryGoods" :key="g.id" class="card-reveal reveal"
                 :style="{ transitionDelay: (i % 4) * 0.05 + 's' }">
              <ProductCard :good="g" :brand-name="getBrandName(g.brandId)" />
            </div>
          </div>
          <el-empty v-else description="该分类暂无商品" :image-size="110" />
        </div>
      </div>
    </section>

    <!-- 热销甄选 -->
    <section class="section section-tint">
      <div class="container">
        <div class="section-head reveal">
          <h2>热销甄选</h2>
          <p>大家都在买的好物</p>
        </div>
        <div v-if="hotLoading" class="grid-skeleton">
          <el-skeleton v-for="i in 4" :key="i" animated :rows="2" />
        </div>
        <div v-else class="products-grid">
          <div v-for="(g, i) in hotGoods" :key="g.id" class="card-reveal reveal"
               :style="{ transitionDelay: (i % 4) * 0.06 + 's' }">
            <ProductCard :good="g" :brand-name="getBrandName(g.brandId)" />
          </div>
        </div>
      </div>
    </section>

    <!-- 秒杀氛围条 -->
    <section class="container seckill-banner reveal">
      <div class="seckill-card">
        <span class="seckill-icon" aria-hidden="true"><el-icon><Lightning /></el-icon></span>
        <div>
          <h3>限时秒杀 · 每晚 20:00</h3>
          <p>爆款直降到底 · 手快有手慢无</p>
        </div>
        <button class="seckill-btn" @click="router.push('/seckill')">去秒杀</button>
      </div>
    </section>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, nextTick, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowDown } from '@element-plus/icons-vue'
import Typewriter from '../components/Typewriter.vue'
import HeroCarousel from '../components/HeroCarousel.vue'
import CategoryTabs from '../components/CategoryTabs.vue'
import CategoryMenu from '../components/CategoryMenu.vue'
import ProductCard from '../components/ProductCard.vue'
import { listHotGoods, listGoods } from '../api/goods'
import { getCategories, getCategoryTree } from '../api/category'
import { getBrands } from '../api/brand'
import { initReveal } from '../utils/reveal'

const router = useRouter()
const mainRef = ref(null)

// 平滑滚动到主功能区
function scrollToMain() {
  mainRef.value?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

const slogans = ['质感生活，即刻拥有', '每一件都值得细品', '冷静克制，高级之选', '好物不将就']

const heroSlides = [
  { tag: '新品首发', title: '极速优选 · 焕新季', desc: '甄选全球好物，用克制的高级感装点日常', cta: '去看看', icon: 'Present', bg: 'linear-gradient(120deg,#1B4DF5 0%,#2F6BFF 45%,#6E8DFF 100%)' },
  { tag: '限时秒杀', title: '秒杀专场 火力全开', desc: '爆款直降到底，手快有手慢无', cta: '抢秒杀', icon: 'Lightning', bg: 'linear-gradient(120deg,#F04E23 0%,#FF6B35 50%,#FF9A5A 100%)' },
  { tag: '品质严选', title: '每一件都经过挑剔', desc: '产地直采 · 层层质检 · 售后无忧', cta: '逛好物', icon: 'ShoppingBag', bg: 'linear-gradient(120deg,#0B6B5C 0%,#0F766E 50%,#2BB8A0 100%)' },
]

const categories = ref([])
const categoryTree = ref([])
const brands = ref([])
const hotGoods = ref([])
const hotLoading = ref(true)

// 选购分类：本页展示商品（不跳转）
const activeCategoryId = ref(0)
const categoryGoods = ref([])
const categoryLoading = ref(false)

const activeCategoryName = computed(() => {
  const c = categories.value.find((x) => x.id === activeCategoryId.value)
  return c ? c.name : '全部好物'
})

async function loadCategoryGoods(catId) {
  categoryLoading.value = true
  try {
    const data = await listGoods({ categoryId: catId, pageSize: 8 })
    categoryGoods.value = data.list || []
  } finally {
    categoryLoading.value = false
    await nextTick()
    await new Promise((r) => requestAnimationFrame(r))
    initReveal()
  }
}

// 品牌ID -> 名称映射
const brandMap = computed(() => {
  const m = {}
  brands.value.forEach((b) => { m[b.id] = b.name })
  return m
})
function getBrandName(brandId) {
  return brandMap.value[brandId] || ''
}

// 选购分类：只展示一级分类（根分类），点击后展示该大类商品
const topCategories = computed(() => {
  return categories.value.filter((c) => c.parentId === 0 || c.parentId == null)
})

function onHeroCta(slide) {
  if (slide.title.includes('秒杀')) router.push('/seckill')
  else if (slide.title.includes('会员')) router.push('/login')
  else router.push('/goods')
}

function onCategory(cat) {
  activeCategoryId.value = cat.id
  loadCategoryGoods(cat.id)
}

// 左侧分类菜单点击：跳转到商品列表页（CategoryMenu 内部已处理跳转，此处留作扩展）
function onCategoryMenuSelect(cat) {
  // CategoryMenu 组件内部已 router.push，这里可加埋点等扩展逻辑
}

onMounted(async () => {
  ;[categories.value, categoryTree.value, brands.value, hotGoods.value] = await Promise.all([
    getCategories(), getCategoryTree(), getBrands(), listHotGoods(),
  ])
  hotLoading.value = false
  // 默认选中第一个分类并在本页加载商品
  if (topCategories.value.length) {
    activeCategoryId.value = topCategories.value[0].id
    loadCategoryGoods(topCategories.value[0].id)
  }
  await new Promise((r) => requestAnimationFrame(r))
  initReveal()
})
</script>

<style scoped>
.hero-cta {
  display: inline-block;
  padding: 13px 34px;
  border-radius: var(--radius-pill);
  background: #fff; color: var(--ink);
  font-size: 15px; font-weight: 700;
  box-shadow: 0 10px 24px rgba(0, 0, 0, .16);
  transition: transform .4s var(--elastic), box-shadow .3s ease;
}
.hero-cta:hover { transform: translateY(-3px) scale(1.03); box-shadow: 0 14px 30px rgba(0, 0, 0, .2); }

.section-tint { background: rgba(255, 255, 255, .5); }

/* Hero 区域：左侧分类菜单 + 右侧轮播图 */
.hero-section {
  display: flex;
  gap: 16px;
  align-items: stretch;
  margin-bottom: 8px;
}
.hero-carousel-wrap {
  flex: 1;
  min-width: 0;
}

@media (max-width: 768px) {
  .hero-section {
    flex-direction: column;
  }
}

/* 分类商品：本页展示 */
.category-goods { margin-top: 28px; }
.category-goods-head {
  display: flex; align-items: baseline; gap: 12px;
  margin-bottom: 18px;
}
.category-goods-head h3 { font-size: 20px; font-weight: 800; }
.category-goods-count { font-size: 13px; color: var(--ink-2); }

.products-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 24px;
}
.grid-skeleton { display: grid; grid-template-columns: repeat(4, 1fr); gap: 24px; }

/* 秒杀氛围条 */
.seckill-banner { padding-bottom: 64px; }
.seckill-card {
  display: flex; align-items: center; gap: 22px;
  padding: 26px 34px;
  border-radius: var(--radius-lg);
  background: linear-gradient(120deg, #F04E23, #FF6B35 55%, #FF9A5A);
  color: #fff;
  box-shadow: 0 16px 40px rgba(240, 78, 35, .28);
}
.seckill-icon {
  flex-shrink: 0;
  width: 60px; height: 60px; border-radius: 18px;
  display: grid; place-items: center;
  background: rgba(255, 255, 255, .2);
  -webkit-backdrop-filter: blur(6px);
  backdrop-filter: blur(6px);
  animation: float 4s ease-in-out infinite;
}
.seckill-icon .el-icon { font-size: 32px; }
.seckill-card h3 { font-size: 22px; font-weight: 800; }
.seckill-card p { font-size: 14px; opacity: .92; margin-top: 2px; }
.seckill-btn {
  margin-left: auto; flex-shrink: 0;
  padding: 12px 30px;
  border-radius: var(--radius-pill);
  background: #fff; color: var(--price);
  font-size: 15px; font-weight: 800;
  box-shadow: 0 10px 24px rgba(0, 0, 0, .18);
  transition: transform .4s var(--elastic);
}
.seckill-btn:hover { transform: scale(1.06) translateY(-2px); }
@keyframes float { 0%, 100% { transform: translateY(0); } 50% { transform: translateY(-8px); } }

@media (max-width: 768px) {
  .products-grid { grid-template-columns: repeat(2, 1fr); gap: 12px; }
  .grid-skeleton { grid-template-columns: repeat(2, 1fr); gap: 12px; }
  .seckill-card { flex-wrap: wrap; padding: 20px; }
  .seckill-btn { margin-left: 0; }
}

@media (max-width: 480px) {
  .products-grid, .grid-skeleton { gap: 10px; }
  .seckill-card { padding: 16px; gap: 14px; }
  .seckill-icon { width: 48px; height: 48px; border-radius: 14px; }
  .seckill-card h3 { font-size: 18px; }
  .seckill-btn { padding: 10px 22px; font-size: 14px; }
}

/* ===== 全屏 Hero 宣传图 ===== */
.hero-full {
  position: relative;
  width: 100%;
  height: calc(100vh - var(--nav-h));
  min-height: 480px;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
}
.hero-bg {
  position: absolute;
  inset: 0;
  background: url('/images/hero-banner.jpg') center/cover no-repeat;
  transform: scale(1.05);
  animation: heroZoom 20s ease-in-out infinite alternate;
}
@keyframes heroZoom {
  0% { transform: scale(1.05); }
  100% { transform: scale(1.15); }
}
.hero-overlay {
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, rgba(15,23,42,.75) 0%, rgba(47,107,255,.45) 50%, rgba(15,23,42,.6) 100%);
}
.hero-content {
  position: relative;
  z-index: 2;
  text-align: center;
  color: #fff;
  padding: 0 20px;
  animation: heroFadeUp 1s ease-out;
}
@keyframes heroFadeUp {
  from { opacity: 0; transform: translateY(30px); }
  to { opacity: 1; transform: translateY(0); }
}
.hero-title {
  font-size: clamp(40px, 7vw, 80px);
  font-weight: 900;
  letter-spacing: 4px;
  margin-bottom: 16px;
  text-shadow: 0 4px 20px rgba(0,0,0,.3);
  background: linear-gradient(135deg, #fff 0%, #c7d9ff 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}
.hero-subtitle {
  font-size: clamp(14px, 2vw, 20px);
  opacity: .9;
  margin-bottom: 36px;
  letter-spacing: 2px;
}
.hero-btns { display: flex; gap: 16px; justify-content: center; flex-wrap: wrap; }
.hero-btn {
  padding: 14px 36px;
  border-radius: 50px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: all .3s ease;
  border: none;
  letter-spacing: 1px;
}
.hero-btn.primary {
  background: linear-gradient(135deg, #2f6bff, #5a8dff);
  color: #fff;
  box-shadow: 0 8px 24px rgba(47,107,255,.4);
}
.hero-btn.primary:hover { transform: translateY(-2px); box-shadow: 0 12px 32px rgba(47,107,255,.5); }
.hero-btn.ghost {
  background: rgba(255,255,255,.15);
  color: #fff;
  border: 1.5px solid rgba(255,255,255,.4);
  backdrop-filter: blur(10px);
}
.hero-btn.ghost:hover { background: rgba(255,255,255,.25); transform: translateY(-2px); }

/* 向下滚动提示 */
.scroll-down {
  position: absolute;
  bottom: 30px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 2;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  color: #fff;
  background: none;
  border: none;
  cursor: pointer;
  animation: bounce 2s ease-in-out infinite;
}
@keyframes bounce {
  0%, 100% { transform: translateX(-50%) translateY(0); }
  50% { transform: translateX(-50%) translateY(8px); }
}
.scroll-text { font-size: 12px; opacity: .8; letter-spacing: 2px; }
.scroll-arrow { font-size: 20px; }

.home-main { position: relative; z-index: 1; }

@media (max-width: 768px) {
  .hero-full { height: calc(100vh - var(--nav-h) - 54px); min-height: 400px; }
  .hero-btns { flex-direction: column; align-items: center; }
  .hero-btn { width: 200px; }
}
</style>
