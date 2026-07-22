<script setup>
/**
 * MenuView — full cookie catalogue with search, category filter, sort, and grid.
 * Pulls from productStore (filtering/sorting handled reactively in the store).
 */
import { ref, computed, onMounted } from "vue";
import { AdjustmentsHorizontalIcon } from "@heroicons/vue/24/outline";
import SearchBar from "../components/ui/SearchBar.vue";
import CategoryFilter from "../components/ui/CategoryFilter.vue";
import CookieCard from "../components/cards/CookieCard.vue";
import BaseButton from "../components/ui/BaseButton.vue";
import LoadingSpinner from "../components/ui/LoadingSpinner.vue";

import { categories, sortOptions } from "../data/cookies";
import { useScrollReveal } from "../composables/useScrollReveal";

const { vReveal } = useScrollReveal();
const items = ref([]);
const loading = ref(false);

const search = ref("");
const category = ref("all");
const sortBy = ref("popular");

// Per-category counts
const filtered = computed(() => {
  let result = [...items.value];

  if (search.value) {
    const term = search.value.toLowerCase();

    result = result.filter(
      (cookie) =>
        cookie.name.toLowerCase().includes(term) ||
        cookie.description.toLowerCase().includes(term),
    );
  }

  if (category.value !== "all") {
    result = result.filter((cookie) => cookie.category === category.value);
  }

  switch (sortBy.value) {
    case "price-low":
      result.sort((a, b) => a.price - b.price);
      break;

    case "price-high":
      result.sort((a, b) => b.price - a.price);
      break;

    case "name":
      result.sort((a, b) => a.name.localeCompare(b.name));
      break;

    default:
      break;
  }

  return result;
});

onMounted(async () => {
  loading.value = true;

  try {
    const response = await fetch("http://localhost:8080/api/products");
    items.value = await response.json();
  } catch (err) {
    console.error(err);
  } finally {
    loading.value = false;
  }
});

const resetFilters = () => {
  search.value = "";
  category.value = "all";
  sortBy.value = "popular";
};
</script>

<template>
  <div class="pt-28">
    <!-- Header band -->
    <section class="bg-cream-50/60 py-14 border-b border-cream-300">
      <div class="container-magic text-center max-w-3xl mx-auto">
        <span class="eyebrow">Our Cookie Menu</span>
        <h1 class="text-chocolate font-medium leading-tight mb-4">
          The Full <span class="text-primary">Crumble</span> Collection
        </h1>
        <p class="text-chocolate/70 text-card">
          From molten-centred stuffed cookies to wholesome vegan classics —
          every cookie here is handmade, baked fresh daily, and ready to ship.
          Filter, sort, and find your new favourite.
        </p>
      </div>
    </section>

    <!-- Sticky filter bar -->
    <section
      class="sticky top-[68px] z-30 bg-background/90 backdrop-blur-md border-b border-cream-300 py-5"
    >
      <div class="container-magic space-y-4">
        <div class="grid lg:grid-cols-[1fr_auto] gap-4 items-center">
          <SearchBar
            :model-value="search"
            placeholder="Search cookies by name, ingredient, or flavour..."
            @update:model-value="search = $event"
          />
          <div class="flex items-center gap-3">
            <AdjustmentsHorizontalIcon class="h-5 w-5 text-chocolate/60" />
            <select
              :value="sortBy"
              class="h-14 bg-surface border border-cream-400/60 rounded-full px-5 text-chocolate focus:outline-none focus:ring-2 focus:ring-primary/40 transition-all cursor-pointer"
              @change="sortBy = $event.target.value"
            >
              <option v-for="opt in sortOptions" :key="opt.id" :value="opt.id">
                {{ opt.label }}
              </option>
            </select>
          </div>
        </div>
        <CategoryFilter
          :model-value="category"
          :categories="categories"
          :counts="counts"
          @update:model-value="category = $event"
        />
      </div>
    </section>

    <!-- Grid -->
    <section class="section">
      <div class="container-magic">
        <!-- Result count -->
        <div class="flex items-center justify-between mb-8">
          <p class="text-chocolate/70 text-card">
            <span class="font-semibold text-chocolate">{{
              filtered.length
            }}</span>
            cookie{{ filtered.length !== 1 ? "s" : "" }} found
          </p>
          <button
            v-if="search || category !== 'all' || sortBy !== 'popular'"
            type="button"
            class="text-sm text-primary-700 hover:text-primary-600 underline-offset-4 hover:underline"
            @click="resetFilters()"
          >
            Clear filters
          </button>
        </div>

        <!-- Loading -->
        <div v-if="loading && !items.length" class="flex justify-center py-24">
          <LoadingSpinner size="lg" label="Warming the oven..." />
        </div>

        <!-- Empty -->
        <div v-else-if="!filtered.length" class="text-center py-24">
          <div class="text-7xl mb-4">🔍</div>
          <h3 class="text-2xl font-semibold text-chocolate mb-2">
            No cookies found
          </h3>
          <p class="text-chocolate/60 text-card mb-6">
            Try a different search term or category filter.
          </p>
          <BaseButton variant="primary" @click="resetFilters()"
            >Reset filters</BaseButton
          >
        </div>

        <!-- Grid -->
        <div v-else class="grid sm:grid-cols-2 lg:grid-cols-3 gap-6">
          <div
            v-for="(cookie, i) in filtered"
            :key="cookie.id"
            v-reveal
            :data-reveal-delay="Math.min(i * 50, 300)"
          >
            <CookieCard :cookie="cookie" />
          </div>
        </div>
      </div>
    </section>
  </div>
</template>
