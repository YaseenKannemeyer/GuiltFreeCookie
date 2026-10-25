<script setup>
import { computed } from "vue";

// Labelled horizontal bars in one hue. Every value is printed, so nothing hides behind a hover.
const props = defineProps({
  rows: { type: Array, required: true }, // [{ key, label, value, display, note? }]
  empty: { type: String, default: "Nothing yet for this period." },
});

const max = computed(() => Math.max(0, ...props.rows.map((row) => row.value)));
</script>

<template>
  <p v-if="!rows.length || max === 0" class="text-chocolate/75">{{ empty }}</p>
  <ul v-else class="space-y-4">
    <li v-for="row in rows" :key="row.key" class="min-w-0">
      <div class="flex items-baseline justify-between gap-3">
        <span class="font-bold truncate">{{ row.label }}</span>
        <span class="tabular shrink-0">{{ row.display }}</span>
      </div>
      <div class="mt-1.5 h-3 rounded-r bg-chocolate/[0.06]" aria-hidden="true">
        <div
          class="h-3 rounded-r bg-[#9A6E45] transition-[width] duration-300 motion-reduce:transition-none"
          :style="{ width: `${Math.max(row.value > 0 ? 2 : 0, (100 * row.value) / max)}%` }"
        />
      </div>
      <p v-if="row.note" class="mt-1 text-sm text-chocolate/70">{{ row.note }}</p>
    </li>
  </ul>
</template>
