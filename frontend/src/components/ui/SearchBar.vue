<script setup>
/**
 * SearchBar — controlled search input with optional icon-only mode and clear button.
 */
import { computed } from 'vue'
import { MagnifyingGlassIcon, XCircleIcon } from '@heroicons/vue/24/solid'

const props = defineProps({
  modelValue: { type: String, default: '' },
  placeholder: { type: String, default: 'Search cookies...' },
  size: { type: String, default: 'md' }, // sm | md | lg
  variant: { type: String, default: 'light' }, // light | dark
})

const emit = defineEmits(['update:modelValue', 'submit'])

const sizeClass = computed(() => {
  switch (props.size) {
    case 'sm':
      return 'h-11 text-sm'
    case 'lg':
      return 'h-16 text-lg'
    case 'md':
    default:
      return 'h-14 text-base'
  }
})

const isDark = computed(() => props.variant === 'dark')

const onSubmit = () => emit('submit', props.modelValue)
const clear = () => emit('update:modelValue', '')
</script>

<template>
  <div class="relative w-full">
    <span
      class="absolute left-5 top-1/2 -translate-y-1/2 pointer-events-none"
      :class="isDark ? 'text-cream/40' : 'text-chocolate/40'"
    >
      <MagnifyingGlassIcon class="h-5 w-5" />
    </span>
    <input
      :value="modelValue"
      :placeholder="placeholder"
      :class="[
        'w-full rounded-full pl-14 pr-12 transition-all duration-300 focus:outline-none focus:ring-2',
        sizeClass,
        isDark
          ? 'bg-chocolate-400/60 border border-cream/15 text-cream placeholder:text-cream/40 focus:ring-primary/50'
          : 'bg-surface border border-cream-400/60 text-chocolate placeholder:text-chocolate/40 focus:ring-primary/40 shadow-soft',
      ]"
      @input="$emit('update:modelValue', $event.target.value)"
      @keyup.enter="onSubmit"
    />
    <button
      v-if="modelValue"
      type="button"
      :class="[
        'absolute right-5 top-1/2 -translate-y-1/2 transition-colors',
        isDark ? 'text-cream/60 hover:text-cream' : 'text-chocolate/40 hover:text-chocolate',
      ]"
      aria-label="Clear search"
      @click="clear"
    >
      <XCircleIcon class="h-5 w-5" />
    </button>
  </div>
</template>
