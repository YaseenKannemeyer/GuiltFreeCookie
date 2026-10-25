<script setup>
/**
 * AdminShell — layout for every /admindashboard route.
 * Chocolate sidebar on desktop; compact top bar + tab row on mobile.
 */
import { computed } from "vue";
import { RouterLink, useRouter } from "vue-router";
import {
  Squares2X2Icon,
  UsersIcon,
  ClipboardDocumentListIcon,
  ArrowLeftStartOnRectangleIcon,
  BuildingStorefrontIcon,
  TruckIcon,
  ArchiveBoxIcon,
  InboxIcon,
  StarIcon,
  EnvelopeIcon,
} from "@heroicons/vue/24/outline";
import { useAdminAuth } from "../composables/useAdminAuth";

const router = useRouter();
const { admin, logout } = useAdminAuth();

const navLinks = [
  {
    label: "Orders",
    to: { name: "AdminOrders" },
    icon: ClipboardDocumentListIcon,
  },
  {
    label: "Delivery",
    to: { name: "AdminShipping" },
    icon: TruckIcon,
  },
  {
    label: "Stock & packs",
    to: { name: "AdminInventory" },
    icon: ArchiveBoxIcon,
  },
  {
    label: "Inbox",
    to: { name: "AdminMessages" },
    icon: InboxIcon,
  },
  {
    label: "Reviews",
    to: { name: "AdminReviews" },
    icon: StarIcon,
  },
  {
    label: "Email previews",
    to: { name: "AdminEmailPreviews" },
    icon: EnvelopeIcon,
  },
  { label: "Cookies", to: { name: "AdminDashboard" }, icon: Squares2X2Icon },
  { label: "Users", to: { name: "AdminUsers" }, icon: UsersIcon },
];

const fullName = computed(
  () =>
    [admin.value?.firstName, admin.value?.lastName].filter(Boolean).join(" ") ||
    "Admin",
);
const initials = computed(() =>
  fullName.value
    .split(" ")
    .map((p) => p[0])
    .join("")
    .slice(0, 2)
    .toUpperCase(),
);

const signOut = () => {
  logout();
  router.push({ name: "AdminLogin" });
};
</script>

<template>
  <div
    class="admin min-h-[100dvh] bg-background lg:grid lg:grid-cols-[27.2rem_1fr]"
  >
    <!-- Desktop sidebar -->
    <aside
      class="hidden lg:flex sticky top-0 h-[100dvh] flex-col bg-chocolate text-cream-100 px-4 py-6"
    >
      <RouterLink :to="{ name: 'AdminDashboard' }" class="px-3 block">
        <span class="wordmark text-cream-50">
          GuiltFree<span class="text-primary">Cookie</span>
        </span>
        <span class="mt-1.5 block text-sm font-semibold text-cream-100/60"
          >Admin</span
        >
      </RouterLink>

      <nav class="mt-10 flex flex-col gap-1" aria-label="Admin">
        <RouterLink
          v-for="link in navLinks"
          :key="link.label"
          :to="link.to"
          class="group flex items-center gap-3 rounded-xl px-3 h-11 text-sm font-medium text-cream-100/75 hover:text-cream-50 hover:bg-cream-50/[0.06] transition-colors"
          active-class="!bg-cream-50/10 !text-cream-50"
        >
          <component
            :is="link.icon"
            class="h-5 w-5 text-primary/80 group-[.router-link-active]:text-primary"
          />
          {{ link.label }}
        </RouterLink>
      </nav>

      <div class="mt-auto space-y-1">
        <RouterLink
          :to="{ name: 'Home' }"
          class="flex items-center gap-3 rounded-xl px-3 h-11 text-sm text-cream-100/75 hover:text-cream-50 hover:bg-cream-50/[0.06] transition-colors"
        >
          <BuildingStorefrontIcon class="h-5 w-5 text-primary/80" />
          View store
        </RouterLink>

        <div
          class="mt-3 pt-4 border-t border-cream-50/10 flex items-center gap-2"
        >
          <RouterLink
            :to="{ name: 'AdminProfile' }"
            class="flex-1 min-w-0 flex items-center gap-3 rounded-xl p-2 hover:bg-cream-50/[0.06] transition-colors"
            active-class="bg-cream-50/10"
          >
            <span
              class="h-9 w-9 shrink-0 rounded-full bg-primary text-chocolate text-sm font-bold flex items-center justify-center"
            >
              {{ initials }}
            </span>
            <span class="min-w-0">
              <span
                class="block text-sm font-semibold text-cream-50 truncate"
                >{{ fullName }}</span
              >
              <span class="block text-sm text-cream-100/60 truncate">{{
                admin?.email
              }}</span>
            </span>
          </RouterLink>
          <button
            type="button"
            class="h-11 w-11 shrink-0 flex items-center justify-center rounded-xl text-cream-100/70 hover:text-cream-50 hover:bg-cream-50/[0.06] transition-colors"
            aria-label="Log out"
            title="Log out"
            @click="signOut"
          >
            <ArrowLeftStartOnRectangleIcon class="h-5 w-5" />
          </button>
        </div>
      </div>
    </aside>

    <!-- Mobile top bar -->
    <header class="lg:hidden sticky top-0 z-40 bg-chocolate text-cream-100">
      <div class="flex items-center justify-between gap-2 px-4 h-16">
        <RouterLink
          :to="{ name: 'AdminDashboard' }"
          class="wordmark text-cream-50 pt-1"
        >
          GuiltFree<span class="text-primary">Cookie</span>
        </RouterLink>
        <div class="flex items-center gap-1">
          <RouterLink
            :to="{ name: 'AdminProfile' }"
            class="h-11 w-11 flex items-center justify-center rounded-full"
            aria-label="My profile"
          >
            <span
              class="h-8 w-8 rounded-full bg-primary text-chocolate text-sm font-bold flex items-center justify-center"
            >
              {{ initials }}
            </span>
          </RouterLink>
          <button
            type="button"
            class="h-11 w-11 flex items-center justify-center rounded-xl text-cream-100/80 hover:bg-cream-50/10"
            aria-label="Log out"
            @click="signOut"
          >
            <ArrowLeftStartOnRectangleIcon class="h-5 w-5" />
          </button>
        </div>
      </div>
      <nav
        class="flex gap-1 px-3 pb-2 overflow-x-auto no-scrollbar"
        aria-label="Admin"
      >
        <RouterLink
          v-for="link in navLinks"
          :key="link.label"
          :to="link.to"
          class="flex items-center gap-2 rounded-lg px-3 h-11 text-sm font-medium text-cream-100/75 whitespace-nowrap"
          active-class="!bg-cream-50/10 !text-cream-50"
        >
          <component :is="link.icon" class="h-4 w-4 text-primary" />
          {{ link.label }}
        </RouterLink>
        <RouterLink
          :to="{ name: 'Home' }"
          class="ml-auto flex items-center gap-2 rounded-lg px-3 h-11 text-sm font-medium text-cream-100/75 whitespace-nowrap"
        >
          <BuildingStorefrontIcon class="h-4 w-4 text-primary" />
          Store
        </RouterLink>
      </nav>
    </header>

    <!-- Page -->
    <main class="min-w-0 px-4 sm:px-6 lg:px-10 py-8 lg:py-10">
      <div class="mx-auto max-w-6xl">
        <slot />
      </div>
    </main>
  </div>
</template>
