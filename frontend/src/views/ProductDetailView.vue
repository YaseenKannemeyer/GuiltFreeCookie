<script setup>
import { catalogApi, errorMessage } from "../api/storeApi";
import { useCart } from "../composables/useCart";
import cookieApi from "../api/cookieApi";
import ProductReviews from "../components/engagement/ProductReviews.vue";
import { ref, watch, computed, onMounted } from "vue";
import { useRoute, useRouter } from "vue-router";
import { ShoppingBagIcon } from "@heroicons/vue/20/solid";

import BaseButton from "../components/ui/BaseButton.vue";
import Breadcrumb from "../components/ui/Breadcrumb.vue";
import CookieInfoCard from "../components/cookieDetails/CookieInfoCard.vue";
import RelatedCookieCard from "../components/cookieDetails/RelatedCookieCard.vue";
import QuantitySelector from "../components/cookieDetails/QuantitySelector.vue";
import {
  useCatalog,
  cookieImage,
  cookieName,
  imageFallback,
} from "../composables/useCatalog";
import { useFormat } from "../composables/useFormat";
import { useToast } from "../composables/useToast";
import {
  formatCategoryLabel,
  categoryBlurb,
} from "../constants/cookieCategories";
import { breadcrumbsFor } from "../seo/breadcrumbs";
import { applyHead, findCookie, headForRoute } from "../seo/head";

const route = useRoute();
const router = useRouter();
const { rand } = useFormat();
const { showToast } = useToast();
const { add: addPack } = useCart();
const loadFailed = ref(false);
const { cookies: catalog } = useCatalog();

// Start from the catalogue when it has this cookie (always, on a
// prerendered page), so the first render is the finished page.
const cookie = ref(findCookie(catalog.value, route.params.id));
const loading = ref(!cookie.value);
const quantity = ref(1);

const selectedImage = ref("");
const selectedPack = ref("");
const pack = computed(() =>
  cookie.value?.variants?.find((v) => v.variantId === selectedPack.value),
);
const gallery = computed(() =>
  cookie.value
    ? [
        { image: cookie.value.image, altText: name.value },
        ...(cookie.value.images || []),
      ]
    : [],
);
const image = computed(() => selectedImage.value || cookieImage(cookie.value));
const name = computed(() => cookieName(cookie.value));
const categoryLabel = computed(() =>
  formatCategoryLabel(cookie.value?.category),
);
// The cookie's own copy if it has some, otherwise the range's one-liner.
const blurb = computed(
  () =>
    cookie.value?.longDescription?.trim() ||
    categoryBlurb(cookie.value?.category),
);
const total = computed(
  () => (pack.value?.price || cookie.value?.price || 0) * quantity.value,
);
const crumbs = computed(() => breadcrumbsFor(route, { cookie: cookie.value }));

const related = computed(() =>
  (cookie.value?.related || []).map((c) => ({
    ...c,
    description: c.name,
    price: c.fromPrice,
  })),
);

const cartBusy = ref(false);
async function addToCart() {
  if (!pack.value) return false;
  cartBusy.value = true;
  try {
    await addPack(pack.value.variantId, quantity.value);
    return true;
  } catch (e) {
    showToast(errorMessage(e), "error", 5000);
    return false;
  } finally {
    cartBusy.value = false;
  }
}
async function addAndConfirm() {
  if (await addToCart())
    showToast(`Added ${quantity.value} × ${pack.value.label} to your cart.`, "success");
}
async function buyNow() {
  if (await addToCart()) router.push({ name: "Checkout", params: { step: "you" } });
}

let requestVersion = 0;
const fetchCookie = async () => {
  const version = ++requestVersion;
  loading.value = true;
  loadFailed.value = false;
  try {
    const data = await catalogApi.detail(route.params.id);
    if (version !== requestVersion) return;
    cookie.value = { ...data, description: data.name, price: data.fromPrice };
    selectedImage.value = "";
    selectedPack.value =
      data.variants.find((v) => v.available)?.variantId || "";
    if (data.slug && route.params.id !== data.slug)
      await router.replace({
        name: "ProductDetail",
        params: { id: data.slug },
      });
    applyHead(headForRoute(route, { cookie: cookie.value }));
  } catch (e) {
    if (version !== requestVersion) return;
    cookie.value = null;
    if (e.response?.status === 404)
      applyHead(headForRoute(route, { missing: true }));
    else loadFailed.value = true;
  } finally {
    if (version === requestVersion) loading.value = false;
  }
};
onMounted(fetchCookie);
watch(() => route.params.id, fetchCookie);
</script>

