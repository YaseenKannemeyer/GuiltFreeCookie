<script setup>
import { ref, onMounted } from "vue";
import { api } from "../../api/storeApi";
import { useSiteInfo } from "../../composables/useSiteInfo";
const { info, load } = useSiteInfo(),
  reviews = ref([]);
onMounted(async () => {
  load();
  try {
    reviews.value = await api("get", "/reviews/featured");
  } catch {
    reviews.value = [];
  }
});
</script>
<template>
  <section
    v-if="reviews.length || info?.stats.reviewCount"
    class="section bg-surface"
  >
    <div class="container-magic">
      <h2 class="font-display text-display">Good cookies. Happy people.</h2>
      <p v-if="info?.stats.reviewCount" class="mt-4">
        {{ Number(info.stats.averageRating).toFixed(1) }} / 5 from
        {{ info.stats.reviewCount }} approved customer reviews
      </p>
      <div class="grid md:grid-cols-2 lg:grid-cols-3 gap-8 mt-10">
        <figure
          v-for="r in reviews"
          :key="r.id"
          class="border-t border-primary pt-5"
        >
          <p class="font-bold">{{ r.rating }} / 5 · {{ r.title }}</p>
          <blockquote class="mt-4 whitespace-pre-line break-words">
            {{ r.body }}
          </blockquote>
          <figcaption class="mt-5 font-semibold">
            {{ r.authorName
            }}<span v-if="r.verifiedBuyer" class="font-normal">
              · Verified buyer</span
            >
          </figcaption>
        </figure>
      </div>
    </div>
  </section>
</template>
