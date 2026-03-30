package com.indolyn.rill.app.dto;

public record LoginResponse(
    Long userId,
    String username,
    String displayName,
    String role,
    String kernelDbName,
    String token,
    String tokenType) {
}
