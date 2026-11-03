<script setup>
/**
 * FeaturedCookies — "Signature Crumble Drops". Two frosted cards on the
 * chocolate block, each with a cookie floating out of the top.
 * Prefers live cookies the showcase above hasn't already shown.
 */
import { computed, onMounted } from "vue";
import { RouterLink } from "vue-router";
import { ArrowRightIcon } from "@heroicons/vue/20/solid";
import { ShoppingBagIcon } from "@heroicons/vue/24/outline";
import BaseButton from "../ui/BaseButton.vue";
import {
  useCatalog,
  cookieImage,
  cookieName,
  imageFallback,
  onePerCategory,
} from "../../composables/useCatalog";
import { useFormat } from "../../composables/useFormat";
import { useScrollReveal } from "../../composables/useScrollReveal";
import {
  categoryBlurb,
  formatCategoryLabel,
} from "../../constants/cookieCategories";

const { cookies: catalog, load } = useCatalog();
const { rand } = useFormat();
const { vReveal } = useScrollReveal();

onMounted(load);

const signature = [
  {
    key: "sig-1",
    name: "Vanilla White Chocolate",
    description:
      "A vanilla-rich cookie with creamy white chocolate chunks, a tender middle and lightly golden edges.",
    image: "/img/cookies/vanilla-white-chocolate.webp",
    label: "Signature",
    price: null,
    to: { name: "Products" },
  },
  {
    key: "sig-2",
    name: "Peanut Chocolate Ripple",
    description:
      "Roasted peanut butter ribbons and dark chocolate chunks in a soft, chewy golden cookie.",
    image: "/img/cookies/peanut-chocolate-ripple.webp",
    label: "Signature",
    price: null,
    to: { name: "Products" },
  },
];

const cookies = computed(() => {
  const all = catalog.value.filter((c) => c.featured);
  if (!all.length) return signature;
  // Skip what CookieShowcase shows (one per range) when there's enough to go round.
  const shown = new Set(
    onePerCategory(all)
      .slice(0, 4)
      .map((c) => c.cookieId),
  );
  const rest = all.filter((c) => !shown.has(c.cookieId));
  const pool =
    rest.length >= 2 ? onePerCategory(rest).concat(rest) : [...all].reverse();
  return [...new Set(pool)].slice(0, 2).map((c) => ({
    key: c.cookieId,
    name: cookieName(c),
    description:
      c.longDescription || categoryBlurb(c.category) || c.ingredients,
    image: cookieImage(c),
    onError: imageFallback(c),
    label: formatCategoryLabel(c.category),
    price: c.price,
    to: { name: "ProductDetail", params: { id: c.slug || c.cookieId } },
  }));
});
</script>

<template>
  <section
    class="featured relative isolate overflow-hidden bg-chocolate py-24 md:py-32 hidden md:block"
    aria-labelledby="featured-title"
  >
    <div class="container-magic">
      <h2
        id="featured-title"
        v-reveal
        class="font-display text-center text-cream-50 text-display"
      >
        Signature <span class="text-primary">crumble drops</span>
      </h2>
      <p
        v-reveal
        class="prose-copy !text-cream-100/80 text-center mx-auto mt-5"
      >
        The two we'd put in your hand first.
      </p>

      <div
        class="mt-28 md:mt-36 grid gap-32 lg:grid-cols-2 lg:gap-10 xl:gap-16"
      >
        <article
          v-for="(cookie, i) in cookies"
          :key="cookie.key"
          v-reveal
          :data-reveal-delay="i * 120"
          class="group relative"
        >
          <img
            :src="cookie.image"
            :alt="cookie.name"
            loading="lazy"
            class="absolute left-1/2 top-0 z-10 w-56 md:w-64 -translate-x-1/2 -translate-y-[62%] drop-shadow-[0_30px_30px_rgba(0,0,0,0.45)] transition-transform duration-700 ease-cookie group-hover:-translate-y-[70%] group-hover:-rotate-6 group-hover:scale-105"
            @error="cookie.onError && cookie.onError($event)"
          />

          <div
            class="relative flex h-full flex-col rounded-[28px] border border-cream-50/10 bg-cream-50/[0.07] px-7 pb-7 pt-32 md:px-10 md:pb-9 md:pt-36 text-center shadow-[inset_0_1px_0_rgba(255,255,255,0.08)] backdrop-blur-md"
          >
            <p class="text-sm font-semibold text-primary">{{ cookie.label }}</p>
            <h3 class="mt-2 text-base text-cream-50">{{ cookie.name }}</h3>
            <p
              class="mx-auto mt-4 mb-8 max-w-md text-base leading-relaxed text-cream-100/80"
            >
              {{ cookie.description }}
            </p>

            <div
              class="mt-auto pt-7 flex flex-wrap items-center gap-4 border-t border-cream-50/10"
              :class="cookie.price ? 'justify-between' : 'justify-center'"
            >
              <span
                v-if="cookie.price"
                class="text-base font-semibold text-cream-50 tabular"
                >{{ rand(cookie.price) }}</span
              >
              <div class="flex items-center gap-2.5">
                <BaseButton :to="cookie.to" variant="secondary">
                  View cookie
                  <ArrowRightIcon class="h-5 w-5" />
                </BaseButton>
                <RouterLink
                  :to="cookie.to"
                  class="h-[52px] w-[52px] flex items-center justify-center rounded-full bg-surface text-chocolate transition-transform hover:scale-105"
                  :aria-label="`Buy ${cookie.name}`"
                >
                  <ShoppingBagIcon class="h-5 w-5" />
                </RouterLink>
              </div>
            </div>
          </div>
        </article>
      </div>
    </div>
  </section>
</template>

<style scoped>
/* The original cookie-crumb photo as a faint texture behind the cards */
.featured::before {
  content: "";
  position: absolute;
  inset: 0;
  z-index: -1;
  background: url("/cookie-background.jpeg") center / cover;
  opacity: 0.08;
  transform: scale(1.3);
  pointer-events: none;
}
</style>
