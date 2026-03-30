package com.indolyn.rill.app.service;

import com.indolyn.rill.app.dto.QueryExecuteResponse;
import com.indolyn.rill.app.dto.QueryHistoryItemResponse;

import java.util.List;

public interface QueryTraceService {

    QueryExecuteResponse execute(String dbName, String sql);

    QueryExecuteResponse getTrace(String traceId);

    List<QueryHistoryItemResponse> getHistory();
}
