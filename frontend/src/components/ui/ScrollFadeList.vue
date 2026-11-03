<script setup>
/**
 * ScrollFadeList — scrollable list whose top and bottom edges fade out
 * while there's more to scroll in that direction.
 *
 * Usage:
 *   <ScrollFadeList :items="orders" item-key="orderId">
 *     <template #item="{ item }">{{ item.reference }}</template>
 *   </ScrollFadeList>
 *
 * itemKey is a property name or a function (item) => key. The list is 320px
 * tall; pass scroll-class to change that. The fades blend into the page
 * background; set --scroll-fade-bg on the component when it sits on another
 * colour.
 */
import { onBeforeUnmount, onMounted, ref, watch } from "vue";

const props = defineProps({
  items: { type: Array, required: true },
  itemKey: { type: [String, Function], required: true },
  maxFadeHeight: { type: Number, default: 76 },
  scrollClass: { type: String, default: "h-80" },
});

const frame = ref(null);
const scroller = ref(null);
const list = ref(null);
let animationFrame = 0;
let resizeObserver = null;

const keyOf = (item) => (typeof props.itemKey === "function" ? props.itemKey(item) : item[props.itemKey]);

const updateFades = () => {
  const el = scroller.value;
  if (!el || !frame.value) return;
  const maxScrollTop = Math.max(0, el.scrollHeight - el.clientHeight);
  const clamp = (distance) => `${Math.min(props.maxFadeHeight, Math.max(0, distance))}px`;
  frame.value.style.setProperty("--top-fade-height", clamp(el.scrollTop));
  frame.value.style.setProperty("--bottom-fade-height", clamp(maxScrollTop - el.scrollTop));
};

const scheduleUpdate = () => {
  cancelAnimationFrame(animationFrame);
  animationFrame = requestAnimationFrame(updateFades);
};

onMounted(() => {
  updateFades();
  scroller.value.addEventListener("scroll", scheduleUpdate, { passive: true });
  resizeObserver = new ResizeObserver(scheduleUpdate);
  resizeObserver.observe(scroller.value);
  resizeObserver.observe(list.value);
});

onBeforeUnmount(() => {
  cancelAnimationFrame(animationFrame);
  scroller.value?.removeEventListener("scroll", scheduleUpdate);
  resizeObserver?.disconnect();
});

watch(() => props.maxFadeHeight, scheduleUpdate);
</script>

<template>
  <div ref="frame" class="scroll-fade relative overflow-hidden rounded-2xl border border-chocolate/10 bg-surface">
    <div :class="scrollClass" class="overflow-y-auto overscroll-contain [scrollbar-gutter:stable]" ref="scroller">
      <ul ref="list" class="p-2">
        <li v-for="item in items" :key="keyOf(item)" class="rounded-xl px-3 py-2">
          <slot name="item" :item="item" />
        </li>
      </ul>
    </div>
  </div>
</template>

<style scoped>
.scroll-fade {
  --scroll-fade-bg: var(--color-background);
  --scrollbar-width: 10px;
  --top-fade-height: 0px;
  --bottom-fade-height: 0px;
}
.scroll-fade::before,
.scroll-fade::after {
  content: "";
  position: absolute;
  left: 0;
  right: var(--scrollbar-width);
  pointer-events: none;
}
.scroll-fade::before {
  top: 0;
  height: var(--top-fade-height);
  background: linear-gradient(to bottom, var(--scroll-fade-bg), transparent);
}
.scroll-fade::after {
  bottom: 0;
  height: var(--bottom-fade-height);
  background: linear-gradient(to top, var(--scroll-fade-bg), transparent);
}
</style>
