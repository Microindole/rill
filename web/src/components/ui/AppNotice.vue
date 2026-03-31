<template>
    <div :class="noticeClass" role="alert">
        <div class="grid h-6 w-6 place-items-center rounded-full bg-white/80 text-xs font-bold">{{ icon }}</div>
        <div class="space-y-1">
            <strong v-if="title" class="block text-sm">{{ title }}</strong>
            <p class="m-0 text-sm">{{ description }}</p>
        </div>
    </div>
</template>

<script setup lang="ts">
import { computed } from "vue";

const props = withDefaults(
    defineProps<{
        tone?: "error" | "success" | "warning" | "info";
        title?: string;
        description: string;
    }>(),
    {
        tone: "info",
        title: ""
    }
);

const icon = computed(() => {
    switch (props.tone) {
        case "error":
            return "!";
        case "success":
            return "✓";
        case "warning":
            return "•";
        default:
            return "i";
    }
});

const toneClass: Record<string, string> = {
    info: "border-slate-200/80 bg-white/75 text-slate-700",
    success: "border-emerald-200/80 bg-emerald-50/80 text-emerald-800",
    warning: "border-amber-200/80 bg-amber-50/80 text-amber-800",
    error: "border-rose-200/80 bg-rose-50/85 text-rose-800"
};

const noticeClass = computed(() => [
    "grid grid-cols-[auto,1fr] items-start gap-3 rounded-xl border px-4 py-3",
    toneClass[props.tone]
]);
</script>
