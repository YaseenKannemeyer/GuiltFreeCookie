<script setup>
/**
 * StepRail — the pill rail that runs from the cart through checkout into order tracking.
 * Each step: { label, state: "done" | "current" | "todo", to? } (done steps with `to` are links).
 */
import { onMounted, ref } from "vue";
import { CheckIcon } from "@heroicons/vue/20/solid";

defineProps({
  steps: { type: Array, required: true },
  label: { type: String, default: "Progress" },
});

// One row always; on narrow screens it scrolls sideways, so start with the current step in view.
const rail = ref(null);
onMounted(() => {
  const current = rail.value?.querySelector("[aria-current='step']");
  if (current) rail.value.scrollLeft = current.offsetLeft - rail.value.clientWidth / 2 + current.offsetWidth / 2;
});
</script>

<template>
  <nav :aria-label="label">
    <ol ref="rail" class="no-scrollbar -my-1 flex min-w-0 items-center gap-1.5 overflow-x-auto py-1">
      <li v-for="(step, i) in steps" :key="step.label" class="shrink-0">
        <component
          :is="step.state === 'done' && step.to ? 'RouterLink' : 'span'"
          :to="step.state === 'done' ? step.to : undefined"
          :aria-current="step.state === 'current' ? 'step' : undefined"
          :class="[
            'group relative isolate overflow-hidden inline-flex items-center gap-1.5 min-h-11 min-w-11 px-3 rounded-full text-sm font-extrabold whitespace-nowrap bg-chocolate/[0.07] transition-colors duration-200',
            step.state === 'done' && 'text-cream-50',
            step.state === 'current' && 'text-chocolate',
            step.state === 'todo' && 'text-chocolate/70',
          ]"
        >
          <span
            aria-hidden="true"
            :class="[
              'absolute inset-0 -z-10 origin-left transition-transform duration-200 ease-out motion-reduce:transition-none',
              step.state === 'todo' ? 'scale-x-0' : 'scale-x-100',
              step.state === 'done' ? 'bg-chocolate' : 'bg-primary',
              step.state === 'done' && step.to && 'group-hover:bg-chocolate-300',
            ]"
          />
          <CheckIcon v-if="step.state === 'done'" class="h-4 w-4" aria-hidden="true" />
          <span v-else class="tabular" aria-hidden="true">{{ i + 1 }}</span>
          <span :class="step.state === 'done' && 'sr-only sm:not-sr-only'">{{ step.label }}</span>
          <span v-if="step.state === 'done'" class="sr-only">(done)</span>
        </component>
      </li>
    </ol>
  </nav>
</template>
