<script setup>
const cookies = ['/cookie2.png', '/cookie.png', '/cookie3.png', '/cookie4.png']
</script>

<template>
  <section class="bg-[#e3d9c8] h-[50vh] min-h-[420px] flex items-center">
    <div class="mx-auto w-full max-w-7xl px-6 sm:px-8 lg:px-16">
      <div class="grid lg:grid-cols-2 items-center gap-12">
        <!-- Images -->
        <div
          class="order-2 lg:order-1 flex justify-center items-center max-w-lg mx-auto mt-0 lg:mt-0"
        >
          <img
            v-for="(cookie, index) in cookies"
            :key="index"
            :src="cookie"
            :alt="`Cookie ${index + 1}`"
            :class="[
              'w-36 lg:w-60 object-contain transition duration-500 hover:scale-110',
              index !== 0 ? '-ml-20' : '',
            ]"
          />
        </div>

        <!-- Text -->
        <div class="mt-20 order-1 lg:order-2 lg:mt-0 lg:ml-20 text-center lg:text-right">
          <h2 class="text-[#35150D] font-semibold leading-tight text-3xl sm:text-4xl lg:text-5xl">
            Only the Finest
            <br />
            Go Into Our Crumble
          </h2>

          <p class="mb-[-25px] mt-6 text-[#4F342B] text-base sm:text-lg leading-7">
            With real butter, rich chocolate, and hand-selected flavors, we focus on quality from
            the very first step. Every batch is baked fresh using premium ingredients that create
            the signature crumble and deep, satisfying taste you'll love.
          </p>
        </div>
      </div>
    </div>
  </section>
</template>
