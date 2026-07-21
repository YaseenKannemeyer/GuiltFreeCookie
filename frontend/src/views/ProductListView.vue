<template>
  <div>
    <!-- Page Header -->
    <section class="hero" style="padding: 40px 20px">
      <h1 style="font-size: 2.5rem">Our Cookies</h1>
      <p>Find the perfect cookie for your dietary needs</p>
    </section>

    <section class="section">
      <!-- Filters -->
      <div class="filters-container">
        <div class="filters-wrapper">
          <SearchBar v-model="searchQuery" />
          <div
            style="
              display: flex;
              flex-wrap: wrap;
              gap: 20px;
              align-items: flex-start;
            "
          >
            <CategoryFilter v-model:selectedCategory="selectedCategory" />
            <PriceFilter
              v-model:minPrice="minPrice"
              v-model:maxPrice="maxPrice"
            />
            <button class="clear-btn" @click="clearFilters">
              Clear Filters
            </button>
          </div>
        </div>
      </div>

      <!-- Results Info -->
      <div class="results-info">
        <span class="results-count">
          Showing {{ filteredCookies.length }} cookie{{
            filteredCookies.length !== 1 ? "s" : ""
          }}
        </span>
      </div>

      <!-- Loading State -->
      <div v-if="loading" class="loading">
        <div class="spinner"></div>
        <p>Loading cookies...</p>
      </div>

      <!-- Product Grid -->
      <div v-else-if="filteredCookies.length > 0" class="product-grid">
        <ProductCard
          v-for="cookie in filteredCookies"
          :key="cookie.cookieId"
          :cookie="cookie"
        />
      </div>

      <!-- Empty State -->
      <div v-else class="empty-state">
        <div class="icon">🔍</div>
        <h3>No cookies found</h3>
        <p>Try adjusting your search or filters</p>
        <button class="btn btn-secondary" @click="clearFilters">
          Clear All Filters
        </button>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from "vue";
import ProductCard from "../components/ProductCard.vue";
import SearchBar from "../components/SearchBar.vue";
import CategoryFilter from "../components/CategoryFilter.vue";
import PriceFilter from "../components/PriceFilter.vue";
import cookieApi from "../api/cookieApi";

const allCookies = ref([]);
const loading = ref(false);
const searchQuery = ref("");
const selectedCategory = ref(null);
const minPrice = ref(0);
const maxPrice = ref(9999);

// Filter cookies based on all criteria
const filteredCookies = computed(() => {
  let result = allCookies.value;

  // Filter by search query
  if (searchQuery.value) {
    const query = searchQuery.value.toLowerCase();
    result = result.filter(
      (cookie) =>
        (cookie.description &&
          cookie.description.toLowerCase().includes(query)) ||
        (cookie.ingredients &&
          cookie.ingredients.toLowerCase().includes(query)) ||
        (cookie.allergies && cookie.allergies.toLowerCase().includes(query)),
    );
  }

  // Filter by category
  if (selectedCategory.value) {
    result = result.filter(
      (cookie) => cookie.category === selectedCategory.value,
    );
  }

  // Filter by price range
  result = result.filter(
    (cookie) =>
      cookie.price >= minPrice.value && cookie.price <= maxPrice.value,
  );

  return result;
});

// Fetch all cookies on mount
onMounted(async () => {
  loading.value = true;
  try {
    allCookies.value = await cookieApi.getAll();
  } catch (error) {
    console.error("Error fetching cookies:", error);
  } finally {
    loading.value = false;
  }
});

// Clear all filters
const clearFilters = () => {
  searchQuery.value = "";
  selectedCategory.value = null;
  minPrice.value = 0;
  maxPrice.value = 9999;
};
</script>

<style scoped></style>
