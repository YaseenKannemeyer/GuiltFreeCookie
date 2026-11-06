<script setup>
/**
 * AdminList — admin accounts. A table from xl up, stacked rows below.
 */
import {
  PencilSquareIcon,
  TrashIcon,
  KeyIcon,
  NoSymbolIcon,
  CheckCircleIcon,
} from "@heroicons/vue/24/outline";
import IconAction from "./IconAction.vue";

defineProps({
  admins: {
    type: Array,
    required: true,
  },
  currentAdminId: {
    type: String,
    default: null,
  },
});

const emit = defineEmits(["edit", "delete", "toggleActive", "resetPassword"]);

const initials = (a) => `${a.firstName?.[0] ?? ""}${a.lastName?.[0] ?? ""}`.toUpperCase() || "?";
</script>

<template>
  <div class="card overflow-hidden">
    <table class="block xl:table xl:table-fixed w-full text-left">
      <thead class="hidden xl:table-header-group bg-cream-400/40 text-sm text-chocolate/70">
        <tr>
          <th scope="col" class="py-3 pl-5 pr-3 font-semibold w-[50%]">Name</th>
          <th scope="col" class="py-3 px-3 font-semibold w-[16%]">Status</th>
          <th scope="col" class="py-3 pl-3 pr-5 font-semibold text-right"><span class="sr-only">Actions</span></th>
        </tr>
      </thead>
      <tbody class="block xl:table-row-group divide-y divide-chocolate/10">
        <tr
          v-for="admin in admins"
          :key="admin.adminId"
          class="grid grid-cols-[minmax(0,1fr)] items-center gap-x-3 gap-y-2 p-4 xl:table-row xl:p-0 hover:bg-cream-400/25 transition-colors"
        >
          <td class="min-w-0 xl:py-3.5 xl:pl-5 xl:pr-3">
            <div class="flex items-center gap-3.5 min-w-0">
              <span
                class="h-10 w-10 shrink-0 rounded-full text-sm font-bold flex items-center justify-center"
                :class="admin.active ? 'bg-tan text-chocolate' : 'bg-chocolate/[0.08] text-chocolate/80'"
              >
                {{ initials(admin) }}
              </span>
              <div class="min-w-0">
                <p class="font-semibold text-chocolate flex flex-wrap items-center gap-2">
                  {{ admin.firstName }} {{ admin.lastName }}
                  <span
                    v-if="admin.adminId === currentAdminId"
                    class="rounded-md bg-chocolate px-1.5 py-0.5 text-sm font-semibold text-cream-50"
                  >You</span>
                </p>
                <p class="text-sm text-chocolate/65 truncate">{{ admin.email }}</p>
              </div>
            </div>
          </td>

          <td class="row-start-2 xl:px-3">
            <span
              class="inline-flex items-center gap-1.5 rounded-full px-2.5 py-1 text-sm font-semibold"
              :class="admin.active ? 'bg-emerald-800/15 text-emerald-900' : 'bg-chocolate/[0.08] text-chocolate/75'"
            >
              <span class="h-1.5 w-1.5 rounded-full" :class="admin.active ? 'bg-emerald-600' : 'bg-chocolate/40'" />
              {{ admin.active ? "Active" : "Deactivated" }}
            </span>
          </td>

          <td class="row-start-3 xl:pl-3 xl:pr-5">
            <div class="flex flex-wrap xl:justify-end gap-0.5">
              <IconAction label="Edit" :icon="PencilSquareIcon" @click="emit('edit', admin)" />
              <IconAction label="Reset password" :icon="KeyIcon" @click="emit('resetPassword', admin)" />
              <IconAction
                :label="admin.active ? 'Deactivate' : 'Activate'"
                :icon="admin.active ? NoSymbolIcon : CheckCircleIcon"
                :disabled="admin.adminId === currentAdminId"
                disabled-reason="You can't deactivate yourself"
                @click="emit('toggleActive', admin)"
              />
              <IconAction
                label="Delete"
                tone="danger"
                :icon="TrashIcon"
                :disabled="admin.adminId === currentAdminId"
                disabled-reason="You can't delete yourself"
                @click="emit('delete', admin)"
              />
            </div>
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</template>
