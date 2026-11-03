<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from "vue";
import { useFormat } from "../../../composables/useFormat";

// Paid revenue per shop day: one line with a light wash, a crosshair that snaps to the nearest day
// (pointer or arrow keys), and the same numbers as a table underneath.
const props = defineProps({
  days: { type: Array, required: true }, // [{ date, revenue, orders }]
});

const { rand } = useFormat();
const HEIGHT = 200;
const AXIS = 28;
const LEFT = 52;
const RIGHT = 12;
const TOP = 12;
const INK = "#9A6E45";

const box = ref(null);
const width = ref(640);
const active = ref(null);
let observer;

onMounted(() => {
  observer = new ResizeObserver(([entry]) => (width.value = Math.max(280, entry.contentRect.width)));
  observer.observe(box.value);
});
onBeforeUnmount(() => observer?.disconnect());

const values = computed(() => props.days.map((day) => Number(day.revenue)));
const top = computed(() => niceMax(Math.max(0, ...values.value)));
const plotWidth = computed(() => width.value - LEFT - RIGHT);
const x = (i) => LEFT + (props.days.length < 2 ? plotWidth.value / 2 : (i * plotWidth.value) / (props.days.length - 1));
const y = (value) => TOP + HEIGHT - (top.value ? (value / top.value) * HEIGHT : 0);

const line = computed(() => values.value.map((v, i) => `${i ? "L" : "M"}${x(i).toFixed(1)},${y(v).toFixed(1)}`).join(""));
const area = computed(() =>
  props.days.length
    ? `${line.value}L${x(props.days.length - 1).toFixed(1)},${TOP + HEIGHT}L${x(0).toFixed(1)},${TOP + HEIGHT}Z`
    : "",
);
const ticks = computed(() => [0, top.value / 2, top.value]);
const labels = computed(() => {
  const n = props.days.length;
  if (!n) return [];
  const picks = n <= 7 ? [...Array(n).keys()] : [0, Math.floor((n - 1) / 2), n - 1];
  return picks.map((i) => ({ i, text: shortDate(props.days[i].date) }));
});
const peak = computed(() => {
  const max = Math.max(...values.value);
  return max > 0 ? values.value.indexOf(max) : null;
});
const quiet = computed(() => values.value.every((v) => v === 0));

function niceMax(max) {
  if (max <= 0) return 100;
  const step = 10 ** Math.floor(Math.log10(max));
  return Math.ceil(max / step / 0.5) * 0.5 * step;
}
function compact(value) {
  return value >= 1000 ? `R${(value / 1000).toFixed(value % 1000 ? 1 : 0)}K` : `R${Math.round(value)}`;
}
function shortDate(iso) {
  return new Date(`${iso}T12:00:00`).toLocaleDateString("en-ZA", { day: "numeric", month: "short" });
}
function point(event) {
  const rect = event.currentTarget.getBoundingClientRect();
  const px = ((event.clientX - rect.left) / rect.width) * width.value;
  const n = props.days.length;
  if (!n) return;
  active.value = n < 2 ? 0 : Math.min(n - 1, Math.max(0, Math.round(((px - LEFT) / plotWidth.value) * (n - 1))));
}
function key(event) {
  const n = props.days.length;
  if (!n) return;
  const moves = { ArrowRight: 1, ArrowLeft: -1, Home: -n, End: n };
  if (!(event.key in moves)) return;
  event.preventDefault();
  active.value = Math.min(n - 1, Math.max(0, (active.value ?? n - 1) + moves[event.key]));
}
const tip = computed(() => {
  if (active.value == null) return null;
  const day = props.days[active.value];
  const left = Math.min(Math.max(x(active.value), 80), width.value - 80);
  return { day, left, top: y(Number(day.revenue)) };
});
</script>

