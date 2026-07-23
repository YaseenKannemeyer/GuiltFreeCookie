<script setup>
/**
 * BaseBadge — small pill label. Variants by tone.
 */
import { computed } from 'vue'

const props = defineProps({
  variant: {
    type: String,
    default: 'primary', // primary | chocolate | cream | success | warning | info
  },
  size: {
    type: String,
    default: 'md', // sm | md
  },
})

const variantClass = computed(() => {
  switch (props.variant) {
    case 'chocolate':
      return 'bg-chocolate text-cream'
    case 'cream':
      return 'bg-cream text-chocolate'
    case 'success':
      return 'bg-green-100 text-green-800'
    case 'warning':
      return 'bg-amber-100 text-amber-800'
    case 'info':
      return 'bg-sky-100 text-sky-800'
    case 'primary':
    default:
      return 'bg-primary/20 text-primary-700'
  }
})

const sizeClass = computed(() =>
  props.size === 'sm' ? 'text-xs px-2.5 py-1' : 'text-sm px-3 py-1.5',
)
</script>

<template>
  <span
    :class="[
      'inline-flex items-center gap-1 rounded-full font-medium tracking-wide',
      variantClass,
      sizeClass,
    ]"
  >
    <slot />
  </span>
</template>
