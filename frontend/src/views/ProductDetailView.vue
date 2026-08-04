<template>
  <div class="min-h-screen bg-[#45130d] relative">
    <!-- Faded Cookie Background Pattern -->
    <div
      class="absolute inset-0 pointer-events-none z-0"
      style="
        background-image: url(&quot;/images/cookie-pattern.png&quot;);
        background-size: cover;
        background-position: center;
        opacity: 0.08;
      "
    />

    <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-14 relative z-10">
      <router-link
        to="/products"
        class="inline-flex items-center gap-1.5 text-sm font-medium text-cream-100/60 hover:text-cream-100 transition-colors mb-8"
      >
        <ArrowLeftIcon class="h-4 w-4" />
        Back to Cookies
      </router-link>

      <!-- Loading State -->
      <div
        v-if="loading"
        class="flex flex-col items-center justify-center py-32"
      >
        <LoadingSpinner />
        <p class="text-cream-100/60 mt-4">Loading cookie details...</p>
      </div>

      <!-- Not Found State -->
      <div
        v-else-if="!cookie"
        class="flex flex-col items-center justify-center py-32 text-center"
      >
        <div class="text-5xl mb-4">😕</div>
        <h3 class="text-xl font-semibold text-cream-100 mb-2">
          Cookie Not Found
        </h3>
        <p class="text-cream-100/60 mb-6">
          The cookie you're looking for doesn't exist.
        </p>
        <BaseButton @click="$router.push('/products')"
          >Browse All Cookies</BaseButton
        >
      </div>

      <!-- Product Detail -->
      <template v-else>
        <div
          class="grid grid-cols-1 lg:grid-cols-[1.1fr_0.9fr] gap-20 items-start"
          v-reveal
        >
          <!-- Image side -->
          <div class="lg:sticky lg:top-24">
            <div
              class="bg-[#5b2b22] rounded-[30px] p-10 flex items-center justify-center shadow-xl"
            >
              <img
                :src="displayImage"
                :alt="cookie.description"
                class="w-full max-w-[420px] object-contain"
              />
            </div>
          </div>

          <!-- Content side -->
          <div class="space-y-6">
            <div>
              <p class="text-sm font-medium text-cream-100/50 mb-2">
                CrumbleMagic Cookies
              </p>
              <h1 class="text-5xl font-bold leading-tight text-cream-50 mb-3">
                {{ cookie.description }}
              </h1>

              <div class="flex items-center gap-1.5 mb-4">
                <StarIcon
                  v-for="n in 5"
                  :key="n"
                  class="h-5 w-5"
                  :class="
                    n <= displayRating ? 'text-primary' : 'text-cream-100/20'
                  "
                />
                <span class="text-cream-100/60 text-sm ml-1"
                  >({{ displayRating.toFixed(1) }})</span
                >
              </div>

              <div class="text-5xl font-bold text-cream-50">
                R{{ cookie.price?.toFixed(2) }}
              </div>
            </div>
            <!-- Product Information -->
            <div class="bg-[#5b2b22] rounded-3xl p-6 space-y-5">
              <div>
                <p
                  class="text-xs uppercase tracking-widest text-cream-100/50 mb-1"
                >
                  Cookie ID
                </p>
                <p class="text-cream-50 font-medium">
                  {{ cookie.cookieId }}
                </p>
              </div>

              <div>
                <p
                  class="text-xs uppercase tracking-widest text-cream-100/50 mb-1"
                >
                  Category
                </p>
                <span
                  class="inline-flex rounded-full bg-primary/20 text-primary px-3 py-1 text-sm font-semibold"
                >
                  {{ cookie.category }}
                </span>
              </div>

              <div>
                <p
                  class="text-xs uppercase tracking-widest text-cream-100/50 mb-2"
                >
                  Ingredients
                </p>
                <p class="text-cream-100/80 leading-7">
                  {{ cookie.ingredients }}
                </p>
              </div>

              <div>
                <p
                  class="text-xs uppercase tracking-widest text-cream-100/50 mb-2"
                >
                  Allergy Information
                </p>

                <div
                  class="inline-flex rounded-full border border-red-400/40 bg-red-500/10 px-3 py-1 text-red-200 text-sm"
                >
                  {{ cookie.allergies }}
                </div>
              </div>

              <div>
                <p
                  class="text-xs uppercase tracking-widest text-cream-100/50 mb-2"
                >
                  Description
                </p>

                <p class="text-cream-100/80 leading-7">
                  {{ cookie.description }}
                </p>
              </div>
            </div>
            <!-- Quantity Selector -->
            <div>
              <p
                class="text-sm font-semibold uppercase tracking-wide text-cream-100/50 mb-2"
              >
                Quantity
              </p>
              <div
                class="inline-flex items-center border border-primary rounded-full overflow-hidden"
              >
                <button
                  type="button"
                  class="px-5 py-3 text-cream-50 hover:bg-primary/10 transition-colors disabled:opacity-40 disabled:cursor-not-allowed"
                  :disabled="quantity <= 1"
                  @click="quantity = Math.max(1, quantity - 1)"
                >
                  -
                </button>

                <span class="px-8 text-cream-50 font-semibold">
                  {{ quantity }}
                </span>

                <button
                  type="button"
                  class="px-5 py-3 text-cream-50 hover:bg-primary/10 transition-colors"
                  @click="quantity++"
                >
                  +
                </button>
              </div>
            </div>

            <!-- Description -->
            <p
              v-if="cookie.longDescription"
              class="text-cream-100/80 leading-8 max-w-xl"
            >
              {{ cookie.longDescription }}
            </p>

            <!-- Buttons -->
            <div class="flex flex-col sm:flex-row gap-4 pt-4">
              <button
                type="button"
                class="w-56 inline-flex items-center justify-center gap-2 bg-primary text-chocolate text-lg font-semibold py-4 rounded-full hover:bg-primary-400 transition-colors duration-300"
                @click="addToCart"
              >
                <ShoppingBagIcon class="h-5 w-5" />
                Add to Cart
              </button>
              <button
                type="button"
                class="w-56 inline-flex items-center justify-center gap-2 bg-cream-50 text-chocolate text-lg font-semibold py-4 rounded-full hover:bg-cream-200 transition-colors duration-300"
                @click="buyNow"
              >
                Buy Now
              </button>
            </div>
          </div>
        </div>

        <!-- You may also like -->
        <div v-if="relatedCookies.length" class="mt-20">
          <h2 class="text-3xl font-bold text-cream-50 mb-8">
            You may also like
          </h2>
          <div class="grid grid-cols-1 lg:grid-cols-2 gap-6">
            <div
              v-for="related in relatedCookies"
              :key="related.id"
              class="bg-[#6a4037] rounded-3xl flex items-center overflow-hidden h-[180px]"
            >
              <!-- Image -->
              <div class="w-40 h-40 shrink-0">
                <img
                  :src="
                    related.image ||
                    related.imageUrl ||
                    '/images/cookie-placeholder.png'
                  "
                  :alt="related.description"
                  class="w-full h-full object-cover rounded-full p-4"
                />
              </div>

              <!-- Content -->
              <div class="p-6 flex-1 flex flex-col justify-center min-w-0">
                <h3 class="text-xl text-cream-50 font-bold mb-1 truncate">
                  {{ related.description }}
                </h3>
                <p class="text-cream-100/70 text-sm mb-3 line-clamp-2">
                  {{
                    related.longDescription ||
                    "A CrumbleMagic favorite, freshly baked."
                  }}
                </p>
                <div class="flex items-center justify-between">
                  <span class="text-cream-50 font-bold text-xl"
                    >R{{ related.price?.toFixed(2) }}</span
                  >
                  <div class="flex items-center gap-2">
                    <button
                      type="button"
                      class="bg-primary text-chocolate text-sm font-semibold px-5 py-2.5 rounded-full hover:bg-primary-400 transition-colors"
                      @click="$router.push(`/products/${related.id}`)"
                    >
                      View More
                    </button>
                    <button
                      type="button"
                      class="h-10 w-10 flex items-center justify-center rounded-full bg-cream-50/10 text-cream-50 hover:bg-cream-50/20 transition-colors"
                      aria-label="Add to cart"
                    >
                      <ShoppingCartIcon class="h-4 w-4" />
                    </button>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </template>
    </div>

    <!-- Newsletter -->
    <section class="bg-[#F6F0E6] py-24 relative overflow-hidden">
      <!-- Cookie Watermark -->
      <img
        src="/cookie-background.jpeg"
        class="absolute left-0 bottom-0 opacity-10 w-72 pointer-events-none"
        alt=""
      />

      <div
        class="max-w-3xl mx-auto px-4 sm:px-6 lg:px-8 text-center relative z-10"
      >
        <h2 class="text-6xl font-bold text-chocolate mb-6">
          Join in and get 10% off!
        </h2>
        <p
          class="text-chocolate/70 text-lg mb-10 leading-relaxed max-w-2xl mx-auto"
        >
          Subscribe to our exclusive newsletter today and instantly receive a
          special 10% discount code for your next order! Be the first to know
          about restocks and special offers.
        </p>
        <form
          class="flex flex-col sm:flex-row gap-4 justify-center max-w-lg mx-auto"
          @submit.prevent="subscribe"
        >
          <div class="relative flex-1">
            <EnvelopeIcon
              class="h-6 w-6 text-chocolate/40 absolute left-5 top-1/2 -translate-y-1/2"
            />
            <input
              v-model="email"
              type="email"
              required
              placeholder="Email address"
              class="w-full pl-14 pr-5 py-4 rounded-full bg-white border border-chocolate/10 text-chocolate placeholder:text-chocolate/40 focus:outline-none focus:ring-2 focus:ring-primary text-lg"
            />
          </div>
          <button
            type="submit"
            class="bg-chocolate text-cream-50 font-semibold px-8 py-4 rounded-full hover:bg-chocolate/90 transition-colors whitespace-nowrap text-lg"
          >
            {{ subscribed ? "Subscribed!" : "Subscribe" }}
          </button>
        </form>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from "vue";
