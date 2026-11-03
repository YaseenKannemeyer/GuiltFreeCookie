<script setup>
/**
 * HoldToConfirmButton — press and hold to confirm a destructive action.
 * Letting go early rolls the fill back. Works with pointer, Enter and Space.
 *
 * Usage:
 *   <HoldToConfirmButton :reset-after="0" @confirm="stage = 'undo'">
 *     <TrashIcon class="h-4 w-4" /> Hold to delete
 *   </HoldToConfirmButton>
 *
 * Emits confirm("pointer" | "keyboard"). The #confirmed slot replaces the
 * default "Confirmed" label.
 */
import { computed, onBeforeUnmount, ref, watch } from "vue";
import { CheckIcon } from "@heroicons/vue/20/solid";

const props = defineProps({
  duration: { type: Number, default: 1600 },
  resetAfter: { type: Number, default: 1800 },
  disabled: { type: Boolean, default: false },
});

const emit = defineEmits(["confirm"]);

const BOUNDARY_PADDING = 8;

const status = ref("idle"); // idle | holding | confirmed
const inputMode = ref(null);
let confirmTimer = null;
let resetTimer = null;
let activePointerId = null;
let holding = false;

const clearConfirmTimer = () => {
  clearTimeout(confirmTimer);
  confirmTimer = null;
};

const completeHold = () => {
  if (!holding) return;
  holding = false;
  activePointerId = null;
  clearConfirmTimer();
  status.value = "confirmed";
  emit("confirm", inputMode.value);

  if (props.resetAfter > 0) {
    resetTimer = setTimeout(() => {
      status.value = "idle";
      resetTimer = null;
    }, props.resetAfter);
  }
};

const cancelHold = () => {
  if (!holding) return;
  holding = false;
  activePointerId = null;
  clearConfirmTimer();
  status.value = "idle";
};

const startHold = (input) => {
  if (props.disabled || status.value === "confirmed" || holding) return;
  clearTimeout(resetTimer);
  resetTimer = null;
  inputMode.value = input;
  holding = true;
  status.value = "holding";
  confirmTimer = setTimeout(completeHold, props.duration);
};

watch(
  () => props.disabled,
  (disabled) => disabled && cancelHold(),
);

onBeforeUnmount(() => {
  clearConfirmTimer();
  clearTimeout(resetTimer);
});

const releaseCapture = (el, pointerId) => {
  if (el.hasPointerCapture(pointerId)) el.releasePointerCapture(pointerId);
};

const onPointerDown = (event) => {
  if (!event.isPrimary || event.button !== 0 || props.disabled) return;
  activePointerId = event.pointerId;
  event.currentTarget.setPointerCapture(event.pointerId);
  startHold("pointer");
};

const onPointerMove = (event) => {
  if (!holding || activePointerId !== event.pointerId) return;
  const rect = event.currentTarget.getBoundingClientRect();
  const isOutside =
    event.clientX < rect.left - BOUNDARY_PADDING ||
    event.clientX > rect.right + BOUNDARY_PADDING ||
    event.clientY < rect.top - BOUNDARY_PADDING ||
    event.clientY > rect.bottom + BOUNDARY_PADDING;

  if (isOutside) {
    cancelHold();
    releaseCapture(event.currentTarget, event.pointerId);
  }
};

const onPointerEnd = (event) => {
  if (activePointerId !== event.pointerId) return;
  cancelHold();
  releaseCapture(event.currentTarget, event.pointerId);
};

const isActivationKey = (event) => event.key === "Enter" || event.key === " ";

const onKeyDown = (event) => {
  if (event.repeat || !isActivationKey(event)) return;
  event.preventDefault();
  startHold("keyboard");
};

const onKeyUp = (event) => {
  if (!isActivationKey(event)) return;
  event.preventDefault();
  cancelHold();
};

const isHolding = computed(() => status.value === "holding");

const fillStyle = computed(() => ({
  clipPath: isHolding.value ? "inset(0 0 0 0)" : "inset(0 100% 0 0)",
  transitionProperty: "clip-path",
  transitionDuration: inputMode.value === "keyboard" ? "0ms" : isHolding.value ? `${props.duration}ms` : "180ms",
  transitionTimingFunction: isHolding.value ? "linear" : "cubic-bezier(0.23, 1, 0.32, 1)",
}));
</script>

<template>
  <button
    type="button"
    class="relative isolate inline-flex min-h-11 min-w-44 touch-none select-none items-center justify-center overflow-hidden rounded-full border-2 border-red-700 px-5 py-2.5 text-sm font-bold transition-[transform,background-color,color] duration-150 ease-cookie cursor-pointer focus:outline-none focus-visible:ring-2 focus-visible:ring-red-700 focus-visible:ring-offset-2 focus-visible:ring-offset-background disabled:cursor-not-allowed disabled:opacity-50"
    :class="[
      status === 'confirmed' ? 'bg-red-700 text-white' : 'bg-red-700/[0.08] text-red-700',
      inputMode === 'pointer' && 'active:scale-[0.98]',
    ]"
    :disabled="disabled"
    :aria-busy="isHolding"
    @blur="cancelHold"
    @keydown="onKeyDown"
    @keyup="onKeyUp"
    @lostpointercapture="cancelHold"
    @pointercancel="onPointerEnd"
    @pointerdown="onPointerDown"
    @pointermove="onPointerMove"
    @pointerup="onPointerEnd"
  >
    <span v-if="status === 'confirmed'" class="relative flex items-center justify-center gap-2">
      <slot name="confirmed">
        <CheckIcon class="h-4 w-4" aria-hidden="true" />
        Confirmed
      </slot>
    </span>
    <template v-else>
      <span class="relative flex items-center justify-center gap-2">
        <slot>Hold to confirm</slot>
      </span>
      <span
        aria-hidden="true"
        class="absolute inset-0 flex items-center justify-center gap-2 bg-red-700 px-5 text-white"
        :style="fillStyle"
      >
        <slot>Hold to confirm</slot>
      </span>
    </template>
  </button>
</template>
