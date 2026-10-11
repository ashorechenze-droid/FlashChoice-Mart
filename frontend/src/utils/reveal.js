// 滚动入场：为页面中的 .reveal 元素注册 IntersectionObserver（淡入上浮）
// 兼容性兜底：部分嵌入 WebView（如豆包内置浏览器）不回调 IntersectionObserver，
// 此时直接全部显示，避免内容被 .reveal 的 opacity:0 隐藏导致页面空白。
export function initReveal() {
  const els = document.querySelectorAll('.reveal:not(.in-view)')
  if (!els.length) return () => {}

  // 环境不支持 IntersectionObserver：全部直接显示
  if (typeof IntersectionObserver === 'undefined') {
    els.forEach((el) => el.classList.add('in-view'))
    return () => {}
  }

  let ioFired = false
  let io = null
  try {
    io = new IntersectionObserver(
      (entries) => {
        ioFired = true
        entries.forEach((en) => {
          if (en.isIntersecting) {
            en.target.classList.add('in-view')
            io.unobserve(en.target)
          }
        })
      },
      { threshold: 0.12, rootMargin: '0px 0px -40px 0px' },
    )
  } catch (e) {
    io = null
  }

  if (!io) {
    els.forEach((el) => el.classList.add('in-view'))
    return () => {}
  }

  els.forEach((el) => io.observe(el))
  // 兜底：800ms 内 IO 无任何回调（环境不驱动），直接全部显示
  const timer = setTimeout(() => {
    if (!ioFired) {
      els.forEach((el) => el.classList.add('in-view'))
      io.disconnect()
    }
  }, 800)
  return () => {
    clearTimeout(timer)
    io.disconnect()
  }
}
