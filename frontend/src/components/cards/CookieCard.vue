<script setup>
/**
 * CookieCard — premium cookie card with circular image, splash, badges,
 * rating, price, and dual CTA (View Details + Add to Cart).
 *
 * Variants:
 *   light  — on cream/background surface (homepage featured grid)
 *   dark   — on chocolate background (related cookies on product page)
 */
import { computed } from "vue";
import { useRouter } from "vue-router";
import { ShoppingBagIcon, EyeIcon } from "@heroicons/vue/24/outline";
import RatingStars from "../../components/ui/RatingStars.vue";
import BaseBadge from "../../components/ui/BaseBadge.vue";
import { useFormat } from "../../composables/useFormat";

const props = defineProps({
  cookie: { type: Object, required: true },
  variant: { type: String, default: "light" }, // light | dark
});

const router = useRouter();

const { currency, truncate } = useFormat();

const isDark = computed(() => props.variant === "dark");

const goToDetails = () =>
  router.push({ name: "Products", params: { id: props.cookie.id } });
</script>

<template>
  <article
    :class="[
      'group relative overflow-hidden rounded-4xl p-6 cursor-pointer transition-all duration-500 ease-cookie',
      isDark
        ? 'bg-chocolate-400/40 text-cream border border-cream/10 hover:bg-chocolate-300/60'
        : 'bg-surface text-chocolate shadow-soft hover:shadow-soft-lg hover:-translate-y-1.5',
    ]"
    @click="goToDetails"
  >
    <!-- Badges -->
    <div class="absolute top-5 left-5 z-10 flex flex-col gap-1.5 items-start">
      <BaseBadge
        v-for="(b, i) in cookie.badges"
        :key="i"
        :variant="isDark ? 'primary' : 'chocolate'"
        size="sm"
      >
        {{ b }}
      </BaseBadge>
    </div>

    <!-- Wishlist / quick add icon -->
    <button
      type="button"
      :class="[
        'absolute top-5 right-5 z-10 flex items-center justify-center h-10 w-10 rounded-full transition-all duration-300',
        isDark
          ? 'bg-cream/10 text-cream hover:bg-primary hover:text-chocolate'
          : 'bg-cream-100 text-chocolate/60 hover:bg-primary hover:text-chocolate',
      ]"
      aria-label="Quick add to cart"
    >
      <ShoppingBagIcon class="h-5 w-5" />
    </button>

    <!-- Circular cookie image with splash glow -->
    <div class="relative flex items-center justify-center mb-6 mt-8">
      <div
        class="absolute inset-0 -m-4 rounded-full bg-primary/30 blur-2xl opacity-0 group-hover:opacity-100 transition-opacity duration-500"
      />
      <div
        :class="[
          'relative w-44 h-44 rounded-full overflow-hidden transition-transform duration-700 ease-cookie group-hover:scale-110 group-hover:rotate-6',
          isDark
            ? 'shadow-cookie ring-4 ring-cream/10'
            : 'shadow-cookie ring-4 ring-cream-100',
        ]"
      >
        <img
          :src="cookie.image"
          :alt="cookie.name"
          loading="lazy"
          class="w-full h-full object-cover"
        />
      </div>
    </div>

    <!-- Body -->
    <div class="space-y-2.5">
      <div class="flex items-center justify-between gap-2">
        <h3
          :class="[
            'text-xl font-semibold',
            isDark ? 'text-cream' : 'text-chocolate',
          ]"
        >
          {{ cookie.name }}
        </h3>
      </div>

      <RatingStars :rating="cookie.rating" :show-value="true" size="sm" />

      <p
        :class="[
          'text-card leading-relaxed min-h-[3.5rem]',
          isDark ? 'text-cream/70' : 'text-chocolate/60',
        ]"
      >
        {{ truncate(cookie.description, 90) }}
      </p>

      <div class="flex items-center justify-between pt-3">
        <span
          :class="[
            'text-2xl font-bold',
            isDark ? 'text-primary-200' : 'text-chocolate',
          ]"
        >
          {{ currency(cookie.price) }}
        </span>
        <div class="flex items-center gap-2">
          <button
            type="button"
            :class="[
              'inline-flex items-center gap-1.5 px-4 py-2.5 rounded-full text-sm font-medium transition-all duration-300',
              isDark
                ? 'bg-cream/10 text-cream hover:bg-cream/20'
                : 'bg-cream-100 text-chocolate hover:bg-cream-200',
            ]"
            @click.stop="goToDetails"
          >
            <EyeIcon class="h-4 w-4" />
            View More
          </button>
        </div>
      </div>
    </div>
  </article>
</template>
