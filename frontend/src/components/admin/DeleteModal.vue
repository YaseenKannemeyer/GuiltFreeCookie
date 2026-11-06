<script setup>
import { ref, watch } from "vue";
import { TrashIcon } from "@heroicons/vue/20/solid";
import BaseModal from "../ui/BaseModal.vue";
import BaseButton from "../ui/BaseButton.vue";
import HoldToConfirmButton from "../ui/HoldToConfirmButton.vue";
import UndoNotice from "../ui/UndoNotice.vue";

const props = defineProps({
  open: {
    type: Boolean,
    default: false,
  },
  cookie: {
    type: Object,
    default: null,
  },
});

const emit = defineEmits(["confirm", "close"]);

// "confirm" only fires once the undo window runs out, so Undo never needs the API.
const stage = ref("idle");
watch(
  () => props.open,
  (open) => open && (stage.value = "idle"),
);

const close = () => emit(stage.value === "undo" ? "confirm" : "close");
</script>

<template>
  <BaseModal :open="open" title="Delete cookie?" size="sm" @close="close">
    <p class="text-chocolate/80 mb-6">
      Delete <span class="font-semibold text-chocolate">{{ cookie?.description }}</span>? Hold the button to
      confirm. You'll have a few seconds to undo.
    </p>

    <UndoNotice
      v-if="stage === 'undo'"
      :message="`${cookie?.description || 'Cookie'} deleted`"
      @undo="stage = 'idle'"
      @expire="emit('confirm')"
    />
    <div v-else class="flex flex-wrap justify-end gap-3">
      <BaseButton variant="outline" size="sm" @click="emit('close')">Cancel</BaseButton>
      <HoldToConfirmButton :reset-after="0" @confirm="stage = 'undo'">
        <TrashIcon class="h-4 w-4" aria-hidden="true" />
        Hold to delete
      </HoldToConfirmButton>
    </div>
  </BaseModal>
</template>
