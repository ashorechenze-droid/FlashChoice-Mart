<template>
  <section class="hero" @mouseenter="paused = true" @mouseleave="paused = false">
    <div class="hero-track" :style="{ transform: 'translateX(-' + index * 100 + '%)' }">
      <div v-for="s in slides" :key="s.title" class="hero-slide" :style="{ background: s.bg }">
        <div class="hero-content">
          <span class="hero-tag">{{ s.tag }}</span>
          <h2 class="hero-title">{{ s.title }}</h2>
          <p class="hero-desc">{{ s.desc }}</p>
          <slot name="cta" :slide="s"></slot>
        </div>
        <div class="hero-deco" aria-hidden="true">
          <el-icon class="deco-main"><component :is="s.icon" /></el-icon>
          <span class="deco-orb o1"></span>
          <span class="deco-orb o2"></span>
        </div>
      </div>
    </div>

    <button class="hero-arrow left" aria-label="上一张" @click="prev"><el-icon><ArrowLeft /></el-icon></button>
    <button class="hero-arrow right" aria-label="下一张" @click="next"><el-icon><ArrowRight /></el-icon></button>

    <div class="hero-dots" role="tablist" aria-label="轮播指示器">
      <button v-for="(s, i) in slides" :key="'d' + i" :class="{ active: i === index }"
              :aria-label="'第 ' + (i + 1) + ' 张'" @click="index = i"></button>
    </div>
  </section>
</template>

<script setup>
import { ref, watch, onMounted, onBeforeUnmount } from 'vue'

// Hero 卡片轮播：自动播放（4.5s）+ 悬停暂停 + 箭头/圆点
const props = defineProps({
  slides: { type: Array, required: true },
})

const index = ref(0)
const paused = ref(false)
let timer = null

function next() { index.value = (index.value + 1) % props.slides.length }
function prev() { index.value = (index.value - 1 + props.slides.length) % props.slides.length }
function start() { stop(); timer = setInterval(next, 4500) }
function stop() { if (timer) { clearInterval(timer); timer = null } }

watch(paused, (p) => (p ? stop() : start()))
onMounted(start)
onBeforeUnmount(stop)
</script>

<style scoped>
.hero {
  position: relative;
  height: 430px;
  border-radius: var(--radius-lg);
  overflow: hidden;
  box-shadow: var(--shadow-md);
  margin-top: 12px;
}
.hero-track { display: flex; height: 100%; transition: transform .75s var(--smooth); }
.hero-slide {
  flex: 0 0 100%;
  display: flex; align-items: center;
  padding: 0 6%;
  position: relative;
  overflow: hidden;
  color: #fff;
}
.hero-content { position: relative; z-index: 2; max-width: 58%; }
.hero-tag {
  display: inline-block;
  font-size: 13px; font-weight: 600; letter-spacing: 2px;
  background: rgba(255, 255, 255, .18);
  border: 1px solid rgba(255, 255, 255, .35);
  border-radius: var(--radius-pill);
  padding: 5px 16px;
  margin-bottom: 18px;
  -webkit-backdrop-filter: blur(6px);
  backdrop-filter: blur(6px);
}
.hero-title { font-size: clamp(30px, 4vw, 52px); font-weight: 800; letter-spacing: 1px; line-height: 1.2; margin-bottom: 14px; }
.hero-desc { font-size: clamp(14px, 1.4vw, 17px); opacity: .92; margin-bottom: 28px; }

.hero-deco { position: absolute; right: 6%; top: 50%; transform: translateY(-50%); z-index: 1; }
.deco-main {
  font-size: 150px; color: rgba(255, 255, 255, .95);
  filter: drop-shadow(0 18px 30px rgba(0, 0, 0, .22));
  animation: decoFloat 5s ease-in-out infinite;
}
.deco-orb { position: absolute; border-radius: 50%; background: rgba(255, 255, 255, .22); }
.deco-orb.o1 { width: 70px; height: 70px; top: -38px; right: -20px; animation: decoFloat 6.5s ease-in-out infinite reverse; }
.deco-orb.o2 { width: 40px; height: 40px; bottom: -16px; left: -46px; animation: decoFloat 5.5s ease-in-out infinite .4s; }
@keyframes decoFloat {
  0%, 100% { transform: translateY(0) rotate(0deg); }
  50% { transform: translateY(-16px) rotate(6deg); }
}

.hero-arrow {
  position: absolute; top: 50%; transform: translateY(-50%);
  z-index: 3;
  width: 44px; height: 44px; border-radius: 50%;
  display: grid; place-items: center;
  background: rgba(255, 255, 255, .85);
  color: var(--ink);
  box-shadow: var(--shadow-sm);
  opacity: 0; pointer-events: none;
  transition: opacity .35s ease, transform .4s var(--elastic);
}
.hero:hover .hero-arrow { opacity: 1; pointer-events: auto; }
.hero-arrow.left { left: 18px; }
.hero-arrow.right { right: 18px; }
.hero-arrow:hover { transform: translateY(-50%) scale(1.12); color: var(--brand); }
.hero-dots {
  position: absolute; bottom: 18px; left: 0; right: 0; z-index: 3;
  display: flex; justify-content: center; gap: 8px;
}
.hero-dots button {
  width: 8px; height: 8px; border-radius: var(--radius-pill);
  background: rgba(255, 255, 255, .5);
  transition: width .35s var(--smooth), background .3s ease;
}
.hero-dots button.active { width: 26px; background: #fff; }

@media (max-width: 768px) {
  .hero { height: 340px; border-radius: 18px; }
  .hero-slide { padding: 0 7%; }
  .hero-content { max-width: 88%; }
  .hero-deco { opacity: .5; right: -30px; }
  .deco-main { font-size: 96px; }
  .hero-arrow { display: none; }
  .hero-dots button { width: 6px; height: 6px; }
  .hero-dots button.active { width: 18px; }
}
</style>
