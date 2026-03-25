import { defineStore } from "pinia";
import { ref } from "vue";
import { mockQueryResult } from "@/data/mockTrace";
import type { QueryExecutionResult } from "@/types/trace";
import { executeQuery as executeQueryRequest } from "@/services/queryApi";

export const useWorkbenchStore = defineStore("workbench", () => {
    const sql = ref(mockQueryResult.sql);
    const dbName = ref("default");
    const running = ref(false);
    const result = ref<QueryExecutionResult | null>(mockQueryResult);
    const lastError = ref("");
    const usingMock = ref(true);

    async function executeQuery() {
        running.value = true;
        lastError.value = "";
        try {
            result.value = await executeQueryRequest({
                dbName: dbName.value,
                sql: sql.value
            });
            usingMock.value = false;
        } catch (error) {
            result.value = {
                ...mockQueryResult,
                dbName: dbName.value,
                sql: sql.value
            };
            usingMock.value = true;
            lastError.value =
                error instanceof Error ? error.message : "Failed to reach backend API";
        } finally {
            running.value = false;
        }
    }

    return {
        dbName,
        sql,
        running,
        result,
        lastError,
        usingMock,
        executeQuery
    };
});
