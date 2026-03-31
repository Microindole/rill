<template>
    <div class="min-h-screen pb-10">
        <header class="site-topbar sticky top-0 z-20 w-full">
            <div class="mx-auto flex max-w-[1500px] flex-wrap items-center justify-between gap-4 px-4 py-3 sm:px-6 lg:px-8">
                <RouterLink to="/" class="text-[1.08rem] font-semibold tracking-tight text-slate-900">Rill Studio</RouterLink>

                <div class="flex flex-wrap items-start gap-4">
                    <nav ref="navTrackRef" class="nav-track relative flex flex-wrap items-center gap-1">
                        <span v-for="item in primaryNav" :key="item.key" :ref="setNavItemRef(item.key)">
                            <RouterLink
                                :to="item.to"
                                class="top-nav-link"
                                :class="{ 'is-active': isPrimaryActive(item) }"
                            >
                                {{ item.label }}
                            </RouterLink>
                        </span>
                        <span class="nav-indicator" :style="navIndicatorStyle"></span>
                    </nav>

                    <RouterLink v-if="!auth.isAuthenticated" to="/login" class="top-nav-cta">登录</RouterLink>
                    <button v-else type="button" class="top-nav-cta" @click="logout">{{ auth.user?.displayName }} · 退出</button>
                </div>
            </div>
        </header>

        <main class="mx-auto mt-6 max-w-[1500px] px-4 sm:px-6 lg:px-8">
            <RouterView v-slot="{ Component, route: currentRoute }">
                <Transition name="page-slide" mode="out-in">
                    <component :is="Component" :key="String(currentRoute.name ?? currentRoute.path)" />
                </Transition>
            </RouterView>
        </main>
    </div>
</template>

<script setup lang="ts">
import { nextTick, onBeforeUnmount, onMounted, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { useAuthStore } from "@/stores/auth";
import { usePlatformStore } from "@/stores/platform";

const auth = useAuthStore();
const platform = usePlatformStore();
const router = useRouter();
const route = useRoute();

const primaryNav = [
    { key: "home", label: "首页", to: "/" },
    { key: "console", label: "控制台", to: "/console" },
    { key: "docs", label: "文档", to: "/docs" },
    { key: "about", label: "关于", to: "/about" }
] as const;

const navTrackRef = ref<HTMLElement | null>(null);
const navItemRefs = ref<Record<string, HTMLElement | null>>({});
const navIndicatorStyle = ref<Record<string, string>>({
    opacity: "0",
    width: "0px",
    transform: "translateX(0px)"
});

function setNavItemRef(key: string) {
    return (el: any) => {
        navItemRefs.value[key] = el as HTMLElement | null;
    };
}

function isPrimaryActive(item: (typeof primaryNav)[number]) {
    if (item.to === "/") {
        return route.path === "/";
    }
    if (item.to === "/docs") {
        return route.path === "/docs" || route.path.startsWith("/docs/");
    }
    return route.path === item.to;
}

async function updateNavIndicator() {
    await nextTick();

    const activeItem = primaryNav.find((item) => isPrimaryActive(item));
    const trackEl = navTrackRef.value;
    if (!activeItem || !trackEl) {
        navIndicatorStyle.value = {
            opacity: "0",
            width: "0px",
            transform: "translateX(0px)"
        };
        return;
    }

    const activeEl = navItemRefs.value[activeItem.key];
    if (!activeEl) {
        return;
    }

    const trackRect = trackEl.getBoundingClientRect();
    const activeRect = activeEl.getBoundingClientRect();
    const x = activeRect.left - trackRect.left;

    navIndicatorStyle.value = {
        opacity: "1",
        width: `${activeRect.width}px`,
        transform: `translateX(${x}px)`
    };
}

async function logout() {
    await auth.logoutCurrentUser();
    platform.resetWorkspaceState();
    await router.push("/");
}

function handleResize() {
    void updateNavIndicator();
}

onMounted(() => {
    void updateNavIndicator();
    window.addEventListener("resize", handleResize, { passive: true });
});

onBeforeUnmount(() => {
    window.removeEventListener("resize", handleResize);
});

watch(
    () => route.fullPath,
    () => {
        void updateNavIndicator();
    }
);
</script>

<style scoped>
.nav-track {
    padding-bottom: 6px;
}

.nav-indicator {
    position: absolute;
    left: 0;
    bottom: 0;
    height: 2px;
    border-radius: 999px;
    background: #0f172a;
    transition:
        transform 260ms cubic-bezier(0.22, 1, 0.36, 1),
        width 260ms cubic-bezier(0.22, 1, 0.36, 1),
        opacity 180ms ease;
    will-change: transform, width;
}

.page-slide-enter-active,
.page-slide-leave-active {
    transition:
        opacity 240ms ease,
        transform 320ms cubic-bezier(0.22, 1, 0.36, 1);
}

.page-slide-enter-from {
    opacity: 0;
    transform: translateX(28px);
}

.page-slide-leave-to {
    opacity: 0;
    transform: translateX(-28px);
}

@media (prefers-reduced-motion: reduce) {
    .nav-indicator,
    .page-slide-enter-active,
    .page-slide-leave-active {
        transition: none;
    }
}
</style>
