<script setup>
/**
 * CookieCard — product tile for the product list and "you might also like".
 * The whole card is one link to the product page.
 */
import { computed } from "vue";
import { RouterLink } from "vue-router";
import { ArrowUpRightIcon } from "@heroicons/vue/20/solid";
import { useFormat } from "../../composables/useFormat";
import {
  cookieImage,
  allergenText,
  cookieName,
  imageFallback,
} from "../../composables/useCatalog";
import {
  formatCategoryLabel,
  categoryTint,
} from "../../constants/cookieCategories";

const props = defineProps({
  cookie: {
    type: Object,
    required: true,
  },
  // Lazy-load everything below the first row
  eager: {
    type: Boolean,
    default: false,
  },
});

const { rand } = useFormat();

const image = computed(() => cookieImage(props.cookie));
const label = computed(() => formatCategoryLabel(props.cookie.category));
const tint = computed(() => categoryTint(props.cookie.category));
const allergens = computed(() => allergenText(props.cookie));
const name = computed(() => cookieName(props.cookie));
</script>

<template>
  <RouterLink
    :to="{
      name: 'ProductDetail',
      params: { id: props.cookie.slug || props.cookie.cookieId },
    }"
    class="group block rounded-2xl focus-visible:outline-offset-4"
  >
    <div
      class="relative aspect-[4/5] overflow-hidden rounded-2xl flex items-center justify-center transition-[filter] duration-500 group-hover:brightness-[0.97]"
      :style="{ backgroundColor: tint }"
    >
      <div
        class="absolute inset-0 bg-[radial-gradient(55%_45%_at_50%_50%,rgba(255,255,255,0.35),transparent_70%)]"
        aria-hidden="true"
      />
      <img
        :src="image"
        @error="imageFallback(props.cookie)($event)"
        :alt="name"
        :loading="eager ? 'eager' : 'lazy'"
        class="relative w-[74%] h-[74%] object-contain drop-shadow-cookie-sm transition-transform duration-700 ease-cookie group-hover:-rotate-[8deg] group-hover:scale-[1.06]"
      />
      <span
        class="absolute top-3 right-3 h-10 w-10 rounded-full bg-chocolate text-cream-50 flex items-center justify-center opacity-0 scale-75 transition-all duration-300 ease-cookie group-hover:opacity-100 group-hover:scale-100 group-focus-visible:opacity-100 group-focus-visible:scale-100"
        aria-hidden="true"
      >
        <ArrowUpRightIcon class="h-5 w-5" />
      </span>
    </div>

    <div class="pt-4 px-1">
      <p class="text-sm font-semibold text-primary-700">{{ label }}</p>
      <div
        class="mt-0.5 flex flex-col sm:flex-row sm:items-start sm:justify-between gap-x-3"
      >
        <h3
          class="text-base font-semibold leading-snug text-chocolate line-clamp-2"
        >
          {{ name }}
        </h3>
        <span class="shrink-0 text-base font-semibold tabular text-chocolate">
          {{ rand(props.cookie.fromPrice ?? props.cookie.price) }}
        </span>
      </div>
      <p
        v-if="cookie.stockStatus === 'OUT_OF_STOCK'"
        class="mt-2 text-sm font-bold"
      >
        Sold out
      </p>
      <p v-else-if="cookie.stockStatus === 'LOW_STOCK'" class="mt-2 text-sm">
        Low stock
      </p>
      <p v-if="cookie.reviewCount" class="mt-1 text-sm">
        {{ Number(cookie.averageRating).toFixed(1) }} / 5 ·
        {{ cookie.reviewCount }} reviews
      </p>
      <p v-if="allergens" class="mt-1 text-sm text-chocolate/70 line-clamp-1">
        Allergens: {{ allergens }}
      </p>
    </div>
  </RouterLink>
</template>
