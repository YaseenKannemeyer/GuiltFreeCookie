<script setup>
import { onMounted, reactive, ref } from "vue";
import BaseButton from "../components/ui/BaseButton.vue";
import BaseInput from "../components/ui/BaseInput.vue";
import { adminFlowApi } from "../api/flowApi";
import { errorMessage } from "../api/storeApi";

const FLOWS = [
  {
    key: "WELCOME",
    toggle: "welcomeEnabled",
    title: "Welcome",
    about: "Five emails over four days for new subscribers. The first one carries their personal code.",
    fields: [
      { key: "welcomeDiscountPercent", label: "Discount (%)", help: "0 sends no code." },
      { key: "welcomeCodeDays", label: "Code lasts (days)" },
    ],
  },
  {
    key: "BROWSE_ABANDONMENT",
    toggle: "browseEnabled",
    title: "Browsed, didn't add",
    about: "Two emails when someone we can email looks at a cookie but doesn't add it. At most once a week.",
    fields: [],
  },
  {
    key: "CART_ABANDONMENT",
    toggle: "cartEnabled",
    title: "Left in the cart",
    about: "Three reminders over two days for carts left for an hour. The third can carry a code.",
    fields: [{ key: "cartDiscountPercent", label: "Discount in the last reminder (%)", help: "0 sends no code." }],
  },
  {
    key: "POST_PURCHASE",
    toggle: "postPurchaseEnabled",
    title: "After delivery",
    about: "Storage tips 3 days after an order is marked delivered, then a review request after 7.",
    fields: [],
  },
  {
    key: "REPLENISHMENT",
    toggle: "replenishmentEnabled",
    title: "Time for a refill",
    about: "One email offering to put their last order back in the cart.",
    fields: [{ key: "replenishmentDays", label: "Days after delivery" }],
  },
  {
    key: "WINBACK",
    toggle: "winbackEnabled",
    title: "Win back",
    about: "Three emails for subscribers who haven't ordered in a while. The last can carry a code.",
    fields: [
      { key: "winbackDays", label: "Days since last order" },
      { key: "winbackDiscountPercent", label: "Discount (%)", help: "0 sends no code." },
    ],
  },
  {
    key: "SUNSET",
    toggle: "sunsetEnabled",
    title: "Quiet subscribers",
    about: "Asks subscribers who haven't opened or clicked in a while if they still want emails, and pauses those who don't answer.",
    fields: [{ key: "sunsetDays", label: "Days without opens or clicks" }],
  },
];

const settings = reactive({});
const stats = reactive({});
const loaded = ref(false);
const busy = ref(false);
const error = ref("");
const notice = ref("");

async function load() {
  error.value = "";
  try {
    const [data, numbers] = await Promise.all([adminFlowApi.settings(), adminFlowApi.stats(30)]);
    Object.assign(settings, data);
    for (const row of numbers) stats[row.flow] = row;
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
    const body = { ...settings };
    for (const flow of FLOWS) for (const field of flow.fields) body[field.key] = Number(settings[field.key]);
    Object.assign(settings, await adminFlowApi.save(body));
    notice.value = "Saved. Changes apply to the next emails that go out.";
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
    <h1 class="font-display text-display">Email flows</h1>
    <p class="mt-3 text-chocolate/80">
      Emails that send themselves when something happens. Nobody gets two on the same day, nothing goes out between 20:00 and 08:00,
      and every one has an unsubscribe link.
      <RouterLink :to="{ name: 'AdminEmailPreviews' }" class="font-bold underline">See how they look</RouterLink>
    </p>

    <p v-if="error" role="alert" class="mt-6 rounded-xl bg-red-800/[0.08] px-4 py-3 text-red-900">
      {{ error }} <button v-if="!loaded" type="button" class="font-bold underline" @click="load">Try again</button>
    </p>
    <div v-if="!loaded && !error" class="mt-8 space-y-4" aria-label="Loading flows">
      <div v-for="n in 4" :key="n" class="h-28 rounded-2xl shimmer" />
    </div>

    <form v-else-if="loaded" class="mt-6" @submit.prevent="save">
      <section v-for="flow in FLOWS" :key="flow.key" class="py-7 border-b border-chocolate/20">
        <div class="flex items-start justify-between gap-4">
          <div class="min-w-0">
            <h2 class="text-[2rem] font-extrabold">{{ flow.title }}</h2>
            <p class="mt-1 text-chocolate/80">{{ flow.about }}</p>
          </div>
          <label class="flex items-center gap-2 font-bold min-h-11 cursor-pointer shrink-0">
            <input v-model="settings[flow.toggle]" type="checkbox" class="h-5 w-5" />
            On
          </label>
        </div>
        <p class="mt-3 text-sm text-chocolate/75 tabular">
          Last 30 days: {{ stats[flow.key]?.sent || 0 }} sent · {{ stats[flow.key]?.opened || 0 }} opened ·
          {{ stats[flow.key]?.clicked || 0 }} clicked
        </p>
        <div v-if="flow.fields.length && settings[flow.toggle]" class="mt-4 grid gap-4 sm:grid-cols-2 [&>*]:min-w-0">
          <BaseInput
            v-for="field in flow.fields"
            :key="field.key"
            v-model="settings[field.key]"
            :label="field.label"
            :help="field.help"
            type="number"
            required
          />
        </div>
      </section>

      <p class="mt-6 text-sm text-chocolate/75">
        Opens are a rough guide: some mail apps load images without the email being read. Clicks are reliable.
      </p>
      <p v-if="notice" role="status" class="mt-4 font-bold">{{ notice }}</p>
      <BaseButton type="submit" size="lg" class="mt-6" :disabled="busy">Save email flows</BaseButton>
    </form>
  </div>
</template>
