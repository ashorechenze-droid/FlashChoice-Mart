<template>
  <div class="goods-list">
    <div class="container">
      <!-- 页面标题 -->
      <div class="list-head reveal">
        <h2>{{ pageTitle }}</h2>
        <p v-if="route.query.keyword">关键词：「{{ route.query.keyword }}」的搜索结果</p>
      </div>

      <!-- 筛选区：分类 / 品牌 / 排序（搜索结果页隐藏筛选，直接展示商品） -->
      <div v-if="!route.query.keyword" class="filters reveal">
        <div class="filter-row">
          <span class="filter-label">分类</span>
          <div class="chips no-scrollbar">
            <!-- 已选中某一级分类时：显示父级分类名，点击回到父级筛选（支持逐级回退） -->
            <button v-if="activeRootCategory" class="chip root-crumb"
                    @click="onCrumbClick">
              {{ activeRootCategory.name }}
              <span class="crumb-close">×</span>
            </button>
            <!-- 全部：未选分类时=查全部；选中大类时=查该大类全部 -->
            <button class="chip"
                    :class="{ active: isAllActive }"
                    @click="setQuery('categoryId', activeRootId || null)">全部</button>
            <button v-for="c in filterCategories" :key="c.id" class="chip"
                    :class="{ active: Number(query.categoryId) === c.id }"
                    @click="setQuery('categoryId', c.id)">{{ c.name }}</button>
          </div>
        </div>
        <div class="filter-row">
          <span class="filter-label">品牌</span>
          <div class="chips no-scrollbar">
            <button class="chip" :class="{ active: !query.brandId }" @click="setQuery('brandId', null)">全部品牌</button>
            <button v-for="b in brands" :key="b.id" class="chip"
                    :class="{ active: Number(query.brandId) === b.id }"
                    @click="setQuery('brandId', b.id)">{{ b.name }}</button>
          </div>
        </div>
        <div class="filter-row">
          <span class="filter-label">排序</span>
          <div class="chips no-scrollbar">
            <button v-for="s in sortOptions" :key="s.value" class="chip"
                    :class="{ active: (query.sort || '') === s.value }"
                    @click="setQuery('sort', s.value)">{{ s.label }}</button>
          </div>
        </div>
      </div>

      <!-- 商品网格（筛选变化时翻转动画） -->
      <div class="grid-wrap">
        <div class="products-grid" :class="{ flipping }" @animationend="onGridAnimEnd">
          <div v-for="(g, i) in list" :key="g.id" class="card-reveal reveal"
               :style="{ transitionDelay: (i % 4) * 0.05 + 's' }">
            <ProductCard :good="g" :brand-name="getBrandName(g.brandId)" />
          </div>
        </div>

        <div v-if="loading" class="state-box"><el-skeleton animated :rows="3" /></div>
        <el-empty v-else-if="!list.length" description="没有找到相关商品，换个条件试试" :image-size="130" />
      </div>

      <!-- 分页 -->
      <div v-if="total > pageSize" class="pager">
        <el-pagination background layout="prev, pager, next" :total="total" :page-size="pageSize"
                       :current-page="query.pageNo" @current-change="onPage" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, watch, nextTick, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import ProductCard from '../components/ProductCard.vue'
import { listGoods } from '../api/goods'
import { getCategories } from '../api/category'
import { getBrands } from '../api/brand'
import { initReveal } from '../utils/reveal'

const route = useRoute()
const router = useRouter()

const categories = ref([])
const brands = ref([])
const list = ref([])
const total = ref(0)
const loading = ref(false)
const flipping = ref(false)
const pageSize = 12

// 品牌ID -> 名称映射，用于商品卡片展示
const brandMap = computed(() => {
  const m = {}
  brands.value.forEach((b) => { m[b.id] = b.name })
  return m
})
function getBrandName(brandId) {
  return brandMap.value[brandId] || ''
}

const sortOptions = [
  { label: '综合', value: '' },
  { label: '销量', value: 'hot' },
  { label: '价格升序', value: 'priceAsc' },
  { label: '价格降序', value: 'priceDesc' },
]

// 查询条件：与 URL query 双向同步，便于分享/刷新保留
const query = reactive({
  keyword: '',
  categoryId: null,
  brandId: null,
  sort: '',
  pageNo: 1,
})

// 当前选中的分类对象
const activeCategory = computed(() =>
  categories.value.find((c) => c.id === Number(query.categoryId))
)

// 当前选中分类所属的一级分类ID（选中一级时是它自己，选中二级时是它的父级）
const activeRootId = computed(() => {
  const c = activeCategory.value
  if (!c) return null
  return c.parentId === 0 || c.parentId == null ? c.id : c.parentId
})

// 当前一级分类对象（用于面包屑展示，点击可回到一级分类列表）
const activeRootCategory = computed(() =>
  categories.value.find((c) => c.id === activeRootId.value)
)

// 分类筛选选项：
//   未选分类 → 显示所有一级分类
//   选中某大类 → 显示该大类下的二级子分类
const filterCategories = computed(() => {
  if (!activeRootId.value) {
    return categories.value.filter((c) => c.parentId === 0 || c.parentId == null)
  }
  return categories.value.filter((c) => c.parentId === activeRootId.value)
})

