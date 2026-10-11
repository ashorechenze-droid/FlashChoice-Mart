<template>
  <section class="typing-strip">
    <div class="container typing-inner">
      <span class="typing-eyebrow">极速优选 · 只为好物</span>
      <div class="typing-line" aria-live="polite">
        <span class="typed">{{ typedText }}</span><span class="cursor" aria-hidden="true"></span>
      </div>
    </div>
  </section>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'

// 打字机标语：80ms/字，停顿 3s 后切换下一条，循环
const props = defineProps({
  slogans: { type: Array, default: () => ['质感生活，即刻拥有', '每一件都值得细品', '冷静克制，高级之选', '好物不将就'] },
})

const typedText = ref('')
let typeTimer = null
let pauseTimer = null
let sloganIdx = 0
let charPos = 0

function typeStep() {
  const cur = props.slogans[sloganIdx]
  if (charPos < cur.length) {
    charPos += 1
    typedText.value = cur.slice(0, charPos)
    typeTimer = setTimeout(typeStep, 80)
  } else {
    pauseTimer = setTimeout(() => {
      charPos = 0
      sloganIdx = (sloganIdx + 1) % props.slogans.length
      typeStep()
    }, 3000)
  }
}

onMounted(typeStep)
onBeforeUnmount(() => {
  clearTimeout(typeTimer)
  clearTimeout(pauseTimer)
})
</script>

<style scoped>
.typing-strip { padding: 36px 0 10px; }
.typing-inner { text-align: center; }
.typing-eyebrow {
  display: inline-block;
  font-size: 13px; font-weight: 600; letter-spacing: 4px;
  color: var(--brand-deep);
  background: var(--brand-soft);
  border-radius: var(--radius-pill);
  padding: 5px 16px;
  margin-bottom: 14px;
}
.typing-line {
  font-size: clamp(26px, 3.4vw, 40px);
  font-weight: 700;
  min-height: 1.3em;
  display: flex; align-items: baseline; justify-content: center;
  gap: 2px;
}
.typed { white-space: pre; }
.cursor {
  display: inline-block;
  width: 3px; height: 0.95em;
  background: var(--brand);
  border-radius: 2px;
  animation: cursorBlink 1.1s steps(2, start) infinite;
}
@keyframes cursorBlink { 0%, 100% { opacity: 1; } 50% { opacity: 0; } }

@media (max-width: 768px) {
  .typing-strip { padding: 26px 0 8px; }
}
</style>
