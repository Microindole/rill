package com.indolyn.rill.app.dto;

import java.time.Instant;

public record SqlSnippetResponse(
    Long id,
    String title,
    String description,
    String sql,
    Instant createdAt,
    Instant updatedAt) {
}
