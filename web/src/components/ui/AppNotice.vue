<template>
    <div class="app-notice" :class="`is-${tone}`" role="alert">
        <div class="app-notice-icon" aria-hidden="true">{{ icon }}</div>
        <div class="app-notice-body">
            <strong v-if="title" class="app-notice-title">{{ title }}</strong>
            <p class="app-notice-text">{{ description }}</p>
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
</script>
