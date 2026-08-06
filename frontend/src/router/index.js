import {createRouter, createWebHistory} from "vue-router";
import {useAuthStore} from "../stores/authStore";
import HomeView from "../views/HomeView.vue";
import ProductListView from "../views/ProductListView.vue";
import ProductDetailView from "../views/ProductDetailView.vue";
import AdminDashboardView from "../views/AdminDashboardView.vue";
import AuthView from "../views/AuthView.vue";

const routes = [
    {
        path: "/",
        name: "Home",
        component: HomeView,
        meta: {title: "GuiltFreeCookie . Handcrafted Cookies"}
    },
    {
        path: "/login",
        name: "Login",
        component: AuthView,
        meta: {title: "Login · GuiltFreeCookie"}
    },
    {
        path: "/products",
        name: "Products",
        component: ProductListView,
        meta: {title: "Our Cookies · GuiltFreeCookie"}
    },
    {
        path: "/product/:id",
        name: "ProductDetail",
        component: ProductDetailView,
        meta: {title: "Product Detail"}
    },
    {
        path: "/contact",
        name: "Contact",
        component: () => import("../views/ContactView.vue"),
        meta: {title: "Contact · GuiltFreeCookie"},
    },
    {
        path: "/about",
        name: "About",
        component: () => import("../views/AboutView.vue"),
        meta: {title: "About · GuiltFreeCookie"},
    },

    {
        path: "/dashboard",
        name: "CustomerDashboard",
        component: () => import("../views/DashboardView.vue"),
        meta: {
            requiresAuth: true,
            title: "CustomerDashboard",
        },
    },

    // =========================
    // Admin Routes
    // =========================
    {
        path: "/admin",
        name: "AdminDashboard",
        component: AdminDashboardView,
        meta: {
            requiresAuth: true,
            requiresAdmin: true,
            title: "Admin Dashboard"
        },
    },
];

const router = createRouter({
    history: createWebHistory(),
    routes,
    scrollBehavior() {
        return {top: 0};
    },
});

router.beforeEach((to, from, next) => {
    const authStore = useAuthStore();

    const defaultTitle = "GuiltFreeCookie";
    document.title = to.meta.title || defaultTitle;

    if (to.meta.requiresAuth && !authStore.isAuthenticated) {
        return next({name: "Login"});
    }

    if (to.meta.requiresAdmin && !authStore.isAdmin) {
        alert("Access denied");
        return next({name: "Home"});
    }

    if (to.name === "Login" && authStore.isAuthenticated) {
        return next({name: "Home"});
    }

    next();
});

export default router;
