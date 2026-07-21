<template>
  <div class="min-h-screen flex flex-col bg-background">
    <TheNavbar />

    <main class="flex-1">
      <RouterView v-slot="{ Component, route }">
        <Transition name="page" mode="out-in">
          <component :is="Component" :key="route.fullPath" />
        </Transition>
      </RouterView>
    </main>

    <TheFooter />
  </div>
</template>

<script setup>
import { onMounted } from "vue";
import { RouterView } from "vue-router";
import TheNavbar from "./layout/TheNavbar.vue";
import TheFooter from "./layout/TheFooter.vue";
</script>

<style>
#app {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

main {
  flex: 1;
}
</style>
