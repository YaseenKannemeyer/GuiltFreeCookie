<script setup>
import { computed, ref, reactive, onMounted, onBeforeUnmount, watch, nextTick } from "vue";
import { useRoute } from "vue-router";
import { ArrowRightIcon } from "@heroicons/vue/24/outline";
import { gsap } from "gsap";
import { api, errorMessage } from "../api/storeApi";
import cookieApi from "../api/cookieApi";
import BaseButton from "../components/ui/BaseButton.vue";
import BaseInput from "../components/ui/BaseInput.vue";
import MagneticDropZone from "../components/ui/MagneticDropZone.vue";

const reasonLabels = {
  RESTOCK: "Restock",
  ADJUSTMENT: "Correction",
  STOCKTAKE: "Stocktake",
  DAMAGED: "Damaged",
  ORDER_RESERVED: "Order",
  ORDER_RELEASED: "Order cancelled",
  REFUNDED: "Refund",
};
const route = useRoute(),
  cookies = ref([]),
  selected = ref(route.query.cookie || ""),
  current = ref(null),
  busy = ref(false),
  error = ref(""),
  notice = ref(""),
  history = ref(null),
  showHistory = ref(false);
const stock = reactive({
    mode: "adjustBy",
    value: "",
    reason: "RESTOCK",
    note: "",
  }),
  details = reactive({}),
  pack = reactive({
    label: "",
    unitsPerPack: 1,
    price: "",
    active: true,
    sortOrder: 0,
  }),
  editingPack = ref(""),
  altText = ref(""),
  file = ref(null);
const detailFields = [
  ["lowStockThreshold", "Low-stock threshold"],
  ["weightGrams", "Weight (g)"],
  ["calories", "Calories (kcal)"],
  ["sugarGrams", "Sugar (g)"],
  ["proteinGrams", "Protein (g)"],
  ["carbsGrams", "Carbohydrates (g)"],
  ["fatGrams", "Fat (g)"],
];
const historyDialog = ref(null);
const activeSection = ref("stock");
const sections = [
  { id: "stock", label: "Restock" },
  { id: "details", label: "Product details" },
  { id: "packs", label: "Pack sizes" },
  { id: "photos", label: "Photos" },
];
const nextQuantity = computed(() => {
  if (stock.value === "" || !Number.isInteger(Number(stock.value))) return null;
  return stock.mode === "setTo"
    ? Number(stock.value)
    : Number(current.value?.cookie.stockQuantity || 0) + Number(stock.value);
});
const stockIsLow = computed(() => current.value &&
  current.value.cookie.stockQuantity <= (current.value.cookie.lowStockThreshold ?? 0));
