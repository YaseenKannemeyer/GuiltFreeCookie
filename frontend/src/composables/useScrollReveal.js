/**
 * useScrollReveal — IntersectionObserver-based "reveal on scroll" directive.
 *
 * Usage in a component:
 *   const { vReveal } = useScrollReveal()
 *   <div v-reveal class="reveal">...</div>
 *
 * Elements with the `.reveal` class start hidden (opacity:0, translateY:24px)
 * and animate in once they intersect the viewport.
 *
 * Pass a numeric delay via :data-reveal-delay="200" (ms) for staggered entrances.
 */
import { onMounted, onUnmounted } from 'vue'

export function useScrollReveal() {
  let observer = null

  const observe = (el) => {
    if (!el) return
    const delay = Number(el.dataset.revealDelay || 0)
    if (delay) {
      el.style.transitionDelay = `${delay}ms`
    }
    observer?.observe(el)
  }

  const vReveal = {
    mounted(el) {
      el.classList.add('reveal')
      observe(el)
    },
    unmounted(el) {
      observer?.unobserve(el)
    },
  }

  onMounted(() => {
    if (typeof IntersectionObserver === 'undefined') {
      // SSR / very old browser fallback — just reveal everything
      document.querySelectorAll('.reveal').forEach((el) => el.classList.add('is-visible'))
      return
    }
    observer = new IntersectionObserver(
      (entries) => {
        entries.forEach((entry) => {
          if (entry.isIntersecting) {
            entry.target.classList.add('is-visible')
            observer?.unobserve(entry.target)
          }
        })
      },
      { threshold: 0.12, rootMargin: '0px 0px -60px 0px' },
    )
    // Pick up any pre-existing .reveal elements rendered before this composable mounted
    document.querySelectorAll('.reveal').forEach((el) => observe(el))
  })

  onUnmounted(() => {
    observer?.disconnect()
    observer = null
  })

  return { vReveal }
}
