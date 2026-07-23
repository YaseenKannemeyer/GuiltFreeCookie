<script setup>
/**
 * TheFooter — premium footer with newsletter, link columns, and social icons.
 */
import { ref } from 'vue'
import { RouterLink } from 'vue-router'
import {
  EnvelopeIcon,
  PhoneIcon,
  MapPinIcon,
  ArrowRightIcon,
} from '@heroicons/vue/24/outline'

const email = ref('')
const subscribed = ref(false)

const onSubscribe = () => {
  if (!email.value || !/.+@.+\..+/.test(email.value)) return
  subscribed.value = true
  email.value = ''
  setTimeout(() => (subscribed.value = false), 4000)
}

const columns = [
  {
    title: 'Navigation',
    links: [
      { label: 'Home', to: '/' },
      { label: 'Menu', to: '/menu' },
      { label: 'About', to: '/about' },
      { label: 'Contact', to: '/contact' },
    ],
  },
  {
    title: 'Legal',
    links: [
      { label: 'Privacy Policy', to: '/contact' },
      { label: 'Terms of Use', to: '/contact' },
      { label: 'Cookie Policy', to: '/contact' },
      { label: 'Refunds', to: '/contact' },
    ],
  },
  {
    title: 'Services',
    links: [
      { label: 'Shop Cookies', to: '/menu' },
      { label: 'Order Ahead', to: '/menu' },
      { label: 'Bulk Orders', to: '/contact' },
      { label: 'Gift Cards', to: '/contact' },
    ],
  },
]

const socials = [
  {
    name: 'Instagram',
    href: '#',
    path: 'M12 2.2c3.2 0 3.6 0 4.8.07 1.17.05 1.97.24 2.67.5.72.28 1.33.66 1.94 1.27.61.61.99 1.22 1.27 1.94.26.7.45 1.5.5 2.67.06 1.2.07 1.6.07 4.8s0 3.6-.07 4.8c-.05 1.17-.24 1.97-.5 2.67a5.4 5.4 0 0 1-1.27 1.94 5.4 5.4 0 0 1-1.94 1.27c-.7.26-1.5.45-2.67.5-1.2.06-1.6.07-4.8.07s-3.6 0-4.8-.07c-1.17-.05-1.97-.24-2.67-.5a5.4 5.4 0 0 1-1.94-1.27 5.4 5.4 0 0 1-1.27-1.94c-.26-.7-.45-1.5-.5-2.67C2.2 15.6 2.2 15.2 2.2 12s0-3.6.07-4.8c.05-1.17.24-1.97.5-2.67.28-.72.66-1.33 1.27-1.94.61-.61 1.22-.99 1.94-1.27.7-.26 1.5-.45 2.67-.5C8.4 2.2 8.8 2.2 12 2.2zm0 1.8c-3.15 0-3.5 0-4.74.07-.9.04-1.38.2-1.7.32-.43.17-.74.37-1.06.69-.32.32-.52.63-.69 1.06-.13.32-.28.8-.32 1.7C3.2 8.5 3.2 8.85 3.2 12s0 3.5.07 4.74c.04.9.2 1.38.32 1.7.17.43.37.74.69 1.06.32.32.63.52 1.06.69.32.13.8.28 1.7.32 1.24.07 1.6.07 4.74.07s3.5 0 4.74-.07c.9-.04 1.38-.2 1.7-.32.43-.17.74-.37 1.06-.69.32-.32.52-.63.69-1.06.13-.32.28-.8.32-1.7.07-1.24.07-1.6.07-4.74s0-3.5-.07-4.74c-.04-.9-.2-1.38-.32-1.7a2.85 2.85 0 0 0-.69-1.06 2.85 2.85 0 0 0-1.06-.69c-.32-.13-.8-.28-1.7-.32C15.5 4 15.15 4 12 4zm0 3.06A4.94 4.94 0 1 0 12 17a4.94 4.94 0 0 0 0-9.88zm0 1.8a3.14 3.14 0 1 1 0 6.28 3.14 3.14 0 0 1 0-6.28zm5.15-.45a1.15 1.15 0 1 1-2.3 0 1.15 1.15 0 0 1 2.3 0z',
  },
  {
    name: 'X',
    href: '#',
    path: 'M18.244 2.25h3.308l-7.227 8.26 8.502 11.24h-6.66l-5.214-6.817L4.99 21.75H1.68l7.73-8.835L1.254 2.25H8.08l4.713 6.231zm-1.161 17.52h1.833L7.084 4.126H5.117z',
  },
  {
    name: 'Facebook',
    href: '#',
    path: 'M22 12a10 10 0 1 0-11.56 9.88v-6.99H7.9V12h2.54V9.8c0-2.5 1.49-3.89 3.78-3.89 1.09 0 2.24.2 2.24.2v2.46h-1.26c-1.24 0-1.63.77-1.63 1.56V12h2.78l-.44 2.89h-2.34v6.99A10 10 0 0 0 22 12z',
  },
  {
    name: 'LinkedIn',
    href: '#',
    path: 'M20.45 20.45h-3.56v-5.57c0-1.33-.03-3.04-1.85-3.04-1.85 0-2.13 1.45-2.13 2.94v5.67H9.35V9h3.42v1.56h.05c.48-.9 1.64-1.85 3.37-1.85 3.6 0 4.27 2.37 4.27 5.45v6.29zM5.34 7.43a2.07 2.07 0 1 1 0-4.14 2.07 2.07 0 0 1 0 4.14zM7.12 20.45H3.56V9h3.56v11.45zM22.22 0H1.77C.79 0 0 .77 0 1.72v20.56C0 23.23.79 24 1.77 24h20.45c.98 0 1.78-.77 1.78-1.72V1.72C24 .77 23.2 0 22.22 0z',
  },
]
</script>

