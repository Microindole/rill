package com.indolyn.rill.app.dto;

import java.time.Instant;
import java.util.List;

public record QueryExecuteResponse(
    String traceId,
    String dbName,
    String sql,
    boolean success,
    long elapsedMs,
    Instant executedAt,
    String rawResult,
    List<String> columns,
    List<List<String>> rows,
    List<QueryTraceStepResponse> traceSteps) {
}
