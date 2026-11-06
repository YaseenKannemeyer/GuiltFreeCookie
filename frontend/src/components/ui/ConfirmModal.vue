<script setup>
import BaseModal from "./BaseModal.vue";
import BaseButton from "./BaseButton.vue";

defineProps({
  open: {
    type: Boolean,
    default: false,
  },
  title: {
    type: String,
    default: "Are you sure?",
  },
  message: {
    type: String,
    default: "",
  },
  confirmText: {
    type: String,
    default: "Confirm",
  },
  cancelText: {
    type: String,
    default: "Cancel",
  },
  variant: {
    type: String,
    default: "primary", // primary | danger
  },
  loading: {
    type: Boolean,
    default: false,
  },
});

const emit = defineEmits(["confirm", "close"]);
</script>

<template>
  <BaseModal :open="open" :title="title" size="sm" @close="emit('close')">
    <p class="text-chocolate/80 mb-6">
      <slot>{{ message }}</slot>
    </p>

    <div class="flex flex-wrap justify-end gap-3">
      <BaseButton variant="outline" size="sm" :disabled="loading" @click="emit('close')">
        {{ cancelText }}
      </BaseButton>
      <BaseButton v-if="variant === 'primary'" variant="primary" size="sm" :loading="loading" @click="emit('confirm')">
        {{ confirmText }}
      </BaseButton>
      <BaseButton v-else variant="danger" size="sm" :loading="loading" :disabled="loading" @click="emit('confirm')">
        {{ confirmText }}
      </BaseButton>
    </div>
  </BaseModal>
</template>
