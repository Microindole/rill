import { computed, ref } from "vue";
import { defineStore } from "pinia";
import { mockOverview } from "@/data/mockOverview";
import { mockQueryResult } from "@/data/mockTrace";
import {
    createWorkspaceSession,
    deleteAdminUserDatabase,
    executeQuery,
    getDashboard,
    getSystemOverview,
    getWorkspaceSession,
    listExportTasks,
    listAdminUsers,
    listScenarios,
    listSnippets,
    listWorkspaceSessions,
    provisionAdminUserDatabase,
    type AdminUser,
    type DashboardResponse,
    type DemoScenario,
    type ExportTask,
    type SqlSnippet,
    type WorkspaceSession,
    type WorkspaceSessionSummary
} from "@/services/api";
import { useAuthStore } from "@/stores/auth";
import type { SystemOverview } from "@/types/overview";
import type { QueryExecutionResult } from "@/types/trace";

export const usePlatformStore = defineStore("platform", () => {
    const auth = useAuthStore();
    const overview = ref<SystemOverview>(mockOverview);
    const overviewLoading = ref(false);
    const overviewUsingMock = ref(true);
    const dashboard = ref<DashboardResponse | null>(null);
    const sessions = ref<WorkspaceSessionSummary[]>([]);
    const activeSession = ref<WorkspaceSession | null>(null);
    const snippets = ref<SqlSnippet[]>([]);
    const scenarios = ref<DemoScenario[]>([]);
    const exportTasks = ref<ExportTask[]>([]);
    const adminUsers = ref<AdminUser[]>([]);
    const result = ref<QueryExecutionResult | null>(mockQueryResult);
    const running = ref(false);
    const sql = ref("show tables;");
    const dataError = ref("");

    const currentDatabase = computed(() => activeSession.value?.currentDatabase ?? "default");

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

    async function loadWorkspaceData() {
        dataError.value = "";
        try {
            const [dashboardResult, sessionList, snippetList, scenarioList, exportTaskList] = await Promise.all([
                getDashboard(auth.token || undefined),
                listWorkspaceSessions(auth.token || undefined),
                listSnippets(auth.token || undefined),
                listScenarios(auth.token || undefined),
                listExportTasks(auth.token || undefined)
            ]);
            dashboard.value = dashboardResult;
            sessions.value = sessionList;
            snippets.value = snippetList;
            scenarios.value = scenarioList;
            exportTasks.value = exportTaskList;
            if (auth.user?.role === "ADMIN") {
                adminUsers.value = await listAdminUsers(auth.token || undefined);
            } else {
                adminUsers.value = [];
            }

            if (sessionList.length > 0) {
                await selectSession(sessionList[0].sessionId);
            } else {
                await createSession();
            }
        } catch (err) {
            dataError.value = err instanceof Error ? err.message : "加载工作台数据失败";
        }
    }

    async function createSession() {
        const session = await createWorkspaceSession(auth.token || undefined);
        activeSession.value = session;
        await refreshSummaries();
    }

    async function selectSession(sessionId: string) {
        activeSession.value = await getWorkspaceSession(auth.token || undefined, sessionId);
    }

    async function runQuery() {
        if (!activeSession.value) {
            dataError.value = "请先创建工作台会话";
            return;
        }
        running.value = true;
        dataError.value = "";
        try {
            result.value = await executeQuery(auth.token || undefined, activeSession.value.sessionId, { sql: sql.value });
            activeSession.value = await getWorkspaceSession(auth.token || undefined, activeSession.value.sessionId);
            await refreshSummaries();
        } catch (err) {
            dataError.value = err instanceof Error ? err.message : "执行 SQL 失败";
        } finally {
            running.value = false;
        }
    }

    async function refreshSummaries() {
        sessions.value = await listWorkspaceSessions(auth.token || undefined);
        dashboard.value = await getDashboard(auth.token || undefined);
        if (auth.user?.role === "ADMIN") {
            adminUsers.value = await listAdminUsers(auth.token || undefined);
        }
    }

    async function provisionUserDatabase(userId: number) {
        await provisionAdminUserDatabase(auth.token || undefined, userId);
        await refreshSummaries();
    }

    async function dropUserDatabase(userId: number) {
        await deleteAdminUserDatabase(auth.token || undefined, userId);
        await refreshSummaries();
    }

    function resetWorkspaceState() {
        dashboard.value = null;
        sessions.value = [];
        activeSession.value = null;
        snippets.value = [];
        scenarios.value = [];
        exportTasks.value = [];
        adminUsers.value = [];
        result.value = mockQueryResult;
        dataError.value = "";
    }

    return {
        overview,
        overviewLoading,
        overviewUsingMock,
        dashboard,
        sessions,
        activeSession,
        snippets,
        scenarios,
        exportTasks,
        adminUsers,
        result,
        running,
        sql,
        dataError,
        currentDatabase,
        loadOverview,
        loadWorkspaceData,
        createSession,
        selectSession,
        runQuery,
        provisionUserDatabase,
        dropUserDatabase,
        resetWorkspaceState
    };
});
