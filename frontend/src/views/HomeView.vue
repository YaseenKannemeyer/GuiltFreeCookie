<template>
  <div>
    <!-- Hero Section -->
    <section
      class="relative w-full overflow-hidden min-h-[750px] lg:min-h-screen flex items-center"
    >
      <!-- Mobile Background -->
      <div class="absolute inset-0 bg-[#d0ab7b] lg:hidden"></div>

      <!-- Desktop Background -->
      <div
        class="hidden lg:block absolute inset-y-0 left-0 w-[68%] bg-[#d0ab7b]"
      ></div>
      <div
        class="hidden lg:block absolute inset-y-0 right-0 w-[32%] bg-[#2d0d08]"
      ></div>

      <!-- Content -->
      <div class="relative z-10 mx-auto w-full max-w-7xl px-6 sm:px-8 lg:px-0">
        <div class="grid grid-cols-1 lg:grid-cols-2 items-center gap-12">
          <!-- ================= LEFT ================= -->
          <div class="max-w-xl text-center lg:text-left py-20 lg:py-0">
            <!-- Main Heading -->
            <h1
              class="font-bold leading-tight text-white text-4xl sm:text-5xl md:text-6xl lg:text-7xl"
            >
              Craving
              <span class="text-chocolate">Cookies?</span>
            </h1>

            <!-- Sub Heading -->
            <h2
              class="mt-3 text-3xl sm:text-4xl md:text-5xl lg:text-6xl font-light text-white"
            >
              We've Got the Crumble!
            </h2>

            <!-- Mobile Cookie -->
            <div class="flex justify-center my-10 md:hidden">
              <img
                src="/cookieHome.png"
                alt="Cookie"
                class="w-72 sm:w-80 drop-shadow-[0_30px_45px_rgba(0,0,0,.35)] select-none pointer-events-none"
              />
            </div>

            <!-- Description -->
            <p
              class="mt-6 lg:mt-8 text-base sm:text-lg leading-7 lg:leading-8 text-white/90 max-w-lg mx-auto lg:mx-0"
            >
              From warm ovens to happy moments! Our cookies are made to crumble
              just right. Sweet, soft, and baked with love in every batch.
            </p>

            <!-- Buttons -->
            <div
              class="mt-12 flex flex-col md:flex-row gap-4 justify-center lg:justify-start"
            >
              <BaseButton :to="{ name: 'menu' }" variant="primary" size="lg"
                >Buy Now</BaseButton
              >

              <BaseButton :to="{ name: 'menu' }" variant="outline" size="lg"
                >See Menu</BaseButton
              >
            </div>
          </div>

          <!-- ================= RIGHT ================= -->
          <div class="relative hidden md:block h-[650px]">
            <!-- Vertical Text -->
            <div
              class="hidden lg:block absolute right-0 top-1/2 -translate-y-1/2 rotate-180"
              style="writing-mode: vertical-rl"
            >
              <h2
                class="ml-[-150px] text-[250px] font-bold text-[#D7B07D] opacity-90"
              >
                Cookies
              </h2>
            </div>

            <!-- Desktop Cookie -->
            <img
              src="/cookieHome.png"
              alt="Cookie"
              class="absolute left-1/2 top-1/2 -translate-x-1/2 -translate-y-1/2 w-[430px] lg:w-[680px] lg:left-[-80px] lg:translate-x-0 drop-shadow-[0_35px_55px_rgba(0,0,0,.35)] select-none pointer-events-none"
            />
          </div>
        </div>
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
