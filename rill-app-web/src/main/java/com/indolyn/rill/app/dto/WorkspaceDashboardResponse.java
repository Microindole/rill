package com.indolyn.rill.app.dto;

import java.util.List;

public record WorkspaceDashboardResponse(
    int totalSessions,
    int totalQueryHistory,
    int totalSnippets,
    int totalScenarios,
    List<String> loadedDatabases,
    List<WorkspaceSessionSummaryResponse> sessions,
    List<QueryHistoryItemResponse> recentQueries) {
}
