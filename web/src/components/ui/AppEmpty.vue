<template>
    <div class="grid min-h-28 place-items-center gap-2 rounded-xl border border-dashed border-white/80 bg-white/45 text-center">
        <div class="h-9 w-9 rounded-xl border border-white/80 bg-white/70"></div>
        <p class="m-0 text-sm text-slate-500">{{ description }}</p>
    </div>
</template>

<script setup lang="ts">
defineProps<{
    description: string;
}>();
</script>
