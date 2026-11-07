<script setup>
import { ref, computed, onMounted } from "vue";
import { UsersIcon } from "@heroicons/vue/24/outline";
import { PlusIcon, ArrowPathIcon } from "@heroicons/vue/20/solid";
import AdminPageHeader from "../components/admin/AdminPageHeader.vue";
import adminApi from "../api/adminApi";
import { useToast } from "../composables/useToast";
import { useAdminAuth } from "../composables/useAdminAuth";
import { logError } from "../utils/logError";
import AdminForm from "../components/admin/AdminForm.vue";
import AdminList from "../components/admin/AdminList.vue";
import AdminDeleteModal from "../components/admin/AdminDeleteModal.vue";
import AdminResetPasswordModal from "../components/admin/AdminResetPasswordModal.vue";
import BaseButton from "../components/ui/BaseButton.vue";
import ConfirmModal from "../components/ui/ConfirmModal.vue";

const { showToast } = useToast();
const { admin: currentAdmin } = useAdminAuth();

const tab = ref("users");

const admins = ref([]);
const loading = ref(false);
const saving = ref(false);
const loadError = ref(false);
const showForm = ref(false);
const selectedAdmin = ref(null);
const deleteTarget = ref(null);
const resetTarget = ref(null);
const deactivateTarget = ref(null);

const currentAdminId = computed(() => currentAdmin.value?.adminId ?? null);

const openForm = (admin = null) => {
  selectedAdmin.value = admin ? { ...admin } : null;
  showForm.value = true;
};

const closeForm = () => {
  showForm.value = false;
  selectedAdmin.value = null;
};

const loadAdmins = async () => {
  loading.value = true;
  loadError.value = false;
  try {
    admins.value = await adminApi.getAll();
  } catch (error) {
    logError("Failed to load admins:", error);
    loadError.value = true;
  } finally {
    loading.value = false;
  }
};

const saveAdmin = async (admin) => {
  if (saving.value) return;
  const isEditing = !!admin.adminId;
  // Caught here when the list already shows it; the server checks too.
  const emailTaken = admins.value.some((a) => a.email?.toLowerCase() === admin.email && a.adminId !== admin.adminId);
  if (emailTaken) {
    showToast("An admin with that email already exists.", "error");
    return;
  }
  saving.value = true;
  try {
    if (isEditing) {
      await adminApi.update(admin);
    } else {
      await adminApi.create(admin);
    }
    closeForm();
    await loadAdmins();
    showToast(isEditing ? "Admin updated." : "Admin added.");
  } catch (error) {
    logError("Failed to save admin:", error);
    showToast(
      error.response?.status === 409
        ? "An admin with that email already exists."
        : isEditing
          ? "Failed to update admin."
          : "Failed to add admin.",
      "error",
    );
  } finally {
    saving.value = false;
  }
};

const confirmDelete = (admin) => {
  deleteTarget.value = admin;
};

const deleteAdmin = async () => {
  const admin = deleteTarget.value;
  deleteTarget.value = null;
  if (!admin) return;

  try {
    await adminApi.delete(admin.adminId);
    await loadAdmins();
    showToast("Admin deleted.");
  } catch (error) {
    logError("Failed to delete admin:", error);
    showToast("Failed to delete admin.", "error");
  }
};

const toggleActive = async (admin) => {
  if (admin.active) {
    deactivateTarget.value = admin;
    return;
  }
  await setAdminActive(admin, true);
};

const confirmDeactivate = async () => {
  const admin = deactivateTarget.value;
  deactivateTarget.value = null;
  if (!admin) return;
  await setAdminActive(admin, false);
};

const setAdminActive = async (admin, active) => {
  try {
    await adminApi.setActive(admin.adminId, active);
    await loadAdmins();
    showToast(active ? "Admin activated." : "Admin deactivated.");
  } catch (error) {
    logError("Failed to update admin status:", error);
    showToast("Failed to update admin status.", "error");
  }
};

const openResetPassword = (admin) => {
  resetTarget.value = admin;
};

const onPasswordReset = async () => {
  await loadAdmins();
  showToast("Temporary password issued.");
};

