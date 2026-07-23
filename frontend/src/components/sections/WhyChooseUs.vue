<script setup>
/**
 * WhyChooseUs — four feature cards: Freshly Baked Daily, Premium Ingredients,
 * Fast Delivery, Handmade with Love.
 */
import { useScrollReveal } from "../../composables/useScrollReveal";

const { vReveal } = useScrollReveal();

const features = [
  {
    title: "Freshly Baked Daily",
    description:
      "Our bakers start before sunrise. Every cookie you taste was baked the same day it ships — never frozen, never stale.",
    icon: "🌅",
    accent: "from-amber-200 to-amber-100",
  },
  {
    title: "Premium Ingredients",
    description:
      "Belgian chocolate, French butter, Madagascan vanilla. We source the same ingredients Michelin chefs use, no compromises.",
    icon: "✨",
    accent: "from-primary-200 to-primary-100",
  },
  {
    title: "Fast Delivery",
    description:
      "Carbon-neutral couriers deliver within 24 hours in major cities. Warm-packaged, beautifully boxed, ready to gift.",
    icon: "🚚",
    accent: "from-emerald-200 to-emerald-100",
  },
  {
    title: "Handmade with Love",
    description:
      "No machines, no conveyors. Each cookie is scooped, shaped, and finished by hand — the way cookies were meant to be made.",
    icon: "🤎",
    accent: "from-rose-200 to-rose-100",
  },
];
</script>

<template>
  <section
    class="section bg-chocolate text-cream relative overflow-hidden texture-dark"
  >
    <!-- Decorative -->
    <div
      class="pointer-events-none absolute -top-24 right-10 w-80 h-80 rounded-full bg-primary/10 blur-3xl"
    />
    <div
      class="pointer-events-none absolute -bottom-24 left-10 w-80 h-80 rounded-full bg-primary/10 blur-3xl"
    />

    <div class="container-magic relative">
      <div v-reveal class="text-center max-w-2xl mx-auto mb-16">
        <span
          class="inline-block text-sm font-medium uppercase tracking-[0.2em] text-primary mb-4"
        >
          Why CrumbleMagic
        </span>
        <h2 class="text-cream font-medium mb-4 leading-tight">
          Not All Cookies Are <span class="text-primary">Created Equal</span>
        </h2>
        <p class="text-cream/70 text-card">
          Four reasons our cookies taste like a warm hug in every bite — and why
          our customers keep coming back for more.
        </p>
      </div>

      <div class="grid sm:grid-cols-2 lg:grid-cols-4 gap-6">
        <div
          v-for="(f, i) in features"
          :key="f.title"
          v-reveal
          data-reveal-delay="100"
          class="group relative bg-chocolate-400/40 border border-cream/10 rounded-4xl p-7 transition-all duration-500 ease-cookie hover:bg-chocolate-300/50 hover:-translate-y-1.5 hover:shadow-soft-lg"
        >
          <!-- Gradient halo -->
          <div
            :class="[
              'absolute -top-3 -right-3 w-20 h-20 rounded-full bg-gradient-to-br opacity-0 group-hover:opacity-100 transition-opacity duration-500 blur-xl',
              f.accent,
            ]"
          />

          <div class="relative">
            <!-- Icon -->
            <div
              class="text-5xl mb-5 inline-block transition-transform duration-500 group-hover:scale-110 group-hover:-rotate-6"
            >
              {{ f.icon }}
            </div>
            <h3 class="text-cream text-xl font-semibold mb-3">{{ f.title }}</h3>
            <p class="text-cream/70 text-card leading-relaxed">
              {{ f.description }}
            </p>
          </div>

          <!-- Number watermark -->
          <span
            class="absolute bottom-3 right-5 text-7xl font-bold text-cream/5 select-none"
          >
            0{{ i + 1 }}
          </span>
        </div>
      </div>
    </div>
  </section>
</template>
