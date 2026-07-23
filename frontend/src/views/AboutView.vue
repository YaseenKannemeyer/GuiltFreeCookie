<script setup>
/**
 * AboutView — brand story page.
 * Hero quote → story split → values grid → stats band → CTA.
 */
import { useScrollReveal } from "../composables/useScrollReveal";
import BaseButton from "../components/ui/BaseButton.vue";
import AboutSection from "../components/sections/AboutSection.vue";
import WhyChooseUs from "../components/sections/WhyChooseUs.vue";
import CtaBanner from "../components/sections/CtaBanner.vue";
import { ArrowRightIcon } from "@heroicons/vue/24/solid";

const { vReveal } = useScrollReveal();

const stats = [
  { value: "2018", label: "Founded in Colombo" },
  { value: "15K+", label: "Cookies baked weekly" },
  { value: "4.9★", label: "Average customer rating" },
  { value: "24hr", label: "Fresh delivery promise" },
];

const values = [
  {
    title: "Slow-crafted, never rushed",
    description:
      "Our dough rests overnight for deeper flavour. We bake in small batches every morning — never frozen, never stale.",
    icon: "⏳",
  },
  {
    title: "Transparent sourcing",
    description:
      "Every ingredient is traceable to its origin. We publish our supplier list yearly because integrity matters.",
    icon: "🌿",
  },
  {
    title: "Packaged with intention",
    description:
      "Plastic-free, compostable packaging. Beautiful enough to gift, sustainable enough to feel good about.",
    icon: "🎁",
  },
];
</script>

<template>
  <div class="pt-24">
    <!-- Hero -->
    <section
      class="section bg-chocolate text-cream relative overflow-hidden texture-dark"
    >
      <div
        class="pointer-events-none absolute -top-32 -right-32 w-96 h-96 rounded-full bg-primary/15 blur-3xl"
      />
      <div
        class="pointer-events-none absolute -bottom-32 -left-32 w-96 h-96 rounded-full bg-primary/15 blur-3xl"
      />

      <div
        class="container-max-w-5xl mx-auto px-6 text-center max-w-4xl relative"
      >
        <div v-reveal>
          <span
            class="inline-block text-sm font-medium uppercase tracking-[0.2em] text-primary mb-5"
          >
            Our Story
          </span>
          <h1
            class="text-cream font-medium leading-tight mb-6 text-4xl lg:text-6xl"
          >
            We bake cookies the way <br />
            <span class="text-primary">grandma used to</span> — only better.
          </h1>
          <p class="text-cream/70 text-body max-w-2xl mx-auto leading-relaxed">
            What started in a tiny Colombo home kitchen in 2018 has grown into a
            cult-favourite bakery serving cookie lovers across the island. Our
            recipe hasn't changed — only the number of ovens has.
          </p>
        </div>
      </div>
    </section>

    <!-- Stats band -->
    <section class="py-14 bg-background border-b border-cream-300">
      <div class="container-magic">
        <div class="grid grid-cols-2 lg:grid-cols-4 gap-8">
          <div
            v-for="(s, i) in stats"
            :key="s.label"
            v-reveal
            :data-reveal-delay="i * 80"
            class="text-center"
          >
            <div class="text-4xl lg:text-5xl font-bold text-primary mb-1">
              {{ s.value }}
            </div>
            <div class="text-chocolate/60 text-card">{{ s.label }}</div>
          </div>
        </div>
      </div>
    </section>

    <!-- Story split -->
    <AboutSection />

    <!-- Values -->
    <section class="section bg-cream-50/60">
      <div class="container-magic">
        <div v-reveal class="text-center max-w-2xl mx-auto mb-14">
          <span class="eyebrow">What We Stand For</span>
          <h2 class="text-chocolate leading-tight">
            Three values we
            <span class="text-primary">never compromise</span> on
          </h2>
        </div>
        <div class="grid md:grid-cols-3 gap-6">
          <div
            v-for="(v, i) in values"
            :key="v.title"
            v-reveal
            :data-reveal-delay="i * 100"
            class="bg-surface rounded-4xl p-8 shadow-soft hover:shadow-soft-lg hover:-translate-y-1.5 transition-all duration-500"
          >
            <div class="text-5xl mb-5">{{ v.icon }}</div>
            <h3 class="text-xl font-semibold text-chocolate mb-3">
              {{ v.title }}
            </h3>
            <p class="text-chocolate/70 text-card leading-relaxed">
              {{ v.description }}
            </p>
          </div>
        </div>
      </div>
    </section>

    <WhyChooseUs />
    <CtaBanner />
  </div>
</template>
