<script setup>
import { useSiteInfo } from "../composables/useSiteInfo";
import { computed, onMounted } from "vue";
import { RouterLink } from "vue-router";
import { EnvelopeIcon, PhoneIcon, MapPinIcon } from "@heroicons/vue/24/outline";
import NewsletterSection from "../components/sections/NewsletterSection.vue";
import { LEGAL_PAGES } from "../constants/business";
import {
  COOKIE_CATEGORIES,
  formatCategoryLabel,
} from "../constants/cookieCategories";
import { useCatalog } from "../composables/useCatalog";

const { info, load } = useSiteInfo();
onMounted(load);
const year = new Date().getFullYear();
const { cookies: catalog } = useCatalog();

// The ranges actually on the shelf, in the usual order. Before the
// catalogue has loaded, the four ranges the home page features.
const FEATURED_RANGES = [
  "DIABETIC_FRIENDLY",
  "HIGH_PROTEIN",
  "GLUTEN_FREE",
  "VEGAN",
];
const ranges = computed(() => {
  const stocked = new Set(catalog.value.map((c) => c.category));
  return stocked.size
    ? COOKIE_CATEGORIES.filter((c) => stocked.has(c))
    : FEATURED_RANGES;
});

// Every link goes somewhere real; no two links share a destination's job.
const columns = computed(() => [
  {
    title: "Shop",
    links: [
      { label: "All cookies", to: "/products" },
      ...ranges.value.map((category) => ({
        label: formatCategoryLabel(category),
        to: { name: "Products", query: { category } },
      })),
    ],
  },
  {
    title: "Explore",
    links: [
      { label: "Home", to: "/" },
      { label: "About us", to: "/about" },
      { label: "Contact", to: "/contact" },
    ],
  },
  {
    title: "Help",
    links: [
      { label: "FAQ", to: "/contact#faq-title" },
      { label: "Bulk and event orders", to: "/contact" },
      { label: "Ingredients and allergens", to: "/#inside" },
    ],
  },
  {
    title: "Legal",
    links: LEGAL_PAGES.map((p) => ({ label: p.label, to: p.path })),
  },
]);

const SOCIAL_ICONS = [
  {
    key: "instagram",
    name: "Instagram",
    path: "M12 2.2c3.2 0 3.6 0 4.8.07 1.17.05 1.97.24 2.67.5.72.28 1.33.66 1.94 1.27.61.61.99 1.22 1.27 1.94.26.7.45 1.5.5 2.67.06 1.2.07 1.6.07 4.8s0 3.6-.07 4.8c-.05 1.17-.24 1.97-.5 2.67a5.4 5.4 0 0 1-1.27 1.94 5.4 5.4 0 0 1-1.94 1.27c-.7.26-1.5.45-2.67.5-1.2.06-1.6.07-4.8.07s-3.6 0-4.8-.07c-1.17-.05-1.97-.24-2.67-.5a5.4 5.4 0 0 1-1.94-1.27 5.4 5.4 0 0 1-1.27-1.94c-.26-.7-.45-1.5-.5-2.67C2.2 15.6 2.2 15.2 2.2 12s0-3.6.07-4.8c.05-1.17.24-1.97.5-2.67.28-.72.66-1.33 1.27-1.94.61-.61 1.22-.99 1.94-1.27.7-.26 1.5-.45 2.67-.5C8.4 2.2 8.8 2.2 12 2.2zm0 1.8c-3.15 0-3.5 0-4.74.07-.9.04-1.38.2-1.7.32-.43.17-.74.37-1.06.69-.32.32-.52.63-.69 1.06-.13.32-.28.8-.32 1.7C3.2 8.5 3.2 8.85 3.2 12s0 3.5.07 4.74c.04.9.2 1.38.32 1.7.17.43.37.74.69 1.06.32.32.63.52 1.06.69.32.13.8.28 1.7.32 1.24.07 1.6.07 4.74.07s3.5 0 4.74-.07c.9-.04 1.38-.2 1.7-.32.43-.17.74-.37 1.06-.69.32-.32.52-.63.69-1.06.13-.32.28-.8.32-1.7.07-1.24.07-1.6.07-4.74s0-3.5-.07-4.74c-.04-.9-.2-1.38-.32-1.7a2.85 2.85 0 0 0-.69-1.06 2.85 2.85 0 0 0-1.06-.69c-.32-.13-.8-.28-1.7-.32C15.5 4 15.15 4 12 4zm0 3.06A4.94 4.94 0 1 0 12 17a4.94 4.94 0 0 0 0-9.88zm0 1.8a3.14 3.14 0 1 1 0 6.28 3.14 3.14 0 0 1 0-6.28zm5.15-.45a1.15 1.15 0 1 1-2.3 0 1.15 1.15 0 0 1 2.3 0z",
  },
  {
    key: "x",
    name: "X",
    path: "M18.244 2.25h3.308l-7.227 8.26 8.502 11.24h-6.66l-5.214-6.817L4.99 21.75H1.68l7.73-8.835L1.254 2.25H8.08l4.713 6.231zm-1.161 17.52h1.833L7.084 4.126H5.117z",
  },
  {
    key: "facebook",
    name: "Facebook",
    path: "M22 12a10 10 0 1 0-11.56 9.88v-6.99H7.9V12h2.54V9.8c0-2.5 1.49-3.89 3.78-3.89 1.09 0 2.24.2 2.24.2v2.46h-1.26c-1.24 0-1.63.77-1.63 1.56V12h2.78l-.44 2.89h-2.34v6.99A10 10 0 0 0 22 12z",
  },
  {
    key: "linkedin",
    name: "LinkedIn",
    path: "M20.45 20.45h-3.56v-5.57c0-1.33-.03-3.04-1.85-3.04-1.85 0-2.13 1.45-2.13 2.94v5.67H9.35V9h3.42v1.56h.05c.48-.9 1.64-1.85 3.37-1.85 3.6 0 4.27 2.37 4.27 5.45v6.29zM5.34 7.43a2.07 2.07 0 1 1 0-4.14 2.07 2.07 0 0 1 0 4.14zM7.12 20.45H3.56V9h3.56v11.45zM22.22 0H1.77C.79 0 0 .77 0 1.72v20.56C0 23.23.79 24 1.77 24h20.45c.98 0 1.78-.77 1.78-1.72V1.72C24 .77 23.2 0 22.22 0z",
  },
];

