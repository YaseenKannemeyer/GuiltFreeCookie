<script setup>
import { onMounted, ref } from "vue";
import { RouterLink, useRoute, useRouter } from "vue-router";
import BaseButton from "../components/ui/BaseButton.vue";
import { useCart } from "../composables/useCart";
import { errorMessage, orderApi } from "../api/storeApi";
import { DEMO_MODE } from "../constants/business";

const route = useRoute();
const router = useRouter();
const { restore } = useCart();

const state = ref("working");
const error = ref("");

// PayFast sends the shopper here after "Cancel": free the held stock and reopen their cart.
onMounted(async () => {
  const { order, token } = route.query;
  if (!order || !token) {
    state.value = "unknown";
    return;
  }
  try {
    const cancelled = await orderApi.cancel(order, token);
    await restore(cancelled?.cartId).catch(() => {});
    state.value = "cancelled";
  } catch (e) {
    if (e?.response?.status === 409) {
      router.replace({ name: "Order", params: { orderNumber: order }, query: { token } });
      return;
    }
    error.value = errorMessage(e);
    state.value = "error";
  }
});
</script>

<template>
  <div class="pt-[var(--nav-h)]">
    <div class="container-magic max-w-[64rem] pt-12 pb-24">
      <p v-if="state === 'working'" role="status">Cancelling your payment…</p>

      <section v-else-if="state === 'cancelled'">
        <h1 class="font-display text-display">Payment cancelled</h1>
        <p class="mt-4 text-chocolate/80 max-w-[40ch]">Nothing was charged, and your cookies are back in your cart whenever you're ready.</p>
        <div class="mt-8 flex flex-wrap gap-3">
          <BaseButton :to="{ name: 'Cart' }" size="lg">Back to my cart</BaseButton>
          <BaseButton :to="{ name: 'Products' }" variant="outline" size="lg">Keep browsing</BaseButton>
        </div>
      </section>

      <section v-else>
        <h1 class="font-display text-display">Payment not completed</h1>
        <p class="mt-4 text-chocolate/80 max-w-[44ch]">
          {{ error || "We couldn't tell which order this was." }} {{ DEMO_MODE ? 'No money is charged in this demo.' : 'Nothing is charged until PayFast confirms a payment.' }}
        </p>
        <div class="mt-8 flex flex-wrap gap-3">
          <BaseButton :to="{ name: 'Cart' }" size="lg">Go to my cart</BaseButton>
          <RouterLink :to="{ name: 'Contact' }" class="btn-outline">Contact us</RouterLink>
        </div>
      </section>
    </div>
  </div>
</template>
