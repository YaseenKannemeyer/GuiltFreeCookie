<script setup>
import { computed, onMounted, reactive, ref } from "vue";
import { useRoute } from "vue-router";
import BaseButton from "../components/ui/BaseButton.vue";
import BaseInput from "../components/ui/BaseInput.vue";
import { emailLinkApi } from "../api/flowApi";
import { errorMessage, orderApi } from "../api/storeApi";

// From the "How were your cookies?" email: one short review per cookie, no account needed.
const route = useRoute();
const order = ref(null);
const loadError = ref("");
const forms = reactive({});

const cookies = computed(() => {
  const seen = new Map();
  for (const item of order.value?.items || []) if (!seen.has(item.cookieId)) seen.set(item.cookieId, item);
  return [...seen.values()];
});
const delivered = computed(() => order.value?.status === "DELIVERED");

onMounted(async () => {
  try {
    order.value = await orderApi.get(route.params.orderNumber, route.query.token);
    const name = [order.value.firstName, order.value.lastName?.charAt(0)].filter(Boolean).join(" ");
    for (const item of cookies.value) {
      forms[item.cookieId] = { rating: 0, title: "", body: "", authorName: name, busy: false, error: "", sent: false };
    }
  } catch (e) {
    loadError.value = errorMessage(e);
  }
});

async function submit(cookieId) {
  const form = forms[cookieId];
  if (!form.rating) {
    form.error = "Choose a star rating.";
    return;
  }
  form.busy = true;
  form.error = "";
  try {
    await emailLinkApi.review({
      orderNumber: route.params.orderNumber,
      token: route.query.token,
      cookieId,
      rating: form.rating,
      title: form.title,
      body: form.body,
      authorName: form.authorName,
    });
    form.sent = true;
  } catch (e) {
    form.error = errorMessage(e);
  } finally {
    form.busy = false;
  }
}
</script>

<template>
  <div class="pt-[var(--nav-h)]">
    <div class="container-magic max-w-[64rem] pt-12 pb-24">
      <h1 class="font-display text-display">How were they?</h1>

      <p v-if="loadError" role="alert" class="mt-6 max-w-[44ch]">
        {{ loadError }} Use the link from your email, or
        <RouterLink :to="{ name: 'Contact' }" class="font-bold underline">contact us</RouterLink>.
      </p>
      <p v-else-if="!order" role="status" class="mt-6">Loading your order…</p>
      <p v-else-if="!delivered" class="mt-4 text-chocolate/80 max-w-[44ch]">
        You can review your cookies once they've arrived. We'll email you when it's time.
      </p>

      <template v-else>
        <p class="mt-4 text-chocolate/80 max-w-[48ch]">
          Tell other shoppers what you thought of order {{ order.orderNumber }}. We check every review before it goes up.
        </p>
        <ul class="mt-8 space-y-6">
          <li v-for="item in cookies" :key="item.cookieId" class="rounded-2xl border border-chocolate/15 bg-cream-100/70 p-5">
            <h2 class="text-[2rem] font-extrabold">{{ item.name }}</h2>
            <p v-if="forms[item.cookieId].sent" role="status" class="mt-3">Thank you! Your review will appear once we've checked it.</p>
            <form v-else class="mt-4 grid gap-4" @submit.prevent="submit(item.cookieId)">
              <fieldset>
                <legend class="text-sm font-bold">Your rating <span aria-hidden="true" class="text-red-800">*</span></legend>
                <div class="mt-1 flex gap-1" role="radiogroup">
                  <label v-for="n in 5" :key="n" class="cursor-pointer">
                    <input v-model="forms[item.cookieId].rating" type="radio" :name="`rating-${item.cookieId}`" :value="n" class="peer sr-only" />
                    <span
                      class="flex h-11 w-11 items-center justify-center rounded-full text-[2.4rem] leading-none peer-focus-visible:ring-2 peer-focus-visible:ring-chocolate"
                      :class="n <= forms[item.cookieId].rating ? 'text-[#9A6E45]' : 'text-chocolate/25'"
                      aria-hidden="true"
                      >★</span
                    >
                    <span class="sr-only">{{ n }} out of 5</span>
                  </label>
                </div>
              </fieldset>
              <BaseInput v-model="forms[item.cookieId].title" label="Headline" required />
              <BaseInput v-model="forms[item.cookieId].body" type="textarea" :rows="3" label="Your review" required />
              <BaseInput v-model="forms[item.cookieId].authorName" label="Name shown with your review" required />
              <p v-if="forms[item.cookieId].error" role="alert" class="text-red-900">{{ forms[item.cookieId].error }}</p>
              <div>
                <BaseButton type="submit" :disabled="forms[item.cookieId].busy">
                  {{ forms[item.cookieId].busy ? "Sending…" : "Post my review" }}
                </BaseButton>
              </div>
            </form>
          </li>
        </ul>
      </template>
    </div>
  </div>
</template>
