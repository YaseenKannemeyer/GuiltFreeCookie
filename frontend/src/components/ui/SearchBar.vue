<script setup>
/**
 * SearchBar — controlled search input with a clear button.
 * Exposes focus() so pages can focus it (e.g. from the navbar search icon).
 */
import { computed, ref } from 'vue'
import { MagnifyingGlassIcon, XMarkIcon } from '@heroicons/vue/20/solid'

const props = defineProps({
  modelValue: { type: String, default: '' },
  placeholder: { type: String, default: 'Search cookies' },
  label: { type: String, default: 'Search' },
  size: { type: String, default: 'md' }, // sm | md | lg
  variant: { type: String, default: 'light' }, // light | dark
})

const emit = defineEmits(['update:modelValue', 'submit'])
const input = ref(null)

const sizeClass = computed(() => {
  switch (props.size) {
    case 'sm':
      return 'h-11 text-base'
    case 'lg':
      return 'h-14 text-base'
    case 'md':
    default:
      return 'h-12 text-base'
  }
})

const isDark = computed(() => props.variant === 'dark')

const onSubmit = () => emit('submit', props.modelValue)
const clear = () => {
  emit('update:modelValue', '')
  input.value?.focus()
}

defineExpose({ focus: () => input.value?.focus() })
</script>

<template>
  <div class="relative w-full">
    <MagnifyingGlassIcon
      class="absolute left-4 top-1/2 -translate-y-1/2 h-5 w-5 pointer-events-none"
      :class="isDark ? 'text-cream/60' : 'text-chocolate/55'"
    />
    <input
      ref="input"
      type="search"
      :value="modelValue"
      :placeholder="placeholder"
      :aria-label="label"
      :class="[
        'w-full rounded-full pl-11 pr-11 transition-colors duration-200 focus:outline-none focus:ring-2 [&::-webkit-search-cancel-button]:hidden',
        sizeClass,
        isDark
          ? 'bg-chocolate-400/60 border border-cream/15 text-cream placeholder:text-cream/50 focus:ring-primary/50'
          : 'bg-surface border border-cream-500/60 text-chocolate placeholder:text-chocolate/[0.66] focus:ring-chocolate/20 focus:border-chocolate/50',
      ]"
      @input="$emit('update:modelValue', $event.target.value)"
      @keyup.enter="onSubmit"
    />
    <button
      v-if="modelValue"
      type="button"
      :class="[
        'absolute right-0 top-1/2 -translate-y-1/2 h-11 w-11 flex items-center justify-center rounded-full transition-colors',
        isDark ? 'text-cream/70 hover:text-cream hover:bg-cream/10' : 'text-chocolate/60 hover:text-chocolate hover:bg-chocolate/[0.07]',
      ]"
      aria-label="Clear search"
      @click="clear"
    >
      <XMarkIcon class="h-5 w-5" />
    </button>
  </div>
</template>
