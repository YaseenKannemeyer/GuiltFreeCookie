<script setup>
import { onMounted, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import BaseButton from "../components/ui/BaseButton.vue";
import { emailLinkApi } from "../api/flowApi";
import { errorMessage } from "../api/storeApi";
import { useCart } from "../composables/useCart";
import { isCustomerLoggedIn } from "../composables/useCustomerAuth";

// Landing page for "Back to my cart" (recovery) and "Reorder" links in emails: fills the cart, then opens it.
const route = useRoute();
const router = useRouter();
const { adopt, merge, cart } = useCart();
const reordering = route.name === "Reorder";
const error = ref("");

// The reorder lands in the shopper's current cart rather than replacing it.
function storedCartId() {
  try {
    return localStorage.getItem("gfc_cart_id");
  } catch {
    return null;
  }
}

onMounted(async () => {
  try {
    const view = reordering
      ? await emailLinkApi.reorder(route.params.orderNumber, route.query.token, cart.value?.cartId || storedCartId())
      : await emailLinkApi.recoverCart(route.params.token);
    adopt(view);
    if (isCustomerLoggedIn()) await merge().catch(() => {});
    router.replace({ name: "Cart" });
  } catch (e) {
    error.value = errorMessage(e);
  }
});
</script>

<template>
  <div class="pt-[var(--nav-h)]">
    <div class="container-magic max-w-[64rem] pt-12 pb-24">
      <p v-if="!error" role="status">{{ reordering ? "Filling your cart…" : "Finding your cart…" }}</p>
      <section v-else>
        <h1 class="font-display text-display">{{ reordering ? "We couldn't refill your cart" : "That cart has moved on" }}</h1>
        <p class="mt-4 text-chocolate/80 max-w-[44ch]">{{ error }}</p>
        <div class="mt-8 flex flex-wrap gap-3">
          <BaseButton :to="{ name: 'Products' }" size="lg">Browse cookies</BaseButton>
          <BaseButton :to="{ name: 'Cart' }" variant="outline" size="lg">Go to my cart</BaseButton>
        </div>
      </section>
    </div>
  </div>
</template>
