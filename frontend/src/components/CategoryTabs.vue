<template>
  <div class="tabs-scroll no-scrollbar">
    <div class="tabs">
      <button v-for="c in categories" :key="c.id" class="tab-pill"
              :class="{ active: c.id === modelValue }"
              @click="$emit('update:modelValue', c.id); $emit('select', c)">
        <img v-if="isImg(c.icon)" :src="c.icon" :alt="c.name" class="tab-icon-img" />
        <el-icon v-else-if="c.icon" class="tab-icon"><component :is="c.icon" /></el-icon>
        {{ c.name }}
      </button>
    </div>
  </div>
</template>

<script setup>
// 分类 Tab：大胶囊横向滚动，点击向上派发 select（页面层决定跳转 / 筛选）
defineProps({
  categories: { type: Array, required: true },
  modelValue: { type: Number, default: 0 },
})
defineEmits(['update:modelValue', 'select'])

// icon 为 http(s) 链接时渲染图片，否则按图标名渲染
function isImg(icon) {
  return typeof icon === 'string' && /^https?:\/\//i.test(icon)
}
</script>

<style scoped>
.tabs-scroll { overflow-x: auto; padding-bottom: 8px; }
.tabs { display: flex; gap: 12px; width: max-content; min-width: 100%; }
.tab-pill {
  min-height: 46px;
  padding: 0 26px;
  border-radius: var(--radius-pill);
  background: var(--surface);
  border: 1px solid var(--line);
  font-size: 15px; font-weight: 600; color: var(--ink-2);
  display: inline-flex; align-items: center; gap: 8px;
  white-space: nowrap;
  transition: color .3s ease, background .3s ease, transform .35s var(--elastic), box-shadow .3s ease;
}
.tab-pill:hover { transform: translateY(-2px); color: var(--brand); border-color: rgba(47, 107, 255, .4); }
.tab-pill.active {
  color: #fff;
  background: linear-gradient(135deg, var(--brand-deep), var(--brand));
  border-color: transparent;
  box-shadow: 0 8px 20px rgba(47, 107, 255, .35);
}
.tab-icon { font-size: 17px; }
.tab-icon-img {
  width: 22px;
  height: 22px;
  border-radius: 6px;
  object-fit: cover;
  flex-shrink: 0;
}

@media (max-width: 768px) {
  .tab-pill { min-height: 44px; padding: 0 18px; font-size: 14px; }
}
</style>
