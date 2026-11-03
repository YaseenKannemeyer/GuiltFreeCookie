<script setup>
/**
 * MagneticDropZone — file drop zone that leans toward an incoming drag and
 * settles into a "ready" state once the file lands.
 *
 * Usage:
 *   <MagneticDropZone v-model="files" accept="image/*" :max-size="10 * 1024 * 1024" />
 */
import { computed, onBeforeUnmount, onMounted, ref } from "vue";
import { gsap } from "gsap";
import {
  ArchiveBoxIcon,
  ArrowUpTrayIcon,
  DocumentArrowUpIcon,
  DocumentIcon,
  DocumentTextIcon,
  PhotoIcon,
  XMarkIcon,
} from "@heroicons/vue/24/outline";
import { CheckCircleIcon } from "@heroicons/vue/20/solid";
import BaseButton from "./BaseButton.vue";

const MAGNET_DISTANCE = 180;
const MAX_MAGNET_OFFSET = 10;
const SETTLE = { duration: 0.45, ease: "power3.out" };

const props = defineProps({
  accept: { type: String, default: "image/*,.pdf,.zip" },
  maxSize: { type: Number, default: 20 * 1024 * 1024 },
  multiple: { type: Boolean, default: false },
  label: { type: String, default: "Drop a file here" },
});

const files = defineModel({ type: Array, default: () => [] });

const input = ref(null);
const zone = ref(null);
const glow = ref(null);
const isNear = ref(false);
const isOver = ref(false);
const error = ref("");
let reduceMotion = false;
let magnet = null;

const formatBytes = (bytes) => {
  if (bytes === 0) return "0 B";
  const units = ["B", "KB", "MB", "GB"];
  const unit = Math.min(Math.floor(Math.log(bytes) / Math.log(1024)), units.length - 1);
  const value = bytes / 1024 ** unit;
  return `${value >= 10 || unit === 0 ? value.toFixed(0) : value.toFixed(1)} ${units[unit]}`;
};

const fileIcon = (file) => {
  if (file.type.startsWith("image/")) return PhotoIcon;
  if (file.type === "application/pdf") return DocumentTextIcon;
  if (file.type.includes("zip") || file.name.toLowerCase().endsWith(".zip")) return ArchiveBoxIcon;
  return DocumentIcon;
};

const isFileDrag = (dataTransfer) => (dataTransfer ? Array.from(dataTransfer.types).includes("Files") : false);

const acceptsFile = (file, accept) => {
  if (!accept.trim()) return true;
  const name = file.name.toLowerCase();
  const type = file.type.toLowerCase();
  return accept
    .split(",")
    .map((rule) => rule.trim().toLowerCase())
    .some((rule) => {
      if (rule.startsWith(".")) return name.endsWith(rule);
      if (rule.endsWith("/*")) return type.startsWith(rule.slice(0, -1));
      return type === rule;
    });
};

const resetMagnet = () => {
  magnet?.x(0);
  magnet?.y(0);
  magnet?.scale(1);
  magnet?.glowX(0);
  magnet?.glowY(0);
  isNear.value = false;
  isOver.value = false;
};

const onWindowDragOver = (event) => {
  if (!isFileDrag(event.dataTransfer) || !zone.value) return;

  const rect = zone.value.getBoundingClientRect();
  const centerX = rect.left + rect.width / 2;
  const centerY = rect.top + rect.height / 2;
  const edgeX = Math.max(Math.abs(event.clientX - centerX) - rect.width / 2, 0);
  const edgeY = Math.max(Math.abs(event.clientY - centerY) - rect.height / 2, 0);
  const proximity = Math.max(0, 1 - Math.hypot(edgeX, edgeY) / MAGNET_DISTANCE);

  if (proximity === 0) {
    resetMagnet();
    return;
  }

  const pointerIsOver =
    event.clientX >= rect.left &&
    event.clientX <= rect.right &&
    event.clientY >= rect.top &&
    event.clientY <= rect.bottom;

  if (!reduceMotion) {
    const deltaX = event.clientX - centerX;
    const deltaY = event.clientY - centerY;
    const distance = Math.max(Math.hypot(deltaX, deltaY), 1);
    const offset = pointerIsOver ? 0 : proximity * MAX_MAGNET_OFFSET;
    magnet.x((deltaX / distance) * offset);
    magnet.y((deltaY / distance) * offset);
    magnet.scale(pointerIsOver ? 1.025 : 1.01);
    magnet.glowX(deltaX);
    magnet.glowY(deltaY);
  }

  isNear.value = true;
  isOver.value = pointerIsOver;
};

// relatedTarget is null once the drag leaves the window altogether.
const onWindowDragLeave = (event) => {
  if (!event.relatedTarget) resetMagnet();
};

onMounted(() => {
  reduceMotion = window.matchMedia("(prefers-reduced-motion: reduce)").matches;
  magnet = {
    x: gsap.quickTo(zone.value, "x", SETTLE),
    y: gsap.quickTo(zone.value, "y", SETTLE),
    scale: gsap.quickTo(zone.value, "scale", SETTLE),
    glowX: gsap.quickTo(glow.value, "x", SETTLE),
    glowY: gsap.quickTo(glow.value, "y", SETTLE),
  };
  window.addEventListener("dragover", onWindowDragOver);
  window.addEventListener("dragleave", onWindowDragLeave);
  window.addEventListener("dragend", resetMagnet);
  window.addEventListener("drop", resetMagnet);
});

