<script setup>
/**
 * LegalPage — shared layout for the policy pages.
 * Header band, "the short version" summary, numbered sections with a sticky
 * contents list (collapsible on mobile), then links to the other policies.
 */
import { ref, computed, onMounted, onUnmounted } from "vue";
import { RouterLink, useRoute } from "vue-router";
import { ChevronDownIcon, ArrowRightIcon } from "@heroicons/vue/20/solid";
import { ExclamationTriangleIcon } from "@heroicons/vue/24/outline";
import { BUSINESS, DEMO_MODE, LEGAL, LEGAL_PAGES } from "../../constants/business";
import Breadcrumb from "../ui/Breadcrumb.vue";
import { breadcrumbsFor } from "../../seo/breadcrumbs";

const props = defineProps({
  title: { type: String, required: true },
  summary: { type: Array, default: () => [] }, // plain-language key points
  sections: { type: Array, required: true }, // [{ id, title }]
});

const route = useRoute();
const crumbs = computed(() => breadcrumbsFor(route));
const otherPages = computed(() => LEGAL_PAGES.filter((p) => p.path !== route.path));

// Highlight the section currently being read in the contents list.
const activeId = ref(props.sections[0]?.id);
let observer = null;
onMounted(() => {
  if (typeof IntersectionObserver === "undefined") return;
  observer = new IntersectionObserver(
    (entries) => {
      const visible = entries.filter((e) => e.isIntersecting);
      if (visible.length) activeId.value = visible[0].target.id;
    },
    { rootMargin: "-20% 0px -70% 0px" },
  );
  props.sections.forEach((s) => {
    const el = document.getElementById(s.id);
    if (el) observer.observe(el);
  });
});
onUnmounted(() => observer?.disconnect());
</script>

<template>
  <div>
    <!-- Header -->
    <section class="bg-tan pt-[calc(var(--nav-h)+2.4rem)] pb-14">
      <div class="container-magic">
        <Breadcrumb :items="crumbs" class="mb-3" />
        <h1 class="font-display text-display">{{ title }}</h1>
        <p class="mt-4 text-sm text-chocolate/80">
          Last updated {{ LEGAL.lastUpdated }}
        </p>
      </div>
    </section>

    <div class="container-magic py-14 lg:py-20 grid gap-x-16 gap-y-8" :class="{ 'lg:grid-cols-[25.6rem_1fr]': !DEMO_MODE }">
      <!-- Contents: sticky list on desktop, collapsible on mobile -->
      <aside v-if="!DEMO_MODE" class="lg:sticky lg:top-[calc(var(--nav-h)+3.2rem)] self-start">
        <nav aria-label="On this page" class="hidden lg:block">
          <p class="text-sm font-bold text-chocolate/75">On this page</p>
          <ol class="mt-3 border-l border-chocolate/15">
            <li v-for="(s, i) in sections" :key="s.id">
              <RouterLink
                :to="{ hash: `#${s.id}` }"
                :class="[
                  'flex items-start gap-2 min-h-11 py-2.5 pl-4 -ml-px border-l-2 text-sm transition-colors',
                  activeId === s.id
                    ? 'border-chocolate text-chocolate font-bold'
                    : 'border-transparent text-chocolate/75 hover:text-chocolate',
                ]"
              >
                <span class="tabular">{{ i + 1 }}.</span>
                <span>{{ s.title }}</span>
              </RouterLink>
            </li>
          </ol>
        </nav>

        <details class="lg:hidden group rounded-2xl border border-chocolate/15 bg-surface">
          <summary class="flex items-center justify-between min-h-11 px-5 py-3 cursor-pointer list-none font-bold">
            On this page
            <ChevronDownIcon class="h-5 w-5 transition-transform group-open:rotate-180" />
          </summary>
          <ol class="px-5 pb-4">
            <li v-for="(s, i) in sections" :key="s.id">
              <RouterLink :to="{ hash: `#${s.id}` }" class="flex gap-2 min-h-11 items-center text-sm text-chocolate/85">
                <span class="tabular">{{ i + 1 }}.</span>{{ s.title }}
              </RouterLink>
            </li>
          </ol>
        </details>
      </aside>

      <article class="min-w-0 max-w-[76.8rem]">
        <!-- Draft notice until an attorney has reviewed the policies -->
        <div
          v-if="!DEMO_MODE && !LEGAL.reviewed"
          class="mb-8 flex gap-3 rounded-2xl border border-primary-700/30 bg-primary/20 p-5"
          role="note"
        >
          <ExclamationTriangleIcon class="h-6 w-6 shrink-0 text-primary-800" />
          <p class="text-sm text-chocolate/85">
            This policy is a draft and is still being reviewed. Questions in the
            meantime? Email
            <a :href="`mailto:${BUSINESS.email}`" class="font-bold underline underline-offset-4">{{ BUSINESS.email }}</a>.
          </p>
        </div>

        <!-- The short version -->
        <section v-if="DEMO_MODE" class="space-y-5" aria-labelledby="demo-policy-title">
          <h2 id="demo-policy-title" class="font-bold text-base">About this demo</h2>
          <p>This is a software demonstration of an online cookie shop. It is not an operating business. The products, prices, stock, dietary categories and delivery estimates are illustrative.</p>
          <p>No goods are sold or shipped, and the demo payment screen does not charge money. Order cancellations and refunds only change the sample order record.</p>
          <p>Use made-up details when testing. Account, checkout and contact entries are stored in the demo database and browser storage. Demo administrators can view orders, messages and captured emails. Emails stay in the demo inbox instead of being sent to an external mailbox.</p>
          <p>The example business policies are not reviewed terms for a real shop. Real trading details, verified product information and appropriate policy review are needed before converting this project into a live business.</p>
        </section>
        <div v-else-if="summary.length" class="rounded-2xl bg-surface border border-chocolate/10 p-6 sm:p-8">
          <h2 class="text-base font-bold">The short version</h2>
          <ul class="mt-4 space-y-3">
            <li v-for="point in summary" :key="point" class="flex gap-3 text-chocolate/85">
              <span class="mt-[0.9rem] h-2 w-2 shrink-0 rounded-full bg-primary-700" aria-hidden="true" />
              <span>{{ point }}</span>
            </li>
          </ul>
        </div>

        <div v-if="!DEMO_MODE" class="mt-12">
          <slot />
        </div>

        <!-- Other policies -->
        <nav aria-label="Other policies" class="mt-16 border-t border-chocolate/15 pt-10">
          <p class="text-sm font-bold text-chocolate/75">Other policies</p>
          <ul class="mt-4 grid sm:grid-cols-3 gap-3">
            <li v-for="p in otherPages" :key="p.path">
              <RouterLink
                :to="p.path"
                class="group flex items-center justify-between gap-3 min-h-11 rounded-2xl border border-chocolate/15 px-5 py-4 font-bold hover:bg-chocolate/[0.05] transition-colors"
              >
                {{ p.label }}
                <ArrowRightIcon class="h-5 w-5 shrink-0 transition-transform group-hover:translate-x-1" />
              </RouterLink>
            </li>
          </ul>
        </nav>
      </article>
    </div>
  </div>
</template>
