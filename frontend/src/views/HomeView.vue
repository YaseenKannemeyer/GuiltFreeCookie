<template>
  <div>
    <!-- Hero Section -->
    <section class="hero">
      <h1>Delicious Cookies,<br />Guilt Free</h1>
      <p>
        Indulge in our handcrafted cookies made with wholesome ingredients.
        Perfect for every dietary need without compromising on taste.
      </p>
      <div
        style="
          display: flex;
          gap: 15px;
          justify-content: center;
          position: relative;
          z-index: 1;
        "
      >
        <router-link to="/products" class="btn btn-primary">
          Browse Cookies
        </router-link>
      </div>
    </section>

    <!-- Features Section -->
    <section class="section">
      <h2 class="section-title">Why Choose Us?</h2>
      <p class="section-subtitle">Cookies you can feel good about eating</p>
      <div class="features">
        <div class="feature-card">
          <div class="feature-icon">🌿</div>
          <h3>Healthy Ingredients</h3>
          <p>
            Made with natural, wholesome ingredients that nourish your body.
          </p>
        </div>
        <div class="feature-card">
          <div class="feature-icon">🏋️</div>
          <h3>High Protein Options</h3>
          <p>
            Fuel your fitness goals with our protein-packed cookie varieties.
          </p>
        </div>
        <div class="feature-card">
          <div class="feature-icon">🚫</div>
          <h3>Allergy Friendly</h3>
          <p>Gluten-free, dairy-free, and nut-free options available.</p>
        </div>
        <div class="feature-card">
          <div class="feature-icon">🩸</div>
          <h3>Diabetic Safe</h3>
          <p>Low sugar alternatives perfect for those watching their intake.</p>
        </div>
      </div>
    </section>

    <!-- Featured Products -->
    <section class="section" style="background: var(--white)">
      <h2 class="section-title">Featured Cookies</h2>
      <p class="section-subtitle">Our most popular selections</p>

      <div v-if="loading" class="loading">
        <div class="spinner"></div>
        <p>Loading featured cookies...</p>
      </div>

      <div v-else-if="featuredCookies.length > 0" class="product-grid">
        <ProductCard
          v-for="cookie in featuredCookies"
          :key="cookie.cookieId"
          :cookie="cookie"
        />
      </div>

      <div v-else class="empty-state">
        <div class="icon">🍪</div>
        <h3>No cookies yet</h3>
        <p>Check back soon for our delicious offerings!</p>
      </div>

      <div style="text-align: center; margin-top: 40px">
        <router-link to="/products" class="btn btn-secondary">
          View All Cookies
        </router-link>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref, onMounted } from "vue";
import ProductCard from "../components/ProductCard.vue";
import cookieApi from "../api/cookieApi";

const featuredCookies = ref([]);
const loading = ref(false);

onMounted(async () => {
  loading.value = true;
  try {
    const allCookies = await cookieApi.getAll();
    // Show first 4 cookies as featured
    featuredCookies.value = allCookies.slice(0, 4);
  } catch (error) {
    console.error("Error fetching featured cookies:", error);
  } finally {
    loading.value = false;
  }
});
</script>

<style scoped></style>
