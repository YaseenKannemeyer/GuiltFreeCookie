<script setup>
/**
 * LoadingSpinner — animated cookie-themed spinner.
 *
 * Usage:
 *   <LoadingSpinner />                       (default)
 *   <LoadingSpinner size="lg" label="Baking..." />
 */
import { computed } from 'vue'

const props = defineProps({
  size: { type: String, default: 'md' }, // sm | md | lg
  label: { type: String, default: '' },
  fullscreen: { type: Boolean, default: false },
})

const sizeClass = computed(() => {
  switch (props.size) {
    case 'sm':
      return 'h-6 w-6'
    case 'lg':
      return 'h-16 w-16'
    case 'md':
    default:
      return 'h-10 w-10'
  }
})
</script>

<template>
  <div
    v-if="fullscreen"
    class="fixed inset-0 z-[100] flex flex-col items-center justify-center bg-background/80 backdrop-blur-sm"
  >
    <div
      :class="sizeClass"
      class="rounded-full border-4 border-cream-300 border-t-primary animate-spin"
    />
    <p v-if="label" class="mt-4 text-chocolate/70 text-card animate-pulse">{{ label }}</p>
  </div>
  <div v-else class="inline-flex flex-col items-center justify-center gap-3">
    <div
      :class="sizeClass"
      class="rounded-full border-4 border-cream-300 border-t-primary animate-spin"
    />
    <p v-if="label" class="text-chocolate/60 text-sm animate-pulse">{{ label }}</p>
  </div>
</template>
