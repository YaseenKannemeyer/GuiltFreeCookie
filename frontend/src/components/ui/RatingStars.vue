<script setup>
/**
 * RatingStars — gold star rating display (read-only) + numeric value.
 *
 * Usage:
 *   <RatingStars :rating="4.9" />               → ★★★★★ 4.9
 *   <RatingStars :rating="4.5" :show-value="false" size="sm" />
 *   <RatingStars :rating="0" editable @update:rating="onPick" />  → interactive
 */
import { computed } from 'vue'
import { StarIcon } from '@heroicons/vue/24/solid'

const props = defineProps({
  rating: { type: Number, default: 0 },
  max: { type: Number, default: 5 },
  size: { type: String, default: 'md' }, // sm | md | lg
  showValue: { type: Boolean, default: true },
  editable: { type: Boolean, default: false },
  color: { type: String, default: '#E8B33D' }, // gold
})

const emit = defineEmits(['update:rating'])

const sizeClass = computed(() => {
  switch (props.size) {
    case 'sm':
      return 'h-3.5 w-3.5'
    case 'lg':
      return 'h-7 w-7'
    case 'md':
    default:
      return 'h-5 w-5'
  }
})

const fullStars = computed(() => Math.floor(props.rating))
const hasHalf = computed(() => props.rating - fullStars.value >= 0.25 && props.rating - fullStars.value < 0.75)
const roundedUp = computed(() => props.rating - fullStars.value >= 0.75)

const effectiveFull = computed(() => (roundedUp.value ? fullStars.value + 1 : fullStars.value))
const emptyStars = computed(() => props.max - effectiveFull.value - (hasHalf.value ? 1 : 0))

const stars = computed(() => {
  const arr = []
  for (let i = 0; i < effectiveFull.value; i++) arr.push('full')
  if (hasHalf.value) arr.push('half')
  for (let i = 0; i < emptyStars.value; i++) arr.push('empty')
  return arr
})

const onPick = (i) => {
  if (!props.editable) return
  emit('update:rating', i + 1)
}
</script>

<template>
  <div class="inline-flex items-center gap-2">
    <div class="inline-flex items-center gap-0.5" :class="editable ? 'cursor-pointer' : ''">
      <template v-for="(star, i) in stars" :key="i">
        <!-- Full star -->
        <StarIcon
          v-if="star === 'full'"
          :class="[sizeClass, editable ? 'transition-transform hover:scale-110' : '']"
          :style="{ color }"
          @click="onPick(i)"
        />
        <!-- Half star (clip-path) -->
        <span v-else-if="star === 'half'" class="relative inline-block" @click="onPick(i)">
          <StarIcon :class="[sizeClass, 'text-cream-200/60']" />
          <span class="absolute inset-0 overflow-hidden" style="width: 50%">
            <StarIcon :class="[sizeClass]" :style="{ color }" />
          </span>
        </span>
        <!-- Empty star -->
        <StarIcon
          v-else
          :class="[sizeClass, editable ? 'hover:scale-110' : '']"
          class="text-cream-200/60"
          @click="onPick(i)"
        />
      </template>
    </div>
    <span
      v-if="showValue"
      :class="[
        'font-medium',
        size === 'sm' ? 'text-xs' : 'text-sm',
      ]"
      class="text-chocolate/70"
    >
      ({{ rating.toFixed(1) }})
    </span>
  </div>
</template>
