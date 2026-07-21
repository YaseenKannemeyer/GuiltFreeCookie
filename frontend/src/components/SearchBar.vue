<template>
  <div class="search-section">
    <div class="search-input-wrapper">
      <input
        type="text"
        class="search-input"
        :value="modelValue"
        @input="$emit('update:modelValue', $event.target.value)"
        placeholder="Search cookies by name, ingredient, or allergy info..."
      />
      <button
        v-if="modelValue"
        class="clear-search"
        @click="$emit('update:modelValue', '')"
      >
        ✕
      </button>
    </div>
  </div>
</template>

<script setup>
defineProps({
  modelValue: {
    type: String,
    default: "",
  },
});

defineEmits(["update:modelValue"]);
</script>

<style scoped>
.clear-search {
  position: absolute;
  right: 15px;
  top: 50%;
  transform: translateY(-50%);
  background: none;
  border: none;
  font-size: 1.2rem;
  cursor: pointer;
  color: var(--text-light);
  padding: 5px;
}

.search-input-wrapper {
  position: relative;
}
</style>