<template>
  <div class="pt-[var(--nav-h)]">
    <!-- Loading -->
    <div
      v-if="loading"
      class="container-magic pt-8 pb-24"
      aria-busy="true"
      aria-label="Loading cookie"
    >
      <div class="shimmer h-4 w-56 rounded-full" />
      <div class="mt-6 grid lg:grid-cols-[1.05fr_0.95fr] gap-10 lg:gap-16">
        <div class="shimmer aspect-square rounded-[28px]" />
        <div class="space-y-5 pt-4">
          <div class="shimmer h-5 w-28 rounded-full" />
          <div class="shimmer h-16 w-4/5 rounded-2xl" />
          <div class="shimmer h-8 w-32 rounded-full" />
          <div class="shimmer h-14 w-full rounded-full mt-10" />
          <div class="shimmer h-40 w-full rounded-2xl mt-8" />
        </div>
      </div>
    </div>

    <!-- Couldn't reach the server -->
    <div
      v-else-if="loadFailed"
      class="container-magic py-28 max-w-lg text-center"
      role="alert"
    >
      <img
        src="/img/cookies/lemon-almond.webp"
        alt=""
        class="mx-auto w-32 rotate-6 drop-shadow-cookie-sm"
      />
      <h1 class="mt-6 text-base font-bold">We couldn't load this cookie</h1>
      <p class="mt-2 text-base text-chocolate/80">
        Check your connection, then try again.
      </p>
      <BaseButton class="mt-8" @click="fetchCookie({ retry: true })"
        >Try again</BaseButton
      >
    </div>

    <!-- Not found -->
    <div v-else-if="!cookie" class="container-magic py-28 max-w-lg text-center">
      <img
        src="/img/cookies/cinnamon-pecan.webp"
        alt=""
        class="mx-auto w-32 -rotate-12 drop-shadow-cookie-sm"
      />
      <h1 class="font-display mt-6 text-display">This one's been eaten</h1>
      <p class="mt-3 text-base text-chocolate/80">
        We couldn't find that cookie. It may have sold out or moved.
      </p>
      <BaseButton :to="{ name: 'Products' }" class="mt-8"
        >See all cookies</BaseButton
      >
    </div>

    <template v-else>
      <div class="container-magic pt-6 pb-20 md:pb-28">
        <Breadcrumb :items="crumbs" class="mb-6" />

        <div
          class="grid lg:grid-cols-[1.05fr_0.95fr] gap-10 lg:gap-16 items-start"
        >
          <!-- Media -->
          <div class="lg:sticky lg:top-[calc(var(--nav-h)+2.4rem)]">
            <div
              class="relative isolate aspect-square overflow-hidden rounded-[28px] bg-chocolate flex items-center justify-center"
            >
              <div
                class="absolute inset-0 -z-10 bg-[radial-gradient(50%_45%_at_50%_52%,rgba(197,159,113,0.4),transparent_72%)]"
                aria-hidden="true"
              />
              <img
                :src="image"
                @error="imageFallback(cookie)($event)"
                :alt="name"
                fetchpriority="high"
                class="w-[78%] h-[78%] object-contain drop-shadow-[0_40px_40px_rgba(0,0,0,0.45)] animate-cookie-in"
              />
            </div>
            <div v-if="gallery.length > 1" class="flex flex-wrap gap-3 mt-4">
              <button
                v-for="(photo, i) in gallery"
                :key="photo.image"
                class="w-20 h-24 rounded-xl bg-tan p-2 focus-visible:outline"
                :aria-label="`View photo ${i + 1}`"
                :aria-pressed="image === cookieApi.getImageUrl(photo.image)"
                @click="selectedImage = cookieApi.getImageUrl(photo.image)"
              >
                <img
                  :src="cookieApi.getImageUrl(photo.image)"
                  :alt="photo.altText || name"
                  class="h-full w-full object-contain"
                />
              </button>
            </div>
          </div>

          <!-- Details -->
          <div class="lg:pt-2">
            <RouterLink
              :to="{ name: 'Products', query: { category: cookie.category } }"
              class="inline-flex items-center min-h-11 rounded-full bg-tan px-4 py-1.5 text-sm font-semibold text-chocolate hover:bg-tan-dark transition-colors"
            >
              {{ categoryLabel }}
            </RouterLink>

            <h1 class="font-display mt-4 text-display">{{ name }}</h1>
            <p v-if="blurb" class="mt-3 text-base text-chocolate/80">
              {{ blurb }}
            </p>

            <p class="mt-5 text-base font-bold tabular">
              {{ rand(pack?.price ?? cookie.fromPrice) }}
            </p>
            <p v-if="pack" class="mt-1 text-sm">
              {{ rand(pack.pricePerCookie) }} per cookie ·
              {{ pack.unitsPerPack }} in a pack
            </p>
            <p v-if="cookie.stockStatus === 'LOW_STOCK'" class="mt-4 font-bold">
              Only {{ cookie.lowStockRemaining }} cookies left
            </p>
            <p
              v-if="cookie.stockStatus === 'OUT_OF_STOCK'"
              class="mt-4 font-bold"
            >
              Currently sold out
            </p>
            <fieldset class="mt-6">
              <legend class="font-bold">Choose your pack</legend>
              <div class="flex flex-wrap gap-2 mt-3">
                <label
                  v-for="v in cookie.variants"
                  :key="v.variantId"
                  class="border border-primary rounded-full px-4 py-3"
                  :class="!v.available ? 'opacity-50' : ''"
                  ><input
                    v-model="selectedPack"
                    type="radio"
                    name="pack"
                    :value="v.variantId"
                    :disabled="!v.available"
                    class="mr-2"
                  />{{ v.label
                  }}<span v-if="!v.available"> · Sold out</span></label
                >
              </div>
            </fieldset>

            <!-- Purchase -->
            <div class="mt-8 flex flex-wrap items-center gap-3">
              <QuantitySelector v-model="quantity" />
              <BaseButton
                size="lg"
                class="h-14 flex-1 min-w-0"
                :disabled="!pack?.available || cartBusy"
                @click="addAndConfirm"
              >
                <ShoppingBagIcon class="h-5 w-5" />
                Add to cart
                <span
                  class="font-normal text-cream-100/75 tabular"
                  v-if="quantity > 1"
                  >&middot; {{ rand(total) }}</span
                >
              </BaseButton>
            </div>
            <BaseButton
              variant="outline"
              size="lg"
              block
              class="mt-3 h-14"
              :disabled="!pack?.available || cartBusy"
              @click="buyNow"
            >
              Buy now
            </BaseButton>

            <!-- Details -->
            <CookieInfoCard :cookie="cookie" class="mt-8" />
            <section
              v-if="
                cookie.nutrition &&
                Object.values(cookie.nutrition).some((v) => v != null)
              "
              class="mt-8"
            >
              <h2 class="font-display text-3xl">Nutrition per cookie</h2>
              <dl class="mt-4 divide-y divide-chocolate/15">
                <template
                  v-for="(label, key) in {
                    weightGrams: 'Weight (g)',
                    calories: 'Energy (kcal)',
                    sugarGrams: 'Sugar (g)',
                    proteinGrams: 'Protein (g)',
                    carbsGrams: 'Carbohydrates (g)',
                    fatGrams: 'Fat (g)',
                  }"
                  :key="key"
                  ><div
                    v-if="cookie.nutrition[key] != null"
                    class="flex justify-between py-3"
                  >
                    <dt>{{ label }}</dt>
                    <dd class="font-bold tabular">
                      {{ cookie.nutrition[key] }}
                    </dd>
                  </div></template
                >
              </dl>
            </section>
          </div>
        </div>
      </div>

      <ProductReviews :key="cookie.cookieId" :cookie-id="cookie.cookieId" />
      <!-- Related -->
      <section
        v-if="related.length"
        class="bg-background py-20 md:py-24"
        aria-labelledby="related-title"
      >
        <div class="container-magic">
          <div class="flex flex-wrap items-end justify-between gap-4 mb-10">
            <h2 id="related-title" class="font-display text-display">
              You might also like
            </h2>
            <RouterLink
              :to="{ name: 'Products' }"
              class="inline-flex items-center min-h-11 font-semibold underline decoration-primary decoration-2 underline-offset-[6px] hover:decoration-chocolate"
            >
              See all cookies
            </RouterLink>
          </div>
          <div class="grid md:grid-cols-2 gap-4 sm:gap-5">
            <RelatedCookieCard
              v-for="c in related"
              :key="c.cookieId"
              :related="c"
            />
          </div>
        </div>
      </section>
    </template>
  </div>
</template>
