<template>
    <button
        :type="nativeType"
        class="app-button"
        :class="[
            `is-${variant}`,
            `is-${size}`,
            subtle ? 'is-subtle' : '',
            loading ? 'is-loading' : '',
            block ? 'is-block' : ''
        ]"
        :disabled="disabled || loading"
        @click="$emit('click', $event)"
    >
        <span v-if="loading" class="app-button-spinner" aria-hidden="true"></span>
        <span><slot /></span>
    </button>
</template>

<script setup lang="ts">
withDefaults(
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

defineEmits<{
    click: [event: MouseEvent];
}>();
</script>
