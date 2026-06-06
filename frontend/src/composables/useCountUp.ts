// composables/useCountUp.ts — 数字滚动动画
import { ref, watch, onBeforeUnmount } from 'vue'

export function useCountUp(target: () => number, duration = 800) {
  const displayValue = ref(0)
  let rafId: number | null = null

  function animate() {
    const end = target()
    const start = displayValue.value
    const startTime = performance.now()

    function step(now: number) {
      const elapsed = now - startTime
      const progress = Math.min(elapsed / duration, 1)
      // ease-out cubic
      const eased = 1 - Math.pow(1 - progress, 3)
      displayValue.value = start + (end - start) * eased
      if (progress < 1) {
        rafId = requestAnimationFrame(step)
      }
    }

    rafId = requestAnimationFrame(step)
  }

  watch(
    () => target(),
    () => animate(),
    { immediate: true }
  )

  onBeforeUnmount(() => {
    if (rafId != null) cancelAnimationFrame(rafId)
  })

  return { displayValue }
}
