<template>
    <div class="auth-page">
        <section class="auth-panel">
            <div class="auth-copy">
                <p class="hero-eyebrow">Token Sign-In</p>
                <h1>登录后进入你的工作台</h1>
                <p>前后端分离场景使用 token 跟踪登录状态，前端只保存 token，不再依赖服务端 session cookie。</p>
            </div>
            <div class="auth-form-panel">
                <div class="auth-tabs">
                    <button :class="{ active: mode === 'login' }" @click="mode = 'login'">登录</button>
                    <button :class="{ active: mode === 'register' }" @click="mode = 'register'">注册</button>
                </div>
                <el-alert
                    v-if="auth.error"
                    type="error"
                    :description="auth.error"
                    show-icon
                    :closable="false"
                    class="auth-alert"
                />
                <el-form @submit.prevent>
                    <el-form-item label="用户名">
                        <el-input v-model="username" placeholder="demo" />
                    </el-form-item>
                    <el-form-item v-if="mode === 'register'" label="展示名">
                        <el-input v-model="displayName" placeholder="Demo User" />
                    </el-form-item>
                    <el-form-item label="密码">
                        <el-input v-model="password" type="password" show-password />
                    </el-form-item>
                    <el-button type="primary" :loading="auth.loading" class="auth-submit" @click="submit">
                        {{ mode === "login" ? "登录" : "注册并登录" }}
                    </el-button>
                    <el-button class="auth-submit" @click="continueAsGuest">
                        以游客身份进入共享 default 数据库
                    </el-button>
                </el-form>
                <p class="auth-tip">
                    默认演示账号：<strong>demo</strong> / <strong>demo123</strong>
                </p>
            </div>
        </section>
    </div>
</template>

<script setup lang="ts">
import { ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { useAuthStore } from "@/stores/auth";

const auth = useAuthStore();
const route = useRoute();
const router = useRouter();

const mode = ref<"login" | "register">("login");
const username = ref("demo");
const displayName = ref("Demo User");
const password = ref("demo123");

async function submit() {
    if (mode.value === "login") {
        await auth.loginWithPassword({
            username: username.value,
            password: password.value
        });
    } else {
        await auth.registerAccount({
            username: username.value,
            displayName: displayName.value,
            password: password.value
        });
    }
    const redirect =
        typeof route.query.redirect === "string" && route.query.redirect.length > 0
            ? route.query.redirect
            : "/console";
    await router.push(redirect);
}

async function continueAsGuest() {
    await auth.logoutCurrentUser();
    await router.push("/console");
}
</script>
