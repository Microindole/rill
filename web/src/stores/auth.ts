import { computed, ref } from "vue";
import { defineStore } from "pinia";
import {
    bindOauthAccount,
    createOauthAccount,
    confirmPasswordChange,
    confirmPasswordReset,
    confirmRegister,
    currentUser,
    getOauthPendingState,
    getAuthConfig,
    login,
    logout,
    register,
    requestPasswordChange,
    requestPasswordReset
} from "@/services/api";
import type {
    AuthConfig,
    AuthUser,
    LoginPayload,
    OauthPendingState,
    PasswordChangeRequestPayload,
    PasswordResetConfirmPayload,
    PasswordResetRequestPayload,
    RegisterPayload
} from "@/types/auth";

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
        captchaSiteKey: "",
        githubLoginEnabled: false
    });
    const oauthPendingState = ref<OauthPendingState | null>(null);
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
            applyAuthenticatedUser(response);
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

    async function confirmRegisterToken(tokenValue: string) {
        loading.value = true;
        error.value = "";
        try {
            const response = await confirmRegister(tokenValue);
            applyAuthenticatedUser(response);
            return response;
        } catch (err) {
            error.value = err instanceof Error ? err.message : "邮箱验证失败";
            throw err;
        } finally {
            loading.value = false;
        }
    }

    async function requestPasswordResetEmail(payload: PasswordResetRequestPayload) {
        loading.value = true;
        error.value = "";
        try {
            return await requestPasswordReset(payload);
        } catch (err) {
            error.value = err instanceof Error ? err.message : "忘记密码请求失败";
            throw err;
        } finally {
            loading.value = false;
        }
    }

    async function confirmPasswordResetToken(payload: PasswordResetConfirmPayload) {
        loading.value = true;
        error.value = "";
        try {
            return await confirmPasswordReset(payload);
        } catch (err) {
            error.value = err instanceof Error ? err.message : "重置密码失败";
            throw err;
        } finally {
            loading.value = false;
        }
    }

    async function requestPasswordChangeEmail(payload: PasswordChangeRequestPayload) {
        loading.value = true;
        error.value = "";
        try {
            return await requestPasswordChange(token.value || undefined, payload);
        } catch (err) {
            error.value = err instanceof Error ? err.message : "改密请求失败";
            throw err;
        } finally {
            loading.value = false;
        }
    }

    async function confirmPasswordChangeToken(payload: PasswordResetConfirmPayload) {
        loading.value = true;
        error.value = "";
        try {
            return await confirmPasswordChange(payload);
        } catch (err) {
            error.value = err instanceof Error ? err.message : "改密确认失败";
            throw err;
        } finally {
            loading.value = false;
        }
    }

    async function loadAuthConfig() {
        authConfig.value = await getAuthConfig();
    }

    async function loadOauthPendingState(state: string) {
        loading.value = true;
        error.value = "";
        try {
            oauthPendingState.value = await getOauthPendingState(state);
            return oauthPendingState.value;
        } catch (err) {
            oauthPendingState.value = null;
            error.value = err instanceof Error ? err.message : "OAuth2 登录状态加载失败";
            throw err;
        } finally {
            loading.value = false;
        }
    }

    async function createOauthLinkedAccount(payload: { state: string; username: string; displayName: string }) {
        loading.value = true;
        error.value = "";
        try {
            const response = await createOauthAccount(payload);
            applyAuthenticatedUser(response);
            oauthPendingState.value = null;
            return response;
        } catch (err) {
            error.value = err instanceof Error ? err.message : "OAuth2 创建账号失败";
            throw err;
        } finally {
            loading.value = false;
        }
    }

    async function bindOauthLinkedAccount(payload: { state: string; username: string; password: string }) {
        loading.value = true;
        error.value = "";
        try {
            const response = await bindOauthAccount(payload);
            applyAuthenticatedUser(response);
            oauthPendingState.value = null;
            return response;
        } catch (err) {
            error.value = err instanceof Error ? err.message : "OAuth2 绑定账号失败";
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
            email: "guest@example.com",
            emailVerified: true,
            displayName: "Guest",
            role: "GUEST",
            kernelDbName: "default"
        };
    }

    function applyAuthenticatedUser(response: {
        token: string;
        userId: number;
        username: string;
        email: string;
        emailVerified: boolean;
        displayName: string;
        role: string;
        kernelDbName: string;
    }) {
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
    }

    async function acceptOauthToken(nextToken: string) {
        setToken(nextToken);
        await hydrateUser();
    }

    return {
        token,
        user,
        loading,
        error,
        authConfig,
        oauthPendingState,
        isAuthenticated,
        isGuest,
        loadAuthConfig,
        loadOauthPendingState,
        hydrateUser,
        loginWithPassword,
        createOauthLinkedAccount,
        bindOauthLinkedAccount,
        registerAccount,
        confirmRegisterToken,
        acceptOauthToken,
        requestPasswordResetEmail,
        confirmPasswordResetToken,
        requestPasswordChangeEmail,
        confirmPasswordChangeToken,
        logoutCurrentUser
    };
});
