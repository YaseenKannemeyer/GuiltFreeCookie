<script setup>
import { ref, onMounted } from "vue";
import { api } from "../../api/storeApi";
const items = ref([]),
  failed = ref(false);
async function load() {
  try {
    items.value = await api("get", "/admin/inventory/low-stock");
    failed.value = false;
  } catch {
    failed.value = true;
  }
}
onMounted(load);
</script>
<template>
  <section class="mb-8 border-b border-chocolate/15 pb-6">
    <div class="flex flex-wrap justify-between gap-3">
      <h2 class="font-display text-3xl">Stock to watch</h2>
      <RouterLink to="/admindashboard/inventory" class="underline min-h-11"
        >Manage stock & packs</RouterLink
      >
    </div>
    <p v-if="failed">
      Stock could not be loaded.
      <button class="underline" @click="load">Retry</button>
    </p>
    <p v-else-if="!items.length" class="mt-3">No low-stock alerts.</p>
    <ul v-else class="flex flex-wrap gap-x-6 gap-y-3 mt-4">
      <li
        v-for="item in items.slice(0, 6)"
        :key="item.cookie?.cookieId || item.cookieId"
      >
        <RouterLink
          :to="{
            name: 'AdminInventory',
            query: { cookie: item.cookie?.cookieId || item.cookieId },
          }"
          class="underline"
          >{{ item.cookie?.description || item.name || item.description }} ·
          {{
            item.cookie?.stockQuantity ?? item.stockQuantity ?? item.stockStatus
          }}</RouterLink
        >
      </li>
    </ul>
  </section>
</template>
