<script setup>
/**
 * AccordionItem — single expandable FAQ row.
 * Designed to be used by parent AccordionList which manages open state.
 */
import { PlusIcon, MinusIcon } from '@heroicons/vue/24/outline'

defineProps({
  question: { type: String, required: true },
  answer: { type: String, required: true },
  isOpen: { type: Boolean, default: false },
})

defineEmits(['toggle'])
</script>

<template>
  <div
    :class="[
      'rounded-3xl border transition-all duration-300 overflow-hidden',
      isOpen
        ? 'bg-cream-50 border-primary/40 shadow-soft'
        : 'bg-surface border-cream-400/60 hover:border-primary/30',
    ]"
  >
    <button
      type="button"
      class="w-full flex items-center justify-between gap-4 px-6 py-5 text-left"
      :aria-expanded="isOpen"
      @click="$emit('toggle')"
    >
      <span class="text-lg font-medium text-chocolate">{{ question }}</span>
      <span
        :class="[
          'shrink-0 flex items-center justify-center h-8 w-8 rounded-full transition-all duration-300',
          isOpen ? 'bg-primary text-chocolate rotate-180' : 'bg-cream-100 text-chocolate/70',
        ]"
      >
        <MinusIcon v-if="isOpen" class="h-5 w-5" />
        <PlusIcon v-else class="h-5 w-5" />
      </span>
    </button>
    <Transition name="accordion">
      <div v-if="isOpen" class="px-6 pb-6">
        <p class="text-chocolate/70 leading-relaxed">{{ answer }}</p>
      </div>
    </Transition>
  </div>
</template>
