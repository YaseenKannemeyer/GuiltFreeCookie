<script setup>
import { ref, reactive, computed, onMounted } from "vue";
import { RouterLink } from "vue-router";
import { PhoneIcon, EnvelopeIcon, MapPinIcon } from "@heroicons/vue/24/outline";
import { CheckIcon, ArrowUpRightIcon } from "@heroicons/vue/20/solid";
import BaseInput from "../../components/ui/BaseInput.vue";
import BaseButton from "../../components/ui/BaseButton.vue";
import { useScrollReveal } from "../../composables/useScrollReveal";
import {
  cleanEmail,
  cleanText,
  cleanMultiline,
  isEmail,
} from "../../utils/normalize";
import { api, errorMessage } from "../../api/storeApi";
import { useSiteInfo } from "../../composables/useSiteInfo";
const { info, load } = useSiteInfo();
onMounted(load);
const busy = ref(false),
  submitError = ref("");

defineProps({
  // The Contact page has its own hero, so it hides this heading.
  showHeading: { type: Boolean, default: true },
});

const { vReveal } = useScrollReveal();

const blank = () => ({
  name: "",
  email: "",
  subject: "",
  message: "",
  phone: "",
  website: "",
});
const form = reactive(blank());
const errors = reactive({});
const handedOff = ref(false);

const contactCards = computed(() =>
  info.value
    ? [
        {
          icon: PhoneIcon,
          label: "Phone",
          value: info.value.phone,
          sub: info.value.hours,
          href: `tel:${info.value.phone.replace(/[^+0-9]/g, "")}`,
        },
        {
          icon: EnvelopeIcon,
          label: "Email",
          value: info.value.supportEmail,
          sub: info.value.responseTime,
          href: `mailto:${info.value.supportEmail}`,
        },
        {
          icon: MapPinIcon,
          label: "Address",
          value: info.value.address,
          sub: "",
          href: `https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(info.value.address)}`,
        },
      ].filter((c) => c.value)
    : [],
);

const validate = () => {
  Object.keys(errors).forEach((k) => delete errors[k]);
  form.name = cleanText(form.name);
  form.email = cleanEmail(form.email);
  form.subject = cleanText(form.subject);
  form.message = cleanMultiline(form.message);
  if (!form.name) errors.name = "Tell us your name.";
  if (!isEmail(form.email))
    errors.email = "Enter an email like you@example.co.za.";
  if (form.message.length < 10)
    errors.message = "Add a little more detail so we can help.";
  return Object.keys(errors).length === 0;
};

const onSubmit = async () => {
  if (busy.value || !validate()) return;
  busy.value = true;
  submitError.value = "";
  try {
    await api("post", "/contact", {
      ...form,
      subject: form.subject || `Message from ${form.name}`,
    });
    handedOff.value = true;
  } catch (e) {
    submitError.value = errorMessage(e);
  } finally {
    busy.value = false;
  }
};

const startOver = () => {
  Object.assign(form, blank());
  handedOff.value = false;
};
</script>

