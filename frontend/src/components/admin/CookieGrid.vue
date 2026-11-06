<script setup>
/**
 * CookieGrid — admin cookie list. A table from md up, stacked rows below.
 */
import {
  PencilSquareIcon,
  TrashIcon,
  ArchiveBoxIcon,
  ArrowUturnLeftIcon,
} from "@heroicons/vue/24/outline";
import { cookieImage, imageFallback } from "../../composables/useCatalog";
import { useFormat } from "../../composables/useFormat";
import { formatCategoryLabel } from "../../constants/cookieCategories";
import IconAction from "./IconAction.vue";

defineProps({
  cookies: {
    type: Array,
    required: true,
  },
});

const emit = defineEmits(["edit", "delete", "archive"]);
const { rand } = useFormat();
</script>

<template>
  <div class="card overflow-hidden">
    <table class="block xl:table xl:table-fixed w-full text-left">
      <thead class="hidden xl:table-header-group bg-cream-400/40 text-sm font-semibold text-chocolate/70">
        <tr>
          <th scope="col" class="py-3 pl-5 pr-3 font-semibold w-[38%]">Cookie</th>
          <th scope="col" class="py-3 px-3 font-semibold">Range</th>
          <th scope="col" class="py-3 px-3 font-semibold text-right">Price</th>
          <th scope="col" class="py-3 px-3 font-semibold">Status</th>
          <th scope="col" class="py-3 pl-3 pr-5 font-semibold text-right w-[17rem]"><span class="sr-only">Actions</span></th>
        </tr>
      </thead>
      <tbody class="block xl:table-row-group divide-y divide-chocolate/10">
        <tr
          v-for="cookie in cookies"
          :key="cookie.cookieId"
          class="grid grid-cols-[minmax(0,1fr)_auto] items-center gap-x-3 gap-y-1 p-4 xl:table-row xl:p-0 hover:bg-cream-400/25 transition-colors"
        >
          <!-- Cookie -->
          <td class="col-span-2 min-w-0 xl:py-3 xl:pl-5 xl:pr-3">
            <div class="flex items-center gap-3.5 min-w-0">
              <div
                class="h-14 w-14 shrink-0 rounded-xl bg-chocolate/[0.06] flex items-center justify-center overflow-hidden"
                :class="{ 'grayscale opacity-60': cookie.archived }"
              >
                <img :src="cookieImage(cookie)" @error="imageFallback(cookie)($event)" alt="" class="h-[85%] w-[85%] object-contain" loading="lazy" />
              </div>
              <div class="min-w-0">
                <p class="font-semibold text-chocolate truncate" :class="{ 'text-chocolate/60': cookie.archived }">
                  {{ cookie.description }}
                </p>
                <p class="text-sm text-chocolate/65 truncate max-w-[44.8rem]">{{ cookie.ingredients }}</p>
              </div>
            </div>
          </td>

          <!-- Range -->
          <td class="hidden xl:table-cell px-3 text-sm text-chocolate/85">
            {{ formatCategoryLabel(cookie.category) }}
          </td>

          <!-- Price -->
          <td class="row-start-2 col-start-2 text-right xl:px-3 font-semibold tabular whitespace-nowrap">
            {{ rand(cookie.price) }}
          </td>

          <!-- Status -->
          <td class="row-start-2 col-start-1 min-w-0 xl:col-span-1 xl:px-3 flex xl:table-cell flex-wrap items-center gap-2 text-sm">
            <span
              class="inline-flex items-center gap-1.5 rounded-full px-2.5 py-1 text-sm font-semibold whitespace-nowrap"
              :class="cookie.archived ? 'bg-chocolate/[0.08] text-chocolate/75' : 'bg-emerald-800/15 text-emerald-900'"
            >
              <span class="h-1.5 w-1.5 rounded-full" :class="cookie.archived ? 'bg-chocolate/40' : 'bg-emerald-600'" />
              {{ cookie.archived ? "Archived" : "On menu" }}
            </span>
            <span class="xl:hidden text-chocolate/65">{{ formatCategoryLabel(cookie.category) }}</span>
          </td>

          <!-- Actions -->
          <td class="row-start-3 col-span-2 xl:pl-3 xl:pr-5">
            <div class="flex flex-wrap xl:justify-end gap-0.5">
              <IconAction label="Edit" :icon="PencilSquareIcon" @click="emit('edit', cookie)" />
              <IconAction
                :label="cookie.archived ? 'Put back on menu' : 'Archive'"
                :icon="cookie.archived ? ArrowUturnLeftIcon : ArchiveBoxIcon"
                @click="emit('archive', cookie)"
              />
              <IconAction label="Delete" tone="danger" :icon="TrashIcon" @click="emit('delete', cookie)" />
            </div>
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</template>
