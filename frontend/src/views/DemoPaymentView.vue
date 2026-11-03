<script setup>
import { ref, onMounted } from "vue";
import { useRoute, useRouter, RouterLink } from "vue-router";
import { DEMO_MODE } from "../constants/business";
import { api, orderApi, errorMessage } from "../api/storeApi";
import { useFormat } from "../composables/useFormat";
import BaseButton from "../components/ui/BaseButton.vue";
const route = useRoute(), router = useRouter();
const order = ref(null), busy = ref(false), error = ref("");
const { rand } = useFormat();
const orderNumber = typeof route.query.order === "string" ? route.query.order : "";
const token = typeof route.query.token === "string" ? route.query.token : "";
onMounted(async () => {
  if (!DEMO_MODE || !orderNumber || !token) { error.value = "Start with a cookie in your cart to try the demo checkout."; return; }
  try { order.value = await orderApi.get(orderNumber, token); }
  catch (e) { error.value = errorMessage(e); }
});
async function complete() {
  if (busy.value) return;
  busy.value = true;
  error.value = "";
  try {
    await api("post", `/orders/${encodeURIComponent(orderNumber)}/demo-payment`, undefined, { params: { token } });
    await router.replace({ name: "Order", params: { orderNumber }, query: { token } });
  } catch (e) { error.value = errorMessage(e); }
  finally { busy.value = false; }
}
</script>
<template>
  <div class="pt-[var(--nav-h)]">
    <section class="container-magic max-w-[64rem] py-12 md:py-20">
      <h1 class="font-display text-display">Try a payment</h1>
      <p class="mt-5">This demo does not charge money. No card or bank details are needed.</p>
      <p v-if="error" role="alert" class="mt-6 text-red-800">{{ error }}</p>
      <div v-if="order" class="mt-8 border-y border-chocolate/15 py-6 space-y-3">
        <p class="font-bold">Order {{ order.orderNumber }}</p>
        <p class="tabular">Demo total: {{ rand(order.total) }}</p>
        <p>Complete the sample payment to see the order confirmation, stock update and captured email.</p>
        <BaseButton class="mt-5" block size="lg" :loading="busy" :disabled="busy || order.status !== 'PENDING_PAYMENT'" @click="complete">Simulate successful payment</BaseButton>
        <BaseButton :to="{ name: 'PaymentCancelled', query: { order: orderNumber, token } }" variant="outline" block :disabled="busy">Cancel and return to cart</BaseButton>
        <RouterLink :to="{ name: 'Order', params: { orderNumber }, query: { token } }" class="inline-flex min-h-11 items-center underline">View order</RouterLink>
      </div>
      <RouterLink v-else :to="{ name: 'Products' }" class="inline-flex min-h-11 items-center mt-6 underline">Shop demo cookies</RouterLink>
    </section>
  </div>
</template>
