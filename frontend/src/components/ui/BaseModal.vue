<script setup>
/**
 * BaseModal — accessible teleport modal with backdrop blur + esc-to-close.
 *
 * Usage:
 *   <BaseModal :open="isOpen" @close="isOpen = false" title="Title">
 *     ...content...
 *   </BaseModal>
 */
import { watch, onUnmounted } from 'vue'
import { XMarkIcon } from '@heroicons/vue/24/outline'

const props = defineProps({
  open: { type: Boolean, default: false },
  title: { type: String, default: '' },
  size: { type: String, default: 'md' }, // sm | md | lg
  closeOnBackdrop: { type: Boolean, default: true },
})

const emit = defineEmits(['close'])

const sizeClass = {
  sm: 'max-w-md',
  md: 'max-w-lg',
  lg: 'max-w-2xl',
}[props.size]

const onKey = (e) => {
  if (e.key === 'Escape' && props.open) emit('close')
}
watch(
  () => props.open,
  (v) => {
    if (typeof document === 'undefined') return
    document.body.style.overflow = v ? 'hidden' : ''
    if (v) document.addEventListener('keydown', onKey)
    else document.removeEventListener('keydown', onKey)
  },
)
onUnmounted(() => {
  if (typeof document !== 'undefined') {
    document.body.style.overflow = ''
    document.removeEventListener('keydown', onKey)
  }
})
</script>

<template>
  <Teleport to="body">
    <Transition name="overlay">
      <div
        v-if="open"
        class="fixed inset-0 z-[90] bg-chocolate/60 backdrop-blur-sm flex items-center justify-center p-4"
        @click.self="closeOnBackdrop && emit('close')"
      >
        <Transition name="modal" appear>
          <div
            v-if="open"
            :class="['w-full bg-background rounded-4xl shadow-soft-xl p-8', sizeClass]"
          >
            <div v-if="title || $slots.header" class="flex items-start justify-between mb-6">
              <h3 v-if="title" class="text-2xl font-medium text-chocolate">{{ title }}</h3>
              <slot name="header" />
              <button
                class="text-chocolate/50 hover:text-chocolate transition-colors p-1 rounded-full hover:bg-cream-100"
                aria-label="Close"
                @click="emit('close')"
              >
                <XMarkIcon class="h-6 w-6" />
              </button>
            </div>
            <slot />
          </div>
        </Transition>
      </div>
    </Transition>
  </Teleport>
</template>
