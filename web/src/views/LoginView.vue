<template>
    <div class="auth-page">
        <section class="auth-stage">
            <div class="auth-copy-panel">
                <p class="hero-eyebrow">账号中心</p>
                <h1>{{ panelTitle }}</h1>
                <p>{{ panelDescription }}</p>
                <div class="auth-copy-summary">
                    <div>
                        <span>认证模式</span>
                        <strong>{{ modeLabel }}</strong>
                    </div>
                    <div>
                        <span>登录态</span>
                        <strong>{{ auth.isAuthenticated ? "已登录" : "未登录" }}</strong>
                    </div>
                </div>
                <div class="auth-copy-points">
                    <div>
                        <strong>个人数据库</strong>
                        <span>注册后自动分配独立数据库与工作台资产。</span>
                    </div>
                    <div>
                        <strong>邮件确认</strong>
                        <span>注册、改密、忘记密码统一走邮件 token 确认链路。</span>
                    </div>
                    <div>
                        <strong>游客模式</strong>
                        <span>不登录也能进入共享数据库快速体验主要功能。</span>
                    </div>
                </div>
            </div>
            <div class="auth-form-panel">
                <div class="auth-panel-header">
                    <p class="auth-panel-kicker">{{ mode === "login" ? "账号登录" : submitLabel }}</p>
                    <h2>{{ formTitle }}</h2>
                    <p class="auth-panel-subtitle">{{ formDescription }}</p>
                </div>
                <div v-if="isPrimaryMode" class="auth-tabs">
                    <button :class="{ active: mode === 'login' }" @click="switchMode('login')">登录</button>
                    <button :class="{ active: mode === 'register' }" @click="switchMode('register')">注册</button>
                </div>
                <AppNotice v-if="auth.error" tone="error" :description="auth.error" class="auth-alert" />
                <AppNotice v-if="successMessage" tone="success" :description="successMessage" class="auth-alert" />

                <template v-if="mode === 'verify'">
                    <div class="auth-result-card">
                        <span class="auth-result-badge" :class="{ success: tokenActionDone, pending: auth.loading }">
                            {{ tokenActionDone ? "验证完成" : auth.loading ? "处理中" : "链接异常" }}
                        </span>
                        <h3>{{ tokenActionDone ? "邮箱验证完成" : auth.loading ? "正在验证邮箱" : "等待验证" }}</h3>
                        <p>{{ verifySubtitle }}</p>
                        <div class="auth-result-actions">
                            <AppButton v-if="tokenActionDone" variant="primary" @click="goConsole">进入工作台</AppButton>
                            <AppButton v-else @click="switchMode('login')">返回登录</AppButton>
                        </div>
                    </div>
                </template>

                <template v-else>
                    <form class="auth-form" @submit.prevent>
                        <label v-if="mode === 'forgot-password'" class="field-block">
                            <span class="field-label">邮箱</span>
                            <input v-model="email" class="app-input is-large" type="email" placeholder="demo@example.com" />
                        </label>

                        <template v-else-if="mode === 'reset-password' || mode === 'change-password'">
                            <label class="field-block">
                                <span class="field-label">新密码</span>
                                <input v-model="password" class="app-input is-large" type="password" />
                            </label>
                            <label class="field-block">
                                <span class="field-label">确认新密码</span>
                                <input v-model="confirmPassword" class="app-input is-large" type="password" />
                            </label>
                        </template>

                        <template v-else>
                            <label class="field-block">
                                <span class="field-label">用户名</span>
                                <input v-model="username" class="app-input is-large" type="text" placeholder="demo" />
                            </label>
                            <label v-if="mode === 'register'" class="field-block">
                                <span class="field-label">邮箱</span>
                                <input v-model="email" class="app-input is-large" type="email" placeholder="demo@example.com" />
                            </label>
                            <label v-if="mode === 'register'" class="field-block">
                                <span class="field-label">展示名</span>
                                <input v-model="displayName" class="app-input is-large" type="text" placeholder="示例用户" />
                            </label>
                            <label class="field-block">
                                <span class="field-label">密码</span>
                                <input v-model="password" class="app-input is-large" type="password" />
                            </label>
                            <div
                                v-if="mode === 'login' && auth.authConfig.captchaEnabled && auth.authConfig.captchaProvider === 'turnstile'"
                                class="field-block"
                            >
                                <span class="field-label">登录验证</span>
                                <TurnstileWidget
                                    :enabled="auth.authConfig.captchaEnabled"
                                    :site-key="auth.authConfig.captchaSiteKey"
                                    @token="(value) => (captchaToken = value)"
                                />
                            </div>
                        </template>

                        <div class="auth-form-actions">
                            <AppButton variant="primary" size="lg" :loading="auth.loading" block class="auth-submit primary" @click="submit">
                                {{ submitLabel }}
                            </AppButton>
                            <AppButton v-if="mode === 'login'" size="lg" subtle block class="auth-submit secondary" @click="continueAsGuest">
                                以游客身份进入共享数据库
                            </AppButton>
                            <AppButton v-if="!isPrimaryMode" size="lg" subtle block class="auth-submit secondary" @click="switchMode('login')">
                                返回登录
                            </AppButton>
                        </div>
                    </form>
                </template>

                <div v-if="mode === 'login'" class="auth-links">
                    <button class="link-button" @click="switchMode('forgot-password')">忘记密码</button>
                </div>
                <div class="auth-tip-box" v-if="mode === 'login' || mode === 'register'">
                    <p class="auth-tip-title">演示账号</p>
                    <p class="auth-tip">
                        默认演示账号：<strong>demo</strong> / <strong>demo123</strong>
                    </p>
                </div>
            </div>
        </section>
    </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { useAuthStore } from "@/stores/auth";
