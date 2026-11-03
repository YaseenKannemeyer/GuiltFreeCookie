// Mirrors za.ac.cput.guiltfreecookie.domain.CookieCategory on the backend.
export const COOKIE_CATEGORIES = [
  "LOW_SUGAR",
  "DIABETIC_FRIENDLY",
  "HIGH_PROTEIN",
  "GLUTEN_FREE",
  "VEGAN",
  "CLASSIC",
];

// Customer-facing copy per category. `short` is the chip label, `blurb`
// is the one-liner shown on the product page and home page.
export const CATEGORY_META = {
  LOW_SUGAR: {
    short: "Low sugar",
    blurb: "All the chocolate, a lot less sugar.",
    tint: "#E7CFA9",
  },
  DIABETIC_FRIENDLY: {
    short: "Diabetic friendly",
    blurb: "Check the recipe, ingredients and nutrition for your choice.",
    tint: "#D9C3A5",
  },
  HIGH_PROTEIN: {
    short: "High protein",
    blurb: "A proper treat that pulls its weight after training.",
    tint: "#E2B98A",
  },
  GLUTEN_FREE: {
    short: "Gluten free",
    blurb: "Made without wheat, still soft in the middle.",
    tint: "#EAD8B8",
  },
  VEGAN: {
    short: "Vegan",
    blurb: "No dairy, no eggs, no one can tell.",
    tint: "#DCC9A0",
  },
  CLASSIC: {
    short: "Classic",
    blurb: "The cookie-jar favourite, made our way.",
    tint: "#D0AB7B",
  },
};

export const formatCategoryLabel = (category) =>
  CATEGORY_META[category]?.short ??
  (category ? category.replace(/_/g, " ").toLowerCase().replace(/^\w/, (c) => c.toUpperCase()) : "");

export const categoryBlurb = (category) => CATEGORY_META[category]?.blurb ?? "";

export const categoryTint = (category) => CATEGORY_META[category]?.tint ?? "#E2D9C8";

// Brand cut-out shots in /public/img, used when a product has no upload yet.
export const FALLBACK_IMAGES = [
  "/img/cookie.webp",
  "/img/cookie2.webp",
  "/img/cookie3.webp",
  "/img/cookie4.webp",
];

export const fallbackImageFor = (key = "") => {
  let hash = 0;
  for (const ch of String(key)) hash = (hash * 31 + ch.charCodeAt(0)) >>> 0;
  return FALLBACK_IMAGES[hash % FALLBACK_IMAGES.length];
};
