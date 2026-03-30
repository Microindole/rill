import { apiBaseUrl } from "@/config/env";
import type {
    ActionMessageResponse,
    AuthConfig,
    LoginPayload,
    LoginResponse,
    PasswordChangeRequestPayload,
    PasswordResetConfirmPayload,
    PasswordResetRequestPayload,
    RegisterPayload,
    AuthUser
} from "@/types/auth";
import type { SystemOverview } from "@/types/overview";
import type { QueryExecutionResult } from "@/types/trace";

export interface QueryHistoryItem {
    traceId: string;
    dbName: string;
    sql: string;
    success: boolean;
    elapsedMs: number;
    executedAt: string;
}

export interface WorkspaceSessionSummary {
    sessionId: string;
    currentDatabase: string;
    createdAt: string;
    lastUsedAt: string;
    recentQueryCount: number;
}

export interface WorkspaceSession {
    sessionId: string;
    currentDatabase: string;
    createdAt: string;
    lastUsedAt: string;
    loadedDatabases: string[];
    recentQueries: QueryHistoryItem[];
}

export interface DashboardResponse {
    totalSessions: number;
    totalQueryHistory: number;
    totalSnippets: number;
    totalScenarios: number;
    loadedDatabases: string[];
    sessions: WorkspaceSessionSummary[];
    recentQueries: QueryHistoryItem[];
}

export interface SqlSnippet {
    id: number;
    title: string;
    description: string | null;
    sql: string;
    createdAt: string;
    updatedAt: string;
}

export interface SqlSnippetRequestPayload {
    title: string;
    description?: string | null;
    sql: string;
}

export interface DemoScenario {
    id: number;
    title: string;
    description: string | null;
    sqlScript: string;
    createdAt: string;
    updatedAt: string;
}

export interface DemoScenarioRequestPayload {
    title: string;
    description?: string | null;
    sqlScript: string;
}

export interface DemoScenarioRunResponse {
    scenarioId: number;
    sessionId: string;
    statementsExecuted: number;
    finalDatabase: string;
    executions: QueryExecutionResult[];
}

export interface ExportTask {
    id: number;
    title: string;
    description: string | null;
    dbName: string;
    sql: string;
    exportFormat: string;
    status: string;
    outputPath: string | null;
    lastError: string | null;
    createdAt: string;
    updatedAt: string;
    completedAt: string | null;
}

export interface ExportTaskRequestPayload {
    title: string;
    description?: string | null;
    dbName?: string;
    sql: string;
    exportFormat: "csv" | "json";
}

export interface AdminUser {
    userId: number;
    username: string;
    email: string;
    emailVerified: boolean;
    displayName: string;
    role: string;
    kernelDbName: string;
    kernelDbProvisioned: boolean;
}

interface ExecuteQueryRequest {
    sql: string;
}

type HttpMethod = "GET" | "POST" | "PUT" | "DELETE";

function authHeaders(token?: string): HeadersInit {
    return token
        ? {
              Authorization: `Bearer ${token}`
          }
        : {};
}

async function parseJsonResponse<T>(response: Response): Promise<T> {
    if (!response.ok) {
        let message = `HTTP ${response.status}`;
        try {
            const data = (await response.json()) as { message?: string };
            if (data.message) {
                message = data.message;
            }
        } catch {
            // ignore non-json error payload
        }
        throw new Error(message);
    }
    return (await response.json()) as T;
}

async function request<T>(path: string, method: HttpMethod, token?: string, body?: unknown): Promise<T> {
    const response = await fetch(`${apiBaseUrl}${path}`, {
        method,
        headers: {
            "Content-Type": "application/json",
            ...authHeaders(token)
        },
        body: body === undefined ? undefined : JSON.stringify(body)
    });
    if (response.status === 204) {
        return undefined as T;
    }
    return parseJsonResponse<T>(response);
}

export function getAuthConfig(): Promise<AuthConfig> {
    return request<AuthConfig>("/api/auth/config", "GET");
}

export function register(payload: RegisterPayload): Promise<ActionMessageResponse> {
    return request<ActionMessageResponse>("/api/auth/register", "POST", undefined, payload);
}

export function login(payload: LoginPayload): Promise<LoginResponse> {
    return request<LoginResponse>("/api/auth/login", "POST", undefined, payload);
}

export function confirmRegister(token: string): Promise<LoginResponse> {
    return request<LoginResponse>("/api/auth/register/confirm", "POST", undefined, { token });
}

export function requestPasswordReset(payload: PasswordResetRequestPayload): Promise<ActionMessageResponse> {
    return request<ActionMessageResponse>("/api/auth/password/reset/request", "POST", undefined, payload);
}

export function confirmPasswordReset(payload: PasswordResetConfirmPayload): Promise<ActionMessageResponse> {
    return request<ActionMessageResponse>("/api/auth/password/reset/confirm", "POST", undefined, payload);
}