import TurnstileWidget from "@/components/TurnstileWidget.vue";
import AppButton from "@/components/ui/AppButton.vue";
import AppNotice from "@/components/ui/AppNotice.vue";

type AuthMode = "login" | "register" | "forgot-password" | "reset-password" | "change-password" | "verify";

const auth = useAuthStore();
const route = useRoute();
const router = useRouter();

const mode = ref<AuthMode>("login");
const username = ref("demo");
const email = ref("demo@example.com");
const displayName = ref("Demo User");
const password = ref("demo123");
const confirmPassword = ref("");
const successMessage = ref("");
const tokenActionDone = ref(false);
const token = ref("");
let captchaToken = "";

const isPrimaryMode = computed(() => mode.value === "login" || mode.value === "register");

const modeLabel = computed(() => {
    switch (mode.value) {
        case "register":
            return "注册";
        case "forgot-password":
            return "找回密码";
        case "reset-password":
            return "重置密码";
        case "change-password":
            return "修改密码";
        case "verify":
            return "邮箱验证";
        default:
            return "登录";
    }
});

const panelTitle = computed(() => {
    switch (mode.value) {
        case "register":
            return "注册后发送验证邮件";
        case "forgot-password":
            return "通过邮件重置密码";
        case "reset-password":
            return "设置新的登录密码";
        case "change-password":
            return "确认密码修改";
        case "verify":
            return "确认邮箱验证链接";
        default:
            return "登录后进入你的工作台";
    }
});

const panelDescription = computed(() => {
    switch (mode.value) {
        case "register":
            return "注册完成后会发送验证邮件，确认后自动签发 token 并进入个人工作台。";
        case "forgot-password":
            return "输入注册邮箱后，系统会发送密码重置链接到你的邮箱。";
        case "reset-password":
            return "这个页面由邮件中的重置密码链接落地而来，提交后可直接返回登录。";
        case "change-password":
            return "这个页面用于确认你发起的改密请求，设置新密码后重新登录即可。";
        case "verify":
            return "邮箱验证链接落地后会自动向后端确认 token，并在成功后跳转到工作台。";
        default:
            return "前后端分离场景使用 token 跟踪登录状态，前端只保存 token，不再依赖服务端 session cookie。";
    }
});

const verifySubtitle = computed(() => {
    if (auth.loading) {
        return "正在校验邮件链接，请稍候。";
    }
    if (tokenActionDone.value) {
        return "邮箱验证成功，当前登录态已经建立。";
    }
    return auth.error || "缺少或无效的验证 token。";
});

const submitLabel = computed(() => {
    switch (mode.value) {
        case "register":
            return "注册并发送验证邮件";
        case "forgot-password":
            return "发送重置密码邮件";
        case "reset-password":
            return "确认重置密码";
        case "change-password":
            return "确认修改密码";
        default:
            return "登录";
    }
});

const formTitle = computed(() => {
    switch (mode.value) {
        case "register":
            return "创建账号并发送验证邮件";
        case "forgot-password":
            return "发送重置密码邮件";
        case "reset-password":
            return "输入并确认新的登录密码";
        case "change-password":
            return "确认你的密码修改请求";
        case "verify":
            return "确认邮箱验证";
        default:
            return "输入账号密码进入工作台";
    }
});

