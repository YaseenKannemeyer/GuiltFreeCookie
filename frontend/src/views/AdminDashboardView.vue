<script setup>
import LowStockWidget from "../components/admin/LowStockWidget.vue";
import { ref, computed, onMounted } from "vue";
import cookieApi from "../api/cookieApi";
import {
  COOKIE_CATEGORIES,
  formatCategoryLabel,
} from "../constants/cookieCategories";
import { useToast } from "../composables/useToast";
import CookieForm from "../components/admin/CookieForm.vue";
import CookieGrid from "../components/admin/CookieGrid.vue";
import DeleteModal from "../components/admin/DeleteModal.vue";
import SearchBar from "../components/ui/SearchBar.vue";
import BaseInput from "../components/ui/BaseInput.vue";
import BaseButton from "../components/ui/BaseButton.vue";
import ConfirmModal from "../components/ui/ConfirmModal.vue";
import AdminPageHeader from "../components/admin/AdminPageHeader.vue";
import { PlusIcon, ArrowPathIcon } from "@heroicons/vue/20/solid";
import { logError } from "../utils/logError";

const { showToast } = useToast();

const cookies = ref([]);
const loading = ref(false);
const saving = ref(false);
const loadError = ref(false);
const status = ref("all"); // all | live | archived
const showForm = ref(false);
const selectedCookie = ref(null);
const deleteTarget = ref(null);
const archiveTarget = ref(null);

const search = ref("");
const category = ref("all");

const categories = computed(() => [
  { id: "all", label: "All ranges" },
  ...COOKIE_CATEGORIES.map((id) => ({ id, label: formatCategoryLabel(id) })),
]);

const counts = computed(() => {
  const result = { all: cookies.value.length };
  cookies.value.forEach((c) => {
    result[c.category] = (result[c.category] || 0) + 1;
  });
  return result;
});

const categoryOptions = computed(() =>
  categories.value.map((c) => ({
    value: c.id,
    label:
      c.id !== "all" && counts.value[c.id] !== undefined
        ? `${c.label} (${counts.value[c.id]})`
        : c.label,
  })),
);

const filteredCookies = computed(() => {
  let result = [...cookies.value];

  if (search.value) {
    const term = search.value.toLowerCase();
    result = result.filter(
      (c) =>
        c.description?.toLowerCase().includes(term) ||
        c.ingredients?.toLowerCase().includes(term),
    );
  }

  if (category.value !== "all") {
    result = result.filter((c) => c.category === category.value);
  }

  if (status.value !== "all") {
    result = result.filter((c) =>
      status.value === "archived" ? c.archived : !c.archived,
    );
  }

  return result;
});

const statusTabs = computed(() => {
  const archived = cookies.value.filter((c) => c.archived).length;
  return [
    { id: "all", label: "All", count: cookies.value.length },
    { id: "live", label: "On menu", count: cookies.value.length - archived },
    { id: "archived", label: "Archived", count: archived },
  ];
});

const hasFilters = computed(
  () => !!search.value || category.value !== "all" || status.value !== "all",
);
const clearFilters = () => {
  search.value = "";
  category.value = "all";
  status.value = "all";
};

// Helpers for Modal State
const openForm = (cookie = null) => {
  selectedCookie.value = cookie ? { ...cookie } : null;
  showForm.value = true;
};

const closeForm = () => {
  showForm.value = false;
  selectedCookie.value = null;
};

// API Actions
const loadCookies = async () => {
  loading.value = true;
  loadError.value = false;
  try {
    cookies.value = await cookieApi.getAllForAdmin();
  } catch (error) {
    logError("Failed to load cookies:", error);
    loadError.value = true;
  } finally {
    loading.value = false;
  }
};

const saveCookie = async ({ cookie, imageFile }) => {
  if (saving.value) return;
  saving.value = true;
  const isEditing = !!cookie.cookieId;
  try {
    await cookieApi.save(cookie, imageFile);
    closeForm();
    await loadCookies();
    showToast(isEditing ? "Cookie updated." : "Cookie added.");
  } catch (error) {
    logError("Failed to save cookie:", error);
    // The API explains a rejected photo ("Upload a JPEG, PNG, WebP or GIF image").
    const reason =
      typeof error.response?.data === "string" && error.response.data;
    showToast(
      reason ||
        (isEditing ? "Failed to update cookie." : "Failed to add cookie."),
      "error",
    );
  } finally {
    saving.value = false;
  }
};

const confirmDelete = (cookie) => {
  deleteTarget.value = cookie;
};

const deleteCookie = async () => {
  const cookie = deleteTarget.value;
  deleteTarget.value = null;
  if (!cookie) return;

  try {
    await cookieApi.delete(cookie.cookieId);
    await loadCookies();
    showToast("Cookie deleted.");
  } catch (error) {
    logError("Failed to delete cookie:", error);
    showToast("Failed to delete cookie.", "error");
  }
};

const toggleArchive = async (cookie) => {
  if (!cookie.archived) {
    archiveTarget.value = cookie;
    return;
  }
  await setCookieArchived(cookie, false);
};

const confirmArchive = async () => {
  const cookie = archiveTarget.value;
  archiveTarget.value = null;
  if (!cookie) return;
  await setCookieArchived(cookie, true);
};

