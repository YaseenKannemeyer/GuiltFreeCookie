<script setup>
/**
 * QuantitySelector — minus / number / plus stepper.
 *
 * Usage:
 *   <QuantitySelector v-model="qty" :min="1" :max="10" />
 *   <QuantitySelector v-model="qty" variant="dark" />   (for use on dark chocolate bg)
 */
import { computed } from 'vue'
import { MinusIcon, PlusIcon } from '@heroicons/vue/24/outline'

const props = defineProps({
  modelValue: { type: Number, default: 1 },
  min: { type: Number, default: 1 },
  max: { type: Number, default: 99 },
  size: { type: String, default: 'md' }, // sm | md | lg
  variant: { type: String, default: 'light' }, // light | dark
})

const emit = defineEmits(['update:modelValue'])

const sizeClass = computed(() => {
  switch (props.size) {
    case 'sm':
      return { wrap: 'h-10', btn: 'h-10 w-10', text: 'text-sm', icon: 'h-4 w-4' }
    case 'lg':
      return { wrap: 'h-16', btn: 'h-16 w-16', text: 'text-xl', icon: 'h-6 w-6' }
    case 'md':
    default:
      return { wrap: 'h-14', btn: 'h-14 w-14', text: 'text-lg', icon: 'h-5 w-5' }
  }
})

const isDark = computed(() => props.variant === 'dark')

const decrement = () => {
  if (props.modelValue > props.min) emit('update:modelValue', props.modelValue - 1)
}
const increment = () => {
  if (props.modelValue < props.max) emit('update:modelValue', props.modelValue + 1)
}
</script>

<template>
  <div
    :class="[
      'inline-flex items-center rounded-full border transition-all',
      sizeClass.wrap,
      isDark
        ? 'bg-chocolate-400/60 border-cream/15'
        : 'bg-cream-50 border-cream-400/60',
    ]"
  >
    <button
      type="button"
      :class="[
        'flex items-center justify-center rounded-full transition-all duration-300 ease-cookie',
        sizeClass.btn,
        modelValue <= min
          ? 'opacity-30 cursor-not-allowed'
          : isDark
            ? 'text-cream hover:bg-chocolate-300 hover:scale-105'
            : 'text-chocolate hover:bg-primary/20 hover:scale-105',
      ]"
      :disabled="modelValue <= min"
      :aria-label="'Decrease quantity'"
      @click="decrement"
    >
      <MinusIcon :class="sizeClass.icon" />
    </button>
    <input
      :value="modelValue"
      type="number"
      :min="min"
      :max="max"
      readonly
      :class="[
        'w-12 text-center bg-transparent border-0 focus:outline-none font-semibold',
        sizeClass.text,
        isDark ? 'text-cream' : 'text-chocolate',
      ]"
      style="-moz-appearance: textfield"
    />
    <button
      type="button"
      :class="[
        'flex items-center justify-center rounded-full transition-all duration-300 ease-cookie',
        sizeClass.btn,
        modelValue >= max
          ? 'opacity-30 cursor-not-allowed'
          : isDark
            ? 'text-cream hover:bg-chocolate-300 hover:scale-105'
            : 'text-chocolate hover:bg-primary/20 hover:scale-105',
      ]"
      :disabled="modelValue >= max"
      :aria-label="'Increase quantity'"
      @click="increment"
    >
      <PlusIcon :class="sizeClass.icon" />
    </button>
  </div>
</template>

<style scoped>
input[type='number']::-webkit-outer-spin-button,
input[type='number']::-webkit-inner-spin-button {
  -webkit-appearance: none;
  margin: 0;
}
</style>