<template>
  <section
    class="section bg-background relative"
    aria-labelledby="contact-title"
  >
    <div class="container-magic">
      <div v-if="showHeading" v-reveal class="mb-12 max-w-2xl">
        <h2 id="contact-title" class="font-display text-display">
          Let's talk <span class="text-primary-700">cookies</span>
        </h2>
        <p class="prose-copy mt-4">
          Questions, custom orders or bulk boxes for an event. Our team will get
          back to you quickly.
        </p>
      </div>
      <h2 v-else id="contact-title" class="sr-only">Contact us</h2>

      <div
        v-reveal
        class="grid lg:grid-cols-[0.85fr_1.15fr] overflow-hidden rounded-[28px] bg-surface shadow-soft-lg"
      >
        <!-- Left: contact info -->
        <div
          class="relative isolate overflow-hidden bg-chocolate text-cream-100 p-7 sm:p-10 lg:p-12"
        >
          <h3 class="text-base text-cream-50">Contact information</h3>
          <p class="mt-2 text-cream-100/75">
            {{
              info?.responseTime ||
              "Send us a message and we’ll get back to you."
            }}
          </p>

          <ul class="mt-9 space-y-3">
            <li v-for="card in contactCards" :key="card.label">
              <a
                :href="card.href"
                :target="card.label === 'Address' ? '_blank' : undefined"
                :rel="card.label === 'Address' ? 'noopener' : undefined"
                class="group flex items-center gap-4 rounded-2xl border border-cream-50/10 bg-cream-50/[0.04] p-4 transition-colors hover:bg-cream-50/[0.09]"
              >
                <span
                  class="h-12 w-12 shrink-0 rounded-full bg-primary/20 text-primary flex items-center justify-center"
                >
                  <component :is="card.icon" class="h-5 w-5" />
                </span>
                <span class="min-w-0 flex-1">
                  <span class="block text-sm text-cream-100/65">{{
                    card.label
                  }}</span>
                  <span class="block font-semibold text-cream-50 break-words">{{
                    card.value
                  }}</span>
                  <span class="block text-sm text-cream-100/65">{{
                    card.sub
                  }}</span>
                </span>
                <ArrowUpRightIcon
                  class="h-5 w-5 shrink-0 text-cream-100/40 transition-colors group-hover:text-primary"
                />
              </a>
            </li>
          </ul>
        </div>

        <!-- Right: form -->
        <div class="p-7 sm:p-10 lg:p-12" aria-live="polite">
          <div
            v-if="handedOff"
            class="h-full flex flex-col items-start justify-center py-10"
          >
            <span
              class="h-14 w-14 rounded-full bg-primary text-chocolate flex items-center justify-center"
            >
              <CheckIcon class="h-7 w-7" />
            </span>
            <h3 class="mt-5 text-base">Message received</h3>
            <p class="mt-2 prose-copy">
              Thank you for getting in touch. {{ info?.responseTime }}
            </p>
            <BaseButton class="mt-7" variant="outline" @click="startOver"
              >Write a new message</BaseButton
            >
          </div>

          <template v-else>
            <h3 class="text-base">Send us a message</h3>
            <form class="mt-7 space-y-5" novalidate @submit.prevent="onSubmit">
              <div class="grid grid-cols-1 sm:grid-cols-2 gap-5">
                <BaseInput
                  v-model="form.name"
                  label="Your name"
                  placeholder="Sarah Ndlovu"
                  autocomplete="name"
                  :error="errors.name"
                  required
                />
                <BaseInput
                  v-model="form.email"
                  label="Your email"
                  type="email"
                  placeholder="sarah@example.co.za"
                  autocomplete="email"
                  :error="errors.email"
                  required
                />
              </div>
              <BaseInput
                v-model="form.subject"
                label="Subject"
                placeholder="Custom cookie order for an event"
              />
              <BaseInput
                v-model="form.message"
                label="Message"
                type="textarea"
                :rows="5"
                placeholder="Tell us how we can help"
                :error="errors.message"
                required
              />
              <div class="absolute -left-[10000px]" aria-hidden="true">
                <label
                  >Website<input
                    v-model="form.website"
                    name="website"
                    tabindex="-1"
                    autocomplete="off"
                /></label>
              </div>
              <BaseInput
                v-model="form.phone"
                label="Mobile number (optional)"
                type="tel"
                autocomplete="tel"
              />
              <p v-if="submitError" role="alert" class="text-red-800">
                {{ submitError }}
              </p>
              <BaseButton
                type="submit"
                variant="primary"
                size="lg"
                block
                :disabled="busy"
              >
                {{ busy ? "Sending…" : "Send message" }}
              </BaseButton>
              <p class="text-sm text-chocolate/75">
                We'll only use your details to reply to you. See our
                <RouterLink
                  to="/privacy-policy"
                  class="font-bold underline underline-offset-4"
                  >Privacy Policy</RouterLink
                >.
              </p>
            </form>
          </template>
        </div>
      </div>
    </div>
  </section>
</template>