const setCookieArchived = async (cookie, archived) => {
  try {
    if (archived) {
      await cookieApi.archive(cookie.cookieId);
      showToast(`${cookie.description} archived.`);
    } else {
      await cookieApi.unarchive(cookie.cookieId);
      showToast(`${cookie.description} is back on the menu.`);
    }
    await loadCookies();
  } catch (error) {
    logError("Failed to update archive status:", error);
    showToast("Failed to update cookie.", "error");
  }
};

onMounted(loadCookies);
</script>

<template>
  <div>
    <AdminPageHeader
      title="Cookies"
      description="Everything on the menu, including archived cookies."
    >
      <BaseButton variant="primary" size="sm" @click="openForm()">
        <PlusIcon class="h-5 w-5 -ml-1" />
        Add cookie
      </BaseButton>
    </AdminPageHeader>
    <LowStockWidget />

    <!-- Status segments double as a summary -->
    <div
      class="flex gap-1 p-1 mb-5 rounded-xl bg-chocolate/[0.06] w-fit max-w-full overflow-x-auto no-scrollbar"
      role="group"
      aria-label="Filter by status"
    >
      <button
        v-for="tab in statusTabs"
        :key="tab.id"
        type="button"
        :aria-pressed="status === tab.id"
        :class="[
          'flex items-center gap-2 rounded-lg h-11 px-4 text-sm font-semibold whitespace-nowrap transition-colors',
          status === tab.id
            ? 'bg-surface text-chocolate shadow-soft'
            : 'text-chocolate/70 hover:text-chocolate',
        ]"
        @click="status = tab.id"
      >
        {{ tab.label }}
        <span
          class="tabular text-sm"
          :class="status === tab.id ? 'text-chocolate/60' : 'text-chocolate/50'"
          >{{ tab.count }}</span
        >
      </button>
    </div>

    <div class="flex flex-col sm:flex-row gap-3 mb-5">
      <SearchBar
        v-model="search"
        size="sm"
        label="Search cookies"
        placeholder="Search by name or ingredient"
        class="flex-1"
      />
      <BaseInput
        v-model="category"
        type="select"
        :options="categoryOptions"
        class="sm:w-60 shrink-0 [&_select]:h-11 [&_select]:py-0 [&_select]:rounded-full"
        ariaLabel="Filter by range"
      />
    </div>

    <!-- Loading -->
    <div
      v-if="loading && !cookies.length"
      class="card divide-y divide-chocolate/10"
      aria-busy="true"
      aria-label="Loading cookies"
    >
      <div v-for="n in 5" :key="n" class="flex items-center gap-4 p-4">
        <div class="shimmer h-14 w-14 rounded-xl" />
        <div class="flex-1 space-y-2">
          <div class="shimmer h-4 w-48 rounded-full" />
          <div class="shimmer h-3 w-72 max-w-full rounded-full" />
        </div>
        <div class="shimmer h-4 w-16 rounded-full" />
      </div>
    </div>

    <!-- Error -->
    <div v-else-if="loadError" class="card py-16 px-6 text-center" role="alert">
      <h2 class="text-base font-semibold">Couldn't load cookies</h2>
      <p class="mt-1 text-chocolate/70">
        We couldn't reach the store. Check your connection and try again.
      </p>
      <BaseButton variant="outline" size="sm" class="mt-5" @click="loadCookies">
        <ArrowPathIcon class="h-4 w-4" />
        Try again
      </BaseButton>
    </div>

    <!-- Empty catalogue -->
    <div v-else-if="cookies.length === 0" class="card py-16 px-6 text-center">
      <img
        src="/img/cookie.webp"
        alt=""
        class="mx-auto w-20 -rotate-12 drop-shadow-cookie-sm"
      />
      <h2 class="mt-4 font-sans text-base font-semibold">No cookies yet</h2>
      <p class="mt-1 text-chocolate/70">
        Add the first cookie and it will show up in the store.
      </p>
      <BaseButton variant="primary" size="sm" class="mt-5" @click="openForm()">
        <PlusIcon class="h-5 w-5 -ml-1" />
        Add cookie
      </BaseButton>
    </div>

    <!-- No matches -->
    <div
      v-else-if="filteredCookies.length === 0"
      class="card py-16 px-6 text-center"
    >
      <h2 class="text-base font-semibold">No cookies match these filters</h2>
      <p class="mt-1 text-chocolate/70">
        Try a different search, range or status.
      </p>
      <BaseButton
        v-if="hasFilters"
        variant="outline"
        size="sm"
        class="mt-5"
        @click="clearFilters"
        >Clear filters</BaseButton
      >
    </div>

    <CookieGrid
      v-else
      :cookies="filteredCookies"
      @edit="openForm"
      @delete="confirmDelete"
      @archive="toggleArchive"
    />

    <CookieForm
      :open="showForm"
      :saving="saving"
      :cookie="selectedCookie"
      @save="saveCookie"
      @close="closeForm"
    />

    <DeleteModal
      :open="!!deleteTarget"
      :cookie="deleteTarget"
      @confirm="deleteCookie"
      @close="deleteTarget = null"
    />

    <ConfirmModal
      :open="!!archiveTarget"
      title="Archive cookie?"
      :message="`${archiveTarget?.description} will be hidden from customers until you put it back on the menu.`"
      confirm-text="Archive"
      variant="danger"
      @confirm="confirmArchive"
      @close="archiveTarget = null"
    />
  </div>
</template>
