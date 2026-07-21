<script setup>
/**
 * Breadcrumb — accessible trail with router-link support.
 *
 * Usage:
 *   <Breadcrumb :items="[{label:'Home', to:'/'}, {label:'Menu', to:'/menu'}, {label:'Caramel'}]" />
 */
import { ChevronRightIcon, HomeIcon } from '@heroicons/vue/20/solid'
import { RouterLink } from 'vue-router'

defineProps({
  items: { type: Array, default: () => [] }, // [{label, to?}]
  showHome: { type: Boolean, default: true },
})
</script>

<template>
  <nav aria-label="Breadcrumb" class="w-full">
    <ol class="flex items-center flex-wrap gap-1 text-sm">
      <li v-if="showHome" class="flex items-center">
        <RouterLink
          to="/"
          class="text-chocolate/60 hover:text-primary transition-colors inline-flex items-center"
        >
          <HomeIcon class="h-4 w-4" />
        </RouterLink>
        <ChevronRightIcon class="h-4 w-4 text-chocolate/30 mx-1" />
      </li>
      <li v-for="(item, i) in items" :key="i" class="flex items-center">
        <RouterLink
          v-if="item.to && i < items.length - 1"
          :to="item.to"
          class="text-chocolate/60 hover:text-primary transition-colors"
        >
          {{ item.label }}
        </RouterLink>
        <span v-else class="text-chocolate font-medium" aria-current="page">
          {{ item.label }}
        </span>
        <ChevronRightIcon
          v-if="i < items.length - 1"
          class="h-4 w-4 text-chocolate/30 mx-1"
        />
      </li>
    </ol>
  </nav>
</template>
