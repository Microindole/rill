package com.indolyn.rill.app.dto;

import java.time.Instant;

public record WorkspaceSessionSummaryResponse(
    String sessionId,
    String currentDatabase,
    Instant createdAt,
    Instant lastUsedAt,
    int recentQueryCount) {
}
