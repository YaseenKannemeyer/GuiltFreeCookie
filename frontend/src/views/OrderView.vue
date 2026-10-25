<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from "vue";
import { RouterLink, useRoute } from "vue-router";
import BaseButton from "../components/ui/BaseButton.vue";
import StepRail from "../components/checkout/StepRail.vue";
import { useFormat } from "../composables/useFormat";
import { useCart } from "../composables/useCart";
import { errorMessage, orderApi, submitPaymentForm } from "../api/storeApi";

const route = useRoute();
const { rand } = useFormat();
const { restore } = useCart();

const order = ref(null);
const loading = ref(true);
const notFound = ref(false);
const error = ref("");
const busy = ref(false);
let timer = null;
let polls = 0;

const token = computed(() => route.query.token);
const VIA = { COURIER: "Courier", PUDO: "PUDO locker", PICKUP: "Collect" };
const PROVINCES = {
  EASTERN_CAPE: "Eastern Cape",
  FREE_STATE: "Free State",
  GAUTENG: "Gauteng",
  KWAZULU_NATAL: "KwaZulu-Natal",
  LIMPOPO: "Limpopo",
  MPUMALANGA: "Mpumalanga",
  NORTH_WEST: "North West",
  NORTHERN_CAPE: "Northern Cape",
  WESTERN_CAPE: "Western Cape",
};

const pickup = computed(() => order.value?.shippingMethod === "PICKUP");
const PROGRESS = ["PENDING_PAYMENT", "PAID", "PROCESSING", "SHIPPED", "DELIVERED"];
const rail = computed(() => {
  const status = order.value?.status;
  const at = PROGRESS.indexOf(status === "READY_FOR_PICKUP" ? "SHIPPED" : status);
  const labels = ["Ordered", "Paid", "Baking", pickup.value ? "Ready to collect" : "On its way", pickup.value ? "Collected" : "Delivered"];
  return labels.map((label, i) => ({
    label,
    state: i === 0 || i < at ? "done" : i === at ? (i === labels.length - 1 ? "done" : "current") : "todo",
  }));
});
const offTrack = computed(() => ["PAYMENT_FAILED", "CANCELLED", "REFUNDED"].includes(order.value?.status));

const headline = computed(() => {
  const o = order.value;
  if (!o) return "";
  return {
    PENDING_PAYMENT: o.canPay ? "Almost there" : "Waiting for payment",
    PAID: `Thanks, ${o.firstName}!`,
    PROCESSING: `Thanks, ${o.firstName}!`,
    READY_FOR_PICKUP: "Ready to collect",
    SHIPPED: "On its way",
    DELIVERED: pickup.value ? "Collected. Enjoy!" : "Delivered. Enjoy!",
    PAYMENT_FAILED: "Payment didn't go through",
    CANCELLED: "Order cancelled",
    REFUNDED: "Order refunded",
  }[o.status];
});

const statusLine = computed(() => {
  const o = order.value;
  if (!o) return "";
  return {
    PENDING_PAYMENT: o.canPay
      ? "We're waiting for PayFast to confirm your payment. This page updates by itself."
      : "We didn't receive payment in time, so nothing was charged.",
    PAID: "Payment received. We'll start baking soon and email you at each step.",
    PROCESSING: "We're baking your cookies.",
    READY_FOR_PICKUP: "Your order is packed and waiting for you.",
    SHIPPED: o.courierName ? `With ${o.courierName}${o.trackingNumber ? `, tracking number ${o.trackingNumber}` : ""}.` : "Your order has left the bakery.",
    DELIVERED: "Thank you for ordering from us.",
    PAYMENT_FAILED: "Nothing was charged, and your cookies are back in your cart.",
    CANCELLED: "Nothing was charged for this order.",
    REFUNDED: "The refund goes back through PayFast. Banks can take a few working days to show it.",
  }[o.status];
});

const addressLines = computed(() => {
  const a = order.value?.address;
  if (!a) return [];
  return [a.complex, `${a.streetNumber} ${a.streetName}`, a.suburb, `${a.city}, ${a.postalCode}`, PROVINCES[a.province]].filter(Boolean);
});

const time = (value) =>
  new Intl.DateTimeFormat("en-ZA", { day: "numeric", month: "short", hour: "2-digit", minute: "2-digit" }).format(new Date(value));

async function fetchOrder() {
  try {
    order.value = await orderApi.get(route.params.orderNumber, token.value);
    error.value = "";
  } catch (e) {
    if (e?.response?.status === 404) notFound.value = true;
    else error.value = errorMessage(e);
  } finally {
    loading.value = false;
  }
}

