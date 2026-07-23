<script setup>
/**
 * BaseInput — labelled form field with error + help text.
 * Supports text, email, tel, password, number, textarea, select.
 */
import { computed, useSlots } from "vue";
import { ExclamationCircleIcon } from "@heroicons/vue/24/outline";

const props = defineProps({
  modelValue: { type: [String, Number], default: "" },
  label: { type: String, default: "" },
  type: { type: String, default: "text" },
  placeholder: { type: String, default: "" },
  id: {
    type: String,
    default: () => `field-${Math.random().toString(36).slice(2, 8)}`,
  },
  name: { type: String, default: "" },
  required: { type: Boolean, default: false },
  disabled: { type: Boolean, default: false },
  error: { type: String, default: "" },
  help: { type: String, default: "" },
  autocomplete: { type: String, default: "off" },
  // For select
  options: { type: Array, default: () => [] }, // [{value,label}]
  // For textarea
  rows: { type: Number, default: 4 },
  // Light (cream) or dark (chocolate) variant
  variant: { type: String, default: "light" }, // light | dark
});

const emit = defineEmits(["update:modelValue", "blur", "focus"]);

const isTextarea = computed(() => props.type === "textarea");
const isSelect = computed(() => props.type === "select");
const fieldClass = computed(() =>
  props.variant === "dark" ? "field-dark" : "field",
);
const slots = useSlots();

const onInput = (e) => {
  const v = e.target.value;
  emit("update:modelValue", props.type === "number" ? Number(v) : v);
};
</script>

<template>
  <div class="w-full">
    <label
      v-if="label"
      :for="id"
      :class="[
        'block text-card font-medium mb-2',
        variant === 'dark' ? 'text-cream/80' : 'text-chocolate/80',
      ]"
    >
      {{ label }}
      <span v-if="required" class="text-primary">*</span>
    </label>

    <!-- Select -->
    <select
      v-if="isSelect"
      :id="id"
      :name="name || id"
      :value="modelValue"
      :disabled="disabled"
      :class="[fieldClass, error ? 'ring-2 ring-red-400 border-red-300' : '']"
      @input="onInput"
      @blur="$emit('blur', $event)"
      @focus="$emit('focus', $event)"
    >
      <option v-for="opt in options" :key="opt.value" :value="opt.value">
        {{ opt.label }}
      </option>
    </select>

    <!-- Textarea -->
    <textarea
      v-else-if="isTextarea"
      :id="id"
      :name="name || id"
      :value="modelValue"
      :placeholder="placeholder"
      :rows="rows"
      :disabled="disabled"
      :autocomplete="autocomplete"
      :class="[
        fieldClass,
        'resize-none',
        error ? 'ring-2 ring-red-400 border-red-300' : '',
      ]"
      @input="onInput"
      @blur="$emit('blur', $event)"
      @focus="$emit('focus', $event)"
    />

    <!-- Generic input -->
    <input
      v-else
      :id="id"
      :name="name || id"
      :type="type"
      :value="modelValue"
      :placeholder="placeholder"
      :disabled="disabled"
      :required="required"
      :autocomplete="autocomplete"
      :class="[fieldClass, error ? 'ring-2 ring-red-400 border-red-300' : '']"
      @input="onInput"
      @blur="$emit('blur', $event)"
      @focus="$emit('focus', $event)"
    />

    <!-- Error / Help text -->
    <p
      v-if="error"
      class="mt-2 text-sm text-red-500 flex items-center gap-1.5 animate-fade-in"
    >
      <ExclamationCircleIcon class="h-4 w-4 shrink-0" />
      {{ error }}
    </p>
    <p
      v-else-if="help"
      :class="[
        'mt-2 text-sm',
        variant === 'dark' ? 'text-cream/50' : 'text-chocolate/50',
      ]"
    >
      {{ help }}
    </p>
  </div>
</template>
