<template>
    <div ref="containerRef" class="turnstile-container"></div>
</template>

<script setup lang="ts">
import { onMounted, onBeforeUnmount, ref, watch } from "vue";

const props = defineProps<{
    siteKey: string;
    enabled: boolean;
}>();

const emit = defineEmits<{
    token: [value: string];
}>();

const containerRef = ref<HTMLDivElement | null>(null);
let widgetId: string | null = null;

function ensureScript(): Promise<void> {
    return new Promise((resolve, reject) => {
        const existing = document.querySelector<HTMLScriptElement>('script[data-rill-turnstile="true"]');
        if (existing) {
            if ((window as any).turnstile) {
                resolve();
                return;
            }
            existing.addEventListener("load", () => resolve(), { once: true });
            existing.addEventListener("error", () => reject(new Error("Failed to load Turnstile")), {
                once: true
            });
            return;
        }
        const script = document.createElement("script");
        script.src = "https://challenges.cloudflare.com/turnstile/v0/api.js?render=explicit";
        script.async = true;
        script.defer = true;
        script.dataset.rillTurnstile = "true";
        script.addEventListener("load", () => resolve(), { once: true });
        script.addEventListener("error", () => reject(new Error("Failed to load Turnstile")), {
            once: true
        });
        document.head.appendChild(script);
    });
}

function renderWidget() {
    if (!props.enabled || !props.siteKey || !containerRef.value || !(window as any).turnstile) {
        return;
    }
    containerRef.value.innerHTML = "";
    widgetId = (window as any).turnstile.render(containerRef.value, {
        sitekey: props.siteKey,
        callback: (token: string) => emit("token", token),
        "expired-callback": () => emit("token", ""),
        "error-callback": () => emit("token", "")
    });
}

async function boot() {
    emit("token", "");
    if (!props.enabled || !props.siteKey) {
        return;
    }
    await ensureScript();
    renderWidget();
}

watch(
    () => [props.enabled, props.siteKey],
    () => {
        void boot();
    }
);

onMounted(() => {
    void boot();
});

onBeforeUnmount(() => {
    if (widgetId && (window as any).turnstile) {
        (window as any).turnstile.remove(widgetId);
    }
});
</script>
