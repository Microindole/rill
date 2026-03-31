<template>
    <teleport to="body">
        <div v-if="modelValue" class="fixed inset-0 z-50 grid place-items-center p-4" @click.self="close">
            <div class="absolute inset-0 bg-slate-900/28 backdrop-blur-sm"></div>
            <section class="relative mica-panel w-full" :style="panelStyle">
                <header class="flex items-center justify-between border-b border-white/50 px-5 py-4">
                    <h2 class="m-0 text-lg font-semibold text-slate-900">{{ title }}</h2>
                    <button
                        type="button"
                        class="grid h-8 w-8 place-items-center rounded-lg border border-white/70 bg-white/70 text-slate-500 transition hover:bg-white"
                        aria-label="关闭"
                        @click="close"
                    >
                        ×
                    </button>
                </header>
                <div class="px-5 py-4">
                    <slot />
                </div>
                <footer v-if="$slots.footer" class="flex justify-end gap-2 border-t border-white/50 px-5 py-4">
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
