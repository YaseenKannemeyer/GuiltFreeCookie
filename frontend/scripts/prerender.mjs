/**
 * Turns the built single-page app into real HTML pages.
 *
 *   npm run build   (client build -> SSR build -> this script)
 *
 * For every public page it renders the app to HTML with that page's own
 * <head> (title, description, canonical, share tags, structured data), so
 * "view source", search engines and link previews all see the finished page.
 * The browser then hydrates it. It also writes 404.html, sitemap.xml,
 * robots.txt and llms.txt.
 *
 * Product pages need the live catalogue. Set PRERENDER_API_URL to the API
 * (e.g. https://guiltfreecookie.co.za/guiltFreeCookie) when building; without
 * it the build still works, but product pages are only rendered in the
 * browser and are left out of the sitemap.
 */
import fs from "node:fs/promises";
import path from "node:path";
import { fileURLToPath, pathToFileURL } from "node:url";
import { loadEnv } from "vite";

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");
const dist = path.join(root, "dist");
const ssrDist = path.join(root, "dist-ssr");
const env = loadEnv("production", root, "");

const yellow = (text) => `\x1b[33m${text}\x1b[0m`;

const template = await fs.readFile(path.join(dist, "index.html"), "utf8");
const manifest = JSON.parse(
  await fs.readFile(path.join(dist, ".vite", "ssr-manifest.json"), "utf8"),
);
const server = await import(
  pathToFileURL(path.join(ssrDist, "entry-server.js")).href
);

const requireCatalog = (process.env.REQUIRE_CATALOG || env.REQUIRE_CATALOG) === "true";
const waitSeconds = Number(process.env.PRERENDER_WAIT_SECONDS || env.PRERENDER_WAIT_SECONDS || (process.env.VERCEL ? 600 : 0));
let catalog = await fetchCatalog(env.PRERENDER_API_URL);
// A push to staging deploys the API and the storefront at the same time, so wait out an API restart.
for (const started = Date.now(); requireCatalog && env.PRERENDER_API_URL && !catalog?.length
    && Date.now() - started < waitSeconds * 1000;) {
  console.warn(yellow("Retrying the catalogue in 20 seconds."));
  await new Promise((resolve) => setTimeout(resolve, 20_000));
  catalog = await fetchCatalog(env.PRERENDER_API_URL);
}
if (requireCatalog && !catalog?.length) {
  throw new Error("A complete catalogue is required for this build. Start the demo API or configure PRERENDER_API_URL.");
}
const pages = [...server.indexablePaths(catalog || []), "/404"];

for (const url of pages) {
  const page = await server.render(url, { catalog });
  await write(fileFor(url), inject(page));
}

await write("sitemap.xml", server.sitemapXml(catalog || []));
await write("robots.txt", server.robotsTxt());
await write("llms.txt", server.llmsTxt(catalog || []));

// Build-only artefacts: nothing to publish.
await fs.rm(path.join(dist, ".vite"), { recursive: true, force: true });
await fs.rm(ssrDist, { recursive: true, force: true });

console.log(
  `Prerendered ${pages.length} pages for ${server.SITE_URL}${catalog ? ` (${catalog.length} cookies)` : ""}.`,
);

async function fetchCatalog(apiUrl) {
  if (!apiUrl) {
    console.warn(
      yellow(
        "PRERENDER_API_URL is not set: product pages will be rendered in the browser only and left out of the sitemap.",
      ),
    );
    return null;
  }
  try {
    const base = apiUrl.replace(/\/+$/, "");
    const catalog = [];
    let page = 0;
    let totalPages = 1;
    while (page < totalPages) {
      const response = await fetch(
        `${base}/api/v1/cookies?page=${page}&size=48`,
        { signal: AbortSignal.timeout(10_000) },
      );
      if (!response.ok) throw new Error(`HTTP ${response.status}`);
      const data = await response.json();
      if (!Array.isArray(data.items)) throw new Error("unexpected response");
      for (const item of data.items) {
        const detailResponse = await fetch(
          `${base}/api/v1/cookies/${encodeURIComponent(item.slug || item.cookieId)}`,
          { signal: AbortSignal.timeout(10_000) },
        );
        if (!detailResponse.ok)
          throw new Error(`HTTP ${detailResponse.status}`);
        const detail = await detailResponse.json();
        catalog.push({
          ...detail,
          description: detail.name,
          price: detail.fromPrice,
        });
      }
      totalPages = data.totalPages;
      page++;
    }
    return catalog;
  } catch (error) {
    console.warn(
      yellow(
        `Couldn't load the catalogue from ${apiUrl} (${error.message}): product pages will be rendered in the browser only.`,
      ),
    );
    return null;
  }
}

function inject({ html, head, teleports, modules, catalogJson, route }) {
  const extras = [
    preloadLinks(modules),
    catalogJson
      ? `<script type="application/json" id="catalog-data">${catalogJson}</script>`
      : "",
  ].filter(Boolean);

  // Function replacements: rendered text may contain "$&" and friends.
  return template
    .replace("<!--app-head-->", () => [head, ...extras].join("\n    "))
    .replace(
      '<div id="app">',
      () =>
        `<div id="app" data-ssr-path="${route.path}" data-ssr-route="${route.name}">`,
    )
    .replace("<!--app-html-->", () => html)
    .replace("<!--app-teleports-->", () => teleports);
}

// The page's own JS and CSS chunks, so it styles at once and hydrates without
// waiting on a chain of imports.
function preloadLinks(modules) {
  const seen = new Set();
  const links = [];
  for (const id of modules) {
    for (const file of manifest[id] || []) {
      if (seen.has(file)) continue;
      seen.add(file);
      if (file.endsWith(".js"))
        links.push(`<link rel="modulepreload" crossorigin href="${file}">`);
      else if (file.endsWith(".css"))
        links.push(`<link rel="stylesheet" crossorigin href="${file}">`);
    }
  }
  return links.join("\n    ");
}

// "/about" -> about.html: static hosts (Netlify, Cloudflare Pages, Vercel,
// GitHub Pages, `vite preview`) serve it at the clean URL /about, matching
// the canonical links.
function fileFor(url) {
  if (url === "/") return "index.html";
  return `${url.replace(/^\//, "")}.html`;
}

async function write(relative, contents) {
  const target = path.join(dist, relative);
  await fs.mkdir(path.dirname(target), { recursive: true });
  await fs.writeFile(target, contents);
}
