package com.indolyn.rill.app.dto;

public record QueryTraceStepResponse(
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
}
