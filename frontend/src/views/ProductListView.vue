<script setup>
import { ref, reactive, computed, watch, onMounted, onUnmounted } from "vue";
import { useRoute, useRouter } from "vue-router";
import { catalogApi, errorMessage } from "../api/storeApi";
import CookieCard from "../components/cards/CookieCard.vue";
import BaseButton from "../components/ui/BaseButton.vue";
import { ChevronDownIcon } from "@heroicons/vue/20/solid";
const route = useRoute(),
  router = useRouter();
const filters = reactive({
  q: "",
  category: "",
  minPrice: "",
  maxPrice: "",
  inStock: false,
  sort: "featured",
});
const categories = ref([]),
  result = ref({ items: [], page: 0, totalPages: 0, totalItems: 0 }),
  busy = ref(true),
  error = ref("");
const filtersExpanded = ref(false);
const activeExtraFilters = computed(() =>
  [filters.minPrice !== "", filters.maxPrice !== "", filters.inStock, filters.sort !== "featured"].filter(Boolean).length,
);
let timer,
  controller,
  syncing = false;
function fromUrl() {
  syncing = true;
  for (const k of ["q", "category", "minPrice", "maxPrice"])
    filters[k] = typeof route.query[k] === "string" ? route.query[k] : "";
  filters.inStock = route.query.inStock === "true";
  filters.sort = route.query.sort || "featured";
  queueMicrotask(() => (syncing = false));
}
fromUrl();
const page = computed(() => Math.max(0, Number(route.query.page) || 0));
async function load() {
  controller?.abort();
  controller = new AbortController();
  const signal = controller.signal;
  busy.value = true;
  error.value = "";
  try {
    result.value = await catalogApi.search(
      { ...route.query, page: page.value, size: 12 },
      signal,
    );
  } catch (e) {
    if (!signal.aborted) error.value = errorMessage(e);
  } finally {
    if (!signal.aborted) busy.value = false;
  }
}
function update(page = 0) {
  const query = {};
  for (const [k, v] of Object.entries(filters))
    if (v !== "" && v !== false) query[k] = String(v);
  if (page) query.page = page;
  router.replace({ query });
}
watch(filters, () => {
  if (syncing) return;
  clearTimeout(timer);
  timer = setTimeout(() => update(), 300);
});
watch(
  () => route.query,
  () => {
    fromUrl();
    load();
  },
);
onMounted(async () => {
  load();
  try {
    categories.value = await catalogApi.categories();
  } catch (e) {
    error.value = errorMessage(e);
  }
});
onUnmounted(() => {
  clearTimeout(timer);
  controller?.abort();
});
function clear() {
  Object.assign(filters, {
    q: "",
    category: "",
    minPrice: "",
    maxPrice: "",
    inStock: false,
    sort: "featured",
  });
}
</script>
<template>
  <div class="pt-[var(--nav-h)]">
    <div class="container-magic py-12 md:py-20">
      <h1 class="font-display text-display">Find your favourite</h1>
      <p class="mt-3 max-w-2xl text-chocolate/80">
        Small-batch cookies for every kind of craving. Explore the flavours and
        find your next treat.
      </p>
      <form class="mt-8 space-y-5" @submit.prevent="update()">
        <label class="block font-semibold"
          >Search cookies<input
            v-model="filters.q"
            type="search"
            maxlength="100"
            placeholder="Chocolate, almond, a favourite flavour…"
            class="field mt-2"
        /></label>
        <div class="flex flex-wrap gap-2" aria-label="Cookie categories">
          <button
            type="button"
            :aria-pressed="!filters.category"
            class="filter-chip"
            :class="!filters.category ? '!bg-chocolate !text-cream-50' : ''"
            @click="filters.category = ''"
          >
            All cookies</button
          ><button
            v-for="c in categories"
            :key="c.category"
            type="button"
            :aria-pressed="filters.category === c.category"
            class="filter-chip"
            :class="
              filters.category === c.category
                ? '!bg-chocolate !text-cream-50'
                : ''
            "
            @click="filters.category = c.category"
          >
            {{ c.label }} ({{ c.count }})
          </button>
        </div>
        <button
          type="button"
          class="sm:hidden flex w-full min-h-11 items-center justify-between gap-3 font-bold"
          :aria-expanded="filtersExpanded"
          aria-controls="extra-filters"
          @click="filtersExpanded = !filtersExpanded"
        >
          <span>Filter & sort<span v-if="activeExtraFilters"> ({{ activeExtraFilters }})</span></span>
          <ChevronDownIcon class="h-5 w-5 transition-transform" :class="{ 'rotate-180': filtersExpanded }" />
        </button>
        <div id="extra-filters" class="grid-cols-2 lg:grid-cols-4 gap-4 sm:grid" :class="filtersExpanded ? 'grid' : 'hidden'">
          <label
            >Min price (R)<input
              v-model="filters.minPrice"
              class="field mt-1"
              type="number"
              min="0"
              step="0.01" /></label
          ><label
            >Max price (R)<input
              v-model="filters.maxPrice"
              class="field mt-1"
              type="number"
              min="0"
              step="0.01" /></label
          ><label class="col-span-2 sm:col-span-1"
            >Sort by<select v-model="filters.sort" class="field mt-1">
              <option value="featured">Featured</option>
              <option value="price_asc">Price: low to high</option>
              <option value="price_desc">Price: high to low</option>
              <option value="newest">Newest</option>
              <option value="name">Name: A–Z</option>
            </select></label
          ><label class="flex items-center gap-3 min-h-11"
            ><input v-model="filters.inStock" type="checkbox" />In stock
            only</label
          >
        </div>
      </form>
      <div
        class="flex items-center justify-between gap-4 mt-7 border-t border-chocolate/15 pt-5"
      >
        <p role="status">
          {{ busy ? "Finding your cookies…" : `${result.totalItems} cookies` }}
        </p>
        <button class="underline min-h-11" @click="clear">Clear filters</button>
      </div>
      <div v-if="error" role="alert" class="py-10">
        <p>{{ error }}</p>
        <BaseButton class="mt-4" @click="load">Try again</BaseButton>
      </div>
      <div v-else-if="!busy && !result.items.length" class="py-16 text-center">
        <h2 class="font-display text-3xl">No cookies found</h2>
        <p class="mt-3">Try another flavour or clear your filters.</p>
      </div>
      <div
        v-else
        class="grid grid-cols-2 lg:grid-cols-4 gap-x-4 gap-y-9 mt-6"
        :aria-busy="busy"
      >
        <CookieCard
          v-for="(c, i) in result.items"
          :key="c.cookieId"
          :cookie="c"
          :eager="i < 4"
        />
      </div>
      <nav
        v-if="result.totalPages > 1"
        class="flex flex-wrap justify-center items-center gap-3 mt-12"
        aria-label="Product pages"
      >
        <BaseButton
          variant="outline"
          :disabled="page === 0 || busy"
          @click="update(page - 1)"
          >Previous</BaseButton
        ><span>{{ page + 1 }} / {{ result.totalPages }}</span
        ><BaseButton
          variant="outline"
          :disabled="page >= result.totalPages - 1 || busy"
          @click="update(page + 1)"
          >Next</BaseButton
        >
      </nav>
    </div>
  </div>
</template>
<style scoped>
.filter-chip {
  min-height: 44px;
  padding: 8px 16px;
  border: 1px solid #c59f71;
  border-radius: 999px;
}
</style>
