<template>
    <button
        :type="nativeType"
        :class="buttonClass"
        :disabled="disabled || loading"
        @click="$emit('click', $event)"
    >
        <svg
            v-if="loading"
            class="h-4 w-4 animate-spin"
            viewBox="0 0 24 24"
            fill="none"
            xmlns="http://www.w3.org/2000/svg"
            aria-hidden="true"
        >
            <circle cx="12" cy="12" r="10" class="opacity-25" stroke="currentColor" stroke-width="4" />
            <path class="opacity-90" fill="currentColor" d="M4 12a8 8 0 018-8v4a4 4 0 00-4 4H4z" />
        </svg>
        <span><slot /></span>
    </button>
</template>

<script setup lang="ts">
import { computed } from "vue";

const props = withDefaults(
    defineProps<{
        variant?: "default" | "primary" | "danger";
        size?: "sm" | "md" | "lg";
        subtle?: boolean;
        loading?: boolean;
        disabled?: boolean;
        block?: boolean;
        nativeType?: "button" | "submit" | "reset";
    }>(),
    {
        variant: "default",
        size: "md",
        subtle: false,
        loading: false,
        disabled: false,
        block: false,
        nativeType: "button"
    }
);

const sizeClass: Record<string, string> = {
    sm: "h-8 px-3 text-xs",
    md: "h-9 px-4 text-sm",
    lg: "h-10 px-5 text-sm"
};

const variantClass: Record<string, string> = {
    default: "border-slate-300 bg-white text-slate-800 hover:bg-slate-50 active:bg-slate-100",
    primary: "border-[#1f5a96] bg-[#0f6cbd] text-white hover:bg-[#115ea3] active:bg-[#0c5a99]",
    danger: "border-[#b42318] bg-[#d13438] text-white hover:bg-[#b92c30] active:bg-[#a4262c]"
};

const buttonClass = computed(() => [
    "inline-flex items-center justify-center gap-2 rounded-md border font-semibold transition duration-150 disabled:opacity-60 disabled:cursor-not-allowed",
    props.block ? "w-full" : "",
    props.subtle ? "bg-transparent" : "",
    sizeClass[props.size],
    variantClass[props.variant]
]);

defineEmits<{
    click: [event: MouseEvent];
}>();
</script>
