<script setup>
import {ref, reactive} from 'vue';
import {useAuthStore} from '../stores/authStore'
import {useRouter} from 'vue-router'
import TheFooter from "../layout/TheFooter.vue";

const authStore = useAuthStore();
const router = useRouter();

const isLogin = ref(true);
const isLoading = ref(false);

const form = reactive({
  customerEmail: '',
  password: '',
  mobileNumber: '',
  name: {
    firstName: '',
    middleName: '',
    lastName: '',
  }
})

const handleAuth = async () => {
  isLoading.value = true;
  try {
    if (isLogin.value) {
      await authStore.login(form.customerEmail, form.password);
    } else {
      await authStore.register(form);
    }
    router.push('/dashboard');
  } catch (error) {
    alert(error.response?.data?.message || "Please enter valid credentials");
  } finally {
    isLoading.value = false;
  }
};

const toggleMode = () => {
  isLogin.value = !isLogin.value;
}
</script>

<template>
  <main class="auth-page">
    <div class="texture-overlay texture-dark opacity-40"></div>

    <section class="auth-container reveal is-visible">
      <div class="auth-glass-card">
        <h1 class="auth-logo luckiest-guy">
          Crumble<span class="text-white/80">Magic</span>
        </h1>

        <div class="auth-body">
          <h2 class="auth-title luckiest-guy">{{ isLogin ? 'Sign In' : 'Join Us' }}</h2>
          <button type="button" class="btn-google">
            <img src="https://upload.wikimedia.org/wikipedia/commons/c/c1/Google_Logo.svg" alt="Google" class="w-5 h-5" />
            <span>Sign in with Google</span>
          </button>

          <div class="divider">
            <span>Or</span>
          </div>

          <form @submit.prevent="handleAuth" class="space-y-4">
            <div class="form-group">
              <input
                  v-model="form.customerEmail"
                  type="email"
                  placeholder="Email"
                  class="field"
                  required
              />
            </div>

            <div v-if="!isLogin" class="space-y-4 animate-in fade-in slide-in-from-top-2 duration-300">
              <div class="grid grid-cols-2 gap-4">
                <input v-model="form.name.firstName" placeholder="First Name" class="field" required />
                <input v-model="form.name.lastName" placeholder="Last Name" class="field" required />
              </div>

              <input
                  v-model="form.name.middleName"
                  placeholder="Middle Name (Optional)"
                  class="field"
              />

              <input v-model="form.mobileNumber" placeholder="Mobile Number" class="field" required />
            </div>
            <div class="form-group">
              <input
                  v-model="form.password"
                  type="password"
                  placeholder="Password"
                  class="field"
                  required
              />
            </div>

            <button
                type="submit"
                class="btn-primary w-full py-4 text-lg shadow-xl"
                :disabled="isLoading"
            >
              {{ isLoading ? 'Baking...' : 'Continue' }}
            </button>
          </form>

          <button @click="toggleMode" class="toggle-link">
            {{ isLogin ? "New here? Create an account" : "Have an account? Sign in" }}
          </button>
        </div>
      </div>
    </section>

    <TheFooter />
  </main>
</template>

<style scoped>
.auth-page {
  @apply relative min-h-screen flex flex-col items-center justify-center overflow-hidden;
  background-image: url('https://images.unsplash.com/photo-1499636136210-6f4ee915583e?auto=format&fit=crop&q=80&w=2000');
  background-size: cover;
  background-position: center;
}

.texture-overlay {
  @apply absolute inset-0 pointer-events-none;
}

.auth-container {
  @apply  relative z-20 w-full max-w-lg px-6 flex-grow flex items-center justify-center;
}

.auth-glass-card {
  @apply w-full bg-white/10 backdrop-blur-2xl border border-white/20 rounded-[40px] p-8 md:p-12 shadow-2xl;
}

.auth-logo {
  @apply text-5xl md:text-6xl text-white text-center mb-8 tracking-tighter;
  text-shadow: 0 4px 10px rgba(46, 14, 11, 0.2);
}

.auth-title{
  @apply text-xl text-chocolate mb-6 opacity-90;
}

.btn-google{
  @apply w-full flex items-center justify-center gap-4 bg-chocolate text-cream py-4 rounded-2xl
  font-medium transition-transform active:scale-95 mb-6 shadow-lg;
}

.divider {
  @apply flex items-center gap-4 my-6 text-chocolate/40 font-medium;
}

.divider::before, .divider::after {
  content: ""; @apply flex-1 h-[1px] bg-chocolate/10;
}

.toggle-link{
  @apply w-full mt-6 text-chocolate/70 text-sm font-medium hover:text-chocolate transition-colors underline decoration-chocolate/20;
}

.field{
  @apply bg-[#e2d1bc] border-none text-chocolate placeholder:text-chocolate/50;
}
</style>