<script setup>
import { ref, reactive, onMounted, watch } from "vue";
import { RouterLink, useRoute } from "vue-router";
import { api, errorMessage } from "../../api/storeApi";
import { useCustomerAuth } from "../../composables/useCustomerAuth";
import BaseButton from "../ui/BaseButton.vue";
import BaseInput from "../ui/BaseInput.vue";
const props = defineProps({ cookieId: { type: String, required: true } }),
  route = useRoute();
const { customer, isLoggedIn } = useCustomerAuth();
const result = ref(null),
  error = ref(""),
  busy = ref(false),
  sent = ref(false),
  page = ref(0),
  sort = ref("newest"),
  loading = ref(false);
const form = reactive({
  rating: 5,
  title: "",
  body: "",
  authorName: customer.value?.name?.firstName || "",
});
async function load() {
  loading.value = true;
  try {
    result.value = await api(
      "get",
      `/cookies/${props.cookieId}/reviews`,
      undefined,
      { params: { page: page.value, sort: sort.value, size: 6 } },
    );
    error.value = "";
  } catch (e) {
    error.value = errorMessage(e);
  } finally {
    loading.value = false;
  }
}
onMounted(load);
watch([page, sort], load);
async function submit() {
  busy.value = true;
  error.value = "";
  try {
    await api("post", `/me/reviews/${props.cookieId}`, form);
    sent.value = true;
  } catch (e) {
    error.value = errorMessage(e);
  } finally {
    busy.value = false;
  }
}
</script>
<template>
  <section
    class="container-magic py-12 md:py-20 border-t border-chocolate/15"
    aria-labelledby="reviews-title"
  >
    <h2 id="reviews-title" class="font-display text-display">
      From the cookie crew
    </h2>
    <p v-if="error" class="mt-5 text-red-800" role="alert">
      {{ error }} <button class="underline" @click="load">Retry</button>
    </p>
    <p v-if="loading" role="status" class="mt-5">Loading reviews…</p>
    <div class="grid lg:grid-cols-[1fr_2fr] gap-10 mt-8">
      <div>
        <template v-if="result"
          ><p class="font-bold text-2xl">
            {{
              result.summary.reviewCount
                ? `${Number(result.summary.averageRating).toFixed(1)} / 5`
                : "No reviews yet"
            }}
          </p>
          <p class="mt-1">{{ result.summary.reviewCount }} approved {{ result.summary.reviewCount === 1 ? "review" : "reviews" }}</p>
          <div
            v-for="rating in [5, 4, 3, 2, 1]"
            :key="rating"
            class="flex gap-3 items-center mt-3"
          >
            <span class="w-10 shrink-0 whitespace-nowrap">{{ rating }} ★</span
            ><span
              class="flex-1 h-3 rounded-full bg-chocolate/10 overflow-hidden"
              role="img"
              :aria-label="`${rating} stars: ${result.summary.histogram[rating]} reviews`"
              ><span
                class="block h-full rounded-full bg-primary"
                :style="{ width: `${(100 * result.summary.histogram[rating]) / Math.max(1, result.summary.reviewCount)}%` }"
              /></span
            ><span class="w-6 text-right">{{ result.summary.histogram[rating] }}</span>
          </div></template
        >
      </div>
      <div>
        <label class="flex flex-wrap items-center gap-3"
          >Sort reviews<select
            v-model="sort"
            class="field max-w-xs"
            @change="page = 0"
          >
            <option value="newest">Newest</option>
            <option value="highest">Highest rating</option>
            <option value="lowest">Lowest rating</option>
          </select></label
        >
        <article
          v-for="review in result?.page.items"
          :key="review.id"
          class="py-6 border-b border-chocolate/15"
        >
          <p class="font-bold">{{ review.rating }} / 5 · {{ review.title }}</p>
          <p class="mt-3 whitespace-pre-line break-words">{{ review.body }}</p>
          <p class="mt-3 text-sm">
            {{ review.authorName
            }}<span v-if="review.verifiedBuyer"> · Verified buyer</span> ·
            {{ new Date(review.createdAt).toLocaleDateString("en-ZA") }}
          </p>
        </article>
        <div
          v-if="result?.page.totalPages > 1"
          class="flex items-center gap-4 mt-6"
        >
          <BaseButton
            variant="outline"
            :disabled="page === 0 || loading"
            @click="page--"
            >Previous</BaseButton
          ><span>{{ page + 1 }} / {{ result.page.totalPages }}</span
          ><BaseButton
            variant="outline"
            :disabled="page + 1 >= result.page.totalPages || loading"
            @click="page++"
            >Next</BaseButton
          >
        </div>
      </div>
    </div>
    <div class="mt-12 max-w-2xl">
      <h3 class="font-display text-3xl">Tried this cookie?</h3>
      <p v-if="sent" role="status" class="mt-4">
        Thank you! Your review is waiting for approval.
      </p>
      <form
        v-else-if="isLoggedIn"
        class="space-y-5 mt-5"
        @submit.prevent="submit"
      >
        <label class="block"
          >Your rating<select v-model.number="form.rating" class="field mt-1">
            <option v-for="n in [5, 4, 3, 2, 1]" :key="n" :value="n">
              {{ n }} out of 5
            </option>
          </select></label
        ><BaseInput
          v-model="form.authorName"
          label="Public display name"
          required
        /><BaseInput
          v-model="form.title"
          label="Review title"
          required
        /><BaseInput
          v-model="form.body"
          type="textarea"
          label="Your review"
          help="Up to 2000 characters. Reviews appear after moderation."
          required
        /><BaseButton type="submit" :disabled="busy">{{
          busy ? "Submitting…" : "Submit review"
        }}</BaseButton>
      </form>
      <p v-else class="mt-4">
        <RouterLink
          :to="{ name: 'CustomerLogin', query: { redirect: route.fullPath } }"
          class="underline font-bold"
          >Log in</RouterLink
        >
        to share your review.
      </p>
    </div>
  </section>
</template>