// "全部"按钮的激活状态：未选分类，或选中的正是当前一级分类（查大类全部）
const isAllActive = computed(() => {
  if (!query.categoryId) return true
  if (activeRootId.value && Number(query.categoryId) === activeRootId.value) return true
  return false
})

const pageTitle = computed(() => {
  const c = categories.value.find((x) => x.id === Number(query.categoryId))
  if (c) return c.name
  const b = brands.value.find((x) => x.id === Number(query.brandId))
  if (b) return `${b.name}品牌`
  return '全部商品'
})

function setQuery(key, val) {
  query[key] = val
  if (key !== 'pageNo') query.pageNo = 1
  syncUrl()
}

// 面包屑点击：选中一级分类时清除分类回全部商品；选中子分类时回一级分类
function onCrumbClick() {
  const c = activeCategory.value
  if (c && (c.parentId === 0 || c.parentId == null)) {
    // 当前选中的就是一级分类，点击面包屑清除分类（回到全部商品）
    setQuery('categoryId', null)
  } else {
    // 当前选中的是子分类，点击面包屑回到一级分类
    setQuery('categoryId', activeRootId.value)
  }
}

function syncUrl() {
  const q = {}
  if (query.keyword) q.keyword = query.keyword
  if (query.categoryId) q.categoryId = query.categoryId
  if (query.brandId) q.brandId = query.brandId
  if (query.sort) q.sort = query.sort
  if (query.pageNo > 1) q.pageNo = query.pageNo
  router.replace({ path: '/goods', query: q })
}

async function load() {
  loading.value = true
  const data = await listGoods({ ...query, pageSize })
  list.value = data.list
  total.value = data.total
  loading.value = false
  // 网格翻转动画（rotateX + scale，纯视觉过渡）
  flipping.value = false
  await nextTick()
  flipping.value = true
  requestAnimationFrame(() => initReveal())
}

function onPage(page) {
  query.pageNo = page
  syncUrl()
}

// 仅响应网格自身动画结束（避免子元素动画冒泡误复位）
function onGridAnimEnd(e) {
  if (e.target === e.currentTarget) flipping.value = false
}

watch(
  () => route.query,
  (q) => {
    query.keyword = q.keyword || ''
    query.categoryId = q.categoryId ? Number(q.categoryId) : null
    query.brandId = q.brandId ? Number(q.brandId) : null
    query.sort = q.sort || ''
    query.pageNo = Number(q.pageNo) || 1
    load()
  },
  { immediate: true },
)

onMounted(async () => {
  ;[categories.value, brands.value] = await Promise.all([getCategories(), getBrands()])
})
</script>

<style scoped>
.goods-list { padding: 36px 0 64px; }
.list-head { text-align: center; margin-bottom: 26px; }
.list-head h2 { font-size: 30px; font-weight: 800; letter-spacing: 1px; }
.list-head p { margin-top: 6px; font-size: 14px; color: var(--ink-2); }

.filters {
  display: flex; flex-direction: column; gap: 12px;
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: var(--radius-lg);
  padding: 18px 22px;
  margin-bottom: 26px;
  box-shadow: var(--shadow-sm);
}
.filter-row { display: flex; align-items: flex-start; gap: 16px; }
.filter-label {
  flex-shrink: 0;
  font-size: 13px; font-weight: 700; color: var(--ink-2);
  padding-top: 8px; width: 40px;
}
.chips { display: flex; gap: 10px; overflow-x: auto; flex-wrap: wrap; }
.chip {
  min-height: 36px;
  padding: 0 18px;
  border-radius: var(--radius-pill);
  background: var(--bg);
  font-size: 13.5px; font-weight: 600; color: var(--ink-2);
  border: 1px solid transparent;
  transition: color .3s ease, background .3s ease, transform .3s var(--elastic);
}
.chip:hover { color: var(--brand); }
.chip.root-crumb {
  background: rgba(47, 107, 255, .08);
  color: var(--brand);
  border-color: rgba(47, 107, 255, .25);
  font-weight: 700;
}
.chip.root-crumb:hover { background: rgba(47, 107, 255, .14); }
.crumb-close { margin-left: 4px; font-size: 15px; opacity: .7; }
.chip.active {
  color: #fff;
  background: linear-gradient(135deg, var(--brand-deep), var(--brand));
  box-shadow: 0 6px 14px rgba(47, 107, 255, .3);
}

.grid-wrap { perspective: 1400px; }
.products-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 24px;
}
.products-grid.flipping { animation: gridFlip .6s var(--elastic); transform-origin: center top; }
@keyframes gridFlip {
  0% { transform: rotateX(0) scale(1); opacity: 1; }
  35% { transform: rotateX(16deg) scale(.95); opacity: .55; }
  100% { transform: rotateX(0) scale(1); opacity: 1; }
}

.state-box { padding: 20px 0; }
.pager { display: flex; justify-content: center; margin-top: 36px; }

@media (max-width: 768px) {
  .goods-list { padding: 24px 0 48px; }
  .list-head h2 { font-size: 24px; }
  .products-grid { grid-template-columns: repeat(2, 1fr); gap: 12px; }
  .filter-label { display: none; }
  .filters { padding: 14px; }
}
</style>
