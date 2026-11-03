<template>
  <!-- Admin area: its own shell, no store chrome -->
  <AdminShell v-if="route.meta.requiresAdmin">
    <RouterView v-slot="{ Component, route: currentRoute }">
      <Transition name="page" mode="out-in">
        <component :is="Component" :key="currentRoute.path" />
      </Transition>
    </RouterView>
  </AdminShell>

  <div v-else class="min-h-[100dvh] flex flex-col bg-background" :class="{ 'demo-mode': DEMO_MODE }">
    <!-- Sentinel the navbar watches to know when the page has scrolled -->
    <div id="top-sentinel" class="absolute top-0 left-0 h-6 w-px" aria-hidden="true" />

    <TheNavbar v-if="!route.meta.hideChrome" />

    <main class="flex-1">
      <RouterView v-slot="{ Component, route: currentRoute }">
        <Transition name="page" mode="out-in">
          <component :is="Component" :key="currentRoute.path" />
        </Transition>
      </RouterView>
    </main>

    <TheFooter v-if="!route.meta.hideChrome && !route.meta.hideFooter" />
  </div>

  <ToastContainer />
</template>

<script setup>
import { defineAsyncComponent, onMounted, onUnmounted } from "vue";
import { RouterView, useRoute } from "vue-router";
import TheNavbar from "./layout/TheNavbar.vue";
import TheFooter from "./layout/TheFooter.vue";
import ToastContainer from "./components/ui/ToastContainer.vue";
import { DEMO_MODE } from "./constants/business";

// Only staff ever see the admin shell, so shoppers never download it.
const AdminShell = defineAsyncComponent(() => import("./layout/AdminShell.vue"));
import { useToast } from "./composables/useToast";

const route = useRoute();
const { showToast } = useToast();

// Tell people when they've lost connection instead of letting requests
// fail silently, and again when it comes back.
const onOffline = () =>
  showToast("You're offline. We'll keep what's on screen until you reconnect.", "error", 6000);
const onOnline = () => showToast("Back online.", "success");
onMounted(() => {
  window.addEventListener("offline", onOffline);
  window.addEventListener("online", onOnline);
});
onUnmounted(() => {
  window.removeEventListener("offline", onOffline);
  window.removeEventListener("online", onOnline);
});
</script>

<style>
#app {
  min-height: 100dvh;
}
.demo-mode { --nav-h: 108px; }
</style>
