/**
 * Every route, with what the <head> and breadcrumbs need (see src/seo):
 *   title        page title, shown as "<title> | GuiltFreeCookie"
 *   description  meta description, around 150 characters
 *   breadcrumb   the current page's crumb after "Home"
 *   faq          the page shows FaqSection (adds FAQPage structured data)
 *   prerender    built into static HTML and listed in the sitemap
 *   renderQuery  query keys that change what the page renders
 *   noindex      kept out of search results
 */
import { HOME_DESCRIPTION } from "../seo/site";

const legal = (path, name, title, description, component) => ({
  path,
  name,
  component,
  meta: { title, breadcrumb: title, description, prerender: true },
});

export const routes = [
  {
    path: "/",
    name: "Home",
    component: () => import("../views/HomeView.vue"),
    meta: { description: HOME_DESCRIPTION, faq: true, prerender: true },
  },
  {
    path: "/products",
    name: "Products",
    component: () => import("../views/ProductListView.vue"),
    meta: {
      title: "Shop all cookies",
      description:
        "Every GuiltFreeCookie flavour in one place: diabetic friendly, high protein, gluten free and vegan cookies, with ingredients, allergens and prices.",
      prerender: true,
      renderQuery: [
        "category",
        "q",
        "minPrice",
        "maxPrice",
        "sort",
        "inStock",
        "page",
      ],
    },
  },
  {
    path: "/product/:id",
    name: "ProductDetail",
    component: () => import("../views/ProductDetailView.vue"),
  },
  {
    path: "/about",
    name: "About",
    component: () => import("../views/AboutView.vue"),
    meta: {
      title: "About us",
      breadcrumb: "About us",
      description:
        "Why we rebuilt the classic cookie with almond flour, low-GI sweeteners and real dark chocolate, and who we bake for. Small-batch cookies from Cape Town.",
      prerender: true,
    },
  },
  {
    path: "/contact",
    name: "Contact",
    component: () => import("../views/ContactView.vue"),
    meta: {
      title: "Contact us",
      breadcrumb: "Contact",
      description:
        "Custom orders, bulk boxes for events, or a question about ingredients and allergens. Call, WhatsApp or email us in Strandfontein, Cape Town.",
      faq: true,
      prerender: true,
    },
  },

  ...[
    ["/login", "CustomerLogin", "login", "Log in"],
    ["/register", "Register", "register", "Create account"],
    ["/forgot-password", "ForgotPassword", "forgot", "Forgot password"],
    ["/reset-password", "ResetPassword", "reset", "Reset password"],
  ].map(([path, name, authMode, title]) => ({
    path,
    name,
    component: () => import("../views/CustomerAuthView.vue"),
    meta: { authMode, title, noindex: true },
  })),
  {
    path: "/account",
    name: "Account",
    component: () => import("../views/AccountView.vue"),
    meta: { title: "My account", requiresCustomer: true, noindex: true },
  },
  ...[
    ["inventory", "AdminInventory", "Stock & packs", "AdminInventoryView"],
    ["messages", "AdminMessages", "Inbox", "AdminMessagesView"],
    ["reviews", "AdminReviews", "Reviews", "AdminReviewsView"],
    [
      "email-previews",
      "AdminEmailPreviews",
      "Email previews",
      "AdminEmailPreviewsView",
    ],
  ].map(([path, name, title, view]) => ({
    path: `/admindashboard/${path}`,
    name,
    component: () => import(`../views/${view}.vue`),
    meta: { title, requiresAdmin: true, noindex: true },
  })),

  // Legal
  legal(
    "/privacy-policy",
    "PrivacyPolicy",
    "Privacy Policy",
    "How GuiltFreeCookie collects, uses and protects your personal information, why we never sell it, and how to see, correct or delete it.",
    () => import("../views/legal/PrivacyPolicyView.vue"),
  ),
  legal(
    "/terms",
    "Terms",
    "Terms and Conditions",
    "The terms for using our website and ordering from GuiltFreeCookie, including allergen and health guidance and your Consumer Protection Act rights.",
    () => import("../views/legal/TermsView.vue"),
  ),
  legal(
    "/cookie-policy",
    "CookiePolicy",
    "Cookie Policy",
    "Shopping on GuiltFreeCookie puts no cookies or trackers in your browser: no advertising, no analytics and no third parties. Here are the details.",
    () => import("../views/legal/CookiePolicyView.vue"),
  ),
  legal(
    "/refund-policy",
    "RefundPolicy",
    "Refund Policy",
    "Damaged, wrong, missing or not fresh? How we replace or refund an order, how to cancel before we bake, and why baked cookies can't be returned.",
    () => import("../views/legal/RefundPolicyView.vue"),
  ),

  // Admin
  {
    path: "/admin",
    name: "AdminLogin",
    component: () => import("../views/AdminLoginView.vue"),
    meta: { title: "Admin log in", hideChrome: true, noindex: true },
  },
  {
    path: "/admindashboard",
    redirect: "/admindashboard/cookies",
  },
  {
    path: "/admindashboard/cookies",
    name: "AdminDashboard",
    component: () => import("../views/AdminDashboardView.vue"),
    meta: { title: "Cookies · Admin", requiresAdmin: true, noindex: true },
  },
  {
    path: "/admindashboard/users",
    name: "AdminUsers",
    component: () => import("../views/AdminUsersView.vue"),
    meta: { title: "Users · Admin", requiresAdmin: true, noindex: true },
  },
  {
    path: "/admindashboard/orders",
    name: "AdminOrders",
    component: () => import("../views/AdminOrdersView.vue"),
    meta: { title: "Orders · Admin", requiresAdmin: true, noindex: true },
  },
  {
    path: "/admindashboard/profile",
    name: "AdminProfile",
    component: () => import("../views/AdminProfileView.vue"),
    meta: { title: "Your profile · Admin", requiresAdmin: true, noindex: true },
  },

  {
    path: "/:pathMatch(.*)*",
    name: "NotFound",
    component: () => import("../views/NotFoundView.vue"),
    meta: { title: "Page not found", noindex: true },
  },
];
