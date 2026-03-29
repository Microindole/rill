package com.indolyn.rill.app.dto;

import java.time.Instant;
import java.util.List;

public record WorkspaceSessionResponse(
    String sessionId,
    String currentDatabase,
    Instant createdAt,
    Instant lastUsedAt,
    List<String> loadedDatabases,
    List<QueryHistoryItemResponse> recentQueries) {
}
