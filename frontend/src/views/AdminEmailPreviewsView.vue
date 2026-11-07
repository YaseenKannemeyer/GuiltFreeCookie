<script setup>
import { ref, onMounted, watch } from "vue";
import { api, errorMessage } from "../api/storeApi";
import BaseButton from "../components/ui/BaseButton.vue";
import { DEMO_MODE } from "../constants/business";
const deliveries = ref([]), deliveryError = ref("");
const retrying = ref(new Set());
const deliveriesLoading = ref(false);
async function loadDeliveries() {
  if (deliveriesLoading.value) return;
  deliveriesLoading.value = true;
  deliveryError.value = "";
  try { deliveries.value = await api("get", "/admin/email-deliveries"); }
  catch (e) { deliveryError.value = errorMessage(e); }
  finally { deliveriesLoading.value = false; }
}
async function retryDelivery(id) {
  if (retrying.value.has(id)) return;
  retrying.value.add(id);
  try { await api("post", `/admin/email-deliveries/${id}/retry`); await loadDeliveries(); }
  catch (e) { deliveryError.value = errorMessage(e); }
  finally { retrying.value.delete(id); }
}
async function openDelivery(id) {
  try { html.value = await api("get", `/admin/email-deliveries/${id}/preview`); }
  catch (e) { deliveryError.value = errorMessage(e); }
}
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
    notice.value = DEMO_MODE ? "Sample captured in the demo inbox below. No email was sent." : "Test email queued for delivery to your admin email.";
    await loadDeliveries();
  } catch (e) {
    error.value = errorMessage(e);
  } finally {
    busy.value = false;
  }
}
onMounted(load);
onMounted(loadDeliveries);
watch(selected, preview);
</script>
<template>
  <div class="max-w-6xl mx-auto">
    <h1 class="font-display text-display">Email studio</h1>
    <p class="mt-3">
      Preview the bakery’s emails with clearly illustrative sample data.
    </p>
    <div class="flex flex-wrap items-end gap-4 mt-7">
      <label class="w-full sm:w-auto sm:flex-1 min-w-0">
        <span class="block text-sm font-semibold mb-1.5">Template</span>
        <select v-model="selected" class="field h-14" :disabled="busy">
          <option v-for="t in templates" :key="t" :value="t">{{ t.replaceAll('-', ' ').replace(/^./, c => c.toUpperCase()) }}</option>
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
      class="block mt-8 mx-auto h-[70dvh] min-h-[32rem] w-full border border-chocolate/20 bg-white"
      :style="{ maxWidth: mobile ? '375px' : '700px' }"
    />
    <section class="mt-10 border-t border-chocolate/15 pt-7" aria-labelledby="delivery-title">
      <div class="flex flex-wrap items-center justify-between gap-3">
        <h2 id="delivery-title" class="font-bold">{{ DEMO_MODE ? 'Demo inbox' : 'Recent email deliveries' }}</h2>
        <BaseButton variant="outline" size="sm" :loading="deliveriesLoading" @click="loadDeliveries">Refresh inbox</BaseButton>
      </div>
      <p class="mt-3 text-sm">{{ DEMO_MODE ? 'Emails are captured here for testing. They are never sent to external mailboxes.' : 'Failed emails retry automatically up to five times. Expired links require a new email.' }}</p>
      <p v-if="deliveryError" role="alert" class="mt-4 text-red-800">{{ deliveryError }}</p>
      <p v-if="!deliveries.length && !deliveriesLoading && !deliveryError" class="mt-5">No messages yet. Send yourself a sample email.</p>
      <ul class="mt-5 divide-y divide-chocolate/15">
        <li v-for="delivery in deliveries" :key="delivery.id" class="py-4 flex flex-wrap justify-between gap-4">
          <div class="min-w-0">
            <p class="font-semibold break-words">{{ delivery.subject }}</p>
            <p class="text-sm break-all">{{ delivery.recipient }} · {{ DEMO_MODE && delivery.status === 'PENDING' ? 'Captured' : delivery.status }} · {{ delivery.attempts }} attempts</p>
          </div>
          <BaseButton v-if="DEMO_MODE && delivery.status === 'PENDING'" size="sm" variant="outline" @click="openDelivery(delivery.id)">Read email</BaseButton>
          <BaseButton v-if="delivery.canRetry" size="sm" variant="outline" :loading="retrying.has(delivery.id)" @click="retryDelivery(delivery.id)">Retry delivery</BaseButton>
        </li>
      </ul>
    </section>
  </div>
</template>
