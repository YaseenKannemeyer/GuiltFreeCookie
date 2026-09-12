<script setup>
import { ref, watch } from "vue";
import { ClipboardDocumentIcon, CheckIcon } from "@heroicons/vue/24/outline";
import BaseModal from "../ui/BaseModal.vue";
import BaseButton from "../ui/BaseButton.vue";
import adminApi from "../../api/adminApi";
import { useToast } from "../../composables/useToast";

const props = defineProps({
  open: {
    type: Boolean,
    default: false,
  },
  admin: {
    type: Object,
    default: null,
  },
});

const emit = defineEmits(["reset", "close"]);

const { showToast } = useToast();
const loading = ref(false);
const tempPassword = ref(null);
const copied = ref(false);

watch(
  () => props.open,
  (isOpen) => {
    if (!isOpen) return;
    tempPassword.value = null;
    copied.value = false;
  },
);

const confirmReset = async () => {
  loading.value = true;
  try {
    const result = await adminApi.resetPassword(props.admin.adminId);
    tempPassword.value = result.temporaryPassword;
    emit("reset", result.admin);
  } catch (error) {
    console.error("Failed to reset password:", error);
    showToast("Failed to reset password.", "error");
    emit("close");
  } finally {
    loading.value = false;
  }
};

const copyPassword = async () => {
  try {
    await navigator.clipboard.writeText(tempPassword.value);
    copied.value = true;
  } catch {
    // Clipboard API may be unavailable; the password is still shown on screen.
  }
};
</script>

<template>
  <BaseModal :open="open" title="Reset Password" size="sm" @close="emit('close')">
    <template v-if="!tempPassword">
      <p class="text-chocolate/70 mb-6">
        This will generate a new temporary password for
        <span class="font-semibold text-chocolate">{{ admin?.firstName }} {{ admin?.lastName }}</span>,
        valid for 24 hours. Their current password stops working immediately.
      </p>
      <div class="flex justify-end gap-3">
        <BaseButton variant="outline" size="sm" @click="emit('close')">Cancel</BaseButton>
        <BaseButton variant="primary" size="sm" :loading="loading" @click="confirmReset">
          Reset Password
        </BaseButton>
      </div>
    </template>

    <template v-else>
      <p class="text-chocolate/70 mb-3">
        Share this temporary password with {{ admin?.firstName }} securely — it won't be shown
        again and expires in 24 hours.
      </p>
      <div class="flex items-center gap-2 bg-cream-100 rounded-2xl px-4 py-3 mb-6">
        <code class="flex-1 font-mono text-lg text-chocolate tracking-wide">{{ tempPassword }}</code>
        <button
          type="button"
          class="h-9 w-9 flex items-center justify-center rounded-full text-chocolate/60 hover:text-chocolate hover:bg-cream-200 transition-colors"
          aria-label="Copy password"
          @click="copyPassword"
        >
          <CheckIcon v-if="copied" class="h-5 w-5 text-green-600" />
          <ClipboardDocumentIcon v-else class="h-5 w-5" />
        </button>
      </div>
      <div class="flex justify-end">
        <BaseButton variant="primary" size="sm" @click="emit('close')">Done</BaseButton>
      </div>
    </template>
  </BaseModal>
</template>
