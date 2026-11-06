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
import { ref, computed, watch, nextTick, onMounted, onUnmounted } from "vue";
import { RouterLink, useRouter, useRoute } from "vue-router";
import { cookieDropPinned } from "../composables/cookieDropState";
import { DEMO_MODE, STAGING } from "../constants/business";
import { useCart } from "../composables/useCart";
import {
  Bars3Icon,
  XMarkIcon,
  ShoppingBagIcon,
  MagnifyingGlassIcon,
} from "@heroicons/vue/24/outline";

const router = useRouter();
const route = useRoute();

const sidebarOpen = ref(false);
const menuButton = ref(null);
const drawer = ref(null);
const scrolled = ref(false);
let previousOverflow = "";
let desktopQuery = null;

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
onUnmounted(() => {
  observer?.disconnect();
  desktopQuery?.removeEventListener("change", onDesktopChange);
  if (sidebarOpen.value) releasePage();
});

const closeSidebar = () => {
  sidebarOpen.value = false;
};

function releasePage() {
  document.body.style.overflow = previousOverflow;
  document.getElementById("app")?.removeAttribute("inert");
}
function onMenuKey(event) {
  if (event.key === "Escape") closeSidebar();
  if (event.key !== "Tab") return;
  const controls = drawer.value?.querySelectorAll("a[href], button:not([disabled])");
  if (!controls?.length) return;
  const first = controls[0], last = controls[controls.length - 1];
  if (event.shiftKey && document.activeElement === first) {
    event.preventDefault();
    last.focus();
  } else if (!event.shiftKey && document.activeElement === last) {
    event.preventDefault();
    first.focus();
  }
}
const onDesktopChange = (event) => {
  if (event.matches) closeSidebar();
};
onMounted(() => {
  desktopQuery = window.matchMedia("(min-width: 1024px)");
  desktopQuery.addEventListener("change", onDesktopChange);
});
watch(sidebarOpen, async (open) => {
  if (open) {
    previousOverflow = document.body.style.overflow;
    document.body.style.overflow = "hidden";
    document.getElementById("app")?.setAttribute("inert", "");
    await nextTick();
    if (sidebarOpen.value) drawer.value?.querySelector("button")?.focus();
  } else {
    releasePage();
    menuButton.value?.focus();
  }
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
  closeSidebar();
  router.push({ name: "Products", query: { focus: "search" } });
};

const { itemCount, ready: cartReady, load: loadCart } = useCart();
onMounted(() => {
  if (!cartReady.value) loadCart().catch(() => {});
});
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
    <p v-if="DEMO_MODE || STAGING" role="note" class="site-notice h-[32px] flex items-center justify-center bg-chocolate text-cream-50 text-sm px-3">
      <template v-if="STAGING">Staging · </template>{{ DEMO_MODE ? "Demo shop · Sample products · No real payments" : "Not the live shop" }}
    </p>
    <div
      class="nav-bar container-magic flex h-[76px] items-center justify-between gap-6"
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
      <div class="nav-actions flex shrink-0 items-center gap-1.5 sm:gap-2">
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
        <RouterLink
          :to="{ name: 'Cart' }"
          :class="[
            'relative h-11 w-11 flex items-center justify-center rounded-full transition-all hover:-translate-y-0.5',
            overHero
              ? 'bg-primary text-chocolate hover:bg-primary-300'
              : 'bg-chocolate text-cream-50 hover:bg-chocolate-300',
          ]"
          :aria-label="itemCount ? `Cart, ${itemCount} ${itemCount === 1 ? 'item' : 'items'}` : 'Cart'"
        >
          <ShoppingBagIcon class="h-5 w-5" />
          <span
            v-if="itemCount"
            class="absolute -top-1 -right-1 min-w-[2.2rem] h-[2.2rem] px-1 rounded-full bg-primary text-chocolate text-[1.2rem] leading-[2.2rem] font-extrabold text-center tabular ring-2 ring-background"
            aria-hidden="true"
            >{{ itemCount > 99 ? "99+" : itemCount }}</span
          >
        </RouterLink>

        <!-- Hamburger (mobile) -->
        <button
          ref="menuButton"
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
          ref="drawer"
          id="mobile-menu"
          class="mobile-drawer fixed top-0 right-0 z-[71] h-[100dvh] w-[35.2rem] max-w-[92vw] overflow-y-auto overscroll-contain bg-chocolate text-cream-50 flex flex-col lg:hidden"
          role="dialog"
          aria-modal="true"
          aria-label="Menu"
          @keydown="onMenuKey"
        >
          <div class="flex shrink-0 items-center justify-between gap-2 px-6 min-h-[76px]">
            <span class="wordmark pt-1">
              GuiltFree<span class="text-primary">Cookie</span>
            </span>
            <button
              type="button"
              class="h-11 w-11 shrink-0 flex items-center justify-center rounded-full text-cream-50/80 hover:text-cream-50 hover:bg-cream-50/10 transition-colors"
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
              class="min-h-11 text-base font-bold text-cream-50/85 hover:text-primary transition-colors py-3 border-b border-cream-50/10"
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

<style scoped>
.mobile-drawer {
  padding-top: env(safe-area-inset-top, 0px);
  padding-bottom: env(safe-area-inset-bottom, 0px);
}
.mobile-drawer :focus-visible {
  outline-color: #c59f71;
}
@media (max-width: 639px) {
  .site-notice {
    font-size: 1.2rem;
    text-align: center;
    line-height: 1.3;
  }
  .nav-bar {
    gap: 0.8rem;
  }
  .wordmark {
    font-size: clamp(2.2rem, 6.1vw, 2.8rem);
  }
  .nav-actions {
    gap: 0;
  }
  .mobile-drawer {
    --fs-base: 1.6rem;
    font-size: var(--fs-base);
  }
}
</style>
