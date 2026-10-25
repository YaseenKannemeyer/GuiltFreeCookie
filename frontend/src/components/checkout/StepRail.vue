<script setup>
/**
 * StepRail — the pill rail that runs from the cart through checkout into order tracking.
 * Each step: { label, state: "done" | "current" | "todo", to? } (done steps with `to` are links).
 */
import { CheckIcon } from "@heroicons/vue/20/solid";

defineProps({
  steps: { type: Array, required: true },
  label: { type: String, default: "Progress" },
});
</script>

<template>
  <nav :aria-label="label">
    <ol class="flex flex-wrap items-center gap-2">
      <li v-for="(step, i) in steps" :key="step.label">
        <component
          :is="step.state === 'done' && step.to ? 'RouterLink' : 'span'"
          :to="step.state === 'done' ? step.to : undefined"
          :aria-current="step.state === 'current' ? 'step' : undefined"
          :class="[
            'group relative isolate overflow-hidden inline-flex items-center gap-1.5 h-9 px-3.5 rounded-full text-sm font-extrabold whitespace-nowrap bg-chocolate/[0.07] transition-colors duration-200',
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
