<script setup>
/**
 * ProductView — single cookie detail page.
 *
 * Layout matches the design reference:
 *   - Dark chocolate background section at top
 *   - Left: large circular cookie image in a rounded frame with splash
 *   - Right: name, rating, price, quantity selector, description, Add to Cart + Buy Now
 *   - Below: Customer Reviews section
 *   - Below: "You may also like" related cookies grid
 */
import { ref, computed, watch, onMounted } from "vue";
import { useRoute, useRouter } from "vue-router";

import {
  ShoppingBagIcon,
  ArrowRightIcon,
  ArrowLeftIcon,
  CheckCircleIcon,
} from "@heroicons/vue/24/outline";
import RatingStars from "../components/ui/RatingStars.vue";
import QuantitySelector from "../components/ui/QuantitySelector.vue";
import BaseButton from "../components/ui/BaseButton.vue";
import BaseBadge from "../components/ui/BaseBadge.vue";
import Breadcrumb from "../components/ui/Breadcrumb.vue";
import CookieCard from "../components/cards/CookieCard.vue";
import LoadingSpinner from "../components/ui/LoadingSpinner.vue";
import { useFormat } from "../composables/useFormat";
import { useScrollReveal } from "../composables/useScrollReveal";

const { vReveal } = useScrollReveal();
const route = useRoute();
const router = useRouter();
const { currency } = useFormat();

const current = ref(null);
const related = ref([]);
const loading = ref(false);
const error = ref(null);
const quantity = ref(1);
const added = ref(false);

const load = async (id) => {
  loading.value = true;
  error.value = null;

  try {
    const response = await fetch(`http://localhost:8080/api/products/${id}`);

    if (!response.ok) {
      throw new Error("Product not found");
    }

    current.value = await response.json();

    // Optional: fetch related products
    const relatedResponse = await fetch(`http://localhost:8080/api/products`);
    related.value = await relatedResponse.json();

    quantity.value = 1;
    added.value = false;

    window.scrollTo({
      top: 0,
      behavior: "smooth",
    });
  } catch (err) {
    error.value = err.message;
  } finally {
    loading.value = false;
  }
};

onMounted(() => load(route.params.id));
watch(
  () => route.params.id,
  (id) => {
    if (id) load(id);
  },
);

const breadcrumbItems = computed(() => [
  { label: "Menu", to: { name: "Products" } },
  { label: current.value?.category || "Cookie" },
  { label: current.value?.name || "Loading..." },
]);

const onAddToCart = () => {
  console.log("Cart coming soon");
};

const onBuyNow = () => {
  router.push({ name: "checkout" });
};
</script>

