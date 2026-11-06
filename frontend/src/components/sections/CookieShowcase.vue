<script setup>
/**
 * CookieShowcase — "Hot picks this week". Alternating full-width cards with
 * an oversized cookie breaking out of the bottom edge; the card floods with
 * that cookie's colour on hover. Shows one live cookie from each range and
 * falls back to the signature flavours if the catalogue can't be reached.
 */
import { computed, onMounted } from "vue";
import { RouterLink } from "vue-router";
import { ArrowRightIcon } from "@heroicons/vue/20/solid";
import BaseButton from "../ui/BaseButton.vue";
import {
  useCatalog,
  cookieImage,
  cookieName,
  imageFallback,
  onePerCategory,
} from "../../composables/useCatalog";
import { useFormat } from "../../composables/useFormat";
import { useScrollReveal } from "../../composables/useScrollReveal";
import {
  formatCategoryLabel,
  categoryBlurb,
} from "../../constants/cookieCategories";

const { cookies: catalog, load } = useCatalog();
const { rand } = useFormat();
const { vReveal } = useScrollReveal();

onMounted(load);

// Hover washes. Dark enough that white text stays above 4.5:1.
const washes = ["#8A4B12", "#8E2440", "#9A5310", "#3D1F17"];

const signature = [
  {
    title: "Double Chocolate Chunk",
    description:
      "Deep cocoa dough, generous dark chocolate chunks and a crackled top, with crisp edges and a soft chocolate centre.",
    image: "/img/cookies/double-chocolate-chunk.webp",
  },
  {
    title: "Cinnamon Pecan",
    description:
      "A warmly spiced cookie with a soft middle, gently crisp edges and toasted pecan pieces baked through the dough.",
    image: "/img/cookies/cinnamon-pecan.webp",
  },
  {
    title: "Lemon Almond",
    description:
      "Bright lemon zest lifts a tender almond cookie, while toasted almond flakes bring a delicate crunch to its golden top.",
    image: "/img/cookies/lemon-almond.webp",
  },
  {
    title: "Raspberry Coconut",
    description:
      "Tart raspberry pieces and toasted coconut brighten a soft vanilla cookie with lightly crisp edges.",
    image: "/img/cookies/raspberry-coconut.webp",
  },
].map((c, i) => ({
  ...c,
  key: `signature-${i}`,
  badge: "Freshly baked",
  price: null,
  to: { name: "Products" },
  secondary: null,
}));

const cards = computed(() => {
  const live = onePerCategory(catalog.value.filter((c) => c.featured))
    .slice(0, 4)
    .map((c) => ({
      key: c.cookieId,
      title: cookieName(c),
      description:
        c.longDescription ||
        [
          categoryBlurb(c.category),
          c.ingredients && `Made with ${c.ingredients.toLowerCase()}.`,
        ]
          .filter(Boolean)
          .join(" "),
      image: cookieImage(c),
      onError: imageFallback(c),
      badge: formatCategoryLabel(c.category),
      price: c.price,
      to: { name: "ProductDetail", params: { id: c.slug || c.cookieId } },
      secondary: { name: "Products", query: { category: c.category } },
    }));
  return (live.length ? live : signature).map((c, i) => ({
    ...c,
    wash: washes[i % washes.length],
  }));
});
</script>

<template>
  <section
    class="bg-tan pt-20 md:pt-28 pb-20 sm:pb-36 lg:pb-44 overflow-hidden"
    aria-labelledby="showcase-title"
  >
    <div class="container-magic">
      <h2
        id="showcase-title"
        v-reveal
        class="font-display text-center text-display mb-16 sm:mb-28 lg:mb-40"
      >
        Hot picks this week
      </h2>

      <div class="showcase-list">
        <article
          v-for="(cookie, index) in cards"
          :key="cookie.key"
          v-reveal
          class="showcase-card group"
          :class="{ reverse: index % 2 === 1 }"
          :style="{ '--wash': cookie.wash }"
        >
          <!-- Text -->
          <div class="showcase-content">
            <span class="showcase-badge">{{ cookie.badge }}</span>

            <h3 class="showcase-title display">
              <!-- On mobile this link stretches over the whole card -->
              <RouterLink :to="cookie.to" class="showcase-title-link">{{
                cookie.title
              }}</RouterLink>
            </h3>

            <p class="showcase-desc">{{ cookie.description }}</p>

            <div class="showcase-actions">
              <BaseButton
                :to="cookie.to"
                variant="primary"
                class="showcase-primary"
              >
                View cookie
                <ArrowRightIcon class="h-5 w-5" />
              </BaseButton>
              <RouterLink
                v-if="cookie.secondary"
                :to="cookie.secondary"
                class="showcase-link"
              >
                More {{ cookie.badge.toLowerCase() }}
              </RouterLink>
              <span v-if="cookie.price" class="showcase-price tabular">{{
                rand(cookie.price)
              }}</span>
            </div>
          </div>

          <!-- Image -->
          <div class="showcase-image-wrap" aria-hidden="true">
            <img
              :src="cookie.image"
              alt=""
              loading="lazy"
              class="showcase-image"
              @error="cookie.onError && cookie.onError($event)"
            />
          </div>
        </article>
      </div>
    </div>
  </section>
</template>

<style scoped>
/* ---------- Desktop / tablet: every card like the first. Short card,
   cookie centred on it and taller than it, so it bursts out top and bottom ---------- */
.showcase-list {
  display: flex;
  flex-direction: column;
  gap: 17.6rem;
}

