<script setup>
/**
 * MadeForSection — who the cookies are for, as big linked rows. Each row
 * deep-links into the product list pre-filtered to the matching range.
 * On desktop a cookie slides in on hover; on mobile it sits beside the text.
 */
import { RouterLink } from "vue-router";
import { ArrowRightIcon } from "@heroicons/vue/24/outline";
import { useScrollReveal } from "../../composables/useScrollReveal";

const { vReveal } = useScrollReveal();

const rows = [
  {
    title: "Choosing your sweeteners",
    body: "Read each recipe’s ingredients and nutrition before choosing.",
    category: "DIABETIC_FRIENDLY",
    cta: "Shop diabetic friendly",
    img: "/img/cookies/cinnamon-pecan.webp",
  },
  {
    title: "Training, lifting, racing",
    body: "A chocolate fix with protein behind it, for after the session.",
    category: "HIGH_PROTEIN",
    cta: "Shop high protein",
    img: "/img/cookies/double-chocolate-chunk.webp",
  },
  {
    title: "Cutting back on sugar",
    body: "All the cookie-jar comfort with a lot less sugar in it.",
    category: "LOW_SUGAR",
    cta: "Shop low sugar",
    img: "/img/cookie.webp",
  },
  {
    title: "Skipping gluten or dairy",
    body: "Gluten-free and vegan bakes that nobody at the table will clock.",
    category: "GLUTEN_FREE",
    cta: "Shop gluten free",
    img: "/img/cookies/lemon-almond.webp",
  },
];
</script>

<template>
  <section class="section bg-background" aria-labelledby="made-for-title">
    <div class="container-magic">
      <h2 id="made-for-title" v-reveal class="font-display text-display max-w-3xl">
        Made for every kind of sweet tooth
      </h2>

      <ul class="mt-12 border-t border-chocolate/15">
        <li v-for="(row, i) in rows" :key="row.title" v-reveal :data-reveal-delay="i * 60">
          <RouterLink
            :to="{ name: 'Products', query: { category: row.category } }"
            class="group relative grid grid-cols-[1fr_auto] lg:grid-cols-[1.1fr_1fr_9.6rem_auto] items-center gap-x-6 gap-y-2 py-6 border-b border-chocolate/15"
          >
            <h3 class="text-base font-bold transition-transform duration-500 ease-cookie lg:group-hover:translate-x-3">
              {{ row.title }}
            </h3>

            <img
              :src="row.img"
              alt=""
              loading="lazy"
              class="row-start-1 col-start-2 h-16 w-16 sm:h-20 sm:w-20 object-contain drop-shadow-cookie-sm lg:hidden"
            />

            <p class="col-span-2 lg:col-span-1 text-chocolate/80 text-base max-w-[38ch]">
              {{ row.body }}
            </p>

            <!-- Desktop hover cookie: its own column, so it never covers text -->
            <img
              :src="row.img"
              alt=""
              loading="lazy"
              class="pointer-events-none hidden lg:block h-20 w-20 -my-4 justify-self-center object-contain drop-shadow-cookie opacity-0 translate-x-6 rotate-12 scale-75 transition-all duration-500 ease-cookie group-hover:opacity-100 group-hover:translate-x-0 group-hover:rotate-0 group-hover:scale-100"
            />

            <span
              class="col-span-2 lg:col-span-1 inline-flex items-center gap-2 font-semibold text-chocolate lg:justify-end"
            >
              <span class="lg:sr-only">{{ row.cta }}</span>
              <span
                class="flex h-12 w-12 items-center justify-center rounded-full border-2 border-chocolate/20 transition-colors duration-300 group-hover:bg-chocolate group-hover:border-chocolate group-hover:text-cream-50"
                aria-hidden="true"
              >
                <ArrowRightIcon class="h-5 w-5 transition-transform duration-300 group-hover:-rotate-45" />
              </span>
            </span>


          </RouterLink>
        </li>
      </ul>
    </div>
  </section>
</template>