<template>
  <div class="pt-20">
    <!-- Breadcrumb
    <div class="container-magic pt-6">
      <Breadcrumb :items="breadcrumbItems" />
    </div> -->

    <!-- Loading state -->
    <div v-if="loading && !current" class="flex justify-center py-32">
      <LoadingSpinner
        size="lg"
        label="Pulling a fresh cookie out of the oven..."
      />
    </div>

    <!-- Error / not found -->
    <div
      v-else-if="error || !current"
      class="container-magic text-center py-32"
    >
      <div class="text-7xl mb-4">🥺</div>
      <h2 class="text-3xl font-semibold text-chocolate mb-2">
        Cookie not found
      </h2>
      <p class="text-chocolate/60 text-card mb-6">
        The cookie you're looking for has either sold out or doesn't exist.
      </p>
      <BaseButton :to="{ name: 'Home' }" variant="primary">
        Back to Home
        <ArrowLeftIcon class="h-5 w-5" />
      </BaseButton>
    </div>

    <template v-else>
      <!-- Hero / detail section -->
      <section
        class="section bg-chocolate text-cream relative overflow-hidden texture-dark"
      >
        <div
          class="pointer-events-none absolute -top-32 -right-32 w-96 h-96 rounded-full bg-primary/10 blur-3xl"
        />
        <div
          class="pointer-events-none absolute -bottom-32 -left-32 w-96 h-96 rounded-full bg-primary/10 blur-3xl"
        />

        <div class="container-magic relative">
          <div class="grid lg:grid-cols-2 gap-12 lg:gap-16 items-center">
            <!-- Left: image -->
            <div v-reveal class="relative">
              <!-- Splash glow -->
              <div class="absolute inset-0 flex items-center justify-center">
                <div
                  class="w-[28rem] h-[28rem] rounded-full bg-primary/25 blur-3xl"
                />
              </div>

              <!-- Dashed orbit ring -->
              <div
                class="absolute inset-2 rounded-full border-2 border-dashed border-cream/15 animate-spin-slow"
              />

              <!-- Cookie image -->
              <div class="relative aspect-square max-w-md mx-auto">
                <div
                  class="absolute inset-0 rounded-full bg-chocolate-300 shadow-cookie"
                />
                <img
                  :src="current.image"
                  :alt="current.name"
                  class="relative w-full h-full object-cover rounded-full p-5"
                />
                <!-- Floating badge -->
                <div
                  v-if="current.badges?.length"
                  class="absolute -top-2 right-4 bg-primary text-chocolate rounded-2xl px-4 py-2 shadow-soft-lg text-sm font-semibold animate-float"
                >
                  {{ current.badges[0] }}
                </div>
              </div>
            </div>

            <!-- Right: details -->
            <div v-reveal data-reveal-delay="100">
              <p
                class="text-cream/60 text-card uppercase tracking-[0.2em] mb-2"
              >
                CrumbleMagic Cookies · {{ current.category }}
              </p>
              <h1
                class="text-4xl lg:text-5xl text-cream font-medium mb-4 leading-tight"
              >
                {{ current.name }}
              </h1>

              <div class="flex items-center gap-4 mb-6">
                <RatingStars
                  :rating="current.rating"
                  :show-value="true"
                  size="md"
                />
                <span class="text-cream/40">·</span>
                <span class="text-cream/70 text-card"
                  >{{ current.reviews?.length || 0 }} reviews</span
                >
              </div>

              <div class="flex items-baseline gap-3 mb-7">
                <span class="text-5xl font-bold text-primary-200">{{
                  currency(current.price)
                }}</span>
                <span class="text-cream/50 text-card">per cookie</span>
              </div>

              <p class="text-cream/80 text-body leading-relaxed mb-8">
                {{ current.description }}
              </p>

              <!-- Ingredients -->
              <div class="mb-8">
                <p
                  class="text-cream/60 text-card uppercase tracking-wider text-sm mb-3"
                >
                  Made With
                </p>
                <div class="flex flex-wrap gap-2">
                  <span
                    v-for="ing in current.ingredients"
                    :key="ing"
                    class="inline-flex items-center px-3.5 py-1.5 rounded-full bg-cream/10 text-cream/80 text-sm border border-cream/10"
                  >
                    {{ ing }}
                  </span>
                </div>
              </div>

              <!-- Quantity + CTAs -->
              <div class="space-y-5">
                <div class="flex items-center gap-5">
                  <span class="text-cream/70 text-card font-medium"
                    >Quantity</span
                  >
                  <QuantitySelector
                    v-model="quantity"
                    :min="1"
                    :max="10"
                    variant="dark"
                    size="md"
                  />
                  <span class="text-cream/50 text-sm">
                    = {{ currency(current.price * quantity) }}
                  </span>
                </div>

                <div class="flex flex-col sm:flex-row gap-3">
                  <BaseButton
                    variant="light"
                    size="lg"
                    block
                    @click="onAddToCart"
                  >
                    <CheckCircleIcon
                      v-if="added"
                      class="h-5 w-5 text-green-600"
                    />
                    <ShoppingBagIcon v-else class="h-5 w-5" />
                    {{ added ? "Added to Cart" : "Add to Cart" }}
                  </BaseButton>
                  <BaseButton
                    variant="primary"
                    size="lg"
                    block
                    @click="onBuyNow"
                  >
                    Buy Now
                    <ArrowRightIcon class="h-5 w-5" />
                  </BaseButton>
                </div>

                <!-- Stock indicator -->
                <div class="flex items-center gap-2 text-sm">
                  <span
                    class="flex h-2 w-2 rounded-full bg-green-400 animate-pulse"
                  />
                  <span class="text-cream/70">
                    {{ current.stock }} in stock · Baked fresh this morning
                  </span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      <!-- Customer reviews -->
      <section class="section bg-background">
        <div class="container-magic">
          <div
            v-reveal
            class="flex items-end justify-between mb-10 flex-wrap gap-4"
          >
            <div>
              <span class="eyebrow">What Customers Say</span>
              <h2 class="text-chocolate leading-tight">
                Customer <span class="text-primary">Reviews</span>
              </h2>
            </div>
            <BaseButton variant="outline"> Share Your Experience </BaseButton>
          </div>

          <div v-if="current.reviews?.length" class="grid md:grid-cols-2 gap-6">
            <div
              v-for="(review, i) in current.reviews"
              :key="review.id"
              v-reveal
              :data-reveal-delay="i * 100"
              class="bg-chocolate text-cream rounded-4xl p-7 relative overflow-hidden"
            >
              <div
                class="pointer-events-none absolute -bottom-10 -right-10 w-40 h-40 rounded-full bg-primary/10 blur-3xl"
              />
              <div class="relative">
                <RatingStars
                  :rating="review.rating"
                  :show-value="false"
                  size="md"
                  class="mb-3"
                />
                <p class="text-cream/80 text-card leading-relaxed mb-5 italic">
                  "{{ review.text }}"
                </p>
                <div
                  class="flex items-center justify-between pt-4 border-t border-cream/10"
                >
                  <div class="flex items-center gap-3">
                    <div
                      class="flex items-center justify-center h-10 w-10 rounded-full bg-primary/20 text-primary font-semibold"
                    >
                      {{ review.author.charAt(0) }}
                    </div>
                    <div>
                      <div class="font-semibold text-cream">
                        {{ review.author }}
                      </div>
                      <div class="text-cream/50 text-sm">Verified Customer</div>
                    </div>
                  </div>
                  <div class="text-cream/40 text-sm">{{ review.date }}</div>
                </div>
              </div>
            </div>
          </div>
          <div v-else class="text-center py-16 text-chocolate/60">
            No reviews yet — be the first to share your experience!
          </div>
        </div>
      </section>

      <!-- You may also like -->
      <section class="section bg-cream-50/60">
        <div class="container-magic">
          <div v-reveal class="mb-10">
            <span class="eyebrow">Keep Crumbing</span>
            <h2 class="text-chocolate leading-tight">
              You May Also <span class="text-primary">Like</span>
            </h2>
          </div>

          <div
            v-if="related.length"
            class="grid sm:grid-cols-2 lg:grid-cols-4 gap-6"
          >
            <div
              v-for="(cookie, i) in related"
              :key="cookie.id"
              v-reveal
              :data-reveal-delay="i * 80"
            >
              <CookieCard :cookie="cookie" />
            </div>
          </div>
          <div v-else class="flex justify-center py-12">
            <LoadingSpinner label="Loading more cookies..." />
          </div>
        </div>
      </section>
    </template>
  </div>
</template>
