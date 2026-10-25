<script setup>
import { ref } from "vue";
import { RouterLink, useRouter, useRoute } from "vue-router";
import { ArrowLeftIcon } from "@heroicons/vue/20/solid";
import adminApi from "../api/adminApi";
import { cleanEmail } from "../utils/normalize";
import { useAdminAuth } from "../composables/useAdminAuth";
import BaseInput from "../components/ui/BaseInput.vue";
import BaseButton from "../components/ui/BaseButton.vue";

const router = useRouter();
const route = useRoute();
const { login } = useAdminAuth();

const email = ref("");
const password = ref("");
const remember = ref(false);
const error = ref("");
const loading = ref(false);

const submit = async () => {
  if (!email.value || !password.value) return;
  error.value = "";
  loading.value = true;
  try {
    const { admin, token } = await adminApi.login(cleanEmail(email.value), password.value);
    login(admin, token, remember.value);
    router.push(route.query.redirect || { name: "AdminOverview" });
  } catch (err) {
    error.value =
      err.response?.status === 401
        ? "That email and password don't match. Check them and try again."
        : "We couldn't reach the server. Check your connection and try again.";
  } finally {
    loading.value = false;
  }
};
</script>

<template>
  <div class="min-h-[100dvh] grid lg:grid-cols-[1fr_1.1fr] bg-background">
    <!-- Brand panel -->
    <div class="relative hidden lg:flex flex-col justify-between overflow-hidden bg-chocolate p-12 text-cream-100">
      <div
        class="absolute inset-0 bg-[radial-gradient(55%_45%_at_50%_60%,rgba(197,159,113,0.3),transparent_70%)]"
        aria-hidden="true"
      />
      <RouterLink :to="{ name: 'Home' }" class="relative wordmark text-cream-50">
        GuiltFree<span class="text-primary">Cookie</span>
      </RouterLink>
      <img
        src="/img/cookieHome.webp"
        alt=""
        class="relative mx-auto w-[78%] max-w-md drop-shadow-cookie animate-cookie-in"
      />
      <p class="relative text-cream-100/70 max-w-xs">
        Manage the menu, the team and the orders from one place.
      </p>
    </div>

    <!-- Form -->
    <div class="flex flex-col justify-center px-6 py-12 sm:px-12">
      <div class="w-full max-w-sm mx-auto">
        <RouterLink :to="{ name: 'Home' }" class="lg:hidden wordmark text-chocolate">
          GuiltFree<span class="text-primary-600">Cookie</span>
        </RouterLink>

        <h1 class="mt-8 lg:mt-0 font-sans text-base font-semibold tracking-[-0.025em] leading-tight">
          Admin log in
        </h1>
        <p class="mt-1.5 text-chocolate/70">Use the email and password your team gave you.</p>

        <form class="mt-8 space-y-4" @submit.prevent="submit" novalidate>
          <BaseInput
            v-model="email"
            type="email"
            label="Email"
            placeholder="you@guiltfreecookie.co.za"
            required
            autocomplete="username"
          />
          <BaseInput
            v-model="password"
            type="password"
            label="Password"
            required
            autocomplete="current-password"
          />

          <label class="flex items-center min-h-11 gap-2.5 text-sm text-chocolate/85 cursor-pointer select-none w-fit">
            <input
              v-model="remember"
              type="checkbox"
              class="h-[18px] w-[18px] rounded border-cream-500"
            />
            Keep me logged in on this device
          </label>

          <p
            v-if="error"
            class="rounded-xl bg-red-700/10 border border-red-700/30 px-4 py-3 text-sm font-semibold text-red-900"
            role="alert"
          >
            {{ error }}
          </p>

          <BaseButton
            type="submit"
            variant="primary"
            block
            class="!mt-6 h-12"
            :disabled="!email || !password"
            :loading="loading"
          >
            {{ loading ? "Logging in" : "Log in" }}
          </BaseButton>
        </form>

        <RouterLink
          :to="{ name: 'Home' }"
          class="mt-6 inline-flex items-center min-h-11 gap-1.5 text-sm font-medium text-chocolate/70 hover:text-chocolate transition-colors"
        >
          <ArrowLeftIcon class="h-4 w-4" />
          Back to the store
        </RouterLink>
      </div>
    </div>
  </div>
</template>
