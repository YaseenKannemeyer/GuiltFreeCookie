import { API_BASE } from "./http";
import { getAdminToken, useAdminAuth } from "../composables/useAdminAuth";
import {
  getCustomerToken,
  useCustomerAuth,
} from "../composables/useCustomerAuth";
export function requestRole(url) {
  if (!url?.startsWith(`${API_BASE}/`)) return null;
  const path = url.slice(API_BASE.length).split("?")[0];
  if (/^\/api\/v1\/me(?:\/|$)/.test(path)) return "customer";
  // Public, but a signed-in customer's cart and checkout belong to their account.
  if (/^\/api\/v1\/(?:carts|checkout|events)(?:\/|$)/.test(path)) return "customer-optional";
  if (
    (/^\/admin\//.test(path) && path !== "/admin/login") ||
    /^\/api\/v1\/admin(?:\/|$)/.test(path) ||
    /^\/(cookie|cart)\//.test(path) ||
    path.startsWith("/api/v1/customers")
  )
    return "admin";
  return null;
}
export function installAuthInterceptors(client, router) {
  client.interceptors.request.use((config) => {
    const role = requestRole(config.url);
    const token =
      role === "customer" || role === "customer-optional"
        ? getCustomerToken()
        : role === "admin"
          ? getAdminToken()
          : null;
    if (token) config.headers.Authorization = `Bearer ${token}`;
    return config;
  });
  client.interceptors.response.use(
    (response) => response,
    async (error) => {
      const url = error.config?.url || "";
      const role = requestRole(url);
      if (
        error.response?.status === 401 &&
        role === "customer" &&
        getCustomerToken()
      ) {
        // A wrong current password also returns 401. Check the session before ending it.
        if (url.endsWith("/me/password")) {
          try {
            await client.get(`${API_BASE}/api/v1/me`);
            return Promise.reject(error);
          } catch (probe) {
            if (probe.response?.status !== 401) return Promise.reject(error);
          }
        }
        useCustomerAuth().logout();
        router.push({
          name: "CustomerLogin",
          query: { redirect: router.currentRoute.value.fullPath },
        });
      } else if (
        error.response?.status === 401 &&
        role === "admin" &&
        getAdminToken() &&
        !url.includes("/changePassword/")
      ) {
        useAdminAuth().logout();
        router.push({ name: "AdminLogin" });
      }
      return Promise.reject(error);
    },
  );
}
