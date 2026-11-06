<script setup>
/**
 * HeroBanner — tan / chocolate colour block with the hero cookie
 * straddling the split. Desktop splits 80 / 20 left/right, mobile top/bottom.
 *
 * Desktop with motion allowed: scrolling pins the hero and plays the cookie
 * drop. The cookie falls into a glass of milk and eight flavours fan out of
 * the splash into a lineup (useCookieDropScroll). Everywhere else the hero
 * stays still and the same lineup follows it as a normal section.
 */
import { nextTick, onBeforeUnmount, onMounted, ref } from "vue";
import { ArrowRightIcon } from "@heroicons/vue/20/solid";
import BaseButton from "../ui/BaseButton.vue";
import CookieLineup from "./CookieLineup.vue";
import { COOKIE_DROP_VIDEO as CLIP, HERO_ARTWORK as ART } from "../../constants/cookieDrop";

// The landscape clip and two-row finale need width and headroom. Phones
// and short landscape windows keep the complete, unpinned static layout.
const CINEMATIC_QUERY = "(min-width: 1024px) and (min-height: 640px) and (prefers-reduced-motion: no-preference)";

// Starts false everywhere, including the prerendered HTML, and switches on
// once mounted: the server can't know the visitor's screen.
const cinematic = ref(false);
const videoReady = ref(false);
const videoFailed = ref(false);
const stage = ref(null);
let cinematicQuery = null;
let drop = null;
let unmounted = false;
let generation = 0;

// GSAP and the timeline are only downloaded where the sequence plays.
async function startDrop() {
  if (!cinematic.value || !stage.value || drop) return;
  const current = generation;
  try {
    const { createCookieDrop } = await import("../../composables/useCookieDropScroll");
    if (unmounted || current !== generation || !cinematic.value || drop) return;
    drop = createCookieDrop(stage.value);
  } catch {
    // A failed lazy chunk should leave the complete, usable static hero.
    if (!unmounted && current === generation) setCinematic(false);
  }
}
function stopDrop() {
  drop?.destroy();
  drop = null;
}

// Unpin before Vue re-renders: the pin wraps the stage in a spacer element.
async function setCinematic(matches) {
  generation += 1;
  stopDrop();
  videoReady.value = false;
  videoFailed.value = false;
  cinematic.value = matches;
  await nextTick();
  startDrop();
}
const onQueryChange = (event) => setCinematic(event.matches);

onMounted(() => {
  cinematicQuery = window.matchMedia(CINEMATIC_QUERY);
  cinematicQuery.addEventListener("change", onQueryChange);
  if (cinematicQuery.matches) setCinematic(true);
});
onBeforeUnmount(() => {
  unmounted = true;
  cinematicQuery?.removeEventListener("change", onQueryChange);
  stopDrop();
});
</script>

