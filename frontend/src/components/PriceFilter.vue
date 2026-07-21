<template>
  <div class="filter-section">
    <span class="filter-label">Price Range:</span>
    <div class="price-inputs">
      <span>R</span>
      <input
        type="number"
        class="price-input"
        :value="minPrice"
        @input="$emit('update:minPrice', Number($event.target.value) || 0)"
        placeholder="Min"
        min="0"
      />
      <span>to</span>
      <input
        type="number"
        class="price-input"
        :value="maxPrice"
        @input="$emit('update:maxPrice', Number($event.target.value) || 9999)"
        placeholder="Max"
        min="0"
      />
    </div>
  </div>
</template>

<script setup>
defineProps({
  minPrice: {
    type: Number,
    default: 0,
  },
  maxPrice: {
    type: Number,
    default: 9999,
  },
});

defineEmits(["update:minPrice", "update:maxPrice"]);
</script>

<style scoped>
.price-inputs span {
  color: var(--text-light);
  font-weight: 500;
}
</style>
