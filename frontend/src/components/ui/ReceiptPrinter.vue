<script setup>
/**
 * ReceiptPrinter — a little chocolate printer that turns checkout progress
 * into a printed receipt.
 *
 * Stages:      processing -> printing -> complete
 * Feed motion: stepped (line by line) | smooth
 *
 * Usage:
 *   <ReceiptPrinter :stage="stage">
 *     <p>Order GFC-1042</p>
 *     ...receipt lines...
 *   </ReceiptPrinter>
 *
 * Slots: default (the receipt paper), #header (top of the machine, defaults
 * to the wordmark), #status (screen text, defaults to a label per stage).
 * :animate="false" switches every transition off.
 */
import { computed, onBeforeUnmount, onMounted, ref } from "vue";
import { CheckCircleIcon } from "@heroicons/vue/20/solid";

const props = defineProps({
  stage: {
    type: String,
    required: true,
    validator: (value) => ["processing", "printing", "complete"].includes(value),
  },
  feedMotion: { type: String, default: "stepped" }, // stepped | smooth
  animate: { type: Boolean, default: true },
  label: { type: String, default: "Receipt printer" },
});

const STATUS_LABELS = {
  processing: "Processing your order",
  printing: "Printing your receipt",
  complete: "Order complete",
};

const TOOTH_COUNT = 40;
const TOOTH_DEPTH = 4;
const toothPoints = Array.from({ length: TOOTH_COUNT * 2 }, (_, i) => {
  const x = 100 - ((i + 1) * 100) / (TOOTH_COUNT * 2);
  const y = i % 2 === 0 ? "100%" : `calc(100% - ${TOOTH_DEPTH}px)`;
  return `${x}% ${y}`;
}).join(", ");
const paperClipPath = `polygon(0 0, 100% 0, 100% calc(100% - ${TOOTH_DEPTH}px), ${toothPoints})`;

const isComplete = computed(() => props.stage === "complete");
const isReceiptVisible = computed(() => props.stage !== "processing");

// The feed grows from nothing to the paper's height, so the page makes room as it prints
// instead of holding an empty gap while processing.
const paper = ref(null);
const paperHeight = ref(0);
let observer = null;

onMounted(() => {
  observer = new ResizeObserver(([entry]) => (paperHeight.value = entry.target.offsetHeight));
  observer.observe(paper.value);
});

onBeforeUnmount(() => observer?.disconnect());
</script>

<template>
  <section
    :aria-label="label"
    :data-stage="stage"
    class="relative isolate flex w-full max-w-sm flex-col items-center"
    :class="{ 'printer-static': !animate }"
  >
    <div class="printer-body relative isolate w-full overflow-hidden rounded-[24px] bg-chocolate-300 p-3 pb-8">
      <div class="relative z-10 flex h-11 items-start justify-between px-1 pt-0.5">
        <slot name="header">
          <span class="font-display text-base leading-none text-cream-50">GuiltFreeCookie</span>
        </slot>
        <span
          aria-hidden="true"
          class="mt-1 h-2.5 w-2.5 rounded-full transition-colors duration-300"
          :class="isComplete ? 'bg-green-400 shadow-[0_0_8px_rgba(74,222,128,0.8)]' : 'bg-primary animate-pulse'"
        />
      </div>

      <div class="printer-screen relative z-10 overflow-hidden rounded-[12px] bg-chocolate-800 p-4 text-cream">
        <div class="relative z-10 flex min-w-0 items-center gap-2">
          <span aria-hidden="true" class="relative grid h-5 w-5 shrink-0 place-items-center">
            <Transition name="status-icon">
              <CheckCircleIcon v-if="isComplete" key="complete" class="col-start-1 row-start-1 h-5 w-5 text-green-400" />
              <svg
                v-else
                key="working"
                class="col-start-1 row-start-1 h-4 w-4 text-primary animate-spin"
                viewBox="0 0 24 24"
                fill="none"
              >
                <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" />
                <path class="opacity-90" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
              </svg>
            </Transition>
          </span>
          <div role="status" aria-live="polite" class="grid min-w-0 flex-1 items-center">
            <Transition name="status-label">
              <span :key="stage" class="col-start-1 row-start-1 truncate text-sm font-semibold leading-none text-cream/80">
                <slot name="status" :stage="stage">{{ STATUS_LABELS[stage] }}</slot>
              </span>
            </Transition>
          </div>
        </div>
      </div>

      <div
        aria-hidden="true"
        class="absolute inset-x-6 bottom-3 z-40 h-2 rounded-[4px] bg-chocolate-900 shadow-[inset_0_2px_4px_rgba(0,0,0,0.6)]"
      />
    </div>

    <div class="relative z-50 -mt-4 w-[calc(80%+48px)] max-w-full overflow-hidden px-6 pb-6">
      <div
        v-if="isReceiptVisible"
        aria-hidden="true"
        class="pointer-events-none absolute inset-x-6 -top-1 z-20 h-2 bg-chocolate/75 blur-[6px]"
      />
      <div
        class="receipt-feed relative isolate"
        :data-stage="stage"
        :data-feed="feedMotion"
        :aria-hidden="!isComplete"
        :style="{ '--paper-h': `${paperHeight}px` }"
      >
        <article
          ref="paper"
          class="receipt-paper z-10 px-6 pb-8 pt-7 font-mono text-sm text-chocolate"
          :style="{ clipPath: paperClipPath }"
        >
          <slot />
        </article>
      </div>
    </div>
  </section>
