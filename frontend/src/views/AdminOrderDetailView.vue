<script setup>
import { computed, onMounted, reactive, ref } from "vue";
import { RouterLink, useRoute } from "vue-router";
import { ArrowLeftIcon } from "@heroicons/vue/20/solid";
import BaseButton from "../components/ui/BaseButton.vue";
import BaseInput from "../components/ui/BaseInput.vue";
import { adminOrderApi, errorMessage } from "../api/storeApi";
import { useAdminAuth } from "../composables/useAdminAuth";
import { useFormat } from "../composables/useFormat";
import { ORDER_STATUS, SHIPPING_METHOD, statusLabel } from "../constants/orderStatus";

const route = useRoute();
const { admin } = useAdminAuth();
const { rand } = useFormat();

const data = ref(null);
const loading = ref(true);
const busy = ref(false);
const error = ref("");
const notice = ref("");
const note = ref("");
const confirmingRefund = ref(false);
const refundNote = ref("");
const tracking = reactive({ courierName: "", trackingNumber: "", trackingUrl: "" });

const order = computed(() => data.value?.order);
const pickup = computed(() => order.value?.shippingMethod === "PICKUP");
const canShip = computed(() => !pickup.value && ["PAID", "PROCESSING", "SHIPPED"].includes(order.value?.status));
const PAID = ["PAID", "PROCESSING", "READY_FOR_PICKUP", "SHIPPED", "DELIVERED"];

// The one move the team usually makes next, matching what the server allows.
const nextMove = computed(() => {
  const s = order.value?.status;
  if (s === "PAID") return { status: "PROCESSING", label: "Start baking", hint: "Marks it as baking. The customer sees this on their order page." };
  if (s === "PROCESSING" && pickup.value)
    return { status: "READY_FOR_PICKUP", label: "Ready to collect", hint: "Emails the customer that it's ready, with the pickup details." };
  if (s === "READY_FOR_PICKUP") return { status: "DELIVERED", label: "Mark collected", hint: "" };
  if (s === "SHIPPED") return { status: "DELIVERED", label: "Mark delivered", hint: "" };
  return null;
});

const PROVINCES = {
  EASTERN_CAPE: "Eastern Cape", FREE_STATE: "Free State", GAUTENG: "Gauteng", KWAZULU_NATAL: "KwaZulu-Natal",
  LIMPOPO: "Limpopo", MPUMALANGA: "Mpumalanga", NORTH_WEST: "North West", NORTHERN_CAPE: "Northern Cape", WESTERN_CAPE: "Western Cape",
};
const addressLines = computed(() => {
  const a = order.value?.address;
  if (!a) return [];
  return [a.complex, `${a.streetNumber} ${a.streetName}`, a.suburb, `${a.city}, ${a.postalCode}`, PROVINCES[a.province]].filter(Boolean);
});

const when = (value) =>
  new Intl.DateTimeFormat("en-ZA", { day: "numeric", month: "short", hour: "2-digit", minute: "2-digit" }).format(new Date(value));
const actorName = (actor) =>
  ({ payfast: "PayFast", system: "Automatic", customer: "Customer" })[actor] || (actor === admin.value?.adminId ? "You" : "Team");

function adopt(view) {
  data.value = view;
  tracking.courierName = view.order.courierName || tracking.courierName;
  tracking.trackingNumber = view.order.trackingNumber || "";
  tracking.trackingUrl = view.order.trackingUrl || "";
}

async function load() {
  error.value = "";
  try {
    adopt(await adminOrderApi.get(route.params.orderNumber));
  } catch (e) {
    error.value = errorMessage(e);
  } finally {
    loading.value = false;
  }
}

async function act(run, done) {
  busy.value = true;
  error.value = "";
  notice.value = "";
  try {
    adopt(await run());
    notice.value = done;
    note.value = "";
  } catch (e) {
    error.value = errorMessage(e);
  } finally {
    busy.value = false;
  }
}

const move = (status, label) =>
  act(() => adminOrderApi.changeStatus(order.value.orderNumber, status, note.value.trim() || null), `${label}: done.`);
const ship = () =>
  act(
    () => adminOrderApi.ship(order.value.orderNumber, { ...tracking, trackingUrl: tracking.trackingUrl.trim() || null }),
    order.value.status === "SHIPPED" ? "Tracking updated." : "Marked shipped. The customer has been emailed.",
  );
async function refund() {
  await act(() => adminOrderApi.refund(order.value.orderNumber, refundNote.value.trim() || null), "Refund recorded. Pay it back in the PayFast dashboard.");
  confirmingRefund.value = false;
}

onMounted(load);
const sentence = (text) => (text ? text.charAt(0) + text.slice(1).toLowerCase() : "");
</script>

