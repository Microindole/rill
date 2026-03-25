package com.indolyn.rill.app.service;

import com.indolyn.rill.app.dto.QueryExecuteResponse;
import com.indolyn.rill.app.dto.QueryHistoryItemResponse;
import com.indolyn.rill.app.dto.QueryTraceStepResponse;
import com.indolyn.rill.core.execution.QueryProcessor;
import com.indolyn.rill.core.execution.QueryResult;
import com.indolyn.rill.core.model.Tuple;
import com.indolyn.rill.core.session.Session;
import com.indolyn.rill.core.sql.ast.StatementNode;
import com.indolyn.rill.core.sql.lexer.Lexer;
import com.indolyn.rill.core.sql.lexer.Token;
import com.indolyn.rill.core.sql.parser.Parser;
import com.indolyn.rill.core.sql.planner.Planner;
import com.indolyn.rill.core.sql.planner.plan.PlanNode;
import com.indolyn.rill.core.sql.planner.plan.command.CreateDatabasePlanNode;
import com.indolyn.rill.core.sql.planner.plan.command.CreateIndexPlanNode;
import com.indolyn.rill.core.sql.planner.plan.command.CreateTablePlanNode;
import com.indolyn.rill.core.sql.planner.plan.command.DeletePlanNode;
import com.indolyn.rill.core.sql.planner.plan.command.InsertPlanNode;
import com.indolyn.rill.core.sql.planner.plan.command.UpdatePlanNode;
import com.indolyn.rill.core.sql.planner.plan.query.AggregatePlanNode;
import com.indolyn.rill.core.sql.planner.plan.query.FilterPlanNode;
import com.indolyn.rill.core.sql.planner.plan.query.IndexScanPlanNode;
import com.indolyn.rill.core.sql.planner.plan.query.JoinPlanNode;
import com.indolyn.rill.core.sql.planner.plan.query.LimitPlanNode;
import com.indolyn.rill.core.sql.planner.plan.query.ProjectPlanNode;
import com.indolyn.rill.core.sql.planner.plan.query.SeqScanPlanNode;
import com.indolyn.rill.core.sql.planner.plan.query.SortPlanNode;
import com.indolyn.rill.core.sql.semantic.SemanticAnalyzer;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

@Service
public class QueryTraceService {

    private static final int MAX_HISTORY_SIZE = 50;

    private final QueryProcessorRegistry registry;
    private final Map<String, QueryExecuteResponse> traces = new ConcurrentHashMap<>();
    private final Deque<QueryHistoryItemResponse> history = new ArrayDeque<>();

    public QueryTraceService(QueryProcessorRegistry registry) {
        this.registry = registry;
    }

