<script setup>
/**
 * AccordionList — manages open-state for a list of FAQ items.
 * Only one item open at a time (classic accordion behaviour).
 */
import { ref } from "vue";
import AccordionItem from "./AccordionItem.vue";

const props = defineProps({
  items: { type: Array, default: () => [] }, // [{id, question, answer}]
  multiple: { type: Boolean, default: false }, // allow multiple open
});

const openIds = ref(new Set([props.items[0]?.id].filter(Boolean)));

const toggle = (id) => {
  if (props.multiple) {
    if (openIds.value.has(id)) openIds.value.delete(id);
    else openIds.value.add(id);
  } else {
    if (openIds.value.has(id)) openIds.value.clear();
    else {
      openIds.value.clear();
      openIds.value.add(id);
    }
  }
  // Force reactivity
  openIds.value = new Set(openIds.value);
};

const isOpen = (id) => openIds.value.has(id);
</script>

<template>
  <div class="flex flex-col gap-3">
    <AccordionItem
      v-for="item in items"
      :key="item.id"
      :question="item.question"
      :answer="item.answer"
      :is-open="isOpen(item.id)"
      @toggle="toggle(item.id)"
    />
  </div>
</template>
