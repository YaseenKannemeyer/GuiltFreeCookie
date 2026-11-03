<script setup>
/**
 * CookieLineup — eight flavours, two from each range. With `in-stage` it is
 * the final frame of the hero's cookie drop: the cookies fly in from the
 * splash and the labels settle (driven by useCookieDropScroll). Otherwise
 * (mobile, reduced motion) it is an ordinary section under the hero.
 */
import { computed, onMounted } from "vue";
import { RouterLink } from "vue-router";
import { ArrowRightIcon } from "@heroicons/vue/20/solid";
import BaseButton from "../ui/BaseButton.vue";
import { useCatalog, cookieName } from "../../composables/useCatalog";
import { useScrollReveal } from "../../composables/useScrollReveal";
import { formatCategoryLabel } from "../../constants/cookieCategories";
import { COOKIE_LINEUP } from "../../constants/cookieDrop";

const props = defineProps({
  inStage: { type: Boolean, default: false },
});

const { cookies: catalog, load } = useCatalog();
const { vReveal } = useScrollReveal();
// In the stage the scroll timeline reveals everything instead.
const vLineupReveal = props.inStage ? {} : vReveal;

onMounted(load);

// Each cookie links to its product once the catalogue has it, else to its range.
const cookies = computed(() => {
  const byName = new Map(catalog.value.map((c) => [cookieName(c).toLowerCase(), c]));
  return COOKIE_LINEUP.map((cookie) => {
    const product = byName.get(cookie.name.toLowerCase());
    return {
      ...cookie,
      range: formatCategoryLabel(cookie.category),
      to: product
        ? { name: "ProductDetail", params: { id: product.cookieId } }
        : { name: "Products", query: { category: cookie.category } },
    };
  });
});
</script>

<template>
  <component
    :is="inStage ? 'div' : 'section'"
    data-drop="lineup"
    :class="inStage ? 'lineup lineup--stage' : 'lineup lineup--flow bg-tan section'"
    aria-labelledby="lineup-title"
  >
    <div :class="inStage ? 'w-full' : 'container-magic'">
      <div class="flex flex-wrap items-end justify-between gap-x-8 gap-y-4">
        <h2 id="lineup-title" v-lineup-reveal data-drop="lineup-heading" class="font-display text-display">
          Find your <span class="sticker">favourite</span>
        </h2>
        <div v-lineup-reveal data-drop="lineup-cta">
          <BaseButton :to="{ name: 'Products' }" variant="primary">
            Shop cookies
            <ArrowRightIcon class="h-5 w-5" />
          </BaseButton>
        </div>
      </div>

      <ul class="lineup-grid" role="list">
        <li
          v-for="(cookie, i) in cookies"
          :key="cookie.slug"
          v-lineup-reveal
          :data-reveal-delay="(i % 2) * 120"
        >
          <RouterLink :to="cookie.to" class="lineup-link group">
            <div data-drop="slot" class="lineup-slot">
              <div data-drop="flier" class="lineup-flier">
                <div data-drop="lift" class="h-full w-full">
                  <img
                    :src="cookie.image"
                    alt=""
                    width="815"
                    height="900"
                    :loading="inStage ? 'eager' : 'lazy'"
                    decoding="async"
                    class="lineup-img"
                  />
                </div>
              </div>
            </div>
            <div data-drop="label" class="mt-3 text-center">
              <p class="font-bold leading-snug text-balance group-hover:underline">{{ cookie.name }}</p>
              <p class="mt-1 text-sm font-semibold text-chocolate/80">{{ cookie.range }}</p>
            </div>
          </RouterLink>
        </li>
      </ul>
    </div>
  </component>
</template>

<style scoped>
.lineup-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 3.2rem 1.6rem;
  margin-top: 3.2rem;
}
@media (min-width: 1024px) {
  .lineup-grid {
    grid-template-columns: repeat(4, minmax(0, 1fr));
    gap: 2.4rem;
  }
}

.lineup-link {
  display: block;
  border-radius: 1.6rem;
}

.lineup-slot {
  aspect-ratio: 815 / 900;
  width: 100%;
  max-width: 26rem;
  margin-inline: auto;
}
.lineup-flier {
  width: 100%;
  height: 100%;
}
.lineup-img {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: contain;
  filter: drop-shadow(0 14px 18px rgba(46, 14, 11, 0.25));
  transition: transform 0.4s cubic-bezier(0.16, 1, 0.3, 1);
}
.lineup-link:hover .lineup-img {
  transform: translateY(-6px) rotate(-3deg);
}

/* Final frame of the hero stage: covers the tan 80% only, left-aligned with
   the hero copy (container-magic: 128rem wide, 4.8rem gutters at lg), and
   sized so heading + two rows fit under the navbar. */
.lineup--stage {
  position: absolute;
  inset: 0 20% 0 0;
  z-index: 20;
  display: flex;
  align-items: center;
  padding: calc(var(--nav-h) + 2.4rem) 4rem 2.4rem max(4.8rem, calc((100% - 128rem) / 2 + 4.8rem));
  pointer-events: none;
}
/* Promote only the animated version, not all eight static mobile images. */
.lineup--stage .lineup-flier {
  will-change: transform, opacity;
}
/* Set by the timeline once the cookies have landed. */
.lineup--stage.is-settled {
  pointer-events: auto;
}
.lineup--stage > div {
  height: 100%;
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
}
.lineup--stage .lineup-grid {
  min-height: 0;
  grid-template-rows: repeat(2, minmax(0, 1fr));
  margin-top: 2.4rem;
}
.lineup--stage .lineup-link {
  height: 100%;
  display: grid;
  grid-template-rows: minmax(0, 1fr) 8.8rem;
}
.lineup--stage .lineup-slot {
  min-height: 0;
  height: 100%;
  max-width: 26rem;
  aspect-ratio: auto;
}
@media (max-width: 1279px), (max-height: 740px) {
  .lineup--stage [data-drop="lineup-heading"] {
    font-size: 3.6rem;
  }
  .lineup--stage .lineup-grid {
    gap: 1.6rem;
  }
}
/* Hidden until the timeline brings them in (it sets visibility inline). */
.lineup--stage [data-drop="lineup-heading"],
.lineup--stage [data-drop="lineup-cta"],
.lineup--stage [data-drop="flier"],
.lineup--stage [data-drop="label"] {
  visibility: hidden;
}
</style>
