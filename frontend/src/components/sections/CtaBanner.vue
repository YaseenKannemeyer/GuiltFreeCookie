<script setup>
/**
 * CtaBanner — promotional call-to-action banner between sections.
 */
import { ArrowRightIcon } from "@heroicons/vue/24/solid";
import BaseButton from "../../components/ui/BaseButton.vue";
import { useScrollReveal } from "../../composables/useScrollReveal";

const { vReveal } = useScrollReveal();
</script>

<template>
  <section class="py-12 bg-[#e3d9c8]">
    <div class="container-magic">
      <div
        v-reveal
        class="relative overflow-hidden rounded-5xl bg-cookie-gradient p-10 lg:p-16 text-center shadow-soft-lg"
      >
        <!-- Decorative -->
        <div
          class="pointer-events-none absolute -top-10 -right-10 w-60 h-60 rounded-full bg-[#d0ab7b] blur-3xl"
        />
        <div
          class="pointer-events-none absolute -bottom-10 -left-10 w-60 h-60 rounded-full bg-[#d0ab7b] blur-3xl"
        />

        <div class="relative">
          <span class="text-5xl mb-4 inline-block">🍪</span>
          <h2
            class="text-chocolate font-medium text-3xl lg:text-4xl mb-3 leading-tight"
          >
            Your next favourite cookie is one click away
          </h2>
          <p class="text-chocolate/80 text-card max-w-xl mx-auto mb-7">
            Browse the full menu, build your dream box, and have warm,
            freshly-baked cookies delivered to your door within 24 hours.
          </p>
          <BaseButton :to="{ name: 'Products' }" variant="primary" size="lg">
            Build Your Cookie Box
            <ArrowRightIcon class="h-5 w-5" />
          </BaseButton>
        </div>
      </div>
    </div>
  </section>
</template>
