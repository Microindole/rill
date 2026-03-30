package com.indolyn.rill.app.service.impl;

import com.indolyn.rill.app.dto.QueryExecuteResponse;
import com.indolyn.rill.app.dto.QueryHistoryItemResponse;
import com.indolyn.rill.app.dto.QueryTraceStepResponse;
import com.indolyn.rill.app.service.DatabaseExecution;
import com.indolyn.rill.app.service.QueryTraceService;
import com.indolyn.rill.app.service.RillQueryService;
import com.indolyn.rill.core.execution.QueryResult;
import com.indolyn.rill.core.execution.trace.TraceCollector;
import com.indolyn.rill.core.execution.trace.TraceEvent;
import com.indolyn.rill.core.model.Tuple;
import com.indolyn.rill.core.sql.ast.StatementNode;
import com.indolyn.rill.core.sql.lexer.Lexer;
import com.indolyn.rill.core.sql.lexer.Token;
import com.indolyn.rill.core.sql.parser.Parser;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

@Service
public class QueryTraceServiceImpl implements QueryTraceService {

    private static final int MAX_HISTORY_SIZE = 50;

    private final RillQueryService rillQueryService;
    private final Map<String, QueryExecuteResponse> traces = new ConcurrentHashMap<>();
    private final Deque<QueryHistoryItemResponse> history = new ArrayDeque<>();

    public QueryTraceServiceImpl(RillQueryService rillQueryService) {
        this.rillQueryService = rillQueryService;
    }

    @Override
    public QueryExecuteResponse execute(String dbName, String sql) {
        String traceId = UUID.randomUUID().toString();
        Instant executedAt = Instant.now();
        long startedAt = System.nanoTime();
        List<QueryTraceStepResponse> traceSteps = new ArrayList<>();
        String normalizedSql = sql == null ? "" : sql.trim();
        List<Token> tokens = List.of();
        StatementNode ast = null;

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
                    ast == null ? "当前 SQL 解析结果为空。" : "生成 AST 节点 " + ast.getClass().getSimpleName() + "。"));

            TraceCollector.start();
            DatabaseExecution execution = rillQueryService.execute(dbName, normalizedSql);
            List<TraceEvent> runtimeEvents = TraceCollector.stop();
            QueryResult queryResult = execution.queryResult();
            traceSteps.addAll(toTraceSteps(runtimeEvents));

            QueryExecuteResponse response =
                new QueryExecuteResponse(
                    traceId,
                    execution.dbName(),
                    execution.sql(),
                    queryResult.success(),
                    elapsedMs(startedAt),
                    executedAt,
                    execution.rawResult(),
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
                    normalizeDbName(dbName),
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

    @Override
    public QueryExecuteResponse getTrace(String traceId) {
        return traces.get(traceId);
    }

    @Override
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

    private List<QueryTraceStepResponse> toTraceSteps(List<TraceEvent> runtimeEvents) {
        List<QueryTraceStepResponse> steps = new ArrayList<>(runtimeEvents.size());
        for (int index = 0; index < runtimeEvents.size(); index++) {
            TraceEvent event = runtimeEvents.get(index);
            steps.add(
                step(
                    event.stage() + "-" + index,
                    event.stage(),
                    traceTitle(event.stage()),
                    event.component(),
                    "completed",
                    0,
                    event.sourceFile(),
                    event.component(),
                    event.sourceMethod(),
                    event.detail()));
        }
        return steps;
    }

    private String traceTitle(String stage) {
        return switch (stage) {
            case "semantic" -> "语义检查";
            case "planner" -> "执行计划";
            case "execution" -> "执行器构造与执行";
            default -> "运行时事件";
        };
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
            tuple.getValues().forEach(value -> row.add(value.getValue() == null ? "NULL" : value.getValue().toString()));
            rows.add(row);
        }
        return rows;
    }
}
