<script setup>
import { computed, onMounted, reactive, ref, watch } from "vue";
import { RouterLink, useRoute, useRouter } from "vue-router";
import BaseButton from "../components/ui/BaseButton.vue";
import BaseInput from "../components/ui/BaseInput.vue";
import StepRail from "../components/checkout/StepRail.vue";
import StickyTotalBar from "../components/checkout/StickyTotalBar.vue";
import ChoiceTile from "../components/checkout/ChoiceTile.vue";
import CartLine from "../components/checkout/CartLine.vue";
import newsletterApi from "../api/newsletterApi";
import { useCart } from "../composables/useCart";
import { useCheckoutAddresses } from "../composables/useCheckoutAddresses";
import { useCustomerAuth } from "../composables/useCustomerAuth";
import { useSiteInfo } from "../composables/useSiteInfo";
import { useFormat } from "../composables/useFormat";
import { cartApi, checkoutApi, errorMessage, submitPaymentForm } from "../api/storeApi";

const STEPS = ["you", "delivery", "pay"];
const DRAFT_KEY = "gfc_checkout_draft";
const PROVINCES = [
  ["EASTERN_CAPE", "Eastern Cape"],
  ["FREE_STATE", "Free State"],
  ["GAUTENG", "Gauteng"],
  ["KWAZULU_NATAL", "KwaZulu-Natal"],
  ["LIMPOPO", "Limpopo"],
  ["MPUMALANGA", "Mpumalanga"],
  ["NORTH_WEST", "North West"],
  ["NORTHERN_CAPE", "Northern Cape"],
  ["WESTERN_CAPE", "Western Cape"],
];
const METHOD_NAMES = { COURIER: "Courier to your door", PUDO: "PUDO locker", PICKUP: "Collect from the bakery" };
const SA_MOBILE = /^(\+27|27|0)[678]\d{8}$/;
const EMAIL = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

const route = useRoute();
const router = useRouter();
const { cart, load, setEmail, forget } = useCart();
const { customer, isLoggedIn } = useCustomerAuth();
const { info: site, load: loadSite } = useSiteInfo();
const { rand } = useFormat();

const step = computed(() => (STEPS.includes(route.params.step) ? route.params.step : "you"));
const rail = computed(() => {
  const at = STEPS.indexOf(step.value);
  return [
    { label: "Cart", state: "done", to: { name: "Cart" } },
    ...["You", "Delivery", "Pay"].map((label, i) => ({
      label,
      state: i < at ? "done" : i === at ? "current" : "todo",
      to: { name: "Checkout", params: { step: STEPS[i] } },
    })),
  ];
});

function readDraft() {
  try {
    return JSON.parse(sessionStorage.getItem(DRAFT_KEY) || "{}");
  } catch {
    return {};
  }
}
const draft = readDraft();
const form = reactive({
  email: draft.email || "",
  firstName: draft.firstName || "",
  lastName: draft.lastName || "",
  phone: draft.phone || "",
  newsletterOptIn: !!draft.newsletterOptIn,
  savedAddressId: draft.savedAddressId || "",
  saveAddress: !!draft.saveAddress,
  shippingMethod: draft.shippingMethod || null,
  address: {
    streetNumber: "",
    streetName: "",
    complex: "",
    suburb: "",
    city: "",
    province: "WESTERN_CAPE",
    postalCode: "",
    ...(draft.address || {}),
  },
  pudoLocker: draft.pudoLocker || "",
  note: draft.note || "",
});
const { addresses: savedAddresses, unavailable: addressesUnavailable, loading: addressesLoading, loadAddresses, chooseAddress, saveBeforeCheckout } = useCheckoutAddresses(form, isLoggedIn);
watch(
  form,
  (value) => {
    try {
      sessionStorage.setItem(DRAFT_KEY, JSON.stringify(value));
    } catch {
      /* The draft is a convenience only. */
    }
  },
  { deep: true },
);

const errors = reactive({});
const error = ref("");
const busy = ref(false);
const options = ref([]);
const priced = ref(null);
const askPhoneHere = ref(false);

const chosen = computed(() => options.value.find((o) => o.method === form.shippingMethod));
const summary = computed(() => priced.value || cart.value);
const contactEmail = computed(() => (isLoggedIn.value ? customer.value.customerEmail : form.email.trim()));

function prefillFromAccount() {
  if (!isLoggedIn.value) return;
  const c = customer.value;
  form.firstName ||= c.name?.firstName || "";
  form.lastName ||= c.name?.lastName || "";
  form.phone ||= c.mobileNumber || "";
}

