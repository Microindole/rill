export type TraceStage =
    | "lexer"
    | "parser"
    | "semantic"
    | "planner"
    | "execution";

export interface TraceStep {
    id: string;
    stage: TraceStage;
    title: string;
    component: string;
    status: "pending" | "running" | "completed";
    durationMs: number;
    sourceFile: string;
    sourceClass: string;
    sourceMethod: string;
    detail: string;
}

export interface QueryExecutionResult {
    traceId: string;
    dbName: string;
    sql: string;
    success: boolean;
    elapsedMs: number;
    executedAt: string;
    rawResult: string;
    columns: string[];
    rows: string[][];
    traceSteps: TraceStep[];
}
