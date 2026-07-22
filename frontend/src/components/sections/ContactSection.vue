<script setup>
/**
 * ContactSection — split layout for the homepage.
 * Left (dark chocolate): contact info cards (phone, email, address).
 * Right (cream): Get In Touch form.
 *
 * The standalone Contact page reuses this same component for consistency.
 */
import { ref } from "vue";
import { PhoneIcon, EnvelopeIcon, MapPinIcon } from "@heroicons/vue/24/outline";
import BaseInput from "../../components/ui/BaseInput.vue";
import BaseButton from "../../components/ui/BaseButton.vue";
import { useScrollReveal } from "../../composables/useScrollReveal";

const { vReveal } = useScrollReveal();

const form = ref({
  name: "",
  email: "",
  subject: "",
  message: "",
});

const submitting = ref(false);

const contactCards = [
  {
    icon: PhoneIcon,
    label: "Phone",
    value: "0114 567 987",
    sub: "0112 345 678",
  },
  {
    icon: EnvelopeIcon,
    label: "Email",
    value: "crumblemagic@gmail.com",
    sub: "We reply within 24 hours",
  },
  {
    icon: MapPinIcon,
    label: "Address",
    value: "376/2, Main Road",
    sub: "Colombo 08, Sri Lanka",
  },
];

const onSubmit = async () => {
  if (!form.value.name || !form.value.email || !form.value.message) {
    alert("Please complete all required fields.");
    return;
  }

  submitting.value = true;

  await new Promise((resolve) => setTimeout(resolve, 800));

  submitting.value = false;

  alert("Message sent! We will be in touch shortly.");

  form.value = {
    name: "",
    email: "",
    subject: "",
    message: "",
  };
};
</script>

<template>
  <section class="section bg-background relative overflow-hidden">
    <div class="container-magic">
      <div v-reveal class="text-center mb-14 max-w-2xl mx-auto">
        <span class="eyebrow">Get In Touch</span>
        <h2 class="text-chocolate leading-tight">
          Let's Talk <span class="text-primary">Cookies</span>
        </h2>
        <p class="text-chocolate/70 text-card mt-4">
          For inquiries, custom orders, or support, feel free to reach out. Our
          team will connect with you promptly!
        </p>
      </div>

      <div
        class="grid lg:grid-cols-2 rounded-5xl overflow-hidden shadow-soft-lg bg-surface"
      >
        <!-- Left: contact info (dark chocolate) -->
        <div
          class="bg-chocolate text-cream p-8 lg:p-12 relative overflow-hidden texture-dark"
        >
          <h3 class="text-2xl font-semibold text-cream mb-2">
            Contact Information
          </h3>
          <p class="text-cream/70 text-card mb-10">
            Reach out through any of the channels below — we usually respond
            within a few hours.
          </p>

          <div class="space-y-4">
            <div
              v-for="card in contactCards"
              :key="card.label"
              class="flex items-center gap-4 bg-chocolate-400/40 border border-cream/10 rounded-3xl p-4 hover:bg-chocolate-300/50 transition-all duration-300"
            >
              <span
                class="flex items-center justify-center h-12 w-12 rounded-full bg-primary/20 text-primary shrink-0"
              >
                <component :is="card.icon" class="h-5 w-5" />
              </span>
              <div>
                <div class="text-cream/60 text-sm uppercase tracking-wider">
                  {{ card.label }}
                </div>
                <div class="text-cream font-semibold text-lg">
                  {{ card.value }}
                </div>
                <div class="text-cream/50 text-sm">{{ card.sub }}</div>
              </div>
            </div>
          </div>

          <!-- Decorative cookie watermark -->
          <div
            class="pointer-events-none absolute -bottom-16 -left-16 w-64 h-64 rounded-full bg-primary/5 blur-2xl"
          />
        </div>

        <!-- Right: form (cream) -->
        <div class="bg-cream-50/50 p-8 lg:p-12">
          <h3 class="text-2xl font-semibold text-chocolate mb-6">
            Send us a message
          </h3>
          <form class="space-y-5" @submit.prevent="onSubmit">
            <div class="grid sm:grid-cols-2 gap-5">
              <BaseInput
                v-model="form.name"
                label="Your Name"
                placeholder="Rusiru Devinda"
                required
              />
              <BaseInput
                v-model="form.email"
                label="Your Email"
                type="email"
                placeholder="rusiru@gmail.com"
                required
              />
            </div>
            <BaseInput
              v-model="form.subject"
              label="Subject"
              placeholder="Custom cookie order for a wedding"
            />
            <BaseInput
              v-model="form.message"
              label="Message"
              type="textarea"
              :rows="5"
              placeholder="Tell us how we can help..."
              required
            />
            <BaseButton
              type="submit"
              variant="primary"
              size="lg"
              block
              :loading="submitting"
            >
              {{ submitting ? "Sending..." : "Send Message" }}
            </BaseButton>
          </form>
        </div>
      </div>
    </div>
  </section>
</template>