onMounted(loadAdmins);
</script>

<template>
  <div>
    <AdminPageHeader title="Users" description="Who can manage the store, and the customers who shop it.">
      <BaseButton v-if="tab === 'users'" variant="primary" size="sm" @click="openForm()">
        <PlusIcon class="h-5 w-5 -ml-1" />
        Add admin
      </BaseButton>
    </AdminPageHeader>

    <div class="flex gap-1 p-1 mb-5 rounded-xl bg-chocolate/[0.06] w-fit" role="group" aria-label="User type">
      <button
        v-for="t in [{ id: 'users', label: 'Admins' }, { id: 'customers', label: 'Customers' }]"
        :key="t.id"
        type="button"
        :aria-pressed="tab === t.id"
        :class="[ 'flex items-center gap-2 rounded-lg h-11 px-4 text-sm font-semibold transition-colors', tab === t.id ? 'bg-surface text-chocolate shadow-soft' : 'text-chocolate/70 hover:text-chocolate', ]"
        @click="tab = t.id"
      >
        {{ t.label }}
        <span v-if="t.id === 'users' && admins.length" class="tabular text-sm text-chocolate/55">{{ admins.length }}</span>
      </button>
    </div>

    <div v-if="tab === 'customers'" class="card py-16 px-6 text-center">
      <UsersIcon class="mx-auto h-10 w-10 text-chocolate/35" />
      <h2 class="mt-3 font-sans text-base font-semibold">Customer management isn't available here yet</h2>
      <p class="mt-1 text-chocolate/70">You can find customer contact and delivery details in their orders.</p>
      <BaseButton :to="{ name: 'AdminOrders' }" variant="outline" size="sm" class="mt-5">View orders</BaseButton>
    </div>

    <template v-else>
      <div v-if="loading && !admins.length" class="card divide-y divide-chocolate/10" aria-busy="true" aria-label="Loading admins">
        <div v-for="n in 3" :key="n" class="flex items-center gap-4 p-4">
          <div class="shimmer h-10 w-10 shrink-0 rounded-full" />
          <div class="min-w-0 flex-1 space-y-2">
            <div class="shimmer h-4 w-40 max-w-full rounded-full" />
            <div class="shimmer h-3 w-56 max-w-full rounded-full" />
          </div>
        </div>
      </div>

      <div v-else-if="loadError" class="card py-16 px-6 text-center" role="alert">
        <h2 class="text-base font-semibold">Couldn't load admins</h2>
        <p class="mt-1 text-chocolate/70">We couldn't reach the store. Check your connection and try again.</p>
        <BaseButton variant="outline" size="sm" class="mt-5" @click="loadAdmins">
          <ArrowPathIcon class="h-4 w-4" />
          Try again
        </BaseButton>
      </div>

      <div v-else-if="admins.length === 0" class="card py-16 px-6 text-center">
        <h2 class="text-base font-semibold">No admins yet</h2>
        <p class="mt-1 text-chocolate/70">Add someone to help run the store.</p>
      </div>

      <AdminList
        v-else
        :admins="admins"
        :current-admin-id="currentAdminId"
        @edit="openForm"
        @delete="confirmDelete"
        @toggle-active="toggleActive"
        @reset-password="openResetPassword"
      />
    </template>

    <AdminForm
      :open="showForm"
      :saving="saving"
      :admin="selectedAdmin"
      @save="saveAdmin"
      @close="closeForm"
    />

    <AdminDeleteModal
      :open="!!deleteTarget"
      :admin="deleteTarget"
      @confirm="deleteAdmin"
      @close="deleteTarget = null"
    />

    <AdminResetPasswordModal
      :open="!!resetTarget"
      :admin="resetTarget"
      @reset="onPasswordReset"
      @close="resetTarget = null"
    />

    <ConfirmModal
      :open="!!deactivateTarget"
      title="Deactivate admin?"
      :message="`${deactivateTarget?.firstName} ${deactivateTarget?.lastName} won't be able to log in until reactivated.`"
      confirm-text="Deactivate"
      variant="danger"
      @confirm="confirmDeactivate"
      @close="deactivateTarget = null"
    />
  </div>
</template>