    public QueryExecuteResponse execute(String dbName, String sql) {
        String normalizedDbName = normalizeDbName(dbName);
        String normalizedSql = sql == null ? "" : sql.trim();
        String traceId = UUID.randomUUID().toString();
        Instant executedAt = Instant.now();
        long startedAt = System.nanoTime();
        List<QueryTraceStepResponse> traceSteps = new ArrayList<>();
        Session session = Session.createAuthenticatedSession(-1, "root");
        session.setCurrentDatabase(normalizedDbName);

        QueryProcessor processor = registry.getOrCreate(normalizedDbName);
        List<Token> tokens = List.of();
        StatementNode ast = null;
        PlanNode plan = null;

        try {
            long lexerStartedAt = System.nanoTime();
            tokens = new Lexer(normalizedSql).tokenize();
            traceSteps.add(
                step(
                    "lexer",
                    "lexer",
                    "词法分析",
                    "Lexer",
                    "completed",
                    elapsedMs(lexerStartedAt),
                    "src/main/java/com/indolyn/rill/core/sql/lexer/Lexer.java",
                    "Lexer",
                    "tokenize",
                    "生成 " + tokens.size() + " 个 token。"));

            long parserStartedAt = System.nanoTime();
            ast = new Parser(tokens).parse();
            traceSteps.add(
                step(
                    "parser",
                    "parser",
                    "语法分析",
                    "Parser",
                    "completed",
                    elapsedMs(parserStartedAt),
                    "src/main/java/com/indolyn/rill/core/sql/parser/Parser.java",
                    "Parser",
                    "parse",
                    ast == null
                        ? "当前 SQL 解析结果为空。"
                        : "生成 AST 节点 " + ast.getClass().getSimpleName() + "。"));

            if (ast != null) {
                long semanticStartedAt = System.nanoTime();
                SemanticAnalyzer semanticAnalyzer = new SemanticAnalyzer(processor.getCatalog());
                semanticAnalyzer.analyze(ast, session);
                traceSteps.add(
                    step(
                        "semantic",
                        "semantic",
                        "语义检查",
                        "SemanticAnalyzer",
                        "completed",
                        elapsedMs(semanticStartedAt),
                        "src/main/java/com/indolyn/rill/core/sql/semantic/SemanticAnalyzer.java",
                        "SemanticAnalyzer",
                        "analyze",
                        "完成表、列、类型与权限校验。"));

                long plannerStartedAt = System.nanoTime();
                plan = new Planner(processor.getCatalog()).createPlan(ast);
                traceSteps.add(
                    step(
                        "planner",
                        "planner",
                        "执行计划",
                        plannerComponent(plan),
                        "completed",
                        elapsedMs(plannerStartedAt),
                        plannerSourceFile(plan),
                        plannerComponent(plan),
                        "build/createPlan",
                        plannerDetail(plan)));
            }

            long executionStartedAt = System.nanoTime();
            QueryResult queryResult = processor.executeStructured(normalizedSql, session);
            String rawResult = processor.render(queryResult);
            traceSteps.add(
                step(
                    "execution",
                    "execution",
                    "执行器构造与执行",
                    "ExecutionEngine",
                    queryResult.success() ? "completed" : "failed",
                    elapsedMs(executionStartedAt),
                    "src/main/java/com/indolyn/rill/core/execution/ExecutionEngine.java",
                    "ExecutionEngine",
                    "execute",
                    executionDetail(plan)));

            QueryExecuteResponse response =
                new QueryExecuteResponse(
                    traceId,
                    normalizedDbName,
                    normalizedSql,
                    queryResult.success(),
                    elapsedMs(startedAt),
                    executedAt,
                    rawResult,
                    toColumns(queryResult),
                    toRows(queryResult),
                    traceSteps);
            recordHistory(response);
            return response;
        } catch (Exception e) {
            traceSteps.add(
                step(
                    "failure",
                    "execution",
                    "执行失败",
                    "QueryProcessor",
                    "failed",
                    elapsedMs(startedAt),
                    "src/main/java/com/indolyn/rill/core/execution/QueryProcessor.java",
                    "QueryProcessor",
                    "executeAndGetResult",
                    e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage()));

            QueryExecuteResponse response =
                new QueryExecuteResponse(
                    traceId,
                    normalizedDbName,
                    normalizedSql,
                    false,
                    elapsedMs(startedAt),
                    executedAt,
                    "ERROR: " + e.getMessage(),
                    List.of(),
                    List.of(),
                    traceSteps);
            recordHistory(response);
            return response;
        }
    }

    public QueryExecuteResponse getTrace(String traceId) {
        return traces.get(traceId);
    }

    public List<QueryHistoryItemResponse> getHistory() {
        return List.copyOf(history);
    }

    private void recordHistory(QueryExecuteResponse response) {
        traces.put(response.traceId(), response);
        history.addFirst(
            new QueryHistoryItemResponse(
                response.traceId(),
                response.dbName(),
                response.sql(),
                response.success(),
                response.elapsedMs(),
                response.executedAt()));
        while (history.size() > MAX_HISTORY_SIZE) {
            history.removeLast();
        }
    }

    private QueryTraceStepResponse step(
        String id,
        String stage,
        String title,
        String component,
        String status,
        long durationMs,
        String sourceFile,
        String sourceClass,
        String sourceMethod,
        String detail) {
        return new QueryTraceStepResponse(
            id, stage, title, component, status, durationMs, sourceFile, sourceClass, sourceMethod, detail);
    }

    private long elapsedMs(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000;
    }

    private String normalizeDbName(String dbName) {
        if (dbName == null || dbName.isBlank()) {
            return "default";
        }
        return dbName.trim();
    }

    private String plannerComponent(PlanNode plan) {
        if (plan == null) {
            return "Planner";
        }
        if (plan instanceof CreateTablePlanNode) {
            return "CreateTablePlanBuilder";
        }
        if (plan instanceof CreateIndexPlanNode) {
            return "CreateIndexPlanBuilder";
        }
        if (plan instanceof InsertPlanNode) {
            return "InsertPlanBuilder";
        }
        if (plan instanceof DeletePlanNode) {
            return "DeletePlanBuilder";
        }
        if (plan instanceof UpdatePlanNode) {
            return "UpdatePlanBuilder";
        }
        if (plan instanceof SeqScanPlanNode
            || plan instanceof FilterPlanNode
            || plan instanceof ProjectPlanNode
            || plan instanceof SortPlanNode
            || plan instanceof LimitPlanNode
            || plan instanceof JoinPlanNode
            || plan instanceof AggregatePlanNode
            || plan instanceof IndexScanPlanNode) {
            return "SelectPlanBuilder";
        }
        return "Planner";
    }

