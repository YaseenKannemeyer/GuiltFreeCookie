<template>
  <div class="filter-section">
    <span class="filter-label">Category:</span>
    <div class="category-chips">
      <button
        class="chip"
        :class="{ active: selectedCategory === null }"
        @click="$emit('update:selectedCategory', null)"
      >
        All
      </button>
      <button
        v-for="cat in categories"
        :key="cat.value"
        class="chip"
        :class="{ active: selectedCategory === cat.value }"
        @click="$emit('update:selectedCategory', cat.value)"
      >
        {{ cat.label }}
      </button>
    </div>
  </div>
</template>

<script setup>
const categories = [
  { value: "LOW_SUGAR", label: "Low Sugar" },
  { value: "DIABETIC_FRIENDLY", label: "Diabetic Friendly" },
  { value: "HIGH_PROTEIN", label: "High Protein" },
  { value: "GLUTEN_FREE", label: "Gluten Free" },
  { value: "VEGAN", label: "Vegan" },
  { value: "CLASSIC", label: "Classic" },
];

defineProps({
  selectedCategory: {
    type: String,
    default: null,
  },
});

defineEmits(["update:selectedCategory"]);
</script>

<style scoped></style>
