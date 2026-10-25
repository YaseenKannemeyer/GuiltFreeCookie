<script setup>
import AccountOrders from "../components/account/AccountOrders.vue";
import AddressBook from "../components/account/AddressBook.vue";
import { reactive, ref, onMounted } from "vue";
import { useRouter } from "vue-router";
import { api, errorMessage } from "../api/storeApi";
import { useCustomerAuth } from "../composables/useCustomerAuth";
import BaseInput from "../components/ui/BaseInput.vue";
import BaseButton from "../components/ui/BaseButton.vue";
const auth = useCustomerAuth(),
  router = useRouter();
const form = reactive({
    firstName: "",
    middleName: "",
    lastName: "",
    mobileNumber: "",
  }),
  passwords = reactive({ currentPassword: "", newPassword: "" });
const busy = ref(false),
  loading = ref(true),
  error = ref(""),
  success = ref("");
async function load() {
  loading.value = true;
  try {
    const c = await api("get", "/me");
    auth.updateSession(c);
    Object.assign(form, c.name, { mobileNumber: c.mobileNumber || "" });
    error.value = "";
  } catch (e) {
    error.value = errorMessage(e);
  } finally {
    loading.value = false;
  }
}
onMounted(load);
async function save(password = false) {
  busy.value = true;
  error.value = "";
  success.value = "";
  try {
    const r = await api(
      "put",
      password ? "/me/password" : "/me",
      password ? passwords : form,
    );
    if (password) {
      auth.updateSession(r.customer, r.token);
      passwords.currentPassword = "";
      passwords.newPassword = "";
    } else auth.updateSession(r);
    success.value = password
      ? "Password updated. Your other sessions have ended."
      : "Your profile is saved.";
  } catch (e) {
    error.value = errorMessage(e);
  } finally {
    busy.value = false;
  }
}
function logout() {
  auth.logout();
  router.push("/login");
}
</script>
<template>
  <div class="pt-[var(--nav-h)]">
    <div class="container-magic py-16 max-w-6xl">
      <div class="flex flex-wrap justify-between gap-4 items-center">
        <h1 class="font-display text-display">My account</h1>
        <BaseButton variant="outline" @click="logout">Log out</BaseButton>
      </div>
      <p class="mt-3">{{ auth.customer.value?.customerEmail }}</p>
      <div class="mt-8 lg:grid lg:grid-cols-[18rem_minmax(0,1fr)] lg:gap-10">
        <nav
          aria-label="Account sections"
          class="flex flex-wrap gap-x-6 gap-y-2 border-b border-chocolate/15 pb-5 mb-7 lg:sticky lg:top-32 lg:self-start lg:flex-col lg:border-0"
        >
          <a
            v-for="[id, label] in [
              ['orders', 'Orders'],
              ['addresses', 'Addresses'],
              ['details', 'Your details'],
              ['password', 'Password'],
            ]"
            :key="id"
            :href="`#${id}`"
            class="inline-flex items-center min-h-11 font-bold underline decoration-primary underline-offset-4"
            >{{ label }}</a
          >
        </nav>
        <div class="min-w-0">
          <AccountOrders />
          <div class="my-10 border-t border-chocolate/20 pt-10">
            <AddressBook />
          </div>
          <p v-if="error" role="alert" class="mt-6 text-red-800">
            {{ error }}
            <button v-if="loading === false" class="underline" @click="load">
              Reload profile
            </button>
          </p>
          <p v-if="success" role="status" class="mt-6">{{ success }}</p>
          <p v-if="loading" class="mt-8" role="status">Loading your profile…</p>
          <template v-else
            ><form
              id="details"
              class="scroll-mt-32 mt-10 space-y-5"
              @submit.prevent="save()"
            >
              <h2 class="text-2xl font-extrabold">Your details</h2>
              <div class="grid sm:grid-cols-2 gap-5">
                <BaseInput
                  v-for="field in [
                    { key: 'firstName', label: 'First name' },
                    { key: 'lastName', label: 'Last name' },
                    { key: 'middleName', label: 'Middle name (optional)' },
                    { key: 'mobileNumber', label: 'Mobile number (optional)' },
                  ]"
                  :key="field.key"
                  v-model="form[field.key]"
                  :label="field.label"
                  :required="
                    field.key === 'firstName' || field.key === 'lastName'
                  "
                />
              </div>
              <BaseButton type="submit" :disabled="busy"
                >Save profile</BaseButton
              >
            </form>
            <form
              id="password"
              class="scroll-mt-32 mt-12 pt-10 border-t border-chocolate/20 space-y-5"
              @submit.prevent="save(true)"
            >
              <h2 class="text-2xl font-extrabold">Change password</h2>
              <BaseInput
                v-model="passwords.currentPassword"
                label="Current password"
                type="password"
                autocomplete="current-password"
                required
              /><BaseInput
                v-model="passwords.newPassword"
                label="New password"
                type="password"
                autocomplete="new-password"
                help="At least 8 characters"
                required
              /><BaseButton type="submit" :disabled="busy"
                >Update password</BaseButton
              >
            </form></template
          >
        </div>
      </div>
    </div>
  </div>
</template>