export function requestPasswordChange(
    token: string | undefined,
    payload: PasswordChangeRequestPayload
): Promise<ActionMessageResponse> {
    return request<ActionMessageResponse>("/api/auth/password/change/request", "POST", token, payload);
}

export function confirmPasswordChange(payload: PasswordResetConfirmPayload): Promise<ActionMessageResponse> {
    return request<ActionMessageResponse>("/api/auth/password/change/confirm", "POST", undefined, payload);
}

export function currentUser(token?: string): Promise<AuthUser> {
    return request<AuthUser>("/api/auth/me", "GET", token);
}

export function logout(token?: string): Promise<void> {
    return request<void>("/api/auth/logout", "DELETE", token);
}

export function getSystemOverview(): Promise<SystemOverview> {
    return request<SystemOverview>("/api/overview", "GET");
}

export function getDashboard(token?: string): Promise<DashboardResponse> {
    return request<DashboardResponse>("/api/workspace/dashboard", "GET", token);
}

export function listWorkspaceSessions(token?: string): Promise<WorkspaceSessionSummary[]> {
    return request<WorkspaceSessionSummary[]>("/api/workspace/sessions", "GET", token);
}

export function createWorkspaceSession(token?: string): Promise<WorkspaceSession> {
    return request<WorkspaceSession>("/api/workspace/sessions", "POST", token);
}

export function getWorkspaceSession(token: string | undefined, sessionId: string): Promise<WorkspaceSession> {
    return request<WorkspaceSession>(`/api/workspace/sessions/${sessionId}`, "GET", token);
}

export function deleteWorkspaceSession(token: string | undefined, sessionId: string): Promise<void> {
    return request<void>(`/api/workspace/sessions/${sessionId}`, "DELETE", token);
}

export function executeQuery(
    token: string | undefined,
    sessionId: string,
    requestBody: ExecuteQueryRequest
): Promise<QueryExecutionResult> {
    return request<QueryExecutionResult>(
        `/api/workspace/sessions/${sessionId}/execute`,
        "POST",
        token,
        requestBody
    );
}

export function listSnippets(token?: string): Promise<SqlSnippet[]> {
    return request<SqlSnippet[]>("/api/workspace/snippets", "GET", token);
}

export function createSqlSnippet(
    token: string | undefined,
    payload: SqlSnippetRequestPayload
): Promise<SqlSnippet> {
    return request<SqlSnippet>("/api/workspace/snippets", "POST", token, payload);
}

export function deleteSqlSnippet(token: string | undefined, id: number): Promise<void> {
    return request<void>(`/api/workspace/snippets/${id}`, "DELETE", token);
}

export function listScenarios(token?: string): Promise<DemoScenario[]> {
    return request<DemoScenario[]>("/api/workspace/scenarios", "GET", token);
}

export function createScenario(
    token: string | undefined,
    payload: DemoScenarioRequestPayload
): Promise<DemoScenario> {
    return request<DemoScenario>("/api/workspace/scenarios", "POST", token, payload);
}

export function deleteScenario(token: string | undefined, id: number): Promise<void> {
    return request<void>(`/api/workspace/scenarios/${id}`, "DELETE", token);
}

export function runScenario(
    token: string | undefined,
    id: number,
    sessionId: string
): Promise<DemoScenarioRunResponse> {
    return request<DemoScenarioRunResponse>(`/api/workspace/scenarios/${id}/run/${sessionId}`, "POST", token);
}

export function listExportTasks(token?: string): Promise<ExportTask[]> {
    return request<ExportTask[]>("/api/workspace/export-tasks", "GET", token);
}

export function createExportTask(
    token: string | undefined,
    payload: ExportTaskRequestPayload
): Promise<ExportTask> {
    return request<ExportTask>("/api/workspace/export-tasks", "POST", token, payload);
}

export function runExportTask(token: string | undefined, id: number): Promise<ExportTask> {
    return request<ExportTask>(`/api/workspace/export-tasks/${id}/run`, "POST", token);
}

export function deleteExportTask(token: string | undefined, id: number): Promise<void> {
    return request<void>(`/api/workspace/export-tasks/${id}`, "DELETE", token);
}

export function listAdminUsers(token?: string): Promise<AdminUser[]> {
    return request<AdminUser[]>("/api/admin/users", "GET", token);
}

export function provisionAdminUserDatabase(token: string | undefined, userId: number): Promise<AdminUser> {
    return request<AdminUser>(`/api/admin/users/${userId}/database/provision`, "POST", token);
}

export function deleteAdminUserDatabase(token: string | undefined, userId: number): Promise<void> {
    return request<void>(`/api/admin/users/${userId}/database`, "DELETE", token);
}