.showcase-card {
  position: relative;
  display: flex;
  align-items: center;
  /* Same height for every card so each cookie bursts out the same way */
  height: 45.6rem;
  padding: 4.8rem 5.6rem;
  border-radius: 28px;
  background: #e3d9c8;
  transition: background-color 0.45s cubic-bezier(0.16, 1, 0.3, 1);
}
.showcase-card:hover {
  background: var(--wash);
}

/* Every other card is mirrored: text right, cookie left */
.reverse {
  flex-direction: row-reverse;
}

.showcase-content {
  position: relative;
  z-index: 2;
  max-width: 54%;
}
.reverse .showcase-content {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  text-align: right;
}

.showcase-badge {
  display: inline-block;
  padding: 0.64rem 1.44rem;
  border-radius: 999px;
  background: #2e0e0b;
  color: #fbf9f4;
  font-size: var(--fs-sm);
  font-weight: 700;
  transition:
    background-color 0.3s,
    color 0.3s;
}
.showcase-card:hover .showcase-badge {
  background: #fbf9f4;
  color: #2e0e0b;
}

.showcase-title {
  margin: 1.44rem 0 0.96rem;
  font-size: var(--fs-display);
  line-height: 1.1;
  color: #2e0e0b;
  letter-spacing: -0.01em;
  text-wrap: balance;
  transition: color 0.3s;
}
.showcase-title-link {
  color: inherit;
}

.showcase-desc {
  max-width: 54.4rem;
  font-size: var(--fs-base);
  line-height: 1.6;
  color: rgba(46, 14, 11, 0.8);
  transition: color 0.3s;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.showcase-card:hover .showcase-title,
.showcase-card:hover .showcase-desc,
.showcase-card:hover .showcase-link,
.showcase-card:hover .showcase-price {
  color: #fbf9f4;
}

.showcase-actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 1.2rem 2.4rem;
  margin-top: 2.4rem;
}
.reverse .showcase-actions {
  flex-direction: row-reverse;
}
.showcase-card:hover .showcase-primary {
  background: #fbf9f4;
  color: #2e0e0b;
}

.showcase-link {
  display: inline-flex;
  align-items: center;
  min-height: 4.4rem;
  font-weight: 700;
  color: #2e0e0b;
  text-decoration: underline;
  text-decoration-thickness: 2px;
  text-underline-offset: 6px;
  transition: color 0.3s;
}

.showcase-price {
  font-size: var(--fs-base);
  font-weight: 800;
  color: #2e0e0b;
  transition: color 0.3s;
}

.showcase-image-wrap {
  position: absolute;
  top: 50%;
  right: 3.2rem;
  width: min(42%, 52.8rem);
  transform: translateY(-50%);
  pointer-events: none;
}
.reverse .showcase-image-wrap {
  right: auto;
  left: 3.2rem;
}

.showcase-image {
  width: 100%;
  filter: drop-shadow(0 30px 30px rgba(46, 14, 11, 0.35));
  transition: transform 0.6s cubic-bezier(0.16, 1, 0.3, 1);
}
.showcase-card:hover .showcase-image {
  transform: rotate(-3deg) scale(1.04);
}
.reverse .showcase-image {
  transform: scaleX(-1);
}
.reverse:hover .showcase-image {
  transform: scaleX(-1) rotate(-3deg) scale(1.04);
}

/* Tablet */
@media (max-width: 1023px) {
  .showcase-list {
    gap: 12.8rem;
  }
  .showcase-card {
    height: auto;
    min-height: 25.6rem;
    padding: 3.6rem 4rem;
  }
  .showcase-content {
    max-width: 58%;
  }
  .showcase-image-wrap {
    width: 38%;
    right: 2rem;
  }
  .reverse .showcase-image-wrap {
    right: auto;
    left: 2rem;
  }
}

/* ---------- Mobile: slim card in the hover colour, alternating sides,
   cookie much taller than the card so it bursts out like desktop ---------- */
@media (max-width: 639px) {
  .showcase-list {
    gap: 5.6rem;
  }
  .showcase-card,
  .showcase-card:hover {
    height: auto;
    min-height: 14.4rem;
    padding: 2rem;
    border-radius: 24px;
    background: var(--wash);
  }
  .showcase-content {
    position: static;
    max-width: 52%;
  }

  .showcase-badge,
  .showcase-desc {
    display: none;
  }

  .showcase-actions {
    display: flex;
    margin-top: 1.2rem;
  }
  .showcase-primary,
  .showcase-link {
    display: none;
  }
  .showcase-price {
    color: #fbf9f4;
  }

  .showcase-title,
  .showcase-card:hover .showcase-title {
    margin: 0;
    font-size: clamp(1.8rem, 5vw, 2.2rem);
    line-height: 1.2;
    color: #fbf9f4;
  }
  /* Whole card is the tap target */
  .showcase-title-link::after {
    content: "";
    position: absolute;
    inset: 0;
    z-index: 3;
    border-radius: inherit;
  }

  /* ~20rem tall cookie on an 12.8rem card */
  .showcase-image-wrap {
    width: 46%;
    right: 0;
  }
  .reverse .showcase-image-wrap {
    right: auto;
    left: 0;
  }
  .reverse .showcase-image {
    transform: scaleX(-1);
  }
  .showcase-card:hover .showcase-image {
    transform: none;
  }
  .reverse:hover .showcase-image {
    transform: scaleX(-1);
  }
  .showcase-image {
    filter: drop-shadow(0 18px 20px rgba(0, 0, 0, 0.4));
  }
}
</style>