<template>
  <div ref="box" class="relative">
    <svg
      :width="width"
      :height="TOP + HEIGHT + AXIS"
      class="block touch-pan-y outline-none focus-visible:ring-2 focus-visible:ring-chocolate rounded"
      role="img"
      tabindex="0"
      :aria-label="`Revenue by day. Use the arrow keys to read each day.`"
      @pointermove="point"
      @pointerleave="active = null"
      @focus="active = active ?? days.length - 1"
      @blur="active = null"
      @keydown="key"
    >
      <g v-for="tick in ticks" :key="tick">
        <line :x1="LEFT" :x2="width - RIGHT" :y1="y(tick)" :y2="y(tick)" stroke="#2E0E0B" stroke-opacity="0.1" stroke-width="1" />
        <text :x="LEFT - 8" :y="y(tick) + 4" text-anchor="end" class="fill-chocolate/70 text-[1.2rem] tabular">{{ compact(tick) }}</text>
      </g>
      <text
        v-for="label in labels"
        :key="label.i"
        :x="x(label.i)"
        :y="TOP + HEIGHT + 20"
        :text-anchor="label.i === 0 ? 'start' : label.i === days.length - 1 ? 'end' : 'middle'"
        class="fill-chocolate/70 text-[1.2rem]"
      >
        {{ label.text }}
      </text>
      <path :d="area" :fill="INK" fill-opacity="0.1" />
      <path :d="line" fill="none" :stroke="INK" stroke-width="2" stroke-linejoin="round" stroke-linecap="round" />
      <g v-if="peak != null && active == null">
        <circle :cx="x(peak)" :cy="y(values[peak])" r="4" :fill="INK" stroke="#F5F0E6" stroke-width="2" />
        <text
          :x="x(peak)"
          :y="y(values[peak]) - 10"
          :text-anchor="peak === 0 ? 'start' : peak === days.length - 1 ? 'end' : 'middle'"
          class="fill-chocolate text-[1.2rem] font-bold"
        >
          {{ rand(values[peak]) }}
        </text>
      </g>
      <g v-if="tip">
        <line :x1="x(active)" :x2="x(active)" :y1="TOP" :y2="TOP + HEIGHT" stroke="#2E0E0B" stroke-opacity="0.35" stroke-width="1" />
        <circle :cx="x(active)" :cy="tip.top" r="4" :fill="INK" stroke="#F5F0E6" stroke-width="2" />
      </g>
    </svg>
    <div
      v-if="tip"
      class="pointer-events-none absolute -translate-x-1/2 rounded-xl bg-chocolate px-3 py-2 text-cream-50 shadow-lg"
      :style="{ left: `${tip.left}px`, top: '0px' }"
      role="status"
    >
      <p class="font-extrabold tabular">{{ rand(tip.day.revenue) }}</p>
      <p class="text-sm opacity-80 whitespace-nowrap">
        {{ shortDate(tip.day.date) }} · {{ tip.day.orders }} {{ tip.day.orders === 1 ? "order" : "orders" }}
      </p>
    </div>
    <p v-if="quiet" class="mt-2 text-chocolate/75">No paid orders in this period yet.</p>
    <details class="mt-3">
      <summary class="cursor-pointer font-bold min-h-11 inline-flex items-center">Show as a table</summary>
      <table class="mt-2 w-full text-sm">
        <thead>
          <tr class="text-left text-chocolate/70">
            <th scope="col" class="py-1.5 font-bold">Day</th>
            <th scope="col" class="py-1.5 font-bold text-right">Orders</th>
            <th scope="col" class="py-1.5 font-bold text-right">Revenue</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="day in days" :key="day.date" class="border-t border-chocolate/10">
            <td class="py-1.5">{{ shortDate(day.date) }}</td>
            <td class="py-1.5 text-right tabular">{{ day.orders }}</td>
            <td class="py-1.5 text-right tabular">{{ rand(day.revenue) }}</td>
          </tr>
        </tbody>
      </table>
    </details>
  </div>
</template>
