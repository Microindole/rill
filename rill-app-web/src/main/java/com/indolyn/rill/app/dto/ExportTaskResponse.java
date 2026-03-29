package com.indolyn.rill.app.dto;

import java.time.Instant;

public record ExportTaskResponse(
    Long id,
    String title,
    String description,
    String dbName,
    String sql,
    String exportFormat,
    String status,
    String outputPath,
    String lastError,
    Instant createdAt,
    Instant updatedAt,
    Instant completedAt) {
}
