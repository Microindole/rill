package com.indolyn.rill.app.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;

import com.indolyn.rill.app.dto.QueryExecuteResponse;
import com.indolyn.rill.app.dto.QueryHistoryItemResponse;
import com.indolyn.rill.app.service.impl.QueryTraceServiceImpl;
import com.indolyn.rill.core.execution.QueryResult;
import com.indolyn.rill.core.execution.trace.TraceCollector;
import com.indolyn.rill.core.model.Column;
import com.indolyn.rill.core.model.DataType;
import com.indolyn.rill.core.model.Schema;
import com.indolyn.rill.core.model.Tuple;
import com.indolyn.rill.core.model.Value;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class QueryTraceServiceTest {

    @Test
    void executeShouldBuildLexerParserAndRuntimeTraceForSuccessfulQuery() {
        RillQueryService rillQueryService = Mockito.mock(RillQueryService.class);
        QueryTraceService service = new QueryTraceServiceImpl(rillQueryService);
        Schema schema = new Schema(List.of(new Column("id", DataType.INT), new Column("name", DataType.VARCHAR)));
        QueryResult queryResult =
            QueryResult.newSelectResult(schema, List.of(new Tuple(List.of(new Value(1), new Value("alice")))));

        doAnswer(
                invocation -> {
                    TraceCollector.record(
                        "semantic",
                        "SelectSemanticValidator",
                        "src/main/java/com/indolyn/rill/core/sql/semantic/SelectSemanticValidator.java",
                        "analyze",
                        "单表查询通过语义检查。");
                    TraceCollector.record(
                        "planner",
                        "SelectPlanBuilder",
                        "src/main/java/com/indolyn/rill/core/sql/planner/SelectPlanBuilder.java",
                        "build",
                        "生成顺序扫描计划。");
                    return new DatabaseExecution("default", "select * from users", queryResult, "OK");
                })
            .when(rillQueryService)
            .execute(eq("default"), eq("select * from users"));

        QueryExecuteResponse response = service.execute("default", "select * from users");

        assertTrue(response.success());
        assertEquals("default", response.dbName());
        assertEquals(List.of("id", "name"), response.columns());
        assertEquals(List.of(List.of("1", "alice")), response.rows());
        assertTrue(response.traceSteps().stream().anyMatch(step -> step.stage().equals("lexer")));
        assertTrue(response.traceSteps().stream().anyMatch(step -> step.stage().equals("parser")));
        assertTrue(response.traceSteps().stream().anyMatch(step -> step.stage().equals("semantic")));
        assertTrue(response.traceSteps().stream().anyMatch(step -> step.stage().equals("planner")));
        assertEquals(response, service.getTrace(response.traceId()));

        List<QueryHistoryItemResponse> history = service.getHistory();
        assertEquals(1, history.size());
        assertEquals(response.traceId(), history.getFirst().traceId());
    }

    @Test
    void executeShouldRecordFailureTraceWhenExecutionThrows() {
        RillQueryService rillQueryService = Mockito.mock(RillQueryService.class);
        QueryTraceService service = new QueryTraceServiceImpl(rillQueryService);

        doThrow(new IllegalStateException("boom"))
            .when(rillQueryService)
            .execute(eq(null), eq("select * from users"));

        QueryExecuteResponse response = service.execute(null, "select * from users");

        assertFalse(response.success());
        assertEquals("default", response.dbName());
        assertTrue(response.rawResult().startsWith("ERROR:"));
        assertTrue(
            response.traceSteps().stream()
                .anyMatch(step -> step.stage().equals("execution") && step.status().equals("failed")));
        assertEquals(1, service.getHistory().size());
    }

    @Test
    void historyShouldBeTrimmedToConfiguredMaximumSize() {
        RillQueryService rillQueryService = Mockito.mock(RillQueryService.class);
        QueryTraceService service = new QueryTraceServiceImpl(rillQueryService);
        QueryResult queryResult = QueryResult.newSuccessResult("OK");

        doAnswer(
                invocation ->
                    new DatabaseExecution(
                        invocation.getArgument(0),
                        invocation.getArgument(1),
                        queryResult,
                        "OK"))
            .when(rillQueryService)
            .execute(Mockito.any(), Mockito.any());

        for (int i = 0; i < 60; i++) {
            service.execute("default", "select " + i);
        }

        List<QueryHistoryItemResponse> history = service.getHistory();
        assertEquals(50, history.size());
        assertTrue(history.getFirst().sql().contains("select 59"));
        assertTrue(history.getLast().sql().contains("select 10"));
    }
}
