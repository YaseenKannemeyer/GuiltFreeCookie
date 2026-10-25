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
export const BUSINESS = {
  tradingName: "GuiltFreeCookie",
  legalName: null,
  registrationNumber: null,
  vatNumber: null,
  informationOfficer: null,
  email: "hello@guiltfreecookie.co.za",
  phone: "+27 82 851 3174",
  phoneHref: "tel:+27828513174",
  address: "41 Caravel Road, Strandfontein, Cape Town, 7798",
  // The same address split up for structured data (schema.org PostalAddress).
  postalAddress: {
    streetAddress: "41 Caravel Road, Strandfontein",
    addressLocality: "Cape Town",
    addressRegion: "Western Cape",
    postalCode: "7798",
    addressCountry: "ZA",
  },
  mapsUrl: "https://maps.google.com/?q=41+Caravel+Road,+Strandfontein,+Cape+Town,+7798",

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
  lastUpdated: "25 October 2026",
  // Set to true once an attorney has reviewed the four policies. While false,
  // each policy page shows a short "draft" notice at the top.
  reviewed: false,
};

// Footer + "other policies" links, in one place.
export const LEGAL_PAGES = [
  { name: "PrivacyPolicy", label: "Privacy Policy", path: "/privacy-policy" },
  { name: "Terms", label: "Terms and Conditions", path: "/terms" },
  { name: "CookiePolicy", label: "Cookie Policy", path: "/cookie-policy" },
  { name: "RefundPolicy", label: "Refund Policy", path: "/refund-policy" },
];
