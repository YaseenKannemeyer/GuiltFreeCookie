<script setup>
/**
 * HomeView — assembles all homepage sections in order:
 * Hero → About → Featured → CTA → Why Choose Us → Testimonials → FAQ → Contact
 */
import HeroBanner from "../components/sections/HeroBanner.vue";
import AboutSection from "../components/sections/AboutSection.vue";
import FeaturedCookies from "../components/sections/FeaturedCookies.vue";
import CtaBanner from "../components/sections/CtaBanner.vue";
import WhyChooseUs from "../components/sections/WhyChooseUs.vue";
import FaqSection from "../components/sections/FaqSection.vue";
import ContactSection from "../components/sections/ContactSection.vue";
import QualitySection from "../components/sections/QualitySection.vue";
import CookieShowcase from "../components/sections/CookieShowcase.vue";
</script>

<template>
  <div>
    <HeroBanner />
    <QualitySection />
    <CookieShowcase />
    <FeaturedCookies />
    <CtaBanner />
    <ContactSection />
  </div>
</template>
