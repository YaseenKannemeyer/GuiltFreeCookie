import { api } from "./storeApi";

// Links in the automated emails, and the admin settings for them.
export const emailLinkApi = {
  unsubscribe: (email, token) => api("post", "/email/unsubscribe", { email, token }),
  resubscribe: (email, token) => api("post", "/email/resubscribe", { email, token }),
  stay: (email, token) => api("post", "/email/stay", { email, token }),
  recoverCart: (token) => api("get", `/carts/recover/${encodeURIComponent(token)}`),
  reorder: (orderNumber, token) =>
    api("post", `/orders/${encodeURIComponent(orderNumber)}/reorder`, undefined, { params: { token } }),
  review: (body) => api("post", "/reviews/by-link", body),
  productViewed: (cookieId, cartId) => api("post", "/events/product-view", { cookieId, cartId }),
};

export const adminFlowApi = {
  settings: () => api("get", "/admin/email-flows"),
  save: (body) => api("put", "/admin/email-flows", body),
  stats: (days = 30) => api("get", "/admin/email-flows/stats", undefined, { params: { days } }),
};
