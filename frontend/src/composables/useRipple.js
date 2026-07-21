/**
 * useRipple — Material-style ripple effect for buttons.
 * Apply v-ripple to any element; on click, an animated circle emanates from the cursor.
 */
export function useRipple() {
  const vRipple = {
    mounted(el) {
      el.classList.add('ripple-container')
      el.addEventListener('click', (e) => {
        const rect = el.getBoundingClientRect()
        const size = Math.max(rect.width, rect.height)
        const x = e.clientX - rect.left - size / 2
        const y = e.clientY - rect.top - size / 2

        const ripple = document.createElement('span')
        ripple.className = 'ripple-effect'
        ripple.style.width = ripple.style.height = `${size}px`
        ripple.style.left = `${x}px`
        ripple.style.top = `${y}px`
        // Use current text colour for ripple tint
        ripple.style.background = 'rgba(255, 255, 255, 0.45)'

        el.appendChild(ripple)
        setTimeout(() => ripple.remove(), 600)
      })
    },
  }
  return { vRipple }
}
