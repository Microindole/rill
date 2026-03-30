<template>
    <div class="site-shell">
        <header class="site-header">
            <div class="brand-block">
                <RouterLink class="brand-link" to="/">Rill</RouterLink>
                <span class="brand-subtitle">Database Studio</span>
            </div>
            <nav class="site-nav">
                <RouterLink to="/">首页</RouterLink>
                <RouterLink to="/console">控制台</RouterLink>
                <RouterLink to="/about">项目介绍</RouterLink>
                <RouterLink v-if="!auth.isAuthenticated" to="/login">登录</RouterLink>
                <button v-else class="header-user" type="button" @click="logout">
                    {{ auth.user?.displayName }} · 退出
                </button>
            </nav>
        </header>
        <RouterView />
    </div>
</template>

<script setup lang="ts">
import { useRouter } from "vue-router";
import { useAuthStore } from "@/stores/auth";
import { usePlatformStore } from "@/stores/platform";

const auth = useAuthStore();
const platform = usePlatformStore();
const router = useRouter();

async function logout() {
    await auth.logoutCurrentUser();
    platform.resetWorkspaceState();
    await router.push("/");
}
</script>
