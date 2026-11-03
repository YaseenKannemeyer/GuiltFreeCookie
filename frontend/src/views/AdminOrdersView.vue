<script setup>
import { computed, onMounted, reactive, ref, watch } from "vue";
import { RouterLink, useRoute, useRouter } from "vue-router";
import { MagnifyingGlassIcon } from "@heroicons/vue/20/solid";
import BaseButton from "../components/ui/BaseButton.vue";
import { adminOrderApi, errorMessage } from "../api/storeApi";
import { useFormat } from "../composables/useFormat";
import { ORDER_STATUS, SHIPPING_METHOD } from "../constants/orderStatus";

const route = useRoute();
const router = useRouter();
const { rand } = useFormat();

// Filters live in the URL so a view can be shared or bookmarked ("all orders to ship").
const filters = reactive({
  q: route.query.q || "",
  status: route.query.status || "",
  from: route.query.from || "",
  to: route.query.to || "",
  page: Number(route.query.page) || 0,
});
const result = ref(null);
const loading = ref(true);
const error = ref("");
const filtered = computed(() => !!(filters.q || filters.status || filters.from || filters.to));

const placed = (value) =>
  new Intl.DateTimeFormat("en-ZA", { day: "numeric", month: "short", hour: "2-digit", minute: "2-digit" }).format(new Date(value));

let debounce;
async function search() {
  loading.value = true;
  error.value = "";
  const query = Object.fromEntries(Object.entries(filters).filter(([, v]) => v !== "" && v !== 0));
  router.replace({ query });
  try {
    result.value = await adminOrderApi.search({ ...query, size: 20 });
  } catch (e) {
    error.value = errorMessage(e);
  } finally {
    loading.value = false;
  }
}
watch(
  () => [filters.status, filters.from, filters.to],
  () => {
    filters.page = 0;
    search();
  },
);
watch(
  () => filters.q,
  () => {
    clearTimeout(debounce);
    debounce = setTimeout(() => {
      filters.page = 0;
      search();
    }, 300);
  },
);
const goPage = (page) => {
  filters.page = page;
  search();
};
const clear = () => Object.assign(filters, { q: "", status: "", from: "", to: "", page: 0 });
const open = (order) => router.push({ name: "AdminOrderDetail", params: { orderNumber: order.orderNumber } });

onMounted(search);
</script>

