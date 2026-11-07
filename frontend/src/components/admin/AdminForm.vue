<script setup>
import { ref, computed, watch } from "vue";
import BaseModal from "../ui/BaseModal.vue";
import BaseInput from "../ui/BaseInput.vue";
import BaseButton from "../ui/BaseButton.vue";
import ConfirmModal from "../ui/ConfirmModal.vue";
import { cleanEmail, cleanText } from "../../utils/normalize";
import { MIN_PASSWORD_LENGTH } from "../../constants/auth";

const props = defineProps({
  saving: { type: Boolean, default: false },
  open: {
    type: Boolean,
    default: false,
  },
  admin: {
    type: Object,
    default: null,
  },
});

const emit = defineEmits(["save", "close"]);

const defaultForm = {
  adminId: null,
  firstName: "",
  lastName: "",
  email: "",
  password: "",
  active: true,
};

const form = ref({ ...defaultForm });
const isEditing = computed(() => !!form.value.adminId);
const showConfirm = ref(false);

watch(
  () => props.open,
  (isOpen) => {
    if (!isOpen) return;
    form.value = props.admin ? { ...props.admin, password: "" } : { ...defaultForm };
    showConfirm.value = false;
  },
  { immediate: true },
);

const passwordTooShort = computed(
  () => !isEditing.value && form.value.password.length > 0 && form.value.password.length < MIN_PASSWORD_LENGTH,
);

const canSubmit = computed(
  () =>
    form.value.firstName &&
    form.value.lastName &&
    form.value.email &&
    (isEditing.value || form.value.password.length >= MIN_PASSWORD_LENGTH),
);

const openConfirm = () => {
  showConfirm.value = true;
};

const confirmSubmit = () => {
  showConfirm.value = false;
  emit("save", {
    ...form.value,
    firstName: cleanText(form.value.firstName),
    lastName: cleanText(form.value.lastName),
    email: cleanEmail(form.value.email),
  });
};
</script>

<template>
  <BaseModal :open="open" :title="admin ? 'Edit admin' : 'Add admin'" @close="emit('close')">
    <div class="space-y-4">
      <div class="grid sm:grid-cols-2 gap-4">
        <BaseInput v-model="form.firstName" label="First name" placeholder="Thandiwe" required />
        <BaseInput v-model="form.lastName" label="Last name" placeholder="Mokoena" required />
      </div>
      <BaseInput
        v-model="form.email"
        type="email"
        label="Email"
        placeholder="thandiwe@guiltfreecookie.co.za"
        required
      />

      <BaseInput
        v-if="!isEditing"
        v-model="form.password"
        type="password"
        label="Password"
        autocomplete="new-password"
        :help="`At least ${MIN_PASSWORD_LENGTH} characters. They can change it from their profile after logging in.`"
        :error="passwordTooShort ? `Use at least ${MIN_PASSWORD_LENGTH} characters.` : ''"
        required
      />
      <p v-else class="text-sm text-chocolate/70">
        To give them a new password, use "Reset password" in the admin list.
      </p>
    </div>

    <div class="flex flex-wrap justify-end gap-3 mt-8">
      <BaseButton variant="outline" size="sm" @click="emit('close')">Cancel</BaseButton>
      <BaseButton variant="primary" size="sm" :disabled="!canSubmit" :loading="saving" @click="openConfirm">
        Save
      </BaseButton>
    </div>

    <ConfirmModal
      :open="showConfirm"
      :title="isEditing ? 'Save changes?' : 'Add admin?'"
      :message="
        isEditing
          ? `Save changes to ${form.firstName} ${form.lastName}?`
          : `Add ${form.firstName} ${form.lastName} as a new admin?`
      "
      confirm-text="Save"
      @confirm="confirmSubmit"
      @close="showConfirm = false"
    />
  </BaseModal>
</template>
