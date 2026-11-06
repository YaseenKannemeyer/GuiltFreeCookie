<script setup>
/**
 * StickyTotalBar — the running total and the step's main action.
 * Pinned to the bottom of the screen on phones; an inline footer from md up.
 */
import { useFormat } from "../../composables/useFormat";

defineProps({
  label: { type: String, required: true },
  amount: { type: [Number, String], default: null },
  note: { type: String, default: "" },
});
const { rand } = useFormat();
</script>

<template>
  <div
    class="sticky-total fixed inset-x-0 bottom-0 z-40 bg-background/95 backdrop-blur border-t border-chocolate/[0.12] px-5 pt-4 pb-[max(1.25rem,env(safe-area-inset-bottom))]
           md:static md:z-auto md:mt-10 md:bg-transparent md:backdrop-blur-none md:px-0 md:pb-0 md:pt-6"
  >
    <div class="max-w-[64rem] mx-auto">
      <div class="flex items-baseline justify-between gap-4 mb-3">
        <span class="text-chocolate/75">{{ label }}</span>
        <span v-if="amount !== null" class="text-[2rem] leading-none font-extrabold tabular" aria-live="polite">{{
          rand(amount)
        }}</span>
      </div>
      <p v-if="note" class="text-sm text-chocolate/75 mb-3">{{ note }}</p>
      <slot />
    </div>
  </div>
</template>

<style scoped>
@media (max-height: 500px) {
  .sticky-total { position: static; margin-top: 3.2rem; padding-inline: 0; background: transparent; backdrop-filter: none; }
}
</style>
