package com.indolyn.rill.app.service;

import java.time.Instant;

public record JwtPrincipal(
    Long userId,
    String username,
    String role,
    String kernelDbName,
    String jwtId,
    Instant expiresAt) {
}
