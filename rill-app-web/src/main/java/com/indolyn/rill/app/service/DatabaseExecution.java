package com.indolyn.rill.app.service;

import com.indolyn.rill.core.execution.QueryResult;

/**
 * Application-layer wrapper for one embedded database execution.
 */
public record DatabaseExecution(
    String dbName,
    String sql,
    QueryResult queryResult,
    String rawResult) {}