function clearErrors() {
  for (const key of Object.keys(errors)) delete errors[key];
  error.value = "";
}

async function reprice() {
  if (!cart.value?.cartId || !form.shippingMethod) {
    priced.value = null;
    return;
  }
  try {
    priced.value = await cartApi.get(cart.value.cartId, form.shippingMethod);
  } catch (e) {
    priced.value = null;
    error.value = errorMessage(e);
  }
}

async function loadOptions() {
  try {
    options.value = await cartApi.shippingOptions(cart.value.cartId);
    if (form.shippingMethod && !chosen.value) form.shippingMethod = null;
    await reprice();
  } catch (e) {
    error.value = errorMessage(e);
  }
}
watch(() => form.shippingMethod, reprice);

const go = (next) => router.push({ name: "Checkout", params: { step: next } });

// Step 2: who it's for. A guest's email is saved on the cart straight away.
async function continueFromYou() {
  clearErrors();
  if (!isLoggedIn.value && !EMAIL.test(form.email.trim())) errors.email = "Enter a valid email address.";
  if (!form.firstName.trim()) errors.firstName = "Enter your first name.";
  if (!form.lastName.trim()) errors.lastName = "Enter your last name.";
  if (form.phone.trim() && !SA_MOBILE.test(form.phone.replace(/\s+/g, "")))
    errors.phone = "Enter a South African mobile number, like 082 123 4567.";
  if (Object.keys(errors).length) return;
  busy.value = true;
  try {
    if (!isLoggedIn.value) await setEmail(form.email.trim());
    // Recorded now, not only at payment, so a shopper who ticked it and left still hears from us.
    if (form.newsletterOptIn) newsletterApi.subscribe(contactEmail.value).catch(() => {});
    go("delivery");
  } catch (e) {
    error.value = errorMessage(e);
  } finally {
    busy.value = false;
  }
}

// Step 3: how it gets to them.
function continueFromDelivery() {
  clearErrors();
  if (!form.shippingMethod) {
    error.value = "Choose how you'd like to get your order.";
    return;
  }
  if (form.shippingMethod !== "PICKUP") {
    if (!form.phone.trim()) {
      askPhoneHere.value = true;
      errors.phone = "Add a mobile number so the courier can reach you.";
    } else if (!SA_MOBILE.test(form.phone.replace(/\s+/g, ""))) {
      askPhoneHere.value = true;
      errors.phone = "Enter a South African mobile number, like 082 123 4567.";
    }
  }
  if (form.shippingMethod === "COURIER") {
    const a = form.address;
    if (!a.streetNumber.trim()) errors.streetNumber = "Enter the street number.";
    if (!a.streetName.trim()) errors.streetName = "Enter the street name.";
    if (!a.suburb.trim()) errors.suburb = "Enter the suburb.";
    if (!a.city.trim()) errors.city = "Enter the city or town.";
    if (!/^\d{4}$/.test(a.postalCode.trim())) errors.postalCode = "Enter the 4-digit postal code.";
  }
  if (form.shippingMethod === "PUDO" && !form.pudoLocker.trim())
    errors.pudoLocker = "Tell us which PUDO locker to deliver to.";
  if (Object.keys(errors).length) return;
  go("pay");
}

// Step 4: place the order, then hand over to PayFast.
async function pay() {
  clearErrors();
  busy.value = true;
  try {
    await saveBeforeCheckout();
    const result = await checkoutApi.placeOrder({
      cartId: cart.value.cartId,
      email: contactEmail.value,
      firstName: form.firstName.trim(),
      lastName: form.lastName.trim(),
      phone: form.phone.trim() || null,
      shippingMethod: form.shippingMethod,
      address: form.shippingMethod === "COURIER" ? { ...form.address, complex: form.address.complex || null } : null,
      pudoLocker: form.shippingMethod === "PUDO" ? form.pudoLocker.trim() : null,
      note: form.note.trim() || null,
      newsletterOptIn: form.newsletterOptIn,
    });
    try {
      localStorage.setItem("gfc_last_order", JSON.stringify({ orderNumber: result.orderNumber, token: result.accessToken }));
      sessionStorage.removeItem(DRAFT_KEY);
    } catch {
      /* Storage is a convenience; the order email has the link too. */
    }
    forget();
    if (result.payment) submitPaymentForm(result.payment);
    else router.replace({ name: "Order", params: { orderNumber: result.orderNumber }, query: { token: result.accessToken } });
  } catch (e) {
    busy.value = false;
    error.value = errorMessage(e);
    if (e?.response?.status === 409) await load().catch(() => {});
  }
}

