import { apiBaseUrl } from "@/config/env";
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

interface ExecuteQueryRequest {
    dbName: string;
    sql: string;
}

async function parseJsonResponse<T>(response: Response): Promise<T> {
    if (!response.ok) {
        throw new Error(`HTTP ${response.status}`);
    }
    return (await response.json()) as T;
}

export async function executeQuery(request: ExecuteQueryRequest): Promise<QueryExecutionResult> {
    const response = await fetch(`${apiBaseUrl}/api/query/execute`, {
        method: "POST",
        headers: {
            "Content-Type": "application/json"
        },
        body: JSON.stringify(request)
    });
    return parseJsonResponse<QueryExecutionResult>(response);
}

export async function getQueryHistory(): Promise<QueryHistoryItem[]> {
    const response = await fetch(`${apiBaseUrl}/api/query/history`);
    return parseJsonResponse<QueryHistoryItem[]>(response);
}

export async function getQueryTrace(traceId: string): Promise<QueryExecutionResult> {
    const response = await fetch(`${apiBaseUrl}/api/query/trace/${traceId}`);
    return parseJsonResponse<QueryExecutionResult>(response);
}

export async function getSystemOverview(): Promise<SystemOverview> {
    const response = await fetch(`${apiBaseUrl}/api/overview`);
    return parseJsonResponse<SystemOverview>(response);
}
