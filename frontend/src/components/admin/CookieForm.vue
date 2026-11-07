<script setup>
import { ref, computed, watch, onBeforeUnmount } from "vue";
import { XMarkIcon } from "@heroicons/vue/20/solid";
import cookieApi from "../../api/cookieApi";
import { COOKIE_CATEGORIES, formatCategoryLabel } from "../../constants/cookieCategories";
import BaseModal from "../ui/BaseModal.vue";
import BaseInput from "../ui/BaseInput.vue";
import BaseButton from "../ui/BaseButton.vue";
import ConfirmModal from "../ui/ConfirmModal.vue";
import MagneticDropZone from "../ui/MagneticDropZone.vue";
import { cleanText, cleanList, cleanPrice } from "../../utils/normalize";

const props = defineProps({
  saving: { type: Boolean, default: false },
  open: {
    type: Boolean,
    default: false,
  },
  cookie: {
    type: Object,
    default: null,
  },
});

const emit = defineEmits(["save", "close"]);

const categoryOptions = COOKIE_CATEGORIES.map((id) => ({
  value: id,
  label: formatCategoryLabel(id),
}));

// 1. Default form state template
const defaultForm = {
  cookieId: null,
  category: "",
  description: "",
  longDescription: "",
  ingredients: "",
  allergies: "",
  price: 0,
};

const form = ref({ ...defaultForm });
const imageFile = ref(null);
const imagePreview = ref(null);
const imageRemoved = ref(false);
const isEditing = computed(() => !!form.value.cookieId);
const showConfirm = ref(false);
let previewUrl;
function releasePreview() {
  if (previewUrl) URL.revokeObjectURL(previewUrl);
  previewUrl = undefined;
}
onBeforeUnmount(releasePreview);

// 2. Reset form state every time the modal opens
watch(
  () => props.open,
  (isOpen) => {
    releasePreview();
    if (!isOpen) return;
    form.value = props.cookie ? { ...props.cookie } : { ...defaultForm };
    imageFile.value = null;
    imageRemoved.value = false;
    imagePreview.value = props.cookie ? cookieApi.getImageUrl(props.cookie.image) : null;
    showConfirm.value = false;
  },
  { immediate: true },
);

const onImageChange = ([file]) => {
  if (!file) return;
  releasePreview();
  imageFile.value = file;
  imageRemoved.value = false;
  previewUrl = URL.createObjectURL(file);
  imagePreview.value = previewUrl;
};

const removeImage = () => {
  releasePreview();
  imageFile.value = null;
  imagePreview.value = null;
  imageRemoved.value = true;
};

const canSubmit = computed(
  () =>
    form.value.description &&
    form.value.category &&
    form.value.price > 0 &&
    (imageFile.value || (isEditing.value && !imageRemoved.value)),
);

const openConfirm = () => {
  showConfirm.value = true;
};

const confirmSubmit = () => {
  showConfirm.value = false;
  emit("save", {
    cookie: {
      ...form.value,
      description: cleanText(form.value.description),
      longDescription: cleanText(form.value.longDescription),
      ingredients: cleanList(form.value.ingredients),
      allergies: cleanText(form.value.allergies),
      price: cleanPrice(form.value.price),
    },
    imageFile: imageFile.value,
  });
};
</script>

<template>
  <BaseModal :open="open" :title="cookie ? 'Edit cookie' : 'Add cookie'" size="lg" @close="emit('close')">
    <form class="space-y-5" @submit.prevent="canSubmit && openConfirm()">
      <!-- Image -->
      <div>
        <p class="block text-sm font-semibold mb-1.5 text-chocolate/85">
          Photo <span class="text-primary-700" aria-hidden="true">*</span>
        </p>

        <div v-if="imagePreview" class="flex items-center gap-4">
          <div class="relative h-28 w-28 shrink-0 rounded-2xl bg-chocolate/[0.06] flex items-center justify-center overflow-hidden">
            <img :src="imagePreview" alt="Cookie preview" class="h-[88%] w-[88%] object-contain" />
          </div>
          <div class="min-w-0 space-y-2">
            <p class="text-sm text-chocolate/75 break-all">
              {{ imageFile ? imageFile.name : "Current photo" }}
            </p>
            <button
              type="button"
              class="inline-flex min-h-11 items-center gap-1.5 text-sm font-semibold text-chocolate hover:text-red-700 transition-colors"
              @click="removeImage"
            >
              <XMarkIcon class="h-4 w-4" />
              Replace photo
            </button>
          </div>
        </div>

        <template v-else>
          <MagneticDropZone
            :model-value="[]"
            accept="image/jpeg,image/png,image/webp,image/gif"
            :max-size="10 * 1024 * 1024"
            label="Drop a photo here"
            @update:model-value="onImageChange"
          />
          <p class="mt-2 text-sm text-chocolate/65">PNG with a transparent background looks best.</p>
        </template>

        <p v-if="isEditing && imageRemoved" class="mt-1 text-sm text-chocolate/65">
          Pick a new photo to replace the current one.
        </p>
      </div>

      <BaseInput v-model="form.description" label="Name" placeholder="e.g. Chocolate Stevia Cookie" required />

      <BaseInput
        v-model="form.longDescription"
        type="textarea"
        :rows="3"
        label="Description"
        placeholder="e.g. Deep cocoa dough with dark chocolate chunks and a soft centre."
        help="One or two sentences under the name on the product page. Leave empty to show the range's tagline."
      />

      <div class="grid sm:grid-cols-2 gap-4">
        <BaseInput v-model="form.price" type="number" label="Price (R)" required />
        <BaseInput
          v-model="form.category"
          type="select"
          label="Range"
          :options="[{ value: '', label: 'Choose a range' }, ...categoryOptions]"
          required
        />
      </div>

      <BaseInput
        v-model="form.ingredients"
        type="textarea"
        :rows="3"
        label="Ingredients"
        placeholder="e.g. Almond flour, cocoa powder, stevia"
        help="Separate with commas. Each one shows as a tag on the product page."
        required
      />

      <BaseInput
        v-model="form.allergies"
        label="Allergens"
        placeholder="e.g. Tree nuts, eggs"
        help="Write 'None' if there aren't any."
        required
      />

      <div class="flex flex-wrap justify-end gap-3 pt-3">
        <BaseButton variant="outline" size="sm" @click="emit('close')">Cancel</BaseButton>
        <BaseButton type="submit" variant="primary" size="sm" :disabled="!canSubmit" :loading="saving">
          {{ isEditing ? "Save changes" : "Add cookie" }}
        </BaseButton>
      </div>
    </form>

    <ConfirmModal
      :open="showConfirm"
      :title="isEditing ? 'Save changes?' : 'Add cookie?'"
      :message="
        isEditing
          ? `Save changes to ${form.description || 'this cookie'}?`
          : `Add ${form.description || 'this cookie'} to the menu?`
      "
      confirm-text="Save"
      @confirm="confirmSubmit"
      @close="showConfirm = false"
    />
  </BaseModal>
</template>
