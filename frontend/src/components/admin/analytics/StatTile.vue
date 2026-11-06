<script setup>
import { computed } from "vue";
import { ArrowDownRightIcon, ArrowUpRightIcon } from "@heroicons/vue/20/solid";

// A headline number with its change against the previous period of the same length.
const props = defineProps({
  label: { type: String, required: true },
  value: { type: String, required: true },
  current: { type: Number, default: null },
  previous: { type: Number, default: null },
  period: { type: String, default: "" },
  hero: { type: Boolean, default: false },
});

const delta = computed(() => {
  if (props.current == null || props.previous == null) return null;
  if (props.previous === 0) return props.current === 0 ? { text: "No change", dir: 0 } : { text: "New this period", dir: 1 };
  const pct = Math.round(((props.current - props.previous) / props.previous) * 100);
  if (pct === 0) return { text: "No change", dir: 0 };
  return { text: `${pct > 0 ? "+" : "−"}${Math.abs(pct)}%`, dir: Math.sign(pct) };
});
</script>

<template>
  <div class="min-w-0">
    <p class="text-sm font-bold text-chocolate/75">{{ label }}</p>
    <p :class="['font-extrabold leading-none mt-2 break-words', hero ? 'text-[clamp(3rem,4vw,4.8rem)]' : 'text-[3rem]']">{{ value }}</p>
    <p v-if="delta" class="mt-2 flex items-center gap-1 text-sm text-chocolate/75">
      <ArrowUpRightIcon v-if="delta.dir > 0" class="h-4 w-4 shrink-0" aria-hidden="true" />
      <ArrowDownRightIcon v-else-if="delta.dir < 0" class="h-4 w-4 shrink-0" aria-hidden="true" />
      <span><strong class="font-extrabold text-chocolate">{{ delta.text }}</strong> {{ period }}</span>
    </p>
  </div>
</template>
