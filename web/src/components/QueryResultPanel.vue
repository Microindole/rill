<template>
    <div class="mb-3 flex flex-wrap gap-2">
        <AppButton size="sm" @click="$emit('reuse-sql', result.sql)">回填 SQL</AppButton>
        <AppButton size="sm" subtle @click="$emit('save-snippet', result.sql)">存为片段</AppButton>
        <AppButton size="sm" subtle @click="$emit('save-scenario', result.sql)">存为场景</AppButton>
    </div>

    <AppNotice
        v-if="!result.success"
        tone="error"
        title="执行失败"
        :description="result.rawResult"
        class="mb-3"
    />

    <div v-if="result.columns.length" class="overflow-auto rounded-xl border border-white/80 bg-white/65">
        <table class="min-w-[560px] w-full border-collapse text-sm">
            <thead>
                <tr class="border-b border-slate-200/60 bg-white/80 text-left text-slate-600">
                    <th v-for="column in result.columns" :key="column" class="px-3 py-2.5 font-semibold">{{ column }}</th>
                </tr>
            </thead>
            <tbody>
                <tr v-for="(row, rowIndex) in result.rows" :key="rowIndex" class="border-b border-slate-100/80 text-slate-700">
                    <td v-for="(column, columnIndex) in result.columns" :key="`${rowIndex}-${column}`" class="px-3 py-2.5 whitespace-nowrap">
                        {{ row[columnIndex] }}
                    </td>
                </tr>
            </tbody>
        </table>
    </div>

    <AppEmpty v-if="result.columns.length === 0 && result.success" description="当前语句没有表格结果" />

    <pre class="mt-3 overflow-auto rounded-xl bg-slate-900 px-3 py-2.5 text-xs leading-6 text-slate-100">{{ result.rawResult }}</pre>
</template>

<script setup lang="ts">
import AppButton from "@/components/ui/AppButton.vue";
import AppNotice from "@/components/ui/AppNotice.vue";
import AppEmpty from "@/components/ui/AppEmpty.vue";
import type { QueryExecutionResult } from "@/types/trace";

defineProps<{
    result: QueryExecutionResult;
}>();

defineEmits<{
    "reuse-sql": [sql: string];
    "save-snippet": [sql: string];
    "save-scenario": [sql: string];
}>();
</script>
