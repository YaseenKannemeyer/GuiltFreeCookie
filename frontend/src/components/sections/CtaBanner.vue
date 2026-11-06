<script setup>
/**
 * CtaBanner — closing call to action. Tan card with the products banner
 * assortment bursting out of its top edge (right of the copy on desktop,
 * above it on mobile).
 */
import { ArrowRightIcon } from "@heroicons/vue/20/solid";
import BaseButton from "../../components/ui/BaseButton.vue";
import { useScrollReveal } from "../../composables/useScrollReveal";

const { vReveal } = useScrollReveal();
</script>

<template>
  <section class="bg-background pt-20 pb-24 md:pt-24 md:pb-32">
    <div class="container-magic">
      <!-- No overflow clipping: the cookies are meant to break out of the
           card. Sizes are in 10px rem. -->
      <div
        v-reveal
        class="cta-panel relative rounded-[28px] bg-tan px-6 pt-[14rem] pb-14 sm:px-12 sm:pt-[17rem] lg:py-20 lg:px-20"
      >
        <!-- Widths keep it clear of the copy column at every breakpoint -->
        <img
          src="/img/banners/products-banner.webp"
          alt=""
          width="1400"
          height="861"
          loading="lazy"
          decoding="async"
          class="pointer-events-none absolute z-0 w-[30rem] -top-[7rem] right-4 sm:w-[38rem] sm:-top-[8rem] sm:right-8 lg:w-[40%] lg:-top-[6.4rem] lg:right-[2.4rem] xl:w-[58rem] xl:right-[4.8rem] drop-shadow-cookie"
        />

        <div class="relative z-10 lg:max-w-[44rem] xl:max-w-[48rem]">
          <h2 class="font-display text-display">
            Your next favourite is
            <span class="sticker">one click away</span>
          </h2>
          <p class="mt-5 text-base text-chocolate/85 max-w-md">
            Pick your range, build a box, and get them baked fresh.
          </p>
          <BaseButton :to="{ name: 'Products' }" variant="primary" size="lg" class="mt-8">
            Shop cookies
            <ArrowRightIcon class="h-5 w-5" />
          </BaseButton>
        </div>
      </div>
    </div>
  </section>
</template>

<style scoped>
@media (max-width: 639px) {
  .cta-panel > img {
    width: min(30rem, 100%);
    right: 0;
  }
}
</style>
