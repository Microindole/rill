import { createRouter, createWebHistory } from "vue-router";
import HomeView from "@/views/HomeView.vue";
import LoginView from "@/views/LoginView.vue";
import ConsoleView from "@/views/ConsoleView.vue";
import AboutView from "@/views/AboutView.vue";
import { useAuthStore } from "@/stores/auth";

const router = createRouter({
    history: createWebHistory(),
    routes: [
        {
            path: "/",
            name: "home",
            component: HomeView
        },
        {
            path: "/login",
            name: "login",
            component: LoginView
        },
        {
            path: "/console",
            name: "console",
            component: ConsoleView
        },
        {
            path: "/about",
            name: "about",
            component: AboutView
        }
    ]
});

router.beforeEach(async (to) => {
    const auth = useAuthStore();
    if (auth.token && auth.user?.userId === 0) {
        await auth.hydrateUser();
    }
    if (to.name === "login" && auth.isAuthenticated) {
        return { name: "console" };
    }
    return true;
});

export default router;
