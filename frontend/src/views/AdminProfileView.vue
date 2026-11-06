<script setup>
import { ref, watch } from "vue";
import adminApi from "../api/adminApi";
import { cleanEmail, cleanText } from "../utils/normalize";
import { logError } from "../utils/logError";
import { MIN_PASSWORD_LENGTH } from "../constants/auth";
import { useAdminAuth } from "../composables/useAdminAuth";
import { useToast } from "../composables/useToast";
import BaseInput from "../components/ui/BaseInput.vue";
import BaseButton from "../components/ui/BaseButton.vue";
import ConfirmModal from "../components/ui/ConfirmModal.vue";
import AdminPageHeader from "../components/admin/AdminPageHeader.vue";

const { admin, updateAdmin, updateSession } = useAdminAuth();
const { showToast } = useToast();

const profileForm = ref({ firstName: "", lastName: "", email: "" });
const savingProfile = ref(false);
const showProfileConfirm = ref(false);

const passwordForm = ref({ currentPassword: "", newPassword: "", confirmPassword: "" });
const passwordError = ref("");
const changingPassword = ref(false);
const showPasswordConfirm = ref(false);

watch(
  admin,
  (value) => {
    if (!value) return;
    profileForm.value = {
      firstName: value.firstName,
      lastName: value.lastName,
      email: value.email,
    };
  },
  { immediate: true },
);

const confirmSaveProfile = async () => {
  showProfileConfirm.value = false;
  savingProfile.value = true;
  try {
    const updated = await adminApi.update({
      ...admin.value,
      firstName: cleanText(profileForm.value.firstName),
      lastName: cleanText(profileForm.value.lastName),
      email: cleanEmail(profileForm.value.email),
    });
    updateAdmin(updated);
    showToast("Profile updated.");
  } catch (error) {
    logError("Failed to update profile:", error);
    showToast(
      error.response?.status === 409 ? "Another admin already uses that email." : "Failed to update profile.",
      "error",
    );
  } finally {
    savingProfile.value = false;
  }
};

const canChangePassword = () =>
  passwordForm.value.currentPassword &&
  passwordForm.value.newPassword.length >= MIN_PASSWORD_LENGTH &&
  passwordForm.value.newPassword === passwordForm.value.confirmPassword;

const openPasswordConfirm = () => {
  passwordError.value = "";

  if (passwordForm.value.newPassword !== passwordForm.value.confirmPassword) {
    passwordError.value = "New passwords don't match.";
    return;
  }

  showPasswordConfirm.value = true;
};

const confirmChangePassword = async () => {
  showPasswordConfirm.value = false;
  changingPassword.value = true;
  try {
    // Changing the password ends every other session, so the server sends
    // back a fresh token for this one.
    const { admin: updated, token } = await adminApi.changePassword(
      admin.value.adminId,
      passwordForm.value.currentPassword,
      passwordForm.value.newPassword,
    );
    updateSession(updated, token);
    passwordForm.value = { currentPassword: "", newPassword: "", confirmPassword: "" };
    showToast("Password changed.");
  } catch (error) {
    passwordError.value =
      error.response?.status === 401
        ? "Current password is incorrect."
        : "Failed to change password.";
  } finally {
    changingPassword.value = false;
  }
};
</script>

<template>
  <div>
    <AdminPageHeader title="Your profile" description="Your name, login email and password." />

    <div class="space-y-6">
      <section class="card grid 2xl:grid-cols-[25.6rem_minmax(0,1fr)] gap-6 md:gap-10 p-6 md:p-8" aria-labelledby="profile-details">
        <div>
          <h2 id="profile-details" class="text-base font-semibold tracking-[-0.01em]">Details</h2>
          <p class="mt-1 text-sm text-chocolate/70">This is the email you log in with.</p>
        </div>
        <form class="space-y-4" @submit.prevent="profileForm.firstName && profileForm.lastName && profileForm.email && (showProfileConfirm = true)">
          <div class="grid sm:grid-cols-2 gap-4">
            <BaseInput v-model="profileForm.firstName" label="First name" autocomplete="given-name" required />
            <BaseInput v-model="profileForm.lastName" label="Last name" autocomplete="family-name" required />
          </div>
          <BaseInput v-model="profileForm.email" type="email" label="Email" autocomplete="email" required />
          <div class="flex justify-end pt-2">
            <BaseButton
              type="submit"
              variant="primary"
              size="sm"
              :loading="savingProfile"
              :disabled="!profileForm.firstName || !profileForm.lastName || !profileForm.email"
            >
              Save changes
            </BaseButton>
          </div>
        </form>
      </section>

      <section class="card grid 2xl:grid-cols-[25.6rem_minmax(0,1fr)] gap-6 md:gap-10 p-6 md:p-8" aria-labelledby="profile-password">
        <div>
          <h2 id="profile-password" class="text-base font-semibold tracking-[-0.01em]">Password</h2>
          <p class="mt-1 text-sm text-chocolate/70">You'll need the new password the next time you log in. Any other device you're logged in on is signed out.</p>
        </div>
        <form class="space-y-4" @submit.prevent="canChangePassword() && openPasswordConfirm()">
          <BaseInput
            v-model="passwordForm.currentPassword"
            type="password"
            label="Current password"
            autocomplete="current-password"
            required
          />
          <div class="grid sm:grid-cols-2 gap-4">
            <BaseInput
              v-model="passwordForm.newPassword"
              type="password"
              label="New password"
              autocomplete="new-password"
              :help="`At least ${MIN_PASSWORD_LENGTH} characters.`"
              :error="passwordForm.newPassword && passwordForm.newPassword.length < MIN_PASSWORD_LENGTH ? `Use at least ${MIN_PASSWORD_LENGTH} characters.` : ''"
              required
            />
            <BaseInput
              v-model="passwordForm.confirmPassword"
              type="password"
              label="Confirm new password"
              autocomplete="new-password"
              required
              :error="passwordForm.confirmPassword && passwordForm.newPassword !== passwordForm.confirmPassword ? 'Passwords don\'t match yet.' : ''"
            />
          </div>

          <p v-if="passwordError" class="text-sm font-medium text-red-700" role="alert">{{ passwordError }}</p>

          <div class="flex justify-end pt-2">
            <BaseButton
              type="submit"
              variant="primary"
              size="sm"
              :loading="changingPassword"
              :disabled="!canChangePassword()"
            >
              Change password
            </BaseButton>
          </div>
        </form>
      </section>
    </div>

    <ConfirmModal
      :open="showProfileConfirm"
      title="Save changes?"
      message="Save changes to your profile details?"
      confirm-text="Save"
      :loading="savingProfile"
      @confirm="confirmSaveProfile"
      @close="showProfileConfirm = false"
    />

    <ConfirmModal
      :open="showPasswordConfirm"
      title="Change password?"
      message="You'll need your new password the next time you log in."
      confirm-text="Change password"
      variant="danger"
      :loading="changingPassword"
      @confirm="confirmChangePassword"
      @close="showPasswordConfirm = false"
    />
  </div>
</template>
