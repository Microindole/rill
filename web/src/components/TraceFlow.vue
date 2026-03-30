<template>
    <div class="trace-panel">
        <div class="trace-rail">
            <div v-for="(step, index) in steps" :key="step.id" class="trace-step-card" :class="`is-${step.status}`">
                <div class="trace-step-head">
                    <span class="trace-step-index">{{ String(index + 1).padStart(2, "0") }}</span>
                    <span class="trace-step-stage">{{ step.stage }}</span>
                </div>
                <strong class="trace-step-title">{{ step.title }}</strong>
                <p class="trace-step-component">{{ step.component }}</p>
                <div class="trace-step-meta">
                    <span>{{ step.durationMs }} ms</span>
                    <span>{{ step.sourceMethod }}</span>
                </div>
                <p class="trace-step-detail">{{ step.detail }}</p>
                <div v-if="index < steps.length - 1" class="trace-step-connector" aria-hidden="true"></div>
            </div>
        </div>
    </div>
</template>

<script setup lang="ts">
import type { TraceStep } from "@/types/trace";

defineProps<{
    steps: TraceStep[];
}>();
</script>
