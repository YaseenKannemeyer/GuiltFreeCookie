<script setup>
import { ref, onMounted } from "vue";
import { useRoute } from "vue-router";
import { accountApi } from "../api/accountApi";
import { errorMessage } from "../api/storeApi";
import BaseButton from "../components/ui/BaseButton.vue";
const route = useRoute(),
  busy = ref(true),
  error = ref(""),
  claimed = ref(null);
async function confirm() {
  busy.value = true;
  error.value = "";
  try {
    if (typeof route.query.token !== "string" || !route.query.token)
      throw new Error("missing-token");
    claimed.value = (await accountApi.confirmClaim(route.query.token)).claimed;
  } catch (e) {
    error.value =
      e.message === "missing-token"
        ? "This link is missing its token. Request a new one from Orders."
        : errorMessage(e);
  } finally {
    busy.value = false;
  }
}
onMounted(confirm);
</script>
<template>
  <div class="pt-[var(--nav-h)]">
    <section class="container-magic max-w-2xl py-16 md:py-24">
      <h1 class="font-display text-display">Your earlier orders</h1>
      <p v-if="busy" role="status" class="mt-6">Adding your orders…</p>
      <p v-else-if="error" role="alert" class="mt-6 text-red-800">
        {{ error }}
      </p>
      <p v-else role="status" class="mt-6">
        {{ claimed }} {{ claimed === 1 ? "order added" : "orders added" }} to
        your account.
      </p>
      <p v-if="claimed === 0" class="mt-3">
        Your earlier paid orders may already be in your account.
      </p>
      <div class="flex flex-wrap gap-3 mt-8">
        <RouterLink to="/account#orders" class="btn-primary"
          >Back to Orders</RouterLink
        ><BaseButton
          v-if="error"
          variant="outline"
          :disabled="busy"
          @click="confirm"
          >Try again</BaseButton
        >
      </div>
    </section>
  </div>
</template>
