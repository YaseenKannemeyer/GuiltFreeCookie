<script setup>
/**
 * ContactView — standalone Contact page.
 * Reuses the ContactSection (split form + info) and FaqSection for richness.
 */
import ContactSection from "../components/sections/ContactSection.vue";
import FaqSection from "../components/sections/FaqSection.vue";
</script>

<template>
  <div class="pt-24">
    <ContactSection />
    <FaqSection />
  </div>
</template>
