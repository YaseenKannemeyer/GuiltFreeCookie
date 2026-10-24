<script setup>
import { ref, reactive, onMounted, watch, nextTick } from "vue";
import { useRoute } from "vue-router";
import { api, errorMessage } from "../api/storeApi";
import cookieApi from "../api/cookieApi";
import BaseButton from "../components/ui/BaseButton.vue";
import BaseInput from "../components/ui/BaseInput.vue";
import LowStockWidget from "../components/admin/LowStockWidget.vue";

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
  file = ref(null),
  uploadInput = ref(null);
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
async function detail() {
  const n = ++version;
  current.value = null;
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
    await detail();
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
    () =>
      api("put", `${base()}/stock`, {
        [stock.mode]: value,
        reason: stock.mode === "setTo" ? "STOCKTAKE" : stock.reason,
        note: stock.note,
      }),
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
    if (uploadInput.value) uploadInput.value.value = "";
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
  detail();
});
onMounted(load);
</script>
<template>
  <div class="p-5 md:p-10 max-w-6xl mx-auto">
    <h1 class="font-display text-display">Stock, packs & photos</h1>
    <p class="mt-3">
      Stock is counted in individual cookies. Pack prices are in rand.
    </p>
    <label class="block mt-7"
      >Choose a cookie<select
        v-model="selected"
        class="field mt-2"
        :disabled="busy"
      >
        <option
          v-for="v in cookies"
          :key="v.cookie.cookieId"
          :value="v.cookie.cookieId"
        >
          {{ v.cookie.description }}{{ v.cookie.archived ? " (archived)" : "" }}
        </option>
      </select></label
    >
    <p v-if="error" role="alert" class="mt-5 text-red-800">
      {{ error }} <button class="underline" @click="load">Reload</button>
    </p>
    <p v-if="notice" role="status" class="mt-5">{{ notice }}</p>
    <p v-if="busy" role="status" class="mt-5">Saving changes…</p>
    <template v-if="current"
      ><section class="py-9 border-b border-chocolate/20">
        <div class="flex justify-between flex-wrap gap-4">
          <h2 class="font-display text-3xl">
            {{ current.cookie.stockQuantity }} cookies in stock
          </h2>
          <BaseButton variant="outline" @click="openHistory"
            >Stock history</BaseButton
          >
        </div>
        <form class="grid sm:grid-cols-2 gap-4 mt-6" @submit.prevent="adjust">
          <label
            >Action<select v-model="stock.mode" class="field mt-1">
              <option value="adjustBy">Adjust stock (+ / −)</option>
              <option value="setTo">Stocktake: set exact count</option>
            </select></label
          ><label
            >Number of cookies<input
              v-model="stock.value"
              type="number"
              step="1"
              :min="stock.mode === 'setTo' ? 0 : undefined"
              class="field mt-1"
              required /></label
          ><label v-if="stock.mode === 'adjustBy'"
            >Reason<select v-model="stock.reason" class="field mt-1">
              <option value="RESTOCK">Restock (fresh bake)</option>
              <option value="ADJUSTMENT">Correction</option>
              <option value="DAMAGED">Damaged or discarded</option>
            </select></label
          ><BaseInput v-model="stock.note" label="Note (optional)" /><BaseButton
            type="submit"
            :disabled="busy"
            >Update stock</BaseButton
          >
        </form>
      </section>
      <section class="py-9 border-b border-chocolate/20">
        <h2 class="font-display text-3xl">Product details</h2>
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
      <section class="py-9 border-b border-chocolate/20">
        <h2 class="font-display text-3xl">Pack sizes</h2>
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
          <div class="sm:col-span-2 flex gap-3">
            <BaseButton type="submit" :disabled="busy">Save pack</BaseButton
            ><BaseButton v-if="editingPack" variant="outline" @click="resetPack"
              >Cancel edit</BaseButton
            >
          </div>
        </form>
      </section>
      <section class="py-9">
        <h2 class="font-display text-3xl">Gallery</h2>
        <p class="mt-3">
          The primary image stays first. Reorder the additional photos below.
        </p>
        <ol class="mt-6 space-y-4">
          <li
            v-for="(photo, i) in current.images"
            :key="photo.imageId"
            class="grid grid-cols-[96px_1fr] sm:flex sm:flex-wrap items-center gap-4"
          >
            <img
              :src="cookieApi.getImageUrl(photo.image)"
              :alt="photo.altText || current.cookie.description"
              class="w-24 h-28 object-contain bg-tan rounded-xl"
            /><span class="sm:flex-1 min-w-[120px] break-words">{{
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
          <label class="block"
            >New photo (up to 10 MB)<input
              ref="uploadInput"
              type="file"
              accept="image/jpeg,image/png,image/webp,image/gif"
              class="block mt-2 max-w-full"
              @change="file = $event.target.files[0]" /></label
          ><BaseInput v-model="altText" label="Describe the photo" /><BaseButton
            type="submit"
            :disabled="busy || current.images.length >= 10"
            >Upload photo</BaseButton
          >
        </form>
      </section></template
    >
    <dialog
      ref="historyDialog"
      class="fixed inset-y-0 left-auto right-0 m-0 max-h-none h-dvh w-full sm:max-w-xl bg-surface text-chocolate shadow-soft-lg p-6 overflow-y-auto backdrop:bg-chocolate/50"
      aria-labelledby="history-title"
      @close="showHistory = false"
    >
      <div class="flex items-center justify-between gap-4">
        <h2 id="history-title" class="font-display text-3xl">Stock history</h2>
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
