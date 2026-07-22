<script setup>
/**
 * TheNavbar — sticky top navigation.
 *
 * - Sticky with backdrop blur, transparent on hero, solid after scroll.
 * - Desktop: logo + nav links + cart icon + count badge.
 * - Mobile: hamburger that toggles a slide-in sidebar.
 */
import { ref, computed, onMounted, onUnmounted } from "vue";
import { RouterLink, useRouter, useRoute } from "vue-router";
import {
  Bars3Icon,
  XMarkIcon,
  ShoppingBagIcon,
  UserIcon,
  MagnifyingGlassIcon,
  HomeIcon,
  CubeIcon,
  InformationCircleIcon,
  PhoneIcon,
} from "@heroicons/vue/24/outline";

const sidebarOpen = ref(false);

const isHomePage = computed(() => route.name === "Home");

const toggleSidebar = () => {
  sidebarOpen.value = !sidebarOpen.value;
};

const closeSidebar = () => {
  sidebarOpen.value = false;
};
const router = useRouter();
const route = useRoute();
const scrolled = ref(false);

const onScroll = () => {
  scrolled.value = window.scrollY > 30;
};

onMounted(() => window.addEventListener("scroll", onScroll, { passive: true }));
onUnmounted(() => window.removeEventListener("scroll", onScroll));

const navLinks = [
  {
    label: "Home",
    to: { name: "Home" },
    icon: HomeIcon,
  },
  {
    label: "Products",
    to: { name: "Products" },
    icon: CubeIcon,
  },
  {
    label: "About",
    to: { name: "About" },
    icon: InformationCircleIcon,
  },
  {
    label: "Contact",
    to: { name: "Contact" },
    icon: PhoneIcon,
  },
];

const onCartClick = () => {
  router.push({ name: "Products" });
};
</script>

<template>
  <header
    :class="[
      'fixed top-0 left-0 right-0 z-50 transition-all duration-500 ease-cookie',
      isHomePage && !scrolled
        ? 'bg-transparent py-5'
        : 'bg-background/85 backdrop-blur-md shadow-soft py-3',
    ]"
  >
    <div class="container-magic flex items-center justify-between">
      <!-- Logo -->
      <RouterLink
        :to="{ name: 'Home' }"
        class="flex items-center gap-2 group"
        @click="closeSidebar"
      >
        <span class="text-2xl">🍪</span>
        <span
          :class="[
            'font-bold text-2xl tracking-tight transition-colors',
            isHomePage && !scrolled ? 'text-chocolate' : 'text-chocolate',
          ]"
        >
          GuiltFree<span
            :class="[
              'transition-colors duration-300',
              isHomePage && !scrolled ? 'text-white' : 'text-primary',
            ]"
            >Cookie
          </span>
        </span>
      </RouterLink>

      <!-- Desktop nav -->
      <nav class="hidden lg:flex items-center gap-10">
        <RouterLink
          v-for="link in navLinks"
          :key="link.label"
          :to="link.to"
          class="nav-link text-card font-medium"
          active-class="router-link-active"
        >
          {{ link.label }}
        </RouterLink>
      </nav>

      <!-- Right actions -->
      <div class="flex items-center gap-3">
        <button
          type="button"
          :class="[
            'hidden sm:flex h-11 w-11 items-center justify-center rounded-full transition-all',
            isHomePage && !scrolled
              ? 'text-white hover:bg-white/10'
              : 'text-chocolate hover:bg-cream-100',
          ]"
          aria-label="Search"
          @click="router.push({ name: 'Products' })"
        >
          <MagnifyingGlassIcon class="h-5 w-5" />
        </button>
        <button
          type="button"
          :class="[
            'hidden sm:flex h-11 w-11 items-center justify-center rounded-full transition-all',
            isHomePage && !scrolled
              ? 'text-white hover:bg-white/10'
              : 'text-chocolate hover:bg-cream-100',
          ]"
          aria-label="Account"
        >
          <UserIcon class="h-5 w-5" />
        </button>

        <!-- Cart with badge -->
        <button
          type="button"
          class="relative h-11 w-11 flex items-center justify-center rounded-full bg-chocolate text-cream hover:bg-chocolate-600 hover:shadow-soft transition-all hover:-translate-y-0.5"
          aria-label="Open cart"
        >
          <ShoppingBagIcon class="h-5 w-5" />
        </button>

        <!-- Hamburger (mobile) -->
        <button
          type="button"
          :class="[
            'lg:hidden h-11 w-11 flex items-center justify-center rounded-full transition-all',
            isHomePage && !scrolled ? 'text-chocolate' : 'text-chocolate',
          ]"
          aria-label="Open menu"
          @click="toggleSidebar"
        >
          <Bars3Icon v-if="!sidebarOpen" class="h-6 w-6" />
          <XMarkIcon v-else class="h-6 w-6" />
        </button>
      </div>
    </div>

    <!-- Mobile sidebar -->
    <Teleport to="body">
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
          class="fixed top-0 right-0 z-[71] h-full w-80 max-w-[85vw] bg-background shadow-soft-xl flex flex-col lg:hidden"
        >
          <div
            class="flex items-center justify-between px-6 py-5 border-b border-cream-300"
          >
            <span class="font-bold text-2xl text-chocolate">
              GuiltFree<span class="text-primary">Cookie</span>
            </span>
            <button
              type="button"
              class="p-2 rounded-full text-chocolate/60 hover:text-chocolate hover:bg-cream-100 transition-colors"
              aria-label="Close menu"
              @click="closeSidebar"
            >
              <XMarkIcon class="h-6 w-6" />
            </button>
          </div>
          <nav class="flex-1 px-6 py-8 flex flex-col gap-2">
            <RouterLink
              v-for="link in navLinks"
              :key="link.label"
              :to="link.to"
              class="flex items-center gap-4 px-4 py-4 rounded-2xl text-lg font-medium text-chocolate hover:bg-cream-100 transition-all"
              active-class="bg-primary/15 text-chocolate"
              @click="closeSidebar"
            >
              <component :is="link.icon" class="h-6 w-6 text-primary" />

              <span>
                {{ link.label }}
              </span>
            </RouterLink>
          </nav>
          <div class="px-6 py-6 border-t border-cream-300 space-y-3">
            <button
              class="w-full flex items-center gap-3 rounded-xl px-4 py-3 hover:bg-cream-100 transition"
            >
              <MagnifyingGlassIcon class="h-5 w-5 text-primary" />
              <span>Search</span>
            </button>

            <button
              class="w-full flex items-center gap-3 rounded-xl px-4 py-3 hover:bg-cream-100 transition"
            >
              <UserIcon class="h-5 w-5 text-primary" />
              <span>My Account</span>
            </button>

            <RouterLink
              :to="{ name: 'Products' }"
              class="btn-primary btn-block flex items-center justify-center gap-2"
              @click="closeSidebar"
            >
              <ShoppingBagIcon class="h-5 w-5" />
              Order Now
            </RouterLink>
          </div>
        </aside>
      </Transition>
    </Teleport>
  </header>
</template>
