<template>
    <div class="result-panel-actions">
        <AppButton size="sm" @click="$emit('reuse-sql', result.sql)">回填到编辑器</AppButton>
        <AppButton size="sm" subtle @click="$emit('save-snippet', result.sql)">存为片段</AppButton>
        <AppButton size="sm" subtle @click="$emit('save-scenario', result.sql)">存为场景</AppButton>
    </div>
    <AppNotice
        v-if="!result.success"
        tone="error"
        title="这次执行失败了"
        :description="result.rawResult"
        class="result-notice"
    />
    <div v-if="result.columns.length" class="result-table-shell">
        <table class="result-table">
            <thead>
                <tr>
                    <th v-for="column in result.columns" :key="column">
                        {{ column }}
                    </th>
                </tr>
            </thead>
            <tbody>
                <tr v-for="(row, rowIndex) in result.rows" :key="rowIndex">
                    <td v-for="(column, columnIndex) in result.columns" :key="`${rowIndex}-${column}`">
                        {{ row[columnIndex] }}
                    </td>
                </tr>
            </tbody>
        </table>
    </div>
    <AppEmpty
        v-if="result.columns.length === 0 && result.success"
        description="当前语句没有表格结果"
    />
    <pre class="raw-result">{{ result.rawResult }}</pre>
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
