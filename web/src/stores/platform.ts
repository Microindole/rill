import { computed, ref } from "vue";
import { defineStore } from "pinia";
import { mockOverview } from "@/data/mockOverview";
import {
    createSqlSnippet,
    createScenario,
    createWorkspaceSession,
    createExportTask,
    downloadExportTaskFile,
    deleteScenario,
    deleteSqlSnippet,
    deleteExportTask,
    deleteWorkspaceSession,
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
    runScenario,
    runExportTask,
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

const WORKSPACE_SQL_DRAFT_PREFIX = "rill.workspace.sqlDraft.";

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
    const result = ref<QueryExecutionResult | null>(null);
    const running = ref(false);
    const sql = ref("");
    const dataError = ref("");
    const draftRecovered = ref(false);
    const exportActionLoading = ref(false);
    const snippetActionLoading = ref(false);
    const scenarioActionLoading = ref(false);
    const sessionActionLoading = ref(false);

    const currentDatabase = computed(() => activeSession.value?.currentDatabase ?? "default");

    function readDraft(sessionId: string) {
        if (typeof window === "undefined") {
            return "";
        }
        return window.localStorage.getItem(`${WORKSPACE_SQL_DRAFT_PREFIX}${sessionId}`) ?? "";
    }

    function writeDraft(sessionId: string, nextSql: string) {
        if (typeof window === "undefined") {
            return;
        }
        const key = `${WORKSPACE_SQL_DRAFT_PREFIX}${sessionId}`;
        if (nextSql.trim().length === 0) {
            window.localStorage.removeItem(key);
            return;
        }
        window.localStorage.setItem(key, nextSql);
    }

    function clearDraftStorage(sessionId: string) {
        if (typeof window === "undefined") {
            return;
        }
        window.localStorage.removeItem(`${WORKSPACE_SQL_DRAFT_PREFIX}${sessionId}`);
    }

    function restoreDraftForSession(sessionId: string) {
        const draft = readDraft(sessionId);
        draftRecovered.value = draft.length > 0;
        sql.value = draft;
    }

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
        sessionActionLoading.value = true;
        dataError.value = "";
        try {
            const session = await createWorkspaceSession(auth.token || undefined);
            activeSession.value = session;
            restoreDraftForSession(session.sessionId);
            await refreshSummaries();
        } catch (err) {
            dataError.value = err instanceof Error ? err.message : "创建工作台会话失败";
        } finally {
            sessionActionLoading.value = false;
        }
    }

    async function selectSession(sessionId: string) {
        activeSession.value = await getWorkspaceSession(auth.token || undefined, sessionId);
        restoreDraftForSession(sessionId);
    }

    async function removeSession(sessionId: string) {
        sessionActionLoading.value = true;
        dataError.value = "";
        try {
            await deleteWorkspaceSession(auth.token || undefined, sessionId);
            clearDraftStorage(sessionId);
            result.value = null;
            await refreshSummaries();
            const nextSession = sessions.value[0];
            if (nextSession) {
                await selectSession(nextSession.sessionId);
            } else {
                activeSession.value = null;
                sql.value = "";
                draftRecovered.value = false;
            }
        } catch (err) {
            dataError.value = err instanceof Error ? err.message : "删除工作台会话失败";
        } finally {
            sessionActionLoading.value = false;
        }
    }

    function setSql(nextSql: string) {
        sql.value = nextSql;
        if (activeSession.value) {
            writeDraft(activeSession.value.sessionId, nextSql);
        }
        if (nextSql.trim().length === 0) {
            draftRecovered.value = false;
        }
    }

    function clearSqlDraft() {
        if (activeSession.value) {
            writeDraft(activeSession.value.sessionId, "");
        }
        sql.value = "";
        draftRecovered.value = false;
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
        snippets.value = await listSnippets(auth.token || undefined);
        scenarios.value = await listScenarios(auth.token || undefined);
        exportTasks.value = await listExportTasks(auth.token || undefined);
        if (auth.user?.role === "ADMIN") {
            adminUsers.value = await listAdminUsers(auth.token || undefined);
        }
    }

    async function createSnippetFromCurrentSql(payload: { title: string; description?: string }) {
        if (!sql.value.trim()) {
            dataError.value = "当前 SQL 为空，无法保存片段";
            return;
        }
        snippetActionLoading.value = true;
        dataError.value = "";
        try {
            await createSqlSnippet(auth.token || undefined, {
                title: payload.title.trim(),
                description: payload.description?.trim() || null,
                sql: sql.value
            });
            snippets.value = await listSnippets(auth.token || undefined);
            dashboard.value = await getDashboard(auth.token || undefined);
        } catch (err) {
            dataError.value = err instanceof Error ? err.message : "创建 SQL 片段失败";
        } finally {
            snippetActionLoading.value = false;
        }
    }

    async function removeSnippet(snippetId: number) {
        snippetActionLoading.value = true;
        dataError.value = "";
        try {
            await deleteSqlSnippet(auth.token || undefined, snippetId);
            snippets.value = await listSnippets(auth.token || undefined);
            dashboard.value = await getDashboard(auth.token || undefined);
        } catch (err) {
            dataError.value = err instanceof Error ? err.message : "删除 SQL 片段失败";
        } finally {
            snippetActionLoading.value = false;
        }
    }

    async function createTaskFromCurrentSql(payload: { title: string; description?: string; exportFormat: "csv" | "json"; runImmediately: boolean }) {
        if (!sql.value.trim()) {
            dataError.value = "当前 SQL 为空，无法创建导出任务";
            return;
        }
        exportActionLoading.value = true;
        dataError.value = "";
        try {
            const created = await createExportTask(auth.token || undefined, {
                title: payload.title.trim(),
                description: payload.description?.trim() || null,
                dbName: currentDatabase.value,
                sql: sql.value,
                exportFormat: payload.exportFormat
            });
            if (payload.runImmediately) {
                await runExportTask(auth.token || undefined, created.id);
            }
            exportTasks.value = await listExportTasks(auth.token || undefined);
        } catch (err) {
            dataError.value = err instanceof Error ? err.message : "创建导出任务失败";
        } finally {
            exportActionLoading.value = false;
        }
    }

    async function createScenarioFromCurrentSql(payload: { title: string; description?: string }) {
        if (!sql.value.trim()) {
            dataError.value = "当前 SQL 为空，无法保存场景";
            return;
        }
        scenarioActionLoading.value = true;
        dataError.value = "";
        try {
            await createScenario(auth.token || undefined, {
                title: payload.title.trim(),
                description: payload.description?.trim() || null,
                sqlScript: sql.value
            });
            scenarios.value = await listScenarios(auth.token || undefined);
            dashboard.value = await getDashboard(auth.token || undefined);
        } catch (err) {
            dataError.value = err instanceof Error ? err.message : "创建场景脚本失败";
        } finally {
            scenarioActionLoading.value = false;
        }
    }

    async function triggerScenario(scenarioId: number) {
        if (!activeSession.value) {
            dataError.value = "请先创建工作台会话后再运行场景";
            return;
        }
        scenarioActionLoading.value = true;
        dataError.value = "";
        try {
            const response = await runScenario(auth.token || undefined, scenarioId, activeSession.value.sessionId);
            activeSession.value = await getWorkspaceSession(auth.token || undefined, activeSession.value.sessionId);
            await refreshSummaries();
            const lastExecution = response.executions.length
                ? response.executions[response.executions.length - 1]
                : null;
            if (lastExecution) {
                result.value = lastExecution;
            }
        } catch (err) {
            dataError.value = err instanceof Error ? err.message : "运行场景脚本失败";
        } finally {
            scenarioActionLoading.value = false;
        }
    }

    async function removeScenario(scenarioId: number) {
        scenarioActionLoading.value = true;
        dataError.value = "";
        try {
            await deleteScenario(auth.token || undefined, scenarioId);
            scenarios.value = await listScenarios(auth.token || undefined);
            dashboard.value = await getDashboard(auth.token || undefined);
        } catch (err) {
            dataError.value = err instanceof Error ? err.message : "删除场景脚本失败";
        } finally {
            scenarioActionLoading.value = false;
        }
    }

    async function triggerExportTask(taskId: number) {
        exportActionLoading.value = true;
        dataError.value = "";
        try {
            await runExportTask(auth.token || undefined, taskId);
            exportTasks.value = await listExportTasks(auth.token || undefined);
        } catch (err) {
            dataError.value = err instanceof Error ? err.message : "执行导出任务失败";
        } finally {
            exportActionLoading.value = false;
        }
    }

    async function removeExportTask(taskId: number) {
        exportActionLoading.value = true;
        dataError.value = "";
        try {
            await deleteExportTask(auth.token || undefined, taskId);
            exportTasks.value = await listExportTasks(auth.token || undefined);
        } catch (err) {
            dataError.value = err instanceof Error ? err.message : "删除导出任务失败";
        } finally {
            exportActionLoading.value = false;
        }
    }

    async function downloadExportTask(taskId: number) {
        dataError.value = "";
        try {
            const { blob, filename } = await downloadExportTaskFile(auth.token || undefined, taskId);
            if (typeof window === "undefined") {
                return;
            }
            const objectUrl = window.URL.createObjectURL(blob);
            const anchor = window.document.createElement("a");
            anchor.href = objectUrl;
            anchor.download = filename;
            window.document.body.appendChild(anchor);
            anchor.click();
            anchor.remove();
            window.URL.revokeObjectURL(objectUrl);
        } catch (err) {
            dataError.value = err instanceof Error ? err.message : "下载导出文件失败";
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
        result.value = null;
        draftRecovered.value = false;
        sql.value = "";
        dataError.value = "";
        exportActionLoading.value = false;
        snippetActionLoading.value = false;
        scenarioActionLoading.value = false;
        sessionActionLoading.value = false;
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
        draftRecovered,
        exportActionLoading,
        snippetActionLoading,
        scenarioActionLoading,
        sessionActionLoading,
        currentDatabase,
        loadOverview,
        loadWorkspaceData,
        createSession,
        selectSession,
        removeSession,
        setSql,
        clearSqlDraft,
        runQuery,
        createSnippetFromCurrentSql,
        removeSnippet,
        createScenarioFromCurrentSql,
        triggerScenario,
        removeScenario,
        createTaskFromCurrentSql,
        triggerExportTask,
        downloadExportTask,
        removeExportTask,
        provisionUserDatabase,
        dropUserDatabase,
        resetWorkspaceState
    };
});
