<script setup>
import { ref, onMounted, watch } from "vue";
import { api, errorMessage } from "../api/storeApi";
import BaseButton from "../components/ui/BaseButton.vue";
const items = ref([]),
  status = ref("PENDING"),
  busy = ref(true),
  error = ref("");
let loadVersion = 0;
async function load() {
  const version = ++loadVersion;
  busy.value = true;
  error.value = "";
  try {
    const result = await api("get", "/admin/reviews", undefined, {
      params: status.value ? { status: status.value } : {},
    });
    if (version === loadVersion) items.value = result;
  } catch (e) {
    if (version === loadVersion) error.value = errorMessage(e);
  } finally {
    if (version === loadVersion) busy.value = false;
  }
}
async function moderate(id, action) {
  busy.value = true;
  try {
    await api("put", `/admin/reviews/${id}/${action}`);
    await load();
  } catch (e) {
    error.value = errorMessage(e);
  } finally {
    busy.value = false;
  }
}
onMounted(load);
watch(status, load);
</script>
<template>
  <div class="max-w-6xl mx-auto">
    <h1 class="font-display text-display">Review moderation</h1>
    <p class="mt-3">
      Only approved reviews appear in the store and rating totals.
    </p>
    <label class="block mt-6 max-w-xs">
      <span class="block text-sm font-semibold mb-1.5">Status</span>
      <select v-model="status" class="field h-14">
        <option value="PENDING">Pending</option>
        <option value="APPROVED">Approved</option>
        <option value="REJECTED">Rejected</option>
        <option value="">All reviews</option>
      </select></label
    >
    <p v-if="error" role="alert" class="mt-5 text-red-800">
      {{ error }} <button class="underline min-h-11" @click="load">Retry</button>
    </p>
    <p v-if="busy" role="status" class="mt-5">Updating reviews…</p>
    <p v-else-if="!error && !items.length" class="py-12">
      No {{ status.toLowerCase() }} reviews.
    </p>
    <article
      v-for="r in items"
      :key="r.id"
      class="py-7 border-b border-chocolate/15"
    >
      <h2 class="font-extrabold text-[2rem] break-words">{{ r.rating }} / 5 · {{ r.title }}</h2>
      <p class="mt-2">
        {{ r.authorName }} · {{ r.status
        }}<span v-if="r.verifiedBuyer"> · Verified buyer</span>
      </p>
      <RouterLink
        :to="`/product/${r.cookieId}`"
        class="underline inline-flex items-center min-h-11 py-2"
        >View cookie</RouterLink
      >
      <p class="my-4 whitespace-pre-line break-words">{{ r.body }}</p>
      <div class="flex flex-wrap gap-3">
        <BaseButton
          v-if="r.status !== 'APPROVED'"
          :disabled="busy"
          @click="moderate(r.id, 'approve')"
          >Approve</BaseButton
        ><BaseButton
          v-if="r.status !== 'REJECTED'"
          variant="outline"
          :disabled="busy"
          @click="moderate(r.id, 'reject')"
          >Reject</BaseButton
        >
      </div>
    </article>
  </div>
</template>
