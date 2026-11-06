<script setup>
import { computed, onMounted, ref } from "vue";
import { RouterLink, useRouter } from "vue-router";
import BaseButton from "../components/ui/BaseButton.vue";
import StepRail from "../components/checkout/StepRail.vue";
import StickyTotalBar from "../components/checkout/StickyTotalBar.vue";
import CartLine from "../components/checkout/CartLine.vue";
import { useCart } from "../composables/useCart";
import { useFormat } from "../composables/useFormat";
import { errorMessage, orderApi, submitPaymentForm } from "../api/storeApi";

const router = useRouter();
const { cart, ready, load, setQuantity, remove, applyDiscount, removeDiscount, restore } = useCart();
const { rand } = useFormat();

const busy = ref(false);
const error = ref("");
const loadError = ref("");
const code = ref("");
const codeError = ref("");
const showCode = ref(false);

const steps = [
  { label: "Cart", state: "current" },
  { label: "You", state: "todo" },
  { label: "Delivery", state: "todo" },
  { label: "Pay", state: "todo" },
];

const items = computed(() => cart.value?.items || []);
const hasIssues = computed(() => items.value.some((line) => line.issue));
const shortBy = computed(() =>
  cart.value?.belowMinimum ? Math.max(0, Number(cart.value.minimumOrder) - Number(cart.value.subtotal)) : 0,
);
const empty = computed(() => ready.value && items.value.length === 0);

async function run(action) {
  busy.value = true;
  error.value = "";
  try {
    await action();
  } catch (e) {
    error.value = errorMessage(e);
  } finally {
    busy.value = false;
  }
}

const changeQuantity = (line, quantity) => run(() => setQuantity(line.cartItemId, quantity));
const removeLine = (line) => run(() => remove(line.cartItemId));

async function apply() {
  if (!code.value.trim()) return;
  busy.value = true;
  codeError.value = "";
  try {
    await applyDiscount(code.value.trim());
    code.value = "";
    showCode.value = false;
  } catch (e) {
    codeError.value = errorMessage(e);
  } finally {
    busy.value = false;
  }
}

async function reload() {
  loadError.value = "";
  try {
    await load();
  } catch (e) {
    loadError.value = errorMessage(e);
  }
}

// Someone who left PayFast with the back button still has an unpaid order holding their cookies.
const LAST_ORDER = "gfc_last_order";
const unpaid = ref(null);
const unpaidBusy = ref(false);

async function checkUnpaid() {
  let last = null;
  try {
    last = JSON.parse(localStorage.getItem(LAST_ORDER) || "null");
  } catch {
    return;
  }
  if (!last?.orderNumber) return;
  try {
    const order = await orderApi.get(last.orderNumber, last.token);
    if (order.status === "PENDING_PAYMENT" && order.canPay) unpaid.value = { ...last, total: order.total };
    else localStorage.removeItem(LAST_ORDER);
  } catch {
    /* Not ours to worry about: the order page still works from its email. */
  }
}

async function finishPaying() {
  unpaidBusy.value = true;
  try {
    submitPaymentForm(await orderApi.payAgain(unpaid.value.orderNumber, unpaid.value.token));
  } catch (e) {
    error.value = errorMessage(e);
    unpaidBusy.value = false;
  }
}

async function changeOrder() {
  unpaidBusy.value = true;
  try {
    const cancelled = await orderApi.cancel(unpaid.value.orderNumber, unpaid.value.token);
    localStorage.removeItem(LAST_ORDER);
    unpaid.value = null;
    await restore(cancelled?.cartId);
  } catch (e) {
    error.value = errorMessage(e);
  } finally {
    unpaidBusy.value = false;
  }
}

onMounted(() => {
  reload();
  checkUnpaid();
});
</script>

