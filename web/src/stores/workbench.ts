import { defineStore } from "pinia";
import { ref } from "vue";
import { mockOverview } from "@/data/mockOverview";
import { mockQueryResult } from "@/data/mockTrace";
import { executeQuery as executeQueryRequest, getSystemOverview } from "@/services/queryApi";
import type { SystemOverview } from "@/types/overview";
import type { QueryExecutionResult } from "@/types/trace";

export const useWorkbenchStore = defineStore("workbench", () => {
    const sql = ref(mockQueryResult.sql);
    const dbName = ref("default");
    const running = ref(false);
    const overviewLoading = ref(false);
    const result = ref<QueryExecutionResult | null>(mockQueryResult);
    const overview = ref<SystemOverview>(mockOverview);
    const lastError = ref("");
    const usingMock = ref(true);
    const overviewUsingMock = ref(true);

    async function loadOverview() {
        overviewLoading.value = true;
        try {
            overview.value = await getSystemOverview();
            overviewUsingMock.value = false;
        } catch {
            overview.value = mockOverview;
            overviewUsingMock.value = true;
        } finally {
            overviewLoading.value = false;
        }
    }

    async function executeQuery() {
        running.value = true;
        lastError.value = "";
        try {
            result.value = await executeQueryRequest({
                dbName: dbName.value,
                sql: sql.value
            });
            if (result.value.success && result.value.dbName) {
                dbName.value = result.value.dbName;
            }
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
        overviewLoading,
        result,
        overview,
        lastError,
        usingMock,
        overviewUsingMock,
        loadOverview,
        executeQuery
    };
});
