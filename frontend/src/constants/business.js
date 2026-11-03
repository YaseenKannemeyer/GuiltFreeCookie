/**
 * Business and legal facts used by the policy pages (Privacy, Terms,
 * Cookies, Refunds) and the footer. Keep them here so every page says the
 * same thing.
 *
 * Anything set to null renders as a highlighted "[to be completed]" blank on
 * the policy pages, so nothing is ever invented. Fill these in before launch:
 *   - legalName / registrationNumber: from your CIPC registration
 *   - vatNumber: only if you're registered for VAT with SARS (else leave null)
 *   - informationOfficer: by default the owner or head of the business; must
 *     also be registered with the Information Regulator
 */
const env = import.meta.env || {};
export const DEMO_MODE = env.VITE_DEMO_MODE !== "false";
const phone = env.VITE_SHOP_PHONE || "";
export const BUSINESS = {
  tradingName: "GuiltFreeCookie",
  legalName: env.VITE_BUSINESS_LEGAL_NAME || null,
  registrationNumber: env.VITE_BUSINESS_REGISTRATION_NUMBER || null,
  vatRegistered: env.VITE_VAT_REGISTERED === "true",
  vatNumber: env.VITE_VAT_NUMBER || null,
  informationOfficer: env.VITE_INFORMATION_OFFICER || null,
  email: env.VITE_SHOP_EMAIL || "hello@example.test",
  phone,
  phoneHref: `tel:${phone.replace(/[^+0-9]/g, "")}`,
  address: env.VITE_SHOP_ADDRESS || "Cape Town, South Africa (demo location)",
  // The same address split up for structured data (schema.org PostalAddress).
  postalAddress: {
    streetAddress: env.VITE_SHOP_STREET || "",
    addressLocality: "Cape Town",
    addressRegion: "Western Cape",
    postalCode: env.VITE_SHOP_POSTAL_CODE || "",
    addressCountry: "ZA",
  },
  mapsUrl: "https://maps.google.com/?q=Cape+Town,+South+Africa",

  // Full profile URLs. Leave null until the account exists: the footer and
  // structured data only show the ones that are set.
  socials: {
    instagram: null,
    facebook: null,
    x: null,
    linkedin: null,
  },

  // Allergens handled in the kitchen (cross-contact warning on the Terms).
  // Based on the current menu's ingredients; confirm against your kitchen.
  kitchenAllergens: ["tree nuts (including almonds)", "peanuts", "dairy", "eggs", "gluten"],
};

export const LEGAL = {
  lastUpdated: "3 November 2026",
  // Set to true once an attorney has reviewed the four policies. While false,
  // each policy page shows a short "draft" notice at the top.
  reviewed: env.VITE_POLICIES_REVIEWED === "true",
};

// Footer + "other policies" links, in one place.
export const LEGAL_PAGES = [
  { name: "PrivacyPolicy", label: "Privacy Policy", path: "/privacy-policy" },
  { name: "Terms", label: "Terms and Conditions", path: "/terms" },
  { name: "CookiePolicy", label: "Cookie Policy", path: "/cookie-policy" },
  { name: "RefundPolicy", label: "Refund Policy", path: "/refund-policy" },
];
