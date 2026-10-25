<script setup>
import { ref, reactive, onMounted, nextTick } from "vue";
import { accountApi } from "../../api/accountApi";
import { errorMessage } from "../../api/storeApi";
import { useCustomerAuth } from "../../composables/useCustomerAuth";
import BaseButton from "../ui/BaseButton.vue";
import BaseInput from "../ui/BaseInput.vue";
const provinces = [
  ["EASTERN_CAPE", "Eastern Cape"],
  ["FREE_STATE", "Free State"],
  ["GAUTENG", "Gauteng"],
  ["KWAZULU_NATAL", "KwaZulu-Natal"],
  ["LIMPOPO", "Limpopo"],
  ["MPUMALANGA", "Mpumalanga"],
  ["NORTH_WEST", "North West"],
  ["NORTHERN_CAPE", "Northern Cape"],
  ["WESTERN_CAPE", "Western Cape"],
];
const { customer } = useCustomerAuth();
const items = ref([]),
  loading = ref(true),
  busy = ref(false),
  error = ref(""),
  notice = ref(""),
  editing = ref(false),
  editId = ref(null),
  deleteId = ref(null),
  editor = ref(null);
const blank = () => ({
  label: "",
  recipientName: [
    customer.value?.name?.firstName,
    customer.value?.name?.lastName,
  ]
    .filter(Boolean)
    .join(" "),
  phone: customer.value?.mobileNumber || "",
  isDefault: false,
  address: {
    streetNumber: "",
    streetName: "",
    complex: "",
    suburb: "",
    city: "",
    province: "WESTERN_CAPE",
    postalCode: "",
  },
});
const form = reactive(blank());
async function load() {
  loading.value = true;
  try {
    items.value = await accountApi.addresses();
    error.value = "";
  } catch (e) {
    error.value = errorMessage(e);
  } finally {
    loading.value = false;
  }
}
onMounted(load);
async function edit(address = null) {
  editId.value = address?.id || null;
  Object.assign(
    form,
    address
      ? {
          label: address.label,
          recipientName: address.recipientName,
          phone: address.phone,
          isDefault: address.isDefault,
          address: { ...address.address },
        }
      : blank(),
  );
  editing.value = true;
  deleteId.value = null;
  error.value = "";
  notice.value = "";
  await nextTick();
  editor.value?.querySelector("input")?.focus();
}
async function save() {
  if (busy.value) return;
  busy.value = true;
  error.value = "";
  notice.value = "";
  try {
    const payload = { ...form, address: { ...form.address } };
    if (editId.value) await accountApi.updateAddress(editId.value, payload);
    else await accountApi.createAddress(payload);
    editing.value = false;
    await load();
    notice.value = "Address saved.";
  } catch (e) {
    error.value = errorMessage(e);
  } finally {
    busy.value = false;
  }
}
async function remove(id) {
  busy.value = true;
  error.value = "";
  try {
    await accountApi.deleteAddress(id);
    deleteId.value = null;
    await load();
    notice.value = "Address deleted.";
  } catch (e) {
    error.value = errorMessage(e);
  } finally {
    busy.value = false;
  }
}
async function makeDefault(id) {
  busy.value = true;
  error.value = "";
  try {
    await accountApi.makeDefault(id);
    await load();
    notice.value = "Default address updated.";
  } catch (e) {
    error.value = errorMessage(e);
  } finally {
    busy.value = false;
  }
}
</script>
<template>
  <section
    id="addresses"
    class="scroll-mt-32"
    aria-labelledby="addresses-heading"
  >
    <div class="flex flex-wrap items-center justify-between gap-3">
      <h2 id="addresses-heading" class="text-2xl font-extrabold">Addresses</h2>
      <BaseButton
        v-if="!editing"
        variant="outline"
        :disabled="loading || busy || items.length >= 10"
        @click="edit()"
        >Add address</BaseButton
      >
    </div>
    <p class="mt-2 text-sm">
      Save up to 10 delivery addresses. You can change the details at checkout.
    </p>
    <p v-if="error" role="alert" class="mt-4 text-red-800">
      {{ error }}
      <button v-if="!editing" class="underline min-h-11" @click="load">
        Retry
      </button>
    </p>
    <p v-if="notice" role="status" class="mt-4">{{ notice }}</p>
    <p v-if="loading" role="status" class="py-6">Loading addresses…</p>
    <p v-else-if="!items.length && !editing" class="py-6">
      No saved addresses yet.
    </p>
    <div class="grid sm:grid-cols-2 gap-4 mt-5">
      <article
        v-for="a in items"
        :key="a.id"
        class="min-w-0 rounded-xl border border-chocolate/20 p-5"
      >
        <div class="flex flex-wrap gap-2 items-center">
          <h3 class="font-bold break-words">{{ a.label }}</h3>
          <span
            v-if="a.isDefault"
            class="rounded-full bg-primary px-3 py-1 text-sm"
            >Default</span
          >
        </div>
        <p class="mt-3 break-words">{{ a.recipientName }}</p>
        <p class="text-sm break-words">{{ a.phone }}</p>
        <address class="not-italic mt-3 break-words">
          {{ a.address.complex }}<br v-if="a.address.complex" />{{
            a.address.streetNumber
          }}
          {{ a.address.streetName }}<br />{{ a.address.suburb }}<br />{{
            a.address.city
          }}, {{ a.address.postalCode }}<br />{{
            provinces.find(([key]) => key === a.address.province)?.[1]
          }}
        </address>
        <div v-if="deleteId !== a.id" class="flex flex-wrap gap-x-5 mt-4">
          <button
            class="underline min-h-11"
            :disabled="busy || editing"
            @click="edit(a)"
          >
            Edit</button
          ><button
            class="underline min-h-11"
            :disabled="busy || editing"
            @click="deleteId = a.id"
          >
            Delete</button
          ><button
            v-if="!a.isDefault"
            class="underline min-h-11"
            :disabled="busy || editing"
            @click="makeDefault(a.id)"
          >
            Make default
          </button>
        </div>
        <div v-else class="mt-4">
          <p>Delete this address?</p>
          <div class="flex flex-wrap gap-3 mt-3">
            <BaseButton size="sm" :disabled="busy" @click="remove(a.id)"
              >Yes, delete</BaseButton
            ><BaseButton
              size="sm"
              variant="outline"
              :disabled="busy"
              @click="deleteId = null"
              >Keep it</BaseButton
            >
          </div>
        </div>
      </article>
    </div>
    <form
      v-if="editing"
      ref="editor"
      class="mt-7 space-y-5 rounded-xl bg-surface p-5 sm:p-7"
      @submit.prevent="save"
    >
      <h3 class="text-xl font-bold">
        {{ editId ? "Edit address" : "New address" }}
      </h3>
      <BaseInput
        v-model="form.label"
        label="Label"
        placeholder="Home or Work"
        required
      /><BaseInput
        v-model="form.recipientName"
        label="Recipient name"
        autocomplete="name"
        required
      /><BaseInput
        v-model="form.phone"
        label="Mobile number"
        type="tel"
        autocomplete="tel"
        required
      />
      <div class="grid grid-cols-[9rem_minmax(0,1fr)] gap-4 [&>*]:min-w-0">
        <BaseInput
          v-model="form.address.streetNumber"
          label="Number"
          autocomplete="address-line1"
          required
        /><BaseInput
          v-model="form.address.streetName"
          label="Street"
          required
        />
      </div>
      <BaseInput
        v-model="form.address.complex"
        label="Unit or complex (optional)"
        autocomplete="address-line2"
      /><BaseInput
        v-model="form.address.suburb"
        label="Suburb"
        autocomplete="address-level3"
        required
      />
      <div class="grid grid-cols-[minmax(0,1fr)_12rem] gap-4 [&>*]:min-w-0">
        <BaseInput
          v-model="form.address.city"
          label="City or town"
          autocomplete="address-level2"
          required
        /><BaseInput
          v-model="form.address.postalCode"
          label="Postal code"
          autocomplete="postal-code"
          required
        />
      </div>
      <BaseInput
        v-model="form.address.province"
        label="Province"
        type="select"
        :options="provinces.map(([value, label]) => ({ value, label }))"
      /><label class="flex items-center gap-3 min-h-11"
        ><input
          v-model="form.isDefault"
          type="checkbox"
          :disabled="editId && items.find((a) => a.id === editId)?.isDefault"
        />Default address</label
      >
      <div class="flex flex-wrap gap-3">
        <BaseButton type="submit" :disabled="busy">{{
          busy ? "Saving…" : "Save address"
        }}</BaseButton
        ><BaseButton variant="outline" :disabled="busy" @click="editing = false"
          >Cancel</BaseButton
        >
      </div>
    </form>
  </section>
</template>
