<script setup>
/**
 * FaqSection — homepage FAQ accordion section.
 * Uses AccordionList with the faqs mock data.
 */
import AccordionList from "../../components/ui/AccordionList.vue";
import { faqs } from "../../data/cookies";
import { useScrollReveal } from "../../composables/useScrollReveal";

const { vReveal } = useScrollReveal();
</script>

<template>
  <section class="section bg-background relative overflow-hidden">
    <div class="container-magic max-w-4xl">
      <div v-reveal class="text-center mb-14">
        <span class="eyebrow">Got Questions?</span>
        <h2 class="text-chocolate leading-tight">
          Frequently Asked <span class="text-primary">Questions</span>
        </h2>
        <p class="text-chocolate/70 text-card max-w-2xl mx-auto mt-4">
          Everything you need to know about our cookies, ingredients, delivery,
          and bulk orders. Still curious? Reach out via our contact form.
        </p>
      </div>

      <div v-reveal data-reveal-delay="100">
        <AccordionList :items="faqs" />
      </div>
    </div>
  </section>
</template>
