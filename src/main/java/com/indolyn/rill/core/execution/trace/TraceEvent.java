package com.indolyn.rill.core.execution.trace;

public record TraceEvent(
    String stage,
    String component,
    String sourceFile,
    String sourceMethod,
    String detail) {
}