onBeforeUnmount(() => {
  window.removeEventListener("dragover", onWindowDragOver);
  window.removeEventListener("dragleave", onWindowDragLeave);
  window.removeEventListener("dragend", resetMagnet);
  window.removeEventListener("drop", resetMagnet);
  gsap.killTweensOf([zone.value, glow.value]);
});

const acceptFiles = (incoming) => {
  const next = props.multiple ? incoming : incoming.slice(0, 1);
  const unsupported = next.find((file) => !acceptsFile(file, props.accept));
  const oversized = next.find((file) => file.size > props.maxSize);

  if (unsupported || oversized) {
    error.value = unsupported
      ? `${unsupported.name} isn't a supported file type.`
      : `${oversized.name} is larger than ${formatBytes(props.maxSize)}.`;
    files.value = [];
    return;
  }

  if (!next.length) return;
  error.value = "";
  files.value = next;
};

const onDrop = (event) => {
  event.preventDefault();
  event.stopPropagation();
  acceptFiles(Array.from(event.dataTransfer.files));
  resetMagnet();
};

const onDragOver = (event) => {
  if (!isFileDrag(event.dataTransfer)) return;
  event.preventDefault();
  event.dataTransfer.dropEffect = "copy";
};

const onInputChange = (event) => {
  acceptFiles(Array.from(event.target.files ?? []));
  event.target.value = "";
};

const openPicker = () => input.value?.click();

const clearFiles = () => {
  error.value = "";
  files.value = [];
};

const firstFile = computed(() => files.value[0]);
const title = computed(() => (isOver.value ? "Let go to add it" : isNear.value ? "Bring it closer" : props.label));
</script>

<template>
  <div
    ref="zone"
    class="relative flex min-h-64 w-full flex-col items-center justify-center overflow-hidden rounded-2xl border-2 px-6 py-8 text-center transition-colors duration-200 ease-cookie"
    :class="
      isOver
        ? 'border-solid border-chocolate bg-chocolate/[0.07]'
        : isNear
          ? 'border-solid border-chocolate/40 bg-chocolate/[0.04]'
          : 'border-dashed border-cream-400 bg-surface'
    "
    @dragover="onDragOver"
    @drop="onDrop"
  >
    <input ref="input" type="file" class="hidden" :accept="accept" :multiple="multiple" @change="onInputChange" />

    <div
      ref="glow"
      aria-hidden="true"
      class="pointer-events-none absolute left-1/2 top-1/2 -ml-20 -mt-20 h-40 w-40 rounded-full bg-primary/40 blur-3xl transition-opacity duration-200"
      :class="isNear ? 'opacity-100' : 'opacity-0'"
    />

    <div v-if="firstFile" class="relative flex w-full flex-col items-center">
      <div class="flex h-14 w-14 items-center justify-center rounded-2xl bg-chocolate text-cream-50 shadow-soft">
        <component :is="fileIcon(firstFile)" class="h-7 w-7" aria-hidden="true" />
      </div>
      <p class="mt-4 flex items-center gap-1.5 font-bold text-chocolate">
        <CheckCircleIcon class="h-5 w-5 text-green-600" aria-hidden="true" />
        {{ files.length === 1 ? "Ready to upload" : `${files.length} files ready` }}
      </p>
      <p class="mt-1 max-w-full truncate text-sm text-chocolate/75">
        {{ firstFile.name }}{{ files.length > 1 ? ` +${files.length - 1}` : "" }}
      </p>
      <p class="text-sm tabular text-chocolate/60">{{ formatBytes(firstFile.size) }}</p>

      <div class="mt-5 flex items-center gap-2">
        <BaseButton variant="cream" size="sm" @click="openPicker">
          <ArrowUpTrayIcon class="h-4 w-4" aria-hidden="true" />
          Replace
        </BaseButton>
        <BaseButton variant="cream" size="sm" :aria-label="`Remove ${firstFile.name}`" @click="clearFiles">
          <XMarkIcon class="h-4 w-4" aria-hidden="true" />
          Remove
        </BaseButton>
      </div>
    </div>

    <button
      v-else
      type="button"
      class="relative flex flex-col items-center rounded-2xl px-6 py-4 cursor-pointer"
      @click="openPicker"
    >
      <span
        class="flex h-14 w-14 items-center justify-center rounded-2xl transition-colors duration-200"
        :class="isOver ? 'bg-chocolate text-cream-50' : 'bg-chocolate/[0.07] text-chocolate'"
      >
        <DocumentArrowUpIcon class="h-7 w-7" aria-hidden="true" />
      </span>
      <span class="mt-4 font-bold text-chocolate">{{ title }}</span>
      <span class="mt-1 text-sm text-chocolate/65">or click to browse · up to {{ formatBytes(maxSize) }}</span>
    </button>

    <p v-if="error" role="alert" class="relative mt-4 max-w-xs text-sm font-semibold text-red-700">
      {{ error }}
    </p>
  </div>
</template>
