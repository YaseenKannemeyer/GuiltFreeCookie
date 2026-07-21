<template>
  <div class="product-card" @click="goToDetail">
    <div class="product-image">
      <span :class="`category-badge badge-${categoryClass}`">
        {{ formatCategory(cookie.category) }}
      </span>
    </div>
    <div class="product-info">
      <h3 class="product-name">{{ cookie.description }}</h3>
      <p class="product-description">{{ cookie.ingredients }}</p>
      <div class="product-footer">
        <div class="product-price">
          <span>R</span>{{ cookie.price?.toFixed(2) }}
        </div>
        <button class="view-btn">View Details</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from "vue"; // <-- THIS WAS MISSING
import { useRouter } from "vue-router";

const props = defineProps({
  cookie: {
    type: Object,
    required: true,
  },
});

const router = useRouter();

const categoryClass = computed(() => {
  return props.cookie.category?.toLowerCase().replace("_", "-") || "classic";
});

const formatCategory = (category) => {
  if (!category) return "";
  return category.replace(/_/g, " ");
};

const goToDetail = () => {
  router.push(`/product/${props.cookie.cookieId}`);
};
</script>
