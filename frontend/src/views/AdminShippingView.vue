<script setup>
import { computed, onMounted, reactive, ref } from "vue";
import BaseButton from "../components/ui/BaseButton.vue";
import BaseInput from "../components/ui/BaseInput.vue";
import { adminOrderApi, errorMessage } from "../api/storeApi";
import { useFormat } from "../composables/useFormat";

const { rand } = useFormat();
const settings = reactive({});
const freeDelivery = ref(true);
const loaded = ref(false);
const busy = ref(false);
const error = ref("");
const notice = ref("");

const METHODS = [
  { key: "courier", title: "Courier to the door", etaLabel: "Delivery time shown to shoppers" },
  { key: "pudo", title: "PUDO locker", etaLabel: "Delivery time shown to shoppers" },
];

const preview = computed(() => {
  const parts = [];
  if (settings.courierEnabled) parts.push(`courier ${rand(settings.courierRate)}`);
  if (settings.pudoEnabled) parts.push(`PUDO locker ${rand(settings.pudoRate)}`);
  if (settings.pickupEnabled) parts.push("collect from the bakery for free");
  if (!parts.length) return "Shoppers won't be able to check out with every option switched off.";
  const free =
    freeDelivery.value && settings.freeShippingThreshold && (settings.courierEnabled || settings.pudoEnabled)
      ? ` Courier and PUDO are free on orders of ${rand(settings.freeShippingThreshold)} or more.`
      : "";
  return `Shoppers can choose ${parts.join(", ")}.${free}`;
});

async function load() {
  error.value = "";
  try {
    const data = await adminOrderApi.shippingSettings();
    Object.assign(settings, data);
    freeDelivery.value = data.freeShippingThreshold != null;
    settings.freeShippingThreshold ??= 500;
    loaded.value = true;
  } catch (e) {
    error.value = errorMessage(e);
  }
}

async function save() {
  busy.value = true;
  error.value = "";
  notice.value = "";
  try {
    const data = await adminOrderApi.saveShippingSettings({
      ...settings,
      courierRate: Number(settings.courierRate),
      pudoRate: Number(settings.pudoRate),
      freeShippingThreshold: freeDelivery.value ? Number(settings.freeShippingThreshold) : null,
    });
    Object.assign(settings, data);
    settings.freeShippingThreshold ??= 500;
    notice.value = "Saved. New checkouts use these settings straight away.";
  } catch (e) {
    error.value = errorMessage(e);
  } finally {
    busy.value = false;
  }
}

onMounted(load);
</script>

<template>
  <div class="max-w-3xl mx-auto">
    <h1 class="font-display text-display">Delivery</h1>
    <p class="mt-3 text-chocolate/80">What shoppers can choose at checkout, and what it costs them.</p>

    <p v-if="error" role="alert" class="mt-6 rounded-xl bg-red-800/[0.08] px-4 py-3 text-red-900">
      {{ error }} <button v-if="!loaded" type="button" class="font-bold underline" @click="load">Try again</button>
    </p>

    <div v-if="!loaded && !error" class="mt-8 space-y-4" aria-label="Loading settings">
      <div v-for="n in 3" :key="n" class="h-28 rounded-2xl shimmer" />
    </div>

    <form v-else-if="loaded" class="mt-8" @submit.prevent="save">
      <section v-for="method in METHODS" :key="method.key" class="py-7 border-b border-chocolate/20">
        <div class="flex flex-wrap items-center justify-between gap-4">
          <h2 class="text-[2rem] font-extrabold">{{ method.title }}</h2>
          <label class="flex items-center gap-2 font-bold min-h-11 cursor-pointer">
            <input v-model="settings[`${method.key}Enabled`]" type="checkbox" class="h-5 w-5" />
            Offered
          </label>
        </div>
        <div v-if="settings[`${method.key}Enabled`]" class="mt-4 grid gap-4 sm:grid-cols-[10rem_1fr]">
          <BaseInput v-model="settings[`${method.key}Rate`]" label="Price (R)" type="number" required />
          <BaseInput v-model="settings[`${method.key}Eta`]" :label="method.etaLabel" />
        </div>
      </section>

      <section class="py-7 border-b border-chocolate/20">
        <div class="flex flex-wrap items-center justify-between gap-4">
          <h2 class="text-[2rem] font-extrabold">Collect from the bakery</h2>
          <label class="flex items-center gap-2 font-bold min-h-11 cursor-pointer">
            <input v-model="settings.pickupEnabled" type="checkbox" class="h-5 w-5" />
            Offered
          </label>
        </div>
        <div v-if="settings.pickupEnabled" class="mt-4 grid gap-4">
          <BaseInput v-model="settings.pickupAddress" label="Pickup address" required help="Shown at checkout and in the ready-to-collect email." />
          <BaseInput v-model="settings.pickupInstructions" label="Instructions (optional)" type="textarea" :rows="2" />
          <BaseInput v-model="settings.pickupEta" label="When it's ready" />
        </div>
      </section>

      <section class="py-7 border-b border-chocolate/20">
        <div class="flex flex-wrap items-center justify-between gap-4">
          <h2 class="text-[2rem] font-extrabold">Free delivery</h2>
          <label class="flex items-center gap-2 font-bold min-h-11 cursor-pointer">
            <input v-model="freeDelivery" type="checkbox" class="h-5 w-5" />
            Offered
          </label>
        </div>
        <div v-if="freeDelivery" class="mt-4 max-w-[16rem]">
          <BaseInput
            v-model="settings.freeShippingThreshold"
            label="On orders from (R)"
            type="number"
            help="After discounts. Applies to courier and PUDO."
          />
        </div>
      </section>

      <p class="mt-7 rounded-2xl bg-cream-100/70 border border-cream-500 p-4" aria-live="polite">{{ preview }}</p>
      <p v-if="notice" role="status" class="mt-4 font-bold">{{ notice }}</p>
      <BaseButton type="submit" size="lg" class="mt-6" :disabled="busy">Save delivery settings</BaseButton>
    </form>
  </div>
</template>
