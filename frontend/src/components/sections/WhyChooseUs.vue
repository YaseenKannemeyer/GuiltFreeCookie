<script setup>
/**
 * WhyChooseUs — four reasons on the chocolate block. The first reason gets
 * the tall tile with a cookie; the other three stack beside it.
 */
import { FireIcon, HeartIcon, UserGroupIcon, ClipboardDocumentCheckIcon } from "@heroicons/vue/24/outline";
import { useScrollReveal } from "../../composables/useScrollReveal";

const { vReveal } = useScrollReveal();

const features = [
  {
    title: "Find your flavour",
    description:
      "Explore chocolate, nutty and fruity flavours, with ingredients listed for each cookie.",
    icon: HeartIcon,
  },
  {
    title: "Baked in small batches",
    description: "Mixed and baked in Cape Town a tray at a time, so every box tastes fresh out of the oven.",
    icon: FireIcon,
  },
  {
    title: "Made for real diets",
    description: "Diabetic friendly, high protein, gluten free and vegan ranges, each one clearly labelled.",
    icon: UserGroupIcon,
  },
  {
    title: "Clear about what's inside",
    description: "Every cookie lists its ingredients and allergens up front. No small print, no guessing.",
    icon: ClipboardDocumentCheckIcon,
  },
];
</script>

<template>
  <section class="section bg-chocolate text-cream-100 relative overflow-hidden" aria-labelledby="why-title">
    <div class="container-magic relative">
      <div v-reveal class="max-w-2xl mb-14">
        <h2 id="why-title" class="font-display text-cream-50 text-display">
          Not all cookies are <span class="text-primary">created equal</span>
        </h2>
        <p class="mt-5 text-base leading-relaxed text-cream-100/75 max-w-xl">
          Four reasons people who read labels keep coming back, and why
          people who don't never notice the difference.
        </p>
      </div>

      <div class="grid gap-4 md:grid-cols-2 lg:grid-cols-[1.15fr_1fr_1fr] lg:grid-rows-2">
        <!-- Lead reason: tall tile with a cookie -->
        <article
          v-reveal
          class="group relative overflow-hidden rounded-[28px] bg-primary p-8 text-chocolate md:col-span-2 lg:col-span-1 lg:row-span-2 min-h-[35.2rem] pb-44 lg:pb-8 flex flex-col"
        >
          <component :is="features[0].icon" class="h-8 w-8" />
          <h3 class="mt-5 text-base leading-tight max-w-[14ch]">{{ features[0].title }}</h3>
          <p class="mt-3 text-base leading-relaxed text-chocolate/85 max-w-sm">{{ features[0].description }}</p>
          <img
            src="/img/cookie4.webp"
            alt=""
            loading="lazy"
            class="pointer-events-none absolute -right-10 -bottom-12 w-64 rotate-12 drop-shadow-cookie transition-transform duration-700 ease-cookie group-hover:rotate-0 group-hover:scale-105"
          />
        </article>

        <article
          v-for="(f, i) in features.slice(1)"
          :key="f.title"
          v-reveal
          :data-reveal-delay="(i + 1) * 90"
          :class="[
            'rounded-[28px] border border-cream-50/10 bg-cream-50/[0.05] p-7 transition-colors duration-300 hover:bg-cream-50/[0.09]',
            i === 2 ? 'md:col-span-2' : '',
          ]"
        >
          <span class="h-11 w-11 rounded-full bg-cream-50/10 text-primary flex items-center justify-center">
            <component :is="f.icon" class="h-6 w-6" />
          </span>
          <h3 class="mt-5 text-base text-cream-50">{{ f.title }}</h3>
          <p class="mt-2 text-base leading-relaxed text-cream-100/75">{{ f.description }}</p>
        </article>
      </div>
    </div>
  </section>
</template>