// Only accounts that exist (set their URLs in constants/business.js).
const socials = computed(() =>
  SOCIAL_ICONS.filter((s) =>
    /^https?:\/\//.test(info.value?.social?.[s.key] || ""),
  ).map((s) => ({ ...s, href: info.value.social[s.key] })),
);
</script>

<template>
  <footer class="bg-chocolate text-cream-100 relative overflow-hidden">
    <!-- Newsletter band -->
    <NewsletterSection tone="dark" />

    <!-- Main footer grid -->
    <div
      class="container-magic py-14 grid grid-cols-2 md:grid-cols-4 lg:grid-cols-[1.6fr_1fr_1fr_1fr_1fr] gap-x-8 gap-y-12"
    >
      <!-- Brand col -->
      <div class="col-span-2 md:col-span-4 lg:col-span-1">
        <RouterLink to="/" class="inline-block wordmark text-cream-50 mb-5">
          GuiltFree<span class="text-primary">Cookie</span>
        </RouterLink>
        <p class="text-cream-100/75 text-base max-w-sm mb-7">
          Sweet, chocolatey cookies with less of the stuff you'd rather skip.
          Baked fresh in Cape Town.
        </p>
        <ul class="text-base text-cream-100/80">
          <li v-if="info?.phone" class="flex items-center gap-3">
            <PhoneIcon class="h-5 w-5 text-primary shrink-0" />
            <a
              v-if="info?.phone"
              :href="`tel:${info.phone.replace(/[^+0-9]/g, '')}`"
              class="inline-flex items-center min-h-11 hover:text-cream-50 transition-colors"
              >{{ info.phone }}</a
            >
          </li>
          <li v-if="info?.supportEmail" class="flex items-center gap-3">
            <EnvelopeIcon class="h-5 w-5 text-primary shrink-0" />
            <a
              v-if="info?.supportEmail"
              :href="`mailto:${info.supportEmail}`"
              class="inline-flex items-center min-h-11 hover:text-cream-50 transition-colors"
              >{{ info.supportEmail }}</a
            >
          </li>
          <li v-if="info?.address" class="flex items-start gap-3">
            <MapPinIcon class="h-5 w-5 text-primary shrink-0 mt-0.5" />
            {{ info?.address }}
          </li>
        </ul>
      </div>

      <!-- Link columns -->
      <div v-for="col in columns" :key="col.title">
        <h3 class="text-cream-50 text-base font-semibold mb-4">
          {{ col.title }}
        </h3>
        <ul>
          <li v-for="link in col.links" :key="link.label">
            <RouterLink
              :to="link.to"
              class="inline-flex items-center min-h-11 text-cream-100/75 hover:text-primary transition-colors text-base"
            >
              {{ link.label }}
            </RouterLink>
          </li>
        </ul>
      </div>
    </div>

    <!-- Bottom bar -->
    <div class="border-t border-cream-50/10">
      <div
        class="container-magic py-6 flex flex-col-reverse md:flex-row items-center justify-between gap-4"
      >
        <p class="text-cream-100/60 text-sm" data-allow-mismatch="text">
          &copy; {{ year }} GuiltFreeCookie. Baked in Cape Town.
        </p>
        <div v-if="socials.length" class="flex items-center gap-2">
          <a
            v-for="s in socials"
            :key="s.name"
            :href="s.href"
            :aria-label="s.name"
            class="flex items-center justify-center h-11 w-11 rounded-full text-cream-100/75 hover:bg-primary hover:text-chocolate transition-colors"
          >
            <svg
              viewBox="0 0 24 24"
              class="h-5 w-5"
              fill="currentColor"
              aria-hidden="true"
            >
              <path :d="s.path" />
            </svg>
          </a>
        </div>
      </div>
    </div>
  </footer>
</template>
