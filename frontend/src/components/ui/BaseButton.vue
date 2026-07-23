<script setup>
/**
 * BaseButton — the single button component used everywhere.
 *
 * Variants: primary | secondary | cream | outline | light
 * Sizes:    sm | md | lg
 *
 * Usage:
 *   <BaseButton variant="primary" size="md" @click="...">Buy Now</BaseButton>
 *   <BaseButton href="..." variant="outline">External</BaseButton>             (renders <a>)
 */
import { computed, useSlots } from "vue";
import { RouterLink } from "vue-router";
import { useRipple } from "../../composables/useRipple";

const { vRipple } = useRipple();
const slots = useSlots();

const props = defineProps({
  variant: { type: String, default: "primary" }, // primary | secondary | cream | outline | light
  size: { type: String, default: "md" }, // sm | md | lg
  to: { type: [String, Object], default: null },
  href: { type: String, default: null },
  type: { type: String, default: "button" },
  disabled: { type: Boolean, default: false },
  block: { type: Boolean, default: false },
  loading: { type: Boolean, default: false },
});

const variantClass = computed(() => {
  switch (props.variant) {
    case "secondary":
      return "btn-secondary";
    case "cream":
      return "btn-cream";
    case "outline":
      return "btn-outline";
    case "light":
      return "btn-light";
    case "primary":
    default:
      return "btn-primary";
  }
});

const sizeClass = computed(() => {
  switch (props.size) {
    case "sm":
      return "text-sm px-5 py-2.5";
    case "lg":
      return "text-body px-9 py-4";
    case "md":
    default:
      return "";
  }
});

const classes = computed(() => [
  variantClass.value,
  sizeClass.value,
  props.block ? "btn-block" : "",
  props.disabled ? "opacity-50 cursor-not-allowed pointer-events-none" : "",
]);

const hasLoader = computed(() => props.loading || false);
</script>

<template>
  <component
    :is="to ? RouterLink : href ? 'a' : 'button'"
    v-ripple
    :to="to"
    :href="href"
    :type="to || href ? null : type"
    :disabled="to || href ? null : disabled"
    :class="classes"
    class="ripple-container"
  >
    <svg
      v-if="hasLoader"
      class="animate-spin h-4 w-4 -ml-1"
      viewBox="0 0 24 24"
      fill="none"
      aria-hidden="true"
    >
      <circle
        class="opacity-25"
        cx="12"
        cy="12"
        r="10"
        stroke="currentColor"
        stroke-width="4"
      />
      <path
        class="opacity-90"
        fill="currentColor"
        d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"
      />
    </svg>
    <slot />
  </component>
</template>
