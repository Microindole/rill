import { computed, ref } from "vue";
import { defineStore } from "pinia";
import { currentUser, login, logout, register } from "@/services/api";
import type { AuthUser, LoginPayload, RegisterPayload } from "@/types/auth";

const TOKEN_KEY = "rill.web.token";

export const useAuthStore = defineStore("auth", () => {
    const token = ref<string>(localStorage.getItem(TOKEN_KEY) ?? "");
    const user = ref<AuthUser | null>({
        userId: 0,
        username: "guest",
        displayName: "Guest",
        role: "GUEST",
        kernelDbName: "default"
    });
    const loading = ref(false);
    const error = ref("");

    const isAuthenticated = computed(() => Boolean(token.value));
    const isGuest = computed(() => !token.value);

    function setToken(nextToken: string) {
        token.value = nextToken;
        if (nextToken) {
            localStorage.setItem(TOKEN_KEY, nextToken);
        } else {
            localStorage.removeItem(TOKEN_KEY);
        }
    }

    async function hydrateUser() {
        try {
            user.value = await currentUser(token.value || undefined);
        } catch {
            clearAuthState();
        }
    }

    async function loginWithPassword(payload: LoginPayload) {
        loading.value = true;
        error.value = "";
        try {
            const response = await login(payload);
            setToken(response.token);
            user.value = {
                userId: response.userId,
                username: response.username,
                displayName: response.displayName,
                role: response.role,
                kernelDbName: response.kernelDbName
            };
        } catch (err) {
            error.value = err instanceof Error ? err.message : "登录失败";
            throw err;
        } finally {
            loading.value = false;
        }
    }

    async function registerAccount(payload: RegisterPayload) {
        loading.value = true;
        error.value = "";
        try {
            const response = await register(payload);
            setToken(response.token);
            user.value = {
                userId: response.userId,
                username: response.username,
                displayName: response.displayName,
                role: response.role,
                kernelDbName: response.kernelDbName
            };
        } catch (err) {
            error.value = err instanceof Error ? err.message : "注册失败";
            throw err;
        } finally {
            loading.value = false;
        }
    }

    async function logoutCurrentUser() {
        if (token.value) {
            try {
                await logout(token.value);
            } catch {
                // ignore logout failure and clear local state anyway
            }
        }
        clearAuthState();
    }

    function clearAuthState() {
        setToken("");
        user.value = {
            userId: 0,
            username: "guest",
            displayName: "Guest",
            role: "GUEST",
            kernelDbName: "default"
        };
    }

    return {
        token,
        user,
        loading,
        error,
        isAuthenticated,
        isGuest,
        hydrateUser,
        loginWithPassword,
        registerAccount,
        logoutCurrentUser
    };
});