<template>
  <section class="relative" aria-labelledby="hero-title">
    <div
      ref="stage"
      class="hero-stage relative isolate overflow-hidden bg-tan flex"
      :class="cinematic ? 'h-screen' : 'min-h-[100svh] lg:min-h-[min(100svh,920px)]'"
    >
      <!-- Chocolate block: bottom band on mobile, right 20% on desktop -->
      <div
        class="hero-chocolate absolute inset-x-0 bottom-0 h-[34%] lg:inset-y-0 lg:left-auto lg:right-0 lg:h-auto lg:w-[20%] bg-chocolate -z-10"
        aria-hidden="true"
      >
        <div
          data-drop="glow"
          class="absolute inset-0 bg-[radial-gradient(60%_50%_at_30%_50%,rgba(197,159,113,0.28),transparent_70%)]"
        />
      </div>

      <!-- The cookie-drop clip, laid over the split and sized by the timeline.
           First-frame poster while it loads; both posters if it fails. -->
      <div v-if="cinematic" data-drop="media" class="hero-media absolute z-0" aria-hidden="true">
        <img :src="CLIP.poster" alt="" class="absolute inset-0 h-full w-full" />
        <img data-drop="splash-poster" :src="CLIP.splashPoster" alt="" class="absolute inset-0 h-full w-full" />
        <video
          data-drop="video"
          class="absolute inset-0 h-full w-full transition-opacity duration-300"
          :class="videoReady && !videoFailed ? 'opacity-100' : 'opacity-0'"
          :poster="CLIP.poster"
          muted
          playsinline
          preload="auto"
          disablepictureinpicture
          disableremoteplayback
          tabindex="-1"
          @loadeddata="videoReady = true"
          @error="videoFailed = true"
        >
          <source
            v-for="(src, i) in CLIP.sources"
            :key="src"
            :src="src"
            type="video/mp4"
            @error="i === CLIP.sources.length - 1 && (videoFailed = true)"
          />
        </video>
      </div>

      <!-- Mid-sequence line over the empty tan while the cookie falls -->
      <div v-if="cinematic" class="absolute inset-0 z-10 flex items-center pointer-events-none" aria-hidden="true">
        <div class="container-magic w-full">
          <p data-drop="tagline" class="hero-tagline font-display text-display">
            Make a <span class="sticker">splash.</span>
          </p>
        </div>
      </div>

      <div
        class="hero-layout container-magic w-full grid lg:grid-cols-[1.05fr_0.95fr] items-center gap-6 lg:gap-0 pt-[calc(var(--nav-h)+4rem)] pb-6 lg:pt-[var(--nav-h)] lg:pb-0"
      >
        <!-- Copy -->
        <div data-drop="copy" class="relative z-10 max-w-xl">
          <div class="animate-fade-up">
            <h1 id="hero-title" class="font-display text-display">
              Why feel <span class="sticker block">guilty?</span>
            </h1>

            <p class="mt-6 text-base leading-relaxed text-chocolate/85 max-w-md">
              Our cookies love you back. Proper chocolate, less sugar, baked for
              every kind of sweet tooth.
            </p>

            <div class="hero-actions mt-9 flex flex-wrap items-center gap-3">
              <BaseButton :to="{ name: 'Products' }" variant="primary" size="lg">
                Shop cookies
                <ArrowRightIcon class="h-5 w-5" />
              </BaseButton>
              <a href="#inside" class="hero-secondary btn-outline px-8 py-4 text-base border-chocolate/35">
                What's inside
              </a>
            </div>
          </div>
        </div>

        <!-- Hero cookie: in the grid on mobile, centred on the split on desktop -->
        <div data-drop="cookie-anchor" class="hero-cookie relative flex justify-center lg:block">
          <div class="w-[78%] max-w-[35.2rem] sm:max-w-[41.6rem] lg:w-full lg:max-w-none animate-cookie-in">
            <img
              data-drop="cookie"
              :src="ART.src"
              alt="A half-chocolate, half-vanilla swirl cookie with milk and chocolate splashing around it"
              :width="ART.width"
              :height="ART.height"
              fetchpriority="high"
              class="block w-full h-auto drop-shadow-cookie select-none"
            />
          </div>
        </div>
      </div>

      <CookieLineup v-if="cinematic" in-stage />
    </div>

    <CookieLineup v-if="!cinematic" />
  </section>
</template>

<style scoped>
@media (max-width: 639px) {
  .hero-stage {
    min-height: 0;
  }
  .hero-layout {
    padding-top: calc(var(--nav-h) + 3.2rem);
    padding-bottom: 2.4rem;
    gap: 2.4rem;
  }
  #hero-title {
    font-size: clamp(4.8rem, 13.5vw, 5.6rem);
  }
  .hero-actions {
    margin-top: 2.4rem;
    gap: 0.8rem;
  }
  .hero-actions > a {
    padding-inline: 1.6rem;
    padding-block: 1.2rem;
  }
  .hero-secondary {
    border-color: transparent;
    text-decoration: underline;
    text-underline-offset: 0.3em;
  }
  .hero-cookie > div {
    width: 82%;
    max-width: 30rem;
  }
  .hero-chocolate {
    height: 25%;
  }
}

@media (min-width: 1024px) {
  /* Cookie centred on the 80% split, sized to the hero's height */
  .hero-cookie {
    position: absolute;
    z-index: 5;
    left: 80%;
    top: calc(50% + var(--nav-h) / 2);
    width: min(44vw, calc((min(100dvh, 92rem) - var(--nav-h)) * 0.73));
    transform: translate(-48.9%, -47.6%);
  }
}

/* Hidden until the timeline brings them in (it sets visibility inline). */
.hero-media,
.hero-tagline {
  visibility: hidden;
}

.hero-media {
  will-change: transform, opacity;
}
/* When the frame can't cover the stage (very wide windows), feather its
   edges: browsers decode its flat backdrop a few levels off the CSS split.
   The left of the frame is empty backdrop, so that fade can be long. */
.hero-media.is-framed {
  --feather: linear-gradient(to right, transparent, #000 30%, #000 92%, transparent),
    linear-gradient(to bottom, transparent, #000 5%, #000 95%, transparent);
  -webkit-mask-image: var(--feather);
  -webkit-mask-composite: source-in;
  mask-image: var(--feather);
  mask-composite: intersect;
}
.hero-media video,
.hero-media img {
  object-fit: fill; /* the box is already exactly 16:9 */
}
</style>
