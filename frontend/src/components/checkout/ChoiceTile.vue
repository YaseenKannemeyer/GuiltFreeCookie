<script setup>
/**
 * ChoiceTile — a radio option drawn as a tile. The chosen tile floods with caramel
 * and takes a chocolate edge; the radio itself stays a real, keyboard-operable input.
 */
defineProps({
  modelValue: { type: [String, Number], default: null },
  value: { type: [String, Number], required: true },
  name: { type: String, required: true },
  disabled: { type: Boolean, default: false },
});
const emit = defineEmits(["update:modelValue"]);
</script>

<template>
  <label
    :class="[
      'group relative block rounded-2xl p-4 cursor-pointer transition-[background-color,border-color,box-shadow] duration-200',
      modelValue === value
        ? 'bg-primary/25 border-2 border-chocolate shadow-soft'
        : 'bg-cream-100/60 border border-cream-500 hover:border-chocolate/50 m-px',
      disabled && 'opacity-55 cursor-not-allowed',
    ]"
  >
    <input
      type="radio"
      class="peer sr-only"
      :name="name"
      :value="value"
      :checked="modelValue === value"
      :disabled="disabled"
      @change="emit('update:modelValue', value)"
    />
    <span
      class="pointer-events-none absolute -inset-1 rounded-[1.2rem] ring-2 ring-chocolate ring-offset-2 ring-offset-background opacity-0 peer-focus-visible:opacity-100"
      aria-hidden="true"
    />
    <slot :selected="modelValue === value" />
  </label>
</template>