<template>
  <div class="p-5 md:p-10 max-w-5xl mx-auto">
    <RouterLink :to="{ name: 'AdminOrders' }" class="inline-flex items-center gap-1.5 font-bold min-h-11">
      <ArrowLeftIcon class="h-5 w-5" aria-hidden="true" /> All orders
    </RouterLink>

    <div v-if="loading" class="mt-6 space-y-4" aria-label="Loading order">
      <div class="h-12 w-2/3 rounded-full shimmer" />
      <div class="h-40 rounded-2xl shimmer" />
    </div>
    <p v-else-if="!order" role="alert" class="mt-6">
      {{ error || "Order not found." }} <button type="button" class="font-bold underline" @click="load">Try again</button>
    </p>

    <template v-else>
      <header class="mt-4 flex flex-wrap items-center gap-x-4 gap-y-2">
        <h1 class="font-display text-[2.8rem] sm:text-display tabular whitespace-nowrap">{{ order.orderNumber }}</h1>
        <span :class="['inline-flex rounded-full px-3 py-1 text-sm font-extrabold', ORDER_STATUS[order.status]?.badge]">{{
          statusLabel(order.status)
        }}</span>
      </header>
      <p class="mt-2 text-chocolate/80">
        <span class="whitespace-nowrap">Placed {{ when(order.createdAt) }}</span> ·
        <span class="whitespace-nowrap">{{ SHIPPING_METHOD[order.shippingMethod] }} · <span class="font-bold tabular">{{ rand(order.total) }}</span></span>
      </p>

      <p v-if="error" role="alert" class="mt-5 rounded-xl bg-red-800/[0.08] px-4 py-3 text-red-900">{{ error }}</p>
      <p v-if="notice" role="status" class="mt-5 rounded-xl bg-primary/25 px-4 py-3 font-bold">{{ notice }}</p>

      <div class="mt-8 grid gap-8 lg:grid-cols-[1fr_28rem]">
        <div class="space-y-8 min-w-0">
          <section class="rounded-2xl border border-chocolate/15 bg-cream-100/70 p-5" aria-labelledby="next-heading">
            <h2 id="next-heading" class="font-bold">Next step</h2>

            <template v-if="order.status === 'PENDING_PAYMENT'">
              <p class="mt-1 text-chocolate/80">
                Waiting for PayFast. Stock is held until {{ order.payBy ? when(order.payBy) : "the payment window closes" }}, then
                the order cancels itself.
              </p>
              <BaseButton variant="outline" class="mt-4" :disabled="busy" @click="move('CANCELLED', 'Cancelled')"
                >Cancel and free the stock</BaseButton
              >
            </template>

            <template v-else-if="nextMove || canShip">
              <p v-if="nextMove?.hint" class="mt-1 text-chocolate/80">{{ nextMove.hint }}</p>
              <div v-if="nextMove" class="mt-4 grid gap-3">
                <BaseInput v-model="note" label="Note for the timeline (optional)" />
                <div>
                  <BaseButton :disabled="busy" @click="move(nextMove.status, nextMove.label)">{{ nextMove.label }}</BaseButton>
                </div>
              </div>

              <form v-if="canShip" class="mt-6 grid grid-cols-1 gap-3 sm:grid-cols-2 [&>*]:min-w-0" @submit.prevent="ship">
                <h3 class="sm:col-span-2 font-bold">{{ order.status === "SHIPPED" ? "Tracking details" : "Ship it" }}</h3>
                <BaseInput v-model="tracking.courierName" label="Courier" required placeholder="The Courier Guy" />
                <BaseInput v-model="tracking.trackingNumber" label="Tracking number" required />
                <div class="sm:col-span-2">
                  <BaseInput v-model="tracking.trackingUrl" label="Tracking link (optional)" type="url" placeholder="https://" />
                </div>
                <div class="sm:col-span-2">
                  <BaseButton type="submit" :variant="order.status === 'SHIPPED' ? 'outline' : 'primary'" :disabled="busy">
                    {{ order.status === "SHIPPED" ? "Update tracking" : "Mark shipped" }}
                  </BaseButton>
                  <p v-if="order.status !== 'SHIPPED'" class="mt-2 text-sm text-chocolate/75">
                    The customer gets an email with the tracking details.
                  </p>
                </div>
              </form>
            </template>

            <p v-else class="mt-1 text-chocolate/80">
              {{
                {
                  DELIVERED: "Nothing left to do: this order is complete.",
                  CANCELLED: "Cancelled. Stock was returned.",
                  PAYMENT_FAILED: "The payment failed. Stock was returned and the customer's cart reopened.",
                  REFUNDED: "Refunded.",
                }[order.status]
              }}
            </p>

            <div v-if="PAID.includes(order.status)" class="mt-6 pt-5 border-t border-chocolate/[0.15]">
              <button
                v-if="!confirmingRefund"
                type="button"
                class="text-sm font-bold underline min-h-11"
                @click="confirmingRefund = true"
              >
                Record a refund
              </button>
              <div v-else class="grid gap-3" role="group" aria-labelledby="refund-heading">
                <h3 id="refund-heading" class="font-bold">Record a refund</h3>
                <p class="text-sm text-chocolate/80">
                  {{
                    ["SHIPPED", "DELIVERED"].includes(order.status)
                      ? "These cookies have left the bakery, so stock won't change."
                      : "The cookies go back into stock."
                  }}
                  This only records it: pay the money back in the PayFast dashboard.
                </p>
                <BaseInput v-model="refundNote" label="Reason (optional)" />
                <div class="flex flex-wrap gap-3">
                  <BaseButton variant="danger" :disabled="busy" @click="refund">Record refund of {{ rand(order.total) }}</BaseButton>
                  <BaseButton variant="cream" :disabled="busy" @click="confirmingRefund = false">Keep the order</BaseButton>
                </div>
              </div>
            </div>
          </section>

          <section v-if="data.customerNote" aria-labelledby="note-heading" class="rounded-2xl bg-primary/20 p-5">
            <h2 id="note-heading" class="font-bold">Customer's note</h2>
            <p class="mt-1 whitespace-pre-line">{{ data.customerNote }}</p>
          </section>

          <section aria-labelledby="items-heading">
            <h2 id="items-heading" class="font-bold">Items</h2>
            <ul class="mt-2 divide-y divide-chocolate/[0.1] border-y border-chocolate/[0.1]">
              <li v-for="item in order.items" :key="item.cookieId + item.variantLabel" class="py-3 flex justify-between gap-4">
                <span>
                  <span class="font-bold">{{ item.name }}</span>
                  <span class="block text-sm text-chocolate/75 tabular"
                    >{{ item.variantLabel }} × {{ item.quantity }} · {{ item.quantity * item.unitsPerPack }}
                    {{ item.quantity * item.unitsPerPack === 1 ? "cookie" : "cookies" }}</span
                  >
                </span>
                <span class="tabular">{{ rand(item.lineTotal) }}</span>
              </li>
            </ul>
            <dl class="mt-3 space-y-1 text-sm">
              <div class="flex justify-between"><dt>Subtotal</dt><dd class="tabular">{{ rand(order.subtotal) }}</dd></div>
              <div v-if="order.discountTotal > 0" class="flex justify-between">
                <dt>Discount ({{ order.discountCode }})</dt><dd class="tabular">−{{ rand(order.discountTotal) }}</dd>
              </div>
              <div class="flex justify-between"><dt>Delivery</dt><dd class="tabular">{{ rand(order.shippingTotal) }}</dd></div>
              <div class="flex justify-between font-extrabold text-base"><dt>Total</dt><dd class="tabular">{{ rand(order.total) }}</dd></div>
            </dl>
          </section>

          <section aria-labelledby="timeline-heading">
            <h2 id="timeline-heading" class="font-bold">Timeline</h2>
            <ol class="mt-3 space-y-3">
              <li v-for="event in [...data.events].reverse()" :key="event.eventId" class="flex gap-3">
                <span class="mt-2 h-2.5 w-2.5 shrink-0 rounded-full bg-chocolate/40" aria-hidden="true" />
                <span>
                  <span class="font-bold">{{ statusLabel(event.status) }}</span>
                  <span v-if="event.note"> · {{ event.note }}</span>
                  <span class="block text-sm text-chocolate/70 tabular">{{ when(event.createdAt) }} · {{ actorName(event.actor) }}</span>
                </span>
              </li>
            </ol>
          </section>
        </div>

        <aside class="space-y-8">
          <section aria-labelledby="customer-heading">
            <h2 id="customer-heading" class="font-bold">Customer</h2>
            <p class="mt-1">{{ order.firstName }} {{ order.lastName }}</p>
            <a :href="`mailto:${order.email}`" class="block underline break-all">{{ order.email }}</a>
            <a v-if="order.phone" :href="`tel:${order.phone.replace(/\s+/g, '')}`" class="block underline">{{ order.phone }}</a>
          </section>
          <section aria-labelledby="delivery-heading">
            <h2 id="delivery-heading" class="font-bold">{{ SHIPPING_METHOD[order.shippingMethod] }}</h2>
            <p v-if="addressLines.length" class="mt-1">{{ addressLines.join(", ") }}</p>
            <p v-else-if="order.pudoLocker" class="mt-1">{{ order.pudoLocker }}</p>
            <p v-else-if="order.pickupDetails" class="mt-1 whitespace-pre-line">{{ order.pickupDetails }}</p>
            <p v-if="order.trackingNumber" class="mt-2 text-sm">
              {{ order.courierName }} {{ order.trackingNumber }}
              <a v-if="order.trackingUrl" :href="order.trackingUrl" class="underline" target="_blank" rel="noopener">Track</a>
            </p>
          </section>
          <section v-if="data.payments.length" aria-labelledby="payments-heading">
            <h2 id="payments-heading" class="font-bold">Payments</h2>
            <ul class="mt-1 space-y-2 text-sm">
              <li v-for="payment in data.payments" :key="payment.paymentId">
                <span class="font-bold">{{ sentence(payment.gatewayStatus) }}</span> · {{ rand(payment.amount) }}
                <span class="block text-chocolate/70 tabular">PayFast ref {{ payment.gatewayReference }} · {{ when(payment.receivedAt) }}</span>
              </li>
            </ul>
          </section>
        </aside>
      </div>
    </template>
  </div>
</template>
