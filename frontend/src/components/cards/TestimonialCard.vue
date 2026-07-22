<script setup>
/**
 * TestimonialCard — single customer review, used in the carousel grid.
 */
import RatingStars from '@/components/ui/RatingStars.vue'

defineProps({
  testimonial: { type: Object, required: true },
  active: { type: Boolean, default: false },
})
</script>

<template>
  <article
    :class="[
      'h-full flex flex-col rounded-4xl p-8 transition-all duration-500 ease-cookie',
      active
        ? 'bg-surface shadow-soft-lg scale-100 opacity-100'
        : 'bg-surface/70 shadow-soft scale-95 opacity-80',
    ]"
  >
    <!-- Quote mark -->
    <div class="text-primary text-6xl leading-none font-serif mb-2 select-none">"</div>

    <!-- Stars -->
    <RatingStars :rating="testimonial.rating" :show-value="false" size="md" class="mb-4" />

    <!-- Body -->
    <p class="text-chocolate/80 text-card leading-relaxed flex-1 mb-6">
      {{ testimonial.text }}
    </p>

    <!-- Author -->
    <div class="flex items-center gap-4 pt-5 border-t border-cream-300">
      <img
        :src="testimonial.avatar"
        :alt="testimonial.name"
        class="w-12 h-12 rounded-full object-cover ring-2 ring-cream-200"
        loading="lazy"
      />
      <div>
        <div class="font-semibold text-chocolate">{{ testimonial.name }}</div>
        <div class="text-sm text-chocolate/60">{{ testimonial.role }}</div>
      </div>
    </div>
  </article>
</template>
