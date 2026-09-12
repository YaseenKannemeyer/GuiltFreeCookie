<script setup>
import { ref } from "vue";
import { RouterLink, useRouter, useRoute } from "vue-router";
import { LockClosedIcon, ArrowLeftIcon } from "@heroicons/vue/24/outline";
import adminApi from "../api/adminApi";
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
  error.value = "";
  loading.value = true;
  try {
    const { admin, token } = await adminApi.login(email.value, password.value);
    login(admin, token, remember.value);
    router.push(route.query.redirect || { name: "AdminDashboard" });
  } catch (err) {
    error.value =
      err.response?.status === 401
        ? "Invalid email or password."
        : "Couldn't reach the server. Please try again.";
  } finally {
    loading.value = false;
  }
};
</script>

<template>
  <div class="min-h-screen bg-chocolate-gradient flex items-center justify-center px-4">
    <div class="w-full max-w-md bg-background rounded-4xl shadow-soft-xl p-10">
      <div class="flex flex-col items-center mb-8">
        <div class="h-14 w-14 rounded-full bg-primary/15 flex items-center justify-center mb-4">
          <LockClosedIcon class="h-6 w-6 text-primary-700" />
        </div>
        <h1 class="text-3xl luckiest-guy text-chocolate">
          GuiltFree<span class="text-primary">Cookie</span>
        </h1>
        <p class="text-chocolate/50 text-sm mt-1 uppercase tracking-widest font-semibold">
          Admin Login
        </p>
      </div>

      <form class="space-y-4" @submit.prevent="submit">
        <BaseInput
          v-model="email"
          type="email"
          label="Email"
          placeholder="you@guiltfreecookie.com"
          required
          autocomplete="username"
        />
        <BaseInput
          v-model="password"
          type="password"
          label="Password"
          placeholder="••••••••"
          required
          autocomplete="current-password"
        />

        <label class="flex items-center gap-2 text-sm text-chocolate/70 cursor-pointer select-none">
          <input
            v-model="remember"
            type="checkbox"
            class="h-4 w-4 rounded border-cream-400 text-primary focus:ring-primary/40 focus:ring-offset-0"
          />
          Remember me
        </label>

        <p v-if="error" class="text-sm text-red-600">{{ error }}</p>

        <BaseButton
          type="submit"
          variant="primary"
          block
          :disabled="!email || !password"
          :loading="loading"
        >
          Log In
        </BaseButton>
      </form>

      <RouterLink
        :to="{ name: 'Home' }"
        class="mt-6 flex items-center justify-center gap-1.5 text-sm text-chocolate/50 hover:text-chocolate transition-colors"
      >
        <ArrowLeftIcon class="h-4 w-4" />
        Back to Store
      </RouterLink>
    </div>
  </div>
</template>