const addressLines = computed(() => {
  const a = form.address;
  return [
    a.complex,
    `${a.streetNumber} ${a.streetName}`.trim(),
    a.suburb,
    `${a.city}, ${a.postalCode}`,
    PROVINCES.find(([v]) => v === a.province)?.[1],
  ].filter(Boolean);
});

async function enterStep() {
  clearErrors();
  if (!cart.value?.items?.length) {
    router.replace({ name: "Cart" });
    return;
  }
  if (step.value !== "you" && !form.firstName) {
    go("you");
    return;
  }
  if (step.value === "pay" && !form.shippingMethod) {
    go("delivery");
    return;
  }
  if (step.value !== "you") await loadOptions();
  window.scrollTo({ top: 0 });
}

watch(step, enterStep);
onMounted(async () => {
  prefillFromAccount();
  loadSite();
  try {
    await load();
  } catch (e) {
    error.value = errorMessage(e);
    return;
  }
  await enterStep();
});
</script>

<template>
  <div class="pt-[var(--nav-h)]">
    <div class="container-magic max-w-[64rem] pt-8 pb-56 md:pb-24">
      <StepRail :steps="rail" label="Checkout progress" />

      <p v-if="error" role="alert" class="mt-6 rounded-xl bg-red-800/[0.08] px-4 py-3 text-red-900">
        {{ error }}
        <RouterLink v-if="/cart/i.test(error)" :to="{ name: 'Cart' }" class="font-bold underline">Review your cart</RouterLink>
      </p>

      <!-- Step 2: You -->
      <form v-if="step === 'you'" novalidate @submit.prevent="continueFromYou">
        <h1 class="font-display text-display mt-8">Who's this order for?</h1>
        <p v-if="!isLoggedIn" class="mt-3 text-chocolate/80">
          No account needed.
          <RouterLink
            :to="{ name: 'CustomerLogin', query: { redirect: '/checkout/you' } }"
            class="font-bold underline decoration-primary decoration-2 underline-offset-4"
            >Sign in</RouterLink
          >
          if you have one.
        </p>
        <div class="mt-8 grid gap-5">
          <p v-if="isLoggedIn" class="rounded-2xl bg-cream-100/60 border border-cream-500 px-4 py-3">
            Confirmation goes to <span class="font-bold">{{ contactEmail }}</span>
          </p>
          <BaseInput
            v-else
            v-model="form.email"
            label="Email"
            type="email"
            autocomplete="email"
            required
            :error="errors.email"
            help="We'll send your order confirmation and updates here."
          />
          <div class="grid sm:grid-cols-2 gap-5">
            <BaseInput v-model="form.firstName" label="First name" autocomplete="given-name" required :error="errors.firstName" />
            <BaseInput v-model="form.lastName" label="Last name" autocomplete="family-name" required :error="errors.lastName" />
          </div>
          <BaseInput
            v-model="form.phone"
            label="Mobile number"
            type="tel"
            autocomplete="tel"
            :error="errors.phone"
            help="Needed for courier and PUDO delivery."
          />
          <label class="flex items-start gap-3 min-h-11 cursor-pointer">
            <input v-model="form.newsletterOptIn" type="checkbox" class="mt-1.5 h-5 w-5 shrink-0" />
            <span>Send me new flavours and offers. You can unsubscribe at any time.</span>
          </label>
        </div>
        <StickyTotalBar label="So far" :amount="cart?.total ?? null">
          <BaseButton type="submit" size="lg" block class="h-14" :disabled="busy">Continue to delivery</BaseButton>
        </StickyTotalBar>
      </form>

      <!-- Step 3: Delivery -->
      <form v-else-if="step === 'delivery'" novalidate @submit.prevent="continueFromDelivery">
        <h1 class="font-display text-display mt-8">How should we get them to you?</h1>
        <fieldset class="mt-8">
          <legend class="sr-only">Delivery option</legend>
          <div v-if="!options.length" class="grid gap-3" aria-label="Loading delivery options">
            <div v-for="n in 3" :key="n" class="h-[5.6rem] rounded-2xl shimmer" />
          </div>
          <div v-else class="grid gap-3">
            <ChoiceTile
              v-for="option in options"
              :key="option.method"
              v-model="form.shippingMethod"
              :value="option.method"
              name="shippingMethod"
            >
              <span class="flex items-baseline justify-between gap-4">
                <span class="font-extrabold">{{ option.label }}</span>
                <span class="font-extrabold tabular">{{ Number(option.cost) === 0 ? "Free" : rand(option.cost) }}</span>
              </span>
              <span class="block text-chocolate/75">{{ option.eta }}</span>
              <span v-if="option.freeShipping" class="block text-sm font-bold mt-1">Free delivery applied</span>
              <span v-if="option.details" class="block text-sm mt-1 whitespace-pre-line">{{ option.details }}</span>
            </ChoiceTile>
          </div>
        </fieldset>

        <div v-if="form.shippingMethod === 'COURIER'" class="mt-8 grid gap-5">
          <h2 class="font-bold">Delivery address</h2>
          <p v-if="isLoggedIn && addressesLoading" role="status">Loading saved addresses…</p>
          <p v-if="isLoggedIn && addressesUnavailable" class="text-sm">Saved addresses could not be loaded. Enter your address below or <button type="button" class="underline min-h-11" @click="loadAddresses">try again</button>.</p>
          <fieldset v-if="isLoggedIn && savedAddresses.length" class="min-w-0">
            <legend class="font-semibold mb-3">Use a saved address</legend>
            <div class="grid sm:grid-cols-2 gap-3">
              <ChoiceTile v-for="saved in savedAddresses" :key="saved.id" :model-value="form.savedAddressId" :value="saved.id" name="savedAddress" class="min-w-0" @update:model-value="chooseAddress">
                <span class="block font-bold break-words">{{ saved.label }}{{ saved.isDefault ? ' · Default' : '' }}</span>
                <span class="block text-sm break-words">{{ saved.recipientName }} · {{ saved.address.streetNumber }} {{ saved.address.streetName }}, {{ saved.address.city }}, {{ saved.address.postalCode }}</span>
              </ChoiceTile>
            </div>
          </fieldset>
          <div class="grid grid-cols-[9rem_minmax(0,1fr)] gap-4 [&>*]:min-w-0">
            <BaseInput v-model="form.address.streetNumber" label="Number" autocomplete="address-line1" required :error="errors.streetNumber" />
            <BaseInput v-model="form.address.streetName" label="Street" required :error="errors.streetName" />
          </div>
          <BaseInput v-model="form.address.complex" label="Unit or complex (optional)" autocomplete="address-line2" />
          <BaseInput v-model="form.address.suburb" label="Suburb" autocomplete="address-level3" required :error="errors.suburb" />
          <div class="grid grid-cols-[minmax(0,1fr)_12rem] gap-4 [&>*]:min-w-0">
            <BaseInput v-model="form.address.city" label="City or town" autocomplete="address-level2" required :error="errors.city" />
            <BaseInput
              v-model="form.address.postalCode"
              label="Postal code"
              autocomplete="postal-code"
              required
              :error="errors.postalCode"
            />
          </div>
          <BaseInput
            v-model="form.address.province"
            label="Province"
            type="select"
            :options="PROVINCES.map(([value, label]) => ({ value, label }))"
          />
          <label v-if="isLoggedIn" class="flex items-start gap-3 min-h-11 cursor-pointer">
            <input v-model="form.saveAddress" type="checkbox" class="mt-1.5 h-5 w-5 shrink-0" />
            <span>Save this address to my account</span>
          </label>
        </div>

        <div v-if="form.shippingMethod === 'PUDO'" class="mt-8">
          <BaseInput
            v-model="form.pudoLocker"
            label="Which PUDO locker?"
            required
            :error="errors.pudoLocker"
            help="The locker's name or code, e.g. the one at your nearest shopping centre."
          />
        </div>

        <div v-if="askPhoneHere" class="mt-6">
          <BaseInput v-model="form.phone" label="Mobile number" type="tel" autocomplete="tel" required :error="errors.phone" />
        </div>

        <div v-if="form.shippingMethod" class="mt-6">
          <BaseInput
            v-model="form.note"
            label="Anything we should know? (optional)"
            type="textarea"
            :rows="3"
            help="A gate code, a gift message, a delivery window that suits you."
          />
        </div>

        <StickyTotalBar
          :label="form.shippingMethod ? 'Total incl. delivery' : 'So far'"
          :amount="summary?.total ?? null"
        >
          <BaseButton type="submit" size="lg" block class="h-14" :disabled="busy">Continue to payment</BaseButton>
        </StickyTotalBar>
      </form>

      <!-- Step 4: Pay -->
      <section v-else-if="step === 'pay' && summary" aria-labelledby="pay-heading">
        <h1 id="pay-heading" class="font-display text-display mt-8">Check it, then pay</h1>

        <div class="mt-8 divide-y divide-chocolate/[0.12] border-y border-chocolate/[0.12]">
          <section class="py-5" aria-labelledby="review-items">
            <div class="flex items-baseline justify-between">
              <h2 id="review-items" class="font-bold">Your cookies</h2>
              <RouterLink :to="{ name: 'Cart' }" class="text-sm font-bold underline">Edit</RouterLink>
            </div>
            <ul class="divide-y divide-chocolate/[0.08]">
              <CartLine v-for="line in summary.items" :key="line.cartItemId" :line="line" :editable="false" />
            </ul>
          </section>
          <section class="py-5" aria-labelledby="review-you">
            <div class="flex items-baseline justify-between">
              <h2 id="review-you" class="font-bold">You</h2>
              <RouterLink :to="{ name: 'Checkout', params: { step: 'you' } }" class="text-sm font-bold underline">Edit</RouterLink>
            </div>
            <p class="mt-1">{{ form.firstName }} {{ form.lastName }}</p>
            <p class="text-chocolate/80">{{ contactEmail }}<span v-if="form.phone"> · {{ form.phone }}</span></p>
          </section>
          <section class="py-5" aria-labelledby="review-delivery">
            <div class="flex items-baseline justify-between">
              <h2 id="review-delivery" class="font-bold">Delivery</h2>
              <RouterLink :to="{ name: 'Checkout', params: { step: 'delivery' } }" class="text-sm font-bold underline">Edit</RouterLink>
            </div>
            <p class="mt-1">{{ METHOD_NAMES[form.shippingMethod] }}<span v-if="chosen"> · {{ chosen.eta }}</span></p>
            <p v-if="form.shippingMethod === 'COURIER'" class="text-chocolate/80">{{ addressLines.join(", ") }}</p>
            <p v-else-if="form.shippingMethod === 'PUDO'" class="text-chocolate/80">{{ form.pudoLocker }}</p>
            <p v-else-if="chosen?.details" class="text-chocolate/80 whitespace-pre-line">{{ chosen.details }}</p>
            <p v-if="form.note" class="mt-1 text-sm">Note: {{ form.note }}</p>
          </section>
        </div>

        <dl class="mt-6 space-y-2">
          <div class="flex justify-between gap-4">
            <dt>Subtotal</dt>
            <dd class="tabular">{{ rand(summary.subtotal) }}</dd>
          </div>
          <div v-if="summary.discountTotal > 0" class="flex justify-between gap-4">
            <dt>Discount <span class="text-chocolate/70">({{ summary.discount.code }})</span></dt>
            <dd class="tabular">−{{ rand(summary.discountTotal) }}</dd>
          </div>
          <div class="flex justify-between gap-4">
            <dt>Delivery</dt>
            <dd class="tabular">{{ Number(summary.shipping) === 0 ? "Free" : rand(summary.shipping) }}</dd>
          </div>
          <div v-if="summary.vatIncluded" class="flex justify-between gap-4 text-sm text-chocolate/75">
            <dt>Includes VAT</dt>
            <dd class="tabular">{{ rand(summary.vatIncluded) }}</dd>
          </div>
        </dl>

        <div class="mt-8 rounded-2xl bg-cream-100/60 border border-cream-500 p-5 text-sm space-y-2">
          <p>You'll pay on PayFast's secure page: card, Instant EFT, SnapScan and other options. We never see your card details.</p>
          <p>Every order is baked fresh, so delivery times are estimates.</p>
          <p v-if="site?.address">Sold by GuiltFreeCookie, {{ site.address }}.</p>
          <p>
            By paying you agree to our
            <RouterLink :to="{ name: 'Terms' }" class="font-bold underline">terms</RouterLink> and
            <RouterLink :to="{ name: 'RefundPolicy' }" class="font-bold underline">refund policy</RouterLink>.
            <template v-if="site?.supportEmail">
              Questions? <a :href="`mailto:${site.supportEmail}`" class="font-bold underline">{{ site.supportEmail }}</a>
            </template>
          </p>
        </div>

        <StickyTotalBar label="Total to pay" :amount="summary.total">
          <BaseButton size="lg" block class="h-14" :disabled="busy || !summary.canCheckout" :loading="busy" @click="pay">
            {{ busy ? "Taking you to PayFast…" : `Pay ${rand(summary.total)}` }}
          </BaseButton>
        </StickyTotalBar>
      </section>
    </div>
  </div>
</template>
