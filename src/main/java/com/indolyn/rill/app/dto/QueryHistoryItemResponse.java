package com.indolyn.rill.app.dto;

import java.time.Instant;

public record QueryHistoryItemResponse(
    String traceId,
    String dbName,
    String sql,
    boolean success,
    long elapsedMs,
    Instant executedAt) {
}
