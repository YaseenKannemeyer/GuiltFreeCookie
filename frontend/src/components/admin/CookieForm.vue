<script setup>
import { reactive, watch } from "vue";

const props = defineProps({
  cookie: {
    type: Object,
    default: null,
  },
});

const emit = defineEmits(["save", "close"]);

const form = reactive({
  cookieId: null,
  category: "",
  description: "",
  ingredients: "",
  allergies: "",
  price: 0,
});

watch(
  () => props.cookie,
  (cookie) => {
    if (cookie) {
      form.cookieId = cookie.cookieId;
      form.category = cookie.category;
      form.description = cookie.description;
      form.ingredients = cookie.ingredients;
      form.allergies = cookie.allergies;
      form.price = cookie.price;
    } else {
      form.cookieId = null;
      form.category = "";
      form.description = "";
      form.ingredients = "";
      form.allergies = "";
      form.price = 0;
    }
  },
  { immediate: true },
);

const submit = () => {
  emit("save", { ...form });
  emit("close");
};
</script>

<template>
  <div class="fixed inset-0 bg-black/50 flex justify-center items-center">
    <div class="bg-white p-6 rounded-xl w-[500px]">
      <h2 class="text-xl font-bold mb-4">
        {{ cookie ? "Edit Cookie" : "Add Cookie" }}
      </h2>

      <input
        v-model="form.description"
        placeholder="Description"
        class="border w-full mb-3 p-2"
      />

      <input
        v-model="form.ingredients"
        placeholder="Ingredients"
        class="border w-full mb-3 p-2"
      />

      <input
        v-model="form.allergies"
        placeholder="Allergies"
        class="border w-full mb-3 p-2"
      />

      <input
        v-model.number="form.price"
        type="number"
        placeholder="Price"
        class="border w-full mb-3 p-2"
      />

      <select v-model="form.category" class="border w-full mb-4 p-2">
        <option>LOW_SUGAR</option>
        <option>VEGAN</option>
        <option>HIGH_PROTEIN</option>
        <option>GLUTEN_FREE</option>
        <option>CLASSIC</option>
      </select>

      <div class="flex justify-end gap-3">
        <button @click="emit('close')">Cancel</button>
        <button
          class="bg-primary text-white px-4 py-2 rounded"
          :disabled="!form.description || !form.category || form.price <= 0"
          @click="submit"
        >
          Save
        </button>
      </div>
    </div>
  </div>
</template>
