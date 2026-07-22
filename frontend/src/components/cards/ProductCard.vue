<script setup>
/**
 * ProductCard — compact horizontal card used in cart summary and "you may also like"
 * sidebars. Lower visual weight than CookieCard.
 */
import { computed } from "vue";
import { useRouter } from "vue-router";
import { TrashIcon } from "@heroicons/vue/24/outline";
import RatingStars from "../../components/ui/RatingStars.vue";
import QuantitySelector from "../../components/ui/QuantitySelector.vue";
import { useFormat } from "../../composables/useFormat";

const props = defineProps({
  item: { type: Object, required: true },
  showControls: { type: Boolean, default: true },
  showRemove: { type: Boolean, default: true },
  variant: { type: String, default: "light" }, // light | dark
  compact: { type: Boolean, default: false },
});

const emit = defineEmits(["remove", "increase", "decrease"]);
const router = useRouter();
const { currency } = useFormat();

const isDark = computed(() => props.variant === "dark");

const goToProduct = () =>
  router.push({ name: "Products", params: { id: props.item.id } });
</script>

<template>
  <div
    :class="[
      'flex gap-4 rounded-3xl p-3 transition-all duration-300',
      isDark ? 'bg-chocolate-400/40' : 'bg-cream-50',
    ]"
  >
    <!-- Image -->
    <button
      type="button"
      :class="[
        'shrink-0 rounded-2xl overflow-hidden ring-2',
        compact ? 'w-16 h-16' : 'w-24 h-24',
        isDark ? 'ring-cream/10' : 'ring-cream-200',
      ]"
      @click="goToProduct"
    >
      <img
        :src="item.image"
        :alt="item.name"
        class="w-full h-full object-cover"
        loading="lazy"
      />
    </button>

    <!-- Body -->
    <div class="flex-1 flex flex-col justify-between min-w-0">
      <div>
        <div class="flex items-start justify-between gap-2">
          <h4
            :class="[
              'font-semibold leading-snug truncate',
              compact ? 'text-card' : 'text-lg',
              isDark ? 'text-cream' : 'text-chocolate',
            ]"
          >
            {{ item.name }}
          </h4>
          <button
            v-if="showRemove"
            type="button"
            :class="[
              'shrink-0 transition-colors p-1 rounded-full',
              isDark
                ? 'text-cream/40 hover:text-red-300'
                : 'text-chocolate/40 hover:text-red-500',
            ]"
            aria-label="Remove from cart"
          >
            <TrashIcon class="h-4 w-4" />
          </button>
        </div>
        <p
          v-if="!compact"
          :class="['text-sm', isDark ? 'text-cream/60' : 'text-chocolate/60']"
        >
          {{ item.category }}
        </p>
        <RatingStars
          v-if="item.rating"
          :rating="item.rating"
          :show-value="false"
          size="sm"
        />
      </div>

      <div class="flex items-center justify-between gap-3 mt-2">
        <span
          :class="['font-bold', isDark ? 'text-primary-200' : 'text-chocolate']"
        >
          {{ currency(item.price * (item.quantity || 1)) }}
        </span>
        <QuantitySelector
          v-if="showControls"
          :model-value="item.quantity"
          size="sm"
          :variant="variant"
          @update:model-value="
            (v) =>
              v > item.quantity
                ? emit('increase', item.id)
                : emit('decrease', item.id)
          "
        />
      </div>
    </div>
  </div>
</template>
