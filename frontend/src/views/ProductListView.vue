<script setup>
import { ref, computed, onMounted } from "vue";
import { AdjustmentsHorizontalIcon } from "@heroicons/vue/24/outline";

import SearchBar from "../components/ui/SearchBar.vue";
import CategoryFilter from "../components/ui/CategoryFilter.vue";
import CookieCard from "../components/cards/CookieCard.vue";
import BaseButton from "../components/ui/BaseButton.vue";
import LoadingSpinner from "../components/ui/LoadingSpinner.vue";
import cookieApi from "../api/cookieApi.js";

import { useScrollReveal } from "../composables/useScrollReveal";
import axios from "axios";

const { vReveal } = useScrollReveal();

const items = ref([]);
const loading = ref(false);
const loadError = ref(null);

const search = ref("");
const category = ref("all");
const sortBy = ref("popular");

const loadCookies = async () => {
  loading.value = true;
  loadError.value = null;

  try {
    items.value = await cookieApi.getAll();
  } catch (error) {
    loadError.value =
      "Couldn't load cookies right now. Please try again shortly.";
  } finally {
    loading.value = false;
  }
};

onMounted(loadCookies);

const filtered = computed(() => {
  let result = [...items.value];

  // Search
  if (search.value) {
    const term = search.value.toLowerCase();

    result = result.filter(
      (cookie) =>
        (cookie.description &&
          cookie.description.toLowerCase().includes(term)) ||
        (cookie.cookieId && cookie.cookieId.toLowerCase().includes(term)),
    );
  }

  // Category
  if (category.value !== "all") {
    result = result.filter((cookie) => cookie.category === category.value);
  }

  // Sorting
  switch (sortBy.value) {
    case "price-low":
      result.sort((a, b) => a.price - b.price);
      break;

    case "price-high":
      result.sort((a, b) => b.price - a.price);
      break;

    case "name":
      result.sort((a, b) => a.cookieId.localeCompare(b.cookieId));
      break;

    default:
      break;
  }

  return result;
});

const resetFilters = () => {
  search.value = "";
  category.value = "all";
  sortBy.value = "popular";
};

// Category counts
const counts = computed(() => {
  const result = { all: items.value.length };

  items.value.forEach((cookie) => {
    result[cookie.category] = (result[cookie.category] || 0) + 1;
  });

  return result;
});
</script>

<template>
  <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-10">
    <!-- Header -->
    <div class="mb-8" v-reveal>
      <h1 class="text-3xl font-bold text-chocolate">Our Cookies</h1>
      <p class="text-chocolate/60 mt-1">
        {{ counts.all }} guilt-free treat{{ counts.all === 1 ? "" : "s" }}
        waiting for you
      </p>
    </div>

    <!-- Controls -->
    <div class="flex flex-col md:flex-row gap-4 mb-8" v-reveal>
      <SearchBar
        v-model="search"
        placeholder="Search cookies..."
        class="flex-1"
      />

      <CategoryFilter
        v-model="category"
        :categories="categories"
        :counts="counts"
      />

      <div class="flex items-center gap-2">
        <AdjustmentsHorizontalIcon class="h-5 w-5 text-chocolate/50" />
        <select
          v-model="sortBy"
          class="rounded-full border border-chocolate/10 bg-surface px-4 py-2.5 text-sm text-chocolate focus:outline-none focus:ring-2 focus:ring-primary"
        >
          <option
            v-for="opt in sortOptions"
            :key="opt.value"
            :value="opt.value"
          >
            {{ opt.label }}
          </option>
        </select>
      </div>
    </div>

    <!-- Loading State -->
    <div v-if="loading" class="flex flex-col items-center justify-center py-24">
      <LoadingSpinner />
      <p class="text-chocolate/60 mt-4">Loading cookies...</p>
    </div>

    <!-- Error State -->
    <div
      v-else-if="loadError"
      class="flex flex-col items-center justify-center py-24 text-center"
    >
      <p class="text-chocolate/70 mb-4">{{ loadError }}</p>
      <BaseButton @click="onMounted">Try Again</BaseButton>
    </div>

    <!-- Empty State -->
    <div
      v-else-if="filtered.length === 0"
      class="flex flex-col items-center justify-center py-24 text-center"
    >
      <div class="text-5xl mb-4">🍪</div>
      <h3 class="text-xl font-semibold text-chocolate mb-2">
        No cookies found
      </h3>
      <p class="text-chocolate/60 mb-4">
        Try adjusting your search or filters.
      </p>
      <BaseButton @click="resetFilters">Reset Filters</BaseButton>
    </div>

    <!-- Product Grid -->
    <div
      v-else
      class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6"
    >
      <CookieCard
        v-for="cookie in filtered"
        :key="cookie.cookieId"
        :cookie="cookie"
        variant="light"
        v-reveal
      />
    </div>
  </div>
</template>