    private String plannerSourceFile(PlanNode plan) {
        String builder = plannerComponent(plan);
        if ("Planner".equals(builder)) {
            return "src/main/java/com/indolyn/rill/core/sql/planner/Planner.java";
        }
        return "src/main/java/com/indolyn/rill/core/sql/planner/" + builder + ".java";
    }

    private String plannerDetail(PlanNode plan) {
        if (plan == null) {
            return "当前 SQL 未生成计划节点。";
        }
        return "生成计划根节点 " + plan.getClass().getSimpleName() + "。";
    }

    private String executionDetail(PlanNode plan) {
        if (plan == null) {
            return "执行内建命令或无计划查询。";
        }
        LinkedHashSet<String> components = new LinkedHashSet<>();
        collectExecutionComponents(plan, components);
        return "执行链路涉及 " + String.join(" -> ", components) + "。";
    }

    private void collectExecutionComponents(PlanNode plan, LinkedHashSet<String> components) {
        if (plan == null) {
            return;
        }
        components.add(mapExecutorComponent(plan));
        if (plan instanceof FilterPlanNode filterPlanNode) {
            collectExecutionComponents(filterPlanNode.getChild(), components);
        } else if (plan instanceof ProjectPlanNode projectPlanNode) {
            collectExecutionComponents(projectPlanNode.getChild(), components);
        } else if (plan instanceof SortPlanNode sortPlanNode) {
            collectExecutionComponents(sortPlanNode.getChild(), components);
        } else if (plan instanceof LimitPlanNode limitPlanNode) {
            collectExecutionComponents(limitPlanNode.getChild(), components);
        } else if (plan instanceof AggregatePlanNode aggregatePlanNode) {
            collectExecutionComponents(aggregatePlanNode.getChild(), components);
        } else if (plan instanceof DeletePlanNode deletePlanNode) {
            collectExecutionComponents(deletePlanNode.getChild(), components);
        } else if (plan instanceof UpdatePlanNode updatePlanNode) {
            collectExecutionComponents(updatePlanNode.getChild(), components);
        } else if (plan instanceof JoinPlanNode joinPlanNode) {
            collectExecutionComponents(joinPlanNode.getLeft(), components);
            collectExecutionComponents(joinPlanNode.getRight(), components);
        }
    }

    private String mapExecutorComponent(PlanNode plan) {
        if (plan instanceof CreateTablePlanNode) {
            return "CreateTableExecutor";
        }
        if (plan instanceof CreateIndexPlanNode) {
            return "CreateIndexExecutor";
        }
        if (plan instanceof CreateDatabasePlanNode) {
            return "CreateDatabaseExecutor";
        }
        if (plan instanceof InsertPlanNode) {
            return "InsertExecutor";
        }
        if (plan instanceof DeletePlanNode) {
            return "DeleteExecutor";
        }
        if (plan instanceof UpdatePlanNode) {
            return "UpdateExecutor";
        }
        if (plan instanceof SeqScanPlanNode) {
            return "SeqScanExecutor";
        }
        if (plan instanceof FilterPlanNode) {
            return "FilterExecutor";
        }
        if (plan instanceof ProjectPlanNode) {
            return "ProjectExecutor";
        }
        if (plan instanceof SortPlanNode) {
            return "SortExecutor";
        }
        if (plan instanceof LimitPlanNode) {
            return "LimitExecutor";
        }
        if (plan instanceof JoinPlanNode) {
            return "JoinExecutor";
        }
        if (plan instanceof AggregatePlanNode) {
            return "AggregateExecutor";
        }
        if (plan instanceof IndexScanPlanNode) {
            return "IndexScanExecutor";
        }
        return "ExecutionEngine";
    }

    private List<String> toColumns(QueryResult queryResult) {
        if (!queryResult.success() || queryResult.schema() == null) {
            return List.of();
        }
        return queryResult.schema().getColumnNames();
    }

    private List<List<String>> toRows(QueryResult queryResult) {
        if (!queryResult.success() || queryResult.schema() == null) {
            return List.of();
        }
        List<List<String>> rows = new ArrayList<>();
        for (Tuple tuple : queryResult.results()) {
            List<String> row = new ArrayList<>(tuple.getValues().size());
            tuple.getValues()
                .forEach(value -> row.add(value.getValue() == null ? "NULL" : value.getValue().toString()));
            rows.add(row);
        }
        return rows;
    }
}
