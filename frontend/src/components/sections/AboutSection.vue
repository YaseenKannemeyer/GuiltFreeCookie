<script setup>
/**
 * AboutSection — split layout: left = hero cookie on a tan panel with a
 * floating "no added sugar" card, right = "Only the finest" copy + the
 * ingredient checklist. Ingredients mirror the FAQ in data/faqs.js.
 */
import { CheckIcon, ArrowRightIcon } from "@heroicons/vue/20/solid";
import { SparklesIcon } from "@heroicons/vue/24/outline";
import BaseButton from "../../components/ui/BaseButton.vue";
import { useScrollReveal } from "../../composables/useScrollReveal";

const { vReveal } = useScrollReveal();

const ingredients = [
  { name: "Ingredients", note: "Listed for each recipe" },
  { name: "Allergens", note: "Check before ordering" },
  { name: "Pack sizes", note: "Choose how many you would like" },
  { name: "Questions", note: "Ask us about your chosen flavour" },
];
</script>

<template>
  <section class="section bg-background relative overflow-hidden" aria-labelledby="about-title">
    <div class="container-magic grid lg:grid-cols-2 gap-14 lg:gap-20 items-center">
      <!-- Left: cookie panel -->
      <div v-reveal class="relative order-2 lg:order-1">
        <div class="relative isolate aspect-[4/4.4] overflow-hidden rounded-[28px] bg-tan flex items-center justify-center">
          <div
            class="absolute inset-0 -z-10 bg-[radial-gradient(55%_45%_at_50%_45%,rgba(255,255,255,0.35),transparent_70%)]"
            aria-hidden="true"
          />
          <img
            src="/img/cookieHome.webp"
            alt="A swirl cookie with milk and chocolate splashing around it"
            loading="lazy"
            class="w-[82%] -mt-12 drop-shadow-cookie"
          />
        </div>

        <!-- Floating card -->
        <div
          class="absolute bottom-5 left-5 right-5 sm:right-auto sm:max-w-xs rounded-2xl bg-surface p-5 shadow-soft-lg"
        >
          <div class="flex items-center gap-3">
            <span class="h-10 w-10 shrink-0 rounded-full bg-chocolate text-primary flex items-center justify-center">
              <SparklesIcon class="h-5 w-5" />
            </span>
            <div>
              <p class="font-semibold leading-tight">Know what’s inside</p>
              <p class="text-sm text-chocolate/70">Check each recipe</p>
            </div>
          </div>
          <p class="mt-3 text-sm leading-relaxed text-chocolate/80">
            Every ingredient earns its place. You can taste it in every crumble.
          </p>
        </div>
      </div>

      <!-- Right: copy -->
      <div v-reveal class="order-1 lg:order-2" data-reveal-delay="100">
        <h2 id="about-title" class="font-display text-display">
          Only the finest go into
          <span class="text-primary-700">our crumble</span>
        </h2>
        <p class="prose-copy mt-6">
          Pick a flavour you love, then take a look at its ingredients and allergens.
          Every recipe is different. We keep the details with each cookie so you
          can make your choice, and we are here if you need a little more information.
        </p>

        <ul class="mt-9 grid sm:grid-cols-2 gap-x-8 gap-y-6">
          <li v-for="ing in ingredients" :key="ing.name" class="flex items-start gap-3.5 group">
            <span
              class="flex items-center justify-center h-8 w-8 rounded-full bg-primary/20 text-primary-800 shrink-0 mt-0.5 transition-colors group-hover:bg-chocolate group-hover:text-cream-50"
            >
              <CheckIcon class="h-5 w-5" />
            </span>
            <div>
              <p class="font-semibold text-base leading-snug">{{ ing.name }}</p>
              <p class="mt-0.5 text-sm text-chocolate/75">{{ ing.note }}</p>
            </div>
          </li>
        </ul>

        <BaseButton :to="{ name: 'Products' }" variant="outline" size="lg" class="mt-10">
          Shop cookies
          <ArrowRightIcon class="h-5 w-5" />
        </BaseButton>
      </div>
    </div>
  </section>
</template>
