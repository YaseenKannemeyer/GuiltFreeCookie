<script setup>
import { computed, onMounted, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import BaseButton from "../components/ui/BaseButton.vue";
import LowStockWidget from "../components/admin/LowStockWidget.vue";
import StatTile from "../components/admin/analytics/StatTile.vue";
import BarList from "../components/admin/analytics/BarList.vue";
import RevenueChart from "../components/admin/analytics/RevenueChart.vue";
import { analyticsApi } from "../api/analyticsApi";
import { errorMessage } from "../api/storeApi";
import { useFormat } from "../composables/useFormat";
import { SHIPPING_METHOD } from "../constants/orderStatus";

const PRESETS = [7, 30, 90];
const FLOW_NAMES = {
  WELCOME: "Welcome",
  BROWSE_ABANDONMENT: "Browsed, didn't add",
  CART_ABANDONMENT: "Left in the cart",
  POST_PURCHASE: "After delivery",
  REPLENISHMENT: "Time for a refill",
  WINBACK: "Win back",
  SUNSET: "Quiet subscribers",
};

const route = useRoute();
const router = useRouter();
const { rand } = useFormat();

const data = ref(null);
const loading = ref(false);
const error = ref("");

// The range lives in the URL, so a refresh or a shared link shows the same numbers.
const today = () => new Date().toLocaleDateString("en-CA", { timeZone: "Africa/Johannesburg" });
const daysAgo = (n) => {
  const d = new Date(`${today()}T12:00:00`);
  d.setDate(d.getDate() - n);
  return d.toLocaleDateString("en-CA");
};
const to = computed(() => (typeof route.query.to === "string" ? route.query.to : today()));
const from = computed(() => (typeof route.query.from === "string" ? route.query.from : daysAgo(29)));
const length = computed(() => Math.round((new Date(to.value) - new Date(from.value)) / 86_400_000) + 1);
const preset = computed(() => (to.value === today() ? PRESETS.find((n) => from.value === daysAgo(n - 1)) : null));
const period = computed(() => `vs the ${length.value} days before`);

function choose(n) {
  router.replace({ query: { from: daysAgo(n - 1), to: today() } });
}
function custom(field, value) {
  if (value) router.replace({ query: { from: from.value, to: to.value, [field]: value } });
}

async function load() {
  loading.value = true;
  error.value = "";
  try {
    data.value = await analyticsApi.summary(from.value, to.value);
  } catch (e) {
    error.value = errorMessage(e);
  } finally {
    loading.value = false;
  }
}
onMounted(load);
watch([from, to], load);

const n = (value) => Number(value || 0);
const percent = (part, whole) => (whole ? `${Math.round((100 * part) / whole)}%` : "–");

const topCookies = computed(() =>
  (data.value?.topCookies || []).map((cookie) => ({
    key: cookie.cookieId,
    label: cookie.name,
    value: n(cookie.revenue),
    display: rand(cookie.revenue),
    note: `${cookie.cookies} ${cookie.cookies === 1 ? "cookie" : "cookies"} in ${cookie.packs} ${cookie.packs === 1 ? "pack" : "packs"}`,
  })),
);
const funnel = computed(() => {
  const f = data.value?.funnel;
  if (!f) return [];
  return [
    { key: "carts", label: "Carts started", value: f.carts, display: String(f.carts) },
    { key: "email", label: "Gave an email", value: f.cartsWithEmail, display: String(f.cartsWithEmail), note: `${percent(f.cartsWithEmail, f.carts)} of carts` },
    { key: "placed", label: "Went to pay", value: f.ordersPlaced, display: String(f.ordersPlaced), note: `${percent(f.ordersPlaced, f.carts)} of carts` },
    { key: "paid", label: "Paid", value: f.ordersPaid, display: String(f.ordersPaid), note: `${percent(f.ordersPaid, f.ordersPlaced)} of those who went to pay` },
  ];
});
const delivery = computed(() => {
  const rows = data.value?.delivery || [];
  const total = rows.reduce((sum, row) => sum + row.orders, 0);
  return rows
    .map((row) => ({
      key: row.method,
      label: SHIPPING_METHOD[row.method] || row.method,
      value: row.orders,
      display: `${row.orders} · ${percent(row.orders, total)}`,
    }))
    .sort((a, b) => b.value - a.value);
});
const flows = computed(() => (data.value?.emailFlows || []).filter((flow) => flow.sent > 0 || flow.orders > 0));
</script>

<template>
  <div class="p-5 md:p-10 max-w-5xl mx-auto">
    <h1 class="font-display text-display">Overview</h1>

    <div class="mt-6 flex flex-wrap items-end gap-3" role="group" aria-label="Period">
      <div class="flex rounded-full bg-chocolate/[0.07] p-1">
        <button
          v-for="days in PRESETS"
          :key="days"
          type="button"
          class="min-h-11 rounded-full px-4 font-bold"
          :class="preset === days ? 'bg-chocolate text-cream-50' : 'hover:bg-chocolate/10'"
          :aria-pressed="preset === days"
          @click="choose(days)"
        >
          {{ days }} days
        </button>
      </div>
      <label class="grid min-w-0">
        <span class="text-sm font-bold">From</span>
        <input type="date" class="field min-w-0" :value="from" :max="to" @change="custom('from', $event.target.value)" />
      </label>
      <label class="grid min-w-0">
        <span class="text-sm font-bold">To</span>
        <input type="date" class="field min-w-0" :value="to" :min="from" @change="custom('to', $event.target.value)" />
      </label>
    </div>

    <p v-if="error" role="alert" class="mt-6 rounded-xl bg-red-800/[0.08] px-4 py-3 text-red-900">
      {{ error }} <button type="button" class="font-bold underline" @click="load">Try again</button>
    </p>

    <div v-if="!data && !error" class="mt-8 space-y-4" aria-label="Loading the overview">
      <div class="h-32 rounded-2xl shimmer" />
      <div class="h-64 rounded-2xl shimmer" />
    </div>

    <div v-else-if="data" :class="['transition-opacity', loading && 'opacity-60']" :aria-busy="loading">
      <section class="mt-8 grid gap-6 rounded-2xl bg-cream-100 p-5 md:p-7 sm:grid-cols-2 lg:grid-cols-[1.4fr_1fr_1fr_1fr]" aria-label="Headline numbers">
        <StatTile hero label="Revenue" :value="rand(data.current.revenue)" :current="n(data.current.revenue)" :previous="n(data.previous.revenue)" :period="period" />
        <StatTile label="Paid orders" :value="String(data.current.orders)" :current="data.current.orders" :previous="data.previous.orders" :period="period" />
        <StatTile label="Average order" :value="rand(data.current.averageOrderValue)" :current="n(data.current.averageOrderValue)" :previous="n(data.previous.averageOrderValue)" :period="period" />
        <StatTile label="Cookies sold" :value="String(data.current.cookiesSold)" :current="data.current.cookiesSold" :previous="data.previous.cookiesSold" :period="period" />
      </section>
      <p class="mt-3 text-sm text-chocolate/75">
        Paid orders only, in Cape Town time. Discounts given: {{ rand(data.current.discounts) }}.
        <template v-if="data.current.refunds"> {{ data.current.refunds }} refunded {{ data.current.refunds === 1 ? "order isn't" : "orders aren't" }} counted.</template>
      </p>

      <section class="mt-8 rounded-2xl bg-cream-100 p-5 md:p-7" aria-labelledby="revenue-heading">
        <h2 id="revenue-heading" class="text-[2rem] font-extrabold">Revenue by day</h2>
        <div class="mt-4"><RevenueChart :days="data.days" /></div>
      </section>

      <div class="mt-8 grid gap-8 md:grid-cols-2">
        <section class="rounded-2xl bg-cream-100 p-5 md:p-7 min-w-0" aria-labelledby="top-heading">
          <h2 id="top-heading" class="text-[2rem] font-extrabold">Best sellers</h2>
          <div class="mt-5"><BarList :rows="topCookies" empty="No cookies sold in this period yet." /></div>
        </section>
        <section class="rounded-2xl bg-cream-100 p-5 md:p-7 min-w-0" aria-labelledby="funnel-heading">
          <h2 id="funnel-heading" class="text-[2rem] font-extrabold">Checkout</h2>
          <p class="mt-1 text-sm text-chocolate/75">Carts and orders started in this period.</p>
          <div class="mt-5"><BarList :rows="funnel" empty="No carts in this period yet." /></div>
        </section>
        <section class="rounded-2xl bg-cream-100 p-5 md:p-7 min-w-0" aria-labelledby="customers-heading">
          <h2 id="customers-heading" class="text-[2rem] font-extrabold">Customers</h2>
          <div class="mt-5 grid grid-cols-2 gap-6">
            <StatTile label="New" :value="String(data.customers.newCustomers)" />
            <StatTile label="Returning" :value="String(data.customers.returningCustomers)" />
          </div>
          <p class="mt-4 text-sm text-chocolate/75">Returning means they'd paid for an order before this period.</p>
        </section>
        <section class="rounded-2xl bg-cream-100 p-5 md:p-7 min-w-0" aria-labelledby="delivery-heading">
          <h2 id="delivery-heading" class="text-[2rem] font-extrabold">Delivery</h2>
          <div class="mt-5"><BarList :rows="delivery" empty="No paid orders in this period yet." /></div>
        </section>
      </div>

      <section class="mt-8 rounded-2xl bg-cream-100 p-5 md:p-7" aria-labelledby="flows-heading">
        <div class="flex flex-wrap items-baseline justify-between gap-3">
          <h2 id="flows-heading" class="text-[2rem] font-extrabold">Email flows</h2>
          <RouterLink :to="{ name: 'AdminEmailFlows' }" class="font-bold underline min-h-11 inline-flex items-center">Flow settings</RouterLink>
        </div>
        <p v-if="!flows.length" class="mt-3 text-chocolate/75">No flow emails went out in this period.</p>
        <div v-else class="mt-4 overflow-x-auto -mx-5 px-5 md:mx-0 md:px-0">
          <table class="w-full min-w-[56rem] text-left">
            <thead>
              <tr class="text-sm text-chocolate/70">
                <th scope="col" class="py-2 pr-4 font-bold">Flow</th>
                <th scope="col" class="py-2 pr-4 font-bold text-right">Sent</th>
                <th scope="col" class="py-2 pr-4 font-bold text-right">Opened</th>
                <th scope="col" class="py-2 pr-4 font-bold text-right">Clicked</th>
                <th scope="col" class="py-2 pr-4 font-bold text-right">Orders</th>
                <th scope="col" class="py-2 font-bold text-right">Revenue</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="flow in flows" :key="flow.flow" class="border-t border-chocolate/10">
                <th scope="row" class="py-2.5 pr-4 font-bold">{{ FLOW_NAMES[flow.flow] }}</th>
                <td class="py-2.5 pr-4 text-right tabular">{{ flow.sent }}</td>
                <td class="py-2.5 pr-4 text-right tabular">{{ flow.opened }} <span class="text-chocolate/60">· {{ percent(flow.opened, flow.sent) }}</span></td>
                <td class="py-2.5 pr-4 text-right tabular">{{ flow.clicked }} <span class="text-chocolate/60">· {{ percent(flow.clicked, flow.sent) }}</span></td>
                <td class="py-2.5 pr-4 text-right tabular">{{ flow.orders }}</td>
                <td class="py-2.5 text-right tabular">{{ rand(flow.revenue) }}</td>
              </tr>
            </tbody>
          </table>
        </div>
        <p class="mt-4 text-sm text-chocolate/75">
          Orders and revenue count paid orders placed within 5 days of a click on that flow's email. Opens are a rough guide.
        </p>
      </section>

      <div class="mt-8"><LowStockWidget /></div>
      <BaseButton :to="{ name: 'AdminOrders' }" variant="outline" class="mt-2">See all orders</BaseButton>
    </div>
  </div>
</template>