<template>
  <footer class="bg-chocolate text-cream relative overflow-hidden texture-dark">
    <!-- Newsletter band -->
    <div class="container-magic py-16 border-b border-cream/10">
      <div class="grid lg:grid-cols-2 gap-8 items-center">
        <div>
          <h2 class="text-h2 text-cream font-medium leading-tight">
            Sweet news, <span class="text-primary">straight to your inbox</span>
          </h2>
          <p class="text-cream/70 text-card mt-3 max-w-md">
            New cookie drops, secret discount codes, and the occasional crumb of bakery gossip. No spam, just sweet stuff.
          </p>
        </div>
        <form class="flex flex-col sm:flex-row gap-3" @submit.prevent="onSubscribe">
          <div class="flex-1 relative">
            <EnvelopeIcon class="absolute left-5 top-1/2 -translate-y-1/2 h-5 w-5 text-cream/40" />
            <input
              v-model="email"
              type="email"
              required
              placeholder="your@email.com"
              class="w-full h-14 rounded-full pl-14 pr-5 bg-chocolate-400/60 border border-cream/15 text-cream placeholder:text-cream/40 focus:outline-none focus:ring-2 focus:ring-primary/50 transition-all"
            />
          </div>
          <button type="submit" class="btn-secondary h-14 px-7">
            <span v-if="!subscribed">Subscribe</span>
            <span v-else>Subscribed!</span>
            <ArrowRightIcon v-if="!subscribed" class="h-5 w-5" />
          </button>
        </form>
      </div>
    </div>

    <!-- Main footer grid -->
    <div class="container-magic py-16 grid grid-cols-1 md:grid-cols-2 lg:grid-cols-5 gap-10">
      <!-- Brand col -->
      <div class="lg:col-span-2">
        <RouterLink to="/" class="flex items-center gap-2 mb-5">
          <span class="text-2xl">🍪</span>
          <span class="font-bold text-2xl">Crumble<span class="text-primary">Magic</span></span>
        </RouterLink>
        <p class="text-cream/70 text-card max-w-sm mb-6">
          Handmade cookies, baked fresh daily with the finest ingredients. Craving cookies? We've got the crumble.
        </p>
        <ul class="space-y-3 text-cream/70">
          <li class="flex items-center gap-3">
            <span class="flex items-center justify-center h-9 w-9 rounded-full bg-cream/10 text-primary">
              <PhoneIcon class="h-4 w-4" />
            </span>
            0114 567 987 · 0112 345 678
          </li>
          <li class="flex items-center gap-3">
            <span class="flex items-center justify-center h-9 w-9 rounded-full bg-cream/10 text-primary">
              <EnvelopeIcon class="h-4 w-4" />
            </span>
            crumblemagic@gmail.com
          </li>
          <li class="flex items-center gap-3">
            <span class="flex items-center justify-center h-9 w-9 rounded-full bg-cream/10 text-primary">
              <MapPinIcon class="h-4 w-4" />
            </span>
            376/2, Main Road, Colombo 08
          </li>
        </ul>
      </div>

      <!-- Link columns -->
      <div v-for="col in columns" :key="col.title">
        <h4 class="font-semibold text-cream mb-4 text-card uppercase tracking-wider text-sm">
          {{ col.title }}
        </h4>
        <ul class="space-y-2.5">
          <li v-for="link in col.links" :key="link.label">
            <RouterLink
              :to="link.to"
              class="text-cream/70 hover:text-primary transition-colors text-card inline-flex items-center group"
            >
              <span class="w-0 group-hover:w-3 h-px bg-primary transition-all duration-300 mr-0 group-hover:mr-2" />
              {{ link.label }}
            </RouterLink>
          </li>
        </ul>
      </div>
    </div>

    <!-- Bottom bar -->
    <div class="border-t border-cream/10">
      <div class="container-magic py-6 flex flex-col md:flex-row items-center justify-between gap-4">
        <p class="text-cream/60 text-sm">
          © {{ new Date().getFullYear() }} CrumbleMagic. Baked with 🤎 in Colombo.
        </p>
        <div class="flex items-center gap-3">
          <a
            v-for="s in socials"
            :key="s.name"
            :href="s.href"
            :aria-label="s.name"
            class="flex items-center justify-center h-10 w-10 rounded-full bg-cream/10 text-cream/80 hover:bg-primary hover:text-chocolate transition-all hover:-translate-y-0.5"
          >
            <svg viewBox="0 0 24 24" class="h-5 w-5" fill="currentColor" aria-hidden="true">
              <path :d="s.path" />
            </svg>
          </a>
        </div>
      </div>
    </div>

    <!-- Decorative cookie watermark -->
    <div class="pointer-events-none absolute -bottom-32 -right-32 w-96 h-96 rounded-full bg-primary/5 blur-3xl" />
    <div class="pointer-events-none absolute -top-32 -left-32 w-80 h-80 rounded-full bg-primary/5 blur-3xl" />
  </footer>
</template>
