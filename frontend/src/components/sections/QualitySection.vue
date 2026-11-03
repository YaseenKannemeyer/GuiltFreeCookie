<script setup>
// One from each range: high protein, diabetic friendly, gluten free, vegan.
const cookies = [
  { src: "/img/cookies/double-chocolate-chunk.webp", alt: "Double chocolate chunk cookie" },
  { src: "/img/cookies/cinnamon-pecan.webp", alt: "Cinnamon pecan cookie" },
  { src: "/img/cookies/lemon-almond.webp", alt: "Lemon almond cookie" },
  { src: "/img/cookies/raspberry-coconut.webp", alt: "Raspberry coconut cookie" },
];
</script>

<template>
  <section
    id="inside"
    class="bg-[#e3d9c8] min-h-[50vh] py-16 lg:py-20 flex items-center scroll-mt-[var(--nav-h)]"
  >
    <div class="mx-auto w-full max-w-7xl px-6 sm:px-8 lg:px-16">
      <div class="grid lg:grid-cols-2 items-center gap-12">
        <div
          class="order-2 lg:order-1 flex justify-center items-center max-w-lg mx-auto mt-0 lg:mt-0"
        >
          <img
            v-for="(cookie, index) in cookies"
            :key="cookie.src"
            :src="cookie.src"
            :alt="cookie.alt"
            :class="[
              'w-36 lg:w-60 object-contain transition duration-500 hover:scale-110 mb-20 lg:mb-0',
              index !== 0 ? '-ml-20' : '',
            ]"
          />
        </div>

        <div class="mt-20 order-1 lg:order-2 lg:mt-0 lg:ml-20 text-left">
          <h2
            class="font-display text-display"
          >
            Know Your Cookie
            <span class="text-primary-700">Before Your First Bite</span>
          </h2>

          <p
            class="hidden lg:block lg:mt-6 text-chocolate text-base max-w-[62ch]"
          >
            Explore the flavours, then check the ingredients and allergens on each
            product page. Recipes vary, so choose the cookie that works for you.
            If you need more detail about an ingredient, ask us before ordering.
          </p>
        </div>
      </div>
    </div>
  </section>
</template>
