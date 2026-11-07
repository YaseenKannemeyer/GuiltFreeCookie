<script setup>
import { ref, onMounted, watch } from "vue";
import { api, errorMessage } from "../api/storeApi";
import BaseButton from "../components/ui/BaseButton.vue";
const items = ref([]),
  status = ref("NEW"),
  busy = ref(true),
  error = ref("");
let loadVersion = 0;
async function load() {
  const version = ++loadVersion;
  busy.value = true;
  error.value = "";
  try {
    const result = await api("get", "/admin/messages", undefined, {
      params: status.value ? { status: status.value } : {},
    });
    if (version === loadVersion) items.value = result;
  } catch (e) {
    if (version === loadVersion) error.value = errorMessage(e);
  } finally {
    if (version === loadVersion) busy.value = false;
  }
}
async function resolve(id) {
  busy.value = true;
  try {
    await api("put", `/admin/messages/${id}/resolve`);
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
    <h1 class="font-display text-display">Customer inbox</h1>
    <label class="block mt-6 max-w-xs">
      <span class="block text-sm font-semibold mb-1.5">Show messages</span>
      <select v-model="status" class="field h-14">
        <option value="NEW">New</option>
        <option value="RESOLVED">Resolved</option>
        <option value="">All messages</option>
      </select></label
    >
    <p v-if="error" role="alert" class="mt-5 text-red-800">
      {{ error }} <button class="underline min-h-11" @click="load">Retry</button>
    </p>
    <p v-if="busy" role="status" class="mt-5">Updating inbox…</p>
    <p v-else-if="!error && !items.length" class="py-12">
      No {{ status.toLowerCase() }} messages.
    </p>
    <article
      v-for="m in items"
      :key="m.id"
      class="py-7 border-b border-chocolate/15"
    >
      <div class="flex flex-wrap justify-between gap-3">
        <h2 class="font-extrabold text-[2rem] min-w-0 break-words">{{ m.subject }}</h2>
        <span class="text-sm font-semibold rounded-full bg-chocolate/[0.07] px-3 py-1">{{ m.status === 'NEW' ? 'New' : 'Resolved' }}</span>
      </div>
      <p class="mt-2">
        {{ m.name }} ·
        <a :href="`mailto:${m.email}`" class="underline break-all">{{
          m.email
        }}</a
        ><span v-if="m.phone"> · {{ m.phone }}</span>
      </p>
      <p class="mt-2 text-sm">
        {{ new Date(m.createdAt).toLocaleString("en-ZA") }}
      </p>
      <p class="my-5 whitespace-pre-line break-words">{{ m.message }}</p>
      <BaseButton
        v-if="m.status === 'NEW'"
        variant="outline"
        :disabled="busy"
        @click="resolve(m.id)"
        >Mark resolved</BaseButton
      >
      <p v-else class="text-sm">
        Resolved {{ new Date(m.resolvedAt).toLocaleString("en-ZA") }}
      </p>
    </article>
  </div>
</template>
