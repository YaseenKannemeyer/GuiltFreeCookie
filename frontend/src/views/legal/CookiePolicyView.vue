<script setup>
/**
 * Cookie Policy — lists exactly what this website stores in the browser.
 * Keep the `storage` table in sync with the code: if the cart feature (or
 * anything else) adds cookies or localStorage keys, add a row here.
 */
import { RouterLink } from "vue-router";
import LegalPage from "../../components/legal/LegalPage.vue";
import LegalSection from "../../components/legal/LegalSection.vue";
import { BUSINESS } from "../../constants/business";

const sections = [
  { id: "what-they-are", title: "What cookies and browser storage are" },
  { id: "what-we-use", title: "What we use" },
  { id: "what-we-dont", title: "What we don't use" },
  { id: "other-sites", title: "Other websites" },
  { id: "control", title: "Managing your settings" },
  { id: "changes", title: "Changes and contact" },
];

const summary = [
  "Shopping on our website doesn't put any cookies or trackers in your browser.",
  "The only thing we store is a sign-in session for our own staff on the admin pages.",
  "No advertising, no analytics, and no third-party trackers.",
];

// Source of truth: src/composables/useAdminAuth.js (STORAGE_KEY) and the
// backend's app.jwt.expiration-minutes (1440 = 24 hours).
const storage = [
  {
    name: "gfc_admin_session",
    type: "Local storage or session storage",
    who: "Staff only, on the admin pages",
    purpose: "Keeps a staff member signed in to the admin dashboard.",
    duration:
      "Until they log out. Session storage is cleared when the tab closes; local storage is used only if they tick \"Keep me logged in\". The sign-in itself expires after 24 hours.",
  },
];
</script>

<template>
  <LegalPage title="Cookie Policy" :summary="summary" :sections="sections">
    <LegalSection id="what-they-are" :number="1" title="What cookies and browser storage are">
      <p>
        Cookies are small files a website saves in your browser. "Local storage" and "session storage" do a
        similar job: they let a website remember something between pages or visits, such as whether you're
        signed in. This policy covers all of them.
      </p>
    </LegalSection>

    <LegalSection id="what-we-use" :number="2" title="What we use">
      <p>
        <strong>If you're shopping or browsing, we don't store anything in your browser.</strong> Filters on
        our product list live in the page address, not in cookies.
      </p>
      <p>The only item we store is strictly necessary for our staff to use the admin dashboard:</p>

      <!-- Table on larger screens, stacked cards on phones -->
      <div class="hidden 2xl:block !max-w-none overflow-hidden rounded-2xl border border-chocolate/15">
        <table class="w-full text-left">
          <thead class="bg-chocolate/[0.06] text-sm">
            <tr>
              <th scope="col" class="p-4 font-bold">Name</th>
              <th scope="col" class="p-4 font-bold">Type</th>
              <th scope="col" class="p-4 font-bold">Purpose</th>
              <th scope="col" class="p-4 font-bold">How long</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-chocolate/10 align-top">
            <tr v-for="item in storage" :key="item.name">
              <td class="p-4 font-bold text-chocolate whitespace-nowrap">{{ item.name }}</td>
              <td class="p-4">{{ item.type }}<span class="mt-1 block text-sm leading-snug text-chocolate/75">{{ item.who }}</span></td>
              <td class="p-4">{{ item.purpose }}</td>
              <td class="p-4">{{ item.duration }}</td>
            </tr>
          </tbody>
        </table>
      </div>
      <dl v-for="item in storage" :key="item.name" class="2xl:hidden rounded-2xl border border-chocolate/15 p-5 space-y-3 !max-w-none">
        <div><dt class="text-sm font-bold">Name</dt><dd class="font-bold text-chocolate break-all">{{ item.name }}</dd></div>
        <div><dt class="text-sm font-bold">Type</dt><dd>{{ item.type }} ({{ item.who.toLowerCase() }})</dd></div>
        <div><dt class="text-sm font-bold">Purpose</dt><dd>{{ item.purpose }}</dd></div>
        <div><dt class="text-sm font-bold">How long</dt><dd>{{ item.duration }}</dd></div>
      </dl>

      <p>
        Because this is strictly necessary for a service our staff ask for (signing in), it doesn't need
        separate consent, which is why you won't see a cookie banner on our site.
      </p>
    </LegalSection>

    <LegalSection id="what-we-dont" :number="3" title="What we don't use">
      <ul>
        <li>No advertising or retargeting cookies.</li>
        <li>No analytics or tracking tools.</li>
        <li>No social media tracking pixels.</li>
        <li>
          No third-party font or icon services: our fonts are served from our own website, so loading a page
          doesn't send your details to anyone else.
        </li>
      </ul>
    </LegalSection>

    <LegalSection id="other-sites" :number="4" title="Other websites">
      <p>
        Some links take you to other websites, such as Google Maps for our address or our social media
        pages. Those sites may set their own cookies, which are covered by their own policies, not this one.
      </p>
    </LegalSection>

    <LegalSection id="control" :number="5" title="Managing your settings">
      <p>
        You can see and delete cookies and stored site data in your browser's settings, usually under
        "Privacy" or "Site data". Clearing it won't affect shopping on our site; for staff, it simply signs
        them out of the admin dashboard.
      </p>
    </LegalSection>

    <LegalSection id="changes" :number="6" title="Changes and contact">
      <p>
        If we start using anything new, such as saving your cart between visits or adding analytics, we'll
        update this page first and ask for your consent wherever it's needed.
      </p>
      <p>
        Questions? Email <a :href="`mailto:${BUSINESS.email}`">{{ BUSINESS.email }}</a>, or read our
        <RouterLink to="/privacy-policy">Privacy Policy</RouterLink> for how we handle personal information.
      </p>
    </LegalSection>
  </LegalPage>
</template>
