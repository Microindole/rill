package com.indolyn.rill.app.dto;

import java.util.List;

public record DemoScenarioRunResponse(
    Long scenarioId,
    String sessionId,
    int statementsExecuted,
    String finalDatabase,
    List<QueryExecuteResponse> executions) {
}