const quantityPreview = ref(null);
let previewTween;
watch(nextQuantity, async () => {
  await nextTick();
  previewTween?.kill();
  if (!quantityPreview.value || window.matchMedia("(prefers-reduced-motion: reduce)").matches) return;
  previewTween = gsap.fromTo(quantityPreview.value, { y: 4 }, { y: 0, duration: 0.25, ease: "power3.out", clearProps: "transform" });
});
onBeforeUnmount(() => previewTween?.kill());
watch(showHistory, async (open) => {
  await nextTick();
  if (open) historyDialog.value?.showModal();
  else historyDialog.value?.close();
});
const base = () => `/admin/cookies/${encodeURIComponent(selected.value)}`;
let version = 0;
async function load() {
  error.value = "";
  try {
    cookies.value = await api("get", "/admin/cookies");
    if (!selected.value && cookies.value.length)
      selected.value = cookies.value[0].cookie.cookieId;
    else if (selected.value) await detail();
  } catch (e) {
    error.value = errorMessage(e);
  }
}
async function detail(refresh = false) {
  const n = ++version;
  if (!refresh) current.value = null;
  showHistory.value = false;
  history.value = null;
  resetPack();
  try {
    const data = await api("get", base());
    if (n !== version) return;
    current.value = data;
    Object.assign(details, {
      slug: data.cookie.slug,
      featured: data.cookie.featured,
    });
    for (const [key] of detailFields) details[key] = data.cookie[key] ?? "";
  } catch (e) {
    if (n === version) error.value = errorMessage(e);
  }
}
async function action(fn, message) {
  if (busy.value) return;
  busy.value = true;
  error.value = "";
  notice.value = "";
  try {
    await fn();
    notice.value = message;
    await detail(true);
  } catch (e) {
    error.value = errorMessage(e);
  } finally {
    busy.value = false;
  }
}
function adjust() {
  const value = Number(stock.value);
  if (
    stock.value === "" ||
    !Number.isInteger(value) ||
    (stock.mode === "setTo" && value < 0)
  ) {
    error.value = "Enter a whole number of cookies.";
    return;
  }
  action(
    async () => {
      await api("put", `${base()}/stock`, {
        [stock.mode]: value,
        reason: stock.mode === "setTo" ? "STOCKTAKE" : stock.reason,
        note: stock.note,
      });
      stock.value = "";
      stock.note = "";
    },
    "Stock updated.",
  );
}
function saveDetails() {
  const data = { slug: details.slug, featured: details.featured };
  for (const [key] of detailFields)
    data[key] = details[key] === "" ? null : Number(details[key]);
  action(() => api("put", `${base()}/details`, data), "Details saved.");
}
function resetPack() {
  editingPack.value = "";
  Object.assign(pack, {
    label: "",
    unitsPerPack: 1,
    price: "",
    active: true,
    sortOrder: 0,
  });
}
function editPack(v) {
  editingPack.value = v.variantId;
  Object.assign(pack, {
    label: v.label,
    unitsPerPack: v.unitsPerPack,
    price: v.price ?? "",
    active: v.active,
    sortOrder: v.sortOrder,
  });
}
function savePack() {
  action(
    () =>
      api(
        editingPack.value ? "put" : "post",
        `${base()}/variants${editingPack.value ? "/" + editingPack.value : ""}`,
        {
          ...pack,
          unitsPerPack: Number(pack.unitsPerPack),
          price: pack.price === "" ? null : Number(pack.price),
          sortOrder: Number(pack.sortOrder),
        },
      ),
    "Pack saved.",
  );
}
function upload() {
  if (!file.value) {
    error.value = "Choose an image first.";
    return;
  }
  const data = new FormData();
  data.append("image", file.value);
  data.append("altText", altText.value);
  action(async () => {
    await api("post", `${base()}/images`, data);
    file.value = null;
    altText.value = "";
  }, "Photo uploaded.");
}
function reorder(index, direction) {
  const ids = current.value.images.map((i) => i.imageId);
  [ids[index], ids[index + direction]] = [ids[index + direction], ids[index]];
  action(
    () => api("put", `${base()}/images/order`, { imageIds: ids }),
    "Gallery order saved.",
  );
}
async function openHistory() {
  showHistory.value = true;
  try {
    history.value = await api("get", `${base()}/inventory`);
  } catch (e) {
    error.value = errorMessage(e);
  }
}
watch(selected, () => {
  error.value = "";
  notice.value = "";
  stock.value = "";
  stock.note = "";
  detail();
});
onMounted(load);
</script>
<template>
  <div class="max-w-6xl mx-auto">
    <h1 class="font-display text-display">Stock & packs</h1>
    <p class="mt-3 text-chocolate/75 max-w-prose">
      Record a fresh bake, check your counts, or update a cookie’s details.
    </p>
    <div class="mt-7 border-y border-chocolate/15 py-5 grid gap-5 md:grid-cols-[minmax(0,1fr)_auto] md:items-center">
    <div class="flex items-center gap-4 min-w-0">
      <img v-if="current?.cookie.image"
        :src="cookieApi.getImageUrl(current.cookie.image)" alt=""
        class="hidden sm:block w-20 h-20 shrink-0 rounded-xl object-contain bg-tan" />
      <div class="flex-1 min-w-0">
      <label for="inventory-cookie" class="block text-sm font-semibold mb-1.5">Choose a cookie</label>
      <select id="inventory-cookie"
        v-model="selected"
        class="field h-14"
        :disabled="busy"
      >
        <option
          v-for="v in cookies"
          :key="v.cookie.cookieId"
          :value="v.cookie.cookieId"
        >
          {{ v.cookie.description }}{{ v.cookie.archived ? " (archived)" : "" }}
        </option>
      </select>
      </div>
    </div>
    <div v-if="current" class="flex flex-wrap items-center gap-x-5 gap-y-2 md:justify-end">
      <p class="font-bold tabular">{{ current.cookie.stockQuantity }} <span class="font-normal text-chocolate/75">in stock</span></p>
      <span v-if="stockIsLow" class="text-sm font-bold text-red-800">{{ current.cookie.stockQuantity === 0 ? 'Out of stock' : 'Low stock' }}</span>
      <span v-else class="text-sm font-semibold text-chocolate/75">In stock</span>
    </div>
    </div>
    <p v-if="error" role="alert" class="mt-5 text-red-800">
      {{ error }} <button class="underline" @click="load">Reload</button>
    </p>
    <p v-if="notice" role="status" class="mt-5 rounded-xl bg-primary/25 px-4 py-3 font-semibold motion-safe:animate-fade-in">{{ notice }}</p>
    <p v-if="busy" role="status" class="mt-5">Saving changes…</p>
    <template v-if="current">
      <nav class="mt-6 grid grid-cols-2 sm:grid-cols-4 gap-1 rounded-xl bg-chocolate/[0.05] p-1" aria-label="Cookie management">
        <button v-for="section in sections" :key="section.id" type="button"
          :aria-pressed="activeSection === section.id" :aria-controls="`inventory-${section.id}`"
          :class="activeSection === section.id ? 'bg-chocolate text-cream-50' : 'text-chocolate/75 hover:bg-chocolate/[0.07]'"
          class="min-h-12 rounded-lg px-3 py-2 text-sm font-bold transition-colors focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-chocolate"
          @click="activeSection = section.id">{{ section.label }}</button>
      </nav>
      <section id="inventory-stock" v-show="activeSection === 'stock'" class="py-8" aria-labelledby="restock-title">
        <div class="flex items-start justify-between flex-wrap gap-4">
          <div>
            <h2 id="restock-title" class="text-[2.4rem] leading-tight font-extrabold">Update stock</h2>
            <p class="mt-2 text-sm text-chocolate/75">Count individual cookies, including those sold in packs.</p>
          </div>
          <BaseButton variant="outline" size="sm" @click="openHistory"
            >Stock history</BaseButton
          >
        </div>
        <form class="mt-6 grid gap-6 lg:grid-cols-[minmax(0,1fr)_minmax(24rem,0.7fr)] lg:gap-8" @submit.prevent="adjust">
          <fieldset :disabled="busy" class="min-w-0 grid content-start sm:grid-cols-2 gap-5">
          <legend class="sr-only">Stock adjustment</legend>
          <div class="min-w-0">
            <label for="stock-mode" class="block text-sm font-semibold mb-1.5">Action</label>
            <select id="stock-mode" v-model="stock.mode" class="field h-14">
              <option value="adjustBy">Add / remove cookies</option>
              <option value="setTo">Set exact stock count</option>
            </select>
          </div>
          <div v-if="stock.mode === 'adjustBy'" class="min-w-0">
            <label for="stock-reason" class="block text-sm font-semibold mb-1.5">Reason</label>
            <select id="stock-reason" v-model="stock.reason" class="field h-14">
              <option value="RESTOCK">Fresh bake</option>
              <option value="ADJUSTMENT">Correction</option>
              <option value="DAMAGED">Damaged or discarded</option>
            </select>
          </div>
          <div class="sm:col-span-2 min-w-0">
            <label for="stock-quantity" class="block text-sm font-semibold mb-1.5">{{ stock.mode === 'setTo' ? 'Counted cookies' : 'Cookies to add or remove' }}</label>
            <input id="stock-quantity"
              v-model="stock.value"
              type="number"
              step="1"
              :min="stock.mode === 'setTo' ? 0 : undefined"
              class="field h-16 text-[2.4rem] font-bold tabular"
              placeholder="0" aria-describedby="stock-help" required />
            <p id="stock-help" class="mt-2 text-sm text-chocolate/75">{{ stock.mode === 'setTo' ? 'This replaces the current stock count.' : 'Use a positive number to add cookies, or a negative number to remove them.' }}</p>
          </div>
          <div class="sm:col-span-2"><BaseInput v-model="stock.note" label="Note (optional)" placeholder="e.g. Saturday morning bake" :disabled="busy" /></div>
          </fieldset>
          <div class="min-w-0 border-t border-chocolate/15 pt-5 lg:border-t-0 lg:border-l lg:pl-8 lg:pt-0">
            <h3 class="font-bold">After this update</h3>
            <div class="mt-5 flex items-center gap-4" aria-live="polite" aria-atomic="true">
              <div class="min-w-0 flex-1"><span class="block text-sm text-chocolate/70">Current</span><span class="block mt-1 text-[3.2rem] leading-tight font-bold tabular break-all">{{ current.cookie.stockQuantity }}</span></div>
              <ArrowRightIcon class="h-5 w-5 shrink-0 text-chocolate/50" aria-hidden="true" />
              <div class="min-w-0 flex-1"><span class="block text-sm text-chocolate/70">New total</span><span ref="quantityPreview" class="block mt-1 text-[3.2rem] leading-tight font-bold tabular break-all">{{ nextQuantity ?? '—' }}</span></div>
            </div>
            <p class="mt-4 text-sm text-chocolate/75">Low-stock alert at {{ current.cookie.lowStockThreshold ?? 0 }} cookies.</p>
            <BaseButton type="submit" class="mt-6 w-full" :loading="busy" :disabled="stock.value === ''">Save stock update</BaseButton>
            <p class="mt-3 text-sm text-chocolate/70">Every update is recorded in stock history.</p>
          </div>
        </form>
      </section>
      <section id="inventory-details" v-show="activeSection === 'details'" class="py-8">
        <h2 class="text-[2.4rem] leading-tight font-extrabold">Product details</h2>
        <form
          class="mt-5 grid sm:grid-cols-2 gap-4"
          @submit.prevent="saveDetails"
        >
          <BaseInput v-model="details.slug" label="URL slug" required /><label
            class="flex items-center gap-3 min-h-11"
            ><input v-model="details.featured" type="checkbox" />Featured on the
            homepage</label
          ><label v-for="[key, label] in detailFields" :key="key"
            >{{ label
            }}<input
              v-model="details[key]"
              type="number"
              min="0"
              :step="['lowStockThreshold', 'calories'].includes(key) ? 1 : 0.1"
              class="field mt-1"
          /></label>
          <div class="sm:col-span-2">
            <BaseButton type="submit" :disabled="busy">Save details</BaseButton>
          </div>
        </form>
      </section>
      <section id="inventory-packs" v-show="activeSection === 'packs'" class="py-8">
        <h2 class="text-[2.4rem] leading-tight font-extrabold">Pack sizes</h2>
        <ul class="mt-4 divide-y divide-chocolate/15">
          <li
            v-for="v in current.variants"
            :key="v.variantId"
            class="py-4 flex flex-wrap justify-between items-center gap-3"
          >
            <span
              >{{ v.label }} · {{ v.unitsPerPack }}
              {{ v.unitsPerPack === 1 ? "cookie" : "cookies" }} ·
              {{
                v.price == null
                  ? "Unit price × pack size"
                  : `R ${Number(v.price).toFixed(2)}`
              }}
              · {{ v.active ? "Active" : "Hidden" }}</span
            >
            <div class="flex gap-3">
              <button
                class="underline min-h-11"
                :disabled="busy"
                @click="editPack(v)"
              >
                Edit</button
              ><button
                v-if="v.active"
                class="underline min-h-11"
                :disabled="busy"
                @click="
                  action(
                    () => api('delete', `${base()}/variants/${v.variantId}`),
                    'Pack hidden.',
                  )
                "
              >
                Hide
              </button>
            </div>
          </li>
        </ul>
        <form class="grid sm:grid-cols-2 gap-4 mt-6" @submit.prevent="savePack">
          <h3 class="sm:col-span-2 font-bold">
            {{ editingPack ? "Edit pack" : "Add a pack" }}
          </h3>
          <BaseInput v-model="pack.label" label="Pack label" required /><label
            >Cookies per pack<input
              v-model="pack.unitsPerPack"
              type="number"
              min="1"
              step="1"
              required
              class="field mt-1" /></label
          ><label
            >Price (R, blank = unit price × pack size)<input
              v-model="pack.price"
              type="number"
              min="0.01"
              step="0.01"
              class="field mt-1" /></label
          ><label
            >Display order<input
              v-model="pack.sortOrder"
              type="number"
              min="0"
              step="1"
              class="field mt-1" /></label
          ><label class="flex items-center gap-3 min-h-11"
            ><input v-model="pack.active" type="checkbox" />Active</label
          >
          <div class="sm:col-span-2 flex flex-wrap gap-3">
            <BaseButton type="submit" :disabled="busy">Save pack</BaseButton
            ><BaseButton v-if="editingPack" variant="outline" @click="resetPack"
              >Cancel edit</BaseButton
            >
          </div>
        </form>
      </section>
      <section id="inventory-photos" v-show="activeSection === 'photos'" class="py-8">
        <h2 class="text-[2.4rem] leading-tight font-extrabold">Gallery</h2>
        <p class="mt-3">
          The primary image stays first. Reorder the additional photos below.
        </p>
        <ol class="mt-6 space-y-4">
          <li
            v-for="(photo, i) in current.images"
            :key="photo.imageId"
            class="grid grid-cols-[80px_minmax(0,1fr)] sm:flex sm:flex-wrap items-center gap-4"
          >
            <img
              :src="cookieApi.getImageUrl(photo.image)"
              :alt="photo.altText || current.cookie.description"
              class="w-24 h-28 object-contain bg-tan rounded-xl"
            /><span class="sm:flex-1 min-w-0 break-words">{{
              photo.altText || "Product photo"
            }}</span
            ><button
              class="underline min-h-11"
              :disabled="busy || i === 0"
              @click="reorder(i, -1)"
            >
              Move up</button
            ><button
              class="underline min-h-11"
              :disabled="busy || i === current.images.length - 1"
              @click="reorder(i, 1)"
            >
              Move down</button
            ><button
              class="underline min-h-11"
              :disabled="busy"
              @click="
                action(
                  () => api('delete', `${base()}/images/${photo.imageId}`),
                  'Photo removed.',
                )
              "
            >
              Remove
            </button>
          </li>
        </ol>
        <form class="space-y-4 mt-7" @submit.prevent="upload">
          <div>
            <p class="mb-1.5 text-sm font-semibold text-chocolate/85">New photo</p>
            <MagneticDropZone
              :model-value="file ? [file] : []"
              accept="image/jpeg,image/png,image/webp,image/gif"
              :max-size="10 * 1024 * 1024"
              label="Drop a photo here"
              @update:model-value="file = $event[0] ?? null"
            />
          </div><BaseInput v-model="altText" label="Describe the photo" /><BaseButton
            type="submit"
            :disabled="busy || current.images.length >= 10"
            >Upload photo</BaseButton
          >
        </form>
      </section></template
    >
    <dialog
      ref="historyDialog"
      class="fixed inset-y-0 left-auto right-0 m-0 max-h-none max-w-full h-dvh w-full sm:max-w-xl bg-surface text-chocolate shadow-soft-lg p-6 overflow-y-auto backdrop:bg-chocolate/50"
      aria-labelledby="history-title"
      @close="showHistory = false"
    >
      <div class="flex items-center justify-between gap-4">
        <h2 id="history-title" class="text-[2.4rem] leading-tight font-extrabold">Stock history</h2>
        <BaseButton variant="outline" @click="showHistory = false"
          >Close</BaseButton
        >
      </div>
      <p v-if="!history" class="mt-5">Loading history…</p>
      <p v-else-if="!history.length" class="mt-5">No stock movements yet.</p>
      <article
        v-for="(movement, i) in history"
        :key="i"
        class="py-5 border-b border-chocolate/15"
      >
        <p class="font-bold">
          {{ movement.quantityChange > 0 ? "+" : ""
          }}{{ movement.quantityChange }}
          {{ Math.abs(movement.quantityChange) === 1 ? "cookie" : "cookies" }} ·
          {{ movement.stockAfter }} remaining
        </p>
        <p>
          {{ reasonLabels[movement.reason] || movement.reason }} ·
          {{ new Date(movement.createdAt).toLocaleString("en-ZA") }}
        </p>
        <p class="mt-2 break-words">{{ movement.note }}</p>
        <p class="text-sm">By {{ movement.actor }}</p>
      </article>
    </dialog>
  </div>
</template>
