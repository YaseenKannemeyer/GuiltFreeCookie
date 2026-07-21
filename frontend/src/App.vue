<template>
  <div id="app">
    <HeaderNav />
    <main>
      <router-view v-slot="{ Component }">
        <transition name="fade" mode="out-in">
          <component :is="Component" />
        </transition>
      </router-view>
    </main>
    <footer class="footer">
      <span class="logo">Guilt<span>Free</span>Cookie</span>
      <p>&copy; 2024 Guilt Free Cookie. All rights reserved.</p>
      <p style="margin-top: 5px; font-size: 0.85rem">
        Healthy cookies for a healthier you
      </p>
    </footer>
  </div>
</template>

<script setup>
import HeaderNav from "./components/HeaderNav.vue";
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
