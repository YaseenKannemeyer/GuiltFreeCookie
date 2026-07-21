<script setup>
/**
 * AboutSection — split layout: left = cookie splash illustration,
 * right = "Only the Finest Go Into Our Crumble" copy + ingredient list.
 */
import { CheckIcon, ArrowRightIcon } from "@heroicons/vue/24/solid";
import BaseButton from "../../components/ui/BaseButton.vue";
import { useScrollReveal } from "../../composables/useScrollReveal";

const { vReveal } = useScrollReveal();

const aboutImage =
  "https://images.unsplash.com/photo-1606312619070-d48b4c652a52?w=900&q=85&auto=format&fit=crop";

const ingredients = [
  { name: "Belgian Chocolate", note: "70% Valrhona, stone-ground" },
  { name: "French Butter", note: "Cultured, churned in Normandy" },
  { name: "Madagascan Vanilla", note: "Hand-podded, 18% vanillin" },
  { name: "Flaked Sea Salt", note: "Maldon, hand-harvested" },
];
</script>

<template>
  <section class="section bg-cream-50/60 relative overflow-hidden">
    <div class="container-magic">
      <div class="grid lg:grid-cols-2 gap-12 lg:gap-20 items-center">
        <!-- Left: image splash -->
        <div v-reveal class="relative order-2 lg:order-1">
          <div
            class="absolute inset-0 -m-6 rounded-[3rem] bg-primary/15 blur-2xl"
          />
          <div class="relative rounded-[2.5rem] overflow-hidden shadow-soft-lg">
            <img
              :src="aboutImage"
              alt="A stack of freshly baked CrumbleMagic cookies"
              class="w-full h-[28rem] lg:h-[36rem] object-cover"
            />
            <!-- Floating ingredient pill -->
            <div
              class="absolute bottom-6 left-6 right-6 lg:left-8 lg:right-auto lg:max-w-xs bg-surface/95 backdrop-blur rounded-3xl p-5 shadow-soft-lg"
            >
              <div class="flex items-center gap-3 mb-2">
                <span
                  class="flex items-center justify-center h-10 w-10 rounded-full bg-primary/15 text-primary-700 text-xl"
                  >🌿</span
                >
                <div>
                  <div class="font-semibold text-chocolate">100% Natural</div>
                  <div class="text-xs text-chocolate/60">
                    No preservatives, ever
                  </div>
                </div>
              </div>
              <p class="text-sm text-chocolate/70 leading-relaxed">
                Every ingredient is traceable to a single trusted farm or
                producer. Quality you can taste in every crumble.
              </p>
            </div>
          </div>
        </div>

        <!-- Right: copy -->
        <div v-reveal class="order-1 lg:order-2" data-reveal-delay="100">
          <span class="eyebrow">Our Promise</span>
          <h2 class="text-chocolate mb-6 leading-tight">
            Only the Finest Go Into <br />
            <span class="text-primary">Our Crumble</span>
          </h2>
          <p class="text-chocolate/70 text-body mb-8 leading-relaxed">
            We don't cut corners, and we don't compromise. Each cookie begins
            with a slow-creamed brown butter base, folded with single-origin
            chocolate, fresh eggs, and a whisper of flaked sea salt. Nothing
            artificial, nothing fake — just honest, exceptional cookies that
            taste like they were made by someone who loves you.
          </p>

          <ul class="space-y-4 mb-9">
            <li
              v-for="ing in ingredients"
              :key="ing.name"
              class="flex items-start gap-4 group"
            >
              <span
                class="flex items-center justify-center h-8 w-8 rounded-full bg-primary/15 text-primary-700 shrink-0 mt-0.5 transition-colors group-hover:bg-primary group-hover:text-cream"
              >
                <CheckIcon class="h-5 w-5" />
              </span>
              <div>
                <div class="font-semibold text-chocolate text-lg">
                  {{ ing.name }}
                </div>
                <div class="text-sm text-chocolate/60">{{ ing.note }}</div>
              </div>
            </li>
          </ul>

          <BaseButton :to="{ name: 'about' }" variant="outline" size="lg">
            Read our story
            <ArrowRightIcon class="h-5 w-5" />
          </BaseButton>
        </div>
      </div>
    </div>
  </section>
</template>