<template>
  <div class="pt-[var(--nav-h)]">
    <div class="container-magic max-w-[64rem] pt-8 pb-48 md:pb-24">
      <StepRail :steps="steps" label="Checkout progress" />

      <h1 class="font-display text-display mt-8">Your cart</h1>

      <section v-if="unpaid" class="mt-6 rounded-2xl border border-chocolate/15 bg-cream-100/70 p-5" aria-labelledby="unpaid-heading">
        <h2 id="unpaid-heading" class="font-bold">You have an unpaid order</h2>
        <p class="mt-1 text-chocolate/80">
          Order {{ unpaid.orderNumber }} for {{ rand(unpaid.total) }} is waiting for payment, and we're holding its cookies for you.
        </p>
        <div class="mt-4 flex flex-wrap gap-3">
          <BaseButton :disabled="unpaidBusy" @click="finishPaying">Finish paying</BaseButton>
          <BaseButton variant="outline" :disabled="unpaidBusy" @click="changeOrder">Change my order</BaseButton>
        </div>
      </section>

      <p v-if="loadError" role="alert" class="mt-6">
        {{ loadError }}
        <button type="button" class="font-bold underline" @click="reload">Try again</button>
      </p>

      <ul v-if="!ready && !loadError" class="mt-6" aria-label="Loading your cart">
        <li v-for="n in 2" :key="n" class="py-5 flex gap-4">
          <div class="h-20 w-20 rounded-2xl shimmer" />
          <div class="flex-1 space-y-2 pt-1">
            <div class="h-4 w-2/3 rounded shimmer" />
            <div class="h-4 w-1/3 rounded shimmer" />
          </div>
        </li>
      </ul>

      <section v-else-if="empty" class="mt-8">
        <p class="text-chocolate/80 max-w-[34ch]">
          Nothing in here yet. Choose a cookie, pick a pack, and it will wait here for you.
        </p>
        <BaseButton :to="{ name: 'Products' }" size="lg" class="mt-8">Browse cookies</BaseButton>
      </section>

      <template v-else-if="cart">
        <p v-if="error" role="alert" class="mt-6 rounded-xl bg-red-800/[0.08] px-4 py-3 text-red-900">
          {{ error }}
        </p>

        <ul class="mt-4 divide-y divide-chocolate/[0.12] border-b border-chocolate/[0.12]" :aria-busy="busy">
          <CartLine
            v-for="line in items"
            :key="line.cartItemId"
            :line="line"
            :busy="busy"
            @quantity="(q) => changeQuantity(line, q)"
            @remove="removeLine(line)"
          />
        </ul>

        <section class="mt-6" aria-labelledby="code-heading">
          <h2 id="code-heading" class="sr-only">Discount code</h2>
          <div v-if="cart.discount" class="flex flex-wrap items-center justify-between gap-3">
            <p>
              <span class="inline-flex items-center rounded-full bg-primary/30 px-3 py-1 text-sm font-extrabold tabular">{{
                cart.discount.code
              }}</span>
              <span v-if="cart.discount.description" class="ml-2 text-sm">{{ cart.discount.description }}</span>
            </p>
            <button
              type="button"
              class="text-sm font-bold underline min-h-11"
              :disabled="busy"
              @click="run(removeDiscount)"
            >
              Remove code
            </button>
            <p v-if="!cart.discount.applied" class="w-full text-sm text-red-900">{{ cart.discount.message }}</p>
          </div>
          <template v-else>
            <button
              v-if="!showCode"
              type="button"
              class="font-bold underline decoration-primary decoration-2 underline-offset-4 min-h-11"
              :aria-expanded="showCode"
              @click="showCode = true"
            >
              Have a discount code?
            </button>
            <form v-else class="flex gap-2 items-start" @submit.prevent="apply">
              <label class="flex-1 min-w-0">
                <span class="sr-only">Discount code</span>
                <input
                  v-model="code"
                  class="field uppercase"
                  autocomplete="off"
                  spellcheck="false"
                  placeholder="Discount code"
                  :aria-invalid="!!codeError"
                  aria-describedby="code-error"
                />
              </label>
              <BaseButton type="submit" variant="outline" :disabled="busy || !code.trim()">Apply</BaseButton>
            </form>
            <p v-if="codeError" id="code-error" role="alert" class="mt-2 text-sm text-red-900">{{ codeError }}</p>
          </template>
        </section>

        <dl class="mt-8 space-y-2">
          <div class="flex justify-between gap-4">
            <dt>Subtotal <span class="text-chocolate/70">({{ cart.cookieCount }} cookies)</span></dt>
            <dd class="tabular">{{ rand(cart.subtotal) }}</dd>
          </div>
          <div v-if="cart.discountTotal > 0" class="flex justify-between gap-4">
            <dt>Discount</dt>
            <dd class="tabular">−{{ rand(cart.discountTotal) }}</dd>
          </div>
          <div class="flex justify-between gap-4">
            <dt>Delivery</dt>
            <dd class="text-chocolate/75 text-right">You'll choose in step 3</dd>
          </div>
        </dl>
        <p v-if="cart.belowMinimum" role="status" class="mt-6 rounded-xl bg-primary/20 px-4 py-3">
          Orders start at <strong>{{ rand(cart.minimumOrder) }}</strong> worth of cookies.
          Add <strong class="tabular">{{ rand(shortBy) }}</strong> more to check out.
          <RouterLink :to="{ name: 'Products' }" class="font-bold underline">Add more cookies</RouterLink>
        </p>
        <p class="mt-6 text-sm text-chocolate/75">
          Baked fresh to order. Questions about ingredients?
          <RouterLink :to="{ name: 'Contact' }" class="font-bold underline">Ask us</RouterLink> before you order.
        </p>

        <StickyTotalBar
          label="So far"
          :amount="cart.total"
          :note="hasIssues ? 'Fix the items marked above to continue.' : cart.belowMinimum ? `Add ${rand(shortBy)} more to check out.` : ''"
        >
          <BaseButton
            size="lg"
            block
            class="h-14"
            :disabled="busy || !cart.canCheckout"
            @click="router.push({ name: 'Checkout', params: { step: 'you' } })"
          >
            Continue
          </BaseButton>
        </StickyTotalBar>
      </template>
    </div>
  </div>
</template>
