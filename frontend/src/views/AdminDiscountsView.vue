<script setup>
import { computed, nextTick, onMounted, reactive, ref } from "vue";
import BaseButton from "../components/ui/BaseButton.vue";
import BaseInput from "../components/ui/BaseInput.vue";
import ChoiceTile from "../components/checkout/ChoiceTile.vue";
import { discountApi } from "../api/discountApi";
import { errorMessage } from "../api/storeApi";
import { useFormat } from "../composables/useFormat";

const TYPES = [
  { value: "PERCENT", label: "Percent off", hint: "e.g. 10% off the cookies" },
  { value: "FIXED", label: "Rand off", hint: "e.g. R50 off the order" },
  { value: "FREE_SHIPPING", label: "Free delivery", hint: "Courier and PUDO cost nothing" },
];
const STATUS = {
  ACTIVE: { label: "Active", badge: "bg-primary text-chocolate" },
  SCHEDULED: { label: "Scheduled", badge: "bg-tan-light text-chocolate" },
  EXPIRED: { label: "Expired", badge: "bg-chocolate/[0.07] text-chocolate/80" },
  USED_UP: { label: "Used up", badge: "bg-chocolate/[0.07] text-chocolate/80" },
  OFF: { label: "Off", badge: "border border-chocolate/30 text-chocolate/80" },
};

const { rand } = useFormat();
const codes = ref([]);
const loaded = ref(false);
const loadError = ref("");
const notice = ref("");

const editing = ref(null); // null = closed, "new" or a discountId
const form = reactive(blank());
const formError = ref("");
const saving = ref(false);
const showRules = ref(false);
const confirming = ref(null);
const switching = ref(false);

const shopCodes = computed(() => codes.value.filter((code) => !code.restrictedToEmail));
const personalCodes = computed(() => codes.value.filter((code) => code.restrictedToEmail));

function blank() {
  return {
    code: "",
    type: "PERCENT",
    value: "",
    description: "",
    startDate: "",
    endDate: "",
    minSubtotal: "",
    maxUses: "",
    onePerEmail: false,
    restrictedToEmail: "",
    active: true,
    // The dates as loaded, so saving an untouched date keeps its exact time (flow codes end mid-day).
    original: { startDate: "", startsAt: null, endDate: "", endsAt: null },
  };
}

async function load() {
  loadError.value = "";
  try {
    codes.value = await discountApi.list();
    loaded.value = true;
  } catch (e) {
    loadError.value = errorMessage(e);
  }
}
onMounted(load);

function status(code) {
  const now = new Date();
  if (!code.active) return "OFF";
  if (code.maxUses != null && code.usedCount >= code.maxUses) return "USED_UP";
  if (code.endsAt && new Date(code.endsAt) <= now) return "EXPIRED";
  if (code.startsAt && new Date(code.startsAt) > now) return "SCHEDULED";
  return "ACTIVE";
}

function offer(code) {
  const what =
    code.type === "PERCENT" ? `${Number(code.value)}% off` : code.type === "FIXED" ? `${rand(code.value)} off` : "Free delivery";
  return code.minSubtotal ? `${what} on orders from ${rand(code.minSubtotal)}` : what;
}

// Codes end at midnight at the start of endsAt, so the last day they work is the day before.
const dateOnly = (value) => (value ? value.slice(0, 10) : "");
function lastDay(endsAt) {
  if (!endsAt) return "";
  const end = new Date(endsAt);
  if (end.getHours() === 0 && end.getMinutes() === 0) end.setDate(end.getDate() - 1);
  return end.toLocaleDateString("en-CA");
}
const nice = (iso) => new Date(`${iso}T12:00:00`).toLocaleDateString("en-ZA", { day: "numeric", month: "short", year: "numeric" });

function runs(code) {
  const start = code.startsAt && new Date(code.startsAt) > new Date() ? `Starts ${nice(dateOnly(code.startsAt))}` : "";
  const end = code.endsAt ? `until ${nice(lastDay(code.endsAt))}` : "";
  if (start && end) return `${start}, ${end}`;
  if (start) return start;
  if (end) return end.charAt(0).toUpperCase() + end.slice(1);
  return "No end date";
}

