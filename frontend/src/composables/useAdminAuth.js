/**
 * useAdminAuth — admin session backed by a real JWT from the backend.
 *
 * POST /admin/login now returns { admin, token }. The token is what
 * actually gates every /admin/** and admin-only /cookie/** request on the
 * server (see SecurityConfig + JwtAuthenticationFilter) — it is a real,
 * expiring credential, not just a client-side flag.
 *
 * "Remember me" decides where that session is kept: localStorage survives
 * browser restarts, sessionStorage clears when the tab closes. Only one of
 * the two is ever written at a time so there's a single source of truth.
 * Either way the token still expires server-side after app.jwt.expiration-
 * minutes — "remember me" means "stay logged in without re-entering your
 * password for that long," not "forever."
 */
import { ref, computed } from "vue";

const STORAGE_KEY = "gfc_admin_session";

function readStoredSession() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY) || sessionStorage.getItem(STORAGE_KEY);
    return JSON.parse(raw || "null");
  } catch {
    return null;
  }
}

const initialSession = readStoredSession();
const admin = ref(initialSession?.admin ?? null);
let token = initialSession?.token ?? null;

function persist(remember) {
  const json = JSON.stringify({ admin: admin.value, token });

  if (remember) {
    localStorage.setItem(STORAGE_KEY, json);
    sessionStorage.removeItem(STORAGE_KEY);
  } else {
    sessionStorage.setItem(STORAGE_KEY, json);
    localStorage.removeItem(STORAGE_KEY);
  }
}

function login(adminRecord, authToken, remember = false) {
  admin.value = adminRecord;
  token = authToken;
  persist(remember);
}

function logout() {
  admin.value = null;
  token = null;
  localStorage.removeItem(STORAGE_KEY);
  sessionStorage.removeItem(STORAGE_KEY);
}

// Refreshes the cached session record after the admin edits their own
// profile or password, writing back to whichever storage already holds it.
function updateAdmin(adminRecord) {
  admin.value = adminRecord;
  persist(localStorage.getItem(STORAGE_KEY) !== null);
}

// Plain functions (not computed/refs) so the router guard and the axios
// interceptors can read them outside component context.
export function isAdminLoggedIn() {
  return !!admin.value && !!token;
}

export function getAdminToken() {
  return token;
}

export function useAdminAuth() {
  return {
    admin,
    isLoggedIn: computed(() => !!admin.value),
    login,
    logout,
    updateAdmin,
  };
}
