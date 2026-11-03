import { createRouter } from "vue-router";
import { isCustomerLoggedIn } from "../composables/useCustomerAuth";
import { routes } from "./routes";
import { isAdminLoggedIn } from "../composables/useAdminAuth";

// A factory, not a singleton: the browser uses web history, prerendering a
// fresh memory-history router per page.
export function createAppRouter(history) {
  const router = createRouter({
    history,
    routes,
    scrollBehavior(to, from, savedPosition) {
      if (savedPosition) return savedPosition;
      if (to.hash) return { el: to.hash, top: 96, behavior: "smooth" };
      // Query-only changes (product list filters) keep the scroll position.
      if (to.path === from.path) return false;
      return { top: 0 };
    },
  });

  router.beforeEach((to) => {
    if (to.meta.requiresCustomer && !isCustomerLoggedIn())
      return { name: "CustomerLogin", query: { redirect: to.fullPath } };
    if (to.meta.requiresAdmin && !isAdminLoggedIn()) {
      return { name: "AdminLogin", query: { redirect: to.fullPath } };
    }

    if (to.name === "AdminLogin" && isAdminLoggedIn()) {
      return { name: "AdminOverview" };
    }
  });

  return router;
}