<template>
  <div class="p-5 md:p-10 max-w-6xl mx-auto">
    <div class="flex flex-wrap items-end justify-between gap-4">
      <h1 class="font-display text-display">Orders</h1>
      <RouterLink :to="{ name: 'AdminShipping' }" class="font-bold underline underline-offset-4 min-h-11 inline-flex items-center"
        >Delivery settings</RouterLink
      >
    </div>

    <form class="mt-7 grid grid-cols-2 gap-3 md:grid-cols-[1fr_20rem_16rem_16rem]" role="search" @submit.prevent="search">
      <label class="relative col-span-2 md:col-span-1">
        <span class="sr-only">Search orders</span>
        <MagnifyingGlassIcon class="absolute left-4 top-1/2 -translate-y-1/2 h-5 w-5 text-chocolate/60" aria-hidden="true" />
        <input v-model="filters.q" type="search" class="field pl-11" placeholder="Order, email or surname" />
      </label>
      <label class="col-span-2 md:col-span-1">
        <span class="sr-only">Status</span>
        <select v-model="filters.status" class="field">
          <option value="">All statuses</option>
          <option v-for="(meta, value) in ORDER_STATUS" :key="value" :value="value">{{ meta.label }}</option>
        </select>
      </label>
      <label class="grid min-w-0 col-span-2 sm:col-span-1">
        <span class="text-sm font-bold">From</span>
        <input v-model="filters.from" type="date" class="field min-w-0 w-full" />
      </label>
      <label class="grid min-w-0 col-span-2 sm:col-span-1">
        <span class="text-sm font-bold">To</span>
        <input v-model="filters.to" type="date" class="field min-w-0 w-full" />
      </label>
    </form>

    <p v-if="error" role="alert" class="mt-6 text-red-900">
      {{ error }} <button type="button" class="font-bold underline" @click="search">Try again</button>
    </p>

    <div v-if="loading && !result" class="mt-8 space-y-3" aria-label="Loading orders">
      <div v-for="n in 5" :key="n" class="h-14 rounded-xl shimmer" />
    </div>

    <section v-else-if="result && !result.items.length" class="mt-12 max-w-[44ch]">
      <h2 class="font-bold">{{ filtered ? "No orders match" : "No orders yet" }}</h2>
      <p class="mt-2 text-chocolate/80">
        {{
          filtered
            ? "Try another search, status or date range."
            : "Orders appear here the moment someone checks out, including ones still waiting for PayFast."
        }}
      </p>
      <BaseButton v-if="filtered" variant="outline" class="mt-5" @click="clear">Clear filters</BaseButton>
    </section>

    <template v-else-if="result">
      <table class="mt-8 w-full text-left hidden md:table" :aria-busy="loading">
        <thead class="text-sm text-chocolate/70 border-b border-chocolate/20">
          <tr>
            <th scope="col" class="py-3 pr-4 font-extrabold">Order</th>
            <th scope="col" class="py-3 pr-4 font-extrabold">Placed</th>
            <th scope="col" class="py-3 pr-4 font-extrabold">Customer</th>
            <th scope="col" class="py-3 pr-4 font-extrabold">Delivery</th>
            <th scope="col" class="py-3 pr-4 font-extrabold text-right">Total</th>
            <th scope="col" class="py-3 font-extrabold">Status</th>
          </tr>
        </thead>
        <tbody class="divide-y divide-chocolate/[0.1]">
          <tr
            v-for="order in result.items"
            :key="order.orderNumber"
            class="cursor-pointer hover:bg-chocolate/[0.04] transition-colors"
            @click="open(order)"
          >
            <td class="py-3.5 pr-4">
              <RouterLink
                :to="{ name: 'AdminOrderDetail', params: { orderNumber: order.orderNumber } }"
                class="font-bold tabular hover:underline"
                @click.stop
                >{{ order.orderNumber }}</RouterLink
              >
            </td>
            <td class="py-3.5 pr-4 tabular text-chocolate/80">{{ placed(order.createdAt) }}</td>
            <td class="py-3.5 pr-4">
              {{ order.customerName }}<span class="block text-sm text-chocolate/70">{{ order.email }}</span>
            </td>
            <td class="py-3.5 pr-4">{{ SHIPPING_METHOD[order.shippingMethod] }} · {{ order.itemCount }} {{ order.itemCount === 1 ? "item" : "items" }}</td>
            <td class="py-3.5 pr-4 text-right tabular font-bold">{{ rand(order.total) }}</td>
            <td class="py-3.5">
              <span :class="['inline-flex rounded-full px-3 py-1 text-sm font-extrabold whitespace-nowrap', ORDER_STATUS[order.status]?.badge]">{{
                ORDER_STATUS[order.status]?.label
              }}</span>
            </td>
          </tr>
        </tbody>
      </table>

      <ul class="mt-6 md:hidden divide-y divide-chocolate/[0.12] border-y border-chocolate/[0.12]" :aria-busy="loading">
        <li v-for="order in result.items" :key="order.orderNumber">
          <RouterLink
            :to="{ name: 'AdminOrderDetail', params: { orderNumber: order.orderNumber } }"
            class="flex items-start justify-between gap-3 py-4"
          >
            <span class="min-w-0">
              <span class="font-bold tabular">{{ order.orderNumber }}</span>
              <span class="block truncate">{{ order.customerName }}</span>
              <span class="block text-sm text-chocolate/70">{{ placed(order.createdAt) }} · {{ SHIPPING_METHOD[order.shippingMethod] }}</span>
            </span>
            <span class="text-right shrink-0">
              <span class="block font-bold tabular">{{ rand(order.total) }}</span>
              <span :class="['mt-1 inline-flex rounded-full px-2.5 py-0.5 text-sm font-extrabold', ORDER_STATUS[order.status]?.badge]">{{
                ORDER_STATUS[order.status]?.label
              }}</span>
            </span>
          </RouterLink>
        </li>
      </ul>

      <nav v-if="result.totalPages > 1" class="mt-6 flex items-center justify-between gap-4" aria-label="Pages">
        <BaseButton variant="outline" :disabled="filters.page === 0 || loading" @click="goPage(filters.page - 1)">Newer</BaseButton>
        <span class="text-sm tabular">Page {{ result.page + 1 }} of {{ result.totalPages }}</span>
        <BaseButton variant="outline" :disabled="filters.page + 1 >= result.totalPages || loading" @click="goPage(filters.page + 1)"
          >Older</BaseButton
        >
      </nav>
    </template>
  </div>
</template>
