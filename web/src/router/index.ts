import { createRouter, createWebHistory } from "vue-router";
import { useAuthStore } from "@/stores/auth";

const router = createRouter({
    history: createWebHistory(),
    routes: [
        {
            path: "/",
            name: "home",
            component: () => import("@/views/HomeView.vue")
        },
        {
            path: "/login",
            name: "login",
            component: () => import("@/views/LoginView.vue")
        },
        {
            path: "/login/oauth2/success",
            name: "login-oauth2-success",
            component: () => import("@/views/LoginView.vue")
        },
        {
            path: "/login/oauth2/error",
            name: "login-oauth2-error",
            component: () => import("@/views/LoginView.vue")
        },
        {
            path: "/console",
            name: "console",
            component: () => import("@/views/ConsoleView.vue")
        },
        {
            path: "/docs/:slug?",
            name: "docs",
            component: () => import("@/views/DocsView.vue")
        },
        {
            path: "/about",
            name: "about",
            component: () => import("@/views/AboutView.vue")
        }
    ]
});

router.beforeEach(async (to) => {
    const auth = useAuthStore();
    if (auth.token && auth.user?.userId === 0) {
        await auth.hydrateUser();
    }
    const hasAuthAction =
        (to.name === "login" || to.name === "login-oauth2-success" || to.name === "login-oauth2-error")
        && typeof to.query.mode === "string"
        && ["verify", "reset-password", "change-password", "oauth2-link"].includes(to.query.mode);
    if ((to.name === "login" || to.name === "login-oauth2-success" || to.name === "login-oauth2-error") && auth.isAuthenticated && !hasAuthAction) {
        return { name: "console" };
    }
    return true;
});

export default router;
