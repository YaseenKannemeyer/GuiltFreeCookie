<template>
  <div class="product-detail">
    <router-link to="/products" class="back-link">
      ← Back to Products
    </router-link>

    <!-- Loading State -->
    <div v-if="loading" class="loading">
      <div class="spinner"></div>
      <p>Loading cookie details...</p>
    </div>

    <!-- Not Found State -->
    <div v-else-if="!cookie" class="empty-state">
      <div class="icon">😕</div>
      <h3>Cookie Not Found</h3>
      <p>The cookie you're looking for doesn't exist.</p>
      <router-link to="/products" class="btn btn-secondary">
        Browse All Cookies
      </router-link>
    </div>

    <!-- Product Detail -->
    <div v-else class="detail-card">
      <div class="detail-image">
        <span :class="`category-badge badge-${categoryClass}`">
          {{ formatCategory(cookie.category) }}
        </span>
      </div>
      <div class="detail-content">
        <span :class="`category-badge detail-category badge-${categoryClass}`">
          {{ formatCategory(cookie.category) }}
        </span>
        <h1 class="detail-title">{{ cookie.description }}</h1>
        <div class="detail-price">
          <span style="font-size: 1.2rem; font-weight: 400">R</span
          >{{ cookie.price?.toFixed(2) }}
        </div>

        <div class="detail-section">
          <h3>📝 Description</h3>
          <p>{{ cookie.description }}</p>
        </div>

        <div class="detail-section">
          <h3>🧁 Ingredients</h3>
          <div class="ingredient-list">
            <span
              v-for="(ingredient, index) in ingredientsList"
              :key="index"
              class="ingredient-item"
            >
              {{ ingredient.trim() }}
            </span>
          </div>
        </div>

        <div class="detail-section">
          <h3>⚠️ Allergy Information</h3>
          <div class="allergy-tags">
            <span
              v-for="(allergy, index) in allergiesList"
              :key="index"
              class="allergy-tag"
            >
              {{ allergy.trim() }}
            </span>
          </div>
          <p v-if="allergiesList.length === 0" style="color: #2e7d32">
            ✅ No known allergens in this product
          </p>
        </div>

        <div class="detail-section" style="margin-top: 30px">
          <router-link to="/products" class="btn btn-primary">
            Continue Shopping
          </router-link>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from "vue";
import { useRoute } from "vue-router";
import cookieApi from "../api/cookieApi";

const route = useRoute();
const cookie = ref(null);
const loading = ref(false);

const categoryClass = computed(() => {
  return cookie.value?.category?.toLowerCase().replace("_", "-") || "classic";
});

const formatCategory = (category) => {
  if (!category) return "";
  return category.replace(/_/g, " ");
};

const ingredientsList = computed(() => {
  if (!cookie.value?.ingredients) return [];
  return cookie.value.ingredients.split(",").filter((i) => i.trim());
});

const allergiesList = computed(() => {
  if (!cookie.value?.allergies) return [];
  return cookie.value.allergies.split(",").filter((a) => a.trim());
});

onMounted(async () => {
  loading.value = true;
  try {
    cookie.value = await cookieApi.getById(route.params.id);
  } catch (error) {
    console.error("Error fetching cookie:", error);
    cookie.value = null;
  } finally {
    loading.value = false;
  }
});
</script>

<style scoped>
.detail-image .category-badge {
  top: 20px;
  left: 20px;
}
</style>
