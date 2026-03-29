package com.indolyn.rill.app.dto;

public record ExportTaskRequest(
    String title,
    String description,
    String dbName,
    String sql,
    String exportFormat) {
}
