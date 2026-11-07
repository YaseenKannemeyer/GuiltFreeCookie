<script>
// Nested confirmations share the page lock. Closing one must not unlock another.
let openModals = 0;
let previousOverflow = '';
</script>

<script setup>
import { computed, nextTick, onBeforeUnmount, ref, useId, watch } from 'vue'
import { XMarkIcon } from '@heroicons/vue/24/outline'
import { registerModal, unregisterModal } from '../../composables/modalStack'

const props = defineProps({
  open: { type: Boolean, default: false },
  title: { type: String, default: '' },
  size: { type: String, default: 'md' },
  closeOnBackdrop: { type: Boolean, default: true },
})
const emit = defineEmits(['close'])
const dialog = ref(null)
const titleId = `modal-${useId()}`
const sizeClass = computed(() => ({ sm: 'max-w-md', md: 'max-w-lg', lg: 'max-w-2xl' })[props.size])
let locked = false

function containTab(event) {
  if (event.key !== 'Tab') return
  const controls = [...dialog.value.querySelectorAll('a[href], button, input, select, textarea, [tabindex]')]
    .filter(el => !el.disabled && el.tabIndex >= 0 && el.getClientRects().length && getComputedStyle(el).visibility !== 'hidden')
  // WebKit may skip buttons according to the system keyboard preference.
  // Traverse explicitly so neither Tab direction can leave this modal.
  event.preventDefault()
  if (!controls.length) return
  const current = controls.indexOf(document.activeElement)
  const next = current < 0 ? (event.shiftKey ? controls.length - 1 : 0)
    : (current + (event.shiftKey ? -1 : 1) + controls.length) % controls.length
  controls[next].focus()
}

function releasePage() {
  unregisterModal(dialog.value)
  if (!locked) return
  locked = false
  if (--openModals === 0) document.body.style.overflow = previousOverflow
}

// A native modal dialog contains keyboard focus, makes the page inert, and
// restores focus to its opener, including when a confirmation opens above it.
watch(() => props.open, async (open) => {
  await nextTick()
  if (!dialog.value || open !== props.open) return
  if (open) {
    if (!locked) {
      if (openModals++ === 0) previousOverflow = document.body.style.overflow
      locked = true
      document.body.style.overflow = 'hidden'
    }
    if (!dialog.value.open) dialog.value.showModal()
    registerModal(dialog.value)
  } else {
    dialog.value.close()
    releasePage()
  }
}, { immediate: true })

onBeforeUnmount(releasePage)
</script>

<template>
  <Teleport to="#teleports">
    <dialog
      ref="dialog"
      class="modal-overlay"
      :aria-labelledby="title ? titleId : undefined"
      aria-modal="true"
      @cancel.prevent="emit('close')"
      @keydown="containTab"
      @click.self="closeOnBackdrop && emit('close')"
    >
      <Transition name="modal">
        <div v-if="open" :class="['modal-panel w-full min-w-0 flex flex-col bg-background rounded-[20px] shadow-soft-xl text-chocolate', sizeClass]">
          <div v-if="title || $slots.header" class="flex items-start justify-between gap-4 px-5 pt-5 pb-4 sm:px-7 sm:pt-7 shrink-0">
            <h2 v-if="title" :id="titleId" class="min-w-0 text-base font-semibold tracking-[-0.015em] text-chocolate">{{ title }}</h2>
            <slot name="header" />
            <button type="button" class="-mr-2 -mt-1 h-11 w-11 shrink-0 flex items-center justify-center text-chocolate/60 hover:text-chocolate transition-colors rounded-full hover:bg-chocolate/[0.07]" aria-label="Close" @click="emit('close')">
              <XMarkIcon class="h-5 w-5" />
            </button>
          </div>
          <div class="min-h-0 overflow-y-auto overscroll-contain px-5 pb-5 sm:px-7 sm:pb-7" :class="{ 'pt-7': !title && !$slots.header }">
            <slot />
          </div>
        </div>
      </Transition>
    </dialog>
  </Teleport>
</template>

<style scoped>
.modal-overlay {
  position: fixed;
  inset: 0;
  margin: 0;
  width: 100%;
  max-width: none;
  height: 100dvh;
  max-height: none;
  padding: max(1.6rem, env(safe-area-inset-top)) max(1.6rem, env(safe-area-inset-right)) max(1.6rem, env(safe-area-inset-bottom)) max(1.6rem, env(safe-area-inset-left));
  border: 0;
  background: transparent;
  overflow: hidden;
}
.modal-overlay[open] { display: flex; align-items: center; justify-content: center; }
.modal-overlay::backdrop { background: rgb(46 14 11 / 60%); backdrop-filter: blur(4px); }
.modal-panel { min-height: 0; max-height: 100%; }
@media (max-width: 639px) {
  .modal-overlay { --fs-base: 1.6rem; font-size: var(--fs-base); }
}
</style>
