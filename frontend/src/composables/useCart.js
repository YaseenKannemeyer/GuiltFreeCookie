/**
 * useCart — the shopper's cart, shared by the navbar, product page, cart and checkout.
 * The cart id lives in localStorage so a guest keeps their cart; the server prices everything.
 */
import { ref, computed } from "vue";
import { cartApi } from "../api/storeApi";

const KEY = "gfc_cart_id";
const CHECKOUT_KEY = "gfc_checkout_cart";
const cart = ref(null);
const ready = ref(false);

function storedId() {
  try {
    return localStorage.getItem(KEY);
  } catch {
    return null;
  }
}
function storeId(id) {
  try {
    if (id) localStorage.setItem(KEY, id);
    else localStorage.removeItem(KEY);
  } catch {
    /* Storage blocked: the cart lasts for this visit only. */
  }
}

// A cart that was checked out, cleared or deleted can't take more items: forget it.
// (A 404 for an unavailable pack or a 409 for stock is about the item, not the cart.)
const isGone = (e) => {
  const status = e?.response?.status;
  const message = e?.response?.data?.message || "";
  return (status === 404 && message === "Cart not found") || (status === 409 && message.includes("checked out"));
};

function adopt(view) {
  if (view?.status && view.status !== "ACTIVE") {
    storeId(null);
    cart.value = null;
    return null;
  }
  cart.value = view;
  if (view?.cartId) storeId(view.cartId);
  return view;
}

async function load() {
  const id = cart.value?.cartId || storedId();
  try {
    if (id) adopt(await cartApi.get(id));
  } catch (e) {
    if (isGone(e)) adopt({ status: "GONE" });
    else throw e;
  } finally {
    ready.value = true;
  }
  return cart.value;
}

async function ensureId() {
  if (cart.value?.cartId) return cart.value.cartId;
  const id = storedId();
  if (id) return id;
  return adopt(await cartApi.create()).cartId;
}

// Runs a change against the cart; a stale cart is replaced once and the change retried.
async function change(run) {
  try {
    return adopt(await run(await ensureId()));
  } catch (e) {
    if (!isGone(e)) throw e;
    adopt({ status: "GONE" });
    return adopt(await run(await ensureId()));
  }
}

export function useCart() {
  return {
    cart,
    ready,
    itemCount: computed(() => cart.value?.itemCount || 0),
    load,
    add: (variantId, quantity) => change((id) => cartApi.addItem(id, variantId, quantity)),
    setQuantity: (itemId, quantity) => change((id) => cartApi.setQuantity(id, itemId, quantity)),
    remove: (itemId) => change((id) => cartApi.removeItem(id, itemId)),
    applyDiscount: (code) => change((id) => cartApi.applyDiscount(id, code)),
    removeDiscount: () => change((id) => cartApi.removeDiscount(id)),
    setEmail: (email) => change((id) => cartApi.setEmail(id, email)),
    // After login: the guest cart joins the account cart; use whichever id comes back.
    async merge() {
      const id = cart.value?.cartId || storedId();
      if (!id) return null;
      try {
        return adopt(await cartApi.merge(id));
      } catch (e) {
        if (isGone(e)) return adopt({ status: "GONE" });
        throw e;
      }
    },
    // Called once an order is placed. The id is kept aside: a cancelled payment reopens that cart.
    forget() {
      try {
        if (cart.value?.cartId) sessionStorage.setItem(CHECKOUT_KEY, cart.value.cartId);
      } catch {
        /* Without storage the shopper starts a fresh cart after cancelling. */
      }
      adopt({ status: "CHECKED_OUT" });
    },
    async restore() {
      let id = null;
      try {
        id = sessionStorage.getItem(CHECKOUT_KEY);
        sessionStorage.removeItem(CHECKOUT_KEY);
      } catch {
        /* Nothing to restore. */
      }
      if (id) storeId(id);
      return load();
    },
    isGone,
  };
}
