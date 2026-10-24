<script setup>
import { computed, reactive, ref, watch } from "vue";
import { useRoute, useRouter, RouterLink } from "vue-router";
import BaseInput from "../components/ui/BaseInput.vue";
import BaseButton from "../components/ui/BaseButton.vue";
import { api, errorMessage } from "../api/storeApi";
import { useCustomerAuth } from "../composables/useCustomerAuth";
const route = useRoute(),
  router = useRouter(),
  auth = useCustomerAuth();
const form = reactive({
  email: "",
  password: "",
  firstName: "",
  middleName: "",
  lastName: "",
  mobileNumber: "",
  newsletterOptIn: false,
});
const remember = ref(false),
  busy = ref(false),
  error = ref(""),
  sent = ref(false);
const mode = computed(() => route.meta.authMode);
const title = computed(
  () =>
    ({
      login: "Welcome back",
      register: "Join the cookie crew",
      forgot: "Forgot your password?",
      reset: "A fresh start",
    })[mode.value],
);
watch(mode, () => {
  error.value = "";
  sent.value = false;
  form.password = "";
});
async function submit() {
  if (busy.value) return;
  error.value = "";
  if (mode.value === "reset" && !route.query.token) {
    error.value =
      "This reset link is missing its token. Request a new link below.";
    return;
  }
  busy.value = true;
  try {
    if (mode.value === "forgot") {
      await api("post", "/auth/forgot-password", { email: form.email.trim() });
      sent.value = true;
      return;
    }
    const endpoint = {
      login: "login",
      register: "register",
      reset: "reset-password",
    }[mode.value];
    const body =
      mode.value === "reset"
        ? { token: route.query.token, newPassword: form.password }
        : mode.value === "register"
          ? { ...form, email: form.email.trim() }
          : { email: form.email.trim(), password: form.password };
    const session = await api("post", `/auth/${endpoint}`, body);
    auth.login(session.customer, session.token, remember.value);
    const redirect = route.query.redirect;
    router.replace(
      typeof redirect === "string" &&
        redirect.startsWith("/") &&
        !redirect.startsWith("//") &&
        !redirect.startsWith("/admin")
        ? redirect
        : "/account",
    );
  } catch (e) {
    error.value = errorMessage(e);
  } finally {
    busy.value = false;
  }
}
</script>
<template>
  <div class="pt-[var(--nav-h)]">
    <div class="container-magic py-16 md:py-24 max-w-2xl">
      <h1 class="font-display text-display">{{ title }}</h1>
      <p class="mt-4 text-chocolate/80">
        {{
          mode === "register"
            ? "Save your details, share your favourites and make yourself at home."
            : mode === "forgot"
              ? "Enter your email and we’ll send you a link to choose a new password."
              : mode === "reset"
                ? "Choose a password with at least 8 characters."
                : "Your next little treat starts here."
        }}
      </p>
      <p v-if="sent" role="status" class="mt-8 p-5 bg-surface rounded-xl">
        If an account uses that email, a reset link is on its way. Check your
        inbox and spam folder.
      </p>
      <form v-else class="mt-8 space-y-5" @submit.prevent="submit">
        <div v-if="mode === 'register'" class="grid sm:grid-cols-2 gap-5">
          <BaseInput
            v-model="form.firstName"
            label="First name"
            autocomplete="given-name"
            required
          /><BaseInput
            v-model="form.lastName"
            label="Last name"
            autocomplete="family-name"
            required
          /><BaseInput
            v-model="form.middleName"
            label="Middle name (optional)"
            autocomplete="additional-name"
          /><BaseInput
            v-model="form.mobileNumber"
            label="Mobile number (optional)"
            type="tel"
            autocomplete="tel"
          />
        </div>
        <BaseInput
          v-if="mode !== 'reset'"
          v-model="form.email"
          label="Email"
          type="email"
          autocomplete="email"
          required
        />
        <BaseInput
          v-if="mode !== 'forgot'"
          v-model="form.password"
          :label="mode === 'reset' ? 'New password' : 'Password'"
          type="password"
          :autocomplete="mode === 'login' ? 'current-password' : 'new-password'"
          required
        />
        <label v-if="mode === 'register'" class="flex gap-3 items-start py-2"
          ><input
            v-model="form.newsletterOptIn"
            type="checkbox"
            class="mt-1 accent-chocolate"
          /><span
            >Send me bakery news and offers. I can unsubscribe at any time.
            <RouterLink to="/privacy-policy" class="underline"
              >Privacy policy</RouterLink
            ></span
          ></label
        >
        <label v-if="mode !== 'forgot'" class="flex gap-3 items-center min-h-11"
          ><input
            v-model="remember"
            type="checkbox"
            class="accent-chocolate"
          />Remember me on this device</label
        >
        <p v-if="error" role="alert" class="text-red-800">{{ error }}</p>
        <BaseButton type="submit" :disabled="busy" block>{{
          busy
            ? "Please wait…"
            : mode === "forgot"
              ? "Send reset link"
              : mode === "register"
                ? "Create account"
                : mode === "reset"
                  ? "Save new password"
                  : "Log in"
        }}</BaseButton>
      </form>
      <nav
        class="mt-8 flex flex-wrap gap-x-6 gap-y-3"
        aria-label="Account options"
      >
        <RouterLink
          v-if="mode !== 'login'"
          to="/login"
          class="underline min-h-11"
          >Log in</RouterLink
        ><RouterLink
          v-if="mode === 'login'"
          to="/register"
          class="underline min-h-11"
          >Create an account</RouterLink
        ><RouterLink
          v-if="mode === 'login' || mode === 'reset'"
          to="/forgot-password"
          class="underline min-h-11"
          >{{
            mode === "reset" ? "Request a new reset link" : "Forgot password?"
          }}</RouterLink
        >
      </nav>
    </div>
  </div>
</template>
