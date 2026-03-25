<template>
    <div class="trace-panel">
        <VueFlow
            :nodes="nodes"
            :edges="edges"
            fit-view-on-init
            class="trace-canvas"
            :nodes-draggable="false"
            :elements-selectable="false"
            :zoom-on-scroll="false"
        >
            <MiniMap />
            <Controls />
        </VueFlow>
    </div>
</template>

<script setup lang="ts">
import { computed } from "vue";
import { VueFlow, type Edge, type Node } from "@vue-flow/core";
import { Controls } from "@vue-flow/controls";
import { MiniMap } from "@vue-flow/minimap";
import type { TraceStep } from "@/types/trace";

const props = defineProps<{
    steps: TraceStep[];
}>();

const nodes = computed<Node[]>(() =>
    props.steps.map((step, index) => ({
        id: step.id,
        position: { x: index * 220, y: 60 },
        data: {
            label: `${step.title}\n${step.component}`
        },
        style: {
            width: "180px",
            borderRadius: "18px",
            padding: "14px",
            border: "1px solid #d6d3d1",
            background: step.status === "completed" ? "#fef7ed" : "#ffffff",
            color: "#292524",
            boxShadow: "0 8px 24px rgba(28, 25, 23, 0.08)",
            whiteSpace: "pre-line"
        }
    }))
);

const edges = computed<Edge[]>(() =>
    props.steps.slice(0, -1).map((step, index) => ({
        id: `${step.id}-${props.steps[index + 1].id}`,
        source: step.id,
        target: props.steps[index + 1].id,
        animated: true,
        style: {
            stroke: "#ea580c",
            strokeWidth: 2
        }
    }))
);
</script>
