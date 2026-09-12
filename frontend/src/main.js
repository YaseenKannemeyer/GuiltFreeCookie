import { createApp } from "vue";
import axios from "axios";
import App from "./App.vue";
import router from "./router";
import { getAdminToken, useAdminAuth } from "./composables/useAdminAuth";
import "./assets/main.css";

// Attach the admin's bearer token to every outgoing request. Harmless on
// public endpoints (the backend just ignores it there) and required for
// anything behind SecurityConfig's authorizeHttpRequests rules.
axios.interceptors.request.use((config) => {
  const token = getAdminToken();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// A 401 while we believed we had a session means the token expired or was
// rejected server-side — clear the stale session and send the admin back
// to log in. Guarded on getAdminToken() so a plain wrong-password response
// from the login form itself isn't mistaken for a session expiring, and
// changePassword is excluded because it also answers 401 for "your current
// password was wrong" — a form validation error, not an expired session —
// which AdminProfileView already handles inline.
axios.interceptors.response.use(
  (response) => response,
  (error) => {
    const isChangePasswordCall = error.config?.url?.includes("/changePassword/");
    if (error.response?.status === 401 && getAdminToken() && !isChangePasswordCall) {
      useAdminAuth().logout();
      router.push({ name: "AdminLogin" });
    }
    return Promise.reject(error);
  },
);

const app = createApp(App);
app.use(router);
app.mount("#app");
