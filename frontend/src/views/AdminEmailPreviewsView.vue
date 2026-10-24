<script setup>
import { ref, onMounted, watch } from "vue";
import { api, errorMessage } from "../api/storeApi";
import BaseButton from "../components/ui/BaseButton.vue";
const templates = ref([]),
  selected = ref(""),
  html = ref(""),
  error = ref(""),
  notice = ref(""),
  busy = ref(false),
  mobile = ref(false);
let version = 0;
async function preview() {
  if (!selected.value) return;
  const current = ++version;
  busy.value = true;
  error.value = "";
  notice.value = "";
  html.value = "";
  try {
    const result = await api("get", `/admin/email-previews/${selected.value}`);
    if (current === version) html.value = result;
  } catch (e) {
    if (current === version) error.value = errorMessage(e);
  } finally {
    if (current === version) busy.value = false;
  }
}
async function load() {
  try {
    templates.value = await api("get", "/admin/email-previews");
    selected.value = templates.value[0] || "";
  } catch (e) {
    error.value = errorMessage(e);
  }
}
async function send() {
  busy.value = true;
  error.value = "";
  try {
    await api("post", `/admin/email-previews/${selected.value}/send-test`);
    notice.value =
      "Test email queued for your admin email. Delivery is disabled when MAIL_ENABLED is false.";
  } catch (e) {
    error.value = errorMessage(e);
  } finally {
    busy.value = false;
  }
}
onMounted(load);
watch(selected, preview);
</script>
<template>
  <div class="p-5 md:p-10 max-w-6xl mx-auto">
    <h1 class="font-display text-display">Email studio</h1>
    <p class="mt-3">
      Preview the bakery’s emails with clearly illustrative sample data.
    </p>
    <div class="flex flex-wrap items-end gap-4 mt-7">
      <label class="w-full sm:w-auto sm:flex-1 min-w-0"
        >Template<select v-model="selected" class="field mt-2">
          <option v-for="t in templates" :key="t">{{ t }}</option>
        </select></label
      ><BaseButton :disabled="busy || !selected" @click="send"
        >Send test to me</BaseButton
      ><label class="flex items-center gap-2 min-h-11"
        ><input v-model="mobile" type="checkbox" />Phone width</label
      >
    </div>
    <p v-if="notice" role="status" class="mt-5">{{ notice }}</p>
    <p v-if="error" role="alert" class="mt-5 text-red-800">
      {{ error }}
      <button class="underline" @click="selected ? preview() : load()">
        Retry
      </button>
    </p>
    <p v-if="busy" role="status" class="mt-5">Preparing email…</p>
    <iframe
      v-if="html"
      :srcdoc="html"
      sandbox=""
      referrerpolicy="no-referrer"
      title="Rendered email preview with sample data"
      class="block mt-8 mx-auto h-[850px] w-full border border-chocolate/20 bg-white"
      :style="{ maxWidth: mobile ? '375px' : '700px' }"
    />
  </div>
</template>
