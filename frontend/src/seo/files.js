/**
 * The crawler-facing files written at build time (see scripts/prerender.mjs):
 * sitemap.xml, robots.txt and llms.txt, plus the list of pages to prerender.
 */
import { routes } from "../router/routes";
import { BUSINESS, LEGAL_PAGES } from "../constants/business";
import { formatCategoryLabel } from "../constants/cookieCategories";
import { allergenText, cookieName } from "../composables/useCatalog";
import { SITE_NAME, SITE_URL, HOME_DESCRIPTION, absoluteUrl } from "./site";

const staticPaths = () =>
  routes.filter((r) => r.meta?.prerender).map((r) => r.path);
const productPath = (cookie) => `/product/${cookie.slug || cookie.cookieId}`;

/** Every page with real content: what gets prerendered and listed in the sitemap. */
export function indexablePaths(catalog = []) {
  return [...staticPaths(), ...catalog.map(productPath)];
}

const escapeXml = (value) =>
  String(value)
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;");

export function sitemapXml(catalog = []) {
  const urls = indexablePaths(catalog)
    .map(
      (path) =>
        `  <url>\n    <loc>${escapeXml(absoluteUrl(path))}</loc>\n  </url>`,
    )
    .join("\n");
  return `<?xml version="1.0" encoding="UTF-8"?>\n<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">\n${urls}\n</urlset>\n`;
}

export function robotsTxt() {
  return [
    "User-agent: *",
    "Allow: /",
    // Staff pages: nothing for search engines there, and they're noindexed too.
    "Disallow: /admin",
    "",
    `Sitemap: ${SITE_URL}/sitemap.xml`,
    "",
  ].join("\n");
}

const pageTitle = (path) => routes.find((r) => r.path === path)?.meta?.title;

/** A plain-language map of the site for AI assistants (https://llmstxt.org). */
export function llmsTxt(catalog = []) {
  const link = (label, path, note) =>
    `- [${label}](${absoluteUrl(path)})${note ? `: ${note}` : ""}`;
  const rand = (price) => `R${Number(price || 0).toFixed(2)}`;

  const lines = [
    `# ${SITE_NAME}`,
    "",
    `> ${HOME_DESCRIPTION} Prices are in South African rand (ZAR).`,
    "",
    "Ingredients and allergens differ between recipes; check the individual product page before ordering. Nutrition is shown only when available for that recipe. Range names are not medical advice or guarantees about traces of allergens. Contact the bakery for product questions.",
    "",
    "Online checkout is not live yet. For custom and bulk orders, contact us.",
    "",
    "## Pages",
    "",
    link(
      "Home",
      "/",
      "Our ranges, featured cookies and answers to common questions about sweeteners, carbs, allergens and storage",
    ),
    link(
      "All cookies",
      "/products",
      "The full range with prices, filterable by diabetic friendly, high protein, gluten free and vegan",
    ),
    link(
      pageTitle("/about") || "About us",
      "/about",
      "Why we bake the way we do and who our cookies are for",
    ),
    link(
      "Contact",
      "/contact",
      "Custom orders, bulk boxes for events and questions",
    ),
  ];

  if (catalog.length) {
    lines.push("", "## Cookies", "");
    catalog.forEach((cookie) => {
      const allergens = allergenText(cookie);
      const note = [
        `${formatCategoryLabel(cookie.category)}, ${rand(cookie.price)}.`,
        cookie.longDescription?.trim(),
        allergens && `Allergens: ${allergens}.`,
      ]
        .filter(Boolean)
        .join(" ");
      lines.push(link(cookieName(cookie), productPath(cookie), note));
    });
  }

  lines.push(
    "",
    "## Policies",
    "",
    ...LEGAL_PAGES.map((page) => link(page.label, page.path)),
    "",
    "## Contact",
    "",
    `- Phone and WhatsApp: ${BUSINESS.phone}`,
    `- Email: ${BUSINESS.email}`,
    `- Address: ${BUSINESS.address}`,
    "",
  );

  return lines.join("\n");
}
