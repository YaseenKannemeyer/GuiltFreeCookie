/**
 * Site-wide SEO facts: the public URL, the brand name and the defaults every
 * page falls back to. VITE_SITE_URL is the domain the site is served from;
 * canonical links, the sitemap and share previews are all built on it.
 */
export const SITE_URL = (import.meta.env.VITE_SITE_URL || (import.meta.env.VITE_DEMO_MODE !== "false" ? "http://127.0.0.1:5173" : "https://guiltfreecookie.co.za")).replace(/\/+$/, "");
export const SITE_NAME = "GuiltFreeCookie";
export const SITE_LOCALE = "en_ZA";
export const SITE_LANGUAGE = "en-ZA";

export const HOME_TITLE = `${SITE_NAME} | Low-sugar cookies baked in Cape Town`;
export const HOME_DESCRIPTION =
  "Chocolatey, properly indulgent cookies with less sugar. Diabetic friendly, high protein, gluten free and vegan ranges, baked in small batches in Cape Town.";

export const SHARE_IMAGE = {
  url: "/og-image.jpg",
  width: 1200,
  height: 630,
  alt: 'The GuiltFreeCookie logo and the words "Why feel guilty?" beside a half-chocolate, half-vanilla swirl cookie with milk and chocolate splashing around it',
};

export const absoluteUrl = (path = "/") =>
  /^https?:\/\//i.test(path) ? path : `${SITE_URL}${path.startsWith("/") ? "" : "/"}${path}`;

// Meta descriptions are cut off around 155-160 characters in results.
export function summarize(text, max = 155) {
  const clean = String(text || "").replace(/\s+/g, " ").trim();
  if (clean.length <= max) return clean;
  const cut = clean.slice(0, max - 1);
  return `${cut.slice(0, cut.lastIndexOf(" ")).replace(/[\s,;:.]+$/, "")}…`;
}