function uses(code) {
  return code.maxUses != null ? `${code.usedCount} of ${code.maxUses} used` : `${code.usedCount} used`;
}

async function open(code) {
  Object.assign(form, blank());
  formError.value = "";
  notice.value = "";
  confirming.value = null;
  if (code) {
    Object.assign(form, {
      code: code.code,
      type: code.type,
      value: code.value ?? "",
      description: code.description || "",
      startDate: dateOnly(code.startsAt),
      endDate: lastDay(code.endsAt),
      minSubtotal: code.minSubtotal ?? "",
      maxUses: code.maxUses ?? "",
      onePerEmail: code.onePerEmail,
      restrictedToEmail: code.restrictedToEmail || "",
      active: code.active,
      original: { startDate: dateOnly(code.startsAt), startsAt: code.startsAt, endDate: lastDay(code.endsAt), endsAt: code.endsAt },
    });
  }
  showRules.value = !!(form.minSubtotal || form.maxUses || form.onePerEmail || form.restrictedToEmail);
  editing.value = code ? code.discountId : "new";
  await nextTick();
  document.getElementById("discount-form")?.scrollIntoView({ behavior: "smooth", block: "start" });
}

function close() {
  editing.value = null;
}

function dayAfter(iso) {
  const d = new Date(`${iso}T12:00:00`);
  d.setDate(d.getDate() + 1);
  return `${d.toLocaleDateString("en-CA")}T00:00:00`;
}

const number = (value) => (value === "" || value == null ? null : Number(value));

async function save() {
  formError.value = "";
  if (form.startDate && form.endDate && form.endDate < form.startDate) {
    formError.value = "The last day can't be before the first day.";
    return;
  }
  saving.value = true;
  const body = {
    code: form.code.trim(),
    type: form.type,
    value: form.type === "FREE_SHIPPING" ? null : number(form.value),
    minSubtotal: number(form.minSubtotal),
    startsAt: !form.startDate ? null : form.startDate === form.original.startDate ? form.original.startsAt : `${form.startDate}T00:00:00`,
    endsAt: !form.endDate ? null : form.endDate === form.original.endDate ? form.original.endsAt : dayAfter(form.endDate),
    maxUses: number(form.maxUses),
    onePerEmail: form.onePerEmail,
    restrictedToEmail: form.restrictedToEmail.trim() || null,
    description: form.description.trim() || null,
    active: form.active,
  };
  try {
    const saved = editing.value === "new" ? await discountApi.create(body) : await discountApi.update(editing.value, body);
    codes.value = editing.value === "new"
      ? [saved, ...codes.value]
      : codes.value.map((code) => (code.discountId === saved.discountId ? saved : code));
    notice.value = `${saved.code} saved.`;
    editing.value = null;
  } catch (e) {
    formError.value = errorMessage(e);
  } finally {
    saving.value = false;
  }
}

async function switchOff(code) {
  switching.value = true;
  try {
    const saved = await discountApi.switchOff(code.discountId);
    codes.value = codes.value.map((existing) => (existing.discountId === code.discountId ? { ...existing, ...(saved || { active: false }) } : existing));
    notice.value = `${code.code} is switched off.`;
    confirming.value = null;
  } catch (e) {
    notice.value = errorMessage(e);
  } finally {
    switching.value = false;
  }
}
</script>

