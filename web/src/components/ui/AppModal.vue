<template>
    <teleport to="body">
        <div v-if="modelValue" class="app-modal" @click.self="close">
            <div class="app-modal-backdrop"></div>
            <section class="app-modal-panel" :style="panelStyle">
                <header class="app-modal-header">
                    <h2>{{ title }}</h2>
                    <button type="button" class="app-modal-close" aria-label="关闭" @click="close">×</button>
                </header>
                <div class="app-modal-body">
                    <slot />
                </div>
                <footer v-if="$slots.footer" class="app-modal-footer">
                    <slot name="footer" />
                </footer>
            </section>
        </div>
    </teleport>
</template>

<script setup lang="ts">
import { computed } from "vue";

const props = withDefaults(
    defineProps<{
        modelValue: boolean;
        title: string;
        width?: string;
    }>(),
    {
        width: "420px"
    }
);

const emit = defineEmits<{
    "update:modelValue": [value: boolean];
}>();

const panelStyle = computed(() => ({
    width: `min(calc(100vw - 32px), ${props.width})`
}));

function close() {
    emit("update:modelValue", false);
}
</script>