// PayFast confirms a few seconds after the shopper returns: check often at first, then ease off.
function schedulePoll() {
  clearTimeout(timer);
  if (order.value?.status !== "PENDING_PAYMENT" || !order.value.canPay) return;
  timer = setTimeout(async () => {
    polls++;
    await fetchOrder();
    schedulePoll();
  }, polls < 40 ? 3000 : 15000);
}

async function payAgain() {
  busy.value = true;
  try {
    submitPaymentForm(await orderApi.payAgain(route.params.orderNumber, token.value));
  } catch (e) {
    busy.value = false;
    error.value = errorMessage(e);
    await fetchOrder();
  }
}

async function cancel() {
  busy.value = true;
  try {
    order.value = await orderApi.cancel(route.params.orderNumber, token.value);
    await restore().catch(() => {});
  } catch (e) {
    error.value = errorMessage(e);
  } finally {
    busy.value = false;
  }
}

watch(() => order.value?.status, schedulePoll);
onMounted(async () => {
  await fetchOrder();
  schedulePoll();
});
onBeforeUnmount(() => clearTimeout(timer));
</script>

<template>
  <div class="pt-[var(--nav-h)]">
    <div class="container-magic max-w-[64rem] pt-8 pb-24">
      <div v-if="loading" aria-label="Loading your order" class="space-y-4">
        <div class="h-9 w-3/4 rounded-full shimmer" />
        <div class="h-14 w-2/3 rounded-2xl shimmer mt-6" />
        <div class="h-40 rounded-2xl shimmer" />
      </div>

      <section v-else-if="notFound">
        <h1 class="font-display text-display">We can't find that order</h1>
        <p class="mt-4 text-chocolate/80 max-w-[44ch]">
          Use the link in your order email: it includes the key that unlocks this page. Still stuck?
          <RouterLink :to="{ name: 'Contact' }" class="font-bold underline">Contact us</RouterLink> with your order number.
        </p>
      </section>

      <p v-else-if="!order && error" role="alert">
        {{ error }} <button type="button" class="font-bold underline" @click="fetchOrder">Try again</button>
      </p>

      <template v-else-if="order">
        <StepRail v-if="!offTrack" :steps="rail" label="Order progress" />

        <h1 class="font-display text-display mt-8">{{ headline }}</h1>

        <div class="mt-6 rounded-2xl bg-cream-100/70 border border-cream-500 overflow-hidden">
          <dl class="grid grid-cols-2 sm:grid-cols-3 border-b border-dashed border-cream-500">
            <div class="p-3.5 min-w-0">
              <dt class="text-sm font-extrabold text-chocolate/70">Order</dt>
              <dd class="font-bold tabular truncate">{{ order.orderNumber.split("-").pop() }}</dd>
            </div>
            <div class="p-3.5 min-w-0 border-l border-dashed border-cream-500">
              <dt class="text-sm font-extrabold text-chocolate/70">Via</dt>
              <dd class="font-bold truncate">{{ VIA[order.shippingMethod] }}</dd>
            </div>
            <div class="p-3.5 min-w-0 col-span-2 sm:col-span-1 border-t sm:border-t-0 sm:border-l border-dashed border-cream-500">
              <dt class="text-sm font-extrabold text-chocolate/70">Estimate</dt>
              <dd class="font-bold">{{ order.deliveryEta || "—" }}</dd>
            </div>
          </dl>
          <div
            :class="[
              'px-4 py-4',
              offTrack ? 'bg-chocolate/[0.07]' : order.status === 'PENDING_PAYMENT' ? 'bg-tan-light/60' : 'bg-primary',
            ]"
            role="status"
            aria-live="polite"
          >
            <p class="font-bold">{{ statusLine }}</p>
            <p class="text-sm text-chocolate/80 mt-1">Order {{ order.orderNumber }}</p>
          </div>
        </div>

        <p v-if="error" role="alert" class="mt-4 text-red-900">{{ error }}</p>

        <div v-if="order.status === 'PENDING_PAYMENT' && order.canPay" class="mt-6 flex flex-wrap gap-3">
          <BaseButton size="lg" :disabled="busy" @click="payAgain">Pay {{ rand(order.total) }} now</BaseButton>
          <BaseButton variant="outline" size="lg" :disabled="busy" @click="cancel">Cancel order</BaseButton>
          <p class="w-full text-sm text-chocolate/75">
            Already paid? It can take a minute to show. We hold your cookies until {{ time(order.payBy) }}.
          </p>
        </div>
        <BaseButton
          v-if="['PAYMENT_FAILED', 'CANCELLED'].includes(order.status)"
          :to="{ name: 'Cart' }"
          size="lg"
          class="mt-6"
          @click="restore"
          >Back to my cart</BaseButton
        >
        <BaseButton
          v-if="order.status === 'SHIPPED' && order.trackingUrl"
          :href="order.trackingUrl"
          size="lg"
          class="mt-6"
          >Track your parcel</BaseButton
        >

        <section v-if="pickup && order.pickupDetails" class="mt-10" aria-labelledby="pickup-heading">
          <h2 id="pickup-heading" class="font-bold">Where to collect</h2>
          <p class="mt-1 whitespace-pre-line">{{ order.pickupDetails }}</p>
        </section>
        <section v-else-if="order.shippingMethod === 'PUDO'" class="mt-10" aria-labelledby="pudo-heading">
          <h2 id="pudo-heading" class="font-bold">PUDO locker</h2>
          <p class="mt-1">{{ order.pudoLocker }}</p>
        </section>
        <section v-else-if="addressLines.length" class="mt-10" aria-labelledby="address-heading">
          <h2 id="address-heading" class="font-bold">Delivering to</h2>
          <p class="mt-1">{{ order.firstName }} {{ order.lastName }}</p>
          <p class="text-chocolate/80">{{ addressLines.join(", ") }}</p>
        </section>

        <section class="mt-10" aria-labelledby="items-heading">
          <h2 id="items-heading" class="font-bold">Your cookies</h2>
          <ul class="mt-2 divide-y divide-chocolate/[0.1] border-y border-chocolate/[0.1]">
            <li v-for="item in order.items" :key="item.cookieId + item.variantLabel" class="py-3 flex justify-between gap-4">
              <span>
                <span class="font-bold">{{ item.name }}</span>
                <span class="block text-sm text-chocolate/75">{{ item.variantLabel }} × {{ item.quantity }}</span>
              </span>
              <span class="tabular">{{ rand(item.lineTotal) }}</span>
            </li>
          </ul>
          <dl class="mt-4 space-y-1.5">
            <div class="flex justify-between gap-4">
              <dt>Subtotal</dt>
              <dd class="tabular">{{ rand(order.subtotal) }}</dd>
            </div>
            <div v-if="order.discountTotal > 0" class="flex justify-between gap-4">
              <dt>Discount <span class="text-chocolate/70">({{ order.discountCode }})</span></dt>
              <dd class="tabular">−{{ rand(order.discountTotal) }}</dd>
            </div>
            <div class="flex justify-between gap-4">
              <dt>Delivery</dt>
              <dd class="tabular">{{ Number(order.shippingTotal) === 0 ? "Free" : rand(order.shippingTotal) }}</dd>
            </div>
            <div class="flex justify-between gap-4 font-extrabold pt-2 border-t border-chocolate/[0.12]">
              <dt>Total</dt>
              <dd class="tabular">{{ rand(order.total) }}</dd>
            </div>
            <div v-if="order.vatIncluded" class="flex justify-between gap-4 text-sm text-chocolate/75">
              <dt>Includes VAT</dt>
              <dd class="tabular">{{ rand(order.vatIncluded) }}</dd>
            </div>
          </dl>
        </section>

        <section class="mt-10" aria-labelledby="timeline-heading">
          <h2 id="timeline-heading" class="font-bold">What's happened so far</h2>
          <ol class="mt-3 space-y-3">
            <li v-for="(entry, i) in [...order.timeline].reverse()" :key="i" class="flex gap-3">
              <span
                :class="['mt-2 h-2.5 w-2.5 shrink-0 rounded-full', i === 0 ? 'bg-primary-700' : 'bg-chocolate/30']"
                aria-hidden="true"
              />
              <span class="flex-1">
                <span class="font-bold">{{ entry.note }}</span>
                <span class="block text-sm text-chocolate/70 tabular">{{ time(entry.at) }}</span>
              </span>
            </li>
          </ol>
        </section>

        <p class="mt-10 text-sm text-chocolate/75">
          <template v-if="order.status !== 'PENDING_PAYMENT'">We've emailed updates to {{ order.email }}. </template>
          Questions about this order?
          <RouterLink :to="{ name: 'Contact' }" class="font-bold underline">Contact us</RouterLink>
          and mention {{ order.orderNumber }}.
        </p>
      </template>
    </div>
  </div>
</template>
