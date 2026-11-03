<script setup>
/**
 * CartLine — one pack size in the cart: what it is, its allergens, quantity and price,
 * plus a one-tap fix when stock changed after it was added.
 */
import { computed } from "vue";
import { RouterLink } from "vue-router";
import { ExclamationTriangleIcon } from "@heroicons/vue/20/solid";
import QuantitySelector from "../ui/QuantitySelector.vue";
import { cookieImage, imageFallback } from "../../composables/useCatalog";
import { useFormat } from "../../composables/useFormat";

const props = defineProps({
  line: { type: Object, required: true },
  busy: { type: Boolean, default: false },
  editable: { type: Boolean, default: true },
});
const emit = defineEmits(["quantity", "remove"]);
const { rand } = useFormat();

const image = computed(() => cookieImage({ image: props.line.image, cookieId: props.line.cookieId }));
const onError = imageFallback({ cookieId: props.line.cookieId });
const packText = computed(() =>
  props.line.unitsPerPack > 1 ? `${props.line.variantLabel} · ${props.line.unitsPerPack} cookies` : props.line.variantLabel,
);
</script>

<template>
  <li class="py-5 flex gap-4">
    <RouterLink
      :to="line.slug ? { name: 'ProductDetail', params: { id: line.slug } } : { name: 'Products' }"
      class="shrink-0 h-20 w-20 rounded-2xl bg-tan/60 grid place-items-center overflow-hidden"
      tabindex="-1"
      aria-hidden="true"
    >
      <img :src="image" alt="" class="h-[85%] w-[85%] object-contain drop-shadow-cookie-sm" loading="lazy" @error="onError" />
    </RouterLink>
    <div class="min-w-0 flex-1">
      <div class="flex items-start justify-between gap-3">
        <div class="min-w-0">
          <RouterLink
            v-if="line.slug"
            :to="{ name: 'ProductDetail', params: { id: line.slug } }"
            class="font-bold hover:underline decoration-primary decoration-2"
            >{{ line.name }}</RouterLink
          >
          <span v-else class="font-bold">{{ line.name }}</span>
          <p class="text-sm text-chocolate/75">{{ packText }}</p>
        </div>
        <span class="font-bold tabular whitespace-nowrap">{{ rand(line.lineTotal) }}</span>
      </div>
      <p v-if="line.allergens" class="mt-1 text-sm">
        <span class="font-bold">Allergens:</span> {{ line.allergens }}
      </p>

      <p
        v-if="line.issue"
        role="alert"
        class="mt-3 flex items-start gap-2 rounded-xl bg-red-800/[0.08] px-3 py-2 text-sm text-red-900"
      >
        <ExclamationTriangleIcon class="h-5 w-5 shrink-0" aria-hidden="true" />
        <span v-if="line.issue === 'UNAVAILABLE'">This isn't available any more. Remove it to continue.</span>
        <span v-else-if="line.maxQuantity > 0"
          >Only {{ line.maxQuantity }} {{ line.maxQuantity === 1 ? "pack" : "packs" }} left now.
          <button
            v-if="editable"
            type="button"
            class="font-bold underline"
            :disabled="busy"
            @click="emit('quantity', line.maxQuantity)"
          >
            Change to {{ line.maxQuantity }}
          </button></span
        >
        <span v-else>This just sold out. Remove it to continue.</span>
      </p>

      <div v-if="editable" class="mt-3 flex items-center justify-between gap-3">
        <QuantitySelector
          v-if="line.issue !== 'UNAVAILABLE'"
          :label="`Quantity of ${line.name}`"
          :model-value="line.quantity"
          :min="1"
          :max="Math.max(line.quantity, line.maxQuantity)"
          size="sm"
          @update:model-value="(q) => emit('quantity', q)"
        />
        <span v-else />
        <button
          type="button"
          class="text-sm font-bold underline decoration-chocolate/30 underline-offset-4 hover:decoration-chocolate min-h-11 px-1"
          :disabled="busy"
          @click="emit('remove')"
        >
          Remove<span class="sr-only"> {{ line.name }}</span>
        </button>
      </div>
      <p v-else class="mt-1 text-sm tabular">× {{ line.quantity }}</p>
    </div>
  </li>
</template>
