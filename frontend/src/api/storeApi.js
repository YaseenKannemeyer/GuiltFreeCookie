import axios from "axios";
import { API_BASE } from "./http";
export const api = async (method, path, data, config = {}) =>
  (await axios({ method, url: `${API_BASE}/api/v1${path}`, data, timeout: 15000, ...config }))
    .data;
export const errorMessage = (e) =>
  e.response?.status === 429
    ? `Too many attempts. Try again in ${e.response.headers?.["retry-after"] || 60} seconds.`
    : e.response?.data?.message ||
      (typeof e.response?.data === "string"
        ? e.response.data
        : "We couldn't connect. Please try again.");
export const catalogApi = {
  search: (params, signal) =>
    api("get", "/cookies", undefined, { params, signal }),
  detail: (id) => api("get", `/cookies/${encodeURIComponent(id)}`),
  categories: () => api("get", "/cookies/categories"),
};
export const cartApi = {
  create: () => api("post", "/carts"),
  // area ({ suburb, city, province, postalCode }) prices the courier for that area.
  get: (cartId, shippingMethod, area) =>
    api("get", `/carts/${cartId}`, undefined, {
      params: shippingMethod ? { shippingMethod, ...(area || {}) } : undefined,
    }),
  shippingOptions: (cartId, area) =>
    api("get", `/carts/${cartId}/shipping-options`, undefined, { params: area || undefined }),
  addItem: (cartId, variantId, quantity) =>
    api("post", `/carts/${cartId}/items`, { variantId, quantity }),
  setQuantity: (cartId, itemId, quantity) =>
    api("put", `/carts/${cartId}/items/${itemId}`, { quantity }),
  removeItem: (cartId, itemId) =>
    api("delete", `/carts/${cartId}/items/${itemId}`),
  applyDiscount: (cartId, code) =>
    api("post", `/carts/${cartId}/discount`, { code }),
  removeDiscount: (cartId) => api("delete", `/carts/${cartId}/discount`),
  setEmail: (cartId, email) => api("put", `/carts/${cartId}/email`, { email }),
  merge: (cartId) => api("post", "/me/cart/merge", { cartId }),
};
export const addressApi = {
  // An empty q only says whether search is switched on.
  suggest: (q, session, signal) =>
    api("get", "/address-search", undefined, { params: { q, session }, signal, timeout: 8000 }),
  place: (id, session) =>
    api("get", "/address-search/place", undefined, { params: { id, session }, timeout: 8000 }),
};
export const checkoutApi = {
  placeOrder: (body) => api("post", "/checkout", body),
};
export const orderApi = {
  // kind: "invoice" | "receipt"
  documentUrl: (orderNumber, token, kind) =>
    `${API_BASE}/api/v1/orders/${encodeURIComponent(orderNumber)}/${kind}?token=${encodeURIComponent(token)}`,
  get: (orderNumber, token) =>
    api("get", `/orders/${orderNumber}`, undefined, { params: { token } }),
  payAgain: (orderNumber, token) =>
    api("post", `/orders/${orderNumber}/payment`, undefined, {
      params: { token },
    }),
  cancelPaid: (orderNumber, token) =>
    api("post", `/orders/${orderNumber}/cancel-paid`, undefined, {
      params: { token },
    }),
  cancel: (orderNumber, token) =>
    api("post", `/orders/${orderNumber}/cancel`, undefined, {
      params: { token },
    }),
};
export const adminOrderApi = {
  search: (params) => api("get", "/admin/orders", undefined, { params }),
  get: (orderNumber) => api("get", `/admin/orders/${orderNumber}`),
  changeStatus: (orderNumber, status, note) =>
    api("put", `/admin/orders/${orderNumber}/status`, { status, note }),
  ship: (orderNumber, body) =>
    api("put", `/admin/orders/${orderNumber}/tracking`, body),
  bookCourier: (orderNumber) =>
    api("post", `/admin/orders/${orderNumber}/courier-booking`, undefined, { timeout: 45000 }),
  waybill: (orderNumber) => api("get", `/admin/orders/${orderNumber}/waybill`, undefined, { timeout: 45000 }),
  refund: (orderNumber, note) =>
    api("post", `/admin/orders/${orderNumber}/refund`, { note }),
  shippingSettings: () => api("get", "/admin/shipping-settings"),
  saveShippingSettings: (body) => api("put", "/admin/shipping-settings", body),
};

export { submitPaymentForm } from "./payment";
