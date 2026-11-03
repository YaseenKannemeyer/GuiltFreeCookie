/**
 * Per-page <head>: title, description, canonical link, robots, social share
 * tags and structured data.
 *
 *   headForRoute(route, { catalog, cookie })  -> a plain description of the head
 *   renderHead(head)                          -> HTML, for prerendered pages
 *   applyHead(head)                           -> updates the live document
 *
 * Prerendering and client-side navigation both go through headForRoute, so a
 * page's head is the same whether it came from the server or the router.
 */
import {
  COOKIE_CATEGORIES,
  formatCategoryLabel,
  categoryBlurb,
} from "../constants/cookieCategories";
import { cookieImage, cookieName } from "../composables/useCatalog";
import { breadcrumbsFor } from "./breadcrumbs";
import { structuredData } from "./schema";
import {
  SITE_NAME,
  SITE_LOCALE,
  HOME_TITLE,
  HOME_DESCRIPTION,
  SHARE_IMAGE,
  absoluteUrl,
  summarize,
} from "./site";

const NOINDEX = "noindex, nofollow";

export const findCookie = (catalog, id) =>
  (catalog || []).find((c) => c.cookieId === id || c.slug === id) || null;

const rand = (price) => `R${Number(price || 0).toFixed(2)}`;
const titleFor = (title) => (title ? `${title} | ${SITE_NAME}` : HOME_TITLE);

function pageHead({
  route,
  title,
  description,
  type = "website",
  image = SHARE_IMAGE,
  cookie = null,
  extraMeta = [],
}) {
  const url = absoluteUrl(route.path);
  const fullTitle = titleFor(title);
  const crumbs = breadcrumbsFor(route, { cookie });
  return {
    title: fullTitle,
    description,
    canonical: url,
    robots: null,
    og: {
      type,
      url,
      title: fullTitle,
      description,
      image: { ...image, url: absoluteUrl(image.url) },
    },
    extraMeta,
    jsonLd: structuredData({
      route,
      url,
      title: fullTitle,
      description,
      crumbs,
      cookie,
    }),
  };
}

function privateHead(title, description = HOME_DESCRIPTION) {
  return {
    title: titleFor(title),
    description,
    canonical: null,
    robots: NOINDEX,
    og: null,
    extraMeta: [],
    jsonLd: null,
  };
}

function productHead(route, cookie) {
  const name = cookieName(cookie);
  const range = formatCategoryLabel(cookie.category);
  const copy = cookie.longDescription?.trim() || categoryBlurb(cookie.category);
  return pageHead({
    route,
    cookie,
    title: `${name} – ${range} cookie`,
    description: summarize(
      `${name}: a ${range.toLowerCase()} cookie, ${rand(cookie.price)}. ${copy}`,
    ),
    type: "product",
    image: { url: cookieImage(cookie), alt: name },
    extraMeta: [
      {
        property: "product:price:amount",
        content: Number(cookie.price).toFixed(2),
      },
      { property: "product:price:currency", content: "ZAR" },
    ],
  });
}

export function headForRoute(
  route,
  { catalog = [], cookie, missing = false } = {},
) {
  const meta = route.meta || {};

  if (meta.noindex) {
    return privateHead(meta.title, meta.description);
  }

  if (route.name === "ProductDetail") {
    const found = cookie || findCookie(catalog, route.params.id);
    if (found) return productHead(route, found);
    // Not found, or not loaded yet (the view applies the real head once it is).
    return missing
      ? privateHead(
          "Cookie not found",
          "We couldn't find that cookie. See the full range instead.",
        )
      : {
          ...pageHead({
            route,
            title: "Cookie",
            description: HOME_DESCRIPTION,
          }),
          jsonLd: null,
        };
  }

  if (
    route.name === "Products" &&
    COOKIE_CATEGORIES.includes(route.query?.category)
  ) {
    const label = formatCategoryLabel(route.query.category);
    return pageHead({
      route,
      title: `${label} cookies`,
      description: summarize(
        `${label} cookies from ${SITE_NAME}. ${categoryBlurb(route.query.category)} Baked fresh in small batches in Cape Town.`,
      ),
    });
  }

  return pageHead({
    route,
    title: meta.title,
    description: meta.description || HOME_DESCRIPTION,
  });
}

// ---- Rendering ------------------------------------------------------------

function metaTags(head) {
  const tags = [
    { tag: "meta", attrs: { name: "description", content: head.description } },
  ];
  if (head.robots)
    tags.push({ tag: "meta", attrs: { name: "robots", content: head.robots } });
  if (head.canonical)
    tags.push({
      tag: "link",
      attrs: { rel: "canonical", href: head.canonical },
    });

  if (head.og) {
    const { og } = head;
    const property = (name, content) =>
      content != null &&
      tags.push({
        tag: "meta",
        attrs: { property: name, content: String(content) },
      });
    const named = (name, content) =>
      content != null &&
      tags.push({ tag: "meta", attrs: { name, content: String(content) } });
    property("og:type", og.type);
    property("og:site_name", SITE_NAME);
    property("og:locale", SITE_LOCALE);
    property("og:url", og.url);
    property("og:title", og.title);
    property("og:description", og.description);
    property("og:image", og.image.url);
    property("og:image:width", og.image.width);
    property("og:image:height", og.image.height);
    property("og:image:alt", og.image.alt);
    named("twitter:card", "summary_large_image");
    named("twitter:title", og.title);
    named("twitter:description", og.description);
    named("twitter:image", og.image.url);
    named("twitter:image:alt", og.image.alt);
  }
  head.extraMeta.forEach((attrs) => tags.push({ tag: "meta", attrs }));
  return tags;
}

const escapeHtml = (value) =>
  String(value)
    .replace(/&/g, "&amp;")
    .replace(/"/g, "&quot;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;");

// JSON inside <script> must not be able to close the tag.
export const safeJson = (data) => JSON.stringify(data).replace(/</g, "\\u003c");

export function renderHead(head) {
  const tags = metaTags(head).map(({ tag, attrs }) => {
    const attributes = Object.entries(attrs)
      .map(([k, v]) => ` ${k}="${escapeHtml(v)}"`)
      .join("");
    return `<${tag} data-head${attributes}>`;
  });
  if (head.jsonLd) {
    tags.push(
      `<script type="application/ld+json" data-head>${safeJson(head.jsonLd)}</script>`,
    );
  }
  return [`<title>${escapeHtml(head.title)}</title>`, ...tags].join("\n    ");
}

export function applyHead(head) {
  if (typeof document === "undefined") return;
  document.title = head.title;
  document.head.querySelectorAll("[data-head]").forEach((el) => el.remove());

  const fragment = document.createDocumentFragment();
  metaTags(head).forEach(({ tag, attrs }) => {
    const el = document.createElement(tag);
    el.setAttribute("data-head", "");
    Object.entries(attrs).forEach(([k, v]) => el.setAttribute(k, v));
    fragment.appendChild(el);
  });
  if (head.jsonLd) {
    const script = document.createElement("script");
    script.type = "application/ld+json";
    script.setAttribute("data-head", "");
    script.textContent = JSON.stringify(head.jsonLd);
    fragment.appendChild(script);
  }
  document.head.appendChild(fragment);
}
