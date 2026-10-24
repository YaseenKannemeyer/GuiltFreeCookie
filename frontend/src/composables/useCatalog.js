/**
 * useCatalog — the public cookie list, fetched once and shared by the
 * home page, product list and product detail page.
 *
 *   const { cookies, loading, error, load } = useCatalog()
 *   onMounted(load)            // no-op once loaded (refreshes a prerendered seed)
 *   load({ force: true })      // refetch (e.g. "Try again")
 */
import { ref } from "vue";
import cookieApi from "../api/cookieApi";
import { catalogApi } from "../api/storeApi";
import { fallbackImageFor } from "../constants/cookieCategories";

const cookies = ref([]);
const loading = ref(false);
const error = ref(null);
const loaded = ref(false);
let inflight = null;
let stale = false;

// Prerendered pages ship the catalogue they were built with, so the first
// render matches the HTML exactly. It may be out of date by now, so the
// first load() still refreshes it, quietly, without showing a skeleton.
export function seedCatalog(data) {
  cookies.value = Array.isArray(data) ? data : [];
  loaded.value = true;
  stale = true;
}

async function load({ force = false } = {}) {
  if (loaded.value && !force && !stale) return cookies.value;
  if (inflight) return inflight;

  stale = false;
  loading.value = true;
  error.value = null;
  inflight = catalogApi
    .search({ featured: true, size: 48 })
    .then((data) => {
      cookies.value = data.items.map((c) => ({
        ...c,
        description: c.name,
        price: c.fromPrice,
      }));
      loaded.value = true;
      return cookies.value;
    })
    .catch((err) => {
      error.value = err;
      return [];
    })
    .finally(() => {
      loading.value = false;
      inflight = null;
    });
  return inflight;
}

// Uploaded image if there is one, otherwise a stable brand cut-out.
export function cookieImage(cookie) {
  return (
    cookieApi.getImageUrl(cookie?.image) || fallbackImageFor(cookie?.cookieId)
  );
}

// <img @error> handler: swap a failed upload for the brand cut-out (once).
export function imageFallback(cookie) {
  return (event) => {
    const img = event.target;
    if (img.dataset.fallback) return;
    img.dataset.fallback = "1";
    img.src = fallbackImageFor(cookie?.cookieId);
  };
}

// Allergen text worth showing, or null when the field is empty / "none".
// "Contains dairy, eggs." -> "Dairy, eggs" so labels can add their own prefix.
export function allergenText(cookie) {
  let text = (cookie?.allergies || "").trim();
  if (!text || /^(none|n\/?a|no(ne)? known|-)\.?$/i.test(text)) return null;
  text = text.replace(/^contains:?\s+/i, "").replace(/\.+$/, "");
  return text.charAt(0).toUpperCase() + text.slice(1);
}

// Display name: trims stray trailing full stops from admin-entered names.
export function cookieName(cookie) {
  return (cookie?.name || cookie?.description || "").trim().replace(/\.+$/, "");
}

// Homepage favourites by name, chosen so neighbouring cards look different.
// The first four cover one range each (hot picks), the next two the signature drops.
export const HOME_FAVOURITES = [
  "Double Chocolate Chunk",
  "Cinnamon Pecan",
  "Lemon Almond",
  "Raspberry Coconut",
  "Vanilla White Chocolate",
  "Peanut Chocolate Ripple",
];

// One cookie per range: favourites first, then anything with a photo, then the rest.
export function onePerCategory(list) {
  const rank = (c) => {
    const i = HOME_FAVOURITES.findIndex(
      (n) => n.toLowerCase() === cookieName(c).toLowerCase(),
    );
    return i === -1 ? HOME_FAVOURITES.length + (c.image ? 0 : 1) : i;
  };
  const seen = new Set();
  return [...list]
    .sort((a, b) => rank(a) - rank(b))
    .filter((c) => !seen.has(c.category) && seen.add(c.category));
}

export function useCatalog() {
  return { cookies, loading, error, loaded, load };
}
