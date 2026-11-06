<script setup>
import { computed, onBeforeUnmount, onMounted, ref, useId } from "vue";
import { MagnifyingGlassIcon } from "@heroicons/vue/20/solid";
import { addressApi } from "../../api/storeApi";

// Search-as-you-type for a South African address. Emits the chosen address as checkout's fields.
const emit = defineEmits(["select"]);
const DEBOUNCE_MS = 150;
const MIN_LENGTH = 3;

const uid = useId();
const enabled = ref(false);
const query = ref("");
const suggestions = ref([]);
const open = ref(false);
const active = ref(-1);
const status = ref("");
let session = newSession();
let timer;
let controller;
let seq = 0;

function newSession() {
  return globalThis.crypto?.randomUUID?.() ?? `${Date.now().toString(36)}-${Math.random().toString(36).slice(2)}`;
}

const message = computed(
  () =>
    ({
      empty: "No matches yet. Try the street and suburb, or type your address below.",
      failed: "Address search isn't working right now. Type your address below.",
      filling: "Filling in your address…",
      "lookup-failed": "We couldn't load that address. Pick it again or type it below.",
    })[status.value] || "Pick your address from the list and we'll fill in the rest.",
);

onMounted(async () => {
  try {
    enabled.value = (await addressApi.suggest("")).enabled;
  } catch {
    enabled.value = false;
  }
});
onBeforeUnmount(() => {
  clearTimeout(timer);
  controller?.abort();
});

function onInput(event) {
  query.value = event.target.value;
  clearTimeout(timer);
  controller?.abort();
  seq++;
  const q = query.value.trim();
  if (q.length < MIN_LENGTH) {
    suggestions.value = [];
    open.value = false;
    status.value = "";
    return;
  }
  timer = setTimeout(() => search(q), DEBOUNCE_MS);
}

async function search(q) {
  controller = new AbortController();
  const mine = ++seq;
  try {
    const result = await addressApi.suggest(q, session, controller.signal);
    if (mine !== seq) return;
    suggestions.value = result.suggestions;
    active.value = result.suggestions.length ? 0 : -1;
    status.value = result.failed ? "failed" : result.suggestions.length ? "" : "empty";
    open.value = result.suggestions.length > 0;
  } catch {
    if (mine !== seq) return;
    suggestions.value = [];
    open.value = false;
    status.value = "failed";
  }
}

async function choose(suggestion) {
  if (!suggestion) return;
  seq++;
  open.value = false;
  query.value = [suggestion.mainText, suggestion.secondaryText].filter(Boolean).join(", ");
  status.value = "filling";
  try {
    emit("select", await addressApi.place(suggestion.placeId, session));
    status.value = "";
  } catch {
    status.value = "lookup-failed";
  } finally {
    session = newSession();
  }
}

function onKeydown(event) {
  const count = suggestions.value.length;
  if (event.key === "Enter") {
    // Enter picks an address; it never submits the delivery form from here.
    event.preventDefault();
    if (open.value && active.value >= 0) choose(suggestions.value[active.value]);
  } else if (event.key === "Escape") {
    open.value = false;
  } else if (count && (event.key === "ArrowDown" || event.key === "ArrowUp")) {
    event.preventDefault();
    if (!open.value) {
      open.value = true;
      return;
    }
    const step = event.key === "ArrowDown" ? 1 : -1;
    active.value = (active.value + step + count) % count;
  }
}

// On a phone the keyboard covers the lower half, so lift the box to the top to leave room for suggestions.
function onFocus(event) {
  open.value = suggestions.value.length > 0;
  if (globalThis.matchMedia?.("(max-width: 767px)").matches) event.target.scrollIntoView({ block: "start", behavior: "smooth" });
}

function onBlur() {
  setTimeout(() => (open.value = false), 120);
}
</script>

<template>
  <div v-if="enabled" class="relative">
    <label :for="`${uid}-input`" class="block text-sm font-semibold mb-1.5 text-chocolate/85">Search your address</label>
    <div class="relative">
      <MagnifyingGlassIcon class="pointer-events-none absolute left-3.5 top-1/2 -translate-y-1/2 h-5 w-5 text-chocolate/60" aria-hidden="true" />
      <input
        :id="`${uid}-input`"
        type="text"
        role="combobox"
        class="field pl-11 scroll-mt-[calc(var(--nav-h)+1rem)]"
        autocomplete="off"
        spellcheck="false"
        enterkeyhint="search"
        placeholder="Start typing, e.g. 12 Main Road, Rondebosch"
        :value="query"
        aria-autocomplete="list"
        :aria-expanded="open ? 'true' : 'false'"
        :aria-controls="`${uid}-list`"
        :aria-activedescendant="open && active >= 0 ? `${uid}-option-${active}` : undefined"
        :aria-describedby="`${uid}-status`"
        @input="onInput"
        @keydown="onKeydown"
        @focus="onFocus"
        @blur="onBlur"
      />
    </div>
    <div
      v-show="open"
      class="absolute z-30 mt-2 w-full overflow-hidden rounded-2xl border border-cream-500 bg-cream-100 shadow-soft"
    >
      <ul :id="`${uid}-list`" role="listbox" aria-label="Matching addresses">
        <li
          v-for="(suggestion, index) in suggestions"
          :id="`${uid}-option-${index}`"
          :key="suggestion.placeId"
          role="option"
          :aria-selected="index === active ? 'true' : 'false'"
          :class="['cursor-pointer px-4 py-3 min-h-11', index === active ? 'bg-chocolate/[0.07]' : '']"
          @mousedown.prevent="choose(suggestion)"
          @mousemove="active = index"
        >
          <span class="block font-semibold break-words">{{ suggestion.mainText }}</span>
          <span v-if="suggestion.secondaryText" class="block text-sm text-chocolate/75 break-words">{{ suggestion.secondaryText }}</span>
        </li>
      </ul>
      <p class="px-4 py-2 text-xs text-right text-chocolate/60 border-t border-cream-500">Powered by Google</p>
    </div>
    <p :id="`${uid}-status`" class="mt-2 text-sm text-chocolate/75" aria-live="polite">{{ message }}</p>
  </div>
</template>
