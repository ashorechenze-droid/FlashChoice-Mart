<template>
  <div class="category-menu" @mouseleave="activeId = null">
    <!-- 左侧一级分类列表 -->
    <ul class="menu-list">
      <li v-for="c in categories" :key="c.id"
          class="menu-item"
          :class="{ active: activeId === c.id }"
          @mouseenter="activeId = c.id"
          @click="onSelect(c)">
        <span class="menu-icon">
          <img v-if="isImg(c.icon)" :src="c.icon" :alt="c.name" class="icon-img" />
          <el-icon v-else><component :is="iconMap[c.icon] || ShoppingBag" /></el-icon>
        </span>
        <span class="menu-name">{{ c.name }}</span>
        <span class="menu-arrow"><el-icon><ArrowRight /></el-icon></span>
      </li>
    </ul>

    <!-- hover 弹出的二级/三级子分类面板（京东风格） -->
    <transition name="fade">
      <div v-if="activeCategory" class="sub-panel">
        <div class="sub-panel-head">
          <span class="sub-title">{{ activeCategory.name }}</span>
          <span class="sub-summary">{{ activeCategory.summary || '点击查看全部' }}</span>
          <span class="sub-all" @click="onSelect(activeCategory)">全部 ></span>
        </div>
        <div v-if="(activeCategory.children || []).length" class="sub-body">
          <!-- 每个二级分类一行：标题 + 三级标签 -->
          <div v-for="sub in activeCategory.children" :key="sub.id" class="sub-row">
            <div class="sub-row-title" @click="onSelect(sub)">
              <img v-if="isImg(sub.icon)" :src="sub.icon" :alt="sub.name" class="row-icon-img" />
              <el-icon v-else class="row-icon"><component :is="iconMap[sub.icon] || ShoppingBag" /></el-icon>
              <span>{{ sub.name }}</span>
            </div>
            <div class="sub-tags">
              <span v-for="leaf in sub.children || []" :key="leaf.id"
                    class="sub-tag" @click="onSelect(leaf)">
                <img v-if="isImg(leaf.icon)" :src="leaf.icon" :alt="leaf.name" class="tag-icon-img" />
                {{ leaf.name }}
              </span>
              <span v-if="!(sub.children || []).length" class="sub-tag plain" @click="onSelect(sub)">
                全部{{ sub.name }}
              </span>
            </div>
          </div>
        </div>
        <div v-else class="sub-empty">该分类暂无子分类</div>
      </div>
    </transition>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import {
  ArrowRight, Headset, Watch, Basketball,
  House, Sunny, CoffeeCup, Timer, Present,
  Lightning, ShoppingBag,
} from '@element-plus/icons-vue'

const props = defineProps({
  categories: { type: Array, required: true },
})
const emit = defineEmits(['select'])

const router = useRouter()
const activeId = ref(null)

// 图标名 → 组件映射（兼容旧 icon 名字符串；URL 走 <img> 渲染）
const iconMap = {
  Headset, Watch, Basketball, House,
  Sunny, CoffeeCup, Timer, Present, Lightning,
  ShoppingBag,
}

// icon 为 http(s) 链接时渲染图片，否则渲染 Element 图标
function isImg(icon) {
  return typeof icon === 'string' && /^https?:\/\//i.test(icon)
}

const activeCategory = computed(() =>
  props.categories.find((c) => c.id === activeId.value)
)

function onSelect(c) {
  emit('select', c)
  router.push({ path: '/goods', query: { categoryId: c.id } })
}
</script>

<style scoped>
.category-menu {
  position: relative;
  width: 220px;
  background: rgba(255, 255, 255, .95);
  border-radius: 12px;
  box-shadow: 0 8px 30px rgba(0, 0, 0, .12);
  overflow: visible;
  z-index: 10;
}

.menu-list {
  list-style: none;
  margin: 0;
  padding: 8px 0;
}

.menu-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 18px;
  cursor: pointer;
  transition: background .2s, color .2s;
  position: relative;
}

.menu-item:hover,
.menu-item.active {
  background: linear-gradient(90deg, rgba(47, 107, 255, .08), transparent);
  color: var(--brand);
}

.menu-item.active::before {
  content: '';
  position: absolute;
  left: 0;
  top: 20%;
  bottom: 20%;
  width: 3px;
  background: var(--brand);
  border-radius: 0 2px 2px 0;
}

.menu-icon {
  width: 24px;
  height: 24px;
  display: grid;
  place-items: center;
  font-size: 18px;
  color: var(--ink-2);
  flex-shrink: 0;
}

.menu-item.active .menu-icon {
  color: var(--brand);
}

.icon-img {
  width: 24px;
  height: 24px;
  border-radius: 6px;
  object-fit: cover;
  display: block;
}

.menu-name {
  flex: 1;
  font-size: 14px;
  font-weight: 600;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.menu-arrow {
  font-size: 12px;
  color: var(--ink-2);
  opacity: .6;
}

/* hover 弹出面板（京东风格：二级分类行 + 三级标签） */
.sub-panel {
  position: absolute;
  left: 100%;
  top: 0;
  width: 520px;
  min-height: 100%;
  background: #fff;
  border-radius: 0 12px 12px 0;
  box-shadow: 8px 8px 30px rgba(0, 0, 0, .1);
  padding: 20px 24px;
  border-left: 1px solid var(--line);
}

.sub-panel-head {
  display: flex;
  align-items: baseline;
  gap: 12px;
  margin-bottom: 14px;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--line);
}

.sub-title {
  font-size: 18px;
  font-weight: 800;
  color: var(--ink);
}

.sub-summary {
  font-size: 12px;
  color: var(--ink-2);
}

.sub-all {
  margin-left: auto;
  font-size: 12px;
  color: var(--brand);
  cursor: pointer;
  font-weight: 600;
}

.sub-all:hover { text-decoration: underline; }

.sub-body {
  display: flex;
  flex-direction: column;
  gap: 14px;
  max-height: 480px;
  overflow-y: auto;
}

.sub-row {
  display: flex;
  gap: 14px;
  padding-bottom: 12px;
  border-bottom: 1px dashed var(--line);
}

.sub-row:last-child { border-bottom: none; }

.sub-row-title {
  flex-shrink: 0;
  width: 108px;
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  font-weight: 700;
  color: var(--ink);
  cursor: pointer;
}

.sub-row-title:hover { color: var(--brand); }

.row-icon { font-size: 15px; color: var(--brand); }
.row-icon-img {
  width: 20px;
  height: 20px;
  border-radius: 4px;
  object-fit: cover;
}

.sub-tags {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px 12px;
}

.sub-tag {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12.5px;
  color: var(--ink-2);
  cursor: pointer;
  transition: color .15s;
  white-space: nowrap;
}

.sub-tag:hover { color: var(--brand); }

.sub-tag.plain { color: #999; }
.sub-tag.plain:hover { color: var(--brand); }

.tag-icon-img {
  width: 18px;
  height: 18px;
  border-radius: 4px;
  object-fit: cover;
}

.sub-empty {
  text-align: center;
  padding: 30px 0;
  color: var(--ink-2);
  font-size: 13px;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity .18s ease, transform .18s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
  transform: translateX(-8px);
}

@media (max-width: 1024px) {
  .category-menu { width: 180px; }
  .sub-panel { width: 400px; }
}

@media (max-width: 768px) {
  .category-menu { display: none; }
}
</style>
