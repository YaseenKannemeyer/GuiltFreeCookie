<script setup>
/**
 * CategoryFilter — pill-style category selector for the Menu page.
 */
import { computed } from 'vue'

const props = defineProps({
  modelValue: { type: String, default: 'all' },
  categories: { type: Array, default: () => [] }, // [{id,label,icon}]
  counts: { type: Object, default: () => ({}) }, // optional {CategoryId: count}
})

const emit = defineEmits(['update:modelValue'])

const select = (id) => emit('update:modelValue', id)
</script>

<template>
  <div class="flex flex-wrap items-center gap-2.5">
    <button
      v-for="cat in categories"
      :key="cat.id"
      type="button"
      :class="[
        'group inline-flex items-center gap-2 rounded-full px-5 py-2.5 text-card font-medium transition-all duration-300 ease-cookie',
        modelValue === cat.id
          ? 'bg-chocolate text-cream shadow-soft'
          : 'bg-surface text-chocolate/70 border border-cream-400/60 hover:border-primary hover:text-chocolate hover:-translate-y-0.5',
      ]"
      @click="select(cat.id)"
    >
      <span v-if="cat.icon" class="text-base">{{ cat.icon }}</span>
      <span>{{ cat.label }}</span>
      <span
        v-if="counts[cat.id] !== undefined"
        :class="[
          'ml-1 text-xs px-1.5 py-0.5 rounded-full',
          modelValue === cat.id ? 'bg-cream/20 text-cream' : 'bg-cream-100 text-chocolate/60',
        ]"
      >
        {{ counts[cat.id] }}
      </span>
    </button>
  </div>
</template>
