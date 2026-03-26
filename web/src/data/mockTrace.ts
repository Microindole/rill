import type { QueryExecutionResult } from "@/types/trace";

export const mockQueryResult: QueryExecutionResult = {
    traceId: "mock-trace",
    dbName: "default",
    sql: "SELECT id, status FROM recovery_test WHERE id = 2 ORDER BY id;",
    success: true,
    elapsedMs: 37,
    executedAt: new Date().toISOString(),
    rawResult:
        "+----+-------------+\n| id | status      |\n+----+-------------+\n| 2  | committed_B |\n+----+-------------+\nQuery finished, 1 rows returned.",
    columns: ["id", "status"],
    rows: [["2", "committed_B"]],
    traceSteps: [
        {
            id: "lexer",
            stage: "lexer",
            title: "词法分析",
            component: "Lexer",
            status: "completed",
            durationMs: 2,
            sourceFile: "src/main/java/com/indolyn/rill/core/sql/lexer/Lexer.java",
            sourceClass: "Lexer",
            sourceMethod: "tokenize",
            detail: "将 SQL 文本拆分为 token 流。"
        },
        {
            id: "parser",
            stage: "parser",
            title: "语法分析",
            component: "Parser",
            status: "completed",
            durationMs: 4,
            sourceFile: "src/main/java/com/indolyn/rill/core/sql/parser/Parser.java",
            sourceClass: "Parser",
            sourceMethod: "parseStatement",
            detail: "构建 SelectStatementNode 抽象语法树。"
        },
        {
            id: "semantic",
            stage: "semantic",
            title: "语义检查",
            component: "SemanticAnalyzer",
            status: "completed",
            durationMs: 6,
            sourceFile: "src/main/java/com/indolyn/rill/core/sql/semantic/SemanticAnalyzer.java",
            sourceClass: "SemanticAnalyzer",
            sourceMethod: "analyze",
            detail: "校验表存在性、列引用与权限。"
        },
        {
            id: "planner",
            stage: "planner",
            title: "执行计划",
            component: "SelectPlanBuilder",
            status: "completed",
            durationMs: 8,
            sourceFile: "src/main/java/com/indolyn/rill/core/sql/planner/SelectPlanBuilder.java",
            sourceClass: "SelectPlanBuilder",
            sourceMethod: "build",
            detail: "生成 SeqScan + Filter + Sort + Project 计划树。"
        },
        {
            id: "execution",
            stage: "execution",
            title: "执行器构造与执行",
            component: "ExecutionEngine",
            status: "completed",
            durationMs: 17,
            sourceFile: "src/main/java/com/indolyn/rill/core/execution/ExecutionEngine.java",
            sourceClass: "ExecutionEngine",
            sourceMethod: "execute",
            detail: "装配 SeqScanExecutor、FilterExecutor、SortExecutor、ProjectExecutor。"
        }
    ]
};
