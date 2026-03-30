import { computed, ref } from "vue";
import { defineStore } from "pinia";
import { currentUser, getAuthConfig, login, logout, register } from "@/services/api";
import type { AuthConfig, AuthUser, LoginPayload, RegisterPayload } from "@/types/auth";

const TOKEN_KEY = "rill.web.token";

export const useAuthStore = defineStore("auth", () => {
    const token = ref<string>(localStorage.getItem(TOKEN_KEY) ?? "");
    const user = ref<AuthUser | null>({
        userId: 0,
        username: "guest",
        email: "guest@example.com",
        emailVerified: true,
        displayName: "Guest",
        role: "GUEST",
        kernelDbName: "default"
    });
    const authConfig = ref<AuthConfig>({
        captchaEnabled: false,
        captchaProvider: "turnstile",
        captchaSiteKey: ""
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
                email: response.email,
                emailVerified: response.emailVerified,
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
            await register(payload);
        } catch (err) {
            error.value = err instanceof Error ? err.message : "注册失败";
            throw err;
        } finally {
            loading.value = false;
        }
    }

    async function loadAuthConfig() {
        authConfig.value = await getAuthConfig();
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
            email: "guest@example.com",
            emailVerified: true,
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
        authConfig,
        isAuthenticated,
        isGuest,
        loadAuthConfig,
        hydrateUser,
        loginWithPassword,
        registerAccount,
        logoutCurrentUser
    };
});
