<template>
    <div class="app-empty">
        <div class="app-empty-mark" aria-hidden="true"></div>
        <p class="app-empty-text">{{ description }}</p>
    </div>
</template>

<script setup lang="ts">
defineProps<{
    description: string;
}>();
</script>
