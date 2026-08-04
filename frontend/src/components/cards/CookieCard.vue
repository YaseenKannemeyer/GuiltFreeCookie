<script setup>
/**
 * CookieCard — premium cookie card with circular image, splash, badges,
 * rating, price, and dual CTA (View Details + Add to Cart).
 *
 * Variants:
 *   light  — on cream/background surface (homepage featured grid)
 *   dark   — on chocolate background (related cookies on product page)
 *
 * NOTE: your backend Cookie entity currently only returns:
 *   cookieId, category, description, ingredients, allergies, price
 * It does NOT return: name, image, rating, badges.
 * This component now falls back gracefully when those are missing,
 * using `description` as the title and `category` as a badge.
 * Once you add real `name`/`imageUrl`/`rating` columns, this will
 * automatically prefer those instead.
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

// Your entity's primary key is `cookieId`, not `id`.
const cookieId = computed(() => props.cookie.cookieId ?? props.cookie.id);

// Title: prefer a real `name` field if you add one later, else fall back
// to description (truncated), else a generic label so it never renders blank.
const displayName = computed(
  () =>
    props.cookie.name ||
    (props.cookie.description
      ? truncate(props.cookie.description, 40)
      : "Cookie"),
);

// Description: avoid showing the same text twice when there's no separate
// name field — if description is being used as the title, show category
// or ingredients as the subtext instead.
const displayDescription = computed(() => {
  if (props.cookie.name) return props.cookie.description || "";
  return props.cookie.ingredients || "";
});

// Image: fall back to a placeholder so <img> never gets `undefined`.
const displayImage = computed(
  () =>
    props.cookie.image ||
    props.cookie.imageUrl ||
    "/images/cookie-placeholder.png",
);

// Badges: fall back to category as a single badge if no badges array exists.
const displayBadges = computed(() => {
  if (Array.isArray(props.cookie.badges) && props.cookie.badges.length) {
    return props.cookie.badges;
  }
  return props.cookie.category
    ? [props.cookie.category.replace(/_/g, " ")]
    : [];
});

// Rating: only render RatingStars if a real rating exists.
const hasRating = computed(
  () => props.cookie.rating !== undefined && props.cookie.rating !== null,
);

const goToDetails = () =>
  router.push({ name: "ProductDetail", params: { id: cookieId.value } });
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
    <div
      v-if="displayBadges.length"
      class="absolute top-5 left-5 z-10 flex flex-col gap-1.5 items-start"
    >
      <BaseBadge
        v-for="(b, i) in displayBadges"
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
      @click.stop
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
          :src="displayImage"
          :alt="displayName"
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
          {{ displayName }}
        </h3>
      </div>

      <RatingStars
        v-if="hasRating"
        :rating="cookie.rating"
        :show-value="true"
        size="sm"
      />

      <p
        v-if="displayDescription"
        :class="[
          'text-card leading-relaxed min-h-[3.5rem]',
          isDark ? 'text-cream/70' : 'text-chocolate/60',
        ]"
      >
        {{ truncate(displayDescription, 90) }}
      </p>

      <div class="flex items-center justify-between pt-3">
        <span
          :class="[
            'text-2xl font-bold',
            isDark ? 'text-primary-200' : 'text-chocolate',
          ]"
        >
          R{{ cookie.price }}
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
