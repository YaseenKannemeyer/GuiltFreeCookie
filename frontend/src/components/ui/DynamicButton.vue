<script setup>
/**
 * DynamicButton — pill button whose label and icon roll over when they
 * change, while its width eases to fit the new content.
 *
 * Variants: primary | secondary | cream | outline   (same looks as BaseButton)
 * Width:    content | full
 *
 * Usage:
 *   <DynamicButton :label="copied ? 'Copied' : 'Copy link'" :state-key="copied" @click="copy">
 *     <template #icon><CheckIcon v-if="copied" class="h-4 w-4" /><LinkIcon v-else class="h-4 w-4" /></template>
 *   </DynamicButton>
 *
 * The icon rolls whenever stateKey changes (it defaults to the label).
 */
import { computed, nextTick, onBeforeUnmount, onMounted, ref, useSlots, watch } from "vue";

const props = defineProps({
  label: { type: String, required: true },
  stateKey: { type: [String, Number, Boolean], default: undefined },
  variant: { type: String, default: "primary" },
  width: { type: String, default: "content" },
  type: { type: String, default: "button" },
});

const slots = useSlots();

const VARIANTS = {
  primary: "bg-chocolate text-cream-50 hover:bg-chocolate-300",
  secondary: "bg-primary text-chocolate hover:bg-primary-300",
  cream: "bg-chocolate/[0.07] text-chocolate hover:bg-chocolate/[0.12]",
  outline: "border-2 border-chocolate/25 bg-transparent text-chocolate hover:border-chocolate",
};

const button = ref(null);
const measure = ref(null);
const measuredWidth = ref(null);
let observer = null;

const iconKey = computed(() => String(props.stateKey ?? props.label));

const syncWidth = () => {
  if (props.width !== "content" || !button.value || !measure.value) return;
  const styles = getComputedStyle(button.value);
  const chrome =
    parseFloat(styles.paddingLeft) +
    parseFloat(styles.paddingRight) +
    parseFloat(styles.borderLeftWidth) +
    parseFloat(styles.borderRightWidth);
  measuredWidth.value = Math.ceil(measure.value.scrollWidth + chrome);
};

onMounted(() => {
  syncWidth();
  observer = new ResizeObserver(syncWidth);
  observer.observe(measure.value);
});

onBeforeUnmount(() => observer?.disconnect());

watch(
  () => [props.label, props.stateKey, props.variant, props.width],
  () => nextTick(syncWidth),
);

const style = computed(() =>
  props.width === "content" && measuredWidth.value ? { width: `${measuredWidth.value}px` } : undefined,
);
</script>

<template>
  <button
    ref="button"
    :type="type"
    class="relative inline-flex min-h-11 items-center justify-center overflow-hidden whitespace-nowrap rounded-full px-5 py-2.5 text-sm font-bold cursor-pointer select-none transition-[width,color,background-color,border-color,transform] duration-300 ease-cookie active:scale-[0.97] focus:outline-none focus-visible:ring-2 focus-visible:ring-chocolate focus-visible:ring-offset-2 focus-visible:ring-offset-background disabled:cursor-not-allowed disabled:opacity-50"
    :class="[VARIANTS[variant] ?? VARIANTS.primary, width === 'full' && 'w-full']"
    :style="style"
  >
    <span class="relative inline-flex items-center gap-2">
      <span v-if="slots.icon" class="relative inline-grid h-4 w-4 shrink-0 overflow-hidden">
        <Transition name="roll">
          <span :key="iconKey" class="col-start-1 row-start-1 flex h-4 w-4 items-center justify-center">
            <slot name="icon" />
          </span>
        </Transition>
      </span>

      <span class="relative inline-grid overflow-hidden">
        <Transition name="roll">
          <span :key="label" class="col-start-1 row-start-1 block">{{ label }}</span>
        </Transition>
      </span>
    </span>

    <span ref="measure" aria-hidden="true" class="pointer-events-none invisible absolute inline-flex items-center gap-2">
      <span v-if="slots.icon" class="flex h-4 w-4 shrink-0 items-center justify-center">
        <slot name="icon" />
      </span>
      <span>{{ label }}</span>
    </span>
  </button>
</template>

<style scoped>
.roll-enter-active,
.roll-leave-active {
  transition:
    opacity 0.18s cubic-bezier(0.23, 1, 0.32, 1),
    transform 0.18s cubic-bezier(0.23, 1, 0.32, 1);
}
.roll-enter-from {
  opacity: 0;
  transform: translateY(8px);
}
.roll-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}
</style>
