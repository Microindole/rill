<template>
    <div class="grid gap-4 lg:grid-cols-[minmax(0,1.1fr)_minmax(360px,460px)]">
        <section class="mica-panel p-6 sm:p-8">
            <p class="text-xs font-semibold uppercase tracking-wide text-slate-500">账号中心</p>
            <h1 class="mt-2 text-3xl font-semibold tracking-tight text-slate-900 sm:text-4xl">{{ panelTitle }}</h1>
            <p class="mt-4 text-sm leading-7 text-slate-600">{{ panelDescription }}</p>

            <div class="mt-6 grid gap-3 sm:grid-cols-2">
                <div class="acrylic-panel p-4">
                    <p class="text-xs uppercase tracking-wide text-slate-500">认证模式</p>
                    <p class="mt-2 font-semibold text-slate-900">{{ modeLabel }}</p>
                </div>
                <div class="acrylic-panel p-4">
                    <p class="text-xs uppercase tracking-wide text-slate-500">登录态</p>
                    <p class="mt-2 font-semibold text-slate-900">{{ auth.isAuthenticated ? "已登录" : "未登录" }}</p>
                </div>
            </div>
        </section>

        <section class="mica-panel p-5 sm:p-6">
            <p class="text-xs font-semibold uppercase tracking-wide text-slate-500">{{ mode === "login" ? "账号登录" : submitLabel }}</p>
            <h2 class="mt-2 text-xl font-semibold text-slate-900">{{ formTitle }}</h2>
            <p class="mt-2 text-sm text-slate-500">{{ formDescription }}</p>

            <div v-if="isPrimaryMode" class="mt-4 grid grid-cols-2 gap-2 rounded-xl border border-white/70 bg-white/60 p-1">
                <button
                    class="rounded-lg py-2 text-sm font-semibold transition"
                    :class="mode === 'login' ? 'bg-white text-slate-900' : 'text-slate-600 hover:bg-white/70'"
                    @click="switchMode('login')"
                >
                    登录
                </button>
                <button
                    class="rounded-lg py-2 text-sm font-semibold transition"
                    :class="mode === 'register' ? 'bg-white text-slate-900' : 'text-slate-600 hover:bg-white/70'"
                    @click="switchMode('register')"
                >
                    注册
                </button>
            </div>

            <AppNotice v-if="auth.error" tone="error" :description="auth.error" class="mt-4" />
            <AppNotice v-if="successMessage" tone="success" :description="successMessage" class="mt-4" />

            <template v-if="mode === 'verify'">
                <div class="mt-5 space-y-4 rounded-xl border border-white/80 bg-white/70 p-4">
                    <span class="chip">{{ tokenActionDone ? "验证完成" : auth.loading ? "处理中" : "链接异常" }}</span>
                    <h3 class="text-lg font-semibold text-slate-900">{{ tokenActionDone ? "邮箱验证完成" : auth.loading ? "正在验证邮箱" : "等待验证" }}</h3>
                    <p class="text-sm text-slate-600">{{ verifySubtitle }}</p>
                    <div class="flex gap-2">
                        <AppButton v-if="tokenActionDone" variant="primary" @click="goConsole">进入工作台</AppButton>
                        <AppButton v-else @click="switchMode('login')">返回登录</AppButton>
                    </div>
                </div>
            </template>

            <template v-else>
                <form class="mt-5 space-y-3" @submit.prevent>
                    <label v-if="mode === 'forgot-password'" class="block space-y-1">
                        <span class="text-sm font-semibold text-slate-700">邮箱</span>
                        <input v-model="email" class="app-input" type="email" placeholder="demo@example.com" />
                    </label>

                    <template v-else-if="mode === 'reset-password' || mode === 'change-password'">
                        <label class="block space-y-1">
                            <span class="text-sm font-semibold text-slate-700">新密码</span>
                            <input v-model="password" class="app-input" type="password" />
                        </label>
                        <label class="block space-y-1">
                            <span class="text-sm font-semibold text-slate-700">确认新密码</span>
                            <input v-model="confirmPassword" class="app-input" type="password" />
                        </label>
                    </template>

                    <template v-else>
                        <label class="block space-y-1">
                            <span class="text-sm font-semibold text-slate-700">用户名</span>
                            <input v-model="username" class="app-input" type="text" placeholder="demo" />
                        </label>
                        <label v-if="mode === 'register'" class="block space-y-1">
                            <span class="text-sm font-semibold text-slate-700">邮箱</span>
                            <input v-model="email" class="app-input" type="email" placeholder="demo@example.com" />
                        </label>
                        <label v-if="mode === 'register'" class="block space-y-1">
                            <span class="text-sm font-semibold text-slate-700">展示名</span>
                            <input v-model="displayName" class="app-input" type="text" placeholder="示例用户" />
                        </label>
                        <label class="block space-y-1">
                            <span class="text-sm font-semibold text-slate-700">密码</span>
                            <input v-model="password" class="app-input" type="password" />
                        </label>
                        <div
                            v-if="mode === 'login' && auth.authConfig.captchaEnabled && auth.authConfig.captchaProvider === 'turnstile'"
                            class="space-y-1"
                        >
                            <span class="text-sm font-semibold text-slate-700">登录验证</span>
                            <div class="rounded-xl border border-white/80 bg-white/70 p-3">
                                <TurnstileWidget
                                    :enabled="auth.authConfig.captchaEnabled"
                                    :site-key="auth.authConfig.captchaSiteKey"
                                    @token="(value) => (captchaToken = value)"
                                />
                            </div>
                        </div>
                    </template>

                    <div class="space-y-2 pt-1">
                        <AppButton variant="primary" size="lg" :loading="auth.loading" block @click="submit">
                            {{ submitLabel }}
                        </AppButton>
                        <AppButton v-if="mode === 'login'" size="lg" subtle block @click="continueAsGuest">
                            以游客身份进入
                        </AppButton>
                        <AppButton v-if="!isPrimaryMode" size="lg" subtle block @click="switchMode('login')">
                            返回登录
                        </AppButton>
                    </div>
                </form>
            </template>

            <div v-if="mode === 'login'" class="mt-3 flex justify-end">
                <button class="text-sm font-semibold text-teal-700" @click="switchMode('forgot-password')">忘记密码</button>
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
            return "输入注册邮箱后，系统会发送密码重置链接。";
        case "reset-password":
            return "这个页面由邮件中的重置密码链接落地而来，提交后可直接返回登录。";
        case "change-password":
            return "这个页面用于确认你发起的改密请求，设置新密码后重新登录即可。";
        case "verify":
            return "邮箱验证链接落地后会自动向后端确认 token，并在成功后跳转到工作台。";
        default:
            return "前后端分离场景使用 token 跟踪登录状态，前端只保存 token。";
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
