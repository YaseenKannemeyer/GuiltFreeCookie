<script setup>
/**
 * AdminPageHeader — title, one-line description and an actions slot.
 */
defineProps({
  title: { type: String, required: true },
  description: { type: String, default: "" },
});
</script>

<template>
  <div class="flex flex-col sm:flex-row sm:items-end justify-between gap-4 mb-8">
    <div class="min-w-0">
      <h1 class="font-display text-display">{{ title }}</h1>
      <p v-if="description" class="mt-3 text-chocolate/75">{{ description }}</p>
    </div>
    <div v-if="$slots.default" class="flex flex-wrap items-center gap-2 shrink-0">
      <slot />
    </div>
  </div>
</template>
