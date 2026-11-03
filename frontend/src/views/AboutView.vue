<script setup>
/**
 * AboutView — brand story page.
 * Story hero → ingredients split → why us → who we bake for → CTA.
 */
import { ArrowRightIcon } from "@heroicons/vue/20/solid";
import BaseButton from "../components/ui/BaseButton.vue";
import PageHero from "../components/sections/PageHero.vue";
import AboutSection from "../components/sections/AboutSection.vue";
import WhyChooseUs from "../components/sections/WhyChooseUs.vue";
import MadeForSection from "../components/sections/MadeForSection.vue";
import CtaBanner from "../components/sections/CtaBanner.vue";
import { useScrollReveal } from "../composables/useScrollReveal";

const { vReveal } = useScrollReveal();
</script>

<template>
  <div>
    <PageHero
      image="/img/banners/about-banner.webp"
      :image-width="1400"
      :image-height="788"
      text="GuiltFreeCookie started with one question: why should a cookie come with guilt? So we rebuilt the classics with smarter ingredients and kept baking until nobody could tell the difference."
    >
      Cookies that <span class="sticker">love you back</span>
      <template #actions>
        <BaseButton :to="{ name: 'Products' }" size="lg">
          Shop cookies
          <ArrowRightIcon class="h-5 w-5" />
        </BaseButton>
      </template>
    </PageHero>

    <!-- Story -->
    <section class="section bg-background" aria-labelledby="story-title">
      <div class="container-magic grid lg:grid-cols-[0.9fr_1.1fr] gap-10 lg:gap-20">
        <h2 id="story-title" v-reveal class="font-display text-display lg:sticky lg:top-[calc(var(--nav-h)+3.2rem)] self-start">
          Sweet first.<br />
          <span class="text-primary-700">Smart underneath.</span>
        </h2>
        <div v-reveal data-reveal-delay="100" class="space-y-6">
          <p class="prose-copy !text-base !leading-relaxed !text-chocolate">
            Most "healthy" cookies taste like a compromise. We wanted the
            opposite: a cookie you'd pick for the chocolate, that also happens
            to work for the way you eat.
          </p>
          <p class="prose-copy">
            Every flavour has its own recipe. Check the ingredients, allergens and
            available nutrition on its product page, and ask us if you need more
            information before choosing.
          </p>
          <p class="prose-copy">
            And it means we taste every batch the same way you will. If it isn't
            a cookie we'd reach for twice, it doesn't go in the box.
          </p>
        </div>
      </div>
    </section>

    <AboutSection />
    <WhyChooseUs />
    <MadeForSection />
    <CtaBanner />
  </div>
</template>
