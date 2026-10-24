<script setup>
/**
 * TheNavbar — fixed storefront navigation.
 *
 * - Transparent over the home hero, solid cream once the page scrolls
 *   (an IntersectionObserver on #top-sentinel, no scroll listener). Stays
 *   transparent while the hero's pinned cookie-drop sequence is on screen.
 * - Desktop: wordmark + links + search / account / cart.
 * - Mobile: hamburger that opens a slide-in drawer.
 */
import { ref, computed, watch, onMounted, onUnmounted } from "vue";
import { RouterLink, useRouter, useRoute } from "vue-router";
import { cookieDropPinned } from "../composables/cookieDropState";
import { useToast } from "../composables/useToast";
import {
  Bars3Icon,
  XMarkIcon,
  ShoppingBagIcon,
  MagnifyingGlassIcon,
} from "@heroicons/vue/24/outline";

const router = useRouter();
const route = useRoute();

const sidebarOpen = ref(false);
const scrolled = ref(false);

const isHomePage = computed(() => route.name === "Home");
const overHero = computed(
  () => isHomePage.value && (!scrolled.value || cookieDropPinned.value),
);

let observer = null;
onMounted(() => {
  const sentinel = document.getElementById("top-sentinel");
  if (!sentinel || typeof IntersectionObserver === "undefined") return;
  observer = new IntersectionObserver(([entry]) => {
    scrolled.value = !entry.isIntersecting;
  });
  observer.observe(sentinel);
});
onUnmounted(() => observer?.disconnect());

const closeSidebar = () => {
  sidebarOpen.value = false;
};

watch(sidebarOpen, (open) => {
  document.body.style.overflow = open ? "hidden" : "";
});
watch(() => route.fullPath, closeSidebar);

const navLinks = [
  { label: "Home", to: { name: "Home" } },
  { label: "Products", to: { name: "Products" } },
  { label: "About", to: { name: "About" } },
  { label: "Contact", to: { name: "Contact" } },
  { label: "Account", to: { name: "Account" } },
];

const goSearch = () => {
  router.push({ name: "Products", query: { focus: "search" } });
};

// Same message as "Add to cart" until the cart exists.
const { showToast } = useToast();
const openCart = () => {
  showToast(
    "The cart is almost ready. You'll be able to order very soon.",
    "info",
  );
};
</script>

<template>
  <header
    :class="[
      'fixed inset-x-0 top-0 z-50 transition-[background-color,box-shadow,backdrop-filter] duration-500 ease-cookie',
      overHero
        ? 'bg-transparent'
        : 'bg-background/90 backdrop-blur-xl shadow-[0_1px_0_rgba(46,14,11,0.08),0_10px_30px_-18px_rgba(46,14,11,0.35)]',
    ]"
  >
    <div
      class="container-magic flex h-[var(--nav-h)] items-center justify-between gap-6"
    >
      <div class="flex items-center gap-12">
        <!-- Wordmark -->
        <RouterLink
          :to="{ name: 'Home' }"
          class="wordmark text-chocolate pt-1 shrink-0"
          aria-label="GuiltFreeCookie home"
        >
          GuiltFree<span
            :class="overHero ? 'sticker sticker-sm' : 'text-primary-600'"
            >Cookie</span
          >
        </RouterLink>

        <!-- Desktop nav -->
        <nav class="hidden lg:flex items-center gap-8" aria-label="Main">
          <RouterLink
            v-for="link in navLinks"
            :key="link.label"
            :to="link.to"
            class="nav-link inline-flex items-center min-h-11 text-base font-semibold text-chocolate"
            active-class="router-link-active"
          >
            {{ link.label }}
          </RouterLink>
        </nav>
      </div>

      <!-- Right actions -->
      <div class="flex items-center gap-1.5 sm:gap-2">
        <button
          type="button"
          :class="[
            'hidden sm:flex h-11 w-11 items-center justify-center rounded-full transition-colors',
            overHero
              ? 'text-chocolate hover:bg-chocolate/5 lg:text-cream-50 lg:hover:bg-cream-50/10'
              : 'text-chocolate hover:bg-chocolate/[0.07]',
          ]"
          aria-label="Search cookies"
          @click="goSearch"
        >
          <MagnifyingGlassIcon class="h-[22px] w-[22px]" />
        </button>
        <!-- Cart (the cart feature replaces openCart) -->
        <button
          type="button"
          :class="[
            'relative h-11 w-11 flex items-center justify-center rounded-full transition-all hover:-translate-y-0.5',
            overHero
              ? 'bg-primary text-chocolate hover:bg-primary-300'
              : 'bg-chocolate text-cream-50 hover:bg-chocolate-300',
          ]"
          aria-label="Open cart"
          @click="openCart"
        >
          <ShoppingBagIcon class="h-5 w-5" />
        </button>

        <!-- Hamburger (mobile) -->
        <button
          type="button"
          class="lg:hidden h-11 w-11 flex items-center justify-center rounded-full text-chocolate hover:bg-chocolate/5 transition-colors"
          :aria-expanded="sidebarOpen"
          aria-controls="mobile-menu"
          aria-label="Open menu"
          @click="sidebarOpen = true"
        >
          <Bars3Icon class="h-7 w-7" />
        </button>
      </div>
    </div>

    <!-- Mobile drawer -->
    <Teleport to="#teleports">
      <Transition name="overlay">
        <div
          v-if="sidebarOpen"
          class="fixed inset-0 z-[70] bg-chocolate/50 backdrop-blur-sm lg:hidden"
          @click="closeSidebar"
        />
      </Transition>
      <Transition name="drawer">
        <aside
          v-if="sidebarOpen"
          id="mobile-menu"
          class="fixed top-0 right-0 z-[71] h-full w-[35.2rem] max-w-[88vw] bg-chocolate text-cream-50 flex flex-col lg:hidden"
          aria-label="Menu"
        >
          <div class="flex items-center justify-between px-6 h-[var(--nav-h)]">
            <span class="wordmark pt-1">
              GuiltFree<span class="text-primary">Cookie</span>
            </span>
            <button
              type="button"
              class="h-11 w-11 flex items-center justify-center rounded-full text-cream-50/80 hover:text-cream-50 hover:bg-cream-50/10 transition-colors"
              aria-label="Close menu"
              @click="closeSidebar"
            >
              <XMarkIcon class="h-7 w-7" />
            </button>
          </div>

          <nav class="flex-1 px-6 pt-6 flex flex-col" aria-label="Mobile">
            <RouterLink
              v-for="link in navLinks"
              :key="link.label"
              :to="link.to"
              class="text-base font-bold text-cream-50/85 hover:text-primary transition-colors py-3 border-b border-cream-50/10"
              active-class="!text-primary"
            >
              {{ link.label }}
            </RouterLink>
          </nav>

          <div class="px-6 pb-8 pt-6 space-y-3 border-t border-cream-50/10">
            <button
              type="button"
              class="w-full flex items-center gap-3 rounded-xl px-3 py-3 text-cream-50/85 hover:bg-cream-50/10 transition"
              @click="goSearch"
            >
              <MagnifyingGlassIcon class="h-5 w-5 text-primary" />
              Search cookies
            </button>
            <RouterLink
              :to="{ name: 'Products' }"
              class="btn-secondary btn-block mt-2"
            >
              <ShoppingBagIcon class="h-5 w-5" />
              Shop cookies
            </RouterLink>
          </div>
        </aside>
      </Transition>
    </Teleport>
  </header>
</template>
