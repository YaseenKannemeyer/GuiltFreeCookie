import { ref, computed } from "vue";
const key = "gfc_customer_session";
let stored;
try {
  stored = JSON.parse(
    localStorage.getItem(key) || sessionStorage.getItem(key) || "null",
  );
} catch {
  stored = null;
}
const customer = ref(stored?.customer || null);
let token = stored?.token || null;
function persist(remember) {
  try {
    const storage = remember ? localStorage : sessionStorage;
    storage.setItem(key, JSON.stringify({ customer: customer.value, token }));
    (remember ? sessionStorage : localStorage).removeItem(key);
  } catch {
    /* A blocked storage area still allows an in-memory session. */
  }
}
function login(record, jwt, remember = false) {
  customer.value = record;
  token = jwt;
  persist(remember);
}
function logout() {
  customer.value = null;
  token = null;
  try {
    localStorage.removeItem(key);
    sessionStorage.removeItem(key);
  } catch {
    /* Storage may be disabled. */
  }
}
function updateSession(record, jwt = token) {
  let remember = false;
  try {
    remember = localStorage.getItem(key) !== null;
  } catch {
    /* Keep session in memory. */
  }
  login(record, jwt, remember);
}
export const getCustomerToken = () => token;
export const isCustomerLoggedIn = () => !!customer.value && !!token;
export const useCustomerAuth = () => ({
  customer,
  isLoggedIn: computed(isCustomerLoggedIn),
  login,
  logout,
  updateSession,
});