const formDescription = computed(() => {
    switch (mode.value) {
        case "register":
            return "填写基础身份信息后，系统会发送验证邮件完成账号激活。";
        case "forgot-password":
            return "输入注册邮箱后，后端会发送一封带确认链接的重置邮件。";
        case "reset-password":
            return "新密码提交成功后，旧密码将失效，请使用新密码重新登录。";
        case "change-password":
            return "这是邮件确认落地页，提交后会正式替换原有登录密码。";
        case "verify":
            return "邮箱验证成功后会自动建立登录态，并可直接进入工作台。";
        default:
            return "登录后可进入个人工作台；也可以先用游客模式体验共享数据库。";
    }
});

onMounted(async () => {
    await auth.loadAuthConfig();
    await syncModeFromRoute();
});

watch(
    () => route.query,
    async () => {
        await syncModeFromRoute();
    }
);

async function syncModeFromRoute() {
    const routeMode = typeof route.query.mode === "string" ? route.query.mode : "";
    token.value = typeof route.query.token === "string" ? route.query.token : "";

    if (routeMode === "verify" || routeMode === "reset-password" || routeMode === "change-password") {
        mode.value = routeMode;
        successMessage.value = "";
        auth.error = "";
        tokenActionDone.value = false;
        if (routeMode === "verify") {
            await runVerifyTokenFlow();
        }
        return;
    }

    if (routeMode === "register") {
        mode.value = "register";
        return;
    }
    if (routeMode === "forgot-password") {
        mode.value = "forgot-password";
        return;
    }
    mode.value = "login";
}

async function runVerifyTokenFlow() {
    if (!token.value) {
        auth.error = "缺少邮箱验证 token";
        return;
    }
    try {
        await auth.confirmRegisterToken(token.value);
        tokenActionDone.value = true;
        successMessage.value = "邮箱验证成功，正在进入工作台。";
    } catch {
        tokenActionDone.value = false;
        return;
    }
}

async function submit() {
    successMessage.value = "";
    auth.error = "";
    try {
        if (mode.value === "login") {
            await auth.loginWithPassword({
                username: username.value,
                password: password.value,
                captchaToken
            });
            const redirect =
                typeof route.query.redirect === "string" && route.query.redirect.length > 0
                    ? route.query.redirect
                    : "/console";
            await router.push(redirect);
            return;
        }

        if (mode.value === "register") {
            await auth.registerAccount({
                username: username.value,
                email: email.value,
                displayName: displayName.value,
                password: password.value
            });
            switchMode("login");
            successMessage.value = "注册成功，验证邮件已发送。完成邮箱验证后再登录。";
            return;
        }

        if (mode.value === "forgot-password") {
            const response = await auth.requestPasswordResetEmail({ email: email.value });
            switchMode("login");
            successMessage.value = response.message;
            return;
        }

        if (mode.value === "reset-password") {
            ensurePasswordConfirmation();
            const response = await auth.confirmPasswordResetToken({
                token: token.value,
                newPassword: password.value
            });
            switchMode("login");
            successMessage.value = response.message;
            return;
        }

        if (mode.value === "change-password") {
            ensurePasswordConfirmation();
            const response = await auth.confirmPasswordChangeToken({
                token: token.value,
                newPassword: password.value
            });
            switchMode("login");
            successMessage.value = response.message;
        }
    } catch (err) {
        if (err instanceof Error && !auth.error) {
            auth.error = err.message;
        }
    }
}

function ensurePasswordConfirmation() {
    if (!token.value) {
        throw new Error("缺少邮件中的确认 token");
    }
    if (!password.value || !confirmPassword.value) {
        throw new Error("新密码和确认密码不能为空");
    }
    if (password.value !== confirmPassword.value) {
        throw new Error("两次输入的新密码不一致");
    }
}

function switchMode(nextMode: AuthMode) {
    mode.value = nextMode;
    password.value = nextMode === "login" || nextMode === "register" ? password.value : "";
    confirmPassword.value = "";
    successMessage.value = "";
    auth.error = "";
    tokenActionDone.value = false;
    const query =
        nextMode === "login"
            ? typeof route.query.redirect === "string" && route.query.redirect.length > 0
                ? { redirect: route.query.redirect }
                : {}
            : nextMode === "register" || nextMode === "forgot-password"
                ? { mode: nextMode }
                : {};
    void router.replace({ path: "/login", query });
}

async function continueAsGuest() {
    await auth.logoutCurrentUser();
    await router.push("/console");
}

async function goConsole() {
    await router.push("/console");
}
</script>
