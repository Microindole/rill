package com.indolyn.rill.app.service;

import com.indolyn.rill.app.dto.QueryExecuteResponse;
import com.indolyn.rill.app.dto.QueryHistoryItemResponse;
import com.indolyn.rill.app.dto.WorkspaceSessionResponse;
import com.indolyn.rill.app.dto.WorkspaceSessionSummaryResponse;

import java.util.List;

public interface WorkspaceService {

    WorkspaceSessionResponse createSession();

    WorkspaceSessionResponse getSession(String sessionId);

    List<WorkspaceSessionSummaryResponse> listSessions();

    List<QueryHistoryItemResponse> getSessionHistory(String sessionId);

    void deleteSession(String sessionId);

    QueryExecuteResponse execute(String sessionId, String sql);
}
