<script setup>
import { computed, onMounted, ref } from "vue";
import { useRoute } from "vue-router";
import BaseButton from "../components/ui/BaseButton.vue";
import { emailLinkApi } from "../api/flowApi";
import { errorMessage } from "../api/storeApi";

// /email/unsubscribe asks first (mail scanners open links, so nothing happens on load);
// /email/subscribed is the "keep me subscribed" button in the re-engagement emails.
const route = useRoute();
const email = computed(() => (typeof route.query.e === "string" ? route.query.e : ""));
const token = computed(() => (typeof route.query.t === "string" ? route.query.t : ""));
const staying = computed(() => route.name === "EmailSubscribed");

const state = ref(email.value && token.value ? (staying.value ? "working" : "ask") : "broken");
const busy = ref(false);
const error = ref("");

async function run(action, next) {
  busy.value = true;
  error.value = "";
  try {
    await action(email.value, token.value);
    state.value = next;
  } catch (e) {
    error.value = errorMessage(e);
    if (state.value === "working") state.value = "broken";
  } finally {
    busy.value = false;
  }
}

onMounted(() => {
  if (staying.value && state.value === "working") run(emailLinkApi.stay, "stayed");
});
</script>

<template>
  <div class="pt-[var(--nav-h)]">
    <div class="container-magic max-w-[64rem] pt-12 pb-24">
      <p v-if="state === 'working'" role="status">One moment…</p>

      <section v-else-if="state === 'ask'">
        <h1 class="font-display text-display">Unsubscribe?</h1>
        <p class="mt-4 text-chocolate/80 max-w-[44ch]">
          You'll stop getting offers, reminders and bakery news at <strong class="break-all">{{ email }}</strong>.
          Emails about orders you place still arrive.
        </p>
        <p v-if="error" role="alert" class="mt-5 rounded-xl bg-red-800/[0.08] px-4 py-3 text-red-900 max-w-[44ch]">{{ error }}</p>
        <div class="mt-8 flex flex-wrap gap-3">
          <BaseButton size="lg" :disabled="busy" @click="run(emailLinkApi.unsubscribe, 'unsubscribed')">Unsubscribe</BaseButton>
          <BaseButton :to="{ name: 'Home' }" variant="outline" size="lg">Keep my emails</BaseButton>
        </div>
      </section>

      <section v-else-if="state === 'unsubscribed'">
        <h1 class="font-display text-display">You're unsubscribed</h1>
        <p class="mt-4 text-chocolate/80 max-w-[44ch]">
          We won't send marketing emails to <strong class="break-all">{{ email }}</strong> again. Order updates still arrive.
        </p>
        <p v-if="error" role="alert" class="mt-5 rounded-xl bg-red-800/[0.08] px-4 py-3 text-red-900 max-w-[44ch]">{{ error }}</p>
        <p class="mt-8">
          Clicked by mistake?
          <button type="button" class="font-bold underline min-h-11" :disabled="busy" @click="run(emailLinkApi.resubscribe, 'back')">
            Subscribe again
          </button>
        </p>
      </section>

      <section v-else-if="state === 'back' || state === 'stayed'">
        <h1 class="font-display text-display">{{ state === "back" ? "Welcome back" : "You're staying with us" }}</h1>
        <p class="mt-4 text-chocolate/80 max-w-[44ch]">
          Thanks! You'll keep getting bakery news and offers. You can unsubscribe from any of our emails.
        </p>
        <BaseButton :to="{ name: 'Products' }" size="lg" class="mt-8">Browse cookies</BaseButton>
      </section>

      <section v-else>
        <h1 class="font-display text-display">That link didn't work</h1>
        <p class="mt-4 text-chocolate/80 max-w-[44ch]">
          {{ error || "It looks incomplete." }} Use the link in your most recent email from us, or
          <RouterLink :to="{ name: 'Contact' }" class="font-bold underline">contact us</RouterLink> and we'll sort it out.
        </p>
      </section>
    </div>
  </div>
</template>
