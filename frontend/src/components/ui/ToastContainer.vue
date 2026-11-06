<script setup>
/**
 * ToastContainer — mount once (see App.vue). Renders whatever is pushed
 * via useToast().showToast(message, type). Types: success | error | info.
 */
import {
  CheckCircleIcon,
  ExclamationCircleIcon,
  InformationCircleIcon,
  XMarkIcon,
} from "@heroicons/vue/24/solid";
import { useToast } from "../../composables/useToast";
import { activeModal } from "../../composables/modalStack";

const { toasts, removeToast } = useToast();
</script>

<template>
  <Teleport :to="activeModal || '#teleports'">
    <!-- Polite live region so screen readers announce each toast -->
    <div
      class="fixed top-4 right-4 sm:top-6 sm:right-6 z-[200] flex flex-col gap-3 w-[calc(100%-3.2rem)] max-w-sm"
      role="status"
      aria-live="polite"
    >
      <TransitionGroup name="toast">
        <div
          v-for="toast in toasts"
          :key="toast.id"
          class="flex items-center gap-3 rounded-2xl py-2 pl-5 pr-2 shadow-soft-lg border"
          :class="
            toast.type === 'error'
              ? 'bg-red-800 border-red-800 text-cream-50'
              : toast.type === 'info'
                ? 'bg-chocolate border-chocolate text-cream-50'
                : 'bg-surface border-chocolate/10 text-chocolate'
          "
        >
          <ExclamationCircleIcon v-if="toast.type === 'error'" class="h-5 w-5 shrink-0 text-red-200" />
          <InformationCircleIcon v-else-if="toast.type === 'info'" class="h-5 w-5 shrink-0 text-primary" />
          <CheckCircleIcon v-else class="h-5 w-5 shrink-0 text-primary-700" />

          <p class="text-sm font-semibold flex-1 py-2">{{ toast.message }}</p>

          <button
            type="button"
            class="h-11 w-11 shrink-0 flex items-center justify-center rounded-xl opacity-60 hover:opacity-100 transition-opacity"
            aria-label="Dismiss"
            @click="removeToast(toast.id)"
          >
            <XMarkIcon class="h-5 w-5" />
          </button>
        </div>
      </TransitionGroup>
    </div>
  </Teleport>
</template>

<style scoped>
.toast-enter-active,
.toast-leave-active {
  transition:
    opacity 0.3s ease,
    transform 0.3s ease;
}
.toast-enter-from,
.toast-leave-to {
  opacity: 0;
  transform: translateX(40px);
}
.toast-leave-active {
  position: absolute;
  width: 100%;
}
</style>
