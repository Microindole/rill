package com.indolyn.rill.app.dto;

import java.time.Instant;

public record DemoScenarioResponse(
    Long id,
    String title,
    String description,
    String sqlScript,
    Instant createdAt,
    Instant updatedAt) {
}
