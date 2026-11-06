<template>
  <article
    class="group relative flex items-center gap-3 sm:gap-5 overflow-hidden rounded-2xl bg-surface p-4 pr-5 shadow-soft transition-shadow duration-300 hover:shadow-soft-lg"
  >
    <div
      class="h-20 w-20 sm:h-28 sm:w-28 shrink-0 rounded-full flex items-center justify-center"
      :style="{ backgroundColor: tint }"
    >
      <img
        :src="image"
        :alt="name"
        loading="lazy"
        class="h-[88%] w-[88%] object-contain drop-shadow-cookie-sm transition-transform duration-500 ease-cookie group-hover:-rotate-12 group-hover:scale-110"
        @error="onError"
      />
    </div>

    <div class="flex-1 min-w-0">
      <p class="text-sm font-semibold text-primary-700">{{ label }}</p>
      <h3 class="mt-0.5 text-base leading-snug line-clamp-2">
        <!-- Stretched link: the whole card is clickable -->
        <RouterLink :to="to" class="after:absolute after:inset-0 focus-visible:after:outline focus-visible:after:outline-2 focus-visible:after:outline-chocolate">
          {{ name }}
        </RouterLink>
      </h3>
      <p class="mt-1 text-sm text-chocolate/75 line-clamp-1">{{ blurb }}</p>

      <div class="mt-3 flex flex-wrap items-center justify-between gap-2">
        <span class="text-base font-semibold tabular">{{ rand(related.price) }}</span>
        <span
          class="pointer-events-none inline-flex items-center gap-1 rounded-full bg-chocolate px-4 py-2 text-sm font-semibold text-cream-50 transition-colors group-hover:bg-chocolate-300"
          aria-hidden="true"
        >
          View cookie
          <ArrowRightIcon class="h-4 w-4" />
        </span>
      </div>
    </div>
  </article>
</template>

<script setup>
import { computed } from "vue";
import { RouterLink } from "vue-router";
import { ArrowRightIcon } from "@heroicons/vue/20/solid";
import { cookieImage, cookieName, imageFallback } from "../../composables/useCatalog";
import { useFormat } from "../../composables/useFormat";
import { formatCategoryLabel, categoryBlurb, categoryTint } from "../../constants/cookieCategories";

const props = defineProps({
  related: {
    type: Object,
    required: true,
  },
});

const { rand } = useFormat();

const name = computed(() => cookieName(props.related));
const image = computed(() => cookieImage(props.related));
const label = computed(() => formatCategoryLabel(props.related.category));
const blurb = computed(() => categoryBlurb(props.related.category));
const tint = computed(() => categoryTint(props.related.category));
const to = computed(() => ({ name: "ProductDetail", params: { id: props.related.cookieId } }));
const onError = (e) => imageFallback(props.related)(e);
</script>
