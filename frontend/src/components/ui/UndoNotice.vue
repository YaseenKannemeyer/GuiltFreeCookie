<script setup>
/**
 * UndoNotice — "done" notice with an Undo button that drains as the undo
 * window runs out. Pairs with HoldToConfirmButton.
 *
 * Usage:
 *   <UndoNotice message="Cookie deleted" @undo="restore" @expire="commitDelete" />
 *
 * Emits undo("pointer" | "keyboard") or expire, never both.
 */
import { onBeforeUnmount, onMounted, ref } from "vue";
import { ArrowUturnLeftIcon, CheckCircleIcon } from "@heroicons/vue/20/solid";

const props = defineProps({
  duration: { type: Number, default: 5000 },
  message: { type: String, default: "Deleted" },
});

const emit = defineEmits(["expire", "undo"]);

const fill = ref(null);
const counting = ref(false);
let timer = null;

onMounted(() => {
  // Commit the full fill before draining it, or the transition never runs.
  fill.value.getBoundingClientRect();
  counting.value = true;
  timer = setTimeout(() => emit("expire"), props.duration);
});

onBeforeUnmount(() => clearTimeout(timer));

const onUndo = (event) => {
  clearTimeout(timer);
  emit("undo", event.detail === 0 ? "keyboard" : "pointer");
};
</script>

<template>
  <div
    role="status"
    aria-live="polite"
    class="flex min-h-11 items-center justify-between gap-3 rounded-full bg-chocolate/[0.07] py-1 pl-4 pr-1"
  >
    <p class="flex min-w-0 items-center gap-2 text-sm font-semibold text-chocolate">
      <CheckCircleIcon class="h-5 w-5 shrink-0 text-green-600" aria-hidden="true" />
      <span class="truncate">{{ message }}</span>
    </p>

    <button
      type="button"
      class="relative isolate flex min-h-11 min-w-24 shrink-0 items-center justify-center overflow-hidden rounded-full border-2 border-chocolate/25 bg-background px-3.5 text-sm font-bold text-chocolate transition-transform duration-150 ease-cookie cursor-pointer active:scale-[0.97] focus:outline-none focus-visible:ring-2 focus-visible:ring-chocolate focus-visible:ring-offset-2 focus-visible:ring-offset-background"
      @click="onUndo"
    >
      <span class="flex items-center gap-1.5">
        <ArrowUturnLeftIcon class="h-4 w-4" aria-hidden="true" />
        Undo
      </span>
      <span
        ref="fill"
        aria-hidden="true"
        class="absolute inset-0 flex items-center justify-center gap-1.5 bg-chocolate px-3.5 text-cream-50"
        :style="{
          clipPath: counting ? 'inset(0 100% 0 0)' : 'inset(0 0 0 0)',
          transitionProperty: 'clip-path',
          transitionDuration: `${duration}ms`,
          transitionTimingFunction: 'linear',
        }"
      >
        <ArrowUturnLeftIcon class="h-4 w-4" aria-hidden="true" />
        Undo
      </span>
    </button>
  </div>
</template>