import { useRoute } from "vue-router";
import {
  ArrowLeftIcon,
  ShoppingBagIcon,
  ShoppingCartIcon,
  EnvelopeIcon,
} from "@heroicons/vue/24/outline";
import { StarIcon } from "@heroicons/vue/24/solid";
import cookieApi from "../api/cookieApi";
import BaseButton from "../components/ui/BaseButton.vue";
import LoadingSpinner from "../components/ui/LoadingSpinner.vue";
import { useScrollReveal } from "../composables/useScrollReveal";

const { vReveal } = useScrollReveal();

const route = useRoute();
const cookie = ref(null);
const loading = ref(false);
const quantity = ref(1);
const relatedCookies = ref([]);
const email = ref("");
const subscribed = ref(false);

const displayImage = computed(
  () =>
    cookie.value?.image ||
    cookie.value?.imageUrl ||
    "/images/cookie-placeholder.png",
);

const displayRating = computed(() => cookie.value?.rating ?? 5);

const addToCart = () => {
  // Hook this up to your cart store, passing `quantity.value`
  console.log(`Added ${quantity.value} x ${cookie.value?.description} to cart`);
};

const buyNow = () => {
  addToCart();
  // Navigate to checkout here
};

const subscribe = () => {
  if (!email.value) return;
  // Hook this up to your newsletter API
  subscribed.value = true;
  email.value = "";
};

const fetchCookie = async () => {
  loading.value = true;
  try {
    cookie.value = await cookieApi.getById(route.params.id);
  } catch (error) {
    console.error("Error fetching cookie:", error);
    cookie.value = null;
  } finally {
    loading.value = false;
  }
};

const fetchRelated = async () => {
  try {
    const all = await cookieApi.getAll?.();
    if (Array.isArray(all)) {
      relatedCookies.value = all
        .filter((c) => c.id !== route.params.id)
        .slice(0, 2);
    }
  } catch (error) {
    console.error("Error fetching related cookies:", error);
    relatedCookies.value = [];
  }
};

onMounted(async () => {
  await fetchCookie();
  fetchRelated();
});
</script>
