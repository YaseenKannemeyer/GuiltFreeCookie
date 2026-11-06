<script setup>
/**
 * NewsletterSection — "Join in and get 10% off" sign-up. Saves the address
 * through POST /newsletter/subscribe; admins export the list from
 * GET /newsletter/subscribers.
 * tone="dark" is the band at the top of the footer; tone="light" works as
 * a standalone section on cream.
 */
import { ref, computed, useId } from "vue";
import { RouterLink } from "vue-router";
import { EnvelopeIcon } from "@heroicons/vue/24/outline";
import { ArrowRightIcon, CheckIcon } from "@heroicons/vue/20/solid";
import { cleanEmail, isEmail } from "../../utils/normalize";
import newsletterApi from "../../api/newsletterApi";
import { DEMO_MODE } from "../../constants/business";

const props = defineProps({
  tone: { type: String, default: "light" }, // light | dark
});

const email = ref("");
const error = ref("");
const subscribed = ref(false);
const submitting = ref(false);
const dark = computed(() => props.tone === "dark");
const inputId = `newsletter-${useId()}`;

const subscribe = async () => {
  error.value = "";
  email.value = cleanEmail(email.value);
  if (!isEmail(email.value)) {
    error.value = "Enter an email address like you@example.co.za.";
    return;
  }
  submitting.value = true;
  try {
    await newsletterApi.subscribe(email.value);
    subscribed.value = true;
    email.value = "";
  } catch (err) {
    error.value =
      err.response?.status === 429
        ? "Lots of sign-ups from this connection. Try again in a little while."
        : err.response?.status === 400
          ? "Enter an email address like you@example.co.za."
          : "We couldn't sign you up just now. Check your connection and try again.";
  } finally {
    submitting.value = false;
  }
};
</script>

<template>
  <section
    :class="[
      'relative overflow-hidden',
      dark ? 'pt-20 pb-14 border-b border-cream-50/10' : 'bg-background py-20 md:py-24',
    ]"
    aria-labelledby="newsletter-title"
  >
    <img
      v-if="!dark"
      src="/img/cookie3.webp"
      alt=""
      loading="lazy"
      class="pointer-events-none absolute -left-16 -bottom-16 w-56 rotate-12 opacity-90 drop-shadow-cookie hidden md:block"
    />

    <div class="container-magic grid lg:grid-cols-[1.1fr_1fr] gap-10 items-end">
      <div>
        <h2
          id="newsletter-title"
          :class="['font-display text-display', dark ? 'text-cream-50' : '']"
        >
          Join in and get <span :class="dark ? 'text-primary' : 'sticker'">10% off</span>
        </h2>
        <p :class="['mt-4 max-w-md text-base leading-relaxed', dark ? 'text-cream-100/75' : 'text-chocolate/80']">
          <template v-if="DEMO_MODE">Try the demo sign-up with a sample email. Use DEMO10 at checkout for 10% off your sample order.</template>
          <template v-else>Subscribe and we'll send a 10% code for your next order, plus first word on restocks and new flavours.</template>
        </p>
      </div>

      <div aria-live="polite">
        <p
          v-if="subscribed"
          :class="[
            'flex items-center gap-3 rounded-2xl px-5 py-4 font-semibold',
            dark ? 'bg-cream-50/10 text-cream-50' : 'bg-surface text-chocolate',
          ]"
        >
          <span class="h-8 w-8 shrink-0 rounded-full bg-primary text-chocolate flex items-center justify-center">
            <CheckIcon class="h-5 w-5" />
          </span>
          {{ DEMO_MODE ? "Demo sign-up saved. Try DEMO10 at checkout." : "You're on the list. We'll email your 10% code." }}
        </p>

        <form v-else class="w-full" novalidate @submit.prevent="subscribe">
          <label :for="inputId" :class="['block text-sm font-semibold mb-2', dark ? 'text-cream-100/85' : 'text-chocolate/85']">
            Email address
          </label>
          <div class="flex flex-col sm:flex-row gap-3">
            <div class="relative flex-1 min-w-0">
              <EnvelopeIcon
                :class="['absolute left-5 top-1/2 -translate-y-1/2 h-5 w-5 pointer-events-none', dark ? 'text-cream-100/50' : 'text-chocolate/45']"
              />
              <input
                :id="inputId"
                v-model="email"
                type="email"
                autocomplete="email"
                placeholder="you@example.co.za"
                :aria-invalid="error ? 'true' : undefined"
                :class="[ 'w-full h-14 rounded-full pl-14 pr-5 text-base focus:outline-none focus:ring-2 transition-shadow', dark ? 'bg-chocolate-300 border border-cream-50/15 text-cream-50 placeholder:text-cream-100/50 focus:ring-primary' : 'bg-surface border border-cream-400 text-chocolate focus:ring-chocolate/25', ]"
              />
            </div>
            <button
              type="submit"
              :disabled="submitting"
              :class="[dark ? 'btn-secondary' : 'btn-primary', 'h-14 px-7', submitting ? 'opacity-60 cursor-wait' : '']"
            >
              {{ submitting ? "Subscribing" : "Subscribe" }}
              <ArrowRightIcon class="h-5 w-5" />
            </button>
          </div>
          <p v-if="error" :class="['mt-2 text-sm font-medium', dark ? 'text-primary-200' : 'text-red-700']" role="alert">
            {{ error }}
          </p>
          <p :class="['mt-3 text-sm', dark ? 'text-cream-100/70' : 'text-chocolate/75']">
            By subscribing you agree to receive marketing emails from us. Unsubscribe any time.
            <RouterLink to="/privacy-policy" class="font-bold underline underline-offset-4">Privacy Policy</RouterLink>
          </p>
        </form>
      </div>
    </div>
  </section>
</template>