</template>

<style scoped>
.printer-body {
  box-shadow:
    0 20px 36px -20px rgba(46, 14, 11, 0.55),
    0 6px 14px -8px rgba(46, 14, 11, 0.24),
    inset 0 1px 0 rgba(251, 249, 244, 0.14),
    inset 0 -1px 0 rgba(0, 0, 0, 0.4);
}
.printer-body::before {
  content: "";
  position: absolute;
  inset: 0;
  z-index: 0;
  border-radius: inherit;
  pointer-events: none;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='180' height='180'%3E%3Cfilter id='n'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='0.9' numOctaves='3' stitchTiles='stitch'/%3E%3CfeColorMatrix type='saturate' values='0'/%3E%3C/filter%3E%3Crect width='100%25' height='100%25' filter='url(%23n)'/%3E%3C/svg%3E");
  background-size: 180px 180px;
  opacity: 0.18;
  mix-blend-mode: overlay;
}
.printer-screen {
  box-shadow: inset 0 2px 6px rgba(0, 0, 0, 0.5);
}
.printer-screen::after {
  content: "";
  position: absolute;
  inset: 0;
  z-index: 20;
  border-radius: inherit;
  pointer-events: none;
  box-shadow: inset 0 0 24px 4px rgba(14, 3, 2, 0.5);
}

.receipt-paper {
  background-color: #fbf9f4;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='180' height='180'%3E%3Cfilter id='n'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='0.75' numOctaves='2' stitchTiles='stitch'/%3E%3CfeColorMatrix type='saturate' values='0'/%3E%3C/filter%3E%3Crect width='100%25' height='100%25' filter='url(%23n)' opacity='0.35'/%3E%3C/svg%3E");
  background-size: 180px 180px;
  background-blend-mode: soft-light;
}

/* The paper hangs from the bottom of the feed and the outer wrapper clips everything
   above the slot, so growing the feed's height is what pushes the paper out. */
.receipt-feed {
  height: 0;
  opacity: 0;
  transition:
    opacity 0.16s cubic-bezier(0.23, 1, 0.32, 1),
    height 1.75s cubic-bezier(0.77, 0, 0.175, 1);
}
.receipt-feed .receipt-paper {
  position: absolute;
  inset: auto 0 0;
}
.receipt-feed[data-stage="printing"] {
  height: var(--paper-h);
  opacity: 1;
}
.receipt-feed[data-stage="printing"][data-feed="stepped"] {
  animation: receipt-feed 1.75s linear both;
}
.receipt-feed[data-stage="complete"] {
  height: auto;
  opacity: 1;
}
.receipt-feed[data-stage="complete"] .receipt-paper {
  position: relative;
}
.receipt-feed::before {
  content: "";
  position: absolute;
  inset: 12px 12px 16px;
  z-index: 0;
  border-radius: 2px;
  pointer-events: none;
  box-shadow: 0 8px 24px rgba(46, 14, 11, 0.24);
}

/* Each feed holds briefly, like a thermal printer advancing a line. */
@keyframes receipt-feed {
  0% { height: 2px; }
  7.5%, 10.5% { height: calc(var(--paper-h) * 0.09); }
  18%, 21% { height: calc(var(--paper-h) * 0.19); }
  28.5%, 31.5% { height: calc(var(--paper-h) * 0.3); }
  39%, 42% { height: calc(var(--paper-h) * 0.42); }
  49.5%, 52.5% { height: calc(var(--paper-h) * 0.55); }
  60%, 63% { height: calc(var(--paper-h) * 0.68); }
  70.5%, 73.5% { height: calc(var(--paper-h) * 0.8); }
  81%, 84% { height: calc(var(--paper-h) * 0.9); }
  91.5%, 94.5% { height: calc(var(--paper-h) * 0.97); }
  100% { height: var(--paper-h); }
}

.status-icon-enter-active,
.status-icon-leave-active {
  transition:
    opacity 0.16s cubic-bezier(0.23, 1, 0.32, 1),
    transform 0.16s cubic-bezier(0.23, 1, 0.32, 1);
}
.status-icon-enter-from {
  opacity: 0;
  transform: scale(0.94);
}
.status-icon-leave-to {
  opacity: 0;
  transform: scale(0.96);
}

.status-label-enter-active,
.status-label-leave-active {
  transition:
    opacity 0.18s cubic-bezier(0.23, 1, 0.32, 1),
    transform 0.18s cubic-bezier(0.23, 1, 0.32, 1);
}
.status-label-enter-from {
  opacity: 0;
  transform: translateY(4px);
}
.status-label-leave-to {
  opacity: 0;
  transform: translateY(-4px);
}

.printer-static *,
.printer-static .receipt-feed {
  transition: none !important;
  animation: none !important;
}
</style>