<template>
  <div class="max-w-4xl mx-auto">
    <div class="flex flex-wrap items-end justify-between gap-4">
      <h1 class="font-display text-display">Discount codes</h1>
      <BaseButton v-if="editing !== 'new'" @click="open(null)">New code</BaseButton>
    </div>
    <p class="mt-3 text-chocolate/80 max-w-[60ch]">
      Codes shoppers type in their cart. A use is counted when an order is paid, not when the code is applied.
    </p>

    <p v-if="notice" role="status" class="mt-5 rounded-xl bg-primary/25 px-4 py-3 font-bold">{{ notice }}</p>
    <p v-if="loadError" role="alert" class="mt-6 rounded-xl bg-red-800/[0.08] px-4 py-3 text-red-900">
      {{ loadError }} <button type="button" class="font-bold underline" @click="load">Try again</button>
    </p>

    <form
      v-if="editing"
      id="discount-form"
      class="mt-8 rounded-2xl border border-chocolate/15 bg-cream-100/70 p-5 md:p-7 grid gap-5 [&>*]:min-w-0 scroll-mt-24"
      @submit.prevent="save"
    >
      <h2 class="text-[2rem] font-extrabold">{{ editing === "new" ? "New code" : `Edit ${form.code}` }}</h2>

      <BaseInput
        :model-value="form.code"
        label="Code"
        required
        help="3–30 letters, numbers, - or _. Shoppers can type it in any case."
        @update:model-value="form.code = $event.toUpperCase().replace(/\s+/g, '')"
      />

      <fieldset class="min-w-0">
        <legend class="text-sm font-bold mb-2">What it gives</legend>
        <div class="grid gap-3 sm:grid-cols-3">
          <ChoiceTile v-for="type in TYPES" :key="type.value" v-model="form.type" :value="type.value" name="discount-type">
            <span class="block font-bold">{{ type.label }}</span>
            <span class="block text-sm text-chocolate/75">{{ type.hint }}</span>
          </ChoiceTile>
        </div>
      </fieldset>

      <div v-if="form.type !== 'FREE_SHIPPING'" class="max-w-[16rem]">
        <BaseInput v-model="form.value" :label="form.type === 'PERCENT' ? 'Percent off (%)' : 'Amount off (R)'" type="number" required />
      </div>

      <BaseInput v-model="form.description" label="Note for the team (optional)" help="Shown in this list only, e.g. “Spring box promotion”." />

      <div class="grid gap-4 sm:grid-cols-2 [&>*]:min-w-0">
        <BaseInput v-model="form.startDate" label="First day (optional)" type="date" help="Leave empty to start now." />
        <BaseInput v-model="form.endDate" label="Last day (optional)" type="date" help="It works until the end of this day." />
      </div>

      <div>
        <button
          type="button"
          class="font-bold underline underline-offset-4 min-h-11"
          :aria-expanded="showRules"
          aria-controls="more-rules"
          @click="showRules = !showRules"
        >
          {{ showRules ? "Hide extra rules" : "More rules" }}
        </button>
        <div v-if="showRules" id="more-rules" class="mt-3 grid gap-4 sm:grid-cols-2 [&>*]:min-w-0">
          <BaseInput v-model="form.minSubtotal" label="Minimum order (R)" type="number" help="Before delivery, after other discounts." />
          <BaseInput v-model="form.maxUses" label="Total uses allowed" type="number" help="1 makes it single-use. Empty means no limit." />
          <BaseInput v-model="form.restrictedToEmail" label="Only for this email" type="email" help="Makes it a personal code." />
          <label class="flex items-start gap-3 min-h-11 cursor-pointer sm:pt-8">
            <input v-model="form.onePerEmail" type="checkbox" class="mt-1.5 h-5 w-5 shrink-0" />
            <span>Once per customer</span>
          </label>
        </div>
      </div>

      <label v-if="editing !== 'new'" class="flex items-start gap-3 min-h-11 cursor-pointer">
        <input v-model="form.active" type="checkbox" class="mt-1.5 h-5 w-5 shrink-0" />
        <span>Active <span class="text-chocolate/70">(untick to switch it off)</span></span>
      </label>

      <p v-if="formError" role="alert" class="rounded-xl bg-red-800/[0.08] px-4 py-3 text-red-900">{{ formError }}</p>
      <div class="flex flex-wrap gap-3">
        <BaseButton type="submit" :disabled="saving">{{ saving ? "Saving…" : "Save code" }}</BaseButton>
        <BaseButton type="button" variant="outline" @click="close">Cancel</BaseButton>
      </div>
    </form>

    <div v-if="!loaded && !loadError" class="mt-8 space-y-3" aria-label="Loading codes">
      <div v-for="n in 3" :key="n" class="h-20 rounded-2xl shimmer" />
    </div>

    <template v-else-if="loaded">
      <section class="mt-10" aria-labelledby="shop-heading">
        <h2 id="shop-heading" class="text-[2rem] font-extrabold">Shop codes</h2>
        <div v-if="!shopCodes.length" class="mt-4">
          <p class="text-chocolate/80">No discount codes yet.</p>
          <BaseButton v-if="!editing" class="mt-4" @click="open(null)">Create a code</BaseButton>
        </div>
        <ul v-else class="mt-4 divide-y divide-chocolate/[0.12] border-y border-chocolate/[0.12]">
          <li v-for="code in shopCodes" :key="code.discountId" class="py-4">
            <div class="flex flex-wrap items-start justify-between gap-x-6 gap-y-3">
              <div class="min-w-0">
                <p class="flex flex-wrap items-center gap-2">
                  <span class="font-extrabold tabular tracking-wide break-all">{{ code.code }}</span>
                  <span :class="['inline-flex rounded-full px-3 py-0.5 text-sm font-extrabold', STATUS[status(code)].badge]">
                    {{ STATUS[status(code)].label }}
                  </span>
                </p>
                <p class="mt-1">{{ offer(code) }}</p>
                <p class="mt-0.5 text-sm text-chocolate/75">
                  {{ runs(code) }} · {{ uses(code) }}<template v-if="code.onePerEmail"> · once per customer</template>
                </p>
                <p v-if="code.description" class="mt-0.5 text-sm text-chocolate/75 break-words">{{ code.description }}</p>
              </div>
              <div class="flex gap-4 shrink-0">
                <button type="button" class="font-bold underline min-h-11" @click="open(code)">Edit</button>
                <button
                  v-if="code.active"
                  type="button"
                  class="font-bold underline min-h-11"
                  :aria-expanded="confirming === code.discountId"
                  @click="confirming = confirming === code.discountId ? null : code.discountId"
                >
                  Switch off
                </button>
              </div>
            </div>
            <div v-if="confirming === code.discountId" class="mt-3 rounded-xl bg-chocolate/[0.06] p-4">
              <p>Switch off {{ code.code }}? Shoppers can't use it any more. Past orders keep their discount, and you can turn it back on by editing it.</p>
              <div class="mt-3 flex flex-wrap gap-3">
                <BaseButton size="sm" :disabled="switching" @click="switchOff(code)">Switch it off</BaseButton>
                <BaseButton size="sm" variant="outline" @click="confirming = null">Keep it on</BaseButton>
              </div>
            </div>
          </li>
        </ul>
      </section>

      <section v-if="personalCodes.length" class="mt-10" aria-labelledby="personal-heading">
        <details>
          <summary class="cursor-pointer min-h-11 flex items-center gap-2">
            <h2 id="personal-heading" class="text-[2rem] font-extrabold inline">Personal codes</h2>
            <span class="text-chocolate/75">({{ personalCodes.length }})</span>
          </summary>
          <p class="mt-2 text-sm text-chocolate/75">
            Single-person codes, mostly sent by the email flows (welcome, cart reminder, win back). Each only works for its email.
          </p>
          <ul class="mt-4 divide-y divide-chocolate/[0.12] border-y border-chocolate/[0.12]">
            <li v-for="code in personalCodes" :key="code.discountId" class="py-3 flex flex-wrap items-start justify-between gap-x-6 gap-y-2">
              <div class="min-w-0">
                <p class="flex flex-wrap items-center gap-2">
                  <span class="font-extrabold tabular tracking-wide break-all">{{ code.code }}</span>
                  <span :class="['inline-flex rounded-full px-3 py-0.5 text-sm font-extrabold', STATUS[status(code)].badge]">
                    {{ STATUS[status(code)].label }}
                  </span>
                </p>
                <p class="mt-1 text-sm">{{ offer(code) }} · {{ runs(code) }} · {{ uses(code) }}</p>
                <p class="mt-0.5 text-sm text-chocolate/75 break-all">{{ code.restrictedToEmail }}</p>
              </div>
              <button v-if="code.active" type="button" class="font-bold underline min-h-11 shrink-0" @click="switchOff(code)">Switch off</button>
            </li>
          </ul>
        </details>
      </section>
    </template>
  </div>
</template>
