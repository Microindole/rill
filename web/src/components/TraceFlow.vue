<template>
    <div class="space-y-2">
        <article
            v-for="(step, index) in steps"
            :key="step.id"
            class="rounded-xl border bg-white/70 p-3"
            :class="statusClass(step.status)"
        >
            <div class="mb-2 flex items-center justify-between text-xs text-slate-500">
                <span>{{ String(index + 1).padStart(2, "0") }} · {{ step.stage }}</span>
                <span>{{ step.durationMs }} ms</span>
            </div>
            <h3 class="text-sm font-semibold text-slate-900">{{ step.title }}</h3>
            <p class="mt-1 text-xs text-slate-500">{{ step.component }} · {{ step.sourceMethod }}</p>
            <p class="mt-2 text-sm text-slate-600">{{ step.detail }}</p>
        </article>
    </div>
</template>

<script setup lang="ts">
import type { TraceStep } from "@/types/trace";

defineProps<{
    steps: TraceStep[];
}>();

function statusClass(status: TraceStep["status"]) {
    if (status === "completed") {
        return "border-emerald-100";
    }
    if (status === "running") {
        return "border-amber-100";
    }
    return "border-white/80";
}
</script>
