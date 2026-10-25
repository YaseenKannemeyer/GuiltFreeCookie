<script setup>
import { ref, onMounted } from "vue";
import { accountApi } from "../../api/accountApi";
import { errorMessage } from "../../api/storeApi";
import cookieApi from "../../api/cookieApi";
import { ORDER_STATUS, SHIPPING_METHOD } from "../../constants/orderStatus";
import { useFormat } from "../../composables/useFormat";
import BaseButton from "../ui/BaseButton.vue";
const { rand } = useFormat();
const orders = ref([]),
  nextPage = ref(0),
  totalPages = ref(0),
  loading = ref(false),
  error = ref("");
const claimCount = ref(0),
  claimError = ref(""),
  sending = ref(false),
  sent = ref(false);
async function load(reset = false) {
  if (loading.value) return;
  loading.value = true;
  error.value = "";
  try {
    const result = await accountApi.orders(reset ? 0 : nextPage.value);
    orders.value = reset ? result.items : [...orders.value, ...result.items];
    nextPage.value = result.page + 1;
    totalPages.value = result.totalPages;
  } catch (e) {
    error.value = errorMessage(e);
  } finally {
    loading.value = false;
  }
}
async function loadClaimable() {
  try {
    claimCount.value = (await accountApi.claimable()).count;
    claimError.value = "";
  } catch (e) {
    claimError.value = errorMessage(e);
  }
}
async function requestLink() {
  sending.value = true;
  claimError.value = "";
  try {
    await accountApi.requestClaim();
    sent.value = true;
  } catch (e) {
    claimError.value = errorMessage(e);
  } finally {
    sending.value = false;
  }
}
onMounted(() => {
  load(true);
  loadClaimable();
});
</script>
<template>
  <section id="orders" class="scroll-mt-32" aria-labelledby="orders-heading">
    <h2 id="orders-heading" class="text-2xl font-extrabold">Orders</h2>
    <div v-if="claimCount > 0" class="my-5 rounded-xl bg-primary/20 p-5">
      <p v-if="!sent">
        We found {{ claimCount }} earlier
        {{ claimCount === 1 ? "order" : "orders" }} placed with this email.
      </p>
      <p v-else role="status">Check your inbox. The link works for 24 hours.</p>
      <BaseButton
        v-if="!sent"
        class="mt-3"
        :disabled="sending"
        @click="requestLink"
        >{{ sending ? "Sending…" : "Send me a link" }}</BaseButton
      >
    </div>
    <p v-if="claimError" role="alert" class="mt-4 text-red-800">
      {{ claimError }}
      <button class="underline min-h-11" @click="loadClaimable">
        Check earlier orders again
      </button>
    </p>
    <p v-if="error" role="alert" class="mt-5 text-red-800">
      {{ error }}
      <button class="underline min-h-11" @click="load(!orders.length)">
        Try again
      </button>
    </p>
    <p v-if="loading" role="status" class="py-6">Loading your orders…</p>
    <div v-if="!loading && !error && !orders.length" class="py-7">
      <p>Your orders will appear here once you place one.</p>
      <RouterLink to="/products" class="btn-outline mt-4"
        >Browse cookies</RouterLink
      >
    </div>
    <ul v-if="orders.length" class="mt-4 divide-y divide-chocolate/15">
      <li v-for="order in orders" :key="order.orderNumber">
        <RouterLink
          :to="{
            name: 'Order',
            params: { orderNumber: order.orderNumber },
            query: { token: order.token },
          }"
          class="grid grid-cols-[64px_minmax(0,1fr)] sm:grid-cols-[80px_minmax(0,1fr)_auto] items-center gap-4 py-5 rounded-lg focus-visible:outline-offset-4"
          ><img
            v-if="order.image"
            :src="cookieApi.getImageUrl(order.image)"
            alt=""
            width="80"
            height="90"
            class="w-full h-20 object-contain bg-tan/30 rounded-xl"
            @error="$event.target.style.visibility = 'hidden'"
          /><span
            v-else
            class="w-full h-20 bg-tan/30 rounded-xl flex items-center justify-center text-sm"
            aria-hidden="true"
            >Order</span
          >
          <div class="min-w-0">
            <p class="font-bold break-words">{{ order.orderNumber }}</p>
            <p class="text-sm mt-1">
              {{
                new Date(order.createdAt).toLocaleDateString("en-ZA", {
                  day: "numeric",
                  month: "short",
                  year: "numeric",
                })
              }}
              · {{ SHIPPING_METHOD[order.shippingMethod] }}
            </p>
            <span
              class="inline-block rounded-full px-3 py-1 text-sm mt-2"
              :class="ORDER_STATUS[order.status]?.badge"
              >{{ ORDER_STATUS[order.status]?.label || order.status }}</span
            >
            <p class="mt-2 font-bold sm:hidden">{{ rand(order.total) }}</p>
          </div>
          <p class="hidden sm:block font-bold tabular">
            {{ rand(order.total) }}
          </p></RouterLink
        >
      </li>
    </ul>
    <BaseButton
      v-if="nextPage < totalPages"
      variant="outline"
      :disabled="loading"
      class="mt-5"
      @click="load()"
      >Load more</BaseButton
    >
  </section>
</template>
